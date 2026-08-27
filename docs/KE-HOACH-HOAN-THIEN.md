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

### 📸 N2. 70 ảnh màn hình chưa chụp

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
| **7** | N2 chụp 70 ảnh · N3 quyết cách xử lý aging | Đủ ảnh cho báo cáo |
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
| 1 | `mvnw test` | ≥ 315, 0 lỗi *(277 lúc lập kế hoạch + đồng thời + hạn mức + quản lý người dùng + đổi mật khẩu)* |
| 2 | 8 script giao diện | ≥ 215, 0 sai |
| 3 | `python scripts/kiem-tu-ngu.py` | 0 |
| 4 | `python scripts/kiem-giao-dien.py` | 0 |
| 4b | `python scripts/kiem-ban-phim.py` | 0 — *tiêu chí mới của việc V6* |
| 5 | Ba bất biến (sổ cái · thanh toán · điều hướng) | 0 lệch |
| 6 | **Bất biến thanh toán dưới tải đồng thời** | 0 lệch — *tiêu chí mới của đợt này* |
| 7 | `reset` chạy hai lần | giống hệt từng dòng |
| 8 | Clone kho về thư mục mới, làm theo README | ✅ **đã chạy** — xem mục 7bis |
| 9 | Kỳ 8/2026 rỗng · kỳ 6, 7 giữ 0 thanh toán | ✓ |
| 10 | 70 ảnh chụp trên bản mã đã đóng băng | đủ |

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
| **V4** Flyway + CI | ✅ xong — **CI đã chạy thật, xanh** | `73fab44`, `42818b8`, `558e6d1` |
| **V5** đo hiệu năng + VAT ra cấu hình | ✅ xong | `eb31aa8` |
| **V6** kiểm bàn phím | ✅ xong | `9ddbe99` |
| **N1** báo cáo mục A | ✅ xong | `0c7d93f` |
| **N1** báo cáo mục B, C, D | ✅ xong | `58d3a2f` |
| **N2** 70 ảnh chụp | ⬜ chưa làm — **chỉ chụp sau khi đóng băng mã** | — |
| **N3** quyết cách xử lý bảng tuổi nợ | ✅ **quyết cách A** — chấp nhận 4 nhóm, giải thích bằng một câu | `71b3520` |
| **G1** làm lại giao diện — hướng "trạm viễn thông" | ✅ xong — **phá quyết định đóng băng mã, có chủ ý** | `ded07b9` |
| **G1b** sửa 5 lỗi chỉ thấy được khi đo trên trình duyệt | ✅ xong | `8cd9b55` |
| **G1c** bỏ dòng "dữ liệu mẫu" khỏi hóa đơn, phiếu thu, Excel | ✅ xong | `960eeab` |
| **G1d** đổi bên phát hành sang công ty hư cấu, bỏ nốt "(GIẢ LẬP)" | ✅ xong | `b21743b` |
| **G2** rà soát tổng thể · sửa 5 mục tồn | ✅ xong | `9d5df48` |
| **G2b** dọn 3 lớp CSS mồ côi và 1 biến chết | ✅ xong | `e64a27e` |
| **G3** script khởi động `chay.cmd` | ✅ xong | `f8d60d6` |
| **G4** nâng cấp thị giác ở tầng token và fragment | ✅ xong — xem `docs/G4-REPORT.md` | `623bba7` `6a4108a` `5f8824e` |

### Ghi chú của G1 — làm lại giao diện

**Đây là việc phá quyết định của chính kế hoạch này.** Mục 7 ghi rõ: *"Không sửa giao diện
sau ngày 6, kể cả sửa tí cho đẹp"*, và N2 ghi *"chỉ chụp sau khi đóng băng mã"*. Người làm đồ
án được nêu cái giá trước — phải viết lại phép kiểm giao diện và chạy lại toàn bộ nghiệm thu —
và vẫn chọn làm cả bố cục. Hệ quả bắt buộc: **N2 phải chụp trên bản giao diện này**, ảnh chụp
của bản cũ không dùng lại được dòng nào.

**Đã đổi những gì.** Khung vỏ tách làm hai tầng: một *thanh máy* chạy hết bề ngang mang bốn
con số vận hành (kỳ đang mở, bản ghi chờ tính, hóa đơn quá hạn, tổng còn nợ) — số do
`LayoutAdvice.tinhTrang()` nạp, hiện ở **mọi** màn hình chứ không riêng trang chủ; và một
*rail* dọc bên trái giữ nguyên cách gom menu theo công việc của Phase 8. Chữ giao diện chuyển
sang Be Vietnam Pro, chữ số liệu sang IBM Plex Mono đều cột. Cả hai nạp từ jsDelivr — cùng
origin với Bootstrap nên **không phải nới CSP**.

#### Ba thứ chỉ lòi ra khi đo, không lòi ra khi nhìn

**1. Một lỗi tương phản có sẵn từ trước, nằm ngoài tầm mắt của phép kiểm.** Nhóm *"Quá hạn
31–60 ngày"* trong `NhomTuoiNo` là chữ trắng trên `#fd7e14` — **2,57:1**, dưới ngưỡng AA
4,5:1 khá xa. Nó sống sót qua cả đợt Phase 8 vì `kiem-giao-dien.py` đọc **template**, còn màu
này khai trong **enum Java**. Cả thang màu tuổi nợ nay đo lại từng bậc, bậc thấp nhất 5,85:1.

