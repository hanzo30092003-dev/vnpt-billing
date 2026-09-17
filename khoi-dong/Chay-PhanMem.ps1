<#
    Chay-PhanMem.ps1 — mo phan mem bang mot cu nhay dup.

    Ai dung: nhan vien giao dich, khong ranh may tinh. Ho khong mo PowerShell,
    khong go lenh, khong go dia chi. Ho bam mot bieu tuong tren Desktop.

    VI SAO KHONG GOI SANG scripts\chay.ps1 CO SAN:
    Script do viet cho lap trinh vien - no HOI dap tuong tac, no doi Ctrl+C de
    tat, no khong mo trinh duyet, va no CO che do 'reset' xoa sach CSDL. Mot
    tham so lac tay la mat du lieu. File nay dung rieng, khong goi sang do,
    nen KHONG CO DUONG NAO di toi reset - du go nham the nao cung khong toi.

    Luong: cong 8080 -> MySQL -> mat khau -> ban dong goi -> chay -> cho san
    sang -> mo trinh duyet -> in huong dan -> giu cua so.

    Chu thich trong file: tieng Viet KHONG dau, theo le cua scripts\chay.ps1.
    Chu HIEN RA cho nguoi dung: tieng Viet CO dau - do duoc la console ve dung
    (xem docs/G7-REPORT.md), va nhan vien giao dich doc de hon han.

    Luu kem BOM UTF-8 (bay da ghi trong CLAUDE.md).
#>

# BOM sua viec DOC file. Dong duoi sua viec GHI ra man hinh - thieu no thi
# tieng Viet van vo du file co BOM, vi PowerShell 5.1 ghi ra console bang
# bang ma cua console (thuong la 437) chu khong phai UTF-8.
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }

$ErrorActionPreference = 'Stop'

$goc     = Split-Path -Parent $PSScriptRoot
$diaChi  = 'http://localhost:8080'
$trangDN = "$diaChi/dang-nhap"
$nhatKy  = Join-Path $PSScriptRoot 'nhat-ky-khoi-dong.log'
$nhatLoi = Join-Path $PSScriptRoot 'nhat-ky-khoi-dong-loi.log'
$tienTrinh = $null
$RONG = 62

# ---------------------------------------------------------------------
# Tien ich in an
# ---------------------------------------------------------------------
function Ghi  ($chu, $mau = 'Gray') { Write-Host $chu -ForegroundColor $mau }
function Tieu ($chu) { Ghi ''; Ghi "  $chu" 'Cyan' }
function Dat  ($chu) { Ghi "  [v] $chu" 'Green' }

# Ve khung tu dong can le. Dem tay so khoang trang trong tung dong la cach
# chac chan sai: chu tieng Viet co dau van la MOT ky tu, nhung sua mot chu la
# phai dem lai ca dong. De may dem.
function VienTren ($mau = 'DarkCyan') { Ghi ('  ┌' + ('─' * $RONG) + '┐') $mau }
function VienGiua ($mau = 'DarkCyan') { Ghi ('  ├' + ('─' * $RONG) + '┤') $mau }
function VienDuoi ($mau = 'DarkCyan') { Ghi ('  └' + ('─' * $RONG) + '┘') $mau }
function DongKhung ($chu, $mau = 'Gray') {
    $c = " $chu"
    if ($c.Length -gt $RONG) { $c = $c.Substring(0, $RONG) }
    Ghi ('  │' + $c.PadRight($RONG) + '│') $mau
}

# Moi thong bao loi phai noi CHUYEN GI XAY RA + PHAI LAM GI (chuan Phase 8).
# Ham nay ep dung dinh dang do: goi thieu ve thu hai la loi cu phap.
function Loi ($chuyenGi, [string[]] $phaiLam) {
    Ghi ''
    VienTren 'Red'
    DongKhung ' KHÔNG MỞ ĐƯỢC PHẦN MỀM' 'Red'
    VienDuoi 'Red'
    Ghi ''
    Ghi '  Chuyện gì xảy ra:' 'Yellow'
    Ghi "     $chuyenGi"
    Ghi ''
    Ghi '  Bạn cần làm:' 'Yellow'
    foreach ($b in $phaiLam) { Ghi "     $b" }
    Ghi ''
    # KHONG dung ReadKey o day. Loi tat chay voi -NoExit nen cua so tu o lai,
    # chu bao "bam phim de dong" se la noi sai. Ban .cmd thi tu pause lay.
    Ghi '  Đọc xong thì đóng cửa sổ này lại.' 'DarkGray'
    exit 1
}

