# G7 — MỞ PHẦN MỀM BẰNG MỘT CÚ NHÁY ĐÚP

> Người dùng đích là nhân viên giao dịch quen Excel và Zalo. Cách chạy cũ — mở PowerShell, `cd`
> vào thư mục, gõ `.\mvnw spring-boot:run`, đợi, rồi tự gõ `localhost:8080` — có bốn chỗ họ
> phải biết trước. Đợt này rút xuống còn **một cú nháy đúp**.

Ngày làm: 28/08/2026 · Bốn file mới trong `khoi-dong/`, không sửa dòng mã ứng dụng nào.

---

## 1. Chọn cách chạy: bản đóng gói `.jar`, không qua Maven

Hai cách, đo trên máy thật:

| | `mvnw spring-boot:run` | `java -jar` |
|---|---|---|
| Thời gian tới lúc `/dang-nhap` trả lời | ~8–10 giây | **~7,8–9,7 giây** (gồm cả kiểm môi trường) |
| Số tiến trình phải quản lý | **ba tầng**: `cmd` → `java` (Maven) → `java` (ứng dụng) | **một**: `java` |
| Cần gì | Maven tải xong phụ thuộc | chỉ cần `.jar` |
| Luôn chạy mã mới nhất | có | chỉ khi đóng gói lại |

**Chọn `.jar`.** Nhưng lý do quyết định **không phải tốc độ** — mà là **số tầng tiến trình**.

`spring-boot:run` tách ra một JVM con: ứng dụng thật là **cháu** của cửa sổ, không phải con.
Muốn "đóng cửa sổ là tắt phần mềm" thì phải với qua hai tầng. Với `java -jar`, ứng dụng là
**con trực tiếp** — đo được: cha của `java.exe` đúng bằng số hiệu của cửa sổ PowerShell. Một
tầng thì ràng buộc vòng đời được; ba tầng thì không.

**Điểm cố ý làm khác `scripts/chay.ps1`.** Script đó, khi bản đóng gói cũ hơn `src/`, chỉ **báo**
chứ không tự đóng gói lại — lập luận của nó là 8 giây đóng gói + 6 giây chạy = 14 giây, chậm hơn
chế độ phát triển, nên để lập trình viên tự chọn. Ở đây thì **tự đóng gói lại**. Người dùng cuối
không có khái niệm "bản đóng gói cũ hơn mã nguồn" để mà chọn, và demo nhầm bản cũ trước hội đồng
là hỏng buổi bảo vệ. Đo lại thì đóng gói chỉ mất **4 giây** chứ không phải 8 — nên cái giá của
việc luôn đúng còn rẻ hơn con số script cũ dựa vào.

`pom.xml` cũng tính vào phép so ngày: đổi thư viện làm bản đóng gói cũ đi mà **không file nào
trong `src/` thay đổi**.

---

## 2. Vì sao không gọi sang `scripts/chay.ps1` có sẵn

Script đó đã làm gần đúng việc này. Nhưng nó viết cho **người khác**:

| | `scripts/chay.ps1` | `khoi-dong/Chay-PhanMem.ps1` |
|---|---|---|
| Người dùng | lập trình viên | nhân viên giao dịch |
| Cổng 8080 bận | **hỏi** có giết tiến trình cũ không | không giết, mở trình duyệt vào bản đang chạy |
| Kiểm MySQL | không | có, và tự bật |
| Kiểm `MYSQL_PASSWORD` | không | có |
| Chờ sẵn sàng rồi mở trình duyệt | không | có |
| Cách tắt | `Ctrl+C` | đóng cửa sổ |
| **Chế độ `reset`** | **có** | **không có đường nào tới** |

Dòng cuối là lý do quyết định. `chay.ps1` nhận tham số `reset` xoá sạch CSDL. Gọi sang nó là mở
một đường mà một tham số lạc tay có thể đi tới chỗ mất dữ liệu — trong khi kỳ 5 và kỳ 6 **không
dựng lại được bằng hạt giống**. Viết độc lập thì đường đó không tồn tại.

Xác minh: `grep -riE "reset|flyway|drop|truncate" khoi-dong/*.ps1 *.cmd` chỉ ra **hai** dòng, cả
hai đều nằm trong đúng đoạn chú thích giải thích vì sao không có đường tới `reset`.

