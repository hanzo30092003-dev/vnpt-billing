# Landing Hanzo · Kế hoạch triển khai

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `/` hiện landing Hanzo cho người chưa đăng nhập: năm cảnh, motion GSAP, đăng nhập bằng khung nở ra từ nút. Kèm đổi tên sang Hanzo và bộ nhận diện mới, không lộ dữ liệu bên trong, không đổi dữ liệu.

**Architecture:** `HomeController` rẽ nhánh theo trạng thái đăng nhập. Một `LandingService` mới cung cấp 4 số đếm và ví dụ tính cước bằng đúng hàm của engine. Landing là một template riêng (không dùng `layout.html`), có CSS/JS riêng và bộ nhận diện trong `tokens.css`. Mọi phông, thư viện và ảnh đều tự host. Đích chuyển hướng khi đăng nhập thất bại đi qua một hàm thuần có whitelist.

**Tech Stack:** Spring Boot 3.5.16 · Thymeleaf · Spring Security · GSAP 3.13.0 (ScrollTrigger, SplitText) tự host · Unbounded / Geist / Geist Mono (woff2 Fontsource) · Playwright + Edge (có sẵn trong `tools-chup-anh/`) · Lighthouse (cài cục bộ, đã được cho phép).

**Spec:** [`docs/superpowers/specs/2026-09-30-landing-hanzo-design.md`](../specs/2026-09-30-landing-hanzo-design.md) · ADR: [`docs/adr/0001-nhan-dien-hanzo.md`](../../adr/0001-nhan-dien-hanzo.md)

## Global Constraints

- **Không đụng:** `pom.xml`, `application.yml`, `db/**`, entity, repository. Không thêm thư viện Java.
- Không chạy profile `reset`; không tính cước, lập hay huỷ hóa đơn. Kỳ 9/2026 giữ rỗng và `MO`.
- **Dữ liệu trước và sau** (đo ở Task 1 và Task 11): 7 kỳ · 23.223 CDR · 338 hóa đơn · 753 chi tiết · 161 thanh toán · 4 bất biến 0 lệch.
- Landing không gọi ra ngoài `localhost`: phông, GSAP, ảnh đều tự host.
- Màu chỉ lấy từ `static/css/tokens.css` (bảng ở spec 4.2). `#0F8A5F` **không bao giờ** dùng cho chữ dưới 24px hoặc làm nền cho chữ nhỏ.
- Không chữ thường trực dưới 13px; nhãn gắn với con số từ 14px trở lên; không viết hoa để bù chữ nhỏ.
- Chữ trên trang lấy đúng spec 5.2, qua `anthropic-skills:chong-van-ai`; không dùng dấu gạch dài; không có `CDR`, `rating`, `billing`, `prorate`, `idempotent`, `snapshot`, `batch` (luật của `kiem-tu-ngu.py`).
- Landing và `/dang-nhap` không in `admin`, `nhanvien01`, `ketoan01`, `123456`.
- Tham số `nguon` **không bao giờ** được dùng làm đích chuyển hướng.
- Tên tệp, thư mục mới: thuần ASCII, không dấu, không khoảng trắng. Tệp `.ps1` có tiếng Việt lưu kèm BOM UTF-8.
- Commit sau mỗi task, thông điệp tiếng Việt không dấu, kết thúc bằng dòng `Co-Authored-By` theo quy định. **Không push** khi chưa hỏi.
- Phép kiểm tích hợp chạy trên CSDL thật: **dừng app trước khi `mvnw test`**.

## Review Focus

1. **GSAP không tải được** (tệp 404, JS lỗi) → toàn bộ nội dung vẫn hiện, nút *Đăng nhập* vẫn dẫn sang `/dang-nhap`. Kiểm ở Task 7, bước 5.
2. **Gói CB01 bị đổi mã hoặc bảng giá thiếu dòng** → landing vẫn trả 200, chỉ ẩn thẻ ví dụ, không 500. Kiểm ở Task 3, bước 1.
3. **Bấm đúp nhanh nút *Đăng nhập*** → chỉ một khung, đúng kích thước, không kẹt hiệu ứng. Kiểm ở Task 8, bước 5.
4. **Phóng to 200% trên màn 1366px** (tức rộng 683px CSS) → dùng bố cục điện thoại: không ghim cảnh nào, không cuộn ngang. Kiểm ở Task 6, bước 4.
5. **Tải lại trang sau khi thấy `/?loi`** → tham số đã bị xoá khỏi thanh địa chỉ, không hiện lại lỗi cũ. Kiểm ở Task 8, bước 5.

---

### Task 1: Mốc dữ liệu + đổi tên sang Hanzo

