# KỊCH BẢN DEMO — 12–15 PHÚT

**Đề tài:** Xây dựng phần mềm quản lý thuê bao và tính cước điện thoại

> ⚠️ Mở đầu buổi demo, nói rõ **một câu**: *"Toàn bộ dữ liệu trong hệ thống là dữ liệu mẫu tự
> sinh phục vụ học tập, không phải dữ liệu thật của nhà mạng nào."* Câu này nên nói ngay, vì
> giao diện có tên "VNPT Billing".

---

## Chuẩn bị trước khi lên trình bày

| # | Việc | Ghi chú |
|---|---|---|
| 1 | MySQL đang chạy, `MYSQL_PASSWORD` đã đặt | |
| 2 | Chạy `mvnw spring-boot:run "-Dspring-boot.run.profiles=reset"` rồi **dừng lại** | Đưa dữ liệu về đúng bộ chuẩn |
| 3 | Chạy `mvnw spring-boot:run` bình thường | |
| 4 | Mở sẵn **3 tab**: `localhost:8080`, cửa sổ terminal, thư mục chứa ảnh chụp dự phòng | |
| 5 | Kiểm nhanh: kỳ **8/2026 phải rỗng** (0 CDR, 0 hóa đơn) | Đây là kỳ dùng để chạy trực tiếp |
| 6 | Chạy sẵn `mvnw test` một lần, **giữ nguyên console** | Phòng khi cuối giờ không kịp chạy lại |

> **Dự phòng:** nếu máy chiếu hoặc MySQL trục trặc, chuyển sang bộ ảnh chụp trong
> `docs/danh-sach-anh-chup.md`. Đừng sửa lỗi môi trường trên sân khấu.

---

## Bước 1 — Đăng nhập và dashboard *(1 phút)*

**Làm:** đăng nhập `admin` / `123456`. Dừng lại ở trang chủ.

**Nói:** hệ thống mô phỏng trọn vòng nghiệp vụ viễn thông: khách hàng → thuê bao → gói cước →
CDR → tính cước → hóa đơn → thanh toán → báo cáo. Dashboard đọc số từ **5 kỳ cước** đã chạy
thật: doanh thu 111.513.012 đ, đã thu 49.190.687 đ, còn nợ 62.322.325 đ.

Chỉ vào biểu đồ cột 5 kỳ: hai màu là **phát sinh** và **đã thu**; khoảng cách giữa hai cột
chính là công nợ.

> **Hỏi:** *Số liệu này lấy ở đâu ra?*
> **Đáp:** đọc thẳng từ cột `hoa_don.tong_thanh_toan` và `hoa_don.con_no`. Cố ý **không** cộng
> lại từ bảng thanh toán — hai cách tính song song là hai nguồn sự thật, và chúng sẽ mâu thuẫn
> đúng lúc khó truy nhất. Có test bất biến chạy trên cả 280 hóa đơn để bảo đảm hai nguồn không
> bao giờ rời nhau.

---

## Bước 2 — Gói cước và bảng giá *(1 phút)*

**Làm:** `/goi-cuoc` → `/bang-gia`.

**Nói:** 5 gói cước, mỗi gói có cước thuê bao tháng và quota ưu đãi. Bảng giá 10 dòng, tách
theo **dịch vụ × hướng × giờ cao điểm**. Chỉ vào cột `block_giay`.

> **Hỏi:** *Block là gì?*
> **Đáp:** đơn vị tính tiền nhỏ nhất. Block 6 giây nghĩa là gọi 45 giây vẫn tính tròn **8
> block**, không phải 7,5. Trong dữ liệu mẫu, **82,8%** cuộc gọi không chia hết cho block — nên
> quy tắc làm tròn này quyết định gần như toàn bộ cước thoại, không phải chi tiết nhỏ.
>
> **Hỏi:** *Vì sao giờ cao điểm chỉ áp cho thoại?*
> **Đáp:** đó là quy tắc nghiệp vụ đã chốt và được gom trong `QuyTacGioCaoDiem`, dùng chung cho
> cả bộ sinh CDR lẫn bộ nhập CSV — một quy tắc, một chỗ.

---

## Bước 3 — Tạo khách hàng doanh nghiệp và 2 thuê bao *(2 phút)*

