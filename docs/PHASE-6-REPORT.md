# BÁO CÁO PHASE 6 — BÁO CÁO, THỐNG KÊ, DASHBOARD

> Báo cáo đang xây dựng. Bàn giao từ Phase 5: [`PHASE-5-REPORT.md`](PHASE-5-REPORT.md) mục 31.

---

# PHẦN I — MỤC A: NỀN DỮ LIỆU CHO BÁO CÁO

## 1. ⭐ Hai tiền đề của đặc tả không đúng — đo trước khi viết code

Giống mục F của Phase 5, hai câu trong đặc tả mô tả một kho mã khác với kho mã đang có. Cả hai
đã được nêu ra và chốt phương án **trước** khi chạy.

### 1.1. Thêm kỳ 3 và 4 chỉ cho **bốn** trong năm nhóm tuổi nợ

Đặc tả đặt mục tiêu *"bảng tuổi nợ đủ 5 nhóm một cách TỰ NHIÊN"* bằng cách thêm hai kỳ. Đo lại
với mốc **10/08/2026**:

| Kỳ | Hạn thanh toán | Số ngày quá hạn | Nhóm |
|---|---|---|---|
| 3/2026 | 15/04/2026 | 117 | Quá hạn trên 90 ✅ |
| 4/2026 | 15/05/2026 | 87 | Quá hạn 61–90 ✅ |
| 5/2026 | 15/06/2026 | 56 | Quá hạn 31–60 ✅ |
| 6/2026 | 15/07/2026 | 26 | Quá hạn 1–30 ✅ |
| — | — | — | **Trong hạn ❌ rỗng** |

Nhóm *Trong hạn* đòi một hóa đơn **chưa tới hạn**, mà bốn kỳ trên đều đã quá hạn. Không có
cách nào lấp nó bằng kỳ cũ.

**Phương án đã chốt:** lập hóa đơn cho cả **kỳ 7/2026** — kỳ này đã tồn tại sẵn trong dữ liệu
mẫu ở trạng thái `MO` với 0 hóa đơn. Hóa đơn lập 31/07 → hạn 15/08, còn 5 ngày nữa mới tới
hạn, rơi đúng vào nhóm *Trong hạn*. Hệ quả phụ: biểu đồ doanh thu có **5 cột** thay vì 4 như
đặc tả viết.

### 1.2. ⚠️ "Đủ 5 nhóm" là tính chất của NGÀY XEM, không phải của dữ liệu

Đây là điểm quan trọng hơn, và nó không mất đi sau khi đã lấp đủ 5 nhóm.

Các dải tuổi nợ rộng **đúng 30 ngày**, trong khi các kỳ cước cách nhau **30–31 ngày**. Độ lệch
đó tích lại. Đặt *t* = số ngày kể từ 15/07/2026 (hạn của kỳ 6), năm kỳ nằm trên năm nhóm khác
nhau khi và chỉ khi:

```
kỳ 7 trong hạn      : t − 31 ≤ 0   →  t ≤ 31
kỳ 6 nhóm 1–30      : 1 ≤ t ≤ 30
kỳ 5 nhóm 31–60     : 31 ≤ t + 30 ≤ 60   →  1 ≤ t ≤ 30
kỳ 4 nhóm 61–90     : 61 ≤ t + 61 ≤ 90   →  0 ≤ t ≤ 29
kỳ 3 nhóm > 90      : t + 91 ≥ 91        →  t ≥ 0
                                    ⇒  1 ≤ t ≤ 29
```

Hôm nay *t* = 26, nên đủ 5 nhóm. Nhưng **từ 14/08/2026** (*t* = 30) kỳ 4 nhảy sang nhóm >90 và
nhóm 61–90 rỗng; đến 15/08 thì kỳ 6 rời nhóm 1–30 và đến lượt nhóm đó rỗng.

Nói cách khác: **cửa sổ chỉ rộng 29 ngày, và hôm nay đã ở ngày thứ 26.** Ảnh chụp bảng aging
phải lấy **trước 14/08/2026**, nếu không sẽ chỉ còn 4 nhóm có nội dung — mà đó không phải lỗi
của ai, đó là số học.

Ghi lại thay vì lặng lẽ chấp nhận, vì người đọc báo cáo về sau mở hệ thống lên sẽ thấy con số
khác ảnh chụp và tưởng có gì hỏng.

### 1.3. Trừ cước trả trước cho kỳ 3 và 4 bị **chặn có chủ đích**

Đặc tả A.2 yêu cầu chạy trừ cước trả trước cho hai kỳ mới. Việc đó **bị từ chối** bởi chốt
chặn thứ hai của `TruCuocTraTruocService.kiemTraTruocKhiChay`: kỳ 6 đã trừ cước rồi, mà trừ
cước **không giao hoán theo kỳ** — số dư vào mỗi kỳ phụ thuộc kỳ trước đó.