**Files:**
- Modify: `src/main/resources/templates/fragments/layout.html:85`
- Modify: `src/main/java/com/hanzo/billing/service/pdf/HoaDonPdfService.java:110`, `src/main/java/com/hanzo/billing/service/pdf/PhieuThuPdfService.java:51`
- Modify: `src/main/resources/templates/hoa-don/chi-tiet.html:44`
- Modify: `src/test/java/com/hanzo/billing/service/pdf/{HoaDonPdfServiceTest.java:144, PhieuThuPdfServiceTest.java:73, PhieuThuPdfTaiLieuThatTest.java:75}`
- Modify: `CLAUDE.md` (luật *đơn vị phát hành hư cấu*), `README.md`
- Create: `docs/STATUS.md`

**Interfaces:** Produces — tên nhà mạng `CÔNG TY CỔ PHẦN VIỄN THÔNG HANZO` (PDF, chữ hoa) / `Công ty Cổ phần Viễn thông Hanzo` (HTML); tên phần mềm trên thanh máy `Hanzo · Quản lý cước`.

- [ ] **Step 1: Ghi mốc dữ liệu.** Chạy bằng `mysql.exe` với `MYSQL_PWD` lấy từ biến `MYSQL_PASSWORD` phạm vi User (không in giá trị ra):
  ```sql
  SELECT CONCAT('ky=',(SELECT COUNT(*) FROM ky_cuoc),' cdr=',(SELECT COUNT(*) FROM chi_tiet_su_dung),
    ' hd=',(SELECT COUNT(*) FROM hoa_don),' cthd=',(SELECT COUNT(*) FROM chi_tiet_hoa_don),
    ' tt=',(SELECT COUNT(*) FROM thanh_toan),' ky9=',(SELECT CONCAT(trang_thai,'/',
    (SELECT COUNT(*) FROM chi_tiet_su_dung c WHERE c.ky_cuoc_id=k.id),'/',
    (SELECT COUNT(*) FROM hoa_don h WHERE h.ky_cuoc_id=k.id)) FROM ky_cuoc k WHERE thang=9 AND nam=2026));
  SELECT CONCAT((SELECT COUNT(*) FROM hoa_don WHERE con_no <> tong_thanh_toan - da_thanh_toan),'/',
    (SELECT COUNT(*) FROM hoa_don h WHERE da_thanh_toan <> IFNULL((SELECT SUM(so_tien) FROM thanh_toan t WHERE t.hoa_don_id=h.id),0)),'/',
    (SELECT COUNT(*) FROM thue_bao tb WHERE so_du <> IFNULL((SELECT SUM(CASE WHEN loai_bien_dong='TRU_CUOC' THEN -so_tien ELSE so_tien END) FROM bien_dong_so_du b WHERE b.thue_bao_id=tb.id),0)),'/',
    (SELECT COUNT(*) FROM chi_tiet_su_dung WHERE trang_thai_tinh_cuoc='DA_TINH' AND bang_gia_cuoc_id IS NULL AND mien_phi=0));
  ```
  Kỳ vọng `ky=7 cdr=23223 hd=338 cthd=753 tt=161 ky9=MO/0/0` và `0/0/0/0`. Lệch thì **dừng và báo**.
- [ ] **Step 2: Sửa 3 phép kiểm PDF cho đỏ trước.** Ở cả ba chỗ, thay `contains("VIỄN THÔNG SÔNG HẬU")` bằng:
  ```java
  assertThat(vanBan).contains("VIỄN THÔNG HANZO")
                    .doesNotContain("SÔNG HẬU")
                    .doesNotContain("VNPT");
  ```
- [ ] **Step 3: Chạy cho thấy đỏ.** `mvnw -q test -Dtest="HoaDonPdfServiceTest,PhieuThuPdfServiceTest,PhieuThuPdfTaiLieuThatTest"` → FAIL, vì PDF vẫn in *Sông Hậu*.
- [ ] **Step 4: Đổi tên ở 4 chỗ** trong mục Files. `data-mau.sql:109` (*"KCN Sông Hậu"*, địa chỉ khách) **không** đụng.
- [ ] **Step 5: Chạy lại** → PASS.
- [ ] **Step 6: Soát sót.** `grep -rn "Sông Hậu\|SÔNG HẬU" src/main src/test` chỉ còn đúng dòng `data-mau.sql:109`.
- [ ] **Step 7: Sửa luật trong `CLAUDE.md`** (giữ nguyên tinh thần chống mạo danh; 3 phép kiểm nay cấm cả `SÔNG HẬU`), sửa `README.md` và các tài liệu mô tả hiện trạng có nhắc tên (`grep -rln "Sông Hậu" docs`). Thêm ngay dưới luật *"Bảng màu chỉ có MỘT nguồn"* một dòng về trạng thái chuyển tiếp của spec 4.6. Các báo cáo lịch sử (`docs/PHASE-*`, `docs/G*-REPORT.md`) giữ nguyên nội dung, chỉ thêm một dòng đầu tệp: *"Từ 30/09/2026 nhà mạng hư cấu đổi tên thành Viễn thông Hanzo; tài liệu này giữ tên cũ tại thời điểm viết."*
- [ ] **Step 8: Tạo `docs/STATUS.md`** theo mẫu quy trình HANZO: bước B5, đang làm spec landing Hanzo, việc tiếp theo là Task 2.
- [ ] **Step 9: Commit** `phase-L1: doi ten Sông Hau thanh Hanzo, tao STATUS.md`.

