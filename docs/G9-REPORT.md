# G9 — Lối tắt Desktop báo sai mật khẩu MySQL: chẩn đoán và sửa gốc

**Ngày:** 17/09/2026 · **Phạm vi sửa:** chỉ `khoi-dong/` · **Không** đụng `src/`, `scripts/`,
`db/`, `pom.xml`, `application.yml`. Không chạy `reset`, không nâng quyền, không đổi/đọc mật khẩu.

## 1. Triệu chứng

Nháy đúp lối tắt → cửa sổ chạy tới Bước 5 rồi hiện *"KHÔNG MỞ ĐƯỢC PHẦN MỀM — Mật khẩu MySQL lưu
trên máy không đúng"*. Nhưng **Bước 3 vẫn in dấu tích xanh** `[v] Đã có mật khẩu kết nối` trước đó.

Log ứng dụng (`nhat-ky-khoi-dong.log`): Flyway không lấy được kết nối →
`Access denied for user 'root'@'localhost' (using password: YES)` · **mã 1045**.

## 2. Nguyên nhân thật — kèm bằng chứng cho cả bốn khả năng

Dùng một script chẩn đoán chỉ-đọc, thử kết nối thật bằng `mysql.exe` (mật khẩu đi qua `MYSQL_PWD`
của tiến trình con, **không** in ra, **không** ghi file):

| Khả năng | Bằng chứng | Kết luận |
|---|---|---|
| 1. Biến chưa tồn tại (chỉ tạm 1 phiên) | Biến có ở **Process** (dài 19) **và** User registry (dài 19); Process == User; bền qua reboot | ❌ Loại |
| **2. Biến tồn tại nhưng giá trị SAI** | Mọi giá trị đang lưu → **1045**, giống hệt một mật khẩu bịa; khác "không mật khẩu" (`using password: NO`) | ✅ **Đúng** |
| 3. `setx` chưa nạp lại | Registry ghi lần cuối 09:20:56; cửa sổ launcher mở 17:08 (sau đó) nên **thấy** biến; log Java ghi `using password: YES` ⇒ biến đã tới tiến trình con | ❌ Loại |
| 4. Mật khẩu đúng nhưng root khoá/hết hạn | Khoá→3118, hết hạn→1820/1862. Ở đây đúng **1045** = sai xác thực; mật khẩu bịa cũng cho cùng 1045; không credential nào đọc được `mysql.user` | ❌ Loại |

**Kết luận:** biến `MYSQL_PASSWORD` tồn tại bền và được giao đúng cho tiến trình con java, **nhưng
giá trị không khớp mật khẩu `root` hiện tại của MySQL** → 1045. (Ghi chú: tồn tại **hai** giá trị
19 ký tự *khác nhau* ở các phạm vi khác nhau, **cả hai** đều bị 1045. Ở phiên trước lần reboot
09:22 hôm nay ứng dụng từng chạy được — tức mật khẩu từng khớp rồi lệch. Việc đối chiếu lại mật
khẩu là của người dùng; script này chỉ phát hiện sớm và chỉ đường, không tự đặt.)

## 3. Bước nào trước đây chỉ kiểm hình thức

Rà cả năm bước — *nó kiểm cái gì*, và *cái đó có chứng minh được điều nó tuyên bố không*:

| Bước | Kiểm gì | Chứng minh được điều nó tuyên bố? |
|---|---|---|
| 1. Cổng 8080 | Cổng mở **và** có trả lời HTTP (mọi mã) | ✅ Thực chất |
| 2. MySQL | Cổng 3306 mở, hoặc bật dịch vụ rồi **đợi 3306 nhận kết nối** | ✅ Thực chất (cổng mở ≠ xác thực được — đó là việc Bước 3) |
| **3. Mật khẩu** | **CŨ: chỉ kiểm biến có tồn tại** → in tích xanh dù giá trị sai | ❌ **Hình thức** — đã sửa |
| 4. Đóng gói | jar tồn tại + mới hơn `src`; chạy `mvnw` rồi kiểm **mã thoát** + file kết quả | ✅ Thực chất |
| 5. Chạy & chờ | Đợi HTTP 200 **hoặc** tiến trình thoát, tối đa 120s; đọc log bắt lỗi | ✅ Thực chất |

Chỉ **Bước 3** báo an toàn trong khi thứ nó canh (kết nối CSDL) đang hỏng — đúng khuôn mẫu đã gặp
nhiều lần trong dự án (`grep -c | wc -l` luôn ra 1; `th:if` cùng thẻ `th:replace` luôn hiện).

## 4. Cách sửa

**`khoi-dong/Chay-PhanMem.ps1` — Bước 3 nay THỬ KẾT NỐI THẬT** (`Test-KetNoiMySQL`):

- Mở một kết nối thật bằng `mysql.exe` với đúng `MYSQL_USER` (mặc định `root`) + `MYSQL_PASSWORD`,
  `-h 127.0.0.1` giống JDBC `localhost:3306` của ứng dụng, câu `SELECT 1` (chỉ kiểm **xác thực**,
  không đụng CSDL nào). Mật khẩu qua `MYSQL_PWD` của tiến trình con — **không** lên dòng lệnh,
  **không** in, **không** ghi file.
- **Chọn `mysql.exe`, không mở TCP đọc gói chào:** đọc gói chào chỉ chứng minh MySQL đang lắng
  nghe (Bước 2 đã biết), **không** kiểm được mật khẩu. `mysql.exe` là client chính chủ, xử lý
  đúng `caching_sha2_password` như Connector/J, nên kết quả 1045 của nó báo trước đúng kết quả
  của ứng dụng. Tìm `mysql.exe` cạnh `mysqld.exe` của dịch vụ đang chạy (đúng phiên bản), rồi mới
  đến PATH và thư mục cài quen thuộc.
