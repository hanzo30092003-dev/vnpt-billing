# RÀ SOÁT TÍNH NĂNG — ĐỦ CHƯA, THIẾU GÌ, THIẾU ĐÓ CÓ ĐÁNG LÀM KHÔNG

> Rà soát **chỉ đọc**. Không sửa file mã nguồn nào, chỉ tạo đúng tài liệu này. Không commit.
> Ứng dụng chạy ở profile mặc định suốt quá trình đo — **không** dùng `reset`. Không sinh, không
> xoá dòng dữ liệu nào.

Ngày rà: 27/08/2026 · Bản `a47f312`

---

## Số nền — tự đo, không lấy từ tài liệu

Đề bài dặn đúng: tài liệu dự án này đã lệch nhiều lần. Mọi con số dưới đây đo bằng lệnh.

| | Đo được | Cách đo |
|---|---|---|
| Template `.html` | **46** | `find templates -name '*.html' \| wc -l` |
| Test chạy thật | **315** | đếm `<testcase>` trong surefire XML **và** `mvnw test` — hai cách độc lập cùng ra 315 |
| Lớp test | **32** | 33 file `.java` trong `src/test`, trừ `TrichVanBanPdf` (0 `@Test`, là lớp trợ giúp) |
| Controller | **16** | `find -path '*controller*'` |
| Đường dẫn (route) | **74** | quét `@GetMapping` / `@PostMapping` |
| Script giao diện | **11** file, **215** phép kiểm | chạy thật, đếm `[DAT ]` |
| Kỳ cước | **6** — 3,4,5/2026 `DA_CHOT`; 6,7,8/2026 `MO`; kỳ 8 rỗng | SQL |

### Ba phép đếm sai của chính lượt này, đã bắt được

1. **`@Test` = 304** nhưng suite báo 315 — chênh 11 do `@ParameterizedTest` (4 cái) nở ra nhiều
   ca. `@Test` không dùng làm số test được.
2. **Tổng `.txt` surefire = 109** — sai nặng. 13 lớp có `@Nested` ghi `Tests run: 0` ở dòng đầu
   vì lớp ngoài không có test trực tiếp. Suýt nữa báo cáo con số 109.
3. **Thuộc tính `tests=` trong XML = 306** — vẫn hụt 9 so với thực tế. Chỉ đếm phần tử
   `<testcase>` mới ra đúng **315**, và trùng khớp với `mvnw test`.

---

## BƯỚC 0 — Kiểm kê skill

| Skill | Dùng cho |
|---|---|
| `product-capability` | Bước 1 — dựng ma trận chức năng từ ý định nghiệp vụ |
| `domain-modeling` | Bước 1, 3 — vòng đời thuê bao, thuật ngữ |
| `superpowers:verification-before-completion` | Bước 2 — bốn mức xác minh |
| `engineering:testing-strategy` | Bước 2 — phân biệt phủ mã và phủ hành vi |
| `codebase-design` | Bước 2 — đọc seam controller/service |

**Bỏ, kèm lý do cụ thể:**

| Skill | Vì sao không dùng |
|---|---|
| `engineering:architecture` · `system-design` | Đề bài **cấm đổi kiến trúc**; phần phân tích thì trùng `codebase-design` |
| `market-research` · `competitive-platform-analysis` | Nguồn B là nghiệp vụ viễn thông chuẩn, không phải phân tích đối thủ |
| `tdd` · `tdd-workflow` | Đây là rà **chỉ đọc**, không viết mã |
| `e2e-testing` | Đòi Playwright — cấm thêm phụ thuộc |
| `qa` | Nó mở phiên báo lỗi lên GitHub; đề bài cấm commit |

### Mâu thuẫn phải nêu

`product-capability` yêu cầu chốt *interfaces và invariants* rồi đề xuất bổ sung chỗ hở.
`CLAUDE.md` **cấm chạm** `service/`, `repository/`, `entity/`, `dto/`, `db/`. Cách xử lý: vẫn
nêu chỗ hở, nhưng **đánh dấu rõ** mục nào chạm tầng được bảo vệ và không tự sửa.

---

## BƯỚC 1 — Ma trận chức năng từ ba nguồn

**A** = phạm vi đã cam kết · **B** = nghiệp vụ viễn thông thực tế · **C** = đã cài đặt thật