### Task 2: Đích chuyển hướng khi đăng nhập thất bại, đăng xuất về landing

**Files:**
- Modify: `src/main/java/com/hanzo/billing/security/XuLyDangNhap.java` (dòng gửi chuyển hướng ở `onAuthenticationFailure`)
- Modify: `src/main/java/com/hanzo/billing/config/SecurityConfig.java` (`logoutSuccessUrl`)
- Create: `src/test/java/com/hanzo/billing/security/XuLyDangNhapDichTest.java`

**Interfaces:** Produces — `public static String XuLyDangNhap.dichKhiThatBai(String duongDanTrangDangNhap, String nguon)`: trả `"/" + phần sau "/dang-nhap"` khi và chỉ khi `nguon` **đúng bằng** `"landing"`; mọi trường hợp khác trả nguyên `duongDanTrangDangNhap`. Hằng `NGUON_LANDING = "landing"`.

- [ ] **Step 1: Viết test thuần (không Spring)**
  ```java
  @Test void landing_loi()  { assertThat(XuLyDangNhap.dichKhiThatBai("/dang-nhap?loi", "landing")).isEqualTo("/?loi"); }
  @Test void landing_khoa() { assertThat(XuLyDangNhap.dichKhiThatBai("/dang-nhap?khoa=15", "landing")).isEqualTo("/?khoa=15"); }
  @Test void khongNguon()   { assertThat(XuLyDangNhap.dichKhiThatBai("/dang-nhap?loi", null)).isEqualTo("/dang-nhap?loi"); }
  @ParameterizedTest
  @ValueSource(strings = {"https://gia-mao.vn", "//gia-mao.vn", "Landing", " landing", "landing ", "/", ""})
  void chongChuyenHuongMo(String nguon) {
      assertThat(XuLyDangNhap.dichKhiThatBai("/dang-nhap?loi", nguon)).isEqualTo("/dang-nhap?loi");
  }
  ```
- [ ] **Step 2: Viết test tích hợp** (`@SpringBootTest` + `MockMvcBuilders.webAppContextSetup(...).apply(springSecurity())`, cùng khuôn `TinhCuocControllerTest`). **Chỉ dùng tên đăng nhập không tồn tại** `khong_ton_tai_l2`, để không tăng bộ đếm sai của tài khoản thật:
  ```java
  mockMvc.perform(post("/dang-nhap").with(csrf()).param("tenDangNhap","khong_ton_tai_l2")
          .param("matKhau","x").param("nguon","landing"))
         .andExpect(redirectedUrl("/?loi"));
  mockMvc.perform(post("/dang-nhap").with(csrf()).param("tenDangNhap","khong_ton_tai_l2").param("matKhau","x"))
         .andExpect(redirectedUrl("/dang-nhap?loi"));
  mockMvc.perform(post("/dang-xuat").with(csrf()).with(user(nguoiDung(VaiTro.ADMIN))))
         .andExpect(redirectedUrl("/?dathoat"));
  ```
- [ ] **Step 3: Chạy** → FAIL (thiếu hàm, sai đích đăng xuất).
- [ ] **Step 4: Cài `dichKhiThatBai`**, gọi nó ngay dòng `sendRedirect` của `onAuthenticationFailure` với `request.getParameter("nguon")`. Đổi `logoutSuccessUrl("/?dathoat")`. `onAuthenticationSuccess` giữ nguyên.
- [ ] **Step 5: Chạy** → PASS.
- [ ] **Step 6: Commit** `phase-L2: dich chuyen huong khi dang nhap that bai co whitelist nguon=landing`.

### Task 3: `LandingService` — 4 số đếm và ví dụ tính cước

**Files:**
- Create: `src/main/java/com/hanzo/billing/dto/landing/SoLieuLanding.java`, `src/main/java/com/hanzo/billing/dto/landing/ViDuTinhCuoc.java`
- Create: `src/main/java/com/hanzo/billing/service/landing/LandingService.java`
- Create: `src/test/java/com/hanzo/billing/service/landing/LandingServiceTest.java` (thuần), `src/test/java/com/hanzo/billing/service/landing/LandingServiceTichHopTest.java` (`@SpringBootTest`)

