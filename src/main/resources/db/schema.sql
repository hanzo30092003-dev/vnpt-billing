-- =====================================================================
-- schema.sql - Cau truc CSDL vnpt_billing
-- He thong quan ly thue bao va tinh cuoc dien thoai
-- Do an Thuc tap nghe nghiep - Phase 1
-- =====================================================================
-- LUU Y VE CACH CHAY:
-- application.yml dang dat spring.sql.init.mode=always nen file nay chay
-- LAI MOI LAN KHOI DONG ung dung. Vi vay phai DROP truoc khi CREATE, neu
-- khong lan khoi dong thu hai se loi "table already exists".
-- He qua: moi lan chay app la CSDL bi dung lai tu dau. Chap nhan duoc o
-- Phase 1 vi toan bo la du lieu mau. Sang Phase 3 (co nhap lieu qua giao
-- dien) phai chuyen sang mode=never hoac dung Flyway.
-- =====================================================================

-- ---------------------------------------------------------------------
-- XOA THEO THU TU NGUOC PHU THUOC KHOA NGOAI
-- ---------------------------------------------------------------------
DROP VIEW IF EXISTS v_doanh_thu_thang;
DROP VIEW IF EXISTS v_thong_ke_thue_bao;

DROP TABLE IF EXISTS nhat_ky_he_thong;
DROP TABLE IF EXISTS giam_tru;
DROP TABLE IF EXISTS nap_tien;
DROP TABLE IF EXISTS thanh_toan;
DROP TABLE IF EXISTS chi_tiet_hoa_don;
DROP TABLE IF EXISTS hoa_don;
DROP TABLE IF EXISTS chi_tiet_su_dung;
DROP TABLE IF EXISTS ky_cuoc;
DROP TABLE IF EXISTS dang_ky_goi_cuoc;
DROP TABLE IF EXISTS lich_su_thue_bao;
DROP TABLE IF EXISTS thue_bao;
DROP TABLE IF EXISTS bang_gia_cuoc;
DROP TABLE IF EXISTS goi_cuoc;
DROP TABLE IF EXISTS khach_hang;
DROP TABLE IF EXISTS nguoi_dung;

