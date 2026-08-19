-- =====================================================================
-- V2__khoa_lac_quan_va_chong_do_mat_khau.sql
-- =====================================================================
-- Hai lan doi cau truc bang cua DOT HOAN THIEN, gop lai thanh mot buoc di tru.
--
-- Truoc khi co Flyway, ca hai lan nay deu duoc go bang tay truc tiep vao CSDL
-- dang chay (ALTER TABLE tren console) roi sua them vao schema.sql. Cach do co
-- hai cho hong: (1) khong ai biet CSDL cua minh da co cot chua, (2) muon dua
-- CSDL ve dung cau truc moi thi phai chay lai schema.sql - ma file do mo dau
-- bang DROP TABLE, tuc MAT SACH DU LIEU. Day chinh la vet den ma viec V4 sinh
-- ra de xoa: tu day, doi cau truc bang la them mot file, chay len la xong,
-- du lieu giu nguyen.
--
-- Ca ba cot deu co DEFAULT nen ALTER chay duoc tren bang DANG CO DU LIEU:
-- moi dong cu nhan gia tri mac dinh, khong dong nao phai sua tay.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. hoa_don.phien_ban - khoa lac quan (viec V1 cua dot hoan thien)
-- ---------------------------------------------------------------------
-- Hibernate them "WHERE phien_ban = ?" vao cau UPDATE, nen hai thu ngan cung
-- thu tien tren mot hoa don thi nguoi ghi sau bi tu choi thay vi de ghi de len
-- nguoi truoc - mat mot lan cong, tuc mat tien cua khach.
-- Dung lai duoc bang KiemTraDongThoiThanhToanTest.epDocDocGhiGhi_benGhiSauBiTuChoi.
--
-- DEFAULT 0 de cac cau INSERT cu trong data-mau.sql / data-van-hanh.sql
-- (khong liet ke cot nay) van chay duoc.
ALTER TABLE hoa_don
    ADD COLUMN phien_ban BIGINT NOT NULL DEFAULT 0;

-- ---------------------------------------------------------------------
-- 2. nguoi_dung.so_lan_sai + khoa_den_luc - chong do mat khau (viec V3c)
-- ---------------------------------------------------------------------
-- Dem so lan nhap sai LIEN TIEP; du 5 lan thi khoa tam tai khoan toi
-- khoa_den_luc. Dang nhap dung mot lan la ca hai ve 0/NULL.
--
-- Khoa TAM chu khong khoa vinh vien: khoa vinh vien bien chinh co che bao ve
-- thanh mot cach tu choi dich vu - ke xau chi can co tinh nhap sai 5 lan vao
-- tai khoan nguoi khac la khoa duoc ho vo thoi han.
ALTER TABLE nguoi_dung
    ADD COLUMN so_lan_sai   INT      NOT NULL DEFAULT 0,
    ADD COLUMN khoa_den_luc DATETIME NULL;
