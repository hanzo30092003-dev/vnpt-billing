# G4 — NÂNG CẤP THỊ GIÁC Ở TẦNG TOKEN VÀ FRAGMENT

> Đợt này **không** đổi hướng thị giác đã chốt ở G1 (`ded07b9`) mà hoàn thiện nó. Không thêm
> thư viện, phông hay phụ thuộc nào. Không chạm `service/`, `repository/`, `entity/`, `dto/`,
> `db/`. Ứng dụng chạy ở profile mặc định suốt quá trình đo — **không** dùng `reset`; dữ liệu
> còn nguyên 280 hóa đơn, 161 thanh toán, kỳ 8/2026 vẫn rỗng.

Bốn cổng, bốn commit: `623bba7` · `6a4108a` · `5f8824e` · và commit của tài liệu này.

---

## Bước 0 — Kiểm kê skill

| Skill | Cổng dùng |
|---|---|
| `design:design-system` | Cổng 2 — thang khoảng cách, cỡ chữ, độ nâng |
| `design:accessibility-review` | Cổng 1, 4 — WCAG AA, bốn trạng thái tương tác |
| `design:design-critique` | Cổng 2, 3 — nhịp bảng, gom lặp |
| `anthropic-skills:web-design-guidelines` | Cổng 2 |
| `superpowers:verification-before-completion` | Cổng 4 |

**Bỏ, kèm lý do:** `imagegen-*`, `brandkit`, `image-to-code`, `stitch-design-taste` — đều sinh
ảnh thiết kế mới, còn đây là sửa CSS tại chỗ. `minimalist-ui`, `industrial-brutalist-ui`,
`high-end-visual-design`, `gpt-taste`, `design-taste-frontend`, `impeccable`,
`redesign-existing-projects` — áp hướng thẩm mỹ khác, mà đợt này **cấm đổi hướng G1**, và phần
lớn đòi GSAP hoặc thư viện mới. `e2e-testing` — đòi Playwright. `frontend-design` — mâu thuẫn
đã ghi ở vòng 4, theo `CLAUDE.md`.

### Mâu thuẫn mới, đã nêu thay vì im lặng chọn

`design:design-system` khuyên dựng thang token **độc lập với framework**. Bootstrap 5.3.3 đã có
thang riêng (`$spacer`, `--bs-border-radius`, `--bs-secondary-rgb`). Dựng thang song song tạo
**hai nguồn sự thật** — đúng thứ `CLAUDE.md` cấm. Cách xử lý: khai thang riêng cho những thứ
Bootstrap không có (`--cach-*`, `--chu-*`, `--bang-*`), nhưng **trỏ biến Bootstrap về token của
mình** ở những chỗ Bootstrap đã có (`--bs-border-radius`, `--bs-secondary-rgb`,
`--bs-focus-ring-color`, `--bs-table-*`). Vẫn còn đúng một nguồn.

---

## CỔNG 1 — Thi hành Đ1 → Đ7

Đo trên **trang thật** trong trình duyệt, không đo trong `<iframe>`.

| | Trước | Sau |
|---|---|---|
| **Đ1** vòng tiêu điểm bàn phím | **1,38:1** | **6,39:1** nền sáng · **8,67:1** nền mực |
| **Đ2** chân trang | **2,70:1** | **5,07:1** |
| **Đ3** nút viền success / danger / secondary | 3,93 / 3,93 / 4,07:1 | **4,56 / 5,70 / 5,07:1** |
| **Đ4** `/bao-cao/cong-no` với chuỗi 200 ký tự | `scrollWidth` **2608** (vỡ) | **1920** (không tràn) |
| **Đ5** chữ biểu đồ | 12px | **14px** |
| **Đ6** nút "Lọc" ba màn hình tiền bạc | `btn-primary` đặc | `btn-outline-primary` |
| **Đ7** `flex-wrap` phân trang | 3/6 khối thiếu | đủ 6 *(cổng 3 làm nó thành thừa)* |

### Đ1 — hai điều phát hiện khi làm

**Bootstrap tính `--bs-focus-ring-color` lúc BIÊN DỊCH từ `$primary` của nó.** Đợt G1 đã nhuộm
`--bs-primary-rgb` thành `10, 95, 104` nhưng vòng tiêu điểm vẫn là `rgba(13,110,253,.25)` —
hai biến độc lập, ghi đè cái này không kéo theo cái kia. Trộn với nền ra `#b4cff6` = **1,38:1**,
vừa trượt ngưỡng 3:1 của 1.4.11 vừa là màu xanh dương không tồn tại trong bảng màu.