-- ---------------------------------------------------------------------
-- 1. nguoi_dung - Tai khoan dang nhap he thong
-- ---------------------------------------------------------------------
CREATE TABLE nguoi_dung (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    ten_dang_nhap   VARCHAR(50)  NOT NULL,
    mat_khau        VARCHAR(100) NOT NULL,
    ho_ten          VARCHAR(100) NOT NULL,
    email           VARCHAR(100),
    vai_tro         ENUM('ADMIN','NHAN_VIEN','KE_TOAN') NOT NULL,
    trang_thai      TINYINT      DEFAULT 1,
    ngay_tao        DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_nguoi_dung_ten_dang_nhap (ten_dang_nhap)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 2. khach_hang - Khach hang ca nhan va doanh nghiep
-- ---------------------------------------------------------------------
CREATE TABLE khach_hang (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    ma_kh           VARCHAR(20)  NOT NULL,
    loai_kh         ENUM('CA_NHAN','DOANH_NGHIEP') NOT NULL,
    ten_kh          VARCHAR(200) NOT NULL,
    so_giay_to      VARCHAR(30)  NOT NULL,
    ngay_sinh       DATE,
    nguoi_dai_dien  VARCHAR(100),
    dia_chi         VARCHAR(300) NOT NULL,
    dien_thoai_lh   VARCHAR(15),
    email           VARCHAR(100),
    ngay_dang_ky    DATE         NOT NULL,
    trang_thai      ENUM('HOAT_DONG','NGUNG_GIAO_DICH') NOT NULL,
    ghi_chu         TEXT,
    PRIMARY KEY (id),
    UNIQUE KEY uq_khach_hang_ma_kh (ma_kh),
    KEY idx_khach_hang_so_giay_to (so_giay_to),
    KEY idx_khach_hang_ten_kh (ten_kh)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 3. goi_cuoc - Danh muc goi cuoc
-- ---------------------------------------------------------------------
CREATE TABLE goi_cuoc (
    id                        BIGINT       NOT NULL AUTO_INCREMENT,
    ma_goi                    VARCHAR(20)  NOT NULL,
    ten_goi                   VARCHAR(100) NOT NULL,
    loai_thue_bao             ENUM('TRA_TRUOC','TRA_SAU') NOT NULL,
    cuoc_thue_bao_thang       DECIMAL(15,2) NOT NULL DEFAULT 0,
    phut_noi_mang_mien_phi    INT          DEFAULT 0,
    phut_ngoai_mang_mien_phi  INT          DEFAULT 0,
    sms_mien_phi              INT          DEFAULT 0,
    data_mien_phi_mb          INT          DEFAULT 0,
    mo_ta                     TEXT,
    ngay_hieu_luc             DATE         NOT NULL,
    ngay_het_hieu_luc         DATE,
    trang_thai                TINYINT      DEFAULT 1,
    PRIMARY KEY (id),
    UNIQUE KEY uq_goi_cuoc_ma_goi (ma_goi)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 4. bang_gia_cuoc - Don gia theo dich vu / huong / khung gio
--    goi_cuoc_id = NULL nghia la gia mac dinh ap dung chung cho moi goi
-- ---------------------------------------------------------------------
CREATE TABLE bang_gia_cuoc (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    goi_cuoc_id        BIGINT        NULL,
    loai_dich_vu       ENUM('THOAI','SMS','DATA') NOT NULL,
    huong              ENUM('NOI_MANG','NGOAI_MANG','QUOC_TE') NOT NULL,
    gio_cao_diem       TINYINT       DEFAULT 0,
    block_giay         INT           NOT NULL DEFAULT 6,
    don_gia            DECIMAL(15,2) NOT NULL,
    ngay_hieu_luc      DATE          NOT NULL,
    ngay_het_hieu_luc  DATE,
    PRIMARY KEY (id),
    KEY idx_bang_gia_tra_cuu (loai_dich_vu, huong, ngay_hieu_luc),
    CONSTRAINT fk_bang_gia_goi_cuoc FOREIGN KEY (goi_cuoc_id) REFERENCES goi_cuoc (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 5. thue_bao - Thue bao dien thoai
-- ---------------------------------------------------------------------
CREATE TABLE thue_bao (
    id                 BIGINT        NOT NULL AUTO_INCREMENT,
    so_thue_bao        VARCHAR(15)   NOT NULL,
    khach_hang_id      BIGINT        NOT NULL,
    goi_cuoc_id        BIGINT        NOT NULL,
    loai_thue_bao      ENUM('TRA_TRUOC','TRA_SAU') NOT NULL,
    ngay_kich_hoat     DATE          NOT NULL,
    ngay_huy           DATE,
    trang_thai         ENUM('HOAT_DONG','TAM_NGUNG_1C','TAM_NGUNG_2C','DA_THANH_LY') NOT NULL,
    so_du              DECIMAL(15,2) DEFAULT 0,
    han_muc_tin_dung   DECIMAL(15,2) DEFAULT 0,
    ngay_tao           DATETIME,
    PRIMARY KEY (id),
    UNIQUE KEY uq_thue_bao_so (so_thue_bao),
    KEY idx_thue_bao_khach_hang (khach_hang_id),
    CONSTRAINT fk_thue_bao_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang (id),
    CONSTRAINT fk_thue_bao_goi_cuoc   FOREIGN KEY (goi_cuoc_id)   REFERENCES goi_cuoc (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 6. lich_su_thue_bao - Nhat ky doi trang thai thue bao
-- ---------------------------------------------------------------------
CREATE TABLE lich_su_thue_bao (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    thue_bao_id         BIGINT       NOT NULL,
    trang_thai_cu       VARCHAR(20),
    trang_thai_moi      VARCHAR(20)  NOT NULL,
    ly_do               VARCHAR(300),
    nguoi_thuc_hien_id  BIGINT,
    thoi_gian           DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_lich_su_thue_bao (thue_bao_id),
    CONSTRAINT fk_lich_su_thue_bao       FOREIGN KEY (thue_bao_id)        REFERENCES thue_bao (id),
    CONSTRAINT fk_lich_su_nguoi_thuc_hien FOREIGN KEY (nguoi_thuc_hien_id) REFERENCES nguoi_dung (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 7. dang_ky_goi_cuoc - Lich su dang ky goi cuoc cua thue bao
-- ---------------------------------------------------------------------
CREATE TABLE dang_ky_goi_cuoc (
    id             BIGINT NOT NULL AUTO_INCREMENT,
    thue_bao_id    BIGINT NOT NULL,
    goi_cuoc_id    BIGINT NOT NULL,
    ngay_bat_dau   DATE   NOT NULL,
    ngay_ket_thuc  DATE,
    trang_thai     ENUM('DANG_AP_DUNG','DA_KET_THUC') NOT NULL,
    PRIMARY KEY (id),
    KEY idx_dang_ky_thue_bao (thue_bao_id),
    CONSTRAINT fk_dang_ky_thue_bao FOREIGN KEY (thue_bao_id) REFERENCES thue_bao (id),
    CONSTRAINT fk_dang_ky_goi_cuoc FOREIGN KEY (goi_cuoc_id) REFERENCES goi_cuoc (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 8. ky_cuoc - Ky tinh cuoc theo thang
-- ---------------------------------------------------------------------
CREATE TABLE ky_cuoc (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    thang            INT           NOT NULL,
    nam              INT           NOT NULL,
    ngay_bat_dau     DATE          NOT NULL,
    ngay_ket_thuc    DATE          NOT NULL,
    trang_thai       ENUM('MO','DANG_TINH','DA_CHOT') NOT NULL,
    ngay_chot        DATETIME,
    so_cdr_xu_ly     INT           DEFAULT 0,
    so_hoa_don_tao   INT           DEFAULT 0,
    tong_doanh_thu   DECIMAL(18,2) DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uq_ky_cuoc_thang_nam (thang, nam)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 9. chi_tiet_su_dung - Ban ghi CDR (Call Detail Record)
-- ---------------------------------------------------------------------
CREATE TABLE chi_tiet_su_dung (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    thue_bao_id           BIGINT        NOT NULL,
    so_thue_bao           VARCHAR(15)   NOT NULL,
    so_bi_goi             VARCHAR(20),
    loai_dich_vu          ENUM('THOAI','SMS','DATA') NOT NULL,
    huong                 ENUM('NOI_MANG','NGOAI_MANG','QUOC_TE') NOT NULL,
    thoi_gian_bat_dau     DATETIME      NOT NULL,
    thoi_luong_giay       INT           DEFAULT 0,
    so_luong              INT           DEFAULT 0,
    gio_cao_diem          TINYINT       DEFAULT 0,
    cuoc_phi              DECIMAL(15,2),
    mien_phi              TINYINT       DEFAULT 0,
    trang_thai_tinh_cuoc  ENUM('CHUA_TINH','DA_TINH','LOI') NOT NULL DEFAULT 'CHUA_TINH',
    ky_cuoc_id            BIGINT,
    nguon                 ENUM('GENERATOR','IMPORT_CSV','NHAP_TAY'),
    PRIMARY KEY (id),
    KEY idx_cdr_thue_bao_thoi_gian (thue_bao_id, thoi_gian_bat_dau),
    KEY idx_cdr_trang_thai_tinh_cuoc (trang_thai_tinh_cuoc),
    CONSTRAINT fk_cdr_thue_bao FOREIGN KEY (thue_bao_id) REFERENCES thue_bao (id),
    CONSTRAINT fk_cdr_ky_cuoc  FOREIGN KEY (ky_cuoc_id)  REFERENCES ky_cuoc (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 10. hoa_don - Hoa don cuoc thang
--     UNIQUE(thue_bao_id, ky_cuoc_id) chong lap hoa don trung ky
-- ---------------------------------------------------------------------
CREATE TABLE hoa_don (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    ma_hoa_don        VARCHAR(30)   NOT NULL,
    thue_bao_id       BIGINT        NOT NULL,
    khach_hang_id     BIGINT        NOT NULL,
    ky_cuoc_id        BIGINT        NOT NULL,
    ngay_lap          DATE          NOT NULL,
    han_thanh_toan    DATE          NOT NULL,
    cuoc_thue_bao     DECIMAL(15,2) DEFAULT 0,
    cuoc_thoai        DECIMAL(15,2) DEFAULT 0,
    cuoc_sms          DECIMAL(15,2) DEFAULT 0,
    cuoc_data         DECIMAL(15,2) DEFAULT 0,
    cuoc_khac         DECIMAL(15,2) DEFAULT 0,
    giam_tru          DECIMAL(15,2) DEFAULT 0,
    tong_truoc_thue   DECIMAL(15,2) NOT NULL,
    thue_vat          DECIMAL(15,2) NOT NULL,
    tong_thanh_toan   DECIMAL(15,2) NOT NULL,
    da_thanh_toan     DECIMAL(15,2) DEFAULT 0,
    con_no            DECIMAL(15,2) NOT NULL,
    trang_thai        ENUM('CHUA_TT','TT_MOT_PHAN','DA_TT','QUA_HAN') NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_hoa_don_ma (ma_hoa_don),
    UNIQUE KEY uq_hoa_don_thue_bao_ky (thue_bao_id, ky_cuoc_id),
    KEY idx_hoa_don_khach_hang (khach_hang_id),
    CONSTRAINT fk_hoa_don_thue_bao   FOREIGN KEY (thue_bao_id)   REFERENCES thue_bao (id),
    CONSTRAINT fk_hoa_don_khach_hang FOREIGN KEY (khach_hang_id) REFERENCES khach_hang (id),
    CONSTRAINT fk_hoa_don_ky_cuoc    FOREIGN KEY (ky_cuoc_id)    REFERENCES ky_cuoc (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 11. chi_tiet_hoa_don - Cac dong khoan muc cua hoa don
-- ---------------------------------------------------------------------
CREATE TABLE chi_tiet_hoa_don (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    hoa_don_id   BIGINT        NOT NULL,
    khoan_muc    VARCHAR(150)  NOT NULL,
    so_luong     DECIMAL(15,2),
    don_vi       VARCHAR(20),
    don_gia      DECIMAL(15,2),
    thanh_tien   DECIMAL(15,2) NOT NULL,
    PRIMARY KEY (id),
    KEY idx_chi_tiet_hoa_don (hoa_don_id),
    CONSTRAINT fk_chi_tiet_hoa_don FOREIGN KEY (hoa_don_id) REFERENCES hoa_don (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 12. thanh_toan - Giao dich thanh toan hoa don
-- ---------------------------------------------------------------------
CREATE TABLE thanh_toan (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    ma_giao_dich      VARCHAR(30)   NOT NULL,
    hoa_don_id        BIGINT        NOT NULL,
    so_tien           DECIMAL(15,2) NOT NULL,
    hinh_thuc         ENUM('TIEN_MAT','CHUYEN_KHOAN','VI_DIEN_TU') NOT NULL,
    ngay_thanh_toan   DATETIME      NOT NULL,
    nguoi_thu_id      BIGINT,
    ghi_chu           VARCHAR(300),
    PRIMARY KEY (id),
    UNIQUE KEY uq_thanh_toan_ma_giao_dich (ma_giao_dich),
    KEY idx_thanh_toan_hoa_don (hoa_don_id),
    CONSTRAINT fk_thanh_toan_hoa_don   FOREIGN KEY (hoa_don_id)   REFERENCES hoa_don (id),
    CONSTRAINT fk_thanh_toan_nguoi_thu FOREIGN KEY (nguoi_thu_id) REFERENCES nguoi_dung (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 13. nap_tien - Nap tien cho thue bao tra truoc
-- ---------------------------------------------------------------------
CREATE TABLE nap_tien (
    id                   BIGINT        NOT NULL AUTO_INCREMENT,
    thue_bao_id          BIGINT        NOT NULL,
    so_tien              DECIMAL(15,2) NOT NULL,
    so_du_truoc          DECIMAL(15,2),
    so_du_sau            DECIMAL(15,2),
    hinh_thuc            ENUM('THE_CAO','CHUYEN_KHOAN','TAI_QUAY'),
    ngay_nap             DATETIME      NOT NULL,
    nguoi_thuc_hien_id   BIGINT,
    PRIMARY KEY (id),
    KEY idx_nap_tien_thue_bao (thue_bao_id),
    CONSTRAINT fk_nap_tien_thue_bao      FOREIGN KEY (thue_bao_id)        REFERENCES thue_bao (id),
    CONSTRAINT fk_nap_tien_nguoi_thuc_hien FOREIGN KEY (nguoi_thuc_hien_id) REFERENCES nguoi_dung (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 14. giam_tru - Khoan giam tru ap dung cho thue bao trong ky
-- ---------------------------------------------------------------------
CREATE TABLE giam_tru (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    thue_bao_id      BIGINT        NOT NULL,
    ky_cuoc_id       BIGINT,
    loai             ENUM('KHUYEN_MAI','SU_CO_DICH_VU','CHIET_KHAU_DN','KHAC') NOT NULL,
    so_tien          DECIMAL(15,2),
    ty_le_phan_tram  DECIMAL(5,2),
    ly_do            VARCHAR(300),
    trang_thai       ENUM('CHUA_AP_DUNG','DA_AP_DUNG'),
    PRIMARY KEY (id),
    KEY idx_giam_tru_thue_bao (thue_bao_id),
    CONSTRAINT fk_giam_tru_thue_bao FOREIGN KEY (thue_bao_id) REFERENCES thue_bao (id),
    CONSTRAINT fk_giam_tru_ky_cuoc  FOREIGN KEY (ky_cuoc_id)  REFERENCES ky_cuoc (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- 15. nhat_ky_he_thong - Ghi vet thao tac nguoi dung
-- ---------------------------------------------------------------------
CREATE TABLE nhat_ky_he_thong (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    nguoi_dung_id  BIGINT,
    hanh_dong      VARCHAR(100) NOT NULL,
    doi_tuong      VARCHAR(50),
    doi_tuong_id   BIGINT,
    noi_dung       TEXT,
    dia_chi_ip     VARCHAR(45),
    thoi_gian      DATETIME     NOT NULL,
    PRIMARY KEY (id),
    KEY idx_nhat_ky_nguoi_dung (nguoi_dung_id),
    CONSTRAINT fk_nhat_ky_nguoi_dung FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------
-- VIEW 1: v_thong_ke_thue_bao
-- Dem so thue bao theo loai va trang thai
-- ---------------------------------------------------------------------
CREATE VIEW v_thong_ke_thue_bao AS
SELECT
    tb.loai_thue_bao          AS loai_thue_bao,
    tb.trang_thai             AS trang_thai,
    COUNT(*)                  AS so_luong,
    COALESCE(SUM(tb.so_du),0) AS tong_so_du
FROM thue_bao tb
GROUP BY tb.loai_thue_bao, tb.trang_thai;

-- ---------------------------------------------------------------------
-- VIEW 2: v_doanh_thu_thang
-- Tong hop doanh thu hoa don theo nam / thang cua ky cuoc
-- ---------------------------------------------------------------------
CREATE VIEW v_doanh_thu_thang AS
SELECT
    kc.nam                                AS nam,
    kc.thang                              AS thang,
    COUNT(hd.id)                          AS so_hoa_don,
    COALESCE(SUM(hd.tong_thanh_toan),0)   AS tong_doanh_thu,
    COALESCE(SUM(hd.da_thanh_toan),0)     AS da_thu,
    COALESCE(SUM(hd.con_no),0)            AS con_no
FROM ky_cuoc kc
LEFT JOIN hoa_don hd ON hd.ky_cuoc_id = kc.id
GROUP BY kc.nam, kc.thang;