# Tra ve $true neu co thu gi do TRA LOI HTTP o cong 8080.
# Bat ky ma HTTP nao cung tinh la san sang - ke ca 401/403/500 - vi dieu can
# biet chi la "may chu da dung day va tra loi", khong phai "trang dung".
function Test-TraLoiHttp {
    try {
        $null = Invoke-WebRequest -Uri $trangDN -UseBasicParsing -TimeoutSec 4
        return $true
    } catch {
        if ($_.Exception.Response) { return $true }   # co tra loi, chi la ma loi
        return $false                                 # chua ket noi duoc
    }
}

function Test-CongMo ([int] $cong) {
    $c = Get-NetTCPConnection -LocalPort $cong -State Listen -ErrorAction SilentlyContinue
    return [bool] $c
}

# Tim mysql.exe de THU KET NOI THAT o buoc 3. Uu tien client di kem chinh
# dich vu MySQL dang chay (cung phien ban, chac chan tuong thich), roi moi
# den PATH va cac thu muc cai dat thong thuong.
function Tim-MysqlExe {
    # 1. Canh mysqld.exe cua dich vu MySQL (doc duong dan tu WMI, cat tham so).
    $dv = @(Get-CimInstance Win32_Service -Filter "Name LIKE 'mysql%'" -ErrorAction SilentlyContinue |
            Sort-Object { $_.State -ne 'Running' })   # dich vu dang chay len truoc
    foreach ($d in $dv) {
        if ($d.PathName -match '"?([A-Za-z]:\\[^"]*?mysqld\.exe)') {
            $mysql = Join-Path (Split-Path $Matches[1]) 'mysql.exe'
            if (Test-Path $mysql) { return $mysql }
        }
    }
    # 2. Tren PATH.
    $g = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($g) { return $g.Source }
    # 3. Cac thu muc cai dat quen thuoc.
    foreach ($mau in @(
        "$env:ProgramFiles\MySQL\MySQL Server *\bin\mysql.exe",
        "${env:ProgramFiles(x86)}\MySQL\MySQL Server *\bin\mysql.exe")) {
        $tim = @(Get-ChildItem $mau -ErrorAction SilentlyContinue | Sort-Object FullName -Descending)
        if ($tim.Count) { return $tim[0].FullName }
    }
    return $null
}

# ---------------------------------------------------------------------
# THU KET NOI THAT toi MySQL, dung DUNG thong tin ung dung se dung
# (user tu MYSQL_USER mac dinh root, mat khau tu MYSQL_PASSWORD).
#
# Mat khau di qua bien moi truong MYSQL_PWD cua TIEN TRINH CON — KHONG bao
# gio nam tren dong lenh (tranh lo qua Task Manager / dong lenh) va KHONG bao
# gio duoc in ra hay ghi vao file.
#
# Tra ve hashtable:
#   Loai = 'OK'      ket noi va xac thuc thanh cong
#          'SAI_MK'  ma 1045 — mat khau/user sai
#          'KHAC'    ma khac (2003 khong ket noi duoc, 1049, 3118 khoa, ...)
#          'KHONG_KIEM_DUOC'  khong tim thay mysql.exe -> khong the kiem tai day
#   Ma        so hieu loi MySQL (khi co), de hien cho nguoi phu trach ky thuat
#   ThongDiep mo ta ngan, KHONG chua mat khau
#
# CHU Y: dung 'localhost' giong JDBC cua ung dung. Tren Windows JDBC
# 'jdbc:mysql://localhost:3306' di bang TCP; mysql.exe -h 127.0.0.1 cung TCP,
# va grant khop deu la 'root'@'localhost'. Khong chi dinh CSDL — buoc nay
# kiem XAC THUC, khong kiem CSDL da nap chua (do la viec cua buoc 5).
# ---------------------------------------------------------------------
function Test-KetNoiMySQL ([string] $matKhau, [string] $nguoiDung = 'root') {
    $mysql = Tim-MysqlExe
    if (-not $mysql) {
        return @{ Loai = 'KHONG_KIEM_DUOC'; Ma = $null
                  ThongDiep = 'Khong tim thay mysql.exe de thu ket noi tai buoc nay.' }
    }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $mysql
    $psi.UseShellExecute = $false
    $psi.CreateNoWindow  = $true
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError  = $true
    # -h 127.0.0.1 va SELECT 1: xac thuc thuan tuy, khong dong toi CSDL nao.
    $psi.Arguments = "-u $nguoiDung -h 127.0.0.1 -P 3306 --connect-timeout=6 --batch --skip-column-names -e ""SELECT 1"""
    # Mat khau qua MYSQL_PWD cua rieng tien trinh con. Dat ca khi rong ('') de
    # MySQL bao dung 'using password: NO' — phan biet duoc voi mat khau sai.
    $psi.EnvironmentVariables['MYSQL_PWD'] = [string] $matKhau
    try {
        $p = [System.Diagnostics.Process]::Start($psi)
        $null = $p.StandardOutput.ReadToEnd()
        $err  = $p.StandardError.ReadToEnd()
        if (-not $p.WaitForExit(15000)) {
            try { $p.Kill() } catch { }
            return @{ Loai = 'KHAC'; Ma = $null
                      ThongDiep = 'mysql.exe khong tra loi trong 15 giay.' }
        }
        if ($p.ExitCode -eq 0) { return @{ Loai = 'OK'; Ma = $null; ThongDiep = 'Ket noi thanh cong.' } }
        # Loc canh bao "Unknown OS character set 'cp1258'" — do KHONG phai loi ket noi.
        $ma = $null
        if ($err -match 'ERROR\s+(\d+)') { $ma = [int] $Matches[1] }
        if ($ma -eq 1045) {
            return @{ Loai = 'SAI_MK'; Ma = 1045; ThongDiep = 'Access denied (1045).' }
        }
        return @{ Loai = 'KHAC'; Ma = $ma
                  ThongDiep = (($err -split "`n" | Where-Object { $_ -match 'ERROR' } | Select-Object -First 1) -replace '\s+', ' ').Trim() }
    } catch {
        return @{ Loai = 'KHONG_KIEM_DUOC'; Ma = $null
                  ThongDiep = "Khong chay duoc mysql.exe: $($_.Exception.Message)" }
    }
}