*Bài học:* phép kiểm tự động chỉ canh được nơi nó nhìn tới. Chuyển một giá trị hiển thị từ
template vào mã Java là lặng lẽ đưa nó ra khỏi tầm canh.

**2. Hai màu tín hiệu của chính bản thiết kế mới cũng trượt.** `--tin-on` làm chữ được 4,44:1
và `--tin-canh` với chữ trắng được 4,24:1 — cả hai *trông* hoàn toàn ổn trên màn hình. Sửa
theo đúng cách từng màu được dùng: `--tin-on` đậm thêm; `--tin-canh` **giữ nguyên độ đậm** vì
nó còn làm viền tiêu điểm của đường tắt "Bỏ qua menu" (sáng hơn thì viền đó tụt xuống dưới
ngưỡng 3:1 trên nền trắng), chỉ đổi chữ trên nền cảnh báo từ trắng sang đen.

Đèn tín hiệu trên thanh máy phải có **bộ màu riêng**: ba màu trên chọn để đọc trên nền trắng,
đặt lên nền mực chúng chỉ còn 2,3–2,9:1, tức ba chấm xám. Cùng ý nghĩa, khác nền, thì khác
trị số.

**3. Bảng màu biểu đồ nằm rải ở 7 file.** Mỗi màn hình có biểu đồ tự gõ bộ mã màu của riêng
nó. Đổi bảng màu lần này quên mất 4 chỗ, và chỉ phát hiện khi đối chiếu trang chủ với phần
còn lại — trang chủ đã sang màu mới còn bốn báo cáo vẫn xanh Bootstrap. Nay biểu đồ đọc qua
`window.MAU` khai trong `app.js`, lấy thẳng từ biến CSS: bảng màu chỉ còn **một** nguồn là
`:root` của `app.css`.

#### Một phép kiểm suýt chết lặng

Đổi tên lớp `sidebar-brand` → `thanh-may-hieu` làm hai khẳng định trong `test-auth.ps1` mất
neo. Một cái **đỏ ngay** (trang 403 phải còn khung vỏ). Cái kia nguy hiểm hơn nhiều: nó khẳng
định trang đăng nhập **không** chứa chuỗi đó — chuỗi không còn tồn tại ở đâu nữa thì nó xanh
vĩnh viễn mà chẳng canh gì. Đúng chuẩn làm việc số 5.

Đối chứng chạy sau khi sửa, dự đoán công bố trước: đỏ **đúng 1** phép kiểm. Lần đối chứng đầu
**không đỏ** — vì tôi phá bằng cách đổi thành `thanh-may-hieu-DOICHUNG`, mà `.Contains()` vẫn
khớp chuỗi con. Phá lại bằng một tên không chứa chuỗi cũ thì đỏ đúng một phép kiểm đã nêu tên.
*Một đối chứng dựng sai cũng cho cảm giác an toàn giả y hệt một phép kiểm sai.*

#### Soát lại trên trình duyệt — năm lỗi mà phép kiểm tĩnh không thể thấy

Cả `kiem-giao-dien.py`, `kiem-ban-phim.py` lẫn 215 phép kiểm HTTP đều **xanh** ở bản đầu của
G1, vì cả ba đều đọc **mã nguồn hoặc chuỗi HTML**. Không cái nào biết trình duyệt cuối cùng
dựng ra hình gì. Đo trên DOM đã dựng xong thì lòi ra năm lỗi:

| Lỗi | Đo được | Vì sao phép kiểm tĩnh mù |
|---|---|---|
| Thanh máy và rail cuộn mất | Cuộn 900px → thanh máy ở `y=-900` | `position` là thuộc tính tính ra lúc dựng hình |
| Nhãn bốn con số quá nhỏ | **9,92px** (`.62rem` viết hoa) | `rem` trong CSS không nói ra pixel cuối cùng |
| Tiêu đề nhóm menu, vai trò | 10,24px và 10,88px | như trên |
| Cuộn ngang ở 375px | `scrollWidth` 430 / khung 375 | tràn là kết quả của bố cục, không có trong mã |
| Nút mở rail hụt vùng bấm | 32×40, chuẩn là 40×40 | kích thước thật do nội dung quyết định |

Lỗi đầu tiên nặng nhất **không phải vì nó xấu** mà vì nó làm chính lý lẽ dựng nên thanh máy
thành sai: tôi đã viết "bốn con số cần thấy TRONG LÚC đang làm việc khác", trong khi thực tế
chỉ thấy được lúc đang ở đầu trang — mà ở đầu trang thì đã có trang chủ rồi. *Một lời giải
thích nghe xuôi tai không chứng minh được thứ nó giải thích có tồn tại hay không.*

#### Ba lần đo sai trước khi đo đúng

Đáng ghi lại vì cả ba đều cho kết quả **trông như đã đạt**:

1. **Đo cuộn ngay sau khi gọi cuộn.** `scrollTo(900)` rồi đọc `scrollY` lập tức ra `0`, nên
   "thanh máy không xê dịch" — kết luận đúng vì lý do sai. Bootstrap đặt
   `scroll-behavior: smooth`, cuộn còn đang chạy. Phải chờ rồi mới đo.
2. **Dò `prefers-reduced-motion` bằng cách duyệt `document.styleSheets`** → báo "không có".
   Thật ra **8/9 stylesheet là CDN nên `.cssRules` bị chặn cross-origin**, và tôi nuốt lỗi đó
   trong `try/catch`. Bootstrap có sẵn quy tắc ấy. Âm tính giả thuần tuý.
