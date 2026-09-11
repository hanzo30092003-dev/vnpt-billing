# DANH SÁCH ẢNH ĐÃ CHỤP

> Ảnh cho **Chương 5** của báo cáo. Chụp tự động bằng Playwright, ngày **10/09/2026**, trên dữ
> liệu 7 kỳ · 23.223 CDR · 338 hóa đơn · 161 thanh toán.
>
> Số thứ tự khớp `docs/danh-sach-anh-chup.md`. Ảnh **63–79** là ảnh **bổ sung** ngoài danh sách
> gốc, đánh số nối tiếp để không đụng số cũ.

---

## ⚠️ Đọc trước — một lỗi giao diện phát hiện trong lúc chụp

**6 trên 7 màn hình báo cáo hiện khối "chưa có dữ liệu" ngay phía trên bảng có đủ dữ liệu.**

Nguyên nhân: `th:if` đặt **cùng thẻ** với `th:replace`. Trong Thymeleaf 3, `th:replace` có độ ưu
tiên **100**, chạy **trước** `th:if` (300) — thẻ bị thay thế trước khi điều kiện được xét, nên
`th:if` **bị bỏ qua hoàn toàn** và khối rỗng luôn hiện.

| Template | Dòng | Câu hiện sai | Số dòng bảng thực tế |
|---|--:|---|--:|
| `bao-cao/doanh-thu-ky.html` | 14 | *"Chưa có kỳ cước nào trong hệ thống."* | **7** |
| `bao-cao/doanh-thu-goi-cuoc.html` | 15 | *"Kỳ đã chọn chưa có hóa đơn nào."* | **4** |
| `bao-cao/doanh-thu-dich-vu.html` | 15 | *"Kỳ đã chọn chưa có hóa đơn nào."* | **9** |
| `bao-cao/thue-bao.html` | 14 | *"Chưa có thuê bao nào."* | **6** |
| `bao-cao/top-thue-bao.html` | 39 | *"Kỳ đã chọn chưa có hóa đơn nào."* | **50** |
| `bao-cao/san-luong.html` | 15 | *"…chưa có cuộc gọi, tin nhắn hay lần truy cập mạng nào."* | **7** |
| `bao-cao/cong-no.html` | — | *(không dính)* | 15 |

**Không sửa ở lượt này** — đây là lượt chụp ảnh, đặc tả cấm sửa mã. Nhưng **6 ảnh 43·44·45·46·47·48
đang chứa câu nói sai đó** và nên **chụp lại sau khi sửa**, nếu không người chấm sẽ thấy màn hình
tự mâu thuẫn với chính nó.

---

## Đợt bổ sung 11/09/2026 — sửa rào phân quyền, chụp thêm 12 ảnh

### A. Lỗ hổng phân quyền trên trang chủ — đã sửa, chỉ trong `index.html`

Trước: bốn khối tiền — *Doanh thu kỳ gần nhất* (dòng 121), *Tổng công nợ* (135), *Top 5 thuê bao
cước cao nhất* (185), *5 giao dịch thanh toán gần nhất* (220) — **không có `sec:authorize`**, trong
khi `SecurityConfig:108-111` chặn nhân viên ở `/bao-cao/top-thue-bao/**` vì đúng nội dung đó.
Nhân viên bị 403 ở báo cáo nhưng nhìn được tên khách và số nợ ngay trang chủ.

Sau: cả bốn khối rào `hasAnyRole('KE_TOAN','ADMIN')`. Vai trò khác nhận **thẻ thay thế cùng khung**
với biểu tượng khoá và một câu nói rõ vì sao — không để trống, lưới bốn thẻ không vỡ.
**Không sửa `SecurityConfig`** — cấu hình đó đúng, template mới là chỗ sai.

| Vai trò | Khối tiền thật hiện ra | Thẻ khoá | |
|---|--:|--:|:--:|
| admin | **4** | 0 | ✅ |
| ketoan01 | **4** | 0 | ✅ |
| nhanvien01 | **0** | 4 | ✅ |

