<#
    chay.ps1 - khoi dong ung dung. Goi qua chay.cmd o goc du an.

    Vi sao co script nay: hai thu lam mat thoi gian nhat khi chay tay la
    (1) cong 8080 con ban vi mot ban chay truoc chua tat - Spring Boot bao
    "Port 8080 was already in use" roi dung han, va (2) phai nho ba cau
    lenh khac nhau cho ba viec khac nhau.

    Ba che do, so giay do duoc tren may dev bang cach chay xen ke 3 luot:

        chay          phat trien  - bien dich lai, co DevTools      ~8 giay
        chay demo     chay tu jar - KHONG bien dich lai             ~6 giay
        chay reset    XOA SACH CSDL roi nap lai du lieu mau

    Che do demo nhanh hon vi bo duoc phan Maven (~4 giay). Doi lai no khong
    bien dich, nen chi dung khi ma nguon dang dung yen - luc chup anh, luc
    demo. Sua ma xong thi dung che do phat trien.
#>
param(
    [ValidateSet('phat-trien', 'demo', 'reset')]
    [string] $CheDo = 'phat-trien'
)

$ErrorActionPreference = 'Stop'
$goc = Split-Path -Parent $PSScriptRoot
Set-Location $goc

function Ghi($chu, $mau = 'Gray') { Write-Host $chu -ForegroundColor $mau }

# ---------------------------------------------------------------------
# 1. Cong 8080 con ai giu khong
# ---------------------------------------------------------------------
$dangGiu = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
if ($dangGiu) {
    $soHieu = @($dangGiu)[0].OwningProcess
    $tt = Get-Process -Id $soHieu -ErrorAction SilentlyContinue
    Ghi ""
    Ghi "  Cong 8080 dang bi chiem." Yellow
    Ghi "  Tien trinh $soHieu ($($tt.ProcessName)), chay tu $($tt.StartTime)."
    Ghi ""
    $tl = Read-Host "  Dung no de chay ban moi? [c/K]"
    if ($tl -eq 'c' -or $tl -eq 'C') {
        Stop-Process -Id $soHieu -Force
        Start-Sleep -Seconds 2
        Ghi "  Da dung tien trinh $soHieu." Green
    } else {
        Ghi "  Giu nguyen. Ung dung dang chay o http://localhost:8080" Cyan
        exit 0
    }
}

# ---------------------------------------------------------------------
# 2. Hoi truoc khi lam viec khong quay lai duoc.
#    Dat TRUOC bang thong tin: mot canh bao pha huy nam duoi mot khoi chao
#    than thien thi rat de bam cho qua.
# ---------------------------------------------------------------------
if ($CheDo -eq 'reset') {
    Ghi ""
    Ghi "  CHE DO RESET" Red
    Ghi "  Se XOA SACH co so du lieu roi nap lai du lieu mau." Red
    Ghi "  Moi hoa don, thanh toan va so cai hien co se mat." Red
    $tl = Read-Host "  Chac chua? [c/K]"
    if ($tl -ne 'c' -and $tl -ne 'C') { Ghi "  Da huy."; exit 0 }
}

# ---------------------------------------------------------------------
# 3. Chay
# ---------------------------------------------------------------------
Ghi ""
Ghi "  Dia chi     http://localhost:8080" Cyan
Ghi "  Tai khoan   admin / nhanvien01 / ketoan01   (mat khau xem README muc 5)"
Ghi "  Dung lai    bam Ctrl+C trong cua so nay"
Ghi ""

switch ($CheDo) {

    'demo' {
        $jar = Join-Path $goc 'target\billing-0.0.1-SNAPSHOT.jar'
        if (-not (Test-Path $jar)) {
            Ghi "  Chua co ban dong goi. Dang dong goi lan dau (~8 giay)..." Yellow
            & .\mvnw.cmd -q package -DskipTests
            if ($LASTEXITCODE -ne 0) { Ghi "  Dong goi that bai." Red; exit 1 }
        } else {
            # Jar cu hon ma nguon thi BAO chu khong tu dong goi lai: dong goi
            # mat 8 giay, cong 6 giay chay la 14 giay - cham hon ca che do phat
            # trien. Nguoi dung tu chon, nhung phai biet minh dang chay ban cu.
            $mucJar = (Get-Item $jar).LastWriteTime
            $moiNhat = Get-ChildItem -Path (Join-Path $goc 'src') -Recurse -File |
                       Sort-Object LastWriteTime -Descending | Select-Object -First 1
            if ($moiNhat -and $moiNhat.LastWriteTime -gt $mucJar) {
                Ghi "  CANH BAO: ma nguon moi hon ban dong goi." Yellow
                Ghi "  File moi nhat: $($moiNhat.Name) ($($moiNhat.LastWriteTime))"
                Ghi "  Ban dang chay se KHONG co thay doi do."
                Ghi "  Muon co: chay khong tham so, hoac .\mvnw.cmd package -DskipTests"
                Ghi ""
            }
        }
        Ghi "  Che do demo - chay tu ban dong goi, khong bien dich lai." Green
        & java -jar $jar
    }

    'reset' {
        Ghi "  Dang xoa va nap lai du lieu mau..." Yellow
        & .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=reset"
    }

    default {
        Ghi "  Che do phat trien - sua ma nguon xong luu lai la tu nap lai." Green
        & .\mvnw.cmd spring-boot:run
    }
}