**Interfaces:**
- Produces: `record SoLieuLanding(long soCdr, long soHoaDon, long soKyCuoc, long soThanhToan)`
- Produces: `record ViDuTinhCuoc(String tenGoi, long thoiLuongGiay, int blockGiay, BigDecimal donGia, long soBlock, BigDecimal cuocGoi, BigDecimal cuocThueBao, BigDecimal tongTruocThue, String nhanThueSuat, BigDecimal thueVat, BigDecimal tongThanhToan)`
- Produces: `LandingService.soLieu(): SoLieuLanding` — 4 phép `count()` trên `ChiTietSuDungRepository`, `HoaDonRepository`, `KyCuocRepository`, `ThanhToanRepository`.
- Produces: `LandingService.viDuTinhCuoc(): Optional<ViDuTinhCuoc>` gọi `viDuTinhCuoc(MA_GOI_VI_DU)` với `MA_GOI_VI_DU = "CB01"`; bản có tham số `Optional<ViDuTinhCuoc> viDuTinhCuoc(String maGoi)` (phạm vi package, để test) dùng `THOAI` · `NOI_MANG` · giờ thường · `THOI_LUONG_VI_DU = 134` · ngày `LocalDate.now()`, trả `Optional.empty()` khi không có gói hoặc không có dòng giá (Review Focus 2).
- Produces: `static ViDuTinhCuoc LandingService.tinh(String tenGoi, BangGiaCuoc gia, BigDecimal cuocThueBao, BigDecimal thueSuat, String nhanThueSuat, long thoiLuongGiay)` — hàm thuần. Công thức **chỉ** gọi hàm sẵn có: `DonViCuoc.soBlock`, `ThamSoTinhCuoc.lamTronTien`; thuế `lamTronTien(tongTruocThue × thueSuat)` giống `BillingService:163`.
- Consumes: `BangGiaLookup.nap().tim(goiCuocId, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, ngay)`, `GoiCuocRepository.findByMaGoi`, `ThamSoNghiepVu.getThueSuatVat()`, `nhanThueSuat()`.

- [ ] **Step 1: Test thuần.** `giaGia(int blockGiay, String donGia)` là hàm phụ trong test, tạo `BangGiaCuoc` chỉ đặt `blockGiay` và `donGia`.
  ```java
  @Test void duDoanCongBoTruoc() {   // spec 5.3
      BangGiaCuoc gia = giaGia(6, "15");
      ViDuTinhCuoc v = LandingService.tinh("Cơ bản", gia, new BigDecimal("50000"), new BigDecimal("0.10"), "10%", 134);
      assertThat(v.soBlock()).isEqualTo(23);
      assertThat(v.cuocGoi()).isEqualByComparingTo("345");
      assertThat(v.tongTruocThue()).isEqualByComparingTo("50345");
      assertThat(v.thueVat()).isEqualByComparingTo("5035");
      assertThat(v.tongThanhToan()).isEqualByComparingTo("55380");
  }
  @Test void doiChungAm_doiDonGiaThiLech() {
      ViDuTinhCuoc v = LandingService.tinh("Cơ bản", giaGia(6, "20"), new BigDecimal("50000"), new BigDecimal("0.10"), "10%", 134);
      assertThat(v.cuocGoi()).isNotEqualByComparingTo("345");
  }
  ```
- [ ] **Step 2: Test tích hợp.**
  - `soLieu()` bằng đúng 4 phép `count()` đọc trực tiếp từ repository.
  - `viDuTinhCuoc()` có mặt, và bằng `tinh(...)` gọi với dòng giá tra từ `BangGiaLookup` — **không** so với số gõ cứng.
  - `viDuTinhCuoc("KHONG_CO_GOI_NAY")` → `Optional.empty()`, không ném lỗi.
- [ ] **Step 3: Chạy** → FAIL.
- [ ] **Step 4: Cài** DTO và `LandingService` theo Interfaces (`@Service`, `@Transactional(readOnly = true)`).
- [ ] **Step 5: Chạy** → PASS. Nếu số thật khác dự đoán 23 / 345 / 55.380: **dừng, phân tích**, không sửa test cho khớp.
- [ ] **Step 6: Commit** `phase-L3: LandingService so dem va vi du tinh cuoc bang ham cua engine`.

### Task 4: Tài nguyên tự host — phông, GSAP, `tokens.css`, favicon

**Files:**
- Create: `src/main/resources/static/fonts/{unbounded,geist,geist-mono}/*-{latin,latin-ext,vietnamese}-wght-normal.woff2` + `OFL.txt` mỗi thư mục (lấy từ `@fontsource-variable/<ten>` trên jsDelivr)
- Create: `src/main/resources/static/vendor/gsap/3.13.0/{gsap,ScrollTrigger,SplitText}.min.js` + `GHI-CHU-GIAY-PHEP.txt` (GSAP Standard "no charge" license, kèm đường dẫn)
- Create: `src/main/resources/static/css/tokens.css`, `src/main/resources/static/favicon.svg`
- Modify: `src/main/java/com/hanzo/billing/config/SecurityConfig.java` (nhóm tài nguyên tĩnh)

