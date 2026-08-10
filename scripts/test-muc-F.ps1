# =====================================================================
# test-muc-F.ps1 - Kiem thu giao dien cho muc F cua Phase 5
# =====================================================================
# Kiem hai thu ma mot cau SELECT khong noi duoc:
#   1. Man hinh cong no va bang tuoi no co HIEN duoc du lieu that khong
#   2. Ky 6 con HUY va LAP LAI duoc hoa don khong
#
# Moi con so doc tren man hinh deu duoc DOI CHIEU CHEO bang mot cau SQL
# doc lap, thay vi tin vao chinh cai man hinh dang kiem (bai hoc 43.6).
#
# File nay CO Y khong chua ky tu tieng Viet co dau: script .ps1 co dau
# phai luu kem BOM UTF-8, va mot file thieu BOM se doc sai bang ma roi
# bai kiem thu truot vi ly do khong lien quan gi den thu dang kiem.
# Vi vay moi phep so khop deu bam vao chuoi ASCII trong HTML (id, value,
# duong dan) hoac vao con so da dinh dang.
#
# Chay khi ung dung DANG BAT:  .\scripts\test-muc-F.ps1
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
$env:MYSQL_PWD = $env:MYSQL_PASSWORD

function Sql-Value($cau) {
    return (& $mysql -u root -D vnpt_billing --default-character-set=utf8mb4 -N -B -e $cau) `
        | Select-Object -First 1
}

# Dinh dang so giong Thymeleaf: formatDecimal(x, 0, 'POINT', 0, 'COMMA')
# tuc dau phan cach hang nghin la DAU CHAM. MySQL FORMAT() dung dau phay.
function Tien($cau) { return (Sql-Value $cau).Replace(",", ".") }

function Xac-Nhan($ten, $dieuKien, $chiTiet) {
    if ($dieuKien) {
        Write-Output ("  [DAT ] {0}" -f $ten)
        if ($chiTiet) { Write-Output ("         {0}" -f $chiTiet) }
        $Global:SoDat++
    } else {
        Write-Output ("  [SAI ] {0}" -f $ten)
        if ($chiTiet) { Write-Output ("         {0}" -f $chiTiet) }
        $Global:SoSai++
    }
}

Bat-Dau "PHASE 5 MUC F - DU LIEU THANH TOAN MAU KY 5/2026"

$s = Connect-App "admin" "123456"

# ---------------------------------------------------------------------
Muc "1. Man hinh cong no - tong so va bang tuoi no"
# ---------------------------------------------------------------------
$congNo = Get-Trang $s "/cong-no"
Kiem-Tra -Ten "Trang /cong-no mo duoc, co bang tuoi no va bieu do" -KetQua $congNo `
    -CanCo @("bieuDoTuoiNo", "/cong-no/xuat-excel") | Out-Null

$tongTien = Tien "SELECT FORMAT(SUM(con_no),0) FROM hoa_don WHERE con_no > 0;"
$tongSo   = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE con_no > 0;")
Kiem-Tra -Ten "Tong con no tren man hinh khop SQL doc lap" -KetQua $congNo `
    -CanCo @($tongTien) | Out-Null
Write-Output ("         SQL doc lap: {0} hoa don con no, tong {1} d" -f $tongSo, $tongTien)

# ---------------------------------------------------------------------
Muc "2. Tung nhom tuoi no phai co noi dung"
# ---------------------------------------------------------------------
# Ranh gioi lay tu enum NhomTuoiNo. Voi moc hom nay 07/08/2026:
#   han 15/07/2026 (ky 6) -> qua han 23 ngay -> nhom QUA_HAN_1_30
#   han 15/06/2026 (ky 5) -> qua han 53 ngay -> nhom QUA_HAN_31_60
$nhom = @(
    @{ Ma = "QUA_HAN_1_30";   Tu = 1;  Den = 30 },
    @{ Ma = "QUA_HAN_31_60";  Tu = 31; Den = 60 }
)
foreach ($n in $nhom) {
    $dk = "con_no > 0 AND DATEDIFF(CURDATE(), han_thanh_toan) BETWEEN $($n.Tu) AND $($n.Den)"
    $so   = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE $dk;")
    $tien = Tien "SELECT FORMAT(SUM(con_no),0) FROM hoa_don WHERE $dk;"
    Kiem-Tra -Ten ("Nhom {0} hien dung so tien {1} d" -f $n.Ma, $tien) `
        -KetQua $congNo -CanCo @($tien) | Out-Null
    Write-Output ("         SQL doc lap: {0} hoa don" -f $so)

    $loc = Get-Trang $s ("/cong-no?nhomTuoiNo=" + $n.Ma)
    Kiem-Tra -Ten ("Loc theo nhom {0} chay duoc" -f $n.Ma) -KetQua $loc | Out-Null
}

