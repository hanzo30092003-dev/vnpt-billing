package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaBilling;
import com.hanzo.billing.dto.KetQuaRating;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Chạy lại TOÀN BỘ một kỳ từ đầu: hủy hóa đơn → hủy định giá → định giá → lập hóa đơn.
 *
 * <p><b>Đây không phải test</b> — ghi thẳng vào CSDL thật. Tên lớp cố ý không khớp mẫu
 * {@code *Test} của Surefire nên {@code mvnw test} bỏ qua. Chạy bằng:</p>
 *
 * <pre>
 * .\mvnw test "-Dtest=ChayLaiToanBoKyThuCong" "-DfailIfNoTests=false"
 * </pre>
 *
 * <p>Dùng khi thay đổi có thể ảnh hưởng tới cả hai tầng — ví dụ mục 4D thêm cột
 * {@code bang_gia_cuoc_id} và áp ưu đãi gói cước. In ra tổng cước gộp và doanh thu để đối
 * chiếu với số liệu mốc.</p>
 *
 * <p>Thứ tự hủy là bắt buộc: {@code huyRatingKy} bị từ chối khi kỳ còn hóa đơn.</p>
 */
@SpringBootTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class ChayLaiToanBoKyThuCong {

    private static final int THANG = 6;
    private static final int NAM = 2026;

    @Autowired private RatingService ratingService;
    @Autowired private BillingService billingService;
    @Autowired private KyCuocRepository kyCuocRepository;
    @Autowired private HoaDonRepository hoaDonRepository;
    @Autowired private ChiTietSuDungRepository chiTietSuDungRepository;

    @Test
    void chay() {
        KyCuoc ky = kyCuocRepository.findByThangAndNam(THANG, NAM).orElseThrow();
        Long kyId = ky.getId();
        in("KY CUOC", THANG + "/" + NAM + " (id=" + kyId + "), trang thai " + ky.getTrangThai());

        int soHoaDonXoa = billingService.huyBillingKy(nap(kyId));
        in("1. HUY LAP HOA DON", soHoaDonXoa + " hoa don da xoa");

        int soCdrReset = ratingService.huyRatingKy(nap(kyId));
        in("2. HUY DINH GIA", soCdrReset + " ban ghi CDR ve CHUA_TINH");

        KetQuaRating rating = ratingService.ratingKy(nap(kyId));
        in("3. DINH GIA", "thanh cong=" + rating.getSoThanhCong()
                + "  loi=" + rating.getSoLoi()
                + "  TONG CUOC GOP=" + rating.getTongCuoc()
                + "  " + rating.getThoiGianMs() + " ms");
        in("   snapshot bang gia", "DA_TINH thieu bang_gia_cuoc_id = "
                + chiTietSuDungRepository.countByTrangThaiTinhCuocAndBangGiaCuocIsNull(
                        TrangThaiTinhCuoc.DA_TINH));

        KetQuaBilling billing = billingService.billingKy(nap(kyId));
        in("4. LAP HOA DON", "tao moi=" + billing.getSoHoaDonTao()
                + "  bo qua=" + billing.getSoBoQuaKhongDuDieuKien()
                + "  DOANH THU=" + billing.getTongDoanhThu()
                + "  " + billing.getThoiGianMs() + " ms");

        in("KIEM LAI", "so hoa don trong ky = " + hoaDonRepository.countByKyCuocId(kyId));
    }

    private KyCuoc nap(Long kyId) {
        return kyCuocRepository.findById(kyId).orElseThrow();
    }

    private void in(String nhan, String noiDung) {
        System.out.println(">>> " + nhan + ": " + noiDung);
    }
}
