# CLAUDE.md — bối cảnh dự án

## Dự án là gì

Đồ án môn **Thực tập nghề nghiệp**: phần mềm **"Quản lý thuê bao và tính cước điện thoại"**.
Mô phỏng nghiệp vụ viễn thông: khách hàng → thuê bao → gói cước → bảng giá → CDR → tính cước
→ hóa đơn → thanh toán → báo cáo.

**Sản phẩm cuối cùng nộp giảng viên là BÁO CÁO**, không phải mã nguồn. Tài liệu trong `docs/`
quan trọng ngang mã nguồn — sửa code mà không cập nhật tài liệu là làm hỏng một nửa sản phẩm.

> ⚠️ **Dữ liệu:** toàn bộ là **dữ liệu mẫu tự sinh** phục vụ học tập. Hệ thống **không** dùng
> dữ liệu thật của bất kỳ nhà mạng nào. Tên "VNPT" chỉ là bối cảnh giả định.

## Stack

Java (máy dev chạy **JDK 25**, biên dịch ở mức `release 21`) · Spring Boot **3.5.16**
(Web MVC, Data JPA, Security, Validation) · MySQL **8** · Thymeleaf · Bootstrap 5.3.3 (CDN) ·
Chart.js 4.4.2 · Apache POI 5.4.1 · Lombok 1.18.46 · Maven Wrapper.

Đặt tên: tiếng Việt **không dấu** — `snake_case` cho CSDL, `camelCase` cho Java.

## Lệnh hay dùng

Chạy thường (giữ nguyên dữ liệu — `spring.sql.init.mode=never`):

```bash
mvnw spring-boot:run
```

Nạp lại dữ liệu mẫu — **XOÁ SẠCH CSDL** (`schema.sql` mở đầu bằng `DROP TABLE`):

