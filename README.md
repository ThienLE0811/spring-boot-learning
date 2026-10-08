# CRUD API — Spring Boot + PostgreSQL

Dự án cá nhân để học Spring Boot. REST API quản lý `Product`, `Category` và `User` với đầy đủ
CRUD, phân trang, tìm kiếm, validation, xử lý lỗi tập trung và xác thực JWT.

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
├── config/                      # JpaAuditingConfig, OpenApiConfig, SecurityConfig
├── entity/                      # Product, Category, User, Role — JPA entity
├── repository/                  # Spring Data JPA repository
├── service/                     # business logic + transaction boundary
├── dto/                         # Request/Response, PageResponse (Java record)
├── security/                    # JwtService, JwtAuthenticationFilter, CustomUserDetailsService
├── web/                         # REST endpoints
└── exception/                   # custom exception + @RestControllerAdvice

src/main/resources/
├── application.yml
└── db/migration/                # Flyway: V1-V2 products, V3-V4 categories + FK,
                                 #         V5-V6 users + seed admin, V7 audit columns
```

Luồng xử lý: **Controller → Service → Repository → PostgreSQL**.
Controller chỉ lo HTTP, Service giữ business logic và ranh giới transaction,
Repository chỉ truy vấn DB.

Chi tiết từng bước của một request (kèm SQL Hibernate sinh ra, ranh giới transaction, sơ đồ tuần tự):
[docs/request-flow.md](docs/request-flow.md).

Giải thích từng thư mục, và phân biệt đâu là quy ước bắt buộc của Maven/Spring/Flyway, đâu là tự đặt:
[docs/project-structure.md](docs/project-structure.md).

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

| Method | Endpoint | Mô tả | Quyền | Status thành công |
|---|---|---|---|---|
| POST | `/api/v1/auth/login` | Đăng nhập, trả JWT | công khai | 200 |
| GET | `/api/v1/products` | Danh sách, phân trang | công khai | 200 |
| GET | `/api/v1/products/{id}` | Chi tiết | công khai | 200 |
| POST | `/api/v1/products` | Tạo mới | công khai | 201 + header `Location` |
| PUT | `/api/v1/products/{id}` | Cập nhật toàn bộ | công khai | 200 |
| PATCH | `/api/v1/products/{id}` | Cập nhật một phần | công khai | 200 |
| DELETE | `/api/v1/products/{id}` | Xoá | công khai | 204 |
| GET | `/api/v1/categories` | Danh sách, phân trang | công khai | 200 |
| GET | `/api/v1/categories/{id}` | Chi tiết | công khai | 200 |
| POST | `/api/v1/categories` | Tạo mới | ADMIN | 201 + header `Location` |
| PUT | `/api/v1/categories/{id}` | Cập nhật toàn bộ | công khai | 200 |
| DELETE | `/api/v1/categories/{id}` | Xoá (409 nếu còn product dùng) | ADMIN | 204 |
| GET | `/api/v1/users` | Danh sách, phân trang | ADMIN | 200 |
| GET | `/api/v1/users/{id}` | Chi tiết | ADMIN | 200 |
| POST | `/api/v1/users` | Tạo mới | ADMIN | 201 + header `Location` |
| PUT | `/api/v1/users/{id}` | Cập nhật username + role | ADMIN | 200 |
| DELETE | `/api/v1/users/{id}` | Xoá | ADMIN | 204 |
| PUT | `/api/v1/users/{id}/password` | Admin đặt lại mật khẩu hộ | ADMIN | 204 |
| GET | `/api/v1/users/me` | Thông tin tài khoản đang đăng nhập | đã đăng nhập | 200 |
| PUT | `/api/v1/users/me/password` | Tự đổi mật khẩu | đã đăng nhập | 204 |

Query param của `GET /api/v1/products`:
`keyword` (tìm theo name hoặc sku), `categoryId` (lọc theo category),
`page`, `size`, `sort` (ví dụ `sort=name,asc`).
Hai bộ lọc độc lập nhau, dùng riêng hay kết hợp đều được; thiếu bộ lọc nào thì bỏ qua bộ đó.
`categoryId` không tồn tại trả về trang rỗng (đây là bộ lọc, không phải truy xuất resource).

`GET /api/v1/categories` và `GET /api/v1/users` dùng `keyword` để tìm theo name / username.

### Xác thực & phân quyền

Đăng nhập lấy JWT rồi gửi kèm ở header `Authorization`:

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

curl http://localhost:8080/api/v1/users -H "Authorization: Bearer $TOKEN"
```

Tài khoản seed sẵn: `admin` / `admin123` (role `ADMIN`).

Phân quyền khai báo tập trung theo URL trong `SecurityConfig`, không rải `@PreAuthorize`
trên từng method. **Thứ tự matcher quan trọng** — rule đầu tiên khớp sẽ được áp dụng:

```java
.requestMatchers("/api/v1/users/me", "/api/v1/users/me/**").authenticated()
.requestMatchers("/api/v1/users/**").hasRole("ADMIN")
```

