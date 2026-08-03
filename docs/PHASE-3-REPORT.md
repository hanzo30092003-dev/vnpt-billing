# BÁO CÁO PHASE 3 — GÓI CƯỚC, BẢNG GIÁ VÀ GHI NHẬN CDR

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Ngày thực hiện:** 02–03/08/2026
**Trạng thái:** ✅ Hoàn thành — đã nghiệm thu đủ 10/10 tiêu chí

> Toàn bộ dữ liệu trong hệ thống là dữ liệu mẫu tự sinh phục vụ học tập.
> Hệ thống không sử dụng dữ liệu thật của bất kỳ nhà mạng nào.

---

## 1. Mục tiêu Phase 3

| Mục | Nội dung |
|---|---|
| **A** | Dọn nợ kỹ thuật Phase 2 — làm trước |
| **B** | Quản lý danh mục gói cước |
| **C** | Quản lý bảng giá cước theo thời gian |
| **D** | ⭐ Bộ sinh CDR giả lập |
| **E** | Nhập CDR từ file CSV |
| **F** | Tra cứu CDR và xuất Excel |
| **G** | Quản lý kỳ cước |

Phạm vi loại trừ: **tuyệt đối chưa viết logic tính cước**. Mọi bản ghi CDR sinh ra
đều có `cuoc_phi = NULL` và `trang_thai_tinh_cuoc = CHUA_TINH`. Engine tính cước
thuộc Phase 4.

---

## 2. Dọn nợ Phase 2 (mục A)

### 2.1. Chuẩn hoá mã khách hàng về 6 chữ số

Dữ liệu mẫu dùng `KH0001` (4 chữ số) còn mã tự sinh dùng `KH000051` (6 chữ số).
Đã đổi toàn bộ 50 mã trong `data-mau.sql` thành `KH000001`–`KH000050`.

Quan trọng hơn: **tách logic sinh mã ra `SinhMaService` riêng để unit test được**.
Trước đây việc tách phần số nằm trong câu SQL native
(`CAST(SUBSTRING(ma_kh, 3) AS UNSIGNED)`), không thể kiểm thử nếu không có CSDL.
Nay repository chỉ trả về `MAX(ma_kh)`, còn việc bỏ tiền tố `KH` rồi `parseInt`
làm ở tầng Java.

**Vì sao phải ép về số thay vì so sánh chuỗi:** so sánh chuỗi chỉ đúng khi mọi mã có
cùng độ dài phần số. Với dữ liệu cũ, `"KH0050" > "KH000051"` theo thứ tự chuỗi — hệ
thống sẽ sinh ra mã trùng.

### 2.2. Unit test cho ma trận chuyển trạng thái

`ThueBaoService.chuyenTrangThai()` là logic nghiệp vụ quan trọng nhất đã viết mà
chưa có test nào. Đã bổ sung **16 test phủ kín ma trận 4×4**, chạy bằng Mockito không
cần Spring context, chia thành 4 nhóm `@Nested` có `@DisplayName` tiếng Việt.

- Test hợp lệ: khẳng định trạng thái được cập nhật, có gọi `save`, có ghi
  `lich_su_thue_bao` và có ghi nhật ký hệ thống
- Test bị chặn: khẳng định ném đúng `NghiepVuException` **và** trạng thái thuê bao
  không bị thay đổi, không có lệnh ghi nào
- Ba trường hợp chuyển sang `DA_THANH_LY` đều khẳng định `ngay_huy` được set

### 2.3. Script kiểm thử thành tài sản của dự án

Ba script rời được chuyển vào `scripts/`, tách phần dùng chung ra `_chung.ps1`, và
**sửa lại chính lỗi mà chúng đã mắc ở Phase 2**: hàm `Kiem-Tra` nay bắt buộc xét mã
trạng thái HTTP trước, chỉ khi status đúng mới dò đến nội dung HTML. Hàm `Post-Form`
cũng báo lỗi ngay nếu trang lấy CSRF token trả về khác 200, thay vì âm thầm lấy token
từ một trang lỗi.

---

## 3. ⚠️ Hai điểm sai lệch đặc tả đã phát hiện

### 3.1. Ma trận chuyển trạng thái là 8/8, không phải 9/7

Đặc tả Phase 3 yêu cầu viết "9 tổ hợp hợp lệ và 7 tổ hợp không hợp lệ". Đối chiếu với
ma trận đã đặc tả ở Phase 2 thì con số thực tế là **8 hợp lệ và 8 bị chặn**:

