-- =====================================================================
-- schema.sql - Script tao cau truc bang cua CSDL vnpt_billing
-- =====================================================================
-- Phase 0: file nay CO CHU DINH de rong.
-- application.yml tro toi day nen file bat buoc phai ton tai, neu khong
-- ung dung se bao loi "No SQL scripts found at location" khi khoi dong.
--
-- Phase 1 se bo sung cac lenh CREATE TABLE: khach_hang, thue_bao,
-- goi_cuoc, bang_gia, cdr, ky_cuoc, hoa_don, chi_tiet_hoa_don,
-- thanh_toan, nguoi_dung, vai_tro ...
-- =====================================================================

-- LUU Y: Spring boc bo toan bo chu thich truoc khi chay script. Neu file
-- chi co chu thich thi phan con lai la chuoi rong va ScriptUtils se nem
-- loi "'script' must not be null or empty" khi khoi dong.
-- Cau lenh SELECT duoi day la no-op giu cho script hop le, KHONG tao bang
-- va KHONG thay doi du lieu. Phase 1 se xoa dong nay khi them CREATE TABLE.
SELECT 1;

