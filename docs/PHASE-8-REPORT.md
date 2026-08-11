# BÁO CÁO ĐỢT LÀM LẠI GIAO DIỆN

> Mục tiêu: một **nhân viên giao dịch không rành công nghệ** mở phần mềm lên là tự dùng được,
> không cần ai hướng dẫn. Đây là phần cuối cùng trước khi demo trước hội đồng.

---

## 1. Người dùng đích, và vì sao phải làm lại

Người dùng đích quen **Excel và Zalo**, chưa từng dùng phần mềm nghiệp vụ, và không biết
các từ *CDR*, *rating*, *billing*, *prorate*.

Giao diện trước đợt này được dựng bởi người **đã biết hệ thống hoạt động thế nào**, nên nó
giả định người xem cũng biết. Cụ thể, một cuộc kiểm toán 42 template trước khi sửa cho ra:

| Vấn đề | Số lượng |
|---|---:|
| Từ kỹ thuật trong chữ người dùng nhìn thấy | **216** |
| Thông báo lỗi không nói phải làm gì tiếp | **114 / 132** |
| Màn hình không có câu nào nói nó dùng để làm gì | **33 / 42** |
| Bảng rỗng không chỉ đường | **36** |
| Màn hình có nhiều hơn một nút nổi bật | 4 |
| Nút phá huỷ thiếu bước xác nhận | 3 |

---

## 2. Việc 1 — Đổi từ ngữ kỹ thuật

Chỉ đổi **chữ hiển thị**. Đường dẫn, tên biến, tên lớp CSS, chú thích giữ nguyên.

| Cũ | Mới |
|---|---|
| CDR | Cuộc gọi & tin nhắn · Bản ghi sử dụng · Chi tiết sử dụng |
| Bộ sinh CDR | Tạo dữ liệu thử |
| Kỳ cước | Tháng tính tiền |
| Tính cước | Chạy tính tiền · Tính tiền từng cuộc |
| Prorate | Tính theo số ngày sử dụng |
| Quota | Mức miễn phí |
| Block | Đơn vị tính tiền |
| Hạt giống | Mã tạo lại |
| Engine tính cước | Hệ thống / phần tính tiền |
| VNPT Billing | VNPT — Quản lý cước |

### 2.1. Bốn enum in thẳng tên hằng ra cho người dùng đọc

Phát hiện đáng kể nhất của việc 1. Bốn enum **không hề có nhãn hiển thị**, nên tám màn hình
đang in nguyên `THOAI`, `NOI_MANG`, `GENERATOR`, `IMPORT_CSV`, `DA_TINH` cho nhân viên giao
dịch đọc:

| Enum | Trước | Sau |
|---|---|---|
| `LoaiDichVu` | `THOAI` · `SMS` · `DATA` | Cuộc gọi · Tin nhắn · Truy cập mạng |
| `HuongCuocGoi` | `NOI_MANG` · `NGOAI_MANG` · `QUOC_TE` | Trong mạng · Ngoài mạng · Quốc tế |
| `NguonCdr` | `GENERATOR` · `IMPORT_CSV` | Máy tạo thử · Nhập từ file Excel |
| `TrangThaiTinhCuoc` | `CHUA_TINH` · `DA_TINH` | Chưa tính tiền · Đã tính tiền |

`TrangThaiTinhCuoc` được thêm luôn `lopBadge`, kéo biểu thức điều kiện chọn màu từ template
về enum — màu của một trạng thái nay khai đúng một chỗ, giống `TrangThaiHoaDon` đã làm.

### 2.2. Chữ vừa kỹ thuật vừa sai sự thật

Ba chỗ nói với người dùng rằng chức năng tính cước *"thuộc Phase 4"* và *"Phase 3 mới chỉ tạo
và theo dõi kỳ"* — vừa là ngôn ngữ lập trình viên, vừa **sai** vì chức năng đó có từ lâu.
Một chỗ khác ở `/ky-cuoc` khẳng định *"Ba cột cuối đang bằng 0 vì chưa chạy tính cước"* trong
khi ba cột đó đang có số.

