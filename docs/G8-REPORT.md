# G8 — LÀM MỚI DỮ LIỆU DEMO TRƯỚC KHI CHỤP ẢNH

> Kỳ cước mới nhất là 7/2026, kết thúc 31/07. Hôm nay 09/09/2026. Demo một phần mềm tính cước
> mà kỳ gần nhất cũ hơn một tháng trông như hệ thống bị bỏ quên. Đợt này thêm kỳ 8/2026 chạy
> trọn vòng, và tạo kỳ 9/2026 rỗng thay chỗ kỳ demo.
>
> Sau đợt này **không sửa gì nữa cho tới khi chụp xong 70 ảnh**.

Ngày làm: 09/09/2026 · Bắt đầu từ `d44001f`

---

## Bước 0 — Kiểm kê skill

Danh sách skill khả dụng trong phiên này có **đúng một** mục: `workflow-authoring` — hướng dẫn
viết script cho công cụ Workflow. Không liên quan tới CSDL, CSS hay tài liệu.

**Không skill nào áp dụng được. Làm tay.**

---

## Hai tiền đề trong đặc tả — một sai

### Tiền đề 1: *"hôm nay là cuối tháng 8"* — **SAI**

Ngày hệ thống là **09/09/2026**, đầu tháng 9. Kết luận của đặc tả vẫn đúng nhưng vì một lý do
khác, và nó kéo theo một hệ quả cần biết trước khi chụp ảnh.

Hạn thanh toán do `BillingService` đặt là `ngày cuối kỳ + 1 tháng`, rồi ép về ngày **15**
(`ThamSoTinhCuoc.NGAY_HAN_THANH_TOAN_THANG_SAU`). Kỳ 8 kết thúc 31/08 → hạn **15/09/2026**.

| | |
|---|---|
| Hôm nay | 09/09/2026 |
| Hạn kỳ 8 | 15/09/2026 |
| Số ngày quá hạn | **−6** → nhóm **Trong hạn** |

> ⚠️ **Nhóm "Trong hạn" chỉ có nội dung tới hết 15/09/2026 — sáu ngày.** Sau mốc đó kỳ 8 rơi
> sang *Quá hạn 1–30* và bảng tuổi nợ lại còn 4 nhóm. **Ảnh bảng tuổi nợ phải chụp trước
> 15/09/2026.**
>
> Đây là tính chất của **ngày xem**, không phải của dữ liệu — cùng loại với ghi chú đã có trong
> `CLAUDE.md` (*"Bảng aging đủ 5 nhóm chỉ đúng tới 13/08/2026"*). Muốn cửa sổ rộng hơn thì phải
> lập hóa đơn kỳ 9, nhưng kỳ 9 chưa kết thúc (30/09) nên lập hóa đơn cho nó là sai nghiệp vụ.

### Tiền đề 2: *"bảng tuổi nợ đang thiếu nhóm Trong hạn"* — **ĐÚNG**, đã đo

Đo tại 09/09/2026 trên 165 hóa đơn còn nợ:

| Nhóm | Số hóa đơn | Còn nợ |
|---|--:|--:|
| Trong hạn | **0** | — |
| Quá hạn 1–30 | 58 | 23.161.085 |
| Quá hạn 31–60 | 58 | 23.828.605 |
| Quá hạn 61–90 | 22 | 6.171.688 |
| Quá hạn trên 90 | 27 | 9.160.947 |

---

## Hiện trạng trước khi làm

| | |
|---|--:|
| Kỳ cước | 6 (3·4·5 `DA_CHOT` · 6·7 `MO` · 8 `MO` rỗng) |
| CDR | 18.723 |
| Hóa đơn | 280 |
| Dòng chi tiết hóa đơn | 620 |
| Thanh toán | 161 |
| Biến động số dư | 35 |
| Thuê bao · khách hàng | 80 · 50 |
| Doanh thu · đã thu · còn nợ | 111.513.012 · 49.190.687 · 62.322.325 |

**Đã sao lưu CSDL trước khi bắt đầu** (`mysqldump --routines --triggers --events`, 2,4 MB).

---

## A. Che hai chuỗi lộ tên máy cá nhân — ✅ `14ea277`