**Interfaces:** Produces — các biến CSS `--hz-giay, --hz-the, --hz-muc, --hz-mo, --hz-vien, --hz-vien-nhap, --hz-nhan, --hz-nhan-lon, --hz-loi, --hz-loi-nen, --hz-thu, --hz-thu-nen, --hz-no, --hz-no-nen` (giá trị sáng/tối đúng spec 4.2), `--hz-chu-tieu-de`, `--hz-chu-than`, `--hz-chu-ma`, thang cỡ chữ `--hz-c13 … --hz-c64`, `--hz-cong: cubic-bezier(0.32,0.72,0,1)`. Chế độ tối qua `@media (prefers-color-scheme: dark)`. `@font-face` có `unicode-range` và `font-display: swap`.

- [ ] **Step 1: Tải tệp.** Kiểm SHA-256 từng tệp và ghi vào `GHI-CHU-GIAY-PHEP.txt`. Mở đầu `gsap.min.js` phải có chuỗi `GSAP 3.13.0`.
- [ ] **Step 2: Viết `tokens.css` và `favicon.svg`** (chữ `H` + chấm `#0F8A5F` trên nền `#16181B`).
- [ ] **Step 3: Test tích hợp** `TaiNguyenTinhTest`: `GET /fonts/geist/geist-vietnamese-wght-normal.woff2`, `GET /vendor/gsap/3.13.0/gsap.min.js`, `GET /css/tokens.css`, `GET /favicon.svg` khi **chưa đăng nhập** → 200. Chạy → FAIL (`/fonts/**`, `/vendor/**` chưa được mở).
- [ ] **Step 4: Thêm `"/fonts/**", "/vendor/**", "/favicon.svg"`** vào dòng `permitAll` của tài nguyên tĩnh. Chạy → PASS.
- [ ] **Step 5: Đo lại tương phản** mọi cặp chữ/nền trong `tokens.css` bằng `scratchpad/tuong-phan.py`; phải khớp bảng spec 4.2.
- [ ] **Step 6: Commit** `phase-L4: tu host phong, GSAP, tokens.css va favicon`.

### Task 5: `GET /` rẽ nhánh + khung HTML của landing (đủ nội dung khi không có JS)

**Files:**
- Modify: `src/main/java/com/hanzo/billing/controller/HomeController.java`, `src/main/java/com/hanzo/billing/config/SecurityConfig.java`
- Create: `src/main/resources/templates/landing.html`
- Create: `src/test/java/com/hanzo/billing/controller/LandingControllerTest.java`, `src/test/java/com/hanzo/billing/KiemTraLoDuLieu.java` (hàm kiểm dùng chung)

**Interfaces:**
- Consumes: `LandingService.soLieu()`, `LandingService.viDuTinhCuoc()` (Task 3).
- Produces: model `soLieu`, `viDu` (có thể rỗng); view `landing`. Các `id` DOM mà Task 6–8 dựa vào: `#tieu-de`, `#canh-hanh-trinh`, `.tram li`, `#the-cdr`, `#the-gia`, `#the-hoa-don`, `#dau-da-thu`, `#vai-ray`, `[data-dem]` (giá trị cuối in sẵn), `a.nut-dang-nhap` (`href="/dang-nhap"`), `#khung-dang-nhap` (`form` gửi `/dang-nhap`, có `_csrf`, `tenDangNhap`, `matKhau`, `input[type=hidden][name=nguon][value=landing]`), `[data-mo-san]` với giá trị `loi` · `khoa` · `dathoat`.
- Produces: `static List<String> KiemTraLoDuLieu.timViPham(String html, Collection<String> tenKhach)` — bỏ đoạn giữa `<!-- vi-du:bat-dau -->` và `<!-- vi-du:ket-thuc -->`, rồi tìm: `thanh-may-hieu`, `Tổng công nợ`, `Còn nợ`, `admin`, `nhanvien01`, `ketoan01`, `123456`, mọi tên trong `tenKhach`, và regex `\d{1,3}(\.\d{3})+ đ`.