> **Một phép kiểm của tôi đỗ vì lý do sai.** Lượt đầu tôi chạy `git grep -i reset khoi-dong/`,
> nó trả về rỗng và tôi suýt ghi "đạt". Nhưng `git grep` **chỉ tìm trong file đã được git theo
> dõi**, mà `khoi-dong/` lúc đó chưa `git add` — nó trả về rỗng vì không thấy file nào, không
> phải vì không có chữ nào. `grep` thường ngay sau đó tìm ra 2 lần "reset". Tiêu chí này chỉ
> có nghĩa **sau khi commit**, và đã kiểm lại đúng cách ở mục 6 bên dưới.

---

## 3. Bốn lỗi đo được trong lúc dựng

### 3.1. Desktop không phải `%USERPROFILE%\Desktop` — thất bại **im lặng**

```
GetFolderPath('Desktop')  = C:\Users\<tên-tài-khoản>\OneDrive\Desktop
$env:USERPROFILE\Desktop  = C:\Users\<tên-tài-khoản>\Desktop   <- CÓ TỒN TẠI, nhưng rỗng
```

Máy này đồng bộ OneDrive nên Desktop thật đã bị chuyển hướng. Đường dẫn cũ **vẫn còn** — nên
ghi `.lnk` vào đó **không báo lỗi gì cả**, chỉ là biểu tượng không bao giờ hiện ra. Đây là loại
lỗi tệ nhất: thành công giả.

Dùng `[Environment]::GetFolderPath('Desktop')` — nó hỏi chính Windows đang cấu hình thế nào.

### 3.2. Tên dịch vụ MySQL không được gõ cứng

Đề bài ban đầu bảo hướng dẫn người dùng "mở Services, tìm **MySQL80**". Trên máy này dịch vụ tên
**`MySQL84`**; `Get-Service -Name 'MySQL80'` trả về rỗng. Gõ cứng một tên là chỉ người dùng đi
tìm một dòng không tồn tại trên máy họ — và họ không có cách nào biết là chỉ dẫn sai chứ không
phải họ tìm kém.

Script dò `Get-Service | Where-Object { $_.Name -like 'mysql*' }` rồi **in đúng tên tìm được**
vào câu hướng dẫn.

### 3.3. Chữ Việt có dấu không qua được COM `WScript.Shell`

```
Unable to save shortcut "C:\...\Qu?n l? thuê bao & tính cư?c.lnk"
```

`WScript.Shell.CreateShortcut` đẩy đường dẫn qua bảng mã ANSI. `ê í ư` sống sót, `ả ý ớ` thành
`?`, rồi `Save()` ném `FileNotFoundException`.

Cách vòng: ghi ra tên **thuần ASCII** (`loi-tat-tam.lnk`) rồi đổi tên bằng
`[System.IO.File]::Move` — hàm .NET dùng Unicode thật. Đổi tên không làm hỏng gì: file `.lnk`
không lưu tên của chính nó bên trong.

### 3.4. Ứng dụng sống sót sau khi cửa sổ chết — **lỗi thật, đã sửa**

Phép thử: mở app, rồi giết **riêng** tiến trình PowerShell giữ nó (không `/T`).

| | java còn lại | cổng 8080 |
|---|--:|---|
| Trước khi sửa | **1** | **vẫn bị giữ** |
| Sau khi sửa | **0** | **đã trả** |

Ứng dụng mồ côi giữ cổng 8080 nghĩa là không cửa sổ nào tắt được nó nữa — phải vào Task Manager,
đúng thứ người dùng này không làm được.

Bản sửa: buộc `java` vào một **Job Object** có cờ `JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE`. Hệ điều
hành tự giết `java` khi tiến trình giữ sổ biến mất — **bất kể biến mất kiểu gì**.

> **Vì sao không chỉ dựa vào `CTRL_CLOSE_EVENT`.** Bấm nút X thì Windows có gửi sự kiện đó cho
> mọi tiến trình dùng chung console, và `java` thường chết theo. Nhưng "thường" không đủ: cửa sổ
> bị treo rồi *End task*, hay PowerShell chết vì lỗi, đều **không** gửi sự kiện đó. Job Object
> bịt cả ba đường. Giá: **0,15 giây** biên dịch `Add-Type` — đo được, không phải ước.
>
> Phép thử ở trên (`Stop-Process -Force`) **khắc nghiệt hơn** bấm nút X, vì nó cố tình không gửi
> `CTRL_CLOSE_EVENT`. Qua được phép thử khắc nghiệt thì qua được cả trường hợp nhẹ hơn.