| Chỗ | Trước | Sau |
|---|---|---|
| `docs/G7-REPORT.md:75-76` | `C:\Users\<tên thật>\…` | `C:\Users\<tên-tài-khoản>\…` |
| `docs/PHASE-0-REPORT.md:202` | UUID Spring đầy đủ | `1e47***` |

**Mở rộng ngoài hai dòng đặc tả nêu.** Đặc tả chỉ nói `G7-REPORT.md:75` và `:76`. Nhưng chính
`docs/RA-SOAT-TRUOC-KHI-NOP.md` **trích lại nguyên chuỗi đó** ở 3 chỗ làm bằng chứng. Che ở G7
mà để nguyên trong tài liệu đã phát hiện ra nó thì không đạt được mục đích nào — đã che cả 5,
và ghi rõ trong bảng V2 rằng bản gốc từng ghi tên thật.

**Giữ nguyên đúng theo quyết định đã chốt:** V3 (`D:\KGU`, 11 lần — đó là *nội dung* của báo
cáo Phase 0, kể lại sự cố đường dẫn tiếng Việt có dấu), V5 (định danh GitHub, 3 lần), và gói
Java `com.hanzo.billing` (1051 lần, định danh kỹ thuật).

**Đối chứng cho phép kiểm "còn lại 0":**

