# HƯỚNG DẪN SỬ DỤNG

**Phần mềm quản lý thuê bao và tính cước điện thoại**

> ⚠️ Toàn bộ dữ liệu trong hệ thống là **dữ liệu mẫu tự sinh** phục vụ mục đích học tập.
> Không phải dữ liệu thật của bất kỳ nhà mạng nào.

---

## 1. Đăng nhập và phân quyền

Mở <http://localhost:8080>. Ba tài khoản, mật khẩu đều là `123456`:

| Tài khoản | Vai trò | Làm được gì |
|---|---|---|
| `admin` | Quản trị | Toàn bộ hệ thống |
| `nhanvien01` | Nhân viên | Khách hàng, thuê bao, báo cáo |
| `ketoan01` | Kế toán | Hóa đơn, thanh toán, công nợ, giảm trừ, báo cáo |

Menu bên trái **chỉ hiện những mục vai trò đó được dùng**. Gõ thẳng đường dẫn của chức năng
không có quyền sẽ ra trang 403 — phân quyền kiểm ở máy chủ, không chỉ ẩn menu.

### Menu bên trái gom theo công việc

Năm nhóm, xếp theo thứ tự bạn hay dùng trong ngày:

| Nhóm | Gồm | Khi nào dùng |
|---|---|---|
| **Khách hàng & thuê bao** | Khách hàng · Thuê bao | Hằng ngày, khi có khách đến quầy |
| **Thu tiền & công nợ** | Hóa đơn · Thanh toán · Công nợ · Giảm trừ | Hằng ngày, khi khách trả tiền |
| **Tính tiền hằng tháng** | Tháng tính tiền · Cuộc gọi & tin nhắn · Chạy tính tiền | Một lần vào đầu mỗi tháng |
| **Báo cáo** | Báo cáo thống kê | Khi cần xem số liệu |
| **Danh mục** | Gói cước · Bảng giá | Hiếm — khai một lần rồi để yên |

### Trang chủ có sẵn lối tắt

Ngay dưới các thẻ số liệu là khối **Việc thường làm** với 6 nút lớn: *Thêm khách hàng*,
*Đăng ký thuê bao*, *Tra cứu hóa đơn*, *Ghi nhận thanh toán*, *Tính tiền hằng tháng*,
*Xem báo cáo*. Chưa quen menu thì cứ bắt đầu từ đó.

### Mỗi màn hình tự giải thích

Ngay dưới tiêu đề của **mọi** màn hình đều có một câu nói màn hình đó dùng để làm gì.
Bảng nào trống cũng nói rõ **vì sao** trống và **bấm đi đâu** tiếp.

---

## 2. Trình tự nghiệp vụ

Các bước phải làm **đúng thứ tự** này; mỗi bước là đầu vào của bước sau:

```
Khách hàng → Thuê bao → (gói cước, bảng giá đã có sẵn)
   → Tạo tháng tính tiền → Tạo/nhập dữ liệu sử dụng → Tính tiền từng cuộc → Lập hóa đơn
   → Ghi nhận thanh toán → Theo dõi công nợ → Xem báo cáo
```

---

## 3. Quản lý khách hàng

### Thêm khách hàng mới
1. **Khách hàng** → **Thêm khách hàng**
2. Chọn **loại khách** trước — form tự đổi theo lựa chọn:
   - *Cá nhân*: cần **CCCD 12 số** và ngày sinh
   - *Doanh nghiệp*: cần **MST 10 số** và người đại diện
3. Điền tên, địa chỉ, ngày đăng ký → **Lưu**

Mã khách hàng (`KH000051`…) do hệ thống tự sinh, không nhập tay.

### Những trường hợp bị chặn
| Tình huống | Hệ thống làm gì |
|---|---|
| CCCD không đủ 12 số / MST không đủ 10 số | Báo lỗi ngay dưới ô, giữ nguyên dữ liệu đã nhập |
| Số giấy tờ trùng khách khác | Báo lỗi và **nêu tên khách đang giữ** số đó |
| Ngừng giao dịch khi còn thuê bao hoạt động | Báo lỗi và **liệt kê số thuê bao** đang chạy |

---

## 4. Quản lý thuê bao

### Đăng ký thuê bao
Vào chi tiết khách hàng → **Đăng ký thuê bao mới**. Chọn:
- **Loại**: trả trước hoặc trả sau
- **Gói cước**: chỉ chọn được gói **cùng loại** với thuê bao

### Bốn tab trên màn hình chi tiết
| Tab | Nội dung |
|---|---|
| Thông tin chung | Số thuê bao, khách hàng, gói, số dư / hạn mức |
| Lịch sử gói cước | Các lần đổi gói |
| Lịch sử biến động | Mỗi lần đổi trạng thái: từ đâu → đến đâu, lý do, ai làm |
| **Biến động số dư** | *Chỉ thuê bao trả trước.* Sổ cái nạp tiền và trừ cước, có số dư trước/sau từng dòng |

### Chuyển trạng thái
Bốn trạng thái: *Hoạt động* · *Tạm ngừng 1 chiều* · *Tạm ngừng 2 chiều* · *Đã thanh lý*.

