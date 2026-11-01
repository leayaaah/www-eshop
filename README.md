# IUH eShop — `iuh.fit.eshop`

## Công nghệ

Java 17, Spring Boot 3.4.1, Maven, Spring MVC, Thymeleaf, Spring Data JPA, MariaDB, Spring Security 6, JWT, REST API, Spring AI 1.0.0-M5 (Google Gemini + MariaDB Vector Store).

## Cài đặt

1. Mở `database/eShop.sql` và chạy trong MariaDB/HeidiSQL. **Script có `DROP DATABASE IF EXISTS eshop` nên sẽ xoá
   database `eshop` cũ trước khi tạo lại.**
2. Mở `src/main/resources/application.properties`, chỉnh `spring.datasource.username` và `spring.datasource.password`
   theo MariaDB của bạn.
3. Cài đặt API key Google Gemini: tạo biến môi trường `GEMINI_API_KEY`.
   - PowerShell (tạm thời): `$env:GEMINI_API_KEY="your_api_key_here"`
   - PowerShell (vĩnh viễn): `[System.Environment]::SetEnvironmentVariable("GEMINI_API_KEY", "your_api_key_here", "User")`
   - Xem thêm trong `.env.example`.
4. Mở thư mục project bằng IntelliJ IDEA, chọn JDK 17 và Load Maven Changes.
5. Chạy `iuh.fit.eshop.EshopApplication`.
6. Truy cập `http://localhost:8080`.

## Spring AI (Google Gemini)

- Chat model: `gemini-2.5-flash`
- Embedding model: `text-embedding-004` (768 chiều)
- Vector Store: MariaDB (cần MariaDB ≥ 11.7 cho tính năng vector; fallback là `SimpleVectorStore` file nếu MariaDB cũ)
- Bật/tắt AI: `app.ai.enabled=true` (mặc định). Nếu thiếu `GEMINI_API_KEY` hoặc MariaDB không hỗ trợ vector, ứng dụng vẫn chạy, các endpoint AI trả 503.
- Nạp dữ liệu vector lúc khởi động: `app.ai.ingest-on-startup=true` (mặc định false trong test).

## Tài khoản mẫu

Mật khẩu các tài khoản mẫu: `123456`.

| Username  | Vai trò       | Ghi chú    |
|-----------|---------------|------------|
| seller1   | ROLE_SELLER   | Quản trị   |
| seller2   | ROLE_SELLER   | Quản trị   |
| customer1 | ROLE_CUSTOMER | Khách hàng |
| customer2 | ROLE_CUSTOMER | Khách hàng |
| locked    | ROLE_CUSTOMER | Bị khoá    |

## Chức năng

- CRUD danh mục và sản phẩm cho SELLER.
- Cửa hàng có tìm kiếm, lọc danh mục, phân trang và trang chi tiết.
- Giỏ hàng session, sửa/xoá số lượng, checkout COD/chuyển khoản giả lập, trừ tồn kho trong transaction.
- Đơn hàng của khách và quản lý trạng thái đơn hàng cho SELLER.
- Đăng ký/đăng nhập, mật khẩu BCrypt, JWT trong cookie HttpOnly cho web, đăng xuất xoá cookie.
- Quản lý tài khoản cho SELLER, kiểm tra quyền ở URL và service.
- REST API sản phẩm và endpoint đăng nhập JWT Bearer.

## REST API

- `GET /api/products?keyword=iphone&categoryId=1&page=0&size=10`
- `GET /api/products/{id}`
- `POST /api/auth/login` JSON `{"username":"seller1","password":"123456"}`
- `POST /api/products`, `PUT /api/products/{id}`, `DELETE /api/products/{id}` dùng `Authorization: Bearer <accessToken>`
  với SELLER.

## Lưu ý

- `app.jwt.secret` là khoá demo; thay bằng khoá riêng trước khi triển khai thật.
- `spring.jpa.hibernate.ddl-auto=none` vì schema được tạo bằng `eShop.sql`.
- HTML POST dùng Thymeleaf `th:action` để Spring Security thêm CSRF token.