3. **Kết luận biểu đồ không vẽ** vì canvas trả về 0 pixel. Thật ra khung xem không compositing
   nên `requestAnimationFrame` không chạy, mà Chart.js vẽ qua đó. Gọi thẳng `chart.draw()` thì
   ra 8811 pixel đúng bảng màu mới. Cùng gốc với việc canvas ở 375px vẫn giữ bề ngang 956px.

Bài học chung: **khi phép đo chạy trong một môi trường thiếu một cơ chế, thứ thiếu đó trông
giống hệt một lỗi của sản phẩm.** Mỗi lần như vậy đều phải dựng đối chứng tách hai khả năng ra
trước khi ghi vào danh sách lỗi.

#### Một lỗi tự tạo ra trong lúc sửa

Bản vá đầu cho lỗi "thanh máy cuộn mất" ghi chiều cao đo được vào `--thanh-may-cao` — đúng
biến mà `.thanh-may` đọc làm `min-height`. Vòng phản hồi: lúc khung tụt về 0×0, thanh máy xuống
dòng thành 375px, JavaScript ghi 375px vào biến, và min-height khoá cứng ở đó. Một cái bánh
cóc — chỉ tăng, không bao giờ giảm. Tách thành hai biến (`--thanh-may-cao` vào,
`--thanh-may-thuc` ra) là hết đường trôi.

#### Nghiệm thu sau khi đổi

315/315 test · 215/215 phép kiểm HTTP · `kiem-tu-ngu` 46 file · `kiem-giao-dien` 38 màn hình ·
`kiem-ban-phim` 213 nút/liên kết. Mọi cặp chữ–nền đều đo lại, không cặp nào dưới ngưỡng.

---

### Ghi chú của G1c — bỏ dòng "dữ liệu mẫu" khỏi chứng từ

**Không phải phá quyết định cũ mà làm nốt một quyết định cũ.** `PHASE-8-REPORT.md` mục 283 và
287 đã thay chân trang màn hình *"Đồ án… dữ liệu mẫu phục vụ học tập"* thành *"Hệ thống quản
lý thuê bao & tính cước — phiên bản 1.0"*, và đầu trang bản in báo cáo thành *"Ngày in: …"*,
với lý do ghi ở dòng 308: *"Chúng là thông tin phụ, không nên tranh chỗ với dữ liệu."* Hóa đơn
PDF, phiếu thu và Excel là ba chỗ Phase 8 chưa với tới.

Dòng đó nằm ở **năm** chỗ chứ không phải một — tìm được một chỗ rồi dừng là sót bốn:

| Nơi | Trước | Sau |
|---|---|---|
| PDF hóa đơn — băng ngang nền xám | *DỮ LIỆU MẪU TỰ SINH — … KHÔNG PHẢI HÓA ĐƠN THẬT* | bỏ hẳn |
| PDF hóa đơn — chân trang | *Hóa đơn mẫu phục vụ mục đích học tập* | `Hóa đơn HD202607-000058` |
| PDF phiếu thu — chân trang | *Phiếu thu mẫu phục vụ mục đích học tập* | `Phiếu thu TT20260620-0004` |
| Màn hình chi tiết hóa đơn | huy hiệu vàng | bỏ hẳn |
| File Excel — chân trang | *Dữ liệu mẫu phục vụ mục đích học tập — Đồ án…* | bỏ hẳn |

**Thay chứ không chỉ xoá.** Chân trang PDF để trống là phí một chỗ có ích: hóa đơn có thể dài
hơn một trang, và một tờ rời khỏi tập thì số hiệu ở chân trang là thứ duy nhất nói nó thuộc về
đâu. Excel thì không cần thay vì đã có sẵn *"Ngày xuất: dd/MM/yyyy HH:mm"* ở đầu file.

**Một thứ giữ lại có chủ ý: chữ "(GIẢ LẬP)" cạnh tên công ty.** Sau đợt này, tờ hóa đơn mang
tên một nhà mạng có thật, mã số thuế và địa chỉ trông như thật — mà hóa đơn điện thoại ở Việt
Nam hay được dùng làm giấy chứng minh nơi cư trú. Chữ ấy là dấu nhận biết duy nhất còn lại.

> **Đợt G1d đã bỏ nốt chữ này** bằng một cách gọn hơn hẳn — xem ghi chú G1d ngay dưới.

**Cũng giữ lại: câu cảnh báo trên màn hình sinh dữ liệu CDR** (`cdr/sinh-du-lieu.html`). Đó
không phải dấu trên chứng từ mà là lời cảnh báo về việc một cái nút sắp làm gì — người bấm cần
biết nó tạo ra cuộc gọi bịa, không phải cuộc gọi thật của khách.

**Ba phép kiểm cũ suýt thành phép kiểm chết.** Chúng khẳng định PDF *có chứa* hằng số
`CHAN_TRANG`. Nếu tôi chỉ đổi hằng số đó thành chuỗi rỗng thì `contains("")` luôn đúng — ba
phép kiểm xanh vĩnh viễn mà không canh gì. Nay viết lại thành ba khẳng định có thể đỏ: chân
trang **có** số hiệu, văn bản **không** còn dòng cũ, và **còn** chữ "(GIẢ LẬP)".

Đối chứng, dự đoán công bố trước: gỡ "(GIẢ LẬP)" khỏi cả hai service thì đỏ **đúng 3** phép
kiểm đã nêu tên. Chạy ra đúng 3 — `Tests run: 17, Failures: 3`.

