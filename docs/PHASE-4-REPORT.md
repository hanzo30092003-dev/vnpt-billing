# BÁO CÁO PHASE 4 — ENGINE TÍNH CƯỚC

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Trạng thái:** 🚧 Đang thực hiện — **mục 4A, 4B, 4C đã xong**, 4D–4G chưa bắt đầu

> Toàn bộ dữ liệu trong hệ thống là dữ liệu mẫu tự sinh phục vụ học tập.
> Hệ thống không sử dụng dữ liệu thật của bất kỳ nhà mạng nào.

Kế hoạch và kết quả rà soát đầu vào nằm ở [`PHASE-4-PLAN.md`](PHASE-4-PLAN.md).

---

## 1. Phạm vi mục 4A

4A là mục dọn đường: chốt quyết định nghiệp vụ, sửa dữ liệu sai mô hình, và dựng các
lớp nền mà `RatingService` sẽ đứng lên. **Tuyệt đối chưa tính cước** — toàn bộ 5017 bản
ghi CDR vẫn ở `CHUA_TINH` với `cuoc_phi = NULL`.

Làm 4A trọn vẹn trước khi viết engine là lặp lại cách Phase 3 làm mục A (dọn nợ Phase 2)
trước khi viết nghiệp vụ mới — cách làm đó đã tỏ ra có giá trị ngay trong chính phase đó.

---

## 2. Ba tổ hợp thiếu bảng giá — quyết định và lý do

Rà soát đầu vào phát hiện **251 bản ghi CDR (5,00%)** thuộc ba tổ hợp không có dòng bảng
giá nào khớp. Cả ba sẽ rơi thẳng vào trạng thái `LOI` ngay lần chạy engine đầu tiên.

Điểm quan trọng: **ba tổ hợp này khác nhau về bản chất nghiệp vụ, nên xử lý khác nhau** —
không gộp chung một cách.

| Tổ hợp | Số CDR | Có thật trong nghiệp vụ? | Xử lý |
|---|---|---|---|
| `DATA + NGOAI_MANG` | 186 | ❌ Không | **Sửa bộ sinh.** Phiên data đi ra Internet, không có khái niệm nội/ngoại mạng. Đây là mô hình hoá sai, không phải thiếu bảng giá |
| `SMS + QUOC_TE` | 49 | ✅ Có | **Thêm dòng bảng giá** 2.500 đ/tin. Giữ nguyên bộ sinh |
| `DATA + QUOC_TE` | 16 | ✅ Có, nhưng ngoài phạm vi | **Sửa bộ sinh.** Đây là data roaming, đã loại khỏi phạm vi đồ án từ kế hoạch gốc |

Quy tắc sau khi chốt:

| Dịch vụ | Hướng hợp lệ | Phân bố |
|---|---|---|
| `THOAI` | NOI_MANG / NGOAI_MANG / QUOC_TE | 55% / 40% / 5% (giữ nguyên) |
| `SMS` | NOI_MANG / NGOAI_MANG / QUOC_TE | 55% / 40% / 5% (giữ nguyên) |
| `DATA` | **NOI_MANG** | 100% |

Cách xử lý bị **loại**: để engine tự lùi về một đơn giá mặc định khi tra không ra. Làm vậy
là che vấn đề — bản ghi sai vẫn ra tiền, và trạng thái `LOI`, thứ được thiết kế ra đúng để
bắt tình huống này, trở thành vô dụng.

---

## 3. ⭐ BÀI HỌC PHƯƠNG PHÁP KIỂM THỬ

Đây là nội dung đáng giá nhất của mục 4A, và là lý do 251 bản ghi kia đáng được viết hẳn
một mục riêng thay vì chỉ sửa lặng lẽ.

### 3.1. Phase 3 đã nhận ra đúng rủi ro này — trên một trục khác

Lớp `QuyTacGioCaoDiem` của Phase 3 có đoạn javadoc mô tả **chính xác** kiểu hỏng đang bàn:

> *"bảng giá hiện chỉ có dòng giờ cao điểm cho THOAI, không có cho SMS và DATA. Nếu gắn cờ
> cao điểm cho SMS, engine tính cước ở Phase 4 sẽ đi tìm dòng giá `SMS + gio_cao_diem = 1`,
> không tìm thấy, và toàn bộ CDR loại đó rơi vào trạng thái LOI."*

Và Phase 3 xử lý rủi ro đó rất tốt:

- Tách hẳn một lớp làm **nơi định nghĩa duy nhất**, dùng chung cho cả bộ sinh và bộ nhập CSV
- Viết javadoc giải thích rõ hậu quả nếu làm sai
- Đặt hẳn **tiêu chí nghiệm thu số 4**: *"non-THOAI mang cờ giờ cao điểm = 0"* — và đo được
  đúng 0

Không có gì để chê ở cách xử lý đó.

### 3.2. Nhưng trục còn lại lọt qua cả 10 tiêu chí

Bảng giá được tra bằng **ba trục**: `loai_dich_vu`, `huong`, `gio_cao_diem`.

Phase 3 nhìn thấy rủi ro trên trục `gio_cao_diem`, xử lý nó, rồi viết một tiêu chí kiểm
**đúng cái luật vừa xử lý**. Trục `huong` có y hệt rủi ro ấy — `chonHuong()` bốc hướng độc
lập với loại dịch vụ, nên sinh ra tổ hợp không có giá — nhưng không ai kiểm, vì không ai
nghĩ tới.

Mười tiêu chí nghiệm thu Phase 3 đều đạt. Dữ liệu vẫn có 5,00% bản ghi hỏng.

### 3.3. Cách chữa không phải là thêm tiêu chí thứ 11

Thêm một tiêu chí *"mọi CDR DATA đều có hướng NOI_MANG"* sẽ vá đúng lỗ hổng vừa phát hiện,
và để nguyên bài học. Trục tiếp theo — hoặc chiều nào đó chưa ai hình dung — vẫn sẽ lọt y
như vậy.

Cách chữa là **đổi thứ được kiểm**:

| | Kiểm luật cụ thể | Kiểm bất biến tổng quát |
|---|---|---|
| Phát biểu | "non-THOAI không mang cờ cao điểm" | "mọi tổ hợp tra giá đều phải tra ra đơn giá" |
| Phạm vi | Đúng một trục, đúng một luật | Mọi trục, kể cả trục chưa ai nghĩ tới |
| Khi thêm trục mới | Phải nhớ viết thêm tiêu chí | Tự động phủ |
| Khi luật đổi | Tiêu chí thành sai, phải sửa tay | Không phải đụng tới |

Bất biến ấy được cài thành `KiemTraDoPhuBangGiaTest`, gồm **hai** test bổ sung cho nhau:

**Test 1 — trên không gian tổ hợp, không phụ thuộc dữ liệu.**
Duyệt mọi tổ hợp mà `QuyTacToHopDichVu` và `QuyTacGioCaoDiem` cho phép tồn tại, rồi hỏi
`BangGiaLookup` xem có tra ra giá không. Test này đúng **ngay cả khi bảng `chi_tiet_su_dung`
còn rỗng**, nên không bao giờ "xanh vì không có gì để kiểm" — và nó sẽ bắt được lỗi 251 bản
ghi **trước khi** sinh ra bản ghi đầu tiên.

**Test 2 — trên dữ liệu thật đang nằm trong CSDL.**
Truy vấn mọi tổ hợp `(gói, dịch vụ, hướng, khung giờ)` thực sự có trong CDR, kiểm ở **cả hai
đầu** khoảng ngày của từng nhóm. Bắt được thứ mà test 1 không thấy: dữ liệu cũ do bộ sinh
đời trước để lại, hoặc bản ghi nhập thẳng bằng SQL.

Một chi tiết nữa: test **hỏi thẳng** `QuyTacGioCaoDiem` bằng hai thời điểm mẫu (một trong
khung, một ngoài khung) để biết dịch vụ nào có thể mang cờ cao điểm, thay vì chép lại luật
"chỉ THOAI mới có cao điểm" vào test. Chép lại thì khi luật đổi, test vẫn xanh theo luật cũ —
đúng kiểu mù mà cả lớp test này sinh ra để tránh.

### 3.4. Bằng chứng: test đỏ trước khi sửa, xanh sau khi sửa

Chạy `mvnw test` **sau khi viết test nhưng trước khi nạp lại dữ liệu**:

```
[ERROR] Tests run: 62, Failures: 2, Errors: 0, Skipped: 0
[INFO] BUILD FAILURE
```

Nội dung hai lần đỏ (trích từ `target/surefire-reports`):

