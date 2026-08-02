# =====================================================================
# test-auth.ps1 - Kiem thu xac thuc va phan quyen (Phase 2 muc B)
# =====================================================================
# Cach chay:  powershell -ExecutionPolicy Bypass -File scripts\test-auth.ps1
# Yeu cau   :  ung dung dang chay tai http://localhost:8080
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

Bat-Dau "KIEM THU XAC THUC VA PHAN QUYEN"

Muc "1. Trang dang nhap cong khai"
$khach = $null
Invoke-WebRequest -Uri "$Global:BaseUrl/dang-nhap" -SessionVariable khach -UseBasicParsing -TimeoutSec 10 | Out-Null
$t = Get-Trang $khach "/dang-nhap"
Kiem-Tra -Ten "Mo /dang-nhap khi chua dang nhap" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="_csrf"', 'nhanvien01', 'ketoan01', 'demo') `
    -KhongDuocCo @('sidebar-brand') | Out-Null

Muc "2. Chua dang nhap thi khong vao duoc trang chu"
$t = Get-Trang $khach "/"
Kiem-Tra -Ten "Mo / khi chua dang nhap thi bi dua ve form dang nhap" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') | Out-Null

Muc "3. Dang nhap sai mat khau bi tu choi"
$sSai = Connect-App "admin" "sai-mat-khau"
$t = Get-Trang $sSai "/"
Kiem-Tra -Ten "admin voi mat khau sai van chi thay form dang nhap" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') | Out-Null

Muc "4. Dang nhap dung va phan quyen menu sidebar"
$phien = @{ }
$mongDoi = @{
    "admin"      = @{ co = @('Khách hàng', 'Hóa đơn', 'Gói cước', 'Báo cáo'); khong = @() }
    "nhanvien01" = @{ co = @('Khách hàng', 'Báo cáo'); khong = @('Hóa đơn', 'Gói cước') }
    "ketoan01"   = @{ co = @('Hóa đơn', 'Báo cáo'); khong = @('Khách hàng', 'Gói cước') }
}
foreach ($tk in @("admin", "nhanvien01", "ketoan01")) {
    $s = Connect-App $tk "123456"
    $phien[$tk] = $s
    $t = Get-Trang $s "/"
    Kiem-Tra -Ten "$tk dang nhap duoc va sidebar dung theo vai tro" -KetQua $t -StatusMongDoi 200 `
        -CanCo ($mongDoi[$tk].co + @('Đăng xuất')) -KhongDuocCo $mongDoi[$tk].khong | Out-Null
}

Muc "5. Go thang URL khong co quyen thi tra 403"
$t = Get-Trang $phien["ketoan01"] "/khach-hang"
Kiem-Tra -Ten "ketoan01 mo /khach-hang" -KetQua $t -StatusMongDoi 403 `
    -CanCo @('Không đủ quyền truy cập', 'sidebar-brand') | Out-Null

$t = Get-Trang $phien["nhanvien01"] "/hoa-don"
Kiem-Tra -Ten "nhanvien01 mo /hoa-don" -KetQua $t -StatusMongDoi 403 | Out-Null

$t = Get-Trang $phien["nhanvien01"] "/quan-tri"
Kiem-Tra -Ten "nhanvien01 mo /quan-tri" -KetQua $t -StatusMongDoi 403 | Out-Null

Muc "6. Header hien ten nguoi dung va vai tro"
$t = Get-Trang $phien["ketoan01"] "/"
Kiem-Tra -Ten "Header hien ho ten va nhan vai tro cua ketoan01" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Trần Thị Kế Toán', 'Kế toán', 'Đăng xuất') | Out-Null

Muc "7. Dang xuat huy phien"
$t = Get-Trang $phien["admin"] "/"
$csrf = Get-Csrf $t.body
try {
    Invoke-WebRequest -Uri "$Global:BaseUrl/dang-xuat" -Method Post -Body @{ _csrf = $csrf } `
        -WebSession $phien["admin"] -UseBasicParsing -TimeoutSec 10 | Out-Null
} catch { }
$t = Get-Trang $phien["admin"] "/"
Kiem-Tra -Ten "Sau dang xuat thi quay ve form dang nhap" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') | Out-Null

Ket-Thuc
