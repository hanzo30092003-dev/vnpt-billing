package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaBilling;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.ChiTietHoaDon;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.entity.GiamTru;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiGiamTru;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.DangKyGoiCuocRepository;
import com.hanzo.billing.repository.GiamTruRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.NhatKyService;
import com.hanzo.billing.service.SinhMaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Engine lập hóa đơn — gom CDR đã định giá thành hóa đơn tháng.
 *
 * <p><b>Phạm vi Phase 4C:</b> dựng đúng bộ khung hóa đơn và giữ bất biến cộng dồn.
 * <b>Chưa áp ưu đãi gói cước</b> — đó là mục 4D. Vì vậy mọi sản lượng đều bị thu tiền đầy
 * đủ và tổng doanh thu ở đây là mức <b>trần</b>.</p>
 *
 * <p><b>Chỉ lập hóa đơn cho thuê bao trả sau</b> (quyết định 5.4). Thuê bao trả trước đã có
 * {@code cuoc_phi} trên từng CDR nhưng không có hóa đơn tháng — cước của họ trừ thẳng vào
 * số dư, thuộc Phase 5.</p>
 *
 * <h2>Bất biến quan trọng nhất của lớp này</h2>
 * <p>{@code SUM(chi_tiet_su_dung.cuoc_phi)} theo từng dịch vụ phải khớp <b>tuyệt đối</b>
 * với các cột {@code cuoc_thoai} / {@code cuoc_sms} / {@code cuoc_data} trên hóa đơn. Muốn
 * vậy thì tầng này <b>chỉ được cộng dồn, tuyệt đối không làm tròn lại</b>: mục 4B đã làm
 * tròn ở tầng CDR và đó là tầng làm tròn duy nhất. Làm tròn thêm một lần nữa ở đây sẽ sinh
 * ra lệch vài đồng mỗi hóa đơn, loại lệch gần như không thể truy nguyên.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    /** Kích thước lô ghi CSDL khi áp ưu đãi, cùng cỡ với các bộ ghi hàng loạt khác. */
    private static final int KICH_THUOC_LO = 500;

    private static final String SQL_AP_UU_DAI = """
            UPDATE chi_tiet_su_dung SET mien_phi = ?, cuoc_phi = ? WHERE id = ?
            """;

    private final JdbcTemplate jdbcTemplate;
    private final BangGiaLookup bangGiaLookup;
    private final HoaDonRepository hoaDonRepository;
    private final ChiTietSuDungRepository chiTietSuDungRepository;
    private final DangKyGoiCuocRepository dangKyGoiCuocRepository;
    private final GiamTruRepository giamTruRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final ThueBaoRepository thueBaoRepository;
    private final KyCuocRepository kyCuocRepository;
    private final SinhMaService sinhMaService;
    private final NhatKyService nhatKyService;

    /** Tổng hợp sản lượng và cước của một thuê bao cho một loại dịch vụ. */
    public record TongHopDichVu(BigDecimal tongCuoc, long tongThoiLuongGiay,
                                long tongSoLuong, long soBanGhi) {

        static TongHopDichVu rong() {
            return new TongHopDichVu(BigDecimal.ZERO, 0, 0, 0);
        }
    }

    /** Dữ liệu tra cứu nạp sẵn cho một lần chạy lập hóa đơn cả kỳ. */
    public record NguonBilling(Map<Long, List<DangKyGoiCuoc>> dangKyTheoThueBao,
                               Map<Long, Map<LoaiDichVu, TongHopDichVu>> cuocTheoThueBao,
                               Map<Long, List<GiamTru>> giamTruTheoThueBao) {
    }

    // =================================================================
    // A. LẬP HÓA ĐƠN CHO MỘT THUÊ BAO
    // =================================================================

    /**
     * Lập hóa đơn cho một thuê bao trong một kỳ.
     *
     * @return hóa đơn vừa lập, hoặc {@code null} nếu thuê bao đã có hóa đơn của kỳ hoặc
     *         không thuộc diện lập hóa đơn
     */
    @Transactional
    public HoaDon tinhCuocThueBao(ThueBao thueBao, KyCuoc ky) {
        return tinhCuocThueBao(thueBao, ky, napNguonBilling(ky));
    }

    private HoaDon tinhCuocThueBao(ThueBao thueBao, KyCuoc ky, NguonBilling nguon) {
        // BƯỚC 0 — chặn trùng ở tầng nghiệp vụ, TRƯỚC khi để UNIQUE của CSDL bắt.
        // Ràng buộc UNIQUE(thue_bao_id, ky_cuoc_id) vẫn là chốt chặn cuối, nhưng nếu để
        // nó bắt thì người dùng nhận về DataIntegrityViolationException khó hiểu.
        if (hoaDonRepository.existsByThueBaoIdAndKyCuocId(thueBao.getId(), ky.getId())) {
            log.debug("Thuê bao {} đã có hóa đơn kỳ {}/{}, bỏ qua",
                    thueBao.getSoThueBao(), ky.getThang(), ky.getNam());
            return null;
        }

        // BƯỚC 1 — cước thuê bao tháng, prorate theo số ngày thực dùng trong kỳ
        KhoangSuDung khoang = tinhKhoangSuDung(thueBao, ky);
        if (khoang == null) {
            return null;
        }
        GoiCuoc goi = timGoiCuocCuoiKy(thueBao, ky, nguon);
        BigDecimal cuocThueBao = tinhCuocThueBaoThang(goi, khoang);

        // BƯỚC 2 — gom cước từ CDR đã định giá. CHỈ CỘNG DỒN, KHÔNG LÀM TRÒN LẠI.
        Map<LoaiDichVu, TongHopDichVu> theoDichVu = nguon.cuocTheoThueBao()
                .getOrDefault(thueBao.getId(), Map.of());
        TongHopDichVu thoai = theoDichVu.getOrDefault(LoaiDichVu.THOAI, TongHopDichVu.rong());
        TongHopDichVu sms = theoDichVu.getOrDefault(LoaiDichVu.SMS, TongHopDichVu.rong());
        TongHopDichVu data = theoDichVu.getOrDefault(LoaiDichVu.DATA, TongHopDichVu.rong());

        // BƯỚC 3 — giảm trừ
        BigDecimal cuocKhac = BigDecimal.ZERO;
        BigDecimal truocGiamTru = cuocThueBao
                .add(thoai.tongCuoc()).add(sms.tongCuoc()).add(data.tongCuoc()).add(cuocKhac);
        List<GiamTru> dsGiamTru = nguon.giamTruTheoThueBao()
                .getOrDefault(thueBao.getId(), List.of());
        BigDecimal giamTru = tinhGiamTru(dsGiamTru, truocGiamTru);

        // BƯỚC 4 — tổng hợp
        BigDecimal tongTruocThue = truocGiamTru.subtract(giamTru);
        if (tongTruocThue.signum() < 0) {
            tongTruocThue = BigDecimal.ZERO;
        }
        BigDecimal thueVat = ThamSoTinhCuoc.lamTronTien(
                tongTruocThue.multiply(ThamSoTinhCuoc.THUE_SUAT_VAT));
        BigDecimal tongThanhToan = tongTruocThue.add(thueVat);

        // BƯỚC 5 — sinh hóa đơn
        HoaDon hoaDon = new HoaDon();
        hoaDon.setMaHoaDon(sinhMaService.sinhMaHoaDon(ky.getThang(), ky.getNam()));
        hoaDon.setThueBao(thueBao);
        hoaDon.setKhachHang(thueBao.getKhachHang());
        hoaDon.setKyCuoc(ky);
        hoaDon.setNgayLap(ky.getNgayKetThuc());
        hoaDon.setHanThanhToan(ky.getNgayKetThuc().plusMonths(1)
                .withDayOfMonth(ThamSoTinhCuoc.NGAY_HAN_THANH_TOAN_THANG_SAU));
        hoaDon.setCuocThueBao(cuocThueBao);
        hoaDon.setCuocThoai(thoai.tongCuoc());
        hoaDon.setCuocSms(sms.tongCuoc());
        hoaDon.setCuocData(data.tongCuoc());
        hoaDon.setCuocKhac(cuocKhac);
        hoaDon.setGiamTru(giamTru);
        hoaDon.setTongTruocThue(tongTruocThue);
        hoaDon.setThueVat(thueVat);
        hoaDon.setTongThanhToan(tongThanhToan);
        hoaDon.setDaThanhToan(BigDecimal.ZERO);
        hoaDon.setConNo(tongThanhToan);
        hoaDon.setTrangThai(TrangThaiHoaDon.CHUA_TT);

        themChiTiet(hoaDon, goi, khoang, cuocThueBao, thoai, sms, data, giamTru);

        HoaDon daLuu = hoaDonRepository.save(hoaDon);
        danhDauGiamTruDaApDung(dsGiamTru);
        return daLuu;
    }

    // -----------------------------------------------------------------
    // Cước thuê bao tháng và prorate
    // -----------------------------------------------------------------

    /** Khoảng ngày thuê bao thực sự sử dụng dịch vụ trong kỳ. */
    private record KhoangSuDung(LocalDate tuNgay, LocalDate denNgay,
                                long soNgaySuDung, long soNgayTrongKy) {

        boolean troVenTronKy() {
            return soNgaySuDung == soNgayTrongKy;
        }
    }

    /**
     * Số ngày thuê bao thực dùng trong kỳ.
     *
     * <p>Một công thức xử lý được mọi trường hợp thay vì phân nhánh theo trạng thái:</p>
     * <ul>
     *   <li>Kích hoạt trước kỳ, chưa hủy → trọn kỳ</li>
     *   <li>Kích hoạt giữa kỳ → từ ngày kích hoạt tới cuối kỳ, <b>tính cả ngày kích hoạt</b></li>
     *   <li>Hủy giữa kỳ → từ đầu kỳ tới ngày hủy</li>
     *   <li>Hủy trước kỳ, hoặc kích hoạt sau kỳ → khoảng rỗng, không lập hóa đơn</li>
     * </ul>
     *
     * @return null nếu thuê bao không dùng ngày nào trong kỳ
     */
    private KhoangSuDung tinhKhoangSuDung(ThueBao thueBao, KyCuoc ky) {
        long soNgayTrongKy =
                ChronoUnit.DAYS.between(ky.getNgayBatDau(), ky.getNgayKetThuc()) + 1;

        // Thuê bao đã thanh lý mà không ghi ngày hủy là dữ liệu hỏng. Không đoán mò:
        // coi như đã hủy từ trước và không lập hóa đơn, nhưng phải để lại cảnh báo.
        if (thueBao.getTrangThai() == TrangThaiThueBao.DA_THANH_LY
                && thueBao.getNgayHuy() == null) {
            log.warn("Thuê bao {} ở trạng thái Đã thanh lý nhưng không có ngày hủy — "
                            + "bỏ qua khi lập hóa đơn kỳ {}/{}. Kiểm tra lại dữ liệu.",
                    thueBao.getSoThueBao(), ky.getThang(), ky.getNam());
            return null;
        }

        LocalDate tuNgay = thueBao.getNgayKichHoat().isAfter(ky.getNgayBatDau())
                ? thueBao.getNgayKichHoat() : ky.getNgayBatDau();
        LocalDate denNgay = (thueBao.getNgayHuy() != null
                && thueBao.getNgayHuy().isBefore(ky.getNgayKetThuc()))
                ? thueBao.getNgayHuy() : ky.getNgayKetThuc();

        if (tuNgay.isAfter(denNgay)) {
            return null;
        }
        return new KhoangSuDung(tuNgay, denNgay,
                ChronoUnit.DAYS.between(tuNgay, denNgay) + 1, soNgayTrongKy);
    }

    /**
     * Cước thuê bao tháng, prorate theo tỷ lệ ngày.
     *
     * <p>Ưu đãi của gói <b>không</b> prorate theo (quyết định 5.6): chỉ cước thuê bao chia
     * theo ngày, còn quỹ ưu đãi giữ nguyên trọn tháng.</p>
     *
     * <p>Chia với scale trung gian rộng rồi mới làm tròn một lần ở cuối. Làm tròn ngay
     * trong phép chia sẽ tích lũy sai số khi nhân với số ngày.</p>
     */
    private BigDecimal tinhCuocThueBaoThang(GoiCuoc goi, KhoangSuDung khoang) {
        BigDecimal cuocThang = goi.getCuocThueBaoThang();
        if (khoang.troVenTronKy()) {
            return ThamSoTinhCuoc.lamTronTien(cuocThang);
        }
        BigDecimal theoTyLe = cuocThang
                .multiply(BigDecimal.valueOf(khoang.soNgaySuDung()))
                .divide(BigDecimal.valueOf(khoang.soNgayTrongKy()), 10, RoundingMode.HALF_UP);
        return ThamSoTinhCuoc.lamTronTien(theoTyLe);
    }

    /**
     * Gói cước dùng để tính cước thuê bao tháng: gói có hiệu lực tại <b>ngày cuối kỳ</b>.
     *
     * <p>Đây là xấp xỉ có chủ đích (quyết định 5.10): nếu khách đổi gói giữa kỳ thì cước
     * thuê bao cả kỳ tính theo gói cuối kỳ, không chia đôi kỳ theo từng gói. Cước sử dụng
     * thì không bị xấp xỉ này — mục 4B đã tra giá theo đúng gói tại thời điểm từng CDR.</p>
     */
    private GoiCuoc timGoiCuocCuoiKy(ThueBao thueBao, KyCuoc ky, NguonBilling nguon) {
        LocalDate ngay = ky.getNgayKetThuc();
        for (DangKyGoiCuoc dangKy : nguon.dangKyTheoThueBao()
                .getOrDefault(thueBao.getId(), List.of())) {
            boolean daBatDau = !dangKy.getNgayBatDau().isAfter(ngay);
            boolean chuaKetThuc = dangKy.getNgayKetThuc() == null
                    || !dangKy.getNgayKetThuc().isBefore(ngay);
            if (daBatDau && chuaKetThuc) {
                return dangKy.getGoiCuoc();
            }
        }
        log.warn("Thuê bao {} không có bản ghi dang_ky_goi_cuoc nào hiệu lực ngày {} — "
                        + "lùi về gói hiện hành. Kiểm tra lại lịch sử đăng ký gói.",
                thueBao.getSoThueBao(), ngay);
        return thueBao.getGoiCuoc();
    }

    // -----------------------------------------------------------------
    // Giảm trừ
    // -----------------------------------------------------------------

    /**
     * Tổng khoản giảm trừ.
     *
     * <p>Một khoản có thể khai theo số tiền tuyệt đối hoặc theo tỷ lệ phần trăm.
     * <b>Khi khai cả hai thì số tiền tuyệt đối thắng</b> — con số cụ thể là thứ người nhập
     * cân nhắc kỹ hơn, và cộng cả hai lại sẽ giảm trừ nhiều gấp đôi ý định.</p>
     *
     * <p>Bảng {@code giam_tru} hiện còn rỗng nên hàm này luôn trả 0 ở Phase 4C.</p>
     */
    private BigDecimal tinhGiamTru(List<GiamTru> danhSach, BigDecimal canCuTinhTyLe) {
        BigDecimal tong = BigDecimal.ZERO;
        for (GiamTru gt : danhSach) {
            if (gt.getSoTien() != null && gt.getSoTien().signum() > 0) {
                tong = tong.add(gt.getSoTien());
            } else if (gt.getTyLePhanTram() != null && gt.getTyLePhanTram().signum() > 0) {
                tong = tong.add(ThamSoTinhCuoc.lamTronTien(canCuTinhTyLe
                        .multiply(gt.getTyLePhanTram())
                        .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP)));
            }
        }
        return tong;
    }

    private void danhDauGiamTruDaApDung(List<GiamTru> danhSach) {
        for (GiamTru gt : danhSach) {
            gt.setTrangThai(TrangThaiGiamTru.DA_AP_DUNG);
        }
        if (!danhSach.isEmpty()) {
            giamTruRepository.saveAll(danhSach);
        }
    }

    // -----------------------------------------------------------------
    // Khoản mục chi tiết
    // -----------------------------------------------------------------

    /**
     * Dựng các dòng khoản mục của hóa đơn — chỉ dòng có thành tiền khác 0.
     *
     * <p><b>Vì sao ba dòng cước sử dụng để trống đơn giá:</b> mỗi dòng là tổng của nhiều
     * bản ghi áp các đơn giá khác nhau (nội mạng / ngoại mạng / quốc tế, giờ thường / giờ
     * cao điểm), nên không tồn tại một đơn giá duy nhất để ghi. Điền đơn giá bình quân
     * suy ngược từ thành tiền sẽ là con số không có thật trên bảng giá nào.</p>
     *
     * <p>Muốn tách chi tiết tới từng bậc giá thì phải lưu {@code bang_gia_cuoc_id} trên
     * từng bản ghi CDR lúc định giá — hiện chưa có cột đó, và <b>suy ngược lại ở khâu lập
     * hóa đơn là sai nguyên tắc</b>: bảng giá có thể đã đổi giữa lúc định giá và lúc lập
     * hóa đơn, khi đó đơn giá in ra không phải đơn giá đã thu. Ghi vào phần hạn chế.</p>
     */
    private void themChiTiet(HoaDon hoaDon, GoiCuoc goi, KhoangSuDung khoang,
                             BigDecimal cuocThueBao, TongHopDichVu thoai,
                             TongHopDichVu sms, TongHopDichVu data, BigDecimal giamTru) {
        List<ChiTietHoaDon> chiTiet = hoaDon.getChiTiet();

        if (cuocThueBao.signum() != 0) {
            String nhan = khoang.troVenTronKy()
                    ? "Cước thuê bao tháng — gói " + goi.getMaGoi()
                    : "Cước thuê bao tháng — gói " + goi.getMaGoi() + " ("
                            + khoang.soNgaySuDung() + "/" + khoang.soNgayTrongKy() + " ngày, từ "
                            + khoang.tuNgay() + " đến " + khoang.denNgay() + ")";
            // Đơn giá là ảnh chụp cước tháng của gói tại thời điểm lập hóa đơn: gói có thể
            // đổi giá sau này, hóa đơn cũ vẫn phải giữ đúng con số đã thu.
            chiTiet.add(dong(hoaDon, nhan,
                    BigDecimal.valueOf(khoang.soNgaySuDung()), "ngày",
                    goi.getCuocThueBaoThang(), cuocThueBao));
        }
        if (thoai.tongCuoc().signum() != 0) {
            chiTiet.add(dong(hoaDon, "Cước thoại (" + thoai.soBanGhi() + " cuộc)",
                    BigDecimal.valueOf(thoai.tongThoiLuongGiay()), "giây",
                    null, thoai.tongCuoc()));
        }
        if (sms.tongCuoc().signum() != 0) {
            chiTiet.add(dong(hoaDon, "Cước tin nhắn",
                    BigDecimal.valueOf(sms.tongSoLuong()), "tin",
                    null, sms.tongCuoc()));
        }
        if (data.tongCuoc().signum() != 0) {
            chiTiet.add(dong(hoaDon, "Cước dữ liệu",
                    BigDecimal.valueOf(DonViCuoc.kbSangMb(data.tongSoLuong())), "MB",
                    null, data.tongCuoc()));
        }
        if (giamTru.signum() != 0) {
            chiTiet.add(dong(hoaDon, "Giảm trừ", null, null, null, giamTru.negate()));
        }
    }

    private ChiTietHoaDon dong(HoaDon hoaDon, String khoanMuc, BigDecimal soLuong,
                               String donVi, BigDecimal donGia, BigDecimal thanhTien) {
        ChiTietHoaDon ct = new ChiTietHoaDon();
        ct.setHoaDon(hoaDon);
        ct.setKhoanMuc(khoanMuc);
        ct.setSoLuong(soLuong);
        ct.setDonVi(donVi);
        ct.setDonGia(donGia);
        ct.setThanhTien(thanhTien);
        return ct;
    }

    // =================================================================
    // B. LẬP HÓA ĐƠN CẢ KỲ
    // =================================================================

    @Transactional
    public KetQuaBilling billingKy(KyCuoc ky) {
        long batDau = System.currentTimeMillis();
        kiemTraTruocKhiChay(ky);

        // Áp ưu đãi TRƯỚC khi gom cước: bước này ghi lại cuoc_phi và mien_phi của từng
        // bản ghi, nên ảnh chụp tổng hợp phải nạp SAU đó mới đúng.
        int soMienPhi = apDungUuDai(ky, napNguonBilling(ky));
        log.info("Đã áp ưu đãi gói cước kỳ {}/{}: {} bản ghi được miễn phí",
                ky.getThang(), ky.getNam(), soMienPhi);

        NguonBilling nguon = napNguonBilling(ky);
        List<ThueBao> danhSach =
                thueBaoRepository.timTheoLoaiKemQuanHe(LoaiThueBao.TRA_SAU);
        log.info("Bắt đầu lập hóa đơn kỳ {}/{}: xét {} thuê bao trả sau",
                ky.getThang(), ky.getNam(), danhSach.size());

        KetQuaBilling ketQua = new KetQuaBilling();
        for (ThueBao thueBao : danhSach) {
            boolean daCo = hoaDonRepository.existsByThueBaoIdAndKyCuocId(
                    thueBao.getId(), ky.getId());
            HoaDon hoaDon = tinhCuocThueBao(thueBao, ky, nguon);
            if (hoaDon != null) {
                ketQua.setSoHoaDonTao(ketQua.getSoHoaDonTao() + 1);
            } else if (daCo) {
                ketQua.setSoBoQuaDaCo(ketQua.getSoBoQuaDaCo() + 1);
            } else {
                ketQua.setSoBoQuaKhongDuDieuKien(ketQua.getSoBoQuaKhongDuDieuKien() + 1);
            }
        }

        ketQua.setThoiGianMs(System.currentTimeMillis() - batDau);
        ketThucKy(ky, ketQua);
        return ketQua;
    }

    /**
     * Cập nhật số liệu tổng hợp của kỳ.
     *
     * <p>Đếm lại từ CSDL thay vì lấy biến đếm của lần chạy, cùng lý do như {@code soCdrXuLy}
     * ở mục 4B: chạy lần hai chỉ lập phần còn thiếu, ghi thẳng biến đếm thì hai cột này tụt
     * xuống con số của riêng lần chạy đó.</p>
     */
    private void ketThucKy(KyCuoc ky, KetQuaBilling ketQua) {
        hoaDonRepository.flush();
        long soHoaDon = hoaDonRepository.countByKyCuocId(ky.getId());
        BigDecimal doanhThu = hoaDonRepository.tinhTongDoanhThuTheoKy(ky.getId());

        ky.setSoHoaDonTao((int) soHoaDon);
        ky.setTongDoanhThu(doanhThu);
        kyCuocRepository.save(ky);
        ketQua.setTongDoanhThu(doanhThu);

        log.info("Xong lập hóa đơn kỳ {}/{}: tạo mới {}, bỏ qua {} đã có, {} không đủ điều "
                        + "kiện. Kỳ có {} hóa đơn, doanh thu {} đ, mất {} ms",
                ky.getThang(), ky.getNam(), ketQua.getSoHoaDonTao(), ketQua.getSoBoQuaDaCo(),
                ketQua.getSoBoQuaKhongDuDieuKien(), soHoaDon, doanhThu, ketQua.getThoiGianMs());

        nhatKyService.ghiNhatKy("LAP_HOA_DON", "KY_CUOC", ky.getId(),
                "Lập hóa đơn kỳ " + ky.getThang() + "/" + ky.getNam() + ": tạo mới "
                        + ketQua.getSoHoaDonTao() + " hóa đơn, kỳ hiện có " + soHoaDon
                        + " hóa đơn, tổng doanh thu " + doanhThu + " đ (chưa trừ ưu đãi gói cước)");
    }

    /**
     * Chặn trước khi chạy.
     *
     * <p>Điều kiện quan trọng nhất là <b>kỳ phải tính cước xong</b>. Lập hóa đơn trên dữ
     * liệu mới định giá một phần sẽ cho ra hóa đơn thiếu tiền — vẫn phát hành bình thường,
     * không lỗi, không cảnh báo. Đúng loại hỏng mà cả Phase 4 đang phòng.</p>
     */
    private void kiemTraTruocKhiChay(KyCuoc ky) {
        if (ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã chốt, không thể lập lại hóa đơn");
        }

        LocalDateTime tuLuc = ky.getNgayBatDau().atStartOfDay();
        LocalDateTime denLuc = ky.getNgayKetThuc().plusDays(1).atStartOfDay();

        long chuaTinh = chiTietSuDungRepository.demTheoKhoangVaTrangThai(
                tuLuc, denLuc, TrangThaiTinhCuoc.CHUA_TINH);
        if (chuaTinh > 0) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " còn " + chuaTinh + " bản ghi chi tiết sử dụng chưa tính cước. "
                    + "Phải chạy tính cước cho toàn kỳ trước khi lập hóa đơn, nếu không hóa "
                    + "đơn sẽ thiếu tiền mà không có dấu hiệu gì.");
        }

        // Bản ghi LOI thì KHÔNG chặn: có những bản ghi hỏng không sửa được, chặn cứng sẽ
        // khiến cả kỳ không bao giờ lập được hóa đơn. Nhưng phải cảnh báo thật rõ.
        long loi = chiTietSuDungRepository.demTheoKhoangVaTrangThai(
                tuLuc, denLuc, TrangThaiTinhCuoc.LOI);
        if (loi > 0) {
            log.warn("Kỳ {}/{} còn {} bản ghi chi tiết sử dụng ở trạng thái LOI. Hóa đơn sẽ "
                            + "KHÔNG bao gồm cước của các bản ghi này.",
                    ky.getThang(), ky.getNam(), loi);
        }
    }

    // =================================================================
    // B'. ÁP ƯU ĐÃI GÓI CƯỚC LÊN TỪNG BẢN GHI CDR
    // =================================================================

    /**
     * Trừ quỹ ưu đãi của từng thuê bao lên các bản ghi CDR đã định giá của kỳ.
     *
     * <p>Bản ghi nằm trong ưu đãi được đặt {@code mien_phi = 1, cuoc_phi = 0}; bản ghi phải
     * thu tiền được đặt {@code mien_phi = 0} và <b>tính lại cước đầy đủ</b> từ dòng bảng giá
     * đã lưu ở {@code bang_gia_cuoc_id}.</p>
     *
     * <p><b>Vì sao phải tính lại thay vì giữ nguyên {@code cuoc_phi}:</b> nếu chỉ giữ
     * nguyên thì hàm này <b>không chạy lại được</b> — lần chạy trước đã đặt {@code cuoc_phi = 0}
     * cho các bản ghi miễn phí, nên lần sau chúng có giá 0 vĩnh viễn dù quỹ đã đổi. Tính
     * lại từ ảnh chụp bảng giá làm hàm này <b>bất biến theo số lần chạy</b>. Đây chính là
     * chỗ cột {@code bang_gia_cuoc_id} thêm ở bước A phát huy tác dụng.</p>
     *
     * @return số bản ghi được miễn phí
     */
    private int apDungUuDai(KyCuoc ky, NguonBilling nguon) {
        Map<Long, BangGiaCuoc> bangGiaTheoId = bangGiaLookup.napTheoId();
        List<ChiTietSuDung> danhSach =
                chiTietSuDungRepository.timDaTinhTheoKyDeApUuDai(ky.getId());

        List<Object[]> lo = new ArrayList<>(KICH_THUOC_LO);
        int soMienPhi = 0;
        Long thueBaoDangXet = null;
        UuDaiGoiCuoc quy = null;

        for (ChiTietSuDung cdr : danhSach) {
            Long thueBaoId = cdr.getThueBao().getId();
            // Danh sách đã sắp xếp theo thuê bao nên chỉ cần mở quỹ mới khi đổi thuê bao
            if (!thueBaoId.equals(thueBaoDangXet)) {
                thueBaoDangXet = thueBaoId;
                quy = new UuDaiGoiCuoc(timGoiCuocCuoiKy(cdr.getThueBao(), ky, nguon));
            }

            BigDecimal cuocDayDu = tinhLaiCuocDayDu(cdr, bangGiaTheoId);
            boolean mienPhi = quy.namTrongUuDai(cdr);
            if (mienPhi) {
                soMienPhi++;
            }
            lo.add(new Object[]{mienPhi ? 1 : 0,
                    mienPhi ? BigDecimal.ZERO : cuocDayDu, cdr.getId()});

            if (lo.size() >= KICH_THUOC_LO) {
                ghiLoUuDai(lo);
            }
        }
        ghiLoUuDai(lo);
        return soMienPhi;
    }

    /** Cước đầy đủ của một bản ghi, tính lại từ đúng dòng bảng giá đã áp dụng lúc định giá. */
    private BigDecimal tinhLaiCuocDayDu(ChiTietSuDung cdr, Map<Long, BangGiaCuoc> bangGiaTheoId) {
        if (cdr.getBangGiaCuoc() == null) {
            throw new NghiepVuException("Bản ghi chi tiết sử dụng mã số " + cdr.getId()
                    + " đã tính cước nhưng không lưu dòng bảng giá đã áp dụng. "
                    + "Chạy lại tính cước cho kỳ này trước khi lập hóa đơn.");
        }
        BangGiaCuoc gia = bangGiaTheoId.get(cdr.getBangGiaCuoc().getId());
        if (gia == null) {
            throw new NghiepVuException("Không tìm thấy dòng bảng giá mã số "
                    + cdr.getBangGiaCuoc().getId() + " mà bản ghi chi tiết sử dụng mã số "
                    + cdr.getId() + " đang tham chiếu. Dòng bảng giá này có thể đã bị xóa.");
        }
        // Dùng CHUNG công thức với engine định giá, không viết lại
        return RatingService.tinhTienTheoBangGia(cdr, gia);
    }

    private void ghiLoUuDai(List<Object[]> lo) {
        if (lo.isEmpty()) {
            return;
        }
        // Chụp một bản sao rồi mới xoá lô gốc: bộ đặt tham số giữ tham chiếu tới danh sách,
        // nếu đóng gói thẳng danh sách đang được dùng lại cho lô sau thì nó trở thành cái
        // bẫy - danh sách bị xoá ngay sau khi ghi, và mọi thứ đọc lại nó về sau đều thấy rỗng.
        List<Object[]> banSao = List.copyOf(lo);
        lo.clear();

        jdbcTemplate.batchUpdate(SQL_AP_UU_DAI, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Object[] dong = banSao.get(i);
                ps.setInt(1, (Integer) dong[0]);
                ps.setBigDecimal(2, (BigDecimal) dong[1]);
                ps.setLong(3, (Long) dong[2]);
            }

            @Override
            public int getBatchSize() {
                return banSao.size();
            }
        });
    }

    /** Chụp lịch sử đăng ký gói, tổng cước theo thuê bao và giảm trừ cho một lần chạy. */
    private NguonBilling napNguonBilling(KyCuoc ky) {
        Map<Long, List<DangKyGoiCuoc>> dangKy = new HashMap<>();
        for (DangKyGoiCuoc dk : dangKyGoiCuocRepository.timTatCaKemGoiVaThueBao()) {
            dangKy.computeIfAbsent(dk.getThueBao().getId(), k -> new ArrayList<>()).add(dk);
        }
        dangKy.values().forEach(ds ->
                ds.sort(Comparator.comparing(DangKyGoiCuoc::getNgayBatDau).reversed()));

        Map<Long, Map<LoaiDichVu, TongHopDichVu>> cuoc = new HashMap<>();
        for (Object[] dong : chiTietSuDungRepository.tongHopCuocTheoThueBao(ky.getId())) {
            Long thueBaoId = (Long) dong[0];
            LoaiDichVu dichVu = (LoaiDichVu) dong[1];
            cuoc.computeIfAbsent(thueBaoId, k -> new EnumMap<>(LoaiDichVu.class))
                    .put(dichVu, new TongHopDichVu(
                            (BigDecimal) dong[2],
                            ((Number) dong[3]).longValue(),
                            ((Number) dong[4]).longValue(),
                            ((Number) dong[5]).longValue()));
        }

        Map<Long, List<GiamTru>> giamTru = new HashMap<>();
        for (GiamTru gt : giamTruRepository.timApDungChoKy(ky.getId())) {
            giamTru.computeIfAbsent(gt.getThueBao().getId(), k -> new ArrayList<>()).add(gt);
        }

        return new NguonBilling(dangKy, cuoc, giamTru);
    }

    // =================================================================
    // C. HỦY LẬP HÓA ĐƠN
    // =================================================================

    /**
     * Xóa toàn bộ hóa đơn của một kỳ để lập lại.
     *
     * <p><b>Không đụng tới {@code cuoc_phi} của CDR</b> — đó là việc của
     * {@code RatingService.huyRatingKy}. Hai chức năng tách bạch để lập lại hóa đơn không
     * bắt buộc phải định giá lại từ đầu.</p>
     *
     * @return số hóa đơn đã xóa
     */
    @Transactional
    public int huyBillingKy(KyCuoc ky) {
        if (ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã chốt, không thể hủy hóa đơn");
        }
        long soThanhToan = thanhToanRepository.demTheoKyCuoc(ky.getId());
        if (soThanhToan > 0) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã ghi nhận " + soThanhToan + " giao dịch thanh toán. Không thể xóa "
                    + "hóa đơn đã thu tiền — các giao dịch đó sẽ trỏ vào hóa đơn không còn "
                    + "tồn tại. Phải hủy giao dịch thanh toán trước.");
        }

        // Xóa chi tiết trước rồi mới xóa hóa đơn: xóa bằng JPQL không kích hoạt cascade,
        // đảo thứ tự sẽ vi phạm khóa ngoại.
        int soChiTiet = hoaDonRepository.xoaChiTietTheoKy(ky.getId());
        int soHoaDon = hoaDonRepository.xoaTheoKy(ky.getId());
        int soGiamTru = giamTruRepository.datLaiChuaApDungTheoKy(ky.getId());

        ky.setSoHoaDonTao(0);
        ky.setTongDoanhThu(BigDecimal.ZERO);
        kyCuocRepository.save(ky);

        log.info("Đã hủy lập hóa đơn kỳ {}/{}: xóa {} hóa đơn, {} dòng chi tiết, trả {} khoản "
                        + "giảm trừ về chưa áp dụng",
                ky.getThang(), ky.getNam(), soHoaDon, soChiTiet, soGiamTru);
        nhatKyService.ghiNhatKy("HUY_LAP_HOA_DON", "KY_CUOC", ky.getId(),
                "Hủy lập hóa đơn kỳ " + ky.getThang() + "/" + ky.getNam() + ": xóa "
                        + soHoaDon + " hóa đơn và " + soChiTiet + " dòng chi tiết. "
                        + "Cước phí trên chi tiết sử dụng giữ nguyên.");
        return soHoaDon;
    }
}
