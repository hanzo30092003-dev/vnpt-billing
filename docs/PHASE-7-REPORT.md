# BÁO CÁO PHASE 7 — HOÀN THIỆN, KIỂM THỬ, TÀI LIỆU

> Phase cuối. Không thêm chức năng mới — chỉ hoàn thiện, sửa lỗi, viết tài liệu.

---

## 1. Phạm vi và ràng buộc

Ràng buộc tự đặt cho cả phase: **không thêm chức năng mới**. Mọi thay đổi phải trả lời được
câu hỏi *"cái này sửa lỗi gì, hay hoàn thiện cái gì đã có?"* — không trả lời được thì không làm.

Ràng buộc này bị thử thách thật ở mục F: nợ số 4 của Phase 6 (*báo cáo chưa có bộ lọc theo
khách hàng*) là một chức năng mới, không phải một lỗi. Nó **không được làm**, và được ghi lại
ở mục 9 như một giới hạn đã biết. Đó là cách một khoản nợ được "xử lý" mà không phải "làm" —
xác định nó thuộc loại gì, rồi kết luận đúng theo loại đó.

---

## 2. Mục A — Rà soát toàn bộ đường điều hướng

### 2.1. Ba lỗi tìm được

| # | Lỗi | Triệu chứng | Nguyên nhân |
|---|---|---|---|
| 1 | `/giam-tru` trả HTTP 500 | Màn hình giảm trừ **hỏng từ Phase 5**, không ai phát hiện | `th:each="gt : ${danhSach}"` — `gt` là toán tử *greater-than* của Thymeleaf |
| 2 | `?trang=-1` trả HTTP 500 | Sửa tay thanh địa chỉ là làm sập trang | Spring Data ném `IllegalArgumentException` khi số trang âm |
| 3 | `/hoa-don/abc` trả HTTP 500 | Đường dẫn sai kiểu ra trang lỗi hệ thống | `MethodArgumentTypeMismatchException` không có nhánh xử lý |

**Lỗi 1 đáng nói nhất.** `gt` là tên biến hoàn toàn hợp lệ trong Java, nhưng trong biểu thức
SpringEL của Thymeleaf nó là từ khoá. Trình phân tích không báo lỗi — nó trả về `null` và trang
sập ở chỗ khác. Màn hình đó đã hỏng suốt từ Phase 5 tới đây mà không phép kiểm nào bắt được, vì
mọi phép kiểm đều **gõ thẳng đường dẫn** các màn hình chính chứ không đi theo liên kết.

Sửa: đổi tên biến thành `khoan` (18 chỗ). Một chỗ nằm lồng trong
`formatDecimal(gt.soTien, ...)` nên phải sửa riêng.

### 2.2. Hai lớp phòng vệ dựng thêm

**`ThamSoPhanTrang`** — gom phép chặn phân trang vào một chỗ:

```java
public static final int SO_DONG_TOI_DA = 200;
public static int trangHopLe(int trang)   { return Math.max(0, trang); }
public static int soDongHopLe(int soDong) { return soDong < 1 ? 1 : Math.min(soDong, SO_DONG_TOI_DA); }
```

Trước đó bốn controller đã có `Math.max(trang, 0)` viết thẳng trong hàm — **đúng luật nhưng mỗi
chỗ một bản sao**, và hai chỗ còn thiếu. Đây là dạng lỗi mà việc nhân bản luật sinh ra: không ai
sai, chỉ là không ai phủ hết. Nay cả 6 controller gọi cùng một hàm, kèm 10 test
(`ThamSoPhanTrangTest`).

**`KiemTraDieuHuongTest`** — 5 test giữ cho lỗi loại 1 không quay lại:

1. Mọi `th:href="@{...}"` trong template phải trỏ tới một đường dẫn có controller nhận
2. Không còn `href="#"` chết trong template
3. Mọi đường dẫn controller khai báo phải đến được từ menu hoặc từ một trang khác
4. Tài nguyên tĩnh (`/css/`, `/js/`, `/images/`, `/webjars/`, `/favicon`) được miễn trừ
5. **Biến vòng lặp `th:each` không được trùng từ khoá Thymeleaf** — `gt`, `lt`, `ge`, `le`,
   `eq`, `ne`, `and`, `or`, `not`, `div`, `mod`, `true`, `false`, `null`

