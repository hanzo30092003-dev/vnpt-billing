# KẾ HOẠCH HOÀN THIỆN — ĐƯA ĐỒ ÁN TỪ 93% LÊN 100%

> Mục tiêu là **100% ở thang đồ án môn học**, không phải thang sản phẩm thương mại.
> Nghĩa là: không còn chỗ nào hội đồng chỉ vào được và nói *"cái này hỏng"* hoặc
> *"cái này khai mà không dùng"*.
>
> Đánh giá gốc: [`DANH-GIA-HE-THONG.md`](DANH-GIA-HE-THONG.md)

---

## 1. Bảy điểm còn thiếu, quy ra điểm số

Bảng dưới lấy từ bảng chấm ở báo cáo đánh giá. Cột cuối là **số điểm lấy lại được** nếu làm
xong — đó là thứ quyết định thứ tự ưu tiên, không phải cảm giác việc nào "khó hơn".

| Mặt | Trọng số | Hiện tại | Sau kế hoạch | Điểm lấy lại |
|---|---:|---:|---:|---:|
| Bảo mật & tuân thủ | 11% | 85 | 97 | **+1,32** |
| Vận hành | 5% | 70 | 95 | **+1,25** |
| Tính đúng số tiền | 18% | 95 | 100 | **+0,90** |
| Chức năng nghiệp vụ | 22% | 95 | 99 | **+0,88** |
| Dữ liệu & toàn vẹn | 11% | 92 | 99 | **+0,77** |
| Kiến trúc & mã | 10% | 95 | 98 | +0,30 |
| Giao diện & UX | 8% | 97 | 100 | +0,24 |
| Kiểm thử · Tài liệu | 15% | 100 | 100 | — |
| **Tổng** | | **93** | **≈ 99** | **+5,7** |

Một điểm cuối cùng đến từ việc dọn xong **nợ sản phẩm nộp** ở mục 3 — thứ không nằm trong mã.

---

## 2. Việc phải làm trong mã

Xếp theo điểm lấy lại được, không theo độ khó.

### 🔴 V1. Khoá lạc quan cho hóa đơn — *nửa ngày*

**Vì sao:** đây là lỗi đúng đắn duy nhất còn lại, và nó hỏng đúng **bất biến trung tâm** mà cả
đồ án được xây quanh nó. Hội đồng hỏi *"hai người cùng thu tiền thì sao"* là không có câu trả lời.

**Làm gì**
1. Thêm `@Version private Long phienBan;` vào `HoaDon`, cột `phien_ban BIGINT DEFAULT 0` trong `schema.sql`
2. Bắt `ObjectOptimisticLockingFailureException` trong `GlobalExceptionHandler`, đổi thành thông báo tiếng Việt: *"Hóa đơn này vừa được người khác cập nhật. Hãy mở lại hóa đơn và ghi nhận lại."*
3. Viết `KiemTraDongThoiThanhToanTest`: hai luồng cùng gọi `ghiNhan` trên một hóa đơn bằng `CountDownLatch`

**Nghiệm thu:** test hai luồng phải cho **đúng một luồng thành công**, luồng kia ném ngoại lệ;
và sau đó `da_thanh_toan = SUM(thanh_toan.so_tien)` vẫn đúng.

> ⚠️ **Bắt buộc chạy đối chứng:** bỏ `@Version` ra → test phải **ĐỎ**. Chưa thấy nó đỏ thì
> chưa biết nó có kiểm gì không. Đây là lần thứ mười áp dụng bài học 43.5 của chính dự án.

---

### 🔴 V2. Cột `hanMucTinDung` — dùng nó hoặc bỏ nó — *nửa ngày*

**Vì sao:** cột này **có trong CSDL, có trên form, có hiển thị**, nhưng **không một dòng mã nào
dùng nó để chặn**. Một cột chết là thứ người chấm rất dễ bắt, và bắt được thì mất điểm cả ở
"mô hình dữ liệu" lẫn "chức năng".

**Chọn một trong hai — đừng để nguyên:**