**Làm:** `/khach-hang/them` → loại *Doanh nghiệp*, MST 10 số, điền đủ, Lưu.
Rồi từ trang chi tiết khách hàng bấm **Đăng ký thuê bao** hai lần, chọn gói **DN500**.

**Cố ý làm sai một lần:** nhập MST chỉ 5 số → bị chặn kèm thông báo tiếng Việt.

**Nói:** mã khách hàng `KH` tự sinh; validation khác nhau theo loại khách — cá nhân dùng CCCD
12 số, doanh nghiệp dùng MST 10 số.

> **Hỏi:** *Vì sao chặn ở cả form lẫn tầng dưới?*
> **Đáp:** form chỉ biết dữ liệu người dùng gõ. Những luật phụ thuộc **trạng thái** — số giấy
> tờ đã có ai dùng chưa, thuê bao còn hoạt động không — phải kiểm ở tầng nghiệp vụ, vì trạng
> thái có thể đã đổi kể từ lúc form được mở.

---

## Bước 4 — Tạm ngừng rồi khôi phục *(1 phút)*

**Làm:** mở một thuê bao đang hoạt động → **Tạm ngừng 1 chiều** (có modal xác nhận) → rồi
**Khôi phục**. Mở tab **Lịch sử biến động**.

**Nói:** hai dòng mới, mỗi dòng ghi trạng thái cũ → mới, lý do, người thực hiện, thời điểm.
Việc chuyển trạng thái đi qua một **ma trận** cố định, không phải muốn đổi sao cũng được.

**Làm thêm:** thử đổi một thuê bao *Đã thanh lý* sang *Hoạt động* → bị chặn.

> **Hỏi:** *Vì sao thanh lý là trạng thái cuối?*
> **Đáp:** số đã thu hồi và có thể cấp lại cho khách khác. Cho khôi phục thì hai khách hàng
> khác nhau cùng dùng một lịch sử số — dữ liệu cước sẽ lẫn.

---

## Bước 5 — Sinh CDR cho kỳ 8/2026 *(1,5 phút)*

**Làm:** `/ky-cuoc` chỉ ra kỳ **8/2026** đang rỗng. Sang `/cdr/sinh-du-lieu`:
khoảng **01/08 → 31/08/2026**, **2000** bản ghi, ô **Hạt giống** nhập `20260800`.

**Nói:** bộ sinh mô phỏng phân bố thật — 70% thoại, 20% SMS, 10% data; giờ tập trung 7h–23h;
không sinh trước ngày kích hoạt của thuê bao.

Chỉ vào khối kết quả: **hạt giống đã dùng = 20260800**.

> **Hỏi:** *Hạt giống để làm gì?*
> **Đáp:** để dữ liệu **tái lập được**. Tới hết Phase 5 bộ sinh dùng `new Random()` không hạt
> giống, nên mất dữ liệu là mất luôn mọi con số đã viết trong báo cáo — và hệ quả là suốt hai
> phase không ai dám chạy lệnh nạp lại dữ liệu. Nay nhập lại đúng hạt giống với cùng tham số
> thì ra **đúng** bộ dữ liệu cũ; có test tự động chứng minh điều đó, so từng bản ghi chứ không
> so số lượng.

---

## Bước 6 — Chạy tính cước kỳ 8 *(1 phút)*

**Làm:** `/tinh-cuoc` → **Tính cước** kỳ 8/2026. Chờ chỉ báo "Đang xử lý" chạy xong.

**Nói:** engine định giá **từng bản ghi**: tra bảng giá theo dịch vụ + hướng + giờ, tính số
block, nhân đơn giá, rồi ghi `cuoc_phi` và **ghi luôn `bang_gia_cuoc_id`** — ảnh chụp dòng giá
đã áp dụng.

> **Hỏi:** *Snapshot đơn giá để làm gì?*
> **Đáp:** để hóa đơn cũ vẫn truy nguyên được giá đã thu, kể cả khi bảng giá đổi về sau. Nếu
> chỉ lưu `cuoc_phi` rồi suy ngược đơn giá lúc lập hóa đơn thì mỗi lần đổi giá là mọi hóa đơn
> cũ giải thích sai.

---

## Bước 7 — ⭐ BẢNG ĐỐI SOÁT *(3 phút — phần quan trọng nhất)*

**Làm:** mở `/tinh-cuoc/doi-soat/21/1` (thuê bao `0821234521`, kỳ 6/2026).

