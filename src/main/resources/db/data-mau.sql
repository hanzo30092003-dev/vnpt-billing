-- =====================================================================
-- data-mau.sql - DU LIEU MAU cho CSDL vnpt_billing
-- =====================================================================
-- CANH BAO: TOAN BO DU LIEU TRONG FILE NAY LA DU LIEU GIA LAP TU SINH,
-- phuc vu muc dich hoc tap cua do an Thuc tap nghe nghiep.
-- KHONG phai du lieu that cua bat ky nha mang nao. Moi ten nguoi, ten
-- doanh nghiep, so CCCD, ma so thue, so dien thoai va dia chi deu la
-- hu cau; su trung hop voi thuc te (neu co) la ngoai y muon.
-- =====================================================================
-- File nay chay SAU schema.sql, ma schema.sql da DROP toan bo bang nen
-- moi lan khoi dong deu nap lai tu dau. Vi vay ID duoc ghi tuong minh
-- de cac khoa ngoai tham chieu on dinh.
-- =====================================================================

-- ---------------------------------------------------------------------
-- 1. nguoi_dung - 3 tai khoan
-- ---------------------------------------------------------------------
-- Mat khau cua ca 3 tai khoan deu la: 123456
-- Chuoi duoi day la hash BCrypt (cost 10) cua "123456", duoc sinh bang
-- BCryptPasswordEncoder cua Spring Security va da kiem chung matches()=true.
-- KHONG BAO GIO luu mat khau dang van ban thuan trong CSDL.
-- ---------------------------------------------------------------------
INSERT INTO nguoi_dung (id, ten_dang_nhap, mat_khau, ho_ten, email, vai_tro, trang_thai, ngay_tao) VALUES
(1, 'admin',      '$2a$10$pPFiHT8ZOiiBMho6Xlb4VuP1T3RqmVOA4MpW4GiFYsp6nf2dvva2m', 'Quản trị hệ thống', 'admin@vnptbilling.local',      'ADMIN',     1, '2025-01-01 08:00:00'),
(2, 'nhanvien01', '$2a$10$pPFiHT8ZOiiBMho6Xlb4VuP1T3RqmVOA4MpW4GiFYsp6nf2dvva2m', 'Nguyễn Văn Nhân',   'nhanvien01@vnptbilling.local', 'NHAN_VIEN', 1, '2025-01-01 08:00:00'),
(3, 'ketoan01',   '$2a$10$pPFiHT8ZOiiBMho6Xlb4VuP1T3RqmVOA4MpW4GiFYsp6nf2dvva2m', 'Trần Thị Kế Toán',  'ketoan01@vnptbilling.local',   'KE_TOAN',   1, '2025-01-01 08:00:00');

-- ---------------------------------------------------------------------
-- 2. goi_cuoc - 5 goi
-- ---------------------------------------------------------------------
INSERT INTO goi_cuoc (id, ma_goi, ten_goi, loai_thue_bao, cuoc_thue_bao_thang,
                      phut_noi_mang_mien_phi, phut_ngoai_mang_mien_phi, sms_mien_phi, data_mien_phi_mb,
                      mo_ta, ngay_hieu_luc, ngay_het_hieu_luc, trang_thai) VALUES
(1, 'CB01',   'Cơ bản',            'TRA_SAU',   50000,     0,   0,   0,     0, 'Gói trả sau cơ bản, không có ưu đãi kèm theo, cước phát sinh tính theo bảng giá chung.', '2025-01-01', NULL, 1),
(2, 'MAX70',  'MAX70',             'TRA_SAU',   70000,   100,   0,  30,  2048, 'Gói trả sau phổ thông: 100 phút nội mạng, 30 SMS và 2 GB data mỗi tháng.',              '2025-01-01', NULL, 1),
(3, 'MAX150', 'MAX150',            'TRA_SAU',  150000,   500, 100, 100,  5120, 'Gói trả sau cao cấp: 500 phút nội mạng, 100 phút ngoại mạng, 100 SMS và 5 GB data.',   '2025-01-01', NULL, 1),
(4, 'DN500',  'Doanh nghiệp 500',  'TRA_SAU',  500000,  2000, 500, 500, 20480, 'Gói dành cho khách doanh nghiệp: 2000 phút nội mạng, 500 phút ngoại mạng, 500 SMS và 20 GB data.', '2025-01-01', NULL, 1),
(5, 'TT01',   'Trả trước chuẩn',   'TRA_TRUOC',     0,     0,   0,   0,     0, 'Gói trả trước tiêu chuẩn, không có cước thuê bao tháng, trừ tiền trực tiếp vào tài khoản.', '2025-01-01', NULL, 1);

