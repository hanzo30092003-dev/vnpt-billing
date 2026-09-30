# RÀ SOÁT TRƯỚC KHI GHI ĐĨA NỘP

> ℹ️ Từ 01/10/2026 nhà mạng hư cấu đổi tên thành **Viễn thông Hanzo** (ADR 0001). Tài liệu này giữ tên cũ *Sông Hậu* đúng như tại thời điểm viết.

> **Rà soát chỉ đọc.** Không file nào bị xoá, sửa hay di chuyển; không `git commit`. Tài liệu
> này là thứ duy nhất được tạo ra.
>
> Đĩa rời khỏi tay sinh viên là **vĩnh viễn, không thu hồi được** — khác repo GitHub còn sửa
> được sau. Nên mọi khẳng định dưới đây đều kèm `file:dòng`, dung lượng đo được, hoặc kết quả
> lệnh. Không có câu "có thể có".

Ngày rà: 28/08/2026 · Trên bản `d44001f` · Kho `D:\KGU\TTNN\APP\vnpt-billing`

---

## Bước 0 — Kiểm kê skill

Rà thư mục skill thật trên đĩa (`~/.claude/skills/`, `~/.claude/plugins/`). Hai skill có liên
quan **tồn tại thật**, đọc được frontmatter:

| Skill | Đường dẫn | Có dùng được không |
|---|---|---|
| `security-review` | `~/.claude/skills/security-review/SKILL.md` | **Không.** Frontmatter: *"Use this skill when adding authentication, handling user input, working with secrets, creating API endpoints"* — đây là checklist để **viết** mã an toàn, không phải để rà một kho đã có. Sai việc |
| `claude-security` | `~/.claude/plugins/marketplaces/claude-plugins-official/plugins/claude-security/skills/claude-security/SKILL.md` | **Không.** Frontmatter khai `disable-model-invocation: true` — chỉ người dùng gọi được. Ngoài ra nó nhắm **lỗ hổng khai thác được**, còn việc này là **rủi ro lộ thông tin khi phát tán** — hai câu hỏi khác nhau. Nó cũng sinh Workflow + nhiều Agent mà đề bài không yêu cầu |

**Kết luận: không skill nào áp dụng được. Rà bằng `grep`/`git`** — đây là việc rà chuỗi thuần.

---

## Ghi chú sửa một tiền đề của đề bài

Đề bài nêu *"và một thư mục `scratchpad/` chứa file tạm"*. **Kho không có thư mục nào tên
`scratchpad`**:

```
find . -type d -iname "*scratch*" -not -path './.git/*'   →   0 kết quả
```

`scratchpad/` là thư mục tạm của phiên làm việc, nằm ở `%TEMP%\claude\...`, **ngoài dự án** —
nó không bao giờ đi lên đĩa. Bước 3 nhóm 3 bên dưới rà thứ thật sự có.

---

## Cách các phép quét được kiểm chứng

Đề bài bắt buộc đối chứng âm. Cách làm: một **file mồi** đặt ngoài kho, chứa sẵn mỗi loại chuỗi
cần tìm. Mỗi mẫu chạy hai lần — trên mồi (phải kêu) rồi trên kho. **Số 0 chỉ được ghi nhận khi
mồi đã kêu.**

Cách này bắt được **bốn lỗi trong chính bộ đo của tôi**, trước khi chúng thành kết luận sai:

| # | Lỗi | Hậu quả nếu không bắt |
|---|---|---|
| 1 | Đếm mồi bằng `grep -c ... \| wc -l` — `-c` luôn in một dòng, kể cả khi khớp 0 | Cột "đối chứng" **luôn báo ĐẠT**, kể cả khi mồi câm. Đối chứng thành vô nghĩa |
| 2 | Mồi thiếu 5 từ khoá (`token=`, VinaPhone/MobiFone/Viettel, tên sinh viên, GVHD, "Viễn thông + địa danh") | 5 phép quét trả 0 mà **không ai chứng minh được chúng biết kêu** |
| 3 | Bản `grep` này xử lý `\\` trong ERE không như mong đợi: `C:\\Users` → **0** khớp, `C:[\]Users` → **1**. Mọi mẫu dùng dấu chéo ngược đều câm | Toàn bộ nhóm "đường dẫn máy cá nhân" và UNC báo 0 sai. Thực tế có **14 dòng** |
| 4 | Mẫu UNC vẫn câm cả sau khi sửa | Đã đổi sang tìm chuỗi cố định (`grep -F`) mới kêu được |

Sau khi sửa, một mẫu bịa (`zzqqxx-khong-he-co`) cho **0** trên chính file mồi — chứng tỏ bộ đo
phân biệt được, chứ không phải kêu bừa.

> Đây là lần thứ **11** trong dự án một phép kiểm tự nó sai trước khi đối tượng bị kiểm sai.

---

## Bước 1 — Kết quả quét bí mật

Quét **toàn bộ cây thư mục**, kể cả file không được git theo dõi (441 file bị `.gitignore` bỏ
qua: 424 trong `target/`, 11 trong `logs/`, 6 file log lẻ). `git grep` chỉ dùng để **đối chiếu
thêm** — nó bỏ sót file chưa `git add`, đúng cái bẫy đợt G7 đã dính.

### 🔴 PHẢI XOÁ trước khi ghi đĩa — 3 mục

