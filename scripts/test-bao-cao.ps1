# =====================================================================
# test-bao-cao.ps1 - Phase 6 muc B, C, D
# =====================================================================
# Kiem ba thu:
#   1. Dashboard va bay bao cao deu MO DUOC (HTTP 200, khong 500)
#   2. Con so tren man hinh KHOP voi mot cau SQL doc lap - tieu chi 6
#   3. Nut Xuat Excel tra ve file xlsx that (chu ky ZIP "PK"), khong rong
#
# File nay CO Y khong chua ky tu tieng Viet co dau - xem ghi chu trong
# test-muc-F.ps1. Moi phep so khop bam vao chuoi ASCII hoac con so da
# dinh dang.
#
# Chay khi ung dung DANG BAT:  .\scripts\test-bao-cao.ps1
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
$env:MYSQL_PWD = $env:MYSQL_PASSWORD

function Sql-Value($cau) {
    return (& $mysql -u root -D vnpt_billing --default-character-set=utf8mb4 -N -B -e $cau) `
        | Select-Object -First 1
}
# Dinh dang giong DinhDangTien: dau CHAM phan cach hang nghin
function Tien($cau) { return (Sql-Value $cau).Replace(",", ".") }

function Xac-Nhan($ten, $dieuKien, $chiTiet) {
    if ($dieuKien) { Write-Host ("  [DAT ] {0}" -f $ten); $Global:SoDat++ }
    else           { Write-Host ("  [SAI ] {0}" -f $ten); $Global:SoSai++ }
    if ($chiTiet)  { Write-Host ("         {0}" -f $chiTiet) }
}

# Tai file Excel va kiem no la xlsx that
function Kiem-Excel($session, $duongDan, $ten) {
    try {
        $r = Invoke-WebRequest -Uri "$Global:BaseUrl$duongDan" -WebSession $session `
                -UseBasicParsing -TimeoutSec 60
        $b = $r.Content
        # xlsx la file ZIP: hai byte dau phai la 'P' 'K' (0x50 0x4B)
        $laZip = $b.Length -gt 2 -and $b[0] -eq 0x50 -and $b[1] -eq 0x4B
        Xac-Nhan "Xuat Excel: $ten" ($r.StatusCode -eq 200 -and $laZip -and $b.Length -gt 2000) `
            ("HTTP {0}, {1:N0} byte, chu ky ZIP {2}" -f $r.StatusCode, $b.Length, $laZip)
    } catch {
        Xac-Nhan "Xuat Excel: $ten" $false ("Loi: " + $_.Exception.Message)
    }
}

Bat-Dau "PHASE 6 - DASHBOARD VA BAY BAO CAO"

$s = Connect-App "admin" "123456"

# ---------------------------------------------------------------------
Muc "1. Dashboard trang chu"
# ---------------------------------------------------------------------
$tc = Get-Trang $s "/"
Kiem-Tra -Ten "Trang chu mo duoc, co du 3 bieu do" -KetQua $tc `
    -CanCo @("bieuDoDoanhThu", "bieuDoGoi", "bieuDoTrangThai") | Out-Null

$tongTb   = Sql-Value "SELECT COUNT(*) FROM thue_bao;"
$dangHd   = Sql-Value "SELECT COUNT(*) FROM thue_bao WHERE trang_thai='HOAT_DONG';"
$congNo   = Tien "SELECT FORMAT(SUM(con_no),0) FROM hoa_don WHERE con_no > 0;"
$kyGanNhat = Sql-Value "SELECT CONCAT(thang,'/',nam) FROM ky_cuoc kc WHERE (SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=kc.id)>0 ORDER BY nam DESC, thang DESC LIMIT 1;"
$dtGanNhat = Tien "SELECT FORMAT(SUM(hd.tong_thanh_toan),0) FROM hoa_don hd JOIN ky_cuoc kc ON kc.id=hd.ky_cuoc_id WHERE CONCAT(kc.thang,'/',kc.nam)=(SELECT CONCAT(thang,'/',nam) FROM ky_cuoc k2 WHERE (SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=k2.id)>0 ORDER BY nam DESC, thang DESC LIMIT 1);"

Kiem-Tra -Ten "[Kiem cheo 1] Tong thue bao = $tongTb" -KetQua $tc -CanCo @($tongTb) | Out-Null
Kiem-Tra -Ten "[Kiem cheo 2] Tong cong no = $congNo d" -KetQua $tc -CanCo @($congNo) | Out-Null
Kiem-Tra -Ten "[Kiem cheo 3] Doanh thu ky gan nhat ($kyGanNhat) = $dtGanNhat d" -KetQua $tc `
    -CanCo @($dtGanNhat) | Out-Null
Write-Host ("         SQL doc lap: {0} thue bao, {1} dang hoat dong" -f $tongTb, $dangHd)

# ---------------------------------------------------------------------
Muc "2. Trang menu bao cao"
# ---------------------------------------------------------------------
$r = Get-Trang $s "/bao-cao"
Kiem-Tra -Ten "Menu bao cao liet ke du 7 duong dan" -KetQua $r `
    -CanCo @("/bao-cao/doanh-thu-ky", "/bao-cao/doanh-thu-goi-cuoc", "/bao-cao/doanh-thu-dich-vu",
             "/bao-cao/thue-bao", "/bao-cao/top-thue-bao", "/bao-cao/san-luong",
             "/bao-cao/cong-no") | Out-Null

# ---------------------------------------------------------------------
Muc "3. C.1 - Doanh thu theo ky"
# ---------------------------------------------------------------------
$r = Get-Trang $s "/bao-cao/doanh-thu-ky"
$tongPhatSinh = Tien "SELECT FORMAT(SUM(tong_thanh_toan),0) FROM hoa_don;"
$tongDaThu    = Tien "SELECT FORMAT(SUM(da_thanh_toan),0) FROM hoa_don;"
$soKy         = Sql-Value "SELECT COUNT(*) FROM ky_cuoc;"
Kiem-Tra -Ten "Trang mo duoc, co bieu do" -KetQua $r -CanCo @("bieuDoDoanhThu") | Out-Null
Kiem-Tra -Ten "[Kiem cheo 4] Tong phat sinh moi ky = $tongPhatSinh d" -KetQua $r `
    -CanCo @($tongPhatSinh) | Out-Null
Kiem-Tra -Ten "[Kiem cheo 5] Tong da thu = $tongDaThu d" -KetQua $r -CanCo @($tongDaThu) | Out-Null
Write-Host ("         SQL doc lap: {0} ky cuoc" -f $soKy)
Kiem-Excel $s "/bao-cao/doanh-thu-ky/xuat-excel" "Doanh thu theo ky"

# ---------------------------------------------------------------------
Muc "4. C.2 - Doanh thu theo goi cuoc"
# ---------------------------------------------------------------------
$kyId = Sql-Value "SELECT kc.id FROM ky_cuoc kc WHERE (SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=kc.id)>0 ORDER BY kc.nam DESC, kc.thang DESC LIMIT 1;"
$r = Get-Trang $s "/bao-cao/doanh-thu-goi-cuoc?kyCuocId=$kyId"
$tongGoi = Tien "SELECT FORMAT(SUM(tong_thanh_toan),0) FROM hoa_don WHERE ky_cuoc_id=$kyId;"
$soGoi   = Sql-Value "SELECT COUNT(DISTINCT tb.goi_cuoc_id) FROM hoa_don hd JOIN thue_bao tb ON tb.id=hd.thue_bao_id WHERE hd.ky_cuoc_id=$kyId;"
Kiem-Tra -Ten "Trang mo duoc, co bieu do tron" -KetQua $r -CanCo @("bieuDoGoi") | Out-Null
Kiem-Tra -Ten "[Kiem cheo 6] Tong theo goi = tong cua ky = $tongGoi d" -KetQua $r `
    -CanCo @($tongGoi) | Out-Null
Write-Host ("         SQL doc lap: {0} goi cuoc co hoa don trong ky" -f $soGoi)
Kiem-Excel $s "/bao-cao/doanh-thu-goi-cuoc/xuat-excel?kyCuocId=$kyId" "Doanh thu theo goi cuoc"

# ---------------------------------------------------------------------
Muc "5. C.3 - Doanh thu theo loai dich vu"
# ---------------------------------------------------------------------
$r = Get-Trang $s "/bao-cao/doanh-thu-dich-vu?kyCuocId=$kyId"
$cuocThoai = Tien "SELECT FORMAT(SUM(cuoc_thoai),0) FROM hoa_don WHERE ky_cuoc_id=$kyId;"
$vat       = Tien "SELECT FORMAT(SUM(thue_vat),0) FROM hoa_don WHERE ky_cuoc_id=$kyId;"
Kiem-Tra -Ten "Trang mo duoc, co bieu do tron" -KetQua $r -CanCo @("bieuDoDichVu") | Out-Null
Kiem-Tra -Ten "[Kiem cheo 7] Cuoc thoai = $cuocThoai d" -KetQua $r -CanCo @($cuocThoai) | Out-Null
Kiem-Tra -Ten "[Kiem cheo 8] Thue VAT = $vat d" -KetQua $r -CanCo @($vat) | Out-Null
Kiem-Excel $s "/bao-cao/doanh-thu-dich-vu/xuat-excel?kyCuocId=$kyId" "Doanh thu theo dich vu"

# ---------------------------------------------------------------------
Muc "6. C.4 - Thong ke thue bao"
# ---------------------------------------------------------------------
$r = Get-Trang $s "/bao-cao/thue-bao"
$daThanhLy = Sql-Value "SELECT COUNT(*) FROM thue_bao WHERE trang_thai='DA_THANH_LY';"
Kiem-Tra -Ten "Trang mo duoc, co du 3 bieu do" -KetQua $r `
    -CanCo @("bieuDoTrangThai", "bieuDoLoai", "bieuDoBienDong") | Out-Null
Kiem-Tra -Ten "[Kiem cheo 9] So thue bao da thanh ly = $daThanhLy" -KetQua $r `
    -CanCo @($daThanhLy) | Out-Null
Kiem-Excel $s "/bao-cao/thue-bao/xuat-excel" "Thong ke thue bao"

# ---------------------------------------------------------------------
Muc "7. C.5 - Top thue bao cuoc cao"
# ---------------------------------------------------------------------
$r = Get-Trang $s "/bao-cao/top-thue-bao?kyCuocId=$kyId&soLuong=10"
$cuocCaoNhat = Tien "SELECT FORMAT(MAX(tong_thanh_toan),0) FROM hoa_don WHERE ky_cuoc_id=$kyId;"
$tbCaoNhat   = Sql-Value "SELECT tb.so_thue_bao FROM hoa_don hd JOIN thue_bao tb ON tb.id=hd.thue_bao_id WHERE hd.ky_cuoc_id=$kyId ORDER BY hd.tong_thanh_toan DESC LIMIT 1;"
Kiem-Tra -Ten "[Kiem cheo 10] Thue bao dung dau = $tbCaoNhat, cuoc $cuocCaoNhat d" -KetQua $r `
    -CanCo @($tbCaoNhat, $cuocCaoNhat) | Out-Null
$r50 = Get-Trang $s "/bao-cao/top-thue-bao?kyCuocId=$kyId&soLuong=50"
Kiem-Tra -Ten "Doi so luong sang 50 van chay" -KetQua $r50 | Out-Null
Kiem-Excel $s "/bao-cao/top-thue-bao/xuat-excel?kyCuocId=$kyId&soLuong=10" "Top thue bao"

# ---------------------------------------------------------------------
Muc "8. C.6 - San luong dich vu"
# ---------------------------------------------------------------------
$r = Get-Trang $s "/bao-cao/san-luong?kyCuocId=$kyId"
# Man hinh hien so co phan cach nghin, nen phai so voi chuoi DA DINH DANG
$soCuocGoi = Tien "SELECT FORMAT(COUNT(*),0) FROM chi_tiet_su_dung WHERE ky_cuoc_id=$kyId AND loai_dich_vu='THOAI';"
$soSms     = Tien "SELECT FORMAT(COALESCE(SUM(so_luong),0),0) FROM chi_tiet_su_dung WHERE ky_cuoc_id=$kyId AND loai_dich_vu='SMS';"
Kiem-Tra -Ten "[Kiem cheo 11] So cuoc goi = $soCuocGoi" -KetQua $r -CanCo @($soCuocGoi) | Out-Null
Kiem-Tra -Ten "[Kiem cheo 12] So tin SMS = $soSms" -KetQua $r -CanCo @($soSms) | Out-Null
Kiem-Excel $s "/bao-cao/san-luong/xuat-excel?kyCuocId=$kyId" "San luong dich vu"

# ---------------------------------------------------------------------
Muc "9. C.7 - Cong no tong hop"
# ---------------------------------------------------------------------
$r = Get-Trang $s "/bao-cao/cong-no"
Kiem-Tra -Ten "Trang mo duoc, co bieu do tuoi no" -KetQua $r -CanCo @("bieuDoTuoiNo") | Out-Null
Kiem-Tra -Ten "[Kiem cheo 13] Tong cong no = $congNo d" -KetQua $r -CanCo @($congNo) | Out-Null

# Ca NAM nhom tuoi no phai co noi dung - tieu chi nghiem thu so 2
$nhom = @(@{Ma="Trong han"; Tu=-99999; Den=0}, @{Ma="1-30"; Tu=1; Den=30},
          @{Ma="31-60"; Tu=31; Den=60}, @{Ma="61-90"; Tu=61; Den=90},
          @{Ma="tren 90"; Tu=91; Den=99999})
$duNhom = $true
foreach ($n in $nhom) {
    $dk = "con_no > 0 AND DATEDIFF(CURDATE(), han_thanh_toan) BETWEEN $($n.Tu) AND $($n.Den)"
    $so   = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE $dk;")
    $tien = Tien "SELECT FORMAT(SUM(con_no),0) FROM hoa_don WHERE $dk;"
    if ($so -eq 0) { $duNhom = $false }
    $co = $r.body.Contains($tien)
    Write-Host ("         Nhom {0,-10} : {1,3} hoa don / {2,14} d  {3}" -f `
        $n.Ma, $so, $tien, $(if ($co) { "hien tren man hinh" } else { "KHONG THAY" }))
}
Xac-Nhan "[Tieu chi 2] Ca 5 nhom tuoi no deu co noi dung" $duNhom ""

