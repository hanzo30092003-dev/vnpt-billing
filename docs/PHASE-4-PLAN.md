# RÀ SOÁT ĐẦU VÀO VÀ KẾ HOẠCH PHASE 4 — ENGINE TÍNH CƯỚC

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Ngày lập:** 03/08/2026
**Trạng thái:** 🔎 Rà soát xong — chưa viết dòng code tính cước nào

> Toàn bộ dữ liệu trong hệ thống là dữ liệu mẫu tự sinh phục vụ học tập.
> Hệ thống không sử dụng dữ liệu thật của bất kỳ nhà mạng nào.

---

## 1. Mục đích tài liệu

Phase 3 kết thúc với 10/10 tiêu chí đạt và tuyên bố "sẵn sàng bước sang Phase 4".
Tài liệu này **kiểm chứng lại tuyên bố đó bằng truy vấn trên CSDL thật** trước khi
viết engine, rồi chốt kế hoạch và tiêu chí nghiệm thu cho Phase 4.

Lý do phải làm bước này: engine tính cước là loại chức năng **hỏng mà không báo lỗi**.
Hóa đơn vẫn phát hành, số tiền vẫn có, chỉ là sai. Mọi thứ sai sót phải bắt ở đầu vào.

Toàn bộ số liệu dưới đây đo trên CSDL `vnpt_billing` ngày 03/08/2026 (5017 CDR).

---

## 2. Kết quả rà soát — những gì Phase 3 nói đúng

| # | Khẳng định của Phase 3 | Kiểm chứng | Kết quả |
|---|---|---|---|
| 1 | `mvnw test` PASS, 30 test | Chạy lại `mvnw test` | ✅ `Tests run: 30, Failures: 0, Errors: 0` — BUILD SUCCESS |
| 2 | Toàn bộ CDR `cuoc_phi IS NULL` và `CHUA_TINH` | `GROUP BY trang_thai_tinh_cuoc` | ✅ 5017/5017 |
| 3 | Tồn tại 3 kỳ cước 5, 6, 7/2026 | `SELECT * FROM ky_cuoc` | ✅ cả 3, đều `MO`, ngày đầu/cuối tháng đúng |
| 4 | 5 thuê bao kích hoạt giữa 6/2026 để test prorate | Truy vấn `ngay_kich_hoat` | ✅ id 78, 34, 79, 35, 80 — ngày 05, 11, 15, 17, 23/06 |
| 5 | Bảng `hoa_don` / `chi_tiet_hoa_don` / `giam_tru` còn rỗng | `COUNT(*)` | ✅ 0 / 0 / 0 — Phase 4 khởi động từ nền sạch |
| 6 | Non-THOAI không mang cờ giờ cao điểm | `GROUP BY loai_dich_vu, gio_cao_diem` | ✅ 0 bản ghi SMS/DATA có cờ = 1 |

Bổ sung hai điều Phase 3 chưa nêu nhưng có lợi cho Phase 4:

- **Toàn bộ 5017 CDR nằm trong tháng 6/2026** → tất cả rơi trọn vào kỳ 6/2026,
  không có bản ghi mồ côi nằm ngoài mọi kỳ (`cdr_ngoai_moi_ky = 0`).
- **`dang_ky_goi_cuoc` khớp 100% với `thue_bao.goi_cuoc_id`** (0 lệch), nên engine
  có thể lấy gói từ `thue_bao` mà không sợ sai — nhưng xem mục 5.10 về việc nên lấy từ đâu.

Một đính chính nhỏ về cách đọc kết quả test: bảng phân rã theo lớp in ra dòng
`Tests run: 0 ... in Ma trận chuyển trạng thái thuê bao`. Đây **không phải lỗi** — đó là
cách Surefire đếm lớp `@Nested` (16 test nằm trong 4 lớp con, được đếm riêng). Dòng
tổng `Results: Tests run: 30` mới là con số đúng. **Khi chụp ảnh 22 của Phase 3 phải
chụp dòng `Results:`, không chụp dòng phân rã** — nếu không, người chấm nhìn thấy
"Tests run: 0" sẽ hiểu nhầm.

---

## 3. 🔴 CHẶN ĐƯỜNG SỐ 1 — bảng giá thiếu 3 tổ hợp, 251 CDR (5,00%) sẽ rơi vào `LOI`

Đây là vấn đề Phase 3 **không phát hiện ra**, và nó nghiêm trọng hơn cả hai điểm
sai lệch đã ghi trong PHASE-3-REPORT.

### 3.1. Hiện tượng

Đối chiếu từng tổ hợp `(loai_dich_vu, huong, gio_cao_diem)` có trong CDR với bảng giá:

| Loại dịch vụ | Hướng | Cao điểm | Số CDR | Dòng giá khớp |
|---|---|---|---|---|
| **DATA** | **NGOAI_MANG** | 0 | **186** | **0** ❌ |
| **SMS** | **QUOC_TE** | 0 | **49** | **0** ❌ |
| **DATA** | **QUOC_TE** | 0 | **16** | **0** ❌ |
| THOAI | NOI_MANG | 1 | 1009 | 1 ✅ |
| THOAI | NOI_MANG | 0 | 919 | 1 ✅ |
| THOAI | NGOAI_MANG | 0 | 731 | 1 ✅ |
| THOAI | NGOAI_MANG | 1 | 720 | 1 ✅ |
| SMS | NOI_MANG | 0 | 557 | 1 ✅ |
| SMS | NGOAI_MANG | 0 | 425 | 1 ✅ |
| DATA | NOI_MANG | 0 | 247 | 1 ✅ |
| THOAI | QUOC_TE | 1 | 86 | 1 ✅ |
| THOAI | QUOC_TE | 0 | 72 | 1 ✅ |

**Tổng: 251 bản ghi (5,00%) không tra được đơn giá.**

### 3.2. Nguyên nhân

