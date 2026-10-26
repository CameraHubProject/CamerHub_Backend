# CAMERAHUB

ĐẶC TẢ CHI TIẾT 12 BUSINESS FLOW

Marketplace máy ảnh cũ | Phiên bản 3.2 | 20/10/2026

Phạm vi: mua ngay hoặc thương lượng, kiểm định tùy chọn/xác minh mô tả, cọc 10%, trả 90% còn lại + phí giao trước gửi máy, vận chuyển mock, bảo vệ 48 giờ và xử lý tranh chấp.

## 1. Quyết định nghiệp vụ đã chốt

| Nội dung | Quyết định |
| --- | --- |
| Mô hình | Marketplace; Buyer và Seller giao dịch trực tiếp trên nền tảng |
| Mua hàng | Mua ngay hoặc chat/thương lượng giá |
| Kiểm định tự nguyện | Bên yêu cầu trả, trừ khi các bên chấp nhận phân bổ khác |
| Kiểm định xác minh | Buyer ứng phí; đúng mô tả Buyer chịu, sai mô tả được xác minh Seller chịu |
| Chưa kiểm định | Được phép đăng bán, phải ghi rõ trạng thái |
| Cọc | 10% giá máy chốt |
| Thanh toán đợt 2 | 90% tiền máy còn lại + phí giao hàng, trước khi Seller gửi máy |
| Hủy sau cọc | Xử lý theo lý do và thời điểm; không mặc định tịch thu cọc |
| Vận chuyển | Buyer trả phí; Mock Shipping Adapter MVP |
| 48 giờ | Bắt đầu khi có đủ xác nhận giao thành công và Buyer đã nhận hàng; mô phỏng |
| Phí sàn | 0đ trong MVP |
| AI | Gợi ý máy đang có bán theo mục đích và ngân sách |

Phân biệt: các thời hạn giữ chỗ/thu tiền, điều kiện mất cọc, khả năng giữ máy trong lúc kiểm định và các quy trình thanh toán tiền thật vẫn cần nhóm xác nhận. Các flow dưới đây là đặc tả đề xuất để phát triển MVP mô phỏng.

## 2. Danh mục flow

F01 — Đăng ký, đăng nhập và xác thực Seller

F02 — Đăng bán và duyệt tin

F03 — Khám phá sản phẩm và AI tư vấn

F04 — Chat và thương lượng giá

F05 — Kiểm định tự nguyện

F06 — Kiểm định xác minh mô tả Seller

F07 — Tạo đơn và cọc 10%

F08 — Trả 90% giá máy và phí vận chuyển

F09 — Tạo vận đơn và giao nhận

F10 — Bảo vệ 48 giờ và hoàn tất

F11 — Hủy đơn, hoàn cọc và hoàn tiền

F12 — Khiếu nại và tranh chấp

## 3. Đặc tả chi tiết

### F01. Đăng ký, đăng nhập và xác thực Seller

Actor: Guest, User, Admin

Tiền điều kiện: Chưa có tài khoản hoặc tài khoản đang hoạt động

Kết quả: Tài khoản hợp lệ, Seller VERIFIED khi được duyệt

Business rule trọng yếu: User có thể cùng là Buyer và Seller; thông tin xác minh không hiển thị công khai.

Luồng chính:

Guest chọn đăng ký, nhập email, mật khẩu và thông tin cơ bản.

Hệ thống kiểm tra dữ liệu, tránh email trùng và tạo User.

Người dùng đăng nhập; hệ thống cấp phiên xác thực.

Muốn đăng bán, User gửi hồ sơ xác thực Seller.

Admin duyệt / yêu cầu bổ sung / từ chối, lưu lý do.

Khi duyệt, Seller được kích hoạt quyền đăng sản phẩm.

Luồng ngoại lệ:

Email trùng hoặc sai định dạng: từ chối yêu cầu.

Hồ sơ thiếu thông tin: NEEDS_REVISION.

Tài khoản bị khóa: không cho thao tác giao dịch.