### 3.5. Biểu tượng: kiểm cấu trúc chứ không chỉ kiểm "mở được"

`.NET` không có hàm ghi `.ico` nhiều cỡ, nên header phải tự dựng. Đọc lại từng byte để chắc:

| | |
|---|---|
| Loại | `1` (icon) — đúng |
| Số khung | **7** (16 · 24 · 32 · 48 · 64 · 128 · 256) |
| Mọi khung | nén PNG, chữ ký `89 50` đúng, offset khớp |
| Windows đọc 16 / 32 / 48 px | trả về **đúng** 16 / 32 / 48 |

Một chỗ **không** hoàn hảo, ghi ra chứ không giấu: hỏi 256px thì `System.Drawing.Icon` trả về
khung **128**. Đó là hạn chế đã biết của bộ đọc GDI+ với khung PNG — Explorer dùng bộ đọc khác
và đọc được khung 256. Hệ quả xấu nhất nếu Explorer cũng không đọc được: biểu tượng ở cỡ lớn
nhất lấy khung 128px, vẫn nét trên màn hình thường.

---

## 4. Hai chỗ tự bắt lỗi của chính mình

**Phép kiểm lối tắt báo động giả.** Sau khi tạo `.lnk`, script đọc lại file để xác nhận. Lượt
đầu nó báo *"đọc lại không khớp"* trong khi lối tắt hoàn toàn đúng. Nguyên nhân: chuỗi UTF-16
trong `.lnk` nằm ở **byte lẻ**, giải mã từ offset 0 thì ghép sai cặp byte.

| Cách đọc | Tìm thấy `Chay-PhanMem.ps1`? |
|---|---|
| UTF-16 từ byte chẵn | không |
| **UTF-16 từ byte lẻ** | **có** |
| Một byte (ASCII) | không |

Sửa thành thử cả ba thể. Rồi **đối chứng âm** để chắc nó vẫn biết báo đỏ:

| | Kết quả | Mong đợi |
|---|---|---|
| Lối tắt thật | ✅ có | có |
| Một lối tắt khác (`Cisco Packet Tracer.lnk`) | ❌ không | không |
| Chính file đó nhưng **đảo byte** | ❌ không | không |
| Tìm một chuỗi bịa | ❌ không | không |

**Câu "bấm phím bất kỳ để đóng cửa sổ" nói sai.** Lối tắt chạy với `-NoExit`, nên cửa sổ **không**
đóng sau khi script kết thúc. Đã đổi thành "Đọc xong thì đóng cửa sổ này lại", và bỏ hẳn lời gọi
`ReadKey` — với `-NoExit` thì chữ tự ở lại, không cần giữ.

**Trình duyệt mặc định chưa đặt thì script chết ngay sau khi app chạy xong.** `Start-Process
$diaChi` nằm ngoài `try`, mà `$ErrorActionPreference = 'Stop'`. Người dùng sẽ thấy màn hình đỏ
lòm trong khi thực ra phần mềm **đang chạy tốt**. Đã bắt lỗi và đổi thành câu: *"Phần mềm VẪN
ĐANG CHẠY, hãy mở trình duyệt rồi gõ địa chỉ này"*.

---

## 5. Chữ tiếng Việt: có dấu, và đo trước khi tin

Chú thích trong mã giữ **không dấu** theo lệ `scripts/chay.ps1`. Chữ **hiện ra cho người dùng**
thì có dấu — nhân viên giao dịch đọc "Kiểm tra kho dữ liệu" dễ hơn hẳn "Kiem tra kho du lieu".

Hai đường phải đúng cùng lúc, và chúng là hai thứ khác nhau:

* **BOM UTF-8** sửa đường **đọc** file (bẫy đã ghi trong `CLAUDE.md`).
* **`[Console]::OutputEncoding = UTF8`** sửa đường **ghi** ra màn hình. Thiếu nó thì PowerShell
  5.1 ghi bằng bảng mã console (thường 437) và chữ vẫn vỡ **dù đã có BOM**.

Không tin suông: cho một cửa sổ console thật **tự đọc lại bộ đệm màn hình của chính nó**
(`$Host.UI.RawUI.GetBufferContents`) rồi so từng ký tự.

```
Kiểm tra kho dữ liệu MySQL — thuê bao, hóa đơn, công nợ     khớp: True
Muốn tắt phần mềm: đóng cửa sổ màu đen này                  khớp: True
```