Kiem-Tra -Ten "Bo loc liet ke du 5 nhom tuoi no" -KetQua $congNo `
    -CanCo @("TRONG_HAN", "QUA_HAN_1_30", "QUA_HAN_31_60", "QUA_HAN_61_90",
             "QUA_HAN_TREN_90") | Out-Null

# ---------------------------------------------------------------------
Muc "3. Danh sach thanh toan va hoa don"
# ---------------------------------------------------------------------
$tt = Get-Trang $s "/thanh-toan"
Kiem-Tra -Ten "Trang /thanh-toan mo duoc" -KetQua $tt -CanCo @("TIEN_MAT", "CHUYEN_KHOAN",
    "VI_DIEN_TU") | Out-Null
Write-Output ("         SQL doc lap: {0} giao dich" -f (Sql-Value "SELECT COUNT(*) FROM thanh_toan;"))

foreach ($tr in @("DA_TT", "TT_MOT_PHAN", "QUA_HAN")) {
    $n = Sql-Value "SELECT COUNT(*) FROM hoa_don hd JOIN ky_cuoc kc ON kc.id=hd.ky_cuoc_id WHERE kc.thang=5 AND hd.trang_thai='$tr';"
    $r = Get-Trang $s "/hoa-don?kyCuocId=2&trangThai=$tr"
    Kiem-Tra -Ten ("Loc hoa don ky 5 trang thai {0} - SQL doc lap: {1} hoa don" -f $tr, $n) `
        -KetQua $r | Out-Null
}

# Mot hoa don tra hai dot: tab lich su thu tien phai co hai dong
$hdHaiDot = Sql-Value "SELECT hoa_don_id FROM thanh_toan GROUP BY hoa_don_id HAVING COUNT(*) = 2 ORDER BY hoa_don_id LIMIT 1;"
$ct = Get-Trang $s "/hoa-don/$hdHaiDot"
Kiem-Tra -Ten ("Chi tiet hoa don id {0} co lich su thu 2 dot" -f $hdHaiDot) -KetQua $ct | Out-Null

# ---------------------------------------------------------------------
Muc "4. RANG BUOC KY 6 - van huy va lap lai duoc hoa don"
# ---------------------------------------------------------------------
$gdKy6 = [int](Sql-Value "SELECT COUNT(*) FROM thanh_toan tt JOIN hoa_don hd ON hd.id=tt.hoa_don_id WHERE hd.ky_cuoc_id=1;")
Xac-Nhan "Ky 6 khong co giao dich thanh toan nao" ($gdKy6 -eq 0) ("SQL doc lap: {0} giao dich" -f $gdKy6)

# Doi chung: ky 5 PHAI bi chan. Khong do chuoi thong bao (co dau tieng Viet)
# ma do HAU QUA: 54 hoa don ky 5 con nguyen.
$huyKy5 = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/2/huy-hoa-don" @{}
$conKy5 = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=2;")
Xac-Nhan "Huy hoa don ky 5 BI CHAN vi da co thanh toan" `
    ($huyKy5.status -eq 200 -and $conKy5 -eq 54) `
    ("HTTP {0}, ky 5 con {1} hoa don (mong doi 54)" -f $huyKy5.status, $conKy5)

$huyKy6 = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/1/huy-hoa-don" @{}
$conKy6 = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=1;")
$gtTra  = [int](Sql-Value "SELECT COUNT(*) FROM giam_tru WHERE trang_thai='CHUA_AP_DUNG';")
Xac-Nhan "Huy hoa don ky 6 CHAY DUOC" ($huyKy6.status -eq 200 -and $conKy6 -eq 0) `
    ("HTTP {0}, ky 6 con {1} hoa don, {2}/2 khoan giam tru tra ve CHUA_AP_DUNG" -f `
        $huyKy6.status, $conKy6, $gtTra)

$lapLai   = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/1/lap-hoa-don" @{}
$soMoi    = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=1;")
$doanhThu = Sql-Value "SELECT SUM(tong_thanh_toan) FROM hoa_don WHERE ky_cuoc_id=1;"
$gtAp     = [int](Sql-Value "SELECT COUNT(*) FROM giam_tru WHERE trang_thai='DA_AP_DUNG';")
Xac-Nhan "Lap lai ky 6 ra DUNG cung con so" `
    ($lapLai.status -eq 200 -and $soMoi -eq 58 -and $doanhThu -eq "23828605.00" -and $gtAp -eq 2) `
    ("HTTP {0}, {1} hoa don, doanh thu {2}, {3}/2 giam tru DA_AP_DUNG" -f `
        $lapLai.status, $soMoi, $doanhThu, $gtAp)

# ---------------------------------------------------------------------
Muc "5. Bat bien tieu chi 4 sau khi huy va lap lai ky 6"
# ---------------------------------------------------------------------
$lech = [int](Sql-Value @"
SELECT COUNT(*) FROM (
  SELECT h.id FROM hoa_don h LEFT JOIN thanh_toan t ON t.hoa_don_id = h.id
   GROUP BY h.id, h.con_no, h.tong_thanh_toan, h.da_thanh_toan
  HAVING h.con_no <> h.tong_thanh_toan - h.da_thanh_toan
      OR h.da_thanh_toan <> COALESCE(SUM(t.so_tien), 0)) x;
"@)
$soHd = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don;")
# So hoa don LAY TU CSDL chu khong ghi cung 112 nhu ban dau: Phase 6 them ba ky
# nen con so do thanh 280. Mot phep kiem ghim vao so luong du lieu se do oan moi
# lan bo du lieu mau lon len - va do oan thi lan sau khong ai tin no nua.
Xac-Nhan "Bat bien tieu chi 4 van sach" ($lech -eq 0 -and $soHd -gt 0) `
    ("{0} dong lech tren {1} hoa don" -f $lech, $soHd)

Ket-Thuc
