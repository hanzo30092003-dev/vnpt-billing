# Spec · Landing page Hanzo, bộ nhận diện mới và luồng đăng nhập

- **Ngày:** 30/09/2026
- **Trạng thái:** chờ HANZO duyệt bản viết
- **Phạm vi:** dự án con thứ nhất của đợt làm lại giao diện. Dự án con thứ hai (46 màn hình nghiệp vụ) có spec riêng, viết sau.
- **Quyết định khó đảo ngược:** [`docs/adr/0001-nhan-dien-hanzo.md`](../../adr/0001-nhan-dien-hanzo.md)
- **Hồ sơ giao diện:** landing theo hồ sơ A (`D:/Work/hanzo-studio/skill-profiles/profiles/A-landing/PROFILE.md`); trang đăng nhập riêng dùng cùng bộ nhận diện.

---

## 1. Mục tiêu

Khi hội đồng chấm mở `http://localhost:8080`, trong 5 giây đầu họ phải hiểu Hanzo là gì, rồi **thấy** phần mềm làm đúng việc của nó: một cuộc gọi biến thành tiền. Trang dẫn tới **một** hành động duy nhất: **Đăng nhập**.

**Người xem:** hội đồng chấm đồ án (chính), và nhân viên của nhà mạng hư cấu *Viễn thông Hanzo* (vai trò trong câu chuyện).

**Thành công khi:**
- Landing đạt các tiêu chí ở mục 7.3, gồm Lighthouse mobile ≥ 90 / 95 / 95.
- Đăng nhập được từ landing bằng chuột **và** chỉ bằng bàn phím.
- Landing không lộ bất kỳ số tiền, công nợ hay tên khách nào cho người chưa đăng nhập.
- Không một dữ liệu nào trong CSDL thay đổi.

---

## 2. Quyết định đã chốt trong buổi thiết kế

| # | Câu hỏi | Chốt |
|---|---|---|
| 1 | Landing để làm gì | Giới thiệu chính phần mềm, số liệu thật, CTA duy nhất là *Đăng nhập* |
| 2 | Motion dựng bằng gì | GSAP 3.13 + ScrollTrigger + SplitText, **tự host**, chỉ tải ở landing |
| 3 | Landing ở đâu | `/` rẽ nhánh: chưa đăng nhập thấy landing, đã đăng nhập thấy trang tổng quan. Không URL nào đổi |
| 4 | Hình ảnh | Minh họa vẽ bằng mã (HTML/SVG + GSAP) là chính, thêm **2 ảnh AI** (Canva) làm nền không khí |
| 5 | Nhận diện | Một bộ chung cho landing và phần mềm, chốt **một lần** ở đây, 46 màn hình kế thừa |
| 5a | Tên | Phần mềm **Hanzo**, nhà mạng **Công ty Cổ phần Viễn thông Hanzo** |
| 6 | Hướng thẩm mỹ | **C · Chứng từ**: giấy sáng, than chì, xanh ngọc lục bảo, con số là nhân vật chính |
| 7 | Phông thân chữ | **Geist**, kèm **Geist Mono** cho mã số; tiêu đề **Unbounded** |
| 8 | Bố cục | **A · Một cuộn phim**: theo chân một cuộc gọi, năm cảnh |
| 9 | Khung đăng nhập | **C · Nút nở thành khung** |
| — | Tiêu đề mở màn | *"Từ cuộc gọi đến hóa đơn, không sót một giây."* |
| — | Lighthouse | Được cài cục bộ vào `tools-chup-anh/` |

Skill đọc thẳng từ `~/.claude/skills-archive/` (Taste, UI-UX Pro Max) và từ thư mục hồ sơ A/B; không chép lại, không đổi cấu hình.

---

## 3. Luồng và bảo mật

### 3.1 `GET /`

`HomeController` rẽ nhánh theo `SecurityUtils.layNguoiDungHienTai()`:

- **Chưa đăng nhập** → view `landing`. Model chỉ gồm:
  - `soCdr`, `soHoaDon`, `soKyCuoc`, `soThanhToan`: 4 phép `count()` trên repository sẵn có.
  - `viDu`: ví dụ tính cước ở mục 5.3.
  - Cờ trạng thái lấy từ tham số: `loi`, `khoa` (số phút), `dathoat`.
- **Đã đăng nhập** → trang tổng quan, **không đổi gì**.

Landing **không** dùng `fragments/layout.html`. `LayoutAdvice.tinhTrang()` vốn đã trả `null` khi chưa đăng nhập, nên không truy vấn công nợ.

### 3.2 `SecurityConfig`

Chỉ thêm, không sửa luật cũ:
- `requestMatchers(HttpMethod.GET, "/").permitAll()` — đúng một đường dẫn, đúng một phương thức.
- Thêm `/fonts/**`, `/vendor/**`, `/favicon.svg` vào nhóm tài nguyên tĩnh `permitAll`.

### 3.3 Đăng nhập, thất bại, đăng xuất

Khung đăng nhập trên landing là một `<form method="post" action="/dang-nhap">` bình thường, gồm `_csrf`, `tenDangNhap`, `matKhau` và trường ẩn `nguon=landing`.

| Tình huống | Hiện nay | Sau thay đổi |
|---|---|---|
| Đúng mật khẩu | `/` → trang tổng quan | giữ nguyên |
| Sai mật khẩu, gửi từ landing | `/dang-nhap?loi` | `/?loi` — landing mở sẵn khung, **không** chạy hiệu ứng nở, hiện lời báo lỗi |
| Bị khoá tạm, gửi từ landing | `/dang-nhap?khoa=N` | `/?khoa=N` — khung mở sẵn, báo thử lại sau N phút |
| Sai hoặc bị khoá, gửi từ `/dang-nhap` | như cũ | giữ nguyên |
| Đăng xuất | `/dang-nhap?dathoat` | `/?dathoat` — landing hiện một dòng báo nhỏ |
| Phiên hết hạn, hoặc gõ thẳng URL bên trong | Spring đưa về `/dang-nhap` | giữ nguyên; `/dang-nhap` được làm lại theo bộ nhận diện mới |
| Đổi mật khẩu xong | `/dang-nhap?doimatkhau` | giữ nguyên |

Sửa trong `XuLyDangNhap`:
- Hàm chọn đích khi thất bại đọc `request.getParameter("nguon")`. **Chỉ** giá trị đúng bằng chuỗi `landing` mới đổi đích sang `/`. Mọi giá trị khác bị bỏ qua. **Tham số này không bao giờ được dùng làm đích chuyển hướng**, để chặn chuyển hướng mở.
- Đổi `logoutSuccessUrl` sang `/?dathoat`.

### 3.4 Khi không có JS

Nút *Đăng nhập* là `<a class="nut-dang-nhap" href="/dang-nhap">`. JS chỉ nâng nó lên thành hiệu ứng nở và chặn điều hướng. Không có JS thì vẫn đăng nhập được qua trang riêng.

### 3.5 Không in tài khoản mẫu

Landing và `/dang-nhap` không in `admin`, `nhanvien01`, `ketoan01`, `123456`. Đây là luật sẵn có của `test-auth.ps1` mục 1. Cửa sổ khởi động vẫn in tài khoản như cũ.

---

## 4. Bộ nhận diện Hanzo

### 4.1 Tên và phạm vi đổi tên

Wordmark `HANZO.` bằng Unbounded 800, dấu chấm màu nhấn. Nhà mạng: **Công ty Cổ phần Viễn thông Hanzo**. Mã số thuế `1800000000` và tổng đài `1800 6060` giữ nguyên (số bịa).

**Đổi:**
- `fragments/layout.html:85`: *"Sông Hậu · Quản lý cước"* → *"Hanzo · Quản lý cước"*
- `HoaDonPdfService.java:110`, `PhieuThuPdfService.java:51`: tên công ty
- `hoa-don/chi-tiet.html:44`: tên công ty
- 3 phép kiểm PDF: thêm `contains("VIỄN THÔNG HANZO")` và `doesNotContain("SÔNG HẬU")`, giữ nguyên `doesNotContain("VNPT")`
- `CLAUDE.md`: luật *"đơn vị phát hành là công ty hư cấu"*, đổi tên, giữ nguyên tinh thần
- `README.md` và tài liệu hiện trạng