```
moiToHopCoTheSinhRa_deuTraDuocDonGia:
  Bảng giá mặc định chung không phủ hết không gian tổ hợp tại ngày 2026-08-03.
    SMS / QUOC_TE / giờ thường

moiToHopTrongCsdl_deuTraDuocDonGia:
  Đã kiểm 60 tổ hợp (gói, dịch vụ, hướng, khung giờ) có trong chi_tiet_su_dung.
    DATA / NGOAI_MANG / giờ thường — gói id 4, ngày 2026-06-01, ảnh hưởng 79 bản ghi CDR
    DATA / NGOAI_MANG / giờ thường — gói id 3, ngày 2026-06-01, ảnh hưởng 52 bản ghi CDR
    SMS  / QUOC_TE   / giờ thường — gói id 4, ngày 2026-06-01, ảnh hưởng 18 bản ghi CDR
    DATA / QUOC_TE   / giờ thường — gói id 5, ngày 2026-06-01, ảnh hưởng  6 bản ghi CDR
    ... (27 dòng, gộp lại đúng 251 bản ghi)
```

**Sự bất đối xứng giữa hai thông báo chính là chỗ đáng chú ý nhất:**

- Test 1 chỉ báo **một** tổ hợp — `SMS / QUOC_TE`. Vì sau khi `QuyTacToHopDichVu` đã thu hẹp
  luật, hai tổ hợp DATA kia **không còn nằm trong không gian tổ hợp nữa**, nên không cần
  bảng giá. Test 1 chỉ ra đúng một việc phải làm: thêm một dòng giá.
- Test 2 báo **cả ba** — vì 251 bản ghi cũ vẫn nằm trong CSDL.

Nói cách khác: test 1 trả lời *"luật và bảng giá đã khớp nhau chưa?"*, test 2 trả lời
*"dữ liệu đang có đã theo kịp luật chưa?"*. Hai câu hỏi khác nhau, và cần cả hai. Nếu chỉ có
test 2 thì sau khi nạp lại dữ liệu nó sẽ xanh, và ta mất luôn khả năng phát hiện bảng giá
thiếu dòng cho một tổ hợp chưa từng phát sinh CDR.

Sau khi sửa bộ sinh, thêm dòng giá và nạp lại dữ liệu:

```
[INFO] Results:
[INFO] Tests run: 62, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### 3.5. Rút gọn thành một câu

> Tiêu chí nghiệm thu viết sau khi đã hiểu vấn đề sẽ kiểm đúng phần đã hiểu. Muốn bắt được
> phần **chưa** hiểu thì phải kiểm bất biến, không kiểm luật.

---

## 4. Các lớp đã viết

Tất cả nằm trong `service/rating/`, cạnh `QuyTacGioCaoDiem` đã có từ Phase 3.

| Lớp | Vai trò |
|---|---|
| `QuyTacToHopDichVu` | Nơi **duy nhất** định nghĩa tổ hợp `(dịch vụ, hướng)` hợp lệ. Dùng chung cho bộ sinh và bộ nhập CSV |
| `DonViCuoc` | Nơi **duy nhất** quy đổi đơn vị: KB↔MB, giây↔phút, và số block |
| `ThamSoTinhCuoc` | VAT 10%, làm tròn HALF_UP về đồng, hạn thanh toán 15 ngày |
| `BangGiaLookup` | Nạp bảng giá vào `Map` một lần, tra theo chuỗi dự phòng bốn bước |

### 4.1. `DonViCuoc` — ba chỗ quy đổi, một lớp

Rà soát phát hiện có **ba** chỗ quy đổi chứ không phải hai như `mo-ta-csdl.md` mục 6 ghi:

| # | Cột | Đơn vị lưu | Đối chiếu với | Sai nếu quên |
|---|---|---|---|---|
| 1 | `chi_tiet_su_dung.so_luong` (DATA) | KB | đơn giá theo MB | ×1024 |
| 2 | `goi_cuoc.data_mien_phi_mb` | MB | sản lượng lưu bằng KB | ×1024 |
| 3 | **`goi_cuoc.phut_*_mien_phi`** | **PHÚT** | **`thoi_luong_giay` lưu bằng GIÂY** | **×60** |

Chỗ thứ ba chưa được ghi ở bất kỳ tài liệu nào trước đây, và có đúng chữ ký nguy hiểm của
hai chỗ kia: hóa đơn vẫn phát hành bình thường, không lỗi, không cảnh báo, chỉ sai số tiền.

Cả ba dùng chung một hàm `soBlock()`, viết bằng phép chia nguyên `(a + b - 1) / b` thay vì
`Math.ceil` trên số thực — số nguyên không có sai số dấu phẩy động, nên không bao giờ gặp
chuyện `3000/60` ra `49,999...` rồi làm tròn thành 50 block.

Làm tròn **lên** với cả ba phép: dùng dở một block thì vẫn tính trọn block. Chuyện này không
hề lý thuyết — trong 3499 cuộc thoại hiện có, **82,8% không chia hết cho block 6 giây**, nên
quy tắc làm tròn quyết định gần như toàn bộ cước thoại.

### 4.2. `BangGiaLookup` — chuỗi dự phòng bốn bước

```
1. giá riêng của gói  + đúng khung giờ
2. giá riêng của gói  + khung giờ thường
3. giá mặc định chung + đúng khung giờ
4. giá mặc định chung + khung giờ thường
→ không có bước nào tìm được: ném NghiepVuException nêu rõ tổ hợp
```

Chuỗi này chỉ lùi trên trục **khung giờ**, không lùi trên trục `(dịch vụ, hướng)`. Lý do:
giá cao điểm là một **bậc phụ thu tuỳ chọn** — thiếu nó thì dùng giá thường là hợp lý. Còn
dịch vụ và hướng là **danh tính** của bản ghi; tra không ra nghĩa là dữ liệu sai chứ không
phải thiếu bậc giá.

Và chuỗi chỉ đi **một chiều**: bản ghi giờ thường tuyệt đối không được lấy giá cao điểm.
Có hẳn một test cho điều này (`BangGiaLookupTest` số 5) vì đây là lỗi thu vượt tiền của khách.

Hai điểm cài đặt đáng ghi lại:

- **Nạp sẵn vào bộ nhớ.** Một kỳ có khoảng 5000 CDR; tra giá bằng một câu SELECT cho mỗi bản
  ghi sẽ sinh 5000 truy vấn. Bảng giá chỉ có 10 dòng — nạp một lần là xong.
  `CdrImportService` đã dùng đúng kỹ thuật này để tra thuê bao.
- **Không lọc theo ngày lúc nạp.** CDR của một kỳ rải suốt tháng và bảng giá có khoảng hiệu
  lực, nên phải tra theo **ngày phát sinh của từng bản ghi**. Dùng
  `timCoHieuLucTaiNgay(ngay)` có sẵn sẽ vi phạm trực tiếp việc số 2 mà PHASE-3-REPORT mục 9
  đã dặn.

Lớp ảnh chụp `BangGiaLookup.Bang` được tách khỏi bean Spring và dựng được thẳng từ một
`List`, nên toàn bộ 9 test của chuỗi dự phòng chạy không cần Spring context lẫn CSDL.

---

## 5. Nạp lại dữ liệu CDR

251 bản ghi cũ thuộc mô hình sai không sửa được bằng `UPDATE` — hướng của chúng vốn không nên
tồn tại. Cách xử lý là dựng lại từ đầu bằng bộ sinh đã sửa:

1. `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"` — nạp lại schema và dữ liệu mẫu,
   trong đó `data-mau.sql` nay có 10 dòng bảng giá
2. Sinh 5000 CDR cho 01/06–30/06/2026 — mất **235 ms**
3. Nhập lại `mau-cdr.csv` — **20 dòng: 17 thành công, 3 lỗi**, đúng như Phase 3

> ⚠️ Phải **dừng ứng dụng** trước khi chạy `mvnw test`. DevTools đang bật, mà lần chạy này
> dùng profile `reset` — một lần tự khởi động lại là `schema.sql` chạy lại và xoá sạch dữ
> liệu vừa sinh.

### 5.1. Phân bố sau khi sinh lại

| Loại dịch vụ | Số CDR | Tỷ lệ | Mục tiêu |
|---|---|---|---|
| THOAI | 3499 | 69,7% | 70% |
| SMS | 1008 | 20,1% | 20% |
| DATA | 510 | 10,2% | 10% |

Phân bố hướng **chỉ tính trên THOAI và SMS**, vì DATA nay luôn là `NOI_MANG`:

| Hướng | Số CDR (THOAI + SMS) | Tỷ lệ | Mục tiêu |
|---|---|---|---|
| NOI_MANG | 2491 | 55,3% | 55% |
| NGOAI_MANG | 1799 | 39,9% | 40% |
| QUOC_TE | 217 | 4,8% | 5% |

