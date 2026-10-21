import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** Explicit development fixtures. Never used by Flyway or application startup. */
class SeedDemo {
    static final String SEED = "camerahub-demo-v1";
    static final String PASSWORD = "CameraHubDemo!2026";
    static final Path PRIVATE_ROOT = Path.of("storage", "private").toAbsolutePath().normalize();
    static final List<Path> createdFiles = new ArrayList<>();
    static int accountsCreated, productsCreated;

    public static void main(String[] args) throws Exception {
        boolean committed = false;
        try (var db = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"))) {
            db.setAutoCommit(false);
            try {
                if (!"camerahub_mvp_dev".equals(scalar(db, "SELECT current_database()"))) throw new IllegalStateException("Only camerahub_mvp_dev is allowed");
                if (!"46".equals(scalar(db, "SELECT count(*) FROM information_schema.tables WHERE table_schema='camerahub' AND table_type='BASE TABLE'")))
                    throw new IllegalStateException("Expected the migrated 46-table schema");
                execute(db, "SELECT pg_advisory_xact_lock(21620261)");

                var admin = account(db, "admin", "Demo Quản trị viên", "ADMIN");
                account(db, "buyer", "Demo Người mua", "BUYER");
                var seller = account(db, "seller", "Demo Người bán 01", "SELLER");
                var seller2 = account(db, "seller2", "Demo Người bán 02", "SELLER");
                var pending = account(db, "pending", "Demo Hồ sơ chờ duyệt", "BUYER");
                var revision = account(db, "revision", "Demo Hồ sơ cần bổ sung", "BUYER");
                account(db, "inspector", "Demo Kiểm định viên", "INSPECTOR");

                verification(db, admin, seller, "seller", "VERIFIED");
                verification(db, admin, seller2, "seller2", "VERIFIED");
                verification(db, admin, pending, "pending", "PENDING");
                verification(db, admin, revision, "revision", "NEEDS_REVISION");

                product(db, admin, seller, "canon-r6", "Canon", "EOS R6", "BODY", "Canon EOS R6 — body", 28_500_000L, 18000,
                        "Vỏ có xước nhẹ ở đáy; màn hình và nút bấm hoạt động theo mô tả mẫu.", "Pin, sạc và dây đeo.");
                product(db, admin, seller2, "sony-a6400", "Sony", "Alpha a6400", "BODY", "Sony Alpha a6400 — body", 14_900_000L, 12500,
                        "Ngoại hình còn đẹp, trầy nhẹ ở ngàm; chưa có báo cáo kiểm định.", "Pin, sạc và nắp body.");
                product(db, admin, seller, "fuji-xt30", "Fujifilm", "X-T30 II", "BODY", "Fujifilm X-T30 II — màu bạc", 19_500_000L, 8200,
                        "Xước nhỏ ở đế; kính ngắm và màn hình được mô tả hoạt động bình thường.", "Pin, dây đeo và hộp.");
                product(db, admin, seller2, "nikon-z6ii", "Nikon", "Z6 II", "BODY", "Nikon Z6 II — body", 24_800_000L, 22000,
                        "Cao su tay cầm có dấu sử dụng; chưa xác minh tình trạng vật lý.", "Pin, sạc, nắp body và dây đeo.");
                product(db, admin, seller, "canon-50", "Canon", "RF 50mm F1.8 STM", "LENS", "Canon RF 50mm F1.8 STM", 3_900_000L, null,
                        "Vỏ có vết xước nhỏ; thông tin kính và lấy nét là mô tả minh họa.", "Nắp trước, nắp sau.");
                product(db, admin, seller2, "sony-2470", "Sony", "FE 24-70mm F2.8 GM", "LENS", "Sony FE 24–70mm F2.8 GM", 22_500_000L, null,
                        "Vòng zoom có dấu sử dụng; chưa có kết quả kiểm định quang học.", "Nắp, hood và túi đựng.");
                product(db, admin, seller, "sigma-1850", "Sigma", "18-50mm F2.8 DC DN", "LENS", "Sigma 18–50mm F2.8 DC DN — Sony E", 9_200_000L, null,
                        "Ngoại hình có xước nhỏ; thông số tình trạng chỉ dùng cho demo.", "Nắp trước, nắp sau và hood.");
                product(db, admin, seller2, "godox-v1", "Godox", "V1-C", "OTHER", "Godox V1-C — đèn flash Canon", 3_200_000L, null,
                        "Có dấu sử dụng trên chân đèn; chưa có báo cáo kiểm tra thực tế.", "Pin, sạc và túi đựng.");

                if (!"7".equals(scalar(db, "SELECT count(*) FROM camerahub.users WHERE id IN (SELECT entity_id FROM camerahub.audit_logs WHERE entity_type='DEMO_SEED' AND action='CREATE_ACCOUNT' AND correlation_id=?)", SEED)))
                    throw new IllegalStateException("Expected seven demo accounts");
                if (!"8".equals(scalar(db, "SELECT count(*) FROM camerahub.products WHERE id IN (SELECT entity_id FROM camerahub.audit_logs WHERE entity_type='DEMO_SEED' AND action='CREATE_PRODUCT' AND correlation_id=?)", SEED)))
                    throw new IllegalStateException("Expected eight demo products");
                db.commit();
                committed = true;
                System.out.println("Demo seed committed: " + accountsCreated + " new accounts; " + productsCreated + " new products.");
                System.out.println("Seven demo accounts and eight demo products are present. Existing rows and passwords were preserved.");
            } catch (Exception exception) {
                db.rollback();
                // Never print connection configuration or database record contents on errors.
                System.err.println("Demo seed rolled back: " + exception.getClass().getSimpleName()
                        + (exception instanceof java.sql.SQLException sql ? " (SQLSTATE " + sql.getSQLState() + ")"
                        : exception instanceof IllegalStateException ? " (" + exception.getMessage() + ")" : ""));
            }
        } finally {
            if (!committed) for (var file : createdFiles) Files.deleteIfExists(file);
        }
        if (!committed) System.exit(1);
    }

