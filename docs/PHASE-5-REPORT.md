# BÁO CÁO PHASE 5 — HÓA ĐƠN, THANH TOÁN, CÔNG NỢ

> Phase 5 đã xong toàn bộ mục A0, A–F và G. Kế hoạch và ba ràng buộc bắt buộc:
> [`PHASE-5-PLAN.md`](PHASE-5-PLAN.md).
>
> ⚠️ **Nợ tài liệu:** báo cáo này chưa có phần viết cho **mục B, C, D**.
> Mục A đã viết ở Phần IV-A. Xem [mục 35](#35-nợ-tài-liệu-của-phase-5).
>
> **Bố cục:** mục 1 (A0) · Phần I–III (mục G) · **Phần IV-A (mục A)** · Phần IV (mục E) ·
> Phần V (mục F) · Phần VI (tổng kết Phase 5, bàn giao Phase 6).

---

## 1. Mục A0 — sửa số liệu chép lại

Rà toàn bộ tài liệu tìm số liệu thuộc diện *"chép lại chưa đo lại"*. Kết quả và nguyên tắc
áp dụng đã ghi ở [`PHASE-5-PLAN.md`](PHASE-5-PLAN.md) mục "Rà soát số liệu chép lại": **một**
con số sai (1.148 → 1.126) và **bốn** tham chiếu chéo lệch, trên tổng số hàng trăm con số
đã đo lại và khớp.

Nguyên tắc rút ra: **chỉ sửa số liệu được trình bày là trạng thái hiện tại**; số đo trong tài
liệu lịch sử giữ nguyên và gắn chú thích hồi cứu. Sửa số cũ trong bản kế hoạch là làm sai lệch
hồ sơ quá trình — mà hồ sơ quá trình chính là thứ báo cáo cần chứng minh.

---

# PHẦN I — MỤC G1: SỔ CÁI BIẾN ĐỘNG SỐ DƯ

## 2. Vấn đề: một con số không giải thích được

Trước Phase 5, bảng `nap_tien` có `so_du_truoc` / `so_du_sau` nên **nạp tiền** truy nguyên
được. Nhưng **trừ cước** không để lại vết nào — trừ xong chỉ còn `thue_bao.so_du` đã đổi.

Hệ quả: `so_du` là một con số không giải thích được. Nhìn vào không biết nó đến từ đâu, và
nếu nó sai thì không có cách nào tìm ra sai ở bước nào.

Cách xử lý: **tổng quát hoá chính bảng cũ** thành sổ cái, cố ý **không** thêm bảng thứ hai cho
phần trừ. Hai bảng cùng ghi số dư là hai nguồn sự thật — đúng thứ ràng buộc ② của Phase 5 cấm,
và đúng bài học 43.6 của Phase 4.

## 3. Thay đổi schema

| Việc | Chi tiết |
|---|---|
| Đổi tên bảng | `nap_tien` → `bien_dong_so_du` |
| Đổi tên cột | `ngay_nap` → `ngay_ghi_nhan` — tên cũ nói **sai nội dung** với dòng `TRU_CUOC` |
| Thêm cột | `loai_bien_dong ENUM('NAP_TIEN','TRU_CUOC','DIEU_CHINH') NOT NULL` |
| Thêm cột | `ky_cuoc_id BIGINT NULL`, FK → `ky_cuoc` — chỉ có giá trị với `TRU_CUOC` |
| Đổi tên | index và **cả hai ràng buộc FK** cho khớp tên bảng mới |

Áp lên CSDL đang chạy bằng `ALTER`, **không** dùng profile `reset` — đúng cách mục 4D đã làm,
vì `reset` sẽ sinh lại CDR ngẫu nhiên và làm mất khả năng đối chiếu với số liệu Phase 4.

Hai điểm đáng ghi lại:

**Đổi tên FK không phải việc làm cho đẹp.** Để nguyên `fk_nap_tien_thue_bao` trên một bảng tên
`bien_dong_so_du` là kiểu đổi tên nửa vời: về sau người đọc gặp tên ràng buộc trong thông báo
lỗi sẽ đi tìm một bảng không còn tồn tại.

**Bước "mặc định `NAP_TIEN` cho dữ liệu cũ" của đặc tả là một việc không cần làm.** Bảng đang
**rỗng** (0 dòng) nên không có gì để backfill. Đã kiểm trước khi viết `ALTER` thay vì thêm một
`DEFAULT` không ai dùng. Cột cố ý **không** có `DEFAULT`: mỗi chỗ ghi sổ cái phải nói rõ đây là
biến động loại gì.

## 4. Quy tắc dấu — gom vào một chỗ

`so_tien` luôn **dương**; chiều cộng/trừ do `loai_bien_dong` quyết định. Quy tắc đó gom trong
enum `LoaiBienDongSoDu`, không rải ra chỗ khác — cùng lý do lớp `DonViCuoc` gom ba chỗ quy đổi
đơn vị của Phase 4.

Ghi rõ trong javadoc: nếu về sau cần điều chỉnh **giảm** thì phải thêm giá trị enum riêng, chứ
**không** đổi dấu `DIEU_CHINH`. Đổi dấu sẽ làm một loại mang hai ý nghĩa và phá chính quy tắc
"dấu suy được từ loại".

## 5. Mười tám dòng mở sổ

Đây là điểm đã nêu ra khi rà đặc tả và được chốt trước khi làm.

Số dư mẫu nạp thẳng vào `thue_bao.so_du` từ `data-mau.sql`, **không có dòng sổ cái nào chống
lưng**. Bất biến G1.6 vì thế sai ngay từ dòng đầu tiên: thuê bao 1 có `so_du = 285.000` trong
khi sổ cái cộng ra `0`. Không phải một thuê bao — **cả 18**.

Phương án đã chốt: mỗi thuê bao trả trước có số dư > 0 nhận một bút toán `DIEU_CHINH` đưa số dư
từ 0 lên đúng giá trị mẫu. ID cố định 1–18, `ngay_ghi_nhan` = `ngay_kich_hoat` của chính thuê
bao đó, đặt trong `data-mau.sql` nên lần chạy `reset` sau vẫn có.

Hai thuê bao còn lại (id 8, 15) có số dư 0 nên **không cần** dòng mở sổ — bất biến tự đúng với
`0 = 0 − 0`. Không tạo dòng 0 đồng chỉ để cho đủ bộ.

## 6. Bất biến và bằng chứng nó không xanh rỗng

`KiemTraSoCaiSoDuTest` — 2 test chạy trên dữ liệu thật:

```
1. Với MỌI thuê bao:  so_du = SUM(nạp + điều chỉnh) − SUM(trừ)
2. Với TỪNG dòng:     so_du_sau − so_du_truoc = số tiền đã áp dấu
```

Test 2 tồn tại vì test 1 kiểm ở mức **tổng**, mà hai dòng sai ngược dấu sẽ triệt tiêu nhau ở
mức tổng — bài học 43.3 của Phase 4.

Cả hai cộng dồn bằng chính `LoaiBienDongSoDu.apDauCho()` mà mã nghiệp vụ dùng, **không** chép
lại quy tắc dấu vào test. Chép lại thì khi quy tắc đổi, test vẫn xanh theo quy tắc cũ.

**Bằng chứng test bắt được lỗi thật** — theo bài học 43.5, một phép kiểm chưa từng đỏ thì chưa
chứng minh được gì:

| Bước | Kết quả |
|---|---|
| Cố ý `UPDATE thue_bao SET so_du = so_du + 1000 WHERE id = 1` | Test **ĐỎ** |
| Thông báo lỗi | `0901234501 (id 1): so_du = 286000.00 nhưng sổ cái cộng ra 285000.00 — chênh 1000.00` |
| Khôi phục lại | Test **XANH** |

Thông báo chỉ thẳng thuê bao nào, lệch bao nhiêu, không bắt người đọc đi dò.

## 7. Nghiệm thu mục G1

| # | Tiêu chí | Kết quả |
|---|---|---|
| 1 | Bất biến sổ cái, mọi thuê bao | ✅ **0 lệch** trên 80 thuê bao / 18 dòng sổ cái |
| 2 | Bất biến từng dòng | ✅ 0 lệch |
| 3 | Test bắt được lỗi thật | ✅ đỏ trước, xanh sau |
| 4 | `mvnw test` | ✅ **150 test**, 0 lỗi, BUILD SUCCESS |
| 5 | Entity khớp bảng đã migrate | ✅ `SchemaValidationTest` xanh |

---

# PHẦN II — MỤC G3: DỰ ĐOÁN CÔNG BỐ TRƯỚC KHI VIẾT CODE

> ⚠️ **Toàn bộ mục này được viết và commit TRƯỚC khi viết một dòng code trừ số dư nào.**
> Đó là điều kiện để nó có giá trị: theo chuẩn làm việc 43.4, nếu số thực tế lệch thì đó là
> tín hiệu **dừng lại phân tích**, không phải tín hiệu sửa cho khớp. Dự đoán viết sau khi thấy
> kết quả thì không còn khả năng đó.

## 8. Cách đo

Mô phỏng đúng thuật toán G2 bằng SQL: duyệt CDR theo `ORDER BY thoi_gian_bat_dau, id`, trừ dần
vào quỹ, **dừng hẳn** ở bản ghi đầu tiên không còn đủ quỹ.

Tập được trừ là một **tiền tố** của dãy. Chứng minh: bản ghi thứ *i* được trừ khi và chỉ khi
luỹ kế(1..*i*) ≤ `so_du`, vì quỹ sau *i−1* bản ghi bằng `so_du` − luỹ kế(*i−1*), và bản ghi *i*
vừa quỹ khi `cuoc_phi`ᵢ ≤ `so_du` − luỹ kế(*i−1*), tức luỹ kế(*i*) ≤ `so_du`. Đã dừng hẳn ở bản
ghi đầu tiên không vừa nên tập đó đúng là tiền tố **dài nhất** thoả luỹ kế ≤ `so_du`.

## 9. ⭐ Dự đoán cho kỳ 6/2026

**Phân nhóm 20 thuê bao trả trước:**

| Nhóm | Dự đoán |
|---|---|
| A. Không phát sinh CDR kỳ 6 | **4** thuê bao (id 8, 12, 15, 20) |
| B. Đủ số dư trả hết cước | **15** thuê bao |
| C. Hết số dư giữa chừng | **1** thuê bao — id 4, `0944567804` |

**Tổng tiền:**

| Chỉ tiêu | Dự đoán |
|---|---|
| Tổng cước kỳ 6 của thuê bao trả trước | **2.001.863 đ** |
| Tổng sẽ trừ được | **1.991.417 đ** |
| Tổng **không** thu được do hết số dư | **10.446 đ** |
| Số dòng `TRU_CUOC` sẽ sinh ra | **16** (chỉ thuê bao có phát sinh CDR) |

**Thuê bao 4 — trường hợp duy nhất hết số dư, đã kiểm tay:**

| Chỉ tiêu | Dự đoán |
|---|---|
| Số dư đầu | 205.000 đ |
| Cước cả kỳ | 215.226 đ (76 bản ghi) |
| Sẽ trừ | **204.780 đ** qua **69** bản ghi |
| Số dư còn lại | **220 đ** |
| Không thu được | **10.446 đ** |

## 10. ⭐ Hai phát hiện khi đo — đặc tả dự kiến sai

### 10.1. Ba thuê bao "cố ý để thấp" KHÔNG thể minh hoạ ca hết số dư

Kế hoạch G3 dự kiến *"≥ 3 thuê bao 18.000 / 20.000 / 22.000 đ sẽ hết số dư giữa chừng"* — số dư
này do chính mục 4F đặt ra với mục đích *"Phase 5 cần trường hợp thật để minh hoạ cảnh báo số
dư không đủ"*.

Đo ra thì **không thuê bao nào trong ba** rơi vào nhóm C:

| Thuê bao | Số dư | Trạng thái | CDR kỳ 5 | CDR kỳ 6 | Nhóm |
|---|---|---|---|---|---|
| `0965678905` (id 5) | 18.000 đ | `TAM_NGUNG_1C` | 0 | **1** (99 đ) | B |
| `0852345612` (id 12) | 22.000 đ | `TAM_NGUNG_1C` | 0 | **0** | A |
| `0810123420` (id 20) | 20.000 đ | `TAM_NGUNG_1C` | 0 | **0** | A |

**Nguyên nhân:** cả ba đều `TAM_NGUNG_1C`. Thuê bao tạm ngưng một chiều gần như không phát sinh
cuộc gọi đi, nên bộ sinh CDR hầu như không tạo bản ghi cho chúng — tổng cộng **1 bản ghi 99 đ**
trên cả hai kỳ. Số dư thấp không bao giờ bị chạm tới.

Đây là **lỗi thiết kế dữ liệu mẫu ở 4F**: chọn đúng ba thuê bao có số dư thấp nhưng lại chọn
nhầm ba thuê bao không có lưu lượng. Hai thuộc tính được đặt độc lập nhau, và không ai đối
chiếu chéo.

### 10.2. Ca hết số dư thật đến từ chỗ không ai chờ

Thuê bao duy nhất rơi vào nhóm C là `0944567804` — thuộc nhóm *"đủ tiền"* (205.000 đ), hoạt
động bình thường. Nó hết số dư không vì số dư thấp mà vì **lưu lượng cao**: 215.226 đ cước trên
76 bản ghi.

Nói cách khác, ca thử mà 4F cố ý dựng thì không xảy ra, còn ca thử thật thì xuất hiện ở chỗ
không ai chuẩn bị. Đây chính là lý do phải **đo** thay vì **suy luận** — và là ví dụ thứ ba
trong dự án cho cùng một bài học.

### 10.3. Thuê bao 4 là minh hoạ tốt cho quy tắc không cắt đôi

Kiểm tay quanh điểm cắt (số dư 205.000 đ):

| # | Bản ghi | Cước | Luỹ kế | Kết quả |
|---|---|---|---|---|
| 68 | id 1626 | 725 đ | 200.505 | ✅ trừ |
| 69 | id 675 | **4.275 đ** | **204.780** | ✅ trừ — quỹ còn **220 đ** |
| 70 | id 3534 | **270 đ** | 205.050 | ❌ **DỪNG** — 270 > 220 |
| 71 | id 4933 | 99 đ | 205.149 | ❌ không trừ |
| 72 | id 2406 | 210 đ | 205.359 | ❌ không trừ |

Bản ghi 71 (99 đ) và 72 (210 đ) đều **nhỏ hơn** 220 đ còn lại — nếu thuật toán *bỏ qua rồi đi
tiếp* thay vì *dừng hẳn* thì hai bản ghi đó sẽ bị trừ. Quy tắc đã chốt là **dừng hẳn**, đúng
quyết định 5.3 của Phase 4.

Đây là bằng chứng cụ thể nhất cho việc kết quả **phụ thuộc thứ tự**, và vì sao truy vấn bắt
buộc phải có `ORDER BY thoi_gian_bat_dau, id` cố định.

## 11. Một điểm cần chốt trước khi chạy: thứ tự kỳ

Dự đoán trên tính từ **số dư hiện tại** và chỉ cho **kỳ 6**. Nếu về sau chạy trừ cước cho kỳ 5
thì kết quả sẽ khác hẳn tuỳ thứ tự:

- Chạy kỳ 5 **trước** rồi kỳ 6: số dư vào kỳ 6 đã bị trừ 1.643.768 đ, nhiều thuê bao sẽ rơi
  vào nhóm C
- Chạy kỳ 6 trước: đúng như dự đoán mục 9

Trừ cước là thao tác **có thứ tự theo thời gian**, không giao hoán. Mục G2 sẽ chặn ở tầng
nghiệp vụ để không chạy được kỳ cũ sau khi kỳ mới đã trừ, và ghi rõ vào báo cáo kết quả.

---

# PHẦN III — MỤC G2 VÀ G4: TRỪ CƯỚC VÀ GIAO DIỆN

## 12. Thuật toán và ba chốt chặn

`TruCuocTraTruocService` duyệt CDR đã định giá của thuê bao trả trước theo
`ORDER BY thueBao.id, thoiGianBatDau, id`, trừ dần vào quỹ, **dừng hẳn** ở bản ghi đầu tiên
không đủ quỹ, rồi ghi **đúng một dòng** `TRU_CUOC` cho mỗi thuê bao.

**Một dòng cho cả kỳ chứ không phải một dòng mỗi CDR.** Sổ cái ghi lại *biến động số dư*, mà
biến động ở đây là một lần trừ cước kỳ. Ghi 76 dòng cho 76 bản ghi sẽ làm sổ cái phình lên mà
không nói thêm được gì — đường đi tới từng bản ghi đã có sẵn ở bảng đối soát cước của mục 4E.

Ba chốt chặn đặt ở **tầng nghiệp vụ**, không trông vào giao diện (bài học 4.4 của Phase 3):

| # | Chặn khi | Lý do |
|---|---|---|
| 1 | Kỳ đã có dòng `TRU_CUOC` | Chạy lại sẽ trừ chồng |
| 2 | Đã trừ cước cho kỳ **muộn hơn** | Trừ cước không giao hoán theo thời gian |
| 3 | Kỳ đã `DA_CHOT` (khi hủy) | Đối xứng với `huyBillingKy` |

**Chốt chặn thứ tư, thêm ở `RatingService`.** Phát hiện khi rà: `huyRatingKy` từ chối chạy khi
kỳ đã có hóa đơn, nhưng **không** biết gì về dòng trừ cước. Hủy tính cước sau khi đã trừ số dư
sẽ xoá sạch `cuoc_phi` trong khi các dòng `TRU_CUOC` vẫn giữ số tiền lấy từ chính chúng — số dư
trở lại thành con số không giải thích được, đúng thứ mục G1 sinh ra để tránh. Đã thêm chốt chặn
đối xứng cho nhánh trả trước.

## 13. Hoàn tác: cộng trả lại, không gán thẳng

Đặc tả viết *"trả `so_du` về giá trị trước khi trừ"*. Cài đặt **cộng trả lại số tiền** thay vì
gán `so_du = so_du_truoc`.

Hai cách cho cùng kết quả khi không có biến động nào xen giữa. Nhưng nếu khách **nạp tiền sau**
lần trừ thì gán thẳng sẽ **xoá mất khoản nạp đó**: trừ 400 (1000 → 600), nạp 500 (600 → 1100),
hoàn tác gán thẳng ra 1000 — mất 500 đ của khách. Cộng trả lại ra 1500, đúng bằng 1000 + 500.

Cộng trả lại còn giữ đúng bất biến sổ cái trong mọi trường hợp, vì nó xoá dòng **và** hoàn số
tiền của đúng dòng đó. Có test riêng cho tình huống này (`coNapTienXenGiua_congTraLai`).

## 14. ⭐ Đối chiếu dự đoán — khớp tuyệt đối

Chạy **toàn bộ qua giao diện** (đăng nhập `admin`, nút *Trừ cước trả trước* trên `/tinh-cuoc`).

| # | Chỉ tiêu | Dự đoán (mục 9) | Thực tế | |
|---|---|---|---|---|
| 1 | Số dòng `TRU_CUOC` | **16** | **16** | ✅ |
| 2 | Tổng đã trừ | **1.991.417 đ** | **1.991.417 đ** | ✅ |
| 3 | Không thu được | **10.446 đ** | **10.446 đ** | ✅ |
| 4 | Thuê bao hết số dư giữa chừng | **1** | **1** | ✅ |
| 5 | Thuê bao 4 — đã trừ | **204.780 đ** | **204.780 đ** | ✅ |
| 6 | Thuê bao 4 — số dư còn lại | **220 đ** | **220 đ** | ✅ |
| 7 | Số bản ghi được trừ | **1.119** | **1.119** | ✅ |

**Cả bảy dự đoán đúng, sáu trong số đó đúng tới từng đồng.** Thời gian chạy 150 ms.

Con số 1.119 đáng chú ý: nó bằng 1.126 bản ghi của thuê bao trả trước trong kỳ, **trừ đi đúng
7 bản ghi** của thuê bao 4 nằm sau điểm dừng — khớp với bảng kiểm tay ở mục 10.3.

## 15. Nghiệm thu mục G

| # | Tiêu chí | Kết quả |
|---|---|---|
| 1 | Bất biến sổ cái `so_du = SUM(nạp) − SUM(trừ)` | ✅ **0 lệch** trên 80 thuê bao / 34 dòng sổ cái |
| 2 | Dự đoán G3 khớp thực tế | ✅ **7/7**, xem mục 14 |
| 3 | Chạy trừ cước hai lần → không trừ chồng | ✅ vẫn 16 dòng / 1.991.417 đ, kèm thông báo tiếng Việt |
| 4 | Hoàn tác → chạy lại → `so_du` giống hệt lần đầu | ✅ so khớp **cả 20 thuê bao**, không lệch dòng nào |
| 5 | Thuê bao trả trước vẫn KHÔNG có hóa đơn nào | ✅ **0** hóa đơn |
| — | `mvnw test` | ✅ **164 test**, 0 lỗi, BUILD SUCCESS |

Thông báo khi chặn chạy lại, nguyên văn từ giao diện:

> Kỳ cước tháng 6/2026 đã trừ cước vào số dư (16 thuê bao). Chạy lại sẽ trừ chồng.
> Phải hủy kết quả trừ cước trước nếu muốn chạy lại.

### 15.1. Một sự cố nhỏ khi kiểm chứng

Lần kiểm tiêu chí 3 đầu tiên báo *"không thấy thông báo lỗi"*, và suýt bị hiểu là chốt chặn
không chạy. Truy ra nguyên nhân nằm ở **phép kiểm**, không ở engine: script `POST` rồi mới `GET`
lại trang để tìm thông báo, nhưng `RedirectAttributes` là **flash attribute** — nó đã bị tiêu
thụ ngay ở lần redirect sau `POST`, nên lần `GET` thứ hai đương nhiên không còn gì. Đọc thẳng
body của `POST` thì thông báo hiện đủ.

Đây là lần thứ hai trong dự án gặp đúng bài học 43.5: một phép kiểm sai tạo báo động giả, và
nếu tin nó thì đã đi "sửa" một chốt chặn đang chạy đúng.

## 16. Mục G4 — giao diện

| Việc | Cài đặt |
|---|---|
| Tab **"Biến động số dư"** | Thay *"Lịch sử nạp tiền"* trên chi tiết thuê bao trả trước. Thêm cột **Loại** và **Kỳ cước**; dấu `+/−` lấy từ `loaiBienDong.congVaoSoDu` |
| Cảnh báo số dư thấp | Badge đỏ cạnh số dư trên chi tiết thuê bao, kèm **icon và chữ** chứ không chỉ màu |
| Bảng cảnh báo tập trung | Khối cuối trang `/tinh-cuoc` liệt kê thuê bao dưới ngưỡng, sắp xếp số dư tăng dần |
| Nút chạy / hoàn tác | Trên `/tinh-cuoc` cạnh nút lập hóa đơn, kèm modal xác nhận cho nút hủy |

Ngưỡng cảnh báo **50.000 đ** đặt trong `ThamSoTinhCuoc.NGUONG_CANH_BAO_SO_DU`, cùng chỗ với VAT
và hạn thanh toán — không rải hằng số ra template.

Hai điểm giữ nguyên chuẩn đã đặt từ 4E:

- **Không dùng màu làm phương tiện duy nhất.** Cảnh báo có icon `⚠` và chữ *"Số dư thấp"*; dấu
  cộng/trừ trong sổ cái là ký tự thật. Bản in đen trắng vẫn đọc được.
- **Cảnh báo không chặn nghiệp vụ nào.** Thuê bao dưới ngưỡng vẫn dùng dịch vụ và vẫn bị trừ
  cước bình thường. Ghi rõ ngay dưới bảng để không ai hiểu nhầm.

Số thuê bao dưới ngưỡng tăng từ **3** lên **6** sau khi trừ cước kỳ 6 — bảng cảnh báo có nội
dung thật để chụp ảnh, và ba thuê bao mới vào danh sách đều là ca *"lưu lượng cao"* chứ không
phải ca *"số dư mẫu thấp"*, đúng như phân tích ở mục 10.

## 17. Hạn chế đã biết của mục G

1. **`DIEU_CHINH` chỉ cộng, không trừ.** Quy tắc dấu suy từ loại nên một loại chỉ mang được một
   chiều. Cần điều chỉnh giảm thì phải thêm giá trị enum riêng, không đổi dấu giá trị hiện có.
2. **Trừ cước theo kỳ, không real-time.** Khách vẫn dùng được dịch vụ khi số dư đã cạn cho tới
   lần chạy kế tiếp. Tính cước thời gian thực nằm ở "Hướng phát triển".
3. ~~Phần cước không thu được không được ghi nhận ở đâu cả.~~ ✅ **Đã xử lý — xem mục 18.**
4. **Chưa chạy trừ cước cho kỳ 5/2026.** Kỳ 5 đã `DA_CHOT` và chốt chặn thứ tự sẽ từ chối chạy
   kỳ cũ sau kỳ 6. Đây là lựa chọn có chủ đích theo mục 11, không phải thiếu sót.

## 18. ⭐ Phần cước không thu được — ghi nhận, nhưng KHÔNG phải là nợ

Hạn chế số 3 ở mục 17 đã được xử lý, và cách xử lý mới là phần đáng ghi lại.

### 18.1. Vì sao KHÔNG thêm bảng nợ

Phản xạ đầu tiên khi thấy 10.446 đ *"biến mất"* là thêm một bảng công nợ cho thuê bao trả
trước. Đó là quyết định sai, vì nó **mô hình hoá một thứ không tồn tại**.

Thuê bao trả trước **không có nợ**. Với trả trước thời gian thực — cách nhà mạng thật vận hành
— cuộc gọi làm cạn số dư bị **chặn ngay tại thời điểm phát sinh**, nên số tiền này không bao
giờ ra đời. Nó chỉ xuất hiện trong hệ thống này vì hai lựa chọn của mô hình:

1. Định giá trước, trừ sau, **theo lô cuối kỳ** (phạm vi G2, không real-time)
2. Quy tắc **không cắt đôi bản ghi** (quyết định 5.3)

Thêm bảng nợ sẽ biến một **hiện vật của mô hình** thành một **thực thể nghiệp vụ** — đúng loại
lỗi mục 4A đã gặp với `DATA/NGOAI_MANG`: bộ sinh tạo ra tổ hợp không nên tồn tại, và cách chữa
đúng là **cấm sinh** chứ không phải thêm dòng bảng giá cho nó.

### 18.2. Cách đã chọn: hai cột trên chính dòng `TRU_CUOC`

```sql
ALTER TABLE bien_dong_so_du
    ADD COLUMN so_cdr_da_tru          INT           NULL AFTER ky_cuoc_id,
    ADD COLUMN so_tien_khong_thu_duoc DECIMAL(15,2) NULL AFTER so_cdr_da_tru;
```

Số liệu đi cùng chính bút toán sinh ra nó, nên đối soát ngược được mà không tạo thêm thực thể
nào. Cả hai cột để `NULL` với dòng `NAP_TIEN` / `DIEU_CHINH` — kiểm chứng: **0 vi phạm**.

Javadoc của `BienDongSoDu.soTienKhongThuDuoc` và chú thích trong `schema.sql` đều ghi thẳng
*"đây là hiện vật của việc trừ theo lô, không phải một khoản nợ"*, để người đọc sau không diễn
giải nó thành khoản phải thu.

### 18.3. Kiểm chứng

Không `UPDATE` tay để lấp hai cột: **hủy trừ cước rồi chạy lại qua giao diện**, để dữ liệu đi
đúng đường code sẽ chạy thật. Việc này đồng thời chứng minh lại tiêu chí 4 lần thứ hai.

| Chỉ tiêu | Giá trị |
|---|---|
| Số dòng `TRU_CUOC` | 16 |
| Tổng đã trừ | 1.991.417 đ |
| `SUM(so_cdr_da_tru)` | **1.119** — khớp con số đã dự đoán ở mục 9 |
| `SUM(so_tien_khong_thu_duoc)` | **10.446 đ** — khớp mục 9 |
| Dòng không phải `TRU_CUOC` mà có hai cột khác NULL | **0** |
| Bất biến sổ cái | **0 lệch** |

Thuê bao 4 giờ đọc được trọn câu chuyện trên **một dòng**: trừ 204.780 đ qua **69** bản ghi,
số dư 205.000 → 220 đ, **10.446 đ** không thu được. Trước khi có hai cột này thì ba con số sau
phải đi tra lại CDR mới biết.

---

# PHẦN IV-A — MỤC A: MÀN HÌNH HÓA ĐƠN

> Mục A là mục đầu tiên của Phase 5 làm ra thứ người dùng nhìn thấy: hai màn hình
> (`/hoa-don` và `/hoa-don/{id}`), một bản in PDF, một bản xuất Excel.
>
> Nó cũng là mục **đặt ra ràng buộc ② cho cả phase** — *một nguồn sự thật cho "còn nợ"* — vì
> đây là chỗ đầu tiên có nhiều màn hình cùng muốn hiển thị một con số tiền.

## A.1. Phạm vi

| Làm | Không làm ở mục này |
|---|---|
| Danh sách hóa đơn: lọc, phân trang, dòng tổng, xuất Excel | Ghi nhận thanh toán *(mục B)* |
| Chi tiết hóa đơn: bốn khối thông tin | Bảng tuổi nợ, đề xuất tạm ngừng *(mục C)* |
| Bốn trạng thái hóa đơn và cách chuyển giữa chúng | Khoản giảm trừ *(mục D)* |
| Xuất hóa đơn ra PDF có dấu tiếng Việt | Sửa engine tính cước *(đã xong ở Phase 4)* |

Hóa đơn **không được tạo ở mục này**. Chúng do `BillingService` của Phase 4 lập; mục A chỉ
đọc và trình bày. Đó là lý do màn hình hóa đơn không có nút "Thêm hóa đơn" nào — một hóa đơn
không có căn cứ từ bản ghi sử dụng thì không nên tồn tại.

## A.2. Màn hình danh sách

**Sáu tiêu chí lọc**, gom trong `BoLocHoaDon`: kỳ cước · trạng thái · khách hàng (tên hoặc mã)
· số thuê bao · khoảng tiền từ – đến.

Ba quyết định trình bày, mỗi cái có lý do:

**1. Dòng tổng tính trên TOÀN BỘ kết quả lọc, không phải trang đang xem.**
`TongHopHoaDon` là một truy vấn gộp nhóm riêng chứ không cộng từ `Page.content`. Cộng từ trang
đang xem thì người dùng lọc ra 148 hóa đơn quá hạn, thấy dòng tổng ghi số của 15 dòng đầu, và
tin đó là tổng nợ thật. Con số sai kiểu này không có gì báo động — nó chỉ *nhỏ hơn sự thật*.

**2. Cột "Còn nợ" ĐỌC THẲNG `hoa_don.con_no`.**
Không màn hình nào tự tính `tong_thanh_toan − SUM(thanh_toan)`. Đây chính là **ràng buộc ②**
của Phase 5, và mục A là nơi nó được đặt ra: khi có hai màn hình cùng hiển thị số còn nợ, thì
hoặc chúng đọc chung một cột, hoặc sớm muộn cũng lệch nhau. Cám dỗ tự tính rất lớn vì câu SQL
đó dễ viết và "không ai sửa gì" — nhưng đó đúng là cách sinh ra nguồn sự thật thứ hai.

**3. Bộ lọc được giữ nguyên khi chuyển trang.** Liên kết phân trang mang theo `chuoiLoc`. Mất
bộ lọc khi sang trang 2 là lỗi kinh điển của màn hình danh sách, và nó âm thầm: người dùng
tưởng mình vẫn đang xem tập đã lọc.

## A.3. Màn hình chi tiết — bốn khối

| Khối | Trả lời câu hỏi |
|---|---|
| Thông tin hóa đơn và thuê bao | Hóa đơn của ai, kỳ nào, hạn bao giờ |
| Bảng khoản mục | Tiền đến từ đâu — từng dòng cước, và dòng giảm trừ mang **thành tiền âm** |
| Khối tổng tiền | Cộng trước thuế → VAT → tổng → đã thu → còn nợ |
| Lịch sử thanh toán | Khách đã trả mấy lần, mỗi lần bao nhiêu, ai thu |

**Liên kết chéo sang bảng đối soát cước** của Phase 4 là chi tiết đáng giá nhất của màn hình
này: từ một con số trên hóa đơn, người dùng lần ngược được tới từng cuộc gọi tạo ra nó. Không
có liên kết đó thì bảng đối soát chỉ tới được bằng cách gõ tay đường dẫn — và một màn hình
không ai tới được thì coi như không có.

## A.4. Bốn trạng thái — và một luật dễ viết sai

| Trạng thái | Nghĩa |
|---|---|
| `CHUA_TT` | Chưa thu đồng nào, còn trong hạn |
| `TT_MOT_PHAN` | Đã thu được một phần |
| `DA_TT` | Còn nợ bằng 0 |
| `QUA_HAN` | Quá hạn thanh toán **và chưa thu đồng nào** |

Ba trạng thái đầu là tình trạng của **tiền**; `QUA_HAN` là tình trạng của **thời gian**. Trộn
hai trục đó vào một cột là nguồn gốc của luật dễ viết sai nhất Phase 5:

> **Quét quá hạn chỉ được chạm hóa đơn CHƯA THU ĐỒNG NÀO.**
> `timCanChuyenQuaHan` có thêm điều kiện `da_thanh_toan IS NULL OR da_thanh_toan = 0`.

Bỏ điều kiện đó thì mọi hóa đơn `TT_MOT_PHAN` quá hạn sẽ bị quét thành `QUA_HAN`, và
`TT_MOT_PHAN` trở thành **trạng thái không thể tồn tại sau ngày hết hạn** — thông tin "khách
này đã trả được một phần" biến mất khỏi hệ thống. Chi tiết ở [mục 33](#33--năm-điểm-sai-lệch-đặc-tả-phát-hiện-ở-phase-5).

Việc quét chạy hai đường: theo lịch 00:05 hằng ngày (`LichChayNenConfig`) và một lần nữa khi
ai đó mở màn hình. Lịch lo trường hợp không ai mở màn hình suốt ngày; lần gọi lúc mở màn hình
lo trường hợp ứng dụng vừa khởi động lại và đã lỡ giờ hẹn. Hàm chỉ đổi trạng thái hóa đơn
**thực sự** quá hạn nên chạy nhiều lần không sinh tác dụng phụ.

## A.5. Xuất PDF — lỗi không làm hỏng file

Hóa đơn in ra PDF bằng OpenPDF, font **Liberation Sans** (SIL OFL 1.1) **nhúng** vào file kèm
bảng mã `IDENTITY_H`.

Đây là chỗ đáng ghi nhất của mục A, vì kiểu lỗi ở đây rất khó thấy:

> Dùng font Base14 có sẵn của chuẩn PDF thì mọi ký tự có dấu ra ô vuông hoặc mất dấu — nhưng
> **file vẫn mở được, vẫn in được, không ngoại lệ nào ném ra, không cảnh báo nào**. Chỉ có chữ
> là sai.

Nên `HoaDonPdfServiceTest` không kiểm "file có tồn tại" hay "kích thước lớn hơn 0" — nó **đọc
lại nội dung PDF** và so khớp chuỗi **có dấu**. Kiểm sự tồn tại của file thì xanh cả khi mọi
chữ tiếng Việt đã thành ô vuông.

`PdfFont` nạp font một lần lúc khởi tạo và ném `IllegalStateException` ngay nếu thiếu file —
thà chết lúc khởi động còn hơn in ra hàng trăm hóa đơn sai chữ rồi mới biết.

## A.6. Nghiệm thu mục A

Số liệu đo trên dữ liệu hiện hành (280 hóa đơn thuộc 5 kỳ, 620 dòng khoản mục):

| Trạng thái | Số hóa đơn | Tổng thanh toán | Còn nợ |
|---|---:|---:|---:|
| Thanh toán một phần | 17 | 6.650.718 đ | 3.572.184 đ |
| Đã thanh toán | 115 | 46.112.153 đ | 0 đ |
| Quá hạn | 148 | 58.750.141 đ | 58.750.141 đ |
| **Chưa thanh toán** | **0** | — | — |

| Phép kiểm | Kết quả |
|---|---|
| `HoaDonServiceTest` | 8 test |
| `HoaDonPdfServiceTest` — đọc lại nội dung PDF, so chuỗi có dấu | 8 test |
| Script `test-muc-F.ps1` — lọc theo kỳ và trạng thái, đối chiếu SQL độc lập | 17 phép kiểm |
| Bất biến `con_no = tong_thanh_toan − da_thanh_toan` trên **mọi** hóa đơn | 0 dòng lệch |

> ⚠️ **Nhóm `CHUA_TT` bằng 0 là do NGÀY XEM, không phải do thiếu dữ liệu.** Hạn thanh toán
> muộn nhất của dữ liệu mẫu là 15/08/2026; từ 16/08/2026 trở đi mọi hóa đơn chưa thu đều đã bị
> quét sang `QUA_HAN`. Muốn ảnh chụp có đủ bốn trạng thái thì phải chụp trước ngày đó — cùng
> họ vấn đề với bảng tuổi nợ 5 nhóm ở [mục 33](#33--năm-điểm-sai-lệch-đặc-tả-phát-hiện-ở-phase-5).

## A.7. Màn hình cần chụp cho mục A

Ảnh của mục A **đã nằm trong** [mục 32](#32-danh-sách-màn-hình-chụp-ảnh-cho-toàn-phase-5) —
số 2, 3, 4 và 5 — và trong [`danh-sach-anh-chup.md`](danh-sach-anh-chup.md) của toàn dự án.

*Cố ý không liệt kê lại ở đây.* Ba danh sách ảnh cho cùng một màn hình là ba nguồn sự thật, và
chỉ cần đổi một màn hình là chúng bắt đầu lệch nhau — đúng cái lỗi mà chính báo cáo này cảnh
báo ở ràng buộc ②.

---

# PHẦN IV — MỤC A–F: HÓA ĐƠN, THANH TOÁN, CÔNG NỢ

## 19. ⭐ Dự đoán đường giảm trừ — công bố TRƯỚC khi chạy

> **Commit này nằm trước commit chạy thật.** Đường giảm trừ được viết ở mục 4C nhưng
> **chưa từng chạy với `giamTru > 0`** — bảng `giam_tru` rỗng suốt Phase 4. Đây đúng dạng
> nhánh code chưa ai đi, cùng loại với nhánh *prorate do huỷ giữa kỳ* mà mục 4F phát hiện.

### 19.1. Hai khoản giảm trừ sẽ thêm cho kỳ 6/2026

| Thuê bao | Hóa đơn | Cước trước giảm trừ | Cách khai |
|---|---|---|---|
| `0941234541` (id 41) | HD202606-000020 | 730.400 đ | **Số tiền tuyệt đối 50.000 đ** |
| `0930123440` (id 40) | HD202606-000019 | 690.800 đ | **Tỷ lệ 7,5%** |

### 19.2. Con số kỳ vọng — tính tay theo công thức của mục 4C

**Thuê bao 41 — giảm trừ tuyệt đối:**

| Bước | Phép tính | Kỳ vọng |
|---|---|---|
| `giam_tru` | khai tuyệt đối, không quy đổi | **50.000 đ** |
| `tong_truoc_thue` | 730.400 − 50.000 | **680.400 đ** |
| `thue_vat` | ROUND(680.400 × 10%) | **68.040 đ** |
| `tong_thanh_toan` | 680.400 + 68.040 | **748.440 đ** |
| `con_no` | = tổng thanh toán (chưa thu) | **748.440 đ** |
| Giảm so với hiện tại | 803.440 − 748.440 | **55.000 đ** |

**Thuê bao 40 — giảm trừ theo tỷ lệ:**

| Bước | Phép tính | Kỳ vọng |
|---|---|---|
| `giam_tru` | ROUND(690.800 × 7,5 / 100) = ROUND(51.810,0) | **51.810 đ** |
| `tong_truoc_thue` | 690.800 − 51.810 | **638.990 đ** |
| `thue_vat` | ROUND(638.990 × 10%) = ROUND(63.899,0) | **63.899 đ** |
| `tong_thanh_toan` | 638.990 + 63.899 | **702.889 đ** |
| `con_no` | = tổng thanh toán | **702.889 đ** |
| Giảm so với hiện tại | 759.880 − 702.889 | **56.991 đ** |

**Toàn kỳ 6/2026:**

| Chỉ tiêu | Kỳ vọng |
|---|---|
| `SUM(giam_tru)` | **101.810 đ** (50.000 + 51.810) |
| Doanh thu kỳ | **23.828.605 đ** (23.940.596 − 55.000 − 56.991) |
| Số hóa đơn | **58** — không đổi |
| Dòng `chi_tiet_hoa_don` tăng thêm | **2** dòng "Giảm trừ", thành tiền **âm** |
| Trạng thái hai khoản giảm trừ | `CHUA_AP_DUNG` → **`DA_AP_DUNG`** |
| Sau khi huỷ hóa đơn lần nữa | quay lại **`CHUA_AP_DUNG`** |

Nếu bất kỳ con số nào lệch → **DỪNG, phân tích trước khi sửa.**

## 20. ⭐ Kết quả đường giảm trừ — khớp tuyệt đối

Chạy toàn bộ **qua giao diện**: huỷ hóa đơn kỳ 6 → lập lại.

| # | Chỉ tiêu | Dự đoán (mục 19) | Thực tế | |
|---|---|---|---|---|
| 1 | TB 41 — `giam_tru` | 50.000 đ | 50.000 đ | ✅ |
| 2 | TB 41 — `tong_truoc_thue` | 680.400 đ | 680.400 đ | ✅ |
| 3 | TB 41 — `thue_vat` | 68.040 đ | 68.040 đ | ✅ |
| 4 | TB 41 — `tong_thanh_toan` / `con_no` | 748.440 đ | 748.440 đ | ✅ |
| 5 | TB 40 — `giam_tru` (7,5%) | 51.810 đ | 51.810 đ | ✅ |
| 6 | TB 40 — `tong_truoc_thue` | 638.990 đ | 638.990 đ | ✅ |
| 7 | TB 40 — `thue_vat` | 63.899 đ | 63.899 đ | ✅ |
| 8 | TB 40 — `tong_thanh_toan` / `con_no` | 702.889 đ | 702.889 đ | ✅ |
| 9 | `SUM(giam_tru)` toàn kỳ | 101.810 đ | 101.810 đ | ✅ |
| 10 | Doanh thu kỳ 6 | 23.828.605 đ | 23.828.605 đ | ✅ |
| 11 | Số hóa đơn | 58 | 58 | ✅ |
| 12 | Dòng `chi_tiet_hoa_don` "Giảm trừ" | 2 dòng, âm | −50.000 và −51.810 | ✅ |
| 13 | Trạng thái hai khoản | → `DA_AP_DUNG` | `DA_AP_DUNG` | ✅ |

**Mười ba dự đoán đúng, tất cả đúng tới từng đồng.**

Chiều ngược lại cũng đúng: huỷ hóa đơn lần nữa → cả hai khoản quay về `CHUA_AP_DUNG`, kỳ 6
còn 0 hóa đơn; lập lại → ra **đúng cùng con số** 101.810 đ / 23.828.605 đ. Đây là lần thứ tư
tính xác định của engine được chứng minh, và là lần đầu **có giảm trừ tham gia**.

## 21. Ràng buộc ① đã được thoả sẵn từ 4C — việc của E là kiểm chứng

Điểm đáng ghi lại: đọc kỹ `BillingService.tinhGiamTru` thì thấy nó **đã** làm đúng ràng buộc ①
từ Phase 4C — quy tỷ lệ thành số tiền tuyệt đối **đúng một lần**
(`lamTronTien(truocGiamTru × tyLe / 100)`), ghi vào `hoa_don.giam_tru`, rồi snapshot thành một
dòng `chi_tiet_hoa_don`. Không có phép nhân nào lặp lại về sau.

Vì vậy mục E **không sửa gì** ở tầng tính tiền. `GiamTruServiceImpl` cố ý **không** có phép
nhân nào với `tyLePhanTram` — thêm một phép quy đổi ở tầng nhập liệu chính là tạo ra tầng làm
tròn thứ hai mà ràng buộc ① cấm. Có test riêng khẳng định: lưu theo tỷ lệ thì cột `so_tien`
phải để **NULL**.

### 21.1. Một chỗ đặc tả chồng nhau, đã xử lý

Javadoc của `tinhGiamTru` (viết ở 4C) ghi: *"khi khai cả hai thì số tiền tuyệt đối thắng"*.
Đặc tả E.2 lại yêu cầu **chặn** việc khai cả hai. Hai câu không mâu thuẫn nhưng chồng nhau:
sau khi form chặn, nhánh "cả hai thì tuyệt đối thắng" trở thành **phòng thủ cho dữ liệu nhập
thẳng bằng SQL**, không còn là luật nghiệp vụ.

Giữ nguyên cả hai và ghi rõ ở đây, thay vì xoá nhánh phòng thủ: dữ liệu mẫu và các script
migration vẫn ghi thẳng vào bảng, nên nhánh đó còn có việc để làm. Ngoài ra `GiamTruServiceImpl`
ép cột còn lại về `NULL` khi lưu, nên bản ghi đi qua giao diện **không bao giờ** mang hai cách
khai cùng lúc.

## 22. Nghiệm thu mục E

| # | Tiêu chí | Kết quả |
|---|---|---|
| 1 | Danh sách, lọc theo kỳ / thuê bao / trạng thái | ✅ `/giam-tru` |
| 2 | Nhập số tiền **hoặc** tỷ lệ, chặn cả hai và chặn không nhập gì | ✅ `@AssertTrue` + 3 test |
| 3 | Chỉ sửa/xoá khi kỳ chưa chốt và khoản `CHUA_AP_DUNG` | ✅ 3 test chốt chặn |
| 4 | Tỷ lệ quy thành tiền đúng một lần lúc lập hóa đơn | ✅ đã có từ 4C, mục 21 |
| 5 | Kiểm chứng end-to-end khớp con số công bố trước | ✅ **13/13**, mục 20 |
| — | `mvnw test` | ✅ **242 test**, 0 lỗi |

---

# PHẦN V — MỤC F: DỮ LIỆU THANH TOÁN MẪU KỲ 5/2026

## 23. ⭐ Hai tiền đề của đặc tả không đúng — phát hiện trước khi viết dòng code nào

Đặc tả mục F đưa ra hai giả định, và **cả hai đều sai** khi đo trên kho mã thật. Đây là phần
đáng ghi lại nhất của mục F, vì nếu làm theo đặc tả từng chữ thì kết quả sẽ là *"chạy xong,
nhìn có vẻ đúng, và hỏng ngay lần đầu có người mở màn hình"*.

### 23.1. `data-mau.sql` không có lấy một dòng hóa đơn

Đặc tả yêu cầu *"dump các dòng thực tế trong `thanh_toan` cùng các cột đã đổi của `hoa_don`"*
rồi *"chạy profile `reset` → kiểm lại bất biến trên 112 hóa đơn"*. Câu thứ hai chỉ có nghĩa
nếu sau `reset` còn 112 hóa đơn.

Đo lại:

```
$ grep -c "INSERT INTO \(hoa_don\|chi_tiet_su_dung\|thanh_toan\)" data-mau.sql
0
```

Mục 10 của chính `data-mau.sql` nói thẳng điều đó: *"CHUA tao du lieu cho cac bang sau …
hoa_don, chi_tiet_hoa_don, thanh_toan → Phase 4, 5"*. Toàn bộ 8.714 CDR và 112 hóa đơn chỉ
tồn tại **trong CSDL đang chạy**, do Phase 3–5 sinh ra qua giao diện.

Hệ quả nặng hơn nhiều so với việc một tiêu chí không kiểm được:

| | |
|---|---|
| `CdrGeneratorService` dùng | `new Random()` — **không hạt giống** |
| Nên chạy `reset` rồi sinh lại CDR sẽ ra | bộ dữ liệu **khác hẳn** |
| Nghĩa là `reset` không phải "nạp lại dữ liệu mẫu" mà là | **xoá sổ** bộ dữ liệu mà cả `PHASE-4-REPORT.md` lẫn `PHASE-5-REPORT.md` dựa vào |

Dự án đã **né** profile `reset` hai lần vì đúng lý do này mà chưa lần nào gọi tên nó: mục 4D
ghi *"áp bằng `ALTER TABLE` chứ không dùng profile reset: reset sẽ sinh lại CDR ngẫu nhiên và
làm mất khả năng đối chiếu"*, mục G1 lặp lại nguyên câu đó. Hai lần đi vòng quanh một cái bẫy
là dấu hiệu cái bẫy cần được lấp, không phải cần đi vòng lần thứ ba.

**Xử lý:** thêm `db/data-van-hanh.sql` — bản dump của toàn bộ trạng thái vận hành — và nạp nó
ngay sau `data-mau.sql`. Ranh giới giữa hai file là ranh giới về **nguồn gốc**, không phải về
chủ đề:

| File | Nguồn gốc | Sửa tay |
|---|---|---|
| `data-mau.sql` | người viết ra | ✅ được |
| `data-van-hanh.sql` | máy sinh ra qua đường code nghiệp vụ | ❌ phải dump lại từ CSDL |

Từ đây `reset` **tái lập** đúng bộ dữ liệu mà báo cáo mô tả, thay vì phá nó.

### 23.2. `TT_MOT_PHAN` là trạng thái không thể tồn tại sau ngày hết hạn

Đặc tả yêu cầu ~15% hóa đơn kỳ 5 ở trạng thái `TT_MOT_PHAN`. Đọc `timCanChuyenQuaHan` thì thấy
điều đó **không thể xảy ra**:

```java
WHERE h.hanThanhToan < :homNay
  AND h.conNo > 0
  AND h.trangThai <> DA_TT
  AND h.trangThai <> QUA_HAN     // ← TT_MOT_PHAN lọt vào đây
```

Hóa đơn trả một phần vẫn còn nợ, không phải `DA_TT`, không phải `QUA_HAN` ⇒ **bị quét về
`QUA_HAN`**. Kỳ 5 hạn 15/06/2026, hôm nay 07/08/2026, và `capNhatQuaHan()` chạy mỗi lần có
người mở `/hoa-don` hoặc `/cong-no`. Nghĩa là nhóm 15% sẽ biến mất ở lần mở màn hình đầu tiên,
phân bố tụt xuống 60/0/40.

Điều trớ trêu: chính javadoc của `suyRaTrangThai` (viết ở mục C) đã mô tả đúng cơ chế này —
*"lịch chạy nền sẽ đưa nó lại về `QUA_HAN` nếu vẫn còn nợ sau hạn, nên không mất thông tin"* —
mà không nhận ra hệ quả của nó là **dòng chữ ngay phía trên trở thành vô nghĩa**. Câu *"hóa
đơn `QUA_HAN` trả một phần thì về `TT_MOT_PHAN`"* đúng trong khoảng vài mili giây, cho tới
lần quét kế tiếp.

**Xử lý đã chốt:** siết truy vấn để quét **chỉ chạm hóa đơn chưa thu đồng nào**.

```java
AND (h.daThanhToan IS NULL OR h.daThanhToan = 0)
```

Ranh giới đúng là: một phép quét đổi trạng thái **của tiền** chỉ được ghi đè khi chưa có đồng
tiền nào đi vào. Và thông tin *"đã quá hạn"* của hóa đơn trả dở **không mất** — màn hình công
nợ suy nó từ `han_thanh_toan` qua cột *số ngày quá hạn* và `NhomTuoiNo`, hoàn toàn độc lập với
cột trạng thái. Kiểm chứng ở mục 29: 8 hóa đơn `TT_MOT_PHAN` vẫn nằm đủ trong nhóm tuổi nợ
*"Quá hạn 31–60 ngày"*.

## 24. Cách làm: cho dữ liệu đi qua đúng đường code

Viết tay 54 câu `UPDATE` cho `da_thanh_toan` / `con_no` / `trang_thai` là làm đúng cái việc mà
ràng buộc ② sinh ra để cấm — `ThanhToanService` là **nơi duy nhất** ghi ba cột đó. Một bộ dữ
liệu mẫu lách qua nơi duy nhất ấy còn làm hỏng chính phép kiểm bất biến: nó sẽ chỉ xác nhận
rằng bộ SQL viết tay tự nhất quán, chứ không xác nhận gì về mã nghiệp vụ.

Cách làm lặp lại đúng cách mục 18.3 đã lấp hai cột `TRU_CUOC`:

```
lớp seed tạm  →  gọi ThanhToanService thật  →  kiểm bất biến  →  MỚI dump thành SQL  →  xoá lớp seed
```

Lớp seed là một test dùng một lần (`SinhThanhToanMauKy5Test`), đã **xoá sau khi dump**. Dự án
có sẵn tiền lệ cho kiểu lớp này: `ChayTinhCuocKyThuCong`, `ChayLapHoaDonKyThuCong` và
`ChayLaiToanBoKyThuCong` của Phase 4 cũng sinh ra để chạy một lần rồi xoá.

**Hạt giống cố định `20260601`.** Mọi lựa chọn ngẫu nhiên lấy từ một `Random` duy nhất; thuật
toán của `java.util.Random` và `Collections.shuffle` đều được đặc tả tường minh trong javadoc
JDK nên chạy lại trên máy khác vẫn ra đúng bộ này. Không có hạt giống thì mọi con số dưới đây
chỉ đúng cho đúng một lần chạy — đúng lỗi mà `CdrGeneratorService` đang mắc (mục 23.1).

Chia nhóm bằng cách **xáo trộn rồi cắt**, không bốc xác suất từng hóa đơn: bốc xác suất cho ra
số lượng dao động quanh 60/15/25 chứ không đúng bằng, và con số viết vào báo cáo khi đó phụ
thuộc may rủi.

| Nhóm | Công thức | Số hóa đơn |
|---|---|---|
| Trả đủ → `DA_TT` | `round(54 × 0,60)` | **32** |
| Trả một phần 30–70% → `TT_MOT_PHAN` | `round(54 × 0,15)` | **8** |
| Không trả → `QUA_HAN` | phần còn lại | **14** |

Trong 32 hóa đơn trả đủ, cứ 4 hóa đơn có 1 hóa đơn **trả làm hai đợt** (8 hóa đơn) — để tab
lịch sử thu tiền trên màn hình chi tiết có hóa đơn nhiều dòng để chụp ảnh, và để đường cộng
dồn `da_thanh_toan += soTien` thật sự được đi qua chứ không chỉ được viết ra.

## 25. ⭐ Kết quả

48 giao dịch = 32 (trả đủ) + 8 (đợt 2) + 8 (trả một phần).

| Chỉ tiêu kỳ 5/2026 | Giá trị |
|---|---|
| Tổng phát sinh | 21.289.162 đ |
| **Tổng đã thu** | **15.117.474 đ** |
| **Còn nợ** | **6.171.688 đ** |

| Trạng thái | Số hóa đơn | Tỷ lệ | Đã thu | Còn nợ |
|---|---|---|---|---|
| `DA_TT` | **32** | **59,3%** | 13.833.535 đ | 0 đ |
| `TT_MOT_PHAN` | **8** | **14,8%** | 1.283.939 đ | 1.607.961 đ |
| `QUA_HAN` | **14** | **25,9%** | 0 đ | 4.563.727 đ |

Ba hình thức thanh toán đều có mặt, phân bố 45/35/20 như đã đặt:

| Hình thức | Giao dịch | Số tiền |
|---|---|---|
| Tiền mặt | 23 | 7.042.665 đ |
| Chuyển khoản | 15 | 5.189.359 đ |
| Ví điện tử | 10 | 2.885.450 đ |

Ngày thu rải 01/06–20/06/2026, tức **có cả trước và sau hạn 15/06** — nhờ vậy dữ liệu mẫu
minh hoạ được cả khách trả đúng hạn lẫn khách trả muộn. Người thu là `ketoan01` trên cả 48
giao dịch.

**Bằng chứng luật siết ở mục 23.2 có hiệu lực.** Lớp seed gọi `capNhatQuaHan()` hai lần, trước
và sau khi thu tiền:

| Lần gọi | Số hóa đơn bị chuyển |
|---|---|
| Trước khi thu | **112** (toàn bộ, cả hai kỳ đều đã quá hạn) |
| Sau khi thu | **0** |

Số 0 ở dòng thứ hai chính là thứ cần chứng minh: 8 hóa đơn `TT_MOT_PHAN` **không** bị quét lại.
Với truy vấn cũ, con số đó sẽ là 8 và nhóm 14,8% biến mất.

## 26. Bất biến tiêu chí 4 — kiểm bằng hai nguồn độc lập

Câu SQL **nguyên văn** trong `PHASE-5-PLAN.md` ràng buộc ②, chạy trên toàn bộ 112 hóa đơn:

```
Số hóa đơn đã duyệt : 112
Số dòng lệch        : 0
```

Ngoài ra, phép kiểm này giờ là **test thường trực** chứ không phải một câu SQL chạy tay:
`KiemTraBatBienThanhToanTest` — 4 test chạy trên CSDL thật.

| # | Nội dung |
|---|---|
| 1 | Mọi hóa đơn: `con_no = tong_thanh_toan − da_thanh_toan` |
| 2 | Mọi hóa đơn: `da_thanh_toan = SUM(thanh_toan.so_tien)` |
| 3 | Trạng thái khớp số còn nợ: `DA_TT` khi và chỉ khi `con_no = 0` |
| 4 | ⭐ Không hóa đơn nào đã thu tiền mà bị quét về `QUA_HAN` — giữ luật mục 23.2 |

Ràng buộc ② đòi chạy phép kiểm này **sau mỗi mục**. Từ mục A tới E nó đúng một cách **rỗng**
vì bảng `thanh_toan` không có dòng nào — mà một bất biến chưa từng có dữ liệu để kiểm thì chưa
chứng minh được gì (bài học 43.5). Mục F là lúc nó có việc thật, và test 4 là chỗ nó bắt được
một luật đã suýt bị đảo ngược.

## 27. ⭐ Kết tinh thành SQL — và bằng chứng nó tương đương đường code

`data-van-hanh.sql`: **1.107.256 byte**, 264 dòng, 72 câu `INSERT` + 4 câu `UPDATE`.

| Mục | Bảng | Số dòng |
|---|---|---|
| 1 | `chi_tiet_su_dung` | 8.714 |
| 2 | `giam_tru` | 2 |
| 3 | `hoa_don` | 112 |
| 4 | `chi_tiet_hoa_don` | 266 |
| 5 | `thanh_toan` | **48** |
| 6 | `bien_dong_so_du` (`TRU_CUOC`) | 16 |
| 7 | `UPDATE thue_bao.so_du` | áp sổ cái lên số dư |
| 8 | `UPDATE ky_cuoc` | trạng thái và số liệu tổng hợp |

Hai chỗ **cố ý không chép số tuyệt đối**:

- **`thue_bao.so_du`** suy từ chính sổ cái nằm ngay phía trên bằng một câu `UPDATE … JOIN`.
  Viết 16 con số tuyệt đối là tạo ra cơ hội cho chúng lệch khỏi sổ cái; câu lệnh này không thể
  lệch, vì nó **chính là** phép tính mà bất biến G1 đòi hỏi.
- **`nhat_ky_he_thong`** để trống. Nó là vết thao tác của người dùng; tái lập vết đó trong dữ
  liệu mẫu là dựng ra một lịch sử không ai thực hiện.

### 27.1. Bằng chứng: so từng dòng, không so số tổng

Chạy `reset` rồi so **ảnh chụp toàn bộ CSDL** trước và sau:

| | |
|---|---|
| Ảnh chụp từ đường code (sau khi seed) | 9.455 dòng |
| Ảnh chụp sau khi chạy `reset` | 9.455 dòng |
| **Khác biệt** | **0** |

Không phải *"số dòng bằng nhau"* mà là **giống hệt từng dòng một** trên cả 14 bảng — đúng
chuẩn làm việc 43.3. Bất biến tiêu chí 4 chạy lại trên CSDL vừa dựng: **0 lệch / 112 hóa đơn**.
Bất biến sổ cái G1 cũng vậy: **0 lệch / 80 thuê bao**.

Phép so này được chạy **ba lần**, và lần thứ ba là lần có giá trị nhất:

| Lần | Sau việc gì | Kết quả |
|---|---|---|
| 1 | `reset` lần đầu | 0 khác biệt |
| 2 | `reset` sau khi đã huỷ + lập lại hóa đơn kỳ 6 | 0 khác biệt |
| 3 | **Sau khi chạy trọn 246 test** | 0 khác biệt |

Lần 3 khẳng định thêm một điều không hiển nhiên: bộ test chạy trên CSDL thật nhưng **không để
lại dấu vết nào** trên dữ liệu mẫu.

## 28. Ràng buộc kỳ 6 — kiểm bằng đối chứng, không chỉ bằng phép đếm

| Phép kiểm | Kết quả |
|---|---|
| Số giao dịch thanh toán của kỳ 6 | **0** |
| Huỷ hóa đơn **kỳ 5** | ❌ **bị chặn** — 54 hóa đơn còn nguyên |
| Huỷ hóa đơn **kỳ 6** | ✅ chạy được — còn 0 hóa đơn, 2/2 khoản giảm trừ trả về `CHUA_AP_DUNG` |
| Lập lại kỳ 6 | ✅ **58 hóa đơn, 23.828.605 đ** — đúng từng đồng con số cũ |
| Bất biến tiêu chí 4 sau vòng huỷ/lập | ✅ 0 lệch / 112 |

Dòng thứ hai là **đối chứng**, và nó quan trọng ngang dòng thứ ba: nếu chỉ kiểm *"kỳ 6 huỷ
được"* thì phép kiểm vẫn xanh ngay cả khi chốt chặn đã hỏng hoàn toàn. Phải cho thấy chốt chặn
**có** chặn ở nơi đáng chặn thì mới biết nó còn sống.

Đây cũng là lần thứ năm tính xác định của engine được chứng minh, và là lần đầu chứng minh
trong lúc **một kỳ khác đã có thanh toán**.

Cách kiểm cố ý **không đọc chuỗi thông báo lỗi** mà đọc **hậu quả** (số hóa đơn còn lại). Đọc
chuỗi thì phép kiểm gắn vào câu chữ tiếng Việt — câu chữ đổi là phép kiểm đỏ oan, mà đỏ oan
thì lần sau không ai tin nó nữa (bài học 43.5).

## 29. Màn hình công nợ và bảng tuổi nợ

`scripts/test-muc-F.ps1` — **17 phép kiểm, 17 đạt**. Mọi con số đọc trên màn hình đều được đối
chiếu chéo bằng một câu SQL độc lập, thay vì tin vào chính cái màn hình đang kiểm.

| Chỉ tiêu | Màn hình | SQL độc lập |
|---|---|---|
| Tổng công nợ | 30.000.293 đ | 30.000.293 đ ✅ |
| Số hóa đơn còn nợ | 80 | 80 ✅ |
| Nhóm *Quá hạn 1–30 ngày* | 23.828.605 đ | 58 hóa đơn kỳ 6 ✅ |
| Nhóm *Quá hạn 31–60 ngày* | 6.171.688 đ | 22 hóa đơn kỳ 5 ✅ |

Con số 22 của nhóm 31–60 ngày là **14 `QUA_HAN` + 8 `TT_MOT_PHAN`** — bằng chứng cụ thể cho
lập luận ở mục 23.2: 8 hóa đơn trả dở vẫn được xếp đúng nhóm tuổi nợ dù cột trạng thái của
chúng không còn ghi chữ *"Quá hạn"*. Thông tin quá hạn nằm ở ngày, không nằm ở enum.

### 29.1. Hạn chế đã biết: chỉ 2 trong 5 nhóm tuổi nợ có nội dung

Hệ thống mới có hai kỳ cước, hạn 15/06 và 15/07/2026. So với mốc 07/08/2026 thì chúng cách 53
và 23 ngày, rơi đúng vào hai nhóm giữa. Ba nhóm *Trong hạn*, *61–90 ngày* và *trên 90 ngày*
**rỗng**, và đó là sự thật số học chứ không phải lỗi.

Làm cho chúng có nội dung đòi hỏi bịa thêm một kỳ cước cũ hơn hoặc sửa `han_thanh_toan` — cả
hai đều là **dựng dữ liệu cho khớp ảnh chụp**, đúng loại lỗi mà mục 10.1 đã phê phán ở 4F. Ghi
lại thành nợ cho Phase 6: nếu cần biểu đồ aging đủ 5 cột thì phải thêm một kỳ cước thật.

## 30. Nghiệm thu mục F

| # | Tiêu chí | Kết quả |
|---|---|---|
| 1 | `mvnw test` PASS, không giảm số test | ✅ **246 test** (242 → 246), 0 lỗi, BUILD SUCCESS |
| 2 | Bất biến 0 lệch trên 112 hóa đơn, trước **và** sau `reset` | ✅ 0 lệch cả hai lần; thêm so từng dòng 9.455/9.455 |
| 3 | Kỳ 5 phân bố ≈ 60/15/25, có hóa đơn `QUA_HAN` | ✅ **59,3 / 14,8 / 25,9** — 14 hóa đơn `QUA_HAN` |
| 4 | Kỳ 6: 0 thanh toán, vẫn huỷ được hóa đơn | ✅ 0 giao dịch; huỷ + lập lại ra đúng 58 hóa đơn / 23.828.605 đ |
| 5 | Màn hình công nợ và biểu đồ aging có dữ liệu thật | ✅ 30.000.293 đ / 80 hóa đơn; 2/5 nhóm có nội dung — xem 29.1 |

---

# PHẦN VI — TỔNG KẾT PHASE 5

## 31. Bàn giao cho Phase 6

### 31.1. Trạng thái dữ liệu

| Bảng | Số bản ghi | Ghi chú |
|---|---|---|
| `khach_hang` · `thue_bao` · `goi_cuoc` | 50 · 80 · 5 | Không đổi từ Phase 1 |
| `bang_gia_cuoc` | 10 | Không đổi từ 4A |
| `ky_cuoc` | 3 | **5/2026 đã chốt** · **6/2026 mở** · 7/2026 chưa dùng |
| `chi_tiet_su_dung` | **8.714** | 3.697 kỳ 5 + 5.017 kỳ 6, tất cả `DA_TINH` |
| `hoa_don` | **112** | 54 kỳ 5 + 58 kỳ 6 — **không còn hóa đơn nào `CHUA_TT`** |
| `chi_tiet_hoa_don` | **266** | 264 dòng cước + 2 dòng "Giảm trừ" thành tiền âm |
| `thanh_toan` | **48** | Toàn bộ thuộc kỳ 5 — kỳ 6 **cố ý để trống** |
| `bien_dong_so_du` | **34** | 18 dòng mở sổ `DIEU_CHINH` + 16 dòng `TRU_CUOC` kỳ 6 |
| `giam_tru` | **2** | Cả hai `DA_AP_DUNG` cho kỳ 6 |
| `nhat_ky_he_thong` | phát sinh khi vận hành | Cố ý không đưa vào dữ liệu mẫu |

**Tiền — số liệu Phase 6 sẽ dựng báo cáo lên trên:**

| Chỉ tiêu | Kỳ 5/2026 | Kỳ 6/2026 | Tổng |
|---|---|---|---|
| Doanh thu (tổng thanh toán) | 21.289.162 đ | 23.828.605 đ | **45.117.767 đ** |
| Đã thu | **15.117.474 đ** | 0 đ | **15.117.474 đ** |
| Còn nợ | **6.171.688 đ** | 23.828.605 đ | **30.000.293 đ** |
| Tỷ lệ thu được | **71,0%** | 0% | 33,5% |
| Giảm trừ | 0 đ | 101.810 đ | 101.810 đ |

**Phân bố trạng thái hóa đơn** — dữ liệu cho biểu đồ tròn của dashboard Phase 6:

| Trạng thái | Kỳ 5 | Kỳ 6 | Tổng |
|---|---|---|---|
| `DA_TT` | 32 | 0 | 32 |
| `TT_MOT_PHAN` | 8 | 0 | 8 |
| `QUA_HAN` | 14 | 58 | 72 |
| `CHUA_TT` | 0 | 0 | 0 |

**Bảng tuổi nợ tại mốc 07/08/2026:**

| Nhóm | Số hóa đơn | Số tiền |
|---|---|---|
| Trong hạn | 0 | 0 đ |
| Quá hạn 1–30 ngày | 58 | 23.828.605 đ |
| Quá hạn 31–60 ngày | 22 | 6.171.688 đ |
| Quá hạn 61–90 ngày | 0 | 0 đ |
| Quá hạn trên 90 ngày | 0 | 0 đ |

**Số dư trả trước:** 20 thuê bao, tổng còn **2.532.583 đ** sau khi trừ cước kỳ 6; **6** thuê
bao dưới ngưỡng cảnh báo 50.000 đ (đếm theo đúng điều kiện của `timSoDuDuoiNguong`, tức đã
loại thuê bao `DA_THANH_LY`).

### 31.2. Bốn việc Phase 6 nhận từ Phase 5

1. **Bộ dữ liệu giờ tái lập được.** Chạy `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"`
   là dựng lại **đúng** bộ số trong báo cáo này, không còn phải sinh lại CDR ngẫu nhiên. Đây là
   thứ Phase 4 và mục G đều không có.
2. **Hai view CSDL chưa ai dùng.** `v_thong_ke_thue_bao` và `v_doanh_thu_thang` có từ Phase 1,
   `v_doanh_thu_thang` giờ đã có số thật (cả `da_thu` lẫn `con_no`). Phase 6 nên đọc chúng
   thay vì viết truy vấn tổng hợp mới — hai cách tính là hai nguồn sự thật.
3. **Kỳ 6 còn nguyên khả năng demo trọn vòng.** Không có giao dịch nào nên `huyBillingKy` vẫn
   chạy. **Đừng ghi nhận thanh toán cho kỳ 6** nếu chưa chụp xong ảnh vòng huỷ/lập.
4. **Nợ tài liệu:** báo cáo này **chưa có phần viết cho mục A, B, C, D**. Phần IV mở đầu bằng
   mục 19 (mục E) mà không có mục nào trước đó — bốn mục ấy đã làm và đã commit nhưng chưa
   được viết lại. Xem mục 35.

### 31.3. Lưu ý khi đổi dữ liệu

`data-van-hanh.sql` là **bản dump**, không phải file soạn tay. Muốn đổi dữ liệu vận hành thì
đổi qua giao diện hoặc qua service rồi dump lại cả file — sửa tay một con số trong đó là dựng
ra nguồn sự thật thứ hai cạnh đoạn mã sinh ra nó.

## 32. Danh sách màn hình chụp ảnh cho toàn Phase 5

Đăng nhập `admin`. **Nhóm 1** là các ảnh nên đưa vào phần trình bày chính.

### Nhóm 1 — Ảnh bắt buộc ⭐

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 1 | **Công nợ + biểu đồ tuổi nợ** | `/cong-no` | Tổng **30.000.293 đ / 80 hóa đơn**; biểu đồ có hai cột thật; bảng aging đủ 5 nhóm |
| 2 | **Danh sách hóa đơn kỳ 5** | `/hoa-don?kyCuocId=2` | Đủ **ba** badge khác nhau: Đã thanh toán / Thanh toán một phần / Quá hạn |
| 3 | **Chi tiết hóa đơn trả hai đợt** | `/hoa-don/307` | Tab lịch sử thu tiền có **2 dòng**; `đã thu` + `còn nợ` = `tổng thanh toán` |
| 4 | **Chi tiết hóa đơn trả một phần** | Lọc `trangThai=TT_MOT_PHAN`, mở một hóa đơn | Còn nợ > 0 nhưng badge là *Thanh toán một phần*, **không** phải *Quá hạn* |
| 5 | **Hóa đơn PDF** | Nút *Xuất PDF* trên ảnh 3 | Dấu tiếng Việt hiện đủ — bằng chứng font đã nhúng (mục 34.3) |
| 6 | **Phiếu thu PDF** | `/thanh-toan` → một giao dịch → *In phiếu thu* | Số tiền bằng chữ; tiêu đề *PHIẾU THU TIỀN* liền mạch |
| 7 | **Danh sách giao dịch thanh toán** | `/thanh-toan` | 48 giao dịch, đủ 3 hình thức, cột người thu là `ketoan01`, dòng tổng 15.117.474 đ |

### Nhóm 2 — Chức năng và chốt chặn

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 8 | Form ghi nhận thanh toán | `/thanh-toan/moi?hoaDonId=…` | Số còn nợ hiện sẵn để đối chiếu |
| 9 | Chặn thu vượt số còn nợ | Nhập số tiền lớn hơn còn nợ | Thông báo *"vượt quá số còn nợ"* |
| 10 | **Chặn huỷ hóa đơn kỳ đã thu tiền** | `/tinh-cuoc` → *Huỷ hóa đơn* kỳ 5 | Thông báo nêu rõ **48 giao dịch** — chốt chặn nói ra con số |
| 11 | Huỷ + lập lại hóa đơn kỳ 6 | `/tinh-cuoc` → kỳ 6 | Ra đúng 58 hóa đơn / 23.828.605 đ |
| 12 | Quản lý giảm trừ | `/giam-tru` | Hai khoản `DA_AP_DUNG`, một khai tiền một khai tỷ lệ |
| 13 | Chặn khai cả tiền lẫn tỷ lệ | `/giam-tru/moi`, nhập cả hai | Thông báo từ `@AssertTrue` |
| 14 | Đề xuất tạm ngừng vì nợ cước | `/cong-no`, khối cuối trang | Danh sách thuê bao quá ngưỡng, kèm ghi chú cảnh báo không chặn nghiệp vụ |
| 15 | Biến động số dư | `/thue-bao/4` → tab *Biến động số dư* | Dòng `TRU_CUOC` 204.780 đ, số dư 205.000 → 220 đ, **10.446 đ** không thu được |
| 16 | Cảnh báo số dư thấp | `/tinh-cuoc`, khối cuối trang | 6 thuê bao dưới ngưỡng, có icon và chữ chứ không chỉ màu |

### Nhóm 3 — Minh chứng kỹ thuật

| # | Ảnh | Cách lấy |
|---|---|---|
| 17 | **Kết quả 246 test** | Console `mvnw test`, phóng to dòng `Tests run: 246, Failures: 0` |
| 18 | **Test bất biến thanh toán** | Chạy `KiemTraBatBienThanhToanTest`, 4 test xanh |
| 19 | Test bất biến sổ cái số dư | Chạy `KiemTraSoCaiSoDuTest`, 2 test xanh |
| 20 | **`test-muc-F.ps1` — 17 đạt / 0 sai** | `.\scripts\test-muc-F.ps1` khi app đang bật |
| 21 | Lịch sử Git Phase 5 | `git log --oneline -12` |

## 33. ⭐ Năm điểm sai lệch đặc tả phát hiện ở Phase 5

| # | Ở đâu | Vấn đề | Xử lý |
|---|---|---|---|
| 1 | Kế hoạch G3 | Dự kiến *"≥ 3 thuê bao 18.000/20.000/22.000 đ hết số dư giữa chừng"*. Cả ba đều `TAM_NGUNG_1C` nên gần như không phát sinh cước — số dư thấp không bao giờ bị chạm tới | Đo lại, ca thật nằm ở thuê bao **4** với lý do ngược hẳn: lưu lượng cao. Ghi ở mục 10 |
| 2 | Javadoc `tinhGiamTru` vs đặc tả E.2 | *"Khai cả hai thì số tiền tuyệt đối thắng"* chồng lên yêu cầu **chặn** khai cả hai | Giữ cả hai, nhánh cũ thành phòng thủ cho dữ liệu nhập thẳng bằng SQL. Mục 21.1 |
| 3 | Đặc tả mục F | Giả định `data-mau.sql` có sẵn hóa đơn để `UPDATE`. Thực tế nó **không có dòng nào**, và `reset` là thao tác **xoá sổ** bộ dữ liệu của báo cáo | Thêm `data-van-hanh.sql`. Mục 23.1 |
| 4 | Mục A vs đặc tả mục F | Quét quá hạn ghi đè cả hóa đơn trả một phần ⇒ `TT_MOT_PHAN` **không thể tồn tại** sau hạn, tiêu chí 15% không đạt được | Siết truy vấn còn hóa đơn chưa thu đồng nào. Mục 23.2 |
| 5 | `scripts/_chung.ps1` từ Phase 2 | `Kiem-Tra` báo kết quả bằng `Write-Output` rồi bị `\| Out-Null` nuốt mất ⇒ phép kiểm **trượt** chỉ làm biến đếm tăng, không bao giờ nói trượt ở đâu | Đổi sang `Write-Host`. Mục 34.1 |

## 34. ⭐ Bài học phương pháp Phase 5

### 34.1. Bài học 43.5 — lần thứ ba và thứ tư, cùng một hình dạng

Bài học 43.5 nói *"một phép kiểm sai nguy hiểm ngang thiếu phép kiểm"*. Phase 5 gặp lại nó
**bốn** lần, và bốn lần đều ở dạng khác nhau:

| Lần | Ở đâu | Phép kiểm sai thế nào |
|---|---|---|
| 1 (Phase 4) | Bảng đối soát | — |
| 2 | Kiểm chốt chặn trừ cước (mục 15.1) | Script đọc flash attribute ở lần `GET` thứ hai, sau khi nó đã bị tiêu thụ ⇒ **báo động giả** |
| 3 | `TrichVanBanPdf` — `setSortByPosition` | Bật sắp-theo-vị-trí để đọc PDF đẹp hơn, nhưng heuristic của nó chèn khoảng trắng giả vào giữa chữ in đậm cỡ lớn: `"PHIẾU THU TIỀN"` trích ra thành `"PHIẾU T HU TIỀN"` trong khi **bản in hoàn toàn bình thường** ⇒ **báo động giả** |
| 4 | `_chung.ps1` + `test-tb.ps1` | Phép kiểm **trượt trong im lặng** |

**Lần 3** đáng nhớ vì nếu tin phép kiểm thì sẽ đi sửa layout của một bản in đang đúng. Cách xử
lý: bỏ hẳn `setSortByPosition` và giữ hành vi mặc định — thứ tự đọc không có giá trị gì với
các khẳng định `contains`, nên bật nó lên chỉ mua thêm rủi ro mà không mua được gì.

**Lần 4 là dạng nguy hiểm nhất vì nó ngược chiều ba lần trước.** Ba lần đầu là *báo động giả*
— phép kiểm đỏ oan, gây khó chịu nhưng lộ ra ngay. Lần này là *im lặng oan*: `Kiem-Tra` vừa
in kết quả vừa `return $true/$false`, mà trong PowerShell cả hai đi vào **cùng một luồng
output**, nên `| Out-Null` ở mọi nơi gọi nuốt luôn dòng `[SAI ]`.

Sửa xong một dòng (`Write-Output` → `Write-Host`) thì lộ ra ngay một phép kiểm **đã trượt từ
mục G4**: `test-tb.ps1` còn dò tên tab cũ *"Lịch sử nạp tiền"* trong khi mục G4 đã đổi thành
*"Biến động số dư"*. Nó trượt âm thầm qua toàn bộ mục G và mục A–E.

Và phép kiểm đối xứng ngay dưới nó còn tệ hơn: `-KhongDuocCo @('Lịch sử nạp tiền')` cho thuê
bao trả sau — sau khi chuỗi đó biến mất khỏi toàn bộ mã nguồn, phép kiểm này trở thành thứ
**không bao giờ có thể đỏ**. Nó vẫn đếm là một phép kiểm đạt, và không kiểm gì cả.

> **Rút ra:** một phép kiểm bám vào **chuỗi hiển thị** là một phép kiểm sẽ mục nát. `test-muc-F.ps1`
> vì vậy bám vào `id`, `value` và đường dẫn — thứ không đổi khi câu chữ đổi — hoặc bám vào
> **hậu quả đo được bằng SQL**, không bám vào thông báo.

### 34.2. Đo tiền đề của đặc tả trước, đừng đo sau

Cả hai vấn đề lớn nhất của mục F (23.1 và 23.2) đều **không phải lỗi cài đặt** — chúng là
những câu đặc tả mô tả một kho mã không tồn tại. Cách phát hiện giống hệt nhau: trước khi viết
dòng code nào, đọc lại chỗ mà đặc tả **giả định** là đã có, rồi chạy một câu lệnh kiểm.

Chi phí: hai câu `grep` và một câu `SELECT`. Nếu bỏ qua, chi phí là chạy xong toàn bộ mục F,
dump ra SQL, rồi mất sạch dữ liệu ở bước `reset` cuối cùng — với báo cáo Phase 4 mất theo.

Đây là bài học 43.4 (*công bố dự đoán trước khi viết code*) áp cho một đối tượng khác: không
chỉ dự đoán **kết quả**, mà kiểm cả **tiền đề**.

### 34.3. Bản quyền font — một ràng buộc kỹ thuật đến từ ngoài kỹ thuật

Mục B phải nhúng font vào PDF: PDF chỉ hiện đúng dấu tiếng Việt khi font được nhúng kèm bảng
mã `Identity-H`. Dùng font Base14 có sẵn (Helvetica, Times) thì mọi ký tự có dấu ra ô vuông
hoặc mất dấu — **và lỗi này không làm hỏng file**, PDF vẫn mở bình thường.

Lựa chọn hiển nhiên là Times New Roman hoặc Arial trong `C:\Windows\Fonts`. Đó là lựa chọn
**sai về pháp lý**: hai font đó do Monotype/Microsoft cấp phép và **không được phép phát hành
lại**. Kho mã này đặt trên GitHub công khai, nên đóng gói chúng vào là vi phạm bản quyền.

Đã dùng **Liberation Sans** trích từ chính jar `pdfbox:3.0.3`, giấy phép **SIL Open Font
License 1.1** cho phép phát hành lại tự do kể cả kèm trong phần mềm. Độ phủ tiếng Việt đã kiểm
bằng `Font.canDisplay` trên chuỗi mẫu đủ dấu, em-dash và ký hiệu đồng: **0 ký tự không hiển
thị được**. Xuất xứ và lý do ghi trong `src/main/resources/fonts/NGUON-FONT.txt`.

> Hai điều rút ra. Thứ nhất: **ràng buộc đến từ ngoài kỹ thuật vẫn là ràng buộc kỹ thuật** —
> nó quyết định file nào được nằm trong kho mã. Thứ hai: vì lỗi thiếu font **không làm hỏng
> file**, test bắt buộc phải **đọc lại nội dung PDF và so khớp chuỗi có dấu**, chứ kiểm "file
> tồn tại và mở được" thì sẽ xanh trên một bản in đầy ô vuông.

### 34.4. Dữ liệu mẫu phải đi qua đường code, và phải có hạt giống cố định

Hai nửa của cùng một bài học, cả hai đều gặp ở mục F:

- **Đi qua đường code** (mục 24): dữ liệu mẫu viết tay lách qua service sẽ làm phép kiểm bất
  biến trở thành vô nghĩa — nó chỉ xác nhận bộ SQL tự nhất quán.
- **Hạt giống cố định** (mục 23.1): dữ liệu sinh ngẫu nhiên **không hạt giống** thì mọi con số
  trong báo cáo chỉ đúng cho đúng một lần chạy, và không ai kiểm lại được.

`CdrGeneratorService` mắc lỗi thứ hai từ Phase 3 và hậu quả chỉ lộ ra ở Phase 5, dưới dạng
*"không dám chạy `reset`"*. Một khiếm khuyết không gây lỗi nào cả — nó chỉ lặng lẽ làm mất khả
năng tái lập, và biểu hiện ra ngoài thành một thói quen né tránh.

## 35. Nợ tài liệu của Phase 5

| # | Nợ | Ghi chú |
|---|---|---|
| 1 | ~~Chưa viết phần báo cáo cho mục A~~ | ✅ **Đã trả** — xem [Phần IV-A](#phần-iv-a--mục-a-màn-hình-hóa-đơn) |
| 1b | **Chưa viết phần báo cáo cho mục B, C, D** | Ba mục đã làm và đã commit (`acc8486`, `d98366c`, `d231088`) nhưng chưa có phần viết |
| 2 | Biểu đồ aging chỉ có 2/5 nhóm có nội dung | Cần thêm một kỳ cước cũ hơn — xem 29.1 |
| 3 | `CdrGeneratorService` chưa có hạt giống cố định | Không còn chặn `reset` nữa vì đã có `data-van-hanh.sql`, nhưng sinh CDR mới vẫn không tái lập được |
| 4 | `DIEU_CHINH` chỉ cộng, không trừ | Từ mục 17, chưa đổi |
| 5 | Trừ cước theo kỳ, không real-time | Từ mục 17, thuộc "Hướng phát triển" |
