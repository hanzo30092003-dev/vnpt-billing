<#
    Kiem-Tra-Moi-Truong.ps1 — do moi truong khi co su co, TACH khoi luong khoi
    dong thuong. Chi CHAN DOAN: khong khoi dong ung dung, khong ghi gi, khong
    dong vao CSDL ngoai mot cau SELECT 1 de thu xac thuc.

    Chay: bam chuot phai file nay -> Run with PowerShell. Hoac trong PowerShell:
        powershell -ExecutionPolicy Bypass -File khoi-dong\Kiem-Tra-Moi-Truong.ps1

    KHONG in mat khau ra man hinh: chi bao co/khong, do dai, va ket qua ket noi.

    Luu kem BOM UTF-8 (le cua du an).
#>
try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$goc = Split-Path -Parent $PSScriptRoot

function Ghi ($chu, $mau = 'Gray') { Write-Host $chu -ForegroundColor $mau }
function OK  ($chu) { Ghi "  [v] $chu" 'Green' }
function KO  ($chu) { Ghi "  [x] $chu" 'Red' }
function Canh($chu) { Ghi "  [!] $chu" 'Yellow' }

function Test-CongMo ([int] $cong) {
    [bool] (Get-NetTCPConnection -LocalPort $cong -State Listen -ErrorAction SilentlyContinue)
}

# Giong het Chay-PhanMem.ps1 — de day noi gi thi luong khoi dong cung y het.
function Tim-MysqlExe {
    $dv = @(Get-CimInstance Win32_Service -Filter "Name LIKE 'mysql%'" -ErrorAction SilentlyContinue |
            Sort-Object { $_.State -ne 'Running' })
    foreach ($d in $dv) {
        if ($d.PathName -match '"?([A-Za-z]:\\[^"]*?mysqld\.exe)') {
            $mysql = Join-Path (Split-Path $Matches[1]) 'mysql.exe'
            if (Test-Path $mysql) { return $mysql }
        }
    }
    $g = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($g) { return $g.Source }
    foreach ($mau in @(
        "$env:ProgramFiles\MySQL\MySQL Server *\bin\mysql.exe",
        "${env:ProgramFiles(x86)}\MySQL\MySQL Server *\bin\mysql.exe")) {
        $tim = @(Get-ChildItem $mau -ErrorAction SilentlyContinue | Sort-Object FullName -Descending)
        if ($tim.Count) { return $tim[0].FullName }
    }
    return $null
}

function Test-KetNoiMySQL ([string] $matKhau, [string] $nguoiDung = 'root') {
    $mysql = Tim-MysqlExe
    if (-not $mysql) { return @{ Loai = 'KHONG_KIEM_DUOC'; Ma = $null; ThongDiep = 'Khong tim thay mysql.exe.' } }
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $mysql; $psi.UseShellExecute = $false; $psi.CreateNoWindow = $true
    $psi.RedirectStandardOutput = $true; $psi.RedirectStandardError = $true
    $psi.Arguments = "-u $nguoiDung -h 127.0.0.1 -P 3306 --connect-timeout=6 --batch --skip-column-names -e ""SELECT 1"""
    $psi.EnvironmentVariables['MYSQL_PWD'] = [string] $matKhau
    try {
        $p = [System.Diagnostics.Process]::Start($psi)
        $null = $p.StandardOutput.ReadToEnd(); $err = $p.StandardError.ReadToEnd()
        if (-not $p.WaitForExit(15000)) { try { $p.Kill() } catch { }; return @{ Loai='KHAC'; Ma=$null; ThongDiep='mysql.exe khong tra loi trong 15 giay.' } }
        if ($p.ExitCode -eq 0) { return @{ Loai='OK'; Ma=$null; ThongDiep='Ket noi thanh cong.' } }
        $ma = $null; if ($err -match 'ERROR\s+(\d+)') { $ma = [int] $Matches[1] }
        if ($ma -eq 1045) { return @{ Loai='SAI_MK'; Ma=1045; ThongDiep='Access denied (1045).' } }
        return @{ Loai='KHAC'; Ma=$ma; ThongDiep=(($err -split "`n" | Where-Object { $_ -match 'ERROR' } | Select-Object -First 1) -replace '\s+',' ').Trim() }
    } catch { return @{ Loai='KHONG_KIEM_DUOC'; Ma=$null; ThongDiep="Khong chay duoc mysql.exe: $($_.Exception.Message)" } }
}