- [ ] **Step 1: Test**
  - `doiChungAm`: `timViPham("<p>Còn nợ 85.848.297 đ</p>", List.of())` **không rỗng**.
  - `chuaDangNhap_thayLanding`: `GET /` → 200, view `landing`, có `HANZO`, có 4 số đếm định dạng vi-VN (ví dụ `23.223`), `timViPham(html, tenCuaMoiKhachTrongCSDL)` **rỗng**.
  - `daDangNhap_thayTongQuan`: `GET /` với `user(...)` → view `index`.
  - `loi`: `GET /?loi` → có `data-mo-san="loi"` và câu *"Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã bị khoá."*
  - `khoa`: `GET /?khoa=15` → có `data-mo-san="khoa"` và `15`.
  - `dathoat`: `GET /?dathoat` → có câu *"Bạn đã đăng xuất khỏi hệ thống."*
  - `khongCoViDu` (lớp test riêng, `@MockitoBean LandingService` trả `Optional.empty()` cho ví dụ): `GET /` → 200, **không** có `vi-du:bat-dau` (Review Focus 2).
- [ ] **Step 2: Chạy** → FAIL (`/` vẫn bắt đăng nhập).
- [ ] **Step 3: Cài.**
  - `SecurityConfig`: `.requestMatchers(HttpMethod.GET, "/").permitAll()` đặt **trước** `anyRequest()`.
  - `HomeController.trangChu`: chưa đăng nhập (`SecurityUtils.layNguoiDungHienTai().isEmpty()`) → đưa `soLieu`, `viDu` vào model, trả `"landing"`; ngược lại giữ nguyên thân hàm hiện tại.
  - Trước khi viết template: chạy skill `anthropic-skills:chong-van-ai` trên toàn bộ chữ của spec 5.2; chữ đưa vào template là bản đã qua skill (mọi chỗ đổi so với spec ghi vào `docs/test-report.md`).
  - `landing.html`: đủ năm cảnh theo spec 5.1, `<link rel="icon" href="/favicon.svg">`. Lời dẫn gắn lớp `mo-ta-trang` (luật dòng giải thích của `kiem-giao-dien.py`). Thẻ ví dụ bọc giữa hai chú thích `vi-du:*`, chỉ hiện khi `viDu` có mặt, ghi *"Ví dụ minh họa · tính theo bảng giá thật của gói Cơ bản"* và số điện thoại che đuôi. Có chú thích `NUT-NOI-BAT-CO-Y: hai nút Đăng nhập là cùng một hành động`. Nút đóng khung có `aria-label="Đóng"`. `<link rel="preload">` hai tệp woff2 của Unbounded (latin, vietnamese).
- [ ] **Step 4: Chạy** → PASS. Chạy cả `KiemTraDieuHuongTest`, `KiemTraUuTienThuocTinhTest` → PASS.
- [ ] **Step 5: Commit** `phase-L5: / re nhanh theo dang nhap, khung HTML landing khong lo du lieu`.

### Task 6: `landing.css` — giao diện tĩnh, hai chế độ, hai khổ

**Files:**
- Create: `src/main/resources/static/css/landing.css`
- Create: `tools-chup-anh/landing-kiem.mjs` (công cụ cá nhân, đã `.gitignore`)

**Interfaces:** Consumes các `id`/lớp của Task 5 và biến của Task 4. Không định nghĩa màu mới ngoài `tokens.css`.

- [ ] **Step 1: Viết `landing-kiem.mjs`.** Nó mở `/` bằng Edge ở bốn tổ hợp 375px/1440px × sáng/tối, **tắt JS**, rồi: chụp ảnh; đo `scrollWidth − innerWidth`; lấy vị trí `a.nut-dang-nhap`; bắt `pageerror`. Ghi ra `ket-qua-landing.json`.
- [ ] **Step 2: Viết `landing.css`** theo spec 5.1: thanh nổi dạng viên thuốc, nút *nút trong nút*, khoảng trắng dọc ≥ 96px, lớp hạt giấy cố định độ mờ 0,03, lưới lệch cỡ ở cảnh 3, khối nền mực ở cảnh 4 (tối thì dùng `--hz-the`), bố cục một cột dưới 768px. Chỉ dùng `transform`/`opacity` cho trạng thái động.
- [ ] **Step 3: Chạy `node tools-chup-anh/landing-kiem.mjs`.** Kỳ vọng: tràn ngang 0 ở cả bốn tổ hợp; nút *Đăng nhập* nằm trong khung nhìn đầu ở 375px; không lỗi. Xem bốn ảnh bằng mắt.
- [ ] **Step 4: Review Focus 4.** Thêm tổ hợp 683×768 → bố cục điện thoại, tràn ngang 0.
- [ ] **Step 5: Commit** `phase-L6: giao dien tinh landing, sang toi, dien thoai`.

### Task 7: `landing.js` — chuyển động năm cảnh

**Files:**
- Create: `src/main/resources/static/js/landing.js`
- Modify: `src/main/resources/templates/landing.html` (script nhỏ đầu trang gắn lớp `js` lên `<html>`; ba thẻ GSAP + `landing.js` có `defer`)
- Modify: `tools-chup-anh/landing-kiem.mjs`