function Mo-TrinhDuyet {
    Ghi ''
    Ghi "  Đang mở trình duyệt vào $diaChi …" 'Cyan'
    # Bat loi o day, khong de no noi len. May chua dat trinh duyet mac dinh
    # thi Start-Process nem loi, ma $ErrorActionPreference='Stop' se lam ca
    # kich ban chet - dung luc phan mem VUA CHAY XONG. Nguoi dung se thay
    # mot man hinh do lom trong khi thuc ra chi con thieu moi buoc go dia chi.
    try {
        Start-Process $diaChi
    } catch {
        Ghi ''
        Ghi '  Máy này chưa đặt trình duyệt mặc định nên không tự mở được.' 'Yellow'
        Ghi '  Phần mềm VẪN ĐANG CHẠY. Hãy mở trình duyệt rồi gõ dòng địa chỉ này:' 'Yellow'
        Ghi ''
        Ghi "      $diaChi" 'White'
        Ghi ''
    }
}

Clear-Host
Ghi ''
Ghi ('  ' + ('═' * ($RONG + 2))) 'DarkCyan'
Ghi '   QUẢN LÝ THUÊ BAO VÀ TÍNH CƯỚC' 'White'
Ghi ('  ' + ('═' * ($RONG + 2))) 'DarkCyan'

# =====================================================================
# BUOC 1 — Phan mem da chay san chua?
# Chay lan hai KHONG duoc khoi dong them tien trinh thu hai: hai ban cung
# ghi vao mot CSDL, va ban thu hai chet ngay voi "Port 8080 already in use".
# =====================================================================
Tieu 'Bước 1/5 — Kiểm tra phần mềm đã chạy chưa'

if (Test-CongMo 8080) {
    if (Test-TraLoiHttp) {
        Dat 'Phần mềm đã chạy sẵn, đang mở trình duyệt…'
        Mo-TrinhDuyet
        Ghi ''
        Ghi '  Không khởi động lần hai. Cửa sổ này đóng được ngay.' 'DarkGray'
        Ghi '  Phần mềm vẫn chạy ở cửa sổ đã mở từ trước.' 'DarkGray'
        Ghi ''
        Start-Sleep -Seconds 4
        exit 0
    }
    # Cong bi giu nhung khong tra loi HTTP => KHONG phai phan mem nay.
    # Mo trinh duyet luc nay se ra mot trang la, gay hoang mang hon la bao loi.
    $ai = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
    $ten = 'không rõ'
    if ($ai) {
        $tt = Get-Process -Id (@($ai)[0].OwningProcess) -ErrorAction SilentlyContinue
        if ($tt) { $ten = "$($tt.ProcessName) (số hiệu $($tt.Id))" }
    }
    Loi "Cổng 8080 đang bị một chương trình khác chiếm: $ten. Đó không phải phần mềm quản lý thuê bao." @(
        '1. Đóng chương trình đó lại rồi bấm lại biểu tượng này.',
        '2. Nếu không biết đó là chương trình gì, khởi động lại máy tính.'
    )
}
Dat 'Cổng 8080 đang trống, sẽ khởi động bản mới.'

