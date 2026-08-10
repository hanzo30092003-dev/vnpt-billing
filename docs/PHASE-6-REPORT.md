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

## 5. ⭐ Đối chiếu dự đoán — khớp tuyệt đối 5/5

| Nhóm tuổi nợ | Dự đoán | Thực tế | |
|---|---|---|---|
| Trong hạn | 58 | **58** | ✅ |
| Quá hạn 1–30 ngày | 58 | **58** | ✅ |
| Quá hạn 31–60 ngày | 22 | **22** | ✅ |
| Quá hạn 61–90 ngày | 16 | **16** | ✅ |
| Quá hạn trên 90 ngày | 11 | **11** | ✅ |
| **Tổng hóa đơn còn nợ** | **165** | **165** | ✅ |

Phân bố trạng thái từng kỳ cũng khớp từng con số:

| Kỳ | Dự đoán `DA_TT`/`TT_MOT_PHAN`/`QUA_HAN` | Thực tế | |
|---|---|---|---|
| 3/2026 | 44 / 3 / 8 | **44 / 3 / 8** | ✅ |
| 4/2026 | 39 / 6 / 10 | **39 / 6 / 10** | ✅ |

**Bằng chứng luật siết ở mục F Phase 5 vẫn còn hiệu lực.** Lớp seed gọi `capNhatQuaHan()` hai
lần: **trước** khi thu đổi 110 hóa đơn, **sau** khi thu đổi **0** hóa đơn. Số 0 đó khẳng định 9
hóa đơn trả một phần của hai kỳ mới không bị kéo về `QUA_HAN`.

## 6. Mục A5 — dump lại và kiểm chứng reset

`data-van-hanh.sql`: **2.393.851 byte**, 352 dòng, 151 `INSERT` + 6 `UPDATE`. Đầu file nay có
dòng cảnh báo **"FILE SINH TỰ ĐỘNG — KHÔNG SỬA TAY"**, tham số `mysqldump` để tái sinh, và
**hạt giống của ba kỳ mới** để dựng lại được nếu mất dữ liệu.

Hai kỳ 3/2026 và 4/2026 thêm vào `data-mau.sql` mục 7 (**định nghĩa kỳ** là dữ liệu gốc); trạng
thái chốt và các cột tổng hợp nằm ở `data-van-hanh.sql` vì chúng là **kết quả chạy engine**. Các
câu `UPDATE ky_cuoc` đó sinh từ chính CSDL, không gõ tay.

| Phép kiểm sau `reset` | Kết quả |
|---|---|
| Ảnh chụp CSDL trước / sau `reset` | **20.101 / 20.101 dòng, 0 khác biệt** |
| Bất biến tiêu chí 4 | **0 lệch / 280 hóa đơn** |
| Bất biến sổ cái số dư | **0 lệch / 80 thuê bao** |
| Bảng tuổi nợ | cả **5 nhóm** đều có nội dung |
| Kỳ 6 và kỳ 7 | **0 giao dịch** thanh toán |

---

# PHẦN II — MỤC B: DASHBOARD

## 7. Nội dung

Thay trang giới thiệu tạm của Phase 0. Bốn thẻ số liệu, một biểu đồ cột doanh thu **5 kỳ**, hai
biểu đồ tròn (theo gói cước, theo trạng thái), bảng top 5 thuê bao cước cao và bảng 5 giao dịch
gần nhất.

**Mọi con số đọc từ cột đã ghi** — `hoa_don.con_no`, `hoa_don.tong_thanh_toan` — chứ không cộng
lại từ bảng `thanh_toan`. Ràng buộc ② của Phase 5 áp cho tầng báo cáo: hai cách tính song song
là hai nguồn sự thật, và chúng sẽ mâu thuẫn nhau đúng vào lúc khó truy nhất.

Hai chi tiết đáng ghi:

- **"Kỳ gần nhất" là kỳ mới nhất CÓ hóa đơn**, không phải kỳ mới nhất. Một kỳ vừa tạo mà chưa
  chạy gì sẽ làm thẻ doanh thu hiện `0 đ` và trông như hệ thống mất số liệu.
