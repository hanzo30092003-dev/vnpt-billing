# TỐI ƯU HIỆU NĂNG

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại

| Phần | Phạm vi | Ngày đo |
|---|---|---|
| I (mục 1–7) | Ghi hàng loạt bản ghi CDR — `CdrGeneratorService`, Phase 3 | 02/08/2026 |
| II (mục 8–10) | Truy vấn thống kê của dashboard và bảy báo cáo — Phase 6 | 10/08/2026 |

---

# PHẦN I — GHI HÀNG LOẠT BẢN GHI CDR

---

## 1. Bài toán

Bộ sinh CDR phải ghi hàng nghìn bản ghi vào bảng `chi_tiet_su_dung` trong một lần
chạy. Đây là thao tác ghi hàng loạt duy nhất của hệ thống tính đến Phase 3, và cũng
là chỗ dễ chậm nhất nếu làm sai.

Có ba mức tối ưu, mỗi mức giải quyết một loại chi phí khác nhau:

| Mức | Vấn đề được giải quyết |
|---|---|
| 1. Bỏ `saveAll()`, dùng `JdbcTemplate` | Chi phí của tầng JPA / persistence context |
| 2. Gom thành lô 500 | Số vòng đi lại giữa ứng dụng và CSDL |
| 3. `rewriteBatchedStatements=true` | Số câu lệnh SQL mà MySQL phải phân tích |

---

## 2. Mức 1 — Không dùng `repository.saveAll()`

`saveAll()` của Spring Data gọi `persist()` cho từng phần tử, sinh ra đúng bằng đó câu
INSERT rời và **giữ toàn bộ entity trong persistence context**. Với 5000 bản ghi:

- Hibernate phải quản lý 5000 entity trong bộ nhớ, dù không bản ghi nào được đọc lại
- Mỗi lần flush phải quét toàn bộ persistence context để tìm thay đổi (dirty checking)

Vì các bản ghi CDR sinh ra chỉ để ghi xuống rồi thôi, toàn bộ chi phí đó là vô ích.
Giải pháp: dùng thẳng `JdbcTemplate.batchUpdate()` với một câu INSERT đã chuẩn bị sẵn.

## 3. Mức 2 — Gom thành lô 500 bản ghi

Kích thước lô là một sự đánh đổi:

- **Lô quá nhỏ** → nhiều vòng đi lại tới CSDL, mỗi vòng tốn một lần chờ mạng
- **Lô quá lớn** → câu lệnh gộp có nguy cơ vượt `max_allowed_packet` của MySQL
  (mặc định 64 MB, nhưng vẫn nên tránh câu lệnh khổng lồ), đồng thời tốn bộ nhớ

500 là điểm cân bằng thường dùng và đủ tốt cho quy mô của đồ án.

## 4. Mức 3 — `rewriteBatchedStatements=true`

Đây là điểm **dễ bị bỏ sót nhất**: chỉ gọi `batchUpdate()` thôi là chưa đủ.

Nếu không bật tham số này, MySQL Connector/J vẫn gửi **từng câu INSERT riêng lẻ** cho
máy chủ, chỉ tiết kiệm được phần lập trình chứ gần như không tiết kiệm được chi phí
phân tích câu lệnh phía MySQL.

Khi bật, driver **viết lại** cả lô thành một câu lệnh nhiều `VALUES`:

```sql
-- Trước: 500 câu lệnh riêng
INSERT INTO chi_tiet_su_dung (...) VALUES (...);
INSERT INTO chi_tiet_su_dung (...) VALUES (...);
-- ... 498 câu nữa

-- Sau: 1 câu lệnh
INSERT INTO chi_tiet_su_dung (...) VALUES (...), (...), (...), ... ;
```

Cấu hình trong `application.yml`:

```
jdbc:mysql://localhost:3306/vnpt_billing?...&rewriteBatchedStatements=true
```

---

## 5. Số liệu đo được

**Môi trường đo:** MySQL 8.4.9 chạy cục bộ trên cùng máy, JDK 25, Spring Boot 3.5.16.
Thời gian lấy từ đồng hồ bên trong `CdrGeneratorService` (chỉ tính phần sinh dữ liệu và
ghi CSDL, không tính thời gian render trang). Mỗi lần đo đều xoá sạch bảng
`chi_tiet_su_dung` trước.

### 5.1. Sinh 5000 bản ghi