Khung viền cũng **để máy căn lề** (`PadRight`) thay vì đếm tay khoảng trắng — sửa một chữ mà
phải đếm lại cả dòng là cách chắc chắn để lệch.

---

## 6. Nghiệm thu

| Tiêu chí | Cách đo | Kết quả |
|---|---|---|
| Nháy đúp → trình duyệt mở trang đăng nhập, không gõ gì | mở qua chính `.lnk` trên Desktop | ✅ sẵn sàng sau **9,5 s**; `GET /` → **302** → `/dang-nhap` → **200**, có ô mật khẩu |
| Trình duyệt *thật sự* tải trang | kết nối TCP tới cổng 8080 thuộc tiến trình nào | ✅ `browser.exe` = **Cốc Cốc**, khớp trình duyệt mặc định trong registry (`CocCocHTML`) |
| Chạy lần hai lúc đang chạy → không mở tiến trình thứ hai | đếm `java.exe` trước/sau | ✅ vẫn **1**, **cùng số hiệu** |
| Tắt MySQL → hướng dẫn rõ, không stack trace | mô phỏng (xem ghi chú dưới) | ✅ không stack trace; câu hướng dẫn **gọi đúng tên dịch vụ dò được** |
| Xoá `MYSQL_PASSWORD` → hướng dẫn, không stack trace | chạy trong phiên đã xoá biến | ✅ không stack trace, java **không** khởi động |
| Đóng cửa sổ → app tắt, cổng 8080 trả | giết riêng cửa sổ, không `/T` | ✅ java **0**, cổng **trả** (trước khi sửa: 1 / vẫn giữ) |
| `git grep -i reset khoi-dong/` | sau khi `git add` | ✅ chỉ 2 dòng **chú thích** giải thích vì sao không có |
| Dữ liệu không suy suyển | đếm bảng trước/sau toàn bộ buổi thử | ✅ CDR **18.723** · hóa đơn **280** · thanh toán **161** · biến động **35** · 6 kỳ; **0** hóa đơn lệch bất biến; kỳ 8 vẫn rỗng; kỳ 6+7 vẫn **0** thanh toán |
| `mvnw test` | đếm `<testcase>` trong XML | ✅ **342 / 342** |
| 8 script giao diện | đếm dấu `[DAT ]` / `[SAI ]` trong output thật của cả 8 | ✅ **215 đạt / 0 sai** |
| 3 phép kiểm Python | chạy cả ba | ✅ đạt — 46 file chữ · 38 màn hình · 180 nút/liên kết |
| Bản dự phòng `.cmd` | chạy nó ở nhánh báo lỗi, `stdin` từ `NUL` | ✅ gọi được `.ps1` · tiếng Việt hiện đúng qua `cmd` · `pause` có nổ (⇒ `if errorlevel 1` bắt đúng mã thoát) |

Chi tiết 8 script: `test-auth` 42 · `test-bao-cao` 39 · `test-bien` 42 · `test-dieu-huong` 15 ·
`test-kh` 14 · `test-ky-rong` 28 · `test-muc-F` 17 · `test-tb` 18.

Bản `.cmd` được thử ở **nhánh báo lỗi** chứ không phải đường thuận lợi. Cố ý: phần riêng của
`.cmd` chỉ gồm `chcp 65001`, lời gọi `.ps1`, và `if errorlevel 1 pause` — nhánh lỗi kiểm được
cả ba, còn đường thuận lợi thì chỉ kiểm được hai. Bản thân `.ps1` đã thử trọn vòng đời ở trên.

> **Bộ đo của tôi hỏng trước, không phải script.** Lượt đầu tôi trích "dòng kết quả cuối" bằng
> một biểu thức đoán mò; hai script không khớp mẫu nên trả `null` và cả lượt chạy đổ lỗi. Nhìn
> vào `_chung.ps1` mới thấy script thật đánh dấu bằng `[DAT ]` / `[SAI ]` — đếm đúng thứ đó thì
> ra 215/0. Bài học cũ: đọc xem thứ mình đo **thật sự in ra cái gì**, đừng đoán định dạng.

### Ghi chú về phép thử MySQL — chỗ **không** đo được thật

Tắt dịch vụ MySQL cần quyền quản trị, mà ràng buộc của đợt này là **không tự nâng quyền**.
`Stop-Service MySQL84` trả về *"Cannot open MySQL84 service"*. Nên hai nhánh MySQL được thử bằng
cách chạy một **bản sao của chính script**, chỉ thay **đúng hai dòng** (đã in `diff` để đối
chiếu):

