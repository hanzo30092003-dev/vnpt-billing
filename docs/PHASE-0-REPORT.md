# BÁO CÁO PHASE 0 — DỰNG KHUNG DỰ ÁN

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Ngày thực hiện:** 31/07/2026
**Trạng thái:** ✅ Hoàn thành — đã nghiệm thu đủ 4/4 tiêu chí với MySQL thật

> Toàn bộ dữ liệu trong hệ thống là dữ liệu mẫu tự sinh phục vụ học tập.
> Hệ thống không sử dụng dữ liệu thật của bất kỳ nhà mạng nào.

---

## 1. Mục tiêu Phase 0

Dựng bộ khung dự án chạy được, **chưa có nghiệp vụ**:

- Project Maven + Spring Boot khởi động thành công
- Cấu hình kết nối MySQL, JPA, script khởi tạo CSDL
- Layout Thymeleaf dùng chung (sidebar + header + vùng nội dung)
- Trang chủ, trang lỗi 404/500
- Spring Security tạm mở toàn bộ để tiện phát triển

Phạm vi loại trừ: không tạo entity, không tạo bảng CSDL, không viết nghiệp vụ.

---

## 2. Khảo sát môi trường thực tế

Trước khi viết code, môi trường máy được kiểm tra và phát hiện **3 điểm lệch** so với yêu cầu công nghệ:

| Hạng mục | Yêu cầu | Thực tế trên máy | Kết luận |
|---|---|---|---|
| Java | 21 | **Temurin JDK 25.0.3**, không có JDK 21 | Cần xử lý |
| Spring Boot | 3.3+ | — | Cần chọn phiên bản cụ thể |
| Maven | có | **Chưa cài**, không có trên PATH | Cần xử lý |
| MySQL 8 | đang chạy | **Port 3306 đóng**, không có service | Cần cài đặt |

Thông tin môi trường bổ sung (ảnh hưởng tới mục 6):

```
Default locale   : vi_VN
Platform encoding: UTF-8
Console codepage : 65001 (UTF-8)
ANSI codepage    : windows-1258   <-- điểm mấu chốt
OS               : Windows 11, amd64
```

---

## 3. Các quyết định kỹ thuật và lý do

### 3.1. Spring Boot 3.5.16

Yêu cầu ghi "3.3+" nên cần chốt một phiên bản cụ thể. Tra cứu danh sách phát hành
chính thức cho thấy **3.5.16 (25/06/2026)** là bản mới nhất của nhánh 3.x.

Lưu ý: Spring Initializr hiện chỉ còn phát hành nhánh 4.x. Vẫn chọn 3.x vì đây là
nhánh mà phần lớn tài liệu tham khảo và giáo trình đang bám theo, thuận lợi khi
trình bày trước hội đồng.

### 3.2. Giữ JDK 25, biên dịch ở mức Java 21

Thay vì cài thêm JDK 21, `pom.xml` khai báo:

```xml
<java.version>21</java.version>
<lombok.version>1.18.46</lombok.version>
```

- `java.version=21` khiến Spring Boot parent đặt `maven.compiler.release=21`, nên
  javac của JDK 25 biên dịch ra **bytecode đúng chuẩn Java 21**. Log build xác nhận:
  `Compiling 3 source files with javac [debug parameters release 21]`.
- Lombok được nâng lên `1.18.46` vì Lombok chạy như annotation processor bên trong
  javac, phải hỗ trợ nội bộ của JDK đang dùng. Bản 1.18.40 mới thêm hỗ trợ JDK 25,
  1.18.46 hỗ trợ tới JDK 26. Bản Lombok mặc định của Boot 3.5.16 có thể chưa đủ mới.

Về nguyên tắc tương thích ngược của Java, cấu hình này chạy được trên **JDK 21 trở lên**.
Tuy nhiên trong khuôn khổ đồ án **mới chỉ kiểm chứng thực tế trên JDK 25**, vì máy phát
triển không cài JDK 21 nên chưa có điều kiện chạy thử để khẳng định.

### 3.3. Maven Wrapper thay vì cài Maven

Dự án được bổ sung `mvnw`, `mvnw.cmd` và `.mvn/wrapper/maven-wrapper.properties`
(Maven Wrapper 3.3.4, kiểu `only-script`, trỏ tới Maven 3.9.16).

Lợi ích: không cần cài Maven vào hệ thống, và **máy nào clone dự án về cũng build
được ngay** — kể cả máy chấm điểm chưa cài Maven.

