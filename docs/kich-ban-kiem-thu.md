# KỊCH BẢN KIỂM THỬ THỦ CÔNG

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Ngày lập:** 10/08/2026 · **Phiên bản:** Phase 7

> ⚠️ Toàn bộ dữ liệu là **dữ liệu mẫu tự sinh** phục vụ học tập, không phải dữ liệu thật của
> bất kỳ nhà mạng nào.

---

## 1. Chuẩn bị trước khi kiểm thử

| Bước | Việc |
|---|---|
| 1 | MySQL 8 đang chạy; biến môi trường `MYSQL_PASSWORD` đã đặt |
| 2 | Nạp lại bộ dữ liệu chuẩn: `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"` |
| 3 | Dừng ứng dụng, chạy lại bình thường: `mvnw spring-boot:run` |
| 4 | Mở <http://localhost:8080> |

**Ba tài khoản** (mật khẩu đều là `123456`):

| Tài khoản | Vai trò | Phạm vi |
|---|---|---|
| `admin` | Quản trị | Toàn quyền |
| `nhanvien01` | Nhân viên | Khách hàng, thuê bao, báo cáo |
| `ketoan01` | Kế toán | Hóa đơn, thanh toán, công nợ, giảm trừ, báo cáo |

**Trạng thái dữ liệu chuẩn:** 6 kỳ cước (3, 4, 5/2026 đã chốt · 6, 7/2026 mở · **8/2026 rỗng**) ·
18.723 CDR · 280 hóa đơn · 161 giao dịch thanh toán · 80 thuê bao · 50 khách hàng.

---

## 2. Bảng kiểm thử

Cột **Kết quả thực tế** và **Đạt/Không** để trống cho người kiểm điền khi chạy.

### 2.1. Xác thực và phân quyền

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 1 | Đăng nhập đúng | Mở `/`, nhập tài khoản, bấm Đăng nhập | `admin` / `123456` | Vào trang chủ, header hiện "Quản trị hệ thống" và badge vai trò | | |
| 2 | Đăng nhập sai mật khẩu | Như trên | `admin` / `sai` | Ở lại form, hiện thông báo lỗi, **không** vào được | | |
| 3 | Chưa đăng nhập | Gõ thẳng `/hoa-don` khi chưa đăng nhập | — | Bị đẩy về form đăng nhập | | |
| 4 | Phân quyền nhân viên | Đăng nhập `nhanvien01`, gõ `/hoa-don` | — | Trang **403** tiếng Việt, không phải 500 | | |
| 5 | Phân quyền kế toán | Đăng nhập `ketoan01`, gõ `/khach-hang` | — | Trang **403** | | |
| 6 | Menu theo vai trò | Đăng nhập lần lượt 3 tài khoản, nhìn sidebar | — | `admin` 13 mục · `nhanvien01` 4 mục · `ketoan01` 6 mục. Cả ba đều thấy *Báo cáo* | | |
| 7 | Đăng xuất | Bấm **Đăng xuất** | — | Về form đăng nhập; bấm Back không vào lại được | | |

### 2.2. Khách hàng

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 8 | Danh sách và lọc | `/khach-hang`, chọn loại *Doanh nghiệp* | — | Chỉ hiện khách doanh nghiệp; bộ lọc giữ nguyên khi sang trang 2 | | |
| 9 | Thêm khách cá nhân | `/khach-hang/them`, điền đủ, Lưu | CCCD **12 số** hợp lệ | Lưu thành công, mã `KH` tự sinh, về danh sách kèm thông báo | | |
| 10 | Chặn CCCD sai | Như trên | CCCD **11 số** | Về lại form kèm thông báo "CCCD phải đủ 12 số", **không** lưu | | |
| 11 | Chặn MST sai | Loại *Doanh nghiệp* | MST **5 số** | Bị chặn kèm thông báo | | |
| 12 | Chặn giấy tờ trùng | Nhập số giấy tờ của khách đã có | — | Bị chặn, nêu rõ khách nào đang giữ | | |
| 13 | Chặn bỏ trống | Để trống tên / giấy tờ / địa chỉ | — | Cả ba ô đều báo lỗi cùng lúc | | |
| 14 | Chặn ngừng giao dịch | Mở khách còn thuê bao hoạt động → **Ngừng giao dịch** | — | Bị chặn, thông báo nêu **số thuê bao** đang chạy | | |

