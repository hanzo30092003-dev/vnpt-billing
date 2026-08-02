# TỐI ƯU HIỆU NĂNG — GHI HÀNG LOẠT BẢN GHI CDR

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại
**Phạm vi:** Bộ sinh CDR giả lập (`CdrGeneratorService`) — Phase 3
**Ngày đo:** 02/08/2026

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