# =====================================================================
# BUOC 2 — MySQL
# Ten dich vu KHONG duoc go cung. Tren may dang phat trien ten la MySQL84,
# may khac co the la MySQL80 hay MySQL. Go cung mot ten la chi nguoi dung di
# tim mot dich vu khong ton tai tren may ho.
# =====================================================================
Tieu 'Bước 2/5 — Kiểm tra kho dữ liệu MySQL'

if (-not (Test-CongMo 3306)) {
    $dv = @(Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -like 'mysql*' })

    if ($dv.Count -eq 0) {
        Loi 'Máy tính này không có MySQL đang chạy, và cũng không tìm thấy dịch vụ MySQL nào đã cài.' @(
            '1. Kiểm tra xem MySQL đã được cài chưa.',
            '2. Nếu đã cài: bấm phím Windows, gõ  services.msc  rồi bấm Enter,',
            '   tìm dòng bắt đầu bằng "MySQL", bấm chuột phải rồi chọn Start.',
            '3. Xong thì bấm lại biểu tượng này.'
        )
    }

    $mot = $dv[0]
    Ghi "  MySQL ($($mot.Name)) đang tắt. Đang bật lên…" 'Yellow'
    $batDuoc = $false
    try {
        Start-Service -Name $mot.Name
        $batDuoc = $true
    } catch {
        $batDuoc = $false
    }

    if (-not $batDuoc) {
        # Bat dich vu can quyen quan tri. KHONG tu nang quyen - huong dan
        # nguoi dung tu lam, va goi DUNG TEN dich vu tim duoc tren may ho.
        Loi "Không bật được MySQL vì thiếu quyền quản trị. Tên dịch vụ trên máy này là: $($mot.Name)." @(
            '1. Bấm phím Windows, gõ chữ:  services.msc  rồi bấm Enter.',
            "2. Tìm dòng tên  $($mot.Name)  trong danh sách.",
            '3. Bấm chuột phải vào nó rồi chọn Start.',
            '4. Quay lại bấm vào biểu tượng phần mềm một lần nữa.'
        )
    }

    # Dich vu bao "dang chay" khong co nghia la da nhan ket noi. Phai doi cong
    # 3306 that su mo ra.
    $het = (Get-Date).AddSeconds(30)
    while (-not (Test-CongMo 3306) -and (Get-Date) -lt $het) { Start-Sleep -Milliseconds 500 }

    if (-not (Test-CongMo 3306)) {
        Loi "Đã bật dịch vụ $($mot.Name) nhưng sau 30 giây nó vẫn chưa nhận kết nối." @(
            '1. Đợi thêm một phút rồi bấm lại biểu tượng này.',
            '2. Vẫn vậy thì khởi động lại máy tính.'
        )
    }
    Dat "Đã bật MySQL ($($mot.Name))."
} else {
    Dat 'MySQL đang chạy.'
}

# =====================================================================
# BUOC 3 — Mat khau MySQL — KIEM KET NOI THAT, khong chi kiem bien co ton tai.
#
# Ban cu chi kiem [string]::IsNullOrWhiteSpace($env:MYSQL_PASSWORD): bien CO
# ton tai la in dau tich xanh roi di tiep. Nhung "bien co gia tri" khong chung
# minh "gia tri do dung". Nguoi dung thay tich xanh o day roi that bai mai tan
# buoc 5 — phep kiem bao an toan trong khi thu no canh (ket noi CSDL) dang hong.
#
# Nay buoc 3 mo MOT ket noi that bang mysql.exe, dung dung user + mat khau ma
# ung dung se dung, va CHI in tich xanh khi xac thuc thanh cong. Sai thi DUNG
# NGAY tai day, khong chay tiep sang buoc 4-5. Mat khau khong bao gio bi in ra.
#
# Vi sao mysql.exe chu khong mo TCP doc goi chao: doc goi chao chi chung minh
# MySQL dang lang nghe (buoc 2 da biet the), KHONG kiem duoc mat khau — dung
# thu can canh. mysql.exe la client chinh chu, xu ly dung caching_sha2 nhu
# Connector/J, nen ket qua 1045 cua no bao truoc dung ket qua cua ung dung.
# =====================================================================
Tieu 'Bước 3/5 — Kiểm tra kết nối tới kho dữ liệu'

