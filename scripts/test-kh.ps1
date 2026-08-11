# =====================================================================
# test-kh.ps1 - Kiem thu quan ly khach hang (Phase 2 muc C)
# =====================================================================
# Cach chay:  powershell -ExecutionPolicy Bypass -File scripts\test-kh.ps1
# Yeu cau   :  ung dung dang chay, CSDL o trang thai du lieu mau
#              (chay profile reset truoc de ket qua on dinh)
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

Bat-Dau "KIEM THU QUAN LY KHACH HANG"

$s = Connect-App "nhanvien01" "123456"
$formThem = "/khach-hang/them"
$postLuu = "/khach-hang/luu"

Muc "1. Danh sach, phan trang 15 dong"
$t = Get-Trang $s "/khach-hang"
Kiem-Tra -Ten "Mo danh sach khach hang" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Quản lý khách hàng', 'pagination') | Out-Null
$soDong = ([regex]::Matches($t.body, '<code>KH\d+</code>')).Count
if ($soDong -eq 15) { Write-Output "  [DAT ] Hien dung 15 dong moi trang"; $Global:SoDat++ }
else { Write-Output "  [SAI ] Hien $soDong dong, mong doi 15"; $Global:SoSai++ }

Muc "2. Tim kiem va loc"
$t = Get-Trang $s "/khach-hang?loaiKh=DOANH_NGHIEP"
Kiem-Tra -Ten "Loc DOANH_NGHIEP" -KetQua $t -StatusMongDoi 200 -CanCo @('Doanh nghiệp') | Out-Null
$t = Get-Trang $s "/khach-hang?loaiKh=CA_NHAN"
Kiem-Tra -Ten "Loc CA_NHAN" -KetQua $t -StatusMongDoi 200 -CanCo @('Cá nhân') | Out-Null

Muc "3. Giu bo loc khi chuyen trang"
$t = Get-Trang $s "/khach-hang?loaiKh=CA_NHAN&trang=1"
Kiem-Tra -Ten "Trang 2 van giu bo loc trong lien ket phan trang" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('loaiKh=CA_NHAN') | Out-Null

Muc "4. Form them mo duoc va co JS doi dong"
$t = Get-Trang $s $formThem
Kiem-Tra -Ten "Mo form them khach hang" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('id="soGiayTo"', 'nhan-giay-to', 'o-doanh-nghiep') | Out-Null

Muc "5. Validation - CCCD 11 so bi chan"
$kq = Post-Form $s $formThem $postLuu @{
    loaiKh = "CA_NHAN"; tenKh = "Nguyen Van Test"; soGiayTo = "12345678901"
    diaChi = "Dia chi test"; ngayDangKy = "2026-07-31"
}
Kiem-Tra -Ten "CCCD 11 so bi chan, ve lai form kem thong bao" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('Số CCCD phải gồm đúng 12 chữ số') | Out-Null

Muc "6. Validation - MST sai dinh dang bi chan"
$kq = Post-Form $s $formThem $postLuu @{
    loaiKh = "DOANH_NGHIEP"; tenKh = "Cong ty Test"; soGiayTo = "12345"
    diaChi = "Dia chi test"; ngayDangKy = "2026-07-31"
}
Kiem-Tra -Ten "MST 5 so bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('Mã số thuế phải gồm 10 hoặc 13 chữ số') | Out-Null

Muc "7. Validation - bo trong truong bat buoc"
$kq = Post-Form $s $formThem $postLuu @{
    loaiKh = "CA_NHAN"; tenKh = ""; soGiayTo = ""; diaChi = ""; ngayDangKy = "2026-07-31"
}
Kiem-Tra -Ten "Bo trong ten / giay to / dia chi deu bi bao loi" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('Vui lòng nhập tên khách hàng', 'Vui lòng nhập số giấy tờ', 'Vui lòng nhập địa chỉ') | Out-Null

# ---------------------------------------------------------------------
# Tu dot lam lai giao dien: KHONG do theo nguyen van cau thong bao nua.
#
# Cau chu la thu ĐƯỢC PHÉP đổi - viec 3 cua dot nay viet lai gan het thong
# bao loi cho de hieu, va bon phep kiem o day do het chi vi chu doi, trong
# khi nghiep vu khong he sai. Mot phep kiem do vi ly do sai cung nguy hiem
# nhu phep kiem khong do khi co loi that.
#
# Nay do ba thu ON DINH hon va manh hon:
#   1. ma trang thai HTTP
#   2. khoi bao loi co hien ra khong  -> lop CSS "alert-danger", markup
#   3. THAO TAC CO BI CHAN THAT KHONG -> dem lai ban ghi trong CSDL
# Diem 3 moi la thu can kiem: truoc day script chi chung minh "co chu do
# tren man hinh", chu khong chung minh la khach hang KHONG bi tao ra.
# ---------------------------------------------------------------------
$mysqlKh = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
$env:MYSQL_PWD = $env:MYSQL_PASSWORD
function Dem-KhachHang {
    return [int](& $mysqlKh -u root -D vnpt_billing -N -B -e "SELECT COUNT(*) FROM khach_hang;" |
                 Select-Object -First 1)
}

Muc "8. Validation - trung so giay to"
$truoc = Dem-KhachHang
$kq = Post-Form $s $formThem $postLuu @{
    loaiKh = "CA_NHAN"; tenKh = "Trung giay to"; soGiayTo = "092301004517"
    diaChi = "Dia chi test"; ngayDangKy = "2026-07-31"
}
$sau = Dem-KhachHang
Kiem-Tra -Ten "So giay to trung khach khac bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('alert-danger') | Out-Null
Xac-Nhan "Khong tao ra khach hang nao khi so giay to trung" ($truoc -eq $sau) `
    ("khach_hang: {0} -> {1}" -f $truoc, $sau)

Muc "9. Chi tiet khach hang doanh nghiep"
$t = Get-Trang $s "/khach-hang/36"
Kiem-Tra -Ten "Chi tiet hien bang thue bao va nut dang ky moi" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Thuê bao đang sở hữu', 'Đăng ký thuê bao mới', 'Doanh nghiệp 500') | Out-Null

Muc "10. Ngung giao dich bi chan khi con thue bao hoat dong"
$ttTruoc = (& $mysqlKh -u root -D vnpt_billing -N -B `
            -e "SELECT trang_thai FROM khach_hang WHERE id=36;" | Select-Object -First 1).Trim()
$kq = Post-Form $s "/khach-hang/36" "/khach-hang/36/ngung-giao-dich" @{ }
$ttSau = (& $mysqlKh -u root -D vnpt_billing -N -B `
          -e "SELECT trang_thai FROM khach_hang WHERE id=36;" | Select-Object -First 1).Trim()
Kiem-Tra -Ten "Chan ngung giao dich khi con thue bao hoat dong" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('alert-danger') | Out-Null
Xac-Nhan "Trang thai khach hang 36 KHONG doi sau khi bi chan" `
    ($ttTruoc -eq 'HOAT_DONG' -and $ttSau -eq 'HOAT_DONG') `
    ("trang_thai: {0} -> {1}" -f $ttTruoc, $ttSau)

Ket-Thuc
