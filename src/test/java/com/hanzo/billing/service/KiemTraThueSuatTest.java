package com.hanzo.billing.service;

import com.hanzo.billing.config.ThamSoNghiepVu;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.service.rating.ThamSoTinhCuoc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ THUẾ SUẤT VAT — đọc từ cấu hình, và mọi hóa đơn đã lập đều khớp với nó.
 *
 * <h2>Vì sao cần</h2>
 * <p>Trước việc V5, thuế suất là hằng số biên dịch cứng trong {@code ThamSoTinhCuoc}. Đưa nó
 * ra cấu hình mở ra <b>hai cách hỏng mới</b> mà trước đó không tồn tại:</p>
 * <ol>
 *   <li>Ai đó gõ cứng lại một chỗ nào đó, và chỗ đó lặng lẽ không đi theo cấu hình nữa</li>
 *   <li>Khai sai giá trị — gõ {@code 10} thay vì {@code 0.10} — rồi hệ thống lập vài trăm tờ
 *       hóa đơn thu gấp 100 lần thuế</li>
 * </ol>
 * <p>Hai nhóm phép kiểm dưới đây canh đúng hai cách hỏng đó.</p>
 *
 * <p><b>Điều kiện chạy:</b> nhóm bất biến cần MySQL đang chạy; hai nhóm còn lại không cần.</p>
 */
@DisplayName("Thuế suất VAT")
class KiemTraThueSuatTest {

    // =================================================================
    @Nested
    @SpringBootTest
    @DisplayName("Bất biến trên dữ liệu thật")
    class BatBienTrenDuLieuThat {

        @Autowired private HoaDonRepository hoaDonRepository;
        @Autowired private ThamSoNghiepVu thamSo;

        /**
         * ⭐ Kiểm <b>từng dòng</b>, không kiểm số tổng (chuẩn làm việc 43.3).
         *
         * <p>Hai hóa đơn lệch ngược dấu nhau triệt tiêu hết ở mức tổng, và mức tổng vẫn khớp
         * trong khi hai tờ hóa đơn cụ thể đều sai.</p>
         */
        @Test
        @DisplayName("1. ⭐ Mọi hóa đơn: thue_vat = làm tròn(tong_truoc_thue × thuế suất)")
        void moiHoaDonKhopVoiThueSuat() {
            List<HoaDon> tatCa = hoaDonRepository.findAll();
            BigDecimal thueSuat = thamSo.getThueSuatVat();

            List<String> lech = new ArrayList<>();
            for (HoaDon hd : tatCa) {
                BigDecimal mongDoi = ThamSoTinhCuoc.lamTronTien(
                        hd.getTongTruocThue().multiply(thueSuat));
                if (mongDoi.compareTo(hd.getThueVat()) != 0) {
                    lech.add("%s: trước thuế %s → chờ %s, đang ghi %s".formatted(
                            hd.getMaHoaDon(), hd.getTongTruocThue(), mongDoi, hd.getThueVat()));
                }
            }

            assertThat(tatCa)
                    .as("Phải có hóa đơn để kiểm, nếu không phép kiểm này xanh một cách rỗng")
                    .isNotEmpty();
            assertThat(lech)
                    .as("Đã soi %d hóa đơn với thuế suất %s. Những tờ dưới đây có thuế VAT "
                            + "không khớp — hoặc thuế suất cấu hình đã đổi sau khi lập hóa đơn, "
                            + "hoặc engine tính sai:%n  %s",
                            tatCa.size(), thamSo.nhanThueSuat(), String.join("\n  ", lech))
                    .isEmpty();
        }

        @Test
        @DisplayName("2. Tổng tiền vẫn cộng đúng: tong_thanh_toan = tong_truoc_thue + thue_vat")
        void tongTienCongDung() {
            List<String> lech = hoaDonRepository.findAll().stream()
                    .filter(hd -> hd.getTongTruocThue().add(hd.getThueVat())
                            .compareTo(hd.getTongThanhToan()) != 0)
                    .map(HoaDon::getMaHoaDon)
                    .toList();

            assertThat(lech)
                    .as("Thuế cộng vào tổng sai thì con số khách phải trả sai:%n  %s",
                            String.join("\n  ", lech))
                    .isEmpty();
        }
    }