$nguoiDung = if ([string]::IsNullOrWhiteSpace($env:MYSQL_USER)) { 'root' } else { $env:MYSQL_USER }

# TH 1 & 3: bien rong trong TIEN TRINH NAY (dung thu ung dung con se nhan).
if ([string]::IsNullOrWhiteSpace($env:MYSQL_PASSWORD)) {
    # Phan biet "chua dat bao gio" voi "da dat nhung cua so nay mo truoc do".
    $daLuu = [Environment]::GetEnvironmentVariable('MYSQL_PASSWORD', 'User')
    if ([string]::IsNullOrWhiteSpace($daLuu)) {
        $daLuu = [Environment]::GetEnvironmentVariable('MYSQL_PASSWORD', 'Machine')
    }
    if (-not [string]::IsNullOrWhiteSpace($daLuu)) {
        # TH 3: da co trong may, chi la cua so nay mo TRUOC luc dat. Bam lai
        # bieu tuong = tien trinh moi = doc duoc bien. Day la cho hay vap nhat.
        Loi 'Máy đã lưu mật khẩu rồi, nhưng cửa sổ này mở trước lúc đó nên chưa thấy.' @(
            '1. Đóng cửa sổ này lại.',
            '2. Bấm lại biểu tượng phần mềm trên Desktop.',
            '   (Biến môi trường chỉ vào được cửa sổ mở MỚI sau khi đặt — nên chỉ',
            '    cần mở lại là xong, không phải đặt lại gì.)'
        )
    }
    # TH 1: that su chua dat bao gio.
    Loi 'Máy tính chưa lưu mật khẩu MySQL, nên phần mềm không vào được kho dữ liệu.' @(
        '1. Bấm phím Windows, gõ chữ:  powershell  rồi bấm Enter.',
        '2. Gõ dòng sau, thay phần trong ngoặc bằng mật khẩu MySQL thật:',
        '',
        '      setx MYSQL_PASSWORD "mật_khẩu_MySQL_của_bạn"',
        '',
        '3. QUAN TRỌNG: đóng cửa sổ vừa gõ xong lại.',
        '   Lệnh setx chỉ có hiệu lực với cửa sổ mở MỚI sau đó — đây là chỗ hay nhầm nhất.',
        '4. Bấm lại biểu tượng phần mềm.'
    )
}

# Bien CO gia tri — gio thu ket noi THAT.
$kn = Test-KetNoiMySQL $env:MYSQL_PASSWORD $nguoiDung

if ($kn.Loai -eq 'SAI_MK') {
    # TH 2: da dat nhung mat khau sai.
    Loi 'Mật khẩu MySQL lưu trên máy không đúng, nên MySQL từ chối kết nối (lỗi 1045).' @(
        '1. Bấm phím Windows, gõ  powershell  rồi bấm Enter.',
        '2. Đặt lại cho đúng mật khẩu MySQL thật (thay phần trong ngoặc):',
        '',
        '      setx MYSQL_PASSWORD "mật_khẩu_MySQL_đúng"',
        '',
        '3. Đóng cửa sổ vừa gõ lại — setx chỉ vào được cửa sổ mở MỚI sau đó.',
        '4. Bấm lại biểu tượng phần mềm để thử lại.',
        '   (Không chắc mật khẩu? Chạy  khoi-dong\Kiem-Tra-Moi-Truong.ps1  để dò.)'
    )
}
elseif ($kn.Loai -eq 'KHAC') {
    # TH 3: MySQL tu choi vi ly do khac — neu ro ma loi cho nguoi phu trach.
    $ma = if ($kn.Ma) { "mã $($kn.Ma)" } else { 'không rõ mã' }
    Loi "MySQL từ chối kết nối vì một lý do khác ($ma), không phải do mật khẩu." @(
        '1. Khởi động lại máy tính rồi bấm lại biểu tượng này.',
        '2. Vẫn vậy thì báo người phụ trách kỹ thuật, kèm dòng này:',
        "   $($kn.ThongDiep)"
    )
}
elseif ($kn.Loai -eq 'KHONG_KIEM_DUOC') {
    # Khong tim thay mysql.exe: KHONG chan duong chay (mat khau co the van
    # dung) — canh bao va di tiep. Buoc 5 van bat duoc loi 1045 neu co.
    Ghi "  [!] Không tìm thấy mysql.exe để kiểm mật khẩu tại đây — sẽ biết chắc khi khởi động." 'Yellow'
}
else {
    Dat 'Kết nối kho dữ liệu thành công.'
}