Đi lần lượt **bốn khối**:

**Khối 1 — thuê bao và gói cước.** Cước thuê bao tháng, quota ưu đãi, và dòng ghi rõ *"dùng
trọn kỳ 30 ngày"* hay prorate theo số ngày.

**Khối 2 — sản lượng và ưu đãi.** ⭐ Dừng lại ở đây. Chỉ vào con số dạng
**`5.739,7 MB (5.877.492 KB)`** — hai đơn vị trong cùng một ô.

**Nói:** đây là chỗ dễ sai nhất của cả hệ thống. CSDL lưu dung lượng bằng **KB**, còn quota của
gói khai bằng **MB**; thời lượng lưu bằng **giây**, còn quota khai bằng **phút**. Hiển thị cả
hai con số để người đọc **tự kiểm** phép quy đổi thay vì phải tin.

**Khối 3 — từng bản ghi.** Chỉ vào dòng được tô nền: bản ghi làm **vượt quota**.

**Khối 4 — đối chiếu với hóa đơn.** Cột chênh lệch **toàn 0 đ**, khung xanh *"Khớp tuyệt đối"*.

**Làm thêm:** mở `/tinh-cuoc/doi-soat/34/1` — `99,6 phút (5.978 giây)` cạnh quota 100 phút → **0 đ**.

> **Hỏi:** *Ba chỗ quy đổi đơn vị là gì?*
> **Đáp:** giây↔phút (×60), KB↔MB (×1024), và quan trọng nhất: **quota phải quy XUỐNG đơn vị
> bản ghi**, không quy bản ghi LÊN đơn vị quota. Cả ba gom trong một lớp `DonViCuoc`.
>
> **Hỏi:** *Vì sao chiều quy đổi lại quan trọng?*
> **Đáp:** quy từng bản ghi lên phút rồi cộng sẽ **làm tròn lên nhiều lần**. Đo trên dữ liệu
> thật: sản lượng bị thổi phồng **+10,97%**, tức thu tiền oan của khách. Thuê bao ở ảnh thứ hai
> là ví dụ: 5.978 giây thật ra là 99,6 phút — còn trong quota; quy sai chiều thì thành 105 phút
> và bị tính vượt.
>
> **Hỏi:** *Vì sao không cắt đôi bản ghi khi vượt quota?*
> **Đáp:** một bản ghi CDR chỉ có **một** cột `cuoc_phi`, không có chỗ lưu "phần trong ưu đãi"
> và "phần vượt". Cắt đôi là phải thêm cột hoặc thêm bảng. Quy tắc đã chốt: gặp bản ghi vượt thì
> **dừng hẳn**. Hệ quả cố hữu là kết quả **phụ thuộc thứ tự**, nên mọi truy vấn duyệt CDR bắt
> buộc `ORDER BY thoi_gian_bat_dau, id`.
>
> **Hỏi:** *Bảng đối soát tự tính lại hay đọc lại?*
> **Đáp:** **đọc lại** số đã ghi. Tự tính lại theo cách riêng thì nó chỉ chứng minh chính nó, và
> tạo ra nguồn sự thật thứ hai.

---

## Bước 8 — Hóa đơn và xuất PDF *(1,5 phút)*

**Làm:** từ bảng đối soát bấm sang hóa đơn tương ứng. Chỉ vào các dòng khoản mục, số tiền bằng
chữ, rồi bấm **Xuất PDF**.

**Nói:** hóa đơn có ràng buộc `UNIQUE(thue_bao_id, ky_cuoc_id)` ở CSDL — một thuê bao chỉ có
đúng một hóa đơn mỗi kỳ, kể cả khi engine chạy lại do lỗi.

> **Hỏi:** *Vì sao phải nhúng font vào PDF?*
> **Đáp:** PDF chỉ hiện đúng dấu tiếng Việt khi font được nhúng kèm bảng mã Identity-H. Dùng
> font mặc định thì chữ có dấu ra ô vuông — **và lỗi đó không làm hỏng file**, PDF vẫn mở bình
> thường. Vì vậy test bắt buộc phải **đọc lại nội dung PDF** và so khớp chuỗi có dấu.
>
> **Hỏi:** *Font lấy ở đâu?*
> **Đáp:** Liberation Sans, giấy phép SIL Open Font License, cho phép phát hành lại. Cố ý
> **không** dùng Times New Roman hay Arial của Windows — hai font đó không được phép đóng gói
> vào một kho mã công khai.