|  | → HOAT_DONG | → TAM_NGUNG_1C | → TAM_NGUNG_2C | → DA_THANH_LY |
|---|---|---|---|---|
| **HOAT_DONG** | ✗ | ✅ | ✅ | ✅ |
| **TAM_NGUNG_1C** | ✅ | ✗ | ✅ | ✅ |
| **TAM_NGUNG_2C** | ✅ | ✗ | ✗ | ✅ |
| **DA_THANH_LY** | ✗ | ✗ | ✗ | ✗ |

Tổng: 3 + 3 + 2 + 0 = **8 hợp lệ**, còn lại 8 bị chặn (4 ô trên đường chéo là chuyển
sang chính trạng thái đang có, cộng 3 ô từ `DA_THANH_LY`, cộng ô
`TAM_NGUNG_2C → TAM_NGUNG_1C`).

Con số 9/7 chỉ đúng nếu `TAM_NGUNG_2C → TAM_NGUNG_1C` được coi là hợp lệ. **Cài đặt
hiện tại giữ nguyên** theo đúng ma trận Phase 2, và đã viết đủ 16 test phủ kín cả
16 ô.

### 3.2. Đơn vị DATA — hai chỗ quy đổi Phase 4 phải xử lý

`chi_tiet_su_dung.so_luong` lưu theo **KB** với dịch vụ DATA. Tài liệu Phase 1 ghi
nhầm là MB, đã sửa lại trong `docs/mo-ta-csdl.md`.

Đã bổ sung một mục cảnh báo riêng (mục 6 của `mo-ta-csdl.md`) vì có **hai** chỗ quy
đổi, không phải một:

| # | Cột | Đơn vị | Phase 4 phải làm |
|---|---|---|---|
| 1 | `chi_tiet_su_dung.so_luong` | KB | Chia 1024 ra MB trước khi nhân đơn giá |
| 2 | `goi_cuoc.data_mien_phi_mb` | MB | So với sản lượng **đã quy đổi**, không so trực tiếp với tổng KB |

Chỗ thứ hai nguy hiểm hơn nhiều. Ví dụ: thuê bao gói MAX70 (ưu đãi 2048 MB) dùng
1.500.000 KB ≈ 1465 MB — **vẫn trong ưu đãi, đáng lẽ không mất tiền data**. Nếu engine
so thẳng `1500000 > 2048` thì kết luận vượt ưu đãi và tính cước cho phần "vượt" khổng
lồ. Hóa đơn **vẫn phát hành bình thường**, không lỗi, không cảnh báo — chỉ phát hiện
được khi ngồi đối chiếu tay.

`mo-ta-csdl.md` mục 6.2 có sẵn câu SQL để tự kiểm chứng ở Phase 4.

---

## 4. Các quyết định kỹ thuật

### 4.1. Quy tắc giờ cao điểm đặt ở một nơi duy nhất

Đây là điểm dễ sai nhất của Phase 3. Bảng giá **chỉ có dòng giờ cao điểm cho THOAI**,
không có cho SMS và DATA. Nếu bộ sinh gắn cờ cao điểm cho SMS, engine Phase 4 sẽ đi
tìm dòng giá `SMS + gio_cao_diem = 1`, không thấy, và toàn bộ CDR loại đó rơi vào
trạng thái `LOI`.

Vì cả bộ sinh lẫn bộ nhập CSV đều cần quy tắc này, đã tách thành lớp
`QuyTacGioCaoDiem` — nơi **duy nhất** định nghĩa. Viết trùng logic ở hai nơi là con
đường chắc chắn dẫn tới sửa một chỗ mà quên chỗ kia.

```java
public static boolean apDung(LoaiDichVu loaiDichVu, LocalDateTime thoiDiem) {
    if (loaiDichVu != LoaiDichVu.THOAI || thoiDiem == null) return false;
    DayOfWeek thu = thoiDiem.getDayOfWeek();
    boolean ngayLamViec = (thu != DayOfWeek.SATURDAY && thu != DayOfWeek.SUNDAY);
    int gio = thoiDiem.getHour();
    return ngayLamViec && gio >= 8 && gio < 20;
}
```

### 4.2. Kiểm tra chồng khoảng hiệu lực viết theo hướng phủ định

