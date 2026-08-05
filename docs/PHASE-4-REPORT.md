# BÁO CÁO PHASE 4 — ENGINE TÍNH CƯỚC

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Trạng thái:** ✅ **Hoàn thành** — mục 4A đến 4F, 148 test PASS, hai kỳ cước đã tính xong

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

## 22. Kế hoạch mục 4D (viết trước khi làm)

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

---

# PHẦN IV — MỤC 4D: ÁP ƯU ĐÃI GÓI CƯỚC

## 23. ⚠️ Mâu thuẫn đặc tả về quy đổi đơn vị — điểm quan trọng nhất của 4D

Đặc tả 4D mục B đưa ra **hai chỉ dẫn không tương thích** trong cùng một khối:

> *"CDR THOAI: `thoi_luong_giay` (GIÂY) → `DonViCuoc.giaySangPhut()`"*
>
> *"⚠️ Ưu đãi trừ theo SẢN LƯỢNG THÔ (quyết định 5.2), không theo block đã làm tròn."*

`giaySangPhut()` làm tròn **lên**. Áp nó cho từng bản ghi rồi cộng dồn chính là một dạng làm
tròn — mâu thuẫn với dòng ngay bên dưới.

### 23.1. Đo trước, quyết sau

Trước khi viết dòng code nào, đã đo cả hai cách trên dữ liệu thật:

| Loại ưu đãi | Cách A — quy từng CDR lên đơn vị quỹ | Cách B — so ở đơn vị nhỏ nhất | Chênh |
|---|---|---|---|
| Data | 45 thuê bao còn trong ưu đãi | 45 | — |
| **Phút nội mạng** | **43** | **45** | **2 thuê bao** |
| **Phút ngoại mạng** | **37** | **39** | **2 thuê bao** |
| SMS | 47 | 47 | — |

Mức "lạm phát" của cách A: **+0,18%** với data, nhưng **+10,97%** với thoại. Chênh lệch lớn
vì cuộc gọi trung bình chỉ khoảng 3 phút — làm tròn lên nửa phút mỗi cuộc, cộng qua vài chục
cuộc là mất cả chục phút quỹ.

Trường hợp cụ thể nhất:

| Thuê bao | Gói | Quỹ | Đã dùng thật | Cách A quy ra | Kết luận của cách A |
|---|---|---|---|---|---|
| 0832345622 | MAX70 | 100 phút | 5.351 giây = **89,2 phút** | **105 phút** | ❌ Vượt quỹ |
| 0834567834 | MAX70 | 100 phút | 5.978 giây = **99,6 phút** | **109 phút** | ❌ Vượt quỹ |

Cả hai thuê bao này **chưa hề vượt quỹ**, nhưng cách A vẫn thu tiền của họ. Đây đúng loại
hỏng mà cả Phase 4 đang phòng: hóa đơn vẫn phát hành bình thường, không lỗi, không cảnh báo,
chỉ sai tiền — và sai theo hướng bất lợi cho khách hàng.

### 23.2. Cách đã chọn: so ở đơn vị nhỏ nhất

Cài đặt **quy quỹ xuống** đơn vị của bản ghi thay vì quy bản ghi lên đơn vị của quỹ:

```
quyNoiMang   = goi.phutNoiMangMienPhi × 60      → GIÂY
quyNgoaiMang = goi.phutNgoaiMangMienPhi × 60    → GIÂY
quySms       = goi.smsMienPhi                   → TIN (không quy đổi)
quyData      = goi.dataMienPhiMb × 1024         → KB
```

Ba lý do:

1. **Không có phép làm tròn nào** trong toàn bộ phép so, nên không có gì để tranh cãi.
2. Đúng nguyên văn dòng thứ hai của đặc tả và đúng lý do đã ghi ở quyết định 5.2:
   *"ưu đãi là ưu đãi của khách, không nên bị hao thêm vì quy tắc làm tròn của nhà mạng"*.
3. Ý đồ của dòng thứ nhất — nhìn ghi chú `[sai → lệch 60 lần]` — là **đừng quên quy đổi**,
   chứ không phải *phải làm tròn lên*. So ở đơn vị nhỏ nhất đạt đúng ý đồ đó, và đạt tốt hơn.

Đã bổ sung `DonViCuoc.phutSangGiay()` và `DonViCuoc.mbSangKb()` — hai phép **nhân đúng tuyệt
đối**, để chiều quy đổi này cũng nằm trong cùng một lớp chứ không rải ra ngoài.

> Nếu muốn đổi sang cách A thì chỉ cần sửa bốn dòng khởi tạo quỹ trong `UuDaiGoiCuoc`. Toàn
> bộ phần còn lại không phụ thuộc lựa chọn này.

### 23.3. Đặc tả còn một chỗ nữa cần đính chính

Đặc tả D.8 yêu cầu test *"đảo thứ tự đầu vào → kết quả không đổi"*. **Điều đó không đúng với
thuật toán đã chốt**, và không thể đúng: quy tắc không cắt đôi bản ghi (quyết định 5.3) làm
kết quả **phụ thuộc thứ tự** một cách cố hữu.

Ví dụ, quỹ 60 giây với ba cuộc 30 / 40 / 20 giây:

| Thứ tự | Diễn biến | Kết quả |
|---|---|---|
| 30 → 40 → 20 | 30 lọt (còn 30) · 40 vượt, quỹ đóng · 20 bị thu | miễn phí 1 cuộc |
| 20 → 40 → 30 | 20 lọt (còn 40) · 40 lọt (còn 0) · 30 vượt | miễn phí 2 cuộc |

Vì vậy test được viết lại thành **hai** test khác nhau, và cả hai đều có giá trị hơn:

- *Tính xác định*: cùng danh sách chạy hai lần cho kết quả giống hệt
- *Phụ thuộc thứ tự*: hai thứ tự khác nhau cho hai kết quả khác nhau — chính là lý do truy
  vấn lấy CDR **bắt buộc** phải có `ORDER BY` cố định. Nếu để CSDL tự chọn thứ tự thì cùng
  một dữ liệu có thể cho ra hai hóa đơn khác nhau ở hai lần chạy.

---

## 24. Bước A — lưu `bang_gia_cuoc_id` lên CDR

Chấp thuận đề xuất mục 21.2. Thêm cột `bang_gia_cuoc_id BIGINT NULL`, FK →
`bang_gia_cuoc(id)`. `RatingService` gán cột này cùng lúc gán `cuoc_phi`.

Áp lên CSDL bằng `ALTER TABLE` chứ **không** dùng profile `reset`: reset sẽ sinh lại CDR
ngẫu nhiên và làm mất khả năng đối chiếu. Sau đó chạy lại trọn vòng — hủy hóa đơn → hủy định
giá → định giá → lập hóa đơn:

| Chỉ tiêu | Trước bước A | Sau bước A | |
|---|---|---|---|
| Tổng cước gộp | 9.686.210 đ | **9.686.210 đ** | ✅ không đổi |
| Doanh thu | 28.692.784 đ | **28.692.784 đ** | ✅ không đổi |
| Số hóa đơn | 58 | 58 | ✅ |
| CDR `DA_TINH` thiếu snapshot | — | **0** | ✅ |

Hai con số đầu là phép thử quan trọng nhất của bước này: nếu lệch thì thay đổi đã vô tình
động vào logic tính cước chứ không chỉ thêm một cột.

### 24.1. Cột này trả giá ngay ở bước B

Bước áp ưu đãi phải quyết định mỗi bản ghi được miễn phí hay bị thu tiền, và nếu bị thu thì
thu bao nhiêu. Nếu chỉ **giữ nguyên** `cuoc_phi` cũ thì bước này **không chạy lại được**:
lần chạy trước đã đặt `cuoc_phi = 0` cho các bản ghi miễn phí, nên lần sau chúng mang giá 0
vĩnh viễn dù quỹ đã đổi.

Nhờ có snapshot, bước áp ưu đãi **tính lại cước đầy đủ** từ đúng dòng bảng giá đã áp dụng, và
trở thành **bất biến theo số lần chạy**. Công thức dùng chung với engine định giá qua
`RatingService.tinhTienTheoBangGia()` — không viết lại ở hai nơi.

---

## 25. Thuật toán quỹ ưu đãi

```
KHỞI TẠO UuDaiGoiCuoc(goi):
    quyNoiMang   ← Quy(goi.phutNoiMangMienPhi   × 60)     # GIÂY
    quyNgoaiMang ← Quy(goi.phutNgoaiMangMienPhi × 60)     # GIÂY
    quySms       ← Quy(goi.smsMienPhi)                    # TIN
    quyData      ← Quy(goi.dataMienPhiMb × 1024)          # KB
    # Quỹ bằng 0 thì ĐÓNG ngay từ đầu (gói CB01)

HÀM namTrongUuDai(cdr):
    NẾU cdr.huong = QUOC_TE:  TRẢ VỀ false        # quốc tế không có ưu đãi, không đụng quỹ
    quy, luong ← THEO cdr.loaiDichVu:
        THOAI → (quy theo hướng,  cdr.thoiLuongGiay)      # GIÂY, sản lượng THÔ
        SMS   → (quySms,          cdr.soLuong)            # TIN
        DATA  → (quyData,         cdr.soLuong)            # KB, sản lượng THÔ
    TRẢ VỀ quy.thu(luong)

HÀM Quy.thu(luong):
    NẾU daDong HOẶC luong > conLai:
        daDong ← true            # quỹ ĐÓNG: mọi bản ghi sau đều thu tiền
        TRẢ VỀ false
    conLai ← conLai − luong
    TRẢ VỀ true

ÁP CHO CẢ KỲ:
    danhSách ← CDR đã DA_TINH của kỳ, ORDER BY (thuê bao, thời gian, id)
    VỚI MỖI thuê bao (danh sách đã gom sẵn theo thuê bao):
        quy ← UuDaiGoiCuoc(gói hiệu lực tại NGÀY CUỐI KỲ)
        VỚI MỖI cdr:
            cuocDayDu ← tinhTienTheoBangGia(cdr, bảng giá đã lưu ở bang_gia_cuoc_id)
            NẾU quy.namTrongUuDai(cdr):  ghi mien_phi=1, cuoc_phi=0
            NGƯỢC LẠI:                   ghi mien_phi=0, cuoc_phi=cuocDayDu
```