| Cách | Việc | Phù hợp khi |
|---|---|---|
| **A. Cưỡng chế** | Trong `BillingService`, sau khi tính `tong_thanh_toan`, nếu tổng nợ chưa trả của thuê bao vượt `hanMucTinDung` thì ghi cảnh báo và đề xuất tạm ngừng | Còn thời gian, muốn thêm một chức năng thật |
| **B. Khai báo trung thực** | Ghi rõ trong `mo-ta-csdl.md` và trên giao diện: *"hạn mức hiện chỉ lưu để tham khảo, chưa dùng để chặn"* | Sát ngày, không muốn đụng engine |

**Khuyến nghị: cách A**, vì hạn mức tín dụng là nghiệp vụ trả sau kinh điển, làm xong là +1
chức năng thật. Nếu chọn B thì phải nói ra, im lặng là mất điểm.

**Nghiệm thu (cách A):** một test dựng thuê bao có hạn mức 100.000 và nợ 150.000 → xuất hiện
trong danh sách đề xuất tạm ngừng.

---

### 🟠 V3. Bảo mật — bốn việc nhỏ, điểm lấy lại nhiều nhất — *2–3 ngày*

Đây là mặt lấy lại nhiều điểm nhất (**+1,32**) vì trọng số cao mà điểm hiện thấp.

| # | Việc | Ở đâu | Công sức |
|---|---|---|---|
| a | **Màn hình quản lý người dùng** — thêm/sửa/khoá tài khoản | Controller + template mới, `/quan-tri/nguoi-dung` (luật chặn đã có sẵn trong `SecurityConfig`) | 1 ngày |
| b | **Đổi mật khẩu** cho người đang đăng nhập | Form + `BCryptPasswordEncoder` đã có | 0,5 ngày |
| c | **Khoá tài khoản sau 5 lần sai** | Thêm `so_lan_sai`, `khoa_den_luc` vào `nguoi_dung`; `AuthenticationFailureHandler` | 0,5 ngày |
| d | **Hết hạn phiên + security headers** | `application.yml`: `server.servlet.session.timeout: 30m`; `SecurityConfig`: `.headers(h -> h.frameOptions(...).contentSecurityPolicy(...))` | 0,5 ngày |

**Vì sao (a) đáng làm nhất:** hiện thêm một nhân viên mới phải **chạy SQL tay**. Hội đồng hỏi
*"làm sao thêm người dùng"* là câu hỏi gần như chắc chắn có.

**Nghiệm thu:** thêm vào `test-auth.ps1` — sai mật khẩu 5 lần thì lần 6 bị khoá; đổi mật khẩu
xong đăng nhập bằng mật khẩu cũ phải trượt.

---

### 🟠 V4. Vận hành — hai việc rẻ, hiệu quả cao — *1,5 ngày*

| Việc | Vì sao | Công sức |
|---|---|---|
| **Flyway** | Bỏ được vết đen *"đổi cấu trúc bảng là mất sạch dữ liệu"*. Chỉ cần đổi `schema.sql` thành `db/migration/V1__khoi_tao.sql`, `data-mau.sql` thành `V2__du_lieu_mau.sql` | 1 ngày |
| **CI GitHub Actions** | Mỗi lần đẩy mã tự chạy `mvnw test`. Huy hiệu xanh trên README là thứ hội đồng nhìn thấy ngay | 0,5 ngày |

CI cần MySQL — dùng `services: mysql:8.4` trong workflow. Nếu vướng, cho CI chạy **241 test
không cần CSDL** trước (`-Dtest='!Kiem*,!Schema*'`) rồi bổ sung sau; có CI chạy một phần vẫn
hơn không có.

---

### 🟡 V5. Biến hai lời tuyên bố thành bằng chứng đo được — *1 ngày*

Báo cáo đánh giá **khẳng định** hai điều mà chưa đo:

1. *"Nạp cả kỳ vào bộ nhớ là trần quy mô"* → **hãy đo**: sinh 200.000 bản ghi (giới hạn form
   cho phép), chạy tính cước, ghi lại thời gian và bộ nhớ đỉnh. Có số rồi thì mới nói được
   *"gấp 10 lần dữ liệu hiện tại thì mất X giây, Y MB"*.
