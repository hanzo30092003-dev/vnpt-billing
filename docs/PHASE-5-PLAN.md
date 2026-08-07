# KẾ HOẠCH PHASE 5 — HÓA ĐƠN, THANH TOÁN, CÔNG NỢ

> ✅ **Phase 5 đã xong.** Tài liệu này giữ nguyên nội dung kế hoạch tại thời điểm lập — mục A0,
> ba ràng buộc bắt buộc và mục G. Đặc tả mục A–F được giao sau và không chép vào đây.
>
> Kết quả và các điểm **đặc tả lệch so với kho mã** ghi ở
> [`PHASE-5-REPORT.md`](PHASE-5-REPORT.md) mục 33. Riêng mục F có hai tiền đề của đặc tả không
> đúng, đã đo lại trước khi viết code — xem mục 23 của báo cáo.

---

## Mục A0 — Sửa số liệu sai ✅ ĐÃ LÀM

`PHASE-4-REPORT.md` mục 45.2 ghi *"1.148 bản ghi CDR của 16 thuê bao trả trước ở kỳ 6"*. Con số
1.148 chép từ `PHASE-4-PLAN.md` mục 5.4 — đo **trước** khi mục 4A sinh lại toàn bộ CDR, nên đã
lỗi thời từ lúc đó.

Đo lại trên CSDL đang chạy:

| Kỳ | Thuê bao trả trước có phát sinh | Số CDR | Tổng cước đã tính |
|---|---|---|---|
| 5/2026 | 15 | **921** | 1.643.768 đ |
| 6/2026 | 16 | **1.126** | 2.001.863 đ |

Kết quả rà soát toàn bộ tài liệu: xem mục "Rà soát số liệu chép lại" ở cuối tài liệu này.

---

## BA RÀNG BUỘC BẮT BUỘC CHO CẢ PHASE 5

### ① Làm tròn ở đúng MỘT tầng

`giam_tru` có cột `ty_le_phan_tram DECIMAL(5,2)` bên cạnh `so_tien`. Áp giảm trừ theo phần trăm
là **nhân rồi làm tròn** — tầng làm tròn thứ hai, vi phạm trực tiếp quyết định 5.8 của Phase 4
(*"làm tròn ở đúng một tầng: tầng CDR"*).

**Cách xử lý:** quy tỉ lệ thành **số tiền tuyệt đối đúng một lần** tại thời điểm lập hóa đơn,
ghi vào `hoa_don.giam_tru`, và **snapshot số tiền đó vào `chi_tiet_hoa_don`**. Từ đó về sau chỉ
cộng trừ, không nhân lại.

Sửa `giam_tru` bắt buộc tính lại **TOÀN BỘ** chuỗi:

```
tong_truoc_thue → thue_vat → tong_thanh_toan → con_no
```

**Cấm sửa mỗi `con_no`.** Mọi phép cộng dồn thanh toán dùng `BigDecimal`, `HALF_UP` scale 0.

### ② Một nguồn sự thật cho "còn nợ"

`hoa_don.con_no` là chỗ ghi **DUY NHẤT**, và chỉ được ghi trong service. Mọi màn hình — chi tiết
hóa đơn, danh sách, công nợ, aging — chỉ **ĐỌC** cột đó. **TUYỆT ĐỐI không** tự tính
`tong_thanh_toan − SUM(thanh_toan)` trên view.

Đây là bài học 43.6 của Phase 4 áp cho công nợ: hai cách tính song song là hai nguồn sự thật.

**Test bất biến, chạy sau MỖI mục** — với mọi hóa đơn:

```sql
SELECT h.id, h.ma_hoa_don, h.tong_thanh_toan, h.da_thanh_toan, h.con_no,
       COALESCE(SUM(t.so_tien), 0) AS tong_thu_thuc
FROM hoa_don h LEFT JOIN thanh_toan t ON t.hoa_don_id = h.id
GROUP BY h.id
HAVING h.con_no <> h.tong_thanh_toan - h.da_thanh_toan
    OR h.da_thanh_toan <> tong_thu_thuc;
-- Phai tra ve 0 dong
```

