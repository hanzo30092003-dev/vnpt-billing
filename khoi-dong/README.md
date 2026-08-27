# Mở phần mềm bằng một cú nháy đúp

Thư mục này chứa cách mở phần mềm **không cần gõ lệnh**. Dành cho người dùng ở quầy giao dịch,
không phải cho lập trình viên.

Cách của lập trình viên (`chay.cmd`, `mvnw spring-boot:run`) vẫn còn nguyên và không đổi gì —
xem mục 4 của `README.md` ở gốc dự án.

---

## Làm một lần: tạo biểu tượng trên màn hình nền

1. Mở thư mục `khoi-dong`.
2. Bấm chuột phải vào **`Tao-LoiTat.ps1`** → chọn **Run with PowerShell**.

Xong. Trên màn hình nền xuất hiện biểu tượng **Quản lý thuê bao & tính cước**:

<img src="bieu-tuong-xem-thu.png" alt="Biểu tượng hình cột phát sóng trên nền xanh mực" width="96">

Hình cột phát sóng, vẽ bằng mã lúc chạy — không tải gì từ mạng.

Chỉ làm một lần. Chép dự án sang máy khác hay thư mục khác thì chạy lại file đó — mọi đường dẫn
đều tự suy ra, không có đường dẫn nào bị ghi cứng.

---

## Hằng ngày: nháy đúp biểu tượng

Nháy đúp → đợi khoảng 10 giây → trình duyệt tự mở vào trang đăng nhập.

Một cửa sổ màu đen hiện ra và **ở lại**. Đó là phần mềm đang chạy.

| Muốn | Làm |
|---|---|
| Mở phần mềm | Nháy đúp biểu tượng |
| Quay lại trang khi lỡ đóng tab | Nháy đúp biểu tượng lần nữa (không mở thêm bản thứ hai) |
| **Tắt phần mềm** | **Đóng cửa sổ màu đen** |

Ba tài khoản demo và mật khẩu hiện ngay trong cửa sổ đen sau khi mở xong.

---

## Ba lỗi hay gặp

Mọi thông báo lỗi đều nói **chuyện gì xảy ra** và **bạn cần làm gì**. Không có dòng tiếng Anh
khó hiểu nào. Dưới đây là ba lỗi hay gặp nhất.

### 1. "Máy tính chưa lưu mật khẩu MySQL"

Mật khẩu cố ý **không** nằm trong bất kỳ file nào của dự án, nên mỗi máy phải khai một lần.

Mở PowerShell rồi gõ (thay bằng mật khẩu thật):

```bash
setx MYSQL_PASSWORD "mat_khau_MySQL_cua_ban"
```

Sau đó **phải đóng cửa sổ vừa gõ lại**. `setx` chỉ có hiệu lực với cửa sổ mở **mới** sau đó —
đây là chỗ hay nhầm nhất.

### 2. "Không bật được MySQL vì thiếu quyền quản trị"

Thông báo sẽ **gọi đúng tên dịch vụ trên máy bạn** (có máy là `MySQL84`, có máy là `MySQL80`).
Bấm phím Windows, gõ `services.msc`, tìm đúng tên đó, bấm chuột phải → **Start**.

Script cố ý **không tự nâng quyền quản trị** — một chương trình tự đòi quyền quản trị lúc khởi
động là thói quen xấu, và người dùng không có cách nào biết nó sẽ làm gì với quyền đó.

### 3. "Cổng 8080 đang bị một chương trình khác chiếm"

Một chương trình khác đang giữ chỗ mà phần mềm cần. Thông báo có nói tên chương trình đó. Đóng
nó lại, hoặc khởi động lại máy.

Lưu ý: nếu chỗ đó do **chính phần mềm này** giữ thì không phải lỗi — script nhận ra và chỉ mở
trình duyệt, không khởi động bản thứ hai.

---

## Gỡ đi

Xoá biểu tượng trên màn hình nền là xong. Không có gì được cài vào máy, không đụng vào registry,
không có dịch vụ chạy nền nào được tạo ra.

Muốn dọn sạch thì xoá thêm `khoi-dong/bieu-tuong.ico` và hai file `nhat-ky-khoi-dong*.log`.

---

## Có gì trong thư mục này

| File | Việc |
|---|---|
| `Chay-PhanMem.ps1` | Script chính: kiểm môi trường → chạy → chờ sẵn sàng → mở trình duyệt |
| `Tao-LoiTat.ps1` | Chạy một lần, tạo biểu tượng trên màn hình nền |
| `Chay-PhanMem.cmd` | Bản dự phòng — nháy đúp file `.ps1` thì Windows mở bằng Notepad chứ không chạy, file `.cmd` thì chạy được |
| `bieu-tuong.ico` | Biểu tượng, do `Tao-LoiTat.ps1` vẽ ra bằng mã, không tải từ mạng |
| `nhat-ky-khoi-dong*.log` | Nhật ký lần chạy gần nhất. Bị `.gitignore` bỏ qua |

---

## Ghi chú cho người bảo trì

**Vì sao không dùng lại `scripts/chay.ps1`.** Script đó viết cho lập trình viên: nó hỏi đáp
tương tác, đợi `Ctrl+C` để tắt, không mở trình duyệt, và **có chế độ `reset` xoá sạch CSDL**.
Gọi sang nó là mở một đường mà một tham số lạc tay có thể đi tới chỗ mất dữ liệu.
`Chay-PhanMem.ps1` đứng độc lập, và trong toàn bộ thư mục này **không có đường nào tới `reset`**.

**Chạy từ bản đóng gói `.jar`, không qua Maven.** Nhanh hơn và quan trọng hơn là chỉ có **một**
tiến trình để quản lý. Nếu bản đóng gói thiếu hoặc cũ hơn `src/` (hoặc `pom.xml`) thì script tự
đóng gói lại trước — người dùng không bao giờ demo nhầm bản cũ.

**File `.ps1` có tiếng Việt phải lưu kèm BOM UTF-8**, và script phải tự đặt
`[Console]::OutputEncoding`. BOM sửa đường **đọc** file, `OutputEncoding` sửa đường **ghi** ra
màn hình — thiếu cái thứ hai thì chữ vẫn vỡ dù đã có BOM.

Lý do đằng sau từng quyết định, và các lỗi đo được trong lúc dựng: `docs/G7-REPORT.md`.