- **Sai thì dừng NGAY ở Bước 3**, không sang Bước 4/5. Ba (thực ra bốn) thông báo phân biệt:

| Trường hợp | Thông báo → hướng dẫn |
|---|---|
| Chưa đặt biến bao giờ | *"Máy tính chưa lưu mật khẩu MySQL"* → `setx` + **nhấn mạnh setx chỉ vào cửa sổ mở MỚI** |
| Đã đặt nhưng cửa sổ mở trước đó (registry có, phiên trống) | *"Máy đã lưu rồi, nhưng cửa sổ này mở trước lúc đó"* → chỉ cần đóng và bấm lại biểu tượng |
| Đã đặt nhưng **sai** (1045) | *"Mật khẩu MySQL lưu trên máy không đúng (lỗi 1045)"* → `setx` lại + gợi ý chạy `Kiem-Tra-Moi-Truong.ps1` |
| MySQL từ chối lý do khác | *"…vì một lý do khác (mã N)"* → nêu mã cho người phụ trách kỹ thuật |
| Không tìm thấy `mysql.exe` | Cảnh báo, **không chặn** (mật khẩu có thể vẫn đúng) — Bước 5 vẫn bắt được nếu sai |

**Giảm lỗi lặp lại:**
- Mọi hướng dẫn đặt mật khẩu đều nói rõ **`setx` chỉ có hiệu lực với cửa sổ mở MỚI** — chỗ hay vấp nhất.
- Sau khi người dùng đặt lại: chỉ cần **đóng và bấm lại biểu tượng** (tiến trình mới đọc biến mới).
  Thông báo nói rõ điều này; muốn dò lại mà không mở ứng dụng thì chạy `Kiem-Tra-Moi-Truong.ps1`.
- **`khoi-dong/Kiem-Tra-Moi-Truong.ps1`** (mới) — chẩn đoán chỉ-đọc, tách khỏi luồng khởi động:
  đo cổng 8080/3306, dịch vụ MySQL, biến ở từng phạm vi (chỉ báo có/không + độ dài), và thử kết
  nối thật. Không khởi động ứng dụng, không thay đổi gì.

Cơ chế Job Object của G7 giữ nguyên; cả hai file lưu kèm **BOM UTF-8**; không có đường nào tới `reset`.

## 5. Bốn tình huống đã thử — thông báo người dùng thấy

Chạy launcher thật trong tiến trình con, điều khiển `MYSQL_PASSWORD`; phần cần admin/mật khẩu đúng
thì **mô phỏng bằng bản sao chỉ đổi một–hai dòng, in `diff` đối chiếu** (như G7).

| # | Tình huống | Cách thử | Kết quả người dùng thấy |
|---|---|---|---|
| 1 | Biến trống, chưa từng lưu | Mô phỏng (ép registry rỗng), diff | *"Máy tính chưa lưu mật khẩu MySQL"* → `setx`; **dừng ở Bước 3**, ExitCode 1 |
| 1b | Biến trống nhưng máy đã lưu | **Thật** (registry hiện có) | *"Máy đã lưu rồi, nhưng cửa sổ này mở trước lúc đó"* → đóng và bấm lại; ExitCode 1 |
| 2 | Biến có nhưng **sai** | **Thật** (`mysql.exe` thật → 1045) | *"Mật khẩu MySQL lưu trên máy không đúng (lỗi 1045)"*; **dừng ở Bước 3**, ExitCode 1 |
| 3 | Biến **đúng** → chạy tiếp | Mô phỏng (`mysql.exe` giả khớp mật khẩu), diff | `[v] Kết nối kho dữ liệu thành công.` → sang Bước 4; ExitCode 0 |
| 4 | MySQL không chạy | Mô phỏng (3306 đóng, không có dịch vụ), diff | Dừng ở **Bước 2**: *"không có MySQL đang chạy…"*; ExitCode 1 |

**Đối chứng âm hai chiều cho phép kiểm mật khẩu** (`Test-KetNoiMySQL`, nạp đúng hàm thật từ file):
mật khẩu **đúng → im (OK)**, **sai → kêu (SAI_MK/1045)**, rỗng → kêu, lỗi khác → KHAC (mã 2003),
thiếu `mysql.exe` → KHÔNG_KIỂM_ĐƯỢC. Đủ cả hai chiều — một phép kiểm luôn im vô dụng ngang luôn kêu.

## 6. Cố ý KHÔNG làm

- **Không đặt/đổi/đọc mật khẩu MySQL**, không hỏi mật khẩu — chỉ in hướng dẫn để người dùng tự làm.
- **Không nâng quyền quản trị** (bật dịch vụ, sửa datadir…) — hướng dẫn người dùng qua `services.msc`.
- **Không chạy `reset`, `scripts/`, không tính cước/lập/huỷ hoá đơn.**
- **Không đọc lại được số liệu để đối chứng** vì chính lỗi 1045 đang chặn mọi kết nối tới CSDL.
  Nhưng lượt này **không có thao tác ghi nào** (chỉ sửa file `khoi-dong/` + vài lần dò kết nối
  chỉ-đọc, tất cả dừng ở tầng xác thực nên không câu lệnh nào chạm bảng). Dữ liệu do đó không thể
  suy suyển; sẽ đối chứng lại được (7 kỳ · 23.223 CDR · 338 hóa đơn · 161 thanh toán · kỳ 9 rỗng
  `MO`) ngay khi mật khẩu được đặt đúng — đó cũng là việc người dùng cần làm để mở lại phần mềm.