Phân bố giữ nguyên đúng ở chỗ nó phải giữ nguyên — thay đổi ở 4A là thu hẹp không gian hướng
của DATA, không đụng tới quy tắc bốc hướng của hai dịch vụ kia.

### 5.2. Độ phủ bảng giá sau khi sửa

| Loại dịch vụ | Hướng | Cao điểm | Số CDR | Dòng giá khớp |
|---|---|---|---|---|
| THOAI | NOI_MANG | 1 | 1013 | 1 ✅ |
| THOAI | NOI_MANG | 0 | 924 | 1 ✅ |
| THOAI | NGOAI_MANG | 1 | 731 | 1 ✅ |
| THOAI | NGOAI_MANG | 0 | 660 | 1 ✅ |
| THOAI | QUOC_TE | 1 | 101 | 1 ✅ |
| THOAI | QUOC_TE | 0 | 70 | 1 ✅ |
| SMS | NOI_MANG | 0 | 554 | 1 ✅ |
| SMS | NGOAI_MANG | 0 | 408 | 1 ✅ |
| **SMS** | **QUOC_TE** | 0 | **46** | **1 ✅ (dòng mới)** |
| DATA | NOI_MANG | 0 | 510 | 1 ✅ |

10 tổ hợp, 10 dòng giá khớp, **0 tổ hợp thiếu**. Không còn tổ hợp DATA nào ngoài `NOI_MANG`.

---

## 6. Kết quả nghiệm thu mục 4A

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvnw test` PASS, có `KiemTraDoPhuBangGiaTest` và test của `DonViCuoc` | ✅ | `Tests run: 62, Failures: 0, Errors: 0` — BUILD SUCCESS |
| 2 | Mọi tổ hợp trong `chi_tiet_su_dung` đều có dòng bảng giá khớp | ✅ | `to_hop_thieu = 0` |
| 3 | `COUNT(*) WHERE loai_dich_vu='DATA' AND huong <> 'NOI_MANG'` | ✅ | **0** |
| 4 | Toàn bộ CDR vẫn `CHUA_TINH`, `cuoc_phi IS NULL` — 4A chưa tính cước | ✅ | 5017/5017 `CHUA_TINH`, 5017/5017 `cuoc_phi NULL`, 5017/5017 chưa gán kỳ |
| 5 | Bảng giá có 10 dòng | ✅ | **10** |

Kiểm thêm ba bất biến của Phase 3 để chắc chắn 4A không phá thứ gì đang chạy:

| Bất biến Phase 3 | Kết quả |
|---|---|
| Non-THOAI mang cờ giờ cao điểm = 0 | ✅ 0 (SMS 1008 và DATA 510 đều cờ 0) |
| Không CDR nào sớm hơn `ngay_kich_hoat` | ✅ 0 |
| Import `mau-cdr.csv` ra 17 thành công / 3 lỗi | ✅ đúng 17/3 |

### 6.1. Số test theo lớp

| Lớp test | Số test | Nội dung |
|---|---|---|
| `SchemaValidationTest` | 1 | Đối chiếu 15 entity với schema thật |
| `ThueBaoServiceTest` | 17 | Ma trận chuyển trạng thái 4×4 |
| `BangGiaCuocServiceTest` | 9 | Chồng khoảng hiệu lực |
| `SinhMaServiceTest` | 3 | Sinh mã khách hàng |
| **`DonViCuocTest`** | **13** | Ba phép quy đổi + làm tròn block |
| **`BangGiaLookupTest`** | **9** | Chuỗi dự phòng bốn bước |
| **`QuyTacToHopDichVuTest`** | **8** | Tổ hợp dịch vụ / hướng |
| **`KiemTraDoPhuBangGiaTest`** | **2** | ⭐ Bất biến độ phủ bảng giá |
| | **62** | 30 cũ + **32 mới** |

> ⚠️ **Khi chụp ảnh kết quả test:** phải chụp dòng tổng `Results: Tests run: 62`. Bảng phân rã
> theo lớp in ra dòng `Tests run: 0 ... in Ma trận chuyển trạng thái thuê bao` — đó là cách
> Surefire đếm lớp `@Nested` chứ không phải lỗi, nhưng người chấm nhìn con số 0 sẽ hiểu nhầm.

---

## 7. Hạn chế đã biết

### 7.1. Một kỳ cước nằm trong đúng một transaction

Engine sẽ bọc toàn bộ một kỳ — hiện là 5017 CDR và khoảng 60 hóa đơn — trong **một
`@Transactional` duy nhất`**. Ổn ở quy mô đồ án: chạy hết hoặc không chạy gì, không có
trạng thái nửa vời cần dọn.

Hệ thống thật xử lý hàng triệu CDR mỗi kỳ **không làm được như vậy**: một transaction dài
như thế giữ khoá quá lâu, làm phình undo log của InnoDB, và một lỗi ở bản ghi cuối cùng sẽ
huỷ toàn bộ công việc của nhiều giờ. Cách làm thật là chia transaction **theo lô hoặc theo
thuê bao**, kèm bảng theo dõi tiến độ để chạy tiếp được từ chỗ dừng.

Ghi lại ở đây như một giới hạn có ý thức, không phải thứ bị bỏ sót.

### 7.2. Số dư mẫu của thuê bao trả trước quá thấp

16 thuê bao trả trước có số dư 3.000–61.000 đ, trong khi cước phát sinh trong kỳ 6/2026 ước
tính 85.000–110.000 đ:

| Số thuê bao | Số dư | Thoại (giây) | SMS | Data (MB) | Cước ước tính |
|---|---|---|---|---|---|
| 0841234511 | 15.000 đ | 12.128 | 13 | 2.189 | ~100.000 đ |
| 0933456703 | 27.500 đ | 10.349 | 15 | 1.671 | ~85.000 đ |
| 0901234501 | 52.000 đ | 11.887 | 18 | 2.567 | ~110.000 đ |

Đây là **hệ quả của dữ liệu mẫu, không phải lỗi engine**. Phase 4 chỉ tính `cuoc_phi` và
không động vào `so_du` (quyết định 5.4 của kế hoạch), nên chưa lộ ra. Nhưng **trước khi
Phase 5 trừ số dư, hoặc trước khi chụp ảnh báo cáo**, cần nâng số dư mẫu trong `data-mau.sql`
lên khoảng **200.000–500.000 đ** — nếu không, gần như toàn bộ thuê bao trả trước sẽ âm số dư
và ảnh chụp màn hình trông như hệ thống bị lỗi.

### 7.3. Các quyết định là xấp xỉ có chủ đích

Ba quyết định ở mục 5 của kế hoạch cố ý đơn giản hoá, cần nêu rõ trong báo cáo cuối kỳ:

- **Không cắt đôi bản ghi CDR** khi nó làm vượt ngưỡng ưu đãi — vì mỗi CDR chỉ có một cột
  `cuoc_phi` và một cờ `mien_phi`, cắt đôi thì bản ghi tự mâu thuẫn
- **Ưu đãi không prorate** theo ngày kích hoạt, chỉ cước thuê bao mới prorate
- **Đổi gói giữa kỳ thì cả kỳ tính theo gói cuối kỳ**, không chia đôi kỳ

---

## 8. Cập nhật nợ tài liệu

Đã xử lý trong 4A:

| # | File | Việc đã làm |
|---|---|---|
| 1 | `entity/ChiTietSuDung.java` | Javadoc `so_luong` ghi *"số MB với DATA"* → sửa thành **KB**, kèm cảnh báo trỏ sang `DonViCuoc` |
| 2 | `README.md` · `docs/mo-ta-csdl.md` | Số dòng bảng giá mẫu 9 → **10** (do 4A thêm dòng SMS/QUOC_TE) |

Còn tồn, gộp vào mục 4G:

| # | File | Vấn đề | Mức |
|---|---|---|---|
| 3 | `README.md` §6 · `mo-ta-csdl.md` §5.1 | Còn cảnh báo `spring.sql.init.mode: always` và *"CSDL bị dựng lại mỗi lần khởi động"* — đã đổi thành `never` từ Phase 2 | 🟡 |
| 4 | `mo-ta-csdl.md` §2.2 | `ma_kh` ghi dạng `KH0001` (4 số) — Phase 3A đã chuẩn hoá 6 số | 🟢 |
| 5 | `mo-ta-csdl.md` §4 | Ghi `ky_cuoc = 1 bản ghi` — thực tế 3 | 🟢 |
| 6 | `mo-ta-csdl.md` §6 | Nay cần ghi **ba** chỗ quy đổi thay vì hai, và trỏ sang `DonViCuoc` | 🟡 |
| 7 | `docs/` | Thiếu `PHASE-1-REPORT.md` (có 0, 2, 3) — nên nêu lý do trong báo cáo cuối | 🟢 |
| 8 | `data-mau.sql` | Số dư thuê bao trả trước quá thấp — xem mục 7.2 | 🟠 |

