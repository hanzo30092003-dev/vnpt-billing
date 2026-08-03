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

## 4. Chạy ứng dụng

### 4.1. Chạy bình thường — giữ nguyên dữ liệu

```bash
mvnw spring-boot:run
```

Đây là cách chạy hằng ngày. `spring.sql.init.mode` mặc định là `never`, nên các
script `schema.sql` / `data-mau.sql` **không** chạy lại và mọi dữ liệu nhập qua giao
diện được giữ nguyên qua các lần khởi động.

Mở trình duyệt: <http://localhost:8080>

### 4.2. Nạp lại dữ liệu mẫu — profile `reset`

```bash
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

> ### ⚠️ CẢNH BÁO
> **Profile `reset` sẽ XOÁ TOÀN BỘ dữ liệu đang có trong CSDL.**
>
> Profile này bật `spring.sql.init.mode=always`, khiến `schema.sql` chạy lại. File đó
> mở đầu bằng `DROP TABLE IF EXISTS` cho cả 15 bảng, nên mọi khách hàng, thuê bao,
> giao dịch bạn đã nhập qua giao diện đều **mất sạch** và CSDL trở về đúng bộ dữ liệu
> mẫu ban đầu (50 khách hàng / 80 thuê bao / 5 gói cước).
>
> Chỉ dùng khi bạn **chủ đích** muốn làm mới CSDL, ví dụ trước khi demo hoặc khi dữ
> liệu thử nghiệm đã lộn xộn.

### 4.3. Chạy kiểm thử

```bash
mvnw test
```

### 4.4. Đóng gói và chạy độc lập

```bash
mvnw clean package
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

## 6. Trạng thái hiện tại — hết Phase 3

**Phase 0 — khung dự án** ✅

- [x] Khung Maven + Spring Boot chạy được
- [x] Cấu hình kết nối MySQL, JPA (`ddl-auto: none`), script khởi tạo CSDL
- [x] Layout Thymeleaf dùng chung: sidebar + header + vùng nội dung
- [x] Trang chủ, trang lỗi 404 / 500
- [x] Spring Security **tạm mở toàn bộ** (`permitAll`) để tiện phát triển

**Phase 1 — cơ sở dữ liệu** ✅

- [x] 15 bảng + 2 view trong `schema.sql`
- [x] 15 entity JPA + 16 enum
- [x] 15 repository Spring Data JPA
- [x] Dữ liệu mẫu: 50 khách hàng, 80 thuê bao, 5 gói cước, 10 dòng bảng giá

**Phase 2 — xác thực và nghiệp vụ đầu tiên** ✅

- [x] Tách profile `reset` để dữ liệu nhập qua giao diện không bị mất khi khởi động lại
- [x] `SchemaValidationTest` kiểm tra ánh xạ Entity ↔ CSDL tự động ở khâu build
- [x] Đăng nhập thật, BCrypt, CSRF, phân quyền 3 vai trò, trang 403
- [x] Quản lý khách hàng: danh sách/tìm kiếm/lọc, form đổi động, validation, xoá mềm
- [x] Quản lý thuê bao: đăng ký, chi tiết 4 tab, chuyển trạng thái theo ma trận,
      đổi gói cước, nạp tiền

Tài khoản dùng thử (mật khẩu đều là `123456`): `admin`, `nhanvien01`, `ketoan01`.

**Phase 3 — gói cước, bảng giá, CDR** ✅

- [x] Chuẩn hoá mã khách hàng 6 chữ số, tách `SinhMaService` có unit test
- [x] 16 unit test phủ kín ma trận chuyển trạng thái thuê bao
- [x] Script kiểm thử chuyển vào `scripts/`, xét mã trạng thái HTTP trước
- [x] Quản lý gói cước, chặn xoá gói đang có thuê bao dùng
- [x] Bảng giá theo thời gian, chặn chồng khoảng hiệu lực (9 unit test)
- [x] Bộ sinh CDR giả lập, 5000 bản ghi trong ~257 ms
- [x] Nhập CDR từ CSV, báo lỗi từng dòng
- [x] Tra cứu CDR, lọc, phân trang, xuất Excel
- [x] Quản lý kỳ cước

Chưa làm (thuộc các phase sau):

- [ ] Phase 4 — engine tính cước (Rating & Billing) — **mục 4A xong**, còn 4B–4G
- [ ] Phase 5 — hóa đơn, thanh toán, công nợ
- [ ] Phase 6 — báo cáo, thống kê, dashboard
- [ ] Phase 7 — hoàn thiện, kiểm thử, tài liệu

Tài liệu:

- [`docs/PHASE-0-REPORT.md`](docs/PHASE-0-REPORT.md) — báo cáo quá trình và kết quả Phase 0
- [`docs/PHASE-2-REPORT.md`](docs/PHASE-2-REPORT.md) — báo cáo Phase 2, kèm danh sách màn hình cần chụp ảnh
- [`docs/PHASE-3-REPORT.md`](docs/PHASE-3-REPORT.md) — báo cáo Phase 3, kèm 2 điểm sai lệch đặc tả và danh sách màn hình
- [`docs/PHASE-4-PLAN.md`](docs/PHASE-4-PLAN.md) — rà soát đầu vào Phase 4, 10 quyết định nghiệp vụ và tiêu chí nghiệm thu
- [`docs/PHASE-4-REPORT.md`](docs/PHASE-4-REPORT.md) — báo cáo Phase 4 (**mục 3: bài học phương pháp kiểm thử — kiểm bất biến thay vì kiểm luật**)
- [`docs/mo-ta-csdl.md`](docs/mo-ta-csdl.md) — mô tả chi tiết 15 bảng và 2 view (**mục 6: cảnh báo quy đổi đơn vị DATA cho Phase 4**)
- [`docs/toi-uu-hieu-nang.md`](docs/toi-uu-hieu-nang.md) — số đo tối ưu ghi hàng loạt CDR
- [`docs/mau-cdr.csv`](docs/mau-cdr.csv) — file CSV mẫu để thử chức năng nhập CDR
- [`scripts/README.md`](scripts/README.md) — script kiểm thử giao diện qua HTTP

> ⚠️ **CSDL bị dựng lại mỗi lần khởi động.** `application.yml` đặt
> `spring.sql.init.mode: always`, nên `schema.sql` (bắt đầu bằng `DROP TABLE IF EXISTS`)
> và `data-mau.sql` chạy lại ở mỗi lần chạy app. Mọi dữ liệu nhập tay sẽ mất.
> Chấp nhận được ở giai đoạn này vì toàn bộ là dữ liệu mẫu, nhưng **trước Phase 2**
> (khi bắt đầu có chức năng nhập liệu) phải chuyển sang `mode: never` hoặc dùng Flyway.

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