### 3.4. Hai file SQL rỗng — giải quyết mâu thuẫn trong đặc tả

Đặc tả có hai yêu cầu xung đột nhau:

- Yêu cầu 3: `application.yml` phải trỏ tới `classpath:db/schema.sql` và `classpath:db/data-mau.sql`
- Phần Lưu ý: **không** tạo bảng CSDL ở Phase 0

Nếu hai file không tồn tại, Spring Boot ném lỗi `No SQL scripts found at location`
ngay khi khởi động. Nên hai file **bắt buộc phải tồn tại**.

Phương án ban đầu là để hai file **chỉ chứa chú thích**. Phương án này **sai** và đã
bị phát hiện khi nghiệm thu với MySQL thật — chi tiết ở mục 5.4. Cách làm đúng: mỗi
file phải chứa **ít nhất một câu lệnh SQL hợp lệ**, ở đây dùng `SELECT 1;` làm no-op:

```sql
-- (chú thích giải thích)
SELECT 1;
```

`SELECT 1` không tạo bảng, không đổi dữ liệu, nên vẫn đúng cam kết "chưa tạo bảng"
của Phase 0. Phase 1 sẽ xoá dòng này khi thêm các lệnh `CREATE TABLE` và `INSERT`
thật. Đã ghi cảnh báo trong `README.md` để hai file không bị xoá nhầm.

### 3.5. Spring Security mở toàn bộ

`SecurityConfig` dùng `anyRequest().permitAll()`, đồng thời tắt `formLogin`,
`httpBasic`, `logout` và CSRF — vì Phase 0 chưa có form nghiệp vụ nào.
Đã ghi chú rõ trong code và README rằng cấu hình này **chỉ dùng cho phát triển**
và sẽ được thay ở Phase 2.

---

## 4. Cấu trúc dự án

```
vnpt-billing/
├── .mvn/wrapper/maven-wrapper.properties
├── mvnw
├── mvnw.cmd
├── pom.xml
├── .gitignore
├── README.md
├── docs/
│   └── PHASE-0-REPORT.md
└── src/
    ├── main/
    │   ├── java/com/hanzo/billing/
    │   │   ├── BillingApplication.java     # lớp khởi động
    │   │   ├── config/SecurityConfig.java
    │   │   ├── controller/HomeController.java
    │   │   ├── dto/            (.gitkeep)
    │   │   ├── entity/         (.gitkeep)
    │   │   ├── enums/          (.gitkeep)
    │   │   ├── exception/      (.gitkeep)
    │   │   ├── repository/     (.gitkeep)
    │   │   ├── service/        (.gitkeep)
    │   │   │   ├── impl/       (.gitkeep)
    │   │   │   └── rating/     (.gitkeep)
    │   │   └── util/           (.gitkeep)
    │   └── resources/
    │       ├── application.yml
    │       ├── db/schema.sql
    │       ├── db/data-mau.sql
    │       ├── static/css/app.css
    │       └── templates/
    │           ├── index.html
    │           ├── error/404.html
    │           ├── error/500.html
    │           └── fragments/layout.html
    └── test/java/com/hanzo/billing/
```

Tổng cộng: **3 lớp Java**, **4 template Thymeleaf**, **1 file CSS**, **2 script SQL**.

---

## 5. Kết quả nghiệm thu