### 2.3. Ba lần phép kiểm tự nó sai

Test 1 và 3 báo động giả **ba lần liên tiếp** trước khi dùng được:

| Lần | Báo động giả | Vì sao | Sửa |
|---|---|---|---|
| 1 | Cắt đường dẫn tại dấu `}` đầu tiên | `@{/hoa-don/{id}(id=${x})}` có `}` lồng nhau | Viết bộ đếm độ sâu ngoặc thay cho biểu thức chính quy |
| 2 | Báo `/css/app.css` không có controller | Tài nguyên tĩnh không đi qua controller | Danh sách miễn trừ tài nguyên tĩnh |
| 3 | Bắt `href="#"` nằm trong **chính chú thích của mình** | Quét cả phần bị comment | Loại bỏ `<!--.*?-->` trước khi quét |

Đúng **bài học 43.5** — *một phép kiểm sai nguy hiểm ngang thiếu phép kiểm*. Ghi lại đây vì lần
thứ ba là lần khó chịu nhất: phép kiểm bắt lỗi trong đoạn văn bản mà chính nó vừa được viết ra
để giải thích nó.

### 2.4. Script `test-dieu-huong.ps1`

13 phép kiểm, và điểm khác biệt nằm ở cách kiểm: script **đọc thuộc tính `href` từ HTML rồi đi
theo**, không gõ đường dẫn dựng sẵn. 23 lần bấm menu + 55 liên kết cấp hai. Đây chính là phép
kiểm mà nếu có từ Phase 5 thì `/giam-tru` đã không hỏng suốt hai phase.

---

## 3. Mục B — Hoàn thiện giao diện

| Việc | Chi tiết |
|---|---|
| **Breadcrumb tự động** | `LayoutAdvice` suy ra vệt điều hướng từ URI cho **mọi** view, không phải khai tay ở từng template. Đoạn số (id) bị bỏ qua; trang chi tiết gắn nhãn "Chi tiết" |
| **Sidebar cho màn hình hẹp** | Dưới 992px: sidebar trượt từ trái (`left: -100vw` → `0`), kèm lớp phủ mờ và nút mở |
| **Dọn liên kết chết** | Bỏ mục "Quản trị" `href="#"`; nối đủ Hóa đơn / Thanh toán / Công nợ / Giảm trừ / Báo cáo |
| **Ghi log ra file** | `logs/vnpt-billing.log`, xoay vòng theo ngày, 10MB/file, giữ 30 ngày. `logs/` vào `.gitignore` |

**Bẫy môi trường đáng ghi:** phần sidebar tốn nhiều thời gian nhất không phải vì CSS khó, mà vì
**Spring phục vụ tài nguyên tĩnh từ `target/classes`, không phải từ `src/main/resources`**. Sửa
`app.css` rồi tải lại trang thì vẫn thấy bản cũ. Cộng thêm hai nguồn nhiễu nữa: giả lập màn hình
di động không đáng tin, và việc đo vị trí sidebar **trong lúc hiệu ứng 0,2 giây đang chạy** cho
ra số nằm giữa chừng. Kết luận chỉ chắc chắn khi đo trên cửa sổ thật rộng 785px: sidebar ở
`x = −250`, nút mở hiện ra, không có thanh cuộn ngang.

---

## 4. Mục C — Xử lý lỗi và rà kỳ rỗng

### 4.1. Bốn nhánh xử lý ngoại lệ

| Ngoại lệ | Kết quả | Mức log |
|---|---|---|
| `NghiepVuException` | Quay lại trang trước kèm thông báo | (có sẵn từ Phase 2) |
| `MethodArgumentTypeMismatchException` | HTTP 400 → `error/400.html` | `warn`, **không** in vết ngăn xếp |
| `NoResourceFoundException` | HTTP 404 → `error/404.html` | `debug` |
| `Exception` | HTTP 500 → `error/500.html` kèm **mã sự cố** `yyyyMMdd-HHmmss` | `error` kèm vết ngăn xếp |