| Chức năng | A | B | C | Ghi chú |
|---|:-:|:-:|:-:|---|
| Quản lý khách hàng (CRUD, lọc, ngừng giao dịch) | đầy đủ | ✓ | ✅ | `KhachHangController` 5 route |
| Quản lý thuê bao (đăng ký, 4 trạng thái, đổi gói) | đầy đủ | ✓ | ✅ | `ThueBaoController` 6 route |
| Gói cước và bảng giá | đầy đủ | ✓ | ✅ | `GoiCuocController` 5 · `BangGiaController` 5 |
| Ghi nhận sử dụng — sinh CDR | đầy đủ | ✓ | ✅ | `CdrGeneratorService`, có hạt giống |
| Ghi nhận sử dụng — **nhập CDR từ CSV** | đầy đủ | ✓ | ✅ | **27 phép kiểm** thêm ở G6 (trước là 0) |
| Engine tính cước (rating) | đầy đủ | ✓ | ✅ | `RatingService` 23 test |
| Lập hóa đơn (billing) | đầy đủ | ✓ | ✅ | `BillingService` 30 test |
| Prorate cước thuê bao theo ngày | đầy đủ | ✓ | ✅ | `BillingService:218`, 6 hóa đơn thật |
| Ưu đãi gói cước, quota | đầy đủ | ✓ | ✅ | `UuDaiGoiCuoc` 16 test |
| Trừ cước trả trước | đầy đủ | ✓ | ✅ | 16 dòng `TRU_CUOC` thật |
| **Nạp tiền trả trước** | đầy đủ | ✓ | ⚠️ | có mã + form + script, **0 dòng dữ liệu** — xem 🔴-3 |
| Hóa đơn: xem, PDF, Excel | đầy đủ | ✓ | ✅ | `HoaDonPdfService` 9 test |
| Ghi nhận thanh toán, phiếu thu | đầy đủ | ✓ | ✅ | `ThanhToanService` 11 test |
| Công nợ, bảng tuổi nợ | đầy đủ | ✓ | ✅ | 263 dòng thật |
| Hạn mức tín dụng | cơ bản | ✓ | ✅ | V2 làm sống lại; câu tài liệu sai **đã sửa** 27/08 |
| Giảm trừ hóa đơn | cơ bản | ✓ | ✅ | 2 dòng thật |
| Đối soát cước | đầy đủ | ✓ | ✅ | `DoiSoatCuocService` 10 test |
| 7 báo cáo thống kê | đầy đủ | ✓ | ✅ | `BaoCaoController` 14 route |
| Quản lý người dùng, phân quyền | *thêm* | ✓ | ✅ | **có ở C mà không ở A** — đợt V3a |
| Đổi mật khẩu, khoá tài khoản | *thêm* | ✓ | ✅ | **có ở C mà không ở A** — đợt V3b |
| Nhật ký thao tác | *thêm* | ✓ | ✅ | **có ở C mà không ở A** |
| **Nhắc nợ (dunning)** | không làm | ✓ | ❌ | đã ghi — xem NHÓM 2 |
| Tính lãi chậm trả | không làm | ✓ | ❌ | `PHASE-5-REPORT.md:700` |
| Tự động cắt dịch vụ | không làm | ✓ | ❌ | `PHASE-5-REPORT.md:698` |
| Quản lý kho số, hợp đồng | không làm | ✓ | ❌ | `DANH-GIA-HE-THONG.md:247` |
| Roaming, đối soát liên mạng | không làm | ✓ | ❌ | `PHASE-4-PLAN.md`, `DANH-GIA:245` |
| Chuyển mạng giữ số (MNP) | — | ✓ | ❌ | NHÓM 3 |
| Cổng thanh toán thật | không làm | ✓ | ❌ | `DANH-GIA-HE-THONG.md:279` |
| Tính cước thời gian thực | không làm | ✓ | ❌ | ghi ở 6 tài liệu |
| Hóa đơn điện tử có mã cơ quan thuế | không làm | ✓ | ❌ | `DANH-GIA-HE-THONG.md:279` |

### Ba chức năng có ở C mà không ở A — đáng nêu trong báo cáo