### 25.1. Vì sao quỹ phải "đóng" chứ không chỉ "không trừ"

Nếu quỹ chỉ đơn giản không bị trừ khi bản ghi quá lớn, thì một bản ghi **ngắn** phát sinh sau
đó vẫn lọt vào phần dư. Khách hàng sẽ thấy cuộc gọi dài mất tiền, còn cuộc ngắn ngay sau đó
lại miễn phí — không giải thích nổi, và kết quả phụ thuộc vào việc bản ghi nào tình cờ nhỏ
hơn phần dư. Cờ `daDong` biến quy tắc thành: *ưu đãi dùng hết là hết*.

### 25.2. Bốn quỹ độc lập

Cạn quỹ data không ảnh hưởng quỹ SMS hay thoại; quỹ nội mạng và ngoại mạng cũng tách riêng.
Có test cho từng cặp.

---

## 26. ⭐ Kiểm chứng dự đoán đã công bố ở 4C

Báo cáo 4C công bố dự đoán **trước khi viết dòng code 4D nào**. Kết quả:

| # | Dự đoán (viết ở 4C) | Kỳ vọng | Thực tế | |
|---|---|---|---|---|
| 1 | Số hóa đơn có `cuoc_data > 0` | tụt 50 → **đúng 5** | **5** | ✅ |
| 2 | 5 hóa đơn đó gồm những ai | 3 gói CB01 + 2 gói MAX70 | **đúng 3 CB01 + 2 MAX70** | ✅ |
| 3 | 45 thuê bao còn trong ưu đãi data có `cuoc_data = 0` | 0 vi phạm | **0** | ✅ |
| 4 | 45 thuê bao còn ưu đãi phút nội mạng, cước = 0 | 0 vi phạm | **0** | ✅ |
| 5 | 39 ngoại mạng, 47 SMS | 0 vi phạm | **0** | ✅ |
| 6 | Tổng `cuoc_data` giảm mạnh | — | **−87,4%** | ✅ |

Năm hóa đơn còn phải trả cước data, đúng như dự đoán:

| Mã hóa đơn | Thuê bao | Gói | Ưu đãi | Đã dùng | Cước data |
|---|---|---|---|---|---|
| HD202606-000001 | 0821234521 | CB01 | 0 MB | 5.740 MB | 143.750 đ |
| HD202606-000007 | 0917890127 | CB01 | 0 MB | 2.516 MB | 62.975 đ |
| HD202606-000010 | 0960123430 | CB01 | 0 MB | 3.009 MB | 75.400 đ |
| HD202606-000005 | 0885678925 | MAX70 | 2.048 MB | 2.628 MB | 18.500 đ |
| HD202606-000011 | 0971234531 | MAX70 | 2.048 MB | 3.038 MB | 27.725 đ |

### 26.1. Một sự cố: câu SQL kiểm chứng sai, không phải engine sai

Lần chạy kiểm chứng đầu tiên báo **25 thuê bao vi phạm** ở phép kiểm ưu đãi SMS. Theo đúng
quy tắc "nếu số không khớp thì dừng lại phân tích trước khi sửa", đã truy nguyên trước khi
động vào code.

Nguyên nhân nằm ở **câu SQL kiểm chứng**, không nằm ở engine: nó cộng cả **SMS quốc tế** vào
phép so với quỹ. Mà tin nhắn quốc tế **không có ưu đãi** — đó là quy tắc đã chốt và engine
làm đúng. Một thuê bao nhắn 20 tin trong nước (nằm gọn trong quỹ 30 tin) cộng 2 tin quốc tế
sẽ có `SUM(cuoc_phi) ≠ 0` một cách hoàn toàn hợp lệ.

Loại `QUOC_TE` ra khỏi phép kiểm thì kết quả là **0 vi phạm** cho cả bốn loại ưu đãi.

Ghi lại vì đây là một biến thể đáng nhớ của bài học mục 3: **một phép kiểm sai cũng nguy hiểm
như thiếu phép kiểm** — nó tạo ra báo động giả, và nếu tin nó thì sẽ đi "sửa" một engine đang
chạy đúng.

---

## 27. Bảng so sánh trước / sau khi áp ưu đãi

| Chỉ tiêu | Trước ưu đãi (4C) | Sau ưu đãi (4D) | Chênh lệch |
|---|---|---|---|
| **Doanh thu** | 28.692.784 đ | **23.940.596 đ** | **−4.752.188 đ (−16,6%)** |
| Cước thuê bao | 18.400.000 đ | 18.400.000 đ | **0 đ** ✅ không đổi |
| Cước thoại | 4.863.778 đ | 2.939.561 đ | −1.924.217 đ (−39,6%) |
| Cước SMS | 214.169 đ | 96.267 đ | −117.902 đ (−55,0%) |
| **Cước data** | 2.606.400 đ | **328.350 đ** | **−2.278.050 đ (−87,4%)** |
| Hóa đơn có cước data | 50 | **5** | −45 |

Cước thuê bao **không đổi một đồng** — đúng như phải thế: ưu đãi không tác động tới cước cố
định hàng tháng.

Tỷ lệ bản ghi được miễn phí:

| Dịch vụ | Tổng bản ghi | Miễn phí | Tỷ lệ |
|---|---|---|---|
| THOAI | 3.499 | 2.292 | 65,5% |
| SMS | 1.008 | 712 | 70,6% |
| DATA | 510 | 353 | 69,2% |
| **Tổng** | **5.017** | **3.357** | **66,9%** |

### 27.1. Cước còn lại đến từ đâu

| Dịch vụ | Hướng | Bản ghi | Miễn phí | Cước còn lại |
|---|---|---|---|---|
| THOAI | NOI_MANG | 1.937 | 1.390 | 363.867 đ |
| THOAI | NGOAI_MANG | 1.391 | 902 | 485.895 đ |
| **THOAI** | **QUOC_TE** | 171 | **0** | **3.425.760 đ** |
| SMS | NOI_MANG | 554 | 398 | 15.444 đ |
| SMS | NGOAI_MANG | 408 | 314 | 23.500 đ |
| **SMS** | **QUOC_TE** | 46 | **0** | **115.000 đ** |
| DATA | NOI_MANG | 510 | 353 | 936.575 đ |

Điểm đáng chú ý: **171 cuộc gọi quốc tế — chỉ 3,4% số bản ghi — sinh ra 3,4 triệu đồng**, gần
bằng toàn bộ cước còn lại của mọi dịch vụ khác cộng lại. Vì quốc tế không có ưu đãi và đơn
giá cao gấp 144 lần nội mạng (3.600 đ/phút so với 15 đ/6 giây). Con số này khiến bảng cước
sau ưu đãi trông rất khác bảng trước ưu đãi, và giải thích vì sao cước thoại chỉ giảm 39,6%
trong khi 65,5% số cuộc gọi được miễn phí.

*(Bảng này tính trên toàn bộ thuê bao; các cột trên hóa đơn chỉ gồm thuê bao trả sau.)*

---