Đảo hai dòng này thì `/users/me` rơi vào nhánh `hasRole("ADMIN")` và user thường
không xem được thông tin của chính mình.

### Quản lý user

`UserResponse` **không có field `password`** — không phải "nhớ đừng trả ra", mà là không
có chỗ để trả. Mật khẩu luôn được `BCryptPasswordEncoder` hash ở tầng service trước khi
chạm DB.

Hai ràng buộc nghiệp vụ vượt ra ngoài phạm vi một bản ghi:

| Tình huống | Kết quả |
|---|---|
| Admin tự xoá tài khoản đang đăng nhập | 400 `Invalid request` |
| Xoá hoặc hạ quyền ADMIN cuối cùng | 409 `Resource in use` |

Đổi mật khẩu tách làm **hai endpoint** thay vì gộp: `/users/me/password` bắt buộc gửi
`currentPassword` (chống kẻ chiếm được phiên đăng nhập đổi mật khẩu), còn
`/users/{id}/password` cho admin reset hộ thì không cần. Gộp lại thì service phải nhận
thêm cờ `isAdmin` để biết có verify hay không — logic phân quyền rò rỉ xuống tầng service.

`password` cũng bị loại khỏi danh sách field được sắp xếp, nên `?sort=password,asc`
trả 400 thay vì âm thầm `ORDER BY password`.

### Quan hệ Product → Category

`products.category_id` **nullable**: product có thể chưa được phân loại, nên `categoryId`
trong payload là tuỳ chọn. Với `PUT`, bỏ `categoryId` đi đồng nghĩa **gỡ** category đang gắn
(PUT thay thế toàn bộ resource).

### PUT vs PATCH

`PUT` thay thế toàn bộ resource: field nào không gửi coi như không có (ví dụ bỏ
`categoryId` nghĩa là gỡ category). `PATCH` chỉ cập nhật field có mặt trong body,
field vắng mặt giữ nguyên giá trị hiện tại — vì vậy cần phân biệt 3 trạng thái cho
mỗi field (không gửi / gửi `null` / gửi giá trị), điều mà một DTO kiểu record bình
thường (như `ProductRequest`) không làm được vì "không gửi" và "gửi `null`" đều bị
Jackson gộp thành cùng một giá trị `null`. `PATCH` vì vậy nhận thẳng `JsonNode` và
service tự kiểm tra từng field có mặt hay không (`JsonNode.has(...)`) rồi mới validate:

```bash
# Chỉ sửa quantity, các field khác giữ nguyên
curl -X PATCH http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -d '{"quantity": 20}'

# Gỡ category đang gắn (gửi categoryId = null), các field khác giữ nguyên
curl -X PATCH http://localhost:8080/api/v1/products/1 \
  -H "Content-Type: application/json" \
  -d '{"categoryId": null}'
```

`ProductResponse` nhúng category ở dạng rút gọn, và do `default-property-inclusion: non_null`
nên field này biến mất khỏi JSON khi product chưa có category:

```json
{
  "id": 1,
  "sku": "SKU-001",
  "name": "Ban phim co",
  "price": 1250000.00,
  "quantity": 15,
  "category": { "id": 1, "name": "Phu kien may tinh" }
}
```

### Ví dụ