2. *"VAT chôn cứng"* → đưa `THUE_SUAT_VAT` vào bảng `tham_so_he_thong` hoặc `application.yml`
   (**0,5 ngày**).

**Vì sao đáng làm:** cả đồ án được chấm cao chính vì thói quen *"đo trên dữ liệu thật, không
suy luận suông"*. Để lại một lời tuyên bố chưa đo trong báo cáo là tự phá chuẩn của mình.

⚠️ Đo xong **phải khôi phục CSDL bằng `reset`** — đừng để 200.000 bản ghi thử trong dữ liệu demo.

---

### 🟡 V6. Kiểm khả năng tiếp cận bằng bàn phím — *nửa ngày*

Báo cáo đánh giá tự ghi *"chưa kiểm bằng trình đọc màn hình"*. Làm mức tối thiểu: đi hết một
quy trình (thêm khách hàng → đăng ký thuê bao → ghi nhận thanh toán) **chỉ bằng `Tab` và
`Enter`**, không chạm chuột. Ghi lại chỗ nào kẹt.

Thường sẽ lộ 2–3 lỗi: modal không nhốt tiêu điểm, thứ tự `Tab` sai, nút chỉ có biểu tượng
thiếu `aria-label`. Sửa xong là mặt giao diện lên 100.

---

## 3. Nợ sản phẩm nộp — nặng hơn mọi việc mã

> **Sản phẩm nộp cho giảng viên là BÁO CÁO.** Ba khoản dưới đây không xong thì mã có hoàn hảo
> cũng không cứu được điểm.

### 📄 N1. `PHASE-5-REPORT.md` còn thiếu mục A, B, C, D

Dòng 6 của file vẫn đang ghi nợ. Bốn mục này là **hóa đơn, thanh toán, công nợ, giảm trừ** —
tức gần một nửa nghiệp vụ của hệ thống.

Gạch đầu dòng đã soạn sẵn ở [`PHASE-7-REPORT.md` mục 10](PHASE-7-REPORT.md) — bạn viết theo đó.
**Ước 1–2 ngày.** Đây là việc của bạn, không phải của tôi.

### 📸 N2. 69 ảnh màn hình chưa chụp

[`danh-sach-anh-chup.md`](danh-sach-anh-chup.md) liệt kê 62 ảnh + 3 ảnh của đợt làm lại giao
diện + 3 ảnh của màn hình quản lý người dùng + 1 ảnh màn hình đổi mật khẩu. **Toàn bộ ảnh cũ (nếu có) đã hết dùng được** vì giao diện vừa đổi hết.

⚠️ **Chỉ chụp SAU khi đóng băng mã.** Chụp trước rồi còn sửa giao diện là chụp lại từ đầu.
Ước **1 ngày**.

### ⏰ N3. Bảng tuổi nợ đã qua mốc 13/08/2026 — phải quyết ngay

Hôm nay đã **16/08/2026**. Hạn thanh toán muộn nhất của dữ liệu mẫu là 15/08/2026, nên nhóm
*Trong hạn* rỗng và bảng chỉ còn **4/5 nhóm**. Ảnh minh hoạ "đủ 5 nhóm tuổi nợ" **không chụp
được nữa**.

| Cách | Việc | Rủi ro |
|---|---|---|
| **A. Chấp nhận 4 nhóm** | Ghi một câu trong báo cáo: *"nhóm Trong hạn rỗng vì mọi hóa đơn mẫu đều đã quá hạn tính tới ngày chụp"* | Không rủi ro. Mất một ảnh đẹp |
| **B. Dời hạn thanh toán** | Dời `han_thanh_toan` của ~20 hóa đơn kỳ 7 về tương lai, dump lại `data-van-hanh.sql`, chạy lại **toàn bộ** nghiệm thu | Đụng dữ liệu vận hành sát ngày demo |
| **C. Thêm kỳ 9/2026** | Lập hóa đơn kỳ mới có hạn thanh toán tương lai | Vừa phải, nhưng phá trạng thái "kỳ 8 rỗng để demo" |

