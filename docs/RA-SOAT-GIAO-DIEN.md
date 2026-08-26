# RÀ SOÁT GIAO DIỆN — VÒNG 4

> Rà soát **chỉ đọc**. Không sửa file mã nguồn hay template nào; chỉ tạo đúng tài liệu này.
> Ứng dụng chạy ở profile mặc định (`No active profile set`) suốt quá trình đo — **không**
> dùng profile `reset`. Dữ liệu còn nguyên 280 hóa đơn.

Ngày rà: 26/08/2026 · Đo trên bản `321975b`

---

## ⚠️ Đính chính bối cảnh trước khi đọc tiếp

Đề bài mô tả trạng thái đã lệch khỏi kho. Rà theo mô tả cũ sẽ cho kết luận sai nền, nên đối
chiếu lại trước:

| Đề bài nói | Kho thật | Cách kiểm |
|---|---|---|
| 277 test PASS | **315** | `mvnw test` |
| 8 script 183/183 | **215/215** | chạy 8 script, đếm `[DAT ]` |
| 42 template | **46** | `find templates -name '*.html'` |
| "code đã đóng băng" | **13 đợt đã sửa sau Phase 8** | `git log` |
| "Phase 8 là vòng một, đây là vòng hai" | **đây là vòng bốn** | xem dưới |

Sau Phase 8 còn các đợt: V1–V6, N1, N3, rồi **G1** (`ded07b9`, làm lại toàn bộ bố cục theo
hướng *trạm viễn thông* — bỏ sidebar, thêm thanh máy, đổi bảng màu và phông), **G1b**
(`8cd9b55`, sửa 5 lỗi chỉ thấy khi đo trên trình duyệt), **G1c/G1d** (chứng từ), **G2**
(`9d5df48`), **G2b** (`e64a27e`), **G3** (`f8d60d6`).

Vòng này vì thế **không lặp lại** những gì G1b và G2 đã sửa — cỡ chữ dưới 13px, cuộn ngang ở
375px, thanh máy cuộn mất, nút mở rail 32×40, `prefers-reduced-motion`, lớp CSS mồ côi. Đã
kiểm lại: tất cả vẫn còn đúng.

**Một dữ kiện quyết định cả bản đề xuất: chưa có ảnh nào được chụp.** N2 ghi `⬜ chưa làm`, và
`find . -iname "*.png"` trả về rỗng. Nghĩa là **mọi sửa đổi giao diện lúc này đều tốn 0 công
chụp lại** — điều kiện đó sẽ không còn sau khi bắt đầu chụp.

---

# BƯỚC 0 — Kiểm kê công cụ

## Đã dùng

| Skill | Dùng cho trục |
|---|---|
| `design:accessibility-review` | Trục 4 — bộ tiêu chí WCAG 2.1 AA dùng làm khung |
| `design:design-critique` | Trục 1, 2 |
| `anthropic-skills:web-design-guidelines` | Trục 1, 6 |
| `superpowers:verification-before-completion` | Đối chiếu tiêu chí nghiệm thu ở cuối |

## Bỏ, kèm lý do

Không dùng cho đủ. Mỗi skill dưới đây bị loại vì một lý do cụ thể:

| Skill | Vì sao không áp dụng được |
|---|---|
| `imagegen-frontend-web` · `imagegen-frontend-mobile` · `brandkit` · `image-to-code` · `stitch-design-taste` | Đều **sinh ảnh thiết kế mới**. Đây là rà soát chỉ đọc trên ứng dụng đã chạy. |
| `minimalist-ui` · `industrial-brutalist-ui` · `high-end-visual-design` · `gpt-taste` · `design-taste-frontend` · `impeccable` · `redesign-existing-projects` | Áp một **hướng thẩm mỹ** riêng (GSAP, bento grid, kiểu editorial). Ba xung đột: đòi thư viện mới (bị cấm), nhắm landing page chứ không phải bảng dữ liệu dày, và hướng thị giác đã chốt ở G1. |
| `e2e-testing` (Playwright) | Dự án không có Playwright. Thêm vào là vi phạm "không thêm phụ thuộc". |
| `understand-anything:*` | Dựng knowledge graph — chi phí dựng lớn, không cần cho một vòng rà giao diện. |
| `ui-ux-pro-max:slides` · `banner-design` · `brand` | Sai loại sản phẩm. |

## Một mâu thuẫn phải nêu

`frontend-design` khuyên *"take one real aesthetic risk"* và dựng một signature element riêng.
Điều đó **chọi với** ràng buộc trong `CLAUDE.md`: Bootstrap 5.3.3 cố định, cấm thêm phụ thuộc,
và ngưỡng khả dụng của Phase 7/8 không được hạ. **Theo `CLAUDE.md`, không theo skill.** Nêu ra
đây thay vì im lặng chọn một bên.

---

# BƯỚC 1 — Rà theo sáu trục

## Trục 1 — Tính nhất quán

### Điểm tốt, có bằng chứng

- **Biểu tượng tuyệt đối nhất quán.** Mỗi khái niệm đúng một icon trên toàn bộ 46 template:
  Khách hàng `bi-people` · Thuê bao `bi-sim` · Hóa đơn `bi-receipt` · Thanh toán `bi-cash-coin`
  · Công nợ `bi-wallet2`. Không có ngoại lệ nào.
- **Đầu bảng nhất quán 100%:** cả **40** thẻ `<thead>` đều dùng `class="table-light"`.
- **Đầu trang có khuôn chung:** 23 template dùng `~{fragments/trang :: tieuDe(...)}`; 10 trong
  14 template còn lại là trang tiêu đề động và dùng đúng đường thoát đã ghi trong tài liệu
  (`~{fragments/trang :: moTa(...)}` bọc trong `.dau-trang`).
- **Khối lọc cùng cấu trúc:** cả 5 màn hình danh sách đều `row g-2`, một `<form>`.

### 1.1 — Khối phân trang chép tay 6 lần, và 3 trong 6 thiếu `flex-wrap`

| File:dòng | `flex-wrap` |
|---|---|
| `templates/cdr/danh-sach.html:163` | có |
| `templates/hoa-don/danh-sach.html:135` | có |
| `templates/thanh-toan/danh-sach.html:104` | có |
| `templates/khach-hang/danh-sach.html:120` | **không** |
| `templates/thue-bao/danh-sach.html:122` | **không** |
| `templates/goi-cuoc/chi-tiet.html:159` | **không** |