### ③ Thứ tự cố định khi trừ số dư

Trừ cước vào số dư trả trước lặp lại **nguyên** tình huống quyết định 5.3: quỹ cạn dần, và bản
ghi làm cạn quỹ **KHÔNG được cắt đôi**. Bắt buộc `ORDER BY thoi_gian_bat_dau, id`. Chạy hai lần
phải ra cùng kết quả.

---

## MỤC G — SỔ CÁI BIẾN ĐỘNG SỐ DƯ & TRỪ CƯỚC TRẢ TRƯỚC

> Làm **SAU** mục A–F. Nếu hết thời gian thì dừng ở mục F và ghi G vào nợ tài liệu.

### G1. Tổng quát hoá `nap_tien` thành sổ cái

Vấn đề: `nap_tien` có `so_du_truoc`/`so_du_sau` nên nạp tiền truy nguyên được, nhưng **trừ cước
không để lại vết nào** — trừ xong chỉ còn `thue_bao.so_du` đã đổi.

Xử lý bằng cách **tổng quát hoá bảng hiện có, KHÔNG thêm bảng thứ hai** (hai bảng cùng ghi số dư
= hai nguồn sự thật, vi phạm ràng buộc ②).

1. `RENAME TABLE nap_tien TO bien_dong_so_du` — áp bằng `ALTER` lên CSDL đang chạy, đồng thời
   sửa `schema.sql` và `data-mau.sql`.
2. Thêm `loai_bien_dong ENUM('NAP_TIEN','TRU_CUOC','DIEU_CHINH') NOT NULL`, mặc định `NAP_TIEN`
   cho dữ liệu cũ.
3. Thêm `ky_cuoc_id BIGINT NULL` FK → `ky_cuoc` (chỉ có giá trị khi `TRU_CUOC`).
4. `so_tien` giữ **dương**; ý nghĩa cộng/trừ do `loai_bien_dong` quyết định.
   `so_du_truoc`/`so_du_sau` vẫn bắt buộc — đó là thứ làm nó thành sổ cái.
5. Cập nhật entity, repository, service, template nạp tiền của Phase 2; `mo-ta-csdl.md`; README.
6. **Bất biến kiểm ngay:** với mọi thuê bao, `so_du` hiện tại phải bằng `SUM(nạp) − SUM(trừ)`
   theo sổ cái → 0 lệch.

### G2. Trừ cước theo kỳ cho thuê bao trả trước

> ⚠️ **Phạm vi: trừ theo kỳ (batch), KHÔNG real-time.** Tính cước thời gian thực vẫn nằm ở
> "Hướng phát triển".

```
Với mỗi thuê bao TRA_TRUOC trong kỳ:
   duyệt CDR ĐÃ DA_TINH của thuê bao, ORDER BY thoi_gian_bat_dau, id
   quỹ = thue_bao.so_du
   Với mỗi CDR:
      NẾU cdr.cuocPhi <= quỹ:  quỹ -= cuocPhi
      NGƯỢC LẠI: DỪNG — bản ghi này và mọi bản ghi sau KHÔNG được trừ
                 (không cắt đôi bản ghi, đúng quyết định 5.3)
   Ghi 1 bản ghi bien_dong_so_du loại TRU_CUOC:
      so_tien = tổng đã trừ, so_du_truoc, so_du_sau, ky_cuoc_id
   Cập nhật thue_bao.so_du = quỹ còn lại
```

- Thuê bao trả trước **KHÔNG lập hóa đơn** — giữ nguyên quyết định 5.4.
- Có chức năng **hoàn tác** đối xứng với `huyBillingKy`: xoá bản ghi `TRU_CUOC` của kỳ và trả
  `so_du` về giá trị trước khi trừ. Chạy lại phải ra cùng kết quả.

### G3. ⭐ Công bố dự đoán trước khi chạy

Đo và ghi vào báo cáo **TRƯỚC** khi viết code trừ số dư (chuẩn làm việc 43.4):