### 2.3. Thuê bao

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 15 | Lọc theo trạng thái | `/thue-bao`, lần lượt 4 trạng thái | — | Mỗi trạng thái một màu badge riêng; số lượng khớp bộ lọc | | |
| 16 | Chặn số trùng | Đăng ký số đã tồn tại | `0901234501` | Bị chặn | | |
| 17 | Chặn đầu số lạ | Đăng ký số đầu `02` | `0201234567` | Bị chặn | | |
| 18 | Chặn ngày tương lai | Ngày kích hoạt sau hôm nay | — | Bị chặn | | |
| 19 | Chặn gói sai loại | Thuê bao trả trước + gói trả sau | — | Bị chặn kèm giải thích | | |
| 20 | Bốn tab chi tiết | Mở thuê bao **trả trước** (id 4) | — | Đủ 4 tab; tab **Biến động số dư** có dòng `TRU_CUOC` 204.780 đ, số dư 205.000 → 220 đ | | |
| 21 | Tab theo loại | Mở thuê bao **trả sau** | — | **Không** có tab Biến động số dư | | |
| 22 | Ma trận trạng thái | Thuê bao *Đã thanh lý* → đổi sang *Hoạt động* | — | Bị chặn (trạng thái cuối) | | |
| 23 | Tạm ngừng rồi khôi phục | Thuê bao hoạt động → *Tạm ngừng 1 chiều* → *Hoạt động* | — | Hai dòng mới trong tab **Lịch sử biến động**, có lý do và người thực hiện | | |

### 2.4. Gói cước, bảng giá, CDR

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 24 | Bảng giá | `/bang-gia` | — | 10 dòng; 3 dòng giờ cao điểm chỉ áp cho THOẠI, đơn giá cao hơn 20% | | |
| 25 | Tra cứu đơn giá | `/bang-gia/tra-cuu`, chọn dịch vụ + hướng + giờ | — | Trả đúng một dòng giá đang hiệu lực | | |
| 26 | Sinh CDR có hạt giống | `/cdr/sinh-du-lieu`: 01/08–31/08/2026, 2000 bản ghi, hạt giống `12345` | — | Sinh xong, khối kết quả hiện **hạt giống đã dùng = 12345** | | |
| 27 | ⭐ Tái lập bằng hạt giống | Xoá CDR vừa sinh, sinh lại **đúng** tham số trên | Hạt giống `12345` | Ra **đúng cùng bộ dữ liệu** (so số bản ghi và phân bố dịch vụ) | | |
| 28 | Chặn tham số sinh CDR | Số lượng `-100`; rồi ngày kết thúc trước ngày bắt đầu | — | Cả hai đều bị chặn, không sinh bản ghi nào | | |
| 29 | Nhập CDR từ CSV | `/cdr/import`, tải file mẫu rồi nhập lại | `mau-cdr.csv` | 20 dòng: **17 thành công, 3 lỗi**, nêu rõ lý do từng dòng lỗi | | |