- **Mảng cho biểu đồ dựng sẵn ở controller**, không để template tự chiếu. Bảng và biểu đồ khi đó
  chắc chắn đọc cùng một nguồn, cùng thứ tự — cách `CongNoController` đã làm từ Phase 5.

## 8. Sidebar: bốn màn hình Phase 5 chưa bao giờ được nối vào menu

Phát hiện khi thêm mục *Báo cáo* vào sidebar: các mục **Hóa đơn**, **Thanh toán**, **Công nợ**
vẫn để `href="#"` từ Phase 0, và **Giảm trừ** thì không có trong menu.

Bốn màn hình ấy dựng ở Phase 5, có test, có ảnh chụp — nhưng người dùng **không có đường nào tới
chúng ngoài việc gõ thẳng URL**. Lý do không ai phát hiện: mọi phép kiểm của Phase 5 đều gọi
`Get-Trang $s "/cong-no"` bằng đường dẫn tuyệt đối, tức là đi vòng qua đúng cái thứ bị hỏng.

> **Bài học:** kiểm một màn hình bằng cách gõ thẳng URL của nó chứng minh màn hình chạy được,
> **không** chứng minh người dùng tới được nó.

Đã nối cả bốn, cộng mục *Báo cáo thống kê*.

---

# PHẦN III — MỤC C VÀ D: BẢY BÁO CÁO

## 9. Bảy báo cáo

| # | Đường dẫn | Nội dung |
|---|---|---|
| C.1 | `/bao-cao/doanh-thu-ky` | Bảng + biểu đồ cột kết hợp đường tỷ lệ thu |
| C.2 | `/bao-cao/doanh-thu-goi-cuoc` | Bảng + biểu đồ tròn + tỷ trọng, chọn kỳ |
| C.3 | `/bao-cao/doanh-thu-dich-vu` | Cơ cấu cước thuê bao / thoại / SMS / dữ liệu |
| C.4 | `/bao-cao/thue-bao` | Theo trạng thái, theo loại, mới và rời mạng theo tháng |
| C.5 | `/bao-cao/top-thue-bao` | Chọn kỳ và số lượng 10 / 20 / 50 |
| C.6 | `/bao-cao/san-luong` | Phút gọi ba hướng, SMS, dữ liệu, so với kỳ liền trước |
| C.7 | `/bao-cao/cong-no` | Bảng tuổi nợ + top 10 khách hàng nợ nhiều nhất |

## 10. Mục D — bốn quyết định kỹ thuật

### 10.1. `BaoCaoExcel` — một khuôn cho cả bảy báo cáo

Ba lớp của Phase 3–5 mỗi lớp tự viết lại đúng cùng một đoạn: tạo workbook, tạo kiểu chữ đậm, đổ
tiêu đề, tự động giãn cột. Bảy báo cáo của Phase 6 sẽ thành bảy bản sao nữa nếu không gom.

Khuôn chung đúng bằng yêu cầu D.1: tiêu đề, phạm vi, ngày xuất, header in đậm có nền,
**freeze pane**, số tiền có phân cách nghìn, dòng tổng in đậm, chân trang *"Dữ liệu mẫu phục vụ
mục đích học tập"*.

> **Freeze pane đặt ngay dưới dòng header**, không phải dòng 1 — phía trên header còn ba dòng
> tiêu đề báo cáo. Khoá nhầm chỗ thì cuộn xuống sẽ mất tên cột, tức mất đúng thứ mà freeze pane
> sinh ra để giữ.

### 10.2. `DinhDangTien` — một helper định dạng duy nhất

Trước Phase 6, mỗi template tự viết `#numbers.formatDecimal(x, 0, 'POINT', 0, 'COMMA')` — chuỗi
đó lặp lại hàng chục lần trên mười mấy file.

Bean tên **`soLieu`** chứ không phải `dinhDang`: tên đó đã thuộc về `DinhDangCdr` từ Phase 3, và
hai bean trùng tên làm ứng dụng **không khởi động được**.