`CdrGeneratorService.chonHuong()` chọn hướng **độc lập với loại dịch vụ** — cùng một
phân bố 55/40/5 áp cho cả THOAI, SMS lẫn DATA:

```java
LoaiDichVu dichVu = chonLoaiDichVu();
HuongCuocGoi huong = chonHuong();      // không hề nhìn vào dichVu
```

Nhưng `data-mau.sql` chỉ định nghĩa 9 dòng giá, trong đó DATA chỉ có `NOI_MANG` và
SMS chỉ có `NOI_MANG` / `NGOAI_MANG`.

### 3.3. Vì sao đây là bài học đáng giá nhất của lần rà soát này

Lớp `QuyTacGioCaoDiem` có sẵn một đoạn javadoc mô tả **chính xác** kiểu hỏng này:

> *"bảng giá hiện chỉ có dòng giờ cao điểm cho THOAI... Nếu gắn cờ cao điểm cho SMS,
> engine tính cước ở Phase 4 sẽ đi tìm dòng giá `SMS + gio_cao_diem = 1`, không tìm
> thấy, và toàn bộ CDR loại đó rơi vào trạng thái LOI."*

Phase 3 đã nhận ra rủi ro này **trên trục `gio_cao_diem`** và xử lý rất tốt: tách hẳn
một lớp dùng chung, viết javadoc giải thích, chứng minh bằng tiêu chí nghiệm thu số 4
("non-THOAI mang cờ cao điểm = 0").

Nhưng **trục `huong` có đúng cùng rủi ro đó và không được bảo vệ gì cả.** Tiêu chí
nghiệm thu chỉ kiểm trục đã nghĩ tới, nên trục còn lại lọt qua toàn bộ 10 tiêu chí.

> **Bài học:** kiểm tra "cờ cao điểm chỉ có ở THOAI" là kiểm tra **một luật cụ thể**.
> Cái đáng kiểm là **bất biến tổng quát**: *mọi tổ hợp `(dịch vụ, hướng, cao điểm)`
> xuất hiện trong CDR đều phải tra được đơn giá*. Bất biến đó bắt được cả hai trục,
> và bắt được cả những trục chưa ai nghĩ tới. Truy vấn ở mục 9.1 chính là nó.

### 3.4. Hai cách xử lý — cần chốt trước khi viết engine

> ✅ **Đã chốt ở mục 4A** — xử lý khác nhau theo bản chất từng tổ hợp, không gộp chung một
> cách. Xem [`PHASE-4-REPORT.md`](PHASE-4-REPORT.md) mục 2.

**Cách A — mở rộng bảng giá + siết quy tắc tổ hợp (đề xuất)**

Xét theo nghiệp vụ viễn thông thật:

| Tổ hợp | Có thật không? | Xử lý |
|---|---|---|
| SMS + QUOC_TE | ✅ Có — nhắn tin quốc tế | Thêm dòng giá (~2.500 đ/tin) |
| DATA + QUOC_TE | ✅ Có — data roaming quốc tế | Thêm dòng giá (đắt, ví dụ 200 đ/MB) |
| DATA + NGOAI_MANG | ❌ Không — data không có khái niệm "ngoại mạng" | Cấm sinh ra |

```sql
INSERT INTO bang_gia_cuoc (goi_cuoc_id, loai_dich_vu, huong, gio_cao_diem,
                           block_giay, don_gia, ngay_hieu_luc, ngay_het_hieu_luc) VALUES
(NULL, 'SMS',  'QUOC_TE', 0, 1, 2500, '2025-01-01', NULL),
(NULL, 'DATA', 'QUOC_TE', 0, 1,  200, '2025-01-01', NULL);
```

Kèm theo: tạo lớp `QuyTacToHopDichVu` — **nơi duy nhất** định nghĩa tổ hợp
`(dịch vụ, hướng)` nào hợp lệ, dùng chung cho cả bộ sinh và bộ nhập CSV, đúng theo
mô hình `QuyTacGioCaoDiem` đã có. Sau đó chạy lại profile `reset` và sinh lại 5000 CDR
để xoá sạch 186 bản ghi `DATA/NGOAI_MANG` cũ.

**Cách B — chỉ siết bộ sinh, không thêm dòng giá nào**

Cấm cả ba tổ hợp trong `QuyTacToHopDichVu` (DATA luôn `NOI_MANG`, SMS không
`QUOC_TE`), rồi sinh lại. Ít việc hơn, nhưng bảng giá mất hai hướng có thật, và
màn hình bảng giá trông nghèo hơn khi demo.

**Cách C — engine tự lùi về giá mặc định khi không tra được (KHÔNG nên)**

Che vấn đề đi. CDR sai vẫn ra tiền, và trạng thái `LOI` — thứ được thiết kế ra
đúng để bắt tình huống này — thành ra vô dụng.

---

## 4. ⚠️ Có BA chỗ quy đổi đơn vị, không phải hai

`docs/mo-ta-csdl.md` mục 6 cảnh báo rất kỹ về hai chỗ quy đổi KB/MB. Rà soát lại thì
còn **một chỗ thứ ba cùng loại, cùng mức nguy hiểm, chưa được ghi ở đâu cả**.

| # | Cột / hằng | Đơn vị lưu | Đối chiếu với | Sai số nếu quên |
|---|---|---|---|---|
| 1 | `chi_tiet_su_dung.so_luong` (DATA) | **KB** | đơn giá 25 đ/MB | ×1024 |
| 2 | `goi_cuoc.data_mien_phi_mb` | **MB** | sản lượng KB | ×1024 |
| 3 | **`goi_cuoc.phut_*_mien_phi`** | **PHÚT** | **`thoi_luong_giay` (GIÂY)** | **×60** |

### 4.1. Chỗ thứ ba nguy hiểm y hệt chỗ thứ hai

Gói MAX70 cho 100 **phút** nội mạng. Thuê bao gọi tổng 5.400 **giây** = 90 phút —
**vẫn trong ưu đãi, đáng lẽ không mất tiền thoại nội mạng**.