**Đối chứng âm:** bỏ đúng một rào (*Tổng công nợ*), dựng lại, chạy lại → phép kiểm kêu
*"nhanvien01 1 khối · Tổng công nợ"*. Khôi phục → đạt lại.

Sau sửa: `mvnw test` **342/342** · 8 script giao diện **215/215** · 3 phép kiểm template đạt.

> ⚠️ **Hai chỗ tiền còn sót, ngoài phạm vi "chỉ sửa `index.html`" — chưa sửa, cần bạn quyết:**
>
> 1. **Thanh máy** (`fragments/layout.html`) hiện *"Còn nợ 85.848.297 đ"* cho **mọi** vai trò — đúng con
>    số vừa bị khoá ở thẻ *Tổng công nợ* ngay bên dưới. Khoá thẻ mà để nguyên thanh máy là tự mâu thuẫn.
> 2. **Biểu đồ Doanh thu theo kỳ** (`index.html`, ngoài 4 khối đã nêu): tiền theo kỳ, kèm link *Xem báo
>    cáo* → `/bao-cao/doanh-thu-ky` — nhân viên bấm vào nhận 403.

### Số mục menu trên rail — phép kiểm phân quyền cho ảnh 03 · 63 · 65

Đếm trên DOM trang thật, cùng khung 1366 với lúc chụp (rail 232px, mở đầy đủ):

| Ảnh | Vai trò | Mục menu |
|---|---|--:|
| 03 | admin | **14** |
| 63 | nhanvien01 | **4** |
| 65 | ketoan01 | **6** |

03 > 63 ✅ · 03 > 65 ✅. **Đối chứng âm:** admin so với chính admin → *"bằng nhau"* — phép kiểm biết kêu.

### 12 ảnh mới — cách chụp và bằng chứng không ghi dữ liệu

| # | Cách lấy | Dữ liệu sau khi chụp |
|---|---|---|
| 63 | Chụp lại sau khi rào | — |
| 27 | Bấm *Chốt kỳ* kỳ 7 → `app.js` gọi `preventDefault()` rồi mở modal → chụp → **Esc** | kỳ 7 vẫn `MO` |
| 08 | POST CCCD 11 số → máy chủ từ chối, form giữ nguyên dữ liệu đã nhập | `khach_hang` = 50 |
| 35 | POST 999.999.999 đ cho hóa đơn còn nợ 160.648 đ → từ chối | `thanh_toan` = 161 |
| 40 | POST khai **cả** tiền lẫn tỷ lệ → `@AssertTrue` từ chối | `giam_tru` = 2 |
| 36 | POST thẳng `/tinh-cuoc/2/huy-hoa-don` (UI đã ẩn nút) → *"Kỳ cước tháng 5/2026 đã chốt, không thể hủy hóa đơn"* | `hoa_don` = 338 |
| 37 · 38 · 51 · 52a · 52b · 80 · 81 | Ảnh **chụp tay** của bạn, đổi tên từ zip (hash khớp từng file) | — |

Dữ liệu kiểm **sau từng ảnh**: 7 kỳ · 23.223 CDR · 338 hóa đơn · 161 thanh toán · kỳ 9 `MO` 0/0 — không
lệch lần nào.

> **Ảnh 36 khác mô tả trong danh sách gốc.** Danh sách ghi thông báo *"48 giao dịch"* (chặn huỷ vì đã thu
> tiền). Nhưng kỳ 5 nay đã **`DA_CHOT`**, và rào "đã chốt" đứng **trước** rào "đã thu tiền" trong
> `huyBillingKy`, nên thông báo thật là *"đã chốt, không thể hủy"*. Muốn ra đúng câu *"48 giao dịch"*
> phải mở lại kỳ 5 — một thao tác ghi. Ảnh vẫn chứng minh điều quan trọng hơn: **gõ thẳng đường dẫn
> vẫn bị chặn ở tầng nghiệp vụ** (bài học 4.4), dù giao diện đã ẩn nút.