---

---

# PHẦN II — MỤC 4B: ĐỊNH GIÁ TỪNG BẢN GHI CDR

## 9. Phạm vi mục 4B

4B chỉ làm **rating** — tính `cuoc_phi` cho từng bản ghi CDR, gán `ky_cuoc_id`, chuyển
trạng thái sang `DA_TINH` hoặc `LOI`. **Chưa lập hóa đơn** (4C) và **chưa áp ưu đãi gói
cước** (4D).

⚠️ Hệ quả phải nhớ khi đọc số: mọi bản ghi ở 4B đều bị tính tiền **đầy đủ**, cờ `mien_phi`
giữ nguyên 0. Tổng cước 9.686.210 đ dưới đây là **cước gộp**, không phải doanh thu. Con số
này sẽ giảm ở 4D khi quỹ ưu đãi được trừ.

Rating chạy cho **tất cả** thuê bao kể cả trả trước, đúng quyết định 5.4: định giá và lập
hóa đơn là hai việc khác nhau. Lớp `RatingService` không hề chạm tới `thue_bao.so_du`.

---

## 10. ⚠️ Một điểm sai lệch đặc tả đã điều chỉnh

**Đặc tả 4B mục B.3 yêu cầu lấy CDR trong khoảng `[ngayBatDau 00:00:00, ngayKetThuc
23:59:59]`.** Cài đặt dùng khoảng **nửa mở** `[ngayBatDau 00:00, ngayKetThuc + 1 ngày 00:00)`.

Lý do: đây chính là điều mục 6.3 của `PHASE-4-PLAN.md` và mục 9 của phần 4A đã dặn. Cận
trên `23:59:59` bỏ sót mọi bản ghi rơi vào khoảng `23:59:59,000001` đến hết ngày.

Đo trên dữ liệu thật để biết mức độ rủi ro thực tế:

| Kiểm tra | Kết quả |
|---|---|
| Kiểu cột `thoi_gian_bat_dau` | `datetime` — **không có phần lẻ giây** |
| Bản ghi muộn nhất trong kỳ | `2026-06-30 23:59:03` |
| Số bản ghi bị mất nếu dùng cận `23:59:59` | **0** — hôm nay |

Nên hai cách viết **cho kết quả giống hệt nhau ở thời điểm này**. Điều chỉnh không phải để
sửa một lỗi đang xảy ra, mà vì khoảng cách tới ranh giới chỉ còn **56 giây**: đổi cột sang
`DATETIME(3)` — một việc hoàn toàn bình thường khi hệ thống cần độ chính xác cao hơn — là
mất bản ghi ngay, và mất **im lặng**. Không có cảnh báo nào, chỉ là hóa đơn thiếu vài dòng.

Chi phí của cách viết đúng bằng 0, nên không có lý do giữ cách viết có bẫy.

Ngoài ra, ba nhánh tính block được viết chung một dạng `soBlock(sảnLượng, gia.blockGiay)`
thay vì gán cứng `block = 1` cho SMS và DATA như đặc tả gợi ý. Với dữ liệu hiện tại
(`block_giay = 1` cho cả hai) kết quả **hoàn toàn giống nhau**; khác biệt chỉ là nếu sau
này có dòng giá "gói 10 MB" thì engine tự xử lý đúng thay vì âm thầm tính sai.

---

## 11. Thuật toán định giá

### 11.1. Mã giả cho một bản ghi

```
HÀM tinhCuoc(cdr, nguonTraCuu):

    ngayPhatSinh ← ngày của cdr.thoiGianBatDau

    # 1. Gói cước TẠI THỜI ĐIỂM PHÁT SINH, không phải gói hiện hành
    goiCuocId ← null
    VỚI MỖI đăngKý TRONG nguonTraCuu.lichSuĐăngKý[cdr.thueBaoId]:   # sắp xếp ngày bắt đầu GIẢM DẦN
        NẾU đăngKý.ngayBatDau ≤ ngayPhatSinh
           VÀ (đăngKý.ngayKetThuc = null HOẶC đăngKý.ngayKetThuc ≥ ngayPhatSinh):
            goiCuocId ← đăngKý.goiCuocId
            THOÁT VÒNG LẶP
    NẾU goiCuocId = null:
        GHI LOG WARN "lùi về gói hiện hành"          # dữ liệu thiếu, không được im lặng
        goiCuocId ← cdr.thueBao.goiCuocId

    # 2. Đơn giá theo NGÀY PHÁT SINH — chuỗi dự phòng bốn bước của 4A
    gia ← bangGia.traGia(goiCuocId, cdr.loaiDichVu, cdr.huong,
                         cdr.gioCaoDiem, ngayPhatSinh)
    NẾU không tìm được: NÉM NghiepVuException      # KHÔNG lùi về đơn giá bất kỳ

    # 3. Sản lượng thô → số block, làm tròn LÊN
    sảnLượng ← THEO cdr.loaiDichVu:
        THOAI → cdr.thoiLuongGiay
        SMS   → cdr.soLuong
        DATA  → kbSangMb(cdr.soLuong)              # CHIA 1024 TRƯỚC, nếu quên là sai 1024 lần
    soBlock ← ceil(sảnLượng / gia.blockGiay)

    # 4. Thành tiền — TẦNG LÀM TRÒN DUY NHẤT
    TRẢ VỀ lamTronTien(soBlock × gia.donGia)
```

### 11.2. Mã giả cho cả một kỳ

```
HÀM ratingKy(ky):
    NẾU ky.trangThai = DA_CHOT:    NÉM "đã chốt, không thể tính lại"
    NẾU ky.trangThai = DANG_TINH:  NÉM "đang kẹt — dùng chức năng Gỡ kỳ bị kẹt"

    ky.trangThai ← DANG_TINH  và  LƯU NGAY

    nguon ← nạp bảng giá (10 dòng) + lịch sử đăng ký gói (80 dòng)   # MỘT LẦN cho cả kỳ
    danhSách ← CDR có thoiGianBatDau ∈ [đầu kỳ, đầu kỳ sau)
                 VÀ trangThaiTinhCuoc ≠ DA_TINH
                 SẮP XẾP THEO (thoiGianBatDau, id)                   # thứ tự CỐ ĐỊNH

    VỚI MỖI cdr, chỉ số i:
        THỬ:
            cuoc ← tinhCuoc(cdr, nguon)
            thêm vào lô thành công;  cộng dồn tổng cước
        BẮT LỖI:
            thêm vào lô lỗi;  GHI LOG kèm id và lý do;  ĐI TIẾP    # không dừng cả kỳ
        NẾU lô đầy 500: ghi xuống CSDL bằng batchUpdate
        NẾU (i+1) chia hết 1000: ghi log tiến trình

    ghi nốt hai lô còn dư
    ky.soCdrXuLy ← ĐẾM LẠI TỪ CSDL số bản ghi DA_TINH của kỳ        # không lấy biến đếm
    ky.trangThai ← MO                                               # chạy xong ≠ chốt kỳ
    ghi nhật ký hệ thống
```

### 11.3. Ba quyết định cài đặt đáng ghi lại

**Bản ghi lỗi vẫn được gán `ky_cuoc_id`.** Nếu để null, chức năng hủy kết quả tính cước
(lọc theo `ky_cuoc_id`) sẽ không dọn được chúng, và chúng nằm lại ở trạng thái `LOI` vĩnh
viễn — không có đường nào đưa về `CHUA_TINH` ngoài sửa tay bằng SQL.

**`soCdrXuLy` đếm lại từ CSDL, không lấy biến đếm của lần chạy.** Engine bỏ qua bản ghi đã
`DA_TINH`, nên lần chạy thứ hai chỉ xử lý phần còn sót. Ghi thẳng biến đếm thì cột này sẽ
tụt từ 5017 xuống con số của riêng lần chạy đó — báo cáo doanh thu sau này đọc phải là sai.
Có hẳn một test cho điểm này (số 14).

**Truy vấn lọc `trangThaiTinhCuoc <> DA_TINH` thay vì `IN (CHUA_TINH, LOI)`.** Hai cách hiện
tương đương vì enum chỉ có ba giá trị, nhưng cách viết phủ định phát biểu đúng bất biến cần
giữ — *không bao giờ tính lại thứ đã tính* — nên nếu sau này thêm trạng thái mới thì nó tự
động được xử lý thay vì bị bỏ quên.

---

## 12. Kết quả chạy thật trên kỳ 6/2026

Chạy bằng bộ chạy thủ công `ChayTinhCuocKyThuCong` (nút bấm thuộc mục 4E):

