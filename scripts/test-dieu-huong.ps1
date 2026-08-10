# =====================================================================
# test-dieu-huong.ps1 - Phase 7 muc A2
# =====================================================================
# Di theo MENU, khong go URL cung.
#
# VI SAO PHAI CO SCRIPT NAY
# Phase 6 phat hien bon man hinh cua Phase 5 (hoa don, thanh toan, cong no,
# giam tru) de href="#" suot ca mot phase - nguoi dung khong co duong nao toi
# chung ngoai viec go thang URL. Khong phep kiem nao bat duoc, vi MOI phep
# kiem deu goi thang duong dan tuyet doi, tuc DI VONG QUA dung cai thu bi hong.
#
# Script nay lam nguoc lai: dang nhap, DOC href tu HTML tra ve, roi goi tiep
# theo dung nhung duong dan do. Neu mot muc menu tro vao hu khong thi no lo ra
# ngay - vi script khong biet truoc URL nao ca.
#
# File nay CO Y khong chua ky tu tieng Viet co dau.
#
# Chay khi ung dung DANG BAT:  .\scripts\test-dieu-huong.ps1
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

function Xac-Nhan($ten, $dieuKien, $chiTiet) {
    if ($dieuKien) { Write-Host ("  [DAT ] {0}" -f $ten); $Global:SoDat++ }
    else           { Write-Host ("  [SAI ] {0}" -f $ten); $Global:SoSai++ }
    if ($chiTiet)  { Write-Host ("         {0}" -f $chiTiet) }
}

# Rut moi href cua the <a class="nav-item"> trong sidebar
function Lay-MucMenu($html) {
    $ketQua = @()
    foreach ($m in [regex]::Matches($html, '<a[^>]*class="nav-item[^"]*"[^>]*href="([^"]+)"')) {
        $ketQua += $m.Groups[1].Value
    }
    # Mot so the dat href TRUOC class - bat ca hai thu tu
    foreach ($m in [regex]::Matches($html, '<a[^>]*href="([^"]+)"[^>]*class="nav-item[^"]*"')) {
        $ketQua += $m.Groups[1].Value
    }
    return $ketQua | Sort-Object -Unique
}

# Rut moi lien ket noi bo trong PHAN NOI DUNG (khong phai sidebar)
function Lay-LienKetNoiDung($html) {
    $ketQua = @()
    foreach ($m in [regex]::Matches($html, 'href="(/[^"#]*)"')) {
        $d = $m.Groups[1].Value
        if ($d -notmatch '^/(css|js|images|webjars)/') { $ketQua += $d }
    }
    return $ketQua | Sort-Object -Unique
}

Bat-Dau "PHASE 7 MUC A2 - DI THEO MENU, KHONG GO URL CUNG"

# Menu mong doi theo vai tro. CHI liet ke so luong toi thieu, con duong dan
# thi DOC TU HTML - neu ghi cung duong dan o day thi lai quay ve dung loi cu.
$vaiTro = @(
    @{ Ten = "admin";      MucToiThieu = 12 },
    @{ Ten = "nhanvien01"; MucToiThieu = 4  },
    @{ Ten = "ketoan01";   MucToiThieu = 6  }
)

$tongMuc = 0
foreach ($vt in $vaiTro) {
    Muc ("Vai tro " + $vt.Ten)
    $s = Connect-App $vt.Ten "123456"

    $trangChu = Get-Trang $s "/"
    Kiem-Tra -Ten ("$($vt.Ten): trang chu mo duoc") -KetQua $trangChu | Out-Null

    $menu = Lay-MucMenu $trangChu.body
    Xac-Nhan ("$($vt.Ten): sidebar co it nhat $($vt.MucToiThieu) muc") `
        ($menu.Count -ge $vt.MucToiThieu) ("doc duoc {0} muc tu HTML" -f $menu.Count)

    $hong = @()
    foreach ($duongDan in $menu) {
        $r = Get-Trang $s $duongDan
        $tongMuc++
        if ($r.status -ne 200) { $hong += ("{0} -> HTTP {1}" -f $duongDan, $r.status) }
    }
    Xac-Nhan ("$($vt.Ten): MOI muc menu deu tra HTTP 200") ($hong.Count -eq 0) `
        $(if ($hong.Count -eq 0) { "da bam thu " + $menu.Count + " muc" } else { $hong -join " | " })

    Write-Host ("         Menu thay duoc: " + ($menu -join "  "))
}

# ---------------------------------------------------------------------
Muc "Di sau mot cap: tu moi muc menu, bam tiep cac lien ket ben trong"
# ---------------------------------------------------------------------
# Chi lam voi admin (thay nhieu man hinh nhat). Khong di qua lien ket co
# tham so id de tranh no ra hang tram request; nhung lien ket do da duoc
# KiemTraDieuHuongTest doi chieu voi bang dinh tuyen roi.
$s = Connect-App "admin" "123456"
$menu = Lay-MucMenu (Get-Trang $s "/").body
$daTham = @{}
$hongCap2 = @()
$soCap2 = 0

foreach ($duongDan in $menu) {
    $trang = Get-Trang $s $duongDan
    foreach ($lk in (Lay-LienKetNoiDung $trang.body)) {
        # Bo lien ket co phan dong (id so) va lien ket tai file
        if ($lk -match '/\d+') { continue }
        if ($lk -match 'xuat-excel|xuat-pdf|tai-file|phieu-thu') { continue }
        if ($daTham.ContainsKey($lk)) { continue }
        $daTham[$lk] = $true
        $r = Get-Trang $s $lk
        $soCap2++
        if ($r.status -ne 200) { $hongCap2 += ("{0} -> HTTP {1}" -f $lk, $r.status) }
    }
}
Xac-Nhan "Moi lien ket cap 2 deu tra HTTP 200" ($hongCap2.Count -eq 0) `
    $(if ($hongCap2.Count -eq 0) { "da bam thu $soCap2 lien ket" } else { $hongCap2 -join " | " })

# ---------------------------------------------------------------------
Muc "Doi chung: vai tro khong du quyen thi menu KHONG hien muc do"
# ---------------------------------------------------------------------
# Neu phep kiem tren xanh chi vi menu rong thi doi chung nay se do.
$menuNhanVien = Lay-MucMenu (Get-Trang (Connect-App "nhanvien01" "123456") "/").body
$menuKeToan   = Lay-MucMenu (Get-Trang (Connect-App "ketoan01" "123456") "/").body

Xac-Nhan "nhanvien01 KHONG thay muc Hoa don trong menu" `
    (-not ($menuNhanVien -contains "/hoa-don")) ("menu: " + ($menuNhanVien -join " "))
Xac-Nhan "ketoan01 KHONG thay muc Khach hang trong menu" `
    (-not ($menuKeToan -contains "/khach-hang")) ("menu: " + ($menuKeToan -join " "))
Xac-Nhan "Ca ba vai tro deu thay muc Bao cao" `
    (($menuNhanVien -contains "/bao-cao") -and ($menuKeToan -contains "/bao-cao")) ""

Write-Host ""
Write-Host ("  Tong so lan bam menu: {0}" -f $tongMuc)

Ket-Thuc