Hai dòng bảng giá cùng bộ khoá (gói cước, dịch vụ, hướng, khung giờ) mà khoảng hiệu
lực đè lên nhau sẽ khiến engine Phase 4 gặp hai đơn giá cùng áp dụng cho một thời
điểm.

Thay vì liệt kê từng kiểu chồng (trùng khít, chồng đầu, chồng cuối, lồng nhau) rồi
ghép bằng OR, hàm được viết theo hướng phủ định — gọn và ít sai hơn:

```java
boolean mot_ketThucTruocHai = ketThuc1 != null && ketThuc1.isBefore(batDau2);
boolean hai_ketThucTruocMot = ketThuc2 != null && ketThuc2.isBefore(batDau1);
return !(mot_ketThucTruocHai || hai_ketThucTruocMot);
```

Ngày kết thúc `null` nghĩa là vô thời hạn, tức khoảng đó không bao giờ "kết thúc
trước" khoảng nào. Có **9 unit test** riêng cho hàm này.

### 4.3. Ba mức tối ưu khi ghi hàng loạt

Chi tiết và số đo trong `docs/toi-uu-hieu-nang.md`. Tóm tắt ba mức:

1. Bỏ `repository.saveAll()`, dùng `JdbcTemplate` — bỏ chi phí persistence context
2. Gom lô 500 bản ghi — giảm số vòng đi lại tới CSDL
3. `rewriteBatchedStatements=true` — giảm số câu lệnh MySQL phải phân tích

Mức 3 **dễ bị bỏ sót nhất** vì tham số nằm ở chuỗi kết nối chứ không nằm trong code.
Người đọc code `batchUpdate()` sẽ tưởng đã tối ưu xong.

### 4.4. Chặn trùng ở tầng nghiệp vụ dù CSDL đã có ràng buộc

`ky_cuoc` đã có `UNIQUE(thang, nam)`, nhưng service vẫn kiểm tra trước. Nếu để CSDL
bắt thì người dùng nhận về `DataIntegrityViolationException` khó hiểu thay vì thông
báo tiếng Việt rõ nghĩa. Cùng cách làm với mã gói cước và số giấy tờ khách hàng.

---

## 5. Kết quả nghiệm thu

