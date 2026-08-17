# ĐÁNH GIÁ HỆ THỐNG — ĐƯỢC BAO NHIÊU PHẦN TRĂM?

> Đánh giá do đọc mã nguồn và đo trên hệ thống đang chạy, không phải kiểm toán độc lập.
> **Chưa** chạy kiểm thử tải, **chưa** kiểm thử xâm nhập, **chưa** kiểm khả năng tiếp cận
> bằng trình đọc màn hình. Ba việc đó nếu làm có thể kéo điểm xuống.

---

## 1. Vì sao không thể trả lời bằng một con số

Câu "được bao nhiêu % so với hệ thống hoàn thiện" chỉ có nghĩa khi nói rõ **hoàn thiện so với
cái gì**. Cùng một hệ thống này:

| Thang đo | Kết quả |
|---|---|
| **A.** So với **đồ án môn học** của sinh viên đại học | **≈ 93%** |
| **B.** So với **phần mềm nội bộ dùng thật** ở một doanh nghiệp nhỏ | **≈ 64%** |
| **C.** So với **hệ thống tính cước thật của nhà mạng** (BSS) | **≈ 12%** |

Ba con số này đều đúng cùng lúc. Chênh nhau vì thang C đo một thứ lớn hơn thang A khoảng
**bốn bậc độ lớn**: hệ thống này xử lý 18.723 bản ghi cước một kỳ, một nhà mạng thật xử lý
hàng trăm triệu.

**Con số 12% không phải lời chê.** Không đồ án sinh viên nào chạm tới hai chữ số có ý nghĩa ở
thang đó — vì phần lớn khối lượng của một BSS thật nằm ở tính cước thời gian thực, thu thập
cước từ tổng đài, đối soát liên mạng, chuyển vùng quốc tế và hóa đơn điện tử có chữ ký số,
tức những mảng cần nhiều năm-người chứ không phải nhiều tuần-người.

---

## 2. Quy mô đã đo

| Hạng mục | Số lượng |
|---|---:|
| Java chính | 156 file · 14.115 dòng |
| Java kiểm thử | 29 file · 6.310 dòng |
| Giao diện Thymeleaf | 43 file · 6.412 dòng |
| CSS + JS | 812 dòng |
| Tài liệu | 16 file · 8.461 dòng |
| Script kiểm thử | 10 file · 1.588 dòng |
| Bảng CSDL · chỉ mục | 15 bảng + 2 view · 60 chỉ mục |
| Thực thể · kho · dịch vụ · điều khiển | 15 · 15 · 24 · 15 |
| Điểm cuối HTTP | 55 GET · 25 POST |
| Kiểm thử tự động | **277 JUnit** + **185 phép kiểm giao diện** |

**Tỷ lệ mã kiểm thử / mã chính = 45%.** Đó là tỷ lệ của một dự án được chăm, không phải của
một bài tập.

---

## 3. Chấm từng mặt

Trọng số đặt theo mức quan trọng đối với **một hệ thống tính tiền** — tính đúng số tiền quan
trọng hơn giao diện đẹp.

| Mặt | Trọng số | A. Đồ án | B. Nội bộ DN nhỏ | C. BSS thật |
|---|---:|---:|---:|---:|
| Chức năng nghiệp vụ | 22% | 95 | 65 | 6 |
| **Tính đúng số tiền** | 18% | 95 | 70 | 18 |
| Kiểm thử | 12% | 100 | 80 | 20 |
| Dữ liệu & toàn vẹn | 11% | 92 | 55 | 8 |
| Bảo mật & tuân thủ | 11% | 85 | 40 | 8 |
| Kiến trúc & chất lượng mã | 10% | 95 | 75 | 15 |
| Giao diện & trải nghiệm | 8% | 97 | 75 | 12 |
| Vận hành | 5% | 70 | 25 | 3 |
| Tài liệu | 3% | 100 | 85 | 35 |
| **Tổng có trọng số** | 100% | **93** | **64** | **12** |

---