Không có fragment dùng chung cho phân trang — sáu bản chép tay, đã trôi ra hai nhánh.

### 1.2 — Cùng hành động "Lọc", ba kiểu nút khác nhau

| Kiểu | File:dòng |
|---|---|
| `btn btn-outline-primary w-100` | `khach-hang/danh-sach.html:45` · `thue-bao/danh-sach.html:50` |
| `btn btn-primary btn-sm` (nút **đặc**) | `hoa-don/danh-sach.html:52` · `thanh-toan/danh-sach.html:36` · `cong-no/danh-sach.html:211` |
| `btn btn-outline-primary` (không `w-100`) | `cdr/danh-sach.html:69` |

### 1.3 — Cùng hành động "Xuất Excel", hai cỡ

`btn btn-outline-success` ở `cdr/danh-sach.html:13`, `cong-no/danh-sach.html:12`,
`hoa-don/danh-sach.html:12`, `thanh-toan/danh-sach.html:12` — nhưng
`btn btn-outline-success `**`btn-sm`** ở `bao-cao/fragments.html:18`.

### 1.4 — Chỉ 1 trong 5 màn hình danh sách có nút xoá bộ lọc

`cdr/danh-sach.html:72` có `<a class="btn btn-outline-secondary">Xoá lọc</a>`. Bốn màn hình kia
(`khach-hang`, `thue-bao`, `hoa-don`, `thanh-toan`) không có. Lọc xong muốn quay lại danh sách
đầy đủ thì phải tự xoá từng ô hoặc sửa URL.

### 1.5 — `table-hover` lệch **ngay trong cùng một màn hình**

| File | Bảng A | Bảng B |
|---|---|---|
| `bao-cao/cong-no.html` | dòng **25**: `table table-sm bang-du-lieu mb-0` (không hover) | dòng **61**: có `table-hover` |
| `goi-cuoc/chi-tiet.html` | dòng **95**: không hover | dòng **129**: có hover |

Toàn cục: **28/42** bảng dữ liệu có `table-hover`. Con số tổng không nói lên gì; hai file trên
mới là bằng chứng — cùng một màn hình, rê chuột lên bảng này thì dòng sáng lên, bảng kia thì
không.

### 1.6 — Số dòng mỗi trang có bốn giá trị

| Controller | Số dòng |
|---|---|
| `CdrController.java:40` | 25 |
| `HoaDonController.java:39` · `ThanhToanController.java:40` | 20 |
| `KhachHangController.java:29` · `ThueBaoController.java:34` | 15 |
| `GoiCuocController.java:27` | 10 |

Không tìm thấy chỗ nào trong `docs/` giải thích vì sao bốn mức. *(Đây là quan sát, không phải
lỗi — CDR dày hơn nên 25 dòng có thể là cố ý. Nêu ra để chốt lại cho có chủ đích.)*

---

## Trục 2 — Kiến trúc thông tin

### 2.1 🔴 — Nút nổi bật duy nhất của ba màn hình tiền bạc là **"Lọc"**

Luật "mỗi màn hình đúng một nút nổi bật" **đạt ở cả 10 màn hình danh sách**. Nhưng nút đó là gì:

| Màn hình | Nút `btn-primary` đặc duy nhất |
|---|---|
| `khach-hang` | Thêm khách hàng |
| `thue-bao` | Đăng ký thuê bao |
| `goi-cuoc` · `bang-gia` · `giam-tru` · `ky-cuoc` | Thêm … / Tạo … |
| `cdr` | Tạo dữ liệu thử |
| **`hoa-don`** | **Lọc** |
| **`thanh-toan`** | **Lọc** |
| **`cong-no`** | **Lọc** |

Trên ba màn hình đó, hành động duy nhất ở đầu trang là "Xuất Excel" kiểu **viền**
(`hoa-don/danh-sach.html:12`), còn nút **đặc** nằm trong khối lọc. Thứ hạng thị giác đang nói:
*việc quan trọng nhất trên màn hình hóa đơn là bấm Lọc.*

`scripts/kiem-giao-dien.py:97` xanh vì nó chỉ đếm `if len(nut) > 1` — bao nhiêu nút, không xét
nút nào. Đây đúng là loại lỗi mà một vòng rà thứ tư phải tìm: **phép kiểm đạt, ý định thì trượt.**

### 2.2 🟡 — Cột đầu tiên của `/hoa-don` là trường duy nhất **không lọc được**

Thứ tự cột (`hoa-don/danh-sach.html:70` trở đi): Mã hóa đơn → Kỳ → Khách hàng → Số thuê bao →
Hạn thanh toán → Tổng thanh toán → Đã thu → Còn nợ → Trạng thái.

`dto/BoLocHoaDon.java:18-23` chỉ có `kyCuocId`, `trangThai`, `khachHang`, `soThueBao`,
`tuSoTien`, `denSoTien` — **không có `maHoaDon`**. Khách đứng ở quầy đọc số điện thoại, nhưng
cột 1 là mã hóa đơn, còn số thuê bao nằm ở cột 4.

### 2.3 🟡 — `/cong-no` có ba bảng, hai thứ tự cột ngược nhau

| Bảng | Hai cột định danh mở đầu |
|---|---|
| `cong-no/danh-sach.html:87` | Số thuê bao → Khách hàng |
| `cong-no/danh-sach.html:144` | Số thuê bao → Khách hàng |
| `cong-no/danh-sach.html:220` | Mã hóa đơn → Kỳ → **Khách hàng → Số thuê bao** |

Cuộn từ trên xuống, mắt phải học lại bố cục giữa chừng.

---

## Trục 3 — Luồng công việc

Đếm bằng cách đi thật qua HTTP, đăng nhập `admin`, xuất phát từ trang chủ.

| Việc hằng ngày | Số bấm tới màn hình đích | Đường đi |
|---|---|---|
| Thêm khách hàng | **1** | lối tắt → `/khach-hang/them` |
| Đăng ký thuê bao | **1** | lối tắt → `/thue-bao/dang-ky` |
| Tra cứu công nợ | **1** | lối tắt → `/hoa-don`, hoặc rail → Công nợ |
| Chạy tính cước | **1** | lối tắt → `/tinh-cuoc` |
| **Ghi nhận thanh toán** | **5** | xem dưới |

