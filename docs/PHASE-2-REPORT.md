# BÁO CÁO PHASE 2 — XÁC THỰC, QUẢN LÝ KHÁCH HÀNG VÀ THUÊ BAO

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Ngày thực hiện:** 31/07/2026
**Trạng thái:** ✅ Hoàn thành — đã nghiệm thu đủ 10/10 tiêu chí

> Toàn bộ dữ liệu trong hệ thống là dữ liệu mẫu tự sinh phục vụ học tập.
> Hệ thống không sử dụng dữ liệu thật của bất kỳ nhà mạng nào.

---

## 1. Mục tiêu Phase 2

Phase 2 gồm hai phần tách bạch:

| Phần | Nội dung |
|---|---|
| **A** | Xử lý nợ kỹ thuật tồn từ Phase 1 — làm trước khi viết bất kỳ form nào |
| **B–E** | Xác thực & phân quyền, quản lý khách hàng, quản lý thuê bao, hạ tầng dùng chung |

Phạm vi loại trừ: chưa làm gói cước / bảng giá (Phase 3), chưa làm CDR, chưa làm
engine tính cước, chưa làm hóa đơn.

---

## 2. Xử lý nợ kỹ thuật Phase 1 (mục A)

### 2.1. Chặn việc CSDL bị dựng lại mỗi lần khởi động

**Vấn đề:** `schema.sql` mở đầu bằng `DROP TABLE IF EXISTS`, kết hợp
`spring.sql.init.mode: always` khiến mỗi lần khởi động ứng dụng là CSDL bị xoá sạch.
Ở Phase 1 điều này chấp nhận được vì toàn bộ là dữ liệu mẫu, nhưng Phase 2 bắt đầu
có form nhập liệu nên phải xử lý dứt điểm trước.

**Cách xử lý — tách bằng Spring profile:**

| File | Nội dung |
|---|---|
| `application.yml` | `spring.sql.init.mode: never` (mặc định) |
| `application-reset.yml` | `spring.sql.init.mode: always` (chỉ khi bật profile) |

```bash
mvnw spring-boot:run
```

