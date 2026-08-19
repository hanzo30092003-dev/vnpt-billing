# DANH SÁCH ẢNH CHỤP MÀN HÌNH — TOÀN DỰ ÁN

> ### ⚠️ Giao diện đã làm lại toàn bộ ở đợt cuối
> Ảnh chụp **trước** đợt làm lại giao diện **không dùng lại được**. Mọi màn hình nay có thêm
> một dòng giải thích dưới tiêu đề; menu bên trái gom thành 5 nhóm theo công việc; trang chủ
> có thêm khối *Việc thường làm*; và chữ hiển thị đã bỏ hết thuật ngữ
> (CDR → *Cuộc gọi & tin nhắn*, Kỳ cước → *Tháng tính tiền*, Quota → *Mức miễn phí*…).
> Chi tiết: [`PHASE-8-REPORT.md`](PHASE-8-REPORT.md).
>
> **Ba ảnh cần chụp thêm cho đợt này:**
>
> | # | Màn hình | Cần thấy rõ |
> |---|---|---|
> | 63 | Trang chủ `/` | Khối **Việc thường làm** với 6 nút lớn có icon |
> | 64 | Menu bên trái của `admin` | Đủ **6 nhóm**: Khách hàng & thuê bao · Thu tiền & công nợ · Tính tiền hằng tháng · Báo cáo · Danh mục · **Quản trị** |
> | 65 | `/hoa-don?kyCuocId=8` | Bảng rỗng có câu giải thích **vì sao** trống + nút dẫn đường |
>
> **Bốn ảnh của đợt hoàn thiện (việc V3a — quản lý người dùng, và V3b — đổi mật khẩu):**
>
> | # | Màn hình | Cần thấy rõ |
> |---|---|---|
> | 66 | `/quan-tri/nguoi-dung` | Cột *Tình trạng* có **cả ba** huy hiệu: Đang dùng · Đã khoá · Tạm khoá tới HH:mm. Dòng của chính người đang đăng nhập mang nhãn *chính bạn* và **không có nút khoá** |
> | 67 | `/quan-tri/nguoi-dung/them` | Ô *Quyền sử dụng* ba lựa chọn; ô mật khẩu kèm câu giải thích |
> | 68 | ⭐ **Khoá tài khoản đá được phiên đang mở** | **Hai cửa sổ cạnh nhau**: bên trái `admin` vừa bấm Khoá; bên phải là phiên của người vừa bị khoá, bấm F5 thì bị đưa về trang đăng nhập. Đây là ảnh trả lời câu *"khoá tài khoản có tác dụng ngay không"* |
> | 69 | `/doi-mat-khau` (việc V3b) | Ba ô mật khẩu + khối *Cần biết trước khi đổi* bên phải. Chụp thêm một ảnh trạng thái lỗi: gõ sai mật khẩu hiện tại → khung đỏ *"Mật khẩu hiện tại không đúng"*. Đăng nhập bằng `nhanvien01` để cho thấy **mọi vai trò** đều vào được màn hình này |
>
> Ảnh 66 cần **ba** tình trạng cùng lúc, mà bộ dữ liệu mẫu chỉ có 3 tài khoản đều đang dùng.
> Cách dựng: chạy `.\scripts	est-auth.ps1` một lần — script tạo sẵn tài khoản `kiemthu01` và
> để nó ở trạng thái **đã khoá**; sau đó nhập sai mật khẩu của `ketoan01` 5 lần để có thêm một
> dòng *Tạm khoá*. Chụp xong chạy `reset` để trả dữ liệu về bộ chuẩn.

Gom từ danh sách rải trong sáu báo cáo Phase 0–6, **đánh số theo thứ tự xuất hiện trong báo
cáo cuối**. Đăng nhập `admin` trừ khi ghi khác.

> ⚠️ Trước khi chụp: chạy `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"` một lần để
> đưa dữ liệu về đúng bộ chuẩn, rồi chạy lại bình thường. Mọi con số trong cột *"điểm cần thấy
> rõ"* đều ứng với bộ dữ liệu đó.

---

## 🔴 ĐỌC TRƯỚC — ba ảnh phụ thuộc thời điểm