Phân hệ **quản trị người dùng** (25 test), **đổi mật khẩu / khoá tài khoản đá phiên đang mở**,
và **nhật ký thao tác**. Cả ba nằm ngoài sáu phân hệ cam kết ban đầu, làm thêm ở các đợt V3a/V3b.

---

## BƯỚC 2 — Bốn mức xác minh

> Dự án từng có `/giam-tru` nghiệm thu "đạt" với 13/13 dự đoán đúng trong khi **trang chưa bao
> giờ render**. Nên mức 3 đo bằng cách **bò theo `href`** đọc từ HTML, không gõ URL; mức 4 đếm
> số `<tr>` thật sự dựng ra, bỏ dòng `colspan` (dòng "không có dữ liệu").

### Mức 3 — bò theo menu

Xuất phát từ `/`, chỉ đi theo `href` đọc được từ HTML trả về: **791 đường tới được**, tất cả
trả 200 trừ ba đường — cả ba đến từ **vệt bánh mì**, không phải menu.

### Mức 4 — chạy với dữ liệu thật

| Màn hình | Mã | Số dòng dựng |
|---|:-:|--:|
| Khách hàng · Thuê bao | 200 | 15 · 15 |
| Gói cước · Bảng giá · CDR | 200 | 5 · 10 · 25 |
| Kỳ cước · Tính cước | 200 | 6 · 12 |
| Hóa đơn · Thanh toán | 200 | 20 · 20 |
| **Công nợ** | 200 | **263** |
| **Giảm trừ** | 200 | **2** |
| 7 báo cáo | 200 | 6 · 15 · 10 · 7 · 6 · 4 · 9 |
| Người dùng | 200 | 4 |

**19/19 màn hình dựng dòng thật — 0 màn hình rỗng.** Kể cả `/giam-tru`, đúng 2 dòng khớp 2
giảm trừ trong CSDL. Mức 4 đạt toàn bộ.

### 🔴 Ba lỗ hổng "tạo ảo giác đã xong"

#### 🔴-1 — Vệt bánh mì có mắt xích bấm ra 404 · **ĐÃ SỬA 27/08**

Kiểm **12 mắt xích khác nhau** trên các màn hình có đường dẫn nhiều đoạn:

| Trên màn hình | Bấm mắt xích | Đi tới | Kết quả |
|---|---|---|---|
| `/thanh-toan/moi/461` *(form thu tiền)* | "Thêm mới" | `/thanh-toan/moi` | **404** |
| `/tinh-cuoc/ky/3` | "Hóa đơn của kỳ" | `/tinh-cuoc/ky` | **404** |

Gốc: `LayoutAdvice.breadcrumb()` suy mắt xích từ từng đoạn đường dẫn và bỏ qua đoạn thuần số,
nhưng `/thanh-toan/moi` không có controller nào phục vụ.

Nặng vì **màn hình ghi nhận thanh toán nằm trong đường demo**: kế toán mở form thu tiền, nhìn
lên vệt bánh mì, bấm — ra trang lỗi.

> **Đối chứng âm** cho phép kiểm này: cho nó 2 đường chắc chắn sống và 3 đường chắc chắn chết —
> khớp đúng cả 5/5. Nó phân biệt được hai chiều, nên con số "2 hỏng / 12" là thật.

#### 🔴-2 — Nhập CDR từ CSV: có mã, có màn hình, tới được từ menu, **0 phép kiểm** · **ĐÃ SỬA 27/08**

| Mức | Kết quả |
|---|---|
| 1. Có mã nguồn | ✅ `CdrImportService`, route `/cdr/import` |
| 2. **Có phép kiểm** | ❌ **0 test Java, 0/8 script HTTP nhắc tới** |
| 3. Tới được từ menu | ✅ có `href="/cdr/import"` trên `/cdr` |
| 4. Chạy với dữ liệu thật | ✅ trang trả 200 |

Đây là chức năng **duy nhất** trong phạm vi cam kết không có bất kỳ phép kiểm nào. Phép grep
phân biệt được — cùng cách đó tìm ra `/khach-hang` được 4 script phủ, `xuat-excel` được 5 script
phủ. Con số 0 là thật, không phải grep hỏng.

*(Không chạy thử import vì sẽ ghi dữ liệu vào CSDL — đề bài cấm.)*

