# ĐÁNH GIÁ HỆ THỐNG — ĐƯỢC BAO NHIÊU PHẦN TRĂM?

> Đánh giá do đọc mã nguồn và đo trên hệ thống đang chạy, không phải kiểm toán độc lập.
> **Chưa** chạy kiểm thử tải, **chưa** kiểm thử xâm nhập. Hai việc đó nếu làm có thể kéo điểm
> xuống.
>
> **Cập nhật sau việc V6:** khả năng tiếp cận **đã kiểm một phần** — đi bằng bàn phím thật trên
> trình duyệt, ghi lại thứ tự tiêu điểm. Bốn vấn đề tìm được đã sửa (xem
> [`KE-HOACH-HOAN-THIEN.md`](KE-HOACH-HOAN-THIEN.md) mục 7bis): số lần bấm Tab để tới ô nhập
> đầu tiên **20 → 5**, hộp thoại trả tiêu điểm về đúng nút vừa bấm, và **0/213** nút còn thiếu
> tên đọc được. **Vẫn chưa** kiểm bằng trình đọc màn hình thật (NVDA/JAWS) — đó là việc khác
> với đi bằng bàn phím, và chưa làm.

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

> ### ✅ ĐÃ SỬA và ĐÃ DỰNG LẠI ĐƯỢC — mục 5.1
>
> Bản đầu của báo cáo này khẳng định lỗi mất tiền từ **đọc mã**, chưa có quan sát. Việc dựng
> lại nó đi qua hai bước, và bước đầu **thất bại**:
>
> | Cách dựng lại | Kết quả khi đã gỡ `@Version` |
> |---|---|
> | 12 luồng cùng gọi `ghiNhan`, thả cùng lúc | ❌ Bất biến **vẫn đúng** — không dựng lại được |
> | **Ép đọc–đọc–ghi–ghi** bằng hai giao dịch điều khiển tay | ✅ **2/2 bên thành công nhưng tiền chỉ tăng 1.000 thay vì 2.000** |
>
> **Lỗi là có thật.** Chỉ là cách đầu không đủ sức dựng nó: thả 12 luồng cùng lúc không bảo đảm
> hai bên cùng *đọc xong* trước khi bên nào *kịp ghi* — mà đó mới là điều kiện duy nhất làm mất
> bản ghi. Ép thứ tự bằng hai chốt chặn thì nó xảy ra ngay và ổn định.
>
> Bài học: **một phép kiểm không đỏ chưa chứng minh được là không có lỗi** — nó có thể chỉ
> đang không chạm tới điều kiện gây lỗi. Suýt nữa tôi rút lại một khẳng định đúng.
>
> **Đã sửa:** `HoaDon.phienBan` (`@Version`) + nhánh xử lý
> `ObjectOptimisticLockingFailureException`, và `KiemTraDongThoiThanhToanTest` với hai phép
> kiểm, phép thứ hai **đã được chứng minh là biết đỏ**.

### 5.1. ✅ Hai người cùng thu tiền một hóa đơn thì mất tiền — *đã dựng lại được và đã sửa*

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

> **Bất biến của dự án chưa từng được kiểm dưới tranh chấp** — điều chưa tài liệu nào nói ra.
> Nay `KiemTraDongThoiThanhToanTest` kiểm nó ở cả hai mức: 12 luồng đua tự nhiên, và một phép
> ép đúng thứ tự đọc–đọc–ghi–ghi đã được chứng minh là biết đỏ khi gỡ khoá ra.

**Cách vá:** thêm `@Version` vào `HoaDon`, hoặc `@Lock(PESSIMISTIC_WRITE)` cho truy vấn nạp
hóa đơn trong luồng thanh toán. Công sức: **nửa ngày**, kèm một test hai luồng chạy song song.

### 5.2. ✅ ~~Không có đường nâng cấp CSDL~~ — ĐÃ SỬA (việc V4)

> **Đánh giá gốc:** *"Không có Flyway/Liquibase. `schema.sql` mở đầu bằng `DROP TABLE`. Nghĩa là
> mọi thay đổi cấu trúc bảng đều đi kèm xoá sạch dữ liệu. Cách vá: đưa schema hiện tại thành
> `V1__khoi_tao.sql` của Flyway. Công sức: 1–2 ngày."*

Đã làm, nhưng **không theo đúng cách vá đề xuất**. Đưa *schema hiện tại* thành `V1` thì hai lần
đổi cấu trúc của chính đợt hoàn thiện biến mất vào bên trong file đó, và Flyway trở thành thứ
khai mà không dùng: có thư mục di trú nhưng chưa từng di trú cái gì.