$topKhach = Sql-Value "SELECT kh.ten_kh FROM hoa_don hd JOIN khach_hang kh ON kh.id=hd.khach_hang_id WHERE hd.con_no>0 GROUP BY kh.id, kh.ten_kh ORDER BY SUM(hd.con_no) DESC LIMIT 1;"
Write-Host ("         Khach no nhieu nhat theo SQL: {0}" -f $topKhach)
Kiem-Excel $s "/bao-cao/cong-no/xuat-excel" "Cong no tong hop"

# ---------------------------------------------------------------------
Muc "10. Bao cao chay duoc voi ky KHONG co du lieu"
# ---------------------------------------------------------------------
# Tao mot ky trong de kiem tieu chi nghiem thu so 7. Ky nay khong lap hoa
# don nen moi bao cao phai hien "khong co du lieu" thay vi bao loi 500.
Post-Form $s "/ky-cuoc" "/ky-cuoc/tao-moi" @{ thang = 12; nam = 2026 } | Out-Null
$kyTrong = Sql-Value "SELECT id FROM ky_cuoc WHERE thang=12 AND nam=2026;"
foreach ($bc in @("doanh-thu-goi-cuoc", "doanh-thu-dich-vu", "top-thue-bao", "san-luong")) {
    $rt = Get-Trang $s "/bao-cao/$bc`?kyCuocId=$kyTrong"
    Kiem-Tra -Ten "Ky trong 12/2026: /bao-cao/$bc khong loi 500" -KetQua $rt | Out-Null
    Kiem-Excel $s "/bao-cao/$bc/xuat-excel?kyCuocId=$kyTrong" "$bc (ky trong)"
}
# Don dep: xoa ky vua tao de khong lam ban bo du lieu mau
& $mysql -u root -D vnpt_billing -e "DELETE FROM ky_cuoc WHERE thang=12 AND nam=2026;" | Out-Null
$conLai = [int](Sql-Value "SELECT COUNT(*) FROM ky_cuoc WHERE thang=12 AND nam=2026;")
Xac-Nhan "Da don ky thu nghiem 12/2026" ($conLai -eq 0) "con lai $conLai dong"

Ket-Thuc