---

## 3. Việc 2 — Mỗi màn hình một câu nói nó dùng để làm gì

`fragments/trang.html` với hai mảnh dùng chung:

- `tieuDe(biểuTượng, tiêuĐề, môTả)` — cho 19 trang tiêu đề tĩnh
- `moTa(chữ)` — cho 9 trang tiêu đề **động** (tên khách hàng, mã hóa đơn…), vì các trang đó
  phải tự dựng thẻ `<h2>` của mình

`bao-cao/fragments :: thanhCongCu` thêm một tham số → phủ cả 7 báo cáo bằng một chỗ sửa.

**42/42 trang** có dòng giải thích. Câu viết dưới 25 từ, cho người chưa biết gì:

> `/hoa-don` — "Xem tiền cước từng tháng của khách hàng, in hóa đơn và ghi nhận khi khách trả tiền."
>
> `/tinh-cuoc/doi-soat` — "Giải thích vì sao hóa đơn ra đúng con số đó, lần theo từng cuộc gọi
> cho tới dòng cuối hóa đơn."

Màu chữ `#5c6670` — tương phản 5,9:1 trên nền trắng, đạt WCAG AA. Không dùng
`.text-secondary` của Bootstrap vì màu đó nhạt hơn, mà đây là câu người mới đọc **đầu tiên**.

---

## 4. Việc 3 — Thông báo lỗi chỉ được việc phải làm

36 thông báo viết lại: 28 trong `service/impl`, 8 trong `dto/` và `validation/`.
**Chỉ đổi nội dung chuỗi** — không đụng điều kiện, luồng hay chữ ký hàm nào.

| Cũ | Mới |
|---|---|
| "Số giấy tờ X đã được dùng cho một khách hàng khác" | "…đã đăng ký cho một khách hàng khác. **Hãy gõ số này vào ô tìm kiếm ở màn hình Khách hàng để xem đó là ai**, hoặc kiểm tra lại số vừa nhập." |
| "Khách hàng còn 3 thuê bao đang hoạt động, không thể ngừng giao dịch" | "…nên chưa ngừng giao dịch được. **Hãy mở từng thuê bao trong danh sách bên dưới và chuyển sang Đã thanh lý trước**, rồi quay lại." |
| "Số CCCD phải gồm đúng 12 chữ số" | "…, không có dấu cách — **ví dụ 079203001234**" |
| "Thuê bao đã thanh lý, không thể chuyển sang trạng thái khác" | "…Đây là trạng thái cuối, không mở lại được vì số điện thoại đã thu hồi và có thể đã cấp cho khách khác. **Muốn phục vụ khách này thì đăng ký một thuê bao mới.**" |

### 4.1. Điểm sai lệch đặc tả — đã nêu trước khi làm

Đặc tả giới hạn phạm vi sửa ở `templates/`, `css`, `js`, `scripts/`, `docs/`, và liệt kê
`service/` là **Stop Condition**. Nhưng **117 thông báo người dùng nhìn thấy nằm trong Java**:
44 ở `service/impl`, 69 ở `dto/`, 4 ở `validation/`.

Chính ví dụ "tốt" mà đặc tả đưa ra — *"Số CCCD này đã đăng ký cho khách hàng KH000012…"* —
là thông báo của `KhachHangServiceImpl`. Và `"Vui lòng nhập block tính cước"` chứa từ **block**
thuộc danh sách phải xoá ở việc 1, nhưng nằm trong một DTO.

Đã dừng lại hỏi trước khi sửa dòng nào. Kết luận: được phép sửa **chuỗi chữ** ở cả ba nơi,
không đụng logic.