| # | Chỗ | Bằng chứng | Vì sao đỏ |
|---|---|---|---|
| **R1** | `logs/` — 11 file, **4,1 MB** | `HANZO` **3 121 lần** · `D:\KGU\...` **457 lần** · `generated security password` **10 lần**<br>VD `logs/vnpt-billing-2026-08-10.0.log`: *"…(D:\KGU\TTNN\APP\vnpt-billing\target\classes **started by HANZO** in D:…"* | In dấu vân tay máy cá nhân — **tên tài khoản Windows** và đường dẫn đầy đủ — hàng nghìn lần lên một cái đĩa không thu hồi được. Với người chấm thì 4 MB này vô nghĩa |
| **R2** | `run.log` (20 KB) · `reset.log` (12 KB) | Cùng loại: `VNPT` 72 và 46 lần, kèm đường dẫn máy | Như trên |
| **R3** | `khoi-dong/nhat-ky-khoi-dong.log` (16 KB) | Nhật ký lần chạy gần nhất của lối tắt G7 | Như trên. `.gitignore` đã bỏ qua nó, nhưng **chép thư mục thì vẫn lên đĩa** — đó chính là lý do phải rà cả file ngoài git |

> Cả ba đều **không nằm trong git**. Chỉ dùng `git grep` là không thấy chúng.

### 🟡 Cần người quyết — 5 mục

| # | Chỗ | Trích (đã che) | Nhận định |
|---|---|---|---|
| **V1** | `.git/` — **9,8 MB** | Email cá nhân `hanz***@gmail.com` xuất hiện **218 lần** | Đúng bằng **109 commit × 2** dòng `author`+`committer`. Đã xác minh: **0 lần** trong nội dung file — chỉ nằm ở siêu dữ liệu commit. Email này vốn đã công khai trên repo GitHub public của chính sinh viên, nên rủi ro thấp; nhưng `.git/` không giúp gì cho người chấm. **Đề xuất: loại** |
| **V2** | `docs/G7-REPORT.md:75` và `:76` | `C:\Users\<tên-tài-khoản>\OneDrive\Desktop`<br>`C:\Users\<tên-tài-khoản>\Desktop`<br>*(đã che ở đợt G8 — bản gốc ghi tên tài khoản thật)* | Lộ **tên tài khoản Windows** trong một tài liệu sẽ được đọc. Ở đây nó có lý do chính đáng — đoạn đó đang chứng minh Desktop bị OneDrive chuyển hướng. Thay `HANZO` bằng `<tên-tài-khoản>` là xong, không mất ý nghĩa |
| **V3** | `docs/PHASE-0-REPORT.md` 9 dòng (`:216 :279 :318 :319 :322 :348 :354 :355 :358`) · `README.md:361` `:369` · `khoi-dong/Tao-LoiTat.ps1:15` | `D:\KGU\TTNN\APP\vnpt-billing`, `D:\HỌC\TTNN\APP` | Đường dẫn máy cá nhân. Không nhạy cảm, nhưng ở PHASE-0 nó là **nội dung có thật của báo cáo** (kể lại sự cố đường dẫn tiếng Việt có dấu) — xoá đi là làm hỏng câu chuyện. Đề xuất **giữ**, người quyết là sinh viên |
| **V4** | `docs/PHASE-0-REPORT.md:202` | `Using generated security password: 1e47***` | Mật khẩu Spring Boot **tự sinh ngẫu nhiên lúc khởi động** khi chưa có `UserDetailsService`. Dự án nay đã có bảng `nguoi_dung` nên chuỗi này **đã chết từ Phase 2**. Vô hại về kỹ thuật — nhưng một người chấm nhìn thấy chữ *"security password"* trong bài nộp sẽ phải dừng lại kiểm tra. Đề xuất che thành `***` |
| **V5** | `README.md:3` · `docs/PHASE-0-REPORT.md:413` | `github.com/hanzo30092003-dev/vnpt-billing` | Định danh GitHub của chính sinh viên, repo đã để public có chủ đích. Giữ hay bỏ là lựa chọn cá nhân, không phải rủi ro |

### 🟢 Hợp lệ — 10 nhóm, kèm lý do

| # | Chỗ | Vì sao **không** phải bí mật |
|---|---|---|
| **X1** | `src/main/resources/application.yml:44` — `password: "${MYSQL_PASSWORD:}"` | Đây là **tên biến môi trường**, không phải mật khẩu. Đã kiểm **mọi phiên bản file này từng tồn tại** (3 phiên bản trong lịch sử): tất cả đều dùng dạng `${…}`, **chưa bao giờ** có giá trị thật |
| **X2** | `README.md:121` — `$env:MYSQL_PASSWORD = "matkhau_root_cua_ban"` | Chữ giữ chỗ, nghĩa đen là *"mật khẩu root của bạn"* |
| **X3** | 8 script trong `scripts/` — `$env:MYSQL_PWD = $env:MYSQL_PASSWORD` | Truyền biến môi trường sang biến môi trường. Không có giá trị nào trong mã |
| **X4** | `.github/workflows/test.yml:36` — `MYSQL_ALLOW_EMPTY_PASSWORD: "yes"` | CI dùng MySQL container không mật khẩu. File tự khai ở dòng 11–12: *"Khong co bi mat nao trong file nay"*. Hai IP duy nhất trong kho là `127.0.0.1` ở dòng `:41` và `:71` — loopback, đề bài đã loại trừ |
| **X5** | `src/main/resources/db/data-mau.sql` — 1 hash `$2a$10$pPFiHT***` dùng chung cho 3 tài khoản | Hash BCrypt, không phải mật khẩu thường. Bản rõ là `123456` và được **công bố có chủ đích** ở README mục 5 — đó là tài khoản demo cho người chấm đăng nhập |
| **X6** | `scripts/test-auth.ps1:151` `$MK_KT = "matkhau***"` · `:252 $MK_MOI = "matkhaumoi***"` | Mật khẩu **đồ gá kiểm thử** cho tài khoản `kiemthu01` do chính script tạo ra. Không mở được gì ngoài CSDL mẫu cục bộ |
| **X7** | `com.hanzo.billing` — **1 048 lần** | Tên gói Java do sinh viên tự đặt. Trùng tên tài khoản Windows nhưng là định danh kỹ thuật, không phải đường dẫn máy. Đã tách bạch: `C:\Users\<tên>` chỉ **2 lần** (mục V2) |
| **X8** | Mọi lần `VNPT` ngoài `logs/` — 62 dòng | Kiểm từng dòng: đều là tên kỹ thuật (`vnpt-billing`, `vnpt_billing`, `com.hanzo.billing`), hoặc **tài liệu giải thích rằng VNPT không được xuất hiện trên chứng từ**. Bên phát hành hoá đơn là công ty **hư cấu** *"Công ty Cổ phần Viễn thông Sông Hậu"*, MST `1800000000`, tổng đài `1800 6060` — đều là số bịa. Có **3 phép kiểm** khẳng định chiều phủ định: `HoaDonPdfServiceTest:145`, `PhieuThuPdfServiceTest:74`, `PhieuThuPdfTaiLieuThatTest:76` đều `assertThat(vanBan).doesNotContain("VNPT")` |
| **X9** | 53 email trong `data-mau.sql` | **100% thuộc miền dành riêng**: 35 × `@example.local`, 3 × `@vnptbilling.local`, 15 × `@<tên>.example.local`. `.local` là miền mDNS (RFC 6762) và `example.*` là miền tài liệu (RFC 2606) — **không định tuyến được**, không thể là email thật của ai |
| **X10** | `src/main/resources/static/css/app.css:205` — `…không đặt --bs-success ở :root: mixin…` | Dương tính giả của mẫu `root:` — đây là chú thích CSS về bộ chọn `:root` |