function MoTa ($v) {
    if ($null -eq $v) { 'KHÔNG CÓ' }
    elseif ($v -eq '') { 'RỖNG' }
    else { "có (dài $($v.Length))" }
}

Clear-Host
Ghi ''
Ghi '  ══ KIỂM TRA MÔI TRƯỜNG — chẩn đoán khi phần mềm không mở được ══' 'Cyan'
Ghi ''

# 1. Cong 8080
Ghi '  1) Cổng 8080 (phần mềm):' 'White'
if (Test-CongMo 8080) { Canh 'Đang có chương trình chiếm cổng 8080 (có thể chính phần mềm đang chạy).' }
else { OK 'Cổng 8080 trống.' }

# 2. MySQL
Ghi ''
Ghi '  2) Dịch vụ MySQL:' 'White'
$dv = @(Get-Service -ErrorAction SilentlyContinue | Where-Object { $_.Name -like 'mysql*' })
if ($dv.Count -eq 0) { KO 'Không tìm thấy dịch vụ MySQL nào đã cài.' }
else { foreach ($d in $dv) { if ($d.Status -eq 'Running') { OK "$($d.Name): đang chạy." } else { KO "$($d.Name): đang TẮT (bật ở services.msc rồi Start)." } } }
if (Test-CongMo 3306) { OK 'Cổng 3306 đang nhận kết nối.' } else { KO 'Cổng 3306 chưa nhận kết nối.' }

# 3. Bien mat khau — khong in gia tri
Ghi ''
Ghi '  3) Biến MYSQL_PASSWORD (chỉ báo có/không và độ dài, KHÔNG in giá trị):' 'White'
$tt = $env:MYSQL_PASSWORD
$nd = [Environment]::GetEnvironmentVariable('MYSQL_PASSWORD', 'User')
$my = [Environment]::GetEnvironmentVariable('MYSQL_PASSWORD', 'Machine')
Ghi "      Trong phiên này (ứng dụng con sẽ nhận) : $(MoTa $tt)"
Ghi "      Lưu cố định cho tài khoản (User)         : $(MoTa $nd)"
Ghi "      Lưu cố định cho cả máy (Machine)         : $(MoTa $my)"
if ([string]::IsNullOrWhiteSpace($tt) -and -not [string]::IsNullOrWhiteSpace($nd)) {
    Canh 'Máy có lưu mật khẩu, nhưng cửa sổ này mở trước lúc đặt nên chưa thấy — mở cửa sổ mới là thấy.'
}

# 4. Thu ket noi that
Ghi ''
Ghi '  4) Thử kết nối thật (đúng cách ứng dụng kết nối):' 'White'
$nguoiDung = if ([string]::IsNullOrWhiteSpace($env:MYSQL_USER)) { 'root' } else { $env:MYSQL_USER }
$mysql = Tim-MysqlExe
if ($mysql) { Ghi "      Dùng: $mysql" 'DarkGray' } else { Canh 'Không tìm thấy mysql.exe — không kiểm được mật khẩu ở đây.' }
if ([string]::IsNullOrWhiteSpace($tt)) {
    KO "Biến trống trong phiên này nên chưa thử được (user: $nguoiDung)."
} else {
    $kn = Test-KetNoiMySQL $tt $nguoiDung
    switch ($kn.Loai) {
        'OK'     { OK "Kết nối và xác thực thành công (user: $nguoiDung). Phần mềm sẽ vào được kho dữ liệu." }
        'SAI_MK' { KO "Mật khẩu SAI — MySQL từ chối (lỗi 1045). Đặt lại: setx MYSQL_PASSWORD ""...""  rồi mở cửa sổ mới." }
        'KHAC'   { KO "MySQL từ chối vì lý do khác (mã $($kn.Ma)): $($kn.ThongDiep)" }
        default  { Canh "Chưa kiểm được: $($kn.ThongDiep)" }
    }
}

Ghi ''
Ghi '  ─────────────────────────────────────────────────────────────' 'DarkGray'
Ghi '  Đọc xong thì đóng cửa sổ này. Đây chỉ là công cụ chẩn đoán,' 'DarkGray'
Ghi '  không khởi động phần mềm và không thay đổi gì.' 'DarkGray'
Ghi ''
