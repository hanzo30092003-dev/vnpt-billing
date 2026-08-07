package com.hanzo.billing.service;

import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ BẤT BIẾN THANH TOÁN — ràng buộc ② của Phase 5, kiểm trên dữ liệu thật.
 *
 * <p>Ràng buộc ② nói {@code hoa_don.con_no} là <b>nguồn sự thật duy nhất</b> cho số còn nợ:
 * chỉ {@code ThanhToanService} được ghi nó, và mọi màn hình chỉ đọc. Một cam kết như vậy chỉ
 * có giá trị nếu có phép kiểm chứng minh hai nguồn không bao giờ rời nhau:</p>
 *
 * <pre>
 * 1. con_no        = tong_thanh_toan − da_thanh_toan     (cột dẫn xuất không lệch cột gốc)
 * 2. da_thanh_toan = SUM(thanh_toan.so_tien)             (cột tổng không lệch bảng chi tiết)
 * </pre>
 *
 * <p>Kế hoạch Phase 5 đòi chạy phép kiểm này <b>sau mỗi mục</b>. Từ mục A tới E nó đúng một
 * cách rỗng vì bảng {@code thanh_toan} không có dòng nào — mà một bất biến chưa từng có dữ
 * liệu để kiểm thì chưa chứng minh được gì (bài học 43.5). Mục F là lúc nó có việc thật.</p>
 *
 * <p>Kiểm <b>toàn bộ</b> hóa đơn chứ không lấy mẫu (chuẩn làm việc 43.2), và kiểm
 * <b>từng dòng</b> chứ không so hai số tổng (chuẩn 43.3): hai hóa đơn lệch ngược dấu sẽ
 * triệt tiêu nhau ở mức tổng và lọt qua.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306}, giống
 * {@code KiemTraSoCaiSoDuTest} và {@code SchemaValidationTest}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
@DisplayName("Bất biến thanh toán")
class KiemTraBatBienThanhToanTest {

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ThanhToanRepository thanhToanRepository;

    @Test
    @DisplayName("1. Với MỌI hóa đơn, con_no = tong_thanh_toan − da_thanh_toan")
    void moiHoaDon_conNoKhopHaiCotGoc() {
        List<HoaDon> tatCa = hoaDonRepository.findAll();
        List<String> lech = new ArrayList<>();

        for (HoaDon h : tatCa) {
            BigDecimal dungRa = h.getTongThanhToan().subtract(khongNull(h.getDaThanhToan()));
            // compareTo chứ không equals: 0 và 0.00 khác scale nhưng cùng giá trị
            if (h.getConNo().compareTo(dungRa) != 0) {
                lech.add(h.getMaHoaDon() + " (id " + h.getId() + "): con_no = " + h.getConNo()
                        + " nhưng tong_thanh_toan − da_thanh_toan = " + dungRa
                        + " — chênh " + h.getConNo().subtract(dungRa));
            }
        }

        assertThat(lech)
                .as("Đã kiểm %d hóa đơn. Hóa đơn dưới đây có con_no không suy được từ hai cột "
                        + "gốc — gần như chắc chắn là có chỗ sửa lẻ một cột thay vì đi trọn "
                        + "chuỗi cập nhật của ThanhToanService:%n  %s",
                        tatCa.size(), String.join("\n  ", lech))
                .isEmpty();
    }

    @Test
    @DisplayName("2. Với MỌI hóa đơn, da_thanh_toan = SUM(thanh_toan.so_tien)")
    void moiHoaDon_daThanhToanKhopBangGiaoDich() {
        Map<Long, BigDecimal> tongTheoHoaDon = new HashMap<>();
        for (ThanhToan giaoDich : thanhToanRepository.findAll()) {
            tongTheoHoaDon.merge(giaoDich.getHoaDon().getId(), giaoDich.getSoTien(),
                    BigDecimal::add);
        }

        List<HoaDon> tatCa = hoaDonRepository.findAll();
        List<String> lech = new ArrayList<>();

        for (HoaDon h : tatCa) {
            BigDecimal daGhi = khongNull(h.getDaThanhToan());
            BigDecimal thucThu = tongTheoHoaDon.getOrDefault(h.getId(), BigDecimal.ZERO);
            if (daGhi.compareTo(thucThu) != 0) {
                lech.add(h.getMaHoaDon() + " (id " + h.getId() + "): da_thanh_toan = " + daGhi
                        + " nhưng bảng thanh_toan cộng ra " + thucThu
                        + " — chênh " + daGhi.subtract(thucThu));
            }
        }

        assertThat(lech)
                .as("Đã kiểm %d hóa đơn trên %d giao dịch thanh toán. Hóa đơn dưới đây có cột "
                        + "tổng rời khỏi bảng chi tiết — hoặc có giao dịch ghi thẳng vào CSDL "
                        + "không qua ThanhToanService, hoặc có chỗ sửa da_thanh_toan bằng tay:"
                        + "%n  %s",
                        tatCa.size(), thanhToanRepository.count(), String.join("\n  ", lech))
                .isEmpty();
    }

