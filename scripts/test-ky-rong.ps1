# =====================================================================
# test-ky-rong.ps1 - Phase 7 muc C2
# =====================================================================
# RA LOI "KY RONG" TREN TOAN BO HE THONG, khong chi bao cao.
#
# Loi tim duoc o Phase 6: SUM() tren mot ky chua co CDR nao tra ve NULL, ma
# ban ghi nhan gia tri long nguyen thuy => Hibernate khong mo hop duoc =>
# HTTP 500. Loi do KHONG BAO GIO lo ra tren du lieu mau vi moi ky deu co CDR;
# no chi xay ra dung luc nguoi dung vua tao mot ky moi.
#
# Cung dang loi do co the con o: dashboard, bang doi soat, man hinh tinh cuoc,
# cong no. Script nay tao ky 8/2026 RONG HOAN TOAN (khong CDR, khong hoa don)
# roi mo LAN LUOT moi man hinh co lien quan toi ky.
#
# Ky 8/2026 duoc GIU LAI sau khi chay - de demo truc tiep (sinh CDR ngay tren
# san khau). Xem docs/kich-ban-demo.md buoc 5.
#
# Chay khi ung dung DANG BAT:  .\scripts\test-ky-rong.ps1
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

Bat-Dau "PHASE 7 MUC C2 - RA LOI KY RONG TREN TOAN BO HE THONG"

$s = Connect-App "admin" "123456"

# ---------------------------------------------------------------------
Muc "1. Tao ky 8/2026 rong hoan toan"
# ---------------------------------------------------------------------
$kyId = Sql-Value "SELECT id FROM ky_cuoc WHERE thang=8 AND nam=2026;"
if (-not $kyId) {
    Post-Form $s "/ky-cuoc" "/ky-cuoc/tao-moi" @{ thang = 8; nam = 2026 } | Out-Null
    $kyId = Sql-Value "SELECT id FROM ky_cuoc WHERE thang=8 AND nam=2026;"
}