| Ảnh | Hạn chụp | Vì sao |
|---|---|---|
| **#31 Bảng tuổi nợ đủ 5 nhóm** | **trước 14/08/2026** | Dải tuổi nợ rộng 30 ngày, các kỳ cách nhau 30–31 ngày ⇒ cửa sổ 5 nhóm chỉ rộng 29 ngày. Từ 14/08 nhóm *61–90* rỗng |
| **#30 Công nợ** | trước 14/08/2026 | Cùng lý do — biểu đồ tuổi nợ đổi hình |
| **#3 Dashboard** | bất kỳ, nhưng ghi ngày | Thẻ *Thuê bao mới trong tháng* đổi theo tháng hiện tại |

Nếu chụp sau mốc trên, **đừng sửa dữ liệu cho khớp ảnh cũ**. Ghi chú ngày chụp dưới ảnh và giải
thích bằng một câu — đó là số học, không phải lỗi. Chi tiết: `PHASE-6-REPORT.md` mục 1.2.

---

## Chương 1 — Giới thiệu hệ thống

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 1 | Đăng nhập | `/dang-nhap`, chưa đăng nhập | Ba tài khoản demo ghi ngay trên form |
| 2 | Trang 403 | `nhanvien01` gõ `/hoa-don` | Trang lỗi tiếng Việt, có nút về trang chủ |
| 3 | 🔴 **Dashboard** | `/` sau khi đăng nhập `admin` | 4 thẻ số liệu; biểu đồ **5 cột**; 2 biểu đồ tròn; 2 bảng có nội dung |

## Chương 2 — Cơ sở dữ liệu

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 4 | Sơ đồ quan hệ 15 bảng | MySQL Workbench → Reverse Engineer | Khoá ngoại giữa `thue_bao` → `hoa_don` → `thanh_toan` |
| 5 | Hai view | Workbench, chạy `SELECT * FROM v_doanh_thu_thang` | Có số thật của 6 kỳ |

## Chương 3 — Quản lý khách hàng và thuê bao

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 6 | Danh sách khách hàng | `/khach-hang` | Bộ lọc, phân trang, badge loại khách |
| 7 | Form thêm khách — **đổi động** | `/khach-hang/them`, đổi loại khách | Nhãn đổi giữa CCCD và MST |
| 8 | Validation chặn CCCD sai | Nhập CCCD 11 số, Lưu | Thông báo đỏ ngay dưới ô, dữ liệu đã nhập **không mất** |
| 9 | Chi tiết khách hàng | `/khach-hang/1` | Bảng thuê bao thuộc khách |
| 10 | Danh sách thuê bao | `/thue-bao` | Đủ **4 màu badge** trạng thái |
| 11 | Chi tiết thuê bao — 4 tab | `/thue-bao/4` (trả trước) | Tab **Biến động số dư** với `TRU_CUOC` 204.780 đ, 205.000 → 220 đ |
| 12 | Lịch sử biến động trạng thái | `/thue-bao/45`, tab Lịch sử | Nhiều bước: hoạt động → tạm ngừng → khôi phục |

## Chương 4 — Gói cước, bảng giá, CDR

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 13 | Danh sách gói cước | `/goi-cuoc` | 5 gói, cột quota ưu đãi |
| 14 | Bảng giá | `/bang-gia` | 10 dòng; 3 dòng **giờ cao điểm** chỉ cho THOẠI, +20% |
| 15 | Tra cứu đơn giá | `/bang-gia/tra-cuu` | Trả đúng một dòng đang hiệu lực |
| 16 | **Sinh CDR có hạt giống** | `/cdr/sinh-du-lieu` | Ô **Hạt giống** và khối kết quả hiện **hạt giống đã dùng** |
| 17 | Kết quả sinh CDR | Sau khi bấm Sinh | Phân bố 70/20/10 và 55/40/5 đúng như quy tắc |
| 18 | Nhập CDR từ CSV | `/cdr/import`, nhập `mau-cdr.csv` | **17 thành công, 3 lỗi**, nêu rõ lý do từng dòng |
| 19 | Tra cứu CDR | `/cdr` | Bộ lọc nhiều tiêu chí, cột cước phí |