```bash
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

Lệnh đầu giữ nguyên dữ liệu, lệnh sau nạp lại bộ dữ liệu mẫu ban đầu. README mục 4
đã viết lại theo 4 tiểu mục kèm cảnh báo in đậm rằng profile `reset` **xoá toàn bộ
dữ liệu đang có**.

**Kiểm chứng hai chiều:**

| Bước | Kết quả |
|---|---|
| Chèn 1 khách hàng qua SQL → khởi động lại bình thường | Khách hàng vẫn còn (51 bản ghi) |
| Chạy lại với profile `reset` | Khách hàng biến mất, về đúng 50 / 80 / 5 |

### 2.2. Biến kiểm tra ánh xạ thành test tự động

Ở Phase 1, việc kiểm tra Entity có khớp schema hay không được làm thủ công bằng cách
chạy ứng dụng với biến môi trường `ddl-auto=validate`. Cách đó không lặp lại được và
không ai nhớ để chạy.

Đã biến thành test: `src/test/java/com/hanzo/billing/SchemaValidationTest.java`

```java
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.sql.init.mode=never"
})
class SchemaValidationTest {
    @Test
    void contextLoads() { }
}
```

Không cần assert gì thêm: chỉ riêng việc Spring context khởi động được với
`ddl-auto=validate` đã chứng minh cả 15 Entity ánh xạ đúng. Thuộc tính
`sql.init.mode=never` là bắt buộc — nếu quên, test sẽ chạy `schema.sql` và **xoá sạch
CSDL đang có**.

Kết quả: `mvnw test` → `Tests run: 1, Failures: 0, Errors: 0`.

### 2.3. Bổ sung dữ liệu lịch sử biến động thuê bao

**Vấn đề:** dữ liệu mẫu có 15 thuê bao không ở trạng thái `HOAT_DONG` nhưng bảng
`lich_su_thue_bao` rỗng. Mở tab lịch sử của chúng sẽ trống trơn — vô lý về nghiệp vụ,
vì trạng thái hiện tại phải đến từ một thao tác nào đó.

Đã bổ sung **21 dòng** với ID cố định để giữ tính idempotent của script:

| Nhóm | Số dòng | Ghi chú |
|---|---|---|
| 8 thuê bao tạm ngưng 1 chiều | 8 | Mỗi thuê bao 1 dòng |
| 2 thuê bao tạm ngưng 2 chiều | 2 | Mỗi thuê bao 1 dòng |
| 3 thuê bao đã thanh lý | 3 | `thoi_gian` khớp với `ngay_huy` trong bảng `thue_bao` |
| 4 thuê bao có lịch sử **nhiều bước** | 8 | ID 8, 24 (→1 chiều→2 chiều); ID 26, 45 (→1 chiều→khôi phục) |

---

## 3. Các quyết định kỹ thuật và lý do

### 3.1. Ma trận chuyển trạng thái đặt thành hằng số ở service

Quy tắc vòng đời thuê bao được khai một chỗ duy nhất trong `ThueBaoServiceImpl`:

```java
CHUYEN_HOP_LE.put(HOAT_DONG,    EnumSet.of(TAM_NGUNG_1C, TAM_NGUNG_2C, DA_THANH_LY));
CHUYEN_HOP_LE.put(TAM_NGUNG_1C, EnumSet.of(TAM_NGUNG_2C, HOAT_DONG, DA_THANH_LY));
CHUYEN_HOP_LE.put(TAM_NGUNG_2C, EnumSet.of(HOAT_DONG, DA_THANH_LY));
CHUYEN_HOP_LE.put(DA_THANH_LY,  EnumSet.noneOf(TrangThaiThueBao.class));
```

Lý do không rải `if/else` ra controller:

- Nhìn một chỗ là biết toàn bộ luật chuyển, thuận tiện khi trình bày trước hội đồng
- Mọi lối vào (giao diện hiện tại, API sau này) đều đi qua cùng một kiểm tra
- `DA_THANH_LY` là trạng thái cuối vì **số thuê bao đã thu hồi và có thể cấp lại cho
  khách khác**; cho khôi phục sẽ làm sai lệch dữ liệu cước lịch sử

### 3.2. Đổi gói cước có hiệu lực từ đầu tháng kế tiếp

Cước thuê bao tính theo chu kỳ tháng. Đổi gói giữa chừng sẽ khiến một tháng phải chia
cước theo hai bảng giá khác nhau. Vì vậy:

```java
LocalDate ngayHieuLuc = LocalDate.now().plusMonths(1).withDayOfMonth(1);
```

Bản ghi `dang_ky_goi_cuoc` cũ được đóng lại (`ngay_ket_thuc` = hôm trước ngày hiệu lực,
trạng thái `DA_KET_THUC`), bản ghi mới mở từ ngày hiệu lực. Ngày này **hiển thị rõ trên
modal** trước khi người dùng bấm xác nhận.

### 3.3. Ràng buộc CCCD / MST phải là constraint mức lớp

Quy tắc "CA_NHAN thì CCCD 12 số, DOANH_NGHIEP thì MST 10 hoặc 13 số" phụ thuộc **đồng
thời hai trường** `loaiKh` và `soGiayTo`. Bean Validation không diễn đạt được quan hệ đó
bằng annotation đặt trên một trường đơn lẻ, nên phải viết constraint mức lớp
`@GiayToHopLe` + `GiayToHopLeValidator`, trong đó dùng `addPropertyNode("soGiayTo")` để
lỗi vẫn hiện đúng ở ô nhập tương ứng.

### 3.4. Bắt `NghiepVuException` tại controller khi lưu form

`GlobalExceptionHandler` chuyển hướng người dùng về màn hình vừa thao tác. Cách đó đúng
cho các nút bấm trên danh sách, nhưng **sai với form**: chuyển hướng sẽ làm mất toàn bộ
nội dung đang gõ dở. Vì vậy hai phương thức `luu()` bắt ngoại lệ tại chỗ, đưa vào
`BindingResult` rồi vẽ lại form kèm dữ liệu cũ.

### 3.5. JOIN FETCH bắt buộc vì `open-in-view = false`

Cấu hình `spring.jpa.open-in-view: false` khiến phiên Hibernate đóng trước khi Thymeleaf
vẽ view. Mọi truy vấn phục vụ màn hình có hiển thị dữ liệu quan hệ đều phải `JOIN FETCH`
sẵn, nếu không sẽ ném `LazyInitializationException`. Riêng `nguoiThucHien` cho phép null
nên dùng `LEFT JOIN FETCH` — dùng `JOIN FETCH` thường sẽ loại mất các dòng đó.

### 3.6. Sidebar `sec:authorize` chỉ là lớp che giao diện

Menu ẩn/hiện theo vai trò cho gọn mắt, nhưng **chốt chặn thật nằm ở `SecurityConfig`**.
Đã kiểm chứng: gõ thẳng URL không có quyền vẫn bị trả về trang 403.

---

## 4. Cấu trúc bổ sung

Repo hiện có **101 file**. Phân bố mã nguồn Java:

| Package | Số file | Bổ sung ở Phase 2 |
|---|---|---|
| `config` | 1 | `SecurityConfig` viết lại hoàn toàn |
| `controller` | 5 | `AuthController`, `KhachHangController`, `ThueBaoController`, `LayoutAdvice` |
| `dto` | 2 | `KhachHangForm`, `ThueBaoForm` |
| `entity` | 15 | không đổi |
| `enums` | 16 | bổ sung nhãn tiếng Việt và màu badge cho 7 enum |
| `exception` | 2 | `NghiepVuException`, `GlobalExceptionHandler` |
| `repository` | 15 | bổ sung truy vấn phân trang, tìm kiếm, JOIN FETCH |
| `security` | 2 | `CustomUserDetailsService`, `NguoiDungPrincipal` |
| `service` + `service/impl` | 6 | `KhachHangService`, `ThueBaoService`, `NhatKyService` |
| `util` | 1 | `SecurityUtils` |
| `validation` | 2 | `@GiayToHopLe` + validator |

Template Thymeleaf (12 file):

```
templates/
├── dang-nhap.html              ← template riêng, không dùng layout sidebar
├── index.html
├── error/{403,404,500}.html
├── fragments/layout.html        ← sidebar theo vai trò + header + modal dùng chung
├── khach-hang/{danh-sach, form, chi-tiet}.html
└── thue-bao/{danh-sach, dang-ky, chi-tiet}.html
```

Lịch sử Git phản ánh đúng quá trình làm việc theo từng mục:

| Commit | Nội dung |
|---|---|
| `88aa9c4` | Phase 2A: xử lý nợ kỹ thuật Phase 1 |
| `069eb69` | Phase 2B: xác thực, phân quyền và hạ tầng dùng chung |
| `fd6b5e9` | Phase 2C: quản lý khách hàng |
| `ac63f4d` | Phase 2D: quản lý thuê bao |

---

## 5. Kết quả nghiệm thu

### 5.1. Đối chiếu 10 tiêu chí

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvnw test` PASS gồm `SchemaValidationTest` | ✅ | `Tests run: 1, Failures: 0, Errors: 0` |
| 2 | Nhập dữ liệu → restart → dữ liệu còn nguyên | ✅ | 52 KH / 82 TB / 1 nạp tiền / 9 nhật ký giữ nguyên qua restart |
| 3 | Profile `reset` → về 50 / 80 / 5 | ✅ | Đúng 50 / 80 / 5, dữ liệu thử nghiệm bị xoá hết |
| 4 | 3 tài khoản, menu và quyền khác nhau; URL cấm → 403 | ✅ | Xem bảng 5.2 |
| 5 | Tạo KH doanh nghiệp → đăng ký 2 thuê bao trả sau | ✅ | `KH000052` + 2 thuê bao `0355577001`, `0355577002` |
| 6 | `DA_THANH_LY` → `HOAT_DONG` bị chặn | ✅ | "Thuê bao 0355577001 đã thanh lý, không thể chuyển sang trạng thái khác" |
| 7 | Tạm ngưng 1 chiều rồi khôi phục → 3 dòng lịch sử | ✅ | Timeline đúng 3 dòng: đăng ký → tạm ngưng → khôi phục |
| 8 | Thuê bao `TAM_NGUNG_1C` mẫu → lịch sử không rỗng | ✅ | Thuê bao id 5 có 1 dòng; id 8 (nhiều bước) có 2 dòng |
| 9 | Đổi gói → hiệu lực đầu tháng kế tiếp, 2 dòng đúng trạng thái | ✅ | Đổi ngày 31/07/2026 → hiệu lực 01/08/2026 |
| 10 | CCCD 11 số, MST sai, số thuê bao trùng → đều bị chặn | ✅ | Xem bảng 5.3 |