> **Hai lần chụp sai của tôi, đã chụp lại.** Ảnh 35 lượt đầu hiện *"Vui lòng chọn hình thức thanh toán"*
> — tôi bỏ trống ô hình thức nên `@NotNull` nổ **trước** rào "vượt số còn nợ". Ảnh 40 lượt đầu lẫn hai
> lỗi *"Vui lòng chọn…"* vì bỏ trống thuê bao và loại. Cả hai chụp lại với các ô khác đã điền đúng, để
> **chỉ còn đúng một lỗi** cần thấy.

### Thanh máy: 148 hôm qua, 90 hôm nay — không phải lỗi, nhưng cần biết

Thanh máy đếm hóa đơn **theo trạng thái** `QUA_HAN`; bảng tuổi nợ đếm **theo ngày**. Hôm nay
`test-muc-F.ps1` (một trong 8 script bạn yêu cầu chạy) huỷ rồi lập lại 58 hóa đơn kỳ 6 — hóa đơn mới
sinh ra ở `CHUA_TT`, nên thanh máy tụt từ 148 xuống **90** dù không hóa đơn nào bớt quá hạn.

`capNhatQuaHan()` chạy lúc **00:05** hằng ngày *hoặc* khi ai đó **mở `/hoa-don`** (`LichChayNenConfig`,
thiết kế cố ý). Lần mở `/hoa-don` kế tiếp sẽ trả về 148. Tôi **không tự kích hoạt** vì đó là một thao
tác đổi trạng thái. Hệ quả: 6 ảnh chụp hôm nay (08 · 27 · 35 · 36 · 40 · 63) ghi **90** trên thanh máy,
55 ảnh hôm qua ghi **148**. Nếu muốn đồng nhất: mở `/hoa-don` một lần rồi chụp lại 6 ảnh đó.

### F. Rail chỉ phủ khung nhìn đầu — đo trên 37 ảnh chụp cả trang có rail

Rail là `position: fixed`, cao bằng khung nhìn. Đo cột x = 60px trên từng ảnh: rail dừng đúng **768px**
ở **mọi** ảnh. Đối chứng: 9 ảnh cao ≤ 780px ra 100%, 18 ảnh cao > 1500px ra trung bình 35%.

| Mức | Rail phủ | Số ảnh |
|---|---|--:|
| **Nặng** | dưới 60% | **20** |
| Vừa | 60–95% | 8 |
| Ổn | từ 95% | 9 |

Danh sách ảnh **nặng** (cột tối cụt ngang rõ nhất khi in):

| # | Ảnh | Cao (px) | Rail phủ |
|---|---|--:|--:|
| 77 | `77-hoa-don-cua-ky-8.png` | 4530 | 17% |
| 23 | `23-doi-soat-sat-ranh-gioi-quota.png` | 3986 | 19% |
| 22 | `22-doi-soat-vuot-quota-data.png` | 3965 | 19% |
| 70 | `70-doi-soat-ky-8.png` | 3965 | 19% |
| 47 | `47-bao-cao-top-thue-bao.png` | 3632 | 21% |
| 28 | `28-danh-sach-hoa-don-ky-5.png` | 2053 | 37% |
| 75 | `75-danh-sach-hoa-don-tat-ca.png` | 2014 | 38% |
| 79 | `79-danh-sach-hoa-don-ky-8.png` | 2014 | 38% |
| 6 | `06-danh-sach-khach-hang.png` | 1970 | 39% |
| 19 | `19-tra-cuu-cdr.png` | 1962 | 39% |
| 78 | `78-tra-cuu-cdr-co-bo-loc.png` | 1962 | 39% |
| 46 | `46-bao-cao-thong-ke-thue-bao.png` | 1889 | 41% |
| 20 | `20-man-hinh-tinh-cuoc.png` | 1869 | 41% |
| 33 | `33-danh-sach-thanh-toan.png` | 1825 | 42% |
| 3 | `03-dashboard-admin.png` | 1750 | 44% |
| 63 | `63-dashboard-nhanvien.png` | 1650 | 47% |
| 65 | `65-dashboard-ketoan.png` | 1650 | 47% |
| 14 | `14-bang-gia.png` | 1526 | 50% |
| 43 | `43-bao-cao-doanh-thu-ky.png` | 1399 | 55% |
| 10 | `10-danh-sach-thue-bao.png` | 1370 | 56% |

