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

Kết quả trên **55 ảnh thật**:

| Phép kiểm | Kết quả |
|---|---|
| Ảnh là trang đăng nhập | **0** — kiểm bằng cả vân tay ảnh 16×16 lẫn siêu dữ liệu (`duongDanCuoi`, có ô mật khẩu) |
| Ảnh trắng / gần trắng | **0** |
| Ảnh dưới 20 KB | **0** |
| Ảnh cao dưới 400px | **0** |
| Trang lỗi bị chụp nhầm | **0** — ảnh 2 (403) và 59 (400) là **cố ý**, đã khai vào danh sách cho phép |
| Thiếu sổ tay / sổ tay thừa | **0 / 0** |

---

## Bảng đối chiếu — 55 ảnh

| # | Tên file | URL | Vai trò | Cỡ | Kiểu chụp | Ghi chú |
|---|---|---|---|--:|---|---|
| 1 | `01-dang-nhap.png` | `/dang-nhap` | (chua dang nhap) | 59 KB | khung nhìn | — |
| 2 | `02-trang-403.png` | `/hoa-don` | nhanvien01 | 138 KB | khung nhìn | — |
| 3 | `03-dashboard-admin.png` | `/` | admin | 561 KB | cả trang | — |
| 6 | `06-danh-sach-khach-hang.png` | `/khach-hang` | admin | 724 KB | cả trang | — |
| 7 | `07-form-them-khach-ca-nhan.png` | `/khach-hang/them` | admin | 223 KB | khung nhìn | — |
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
| 28 | `28-danh-sach-hoa-don-ky-5.png` | `/hoa-don?kyCuocId=2` | admin | 916 KB | cả trang | — |
| 29 | `29-chi-tiet-hoa-don-tra-hai-dot.png` | `/hoa-don/307` | admin | 474 KB | cả trang | — |
| 30 | `30-cong-no.png` | `/cong-no` | ketoan01 | 530 KB | cả trang | cắt 1366x1250 · chặn cao 21412 -> 1250 px |
| 31 | `31-bang-tuoi-no-5-nhom.png` | `/cong-no` | ketoan01 | 71 KB | cả trang | cắt 643x368 |
| 32 | `32-de-xuat-tam-ngung.png` | `/cong-no` | ketoan01 | 690 KB | cả trang | cắt 1102x1400 · chặn cao 6988 -> 1400 px |
| 33 | `33-danh-sach-thanh-toan.png` | `/thanh-toan` | ketoan01 | 925 KB | cả trang | — |
| 34 | `34-form-ghi-nhan-thanh-toan.png` | `/thanh-toan/moi/3123` | ketoan01 | 242 KB | khung nhìn | — |
| 39 | `39-danh-sach-giam-tru.png` | `/giam-tru` | admin | 303 KB | cả trang | — |
| 41 | `41-bien-dong-so-du-tra-truoc.png` | `/tinh-cuoc` | admin | 209 KB | cả trang | cắt 1102x667 |
| 42 | `42-menu-bao-cao.png` | `/bao-cao` | admin | 392 KB | cả trang | — |
| 43 | `43-bao-cao-doanh-thu-ky.png` | `/bao-cao/doanh-thu-ky` | admin | 433 KB | cả trang | — |
| 44 | `44-bao-cao-doanh-thu-goi-cuoc.png` | `/bao-cao/doanh-thu-goi-cuoc` | admin | 345 KB | cả trang | — |
| 45 | `45-bao-cao-doanh-thu-dich-vu.png` | `/bao-cao/doanh-thu-dich-vu?kyCuocId=1` | admin | 358 KB | cả trang | — |
| 46 | `46-bao-cao-thong-ke-thue-bao.png` | `/bao-cao/thue-bao` | admin | 492 KB | cả trang | — |
| 47 | `47-bao-cao-top-thue-bao.png` | `/bao-cao/top-thue-bao?soLuong=50` | admin | 1828 KB | cả trang | — |
| 48 | `48-bao-cao-san-luong.png` | `/bao-cao/san-luong?kyCuocId=3` | admin | 379 KB | cả trang | — |
| 50 | `50-ban-in-bao-cao-doanh-thu.png` | `/bao-cao/doanh-thu-ky` | admin | 258 KB | cả trang | bản in |
| 59 | `59-trang-loi-400.png` | `/hoa-don/abc` | admin | 176 KB | khung nhìn | — |
| 61 | `61-rail-thu-gon-duoi-992px.png` | `/` | admin | 206 KB | khung nhìn | — |
| 63 | `63-dashboard-nhanvien.png` | `/` | nhanvien01 | 480 KB | cả trang | — |
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
---