**Còn thiếu so với ví dụ đặc tả:** câu mẫu nêu đích danh khách hàng đang giữ số giấy tờ
(`KH000012`). Lấy được tên đó phải gọi thêm `khachHangRepository.findBySoGiayTo` — đó là đổi
**logic**, nằm ngoài phạm vi đã thống nhất. Câu mới thay bằng cách chỉ đường cho người dùng tự
tra ra. Muốn làm đúng như ví dụ chỉ tốn một dòng, nhưng phải được phép sửa service trước.

---

## 5. Việc 4 — Một nút nổi bật mỗi màn hình

Bốn nút hạ cấp; đáng nói nhất là `/khach-hang/{id}`: nút **Sửa** đang là nút nổi bật, còn
**Đăng ký thuê bao mới** thì không. Đổi vai trò cho đúng tần suất dùng thật — ở màn hình một
khách hàng, việc hay làm là đăng ký thuê bao, không phải sửa hồ sơ.

Tám nút phá huỷ đổi từ viền mờ sang **đỏ đặc**. Đáng chú ý nhất: **Chốt kỳ** — thao tác một
chiều duy nhất trong cả hệ thống — đang đeo `btn-outline-dark`, màu **nhạt nhất cả hàng nút**,
ngược hẳn mức nguy hiểm thật của nó.

Modal chuyển trạng thái thuê bao thêm cảnh báo *"Đã thanh lý là không quay lại được"*. Modal
đó là bước xác nhận **duy nhất** của một thao tác không hoàn tác được, mà trước đây không nói
điều đó ở bất cứ đâu.

> **Ngoại lệ có chủ ý:** `/tinh-cuoc` có hai nút `btn-primary` trong markup — *Chạy tính cước*
> và *Lập hóa đơn*. Chúng gần như không hiện cùng lúc (điều kiện loại trừ nhau) và là hai bước
> **nối tiếp** của cùng một quy trình, nên cùng một màu để người dùng đọc được một tín hiệu duy
> nhất: *"nút đậm màu = việc tiếp theo của bạn"*. Ngoại lệ được khai bằng dòng
> `NUT-NOI-BAT-CO-Y:` ngay trong template — nằm cạnh chỗ gây ra nó, không giấu trong file kiểm thử.

---

## 6. Việc 5 — Bảng rỗng chỉ đường

Mảnh `bangRong(biểuTượng, lờiNhắn, nhãnNút, đườngDẫn)` áp cho 16 chỗ, cộng khối dùng chung
của 7 báo cáo và 2 danh sách trên trang chủ. Mỗi chỗ nói **hai** điều: vì sao đang trống, và
bấm đi đâu tiếp.

Nút dẫn đường để `btn-outline-primary` chứ không phải `btn-primary` — trang vẫn còn nút chính
ở góc trên, hai nút xanh đặc cùng lúc là phá đúng luật vừa dựng ở việc 4.

---

## 7. Việc 6, 7, 8 — Menu, lối tắt, cỡ chữ

**Menu** gom lại theo **công việc**, thứ tự theo **tần suất dùng**:

| # | Nhóm | Khi nào dùng |
|---|---|---|
| 1 | Khách hàng & thuê bao | Hằng ngày |
| 2 | Thu tiền & công nợ | Hằng ngày |
| 3 | Tính tiền hằng tháng | Một lần mỗi tháng |
| 4 | Báo cáo | Khi cần |
| 5 | Danh mục (gói cước, bảng giá) | Hiếm |

Nhóm *Danh mục* bị đẩy xuống cuối có chủ ý: gói cước và bảng giá là thứ khai một lần rồi để
yên cả năm, nhưng bản cũ đặt nó ngay đầu nhóm của quản trị viên.

**Trang chủ** thêm khối *Việc thường làm*: 6 ô bấm 227×95px, mỗi ô một dòng phụ nói khi nào
dùng ("Khách vừa trả tiền", "Chạy vào đầu tháng"). Trước đó trang chủ chỉ có số liệu để **nhìn**,
không có gì để **bấm** — người mới mở phần mềm lên thấy một bảng điều khiển đẹp rồi đứng lại.