Nghiệm thu: 315/315 test · 215/215 phép kiểm HTTP · 3 phép kiểm Python đạt. Xuất PDF thật qua
HTTP rồi trích văn bản bằng OpenPDF để soi, không dựa vào mã nguồn: cả hai tờ đều sạch dòng cũ
và đều còn "(GIẢ LẬP)".

---

### Ghi chú của G3 — script khởi động

**Đo trước khi tối ưu.** Bấm giờ từ lúc gõ lệnh tới lúc `/dang-nhap` trả về 200 — không dùng
con số `Started BillingApplication` mà JVM tự báo, vì nó bỏ qua phần Maven ở đầu. Chạy xen kẽ
3 lượt mỗi cách để triệt tiêu trôi do máy bận:

| Cách chạy | Thời gian | Đánh đổi |
|---|---|---|
| `mvnw spring-boot:run` | ~9,8 giây | có DevTools, tự biên dịch lại |
| `java -jar` | ~5,9 giây | phải đóng gói lại (8 giây) sau khi sửa mã |
| `java -jar` + CDS | ~4,9 giây | thêm kho **96 MB**, phải dựng lại sau mỗi lần đóng gói |

**CDS bị loại dù nhanh nhất.** Nó mua thêm 1 giây bằng 96 MB và một bước dựng nữa — với một
đồ án thì đó là cái giá sai. Ghi lại ở đây để lần sau khỏi đo lại.

**Một giả thuyết bị bác bỏ:** `mvnw spring-boot:run` truyền cờ `-XX:TieredStopAtLevel=1`, và
JVM của nó khởi động nhanh hơn `java -jar` (4,5s so với 5,7s), nên tôi đoán áp cờ đó cho jar
sẽ nhanh hơn. Đo ra **chậm hơn**: 6,9 giây so với 5,9 giây. Cờ đó bị bỏ.

#### Cái thật sự làm mất thời gian không phải mấy giây đó

Là **cổng 8080 còn bận** vì một bản chạy cũ chưa tắt. Spring Boot báo *Port 8080 was already
in use* rồi dừng hẳn, và thông báo đó không nói ai đang giữ cổng. Sáng nay chuyện này đã xảy
ra thật một lần. `chay.ps1` kiểm cổng **trước tiên**, in ra số hiệu tiến trình, tên và giờ nó
bắt đầu chạy, rồi hỏi có dừng không.

#### Ba chế độ

| Lệnh | Làm gì | Đo được |
|---|---|---|
| `chay` | Phát triển — biên dịch lại, có DevTools | ~8–10 giây |
| `chay demo` | Chạy từ bản đóng gói, **không** biên dịch lại | **~6,2 giây** |
| `chay reset` | Xoá sạch CSDL rồi nạp lại dữ liệu mẫu | hỏi xác nhận trước |

`demo` **không tự đóng gói lại** khi jar cũ hơn `src/` — nó chỉ nói rõ file nào mới hơn. Lý
do: đóng gói 8 giây cộng chạy 6 giây là 14 giây, chậm hơn cả chế độ phát triển. Người dùng tự
chọn, nhưng phải biết mình đang chạy bản cũ.

Câu xác nhận của `reset` đặt **trước** bảng thông tin, không phải sau: một cảnh báo phá huỷ
nằm dưới một khối chào thân thiện thì rất dễ bấm cho qua.

#### Đã thử từng nhánh, không chỉ nhánh thuận

| Nhánh | Kết quả |
|---|---|
| `reset` trả lời "k" | in *Đã huỷ*, thoát 0 — **280 hóa đơn còn nguyên** |
| Cổng bận, trả lời "k" | giữ tiến trình cũ, không giết nhầm |
| Cổng bận, trả lời "c" | thay PID 33388 bằng 33112, phục vụ được sau 8,1 giây |
| jar cũ hơn `src/` | cảnh báo đúng tên file và giờ sửa |

#### Ba lỗi thoát ký tự khi viết script

Sinh file `.ps1` qua Python rồi qua JSON làm `` trong `targetilling` thành **ký tự
backspace 0x08**, và `	` trong `	est-dieu-huong` thành **Tab**. PowerShell báo *Illegal
characters in path*. Cách chữa: viết file bằng heredoc trích dẫn của shell, không qua tầng
thoát nào, rồi mới chuyển sang UTF-8 có BOM bằng PowerShell.

Cũng ở đợt này, phép kiểm cổng `netstat | grep "LISTENING.*:8080"` **luôn báo trống** vì
`netstat` in `:8080` *trước* chữ `LISTENING`. Nó làm một phép đo khởi động cho ra 124 ms —
con số vô lý mà suýt thì tin. Đây là lần thứ tư trong đợt hoàn thiện một phép kiểm viết vội
cho kết quả trông như đã đạt.

---

### Ghi chú của G2 — rà soát tổng thể

**Phần nghiệp vụ không có lỗi nào.** Truy vấn thẳng MySQL: cả 10 con số `CLAUDE.md` ghi đều
khớp CSDL (6 kỳ · 18.723 CDR · 280 hóa đơn · 620 chi tiết · 161 thanh toán · 34 biến động ·
2 giảm trừ · doanh thu 111.513.012 · đã thu 49.190.687 · còn nợ 62.322.325). Bốn bất biến
**0 lệch**: `con_no = tổng − đã thu`, `đã thu = SUM(thanh_toan)`, sổ cái số dư 80 thuê bao,
và `so_du_sau = so_du_truoc ± so_tien` từng dòng. Kỳ 8 rỗng, kỳ 6–7 giữ 0 thanh toán.

