# Script kiểm thử giao diện

Năm script PowerShell kiểm thử các luồng nghiệp vụ qua HTTP thật — tổng **94 phép kiểm** —
bổ sung cho unit test trong `src/test/java`. Unit test kiểm tra logic nghiệp vụ ở tầng
service; các script này kiểm tra cả chuỗi controller → template → bảo mật, tức những thứ
unit test không chạm tới.

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
| `test-muc-F.ps1` | Công nợ, bảng tuổi nợ, danh sách thanh toán; chốt chặn huỷ hóa đơn kỳ đã thu tiền | **Có** — huỷ rồi lập lại hóa đơn kỳ 6 |
| `test-bao-cao.ps1` | Dashboard và bảy báo cáo, 13 con số đối chiếu chéo bằng SQL, 11 file Excel, báo cáo trên kỳ rỗng | **Có** — tạo rồi xoá một kỳ thử |
| `chay-ky-moi-phase6.ps1` | Dựng kỳ 3, 4, 7/2026: tạo kỳ → sinh CDR → tính cước → lập hóa đơn → chốt | **Có** — chỉ chạy một lần ở Phase 6 |
| `_chung.ps1` | Hàm dùng chung, không chạy trực tiếp | — |

> ⚠️ Ba script cuối **ghi vào CSDL**. `test-muc-F.ps1` và `test-bao-cao.ps1` tự trả dữ liệu về
> như cũ, nhưng ID hóa đơn kỳ 6 sẽ đổi sau vòng huỷ/lập — chạy profile `reset` sau đó nếu cần
> khớp lại đúng `data-van-hanh.sql`.

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

## Nguyên tắc thứ hai: đừng buộc phép kiểm vào chuỗi hiển thị

Rút ra ở Phase 6 sau hai lần báo động giả liên tiếp (`PHASE-6-REPORT.md` mục 16.1):

- `test-auth.ps1` từng kiểm phân quyền sidebar bằng cách dò **chữ** `'Hóa đơn'` trên cả trang.
  Nó đúng *tình cờ*, vì trang chủ cũ không chứa chữ đó. Trang chủ thành dashboard → chữ ấy xuất
  hiện hợp lệ trong thân trang → phép kiểm đỏ oan. Nay dò `href="/hoa-don"`, chính xác tới mức
  không nhầm với `href="/hoa-don/307"` của bảng dữ liệu.
- `test-muc-F.ps1` từng ghim cứng `112` hóa đơn. Phase 6 thêm ba kỳ thành 280 → đỏ oan dù bất
  biến hoàn toàn sạch. Nay lấy số hóa đơn **từ CSDL**, vì bất biến nói về *quan hệ giữa các cột*
  chứ không nói gì về *số lượng dòng*.

Quy tắc chung: **buộc phép kiểm vào thứ nó thật sự nói về** — `id`, `href`, giá trị đọc lại từ
CSDL — chứ không vào câu chữ hay vào quy mô dữ liệu. Một phép kiểm đỏ oan vài lần thì lần sau
không ai tin nó nữa, và đó mới là thiệt hại thật.

## Lưu ý về mã hoá file

Các file `.ps1` chứa tiếng Việt **phải lưu kèm BOM UTF-8**. PowerShell 5.1 đọc file
`.ps1` không có BOM bằng bảng mã ANSI của hệ thống (`windows-1258` trên máy phát triển),
khiến toàn bộ chuỗi tiếng Việt bị hỏng và script không chạy được.