**Khuyến nghị: cách A.** Cách B đụng dữ liệu sát ngày demo, đúng lúc không nên đụng gì cả — và
bản thân việc *giải thích được vì sao chỉ còn 4 nhóm* đã là một điểm cộng, vì nó cho thấy bạn
hiểu tính chất phụ thuộc ngày xem của bảng aging.

---

## 4. Lộ trình 8 ngày

Thứ tự đã tính tới rủi ro: **việc đụng mã làm trước, đóng băng, rồi mới chụp ảnh và viết báo cáo.**

| Ngày | Việc | Kết quả |
|---|---|---|
| **1** | V1 khoá lạc quan · V2 hạn mức tín dụng | Hết lỗi đúng đắn, hết cột chết |
| **2–3** | V3 bảo mật (4 việc) | Quản lý người dùng, đổi mật khẩu, khoá tài khoản, hết hạn phiên |
| **4** | V4 Flyway + CI | Nâng cấp CSDL được, có huy hiệu xanh |
| **5** | V5 đo hiệu năng + VAT cấu hình · V6 kiểm bàn phím | Lời tuyên bố thành số đo |
| **6** | **ĐÓNG BĂNG MÃ.** Chạy trọn nghiệm thu: `mvnw test` · 8 script · 3 bất biến · `reset` đối chiếu từng dòng | Bản demo cuối |
| **7** | N2 chụp 65 ảnh · N3 quyết cách xử lý aging | Đủ ảnh cho báo cáo |
| **8** | N1 viết mục A–D · rà lại toàn bộ tài liệu | Báo cáo hoàn chỉnh |

### Đường lui khi thiếu thời gian

| Còn | Làm gì |
|---|---|
| **1 ngày** | V1 (khoá lạc quan) → N3 (quyết aging) → N2 chụp **12 ảnh tối thiểu** đã liệt kê sẵn trong `danh-sach-anh-chup.md` |
| **3 ngày** | Thêm V2 + V3a (quản lý người dùng) + N1 |
| **5 ngày** | Thêm V4 (Flyway + CI) + trọn N2 |

Nếu chỉ có một ngày: **V1 vẫn phải làm**. Một hệ thống tính tiền có lỗi mất tiền là lỗi nặng
hơn mọi thứ thiếu khác cộng lại.

---

## 5. Việc **KHÔNG** nên làm

Nhiều thứ trong báo cáo đánh giá thuộc thang B và C — đưa vào đồ án chỉ tốn thời gian mà
**không lấy thêm được điểm nào ở thang A**:

| Đừng làm | Vì sao |
|---|---|
| Hóa đơn điện tử có mã cơ quan thuế | Rào cản pháp lý, cần nhà cung cấp được cấp phép. Chỉ cần **ghi nhận** trong mục hạn chế |
| REST API, Swagger | Hệ thống cố ý dựng theo lối kết xuất phía máy chủ. Thêm API là thêm một tầng không ai gọi |
| Docker, Kubernetes | README đã chạy được từ bản clone. Docker không thêm điểm ở thang này |
| Tính cước thời gian thực (OCS), thu thập từ tổng đài | Nhiều tháng-người. Ghi vào mục "hướng phát triển" là đủ |
| Cổng tự phục vụ cho khách hàng | Ngoài phạm vi đề tài |
| Đa ngôn ngữ | Đề tài là hệ thống tiếng Việt |

**Cách xử lý đúng với chúng:** một mục *"Hạn chế và hướng phát triển"* trong báo cáo cuối,
nói rõ **biết là thiếu** và **thiếu vì sao**. Biết mình thiếu gì được chấm cao hơn là im lặng.

---

## 6. Nghiệm thu cuối — chạy trọn bộ ở ngày 6