### 2.5. Tính cước và hóa đơn

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 30 | Tính cước một kỳ | `/tinh-cuoc` → **Tính cước** kỳ 8/2026 | Sau khi đã sinh CDR | Mọi bản ghi chuyển `DA_TINH`, 0 bản ghi lỗi | | |
| 31 | Lập hóa đơn | → **Lập hóa đơn** kỳ 8/2026 | — | Sinh hóa đơn cho thuê bao **trả sau**; trả trước **không** có hóa đơn | | |
| 32 | ⭐ Bảng đối soát | `/tinh-cuoc/doi-soat/21/1` | — | Khối 2 hiện `5.739,7 MB (5.877.492 KB)` — **hai con số trong ngoặc** chứng minh quy đổi KB→MB đúng; khối 4 chênh lệch **toàn 0 đ** | | |
| 33 | Đối soát sát ranh giới | `/tinh-cuoc/doi-soat/34/1` | — | `99,6 phút (5.978 giây)` cạnh quota 100 phút → **0 đ** | | |
| 34 | Tính xác định | Huỷ hóa đơn kỳ 6 rồi lập lại | — | Ra **đúng** 58 hóa đơn / 23.828.605 đ như trước | | |
| 35 | Chặn huỷ khi đã thu tiền | **Huỷ hóa đơn** kỳ 5 (đã có thanh toán) | — | Bị chặn, thông báo nêu **48 giao dịch** | | |
| 36 | Chặn chốt kỳ trống | **Chốt kỳ** kỳ 8 khi chưa có hóa đơn | — | Bị chặn kèm giải thích | | |
| 37 | Xuất hóa đơn PDF | Mở một hóa đơn → **Xuất PDF** | — | PDF mở được, **dấu tiếng Việt hiện đủ**, có tiền bằng chữ | | |

### 2.6. Thanh toán và công nợ

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 38 | Ghi nhận thanh toán | Đăng nhập `ketoan01`, mở hóa đơn còn nợ → **Ghi nhận thanh toán** | Thu đủ số còn nợ | Trạng thái → *Đã thanh toán*, `còn nợ = 0`, có dòng trong lịch sử thu | | |
| 39 | Thanh toán một phần | Thu **50%** số còn nợ | — | Trạng thái → *Thanh toán một phần*; `đã thu + còn nợ = tổng thanh toán` | | |
| 40 | Chặn thu vượt | Thu nhiều hơn số còn nợ | — | Bị chặn, nêu rõ số còn nợ | | |
| 41 | Chặn số tiền ≤ 0 | Thu `0` rồi `-100000` | — | Cả hai bị chặn | | |
| 42 | Chặn ngày tương lai | Ngày thanh toán năm 2027 | — | Bị chặn | | |
| 43 | Chặn thu hóa đơn đã đủ | Thu tiếp một hóa đơn *Đã thanh toán* | — | Bị chặn | | |
| 44 | In phiếu thu | `/thanh-toan` → một giao dịch → **In phiếu thu** | — | PDF có số tiền **bằng chữ**, tiêu đề *PHIẾU THU TIỀN* liền mạch | | |
| 45 | ⭐ Bảng tuổi nợ | `/cong-no` | — | **Cả 5 nhóm** có nội dung; tổng khớp dòng *Tổng còn nợ* — xem cảnh báo mốc thời gian ở mục 4 | | |
| 46 | Đề xuất tạm ngừng | `/cong-no`, khối cuối trang | — | Liệt kê thuê bao quá hạn > 15 ngày, kèm ghi chú **cảnh báo không chặn nghiệp vụ** | | |

### 2.7. Giảm trừ

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 47 | Danh sách giảm trừ | `/giam-tru` | — | 2 khoản *Đã áp dụng*: một khai **số tiền** 50.000 đ, một khai **tỷ lệ** 7,50% | | |
| 48 | Chặn khai cả hai | `/giam-tru/moi`, nhập cả số tiền lẫn tỷ lệ | — | Bị chặn kèm thông báo | | |
| 49 | Chặn không nhập gì | Để trống cả hai | — | Bị chặn | | |
| 50 | Chặn sửa khoản đã áp dụng | Sửa một khoản *Đã áp dụng* | — | Bị chặn, giải thích phải huỷ hóa đơn trước | | |

