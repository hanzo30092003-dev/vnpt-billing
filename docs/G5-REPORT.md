# G5 — BA VIỆC S SAU ĐỢT RÀ TÍNH NĂNG

> Ba mục đầu trong khuyến nghị của `docs/RA-SOAT-TINH-NANG.md`. Không thêm tính năng nào; cả ba
> chỉ dọn những chỗ hội đồng có thể chạm phải khi bảo vệ.

Ngày làm: 27/08/2026 · Trên bản `a47f312`

---

## Việc 1 — Vệt bánh mì bấm ra 404

### Vấn đề đo được

| Trên màn hình | Bấm mắt xích | Đi tới | Kết quả |
|---|---|---|---|
| `/thanh-toan/moi/461` *(form thu tiền)* | "Thêm mới" | `/thanh-toan/moi` | **404** |
| `/tinh-cuoc/ky/3` | "Hóa đơn của kỳ" | `/tinh-cuoc/ky` | **404** |

Gốc: `LayoutAdvice.breadcrumb()` suy mắt xích từ **từng đoạn** của đường dẫn. Khi đường dẫn kết
thúc bằng id, nó còn thêm một mắt xích *"Chi tiết"*, đẩy mắt xích trước đó vào **giữa** — và
template dựng mọi mắt xích giữa thành liên kết. Nhưng `/thanh-toan/moi` không có controller nào
phục vụ.

Nặng vì màn hình ghi nhận thanh toán **nằm trong đường demo**.

### Cách sửa — hỏi thẳng Spring, không giữ danh sách khai tay

`LayoutAdvice` giờ tiêm `RequestMappingHandlerMapping` và dựng sổ đường dẫn GET tĩnh. Mắt xích
chỉ thành liên kết khi đường dẫn của nó **thật sự có controller phục vụ**; không thì `duongDan`
để `null` và template dựng thành chữ thường.

Hai chi tiết đáng ghi:

- **Dùng `ObjectProvider` chứ không tiêm thẳng.** `RequestMappingHandlerMapping` được dựng
  **sau** lớp advice này; tiêm thẳng là vòng phụ thuộc lúc khởi động.
- **Không giữ danh sách khai tay.** Một danh sách khai tay sẽ lệch ngay lần đầu ai đó thêm màn
  hình mới, và lệch **im lặng** — đúng loại lỗi đợt rà vừa rồi phải đi tìm bằng cách bò từng
  liên kết.

Mắt xích cuối cũng chuyển sang `null` cho nhất quán: nó là chính trang đang mở.

### Kết quả

| | Trước | Sau |
|---|--:|--:|
| Mắt xích là liên kết | 12 | 10 |
| Mắt xích là chữ thường | — | 15 |
| **Mắt xích hỏng** | **2** | **0** |

Kiểm trên **15 màn hình**, 10 đường dẫn khác nhau, **theo cả chuyển hướng**.

> **Một khẳng định của chính tôi bị bác bỏ giữa chừng.** Lượt kiểm đầu sau khi sửa báo còn 1 mắt
> xích hỏng: `/quan-tri` trả **302**. Mở mã ra thì `HomeController:62` **đã có sẵn**
> `@GetMapping("/quan-tri")` chuyển hướng sang `/quan-tri/nguoi-dung`, kèm javadoc giải thích
> đúng vấn đề breadcrumb này từ trước. 302 là **đúng và cố ý** — phép kiểm của tôi quá nghiêm vì
> coi mọi mã khác 200 là hỏng. Sửa phép kiểm để theo chuyển hướng, kết quả thật là **0 hỏng**.

**Đối chứng âm:** 3 đường chắc chắn sống (`/`, `/khach-hang`, `/quan-tri`) → 200; 3 đường chắc
chắn chết (`/thanh-toan/moi`, `/tinh-cuoc/ky`, `/duong-bia`) → 404. Khớp **6/6**, nên số 0 ở
trên đáng tin.

---

## Việc 2 — Câu tài liệu sai về hạn mức tín dụng

`DANH-GIA-HE-THONG.md` ghi:

> *Cưỡng chế hạn mức tín dụng — cột `hanMucTinDung` **có lưu nhưng không chỗ nào dùng để chặn**
> — cột chết*

**Sai từ đợt V2.** `HoaDonRepository:272-289` dùng nó (`timThueBaoVuotHanMuc()`), và `/cong-no`
dựng bảng *Thuê bao vượt hạn mức* với **4 dòng** thật.

Câu mới nói đúng cả hai vế — cột đã sống, nhưng vẫn chưa phải *cưỡng chế*:

> *Đợt V2 đã làm cột `hanMucTinDung` sống lại… Nhưng đó là **cảnh báo để người dùng tự xử**,
> chưa phải cưỡng chế — hệ thống không tự chặn cuộc gọi.*

Các chỗ khác nhắc "cột chết" (`CLAUDE.md:287`, `mo-ta-csdl.md:163`, `KE-HOACH:262`) đều đúng vì
chúng kể **lịch sử** — *"từng là"*, *"đã hết là"* — nên giữ nguyên.

---

## Việc 3 — Nạp một giao dịch `NAP_TIEN` vào bộ dữ liệu mẫu

### Vấn đề

| Loại biến động số dư | Trước |
|---|--:|
| `DIEU_CHINH` | 18 |
| `TRU_CUOC` | 16 |
| **`NAP_TIEN`** | **0** |