### F02. Đăng bán và duyệt tin

Actor: Seller, Admin

Tiền điều kiện: Seller VERIFIED

Kết quả: PUBLISHED, NEEDS_REVISION hoặc REJECTED

Business rule trọng yếu: Duyệt tin không đồng nghĩa chứng nhận chất lượng vật lý; mỗi máy độc nhất.

Luồng chính:

Seller chọn đăng bán; nhập danh mục, model, tình trạng, lỗi, giá, phụ kiện, serial (không công khai toàn bộ).

Upload ảnh/video thật qua Object Storage và lưu khóa file.

Lưu nháp hoặc gửi duyệt; hệ thống kiểm tra trường bắt buộc.

Admin đánh giá tính đầy đủ, dấu hiệu trùng máy và nội dung không phù hợp.

Admin duyệt; tin thành PUBLISHED, hoặc trả lại chỉnh sửa/từ chối.

Máy chưa kiểm định hiển thị NOT_INSPECTED.

Luồng ngoại lệ:

Thiếu ảnh/giá: không gửi duyệt.

Tin có dấu hiệu trùng thiết bị: kiểm tra thủ công.

Sửa mô tả quan trọng sau báo cáo kiểm định: đánh giá hiệu lực báo cáo.

### F03. Khám phá sản phẩm và AI tư vấn

Actor: Guest, Buyer, AI

Tiền điều kiện: Tin PUBLISHED

Kết quả: Buyer xác định sản phẩm quan tâm

Business rule trọng yếu: Không được để AI tạo giá, hàng tồn hay chứng nhận kiểm định giả.

Luồng chính:

Người dùng tìm hãng/model, lọc giá, tình trạng và trạng thái kiểm định.

Xem ảnh, lỗi đã khai báo, phụ kiện, giá và báo cáo kiểm định nếu có.

Nếu dùng AI, cung cấp ngân sách, mục đích sử dụng và mức kinh nghiệm.

Backend lọc sản phẩm thực đang bán; AI chỉ giải thích lựa chọn có trong dữ liệu.

Người dùng chọn Mua ngay hoặc Chat Seller.

Luồng ngoại lệ:

Không có kết quả: gợi ý điều chỉnh bộ lọc.

AI lỗi: vẫn cho duyệt sản phẩm thủ công.

Máy vừa reserved/sold: không được checkout.

### F04. Chat và thương lượng giá

Actor: Buyer, Seller

Tiền điều kiện: Buyer đăng nhập; sản phẩm còn giao dịch

Kết quả: Offer ACCEPTED hoặc kết thúc đàm phán

Business rule trọng yếu: Chấp nhận offer không tự khóa máy; chỉ tạo đơn/giữ chỗ hợp lệ mới khóa.

Luồng chính:

Buyer mở cuộc chat gắn với product.

Hai bên hỏi tình trạng/giá/phụ kiện/kiểm định.

Buyer gửi price offer có giá và thời hạn.

Seller ACCEPT / REJECT / COUNTER; nếu COUNTER Buyer quyết định tương tự.

Khi bên nhận ACCEPT, lưu offered_price và thời hạn hiệu lực.

Buyer có thể chuyển sang kiểm định hoặc checkout theo offer.

Luồng ngoại lệ:

Offer hết hạn, bị rút, hoặc máy đã bán: từ chối sử dụng.

Tin nhắn gửi thất bại: retry an toàn.

Người mua và người bán cùng một user: cấm giao dịch.

### F05. Kiểm định tự nguyện

Actor: Buyer hoặc Seller, Admin, Inspector

Tiền điều kiện: Sản phẩm xác định được; yêu cầu kiểm định hợp lệ

Kết quả: Báo cáo được phát hành, phí được ghi nhận

Business rule trọng yếu: Kiểm định không bắt buộc; kết quả máy không đạt vẫn phải được ghi trung thực.

Luồng chính:

Một bên yêu cầu kiểm định VOLUNTARY.

Hiển thị phạm vi, phí và quy tắc người trả mặc định là người yêu cầu.

