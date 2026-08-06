# BÁO CÁO PHASE 5 — HÓA ĐƠN, THANH TOÁN, CÔNG NỢ

> Báo cáo đang xây dựng. Mục A–F chưa được giao; hiện có mục A0 và mục G.
> Kế hoạch và ba ràng buộc bắt buộc: [`PHASE-5-PLAN.md`](PHASE-5-PLAN.md).

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
