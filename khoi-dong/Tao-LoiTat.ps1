<#
    Tao-LoiTat.ps1 — chay MOT LAN, tao bieu tuong tren Desktop.

    Sau khi chay xong, nguoi dung chi con viec nhay dup bieu tuong do.

    HAI BAY DA DO DUOC TREN MAY THAT, dung go cung lai:

    1. Desktop KHONG phai $env:USERPROFILE\Desktop.
       May nay dong bo OneDrive nen Desktop that la
       C:\Users\<ten>\OneDrive\Desktop, trong khi C:\Users\<ten>\Desktop VAN
       TON TAI nhung rong. Ghi vao do thi khong bao loi gi ca ma bieu tuong
       khong bao gio hien ra - that bai IM LANG, loai kho tim nhat.
       [Environment]::GetFolderPath('Desktop') hoi dung cho Windows dang dung.

    2. Duong dan du an suy tu $PSScriptRoot, khong go cung D:\KGU\...
       Kho nay chep sang may khac hay thu muc khac van phai chay duoc.
#>

try { [Console]::OutputEncoding = [System.Text.Encoding]::UTF8 } catch { }
$ErrorActionPreference = 'Stop'

$khoiDong = $PSScriptRoot
$goc      = Split-Path -Parent $khoiDong
$kichBan  = Join-Path $khoiDong 'Chay-PhanMem.ps1'
$TEN      = 'Quản lý thuê bao & tính cước'

function Ghi ($chu, $mau = 'Gray') { Write-Host $chu -ForegroundColor $mau }

Ghi ''
Ghi '  ══════════════════════════════════════════════════════════' 'DarkCyan'
Ghi '   TẠO LỐI TẮT TRÊN MÀN HÌNH NỀN' 'White'
Ghi '  ══════════════════════════════════════════════════════════' 'DarkCyan'
Ghi ''

if (-not (Test-Path $kichBan)) {
    Ghi "  Không thấy $kichBan" 'Red'
    Ghi '  Chạy lại file này từ đúng thư mục khoi-dong của dự án.' 'Yellow'
    exit 1
}