**Chưa sửa — bạn quyết.** Ba hướng: (a) chấp nhận, rail cụt là tính chất của ảnh cả trang; (b) chụp
**khung nhìn** thay vì cả trang cho các trang danh sách dài — mất phần dưới nhưng rail liền; (c) chụp
cả trang rồi cắt bỏ rail bằng `clip` từ x = 232px — mất menu nhưng ảnh sạch. Hướng (c) hợp với báo cáo
in nhất vì menu đã có riêng ở ảnh 03/63/65.

---

## Cấu hình chụp

| | |
|---|---|
| Công cụ | Playwright (`playwright-core`) + Microsoft Edge đã cài sẵn |
| Khung nhìn | **1366 × 768** |
| `deviceScaleFactor` | **2** → ảnh ra 2732px, bố cục vẫn 1366 |
| Cuộn ngang | **0** trên toàn bộ 55 trang |
| Nơi đặt công cụ | `tools-chup-anh/` — đã `.gitignore`, **không** đụng `pom.xml` |

**Vì sao 1366 chứ không 1920.** Báo cáo A4 lề trái 4 cm, ba lề còn lại 2,5 cm → bề rộng dùng được
**14,5 cm**. Ảnh 1920px thu về đó còn ~38%: chữ 13px trên màn hình thành ~5px trên giấy, không đọc
được khi in. Ở 1366px tỷ lệ thu là ~54%, chữ 13px còn ~7px — đọc được. `deviceScaleFactor: 2` giữ
độ nét mà không đổi bố cục.

**Ba ảnh cắt vùng thay vì chụp cả trang:** `/cong-no` cao **21.412px** vì liệt kê hết 223 hóa đơn
còn nợ không phân trang; chụp cả trang ra ảnh **10,7 MB** vô dụng trong Word, và ba mục 30/31/32
sẽ ra ba tấm gần như giống hệt nhau. Ba mục đó thực ra là **ba khối khác nhau trên cùng một trang**.

**Một ảnh dùng khung nhìn khác:** ảnh 61 chụp ở **900 × 820** — chính danh sách gốc đòi *"thu cửa
sổ dưới 992px"* để thấy rail thu gọn.

---

## Kiểm chứng ảnh — kèm đối chứng âm

Trước khi tin bất kỳ số 0 nào, bộ kiểm tự tạo **hai ảnh mồi** rồi chứng minh nó bắt được cả hai:

| Ảnh mồi | Phép kiểm có bắt được? |
|---|---|
| Ảnh trắng tinh 1366×768 | ✅ `TRANG (chỉ 0.00% pixel khác màu nền)` + `QUÁ NHỎ (5 KB)` |
| Bản sao ảnh trang đăng nhập | ✅ `LÀ TRANG ĐĂNG NHẬP (lệch vân tay 0.0)` |

> **Đối chứng âm đã bắt được một lỗi trong chính bộ kiểm.** Lượt đầu ảnh mồi trắng chỉ bị bắt bởi
> phép "quá nhỏ", **không** bởi phép kiểm trắng. Nguyên nhân: tôi lượng tử hoá màu nền bằng 4 bit
> cao rồi dựng lại bằng `×16`, nên trắng tinh (255) dựng lại thành **240** — mỗi pixel lệch 45,
> vượt ngưỡng 40, và ảnh trắng tuyền bị tính là **100% nội dung**. Phép kiểm trắng **không bao giờ
> nổ được**. Sửa sang lấy **tâm khoang** (`×16 + 8`) thì mồi nổ đúng.

Kết quả trên **60 ảnh tự động** (chạy lại 11/09 sau khi thêm 12 ảnh; 7 ảnh chụp tay cũng qua cùng bộ kiểm):