Đây đúng là tình huống mục 11 của báo cáo Phase 5 đã dự báo, chỉ khác là nó xảy ra với kỳ 3 và
4 thay vì kỳ 5.

**Phương án đã chốt: bỏ trừ cước cho các kỳ mới.** Lý do:

1. Thuê bao trả trước **không có hóa đơn** (quyết định 5.4), nên việc này không ảnh hưởng một
   con số nào của bất kỳ báo cáo Phase 6 nào — doanh thu, công nợ, tuổi nợ đều dựng trên
   `hoa_don`.
2. Cách còn lại là huỷ trừ cước kỳ 6 rồi chạy lại đúng thứ tự 3→4→5→6. Tổng cước bốn kỳ của
   thuê bao trả trước vượt xa tổng số dư mở sổ (4.524.000 đ), nên phần lớn thuê bao sẽ hết số
   dư giữa chừng và **mọi con số kỳ 6 đã kiểm chứng ở mục 14 và 18.3 của Phase 5 sẽ sai** —
   trong đó có kết quả đối chiếu dự đoán 7/7.

Đổi lấy: bốn kỳ có CDR trả trước đã định giá mà chưa trừ vào số dư. Đây là **nợ tài liệu**, ghi
ở mục cuối, không phải một bất biến bị vi phạm — bất biến sổ cái vẫn đúng vì nó chỉ ràng buộc
`so_du` với các dòng **đã ghi**.

## 2. Mục A1 — hạt giống cho bộ sinh CDR

Khoản nợ số 3 của Phase 5. Tới hết Phase 5, `CdrGeneratorService` giữ một `Random` khởi tạo
bằng `new Random()` ở mức **field**: bộ CDR sinh ra không tái lập được, và một field khả biến
dùng chung nằm trong một singleton.

Nay `Random` là **biến cục bộ** của đúng lần chạy, khởi tạo từ hạt giống người dùng nhập — hoặc
từ hạt giống hệ thống tự bốc rồi **trả về trong kết quả** và ghi vào nhật ký. Một lần sinh
"ngẫu nhiên" vẫn nói ra được con số dựng lại chính nó.

**Một điều kiện không hiển nhiên đi kèm.** `layThueBaoHopLe()` nay **sắp theo `id`**. Bộ sinh
bốc thuê bao bằng chỉ số trong danh sách, mà hai truy vấn nguồn (`findByTrangThai`,
`findAllById`) đều **không có `ORDER BY`** — thứ tự chúng trả về là chi tiết cài đặt của CSDL.
Không sắp thì hạt giống không bảo đảm được gì, và nó sẽ hỏng vào một ngày không ai đoán trước.

`CdrGeneratorHatGiongTest` — 3 test trên CSDL thật:

| # | Nội dung |
|---|---|
| 1 | Cùng hạt giống → đúng cùng bộ bản ghi, so **từng dòng** chứ không so số lượng |
| 2 | Hạt giống khác → bộ dữ liệu khác — chứng minh test 1 không xanh rỗng |
| 3 | Bỏ trống hạt giống vẫn tái lập được bằng con số hệ thống trả về |

Test dùng `@Transactional` nên Spring rollback sau mỗi phương thức; đã kiểm lại sau khi chạy:
**0 bản ghi sót**, tổng CDR vẫn 8.714.

## 3. Mục A2 — dựng ba kỳ mới qua giao diện

Chạy trọn quy trình *tạo kỳ → sinh CDR → tính cước → lập hóa đơn → chốt kỳ* qua giao diện bằng
`scripts/chay-ky-moi-phase6.ps1`. **16 phép kiểm, 16 đạt.**

| Kỳ | Hạt giống | CDR yêu cầu | CDR thực sinh | Hóa đơn | Doanh thu | Trạng thái |
|---|---|---|---|---|---|---|
| 3/2026 | `20260300` | 3.000 | **2.770** | 55 | 21.497.051 đ | `DA_CHOT` |
| 4/2026 | `20260400` | 3.500 | **3.239** | 55 | 21.737.109 đ | `DA_CHOT` |
| 7/2026 | `20260700` | 4.000 | **4.000** | 58 | 23.161.085 đ | `MO` |

> **Hạt giống là thứ duy nhất dựng lại được đúng ba bộ CDR này.** Ghi ở đây, trong
> `scripts/chay-ky-moi-phase6.ps1` và trong `nhat_ky_he_thong`.

**Kỳ 7 giữ `MO`, không chốt** — cùng lý do kỳ 6 được giữ sạch giao dịch: phải còn ít nhất một
kỳ demo được trọn vòng *huỷ hóa đơn → lập lại*.

