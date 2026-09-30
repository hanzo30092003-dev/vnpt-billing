# GÓI LỆNH — vnpt-billing — phase-13: Khung đăng nhập nở ra từ nút + `/dang-nhap` mới

Thư mục: D:\KGU\TTNN\APP\vnpt-billing
Nhánh: `phase-13-khung-dang-nhap`
Đọc trước: CLAUDE.md (Pipeline, Invariants), docs/STATUS.md, docs/LOI-DA-GAP.md, spec mục 3.3, 3.4, 5.5, kế hoạch **Task 8** và **Task 9**; bản thử đã duyệt `.superpowers/brainstorm/1339-1790779995/content/khung-dang-nhap.html` (kiểu C)
Bước workflow: B5 + B6 · chạy bằng `/ship docs/phases/phase-13.md`
Giao diện: CO — URL: `http://localhost:8080/`, `/?loi`, `/?khoa=15`, `/?dathoat`, `/dang-nhap`, `/dang-nhap?loi`, `/dang-nhap?khoa=15`, `/dang-nhap?hethan`

## Mục tiêu (1 câu)
Sau phase này, người dùng bấm *Đăng nhập* trên landing thì chính nút đó nở thành khung đăng nhập; đăng nhập được bằng chuột lẫn chỉ bằng bàn phím; sai thì quay về landing với khung mở sẵn và lời báo lỗi; còn `/dang-nhap` mang bộ nhận diện Hanzo.

## Phạm vi
Làm:
- `landing.js` + `landing.css`: `moKhung(nut, coHieuUng)`, `dongKhung()` theo spec 5.5 và kế hoạch Task 8 bước 1–3.
- `templates/dang-nhap.html`: giao diện theo `tokens.css` + phông tự host + favicon, sáng/tối; bỏ Bootstrap CDN **khỏi trang này**.
- Mở rộng `tools-chup-anh/landing-kiem.mjs` theo kế hoạch Task 8 bước 5 và Task 9 bước 2.

KHÔNG làm (dù thấy cần):
- Không đổi `action`, tên trường (`tenDangNhap`, `matKhau`), `_csrf`, hay câu chữ của 5 khối thông báo trên `/dang-nhap` (`loi`, `dathoat`, `khoa`, `doimatkhau`, `hethan`).
- Không đổi `XuLyDangNhap`, `SecurityConfig` (xong ở phase-10, phase-11).
- Không in tài khoản mẫu ở bất kỳ đâu.

## Bối cảnh cần biết
- Khung nở từ `getBoundingClientRect()` của **chính nút đã bấm** (thanh nổi hoặc cảnh 5): nền `--hz-muc` → `--hz-the`, bo góc 8 → 14px, khoảng 0,6 giây, `expo.inOut`; nội dung hiện sau. Đóng thì chạy ngược về đúng nút đó.
- `role="dialog"`, `aria-modal="true"`, `aria-labelledby`; nền `inert`; Tab đi vòng trong khung; Esc đóng; tiêu điểm trả về nút đã mở.
- **Hai bẫy đã bắt ở bản thử, bắt buộc canh:** mọi `focus()` dùng `{ preventScroll: true }`; mỗi lần mở gọi `gsap.killTweensOf` + `clearProps: 'all'`.
- Cờ `dangMo` chặn mở hai lần (Review Focus 3).
- `data-mo-san` là `loi` hoặc `khoa` → mở ngay không hiệu ứng, con trỏ vào ô mật khẩu; `dathoat` → hiện dòng báo; sau đó `history.replaceState(null, '', '/')` (Review Focus 5).
- Giảm chuyển động: mở và đóng tức thì, bỏ hiệu ứng rung.
- **Thử đăng nhập sai chỉ bằng tên không tồn tại.** Sai bằng tài khoản thật sẽ tăng bộ đếm; 5 lần là khoá 15 phút.

## Việc cụ thể, theo thứ tự
1. Viết các bước kiểm trong `landing-kiem.mjs`: bàn phím, Esc, bấm đúp, tải lại sau `/?loi` → chạy → đỏ.
2. Cài `moKhung`, `dongKhung`, trạng thái mở sẵn, giảm chuyển động → chạy → xanh.
3. Kiểm tay trên app thật: đúng → trang tổng quan; tên không tồn tại → landing, khung mở, có lỗi.
4. Làm lại `dang-nhap.html`; chụp 375/1440 × sáng/tối, cả `?loi`, `?khoa=15`, `?hethan`.

## Tiêu chí xong (tự kiểm, phải kèm bằng chứng)
- [ ] Chỉ bằng bàn phím: Tab tới nút → Enter → gõ tên không tồn tại → Enter → URL `/`, khung mở, tiêu điểm ở ô mật khẩu
- [ ] Esc đóng khung, tiêu điểm về đúng nút đã mở
- [ ] Bấm đúp nút → đúng một `[role=dialog]`, rộng 400px
- [ ] Tải lại sau `/?loi` → URL là `/`, không còn lời báo lỗi
- [ ] Đăng nhập đúng từ landing → trang tổng quan
- [ ] `/dang-nhap` giữ nguyên 5 câu thông báo (so chuỗi trước/sau); `test-auth.ps1` mục 1 vẫn đạt (không `thanh-may-hieu`, không tài khoản mẫu)
- [ ] 0 lỗi JS; `.\mvnw.cmd -B -ntp clean test` pass; `kiem-ban-phim.py` ĐẠT; mốc dữ liệu y nguyên

## Skill dùng (theo bảng trong CLAUDE.md)
superpowers:test-driven-development → hồ sơ A `accessibility-review` cho khung đăng nhập → superpowers:verification-before-completion

## Giới hạn
90 phút. Vượt 2 khối thì dừng, đề xuất tách khung đăng nhập và `/dang-nhap` thành hai phase.

## Dừng và hỏi tôi nếu (ngoài điều kiện trong CLAUDE.md)
- Muốn đổi câu chữ hay trường của form đăng nhập.
- Bỏ Bootstrap khỏi `/dang-nhap` làm hỏng một phép kiểm có sẵn.

## Kết thúc
Báo cáo theo mẫu CLAUDE.md → cập nhật docs/STATUS.md → commit `phase-13: khung dang nhap no ra tu nut, trang dang nhap Hanzo`.