Nếu muốn phân chia khác, Buyer và Seller trao đổi rồi cùng xác nhận fee agreement trên hệ thống.

Ghi nhận phí mô phỏng, Admin phân công Inspector.

Inspector xác nhận đúng máy/serial, kiểm tra checklist, bổ sung chứng cứ.

Phát hành Camera Health Report, đánh giá Certified theo tiêu chuẩn.

Buyer lựa chọn tiếp tục mua hoặc dừng.

Luồng ngoại lệ:

Không thỏa thuận phí: người yêu cầu tự trả nếu chấp nhận, hoặc hủy kiểm định nhưng vẫn mua bán.

Máy/serial không khớp: tạm dừng.

Không kiểm tra được: NOT_TESTED, không tự đánh PASS.

### F06. Kiểm định xác minh mô tả Seller

Actor: Buyer, Seller, Inspector, Admin

Tiền điều kiện: Buyer chỉ rõ các cam kết cần xác minh

Kết quả: Báo cáo + quyết toán phí có truy vết

Business rule trọng yếu: Phí kiểm định độc lập tiền cọc; không tự khấu trừ nếu chưa có thỏa thuận.

Luồng chính:

Buyer tạo DESCRIPTION_VERIFICATION từ tin đăng.

Backend chụp snapshot các cam kết tại thời điểm yêu cầu.

Buyer tạm ứng phí kiểm định (mô phỏng).

Seller bàn giao đúng máy và Inspector kiểm tra từng cam kết.

Inspector lập kết quả MATCH / MISREPRESENTED / INCONCLUSIVE có bằng chứng.

Nếu đúng mô tả: Buyer chịu phí; nếu sai mô tả đủ điều kiện: Seller chịu phí cuối cùng và xử lý hoàn khoản ứng Buyer.

Nếu chưa đủ căn cứ: Admin quyết toán theo quy tắc đã thông qua.

Buyer có quyền tiếp tục mua, trả giá lại hoặc dừng.

Luồng ngoại lệ:

Seller không hợp tác: đóng/đưa vào xử lý, không tự suy luận là gian lận.

Seller sửa tin: dùng snapshot gốc.

Kết quả bất định: không tự chuyển phí.

### F07. Tạo đơn và cọc 10%

Actor: Buyer, System

Tiền điều kiện: Giá mua ngay hoặc offer còn hiệu lực; kiểm định xong hoặc bỏ qua

Kết quả: AWAITING_BALANCE, deposit PAID

Business rule trọng yếu: Giá sau cọc không được tự thay đổi; cọc được tính vào giá máy, không thu lần hai.

Luồng chính:

Buyer chọn checkout; backend kiểm tra sản phẩm và offer.

Tạo đơn với snapshot giá, thông tin máy, điều khoản và thời hạn cọc.

Đặt giữ máy tạm thời bằng transaction/khóa hợp lệ.

Tính cọc = làm tròn 10% giá máy theo quy tắc nhất quán.

Buyer mô phỏng thanh toán cọc; ghi payment transaction.

Nếu thành công, đơn AWAITING_BALANCE và giữ máy cho Buyer.

Luồng ngoại lệ:

Có đơn/giữ chỗ khác: PRODUCT_UNAVAILABLE.

Cọc thất bại/quá hạn: hủy giữ máy nếu hợp lệ.

Webhook/sự kiện thanh toán lặp: idempotent.

### F08. Trả 90% giá máy và phí vận chuyển

Actor: Buyer, Mock Payment, Shipping Quote

Tiền điều kiện: Cọc PAID; đơn AWAITING_BALANCE

Kết quả: READY_TO_SHIP

Business rule trọng yếu: Không cho Seller tạo giao hàng thực tế nếu chưa đủ hai khoản; phí giao dịch MVP = 0.

Luồng chính:

Buyer mở đơn và xác nhận địa chỉ giao.

Mock Shipping trả báo giá; Buyer chịu phí ship.

