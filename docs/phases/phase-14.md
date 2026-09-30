# GÓI LỆNH — vnpt-billing — phase-14: Ảnh AI + nghiệm thu landing

Thư mục: D:\KGU\TTNN\APP\vnpt-billing
Nhánh: `phase-14-anh-ai-nghiem-thu`
Đọc trước: CLAUDE.md (Pipeline, Invariants), docs/STATUS.md, docs/LOI-DA-GAP.md, spec mục 5.4, 6.4, 7, kế hoạch **Task 10** và **Task 11**
Bước workflow: B5 + B6 (nghiệm thu toàn bộ spec) · chạy bằng `/ship docs/phases/phase-14.md`
Giao diện: CO — URL: `http://localhost:8080/` ở 375×812 và 1440×900, sáng và tối

## Mục tiêu (1 câu)
Sau phase này, landing có hai ảnh nền không khí tự host, và mọi tiêu chí nghiệm thu của spec mục 7 đều có kết quả Đạt/Không đạt kèm bằng chứng trong `docs/test-report.md`.

## Phạm vi
Làm:
- Sinh 2 ảnh bằng Canva `generate-image` (tải công cụ qua ToolSearch) theo spec 5.4; chuyển WebP bằng canvas của Edge qua Playwright (`toDataURL('image/webp', 0.82)`); lưu `static/images/landing/{quay-giao-dich,nen-loi-moi}.webp`; gắn vào `landing.html`.
- `scripts/test-auth.ps1` mục 2 viết lại (lưu kèm BOM UTF-8), có đối chứng âm (kế hoạch Task 11 bước 1).
- Cài `lighthouse` **cục bộ** vào `tools-chup-anh/` (`npm install lighthouse --save-dev`, **không** `-g`); chạy mobile 3 lần với `CHROME_PATH` trỏ tới Edge, lấy trung vị.
- Kiểm ngoại tuyến (chặn mọi địa chỉ ngoài `localhost`, dựa trên `tools-chup-anh/thu-mat-mang.mjs`), Taste mục 14 Pre-Flight Check, `accessibility-review` của hồ sơ A.
- `docs/test-report.md` (mới), `docs/STATUS.md`.

KHÔNG làm (dù thấy cần):
- Không dùng ảnh stock, ảnh giữ chỗ hay logo của nhà mạng thật.
- Không chụp lại 67 ảnh báo cáo, không sửa chương giao diện của báo cáo (sau spec 46 màn hình).
- Không sửa code để "lách" Lighthouse (ví dụ tắt chuyển động khi phát hiện công cụ đo).

## Bối cảnh cần biết
- Ảnh 1 · 1600×1000: một nhân viên quầy giao dịch viễn thông đang tiếp khách, ánh sáng ban ngày, tông trung tính; không logo, không màu thương hiệu, không chữ đọc được. Dùng cho cả ba thẻ vai trò bằng `object-position` khác nhau; có `alt` mô tả.
- Ảnh 2 · 1800×600: mặt giấy chứng từ và bóng một cột phát sóng mờ, tương phản thấp; `loading="lazy"`, `alt=""`.
- Mỗi ảnh ≤ 200 KB, có `width`/`height`.
- Mốc Lighthouse theo hồ sơ A: Performance ≥ 90, Accessibility ≥ 95, SEO ≥ 95.
- Đường dẫn Edge: `C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe`.

## Việc cụ thể, theo thứ tự
1. Sinh ảnh, duyệt bằng mắt; không đạt sau 3 lần thì **dừng và hỏi**. Chuyển WebP, gắn vào trang, chạy lại `landing-kiem.mjs` (tràn ngang 0, không lỗi JS).
2. Viết lại mục 2 `test-auth.ps1`; chứng minh đỏ (tạm đổi chuỗi cấm thành chuỗi có trên trang) rồi xanh.
3. Dừng app → `.\mvnw.cmd -B -ntp clean test`; ba script Python; chạy app → 8 script trong `scripts/`.
4. Lighthouse, ngoại tuyến, Pre-Flight Check, `accessibility-review`.
5. Đo lại mốc dữ liệu; viết `docs/test-report.md` theo từng dòng của spec 7.1–7.4.

## Tiêu chí xong (tự kiểm, phải kèm bằng chứng)
- [ ] 2 ảnh WebP ≤ 200 KB, đúng kích thước, đã duyệt bằng mắt
- [ ] `test-auth.ps1` mục 2: đỏ khi phá tạm, xanh khi hoàn lại
- [ ] `mvnw test` pass toàn bộ (ghi tổng số, dự kiến khoảng 352); 3 script Python ĐẠT; số phép kiểm đạt của 8 script không giảm
- [ ] Lighthouse mobile trung vị 3 lần: Performance ≥ 90 · Accessibility ≥ 95 · SEO ≥ 95 (dán số)
- [ ] Ngoại tuyến: ảnh chụp giống lúc có mạng
- [ ] `docs/test-report.md` có ảnh 375/1440 × sáng/tối và mọi dòng spec 7.1–7.4 kèm Đạt/Không đạt
- [ ] Mốc dữ liệu y nguyên: 7 · 23.223 · 338 · 753 · 161 · kỳ 9 rỗng `MO` · bất biến 0/0/0/0

## Skill dùng (theo bảng trong CLAUDE.md)
superpowers:verification-before-completion (chính) · hồ sơ A: `accessibility-review`, `design-taste-frontend` mục 14 · /code-review trên toàn nhánh trước khi báo xong

## Giới hạn
90 phút. Vượt 2 khối thì dừng, đề xuất tách ảnh AI và nghiệm thu thành hai phase.

## Dừng và hỏi tôi nếu (ngoài điều kiện trong CLAUDE.md)
- Canva không sinh được ảnh đạt điều kiện sau 3 lần.
- Lighthouse dưới mốc mà cách sửa đụng tới thiết kế đã duyệt (bỏ cảnh ghim, bỏ phông, bỏ ảnh).
- Số phép kiểm đạt của 8 script giảm.

## Kết thúc
Báo cáo theo mẫu CLAUDE.md → cập nhật docs/STATUS.md (xong spec landing; việc tiếp theo: spec 46 màn hình) → commit `phase-14: anh AI va nghiem thu landing Hanzo`.
