# Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại

[![test](https://github.com/hanzo30092003-dev/vnpt-billing/actions/workflows/test.yml/badge.svg)](https://github.com/hanzo30092003-dev/vnpt-billing/actions/workflows/test.yml)

Đồ án môn **Thực tập nghề nghiệp**.

Hệ thống mô phỏng nghiệp vụ viễn thông: quản lý khách hàng, thuê bao, gói cước, bảng giá,
thu thập CDR (Call Detail Record), tính cước theo kỳ, lập hóa đơn, ghi nhận thanh toán, theo
dõi công nợ và báo cáo doanh thu.

> ### ⚠️ Ghi chú về dữ liệu
> Toàn bộ dữ liệu trong hệ thống là **dữ liệu mẫu tự sinh** phục vụ mục đích học tập.
> Hệ thống **không** sử dụng dữ liệu thật của bất kỳ nhà mạng nào. Tên "VNPT" chỉ dùng làm
> bối cảnh giả định cho đồ án. Tên khách hàng, số CCCD, mã số thuế và số điện thoại đều do
> bộ sinh dữ liệu tạo ra, không tương ứng với người hay tổ chức nào có thật.

---

## 1. Màn hình chính

| Màn hình | Đường dẫn | Nội dung |
|---|---|---|
| **Bảng điều khiển** | `/` | 6 thẻ số liệu, biểu đồ doanh thu 6 kỳ, cơ cấu dịch vụ, tuổi nợ |
| **Bảng đối soát cước** | `/tinh-cuoc/doi-soat/{tb}/{ky}` | Vì sao hóa đơn ra con số đó — từ CDR thô tới từng cột hóa đơn |
| **Điều khiển tính cước** | `/tinh-cuoc` | Tính cước · lập hóa đơn · trừ cước trả trước · chốt kỳ, kèm đường lùi |
| **Công nợ** | `/cong-no` | Bảng tuổi nợ 5 nhóm, danh sách nợ, đề xuất tạm ngừng |
| **Báo cáo thống kê** | `/bao-cao` | 7 báo cáo, mỗi báo cáo xuất Excel và in được |

Danh sách đầy đủ 69 màn hình cần chụp cho báo cáo: [`docs/danh-sach-anh-chup.md`](docs/danh-sach-anh-chup.md).

---

## 2. Công nghệ sử dụng

| Thành phần | Phiên bản |
|---|---|
| Java | biên dịch ở mức `release 21` (máy dev chạy JDK 25) |
| Spring Boot | 3.5.16 |
| Spring Web MVC · Data JPA · Security · Validation | theo Boot BOM |
| MySQL | 8.4 |
| Thymeleaf | 3.1.5 (theo Boot BOM) |
| Bootstrap · Bootstrap Icons | 5.3.3 · 1.11.3 (CDN) |
| Chart.js | 4.4.2 (CDN) |
| Apache POI (xuất Excel) | 5.4.1 |
| OpenPDF (xuất PDF) | 2.0.3 |
| Lombok | 1.18.46 |
| Build | Maven Wrapper (`mvnw`) |

**Quy ước đặt tên:** tiếng Việt không dấu — `snake_case` cho bảng/cột CSDL, `camelCase` cho
Java, để dễ đối chiếu với báo cáo.

---

## 3. Yêu cầu môi trường

- **JDK 21** trở lên
- **MySQL 8** đang chạy tại `localhost:3306`
- **Maven** — không cần cài, dùng Maven Wrapper (`mvnw`) kèm theo
- Đường dẫn dự án phải **thuần ASCII** (xem mục 8)

```bash
java -version
```

---

## 4. Cài đặt từng bước

### Bước 1 — Lấy mã nguồn

```bash
git clone <đường-dẫn-repo> vnpt-billing
```

### Bước 2 — Khai báo mật khẩu MySQL

**Mật khẩu không nằm trong mã nguồn.** `application.yml` đọc từ biến môi trường:

```yaml
spring:
  datasource:
    username: "${MYSQL_USER:root}"
    password: "${MYSQL_PASSWORD:}"
```

Cú pháp `${TEN_BIEN:mặc_định}` nghĩa là lấy biến môi trường, chưa đặt thì dùng mặc định.

Đặt vĩnh viễn (Windows):

```bash
setx MYSQL_PASSWORD "matkhau_root_cua_ban"
```

> ⚠️ **Bắt buộc mở lại terminal sau `setx`.** Lệnh này ghi vào registry, terminal đang mở vẫn
> giữ bản môi trường cũ. Kiểm bằng `$env:MYSQL_PASSWORD` (PowerShell).

Chỉ cho phiên hiện tại:

```bash
$env:MYSQL_PASSWORD = "matkhau_root_cua_ban"
```

Tài khoản không phải `root` thì đặt thêm `MYSQL_USER`.

### Bước 3 — Nạp CSDL lần đầu

Không cần tạo database thủ công — chuỗi kết nối có `createDatabaseIfNotExist=true`. Chạy một
lần với profile `reset` để tạo bảng và nạp dữ liệu mẫu:

```bash
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

Đợi log hiện `Started BillingApplication`, rồi **dừng lại** (`Ctrl+C`).

### Bước 4 — Chạy ứng dụng

```bash
mvnw spring-boot:run
```

Mở <http://localhost:8080>.

> Từ bước 4 trở đi, Flyway chỉ chạy những file di trú **chưa từng chạy** trên CSDL này, nên
> dữ liệu **được giữ nguyên** qua mọi lần khởi động lại. Chỉ chạy lại profile `reset` khi chủ
> đích muốn về dữ liệu mẫu gốc.

---

## 5. Ba tài khoản demo

Mật khẩu đều là `123456`.

| Tài khoản | Vai trò | Phạm vi |
|---|---|---|
| `admin` | `ADMIN` | Toàn bộ hệ thống, kể cả `/tinh-cuoc` và `/ky-cuoc` |
| `nhanvien01` | `NHANVIEN` | Khách hàng, thuê bao, và hai báo cáo không chứa số tiền của khách (thống kê thuê bao, sản lượng) |
| `ketoan01` | `KETOAN` | Hóa đơn, thanh toán, công nợ, giảm trừ, báo cáo |

Phân quyền kiểm **ở máy chủ**, không chỉ ẩn menu — gõ thẳng đường dẫn không có quyền vẫn ra
trang 403.

Thêm tài khoản mới, đổi quyền, đặt lại mật khẩu hay khoá tài khoản đều làm **trên giao diện**
tại `admin` › *Quản trị › Người dùng* — không phải gõ SQL tay như trước. Khoá một tài khoản
là người đó bị thoát ra **ngay lúc đó**, không đợi tới lần đăng nhập sau.

Mọi vai trò tự đổi được mật khẩu của mình ở nút **Đổi mật khẩu** cạnh tên người dùng — phải
nhập đúng mật khẩu hiện tại, và đổi xong thì phiên kết thúc, đăng nhập lại bằng mật khẩu mới.
Nhập sai mật khẩu 5 lần liên tiếp thì tài khoản bị khoá tạm 15 phút.

---

## 6. Dữ liệu mẫu kèm theo

Hai file chạy nối tiếp khi `reset`, ranh giới giữa chúng là ranh giới về **nguồn gốc**:

| File | Nội dung | Nguồn gốc |
|---|---|---|
| `db/data-mau.sql` | 3 tài khoản · 50 khách hàng · 80 thuê bao · 5 gói cước · 10 dòng bảng giá · **6 kỳ cước** · 18 dòng mở sổ số dư | Viết tay — sửa được |
| `db/data-van-hanh.sql` | 18.723 CDR đã định giá · 280 hóa đơn · 620 chi tiết · 161 thanh toán · 34 dòng sổ cái số dư · 2 giảm trừ | **Bản dump** do máy sinh |

Sáu kỳ cước sau khi nạp:

| Kỳ | Trạng thái | CDR | Hóa đơn | Doanh thu | Thanh toán |
|---|---|---:|---:|---:|---:|
| 3/2026 | Đã chốt | 2.770 | 55 | 21.497.051 đ | 58 |
| 4/2026 | Đã chốt | 3.239 | 55 | 21.737.109 đ | 55 |
| 5/2026 | Đã chốt | 3.697 | 54 | 21.289.162 đ | 48 |
| 6/2026 | Mở | 5.017 | 58 | 23.828.605 đ | **0** |
| 7/2026 | Mở | 4.000 | 58 | 23.161.085 đ | **0** |
| **8/2026** | Mở | **0** | **0** | **0 đ** | **0** |

> **Kỳ 8/2026 cố ý để rỗng** — đó là kỳ dành cho demo trực tiếp (sinh CDR → tính cước → lập
> hóa đơn ngay trên sân khấu) và cũng là kỳ để kiểm "màn hình chịu được kỳ rỗng".
>
> **Kỳ 6 và 7 cố ý giữ 0 thanh toán** — có thanh toán thì `huyBillingKy` từ chối xoá hóa đơn,
> và mất luôn hai kỳ còn demo được trọn vòng *huỷ hóa đơn → lập lại*.

> ⚠️ `data-van-hanh.sql` **không phải file soạn tay.** Muốn đổi dữ liệu vận hành thì đổi qua
> giao diện hoặc qua service rồi dump lại cả file — sửa tay một con số trong đó là dựng ra
> nguồn sự thật thứ hai cạnh đoạn mã sinh ra nó. Cách tái sinh ghi ở đầu file.

### ⚠️ Cảnh báo về profile `reset`

> **Profile `reset` XOÁ TOÀN BỘ dữ liệu đang có.** Nó mở khoá `flyway clean` — lệnh xoá sạch
> cả 15 bảng, 2 view lẫn bảng lịch sử di trú — rồi dựng lại từ `db/migration/` và nạp hai file
> dữ liệu mẫu. Mọi khách hàng, thuê bao, giao dịch nhập qua giao diện đều **mất sạch**.
>
> Ở cấu hình thường, `flyway.clean-disabled: true` nên lệnh đó **không gọi được** — muốn xoá
> sạch thì phải nêu đích danh profile `reset`.

Bù lại, `reset` **tái lập đúng** bộ dữ liệu mà báo cáo mô tả: chạy lại bao nhiêu lần cũng ra
đúng 20.102 dòng giống hệt nhau. Trước Phase 5 mục F thì không — `data-mau.sql` không chứa hóa
đơn nào, mà bộ sinh CDR lại dùng `new Random()` không hạt giống, nên mỗi lần `reset` ra một bộ
số khác và mọi con số trong báo cáo mất khả năng tái lập.

---

## 7. Chạy kiểm thử

### 7.1. Test tự động (JUnit)

```bash
mvnw test
```

**307 test.** Phần lớn chạy độc lập không cần CSDL; 11 lớp cần MySQL đang chạy vì chúng kiểm
bất biến trên **dữ liệu thật** chứ không trên dữ liệu dựng sẵn — kể cả bất biến thanh toán
**dưới tải đồng thời**.

> ⚠️ **Dừng ứng dụng trước khi chạy `mvnw test`** — bộ test chạy trên CSDL thật.

> ⚠️ Dòng phân rã theo lớp có thể in `Tests run: 0 ... in Ma trận chuyển trạng thái thuê bao`.
> Đó là cách Surefire đếm lớp `@Nested`, **không phải lỗi** — con số đúng ở dòng tổng.

### 7.2. Script kiểm thử giao diện

**8 script** trong `scripts/`, tổng **215 phép kiểm**, chạy khi ứng dụng đang bật:

| Script | Kiểm | Nội dung |
|---|---:|---|
| `test-auth.ps1` | 42 | Đăng nhập, CSRF, phân quyền 3 vai trò, phạm vi báo cáo theo nội dung, quản lý người dùng, đổi mật khẩu, khoá tạm |
| `test-kh.ps1` | 14 | Khách hàng: tạo, sửa, validation, trùng giấy tờ |
| `test-tb.ps1` | 18 | Thuê bao: đăng ký, chuyển trạng thái, đổi gói |
| `test-muc-F.ps1` | 17 | Bất biến thanh toán và công nợ |
| `test-bao-cao.ps1` | 39 | 7 báo cáo + dashboard + xuất Excel |
| `test-dieu-huong.ps1` | 15 | **Đi theo link thật** trên trang, không gõ URL |
| `test-ky-rong.ps1` | 28 | Kỳ 8/2026 rỗng: 17 màn hình + 4 Excel + 3 thao tác |
| `test-bien.ps1` | 42 | Tham số biên, dữ liệu xấu, 12 × 403 |

Xem [`scripts/README.md`](scripts/README.md).

### 7.3. Kịch bản kiểm thử thủ công

70 ca kiểm thủ công chia 9 nhóm: [`docs/kich-ban-kiem-thu.md`](docs/kich-ban-kiem-thu.md).

---

## 8. Cấu trúc dự án

```
vnpt-billing/
├── pom.xml
├── mvnw / mvnw.cmd                      # Maven Wrapper
├── README.md
├── CLAUDE.md                            # bối cảnh dự án, ràng buộc nghiệp vụ
├── docs/                                # 15 tài liệu — xem mục 9
├── scripts/                             # 8 script kiểm thử giao diện qua HTTP
├── logs/                                # log ứng dụng, xoay vòng theo ngày (không commit)
└── src/
    ├── main/
    │   ├── java/com/hanzo/billing/
    │   │   ├── BillingApplication.java  # lớp khởi động
    │   │   ├── config/                  # Security, MVC, khởi tạo
    │   │   ├── controller/              # tầng điều khiển (Spring MVC)
    │   │   ├── dto/                     # đối tượng truyền dữ liệu cho form/view
    │   │   ├── entity/                  # 15 thực thể JPA
    │   │   ├── enums/                   # 16 kiểu liệt kê nghiệp vụ
    │   │   ├── exception/               # ngoại lệ nghiệp vụ + GlobalExceptionHandler
    │   │   ├── repository/              # 15 repository Spring Data JPA
    │   │   ├── service/
    │   │   │   ├── impl/                # cài đặt nghiệp vụ
    │   │   │   └── rating/              # thuật toán tính cước
    │   │   └── util/                    # tiện ích dùng chung
    │   └── resources/
    │       ├── application.yml
    │       ├── application-reset.yml
    │       ├── db/
    │       │   ├── migration/           # Flyway: V1 khởi tạo, V2 đổi cấu trúc đợt hoàn thiện
    │       │   ├── data-mau.sql         # dữ liệu gốc, viết tay
    │       │   └── data-van-hanh.sql    # kết quả vận hành, bản dump do máy sinh
    │       ├── static/css/app.css
    │       └── templates/
    │           ├── fragments/layout.html
    │           ├── error/{400,403,404,500}.html
    │           └── <phân-hệ>/*.html
    └── test/java/com/hanzo/billing/     # 307 test
```

---

## 9. Tài liệu

| Tài liệu | Nội dung |
|---|---|
| [`docs/huong-dan-su-dung.md`](docs/huong-dan-su-dung.md) | **Hướng dẫn thao tác từng chức năng** |
| [`docs/mo-ta-csdl.md`](docs/mo-ta-csdl.md) | Mô tả 15 bảng + 2 view (mục 6: cảnh báo quy đổi đơn vị DATA) |
| [`docs/kich-ban-kiem-thu.md`](docs/kich-ban-kiem-thu.md) | 70 ca kiểm thủ công + tổng hợp test tự động |
| [`docs/kich-ban-demo.md`](docs/kich-ban-demo.md) | 12 bước demo 18 phút, kèm câu hỏi hội đồng |
| [`docs/danh-sach-anh-chup.md`](docs/danh-sach-anh-chup.md) | 69 ảnh màn hình cần chụp cho báo cáo |
| [`docs/toi-uu-hieu-nang.md`](docs/toi-uu-hieu-nang.md) | Số đo ghi hàng loạt CDR và chỉ mục |
| [`docs/mau-cdr.csv`](docs/mau-cdr.csv) | File CSV mẫu cho chức năng nhập CDR |
| [`scripts/README.md`](scripts/README.md) | Script kiểm thử giao diện |

Báo cáo theo phase:

| Phase | Nội dung | Báo cáo |
|---|---|---|
| 0 | Khung dự án | [`PHASE-0-REPORT.md`](docs/PHASE-0-REPORT.md) |
| 1 | CSDL 15 bảng + 2 view | [`mo-ta-csdl.md`](docs/mo-ta-csdl.md) |
| 2 | Xác thực, khách hàng, thuê bao | [`PHASE-2-REPORT.md`](docs/PHASE-2-REPORT.md) |
| 3 | Gói cước, bảng giá, CDR, kỳ cước | [`PHASE-3-REPORT.md`](docs/PHASE-3-REPORT.md) |
| 4 | Engine tính cước | [`PHASE-4-PLAN.md`](docs/PHASE-4-PLAN.md) · [`PHASE-4-REPORT.md`](docs/PHASE-4-REPORT.md) |
| 5 | Hóa đơn, thanh toán, công nợ | [`PHASE-5-PLAN.md`](docs/PHASE-5-PLAN.md) · [`PHASE-5-REPORT.md`](docs/PHASE-5-REPORT.md) |
| 6 | Báo cáo, thống kê, dashboard | [`PHASE-6-REPORT.md`](docs/PHASE-6-REPORT.md) |
| 7 | Hoàn thiện, kiểm thử, tài liệu | [`PHASE-7-REPORT.md`](docs/PHASE-7-REPORT.md) |

Hai mục đáng đọc nhất nếu chỉ có thời gian đọc hai mục: **`PHASE-4-REPORT.md` mục 43** (bảy
chuẩn làm việc, rút ra từ những phép kiểm sai) và **`PHASE-7-REPORT.md` mục tổng kết**.

---

## 10. Khắc phục sự cố

### `spring-boot:run` báo `Could not find or load main class`

**Nguyên nhân:** đường dẫn dự án chứa ký tự tiếng Việt có dấu (ví dụ `D:\HỌC\...`). Spring Boot
Maven plugin ghi classpath vào một *argfile* rồi truyền cho JVM con. Trình khởi động `java` đọc
argfile bằng bảng mã ANSI của Windows (`windows-1258`) **trước khi** máy ảo khởi động, nên ký
tự `Ọ` thành `?` và classpath hỏng. Không tham số `-D...` nào sửa được vì lỗi xảy ra trước khi
JVM đọc tham số.

**Cách khắc phục (chọn 1):**

1. **Đặt dự án ở đường dẫn không dấu** — cách triệt để. Ví dụ `D:\HOC\TTNN\APP\vnpt-billing`.
2. **Giữ nguyên đường dẫn**, chạy bằng file JAR:

   ```bash
   mvnw package
   ```

   ```bash
   java -jar target/billing-0.0.1-SNAPSHOT.jar
   ```

3. **Chạy từ IntelliJ IDEA** (nút Run trên `BillingApplication`) — IntelliJ tự dựng dòng lệnh
   nên không đi qua argfile.

### Không khởi động được vì chưa có MySQL

Ứng dụng cần MySQL tại `localhost:3306`. Chỉ muốn xem giao diện mà chưa cài MySQL:

```bash
java -Dspring.sql.init.mode=never -Dspring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration -jar target/billing-0.0.1-SNAPSHOT.jar
```

### Sửa CSS/JS mà trình duyệt không thấy đổi

Spring phục vụ tài nguyên tĩnh từ `target/classes`, **không** từ `src/main/resources`. Chạy lại
`mvnw spring-boot:run` (hoặc `mvnw compile`) để chép sang, rồi tải lại trang bỏ qua bộ nhớ đệm
(`Ctrl+F5`).

### Trang báo 500 kèm mã sự cố

Ghi lại mã (dạng `20260810-143052`) rồi tìm trong `logs/vnpt-billing.log` — đó là khoá tra cứu
giữa cái người dùng thấy và cái nhật ký ghi. Log xoay vòng theo ngày, giữ 30 ngày.

### Các sự cố nghiệp vụ khác

Xem [`docs/huong-dan-su-dung.md`](docs/huong-dan-su-dung.md) mục 11.