## Chương 5 — Engine tính cước ⭐

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 20 | Màn hình điều khiển tính cước | `/tinh-cuoc` | 6 kỳ; kỳ đã chốt chỉ xem; cột *Bước tiếp theo* |
| 21 | Hộp kết quả sau khi chạy | Huỷ rồi lập lại hóa đơn kỳ 6 | Khung xanh kèm số hóa đơn, doanh thu, thời gian |
| 22 | ⭐ **Đối soát — vượt quota data** | `/tinh-cuoc/doi-soat/21/1` | Khối 2: **`5.739,7 MB (5.877.492 KB)`** — hai đơn vị trong một ô |
| 23 | ⭐ **Đối soát — sát ranh giới quota** | `/tinh-cuoc/doi-soat/34/1` | `99,6 phút (5.978 giây)` cạnh quota 100 phút → **0 đ** |
| 24 | Khối 4 — đối chiếu hóa đơn | Cuộn cuối ảnh 22 | Cột chênh lệch **toàn 0 đ**, khung *"Khớp tuyệt đối"* |
| 25 | Dòng làm vượt ưu đãi | Ảnh 22, khối 3 | Dòng tô nền + chú thích quy tắc **không cắt đôi bản ghi** |
| 26 | **Bản in A4 bảng đối soát** | Ảnh 22 → **In** → xem trước | Mất sidebar và nút; hiện đủ mọi dòng |
| 27 | Modal cảnh báo chốt kỳ | Bấm **Chốt kỳ**, chưa xác nhận | Câu *"MỘT CHIỀU, không có đường quay lại"* |

## Chương 6 — Hóa đơn, thanh toán, công nợ

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 28 | Danh sách hóa đơn kỳ 5 | `/hoa-don?kyCuocId=2` | Đủ **ba** badge: Đã TT / TT một phần / Quá hạn |
| 29 | Chi tiết hóa đơn trả **hai đợt** | `/hoa-don/307` | Tab lịch sử thu có **2 dòng**; đã thu + còn nợ = tổng |
| 30 | 🔴 **Công nợ** | `/cong-no` | Tổng **62.322.325 đ / 165 hóa đơn**; biểu đồ tuổi nợ |
| 31 | 🔴 **Bảng tuổi nợ đủ 5 nhóm** | `/cong-no`, khối trên cùng | Cả 5 nhóm khác 0 — **chụp trước 14/08/2026** |
| 32 | Đề xuất tạm ngừng | `/cong-no`, khối cuối | Ghi chú **cảnh báo không chặn nghiệp vụ** |
| 33 | Danh sách giao dịch thanh toán | `/thanh-toan` | 161 giao dịch, đủ 3 hình thức, người thu `ketoan01` |
| 34 | Form ghi nhận thanh toán | `/thanh-toan/moi/{id}` | Số còn nợ hiện sẵn để đối chiếu |
| 35 | Chặn thu vượt số còn nợ | Nhập số lớn hơn còn nợ | Thông báo *"vượt quá số còn nợ"* |
| 36 | **Chặn huỷ hóa đơn kỳ đã thu tiền** | `/tinh-cuoc` → Huỷ hóa đơn kỳ 5 | Thông báo nêu rõ **48 giao dịch** |
| 37 | **Hóa đơn PDF** | Ảnh 29 → **Xuất PDF** | Dấu tiếng Việt đủ; tiền bằng chữ |
| 38 | **Phiếu thu PDF** | `/thanh-toan` → In phiếu thu | Tiêu đề *PHIẾU THU TIỀN* liền mạch |
| 39 | Quản lý giảm trừ | `/giam-tru` | 2 khoản: một khai **tiền**, một khai **tỷ lệ 7,50%** |
| 40 | Chặn khai cả tiền lẫn tỷ lệ | `/giam-tru/moi`, nhập cả hai | Thông báo từ `@AssertTrue` |
| 41 | Biến động số dư trả trước | `/tinh-cuoc`, khối cuối trang | 6 thuê bao dưới ngưỡng, có **icon và chữ** chứ không chỉ màu |