### Các phép quét trả **0** — và cách đối chứng

| Nhóm | Mẫu | Kho | Mồi kêu |
|---|---|--:|--:|
| Khoá riêng | `BEGIN … PRIVATE KEY` | 0 | 1 |
| Token | `Bearer <token>` · `token=<giá trị>` | 0 · 0 | 1 · 1 |
| API key / secret | `api_key=` · `secret=` | 0 · 0 | 2 · 1 |
| Base64 dài ≥ 60 ký tự | `[A-Za-z0-9+/]{60,}` | 0 | 1 |
| Chuỗi kết nối lộ mật khẩu | `://user:pass@host` | 0 | 3 |
| Đường dẫn UNC | `\\máy\chia-sẻ` (dùng `grep -F`) | 0 | 3 |
| IP thật (trừ loopback) | `([0-9]{1,3}\.){3}[0-9]{1,3}` | 0 | 5 |
| Đơn vị thực tập | `VNPT + tỉnh` · `Viễn thông + địa danh` · `Trung tâm Kinh doanh` | 0 | 1 · 2 · 1 |
| Nhà mạng khác | `VinaPhone\|MobiFone\|Viettel` | 0 | 1 |
| Hệ thống nội bộ | `CCBS\|BCCS` | 0 | 1 |
| Mã nhân viên | `mã nhân viên: <số>` | 0 | 1 |
| Tên người | `Võ Đoàn Bảo Long` · `Bảo Long` | 0 · 0 | 1 · 2 |
| Giảng viên hướng dẫn | `GVHD\|giảng viên hướng dẫn` | 0 | 2 |
| Đơn vị tiếp nhận | `đơn vị thực tập\|cơ quan tiếp nhận` | 0 | 4 |
| Email cá nhân trong file | `@gmail.com` | 0 | — |

Mọi số 0 ở trên đều có mồi kêu ở cột phải. Riêng `@gmail.com` được đối chứng theo cách khác:
nó **có** 218 lần trong lịch sử (mục V1), nên phép quét chắc chắn biết kêu — và **0** là kết
quả riêng của phần nội dung file.

### Quét lịch sử Git — bí mật trong commit cũ

Lịch sử giữ lại cả file đã xoá, nên file hiện tại sạch **không** chứng minh lịch sử sạch.

**Đối chứng trước:** chuỗi *"Dữ liệu mẫu phục vụ học tập"* đã bị gỡ khỏi mọi file ở đợt G1c.

```
trong file hiện tại : 0
trong lịch sử git   : 2 commit  (960eeab, e0ba98d)
```

Phép quét **với tới được nội dung đã xoá**. Chỉ sau đó mới tin các số 0 dưới đây.

Cách quét: đổ **toàn bộ 2 307 đối tượng** (22 MB) và riêng **922 blob** (nội dung file) thành
luồng rồi grep.

| Tìm gì | Toàn bộ lịch sử | Chỉ nội dung file |
|---|--:|--:|
| `secret=` · `api_key=` · `Bearer` · `PRIVATE KEY` | 0 | 0 |
| `://user:pass@host` | 0 | 0 |
| IP thật (không phải loopback) | — | **0** |
| VNPT + tỉnh · Viễn thông Kiên Giang | 0 | 0 |
| VinaPhone / MobiFone / Viettel · CCBS / BCCS | 0 | 0 |
| Tên sinh viên · GVHD | 0 | 0 |
| `@gmail.com` | **218** | **0** |
| `C:\Users\<tên>` | — | 2 |
| `password` kèm giá trị | — | 45 |

**45 dòng `password` trong lịch sử — phân loại, giá trị đã che:**

| Số lần | Dạng | Là gì |
|--:|---|---|
| 29 | `MYSQL_PASSWORD = "matkhau_root_cua_ban"` | Chữ giữ chỗ trong README |
| 12 | `MYSQL_PWD = $env***` | Truyền biến môi trường |
| 4 | `password: 1e47***` | UUID Spring tự sinh (mục V4) |