### 5.2. Phân quyền theo vai trò

Menu sidebar hiển thị đúng theo vai trò:

| Tài khoản | Khách hàng | Hóa đơn | Gói cước | Báo cáo |
|---|---|---|---|---|
| `admin` | ✅ | ✅ | ✅ | ✅ |
| `nhanvien01` | ✅ | — | — | ✅ |
| `ketoan01` | — | ✅ | — | ✅ |

Gõ thẳng URL không có quyền:

| Thao tác | Kết quả |
|---|---|
| `ketoan01` mở `/khach-hang` | HTTP **403**, trang thân thiện dùng layout chung |
| `nhanvien01` mở `/hoa-don` | HTTP **403** |
| `nhanvien01` mở `/quan-tri` | HTTP **403** |
| `admin` mở `/quan-tri` | HTTP 404 — qua được lớp bảo mật, chỉ là chưa có controller (Phase 3) |

Ngoài ra: chưa đăng nhập mở `/` bị đưa về form đăng nhập; sai mật khẩu bị từ chối;
đăng xuất huỷ phiên thành công.

### 5.3. Kiểm thử validation

| Trường hợp | Kết quả |
|---|---|
| CCCD 11 số | ❌ Chặn — "Số CCCD phải gồm đúng 12 chữ số" |
| MST 5 số | ❌ Chặn — "Mã số thuế phải gồm 10 hoặc 13 chữ số" |
| Bỏ trống tên / số giấy tờ / địa chỉ | ❌ Chặn — báo lỗi từng ô |
| Số giấy tờ trùng khách khác | ❌ Chặn — "đã được dùng cho một khách hàng khác" |
| Số thuê bao trùng | ❌ Chặn — "đã tồn tại trong hệ thống" |
| Số thuê bao `0123456789` (đầu số sai) | ❌ Chặn — "bắt đầu bằng 03, 05, 07, 08 hoặc 09" |
| Ngày kích hoạt 01/01/2027 | ❌ Chặn — "không được ở tương lai" |
| Gói trả trước cho thuê bao trả sau | ❌ Chặn — "chỉ áp dụng cho thuê bao…" |
| Nạp tiền cho thuê bao trả sau | ❌ Chặn — "Chỉ nạp tiền được cho thuê bao trả trước" |
| Ngừng giao dịch khi còn thuê bao hoạt động | ❌ Chặn — "còn 2 thuê bao đang hoạt động" |

