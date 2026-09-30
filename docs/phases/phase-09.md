# GÓI LỆNH — vnpt-billing — phase-09: Đổi tên sang Hanzo + mốc dữ liệu

Thư mục: D:\KGU\TTNN\APP\vnpt-billing
Nhánh: `phase-09-doi-ten-hanzo`
Đọc trước: CLAUDE.md (mục Pipeline, Invariants), docs/STATUS.md, spec `docs/superpowers/specs/2026-09-30-landing-hanzo-design.md` mục 4.1, kế hoạch `docs/superpowers/plans/2026-09-30-landing-hanzo.md` **Task 1**
Bước workflow: B5 (build) · **làm thẳng, không `/ship`** (dưới 30 phút, theo `D:\Work\hanzo-studio\pipeline\README.md`)
Giao diện: KHONG

## Mục tiêu (1 câu)
Sau phase này, mọi chứng từ (hóa đơn PDF, phiếu thu PDF, màn chi tiết hóa đơn) và thanh máy đều mang tên **Hanzo** / **Công ty Cổ phần Viễn thông Hanzo**, và có một mốc dữ liệu đo trước cho cả đợt.

## Phạm vi
Làm:
- Ghi mốc dữ liệu trước khi đụng code (kế hoạch Task 1 bước 1).
- Đổi tên ở 4 chỗ mã nguồn + 3 phép kiểm PDF + `CLAUDE.md` + `README.md` + tài liệu hiện trạng.
- Thêm một dòng ghi chú đầu các báo cáo lịch sử (`docs/PHASE-*`, `docs/G*-REPORT.md`).

KHÔNG làm (dù thấy cần):
- Không đổi `data-mau.sql:109` (*"Lô B2 KCN Sông Hậu…"* là địa chỉ khách, không phải tên nhà mạng).
- Không đổi màu, phông, bố cục; không đụng landing.
- Không đổi tên thư mục, package Java (`vnpt`, `com.hanzo.billing` giữ nguyên).
- Không đổi tên, biểu tượng lối tắt Desktop.

## Bối cảnh cần biết
- Chỗ đổi: `fragments/layout.html:85`, `HoaDonPdfService.java:110`, `PhieuThuPdfService.java:51`, `hoa-don/chi-tiet.html:44`.
- Phép kiểm: `HoaDonPdfServiceTest.java:144`, `PhieuThuPdfServiceTest.java:73`, `PhieuThuPdfTaiLieuThatTest.java:75`.
- PDF in chữ hoa: `CÔNG TY CỔ PHẦN VIỄN THÔNG HANZO`. HTML: `Công ty Cổ phần Viễn thông Hanzo`. Thanh máy: `Hanzo · Quản lý cước`.
- Mã số thuế `1800000000`, tổng đài `1800 6060` giữ nguyên (số bịa).

## Việc cụ thể, theo thứ tự
1. Ghi mốc dữ liệu bằng câu SQL ở kế hoạch Task 1 bước 1. Lệch kỳ vọng thì **dừng**.
2. Sửa 3 phép kiểm PDF: `contains("VIỄN THÔNG HANZO")`, `doesNotContain("SÔNG HẬU")`, `doesNotContain("VNPT")`. Chạy → phải **đỏ**.
3. Đổi tên ở 4 chỗ. Chạy lại → **xanh**.
4. `grep -rn "Sông Hậu\|SÔNG HẬU" src/main src/test` → chỉ còn `data-mau.sql:109`.
5. Sửa `CLAUDE.md` (luật đơn vị phát hành hư cấu; thêm dòng trạng thái chuyển tiếp dưới luật *"Bảng màu chỉ có MỘT nguồn"*), `README.md`, các tài liệu hiện trạng; thêm dòng ghi chú đầu báo cáo lịch sử (câu chữ ở kế hoạch Task 1 bước 7).

## Tiêu chí xong (tự kiểm, phải kèm bằng chứng)
- [ ] Mốc dữ liệu: `ky=7 cdr=23223 hd=338 cthd=753 tt=161 ky9=MO/0/0` · bất biến `0/0/0/0` (dán kết quả truy vấn)
- [ ] 3 phép kiểm PDF: đỏ trước khi sửa code, xanh sau khi sửa (dán cả hai lần chạy)
- [ ] `.\mvnw.cmd -B -ntp clean test` → 343 test, 0 lỗi (dừng app trước khi chạy)
- [ ] `grep` bước 4 chỉ còn đúng một dòng
- [ ] Không đổi tệp ngoài: 4 tệp mã, 3 tệp test, `CLAUDE.md`, `README.md`, `docs/**`

## Skill dùng (theo bảng trong CLAUDE.md)
superpowers:test-driven-development → superpowers:verification-before-completion

## Giới hạn
30 phút. Vượt thì dừng, báo lý do.

## Dừng và hỏi tôi nếu (ngoài điều kiện trong CLAUDE.md)
- Mốc dữ liệu lệch ở bước 1.
- `grep` tìm thấy tên cũ ở chỗ nào khác ngoài danh sách trên.

## Kết thúc
Báo cáo theo mẫu CLAUDE.md → cập nhật docs/STATUS.md → commit `phase-09: doi ten Song Hau thanh Hanzo, ghi moc du lieu`.