| # | Tiêu chí | Ngưỡng |
|---|---|---|
| 1 | `mvnw test` | ≥ 307, 0 lỗi *(277 lúc lập kế hoạch + đồng thời + hạn mức + quản lý người dùng + đổi mật khẩu)* |
| 2 | 8 script giao diện | ≥ 215, 0 sai |
| 3 | `python scripts/kiem-tu-ngu.py` | 0 |
| 4 | `python scripts/kiem-giao-dien.py` | 0 |
| 5 | Ba bất biến (sổ cái · thanh toán · điều hướng) | 0 lệch |
| 6 | **Bất biến thanh toán dưới tải đồng thời** | 0 lệch — *tiêu chí mới của đợt này* |
| 7 | `reset` chạy hai lần | giống hệt từng dòng |
| 8 | Clone kho về thư mục mới, làm theo README | chạy được |
| 9 | Kỳ 8/2026 rỗng · kỳ 6, 7 giữ 0 thanh toán | ✓ |
| 10 | 69 ảnh chụp trên bản mã đã đóng băng | đủ |

Tiêu chí **6** là cái mới và là cái đáng giá nhất: cho tới hôm nay, bất biến trung tâm của đồ
án mới chỉ được chứng minh **khi có một người dùng tại một thời điểm**.

---

## 7. Rủi ro

| Rủi ro | Phòng |
|---|---|
| Sửa mã sát ngày demo làm hỏng thứ đang chạy | Đóng băng mã ở ngày 6; sau đó chỉ sửa tài liệu |
| Flyway đổi cách nạp CSDL, có thể vỡ `reset` | Làm ở ngày 4, còn 2 ngày đệm. Vỡ thì bỏ Flyway, mất 1,25 điểm chứ không mất bản demo |
| CI đỏ vì thiếu MySQL | Cho CI chạy trước 241 test không cần CSDL |
| Chụp ảnh xong lại sửa giao diện | Không sửa giao diện sau ngày 6, kể cả "sửa tí cho đẹp" |
| Đo hiệu năng 200.000 bản ghi làm bẩn dữ liệu demo | Chạy `reset` ngay sau khi đo, đối chiếu lại từng dòng |
| Hết thời gian ở ngày 8 | N1 (mục A–D) là việc dài nhất — bắt đầu **song song** từ ngày 1, đừng để tới ngày 8 |

---

## 7bis. ⭐ TIẾN ĐỘ — đọc mục này trước khi làm tiếp

> Cập nhật sau mỗi việc. Đây là **nguồn sự thật duy nhất** về việc nào đã xong; đừng suy ra
> từ lịch sử chat, vì chat sẽ bị tóm tắt và mất chi tiết.

| Việc | Trạng thái | Commit |
|---|---|---|
| **V1** khoá lạc quan cho hóa đơn | ✅ xong | `f10ffde`, `388a67a` |
| **V2** cột `hanMucTinDung` hết là cột chết | ✅ xong | `bf3c423` |
| **V3d** phiên + security headers | ✅ xong | `fa8945a` |
| **V3c** khoá tài khoản sau 5 lần sai | ✅ xong | `fa8945a` |
| **V3a** màn hình quản lý người dùng | ✅ xong | `d71eee5` |
| **V3b** đổi mật khẩu | ✅ xong | `8f41400` |
| **V4** Flyway + CI | ✅ xong (CI chưa chạy được tới khi đẩy mã) | `73fab44` |
| **V5** đo hiệu năng + VAT ra cấu hình | ⬜ **việc tiếp theo** | — |
| **V6** kiểm bàn phím | ⬜ chưa làm | — |
| **N1** báo cáo mục A | ✅ xong | `0c7d93f` |
| **N1** báo cáo mục B, C, D | ⬜ chưa làm | — |
| **N2** 69 ảnh chụp | ⬜ chưa làm — **chỉ chụp sau khi đóng băng mã** | — |
| **N3** quyết cách xử lý bảng tuổi nợ | ⬜ chưa quyết | — |

### Ghi chú của V3a — hai thứ kế hoạch không nói tới

