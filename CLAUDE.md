# CLAUDE.md — bối cảnh dự án

## Dự án là gì

Đồ án môn **Thực tập nghề nghiệp**: phần mềm **"Quản lý thuê bao và tính cước điện thoại"**.
Mô phỏng nghiệp vụ viễn thông: khách hàng → thuê bao → gói cước → bảng giá → CDR → tính cước
→ hóa đơn → thanh toán → báo cáo.

**Sản phẩm cuối cùng nộp giảng viên là BÁO CÁO**, không phải mã nguồn. Tài liệu trong `docs/`
quan trọng ngang mã nguồn — sửa code mà không cập nhật tài liệu là làm hỏng một nửa sản phẩm.

> ⚠️ **Dữ liệu:** toàn bộ là **dữ liệu mẫu tự sinh** phục vụ học tập. Hệ thống **không** dùng
> dữ liệu thật của bất kỳ nhà mạng nào. Đơn vị phát hành hóa đơn là **Công ty Cổ phần Viễn
> thông Sông Hậu** — một doanh nghiệp **hư cấu** dựng riêng cho đồ án; mã số thuế và số tổng
> đài trên chứng từ cũng là số bịa. Tên thư mục và gói Java vẫn giữ chữ `vnpt` vì đó là tên
> kỹ thuật có từ đầu, không hiện ra cho người dùng.

## Stack

Java (máy dev chạy **JDK 25**, biên dịch ở mức `release 21`) · Spring Boot **3.5.16**
(Web MVC, Data JPA, Security, Validation) · MySQL **8** · Thymeleaf · Bootstrap 5.3.3 (CDN) ·
Chart.js 4.4.2 · Apache POI 5.4.1 · Lombok 1.18.46 · Maven Wrapper.

Đặt tên: tiếng Việt **không dấu** — `snake_case` cho CSDL, `camelCase` cho Java.

## Lệnh hay dùng

Có `chay.cmd` ở gốc dự án gói sẵn ba việc hay làm. Nó kiểm cổng 8080 trước — thứ hay làm mất
thời gian nhất là một bản chạy cũ chưa tắt, Spring Boot báo *Port 8080 was already in use*
rồi dừng hẳn:

```bash
chay
```

| Lệnh | Làm gì | Đo được |
|---|---|---|
| `chay` | Phát triển — biên dịch lại, có DevTools tự nạp lại | **~8–10 giây** |
| `chay demo` | Chạy thẳng từ bản đóng gói, **không** biên dịch lại | **~6 giây** |
| `chay reset` | **XOÁ SẠCH CSDL** rồi nạp lại dữ liệu mẫu (hỏi xác nhận trước) | |

`chay demo` nhanh hơn vì bỏ được phần Maven (~4 giây). Đổi lại nó không biên dịch, nên chỉ
dùng khi mã nguồn đang đứng yên — lúc chụp ảnh, lúc demo. Nếu jar cũ hơn `src/` thì script
**nói rõ file nào mới hơn** chứ không tự đóng gói lại: đóng gói mất 8 giây, cộng 6 giây chạy
là 14 giây, chậm hơn cả chế độ phát triển.

Số giây đo bằng cách chạy xen kẽ 3 lượt mỗi cách, tính từ lúc gõ lệnh tới lúc `/dang-nhap`
trả về 200 — không phải con số `Started BillingApplication` mà JVM tự báo (nó bỏ qua phần
Maven ở đầu).

Vẫn gọi tay được nếu muốn:

```bash
mvnw spring-boot:run
```

```bash
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

Chạy test (315 test, cần MySQL đang chạy):

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
* **`mvnw` phải giữ bit thực thi trong git** (`git ls-files -s mvnw` → `100755`). Windows không
  lộ ra vì Git Bash gọi script qua `sh`, nhưng trên Linux `./mvnw` chết ngay với *Permission
  denied*. Chỉ CI bắt được — phép kiểm clone chạy trên Windows vẫn đạt.

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
* **Nút chỉ có biểu tượng phải có `aria-label`** — `title` một mình không đủ: nhiều trình đọc
  màn hình bỏ qua nó, và người dùng bàn phím không bao giờ thấy nó. Thêm nút mới thì chạy
  `python scripts/kiem-ban-phim.py`.
* **Đường tắt "Bỏ qua menu" trong `layout.html` không được bỏ**, và đích `#noi-dung` phải giữ
  `tabindex="-1"` — thiếu nó thì bấm đường tắt chỉ cuộn màn hình chứ tiêu điểm không nhảy vào.
  Đo được: không có đường tắt thì phải bấm Tab **20 lần** mới tới ô nhập đầu tiên, ở mọi trang.