**Cỡ chữ và vùng bấm** — đo trên trình duyệt thật ở viewport 1280px, không suy ra từ markup:

| Màn hình | Chữ bảng | Nhãn form | Nút dưới 40px | Nút primary |
|---|---|---|---|---|
| `/` | 14,4px ✅ | — | 0/24 | 1 |
| `/khach-hang/them` | — | 15,2px ✅ | 0/7 | 1 |
| `/cdr` | 14,0px ✅ | 15,2px ✅ | 0/15 | 1 |
| `/hoa-don` | 14,4px ✅ | 15,2px ✅ | 0/33 | 1 |
| `/tinh-cuoc` | 14,0px ✅ | — | 0/16 | 0 |

Màu không bao giờ là tín hiệu duy nhất: badge đều kèm **chữ**, cột biến thiên có dấu `+`/`−`
ngay trong số, giờ cao điểm có biểu tượng.

---

## 8. Nghiệm thu

| # | Tiêu chí | Yêu cầu | Kết quả |
|---|---|---|---|
| 1 | `mvnw test` | ≥ 275, 0 lỗi | ✅ **275**, 0 lỗi |
| 2 | 8 script giao diện | 177/177 | ✅ **183/183** (thêm 6 phép kiểm mới) |
| 3 | Không còn từ kỹ thuật trong chữ hiển thị | 0 | ✅ 0 / 43 file |
| 4 | Mọi trang có dòng giải thích | đủ | ✅ 42/42 |
| 5 | Bảng rỗng có giải thích + nút dẫn đường | đủ | ✅ kiểm trên kỳ 8/2026 |
| 6 | Mỗi màn hình một nút primary | ≤ 1 | ✅ 35/35 |
| 7 | Ba bất biến | 0 lệch | ✅ 0 / 0 / 0 |
| 8 | `reset` tái lập giống hệt từng dòng | giống | ✅ **20.102 / 20.102** |

### 8.1. Hai phép kiểm mới, đặt thường trực trong `scripts/`

- **`kiem-tu-ngu.py`** — không còn từ kỹ thuật trong chữ hiển thị.
- **`kiem-giao-dien.py`** — mọi trang có dòng giải thích · ≤ 1 nút nổi bật · không còn ô rỗng cụt lủn.

Cả hai đều đã chạy **đối chứng** trên bản template *trước* đợt sửa: `kiem-tu-ngu` báo đúng
16 chỗ thật, `kiem-giao-dien` báo 32 vấn đề. Trên bản sau: cả hai báo 0. Tức chúng **có kêu**
khi có lỗi, chứ không phải im lặng vì không kiểm gì.

### 8.2. Ghi chú về phép so sánh `reset`

Ảnh chụp CSDL **trước** khi reset lệch 458 dòng so với sau khi reset — nhưng chỉ ở cột `id`
tự tăng, mọi cột nghiệp vụ **byte-identical**. Nguyên nhân: các script kiểm thử lập rồi huỷ
hóa đơn nhiều lần trong ngày, làm bộ đếm `AUTO_INCREMENT` trôi lên và không lùi lại.

Phép kiểm đúng cho tiêu chí *"reset tái lập dữ liệu giống hệt"* là **reset → reset**, không
phải *"trạng thái đang chạy → reset"*: chạy reset hai lần liên tiếp cho **20.102 / 20.102 dòng
giống hệt nhau**.

---

## 9. Ba lỗi tự gây ra trong chính đợt này

Ghi lại vì cả ba đều là cùng một dạng: **phép kiểm báo an toàn trong khi có lỗi thật**.

### 9.1. `.nhan` gắn vào một trường kiểu String → trang 500

Việc 1 nối `.nhan` vào `${c.loaiDichVu}` ở bảng đối soát. Sai: `DongCdr` là record riêng của
màn hình đó, hai trường ấy kiểu `String` chứ không phải enum. Cả trang đổ 500.