Nếu engine so thẳng `5400 > 100` thì kết luận vượt ưu đãi và tính cước cho toàn bộ.
Hóa đơn vẫn phát hành bình thường, không lỗi, không cảnh báo — đúng cái chữ ký của
lỗi mà mục 6 của `mo-ta-csdl.md` đã mô tả.

Ba chỗ này giống nhau đến mức nên gom về **một lớp `DonViCuoc` duy nhất**:

```java
public final class DonViCuoc {
    public static final int KB_MOI_MB  = 1024;
    public static final int GIAY_MOI_PHUT = 60;

    public static BigDecimal kbSangMb(long soKb) { ... }
    public static BigDecimal giaySangPhut(long soGiay) { ... }
}
```

### 4.2. Dữ liệu hiện có đủ để kiểm chứng cả bốn loại ưu đãi

Đếm số thuê bao **thực sự còn nằm trong ưu đãi** trong kỳ 6/2026 — đây chính là tập
thuê bao phải có cước tương ứng **bằng 0** sau khi chạy engine:

| Loại ưu đãi | Số thuê bao còn trong ưu đãi |
|---|---|
| Data (MB) | **46** |
| Phút nội mạng | **45** |
| Phút ngoại mạng | **40** |
| SMS | **47** |

> Bốn con số này đo trên dữ liệu CDR **trước** mục 4A. Mục 4A sinh lại toàn bộ CDR nên chúng
> đã thay đổi — số liệu dùng cho 4D nằm ở `PHASE-4-REPORT.md` mục 20.2.

Nếu sau khi chạy engine mà bất kỳ nhóm nào trong bốn nhóm này có cước khác 0 →
engine đang quên đúng chỗ quy đổi tương ứng. Câu SQL ở mục 9.2.

---

## 5. Mười quyết định nghiệp vụ phải chốt trước khi viết code

> ### ✅ Đối chiếu sau khi Phase 4 hoàn tất
>
> Cả 10 quyết định đều đã được thực hiện. Một quyết định phải **làm rõ thêm** trong quá
> trình làm, không phải đổi ý:
>
> | # | Nội dung | Trạng thái | Cài đặt ở |
> |---|---|---|---|
> | 5.1 | Làm tròn block lên (CEILING) | ✅ đúng như chốt | `DonViCuoc.soBlock` — 4A |
> | 5.2 | Ưu đãi trừ theo **sản lượng thô** | ✅ đúng như chốt, **đã làm rõ** ⬇ | `UuDaiGoiCuoc` — 4D |
> | 5.3 | Không cắt đôi bản ghi làm vượt quota | ✅ đúng như chốt | `UuDaiGoiCuoc.Quy` — 4D |
> | 5.4 | Rating cho mọi thuê bao, billing chỉ trả sau | ✅ đúng như chốt | `BillingService` — 4C |
> | 5.5 | Tạm ngưng vẫn thu cước thuê bao | ✅ đúng như chốt | `QuyTacKyCuoc` — 4C |
> | 5.6 | Prorate cước thuê bao, ưu đãi trọn tháng | ✅ đúng như chốt | `QuyTacKyCuoc` — 4C |
> | 5.7 | VAT 10% là hằng số | ✅ đúng như chốt | `ThamSoTinhCuoc` — 4A |
> | 5.8 | Làm tròn ở đúng một tầng — tầng CDR | ✅ đúng như chốt | `ThamSoTinhCuoc.lamTronTien` — 4A |
> | 5.9 | Chỉ xử lý `CHUA_TINH`/`LOI`; có đường hủy và gỡ kỳ kẹt | ✅ đúng như chốt | `RatingService` — 4B |
> | 5.10 | Gói lấy theo `dang_ky_goi_cuoc` | ✅ đúng như chốt | `QuyTacKyCuoc` — 4C |
>
> **Chỗ phải làm rõ ở 5.2.** Quyết định gốc chỉ nói *"trừ theo sản lượng thô, không theo
> block đã làm tròn"* — tức phân biệt với **block tính cước**. Nhưng vẫn còn một câu chưa
> trả lời: khi so sản lượng (giây, KB) với quota (phút, MB) thì quy đổi theo chiều nào?
> Mục 4D đo cả hai chiều và chọn **so ở đơn vị nhỏ nhất** (quy quota xuống giây và KB), vì
> chiều ngược lại làm tròn lên từng bản ghi rồi cộng dồn nên ăn mòn quỹ của khách — đo được
> **+10,97%** với thoại, làm 4 thuê bao bị coi là vượt quota trong khi chưa hề vượt. Đây là
> cách đọc trung thành nhất với chính lý do đã ghi ở 5.2. Chi tiết:
> [`PHASE-4-REPORT.md`](PHASE-4-REPORT.md) mục 23.

Đây là các chỗ mà **mọi lựa chọn đều "chạy được"**, nên nếu không chốt trước thì sẽ
chốt ngầm bằng dòng code đầu tiên viết ra, và không ai rà lại được nữa.

### 5.1. Làm tròn block

`block_giay = 6` với thoại. Cuộc 45 giây → `ceil(45/6) = 8` block × 15 đ = **120 đ**
(không phải 112,5 đ).

Chuyện này **không hề lý thuyết**: trong 3537 cuộc thoại hiện có, **2930 cuộc (82,8%)
không chia hết cho 6**. Quy tắc làm tròn quyết định gần như toàn bộ cước thoại.

- **Đề xuất:** làm tròn **lên** (`RoundingMode.CEILING`) theo đúng thông lệ ngành.
- Phải chặn `blockGiay` null hoặc 0 trước khi chia — nếu không là `ArithmeticException`
  giữa lúc tính cả kỳ.

### 5.2. Ưu đãi tính theo sản lượng thô hay theo block đã làm tròn?