**Kết luận lịch sử: chưa bao giờ có mật khẩu thật nào được commit.** Cũng chỉ có **4 file cấu
hình** từng tồn tại trong toàn bộ lịch sử (`application.yml`, `application-reset.yml`,
`test.yml`, `maven-wrapper.properties`) — **không có `.env`** nào.

> Nghĩa là: **không cần viết lại lịch sử Git.** Không cần `filter-branch`, không cần
> `git-filter-repo`. Chỉ cần không chép `.git/` lên đĩa là đủ.

---

## Bước 2 — Kiểm chứng "dữ liệu mẫu tự sinh"

Báo cáo sẽ cam kết không dùng dữ liệu thật. Dưới đây là bằng chứng **kiểm lại được**, không
phải lời nói.

### Số CCCD — khuôn mẫu số học lộ rõ

35 số, **35 số duy nhất**, không trùng nhau. Sáu số đầu tiên:

```
092301 004517          6 chữ số cuối, tách đôi:  00 45 17
089201 118426                                    11 84 26
091301 227315                                    22 73 15
087202 336204                                    33 62 04
086301 445193                                    44 51 93
084202 554082                                    55 40 82
```

Cặp đầu của nhóm 6 chữ số cuối chạy **00 → 11 → 22 → 33 → 44 → 55**, cấp số cộng công sai 11.
CCCD thật do Bộ Công an cấp **không bao giờ** có quy luật số học như vậy giữa các công dân
khác nhau. Ba chữ số đầu là mã tỉnh hợp lệ (092, 089, 091, 087, 086, 084) — cố ý cho số trông
đúng dạng, không phải cho số trở thành thật.

### Số điện thoại — còn lộ liễu hơn

```
0901234501   0912345602   0933456703   0944567804   0965678905   0976789006
```

Phần thân là dãy chữ số **tăng dần liên tiếp** (`0123450`, `1234560`, `3345670`…), phần đuôi là
**số đếm** `01, 02, 03, 04, 05, 06`. 115 lần xuất hiện, 80 số duy nhất — khớp đúng 80 thuê bao
mà `CLAUDE.md` ghi.

### Tên khách hàng — xếp theo bảng chữ cái

```
KH000001  Nguyễn Văn An      (A)
KH000002  Trần Thị Bình      (B)
KH000003  Lê Hoàng Cường     (C)
KH000004  Phạm Thị Dung      (D)
KH000005  Huỳnh Văn Đức      (Đ)
KH000006  Võ Thị Em          (E)
```

Tên riêng chạy đúng thứ tự bảng chữ cái tiếng Việt. Đây là dấu hiệu kinh điển của bộ sinh dữ
liệu, không phải danh sách khách hàng.

### Mã số thuế doanh nghiệp — cùng một bộ sinh

```
0810123420   0812345632   0814567844   0816789056   0819123480   0821234521
```

Cũng là dãy chữ số tăng dần. 95 mã duy nhất. Tên doanh nghiệp là tên Mekong hư cấu — *"Công ty
TNHH Thương mại Cửu Long"*, *"Công ty CP Thủy sản Hậu Giang"*, *"Công ty TNHH Xây dựng Tây
Đô"* — và khớp đúng slug email `@cuulongtm.example.local`, `@haugiangts.example.local`,
`@taydoxd.example.local`. Một bộ sinh duy nhất tạo ra cả tên lẫn email.

### Bảng giá cước — tự đặt, không phải biểu giá công bố

`data-mau.sql:53-63`, đơn giá đồng:

| Dịch vụ | Hướng | Block | Giá thường | Giờ cao điểm |
|---|---|--:|--:|--:|
| THOẠI | Nội mạng | 6 s | 15 | **18** |
| THOẠI | Ngoại mạng | 6 s | 25 | **30** |
| THOẠI | Quốc tế | 60 s | 3 600 | **4 320** |
| SMS | Nội / Ngoại / Quốc tế | 1 tin | 99 / 250 / 2 500 | — |
| DATA | Nội mạng | 1 MB | 25 | — |

Giá cao điểm là **đúng +20%** ở cả ba dòng (15×1,2=18 · 25×1,2=30 · 3600×1,2=4320), và chính
file ghi ở dòng 60: *"Gio cao diem: +20% so voi gia thuong"*. Một biểu giá **công bố thật**
không bao giờ là một phép nhân 1,2 sạch trên toàn bộ dịch vụ — nó là kết quả thương lượng và
làm tròn theo từng dịch vụ. Đây là giá tự đặt cho bài toán.

### `data-van-hanh.sql` — bản dump máy sinh

File tự khai ở đầu (`:1-21`):

> *"FILE SINH TU DONG - KHONG SUA TAY. Moi dong INSERT duoi day la ban dump CAC DONG THUC TE
> trong CSDL sau khi da chay tron ven cac buoc nghiep vu QUA DUNG DUONG CODE."*

Kèm nguyên văn lệnh `mysqldump` để tái sinh. CDR sinh từ `CdrGeneratorService` với hạt giống
ghi rõ trong `CLAUDE.md` (kỳ 3 `20260300` · kỳ 4 `20260400` · kỳ 7 `20260700`); hóa đơn và
thanh toán sinh từ thao tác qua giao diện. Nội dung file là **hệ quả** của việc chạy mã trên dữ
liệu mẫu — không có đường nào để dữ liệu ngoài lọt vào.

### Kết luận Bước 2

> **Lời tuyên bố "dữ liệu mẫu tự sinh" đứng vững, và kiểm lại được bằng lệnh** — CCCD, số điện
> thoại, mã số thuế và tên khách hàng đều lộ khuôn mẫu số học hoặc thứ tự bảng chữ cái; 100%
> email thuộc miền dành riêng RFC; bảng giá là phép nhân 1,2 tự đặt; và không có một chuỗi nào
> trong toàn kho lẫn toàn bộ lịch sử Git chỉ tới một đơn vị viễn thông có thật.