# =====================================================================
# BUOC 4 — Ban dong goi
# Chay tu ban dong goi (.jar) chu khong qua Maven: xem docs/G7-REPORT.md.
# =====================================================================
Tieu 'Bước 4/5 — Chuẩn bị phần mềm'

function Tim-BanDongGoi {
    $ds = @(Get-ChildItem (Join-Path $goc 'target') -Filter '*.jar' -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notlike '*sources*' -and $_.Name -notlike '*javadoc*' })
    if ($ds.Count -eq 0) { return $null }
    return ($ds | Sort-Object LastWriteTime -Descending)[0]
}

$jar = Tim-BanDongGoi
$canDongGoi = $false
$vi = ''

if (-not $jar) {
    $canDongGoi = $true
    $vi = 'chưa có bản đóng gói nào'
} else {
    # pom.xml phai tinh vao: doi thu vien cung lam ban dong goi cu di, ma
    # khong file nao trong src/ thay doi ca.
    $nguon  = @(Get-ChildItem (Join-Path $goc 'src') -Recurse -File -ErrorAction SilentlyContinue)
    $nguon += @(Get-Item (Join-Path $goc 'pom.xml') -ErrorAction SilentlyContinue)
    $moi = $nguon | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($moi -and $moi.LastWriteTime -gt $jar.LastWriteTime) {
        $canDongGoi = $true
        $vi = "mã nguồn mới hơn bản đóng gói ($($moi.Name))"
    }
}

if ($canDongGoi) {
    Ghi "  Cần đóng gói lại vì $vi." 'Yellow'
    Ghi '  Bước này khoảng 10 giây. Lần đầu trên máy mới lâu hơn vì phải tải thư viện.' 'Yellow'
    $ma = 1
    Push-Location $goc
    try {
        & .\mvnw.cmd -q package -DskipTests
        $ma = $LASTEXITCODE
    } finally {
        Pop-Location
    }
    if ($ma -ne 0) {
        Loi 'Đóng gói phần mềm thất bại, nên chưa chạy được.' @(
            '1. Kiểm tra máy có đang kết nối mạng không (lần đầu cần tải thư viện).',
            '2. Báo người phụ trách kỹ thuật kèm dòng chữ này:',
            "   mvnw package thất bại với mã lỗi $ma"
        )
    }
    $jar = Tim-BanDongGoi
    if (-not $jar) {
        Loi 'Đóng gói xong nhưng không thấy file kết quả trong thư mục target.' @(
            '1. Báo người phụ trách kỹ thuật: thư mục target không có file .jar.'
        )
    }
    Dat 'Đóng gói xong.'
} else {
    Dat 'Phần mềm đã sẵn sàng.'
}

# =====================================================================
# BUOC 5 — Chay va cho san sang
# =====================================================================
Tieu 'Bước 5/5 — Đang mở phần mềm'

Remove-Item $nhatKy, $nhatLoi -ErrorAction SilentlyContinue