#### 🔴-3 — Nạp tiền trả trước: chưa từng chạy trên bộ dữ liệu này · **ĐÃ SỬA 27/08**

| Loại biến động số dư | Số dòng |
|---|--:|
| `DIEU_CHINH` | 18 |
| `TRU_CUOC` | 16 |
| **`NAP_TIEN`** | **0** |

Số dư ban đầu của 20 thuê bao trả trước được nạp bằng **điều chỉnh**, không phải nạp tiền. Chức
năng `napTien` có mã (`ThueBaoController:147`), có form trên màn hình chi tiết, có `test-tb.ps1`
phủ ở tầng HTTP — nhưng **không dòng lịch sử nào** để chỉ vào khi demo.

### Mức 2 — bảy service không có test Java

| Service | Được phủ ở tầng khác? |
|---|---|
| `KhachHangService` | ✅ `test-kh.ps1` (14 phép kiểm) |
| `GoiCuocService` | ✅ 4 script nhắc tới |
| `BaoCaoExcel` | ✅ `test-bao-cao.ps1` tải 11 file Excel |
| `ChiTietSuDungService` | ✅ `test-ky-rong.ps1` |
| `ChanTrangPdf` | ✅ gián tiếp qua 3 test PDF |
| `QuyTacKyCuoc` | ✅ gián tiếp qua `BillingServiceTest` |
| **`CdrImportService`** | ❌ **không chỗ nào** — 🔴-2 |

Sáu trong bảy được phủ ở tầng HTTP. "Không có test Java" ≠ "không được kiểm" — chỉ `CdrImport`
là thật sự trống.

---

## BƯỚC 3 — Năm kịch bản vòng đời

### 1. Khách mới → thuê bao → sử dụng → tính cước → hóa đơn → thu tiền

**ĐI TRỌN ĐƯỢC.** Mọi mắt xích tới được từ menu và có dữ liệu thật: 50 khách hàng · 80 thuê bao
· 18.723 CDR · 280 hóa đơn · 161 thanh toán.

**Một chỗ vướng, không đứt:** lối tắt trang chủ ghi *"Ghi nhận thanh toán"* nhưng dẫn tới
`/cong-no`, màn hình chứa **0** lần chuỗi đó. Đường thật tốn **5 lần bấm** trong khi bốn việc
hằng ngày còn lại chỉ tốn 1. Đã ghi ở `RA-SOAT-GIAO-DIEN.md` Đ12, cố ý để ngoài phạm vi G4.

### 2. Nợ → quá hạn → nhắc nợ → tạm ngừng → trả tiền → khôi phục

**ĐỨT ĐÚNG MỘT MẮT XÍCH: "nhắc nợ".** Không có chức năng gửi thư, tin nhắn hay in giấy báo nợ.
Hệ thống nhảy thẳng từ *quá hạn* sang `deXuatTamNgung()`.

Các mắt xích còn lại đủ và tới được:

| Mắt xích | Bằng chứng |
|---|---|
| Quá hạn | `capNhatQuaHan()` chạy mỗi lần mở `/hoa-don`; 148 hóa đơn quá hạn |
| Tạm ngừng | `TAM_NGUNG_1C` 8 thuê bao · `TAM_NGUNG_2C` 4 thuê bao |
| Trả tiền | 161 thanh toán |
| **Khôi phục** | ✅ ma trận `ThueBaoServiceImpl:52-55` cho `TAM_NGUNG_1C/2C → HOAT_DONG`; mở thuê bao id=5 thì giao diện **mời đúng** lựa chọn *Hoạt động* |

*(Ban đầu tôi tưởng "khôi phục" cũng thiếu vì grep chữ "khôi phục" ra 0 file. Sai — nó không
phải một chức năng riêng mà là một nước đi của `chuyenTrangThai`.)*

### 3. Đổi gói cước giữa chu kỳ → kỳ sau tính đúng gói mới

**ĐI TRỌN ĐƯỢC.** Nút *"Đổi gói cước"* + *"Xác nhận đổi gói"* có trên màn hình chi tiết thuê
bao. Quyết định 5.10 (`PHASE-4-PLAN.md:378`) đã chốt lấy gói từ đâu.

### 4. Trả trước: nạp tiền → dùng → trừ cước → hết tiền