Đáng ghi: trước lượt đối chiếu này, 8 script đã chạy vài lượt, có lượt **huỷ rồi lập lại hóa
đơn kỳ 6 và kỳ 8**. Dữ liệu vẫn về đúng con số tài liệu ghi — đó là bằng chứng các script thật
sự idempotent, chứ không phải chỉ tự nhận vậy.

#### Năm mục đã sửa

| Mục | Vấn đề | Sửa |
|---|---|---|
| 1 | `.card-header` **11,5px** — đợt G1 hạ từ `.95rem`, có mặt 54 lần trên 24 màn hình | 14px, bỏ viết hoa |
| 2 | `kich-ban-kiem-thu.md` ghi `test-auth.ps1` có **11** phép kiểm; cả bảng cộng được 177 | đo lại: 42, tổng **215** |
| 3 | Ba tài liệu người ta *làm theo* vẫn nói "sidebar"; `admin` ghi 13 mục menu | đổi sang "rail"; đo lại: **14 mục** |
| 4 | `danh-sach-anh-chup.md` #51 ghi "269 test", #56 ghi "13 đạt" | 315 và 15 |
| 5 | Số ảnh chụp: kế hoạch ghi 70, một chỗ ghi 65 | thống nhất **70** |

Kèm theo: nâng 5 khai báo còn ở 12,5–12,8px lên 13px cho luật *"không có chữ giao diện thường
trực nào dưới 13px"* thành đúng, và chặn cộng dồn `.small` trong `.card-header` (0.875em lồng
trong 14px ra 12,3px).

#### Bốn lần phép kiểm của tôi báo động giả

Cùng loại với ba lần ở G1b, nên ghi lại thành một chỗ:

1. Báo `.so-lieu` được dùng 9 lần → cả 9 đều là `dai-/o-/the-so-lieu`; `` trong grep khớp cả
   sau dấu gạch nối. Lớp đó **đúng là mồ côi**, phép kiểm chứng mới sai.
2. Báo `--tin-canh` tụt tương phản → tôi đo cặp *chữ trắng trên nền cam*, mà chú thích ngay
   trong `app.css` đã ghi màu này **luôn đi với chữ đen**. Cặp tôi đo không tồn tại.
3. Báo 25 biến `--bs-*` là mã chết → đó chính là biến của Bootstrap; tôi chỉ quét trong
   `app.css` nên không thấy nơi tiêu thụ.
4. Báo danh sách ảnh chỉ có **62** mục → thật ra đủ **70**; các mục 63–70 nằm trong khối trích
   dẫn `> |` nên biểu thức `^\| [0-9]+ \|` bỏ sót.

Bài học chung, đúng chuẩn làm việc số 5: **phép kiểm viết vội để xác minh một phát hiện cũng
là một phép kiểm** — nó sai thì hậu quả y hệt.

#### Mục 6 — dọn 3 lớp CSS mồ côi và 1 biến chết

Làm sau, khi người làm đồ án hỏi tới. Trước khi xoá có tra nguồn gốc từng cái bằng
`git show ded07b9^`, vì "rác của chính mình" và "mã chết có sẵn" là hai chuyện khác nhau:

| | Có trước G1? | Kết luận |
|---|---|---|
| `.nhan-muc` (+ `::after`) | không | đợt G1 tạo ra rồi không dùng — rác của chính đợt đó |
| `--tin-loi-nen` | không | như trên |
| `.o-tim-kiem` | có, và **cũng đã mồ côi từ trước** | mã chết cũ, đợt G1 chép sang |
| `.so-lieu` | có, và **cũng đã mồ côi từ trước** | như trên; chỉ bỏ khỏi nhóm chọn, giữ `.the-so-lieu .gia-tri` |

Bỏ 27 dòng. Sau khi bỏ, ba phép dò đều ra rỗng: **0 lớp mồ côi · 0 biến chết · 0 khai báo
`font-size` dưới 13px** — `.nhan-muc` chính là khai báo 11,5px cuối cùng còn sót.

#### Một thứ tìm ra nhưng CỐ Ý không sửa

`.badge` của Bootstrap là `0.75em`; lồng trong ô bảng 14,4px thì ra **10,8px**. Trên `/cong-no`
có 175 chỗ như vậy, và đó là chữ mang nghĩa — *Quá hạn*, *Đang dùng*, *Trong hạn*.

Không sửa vì hai lẽ: nó là **lỗi có sẵn** (kiểm `git show ded07b9^` — trước đợt G1 cũng vậy),
và nâng lên 13px sẽ đổi mật độ hiển thị của **mọi** bảng trong phần mềm. Đó là quyết định về
hình thức, không phải sửa lỗi, nên để người làm đồ án quyết. Sửa thì chỉ một quy tắc:
`.badge { font-size: .8125rem; }`.

---

### Ghi chú của G1d — đổi bên phát hành sang công ty hư cấu