# ---------------------------------------------------------------------
# So rang buoc tien trinh (Job Object) — de java KHONG THE song lau hon
# cua so nay.
#
# DO DUOC, khong phai lo xa: giet rieng cua so PowerShell thi java van chay
# tiep va van giu cong 8080. Luc do phan mem thanh mot ban chay ma khong
# cua so nao tat duoc, phai vao Task Manager moi dung — dung thu nguoi dung
# nay khong lam duoc.
#
# Dong cua so bang nut X thi Windows co gui CTRL_CLOSE_EVENT va java thuong
# chet theo. Nhung "thuong" khong du: cua so bi treo roi End task, hay
# PowerShell chet vi loi, deu khong gui su kien do. Job Object bit ca ba
# duong — he dieu hanh tu giet java khi tien trinh nay bien mat, bat ke
# bien mat kieu gi. Gia: 0,15 giay bien dich.
# ---------------------------------------------------------------------
$soRangBuoc = [IntPtr]::Zero
try {
    if (-not ('RangBuoc' -as [type])) {
        Add-Type -TypeDefinition @'
using System;
using System.Runtime.InteropServices;
public static class RangBuoc {
    [DllImport("kernel32.dll", CharSet = CharSet.Unicode)]
    private static extern IntPtr CreateJobObject(IntPtr a, string n);
    [DllImport("kernel32.dll")]
    private static extern bool SetInformationJobObject(IntPtr j, int loai, IntPtr tin, uint co);
    [DllImport("kernel32.dll")]
    private static extern bool AssignProcessToJobObject(IntPtr j, IntPtr p);

    [StructLayout(LayoutKind.Sequential)]
    private struct GioiHan {
        public long ThoiGianMoiTienTrinh;
        public long ThoiGianCaSo;
        public uint Co;
        public UIntPtr BoNhoLamViecToiThieu;
        public UIntPtr BoNhoLamViecToiDa;
        public uint SoTienTrinhToiDa;
        public UIntPtr ChonNhan;
        public uint HangUuTien;
        public uint HangDinhThoi;
    }
    [StructLayout(LayoutKind.Sequential)]
    private struct DemVaoRa { public ulong a, b, c, d, e, f; }
    [StructLayout(LayoutKind.Sequential)]
    private struct GioiHanMoRong {
        public GioiHan CoBan;
        public DemVaoRa VaoRa;
        public UIntPtr BoNhoMoiTienTrinh;
        public UIntPtr BoNhoCaSo;
        public UIntPtr DinhMoiTienTrinh;
        public UIntPtr DinhCaSo;
    }

    public static IntPtr Tao() {
        IntPtr so = CreateJobObject(IntPtr.Zero, null);
        if (so == IntPtr.Zero) { return IntPtr.Zero; }
        GioiHanMoRong tin = new GioiHanMoRong();
        tin.CoBan.Co = 0x2000;              // JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
        int co = Marshal.SizeOf(typeof(GioiHanMoRong));
        IntPtr vung = Marshal.AllocHGlobal(co);
        Marshal.StructureToPtr(tin, vung, false);
        bool duoc = SetInformationJobObject(so, 9, vung, (uint)co);
        Marshal.FreeHGlobal(vung);
        return duoc ? so : IntPtr.Zero;
    }
    public static bool Gan(IntPtr so, IntPtr tienTrinh) {
        if (so == IntPtr.Zero) { return false; }
        return AssignProcessToJobObject(so, tienTrinh);
    }
}
'@
    }
    $soRangBuoc = [RangBuoc]::Tao()
} catch {
    # Khong tao duoc so rang buoc thi van chay tiep. Mat mot lop bao ve, chu
    # khong phai ly do de khong mo duoc phan mem.
    $soRangBuoc = [IntPtr]::Zero
}