### 3.1 🔴 — Lối tắt "Ghi nhận thanh toán" không dẫn tới chỗ ghi nhận thanh toán

Lối tắt số 4 trên trang chủ ghi **"Ghi nhận thanh toán"** nhưng `href="/cong-no"`. Đếm trên
trang trả về: `/cong-no` chứa **0** lần chuỗi "Ghi nhận thanh toán" và **254** liên kết
`/hoa-don/{id}`.

Đường thật: lối tắt (1) → nhập số thuê bao (2) → Lọc (3) → mở hóa đơn (4) →
`/thanh-toan/moi/{id}` (5) → điền form → Lưu.

### 3.2 🟡 — Ghi nhận thanh toán chỉ có **một** đường vào

`/thanh-toan` (danh sách giao dịch) chứa **0** liên kết `thanh-toan/moi`. Đường duy nhất khởi
động được việc thu tiền là màn hình chi tiết hóa đơn. Kế toán mở đúng màn hình tên là
"Thanh toán" thì không thu tiền được ở đó.

### Điểm tốt

Sáu lối tắt trên trang chủ đều bọc `sec:authorize` đúng vai trò; bốn trong năm việc hằng ngày
chỉ tốn một lần bấm. Vấn đề gọn lại ở đúng một luồng.

---

## Trục 4 — Khả năng tiếp cận (WCAG 2.1 AA)

> **Ghi chú về phương pháp.** Lượt đo đầu chạy trong `<iframe>` cho ra **209** cặp trượt tương
> phản. Kiểm chứng lại trên trang thật thì nút thanh máy đo được **10,13:1 (đạt)** trong khi
> iframe báo 1,88:1 — iframe không phân giải được màu nền kế thừa. **Toàn bộ 209 con số đó đã
> bị bỏ.** Mọi tỉ số dưới đây đo trên trang thật, `getComputedStyle` + tự dò màu nền tổ tiên.

### Bảng tương phản — chỉ liệt kê cặp **trượt**

| Thành phần | Chữ | Nền | Tỉ số | Cần | Đạt? |
|---|---|---|---|---|---|
| Vòng tiêu điểm bàn phím | `rgba(13,110,253,.25)` → trộn ra `#b4cff6` | `#eceff3` | **1,38:1** | 3:1 | ❌ |
| `.app-footer` | `#8a939c` | `#eceff3` | **2,70:1** | 4,5:1 | ❌ |
| `.btn-outline-success` "Xuất Excel" | `#198754` | `#eceff3` | **3,93:1** | 4,5:1 | ❌ |
| `.btn-outline-danger` "Tạm ngừng" | `#dc3545` | `#eceff3` | **3,93:1** | 4,5:1 | ❌ |
| `.btn-outline-secondary` "Xem thuê bao" | `#6c757d` | `#eceff3` | **4,07:1** | 4,5:1 | ❌ |

Đối chứng — các cặp **đạt**, đo cùng cách: chữ chính `#101d28`/`#ffffff` 17,10:1 · chữ mờ
`#5c6670`/`#ffffff` 5,85:1 · màu chủ đạo `#0a5f68`/`#ffffff` 7,37:1 · chữ rail
`#b8c6d3`/`#12283a` 8,67:1 · nút trên thanh máy `#b8c6d3`/`#0b1a26` 10,13:1.

### 4.1 🔴 — Vòng tiêu điểm bàn phím vẫn là màu xanh của Bootstrap (1.4.11)

Đo trên `:root`: `--bs-primary-rgb` = `10, 95, 104` (đã nhuộm ở G1 ✅) nhưng
`--bs-focus-ring-color` = **`rgba(13, 110, 253, 0.25)`** — xanh gốc Bootstrap, chưa nhuộm. Hai
biến này độc lập: Bootstrap tính `--bs-focus-ring-color` lúc biên dịch từ `$primary` của nó,
nên ghi đè `--bs-primary-rgb` lúc chạy **không** kéo theo.

Hệ quả kép:

1. Vòng tiêu điểm trộn với nền ra `#b4cff6`, tương phản **1,38:1** — trượt xa ngưỡng 3:1.
2. Đó là màu **không tồn tại** trong bảng màu *trạm viễn thông*. Người dùng bàn phím thấy một
   vệt xanh dương lạ trên nền chàm.

`app.css` không có quy tắc `:focus` nào cho `.btn` hay liên kết — chỉ có cho `.form-control`
(dòng 696), `.loi-tat` (850), `.bo-qua-menu` (1158), `.content` (1165). Nút và liên kết phó mặc
hoàn toàn cho mặc định Bootstrap.

**Đổi màu thôi không đủ:** tính thử `--nhan` ở alpha 0,25 / 0,40 / 0,50 ra 1,47 / 1,90 / 2,28:1
— vẫn trượt. **Alpha mới là vấn đề.** Viền ô nhập lúc lấy tiêu điểm đã làm đúng cách: dùng màu
**đặc** `#0a5f68` = 6,39:1 (`app.css:697`).

### 4.2 🔴 — Chân trang 2,70:1, và màu gõ cứng ngoài bảng màu (1.4.3)

`app.css:438` — `.app-footer { color: #8a939c; }`. Đây là **mã màu gõ thẳng**, không phải
token. Vi phạm luật đã ghi trong `CLAUDE.md`: *"Bảng màu chỉ có MỘT nguồn: `:root` trong
`app.css`"*. Chân trang nằm trong `layout.html` nên có mặt ở **mọi màn hình**.

Thay bằng `var(--chu-mo)` (`#5c6670`) → **5,07:1**, đạt.

### 4.3 🟡 — Ba biến thể nút viền chưa được nhuộm ở G1 (1.4.3)

G1 nhuộm `--bs-primary` nhưng để `success`, `danger`, `secondary` ở mặc định Bootstrap. Token
sẵn có trong `:root` đều đạt, **không cần thêm màu mới**:

| Hiện tại | Tỉ số | Token thay thế | Tỉ số mới |
|---|---|---|---|
| `#198754` success | 3,93:1 ❌ | `--tin-on` `#137a63` | **4,56:1** ✅ |
| `#dc3545` danger | 3,93:1 ❌ | `--tin-loi` `#b3232c` | **5,70:1** ✅ |
| `#6c757d` secondary | 4,07:1 ❌ | `--chu-mo` `#5c6670` | **5,07:1** ✅ |

