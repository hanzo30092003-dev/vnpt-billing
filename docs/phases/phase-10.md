# GÓI LỆNH — vnpt-billing — phase-10: Đích chuyển hướng đăng nhập + LandingService

Thư mục: D:\KGU\TTNN\APP\vnpt-billing
Nhánh: `phase-10-dang-nhap-landing-service`
Đọc trước: CLAUDE.md (Pipeline, Invariants), docs/STATUS.md, docs/LOI-DA-GAP.md, spec mục 3.3 và 5.3, kế hoạch **Task 2** và **Task 3**
Bước workflow: B5 + B6 · chạy bằng `/ship docs/phases/phase-10.md`
Giao diện: KHONG

## Mục tiêu (1 câu)
Sau phase này, đăng nhập sai từ landing được đưa về `/?loi` hoặc `/?khoa=N` qua whitelist an toàn, đăng xuất về `/?dathoat`, và có `LandingService` cho 4 số đếm cùng ví dụ tính cước tính bằng đúng hàm của engine.

## Phạm vi
Làm:
- `XuLyDangNhap.dichKhiThatBai(String duongDanTrangDangNhap, String nguon)` + hằng `NGUON_LANDING = "landing"`; gọi ở dòng `sendRedirect` của `onAuthenticationFailure`.
- `SecurityConfig`: `logoutSuccessUrl("/?dathoat")`.
- `dto/landing/SoLieuLanding`, `dto/landing/ViDuTinhCuoc`, `service/landing/LandingService` đúng chữ ký ở kế hoạch Task 3 (mục Interfaces).

KHÔNG làm (dù thấy cần):
- Không mở `GET /` cho người chưa đăng nhập, không tạo template landing (phase-11).
- Không sửa `onAuthenticationSuccess`, không đổi luật khoá tạm (5 lần sai, 15 phút).
- Không đụng `pom.xml`, `application.yml`, `db/**`.

## Bối cảnh cần biết
- Handler thất bại hiện dựng `duongDan = "/dang-nhap?loi"` hoặc `"/dang-nhap?khoa=" + N` rồi `sendRedirect`.
- Tham số `nguon` do trình duyệt gửi lên → **không tin**; chỉ so bằng đúng chuỗi `landing`, **không bao giờ** dùng làm đích.
- Ví dụ tính cước: gói `CB01`, `THOAI` · `NOI_MANG` · giờ thường · 134 giây. Tra giá bằng `BangGiaLookup.nap().tim(...)`, số khối bằng `DonViCuoc.soBlock`, làm tròn bằng `ThamSoTinhCuoc.lamTronTien`, thuế tính giống `BillingService:163` (`lamTronTien(tongTruocThue × thamSo.getThueSuatVat())`).
- Bảng giá thật: dòng chung, khối 6 giây, 15 đ/khối. **Dự đoán công bố trước:** 23 khối · 345 đ · trước thuế 50.345 · thuế 5.035 · tổng **55.380 đ**.
- Khuôn test MVC có bảo mật: xem `TinhCuocControllerTest` (`webAppContextSetup` + `springSecurity()`, hàm `nguoiDung(VaiTro)`).

## Việc cụ thể, theo thứ tự
1. Test thuần `XuLyDangNhapDichTest` (kế hoạch Task 2 bước 1): landing → `/?loi`, `/?khoa=15`; không `nguon` → giữ nguyên; 7 giá trị giả mạo (`https://gia-mao.vn`, `//gia-mao.vn`, `Landing`, `" landing"`, `"landing "`, `/`, rỗng) → giữ nguyên `/dang-nhap?loi`.
2. Test tích hợp đích chuyển hướng (Task 2 bước 2), **chỉ dùng tên đăng nhập không tồn tại** `khong_ton_tai_l2`; đăng xuất → `/?dathoat`.
3. Test thuần `LandingServiceTest` (Task 3 bước 1): dự đoán 23 / 345 / 50.345 / 5.035 / 55.380; đối chứng âm đơn giá 20 đ → khác 345.
4. Test tích hợp `LandingServiceTichHopTest` (Task 3 bước 2): `soLieu()` bằng 4 `count()`; `viDuTinhCuoc()` bằng `tinh(...)` với dòng giá tra thật; `viDuTinhCuoc("KHONG_CO_GOI_NAY")` → `Optional.empty()`.
5. Cài đặt theo chữ ký trong kế hoạch.

## Tiêu chí xong (tự kiểm, phải kèm bằng chứng)
- [ ] Mọi test mới đỏ trước khi cài, xanh sau khi cài
- [ ] `.\mvnw.cmd -B -ntp clean test` pass toàn bộ, tổng số test tăng so với 343 (ghi số)
- [ ] Số thật của ví dụ khớp dự đoán 23 / 345 / 55.380 — lệch thì dừng phân tích, không sửa test cho khớp
- [ ] Không test nào đăng nhập sai bằng tài khoản có thật (`grep "khong_ton_tai"` trong test mới)
- [ ] Mốc dữ liệu y nguyên sau khi chạy test (câu SQL ở kế hoạch Task 1 bước 1)
- [ ] Không đổi tệp ngoài: `XuLyDangNhap.java`, `SecurityConfig.java`, `dto/landing/**`, `service/landing/**`, test tương ứng

## Skill dùng (theo bảng trong CLAUDE.md)
superpowers:test-driven-development → superpowers:verification-before-completion · springboot-patterns (skill của dự án)

## Giới hạn
90 phút. Vượt 2 khối thì dừng, đề xuất tách phase.

## Dừng và hỏi tôi nếu (ngoài điều kiện trong CLAUDE.md)
- Bảng giá thật cho CB01 / thoại / nội mạng / giờ thường không còn là khối 6 giây · 15 đ.
- Phải sửa `SecurityConfig` ở chỗ nào khác ngoài `logoutSuccessUrl`.

## Kết thúc
Báo cáo theo mẫu CLAUDE.md → cập nhật docs/STATUS.md → commit `phase-10: dich chuyen huong dang nhap co whitelist, LandingService`.