> **Đã thanh lý là trạng thái cuối** — không khôi phục được. Số thuê bao đã thu hồi và có thể
> cấp cho khách khác; cho khôi phục thì hai khách hàng sẽ dùng chung một lịch sử cước.

---

## 5. Tháng tính tiền và tính cước

Toàn bộ điều khiển ở màn hình **Chạy tính tiền** (nhóm *Tính tiền hằng tháng*).

### Bước 1 — Tạo tháng tính tiền
**Tháng tính tiền** → nhập tháng và năm. Ngày đầu/cuối tháng hệ thống tự tính.

### Bước 2 — Đưa dữ liệu sử dụng vào
Hai cách:

**Tạo dữ liệu thử** — **Cuộc gọi & tin nhắn** → **Tạo dữ liệu thử**:
- Chọn khoảng ngày và số lượng bản ghi
- **Mã tạo lại** *(nên nhập)*: ghi lại con số này thì sau có thể **dựng lại đúng bộ dữ liệu đó**.
  Để trống cũng được — hệ thống tự chọn và hiện con số đã dùng ở khối kết quả.

**Nhập từ file** — **Cuộc gọi & tin nhắn** → **Nhập từ file**. Tải file mẫu để biết định dạng
cột. Hệ thống báo rõ từng dòng lỗi và lý do, các dòng hợp lệ vẫn được nhập.

### Bước 3 — Tính tiền từng cuộc
**Chạy tính tiền** → nút **Chạy tính cước** ở dòng tháng tương ứng. Hệ thống tính tiền cho
**từng cuộc gọi, tin nhắn, lần truy cập mạng**: tra
bảng giá theo dịch vụ + hướng + giờ cao điểm, tính số đơn vị, nhân đơn giá.

### Bước 4 — Lập hóa đơn
Nút **Lập hóa đơn**. Hệ thống áp ưu đãi gói cước, trừ giảm trừ, cộng VAT 10%.

> Thuê bao **trả trước không có hóa đơn** — cước của họ trừ thẳng vào số dư ở bước riêng
> (**Trừ cước trả trước**).

### Bước 5 — Chốt kỳ
Nút **Chốt kỳ**. ⚠️ **Thao tác một chiều, không có đường quay lại.** Sau khi chốt, kỳ đó không
tính lại, không lập lại, không huỷ được.

### Sửa sai khi chạy nhầm
| Muốn làm gì | Nút | Điều kiện |
|---|---|---|
| Bỏ hóa đơn để lập lại | **Huỷ hóa đơn** | Kỳ **chưa chốt** và **chưa có thanh toán** nào |
| Bỏ kết quả định giá | **Huỷ tính cước** | Kỳ chưa có hóa đơn và chưa trừ cước |
| Bỏ kết quả trừ cước | **Huỷ trừ cước** | Chưa trừ cước cho kỳ muộn hơn |

---

## 6. ⭐ Bảng đối soát cước

Đây là màn hình giải thích **vì sao hóa đơn ra con số đó**. Mở từ chi tiết thuê bao hoặc từ
hóa đơn → **Xem đối soát cước**.

Bốn khối, đọc từ trên xuống:

| Khối | Trả lời câu hỏi |
|---|---|
| 1. Thuê bao và gói cước | Gói nào, mức miễn phí bao nhiêu, dùng trọn tháng hay tính theo ngày |
| 2. Sản lượng và ưu đãi | Đã dùng bao nhiêu, được miễn phí bao nhiêu, vượt bao nhiêu |
| 3. Chi tiết từng bản ghi | Cuộc nào tính tiền, cuộc nào miễn phí, cuộc nào làm vượt mức miễn phí |
| 4. Đối chiếu với hóa đơn | Số cộng từ chi tiết sử dụng có khớp số trên hóa đơn không |

**Chỗ đáng chú ý nhất ở khối 2:** sản lượng hiện **hai đơn vị**, ví dụ `5.739,7 MB (5.877.492 KB)`
hay `99,6 phút (5.978 giây)`. Hệ thống lưu bằng KB và giây, còn mức miễn phí của gói khai bằng MB và phút —
hiển thị cả hai để người đọc **tự kiểm** phép quy đổi.

**Khối 4** là phần đáng tin nhất: cột chênh lệch phải **toàn 0 đ**. Nếu khác 0 thì có sai sót
thật ở phần tính tiền.

---

## 7. Hóa đơn và thanh toán

### Xem hóa đơn
**Hóa đơn** → lọc theo kỳ, trạng thái, khách hàng, khoảng tiền. Bốn trạng thái:

| Trạng thái | Nghĩa |
|---|---|
| Chưa thanh toán | Chưa thu đồng nào, còn trong hạn |
| Thanh toán một phần | Đã thu được một phần |
| Đã thanh toán | Còn nợ bằng 0 |
| Quá hạn | Quá hạn thanh toán **và chưa thu đồng nào** |

