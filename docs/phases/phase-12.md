# GÓI LỆNH — vnpt-billing — phase-12: Giao diện và chuyển động landing

Thư mục: D:\KGU\TTNN\APP\vnpt-billing
Nhánh: `phase-12-landing-giao-dien-chuyen-dong`
Đọc trước: CLAUDE.md (Pipeline, Invariants), docs/STATUS.md, docs/LOI-DA-GAP.md, spec mục 4.2–4.5, 5.1, 6.2, 6.3, kế hoạch **Task 6** và **Task 7**; bản thử đã duyệt `.superpowers/brainstorm/1339-1790779995/content/demo-a-cuon-phim.html` (chỉ tham khảo nhịp chuyển động, không chép màu hay số)
Bước workflow: B5 + B6 · chạy bằng `/ship docs/phases/phase-12.md`
Giao diện: CO — URL: `http://localhost:8080/` ở 375×812, 683×768, 1440×900; sáng và tối

## Mục tiêu (1 câu)
Sau phase này, landing ở `/` có đủ giao diện theo bộ nhận diện Hanzo (sáng, tối, điện thoại) và chuyển động năm cảnh bằng GSAP. Tắt JS hoặc bật giảm chuyển động thì nội dung vẫn đủ.

## Phạm vi
Làm:
- `static/css/landing.css` theo spec 5.1: thanh nổi dạng viên thuốc, nút *nút trong nút*, khoảng trắng dọc ≥ 96px, lớp hạt giấy cố định (độ mờ 0,03), lưới lệch cỡ cảnh 3, khối nền mực cảnh 4 (tối dùng `--hz-the`), một cột dưới 768px.
- `static/js/landing.js` cho cảnh 1–5 (kế hoạch Task 7 bước 1–3); script nhỏ đầu trang gắn lớp `js` lên `<html>`; ba tệp GSAP và `landing.js` có `defer`.
- `tools-chup-anh/landing-kiem.mjs` (công cụ cá nhân, đã `.gitignore`) theo kế hoạch Task 6 bước 1, Task 7 bước 4–5.

KHÔNG làm (dù thấy cần):
- Không làm khung đăng nhập nở ra (phase-13); nút *Đăng nhập* phase này vẫn là liên kết `/dang-nhap`.
- Không định nghĩa màu mới ngoài `tokens.css`; không gõ mã màu trong template.
- Không thêm thư viện JS nào ngoài ba tệp GSAP đã tự host.

## Bối cảnh cần biết
- Mọi tween nằm trong `gsap.matchMedia()` với hai nhánh `(min-width: 768px) and (prefers-reduced-motion: no-preference)` và `(max-width: 767px) and (prefers-reduced-motion: no-preference)`. Không có nhánh `reduce`.
- LCP là tiêu đề: SplitText chạy sau `document.fonts.ready`, ký tự đi lên từ `yPercent: 100`, **không** đặt `opacity: 0` cho tiêu đề.
- `[data-dem]` đã in sẵn giá trị thật từ máy chủ; JS chỉ đếm từ 0 lên đúng giá trị đó (`Intl.NumberFormat('vi-VN')`).
- Cảnh 2 (≥ 768px): ghim `#canh-hanh-trinh`, `end: '+=2800'`, `scrub: 1`, trình tự CDR → giá → kết quả → hóa đơn in dần (`clipPath`) → con dấu. Dưới 768px: không ghim.
- Không dùng `window.addEventListener('scroll')`. Chỉ tạo chuyển động bằng `transform`/`opacity`; `backdrop-filter` chỉ cho thanh nổi.
- Bài học từ bản thử: dải kéo ngang cảnh 4 từng làm trang tràn ngang 78px — phải đo tràn ngang ở mọi khổ.

## Việc cụ thể, theo thứ tự
1. Viết `landing-kiem.mjs` (tắt JS, bốn tổ hợp khổ × chế độ) → chạy trên landing chưa có CSS để có mốc.
2. Viết `landing.css` → chạy lại: tràn ngang 0, nút *Đăng nhập* trong khung nhìn đầu ở 375px; thêm tổ hợp 683×768 (Review Focus 4).
3. Viết `landing.js` → mở rộng công cụ: bật JS, cuộn 8 mốc, bắt `pageerror`/`console.error`; chạy thêm `reducedMotion: 'reduce'`.
4. Review Focus 1: chặn `/vendor/gsap/**` → mọi `[data-dem]` hiện giá trị thật, bốn trạm và hóa đơn hiện đủ, nút sang `/dang-nhap`.

## Tiêu chí xong (tự kiểm, phải kèm bằng chứng)
- [ ] Tràn ngang = 0 ở 375, 683, 1440 × sáng, tối (dán `ket-qua-landing.json`)
- [ ] Nút *Đăng nhập* nằm trong khung nhìn đầu ở 375px
- [ ] Bật JS: 0 `pageerror`, 0 `console.error` qua 8 mốc cuộn; ảnh chụp cảnh 2 thấy con dấu *ĐÃ THU*
- [ ] `reducedMotion: 'reduce'`: không có `pin-spacer`, hóa đơn và con dấu hiện đầy đủ
- [ ] Chặn GSAP: nội dung đủ, số liệu đúng giá trị thật
- [ ] Ảnh 375/1440 × sáng/tối lưu kèm báo cáo phase
- [ ] `.\mvnw.cmd -B -ntp clean test` vẫn pass; `kiem-giao-dien.py`, `kiem-ban-phim.py` ĐẠT; mốc dữ liệu y nguyên

## Skill dùng (theo bảng trong CLAUDE.md)
Hồ sơ A (`D:/Work/hanzo-studio/skill-profiles/profiles/A-landing/PROFILE.md`): `design-taste-frontend` (mục 0 Design Read, mục 14 Pre-Flight Check) + `high-end-visual-design` → superpowers:verification-before-completion

## Giới hạn
90 phút. Vượt 2 khối thì dừng, đề xuất tách thành phase giao diện và phase chuyển động.

## Dừng và hỏi tôi nếu (ngoài điều kiện trong CLAUDE.md)
- Cần màu hay cỡ chữ nằm ngoài `tokens.css` và spec 4.2–4.3.
- Không giữ được tràn ngang = 0 ở cảnh 4 mà không bỏ kéo ngang.

## Kết thúc
Báo cáo theo mẫu CLAUDE.md → cập nhật docs/STATUS.md → commit `phase-12: giao dien va chuyen dong nam canh landing`.