-- ---------------------------------------------------------------------
-- 3. bang_gia_cuoc - bang gia mac dinh (goi_cuoc_id = NULL)
-- ---------------------------------------------------------------------
-- goi_cuoc_id NULL nghia la don gia ap dung chung cho moi goi cuoc.
-- block_giay: don vi tinh block - giay voi THOAI, 1 tin voi SMS, 1 MB voi DATA.
-- Ban ghi gio_cao_diem = 1 chi ap dung cho dich vu THOAI, don gia cao hon 20%.
-- ---------------------------------------------------------------------
INSERT INTO bang_gia_cuoc (goi_cuoc_id, loai_dich_vu, huong, gio_cao_diem, block_giay, don_gia, ngay_hieu_luc, ngay_het_hieu_luc) VALUES
(NULL, 'THOAI', 'NOI_MANG',   0,  6,   15, '2025-01-01', NULL),
(NULL, 'THOAI', 'NGOAI_MANG', 0,  6,   25, '2025-01-01', NULL),
(NULL, 'THOAI', 'QUOC_TE',    0, 60, 3600, '2025-01-01', NULL),
(NULL, 'SMS',   'NOI_MANG',   0,  1,   99, '2025-01-01', NULL),
(NULL, 'SMS',   'NGOAI_MANG', 0,  1,  250, '2025-01-01', NULL),
(NULL, 'DATA',  'NOI_MANG',   0,  1,   25, '2025-01-01', NULL),
-- Gio cao diem: +20% so voi gia thuong
(NULL, 'THOAI', 'NOI_MANG',   1,  6,   18, '2025-01-01', NULL),
(NULL, 'THOAI', 'NGOAI_MANG', 1,  6,   30, '2025-01-01', NULL),
(NULL, 'THOAI', 'QUOC_TE',    1, 60, 4320, '2025-01-01', NULL);

-- ---------------------------------------------------------------------
-- 4. khach_hang - 50 khach (35 ca nhan + 15 doanh nghiep)
-- ---------------------------------------------------------------------
-- Ca nhan: so_giay_to la CCCD 12 so. Doanh nghiep: so_giay_to la MST 10 so.
-- Dia chi lay theo cac tinh Dong bang song Cuu Long.
-- ---------------------------------------------------------------------
INSERT INTO khach_hang (id, ma_kh, loai_kh, ten_kh, so_giay_to, ngay_sinh, nguoi_dai_dien,
                        dia_chi, dien_thoai_lh, email, ngay_dang_ky, trang_thai, ghi_chu) VALUES
