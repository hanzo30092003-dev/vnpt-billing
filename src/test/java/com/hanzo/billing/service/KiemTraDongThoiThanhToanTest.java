package com.hanzo.billing.service;

import com.hanzo.billing.dto.ThanhToanForm;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.HinhThucThanhToan;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ BẤT BIẾN THANH TOÁN DƯỚI TẢI ĐỒNG THỜI.
 *
 * <p><b>Vì sao phải có lớp này.</b> {@code KiemTraBatBienThanhToanTest} kiểm bất biến
 * {@code da_thanh_toan = SUM(thanh_toan.so_tien)} trên <i>toàn bộ</i> hóa đơn, từng dòng một —
 * nhưng nó chạy <b>sau</b>, trên dữ liệu đã đứng yên. Nghĩa là suốt tám phase, bất biến trung
 * tâm của cả dự án mới chỉ được chứng minh <b>khi có một người dùng tại một thời điểm</b>.
 * Không tài liệu nào nói ra giới hạn đó, và không phép kiểm nào chạm tới nó.</p>
 *
 * <p>Kịch bản thật rất tầm thường: hai thu ngân cùng mở một hóa đơn còn nợ 100.000 đ, mỗi
 * người thu 50.000 đ.</p>
 *
 * <pre>
 *   A đọc con_no = 100.000          B đọc con_no = 100.000
 *   A qua kiểm (50.000 ≤ 100.000)   B qua kiểm (50.000 ≤ 100.000)
 *   A ghi da_thanh_toan = 50.000    B ghi da_thanh_toan = 50.000   ← đè lên A
 * </pre>
 *
 * <p>Kết quả: <b>hai dòng thanh toán tổng 100.000 đ, nhưng {@code da_thanh_toan} chỉ 50.000 đ</b>
 * — khách trả đủ mà hệ thống ghi là còn nợ một nửa.</p>
 *
 * <p>{@code HoaDon.phienBan} ({@code @Version}) chặn việc đó: người ghi sau mang số phiên bản
 * cũ, câu UPDATE khớp 0 dòng, và nhận {@code ObjectOptimisticLockingFailureException}.</p>
 *
 * <h2>⚠️ Đối chứng đã chạy — và KHÔNG cho kết quả như dự đoán</h2>
 *
 * <p>Gỡ {@code @Version} khỏi {@code HoaDon} rồi chạy lại lớp này, <b>bất biến vẫn đúng</b>:
 * 2/12 luồng ghi được, {@code da_thanh_toan} = 10.338 = đúng tổng hai dòng thanh toán. Không
 * quan sát được lần mất nào.</p>
 *
 * <p>Nghĩa là kịch bản mất tiền ở trên <b>chưa được chứng minh là xảy ra thật</b> — nó suy ra
 * từ đọc mã. Nhiều khả năng khoá dòng của InnoDB, cộng với việc mỗi giao dịch chỉ chạy câu
 * SELECT khi tới lượt mình, đã xếp các lần đọc ra sau các lần ghi trước đó.</p>
 *
 * <p><b>Nhưng một lần không tái hiện được cũng không chứng minh là an toàn.</b> Cái đang bảo
 * vệ hệ thống là cách một hệ quản trị CSDL cụ thể xếp hàng, không phải điều gì trong mã. Giữ
 * {@code @Version} để biến tính chất <b>tình cờ</b> đó thành bảo đảm <b>tường minh</b>.</p>
 *
 * <p><b>Vậy lớp này còn kiểm gì?</b> Nó kiểm bất biến trung tâm <b>dưới tranh chấp</b> — điều
 * mà suốt tám phase chưa phép kiểm nào chạm tới, vì
 * {@code KiemTraBatBienThanhToanTest} chỉ chạy trên dữ liệu đã đứng yên. Nó không còn được
 * quảng cáo là "bằng chứng có lỗi", vì nó không phải.</p>
 *
 * <p><b>Hai lần phép kiểm này từng xanh rỗng trước khi dùng được:</b> bản đầu dùng 2 luồng —
 * hai luồng thả cùng lúc vẫn chạy nối đuôi nên không có tranh chấp nào để bắt; và lần kiểm
 * "còn {@code @Version} không" bằng {@code -match '@Version'} lại khớp cả chữ trong javadoc
 * nên báo là còn trong khi đã gỡ. Bài học 43.5, lần thứ mười và mười một.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306}.</p>
 *
 * <p><b>Lớp này KHÔNG để lại rác.</b> Nó chọn một hóa đơn thật đang còn nợ, thu tiền lên đó,
 * rồi xoá sạch mọi giao dịch nó vừa tạo và trả hóa đơn về đúng số cũ. Chạy bao nhiêu lần cũng
 * ra một kết quả và không làm bẩn dữ liệu demo.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.sql.init.mode=never"
})
@DisplayName("Bất biến thanh toán dưới tải đồng thời")
class KiemTraDongThoiThanhToanTest {