```
>>> KY CUOC: 6/2026 (id=1), trang thai MO
>>> LAN 1 - tinh cuoc: thanh cong=5017  loi=0  tong cuoc=9686210.00  thoi gian=1413 ms
>>>   trang thai CDR: DA_TINH=5017  LOI=0  gan ky=5017
>>> LAN 2 - chay lai, phai khong co gi de lam: thanh cong=0  loi=0  tong cuoc=0  thoi gian=25 ms
>>> HUY KET QUA: 5017 ban ghi ve CHUA_TINH
>>> LAN 3 - tinh lai sau khi huy: thanh cong=5017  loi=0  tong cuoc=9686210.00  thoi gian=1408 ms
>>>   trang thai CDR: DA_TINH=5017  LOI=0  gan ky=5017
>>> TINH XAC DINH: Tong cuoc lan 1 = 9686210.00 | lan 3 = 9686210.00 | GIONG NHAU
```

### 12.1. Bằng chứng engine tính xác định

Ba lần chạy trên chứng minh hai tính chất khác nhau, và cần cả hai:

| Lần | Việc | Kết quả | Chứng minh điều gì |
|---|---|---|---|
| 1 | Tính cước lần đầu | 5017 bản ghi, 9.686.210 đ, 1413 ms | Engine chạy được |
| 2 | Chạy lại ngay, **không** hủy | 0 bản ghi, 25 ms | **Không tính trùng** — bản ghi `DA_TINH` bị loại khỏi truy vấn, cước đã tính không đổi |
| 3 | Hủy sạch rồi tính lại từ đầu | 5017 bản ghi, **9.686.210 đ** | **Tính xác định** — cùng đầu vào cho cùng kết quả, không phụ thuộc thứ tự trả về của CSDL |

Lần 2 và lần 3 kiểm hai chuyện dễ nhầm là một. Lần 2 có thể "đúng" chỉ vì engine không làm
gì cả. Lần 3 mới thực sự chạy lại toàn bộ phép tính và ra đúng con số cũ tới từng đồng.

Tính xác định là điều kiện **bắt buộc** cho mục 4D: khi trừ dần quỹ ưu đãi theo thứ tự thời
gian, thứ tự duyệt phải cố định, nếu không mỗi lần chạy sẽ cho hóa đơn khác nhau.

### 12.2. Cước gộp theo loại dịch vụ

| Loại dịch vụ | Số CDR | Tổng cước gộp | Tỷ trọng |
|---|---|---|---|
| THOAI | 3499 | 6.199.739 đ | 64,0% |
| SMS | 1008 | 271.846 đ | 2,8% |
| DATA | 510 | 3.214.625 đ | 33,2% |
| **Tổng** | **5017** | **9.686.210 đ** | 100% |

Điểm đáng chú ý: **DATA chiếm 33,2% cước gộp nhưng chỉ 10,2% số bản ghi.** Vì mỗi phiên data
sinh 1–500 MB với đơn giá 25 đ/MB, trong khi một cuộc gọi trung bình chỉ vài trăm đồng.

Đây cũng là chỗ mục 4D sẽ thay đổi mạnh nhất: bốn gói trong hệ thống cho 2.048–20.480 MB
miễn phí mỗi tháng, nên phần lớn 3,2 triệu đồng cước data này sẽ biến mất khi ưu đãi được
áp. Nếu sau khi làm 4D mà cước data **không** giảm đáng kể thì gần như chắc chắn engine đã
mắc đúng cái bẫy KB/MB mô tả ở `mo-ta-csdl.md` mục 6.

Cước thấp nhất 15 đ (một cuộc nội mạng dưới 6 giây), cao nhất 129.600 đ (cuộc quốc tế giờ
cao điểm 1798 giây = 30 block × 4.320 đ).

---

## 13. Kết quả nghiệm thu mục 4B

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvnw test` PASS, tối thiểu 72 test | ✅ | **84 test**, 0 lỗi (62 + **22 mới**) |
| 2 | Rating 5017 CDR dưới 10 giây | ✅ | **1413 ms** |
| 3 | Bảy kiểm chứng SQL mục F | ✅ | Bảng 13.1 |
| 4 | Bảng `hoa_don` vẫn rỗng | ✅ | `hoa_don = 0`, `chi_tiet_hoa_don = 0` |
| 5 | Đối chiếu tính tay khớp hệ thống | ✅ | Bảng 13.2 — đối chiếu **toàn bộ 5017 bản ghi**, 0 lệch |

### 13.1. Bảy kiểm chứng SQL

| # | Kiểm chứng | Kỳ vọng | Thực tế |
|---|---|---|---|
| 1 | CDR ở trạng thái `LOI` | 0 | ✅ **0** |
| 2 | `DA_TINH` mà `cuoc_phi IS NULL` | 0 | ✅ **0** |
| 3 | `DA_TINH` mà `ky_cuoc_id IS NULL` | 0 | ✅ **0** |
| 4 | Số bản ghi và tổng cước của kỳ | 5017 | ✅ **5017**, tổng **9.686.210 đ** |
| 5 | Chạy lần hai → tổng cước không đổi | không đổi | ✅ 0 bản ghi xử lý, `DA_TINH` vẫn 5017 |
| 6 | Hủy rồi tính lại → tổng cước giống hệt | giống | ✅ **9.686.210 đ** cả hai lần |
| 7 | Đối chiếu công thức tính tay | 0 lệch | ✅ **0/5017 lệch** |

Kiểm chứng số 7 làm trên **toàn bộ** 5017 bản ghi chứ không chỉ 3 mẫu như đặc tả yêu cầu —
câu SQL tự dựng lại công thức `CEIL(sản lượng / block) × đơn giá` rồi so với `cuoc_phi` đã
lưu. Ba mẫu chứng minh được ít hơn nhiều so với việc kiểm hết, mà chi phí gần như bằng nhau.

### 13.2. Đối chiếu tính tay — ba bản ghi mỗi loại dịch vụ

| id | Dịch vụ | Hướng | Cao điểm | Sản lượng | Block | Đơn giá | Số block (tay) | Cước tay | Cước hệ thống |
|---|---|---|---|---|---|---|---|---|---|
| 2 | THOAI | QUOC_TE | 0 | 133 giây | 60 | 3.600 | ⌈133/60⌉ = **3** | 10.800 | ✅ 10.800 |
| 4 | THOAI | NOI_MANG | 0 | 49 giây | 6 | 15 | ⌈49/6⌉ = **9** | 135 | ✅ 135 |
| 5 | THOAI | NOI_MANG | **1** | 40 giây | 6 | 18 | ⌈40/6⌉ = **7** | 126 | ✅ 126 |
| 1 | SMS | NOI_MANG | 0 | 1 tin | 1 | 99 | **1** | 99 | ✅ 99 |
| 3 | SMS | NGOAI_MANG | 0 | 1 tin | 1 | 250 | **1** | 250 | ✅ 250 |
| 7 | SMS | NGOAI_MANG | 0 | 1 tin | 1 | 250 | **1** | 250 | ✅ 250 |
| 20 | DATA | NOI_MANG | 0 | 261.730 KB | 1 | 25 | ⌈261730/1024⌉ = **256** | 6.400 | ✅ 6.400 |
| 24 | DATA | NOI_MANG | 0 | 269.476 KB | 1 | 25 | ⌈269476/1024⌉ = **264** | 6.600 | ✅ 6.600 |
| 30 | DATA | NOI_MANG | 0 | 27.072 KB | 1 | 25 | ⌈27072/1024⌉ = **27** | 675 | ✅ 675 |

Ba dòng đáng soi kỹ:

- **id 4**: 49 giây không chia hết cho block 6 giây → 8,17 làm tròn **lên** 9 block. Nếu cắt
  phần thập phân thì ra 120 đ thay vì 135 đ.
- **id 5**: cùng hướng nội mạng với id 4 nhưng đơn giá 18 đ chứ không phải 15 đ — engine đã
  lấy đúng dòng giờ cao điểm.
- **id 20**: 261.730 KB = 255,6 MB → làm tròn lên **256 MB**. Nếu quên chia 1024 thì cước
  bản ghi này là 6.543.250 đ thay vì 6.400 đ — gấp hơn **1000 lần**, và một mình nó chiếm
  hai phần ba tổng cước của cả 5017 bản ghi.

### 13.3. Số test theo lớp

| Lớp test | Số test | Ghi chú |
|---|---|---|
| `SchemaValidationTest` | 1 | |
| `ThueBaoServiceTest` | 17 | |
| `BangGiaCuocServiceTest` | 9 | |
| `SinhMaServiceTest` | 3 | |
| `DonViCuocTest` | 13 | 4A |
| `BangGiaLookupTest` | 9 | 4A |
| `QuyTacToHopDichVuTest` | 8 | 4A |
| `KiemTraDoPhuBangGiaTest` | 2 | 4A |
| **`RatingServiceTest`** | **22** | **4B** |
| | **84** | |

22 test mới chia bốn nhóm: quy sản lượng về block (7), chọn gói cước tại thời điểm phát
sinh (4), chạy cả kỳ (5), hủy kết quả và gỡ kỳ bị kẹt (6).

Test số 10 kiểm cả **log WARN** của đường lùi gói cước, không chỉ kiểm giá trị trả về. Lý do:
đường lùi đó vẫn cho ra một con số hợp lệ, nên nếu không có cảnh báo thì dữ liệu thiếu bản
ghi `dang_ky_goi_cuoc` sẽ không bao giờ lộ ra.

---

## 14. Đường gỡ khi kỳ kẹt ở trạng thái Đang tính

`ratingKy` đặt kỳ sang `DANG_TINH` trước khi chạy và chỉ trả về `MO` khi xong. Nếu tiến
trình bị giết giữa chừng — mất điện, kill process, hết bộ nhớ — kỳ nằm lại `DANG_TINH` vĩnh
viễn, và chính bước kiểm tra đầu `ratingKy` sẽ từ chối mọi lần chạy sau. Không có đường gỡ
thì cách duy nhất là sửa tay bằng lệnh UPDATE trong CSDL.

`RatingService.goKyBiKet(kyId)` chỉ đổi trạng thái kỳ, **không đụng tới CDR**: bản ghi đã
kịp tính giữ nguyên `DA_TINH`, chạy lại sẽ xử lý nốt phần còn sót. Muốn làm sạch hẳn thì gọi
tiếp `huyRatingKy`. Mọi lần gỡ đều ghi `nhat_ky_he_thong` với hành động `GO_KY_BI_KET`.

Chức năng này **có 3 test** (số 20, 21, 22) dù nó chỉ chạy khi có sự cố — đúng vì đó là loại
mã mà nếu không kiểm thử thì lúc cần đến mới phát hiện nó hỏng, và lúc đó hệ thống đang
trong tình trạng tệ sẵn rồi.

---

## 15. Cái bẫy DevTools đã được chặn hẳn

Mục 4A phát hiện: chạy `mvnw test` trong lúc ứng dụng đang bật profile `reset` sẽ biên dịch
lại `target/`, DevTools thấy file đổi nên tự khởi động lại, và `schema.sql` chạy lại —
`DROP TABLE`, xóa sạch dữ liệu vừa sinh. Ở 4A phải nhớ dừng ứng dụng thủ công trước.

4B chặn hẳn bằng cách thêm vào `application-reset.yml`:

```yaml
spring:
  devtools:
    restart:
      enabled: false