    /**
     * Trạng thái phải suy được từ số còn nợ, không được rời nó.
     *
     * <p>Hai test trên kiểm quan hệ giữa các cột <i>tiền</i>. Test này kiểm cột
     * <i>trạng thái</i> — thứ mà mọi màn hình dùng để tô badge và để lọc — có còn nói đúng
     * về số tiền hay không. Lệch ở đây không làm sai một đồng nào nhưng làm sai mọi bộ lọc
     * và mọi con số thống kê đếm theo trạng thái.</p>
     */
    @Test
    @DisplayName("3. Trạng thái khớp số còn nợ: DA_TT khi và chỉ khi con_no = 0")
    void trangThaiKhopSoConNo() {
        List<HoaDon> tatCa = hoaDonRepository.findAll();
        List<String> lech = new ArrayList<>();

        for (HoaDon h : tatCa) {
            boolean hetNo = h.getConNo().signum() == 0;
            boolean daTt = h.getTrangThai() == TrangThaiHoaDon.DA_TT;
            if (hetNo != daTt) {
                lech.add(h.getMaHoaDon() + " (id " + h.getId() + "): con_no = " + h.getConNo()
                        + " nhưng trạng thái là " + h.getTrangThai());
            }
        }

        assertThat(lech)
                .as("Đã kiểm %d hóa đơn. Hóa đơn dưới đây có trạng thái mâu thuẫn với số còn "
                        + "nợ:%n  %s", tatCa.size(), String.join("\n  ", lech))
                .isEmpty();
    }

    /**
     * ⭐ Luật siết ở mục F: quét quá hạn không được ghi đè hóa đơn <b>đã thu được tiền</b>.
     *
     * <p>Trước mục F, {@code timCanChuyenQuaHan} quét mọi hóa đơn còn nợ không phải
     * {@code DA_TT}, nên hóa đơn trả một phần cũng bị kéo về {@code QUA_HAN}. Hệ quả:
     * {@code TT_MOT_PHAN} là trạng thái <b>không thể tồn tại</b> sau ngày hết hạn — nó chỉ
     * sống tới lần kế tiếp có ai đó mở màn hình hóa đơn hoặc công nợ.</p>
     *
     * <p>Đây là phép kiểm giữ cho luật đó không bị lặng lẽ đảo lại: một dòng
     * {@code QUA_HAN} có {@code da_thanh_toan > 0} nghĩa là quét đã ghi đè lên tiền thật.</p>
     */
    @Test
    @DisplayName("4. ⭐ Không hóa đơn nào đã thu tiền mà bị quét về QUA_HAN")
    void hoaDonDaThuTien_khongBiQuetVeQuaHan() {
        List<HoaDon> tatCa = hoaDonRepository.findAll();
        List<String> lech = new ArrayList<>();

        for (HoaDon h : tatCa) {
            if (h.getTrangThai() == TrangThaiHoaDon.QUA_HAN
                    && khongNull(h.getDaThanhToan()).signum() > 0) {
                lech.add(h.getMaHoaDon() + " (id " + h.getId() + "): đã thu "
                        + h.getDaThanhToan() + " đ nhưng trạng thái là QUA_HAN, đáng lẽ phải "
                        + "là TT_MOT_PHAN");
            }
        }

        assertThat(lech)
                .as("Đã kiểm %d hóa đơn. Hóa đơn dưới đây đã thu được một phần nhưng trạng "
                        + "thái bị quét quá hạn ghi đè:%n  %s",
                        tatCa.size(), String.join("\n  ", lech))
                .isEmpty();
    }

    private static BigDecimal khongNull(BigDecimal giaTri) {
        return giaTri == null ? BigDecimal.ZERO : giaTri;
    }
}