    private static final String GHI_CHU = "[test đồng thời] tự xoá sau khi chạy";

    @Autowired
    private ThanhToanService thanhToanService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ThanhToanRepository thanhToanRepository;

    /**
     * Số thu ngân cùng bấm một lúc.
     *
     * <p><b>Vì sao 12 chứ không phải 2.</b> Bản đầu của phép kiểm này dùng đúng 2 luồng, thả
     * cùng lúc bằng một {@link CountDownLatch} — và nó <b>XANH cả khi đã gỡ {@code @Version}
     * ra</b>, tức là không kiểm được gì. Lý do: thả cùng lúc không có nghĩa là chồng lên nhau.
     * Mỗi luồng còn phải xin kết nối từ bể, mở giao dịch, rồi mới chạy câu SELECT; với hai
     * luồng và giao dịch ngắn thì luồng này thường xong hẳn trước khi luồng kia kịp đọc, nên
     * không bao giờ có tranh chấp để mà bắt.</p>
     *
     * <p>Mất bản ghi chỉ xảy ra khi <b>cả hai cùng ĐỌC trước khi một trong hai kịp GHI</b>.
     * Muốn cửa sổ đó chắc chắn mở ra thì phải có đủ luồng. 12 luồng làm nó xảy ra ổn định.</p>
     */
    private static final int SO_LUONG = 12;

    @Test
    @DisplayName("Nhiều người cùng thu tiền một hóa đơn: bất biến không được lệch")
    void nhieuNguoiCungThu_batBienKhongLech() throws Exception {
        HoaDon hoaDon = chonHoaDonConNo();
        Long id = hoaDon.getId();

        BigDecimal daThuBanDau = khongNull(hoaDon.getDaThanhToan());
        BigDecimal conNoBanDau = hoaDon.getConNo();
        TrangThaiHoaDon trangThaiBanDau = hoaDon.getTrangThai();

        // Mỗi luồng thu một phần nhỏ -> xét RIÊNG LẺ thì mọi lần thu đều hợp lệ.
        // Đó chính là điều làm lỗi này nguy hiểm: không phép kiểm nghiệp vụ nào từ
        // chối được, vì tách ra thì lần nào cũng đúng.
        BigDecimal moiNguoiThu = conNoBanDau
                .divide(BigDecimal.valueOf(SO_LUONG * 2L), 0, java.math.RoundingMode.DOWN);
        if (moiNguoiThu.signum() <= 0) {
            moiNguoiThu = BigDecimal.ONE;
        }

        int soThanhCong = chayCungLuc(id, moiNguoiThu);

        try {
            HoaDon sau = hoaDonRepository.findById(id).orElseThrow();
            BigDecimal tongGiaoDich = tongThanhToanCuaHoaDon(id);

            System.out.printf("[DO] hoa don %d: %d/%d luong ghi duoc | "
                            + "da_thanh_toan=%s | tong giao dich=%s | moi luong thu=%s%n",
                    id, soThanhCong, SO_LUONG, khongNull(sau.getDaThanhToan()),
                    tongGiaoDich, moiNguoiThu);

            // ⭐ Đây mới là điều cần chứng minh. KHÔNG khẳng định "đúng N luồng thành
            // công" — con số đó phụ thuộc lịch chạy của máy và sẽ làm phép kiểm chập chờn.
            // Thứ phải đúng bất kể có bao nhiêu luồng thắng là BẤT BIẾN.
            assertThat(khongNull(sau.getDaThanhToan()))
                    .as("da_thanh_toan phải bằng đúng tổng các dòng thanh toán — "
                            + "bất biến trung tâm của dự án, xét NGAY SAU khi có tranh chấp "
                            + "(%d/%d luồng ghi thành công)", soThanhCong, SO_LUONG)
                    .isEqualByComparingTo(tongGiaoDich);

            assertThat(sau.getConNo())
                    .as("con_no = tong_thanh_toan − da_thanh_toan")
                    .isEqualByComparingTo(
                            sau.getTongThanhToan().subtract(khongNull(sau.getDaThanhToan())));

            assertThat(khongNull(sau.getDaThanhToan()))
                    .as("Số đã thu phải bằng số cũ cộng đúng số lần ghi thành công")
                    .isEqualByComparingTo(
                            daThuBanDau.add(moiNguoiThu.multiply(BigDecimal.valueOf(soThanhCong))));

            assertThat(soThanhCong)
                    .as("Phải có ít nhất một luồng ghi được, nếu không phép kiểm này "
                            + "xanh một cách rỗng")
                    .isGreaterThanOrEqualTo(1);
        } finally {
            donDep(id, daThuBanDau, conNoBanDau, trangThaiBanDau);
        }
    }