**Đổi màu thôi không đủ.** Tính thử `--nhan` ở alpha 0,25 / 0,40 / 0,50 ra 1,47 / 1,90 /
2,28:1 — đều vẫn trượt. **Alpha mới là vấn đề**, không phải màu. Dùng outline **đặc**, đúng như
viền ô nhập vốn đã làm.

**Không màu nào đạt 3:1 trên cả bốn bề mặt** — đo bảng chéo:

| | `--nen` | `--be-mat` | `--muc` | `--muc-sau` |
|---|---|---|---|---|
| `--nhan` #0a5f68 | 6,39 ✅ | 7,37 ✅ | 2,05 ❌ | 2,39 ❌ |
| `--muc-chu` #b8c6d3 | 1,51 ❌ | 1,74 ❌ | 8,67 ✅ | 10,13 ✅ |

Nên token `--vien-tieu-diem` **đổi giá trị theo ngữ cảnh** (khai lại trong `.rail`,
`.thanh-may`) thay vì chia thành hai quy tắc rời.

### Đ3 — vì sao phải ghi đè từng lớp

Đặt `--bs-success` ở `:root` **không có tác dụng**: mixin `button-outline-variant` của Bootstrap
sinh mã màu **đặc** vào `--bs-btn-color`, không tham chiếu qua `var()`. Phải ghi đè từng lớp
`.btn-outline-*`. Ba token thay thế đều có sẵn trong `:root` — **không thêm màu mới**.

---

## CỔNG 2 — Tầng token

### 2.1 Thang khoảng cách — bội số 4px

| | Trước | Sau |
|---|---|---|
| Giá trị `padding`/`margin`/`gap` có đơn vị | 58 | 58 |
| **Lệch thang** | **38** (16 giá trị khác nhau) | **0** |
| Khai báo dùng token `--cach-*` | 0 | 34 |

Các giá trị lệch là dấu vết của những lần chỉnh bằng mắt: `.55rem`, `.65rem`, `.85rem`,
`1.35rem`… khiến hai khối cạnh nhau lệch 1–2px mà không ai chủ ý.

**Hai chỗ làm tròn LÊN có chủ ý, không xuống** — bắt được vì đọc từng chỗ thay vì làm tròn mù:

- `.the-so-lieu .nhan` `padding-right` 28 → **32px**. Biểu tượng đặt `right: .95rem` cỡ
  `1.15rem`, tức mép trái cách lề phải **33,6px**. Giá trị cũ 28px để nhãn **chạy dưới biểu
  tượng 5,6px**. Làm tròn xuống 24px là chồng hẳn.
- `.timeline` `padding-left` 28 → **32px**, **và phải bù** `.timeline-item::before` từ
  `-1.55rem` sang `-1.8rem`. Không bù thì chấm tròn lệch khỏi đường kẻ dọc **5,4px** — đo được
  trước khi sửa.

### 2.2 Thang cỡ chữ — 8 bậc

Trước **15 giá trị** khác nhau, nhiều cái sát tới mức không phân biệt được: 13,0 / 13,4 / 13,6
và 14,0 / 14,1 / 14,4 / 14,7. Sau: `--chu-1` … `--chu-8` (13 · 14 · 15 · 16 · 18 · 20 · 24 · 28px).

**Sàn khả dụng giữ nguyên** — đo lại trên trang thật: **0 chỗ dưới 13px**. Nhãn gắn với con số
vẫn ≥14px. Hai chỗ làm tròn **lên** vì ngữ cảnh máy chiếu: tiêu đề trang 21,6 → 24px, mục menu
14,7 → 15px.

### 2.3 Độ nâng, bo góc, độ dày viền

| | Trước | Sau |
|---|---|---|
| Giá trị `border-radius` | 5, phần lớn gõ thẳng | `--bo-1..3` |
| Giá trị `box-shadow` | 7, dùng cho **ba việc khác nhau** | `--nang-0..3` |
| Khai báo độ dày viền gõ cứng | **21** | **0** |

