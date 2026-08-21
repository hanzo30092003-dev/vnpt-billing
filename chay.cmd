@echo off
REM Vo boc de bam doi chuot chay duoc tu Explorer, va de khong phai chinh
REM ExecutionPolicy cua PowerShell.
REM
REM     chay              phat trien  - bien dich lai, co DevTools
REM     chay demo         chay tu jar - nhanh hon, khong bien dich lai
REM     chay reset        XOA SACH CSDL roi nap lai du lieu mau

set CHEDO=%1
if "%CHEDO%"=="" set CHEDO=phat-trien

powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\chay.ps1" -CheDo %CHEDO%
if errorlevel 1 pause