    // =================================================================
    // Trợ giúp
    // =================================================================

    /** Thả {@link #SO_LUONG} luồng cùng lúc vào {@code ghiNhan} trên cùng một hóa đơn. */
    private int chayCungLuc(Long hoaDonId, BigDecimal soTien) throws Exception {
        CountDownLatch vach = new CountDownLatch(1);
        ExecutorService may = Executors.newFixedThreadPool(SO_LUONG);

        Callable<Boolean> motLanThu = () -> {
            vach.await();
            try {
                thanhToanService.ghiNhan(dungForm(hoaDonId, soTien));
                return true;
            } catch (Exception ex) {
                return false;
            }
        };

        List<Future<Boolean>> ketQua = new java.util.ArrayList<>();
        for (int i = 0; i < SO_LUONG; i++) {
            ketQua.add(may.submit(motLanThu));
        }
        vach.countDown();

        int soThanhCong = 0;
        for (Future<Boolean> f : ketQua) {
            if (f.get(60, TimeUnit.SECONDS)) {
                soThanhCong++;
            }
        }

        may.shutdown();
        may.awaitTermination(10, TimeUnit.SECONDS);
        return soThanhCong;
    }

    private ThanhToanForm dungForm(Long hoaDonId, BigDecimal soTien) {
        ThanhToanForm form = new ThanhToanForm();
        form.setHoaDonId(hoaDonId);
        form.setSoTien(soTien);
        form.setHinhThuc(HinhThucThanhToan.TIEN_MAT);
        form.setNgayThanhToan(LocalDate.now());
        form.setGhiChu(GHI_CHU);
        return form;
    }

    /**
     * Chọn một hóa đơn còn nợ ít nhất 2 đồng — lấy từ CSDL chứ không chọn cứng theo id, để
     * thêm bớt dữ liệu mẫu không làm đổ phép kiểm.
     */
    private HoaDon chonHoaDonConNo() {
        return hoaDonRepository.findAll().stream()
                .filter(h -> h.getConNo() != null
                        && h.getConNo().compareTo(BigDecimal.valueOf(2)) >= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Không có hóa đơn nào còn nợ để kiểm — dữ liệu mẫu đã bị đổi?"));
    }

    private BigDecimal tongThanhToanCuaHoaDon(Long hoaDonId) {
        return thanhToanRepository.findAll().stream()
                .filter(t -> t.getHoaDon() != null && hoaDonId.equals(t.getHoaDon().getId()))
                .map(t -> khongNull(t.getSoTien()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Xoá giao dịch do chính phép kiểm này tạo, rồi trả hóa đơn về đúng số cũ. */
    private void donDep(Long hoaDonId, BigDecimal daThuCu, BigDecimal conNoCu,
                        TrangThaiHoaDon trangThaiCu) {
        List<com.hanzo.billing.entity.ThanhToan> rac = thanhToanRepository.findAll().stream()
                .filter(t -> t.getHoaDon() != null && hoaDonId.equals(t.getHoaDon().getId()))
                .filter(t -> GHI_CHU.equals(t.getGhiChu()))
                .toList();
        thanhToanRepository.deleteAll(rac);

        HoaDon hoaDon = hoaDonRepository.findById(hoaDonId).orElseThrow();
        hoaDon.setDaThanhToan(daThuCu);
        hoaDon.setConNo(conNoCu);
        hoaDon.setTrangThai(trangThaiCu);
        hoaDonRepository.save(hoaDon);
    }

    private static BigDecimal khongNull(BigDecimal so) {
        return so == null ? BigDecimal.ZERO : so;
    }
}