# ---------------------------------------------------------------------
# 1. Ve bieu tuong
#    Ve bang hinh khoi co ban chu KHONG dung glyph cua font: font co the
#    thieu tren may khac va glyph thanh o vuong rong. Hinh cot phat song di
#    dung huong thi giac "tram vien thong" da chot o dot G1.
# ---------------------------------------------------------------------
function Tao-BieuTuong ($duongDanIco, $duongDanXem) {
    Add-Type -AssemblyName System.Drawing

    $N    = 256
    $muc  = [System.Drawing.ColorTranslator]::FromHtml('#12283a')   # nền mực
    $sang = [System.Drawing.Color]::White
    $nhan = [System.Drawing.ColorTranslator]::FromHtml('#5bb8c4')   # nhấn cyan

    $bm = New-Object System.Drawing.Bitmap $N, $N
    $g  = [System.Drawing.Graphics]::FromImage($bm)
    $g.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $g.Clear([System.Drawing.Color]::Transparent)

    # Nền bo góc
    $r   = 54
    $duong = New-Object System.Drawing.Drawing2D.GraphicsPath
    $x = 6; $y = 6; $w = 244; $h = 244; $d = $r * 2
    $duong.AddArc($x,           $y,           $d, $d, 180, 90)
    $duong.AddArc($x + $w - $d, $y,           $d, $d, 270, 90)
    $duong.AddArc($x + $w - $d, $y + $h - $d, $d, $d,   0, 90)
    $duong.AddArc($x,           $y + $h - $d, $d, $d,  90, 90)
    $duong.CloseFigure()
    $co = New-Object System.Drawing.SolidBrush $muc
    $g.FillPath($co, $duong)

    # Cot ang-ten: hinh thang thuon
    $coSang = New-Object System.Drawing.SolidBrush $sang
    $cot = @(
        (New-Object System.Drawing.Point 118, 116),
        (New-Object System.Drawing.Point 138, 116),
        (New-Object System.Drawing.Point 154, 216),
        (New-Object System.Drawing.Point 102, 216)
    )
    $g.FillPolygon($coSang, $cot)

    # Hai rãnh cắt ngang cột, tô bằng màu nền -> nhìn ra giàn thép ở cỡ lớn,
    # tự biến mất gọn gàng ở cỡ 16px thay vì thành vệt bẩn.
    $g.FillRectangle($co, 100, 152, 60, 11)
    $g.FillRectangle($co, 100, 182, 60, 11)

    # Chấm phát sóng
    $g.FillEllipse($coSang, 113, 77, 30, 30)

    # Bốn cung sóng toả lên hai bên, đối xứng qua trục đứng
    $tamX = 128; $tamY = 92
    foreach ($bk in @(46, 76)) {
        $but = New-Object System.Drawing.Pen $sang, 13
        $but.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
        $but.EndCap   = [System.Drawing.Drawing2D.LineCap]::Round
        if ($bk -eq 76) { $but.Color = $nhan }
        $o = New-Object System.Drawing.Rectangle ($tamX - $bk), ($tamY - $bk), ($bk * 2), ($bk * 2)
        $g.DrawArc($but, $o, 195, 50)   # cung trên bên trái
        $g.DrawArc($but, $o, 295, 50)   # cung trên bên phải
        $but.Dispose()
    }

    $g.Dispose()
    $bm.Save($duongDanXem, [System.Drawing.Imaging.ImageFormat]::Png)

    # --- Dong goi thanh .ico nhieu co ---
    # .NET khong co ham ghi .ico nhieu co, phai tu dung header. Windows Vista
    # tro len doc duoc khung PNG ben trong .ico.
    $cac = @(16, 24, 32, 48, 64, 128, 256)
    $khung = @()
    foreach ($s in $cac) {
        $nho = New-Object System.Drawing.Bitmap $s, $s
        $g2  = [System.Drawing.Graphics]::FromImage($nho)
        $g2.SmoothingMode     = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
        $g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
        $g2.PixelOffsetMode   = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
        $g2.DrawImage($bm, (New-Object System.Drawing.Rectangle 0, 0, $s, $s))
        $g2.Dispose()
        $ms = New-Object System.IO.MemoryStream
        $nho.Save($ms, [System.Drawing.Imaging.ImageFormat]::Png)
        $khung += , @{ Co = $s; Byte = $ms.ToArray() }
        $ms.Dispose(); $nho.Dispose()
    }
    $bm.Dispose()

    $ra = New-Object System.IO.MemoryStream
    $bw = New-Object System.IO.BinaryWriter $ra
    $bw.Write([UInt16] 0)                 # dự trữ
    $bw.Write([UInt16] 1)                 # loại 1 = icon
    $bw.Write([UInt16] $khung.Count)
    $lech = 6 + 16 * $khung.Count
    foreach ($k in $khung) {
        $c = $k.Co
        if ($c -ge 256) { $c = 0 }        # 0 nghĩa là 256
        $bw.Write([Byte] $c)              # rộng
        $bw.Write([Byte] $c)              # cao
        $bw.Write([Byte] 0)               # số màu bảng
        $bw.Write([Byte] 0)               # dự trữ
        $bw.Write([UInt16] 1)             # mặt phẳng
        $bw.Write([UInt16] 32)            # bit/điểm
        $bw.Write([UInt32] $k.Byte.Length)
        $bw.Write([UInt32] $lech)
        $lech += $k.Byte.Length
    }
    foreach ($k in $khung) { $bw.Write($k.Byte) }
    $bw.Flush()
    [System.IO.File]::WriteAllBytes($duongDanIco, $ra.ToArray())
    $bw.Dispose(); $ra.Dispose()
}

$ico    = Join-Path $khoiDong 'bieu-tuong.ico'
$xemThu = Join-Path $khoiDong 'bieu-tuong-xem-thu.png'
$coIcon = $false
try {
    Tao-BieuTuong $ico $xemThu
    # Doc lai de chac chan Windows hieu duoc file vua ghi. Ghi ra mot file
    # hong ma khong biet thi loi tat se hien bieu tuong trang.
    Add-Type -AssemblyName System.Drawing
    $thu = New-Object System.Drawing.Icon $ico
    $thu.Dispose()
    $coIcon = $true
    Ghi "  [v] Đã vẽ biểu tượng: $(Split-Path -Leaf $ico)" 'Green'
} catch {
    Ghi "  [!] Không vẽ được biểu tượng riêng ($($_.Exception.Message))." 'Yellow'
    Ghi '      Lối tắt sẽ dùng biểu tượng mặc định — không ảnh hưởng gì tới việc chạy.' 'DarkGray'
}

# ---------------------------------------------------------------------
# 2. Tao loi tat
# ---------------------------------------------------------------------
$manHinhNen = [Environment]::GetFolderPath('Desktop')
if ([string]::IsNullOrWhiteSpace($manHinhNen) -or -not (Test-Path $manHinhNen)) {
    Ghi '  Không xác định được thư mục Màn hình nền của bạn.' 'Red'
    exit 1
}

