# =====================================================================
# chay-ky-moi-phase6.ps1 - Phase 6 muc A2
# =====================================================================
# Chay TRON quy trinh cho ba ky moi QUA GIAO DIEN, khong UPDATE tay:
#     tao ky -> sinh CDR (hat giong co dinh) -> tinh cuoc -> lap hoa don
#
# Ky 3 va 4/2026 duoc CHOT; ky 7/2026 giu MO de con demo duoc vong
# huy -> lap lai (cung ly do ky 6 duoc giu sach giao dich).
#
# KHONG chay tru cuoc tra truoc cho cac ky nay - xem PHASE-6-REPORT muc
# ve tinh khong giao hoan cua tru cuoc.
#
# HAT GIONG ghi ngay trong script: day la thu duy nhat dung lai duoc
# dung bo CDR nay neu mat du lieu.
#
# Chay khi ung dung DANG BAT:  .\scripts\chay-ky-moi-phase6.ps1
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

$mysql = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
$env:MYSQL_PWD = $env:MYSQL_PASSWORD

function Sql-Value($cau) {
    return (& $mysql -u root -D vnpt_billing --default-character-set=utf8mb4 -N -B -e $cau) `
        | Select-Object -First 1
}

function Xac-Nhan($ten, $dieuKien, $chiTiet) {
    if ($dieuKien) {
        Write-Host ("  [DAT ] {0}" -f $ten)
        $Global:SoDat++
    } else {
        Write-Host ("  [SAI ] {0}" -f $ten)
        $Global:SoSai++
    }
    if ($chiTiet) { Write-Host ("         {0}" -f $chiTiet) }
}

# ---------------------------------------------------------------------
# Ba ky can dung. soLuong cua ky 3 va 4 lay theo dac ta; ky 7 chon 4000
# cho nam giua ky 5 (3.697) va ky 6 (5.017).
# ---------------------------------------------------------------------
$cacKy = @(
    @{ Thang = 3; Nam = 2026; TuNgay = "2026-03-01"; DenNgay = "2026-03-31"; SoLuong = 3000; HatGiong = 20260300; Chot = $true  },
    @{ Thang = 4; Nam = 2026; TuNgay = "2026-04-01"; DenNgay = "2026-04-30"; SoLuong = 3500; HatGiong = 20260400; Chot = $true  },
    @{ Thang = 7; Nam = 2026; TuNgay = "2026-07-01"; DenNgay = "2026-07-31"; SoLuong = 4000; HatGiong = 20260700; Chot = $false }
)

Bat-Dau "PHASE 6 MUC A2 - DUNG KY 3, 4 VA 7/2026"

$s = Connect-App "admin" "123456"

foreach ($ky in $cacKy) {
    $ten = "{0}/{1}" -f $ky.Thang, $ky.Nam
    Muc ("Ky " + $ten)

    # --- 1. Tao ky (bo qua neu da co, nhu ky 7) ---
    $kyId = Sql-Value "SELECT id FROM ky_cuoc WHERE thang=$($ky.Thang) AND nam=$($ky.Nam);"
    if (-not $kyId) {
        Post-Form $s "/ky-cuoc" "/ky-cuoc/tao-moi" @{ thang = $ky.Thang; nam = $ky.Nam } | Out-Null
        $kyId = Sql-Value "SELECT id FROM ky_cuoc WHERE thang=$($ky.Thang) AND nam=$($ky.Nam);"
        Xac-Nhan "Tao ky $ten" ($null -ne $kyId) "ky_cuoc_id = $kyId"
    } else {
        Write-Host "  [ -- ] Ky $ten da ton tai, ky_cuoc_id = $kyId"
    }

    # --- 2. Sinh CDR voi hat giong co dinh ---
    $truoc = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung;")
    $kq = Post-Form $s "/cdr/sinh-du-lieu" "/cdr/sinh-du-lieu" @{
        tuNgay = $ky.TuNgay; denNgay = $ky.DenNgay; soLuong = $ky.SoLuong
        phamVi = "TAT_CA"; hatGiong = $ky.HatGiong
    }
    $sau = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung;")
    Xac-Nhan "Sinh CDR ky $ten (hat giong $($ky.HatGiong))" `
        ($kq.status -eq 200 -and ($sau - $truoc) -gt 0) `
        ("HTTP {0}, them {1} ban ghi" -f $kq.status, ($sau - $truoc))

    # --- 3. Tinh cuoc ---
    $kq = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/tinh-cuoc" @{}
    $chuaTinh = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung WHERE ky_cuoc_id=$kyId AND trang_thai_tinh_cuoc <> 'DA_TINH';")
    $daTinh   = [int](Sql-Value "SELECT COUNT(*) FROM chi_tiet_su_dung WHERE ky_cuoc_id=$kyId AND trang_thai_tinh_cuoc='DA_TINH';")
    Xac-Nhan "Tinh cuoc ky $ten" ($kq.status -eq 200 -and $chuaTinh -eq 0 -and $daTinh -gt 0) `
        ("HTTP {0}, {1} ban ghi DA_TINH, {2} ban ghi chua tinh hoac loi" -f $kq.status, $daTinh, $chuaTinh)

    # --- 4. Lap hoa don ---
    $kq = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/lap-hoa-don" @{}
    $soHd     = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don WHERE ky_cuoc_id=$kyId;")
    $doanhThu = Sql-Value "SELECT COALESCE(SUM(tong_thanh_toan),0) FROM hoa_don WHERE ky_cuoc_id=$kyId;"
    $han      = Sql-Value "SELECT DISTINCT han_thanh_toan FROM hoa_don WHERE ky_cuoc_id=$kyId;"
    Xac-Nhan "Lap hoa don ky $ten" ($kq.status -eq 200 -and $soHd -gt 0) `
        ("HTTP {0}, {1} hoa don, doanh thu {2} d, han thanh toan {3}" -f $kq.status, $soHd, $doanhThu, $han)

    # --- 5. Chot ky (chi ky 3 va 4) ---
    if ($ky.Chot) {
        $kq = Post-Form $s "/tinh-cuoc" "/tinh-cuoc/$kyId/chot-ky" @{}
        $tt = Sql-Value "SELECT trang_thai FROM ky_cuoc WHERE id=$kyId;"
        Xac-Nhan "Chot ky $ten" ($kq.status -eq 200 -and $tt -eq "DA_CHOT") "trang_thai = $tt"
    } else {
        $tt = Sql-Value "SELECT trang_thai FROM ky_cuoc WHERE id=$kyId;"
        Xac-Nhan "Ky $ten GIU trang thai MO de con demo huy/lap lai" ($tt -eq "MO") "trang_thai = $tt"
    }
}

# ---------------------------------------------------------------------
Muc "Tong hop sau khi dung ba ky"
# ---------------------------------------------------------------------
& $mysql -u root -D vnpt_billing --default-character-set=utf8mb4 -e @"
SELECT kc.thang, kc.nam, kc.trang_thai, kc.so_cdr_xu_ly, kc.so_hoa_don_tao,
       kc.tong_doanh_thu, MIN(hd.han_thanh_toan) AS han,
       DATEDIFF(CURDATE(), MIN(hd.han_thanh_toan)) AS so_ngay_qua_han
  FROM ky_cuoc kc LEFT JOIN hoa_don hd ON hd.ky_cuoc_id = kc.id
 GROUP BY kc.id, kc.thang, kc.nam, kc.trang_thai, kc.so_cdr_xu_ly,
          kc.so_hoa_don_tao, kc.tong_doanh_thu
 ORDER BY kc.nam, kc.thang;
"@

$lech = [int](Sql-Value @"
SELECT COUNT(*) FROM (
  SELECT h.id FROM hoa_don h LEFT JOIN thanh_toan t ON t.hoa_don_id = h.id
   GROUP BY h.id, h.con_no, h.tong_thanh_toan, h.da_thanh_toan
  HAVING h.con_no <> h.tong_thanh_toan - h.da_thanh_toan
      OR h.da_thanh_toan <> COALESCE(SUM(t.so_tien), 0)) x;
"@)
$soHd = [int](Sql-Value "SELECT COUNT(*) FROM hoa_don;")
Xac-Nhan "Bat bien thanh toan van sach" ($lech -eq 0) `
    ("{0} dong lech tren {1} hoa don" -f $lech, $soHd)

$gdKy6 = [int](Sql-Value "SELECT COUNT(*) FROM thanh_toan tt JOIN hoa_don hd ON hd.id=tt.hoa_don_id JOIN ky_cuoc kc ON kc.id=hd.ky_cuoc_id WHERE kc.thang=6;")
Xac-Nhan "Ky 6 van 0 giao dich thanh toan" ($gdKy6 -eq 0) "SQL doc lap: $gdKy6 giao dich"

Ket-Thuc