| Mẫu | Kết quả | Mẫu rộng hơn để chứng minh nó vẫn khớp được |
|---|--:|--:|
| `Users\HANZO` | 0 | `Users\` bất kỳ → **7** |
| `1e4748c3` | 0 | `1e47` (dạng đã che) → **3** |

---

## C.1 — DỰ ĐOÁN, công bố TRƯỚC khi sinh bản ghi CDR nào

> Theo chuẩn lập từ mục 4C: *"Công bố dự đoán TRƯỚC khi viết code — số lệch là tín hiệu dừng
> lại phân tích, không phải tín hiệu sửa cho khớp."* Phần này được commit **trước** commit chạy
> thật; kiểm bằng thứ tự commit.

### Câu truy vấn dự đoán đã được hiệu chuẩn trước khi dùng

Luật lấy từ `QuyTacKyCuoc.khoangSuDung` và `BillingService.tinhCuocThueBao`, viết lại bằng SQL.
Không tin nó ngay — **chạy trên 5 kỳ đã biết kết quả thật**:

| Kỳ | Dự đoán số hóa đơn | Thực tế đã có | Khớp | Dự đoán số prorate |
|---|--:|--:|:--:|--:|
| 3/2026 | 55 | 55 | ✅ | 0 |
| 4/2026 | 55 | 55 | ✅ | 0 |
| 5/2026 | 54 | 54 | ✅ | **1** |
| 6/2026 | 58 | 58 | ✅ | **5** |
| 7/2026 | 58 | 58 | ✅ | 0 |

Tái tạo đúng **5/5** kỳ, và nhánh prorate **có** trả về dòng ở kỳ 5 và kỳ 6 — nên nó biết kêu.
Số 0 cho kỳ 8 là kết quả thật, không phải câu truy vấn hỏng.

### Dự đoán cho kỳ 8/2026

| # | Đại lượng | Dự đoán |
|---|---|--:|
| 1 | **Số hóa đơn kỳ 8** | **58** |
| 2 | **Số thuê bao bị prorate** | **0** |
| 3 | **Tổng cước thuê bao kỳ 8** | **18.750.000 đ** |

**Vì sao 58:** 80 thuê bao − 20 trả trước = 60 trả sau; trừ 2 thuê bao đã thanh lý có ngày huỷ
**trước** 01/08 (id 33 huỷ 20/05/2026, id 74 huỷ 30/04/2026) → khoảng sử dụng rỗng → không lập
hóa đơn. Còn **58**.

**Vì sao 0 prorate:** ngày kích hoạt muộn nhất trong toàn bộ 80 thuê bao là **23/06/2026**, và
cả 3 ngày huỷ đều nằm trong tháng 3–5/2026. **Không thuê bao nào kích hoạt hoặc huỷ trong
tháng 8/2026** (đo: `kich_hoat_trong_t8 = 0`, `huy_trong_t8 = 0`), nên cả 58 đều dùng trọn 31
ngày.

### Bảng tuổi nợ dự đoán sau khi có kỳ 8 (xem ngày 09/09/2026)

| Nhóm | Kỳ nguồn | Hạn thanh toán | Ngày quá hạn | Dự đoán số HĐ |
|---|---|---|--:|--:|
| **Trong hạn** | **8/2026** | 15/09/2026 | −6 | **58** |
| Quá hạn 1–30 | 7/2026 | 15/08/2026 | 25 | 58 |
| Quá hạn 31–60 | 6/2026 | 15/07/2026 | 56 | 58 |
| Quá hạn 61–90 | 5/2026 | 15/06/2026 | 86 | 22 |
| Quá hạn trên 90 | 3+4/2026 | 15/04 · 15/05 | 147 · 117 | 27 |
| | | | **Tổng** | **223** |

**Đủ 5 nhóm có nội dung** — đó là mục đích chính của việc thêm kỳ 8.

### Trả trước — dự đoán hai giai đoạn

Số thuê bao trả trước **hết số dư** phụ thuộc vào cước sinh ra từ CDR kỳ 8, mà CDR chưa tồn
tại. Không thể dự đoán chính xác ở giai đoạn này — nói thẳng thay vì đoán một con số.

Đo được **bây giờ** (20 thuê bao trả trước):

| Số dư hiện tại | Số thuê bao |
|---|--:|
| = 0 | **1** |
| < 50.000 | 5 |
| 50.000 – 150.000 | 6 |
| ≥ 150.000 | 8 |

Dự đoán chính xác sẽ được công bố ở **giai đoạn 2** — sau khi sinh CDR, **trước** khi chạy trừ
cước, và cũng commit trước khi chạy.

### Các đại lượng khác

| | Trước | Dự đoán sau |
|---|--:|--:|
| Kỳ cước | 6 | **8** (thêm kỳ 8 có dữ liệu + kỳ 9 rỗng) |
| Hóa đơn | 280 | **338** |
| CDR | 18.723 | **≈ 23.223** (thêm ~4.500) |
| Hạt giống CDR kỳ 8 | — | **`20260800`** |

---

## C.1b — DỰ ĐOÁN GIAI ĐOẠN 2: trừ cước trả trước, công bố TRƯỚC khi chạy

Sinh CDR xong (4.500 bản ghi, hạt giống `20260800`), chạy tính cước (0 lỗi, tổng cước
8.455.062 đ) và lập hóa đơn xong. Giờ mới dự đoán được phần trả trước.

**Luật trừ cước, đọc từ `TruCuocTraTruocService`:** duyệt CDR theo `thoi_gian_bat_dau, id`;
khi gặp bản ghi có cước **lớn hơn số dư còn lại** thì đặt cờ hết số dư và **bỏ qua toàn bộ bản
ghi còn lại** (không cắt đôi bản ghi — quyết định 5.3). Nếu trừ được 0 đồng thì **không sinh
dòng sổ cái nào**.

Mô phỏng bằng luỹ kế cửa sổ, 15 thuê bao trả trước có phát sinh trong kỳ 8:

| Dự đoán | Số thuê bao |
|---|--:|
| **Hết số dư giữa kỳ** | **6** — id 1 · 4 · 9 · 11 · 17 · 19 |
| Trừ đủ toàn bộ | 9 — id 2 · 3 · 6 · 7 · 10 · 13 · 14 · 16 · 18 |
| Không phát sinh CDR (tạm ngưng 2 chiều / đã thanh lý) | 5 |

| | Dự đoán |
|---|--:|
| Tổng tiền trừ được | **1.238.927 đ** |
| Số dòng `bien_dong_so_du` mới | **15** |
| `bien_dong_so_du` sau | 35 → **50** |

Trường hợp đáng chú ý: thuê bao **id 4** (`0944567804`) số dư còn **220 đ**, cước kỳ 8 là
72.554 đ — dự đoán chỉ trừ được **99 đ** (đúng một bản ghi SMS) rồi dừng.

---

## B. Nâng cỡ huy hiệu — ✅ `e79cfc9`

**Đo trước khi sửa, không lấy số trong đặc tả.** Đặc tả ghi 10,8px; đo thật ra **10,5px**. Và
phát hiện thêm một điều đặc tả không nêu: huy hiệu **không đồng cỡ**.

| Màn hình | Cỡ chữ ô cha | Cỡ huy hiệu |
|---|--:|--:|
| `/hoa-don` (trong bảng) | 14px | **10,5px** |
| `/cong-no` | 16px | 12px |

Nguyên nhân: Bootstrap đặt `.badge { font-size: .75em }` — **tương đối** với ô chứa, nên cùng
một huy hiệu ra hai cỡ tuỳ chỗ đặt. Chỗ nhỏ nhất 10,5px lại đúng là chỗ huy hiệu mang thông
tin trạng thái hóa đơn.

**Sửa:** đặt cỡ **tuyệt đối**, neo vào `var(--chu-1)` = 13px — sàn tuyệt đối của thang cỡ chữ
lập ở G4. Dùng token, không gõ số.

**Không thêm luật mới.** `.badge` đã có sẵn ở `app.css:963` (mục 10) nhưng thiếu `font-size`;
thêm một khối `.badge` thứ hai là tạo ra hai chỗ định nghĩa cho cùng một thứ. Luật `.badge`
trong `@media print` (dòng 1234) không đụng tới.

**Đo lại sau khi sửa — 3 bề rộng × 4 màn hình nhiều huy hiệu nhất:**

| | 1024 | 1366 | 1920 |
|---|--:|--:|--:|
| `/hoa-don` · `/cong-no` · `/thue-bao` · `/tinh-cuoc` | **0** | **0** | **0** |
| Cỡ huy hiệu | 13px | 13px | 13px |

**Đối chứng âm cho phép đo tràn ngang:** chèn một phần tử rộng `innerWidth + 700` → phép đo báo
**715**; gỡ ra → về **0**. Phép đo biết kêu, nên các số 0 ở trên có nghĩa.

> **Một bẫy đo dính giữa chừng.** Lượt đầu tôi đo bằng `iframe` cho nhanh — `contentDocument`
> trả `null` vì Spring Security đặt `X-Frame-Options`. Đúng loại bẫy iframe đã gặp ở đợt trước
> (209 kết quả sai). Chuyển sang đo trên **trang thật**, điều hướng từng trang một.

---

## C.2 · C.3 — Chạy kỳ 8 và tạo kỳ 9 · ✅ `b606262`

Chạy **qua giao diện**, đúng thứ tự quy trình:

| Bước | Kết quả |
|---|---|
| Sinh CDR | 4.500 bản ghi · 01/08–31/08/2026 · **hạt giống `20260800`** · 65 thuê bao phát sinh |
| Tính cước | 4.500 bản ghi, **0 lỗi**, tổng cước 8.455.062 đ |
| Lập hóa đơn | **58 hóa đơn**, bỏ qua đúng 2 thuê bao, doanh thu 23.525.972 đ |
| Trừ cước trả trước | 15 thuê bao, trừ **1.238.927 đ** qua 801 bản ghi; **6 thuê bao hết số dư**, không thu được 612.226 đ |
| Chốt kỳ | `DA_CHOT` lúc 09/09/2026 09:11 |

**Cố ý không sinh thanh toán cho kỳ 8** — 58 hóa đơn giữ `CHUA_TT` để lấp nhóm *Trong hạn*.

**Kỳ 9/2026**: tạo rỗng, `MO`, 0 CDR, 0 hóa đơn. Thay kỳ 8 làm kỳ demo trực tiếp.

### Dự đoán vs thực tế — khớp toàn bộ

| Đại lượng | Dự đoán | Thực tế | |
|---|--:|--:|:--:|
| Số hóa đơn kỳ 8 | 58 | 58 | ✅ |
| Số thuê bao prorate | 0 | 0 | ✅ |
| Tổng cước thuê bao | 18.750.000 đ | 18.750.000 đ | ✅ |
| Hạn thanh toán | 15/09/2026 | 15/09/2026 | ✅ |
| Trả trước hết số dư | 6 | 6 | ✅ |
| Tiền trừ được | 1.238.927 đ | 1.238.927 đ | ✅ |
| Dòng `bien_dong_so_du` mới | 15 | 15 | ✅ |

Không con số nào lệch, nên không phải dừng lại phân tích.

### Một hệ quả đặc tả không lường

`TruCuocTraTruocService` **chặn trừ cước khi một kỳ muộn hơn đã trừ** (trừ cước không giao hoán
theo kỳ). Kỳ 7 **chưa** trừ cước; từ khi kỳ 8 đã trừ thì kỳ 7 **không trừ được nữa** cho tới
khi huỷ trừ cước kỳ 8. Nút *Trừ cước trả trước* của kỳ 7 vẫn hiện và sẽ báo một lỗi nghiệp vụ
giải thích rõ lý do — không phải sự cố.

Đã kiểm `huyBillingKy`: nó chỉ chặn khi kỳ **đã chốt** hoặc **có thanh toán**, **không** chặn
theo kỳ muộn hơn. Nên tiêu chí *"kỳ 6 và kỳ 7 vẫn huỷ hóa đơn được"* vẫn giữ. Việc này đảo
ngược được bằng nút *Huỷ trừ cước* của kỳ 8.

---

## C.4 — Dump lại `data-van-hanh.sql`

Bộ lọc mục 6 giữ `IN ('TRU_CUOC','NAP_TIEN')` và công thức mục 7 giữ phân dấu theo loại — đúng
hai chỗ phải sửa cùng nhau mà G5 cảnh báo.

### Sửa một bẫy còn sót từ G5

Dòng **19** của phần hướng dẫn tái sinh **vẫn ghi bộ lọc cũ**:

```
rieng bien_dong_so_du them --where="loai_bien_dong='TRU_CUOC'"
```

G5 đã sửa **dữ liệu** ở mục 6 và **công thức** ở mục 7 cho đúng cả hai loại, nhưng **quên sửa
chính câu hướng dẫn**. Ai làm theo đúng chữ sẽ đánh rơi dòng `NAP_TIEN`, và mục 7 ra số dư sai
mà **không báo gì cả**. Đã sửa, và thêm một dòng cảnh báo in hoa để không ai quay lại bộ lọc cũ.

`data-mau.sql`: thêm kỳ 9 (**id 46** — ID thật do CSDL cấp, không đánh số lại) và viết lại chú
thích kỳ 8 cho đúng hiện trạng.

### Xác minh — mô phỏng trước, `reset` sau

**Bước 1 — không chạy `reset`.** Nạp cả hai file SQL vào một CSDL **nháp riêng**
(`vnpt_billing_mophong`) rồi so từng cột với CSDL đang chạy:

| So sánh | Lệch |
|---|--:|
| Số dư từng thuê bao · trạng thái thuê bao | **0** |
| Mọi cột tiền của 338 hóa đơn | **0** |
| `cuoc_phi` · `mien_phi` · `ky_cuoc_id` của 23.223 CDR | **0** |
| Tổng hợp 7 kỳ cước | **0** |
| 50 dòng sổ cái | **0** |

Chênh lệch duy nhất: `nguoi_dung` 4 vs 3 — tài khoản `kiemthu01` do `test-auth.ps1` tạo, có sẵn
từ trước, không liên quan bản dump.

**Đối chứng âm:** cố tình làm sai 3 giá trị trong CSDL nháp → cả ba phép so đều báo đúng **1**
lệch.

**Bước 2 — `reset` thật, hai lần.** Chỉ chạy sau khi mô phỏng sạch.

| | |
|---|---|
| `reset` lần 1 | 20 giây, log có `Started BillingApplication` |
| `reset` lần 2 | 18 giây, log có `Started BillingApplication` |
| So `reset→reset` từng dòng | **giống hệt** — 24.866 dòng mỗi bản |
| Đối chứng âm cho phép so | sửa một byte → báo 49.732 dòng khác |

Sau `reset`, mọi số y hệt trước `reset`: 7 kỳ · 23.223 CDR · 338 hóa đơn · 5 nhóm tuổi nợ · ba
bất biến 0 lệch.

### Hai lỗi của chính bộ sinh, bắt được **trước** khi chạy `reset`

| # | Lỗi | Nếu không bắt |
|---|---|---|
| 1 | `FORMAT(tong_doanh_thu,2,'en_US')` chèn dấu phân cách nghìn → sinh ra `tong_doanh_thu = 21,737,109.00` | **SQL sai cú pháp**. `reset` sẽ chết giữa chừng sau khi Flyway đã xoá sạch CSDL |
| 2 | `mysqldump --no-data` giữ lại `AUTO_INCREMENT=N` của CSDL đang chạy → bảng nháp đánh số từ 11, khoá ngoại thất bại | **Báo động giả**: tưởng bản dump hỏng, trong khi đó là lỗi của phép mô phỏng — `reset` thật dùng Flyway dựng bảng mới từ 1 |

---

## D — Số liệu đã cập nhật, từng `file:dòng`

Áp nguyên tắc A0: **chỉ sửa số trình bày hiện trạng**; số đo trong tài liệu lịch sử giữ nguyên,
và nơi nào là bảng *bàn giao* thì cập nhật **kèm chú thích hồi cứu** ghi rõ số cũ.

| File:dòng | Cũ → mới |
|---|---|
| `CLAUDE.md:164-169` | 6 kỳ → **7 kỳ** · 18.723 → **23.223** CDR · 280 → **338** hóa đơn · 620 → **753** chi tiết · 35 → **50** sổ cái · doanh thu 111.513.012 → **135.038.984** · còn nợ 62.322.325 → **85.848.297** · tỷ lệ thu 44,1% → **36,4%** |
| `CLAUDE.md:170-175` | *(thêm)* cảnh báo bảng tuổi nợ đủ 5 nhóm chỉ tới 15/09/2026 |
| `CLAUDE.md:178` | hạt giống: thêm **kỳ 8 `20260800`** |
| `CLAUDE.md:192-198` | ràng buộc "kỳ 8 phải giữ RỖNG" → **"kỳ 9 phải giữ RỖNG"**, *(thêm)* ràng buộc thứ tự trừ cước theo kỳ |
| `README.md:195` | `data-mau.sql` 6 → **7 kỳ cước** |
| `README.md:196` | `data-van-hanh.sql` 18.723 → **23.223** CDR · 280 → **338** hóa đơn · 620 → **753** chi tiết · 34 → **32** dòng sổ cái |
| `README.md:264` | `test-ky-rong.ps1` kỳ 8 → **kỳ 9** |
| `docs/mo-ta-csdl.md:468` | 18.723 → **23.223**, thêm `+ 4.500` cho kỳ 8; "kỳ 8 không có bản ghi" → **kỳ 9** |
| `docs/kich-ban-kiem-thu.md:28-29` | 6 → **7 kỳ** · 18.723 → **23.223** · 280 → **338** · kỳ rỗng 8 → **9** |
| `docs/kich-ban-kiem-thu.md:186` | `test-ky-rong.ps1` kỳ 8 → **kỳ 9** |
| `docs/kich-ban-demo.md:19` | bước 5 kiểm kỳ **8** rỗng → kỳ **9** rỗng |
| `docs/kich-ban-demo.md:33` | doanh thu · còn nợ theo số mới |
| `docs/danh-sach-anh-chup.md:44-90` | quyết định N3 *"chấp nhận 4 nhóm"* → **đủ 5 nhóm, hạn chụp trước 15/09**; bảng tuổi nợ đo lại 09/09; số đo cũ giữ trong khối trích dẫn |
| `docs/danh-sach-anh-chup.md:163-164` | ảnh #30 tổng 62.322.325/165 → **85.848.297/223**; ảnh #31 *"4 nhóm"* → **"đủ 5 nhóm"** |
| `docs/PHASE-6-REPORT.md:409-415` | bảng bàn giao: 5 → **7 kỳ** · 18.723 → **23.223** · 280 → **338** · 620 → **753** · 34 → **50** |
| `docs/PHASE-6-REPORT.md:421-430` | tiền theo số mới **+ chú thích hồi cứu** ghi nguyên số Phase 6 |
| `docs/PHASE-7-REPORT.md:222-231` | bảng bàn giao + tiền theo số mới **+ chú thích hồi cứu** |
| `docs/PHASE-7-REPORT.md:245` | "kỳ 8/2026 rỗng" → **kỳ 9**, kèm ghi chú đổi vai |
| `scripts/test-ky-rong.ps1` (23 chỗ) | trỏ từ **kỳ 8 → kỳ 9**; 18.723 → 23.223 trong chú thích |
| `src/test/.../CdrImportServiceTest.java:46` | javadoc 18.723 → **23.223** |

### Giữ nguyên — tài liệu lịch sử

`G4` · `G5` · `G6` · `G7-REPORT` · `KE-HOACH-HOAN-THIEN` · `PHASE-5` · `PHASE-8-REPORT` ·
`DANH-GIA-HE-THONG` · `toi-uu-hieu-nang` · `RA-SOAT-GIAO-DIEN` · `RA-SOAT-TINH-NANG`. Chúng ghi
số **đo được tại thời điểm đó**; sửa chúng là làm sai lệch hồ sơ quá trình.

### Một sửa **ngoài** danh sách đặc tả nêu — và vì sao bắt buộc

`scripts/test-ky-rong.ps1` gắn cứng `thang=8`. Nguy hiểm hơn một chú thích cũ: khối dọn dẹp
đầu script **ép kỳ về `MO` bằng SQL trực tiếp** rồi **huỷ sạch hóa đơn** của nó nếu thấy có.

Chạy script đó sau đợt này sẽ **xoá 58 hóa đơn kỳ 8 vừa tạo và mở lại kỳ đã chốt** — mà vẫn báo
xanh 28/28, vì nó chỉ khẳng định "kỳ rỗng thì màn hình mở được". Đúng loại **phép kiểm tự phá
dữ liệu rồi báo đạt**. Đã trỏ sang kỳ 9.

---

## E — Nghiệm thu

| | Kết quả |
|---|---|
| `mvnw test` | **342 / 342**, 0 lỗi — đếm `<testcase>` trong XML: **342**, `<failure>`/`<error>`: **0** |
| 8 script giao diện | **215 đạt / 0 sai** — `test-auth` 42 · `test-bao-cao` 39 · `test-bien` 42 · `test-dieu-huong` 15 · `test-kh` 14 · `test-ky-rong` 28 · `test-muc-F` 17 · `test-tb` 18 |
| 3 phép kiểm Python | ✅ đạt — 46 file chữ · 38 màn hình · 180 nút/liên kết |
| Dữ liệu sau khi chạy test | 7 kỳ · 23.223 CDR · 338 hóa đơn · 753 chi tiết · 161 thanh toán · 50 sổ cái — **không suy suyển** |
| Dữ liệu sau khi chạy 8 script | **kỳ 8 vẫn `DA_CHOT`, 4.500 CDR, 58 hóa đơn** · kỳ 9 vẫn rỗng `MO` — bằng chứng bản sửa `test-ky-rong.ps1` đúng và cần thiết |
| Bất biến `con_no = tong_thanh_toan − da_thanh_toan` | **0 lệch** / 338 hóa đơn |
| Bất biến `da_thanh_toan = SUM(thanh_toan)` | **0 lệch** / 338 hóa đơn |
| Bất biến sổ cái `so_du = SUM(nạp+đc) − SUM(trừ)` | **0 lệch** / 80 thuê bao |
| Dự đoán C.1 vs thực tế | **7/7 khớp** |
| Bảng tuổi nợ | **đủ 5 nhóm** — 58 · 58 · 58 · 22 · 27 |
| Kỳ 9/2026 | rỗng, `MO`, 0 CDR, 0 hóa đơn |
| Kỳ 6 và kỳ 7 | vẫn **0 thanh toán**, vẫn huỷ hóa đơn được |
| Cuộn ngang sau khi đổi cỡ huy hiệu | **0** ở cả 1024 · 1366 · 1920 |
| `reset → reset` | **giống hệt từng dòng**, 24.866 dòng |

### Bảng số liệu 7 kỳ cước

| Kỳ | Khoảng ngày | Trạng thái | CDR | Hóa đơn | Doanh thu | Thanh toán |
|---|---|---|--:|--:|--:|--:|
| 3/2026 | 01/03 → 31/03 | `DA_CHOT` | 2.770 | 55 | 21.497.051 đ | 58 |
| 4/2026 | 01/04 → 30/04 | `DA_CHOT` | 3.239 | 55 | 21.737.109 đ | 55 |
| 5/2026 | 01/05 → 31/05 | `DA_CHOT` | 3.697 | 54 | 21.289.162 đ | 48 |
| 6/2026 | 01/06 → 30/06 | `MO` | 5.017 | 58 | 23.828.605 đ | **0** |
| 7/2026 | 01/07 → 31/07 | `MO` | 4.000 | 58 | 23.161.085 đ | **0** |
| **8/2026** | 01/08 → 31/08 | **`DA_CHOT`** | **4.500** | **58** | **23.525.972 đ** | **0** |
| **9/2026** | 01/09 → 30/09 | **`MO`** | **0** | **0** | **0 đ** | **0** |
| | | | **23.223** | **338** | **135.038.984 đ** | **161** |

### Bảng tuổi nợ 5 nhóm (xem ngày 09/09/2026)

| Nhóm | Kỳ nguồn | Hạn thanh toán | Ngày quá hạn | Hóa đơn | Còn nợ |
|---|---|---|--:|--:|--:|
| **Trong hạn** | 8/2026 | 15/09/2026 | **−6** | **58** | 23.525.972 đ |
| Quá hạn 1–30 | 7/2026 | 15/08/2026 | 25 | 58 | 23.161.085 đ |
| Quá hạn 31–60 | 6/2026 | 15/07/2026 | 56 | 58 | 23.828.605 đ |
| Quá hạn 61–90 | 5/2026 | 15/06/2026 | 86 | 22 | 6.171.688 đ |
| Quá hạn trên 90 | 3 + 4/2026 | 15/04 · 15/05 | 147 · 117 | 27 | 9.160.947 đ |
| | | | | **223** | **85.848.297 đ** |

---

## Cố ý không làm

| | Vì sao |
|---|---|
| **Không lập hóa đơn kỳ 9** | Kỳ 9 chưa kết thúc (30/09). Lập hóa đơn cho một kỳ đang chạy là sai nghiệp vụ — và sẽ mất luôn kỳ demo rỗng |
| **Không sinh thanh toán cho kỳ 8** | Đó là điều làm nhóm *Trong hạn* có nội dung. Thu tiền là mất mục đích của cả việc này |
| **Không trừ cước cho kỳ 7** | Kỳ 7 cố ý dừng ở bước *"chờ trừ cước trả trước"* để demo được bước đó. Sau khi kỳ 8 đã trừ thì kỳ 7 không trừ được nữa — đảo ngược bằng *Huỷ trừ cước* kỳ 8 nếu cần |
| **Không dời hạn thanh toán để kéo dài cửa sổ 5 nhóm** | Sửa `han_thanh_toan` là sửa chứng từ. Cửa sổ 6 ngày là **tính chất của ngày xem**, và giải thích được nó là điểm cộng — đúng lập luận N3 cũ, vẫn giữ |
| **Không sửa số trong tài liệu lịch sử** | 11 tài liệu ghi số cũ được giữ nguyên; sửa chúng là làm sai lệch hồ sơ quá trình |
| **Không chạm `service/` · `repository/` · `entity/` · `dto/`** | Không cần: mọi thay đổi nghiệp vụ đều chạy **qua giao diện**, đúng như `data-van-hanh.sql` đòi hỏi |
| **Không đánh số lại ID kỳ 9** | ID thật là **46** (auto-increment đã nhảy vì các kỳ thử nghiệm tạo rồi xoá). Đánh số lại là tự tạo ra nguồn sự thật thứ hai |

---

## Sẵn sàng chụp ảnh

| | |
|---|---|
| Kỳ 9/2026 rỗng và `MO` | ✅ 0 CDR · 0 hóa đơn |
| Kỳ 6, 7 giữ 0 thanh toán | ✅ vẫn huỷ hóa đơn được |
| Ba bất biến | ✅ 0 lệch |
| Huy hiệu | ✅ 13px, không cuộn ngang ở 3 bề rộng |
| Bảng tuổi nợ | ✅ đủ 5 nhóm |

> ⚠️ **Hạn chụp ảnh bảng tuổi nợ: trước hết ngày 15/09/2026.** Sau mốc đó kỳ 8 rơi sang *Quá
> hạn 1–30* và bảng còn 4 nhóm. Không phải lỗi — nhưng ảnh sẽ không khớp `danh-sach-anh-chup.md`
> nữa.