(1,  'KH0001', 'CA_NHAN', 'Nguyễn Văn An',     '092301004517', '1988-03-12', NULL, '145 Trần Hưng Đạo, Ninh Kiều, Cần Thơ',        '0901234501', 'an.nv@example.local',     '2024-01-10', 'HOAT_DONG', NULL),
(2,  'KH0002', 'CA_NHAN', 'Trần Thị Bình',     '089201118426', '1992-07-25', NULL, '27 Nguyễn Trãi, Long Xuyên, An Giang',         '0912345602', 'binh.tt@example.local',   '2024-02-15', 'HOAT_DONG', NULL),
(3,  'KH0003', 'CA_NHAN', 'Lê Hoàng Cường',    '091301227315', '1985-11-03', NULL, '88 Lê Lợi, Rạch Giá, Kiên Giang',              '0933456703', 'cuong.lh@example.local',  '2024-03-01', 'HOAT_DONG', NULL),
(4,  'KH0004', 'CA_NHAN', 'Phạm Thị Dung',     '087202336204', '1995-05-18', NULL, '12 Phạm Hữu Lầu, Cao Lãnh, Đồng Tháp',         '0944567804', 'dung.pt@example.local',   '2024-03-22', 'HOAT_DONG', NULL),
(5,  'KH0005', 'CA_NHAN', 'Huỳnh Văn Đức',     '086301445193', '1990-09-30', NULL, '56 Hùng Vương, Vĩnh Long',                     '0965678905', 'duc.hv@example.local',    '2024-04-08', 'HOAT_DONG', NULL),
(6,  'KH0006', 'CA_NHAN', 'Võ Thị Em',         '084202554082', '1998-01-14', NULL, '9 Nguyễn Thị Minh Khai, Trà Vinh',             '0976789006', 'em.vt@example.local',     '2024-05-03', 'HOAT_DONG', NULL),
(7,  'KH0007', 'CA_NHAN', 'Đặng Minh Phúc',    '094301663971', '1983-06-27', NULL, '233 Lê Duẩn, Sóc Trăng',                       '0987890107', 'phuc.dm@example.local',   '2024-06-12', 'HOAT_DONG', NULL),
(8,  'KH0008', 'CA_NHAN', 'Bùi Thị Giang',     '095202772860', '1993-12-05', NULL, '41 Trần Phú, Bạc Liêu',                        '0818901208', 'giang.bt@example.local',  '2024-06-28', 'HOAT_DONG', 'Khách hàng đề nghị tạm ngưng hai chiều'),
(9,  'KH0009', 'CA_NHAN', 'Ngô Văn Hải',       '096301881759', '1987-04-19', NULL, '70 Nguyễn Tất Thành, Cà Mau',                  '0829012309', 'hai.nv@example.local',    '2024-07-20', 'HOAT_DONG', NULL),
(10, 'KH0010', 'CA_NHAN', 'Dương Thị Hoa',     '093202990648', '1991-08-08', NULL, '18 Võ Văn Kiệt, Vị Thanh, Hậu Giang',          '0830123410', 'hoa.dt@example.local',    '2024-08-09', 'HOAT_DONG', NULL),
(11, 'KH0011', 'CA_NHAN', 'Lý Văn Khang',      '082301109537', '1996-02-22', NULL, '104 Ấp Bắc, Mỹ Tho, Tiền Giang',               '0841234511', 'khang.lv@example.local',  '2024-08-29', 'HOAT_DONG', NULL),
(12, 'KH0012', 'CA_NHAN', 'Trương Thị Lan',    '083202218426', '1989-10-11', NULL, '62 Đồng Khởi, Bến Tre',                        '0852345612', 'lan.tt@example.local',    '2024-09-17', 'HOAT_DONG', NULL),
(13, 'KH0013', 'CA_NHAN', 'Phan Văn Long',     '080301327315', '1994-03-07', NULL, '215 Hùng Vương, Tân An, Long An',              '0883456713', 'long.pv@example.local',   '2024-10-05', 'HOAT_DONG', NULL),
(14, 'KH0014', 'CA_NHAN', 'Đỗ Thị Mai',        '092202436204', '1997-11-29', NULL, '33 Mậu Thân, Ninh Kiều, Cần Thơ',              '0904567814', 'mai.dt@example.local',    '2024-10-31', 'HOAT_DONG', NULL),
(15, 'KH0015', 'CA_NHAN', 'Hồ Văn Nam',        '089301545193', '1986-07-16', NULL, '77 Trần Hưng Đạo, Châu Đốc, An Giang',         '0915678915', 'nam.hv@example.local',    '2024-11-23', 'NGUNG_GIAO_DICH', 'Khách hàng đã thanh lý thuê bao và ngừng giao dịch'),
(16, 'KH0016', 'CA_NHAN', 'Vũ Thị Ngọc',       '091202654082', '1999-05-04', NULL, '8 Nguyễn Trung Trực, Rạch Giá, Kiên Giang',    '0936789016', 'ngoc.vt@example.local',   '2024-12-11', 'HOAT_DONG', NULL),
(17, 'KH0017', 'CA_NHAN', 'Cao Văn Phong',     '087301763971', '1984-09-21', NULL, '150 Nguyễn Huệ, Sa Đéc, Đồng Tháp',            '0947890117', 'phong.cv@example.local',  '2025-01-04', 'HOAT_DONG', NULL),
(18, 'KH0018', 'CA_NHAN', 'Lâm Thị Quyên',     '086202872860', '1992-01-08', NULL, '24 Lê Thái Tổ, Bình Minh, Vĩnh Long',          '0968901218', 'quyen.lt@example.local',  '2025-01-25', 'HOAT_DONG', NULL),
(19, 'KH0019', 'CA_NHAN', 'Tạ Văn Sơn',        '084301981759', '1990-06-13', NULL, '91 Điện Biên Phủ, Trà Vinh',                   '0979012319', 'son.tv@example.local',    '2025-02-13', 'HOAT_DONG', NULL),
(20, 'KH0020', 'CA_NHAN', 'Đinh Thị Thu',      '094202090648', '1995-12-26', NULL, '37 Hai Bà Trưng, Sóc Trăng',                   '0810123420', 'thu.dt@example.local',    '2025-03-02', 'HOAT_DONG', NULL),
(21, 'KH0021', 'CA_NHAN', 'Mai Văn Tuấn',      '095301209537', '1988-08-17', NULL, '128 Cách Mạng Tháng 8, Bạc Liêu',              '0821234521', 'tuan.mv@example.local',   '2025-03-20', 'HOAT_DONG', NULL),
(22, 'KH0022', 'CA_NHAN', 'Châu Thị Uyên',     '096202318426', '1993-04-02', NULL, '65 Lý Thường Kiệt, Cà Mau',                    '0832345622', 'uyen.ct@example.local',   '2025-04-06', 'HOAT_DONG', NULL),
(23, 'KH0023', 'CA_NHAN', 'Kiều Văn Vinh',     '093301427315', '1981-10-24', NULL, '19 Trần Quốc Toản, Vị Thanh, Hậu Giang',       '0843456723', 'vinh.kv@example.local',   '2025-04-28', 'HOAT_DONG', NULL),
(24, 'KH0024', 'CA_NHAN', 'Thạch Thị Xuân',    '082202536204', '1996-02-09', NULL, '82 Lê Văn Duyệt, Gò Công, Tiền Giang',         '0854567824', 'xuan.tt@example.local',   '2025-05-15', 'HOAT_DONG', 'Nợ cước kéo dài, đã tạm ngưng hai chiều'),
(25, 'KH0025', 'CA_NHAN', 'Nguyễn Hữu Yên',    '083301645193', '1987-11-30', NULL, '46 Nguyễn Đình Chiểu, Bến Tre',                '0885678925', 'yen.nh@example.local',    '2025-06-02', 'HOAT_DONG', NULL),
(26, 'KH0026', 'CA_NHAN', 'Trần Quốc Bảo',     '080202754082', '1994-07-12', NULL, '173 Quốc lộ 1A, Bến Lức, Long An',             '0906789026', 'bao.tq@example.local',    '2025-06-21', 'HOAT_DONG', NULL),
(27, 'KH0027', 'CA_NHAN', 'Lê Thị Cẩm',        '092301863971', '1998-03-25', NULL, '59 Nguyễn Văn Cừ, Ninh Kiều, Cần Thơ',         '0917890127', 'cam.lt@example.local',    '2025-07-09', 'HOAT_DONG', NULL),
(28, 'KH0028', 'CA_NHAN', 'Phạm Hoàng Duy',    '089202972860', '1985-09-06', NULL, '11 Tôn Đức Thắng, Long Xuyên, An Giang',       '0938901228', 'duy.ph@example.local',    '2025-07-29', 'HOAT_DONG', NULL),
(29, 'KH0029', 'CA_NHAN', 'Huỳnh Thị Hạnh',    '091301081759', '1991-01-19', NULL, '94 Mạc Cửu, Hà Tiên, Kiên Giang',              '0949012329', 'hanh.ht@example.local',   '2025-08-18', 'HOAT_DONG', NULL),
(30, 'KH0030', 'CA_NHAN', 'Võ Minh Khôi',      '087202190648', '1997-05-28', NULL, '30 Lê Đại Hành, Cao Lãnh, Đồng Tháp',          '0960123430', 'khoi.vm@example.local',   '2025-09-05', 'HOAT_DONG', NULL),
(31, 'KH0031', 'CA_NHAN', 'Đặng Thị Linh',     '086301309537', '1989-12-14', NULL, '67 Phạm Thái Bường, Vĩnh Long',                '0971234531', 'linh.dt@example.local',   '2025-09-24', 'HOAT_DONG', NULL),
(32, 'KH0032', 'CA_NHAN', 'Bùi Quang Minh',    '084202418426', '1993-06-01', NULL, '22 Nguyễn Đáng, Trà Vinh',                     '0812345632', 'minh.bq@example.local',   '2025-10-12', 'HOAT_DONG', NULL),
(33, 'KH0033', 'CA_NHAN', 'Ngô Thị Như',       '094301527315', '1996-10-07', NULL, '108 Trần Hưng Đạo, Sóc Trăng',                 '0823456733', 'nhu.nt@example.local',    '2025-11-01', 'HOAT_DONG', 'Đã thanh lý thuê bao trả sau tháng 5/2026'),
(34, 'KH0034', 'CA_NHAN', 'Dương Văn Quân',    '095202636204', '1990-04-23', NULL, '75 Võ Thị Sáu, Bạc Liêu',                      '0834567834', 'quan.dv@example.local',   '2026-06-08', 'HOAT_DONG', 'Khách hàng mới, hòa mạng giữa tháng 6/2026'),
(35, 'KH0035', 'CA_NHAN', 'Lý Thị Trâm',       '096301745193', '1999-08-15', NULL, '53 Phan Ngọc Hiển, Cà Mau',                    '0845678935', 'tram.lt@example.local',   '2026-06-14', 'HOAT_DONG', 'Khách hàng mới, hòa mạng giữa tháng 6/2026'),
(36, 'KH0036', 'DOANH_NGHIEP', 'Công ty TNHH Thương mại Cửu Long',   '1801234567', NULL, 'Nguyễn Thành Trung', '201 Nguyễn Văn Linh, Ninh Kiều, Cần Thơ',       '02923812001', 'lienhe@cuulongtm.example.local',  '2024-01-15', 'HOAT_DONG', NULL),
(37, 'KH0037', 'DOANH_NGHIEP', 'Công ty CP Thủy sản Hậu Giang',      '1802345678', NULL, 'Trần Thị Kim Anh',   'Lô B2 KCN Sông Hậu, Châu Thành, Hậu Giang',     '02933812002', 'lienhe@haugiangts.example.local', '2024-02-08', 'HOAT_DONG', NULL),
(38, 'KH0038', 'DOANH_NGHIEP', 'Công ty TNHH Xây dựng Tây Đô',       '1803456789', NULL, 'Lê Minh Hoàng',      '46 Trần Văn Khéo, Ninh Kiều, Cần Thơ',          '02923812003', 'lienhe@taydoxd.example.local',    '2024-03-05', 'HOAT_DONG', NULL),
(39, 'KH0039', 'DOANH_NGHIEP', 'Công ty CP Lương thực Đồng Tháp',    '1804567890', NULL, 'Phạm Văn Sáng',      'KCN Trần Quốc Toản, Cao Lãnh, Đồng Tháp',       '02773812004', 'lienhe@dongthaplt.example.local', '2024-03-30', 'HOAT_DONG', NULL),
(40, 'KH0040', 'DOANH_NGHIEP', 'Công ty TNHH Vận tải Sông Tiền',     '1805678901', NULL, 'Huỳnh Quốc Việt',    '88 Lý Thường Kiệt, Mỹ Tho, Tiền Giang',         '02733812005', 'lienhe@songtienvt.example.local', '2024-05-12', 'HOAT_DONG', NULL),
(41, 'KH0041', 'DOANH_NGHIEP', 'Công ty CP Nông sản An Giang',       '1806789012', NULL, 'Võ Thị Ngọc Hân',    'KCN Bình Long, Châu Phú, An Giang',             '02963812006', 'lienhe@angiangns.example.local',  '2024-06-18', 'HOAT_DONG', NULL),
(42, 'KH0042', 'DOANH_NGHIEP', 'Công ty TNHH Cơ khí Cần Thơ',        '1807890123', NULL, 'Đặng Hữu Nghĩa',     'KCN Trà Nóc 1, Bình Thủy, Cần Thơ',             '02923812007', 'lienhe@cankhoct.example.local',   '2024-07-25', 'HOAT_DONG', NULL),
(43, 'KH0043', 'DOANH_NGHIEP', 'Công ty CP Du lịch Kiên Giang',      '1808901234', NULL, 'Bùi Thanh Tùng',     '12 Nguyễn Trung Trực, Rạch Giá, Kiên Giang',    '02973812008', 'lienhe@kiengiangdl.example.local','2024-08-20', 'HOAT_DONG', NULL),
(44, 'KH0044', 'DOANH_NGHIEP', 'Công ty TNHH May mặc Vĩnh Long',     '1809012345', NULL, 'Ngô Thị Bích Trâm',  'KCN Hòa Phú, Long Hồ, Vĩnh Long',               '02703812009', 'lienhe@vinhlongmm.example.local', '2024-09-12', 'HOAT_DONG', NULL),
(45, 'KH0045', 'DOANH_NGHIEP', 'Công ty CP Dược phẩm Trà Vinh',      '1810123456', NULL, 'Dương Văn Thắng',    '59 Nguyễn Thị Minh Khai, Trà Vinh',             '02943812010', 'lienhe@travinhdp.example.local',  '2024-10-16', 'HOAT_DONG', NULL),
(46, 'KH0046', 'DOANH_NGHIEP', 'Công ty TNHH Điện lạnh Sóc Trăng',   '1811234567', NULL, 'Lý Hoàng Nam',       '174 Lê Duẩn, Sóc Trăng',                        '02993812011', 'lienhe@soctrangdl.example.local', '2024-11-08', 'HOAT_DONG', NULL),
(47, 'KH0047', 'DOANH_NGHIEP', 'Công ty CP Chế biến Bạc Liêu',       '1812345678', NULL, 'Trương Minh Đức',    'KCN Trà Kha, Bạc Liêu',                         '02913812012', 'lienhe@baclieucb.example.local',  '2024-12-03', 'HOAT_DONG', NULL),
(48, 'KH0048', 'DOANH_NGHIEP', 'Công ty TNHH Thủy hải sản Cà Mau',   '1813456789', NULL, 'Phan Thị Thu Hà',    'KCN Hòa Trung, Cái Nước, Cà Mau',               '02903812013', 'lienhe@camauths.example.local',   '2025-01-10', 'HOAT_DONG', NULL),
(49, 'KH0049', 'DOANH_NGHIEP', 'Công ty CP Vật liệu Tiền Giang',     '1814567890', NULL, 'Đỗ Quang Hưng',      'KCN Mỹ Tho, Tiền Giang',                        '02733812014', 'lienhe@tiengiangvl.example.local','2025-02-05', 'HOAT_DONG', NULL),
(50, 'KH0050', 'DOANH_NGHIEP', 'Công ty TNHH Dừa Bến Tre',           '1815678901', NULL, 'Hồ Thị Mỹ Linh',     'KCN Giao Long, Châu Thành, Bến Tre',            '02753812015', 'lienhe@bentredua.example.local',  '2025-03-14', 'HOAT_DONG', NULL);