Backend tính balance = giá chốt - cọc; đợt 2 = balance + ship.

Buyer xác nhận và thanh toán mô phỏng đợt 2.

Backend xác nhận đã thu đủ, chuyển READY_TO_SHIP và báo Seller.

Luồng ngoại lệ:

Báo giá hết hạn: lấy báo giá mới, Buyer xác nhận lại.

Thanh toán thất bại: vẫn AWAITING_BALANCE.

Quá hạn: sang F11 xét hủy/cọc.

### F09. Tạo vận đơn và giao nhận

Actor: Seller, Buyer, Mock Shipping

Tiền điều kiện: READY_TO_SHIP; đã thanh toán đủ

Kết quả: DELIVERED + buyer_received

Business rule trọng yếu: MVP dùng mock adapter; sự kiện vận chuyển phải lưu lịch sử và xác minh nguồn.

Luồng chính:

Seller xác nhận kiện hàng và địa chỉ lấy hàng.

Backend gửi tạo vận đơn tới Shipping Adapter, nhận tracking code.

Seller bàn giao kiện hàng, trạng thái SHIPPING.

Adapter gửi sự kiện đang giao, giao thành công/thất bại.

Buyer xác nhận thực nhận; backend lưu bằng chứng và thời gian.

Nếu đủ xác nhận giao thành công + nhận hàng, sang F10.

Luồng ngoại lệ:

API lỗi: retry với idempotency, không tạo vận đơn trùng.

Giao thất bại, mất/hỏng hàng: mở sự cố.

Provider báo giao thành công nhưng Buyer phủ nhận: đối soát, không tự mở 48h.

### F10. Bảo vệ 48 giờ và hoàn tất

Actor: Buyer, System, Admin

Tiền điều kiện: Có đủ sự kiện giao thành công và Buyer nhận

Kết quả: COMPLETED hoặc ON_HOLD

Business rule trọng yếu: Mốc 48h không làm mất quyền bảo hành/hậu mãi khác nếu có.

Luồng chính:

Lưu starts_at khi đủ điều kiện; ends_at = starts_at + 48 giờ.

Buyer kiểm tra máy thực tế và có thể mở khiếu nại.

Background job kiểm tra hết hạn và trạng thái dispute.

Nếu không có khiếu nại chặn, tạo simulated payout idempotent.

Cập nhật protection RELEASED và order COMPLETED.

Luồng ngoại lệ:

Khiếu nại hợp lệ: ON_HOLD, sang F12.

Tác vụ chạy trùng: chỉ release một lần.

Chưa có xác nhận nhận hàng: không tự bắt đầu 48h.

### F11. Hủy đơn, hoàn cọc và hoàn tiền

Actor: Buyer, Seller, Admin

Tiền điều kiện: Có đơn hàng; người yêu cầu có quyền

Kết quả: CANCELLED/REFUND_PENDING/REFUNDED và bản quyết toán

Business rule trọng yếu: Tiền hoàn + tiền giữ không vượt tiền đã thu; mất cọc phải có điều khoản và căn cứ.

Luồng chính:

Buyer hoặc Seller chọn hủy và ghi lý do.

Backend kiểm tra trạng thái đơn, số tiền đã thanh toán, vận chuyển.

Nếu chưa cọc: hủy giữ chỗ theo chính sách.

Nếu đã cọc/trả đủ: tạo cancellation request chờ xử lý.

Admin hoặc policy engine xác định khoản hoàn, khoản giữ, căn cứ và trách nhiệm.

Tạo refund settlement, thực thi giao dịch hoàn mô phỏng một lần.

Lưu audit, thông báo và xử lý trạng thái bán lại máy nếu hợp lệ.

Luồng ngoại lệ:

Seller không giao hoặc sai mô tả được xác minh: hướng hoàn toàn bộ các khoản thuộc phạm vi.

Buyer đổi ý: xét chính sách thời điểm và lý do, không tự tịch thu cọc.

Đang vận chuyển: đối chiếu vận đơn và trách nhiệm chi phí.