### 2.8. Dashboard và báo cáo

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 51 | Dashboard | `/` | — | 4 thẻ số liệu; biểu đồ doanh thu **5 cột**; 2 biểu đồ tròn; 2 bảng có nội dung | | |
| 52 | Đối chiếu số dashboard | So thẻ *Tổng công nợ* với dòng tổng ở `/cong-no` | — | **Hai màn hình cùng một số** | | |
| 53 | Doanh thu theo kỳ | `/bao-cao/doanh-thu-ky` | — | Bảng 6 kỳ + biểu đồ cột kèm đường tỷ lệ thu trên trục phải | | |
| 54 | Doanh thu theo gói | `/bao-cao/doanh-thu-goi-cuoc`, đổi kỳ | — | Tỷ trọng cộng lại **100%**; đổi kỳ thì bảng và biểu đồ đổi cùng lúc | | |
| 55 | Doanh thu theo dịch vụ | `/bao-cao/doanh-thu-dich-vu` kỳ 6 | — | Dòng *Giảm trừ* mang **dấu âm**; tổng trước thuế + VAT = tổng thanh toán | | |
| 56 | Thống kê thuê bao | `/bao-cao/thue-bao` | — | Các lát biểu đồ cộng lại đúng **80**; biểu đồ đường mới/rời mạng cùng một trục | | |
| 57 | Top thuê bao | `/bao-cao/top-thue-bao`, đổi 10 → 50 | — | Sắp giảm dần; đổi số lượng **giữ nguyên kỳ** đang chọn | | |
| 58 | Sản lượng dịch vụ | `/bao-cao/san-luong` kỳ 7 | — | Cột *Kỳ trước* là kỳ **6/2026**; biến động có dấu +/− và màu | | |
| 59 | Xuất Excel | Bấm **Xuất Excel** ở bất kỳ báo cáo nào | — | File mở được: header có nền, **freeze pane**, số có phân cách nghìn, dòng tổng đậm, chân trang ghi *dữ liệu mẫu* | | |
| 60 | In báo cáo | Bấm **In** → xem trước | — | Mất sidebar và nút; có tiêu đề riêng cho bản in | | |

### 2.9. Trường hợp biên và xử lý lỗi

| STT | Chức năng | Bước thực hiện | Dữ liệu | Kết quả mong đợi | Kết quả thực tế | Đạt |
|---|---|---|---|---|---|---|
| 61 | ⭐ Kỳ rỗng | Chọn kỳ **8/2026** ở cả 4 báo cáo có ô chọn kỳ | — | Hiện *"Không có dữ liệu"*, **không** lỗi 500 | | |
| 62 | Xuất Excel kỳ rỗng | Xuất Excel 4 báo cáo đó | — | File vẫn tải được, có dòng *Không có dữ liệu* | | |
| 63 | Trang âm | Gõ `/hoa-don?trang=-1` | — | Hiện trang đầu, **không** lỗi | | |
| 64 | Tham số sai kiểu | Gõ `/hoa-don/abc` | — | Trang **400** nêu rõ tham số nào sai — không phải 500 | | |
| 65 | Bản ghi không tồn tại | Gõ `/hoa-don/999999` | — | Thông báo tiếng Việt, không lỗi hệ thống | | |
| 66 | Chuỗi quá dài | Tên khách hàng 500 ký tự | — | Bị chặn kèm thông báo giới hạn | | |
| 67 | Ký tự đặc biệt | Tên khách hàng `<script>alert(1)</script>`, bỏ trống địa chỉ | — | Form về lại, thẻ script **hiện dạng chữ**, không chạy | | |
| 68 | Kỳ trùng | Tạo kỳ 6/2026 lần nữa | — | Bị chặn | | |
| 69 | Tháng ngoài khoảng | Tạo kỳ tháng `13` | — | Bị chặn | | |
| 70 | Màn hình hẹp | Thu cửa sổ dưới 992px | — | Sidebar ẩn, hiện nút mở; bấm mở/đóng được; trang **không cuộn ngang** | | |

---

## 3. Tổng hợp kiểm thử tự động