### 5.1. Đối chiếu tiêu chí

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvn clean compile` không lỗi | ✅ Đạt | `BUILD SUCCESS`, `javac [debug parameters release 21]`, 18.1s |
| 2 | `mvn spring-boot:run` khởi động | ✅ Đạt | `Started BillingApplication in 2.059 seconds`, với MySQL thật — xem mục 5.4 |
| 3 | localhost:8080 có sidebar + header | ✅ Đạt | HTTP 200, 3596 bytes |
| 4 | Không có exception trong log | ✅ Đạt | Chỉ 1 WARN chuẩn của Spring Security |

### 5.2. Các kiểm thử bổ sung

| Hạng mục | Kết quả |
|---|---|
| `mvn package` (đóng gói JAR) | ✅ `BUILD SUCCESS`, tạo `billing-0.0.1-SNAPSHOT.jar` |
| `java -jar` chạy JAR | ✅ HTTP 200, trang chủ render đúng |
| Thời gian khởi động | 1.363 giây (`Started BillingApplication`) |
| 11 mục menu sidebar | ✅ Đủ: Trang chủ, Khách hàng, Thuê bao, Gói cước, Bảng giá, CDR, Tính cước, Hóa đơn, Thanh toán, Báo cáo, Quản trị |
| Tiêu đề header | ✅ "HỆ THỐNG QUẢN LÝ THUÊ BAO & TÍNH CƯỚC" |
| Ghi chú dữ liệu mẫu | ✅ "Dữ liệu trong hệ thống là dữ liệu mẫu phục vụ học tập" |
| Layout render sạch | ✅ Không còn sót cú pháp `th:*` trong HTML trả về |
| Trang 404 | ✅ Status 404, render đúng template kèm layout (không rơi vào Whitelabel) |
| Trang 500 | ✅ Status 500, render đúng template kèm layout |
| `/css/app.css` | ✅ HTTP 200, 2409 bytes |
| 4 link CDN (Bootstrap CSS/JS, Bootstrap Icons, Chart.js) | ✅ Cả 4 đều HTTP 200 |

**Lưu ý về trang lỗi:** khi kiểm thử bằng `Invoke-WebRequest` mặc định, trang 404 trả
về JSON chứ không phải HTML. Đây **không phải lỗi**: `BasicErrorController` của Spring
Boot phân nhánh theo header `Accept`. Khi gửi `Accept: text/html` (giống trình duyệt),
template `error/404.html` được render đúng. Bài học: phải kiểm thử trang lỗi bằng
header giống trình duyệt thật.

### 5.3. WARN duy nhất trong log

```
WARN .s.s.UserDetailsServiceAutoConfiguration :
Using generated security password: 1e4748c3-f4f8-4610-a161-b42e0647f369
```

Đây là thông báo mặc định của Spring Security khi chưa khai báo người dùng. Phase 0
đang để `permitAll` nên mật khẩu này không được dùng tới. Thông báo sẽ tự mất ở
Phase 2 khi cấu hình đăng nhập thật. **Không phải exception.**

### 5.4. Nghiệm thu bổ sung với MySQL thật

Ở lần nghiệm thu đầu, máy chưa cài MySQL nên phải chạy thử với tầng CSDL bị loại tạm
thời qua biến môi trường (`SPRING_AUTOCONFIGURE_EXCLUDE`, `SPRING_SQL_INIT_MODE`),
và báo cáo đã ghi rõ **việc khởi động đầy đủ với MySQL thật chưa được kiểm chứng**.

Sau khi cài MySQL 8.4.9, lần nghiệm thu đầy đủ đã được thực hiện tại đúng đường dẫn
`D:\KGU\TTNN\APP\vnpt-billing`, **không dùng bất kỳ biến loại trừ nào**.

#### Lỗi phát hiện được — hai file SQL chỉ chứa chú thích

Lần chạy đầu tiên với DataSource thật **thất bại**:

```
BeanCreationException: Error creating bean with name 'dataSourceScriptDatabaseInitializer'
  Failed to execute database script from resource [class path resource [db/schema.sql]]
Caused by: java.lang.IllegalArgumentException: 'script' must not be null or empty
    at org.springframework.jdbc.datasource.init.ScriptUtils.splitSqlScript