| Lần đo | Chưa bật `rewriteBatchedStatements` | Đã bật |
|---|---|---|
| Lần 1 | 1236 ms | 248 ms |
| Lần 2 | 1092 ms | 274 ms |
| Lần 3 | 1122 ms | 249 ms |
| **Trung bình** | **1150 ms** | **257 ms** |

**Cải thiện: nhanh hơn khoảng 4,5 lần**, tiết kiệm ~893 ms.

### 5.2. Sinh 20000 bản ghi

| | Chưa bật | Đã bật |
|---|---|---|
| Thời gian | 4421 ms | 1145 ms |

**Cải thiện: nhanh hơn khoảng 3,9 lần**, tiết kiệm ~3,3 giây.

### 5.3. Biểu đồ so sánh

```
Sinh 5000 bản ghi
  Chưa bật  ████████████████████████████████████████████  1150 ms
  Đã bật    ██████████                                     257 ms

Sinh 20000 bản ghi
  Chưa bật  ████████████████████████████████████████████  4421 ms
  Đã bật    ███████████                                   1145 ms
```

---

## 6. Nhận xét

1. **Mức cải thiện tăng theo số bản ghi về giá trị tuyệt đối** nhưng tỷ lệ thì ổn định
   quanh 4 lần. Điều này hợp lý: chi phí tiết kiệm được tỷ lệ thuận với số câu lệnh
   mà MySQL không còn phải phân tích.

2. **Cả hai trường hợp đều đạt tiêu chí "dưới 10 giây"** cho 5000 bản ghi. Nghĩa là nếu
   chỉ nhìn vào tiêu chí nghiệm thu thì sẽ không phát hiện ra thiếu sót — đây chính là
   lý do phải đo và so sánh có chủ đích, thay vì chỉ kiểm tra "có chạy được không".

3. **Tham số nằm ở chuỗi kết nối, không nằm trong code.** Một người đọc code
   `batchUpdate()` sẽ tưởng đã tối ưu xong. Vì vậy tham số này được ghi chú thẳng trong
   `application.yml` kèm liên kết tới tài liệu đo.

4. **Giới hạn của phép đo:** MySQL chạy cùng máy nên độ trễ mạng gần bằng 0. Trong môi
   trường thật, nơi CSDL nằm trên máy chủ riêng, khoảng cách giữa hai phương án sẽ còn
   lớn hơn nữa vì mỗi câu lệnh thừa đều phải đi qua mạng.

---

## 7. Cách tự kiểm chứng lại

1. Mở `src/main/resources/application.yml`, xoá `&rewriteBatchedStatements=true` khỏi
   chuỗi kết nối
2. Khởi động lại ứng dụng, vào `/cdr/sinh-du-lieu`, sinh 5000 bản ghi, ghi lại số ms
3. Xoá dữ liệu: `DELETE FROM chi_tiet_su_dung;`
4. Thêm lại tham số, khởi động lại, sinh 5000 bản ghi, so sánh

Không cần sửa file cũng đo được bằng cách ghi đè biến môi trường:

```bash
set SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/vnpt_billing?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Ho_Chi_Minh&createDatabaseIfNotExist=true
```

---

# PHẦN II — TRUY VẤN THỐNG KÊ CỦA BÁO CÁO (PHASE 6)

## 8. Bài toán và cách tiếp cận

Phase 6 thêm một dashboard và bảy báo cáo, tất cả đều là **truy vấn gom nhóm** trên hai bảng
lớn nhất: `chi_tiet_su_dung` (18.723 dòng) và `hoa_don` (280 dòng).

Quyết định kiến trúc đặt ra từ đầu (yêu cầu D.4): **gom số ngay trong CSDL** bằng
`SELECT new ... + GROUP BY`, không load entity lên rồi cộng trong Java. Với quy mô hiện tại hai
cách đều nhanh; nhưng cách sau tăng tuyến tính theo số bản ghi và sẽ hỏng **lặng lẽ** khi dữ
liệu lớn lên — đúng lúc không ai còn nhớ vì sao nó được viết như vậy.

## 9. ⭐ Đo trước khi thêm index

Yêu cầu D.5 là *"thêm index nếu query chậm"*. Điều kiện **nếu** đó quan trọng: thêm index vô
tội vạ làm chậm mọi thao tác ghi và phình dung lượng, đổi lấy một cải thiện không đo được.