Cách đã làm: `V1__khoi_tao.sql` là cấu trúc **trước** đợt hoàn thiện, còn
`V2__khoa_lac_quan_va_chong_do_mat_khau.sql` là ba cột thêm trong đợt này
(`hoa_don.phien_ban`, `nguoi_dung.so_lan_sai`, `nguoi_dung.khoa_den_luc`) — trước đó cả ba đều
được gõ `ALTER TABLE` bằng tay trên console, không dấu vết.

**Đo được, không phải nói suông:** dựng một CSDL nháp chỉ có `V1`, chèn một dòng dữ liệu, chạy
`V2` lên — dòng đó còn nguyên và ba cột mới đã có. Và CSDL do Flyway dựng đối chiếu với CSDL do
cơ chế cũ dựng: **0 dòng lệch trên 20.519 dòng dump**.

### 5.3. 🔴 Hóa đơn chưa hợp pháp ở Việt Nam

Hóa đơn xuất ra là PDF do `OpenPDF` dựng. Từ 01/07/2022, hóa đơn bán hàng ở Việt Nam bắt buộc
là **hóa đơn điện tử có mã của cơ quan thuế**, có chữ ký số, phát hành qua nhà cung cấp được
cấp phép và truyền dữ liệu về Tổng cục Thuế (Nghị định 123/2020, Thông tư 78/2021).

Đây là rào cản **pháp lý**, không phải kỹ thuật — không vá được bằng cách viết thêm mã, mà
phải tích hợp với một nhà cung cấp hóa đơn điện tử. Công sức: **3–6 tuần** kể cả thủ tục.

~~Thuế suất VAT còn đang **chôn cứng** `THUE_SUAT_VAT = 0.10` trong `ThamSoTinhCuoc`.~~
✅ **ĐÃ SỬA (việc V5).** Thuế suất nay đọc từ `billing.thue-suat-vat` trong `application.yml`.
Ba chỗ **hiển thị** cũng đi theo — màn hình chi tiết hóa đơn, bản PDF, bảng đối soát: tính 8%
mà tờ hóa đơn khách cầm vẫn ghi 10% thì tệ hơn là không làm. Khai sai (gõ `10` thay vì `0.10`)
làm ứng dụng **không khởi động được**, chứ không lặng lẽ lập vài trăm tờ hóa đơn sai.

### 5.4. 🟠 Trần quy mô cứng — ĐÃ ĐO, không còn là phỏng đoán

`RatingService:266` nạp **toàn bộ bản ghi cước của một kỳ vào một `List` trong bộ nhớ**;
`TruCuocTraTruocService:92` cũng vậy. Phía ghi đã chia lô, phía đọc thì chưa.

> **Bản đánh giá gốc viết:** *"18.723 bản ghi thì không sao. Một triệu bản ghi bắt đầu nguy
> hiểm; vài chục triệu là hết bộ nhớ."* — đó là **phỏng đoán chưa đo**, và để một câu như vậy
> trong báo cáo là tự phá chuẩn làm việc của chính dự án (*"đo trên dữ liệu thật, không suy
> luận suông"*).

**Đã đo** (việc V5): sinh 200.000 bản ghi — gấp **10,7 lần** toàn bộ dữ liệu hiện tại — rồi
chạy trọn vòng trên máy phát triển, JVM ghim `-Xmx2g`:

| Bước | Thời gian | Heap đỉnh |
|---|---:|---:|
| Sinh 200.000 bản ghi | 11,4 giây | 147 MB |
| Tính cước 200.000 bản ghi | 70,4 giây | **412 MB** |
| Lập hóa đơn (58 hóa đơn) | 85,6 giây | **490 MB** |
| **Trọn vòng** | **2 phút 47** | **490 MB / 2 GB** |

Số liệu chi tiết và cách tự chạy lại: [`toi-uu-hieu-nang.md`](toi-uu-hieu-nang.md) phần III.

**Kết luận đọc được từ số đo:** ở mức 200.000 bản ghi, hệ thống dùng hết **một phần tư** của
2 GB heap và chạy xong trong chưa tới 3 phút. Trần quy mô **có thật** nhưng **xa hơn nhiều** so
với câu chữ ở trên: ngoại suy tuyến tính từ 490 MB cho 200.000 bản ghi, 2 GB heap chịu được
khoảng **800.000 bản ghi một kỳ**. Với một tỉnh cỡ vài chục nghìn thuê bao thì đó là dư.

⚠️ Ngoại suy tuyến tính là một **giả định**, không phải số đo — đã ghi rõ để không ai trích lại
con số 800.000 như thể nó được đo. **Cách vá vẫn nguyên giá trị:** đổi sang con trỏ cuộn hoặc
phân trang theo lô. Công sức: **2–3 ngày**. Nhưng nay nó là việc *có thể hoãn có căn cứ*, chứ
không phải việc gấp vì một câu phỏng đoán.

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