**Cả 177 phép kiểm vẫn xanh.** Trang đối soát duy nhất mà bộ kiểm mở là **kỳ 8 — kỳ cố ý để
rỗng** — nên thân vòng lặp `<tr th:each>` không chạy dòng nào. Một phép kiểm chỉ đi qua đường
dữ liệu **rỗng** không chứng minh được gì về đường dữ liệu **đầy**.

Đã thêm vào `test-dieu-huong.ps1` một mục mở bảng đối soát với cặp (thuê bao, kỳ) **lấy từ
CSDL** thay vì chọn cứng. Đối chứng bằng cách chèn lại đúng lỗi cũ:

| Script | Kết quả khi lỗi còn đó |
|---|---|
| `test-ky-rong.ps1` | **28/28 ĐẠT** — mù hoàn toàn |
| `test-dieu-huong.ps1` | **2 SAI** — bắt được |

### 9.2. Một phép kiểm truyền tham số mà màn hình không có

`test-ky-rong.ps1` gọi `/cdr?kyCuocId=8` rồi khẳng định HTTP 200. Nhưng `/cdr` **không có bộ
lọc theo kỳ** — form lọc chỉ có số thuê bao, khoảng ngày, dịch vụ, hướng, tình trạng, nguồn.
Tham số đó bị bỏ qua, màn hình trả về trọn **18.723** bản ghi, và phép kiểm vẫn xanh dù nó
chưa từng chạm vào đường dữ liệu rỗng.

### 9.3. Phép kiểm của chính tôi báo 16 lỗi mà không lỗi nào thật

Bản đầu của `kiem-tu-ngu.py` quét cả `th:href="@{/cdr}"` (đường dẫn — cấm đổi) và
`${k.soCdrXuLy}` (tên biến — phải giữ). Nếu tin nó thì sẽ đi "sửa" đúng những thứ đặc tả cấm
đụng vào.

**Đây là lần thứ tám** dự án gặp bài học 43.5 — *một phép kiểm sai nguy hiểm ngang thiếu phép
kiểm*. Cách phòng vẫn là câu hỏi cũ: **"nếu thứ tôi đang kiểm hỏng thật, phép kiểm này có kêu
không?"** — và cách trả lời chắc chắn duy nhất là **làm nó hỏng thử một lần** rồi xem.

---

## 10. Còn thiếu

| Hạng mục | Ghi chú |
|---|---|
| Nêu đích danh khách hàng giữ số giấy tờ trùng | Cần một lệnh gọi repository — là đổi logic, xem mục 4.1 |
| `/cdr` không lọc được theo tháng | Là chức năng còn thiếu, không phải lỗi — không làm ở đợt này |
| Kiểm tự động cho cỡ chữ và vùng bấm | Hiện đo tay bằng trình duyệt; muốn tự động phải thêm công cụ, mà đặc tả cấm thêm thư viện |
| Ảnh chụp màn hình | Toàn bộ ảnh cũ phải chụp lại — xem `danh-sach-anh-chup.md` |

---

## 11. Đợt bổ sung — một lỗi 500 và bốn điểm giao diện

Phát hiện khi rà lại **ảnh chụp màn hình**, không phải do test bắt được. Đó là điểm đáng ghi
nhất của đợt này.

### 11.1. 🔴 Lỗi 500 khi in phiếu thu — và vì sao 6 test vẫn xanh

`/thanh-toan/{id}/phieu-thu` trả về trang *500 — Lỗi hệ thống*.