Bóng đổ là chỗ lộn xộn nhất: có chỗ dùng để làm nổi thẻ, có chỗ vẽ viền trái, có chỗ làm vòng
tiêu điểm — ba việc trong một thuộc tính. Ô nhập lúc lấy tiêu điểm trước dùng **bóng mờ**
`.2rem` còn nút dùng **outline** — hai cơ chế song song cho cùng một trạng thái; nay cả hai
dùng outline.

### 2.4 Nhịp thị giác bảng dữ liệu

`--bang-cao-dong: 40px`, `--bang-dem-doc`, `--bang-dem-ngang`, `--bang-nen-hover`. Đo trên
`/bao-cao/cong-no` — đúng hai bảng vòng 4 nói là lệch nhau:

| | Bảng 1 | Bảng 2 |
|---|---|---|
| Chiều cao dòng | 40px | 40px |
| Cỡ chữ ô | 14px | 14px |
| Nền đầu bảng | `rgb(244,246,249)` | `rgb(244,246,249)` |
| Màu rê chuột | `#e8f3f4` | `#e8f3f4` |

Gỡ nốt hai mã màu lạc ngoài bảng màu: `#f4f6f9` (nền đầu bảng) và `#6ecfd8` (biểu tượng đăng
nhập) → token.

### 2.5 Bốn trạng thái tương tác

Rà ra: `app.css` có `:hover` (3 lần) và `:focus-visible` (8 lần) nhưng **`:active` 0 lần** và
**`:disabled` 0 lần** — hai trong bốn trạng thái phó mặc hoàn toàn cho Bootstrap. Hệ quả: nút
nhấn xuống không phản hồi thống nhất, nút bị vô hiệu chỉ mờ đi bằng `opacity` nên vẫn trông như
bấm được.

Sau: `:hover` 4 · `:focus-visible` 9 · `:active` 7 · `disabled` 17, kèm khối
`prefers-reduced-motion` tắt phần biến hình.

### 2.6 Kèm theo — vùng chạm

Nút chỉ có biểu tượng trong bảng **32×40 → 40×40**, sửa bằng một quy tắc
`.bang-du-lieu .btn-sm { min-width }` thay vì sửa 24 template. Đo lại `/khach-hang`: **0 vùng
chạm dưới ngưỡng**.

---

## CỔNG 3 — Gom lặp thành fragment

| Khối | Trước | Sau |
|---|---|---|
| Phân trang | chép tay **6 chỗ**, đã trôi hai nhánh | `phanTrang(trang, duongDan, chuoiLoc)` |
| Cặp nút "Lọc" + "Xoá bộ lọc" | **8 màn hình, 3 kiểu nút, 2 kiểu nút xoá** | `nutLoc(duongDanGoc, lopCot)` |
| Nút "Xuất Excel" | 5 chỗ, **2 cỡ** | `nutExcel(duongDan)` |

Fragment phân trang phủ được cả `goi-cuoc/chi-tiet` vốn có **đường dẫn động**
(`/goi-cuoc/{id}`) và không có `chuoiLoc`. Bản ở đó trước liệt kê **mọi** trang; nay theo cùng
luật cửa sổ ±3 như năm màn hình kia.

### Đối chứng: refactor có đổi hành vi không

So URL sinh ra trước và sau, bằng `git stash`:

```
bản cũ : /hoa-don?trang=0&amp;trangThai=QUA_HAN
bản mới: /hoa-don?trang=0&amp;trangThai=QUA_HAN
```

**Giống hệt từng byte.**

---

## Ba khẳng định của vòng 4 bị BÁC BỎ

Ghi lại theo chuẩn làm việc số 5 (*"một phép kiểm sai nguy hiểm ngang thiếu phép kiểm"*). Nếu
không kiểm lại, cổng 3 sẽ "sửa" hai thứ không hỏng.

### 1. Đ10 — "chỉ 1 trong 5 màn hình có nút Xoá lọc"

**Sai.** Cả sáu màn hình đều đã có. Lỗi do tôi grep chuỗi `"Xoá lọc"` trong khi chữ thật là
`"Xoá **bộ** lọc"`. Không thêm nút đã có sẵn — chỉ thống nhất kiểu dáng.

### 2. Đ11 / phát hiện 1.5 — "`table-hover` lệch, bảng này sáng lên bảng kia không"

