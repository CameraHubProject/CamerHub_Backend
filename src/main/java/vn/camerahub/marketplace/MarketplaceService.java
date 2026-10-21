package vn.camerahub.marketplace;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketplaceService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    public MarketplaceService(JdbcTemplate jdbc, ObjectMapper json) { this.jdbc=jdbc; this.json=json; }

    public static final String PRODUCT_SELECT = """
        SELECT p.id,p.title,b.name AS brand,m.model_name,m.device_type,p.price_vnd,p.shutter_count,
        p.condition_notes,p.inspection_badge,p.description,p.accessories,p.repair_history,p.listing_status,
        p.presentation::text,m.specs::text,p.seller_id
        FROM camerahub.products p JOIN camerahub.camera_models m ON m.id=p.model_id
        JOIN camerahub.brands b ON b.id=m.brand_id
        """;
    public List<Map<String,Object>> products() {
        return jdbc.query(PRODUCT_SELECT+" WHERE p.listing_status='PUBLISHED' ORDER BY p.created_at DESC,p.id",this::product);
    }
    public Map<String,Object> detail(UUID id) {
        var rows=jdbc.query(PRODUCT_SELECT+" WHERE p.id=? AND p.listing_status='PUBLISHED'",this::product,id);
        if(rows.isEmpty()) throw failure(HttpStatus.NOT_FOUND,"Thiết bị không còn được đăng bán.");
        var result=rows.getFirst();
        var checks=jdbc.queryForList("""
            SELECT d.display_name AS name,r.result,r.notes FROM camerahub.inspection_results r
            JOIN camerahub.inspection_reports report ON report.id=r.report_id
            JOIN camerahub.inspection_requests request ON request.id=report.request_id
            JOIN camerahub.inspection_check_definitions d ON d.code=r.check_code
            WHERE request.product_id=? AND report.superseded_at IS NULL
            AND report.id=(SELECT r2.id FROM camerahub.inspection_reports r2
              JOIN camerahub.inspection_requests q2 ON q2.id=r2.request_id
              WHERE q2.product_id=? AND r2.superseded_at IS NULL ORDER BY r2.issued_at DESC LIMIT 1)
            ORDER BY d.code
            """,id,id);
        result.put("inspectionChecks",checks);
        return result;
    }
    private Map<String,Object> product(java.sql.ResultSet row,int index) throws java.sql.SQLException {
        var item=new LinkedHashMap<String,Object>();
        item.put("id",row.getString("id")); item.put("title",row.getString("title"));
        item.put("brand",row.getString("brand")); item.put("modelName",row.getString("model_name"));
        item.put("deviceType",row.getString("device_type")); item.put("priceVnd",row.getString("price_vnd"));
        item.put("shutterCount",row.getObject("shutter_count")); item.put("conditionNotes",row.getString("condition_notes"));
        item.put("inspectionBadge",row.getString("inspection_badge")); item.put("description",row.getString("description"));
        item.put("accessories",row.getString("accessories")); item.put("repairHistory",row.getString("repair_history"));
        item.put("listingStatus",row.getString("listing_status")); item.put("presentation",parse(row.getString("presentation")));
        item.put("specs",parse(row.getString("specs")));
        item.put("images",jdbc.queryForList("SELECT '/api/public/product-media/'||id::text FROM camerahub.product_media WHERE product_id=? AND storage_key LIKE 'product-public/%' ORDER BY sort_order,created_at",String.class,row.getObject("id",UUID.class)));
        return item;
    }
    public List<Map<String,Object>> models() {
        return jdbc.queryForList("SELECT m.id,b.name AS brand,m.model_name AS \"modelName\",m.device_type AS \"deviceType\" FROM camerahub.camera_models m JOIN camerahub.brands b ON b.id=m.brand_id ORDER BY b.name,m.model_name");
    }
    public List<Map<String,Object>> brands() {
        return jdbc.queryForList("SELECT id,name FROM camerahub.brands ORDER BY name");
    }
    public List<Map<String,Object>> showrooms() {
        return jdbc.queryForList("SELECT id,name,address FROM camerahub.showrooms WHERE active ORDER BY name");
    }
    public List<Map<String,Object>> addresses(UUID user) {
        return jdbc.queryForList("SELECT id,recipient_name AS \"recipientName\",phone,province,street_address AS \"streetAddress\",is_default AS \"isDefault\" FROM camerahub.user_addresses WHERE user_id=? ORDER BY is_default DESC,created_at DESC",user);
    }
    @Transactional
    public Map<String,Object> address(UUID user, MarketplaceController.Address input) {
        lockUser(user);
        if(input.isDefault()) jdbc.update("UPDATE camerahub.user_addresses SET is_default=false WHERE user_id=?",user);
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO camerahub.user_addresses(id,user_id,recipient_name,phone,province,street_address,is_default) VALUES (?,?,?,?,?,?,?)",
            id,user,input.recipientName().strip(),input.phone().strip(),input.province().strip(),input.streetAddress().strip(),input.isDefault());
        return Map.of("id",id);
    }
    public List<Map<String,Object>> listings(UUID user) {
        return jdbc.query(PRODUCT_SELECT+" WHERE p.seller_id=? ORDER BY p.created_at DESC",this::product,user);
    }
    @Transactional
    public Map<String,Object> listing(UUID user,MarketplaceController.Listing input) {
        requireSeller(user);
        UUID model=resolveListingModel(input);
        long price=money(input.priceVnd()); UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO camerahub.products(id,seller_id,model_id,title,price_vnd,shutter_count,description,
              condition_notes,accessories,serial_number_private,listing_status,presentation)
            VALUES (?,?,?,?,?,?,?,?,?,?,'PENDING_REVIEW',?::jsonb)
            """,id,user,model,input.title().strip(),price,input.shutterCount(),input.description().strip(),
            input.conditionNotes().strip(),input.accessories(),input.serialNumber(),encode(Map.of("purposes",input.purposes())));
        jdbc.update("INSERT INTO camerahub.product_status_history(product_id,new_status,actor_id,reason) VALUES (?,'PENDING_REVIEW',?,'Người bán gửi tin')",id,user);
        return Map.of("id",id,"listingStatus","PENDING_REVIEW");
    }
    private UUID resolveListingModel(MarketplaceController.Listing input) {
        if(!input.isSelectionValid()) throw failure(HttpStatus.BAD_REQUEST,"Vui lòng chọn một hãng máy hợp lệ.");
        if(input.modelId()!=null) {
            if(jdbc.queryForObject("SELECT count(*) FROM camerahub.camera_models WHERE id=?",Integer.class,input.modelId())!=1)
                throw failure(HttpStatus.BAD_REQUEST,"Vui lòng chọn model có trong danh mục.");
            return input.modelId();
        }
        if(jdbc.queryForObject("SELECT count(*) FROM camerahub.brands WHERE id=?",Integer.class,input.brandId())!=1)
            throw failure(HttpStatus.BAD_REQUEST,"Hãng máy không có trong danh mục.");
        String type=input.deviceType()==null?"BODY":input.deviceType();
        // The existing schema requires model_id. Reuse one explicitly unspecified record per
        // brand/type, rather than attributing the listing to an arbitrary real camera model.
        // This is ordinary listing data, with no schema migration or modification of old models.
        return jdbc.queryForObject("""
            INSERT INTO camerahub.camera_models(brand_id,model_name,device_type)
            VALUES (?,'Chưa khai báo model',?)
            ON CONFLICT(brand_id,model_name,device_type) DO UPDATE SET model_name=EXCLUDED.model_name
            RETURNING id
            """,UUID.class,input.brandId(),type);
    }
    public void requireSeller(UUID user) {
        if(jdbc.queryForObject("SELECT count(*) FROM camerahub.seller_verifications WHERE seller_id=? AND status='VERIFIED'",Integer.class,user)==0)
            throw failure(HttpStatus.FORBIDDEN,"Hoàn tất xác minh người bán trước khi đăng tin.");
    }
    public List<Map<String,Object>> reviews() {
        return jdbc.query(PRODUCT_SELECT+" WHERE p.listing_status='PENDING_REVIEW' ORDER BY p.created_at",this::product);
    }
    @Transactional
    public void review(UUID admin,UUID product,MarketplaceController.Review input) {
        String status=input.approve()?"PUBLISHED":"NEEDS_REVISION";
        if(jdbc.update("UPDATE camerahub.products SET listing_status=?,reviewed_by=?,reviewed_at=now(),rejection_reason=?,updated_at=now() WHERE id=? AND listing_status='PENDING_REVIEW'",status,admin,input.note(),product)!=1)
            throw failure(HttpStatus.CONFLICT,"Tin đăng đã được xử lý.");
        jdbc.update("INSERT INTO camerahub.product_status_history(product_id,old_status,new_status,actor_id,reason) VALUES (?,'PENDING_REVIEW',?,?,?)",product,status,admin,input.note());
    }
    @Transactional
    public List<Map<String,Object>> checkout(UUID user,MarketplaceController.Checkout input) {
        lockUser(user);
        var ids=input.productIds().stream().distinct().sorted().toList();
        if(ids.size()!=input.productIds().size()) throw failure(HttpStatus.BAD_REQUEST,"Mỗi thiết bị chỉ có một chiếc.");
        String fingerprint=hash(encode(List.of(ids,input.delivery(),input.address())));
        var old=jdbc.queryForList("SELECT buyer_id,request_hash FROM camerahub.checkout_batches WHERE id=?",input.requestId());
        if(!old.isEmpty()) {
            if(!user.equals(old.getFirst().get("buyer_id")) || !fingerprint.equals(old.getFirst().get("request_hash")))
                throw failure(HttpStatus.CONFLICT,"Mã yêu cầu đã dùng cho giỏ hàng khác.");
            return batchOrders(user,input.requestId());
        }
        String address;
        if("SHOWROOM".equals(input.delivery())) {
            UUID showroom;
            try { showroom=UUID.fromString(input.address().streetAddress()); }
            catch(IllegalArgumentException e) { throw failure(HttpStatus.BAD_REQUEST,"Chọn showroom nhận hàng."); }
            var rooms=jdbc.queryForList("SELECT name,address FROM camerahub.showrooms WHERE id=? AND active",showroom);
            if(rooms.isEmpty()) throw failure(HttpStatus.BAD_REQUEST,"Showroom chưa khả dụng.");
            address=encode(Map.of("delivery","SHOWROOM","recipientName",input.address().recipientName(),"phone",input.address().phone(),"showroom",rooms.getFirst()));
        } else address=encode(input.address());
        jdbc.update("INSERT INTO camerahub.checkout_batches(id,buyer_id,request_hash) VALUES (?,?,?)",input.requestId(),user,fingerprint);
        for(UUID id:ids) {
            var rows=jdbc.queryForList("SELECT seller_id,listing_status FROM camerahub.products WHERE id=? FOR UPDATE",id);
            if(rows.isEmpty() || !"PUBLISHED".equals(rows.getFirst().get("listing_status")))
                throw failure(HttpStatus.CONFLICT,"Một thiết bị vừa hết hàng. Vui lòng kiểm tra lại giỏ.");
            UUID seller=(UUID)rows.getFirst().get("seller_id");
            if(user.equals(seller)) throw failure(HttpStatus.BAD_REQUEST,"Bạn không thể mua thiết bị của chính mình.");
            UUID order=UUID.randomUUID();
            // Every price and snapshot comes from the database. Browser prices are never accepted.
            jdbc.update("""
                INSERT INTO camerahub.orders(id,product_id,buyer_id,seller_id,agreed_price_vnd,product_snapshot,
                  shipping_address_snapshot,payment_deadline,deposit_deadline,checkout_batch_id)
                SELECT ?,p.id,?,p.seller_id,p.price_vnd,
                  jsonb_build_object('id',p.id,'title',p.title,'priceVnd',p.price_vnd::text,'conditionNotes',p.condition_notes),
                  ?::jsonb,now()+interval '24 hours',now()+interval '24 hours',?
                FROM camerahub.products p WHERE p.id=?
                """,order,user,address,input.requestId(),id);
            jdbc.update("INSERT INTO camerahub.order_deposits(order_id,amount_vnd,deposit_rate_bps) SELECT id,deposit_amount_vnd,deposit_rate_bps FROM camerahub.orders WHERE id=?",order);
            jdbc.update("UPDATE camerahub.products SET listing_status='RESERVED',updated_at=now() WHERE id=?",id);
            jdbc.update("INSERT INTO camerahub.product_status_history(product_id,old_status,new_status,actor_id,reason) VALUES (?,'PUBLISHED','RESERVED',?,'Tạo đơn mô phỏng')",id,user);
        }
        return batchOrders(user,input.requestId());
    }
    private static final String ORDER_SELECT="""
        SELECT id,product_id AS "productId",buyer_id AS "buyerId",seller_id AS "sellerId",order_status AS status,
        agreed_price_vnd::text AS "priceVnd",shipping_fee_vnd::text AS "shippingFeeVnd",total_vnd::text AS "totalVnd",
        deposit_amount_vnd::text AS "depositVnd",balance_product_amount_vnd::text AS "balanceVnd",deposit_rate_bps AS "depositRateBps",
        product_snapshot->>'title' AS title,created_at AS "createdAt",payment_deadline AS "paymentDeadline",
        shipping_address_snapshot::text AS "addressJson" FROM camerahub.orders
        """;
    public List<Map<String,Object>> orders(UUID user) {
        return jdbc.queryForList(ORDER_SELECT+" WHERE buyer_id=? OR seller_id=? ORDER BY created_at DESC",user,user);
    }
    private List<Map<String,Object>> batchOrders(UUID user,UUID batch) {
        return jdbc.queryForList(ORDER_SELECT+" WHERE buyer_id=? AND checkout_batch_id=? ORDER BY id",user,batch);
    }
    public Map<String,Object> order(UUID user,UUID id) {
        var rows=jdbc.queryForList(ORDER_SELECT+" WHERE id=? AND (buyer_id=? OR seller_id=?)",id,user,user);
        if(rows.isEmpty()) throw failure(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng.");
        var result=rows.getFirst(); result.put("address",parse((String)result.remove("addressJson")));
        result.put("payments",jdbc.queryForList("SELECT payment_stage AS stage,amount_vnd::text AS \"amountVnd\",status,created_at AS \"createdAt\" FROM camerahub.payment_records WHERE order_id=? ORDER BY created_at",id));
        return result;
    }
    @Transactional
    public Map<String,Object> pay(UUID user,UUID id,String stage) {
        var rows=jdbc.queryForList("SELECT *,payment_deadline<now() AS expired FROM camerahub.orders WHERE id=? AND buyer_id=? FOR UPDATE",id,user);
        if(rows.isEmpty()) throw failure(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng.");
        var order=rows.getFirst();
        boolean deposit="DEPOSIT".equals(stage);
        if(!deposit && !"BALANCE_AND_SHIPPING".equals(stage)) throw failure(HttpStatus.BAD_REQUEST,"Giai đoạn thanh toán không hợp lệ.");
        // A retry for the same stage never creates a second successful payment.
        if(jdbc.queryForObject("SELECT count(*) FROM camerahub.payment_records WHERE order_id=? AND payment_stage=? AND status='SIMULATED_PAID'",Integer.class,id,stage)>0) return order(user,id);
        String expected=deposit?"AWAITING_DEPOSIT":"AWAITING_BALANCE";
        if(!expected.equals(order.get("order_status")) || Boolean.TRUE.equals(order.get("expired")))
            throw failure(HttpStatus.CONFLICT,"Đơn hàng không còn ở bước thanh toán này hoặc đã hết hạn.");
        long amount=deposit?((Number)order.get("deposit_amount_vnd")).longValue():
            Math.addExact(((Number)order.get("balance_product_amount_vnd")).longValue(),((Number)order.get("shipping_fee_vnd")).longValue());
        jdbc.update("INSERT INTO camerahub.payment_records(order_id,amount_vnd,status,idempotency_key,payment_stage,simulated_at) VALUES (?,?,'SIMULATED_PAID',?,?,now())",id,amount,id+":"+stage,stage);
        jdbc.update("UPDATE camerahub.order_deposits SET status=?,paid_at=COALESCE(paid_at,now()) WHERE order_id=?",deposit?"PAID":"APPLIED",id);
        jdbc.update("UPDATE camerahub.orders SET order_status=?,payment_deadline=now()+interval '24 hours',balance_deadline=CASE WHEN ? THEN now()+interval '24 hours' ELSE balance_deadline END,updated_at=now() WHERE id=?",deposit?"AWAITING_BALANCE":"READY_TO_SHIP",deposit,id);
        return order(user,id);
    }
    @Transactional
    public void cancelUnpaid(UUID user,UUID id) {
        var rows=jdbc.queryForList("SELECT product_id,order_status FROM camerahub.orders WHERE id=? AND buyer_id=? FOR UPDATE",id,user);
        if(rows.isEmpty()) throw failure(HttpStatus.NOT_FOUND,"Không tìm thấy đơn hàng.");
        var order=rows.getFirst();
        if("CANCELLED".equals(order.get("order_status"))) return;
        if(!"AWAITING_DEPOSIT".equals(order.get("order_status"))) throw failure(HttpStatus.CONFLICT,"Chỉ tự hủy được đơn chưa thanh toán.");
        jdbc.update("UPDATE camerahub.orders SET order_status='CANCELLED',updated_at=now() WHERE id=?",id);
        jdbc.update("UPDATE camerahub.order_deposits SET status='CANCELLED' WHERE order_id=?",id);
        jdbc.update("UPDATE camerahub.products SET listing_status='PUBLISHED',updated_at=now() WHERE id=? AND listing_status='RESERVED'",order.get("product_id"));
        jdbc.update("INSERT INTO camerahub.product_status_history(product_id,old_status,new_status,actor_id,reason) VALUES (?,'RESERVED','PUBLISHED',?,'Hủy đơn chưa thanh toán')",order.get("product_id"),user);
    }
    public List<Map<String,Object>> buybacks(UUID user) {
        return jdbc.queryForList("SELECT id,model,condition_notes AS \"conditionNotes\",expected_price_vnd::text AS \"expectedPriceVnd\",status,created_at AS \"createdAt\" FROM camerahub.buyback_requests WHERE user_id=? ORDER BY created_at DESC",user);
    }
    public Map<String,Object> buyback(UUID user,MarketplaceController.Buyback input) {
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO camerahub.buyback_requests(id,user_id,model,condition_notes,expected_price_vnd) VALUES (?,?,?,?,?)",id,user,input.model().strip(),input.conditionNotes().strip(),input.expectedPriceVnd()==null||input.expectedPriceVnd().isBlank()?null:money(input.expectedPriceVnd()));
        return Map.of("id",id,"status","PENDING");
    }
    private void lockUser(UUID user) { jdbc.queryForObject("SELECT id FROM camerahub.users WHERE id=? FOR UPDATE",UUID.class,user); }
    private Map<String,Object> parse(String value) {
        try { return json.readValue(value,new TypeReference<LinkedHashMap<String,Object>>(){}); }
        catch(Exception e) { throw new IllegalStateException("Stored JSON is invalid",e); }
    }
    private String encode(Object value) {
        try { return json.writeValueAsString(value); } catch(Exception e) { throw new IllegalStateException(e); }
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch(Exception e) { throw new IllegalStateException(e); }
    }
    static long money(String value) {
        if(value==null || !value.matches("[1-9][0-9]{0,12}")) throw failure(HttpStatus.BAD_REQUEST,"Giá phải là số nguyên dương, tối đa 13 chữ số.");
        return Long.parseLong(value);
    }
    static ResponseStatusException failure(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
}