**1. Khoá tài khoản phải đá được phiên đang mở.** Kế hoạch chỉ viết *"thêm/sửa/khoá tài
khoản"*. Nhưng một nút Khoá chỉ đặt `trang_thai = 0` thì **chỉ có tác dụng từ lần đăng nhập
sau** — người vừa bị khoá vẫn ngồi thao tác tiếp tới hết ca. Đó là khoá trên giấy, và nó phá
đúng lý do người ta bấm nút đó: có người vừa nghỉ việc.

Cách xử lý: khai tường minh `SessionRegistry` + `HttpSessionEventPublisher` trong
`SecurityConfig` (trước đây `maximumSessions(1)` dùng một sổ nội bộ **không ai lấy ra được**),
rồi `NguoiDungServiceImpl` đánh dấu hết hạn mọi phiên của người bị khoá. Dựng lại được bằng
`test-auth.ps1` mục 9.5: một phiên thật đang mở, khoá từ phiên khác, phiên kia bị đưa về trang
đăng nhập ngay yêu cầu kế tiếp.

**2. Bất biến "luôn còn ít nhất một quản trị viên đang hoạt động".** Đây là bất biến duy nhất
của phân hệ này mà vi phạm thì **không sửa được bằng chính phần mềm**: khoá nốt quản trị viên
cuối cùng, hoặc hạ quyền người đó, là không còn ai mở được màn hình quản lý người dùng — kể cả
người vừa gây ra. Đường ra duy nhất khi đó là mở CSDL lên gõ SQL tay, tức đúng thứ việc V3a
sinh ra để không phải làm nữa. Cùng nhóm với nó: không tự khoá và không tự đổi quyền của chính
mình.

**Đối chứng đã chạy** (bài học 43.5, lần thứ mười một): phá ba chốt chặn — bỏ `expireNow()`,
đổi `conLai <= 1` thành `<= 0`, vô hiệu hoá phép kiểm tự khoá — thì **đúng 4 phép kiểm dự đoán
trước** chuyển đỏ và không phép kiểm nào khác. Khôi phục thì 16/16 xanh lại.

**Nợ để lại:** `test-auth.ps1` mục 9 tạo tài khoản `kiemthu01` và để nó ở trạng thái **đã
khoá** (chạy lại nhiều lần vẫn ra đúng trạng thái đó). Sau `reset` thì tài khoản này biến mất,
nên **chạy script trước, chụp ảnh sau** nếu muốn ảnh #66 có đủ ba huy hiệu tình trạng.

### Ghi chú của V3b

**Nghiệm thu của mục V3 nay đã đủ cả hai vế.** Câu nghiệm thu trong kế hoạch là *"sai mật khẩu 5
lần thì lần 6 bị khoá; đổi mật khẩu xong đăng nhập bằng mật khẩu cũ phải trượt"*. Vế đầu thuộc
V3c — làm từ đợt trước nhưng **phép kiểm chưa bao giờ được viết ra**; nay trả nốt ở
`test-auth.ps1` mục 11.

Phép kiểm đáng giá nhất của mục 11 là bước **đối chứng**: đang trong thời gian khoá tạm thì
**mật khẩu ĐÚNG cũng không vào được**. Chỉ kiểm "nhập sai bị từ chối" thì không phân biệt được
với hành vi bình thường của một hệ thống *không hề có* khoá tạm — đúng loại phép kiểm xanh mà
chẳng chứng minh gì (bài học 43.5).

**Ba chốt chặn của V3b, xếp theo mức quan trọng:**

1. **Phải nhập đúng mật khẩu hiện tại.** Một phiên đang mở không chứng minh người ngồi trước máy
   là chủ tài khoản — quầy giao dịch là chỗ máy để không khoá màn hình cả ngày. Thiếu phép kiểm
   này thì ai đi ngang một máy bỏ trống cũng chiếm hẳn được tài khoản.
2. **Đổi xong thì phiên hết giá trị.** Cùng một luật với nút Khoá của V3a, chỉ khác đường vào:
   *thông tin xác thực vừa đổi thì phiên dựng trên thông tin cũ không còn giá trị*. Người ta đổi
   mật khẩu **vì** nghi có người biết mật khẩu cũ — giữ nguyên phiên là để kẻ đó ngồi lại trong
   hệ thống.