**Một ranh giới cố ý không gộp.** `DonViCuoc.giaySangPhut` làm tròn **lên** vì đó là *luật tính
tiền* — dùng dở một block thì trả trọn block. Với một bảng *thống kê sản lượng*, làm tròn lên là
sai: 5.978 giây phải hiện **99,6 phút** chứ không phải 100. Hai phép quy đổi khác nhau về **mục
đích** nên khác nhau về chế độ làm tròn; cái chung là **hệ số**, và hệ số vẫn chỉ khai báo một
lần trong `DonViCuoc`. Dùng chung hằng số, không dùng chung luật.

### 10.3. Truy vấn gom nhóm trong CSDL

Mọi truy vấn thống kê đều `SELECT new ... + GROUP BY`, không load entity rồi cộng trong Java.
Với 280 hóa đơn hai cách đều nhanh; nhưng cách sau tăng tuyến tính theo số bản ghi và sẽ hỏng
**lặng lẽ** khi dữ liệu lớn lên.

### 10.4. Đo trước khi thêm index — và kết luận là **không thêm**

Chi tiết ở [`toi-uu-hieu-nang.md`](toi-uu-hieu-nang.md) Phần II. Tóm tắt:

- Mọi màn hình đáp ứng trong **40–137 ms**.
- Truy vấn nặng nhất (sản lượng, quét CDR một kỳ) đã có index: `EXPLAIN` cho `type=ref`,
  `key=fk_cdr_ky_cuoc`, `Using index`. Index đó **không ai tạo cho báo cáo** — MySQL tự sinh nó
  cho ràng buộc khóa ngoại từ Phase 1.
- Chỗ duy nhất còn quét toàn bảng là `WHERE con_no > 0` trên `hoa_don`. **Không thêm index**:
  280 dòng, điều kiện đúng với 59% số dòng nên tính chọn lọc quá kém, và màn hình đã chạy 50 ms.
  Đã ghi **ngưỡng để xem lại** vào tài liệu thay vì để lần sau phải đoán lại.

> Kết luận của Phần II là *không thêm gì cả* — nhưng đó là kết luận **có số liệu chống lưng**,
> khác hẳn với việc quên mất.

## 11. ⭐ Ba lỗi thật bắt được khi kiểm chứng

### 11.1. `SUM` trên kỳ rỗng trả `NULL` — cả trang báo cáo HTTP 500

`SanLuongDichVu` có sáu tham số kiểu `long` **nguyên thuỷ**. Với một kỳ chưa có bản ghi CDR nào,
`SUM(...)` trả về `NULL`, Hibernate không mở hộp được và toàn bộ trang trả về **HTTP 500**.

Điều đáng nói: lỗi này **không bao giờ lộ ra trên dữ liệu mẫu**, vì cả năm kỳ đều có CDR. Nó chỉ
xảy ra đúng lúc người dùng vừa tạo một kỳ mới rồi mở báo cáo lên xem — tức đúng lúc khó đoán
nhất và khó tái hiện nhất.

Bắt được nhờ tiêu chí nghiệm thu số 7 (*"mọi báo cáo chạy được khi không có dữ liệu"*), và cách
kiểm là **tạo một kỳ trống thật** rồi mở cả bốn báo cáo có tham số kỳ. Đã thêm `COALESCE` quanh
mọi `SUM`, và thêm phép kiểm 11 của `BaoCaoServiceTest` dựng lại đúng tình huống đó bằng một kỳ
tạm (`@Transactional` rollback ngay sau khi chạy).

### 11.2. Biểu thức Thymeleaf lồng nhau làm vỡ cả trang

Viết `${a} != null and ${a}.signum() < 0 ? 'x' : 'y'` trong `th:classappend`: Thymeleaf **không
cho lồng `${...}` bên trong một biểu thức khác**, và cả trang ném `TemplateInputException`.

Đáng ghi vì lỗi này **không hiện ra lúc biên dịch** — Java compile sạch, test đơn vị xanh, chỉ
lộ ra khi thật sự mở đúng trang đó lên. Đã chuyển phần tính màu vào `DinhDangTien.lopBienThien`,
để biểu thức trong template chỉ còn một lời gọi phẳng.

### 11.3. Sidebar `href="#"` — xem mục 8

