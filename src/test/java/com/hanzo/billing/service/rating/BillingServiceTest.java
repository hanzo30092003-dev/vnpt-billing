package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaBilling;
import com.hanzo.billing.entity.ChiTietHoaDon;
import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiDangKyGoi;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.DangKyGoiCuocRepository;
import com.hanzo.billing.repository.GiamTruRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.NhatKyService;
import com.hanzo.billing.service.SinhMaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử engine lập hóa đơn.
 *
 * <p>Trọng tâm là ba nhóm: prorate cước thuê bao theo số ngày thực dùng, quy tắc thuê bao
 * nào được lập hóa đơn, và <b>bất biến cộng dồn</b> — tầng này chỉ được cộng, tuyệt đối
 * không làm tròn lại.</p>
 *
 * <p>Năm ca prorate đầu dùng đúng ngày kích hoạt và cước gói của 5 thuê bao có thật trong
 * dữ liệu mẫu, đối chiếu với bảng giá trị kỳ vọng đã tính sẵn ở
 * {@code docs/PHASE-4-PLAN.md} mục 5.6.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Engine lập hóa đơn")
class BillingServiceTest {

    private static final Long KY_ID = 1L;

    @Mock private HoaDonRepository hoaDonRepository;
    @Mock private ChiTietSuDungRepository chiTietSuDungRepository;
    @Mock private DangKyGoiCuocRepository dangKyGoiCuocRepository;
    @Mock private GiamTruRepository giamTruRepository;
    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private ThueBaoRepository thueBaoRepository;
    @Mock private KyCuocRepository kyCuocRepository;
    @Mock private SinhMaService sinhMaService;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks
    private BillingService service;

    // =================================================================
    // NHÓM 1 — PRORATE CƯỚC THUÊ BAO
    // =================================================================

    @Nested
    @DisplayName("Prorate cước thuê bao theo số ngày thực dùng")
    class Prorate {