**ĐỨT Ở MẮT XÍCH ĐẦU** — xem 🔴-3. Ba mắt xích sau đủ dữ liệu: 16 dòng `TRU_CUOC`, 2 thuê bao
trả trước còn **0 đồng**. Chỉ *nạp tiền* là chưa có dòng nào.

### 5. Thanh lý giữa kỳ → hóa đơn cuối kỳ prorate đúng

**ĐI TRỌN ĐƯỢC, và xác minh được bằng dữ liệu thật.** 6/280 hóa đơn có cước thuê bao khác giá
gói, tức prorate đã chạy:

| Thuê bao | Gói | Giá tháng | Hóa đơn ghi | Kỳ | Trạng thái |
|---|---|--:|--:|---|---|
| 0823456733 | Cơ bản | 50.000 | **32.258** | 5/2026 | **`DA_THANH_LY`** |
| 0834567834 | MAX70 | 70.000 | 46.667 | 6/2026 | `HOAT_DONG` |
| 0845678935 | MAX150 | 150.000 | 70.000 | 6/2026 | `HOAT_DONG` |
| 0967901278 | Doanh nghiệp 500 | 500.000 | 433.333 | 6/2026 | `HOAT_DONG` |

Dòng đầu là **đúng nguyên kịch bản 5**: thuê bao thanh lý giữa kỳ, hóa đơn kỳ đó thu 32.258 đ
thay vì 50.000 đ.

---

## BƯỚC 4 — Phân loại thiếu sót

### NHÓM 1 — Thiếu và ảnh hưởng tới demo

| | Vì sao hội đồng có thể chạm tới |
|---|---|
| 🔴-1 Vệt bánh mì 404 | Nằm trên **màn hình thu tiền**, ngay trong đường demo. Bấm một cái là ra trang lỗi |
| 🔴-3 Không có lịch sử nạp tiền | Hỏi *"cho xem một lần khách nạp thẻ"* thì không có dòng nào để chỉ; phải nạp trực tiếp trên sân khấu |
| 🔴-2 Nhập CSV không phép kiểm nào | Hỏi *"cái này kiểm thế nào"* thì đây là chỗ duy nhất trả lời được là "chưa" |
| Lối tắt "Ghi nhận thanh toán" dẫn sai chỗ | Đã ghi Đ12; 5 lần bấm trong khi việc khác 1 lần |

### NHÓM 2 — Thiếu nhưng đã cố ý loại, **có ghi**

| Mục | Ghi ở đâu |
|---|---|
| Nhắc nợ tự động (dunning) | `DANH-GIA-HE-THONG.md:246` · `PHASE-5-REPORT.md:699` |
| Tính lãi chậm trả | `PHASE-5-REPORT.md:700` |
| Tự động cắt dịch vụ | `PHASE-5-REPORT.md:698` |
| Quản lý kho số, hợp đồng | `DANH-GIA-HE-THONG.md:247` |
| Bộ lọc theo khách hàng cho báo cáo | `PHASE-7-REPORT.md:158` — ghi rõ *"Không làm — vi phạm ràng buộc của phase"* |
| `/cdr` không lọc theo tháng | `PHASE-8-REPORT.md:437` |
| Cột tổng cuối bảng (yêu cầu ④ gạch 3) | `PHASE-8-REPORT.md:419` |

**Không tìm thấy mục nào bị loại mà chưa được ghi ở đâu cả.** Đây là điểm mạnh đáng nêu: mọi
thứ cố ý không làm đều có vết.

**Nhưng tìm thấy chiều ngược lại — tài liệu lỗi thời:**
`DANH-GIA-HE-THONG.md:248` ghi *"Cưỡng chế hạn mức tín dụng — cột `hanMucTinDung` có lưu nhưng
không chỗ nào dùng để chặn, cột chết"*. **Sai từ đợt V2**: `HoaDonRepository:272-289` dùng nó,
và `/cong-no` dựng bảng hạn mức **4 dòng** thật. Câu đó cần sửa trước khi nộp.

### NHÓM 3 — Không thuộc phạm vi một đồ án thực tập

Roaming và đối soát liên mạng · chuyển mạng giữ số (MNP) · cổng thanh toán thật · tính cước thời
gian thực · hóa đơn điện tử có mã cơ quan thuế · cổng tra cứu cho khách hàng · nhiều chi
nhánh/đơn vị.