## 28. Kết quả nghiệm thu mục 4D

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvnw test` PASS, tối thiểu 125 test | ✅ | **130 test**, 0 lỗi (111 + **19 mới**) |
| 2 | Bước A: mọi CDR `DA_TINH` có snapshot, cước gộp vẫn 9.686.210 đ | ✅ | Bảng mục 24 |
| 3 | Dự đoán số 1 ra **đúng 5** | ✅ | Bảng mục 26 |
| 4 | Mười kiểm chứng SQL | ✅ | Bảng 28.1 |
| 5 | Bất biến cộng dồn vẫn lệch 0,00 đ | ✅ | Kiểm chứng 9 |
| 6 | Bảng so sánh trước/sau | ✅ | Mục 27 |

### 28.1. Mười kiểm chứng SQL

| # | Kiểm chứng | Kỳ vọng | Thực tế |
|---|---|---|---|
| 1 | Hóa đơn có `cuoc_data > 0` | 5 | ✅ **5** |
| 2 | Thành phần 5 hóa đơn đó | 3 CB01 + 2 MAX70 | ✅ đúng |
| 3 | Thuê bao còn ưu đãi data mà vẫn bị thu | 0 | ✅ **0** |
| 4 | Thuê bao còn ưu đãi phút NM / NgM mà vẫn bị thu | 0 | ✅ **0 / 0** |
| 5 | Thuê bao còn ưu đãi SMS trong nước mà vẫn bị thu | 0 | ✅ **0** |
| 6 | Tổng cước data | giảm mạnh | ✅ **−87,4%** |
| 7 | `mien_phi = 1` mà `cuoc_phi ≠ 0` | 0 | ✅ **0** |
| 8 | Tổng sản lượng miễn phí vượt quỹ (cả 3 loại) | 0 | ✅ **0 / 0 / 0** |
| 8b | Bản ghi quốc tế được miễn phí | 0 | ✅ **0** |
| 9 | Bất biến cộng dồn cả 3 dịch vụ | lệch 0 đ | ✅ **0,00 đ**; từng hóa đơn: **0/58 lệch** |
| 9c | Các bất biến tiền tệ khác của 4C | 0 dòng lệch | ✅ **0** |
| 10 | Hủy → chạy lại → doanh thu giống hệt | giống | ✅ 23.940.596 đ |

### 28.2. Số test theo lớp

| Lớp test | Số test | Ghi chú |
|---|---|---|
| Phase 2–3 | 30 | |
| 4A (`DonViCuoc`, `BangGiaLookup`, `QuyTacToHopDichVu`, `KiemTraDoPhuBangGia`) | 33 | +1 test snapshot bảng giá |
| `RatingServiceTest` | 22 | 4B |
| `BillingServiceTest` | 29 | 4C + **3 test áp ưu đãi** |
| **`UuDaiGoiCuocTest`** | **16** | **4D** |
| | **130** | |

16 test của `UuDaiGoiCuoc` chia ba nhóm: bẫy quy đổi đơn vị (7), quy tắc trừ quỹ (7), tính
xác định và phụ thuộc thứ tự (2). Hai test đầu nhóm 1 chính là hai cái bẫy 1024 lần và 60 lần
mà `mo-ta-csdl.md` mục 6 cảnh báo.

---

## 29. Hạn chế bổ sung sau 4D

### 29.1. Quỹ ưu đãi lấy theo gói cuối kỳ

Giống cước thuê bao (mục 21.1): nếu khách đổi gói giữa kỳ thì quỹ ưu đãi lấy theo gói có hiệu
lực tại **ngày cuối kỳ**, không chia quỹ theo tỷ lệ giữa hai gói. Cước **sử dụng** thì vẫn tra
giá theo đúng gói tại thời điểm từng bản ghi (mục 4B) — hai chỗ này dùng hai mốc khác nhau
một cách có chủ đích, nhưng cần nêu rõ trong báo cáo cuối kỳ.

### 29.2. Bản ghi làm vượt ngưỡng bị thu tiền toàn bộ

Quyết định 5.3 không cắt đôi bản ghi. Hệ quả có thể thấy trên hóa đơn: một phiên data 500 MB
phát sinh khi quỹ chỉ còn 400 MB sẽ bị thu tiền cho **cả** 500 MB, và 400 MB còn lại của quỹ
coi như mất. Đây là xấp xỉ có chủ đích, bắt nguồn từ việc `chi_tiet_su_dung` chỉ có một cột
`cuoc_phi` và một cờ `mien_phi`.

Muốn chính xác tuyệt đối thì phải tách bản ghi thành hai dòng, hoặc thêm cột "sản lượng trong
ưu đãi" — cả hai đều là thay đổi schema, nên để lại làm hướng phát triển.

### 29.3. Bước áp ưu đãi nằm trong `BillingService`

Về mặt khái niệm, quỹ ưu đãi là chuyện của **kỳ cước** chứ không của từng bản ghi, nên đặt ở
tầng lập hóa đơn là hợp lý. Nhưng nó **ghi vào `chi_tiet_su_dung`** — tức tầng lập hóa đơn
đang sửa dữ liệu của tầng định giá. Hiện chấp nhận được vì hai tầng luôn chạy nối tiếp nhau và
bước này bất biến theo số lần chạy. Nếu về sau tách thành hai tiến trình chạy độc lập thì nên
tách `UuDaiService` riêng.

---

## 30. Kế hoạch mục 4E (viết trước khi làm)

Dựng giao diện: màn hình chạy tính cước theo kỳ, lập hóa đơn, chốt kỳ, và các đường hủy/gỡ đã
có sẵn ở tầng nghiệp vụ.

Bốn việc 4E phải làm:

1. Nút **Tính cước kỳ** và **Lập hóa đơn kỳ**, có xác nhận trước khi chạy và hiển thị kết quả
   (số bản ghi, số hóa đơn, thời gian, tổng cước)
2. Nút **Hủy kết quả tính cước** / **Hủy lập hóa đơn** / **Gỡ kỳ bị kẹt**, mỗi nút một modal
   cảnh báo riêng — đặc biệt nút gỡ kỳ kẹt phải nói rõ nó chỉ dùng khi có sự cố
3. **Chốt kỳ** (`MO` → `DA_CHOT`), thao tác một chiều, phải cảnh báo rõ
4. Xóa ba lớp bộ chạy thủ công `ChayTinhCuocKyThuCong`, `ChayLapHoaDonKyThuCong`,
   `ChayLaiToanBoKyThuCong` khi giao diện đã thay được chúng

> Cũng nên chốt kỳ 5/2026 ở 4E — nợ này ghi từ `PHASE-3-REPORT.md` mục 8.2, đến giờ mới có
> chức năng chốt kỳ để xử lý.

---

# PHẦN V — MỤC 4E: GIAO DIỆN ĐIỀU KHIỂN VÀ BẢNG ĐỐI SOÁT

## 31. Phạm vi mục 4E

Đưa engine ra giao diện. Trước 4E, engine chỉ chạy được qua ba lớp bộ chạy thủ công gọi từ
dòng lệnh; sau 4E, cả ba lớp đó **đã bị xoá** và mọi thao tác đều qua nút bấm.

Hai màn hình mới:

| Đường dẫn | Vai trò |
|---|---|
| `/tinh-cuoc` | Điều khiển: chạy tính cước, lập hóa đơn, hủy, gỡ kỳ kẹt, chốt kỳ |
| `/tinh-cuoc/doi-soat/{thueBaoId}/{kyId}` | **Bảng đối soát cước** — màn hình chứng minh engine đúng |
| `/tinh-cuoc/ky/{id}` | Danh sách hóa đơn của một kỳ, cửa vào bảng đối soát |

---

## 32. Một điểm đặc tả phải bổ sung: trạng thái nút

Đặc tả A.2 mô tả nút theo trạng thái kỳ, nhưng cột `ky_cuoc.trang_thai` chỉ có **ba giá
trị** (`MO` / `DANG_TINH` / `DA_CHOT`) — không đủ để phân biệt "kỳ mở chưa tính cước" với
"kỳ mở đã tính xong chờ lập hóa đơn" với "kỳ mở đã có hóa đơn". Cả ba đều là `MO`.

Phải đếm thêm mới suy ra được, nên bổ sung DTO `TinhTrangKy` gom năm con số: bản ghi chưa
tính, bản ghi lỗi, bản ghi đã tính, số hóa đơn, số giao dịch thanh toán. Từ đó dẫn xuất ra
sáu điều kiện bật nút:

| Nút | Điều kiện |
|---|---|
| Chạy tính cước | kỳ `MO` và còn bản ghi `CHUA_TINH` hoặc `LOI` |
| Lập hóa đơn | kỳ `MO`, **không còn** bản ghi `CHUA_TINH`, đã có bản ghi `DA_TINH`, chưa có hóa đơn |
| Huỷ hóa đơn | kỳ `MO`, có hóa đơn, **chưa** có giao dịch thanh toán nào |
| Huỷ tính cước | kỳ `MO`, **không** còn hóa đơn, có bản ghi `DA_TINH` |
| Chốt kỳ | kỳ `MO`, đã có hóa đơn |
| Gỡ kỳ kẹt | kỳ đang `DANG_TINH` |

Kỳ `DA_CHOT` không hiện nút nào, chỉ còn dòng chữ *"Chỉ xem"*.

> ⚠️ Giao diện chỉ **ẩn bớt nút cho gọn mắt**. Chốt chặn thật vẫn nằm nguyên ở tầng nghiệp
> vụ — gõ thẳng đường dẫn vào một thao tác bị cấm vẫn bị từ chối với đúng thông báo tiếng
> Việt. Có test riêng cho điều này (mục 34).

Mục 4E cũng bổ sung `KyCuocService.chotKy()` — chức năng chưa tồn tại. Nó yêu cầu kỳ **đã có
hóa đơn**: chốt một kỳ trống sẽ khóa vĩnh viễn một kỳ rỗng, và **cố ý không có chức năng
mở lại** vì chốt kỳ là thao tác một chiều theo thiết kế.

---

## 33. Bảng đối soát cước — màn hình quan trọng nhất

### 33.1. Nguyên tắc: chỉ đọc lại, không tính lại

`DoiSoatCuocService` **không tính lại bất cứ con số tiền nào**. Mọi giá trị lấy thẳng từ dữ
liệu đã ghi: `cuoc_phi` và `mien_phi` do engine đặt, đơn giá lấy từ dòng bảng giá đã chụp ở
`bang_gia_cuoc_id`.

Đây là điều kiện để bảng đối soát có giá trị chứng minh. Nếu nó tự tính lại theo cách riêng
thì nó không chứng minh được hóa đơn đúng — nó chỉ chứng minh chính nó.

Riêng khoảng ngày sử dụng và gói cước hiệu lực thì **dùng chung** `QuyTacKyCuoc` với engine
lập hóa đơn, đúng vì lý do trên: nếu đối soát tự tính khác đi thì nó sẽ có lúc mâu thuẫn với
chính hóa đơn mà nó đang đối soát.

### 33.2. Bốn khối

**Khối 1** — thuê bao, khách hàng, gói cước và bốn mức ưu đãi. Nếu cước thuê bao bị prorate
thì ghi thẳng phép tính ra màn hình, ví dụ `150.000 × 8 ÷ 30 = 40.000 đ`, kèm câu nhắc ưu
đãi **không** bị chia theo ngày.

**Khối 2** — bảng sản lượng và ưu đãi, năm dòng: thoại nội mạng, thoại ngoại mạng, tin nhắn
trong nước, dữ liệu, và một dòng riêng cho quốc tế ghi rõ *"không áp dụng ưu đãi"*.

Cột "Đã dùng" hiện **hai con số**: đơn vị người đọc hiểu và giá trị gốc trong ngoặc. Cột
quota cũng vậy. Ví dụ thật lấy từ màn hình:

| Loại | Đã dùng | Quota của gói | Được miễn phí | Vượt ưu đãi | Thành tiền |
|---|---|---|---|---|---|
| Thoại nội mạng<br>*Quota 6.000 giây* | **89,2 phút**<br>*(5.351 giây)* | 100 phút | 89,2 phút | 0 phút | **0 đ** |
| Dữ liệu<br>*Quota 2.097.152 KB* | **1.441,9 MB**<br>*(1.476.505 KB)* | 2.048 MB | 1.441,9 MB | 0 MB | **0 đ** |

Cặp số trong ngoặc là **bằng chứng trực quan** rằng hệ thống không nhầm giây với phút hay KB
với MB. Người chấm nhìn `5.351 giây` cạnh `Quota 6.000 giây` là tự kiểm được ngay.

Dòng đầu bảng trên còn là ví dụ đắt nhất của cả Phase 4: đây đúng thuê bao `0832345622` đã
phân tích ở mục 23. Cách quy đổi bị loại bỏ sẽ cộng dồn làm tròn từng cuộc thành **105 phút**
và kết luận vượt quota 100 phút; cách đã chọn so `5.351 ≤ 6.000` giây và cho **0 đ**. Chênh
lệch đó hiện ra ngay trên màn hình, không cần đọc code.

**Khối 3** — chi tiết từng bản ghi: thời gian, số bị gọi, dịch vụ, hướng, cờ cao điểm, sản
lượng, số block, **đơn giá**, cước, và badge *Miễn phí*. Dòng DATA hiện cả hai đơn vị ngay
trong ô sản lượng, ví dụ `435.754 KB = 425,5 MB`.

Bản ghi **làm vượt ưu đãi** — bản ghi đầu tiên bị tính tiền của mỗi loại — được tô nền **và**
thêm vạch màu ở mép trái, kèm chú thích giải thích quy tắc không cắt đôi bản ghi. Dùng cả
nền lẫn vạch chứ không chỉ màu, để bảng in đen trắng vẫn phân biệt được.

Mặc định hiện 50 dòng, có nút **Xem tất cả** để chụp ảnh; khi in ra giấy thì CSS tự bỏ giới
hạn này.

**Khối 4** — đối chiếu với hóa đơn. Ví dụ thật:

| Khoản mục | Tính từ chi tiết sử dụng | Trên hóa đơn | Chênh lệch |
|---|---|---|---|
| Cước thuê bao tháng | 70.000 đ | 70.000 đ | **0 đ** |
| Cước thoại | 27.930 đ | 27.930 đ | **0 đ** |
| Cước tin nhắn | 0 đ | 0 đ | **0 đ** |
| Cước dữ liệu | 0 đ | 0 đ | **0 đ** |
| Tổng trước thuế | 97.930 đ | 97.930 đ | **0 đ** |
| Thuế VAT 10% | 9.793 đ | 9.793 đ | **0 đ** |
| Tổng thanh toán | 107.723 đ | 107.723 đ | **0 đ** |

Đây là **bất biến cộng dồn hiển thị ngay trên giao diện** — thứ mà từ 4C tới giờ chỉ chứng
minh được bằng câu SQL.

### 33.3. In ra giấy A4

Khối `@media print` (15 quy tắc) bỏ sidebar, thanh trên, chân trang và mọi nút; hiện tiêu đề
riêng cho bản in; **bỏ giới hạn 50 dòng**; lặp lại dòng tiêu đề bảng ở mỗi trang
(`thead { display: table-header-group }`); và đổi badge sang dạng viền đen thay vì nền màu để
bản in đen trắng vẫn đọc được.

---

## 34. Kết quả nghiệm thu mục 4E

| # | Tiêu chí | Kết quả | Bằng chứng |
|---|---|---|---|
| 1 | `mvnw test` PASS, tối thiểu 136 test | ✅ | **148 test**, 0 lỗi (130 + **18 mới**) |
| 2 | Chạy trọn vòng qua giao diện, doanh thu ra đúng | ✅ | **23.940.596 đ** — mục 34.1 |
| 3 | Bảng đối soát, chênh lệch khối 4 toàn 0 | ✅ | **58/58 hóa đơn** — mục 34.2 |
| 4 | Ba lớp bộ chạy thủ công đã xoá | ✅ | Engine chỉ chạy qua nút bấm |
| 5 | In ra A4 đọc được | ✅ | 15 quy tắc `@media print` nạp đúng |

### 34.1. Chạy trọn một vòng qua giao diện

Không dùng SQL, chỉ bấm nút:

| Bước | Thao tác | Kết quả |
|---|---|---|
| 1 | Huỷ hóa đơn | 58 hóa đơn bị xoá |
| 2 | Huỷ kết quả tính cước | 5017 bản ghi về `CHUA_TINH` |
| 3 | Chạy tính cước | 5017 bản ghi định giá lại |
| 4 | Lập hóa đơn | 58 hóa đơn, **doanh thu 23.940.596 đ** |

Doanh thu ra **đúng con số của 4D tới từng đồng** sau khi xoá sạch và làm lại toàn bộ bằng
giao diện. Đây là lần thứ ba tính xác định được chứng minh, và là lần đầu qua đúng con đường
người dùng thật sẽ đi.

Sau đó **chốt kỳ**: mọi nút thao tác biến mất, chỉ còn dòng *"Chỉ xem"*; gọi lại thao tác
tính cước trên kỳ đã chốt bị từ chối với thông báo tiếng Việt.

### 34.2. Bảng đối soát — quét toàn bộ, không lấy mẫu

Đặc tả yêu cầu kiểm 3 thuê bao đại diện. Giữ chuẩn "kiểm hết thay vì lấy mẫu" đã lập từ 4B,
đã mở **cả 58 bảng đối soát**:

| Kết quả khối 4 | Số hóa đơn |
|---|---|
| **Khớp tuyệt đối** (chênh lệch toàn 0) | **58** |
| Có chênh lệch | **0** |

Ba thuê bao đại diện theo đúng đặc tả:

| Vai trò | Thuê bao | Số liệu đáng chú ý | Khối 4 |
|---|---|---|---|
| **Trọn trong ưu đãi** | 0827890157 (DN500) | **70/70 bản ghi miễn phí**; 100,3 phút nội mạng / quota 2.000 phút | ✅ Khớp |
| **Vượt ưu đãi data** | 0821234521 (CB01) | **5.739,7 MB (5.877.492 KB)** → cước data **143.750 đ**; 0/74 miễn phí | ✅ Khớp |
| **Prorate** | 0834567834 (MAX70) | **20/30 ngày**; 99,6 phút (5.978 giây) / quota 100 phút → **0 đ** | ✅ Khớp |

Thuê bao trả trước (ví dụ 0901234501) mở bảng đối soát vẫn ra trang bình thường với khung
thông báo giải thích vì sao không có hóa đơn tháng — không phải lỗi 500.

Hai dòng đáng soi kỹ nhất cho báo cáo:

- **0821234521**: `5.739,7 MB (5.877.492 KB)` — quy đổi KB→MB hiện ngay trên màn hình, và
  gói CB01 có quota 0 MB nên toàn bộ bị thu tiền.
- **0834567834**: `99,6 phút (5.978 giây)` với `Quota 100 phút / Quota 6.000 giây` → **0 đ**.
  Đây là thuê bao sát ranh giới nhất; cách quy đổi bị loại bỏ ở mục 23 sẽ tính ra 109 phút và
  thu tiền oan.

### 34.3. Số test theo lớp

| Lớp test | Số test | Ghi chú |
|---|---|---|
| Phase 2–3 | 30 | |
| 4A · 4B · 4C · 4D | 100 | |
| **`TinhCuocControllerTest`** | **9** | **4E** — phân quyền và xử lý lỗi ở tầng HTTP |
| **`DoiSoatCuocServiceTest`** | **9** | **4E** — nội dung bảng đối soát |
| | **148** | |

Test đáng chú ý nhất là `DoiSoatCuocServiceTest` số 1: nó dựng **hai bản ghi cùng dịch vụ,
cùng hướng, cùng khung giờ nhưng mang hai dòng bảng giá khác nhau** — đúng tình huống bảng
giá đổi giữa kỳ — rồi khẳng định mỗi bản ghi hiện đơn giá của chính nó. Nếu bảng đối soát tra
lại bảng giá hiện hành thì cả hai sẽ hiện cùng một giá và test đỏ. Đây là test chứng minh
snapshot của mục 4D thật sự hoạt động.

### 34.4. Hai sự cố nhỏ khi làm

**Test dùng `@WithMockUser` không vẽ được trang.** Layout đọc
`#authentication.principal.hoTen` và `.vaiTro.nhan`, mà người dùng giả mặc định của Spring
Security không có hai thuộc tính đó. Đã đổi sang dựng `NguoiDungPrincipal` thật — vừa sửa
được lỗi, vừa đúng với cách hệ thống chạy thật hơn.

