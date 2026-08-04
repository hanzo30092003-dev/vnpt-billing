package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaRating;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.DangKyGoiCuocRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine định giá — tính {@code cuoc_phi} cho từng bản ghi CDR.
 *
 * <p><b>Phạm vi:</b> lớp này chỉ làm <i>rating</i> (định giá từng bản ghi), không làm
 * <i>billing</i> (lập hóa đơn) và chưa áp ưu đãi của gói cước. Vì vậy mọi bản ghi đều bị
 * tính tiền đầy đủ và cờ {@code mien_phi} giữ nguyên 0 — mục 4D mới trừ quỹ ưu đãi.</p>
 *
 * <p>Rating chạy cho <b>tất cả</b> thuê bao, kể cả trả trước: định giá và lập hóa đơn là
 * hai việc khác nhau. Lớp này tuyệt đối không động vào {@code thue_bao.so_du}.</p>
 *
 * <h2>Thuật toán cho một bản ghi</h2>
 * <pre>
 * 1. Tìm gói cước có hiệu lực TẠI THỜI ĐIỂM CDR qua dang_ky_goi_cuoc
 *    (lùi về thue_bao.goi_cuoc_id nếu không có, kèm log WARN)
 * 2. Tra đơn giá qua BangGiaLookup, theo NGÀY PHÁT SINH của CDR
 *    (chuỗi dự phòng bốn bước: giá riêng gói → giá chung, cao điểm → thường)
 * 3. Quy sản lượng thô về số block:
 *      THOAI → soBlock(thoi_luong_giay, block_giay)
 *      SMS   → soBlock(so_luong,        block_giay)
 *      DATA  → soBlock(kbSangMb(so_luong), block_giay)
 * 4. cuoc_phi = lamTronTien(soBlock × don_gia)
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RatingService {

    /** Kích thước lô ghi CSDL, cùng cỡ với bộ sinh CDR. */
    private static final int KICH_THUOC_LO = 500;

    /** Số bản ghi giữa hai lần ghi log tiến trình. */
    private static final int BUOC_GHI_LOG = 1000;

    private static final String SQL_DA_TINH = """
            UPDATE chi_tiet_su_dung
               SET cuoc_phi = ?, trang_thai_tinh_cuoc = 'DA_TINH', ky_cuoc_id = ?,
                   bang_gia_cuoc_id = ?
             WHERE id = ?
            """;

    /**
     * Bản ghi lỗi vẫn được gán {@code ky_cuoc_id}. Nếu để null thì chức năng hủy kết quả
     * tính cước (lọc theo {@code ky_cuoc_id}) sẽ không dọn được chúng, và chúng nằm lại ở
     * trạng thái LOI vĩnh viễn.
     */
    private static final String SQL_LOI = """
            UPDATE chi_tiet_su_dung
               SET cuoc_phi = NULL, trang_thai_tinh_cuoc = 'LOI', ky_cuoc_id = ?,
                   bang_gia_cuoc_id = NULL
             WHERE id = ?
            """;

    private static final String SQL_HUY = """
            UPDATE chi_tiet_su_dung
               SET trang_thai_tinh_cuoc = 'CHUA_TINH', cuoc_phi = NULL,
                   mien_phi = 0, ky_cuoc_id = NULL, bang_gia_cuoc_id = NULL
             WHERE ky_cuoc_id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ChiTietSuDungRepository chiTietSuDungRepository;
    private final DangKyGoiCuocRepository dangKyGoiCuocRepository;
    private final KyCuocRepository kyCuocRepository;
    private final HoaDonRepository hoaDonRepository;
    private final BangGiaLookup bangGiaLookup;
    private final NhatKyService nhatKyService;

    /**
     * Dữ liệu tra cứu nạp sẵn cho một lần chạy.
     *
     * <p>Chụp một lần rồi dùng lại cho mọi bản ghi: bảng giá 10 dòng và lịch sử đăng ký
     * gói 80 dòng, so với 5017 bản ghi CDR. Không có ảnh chụp này thì mỗi bản ghi sẽ sinh
     * ít nhất hai câu truy vấn.</p>
     *
     * <p>Danh sách đăng ký của mỗi thuê bao đã sắp xếp <b>ngày bắt đầu giảm dần</b>, nên
     * khi có nhiều bản ghi cùng phủ một ngày thì bản đăng ký muộn nhất thắng.</p>
     */
    public record NguonTraCuu(BangGiaLookup.Bang bangGia,
                              Map<Long, List<DangKyGoiCuoc>> dangKyTheoThueBao) {
    }

    // =================================================================
    // A. ĐỊNH GIÁ MỘT BẢN GHI
    // =================================================================

    /**
     * Cước của một bản ghi CDR.
     *
     * <p><b>Không ghi xuống CSDL</b> — chỉ trả về số tiền. Việc ghi do {@link #ratingKy}
     * làm theo lô. Tách như vậy để hàm tính toán kiểm thử được mà không cần CSDL.</p>
     *
     * <p>Mỗi lần gọi tự nạp ảnh chụp tra cứu, nên dùng cho một bản ghi lẻ. Khi tính cả kỳ
     * phải dùng {@link #ratingKy} để ảnh chụp được tái sử dụng.</p>
     *
     * @throws NghiepVuException nếu không tra được đơn giá cho tổ hợp của bản ghi
     */
    @Transactional(readOnly = true)
    public BigDecimal ratingCdr(ChiTietSuDung cdr) {
        return tinhCuoc(cdr, napNguonTraCuu()).cuocPhi();
    }

    /**
     * Kết quả định giá một bản ghi: số tiền và <b>dòng bảng giá đã áp dụng</b>.
     *
     * <p>Trả về cả mã dòng bảng giá để ghi xuống {@code chi_tiet_su_dung.bang_gia_cuoc_id}
     * — ảnh chụp thật tại thời điểm định giá, không phải thứ suy ngược lại về sau.</p>
     */
    public record KetQuaDinhGia(BigDecimal cuocPhi, Long bangGiaCuocId) {
    }

    private KetQuaDinhGia tinhCuoc(ChiTietSuDung cdr, NguonTraCuu nguon) {
        LocalDate ngayPhatSinh = cdr.getThoiGianBatDau().toLocalDate();
        Long goiCuocId = timGoiCuocTaiThoiDiem(cdr, ngayPhatSinh, nguon);

        BangGiaCuoc gia = nguon.bangGia().traGia(
                goiCuocId,
                cdr.getLoaiDichVu(),
                cdr.getHuong(),
                Boolean.TRUE.equals(cdr.getGioCaoDiem()),
                ngayPhatSinh);

        long soBlock = DonViCuoc.soBlock(sanLuongTho(cdr), gia.getBlockGiay());
        BigDecimal cuocPhi = ThamSoTinhCuoc.lamTronTien(
                BigDecimal.valueOf(soBlock).multiply(gia.getDonGia()));
        return new KetQuaDinhGia(cuocPhi, gia.getId());
    }

    /**
     * Sản lượng thô đưa vào chia block, theo từng loại dịch vụ.
     *
     * <p>DATA phải quy KB sang MB <b>trước</b> khi chia block, vì đơn giá 25 đ đặt theo MB
     * còn cột {@code so_luong} lưu theo KB. Quên bước này là sai 1024 lần — xem
     * {@link DonViCuoc}.</p>
     */
    private static long sanLuongTho(ChiTietSuDung cdr) {
        return switch (cdr.getLoaiDichVu()) {
            case THOAI -> khongNull(cdr.getThoiLuongGiay());
            case SMS -> khongNull(cdr.getSoLuong());
            case DATA -> DonViCuoc.kbSangMb(khongNull(cdr.getSoLuong()));
        };
    }

    private static long khongNull(Integer giaTri) {
        return giaTri == null ? 0L : giaTri;
    }

    /**
     * Gói cước của thuê bao <b>tại thời điểm phát sinh CDR</b>.
     *
     * <p>Phải tra theo {@code dang_ky_goi_cuoc} chứ không lấy {@code thue_bao.goi_cuoc_id}:
     * cột đó chỉ phản ánh gói <i>hiện hành</i>, nên nếu khách đổi gói sau khi kỳ kết thúc
     * thì cả kỳ cũ sẽ bị tính theo gói mới.</p>
     *
     * <p>Đường lùi về gói hiện hành có ghi log WARN. Nó chỉ xảy ra khi dữ liệu thiếu bản
     * ghi đăng ký — im lặng lùi thì sai lệch không bao giờ lộ ra.</p>
     */
    private Long timGoiCuocTaiThoiDiem(ChiTietSuDung cdr, LocalDate ngay, NguonTraCuu nguon) {
        List<DangKyGoiCuoc> lichSu = nguon.dangKyTheoThueBao()
                .getOrDefault(cdr.getThueBao().getId(), List.of());

        for (DangKyGoiCuoc dangKy : lichSu) {
            boolean daBatDau = !dangKy.getNgayBatDau().isAfter(ngay);
            boolean chuaKetThuc = dangKy.getNgayKetThuc() == null
                    || !dangKy.getNgayKetThuc().isBefore(ngay);
            if (daBatDau && chuaKetThuc) {
                return dangKy.getGoiCuoc().getId();
            }
        }

        Long goiHienHanh = cdr.getThueBao().getGoiCuoc().getId();
        log.warn("CDR id={} của thuê bao {} ngày {} không khớp bản ghi dang_ky_goi_cuoc nào — "
                        + "lùi về gói hiện hành id={}. Kiểm tra lại lịch sử đăng ký gói.",
                cdr.getId(), cdr.getSoThueBao(), ngay, goiHienHanh);
        return goiHienHanh;
    }

    /** Chụp bảng giá và lịch sử đăng ký gói cho một lần chạy. */
    private NguonTraCuu napNguonTraCuu() {
        Map<Long, List<DangKyGoiCuoc>> theoThueBao = new HashMap<>();
        for (DangKyGoiCuoc dangKy : dangKyGoiCuocRepository.timTatCaKemGoiVaThueBao()) {
            theoThueBao.computeIfAbsent(dangKy.getThueBao().getId(), k -> new ArrayList<>())
                    .add(dangKy);
        }
        theoThueBao.values().forEach(ds ->
                ds.sort(Comparator.comparing(DangKyGoiCuoc::getNgayBatDau).reversed()));
        return new NguonTraCuu(bangGiaLookup.nap(), theoThueBao);
    }

    // =================================================================
    // B. ĐỊNH GIÁ CẢ MỘT KỲ
    // =================================================================

    /**
     * Tính cước toàn bộ CDR chưa tính của một kỳ.
     *
     * <p>Bản ghi lỗi không làm dừng cả kỳ: nó được đánh {@code LOI}, ghi log kèm mã số và
     * lý do, rồi engine đi tiếp. Một dòng bảng giá thiếu không được phép chặn việc tính
     * cước của mấy nghìn bản ghi còn lại.</p>
     */
    @Transactional
    public KetQuaRating ratingKy(KyCuoc ky) {
        long batDau = System.currentTimeMillis();
        kiemTraTruocKhiChay(ky);

        ky.setTrangThai(TrangThaiKyCuoc.DANG_TINH);
        kyCuocRepository.saveAndFlush(ky);

        NguonTraCuu nguon = napNguonTraCuu();

        // Khoảng NỬA MỞ [đầu kỳ 00:00, đầu kỳ sau 00:00) — xem javadoc của timCanTinhCuoc
        LocalDateTime tuLuc = ky.getNgayBatDau().atStartOfDay();
        LocalDateTime denLuc = ky.getNgayKetThuc().plusDays(1).atStartOfDay();

        List<ChiTietSuDung> danhSach = chiTietSuDungRepository.timCanTinhCuoc(tuLuc, denLuc);
        log.info("Bắt đầu tính cước kỳ {}/{}: {} bản ghi cần xử lý, {} dòng bảng giá",
                ky.getThang(), ky.getNam(), danhSach.size(), nguon.bangGia().soDong());

        KetQuaRating ketQua = new KetQuaRating();
        List<Object[]> loThanhCong = new ArrayList<>(KICH_THUOC_LO);
        List<Long> loLoi = new ArrayList<>(KICH_THUOC_LO);

        for (int i = 0; i < danhSach.size(); i++) {
            ChiTietSuDung cdr = danhSach.get(i);
            try {
                KetQuaDinhGia dinhGia = tinhCuoc(cdr, nguon);
                loThanhCong.add(new Object[]{dinhGia.cuocPhi(), ky.getId(),
                        dinhGia.bangGiaCuocId(), cdr.getId()});
                ketQua.setTongCuoc(ketQua.getTongCuoc().add(dinhGia.cuocPhi()));
                ketQua.setSoThanhCong(ketQua.getSoThanhCong() + 1);
            } catch (RuntimeException ex) {
                loLoi.add(cdr.getId());
                ketQua.setSoLoi(ketQua.getSoLoi() + 1);
                log.error("Không tính được cước CDR id={} ({} / {} lúc {}): {}",
                        cdr.getId(), cdr.getLoaiDichVu(), cdr.getHuong(),
                        cdr.getThoiGianBatDau(), ex.getMessage());
            }

            if (loThanhCong.size() >= KICH_THUOC_LO) {
                ghiLoThanhCong(loThanhCong);
            }
            if (loLoi.size() >= KICH_THUOC_LO) {
                ghiLoLoi(loLoi, ky.getId());
            }
            if ((i + 1) % BUOC_GHI_LOG == 0) {
                log.info("Tính cước kỳ {}/{}: đã xử lý {}/{} bản ghi",
                        ky.getThang(), ky.getNam(), i + 1, danhSach.size());
            }
        }
        ghiLoThanhCong(loThanhCong);
        ghiLoLoi(loLoi, ky.getId());

        ketQua.setThoiGianMs(System.currentTimeMillis() - batDau);
        ketThucKy(ky, ketQua);
        return ketQua;
    }

    /**
     * Trả kỳ về {@code MO} sau khi chạy xong — <b>chạy xong không đồng nghĩa với chốt kỳ</b>.
     * Chốt kỳ là thao tác riêng, có chủ đích, thuộc mục 4E.
     */
    private void ketThucKy(KyCuoc ky, KetQuaRating ketQua) {
        // Đếm lại từ CSDL thay vì lấy soThanhCong: chạy lần hai chỉ xử lý phần còn sót,
        // nếu ghi thẳng soThanhCong thì cột này sẽ tụt xuống con số của riêng lần chạy đó.
        long daTinh = chiTietSuDungRepository.countByKyCuocIdAndTrangThaiTinhCuoc(
                ky.getId(), TrangThaiTinhCuoc.DA_TINH);

        ky.setTrangThai(TrangThaiKyCuoc.MO);
        ky.setSoCdrXuLy((int) daTinh);
        kyCuocRepository.save(ky);

        log.info("Xong tính cước kỳ {}/{}: {} thành công, {} lỗi, tổng cước {}, mất {} ms",
                ky.getThang(), ky.getNam(), ketQua.getSoThanhCong(), ketQua.getSoLoi(),
                ketQua.getTongCuoc(), ketQua.getThoiGianMs());

        nhatKyService.ghiNhatKy("TINH_CUOC", "KY_CUOC", ky.getId(),
                "Tính cước kỳ " + ky.getThang() + "/" + ky.getNam() + ": "
                        + ketQua.getSoThanhCong() + " bản ghi thành công, "
                        + ketQua.getSoLoi() + " lỗi, tổng cước gộp " + ketQua.getTongCuoc()
                        + " đ, mất " + ketQua.getThoiGianMs() + " ms");
    }

    /**
     * Chặn ở tầng nghiệp vụ trước khi chạy.
     *
     * <p>Ràng buộc {@code UNIQUE(thue_bao_id, ky_cuoc_id)} của bảng hóa đơn chỉ chặn được
     * ở mục 4C. Ở đây phải tự kiểm, để người dùng nhận thông báo tiếng Việt rõ nghĩa thay
     * vì một ngoại lệ kỹ thuật.</p>
     */
    private void kiemTraTruocKhiChay(KyCuoc ky) {
        if (ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã chốt, không thể tính lại");
        }
        if (ky.getTrangThai() == TrangThaiKyCuoc.DANG_TINH) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đang ở trạng thái Đang tính. Nếu lần chạy trước bị dừng giữa chừng, "
                    + "dùng chức năng \"Gỡ kỳ bị kẹt\" để đưa kỳ về trạng thái Mở rồi chạy lại.");
        }
    }

    private void ghiLoThanhCong(List<Object[]> lo) {
        if (lo.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(SQL_DA_TINH, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] dong = lo.get(i);
                ps.setBigDecimal(1, (BigDecimal) dong[0]);
                ps.setLong(2, (Long) dong[1]);
                ps.setLong(3, (Long) dong[2]);
                ps.setLong(4, (Long) dong[3]);
            }

            @Override
            public int getBatchSize() {
                return lo.size();
            }
        });
        lo.clear();
    }

    private void ghiLoLoi(List<Long> lo, Long kyCuocId) {
        if (lo.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate(SQL_LOI, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, kyCuocId);
                ps.setLong(2, lo.get(i));
            }

            @Override
            public int getBatchSize() {
                return lo.size();
            }
        });
        lo.clear();
    }

    // =================================================================
    // C. HỦY KẾT QUẢ TÍNH CƯỚC
    // =================================================================

    /**
     * Đưa toàn bộ CDR của một kỳ về {@code CHUA_TINH} để tính lại từ đầu.
     *
     * <p>Đây là đường duy nhất để tính lại một kỳ, vì engine cố ý bỏ qua bản ghi đã
     * {@code DA_TINH}.</p>
     *
     * @return số bản ghi CDR đã đưa về trạng thái chưa tính
     */
    @Transactional
    public int huyRatingKy(KyCuoc ky) {
        if (ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã chốt, không thể hủy kết quả tính cước");
        }
        long soHoaDon = hoaDonRepository.countByKyCuocId(ky.getId());
        if (soHoaDon > 0) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã phát hành " + soHoaDon + " hóa đơn. Phải xóa các hóa đơn đó trước "
                    + "khi hủy kết quả tính cước, nếu không hóa đơn sẽ không còn khớp với "
                    + "chi tiết sử dụng.");
        }

        int soBanGhi = jdbcTemplate.update(SQL_HUY, ky.getId());

        ky.setTrangThai(TrangThaiKyCuoc.MO);
        ky.setSoCdrXuLy(0);
        kyCuocRepository.save(ky);

        log.info("Đã hủy kết quả tính cước kỳ {}/{}: {} bản ghi CDR về CHUA_TINH",
                ky.getThang(), ky.getNam(), soBanGhi);
        nhatKyService.ghiNhatKy("HUY_TINH_CUOC", "KY_CUOC", ky.getId(),
                "Hủy kết quả tính cước kỳ " + ky.getThang() + "/" + ky.getNam() + ": "
                        + soBanGhi + " bản ghi CDR trở về trạng thái Chưa tính");
        return soBanGhi;
    }

    // =================================================================
    // D. GỠ KỲ BỊ KẸT Ở TRẠNG THÁI ĐANG TÍNH
    // =================================================================

    /**
     * Đưa một kỳ đang kẹt ở {@code DANG_TINH} về {@code MO}.
     *
     * <p><b>Vì sao cần chức năng này:</b> {@link #ratingKy} đặt kỳ sang {@code DANG_TINH}
     * trước khi chạy và chỉ trả về {@code MO} khi xong. Nếu tiến trình bị giết giữa chừng
     * — mất điện, kill process, hết bộ nhớ — kỳ nằm lại ở {@code DANG_TINH} vĩnh viễn, và
     * chính {@link #kiemTraTruocKhiChay} sẽ từ chối mọi lần chạy sau. Không có đường gỡ thì
     * cách duy nhất là sửa tay bằng lệnh UPDATE trong CSDL.</p>
     *
     * <p><b>Chỉ đổi trạng thái kỳ, không đụng tới CDR.</b> Bản ghi đã kịp tính ở lần chạy
     * dở giữ nguyên {@code DA_TINH}; chạy lại sẽ xử lý nốt phần còn sót. Muốn làm sạch hẳn
     * thì gọi tiếp {@link #huyRatingKy}.</p>
     */
    @Transactional
    public void goKyBiKet(Long kyCuocId) {
        KyCuoc ky = kyCuocRepository.findById(kyCuocId)
                .orElseThrow(() -> new NghiepVuException(
                        "Không tìm thấy kỳ cước có mã số " + kyCuocId));

        if (ky.getTrangThai() != TrangThaiKyCuoc.DANG_TINH) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " không bị kẹt (đang ở trạng thái " + nhan(ky.getTrangThai())
                    + "). Chức năng này chỉ dùng cho kỳ kẹt ở trạng thái Đang tính do lần "
                    + "chạy trước bị dừng đột ngột.");
        }

        ky.setTrangThai(TrangThaiKyCuoc.MO);
        kyCuocRepository.save(ky);

        log.warn("Gỡ kỳ kẹt thủ công: kỳ {}/{} từ DANG_TINH về MO",
                ky.getThang(), ky.getNam());
        nhatKyService.ghiNhatKy("GO_KY_BI_KET", "KY_CUOC", ky.getId(),
                "Gỡ kỳ kẹt thủ công: đưa kỳ " + ky.getThang() + "/" + ky.getNam()
                        + " từ Đang tính về Mở. Dữ liệu CDR giữ nguyên — cần chạy lại tính "
                        + "cước để xử lý nốt phần còn sót, hoặc hủy kết quả để tính lại từ đầu.");
    }

    private static String nhan(TrangThaiKyCuoc trangThai) {
        return switch (trangThai) {
            case MO -> "Mở";
            case DANG_TINH -> "Đang tính";
            case DA_CHOT -> "Đã chốt";
        };
    }
}
