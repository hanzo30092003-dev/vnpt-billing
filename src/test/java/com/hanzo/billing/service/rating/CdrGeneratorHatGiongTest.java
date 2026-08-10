package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaSinhCdr;
import com.hanzo.billing.dto.SinhCdrForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ HẠT GIỐNG CỦA BỘ SINH CDR — mục A1 của Phase 6.
 *
 * <p><b>Vì sao test này tồn tại.</b> Thêm một ô "hạt giống" vào form là việc dễ; khẳng định
 * rằng nó <i>thật sự</i> làm dữ liệu tái lập được mới là điều cần chứng minh. Một tham số hạt
 * giống không có phép kiểm chỉ là một lời hứa — và nó sẽ hỏng lặng lẽ ngay lần đầu có ai đó
 * thêm một lời gọi {@code Math.random()} vào giữa đường sinh.</p>
 *
 * <p>Đây cũng là khoản nợ số 3 của Phase 5 (xem {@code PHASE-5-REPORT.md} mục 35): bộ sinh
 * dùng {@code new Random()} không hạt giống từ Phase 3, và hậu quả chỉ lộ ra ở Phase 5 dưới
 * dạng <i>"không dám chạy reset"</i>.</p>
 *
 * <h2>Vì sao chạy được trên CSDL thật mà không làm bẩn dữ liệu</h2>
 * <p>{@code @Transactional} ở mức lớp test khiến Spring <b>rollback</b> sau mỗi phương thức.
 * {@code NhatKyServiceImpl} khai {@code Propagation.REQUIRED} nên dòng nhật ký cũng nằm trong
 * cùng giao dịch và cũng bị rollback. Kết thúc test, bảng {@code chi_tiet_su_dung} không thêm
 * một dòng nào — chỉ bộ đếm AUTO_INCREMENT nhích lên, thứ không ảnh hưởng gì.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
@Transactional
@DisplayName("Hạt giống bộ sinh CDR")
class CdrGeneratorHatGiongTest {

    /** Khoảng ngày cố ý nằm ngoài mọi kỳ cước của dữ liệu mẫu, cho dễ nhận ra nếu sót lại. */
    private static final LocalDate TU_NGAY = LocalDate.of(2025, 9, 1);
    private static final LocalDate DEN_NGAY = LocalDate.of(2025, 9, 30);
    private static final int SO_LUONG = 120;

    @Autowired
    private CdrGeneratorService cdrGeneratorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static SinhCdrForm form(Long hatGiong) {
        SinhCdrForm f = new SinhCdrForm();
        f.setTuNgay(TU_NGAY);
        f.setDenNgay(DEN_NGAY);
        f.setSoLuong(SO_LUONG);
        f.setPhamVi("TAT_CA");
        f.setHatGiong(hatGiong);
        return f;
    }

    /**
     * Đọc lại các bản ghi vừa sinh thành chuỗi để so sánh, rồi <b>xoá chúng đi</b> để lần sinh
     * kế tiếp bắt đầu từ bảng sạch.
     *
     * <p>So <b>nội dung từng bản ghi</b> chứ không so số lượng: hai lần sinh khác hẳn nhau vẫn
     * ra cùng số bản ghi, nên đếm thôi thì phép kiểm không bao giờ đỏ (chuẩn làm việc 43.3).</p>
     */
    private List<String> layRoiXoa() {
        List<String> banGhi = jdbcTemplate.query(
                """
                SELECT thue_bao_id, so_bi_goi, loai_dich_vu, huong, thoi_gian_bat_dau,
                       thoi_luong_giay, so_luong, gio_cao_diem
                  FROM chi_tiet_su_dung
                 WHERE thoi_gian_bat_dau >= ? AND thoi_gian_bat_dau < ?
                 ORDER BY id
                """,
                (rs, i) -> rs.getLong(1) + "|" + rs.getString(2) + "|" + rs.getString(3) + "|"
                        + rs.getString(4) + "|" + rs.getTimestamp(5) + "|" + rs.getInt(6) + "|"
                        + rs.getInt(7) + "|" + rs.getInt(8),
                TU_NGAY.atStartOfDay(), DEN_NGAY.plusDays(1).atStartOfDay());

        jdbcTemplate.update(
                "DELETE FROM chi_tiet_su_dung WHERE thoi_gian_bat_dau >= ? AND thoi_gian_bat_dau < ?",
                TU_NGAY.atStartOfDay(), DEN_NGAY.plusDays(1).atStartOfDay());
        return banGhi;
    }

    @Test
    @DisplayName("1. ⭐ Cùng hạt giống → sinh ra ĐÚNG cùng bộ bản ghi, từng dòng một")
    void cungHatGiong_raCungBoDuLieu() {
        cdrGeneratorService.sinh(form(777L));
        List<String> lanMot = layRoiXoa();

        cdrGeneratorService.sinh(form(777L));
        List<String> lanHai = layRoiXoa();

        assertThat(lanMot)
                .as("Lần sinh đầu phải tạo ra bản ghi, nếu không thì hai danh sách rỗng "
                        + "bằng nhau và phép kiểm này xanh mà chẳng chứng minh gì")
                .isNotEmpty();
        assertThat(lanHai)
                .as("Cùng hạt giống 777 với cùng khoảng ngày và số lượng phải cho ra đúng "
                        + "cùng bộ bản ghi. Lệch nghĩa là còn một nguồn ngẫu nhiên nào đó "
                        + "không đi qua hạt giống — hoặc thứ tự danh sách thuê bao đã đổi.")
                .containsExactlyElementsOf(lanMot);
    }

    @Test
    @DisplayName("2. Hạt giống khác → bộ dữ liệu khác (phép kiểm 1 không xanh rỗng)")
    void hatGiongKhac_raBoDuLieuKhac() {
        cdrGeneratorService.sinh(form(777L));
        List<String> lanMot = layRoiXoa();

        cdrGeneratorService.sinh(form(778L));
        List<String> lanHai = layRoiXoa();

        assertThat(lanHai)
                .as("Đổi hạt giống mà vẫn ra y hệt thì hạt giống không có tác dụng gì, và "
                        + "phép kiểm 1 đang xanh vì một lý do khác với lý do ta tưởng")
                .isNotEqualTo(lanMot);
    }

    @Test
    @DisplayName("3. Bỏ trống hạt giống vẫn tái lập được — hệ thống trả về số đã dùng")
    void bongTrongHatGiong_vanTaiLapDuoc() {
        KetQuaSinhCdr tuBoc = cdrGeneratorService.sinh(form(null));
        List<String> lanTuBoc = layRoiXoa();

        // Dùng lại đúng con số hệ thống vừa báo
        cdrGeneratorService.sinh(form(tuBoc.getHatGiongDaDung()));
        List<String> lanDungLai = layRoiXoa();

        assertThat(lanDungLai)
                .as("Hạt giống hệ thống tự bốc (%d) phải dựng lại được đúng bộ dữ liệu. "
                        + "Đây là điều làm cho một lần sinh 'ngẫu nhiên' vẫn tái lập được.",
                        tuBoc.getHatGiongDaDung())
                .containsExactlyElementsOf(lanTuBoc);
    }
}
