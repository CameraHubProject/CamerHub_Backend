# EXE CP3 marketplace API

> Tài liệu tham khảo bản monolith trước khi tách. Mã nguồn và công cụ cũ đã được nén vào `.local/backups/legacy-before-cleanup-20261026.zip`. Lệnh chạy và kết quả kiểm thử hiện tại: [microservices](../microservices/README.md). Xác minh người bán đã được bỏ khỏi ứng dụng hiện tại.

Frontend routes, run instructions and integration limits: `D:\CameraHub_Frontend\docs\PROTOTYPE_MARKETPLACE.md`.

V2 preserves V1, existing accounts/products and existing 10% orders. New orders default to 30%; generated amounts use each order's stored rate. Three tables are added: checkout_batches, showrooms and buyback_requests (46 application tables total). Product presentation metadata and checkout batch references are additive.

| Endpoint | Methods | Behavior |
| --- | --- | --- |
| `/api/public/products` | GET | All published products, safe metadata and media URLs |
| `/api/public/products/{id}` | GET | Published product plus available inspection results |
| `/api/public/sellers/{id}` | GET | Public member name, join date, optional verification badge, live/sold counts and actual review average; active accounts only |
| `/api/public/sellers/{id}/listings` | GET | `status=PUBLISHED` or `SOLD`, zero-based `page`, 12 items per page; unpublished inventory is excluded |
| `/api/public/sellers/{id}/reviews` | GET | Paginated completed-purchase reviews; public author name and product, no order ID or contact/address data |
| `/api/marketplace/orders/{id}/seller-review` | POST | Buyer-only rating 1–5 and comment up to 2000 characters; one review per COMPLETED order, with CSRF |
| `/api/public/models`, `/api/public/showrooms` | GET | Configured models/active showrooms |
| `/api/public/product-media/{id}` | GET | Published photos; pending photos require owner or Admin; no-store |
| `/api/marketplace/addresses` | GET, POST | Owner-scoped address book |
| `/api/marketplace/listings` | GET, POST | Own inventory; any signed-in active account can submit for review, without seller verification or a SELLER role |
| `/api/marketplace/listings/{id}/media` | POST multipart file | Owner/editable status, JPG/PNG, 5 MB, 24 MP, max eight; metadata stripped |
| `/api/marketplace/checkout` | POST | requestId, productIds, delivery (DELIVERY/SHOWROOM), recipient address |
| `/api/marketplace/orders` | GET | Buyer's and seller's orders |
| `/api/marketplace/orders/{id}` | GET | Participant-only detail, snapshots and payment history |
| `/api/marketplace/orders/{id}/pay` | POST | stage DEPOSIT/BALANCE_AND_SHIPPING; simulation |
| `/api/marketplace/orders/{id}/cancel` | POST | Buyer-only unpaid cancellation |
| `/api/marketplace/buyback` | GET, POST | Owner-scoped buyback requests |
| `/api/admin/listings` | GET | Pending listing queue |
| `/api/admin/listings/{id}/review` | POST | Admin-only approve and required note |

All authenticated POSTs require a fresh CSRF token. Checkout IDs bind to owner and canonical request payload. Product locks use UUID order; cart creation commits together or rolls back. The server snapshots database prices. Successful payment stages are unique and ordered. Cancellation releases only the unpaid order's reserved device.

V5 adds `seller_reviews` without modifying earlier migrations or the source schema. Public member pages do not expose email, phone, addresses, verification evidence, private serials or drafts. Sold product details and photos remain viewable with purchasing disabled in the UI; the checkout API continues to accept only PUBLISHED products. Review eligibility comes from the actual COMPLETED order state; payment alone does not create a completed purchase or a rating. Shipping/fulfilment progression remains outside the current payment simulation.

The existing `/api/public/featured-products` contract is retained. AI consultation uses an OpenAI key only in the ignored backend `.env`; see `AI_ASSISTANT.md` for public catalog tools, web citations and limits. OTP, banking and shipping credentials are not connected. No fabricated showroom or inspection/warranty records run at startup.
