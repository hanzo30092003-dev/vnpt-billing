# =====================================================================
# test-bien.ps1 - Phase 7 muc C3 va C4
# =====================================================================
# C3: truong hop bien tren form - rong, so am, so qua lon, ngay khong hop le,
#     chuoi qua dai, ky tu dac biet, ngay tuong lai
# C4: go thang URL vao chuc nang khong co quyen -> 403
#
# NGUYEN TAC: moi ca o day deu la ca BI CHAN. Script khong tao ra du lieu moi
# nao, nen chay bao nhieu lan cung khong lam ban CSDL.
#
# Chay khi ung dung DANG BAT:  .\scripts\test-bien.ps1
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
$env:MYSQL_PWD = $env:MYSQL_PASSWORD
function Sql-Value($cau) {
    return (& $mysql -u root -D vnpt_billing --default-character-set=utf8mb4 -N -B -e $cau) `
        | Select-Object -First 1
}
function Xac-Nhan($ten, $dieuKien, $chiTiet) {
    if ($dieuKien) { Write-Host ("  [DAT ] {0}" -f $ten); $Global:SoDat++ }
    else           { Write-Host ("  [SAI ] {0}" -f $ten); $Global:SoSai++ }
    if ($chiTiet)  { Write-Host ("         {0}" -f $chiTiet) }
}

Bat-Dau "PHASE 7 MUC C3 + C4 - TRUONG HOP BIEN VA PHAN QUYEN"

$admin = Connect-App "admin" "123456"

# ---------------------------------------------------------------------
Muc "1. Tham so URL bat thuong - khong duoc tra 500"
# ---------------------------------------------------------------------
# Nut "Truoc" o trang dau sinh ?trang=-1. Loi nay da lam 500 truoc Phase 7.
$bienUrl = @(
    @{ Ten = "trang am";            D = "/hoa-don?trang=-1" },
    @{ Ten = "trang am (thanh toan)"; D = "/thanh-toan?trang=-5" },
    @{ Ten = "trang am (khach hang)"; D = "/khach-hang?trang=-99" },
    @{ Ten = "trang cuc lon";       D = "/hoa-don?trang=999999" },
    @{ Ten = "trang khong phai so"; D = "/hoa-don?trang=abc";        Mong = 400 },
    @{ Ten = "id khong ton tai";    D = "/hoa-don/999999" },
    @{ Ten = "id khong phai so";    D = "/hoa-don/abc";              Mong = 400 },
    @{ Ten = "ky cuoc khong ton tai"; D = "/bao-cao/san-luong?kyCuocId=999999" },
    @{ Ten = "so tien loc am";      D = "/hoa-don?tuSoTien=-1000" },
    @{ Ten = "doi soat thue bao la"; D = "/tinh-cuoc/doi-soat/999999/1" }
)
foreach ($b in $bienUrl) {
    $r = Get-Trang $admin $b.D
    $mong = if ($b.Mong) { $b.Mong } else { 200 }
    # Chap nhan 200 (xu ly duoc), 400 (tu choi tham so sai kieu) hoac 302
    # (chuyen huong kem thong bao). CHI 500 la sai.
    $ok = $r.status -ne 500 -and $r.status -ne 0
    Xac-Nhan ("{0}" -f $b.Ten) $ok ("HTTP {0}  {1}" -f $r.status, $b.D)
}

# ---------------------------------------------------------------------
Muc "2. Form khach hang - chuoi qua dai va ky tu dac biet"
# ---------------------------------------------------------------------
$soKhTruoc = [int](Sql-Value "SELECT COUNT(*) FROM khach_hang;")

$chuoiDai = "A" * 500
$r = Post-Form $admin "/khach-hang/them" "/khach-hang/luu" @{
    loaiKh = "CA_NHAN"; tenKh = $chuoiDai; soGiayTo = "012345678901"
    diaChi = "Can Tho"; ngayDangKy = "2026-01-01"; trangThai = "HOAT_DONG"
}
$soKhSau = [int](Sql-Value "SELECT COUNT(*) FROM khach_hang;")
Xac-Nhan "Ten khach hang 500 ky tu bi chan" ($r.status -eq 200 -and $soKhSau -eq $soKhTruoc) `
    ("HTTP {0}, so khach hang {1} -> {2}" -f $r.status, $soKhTruoc, $soKhSau)

# CO Y BO TRONG dia chi de ban ghi BI TU CHOI. Hai ly do:
#   1. Script nay khong duoc tao du lieu moi - lan chay dau da lo tao that mot
#      khach hang ten "<script>alert(1)</script>" va phai xoa tay
#   2. Va quan trong hon: cho NGUY HIEM cua XSS chinh la luc form VE LAI kem
#      gia tri vua nhap. Ca bi tu choi moi dung la ca can kiem.
$r = Post-Form $admin "/khach-hang/them" "/khach-hang/luu" @{
    loaiKh = "CA_NHAN"; tenKh = "<script>alert(1)</script>"; soGiayTo = "012345678901"
    diaChi = ""; ngayDangKy = "2026-01-01"; trangThai = "HOAT_DONG"
}
$soKhSau2 = [int](Sql-Value "SELECT COUNT(*) FROM khach_hang;")
$coScript = $r.body -match "<script>alert\(1\)</script>"
$coEscape = $r.body -match "&lt;script&gt;"
Xac-Nhan "The script duoc escape khi form ve lai (chong XSS), khong luu ban ghi" `
    ((-not $coScript) -and $soKhSau2 -eq $soKhTruoc) `
    ("co the script nguyen ven: {0}; co dang da escape: {1}; so khach hang {2} -> {3}" -f `
        $coScript, $coEscape, $soKhTruoc, $soKhSau2)

# ---------------------------------------------------------------------
Muc "3. Form thanh toan - so am, so qua lon, ngay tuong lai"
# ---------------------------------------------------------------------
# Lay mot hoa don CON NO de thu
$hoaDonId = Sql-Value "SELECT id FROM hoa_don WHERE con_no > 0 ORDER BY id LIMIT 1;"
$conNo    = Sql-Value "SELECT con_no FROM hoa_don WHERE id=$hoaDonId;"
$soGdTruoc = [int](Sql-Value "SELECT COUNT(*) FROM thanh_toan;")

$caThanhToan = @(
    @{ Ten = "so tien am";        SoTien = "-100000"; Ngay = "2026-08-01" },
    @{ Ten = "so tien bang 0";    SoTien = "0";       Ngay = "2026-08-01" },
    @{ Ten = "so tien vuot con no"; SoTien = "999999999"; Ngay = "2026-08-01" },
    @{ Ten = "ngay o tuong lai";  SoTien = "10000";   Ngay = "2027-01-01" }
)
foreach ($ca in $caThanhToan) {
    $r = Post-Form $admin "/thanh-toan/moi/$hoaDonId" "/thanh-toan" @{
        hoaDonId = $hoaDonId; soTien = $ca.SoTien; hinhThuc = "TIEN_MAT"
        ngayThanhToan = $ca.Ngay
    }
    $soGdSau = [int](Sql-Value "SELECT COUNT(*) FROM thanh_toan;")
    Xac-Nhan ("Thanh toan: {0} bi chan" -f $ca.Ten) `
        ($r.status -eq 200 -and $soGdSau -eq $soGdTruoc) `
        ("HTTP {0}, so giao dich van {1}" -f $r.status, $soGdSau)
}

# ---------------------------------------------------------------------
Muc "4. Form sinh CDR - so luong bat thuong"
# ---------------------------------------------------------------------
$soCdrTruoc = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung;")
$caSinh = @(
    @{ Ten = "so luong am";       SL = "-100";    Tu = "2026-08-01"; Den = "2026-08-31" },
    @{ Ten = "so luong 0";        SL = "0";       Tu = "2026-08-01"; Den = "2026-08-31" },
    @{ Ten = "so luong vuot tran"; SL = "999999"; Tu = "2026-08-01"; Den = "2026-08-31" },
    @{ Ten = "ngay ket thuc truoc ngay bat dau"; SL = "10"; Tu = "2026-08-31"; Den = "2026-08-01" }
)
foreach ($ca in $caSinh) {
    $r = Post-Form $admin "/cdr/sinh-du-lieu" "/cdr/sinh-du-lieu" @{
        tuNgay = $ca.Tu; denNgay = $ca.Den; soLuong = $ca.SL; phamVi = "TAT_CA"
    }
    $soCdrSau = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung;")
    Xac-Nhan ("Sinh CDR: {0} bi chan" -f $ca.Ten) `
        ($r.status -eq 200 -and $soCdrSau -eq $soCdrTruoc) `
        ("HTTP {0}, so CDR van {1}" -f $r.status, $soCdrSau)
}

# ---------------------------------------------------------------------
Muc "5. Form ky cuoc - thang/nam ngoai khoang, trung ky"
# ---------------------------------------------------------------------
$soKyTruoc = [int](Sql-Value "SELECT COUNT(*) FROM ky_cuoc;")
$caKy = @(
    @{ Ten = "thang 13";      Thang = 13;  Nam = 2026 },
    @{ Ten = "thang 0";       Thang = 0;   Nam = 2026 },
    @{ Ten = "nam 1900";      Thang = 5;   Nam = 1900 },
    @{ Ten = "trung ky da co"; Thang = 6;  Nam = 2026 }
)
foreach ($ca in $caKy) {
    $r = Post-Form $admin "/ky-cuoc" "/ky-cuoc/tao-moi" @{ thang = $ca.Thang; nam = $ca.Nam }
    $soKySau = [int](Sql-Value "SELECT COUNT(*) FROM ky_cuoc;")
    Xac-Nhan ("Ky cuoc: {0} bi chan" -f $ca.Ten) `
        ($r.status -eq 200 -and $soKySau -eq $soKyTruoc) `
        ("HTTP {0}, so ky van {1}" -f $r.status, $soKySau)
}

# ---------------------------------------------------------------------
Muc "6. C4 - go thang URL vao chuc nang khong co quyen -> 403"
# ---------------------------------------------------------------------
$nhanVien = Connect-App "nhanvien01" "123456"
$keToan   = Connect-App "ketoan01" "123456"

$ca403 = @(
    @{ Ten = "nhanvien01 -> /hoa-don";   Phien = $nhanVien; D = "/hoa-don" },
    @{ Ten = "nhanvien01 -> /thanh-toan"; Phien = $nhanVien; D = "/thanh-toan" },
    @{ Ten = "nhanvien01 -> /cong-no";   Phien = $nhanVien; D = "/cong-no" },
    @{ Ten = "nhanvien01 -> /giam-tru";  Phien = $nhanVien; D = "/giam-tru" },
    @{ Ten = "nhanvien01 -> /tinh-cuoc"; Phien = $nhanVien; D = "/tinh-cuoc" },
    @{ Ten = "nhanvien01 -> /goi-cuoc";  Phien = $nhanVien; D = "/goi-cuoc" },
    @{ Ten = "nhanvien01 -> /cdr";       Phien = $nhanVien; D = "/cdr" },
    @{ Ten = "ketoan01 -> /khach-hang";  Phien = $keToan;   D = "/khach-hang" },
    @{ Ten = "ketoan01 -> /thue-bao";    Phien = $keToan;   D = "/thue-bao" },
    @{ Ten = "ketoan01 -> /tinh-cuoc";   Phien = $keToan;   D = "/tinh-cuoc" },
    @{ Ten = "ketoan01 -> /ky-cuoc";     Phien = $keToan;   D = "/ky-cuoc" },
    @{ Ten = "ketoan01 -> /bang-gia";    Phien = $keToan;   D = "/bang-gia" }
)
foreach ($ca in $ca403) {
    $r = Get-Trang $ca.Phien $ca.D
    Xac-Nhan ("{0} tra 403" -f $ca.Ten) ($r.status -eq 403) ("HTTP {0}" -f $r.status)
}

# Doi chung: 403 phai la vi THIEU QUYEN, khong phai vi duong dan hong.
# Admin mo dung nhung duong dan do phai duoc.
$hong = @()
foreach ($d in @("/hoa-don", "/thanh-toan", "/cong-no", "/giam-tru", "/tinh-cuoc",
                 "/goi-cuoc", "/cdr", "/khach-hang", "/thue-bao", "/ky-cuoc", "/bang-gia")) {
    if ((Get-Trang $admin $d).status -ne 200) { $hong += $d }
}
Xac-Nhan "Doi chung: admin mo duoc TAT CA nhung duong dan tren" ($hong.Count -eq 0) `
    $(if ($hong.Count -eq 0) { "11/11 duong dan tra 200" } else { $hong -join " " })

# ---------------------------------------------------------------------
Muc "7. Khong dang nhap thi bi day ve trang dang nhap"
# ---------------------------------------------------------------------
$khach = $null
Invoke-WebRequest -Uri "$Global:BaseUrl/dang-nhap" -SessionVariable khach -UseBasicParsing -TimeoutSec 10 | Out-Null
foreach ($d in @("/", "/hoa-don", "/bao-cao", "/thue-bao")) {
    $r = Get-Trang $khach $d
    $veDangNhap = $r.body -match 'name="matKhau"'
    Xac-Nhan ("Chua dang nhap: {0} bi day ve form dang nhap" -f $d) $veDangNhap `
        ("HTTP {0}" -f $r.status)
}

# ---------------------------------------------------------------------
Muc "8. CSDL khong bi thay doi boi script nay"
# ---------------------------------------------------------------------
$soKh  = [int](Sql-Value "SELECT COUNT(*) FROM khach_hang;")
$soGd  = [int](Sql-Value "SELECT COUNT(*) FROM thanh_toan;")
$soCdr = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung;")
$soKy  = [int](Sql-Value "SELECT COUNT(*) FROM ky_cuoc;")
Xac-Nhan "Moi ca deu la ca BI CHAN - CSDL khong doi" `
    ($soKh -eq $soKhTruoc -and $soGd -eq $soGdTruoc -and $soCdr -eq $soCdrTruoc -and $soKy -eq $soKyTruoc) `
    ("khach_hang {0}, thanh_toan {1}, CDR {2}, ky_cuoc {3}" -f $soKh, $soGd, $soCdr, $soKy)

Ket-Thuc