Các báo cáo lịch sử (`PHASE-*`, `G*`) giữ tên cũ, thêm một dòng ghi chú ở đầu.

**Không đổi:** `data-mau.sql:109` *"Lô B2 KCN Sông Hậu, Châu Thành, Hậu Giang"* — đây là địa chỉ của một khách hàng, không phải tên nhà mạng.

### 4.2 Màu

Nguồn duy nhất cho bộ nhận diện mới: `static/css/tokens.css`. Tương phản đo bằng công thức WCAG 2.1:

| Token | Vai trò | Sáng | Tối |
|---|---|---|---|
| `--hz-giay` | nền | `#F4F4F2` | `#111315` |
| `--hz-the` | thẻ, khung | `#FCFCFB` | `#1A1D20` |
| `--hz-muc` | chữ chính | `#16181B` (16,2:1) | `#ECEDEA` (15,8:1) |
| `--hz-mo` | chữ phụ | `#5C605B` (5,8:1) | `#9DA29B` (7,2:1) |
| `--hz-vien` | viền trang trí | `#DADCD8` | `#2B2F33` |
| `--hz-vien-nhap` | viền ô nhập | `#83877F` (3,3:1) | `#6B706A` (3,7:1) |
| `--hz-nhan` | nhấn: chữ nhỏ, nút, liên kết | `#0B6B4A` (5,9:1; chữ sáng trên nền này 6,4:1) | `#3FBF8A` (8,0:1) |
| `--hz-nhan-lon` | nhấn: chữ ≥ 24px, con dấu, thanh tiến độ | `#0F8A5F` (3,96:1, ngưỡng 3:1) | `#3FBF8A` |
| `--hz-loi` / `--hz-loi-nen` | lỗi | `#A8281F` / `#FBEDEB` (6,2:1) | `#F08A7E` / `#2A1715` (7,0:1) |
| `--hz-thu` / `--hz-thu-nen` | trạng thái *đã thu* | `#0B6B4A` / `#DDF1E8` (5,5:1) | `#3FBF8A` / `#12251D` (6,9:1) |
| `--hz-no` / `--hz-no-nen` | trạng thái *còn nợ* | `#8A2A1F` / `#F6E1DD` (6,9:1) | `#F08A7E` / `#2A1715` (7,0:1) |

Quy tắc:
- `#0F8A5F` **không bao giờ** làm chữ dưới 24px hay nền cho chữ nhỏ (chỉ đạt 3,96:1).
- Không dùng trắng tinh hay đen tuyền.
- Chỉ **một** màu nhấn trên toàn trang.
- Mọi cặp màu mới thêm sau này phải đo lại và ghi vào bảng này.

### 4.3 Chữ

| Vai trò | Phông | Dùng khi |
|---|---|---|
| Tiêu đề | Unbounded (biến thiên, 600–800) | chỉ từ 24px trở lên |
| Giao diện, thân chữ | Geist (biến thiên, 400–600) | mọi chữ còn lại; số tiền dùng `font-variant-numeric: tabular-nums` |
| Mã | Geist Mono (400–500) | số thuê bao, mã hóa đơn, mã kỳ |

Thang cỡ chữ (px): 13 · 14 · 16 · 18 · 22 · 28 · 36 · 48 · 64 · tiêu đề mở màn `clamp(44px, 7vw, 108px)`.

Giữ luật của dự án: không chữ thường trực nào dưới 13px; nhãn gắn với một con số từ 14px trở lên; không viết hoa để bù cho chữ nhỏ.

### 4.4 Tự host phông