**Test đầu tiên cho trang "chưa có hóa đơn" dựng thuê bao thiếu khách hàng và gói cước.**
Trang đổ vì `tenKh` null. Nhưng dữ liệu đó **không giống thật** — engine luôn nạp thuê bao
đầy đủ quan hệ. Đã sửa dữ liệu test cho đúng tình huống thật (thuê bao đầy đủ, chỉ thiếu hóa
đơn) thay vì thêm kiểm tra null vào template cho một trạng thái không bao giờ xảy ra.

---

## 35. Trạng thái dữ liệu hiện tại và lưu ý khi chụp ảnh

Kỳ 6/2026 hiện: **5017 CDR đã tính, 58 hóa đơn, doanh thu 23.940.596 đ, trạng thái `MO`**.

> ⚠️ **Kỳ đã được chốt trong lúc kiểm chứng rồi trả lại `MO` bằng lệnh SQL.** Chức năng chốt
> kỳ là **một chiều theo thiết kế** và cố ý không có nút mở lại, nên nếu để nguyên trạng thái
> `DA_CHOT` thì sẽ không chụp được ảnh các nút thao tác. Việc chốt kỳ đã được kiểm chứng đầy
> đủ (mục 34.1); sau khi chụp xong ảnh, chỉ cần bấm **Chốt kỳ** một lần là về lại trạng thái
> cuối cùng.

