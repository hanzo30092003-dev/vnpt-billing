# =====================================================================
# _chung.ps1 - Ham dung chung cho cac script kiem thu giao dien
# =====================================================================
# Cac script khac nap file nay bang:  . "$PSScriptRoot\_chung.ps1"
#
# BAI HOC TU PHASE 2 (da ghi trong docs/PHASE-2-REPORT.md muc 6.2):
# Truoc day cac script chi do chuoi trong HTML tra ve ma khong xet ma
# trang thai HTTP. Hau qua: mot loi HTTP 500 van "dau" bai kiem thu, vi
# trang loi cung dung layout chung nen van chua cac chuoi dang tim - tham
# chi van chua CSRF token cua form dang xuat, khien buoc POST sau do chay
# duoc. Vi vay ham Kiem-Tra duoi day BAT BUOC xet status TRUOC, chi khi
# status dung moi do den noi dung.
# =====================================================================

$Global:BaseUrl = "http://localhost:8080"
$Global:SoDat = 0
$Global:SoSai = 0

function Get-Csrf($html) {
    if ($html -match 'name="_csrf"\s+value="([^"]+)"') { return $Matches[1] }
    return $null
}

# Dang nhap, tra ve WebSession da xac thuc
function Connect-App($tenDangNhap, $matKhau) {
    $s = $null
    $r = Invoke-WebRequest -Uri "$Global:BaseUrl/dang-nhap" -SessionVariable s -UseBasicParsing -TimeoutSec 10
    try {
        Invoke-WebRequest -Uri "$Global:BaseUrl/dang-nhap" -Method Post -WebSession $s `
            -UseBasicParsing -TimeoutSec 10 `
            -Body @{ tenDangNhap = $tenDangNhap; matKhau = $matKhau; _csrf = (Get-Csrf $r.Content) } | Out-Null
    } catch { }
    return $s
}

# GET mot trang, luon tra ve @{ status; body } ke ca khi loi
function Get-Trang($session, $duongDan) {
    try {
        $r = Invoke-WebRequest -Uri "$Global:BaseUrl$duongDan" -WebSession $session -UseBasicParsing `
            -Headers @{ Accept = "text/html" } -TimeoutSec 20
        return @{ status = [int]$r.StatusCode; body = [string]$r.Content }
    } catch {
        $resp = $_.Exception.Response
        $b = ""
        try { $b = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd() } catch { }
        $st = 0
        try { $st = [int]$resp.StatusCode } catch { }
        return @{ status = $st; body = [string]$b }
    }
}

# POST mot form. Lay CSRF token tu $trangLayCsrf truoc khi gui.
function Post-Form($session, $trangLayCsrf, $duongDanPost, $duLieu) {
    $t = Get-Trang $session $trangLayCsrf
    # Neu trang lay token bi loi thi bao ngay, khong im lang lay token tu trang loi
    if ($t.status -ne 200) {
        return @{ status = $t.status; body = $t.body; loiLayToken = $true }
    }
    $duLieu["_csrf"] = Get-Csrf $t.body
    try {
        $r = Invoke-WebRequest -Uri "$Global:BaseUrl$duongDanPost" -Method Post -Body $duLieu `
            -WebSession $session -UseBasicParsing -Headers @{ Accept = "text/html" } -TimeoutSec 30
        return @{ status = [int]$r.StatusCode; body = [string]$r.Content }
    } catch {
        $resp = $_.Exception.Response
        $b = ""
        try { $b = (New-Object IO.StreamReader($resp.GetResponseStream())).ReadToEnd() } catch { }
        $st = 0
        try { $st = [int]$resp.StatusCode } catch { }
        return @{ status = $st; body = [string]$b }
    }
}

# ---------------------------------------------------------------------
# Ham kiem tra chinh: XET STATUS TRUOC, chi khi status dung moi do chuoi
# ---------------------------------------------------------------------
# LUU Y VE Write-Host (sua o Phase 5 muc F):
# Ham nay vua bao ket qua vua "return $true/$false", ma trong PowerShell
# ca hai deu di vao CUNG MOT LUONG output. Moi noi goi deu ket thuc bang
# "| Out-Null" de nuot cai boolean - va thanh ra nuot luon cac dong
# [DAT ] / [SAI ] neu chung duoc ghi bang Write-Output.
#
# Hau qua truoc khi sua: mot phep kiem TRUOT chi lam bien dem tang len,
# con dong noi ro thieu chuoi nao thi khong bao gio hien ra. Nguoi chay
# script thay "3 sai" ma khong co cach nao biet sai o dau.
#
# Write-Host ghi thang ra man hinh, khong qua pipeline, nen Out-Null chi
# con nuot dung cai boolean nhu y dinh ban dau.
# ---------------------------------------------------------------------
function Kiem-Tra {
    param(
        [string] $Ten,
        [hashtable] $KetQua,
        [int] $StatusMongDoi = 200,
        [string[]] $CanCo = @(),
        [string[]] $KhongDuocCo = @()
    )

    if ($KetQua.loiLayToken) {
        Write-Host ("  [SAI ] {0}" -f $Ten)
        Write-Host ("         Trang lay CSRF token tra ve HTTP {0}, khong the gui form" -f $KetQua.status)
        $Global:SoSai++
        return $false
    }

    # BUOC 1 - bat buoc: ma trang thai HTTP
    if ($KetQua.status -ne $StatusMongDoi) {
        Write-Host ("  [SAI ] {0}" -f $Ten)
        Write-Host ("         HTTP {0}, mong doi {1}" -f $KetQua.status, $StatusMongDoi)
        $Global:SoSai++
        return $false
    }

    # BUOC 2 - chi chay khi status da dung
    foreach ($chuoi in $CanCo) {
        if (-not $KetQua.body.Contains($chuoi)) {
            Write-Host ("  [SAI ] {0}" -f $Ten)
            Write-Host ("         HTTP {0} dung nhung khong tim thay chuoi: {1}" -f $KetQua.status, $chuoi)
            $Global:SoSai++
            return $false
        }
    }
    foreach ($chuoi in $KhongDuocCo) {
        if ($KetQua.body.Contains($chuoi)) {
            Write-Host ("  [SAI ] {0}" -f $Ten)
            Write-Host ("         Khong duoc chua chuoi nhung van thay: {1}" -f $KetQua.status, $chuoi)
            $Global:SoSai++
            return $false
        }
    }

    Write-Host ("  [DAT ] {0}" -f $Ten)
    $Global:SoDat++
    return $true
}

function Bat-Dau($tieuDe) {
    $Global:SoDat = 0
    $Global:SoSai = 0
    Write-Output ""
    Write-Output "====================================================="
    Write-Output "  $tieuDe"
    Write-Output "====================================================="
}

function Ket-Thuc {
    Write-Output ""
    Write-Output "-----------------------------------------------------"
    Write-Output ("  KET QUA: {0} dat / {1} sai" -f $Global:SoDat, $Global:SoSai)
    Write-Output "-----------------------------------------------------"
    if ($Global:SoSai -gt 0) { exit 1 }
}

function Muc($ten) {
    Write-Output ""
    Write-Output "--- $ten ---"
}