> Hóa đơn đã thu được một phần rồi quá hạn vẫn giữ trạng thái *Thanh toán một phần*. Thông tin
> quá hạn nằm ở cột **số ngày quá hạn** và **nhóm tuổi nợ** trên màn hình công nợ.

### Ghi nhận thanh toán
Mở hóa đơn → **Ghi nhận thanh toán**. Số tiền mặc định là trọn số còn nợ; sửa được nếu khách
trả một phần. Chọn hình thức và ngày thu → **Lưu**.

Bị chặn khi: số tiền **vượt số còn nợ**, số tiền ≤ 0, **ngày ở tương lai**, hoặc hóa đơn đã thu đủ.

### Xuất file
| Việc | Ở đâu |
|---|---|
| Hóa đơn PDF | Chi tiết hóa đơn → **Xuất PDF** |
| Phiếu thu PDF | Danh sách thanh toán → **In phiếu thu** |
| Danh sách ra Excel | Nút **Xuất Excel** trên mọi màn hình danh sách |

---

## 8. Công nợ

**Công nợ** cho ba khối:

1. **Bảng tuổi nợ** — chia theo số ngày quá hạn: *Trong hạn* · *1–30* · *31–60* · *61–90* ·
   *trên 90 ngày*. Kèm biểu đồ.
2. **Danh sách hóa đơn còn nợ** — sắp theo số ngày quá hạn giảm dần, nợ lâu nhất lên đầu.
3. **Đề xuất tạm ngừng** — thuê bao có hóa đơn quá hạn trên 15 ngày.

> Đây là **đề xuất**, không phải hành động tự động. Hệ thống liệt kê ra, người dùng quyết định
> và bấm nút. Cắt dịch vụ của khách là việc có hậu quả thật, và dữ liệu công nợ có thể chưa cập
> nhật — khách vừa nộp tiền ở quầy khác.

---

## 9. Giảm trừ

**Giảm trừ** → **Thêm giảm trừ**. Khai **một trong hai**:
- **Số tiền tuyệt đối**, ví dụ 50.000 đ
- **Tỷ lệ phần trăm**, ví dụ 7,5%

> Không khai cả hai cùng lúc, cũng không để trống cả hai — hệ thống chặn.

Tỷ lệ được quy thành số tiền **đúng một lần** lúc lập hóa đơn, rồi ghi cứng vào hóa đơn. Từ đó
về sau chỉ cộng trừ, không nhân lại.

Khoản đã **Đã áp dụng** không sửa hay xoá được, vì số tiền của nó đã nằm trong hóa đơn. Muốn
đổi thì huỷ hóa đơn của kỳ — khoản sẽ tự quay về *Chưa áp dụng*.

---

## 10. Báo cáo

**Báo cáo thống kê** → 7 báo cáo:

| Báo cáo | Trả lời |
|---|---|
| Doanh thu theo kỳ | Kỳ nào thu được bao nhiêu, tỷ lệ thu ra sao |
| Doanh thu theo gói cước | Gói nào đóng góp nhiều nhất |
| Doanh thu theo loại dịch vụ | Cơ cấu cước thuê bao / thoại / SMS / dữ liệu |
| Thống kê thuê bao | Theo trạng thái, theo loại, mới và rời mạng theo tháng |
| Top thuê bao cước cao | Ai dùng nhiều nhất trong kỳ |
| Sản lượng dịch vụ | Phút gọi, tin nhắn, dung lượng, so với kỳ trước |
| Công nợ tổng hợp | Bảng tuổi nợ + top khách nợ nhiều nhất |

Mỗi báo cáo có nút **Xuất Excel** và **In**. Bản in tự bỏ menu và các nút.

---

## 11. Xử lý sự cố thường gặp

| Hiện tượng | Nguyên nhân và cách xử lý |
|---|---|
| Không đăng nhập được | Mật khẩu mặc định là `123456`. Kiểm MySQL đã chạy chưa |
| Báo cáo trống trơn | Kỳ đang chọn chưa có hóa đơn. Đổi sang kỳ khác ở ô chọn kỳ |
| Không bấm được **Lập hóa đơn** | Kỳ chưa chạy tính cước, hoặc kỳ đã chốt |
| Không huỷ được hóa đơn | Kỳ đã có giao dịch thanh toán, hoặc kỳ đã chốt |
| Trang báo **400** | Địa chỉ có tham số sai định dạng — thường do sửa tay trên thanh địa chỉ |
| Bảng trống mà không rõ vì sao | Đọc câu giải thích ngay trong bảng — nó nói rõ vì sao trống và bấm đi đâu tiếp |
| Không biết màn hình này để làm gì | Đọc dòng chữ xám ngay dưới tiêu đề màn hình |
| Trang báo **500** kèm mã sự cố | Đọc mã đó cho người quản trị; chi tiết nằm trong `logs/vnpt-billing.log` |
| PDF hiện ô vuông thay vì chữ | Không xảy ra với bản này — font đã nhúng sẵn |
| Muốn về lại dữ liệu mẫu ban đầu | Chạy `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"`. ⚠️ **Xoá sạch** dữ liệu đang có |