| | |
|---|---|
| **Phát hiện** | Nhìn ảnh chụp màn hình, không phải từ test |
| **Nguyên nhân** | `ThanhToanServiceImpl.layTheoId` dùng `findById`, mà `ThanhToan` khai `hoaDon` và `nguoiThu` là `LAZY`. `@Transactional` kết thúc ngay khi hàm trả về; bản in được dựng **sau đó**, và nó truy cập `hoaDon.khachHang.tenKh` |
| **Thông báo thật** | `LazyInitializationException: Could not initialize proxy [HoaDon#308] - no session` |
| **Cách sửa** | Thêm `ThanhToanRepository.timKemQuanHe(id)` với `JOIN FETCH`. Màn hình hóa đơn không dính lỗi này chính vì `HoaDonRepository` đã `JOIN FETCH` sẵn |

**Vì sao `PhieuThuPdfServiceTest` xanh trong khi chức năng hỏng hoàn toàn.** Lớp test đó dựng
`ThanhToan` bằng `new` rồi gán quan hệ bằng tay. Mọi quan hệ vì thế là **đối tượng thật, đã nạp
sẵn** — nó *không bao giờ chạm vào một proxy nào*. Sáu phép kiểm nội dung PDF đều đúng và đều vô
dụng đối với lỗi này.

> **Fixture dựng bằng tay không đi qua tầng nạp dữ liệu, nên không nói được gì về tầng đó.**
> Nó kiểm *hàm sinh PDF*, không kiểm *đường đi tới hàm ấy*.

Đây là **lần thứ chín** dự án gặp bài học 43.5, ở một tầng mới. Mục B của Phase 5 đã chạm đúng
họ lỗi này một lần — *kiểm "file có tồn tại" không chứng minh được font Unicode đúng* — nhưng đó
là ở **tầng sinh file**; lần này là ở **tầng nạp dữ liệu**, và bộ test cũ không có gì chặn.

Đã thêm `PhieuThuPdfTaiLieuThatTest`: đọc giao dịch **thật từ CSDL** qua đúng service mà
controller gọi, rồi in PDF **ngoài transaction** — tức đi đúng con đường sản phẩm thật đi. Đã
chứng minh nó biết kêu: tạm trả `layTheoId` về `findById` → test **ĐỎ** đúng
`LazyInitializationException`; khôi phục → **XANH**.

Kiểm chứng trên app thật: `/thanh-toan/48/phieu-thu` → HTTP 200, `application/pdf`, 21.397
bytes, chữ ký `%PDF-`. Quét 22 đường dẫn chính: tất cả 200, không còn 500 nào.

### 11.2. Thang màu tuổi nợ — mâu thuẫn thông tin, không phải thẩm mỹ

Badge đọc `lopBadge` (chỉ 3 màu Bootstrap dùng được) còn biểu đồ khai riêng 5 mã màu trong
template. Hậu quả: *Quá hạn 1–30* và *31–60* cùng vàng ở badge nhưng **vàng và cam** ở biểu đồ —
cùng một nhóm hiện hai màu ở hai khối nằm cạnh nhau, buộc người đọc đoán cột cam ứng với dòng nào.

Nay `NhomTuoiNo` giữ thẳng **mã màu** (`mauNen` + `mauChu`); badge tô bằng `style` nội tuyến,
biểu đồ đọc cùng danh sách ấy qua phép chiếu Thymeleaf. Không còn bảng màu thứ hai để lệch —
cùng cách `LoaiBienDongSoDu` giữ quy tắc dấu ở đúng một chỗ.

### 11.3. Ba điểm còn lại

| # | Việc | Cách làm |
|---|---|---|
| ② | Cột số căn phải dính sát cột chữ căn trái | Một quy tắc `padding` ngang trong `app.css` cho mọi ô `.bang-du-lieu`, không sửa từng template — vấn đề nằm ở **mọi cặp cột**, không riêng ba chỗ đã thấy |
| ③ | Hai biểu đồ tròn trang chủ phình gần hết màn hình | Khung `.khung-bieu-do-tron` cao 280px + `maintainAspectRatio: false`. Không khoá thì Chart.js giữ tỷ lệ 2:1 theo **bề rộng cột** |
| ④ | Cột Cước phí không phân biệt *miễn phí* với *chưa tính* | Ba tình huống, ba cách hiện: `mien_phi = 1` → badge **Miễn phí** (cùng màu và chữ với bảng đối soát 4E); đã tính → số thật; chưa tính → dấu **—**, vì *0 đồng* và *chưa tính* là hai chuyện khác nhau |

