# STATUS · vnpt-billing (Hanzo)

**Cập nhật:** 01/10/2026 · tạo bằng prompt "Khởi động dự án cũ" (không sửa code)

## Bước hiện tại

**B4 · chia phase** của đợt làm lại giao diện (vòng lặp mới, sau Phase 0–8).

| Bước | Trạng thái |
|---|---|
| B1–B2 · vấn đề, phạm vi | Có sẵn trước khi áp quy trình, nằm trong `docs/PHASE-*-PLAN.md`, `docs/RA-SOAT-TINH-NANG.md`; không có `01-van-de.md`, `02-mvp.md` |
| B3 · thiết kế | ✅ Đợt làm lại giao diện: `docs/superpowers/specs/2026-09-30-landing-hanzo-design.md` + `docs/adr/0001-nhan-dien-hanzo.md` |
| B4 · chia phase | 🟡 Đang làm: kế hoạch `docs/superpowers/plans/2026-09-30-landing-hanzo.md` → tách thành `docs/phases/phase-09.md` … `phase-14.md` |
| B5–B6 · dựng, kiểm | Phase 0–8 xong (343 test, 8 script). Đợt landing chưa bắt đầu |
| B7 · tài liệu | Một phần: `huong-dan-su-dung.md`, `kich-ban-demo.md`, `kich-ban-kiem-thu.md`, 67 ảnh chụp. **Chưa thấy tệp báo cáo Word trong kho** |
| B8 · giao | Chưa: nộp báo cáo + đĩa CD theo `docs/RA-SOAT-TRUOC-KHI-NOP.md` |

## Đã có

- Phần mềm chạy đủ 6 phân hệ cam kết, cộng quản trị người dùng và nhật ký thao tác. 343 test đạt (30/09/2026).
- Dữ liệu: 7 kỳ · 23.223 CDR · 338 hóa đơn · 753 chi tiết · 161 thanh toán · kỳ 9/2026 rỗng và `MO` · 4 bất biến 0 lệch (đo 18/09/2026, đợt G10).
- Mở phần mềm bằng một cú nháy đúp (`khoi-dong/`); Bước 3 kiểm kết nối MySQL thật (G9).
- Spec và kế hoạch cho landing Hanzo đã được HANZO duyệt (30/09/2026).

## Thiếu

- Landing page, bộ nhận diện Hanzo, 46 màn hình theo nhận diện mới (hai dự án con).
- Dây chuyền `/ship` chưa cài vào dự án (đang cài).
- Tệp báo cáo Word nộp giảng viên (chưa thấy trong kho).
- 67 ảnh chụp và các chương giao diện của báo cáo sẽ lệch sau khi đổi giao diện → chụp lại sau dự án con thứ hai.

## Việc tiếp theo

Cài `/ship`, điền mục Pipeline và Invariants vào `CLAUDE.md`, rồi viết `docs/phases/phase-09.md` … `phase-14.md` từ kế hoạch.

## Vướng mắc, cần nhớ

- Test chạy trên **CSDL thật** `vnpt_billing` (không có CSDL test riêng). Dừng app trước `mvnw test`; đo lại mốc dữ liệu sau mỗi phase.
- Mật khẩu MySQL của máy đã đặt lại ở đợt G10. Nó chỉ nằm trong biến môi trường `MYSQL_PASSWORD` (User), không nằm trong tệp nào.
- Bản sao lưu CSDL `D:\backup-mysql-truoc-G10` (234 tệp, 514,66 MB) vẫn giữ.
- Việc nhỏ còn treo: Bước 1 của bộ khởi động coi mọi phản hồi HTTP là phần mềm đã chạy; tên và biểu tượng lối tắt Desktop chưa đổi sang Hanzo.
- Cổng 8080 từng bị dự án QLHS (mở từ VS Code) chiếm; mở phần mềm thấy mất CSS thì kiểm cổng trước.

## File đang dở

Không có.