- Bao nhiêu trong 20 thuê bao trả trước có `so_du` đủ trả hết cước kỳ 6
- Bao nhiêu sẽ hết số dư giữa chừng (dự kiến ≥ 3 thuê bao 18.000 / 20.000 / 22.000 đ)
- Tổng số tiền sẽ trừ được, tổng cước **không** thu được do hết số dư

Nếu số thực tế khác dự đoán → **DỪNG, phân tích trước khi sửa.**

### G4. Màn hình

- Chi tiết thuê bao trả trước: tab **"Biến động số dư"** thay cho *"Lịch sử nạp tiền"*, hiện cả
  nạp và trừ, có `so_du_truoc`/`so_du_sau` từng dòng
- Cảnh báo trực quan cho thuê bao `so_du` dưới ngưỡng (mặc định 50.000 đ)
- Nút chạy / hoàn tác trừ cước trên `/tinh-cuoc`, cạnh nút lập hóa đơn

### Tiêu chí nghiệm thu mục G

| # | Tiêu chí |
|---|---|
| 1 | Bất biến sổ cái: `so_du = SUM(nạp) − SUM(trừ)` cho mọi thuê bao → 0 lệch |
| 2 | Dự đoán G3 khớp thực tế |
| 3 | Chạy trừ cước hai lần → không trừ chồng |
| 4 | Hoàn tác → chạy lại → `so_du` giống hệt lần đầu |
| 5 | Thuê bao trả trước vẫn KHÔNG có hóa đơn nào |

---

## Rà soát số liệu chép lại (kết quả mục A0)

Nguyên tắc áp dụng: **chỉ sửa số liệu được trình bày là trạng thái HIỆN TẠI.** Số đo trong tài
liệu lịch sử (PLAN, các mục báo cáo cũ) giữ nguyên và **gắn chú thích hồi cứu** — sửa chúng là
làm sai lệch hồ sơ quá trình. Đây đúng là cách `PHASE-4-PLAN.md` mục 4.2 đã làm sẵn.

Bốn mốc từng làm số liệu cũ mất hiệu lực: **4A** sinh lại toàn bộ CDR và thêm dòng
`SMS/QUOC_TE`; **4D** thêm cột `bang_gia_cuoc_id`; **4F** chạy kỳ 5 và điều chỉnh số dư mẫu.

| # | Vị trí | Vấn đề | Xử lý |
|---|---|---|---|
| 1 | `PHASE-4-REPORT.md` 45.2 | 1.148 CDR trả trước — số trước 4A | ✅ Sửa thành 1.126, bổ sung kỳ 5 (921 / 15 thuê bao) |
| 2 | `PHASE-4-PLAN.md` 5.4 | Nguồn gốc của 1.148, không có chú thích | ✅ Thêm chú thích hồi cứu, giữ nguyên số gốc |
| 3 | `PHASE-4-REPORT.md` 39 | Tham chiếu *"bàn giao (mục 44)"* — mục 44 là danh sách ảnh | ✅ Sửa thành mục 45.2 |
| 4 | `mo-ta-csdl.md` 4 | Tham chiếu *"trạng thái dữ liệu … mục 42"* — mục 42 là sáu sai lệch | ✅ Sửa thành mục 45.1 |
| 5 | `README.md` mục 6 | Ghi *"10 dòng bảng giá"* ở mốc Phase 1, thực tế Phase 1 có 9 | ✅ Ghi 9, chú thích +1 ở 4A |

**Đã đo lại và KHỚP, không phải sửa:** toàn bộ mục 41 (số liệu hai kỳ), 45.1 (bàn giao), 35
(trạng thái kỳ 6 và 17 số liệu màn hình cần chụp — kể cả `5.877.492 KB` của thuê bao 21 và
`5.978 giây / 99,6 phút` nội mạng của thuê bao 34), 5.1–5.2 (phân bố và độ phủ CDR kỳ 6), 38
(kết quả kỳ 5), 40 (ba nhóm số dư 15/3/2 — đã đối chiếu cả `data-mau.sql`, không chỉ CSDL đang
chạy), và các số liệu tổng ở README.
