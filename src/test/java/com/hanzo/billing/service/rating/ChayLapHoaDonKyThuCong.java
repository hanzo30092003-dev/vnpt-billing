package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaBilling;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Bộ chạy lập hóa đơn THỦ CÔNG cho một kỳ, dùng từ dòng lệnh.
 *
 * <p><b>Đây không phải test</b> — nó ghi thẳng vào CSDL thật và không khẳng định điều gì.
 * Tên lớp cố ý không khớp mẫu {@code *Test} của Surefire nên {@code mvnw test} bỏ qua.
 * Chạy có chủ đích bằng:</p>
 *
 * <pre>
 * .\mvnw test "-Dtest=ChayLapHoaDonKyThuCong" "-DfailIfNoTests=false"
 * </pre>
 *
 * <p>Kịch bản: lập hóa đơn lần 1 → chạy lại lần 2 (không được tạo thêm) → hủy → lập lại
 * lần 3 (phải ra đúng doanh thu của lần 1). Bước cuối là bằng chứng lập hóa đơn
 * <b>xác định</b>.</p>
 *
 * <p>Xóa lớp này khi mục 4E có nút bấm trên giao diện.</p>
 */
@SpringBootTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class ChayLapHoaDonKyThuCong {

    private static final int THANG = 6;
    private static final int NAM = 2026;

    @Autowired private BillingService billingService;
    @Autowired private KyCuocRepository kyCuocRepository;
    @Autowired private HoaDonRepository hoaDonRepository;

    @Test
    void chay() {
        KyCuoc ky = kyCuocRepository.findByThangAndNam(THANG, NAM).orElseThrow();
        in("KY CUOC", THANG + "/" + NAM + " (id=" + ky.getId() + "), trang thai "
                + ky.getTrangThai());

        KetQuaBilling lan1 = billingService.billingKy(nap(ky));
        inKetQua("LAN 1 - lap hoa don", lan1);

        KetQuaBilling lan2 = billingService.billingKy(nap(ky));
        inKetQua("LAN 2 - chay lai, khong duoc tao them", lan2);

        int soXoa = billingService.huyBillingKy(nap(ky));
        in("HUY LAP HOA DON", soXoa + " hoa don da xoa, con lai "
                + hoaDonRepository.countByKyCuocId(ky.getId()));

        KetQuaBilling lan3 = billingService.billingKy(nap(ky));
        inKetQua("LAN 3 - lap lai sau khi huy", lan3);

        in("TINH XAC DINH", "Doanh thu lan 1 = " + lan1.getTongDoanhThu()
                + " | lan 3 = " + lan3.getTongDoanhThu()
                + " | " + (lan1.getTongDoanhThu().compareTo(lan3.getTongDoanhThu()) == 0
                        ? "GIONG NHAU" : "*** KHAC NHAU ***"));
    }

    private KyCuoc nap(KyCuoc ky) {
        return kyCuocRepository.findById(ky.getId()).orElseThrow();
    }

    private void inKetQua(String nhan, KetQuaBilling kq) {
        in(nhan, "tao moi=" + kq.getSoHoaDonTao()
                + "  bo qua da co=" + kq.getSoBoQuaDaCo()
                + "  bo qua khong du dieu kien=" + kq.getSoBoQuaKhongDuDieuKien()
                + "  doanh thu ky=" + kq.getTongDoanhThu()
                + "  thoi gian=" + kq.getThoiGianMs() + " ms");
    }

    private void in(String nhan, String noiDung) {
        System.out.println(">>> " + nhan + ": " + noiDung);
    }
}
