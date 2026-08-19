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

Chạy thường (Flyway chỉ chạy file di trú **chưa từng chạy**, dữ liệu giữ nguyên):

```bash
mvnw spring-boot:run
```

Nạp lại dữ liệu mẫu — **XOÁ SẠCH CSDL** (`flyway clean` → `migrate` → nạp 2 file dữ liệu,
xem `FlywayResetConfig`):

```bash
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

Chạy test (307 test, cần MySQL đang chạy):

```bash
mvnw test
```

> Từ Phase 5 mục F, `reset` **tái lập đúng** bộ dữ liệu mà báo cáo mô tả chứ không còn xoá
> sổ nó: `db/data-van-hanh.sql` chứa bản dump của CDR, hóa đơn, thanh toán và sổ cái. File đó
> do máy sinh — **sửa tay là sai**, phải dump lại từ CSDL.

Kết nối MySQL đọc từ biến môi trường `MYSQL_USER` (mặc định `root`) và `MYSQL_PASSWORD` —
**không** ghi mật khẩu vào mã nguồn.

## ⚠️ Bẫy môi trường đã gặp

* **Dừng app trước khi `mvnw test`.** Biên dịch lại `target/` làm DevTools restart; ở profile
  `reset` thì mỗi lần restart là `flyway clean` chạy lại → mất sạch dữ liệu. Từ 4B đã chặn hẳn
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

## Bảy chuẩn làm việc lập ở Phase 4 (`PHASE-4-REPORT.md` mục 43)

1. **Kiểm bất biến, đừng kiểm luật cụ thể** — tiêu chí viết sau khi hiểu vấn đề chỉ bắt được
   phần đã hiểu.
2. **Kiểm toàn bộ, đừng lấy mẫu** — khi đã viết được câu SQL thì chi phí gần như bằng nhau.
3. **Kiểm từng dòng, đừng kiểm số tổng** — hai sai lệch ngược dấu triệt tiêu nhau ở mức tổng.
4. **Công bố dự đoán TRƯỚC khi viết code** — số lệch là tín hiệu dừng lại phân tích, không phải
   tín hiệu sửa cho khớp.
5. **Một phép kiểm sai nguy hiểm ngang thiếu phép kiểm** — báo động giả làm hỏng lòng tin vào
   mọi phép kiểm còn lại.
6. **Đối soát phải đọc lại, không được tính lại.**
7. **Dữ liệu thử thiết kế theo một chiều chỉ đúng theo chiều đó** — muốn dựng một tình huống
   phải kiểm mọi điều kiện cần cùng xảy ra trên **cùng một** bản ghi.

## Cách làm việc mong đợi

* Commit sau mỗi mục nhỏ (`Phase 4A`, `4B`, …), thông điệp commit tiếng Việt không dấu.
* Viết `docs/PHASE-x-REPORT.md` ở cuối mỗi phase: những gì đã làm, **điểm sai lệch đặc tả**,
  bài học, số liệu nghiệm thu, danh sách màn hình cần chụp ảnh.
* **Phát hiện đặc tả mâu thuẫn thì NÊU RA và đề xuất, không im lặng làm theo.** Phase 4 bắt được
  6 điểm như vậy — đó là phần có giá trị nhất của báo cáo.
* Đo trên dữ liệu thật trước khi quyết định, không suy luận suông.

## Tiến độ

Phase 0–8 ✅. **Đang chạy đợt hoàn thiện** theo
[`docs/KE-HOACH-HOAN-THIEN.md`](docs/KE-HOACH-HOAN-THIEN.md) — xem mục *Tiến độ* ở cuối file
đó để biết việc nào xong, việc nào tiếp theo.

| Phase | Nội dung | Báo cáo |
|---|---|---|
| 0 | Khung dự án | `docs/PHASE-0-REPORT.md` |
| 1 | CSDL: 15 bảng + 2 view | `docs/mo-ta-csdl.md` |
| 2 | Xác thực, khách hàng, thuê bao | `docs/PHASE-2-REPORT.md` |
| 3 | Gói cước, bảng giá, CDR, kỳ cước | `docs/PHASE-3-REPORT.md` |
| 4 | Engine tính cước (rating + billing) | `docs/PHASE-4-PLAN.md` · `docs/PHASE-4-REPORT.md` |
| 5 | Hóa đơn, thanh toán, công nợ | `docs/PHASE-5-PLAN.md` · `docs/PHASE-5-REPORT.md` |
| 6 | Báo cáo, thống kê, dashboard | `docs/PHASE-6-REPORT.md` |
| 7 | Hoàn thiện, kiểm thử, tài liệu | `docs/PHASE-7-REPORT.md` |
| 8 | Làm lại giao diện cho người không rành công nghệ | `docs/PHASE-8-REPORT.md` |

**Dữ liệu hiện tại:** **6 kỳ cước** (3, 4, 5/2026 `DA_CHOT` · 6, 7, 8/2026 `MO`; **kỳ 8 rỗng có chủ đích**) · 18.723 CDR (tất
cả `DA_TINH`) · **280 hóa đơn** (55 · 55 · 54 · 58 · 58) · 620 chi tiết hóa đơn · **161 thanh
toán** (kỳ 3: 58 · kỳ 4: 55 · kỳ 5: 48 · kỳ 6–8: **0**) · 34 dòng `bien_dong_so_du` · 2 giảm trừ.

Tiền: doanh thu **111.513.012 đ**, đã thu **49.190.687 đ**, còn nợ **62.322.325 đ** (44,1%).
Chi tiết bàn giao: `PHASE-6-REPORT.md` mục 14.

**Hạt giống CDR** (thứ duy nhất dựng lại được dữ liệu nếu mất): kỳ 3 `20260300` · kỳ 4
`20260400` · kỳ 7 `20260700`. Kỳ 5 và 6 sinh trước khi có tham số hạt giống nên **chỉ còn bản
dump** `data-van-hanh.sql`.

**Ràng buộc sinh ra ở Phase 5 — đừng phá:**

* Bất biến sổ cái `so_du = SUM(nạp + điều chỉnh) − SUM(trừ)` đúng với **mọi** thuê bao;
  `KiemTraSoCaiSoDuTest` kiểm sau mỗi thay đổi. Sửa thẳng `thue_bao.so_du` mà không ghi sổ là vi phạm.
* Bất biến thanh toán `con_no = tong_thanh_toan − da_thanh_toan` và
  `da_thanh_toan = SUM(thanh_toan.so_tien)` đúng với **mọi** hóa đơn;
  `KiemTraBatBienThanhToanTest` kiểm. `ThanhToanService` là nơi ghi **duy nhất**.
* Quét quá hạn **chỉ chạm hóa đơn chưa thu đồng nào**. Bỏ điều kiện đó thì `TT_MOT_PHAN` thành
  trạng thái không thể tồn tại sau ngày hết hạn — xem báo cáo mục 23.2.
* Quy tắc dấu chỉ nằm trong enum `LoaiBienDongSoDu`, không chép ra chỗ khác.
* Trừ cước **không giao hoán theo kỳ**; `huyRatingKy` bị chặn khi kỳ đã trừ cước.
* **Kỳ 8/2026 phải giữ RỖNG** — đó là kỳ demo trực tiếp và là kỳ kiểm "màn hình chịu được kỳ rỗng".
* **Kỳ 6 và kỳ 7 phải giữ 0 giao dịch thanh toán.** Có thanh toán là `huyBillingKy` từ chối xoá
  hóa đơn, và mất luôn hai kỳ còn demo được trọn vòng huỷ → lập lại.
* `db/data-van-hanh.sql` là **bản dump**, không phải file soạn tay — sửa qua service rồi dump lại.
* **Quét quá hạn chỉ chạm hóa đơn chưa thu đồng nào** — bỏ điều kiện đó thì `TT_MOT_PHAN` thành
  trạng thái không thể tồn tại sau hạn.
* Mọi truy vấn thống kê **gom nhóm trong CSDL** (`SELECT new` + `GROUP BY`), không load entity
  rồi cộng trong Java. Định dạng số đi qua bean `soLieu` (`DinhDangTien`), không viết lặp.
* **Chữ hiển thị đã bỏ hết thuật ngữ** (đợt Phase 8). Trước khi thêm chữ mới vào template,
  chạy `python scripts/kiem-tu-ngu.py` và `python scripts/kiem-giao-dien.py`. Tên biến, đường
  dẫn, tên lớp CSS thì giữ nguyên — hai phép kiểm đó đã biết bỏ qua chúng.
* **Mỗi màn hình đúng MỘT nút nổi bật.** Muốn phá luật thì khai `NUT-NOI-BAT-CO-Y:` kèm lý do
  ngay trong template, đừng sửa file kiểm thử.
* **`HoaDon.phienBan` (`@Version`) không được bỏ.** Thiếu nó, hai người cùng thu tiền một hóa
  đơn làm mất một lần cộng — đã dựng lại được bằng
  `KiemTraDongThoiThanhToanTest.epDocDocGhiGhi_benGhiSauBiTuChoi`. Lưu ý phép kiểm 12 luồng
  trong cùng lớp đó **không** dựng lại được lỗi; chỉ phép ép thứ tự đọc–đọc–ghi–ghi mới bắt.
* **`NhatKyServiceImpl` không được tin `X-Forwarded-For` lại.** Hệ thống chạy không proxy nên
  header đó do người gửi tự khai; nó từng phá được **mọi** đường ghi (cột 45 ký tự + ghi nhật
  ký chung giao dịch với nghiệp vụ). Xem javadoc của `layDiaChiIp`.
* **Phân hệ quản trị luôn phải còn ít nhất MỘT quản trị viên đang hoạt động.** Đây là bất
  biến duy nhất mà vi phạm thì không sửa được bằng chính phần mềm — khoá hoặc hạ quyền quản
  trị viên cuối cùng là mất luôn màn hình dùng để sửa. Cùng nhóm: không tự khoá và không tự
  đổi quyền của chính mình. `NguoiDungServiceTest` kiểm cả ba, kèm đối chứng.
* **Khoá tài khoản phải đá được phiên đang mở**, không chỉ chặn lần đăng nhập sau. Cần
  `SessionRegistry` khai tường minh trong `SecurityConfig` (sổ nội bộ của `maximumSessions(1)`
  không lấy ra được). Bỏ `.sessionRegistry(...)` là nút Khoá thành khoá trên giấy mà mọi test
  Mockito vẫn xanh — chỉ `test-auth.ps1` mục 9.5 bắt được.
* **Tên đăng nhập không sửa được sau khi tạo** — nó đã ký trong sổ nhật ký. Mật khẩu để trống
  lúc sửa nghĩa là giữ nguyên, không phải xoá.
* **Đổi mật khẩu phải nhập đúng mật khẩu hiện tại**, dù người dùng đã đăng nhập rồi. Một phiên
  đang mở không chứng minh người ngồi trước máy là chủ tài khoản.
* **Mọi đường đổi mật khẩu đều làm phiên đang mở của tài khoản đó hết giá trị** — cả tự đổi
  (`/doi-mat-khau`) lẫn quản trị viên đặt lại hộ. Cùng luật với nút Khoá: thông tin xác thực
  đổi thì phiên dựng trên thông tin cũ không còn giá trị. `test-auth.ps1` mục 10 canh việc này.
* **`/doi-mat-khau` KHÔNG được đặt dưới `/quan-tri/**`** — nhân viên quầy và kế toán cũng phải
  đổi được mật khẩu khởi tạo của họ.
* **Báo cáo phân quyền theo NỘI DUNG, không theo tiền tố đường dẫn.** Báo cáo có số tiền của
  khách → `KE_TOAN` + `ADMIN`, cùng luật với `/hoa-don` và `/cong-no`. Nới lại thành cả cụm
  `/bao-cao/**` là mở lại đúng lỗ hổng cũ. `test-auth.ps1` mục 8 canh việc này.
* **`han_muc_tin_dung` đã hết là cột chết** — `HoaDonRepository.timThueBaoVuotHanMuc()` dùng nó.
  Hạn mức `0` nghĩa là **chưa đặt**, không phải "không cho nợ đồng nào".
* **Bảng aging đủ 5 nhóm chỉ đúng tới 13/08/2026** — đó là tính chất của ngày xem chứ không phải
  của dữ liệu. Xem `PHASE-6-REPORT.md` mục 1.2 trước khi tưởng có gì hỏng.