`static/fonts/{unbounded,geist,geist-mono}/`, định dạng woff2, lấy từ gói Fontsource, kèm tệp giấy phép SIL OFL 1.1. Mỗi phông khai ba `@font-face` với `unicode-range` (latin, latin-ext, vietnamese) và `font-display: swap`. Trang tiếng Việt chỉ tải latin + vietnamese: đo được khoảng 130 KB cho cả ba phông.

Tải trước phông Unbounded (latin + vietnamese) vì nó vẽ phần tử LCP.

### 4.5 Chế độ tối

Theo `prefers-color-scheme`; cả hai chế độ đều được thiết kế theo bảng 4.2. Riêng cảnh 4 (ba người dùng) là **khối nền mực có chủ đích** ở chế độ sáng — ngoại lệ "một lần đổi nền có chủ đích" mà Taste mục 4.11 cho phép. Ở chế độ tối, khối này dùng `--hz-the` để vẫn tách khỏi nền.

### 4.6 Trạng thái chuyển tiếp (có chủ đích)

Sau spec này, landing và `/dang-nhap` dùng `tokens.css`, còn 46 màn hình nghiệp vụ vẫn dùng bảng màu cũ trong `app.css`. Đăng nhập xong người dùng thấy giao diện cũ, chỉ có tên đã đổi thành Hanzo. Luật *"bảng màu chỉ có một nguồn"* trong `CLAUDE.md` tạm thời bị vi phạm; ghi chú điều này ngay dưới luật đó. Spec thứ hai sẽ gộp về một nguồn.

### 4.7 Biểu tượng trang

`static/favicon.svg`: chữ `H` Unbounded và dấu chấm `#0F8A5F` trên nền `#16181B`. Khai ở landing và `/dang-nhap`.

---

## 5. Landing

### 5.1 Năm cảnh

| # | Cảnh | Mục đích (một câu) | Chuyển động (màn hình ≥ 768px) | Màn hình < 768px |
|---|---|---|---|---|
| 0 | Thanh nổi: `HANZO.` · **Đăng nhập ↗** | Hành động duy nhất luôn trong tầm tay | thanh nổi dạng viên thuốc tách khỏi mép trên; nút nở thành khung (5.5) | giữ nguyên |
| 1 | Mở màn | Nói Hanzo là gì trong 5 giây | tiêu đề tách chữ bay lên; hai con số đếm lên | cỡ chữ co theo `clamp` |
| 2 | Hành trình một cuộc gọi | Cho thấy phần mềm làm đúng việc của nó | ghim, cuộn khoảng 3 lần chiều cao màn hình; bốn trạm sáng dần; thẻ CDR → thẻ tính cước → hóa đơn in dần → con dấu *ĐÃ THU* | **không ghim**; bốn trạm xếp dọc, mỗi trạm kèm thẻ của nó, hiện dần khi cuộn |
| 3 | Con số thật | Chứng minh nó đang chạy thật, trên dữ liệu thật | lưới lệch cỡ, số đếm lên khi cuộn tới | một cột |
| 4 | Ba người dùng | Cho thấy mỗi vai trò chỉ thấy phần việc của mình | khối nền mực, kéo ngang có ghim | không ghim, xếp dọc |
| 5 | Lời mời cuối + chân trang | Nhắc lại hành động duy nhất | tiêu đề trồi lên; nút nở thành khung | giữ nguyên |

Chi tiết trình bày lấy theo `high-end-visual-design`:
- Thanh đầu trang nổi, không dính sát mép.
- Nút chính kiểu *nút trong nút*: mũi tên đặt trong vòng tròn riêng.
- Khoảng trắng dọc mỗi cảnh ít nhất 96px (trừ cảnh ghim).
- Đường cong chuyển động `cubic-bezier(0.32, 0.72, 0, 1)` cho giao diện; cảnh bám theo cuộn dùng `scrub`.
- Một lớp hạt giấy độ mờ 0,03, gắn vào phần tử `position: fixed; pointer-events: none`.

### 5.2 Nội dung chữ (bản nháp)

Bản nháp theo `copywriting`. Ở bước dựng, toàn bộ qua `anthropic-skills:chong-van-ai`. **Không dùng dấu gạch dài.**