| Phép kiểm | Kết quả |
|---|---|
| Ảnh là trang đăng nhập | **0** — kiểm bằng cả vân tay ảnh 16×16 lẫn siêu dữ liệu (`duongDanCuoi`, có ô mật khẩu) |
| Ảnh trắng / gần trắng | **0** |
| Ảnh dưới 20 KB | **0** |
| Ảnh cao dưới 400px | **0** |
| Trang lỗi bị chụp nhầm | **0** — ảnh 2 (403) và 59 (400) là **cố ý**, đã khai vào danh sách cho phép |
| Thiếu sổ tay / sổ tay thừa | **0 / 0** với ảnh tự động — 7 ảnh chụp tay không có sổ tay, đúng bản chất |
| Ảnh cao dưới 400px — **4 ảnh chụp tay** (51 · 52a · 52b · 80: 161–182px) | Cảnh báo **đúng**: đó là ảnh cắt gọn một dòng console và một biểu tượng. Giữ nguyên phép kiểm, không tắt |

---

## Bảng đối chiếu — 67 ảnh

| # | Tên file | URL / nội dung | Vai trò | Cỡ | Kiểu chụp | Ghi chú |
|---|---|---|---|--:|---|---|
| 1 | `01-dang-nhap.png` | `/dang-nhap` | (chua dang nhap) | 59 KB | khung nhìn | — |
| 2 | `02-trang-403.png` | `/hoa-don` | nhanvien01 | 138 KB | khung nhìn | — |
| 3 | `03-dashboard-admin.png` | `/` | admin | 561 KB | cả trang | — |
| 6 | `06-danh-sach-khach-hang.png` | `/khach-hang` | admin | 724 KB | cả trang | — |
| 7 | `07-form-them-khach-ca-nhan.png` | `/khach-hang/them` | admin | 223 KB | khung nhìn | — |
| 8 | `08-validation-chan-cccd-sai.png` | `/khach-hang/luu` | admin | 275 KB | cả trang | POST bị từ chối |
| 9 | `09-chi-tiet-khach-hang.png` | `/khach-hang/1` | admin | 271 KB | cả trang | — |
| 10 | `10-danh-sach-thue-bao.png` | `/thue-bao` | admin | 625 KB | cả trang | — |
| 11 | `11-chi-tiet-thue-bao-tra-truoc.png` | `/thue-bao/4` | admin | 265 KB | cả trang | — |
| 12 | `12-lich-su-bien-dong-trang-thai.png` | `/thue-bao/45` | admin | 262 KB | cả trang | tab `#tab-trang-thai` |
| 13 | `13-danh-sach-goi-cuoc.png` | `/goi-cuoc` | admin | 356 KB | cả trang | — |
| 14 | `14-bang-gia.png` | `/bang-gia` | admin | 500 KB | cả trang | — |
| 15 | `15-tra-cuu-don-gia.png` | `/bang-gia/tra-cuu` | admin | 406 KB | khung nhìn | — |
| 16 | `16-form-sinh-cdr.png` | `/cdr/sinh-du-lieu` | admin | 310 KB | khung nhìn | — |
| 18 | `18-form-nhap-cdr-csv.png` | `/cdr/import` | admin | 395 KB | khung nhìn | — |
| 19 | `19-tra-cuu-cdr.png` | `/cdr` | admin | 871 KB | cả trang | — |
| 20 | `20-man-hinh-tinh-cuoc.png` | `/tinh-cuoc` | admin | 706 KB | cả trang | — |
| 22 | `22-doi-soat-vuot-quota-data.png` | `/tinh-cuoc/doi-soat/21/1` | admin | 1627 KB | cả trang | — |
| 23 | `23-doi-soat-sat-ranh-gioi-quota.png` | `/tinh-cuoc/doi-soat/34/1` | admin | 1674 KB | cả trang | — |
| 26 | `26-ban-in-a4-bang-doi-soat.png` | `/tinh-cuoc/doi-soat/21/1` | admin | 1792 KB | cả trang | bản in |
| 27 | `27-modal-canh-bao-chot-ky.png` | `/tinh-cuoc` | admin | 655 KB | cả trang | modal, chưa xác nhận |
| 28 | `28-danh-sach-hoa-don-ky-5.png` | `/hoa-don?kyCuocId=2` | admin | 916 KB | cả trang | — |
| 29 | `29-chi-tiet-hoa-don-tra-hai-dot.png` | `/hoa-don/307` | admin | 474 KB | cả trang | — |
| 30 | `30-cong-no.png` | `/cong-no` | ketoan01 | 530 KB | cả trang | cắt 1366x1250 · chặn cao 21412 -> 1250 px |
| 31 | `31-bang-tuoi-no-5-nhom.png` | `/cong-no` | ketoan01 | 71 KB | cả trang | cắt 643x368 |
| 32 | `32-de-xuat-tam-ngung.png` | `/cong-no` | ketoan01 | 690 KB | cả trang | cắt 1102x1400 · chặn cao 6988 -> 1400 px |
| 33 | `33-danh-sach-thanh-toan.png` | `/thanh-toan` | ketoan01 | 925 KB | cả trang | — |
| 34 | `34-form-ghi-nhan-thanh-toan.png` | `/thanh-toan/moi/3123` | ketoan01 | 242 KB | khung nhìn | — |
| 35 | `35-chan-thu-vuot-so-con-no.png` | `/thanh-toan (POST)` | ketoan01 | 317 KB | cả trang | POST bị từ chối |
| 36 | `36-chan-huy-hoa-don-ky-da-chot.png` | `POST /tinh-cuoc/2/huy-hoa-don` | admin | 722 KB | cả trang | POST bị từ chối |
| 37 | `37-hoa-don-pdf.png` | PDF hóa đơn HD202608-000058 | (tự chụp) | 147 KB | chụp tay | Snipping Tool, không qua Playwright |
| 38 | `38-phieu-thu-pdf.png` | PDF phiếu thu TT20260620-0004 | (tự chụp) | 97 KB | chụp tay | Snipping Tool, không qua Playwright |
| 39 | `39-danh-sach-giam-tru.png` | `/giam-tru` | admin | 303 KB | cả trang | — |
| 40 | `40-chan-khai-ca-tien-lan-ty-le.png` | `/giam-tru (POST)` | admin | 292 KB | cả trang | POST bị từ chối |
| 41 | `41-bien-dong-so-du-tra-truoc.png` | `/tinh-cuoc` | admin | 209 KB | cả trang | cắt 1102x667 |
| 42 | `42-menu-bao-cao.png` | `/bao-cao` | admin | 392 KB | cả trang | — |
| 43 | `43-bao-cao-doanh-thu-ky.png` | `/bao-cao/doanh-thu-ky` | admin | 433 KB | cả trang | — |
| 44 | `44-bao-cao-doanh-thu-goi-cuoc.png` | `/bao-cao/doanh-thu-goi-cuoc` | admin | 345 KB | cả trang | — |
| 45 | `45-bao-cao-doanh-thu-dich-vu.png` | `/bao-cao/doanh-thu-dich-vu?kyCuocId=1` | admin | 358 KB | cả trang | — |
| 46 | `46-bao-cao-thong-ke-thue-bao.png` | `/bao-cao/thue-bao` | admin | 492 KB | cả trang | — |
| 47 | `47-bao-cao-top-thue-bao.png` | `/bao-cao/top-thue-bao?soLuong=50` | admin | 1828 KB | cả trang | — |
| 48 | `48-bao-cao-san-luong.png` | `/bao-cao/san-luong?kyCuocId=3` | admin | 379 KB | cả trang | — |
| 50 | `50-ban-in-bao-cao-doanh-thu.png` | `/bao-cao/doanh-thu-ky` | admin | 258 KB | cả trang | bản in |
| 51 | `51-ket-qua-342-test-tu-dong.png` | Console: Tests run 342, Failures 0 | (tự chụp) | 15 KB | chụp tay | Snipping Tool, không qua Playwright |
| 52a | `52a-doi-chung-am-do-27-test-4-loi.png` | Console: 27 test, 4 lỗi sau khi gỡ luật | (tự chụp) | 14 KB | chụp tay | Snipping Tool, không qua Playwright |
| 52b | `52b-doi-chung-am-xanh-27-test-0-loi.png` | Console: 27 test, 0 lỗi sau khôi phục | (tự chụp) | 15 KB | chụp tay | Snipping Tool, không qua Playwright |
| 59 | `59-trang-loi-400.png` | `/hoa-don/abc` | admin | 176 KB | khung nhìn | — |
| 61 | `61-rail-thu-gon-duoi-992px.png` | `/` | admin | 206 KB | khung nhìn | — |
| 63 | `63-dashboard-nhanvien.png` | `/` | nhanvien01 | 363 KB | cả trang | — |
| 64 | `64-bao-cao-cong-no.png` | `/bao-cao/cong-no` | ketoan01 | 411 KB | cả trang | — |
| 65 | `65-dashboard-ketoan.png` | `/` | ketoan01 | 480 KB | cả trang | — |
| 66 | `66-form-them-khach-doanh-nghiep.png` | `/khach-hang/them` | admin | 223 KB | khung nhìn | — |
| 67 | `67-form-dang-ky-thue-bao.png` | `/thue-bao/dang-ky` | admin | 257 KB | khung nhìn | — |
| 68 | `68-chi-tiet-goi-cuoc.png` | `/goi-cuoc/1` | admin | 339 KB | cả trang | — |
| 69 | `69-danh-sach-ky-cuoc.png` | `/ky-cuoc` | admin | 436 KB | cả trang | — |
| 70 | `70-doi-soat-ky-8.png` | `/tinh-cuoc/doi-soat/21/8` | admin | 1617 KB | cả trang | — |
| 71 | `71-quan-tri-nguoi-dung.png` | `/quan-tri/nguoi-dung` | admin | 304 KB | cả trang | — |
| 72 | `72-no-vuot-han-muc-tin-dung.png` | `/cong-no` | ketoan01 | 185 KB | cả trang | cắt 1102x467 |
| 73 | `73-thue-bao-tab-bien-dong-so-du.png` | `/thue-bao/4` | admin | 291 KB | cả trang | tab `#tab-so-du` |
| 74 | `74-thue-bao-tab-lich-su-goi-cuoc.png` | `/thue-bao/4` | admin | 241 KB | cả trang | tab `#tab-goi` |
| 75 | `75-danh-sach-hoa-don-tat-ca.png` | `/hoa-don` | admin | 909 KB | cả trang | — |
| 76 | `76-chi-tiet-thue-bao-tra-sau.png` | `/thue-bao/21` | admin | 253 KB | cả trang | — |
| 77 | `77-hoa-don-cua-ky-8.png` | `/tinh-cuoc/ky/8` | admin | 1936 KB | cả trang | — |
| 78 | `78-tra-cuu-cdr-co-bo-loc.png` | `/cdr?loaiDichVu=DATA&huong=NOI_MANG` | admin | 950 KB | cả trang | — |
| 79 | `79-danh-sach-hoa-don-ky-8.png` | `/hoa-don?kyCuocId=8` | admin | 925 KB | cả trang | — |
| 80 | `80-bieu-tuong-desktop.png` | Biểu tượng trên màn hình nền | (tự chụp) | 29 KB | chụp tay | Snipping Tool, không qua Playwright |
| 81 | `81-cua-so-khoi-dong.png` | Cửa sổ khởi động năm bước | (tự chụp) | 49 KB | chụp tay | Snipping Tool, không qua Playwright |