* dòng điều kiện `if (-not (Test-CongMo 3306))` → `if ($true)`;
* bộ lọc dịch vụ → tên một dịch vụ **có thật đang tắt mà cũng không có quyền bật**
  (`AarSvc_1b09648c9`), và một tên không tồn tại.

Toàn bộ phần sinh thông báo lỗi giữ nguyên. Nhánh thứ nhất in đúng tên `AarSvc_1b09648c9` vào
câu hướng dẫn — đó là bằng chứng cho tính chất "không gõ cứng tên dịch vụ", chứ không phải suy
đoán. Nhưng cần nói thẳng: **đây là mô phỏng điều kiện, không phải tắt MySQL thật.**

---

## 7. Cố ý không làm

| | Vì sao |
|---|---|
| **Không tự nâng quyền quản trị** | Một chương trình đòi quyền quản trị lúc khởi động là thói quen xấu, và người dùng không có cách nào biết nó dùng quyền đó làm gì. Thiếu quyền thì **hướng dẫn người dùng tự bật**, gọi đúng tên dịch vụ trên máy họ |
| **Không cài gì, không đụng registry, không tạo dịch vụ nền** | Gỡ đi = xoá một biểu tượng. Đồ án phải sạch dấu vết trên máy hội đồng |
| **Không tải biểu tượng từ mạng** | <img src="../khoi-dong/bieu-tuong-xem-thu.png" width="64" alt="biểu tượng cột phát sóng"><br>Biểu tượng vẽ bằng GDI+ ngay lúc chạy — hình cột phát sóng, đúng hướng thị giác *"trạm viễn thông"* chốt ở G1, dùng đúng màu mực `#12283a` và nhấn cyan của `app.css`. Vẽ bằng hình khối chứ **không** dùng glyph của font: font thiếu trên máy khác thì glyph thành ô vuông rỗng |
| **Không ghi mật khẩu vào bất kỳ file nào** | Đọc từ `MYSQL_PASSWORD` y như ứng dụng. Script chỉ kiểm biến đó **có hay không**, không bao giờ in ra |
| **Không sửa `scripts/chay.ps1`** | Ngoài phạm vi, và nó vẫn đúng cho người dùng của nó |
| **Không xử lý mật khẩu MySQL rỗng hợp lệ** | Có cấu hình MySQL để `root` không mật khẩu; script coi biến rỗng là chưa khai và dừng. Đổi lại là mất luôn khả năng bắt lỗi hay gặp nhất. Hạn chế đã biết, không phải sót |
| **Không kiểm được nút X thật** | Môi trường công cụ không tạo được cửa sổ có `MainWindowHandle`, nên không gửi được `WM_CLOSE`. Đã bù bằng cách làm tính chất đó **đúng theo cấu tạo** (Job Object) rồi thử bằng cách khắc nghiệt hơn |

---

## 8. File tạo ra

| File | Việc |
|---|---|
| `khoi-dong/Chay-PhanMem.ps1` | Script chính, 5 bước, BOM UTF-8 |
| `khoi-dong/Tao-LoiTat.ps1` | Chạy một lần: vẽ biểu tượng + tạo `.lnk` trên Desktop |
| `khoi-dong/Chay-PhanMem.cmd` | Bản dự phòng — nháy đúp `.ps1` thì Windows mở Notepad, `.cmd` thì chạy |
| `khoi-dong/README.md` | Dùng để làm gì · tạo lối tắt · gỡ · ba lỗi hay gặp |
| `khoi-dong/.gitignore` | Bỏ qua hai file nhật ký sinh lúc chạy |

Sửa thêm: `README.md` (mục **2bis** *Cách mở phần mềm*, đặt trước phần dòng lệnh cho lập trình
viên) và `docs/huong-dan-su-dung.md` (mục *Cách mở phần mềm* ở đầu file).

Cả hai mục đều chèn **không đánh số lại** các mục cũ: `CLAUDE.md`, chính `README.md` và nhiều
tài liệu khác đang tham chiếu chéo "xem mục 5", "xem mục 8" — đánh số lại là làm hỏng hết.
Dùng hậu tố `bis`, đúng lệ đã có ở `KE-HOACH-HOAN-THIEN.md` mục 7bis.