---

## Bước 3 — Thứ không nên đưa lên đĩa

### Nhóm 1 — Thư mục nặng, vô nghĩa với người chấm

| Đường dẫn | Dung lượng | Ghi chú |
|---|--:|---|
| `target/` | **89,3 MB** | Gồm `billing-0.0.1-SNAPSHOT.jar` 81,7 MB · `classes/` 4,5 MB · `surefire-reports/` 1,3 MB · `*.jar.original` 1,1 MB. Dựng lại được bằng `mvnw package` |
| `node_modules/` | — | **Không tồn tại** (dự án không dùng npm) |
| `.mvn/wrapper/*.jar` | — | **Không có jar**, chỉ có `maven-wrapper.properties` 1 KB. Phải **giữ**, thiếu nó `mvnw` không chạy |

> Đã kiểm bên trong `billing-0.0.1-SNAPSHOT.jar`: `BOOT-INF/classes/application.yml` dùng
> `username: "${MYSQL_USER:root}"` và `password: "${MYSQL_PASSWORD:}"` — **jar không nhúng bí
> mật nào**. (Đối chứng: tìm một chuỗi bịa trong file đó cho 0, tìm `password` cho 1 — chứng tỏ
> có đọc được thật vào trong jar.) Xem Bước 4 về việc **có nên** đưa riêng jar lên đĩa.

### Nhóm 2 — `.git/` và rủi ro lịch sử

| | |
|---|--:|
| Dung lượng | **9,8 MB** |
| Số commit | 109 |
| Số đối tượng | 2 307 (922 blob) |

⚠️ **Rủi ro cố hữu của `.git/`:** lịch sử giữ lại **cả file đã xoá**. Một mật khẩu từng nằm
trong một commit cũ vẫn đọc được nguyên vẹn dù file hiện tại đã sạch — `git show <sha>` là đủ,
không cần công cụ gì đặc biệt.

**Đã kiểm thật, không suy đoán** (chi tiết ở Bước 1): quét toàn bộ 2 307 đối tượng.

* Bí mật trong nội dung file: **0** — và phép quét đã được chứng minh với tới nội dung đã xoá.
* `@gmail.com`: **218 lần**, đúng bằng 109 × 2 dòng `author`/`committer`. **0 lần** trong nội
  dung file.

→ **Không cần viết lại lịch sử.** Chỉ cần không chép `.git/` lên đĩa.

### Nhóm 3 — File tạm và sinh ra lúc chạy

| Đường dẫn | Dung lượng | Phân loại |
|---|--:|---|
| `logs/` (11 file) | **4,1 MB** | 🔴 R1 |
| `run.log` | 20 KB | 🔴 R2 |
| `reset.log` | 12 KB | 🔴 R2 |
| `khoi-dong/nhat-ky-khoi-dong.log` | 16 KB | 🔴 R3 |
| `run.err` · `reset.err` · `khoi-dong/nhat-ky-khoi-dong-loi.log` | **0 byte** cả ba | File rỗng — loại cho gọn |
| `scratchpad/` | — | **Không tồn tại trong kho** (xem ghi chú tiền đề ở trên) |
| `msg-*.txt` · `commit-*.txt` · `*.tmp` · `*.bak` · `*.orig` | — | **Không có file nào.** `find` trả 0 kết quả |
| Script `.ps1`/`.py` dựng một lần rồi bỏ | — | **Không có.** Cả 8 script trong `scripts/` đều đang được dùng (215 phép kiểm), 3 script `.py` đều đang chạy, 2 script `khoi-dong/` là sản phẩm đợt G7 |

### Nhóm 4 — Tài liệu trùng lặp hoặc lỗi thời

Rà cả 24 file trong `docs/` (764 KB): **không có bản trùng lặp nào**. Các file `PHASE-*-REPORT`
là báo cáo của từng giai đoạn khác nhau, `G4`–`G7` là bốn đợt hoàn thiện khác nhau, hai file
`RA-SOAT-*` rà hai mặt khác nhau (giao diện / tính năng). `mau-cdr.csv` (4 KB) là file mẫu để
thử chức năng nhập CSV — **phải giữ**, nó là dữ liệu đầu vào của một chức năng có thật.

Không có bản sao lưu CSDL nào nằm rải rác — hai file SQL trong `src/main/resources/db/`
(2,4 MB) là **nguồn dữ liệu chính thức**, phải giữ.

---

## Bước 4 — Cấu trúc đĩa và cách tạo

### 4.1. Cây thư mục đề xuất

```
DIA-NOP/                                            ước tính
├── DOC-TRUOC-KHI-CHAY.txt                            2 KB
│
├── 1-BAO-CAO/                                      (bản Word/PDF của báo cáo)
│   └── Bao-cao-TTNN-Vo-Doan-Bao-Long.pdf
│
├── 2-MA-NGUON/                                     5,8 MB
│   └── vnpt-billing/
│       ├── src/                                    4,7 MB
│       │   ├── main/java/           1,0 MB
│       │   ├── main/resources/      3,3 MB   (db 2,4 MB · templates 469 KB
│       │   │                                  · fonts 408 KB · static 60 KB)
│       │   └── test/                448 KB
│       ├── docs/                                   764 KB   (24 tài liệu)
│       ├── scripts/                                144 KB   (8 script kiểm thử + 3 python)
│       ├── khoi-dong/                              101 KB   (mở bằng một cú nháy đúp)
│       ├── .github/workflows/                        8 KB
│       ├── .mvn/wrapper/                             1 KB   (BẮT BUỘC giữ)
│       ├── pom.xml · mvnw · mvnw.cmd · chay.cmd     33 KB
│       ├── README.md · CLAUDE.md                    44 KB
│       └── .gitignore                                4 KB
│
├── 3-CHAY-NGAY/                                    81,7 MB   (tuỳ chọn — xem 4.2)
│   └── billing.jar
│
├── 4-TAI-LIEU-PDF/                                 ~6 MB     (xem 4.4)
│
└── 5-ANH-CHUP/                                     ~20–40 MB (70 ảnh, chưa chụp — việc N2)
```

