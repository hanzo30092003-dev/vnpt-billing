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

## C.2–C.4 · D · E — *(chưa chạy)*