- **Nhãn trên tiêu đề:** Phần mềm quản lý thuê bao và tính cước
- **Tiêu đề:** Từ cuộc gọi đến hóa đơn, *không sót một giây.* (vế sau màu nhấn lớn)
- **Dòng dẫn:** Hanzo ghi nhận từng cuộc gọi, tính cước theo đúng bảng giá, lập hóa đơn cho cả kỳ và cho kế toán biết ngay khách nào còn nợ.
- **Hai con số mở màn:** `{soCdr}` cuộc gọi, tin nhắn, lượt dùng mạng đã tính · `{soHoaDon}` hóa đơn đã lập
- **Cảnh 2, tiêu đề phụ:** Hành trình của một cuộc gọi
  1. **Ghi nhận** — Cuộc gọi kết thúc, hệ thống lưu lại số gọi, số nhận và thời lượng.
  2. **Tính cước** — Đối chiếu bảng giá, tính theo từng khối 6 giây.
  3. **Lập hóa đơn** — Cuối kỳ, mọi cuộc gọi dồn thành một tờ hóa đơn.
  4. **Thu tiền** — Kế toán ghi nhận thanh toán, công nợ tự cập nhật.
- **Cảnh 3:** `{soCdr}` bản ghi sử dụng đã tính cước · `{soHoaDon}` hóa đơn qua `{soKyCuoc}` kỳ cước · `{soThanhToan}` lần thu tiền đã ghi nhận
- **Cảnh 4, tiêu đề:** Mỗi người một việc, một màn hình.
  - **Nhân viên quầy** — Thêm khách hàng, đăng ký số mới, đổi gói cước cho khách.
  - **Kế toán** — Xem hóa đơn, thu tiền, theo dõi ai còn nợ và nợ đã bao lâu.
  - **Quản trị** — Chạy tính cước cuối tháng, quản lý bảng giá, cấp tài khoản.
- **Cảnh 5, tiêu đề:** Mở phần mềm. · Nút: **Đăng nhập**
- **Chân trang:** HANZO · Đồ án Thực tập nghề nghiệp · Dữ liệu mẫu tự sinh
- **Thẻ `<title>`:** Hanzo · Quản lý thuê bao và tính cước
- **Mô tả meta:** Phần mềm tính cước viễn thông: ghi nhận cuộc gọi, tính cước theo bảng giá, lập hóa đơn và theo dõi công nợ.

Dấu gạch ngang giữa tên trạm và mô tả ở danh sách trên chỉ là cách trình bày trong spec. Trên trang, tên trạm và mô tả là hai dòng riêng.

### 5.3 Ví dụ tính cước ở cảnh 2 — tính lúc hiển thị, không gõ cứng

Đầu vào cố định (đặt thành hằng số trong controller):
- Gói **CB01 · Cơ bản** — không có phút miễn phí, nên cuộc gọi chắc chắn bị tính tiền.
- Thoại **nội mạng**, **giờ thường**, **134 giây**.
- Số hiển thị che bớt đuôi: `0901 234 5··` → `0912 345 6··`.

Tính bằng đúng mã của engine, **không viết lại công thức**:
1. Tra dòng giá: `BangGiaLookup` → `bang_gia_cuoc` (giá gói trước, không có thì giá chung).
2. `soBlock = DonViCuoc.soBlock(134, block_giay)`.
3. `cuocGoi = ThamSoTinhCuoc.lamTronTien(soBlock × don_gia)`.
4. Hóa đơn minh họa **một cuộc gọi**: cước thuê bao (`goi_cuoc.cuoc_thue_bao_thang`) + `cuocGoi`, thuế theo `billing.thue-suat-vat` (nhãn lấy qua `thamSo.nhanThueSuat()`), tổng.

**Dự đoán công bố trước** (chuẩn làm việc số 4), theo bảng giá hiện tại:

| Bước | Giá trị |
|---|---|
| Dòng giá | chung · nội mạng · giờ thường · khối 6 giây · 15 đ |
| Số khối | ⌈134 / 6⌉ = **23** |
| Cước cuộc gọi | 23 × 15 = **345 đ** |
| Trước thuế | 50.000 + 345 = **50.345 đ** |
| Thuế 10% | 5.034,5 → **5.035 đ** (HALF_UP) |
| Tổng | **55.380 đ** |

Lệch dự đoán thì dừng lại phân tích, không sửa số cho khớp.

Thẻ ghi rõ: *"Ví dụ minh họa · tính theo bảng giá thật của gói Cơ bản"*. Hóa đơn minh họa **không** lấy hóa đơn của khách nào. Tên nhà mạng trên thẻ: **VIỄN THÔNG HANZO**.

### 5.4 Ảnh AI

Sinh bằng Canva `generate-image` ở bước dựng, lưu `static/images/landing/*.webp`, có sẵn `width`/`height`.

- **Ảnh 1 · quầy giao dịch** — 1600×1000. Một nhân viên quầy giao dịch viễn thông đang tiếp khách, ánh sáng ban ngày, tông trung tính khớp bảng màu. Không logo, không màu thương hiệu của nhà mạng nào, không chữ đọc được trên màn hình. Dùng cho cả ba thẻ vai trò bằng `object-position` khác nhau.
- **Ảnh 2 · dải nền không khí** — 1800×600. Mặt giấy chứng từ và bóng một cột phát sóng mờ, độ tương phản thấp; đặt sau lời mời cuối. Chỉ tải khi cuộn tới (`loading="lazy"`).
- Mỗi ảnh ≤ 200 KB; `alt` mô tả đúng nội dung; ảnh 2 là trang trí nên `alt=""`.

Nếu Canva không sinh được ảnh đạt các điều kiện trên, **dừng và hỏi HANZO**, không dùng ảnh stock hay ảnh giữ chỗ.

### 5.5 Khung đăng nhập — nút nở thành khung

- Bấm nút *Đăng nhập* (thanh nổi hoặc cảnh 5): khung phình ra **từ chính vị trí và kích thước của nút đó**. Nền chuyển từ `--hz-muc` sang `--hz-the`, bo góc từ 8 lên 14px, khoảng 0,6 giây, `expo.inOut`; nội dung hiện sau.
- Đóng thì chạy ngược lại, về đúng nút đã mở.
- `role="dialog"`, `aria-modal="true"`, `aria-labelledby`. Nền phía sau đặt `inert`. Tab chỉ đi vòng trong khung, Esc đóng, đóng xong tiêu điểm về đúng nút đã mở.
- Lỗi: khung rung nhẹ (bỏ qua khi giảm chuyển động), xoá ô mật khẩu, đưa con trỏ về đó. Chữ báo lỗi dùng lại đúng câu của `/dang-nhap` hiện tại cho `?loi` và `?khoa=N`.
- Hai bẫy đã bắt ở bản thử, bắt buộc canh:
  1. `focus({ preventScroll: true })` — nếu không, trình duyệt cuộn lệch trang.
  2. `gsap.killTweensOf` + `clearProps` mỗi lần mở — nếu không, GSAP nhớ phép biến đổi cũ và khung mở lệch chỗ.
- Có `?loi`, `?khoa` hoặc `?dathoat` trên URL: khung (hoặc dòng báo) hiện **ngay, không hiệu ứng**, rồi dùng `history.replaceState` xoá tham số khỏi thanh địa chỉ.

---

## 6. Tài nguyên, hiệu năng, khả năng dùng

### 6.1 Tệp

| Tệp | Mới / sửa |
|---|---|
| `templates/landing.html` | mới |
| `templates/dang-nhap.html` | sửa giao diện, form và tên trường giữ nguyên |
| `static/css/tokens.css`, `static/css/landing.css` | mới |
| `static/js/landing.js` | mới |
| `static/vendor/gsap/3.13.0/{gsap,ScrollTrigger,SplitText}.min.js` + `GHI-CHU-GIAY-PHEP.txt` | mới, 124 KB |
| `static/fonts/...` + giấy phép OFL | mới |
| `static/images/landing/*.webp` | mới |
| `static/favicon.svg` | mới |
| `HomeController`, `SecurityConfig`, `XuLyDangNhap` | sửa như mục 3 |
| `fragments/layout.html`, 2 dịch vụ PDF, `hoa-don/chi-tiet.html` | đổi tên như mục 4.1 |

