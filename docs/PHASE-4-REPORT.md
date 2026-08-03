# BÁO CÁO PHASE 4 — ENGINE TÍNH CƯỚC

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Môn học:** Thực tập nghề nghiệp
**Trạng thái:** 🚧 Đang thực hiện — **mục 4A đã xong**, 4B–4G chưa bắt đầu

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

## 9. Việc tiếp theo — mục 4B

Dựng `RatingService` trên nền `BangGiaLookup` và `DonViCuoc`: tính `cuoc_phi` cho từng CDR,
gán `ky_cuoc_id`, chuyển trạng thái sang `DA_TINH` hoặc `LOI`.

Ba điều 4B phải giữ:

1. Tra giá theo **ngày phát sinh của CDR**, không phải ngày chạy engine
2. Ranh giới kỳ dùng **nửa mở** `[đầu kỳ 00:00, đầu kỳ sau 00:00)` — viết
   `ngayKetThuc.atTime(23,59,59)` sẽ bỏ sót bản ghi có phần lẻ giây, và hiện có 10 CDR phát
   sinh lúc 23 giờ ngày 30/06, sát ngay ranh giới đó
3. Làm tròn tiền ở **đúng một tầng**: từng CDR. Các mức trên chỉ cộng dồn