## 12. Nghiệm thu mục B, C, D

`scripts/test-bao-cao.ps1` — **38 phép kiểm, 38 đạt**:

| Nhóm | Nội dung |
|---|---|
| **13 con số đối chiếu chéo** | Mỗi con số trên màn hình so với một câu SQL **độc lập** |
| **11 file Excel** | Đều là xlsx thật (chữ ký ZIP `PK`), 3,9–5,0 KB |
| **Bảng tuổi nợ** | Cả 5 nhóm đều có nội dung |
| **Kỳ rỗng** | Bốn báo cáo có tham số kỳ đều chạy, không lỗi 500 |

`BaoCaoServiceTest` — **11 test**, và điểm mạnh của nó là **kiểm chéo giữa các đường truy vấn**
thay vì so với hằng số chép tay:

```
SUM gom theo KỲ  ==  SUM gom theo GÓI CƯỚC  ==  SUM của bảng CƠ CẤU CƯỚC
```

Ba câu truy vấn gom nhóm theo ba cách hoàn toàn khác nhau. Nếu một câu sai điều kiện join hoặc
sót một nhóm, con số của nó lệch khỏi hai câu còn lại — mà đó đúng là loại lỗi một hằng số chép
tay **không bắt được**, vì hằng số chỉ chứng minh hôm nay dữ liệu vẫn như hôm qua.

---

# PHẦN IV — TỔNG KẾT PHASE 6

## 13. Nghiệm thu

| # | Tiêu chí | Kết quả |
|---|---|---|
| 1 | `mvnw test` PASS, không giảm số test | ✅ **260 test** (246 → 260), 0 lỗi |
| 2 | Bốn kỳ có dữ liệu; aging đủ **5 nhóm** | ✅ **5 kỳ**; cả 5 nhóm có nội dung — xem cảnh báo 1.2 |
| 3 | Dự đoán A.4 khớp thực tế | ✅ **5/5**, và 6/6 cả phân bố trạng thái từng kỳ |
| 4 | Bất biến thanh toán, trước và sau `reset` | ✅ **0 lệch / 280 hóa đơn** cả hai lần |
| 5 | `data-van-hanh.sql` dump lại; reset tái lập giống hệt | ✅ **20.101/20.101 dòng, 0 khác biệt** |
| 6 | Kiểm chéo ≥ 5 con số bằng SQL | ✅ **13 con số**, tất cả khớp |
| 7 | Báo cáo chạy được khi không có dữ liệu | ✅ 4 báo cáo × (màn hình + Excel) trên một kỳ rỗng |
| 8 | Kỳ 6 vẫn 0 thanh toán, vẫn huỷ được hóa đơn | ✅ huỷ + lập lại ra đúng 58 hóa đơn / 23.828.605 đ |

**Tổng kiểm thử giao diện: 5 script, 94 phép kiểm, 94 đạt.**

## 14. Số liệu bàn giao cho Phase 7

| Bảng | Số bản ghi | Ghi chú |
|---|---|---|
| `khach_hang` · `thue_bao` · `goi_cuoc` | 50 · 80 · 5 | Không đổi |
| `ky_cuoc` | **5** | 3, 4, 5/2026 `DA_CHOT` · 6, 7/2026 `MO` |
| `chi_tiet_su_dung` | **18.723** | Tất cả `DA_TINH` |
| `hoa_don` | **280** | 55 · 55 · 54 · 58 · 58 |
| `chi_tiet_hoa_don` | **620** | Gồm 2 dòng "Giảm trừ" thành tiền âm |
| `thanh_toan` | **161** | Kỳ 3: 58 · kỳ 4: 55 · kỳ 5: 48 · kỳ 6–7: **0** |
| `bien_dong_so_du` | **34** | 18 mở sổ + 16 `TRU_CUOC` kỳ 6 |
| `giam_tru` | **2** | Cả hai `DA_AP_DUNG` |

**Tiền:**

| Chỉ tiêu | Giá trị |
|---|---|
| Doanh thu 5 kỳ | **111.513.012 đ** |
| Đã thu | **49.190.687 đ** |
| Còn nợ | **62.322.325 đ** |
| Tỷ lệ thu chung | **44,1%** |