### 4.4 🟡 — 22 ô nhập lọc không gắn được nhãn (3.3.2 · 4.1.2)

Nhãn có hiện ra bằng mắt nhưng ô nhập **không có `id`**, nên `<label>` không có `for` để trỏ tới.

| Template | Ô nhập thiếu `id` |
|---|---|
| `khach-hang/danh-sach.html` | 3 (`tuKhoa`, `loaiKh`, `trangThai`) |
| `thue-bao/danh-sach.html` | 4 |
| `cong-no/danh-sach.html` | 2 |

Tổng 22 ô trên 10 màn hình đã quét. Trình đọc màn hình đọc ra "edit, blank" — người khiếm thị
không biết ô đó để nhập gì.

### 4.5 🟡 — Nút thao tác trong bảng 32×40 (2.5.5)

`khach-hang/danh-sach.html` cột *Thao tác*: "Xem chi tiết" và "Sửa" đo được **32×40 px**. Ngưỡng
của chính dự án là 40×40; WCAG 2.5.5 đòi 44×44. Cũng gặp ở `quan-tri/nguoi-dung`. *(Cả hai đều
**có** `aria-label` — phần đó đúng.)*

### 4.6 🟢 — Thứ tự heading nhảy h2 → h5

Modal xác nhận trong `fragments/layout.html` dùng `<h5 class="modal-title">`. Nhẹ, vì modal ẩn
tới khi mở.

### Điểm tốt

- **0** ảnh thiếu `alt` trên toàn bộ 10 màn hình đã quét.
- Mọi nút chỉ có biểu tượng đều có `aria-label` — `kiem-ban-phim.py` canh 213 nút/liên kết.
- Đường tắt "Bỏ qua menu" là **phần tử tab đầu tiên** của mọi trang, đúng thiết kế.

---

## Trục 5 — Trạng thái biên

### Điểm tốt — phần này làm tốt nhất trong sáu trục

Thử `/bao-cao/doanh-thu-goi-cuoc?kyCuocId=8` (kỳ 8/2026 rỗng có chủ ý). Toàn bộ chữ hiện ra:

> *"Kỳ đã chọn chưa có hóa đơn nào. Báo cáo lấy số từ hóa đơn. Tháng nào chưa lập hóa đơn thì
> báo cáo của tháng đó trống. **Sang màn hình tính tiền hằng tháng**"*

Nói **vì sao** trống và **đi đâu tiếp** — đúng luật Phase 8. Bốn màn hình báo cáo đều vậy.

> Một hướng điều tra đã bị bác bỏ: 13 template có `<tbody>` mà không dùng fragment `bangRong`
> trông như thiếu trạng thái rỗng. Mở ra kiểm thì chúng dùng cách khác, tốt ngang. **Không phải
> lỗi.**

`/hoa-don` với bộ lọc không khớp: *"Không có hóa đơn nào khớp bộ lọc đang đặt. Hãy nới bộ lọc,
hoặc kiểm tra xem tháng đã chọn…"* — cũng đúng khuôn.

### 5.1 🔴 — 6 trong 42 bảng thiếu `.table-responsive`, và một cái làm vỡ trang

| File:dòng |
|---|
| `bao-cao/cong-no.html:25` |
| `bao-cao/thue-bao.html:60` · `:76` |
| `cdr/import.html:63` |
| `cdr/sinh-du-lieu.html:167` · `:179` |

**Đo thật trên `/bao-cao/cong-no`**, nhét chuỗi 200 ký tự liền không khoảng trắng vào ô đầu tiên
của bảng ở dòng 25 *(chỉ sửa DOM trên trình duyệt, không ghi vào CSDL)*:

| | Trước | Sau |
|---|---|---|
| `documentElement.scrollWidth` | 1265 | **2608** |
| Trang cuộn ngang | không | **có** |

Đối chứng trên `/khach-hang` (bảng **có** `.table-responsive`), cùng chuỗi đó: `scrollWidth` giữ
nguyên 1265, bảng tự cuộn trong khung (2878 / 956). **Lớp bọc làm đúng việc của nó — 6 bảng kia
chỉ đơn giản là thiếu nó.**

### 5.2 🟢 — Tên dài có khoảng trắng làm dòng cao 190px

`.bang-du-lieu td` có `white-space: normal`, `word-break: normal`, `max-width: none`. Tên khách
hàng 200 ký tự **có** khoảng trắng thì xuống dòng, dòng cao **190px** thay vì ~40px. Xấu nhưng
không vỡ, và tên doanh nghiệp thật hiếm khi dài vậy.

---

## Trục 6 — Ngữ cảnh máy chiếu

Đo ở **1024×768**, độ phân giải máy chiếu phổ biến.

### Điểm tốt

- **Không cuộn ngang** ở 1024px.
- Chữ thân 16px · ô bảng 14,4px · đầu bảng 14px · nhãn form 15,2px · giá trị thẻ số liệu
  25,6px. Đọc được từ xa.

### 6.1 🔴 — Chữ biểu đồ 12px là chữ nhỏ nhất trong phòng

`static/js/app.js:196` — `Chart.defaults.font.size = 12;`

Ba biểu đồ trên trang chủ ở 1024×768: `bieuDoDoanhThu` 699×233px, `bieuDoGoi` và
`bieuDoTrangThai` mỗi cái 324×280px. Nhãn trục, chú giải và số trên trục đều 12px. Chiếu lên
tường, đó là dòng chữ đầu tiên không đọc được từ cuối phòng — trong khi biểu đồ chính là thứ
hội đồng nhìn lâu nhất.

### 6.2 🟡 — Huy hiệu trạng thái 10,8px

`.badge` của Bootstrap là `font-size: 0.75em`; `app.css` không đặt lại (chỉ có `.badge.bg-orange`
ở dòng 160 đổi màu nền). Lồng trong ô bảng 14,4px ra **10,8px**. Riêng `/cong-no` có **175** huy
hiệu như vậy, và chúng mang nghĩa: *Quá hạn*, *Đang dùng*, *Trong hạn*.

