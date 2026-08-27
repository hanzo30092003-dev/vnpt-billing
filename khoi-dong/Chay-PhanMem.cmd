@echo off
REM ---------------------------------------------------------------------
REM Ban du phong cua Chay-PhanMem.ps1.
REM
REM Vi sao can file nay: nhay dup mot file .ps1 tren Windows KHONG chay no.
REM Mac dinh Windows mo .ps1 bang Notepad. File .cmd thi nhay dup la chay.
REM
REM Loi tat tren Desktop goi thang sang .ps1 nen khong can file nay. No danh
REM cho luc chua tao loi tat, hoac luc ai do mo thang thu muc du an.
REM
REM KHONG dat -NoExit o day: cua so cmd se treo o dau nhac PowerShell. Thay
REM vao do dung pause khi loi - cung tac dung giu chu lai de doc.
REM ---------------------------------------------------------------------
chcp 65001 > nul
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0Chay-PhanMem.ps1"
if errorlevel 1 pause