Chức năng nạp tiền có mã, có form, có `test-tb.ps1` phủ — nhưng script **chỉ thử ca bị chặn**
(nạp cho thuê bao trả sau), chưa bao giờ đi đường thành công. Không có dòng lịch sử nào để chỉ
vào khi demo kịch bản *"nạp tiền → dùng → trừ cước → hết tiền"*.

### Làm qua giao diện, không ghi thẳng CSDL

Chọn thuê bao **`0818901208`** (id 8): trả trước, số dư **0**, đang `TAM_NGUNG_2C` — đúng câu
chuyện *"khách hết tiền nên bị tạm ngưng hai chiều"*.

Kiểm luật trước khi gửi: `ThueBaoServiceImpl:266-270` chỉ chặn thuê bao **trả sau** và thuê bao
**đã thanh lý**; `TAM_NGUNG_2C` được phép nạp. POST qua đúng form của màn hình chi tiết:
100.000 đ, hình thức **Thẻ cào**.

| | Sau |
|---|---|
| `NAP_TIEN` | **1** |
| Thuê bao 8 số dư | 0 → **100.000** |
| Dòng sổ cái | `truoc=0 · sau=100000 · hinh_thuc=THE_CAO` |
| `thanh_toan` · `hoa_don` | vẫn **161** · **280** — không suy suyển |

### Dump lại — và chỗ suýt sai

`data-van-hanh.sql` không phải file soạn tay. Đọc hướng dẫn tái sinh ngay trong file mới thấy
**hai chỗ phải sửa cùng nhau**, sửa một chỗ là `reset` vỡ bất biến:

- **Mục 6** dump `bien_dong_so_du` với `--where="loai_bien_dong='TRU_CUOC'"` — lọc này sẽ **bỏ
  rơi** dòng `NAP_TIEN`. Đổi thành `IN ('TRU_CUOC','NAP_TIEN')`.
- **Mục 7** là câu UPDATE suy ra số dư: `so_du = so_du − SUM(TRU_CUOC)`. Giữ nguyên thì dòng nạp
  bị **trừ thay vì cộng**. Đổi sang phân dấu theo loại:

```sql
SUM(CASE WHEN loai_bien_dong = 'TRU_CUOC' THEN -so_tien ELSE so_tien END)
```

Vẫn giữ đúng triết lý file đã ghi: **suy ra, không chép số tuyệt đối** — *"viết số tuyệt đối là
tạo ra cơ hội cho chúng lệch khỏi sổ cái nằm ngay bên trên"*.

Số dòng bị ảnh hưởng đổi từ 16 → **17** (16 thuê bao bị trừ cước + 1 thuê bao được nạp; thuê
bao 8 không nằm trong nhóm bị trừ).

### Xác minh mà KHÔNG chạy `reset`

Chạy `reset` để kiểm sẽ xoá sạch CSDL — nếu dump sai thì mất luôn kỳ 5 và kỳ 6, hai kỳ **không
dựng lại được bằng hạt giống**. Nên thay vì chạy, **mô phỏng trọn `reset` bằng cách đọc hai file
SQL**:

1. Lấy `so_du` gốc của 80 thuê bao từ `data-mau.sql`
2. Gộp sổ cái: 18 dòng `DIEU_CHINH` từ `data-mau.sql` + 17 dòng từ `data-van-hanh.sql`
3. Áp đúng công thức mục 7
4. So với `thue_bao.so_du` trong CSDL đang chạy

| | Kết quả |
|---|---|
| Vi phạm bất biến `so_du = SUM(nạp+đc) − SUM(trừ)` | **0** / 80 thuê bao |
| Số dư sau reset lệch so với CSDL đang chạy | **0** / 80 thuê bao |
| Thuê bao 8 | gốc `0` → sổ cái `NAP_TIEN 100000` → sau reset **100000** = đang chạy **100000** |

Một lần `reset` sau này sẽ dựng lại đúng trạng thái hiện tại, kèm giao dịch nạp tiền mới.

---

## Nghiệm thu

| | |
|---|---|
| `mvnw test` | **315 / 315**, gồm `KiemTraSoCaiSoDuTest` (bất biến sổ cái trên 80 thuê bao) |
| 8 script giao diện | **215 / 215** |
| 3 phép kiểm Python | đạt cả ba |
| Mắt xích vệt bánh mì hỏng | **2 → 0** (15 màn hình, 10 đường dẫn) |
| Ràng buộc dữ liệu | kỳ 6+7 vẫn **0** thanh toán · kỳ 8 vẫn **rỗng** · 280 hóa đơn · 161 thanh toán |

---

## Còn lại từ bản rà tính năng

| | Vì sao chưa làm |
|---|---|
| **Mục 4** — phép kiểm cho `/cdr/import` | **M** (1–2 giờ), không chặn việc bảo vệ. Vẫn là chức năng duy nhất trong phạm vi cam kết không có phép kiểm nào |
| **Mục 5** — đổi chữ lối tắt *"Ghi nhận thanh toán"* | **S**, nhưng chạm chữ hiển thị nên phải chạy lại `kiem-tu-ngu.py` và `kiem-giao-dien.py`; để cùng đợt với mục 4 |

Kết luận của bản rà tính năng **không đổi**: phần mềm đã đủ tính năng cốt lõi để bảo vệ đồ án.
Ba việc này chỉ dọn ba chỗ hội đồng có thể chạm phải.