**`AccessDeniedException` phải được ném lại**, không được bắt. Bắt nó thì `ExceptionTranslationFilter`
của Spring Security không còn thấy, và mọi lỗi 403 biến thành 500 — tức là biến một câu trả lời
đúng ("bạn không có quyền") thành một lời nói dối ("máy chủ hỏng").

**Mã sự cố** là khoá tra cứu giữa cái người dùng nhìn thấy và cái nhật ký ghi lại. Người dùng
đọc mã cho người quản trị, người quản trị tìm mã đó trong `logs/vnpt-billing.log`.

### 4.2. Rà kỳ rỗng — một kết quả âm có giá trị

Thêm **kỳ 8/2026 hoàn toàn rỗng**, rồi quét 17 màn hình + 4 lần xuất Excel + 3 thao tác nghiệp
vụ (`test-ky-rong.ps1`, 28 phép kiểm).

**Kết quả: 0 lỗi 500.** Đây là kết quả âm, nhưng có giá trị: nó xác nhận lỗi
`SUM()` trả `NULL` không unbox được — tìm ra ở Phase 6 và sửa bằng `COALESCE(..., 0L)` — là
**trường hợp duy nhất** của lớp lỗi đó, chứ không phải cái đầu tiên trong một dãy.

Script được viết **tự dọn và chạy lại được bao nhiêu lần cũng ra một kết quả**. Bản đầu không
như vậy: nó lập 58 hóa đơn cho kỳ 8 rồi để nguyên, nên lần chạy thứ hai gặp kỳ đã `DA_CHOT` và
hỏng. Phải thêm bước dọn ở đầu script và bước hoàn tác (huỷ hóa đơn) sau phép kiểm phá hoại.

---

## 5. Mục D, E, G — Ba tài liệu kiểm thử và demo

| Tài liệu | Nội dung |
|---|---|
| `docs/kich-ban-kiem-thu.md` | **70 ca kiểm thủ công** chia 9 nhóm + tổng hợp test tự động + cảnh báo ca kiểm phụ thuộc ngày xem |
| `docs/kich-ban-demo.md` | **12 bước, 18 phút** (rút gọn còn 12–15). Mỗi bước kèm *câu hỏi hội đồng có thể hỏi* và gợi ý trả lời |
| `docs/danh-sach-anh-chup.md` | **62 ảnh** chia 8 chương, 3 ảnh đánh dấu 🔴 phụ thuộc ngày chụp, kèm danh sách tối thiểu 12 ảnh nếu thiếu thời gian |

Kịch bản demo cố ý đặt **bảng đối soát cước** ở vị trí trung tâm và chuẩn bị sẵn câu trả lời cho
sáu câu hỏi khó: vì sao dùng `BigDecimal`, ba chỗ quy đổi đơn vị, vì sao không cắt đôi bản ghi
khi vượt quota, thế nào là thao tác lặp lại được, vì sao phải chụp ảnh đơn giá, và vì sao chọn
Spring Boot 3.5 thay vì 4.x.

---

## 6. Mục F — Dọn nợ và tài liệu

### 6.1. Năm khoản nợ của Phase 6

| # | Nợ | Xử lý |
|---|---|---|
| 1 | Bảng aging đủ 5 nhóm chỉ đúng tới **13/08/2026** | **Ghi nhận, không sửa.** Đây là tính chất của *ngày xem*, không phải của dữ liệu — xem mục 9.1 |
| 2 | Bốn kỳ có CDR trả trước chưa trừ vào số dư (kỳ 3, 4, 5, 7) | **Ghi nhận, không sửa.** Hệ quả của quyết định Phase 6 mục 1.3; không vi phạm bất biến sổ cái — xem mục 9.2 |
| 3 | Dashboard hiện liên kết `/hoa-don/{id}` cho mọi vai trò → 403 khi bấm | ✅ **Đã hết.** Kiểm lại bằng cách đăng nhập cả ba vai trò, lấy mọi `href` trên dashboard rồi đi theo: `nhanvien01` 5 liên kết, `ketoan01` 7, `admin` 14 — **0 liên kết trả 403 hay 500** |
| 4 | Báo cáo chưa có bộ lọc theo khách hàng | **Không làm** — đây là chức năng mới, vi phạm ràng buộc của phase. Ghi ở mục 9.3 |
| 5 | Phần viết báo cáo cho mục A–D của Phase 5 | **Không viết hộ** theo yêu cầu. Danh sách gạch đầu dòng ở mục 10 |