## 15. Danh sách màn hình chụp ảnh cho Phase 6

### Nhóm 1 — Ảnh bắt buộc ⭐

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 1 | **Dashboard** | `/` | 4 thẻ số liệu; biểu đồ **5 cột**; 2 biểu đồ tròn; 2 bảng có nội dung |
| 2 | **Doanh thu theo kỳ** | `/bao-cao/doanh-thu-ky` | Biểu đồ cột **5 kỳ** kèm đường tỷ lệ thu trên trục phải |
| 3 | **Bảng tuổi nợ đủ 5 nhóm** ⚠️ | `/bao-cao/cong-no` | Cả 5 nhóm khác 0 — **chụp trước 14/08/2026**, xem mục 1.2 |
| 4 | Sản lượng dịch vụ | `/bao-cao/san-luong` | Cột "kỳ trước" và biến động % có dấu |
| 5 | Thống kê thuê bao | `/bao-cao/thue-bao` | Biểu đồ đường thuê bao mới / rời mạng trên cùng một trục |

### Nhóm 2 — Báo cáo còn lại và xuất file

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 6 | Menu báo cáo | `/bao-cao` | 7 thẻ + bảng số liệu nhanh 5 kỳ |
| 7 | Doanh thu theo gói cước | `/bao-cao/doanh-thu-goi-cuoc` | Tỷ trọng cộng đủ 100% |
| 8 | Doanh thu theo loại dịch vụ | `/bao-cao/doanh-thu-dich-vu` | Dòng giảm trừ mang **dấu âm** |
| 9 | Top thuê bao cước cao | `/bao-cao/top-thue-bao?soLuong=50` | Đổi 10/20/50 giữ nguyên kỳ đang chọn |
| 10 | **File Excel mở trong Excel** | Bấm *Xuất Excel* ở ảnh 2 | Header có nền, **freeze pane**, dòng tổng đậm, chân trang |
| 11 | **Bản in A4** | Bấm *In* ở ảnh 3 → xem trước | Không còn sidebar/nút; có tiêu đề riêng cho bản in |
| 12 | Sinh CDR có hạt giống | `/cdr/sinh-du-lieu` | Ô **Hạt giống** và khối kết quả hiện hạt giống đã dùng |

### Nhóm 3 — Minh chứng kỹ thuật

| # | Ảnh | Cách lấy |
|---|---|---|
| 13 | **260 test PASS** | Console `mvnw test`, dòng `Tests run: 260, Failures: 0` |
| 14 | **`test-bao-cao.ps1` — 38 đạt / 0 sai** | `.\scripts\test-bao-cao.ps1` |
| 15 | Kiểm chéo ba đường truy vấn | Chạy `BaoCaoServiceTest`, 11 test xanh |
| 16 | Lịch sử Git Phase 6 | `git log --oneline -6` |

## 16. ⭐ Bài học phương pháp Phase 6

### 16.1. Bài học 43.5 — lần thứ năm và thứ sáu

Phase 5 đã gặp bài học *"một phép kiểm sai nguy hiểm ngang thiếu phép kiểm"* bốn lần. Phase 6
gặp thêm hai, và cả hai đều là **phép kiểm buộc vào chuỗi hiển thị**:

| Lần | Ở đâu | Sai thế nào |
|---|---|---|
| 5 | `test-auth.ps1` | Kiểm phân quyền sidebar bằng cách dò **chữ** `'Hóa đơn'` trên cả trang. Nó đúng *tình cờ*, vì trang chủ cũ là trang giới thiệu không chứa chữ đó. Trang chủ thành dashboard → chữ ấy xuất hiện hợp lệ trong thân trang → **báo động giả** |
| 6 | `test-muc-F.ps1` | Ghim cứng `soHd -eq 112`. Phase 6 thêm ba kỳ thành 280 → phép kiểm **đỏ oan** dù bất biến hoàn toàn sạch |

