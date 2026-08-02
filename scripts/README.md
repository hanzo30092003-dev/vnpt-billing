# Script kiểm thử giao diện

Ba script PowerShell kiểm thử các luồng nghiệp vụ qua HTTP thật, bổ sung cho unit test
trong `src/test/java`. Unit test kiểm tra logic nghiệp vụ ở tầng service; các script này
kiểm tra cả chuỗi controller → template → bảo mật, tức những thứ unit test không chạm tới.

## Yêu cầu trước khi chạy

1. MySQL đang chạy và biến `MYSQL_PASSWORD` đã đặt
2. Ứng dụng đang chạy tại `http://localhost:8080`
3. CSDL ở trạng thái dữ liệu mẫu — nên chạy profile `reset` trước để kết quả ổn định:

```bash
mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"
```

## Danh sách script

| Script | Kiểm gì | Có ghi dữ liệu? |
|---|---|---|
| `test-auth.ps1` | Đăng nhập, đăng xuất, phân quyền 3 vai trò, trang 403, sidebar theo vai trò | Không |
| `test-kh.ps1` | Danh sách/lọc/phân trang khách hàng, validation CCCD và MST, trùng giấy tờ, chặn ngừng giao dịch | Không (mọi ca đều là ca bị chặn) |
| `test-tb.ps1` | Danh sách/lọc thuê bao, validation đăng ký, 4 tab chi tiết, lịch sử biến động, chặn khôi phục thuê bao đã thanh lý | Không |
| `_chung.ps1` | Hàm dùng chung, không chạy trực tiếp | — |

## Cách chạy

```bash
powershell -ExecutionPolicy Bypass -File scripts\test-auth.ps1
```

```bash
powershell -ExecutionPolicy Bypass -File scripts\test-kh.ps1
```

```bash
powershell -ExecutionPolicy Bypass -File scripts\test-tb.ps1
```

Mỗi script in ra `[DAT ]` / `[SAI ]` cho từng phép kiểm và tổng kết ở cuối. Script
thoát với mã 1 nếu có phép kiểm nào sai, nên dùng được trong pipeline CI.

## Nguyên tắc quan trọng: xét mã trạng thái HTTP trước

Hàm `Kiem-Tra` trong `_chung.ps1` **bắt buộc kiểm tra mã trạng thái HTTP trước**, chỉ
khi status đúng như mong đợi mới dò đến nội dung HTML.

Đây là bài học rút ra từ sự cố ở Phase 2 (ghi trong `docs/PHASE-2-REPORT.md` mục 6.2).
Phiên bản cũ của các script chỉ dò chuỗi trong HTML trả về. Khi form khách hàng bị lỗi
HTTP 500, bài kiểm thử vẫn "đậu" vì:

- Trang lỗi 500 cũng dùng layout chung nên vẫn chứa các chuỗi đang tìm
- Trang lỗi còn chứa cả CSRF token của form đăng xuất, nên bước POST tiếp theo vẫn
  lấy được token và chạy được

Kết quả là một lỗi 500 nằm im qua nhiều vòng kiểm thử. Vì vậy công cụ đã mắc lỗi này
phải được sửa lại chính nó.

Hàm `Post-Form` cũng được siết theo hướng đó: nếu trang dùng để lấy CSRF token trả về
mã khác 200 thì báo lỗi ngay, thay vì âm thầm lấy token từ một trang lỗi.

## Lưu ý về mã hoá file

Các file `.ps1` chứa tiếng Việt **phải lưu kèm BOM UTF-8**. PowerShell 5.1 đọc file
`.ps1` không có BOM bằng bảng mã ANSI của hệ thống (`windows-1258` trên máy phát triển),
khiến toàn bộ chuỗi tiếng Việt bị hỏng và script không chạy được.