## Chưa chụp được — 24 mục, kèm lý do

Đặc tả lượt này cấm **mọi thao tác ghi dữ liệu** và cấm chạy 8 script trong `scripts/`. Những mục
dưới đây đều cần một trong hai thứ đó, hoặc cần công cụ ngoài trình duyệt.

### Cần thao tác GHI dữ liệu — bị cấm ở lượt này

| # | Ảnh | Cần làm gì | Vì sao chưa chụp |
|---|---|---|---|
| 17 | Kết quả sinh CDR | Bấm **Sinh dữ liệu** | Sinh CDR = ghi vào `chi_tiet_su_dung` |
| 21 | Hộp kết quả sau khi chạy | Huỷ rồi lập lại hóa đơn kỳ 6 | Xoá + tạo lại 58 hóa đơn |
| 27 | Modal cảnh báo chốt kỳ | Bấm **Chốt kỳ** | Nút mở modal, nhưng bấm nhầm một lần là **chốt kỳ vĩnh viễn** — không đáng liều |
| 36 | Chặn huỷ hóa đơn kỳ đã thu | Bấm **Huỷ hóa đơn** kỳ 5 | Cùng lý do: nếu chốt chặn không nổ thì mất 54 hóa đơn |
| 8 | Validation chặn CCCD sai | Nhập CCCD 11 số rồi **Lưu** | Gửi form = POST |
| 35 | Chặn thu vượt số còn nợ | Nhập số lớn hơn còn nợ rồi **Lưu** | Gửi form = POST |
| 40 | Chặn khai cả tiền lẫn tỷ lệ | Nhập cả hai rồi **Lưu** | Gửi form = POST |

> Bốn mục 8 · 35 · 40 và 27 **sẽ bị chặn** và không ghi được gì — nhưng *"sẽ bị chặn"* là điều
> đang cần chứng minh, nên không thể lấy nó làm lý do để bấm. Chụp tay ở một lượt được phép ghi.

### Cần công cụ ngoài trình duyệt

| # | Ảnh | Cần gì |
|---|---|---|
| 4 | Sơ đồ quan hệ 15 bảng | MySQL Workbench → Reverse Engineer |
| 5 | Hai view | MySQL Workbench |
| 37 | Hóa đơn PDF | Mở file PDF tải về |
| 38 | Phiếu thu PDF | Mở file PDF tải về |
| 49 | File Excel mở trong Excel | Microsoft Excel |
| 51 | Kết quả 342 test | Console `mvnw test` |
| 52 | Test ĐỎ → XANH | Console, và **cần `UPDATE` dữ liệu** |
| 53 | Test bất biến thanh toán | Console |
| 54 | Test hạt giống bộ sinh CDR | Console |
| 55 | Test bất biến điều hướng | Console |
| 56 | Script đi theo menu | `scripts/test-dieu-huong.ps1` — **cấm chạy lượt này** |
| 57 | Script rà kỳ rỗng | `scripts/test-ky-rong.ps1` — **cấm chạy lượt này** |
| 58 | Script trường hợp biên | `scripts/test-bien.ps1` — **cấm chạy lượt này** |
| 62 | Lịch sử Git toàn dự án | Console `git log --oneline` |

### Không dựng được tình huống

| # | Ảnh | Vì sao |
|---|---|---|
| 60 | Trang lỗi 500 có mã sự cố | Không có đường nào ép hệ thống lỗi 500 mà không sửa mã |

### Đã nằm trong ảnh khác

| # | Ảnh | Nằm ở đâu |
|---|---|---|
| 24 | Khối 4 — đối chiếu hóa đơn | Trong ảnh **22** (chụp cả trang, cao 3.965px, đủ 4 khối) |
| 25 | Dòng làm vượt ưu đãi | Trong ảnh **22**, khối 3 |

---

## Ảnh bổ sung ngoài danh sách gốc — 17 tấm

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

---

## Ghi chú vận hành

**Dung lượng:** tổng **30,5 MB** cho 55 ảnh. Thư mục `docs/screenshots/` **chưa** được thêm vào
`.gitignore` — đặc tả yêu cầu báo trước khi làm việc đó. Đây là quyết định của bạn:

* **Giữ trong git** — ảnh đi cùng báo cáo, ai clone về cũng có. Kho tăng từ ~5,8 MB lên ~36 MB.
* **Thêm vào `.gitignore`** — kho gọn, nhưng ảnh chỉ nằm trên máy này.

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