3. **Quản trị viên đặt lại mật khẩu hộ ai thì phiên người đó cũng bị đá.** Đây là chỗ V3a còn
   hở: lý do đặt lại mật khẩu hộ gần như luôn là "tài khoản có thể đã lộ", mà bản V3a chỉ đổi
   hash chứ không đuổi ai ra.

**Đối chứng đã chạy:** phá cả ba chốt chặn → **đúng 3 phép kiểm dự đoán trước** chuyển đỏ
(`saiMatKhauHienTaiThiBiChan`, `doiXongThiPhienDangMoHetGiaTri`,
`quanTriDatLaiMatKhauThiPhienNguoiDoBiDa`), không phép kiểm nào khác. Khôi phục thì 25/25 xanh.

Kèm một phép kiểm ngược chiều (số 25): **sửa họ tên mà không đổi mật khẩu thì không đá phiên
ai** — thiếu nó thì chốt chặn số 3 vẫn xanh ngay cả khi mọi lần sửa tài khoản đều đá văng người
đang dùng ra ngoài.

### Ghi chú của V4 — chỗ kế hoạch nói một câu mà thực tế cần một quyết định

Kế hoạch viết: *"Chỉ cần đổi `schema.sql` thành `db/migration/V1__khoi_tao.sql`,
`data-mau.sql` thành `V2__du_lieu_mau.sql`"*. Làm đúng câu đó thì hỏng **hai chỗ**.

**1. Đưa dữ liệu mẫu vào thư mục di trú là tự khoá tay mình.** Flyway lưu checksum từng file đã
chạy. `data-van-hanh.sql` là **bản dump được sinh lại** mỗi khi trạng thái vận hành đổi (Phase 5
mục F đã dump lại một lần) — biến nó thành file di trú nghĩa là mỗi lần dump lại, **mọi CSDL
đang chạy từ chối khởi động** với *"Migration checksum mismatch"*. Đổi lấy một quy trình mà dự
án đang dựa vào, để lấy về một dòng cấu hình ngắn hơn.

> **Cách đã làm:** Flyway giữ **cấu trúc**; dữ liệu mẫu nạp bằng đường riêng trong
> `FlywayResetConfig`, chỉ ở profile `reset`. Cũng bỏ luôn `spring.sql.init` — tài liệu Spring
> Boot khuyến cáo không dùng chung với Flyway và nói rõ sẽ bỏ hỗ trợ.

**2. Lấy schema *hiện tại* làm `V1` thì Flyway thành thứ khai mà không dùng** — có thư mục di
trú nhưng chưa từng di trú cái gì, đúng loại điểm trừ mà mục 1 của kế hoạch này nói tới.

> **Cách đã làm:** `V1__khoi_tao.sql` là cấu trúc **trước** đợt hoàn thiện; ba cột thêm trong
> đợt này thành `V2__khoa_lac_quan_va_chong_do_mat_khau.sql`. Lịch sử di trú **chính là** lịch
> sử dự án, và có một bước di trú thật để chỉ vào.

**Bốn phép kiểm đã chạy, không phép kiểm nào là suy luận:**

| Phép kiểm | Kết quả |
|---|---|
| CSDL do Flyway dựng vs CSDL do cơ chế cũ dựng | **0 dòng lệch** / 20.519 dòng |
| CSDL nháp có `V1` + 1 dòng dữ liệu → chạy `V2` lên | Dòng dữ liệu **còn nguyên**, 3 cột mới đã có |
| `mvnw test` (gồm `SchemaValidationTest` đối chiếu 15 Entity với cấu trúc do di trú dựng) | 307/307 |
| Chạy thường sau khi đã di trú | *"Schema is up to date. No migration necessary"* — không đụng dữ liệu |

**Việc dọn kèm theo:** bỏ `spring.sql.init.mode=never` khỏi 11 lớp test — thay đổi này làm nó
thành cấu hình chết, và cấu hình chết thì lần sau có người đọc sẽ tin là nó đang có tác dụng.