-- ---------------------------------------------------------------------
-- 5. thue_bao - 80 thue bao
-- ---------------------------------------------------------------------
-- Phan bo theo yeu cau:
--   Loai      : 60 TRA_SAU (id 21-80), 20 TRA_TRUOC (id 1-20)
--   Trang thai: 65 HOAT_DONG, 8 TAM_NGUNG_1C, 4 TAM_NGUNG_2C, 3 DA_THANH_LY
--   Chu so huu: KH ca nhan (1-35) moi nguoi 1 thue bao = 35
--               KH doanh nghiep (36-50) moi don vi 2-5 thue bao = 45
--   Ngay kich hoat trai deu tu 2024-01 den 2026-06. Cac thue bao 34, 35,
--   78, 79, 80 kich hoat GIUA THANG 6/2026 de thu nghiem tinh cuoc prorate.
--   so_du chi co y nghia voi TRA_TRUOC; han_muc_tin_dung chi voi TRA_SAU.
-- ---------------------------------------------------------------------
INSERT INTO thue_bao (id, so_thue_bao, khach_hang_id, goi_cuoc_id, loai_thue_bao,
                      ngay_kich_hoat, ngay_huy, trang_thai, so_du, han_muc_tin_dung) VALUES
-- Ca nhan tra truoc (goi TT01)
(1,  '0901234501',  1, 5, 'TRA_TRUOC', '2024-01-15', NULL,         'HOAT_DONG',     52000, 0),
(2,  '0912345602',  2, 5, 'TRA_TRUOC', '2024-02-20', NULL,         'HOAT_DONG',    118000, 0),
(3,  '0933456703',  3, 5, 'TRA_TRUOC', '2024-03-05', NULL,         'HOAT_DONG',     27500, 0),
(4,  '0944567804',  4, 5, 'TRA_TRUOC', '2024-03-28', NULL,         'HOAT_DONG',    205000, 0),
(5,  '0965678905',  5, 5, 'TRA_TRUOC', '2024-04-12', NULL,         'TAM_NGUNG_1C',   3000, 0),
(6,  '0976789006',  6, 5, 'TRA_TRUOC', '2024-05-08', NULL,         'HOAT_DONG',     89000, 0),
(7,  '0987890107',  7, 5, 'TRA_TRUOC', '2024-06-17', NULL,         'HOAT_DONG',    143000, 0),
(8,  '0818901208',  8, 5, 'TRA_TRUOC', '2024-07-01', NULL,         'TAM_NGUNG_2C',      0, 0),
(9,  '0829012309',  9, 5, 'TRA_TRUOC', '2024-07-25', NULL,         'HOAT_DONG',     66000, 0),
(10, '0830123410', 10, 5, 'TRA_TRUOC', '2024-08-14', NULL,         'HOAT_DONG',    310000, 0),
(11, '0841234511', 11, 5, 'TRA_TRUOC', '2024-09-03', NULL,         'HOAT_DONG',     15000, 0),
(12, '0852345612', 12, 5, 'TRA_TRUOC', '2024-09-22', NULL,         'TAM_NGUNG_1C',   8500, 0),
(13, '0883456713', 13, 5, 'TRA_TRUOC', '2024-10-10', NULL,         'HOAT_DONG',    175000, 0),
(14, '0904567814', 14, 5, 'TRA_TRUOC', '2024-11-05', NULL,         'HOAT_DONG',     43000, 0),
(15, '0915678915', 15, 5, 'TRA_TRUOC', '2024-11-28', '2026-03-15', 'DA_THANH_LY',       0, 0),
(16, '0936789016', 16, 5, 'TRA_TRUOC', '2024-12-16', NULL,         'HOAT_DONG',     97000, 0),
(17, '0947890117', 17, 5, 'TRA_TRUOC', '2025-01-09', NULL,         'HOAT_DONG',    128000, 0),
(18, '0968901218', 18, 5, 'TRA_TRUOC', '2025-01-30', NULL,         'HOAT_DONG',     61000, 0),
(19, '0979012319', 19, 5, 'TRA_TRUOC', '2025-02-18', NULL,         'HOAT_DONG',    234000, 0),
(20, '0810123420', 20, 5, 'TRA_TRUOC', '2025-03-07', NULL,         'TAM_NGUNG_1C',   1200, 0),
-- Ca nhan tra sau (goi CB01 / MAX70 / MAX150)
(21, '0821234521', 21, 1, 'TRA_SAU',   '2025-03-25', NULL,         'HOAT_DONG',    0,  500000),
(22, '0832345622', 22, 2, 'TRA_SAU',   '2025-04-11', NULL,         'HOAT_DONG',    0,  500000),
(23, '0843456723', 23, 3, 'TRA_SAU',   '2025-05-02', NULL,         'HOAT_DONG',    0, 1000000),
(24, '0854567824', 24, 1, 'TRA_SAU',   '2025-05-20', NULL,         'TAM_NGUNG_2C', 0,  500000),
(25, '0885678925', 25, 2, 'TRA_SAU',   '2025-06-08', NULL,         'HOAT_DONG',    0,  500000),
(26, '0906789026', 26, 3, 'TRA_SAU',   '2025-06-27', NULL,         'HOAT_DONG',    0, 1000000),
(27, '0917890127', 27, 1, 'TRA_SAU',   '2025-07-15', NULL,         'HOAT_DONG',    0,  500000),
(28, '0938901228', 28, 2, 'TRA_SAU',   '2025-08-04', NULL,         'TAM_NGUNG_1C', 0,  500000),
(29, '0949012329', 29, 3, 'TRA_SAU',   '2025-08-23', NULL,         'HOAT_DONG',    0, 1000000),
(30, '0960123430', 30, 1, 'TRA_SAU',   '2025-09-10', NULL,         'HOAT_DONG',    0,  500000),
(31, '0971234531', 31, 2, 'TRA_SAU',   '2025-09-29', NULL,         'HOAT_DONG',    0,  500000),
(32, '0812345632', 32, 3, 'TRA_SAU',   '2025-10-17', NULL,         'HOAT_DONG',    0, 1000000),
(33, '0823456733', 33, 1, 'TRA_SAU',   '2025-11-06', '2026-05-20', 'DA_THANH_LY',  0,  500000),
(34, '0834567834', 34, 2, 'TRA_SAU',   '2026-06-11', NULL,         'HOAT_DONG',    0,  500000),
(35, '0845678935', 35, 3, 'TRA_SAU',   '2026-06-17', NULL,         'HOAT_DONG',    0, 1000000),
-- Doanh nghiep (goi DN500 / MAX150), moi don vi 2-5 thue bao
(36, '0856789036', 36, 4, 'TRA_SAU',   '2024-01-20', NULL,         'HOAT_DONG',    0,  5000000),
(37, '0887890137', 36, 4, 'TRA_SAU',   '2024-01-20', NULL,         'HOAT_DONG',    0,  5000000),
(38, '0908901238', 37, 4, 'TRA_SAU',   '2024-02-14', NULL,         'HOAT_DONG',    0,  5000000),
(39, '0919012339', 37, 3, 'TRA_SAU',   '2024-02-14', NULL,         'TAM_NGUNG_1C', 0,  3000000),
(40, '0930123440', 38, 4, 'TRA_SAU',   '2024-03-11', NULL,         'HOAT_DONG',    0,  5000000),
(41, '0941234541', 38, 4, 'TRA_SAU',   '2024-03-11', NULL,         'HOAT_DONG',    0,  5000000),
(42, '0962345642', 39, 4, 'TRA_SAU',   '2024-04-05', NULL,         'HOAT_DONG',    0,  5000000),
(43, '0973456743', 39, 3, 'TRA_SAU',   '2024-04-05', NULL,         'HOAT_DONG',    0,  3000000),
(44, '0814567844', 40, 4, 'TRA_SAU',   '2024-05-19', NULL,         'HOAT_DONG',    0,  5000000),
(45, '0825678945', 40, 4, 'TRA_SAU',   '2024-05-19', NULL,         'HOAT_DONG',    0,  5000000),
(46, '0836789046', 41, 4, 'TRA_SAU',   '2024-06-23', NULL,         'HOAT_DONG',    0,  5000000),
(47, '0847890147', 41, 3, 'TRA_SAU',   '2024-06-23', NULL,         'TAM_NGUNG_1C', 0,  3000000),
(48, '0858901248', 42, 4, 'TRA_SAU',   '2024-07-30', NULL,         'HOAT_DONG',    0,  8000000),
(49, '0889012349', 42, 4, 'TRA_SAU',   '2024-07-30', NULL,         'HOAT_DONG',    0,  8000000),
(50, '0900123450', 42, 3, 'TRA_SAU',   '2024-07-30', NULL,         'HOAT_DONG',    0,  3000000),
(51, '0911234551', 43, 4, 'TRA_SAU',   '2024-08-26', NULL,         'TAM_NGUNG_2C', 0,  8000000),
(52, '0932345652', 43, 4, 'TRA_SAU',   '2024-08-26', NULL,         'HOAT_DONG',    0,  8000000),
(53, '0943456753', 43, 3, 'TRA_SAU',   '2024-08-26', NULL,         'HOAT_DONG',    0,  3000000),
(54, '0964567854', 44, 4, 'TRA_SAU',   '2024-09-18', NULL,         'HOAT_DONG',    0,  8000000),
(55, '0975678955', 44, 4, 'TRA_SAU',   '2024-09-18', NULL,         'HOAT_DONG',    0,  8000000),
(56, '0816789056', 44, 3, 'TRA_SAU',   '2024-09-18', NULL,         'HOAT_DONG',    0,  3000000),
(57, '0827890157', 45, 4, 'TRA_SAU',   '2024-10-22', NULL,         'HOAT_DONG',    0,  8000000),
(58, '0838901258', 45, 4, 'TRA_SAU',   '2024-10-22', NULL,         'TAM_NGUNG_1C', 0,  8000000),
(59, '0849012359', 45, 3, 'TRA_SAU',   '2024-10-22', NULL,         'HOAT_DONG',    0,  3000000),
(60, '0850123460', 46, 4, 'TRA_SAU',   '2024-11-14', NULL,         'HOAT_DONG',    0,  8000000),
(61, '0881234561', 46, 4, 'TRA_SAU',   '2024-11-14', NULL,         'HOAT_DONG',    0,  8000000),
(62, '0902345662', 46, 3, 'TRA_SAU',   '2024-11-14', NULL,         'HOAT_DONG',    0,  3000000),
(63, '0913456763', 47, 4, 'TRA_SAU',   '2024-12-09', NULL,         'HOAT_DONG',    0, 10000000),
(64, '0934567864', 47, 4, 'TRA_SAU',   '2024-12-09', NULL,         'HOAT_DONG',    0, 10000000),
(65, '0945678965', 47, 4, 'TRA_SAU',   '2024-12-09', NULL,         'HOAT_DONG',    0, 10000000),
(66, '0966789066', 47, 3, 'TRA_SAU',   '2024-12-09', NULL,         'TAM_NGUNG_2C', 0,  3000000),
(67, '0977890167', 48, 4, 'TRA_SAU',   '2025-01-16', NULL,         'HOAT_DONG',    0, 10000000),
(68, '0818901268', 48, 4, 'TRA_SAU',   '2025-01-16', NULL,         'HOAT_DONG',    0, 10000000),
(69, '0829012369', 48, 4, 'TRA_SAU',   '2025-01-16', NULL,         'HOAT_DONG',    0, 10000000),
(70, '0839123470', 48, 3, 'TRA_SAU',   '2025-01-16', NULL,         'TAM_NGUNG_1C', 0,  3000000),
(71, '0840234571', 49, 4, 'TRA_SAU',   '2025-02-11', NULL,         'HOAT_DONG',    0, 10000000),
(72, '0851345672', 49, 4, 'TRA_SAU',   '2025-02-11', NULL,         'HOAT_DONG',    0, 10000000),
(73, '0882456773', 49, 4, 'TRA_SAU',   '2025-02-11', NULL,         'HOAT_DONG',    0, 10000000),
(74, '0903567874', 49, 3, 'TRA_SAU',   '2025-02-11', '2026-04-30', 'DA_THANH_LY',  0,  3000000),
(75, '0914678975', 49, 3, 'TRA_SAU',   '2025-02-11', NULL,         'HOAT_DONG',    0,  3000000),
(76, '0935789076', 50, 4, 'TRA_SAU',   '2025-03-19', NULL,         'HOAT_DONG',    0, 10000000),
(77, '0946890177', 50, 4, 'TRA_SAU',   '2025-03-19', NULL,         'HOAT_DONG',    0, 10000000),
(78, '0967901278', 50, 4, 'TRA_SAU',   '2026-06-05', NULL,         'HOAT_DONG',    0, 10000000),
(79, '0978012379', 50, 3, 'TRA_SAU',   '2026-06-15', NULL,         'HOAT_DONG',    0,  3000000),
(80, '0819123480', 50, 3, 'TRA_SAU',   '2026-06-23', NULL,         'HOAT_DONG',    0,  3000000);

