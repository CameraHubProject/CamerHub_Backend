package vn.camerahub.identity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SellerVerificationService {
    private final JdbcTemplate jdbc;
    private final PrivateEvidenceStore files;
    private final AccountService accounts;
    public SellerVerificationService(JdbcTemplate jdbc, PrivateEvidenceStore files, AccountService accounts) {
        this.jdbc = jdbc; this.files = files; this.accounts = accounts;
    }

    private static final String SELECT = """
            SELECT v.id,v.seller_id,v.status,v.review_note,v.created_at,v.reviewed_at,u.full_name
            FROM camerahub.seller_verifications v JOIN camerahub.users u ON u.id=v.seller_id
            """;

    public List<Verification> mine(UUID seller) {
        return query(" WHERE v.seller_id=? ORDER BY v.created_at DESC,v.id", seller);
    }

    public List<Verification> queue() {
        return query(" WHERE v.status IN ('PENDING','NEEDS_REVISION') ORDER BY v.created_at,v.id LIMIT 100");
    }

    private List<Verification> query(String where, Object... args) {
        return jdbc.query(SELECT + where, (row, index) -> new Verification(row.getObject("id", UUID.class),
                row.getObject("seller_id", UUID.class), row.getString("full_name"), row.getString("status"),
                row.getString("review_note"), row.getTimestamp("created_at").toInstant(),
                row.getTimestamp("reviewed_at") == null ? null : row.getTimestamp("reviewed_at").toInstant()), args);
    }

    @Transactional
    public Verification submit(UUID seller, MultipartFile file) {
        lockSeller(seller);
        var active = jdbc.queryForList("SELECT status FROM camerahub.seller_verifications WHERE seller_id=? AND status IN ('PENDING','VERIFIED')", String.class, seller);
        if (!active.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Hồ sơ đang chờ duyệt hoặc đã xác minh.");
        var revisions = jdbc.queryForList("SELECT id FROM camerahub.seller_verifications WHERE seller_id=? AND status='NEEDS_REVISION'", UUID.class, seller);
        String key = files.put(file);
        // Keep the previous evidence until commit; delete a new upload on any rollback.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if (status != STATUS_COMMITTED) files.delete(key); }
        });
        UUID id;
        if (!revisions.isEmpty()) {
            id = revisions.getFirst();
            String previous = jdbc.queryForObject("SELECT evidence_private_key FROM camerahub.seller_verifications WHERE id=?", String.class, id);
            jdbc.update("UPDATE camerahub.seller_verifications SET evidence_private_key=?,status='PENDING',reviewed_by=NULL,reviewed_at=NULL WHERE id=?", key, id);
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { if (previous != null) files.delete(previous); }
            });
        } else {
            id = UUID.randomUUID();
            jdbc.update("INSERT INTO camerahub.seller_verifications(id,seller_id,evidence_private_key) VALUES (?,?,?)", id, seller, key);
        }
        accounts.audit(seller, id, "SELLER_VERIFICATION_SUBMITTED");
        return query(" WHERE v.id=?", id).getFirst();
    }

    @Transactional
    public Verification review(UUID admin, UUID id, String decision, String note) {
        if (decision == null || !List.of("VERIFIED","REJECTED","NEEDS_REVISION").contains(decision) || note == null || note.isBlank() || note.length()>2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Chọn quyết định và ghi lý do (tối đa 2.000 ký tự).");
        }
        var found = query(" WHERE v.id=?", id);
        if (found.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ.");
        UUID seller = found.getFirst().sellerId();
        if (seller.equals(admin)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Không thể tự duyệt hồ sơ của mình.");
        lockSeller(seller);
        String status = jdbc.queryForObject("SELECT status FROM camerahub.seller_verifications WHERE id=? FOR UPDATE", String.class, id);
        if (!"PENDING".equals(status)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Chỉ duyệt hồ sơ đang chờ xử lý.");
        jdbc.update("UPDATE camerahub.seller_verifications SET status=?,review_note=?,reviewed_by=?,reviewed_at=now() WHERE id=?", decision, note.strip(), admin, id);
        if ("VERIFIED".equals(decision)) {
            jdbc.update("INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'SELLER') ON CONFLICT DO NOTHING", seller);
        }
        // Store the reason with the decision, without copying private document contents into the audit log.
        jdbc.update("""
                INSERT INTO camerahub.audit_logs(actor_id,entity_type,entity_id,action,old_data,new_data)
                VALUES (?,'SELLER_VERIFICATION',?,'REVIEW',jsonb_build_object('status',CAST(? AS text)),
                  jsonb_build_object('status',CAST(? AS text),'reason',CAST(? AS text)))
                """, admin, id, status, decision, note.strip());
        return query(" WHERE v.id=?", id).getFirst();
    }

    private void lockSeller(UUID seller) {
        var statuses = jdbc.queryForList("SELECT account_status FROM camerahub.users WHERE id=? FOR UPDATE", String.class, seller);
        if (statuses.isEmpty() || !"ACTIVE".equals(statuses.getFirst())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Tài khoản không hoạt động.");
        }
    }

    public java.nio.file.Path evidence(UUID requester, boolean admin, UUID id) {
        var found = query(" WHERE v.id=?", id);
        if (found.isEmpty() || (!admin && !found.getFirst().sellerId().equals(requester))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy hồ sơ.");
        }
        String key = jdbc.queryForObject("SELECT evidence_private_key FROM camerahub.seller_verifications WHERE id=?", String.class, id);
        return files.resolve(key);
    }

    public record Verification(UUID id, UUID sellerId, String fullName, String status, String reviewNote,
                               Instant createdAt, Instant reviewedAt) {}
}