```bash
# Danh sách
curl "http://localhost:8080/api/v1/products?keyword=chuot&page=0&size=10&sort=name,asc"

# Lọc theo category
curl "http://localhost:8080/api/v1/products?categoryId=1"

# Kết hợp cả hai bộ lọc
curl "http://localhost:8080/api/v1/products?keyword=ban&categoryId=1"

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
| Body sai cú pháp JSON hoặc sai kiểu (`"role": "SUPERADMIN"`) | 400 |
| Query param sai kiểu (`?categoryId=abc`) | 400 |
| Không tìm thấy id (kể cả `categoryId` không tồn tại) | 404 |
| SKU / tên category trùng, vi phạm ràng buộc DB | 409 |
| Xoá category còn product tham chiếu | 409 (`Resource in use`) |
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
- **`@ManyToOne(fetch = LAZY)` + `@EntityGraph` chống N+1**: để LAZY thì query trả về N product
  sẽ sinh thêm N câu `SELECT category` lúc map sang DTO. `@EntityGraph(attributePaths = "category")`
  trên `findAll`/`findById`/derived query gộp lại thành một `LEFT JOIN FETCH`.
  Bật profile `dev` rồi gọi `GET /api/v1/products` để tự quan sát số câu SQL.
- **Quan hệ khai báo một chiều** (`Product.category`, không có `Category.products`):
  tránh phải đồng bộ hai đầu quan hệ, và danh sách product của một category nên lấy
  bằng query có phân trang chứ không nạp cả `List` vào bộ nhớ.
- **Index cho cột FK**: PostgreSQL *không* tự tạo index cho `category_id`.
  Thiếu nó thì mỗi lần xoá/sửa `categories` phải seq scan cả bảng `products`.
- **Chặn xoá ở tầng service thay vì chờ FK nổ**: FK đã chặn sẵn, nhưng để nó ném
  `DataIntegrityViolationException` thì client chỉ nhận được thông báo chung chung.
  `ResourceInUseException` cho phép trả 409 kèm đúng lý do.
- **`findById` thay vì `getReferenceById`** khi resolve `categoryId`: `getReferenceById`
  chỉ trả proxy, id sai sẽ lộ thành lỗi FK lúc flush (500 khó hiểu) thay vì 404 rõ ràng.
- **`Specification` cho bộ lọc tuỳ chọn**: mỗi bộ lọc tuỳ chọn làm số tổ hợp nhân đôi
  (`keyword` × `categoryId` = 4 trường hợp). Derived query sẽ cần 4 method và 4 nhánh `if`;
  thêm một bộ lọc nữa thành 8. `ProductSpecifications` ghép điều kiện động nên số method
  tăng tuyến tính. Bộ lọc vắng mặt trả về `Specification.unrestricted()` thay vì `null`,
  để phía gọi khỏi phải kiểm tra null trước khi `.and()`.
- **Lọc category so sánh thẳng với khoá ngoại** (`root.get("category").get("id")`):
  Hibernate dùng luôn cột `products.category_id`, không phát sinh join thừa với bảng
  `categories` bên cạnh `LEFT JOIN FETCH` của `@EntityGraph`.
- **`MethodArgumentTypeMismatchException` phải có handler riêng**: không có nó thì
  `?categoryId=abc` rơi xuống `handleUnexpected` và trả 500 cho một lỗi đầu vào.
- **`PATCH` nhận `JsonNode` thay vì DTO kiểu record**: PATCH cần phân biệt "không gửi field"
  (giữ nguyên) với "gửi field = null" (xoá, áp dụng cho `description`/`categoryId`) — hai
  trạng thái mà một field `String`/`Long` bình thường trên record không phân biệt được vì
  Jackson gộp cả hai thành `null`. Đổi lại, `ProductService.patch()` phải tự validate thủ
  công từng field thay vì dựa vào Bean Validation, nên ném `InvalidRequestException` riêng
  (handler mới trong `GlobalExceptionHandler`) chứ không phải `MethodArgumentNotValidException`.
- **`DEFAULT now()` ở DB không thay được `@CreatedDate`**: Hibernate liệt kê đầy đủ mọi cột
  trong câu `INSERT`, nên field null sẽ bind `NULL` và **đè lên** default của DB → vi phạm
  `NOT NULL`. `DEFAULT` chỉ có tác dụng khi cột vắng mặt khỏi câu lệnh (trường hợp file seed
  Flyway). Bảng `users` từng dính đúng lỗi này và chỉ lộ ra khi có endpoint tạo user.
- **`ALTER TABLE ADD COLUMN ... NOT NULL` bắt buộc có `DEFAULT`** khi bảng đã có dữ liệu —
  PostgreSQL cần biết điền gì vào các dòng cũ. Migration trên bảng đã có dữ liệu luôn khó
  hơn `CREATE TABLE` từ đầu, điều mà `ddl-auto=update` giấu đi hoàn toàn.
- **Derived query được kiểm tra lúc khởi động, không phải lúc compile**: `countByRoles` sai
  tên field vẫn biên dịch bình thường, chỉ nổ `PropertyReferenceException` khi Spring Data
  tạo bean repository. `mvn compile` xanh **không** chứng minh repository đúng — phải chạy app.
- **BCrypt phải dùng `matches()` chứ không `equals()`**: mỗi lần `encode()` trộn một salt ngẫu
  nhiên nên hash cùng một chuỗi hai lần ra hai kết quả khác nhau. `matches(raw, hash)` đọc salt
  nhúng sẵn trong `hash` rồi mới so sánh.
- **Thứ tự "sửa entity" và "chạy query" trong một transaction có ý nghĩa**: trước khi chạy query,
  Hibernate tự flush các thay đổi đang chờ (*auto-flush before query*). Vì vậy
  `UserService.update()` phải đếm số ADMIN **trước** khi `setRole()`, nếu không phép đếm sẽ
  chạy trên dữ liệu đã bị hạ quyền.
- **Service không đọc `SecurityContextHolder`**: controller lấy `Authentication` rồi truyền
  username xuống dạng `String`. `SecurityContextHolder` dùng `ThreadLocal` — khái niệm của tầng
  web; tách ra thì service test được bằng Mockito thuần, không phải dựng SecurityContext giả.
- **`@WebMvcTest(addFilters = false)` làm tham số `Authentication` thành `null`**: nó gỡ
  `SecurityContextHolderAwareRequestFilter`, filter duy nhất khiến `request.getUserPrincipal()`
  trả về `Authentication`. `@WithMockUser` chỉ set `SecurityContextHolder` nên không đủ —
  phải gán thẳng `.principal(...)` vào request (xem `UserControllerTest`).

## Gợi ý bài tập tiếp theo

1. Thêm bộ lọc khoảng giá (`minPrice`/`maxPrice`) — chỉ cần thêm method vào
   `ProductSpecifications` rồi `.and(...)`, không phải đụng vào repository.