## 4. Ba điểm hệ thống làm tốt hơn mức thường thấy

### 4.1. Kỷ luật tính tiền

Tiền dùng `BigDecimal`, làm tròn `HALF_UP` scale 0, và **chỉ làm tròn ở đúng một tầng** —
tầng từng bản ghi cước. Ba chỗ quy đổi đơn vị gom hết vào `DonViCuoc`. Quy tắc "không cắt đôi
bản ghi khi vượt mức miễn phí" được ghi nhận là làm kết quả **phụ thuộc thứ tự**, nên mọi truy
vấn duyệt cước đều bắt buộc `ORDER BY thoi_gian_bat_dau, id`. Đơn giá được **chụp ảnh** vào
hóa đơn qua `bang_gia_cuoc_id`, nên sửa bảng giá không làm hóa đơn cũ đổi số.

Đây là loại kỷ luật mà phần lớn phần mềm kế toán nhỏ ngoài thị trường **không** có.

### 4.2. Bảng đối soát cước

Màn hình giải thích vì sao hóa đơn ra đúng con số đó, lần từ từng cuộc gọi tới dòng cuối hóa
đơn, và **đọc lại số đã ghi chứ không tính lại**. Cột chênh lệch phải toàn 0 đồng.

Nhiều phần mềm thương mại không có màn hình này. Nó là thứ đáng đem ra demo nhất.

### 4.3. Kỷ luật kiểm thử

277 test + 185 phép kiểm giao diện là nhiều. Nhưng thứ đáng giá hơn con số là **thói quen đối
chứng**: mỗi phép kiểm mới đều được chứng minh là *có kêu khi có lỗi* bằng cách cố ý làm hỏng
rồi xem nó có đỏ không. Dự án ghi lại **chín lần** gặp cùng một bài học — *một phép kiểm sai
nguy hiểm ngang thiếu phép kiểm*. Đó là mức độ nghiêm túc về chất lượng hiếm thấy ở quy mô này.

---

## 5. Bốn lỗ hổng phải vá trước khi dùng thật

Xếp theo mức nguy hiểm. Cả bốn đều tự xác minh trong mã, không suy đoán.

### 5.1. 🔴 Hai người cùng thu tiền một hóa đơn thì mất tiền

`ThanhToanServiceImpl.ghiNhan` chỉ có `@Transactional` trần và `findById`. **Không `@Version`,
không `LockModeType`, không `SELECT … FOR UPDATE`** ở bất kỳ đâu trong `repository/`.

Kịch bản: hóa đơn còn nợ 100.000 đ. Hai thu ngân cùng mở, mỗi người thu 50.000 đ.

1. Cả hai cùng đọc `con_no = 100.000`
2. Cả hai cùng qua được `kiemTra` (mỗi số đều ≤ 100.000)
3. Cả hai cùng ghi `da_thanh_toan = 0 + 50.000`

Kết quả: **2 dòng thanh toán tổng 100.000 đ, nhưng `da_thanh_toan` chỉ 50.000 đ.**

Đây là hỏng đúng **bất biến trung tâm** mà cả dự án được xây quanh nó
(`da_thanh_toan = SUM(thanh_toan.so_tien)`). `KiemTraBatBienThanhToanTest` không bao giờ bắt
được, vì nó chạy *sau*, trên dữ liệu đã đứng yên.

> **Bất biến của dự án hiện chỉ đúng khi có một người dùng tại một thời điểm** — điều chưa
> tài liệu nào nói ra.

**Cách vá:** thêm `@Version` vào `HoaDon`, hoặc `@Lock(PESSIMISTIC_WRITE)` cho truy vấn nạp
hóa đơn trong luồng thanh toán. Công sức: **nửa ngày**, kèm một test hai luồng chạy song song.

### 5.2. 🔴 Không có đường nâng cấp CSDL

Không có Flyway/Liquibase. `schema.sql` mở đầu bằng `DROP TABLE`. Nghĩa là **mọi thay đổi cấu
trúc bảng đều đi kèm xoá sạch dữ liệu**. Với dữ liệu mẫu thì tiện; với dữ liệu thật thì không
có cách nào thêm một cột mà không mất toàn bộ lịch sử cước.