### 35.1. Màn hình nên chụp cho báo cáo

Đăng nhập bằng `admin`. Thứ tự ưu tiên từ trên xuống.

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 1 | ⭐ **Đối soát — thuê bao vượt ưu đãi data** | `/tinh-cuoc/doi-soat/21/1` | **Ảnh quan trọng nhất.** Khối 2 hiện `5.739,7 MB (5.877.492 KB)`; khối 3 có dòng DATA `435.754 KB = 425,5 MB` và badge Miễn phí; khối 4 chênh lệch toàn 0 |
| 2 | ⭐ **Đối soát — thuê bao sát ranh giới quota phút** | `/tinh-cuoc/doi-soat/34/1` | `99,6 phút (5.978 giây)` cạnh `Quota 100 phút / 6.000 giây` → **0 đ**. Phóng to đúng dòng này |
| 3 | ⭐ **Khối 4 — đối chiếu hóa đơn** | Cuộn xuống cuối ảnh 1 hoặc 2 | Cột chênh lệch **toàn 0 đ** và khung xanh "Khớp tuyệt đối" |
| 4 | Đối soát — thuê bao trọn trong ưu đãi | `/tinh-cuoc/doi-soat/57/1` | 70/70 bản ghi Miễn phí, mọi dòng khối 2 đều 0 đ |
| 5 | Đối soát — dòng làm vượt ưu đãi | Ảnh 1, phần khối 3 | Dòng tô nền kèm vạch mép trái và chú thích quy tắc không cắt đôi bản ghi |
| 6 | Đối soát — prorate | Ảnh 2, khối 1 | Khung ghi `20/30 ngày` và phép tính cước thuê bao |
| 7 | **Bản in A4** | Mở ảnh 1 rồi bấm **In** → xem trước | Không còn sidebar và nút; hiện đủ mọi dòng; tiêu đề riêng cho bản in |
| 8 | Màn hình điều khiển | `/tinh-cuoc` | Ba kỳ, cột "Bước tiếp theo", các nút bật theo trạng thái |
| 9 | Modal cảnh báo chốt kỳ | Bấm **Chốt kỳ**, không xác nhận | Câu cảnh báo "MỘT CHIỀU, không có đường quay lại" |
| 10 | Modal cảnh báo gỡ kỳ kẹt | Cần kỳ đang `DANG_TINH` | Câu "CHỈ dùng khi lần chạy trước bị dừng đột ngột" |
| 11 | Hộp kết quả sau khi chạy | Bấm **Huỷ hóa đơn** rồi **Lập hóa đơn** | Khung xanh "Hoàn thành" kèm số hóa đơn, doanh thu, thời gian |
| 12 | Kỳ đã chốt | Bấm **Chốt kỳ** và xác nhận | Badge "Đã chốt", ngày chốt, dòng "Chỉ xem", không còn nút nào |
| 13 | Danh sách hóa đơn của kỳ | `/tinh-cuoc/ky/1` | 58 hóa đơn; đúng **5 dòng** có cột Dữ liệu tô đỏ đậm |
| 14 | Trang đối soát của thuê bao trả trước | `/tinh-cuoc/doi-soat/1/1` | Khung vàng giải thích vì sao không có hóa đơn tháng |
| 15 | Chặn phân quyền | Đăng nhập `ketoan01`, gõ `/tinh-cuoc` | Trang 403 |
| 16 | Đường vào từ chi tiết thuê bao | `/thue-bao/22` | Nút "Xem đối soát cước" đổ ra danh sách kỳ |
| 17 | Kết quả 148 test | Console `mvnw test` | Dòng `Results: Tests run: 148, Failures: 0` |

> Ảnh 1 và 2 là hai ảnh nên đưa vào phần trình bày chính. Chúng cho thấy cùng lúc: quy đổi
> đơn vị đúng, quỹ ưu đãi trừ đúng, đơn giá lấy từ ảnh chụp bảng giá, và hóa đơn khớp tuyệt
> đối với chi tiết sử dụng — tức toàn bộ những gì Phase 4 phải chứng minh, trên một màn hình.

---

## 36. Kế hoạch mục 4F (viết trước khi làm)

Còn hai việc để khép Phase 4:

1. **Chốt kỳ 5/2026** — nợ ghi từ `PHASE-3-REPORT.md` mục 8.2, nay đã có chức năng để xử lý.
   Kỳ 5/2026 không có CDR nên sẽ không lập được hóa đơn; cần quyết định xử lý kỳ trống thế
   nào (chốt kỳ hiện yêu cầu phải có hóa đơn)
2. **Nợ tài liệu còn tồn** ở mục 8 — cảnh báo `spring.sql.init.mode` đã lỗi thời trong
   `README.md` và `mo-ta-csdl.md`, `ma_kh` ghi 4 chữ số, số kỳ cước mẫu, và mục 6 của
   `mo-ta-csdl.md` cần ghi **ba** chỗ quy đổi thay vì hai
3. Nâng số dư mẫu của 16 thuê bao trả trước (mục 7.2) trước khi Phase 5 trừ số dư

---

# PHẦN VI — MỤC 4F: KHÉP PHASE 4

## 37. ⭐ Dự đoán kỳ 5/2026 — công bố TRƯỚC khi chạy

Đo điều kiện trên dữ liệu hiện có, trước khi sinh một bản ghi CDR nào cho kỳ 5.

### 37.1. Số hóa đơn: dự đoán **54**

| Nhóm | Số thuê bao | Vào kỳ 5? |
|---|---|---|
| Tổng thuê bao trả sau | 60 | |
| — Kích hoạt **sau** 31/05/2026 (chưa tồn tại ở kỳ 5) | 5 | ❌ |
| — Đã thanh lý, huỷ **trước** kỳ 5 (30/04/2026) | 1 | ❌ |
| — Đã thanh lý, huỷ **trong** kỳ 5 (20/05/2026) | 1 | ✅ có hóa đơn, prorate |
| **Dự đoán số hóa đơn kỳ 5** | **54** | |

Ít hơn 58 của kỳ 6 đúng 4 hóa đơn: mất 5 thuê bao kích hoạt tháng 6, được lại 1 thuê bao
thanh lý ngày 20/05 mà ở kỳ 6 đã bị loại.

### 37.2. Prorate: dự đoán **đúng 1 thuê bao** — nhưng không phải lý do đặc tả nêu

Đặc tả 4F mục A.3 dự kiến *"không thuê bao nào bị prorate ở kỳ 5 do kích hoạt giữa kỳ"*.
Kiểm dữ liệu: **đúng, 0 thuê bao kích hoạt trong tháng 5/2026**.

Nhưng prorate còn một nguyên nhân thứ hai mà đặc tả không nhắc: **huỷ giữa kỳ**. Thuê bao
`0823456733` (id 33, gói CB01) huỷ ngày 20/05/2026, tức dùng 20/31 ngày của kỳ 5.

| Thuê bao | Gói | Cước tháng | Ngày huỷ | Số ngày | Cước prorate dự đoán |
|---|---|---|---|---|---|
| 0823456733 | CB01 | 50.000 đ | 20/05/2026 | 20/31 | **32.258 đ** |

Đây là lần đầu nhánh "huỷ giữa kỳ" của công thức khoảng ngày (mục 17.1) được kiểm trên dữ
liệu thật — ở kỳ 6 không có thuê bao nào rơi vào nhánh này.

### 37.3. Tổng cước thuê bao: dự đoán **17.762.258 đ**

```
53 thuê bao thu đủ cước tháng           17.730.000 đ
+ thuê bao 0823456733 prorate 20/31 ngày    32.258 đ
= tổng dự đoán                          17.762.258 đ
```

Thấp hơn 18.400.000 đ của kỳ 6 đúng **637.742 đ**.

### 37.4. Bộ sinh CDR: dự đoán bỏ qua **5 thuê bao**

Có 65 thuê bao `HOAT_DONG`, trong đó 60 kích hoạt trước 31/05/2026. Bộ sinh cố ý không sinh
bản ghi trước ngày kích hoạt, nên 5 thuê bao kích hoạt tháng 6 phải bị bỏ qua — kết quả sinh
phải báo **"5 thuê bao bỏ qua"**.

> Nếu bất kỳ con số nào ở trên lệch so với thực tế: **dừng lại phân tích trước khi sửa**.
> Kết quả đối chiếu ở mục 38.

---

## 38. Kết quả kỳ 5/2026 — đối chiếu dự đoán

Sinh 4000 bản ghi cho 01/05–31/05/2026, rồi chạy tính cước → lập hóa đơn → chốt kỳ, **toàn
bộ qua giao diện**.

| # | Dự đoán (mục 37) | Kỳ vọng | Thực tế | |
|---|---|---|---|---|
| 1 | Số hóa đơn | **54** | **54** | ✅ |
| 2 | Số thuê bao bị prorate | **1** | **1** | ✅ |
| 3 | Cước prorate của `0823456733` | **32.258 đ** | **32.258 đ** | ✅ |
| 4 | Tổng cước thuê bao | **17.762.258 đ** | **17.762.258 đ** | ✅ |
| 5 | Thuê bao bị bỏ qua khi sinh CDR | **5** | **5** (60/65 có phát sinh) | ✅ |

**Cả năm dự đoán đúng, bốn trong số đó đúng tới từng đồng.**

Ba con số phụ đáng ghi: sinh ra **3697** bản ghi trên 4000 lượt yêu cầu — phần chênh là các
lượt rơi vào 5 thuê bao kích hoạt tháng 6, bị bộ sinh bỏ qua đúng như thiết kế. Tính cước
3697 bản ghi mất **1261 ms**, lập 54 hóa đơn mất **2596 ms**. Bất biến cộng dồn ở kỳ 5 cũng
**lệch 0 đồng** trên cả 54 hóa đơn.