```

Chỉ tắt trong profile `reset` — profile này vốn chỉ dùng để nạp lại dữ liệu mẫu, không phải
để ngồi sửa code. Chạy thường vẫn có DevTools như cũ.

Đây là ví dụ nhỏ của cùng một bài học ở mục 3: biến một việc **phải nhớ** thành một việc
**không thể quên**.

---

---

# PHẦN III — MỤC 4C: GOM CDR THÀNH HÓA ĐƠN

## 16. Phạm vi mục 4C

4C dựng bộ khung hóa đơn và chứng minh **bất biến cộng dồn**, để 4D chỉ còn việc trừ ưu đãi
trên một nền đã đúng. **Chưa áp ưu đãi gói cước.**

Chỉ lập hóa đơn cho thuê bao **trả sau** (quyết định 5.4). Thuê bao trả trước đã có
`cuoc_phi` trên từng CDR nhưng không có hóa đơn tháng — cước của họ trừ thẳng vào số dư,
thuộc Phase 5.

---

## 17. Ba điểm điều chỉnh so với đặc tả

### 17.1. Một công thức prorate thay cho phân nhánh theo trạng thái

Đặc tả liệt kê quy tắc theo từng trạng thái thuê bao. Cài đặt dùng **một công thức khoảng
ngày** phủ hết mọi trường hợp, cho kết quả giống hệt:

```
tuNgay  = max(ngayKichHoat, đầu kỳ)
denNgay = min(ngayHuy nếu có, cuối kỳ)
nếu tuNgay > denNgay  →  không lập hóa đơn
soNgaySuDung = denNgay − tuNgay + 1        # tính CẢ hai đầu
```

| Trường hợp | Công thức cho ra |
|---|---|
| Kích hoạt trước kỳ, chưa hủy | Trọn kỳ, thu đủ cước tháng |
| Kích hoạt giữa kỳ | Từ ngày kích hoạt tới cuối kỳ |
| Hủy giữa kỳ | Từ đầu kỳ tới ngày hủy |
| Hủy **trước** kỳ | Khoảng rỗng → không lập hóa đơn |
| Kích hoạt **sau** kỳ | Khoảng rỗng → không lập hóa đơn |

Hai dòng cuối là trường hợp đặc tả không nhắc tới. Phân nhánh theo trạng thái thì phải nhớ
viết thêm hai nhánh; công thức khoảng ngày xử lý sẵn mà không cần nghĩ tới.

Thêm một ca đặc tả không nêu: thuê bao **`DA_THANH_LY` nhưng `ngay_huy` để trống**. Đây là
dữ liệu hỏng. Cài đặt **không đoán mò** — bỏ qua và ghi log WARN. Nếu đoán "chưa hủy" thì
sẽ thu đủ cước tháng của một thuê bao đã thanh lý, sai tiền mà không có dấu hiệu gì. Có test
riêng (số 11).

### 17.2. Hạn thanh toán: hai cách phát biểu trùng nhau

Đặc tả ghi *"hanThanhToan = ngày 15 tháng kế tiếp"*, còn hằng số từ 4A ghi *"số ngày từ
ngày lập tới hạn thanh toán = 15"*. Hai cách này **cho kết quả giống hệt nhau** vì ngày lập
luôn là ngày cuối tháng:

| Ngày lập | + 15 ngày | Ngày 15 tháng sau |
|---|---|---|
| 30/06 | 15/07 | 15/07 |
| 31/07 | 15/08 | 15/08 |
| 28/02 | 15/03 | 15/03 |
| 29/02 | 15/03 | 15/03 |

Đã đổi tên hằng số thành `NGAY_HAN_THANH_TOAN_THANG_SAU` theo cách phát biểu của đặc tả, vì
nó không phụ thuộc giả định "ngày lập luôn cuối tháng".

### 17.3. ⚠️ Dòng cước sử dụng KHÔNG có đơn giá — và vì sao không nên có

Đặc tả yêu cầu *"chi_tiet_hoa_don cho mọi khoản mục > 0, SNAPSHOT đơn giá tại thời điểm
lập"*. Cài đặt tạo 4 loại dòng, nhưng **chỉ dòng cước thuê bao có đơn giá**; ba dòng cước sử
dụng để trống cột `don_gia`.

Lý do không phải là lười:

- Mỗi dòng cước sử dụng là **tổng của nhiều bản ghi áp đơn giá khác nhau** — nội mạng 15 đ,
  ngoại mạng 25 đ, quốc tế 3.600 đ, cộng thêm bậc giờ cao điểm. Không tồn tại một đơn giá
  duy nhất để ghi.
- Điền đơn giá bình quân suy ngược từ thành tiền sẽ là **con số không có trên bảng giá nào**.
- Tách chi tiết tới từng bậc giá thì phải **suy ngược lại bảng giá ở khâu lập hóa đơn** —
  và đó là sai nguyên tắc: bảng giá có thể đã đổi giữa lúc định giá và lúc lập hóa đơn, khi
  đó đơn giá in ra không phải đơn giá đã thu. Đúng loại sai lệch âm thầm mà cả Phase 4 đang
  phòng.

**Muốn có snapshot thật tới từng bậc giá thì phải lưu `bang_gia_cuoc_id` trên từng bản ghi
CDR ngay lúc định giá.** Hiện chưa có cột đó. Đã ghi vào phần hạn chế (mục 21.2) như một đề
xuất thay đổi schema, không tự ý thêm ở 4C.

Một điểm nữa về đặc tả B.1 (*"kỳ chưa rating xong → cảnh báo rõ"*): cài đặt hiểu theo hai
mức khác nhau — bản ghi `CHUA_TINH` thì **chặn cứng** (hóa đơn sẽ thiếu tiền), bản ghi `LOI`
thì **chỉ ghi log WARN**. Chặn cứng cả `LOI` sẽ khiến một bản ghi hỏng không sửa được làm cả
kỳ không bao giờ lập được hóa đơn.

---

## 18. Thuật toán lập hóa đơn

```
HÀM tinhCuocThueBao(thueBao, ky, nguon):

    # BƯỚC 0 — chặn trùng ở tầng nghiệp vụ, TRƯỚC khi để UNIQUE của CSDL bắt
    NẾU đã có hóa đơn (thueBao, ky):  TRẢ VỀ null

    # BƯỚC 1 — cước thuê bao tháng, prorate theo số ngày thực dùng
    khoang ← tinhKhoangSuDung(thueBao, ky)        # công thức ở mục 17.1
    NẾU khoang = null:  TRẢ VỀ null
    goi ← gói hiệu lực tại NGÀY CUỐI KỲ qua dang_ky_goi_cuoc
          (lùi về thue_bao.goi_cuoc_id kèm log WARN nếu thiếu)
    cuocThueBao ← NẾU trọn kỳ THÌ goi.cuocThueBaoThang
                  NGƯỢC LẠI lamTronTien(goi.cuocThueBaoThang × soNgày / soNgàyTrongKỳ)

    # BƯỚC 2 — gom cước từ CDR đã DA_TINH. CHỈ CỘNG DỒN, KHÔNG LÀM TRÒN LẠI.
    cuocThoai, cuocSms, cuocData ← tra từ ảnh chụp tổng hợp theo (thuê bao, dịch vụ)

    # BƯỚC 3 — giảm trừ (bảng còn rỗng, đường xử lý viết sẵn)
    truocGiamTru ← cuocThueBao + cuocThoai + cuocSms + cuocData + cuocKhac
    giamTru ← Σ (số tiền tuyệt đối, hoặc tỷ lệ % nếu không khai số tiền)

    # BƯỚC 4 — tổng hợp
    tongTruocThue ← max(0, truocGiamTru − giamTru)
    thueVat       ← lamTronTien(tongTruocThue × 10%)
    tongThanhToan ← tongTruocThue + thueVat

    # BƯỚC 5 — sinh hóa đơn
    maHoaDon      ← "HD" + yyyyMM + "-" + số thứ tự 6 chữ số, đếm riêng theo kỳ
    ngayLap       ← ngày cuối kỳ
    hanThanhToan  ← ngày 15 tháng kế tiếp
    trangThai ← CHUA_TT, daThanhToan ← 0, conNo ← tongThanhToan
    thêm dòng chi tiết cho mọi khoản mục khác 0