---

## Chưa chụp được — 15 mục, kèm lý do

### Cần ghi dữ liệu thật — không làm

| # | Ảnh | Vì sao |
|---|---|---|
| 17 | Kết quả sinh CDR | Sinh CDR = ghi vào `chi_tiet_su_dung` |
| 21 | Hộp kết quả sau khi chạy | Huỷ rồi lập lại 58 hóa đơn kỳ 6 |

### Cần công cụ ngoài trình duyệt

| # | Ảnh | Cần gì |
|---|---|---|
| 4 | Sơ đồ quan hệ 15 bảng | MySQL Workbench → Reverse Engineer |
| 5 | Hai view | MySQL Workbench |
| 49 | File Excel mở trong Excel | Microsoft Excel |
| 53 · 54 · 55 | Ba lớp test bất biến | Console `mvnw test -Dtest=…` |
| 56 · 57 · 58 | Ba script giao diện | Console — 15/28/42 đạt, đã chạy ở đợt này, chỉ thiếu ảnh |
| 62 | Lịch sử Git | Console `git log --oneline` |

### Không dựng được tình huống

| # | Ảnh | Vì sao |
|---|---|---|
| 60 | Trang lỗi 500 có mã sự cố | Không có đường nào ép lỗi 500 mà không sửa mã |

