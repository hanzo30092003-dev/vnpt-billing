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
# Ba chuoi 'nhanvien01', 'ketoan01', 'demo' da BO khoi phep kiem: bang tai
# khoan dung thu tung in san tren trang dang nhap nay da go di - mot man hinh
# dang nhap cong khai ten dang nhap va mat khau la thu khong ton tai trong
# phan mem that. Nay do cai dang co that o do: form dang nhap, o mat khau, va
# KHONG duoc lo sidebar (tuc chua dang nhap thi khong thay gi ben trong).
Kiem-Tra -Ten "Mo /dang-nhap khi chua dang nhap" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="_csrf"', 'name="tenDangNhap"', 'name="matKhau"') `
    -KhongDuocCo @('sidebar-brand') | Out-Null

# Doi chung cho viec bo bang tai khoan: trang dang nhap KHONG duoc lo ten
# tai khoan hay mat khau nao nua.
Kiem-Tra -Ten "Trang dang nhap khong con lo tai khoan/mat khau" -KetQua $t -StatusMongDoi 200 `
    -KhongDuocCo @('nhanvien01', 'ketoan01', '123456') | Out-Null

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
# SUA O PHASE 6: truoc day phep kiem nay do CHU trong sidebar ('Hoa don',
# 'Khach hang'). No dung tinh co, vi trang chu cu la trang gioi thieu tam
# khong chua nhung chu do. Tu khi trang chu thanh dashboard, cac chu ay xuat
# hien hop le trong THAN TRANG - cot bang "Khach hang", dong "... hoa don" -
# va phep kiem bao dong gia.
#
# Nay do DUONG DAN cua lien ket sidebar. Dau nhay dong sau ten la co y:
# 'href="/hoa-don"' KHONG khop voi 'href="/hoa-don/307"' cua bang top thue
# bao, nen phep kiem chi noi ve muc menu chu khong dinh vao du lieu.
$phien = @{ }
$mongDoi = @{
    "admin"      = @{ co = @('href="/khach-hang"', 'href="/hoa-don"', 'href="/goi-cuoc"',
                             'href="/bao-cao"'); khong = @() }
    "nhanvien01" = @{ co = @('href="/khach-hang"', 'href="/bao-cao"')
                      khong = @('href="/hoa-don"', 'href="/goi-cuoc"') }
    "ketoan01"   = @{ co = @('href="/hoa-don"', 'href="/bao-cao"')
                      khong = @('href="/khach-hang"', 'href="/goi-cuoc"') }
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

# ---------------------------------------------------------------------
Muc "8. Bao cao chia theo NOI DUNG, khong mo ca cum"
# ---------------------------------------------------------------------
# Truoc dot bao mat, ca cum /bao-cao/** o muc authenticated(). He qua khong ai
# luong: nhanvien01 bi 403 o /cong-no va /hoa-don, nhung mo /bao-cao/cong-no
# thi thay du ten khach hang va so tien no, lai con tai duoc file Excel cong no.
# He thong khoa mot cua va de mo cua ngay ben canh.
#
# Nay luat di theo DU LIEU: bao cao nao co so tien cua khach thi cung luat voi
# /hoa-don va /cong-no. Hai bao cao khong co so tien nao - thong ke thue bao va
# san luong - van mo cho moi vai tro.
$nvBc = Connect-App "nhanvien01" "123456"
$ktBc = Connect-App "ketoan01" "123456"

$coTien = @("/bao-cao/cong-no", "/bao-cao/top-thue-bao", "/bao-cao/doanh-thu-ky",
            "/bao-cao/doanh-thu-goi-cuoc", "/bao-cao/doanh-thu-dich-vu",
            "/bao-cao/cong-no/xuat-excel")
$khongTien = @("/bao-cao", "/bao-cao/thue-bao", "/bao-cao/san-luong")

$loT = @($coTien | Where-Object { (Get-Trang $nvBc $_).status -ne 403 })
Xac-Nhan "nhanvien01 KHONG mo duoc bao cao co so tien cua khach" ($loT.Count -eq 0) `
    $(if ($loT) { "van mo duoc: " + ($loT -join ', ') } else { "$($coTien.Count)/$($coTien.Count) duong deu tra 403" })