```

Ba quyết định cài đặt đáng ghi lại:

**Ảnh chụp tra cứu nạp một lần cho cả kỳ.** Lịch sử đăng ký gói, tổng cước gom theo
(thuê bao, dịch vụ) và danh sách giảm trừ đều nạp một lần. Riêng phần tổng cước là **một
truy vấn `GROUP BY` duy nhất** thay vì ba truy vấn cho mỗi thuê bao — với 58 thuê bao đó là
174 truy vấn tiết kiệm được.

**`soHoaDonTao` và `tongDoanhThu` đếm lại từ CSDL**, không lấy biến đếm của lần chạy — cùng
lý do như `soCdrXuLy` ở 4B.

**Giảm trừ: khai cả số tiền lẫn tỷ lệ thì số tiền tuyệt đối thắng.** Cộng cả hai lại sẽ giảm
trừ gấp đôi ý định người nhập. Bảng `giam_tru` hiện rỗng nên quy tắc này chưa phát huy, nhưng
phải chốt trước khi có dữ liệu.

---

## 19. Kết quả chạy thật trên kỳ 6/2026

```
>>> KY CUOC: 6/2026 (id=1), trang thai MO
>>> LAN 1 - lap hoa don: tao moi=58  bo qua da co=0  bo qua khong du dieu kien=2
                         doanh thu ky=28692784.00  thoi gian=722 ms
>>> LAN 2 - chay lai:    tao moi=0   bo qua da co=58 bo qua khong du dieu kien=2
                         doanh thu ky=28692784.00  thoi gian=148 ms
>>> HUY LAP HOA DON: 58 hoa don da xoa, con lai 0
>>> LAN 3 - lap lai sau khi huy: tao moi=58  doanh thu ky=28692784.00  thoi gian=354 ms
>>> TINH XAC DINH: Doanh thu lan 1 = 28692784.00 | lan 3 = 28692784.00 | GIONG NHAU
```

**58 hóa đơn, 2 thuê bao bị bỏ qua** — đúng hai thuê bao trả sau đã thanh lý với ngày hủy
20/05/2026 và 30/04/2026, tức trước kỳ 6/2026.

Ba lần chạy chứng minh cùng bộ tính chất như ở 4B: chạy được, không tạo trùng, và lập lại
sau khi hủy ra đúng con số cũ tới từng đồng.

### 19.1. Đối chiếu prorate — tính tay so với hệ thống

| Số thuê bao | Gói | Cước tháng | Kích hoạt | Số ngày | Tính tay | Hệ thống |
|---|---|---|---|---|---|---|
| 0967901278 | DN500 | 500.000 | 05/06 | 26 | 500000 × 26/30 = **433.333** | ✅ 433.333 |
| 0834567834 | MAX70 | 70.000 | 11/06 | 20 | 70000 × 20/30 = **46.667** | ✅ 46.667 |
| 0978012379 | MAX150 | 150.000 | 15/06 | 16 | 150000 × 16/30 = **80.000** | ✅ 80.000 |
| 0845678935 | MAX150 | 150.000 | 17/06 | 14 | 150000 × 14/30 = **70.000** | ✅ 70.000 |
| 0819123480 | MAX150 | 150.000 | 23/06 | 8 | 150000 × 8/30 = **40.000** | ✅ 40.000 |

Khớp đúng bảng giá trị kỳ vọng đã tính sẵn ở `PHASE-4-PLAN.md` mục 5.6, không sửa lại con số
nào. Số ngày tính **bao gồm cả ngày kích hoạt**: thuê bao vào mạng ngày 23 dùng 8 ngày
(23→30), không phải 7.

### 19.2. Ba hóa đơn mẫu

| Mã hóa đơn | Thuê bao | Thuê bao tháng | Thoại | SMS | Data | Trước thuế | VAT | Thanh toán |
|---|---|---|---|---|---|---|---|---|
| HD202606-000001 | 0821234521 | 50.000 | 29.180 | 2.886 | 143.750 | 225.816 | 22.582 | 248.398 |
| HD202606-000002 | 0832345622 | 70.000 | 43.446 | 1.146 | 36.150 | 150.742 | 15.074 | 165.816 |
| HD202606-000003 | 0843456723 | 150.000 | 58.549 | 7.844 | 44.525 | 260.918 | 26.092 | 287.010 |

Ngày lập 30/06/2026, hạn thanh toán 15/07/2026, trạng thái `CHUA_TT`, còn nợ bằng tổng thanh
toán.

---

## 20. Kết quả nghiệm thu mục 4C

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvnw test` PASS, tối thiểu 96 test | ✅ | **110 test**, 0 lỗi (84 + **26 mới**) |
| 2 | Billing kỳ 6/2026 dưới 10 giây | ✅ | **722 ms** |
| 3 | Chín kiểm chứng SQL mục E | ✅ | Bảng 20.1 |
| 4 | 5 thuê bao prorate khớp bảng kỳ vọng | ✅ | Bảng 19.1 — khớp cả 5 |
| 5 | Ghi đủ bảng số liệu mốc | ✅ | Mục 20.2 |

### 20.1. Chín kiểm chứng SQL

| # | Kiểm chứng | Kỳ vọng | Thực tế |
|---|---|---|---|
| 1 | ⭐ `SUM(cuoc_phi)` theo dịch vụ vs cột trên hóa đơn | lệch 0 đ | ✅ **0,00 đ** cả ba dịch vụ |
| 1b | CDR của thuê bao trả sau không có hóa đơn | 0 | ✅ **0** |
| 1c | Đối chiếu **từng hóa đơn một** | 0 dòng lệch | ✅ **0/58** |
| 2 | `tong_truoc_thue` = tổng các khoản − giảm trừ | 0 dòng lệch | ✅ **0** |
| 3 | `thue_vat = ROUND(tong_truoc_thue × 0,10)` | 0 dòng lệch | ✅ **0** |
| 4 | `tong_thanh_toan`, `con_no` | 0 dòng lệch | ✅ **0** |
| 5 | `SUM(chi_tiet.thanh_tien)` = `tong_truoc_thue` | 0 dòng lệch | ✅ **0** |
| 5b | Từng khoản mục chi tiết khớp cột tương ứng | 0 dòng lệch | ✅ **0** |
| 6 | Số hóa đơn theo loại thuê bao | 58 trả sau, **0 trả trước** | ✅ đúng |
| 6b | Thuê bao đủ điều kiện vs số hóa đơn | bằng nhau | ✅ **58 = 58** |
| 7 | `SUM(tong_thanh_toan)` = `ky_cuoc.tong_doanh_thu` | bằng nhau | ✅ 28.692.784 |
| 8 | Chạy lần hai → số hóa đơn không đổi | không đổi | ✅ 0 tạo mới, 58 bỏ qua |
| 9 | Hủy → lập lại → doanh thu giống hệt | giống | ✅ 28.692.784 cả hai lần |