---

## BƯỚC 5 — Khuyến nghị

### Năm mục đáng làm, theo thứ tự

| # | Việc | Công sức | Vì sao đáng | Rủi ro đỏ test/script |
|---|---|---|---|---|
| **1** ✅ | Sửa vệt bánh mì 404 — bỏ mắt xích cho đoạn không có controller | **S** | Lỗi duy nhất hội đồng **nhìn thấy** trên đường demo | **Đã làm 27/08.** Hỏi thẳng Spring qua `RequestMappingHandlerMapping` thay vì giữ danh sách khai tay. Đo lại: 0/10 mắt xích hỏng |
| **2** ✅ | Sửa `DANH-GIA-HE-THONG.md:248` — hạn mức không còn là cột chết | **S** | Tài liệu là **sản phẩm nộp** | **Đã làm 27/08** |
| **3** ✅ | Nạp một giao dịch `NAP_TIEN` vào bộ dữ liệu mẫu | **S** | Để kịch bản 4 có dòng lịch sử chỉ vào | **Đã làm 27/08.** Nạp 100.000 đ thẻ cào cho `0818901208` qua giao diện; dump lại mục 6 và sửa công thức mục 7 |
| **4** ✅ | Thêm phép kiểm cho `/cdr/import` | **M** | Chức năng duy nhất trong phạm vi cam kết không có phép kiểm nào | **Đã làm 27/08.** 27 phép kiểm Mockito, không ghi dòng nào vào CSDL |
| **5** ✅ | Đổi chữ lối tắt "Ghi nhận thanh toán" → "Tra cứu để thu tiền" | **S** | Nhãn đang hứa một việc mà màn hình đích không làm | **Đã làm 27/08** |

Mục 1 và 2 nên làm **trước khi chụp 70 ảnh** — mục 1 đổi trang, mục 2 đổi tài liệu sẽ in kèm.

### Không nên làm — viết vào "Hạn chế và hướng phát triển"

| Mục | Vì sao viết thay vì code |
|---|---|
| Nhắc nợ tự động | Cần hạ tầng gửi thư/tin nhắn — ngoài phạm vi, và đã ghi ở 2 tài liệu |
| Tính lãi chậm trả · tự động cắt dịch vụ | Thêm quy tắc nghiệp vụ mới, chạm `service/` và `entity/` sát ngày bảo vệ |
| Bộ lọc khách hàng cho báo cáo | Phase 7 đã cân nhắc và **ghi rõ là không làm** — lật lại là phá một quyết định có lý do |
| Đổi luồng "Ghi nhận thanh toán" thành nhập số thuê bao thẳng | Thiết kế lại luồng nghiệp vụ, cần action mới ở controller. Mục 5 ở trên chữa được 80% giá trị bằng 10 phút |
| Kho số, hợp đồng, roaming, MNP, cổng thanh toán | NHÓM 3 — không thuộc phạm vi đồ án thực tập |

---

## Trả lời thẳng

**Rồi. Phần mềm đã đủ tính năng cốt lõi để bảo vệ đồ án.**

Sáu phân hệ cam kết đều có mã, có phép kiểm, tới được từ menu và chạy với dữ liệu thật — 19/19
màn hình dựng dòng thật, không màn hình nào rỗng. Ba trong năm kịch bản vòng đời đi trọn được;
kịch bản 5 (prorate khi thanh lý giữa kỳ) còn xác minh được bằng dữ liệu thật. Hai kịch bản đứt
thì đứt ở **một mắt xích đã được ghi là cố ý không làm** (nhắc nợ) và **một mắt xích thiếu dữ
liệu mẫu chứ không thiếu chức năng** (nạp tiền). Phần làm thêm ngoài cam kết — quản trị người
dùng, đổi mật khẩu, nhật ký — là điểm cộng.

**Cập nhật 27/08:** **cả năm mục đã làm xong** — mục 1–3 ở `docs/G5-REPORT.md`, mục 4–5 ở
`docs/G6-REPORT.md`. Không còn khuyến nghị nào treo.

Điều đáng nói nhất khi bảo vệ **không phải** là danh sách tính năng, mà là: mọi thứ cố ý không
làm đều có vết trong tài liệu, kèm lý do. Đó là thứ hiếm hơn một phân hệ nữa.