---

## Bước 9 — Kế toán: ghi nhận thanh toán *(1,5 phút)*

**Làm:** đăng xuất, đăng nhập `ketoan01`. Chỉ vào sidebar — **không còn** mục Khách hàng và
Thuê bao. Gõ thẳng `/khach-hang` → trang **403**.

Mở một hóa đơn còn nợ → **Ghi nhận thanh toán**, thu **một phần** → trạng thái đổi sang *Thanh
toán một phần*. Thu nốt → *Đã thanh toán*. Bấm **In phiếu thu**.

**Cố ý làm sai:** thu nhiều hơn số còn nợ → bị chặn.

> **Hỏi:** *Vì sao tiền dùng `BigDecimal` chứ không `double`?*
> **Đáp:** `double` là số nhị phân dấu chấm động, không biểu diễn chính xác được 0,1 — cộng dồn
> hàng nghìn bản ghi thì sai số tích lại và `SUM(cuoc_phi)` sẽ không khớp cột trên hóa đơn.
> `BigDecimal` là số thập phân chính xác. Quy ước của dự án: `HALF_UP`, scale 0, và **chỉ làm
> tròn ở đúng một tầng** — tầng CDR; các mức trên chỉ cộng dồn.

---

## Bước 10 — Công nợ và bảng tuổi nợ *(1,5 phút)*

**Làm:** `/cong-no`.

**Nói:** bảng tuổi nợ chia 5 nhóm theo số ngày quá hạn, tính từ `han_thanh_toan`. Chỉ vào biểu
đồ và khối **Đề xuất tạm ngừng** ở cuối trang.

Nhấn mạnh dòng ghi chú: đây là **đề xuất**, hệ thống liệt kê ra và người dùng bấm nút — không
tự động cắt dịch vụ của khách.

> **Hỏi:** *Hóa đơn trả một phần rồi quá hạn thì trạng thái là gì?*
> **Đáp:** vẫn là *Thanh toán một phần*. Quét quá hạn cố ý **chỉ chạm hóa đơn chưa thu đồng
> nào** — nếu không, mọi hóa đơn trả dở đều bị xoá dấu vết "đã thu được một phần". Thông tin
> quá hạn không mất: nó nằm ở cột *số ngày quá hạn* và ở nhóm tuổi nợ, suy từ ngày.

---

## Bước 11 — Báo cáo và biểu đồ *(1,5 phút)*

**Làm:** `/bao-cao` → mở **Doanh thu theo kỳ** (biểu đồ cột kèm đường tỷ lệ thu) → **Sản lượng
dịch vụ** (so với kỳ trước) → bấm **Xuất Excel** và mở file ra.

**Nói:** 7 báo cáo, mọi truy vấn gom nhóm **ngay trong CSDL** chứ không load dữ liệu lên rồi
cộng trong Java. File Excel có freeze pane, dòng tổng in đậm, và chân trang ghi rõ *dữ liệu mẫu
phục vụ học tập*.

> **Hỏi:** *Làm sao biết số trên báo cáo là đúng?*
> **Đáp:** có test kiểm **chéo ba đường truy vấn khác nhau** cho cùng một con số — gom theo kỳ,
> gom theo gói cước, và bảng cơ cấu cước phải ra bằng nhau. So với một hằng số chép tay thì chỉ
> chứng minh hôm nay dữ liệu vẫn như hôm qua; so ba đường gom nhóm khác nhau mới bắt được lỗi
> sai điều kiện join.

---

## Bước 12 — Kiểm thử *(1,5 phút)*

**Làm:** chuyển sang console, chỉ vào dòng `Tests run: 269, Failures: 0`.
Rồi mở ảnh **test đỏ → xanh** đã chuẩn bị.

**Nói:** 269 test tự động và 177 phép kiểm giao diện qua 8 script. Nhưng con số không phải là
điều đáng nói nhất.

Chỉ vào ảnh test đỏ: *"Một phép kiểm chưa từng đỏ thì chưa chứng minh được gì."* Với mỗi bất
biến quan trọng, tôi cố ý làm hỏng dữ liệu một lần để xem test có bắt được không — rồi mới khôi
phục.