# --- DON DEP TRUOC KHI CHAY: lam script CHAY LAI DUOC ---
# Muc 5 ben duoi co y thuc hien cac thao tac PHA HUY tren ky 8. Neu lan chay
# truoc dut giua chung, ky 8 con hoa don hoac da bi chot, va lan chay nay se
# do vi mot ly do do CHINH NO gay ra o lan truoc - dung kieu bao dong gia ma
# bai hoc 43.5 canh bao.
#
# Mo lai ky bang SQL truc tiep vi chot ky la thao tac MOT CHIEU theo thiet ke,
# co y khong co nut mo lai. Phase 4E cung da lam dung cach nay khi kiem chung.
if ((Sql-Value "SELECT trang_thai FROM ky_cuoc WHERE id=$kyId;") -ne "MO") {
    & $mysql -u root -D vnpt_billing -e `
        "UPDATE ky_cuoc SET trang_thai='MO', ngay_chot=NULL WHERE id=$kyId;" | Out-Null
    Write-Host "  [ -- ] Da mo lai ky 8 (lan chay truoc de lai trang thai DA_CHOT)"
}
if ([int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=$kyId;") -gt 0) {
    Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/huy-hoa-don" @{} | Out-Null
    Write-Host "  [ -- ] Da huy hoa don ky 8 con sot lai tu lan chay truoc"
}
$soCdr  = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung WHERE ky_cuoc_id=$kyId;")
$soHoaDon = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=$kyId;")
Xac-Nhan "Ky 8/2026 ton tai va RONG HOAN TOAN" `
    ($null -ne $kyId -and $soCdr -eq 0 -and $soHoaDon -eq 0) `
    ("ky_cuoc_id = {0}, {1} CDR, {2} hoa don" -f $kyId, $soCdr, $soHoaDon)

# Mot thue bao bat ky de thu bang doi soat
$thueBaoId = Sql-Value "SELECT id FROM thue_bao WHERE loai_thue_bao='TRA_SAU' ORDER BY id LIMIT 1;"

# ---------------------------------------------------------------------
Muc "2. MOI man hinh co tham so ky - mo voi ky 8/2026 rong"
# ---------------------------------------------------------------------
$manHinh = @(
    @{ Ten = "Bao cao doanh thu theo goi";  D = "/bao-cao/doanh-thu-goi-cuoc?kyCuocId=$kyId" },
    @{ Ten = "Bao cao doanh thu theo dich vu"; D = "/bao-cao/doanh-thu-dich-vu?kyCuocId=$kyId" },
    @{ Ten = "Bao cao top thue bao";        D = "/bao-cao/top-thue-bao?kyCuocId=$kyId" },
    @{ Ten = "Bao cao san luong dich vu";   D = "/bao-cao/san-luong?kyCuocId=$kyId" },
    @{ Ten = "Danh sach hoa don loc ky 8";  D = "/hoa-don?kyCuocId=$kyId" },
    # /cdr KHONG co bo loc theo ky cuoc - form loc chi co so thue bao, khoang
    # ngay, dich vu, huong, tinh trang, nguon. Ban cu truyen ?kyCuocId=8 roi
    # khang dinh HTTP 200: tham so do bi bo qua, man hinh tra ve ca 18.723 ban
    # ghi, va phep kiem xanh du KHONG he kiem duoc gi ve duong du lieu rong.
    # Doi sang mot bo loc man hinh THUC SU hieu va chac chan khong khop gi.
    @{ Ten = "Danh sach CDR voi bo loc khong khop gi"; D = "/cdr?soThueBao=0000000000" },
    @{ Ten = "Hoa don cua ky 8";            D = "/tinh-cuoc/ky/$kyId" },
    @{ Ten = "BANG DOI SOAT ky 8";          D = "/tinh-cuoc/doi-soat/$thueBaoId/$kyId" },
    @{ Ten = "Giam tru loc ky 8";           D = "/giam-tru?kyCuocId=$kyId" }
)
foreach ($mh in $manHinh) {
    $r = Get-Trang $s $mh.D
    Xac-Nhan ("{0}" -f $mh.Ten) ($r.status -eq 200) ("HTTP {0}  {1}" -f $r.status, $mh.D)
}

# ---------------------------------------------------------------------
Muc "3. Man hinh KHONG co tham so ky nhung phai chiu duoc ky rong"
# ---------------------------------------------------------------------
# Dashboard chon "ky gan nhat CO hoa don" - ky 8 rong khong duoc lam no vo.
# Man hinh tinh cuoc liet ke MOI ky, ke ca ky chua chay gi.
$khac = @(
    @{ Ten = "Dashboard trang chu";      D = "/" },
    @{ Ten = "Man hinh tinh cuoc";       D = "/tinh-cuoc" },
    @{ Ten = "Danh sach ky cuoc";        D = "/ky-cuoc" },
    @{ Ten = "Cong no";                  D = "/cong-no" },
    @{ Ten = "Bao cao cong no tong hop"; D = "/bao-cao/cong-no" },
    @{ Ten = "Bao cao doanh thu theo ky";D = "/bao-cao/doanh-thu-ky" },
    @{ Ten = "Bao cao thong ke thue bao";D = "/bao-cao/thue-bao" },
    @{ Ten = "Menu bao cao";             D = "/bao-cao" }
)
foreach ($mh in $khac) {
    $r = Get-Trang $s $mh.D
    Xac-Nhan ("{0}" -f $mh.Ten) ($r.status -eq 200) ("HTTP {0}  {1}" -f $r.status, $mh.D)
}

# ---------------------------------------------------------------------
Muc "4. Xuat Excel cua moi bao cao voi ky rong"
# ---------------------------------------------------------------------
foreach ($bc in @("doanh-thu-goi-cuoc", "doanh-thu-dich-vu", "top-thue-bao", "san-luong")) {
    try {
        $r = Invoke-WebRequest -Uri "$Global:BaseUrl/bao-cao/$bc/xuat-excel?kyCuocId=$kyId" `
                -WebSession $s -UseBasicParsing -TimeoutSec 60
        $laZip = $r.Content.Length -gt 2 -and $r.Content[0] -eq 0x50 -and $r.Content[1] -eq 0x4B
        Xac-Nhan "Xuat Excel $bc (ky rong)" ($r.StatusCode -eq 200 -and $laZip) `
            ("HTTP {0}, {1:N0} byte" -f $r.StatusCode, $r.Content.Length)
    } catch {
        Xac-Nhan "Xuat Excel $bc (ky rong)" $false ("Loi: " + $_.Exception.Message)
    }
}

# ---------------------------------------------------------------------
Muc "5. Thao tac nghiep vu tren ky rong phai BAO LOI TIENG VIET, khong 500"
# ---------------------------------------------------------------------
# Chot mot ky chua co hoa don nao phai bi tu choi - va tu choi bang thong bao
# nghiep vu, khong phai bang trang loi he thong.
$r = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/chot-ky" @{}
$trangThai = Sql-Value "SELECT trang_thai FROM ky_cuoc WHERE id=$kyId;"
Xac-Nhan "Chot ky rong BI CHAN va ky van MO" ($r.status -eq 200 -and $trangThai -eq "MO") `
    ("HTTP {0}, trang_thai = {1}" -f $r.status, $trangThai)

# Lap hoa don khi ky KHONG CO CDR NAO.
# ⭐ PHAT HIEN: viec nay CHAY DUOC va tao ra 58 hoa don chi co cuoc thue bao
# thang (18.750.000 d + VAT). Ve nghiep vu day la dung - thue bao tra sau no
# cuoc thue bao thang du khong phat sinh cuoc goi nao. Nhung no cho thay hai
# dieu can ghi vao tai lieu:
#   1. "Ky rong" khong dong nghia "khong lap duoc hoa don"
#   2. Ky 8/2026 CHUA KET THUC (het 31/08) ma van bi tinh TRON cuoc thang,
#      khong prorate theo so ngay da qua
# Khong sua thanh chan lai: kich ban demo CO Y lap hoa don ky 8 ngay tren san
# khau. Ghi vao PHASE-7-REPORT muc han che thay vi doi hanh vi.
$r = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/lap-hoa-don" @{}
$soHd = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=$kyId;")
Xac-Nhan "Lap hoa don tren ky rong khong gay loi 500" ($r.status -eq 200) `
    ("HTTP {0}, ky 8 co {1} hoa don (chi cuoc thue bao thang)" -f $r.status, $soHd)

# Tru cuoc tra truoc tren ky rong
$r = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/tru-cuoc-tra-truoc" @{}
$soDongSoCai = [int](Sql-Value "SELECT COUNT(*) FROM bien_dong_so_du WHERE ky_cuoc_id=$kyId;")
Xac-Nhan "Tru cuoc tra truoc tren ky rong khong gay loi 500, khong ghi so cai" `
    ($r.status -eq 200 -and $soDongSoCai -eq 0) `
    ("HTTP {0}, {1} dong so cai" -f $r.status, $soDongSoCai)

# --- HOAN TAC: tra ky 8 ve trang thai rong de con demo truc tiep ---
# Buoc nay BAT BUOC. Khong hoan tac thi script tu lam ban dung cai ky ma no
# duoc viet ra de bao ve.
#
# ⭐ VA DAY LA CHO DE SAI: sau khi lap hoa don, ky 8 KHONG con rong nen phep
# kiem "chot ky rong bi chan" o tren se CHOT THAT neu chay lai. Vi vay thu tu
# trong muc 5 la co y: chot TRUOC (luc ky con rong), lap hoa don SAU, roi huy.
$r = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/huy-hoa-don" @{}
$soHdSauHuy = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=$kyId;")
Xac-Nhan "Huy hoa don ky 8 - tra ky ve rong" ($r.status -eq 200 -and $soHdSauHuy -eq 0) `
    ("HTTP {0}, ky 8 con {1} hoa don" -f $r.status, $soHdSauHuy)

# ---------------------------------------------------------------------
Muc "6. Ky 8 van RONG sau khi ra - san sang de demo"
# ---------------------------------------------------------------------
$soCdrCuoi   = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung WHERE ky_cuoc_id=$kyId;")
$soHoaDonCuoi = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=$kyId;")
$ttCuoi = Sql-Value "SELECT trang_thai FROM ky_cuoc WHERE id=$kyId;"
Xac-Nhan "Ky 8/2026 van rong va van MO" `
    ($soCdrCuoi -eq 0 -and $soHoaDonCuoi -eq 0 -and $ttCuoi -eq "MO") `
    ("{0} CDR, {1} hoa don, trang_thai {2}" -f $soCdrCuoi, $soHoaDonCuoi, $ttCuoi)

# Bat bien van sach
$lech = [int](Sql-Value @"
SELECT COUNT(*) FROM (
  SELECT h.id FROM hoa_don h LEFT JOIN thanh_toan t ON t.hoa_don_id = h.id
   GROUP BY h.id, h.con_no, h.tong_thanh_toan, h.da_thanh_toan
  HAVING h.con_no <> h.tong_thanh_toan - h.da_thanh_toan
      OR h.da_thanh_toan <> COALESCE(SUM(t.so_tien), 0)) x;
"@)
Xac-Nhan "Bat bien thanh toan van 0 lech" ($lech -eq 0) `
    ("{0} dong lech tren {1} hoa don" -f $lech, (Sql-Value "SELECT COUNT(*) FROM hoa_don;"))

Ket-Thuc