$chan = @($khongTien | Where-Object { (Get-Trang $nvBc $_).status -ne 200 })
Xac-Nhan "nhanvien01 VAN mo duoc bao cao khong co so tien" ($chan.Count -eq 0) `
    $(if ($chan) { "bi chan nham: " + ($chan -join ', ') } else { "thong ke thue bao va san luong deu 200" })

# Doi chung: neu ke toan cung bi chan thi phep kiem tren xanh vi ly do sai
$ktHong = @(($coTien + $khongTien) | Where-Object { (Get-Trang $ktBc $_).status -ne 200 })
Xac-Nhan "Doi chung: ketoan01 mo duoc TAT CA bao cao" ($ktHong.Count -eq 0) `
    $(if ($ktHong) { "bi chan nham: " + ($ktHong -join ', ') } else { "$(($coTien + $khongTien).Count)/$(($coTien + $khongTien).Count) duong deu 200" })

# Menu khong duoc dan nguoi dung toi mot trang ho khong mo duoc (bai hoc Phase 7)
$menuNv = Get-Trang $nvBc "/bao-cao"
$theHong = @([regex]::Matches($menuNv.body, 'href="(/bao-cao/[^"?]+)"') |
             ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique |
             Where-Object { (Get-Trang $nvBc $_).status -ne 200 })
Xac-Nhan "Menu bao cao cua nhanvien01 khong co the nao dan toi 403" ($theHong.Count -eq 0) `
    $(if ($theHong) { "the hong: " + ($theHong -join ', ') } else { "moi the tren menu deu mo duoc" })

# ---------------------------------------------------------------------
Muc "9. Man hinh quan ly nguoi dung (V3a)"
# ---------------------------------------------------------------------
# Truoc dot nay, them mot nhan vien moi phai CHAY SQL TAY kem mot chuoi hash
# BCrypt sinh san o dau do. Cau hoi "lam sao them nguoi dung" gan nhu chac chan
# duoc hoi khi bao ve, va cau tra loi cu la "mo CSDL len go lenh".
#
# PHEP KIEM DANG GIA NHAT O DAY LA 9.5: khoa mot tai khoan ma KHONG da phien
# dang mo cua ho thi nut "Khoa" chi co tac dung tu lan dang nhap sau - nguoi bi
# khoa van ngoi thao tac tiep toi het ca. Do la khoa tren giay. Vi vay phep kiem
# dung mot phien THAT dang mo, khoa no tu mot phien khac, roi doi chieu.
#
# TINH LAP LAI: muc nay tao tai khoan kiemthu01 va de lai o trang thai DA KHOA.
# Chay lai lan sau, no nhan ra tai khoan da co, kiem luon nhanh "trung ten dang
# nhap bi chan", roi mo khoa - dung - khoa lai. Ket qua giong nhau o moi lan chay.

$TEN_KT = "kiemthu01"
$MK_KT = "matkhau123"

# Lay ra dung mot dong <tr> cua bang nguoi dung, de doc trang thai va ma so
function Lay-Dong-NguoiDung($body, $ten) {
    if ($body -match ($ten + '[\s\S]{0,4000}?</tr>')) { return $Matches[0] }
    return ""
}

$adm = Connect-App "admin" "123456"
$nvQt = Connect-App "nhanvien01" "123456"

$t = Get-Trang $nvQt "/quan-tri/nguoi-dung"
Kiem-Tra -Ten "nhanvien01 mo /quan-tri/nguoi-dung bi tu choi" -KetQua $t -StatusMongDoi 403 | Out-Null

$t = Get-Trang $adm "/quan-tri/nguoi-dung"
Kiem-Tra -Ten "admin mo duoc danh sach nguoi dung" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Quản lý người dùng', 'admin', 'nhanvien01', 'ketoan01') | Out-Null

# --- 9.1 Them tai khoan moi (hoac xac nhan ten trung bi chan) ---
# Hai nhanh duoi day co y cho CUNG MOT SO phep kiem: so phep kiem cua ca bo
# script khong duoc dao dong theo viec CSDL vua reset hay chua - mot con so
# nghiem thu doi theo trang thai la con so khong doi chieu duoc.
$duLieuTao = @{
    tenDangNhap = $TEN_KT
    hoTen       = "Tai khoan kiem thu"
    email       = "kiemthu01@vnptbilling.local"
    vaiTro      = "NHAN_VIEN"
    matKhau     = $MK_KT
}
if ($t.body.Contains($TEN_KT)) {
    Xac-Nhan "Tai khoan $TEN_KT con lai tu lan chay truoc" $true "khong tao lai"
} else {
    $r = Post-Form $adm "/quan-tri/nguoi-dung/them" "/quan-tri/nguoi-dung/luu" $duLieuTao
    Kiem-Tra -Ten "Tao moi tai khoan $TEN_KT" -KetQua $r -StatusMongDoi 200 `
        -CanCo @($TEN_KT, 'Đã tạo tài khoản') -KhongDuocCo @('Đã có tài khoản') | Out-Null
}

