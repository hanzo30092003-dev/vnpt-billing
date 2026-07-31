# BÁO CÁO PHASE 0 — DỰNG KHUNG DỰ ÁN

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Ngày thực hiện:** 31/07/2026
**Trạng thái:** Hoàn thành, còn 1 việc tồn đọng cần thao tác thủ công (mục 7)

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

Cấu hình này chạy được trên **cả JDK 21 lẫn JDK 25**, không cần cài thêm gì.

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
ngay khi khởi động. Cách xử lý: tạo hai file **chỉ chứa chú thích**, không có câu
lệnh SQL nào. Script chạy nhưng không thực thi gì, ứng dụng khởi động bình thường,
và vẫn đúng cam kết "chưa tạo bảng".

Đã ghi cảnh báo trong `README.md` để tránh bị xoá nhầm ở các phase sau.

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
| 2 | `mvn spring-boot:run` khởi động | ⚠️ Xem mục 6 | Đạt ở đường dẫn ASCII; lỗi ở đường dẫn có dấu |
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

### 5.4. Giới hạn của lần nghiệm thu này

Máy chưa cài MySQL (port 3306 đóng), nên quá trình chạy thử được thực hiện với tầng
CSDL bị loại tạm thời **thông qua biến môi trường, không chỉnh sửa file cấu hình nào**:

```
SPRING_AUTOCONFIGURE_EXCLUDE = DataSourceAutoConfiguration,
                               HibernateJpaAutoConfiguration,
                               DataSourceTransactionManagerAutoConfiguration
SPRING_SQL_INIT_MODE         = never
```

Do đó **việc khởi động đầy đủ với MySQL thật chưa được kiểm chứng**. Đây là hạng mục
cần xác nhận lại sau khi cài MySQL 8 và thay `CHANGE_ME` trong `application.yml`.

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

Đã thống nhất: **đổi tên `D:\HỌC` thành `D:\KGU`**, dự án sẽ nằm tại
`D:\KGU\TTNN\APP\vnpt-billing` — đường dẫn thuần ASCII.

Trong lúc chưa đổi tên, hai cách chạy vẫn hoạt động bình thường (đã kiểm chứng):

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

### 7.1. Đổi tên thư mục (chưa thực hiện được tự động)

Thao tác đổi tên `D:\HỌC` → `D:\KGU` **không thực hiện được từ trong phiên làm việc**
vì working directory của công cụ nằm tại `D:\HỌC\TTNN\APP`, mà Windows khoá mọi thư
mục tổ tiên của working directory.

Đã kiểm chứng chính xác quy luật này:

| Thư mục | Quan hệ với working directory | Kết quả đổi tên |
|---|---|---|
| `vnpt-billing` | thư mục **con** | ✅ Thành công |
| `D:\HỌC\TTNN` | thư mục **tổ tiên** | ❌ `Access denied` |
| `D:\HỌC` | thư mục **tổ tiên** | ❌ `Access denied` |

**Cách xử lý:** đóng Claude Code, đổi tên bằng File Explorer; hoặc mở PowerShell ở
ngoài thư mục đó rồi chạy:

```powershell
Rename-Item -LiteralPath "D:\HỌC" -NewName "KGU"
```

### 7.2. Cài đặt MySQL 8

Cần cài MySQL 8, đảm bảo chạy tại `localhost:3306`, và thay `CHANGE_ME` trong
`application.yml` bằng mật khẩu `root` thật. Không cần tạo database thủ công vì chuỗi
kết nối đã có `createDatabaseIfNotExist=true`.

Sau khi hoàn tất 7.1 và 7.2, cần chạy lại `mvnw spring-boot:run` để xác nhận tiêu chí
nghiệm thu số 2 trong điều kiện đầy đủ.

---

## 8. Kế hoạch các phase tiếp theo

| Phase | Nội dung |
|---|---|
| 1 | Thiết kế CSDL: bảng, entity JPA, repository, sinh dữ liệu mẫu |
| 2 | Bật Spring Security thật, đăng nhập và phân quyền theo vai trò |
| 3 | Nghiệp vụ danh mục: khách hàng, thuê bao, gói cước, bảng giá |
| 4 | Nghiệp vụ lõi: CDR, engine tính cước, hóa đơn, thanh toán |
| 5 | Báo cáo và biểu đồ doanh thu bằng Chart.js |

---

## 9. Tổng kết

Phase 0 đạt mục tiêu: bộ khung dự án hoàn chỉnh, biên dịch và đóng gói thành công,
giao diện chạy đúng với đầy đủ sidebar, header, trang chủ và hai trang lỗi.

Ba điểm lệch môi trường (thiếu JDK 21, thiếu Maven, thiếu MySQL) đều đã được xử lý
hoặc ghi nhận rõ ràng. Sự cố đường dẫn tiếng Việt đã được chẩn đoán tới nguyên nhân
gốc bằng thí nghiệm đối chứng, và có phương án khắc phục triệt để.

Hai việc tồn đọng ở mục 7 đều là thao tác cấu hình máy, không liên quan tới mã nguồn.
