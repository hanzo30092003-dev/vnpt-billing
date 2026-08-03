package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaImportCdr;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Nhập bản ghi CDR từ file CSV.
 *
 * <p>Định dạng cột:
 * {@code so_thue_bao,so_bi_goi,loai_dich_vu,huong,thoi_gian_bat_dau,thoi_luong_giay,so_luong}</p>
 *
 * <p>Dòng sai bị bỏ qua và ghi lại lý do, KHÔNG chặn cả file — một file vài nghìn dòng
 * mà hỏng vì một dòng lỗi thì người dùng không có cách nào nhập được phần còn lại.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CdrImportService {

    private static final DateTimeFormatter DINH_DANG_THOI_GIAN =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int SO_COT = 7;
    private static final int KICH_THUOC_LO = 500;
    private static final int DO_DAI_TOI_DA_HIEN_THI = 120;

    private static final String SQL_CHEN = """
            INSERT INTO chi_tiet_su_dung
                (thue_bao_id, so_thue_bao, so_bi_goi, loai_dich_vu, huong,
                 thoi_gian_bat_dau, thoi_luong_giay, so_luong, gio_cao_diem,
                 cuoc_phi, mien_phi, trang_thai_tinh_cuoc, ky_cuoc_id, nguon)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, 0, 'CHUA_TINH', NULL, 'IMPORT_CSV')
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ThueBaoRepository thueBaoRepository;
    private final NhatKyService nhatKyService;

    /** Một dòng đã hợp lệ, chờ ghi xuống CSDL. */
    private record DongHopLe(Long thueBaoId, String soThueBao, String soBiGoi,
                             String loaiDichVu, String huong, LocalDateTime thoiGian,
                             int thoiLuongGiay, int soLuong, boolean gioCaoDiem) {
    }

    /**
     * Đọc và nhập file.
     *
     * <p>Toàn bộ chạy trong một giao dịch: hoặc tất cả dòng hợp lệ được ghi, hoặc không
     * dòng nào được ghi nếu có sự cố kỹ thuật giữa chừng.</p>
     */
    @Transactional
    public KetQuaImportCdr nhap(MultipartFile file) {
        long batDau = System.currentTimeMillis();

        if (file == null || file.isEmpty()) {
            throw new NghiepVuException("Vui lòng chọn file CSV cần nhập");
        }
        String tenFile = file.getOriginalFilename();
        if (tenFile != null && !tenFile.toLowerCase().endsWith(".csv")) {
            throw new NghiepVuException("Chỉ chấp nhận file có phần mở rộng .csv");
        }

        KetQuaImportCdr ketQua = new KetQuaImportCdr();
        ketQua.setTenFile(tenFile);

        // Nạp sẵn toàn bộ thuê bao vào Map để tra O(1); nếu truy vấn CSDL cho từng
        // dòng thì file 5000 dòng sẽ sinh 5000 câu SELECT.
        Map<String, ThueBao> theoSo = new HashMap<>();
        for (ThueBao tb : thueBaoRepository.findAll()) {
            theoSo.put(tb.getSoThueBao(), tb);
        }

        List<DongHopLe> lo = new ArrayList<>(KICH_THUOC_LO);
        int soDong = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String dong;
            while ((dong = reader.readLine()) != null) {
                soDong++;

                // Bỏ qua dòng tiêu đề và dòng trống
                if (dong.isBlank()) {
                    continue;
                }
                if (soDong == 1 && dong.toLowerCase().startsWith("so_thue_bao")) {
                    continue;
                }
                ketQua.setTongDong(ketQua.getTongDong() + 1);

                try {
                    lo.add(phanTichDong(dong, theoSo));
                    ketQua.setSoThanhCong(ketQua.getSoThanhCong() + 1);
                } catch (NghiepVuException ex) {
                    ketQua.getDanhSachLoi().add(new KetQuaImportCdr.DongLoi(
                            soDong, catNgan(dong), ex.getMessage()));
                }

                if (lo.size() >= KICH_THUOC_LO) {
                    ghiLo(lo);
                    lo.clear();
                }
            }
        } catch (IOException e) {
            throw new NghiepVuException("Không đọc được nội dung file: " + e.getMessage());
        }

        if (!lo.isEmpty()) {
            ghiLo(lo);
        }

        ketQua.setThoiGianMs(System.currentTimeMillis() - batDau);
        log.info("Nhập CDR từ {}: {} thành công, {} lỗi",
                tenFile, ketQua.getSoThanhCong(), ketQua.getSoLoi());

        nhatKyService.ghiNhatKy("IMPORT_CDR", "CHI_TIET_SU_DUNG", null,
                "Nhập CDR từ file " + tenFile + ": " + ketQua.getSoThanhCong()
                        + " dòng thành công, " + ketQua.getSoLoi() + " dòng lỗi");

        return ketQua;
    }

    /**
     * Kiểm tra và chuyển một dòng CSV thành bản ghi.
     *
     * @throws NghiepVuException nếu dòng không hợp lệ, thông điệp là lý do hiển thị
     *                           cho người dùng
     */
    private DongHopLe phanTichDong(String dong, Map<String, ThueBao> theoSo) {
        String[] cot = dong.split(",", -1);
        if (cot.length != SO_COT) {
            throw new NghiepVuException("Dòng phải có đúng " + SO_COT
                    + " cột, đang có " + cot.length + " cột");
        }

        String soThueBao = cot[0].trim();
        String soBiGoi = cot[1].trim();
        String chuoiDichVu = cot[2].trim();
        String chuoiHuong = cot[3].trim();
        String chuoiThoiGian = cot[4].trim();
        String chuoiThoiLuong = cot[5].trim();
        String chuoiSoLuong = cot[6].trim();

        // 1. Thuê bao phải tồn tại
        ThueBao thueBao = theoSo.get(soThueBao);
        if (thueBao == null) {
            throw new NghiepVuException("Không tìm thấy thuê bao " + soThueBao + " trong hệ thống");
        }

        // 2. Không nhận CDR của thuê bao đã khoá hai chiều hoặc đã thanh lý
        if (thueBao.getTrangThai() == TrangThaiThueBao.TAM_NGUNG_2C
                || thueBao.getTrangThai() == TrangThaiThueBao.DA_THANH_LY) {
            throw new NghiepVuException("Thuê bao " + soThueBao + " đang ở trạng thái "
                    + thueBao.getTrangThai().getNhan() + ", không nhận bản ghi sử dụng");
        }

        // 3. Enum loại dịch vụ
        LoaiDichVu loaiDichVu;
        try {
            loaiDichVu = LoaiDichVu.valueOf(chuoiDichVu);
        } catch (IllegalArgumentException e) {
            throw new NghiepVuException("Loại dịch vụ '" + chuoiDichVu
                    + "' không hợp lệ, chỉ chấp nhận THOAI, SMS hoặc DATA");
        }

        // 4. Enum hướng
        HuongCuocGoi huong;
        try {
            huong = HuongCuocGoi.valueOf(chuoiHuong);
        } catch (IllegalArgumentException e) {
            throw new NghiepVuException("Hướng '" + chuoiHuong
                    + "' không hợp lệ, chỉ chấp nhận NOI_MANG, NGOAI_MANG hoặc QUOC_TE");
        }

        // 5. Tổ hợp (dịch vụ, hướng) phải hợp lệ.
        //    Dùng CHUNG quy tắc với bộ sinh CDR, không viết lại logic ở đây.
        if (!QuyTacToHopDichVu.hopLe(loaiDichVu, huong)) {
            throw new NghiepVuException(QuyTacToHopDichVu.thongBaoKhongHopLe(loaiDichVu, huong));
        }

        // 6. Thời gian đúng định dạng
        LocalDateTime thoiGian;
        try {
            thoiGian = LocalDateTime.parse(chuoiThoiGian, DINH_DANG_THOI_GIAN);
        } catch (DateTimeParseException e) {
            throw new NghiepVuException("Thời gian '" + chuoiThoiGian
                    + "' sai định dạng, phải là yyyy-MM-dd HH:mm:ss");
        }

        // 7. Không nhận bản ghi phát sinh trước ngày kích hoạt thuê bao
        if (thoiGian.toLocalDate().isBefore(thueBao.getNgayKichHoat())) {
            throw new NghiepVuException("Thời gian phát sinh sớm hơn ngày kích hoạt thuê bao ("
                    + thueBao.getNgayKichHoat() + ")");
        }

        // 8. Các trường số
        int thoiLuong = docSoNguyen(chuoiThoiLuong, "thoi_luong_giay");
        int soLuong = docSoNguyen(chuoiSoLuong, "so_luong");
        if (thoiLuong < 0 || soLuong < 0) {
            throw new NghiepVuException("thoi_luong_giay và so_luong không được âm");
        }

        // 9. Cuộc gọi bắt buộc phải có thời lượng
        if (loaiDichVu == LoaiDichVu.THOAI && thoiLuong <= 0) {
            throw new NghiepVuException("Bản ghi THOAI phải có thoi_luong_giay lớn hơn 0");
        }

        return new DongHopLe(
                thueBao.getId(),
                soThueBao,
                soBiGoi.isEmpty() ? null : soBiGoi,
                loaiDichVu.name(),
                huong.name(),
                thoiGian,
                thoiLuong,
                soLuong,
                // Dùng CHUNG quy tắc với bộ sinh CDR, không viết lại logic ở đây
                QuyTacGioCaoDiem.apDung(loaiDichVu, thoiGian));
    }

    private int docSoNguyen(String giaTri, String tenCot) {
        if (giaTri == null || giaTri.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(giaTri);
        } catch (NumberFormatException e) {
            throw new NghiepVuException("Cột " + tenCot + " phải là số nguyên, đang là '" + giaTri + "'");
        }
    }

    private String catNgan(String dong) {
        return dong.length() <= DO_DAI_TOI_DA_HIEN_THI
                ? dong : dong.substring(0, DO_DAI_TOI_DA_HIEN_THI) + "...";
    }

    /** Ghi một lô bằng batchUpdate, cùng cách tối ưu như bộ sinh CDR. */
    private void ghiLo(List<DongHopLe> lo) {
        jdbcTemplate.batchUpdate(SQL_CHEN, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                DongHopLe d = lo.get(i);
                ps.setLong(1, d.thueBaoId());
                ps.setString(2, d.soThueBao());
                ps.setString(3, d.soBiGoi());
                ps.setString(4, d.loaiDichVu());
                ps.setString(5, d.huong());
                ps.setTimestamp(6, Timestamp.valueOf(d.thoiGian()));
                ps.setInt(7, d.thoiLuongGiay());
                ps.setInt(8, d.soLuong());
                ps.setInt(9, d.gioCaoDiem() ? 1 : 0);
            }

            @Override
            public int getBatchSize() {
                return lo.size();
            }
        });
    }
}
