package com.hanzo.billing.service.pdf;

import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.service.ThanhToanService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ In phiếu thu từ giao dịch <b>đọc thật từ CSDL</b>, ngoài transaction.
 *
 * <h2>Vì sao cần lớp test này khi đã có {@code PhieuThuPdfServiceTest}</h2>
 * <p>Lớp test kia dựng {@code ThanhToan} bằng {@code new} và gán quan hệ bằng tay, nên mọi
 * quan hệ đều là đối tượng thật, đã nạp sẵn. Nó <b>không bao giờ chạm vào proxy LAZY</b>.</p>
 *
 * <p>Trong khi chạy thật, {@code layTheoId} đọc từ CSDL rồi trả ra ngoài — transaction đóng
 * lại — và bản in mới truy cập {@code hoaDon.khachHang}. Nếu truy vấn không {@code JOIN FETCH}
 * thì chỗ đó ném {@code LazyInitializationException} và người dùng nhận <b>trang lỗi 500</b>.</p>
 *
 * <p>Đó đúng là lỗi đã xảy ra thật ở màn hình <i>Phiếu thu</i>, và bộ test cũ <b>xanh hoàn
 * toàn</b> trong lúc chức năng hỏng. Lần thứ tư của bài học 43.5: một phép kiểm không đi qua
 * đường mà sản phẩm thật đi thì không chứng minh được gì về đường đó.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy. Nếu bảng {@code thanh_toan} còn rỗng thì test
 * tự bỏ qua — nó kiểm một đường đi, không kiểm dữ liệu.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("Phiếu thu in từ dữ liệu thật")
class PhieuThuPdfTaiLieuThatTest {

    @Autowired
    private ThanhToanService thanhToanService;

    @Autowired
    private ThanhToanRepository thanhToanRepository;

    @Autowired
    private PhieuThuPdfService phieuThuPdfService;

    @Test
    @DisplayName("1. ⭐ Đọc giao dịch từ CSDL rồi in — không được ném LazyInitializationException")
    void inTuDuLieuThat() throws IOException {
        List<ThanhToan> tatCa = thanhToanRepository.findAll();
        Assumptions.assumeFalse(tatCa.isEmpty(),
                "Bảng thanh_toan còn rỗng — bỏ qua, test này kiểm đường đi chứ không kiểm dữ liệu");

        Long id = tatCa.get(0).getId();

        // Lấy qua service ĐÚNG NHƯ controller làm: transaction kết thúc ngay khi hàm trả về
        ThanhToan giaoDich = thanhToanService.layTheoId(id);

        // Mọi lời gọi dưới đây nằm NGOÀI transaction — đây là chỗ lỗi 500 từng xảy ra
        byte[] pdf = phieuThuPdfService.xuat(giaoDich);

        String vanBan = TrichVanBanPdf.trich(pdf);
        assertThat(vanBan)
                .contains("PHIẾU THU TIỀN")
                .contains(giaoDich.getMaGiaoDich())
                .contains(giaoDich.getHoaDon().getMaHoaDon())
                .contains(giaoDich.getHoaDon().getKhachHang().getTenKh())
                .contains(giaoDich.getHoaDon().getThueBao().getSoThueBao())
                .contains(PhieuThuPdfService.CHAN_TRANG);
    }

    @Test
    @DisplayName("2. Tên file phiếu thu dựng được từ giao dịch thật")
    void tenFileTuDuLieuThat() {
        List<ThanhToan> tatCa = thanhToanRepository.findAll();
        Assumptions.assumeFalse(tatCa.isEmpty(), "Bảng thanh_toan còn rỗng — bỏ qua");

        ThanhToan giaoDich = thanhToanService.layTheoId(tatCa.get(0).getId());

        assertThat(PhieuThuPdfService.tenFile(giaoDich))
                .startsWith("PhieuThu_")
                .endsWith(".pdf");
    }
}
