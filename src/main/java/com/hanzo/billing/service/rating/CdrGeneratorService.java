package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaSinhCdr;
import com.hanzo.billing.dto.SinhCdrForm;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Bộ sinh CDR giả lập.
 *
 * <p>Sinh dữ liệu chi tiết sử dụng dịch vụ để làm đầu vào cho engine tính cước ở
 * Phase 4. Mọi bản ghi sinh ra đều ở trạng thái {@code CHUA_TINH} với
 * {@code cuoc_phi = NULL} — Phase 3 tuyệt đối không tính cước.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CdrGeneratorService {

    // ---------- Phân bố loại dịch vụ: 70% thoại, 20% SMS, 10% data ----------
    private static final int NGUONG_THOAI = 70;
    private static final int NGUONG_SMS = 90;   // 70..89 là SMS, 90..99 là DATA

    // ---------- Phân bố hướng: 55% nội mạng, 40% ngoại mạng, 5% quốc tế ----------
    // Chỉ áp dụng cho dịch vụ có nhiều hướng (THOAI và SMS). DATA luôn NOI_MANG —
    // xem QuyTacToHopDichVu.
    private static final int NGUONG_NOI_MANG = 55;
    private static final int NGUONG_NGOAI_MANG = 95;

    // ---------- Khung giờ phát sinh: tập trung 7h–23h ----------
    private static final int GIO_SOM_NHAT = 7;
    private static final int GIO_MUON_NHAT = 23;

    /** Kích thước lô ghi CSDL. */
    private static final int KICH_THUOC_LO = 500;

    private static final String SQL_CHEN = """
            INSERT INTO chi_tiet_su_dung
                (thue_bao_id, so_thue_bao, so_bi_goi, loai_dich_vu, huong,
                 thoi_gian_bat_dau, thoi_luong_giay, so_luong, gio_cao_diem,
                 cuoc_phi, mien_phi, trang_thai_tinh_cuoc, ky_cuoc_id, nguon)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 0, 'CHUA_TINH', NULL, 'GENERATOR')
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ThueBaoRepository thueBaoRepository;
    private final NhatKyService nhatKyService;

    /** Một bản ghi CDR đã dựng xong, chờ ghi xuống CSDL theo lô. */
    private record BanGhiCdr(Long thueBaoId, String soThueBao, String soBiGoi,
                             String loaiDichVu, String huong, LocalDateTime thoiGian,
                             int thoiLuongGiay, int soLuong, boolean gioCaoDiem) {
    }

    /**
     * Sinh CDR giả lập.
     *
     * <h2>Hạt giống — thêm ở Phase 6 mục A1</h2>
     * <p>Trước Phase 6, lớp này giữ một {@code Random} khởi tạo bằng {@code new Random()} ở mức
     * <b>field</b>. Hai khiếm khuyết cùng lúc: bộ dữ liệu sinh ra <b>không tái lập được</b>, và
     * một field khả biến dùng chung giữa các request là thứ không nên có trong một singleton.</p>
     *
     * <p>Nay {@code Random} là <b>biến cục bộ</b> của đúng lần chạy này, khởi tạo từ hạt giống
     * người dùng nhập — hoặc từ một hạt giống hệ thống tự bốc rồi <b>trả về trong kết quả</b>.
     * Nhờ vậy mọi lần sinh đều nói ra được con số dựng lại chính nó.</p>
     */
    @Transactional
    public KetQuaSinhCdr sinh(SinhCdrForm form) {
        long batDau = System.currentTimeMillis();

        if (form.getDenNgay().isBefore(form.getTuNgay())) {
            throw new NghiepVuException("Ngày kết thúc phải sau hoặc bằng ngày bắt đầu");
        }

        // Bốc hạt giống TRƯỚC khi dùng, để con số ghi vào kết quả đúng là con số đã dùng.
        long hatGiong = form.getHatGiong() != null ? form.getHatGiong() : new Random().nextLong();
        Random ngauNhien = new Random(hatGiong);

        List<ThueBao> dsThueBao = layThueBaoHopLe(form);
        if (dsThueBao.isEmpty()) {
            throw new NghiepVuException("Không có thuê bao nào hợp lệ để sinh dữ liệu");
        }

        // Danh sách số dùng làm "số bị gọi" khi cuộc gọi là NỘI MẠNG
        List<String> soNoiMang = dsThueBao.stream().map(ThueBao::getSoThueBao).toList();

        KetQuaSinhCdr ketQua = new KetQuaSinhCdr();
        List<BanGhiCdr> lo = new ArrayList<>(KICH_THUOC_LO);
        boolean[] thueBaoCoPhatSinh = new boolean[dsThueBao.size()];
        int daSinh = 0;

        for (int i = 0; i < form.getSoLuong(); i++) {
            int viTri = ngauNhien.nextInt(dsThueBao.size());
            ThueBao thueBao = dsThueBao.get(viTri);

            LocalDateTime thoiDiem = sinhThoiDiem(
                    thueBao, form.getTuNgay(), form.getDenNgay(), ngauNhien);
            if (thoiDiem == null) {
                // Thuê bao này kích hoạt sau khoảng yêu cầu, bỏ qua lượt sinh
                continue;
            }

            LoaiDichVu dichVu = chonLoaiDichVu(ngauNhien);
            HuongCuocGoi huong = chonHuong(dichVu, ngauNhien);

            BanGhiCdr banGhi = new BanGhiCdr(
                    thueBao.getId(),
                    thueBao.getSoThueBao(),
                    sinhSoBiGoi(huong, soNoiMang, thueBao.getSoThueBao(), ngauNhien),
                    dichVu.name(),
                    huong.name(),
                    thoiDiem,
                    dichVu == LoaiDichVu.THOAI ? sinhThoiLuongGoi(ngauNhien) : 0,
                    sinhSoLuong(dichVu, ngauNhien),
                    // Quy tắc giờ cao điểm nằm ở QuyTacGioCaoDiem, dùng chung với bộ nhập CSV
                    QuyTacGioCaoDiem.apDung(dichVu, thoiDiem));

            lo.add(banGhi);
            thueBaoCoPhatSinh[viTri] = true;
            daSinh++;

            ketQua.getTheoLoaiDichVu().merge(dichVu.name(), 1, Integer::sum);
            ketQua.getTheoHuong().merge(huong.name(), 1, Integer::sum);

            if (lo.size() >= KICH_THUOC_LO) {
                ghiLo(lo);
                lo.clear();
            }
        }
        if (!lo.isEmpty()) {
            ghiLo(lo);
        }

        int soCoPhatSinh = 0;
        for (boolean co : thueBaoCoPhatSinh) {
            if (co) {
                soCoPhatSinh++;
            }
        }

        ketQua.setTongBanGhi(daSinh);
        ketQua.setSoThueBaoCoPhatSinh(soCoPhatSinh);
        ketQua.setSoThueBaoBoQua(dsThueBao.size() - soCoPhatSinh);
        ketQua.setThoiGianMs(System.currentTimeMillis() - batDau);
        ketQua.setHatGiongDaDung(hatGiong);

        log.info("Đã sinh {} bản ghi CDR trong {} ms, hạt giống {}",
                daSinh, ketQua.getThoiGianMs(), hatGiong);
        // Ghi hạt giống vào nhật ký: đây là chỗ duy nhất còn lưu lại con số đó sau khi người
        // dùng rời màn hình, và là thứ duy nhất dựng lại được đúng bộ CDR này.
        nhatKyService.ghiNhatKy("SINH_CDR", "CHI_TIET_SU_DUNG", null,
                "Sinh " + daSinh + " bản ghi CDR từ " + form.getTuNgay() + " đến "
                        + form.getDenNgay() + ", hạt giống " + hatGiong
                        + ", mất " + ketQua.getThoiGianMs() + " ms");

        return ketQua;
    }

    // =================================================================
    // GHI XUỐNG CSDL THEO LÔ
    // =================================================================

    /**
     * Ghi một lô bản ghi bằng {@link JdbcTemplate#batchUpdate}.
     *
     * <p><b>Vì sao không dùng {@code repository.saveAll()}:</b> saveAll của Spring Data
     * gọi {@code persist} cho từng phần tử, sinh ra đúng bằng đó câu INSERT rời và giữ
     * toàn bộ entity trong persistence context. Với 5000 bản ghi, chi phí này lớn hơn
     * batchUpdate nhiều lần và tốn bộ nhớ vô ích vì các entity đó không dùng lại.</p>
     *
     * <p>Kích thước lô 500 là điểm cân bằng: đủ lớn để giảm số vòng đi lại tới CSDL,
     * đủ nhỏ để câu lệnh gộp không vượt giới hạn {@code max_allowed_packet} của MySQL.</p>
     */
    private void ghiLo(List<BanGhiCdr> lo) {
        jdbcTemplate.batchUpdate(SQL_CHEN, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                BanGhiCdr b = lo.get(i);
                ps.setLong(1, b.thueBaoId());
                ps.setString(2, b.soThueBao());
                ps.setString(3, b.soBiGoi());
                ps.setString(4, b.loaiDichVu());
                ps.setString(5, b.huong());
                ps.setTimestamp(6, Timestamp.valueOf(b.thoiGian()));
                ps.setInt(7, b.thoiLuongGiay());
                ps.setInt(8, b.soLuong());
                ps.setInt(9, b.gioCaoDiem() ? 1 : 0);
            }

            @Override
            public int getBatchSize() {
                return lo.size();
            }
        });
    }

    // =================================================================
    // CÁC QUY TẮC SINH DỮ LIỆU
    // =================================================================

    /**
     * Thuê bao được phép sinh CDR.
     *
     * <p>Loại bỏ TAM_NGUNG_2C và DA_THANH_LY: thuê bao khoá hai chiều không gọi đi
     * cũng không nhận được, còn thuê bao đã thanh lý thì số đã thu hồi. Sinh CDR cho
     * chúng là dữ liệu vô nghĩa và sẽ làm sai kết quả tính cước.</p>
     */
    private List<ThueBao> layThueBaoHopLe(SinhCdrForm form) {
        List<ThueBao> dsGoc;
        if ("CHON".equals(form.getPhamVi()) && form.getThueBaoIds() != null
                && !form.getThueBaoIds().isEmpty()) {
            dsGoc = thueBaoRepository.findAllById(form.getThueBaoIds());
        } else {
            dsGoc = thueBaoRepository.findByTrangThai(TrangThaiThueBao.HOAT_DONG);
        }
        return dsGoc.stream()
                .filter(tb -> tb.getTrangThai() != TrangThaiThueBao.TAM_NGUNG_2C
                        && tb.getTrangThai() != TrangThaiThueBao.DA_THANH_LY)
                // ⭐ SẮP THEO id LÀ ĐIỀU KIỆN CỦA HẠT GIỐNG, không phải để cho gọn.
                // Bộ sinh bốc thuê bao bằng chỉ số trong danh sách này, nên cùng một hạt giống
                // chỉ cho ra cùng bộ dữ liệu khi danh sách có cùng thứ tự. Hai truy vấn ở trên
                // đều KHÔNG có ORDER BY — thứ tự chúng trả về là chi tiết cài đặt của CSDL,
                // đúng loại thứ hôm nay ổn định và ngày mai thì không.
                .sorted(Comparator.comparing(ThueBao::getId))
                .toList();
    }

    /**
     * Sinh thời điểm phát sinh cho một thuê bao.
     *
     * <p><b>Quy tắc quan trọng:</b> không bao giờ sinh CDR trước ngày kích hoạt của
     * thuê bao. Năm thuê bao trong dữ liệu mẫu kích hoạt giữa tháng 6/2026 vì vậy chỉ
     * có CDR từ ngày kích hoạt trở đi — đây chính là điều kiện để thử nghiệm tính cước
     * theo tỷ lệ ngày (prorate) ở Phase 4.</p>
     *
     * @return null nếu thuê bao kích hoạt sau cả khoảng yêu cầu
     */
    private LocalDateTime sinhThoiDiem(ThueBao thueBao, LocalDate tuNgay, LocalDate denNgay,
                                       Random ngauNhien) {
        LocalDate batDau = thueBao.getNgayKichHoat().isAfter(tuNgay)
                ? thueBao.getNgayKichHoat() : tuNgay;
        if (batDau.isAfter(denNgay)) {
            return null;
        }
        long soNgay = denNgay.toEpochDay() - batDau.toEpochDay();
        LocalDate ngay = batDau.plusDays(soNgay <= 0 ? 0 : ngauNhien.nextLong(soNgay + 1));

        // Giờ tập trung 7h–23h cho giống thói quen sử dụng thực tế,
        // không rải đều 0h–24h vì ban đêm gần như không phát sinh.
        int gio = GIO_SOM_NHAT + ngauNhien.nextInt(GIO_MUON_NHAT - GIO_SOM_NHAT + 1);
        return ngay.atTime(gio, ngauNhien.nextInt(60), ngauNhien.nextInt(60));
    }

    /** 70% THOAI, 20% SMS, 10% DATA. */
    private LoaiDichVu chonLoaiDichVu(Random ngauNhien) {
        int r = ngauNhien.nextInt(100);
        if (r < NGUONG_THOAI) {
            return LoaiDichVu.THOAI;
        }
        return r < NGUONG_SMS ? LoaiDichVu.SMS : LoaiDichVu.DATA;
    }

    /**
     * Hướng của bản ghi — <b>phụ thuộc loại dịch vụ</b>.
     *
     * <p>Dịch vụ có nhiều hướng (THOAI, SMS) giữ nguyên phân bố 55% nội mạng /
     * 40% ngoại mạng / 5% quốc tế. Dịch vụ chỉ có một hướng hợp lệ (DATA) thì không
     * bốc ngẫu nhiên, trả thẳng hướng đó.</p>
     *
     * <p><b>Trước Phase 4A hàm này không nhận tham số</b> — hướng được bốc độc lập với
     * loại dịch vụ, nên sinh ra {@code DATA + NGOAI_MANG} và {@code DATA + QUOC_TE},
     * hai tổ hợp không tồn tại về mặt nghiệp vụ và không có dòng bảng giá nào khớp.
     * Danh sách hướng hợp lệ nằm ở {@link QuyTacToHopDichVu}, dùng chung với bộ nhập CSV.</p>
     */
    private HuongCuocGoi chonHuong(LoaiDichVu dichVu, Random ngauNhien) {
        Set<HuongCuocGoi> hopLe = QuyTacToHopDichVu.huongHopLe(dichVu);
        if (hopLe.size() == 1) {
            return hopLe.iterator().next();
        }
        int r = ngauNhien.nextInt(100);
        if (r < NGUONG_NOI_MANG) {
            return HuongCuocGoi.NOI_MANG;
        }
        return r < NGUONG_NGOAI_MANG ? HuongCuocGoi.NGOAI_MANG : HuongCuocGoi.QUOC_TE;
    }

    /**
     * Thời lượng cuộc gọi, phân bố LỆCH VỀ NGẮN cho giống thực tế:
     * phần lớn cuộc gọi chỉ vài chục giây tới vài phút, số cuộc gọi rất dài rất hiếm.
     * <ul>
     *   <li>80% nằm trong 30–180 giây</li>
     *   <li>5% là cuộc rất ngắn 5–29 giây (gọi nhầm, máy bận)</li>
     *   <li>15% còn lại rải tới 1800 giây</li>
     * </ul>
     */
    private int sinhThoiLuongGoi(Random ngauNhien) {
        int r = ngauNhien.nextInt(100);
        if (r < 80) {
            return 30 + ngauNhien.nextInt(151);
        }
        if (r < 85) {
            return 5 + ngauNhien.nextInt(25);
        }
        return 181 + ngauNhien.nextInt(1620);
    }

    /**
     * SMS luôn là 1 tin.
     * DATA sinh 1024–512000 <b>KB</b> (tức 1 MB đến 500 MB mỗi phiên).
     *
     * <p><b>Lưu ý cho Phase 4:</b> đơn vị lưu của cột {@code so_luong} với DATA là KB.
     * Bảng giá DATA hiện có {@code block_giay = 1} và đơn giá 25đ — nếu engine tính cước
     * hiểu block là 1 MB thì phải chia {@code so_luong} cho 1024 trước khi nhân đơn giá.</p>
     */
    private int sinhSoLuong(LoaiDichVu dichVu, Random ngauNhien) {
        if (dichVu == LoaiDichVu.SMS) {
            return 1;
        }
        if (dichVu == LoaiDichVu.DATA) {
            return 1024 + ngauNhien.nextInt(512000 - 1024 + 1);
        }
        return 0;
    }

    /**
     * Số bị gọi tuỳ theo hướng:
     * <ul>
     *   <li>NỘI MẠNG: lấy ngẫu nhiên số của một thuê bao khác trong hệ thống</li>
     *   <li>NGOẠI MẠNG: sinh số 10 chữ số hợp lệ (đầu số 03/05/07/08/09)</li>
     *   <li>QUỐC TẾ: dạng +xx theo một số mã quốc gia thông dụng</li>
     * </ul>
     */
    private String sinhSoBiGoi(HuongCuocGoi huong, List<String> soNoiMang, String soGoiDi,
                               Random ngauNhien) {
        if (huong == HuongCuocGoi.NOI_MANG) {
            if (soNoiMang.size() <= 1) {
                return soNoiMang.isEmpty() ? null : soNoiMang.get(0);
            }
            String so;
            do {
                so = soNoiMang.get(ngauNhien.nextInt(soNoiMang.size()));
            } while (so.equals(soGoiDi)); // không tự gọi chính mình
            return so;
        }
        if (huong == HuongCuocGoi.NGOAI_MANG) {
            int[] dauSo = {3, 5, 7, 8, 9};
            StringBuilder sb = new StringBuilder("0");
            sb.append(dauSo[ngauNhien.nextInt(dauSo.length)]);
            for (int i = 0; i < 8; i++) {
                sb.append(ngauNhien.nextInt(10));
            }
            return sb.toString();
        }
        // QUỐC TẾ
        String[] maQuocGia = {"+1", "+44", "+61", "+65", "+66", "+81", "+82", "+86"};
        StringBuilder sb = new StringBuilder(maQuocGia[ngauNhien.nextInt(maQuocGia.length)]);
        for (int i = 0; i < 9; i++) {
            sb.append(ngauNhien.nextInt(10));
        }
        return sb.toString();
    }
}