## Chương 7 — Báo cáo và thống kê

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 42 | Menu báo cáo | `/bao-cao` | 7 thẻ + bảng số liệu nhanh theo kỳ |
| 43 | **Doanh thu theo kỳ** | `/bao-cao/doanh-thu-ky` | Biểu đồ cột **5 kỳ** + đường tỷ lệ thu trên trục phải |
| 44 | Doanh thu theo gói cước | `/bao-cao/doanh-thu-goi-cuoc` | Tỷ trọng cộng đủ **100%** |
| 45 | Doanh thu theo loại dịch vụ | `/bao-cao/doanh-thu-dich-vu` kỳ 6 | Dòng *Giảm trừ* mang **dấu âm** |
| 46 | Thống kê thuê bao | `/bao-cao/thue-bao` | Biểu đồ đường mới/rời mạng trên **cùng một trục** |
| 47 | Top thuê bao cước cao | `/bao-cao/top-thue-bao?soLuong=50` | Đổi 10/20/50 vẫn giữ nguyên kỳ đang chọn |
| 48 | Sản lượng dịch vụ | `/bao-cao/san-luong` kỳ 7 | Cột *Kỳ trước* = kỳ 6; biến động có dấu +/− |
| 49 | **File Excel mở trong Excel** | Ảnh 43 → **Xuất Excel** | Header có nền, **freeze pane**, dòng tổng đậm, chân trang |
| 50 | **Bản in báo cáo** | Ảnh 43 → **In** → xem trước | Có tiêu đề riêng cho bản in |

## Chương 8 — Chất lượng và kiểm thử

| # | Ảnh | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 51 | **Kết quả 269 test** | Console `mvnw test` | Dòng `Tests run: 269, Failures: 0` |
| 52 | ⭐ **Test ĐỎ → XANH** | Cố ý `UPDATE thue_bao SET so_du = so_du + 1000 WHERE id = 1`, chạy `KiemTraSoCaiSoDuTest` → đỏ; khôi phục → xanh | Hai ảnh cạnh nhau; thông báo lỗi nêu **đúng thuê bao và số chênh** |
| 53 | Test bất biến thanh toán | Chạy `KiemTraBatBienThanhToanTest` | 4 test xanh |
| 54 | Test hạt giống bộ sinh CDR | Chạy `CdrGeneratorHatGiongTest` | 3 test xanh |
| 55 | **Test bất biến điều hướng** | Chạy `KiemTraDieuHuongTest` | 5 test xanh — không còn link hỏng |
| 56 | Script đi theo menu | `.\scripts\test-dieu-huong.ps1` | 13 đạt / 0 sai |
| 57 | Script rà kỳ rỗng | `.\scripts\test-ky-rong.ps1` | 28 đạt / 0 sai |
| 58 | Script trường hợp biên | `.\scripts\test-bien.ps1` | 42 đạt / 0 sai |
| 59 | Trang lỗi 400 | Gõ `/hoa-don/abc` | Nêu rõ **tham số nào sai** |
| 60 | Trang lỗi 500 có mã sự cố | (chụp nếu dựng được tình huống) | Mã sự cố để tra file log |
| 61 | Sidebar thu gọn | Thu cửa sổ dưới 992px | Nút mở menu; sidebar trượt ra; **không cuộn ngang** |
| 62 | Lịch sử Git toàn dự án | `git log --oneline` | Thấy được tiến trình 8 phase |

---

## Cách chụp và đặt tên

| Việc | Quy ước |
|---|---|
| Độ phân giải | Cửa sổ **1280×800** cho ảnh toàn màn hình |
| Cắt cúp | Giữ nguyên sidebar để người đọc định vị được đang ở đâu |
| Phóng to | Với ảnh 22, 23, 24 nên chèn thêm ảnh **phóng to đúng vùng số** |
| Đặt tên file | `NN-ten-man-hinh.png`, ví dụ `22-doi-soat-vuot-quota-data.png` |
| Chú thích | Mỗi ảnh một câu dưới ảnh, nói **điều cần thấy**, không mô tả lại ảnh |
| Ảnh 🔴 | Ghi thêm **ngày chụp** trong chú thích |

**Tối thiểu nếu thiếu thời gian** — 12 ảnh: 3, 11, 14, 16, 22, 23, 24, 29, 31, 37, 43, 51.
Bộ này đi hết một vòng nghiệp vụ và chạm đủ những điểm kỹ thuật đáng nói nhất.
