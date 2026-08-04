package com.hanzo.billing.service.rating;

import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ BẤT BIẾN ĐỘ PHỦ BẢNG GIÁ — mọi tổ hợp tra giá đều phải tra ra đơn giá.
 *
 * <p><b>Vì sao có test này — bài học phương pháp kiểm thử của Phase 4.</b></p>
 *
 * <p>Phase 3 đã nhận ra rủi ro "bảng giá không phủ hết tổ hợp" và xử lý rất tốt trên trục
 * {@code gio_cao_diem}: tách hẳn {@link QuyTacGioCaoDiem} làm nơi định nghĩa duy nhất, viết
 * javadoc giải thích hậu quả, rồi đặt tiêu chí nghiệm thu số 4 — "non-THOAI mang cờ giờ cao
 * điểm = 0".</p>
 *
 * <p>Nhưng tiêu chí đó kiểm <b>đúng cái luật vừa được xử lý</b>. Trục {@code huong} có y hệt
 * rủi ro ấy, không ai kiểm, và lọt qua toàn bộ 10 tiêu chí nghiệm thu: bộ sinh chọn hướng
 * độc lập với loại dịch vụ nên tạo ra 251 bản ghi (5,00%) thuộc ba tổ hợp không có dòng
 * bảng giá nào khớp.</p>
 *
 * <p>Cách chữa không phải là thêm tiêu chí thứ 11 cho trục {@code huong} — làm vậy thì trục
 * tiếp theo vẫn sẽ lọt. Cách chữa là chuyển từ kiểm <b>một luật cụ thể</b> sang kiểm
 * <b>bất biến tổng quát</b>: <i>mọi tổ hợp tra giá đều phải tra ra đơn giá</i>. Bất biến đó
 * bao trùm cả hai trục, và bao trùm cả trục chưa ai nghĩ tới.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306}, giống
 * {@code SchemaValidationTest}. Đặt {@code spring.sql.init.mode=never} để test không vô tình
 * chạy {@code schema.sql} — file đó mở đầu bằng {@code DROP TABLE}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
@DisplayName("Bất biến độ phủ bảng giá")
class KiemTraDoPhuBangGiaTest {

    @Autowired
    private BangGiaLookup bangGiaLookup;

    @Autowired
    private ChiTietSuDungRepository chiTietSuDungRepository;

    /**
     * Kiểm tra trên <b>không gian tổ hợp</b>, không phụ thuộc dữ liệu đang có.
     *
     * <p>Đây là nửa quan trọng hơn của bất biến: nó đúng ngay cả khi bảng
     * {@code chi_tiet_su_dung} còn rỗng, nên không bao giờ "xanh vì không có gì để kiểm".
     * Test này sẽ bắt được lỗi 251 bản ghi <b>trước khi</b> sinh ra bản ghi đầu tiên.</p>
     */
    @Test
    @DisplayName("1. Mọi tổ hợp bộ sinh CDR có thể tạo ra đều tra được đơn giá")
    void moiToHopCoTheSinhRa_deuTraDuocDonGia() {
        BangGiaLookup.Bang bang = bangGiaLookup.nap();
        LocalDate homNay = LocalDate.now();
        List<String> thieu = new ArrayList<>();

        for (LoaiDichVu dichVu : LoaiDichVu.values()) {
            for (HuongCuocGoi huong : QuyTacToHopDichVu.huongHopLe(dichVu)) {
                for (boolean gioCaoDiem : coTheMangCoCaoDiem(dichVu)) {
                    if (bang.tim(null, dichVu, huong, gioCaoDiem, homNay).isEmpty()) {
                        thieu.add(moTa(dichVu, huong, gioCaoDiem));
                    }
                }
            }
        }

        assertThat(thieu)
                .as("Bảng giá mặc định chung không phủ hết không gian tổ hợp tại ngày %s. "
                        + "Mọi CDR thuộc các tổ hợp dưới đây sẽ rơi vào trạng thái LOI khi "
                        + "chạy engine tính cước. Bổ sung dòng bảng giá, hoặc thu hẹp "
                        + "QuyTacToHopDichVu nếu tổ hợp đó không nên tồn tại.%n  %s",
                        homNay, String.join("\n  ", thieu))
                .isEmpty();
    }