# Chot chan trung ten dang nhap: kiem O MOI LAN CHAY, khong phu thuoc nhanh tren
$r = Post-Form $adm "/quan-tri/nguoi-dung/them" "/quan-tri/nguoi-dung/luu" $duLieuTao
Kiem-Tra -Ten "Tao trung ten dang nhap bi chan" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('Đã có tài khoản') | Out-Null

# --- 9.2 Doc ma so cua hai tai khoan can dung ---
$t = Get-Trang $adm "/quan-tri/nguoi-dung"
$idKt = $null
if ($t.body -match ($TEN_KT + '[\s\S]{0,4000}?/quan-tri/nguoi-dung/(\d+)/sua')) { $idKt = $Matches[1] }
$idAdmin = $null
if ($t.body -match ('>admin<[\s\S]{0,4000}?/quan-tri/nguoi-dung/(\d+)/sua')) { $idAdmin = $Matches[1] }
Xac-Nhan "Doc duoc ma so cua $TEN_KT va cua admin tren danh sach" `
    (($null -ne $idKt) -and ($null -ne $idAdmin)) "$TEN_KT = $idKt, admin = $idAdmin"

# --- 9.3 Mo khoa neu lan chay truoc de lai trang thai khoa ---
$dong = Lay-Dong-NguoiDung $t.body $TEN_KT
if ($dong.Contains("Đã khoá")) {
    $r = Post-Form $adm "/quan-tri/nguoi-dung" "/quan-tri/nguoi-dung/$idKt/mo-khoa" @{ }
    Kiem-Tra -Ten "Mo khoa tai khoan $TEN_KT" -KetQua $r -StatusMongDoi 200 `
        -CanCo @('Đã mở khoá tài khoản') | Out-Null
} else {
    Xac-Nhan "Tai khoan $TEN_KT dang mo khoa san" $true "khong can go khoa"
}

# --- 9.4 Mat khau do admin dat phai dang nhap duoc that ---
$sKt = Connect-App $TEN_KT $MK_KT
$t = Get-Trang $sKt "/"
Kiem-Tra -Ten "$TEN_KT dang nhap duoc bang mat khau admin vua dat" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Đăng xuất') | Out-Null

# --- 9.5 ⭐ Khoa tai khoan phai da duoc PHIEN DANG MO ---
$r = Post-Form $adm "/quan-tri/nguoi-dung" "/quan-tri/nguoi-dung/$idKt/khoa" @{ }
Kiem-Tra -Ten "admin khoa tai khoan $TEN_KT" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('Đã khoá tài khoản') | Out-Null

$t = Get-Trang $sKt "/"
Kiem-Tra -Ten "⭐ Phien dang mo cua $TEN_KT bi thoat ra ngay" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') -KhongDuocCo @('Đăng xuất') | Out-Null

$sKt2 = Connect-App $TEN_KT $MK_KT
$t = Get-Trang $sKt2 "/"
Kiem-Tra -Ten "Tai khoan da khoa thi khong dang nhap lai duoc" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') -KhongDuocCo @('Đăng xuất') | Out-Null