**Sai ở phần hành vi.** Và lần chứng minh đầu của tôi cũng hỏng: tôi duyệt `document.styleSheets`
nhưng `cssRules` của Bootstrap **ném lỗi cross-origin** nên vòng lặp bỏ qua — tôi chỉ quét được
`app.css`, tức "bằng chứng" đó không hề nhìn thấy quy tắc của Bootstrap.

Dựng lại bằng đối chứng chặt: nhân bản **đúng hai quy tắc** rồi thay `:hover` bằng một lớp.

| | Nền bình thường | Nền khi hover |
|---|---|---|
| Bảng **không** có `table-hover` | `rgb(236,239,243)` | **`rgb(232,243,244)`** |
| Bảng **có** `table-hover` | `rgb(236,239,243)` | **`rgb(232,243,244)`** |

Giống hệt. Quy tắc `.bang-du-lieu tbody tr:hover > *` (do chính đợt G1 thêm) thắng nhờ độ đặc
hiệu **(0,2,2)** so với **(0,1,2)** của Bootstrap. Lớp `table-hover` là **markup chết** — đã gỡ
khỏi 23 template (28 lần).

> **Ràng buộc mới, đã ghi vào `app.css`:** bỏ quy tắc `.bang-du-lieu tbody tr:hover > *` là
> **mất hover trên toàn hệ thống**, không còn `.table-hover` đỡ nữa.

### 3. Bảng chấm điểm vòng 4 dựa một phần vào hai phát hiện trên

Nên điểm Trục 1 ở vòng 4 (**3/5**) hơi thấp so với thực tế.

---

## Một phép kiểm bị YẾU ĐI, đã bù

`kiem-ban-phim.py` đếm **213 → 180** nút/liên kết. Không phải vì mất tên đọc được, mà vì gom
fragment làm giảm markup trong **mã nguồn** — phạm vi quét tĩnh hẹp lại.

Bù bằng phép đo trên trang **đã dựng**, chặt hơn hẳn:

| | |
|---|---|
| Số trang | 15 |
| Nút/liên kết đã dựng | **998** |
| Thiếu tên đọc được | **0** |

**Đối chứng âm bắt buộc** — gỡ `aria-label` và `title` khỏi một trang:

```
bản thật          : 76 nút, 0 thiếu tên
bản gỡ aria-label : 76 nút, 32 thiếu tên
```

Phép kiểm **kêu được** (0 → 32), nên số 0 ở trên đáng tin.

Nó cũng bắt được một lỗi thật trong chính đợt này: thẻ `<a th:replace>` rỗng bị đọc là liên kết
chỉ có biểu tượng. Cách xử lý là **đổi thẻ chủ sang `<div>`**, không nới lỏng phép kiểm.

---

## CỔNG 4 — Nghiệm thu

### Số liệu

| Tiêu chí | Kết quả |
|---|---|
| `mvnw test` | **315 / 315**, 0 lỗi |
| 8 script giao diện | **215 / 215**, 0 sai |
| 3 phép kiểm Python | đạt cả ba |
| Màn hình trả 200 | **25 / 25** |

### Tương phản WCAG AA — đo trên trang thật, 12 màn hình

| Bề rộng | Cặp trượt AA | Trang cuộn ngang |
|---|---|---|
| 1024×768 *(máy chiếu)* | **0** | **0** |
| 1366×768 | **0** | **0** |
| 1920×1080 | **0** | **0** |

Trước đợt này, đo cùng cách trên hai trang đã cho **5 cặp trượt**. Cặp cuối cùng tìm ra ở cổng 4
là `.text-secondary` — nó giải ra từ `--bs-secondary-rgb`, **khác chỗ** với nút viền đã sửa ở Đ3;
đã trỏ về `--chu-mo`.

### Sáu bảng đã bọc `.table-responsive`

Thử nhét chuỗi 200 ký tự liền không khoảng trắng (chỉ sửa DOM trình duyệt, **không** ghi CSDL):

| Chỗ | `scrollWidth` trước → sau | Tràn |
|---|---|---|
| `/bao-cao/cong-no` bảng 1 | 1920 → 1920 | không |
| `/bao-cao/thue-bao` bảng 1 | 1905 → 1905 | không |
| `/bao-cao/thue-bao` bảng 2 | 1905 → 1905 | không |
| `/cdr/import` bảng 1 | 1920 → 1905 | không |