```

Nguyên nhân: `ScriptUtils` **bóc bỏ toàn bộ chú thích trước khi thực thi**. Hai file
chỉ có chú thích nên phần còn lại là chuỗi rỗng, và `Assert.hasText` ném lỗi.

Giả định ban đầu "file chỉ chứa chú thích sẽ chạy như một no-op" là **sai**. Nó không
bị phát hiện ở lần nghiệm thu đầu vì đường dẫn khởi tạo CSDL đã bị vô hiệu hoá — đúng
là hạng mục mà báo cáo đã đánh dấu là chưa kiểm chứng. Đây là ví dụ cho thấy vì sao
phải ghi rõ giới hạn của phép thử thay vì tuyên bố đạt.

Đã sửa bằng cách thêm `SELECT 1;` vào mỗi file (xem mục 3.4).

#### Cảnh báo Hibernate được dọn

Khi JPA khởi tạo thật, log xuất hiện thêm cảnh báo:

```
WARN org.hibernate.orm.deprecation : HHH90000025: MySQLDialect does not need to be
specified explicitly using 'hibernate.dialect'
```

Dòng `hibernate.dialect` trong `application.yml` là thừa vì Hibernate tự nhận diện
MySQL qua driver. Đã xoá, cảnh báo biến mất.

#### Kết quả nghiệm thu đầy đủ

| Hạng mục kiểm tra | Kết quả |
|---|---|
| `ClassNotFoundException` | ✅ Không còn — xác nhận việc đổi tên thư mục đã xử lý triệt để sự cố argfile ở mục 6 |
| HikariCP | ✅ `HikariPool-1 - Start completed` |
| Tự tạo database | ✅ `vnpt_billing` được tạo nhờ `createDatabaseIfNotExist=true` (trước đó chưa tồn tại) |
| Khởi động | ✅ `Started BillingApplication in 2.059 seconds` |
| `GET /` | ✅ 200, đủ 11/11 mục menu, có sidebar, header và ghi chú dữ liệu mẫu |
| `GET /duong-dan-khong-ton-tai` | ✅ 404, render `error/404.html` kèm layout |
| `GET /css/app.css` | ✅ 200, 2409 bytes |
| Khởi động lần thứ hai liên tiếp | ✅ `Started BillingApplication in 2.133 seconds` |
| Exception trong log | ✅ Không có |
| Số bảng trong `vnpt_billing` sau 2 lần chạy | ✅ **0 bảng** — đúng cam kết Phase 0 chưa tạo bảng |

Ghi chú về cách lấy mật khẩu khi nghiệm thu: `setx` ghi biến vào registry người dùng,
nhưng tiến trình đang mở giữ bản môi trường cũ nên không thấy biến mới. Giá trị được
đọc trực tiếp từ `HKCU:\Environment` rồi nạp vào tiến trình con — đúng cơ chế mà một
terminal mới dùng để lấy biến, nên kết quả phản ánh trung thực.

---

## 6. Sự cố kỹ thuật: đường dẫn có dấu tiếng Việt

Đây là vấn đề đáng chú ý nhất của Phase 0, xin trình bày đầy đủ cả quá trình chẩn đoán.

### 6.1. Hiện tượng

Dự án đặt tại `D:\HỌC\TTNN\APP\vnpt-billing`.

- `mvn clean compile` → thành công
- `mvn package` → thành công
- `mvn spring-boot:run` → **thất bại**:

```
Error: Could not find or load main class com.hanzo.billing.BillingApplication
Caused by: java.lang.ClassNotFoundException: com.hanzo.billing.BillingApplication
```

### 6.2. Quá trình chẩn đoán

| Bước | Việc làm | Kết quả |
|---|---|---|
| 1 | Đọc kỹ log build | Phát hiện dòng `skip non existing resourceDirectory D:\H?C\TTNN\APP\...` — ký tự `Ọ` biến thành `?` |
| 2 | Kiểm tra `target/classes` | 3 file `.class` tồn tại đầy đủ → **không phải lỗi biên dịch** |
| 3 | Thí nghiệm đối chứng: copy nguyên dự án sang đường dẫn thuần ASCII rồi chạy lại | **Khởi động thành công trong 1.363 giây** → xác nhận nguyên nhân là đường dẫn |
| 4 | Thử `java -jar` tại chính đường dẫn có dấu | **Chạy được**, trang chủ render đúng |
| 5 | Thử `mvn spring-boot:run -Dspring-boot.run.fork=false` | Vẫn thất bại |

### 6.3. Phân tích nguyên nhân

Bằng chứng thu được (bước 1–4) khẳng định lỗi phát sinh ở **ranh giới fork tiến trình**:
biên dịch và đóng gói (I/O thuần Java) đều bình thường, chỉ hỏng khi Maven plugin
khởi tạo một JVM con.

Cơ chế cụ thể: `spring-boot-maven-plugin` fork một JVM mới và truyền classpath qua
*argfile*. Trình khởi động `java` đọc argfile bằng **ANSI codepage của hệ điều hành
(`windows-1258`)**, và việc này diễn ra **trước khi máy ảo khởi động** — nên không có
tham số `-Dfile.encoding` hay `-Dsun.jnu.encoding` nào can thiệp được. Ký tự `Ọ`
(U+1ECC) không biểu diễn được trong bảng mã đó nên bị thay bằng `?`, classpath trỏ
sai, JVM con không tìm thấy `target/classes`.

Điều này giải thích vì sao bước 5 (`fork=false`) cũng không cứu được: DevTools trên
classpath vẫn buộc plugin fork tiến trình.

### 6.4. Hướng xử lý

Phương án đã chọn: **đổi tên `D:\HỌC` thành `D:\KGU`**, đưa dự án về
`D:\KGU\TTNN\APP\vnpt-billing` — đường dẫn thuần ASCII.

> ✅ **Đã thực hiện xong và đã kiểm chứng.** Dự án hiện nằm tại
> `D:\KGU\TTNN\APP\vnpt-billing`. `mvnw spring-boot:run` chạy tại đường dẫn mới đã
> khởi động thành công, không còn `ClassNotFoundException` — xem mục 5.4.

Hai cách chạy dưới đây là phương án dự phòng khi buộc phải giữ đường dẫn có dấu
(đã kiểm chứng hoạt động bình thường):

```bash
mvnw package
```

```bash
java -jar target/billing-0.0.1-SNAPSHOT.jar
```

Hoặc bấm Run trực tiếp trong IntelliJ IDEA — IntelliJ tự dựng dòng lệnh, không đi
qua argfile của Maven plugin.

---

## 7. Việc tồn đọng

Trạng thái cập nhật sau phiên làm việc ngày 31/07/2026.

### 7.1. Đổi tên thư mục — ✅ ĐÃ XONG

Ban đầu thao tác này **không thực hiện được từ trong phiên làm việc** vì working
directory của công cụ nằm tại `D:\HỌC\TTNN\APP`, mà Windows khoá mọi thư mục tổ tiên
của working directory. Quy luật đã được kiểm chứng chính xác:

| Thư mục | Quan hệ với working directory | Kết quả đổi tên |
|---|---|---|
| `vnpt-billing` | thư mục **con** | ✅ Thành công |
| `D:\HỌC\TTNN` | thư mục **tổ tiên** | ❌ `Access denied` |
| `D:\HỌC` | thư mục **tổ tiên** | ❌ `Access denied` |

Đã xử lý bằng cách đổi tên thủ công ở ngoài phiên làm việc. Dự án hiện nằm tại
`D:\KGU\TTNN\APP\vnpt-billing`, thư mục cũ không còn tồn tại.

### 7.2. Khởi tạo Git — ✅ ĐÃ XONG

- `git init -b main` tại thư mục gốc dự án
- Rà soát `.gitignore`: kiểm chứng **16 quy tắc** bằng `git check-ignore`, đảm bảo
  `target/`, `.idea/`, `*.iml`, `.vscode/`, `*.log`, `.env`, `application-local.yml`,
  `.mvn/wrapper/maven-wrapper.jar` bị loại; đồng thời `mvnw`, `mvnw.cmd`,
  `maven-wrapper.properties` và `docs/` **không** bị loại
- Commit đầu tiên `3930963` — 27 file, 1622 dòng, không lọt file nào trong `target/`
- Kiểm tra ký tự xuống dòng: `core.autocrlf=true` hoạt động đúng — `mvnw` (script
  bash) lưu LF trong repo nên chạy được trên Linux/macOS, còn `mvnw.cmd` được trả về
  CRLF khi checkout trên Windows

### 7.3. Bảo mật cấu hình — ✅ ĐÃ XONG

`application.yml` không còn chứa mật khẩu dạng văn bản thuần. Thông tin đăng nhập
CSDL được đọc từ biến môi trường:

```yaml
username: "${MYSQL_USER:root}"
password: "${MYSQL_PASSWORD:}"
```

Nhờ vậy mã nguồn đẩy lên GitHub không mang theo mật khẩu. `.gitignore` cũng đã chặn
`.env`, `.env.*`, `application-local.yml` và `application-local.properties`.
Hướng dẫn đặt biến môi trường bằng `setx` đã được bổ sung vào `README.md` mục 3.2.

### 7.4. Cài đặt MySQL 8 — ✅ ĐÃ XONG

| Hạng mục | Giá trị |
|---|---|
| Phiên bản | MySQL 8.4.9 |
| Service | `MySQL84`, Status **Running**, StartType **Automatic** |
| Cổng | 3306 mở |
| Thư mục dữ liệu | `C:\ProgramData\MySQL\MySQL Server 8.4\Data` |
| Xác thực | Strong Password Encryption (`caching_sha2_password`) |

Một điểm dễ nhầm khi cài: lệnh `winget install Oracle.MySQL` chỉ chạy bộ cài MSI, nó
**không** khởi tạo thư mục dữ liệu và **không** tạo Windows service. Sau bước này
`winget list` đã báo "MySQL Server 8.4" nhưng máy vẫn chưa có service nào và port
3306 vẫn đóng. Phải chạy thêm `mysql_configurator.exe` với quyền quản trị thì server
mới thực sự hoạt động.

Lo ngại về lỗi `Public Key Retrieval is not allowed` khi dùng `caching_sha2_password`
đã **không xảy ra**, vì MySQL 8 bật sẵn TLS và Connector/J mặc định `sslMode=PREFERRED`
nên quá trình bắt tay diễn ra trên kênh mã hoá. Chuỗi JDBC giữ nguyên, không cần thêm
`allowPublicKeyRetrieval`.

### 7.5. Đẩy mã nguồn lên GitHub — ✅ ĐÃ XONG

Repository riêng tư đã được tạo và đồng bộ:

| Hạng mục | Giá trị |
|---|---|
| URL | `https://github.com/hanzo30092003-dev/vnpt-billing` |
| Chế độ | **Private** |
| Nhánh mặc định | `main` |
| Số file trên remote | 27 — khớp với repo cục bộ, 0 file trong `target/` |
| Số commit | 2 |

