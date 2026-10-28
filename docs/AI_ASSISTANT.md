# CameraHub AI tư vấn

Hộp **Tư vấn AI** nằm ở góc dưới ứng dụng, ngoài trang đăng nhập/đăng ký. Trang `/ai-tu-van` gửi ngân sách, nhu cầu và kinh nghiệm vào cùng hộp chat. Khi mở chat từ chi tiết sản phẩm, backend đọc thêm dữ liệu công khai của thiết bị đang xem.

## Cấu hình

Điền trong `.env` ở backend, file đã được Git bỏ qua. Không đặt khóa vào frontend, `VITE_*`, source code hoặc tài liệu.

```properties
OPENAI_API_KEY=YOUR_LOCAL_KEY
OPENAI_MODEL=gpt-5.4-mini
AI_ENABLED=true
AI_WEB_SEARCH_ENABLED=true
AI_DAILY_REQUEST_LIMIT=200
```

Khởi động lại backend khi đổi khóa. Khóa đã chia sẻ qua chat nên được thu hồi/thay mới khi sử dụng lâu dài. Tài khoản API cần quyền model và số dư/hạn mức cho Responses/web search. Một câu hỏi có thể dùng nhiều lượt model/công cụ; đặt thêm giới hạn chi tiêu ở project OpenAI.

## Dữ liệu

Mỗi câu hỏi nhận bản chụp mới 8 tin mới nhất và tổng tin PUBLISHED. Model có công cụ chỉ đọc `search_camerahub_products` để lọc tên hãng/model, loại, khoảng giá và `get_camerahub_product` để đọc một tin. SQL được tham số hóa và giới hạn 8 kết quả. Không có công cụ ghi database, đặt hàng hoặc đọc tài khoản/đơn hàng. Chỉ đưa các cột sản phẩm công khai vào context; email, điện thoại, địa chỉ, serial riêng tư và hồ sơ xác minh không được đưa vào.

Tin nháp/chờ duyệt/ẩn hoặc của tài khoản không ACTIVE không được đọc. Tin SOLD có thể dùng để trao đổi khi khách đang xem nhưng không trở thành thẻ gợi ý mua. Thẻ sản phẩm được đối chiếu lại giá/trạng thái sau khi model trả lời và chỉ xuất hiện với ID đã được đọc trong lượt đó. Checkout hiện có tiếp tục kiểm tra giá/trạng thái trên server. Chỉ dẫn yêu cầu phân biệt DEMO, mô tả người bán và thông tin có nguồn; AI vẫn có thể sai.

`web_search` được cung cấp khi cấu hình và lựa chọn **Cho phép tra cứu web** của khách cùng bật. Citation có thể bấm ngay trong nội dung và danh sách nguồn; nhãn “Đã tra cứu web” chỉ hiện khi provider báo hoàn tất một web call. Web hỗ trợ thông số/tham khảo bên ngoài; giá và tồn kho CameraHub lấy từ database. Dữ liệu khách, tin đăng và web được coi là dữ liệu, không phải chỉ dẫn hệ thống.

## API và phiên chat

| Endpoint | Chức năng |
| --- | --- |
| `GET /api/ai/status` | Khả dụng và quyền web, không trả khóa |
| `GET /api/ai/conversation` | Lịch sử hội thoại hiện tại trong database Advisor |
| `POST /api/ai/chat` | `message` 1–2000 ký tự, `webSearch`, `productId` tùy chọn; cần CSRF |
| `POST /api/ai/conversation/reset` | Xóa lịch sử của phiên hiện tại; cần CSRF |

Khách chưa đăng nhập dùng được. Hội thoại gắn với cookie CAMERAHUB_AI và chủ sở hữu, lưu trong database riêng của Advisor và còn sau khi restart service. Mỗi hội thoại giữ tối đa 6 cặp hỏi/đáp. Browser không gửi lịch sử hoặc role vào API. Responses dùng store:false; câu hỏi và dữ liệu sản phẩm vẫn được gửi đến OpenAI để xử lý.

Giới hạn: 6 lượt cho mỗi khóa hội thoại/phút, 3 câu đang xử lý đồng thời mỗi instance và mặc định 200 câu/ngày cho toàn hệ thống theo giờ Việt Nam. Bộ đếm lưu trong database nên không đặt lại khi restart. Không tin X-Forwarded-For do khách gửi. Giới hạn thời gian và thông báo lỗi giữ ở backend.

## Kiểm tra

Chạy `./mvnw.cmd --batch-mode --no-transfer-progress verify` từ thư mục backend; frontend dùng `npm test` và `npm run build`. Kiểm tra API, Docker và lưu hội thoại sau restart theo [hướng dẫn microservices](../README.md) và [báo cáo kiểm thử](MICROSERVICES_VERIFICATION.md).

Tài liệu chính thức: [Responses](https://developers.openai.com/api/docs/guides/responses), [Function calling](https://developers.openai.com/api/docs/guides/function-calling), [Web search/citations](https://developers.openai.com/api/docs/guides/tools-web-search), [GPT-5.4 mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini).