**Interfaces:** Consumes các `id` của Task 5. Mọi tween nằm trong `gsap.matchMedia()`, với hai nhánh `(min-width: 768px) and (prefers-reduced-motion: no-preference)` và `(max-width: 767px) and (prefers-reduced-motion: no-preference)`. Không có nhánh cho `reduce`. Không dùng `window.addEventListener('scroll')`; mọi thứ theo cuộn đi qua ScrollTrigger.

- [ ] **Step 1: Mở màn.** `SplitText.create('#tieu-de', { type: 'lines,chars', mask: 'lines' })` chạy sau `document.fonts.ready`; ký tự đi lên từ `yPercent: 100`, **không** đặt `opacity: 0` cho tiêu đề (giữ LCP). `[data-dem]` đếm từ 0 tới giá trị đã in sẵn, định dạng `Intl.NumberFormat('vi-VN')`.
- [ ] **Step 2: Cảnh 2 (≥ 768px).** Timeline ghim `#canh-hanh-trinh`, `end: '+=2800'`, `scrub: 1`. Trình tự đúng bản thử A đã duyệt: thẻ CDR vào → thẻ giá vào → kết quả hiện → hóa đơn in dần bằng `clipPath` → con dấu `scale: 3.2 → 1`, `back.out(2.2)`. `.tram li` bật lớp `on`/`xong` theo `progress`. Dưới 768px: không ghim, mỗi trạm hiện dần bằng `ScrollTrigger` riêng.
- [ ] **Step 3: Cảnh 3–5.** Số đếm lên khi vào khung nhìn. `#vai-ray` kéo ngang có ghim (≥ 768px), `invalidateOnRefresh: true`. Tiêu đề cảnh 5 trồi lên.
- [ ] **Step 4: Mở rộng `landing-kiem.mjs`.**
  - Bật JS, cuộn qua 8 mốc, chụp ảnh, bắt lỗi.
  - Chạy thêm với `reducedMotion: 'reduce'`: không phần tử nào `pin-spacer`, hóa đơn và con dấu hiện đầy đủ.
  - Tràn ngang vẫn 0.
- [ ] **Step 5: Review Focus 1.** Chặn `/vendor/gsap/**` (route abort) → mọi `[data-dem]` hiện giá trị thật, bốn trạm và hóa đơn hiện đủ, bấm `a.nut-dang-nhap` sang `/dang-nhap`.
- [ ] **Step 6: Commit** `phase-L7: chuyen dong nam canh landing bang GSAP`.

### Task 8: Khung đăng nhập — nút nở thành khung

**Files:**
- Modify: `src/main/resources/static/js/landing.js`, `src/main/resources/static/css/landing.css`
- Modify: `tools-chup-anh/landing-kiem.mjs`

**Interfaces:** Consumes `a.nut-dang-nhap`, `#khung-dang-nhap`, `[data-mo-san]`. Produces `moKhung(nut: HTMLElement, coHieuUng: boolean)`, `dongKhung()` — dùng nội bộ `landing.js`.

- [ ] **Step 1: Cài theo spec 5.5.**
  - Bấm nút → `preventDefault`, nở ra từ `getBoundingClientRect()` của chính nút đó.
  - `role="dialog"`, `aria-modal`, nền `inert`.
  - Tab đi vòng trong khung, Esc đóng, tiêu điểm trả về nút đã mở.
  - Mỗi lần mở: `gsap.killTweensOf` + `clearProps: 'all'`; mọi `focus()` dùng `{ preventScroll: true }`.
  - Cờ `dangMo` chặn mở lần hai.
- [ ] **Step 2: Mở sẵn theo tham số.** `data-mo-san` là `loi` hoặc `khoa` → mở ngay, không hiệu ứng, con trỏ vào ô mật khẩu. `dathoat` → hiện dòng báo. Sau đó `history.replaceState(null, '', '/')`.
- [ ] **Step 3: Giảm chuyển động** → mở và đóng tức thì; bỏ hiệu ứng rung khi có lỗi.
- [ ] **Step 4: Kiểm tay trên app thật.** Đăng nhập đúng bằng `admin` / mật khẩu của môi trường phát triển → vào trang tổng quan. Đăng nhập sai bằng một **tên không tồn tại** → quay về landing, khung mở sẵn, có lỗi. Không dùng tài khoản thật để thử sai.
- [ ] **Step 5: Mở rộng `landing-kiem.mjs`:**
  - Chỉ dùng bàn phím: Tab tới nút, Enter, gõ tên không tồn tại, Enter → URL về `/`, khung mở, tiêu điểm ở ô mật khẩu.
  - Esc đóng, tiêu điểm về đúng nút.
  - **Review Focus 3:** bấm đúp nút → đúng một `[role=dialog]` hiện, rộng 400px.
  - **Review Focus 5:** tải lại sau `/?loi` → URL là `/`, không có lời báo lỗi.