# --- 9.6 Khong tu khoa duoc chinh minh ---
# Nut khoa da an tren dong cua chinh nguoi dang dang nhap, nhung day la request
# gui thang - dung thu ma man hinh khong cho bam.
$r = Post-Form $adm "/quan-tri/nguoi-dung" "/quan-tri/nguoi-dung/$idAdmin/khoa" @{ }
Kiem-Tra -Ten "admin tu khoa chinh minh bi chan" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('Không thể tự khoá') | Out-Null

$t = Get-Trang $adm "/quan-tri/nguoi-dung"
Kiem-Tra -Ten "Doi chung: admin van vao duoc man hinh sau khi bi tu choi" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Quản lý người dùng') | Out-Null

# ---------------------------------------------------------------------
Muc "10. Tu doi mat khau (V3b)"
# ---------------------------------------------------------------------
# NGHIEM THU CUA KE HOACH: "doi mat khau xong dang nhap bang mat khau cu phai truot".
#
# Dung tai khoan kiemthu01 cua muc 9 (dang o trang thai da khoa) nen phai mo khoa
# truoc. Cuoi muc nay mat khau duoc dat lai ve $MK_KT bang duong quan tri, de lan
# chay sau bat dau lai tu dung diem xuat phat.

$MK_MOI = "matkhaumoi456"

$t = Get-Trang $adm "/quan-tri/nguoi-dung"
$dong = Lay-Dong-NguoiDung $t.body $TEN_KT
if ($dong.Contains("Đã khoá")) {
    Post-Form $adm "/quan-tri/nguoi-dung" "/quan-tri/nguoi-dung/$idKt/mo-khoa" @{ } | Out-Null
}

$sKt = Connect-App $TEN_KT $MK_KT
$t = Get-Trang $sKt "/doi-mat-khau"
Kiem-Tra -Ten "$TEN_KT mo duoc man hinh doi mat khau (vai tro nhan vien)" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Đổi mật khẩu', 'name="matKhauCu"', 'name="matKhauMoi"', 'name="xacNhanMatKhauMoi"') | Out-Null

# --- 10.1 Go sai mat khau hien tai thi bi chan ---
$r = Post-Form $sKt "/doi-mat-khau" "/doi-mat-khau" @{
    matKhauCu = "doan-bua"; matKhauMoi = $MK_MOI; xacNhanMatKhauMoi = $MK_MOI }
Kiem-Tra -Ten "⭐ Sai mat khau hien tai thi bi chan" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('Mật khẩu hiện tại không đúng') | Out-Null

# Doi chung: mat khau CHUA he doi sau lan bi chan o tren
$sDoiChung = Connect-App $TEN_KT $MK_KT
$t = Get-Trang $sDoiChung "/"
Kiem-Tra -Ten "Doi chung: bi chan thi mat khau van nguyen" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Đăng xuất') | Out-Null

# --- 10.2 Hai o mat khau moi khong khop thi bi chan ---
$r = Post-Form $sKt "/doi-mat-khau" "/doi-mat-khau" @{
    matKhauCu = $MK_KT; matKhauMoi = $MK_MOI; xacNhanMatKhauMoi = "go-nham" }
Kiem-Tra -Ten "Hai o mat khau moi khong khop thi bi chan" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('không giống nhau') | Out-Null

# --- 10.3 Doi that: phien hien tai phai ket thuc ---
$sKt = Connect-App $TEN_KT $MK_KT
$r = Post-Form $sKt "/doi-mat-khau" "/doi-mat-khau" @{
    matKhauCu = $MK_KT; matKhauMoi = $MK_MOI; xacNhanMatKhauMoi = $MK_MOI }
Kiem-Tra -Ten "⭐ Doi xong bi dua ve man hinh dang nhap" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('Đã đổi mật khẩu', 'name="matKhau"') -KhongDuocCo @('Đăng xuất') | Out-Null

$t = Get-Trang $sKt "/"
Kiem-Tra -Ten "⭐ Phien cu khong dung tiep duoc sau khi doi mat khau" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') -KhongDuocCo @('Đăng xuất') | Out-Null

# --- 10.4 NGHIEM THU: mat khau cu phai TRUOT, mat khau moi phai VAO DUOC ---
$sCu = Connect-App $TEN_KT $MK_KT
$t = Get-Trang $sCu "/"
Kiem-Tra -Ten "⭐ NGHIEM THU: dang nhap bang mat khau CU bi truot" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') -KhongDuocCo @('Đăng xuất') | Out-Null