### 5.4. Đối chiếu trực tiếp với CSDL

Không chỉ kiểm tra giao diện, các thao tác ghi đều được soi lại trong CSDL:

**Nạp tiền 100.000 đ cho thuê bao `0901234501`:**

| Bảng | Giá trị |
|---|---|
| `thue_bao.so_du` | 52.000 → **152.000** |
| `nap_tien` | `so_tien`=100.000, `so_du_truoc`=52.000, `so_du_sau`=152.000, `nguoi_thuc_hien_id`=2 |

**Đổi gói cước (thuê bao id 82, ngày 31/07/2026):**

| `goi_cuoc_id` | `ngay_bat_dau` | `ngay_ket_thuc` | `trang_thai` |
|---|---|---|---|
| 4 (DN500) | 2026-07-31 | 2026-07-31 | `DA_KET_THUC` |
| 3 (MAX150) | **2026-08-01** | NULL | `DANG_AP_DUNG` |

`nhat_ky_he_thong` ghi đủ 9 dòng cho 9 thao tác ghi dữ liệu đã thực hiện.

---

## 6. Sự cố kỹ thuật

### 6.1. Lombok chưa từng chạy — lỗi tiềm ẩn từ Phase 1

**Hiện tượng:** ngay khi viết dòng code đầu tiên của Phase 2 có gọi getter/setter,
build đổ 69 dòng lỗi `cannot find symbol` trải khắp nhiều file, kể cả biến `log` do
`@Slf4j` sinh ra.