Nêu **một ví dụ cụ thể**: màn hình giảm trừ dùng biến lặp tên `gt`, trùng đúng toán tử *lớn hơn*
của Thymeleaf, nên trang **chưa bao giờ hiển thị được** — HTTP 500 ngay từ lần đầu. Nó sống sót
qua hai phase vì mọi phép kiểm đều gõ thẳng URL, tức đi vòng qua đúng cái đang hỏng. Chỉ khi
viết một script **đi theo menu** thì lỗi mới lộ ra.

> **Hỏi:** *Vì sao chọn Spring Boot 3.5 khi đã có bản 4.x?*
> **Đáp:** 3.5 là nhánh **LTS** hiện hành, chạy trên Java 21 LTS, và toàn bộ hệ sinh thái quanh
> nó — Spring Security, Data JPA, Thymeleaf, tài liệu, câu trả lời trên diễn đàn — đã ổn định.
> Với một đồ án cần chạy đúng và giải thích được, chọn nhánh mới nhất chỉ đổi lấy rủi ro không
> cần thiết. Dự án biên dịch ở mức `release 21` dù máy phát triển chạy JDK 25, để mã nguồn chạy
> được trên môi trường Java 21.
>
> **Hỏi:** *Idempotent nghĩa là gì trong dự án này?*
> **Đáp:** chạy nhiều lần cho cùng một kết quả như chạy một lần. Ví dụ: quét hóa đơn quá hạn chỉ
> đổi hóa đơn **thực sự** quá hạn nên gọi bao nhiêu lần cũng vô hại; chạy trừ cước lần hai bị
> chặn để không trừ chồng; và lệnh nạp lại dữ liệu mẫu dựng lại **đúng** bộ số cũ, đã kiểm bằng
> cách so ảnh chụp CSDL trước và sau — 20.101 dòng, 0 khác biệt.

---

## Câu hỏi hay gặp — trả lời nhanh

| Câu hỏi | Ý chính khi trả lời |
|---|---|
| Dữ liệu thật hay giả? | **Giả lập hoàn toàn**, tự sinh, phục vụ học tập |
| Hệ thống có tính cước thời gian thực không? | Không. Định giá trước, trừ theo **lô cuối kỳ**. Real-time nằm ở "Hướng phát triển" |
| Thuê bao trả trước có hóa đơn không? | Không. Trừ thẳng vào số dư và ghi **sổ cái** `bien_dong_so_du` |
| Số dư có thể âm không? | Không. Gặp bản ghi không đủ quỹ thì dừng hẳn; phần không thu được ghi lại như **hiện vật của mô hình**, không phải khoản nợ |
| Điểm yếu lớn nhất? | Chưa có real-time; bảng tuổi nợ đủ 5 nhóm phụ thuộc mốc thời gian; chưa có phân trang cho báo cáo lớn |
| Nếu làm lại sẽ làm khác gì? | Đặt hạt giống cho bộ sinh dữ liệu **ngay từ đầu**, và viết phép kiểm đi theo liên kết **từ Phase 2** thay vì Phase 7 |

---

## Phân bổ thời gian

| Bước | Phút | Cộng dồn |
|---|---|---|
| 1 Dashboard | 1,0 | 1,0 |
| 2 Gói cước, bảng giá | 1,0 | 2,0 |
| 3 Khách hàng + thuê bao | 2,0 | 4,0 |
| 4 Tạm ngừng / khôi phục | 1,0 | 5,0 |
| 5 Sinh CDR | 1,5 | 6,5 |
| 6 Tính cước | 1,0 | 7,5 |
| **7 Bảng đối soát** ⭐ | **3,0** | **10,5** |
| 8 Hóa đơn + PDF | 1,5 | 12,0 |
| 9 Thanh toán | 1,5 | 13,5 |
| 10 Công nợ | 1,5 | 15,0 |
| 11 Báo cáo | 1,5 | 16,5 |
| 12 Kiểm thử | 1,5 | 18,0 |

> Tổng đầy đủ **18 phút**. Muốn gọn về **12–15 phút** thì bỏ bước 2 và bước 4, rút gọn bước 3
> còn một thuê bao. **Không rút ngắn bước 7** — đó là phần có nội dung kỹ thuật cao nhất và là
> chỗ trả lời được nhiều câu hỏi nhất.