### 9.1. Thời gian đáp ứng thực tế

Đo bằng `Measure-Command` quanh một lượt tải trang trọn vẹn (gồm truy vấn, dựng model và
render Thymeleaf), sau khi ứng dụng đã khởi động xong. Mỗi trang đo 1 lần "nguội" và 5 lần
liên tiếp lấy trung bình.

| Màn hình | Lần 1 | Trung bình 5 lần |
|---|---|---|
| Dashboard (4 thẻ + 3 biểu đồ + 2 bảng) | 137 ms | **113 ms** |
| C.1 Doanh thu theo kỳ | 74 ms | 71 ms |
| C.2 Doanh thu theo gói cước | 40 ms | 49 ms |
| C.3 Doanh thu theo loại dịch vụ | 48 ms | 46 ms |
| C.4 Thống kê thuê bao | 67 ms | 66 ms |
| C.5 Top 50 thuê bao cước cao | 57 ms | 56 ms |
| C.6 Sản lượng dịch vụ | 48 ms | 48 ms |
| C.7 Công nợ tổng hợp | 53 ms | 50 ms |

Dashboard chậm nhất là hợp lý — nó chạy **sáu** truy vấn trong một lượt tải, còn mỗi báo cáo
chỉ chạy một tới hai.

### 9.2. Truy vấn nặng nhất đã có index sẵn

Truy vấn đáng ngại nhất là sản lượng dịch vụ: nó quét CDR của một kỳ với sáu phép
`SUM(CASE WHEN ...)`. `EXPLAIN`:

```
type  = ref            <-- không phải ALL
key   = fk_cdr_ky_cuoc
rows  = 4000
Extra = Using index
```

Cột `ky_cuoc_id` **đã có index** — nhưng không phải do ai chủ ý tạo cho báo cáo: MySQL tự sinh
index cho mọi ràng buộc khóa ngoại, và `fk_cdr_ky_cuoc` có từ Phase 1. Nói cách khác, truy vấn
nặng nhất của Phase 6 được cứu bởi một quyết định thiết kế CSDL từ năm phase trước.

### 9.3. Chỗ duy nhất còn quét toàn bảng

```
EXPLAIN SELECT COUNT(*) FROM hoa_don WHERE con_no > 0;
  type = ALL,  rows = 280,  key = NULL
```

Bảng `hoa_don` **không có index trên `con_no`**, nên truy vấn công nợ quét toàn bảng.

**Quyết định: KHÔNG thêm index.** Lý do:

1. 280 dòng nằm gọn trong một vài trang dữ liệu; quét toàn bảng ở quy mô này nhanh hơn đi qua
   index rồi quay lại đọc dòng.
2. `con_no > 0` đúng với **165/280 dòng — 59%**. Index có tính chọn lọc kém như vậy thường bị
   chính bộ tối ưu của MySQL bỏ qua, nên nhiều khả năng nó chỉ tồn tại để làm chậm mọi lần ghi
   vào `hoa_don`.
3. Toàn bộ màn hình công nợ đáp ứng trong **50 ms**. Không có gì để cải thiện mà đo được.

> **Ngưỡng để xem lại:** nếu `hoa_don` vượt khoảng 100.000 dòng, hoặc thời gian màn hình công
> nợ vượt 500 ms, thì đo lại và cân nhắc index trên `(con_no)` hoặc
> `(ky_cuoc_id, con_no)`. Ghi ngưỡng ra đây để lần sau không phải đoán lại.

## 10. Nhận xét

1. **"Đo trước, tối ưu sau" áp dụng cả cho việc KHÔNG tối ưu.** Kết luận của Phần II là *không
   thêm gì cả* — nhưng nó là một kết luận có số liệu chống lưng, khác hẳn với việc quên mất.

2. **Index hữu ích nhất của báo cáo là thứ không ai tạo cho báo cáo.** `fk_cdr_ky_cuoc` sinh ra
   từ ràng buộc khóa ngoại ở Phase 1. Thiết kế CSDL chặt chẽ trả cổ tức ở những phase mà lúc
   viết chưa ai hình dung.

3. **Phần I và Phần II cho hai kết luận trái ngược nhau, và cả hai đều đúng.** Phần I tìm ra
   một tối ưu đáng giá 4,5 lần; Phần II kết luận không cần tối ưu gì. Cái chung là **cách làm**:
   đo, so sánh có chủ đích, rồi mới quyết.