    static UUID account(Connection db, String alias, String name, String role) throws Exception {
        var id = id("account/" + alias);
        String email = alias + "@demo.camerahub.test";
        String byEmail = scalar(db, "SELECT id::text FROM camerahub.users WHERE lower(email)=?", email);
        if (byEmail != null) {
            if (!id.toString().equals(byEmail) || !owned(db, id, "CREATE_ACCOUNT")) throw new IllegalStateException("Account collision");
            return id;
        }
        execute(db, "INSERT INTO camerahub.users(id,email,password_hash,full_name) VALUES (?,?,?,?)", id, email,
                new BCryptPasswordEncoder(12).encode(PASSWORD), name);
        execute(db, "INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,'BUYER')", id);
        if (!"BUYER".equals(role)) execute(db, "INSERT INTO camerahub.user_roles(user_id,role_code) VALUES (?,?)", id, role);
        mark(db, id, id, "CREATE_ACCOUNT");
        accountsCreated++;
        return id;
    }

    static void verification(Connection db, UUID admin, UUID seller, String alias, String status) throws Exception {
        var id = id("verification/" + alias);
        if (exists(db, "seller_verifications", id)) {
            if (!owned(db,id,"CREATE_VERIFICATION")) throw new IllegalStateException("Verification collision");
            return;
        }
        String key = id("evidence/" + alias) + ".png";
        Path path = PRIVATE_ROOT.resolve(key);
        if (Files.exists(path)) throw new IllegalStateException("Evidence file collision");
        Files.createDirectories(PRIVATE_ROOT);
        var image = new BufferedImage(1000, 480, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(new Color(247,246,242)); graphics.fillRect(0,0,1000,480);
            graphics.setColor(new Color(38,38,38)); graphics.setFont(new Font("SansSerif",Font.BOLD,42));
            graphics.drawString("CAMERAHUB DEMO ONLY",45,95);
            graphics.setFont(new Font("SansSerif",Font.PLAIN,25));
            graphics.drawString("Synthetic seller verification fixture",45,170);
            graphics.drawString(alias + "@demo.camerahub.test",45,225);
            graphics.drawString("No real identity or personal documents",45,280);
            graphics.drawString("Not valid proof of identity",45,335);
        } finally { graphics.dispose(); }
        // Register for cleanup before writing, including partial filesystem failures.
        createdFiles.add(path);
        if (!ImageIO.write(image,"png",path.toFile())) throw new IllegalStateException("PNG encoder unavailable");
        boolean pending = "PENDING".equals(status);
        execute(db, """
                INSERT INTO camerahub.seller_verifications(id,seller_id,status,evidence_private_key,review_note,reviewed_by,reviewed_at)
                VALUES (?,?,?,?,?,?,CASE WHEN CAST(? AS boolean) THEN NULL ELSE now() END)
                """, id,seller,status,key,pending ? null : "NEEDS_REVISION".equals(status)
                ? "DEMO: Vui lòng gửi lại file minh họa rõ nét hơn. Đây không phải hồ sơ thật."
                : "DEMO: Trạng thái xác minh mô phỏng, không xác minh danh tính thật.", pending ? null : admin, pending);
        mark(db,admin,id,"CREATE_VERIFICATION");
    }