Refund lỗi: chưa đánh dấu hoàn tất.

### F12. Khiếu nại và tranh chấp

Actor: Buyer, Seller, Admin, Inspector

Tiền điều kiện: Đơn thuộc Buyer; vấn đề nằm trong phạm vi được tiếp nhận

Kết quả: CLOSED với quyết định, bằng chứng và lịch sử

Business rule trọng yếu: Phải tách quyết định tranh chấp khỏi thao tác thanh toán/hoàn tiền; có audit log.

Luồng chính:

Buyer chọn đơn, lý do và tải bằng chứng.

Hệ thống tạo dispute; nếu đủ điều kiện, giữ payout mô phỏng.

Seller nhận yêu cầu và phản hồi bằng chứng.

Admin đối chiếu tin gốc, báo cáo kiểm định, vận chuyển, thanh toán.

Có thể yêu cầu Inspector làm rõ kỹ thuật.

Admin quyết định: chấp nhận giao dịch, hỗ trợ, trả hàng/hoàn tiền hoặc yêu cầu bổ sung.

Thực thi quyết định, cập nhật tiền mô phỏng, thông báo và đóng hồ sơ sau khi hoàn thành.

Luồng ngoại lệ:

Thiếu bằng chứng: yêu cầu bổ sung.

Seller không phản hồi: xử lý theo hạn quy định, không tự kết luận.

Đã release: chuyển theo cơ chế hậu mãi, không giả định thu hồi được ngay.

Refund chưa hoàn tất: dispute vẫn đang xử lý.

## 4. Các trạng thái cần quản lý riêng

Product: DRAFT, PENDING_REVIEW, PUBLISHED, RESERVED, SOLD, HIDDEN

Offer: PENDING, ACCEPTED, REJECTED, COUNTERED, WITHDRAWN, EXPIRED

Order: AWAITING_DEPOSIT, AWAITING_BALANCE, READY_TO_SHIP, SHIPPING, DELIVERED, PROTECTION_ACTIVE, DISPUTED, CANCELLATION_REVIEW, COMPLETED, CANCELLED

Payment: PENDING, SIMULATED_PAID, FAILED, SIMULATED_REFUNDED

Inspection: REQUESTED, FEE_AGREED, ASSIGNED, RECEIVED, IN_PROGRESS, COMPLETED, CANCELLED

Protection: NOT_STARTED, ACTIVE, ON_HOLD, RELEASED

Dispute: OPEN, WAITING_SELLER, INVESTIGATING, DECIDED, RESOLVING, CLOSED

## 5. Các chính sách chưa chốt

Thời hạn thanh toán cọc và thời hạn thanh toán đợt hai.

Chi tiết hoàn/mất cọc tùy thời điểm Buyer hủy; phí vận chuyển khi trả hàng.

Seller có bắt buộc giữ máy trong lúc Buyer yêu cầu kiểm định hay không, nếu có thì tối đa bao lâu.

Tiêu chí đánh giá sai mô tả đủ điều kiện chuyển phí; cách xử lý kết quả INCONCLUSIVE.

Cơ chế thu/giữ/giải ngân tiền thật cần đối tác và kiểm tra pháp lý; MVP chỉ mô phỏng.

## 6. Dữ liệu và test case tối thiểu

Lưu snapshot thông tin máy khi kiểm định xác minh và khi checkout.

Không cho hai đơn đang hoạt động giữ cùng một máy.

Cọc 10% + đợt 2 (90% giá máy + ship) phải khớp tổng đơn.

Không tạo vận đơn nếu chưa nhận đủ thanh toán mô phỏng.

Không bắt đầu 48h nếu thiếu một trong hai sự kiện giao thành công và nhận hàng.

Khi dispute hợp lệ, không thực hiện payout mô phỏng.

Sự kiện payment/shipping/refund lặp không được ghi nhận giao dịch trùng.

File kiểm định, chứng cứ và thông tin cá nhân phải có phân quyền xem và lưu Object Storage theo mức công khai phù hợp.