**Không đụng:** `pom.xml`, `application.yml`, `db/**`, entity, repository.

### 6.2 Hiệu năng

- **LCP là tiêu đề mở màn:** vẽ ngay ở khung hình đầu. Hiệu ứng tách chữ chạy trên chữ đã hiện; **không** ẩn chữ về `opacity: 0` trước khi tách, vì Chrome không tính LCP cho phần tử đang ẩn.
- Tải trước phông của tiêu đề. Script GSAP đặt `defer` ở cuối trang.
- Chỉ tạo chuyển động bằng `transform` và `opacity`; `backdrop-filter` chỉ dùng cho thanh nổi và lớp nền mờ của khung đăng nhập.
- Không dùng `window.addEventListener('scroll')` — mọi thứ theo cuộn đi qua ScrollTrigger.

### 6.3 Không có chuyển động, không có JS

- Có `prefers-reduced-motion: reduce`: không ghim cảnh nào; bốn trạm và hóa đơn hiện đầy đủ; số liệu hiện ngay giá trị cuối; khung đăng nhập mở và đóng tức thì.
- Không có JS: mọi số liệu **in sẵn giá trị thật từ máy chủ**; hóa đơn minh họa hiện đầy đủ; nút *Đăng nhập* là liên kết sang `/dang-nhap`.
- Trạng thái "chưa chạy hiệu ứng" chỉ áp khi JS đã chạy (lớp `js` gắn vào `<html>` bằng một script nhỏ đầu trang), để không bao giờ có nội dung bị ẩn vĩnh viễn.

### 6.4 Ngoại tuyến

Landing không gọi ra ngoài `localhost` lần nào. Kiểm bằng cách chặn mọi địa chỉ ngoài `localhost` (dựa trên `tools-chup-anh/thu-mat-mang.mjs`): ảnh chụp phải giống hệt lúc có mạng.

---

## 7. Kiểm thử và nghiệm thu

### 7.1 Phép kiểm Java mới

Dùng MySQL như các lớp tích hợp hiện có.

| Phép kiểm | Khẳng định | Đối chứng âm |
|---|---|---|
| Landing khi chưa đăng nhập | `GET /` → 200, view `landing`, có `HANZO`; 4 số đếm hiển thị **bằng** `count()` đọc thẳng từ CSDL | — |
| Landing không lộ dữ liệu bên trong | không có `thanh-may-hieu`, `Tổng công nợ`, `Còn nợ`; không có tên của **bất kỳ** khách nào trong bảng `khach_hang`; không có `admin`, `nhanvien01`, `ketoan01`, `123456`; không có chuỗi tiền khớp `\d{1,3}(\.\d{3})+ đ` **ngoài** các số của thẻ ví dụ | chạy cùng bộ lọc trên một chuỗi cố ý chứa `85.848.297 đ` → phải báo lỗi |
| Ví dụ tính cước khớp engine | các số trong thẻ ví dụ bằng kết quả tự tính trong test qua `BangGiaLookup` + `DonViCuoc` + `ThamSoTinhCuoc` | đổi đơn giá trong test → phải lệch |
| Đã đăng nhập | `GET /` → trang tổng quan, có `thanh-may-hieu` | — |
| Đích chuyển hướng | thất bại + `nguon=landing` → `/?loi`; không có `nguon` → `/dang-nhap?loi`; bị khoá + landing → `/?khoa=N`; đăng xuất → `/?dathoat` | — |
| Chống chuyển hướng mở | `nguon=https://gia-mao.vn`, `nguon=//gia-mao.vn`, `nguon=Landing` → đều về `/dang-nhap?loi` | — |
| Đổi tên | 3 phép kiểm PDF có `VIỄN THÔNG HANZO`, không có `VNPT`, không có `SÔNG HẬU` | — |

