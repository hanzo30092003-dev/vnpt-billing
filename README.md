# Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại

Đồ án môn **Thực tập nghề nghiệp**.

Hệ thống mô phỏng nghiệp vụ viễn thông: quản lý khách hàng, thuê bao, gói cước,
bảng giá, thu thập CDR (Call Detail Record), tính cước theo kỳ, lập hóa đơn,
ghi nhận thanh toán và báo cáo doanh thu.

> **Ghi chú về dữ liệu:** toàn bộ dữ liệu trong hệ thống là **dữ liệu mẫu tự sinh**
> phục vụ mục đích học tập. Hệ thống **không** sử dụng dữ liệu thật của bất kỳ
> nhà mạng nào. Tên "VNPT" chỉ dùng làm bối cảnh giả định cho đồ án.

---

## 1. Công nghệ sử dụng

| Thành phần | Phiên bản |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.16 |
| Spring Web MVC, Spring Data JPA, Spring Security, Spring Validation | theo Boot BOM |
| MySQL | 8.x |
| Thymeleaf | theo Boot BOM |
| Bootstrap | 5.3.3 (CDN) |
| Bootstrap Icons | 1.11.3 (CDN) |
| Chart.js | 4.4.2 (CDN) |
| Lombok | 1.18.46 |
| Build tool | Maven |

**Quy ước đặt tên:** tiếng Việt không dấu — `snake_case` cho bảng/cột CSDL,
`camelCase` cho Java, để dễ đối chiếu với báo cáo.

---

## 2. Yêu cầu môi trường

- **JDK 21** trở lên (dự án biên dịch ở mức bytecode Java 21)
- **MySQL 8** đang chạy tại `localhost:3306`
- **Maven 3.9+** — hoặc dùng Maven Wrapper kèm theo (`mvnw`), không cần cài Maven

Kiểm tra nhanh:

```bash
java -version
```

---

## 3. Cài đặt

### 3.1. Chuẩn bị CSDL

Không cần tạo database thủ công: chuỗi kết nối đã có `createDatabaseIfNotExist=true`
nên MySQL sẽ tự tạo schema `vnpt_billing` ở lần chạy đầu tiên.

### 3.2. Cấu hình trước khi chạy — khai báo mật khẩu MySQL

**Mật khẩu không nằm trong mã nguồn.** `application.yml` đọc thông tin đăng nhập từ
biến môi trường:

```yaml
spring:
  datasource:
    username: "${MYSQL_USER:root}"
    password: "${MYSQL_PASSWORD:}"
```

Cú pháp `${TEN_BIEN:giá_trị_mặc_định}` nghĩa là: lấy biến môi trường `TEN_BIEN`,
nếu chưa đặt thì dùng giá trị mặc định. Mặc định của `MYSQL_USER` là `root`, còn
`MYSQL_PASSWORD` mặc định là chuỗi rỗng.

Đặt biến môi trường (Windows, chạy một lần duy nhất):

```bash
setx MYSQL_PASSWORD "matkhau_root_cua_ban"
```

> ⚠️ **Bắt buộc mở lại terminal sau khi chạy `setx`.** Lệnh này ghi biến vào
> registry của người dùng, nhưng terminal đang mở vẫn giữ bản môi trường cũ nên
> chưa thấy biến mới. Đóng và mở lại PowerShell / Command Prompt / IntelliJ thì
> biến mới có hiệu lực. Kiểm tra bằng `echo %MYSQL_PASSWORD%` (CMD) hoặc
> `$env:MYSQL_PASSWORD` (PowerShell).

Nếu tài khoản MySQL không phải `root`, đặt thêm:

```bash
setx MYSQL_USER "ten_dang_nhap"
```

Cách tạm thời chỉ áp dụng cho phiên terminal hiện tại (không ghi vào registry):

```bash
$env:MYSQL_PASSWORD = "matkhau_root_cua_ban"
```

---

## 4. Chạy dự án

```bash
mvn clean compile
```

```bash
mvn spring-boot:run
```

Mở trình duyệt: <http://localhost:8080>

Đóng gói thành file JAR chạy độc lập:

```bash
mvn clean package
```

```bash
java -jar target/billing-0.0.1-SNAPSHOT.jar
```

---

## 5. Cấu trúc dự án

```
vnpt-billing/
├── pom.xml
├── README.md
└── src/main/
    ├── java/com/hanzo/billing/
    │   ├── BillingApplication.java      # lớp khởi động
    │   ├── config/                      # cấu hình (Security, MVC, ...)
    │   ├── controller/                  # tầng điều khiển (Spring MVC)
    │   ├── dto/                         # đối tượng truyền dữ liệu cho form/view
    │   ├── entity/                      # thực thể ánh xạ bảng CSDL (JPA)
    │   ├── enums/                        # các kiểu liệt kê nghiệp vụ
    │   ├── exception/                   # ngoại lệ và xử lý lỗi
    │   ├── repository/                  # tầng truy xuất dữ liệu (Spring Data JPA)
    │   ├── service/                     # giao diện nghiệp vụ
    │   │   ├── impl/                    # cài đặt nghiệp vụ
    │   │   └── rating/                  # thuật toán tính cước (rating engine)
    │   └── util/                        # tiện ích dùng chung
    └── resources/
        ├── application.yml
        ├── db/
        │   ├── schema.sql               # script tạo bảng
        │   └── data-mau.sql             # script chèn dữ liệu mẫu
        ├── static/css/app.css
        └── templates/
            ├── index.html               # trang chủ
            ├── error/{404,500}.html     # trang lỗi
            └── fragments/layout.html    # layout dùng chung
```