Kiểm chứng số 1b tuy nhỏ nhưng **không thể thiếu**: nếu có thuê bao trả sau phát sinh CDR mà
không được lập hóa đơn thì tổng ở kiểm chứng 1 vẫn có thể tình cờ khớp trong khi hóa đơn
đang thiếu. Và kiểm chứng 1c đối chiếu **từng hóa đơn một** chứ không chỉ tổng — hai sai lệch
ngược dấu ở hai hóa đơn khác nhau sẽ triệt tiêu nhau ở mức tổng.

> **Một chi tiết về VAT đáng ghi lại.** Tổng VAT của 58 hóa đơn là 2.608.437 đ, còn
> `ROUND(tổng trước thuế × 10%)` là 2.608.435 đ — **lệch 2 đồng**. Đây **không phải lỗi**:
> VAT tính trên từng hóa đơn rồi mới cộng, đúng như hóa đơn thật phải làm. Nó cho thấy tại
> sao bất biến phải kiểm **trên từng dòng**, không kiểm trên số tổng — kiểm ở mức tổng thì
> câu SQL này sẽ báo lệch và người đọc tưởng engine sai.

### 20.2. 📌 Số liệu mốc TRƯỚC KHI áp ưu đãi — để 4D đối chiếu

| Chỉ tiêu | Giá trị |
|---|---|
| **Tổng doanh thu kỳ 6/2026** | **28.692.784 đ** |
| Tổng trước thuế | 26.084.347 đ |
| — Cước thuê bao | 18.400.000 đ |
| — Cước thoại | 4.863.778 đ |
| — Cước SMS | 214.169 đ |
| — **Cước data** | **2.606.400 đ** |
| **Số hóa đơn có `cuoc_data > 0`** | **50 / 58** |
| Số hóa đơn | 58 |
| Số dòng chi tiết hóa đơn | 208 |

**Dự đoán cho 4D**, đo trên đúng 58 thuê bao đang có hóa đơn (không phải con số 46 của
`PHASE-4-PLAN.md` mục 4.2 — con số đó đo trước khi mục 4A sinh lại dữ liệu):

| Loại ưu đãi | Số thuê bao **còn trong** ưu đãi | Sau 4D phải có cước tương ứng = 0 |
|---|---|---|
| Data | **45** / 50 thuê bao có phát sinh data | ✅ |
| Phút nội mạng | **45** | ✅ |
| Phút ngoại mạng | **39** | ✅ |
| SMS | **47** | ✅ |

Suy ra **số hóa đơn có `cuoc_data > 0` phải tụt từ 50 xuống đúng 5**, gồm 3 thuê bao gói
CB01 (ưu đãi 0 MB) và 2 thuê bao gói MAX70 đã dùng quá 2.048 MB. Chi tiết theo gói:

| Gói | Ưu đãi | Số thuê bao có data | Còn trong ưu đãi | Vẫn phải trả |
|---|---|---|---|---|
| CB01 | 0 MB | 3 | 0 | **3** |
| MAX70 | 2.048 MB | 4 | 2 | **2** |
| MAX150 | 5.120 MB | 14 | 14 | 0 |
| DN500 | 20.480 MB | 29 | 29 | 0 |

Con số 5 này là dự đoán **chính xác chứ không phải ước lượng**, vì quyết định 5.3 không cắt
đôi bản ghi: thuê bao có tổng sản lượng nằm trong ưu đãi thì **mọi** bản ghi của họ đều miễn
phí. Nếu cước data **không** giảm thì engine đã mắc bẫy KB/MB mô tả ở `mo-ta-csdl.md` mục 6.

Cước thuê bao 18.400.000 đ thì **không được đổi** ở 4D: ưu đãi không prorate và cũng không
tác động tới cước cố định hàng tháng.

### 20.3. Số test theo lớp

| Lớp test | Số test | Ghi chú |
|---|---|---|
| `SchemaValidationTest` · `ThueBaoServiceTest` · `BangGiaCuocServiceTest` · `SinhMaServiceTest` | 30 | Phase 2–3 |
| `DonViCuocTest` · `BangGiaLookupTest` · `QuyTacToHopDichVuTest` · `KiemTraDoPhuBangGiaTest` | 32 | 4A |
| `RatingServiceTest` | 22 | 4B |
| **`BillingServiceTest`** | **26** | **4C** |
| | **110** | |

26 test mới chia bốn nhóm: prorate (7), quy tắc thuê bao được lập hóa đơn (7), tổng hợp tiền
và bất biến cộng dồn (5), chạy cả kỳ và hủy (7).

Test số 17 là test đáng chú ý nhất: nó nạp vào các con số **lẻ tới hàng xu** (1000,49 đ /
250,51 đ / 99,99 đ) rồi khẳng định hóa đơn giữ nguyên từng xu. Mục 4B luôn sinh số nguyên
đồng nên trên dữ liệu thật sẽ không bao giờ thấy khác biệt — nhưng nếu tầng này lỡ thêm một
lần làm tròn thì test đỏ ngay, trong khi kiểm chứng SQL trên dữ liệu thật vẫn xanh.

---

## 21. Hạn chế bổ sung sau 4C

### 21.1. Đổi gói giữa kỳ: cước thuê bao tính theo gói cuối kỳ

Cước thuê bao tháng lấy theo gói có hiệu lực tại **ngày cuối kỳ**, không chia đôi kỳ theo
từng gói (quyết định 5.10). Cước **sử dụng** thì không bị xấp xỉ này — mục 4B đã tra giá theo
đúng gói tại thời điểm từng bản ghi CDR.

Hiện chưa lộ ra vì dữ liệu mẫu không có thuê bao nào đổi gói giữa kỳ.

### 21.2. Đề xuất thay đổi schema: lưu `bang_gia_cuoc_id` trên CDR

Như phân tích ở mục 17.3, hóa đơn không thể in đơn giá cho các dòng cước sử dụng vì bảng giá
đã áp không được lưu lại. Thêm cột `bang_gia_cuoc_id` vào `chi_tiet_su_dung` khi định giá sẽ
mở ra:

- Chi tiết hóa đơn tách theo từng bậc giá, có đơn giá snapshot thật
- Truy nguyên được vì sao một bản ghi ra đúng số tiền đó
- Kiểm chứng lại cước cũ ngay cả sau khi bảng giá đã đổi

Không tự ý thêm ở 4C vì đây là thay đổi schema, cần cân nhắc cùng lúc với 4D.

### 21.3. Hai đường hủy tách bạch

`RatingService.huyRatingKy` xóa `cuoc_phi` của CDR; `BillingService.huyBillingKy` xóa hóa
đơn. Tách bạch để lập lại hóa đơn không bắt buộc phải định giá lại từ đầu. Hệ quả cần nhớ:
**hủy rating trong khi kỳ còn hóa đơn sẽ bị từ chối**, phải hủy hóa đơn trước.

---

## 22. Việc tiếp theo — mục 4D

Áp ưu đãi gói cước: trừ dần quỹ ưu đãi theo thứ tự thời gian, đánh cờ `mien_phi` trên các
bản ghi nằm trong ưu đãi, rồi lập lại hóa đơn.

Bốn điều 4D phải giữ:

1. **Ba chỗ quy đổi đơn vị** — dùng `DonViCuoc`, tuyệt đối không viết lại phép chia 1024
   hay 60 ở chỗ khác
2. **Không cắt đôi bản ghi CDR** (quyết định 5.3): mỗi bản ghi hoặc hoàn toàn trong ưu đãi
   (`mien_phi = 1`, `cuoc_phi = 0`) hoặc hoàn toàn tính tiền. Duyệt theo thứ tự thời gian cố
   định — tính xác định đã được chứng minh ở 4B chính là điều kiện cho việc này
3. **Ưu đãi không prorate** theo ngày kích hoạt (quyết định 5.6)
4. Sau khi áp ưu đãi phải **hủy và lập lại hóa đơn**, rồi đối chiếu với bảng số liệu mốc ở
   mục 20.2

> ⚠️ Phép thử quyết định của 4D: **45 thuê bao còn trong ưu đãi data phải có `cuoc_data = 0`**,
> và số hóa đơn có `cuoc_data > 0` phải tụt từ 50 xuống **đúng 5** (bảng ở mục 20.2). Câu SQL
> kiểm chứng có sẵn ở `PHASE-4-PLAN.md` mục 9.2.