> Đây là **lỗi có sẵn**, không do đợt G1: `git show ded07b9^` cho thấy trước G1 `app.css` cũng
> không đặt cỡ chữ cho `.badge`. Đợt G2 đã tìm ra và **cố ý không sửa** vì nó đổi mật độ hiển thị
> của mọi bảng. Ngữ cảnh máy chiếu là lý do mới để xét lại.

---

# BƯỚC 2 — Chấm điểm hiện trạng

| Trục | Điểm | Lý do một dòng | Phát hiện nghiêm trọng nhất |
|---|:---:|---|---|
| 1. Tính nhất quán | **3/5** | Icon và đầu bảng nhất quán tuyệt đối, nhưng nút và phân trang đã trôi ra nhiều nhánh | `table-hover` lệch **ngay trong cùng một màn hình** (`bao-cao/cong-no.html:25` vs `:61`) |
| 2. Kiến trúc thông tin | **2/5** | Thứ hạng thị giác đang đề cao sai việc trên đúng ba màn hình tiền bạc | Nút nổi bật duy nhất của `/hoa-don`, `/thanh-toan`, `/cong-no` là **"Lọc"** |
| 3. Luồng công việc | **3/5** | Bốn trong năm việc chỉ một lần bấm; một việc lệch hẳn | Lối tắt "Ghi nhận thanh toán" dẫn tới màn hình có **0** nút ghi nhận thanh toán |
| 4. Khả năng tiếp cận | **2/5** | `alt`, `aria-label`, đường tắt đều đúng, nhưng năm cặp màu trượt AA và một cặp trượt rất xa | Vòng tiêu điểm bàn phím **1,38:1** trên ngưỡng 3:1 — và là màu ngoài bảng màu |
| 5. Trạng thái biên | **4/5** | Trạng thái rỗng thuộc loại làm tốt hiếm thấy; chỉ hụt lớp bọc cuộn ở 6 bảng | `/bao-cao/cong-no` vỡ trang: `scrollWidth` 1265 → **2608** |
| 6. Ngữ cảnh máy chiếu | **3/5** | Bố cục và chữ thân chịu được 1024×768; chữ biểu đồ thì không | `Chart.defaults.font.size = 12` (`app.js:196`) |

---

# BƯỚC 3 — Đề xuất, xếp theo tỉ lệ giá trị trên công sức

> **Về mục ⚠️ Ảnh hưởng ảnh chụp.** N2 ghi `⬜ chưa làm` và `find . -iname "*.png"` trả về
> **rỗng** — chưa có ảnh nào tồn tại. Nên **mọi đề xuất dưới đây đều tốn 0 công chụp lại, với
> đúng một điều kiện: làm xong rồi mới chụp.** Mục ⚠️ của từng đề xuất ghi những ảnh sẽ khác đi
> nếu ai đó chụp trước rồi sửa sau.

## NÊN LÀM NGAY — S, giá trị cao, không ảnh hưởng ảnh

### Đ1. Vòng tiêu điểm bàn phím dùng màu đặc của bảng màu

- **Vấn đề** — `--bs-focus-ring-color` = `rgba(13,110,253,.25)`, trộn ra `#b4cff6`, **1,38:1**
  trên `--nen`. Trượt WCAG 1.4.11 (≥3:1) và là màu ngoài bảng màu. `app.css` không có quy tắc
  `:focus` nào cho `.btn`/liên kết (chỉ dòng 696, 850, 1158, 1165).
- **Đề xuất** — thêm vào `:root` trong `app.css`: `--bs-focus-ring-color: var(--nhan);` và một
  quy tắc `.btn:focus-visible, .page-link:focus-visible { outline: 2px solid var(--nhan);
  outline-offset: 2px; box-shadow: none; }`. Dùng màu **đặc** — đã tính: alpha 0,25/0,40/0,50
  cho ra 1,47/1,90/2,28:1, đều vẫn trượt.
- **Công sức** — S (khoảng 10 phút)
- **Giá trị** — **Cao.** Sửa tiêu chí AA đang trượt xa nhất, và dọn nốt màu lạc duy nhất còn sót
  sau đợt nhuộm G1.
- **Rủi ro** — Không. `kiem-giao-dien.py`, `kiem-tu-ngu.py`, `kiem-ban-phim.py` không đọc màu
  tiêu điểm; 315 test Java không chạm CSS.
- **⚠️ Ảnh hưởng ảnh chụp** — **Không.** Vòng tiêu điểm chỉ hiện khi đang lấy tiêu điểm bằng bàn
  phím; không ảnh nào trong `danh-sach-anh-chup.md` chụp trạng thái đó.

### Đ2. Chân trang dùng token thay vì mã màu gõ cứng

- **Vấn đề** — `app.css:438` `.app-footer { color: #8a939c; }` → **2,70:1** trên `--nen`, trượt
  1.4.3. Là mã màu gõ thẳng, vi phạm luật *"bảng màu chỉ có MỘT nguồn"* trong `CLAUDE.md`. Nằm
  trong `layout.html` nên có ở **mọi** màn hình.
- **Đề xuất** — đổi `#8a939c` thành `var(--chu-mo)` → **5,07:1**.
- **Công sức** — S (2 phút, một dòng)
- **Giá trị** — **Cao.** Một dòng sửa một lỗi AA có mặt trên toàn bộ 46 màn hình, đồng thời đóng
  lại một chỗ vi phạm quy ước của chính dự án.
- **Rủi ro** — Không.
- **⚠️ Ảnh hưởng ảnh chụp** — Chân trang xuất hiện ở **mọi** ảnh toàn màn hình. Chữ sẽ đậm hơn
  một bậc. Chụp trước rồi sửa sau thì **toàn bộ 70 ảnh** lệch. Làm trước khi chụp: 0 ảnh hưởng.

### Đ3. Nhuộm nốt ba biến thể nút viền

- **Vấn đề** — G1 nhuộm `--bs-primary` nhưng bỏ `success`/`danger`/`secondary`: `#198754` =
  3,93:1 (`hoa-don/danh-sach.html:12` "Xuất Excel"), `#dc3545` = 3,93:1 ("Tạm ngừng" trên
  `/cong-no`), `#6c757d` = 4,07:1 ("Xem thuê bao"). Cả ba trượt 4,5:1.
