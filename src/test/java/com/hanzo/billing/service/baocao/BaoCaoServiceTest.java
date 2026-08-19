package com.hanzo.billing.service.baocao;

import com.hanzo.billing.dto.baocao.CoCauCuoc;
import com.hanzo.billing.dto.baocao.DongDoanhThuGoi;
import com.hanzo.billing.dto.baocao.DongDoanhThuKy;
import com.hanzo.billing.dto.baocao.DongKhachHangNo;
import com.hanzo.billing.dto.baocao.DongTopThueBao;
import com.hanzo.billing.dto.baocao.SanLuongDichVu;
import com.hanzo.billing.dto.baocao.ThongKeThueBao;
import com.hanzo.billing.dto.baocao.ThongTinDashboard;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ KIỂM CHÉO SỐ LIỆU BÁO CÁO — tiêu chí nghiệm thu số 6 của Phase 6.
 *
 * <h2>Kiểm chéo nghĩa là gì ở đây</h2>
 * <p>Không so số của báo cáo với một hằng số chép tay — làm vậy chỉ chứng minh rằng hôm nay
 * dữ liệu vẫn như hôm qua, và phép kiểm sẽ đỏ oan mỗi lần bộ dữ liệu mẫu đổi. Thay vào đó, so
 * <b>hai hoặc ba đường truy vấn khác nhau</b> phải cho ra cùng một con số:</p>
 *
 * <pre>
 * SUM theo KỲ  ==  SUM theo GÓI CƯỚC  ==  SUM của bảng CƠ CẤU CƯỚC
 * </pre>
 *
 * <p>Ba câu truy vấn đó gom nhóm theo ba cách hoàn toàn khác nhau. Nếu một câu sai điều kiện
 * join hoặc sót một nhóm, con số của nó sẽ lệch khỏi hai câu còn lại — mà đó đúng là loại lỗi
 * một hằng số chép tay không bắt được.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("Kiểm chéo số liệu báo cáo")
class BaoCaoServiceTest {

    @Autowired private BaoCaoService baoCaoService;
    @Autowired private HoaDonRepository hoaDonRepository;
    @Autowired private ThueBaoRepository thueBaoRepository;
    @Autowired private KyCuocRepository kyCuocRepository;

    private KyCuoc kyCoDuLieu() {
        return baoCaoService.kyMacDinh().orElseThrow(
                () -> new IllegalStateException("Không có kỳ nào đã lập hóa đơn"));
    }