$sMoi = Connect-App $TEN_KT $MK_MOI
$t = Get-Trang $sMoi "/"
Kiem-Tra -Ten "Doi chung: dang nhap bang mat khau MOI thi vao duoc" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Đăng xuất') | Out-Null

# --- 10.5 Quan tri dat lai mat khau thi phien dang mo cua nguoi do bi da ---
$r = Post-Form $adm "/quan-tri/nguoi-dung/$idKt/sua" "/quan-tri/nguoi-dung/luu" @{
    id = $idKt; tenDangNhap = $TEN_KT; hoTen = "Tai khoan kiem thu"
    email = "kiemthu01@vnptbilling.local"; vaiTro = "NHAN_VIEN"; matKhau = $MK_KT }
Kiem-Tra -Ten "admin dat lai mat khau cho $TEN_KT" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('Đã cập nhật tài khoản') | Out-Null

$t = Get-Trang $sMoi "/"
Kiem-Tra -Ten "⭐ Quan tri dat lai mat khau thi phien dang mo cua nguoi do bi thoat" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') -KhongDuocCo @('Đăng xuất') | Out-Null

# ---------------------------------------------------------------------
Muc "11. Khoa tam sau 5 lan nhap sai (V3c - tra no nghiem thu)"
# ---------------------------------------------------------------------
# NGHIEM THU CUA KE HOACH: "sai mat khau 5 lan thi lan 6 bi khoa".
# V3c da lam tu dot truoc nhung phep kiem nay chua bao gio duoc viet ra.
#
# Phep kiem manh nhat o day la buoc doi chung: dang trong thoi gian khoa tam thi
# MAT KHAU DUNG cung khong vao duoc. Chi kiem "nhap sai bi tu choi" thi khong phan
# biet duoc voi hanh vi binh thuong cua mot he thong KHONG he co khoa tam.

$sLoc = $null
Invoke-WebRequest -Uri "$Global:BaseUrl/dang-nhap" -SessionVariable sLoc -UseBasicParsing -TimeoutSec 10 | Out-Null

$ketQuaLanCuoi = $null
for ($i = 1; $i -le 5; $i++) {
    $ketQuaLanCuoi = Post-Form $sLoc "/dang-nhap" "/dang-nhap" @{
        tenDangNhap = $TEN_KT; matKhau = "sai-lan-$i" }
}
Kiem-Tra -Ten "⭐ Nhap sai 5 lan lien tiep thi tai khoan bi khoa tam" -KetQua $ketQuaLanCuoi -StatusMongDoi 200 `
    -CanCo @('Tài khoản đang tạm khoá') | Out-Null

$r = Post-Form $sLoc "/dang-nhap" "/dang-nhap" @{ tenDangNhap = $TEN_KT; matKhau = $MK_KT }
Kiem-Tra -Ten "⭐ DOI CHUNG: dang bi khoa tam thi mat khau DUNG cung khong vao duoc" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('name="matKhau"') -KhongDuocCo @('Đăng xuất') | Out-Null

$r = Post-Form $adm "/quan-tri/nguoi-dung" "/quan-tri/nguoi-dung/$idKt/mo-khoa" @{ }
Kiem-Tra -Ten "Quan tri go duoc khoa tam ngay, khong phai doi 15 phut" -KetQua $r -StatusMongDoi 200 `
    -CanCo @('Đã mở khoá tài khoản') | Out-Null

$sSauMoKhoa = Connect-App $TEN_KT $MK_KT
$t = Get-Trang $sSauMoKhoa "/"
Kiem-Tra -Ten "Mo khoa xong thi dang nhap lai duoc ngay" -KetQua $t -StatusMongDoi 200 `
    -CanCo @('Đăng xuất') | Out-Null

# Tra trang thai ve dung diem xuat phat: kiemthu01 bi khoa, mat khau la $MK_KT
Post-Form $adm "/quan-tri/nguoi-dung" "/quan-tri/nguoi-dung/$idKt/khoa" @{ } | Out-Null


Ket-Thuc