`KiemTraDieuHuongTest` và `KiemTraUuTienThuocTinhTest` tự quét template mới. Dự kiến 343 → khoảng 352 test, tất cả phải đạt.

### 7.2 Script có sẵn

- `test-auth.ps1` mục 2 viết lại: *"chưa đăng nhập mở `/` thì thấy landing và không thấy gì bên trong"*.
  - `CanCo`: `HANZO`, `name="matKhau"`.
  - `KhongDuocCo`: `thanh-may-hieu`, `Tổng công nợ`, `Còn nợ`.
  - Đối chứng âm ghi trong chú thích của script.
- `kiem-tu-ngu.py`, `kiem-giao-dien.py`, `kiem-ban-phim.py` phải đạt trên template mới. Landing có hai nút *Đăng nhập* cùng một hành động, nên khai `NUT-NOI-BAT-CO-Y:` kèm lý do trong template, **không** sửa phép kiểm.
- Chạy lại cả 8 script trong `scripts/`; số phép kiểm đạt không được giảm.

### 7.3 Nghiệm thu bằng trình duyệt (hồ sơ A)

| Tiêu chí | Cách đo |
|---|---|
| Lighthouse mobile: Performance ≥ 90, Accessibility ≥ 95, SEO ≥ 95 | `lighthouse` cài cục bộ trong `tools-chup-anh/` (đã được cho phép), chạy với Edge |
| 375px không cuộn ngang; nút *Đăng nhập* thấy được không cần cuộn | Playwright đo `scrollWidth` và vị trí nút |
| Ảnh chụp 375px và 1440px, sáng và tối | lưu vào `docs/test-report.md` |
| Ngoại tuyến giống hệt | chặn mạng ngoài, so ảnh |
| Giảm chuyển động: đủ nội dung | Playwright với `reducedMotion: 'reduce'` |
| Đăng nhập chỉ bằng bàn phím, cả khi sai mật khẩu | Playwright điều khiển bằng phím |
| Taste mục 14 Pre-Flight Check · `accessibility-review` | chạy theo skill, ghi kết quả |
| Không lỗi JS trên cả hai chế độ và cả hai khổ | bắt `pageerror` và `console.error` |

### 7.4 Dữ liệu không suy suyển

Trước và sau khi làm: 7 kỳ · 23.223 CDR · 338 hóa đơn · 753 chi tiết · 161 thanh toán · kỳ 9/2026 rỗng và `MO` · 4 bất biến 0 lệch. Không chạy `reset`, không tính cước, không lập hay huỷ hóa đơn.

---

## 8. Ngoài phạm vi spec này

- **46 màn hình nghiệp vụ** — spec thứ hai: gộp bảng màu về `tokens.css`, tự host luôn Bootstrap và Chart.js để phần mềm chạy được khi mất mạng.
- **Chụp lại 67 ảnh, sửa các chương báo cáo** — làm sau spec thứ hai, để không chụp hai lần. Đợt này chỉ chụp landing và `/dang-nhap`.
- **Bước 1 của bộ khởi động** coi mọi phản hồi HTTP là phần mềm đã chạy — việc nhỏ riêng.
- **Tên và biểu tượng lối tắt Desktop** sang Hanzo — việc nhỏ riêng.

---

## 9. Ràng buộc phải giữ trong lúc làm

- Không chạy profile `reset`; kỳ 9/2026 phải rỗng và `MO`.
- Không ghi mật khẩu vào tệp nào trong kho.
- Tệp `.ps1` có tiếng Việt lưu kèm BOM UTF-8.
- Đường dẫn và tên tệp mới thuần ASCII, không dấu, không khoảng trắng.
- Giữ nguyên đường tắt *"Bỏ qua menu"* của `layout.html` và mọi luật giao diện trong `CLAUDE.md`, trừ ngoại lệ ghi ở mục 4.6.
