# GÓI LỆNH — vnpt-billing — phase-11: Tài nguyên tự host + `/` rẽ nhánh + khung HTML landing

Thư mục: D:\KGU\TTNN\APP\vnpt-billing
Nhánh: `phase-11-landing-khung-html`
Đọc trước: CLAUDE.md (Pipeline, Invariants), docs/STATUS.md, docs/LOI-DA-GAP.md, spec mục 3.1, 3.2, 3.4, 3.5, 4.2–4.4, 4.7, 5.1–5.3, kế hoạch **Task 4** và **Task 5**
Bước workflow: B5 + B6 · chạy bằng `/ship docs/phases/phase-11.md`
Giao diện: CO — URL: `http://localhost:8080/` (chưa đăng nhập), `/?loi`, `/?khoa=15`, `/?dathoat`

## Mục tiêu (1 câu)
Sau phase này, người chưa đăng nhập mở `http://localhost:8080/` thấy landing đủ năm cảnh bằng HTML thuần (chưa có CSS riêng và chuyển động), với số liệu thật, ví dụ tính cước thật, form đăng nhập hoạt động, và **không lộ** bất kỳ dữ liệu bên trong nào.

## Phạm vi
Làm:
- Tải phông woff2 (Unbounded, Geist, Geist Mono; bộ latin, latin-ext, vietnamese) + giấy phép OFL vào `static/fonts/`; GSAP 3.13.0 (`gsap`, `ScrollTrigger`, `SplitText`) + ghi chú giấy phép vào `static/vendor/gsap/3.13.0/`; ghi SHA-256 từng tệp.
- `static/css/tokens.css` (bảng màu sáng/tối đúng spec 4.2, `@font-face` có `unicode-range`), `static/favicon.svg`.
- `SecurityConfig`: `/fonts/**`, `/vendor/**`, `/favicon.svg` vào nhóm tĩnh; `GET "/"` `permitAll` đặt **trước** `anyRequest()`.
- `HomeController.trangChu`: chưa đăng nhập → model `soLieu`, `viDu` → view `landing`; đã đăng nhập → giữ nguyên thân hàm cũ.
- `templates/landing.html`: năm cảnh, các `id` DOM ở kế hoạch Task 5 (mục Interfaces), form đăng nhập có `nguon=landing`, ba trạng thái `data-mo-san`.
- `KiemTraLoDuLieu.timViPham(...)` + `LandingControllerTest` (kế hoạch Task 5 bước 1), thêm lớp test `khongCoViDu` với `@MockitoBean LandingService`.

KHÔNG làm (dù thấy cần):
- Không viết `landing.css`, `landing.js` (phase-12), không làm khung nở (phase-13), không sửa `/dang-nhap` (phase-13).
- Không nạp Bootstrap hay bất kỳ tài nguyên ngoài `localhost` trên landing.
- Không đụng `layout.html` và 46 màn hình nghiệp vụ.

## Bối cảnh cần biết
- Chữ trên trang: đúng spec 5.2, **chạy `anthropic-skills:chong-van-ai` trước khi đưa vào template**; không dấu gạch dài; không có `CDR`, `rating`, `billing`… (luật `kiem-tu-ngu.py`).
- Lời dẫn mở màn gắn lớp `mo-ta-trang` (luật dòng giải thích của `kiem-giao-dien.py`). Thêm chú thích `NUT-NOI-BAT-CO-Y: hai nút Đăng nhập là cùng một hành động`.
- Thẻ ví dụ bọc giữa `<!-- vi-du:bat-dau -->` và `<!-- vi-du:ket-thuc -->` (bộ lọc chống lộ tiền bỏ qua đoạn này); chỉ hiện khi `viDu` có mặt.
- Câu báo lỗi dùng lại đúng câu của `dang-nhap.html` hiện tại cho `?loi`, `?khoa=N`, `?dathoat`.
- Kiểm "chưa đăng nhập": `SecurityUtils.layNguoiDungHienTai().isEmpty()` (giống `LayoutAdvice`).

## Việc cụ thể, theo thứ tự
1. Test `TaiNguyenTinhTest` (4 tài nguyên, chưa đăng nhập → 200) → đỏ → sửa `SecurityConfig` nhóm tĩnh → xanh.
2. Tải tài nguyên, viết `tokens.css`, `favicon.svg`; đo lại tương phản mọi cặp màu (khớp bảng spec 4.2).
3. Test `LandingControllerTest` + `khongCoViDu` + đối chứng âm của `timViPham` → đỏ.
4. `permitAll GET /`, rẽ nhánh `HomeController`, viết `landing.html` → xanh.

## Tiêu chí xong (tự kiểm, phải kèm bằng chứng)
- [ ] Đối chứng âm: `timViPham("<p>Còn nợ 85.848.297 đ</p>", …)` không rỗng
- [ ] `GET /` chưa đăng nhập: 200, view `landing`, 4 số đếm khớp `count()`, `timViPham` với tên của **mọi** khách trong CSDL → rỗng
- [ ] `GET /` đã đăng nhập → view `index`
- [ ] `.\mvnw.cmd -B -ntp clean test` pass toàn bộ (gồm `KiemTraDieuHuongTest`, `KiemTraUuTienThuocTinhTest`), ghi tổng số test
- [ ] `python scripts/kiem-tu-ngu.py`, `kiem-giao-dien.py`, `kiem-ban-phim.py` → ĐẠT
- [ ] Chạy app, mở `/`: đủ năm cảnh, chữ đúng spec; đăng nhập đúng bằng form trên landing vào được trang tổng quan
- [ ] Mốc dữ liệu y nguyên

## Skill dùng (theo bảng trong CLAUDE.md)
superpowers:test-driven-development → anthropic-skills:chong-van-ai (chữ trên trang) → superpowers:verification-before-completion

## Giới hạn
90 phút. Vượt 2 khối thì dừng, đề xuất tách phase.

## Dừng và hỏi tôi nếu (ngoài điều kiện trong CLAUDE.md)
- Mở `GET /` làm đỏ bất kỳ phép kiểm phân quyền cũ nào.
- `chong-van-ai` muốn đổi nghĩa (không chỉ câu chữ) của nội dung spec 5.2.

## Kết thúc
Báo cáo theo mẫu CLAUDE.md → cập nhật docs/STATUS.md → commit `phase-11: tu host tai nguyen, / re nhanh, khung HTML landing khong lo du lieu`.