    static void product(Connection db, UUID admin, UUID seller, String alias, String brandName, String modelName,
                        String type, String title, long price, Integer shutter, String condition, String accessories) throws Exception {
        var id = id("product/" + alias);
        if (exists(db,"products",id)) {
            if (!owned(db,id,"CREATE_PRODUCT")) throw new IllegalStateException("Product collision");
            return;
        }
        String brand = scalar(db,"SELECT id::text FROM camerahub.brands WHERE name=?",brandName);
        if (brand == null) {
            var brandId=id("brand/"+brandName);
            execute(db,"INSERT INTO camerahub.brands(id,name) VALUES (?,?)",brandId,brandName);
            brand=brandId.toString();
        }
        String model = scalar(db,"SELECT id::text FROM camerahub.camera_models WHERE brand_id=? AND model_name=? AND device_type=?",UUID.fromString(brand),modelName,type);
        if (model == null) {
            var modelId=id("model/"+alias);
            execute(db,"INSERT INTO camerahub.camera_models(id,brand_id,model_name,device_type) VALUES (?,?,?,?)",modelId,UUID.fromString(brand),modelName,type);
            model=modelId.toString();
        }
        execute(db,"""
                INSERT INTO camerahub.products(id,seller_id,model_id,title,serial_number_private,serial_last4,
                    shutter_count,price_vnd,description,condition_notes,repair_history,accessories,listing_status,
                    inspection_badge,reviewed_by,reviewed_at)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'PUBLISHED','NOT_INSPECTED',?,now())
                """,id,seller,UUID.fromString(model),"[DEMO] "+title,"DEMO-"+alias,"DEMO",shutter,price,
                "DỮ LIỆU DEMO. Không phải hàng bán thật. Giá, số lần chụp, tình trạng và phụ kiện là thông tin giả lập để trình diễn CameraHub.",
                "DEMO · "+condition,"DEMO: Chưa xác minh lịch sử sửa chữa.","DEMO: "+accessories,admin);
        execute(db,"INSERT INTO camerahub.product_status_history(product_id,old_status,new_status,actor_id,reason) VALUES (?,NULL,'PUBLISHED',?,'DEMO: seeded for presentation')",id,admin);
        mark(db,admin,id,"CREATE_PRODUCT");
        productsCreated++;
    }

    static boolean exists(Connection db,String table,UUID id) throws Exception {
        // Table names are internal constants only, never supplied by a user or environment value.
        return scalar(db,"SELECT id::text FROM camerahub."+table+" WHERE id=?",id)!=null;
    }
    static boolean owned(Connection db,UUID id,String action) throws Exception {
        return scalar(db,"SELECT id::text FROM camerahub.audit_logs WHERE entity_id=? AND entity_type='DEMO_SEED' AND correlation_id=? AND action=?",id,SEED,action)!=null;
    }
    static void mark(Connection db,UUID actor,UUID entity,String action) throws Exception {
        execute(db,"INSERT INTO camerahub.audit_logs(actor_id,entity_type,entity_id,action,correlation_id,new_data) VALUES (?,'DEMO_SEED',?,?,?,'{\"synthetic\":true}'::jsonb)",actor,entity,action,SEED);
    }
    static UUID id(String value) { return UUID.nameUUIDFromBytes((SEED+"/"+value).getBytes(StandardCharsets.UTF_8)); }
    static void execute(Connection db,String sql,Object... args) throws Exception {
        try (var statement=db.prepareStatement(sql)) { bind(statement,args); statement.execute(); }
    }
    static String scalar(Connection db,String sql,Object... args) throws Exception {
        try (var statement=db.prepareStatement(sql)) { bind(statement,args); try (var rows=statement.executeQuery()) { return rows.next()?rows.getString(1):null; } }
    }
    static void bind(java.sql.PreparedStatement statement,Object[] args) throws Exception {
        for(int i=0;i<args.length;i++) statement.setObject(i+1,args[i]);
    }
}