---

## 6. Trạng thái hiện tại — Phase 0

Đã hoàn thành khung dự án:

- [x] Khung Maven + Spring Boot chạy được
- [x] Cấu hình kết nối MySQL, JPA (`ddl-auto: none`), script khởi tạo CSDL
- [x] Layout Thymeleaf dùng chung: sidebar + header + vùng nội dung
- [x] Trang chủ, trang lỗi 404 / 500
- [x] Spring Security **tạm mở toàn bộ** (`permitAll`) để tiện phát triển

Chưa làm (thuộc các phase sau):

- [ ] Phase 1 — bảng CSDL, entity, repository, dữ liệu mẫu
- [ ] Phase 2 — bật đăng nhập và phân quyền theo vai trò
- [ ] Phase 3 — nghiệp vụ khách hàng / thuê bao / gói cước / bảng giá
- [ ] Phase 4 — CDR, engine tính cước, hóa đơn, thanh toán
- [ ] Phase 5 — báo cáo và biểu đồ (Chart.js)

Báo cáo chi tiết quá trình và kết quả: [`docs/PHASE-0-REPORT.md`](docs/PHASE-0-REPORT.md)

> **Về hai file `src/main/resources/db/schema.sql` và `data-mau.sql`:** hiện mỗi file
> chỉ có chú thích kèm một câu lệnh no-op `SELECT 1;`.
>
> - Hai file **bắt buộc phải tồn tại** vì `application.yml` trỏ tới chúng — nếu xoá,
>   ứng dụng báo `No SQL scripts found at location` khi khởi động.
> - Câu `SELECT 1;` **không được xoá** khi file chưa có lệnh SQL nào khác. Spring bóc
>   hết chú thích trước khi chạy script, nên file chỉ có chú thích sẽ thành chuỗi rỗng
>   và gây lỗi `'script' must not be null or empty`.
> - Phase 1 sẽ xoá `SELECT 1;` khi thêm các lệnh `CREATE TABLE` và `INSERT` thật.

---

## 7. Khắc phục sự cố

### `spring-boot:run` báo `Could not find or load main class`

**Triệu chứng:** `mvn clean compile` và `mvn package` chạy bình thường, nhưng
`mvn spring-boot:run` báo lỗi:

```
Error: Could not find or load main class com.hanzo.billing.BillingApplication
```

**Nguyên nhân:** đường dẫn dự án có chứa ký tự tiếng Việt có dấu (ví dụ `D:\HỌC\...`).
Spring Boot Maven plugin ghi classpath vào một *argfile* rồi truyền cho JVM con.
Trình khởi động `java` đọc argfile bằng bảng mã ANSI của Windows (`windows-1258`)
**trước khi** máy ảo khởi động, nên ký tự `Ọ` bị biến thành `?` và classpath hỏng.
Không có tham số `-D...` nào sửa được vì lỗi xảy ra trước khi JVM đọc tham số.

**Cách khắc phục (chọn 1):**

1. **Đặt dự án ở đường dẫn không dấu** — cách triệt để, mọi lệnh Maven đều chạy.
   Ví dụ `D:\HOC\TTNN\APP\vnpt-billing` hoặc `D:\TTNN\vnpt-billing`.
2. **Giữ nguyên đường dẫn**, chạy bằng file JAR (đã kiểm chứng là chạy tốt):

   ```bash
   mvnw package
   ```

   ```bash
   java -jar target/billing-0.0.1-SNAPSHOT.jar
   ```

3. **Chạy trực tiếp từ IntelliJ IDEA** (nút Run trên `BillingApplication`) —
   IntelliJ tự dựng dòng lệnh nên không đi qua argfile của Maven plugin.

### Ứng dụng không khởi động được vì chưa có MySQL

Ứng dụng cần MySQL đang chạy tại `localhost:3306`. Nếu chỉ muốn xem giao diện mà
chưa cài MySQL, có thể tạm bỏ qua tầng CSDL bằng biến môi trường:

```bash
java -Dspring.sql.init.mode=never -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration -jar target/billing-0.0.1-SNAPSHOT.jar
```

### Log hiện dòng `Using generated security password: ...`

Đây là thông báo bình thường của Spring Security khi chưa cấu hình người dùng.
Phase 0 đang để `permitAll` nên mật khẩu này không dùng đến. Phase 2 sẽ thay bằng
đăng nhập thật và thông báo sẽ tự mất.

---

## 8. Lưu ý bảo mật khi phát triển

`SecurityConfig` ở Phase 0 đang mở toàn bộ endpoint và tắt CSRF.
**Không** triển khai cấu hình này ra môi trường thật — Phase 2 sẽ thay bằng
đăng nhập và phân quyền đầy đủ.
