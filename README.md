# CRUD API — Spring Boot + PostgreSQL

Dự án cá nhân để học Spring Boot. REST API quản lý `Product` với đầy đủ CRUD,
phân trang, tìm kiếm, validation và xử lý lỗi tập trung.

## Tech stack

| Thành phần | Version |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring MVC / Data JPA / Validation | theo BOM của Boot |
| PostgreSQL | 18 |
| Flyway | theo BOM của Boot |
| Maven | 3.9.16 (có wrapper `mvnw`) |

## Cấu trúc thư mục

```
src/main/java/com/example/crudapi/
├── CrudApiApplication.java      # entry point
├── config/                      # JpaAuditingConfig, OpenApiConfig
├── entity/Product.java          # JPA entity ánh xạ bảng products
├── repository/                  # Spring Data JPA repository
├── service/ProductService.java  # business logic + transaction boundary
├── dto/                         # ProductRequest/Response, PageResponse (Java record)
├── web/ProductController.java   # REST endpoints
└── exception/                   # custom exception + @RestControllerAdvice

src/main/resources/
├── application.yml
└── db/migration/                # Flyway: V1 tạo bảng, V2 seed dữ liệu mẫu
```

Luồng xử lý: **Controller → Service → Repository → PostgreSQL**.
Controller chỉ lo HTTP, Service giữ business logic và ranh giới transaction,
Repository chỉ truy vấn DB.

## Chạy dự án

### 1. Khởi động PostgreSQL

**Cách A — Dùng Postgres cài sẵn trên máy (mặc định):**

```bash
psql -U postgres -c "CREATE DATABASE crud_learning;"
```

`application.yml` mặc định trỏ tới `localhost:5432/crud_learning` với user `postgres`.
Nếu mật khẩu khác `postgres`, truyền qua biến môi trường:

```powershell
$env:DB_PASSWORD="123@123"; .\mvnw spring-boot:run
```

**Cách B — Docker (DB riêng biệt, không đụng vào Postgres local):**

```bash
docker compose up -d
```

Container map ra cổng **5433** (vì 5432 đã bị Postgres local chiếm), database `crud_learning`,
user/password `postgres`/`postgres`. Khi dùng cách này phải trỏ app sang 5433:

```powershell
$env:DB_PORT="5433"; .\mvnw spring-boot:run
```

### 2. Chạy ứng dụng

```bash
./mvnw spring-boot:run
# hoặc bật profile dev để xem câu SQL Hibernate sinh ra:
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Flyway tự chạy migration khi khởi động: tạo bảng `products` và chèn 3 bản ghi mẫu.

### 3. Chạy test

```bash
./mvnw test
```

Test hiện tại không cần database (service dùng Mockito, controller dùng `@WebMvcTest`).

## API

Swagger UI: <http://localhost:8080/swagger-ui.html>

| Method | Endpoint | Mô tả | Status thành công |
|---|---|---|---|
| GET | `/api/v1/products` | Danh sách, phân trang | 200 |
| GET | `/api/v1/products/{id}` | Chi tiết | 200 |
| POST | `/api/v1/products` | Tạo mới | 201 + header `Location` |
| PUT | `/api/v1/products/{id}` | Cập nhật toàn bộ | 200 |
| DELETE | `/api/v1/products/{id}` | Xoá | 204 |

Query param của `GET /api/v1/products`:
`keyword` (tìm theo name hoặc sku), `page`, `size`, `sort` (ví dụ `sort=name,asc`).

### Ví dụ

```bash
# Danh sách
curl "http://localhost:8080/api/v1/products?keyword=chuot&page=0&size=10&sort=name,asc"

# Tạo mới
curl -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-100","name":"Tai nghe","description":"Bluetooth 5.3","price":890000,"quantity":20}'

# Cập nhật
curl -X PUT http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -d '{"sku":"SKU-001","name":"Ban phim co v2","description":"switch brown","price":1350000,"quantity":12}'

# Xoá
curl -X DELETE http://localhost:8080/api/v1/products/1
```

### Response lỗi

Theo chuẩn RFC 7807 (`ProblemDetail`):

```json
{
  "type": "about:blank",
  "title": "Validation failed",
  "status": 400,
  "detail": "Du lieu gui len khong hop le",
  "timestamp": "2026-10-07T03:00:00Z",
  "errors": { "sku": "sku khong duoc de trong" }
}
```

| Tình huống | HTTP status |
|---|---|
| Payload không hợp lệ | 400 |
| Không tìm thấy id | 404 |
| SKU trùng / vi phạm ràng buộc DB | 409 |
| Lỗi không lường trước | 500 |

## Những điểm đáng chú ý khi học

- **Flyway thay vì `ddl-auto=update`**: schema được version hoá trong `db/migration`,
  Hibernate để `validate` nên sẽ báo lỗi ngay lúc khởi động nếu entity lệch với bảng thật.
- **DTO tách khỏi Entity**: client không thể tự set `id`, `version`, `createdAt`;
  và đổi cấu trúc bảng không làm vỡ API contract.
- **`@Transactional(readOnly = true)` ở cấp class**, chỉ method ghi mới override bằng `@Transactional`.
- **Không gọi `save()` trong `update()`**: entity đang ở trạng thái *managed*,
  Hibernate tự flush khi transaction commit (dirty checking).
- **`@Version`**: optimistic locking, chống lost update khi hai request cùng sửa một bản ghi → 409.
- **`open-in-view: false`**: tắt Open Session In View, buộc tầng service phải nạp đủ dữ liệu,
  tránh lazy loading ngoài ý muốn ở tầng view.
- **`@EnableJpaAuditing` đặt ở class config riêng** chứ không ở class `@SpringBootApplication`,
  nếu không `@WebMvcTest` sẽ fail vì thiếu JPA metamodel.

## Gợi ý bài tập tiếp theo

1. Thêm entity `Category` và quan hệ `@ManyToOne` từ `Product`.
2. Viết integration test với Testcontainers (Postgres thật trong Docker).
3. Thêm `PATCH` để cập nhật một phần.
4. Thêm Spring Security + JWT.
5. Thêm Spring Boot Actuator để xem health/metrics.