* **Đơn vị phát hành chứng từ là công ty HƯ CẤU "Công ty Cổ phần Viễn thông Sông Hậu" —
  không được đổi về tên một nhà mạng có thật.** Đợt G1c gỡ hết dòng cảnh báo *"dữ liệu mẫu"*
  cho tờ hóa đơn trông chuyên nghiệp; làm được điều đó **chỉ vì** bên phát hành là công ty
  bịa, nên tờ giấy không mạo danh ai và không có gì phải cảnh báo. Đổi ngược về "VNPT" mà vẫn
  giữ tình trạng không cảnh báo là biến nó thành hóa đơn giả hoàn chỉnh của một doanh nghiệp
  có thật — mà hóa đơn điện thoại ở Việt Nam hay được dùng làm giấy chứng minh nơi cư trú.
  Mã số thuế `1800000000` và tổng đài `1800 6060` cũng là số bịa; tổng đài thật của VNPT là
  `1800 1166`, đừng chép lại. Ba phép kiểm canh cả hai chiều (`contains` tên hư cấu +
  `doesNotContain("VNPT")`): `HoaDonPdfServiceTest`, `PhieuThuPdfServiceTest`,
  `PhieuThuPdfTaiLieuThatTest`.
* **Chân trang PDF ghi SỐ HIỆU chứng từ, không bỏ trống.** Hóa đơn có thể dài hơn một trang;
  một tờ rời khỏi tập thì số hiệu ở chân trang là thứ duy nhất nói nó thuộc về đâu.
* **`--thanh-may-cao` KHÔNG được để JavaScript ghi đè.** `.thanh-may` đọc biến đó làm
  `min-height`, nên ghi chiều cao đo được ngược vào nó là tạo vòng phản hồi: thanh cao lên một
  lần (ví dụ lúc cửa sổ hẹp làm chữ xuống dòng) thì `min-height` khoá luôn ở đó và **không bao
  giờ co lại**. Đã dính đúng lỗi này một lần — thanh máy bị khoá ở 375px, chiếm gần nửa khung.
  Chiều cao đo được đi vào **`--thanh-may-thuc`**, một biến khác, và chỉ rail đọc nó.
* **Phép đo `--thanh-may-thuc` phải neo vào `document.fonts.ready`, không chỉ `ResizeObserver`.**
  Lúc `app.js` chạy, Be Vietnam Pro chưa tải xong nên thanh máy đo được 58px; tải xong nó cao
  73px. `ResizeObserver` đáng lẽ bù lại, nhưng nó **chỉ chạy khi trình duyệt còn chạy vòng vẽ** —
  trong khung xem tích hợp thì không chạy lần nào. Lệch 15px nghĩa là mục "Trang chủ" trên rail
  chui xuống dưới thanh máy.
* **Không có chữ giao diện thường trực nào dưới 13px; nhãn gắn với một con số thì ≥14px.**
  Bản đầu của đợt G1 để nhãn bốn con số ở `.62rem` VIẾT HOA — đo ra **9,92px**, và nhãn đó là
  thứ duy nhất nói con số bên cạnh nghĩa là gì. Tiếng Việt có dấu viết hoa ở cỡ nhỏ khó đọc hơn
  hẳn viết thường, nên đừng dùng `text-transform: uppercase` để bù cho chữ nhỏ.
* **Bảng màu chỉ có MỘT nguồn: `:root` trong `app.css`.** Biểu đồ đọc qua `window.MAU` khai ở
  `app.js` (đọc thẳng từ biến CSS), **không gõ mã màu trong template**. Trước đợt G1, bảy màn
  hình có biểu đồ mỗi cái tự gõ một bộ — đổi bảng màu là quên đúng 4 chỗ.
