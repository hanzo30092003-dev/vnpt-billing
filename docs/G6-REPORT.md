# G6 — HAI MỤC CUỐI CỦA BẢN RÀ TÍNH NĂNG

> Mục 4 và mục 5 trong khuyến nghị của `docs/RA-SOAT-TINH-NANG.md`. Sau đợt này **không còn
> khuyến nghị nào treo**.

Ngày làm: 27/08/2026 · Trên bản `8308e03`

---

## Mục 4 — Phép kiểm cho `/cdr/import`

### Vì sao đáng làm

Đợt rà tính năng tìm ra: nhập CDR từ CSV là chức năng **duy nhất** trong phạm vi đã cam kết có
đủ mã nguồn, có màn hình, tới được từ menu, trang trả 200 — nhưng **không một phép kiểm nào**
chạm tới. 0 test Java, và cả 8 script giao diện đều không nhắc tên nó.

Đó là loại lỗ hổng nguy hiểm hơn một chức năng còn thiếu: nó **tạo ảo giác đã xong**.

### Quyết định quan trọng nhất: Mockito, không chạm CSDL

`CdrImportService` ghi thẳng vào `chi_tiet_su_dung` bằng `JdbcTemplate`. Bộ test của dự án chạy
trên **CSDL thật**, mà **18.723 CDR** là con số tài liệu bàn giao ghi rõ. Một phép kiểm chèn
thật vào đó là làm hỏng dữ liệu mẫu **ngay lần chạy đầu**, và hỏng im lặng.

Giả lập cả ba phụ thuộc (`JdbcTemplate`, `ThueBaoRepository`, `NhatKyService`) thì không dòng
nào được ghi — mà vẫn kiểm được **trọn bộ luật**, vì toàn bộ luật nằm ở tầng phân tích dòng,
chạy **trước** khi chạm CSDL.

Đây cũng là quy ước áp đảo của dự án: phần lớn test dùng `@ExtendWith(MockitoExtension.class)`.

### 27 phép kiểm, chia năm nhóm

| Nhóm | Số | Phủ cái gì |
|---|--:|---|
| 1. Hai chốt chặn ở mức file | 4 | không chọn file · file rỗng · sai đuôi `.csv` · và **chặn ở mức file thì không ghi gì** |
| 2. Dòng tiêu đề và dòng trắng | 3 | tiêu đề không tính là bản ghi · dòng trắng không thành dòng lỗi · file không tiêu đề vẫn nhập được |
| 3. Chín luật kiểm từng dòng | 15 | sai số cột · thuê bao lạ · tạm ngưng 2C · đã thanh lý · dịch vụ lạ · hướng lạ · tổ hợp DATA+hướng · thời gian sai định dạng · trước ngày kích hoạt · cột số không phải số · số âm · THOAI thiếu thời lượng |
| 4. File lẫn dòng tốt và dòng hỏng | 2 | dòng hỏng không kéo theo dòng tốt · **số dòng báo lỗi đếm theo file gốc** |
| 5. Ghi xuống CSDL và nhật ký | 3 | lô hợp lệ có gọi `batchUpdate` · file toàn dòng hỏng thì **không gọi lần nào** · mỗi lần nhập ghi một dòng nhật ký |

### Ba phép kiểm là ĐỐI CHỨNG, không phải phủ thêm luật

Trộn lẫn trong nhóm 3, cố ý:

| Phép kiểm | Nó canh cái gì |
|---|---|
| `tamNgungMotChieuVanNhan` | Chặn nhầm `TAM_NGUNG_1C` là **chặn oan** thuê bao vẫn gọi ra được |
| `dataNoiMangVanNhan` | Luật tổ hợp phải chặn `DATA+NGOAI_MANG` mà **không** chặn `DATA+NOI_MANG` |
| `smsKhongCanThoiLuong` | Luật "phải có thời lượng" chỉ áp cho `THOAI`, SMS thời lượng 0 là bình thường |

Không có ba cái này thì một phép "chặn tất" cũng làm mọi phép kiểm còn lại xanh.

### Đối chứng âm — dự đoán công bố trước

Gỡ ba luật khỏi service, **dự đoán đỏ đúng 4 phép kiểm**: `thueBaoTamNgungHaiChieu`,
`thueBaoDaThanhLy`, `goiKhongCoThoiLuong`, `dataChiCoNoiMang`.

```
Tests run: 27, Failures: 4
  CdrImportServiceTest.dataChiCoNoiMang
  CdrImportServiceTest.goiKhongCoThoiLuong
  CdrImportServiceTest.thueBaoDaThanhLy
  CdrImportServiceTest.thueBaoTamNgungHaiChieu
```