### 38.1. Một nhánh code lần đầu được chạy trên dữ liệu thật

Đặc tả 4F dự kiến *"không thuê bao nào bị prorate ở kỳ 5 do kích hoạt giữa kỳ"* — đúng, 0
thuê bao kích hoạt trong tháng 5. Nhưng prorate còn **nguyên nhân thứ hai** mà đặc tả không
nhắc: **huỷ giữa kỳ**.

Thuê bao `0823456733` thanh lý ngày 20/05/2026, tức dùng 20/31 ngày của kỳ 5. Ở kỳ 6 thuê
bao này bị loại hoàn toàn (đã huỷ trước kỳ), nên **nhánh "huỷ giữa kỳ" của công thức khoảng
ngày chưa từng chạy trên dữ liệu thật cho tới lúc này**.

Nó chạy đúng ngay lần đầu — nhưng đó là vì công thức ở mục 17.1 được viết dưới dạng **một
khoảng ngày phủ mọi trường hợp** thay vì phân nhánh theo trạng thái. Nếu viết theo cách phân
nhánh thì nhánh này rất dễ bị bỏ quên: nó không có trong đặc tả 4C, không có trong dữ liệu
kỳ 6, và chỉ lộ ra ở đây — sau ba mục.

---

## 39. Rà soát nợ tài liệu

Rà lại **toàn bộ** danh sách nợ tích luỹ từ 4A đến 4E. Với mỗi mục, kiểm lại xem còn đúng là
nợ không trước khi sửa.

| # | Mục nợ | Kết luận |
|---|---|---|
| 1 | Javadoc `ChiTietSuDung.so_luong` ghi *"số MB với DATA"* | ✅ **Đã sửa ở 4A** — đúng là KB |
| 2 | Javadoc `BangGiaCuoc.blockGiay` ghi *"1 MB với data"* | ⏸️ **Kiểm ra KHÔNG phải nợ** — với cách đã chốt (quy KB→MB rồi mới chia block, `block_giay = 1`) thì câu đó **đúng**. Giữ nguyên |
| 3 | Số dòng bảng giá mẫu 9 → 10 | ✅ Đã sửa ở 4A |
| 4 | Đề xuất lưu `bang_gia_cuoc_id` | ✅ **Đã làm ở 4D bước A** |
| 5 | Cảnh báo `spring.sql.init.mode: always` đã lỗi thời | ✅ **Đã sửa ở 4F** — `README.md` mục 6 và `mo-ta-csdl.md` mục 5.1 |
| 6 | `ma_kh` ghi dạng `KH0001` (4 chữ số) | ✅ **Đã sửa ở 4F** — 6 chữ số từ Phase 3A |
| 7 | `mo-ta-csdl.md` mục 4 ghi `ky_cuoc = 1 bản ghi` | ✅ **Đã sửa ở 4F** — 3 kỳ, kèm ghi chú phân biệt dữ liệu script với dữ liệu sinh lúc chạy |
| 8 | `mo-ta-csdl.md` mục 6 cần ghi **ba** chỗ quy đổi | ✅ **Đã viết lại ở 4F** — thêm chỗ thứ ba, thêm mục 6.0 về cách so đơn vị và mục 6.4 về cột snapshot |
| 9 | Số dư thuê bao trả trước quá thấp | ✅ **Đã sửa ở 4F** — mục 40 |
| 10 | README chưa có hướng dẫn chạy engine qua giao diện | ✅ **Đã thêm ở 4F** — mục 4.4 |
| 11 | Thiếu `PHASE-1-REPORT.md` | ⏸️ **Cố ý để lại** — xem bên dưới |
| 12 | Hóa đơn kỳ 5 quá hạn chưa tự chuyển `QUA_HAN` | ⏸️ **Cố ý để lại** — xem bên dưới |

### 39.1. Hai mục cố ý để lại

**Không viết bù `PHASE-1-REPORT.md`.** Phase 1 là phase thiết kế CSDL, và toàn bộ kết quả của
nó đã được ghi đầy đủ ở `mo-ta-csdl.md` — một tài liệu sống, được cập nhật liên tục qua các
phase. Viết thêm một báo cáo quá trình cho một phase đã kết thúc từ lâu sẽ tạo ra tài liệu
thứ hai mô tả cùng một thứ, và hai tài liệu song song là con đường chắc chắn dẫn tới việc
sửa một chỗ mà quên chỗ kia. Sẽ nêu lý do này trong báo cáo cuối kỳ.

**Không cài chức năng chuyển hóa đơn sang `QUA_HAN` ở Phase 4.** Kiểm chứng: 54 hóa đơn kỳ
5/2026 có hạn thanh toán 15/06/2026 — đã quá hạn — nhưng vẫn ở `CHUA_TT`. Đây **đúng như dự
kiến**: theo dõi công nợ thuộc Phase 5, và cột `trang_thai` của hóa đơn còn phụ thuộc dữ liệu
thanh toán mà Phase 4 cố ý chưa tạo. Ghi vào bàn giao (mục 45.2) để Phase 5 nhận.

---

## 40. Điều chỉnh số dư thuê bao trả trước

Nợ ghi từ mục 7.2: số dư mẫu 3.000–61.000 đ quá thấp so với cước phát sinh một tháng
(~85.000–110.000 đ), nên khi Phase 5 trừ cước vào số dư thì gần như cả 20 thuê bao đều âm
tiền — màn hình trông như hệ thống bị lỗi.

| Nhóm | Số thuê bao | Số dư | Lý do |
|---|---|---|---|
| Đủ tiền | **15** | 205.000 – 465.000 đ | Đủ trả cước một tháng, còn dư |
| **Cố ý để thấp** | **3** | 18.000 / 20.000 / 22.000 đ | Phase 5 cần trường hợp thật để minh hoạ cảnh báo *"số dư không đủ"* |
| Bằng 0 | 2 | 0 đ | Đã tạm ngưng hai chiều / đã thanh lý |

> Đây là **điều chỉnh dữ liệu mẫu cho hợp lý về nghiệp vụ, không phải sửa logic**. Không dòng
> mã nào của engine thay đổi, và không con số nào của kỳ 5 hay kỳ 6 bị ảnh hưởng — thuê bao
> trả trước không có hóa đơn tháng và engine không đụng tới `so_du`.

Sửa ở cả hai nơi: `data-mau.sql` (để lần chạy profile `reset` sau này nhận giá trị mới) và
CSDL đang chạy (để dữ liệu demo hiện tại dùng được ngay).

---

## 41. 📌 BẢNG SỐ LIỆU HAI KỲ

| Chỉ tiêu | Kỳ 5/2026 | Kỳ 6/2026 | Chênh lệch |
|---|---|---|---|
| Số bản ghi CDR | 3.697 | 5.017 | −1.320 |
| Số thuê bao phát sinh | 60 | 66 | −6 |
| **Số hóa đơn** | **54** | **58** | **−4** |
| Cước thuê bao | 17.762.258 đ | 18.400.000 đ | −637.742 đ |
| Cước thoại | 1.309.173 đ | 2.939.561 đ | −1.630.388 đ |
| Cước SMS | 90.626 đ | 96.267 đ | −5.641 đ |
| Cước dữ liệu | 191.725 đ | 328.350 đ | −136.625 đ |
| **Tổng doanh thu** | **21.289.162 đ** | **23.940.596 đ** | **−2.651.434 đ** |
| Trạng thái | **Đã chốt** | Mở | |

Hai kỳ chênh nhau đủ để biểu đồ Phase 6 có hình dạng, và kỳ 5 đã chốt nên là dữ liệu ổn định
để đối chiếu.

Điểm đáng chú ý: CDR giảm 26% nhưng **cước thoại giảm 55%**. Vì cước thoại tập trung ở các
cuộc quốc tế (không có ưu đãi, đơn giá cao gấp 144 lần nội mạng), mà số cuộc quốc tế giảm
theo tỷ lệ trong khi phần lớn cuộc nội mạng vốn đã miễn phí. Đây cũng là minh hoạ vì sao
doanh thu **không** tỷ lệ thuận với sản lượng khi có gói cước.

---

## 42. ⭐ SÁU ĐIỂM SAI LỆCH ĐẶC TẢ ĐÃ PHÁT HIỆN Ở PHASE 4

Đây là mục có giá trị nhất của báo cáo. Sáu điểm dưới đây đều là chỗ **đặc tả nói một đằng mà
làm theo sẽ sai**, hoặc **đặc tả không lường tới**. Mỗi điểm ghi: phát hiện thế nào, hậu quả
nếu bỏ qua, và cách xử lý.

### 42.1. 🔴 251 bản ghi CDR không tra được đơn giá — trục `huong` không ai kiểm

| | |
|---|---|
| **Phát hiện** | Rà soát đầu Phase 4: đối chiếu **mọi tổ hợp** `(dịch vụ, hướng, cao điểm)` có trong CDR với bảng giá |
| **Nguyên nhân** | Bộ sinh CDR chọn hướng **độc lập** với loại dịch vụ, nhưng bảng giá chỉ có `DATA/NOI_MANG` và `SMS/{NOI_MANG, NGOAI_MANG}` |
| **Hậu quả nếu bỏ qua** | 251 bản ghi (**5,00%**) rơi thẳng vào trạng thái `LOI`, hóa đơn thiếu tiền của 5% sản lượng |
| **Cách xử lý** | Ba tổ hợp, ba cách khác nhau theo bản chất: `DATA/NGOAI_MANG` và `DATA/QUOC_TE` là mô hình hoá sai → cấm sinh; `SMS/QUOC_TE` là dịch vụ có thật → thêm dòng bảng giá. Tạo `QuyTacToHopDichVu` làm nơi duy nhất định nghĩa |