- **Đề xuất** — trong `:root`: `--bs-success: var(--tin-on); --bs-danger: var(--tin-loi);
  --bs-secondary-color: var(--chu-mo);` cho ra 4,56 / 5,70 / 5,07:1. **Không thêm màu mới** —
  cả ba token đã có sẵn trong `:root`.
- **Công sức** — S (15 phút, gồm đo lại)
- **Giá trị** — **Cao.** Ba lỗi AA cùng lúc, và làm đợt nhuộm G1 hoàn chỉnh.
- **Rủi ro** — Nút "Xuất Excel" và "Tạm ngừng" đổi sắc nhẹ. Không script nào chốt mã màu — đã
  kiểm `grep -rn "198754\|dc3545" scripts/ src/test/` → 0 kết quả.
- **⚠️ Ảnh hưởng ảnh chụp** — Ảnh **#28** (danh sách hóa đơn), **#30** (công nợ), **#32** (đề
  xuất tạm ngừng), **#33** (danh sách thanh toán) và mọi ảnh có nút Xuất Excel sẽ đổi sắc. Làm
  trước khi chụp: 0.

### Đ4. Bọc `.table-responsive` cho 6 bảng còn thiếu

- **Vấn đề** — `bao-cao/cong-no.html:25`, `bao-cao/thue-bao.html:60` và `:76`,
  `cdr/import.html:63`, `cdr/sinh-du-lieu.html:167` và `:179`. Đo thật: nhét chuỗi 200 ký tự vào
  bảng dòng 25 làm `scrollWidth` nhảy **1265 → 2608**, trang cuộn ngang.
- **Đề xuất** — bọc mỗi bảng trong `<div class="table-responsive">`, đúng như 36 bảng kia.
- **Công sức** — S (15 phút)
- **Giá trị** — **Cao.** Chặn vỡ bố cục, và đưa 6 bảng lạc về đúng khuôn chung.
- **Rủi ro** — Không. Thêm một thẻ bọc, không đổi nội dung bảng.
- **⚠️ Ảnh hưởng ảnh chụp** — **Không.** Ở dữ liệu hiện tại các bảng này chưa tràn nên hình không
  đổi. Ảnh **#47** (thống kê thuê bao) và **#16** (sinh CDR) giữ nguyên.

### Đ5. Nâng chữ biểu đồ cho máy chiếu

- **Vấn đề** — `static/js/app.js:196` `Chart.defaults.font.size = 12;`. Ở 1024×768, ba biểu đồ
  trang chủ (699×233 và 2×324×280) có nhãn trục và chú giải 12px — chữ nhỏ nhất trên màn hình,
  trên đúng thứ hội đồng nhìn lâu nhất.
- **Đề xuất** — đổi thành `14`. Một dòng. *(Ý kiến thẩm mỹ: 14 là mức vừa đủ; 16 làm chú giải
  chiếm chỗ của vùng vẽ trên biểu đồ tròn 324px.)*
- **Công sức** — S (5 phút, gồm mở lại xem)
- **Giá trị** — **Cao** cho buổi demo — đây là ngữ cảnh trình chiếu, không phải màn hình để bàn.
- **Rủi ro** — Không. Không test nào chốt cỡ chữ biểu đồ.
- **⚠️ Ảnh hưởng ảnh chụp** — Ảnh **#3** (dashboard), **#43** (doanh thu theo kỳ), **#44** (doanh
  thu theo gói cước), **#45** (doanh thu theo loại dịch vụ), **#46** (thống kê thuê bao) đều có
  biểu đồ. Chụp trước rồi sửa sau là phải chụp lại cả năm. Làm trước: 0.

### Đ6. Hạ nút "Lọc" xuống kiểu viền trên ba màn hình tiền bạc

- **Vấn đề** — `hoa-don/danh-sach.html:52`, `thanh-toan/danh-sach.html:36`,
  `cong-no/danh-sach.html:211` dùng `btn btn-primary btn-sm`, khiến "Lọc" thành nút nổi bật
  **duy nhất** của màn hình. Bảy màn hình danh sách khác dành nút đó cho hành động chính.
- **Đề xuất** — đổi cả ba thành `btn btn-outline-primary btn-sm`, khớp với `khach-hang`,
  `thue-bao`, `cdr`. Ba màn hình đó sẽ không còn nút đặc nào — đúng thực tế, vì chúng không có
  hành động tạo mới.
- **Công sức** — S (10 phút)
- **Giá trị** — **Cao.** Sửa thứ hạng thị giác đang đề cao sai việc, và dọn luôn một trong ba
  nhánh của phát hiện 1.2.
- **Rủi ro** — `kiem-giao-dien.py:97` chỉ chặn khi `len(nut) > 1`; **0 nút vẫn đạt**. Đã đọc mã
  script để xác nhận.
- **⚠️ Ảnh hưởng ảnh chụp** — Ảnh **#28**, **#30**, **#33** đổi kiểu một nút. Làm trước: 0.

### Đ7. Thêm `flex-wrap` cho ba khối phân trang còn thiếu

- **Vấn đề** — `khach-hang/danh-sach.html:120`, `thue-bao/danh-sach.html:122`,
  `goi-cuoc/chi-tiet.html:159` thiếu `flex-wrap` mà ba khối kia có. Nhiều trang + cửa sổ hẹp thì
  dãy số trang đẩy ngang.
- **Đề xuất** — thêm `flex-wrap` vào ba dòng đó cho khớp `cdr:163`, `hoa-don:135`,
  `thanh-toan:104`.
- **Công sức** — S (5 phút)
- **Giá trị** — **Trung bình.** Ở dữ liệu hiện tại (280 hóa đơn / 20 dòng = 14 trang) chưa tràn;
  đây là chặn trước.
- **Rủi ro** — Không.
- **⚠️ Ảnh hưởng ảnh chụp** — **Không.** Chưa tràn nên hình không đổi. Ảnh **#6** giữ nguyên.

## CÂN NHẮC — M, có đánh đổi

### Đ8. Gắn `id` cho 22 ô nhập lọc

- **Vấn đề** — `khach-hang/danh-sach.html` (3 ô), `thue-bao/danh-sach.html` (4),
  `cong-no/danh-sach.html` (2) và các màn hình khác — tổng **22** ô không có `id`, nên `<label>`
  không gắn được. Trượt 3.3.2 và 4.1.2.