**Đề xuất đến từ người làm đồ án, và nó đúng hơn cách tôi đang bảo vệ.** Ở G1c tôi giữ chữ
"(GIẢ LẬP)" cạnh tên VNPT vì lo tờ hóa đơn không còn dấu nhận biết nào. Nhưng dán nhãn "giả
lập" lên tên một doanh nghiệp có thật là chữa triệu chứng: tờ giấy vẫn mạo danh họ, chỉ là có
thêm một dòng đính chính. **Đổi hẳn bên phát hành sang một công ty bịa thì vấn đề biến mất chứ
không phải được che đi** — không mạo danh ai thì không có gì để cảnh báo, nên bỏ hết dấu là
hợp lý chứ không phải nhân nhượng.

Tên chọn: **Công ty Cổ phần Viễn thông Sông Hậu**. Sông Hậu chảy qua Ninh Kiều, Cần Thơ nên
khớp với địa chỉ vốn đã có trên chứng từ, và không trùng nhà mạng nào.

**Bắt được thêm một thứ nhờ đổi tên: số tổng đài `1800 1166` trên hóa đơn là tổng đài THẬT của
VNPT.** Nó nằm im ở đó từ Phase 5, qua mọi đợt rà soát, vì không phép kiểm nào biết một chuỗi
số là số thật của ai. Nay đổi thành `1800 6060`, mã số thuế đổi từ `0100000000` sang
`1800000000` — đầu 18 là Cần Thơ, khớp địa chỉ.

**Phép kiểm đổi chiều canh.** Trước: "PDF phải chứa (GIẢ LẬP)". Nay: "PDF phải chứa tên công ty
hư cấu **và không được chứa** VNPT". Chiều phủ định là chiều quan trọng — nó bắt đúng cái sai
thật sự, là chuyện tên một nhà mạng có thật quay lại trên một tờ chứng từ không còn dòng cảnh
báo nào.

Đối chứng, dự đoán nêu trước: đổi bên phát hành ngược về VNPT thì đỏ đúng 3 phép kiểm. Chạy ra
`Tests run: 17, Failures: 3` — mỗi lớp kiểm PDF một cái.

**Không đổi:** tên thư mục `vnpt-billing` và gói Java `com.hanzo.billing`. Đó là tên kỹ thuật
có từ Phase 0, không hiện ra cho người dùng, và đổi thì kéo theo cả lịch sử git lẫn đường dẫn
CI mà chẳng được gì.

Nghiệm thu: 315/315 test · 215/215 phép kiểm HTTP · 3 phép kiểm Python. Xuất PDF thật qua HTTP
rồi trích văn bản: cả hai tờ đều không còn "VNPT", "GIẢ LẬP", dòng dữ liệu mẫu, lẫn số tổng đài
thật.

---

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

**✅ CI đã chạy thật và xanh** — 315 test, 0 lỗi, 2 phút 38 giây trên máy Linux sạch với MySQL
8.4 mới dựng, nạp dữ liệu bằng đúng đường `reset` mà người dùng thật đi. Không phải dùng tới
đường lui "chạy trước bộ test không cần CSDL".

Nhưng **phải đỏ hai lần mới xanh**, và cả hai lần đều đáng ghi lại:

**Lần 1 — `exit code 124`, tức lệnh `timeout` giết sau 300 giây.** Vấn đề lớn hơn con số 300:
bước đó **không in một dòng nào của `reset.log` ra**, nên không chẩn đoán được gì. Tôi đã đoán
là "lần chạy đầu chưa có cache Maven nên tải thư viện lâu" — **đoán sai**. Một bước CI thất bại
mà không nói được vì sao thì gần như vô dụng; đã sửa để nó thoát sớm khi thấy
`APPLICATION FAILED TO START` và in 120 dòng cuối của log trước khi báo lỗi.

**Lần 2 — `exit code 126`: `./mvnw: Permission denied`.** `git` lưu `mvnw` với mode `100644`,
tức **không có bit thực thi**. Trên Windows điều đó không lộ ra vì Git Bash gọi script qua `sh`;
trên Linux thì `./mvnw` chết ngay. Đây cũng là nguyên nhân **thật sự** của lần đỏ thứ nhất —
lúc đó lệnh chạy ở nền (`&`) nên lỗi bị nuốt, và chỉ khi tách bước tải phụ thuộc ra chạy ở tiền
cảnh thì mã lỗi thật mới hiện.

> **Điều này nói một câu đáng giá về tiêu chí nghiệm thu số 8.** Phép kiểm *"clone kho về thư
> mục mới rồi làm theo README"* đã chạy **đạt** — trên Windows. Cùng bản clone đó trên Linux thì
> **không chạy được**. Một phép kiểm chỉ chạy trên một hệ điều hành chỉ đúng trên hệ điều hành
> đó, và CI là thứ duy nhất bắt được điều này. Đúng chuẩn làm việc 43.7 của dự án: *dữ liệu thử
> thiết kế theo một chiều chỉ đúng theo chiều đó.*

⚠️ Kho đang để **PRIVATE**, nên huy hiệu trên README chỉ hiện với người có quyền truy cập; người
ngoài thấy ảnh hỏng. Muốn hội đồng nhìn thấy huy hiệu xanh thì phải chuyển kho sang public —
quyết định đó là của bạn, không phải việc mã.

### Ghi chú của V5

**Phần VAT hoá ra rộng hơn "đưa một hằng số ra cấu hình".** Thuế suất được dùng ở **hai** chỗ
tính (lập hóa đơn, bảng đối soát) nhưng chuỗi `10%` còn được gõ cứng ở **ba** chỗ hiển thị:
màn hình chi tiết hóa đơn, bản PDF, và nhãn dòng thuế của bảng đối soát. Đưa con số ra cấu hình
mà để tờ hóa đơn khách cầm vẫn in "Thuế GTGT (10%)" trong khi hệ thống tính 8% thì **tệ hơn là
không làm** — nên cả năm chỗ nay đi qua `ThamSoNghiepVu.nhanThueSuat()`.