| | Dung lượng |
|---|--:|
| Kho hiện tại trên đĩa cứng | **109,0 MB** |
| Loại `target/` + `.git/` + `logs/` + 6 file log lẻ | **−103,4 MB** |
| **Mã nguồn sạch** | **5,8 MB** |
| + `billing.jar` (tuỳ chọn) | 87,5 MB |
| + PDF tài liệu (ước) | ~93 MB |
| + 70 ảnh chụp (ước) | **~110–135 MB** |

Vừa thoải mái một đĩa **CD 700 MB**; DVD thì thừa sức.

### 4.2. Có nên đưa `billing.jar` lên đĩa không — nên, và đây là lý do

Bỏ hết `target/` thì người chấm **không chạy được** nếu máy họ không có Maven hoặc không có
mạng: `khoi-dong/Chay-PhanMem.ps1` sẽ tự chạy `mvnw package`, mà lần đầu trên máy mới cần tải
thư viện về.

Đưa **riêng file `billing.jar`** (81,7 MB) vào `3-CHAY-NGAY/` thì chạy được ngay, không cần
Maven, không cần mạng. Jar đã được kiểm là **không nhúng bí mật**. Đây là đánh đổi 81,7 MB lấy
việc người chấm bấm là chạy — trên một cái đĩa 700 MB thì rất đáng.

Vẫn **không** đưa cả thư mục `target/`: `classes/`, `test-classes/`, `surefire-reports/` và
`*.jar.original` là 7,6 MB rác không ai đọc.

### 4.3. Script dàn dựng — *chỉ là nội dung, CHƯA tạo file*

> Đây là **văn bản trong tài liệu**, không phải file `.ps1` trên đĩa. Chưa chạy lần nào. Muốn
> dùng thì tự chép ra file và chạy, sau khi đã đọc kỹ.

```powershell
<#
    Tao-Dia-Nop.ps1 — chep kho sang thu muc dan dung, loai thu khong duoc len dia.
    Chi DOC tu kho goc va GHI sang thu muc moi. Khong sua gi trong kho goc.
#>
param(
    [string] $Goc  = "D:\KGU\TTNN\APP\vnpt-billing",
    [string] $Dich = "D:\DIA-NOP",
    [switch] $KemBanChay          # them -KemBanChay de chep ca billing.jar
)

$ErrorActionPreference = 'Stop'
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

if (-not (Test-Path $Goc))  { throw "Khong thay kho goc: $Goc" }
if (Test-Path $Dich) { throw "Thu muc dich da ton tai. Xoa hoac doi ten truoc: $Dich" }

$maNguon = Join-Path $Dich "2-MA-NGUON\vnpt-billing"
New-Item -ItemType Directory -Force -Path $maNguon | Out-Null

# robocopy: /E chep ca thu muc rong, /XD loai thu muc, /XF loai file.
#   target  — 89 MB build, dung lai duoc bang mvnw package
#   .git    — 9,8 MB lich su, chua email ca nhan trong 218 dong sieu du lieu
#   logs    — 4,1 MB nhat ky, chua ten tai khoan Windows 3121 lan
robocopy $Goc $maNguon /E /NFL /NDL /NJH /NJS /NP `
    /XD target .git logs .idea .vscode node_modules `
    /XF *.log *.err *.tmp *.bak *.orig | Out-Null

# robocopy tra ve 0-7 la thanh cong, >=8 moi la loi.
if ($LASTEXITCODE -ge 8) { throw "robocopy loi, ma $LASTEXITCODE" }

if ($KemBanChay) {
    $jar = Get-ChildItem (Join-Path $Goc 'target') -Filter 'billing-*.jar' |
           Where-Object { $_.Name -notlike '*.original' } |
           Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($jar) {
        New-Item -ItemType Directory -Force -Path (Join-Path $Dich '3-CHAY-NGAY') | Out-Null
        Copy-Item $jar.FullName (Join-Path $Dich '3-CHAY-NGAY\billing.jar')
    } else {
        Write-Host "  Chua co ban dong goi. Chay truoc:  .\mvnw.cmd package -DskipTests" -ForegroundColor Yellow
    }
}

# -------------------------------------------------------------------
# NGHIEM THU — dung tin la da sach, phai do lai tren thu muc dan dung
# -------------------------------------------------------------------
Write-Host ""
Write-Host "  KIEM TRA THU MUC DAN DUNG" -ForegroundColor Cyan

$loi = 0
foreach ($x in 'target', '.git', 'logs') {
    if (Test-Path (Join-Path $maNguon $x)) { Write-Host "  [SAI] con $x" -ForegroundColor Red; $loi++ }
    else                                   { Write-Host "  [OK ] khong con $x" -ForegroundColor Green }
}

$rac = @(Get-ChildItem $Dich -Recurse -File -Include *.log,*.err,*.tmp,*.bak,*.orig -EA SilentlyContinue)
if ($rac.Count -gt 0) { Write-Host "  [SAI] con $($rac.Count) file rac" -ForegroundColor Red; $loi++ }
else                  { Write-Host "  [OK ] khong con file log/tmp" -ForegroundColor Green }

# Quet lai bi mat NGAY TREN thu muc sap ghi dia.
# Doi chung: mau bia phai cho 0, mau chac chan co phai cho >0 - neu ca hai
# deu 0 thi phep quet hong chu khong phai thu muc sach.
$bia = @(Get-ChildItem $Dich -Recurse -File -EA SilentlyContinue |
         Select-String -Pattern 'zzqqxx-khong-he-co-chuoi-nay' -EA SilentlyContinue)
$chac = @(Get-ChildItem $Dich -Recurse -File -Filter '*.yml' -EA SilentlyContinue |
          Select-String -Pattern 'MYSQL_PASSWORD' -EA SilentlyContinue)
if ($bia.Count -eq 0 -and $chac.Count -gt 0) {
    Write-Host "  [OK ] phep quet hoat dong (bia=0, chac=$($chac.Count))" -ForegroundColor Green
    $nghi = @(Get-ChildItem $Dich -Recurse -File -EA SilentlyContinue |
              Select-String -Pattern 'BEGIN [A-Z ]*PRIVATE KEY','Bearer [A-Za-z0-9._-]{10,}' -EA SilentlyContinue)
    if ($nghi.Count -gt 0) { Write-Host "  [SAI] con $($nghi.Count) chuoi nghi la khoa" -ForegroundColor Red; $loi++ }
    else                   { Write-Host "  [OK ] khong con khoa/token" -ForegroundColor Green }
} else {
    Write-Host "  [SAI] phep quet KHONG dang tin (bia=$($bia.Count), chac=$($chac.Count))" -ForegroundColor Red; $loi++
}

$mb = [math]::Round((Get-ChildItem $Dich -Recurse -File | Measure-Object Length -Sum).Sum / 1MB, 1)
Write-Host ""
Write-Host "  Tong dung luong: $mb MB" -ForegroundColor Cyan
if ($loi -eq 0) { Write-Host "  SAN SANG GHI DIA." -ForegroundColor Green }
else            { Write-Host "  CON $loi VAN DE - dung ghi dia." -ForegroundColor Red }
```