### Đã nằm trong ảnh khác

| # | Ảnh | Nằm ở đâu |
|---|---|---|
| 24 · 25 | Khối 4 đối chiếu · dòng vượt ưu đãi | Trong ảnh **22** (cả trang, đủ 4 khối) |

## Ảnh bổ sung ngoài danh sách gốc — 19 tấm

Danh sách gốc không có, nhưng đặc tả lượt này yêu cầu (dashboard cả ba vai trò, form đổi động,
đủ các tab của chi tiết thuê bao) hoặc bổ sung cho đầy đủ phân hệ:

| # | Ảnh | Vì sao thêm |
|---|---|---|
| 63 · 65 | Dashboard `nhanvien01` · `ketoan01` | Đặc tả đòi dashboard **cả ba vai trò** để thấy menu khác nhau |
| 64 | Báo cáo công nợ | Nhóm 1 của đặc tả |
| 66 | Form thêm khách — **Doanh nghiệp** | Đặc tả đòi 2 ảnh riêng để thấy form đổi động |
| 67 | Form đăng ký thuê bao | Phân hệ thuê bao |
| 68 | Chi tiết gói cước | Phân hệ gói cước |
| 69 | Danh sách 7 kỳ cước | Đặc tả nhóm 3 |
| 70 | Đối soát kỳ 8 | Kỳ mới thêm ở đợt G8 |
| 71 | Quản trị người dùng | Phân hệ quản trị |
| 72 | Nợ vượt hạn mức tín dụng | Khối riêng trên `/cong-no` |
| 73 · 74 | Tab **Biến động số dư** · tab **Lịch sử gói cước** | Nội dung tab ẩn **không** nằm trong ảnh chụp cả trang — phải bấm tab mới hiện |
| 75 · 79 | Danh sách hóa đơn toàn bộ · lọc kỳ 8 | |
| 76 | Chi tiết thuê bao **trả sau** | Ảnh 11 là trả trước |
| 77 | Hóa đơn của kỳ 8 | |
| 78 | Tra cứu CDR **có bộ lọc** | Ảnh 19 chưa lọc |
| 80 · 81 | Biểu tượng Desktop · cửa sổ khởi động | Chụp tay — cách mở phần mềm bằng một cú nháy đúp (đợt G7), không có trong danh sách gốc |