**Đối chứng đã lộ ra hai lỗ hổng trong chính bộ phép kiểm mới viết.** Gõ cứng lại thuế suất ở cả
ba chỗ rồi chạy: **chỉ một** phép kiểm đỏ (nhãn trên bản PDF). Lý do là bộ dữ liệu mẫu vốn dùng
đúng 10%, nên con số cấu hình và con số gõ cứng trùng nhau — không phép kiểm nào phân biệt được.
Đã bổ sung hai phép kiểm đặt thuế suất **8%** để phân biệt: một cho engine lập hóa đơn, một cho
nhãn bảng đối soát. Chạy lại đối chứng: cả ba đều đỏ.

Đây đúng là bài học 43.5 của dự án — *một phép kiểm sai nguy hiểm ngang thiếu phép kiểm* — nhưng
ở dạng ít gặp hơn: phép kiểm **đúng** mà **không phân biệt được** hai trường hợp cần phân biệt.

**Phần đo hiệu năng trả lời đúng câu chưa đo trong báo cáo đánh giá.** Sinh 200.000 bản ghi
(gấp 10,7 lần dữ liệu hiện tại), JVM ghim `-Xmx2g`, lấy mẫu heap mỗi 300 ms:

| Bước | Thời gian | Heap đỉnh |
|---|---:|---:|
| Sinh 200.000 bản ghi | 11,4 giây | 147 MB |
| Tính cước 200.000 bản ghi | 70,4 giây | **412 MB** |
| Lập hóa đơn (58 hóa đơn) | 85,6 giây | **490 MB** |
| **Trọn vòng** | **2 phút 47** | **490 MB / 2 GB** |

Trần quy mô **có thật nhưng xa hơn nhiều** so với câu *"một triệu bản ghi bắt đầu nguy hiểm"*:
ngoại suy tuyến tính cho khoảng 800.000 bản ghi một kỳ ở 2 GB heap. Con số ngoại suy đó đã được
ghi rõ là **ngoại suy chứ không phải số đo** — trong `toi-uu-hieu-nang.md` phần III và trong
`DANH-GIA-HE-THONG.md` mục 5.4.

**Đã khôi phục:** `reset` sau khi đo, rồi đối chiếu bản dump với bản trước lúc đo — **0 dòng
lệch trên 20.519 dòng**. 200.000 bản ghi thử và kỳ 9/2026 không còn dấu vết.

### Ghi chú của V6 — đo bằng bàn phím thật, và một giới hạn phải nói ra

Kế hoạch đoán *"thường sẽ lộ 2–3 lỗi: modal không nhốt tiêu điểm, thứ tự Tab sai, nút chỉ có
biểu tượng thiếu aria-label"*. Đo thật ra **bốn** phát hiện, và **không** phát hiện nào trùng
với dự đoán đầu tiên:

| # | Phát hiện | Đo bằng gì |
|---|---|---|
| 1 | **Phải bấm Tab 20 lần** mới tới ô nhập đầu tiên của màn hình thêm khách hàng — 15 mục sidebar + đăng xuất + 3 mắt xích vệt bánh mì — và trả lại từng ấy lần ở **mọi** trang | Nghe sự kiện `focusin` rồi bấm Tab 26 lần, ghi lại thứ tự thật |
| 2 | **Bấm Esc đóng hộp thoại thì tiêu điểm nằm lại trên nút "Đồng ý" của hộp thoại vừa ẩn đi** — lần Tab kế tiếp bắt đầu lại từ đầu trang | Mở modal bằng bàn phím, bấm Esc, so `document.activeElement` |
| 3 | **7 nút không có tên đọc được nào** (6 nút đóng của Bootstrap + 1 nút xoá dòng bảng giá) | Quét tĩnh 213 nút/liên kết |
| 4 | **12 nút chỉ có `title`** — trình đọc màn hình nhiều cái bỏ qua, người dùng bàn phím không bao giờ thấy | Quét tĩnh |

Thứ tự Tab **đúng** với thứ tự nhìn thấy, và hộp thoại **có** nhốt tiêu điểm — hai thứ kế hoạch
đoán là hỏng thì lại không hỏng. Ngược lại, hai thứ hỏng nặng nhất (số 1 và số 2) không nằm
trong dự đoán. Đây đúng là chuẩn làm việc 43.1: *kiểm bất biến, đừng kiểm luật cụ thể* — đi đo
thật thì thấy cái mình không nghĩ tới.

**Đã sửa cả bốn, và đo lại bằng trình duyệt thật:**

| | Trước | Sau |
|---|---:|---:|
| Số lần Tab tới ô nhập đầu tiên | 20 | **5** |
| Tiêu điểm sau khi Esc đóng hộp thoại | nút đã ẩn | **đúng nút vừa bấm** |
| Nút thiếu tên đọc được | 20 | **0** / 213 |

**Phép kiểm mới `scripts/kiem-ban-phim.py`** canh hai điều: mọi nút có tên đọc được, và đường
tắt bỏ qua menu còn nguyên. Đối chứng đã chạy: trên bản template trước khi sửa nó báo đúng
**21** chỗ, sau khi sửa báo **0**.