Đúng 4, đúng tên. Service đã khôi phục nguyên vẹn (`git diff` rỗng).

### Phần cố ý KHÔNG kiểm

Câu SQL chèn có đúng cột không — đó là việc của một phép kiểm **chạm CSDL**, nằm ngoài phạm vi
đợt này. Ghi ra đây để người sau biết chỗ hở còn lại nằm ở đâu, thay vì tưởng đã phủ hết.

---

## Mục 5 — Đổi chữ lối tắt

### Vấn đề

Lối tắt số 4 trên trang chủ ghi **"Ghi nhận thanh toán"** nhưng `href="/cong-no"`. Màn hình đó
chứa **0** lần chuỗi "Ghi nhận thanh toán"; nút thật nằm trên chi tiết hóa đơn, **cách đó ba
lần bấm nữa**. Nhãn hứa một việc mà chỗ nó dẫn tới không làm được.

### Sửa

| | Trước | Sau |
|---|---|---|
| Nhãn | Ghi nhận thanh toán | **Tra cứu để thu tiền** |
| Dòng phụ | Khách vừa trả tiền | **Tìm hóa đơn còn nợ của khách** |

Nhãn mới nói đúng thứ màn hình đích **làm**, không nói thứ người dùng **muốn**.

**Nút thật trên chi tiết hóa đơn giữ nguyên tên "Ghi nhận thanh toán"** — chuỗi đó xuất hiện ở
nhiều tài liệu và màn hình khác, và ở đó nó đúng. Chỉ nhãn lối tắt là sai.

`docs/huong-dan-su-dung.md` cập nhật theo, kèm một câu nói rõ nút thu tiền nằm ở đâu.

### Vì sao không làm Đ12 (đổi luồng)

Hướng đầy đủ là thêm ô "Số thuê bao" ngay trên `/thanh-toan` để nhảy thẳng tới hóa đơn còn nợ.
Nhưng nó cần **action mới ở controller** — thiết kế lại một luồng nghiệp vụ, không phải chỉnh
giao diện. Mục 5 chữa được phần lớn giá trị (nhãn không còn nói dối) bằng 10 phút.

Luồng vẫn tốn 5 lần bấm. Đó là **hạn chế đã ghi**, không phải lỗi bị bỏ sót.

---

## Nghiệm thu

| | Trước G6 | Sau G6 |
|---|--:|--:|
| `mvnw test` | 315 | **342** (0 lỗi) |
| Lớp test | 32 | **33** |
| 8 script giao diện | 215 | **215** (0 sai) |
| 3 phép kiểm Python | đạt | **đạt** |

### Dữ liệu không suy suyển — điều quan trọng nhất của mục 4

| Bảng | Phải giữ | Đo được |
|---|--:|--:|
| `chi_tiet_su_dung` | 18.723 | **18.723** |
| `bien_dong_so_du` | 35 | **35** |
| `hoa_don` | 280 | **280** |
| `thanh_toan` | 161 | **161** |

27 phép kiểm mới chạy trên CSDL thật mà **không ghi một dòng nào** — đúng như thiết kế.

### Số liệu đã cập nhật ở bốn chỗ mô tả hiện trạng

`CLAUDE.md:61` · `README.md:222` · `README.md:300` · `docs/danh-sach-anh-chup.md` ảnh **#51**
(*"Kết quả 342 test"* — đây là danh sách chụp sắp dùng, để sai là chụp ra ảnh mâu thuẫn với
console).

Các chỗ khác ghi 315 đều là **ghi chép lịch sử của những đợt trước** (`KE-HOACH` các mục V/N/G,
`RA-SOAT-GIAO-DIEN`) — giữ nguyên, vì chúng kể đúng cái đã đúng lúc đó.

---

## Trạng thái sau sáu đợt G

Bản rà tính năng đưa ra **5 khuyến nghị**. Cả năm đã làm xong:

| | Việc | Đợt |
|---|---|---|
| 1 | Vệt bánh mì 404 | G5 |
| 2 | Câu tài liệu sai về hạn mức | G5 |
| 3 | Nạp một giao dịch `NAP_TIEN` | G5 |
| 4 | Phép kiểm cho `/cdr/import` | **G6** |
| 5 | Đổi chữ lối tắt | **G6** |

Kết luận của bản rà tính năng không đổi và giờ vững hơn: **phần mềm đã đủ tính năng cốt lõi để
bảo vệ đồ án.** Việc còn lại duy nhất trong kế hoạch là **N2 — chụp 70 ảnh**, và chưa có ảnh
nào bị ảnh hưởng vì chưa chụp tấm nào.