**Quá trình chẩn đoán:**

| Bước | Việc làm | Kết quả |
|---|---|---|
| 1 | Xem các file lỗi | Tất cả đều là chỗ gọi thành viên do Lombok sinh |
| 2 | Kiểm tra `dependency:tree` | Lombok 1.18.46 có mặt, scope `compile (optional)` |
| 3 | Tìm cảnh báo về annotation processing | Không có cảnh báo nào |
| 4 | Đối chiếu với Phase 1 | Phase 1 build sạch 49 file — mâu thuẫn |

**Nguyên nhân:** từ **JDK 23**, `javac` không còn tự động bật annotation processing khi
chỉ thấy processor nằm trên classpath (JDK 21/22 vẫn chạy nhưng có cảnh báo). Lombok vì
vậy im lặng không sinh gì cả.

**Vì sao Phase 1 không lộ ra:** Phase 1 chỉ có entity, enum và repository — **không có
dòng code nào gọi getter/setter**. Hibernate ánh xạ entity qua *field access* (do `@Id`
đặt trên trường) nên không cần getter. `SchemaValidationTest` cũng chỉ kiểm tra cấu trúc
cột, không đụng tới method. Phase 2 là code đầu tiên thực sự dùng chúng.

**Cách khắc phục:** khai báo Lombok làm annotation processor tường minh trong `pom.xml`:

```xml
<annotationProcessorPaths>
    <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
    </path>
</annotationProcessorPaths>
```

Kiểm chứng bằng `javap` trên file `.class` đã biên dịch: `NguoiDung.class` giờ có đủ
`getHoTen()`, `setHoTen()`, `getVaiTro()`.

### 6.2. `#fields` đặt ngoài thẻ `<form>` làm hỏng trang khi validation thất bại

**Hiện tượng:** mọi lần submit form khách hàng với dữ liệu sai đều trả về HTTP 500 thay
vì vẽ lại form kèm thông báo lỗi.

**Nguyên nhân:** khối alert hiển thị lỗi nghiệp vụ được đặt **trước** thẻ
`<form th:object="${khachHangForm}">`. Biểu thức `#fields` chỉ hoạt động trong phạm vi
một đối tượng đang bind, nên Thymeleaf ném
`Method hasGlobalErrors(String) cannot be found`.

**Điểm đáng lưu ý về phương pháp kiểm thử:** lỗi này suýt bị bỏ sót vì kịch bản kiểm thử
vẫn "đậu" ở bước tạo mới. Nguyên nhân: script lấy CSRF token bằng cách dò chuỗi trong
HTML trả về, mà **trang lỗi 500 cũng dùng layout chung, trong đó có form đăng xuất mang
CSRF token**. Script lấy nhầm token từ đó nên bước POST sau vẫn chạy được. Bài học: khi
kiểm thử tự động, phải kiểm tra cả **mã trạng thái HTTP** chứ không chỉ kiểm tra sự có
mặt của một chuỗi.

**Cách khắc phục:** chuyển khối alert vào trong thẻ `<form>` và bỏ tham số:
`#fields.hasGlobalErrors()`.

### 6.3. Hai lỗi nhỏ trong kịch bản kiểm thử (không phải lỗi ứng dụng)

Ghi lại để tránh lặp:

- File `.ps1` chứa tiếng Việt phải lưu **kèm BOM UTF-8**, nếu không PowerShell 5.1 đọc
  bằng ANSI codepage (`windows-1258`) và toàn bộ chuỗi tiếng Việt bị hỏng
- `$home` là biến chỉ đọc của PowerShell, không được dùng làm tên biến

---

## 7. Việc tồn đọng

Không còn việc tồn đọng nào của Phase 2. Hai điểm cần lưu ý khi bước sang phase sau:

### 7.1. Mã khách hàng đang có hai định dạng

Dữ liệu mẫu dùng `KH0001`–`KH0050` (4 chữ số), còn mã tự sinh theo yêu cầu là
`KH` + 6 chữ số (`KH000051` trở đi). Hệ thống hoạt động bình thường vì mã chỉ cần duy
nhất, nhưng nếu muốn thống nhất hình thức thì nên chuẩn hoá dữ liệu mẫu về 6 chữ số ở
Phase 3.