Hai bảng ở `/cdr/sinh-du-lieu` chỉ hiện **sau khi sinh dữ liệu** nên không dựng được ở trạng
thái hiện tại; lớp bọc đã xác minh trong mã nguồn.

*(Đối chiếu: cùng phép thử này ở vòng 4 làm `scrollWidth` nhảy 1265 → **2608**.)*

### Cỡ chữ biểu đồ

`Chart.defaults.font.size` = **14** (trước 12).

### Ba bất biến

| Bất biến | Kết quả |
|---|---|
| `con_no = tong_thanh_toan − da_thanh_toan` | **0 lệch** / 280 hóa đơn |
| `da_thanh_toan = SUM(thanh_toan.so_tien)` | **0 lệch** |
| Sổ cái `so_du = SUM(nạp+điều chỉnh) − SUM(trừ)` | **0 lệch** / 80 thuê bao |
| `so_du_sau = so_du_truoc ± so_tien` từng dòng | **0 lệch** |
| Điều hướng (`KiemTraDieuHuongTest`) | **5 / 5** |

Dữ liệu không đổi: 280 hóa đơn · 161 thanh toán · kỳ 8/2026 vẫn rỗng.

---

## Bảng chấm điểm — vòng 4 so với sau G4

| Trục | Vòng 4 | Sau G4 | Vì sao đổi |
|---|:---:|:---:|---|
| 1. Tính nhất quán | 3/5 | **5/5** | Ba khối lặp gom về một nguồn; khoảng cách lệch thang 38→0; cỡ chữ 15→8 bậc; độ dày viền gõ cứng 21→0. Hai phát hiện cũ hoá ra là báo động giả nên điểm cũ vốn đã hơi thấp. |
| 2. Kiến trúc thông tin | 2/5 | **4/5** | Đ6 trả nút nổi bật về đúng việc trên ba màn hình tiền bạc. **Chưa 5/5**: cột 1 của `/hoa-don` vẫn là "Mã hóa đơn" — trường duy nhất không lọc được; sửa cần đụng `dto/BoLocHoaDon`, ngoài phạm vi. |
| 3. Luồng công việc | 3/5 | **3/5** | **Không đổi.** Đ12 bị loại khỏi phạm vi — lối tắt "Ghi nhận thanh toán" vẫn dẫn tới `/cong-no` (0 nút ghi nhận), vẫn 5 lần bấm. Đây là thiết kế lại luồng nghiệp vụ, không phải chỉnh giao diện. |
| 4. Khả năng tiếp cận | 2/5 | **5/5** | 5 cặp trượt AA → **0** ở cả ba bề rộng. Vòng tiêu điểm 1,38 → 6,39/8,67:1. Vùng chạm 32×40 → 40×40. Bốn trạng thái tương tác đủ. **Còn lại**: 22 ô nhập lọc thiếu `id` (Đ8, cân nhắc) và heading nhảy h2→h5 trong modal ẩn. |
| 5. Trạng thái biên | 4/5 | **5/5** | 6 bảng thiếu lớp bọc → 0; đo lại bằng đúng phép thử từng làm vỡ trang thì không còn tràn. |
| 6. Ngữ cảnh máy chiếu | 3/5 | **4/5** | Chữ biểu đồ 12 → 14px; tiêu đề trang 21,6 → 24px. **Chưa 5/5**: huy hiệu trạng thái vẫn **10,8px** (Đ9) — nâng lên đổi mật độ mọi bảng, là quyết định về hình thức nên để người làm đồ án quyết. |

---

## Còn lại, cố ý không làm

| | Vì sao |
|---|---|
| **Đ8** gắn `id` cho 22 ô nhập lọc | M, cần chạy lại 215 phép kiểm; giá trị trung bình vì người dùng đích không dùng trình đọc màn hình |
| **Đ9** huy hiệu 10,8px → 13px | Đổi mật độ **mọi** bảng — quyết định hình thức, không phải vá lỗi. Một dòng: `.badge { font-size: .8125rem; }` |
| **Đ12** lối tắt "Ghi nhận thanh toán" | Thiết kế lại luồng nghiệp vụ, đụng controller — ngoài phạm vi đã giao |
| **Đ14** thống nhất số dòng mỗi trang | Gần như chắc chắn làm đỏ script, giá trị thấp |
| Cột 1 của `/hoa-don` | Sửa cần thêm trường vào `dto/BoLocHoaDon` — tầng bị cấm chạm |
