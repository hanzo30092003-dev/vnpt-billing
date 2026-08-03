package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaRating;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Bộ chạy tính cước THỦ CÔNG cho một kỳ, dùng từ dòng lệnh.
 *
 * <p><b>Đây không phải test</b> — nó ghi thẳng vào CSDL thật và không khẳng định điều gì.
 * Tên lớp cố ý không khớp mẫu {@code *Test} / {@code Test*} của Surefire nên
 * {@code mvnw test} <b>bỏ qua</b> nó. Chạy có chủ đích bằng:</p>
 *
 * <pre>
 * .\mvnw test "-Dtest=ChayTinhCuocKyThuCong" "-DfailIfNoTests=false"
 * </pre>
 *
 * <p>Tồn tại vì mục 4B chưa có giao diện — nút "Tính cước kỳ" thuộc mục 4E. Khi màn hình
 * đó có rồi thì xóa lớp này đi.</p>
 *
 * <p>Kịch bản chạy: tính cước lần 1 → chạy lại lần 2 (phải không đổi) → hủy kết quả →
 * tính lại lần 3 (phải ra đúng con số của lần 1). Bước cuối là bằng chứng engine tính
 * <b>xác định</b>: cùng đầu vào luôn cho cùng kết quả.</p>
 */
@SpringBootTest
@TestPropertySource(properties = "spring.sql.init.mode=never")
class ChayTinhCuocKyThuCong {

    private static final int THANG = 6;
    private static final int NAM = 2026;

    @Autowired private RatingService ratingService;
    @Autowired private KyCuocRepository kyCuocRepository;
    @Autowired private ChiTietSuDungRepository chiTietSuDungRepository;

    @Test
    void chay() {
        KyCuoc ky = kyCuocRepository.findByThangAndNam(THANG, NAM).orElseThrow();
        in("KY CUOC", THANG + "/" + NAM + " (id=" + ky.getId() + "), trang thai "
                + ky.getTrangThai());

        KetQuaRating lan1 = ratingService.ratingKy(ky);
        inKetQua("LAN 1 - tinh cuoc", lan1);
        inTrangThai(ky.getId());

        KetQuaRating lan2 = ratingService.ratingKy(nap(ky));
        inKetQua("LAN 2 - chay lai, phai khong co gi de lam", lan2);

        int soHuy = ratingService.huyRatingKy(nap(ky));
        in("HUY KET QUA", soHuy + " ban ghi ve CHUA_TINH");

        KetQuaRating lan3 = ratingService.ratingKy(nap(ky));
        inKetQua("LAN 3 - tinh lai sau khi huy", lan3);
        inTrangThai(ky.getId());

        in("TINH XAC DINH", "Tong cuoc lan 1 = " + lan1.getTongCuoc()
                + " | lan 3 = " + lan3.getTongCuoc()
                + " | " + (lan1.getTongCuoc().compareTo(lan3.getTongCuoc()) == 0
                        ? "GIONG NHAU" : "*** KHAC NHAU ***"));
    }

    /** Nạp lại kỳ từ CSDL để lấy đúng trạng thái sau bước trước. */
    private KyCuoc nap(KyCuoc ky) {
        return kyCuocRepository.findById(ky.getId()).orElseThrow();
    }

    private void inTrangThai(Long kyId) {
        in("  trang thai CDR",
                "DA_TINH=" + chiTietSuDungRepository.countByKyCuocIdAndTrangThaiTinhCuoc(
                        kyId, TrangThaiTinhCuoc.DA_TINH)
                        + "  LOI=" + chiTietSuDungRepository.countByKyCuocIdAndTrangThaiTinhCuoc(
                        kyId, TrangThaiTinhCuoc.LOI)
                        + "  gan ky=" + chiTietSuDungRepository.countByKyCuocId(kyId));
    }

    private void inKetQua(String nhan, KetQuaRating kq) {
        in(nhan, "thanh cong=" + kq.getSoThanhCong()
                + "  loi=" + kq.getSoLoi()
                + "  tong cuoc=" + kq.getTongCuoc()
                + "  thoi gian=" + kq.getThoiGianMs() + " ms");
    }

    private void in(String nhan, String noiDung) {
        System.out.println(">>> " + nhan + ": " + noiDung);
    }
}
