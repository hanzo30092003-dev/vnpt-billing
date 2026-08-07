# =====================================================================
# test-tb.ps1 - Kiem thu quan ly thue bao (Phase 2 muc D)
# =====================================================================
# Cach chay:  powershell -ExecutionPolicy Bypass -File scripts\test-tb.ps1
# Yeu cau   :  ung dung dang chay, CSDL o trang thai du lieu mau
#              (chay profile reset truoc de ket qua on dinh)
#
# LUU Y: script nay CO GHI DU LIEU (dang ky thue bao moi, doi trang thai,
# nap tien). Chay xong nen reset lai neu can bo du lieu sach de demo.
# =====================================================================
. "$PSScriptRoot\_chung.ps1"

Bat-Dau "KIEM THU QUAN LY THUE BAO"

$s = Connect-App "nhanvien01" "123456"
$formDangKy = "/thue-bao/dang-ky"
$postLuu = "/thue-bao/luu"

Muc "1. Danh sach va badge mau theo trang thai"
$t = Get-Trang $s "/thue-bao"
Kiem-Tra -Ten "Mo danh sach thue bao, du 4 mau badge" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Quản lý thuê bao', 'bg-success', 'bg-warning', 'bg-orange', 'bg-secondary') | Out-Null

Muc "2. Loc theo trang thai"
foreach ($tt in @("HOAT_DONG", "TAM_NGUNG_1C", "TAM_NGUNG_2C", "DA_THANH_LY")) {
    $t = Get-Trang $s "/thue-bao?trangThai=$tt"
    Kiem-Tra -Ten "Loc trang thai $tt" -KetQua $t -StatusMongDoi 200 -CanCo @('Tìm thấy') | Out-Null
}

Muc "3. Form dang ky mo duoc, co JS loc goi theo loai"
$t = Get-Trang $s $formDangKy
Kiem-Tra -Ten "Mo form dang ky thue bao" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('id="soThueBao"', 'data-loai', 'o-tra-sau') | Out-Null

Muc "4. Validation khi dang ky"
$kq = Post-Form $s $formDangKy $postLuu @{
    khachHangId = "1"; soThueBao = "0901234501"; loaiThueBao = "TRA_SAU"; goiCuocId = "1"
    ngayKichHoat = "2026-07-31"; hanMucTinDung = "500000"
}
Kiem-Tra -Ten "So thue bao trung bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('đã tồn tại trong hệ thống') | Out-Null

$kq = Post-Form $s $formDangKy $postLuu @{
    khachHangId = "1"; soThueBao = "0123456789"; loaiThueBao = "TRA_SAU"; goiCuocId = "1"
    ngayKichHoat = "2026-07-31"; hanMucTinDung = "0"
}
Kiem-Tra -Ten "Dau so khong hop le bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('bắt đầu bằng 03, 05, 07, 08 hoặc 09') | Out-Null

$kq = Post-Form $s $formDangKy $postLuu @{
    khachHangId = "1"; soThueBao = "0355500001"; loaiThueBao = "TRA_SAU"; goiCuocId = "1"
    ngayKichHoat = "2027-01-01"; hanMucTinDung = "0"
}
Kiem-Tra -Ten "Ngay kich hoat o tuong lai bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('không được ở tương lai') | Out-Null

$kq = Post-Form $s $formDangKy $postLuu @{
    khachHangId = "1"; soThueBao = "0355500002"; loaiThueBao = "TRA_SAU"; goiCuocId = "5"
    ngayKichHoat = "2026-07-31"; hanMucTinDung = "0"
}
Kiem-Tra -Ten "Goi cuoc khac loai thue bao bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('chỉ áp dụng cho thuê bao') | Out-Null

Muc "5. Chi tiet thue bao va 4 tab"
$t = Get-Trang $s "/thue-bao/1"
# Tab thu 4 doi ten o Phase 5 muc G4: "Lich su nap tien" -> "Bien dong so du",
# vi tu muc G1 no hien ca chieu nap lan chieu tru cuoc. Hai phep kiem duoi day
# van do TEN CU nen TRUOT tu luc do - va khong ai thay, vi Kiem-Tra bao ket qua
# bang Write-Output roi bi "| Out-Null" nuot mat. Da sua ca hai o Phase 5 muc F.
Kiem-Tra -Ten "Thue bao tra truoc co du 4 tab, gom tab bien dong so du" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Thông tin chung', 'Lịch sử gói cước', 'Lịch sử biến động', 'Biến động số dư') | Out-Null

$t = Get-Trang $s "/thue-bao/21"
Kiem-Tra -Ten "Thue bao tra sau KHONG co tab bien dong so du" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Thông tin chung') -KhongDuocCo @('Biến động số dư') | Out-Null

Muc "6. Lich su bien dong cua du lieu mau khong rong"
$t = Get-Trang $s "/thue-bao/5"
Kiem-Tra -Ten "Thue bao tam ngung 1 chieu co lich su" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('timeline-item') -KhongDuocCo @('Chưa có biến động trạng thái nào') | Out-Null

$t = Get-Trang $s "/thue-bao/8"
$soBuoc = ([regex]::Matches($t.body, 'class="timeline-item"')).Count
if ($t.status -eq 200 -and $soBuoc -eq 2) { Write-Output "  [DAT ] Thue bao 8 co lich su nhieu buoc (2 dong)"; $Global:SoDat++ }
else { Write-Output "  [SAI ] Thue bao 8: HTTP $($t.status), $soBuoc dong (mong doi 2)"; $Global:SoSai++ }

Muc "7. Chan khoi phuc thue bao da thanh ly"
$kq = Post-Form $s "/thue-bao/15" "/thue-bao/15/chuyen-trang-thai" @{
    trangThaiMoi = "HOAT_DONG"; lyDo = "Thử khôi phục thuê bao đã thanh lý"
}
Kiem-Tra -Ten "DA_THANH_LY -> HOAT_DONG bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('đã thanh lý, không thể chuyển sang trạng thái khác') | Out-Null

Muc "8. Nap tien"
$kq = Post-Form $s "/thue-bao/21" "/thue-bao/21/nap-tien" @{ soTien = "50000"; hinhThuc = "THE_CAO" }
Kiem-Tra -Ten "Nap tien cho thue bao TRA_SAU bi chan" -KetQua $kq -StatusMongDoi 200 `
    -CanCo @('Chỉ nạp tiền được cho thuê bao trả trước') | Out-Null

Ket-Thuc
