# ADR 0001 · Bộ nhận diện Hanzo, chốt một lần cho cả landing và phần mềm

- **Ngày:** 30/09/2026
- **Trạng thái:** đã chấp nhận
- **Spec liên quan:** [`docs/superpowers/specs/2026-09-30-landing-hanzo-design.md`](../superpowers/specs/2026-09-30-landing-hanzo-design.md)

## Bối cảnh

Đợt làm lại giao diện gồm hai dự án con: landing page mới, rồi 46 màn hình nghiệp vụ. Phần mềm đang có một bộ nhận diện riêng (nền mực, nhấn xanh mòng két, phông Be Vietnam Pro, tên *Sông Hậu*). Nếu mỗi dự án con tự chọn màu, phông và tên, thì hoặc phải làm lại hai lần, hoặc bấm *Đăng nhập* xong người dùng sẽ thấy như sang một ứng dụng khác.

## Quyết định

Chốt **một** bộ nhận diện ngay ở dự án con thứ nhất; dự án con thứ hai kế thừa, không chọn lại.

- **Tên:** phần mềm là **Hanzo**; nhà mạng hư cấu phát hành chứng từ là **Công ty Cổ phần Viễn thông Hanzo** (thay cho *Sông Hậu*).
- **Hướng thẩm mỹ:** *Chứng từ* — nền giấy sáng `#F4F4F2`, chữ than chì `#16181B`, một màu nhấn xanh ngọc lục bảo mang nghĩa *đã thu tiền*: `#0B6B4A` cho chữ nhỏ và nút, `#0F8A5F` chỉ cho chữ từ 24px trở lên và đồ họa.
- **Chữ:** Unbounded cho tiêu đề, Geist cho giao diện, Geist Mono cho mã số. Tất cả tự host, đều có bộ chữ tiếng Việt.
- **Nguồn duy nhất:** `static/css/tokens.css`.
- **Hai giọng:** landing là giọng to (tràn màn hình, chuyển động mạnh); màn hình nghiệp vụ là giọng nhỏ (tiết chế, dày dữ liệu, theo hồ sơ B).

## Các phương án đã xét

| Phương án | Vì sao không chọn |
|---|---|
| Landing và phần mềm dùng hai bộ nhận diện riêng | Đăng nhập xong như sang một ứng dụng khác |
| Giữ bộ nhận diện cũ, chỉ thêm landing | Người dùng đã muốn làm lại toàn bộ giao diện; giữ màu cũ là trói landing vào thứ sắp bị thay |
| Hướng *Tín hiệu* (nền tối, sóng) | Lý do chính là hình ảnh dòng sông gắn với tên *Sông Hậu*; đổi tên thành Hanzo thì lý do đó mất |
| Hướng *Phòng điều hành* (sáng, bảng điều khiển) | Gần kiểu trang SaaS quen thuộc nhất, phải dựa hoàn toàn vào chuyển động để khác biệt |
| Chỉ đổi tên phần mềm, giữ *Sông Hậu* làm nhà mạng | Câu chuyện "hãng phần mềm triển khai cho nhà mạng" rất thật, nhưng người dùng chọn một cái tên xuyên suốt |
| Phông thân chữ Be Vietnam Pro hoặc Archivo | Be Vietnam Pro cùng dáng hình học với Unbounded nên tiêu đề và thân chữ kém tách bạch, lại rộng hơn; Archivo ép hẹp thì dấu hơi chật |

## Hệ quả

- **Khó đảo ngược:** đổi lại màu, phông hay tên sau khi dự án con thứ hai xong là phải sửa 46 màn hình, 3 phép kiểm PDF, chụp lại toàn bộ ảnh báo cáo.
- **Có một giai đoạn chuyển tiếp:** từ khi xong dự án con thứ nhất tới khi xong dự án con thứ hai, phần mềm có hai bảng màu (`tokens.css` và `app.css` cũ). Luật *"bảng màu chỉ có một nguồn"* trong `CLAUDE.md` tạm thời bị vi phạm, có ghi chú ngay dưới luật đó.
- **Chuẩn tương phản đã đo, không ước lượng:** `#0F8A5F` chỉ đạt 3,96:1 nên bị giới hạn ở chữ lớn và đồ họa. Mọi cặp màu thêm sau này phải đo lại.
- **Luật chống mạo danh giữ nguyên tinh thần:** tên mới vẫn là doanh nghiệp hư cấu; ba phép kiểm PDF vẫn cấm `VNPT` và nay cấm thêm `SÔNG HẬU`.