    @Test
    @DisplayName("1. ⭐ Doanh thu một kỳ: gom theo KỲ, theo GÓI và theo KHOẢN MỤC phải bằng nhau")
    void baDuongGomNhomChoCungMotSo() {
        KyCuoc ky = kyCoDuLieu();

        BigDecimal theoKy = baoCaoService.doanhThuTheoKy().stream()
                .filter(d -> d.kyId().equals(ky.getId()))
                .map(DongDoanhThuKy::phatSinh)
                .findFirst().orElseThrow();

        BigDecimal theoGoi = baoCaoService.doanhThuTheoGoi(ky.getId()).stream()
                .map(DongDoanhThuGoi::phatSinh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal theoKhoanMuc = baoCaoService.coCauCuoc(ky.getId()).tongThanhToan();

        assertThat(theoGoi)
                .as("Gom theo gói cước phải ra đúng tổng của kỳ %s. Lệch nghĩa là câu theo "
                        + "gói bị sót nhóm — thường do JOIN làm rơi hóa đơn không có gói.",
                        ky.getThang() + "/" + ky.getNam())
                .isEqualByComparingTo(theoKy);
        assertThat(theoKhoanMuc)
                .as("Bảng cơ cấu cước phải ra đúng tổng của kỳ %s",
                        ky.getThang() + "/" + ky.getNam())
                .isEqualByComparingTo(theoKy);
    }

    @Test
    @DisplayName("2. ⭐ Tổng doanh thu mọi kỳ khớp tổng trên bảng hóa đơn")
    void tongMoiKyKhopBangHoaDon() {
        List<DongDoanhThuKy> theoKy = baoCaoService.doanhThuTheoKy();

        BigDecimal congDon = theoKy.stream()
                .map(DongDoanhThuKy::phatSinh).reduce(BigDecimal.ZERO, BigDecimal::add);
        long soHoaDon = theoKy.stream().mapToLong(DongDoanhThuKy::soHoaDon).sum();

        BigDecimal tongThat = hoaDonRepository.findAll().stream()
                .map(h -> h.getTongThanhToan()).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(soHoaDon).isEqualTo(hoaDonRepository.count());
        assertThat(congDon).isEqualByComparingTo(tongThat);
    }

    @Test
    @DisplayName("3. ⭐ Còn nợ trên dashboard khớp tổng cột con_no")
    void conNoDashboardKhopCotConNo() {
        ThongTinDashboard dashboard = baoCaoService.dashboard();

        BigDecimal tongThat = hoaDonRepository.findAll().stream()
                .map(h -> h.getConNo())
                .filter(c -> c.signum() > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(dashboard.tongCongNo())
                .as("Thẻ công nợ phải ĐỌC cột con_no, không cộng lại từ bảng thanh toán")
                .isEqualByComparingTo(tongThat);
    }

    @Test
    @DisplayName("4. Cơ cấu cước: các khoản mục cộng lại đúng bằng tổng trước thuế + giảm trừ")
    void coCauCuocTuNhatQuan() {
        KyCuoc ky = kyCoDuLieu();
        CoCauCuoc coCau = baoCaoService.coCauCuoc(ky.getId());

        // tong_truoc_thue = (tổng cước) − (giảm trừ), theo đúng công thức của BillingService
        assertThat(coCau.tongCuocTruocGiamTru().subtract(coCau.giamTru()))
                .as("Tổng trước thuế phải bằng tổng cước trừ giảm trừ")
                .isEqualByComparingTo(coCau.tongTruocThue());

        assertThat(coCau.tongTruocThue().add(coCau.thueVat()))
                .as("Tổng thanh toán phải bằng tổng trước thuế cộng VAT")
                .isEqualByComparingTo(coCau.tongThanhToan());
    }

    @Test
    @DisplayName("5. Cơ cấu thuê bao: các lát biểu đồ cộng lại đúng bằng tổng thuê bao")
    void coCauThueBaoCongDuTong() {
        ThongKeThueBao thongKe = baoCaoService.thongKeThueBao();
        long tong = thueBaoRepository.count();

        assertThat(thongKe.theoTrangThai().stream().mapToLong(m -> m.soLuong()).sum())
                .as("Tổng các lát 'theo trạng thái'").isEqualTo(tong);
        assertThat(thongKe.theoLoai().stream().mapToLong(m -> m.soLuong()).sum())
                .as("Tổng các lát 'theo loại'").isEqualTo(tong);
        assertThat(thongKe.thueBaoMoi().stream().mapToLong(Long::longValue).sum())
                .as("Mọi thuê bao đều có ngày kích hoạt nên tổng chuỗi 'mới' bằng tổng thuê bao")
                .isEqualTo(tong);

        assertThat(thongKe.mocThoiGian())
                .as("Hai chuỗi phải nằm trên CÙNG một trục thời gian")
                .hasSameSizeAs(thongKe.thueBaoMoi())
                .hasSameSizeAs(thongKe.thueBaoRoiMang());
    }

    @Test
    @DisplayName("6. Top thuê bao cước cao sắp giảm dần và không vượt quá số lượng yêu cầu")
    void topThueBaoSapDung() {
        KyCuoc ky = kyCoDuLieu();
        List<DongTopThueBao> top = baoCaoService.topThueBaoCuocCao(ky.getId(), 10);

        assertThat(top).isNotEmpty().hasSizeLessThanOrEqualTo(10);
        assertThat(top).isSortedAccordingTo(
                (a, b) -> b.tongThanhToan().compareTo(a.tongThanhToan()));
    }

    @Test
    @DisplayName("7. Sản lượng: tổng phút ba hướng bằng tổng phút chung")
    void sanLuongCongDuBaHuong() {
        KyCuoc ky = kyCoDuLieu();
        SanLuongDichVu sanLuong = baoCaoService.sanLuong(ky.getId());

        assertThat(sanLuong.giayNoiMang() + sanLuong.giayNgoaiMang() + sanLuong.giayQuocTe())
                .as("Ba hướng phải cộng đủ tổng giây — thiếu nghĩa là CASE WHEN sót một hướng")
                .isEqualTo(sanLuong.tongGiay());
        assertThat(sanLuong.rong()).isFalse();
    }

    @Test
    @DisplayName("8. ⭐ Top khách hàng nợ: tổng nợ của họ không vượt tổng công nợ toàn hệ thống")
    void topKhachNoKhongVuotTong() {
        List<DongKhachHangNo> top = baoCaoService.topKhachHangNo(10);
        BigDecimal tongTop = top.stream()
                .map(DongKhachHangNo::tongConNo).reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(top).isNotEmpty();
        assertThat(tongTop).isLessThanOrEqualTo(hoaDonRepository.tongConNoToanHeThong());
        assertThat(top).isSortedAccordingTo(
                (a, b) -> b.tongConNo().compareTo(a.tongConNo()));
    }

    /**
     * Kỳ liền trước phải so theo <b>tháng/năm</b>, không theo id. Trong dữ liệu mẫu kỳ 3 và 4
     * mang id 4 và 5 vì được tạo sau — so theo id sẽ ghép nhầm cặp kỳ và mọi con số "so với
     * kỳ trước" của báo cáo C.6 đều vô nghĩa.
     */
    @Test
    @DisplayName("9. ⭐ Kỳ liền trước tính theo tháng/năm chứ không theo id")
    void kyLienTruocTheoThoiGian() {
        KyCuoc ky = kyCoDuLieu();
        KyCuoc truoc = baoCaoService.kyLienTruoc(ky.getId()).orElseThrow();

        int khoaNay = ky.getNam() * 100 + ky.getThang();
        int khoaTruoc = truoc.getNam() * 100 + truoc.getThang();
        assertThat(khoaTruoc)
                .as("Kỳ %d/%d phải đứng trước kỳ %d/%d theo thời gian",
                        truoc.getThang(), truoc.getNam(), ky.getThang(), ky.getNam())
                .isLessThan(khoaNay);
    }

    @Test
    @DisplayName("10. Báo cáo chạy được khi truyền kỳ null, không ném lỗi")
    void chayDuocKhiKhongCoKy() {
        assertThat(baoCaoService.doanhThuTheoGoi(null)).isEmpty();
        assertThat(baoCaoService.topThueBaoCuocCao(null, 10)).isEmpty();
        assertThat(baoCaoService.sanLuong(null).rong()).isTrue();
        assertThat(baoCaoService.coCauCuoc(null).rong()).isTrue();
        assertThat(baoCaoService.kyLienTruoc(null)).isEmpty();
    }

    /**
     * ⭐ Kỳ <b>có thật nhưng RỖNG</b> — khác hẳn kỳ {@code null} của phép kiểm 10.
     *
     * <p>Đây là một lỗi thật đã bắt được ở mục C: {@code SUM(...)} trên một kỳ không có bản
     * ghi CDR nào trả về {@code NULL}, mà sáu tham số của {@link SanLuongDichVu} đều là
     * {@code long} nguyên thuỷ — Hibernate không mở hộp được {@code null} và cả trang báo cáo
     * trả về <b>HTTP 500</b>.</p>
     *
     * <p>Lỗi này <b>không bao giờ lộ ra trên dữ liệu mẫu</b> vì cả năm kỳ đều có CDR. Nó chỉ
     * xảy ra đúng lúc người dùng vừa tạo một kỳ mới rồi mở báo cáo lên xem — tức lúc khó đoán
     * nhất. Phép kiểm này dựng lại đúng tình huống đó bằng một kỳ tạm, và
     * {@code @Transactional} rollback nó ngay sau khi chạy xong.</p>
     */
    @Test
    @Transactional
    @DisplayName("11. ⭐ Kỳ CÓ THẬT nhưng chưa có CDR nào: mọi báo cáo trả về rỗng, không ném lỗi")
    void kyCoThatNhungRong_khongNemLoi() {
        KyCuoc kyRong = new KyCuoc();
        kyRong.setThang(11);
        kyRong.setNam(2029);
        kyRong.setNgayBatDau(LocalDate.of(2029, 11, 1));
        kyRong.setNgayKetThuc(LocalDate.of(2029, 11, 30));
        kyRong.setTrangThai(TrangThaiKyCuoc.MO);
        kyRong.setSoCdrXuLy(0);
        kyRong.setSoHoaDonTao(0);
        kyRong.setTongDoanhThu(BigDecimal.ZERO);
        KyCuoc daLuu = kyCuocRepository.save(kyRong);

        assertThat(baoCaoService.sanLuong(daLuu.getId()).rong())
                .as("SUM trên kỳ rỗng trả NULL — thiếu COALESCE là ném lỗi mở hộp ở đây")
                .isTrue();
        assertThat(baoCaoService.coCauCuoc(daLuu.getId()).rong()).isTrue();
        assertThat(baoCaoService.doanhThuTheoGoi(daLuu.getId())).isEmpty();
        assertThat(baoCaoService.topThueBaoCuocCao(daLuu.getId(), 10)).isEmpty();
    }
}