### 7.2. Các mục menu chưa có controller

Sidebar đã hiện đủ nhóm menu theo vai trò, nhưng Gói cước, Bảng giá, CDR, Tính cước,
Hóa đơn, Thanh toán, Công nợ, Báo cáo, Quản trị vẫn trỏ `#`. Đây là phạm vi của
Phase 3 trở đi, không phải thiếu sót của Phase 2.

---

## 8. Kế hoạch các phase tiếp theo

| Phase | Nội dung |
|---|---|
| 3 | Gói cước, bảng giá cước, CDR (bộ sinh giả lập + import CSV) |
| 4 | Engine tính cước — Rating và Billing (phần lõi) |
| 5 | Hóa đơn, thanh toán, công nợ |
| 6 | Báo cáo, thống kê, dashboard |
| 7 | Hoàn thiện, kiểm thử, tài liệu |

---

## 9. Danh sách màn hình nên chụp ảnh cho báo cáo

Gợi ý thứ tự chụp để mạch báo cáo đi từ đăng nhập → phân quyền → nghiệp vụ → ràng buộc.
Trước khi chụp nên chạy `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"` để dữ
liệu sạch đẹp.

### Nhóm 1 — Xác thực và phân quyền

| # | Màn hình | Cách lấy | Điểm cần thấy rõ trong ảnh |
|---|---|---|---|
| 1 | Trang đăng nhập | `/dang-nhap` | Bảng 3 tài khoản demo, ghi chú "hệ thống demo học tập" |
| 2 | Đăng nhập sai mật khẩu | Nhập sai rồi Enter | Alert đỏ "Tên đăng nhập hoặc mật khẩu không đúng" |
| 3 | Trang chủ với vai trò `admin` | Đăng nhập `admin` | Sidebar đầy đủ 5 nhóm menu, header hiện "Quản trị viên" |
| 4 | Trang chủ với vai trò `nhanvien01` | Đăng nhập `nhanvien01` | Sidebar **không có** nhóm Kế toán và Danh mục |
| 5 | Trang chủ với vai trò `ketoan01` | Đăng nhập `ketoan01` | Sidebar **không có** nhóm Quầy giao dịch |
| 6 | Trang 403 | Đăng nhập `ketoan01`, gõ `/khach-hang` | Trang 403 thân thiện vẫn có sidebar và header |

> Ảnh 3–5 nên chụp cạnh nhau trong báo cáo để thấy rõ sự khác biệt của sidebar.

### Nhóm 2 — Quản lý khách hàng

| # | Màn hình | Cách lấy | Điểm cần thấy rõ trong ảnh |
|---|---|---|---|
| 7 | Danh sách khách hàng | `/khach-hang` | 15 dòng/trang, thanh phân trang, badge trạng thái |
| 8 | Tìm kiếm và lọc | Lọc "Doanh nghiệp" | Dòng "Tìm thấy 15 khách hàng", bộ lọc vẫn giữ giá trị |
| 9 | Form thêm — Cá nhân | `/khach-hang/them` | Có ô **Ngày sinh**, nhãn **Số CCCD** |
| 10 | Form thêm — Doanh nghiệp | Đổi loại sang Doanh nghiệp | Ô Ngày sinh biến mất, hiện **Người đại diện**, nhãn đổi thành **Mã số thuế** |
| 11 | Validation CCCD sai | Nhập CCCD 11 số rồi Lưu | Ô soGiayTo viền đỏ, thông báo "phải gồm đúng 12 chữ số" |
| 12 | Validation trùng giấy tờ | Nhập số CCCD đã có | Alert đỏ đầu form "đã được dùng cho một khách hàng khác" |
| 13 | Chi tiết khách hàng | Mở một khách doanh nghiệp | Bảng thuê bao đang sở hữu + nút "Đăng ký thuê bao mới" |
| 14 | Chặn ngừng giao dịch | Bấm "Ngừng giao dịch" khi còn thuê bao chạy | Alert đỏ "còn N thuê bao đang hoạt động" |

> Ảnh 9 và 10 nên đặt cạnh nhau để minh hoạ form đổi động.

### Nhóm 3 — Quản lý thuê bao