-- Sinh ngay_tao tu ngay kich hoat de khong phai liet ke thu cong 80 dong
UPDATE thue_bao SET ngay_tao = TIMESTAMP(ngay_kich_hoat, '08:00:00');

-- ---------------------------------------------------------------------
-- 6. dang_ky_goi_cuoc - mot ban ghi dang ap dung cho moi thue bao
-- ---------------------------------------------------------------------
-- Sinh truc tiep tu bang thue_bao de bao dam goi cuoc va ngay bat dau
-- luon khop, tranh sai lech do go tay 80 dong.
-- ---------------------------------------------------------------------
INSERT INTO dang_ky_goi_cuoc (thue_bao_id, goi_cuoc_id, ngay_bat_dau, ngay_ket_thuc, trang_thai)
SELECT id, goi_cuoc_id, ngay_kich_hoat, NULL, 'DANG_AP_DUNG' FROM thue_bao;

-- ---------------------------------------------------------------------
-- 7. ky_cuoc - ky thang 6/2026, dang mo
-- ---------------------------------------------------------------------
INSERT INTO ky_cuoc (id, thang, nam, ngay_bat_dau, ngay_ket_thuc, trang_thai,
                     ngay_chot, so_cdr_xu_ly, so_hoa_don_tao, tong_doanh_thu) VALUES
(1, 6, 2026, '2026-06-01', '2026-06-30', 'MO', NULL, 0, 0, 0);

-- ---------------------------------------------------------------------
-- 8. CHUA tao du lieu cho cac bang sau - thuoc pham vi cac phase ke tiep:
--    chi_tiet_su_dung (CDR)  -> Phase 3
--    hoa_don, chi_tiet_hoa_don, thanh_toan -> Phase 4, 5
--    nap_tien, giam_tru, lich_su_thue_bao, nhat_ky_he_thong -> phat sinh
--    trong qua trinh van hanh
-- ---------------------------------------------------------------------