> ⚠️ **Một giới hạn phải nói ra.** Trình duyệt tự động hoá trong phiên làm việc gửi phím tổng
> hợp **không kích hoạt hành vi mặc định của trình duyệt**: Tab chuyển được tiêu điểm, Escape
> tới được trình nghe của Bootstrap, nhưng **Enter không gửi được biểu mẫu và không bấm được
> nút**. Đã kiểm chứng điều này là giới hạn công cụ chứ không phải lỗi ứng dụng: trang đăng
> nhập không nạp một dòng JavaScript nào — biểu mẫu HTML thuần với `<button type="submit">` —
> mà Enter trong ô mật khẩu vẫn không kích hoạt gửi.
>
> Nghĩa là **phần "Enter kích hoạt" của V6 chưa được kiểm bằng máy**. Việc còn lại là một lượt
> đi tay 3 phút: đăng nhập → thêm khách hàng → đăng ký thuê bao → ghi nhận thanh toán, chỉ bằng
> `Tab` và `Enter`. Về mặt mã đánh dấu thì không có gì cản: mọi thứ bấm được đều là `<button>`
> hoặc `<a href>` thật, không có `<div onclick>` nào, và `href="#"` chết đã bị
> `KiemTraDieuHuongTest` cấm từ Phase 6.

### ✅ Tiêu chí nghiệm thu số 8 — clone kho về thư mục mới, ĐÃ CHẠY

Đây là tiêu chí duy nhất trong bảng mục 6 chưa từng chạy lần nào, và nó vừa trở nên đáng chạy
hơn hẳn: việc V4 đổi hẳn cách nạp CSDL (Flyway thay `spring.sql.init`), nên câu *"clone về máy
trắng có chạy được không"* không còn là câu hỏi đã trả lời.

Đã `git clone` từ chính kho này ra một thư mục mới rồi đi đúng bốn bước của README:

| Bước | Kết quả |
|---|---|
| 1. `git clone` | 80 commit; **mọi file quyết định đều có** — hai file di trú, hai file dữ liệu mẫu, `FlywayResetConfig`, workflow CI, ba script python |
| 2. Khai `MYSQL_PASSWORD` | biến môi trường đã có sẵn |
| 3. `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"` | Flyway chạy V1 → V2, nạp hai file dữ liệu, khởi động 7,7 giây |
| 4. `mvnw spring-boot:run` | *"Schema is up to date. No migration necessary"*, khởi động 4,9 giây |

**Phần đáng giá nhất là phép đối chiếu kèm theo:** CSDL do bản clone dựng so với CSDL đang có —
**0 dòng lệch trên 20.519 dòng**. Tức là toàn bộ bộ dữ liệu mà báo cáo mô tả **dựng lại được từ
mã nguồn**, không phụ thuộc vào thứ gì còn sót trên máy này.

Chạy nốt cả bộ nghiệm thu **từ trong bản clone**: `mvnw test` 315/315 · 8 script giao diện
215/215 · 3 script python đều 0.

> Vì clone lấy từ kho chứ không chép thư mục, phép kiểm này còn trả lời một câu khác: **có file
> nào chỉ nằm trong thư mục làm việc mà quên commit không.** Không có file nào.

Thư mục clone đã xoá sau khi đo.

### ✅ N3 — quyết cách A, kèm số đo

Mốc 14/08/2026 đã qua. **Chọn cách A: chấp nhận 4 nhóm và giải thích bằng một câu.** Cách B
(dời hạn thanh toán) và cách C (thêm kỳ 9) đều đụng dữ liệu vận hành đúng lúc không nên đụng gì
cả, và cách C còn phá trạng thái *"kỳ 8 rỗng để demo"*.

Đo ngày 19/08/2026: **chỉ nhóm *Trong hạn* rỗng**, bốn nhóm quá hạn đều có nội dung và mỗi kỳ
cước rơi đúng một nhóm — kỳ 7 → *1–30* · kỳ 6 → *31–60* · kỳ 5 → *61–90* · kỳ 3 và 4 → *trên
90*. Tổng 165 hóa đơn / 62.322.325 đ, khớp đúng số bàn giao.

Câu giải thích để dán dưới ảnh, bảng số đo đầy đủ và mốc **15/09/2026** (từ ngày đó chỉ còn 3
nhóm, vì kỳ 7 rời nhóm *1–30* mà không có kỳ nào thay chỗ) đã ghi ở đầu
[`danh-sach-anh-chup.md`](danh-sach-anh-chup.md).

> **Một nhánh dự đoán của `PHASE-6-REPORT.md` mục 1.2 nay đã tự gỡ.** Báo cáo đó (viết
> 10/08/2026) dự đoán *"từ 14/08 nhóm 61–90 rỗng"* — đúng cho ngày 14/08, nhưng nhóm ấy **đã
> đầy lại** vì kỳ 5 già thêm và rơi vào nó. Các nhóm quá hạn cạn rồi đầy lại khi kỳ cước già
> đi; riêng *Trong hạn* rỗng vĩnh viễn vì bộ dữ liệu mẫu không sinh thêm hóa đơn nào nữa. Ghi
> lại để người đọc sau không thấy hai tài liệu nói khác nhau mà tưởng có gì sai.

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
| `mvnw test` | 277 | **315** |
| Phép kiểm giao diện | 177 | **215** |
| Lớp test cần MySQL | 8 | 11 |
| Ảnh cần chụp | 65 | **70** |

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