---

## Ghi chú vận hành

**Dung lượng:** tổng **32,9 MB** cho 67 ảnh. Thư mục `docs/screenshots/` **giữ trong git** — quyết
định ngày 10/09: ảnh là tài liệu của báo cáo, ai clone kho về cũng phải có. `tools-chup-anh/` thì
vẫn `.gitignore` vì đó là công cụ cá nhân, không phải phần mềm được chấm.

**Dữ liệu sau khi chụp — không suy suyển:**

| | |
|---|---|
| 7 kỳ · 23.223 CDR · 338 hóa đơn · 753 chi tiết · 161 thanh toán · 50 dòng sổ cái | ✅ y nguyên |
| Kỳ 9/2026 | ✅ vẫn rỗng, `MO`, 0 CDR, 0 hóa đơn |
| Bất biến `con_no` · `da_thanh_toan` · sổ cái số dư | ✅ **0 · 0 · 0** lệch |

**Không chạy:** script nào trong `scripts/`, profile `reset`, tính cước, lập hóa đơn, huỷ hóa đơn,
trừ cước, chốt kỳ, sinh CDR. Lần POST **duy nhất** trong toàn bộ lượt là đăng nhập.

**Ba thao tác giao diện đã dùng, đều không ghi dữ liệu:** bấm tab Bootstrap (chỉ đổi lớp CSS),
đổi ô chọn *Loại khách hàng* trên form **chưa gửi**, và `emulateMedia('print')` để chụp bản in.

**Chạy lại:**

```bash
node tools-chup-anh/chup.mjs tatca
```

```bash
node tools-chup-anh/kiem-anh.mjs
```