Hai điểm vướng gặp phải khi cài đặt, ghi lại để tham khảo:

1. Sau khi cài GitHub CLI, terminal đang mở vẫn báo `gh is not recognized`. Nguyên
   nhân: tiến trình terminal giữ bản `PATH` chụp lúc khởi động, không tự cập nhật khi
   có phần mềm mới cài. Phải mở lại cửa sổ terminal, hoặc gọi bằng đường dẫn đầy đủ.
2. Đã đăng nhập github.com trên trình duyệt nhưng `gh auth status` vẫn báo chưa đăng
   nhập. Nguyên nhân: CLI lưu token riêng trên máy, độc lập hoàn toàn với phiên đăng
   nhập của trình duyệt — vẫn phải chạy `gh auth login`.

---

## 8. Kế hoạch các phase tiếp theo

Dự án gồm 8 phase (Phase 0–7). Phase 0 là phần đã trình bày trong báo cáo này.

| Phase | Nội dung |
|---|---|
| 1 | Thiết kế CSDL 15 bảng, entity JPA, repository, dữ liệu mẫu |
| 2 | Bật Spring Security thật; quản lý khách hàng và thuê bao |
| 3 | Gói cước, bảng giá cước, CDR (bộ sinh giả lập + import CSV) |
| 4 | Engine tính cước — Rating và Billing (phần lõi) |
| 5 | Hóa đơn, thanh toán, công nợ |
| 6 | Báo cáo, thống kê, dashboard |
| 7 | Hoàn thiện, kiểm thử, tài liệu |