**⚠️ CI chưa xác minh được.** `.github/workflows/test.yml` đã viết (MySQL 8.4 service, nạp dữ
liệu bằng đúng đường `reset` mà người dùng thật đi, rồi chạy cả 307 test), nhưng **chỉ chạy khi
đẩy mã lên GitHub**. Huy hiệu trên README xám cho tới lần đẩy đầu tiên. Nếu CI đỏ, đường lui đã
ghi trong kế hoạch: cho chạy trước bộ test không cần CSDL.

### Việc phát sinh ngoài kế hoạch, đã làm

Hai lỗ hổng do bản quét bảo mật tìm ra, cả hai đều **tự dựng lại được** trước khi vá:

| Lỗ hổng | Đã làm gì |
|---|---|
| `X-Forwarded-For` do client tự đặt, ghi vào cột 45 ký tự trong cùng giao dịch với nghiệp vụ → một header 48 ký tự phá được **mọi** đường ghi | Bỏ hẳn nhánh header, chỉ dùng `getRemoteAddr()` (`fa8945a`) |
| `/bao-cao/**` mở cả cụm → `nhanvien01` bị 403 ở `/cong-no` nhưng xem được `/bao-cao/cong-no` kèm tên khách và số tiền nợ | Phân quyền theo nội dung + bọc menu `sec:authorize` (`8a256a6`) |

### ✅ Nợ kỹ thuật của đợt này — ĐÃ TRẢ

Khoản nợ ghi ở đây là: `schema.sql` đã đổi hai lần (`hoa_don.phien_ban`, rồi
`nguoi_dung.so_lan_sai` + `khoa_den_luc`) mà cả hai mới chỉ `ALTER TABLE` vào CSDL đang chạy,
và **chưa chạy `reset` hai lần đối chiếu từng dòng** — tiêu chí nghiệm thu số 7.

Đã chạy, trước khi đụng tới Flyway (cố ý: nếu sau đó có gì vỡ thì biết chắc là do Flyway):

| Phép kiểm | Kết quả |
|---|---|
| `reset` chạy hai lần, dump so từng dòng | **0 dòng lệch** trên 20.519 dòng |
| Số liệu sau `reset` so với con số báo cáo mô tả | **15/15 khớp** — kể cả doanh thu 111.513.012 đ, đã thu 49.190.687 đ, còn nợ 62.322.325 đ |
| `mvnw test` trên CSDL vừa reset | 307/307 xanh |

Phép kiểm thứ hai là phép kiểm đáng giá: *"hai lần reset giống nhau"* một mình nó vẫn xanh khi
cả hai lần cùng dựng ra một bộ dữ liệu **sai giống hệt nhau**.

`data-mau.sql` và `data-van-hanh.sql` **không cần dump lại**: hai file đó tái lập đúng bộ dữ
liệu mà báo cáo mô tả.

> ⚠️ Lưu ý cho ngày chụp ảnh: `reset` xoá tài khoản `kiemthu01` do `test-auth.ps1` tạo. Muốn
> ảnh #66 có đủ ba huy hiệu tình trạng thì chạy script **trước**, chụp **sau**.

### Số liệu hiện tại

| | Trước đợt | Bây giờ |
|---|---:|---:|
| `mvnw test` | 277 | **307** |
| Phép kiểm giao diện | 177 | **215** |
| Lớp test cần MySQL | 8 | 11 |
| Ảnh cần chụp | 65 | **69** |

---

## 8. Sau kế hoạch này thì được bao nhiêu

| Thang | Trước | Sau |
|---|---:|---:|
| **A. Đồ án môn học** | 93% | **≈ 99–100%** |
| B. Phần mềm nội bộ dùng thật | 64% | ≈ 75% |
| C. BSS thật của nhà mạng | 12% | ≈ 13% |

Thang B lên 75% là **phần thưởng kèm theo**, không phải mục tiêu — khoá lạc quan, Flyway,
quản lý người dùng và CI đều là thứ một sản phẩm thật cần. Thang C gần như không đổi, và
điều đó là bình thường: khoảng cách ở đó là khoảng cách về **bản chất hệ thống**, không phải
về số việc còn phải làm.