Cuộc 45 giây trừ vào quỹ ưu đãi 45 giây hay 48 giây (8 block × 6)?
**Đề xuất:** trừ theo **sản lượng thô**. Ưu đãi là ưu đãi của khách, không nên bị
hao thêm vì quy tắc làm tròn của nhà mạng.

### 5.3. Bản ghi làm vượt ngưỡng ưu đãi thì xử lý sao?

Còn 30 giây ưu đãi, phát sinh cuộc 200 giây. Cắt đôi (30 giây miễn phí + 170 giây
tính tiền) hay tính tiền cả cuộc?

**Đề xuất: KHÔNG cắt đôi bản ghi.** Lý do là ràng buộc của chính CSDL: mỗi CDR chỉ có
**một** cột `cuoc_phi` và **một** cờ `mien_phi`. Cắt đôi thì bản ghi vừa `mien_phi = 1`
vừa `cuoc_phi > 0` — tự mâu thuẫn, và không có cột nào lưu "phần vượt".

Quy tắc: duyệt CDR **theo thứ tự thời gian**, bản ghi nào còn nằm **trọn** trong ưu đãi
thì `mien_phi = 1, cuoc_phi = 0`; bản ghi làm vượt ngưỡng và mọi bản ghi sau đó thì
`mien_phi = 0` và tính tiền đầy đủ.

Đây là **xấp xỉ có chủ đích** — phải ghi rõ vào báo cáo Phase 4, không được lờ đi.
Thứ tự duyệt phải cố định (`ORDER BY thoi_gian_bat_dau, id`) để chạy hai lần ra cùng
kết quả.

### 5.4. Thuê bao trả trước — 16 thuê bao, 1148 CDR

Gói TT01 có cước thuê bao tháng = 0 và không ưu đãi gì. Nghiệp vụ trả trước là trừ
thẳng số dư, **không lập hóa đơn tháng**.

**Đề xuất: tách rating khỏi billing.**
- **Rating** (tính `cuoc_phi` cho từng CDR): chạy cho **tất cả** thuê bao, cả trả trước.
- **Billing** (lập `hoa_don`): chỉ cho thuê bao **TRA_SAU**.

⚠️ **Cảnh báo về số dư trả trước.** Nếu Phase 4 (hoặc Phase 5) trừ cước vào `so_du`
thì gần như toàn bộ 16 thuê bao sẽ **âm số dư**, vì dữ liệu mẫu nạp số dư quá nhỏ so
với sản lượng đã sinh:

| Số thuê bao | Số dư | Thoại (giây) | SMS | Data (MB) | Cước ước tính |
|---|---|---|---|---|---|
| 0841234511 | 15.000 đ | 12.128 | 13 | 2.189 | ~100.000 đ |
| 0933456703 | 27.500 đ | 10.349 | 15 | 1.671 | ~85.000 đ |
| 0901234501 | 52.000 đ | 11.887 | 18 | 2.567 | ~110.000 đ |

Đây là **hệ quả của dữ liệu mẫu, không phải lỗi engine**. Chốt một trong hai:
(a) Phase 4 chỉ tính `cuoc_phi`, chưa động vào `so_du` — dồn sang Phase 5; hoặc
(b) nâng số dư mẫu trong `data-mau.sql` lên mức hợp lý (200.000–500.000 đ) trước khi trừ.
**Đề xuất (a)** — giữ Phase 4 thuần rating + billing trả sau.

### 5.5. Thuê bao ở trạng thái tạm ngưng có phải trả cước thuê bao tháng?

Phân bố thuê bao trả sau: 50 `HOAT_DONG`, 5 `TAM_NGUNG_1C`, 3 `TAM_NGUNG_2C`,
2 `DA_THANH_LY`.

**Đề xuất:**

| Trạng thái | Lập hóa đơn? | Cước thuê bao | Ghi chú |
|---|---|---|---|
| `HOAT_DONG` | ✅ | Đủ (hoặc prorate) | |
| `TAM_NGUNG_1C` | ✅ | Đủ | Còn nhận cuộc gọi = còn dùng dịch vụ. Có 1 CDR thuộc nhóm này |
| `TAM_NGUNG_2C` | ✅ | Đủ | Số vẫn giữ chỗ cho khách. Không có CDR |
| `DA_THANH_LY` | ❌ | — | Trừ khi `ngay_huy` rơi trong kỳ → hóa đơn chốt cuối, prorate |

### 5.6. Công thức prorate cước thuê bao

```
cuocThueBao = cuocThueBaoThang × soNgaySuDung / soNgayTrongKy
```

Với `soNgaySuDung` tính **bao gồm cả ngày kích hoạt**. Ví dụ thuê bao 0819123480
(gói MAX150, 150.000 đ) kích hoạt 23/06/2026: `30 − 23 + 1 = 8` ngày →
`150.000 × 8/30 = 40.000 đ`.

Câu hỏi đi kèm: **ưu đãi có prorate theo không?**
**Đề xuất: KHÔNG.** Cước thuê bao prorate, ưu đãi giữ nguyên trọn tháng. Dễ giải thích,
có lợi cho khách, và tránh phải xử lý "2048 MB × 8/30 = 546,13 MB" lẻ.

Năm thuê bao dùng để kiểm chứng, giá trị kỳ vọng tính sẵn:

| Số thuê bao | Gói | Cước tháng | Kích hoạt | Số ngày | Cước prorate kỳ vọng |
|---|---|---|---|---|---|
| 0967901278 | DN500 | 500.000 | 05/06 | 26 | **433.333 đ** |
| 0834567834 | MAX70 | 70.000 | 11/06 | 20 | **46.667 đ** |
| 0978012379 | MAX150 | 150.000 | 15/06 | 16 | **80.000 đ** |
| 0845678935 | MAX150 | 150.000 | 17/06 | 14 | **70.000 đ** |
| 0819123480 | MAX150 | 150.000 | 23/06 | 8 | **40.000 đ** |