* **Thang màu tuổi nợ nằm trong enum `NhomTuoiNo`, và mọi bậc phải đạt AA 4,5:1.** Đây là chỗ
  `kiem-giao-dien.py` **không nhìn tới** vì nó đọc template chứ không đọc mã Java — bậc
  "31–60 ngày" từng là chữ trắng trên `#fd7e14` = **2,57:1** và sống sót qua cả Phase 8. Thêm
  màu hiển thị vào mã Java là đưa nó ra khỏi tầm canh của phép kiểm, phải tự đo tay.
* **Đèn tín hiệu trên thanh máy dùng bộ màu riêng (`--den-*`), không dùng lại `--tin-*`.**
  Ba màu `--tin-*` chọn để đọc trên nền TRẮNG; đặt lên nền mực chúng chỉ còn 2,3–2,9:1.
* **`--tin-canh` không được làm sáng lên.** Nó còn làm viền tiêu điểm của đường tắt "Bỏ qua
  menu"; sáng hơn là viền đó tụt xuống dưới 3:1 trên nền trắng. Nền cảnh báo đi với **chữ
  đen** (4,95:1), chữ trắng chỉ được 4,24:1.
* **`thanh-may-hieu` là dấu hiệu "trang này có khung vỏ"** mà `test-auth.ps1` mục 1 và mục 5
  dựa vào. Đổi tên lớp đó phải sửa cả hai chỗ trong script — và lưu ý phép kiểm mục 1 khẳng
  định điều **phủ định** (trang đăng nhập không được có), nên đổi tên mà quên sửa thì nó xanh
  vĩnh viễn chứ không đỏ. Dựng đối chứng bằng tên **không chứa** chuỗi cũ: `.Contains()` vẫn
  khớp chuỗi con.
* **Hộp xác nhận phải trả tiêu điểm về nút đã mở nó** (`hidden.bs.modal` trong `app.js`).
  Bootstrap tự làm việc này khi modal mở bằng `data-bs-toggle`, nhưng ở đây modal mở bằng mã
  nên nó không biết nút nào gọi.
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
* **Thuế suất VAT đọc từ `billing.thue-suat-vat`, không gõ cứng.** Ba chỗ *hiển thị* (màn hình
  chi tiết hóa đơn, bản PDF, bảng đối soát) phải đi theo qua `thamSo.nhanThueSuat()` — tính 8%
  mà tờ hóa đơn khách cầm ghi 10% thì tệ hơn là không làm. Gõ cứng lại chỉ có **ba** phép kiểm
  bắt được: `BillingServiceTest.thueTinhTheoCauHinh`,
  `DoiSoatCuocServiceTest.nhanDongThueDiTheoCauHinh`,
  `HoaDonPdfServiceTest.nhanThueDiTheoCauHinh` — cả ba đều đặt thuế suất 8% để phân biệt được
  với hằng số 0.10 trùng với dữ liệu mẫu.
* **Đổi thuế suất KHÔNG tính lại hóa đơn cũ** (hóa đơn là chứng từ). Hệ quả: bảng đối soát của
  hóa đơn cũ sẽ hiện lệch sau khi đổi thuế suất — xem javadoc `ThamSoNghiepVu`.
* **Báo cáo phân quyền theo NỘI DUNG, không theo tiền tố đường dẫn.** Báo cáo có số tiền của
  khách → `KE_TOAN` + `ADMIN`, cùng luật với `/hoa-don` và `/cong-no`. Nới lại thành cả cụm
  `/bao-cao/**` là mở lại đúng lỗ hổng cũ. `test-auth.ps1` mục 8 canh việc này.
* **`han_muc_tin_dung` đã hết là cột chết** — `HoaDonRepository.timThueBaoVuotHanMuc()` dùng nó.
  Hạn mức `0` nghĩa là **chưa đặt**, không phải "không cho nợ đồng nào".
* **Bảng aging đủ 5 nhóm chỉ đúng tới 13/08/2026** — đó là tính chất của ngày xem chứ không phải
  của dữ liệu. Xem `PHASE-6-REPORT.md` mục 1.2 trước khi tưởng có gì hỏng.