        /**
         * Kỳ 6/2026 có 30 ngày. Số ngày sử dụng tính BAO GỒM cả ngày kích hoạt, nên thuê
         * bao kích hoạt ngày 23 dùng 8 ngày (23→30) chứ không phải 7.
         */
        private BigDecimal cuocThueBaoCua(String ngayKichHoat, int cuocThang) {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, ngayKichHoat, null);
            chuanBiNguon(List.of(dangKyTronVen(tb, cuocThang)), List.of());
            chuanBiLuu();
            return service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO)).getCuocThueBao();
        }

        @Test
        @DisplayName("1. Kích hoạt 05/06, gói DN500 500.000 đ → 26/30 ngày → 433.333 đ")
        void kichHoat05_goiDN500() {
            assertThat(cuocThueBaoCua("2026-06-05", 500_000)).isEqualByComparingTo("433333");
        }

        @Test
        @DisplayName("2. Kích hoạt 11/06, gói MAX70 70.000 đ → 20/30 ngày → 46.667 đ")
        void kichHoat11_goiMAX70() {
            assertThat(cuocThueBaoCua("2026-06-11", 70_000)).isEqualByComparingTo("46667");
        }

        @Test
        @DisplayName("3. Kích hoạt 15/06, gói MAX150 150.000 đ → 16/30 ngày → 80.000 đ")
        void kichHoat15_goiMAX150() {
            assertThat(cuocThueBaoCua("2026-06-15", 150_000)).isEqualByComparingTo("80000");
        }

        @Test
        @DisplayName("4. Kích hoạt 17/06, gói MAX150 150.000 đ → 14/30 ngày → 70.000 đ")
        void kichHoat17_goiMAX150() {
            assertThat(cuocThueBaoCua("2026-06-17", 150_000)).isEqualByComparingTo("70000");
        }

        @Test
        @DisplayName("5. Kích hoạt 23/06, gói MAX150 150.000 đ → 8/30 ngày → 40.000 đ")
        void kichHoat23_goiMAX150() {
            assertThat(cuocThueBaoCua("2026-06-23", 150_000)).isEqualByComparingTo("40000");
        }

        @Test
        @DisplayName("6. Kích hoạt từ trước kỳ → thu đủ cước tháng, không prorate")
        void kichHoatTruocKy_thuDuCuocThang() {
            assertThat(cuocThueBaoCua("2025-03-01", 150_000)).isEqualByComparingTo("150000");
        }

        @Test
        @DisplayName("7. Dòng chi tiết ghi rõ số ngày prorate và ảnh chụp cước tháng của gói")
        void dongChiTiet_ghiRoSoNgayVaDonGia() {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2026-06-23", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));
            ChiTietHoaDon dong = hoaDon.getChiTiet().get(0);

            assertThat(dong.getKhoanMuc()).contains("8/30 ngày");
            assertThat(dong.getSoLuong()).isEqualByComparingTo("8");
            assertThat(dong.getDonVi()).isEqualTo("ngày");
            // Đơn giá là ảnh chụp cước tháng của gói, không phải cước đã prorate
            assertThat(dong.getDonGia()).isEqualByComparingTo("150000");
            assertThat(dong.getThanhTien()).isEqualByComparingTo("40000");
        }
    }

    // =================================================================
    // NHÓM 2 — THUÊ BAO NÀO ĐƯỢC LẬP HÓA ĐƠN
    // =================================================================

    @Nested
    @DisplayName("Quy tắc thuê bao được lập hóa đơn")
    class DienLapHoaDon {

        @Test
        @DisplayName("8. Tạm ngưng hai chiều trọn kỳ: vẫn có hóa đơn, cước thuê bao đủ, sử dụng 0")
        void tamNgung2C_vanCoHoaDon() {
            // Số vẫn giữ chỗ cho khách nên vẫn thu cước thuê bao, nhưng không phát sinh CDR
            ThueBao tb = thueBao(TrangThaiThueBao.TAM_NGUNG_2C, "2025-01-01", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));

            assertThat(hoaDon).isNotNull();
            assertThat(hoaDon.getCuocThueBao()).isEqualByComparingTo("150000");
            assertThat(hoaDon.getCuocThoai()).isEqualByComparingTo("0");
            assertThat(hoaDon.getCuocSms()).isEqualByComparingTo("0");
            assertThat(hoaDon.getCuocData()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("9. Đã thanh lý TRƯỚC kỳ: không lập hóa đơn")
        void daThanhLyTruocKy_khongLapHoaDon() {
            ThueBao tb = thueBao(TrangThaiThueBao.DA_THANH_LY, "2025-11-06", "2026-05-20");
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());

            assertThat(service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO))).isNull();
            verify(hoaDonRepository, never()).save(any());
        }

        @Test
        @DisplayName("10. Đã thanh lý GIỮA kỳ: có hóa đơn, prorate đến ngày huỷ")
        void daThanhLyGiuaKy_prorateDenNgayHuy() {
            // Huỷ ngày 15/06 → dùng 01/06 đến 15/06 = 15 ngày → nửa tháng
            ThueBao tb = thueBao(TrangThaiThueBao.DA_THANH_LY, "2025-01-01", "2026-06-15");
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));

            assertThat(hoaDon).isNotNull();
            assertThat(hoaDon.getCuocThueBao()).isEqualByComparingTo("75000");
        }

        @Test
        @DisplayName("11. Đã thanh lý nhưng THIẾU ngày huỷ: không lập hóa đơn, không đoán mò")
        void daThanhLyThieuNgayHuy_khongLapHoaDon() {
            // Dữ liệu hỏng. Nếu đoán "chưa huỷ" thì sẽ thu đủ cước tháng của một thuê bao
            // đã thanh lý — sai tiền mà không có dấu hiệu gì.
            ThueBao tb = thueBao(TrangThaiThueBao.DA_THANH_LY, "2025-01-01", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());

            assertThat(service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO))).isNull();
        }

        @Test
        @DisplayName("12. Kích hoạt SAU khi kỳ đã kết thúc: không lập hóa đơn")
        void kichHoatSauKy_khongLapHoaDon() {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2026-07-15", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());

            assertThat(service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO))).isNull();
        }

        @Test
        @DisplayName("13. Đã có hóa đơn của kỳ: bỏ qua, không tạo bản thứ hai")
        void daCoHoaDon_thiBoQua() {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            chuanBiNguonKhongExists(List.of(dangKyTronVen(tb, 150_000)), List.of());
            when(hoaDonRepository.existsByThueBaoIdAndKyCuocId(anyLong(), eq(KY_ID)))
                    .thenReturn(true);

            assertThat(service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO))).isNull();
            verify(hoaDonRepository, never()).save(any());
        }

        @Test
        @DisplayName("14. Lập hóa đơn cả kỳ chỉ xét thuê bao TRẢ SAU — trả trước không có hóa đơn tháng")
        void billingKy_chiXetTraSau() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            chuanBiChayCaKy(ky, List.of(), List.of());

            service.billingKy(ky);

            // Chốt chặn của quyết định 5.4: truy vấn lấy thuê bao phải lọc đúng TRA_SAU
            verify(thueBaoRepository).timTheoLoaiKemQuanHe(LoaiThueBao.TRA_SAU);
            verify(thueBaoRepository, never()).timTheoLoaiKemQuanHe(LoaiThueBao.TRA_TRUOC);
        }
    }

    // =================================================================
    // NHÓM 3 — TỔNG HỢP TIỀN VÀ BẤT BIẾN CỘNG DỒN
    // =================================================================

    @Nested
    @DisplayName("Tổng hợp tiền và bất biến cộng dồn")
    class TongHopTien {

        @Test
        @DisplayName("15. Không phát sinh CDR: vẫn có hóa đơn gồm cước thuê bao và VAT")
        void khongCoCdr_vanCoHoaDon() {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));

            assertThat(hoaDon.getTongTruocThue()).isEqualByComparingTo("150000");
            assertThat(hoaDon.getThueVat()).isEqualByComparingTo("15000");
            assertThat(hoaDon.getTongThanhToan()).isEqualByComparingTo("165000");
            assertThat(hoaDon.getChiTiet()).hasSize(1);
        }

        @Test
        @DisplayName("16. VAT đúng 10% và tổng thanh toán = trước thuế + VAT")
        void vatMuoiPhanTram() {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            // List.<Object[]>of chứ không phải List.of: một mảng Object[] truyền vào tham số
            // varargs sẽ bị TRẢI ra thành từng phần tử, cho ra List<Object> chứ không phải
            // List<Object[]> như mong đợi.
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.<Object[]>of(
                    tongHop(tb, LoaiDichVu.THOAI, "123456", 5000, 0, 40)));
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));

            BigDecimal truocThue = hoaDon.getTongTruocThue();
            assertThat(truocThue).isEqualByComparingTo("273456");            // 150000 + 123456
            assertThat(hoaDon.getThueVat()).isEqualByComparingTo("27346");   // 27345,6 → HALF_UP
            assertThat(hoaDon.getTongThanhToan())
                    .isEqualByComparingTo(truocThue.add(hoaDon.getThueVat()));
            assertThat(hoaDon.getConNo()).isEqualByComparingTo(hoaDon.getTongThanhToan());
            assertThat(hoaDon.getDaThanhToan()).isEqualByComparingTo("0");
            assertThat(hoaDon.getTrangThai()).isEqualTo(TrangThaiHoaDon.CHUA_TT);
        }

        @Test
        @DisplayName("17. ⭐ BẤT BIẾN CỘNG DỒN: cước sử dụng vào hóa đơn KHÔNG bị làm tròn lại")
        void batBienCongDon_khongLamTronLai() {
            // Cố ý đưa vào số lẻ tới hàng xu. Mục 4B luôn sinh số nguyên đồng, nhưng nếu
            // tầng này có thêm một lần làm tròn thì phần lẻ sẽ biến mất — và đó chính là
            // nguyên nhân khiến SUM(cuoc_phi) không khớp cột trên hóa đơn.
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 0)), List.<Object[]>of(
                    tongHop(tb, LoaiDichVu.THOAI, "1000.49", 3600, 0, 12),
                    tongHop(tb, LoaiDichVu.SMS, "250.51", 0, 3, 3),
                    tongHop(tb, LoaiDichVu.DATA, "99.99", 0, 4096, 2)));
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));

            assertThat(hoaDon.getCuocThoai()).isEqualByComparingTo("1000.49");
            assertThat(hoaDon.getCuocSms()).isEqualByComparingTo("250.51");
            assertThat(hoaDon.getCuocData()).isEqualByComparingTo("99.99");
            // Tổng trước thuế cũng chỉ là phép cộng, không làm tròn
            assertThat(hoaDon.getTongTruocThue()).isEqualByComparingTo("1350.99");
        }

        @Test
        @DisplayName("18. Dòng chi tiết cộng lại khớp đúng tổng trước thuế trên hóa đơn")
        void chiTietKhopCotTrenHoaDon() {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 50_000)), List.<Object[]>of(
                    tongHop(tb, LoaiDichVu.THOAI, "5000", 1800, 0, 10),
                    tongHop(tb, LoaiDichVu.DATA, "2500", 0, 2048, 1)));
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));

            // 3 dòng: cước thuê bao, cước thoại, cước dữ liệu. Không có dòng SMS vì bằng 0.
            assertThat(hoaDon.getChiTiet()).hasSize(3);
            BigDecimal tongDong = hoaDon.getChiTiet().stream()
                    .map(ChiTietHoaDon::getThanhTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assertThat(tongDong).isEqualByComparingTo(hoaDon.getTongTruocThue());

            // Dữ liệu hiển thị theo MB, quy đổi lên từ 2048 KB
            ChiTietHoaDon dongData = hoaDon.getChiTiet().get(2);
            assertThat(dongData.getDonVi()).isEqualTo("MB");
            assertThat(dongData.getSoLuong()).isEqualByComparingTo("2");
            // Dòng cước sử dụng để trống đơn giá: tổng này gộp nhiều bậc giá khác nhau
            assertThat(dongData.getDonGia()).isNull();
        }

        @Test
        @DisplayName("19. Ngày lập là ngày cuối kỳ, hạn thanh toán là ngày 15 tháng kế tiếp")
        void ngayLapVaHanThanhToan() {
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            chuanBiNguon(List.of(dangKyTronVen(tb, 150_000)), List.of());
            chuanBiLuu();

            HoaDon hoaDon = service.tinhCuocThueBao(tb, ky(TrangThaiKyCuoc.MO));

            assertThat(hoaDon.getNgayLap()).isEqualTo(LocalDate.of(2026, 6, 30));
            assertThat(hoaDon.getHanThanhToan()).isEqualTo(LocalDate.of(2026, 7, 15));
        }
    }

    // =================================================================
    // NHÓM 4 — CHẠY CẢ KỲ, CHẠY LẠI VÀ HỦY
    // =================================================================

    @Nested
    @DisplayName("Chạy cả kỳ, chạy lại và hủy lập hóa đơn")
    class ChayVaHuy {

        @Test
        @DisplayName("20. Chạy lần hai: mọi thuê bao đã có hóa đơn → không tạo thêm bản nào")
        void chayLanHai_khongTaoThem() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            ThueBao tb = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            chuanBiChayCaKy(ky, List.of(tb), List.of(dangKyTronVen(tb, 150_000)));
            when(hoaDonRepository.existsByThueBaoIdAndKyCuocId(anyLong(), eq(KY_ID)))
                    .thenReturn(true);

            KetQuaBilling ketQua = service.billingKy(ky);

            assertThat(ketQua.getSoHoaDonTao()).isZero();
            assertThat(ketQua.getSoBoQuaDaCo()).isEqualTo(1);
            verify(hoaDonRepository, never()).save(any());
            // Số liệu kỳ đếm lại từ CSDL nên không bị tụt về 0
            assertThat(ky.getSoHoaDonTao()).isEqualTo(58);
        }

        @Test
        @DisplayName("21. Mã hóa đơn sinh liên tiếp không trùng nhau")
        void maHoaDonKhongTrung() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            ThueBao tb1 = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            ThueBao tb2 = thueBao(TrangThaiThueBao.HOAT_DONG, "2025-01-01", null);
            tb2.setId(999L);
            tb2.setSoThueBao("0900000999");

            chuanBiChayCaKy(ky, List.of(tb1, tb2),
                    List.of(dangKyTronVen(tb1, 150_000), dangKyTronVen(tb2, 150_000)));
            when(hoaDonRepository.existsByThueBaoIdAndKyCuocId(anyLong(), eq(KY_ID)))
                    .thenReturn(false);
            when(sinhMaService.sinhMaHoaDon(6, 2026))
                    .thenReturn("HD202606-000001", "HD202606-000002");
            when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(loi -> loi.getArgument(0));

            service.billingKy(ky);

            ArgumentCaptor<HoaDon> batLay = ArgumentCaptor.forClass(HoaDon.class);
            verify(hoaDonRepository, times(2)).save(batLay.capture());
            assertThat(batLay.getAllValues())
                    .extracting(HoaDon::getMaHoaDon)
                    .containsExactly("HD202606-000001", "HD202606-000002")
                    .doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("22. Kỳ còn bản ghi CHƯA TÍNH cước: từ chối lập hóa đơn")
        void conCdrChuaTinh_thiTuChoi() {
            // Lập hóa đơn trên dữ liệu mới định giá một phần sẽ cho ra hóa đơn thiếu tiền,
            // vẫn phát hành bình thường, không lỗi, không cảnh báo.
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            when(chiTietSuDungRepository.demTheoKhoangVaTrangThai(
                    any(LocalDateTime.class), any(LocalDateTime.class),
                    eq(TrangThaiTinhCuoc.CHUA_TINH))).thenReturn(137L);

            assertThatThrownBy(() -> service.billingKy(ky))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("137")
                    .hasMessageContaining("chưa tính cước");

            verify(hoaDonRepository, never()).save(any());
        }

        @Test
        @DisplayName("23. Kỳ đã chốt: từ chối lập hóa đơn, không chạm vào CSDL")
        void kyDaChot_thiTuChoiLapHoaDon() {
            assertThatThrownBy(() -> service.billingKy(ky(TrangThaiKyCuoc.DA_CHOT)))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã chốt");

            verifyNoInteractions(hoaDonRepository);
            verifyNoInteractions(thueBaoRepository);
        }

        @Test
        @DisplayName("24. Hủy lập hóa đơn: xóa chi tiết TRƯỚC rồi mới xóa hóa đơn")
        void huyBilling_xoaChiTietTruoc() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            ky.setSoHoaDonTao(58);
            ky.setTongDoanhThu(new BigDecimal("100000000"));
            when(thanhToanRepository.demTheoKyCuoc(KY_ID)).thenReturn(0L);
            when(hoaDonRepository.xoaChiTietTheoKy(KY_ID)).thenReturn(174);
            when(hoaDonRepository.xoaTheoKy(KY_ID)).thenReturn(58);
            when(giamTruRepository.datLaiChuaApDungTheoKy(KY_ID)).thenReturn(0);

            int soHoaDon = service.huyBillingKy(ky);

            assertThat(soHoaDon).isEqualTo(58);
            assertThat(ky.getSoHoaDonTao()).isZero();
            assertThat(ky.getTongDoanhThu()).isEqualByComparingTo("0");

            // Thứ tự bắt buộc: JPQL không kích hoạt cascade nên đảo lại là vi phạm khóa ngoại
            InOrder thuTu = inOrder(hoaDonRepository);
            thuTu.verify(hoaDonRepository).xoaChiTietTheoKy(KY_ID);
            thuTu.verify(hoaDonRepository).xoaTheoKy(KY_ID);

            verify(nhatKyService).ghiNhatKy(eq("HUY_LAP_HOA_DON"), eq("KY_CUOC"),
                    eq(KY_ID), anyString());
        }

        @Test
        @DisplayName("25. Hủy lập hóa đơn khi đã ghi nhận thanh toán: bị từ chối")
        void huyBilling_daCoThanhToan_thiTuChoi() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            when(thanhToanRepository.demTheoKyCuoc(KY_ID)).thenReturn(12L);

            assertThatThrownBy(() -> service.huyBillingKy(ky))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("12 giao dịch thanh toán");

            verify(hoaDonRepository, never()).xoaTheoKy(anyLong());
        }

        @Test
        @DisplayName("26. Hủy lập hóa đơn khi kỳ đã chốt: bị từ chối")
        void huyBilling_kyDaChot_thiTuChoi() {
            assertThatThrownBy(() -> service.huyBillingKy(ky(TrangThaiKyCuoc.DA_CHOT)))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã chốt");

            verifyNoInteractions(hoaDonRepository);
            verifyNoInteractions(thanhToanRepository);
        }
    }

    // =================================================================
    // TIỆN ÍCH DỰNG DỮ LIỆU
    // =================================================================

    /** Nạp ba nguồn tra cứu và mặc định thuê bao CHƯA có hóa đơn của kỳ. */
    private void chuanBiNguon(List<DangKyGoiCuoc> dangKy, List<Object[]> tongHop) {
        chuanBiNguonKhongExists(dangKy, tongHop);
        when(hoaDonRepository.existsByThueBaoIdAndKyCuocId(anyLong(), eq(KY_ID)))
                .thenReturn(false);
    }

    private void chuanBiNguonKhongExists(List<DangKyGoiCuoc> dangKy, List<Object[]> tongHop) {
        when(dangKyGoiCuocRepository.timTatCaKemGoiVaThueBao()).thenReturn(dangKy);
        when(chiTietSuDungRepository.tongHopCuocTheoThueBao(KY_ID)).thenReturn(tongHop);
        when(giamTruRepository.timApDungChoKy(KY_ID)).thenReturn(List.of());
    }

    private void chuanBiLuu() {
        when(sinhMaService.sinhMaHoaDon(6, 2026)).thenReturn("HD202606-000001");
        when(hoaDonRepository.save(any(HoaDon.class))).thenAnswer(loi -> loi.getArgument(0));
    }

    /** Stub đủ cho một lượt chạy {@code billingKy}. */
    private void chuanBiChayCaKy(KyCuoc ky, List<ThueBao> danhSach,
                                 List<DangKyGoiCuoc> dangKy) {
        when(chiTietSuDungRepository.demTheoKhoangVaTrangThai(
                any(LocalDateTime.class), any(LocalDateTime.class), any()))
                .thenReturn(0L);
        when(dangKyGoiCuocRepository.timTatCaKemGoiVaThueBao()).thenReturn(dangKy);
        when(chiTietSuDungRepository.tongHopCuocTheoThueBao(KY_ID)).thenReturn(List.of());
        when(giamTruRepository.timApDungChoKy(KY_ID)).thenReturn(List.of());
        when(thueBaoRepository.timTheoLoaiKemQuanHe(LoaiThueBao.TRA_SAU)).thenReturn(danhSach);
        when(hoaDonRepository.countByKyCuocId(KY_ID)).thenReturn(58L);
        when(hoaDonRepository.tinhTongDoanhThuTheoKy(KY_ID))
                .thenReturn(new BigDecimal("123456789"));
    }

    private static KyCuoc ky(TrangThaiKyCuoc trangThai) {
        KyCuoc ky = new KyCuoc();
        ky.setId(KY_ID);
        ky.setThang(6);
        ky.setNam(2026);
        ky.setNgayBatDau(LocalDate.of(2026, 6, 1));
        ky.setNgayKetThuc(LocalDate.of(2026, 6, 30));
        ky.setTrangThai(trangThai);
        ky.setSoHoaDonTao(0);
        ky.setTongDoanhThu(BigDecimal.ZERO);
        return ky;
    }

    private static GoiCuoc goi(int cuocThueBaoThang) {
        GoiCuoc goi = new GoiCuoc();
        goi.setId(3L);
        goi.setMaGoi("MAX150");
        goi.setCuocThueBaoThang(BigDecimal.valueOf(cuocThueBaoThang));
        return goi;
    }

    private static ThueBao thueBao(TrangThaiThueBao trangThai, String ngayKichHoat,
                                   String ngayHuy) {
        KhachHang kh = new KhachHang();
        kh.setId(10L);

        ThueBao tb = new ThueBao();
        tb.setId(100L);
        tb.setSoThueBao("0900000100");
        tb.setKhachHang(kh);
        tb.setLoaiThueBao(LoaiThueBao.TRA_SAU);
        tb.setTrangThai(trangThai);
        tb.setNgayKichHoat(LocalDate.parse(ngayKichHoat));
        tb.setNgayHuy(ngayHuy == null ? null : LocalDate.parse(ngayHuy));
        return tb;
    }

    /** Bản ghi đăng ký gói phủ trọn mọi kỳ đang xét, với cước thuê bao tháng cho trước. */
    private static DangKyGoiCuoc dangKyTronVen(ThueBao thueBao, int cuocThueBaoThang) {
        DangKyGoiCuoc dk = new DangKyGoiCuoc();
        dk.setThueBao(thueBao);
        dk.setGoiCuoc(goi(cuocThueBaoThang));
        dk.setNgayBatDau(LocalDate.of(2020, 1, 1));
        dk.setNgayKetThuc(null);
        dk.setTrangThai(TrangThaiDangKyGoi.DANG_AP_DUNG);
        return dk;
    }

    /** Một dòng kết quả của truy vấn tổng hợp cước theo thuê bao. */
    private static Object[] tongHop(ThueBao tb, LoaiDichVu dichVu, String tongCuoc,
                                    long tongGiay, long tongSoLuong, long soBanGhi) {
        return new Object[]{tb.getId(), dichVu, new BigDecimal(tongCuoc),
                tongGiay, tongSoLuong, soBanGhi};
    }
}