### 5.7. Thuế VAT — hiện không có chỗ nào lưu thuế suất

`hoa_don.thue_vat` là `NOT NULL` nhưng không bảng nào, cột nào lưu thuế suất.

**Đề xuất:** hằng số `ThamSoTinhCuoc.THUE_SUAT_VAT = 0.10` (10%), ghi rõ trong tài liệu.
Đừng thêm bảng cấu hình cho một hằng số duy nhất.

### 5.8. Làm tròn tiền và bất biến cộng dồn

Đây là chỗ dễ ra hóa đơn "lệch 1 đồng" mà rất khó truy.

- VND không có đơn vị nhỏ hơn đồng → làm tròn `HALF_UP` về **0 chữ số thập phân**,
  lưu vào cột `DECIMAL(15,2)` dưới dạng `x.00`.
- **Làm tròn ở đúng một tầng: từng CDR.** Các mức trên chỉ cộng dồn, không làm tròn lại.
  Làm tròn ở nhiều tầng thì `SUM(cuoc_phi) ≠ hoa_don.cuoc_thoai` và không ai giải thích nổi.
- Bất biến phải đúng sau khi chạy, kiểm bằng SQL ở mục 9.3:

```
hoa_don.cuoc_thoai = SUM(cuoc_phi) các CDR THOAI của thuê bao trong kỳ
tong_truoc_thue    = cuoc_thue_bao + cuoc_thoai + cuoc_sms + cuoc_data + cuoc_khac − giam_tru
thue_vat           = ROUND(tong_truoc_thue × 0.10)
tong_thanh_toan    = tong_truoc_thue + thue_vat
con_no             = tong_thanh_toan − da_thanh_toan
```

### 5.9. Chạy lại engine — chống trùng và cách gỡ

`hoa_don` có `UNIQUE(thue_bao_id, ky_cuoc_id)`. Chạy engine lần hai trên cùng kỳ sẽ
ném `DataIntegrityViolationException`.

Theo đúng bài học 4.4 của Phase 3 ("chặn ở tầng nghiệp vụ dù CSDL đã có ràng buộc"):

- Engine **chỉ xử lý CDR ở trạng thái `CHUA_TINH` và `LOI`**, bỏ qua `DA_TINH`.
- Kiểm tra kỳ trước khi chạy, ném `NghiepVuException` tiếng Việt nếu kỳ đã `DA_CHOT`
  hoặc đã có hóa đơn.
- Có chức năng **"Hủy kết quả tính cước kỳ X"**: xoá hóa đơn của kỳ, đưa CDR về
  `CHUA_TINH`, gỡ `ky_cuoc_id`, đặt kỳ về `MO`. Chỉ cho phép khi kỳ **chưa** `DA_CHOT`.
- Vòng đời kỳ: `MO → DANG_TINH → MO` (chạy xong, chưa chốt) và `MO → DA_CHOT` (chốt).
  ⚠️ Nếu engine chết giữa chừng, kỳ kẹt ở `DANG_TINH` và không chạy lại được — phải có
  đường gỡ (đặt lại về `MO`), nếu không thì phải sửa tay trong CSDL.

### 5.10. Lấy gói cước từ đâu — `thue_bao` hay `dang_ky_goi_cuoc`?

`thue_bao.goi_cuoc_id` là gói **hiện hành**; `dang_ky_goi_cuoc` giữ **lịch sử**. Nếu
khách đổi gói giữa kỳ thì hai nguồn cho hai kết quả khác nhau.

Hiện tại hai nguồn khớp 100% (0 lệch) nên chưa lộ ra, nhưng chức năng đổi gói **đã có
từ Phase 2** — chỉ cần một thao tác đổi gói là dữ liệu lệch ngay.

**Đề xuất:** tra gói qua `dang_ky_goi_cuoc` theo **ngày cuối kỳ**, lùi về
`thue_bao.goi_cuoc_id` nếu không tìm thấy bản ghi nào. Ghi rõ đây là xấp xỉ: nếu đổi
gói giữa kỳ, cả kỳ tính theo gói cuối kỳ, không chia đôi kỳ theo gói.

---

## 6. Kiến trúc đề xuất

Đặt toàn bộ trong `service/rating/` cạnh `QuyTacGioCaoDiem` đã có.

| Lớp | Trách nhiệm | Vì sao tách riêng |
|---|---|---|
| `DonViCuoc` | Hằng số + quy đổi KB↔MB, giây↔phút | Ba chỗ quy đổi ở mục 4, một nơi duy nhất |
| `QuyTacToHopDichVu` | Tổ hợp `(dịch vụ, hướng)` nào hợp lệ | Chặn đường số 1; cùng mô hình `QuyTacGioCaoDiem` |
| `ThamSoTinhCuoc` | VAT, chế độ làm tròn, hạn thanh toán | Tham số nghiệp vụ tập trung, dễ đối chiếu báo cáo |
| `BangGiaLookup` | Nạp sẵn bảng giá vào `Map`, tra theo `(gói, dịch vụ, hướng, cao điểm, ngày)` | Xem mục 6.1 |
| `RatingService` | Tính `cuoc_phi` cho từng CDR | Rating thuần tuý, unit test không cần CSDL |
| `UuDaiGoiCuoc` | Quỹ ưu đãi của một thuê bao trong kỳ, trừ dần | Trạng thái có thể test độc lập |
| `BillingService` | Gom CDR → `hoa_don` + `chi_tiet_hoa_don` | Tách khỏi rating, đúng như mục 5.4 |
| `TinhCuocController` | Màn hình chạy tính cước theo kỳ | |

### 6.1. Vì sao `BangGiaLookup` phải nạp sẵn vào bộ nhớ

Kỳ 6/2026 có 5017 CDR. Nếu tra giá bằng một câu SELECT cho mỗi bản ghi thì tính cước
một kỳ sinh **5017 câu truy vấn**. Bảng giá chỉ có 9–11 dòng — nạp một lần vào `Map`
là xong.