### 6.2. Tài liệu

- **`README.md` viết lại**: bảng màn hình chính, yêu cầu môi trường, cài đặt 4 bước, 3 tài khoản
  demo, bảng 6 kỳ cước, cảnh báo `reset`, cấu trúc thư mục, mục lục tài liệu, khắc phục sự cố
- **`docs/huong-dan-su-dung.md` mới**: 11 mục, hướng dẫn thao tác từng chức năng, kèm bảng
  *"những trường hợp bị chặn"* cho mỗi màn hình có ràng buộc
- **`docs/mo-ta-csdl.md` rà lại**: cập nhật mục 4 và 4.1 theo dữ liệu hiện hành

### 6.3. Cách rà `mo-ta-csdl.md` — và một phép kiểm sai nữa

Rà 155 cột bằng mắt là không khả thi, nên viết script đối chiếu
`information_schema.COLUMNS` với nội dung tài liệu.

**Bản đầu báo "0 cột thiếu" — và đó là báo cáo sai.** Script tách tài liệu thành khối theo tiêu
đề, nhưng biểu thức nhận diện tiêu đề không khớp tiêu đề thật (tiêu đề là
`### 2.5. \`thue_bao\` — Thuê bao điện thoại`, có cả phần mô tả tiếng Việt phía sau). Kết quả là
**không khối nào được nhận diện**, vòng lặp không so sánh gì, và biến đếm giữ nguyên 0. Con số 0
đó không có nghĩa "đầy đủ" mà có nghĩa "không kiểm gì".

Bản sửa neo vào mẫu `^### 2.\d+. \`tên_bảng\`` và đòi mỗi cột phải có **dòng bảng của riêng nó**
(`^| \`tên_cột\` |`) nằm trong **đúng mục của bảng đó** — chứ không phải xuất hiện đâu đó trong
tài liệu. Kết quả thật: **15/15 bảng, 155/155 cột** đều có dòng mô tả riêng, không cột nào thiếu.

Đây là lần thứ **bảy** trong dự án gặp bài học 43.5.

---

## 7. Nghiệm thu

| # | Tiêu chí | Yêu cầu | Kết quả |
|---|---|---|---|
| 1 | `mvnw test` | ≥ 270 test, 0 lỗi | ✅ **275 test**, 0 lỗi |
| 2 | `KiemTraDieuHuongTest` | PASS | ✅ 5/5 |
| 3 | Kỳ 8/2026 rỗng, mọi màn hình mở được | 0 lỗi 500 | ✅ 28/28 (`test-ky-rong.ps1`) |
| 4 | Bất biến sổ cái + bất biến thanh toán | 0 dòng lệch | ✅ 0 / 0 / 0 (ba câu SQL) |
| 5 | `reset` tái lập đúng dữ liệu | Giống từng dòng | ✅ **20.102 / 20.102 dòng giống hệt** |
| 6 | Clone repo → chạy được theo README | Chạy được | ✅ (mục 7.1) |
| 7 | Bốn tài liệu có mặt | Đủ 4 | ✅ kiểm thử · demo · ảnh chụp · hướng dẫn sử dụng |

### 7.1. Kiểm clone — làm thật, không suy luận

Clone kho về một thư mục mới hoàn toàn, rồi làm đúng theo README bước 3:

```
git clone <repo> clone-test
cd clone-test
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

Kết quả: `Started BillingApplication in 6.103 seconds`, `GET /login` trả HTTP 200. Thư mục
`target/` và `logs/` **không** lọt vào bản clone.

Phép kiểm này gộp luôn tiêu chí 5: `reset` chạy **từ bản clone** phải dựng lại đúng bộ dữ liệu
mà bản gốc đang có. Chụp CSDL trước và sau rồi so từng dòng — **20.102 dòng, không dòng nào
khác**. Một lệnh chứng minh cả hai điều: bản clone chạy được, và nó chạy ra đúng dữ liệu của
báo cáo.

### 7.2. Số liệu bàn giao

| Hạng mục | Số lượng |
|---|---|
| Kỳ cước | **6** (3, 4, 5/2026 đã chốt · 6, 7, 8/2026 mở) |
| CDR | 18.723, tất cả `DA_TINH` |
| Hóa đơn | 280 · chi tiết hóa đơn 620 |
| Thanh toán | 161 (kỳ 3: 58 · kỳ 4: 55 · kỳ 5: 48 · **kỳ 6, 7, 8: 0**) |
| Khách hàng · thuê bao | 50 · 80 |
| Sổ cái biến động số dư | 34 dòng |
| Test tự động | **275** (237 độc lập · 38 cần MySQL, 8 lớp) |
| Phép kiểm giao diện | **177** trên 8 script |

Tiền: doanh thu **111.513.012 đ**, đã thu **49.190.687 đ**, còn nợ **62.322.325 đ** (44,1%).

---

## 8. Trạng thái CSDL để demo

| Điều kiện | Trạng thái |
|---|---|
| Kỳ 8/2026 rỗng, sẵn sàng chạy trực tiếp trên sân khấu | ✅ 0 CDR · 0 hóa đơn · trạng thái `MO` |
| Kỳ 6 và 7 giữ 0 thanh toán → còn huỷ hóa đơn được | ✅ 0 / 0 |
| Ba kỳ đã chốt có đủ 5 nhóm tuổi nợ | ✅ (tới 13/08/2026 — mục 9.1) |
| Cả ba bất biến | ✅ 0 lệch |

Kỳ 8 đã được đưa vào `data-mau.sql` nên `reset` dựng lại đúng trạng thái này. ID của nó là **8**
chứ không phải 6 — id 6 và 7 đã bị các kỳ thử nghiệm tạm dùng rồi xoá. Giữ đúng id thật thay vì
đánh số lại, vì `data-van-hanh.sql` tham chiếu tới id.

---

## 9. Giới hạn đã biết

### 9.1. Bảng aging đủ 5 nhóm chỉ đúng tới 13/08/2026

Sau ngày đó, hóa đơn cũ nhất trôi sang nhóm *trên 90 ngày* và nhóm *61–90* rỗng đi. **Đây không
phải lỗi** — đó là tính chất của *ngày xem*, không phải của dữ liệu. Chụp ảnh minh hoạ bảng
aging đủ 5 nhóm thì chụp trước ngày đó (3 ảnh 🔴 trong `danh-sach-anh-chup.md`).

### 9.2. Bốn kỳ có CDR trả trước chưa trừ vào số dư

Kỳ 3, 4, 5, 7 có CDR của thuê bao trả trước nhưng chưa chạy bước *trừ cước trả trước*. Không vi
phạm bất biến sổ cái (`so_du = SUM(nạp) − SUM(trừ)` vẫn đúng), chỉ nghĩa là số dư của các thuê
bao đó cao hơn mức "đã trừ hết cước". Chạy bù được bất cứ lúc nào, nhưng **trừ cước không giao
hoán theo kỳ**, nên phải chạy theo đúng thứ tự kỳ.

### 9.3. Báo cáo chưa lọc theo khách hàng

Bảy báo cáo lọc theo kỳ; chỉ `/cong-no` có lọc theo khách hàng. Là **chức năng còn thiếu**, không
phải lỗi — nên không làm ở phase này.

### 9.4. Ba hành vi cố ý giữ nguyên

| Hành vi | Vì sao giữ |
|---|---|
| Lập hóa đơn cho kỳ rỗng vẫn ra hóa đơn chỉ có cước thuê bao | Đúng nghiệp vụ: khách không gọi vẫn phải trả phí thuê bao |
| Lập hóa đơn cho kỳ chưa kết thúc tính trọn tháng | Kịch bản demo lập hóa đơn kỳ 8 ngay trên sân khấu — chặn lại là chặn luôn phần demo giá trị nhất |
| Trạng thái *Thanh toán một phần* không chuyển thành *Quá hạn* | Ràng buộc từ Phase 5: quét quá hạn chỉ chạm hóa đơn chưa thu đồng nào |

---

## 10. Gạch đầu dòng cho mục A–D của Phase 5

> Phần này **không phải bài viết**. Đây là liệt kê những gì đã làm ở mục A–D của Phase 5 để
> người viết báo cáo dựa vào mà viết.

**Mục A — Màn hình hóa đơn**

- Danh sách hóa đơn: lọc theo kỳ, trạng thái, khách hàng, khoảng tiền; phân trang; xuất Excel
- Chi tiết hóa đơn: khối thông tin thuê bao, bảng khoản mục, khối tổng tiền, khối lịch sử thanh toán
- Bốn trạng thái hóa đơn: `CHUA_TT`, `TT_MOT_PHAN`, `DA_TT`, `QUA_HAN`
- Xuất hóa đơn ra PDF (OpenPDF), nhúng font để không vỡ dấu tiếng Việt
- Liên kết chéo sang **bảng đối soát cước** của Phase 4 — trả lời "vì sao ra con số này"
- Cột `con_no` chỉ **đọc** trên mọi màn hình, không màn hình nào tự tính lại

**Mục B — Ghi nhận thanh toán**

- Form ghi nhận thanh toán: số tiền mặc định bằng trọn số còn nợ, sửa được để thu một phần
- Ba hình thức: tiền mặt, chuyển khoản, ví điện tử
- Bốn phép chặn: vượt số còn nợ · số tiền ≤ 0 · ngày ở tương lai · hóa đơn đã thu đủ
- `ThanhToanService` là **nơi ghi duy nhất** của `da_thanh_toan`, `con_no`, `trang_thai`
- Cộng dồn bằng `BigDecimal`, không dùng `double`
- In phiếu thu ra PDF
- Danh sách thanh toán: lọc theo kỳ, hình thức, người thu, khoảng ngày

**Mục C — Công nợ**

- Bảng tuổi nợ 5 nhóm: trong hạn · 1–30 · 31–60 · 61–90 · trên 90 ngày
- Danh sách hóa đơn còn nợ, sắp theo số ngày quá hạn giảm dần
- Đề xuất tạm ngừng thuê bao có hóa đơn quá hạn trên 15 ngày — **là đề xuất, không tự động cắt**
- Quét chuyển trạng thái quá hạn, **chỉ chạm hóa đơn chưa thu đồng nào**
  (bỏ điều kiện này thì `TT_MOT_PHAN` thành trạng thái không thể tồn tại sau hạn)
- Biểu đồ tuổi nợ

**Mục D — Giảm trừ**

- Khai giảm trừ theo **số tiền tuyệt đối** hoặc **tỷ lệ phần trăm**, chặn khai cả hai hoặc bỏ trống cả hai
- Tỷ lệ quy thành số tiền **đúng một lần** lúc lập hóa đơn rồi ghi cứng — giữ nguyên tắc *làm
  tròn ở đúng một tầng*
- Khoản `DA_AP_DUNG` không sửa/xoá được vì số tiền đã nằm trong hóa đơn; huỷ hóa đơn thì tự quay
  về `CHUA_AP_DUNG`
- Giảm trừ vào hóa đơn dưới dạng một dòng `chi_tiet_hoa_don` **thành tiền âm**
- Sửa giảm trừ phải tính lại **toàn chuỗi** `tong_truoc_thue → thue_vat → tong_thanh_toan → con_no`

**Điểm chung nên nhấn khi viết**

- Ba ràng buộc của Phase 5 (`PHASE-5-PLAN.md`) được giữ nguyên vẹn qua cả bốn mục
- Bất biến `con_no = tong_thanh_toan − da_thanh_toan` và
  `da_thanh_toan = SUM(thanh_toan.so_tien)` được kiểm sau **mỗi** mục, không phải chỉ ở cuối

---

## 11. TỔNG KẾT TOÀN DỰ ÁN

### 11.1. Đã làm được gì

Tám phase, từ khung dự án rỗng tới một hệ thống chạy trọn vòng nghiệp vụ viễn thông:

| Phase | Nội dung | Kết quả cụ thể |
|---|---|---|
| 0 | Khung dự án | Spring Boot + MySQL + Thymeleaf chạy được, layout dùng chung |
| 1 | Cơ sở dữ liệu | 15 bảng + 2 view · 15 entity · 16 enum · 15 repository |
| 2 | Xác thực, khách hàng, thuê bao | Đăng nhập BCrypt, 3 vai trò, CSRF; form đổi động; ma trận chuyển trạng thái |
| 3 | Gói cước, bảng giá, CDR | Bảng giá theo thời gian chặn chồng khoảng; bộ sinh CDR; nhập CSV |
| 4 | **Engine tính cước** | Định giá từng CDR, quỹ ưu đãi 4 loại, prorate, VAT, **bảng đối soát cước** |
| 5 | Hóa đơn, thanh toán, công nợ | Sổ cái số dư; 4 trạng thái hóa đơn; tuổi nợ 5 nhóm; giảm trừ |
| 6 | Báo cáo, thống kê | Dashboard; 7 báo cáo; xuất Excel; in; gom nhóm trong CSDL |
| 7 | Hoàn thiện | 3 lỗi 500 được sửa; 4 tài liệu; 275 test; clone chạy được |

**Quy mô cuối:** 15 bảng · 18.723 CDR · 280 hóa đơn · 161 thanh toán · 275 test tự động ·
177 phép kiểm giao diện · 15 tài liệu.

### 11.2. Ba quyết định kỹ thuật định hình cả dự án

**1. `BigDecimal`, `HALF_UP`, scale 0 — làm tròn ở đúng MỘT tầng.**
Tầng làm tròn duy nhất là tầng CDR; mọi mức trên chỉ cộng dồn. Nếu làm tròn ở nhiều tầng thì
`SUM(cuoc_phi) ≠ hoa_don.cuoc_thoai` và bảng đối soát sẽ lệch. Quyết định này về sau ràng buộc
cả cách xử lý giảm trừ theo tỷ lệ ở Phase 5 — tỷ lệ phải quy thành số tiền đúng một lần.

**2. Không cắt đôi bản ghi khi vượt quota.**
Nghe như chi tiết nhỏ, nhưng nó khiến kết quả **phụ thuộc thứ tự duyệt**, và từ đó **mọi** truy
vấn duyệt CDR bắt buộc `ORDER BY thoi_gian_bat_dau, id`. Ràng buộc lan sang cả bước trừ cước trả
trước ở Phase 5 — cùng một tình huống quỹ cạn dần.

**3. Một nguồn sự thật cho "còn nợ".**
`hoa_don.con_no` chỉ được ghi trong `ThanhToanService`. Mọi màn hình chỉ đọc. Cám dỗ tính
`tong_thanh_toan − SUM(thanh_toan)` ngay trên view rất lớn vì nó nhanh và "không ai sửa gì" —
nhưng đó là cách sinh ra nguồn sự thật thứ hai, và hai nguồn thì sớm muộn cũng lệch.

### 11.3. Bài học lớn nhất: phép kiểm sai nguy hiểm ngang phép kiểm thiếu

Bài học 43.5 xuất hiện **bảy lần** trong dự án, mỗi lần một dạng khác nhau:

| Lần | Ở đâu | Phép kiểm sai vì |
|---|---|---|
| 1 | Phase 4 | Kiểm luật cụ thể thay vì kiểm bất biến |
| 2 | Phase 4 | Lấy mẫu thay vì kiểm toàn bộ |
| 3 | Phase 5 | Dữ liệu thử thiết kế theo một chiều, chỉ đúng theo chiều đó |
| 4 | Phase 6 | `test-auth.ps1` tìm chữ "Hóa đơn" trên **toàn trang** — dashboard mới có chữ đó một cách hợp lệ |
| 5 | Phase 6 | `test-muc-F.ps1` chôn cứng số 112 hóa đơn, thêm kỳ mới là báo sai |
| 6 | Phase 7 | Test điều hướng bắt lỗi trong **chính chú thích của mình** |
| 7 | Phase 7 | Script rà tài liệu báo "0 cột thiếu" trong khi nó **không nhận diện được mục nào** |

Điểm chung của cả bảy: phép kiểm **báo an toàn**, và cái sai chỉ lộ ra khi đi kiểm chính phép
kiểm đó. Lần 7 là nguy hiểm nhất — nó cho ra con số 0 đúng như mong đợi, và nếu không dừng lại
hỏi *"0 này nghĩa là đầy đủ, hay nghĩa là không kiểm gì?"* thì tài liệu sẽ được tuyên bố là khớp
schema mà không ai từng đối chiếu.

**Cách phòng đã rút ra:** mỗi phép kiểm mới phải trả lời được câu *"nếu thứ tôi đang kiểm hỏng
thật, phép kiểm này có kêu không?"* — và cách duy nhất trả lời chắc chắn là **làm nó hỏng thử
một lần** rồi xem nó có kêu không.

### 11.4. Sáu điểm sai lệch đặc tả đã nêu ra

Nêu ra và đề xuất thay vì im lặng làm theo — phần này là phần có giá trị nhất của báo cáo:

| Phase | Tiền đề trong đặc tả | Thực tế |
|---|---|---|
| 4 | Bảng giá phủ hết mọi tổ hợp dịch vụ × hướng | Thiếu `SMS/QUOC_TE`, 251 CDR không tra được đơn giá |
| 4 | Quy đổi đơn vị là chuyện hiển thị | Quy sản lượng **lên** đơn vị quota thổi phồng **+10,97%** và thu tiền oan của khách |
| 5 | `data-mau.sql` có sẵn hóa đơn để seed thanh toán | Không có hóa đơn nào |
| 5 | Quét quá hạn áp cho mọi hóa đơn quá hạn | Áp thế thì `TT_MOT_PHAN` thành trạng thái không thể tồn tại |
| 6 | Kỳ 7/2026 đã có sẵn | Chưa có, phải lập |
| 6 | Kỳ mới cần trừ cước trả trước như kỳ 6 | Trừ cước **không giao hoán theo kỳ** — chạy sai thứ tự là hỏng sổ cái |

### 11.5. Còn thiếu gì

| Hạng mục | Ghi chú |
|---|---|
| Bộ lọc khách hàng trên 7 báo cáo | Chức năng còn thiếu, đã ghi ở mục 9.3 |
| Trừ cước trả trước cho kỳ 3, 4, 5, 7 | Chạy bù được, phải theo đúng thứ tự kỳ |
| Bảng aging bền theo thời gian | Cần nới dải tuổi nợ hoặc sinh dữ liệu theo ngày hiện tại |
| Giao diện quản trị người dùng | Ba tài khoản nạp từ `data-mau.sql`, không có màn hình quản lý |
| Kiểm thử tự động cho giao diện | 177 phép kiểm hiện chạy qua HTTP bằng PowerShell, không phải Selenium |

### 11.6. Nếu làm lại từ đầu

- **Viết `KiemTraDieuHuongTest` từ Phase 2.** Nếu có nó sớm, `/giam-tru` đã không hỏng suốt hai
  phase mà không ai biết.
- **Cho `CdrGeneratorService` nhận hạt giống ngay từ Phase 3.** Thiếu nó khiến toàn bộ dữ liệu
  của Phase 4 và 5 không dựng lại được, và phải cứu bằng cách dump ra `data-van-hanh.sql`.
- **Đặt quy ước tên biến trong template ngay từ đầu.** Tên hợp lệ trong Java có thể là từ khoá
  của Thymeleaf, và trình phân tích không báo lỗi mà trả `null`.
- **Công bố dự đoán trước khi viết code sớm hơn.** Phase 5 mục G3 mới bắt đầu làm và bắt được
  ngay hai vấn đề mà chạy code trước rồi nhìn kết quả sẽ không thấy.