### 3.1. Vì sao sinh thiếu bản ghi — và con số thiếu tự giải thích được

Yêu cầu 3.000 nhưng ra 2.770 (thiếu 7,7%); yêu cầu 3.500 ra 3.239 (thiếu 7,5%). Không phải lỗi:
bộ sinh bốc ngẫu nhiên một thuê bao rồi **bỏ qua lượt** nếu thuê bao đó kích hoạt sau khoảng
ngày yêu cầu.

Dữ liệu mẫu có **5 trong 65** thuê bao hoạt động kích hoạt giữa tháng 6/2026 (id 34, 35, 78,
79, 80 — chính năm thuê bao dựng lên để thử prorate ở Phase 4). Với kỳ tháng 3 và tháng 4,
năm thuê bao đó không thể có bản ghi nào:

```
5 / 65 = 7,69%   ứng với   230/3000 = 7,67%   và   261/3500 = 7,46%
```

Kỳ 7/2026 sinh đủ 4.000 vì tới tháng 7 thì cả 65 thuê bao đều đã kích hoạt — đúng như suy
luận trên dự đoán.

### 3.2. Tại sao kỳ 3 và 4 có 55 hóa đơn, không phải 54 hay 58

Số hóa đơn khác nhau giữa các kỳ vì tập thuê bao trả sau **đủ điều kiện lập hóa đơn** đổi theo
thời gian: kỳ 6 và 7 có 58, kỳ 5 có 54, kỳ 3 và 4 có 55. Đây là dữ liệu đo được, không phải
con số đặt trước; phần đối chiếu chi tiết nằm ở mục kiểm chéo.

## 4. ⭐ Mục A3 — DỰ ĐOÁN CÔNG BỐ TRƯỚC KHI SEED THANH TOÁN

> ⚠️ **Mục này được viết và commit TRƯỚC khi chạy lớp seed thanh toán.** Đó là điều kiện để nó
> có giá trị: theo chuẩn làm việc 43.4, nếu số thực tế lệch thì đó là tín hiệu **dừng lại phân
> tích**, không phải tín hiệu sửa cho khớp.

### 4.1. Cách tính

Mọi hóa đơn trong cùng một kỳ có **cùng** `han_thanh_toan` — đã kiểm bằng SQL:
`COUNT(DISTINCT han_thanh_toan) = 1` cho cả năm kỳ. Vì vậy **mỗi kỳ rơi trọn vẹn vào đúng một
nhóm tuổi nợ**, và bài toán rút về: mỗi kỳ còn bao nhiêu hóa đơn chưa trả hết.

Tỷ lệ seed dùng phép chia **số nguyên** `(tổng × phần_trăm + 50) / 100` chứ không nhân số thực
rồi làm tròn: `55 × 0,70` trong `double` ra `38,49999999999999`, làm tròn thành **38** thay vì
39. Một dự đoán sai vì số dấu chấm động là dự đoán sai vì lý do không liên quan gì tới nghiệp
vụ, và nó làm hỏng chính cơ chế mà mục này dựng lên.

| Kỳ | Số hóa đơn | `DA_TT` | `TT_MOT_PHAN` | Không trả | **Còn nợ** |
|---|---|---|---|---|---|
| 3/2026 | 55 | (55×80+50)/100 = **44** | (55×5+50)/100 = **3** | 8 | **11** |
| 4/2026 | 55 | (55×70+50)/100 = **39** | (55×10+50)/100 = **6** | 10 | **16** |
| 5/2026 | 54 | 32 *(đã có từ mục F)* | 8 | 14 | **22** |
| 6/2026 | 58 | 0 *(cố ý không thu)* | 0 | 58 | **58** |
| 7/2026 | 58 | 0 *(cố ý không thu)* | 0 | 58 | **58** |

### 4.2. ⭐ Dự đoán bảng tuổi nợ tại mốc 10/08/2026

| Nhóm tuổi nợ | Kỳ tương ứng | Số ngày quá hạn | **Số hóa đơn dự đoán** |
|---|---|---|---|
| Trong hạn | 7/2026 | −5 | **58** |
| Quá hạn 1–30 ngày | 6/2026 | 26 | **58** |
| Quá hạn 31–60 ngày | 5/2026 | 56 | **22** |
| Quá hạn 61–90 ngày | 4/2026 | 87 | **16** |
| Quá hạn trên 90 ngày | 3/2026 | 117 | **11** |
| **Tổng hóa đơn còn nợ** | | | **165** |

**Cả năm nhóm đều khác 0** — đó là điều tiêu chí nghiệm thu số 2 đòi hỏi.

Nếu bất kỳ con số nào lệch → **DỪNG, phân tích trước khi sửa.**
