# STATUS · vnpt-billing (Hanzo)

**Cập nhật:** 01/10/2026 · xong phase-09, đã merge vào `main` (chưa push)

## Bước hiện tại

**B5 · dựng** của đợt làm lại giao diện (vòng lặp mới, sau Phase 0–8). B4 đã xong.

| Bước | Trạng thái |
|---|---|
| B1–B2 · vấn đề, phạm vi | Có sẵn trước khi áp quy trình, nằm trong `docs/PHASE-*-PLAN.md`, `docs/RA-SOAT-TINH-NANG.md`; không có `01-van-de.md`, `02-mvp.md` |
| B3 · thiết kế | ✅ Đợt làm lại giao diện: `docs/superpowers/specs/2026-09-30-landing-hanzo-design.md` + `docs/adr/0001-nhan-dien-hanzo.md` |
| B4 · chia phase | ✅ `docs/phases/phase-09.md` … `phase-14.md` (từ kế hoạch `docs/superpowers/plans/2026-09-30-landing-hanzo.md`) |
| B5–B6 · dựng, kiểm | Phase 0–8 xong (343 test, 8 script). Đợt landing: **phase-09 ✅** (đổi tên Hanzo), phase-10 → 14 chưa làm |
| B7 · tài liệu | Một phần: `huong-dan-su-dung.md`, `kich-ban-demo.md`, `kich-ban-kiem-thu.md`, 67 ảnh chụp. **Chưa thấy tệp báo cáo Word trong kho** |
| B8 · giao | Chưa: nộp báo cáo + đĩa CD theo `docs/RA-SOAT-TRUOC-KHI-NOP.md` |

## Đã có

- Phần mềm chạy đủ 6 phân hệ cam kết, cộng quản trị người dùng và nhật ký thao tác. 343 test đạt (30/09/2026).
- Dữ liệu: 7 kỳ · 23.223 CDR · 338 hóa đơn · 753 chi tiết · 161 thanh toán · kỳ 9/2026 rỗng và `MO` · 4 bất biến 0 lệch (đo lại 01/10/2026 trước và sau phase-09).
- Tên phần mềm và nhà mạng hư cấu đã là **Hanzo** (phase-09); PDF bị 3 phép kiểm cấm `VNPT` và `SÔNG HẬU`.
- Mở phần mềm bằng một cú nháy đúp (`khoi-dong/`); Bước 3 kiểm kết nối MySQL thật (G9).
- Spec và kế hoạch cho landing Hanzo đã được HANZO duyệt (30/09/2026).

## Thiếu

- Landing page, bộ nhận diện Hanzo, 46 màn hình theo nhận diện mới (hai dự án con).
- Tệp báo cáo Word nộp giảng viên (chưa thấy trong kho).
- 67 ảnh chụp và các chương giao diện của báo cáo sẽ lệch sau khi đổi giao diện → chụp lại sau dự án con thứ hai.

## Việc tiếp theo

**phase-10:** mở phiên Claude Code **mới trong thư mục `vnpt-billing`** (để nạp `/ship`), từ `main` tạo nhánh `phase-10-dang-nhap-landing-service`, gõ `/ship docs/phases/phase-10.md`.

## Vướng mắc, cần nhớ

- Test chạy trên **CSDL thật** `vnpt_billing` (không có CSDL test riêng). Dừng app trước `mvnw test`; đo lại mốc dữ liệu sau mỗi phase.
- Mật khẩu MySQL của máy đã đặt lại ở đợt G10. Nó chỉ nằm trong biến môi trường `MYSQL_PASSWORD` (User), không nằm trong tệp nào.
- Bản sao lưu CSDL `D:\backup-mysql-truoc-G10` (234 tệp, 514,66 MB) vẫn giữ.
- Việc nhỏ còn treo: Bước 1 của bộ khởi động coi mọi phản hồi HTTP là phần mềm đã chạy; tên và biểu tượng lối tắt Desktop chưa đổi sang Hanzo.
- Cổng 8080 từng bị dự án QLHS (mở từ VS Code) chiếm; mở phần mềm thấy mất CSS thì kiểm cổng trước.
- 67 ảnh chụp trong `docs/screenshots/` vẫn mang tên cũ *Sông Hậu* trên thanh máy — đúng, sẽ chụp lại sau spec 46 màn hình.

## File đang dở

Không có.