`CdrImportService` đã dùng đúng kỹ thuật này để tra thuê bao (`Map<String, ThueBao>`,
kèm bình luận giải thích). Làm lại y hệt ở đây là nhất quán với repo.

⚠️ **Không dùng `BangGiaCuocRepository.timCoHieuLucTaiNgay(ngay)`** cho việc này. Hàm
đó trả bảng giá tại **một** ngày, trong khi CDR rải suốt tháng và bảng giá có khoảng
hiệu lực — tra theo "ngày chạy engine" hoặc "ngày cuối kỳ" là **vi phạm trực tiếp**
việc số 2 mà PHASE-3-REPORT mục 9 đã dặn ("tra giá theo ngày phát sinh của CDR, không
phải ngày hiện tại"). Nạp **toàn bộ** bảng giá rồi giải quyết khoảng hiệu lực trong
bộ nhớ theo `thoi_gian_bat_dau` của từng CDR.

### 6.2. Thứ tự ưu tiên khi tra giá

1. Dòng gắn **đúng gói cước** của thuê bao, còn hiệu lực tại ngày phát sinh CDR
2. Dòng **mặc định chung** (`goi_cuoc_id IS NULL`), còn hiệu lực tại ngày đó
3. Không có dòng nào → CDR về `LOI`, **không** lùi về giá bất kỳ (mục 3.4 cách C)

Nếu bước 1 hoặc 2 tìm được **nhiều hơn một** dòng thì bảng giá đang chồng khoảng —
lẽ ra không xảy ra vì Phase 3C đã chặn, nhưng engine vẫn nên ghi log cảnh báo thay vì
âm thầm lấy dòng đầu tiên.

### 6.3. Ranh giới thời gian của kỳ — dùng nửa mở

Repository hiện có `findByThueBaoIdAndThoiGianBatDauBetween`. `BETWEEN` của JPA là
**bao gồm hai đầu**, nên phải dựng đúng cận trên. Dùng nửa mở cho an toàn:

```java
// ĐÚNG: [00:00:00 ngày đầu kỳ, 00:00:00 ngày đầu kỳ sau)
tuLuc  = kyCuoc.getNgayBatDau().atStartOfDay();
denLuc = kyCuoc.getNgayKetThuc().plusDays(1).atStartOfDay();   // rồi dùng <
```

Viết `ngayKetThuc.atTime(23, 59, 59)` sẽ **bỏ sót** bản ghi có phần lẻ giây. Dữ liệu
hiện tại chỉ lưu tới giây nên chưa lộ, nhưng có **10 CDR phát sinh lúc 23 giờ ngày
30/06** — sát ngay ranh giới đó. Loại lỗi này mất vài bản ghi mà không báo gì.

---

## 7. Lộ trình Phase 4

| Mục | Nội dung | Phụ thuộc |
|---|---|---|
| **4A** | Chốt 10 quyết định ở mục 5; xử lý chặn đường số 1 (mục 3.4); viết `DonViCuoc`, `QuyTacToHopDichVu`, `ThamSoTinhCuoc` + unit test | — |
| **4B** | `BangGiaLookup` — nạp Map, tra theo ngày phát sinh, ưu tiên gói riêng. Unit test không cần CSDL | 4A |
| **4C** | `RatingService` — tính `cuoc_phi` từng CDR, làm tròn block, gán `ky_cuoc_id`, đặt `DA_TINH` / `LOI` | 4B |
| **4D** | `UuDaiGoiCuoc` — quỹ ưu đãi 4 loại, trừ dần theo thứ tự thời gian, đánh `mien_phi` | 4C |
| **4E** | `BillingService` — lập hóa đơn trả sau, prorate, VAT, giảm trừ, chống chạy trùng | 4D |
| **4F** | Màn hình chạy tính cước theo kỳ + chốt kỳ + hủy kết quả; cập nhật `so_cdr_xu_ly`, `so_hoa_don_tao`, `tong_doanh_thu` | 4E |
| **4G** | Chạy thật kỳ 6/2026, đối chiếu bộ SQL mục 9, viết `PHASE-4-REPORT.md` | 4F |

Làm **4A trước tiên và trọn vẹn** — đúng theo cách Phase 3 làm mục A (dọn nợ) trước
khi viết nghiệp vụ mới, và cách làm đó đã tỏ ra có giá trị ngay trong chính phase đó.

---

## 8. Tiêu chí nghiệm thu đề xuất (12 tiêu chí)

| # | Tiêu chí | Cách đo |
|---|---|---|
| 1 | `mvnw test` PASS, tối thiểu **55 test** (30 cũ + ≥25 mới) | Dòng `Results: Tests run:` |
| 2 | **0 CDR ở trạng thái `LOI`** sau khi chạy kỳ 6/2026 | SQL 9.1 |
| 3 | Mọi tổ hợp `(dịch vụ, hướng, cao điểm)` trong CDR đều tra được đơn giá | SQL 9.1 |
| 4 | Thuê bao còn trong ưu đãi data có `cuoc_data = 0` | SQL 9.2 |
| 5 | Thuê bao còn trong ưu đãi phút NM / phút NgM / SMS có cước tương ứng = 0 | SQL 9.2 |
| 6 | `SUM(cuoc_phi)` theo dịch vụ **khớp tuyệt đối** với các cột trên `hoa_don` | SQL 9.3 |
| 7 | `tong_truoc_thue`, `thue_vat`, `tong_thanh_toan`, `con_no` khớp công thức mục 5.8 | SQL 9.3 |
| 8 | 5 thuê bao prorate ra **đúng** giá trị kỳ vọng ở bảng mục 5.6 | SQL 9.4 |
| 9 | Chạy engine lần hai trên cùng kỳ bị chặn bằng thông báo tiếng Việt, **không** tạo hóa đơn trùng | Thao tác trên giao diện |
| 10 | Chốt kỳ rồi thì không chạy lại được; hủy kết quả đưa hệ thống về đúng trạng thái trước khi chạy | Thao tác + SQL 9.5 |
| 11 | Tính cước 5017 CDR **dưới 10 giây**, có số đo, và có đếm số câu truy vấn phát sinh | Log + số đo |
| 12 | Thuê bao trả trước **không** sinh hóa đơn, nhưng CDR của họ vẫn có `cuoc_phi` | SQL 9.6 |

> Về tiêu chí 11: Phase 3 đã cho thấy "đạt tiêu chí" và "làm đúng" là hai chuyện khác
> nhau — cả phương án 1150 ms lẫn 257 ms đều đạt mốc "dưới 10 giây". Vì vậy tiêu chí
> này đòi thêm **số câu truy vấn**, là con số phản ánh đúng thứ cần kiểm (mục 6.1).

---

## 9. Bộ SQL tự kiểm chứng sau khi chạy engine

### 9.1. Không còn CDR nào tra không ra giá (tiêu chí 2, 3)

```sql
-- Ca hai cau phai tra ve 0
SELECT COUNT(*) AS cdr_loi FROM chi_tiet_su_dung WHERE trang_thai_tinh_cuoc = 'LOI';

SELECT COUNT(*) AS to_hop_khong_co_gia
FROM chi_tiet_su_dung c
WHERE NOT EXISTS (
    SELECT 1 FROM bang_gia_cuoc b
     WHERE b.goi_cuoc_id IS NULL
       AND b.loai_dich_vu = c.loai_dich_vu
       AND b.huong        = c.huong
       AND b.gio_cao_diem = c.gio_cao_diem
       AND b.ngay_hieu_luc <= DATE(c.thoi_gian_bat_dau)
       AND (b.ngay_het_hieu_luc IS NULL OR b.ngay_het_hieu_luc >= DATE(c.thoi_gian_bat_dau)));
```

### 9.2. Bốn loại ưu đãi (tiêu chí 4, 5) — cả bốn phải trả về 0 dòng

```sql
-- DATA: thue bao con trong uu dai ma van bi tinh tien -> engine quen chia 1024
SELECT t.so_thue_bao, g.ma_goi, g.data_mien_phi_mb AS uu_dai_mb,
       ROUND(SUM(c.so_luong)/1024, 2) AS da_dung_mb, h.cuoc_data
FROM chi_tiet_su_dung c
JOIN thue_bao t ON t.id = c.thue_bao_id
JOIN goi_cuoc g ON g.id = t.goi_cuoc_id
JOIN hoa_don  h ON h.thue_bao_id = t.id AND h.ky_cuoc_id = c.ky_cuoc_id
WHERE c.loai_dich_vu = 'DATA' AND c.ky_cuoc_id = 1
GROUP BY t.id, h.id
HAVING da_dung_mb < uu_dai_mb AND h.cuoc_data <> 0;

-- THOAI noi mang: con trong uu dai phut ma van bi tinh tien -> engine quen chia 60
SELECT t.so_thue_bao, g.ma_goi, g.phut_noi_mang_mien_phi AS uu_dai_phut,
       ROUND(SUM(c.thoi_luong_giay)/60, 2) AS da_dung_phut, h.cuoc_thoai
FROM chi_tiet_su_dung c
JOIN thue_bao t ON t.id = c.thue_bao_id
JOIN goi_cuoc g ON g.id = t.goi_cuoc_id
JOIN hoa_don  h ON h.thue_bao_id = t.id AND h.ky_cuoc_id = c.ky_cuoc_id
WHERE c.loai_dich_vu = 'THOAI' AND c.huong = 'NOI_MANG' AND c.ky_cuoc_id = 1
  AND g.phut_noi_mang_mien_phi > 0
GROUP BY t.id, h.id
HAVING da_dung_phut < uu_dai_phut AND h.cuoc_thoai <> 0;
```

(Hai câu còn lại cho phút ngoại mạng và SMS viết tương tự, đổi `huong` và cột ưu đãi.)

### 9.3. Bất biến cộng dồn của hóa đơn (tiêu chí 6, 7) — phải trả về 0 dòng

```sql
SELECT h.ma_hoa_don, h.cuoc_thoai, x.tong_thoai, h.cuoc_sms, x.tong_sms,
       h.cuoc_data, x.tong_data
FROM hoa_don h
JOIN (SELECT c.thue_bao_id, c.ky_cuoc_id,
             COALESCE(SUM(CASE WHEN c.loai_dich_vu='THOAI' THEN c.cuoc_phi END),0) AS tong_thoai,
             COALESCE(SUM(CASE WHEN c.loai_dich_vu='SMS'   THEN c.cuoc_phi END),0) AS tong_sms,
             COALESCE(SUM(CASE WHEN c.loai_dich_vu='DATA'  THEN c.cuoc_phi END),0) AS tong_data
      FROM chi_tiet_su_dung c WHERE c.trang_thai_tinh_cuoc='DA_TINH'
      GROUP BY c.thue_bao_id, c.ky_cuoc_id) x
  ON x.thue_bao_id = h.thue_bao_id AND x.ky_cuoc_id = h.ky_cuoc_id
WHERE h.cuoc_thoai <> x.tong_thoai
   OR h.cuoc_sms   <> x.tong_sms
   OR h.cuoc_data  <> x.tong_data;

SELECT ma_hoa_don, tong_truoc_thue, thue_vat, tong_thanh_toan, con_no FROM hoa_don
WHERE tong_truoc_thue <> cuoc_thue_bao + cuoc_thoai + cuoc_sms + cuoc_data + cuoc_khac - giam_tru
   OR tong_thanh_toan <> tong_truoc_thue + thue_vat
   OR con_no          <> tong_thanh_toan - da_thanh_toan;
```

### 9.4. Prorate (tiêu chí 8)

```sql
SELECT t.so_thue_bao, t.ngay_kich_hoat, g.cuoc_thue_bao_thang, h.cuoc_thue_bao,
       ROUND(g.cuoc_thue_bao_thang * (DAY(LAST_DAY(k.ngay_bat_dau)) - DAY(t.ngay_kich_hoat) + 1)
             / DAY(LAST_DAY(k.ngay_bat_dau))) AS ky_vong
FROM hoa_don h
JOIN thue_bao t ON t.id = h.thue_bao_id
JOIN goi_cuoc g ON g.id = t.goi_cuoc_id
JOIN ky_cuoc  k ON k.id = h.ky_cuoc_id
WHERE t.ngay_kich_hoat BETWEEN k.ngay_bat_dau AND k.ngay_ket_thuc;
```

### 9.5. Sau khi "hủy kết quả tính cước" — cả ba phải về 0 (tiêu chí 10)

```sql
SELECT (SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id = 1)                              AS hoa_don_con_lai,
       (SELECT COUNT(*) FROM chi_tiet_su_dung WHERE ky_cuoc_id = 1)                     AS cdr_con_gan_ky,
       (SELECT COUNT(*) FROM chi_tiet_su_dung WHERE trang_thai_tinh_cuoc <> 'CHUA_TINH') AS cdr_chua_reset;
```

### 9.6. Trả trước không có hóa đơn nhưng CDR vẫn có cước (tiêu chí 12)

```sql
SELECT (SELECT COUNT(*) FROM hoa_don h JOIN thue_bao t ON t.id = h.thue_bao_id
         WHERE t.loai_thue_bao = 'TRA_TRUOC')                        AS hd_tra_truoc_phai_bang_0,
       (SELECT COUNT(*) FROM chi_tiet_su_dung c JOIN thue_bao t ON t.id = c.thue_bao_id
         WHERE t.loai_thue_bao = 'TRA_TRUOC' AND c.cuoc_phi IS NULL) AS cdr_tra_truoc_chua_tinh_phai_bang_0;
```

---

## 10. Nợ tài liệu phát hiện khi rà soát

Không sửa trong lần rà soát này để giữ thay đổi gọn. Nên gộp vào mục 4A.

| # | File | Vấn đề | Mức |
|---|---|---|---|
| 1 | `entity/ChiTietSuDung.java:61` | Javadoc `so_luong` còn ghi *"số MB với DATA"* — thực tế là **KB** | 🔴 Cao — người viết engine đọc entity trước tiên, và đây đúng là cái bẫy mục 4 |
| 2 | `entity/BangGiaCuoc.java:49` | Javadoc `blockGiay` ghi *"1 MB với data"*, mâu thuẫn với `block_giay = 1` khi `so_luong` là KB | 🔴 Cao — cùng gốc với #1 |
| 3 | `README.md` §6 | Còn cảnh báo `spring.sql.init.mode: always` và *"CSDL bị dựng lại mỗi lần khởi động"* — đã đổi thành `never` từ Phase 2 | 🟡 Trung bình — cảnh báo sai làm người đọc sợ chạy app |
| 4 | `docs/mo-ta-csdl.md` §5.1 | Cùng lỗi như #3 | 🟡 Trung bình |
| 5 | `docs/mo-ta-csdl.md` §2.2 | `ma_kh` ghi dạng `KH0001` (4 số) — Phase 3A đã chuẩn hoá **6 số** | 🟢 Thấp |
| 6 | `docs/mo-ta-csdl.md` §4 | Bảng dữ liệu mẫu ghi `ky_cuoc = 1 bản ghi` — thực tế **3** | 🟢 Thấp |
| 7 | `docs/` | Có `PHASE-0`, `PHASE-2`, `PHASE-3` nhưng **thiếu `PHASE-1-REPORT.md`** | 🟢 Thấp — nên nêu lý do trong báo cáo cuối |

---

## 11. Tóm tắt

Phase 3 bàn giao một nền dữ liệu **đúng như đã mô tả**: 30 test xanh, 5017 CDR sạch
đúng trạng thái `CHUA_TINH`, 3 kỳ cước, 5 thuê bao prorate, bảng hóa đơn còn rỗng.
Sáu khẳng định kiểm chứng được đều đúng.

Nhưng vẫn còn ba việc phải làm trước khi viết dòng code tính cước nào:

**Một —** bảng giá thiếu 3 tổ hợp, **251 CDR (5,00%)** sẽ rơi thẳng vào `LOI`. Điều
đáng chú ý không phải con số, mà là **vì sao nó lọt qua cả 10 tiêu chí nghiệm thu**:
Phase 3 đã nhận ra đúng kiểu rủi ro này trên trục `gio_cao_diem`, xử lý rất tốt, rồi
viết tiêu chí kiểm **đúng cái luật vừa xử lý** thay vì kiểm **bất biến tổng quát**.
Trục `huong` có cùng rủi ro, không ai kiểm, và lọt.

**Hai —** có **ba** chỗ quy đổi đơn vị, không phải hai. Chỗ thứ ba (`phut_*_mien_phi`
tính bằng phút, đối chiếu với `thoi_luong_giay` tính bằng giây) chưa được ghi ở bất kỳ
tài liệu nào, và có đúng chữ ký nguy hiểm mà `mo-ta-csdl.md` mục 6 đã mô tả: sai 60
lần, hóa đơn vẫn phát hành bình thường, không cảnh báo nào.

**Ba —** mười quyết định nghiệp vụ ở mục 5 phải chốt **trước**, vì mọi lựa chọn đều
"chạy được". Không chốt trước thì chúng sẽ được chốt ngầm bằng dòng code đầu tiên viết
ra, và không ai rà lại được nữa. Riêng làm tròn block ảnh hưởng tới **82,8% số cuộc gọi**.

Cả ba đều thuộc mục 4A. Làm xong 4A rồi mới viết engine.