    // =================================================================
    @Nested
    @SpringBootTest
    @TestPropertySource(properties = "billing.thue-suat-vat=0.08")
    @DisplayName("⭐ ĐỐI CHỨNG — giá trị thật sự đến từ cấu hình")
    class DoiChungDocTuCauHinh {

        @Autowired private ThamSoNghiepVu thamSo;

        /**
         * ⭐ Đây là phép kiểm khiến việc V5 có nghĩa.
         *
         * <p>Không có nó, mọi phép kiểm còn lại vẫn xanh rực rỡ khi thuế suất bị gõ cứng lại
         * thành {@code 0.10} ở đâu đó — vì bộ dữ liệu mẫu vốn dùng đúng 10%.</p>
         */
        @Test
        @DisplayName("3. ⭐ Đổi cấu hình sang 8% thì hệ thống thấy 8%, cả số lẫn chữ")
        void doiCauHinhThiHeThongThayGiaTriMoi() {
            assertThat(thamSo.getThueSuatVat())
                    .as("Giá trị vẫn là 0.10 nghĩa là nó không đọc từ cấu hình mà từ một chỗ "
                            + "gõ cứng nào đó")
                    .isEqualByComparingTo(new BigDecimal("0.08"));
            assertThat(thamSo.nhanThueSuat()).isEqualTo("8%");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("Chữ hiển thị và ràng buộc lúc khởi động")
    class ChuVaRangBuoc {

        private ThamSoNghiepVu voi(String thueSuat) {
            ThamSoNghiepVu t = new ThamSoNghiepVu();
            t.setThueSuatVat(new BigDecimal(thueSuat));
            return t;
        }

        @Test
        @DisplayName("4. Thuế suất viết cho người đọc, dùng dấu phẩy thập phân kiểu Việt")
        void nhanThueSuatVietChoNguoiDoc() {
            assertThat(voi("0.10").nhanThueSuat()).isEqualTo("10%");
            assertThat(voi("0.08").nhanThueSuat()).isEqualTo("8%");
            assertThat(voi("0.085").nhanThueSuat()).isEqualTo("8,5%");
            assertThat(voi("0").nhanThueSuat()).isEqualTo("0%");
        }

        /**
         * ⭐ Ràng buộc phải chặn ngay lúc <b>khởi động</b>, không phải lúc lập hóa đơn.
         *
         * <p>Gõ {@code 10} thay vì {@code 0.10} là lỗi rất dễ mắc, và nếu nó chỉ lộ ra lúc lập
         * hóa đơn thì lúc đó đã có vài trăm tờ hóa đơn thu gấp 100 lần thuế.</p>
         */
        @Test
        @DisplayName("5. ⭐ Khai thuế suất sai thì ứng dụng KHÔNG khởi động được")
        void khaiSaiThiKhongKhoiDongDuoc() {
            ApplicationContextRunner khungChay = new ApplicationContextRunner()
                    .withConfiguration(org.springframework.boot.autoconfigure.AutoConfigurations.of(
                            ConfigurationPropertiesAutoConfiguration.class,
                            ValidationAutoConfiguration.class))
                    .withUserConfiguration(CauHinhThu.class);

            khungChay.withPropertyValues("billing.thue-suat-vat=10")
                    .run(ctx -> assertThat(ctx)
                            .as("Gõ 10 thay vì 0.10 phải làm ứng dụng chết ngay lúc khởi động")
                            .hasFailed());

            khungChay.withPropertyValues("billing.thue-suat-vat=-0.1")
                    .run(ctx -> assertThat(ctx).hasFailed());

            // Đối chứng: giá trị hợp lệ thì khởi động bình thường
            khungChay.withPropertyValues("billing.thue-suat-vat=0.08")
                    .run(ctx -> assertThat(ctx).hasNotFailed());
        }

        @EnableConfigurationProperties(ThamSoNghiepVu.class)
        static class CauHinhThu {
        }
    }
}