try {
    # -NoNewWindow: tien trinh java dung CHUNG cua so nay. Nho vay dong cua so
    # la java tat theo, dung nhu nguoi dung mong doi. Neu de java o cua so
    # rieng thi no song sot lai sau khi dong, giu cong 8080, va lan sau khong
    # chay duoc nua.
    #
    # Chuyen huong ra file de man hinh sach: nguoi dung khong can doc log
    # Spring Boot. PowerShell 5.1 KHONG cho ghi chung mot file cho ca hai
    # luong, nen phai tach lam hai.
    $tienTrinh = Start-Process -FilePath 'java' `
        -ArgumentList @('-jar', ('"' + $jar.FullName + '"')) `
        -WorkingDirectory $goc `
        -NoNewWindow -PassThru `
        -RedirectStandardOutput $nhatKy `
        -RedirectStandardError  $nhatLoi

    # Gan ngay sau khi tao. Tu day tro di java nam trong so: tien trinh nay
    # chet la Windows giet java theo, khong con duong nao thanh mo coi.
    #
    # Rao ky: neu Add-Type o tren hong thi lop RangBuoc khong ton tai, goi
    # thang vao no se nem loi va bi bat o catch ben duoi — thanh ra khong mo
    # duoc phan mem chi vi mot lop bao ve phu khong dung duoc. Mat lop bao ve
    # thi chap nhan, chan duong chay thi khong.
    if ($soRangBuoc -ne [IntPtr]::Zero -and ('RangBuoc' -as [type])) {
        try { $null = [RangBuoc]::Gan($soRangBuoc, $tienTrinh.Handle) } catch { }
    }
} catch {
    Loi "Không khởi động được phần mềm: $($_.Exception.Message)" @(
        '1. Kiểm tra máy đã cài Java chưa bằng cách gõ:  java -version',
        '2. Chưa có thì cài Java 21 trở lên rồi bấm lại biểu tượng này.'
    )
}

Ghi ''
Write-Host '  Đang khởi động, thường mất 5–15 giây' -NoNewline
$sanSang = $false
$het = (Get-Date).AddSeconds(120)

while ((Get-Date) -lt $het) {
    # Kiem tien trinh con song KHONG phai nghi thuc. Neu java da chet (sai mat
    # khau chang han) thi hoi tiep 120 giay la bat nguoi dung ngoi nhin mot
    # cai khong bao gio toi.
    if ($tienTrinh.HasExited) { break }
    if (Test-TraLoiHttp) { $sanSang = $true; break }
    Write-Host '.' -NoNewline -ForegroundColor DarkCyan
    Start-Sleep -Seconds 2
}
Ghi ''

if (-not $sanSang) {
    $vet = ''
    foreach ($f in @($nhatLoi, $nhatKy)) {
        if (Test-Path $f) { $vet += (Get-Content $f -Raw -ErrorAction SilentlyContinue) }
    }

    # Dich loi ky thuat sang tieng Viet. Nem nguyen stack trace Java vao mat
    # nhan vien giao dich thi ho khong lam gi duoc voi no.
    $chuyenGi = 'Phần mềm khởi động quá lâu nên đã phải dừng lại.'
    $phaiLam  = @(
        '1. Bấm lại biểu tượng này một lần nữa.',
        '2. Vẫn vậy thì báo người phụ trách kỹ thuật, kèm file:',
        "   $nhatLoi"
    )
    if ($vet -match 'Access denied for user') {
        $chuyenGi = 'Mật khẩu MySQL lưu trên máy không đúng, nên phần mềm bị từ chối kết nối.'
        $phaiLam  = @(
            '1. Bấm phím Windows, gõ  powershell  rồi bấm Enter.',
            '2. Đặt lại mật khẩu cho đúng:',
            '',
            '      setx MYSQL_PASSWORD "mật_khẩu_MySQL_đúng"',
            '',
            '3. Đóng cửa sổ vừa gõ lại, rồi bấm lại biểu tượng này.'
        )
    }
    elseif ($vet -match 'Communications link failure|Connection refused|CommunicationsException') {
        $chuyenGi = 'Phần mềm không liên lạc được với MySQL, dù dịch vụ báo đang chạy.'
        $phaiLam  = @(
            '1. Khởi động lại máy tính rồi bấm lại biểu tượng này.',
            '2. Vẫn vậy thì báo người phụ trách kỹ thuật.'
        )
    }
    elseif ($vet -match 'Unknown database|Unknown table|Table .* doesn') {
        $chuyenGi = 'Kho dữ liệu chưa được nạp lần đầu nên phần mềm không thấy bảng nào.'
        $phaiLam  = @(
            '1. Báo người phụ trách kỹ thuật: cần nạp cơ sở dữ liệu lần đầu.',
            '   (Xem mục "Cài đặt từng bước" trong README.md của dự án.)'
        )
    }
    elseif ($vet -match 'Port 8080 was already in use|Web server failed to start') {
        $chuyenGi = 'Cổng 8080 bị chiếm ngay lúc khởi động.'
        $phaiLam  = @(
            '1. Khởi động lại máy tính rồi bấm lại biểu tượng này.'
        )
    }

    if ($tienTrinh -and -not $tienTrinh.HasExited) {
        Stop-Process -Id $tienTrinh.Id -Force -ErrorAction SilentlyContinue
    }
    Loi $chuyenGi $phaiLam
}

Dat 'Phần mềm đã sẵn sàng.'
Mo-TrinhDuyet

# =====================================================================
# Huong dan cuoi — thu nguoi dung can nhin thay suot buoi lam viec
# =====================================================================
Ghi ''
VienTren
DongKhung 'PHẦN MỀM ĐANG CHẠY' 'White'
VienGiua
DongKhung 'Địa chỉ     http://localhost:8080'
DongKhung ''
DongKhung 'Tài khoản   admin        — toàn bộ hệ thống'
DongKhung '            nhanvien01   — khách hàng và thuê bao'
DongKhung '            ketoan01     — hóa đơn, thu tiền, công nợ'
DongKhung 'Mật khẩu    123456   (cả ba tài khoản)'
DongKhung ''
VienGiua
DongKhung 'Muốn tắt phần mềm: đóng cửa sổ màu đen này' 'Yellow'
VienDuoi
Ghi ''
Ghi '  Lỡ đóng nhầm? Bấm lại biểu tượng trên Desktop là mở lại được.' 'DarkGray'
Ghi ''

# =====================================================================
# Giu cua so — dong cua so la tat phan mem
# =====================================================================
try {
    while (-not $tienTrinh.HasExited) { Start-Sleep -Seconds 1 }
    Ghi ''
    Ghi '  Phần mềm đã dừng.' 'Yellow'
} finally {
    # Chay khi nguoi dung bam Ctrl+C. Truong hop dong cua so bang nut X thi
    # Windows tu tat ca cay tien trinh cua cua so do - da thu that, xem
    # docs/G7-REPORT.md.
    if ($tienTrinh -and -not $tienTrinh.HasExited) {
        Stop-Process -Id $tienTrinh.Id -Force -ErrorAction SilentlyContinue
    }
}