- **Đề xuất** — thêm `id` cho mỗi ô và `for` tương ứng trên `<label>`. Không đổi `name` nên
  không đụng controller.
- **Công sức** — **M** (30–45 phút cho 22 ô trên nhiều file)
- **Giá trị** — **Trung bình.** Đúng chuẩn AA, nhưng người dùng đích của đồ án này không dùng
  trình đọc màn hình; giá trị nằm ở chỗ báo cáo nói được "đã đạt AA ở mục nhãn form".
- **Rủi ro** — Thấp. Đổi `name` mới là nguy hiểm — **không đổi**. Cần chạy lại 215 phép kiểm HTTP
  vì vài script dò theo chuỗi `name="..."`.
- **⚠️ Ảnh hưởng ảnh chụp** — **Không.** `id` không hiện ra bằng mắt.

### Đ9. Nâng huy hiệu trạng thái lên 13px

- **Vấn đề** — `.badge` = 0,75em → **10,8px** trong ô bảng 14,4px. Riêng `/cong-no` có 175 huy
  hiệu, và chúng mang nghĩa (*Quá hạn*, *Đang dùng*).
- **Đề xuất** — thêm `.badge { font-size: .8125rem; }` vào `app.css`.
- **Công sức** — S để sửa, nhưng **M** nếu tính cả công xem lại — nó đổi mật độ hiển thị của
  **mọi** bảng trong phần mềm.
- **Giá trị** — **Trung bình.** Cao cho buổi chiếu, thấp cho dùng hằng ngày trên màn hình để bàn.
- **Rủi ro** — Bảng cao thêm; cột trạng thái rộng thêm; có thể đẩy bố cục ở bảng nhiều cột như
  `/cong-no` bảng thứ ba (8 cột). **Phải đo lại cuộn ngang sau khi sửa.**
- **⚠️ Ảnh hưởng ảnh chụp** — Đổi hình ở **mọi** ảnh có bảng — ước lượng **hơn 30** trong 70 ảnh,
  gồm #6, #28, #30, #31, #33. Đây là đề xuất tốn ảnh nhất nếu làm sai thứ tự.

### Đ10. Thêm nút "Xoá lọc" cho bốn màn hình danh sách

- **Vấn đề** — chỉ `cdr/danh-sach.html:72` có. `khach-hang`, `thue-bao`, `hoa-don`, `thanh-toan`
  không có đường quay lại danh sách đầy đủ.
- **Đề xuất** — chép mẫu ở `cdr/danh-sach.html:72-74` sang bốn màn hình.
- **Công sức** — **M** (20–30 phút)
- **Giá trị** — **Trung bình.** Người dùng đích quen Excel, nơi bỏ lọc là một lần bấm.
- **Rủi ro** — Thêm một liên kết vào mỗi màn hình. `kiem-ban-phim.py` đếm 213 nút/liên kết và
  đòi mỗi cái có tên đọc được — nút này có chữ nên đạt; vẫn phải chạy lại.
- **⚠️ Ảnh hưởng ảnh chụp** — Ảnh **#6** (danh sách khách hàng), **#28**, **#33** có thêm một nút
  trong khối lọc.

### Đ11. Thống nhất `table-hover` trong hai màn hình lệch

- **Vấn đề** — `bao-cao/cong-no.html:25` không hover / `:61` có; `goi-cuoc/chi-tiet.html:95`
  không / `:129` có.
- **Đề xuất** — thêm `table-hover` vào hai bảng thiếu.
- **Công sức** — S
- **Giá trị** — **Thấp–Trung bình.** *(Một phần là ý kiến: có lập luận rằng bảng tổng hợp không
  cần hover vì không bấm được vào dòng. Nếu theo lập luận đó thì nên gỡ hover khỏi bảng tổng hợp
  thay vì thêm — miễn là **trong cùng một màn hình phải nhất quán**.)*
- **Rủi ro** — Không.
- **⚠️ Ảnh hưởng ảnh chụp** — **Không.** Hover chỉ hiện khi rê chuột.

## GHI VÀO HƯỚNG PHÁT TRIỂN — L, hoặc đụng tầng ngoài giao diện

### Đ12. Làm lối tắt "Ghi nhận thanh toán" dẫn đúng tới việc ghi nhận thanh toán

- **Vấn đề** — lối tắt số 4 trên trang chủ ghi "Ghi nhận thanh toán" nhưng `href="/cong-no"`,
  màn hình chứa **0** lần chuỗi đó. Đường thật tốn **5** lần bấm, trong khi bốn việc hằng ngày
  còn lại chỉ tốn 1. Và `/thanh-toan` có **0** liên kết `thanh-toan/moi` — chỉ khởi động được từ
  chi tiết hóa đơn.
- **Đề xuất** — hai hướng, đều **không** đổi route hay service: (a) đổi chữ trên lối tắt thành
  *"Tra cứu để thu tiền"* cho đúng thứ nó làm — S, nhưng chỉ chữa nhãn; (b) thêm ô "Số thuê bao"
  ngay trên `/thanh-toan` để nhập số rồi nhảy thẳng tới hóa đơn còn nợ — **L**, và cần một action
  mới ở controller.
- **Công sức** — **L** cho hướng (b)
- **Giá trị** — **Cao** về nghiệp vụ, nhưng đây là việc *thiết kế lại một luồng*, không phải
  chỉnh giao diện. Sát ngày demo mà đụng vào là sai thời điểm.
- **Rủi ro** — **Cao.** Hướng (b) đụng controller, tức ra ngoài phạm vi "chỉ lớp giao diện", và
  cần test mới. `test-muc-F.ps1` (17 phép kiểm) đi qua đúng luồng này.
- **⚠️ Ảnh hưởng ảnh chụp** — Hướng (a) đổi chữ ở ảnh **#3** (dashboard) và **#63** (khối *Việc
  thường làm*). Hướng (b) thêm hẳn một khối vào ảnh **#33**, và có thể cần một ảnh mới.

### Đ13. Rút khối phân trang thành một fragment dùng chung

- **Vấn đề** — sáu bản chép tay ở `cdr:163`, `hoa-don:135`, `thanh-toan:104`, `khach-hang:120`,
  `thue-bao:122`, `goi-cuoc/chi-tiet:159`, đã trôi ra hai nhánh (`flex-wrap`).