**Cách vá:** đưa schema hiện tại thành `V1__khoi_tao.sql` của Flyway. Công sức: **1–2 ngày**.

### 5.3. 🔴 Hóa đơn chưa hợp pháp ở Việt Nam

Hóa đơn xuất ra là PDF do `OpenPDF` dựng. Từ 01/07/2022, hóa đơn bán hàng ở Việt Nam bắt buộc
là **hóa đơn điện tử có mã của cơ quan thuế**, có chữ ký số, phát hành qua nhà cung cấp được
cấp phép và truyền dữ liệu về Tổng cục Thuế (Nghị định 123/2020, Thông tư 78/2021).

Đây là rào cản **pháp lý**, không phải kỹ thuật — không vá được bằng cách viết thêm mã, mà
phải tích hợp với một nhà cung cấp hóa đơn điện tử. Công sức: **3–6 tuần** kể cả thủ tục.

Thuế suất VAT còn đang **chôn cứng** `THUE_SUAT_VAT = 0.10` trong `ThamSoTinhCuoc`; thuế suất
thay đổi thì phải sửa mã và biên dịch lại.

### 5.4. 🟠 Trần quy mô cứng

`RatingService:266` nạp **toàn bộ bản ghi cước của một kỳ vào một `List` trong bộ nhớ**;
`TruCuocTraTruocService:92` cũng vậy. Phía ghi đã chia lô, phía đọc thì chưa.

18.723 bản ghi thì không sao. Một triệu bản ghi bắt đầu nguy hiểm; vài chục triệu là hết bộ
nhớ. **Cách vá:** đổi sang con trỏ cuộn hoặc phân trang theo lô. Công sức: **2–3 ngày**.

---

## 6. Còn thiếu gì so với một hệ thống hoàn thiện

### 6.1. Bảo mật — thiếu nhiều thứ cơ bản

| Thiếu | Hậu quả |
|---|---|
| Khoá tài khoản sau N lần sai | Dò mật khẩu tự do, không giới hạn |
| Chính sách mật khẩu, đổi mật khẩu, quên mật khẩu | Ba tài khoản dùng chung mật khẩu `123456`, không ai đổi được |
| Màn hình quản lý người dùng | Thêm nhân viên mới phải chạy SQL tay |
| Giới hạn phiên, tự hết hạn phiên | Đăng nhập bỏ đó cả ngày vẫn dùng được |
| Bắt buộc HTTPS, security headers (HSTS/CSP) | Mật khẩu đi trên mạng dạng rõ |
| Bảo vệ dữ liệu cá nhân (CCCD, MST) | Lưu dạng rõ, không mã hoá, không ghi vết ai đã xem — Nghị định 13/2023 |

**Đã có và làm tốt:** BCrypt, phân quyền kiểm ở máy chủ (đã kiểm bằng 12 phép thử 403 thật),
CSRF bật mặc định, Thymeleaf tự thoát HTML (đã kiểm chống XSS), sổ nhật ký với 27 điểm ghi vết.

### 6.2. Chức năng nghiệp vụ còn thiếu so với BSS

| Thiếu | Vì sao quan trọng |
|---|---|
| **Tính cước thời gian thực (OCS)** cho trả trước | Hiện trả trước bị trừ cước **theo lô hằng tháng** — thực tế phải trừ ngay khi cuộc gọi kết thúc, nếu không khách gọi âm số dư |
| **Tầng thu thập cước (mediation)** | Cước hiện đến từ bộ tạo thử hoặc file CSV, không từ tổng đài |
| Đối soát liên mạng, chuyển vùng | Không tính được cước gọi sang mạng khác / ra nước ngoài đúng nghĩa |
| Quy trình nhắc nợ tự động (dunning) | Hiện chỉ *gợi ý* tạm ngừng, người dùng tự bấm |
| Quản lý kho số, hợp đồng | Số thuê bao nhập tay, không có vòng đời số |
| Cưỡng chế hạn mức tín dụng | Cột `hanMucTinDung` **có lưu nhưng không chỗ nào dùng để chặn** — cột chết |
| Thu tiền đa kênh, đối soát ngân hàng | Chỉ ghi nhận thủ công ba hình thức |
| Cổng tự phục vụ cho khách hàng | Khách không tra được cước của mình |