```bash
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

Chạy test (148 test, cần MySQL đang chạy):

```bash
mvnw test
```

Kết nối MySQL đọc từ biến môi trường `MYSQL_USER` (mặc định `root`) và `MYSQL_PASSWORD` —
**không** ghi mật khẩu vào mã nguồn.

## ⚠️ Bẫy môi trường đã gặp

* **Dừng app trước khi `mvnw test`.** Biên dịch lại `target/` làm DevTools restart; ở profile
  `reset` thì mỗi lần restart là `schema.sql` chạy lại → mất sạch dữ liệu. Từ 4B đã chặn hẳn
  bằng `spring.devtools.restart.enabled: false` trong `application-reset.yml`, nhưng thói quen
  dừng app trước khi test vẫn đúng vì test chạy trên CSDL thật.
* **Đường dẫn dự án phải thuần ASCII.** Có dấu tiếng Việt thì `spring-boot:run` báo
  `Could not find or load main class` — `java` đọc argfile bằng bảng mã ANSI trước khi JVM khởi
  động, không tham số nào sửa được.
* **Lombok phải khai `annotationProcessorPaths` tường minh** trong `maven-compiler-plugin`.
  Từ JDK 23, javac không tự bật annotation processing khi chỉ thấy processor trên classpath —
  không khai thì Lombok im lặng không sinh gì.
* **File `.ps1` có tiếng Việt phải lưu kèm BOM UTF-8**, nếu không PowerShell đọc sai bảng mã.

## Quy ước nghiệp vụ tuyệt đối không được vi phạm

* **Tiền dùng `BigDecimal`, `HALF_UP` scale 0.** Tầng làm tròn **DUY NHẤT** là tầng CDR; các
  mức trên chỉ cộng dồn, không làm tròn lại. Làm tròn nhiều tầng ⇒ `SUM(cuoc_phi) ≠ hoa_don.cuoc_thoai`.
* **Ba chỗ quy đổi đơn vị** (gom trong `service/rating/DonViCuoc.java`): giây↔phút (×60),
  KB↔MB (×1024), và **quota ưu đãi phải quy XUỐNG đơn vị bản ghi**, không quy bản ghi LÊN đơn vị
  quota. Quy lên làm tròn từng bản ghi rồi cộng dồn ⇒ thổi phồng sản lượng **+10,97%** và thu tiền
  oan của khách. Chi tiết: `docs/mo-ta-csdl.md` mục 6, `docs/PHASE-4-REPORT.md` mục 23.
* **Không cắt đôi bản ghi khi vượt quota** (quyết định 5.3). Hệ quả cố hữu: kết quả **phụ thuộc
  thứ tự** ⇒ mọi truy vấn duyệt CDR **bắt buộc** `ORDER BY thoi_gian_bat_dau, id`.
* **`UNIQUE(thue_bao_id, ky_cuoc_id)` trên `hoa_don`** là lớp chống hóa đơn trùng ở CSDL — vẫn
  phải chặn thêm ở tầng nghiệp vụ, đừng bỏ.
* **Bảng đối soát CHỈ ĐỌC LẠI, KHÔNG TÍNH LẠI.** `DoiSoatCuocService` lấy thẳng số đã ghi. Tự
  tính lại theo cách riêng thì nó chỉ chứng minh chính nó, và tạo ra nguồn sự thật thứ hai.

## Ba ràng buộc bắt buộc cho cả Phase 5 (`docs/PHASE-5-PLAN.md`)

1. **Làm tròn ở đúng MỘT tầng.** `giam_tru.ty_le_phan_tram` nhân rồi làm tròn là tầng làm tròn
   thứ hai ⇒ vi phạm quyết định 5.8. Cách xử lý: quy tỉ lệ thành **số tiền tuyệt đối đúng một
   lần** lúc lập hóa đơn, ghi vào `hoa_don.giam_tru`, snapshot vào `chi_tiet_hoa_don`; từ đó về
   sau **chỉ cộng trừ, không nhân**. Sửa `giam_tru` phải tính lại **toàn bộ** chuỗi
   `tong_truoc_thue → thue_vat → tong_thanh_toan → con_no` — cấm sửa mỗi `con_no`. Cộng dồn
   thanh toán luôn dùng `BigDecimal`.
2. **Một nguồn sự thật cho "còn nợ".** `hoa_don.con_no` là chỗ ghi DUY NHẤT và chỉ ghi trong
   service. Mọi màn hình (chi tiết, danh sách, công nợ, aging) chỉ **ĐỌC** cột đó — tuyệt đối
   không tự tính `tong_thanh_toan − SUM(thanh_toan)` trên view. Test bất biến chạy sau **mỗi
   mục**: với MỌI hóa đơn, `con_no = tong_thanh_toan − da_thanh_toan` VÀ
   `da_thanh_toan = SUM(thanh_toan.so_tien)` → 0 dòng lệch.
3. **Thứ tự cố định khi trừ số dư.** Trừ cước trả trước lặp lại nguyên tình huống quyết định
   5.3: quỹ cạn dần, bản ghi làm cạn quỹ **KHÔNG được cắt đôi**. Bắt buộc
   `ORDER BY thoi_gian_bat_dau, id`; chạy hai lần phải ra cùng kết quả.

## Sáu chuẩn làm việc lập ở Phase 4 (`PHASE-4-REPORT.md` mục 43)

1. **Kiểm bất biến, đừng kiểm luật cụ thể** — tiêu chí viết sau khi hiểu vấn đề chỉ bắt được
   phần đã hiểu.
2. **Kiểm toàn bộ, đừng lấy mẫu** — khi đã viết được câu SQL thì chi phí gần như bằng nhau.
3. **Kiểm từng dòng, đừng kiểm số tổng** — hai sai lệch ngược dấu triệt tiêu nhau ở mức tổng.
4. **Công bố dự đoán TRƯỚC khi viết code** — số lệch là tín hiệu dừng lại phân tích, không phải
   tín hiệu sửa cho khớp.
5. **Một phép kiểm sai nguy hiểm ngang thiếu phép kiểm** — báo động giả làm hỏng lòng tin vào
   mọi phép kiểm còn lại.
6. **Đối soát phải đọc lại, không được tính lại.**

## Cách làm việc mong đợi

* Commit sau mỗi mục nhỏ (`Phase 4A`, `4B`, …), thông điệp commit tiếng Việt không dấu.
* Viết `docs/PHASE-x-REPORT.md` ở cuối mỗi phase: những gì đã làm, **điểm sai lệch đặc tả**,
  bài học, số liệu nghiệm thu, danh sách màn hình cần chụp ảnh.
* **Phát hiện đặc tả mâu thuẫn thì NÊU RA và đề xuất, không im lặng làm theo.** Phase 4 bắt được
  6 điểm như vậy — đó là phần có giá trị nhất của báo cáo.
* Đo trên dữ liệu thật trước khi quyết định, không suy luận suông.

## Tiến độ

Phase 0–4 ✅ · **đang vào Phase 5**.

| Phase | Nội dung | Báo cáo |
|---|---|---|
| 0 | Khung dự án | `docs/PHASE-0-REPORT.md` |
| 1 | CSDL: 15 bảng + 2 view | `docs/mo-ta-csdl.md` |
| 2 | Xác thực, khách hàng, thuê bao | `docs/PHASE-2-REPORT.md` |
| 3 | Gói cước, bảng giá, CDR, kỳ cước | `docs/PHASE-3-REPORT.md` |
| 4 | Engine tính cước (rating + billing) | `docs/PHASE-4-PLAN.md` · `docs/PHASE-4-REPORT.md` |
| 5 | Hóa đơn, thanh toán, công nợ | 🔄 `docs/PHASE-5-PLAN.md` · `docs/PHASE-5-REPORT.md` |
| 6 | Báo cáo, thống kê, dashboard | ⏳ |
| 7 | Hoàn thiện, kiểm thử, tài liệu | ⏳ |

**Dữ liệu hiện tại:** 8.714 CDR (tất cả `DA_TINH`) · 112 hóa đơn (54 kỳ 5 + 58 kỳ 6, tất cả
`CHUA_TT`) · 264 chi tiết hóa đơn · 34 dòng `bien_dong_so_du` (18 mở sổ + 16 trừ cước kỳ 6) ·
`thanh_toan`/`giam_tru` vẫn rỗng · kỳ 5/2026 `DA_CHOT`, kỳ 6/2026 `MO`.

**Phase 5 đã làm** (mục A–F chưa được giao): A0 rà số liệu tài liệu · G1 sổ cái
`bien_dong_so_du` (đổi tên từ `nap_tien`, +18 dòng mở sổ `DIEU_CHINH`) · G2
`TruCuocTraTruocService` + hoàn tác · G3 dự đoán 7/7 đúng · G4 giao diện. Kỳ 6 đã trừ
**1.991.417 đ** cho 16 thuê bao; kỳ 5 **chưa** trừ và sẽ bị chặn vì không giao hoán.

**Còn nợ từ Phase 4:** chuyển 54 hóa đơn quá hạn kỳ 5 (hạn 15/06/2026) sang `QUA_HAN`; và
`huyBillingKy` từ chối xoá hóa đơn khi kỳ đã có thanh toán ⇒ tạo thanh toán ở **kỳ 5** trước,
giữ kỳ 6 linh hoạt để demo.

**Ràng buộc mới sinh ra ở mục G — đừng phá:**

* Bất biến sổ cái `so_du = SUM(nạp + điều chỉnh) − SUM(trừ)` đúng với **mọi** thuê bao;
  `KiemTraSoCaiSoDuTest` kiểm sau mỗi thay đổi. Sửa thẳng `thue_bao.so_du` mà không ghi sổ là vi phạm.
* Quy tắc dấu chỉ nằm trong enum `LoaiBienDongSoDu`, không chép ra chỗ khác.
* Trừ cước **không giao hoán theo kỳ**; `huyRatingKy` bị chặn khi kỳ đã trừ cước.