- **Đề xuất** — dựng `~{fragments/trang :: phanTrang(ketQua, duongDan, chuoiLoc)}` rồi thay cả
  sáu chỗ.
- **Công sức** — **L** (trên 1 giờ: sáu chỗ, mỗi chỗ một kiểu ghép URL khác nhau)
- **Giá trị** — **Trung bình.** Trả nợ kỹ thuật, người dùng không thấy gì.
- **Rủi ro** — **Trung bình.** Sáu màn hình cùng lúc; sai một chỗ là hỏng phân trang. Đ7 chữa
  triệu chứng trong 5 phút với rủi ro gần bằng 0.
- **⚠️ Ảnh hưởng ảnh chụp** — Không, nếu làm đúng.

### Đ14. Thống nhất số dòng mỗi trang

- **Vấn đề** — 25 / 20 / 15 / 10 ở sáu controller, không tìm thấy chỗ nào trong `docs/` giải
  thích.
- **Đề xuất** — hoặc chốt về hai mức có lý do ghi rõ (ví dụ 20 cho bảng thường, 25 cho CDR), hoặc
  giữ nguyên và **ghi lý do vào `CLAUDE.md`**.
- **Công sức** — S để sửa số, nhưng **L** để nghiệm thu lại: đổi số dòng làm lệch mọi phép kiểm
  đếm dòng.
- **Giá trị** — **Thấp.**
- **Rủi ro** — **Cao.** `test-bien.ps1` và `test-ky-rong.ps1` đi qua phân trang; đổi số dòng gần
  như chắc chắn làm đỏ vài phép kiểm.
- **⚠️ Ảnh hưởng ảnh chụp** — Đổi số dòng hiện ra ở **mọi** ảnh danh sách.

---

# BƯỚC 4 — Khuyến nghị dứt khoát

**Còn 2 giờ trước khi chụp ảnh và nộp báo cáo: làm Đ1, Đ2, Đ3, Đ4, Đ5, Đ6. Bỏ hết phần còn lại.**

Sáu đề xuất đó cộng lại khoảng **55 phút**, còn dư hơn một giờ để chạy lại nghiệm thu và chụp.
Chúng gom được bốn lỗi WCAG AA đang trượt (Đ1, Đ2, Đ3), một lỗi vỡ bố cục đo được (Đ4), lỗi
đọc-được duy nhất của buổi chiếu (Đ5), và lỗi thứ hạng thị giác nặng nhất (Đ6). Cả sáu đều là S,
đều gói trong `app.css`, `app.js` và bốn template, và **không cái nào đụng service, repository,
entity hay db**.

**Thứ tự bắt buộc: sửa hết sáu cái, chạy lại nghiệm thu, rồi mới chụp.** Vì chưa có ảnh nào, đây
là cửa sổ duy nhất mà sáu thay đổi này tốn 0 công chụp lại. Chụp trước rồi sửa sau thì riêng Đ2
làm lệch **cả 70 ảnh** (chân trang có ở mọi màn hình), Đ5 làm lệch **5 ảnh có biểu đồ** (#3, #43,
#44, #45, #46), Đ3 và Đ6 làm lệch **#28, #30, #32, #33**.

**Bỏ Đ9** dù nó có lý cho máy chiếu: nó đổi mật độ hơn 30 ảnh và cần đo lại cuộn ngang trên mọi
bảng — không phải việc để làm trong hai giờ cuối. **Bỏ Đ8, Đ10, Đ11** vì giá trị trung bình mà
đều cần chạy lại 215 phép kiểm HTTP. **Bỏ Đ12, Đ13, Đ14**: Đ12 là thiết kế lại một luồng nghiệp
vụ, Đ13 chạm sáu màn hình cùng lúc trong khi Đ7 chữa xong triệu chứng trong 5 phút, Đ14 gần như
chắc chắn làm đỏ script.

**Đ7 là cái duy nhất nằm giữa.** Nó S, rủi ro bằng 0, nhưng giá trị chỉ Trung bình vì dữ liệu
hiện tại chưa đủ để tràn. Còn dư thời gian sau khi nghiệm thu xong thì làm; hết giờ thì bỏ mà
không mất gì.

---

## Phụ lục — Sáu lần phép kiểm của chính lượt rà này báo động giả

Ghi lại theo chuẩn làm việc số 5 của dự án (*"một phép kiểm sai nguy hiểm ngang thiếu phép
kiểm"*). Cả sáu đều cho kết quả **trông như đã đạt** hoặc **trông như một phát hiện lớn**:

1. **209 cặp trượt tương phản** đo trong `<iframe>` — kiểm lại trên trang thật thì nút thanh máy
   là **10,13:1 (đạt)** chứ không phải 1,88:1. iframe không phân giải được màu nền kế thừa. **Bỏ
   toàn bộ, đo lại trên trang thật, còn 5 cặp.**
2. **"13 template thiếu trạng thái rỗng"** — mở ra thì chúng dùng cách khác, tốt ngang fragment
   `bangRong`. Không phải lỗi.
3. **`/tinh-cuoc` toàn nút phá huỷ** — đọc template thì "Chạy tính cước" và "Lập hóa đơn" là
   `btn-primary` có điều kiện, kèm khai `NUT-NOI-BAT-CO-Y:` ở dòng 87. Quyết định có chủ ý.
4. **Báo cáo trên kỳ rỗng "không render gì"** — tôi gõ nhầm tham số `kyId` thay vì `kyCuocId`,
   nên trang rơi về kỳ mặc định. Gõ đúng thì trạng thái rỗng hiện ra đầy đủ.
5. **Thứ tự cột `/cong-no`** — lượt trích đầu gộp nhầm hai bảng thành một danh sách 13 cột. Tách
   đúng theo dòng mới thấy bảng `:87` và `:144` **giống nhau**, chỉ `:220` khác.
6. **"Không có chỉ báo tiêu điểm"** — `.focus()` bằng mã không kích hoạt `:focus-visible` nên
   `boxShadow` đọc ra `none`. Phải đọc thẳng biến `--bs-focus-ring-color` mới thấy vấn đề **thật**
   (màu sai + alpha quá nhạt), khác hẳn vấn đề tôi tưởng.