| Loại | Số lượng | Ghi chú |
|---|---|---|
| **Test tự động (JUnit)** | **275** | `mvnw test`, 0 lỗi |
| — chạy độc lập, không cần CSDL | 237 | Logic nghiệp vụ, quy đổi đơn vị, ma trận trạng thái, PDF, đọc số tiền, phân trang |
| — cần MySQL, chạy trên dữ liệu thật | 38 | 8 lớp, liệt kê dưới đây |

Tám lớp cần MySQL:

| Lớp | Số test | Kiểm gì |
|---|---|---|
| `SchemaValidationTest` | 1 | Entity khớp bảng thật |
| `KiemTraDoPhuBangGiaTest` | 3 | Bảng giá phủ mọi tổ hợp dịch vụ hợp lệ |
| `KiemTraSoCaiSoDuTest` | 2 | Bất biến sổ cái số dư |
| `KiemTraBatBienThanhToanTest` | 4 | Bất biến thanh toán |
| `CdrGeneratorHatGiongTest` | 3 | Hạt giống làm dữ liệu tái lập được |
| `BaoCaoServiceTest` | 11 | Kiểm chéo ba đường truy vấn báo cáo |
| `KiemTraDieuHuongTest` | 5 | Mọi liên kết trỏ tới endpoint có thật |
| `TinhCuocControllerTest` | 9 | Điều khiển engine tính cước |
| **Phép kiểm giao diện (PowerShell)** | **177** | 8 script, chạy khi ứng dụng đang bật |

Chi tiết 8 script:

| Script | Phép kiểm | Nội dung |
|---|---|---|
| `test-auth.ps1` | 11 | Đăng nhập, đăng xuất, phân quyền, sidebar theo vai trò |
| `test-kh.ps1` | 12 | Khách hàng: lọc, phân trang, validation, chặn nghiệp vụ |
| `test-tb.ps1` | 16 | Thuê bao: lọc, đăng ký, 4 tab, ma trận trạng thái |
| `test-muc-F.ps1` | 17 | Công nợ, tuổi nợ, chốt chặn huỷ hóa đơn kỳ đã thu |
| `test-bao-cao.ps1` | 38 | Dashboard, 7 báo cáo, 13 con số đối chiếu chéo, 11 file Excel |
| `test-dieu-huong.ps1` | 13 | Đi theo **menu**, không gõ URL cứng |
| `test-ky-rong.ps1` | 28 | Kỳ 8/2026 rỗng trên 17 màn hình + 4 Excel + 3 thao tác |
| `test-bien.ps1` | 42 | Trường hợp biên và 12 ca phân quyền 403 |

**Bất biến kiểm liên tục trên dữ liệu thật:**

| Bất biến | Phạm vi |
|---|---|
| `con_no = tong_thanh_toan − da_thanh_toan` | 280 hóa đơn, 0 lệch |
| `da_thanh_toan = SUM(thanh_toan.so_tien)` | 280 hóa đơn, 0 lệch |
| `so_du = SUM(nạp + điều chỉnh) − SUM(trừ)` | 80 thuê bao, 0 lệch |
| Mọi `th:href` trỏ tới endpoint có thật | 128 liên kết trong 41 template |
| Bảng giá phủ mọi tổ hợp dịch vụ hợp lệ | 10 dòng giá |

---

## 4. ⚠️ Ca kiểm thử phụ thuộc thời điểm

**STT 45 — bảng tuổi nợ đủ 5 nhóm — chỉ đúng tới hết 13/08/2026.**

Các dải tuổi nợ rộng đúng 30 ngày trong khi các kỳ cước cách nhau 30–31 ngày, nên cửa sổ mà cả
năm kỳ rơi vào năm nhóm khác nhau chỉ rộng **29 ngày**. Từ 14/08/2026 nhóm *Quá hạn 61–90* sẽ
rỗng vì kỳ 4/2026 chuyển sang nhóm *trên 90*.

Đây là **số học**, không phải lỗi. Người kiểm thử sau mốc đó nên ghi *"không áp dụng"* thay vì
*"không đạt"*. Chi tiết: `PHASE-6-REPORT.md` mục 1.2.