    /**
     * Kiểm tra trên <b>dữ liệu thật</b> đang nằm trong CSDL.
     *
     * <p>Bổ sung cho test 1 ở hai điểm mà không gian tổ hợp không nói được: dữ liệu có thể
     * chứa tổ hợp cũ do bộ sinh đời trước để lại hoặc do nhập tay bằng SQL, và mỗi nhóm có
     * khoảng ngày riêng nên phải kiểm đúng khoảng hiệu lực của bảng giá tại đó.</p>
     *
     * <p>Tra theo đúng gói cước của thuê bao, vì engine ưu tiên dòng gắn gói trước dòng
     * mặc định chung — phải kiểm đúng đường mà engine sẽ đi.</p>
     */
    @Test
    @DisplayName("2. Mọi tổ hợp thực sự có trong CSDL đều tra được đơn giá, ở cả hai đầu khoảng ngày")
    void moiToHopTrongCsdl_deuTraDuocDonGia() {
        BangGiaLookup.Bang bang = bangGiaLookup.nap();
        List<Object[]> toHop = chiTietSuDungRepository.thongKeToHopTraGia();
        List<String> thieu = new ArrayList<>();

        for (Object[] dong : toHop) {
            Long goiCuocId = (Long) dong[0];
            LoaiDichVu dichVu = (LoaiDichVu) dong[1];
            HuongCuocGoi huong = (HuongCuocGoi) dong[2];
            boolean gioCaoDiem = Boolean.TRUE.equals(dong[3]);
            long soBanGhi = (Long) dong[4];
            LocalDate somNhat = ((LocalDateTime) dong[5]).toLocalDate();
            LocalDate muonNhat = ((LocalDateTime) dong[6]).toLocalDate();

            // Không dùng Set.of: nhóm chỉ có CDR trong đúng một ngày thì hai giá trị
            // trùng nhau, và Set.of ném IllegalArgumentException khi gặp phần tử lặp.
            Set<LocalDate> ngayCanKiem = new LinkedHashSet<>(List.of(somNhat, muonNhat));
            for (LocalDate ngay : ngayCanKiem) {
                if (bang.tim(goiCuocId, dichVu, huong, gioCaoDiem, ngay).isEmpty()) {
                    thieu.add(moTa(dichVu, huong, gioCaoDiem)
                            + " — gói id " + goiCuocId
                            + ", ngày " + ngay
                            + ", ảnh hưởng " + soBanGhi + " bản ghi CDR");
                }
            }
        }

        assertThat(thieu)
                .as("Đã kiểm %d tổ hợp (gói, dịch vụ, hướng, khung giờ) có trong "
                        + "chi_tiet_su_dung. Các tổ hợp dưới đây tra không ra đơn giá và sẽ "
                        + "rơi vào trạng thái LOI:%n  %s",
                        toHop.size(), String.join("\n  ", thieu))
                .isEmpty();
    }

    /**
     * Mọi bản ghi đã tính cước phải lưu lại dòng bảng giá đã áp dụng.
     *
     * <p>Không có ảnh chụp này thì khâu lập hóa đơn buộc phải <i>suy ngược</i> lại bảng giá,
     * mà bảng giá lúc đó có thể đã đổi — đơn giá in trên hóa đơn sẽ không phải đơn giá đã
     * thu. Xem {@code docs/PHASE-4-REPORT.md} mục 17.3.</p>
     */
    @Test
    @DisplayName("3. Mọi CDR đã tính cước đều lưu lại dòng bảng giá đã áp dụng")
    void moiCdrDaTinh_deuLuuBangGiaDaApDung() {
        assertThat(chiTietSuDungRepository.countByTrangThaiTinhCuocAndBangGiaCuocIsNull(
                TrangThaiTinhCuoc.DA_TINH))
                .as("Bản ghi DA_TINH mà bang_gia_cuoc_id để trống thì không truy nguyên được "
                        + "đơn giá đã thu")
                .isZero();
    }

    /**
     * Các giá trị cờ cao điểm mà một loại dịch vụ có thể mang.
     *
     * <p>Hỏi thẳng {@link QuyTacGioCaoDiem} bằng hai thời điểm mẫu thay vì chép lại luật
     * "chỉ THOAI mới có cao điểm" vào test. Chép lại thì khi luật đổi, test vẫn xanh theo
     * luật cũ — đúng kiểu mù mà cả lớp test này sinh ra để tránh.</p>
     */
    private static Set<Boolean> coTheMangCoCaoDiem(LoaiDichVu dichVu) {
        LocalDateTime trongKhung = LocalDate.of(2026, 6, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY)).atTime(10, 0);
        LocalDateTime ngoaiKhung = LocalDate.of(2026, 6, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY)).atTime(3, 0);

        Set<Boolean> ketQua = new LinkedHashSet<>();
        ketQua.add(QuyTacGioCaoDiem.apDung(dichVu, trongKhung));
        ketQua.add(QuyTacGioCaoDiem.apDung(dichVu, ngoaiKhung));
        return ketQua;
    }

    private static String moTa(LoaiDichVu dichVu, HuongCuocGoi huong, boolean gioCaoDiem) {
        return dichVu + " / " + huong + (gioCaoDiem ? " / giờ cao điểm" : " / giờ thường");
    }
}