### 6.3. Vận hành — mặt yếu nhất

Không có Docker, không có CI, không có Actuator/health/metrics, không có quy trình sao lưu –
phục hồi, không có giám sát cảnh báo, không có API để tích hợp.

**Đã có:** ghi log ra file xoay vòng theo ngày giữ 30 ngày, mã sự cố tra cứu được, một việc
chạy theo lịch (quét quá hạn 00:05 hằng ngày), và `reset` tái lập được **20.102 dòng giống hệt
từng dòng** — khả năng tái lập dữ liệu này thì nhiều hệ thống thật cũng không có.

---

## 7. Muốn nâng lên mức nào thì làm gì

### Lên ~80% thang B (dùng thật được ở một doanh nghiệp nhỏ) — ước 4–6 tuần

1. Khoá lạc quan trên hóa đơn + test hai luồng — *nửa ngày*
2. Flyway — *1–2 ngày*
3. Màn hình quản lý người dùng + đổi/quên mật khẩu + khoá tài khoản — *1 tuần*
4. HTTPS + security headers + giới hạn phiên — *1–2 ngày*
5. Quy trình sao lưu, phục hồi và tài liệu vận hành — *2–3 ngày*
6. Docker + CI chạy `mvnw test` mỗi lần đẩy mã — *2–3 ngày*
7. Actuator + health check + giám sát tối thiểu — *2 ngày*
8. Tính cước theo lô cuộn thay vì nạp cả kỳ — *2–3 ngày*
9. VAT và tham số cước đưa vào bảng cấu hình — *2 ngày*

### Lên mức bán được ra thị trường — thêm 3–6 tháng

Hóa đơn điện tử có mã cơ quan thuế, REST API, cổng thanh toán, nhắc nợ tự động, cổng khách
hàng, nhiều đơn vị/chi nhánh, phân quyền chi tiết tới từng thao tác.

### Lên BSS thật — nhiều năm-người

Tính cước thời gian thực, tầng thu thập từ tổng đài, liên mạng, chuyển vùng, đối soát doanh
thu, khả dụng cao. Đây không còn là mở rộng, mà là **xây lại từ đầu với kiến trúc khác**.

---

## 8. Kết luận

Là **đồ án môn học**, hệ thống này ở khoảng **93%** — và phần trăm còn thiếu chủ yếu là hạ
tầng kỹ thuật (CI, Docker) chứ không phải nghiệp vụ. Ba thứ vượt hẳn mức thường thấy: kỷ luật
tính tiền, bảng đối soát, và thói quen đối chứng phép kiểm.

Là **phần mềm dùng thật**, khoảng **64%** — nghiệp vụ gần đủ, nhưng bốn lỗ hổng ở mục 5 phải
vá trước, mà ba trong bốn cái chỉ tốn vài ngày. Riêng hóa đơn điện tử là rào cản pháp lý.

So với **BSS thật của nhà mạng**, khoảng **12%** — và đó là con số bình thường, vì hai thứ
khác nhau về bản chất chứ không chỉ về quy mô.

**Điều đáng nói nhất không nằm ở con số nào ở trên.** Hệ thống này ghi lại được **chín lần**
nó tự bắt được rằng *phép kiểm của chính nó đang sai* — mỗi lần đều tìm ra bằng cách cố ý làm
hỏng rồi xem phép kiểm có kêu không. Đó là thói quen mà phần lớn kỹ sư đi làm vài năm vẫn chưa
có, và nó có giá trị lâu dài hơn bất cứ chức năng nào trong danh sách.