> Ba mục 🔴 (R1–R3) được loại bởi `/XD logs` và `/XF *.log`. Mục 🟡 V2 và V4 **không** tự sửa
> được bằng script — chúng nằm giữa nội dung tài liệu, phải sửa tay nếu quyết định sửa.

### 4.4. Nội dung `DOC-TRUOC-KHI-CHAY.txt`

> Lưu **UTF-8 có BOM** để Notepad cũ không đọc vỡ tiếng Việt.

```text
========================================================================
  PHAN MEM QUAN LY THUE BAO VA TINH CUOC DIEN THOAI
  Do an mon Thuc tap nghe nghiep — Truong Dai hoc Kien Giang
========================================================================

DIA NAY CO GI
  1-BAO-CAO       Ban bao cao (PDF)
  2-MA-NGUON      Toan bo ma nguon
  3-CHAY-NGAY     Ban dong goi chay duoc ngay (billing.jar)
  4-TAI-LIEU-PDF  Tai lieu ky thuat ban PDF
  5-ANH-CHUP      Anh chup man hinh

------------------------------------------------------------------------
CAN CAI GI TRUOC
------------------------------------------------------------------------
  1. Java 21 tro len       kiem bang:  java -version
  2. MySQL 8 dang chay o localhost:3306
  3. Khai mat khau MySQL mot lan. Mo PowerShell va go:

         setx MYSQL_PASSWORD "mat_khau_MySQL_cua_ban"

     Sau do PHAI dong cua so vua go lai — lenh nay chi co hieu luc voi
     cua so mo MOI sau do.

     (Mat khau CO Y khong nam trong bat ky file nao tren dia nay.)

------------------------------------------------------------------------
CACH CHAY — CHON MOT
------------------------------------------------------------------------
  CACH A — nhanh nhat, khong can Maven va khong can mang
     Chep 3-CHAY-NGAY\billing.jar ra o cung, roi go:
         java -jar billing.jar
     Doi khoang 10 giay, mo trinh duyet vao:  http://localhost:8080

  CACH B — chay tu ma nguon, co bieu tuong tren man hinh nen
     1. Chep ca thu muc 2-MA-NGUON\vnpt-billing ra o cung
        (duong dan KHONG duoc co dau tieng Viet — vi du D:\HOC\vnpt-billing)
     2. Mo thu muc khoi-dong, bam chuot phai vao Tao-LoiTat.ps1
        > chon Run with PowerShell
     3. Tu do chi can nhay dup bieu tuong tren man hinh nen.
        Trinh duyet tu mo vao trang dang nhap.
        Muon tat: dong cua so mau den.

  Lan chay dau tien can nap co so du lieu mau — xem muc 4 cua
  2-MA-NGUON\vnpt-billing\README.md

------------------------------------------------------------------------
BA TAI KHOAN DEMO — mat khau deu la:  123456
------------------------------------------------------------------------
  admin        Quan tri     Toan bo he thong
  nhanvien01   Nhan vien    Khach hang, thue bao, bao cao khong co so tien
  ketoan01     Ke toan      Hoa don, thanh toan, cong no, giam tru, bao cao

------------------------------------------------------------------------
GHI CHU VE DU LIEU
------------------------------------------------------------------------
  Toan bo du lieu trong he thong la DU LIEU MAU TU SINH phuc vu hoc tap.
  He thong KHONG dung du lieu that cua bat ky nha mang nao. Don vi phat
  hanh hoa don la "Cong ty Co phan Vien thong Song Hau" — mot doanh nghiep
  HU CAU dung rieng cho do an; ma so thue va so tong dai tren chung tu
  cung la so bia. Ten khach hang, so CCCD, so dien thoai va email deu do
  bo sinh du lieu tao ra.
========================================================================
```