| # | Màn hình | Cách lấy | Điểm cần thấy rõ trong ảnh |
|---|---|---|---|
| 15 | Danh sách thuê bao | `/thue-bao` | Đủ 4 màu badge: xanh lá, vàng, cam, xám |
| 16 | Lọc theo trạng thái | Lọc `TAM_NGUNG_1C` | "Tìm thấy 8 thuê bao" |
| 17 | Form đăng ký thuê bao | `/thue-bao/dang-ky` | Ô hạn mức tín dụng hiện khi chọn Trả sau |
| 18 | Lọc gói cước theo loại | Đổi sang Trả trước | Danh sách gói chỉ còn gói trả trước, ô hạn mức biến mất |
| 19 | Chi tiết — tab Thông tin chung | Mở một thuê bao trả sau | 4 tab, liên kết sang khách hàng |
| 20 | Modal chuyển trạng thái | Bấm "Chuyển trạng thái" | Chỉ liệt kê trạng thái hợp lệ, ô **Lý do bắt buộc** |
| 21 | Tab Lịch sử biến động | Thuê bao id 8 hoặc 26 | Timeline nhiều bước, có người thực hiện |
| 22 | Chặn khôi phục thuê bao đã thanh lý | Mở thuê bao `DA_THANH_LY` | Alert xám giải thích + các nút bị mờ |
| 23 | Thông báo chặn chuyển trạng thái | Thử chuyển từ `DA_THANH_LY` | Alert đỏ "đã thanh lý, không thể chuyển sang trạng thái khác" |
| 24 | Modal đổi gói cước | Bấm "Đổi gói cước" | Dòng xanh ghi rõ **ngày hiệu lực là đầu tháng kế tiếp** |
| 25 | Tab Lịch sử gói cước sau khi đổi | Sau khi đổi gói | 2 dòng: `Đã kết thúc` và `Đang áp dụng` |
| 26 | Modal nạp tiền | Thuê bao trả trước | Hiện số dư hiện tại |
| 27 | Tab Lịch sử nạp tiền | Sau khi nạp | Số dư trước / sau, hình thức, người thực hiện |

### Nhóm 4 — Minh chứng kỹ thuật

| # | Ảnh | Cách lấy |
|---|---|---|
| 28 | `mvnw test` PASS | Console: `Tests run: 1, Failures: 0, Errors: 0` |
| 29 | Dữ liệu sống sót qua restart | Hai ảnh `SELECT COUNT(*)` trước và sau khi khởi động lại |
| 30 | Profile `reset` đưa về dữ liệu mẫu | Console hiện `profile is active: "reset"` + `COUNT(*)` = 50/80/5 |
| 31 | Lịch sử Git 4 commit Phase 2 | `git log --oneline` |

---

## 10. Tổng kết

Phase 2 hoàn thành đủ 10/10 tiêu chí nghiệm thu. Hệ thống đã có xác thực thật, phân
quyền ba vai trò, và hai nghiệp vụ đầu tiên chạy hoàn chỉnh từ giao diện xuống CSDL.

Nợ kỹ thuật của Phase 1 được xử lý dứt điểm trước khi viết dòng form đầu tiên — quyết
định này về sau chứng minh là đúng, vì toàn bộ dữ liệu nhập trong quá trình kiểm thử
Phase 2 đều sống sót qua các lần khởi động lại.

Điểm đáng chú ý nhất về mặt kỹ thuật là phát hiện **Lombok chưa từng chạy kể từ Phase 1**
(mục 6.1). Lỗi này nằm im suốt Phase 1 vì không có code nào gọi tới thành viên do Lombok
sinh ra, và chỉ lộ ra ở dòng code đầu tiên của Phase 2. Đây là ví dụ cho thấy build
thành công không đồng nghĩa với cấu hình đúng — phải có code thật sử dụng thì mới biết.

Về phương pháp, mục 6.2 ghi lại một bài học về kiểm thử tự động: kiểm tra sự có mặt của
một chuỗi trong HTML là chưa đủ, phải kiểm tra cả mã trạng thái HTTP, nếu không một lỗi
500 vẫn có thể "đậu" bài kiểm thử.

Sẵn sàng bước sang Phase 3.