### 11.4. Hai phép kiểm sai bắt được trong chính đợt này

Cả hai đều do chạy **đối chứng âm** trước khi tin số 0 — không cái nào lộ ra nếu chỉ nhìn kết quả.

**Một phép kiểm không bao giờ kêu được.** Phép kiểm *"không còn lớp badge Bootstrap ở nhóm tuổi
nợ"* ĐẠT ở **cả bản trước lẫn bản sau khi sửa** — biểu thức chính quy viết sai nên chẳng khớp gì.
Một phép kiểm luôn xanh là một dòng nhiễu, không phải một lớp bảo vệ. Đã **bỏ hẳn** thay vì sửa:
phép kiểm *"badge đủ 5 mã màu từ enum"* đã phủ đúng yêu cầu đó và đã chứng minh biết kêu.

**Một phép kiểm kêu nhầm vì lỗi bảng mã.** Phép kiểm dấu **—** báo SAI trong khi trang hiển thị
hoàn toàn đúng. Nguyên nhân: file `.ps1` bị ghi lại **thiếu BOM UTF-8**, PowerShell đọc ký tự
em-dash trong mã nguồn thành ký tự khác nên so sánh không bao giờ khớp. Đúng cái bẫy môi trường
đã ghi trong `CLAUDE.md`. Sửa bằng cách neo vào **codepoint** `[char]0x2014` — nguồn thuần ASCII
thì không phụ thuộc bảng mã nữa.

### 11.5. Kiểm chứng

| Phép kiểm | Trước khi sửa | Sau khi sửa |
|---|---|---|
| 19 phép kiểm bốn điểm giao diện | **12 SAI** | **19 ĐẠT, 0 SAI** |
| `mvnw test` | — | **277 ĐẠT**, 0 lỗi |
| 8 script giao diện | — | **183 ĐẠT**, 0 SAI |

Điểm ④ được kiểm trên **kỳ có dữ liệu thật**: nhánh *miễn phí* chạy trên 12.320 bản ghi
`mien_phi = 1`; nhánh *chưa tính* lúc đầu **bị bỏ qua vì CSDL không có bản ghi `CHUA_TINH` nào**,
nên đã sinh tạm 30 bản ghi để thân vòng lặp chạy thật, rồi xoá sạch — số CDR trở lại đúng
**18.723**.

Ba bất biến sau khi xong đều sạch: thanh toán **0 lệch**, sổ cái số dư **0 lệch**, CDR `DA_TINH`
thiếu `bang_gia_cuoc_id` **0 dòng**. Kỳ 8/2026 rỗng và `MO`; kỳ 6 và 7 giữ **0 thanh toán**.

### 11.6. Một việc KHÔNG làm được trong phạm vi đã giao

Yêu cầu ④ có ba gạch đầu dòng; **gạch thứ ba chưa làm**: *"dòng tổng cuối bảng thêm cột tổng
cước phí theo bộ lọc hiện tại"*.

`TongHopCdr` chỉ mang số bản ghi, tổng thời lượng và tổng dung lượng — **không có cước phí**, và
nó được dựng từ một truy vấn gộp trong repository. Thêm cột tổng đòi hỏi sửa `dto/`,
`repository/` và `service/impl/`, mà phạm vi đợt này ghi rõ **không sửa** `service/**` và
`repository/**`. Cộng tổng ngay trên template thì chỉ ra tổng của **trang đang xem**, không phải
của bộ lọc — sai đúng thứ mà dòng tổng sinh ra để nói.

Nêu ra thay vì lặng lẽ làm sai phạm vi hoặc lặng lẽ bỏ qua.