### 4.5. Tài liệu nên xuất PDF

Người chấm mở đĩa trên máy không có trình soạn thảo Markdown sẽ thấy `.md` là chữ thô đầy dấu
`#` và `|`. Bảng biểu — thứ chiếm phần lớn các tài liệu này — sẽ vỡ hoàn toàn.

**Ưu tiên 1 — người chấm gần như chắc chắn mở** (7 file):

| File | Vì sao |
|---|---|
| `README.md` | Cửa vào: cài đặt, chạy, tài khoản demo |
| `docs/huong-dan-su-dung.md` | Hướng dẫn cho người dùng cuối |
| `docs/mo-ta-csdl.md` | 15 bảng + 2 view — phần được hỏi nhiều nhất khi bảo vệ |
| `docs/kich-ban-demo.md` | Kịch bản demo trước hội đồng |
| `docs/kich-ban-kiem-thu.md` | Kịch bản kiểm thử thủ công |
| `docs/DANH-GIA-HE-THONG.md` | Tự đánh giá — điểm mạnh và hạn chế |
| `khoi-dong/README.md` | Cách mở phần mềm bằng một cú nháy đúp |

**Ưu tiên 2 — bằng chứng quá trình làm** (9 file): `PHASE-0` → `PHASE-8` báo cáo. Đây là phần
dày nhất (416 KB) và là bằng chứng cho quá trình, nên xuất cả.

**Ưu tiên 3 — giữ nguyên `.md`** (8 file): `CLAUDE.md`, `KE-HOACH-HOAN-THIEN.md`, `G4`–`G7`,
`RA-SOAT-*`, `danh-sach-anh-chup.md`, `toi-uu-hieu-nang.md`, và **chính tài liệu này**. Chúng
là ghi chép nội bộ cho người bảo trì; ai cần thì đọc bản `.md` trong `2-MA-NGUON`.

Cách xuất hàng loạt mà không cần cài gì thêm: mở `.md` trong VS Code → *Markdown: Open
Preview* → **Print** → *Save as PDF*. Hoặc dán vào Word rồi *Save As → PDF* nếu muốn đánh số
trang khớp báo cáo.

---

## Bước 5 — Kết luận

### Đếm phát hiện

| | Số mục | Là gì |
|---|--:|---|
| 🔴 **Phải xoá** | **3** | R1 `logs/` 4,1 MB · R2 `run.log`+`reset.log` · R3 `khoi-dong/nhat-ky-khoi-dong.log` — **tất cả đều là file nhật ký in dấu vân tay máy cá nhân** |
| 🟡 **Cần người quyết** | **5** | V1 `.git/` (218 lần email cá nhân ở siêu dữ liệu) · V2 `C:\Users\<tên>` 2 dòng · V3 `D:\KGU` 12 dòng · V4 mật khẩu Spring đã chết · V5 định danh GitHub |
| 🟢 **Hợp lệ** | **10 nhóm** | Biến môi trường, chữ giữ chỗ, hash BCrypt, tài khoản demo công bố có chủ đích, tên gói Java, miền `.local`/`example.local`, dương tính giả CSS… |

**Không có mục nào là bí mật thật.** Không mật khẩu, không khoá riêng, không token, không chuỗi
kết nối lộ thông tin đăng nhập, không IP máy chủ thật, không một dấu vết nào của đơn vị thực
tập — cả trong file hiện tại lẫn trong toàn bộ 109 commit lịch sử.

### Dung lượng

| | |
|---|--:|
| Kho hiện tại | **109,0 MB** |
| Sau khi loại `target/`, `.git/`, `logs/`, 6 file log lẻ | **5,8 MB** |
| Đĩa hoàn chỉnh (mã nguồn + jar + PDF + ảnh) | **~110–135 MB** |

### Trả lời thẳng

> **Kho mã nguồn hiện tại CHƯA an toàn để ghi thẳng lên đĩa — phải xử lý 3 mục 🔴 trước.**
>
> Nhưng cả ba đều là **file nhật ký chạy**, và cả ba đều bị loại **tự động** bởi một dòng
> `robocopy /XD logs /XF *.log *.err` trong script ở mục 4.3. Không phải sửa một dòng mã nào,
> không phải viết lại lịch sử Git, không phải xoá gì trong kho gốc.
>
> **Bản thân mã nguồn thì sạch.** Sau khi dàn dựng qua script và chạy phần nghiệm thu trong đó,
> đĩa an toàn để nộp.

### Việc nên làm, theo thứ tự

1. Chạy script mục 4.3 (đọc kỹ trước) → loại xong cả 3 mục 🔴, và tự nghiệm thu lại.
2. Quyết 5 mục 🟡. Đề xuất: **loại `.git/`** (V1 — script đã làm sẵn); **che `HANZO`** thành
   `<tên-tài-khoản>` ở `docs/G7-REPORT.md:75-76` (V2); **che UUID** ở
   `docs/PHASE-0-REPORT.md:202` (V4); **giữ nguyên** V3 và V5.
3. Đóng gói `billing.jar` mới nhất: `.\mvnw.cmd package -DskipTests` rồi chạy script với
   `-KemBanChay`.
4. Xuất PDF theo mục 4.5, đặt vào `4-TAI-LIEU-PDF/`.
5. Chụp 70 ảnh (việc **N2** — chưa làm, xem `docs/danh-sach-anh-chup.md`), đặt vào `5-ANH-CHUP/`.
6. Tạo `DOC-TRUOC-KHI-CHAY.txt` theo mục 4.4, lưu **UTF-8 có BOM**.
7. **Thử đĩa trên một máy khác** trước khi nộp — đó là phép kiểm cuối cùng, và là phép kiểm duy
   nhất chứng minh được người chấm sẽ chạy được.