**Điều đáng nói không phải con số mà là vì sao nó lọt qua cả 10 tiêu chí nghiệm thu Phase 3.**
Phase 3 đã nhận ra đúng kiểu rủi ro này trên trục `gio_cao_diem`, xử lý rất tốt — tách lớp
riêng, viết javadoc giải thích — rồi đặt tiêu chí kiểm **đúng cái luật vừa xử lý**. Trục
`huong` có y hệt rủi ro, không ai kiểm, và lọt.

### 42.2. 🟠 Cận `23:59:59` bỏ sót bản ghi cuối kỳ

| | |
|---|---|
| **Phát hiện** | Đặc tả 4B yêu cầu lấy CDR trong `[đầu kỳ 00:00:00, cuối kỳ 23:59:59]` |
| **Vấn đề** | Cận trên đóng ở `23:59:59` bỏ sót mọi bản ghi từ `23:59:59,000001` tới hết ngày |
| **Đo thực tế** | Cột `thoi_gian_bat_dau` khai `DATETIME` không có phần lẻ giây → **0 bản ghi bị mất hôm nay**. Nhưng bản ghi muộn nhất là `30/06/2026 23:59:03` — cách ranh giới **56 giây** |
| **Hậu quả nếu bỏ qua** | Đổi cột sang `DATETIME(3)` — một việc bình thường khi cần độ chính xác cao hơn — là mất bản ghi ngay, và mất **im lặng** |
| **Cách xử lý** | Dùng khoảng **nửa mở** `[đầu kỳ 00:00, đầu kỳ sau 00:00)`. Chi phí bằng 0, kết quả hôm nay giống hệt |

### 42.3. 🟠 Snapshot đơn giá không khả thi ở mức dòng hóa đơn

| | |
|---|---|
| **Phát hiện** | Đặc tả 4C yêu cầu *"chi_tiet_hoa_don … SNAPSHOT đơn giá tại thời điểm lập"* |
| **Vấn đề** | Mỗi dòng cước sử dụng là tổng của nhiều bản ghi áp **đơn giá khác nhau** (nội mạng 15 đ, ngoại mạng 25 đ, quốc tế 3.600 đ, cộng bậc cao điểm). Không tồn tại một đơn giá duy nhất để ghi |
| **Hậu quả nếu làm bừa** | Điền đơn giá bình quân suy ngược từ thành tiền = con số **không có trên bảng giá nào**. Còn tách tới từng bậc giá thì phải **suy ngược lại bảng giá ở khâu lập hóa đơn** — mà bảng giá có thể đã đổi, nên đơn giá in ra không phải đơn giá đã thu |
| **Cách xử lý** | 4C: để trống đơn giá ở ba dòng cước sử dụng, ghi rõ lý do, và **đề xuất đổi schema**. 4D: thực hiện đề xuất — thêm cột `chi_tiet_su_dung.bang_gia_cuoc_id`. Nhờ đó bảng đối soát ở 4E in được đơn giá thật của **từng bản ghi** |

Đây là điểm duy nhất trong sáu điểm mà cách xử lý **kéo dài qua ba mục**: nêu vấn đề ở 4C,
đổi schema ở 4D, thu hoạch ở 4E.

### 42.4. 🔴 Mâu thuẫn quy đổi ưu đãi — 4 thuê bao bị thu tiền oan

| | |
|---|---|
| **Phát hiện** | Đặc tả 4D mục B đưa **hai chỉ dẫn không tương thích trong cùng một khối**: *"dùng `DonViCuoc.giaySangPhut()`"* (làm tròn lên từng bản ghi) và *"ưu đãi trừ theo sản lượng thô"* |
| **Cách quyết** | Đo **cả hai cách trên dữ liệu thật trước khi viết code** |
| **Kết quả đo** | Cách "quy từng CDR lên" thổi phồng tổng phút thoại **+10,97%**, làm **4 thuê bao** bị coi là vượt quota trong khi chưa hề vượt |
| **Hậu quả nếu bỏ qua** | Thuê bao `0832345622` dùng thật **89,2 phút** trên quota 100 phút, bị tính thành 105 phút và **thu tiền oan**. Hóa đơn vẫn phát hành bình thường, không cảnh báo |
| **Cách xử lý** | So ở **đơn vị nhỏ nhất**: quy quota **xuống** giây và KB (`phút × 60`, `MB × 1024`) rồi so với sản lượng thô. Không còn phép làm tròn nào trong phép so |

Đây là điểm sai lệch **tốn kém nhất nếu bỏ qua**, vì nó sai theo hướng bất lợi cho khách hàng
và không có dấu hiệu nào để phát hiện ngoài việc ngồi đối chiếu tay.

### 42.5. 🟡 Test "đảo thứ tự → kết quả không đổi" là bất khả

| | |
|---|---|
| **Phát hiện** | Đặc tả 4D mục D.8 yêu cầu test *"đảo thứ tự đầu vào → kết quả không đổi"* |
| **Vấn đề** | Điều đó **không thể đúng** với thuật toán đã chốt: quy tắc không cắt đôi bản ghi (quyết định 5.3) làm kết quả **phụ thuộc thứ tự** một cách cố hữu |
| **Ví dụ** | Quota 60 giây, ba cuộc 30/40/20 giây: thứ tự gốc miễn phí **1** cuộc, đảo ngược miễn phí **2** cuộc |
| **Hậu quả nếu làm theo** | Viết một test khẳng định điều sai — hoặc test đỏ vĩnh viễn, hoặc phải sửa thuật toán cho "đúng" theo một yêu cầu vô lý |
| **Cách xử lý** | Tách thành **hai** test có giá trị hơn: *tính xác định* (cùng danh sách chạy hai lần ra cùng kết quả) và *phụ thuộc thứ tự* (hai thứ tự cho hai kết quả khác nhau) — cái thứ hai chính là bằng chứng vì sao truy vấn **bắt buộc** phải có `ORDER BY` cố định |

### 42.6. 🟡 `ky_cuoc.trang_thai` ba giá trị không đủ phân biệt sáu tình huống nút

| | |
|---|---|
| **Phát hiện** | Đặc tả 4E mô tả nút theo trạng thái kỳ, nhưng cột `trang_thai` chỉ có `MO` / `DANG_TINH` / `DA_CHOT` |
| **Vấn đề** | Ba tình huống rất khác nhau — *chưa tính cước*, *đã tính xong chờ lập hóa đơn*, *đã có hóa đơn* — đều mang cùng một trạng thái `MO` |
| **Hậu quả nếu bỏ qua** | Hoặc hiện đủ mọi nút cho mọi kỳ (người dùng bấm nhầm rồi nhận lỗi), hoặc thêm giá trị vào enum trạng thái — làm cột này mang hai ý nghĩa chồng nhau |
| **Cách xử lý** | Giữ nguyên enum, thêm DTO `TinhTrangKy` gom năm con số đếm được và dẫn xuất ra sáu điều kiện bật nút. Trạng thái ở CSDL vẫn chỉ mô tả **vòng đời kỳ**, còn tiến độ xử lý được suy ra khi cần |

---

## 43. ⭐ BÀI HỌC PHƯƠNG PHÁP

Sáu bài học rút từ chính những sai lệch ở mục 42. Chúng không phải lý thuyết — mỗi cái đều
gắn với một lỗi thật đã bắt được hoặc đã suýt bỏ sót.

### 43.1. Kiểm **bất biến**, đừng kiểm **luật cụ thể**

Phase 3 viết tiêu chí *"non-THOAI không mang cờ giờ cao điểm"* — kiểm đúng cái luật vừa xử
lý. Trục `huong` có cùng rủi ro, không ai kiểm, và 251 bản ghi lọt qua cả 10 tiêu chí.

Bất biến đúng phải là: *mọi tổ hợp tra giá đều phải tra ra đơn giá*. Nó bao trùm mọi trục,
kể cả trục chưa ai nghĩ tới.

> **Tiêu chí nghiệm thu viết sau khi đã hiểu vấn đề sẽ kiểm đúng phần đã hiểu. Muốn bắt được
> phần chưa hiểu thì phải kiểm bất biến.**

### 43.2. Kiểm **toàn bộ**, đừng lấy mẫu — khi chi phí gần như bằng nhau

| Mục | Đặc tả yêu cầu | Đã làm |
|---|---|---|
| 4B | Đối chiếu tay 3 CDR mỗi loại dịch vụ | Đối chiếu **toàn bộ 5017** bản ghi bằng SQL |
| 4C | — | Đối chiếu **từng hóa đơn một**, không chỉ tổng |
| 4E | Mở bảng đối soát 3 thuê bao đại diện | Mở **cả 58 bảng đối soát** |

Ba mẫu chứng minh được ít hơn nhiều so với kiểm hết, mà chi phí chênh nhau không đáng kể khi
đã viết được câu truy vấn.

### 43.3. Kiểm **từng dòng**, đừng kiểm **số tổng**

Hai sai lệch ngược dấu ở hai hóa đơn khác nhau sẽ **triệt tiêu nhau** ở mức tổng.

Có một ví dụ cụ thể ở 4C: tổng VAT của 58 hóa đơn là 2.608.437 đ, còn
`ROUND(tổng trước thuế × 10%)` là 2.608.435 đ — **lệch 2 đồng**. Đây không phải lỗi: VAT tính
trên từng hóa đơn rồi mới cộng, đúng như hóa đơn thật phải làm. Nhưng nếu kiểm ở mức tổng thì
câu SQL sẽ báo lệch và người đọc tưởng engine sai.

### 43.4. Công bố **dự đoán trước** khi viết code

Áp dụng hai lần, cả hai lần đều ra đúng:

| Ở mục | Dự đoán công bố trước | Kết quả |
|---|---|---|
| 4C → 4D | Số hóa đơn có `cuoc_data > 0` tụt từ 50 xuống **đúng 5**, gồm 3 gói CB01 + 2 gói MAX70 | ✅ đúng cả con số lẫn thành phần |
| 4F | Kỳ 5 có **54** hóa đơn, **1** thuê bao prorate **32.258 đ**, tổng cước thuê bao **17.762.258 đ** | ✅ đúng tới từng đồng |

Giá trị của cách làm này không nằm ở việc đoán đúng, mà ở chỗ: **nếu số thực tế lệch thì đó
là tín hiệu dừng lại phân tích, không phải tín hiệu sửa cho khớp.** Dự đoán viết sau khi thấy
kết quả thì không còn khả năng đó.

### 43.5. Một **phép kiểm sai** nguy hiểm ngang **thiếu phép kiểm**

Ở 4D, lần kiểm chứng đầu báo *25 thuê bao vi phạm* ở phép kiểm ưu đãi SMS. Truy nguyên ra
nguyên nhân nằm ở **câu SQL kiểm chứng**, không ở engine: nó cộng cả SMS quốc tế — vốn không
có ưu đãi — vào phép so với quota.

Nếu tin phép kiểm đó thì đã đi "sửa" một engine đang chạy đúng. Phép kiểm sai tạo ra báo động
giả, và báo động giả làm hỏng lòng tin vào mọi phép kiểm còn lại.

### 43.6. Bảng đối soát phải **đọc lại**, không được **tính lại**

`DoiSoatCuocService` không tính lại bất cứ con số tiền nào — mọi giá trị lấy thẳng từ dữ liệu
đã ghi. Nếu nó tự tính lại theo cách riêng thì nó không chứng minh được hóa đơn đúng; nó chỉ
chứng minh chính nó.

Hệ quả kéo theo: khoảng ngày sử dụng và gói cước hiệu lực phải **dùng chung** lớp
`QuyTacKyCuoc` với engine lập hóa đơn. Hai cách tính song song là hai nguồn sự thật, và bảng
đối soát sẽ có lúc mâu thuẫn với chính hóa đơn nó đang đối soát.

---

## 44. Danh sách màn hình chụp ảnh cho toàn Phase 4

Đăng nhập `admin`. Nhóm 1 là các ảnh nên đưa vào phần trình bày chính.

### Nhóm 1 — Bảng đối soát cước ⭐

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 1 | **Đối soát — thuê bao vượt ưu đãi data** | `/tinh-cuoc/doi-soat/21/1` | Khối 2: `5.739,7 MB (5.877.492 KB)` — quy đổi KB→MB hiện ngay trên màn hình. Gói CB01 quota 0 MB nên toàn bộ bị thu tiền |
| 2 | **Đối soát — thuê bao sát ranh giới quota phút** | `/tinh-cuoc/doi-soat/34/1` | `99,6 phút (5.978 giây)` cạnh `Quota 100 phút / 6.000 giây` → **0 đ**. Phóng to đúng dòng này |
| 3 | **Khối 4 — đối chiếu hóa đơn** | Cuộn cuối ảnh 1 hoặc 2 | Cột chênh lệch **toàn 0 đ**, khung xanh *"Khớp tuyệt đối"* |
| 4 | Đối soát — dòng làm vượt ưu đãi | Ảnh 1, khối 3 | Dòng tô nền kèm vạch mép trái và chú thích quy tắc không cắt đôi bản ghi |
| 5 | Đối soát — thuê bao trọn trong ưu đãi | `/tinh-cuoc/doi-soat/57/1` | 70/70 bản ghi *Miễn phí*, mọi dòng khối 2 đều 0 đ |
| 6 | Đối soát — prorate | Ảnh 2, khối 1 | Khung ghi `20/30 ngày` và phép tính cước thuê bao |
| 7 | **Bản in A4** | Mở ảnh 1 → bấm **In** → xem trước | Không còn sidebar và nút; hiện đủ mọi dòng; tiêu đề riêng cho bản in |

### Nhóm 2 — Màn hình điều khiển

| # | Màn hình | Cách lấy | Điểm cần thấy rõ |
|---|---|---|---|
| 8 | Màn hình tính cước | `/tinh-cuoc` | Ba kỳ; kỳ 5 **Đã chốt** chỉ xem, kỳ 6 **Mở** còn nút; cột "Bước tiếp theo" |
| 9 | Hộp kết quả sau khi chạy | Bấm **Huỷ hóa đơn** rồi **Lập hóa đơn** kỳ 6 | Khung xanh *"Hoàn thành"* kèm số hóa đơn, doanh thu, thời gian |
| 10 | Modal cảnh báo chốt kỳ | Bấm **Chốt kỳ**, chưa xác nhận | Câu *"MỘT CHIỀU, không có đường quay lại"* |
| 11 | Kỳ đã chốt | Dòng kỳ 5/2026 | Badge *Đã chốt*, ngày chốt, dòng *"Chỉ xem"*, không còn nút |
| 12 | Danh sách hóa đơn của kỳ | `/tinh-cuoc/ky/1` | 58 hóa đơn; đúng **5 dòng** có cột Dữ liệu tô đỏ đậm |
| 13 | Chặn phân quyền | Đăng nhập `ketoan01`, gõ `/tinh-cuoc` | Trang 403 |
| 14 | Đường vào từ chi tiết thuê bao | `/thue-bao/22` | Nút *"Xem đối soát cước"* đổ ra danh sách kỳ |

### Nhóm 3 — Minh chứng kỹ thuật

| # | Ảnh | Cách lấy |
|---|---|---|
| 15 | **Kết quả 148 test** | Console `mvnw test`, phóng to dòng `Results: Tests run: 148, Failures: 0` |
| 16 | Test bẫy quy đổi đơn vị | Chạy `UuDaiGoiCuocTest` trong IntelliJ, chụp cây kết quả nhóm *"Bẫy quy đổi đơn vị"* |
| 17 | Test bất biến độ phủ bảng giá | Chạy `KiemTraDoPhuBangGiaTest`, 3 test xanh |
| 18 | Lịch sử Git Phase 4 | `git log --oneline -8` |

> Ảnh 1 và 2 cho thấy cùng lúc: quy đổi đơn vị đúng, quỹ ưu đãi trừ đúng, đơn giá lấy từ ảnh
> chụp bảng giá, và hóa đơn khớp tuyệt đối với chi tiết sử dụng — tức toàn bộ những gì Phase 4
> phải chứng minh, trên một màn hình.

---

## 45. Bàn giao cho Phase 5

### 45.1. Trạng thái dữ liệu

| Bảng | Số bản ghi | Ghi chú |
|---|---|---|
| `khach_hang` · `thue_bao` · `goi_cuoc` | 50 · 80 · 5 | Không đổi từ Phase 1 |
| `bang_gia_cuoc` | 10 | +1 dòng `SMS/QUOC_TE` thêm ở 4A |
| `ky_cuoc` | 3 | **5/2026 đã chốt** · **6/2026 mở** · 7/2026 chưa dùng |
| `chi_tiet_su_dung` | **8.714** | 3.697 của kỳ 5 + 5.017 của kỳ 6, tất cả `DA_TINH` |
| `hoa_don` | **112** | 54 của kỳ 5 + 58 của kỳ 6, tất cả `CHUA_TT` |
| `chi_tiet_hoa_don` | **264** | Tự sinh theo hóa đơn; hóa đơn không phát sinh cước chỉ có 1 dòng |
| `thanh_toan` · `nap_tien` · `giam_tru` | **0** | **Cố ý để trống** — thuộc Phase 5 |

Thuê bao trả trước: 15 có số dư 205.000–465.000 đ, **3 cố ý để thấp** (18.000 / 20.000 /
22.000 đ) làm ca thử *"số dư không đủ"*, 2 bằng 0 vì đã tạm ngưng / thanh lý.

### 45.2. Ba việc Phase 5 nhận từ Phase 4

1. **Chuyển hóa đơn quá hạn sang `QUA_HAN`.** 54 hóa đơn kỳ 5/2026 có hạn thanh toán
   15/06/2026 đã quá hạn nhưng vẫn `CHUA_TT` — Phase 4 cố ý không cài vì trạng thái hóa đơn
   phụ thuộc dữ liệu thanh toán.
2. **Trừ cước vào số dư thuê bao trả trước.** Engine tính `cuoc_phi` cho cả thuê bao trả
   trước nhưng **không** động vào `so_du` (quyết định 5.4). Toàn bộ 20 thuê bao trả trước đã
   có cước trên CDR, chờ Phase 5 xử lý:

   | Kỳ | Thuê bao trả trước có phát sinh | Số CDR | Tổng cước đã tính |
   |---|---|---|---|
   | 5/2026 | 15 | **921** | 1.643.768 đ |
   | 6/2026 | 16 | **1.126** | 2.001.863 đ |
3. **Ràng buộc đã có sẵn cần tôn trọng.** `BillingService.huyBillingKy` từ chối xoá hóa đơn
   khi kỳ đã ghi nhận thanh toán. Sau khi Phase 5 tạo dữ liệu thanh toán, **kỳ đó không huỷ
   hóa đơn được nữa** — nên tạo thanh toán ở kỳ 5 (đã chốt) trước, giữ kỳ 6 linh hoạt để demo.

### 45.3. Lưu ý về kỳ 6/2026

Kỳ 6/2026 **cố ý để trạng thái `MO`** để chụp ảnh các nút thao tác. Kỳ này đã được chốt một
lần trong lúc kiểm chứng ở 4E rồi trả lại `MO` bằng lệnh SQL — chốt kỳ là thao tác một chiều
theo thiết kế và cố ý không có nút mở lại. Sau khi chụp xong ảnh, bấm **Chốt kỳ** một lần là
đưa về trạng thái cuối cùng.