### 5.1. Đối chiếu 10 tiêu chí

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvnw test` PASS, tối thiểu 30 test | ✅ | **30 test**, 0 lỗi |
| 2 | `ma_kh` 6 chữ số, thứ tự chuỗi = thứ tự số | ✅ | 50/50 khớp `^KH[0-9]{6}$`, min `KH000001`, max `KH000050` |
| 3 | Sinh 5000 CDR dưới 10 giây, có số đo trước/sau | ✅ | 1150 ms → **257 ms** |
| 4 | Non-THOAI mang cờ giờ cao điểm = 0 | ✅ | **0** (kiểm cả sau khi import CSV) |
| 5 | Không CDR nào sớm hơn `ngay_kich_hoat` | ✅ | **0** |
| 6 | Import `mau-cdr.csv` → 17 thành công / 3 lỗi | ✅ | Đúng 17/3, kèm số dòng và lý do |
| 7 | Không tạo được bảng giá chồng khoảng | ✅ | 0 cặp chồng trong CSDL, thao tác thử bị chặn |
| 8 | Tra cứu, lọc, phân trang CDR; Excel mở được | ✅ | 25 dòng/trang, file .xlsx hợp lệ |
| 9 | Tồn tại 3 kỳ cước 5, 6, 7/2026 | ✅ | `5/2026, 6/2026, 7/2026` |
| 10 | Toàn bộ CDR `cuoc_phi IS NULL` và `CHUA_TINH` | ✅ | 5017/5017 |

### 5.2. Phân bố dữ liệu CDR sinh ra

Kiểm chứng quy tắc sinh trên 5000 bản ghi:

| Loại dịch vụ | Số lượng | Tỷ lệ | Mục tiêu |
|---|---|---|---|
| THOAI | 3509 | 70,2% | 70% |
| SMS | 980 | 19,6% | 20% |
| DATA | 511 | 10,2% | 10% |

| Hướng | Số lượng | Tỷ lệ | Mục tiêu |
|---|---|---|---|
| NOI_MANG | 2739 | 54,8% | 55% |
| NGOAI_MANG | 2027 | 40,5% | 40% |
| QUOC_TE | 234 | 4,7% | 5% |

**Điều kiện test prorate ở Phase 4** — năm thuê bao kích hoạt giữa tháng 6/2026 có
bản ghi sớm nhất đúng bằng ngày kích hoạt:

| Số thuê bao | Ngày kích hoạt | Số CDR | CDR sớm nhất |
|---|---|---|---|
| 0967901278 | 05/06/2026 | 90 | 05/06/2026 |
| 0834567834 | 11/06/2026 | 76 | 11/06/2026 |
| 0978012379 | 15/06/2026 | 75 | 15/06/2026 |
| 0845678935 | 17/06/2026 | 97 | 17/06/2026 |
| 0819123480 | 23/06/2026 | 77 | 23/06/2026 |

### 5.3. Số đo hiệu năng

| Số bản ghi | Chưa bật `rewriteBatchedStatements` | Đã bật | Cải thiện |
|---|---|---|---|
| 5 000 | 1150 ms (trung bình 3 lần) | **257 ms** (trung bình 3 lần) | ~4,5 lần |
| 20 000 | 4421 ms | **1145 ms** | ~3,9 lần |

Điểm đáng chú ý: **cả hai phương án đều đạt tiêu chí "dưới 10 giây"**. Nếu chỉ kiểm
tra tiêu chí nghiệm thu thì sẽ không phát hiện ra thiếu sót — phải đo và so sánh có
chủ đích mới thấy.

### 5.4. Quy tắc giờ cao điểm trên dữ liệu import

| Loại dịch vụ | Cờ cao điểm | Số bản ghi | Khung giờ | Các thứ |
|---|---|---|---|---|
| THOAI | 1 | 8 | 08:05 – 19:55 | Thứ Hai đến Thứ Sáu |
| THOAI | 0 | 2 | 10:00, 21:40 | Thứ Bảy, Thứ Ba (ngoài khung) |
| SMS | 0 | 4 | — | — |
| DATA | 0 | 3 | — | — |

### 5.5. Xuất Excel

| Phạm vi | Kích thước | Thời gian | Số dòng trong sheet |
|---|---|---|---|
| Toàn bộ 5017 bản ghi | 320,8 KB | 1145 ms | 5019 (1 tiêu đề + 5017 dữ liệu + 1 tổng) |
| Lọc `nguon=IMPORT_CSV` | 5,2 KB | — | 19 (1 + 17 + 1) |

File được kiểm chứng bằng cách mở lại bằng `ZipArchive`: magic bytes `PK` đúng, có
đủ `xl/workbook.xml`, `xl/worksheets/sheet1.xml`, có dòng `TỔNG CỘNG`.

---

## 6. Sự cố kỹ thuật

### 6.1. Truy vấn tổng hợp trả về `Object[]` bị `ClassCastException`

**Hiện tượng:** màn hình tra cứu CDR trả HTTP 500 ngay lần mở đầu tiên.

```
java.lang.ClassCastException: class [Ljava.lang.Object; cannot be cast to class java.lang.Number
    at ChiTietSuDungServiceImpl.tinhTong(ChiTietSuDungServiceImpl.java:58)
```

**Nguyên nhân:** phương thức repository khai kiểu trả về là `Object[]`. Với truy vấn
tổng hợp một dòng nhiều cột, Spring Data bọc kết quả thành `Object[]{ Object[]{...} }`
— phần tử đầu tiên là một mảng chứ không phải giá trị đầu tiên như mong đợi.

**Cách khắc phục:** đổi kiểu trả về sang `List<Object[]>` rồi lấy phần tử đầu. Khai
theo danh sách thì cấu trúc luôn rõ ràng, không phụ thuộc số dòng trả về.

### 6.2. Nhắc lại bài học kiểm thử từ Phase 2

Lỗi 6.1 bị bắt ngay vì kịch bản kiểm thử **xét mã trạng thái HTTP trước**. Ở Phase 2,
một lỗi HTTP 500 tương tự đã lọt qua nhiều vòng kiểm thử vì script chỉ dò chuỗi trong
HTML — mà trang lỗi cũng dùng layout chung nên vẫn chứa các chuỗi đang tìm.

Việc sửa công cụ kiểm thử ở mục A đã có tác dụng ngay trong chính phiên này.

---

## 7. Cấu trúc dự án

Repo hiện có **153 file**. Phân bố mã nguồn Java:

| Package | Số file | Bổ sung ở Phase 3 |
|---|---|---|
| `controller` | 9 | `GoiCuocController`, `BangGiaController`, `CdrController`, `KyCuocController` |
| `dto` | 11 | `GoiCuocForm`, `GoiCuocDto`, `BangGiaForm`, `SinhCdrForm`, `KetQuaSinhCdr`, `KetQuaImportCdr`, `BoLocCdr`, `TongHopCdr`, `KyCuocForm` |
| `service` + `impl` | 16 | `GoiCuocService`, `BangGiaCuocService`, `ChiTietSuDungService`, `KyCuocService`, `SinhMaService` |
| `service/rating` | 3 | `CdrGeneratorService`, `CdrImportService`, `QuyTacGioCaoDiem` |
| `validation` | 5 | `@KhoangHieuLucHopLe` + validator + interface `CoKhoangHieuLuc` |
| `util` | 2 | `DinhDangCdr` |
| `entity` · `enums` · `repository` | 15 · 16 · 15 | Không đổi số lượng, bổ sung truy vấn |

Template Thymeleaf: **22 file** trong 8 thư mục.

Kiểm thử: 4 lớp, **30 test**.

| Lớp test | Số test | Nội dung |
|---|---|---|
| `SchemaValidationTest` | 1 | Đối chiếu 15 entity với schema thật |
| `ThueBaoServiceTest` | 17 | Ma trận chuyển trạng thái 4×4 + ràng buộc lý do |
| `BangGiaCuocServiceTest` | 9 | Chồng khoảng hiệu lực |
| `SinhMaServiceTest` | 3 | Sinh mã khách hàng |

Lịch sử Git — 7 commit theo đúng từng mục:

| Commit | Nội dung |
|---|---|
| `2837c5c` | Phase 3A: dọn nợ Phase 2 |
| `a018690` | Phase 3B: quản lý gói cước |
| `af5db33` | Phase 3C: bảng giá cước và kiểm tra chồng khoảng |
| `9cd58cf` | Phase 3D: bộ sinh CDR giả lập |
| `e12f48e` | Phase 3E: nhập CDR từ file CSV |
| `97cfa3c` | Phase 3F: tra cứu CDR và xuất Excel |
| `1a7f462` | Phase 3G: quản lý kỳ cước |

---

## 8. Hướng phát triển

### 8.1. Bổ sung bước trung gian khi khôi phục thuê bao bị khoá hai chiều

Ma trận hiện tại không cho `TAM_NGUNG_2C → TAM_NGUNG_1C`. Trong nghiệp vụ viễn thông
thực tế, khách hàng bị chặn hai chiều do nợ cước sau khi **thanh toán một phần**
thường được mở lại một chiều trước (cho phép nhận cuộc gọi, chưa cho gọi đi), rồi mới
khôi phục hoàn toàn khi trả hết nợ.

Hệ thống hiện chưa mô phỏng bước trung gian này. Muốn bổ sung thì cần:

- Thêm ô `TAM_NGUNG_2C → TAM_NGUNG_1C` vào ma trận trong `ThueBaoServiceImpl`
- Thêm điều kiện nghiệp vụ: chỉ cho phép khi đã có thanh toán một phần cho công nợ
  (phụ thuộc bảng `thanh_toan` và `hoa_don` — thuộc Phase 5)
- Bổ sung test cho ô mới, ma trận thành 9 hợp lệ / 7 bị chặn

Đây cũng chính là lý do đặc tả ban đầu ghi 9/7 — con số đó phản ánh nghiệp vụ đầy đủ,
còn cài đặt hiện tại phản ánh phạm vi đã thống nhất ở Phase 2.

### 8.2. Các việc khác

- **Kỳ cước 5/2026 đang để trạng thái `MO`** dù đã là quá khứ. Việc chốt kỳ thuộc
  Phase 4; khi có chức năng đó nên chốt kỳ 5 để dữ liệu phản ánh đúng thực tế.
- **Chưa có màn hình sửa/xoá bản ghi CDR lẻ.** Hiện chỉ sinh, nhập và tra cứu. Nếu
  nghiệp vụ cần đính chính một bản ghi sai thì phải bổ sung ở phase sau.
- **File CSV mẫu tồn tại hai bản** — `src/main/resources/mau-cdr.csv` (bản phục vụ
  nút tải về, đọc từ classpath nên chạy được cả khi đóng gói JAR) và
  `docs/mau-cdr.csv` (bản đưa vào báo cáo). Hai bản có nội dung giống nhau; nếu sửa
  thì phải sửa cả hai.

---

## 9. Kế hoạch Phase tiếp theo

| Phase | Nội dung |
|---|---|
| **4** | Engine tính cước — Rating và Billing (phần lõi) |
| 5 | Hóa đơn, thanh toán, công nợ |
| 6 | Báo cáo, thống kê, dashboard |
| 7 | Hoàn thiện, kiểm thử, tài liệu |

**Ba việc Phase 4 phải làm ngay từ đầu:**

1. Đọc `docs/mo-ta-csdl.md` **mục 6** trước khi viết dòng code tính cước nào — hai
   chỗ quy đổi KB/MB
2. Tra giá theo **ngày phát sinh của CDR**, không phải ngày hiện tại — bảng giá có
   khoảng hiệu lực, xem màn hình `/bang-gia/tra-cuu` để hình dung
3. Ưu tiên dòng giá gắn với gói cước cụ thể trước dòng giá mặc định chung

---

## 10. Danh sách màn hình nên chụp ảnh cho báo cáo

Trước khi chụp nên chạy `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"` rồi
sinh lại 5000 CDR để dữ liệu sạch đẹp. Đăng nhập bằng tài khoản `admin`.

### Nhóm 1 — Gói cước và bảng giá

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 1 | Danh sách gói cước | `/goi-cuoc` | Cột tóm tắt ưu đãi dạng "100 phút NM · 30 SMS · 2 GB", cột số thuê bao đang dùng |
| 2 | Chặn xoá gói đang dùng | Mở gói DN500, bấm Xoá | Thông báo "đang được 31 thuê bao sử dụng, không thể xoá" |
| 3 | Chi tiết gói cước | `/goi-cuoc/4` | Bảng giá riêng + danh sách thuê bao đang dùng có phân trang |
| 4 | Danh sách bảng giá | `/bang-gia` | Dòng "Giá mặc định chung", cột khung giờ có badge Cao điểm |
| 5 | **Chặn chồng khoảng hiệu lực** | Thêm dòng THOAI/NOI_MANG/thường hiệu lực 01/06/2026 | Thông báo chỉ rõ dòng xung đột kèm mã số, khoảng hiệu lực và đơn giá |
| 6 | Xem giá đang áp dụng | `/bang-gia/tra-cuu?ngay=2026-06-15` | Minh hoạ cơ chế giá theo thời gian |
| 7 | So sánh hai ngày tra cứu | Chụp thêm `ngay=2024-01-01` (ra 0 đơn giá) | Đặt cạnh ảnh 6 để thấy rõ giá có hiệu lực từ 01/01/2025 |

### Nhóm 2 — Bộ sinh CDR ⭐

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 8 | Form sinh dữ liệu | `/cdr/sinh-du-lieu` | Khối "Quy tắc sinh dữ liệu" bên trái, đặc biệt dòng giờ cao điểm chỉ cho THOẠI |
| 9 | **Kết quả sinh 5000 bản ghi** | Bấm Sinh dữ liệu | Ba ô thống kê (số bản ghi, thuê bao phát sinh, **thời gian ms**) + hai bảng phân bố |
| 10 | Trạng thái đang xử lý | Chụp ngay khi vừa bấm | Nút chuyển thành "Đang sinh dữ liệu, vui lòng đợi..." |

> Ảnh 9 là ảnh quan trọng nhất của Phase 3 — nên phóng to phần thời gian và bảng phân bố.

### Nhóm 3 — Nhập CDR từ CSV

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 11 | Form nhập CSV | `/cdr/import` | Bảng mô tả 7 cột, ghi chú DATA đơn vị KB, ghi chú cờ cao điểm do hệ thống tự tính |
| 12 | **Kết quả 17/3** | Tải file mẫu rồi nhập lại | Ba ô thống kê + bảng liệt kê 3 dòng lỗi kèm số dòng và lý do |
| 13 | Nội dung file mẫu | Mở `docs/mau-cdr.csv` bằng Notepad | 3 dòng cuối là 3 dòng cố ý sai |

### Nhóm 4 — Tra cứu CDR

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 14 | Danh sách CDR | `/cdr` | 25 dòng/trang, **dòng tổng cuối bảng**, cột cước phí để trống |
| 15 | Lọc theo dịch vụ DATA | `/cdr?loaiDichVu=DATA` | Cột số lượng hiện MB (đã quy đổi từ KB) |
| 16 | Lọc theo nguồn | `/cdr?nguon=IMPORT_CSV` | 17 bản ghi, cột Nguồn hiện IMPORT_CSV |
| 17 | Lọc kết hợp | `/cdr?loaiDichVu=THOAI&huong=QUOC_TE` | Nhiều điều kiện cùng lúc, dòng tổng đổi theo |
| 18 | File Excel đã xuất | Bấm Xuất Excel rồi mở bằng Excel | Dòng tiêu đề nền xám, dòng TỔNG CỘNG cuối bảng |
| 19 | Liên kết từ chi tiết thuê bao | Mở `/thue-bao/1`, bấm "Xem chi tiết sử dụng" | Sang `/cdr` với bộ lọc đặt sẵn theo số thuê bao |

### Nhóm 5 — Kỳ cước

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 20 | Danh sách kỳ cước | `/ky-cuoc` | Ba kỳ 5, 6, 7/2026 với khoảng ngày tự tính đúng |
| 21 | Chặn tạo kỳ trùng | Tạo lại kỳ 6/2026 | Thông báo "Kỳ cước tháng 6/2026 đã tồn tại, không thể tạo trùng" |

### Nhóm 6 — Minh chứng kỹ thuật

| # | Ảnh | Cách lấy |
|---|---|---|
| 22 | **Kết quả 30 test** | Console `mvnw test`, phóng to phần `Tests run: 30, Failures: 0` |
| 23 | **Chi tiết test ma trận trạng thái** | Chạy test trong IntelliJ, chụp cây kết quả có `@DisplayName` tiếng Việt |
| 24 | Test chồng khoảng hiệu lực | Cùng cách, chụp 9 test của `BangGiaCuocServiceTest` |
| 25 | Số đo hiệu năng | Hai ảnh kết quả sinh 5000 bản ghi: trước và sau khi bật `rewriteBatchedStatements` |
| 26 | SQL kiểm chứng tiêu chí 4 | Câu `SELECT COUNT(*) ... gio_cao_diem = 1` trả về 0 |
| 27 | SQL kiểm chứng tiêu chí 10 | Câu đếm `cuoc_phi IS NULL` và `CHUA_TINH` — cả hai bằng 5017 |
| 28 | Lịch sử Git 7 commit Phase 3 | `git log --oneline -7` |

> Ảnh 23 là ảnh nên đưa vào phần trình bày về kiểm thử: 16 `@DisplayName` tiếng Việt
> mô tả rõ từng ô của ma trận, người chấm nhìn là hiểu ngay logic nghiệp vụ.

---

## 11. Tổng kết

Phase 3 hoàn thành đủ 10/10 tiêu chí nghiệm thu. Hệ thống đã có danh mục gói cước,
bảng giá quản lý theo thời gian, hai đường nạp dữ liệu CDR (sinh giả lập và nhập CSV),
màn hình tra cứu kèm xuất Excel, và quản lý kỳ cước — tức toàn bộ đầu vào mà engine
tính cước ở Phase 4 cần.

Nợ kỹ thuật Phase 2 được xử lý dứt điểm trước khi viết nghiệp vụ mới, trong đó việc
sửa chính công cụ kiểm thử đã mắc lỗi (mục 2.3) tỏ ra có giá trị ngay: lỗi HTTP 500 ở
mục 6.1 bị bắt ngay lần chạy đầu tiên thay vì lọt qua như ở Phase 2.

Hai điểm sai lệch đặc tả đã được ghi lại đầy đủ thay vì âm thầm làm theo hoặc âm thầm
làm khác: ma trận 8/8 (mục 3.1) và hai chỗ quy đổi đơn vị DATA (mục 3.2). Điểm thứ hai
đặc biệt quan trọng vì nếu bỏ sót ở Phase 4 thì hóa đơn vẫn phát hành bình thường
nhưng sai số tiền khoảng 1024 lần — loại lỗi không có cảnh báo nào để phát hiện.

Về hiệu năng, con số 1150 ms → 257 ms cho thấy một điều đáng suy nghĩ: cả hai phương
án đều "đạt" tiêu chí dưới 10 giây. Nếu chỉ dừng ở việc kiểm tra tiêu chí thì thiếu
sót đã không bao giờ lộ ra.

Sẵn sàng bước sang Phase 4.