Đã sửa cả hai theo cùng một nguyên tắc: **buộc phép kiểm vào thứ nó thật sự nói về**. Kiểm menu
thì dò `href="/hoa-don"` — chính xác tới mức không nhầm với `href="/hoa-don/307"` của bảng dữ
liệu. Kiểm bất biến thì lấy số hóa đơn **từ CSDL**, vì bất biến nói về *quan hệ giữa các cột*,
không nói gì về *số lượng dòng*.

> Một phép kiểm ghim vào con số dữ liệu sẽ đỏ oan mỗi lần bộ dữ liệu lớn lên. Và đỏ oan vài lần
> thì lần sau không ai tin nó nữa — đó mới là thiệt hại thật.

### 16.2. Kiểm bằng URL trực tiếp không chứng minh người dùng tới được

Bốn màn hình của Phase 5 có test, có ảnh chụp, có tài liệu — và **không có đường nào tới chúng
từ menu** suốt cả một phase. Mọi phép kiểm đều gọi thẳng `/cong-no`, tức đi vòng qua đúng cái
thứ bị hỏng.

Muốn bắt loại lỗi này thì phép kiểm phải **xuất phát từ trang chủ và đi theo liên kết**, hoặc
tối thiểu là khẳng định liên kết có tồn tại — như `test-auth.ps1` sau khi sửa đang làm.

### 16.3. Lỗi chỉ xuất hiện trên dữ liệu mà bộ dữ liệu mẫu không có

Lỗi `SUM` trả `NULL` (mục 11.1) không thể lộ ra trên dữ liệu mẫu, vì mọi kỳ đều có CDR. Nó cần
một **kỳ rỗng** — thứ chỉ tồn tại trong khoảnh khắc giữa lúc người dùng tạo kỳ và lúc chạy tính
cước.

Tiêu chí nghiệm thu số 7 (*"báo cáo chạy được khi không có dữ liệu"*) chính là thứ ép phải dựng
ra trạng thái đó. Đây là biến thể của bài học 43.7: **dữ liệu thử thiết kế theo một chiều chỉ
đúng theo chiều đó** — bộ dữ liệu mẫu được dựng để *có* số liệu, nên nó không bao giờ tự kiểm
được nhánh *không có* số liệu.

### 16.4. Tiền đề của đặc tả vẫn là thứ phải đo đầu tiên

Lần thứ hai liên tiếp (sau mục F), hai câu trong đặc tả mô tả một kho mã không tồn tại: *"thêm
hai kỳ là đủ 5 nhóm"* (thiếu một kỳ) và *"chạy trừ cước cho kỳ mới"* (bị chốt chặn từ chối).

Chi phí phát hiện: một bảng tính số ngày quá hạn và một lần đọc `kiemTraTruocKhiChay`. Chi phí
nếu bỏ qua: dựng xong toàn bộ dữ liệu rồi mới thấy nhóm *Trong hạn* rỗng, và một chốt chặn nghiệp
vụ nổ ra giữa chừng.

## 17. Nợ tài liệu của Phase 6

| # | Nợ | Ghi chú |
|---|---|---|
| 1 | **Bảng aging đủ 5 nhóm chỉ đúng tới 13/08/2026** | Tính chất của ngày xem, không phải của dữ liệu — mục 1.2. Muốn bền thì phải nới dải tuổi nợ hoặc thêm kỳ theo thời gian |
| 2 | **Bốn kỳ có CDR trả trước chưa trừ vào số dư** | Kỳ 3, 4, 5, 7. Hệ quả của quyết định ở mục 1.3; không vi phạm bất biến sổ cái |
| 3 | Dashboard hiện liên kết `/hoa-don/{id}` cho mọi vai trò | `nhanvien01` bấm vào sẽ nhận 403. Phân quyền phía máy chủ vẫn đúng, chỉ là trải nghiệm chưa gọn |
| 4 | Báo cáo chưa có bộ lọc theo khách hàng | Chỉ `/cong-no` của Phase 5 có; bảy báo cáo mới lọc theo kỳ |
| 5 | Vẫn còn nợ **phần viết cho mục A–D của Phase 5** | Từ `PHASE-5-REPORT.md` mục 35, chưa xử lý |