$lnk = Join-Path $manHinhNen "$TEN.lnk"
$psExe = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
if (-not (Test-Path $psExe)) { $psExe = 'powershell.exe' }

# BAY THU BA, do duoc luc chay that:
# WScript.Shell la COM va no day duong dan qua bang ma ANSI. Ten
# "Quản lý thuê bao & tính cước" bi bien thanh "Qu?n l? thuê bao & tính cư?c"
# - chu 'ê í ư' song sot con 'ả ý ớ' thanh dau hoi - roi Save() nem
# FileNotFoundException. Chu Viet co dau KHONG di qua duoc COM nay.
#
# Cach vong: ghi ra ten THUAN ASCII truoc, roi doi ten bang .NET. Ham file
# cua .NET dung Unicode that nen ten tieng Viet qua duoc. Doi ten khong lam
# hong gi ca: file .lnk khong luu ten cua chinh no ben trong.
$tam = Join-Path $khoiDong 'loi-tat-tam.lnk'
Remove-Item $tam -ErrorAction SilentlyContinue

$vo = New-Object -ComObject WScript.Shell
$lt = $vo.CreateShortcut($tam)
$lt.TargetPath = $psExe
# -NoExit: neu kich ban chet vi mot loi khong luong truoc, cua so o lai de doc
# duoc thong bao. Khong co no thi cua so nhay len roi bien mat, nguoi dung
# khong hoc duoc gi.
$lt.Arguments        = '-ExecutionPolicy Bypass -NoExit -File "' + $kichBan + '"'
$lt.WorkingDirectory = $goc
$lt.Description      = 'Mở phần mềm quản lý thuê bao và tính cước'
$lt.WindowStyle      = 1
if ($coIcon) { $lt.IconLocation = "$ico,0" }
$lt.Save()

# Doi sang ten that. [System.IO.File] dung Unicode nen ten co dau qua duoc.
[System.IO.File]::Delete($lnk)
[System.IO.File]::Move($tam, $lnk)

# ---------------------------------------------------------------------
# 3. Doc lai loi tat vua ghi de xac nhan, thay vi tin la da xong
#    KHONG doc lai bang COM: chinh COM la thu khong nuot noi ten co dau.
#    Doc thang byte va tim chuoi UTF-16 cua duong dan kich ban - do la bang
#    chung tham so da nam trong file, khong phai suy doan.
# ---------------------------------------------------------------------
function Co-Chuoi ([byte[]] $byte, [string] $tim) {
    # Chuoi UTF-16 trong .lnk KHONG nam o byte chan. Do duoc: giai ma tu
    # offset 0 thi ghep sai cap byte va khong tim thay gi - phep kiem ban dau
    # bao dong gia dung vi ly do nay. Phai thu ca hai the le chan va le le,
    # them ca dang mot byte vi .lnk luu duong dan o CA HAI dang.
    if ([System.Text.Encoding]::Unicode.GetString($byte).Contains($tim)) { return $true }
    if ($byte.Length -gt 1 -and
        [System.Text.Encoding]::Unicode.GetString($byte, 1, $byte.Length - 1).Contains($tim)) { return $true }
    if ([System.Text.Encoding]::ASCII.GetString($byte).Contains($tim)) { return $true }
    return $false
}

$ok = $false
if ([System.IO.File]::Exists($lnk)) {
    $byte = [System.IO.File]::ReadAllBytes($lnk)
    $ok   = ($byte.Length -gt 0) -and
            (Co-Chuoi $byte 'Chay-PhanMem.ps1') -and
            (Co-Chuoi $byte 'powershell.exe')
}

Ghi ''
if ($ok) {
    Ghi '  [v] Đã tạo xong lối tắt (đọc lại nội dung file để chắc chắn).' 'Green'
} else {
    Ghi '  [!] Lối tắt tạo ra nhưng đọc lại không khớp — hãy báo người phụ trách kỹ thuật.' 'Red'
}
Ghi ''
Ghi "  Tên       $TEN" 'White'
Ghi "  Nằm ở     $manHinhNen"
Ghi "  Chạy      $kichBan"
Ghi ''
Ghi '  Xong rồi. Từ giờ chỉ cần nháy đúp biểu tượng đó trên màn hình nền.' 'Cyan'
Ghi '  Cửa sổ này đóng được.' 'DarkGray'
Ghi ''