---

## 9. Tổng kết

Phase 0 đạt mục tiêu và **đã nghiệm thu đủ 4/4 tiêu chí**: bộ khung dự án hoàn chỉnh,
biên dịch và đóng gói thành công, khởi động đầy đủ với MySQL 8.4.9 thật tại đúng
đường dẫn dự án, giao diện chạy đúng với đầy đủ sidebar, header, trang chủ và hai
trang lỗi.

Ba điểm lệch môi trường (thiếu JDK 21, thiếu Maven, thiếu MySQL) đều đã được xử lý
hoặc ghi nhận rõ ràng. Sự cố đường dẫn tiếng Việt đã được chẩn đoán tới nguyên nhân
gốc bằng thí nghiệm đối chứng và **đã khắc phục triệt để** bằng việc đổi tên thư mục.

Mã nguồn đã được đưa vào quản lý phiên bản bằng Git, đẩy lên GitHub ở chế độ riêng
tư, và mật khẩu CSDL đã được tách khỏi mã nguồn.

Hai lỗi thật đã được phát hiện và khắc phục trong Phase 0, cả hai đều chỉ lộ ra khi
chạy thử trong điều kiện đầy đủ chứ không thể thấy bằng cách đọc mã:

1. **Đường dẫn có dấu tiếng Việt** làm hỏng classpath khi Maven fork JVM con (mục 6)
2. **Hai file SQL chỉ chứa chú thích** khiến `ScriptUtils` ném lỗi chuỗi rỗng (mục 5.4)

Lỗi thứ hai đáng chú ý về mặt phương pháp: nó nằm đúng trong phần mà báo cáo đã đánh
dấu là "chưa kiểm chứng" ở lần nghiệm thu đầu. Nếu khi đó tuyên bố đạt thay vì ghi rõ
giới hạn, lỗi sẽ trôi sang Phase 1 và khó truy nguyên hơn nhiều.

Phase 0 khép lại, không còn việc tồn đọng. Sẵn sàng bước sang Phase 1.