- [ ] **Step 6: Commit** `phase-L8: khung dang nhap no ra tu nut, ban phim va trang thai loi`.

### Task 9: `/dang-nhap` theo bộ nhận diện mới

**Files:** Modify `src/main/resources/templates/dang-nhap.html`.

**Interfaces:** Giữ nguyên `action`, tên trường, `_csrf` và 5 khối thông báo (`loi`, `dathoat`, `khoa`, `doimatkhau`, `hethan`) cùng câu chữ hiện tại. Chỉ đổi giao diện sang `tokens.css` + phông tự host, bỏ Bootstrap CDN khỏi trang này.

- [ ] **Step 1: Sửa template.** Wordmark `HANZO.`, có favicon, hai chế độ sáng/tối.
- [ ] **Step 2: Chạy `mvnw -q test -Dtest=KiemTraDieuHuongTest`** → PASS. Chụp `/dang-nhap` ở 375/1440 × sáng/tối, thêm cả `?loi`, `?khoa=15`, `?hethan`.
- [ ] **Step 3: Commit** `phase-L9: trang dang nhap theo nhan dien Hanzo`.

### Task 10: Hai ảnh AI

**Files:** Create `src/main/resources/static/images/landing/{quay-giao-dich,nen-loi-moi}.webp`; modify `landing.html`.

- [ ] **Step 1: Sinh ảnh bằng Canva `generate-image`** (tải công cụ qua ToolSearch) theo mô tả spec 5.4.
- [ ] **Step 2: Duyệt bằng mắt.** Không logo, không màu thương hiệu nhà mạng, không chữ đọc được. Không đạt sau 3 lần sinh → **dừng và hỏi HANZO**; không dùng ảnh stock.
- [ ] **Step 3: Chuyển WebP** bằng canvas của Edge qua Playwright (`toDataURL('image/webp', 0.82)`), không cài công cụ mới. Mỗi ảnh ≤ 200 KB, đúng kích thước 1600×1000 và 1800×600.
- [ ] **Step 4: Gắn ảnh vào trang.** Có `width`/`height`; ảnh 2 `loading="lazy"`, `alt=""`; ảnh 1 có `alt` mô tả. Chạy lại `landing-kiem.mjs` → tràn ngang 0, không lỗi.
- [ ] **Step 5: Commit** `phase-L10: hai anh AI cho canh ba nguoi dung va loi moi cuoi`.

### Task 11: Script, nghiệm thu, tài liệu

**Files:**
- Modify: `scripts/test-auth.ps1` (mục 2, lưu kèm BOM UTF-8)
- Create: `docs/test-report.md`
- Modify: `docs/STATUS.md`

- [ ] **Step 1: Viết lại mục 2 của `test-auth.ps1`.** `CanCo @('HANZO','name="matKhau"')`, `KhongDuocCo @('thanh-may-hieu','Tổng công nợ','Còn nợ')`. Đối chứng âm: tạm đổi một chuỗi cấm thành chuỗi có trên trang → mục 2 đỏ; hoàn lại → xanh.
- [ ] **Step 2: Chạy toàn bộ.** `mvnw test` (dừng app trước) → không test nào đỏ, tổng khoảng 352. `python scripts/kiem-tu-ngu.py`, `kiem-giao-dien.py`, `kiem-ban-phim.py` → ĐẠT. Chạy app, rồi chạy 8 script trong `scripts/` → số phép kiểm đạt không giảm so với lần chạy gần nhất.
- [ ] **Step 3: Cài Lighthouse** vào `tools-chup-anh/` (`npm install lighthouse --save-dev`, **không** `-g`). Chạy bản mobile với `CHROME_PATH` trỏ tới Edge, 3 lần, lấy trung vị. Đạt: Performance ≥ 90, Accessibility ≥ 95, SEO ≥ 95.
- [ ] **Step 4: Chạy các kiểm còn lại.**
  - Ngoại tuyến: chặn mọi địa chỉ ngoài `localhost` (theo `thu-mat-mang.mjs`) → ảnh giống hệt lúc có mạng.
  - Taste mục 14 Pre-Flight Check.
  - `accessibility-review` của hồ sơ A.
- [ ] **Step 5: Kiểm lại mốc dữ liệu** như Task 1 bước 1 → phải y hệt.
- [ ] **Step 6: Ghi `docs/test-report.md`.** Bảng từng tiêu chí của spec 7.1–7.4 kèm Đạt/Không đạt và bằng chứng (lệnh, số, ảnh 375/1440 × sáng/tối).
- [ ] **Step 7: Cập nhật `docs/STATUS.md`** (xong spec landing; việc tiếp theo: spec 46 màn hình). Commit `phase-L11: nghiem thu landing Hanzo`.
