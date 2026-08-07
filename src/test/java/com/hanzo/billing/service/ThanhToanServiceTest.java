package com.hanzo.billing.service;

import com.hanzo.billing.dto.ThanhToanForm;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.enums.HinhThucThanhToan;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.NguoiDungRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.service.impl.ThanhToanServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Ghi nhận thanh toán")
class ThanhToanServiceTest {

    private static final Long HD_ID = 7L;

    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private HoaDonRepository hoaDonRepository;
    @Mock private NguoiDungRepository nguoiDungRepository;
    @Mock private SinhMaService sinhMaService;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks private ThanhToanServiceImpl service;

    private static HoaDon hoaDon(String tong, String daThu, String conNo,
                                 TrangThaiHoaDon trangThai) {
        HoaDon h = new HoaDon();
        h.setId(HD_ID);
        h.setMaHoaDon("HD202605-000007");
        h.setTongThanhToan(new BigDecimal(tong));
        h.setDaThanhToan(new BigDecimal(daThu));
        h.setConNo(new BigDecimal(conNo));
        h.setTrangThai(trangThai);
        return h;
    }

    private static ThanhToanForm form(String soTien) {
        ThanhToanForm f = new ThanhToanForm();
        f.setHoaDonId(HD_ID);
        f.setSoTien(new BigDecimal(soTien));
        f.setHinhThuc(HinhThucThanhToan.TIEN_MAT);
        f.setNgayThanhToan(LocalDate.now());
        return f;
    }

    private void sanSang(HoaDon h) {
        when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(h));
        when(sinhMaService.sinhMaThanhToan(any())).thenReturn("TT20260615-0001");
    }

    // =================================================================

    @Nested
    @DisplayName("Chuỗi cập nhật hóa đơn")
    class ChuoiCapNhat {

        @Test
        @DisplayName("1. Thanh toán 50% → TT_MOT_PHAN, con_no đúng")
        void thanhToanMotNua() {
            HoaDon h = hoaDon("1000000", "0", "1000000", TrangThaiHoaDon.CHUA_TT);
            sanSang(h);

            service.ghiNhan(form("500000"));

            assertThat(h.getDaThanhToan()).isEqualByComparingTo("500000");
            assertThat(h.getConNo()).isEqualByComparingTo("500000");
            assertThat(h.getTrangThai()).isEqualTo(TrangThaiHoaDon.TT_MOT_PHAN);
        }

        @Test
        @DisplayName("2. Thanh toán nốt phần còn lại → DA_TT, con_no = 0")
        void thanhToanNot() {
            HoaDon h = hoaDon("1000000", "500000", "500000", TrangThaiHoaDon.TT_MOT_PHAN);
            sanSang(h);

            service.ghiNhan(form("500000"));

            assertThat(h.getDaThanhToan()).isEqualByComparingTo("1000000");
            assertThat(h.getConNo()).isEqualByComparingTo("0");
            assertThat(h.getTrangThai()).isEqualTo(TrangThaiHoaDon.DA_TT);
        }

        @Test
        @DisplayName("3. Thanh toán trọn một lần → DA_TT ngay")
        void thanhToanTronMotLan() {
            HoaDon h = hoaDon("250000", "0", "250000", TrangThaiHoaDon.CHUA_TT);
            sanSang(h);

            service.ghiNhan(form("250000"));

            assertThat(h.getConNo()).isEqualByComparingTo("0");
            assertThat(h.getTrangThai()).isEqualTo(TrangThaiHoaDon.DA_TT);
        }

        /**
         * ⭐ Bất biến trung tâm của ràng buộc ②.
         *
         * <p>{@code con_no} phải luôn bằng {@code tong_thanh_toan − da_thanh_toan} sau
         * <b>mọi</b> lần ghi nhận, không phải chỉ ở lần đầu.</p>
         */
        @Test
        @DisplayName("4. ⭐ Bất biến con_no = tong_thanh_toan − da_thanh_toan sau mỗi lần thu")
        void batBienConNo() {
            HoaDon h = hoaDon("1000000", "0", "1000000", TrangThaiHoaDon.CHUA_TT);
            sanSang(h);

            for (String lan : new String[]{"300000", "200000", "450000", "50000"}) {
                service.ghiNhan(form(lan));
                assertThat(h.getConNo())
                        .as("Sau khi thu %s đ", lan)
                        .isEqualByComparingTo(
                                h.getTongThanhToan().subtract(h.getDaThanhToan()));
            }
            assertThat(h.getDaThanhToan()).isEqualByComparingTo("1000000");
            assertThat(h.getConNo()).isEqualByComparingTo("0");
            assertThat(h.getTrangThai()).isEqualTo(TrangThaiHoaDon.DA_TT);
        }

        /**
         * Hóa đơn quá hạn trả một phần thì về {@code TT_MOT_PHAN}: quá hạn là tình trạng của
         * <i>thời gian</i>, ba giá trị kia là tình trạng của <i>tiền</i>. Lịch chạy nền sẽ
         * đưa lại về {@code QUA_HAN} nếu vẫn còn nợ sau hạn.
         */
        @Test
        @DisplayName("5. Hóa đơn QUA_HAN trả một phần → TT_MOT_PHAN")
        void quaHanTraMotPhan() {
            HoaDon h = hoaDon("500000", "0", "500000", TrangThaiHoaDon.QUA_HAN);
            sanSang(h);

            service.ghiNhan(form("200000"));

            assertThat(h.getTrangThai()).isEqualTo(TrangThaiHoaDon.TT_MOT_PHAN);
            assertThat(h.getConNo()).isEqualByComparingTo("300000");
        }
    }

    @Nested
    @DisplayName("Ba chốt chặn")
    class ChotChan {

        @Test
        @DisplayName("6. ⭐ Không thanh toán vượt số còn nợ")
        void khongVuotConNo() {
            HoaDon h = hoaDon("500000", "400000", "100000", TrangThaiHoaDon.TT_MOT_PHAN);
            when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(h));

            assertThatThrownBy(() -> service.ghiNhan(form("100001")))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("vượt quá số còn nợ");

            verify(thanhToanRepository, never()).save(any());
            assertThat(h.getDaThanhToan()).isEqualByComparingTo("400000");
        }

        @Test
        @DisplayName("7. Không thanh toán hóa đơn đã DA_TT")
        void khongThanhToanHoaDonDaTraDu() {
            HoaDon h = hoaDon("500000", "500000", "0", TrangThaiHoaDon.DA_TT);
            when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(h));

            assertThatThrownBy(() -> service.ghiNhan(form("1000")))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã thanh toán đủ");

            verify(thanhToanRepository, never()).save(any());
        }

        @Test
        @DisplayName("8. Ngày thanh toán không được ở tương lai")
        void ngayKhongOTuongLai() {
            HoaDon h = hoaDon("500000", "0", "500000", TrangThaiHoaDon.CHUA_TT);
            when(hoaDonRepository.findById(HD_ID)).thenReturn(Optional.of(h));
            ThanhToanForm f = form("1000");
            f.setNgayThanhToan(LocalDate.now().plusDays(1));

            assertThatThrownBy(() -> service.ghiNhan(f))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("tương lai");

            verify(thanhToanRepository, never()).save(any());
        }

        @Test
        @DisplayName("9. Thu đúng bằng số còn nợ thì được — ranh giới không bị chặn nhầm")
        void thuDungBangConNo() {
            HoaDon h = hoaDon("500000", "400000", "100000", TrangThaiHoaDon.TT_MOT_PHAN);
            sanSang(h);

            service.ghiNhan(form("100000"));

            assertThat(h.getConNo()).isEqualByComparingTo("0");
            assertThat(h.getTrangThai()).isEqualTo(TrangThaiHoaDon.DA_TT);
        }
    }

    @Nested
    @DisplayName("Bản ghi giao dịch")
    class BanGhiGiaoDich {

        @Test
        @DisplayName("10. Ghi đủ mã giao dịch, số tiền, hình thức và nhật ký hệ thống")
        void ghiDuThongTin() {
            HoaDon h = hoaDon("500000", "0", "500000", TrangThaiHoaDon.CHUA_TT);
            sanSang(h);
            ThanhToanForm f = form("120000");
            f.setGhiChu("Thu tại quầy Ninh Kiều");

            service.ghiNhan(f);

            ArgumentCaptor<ThanhToan> bat = ArgumentCaptor.forClass(ThanhToan.class);
            verify(thanhToanRepository).save(bat.capture());
            ThanhToan gd = bat.getValue();

            assertThat(gd.getMaGiaoDich()).isEqualTo("TT20260615-0001");
            assertThat(gd.getSoTien()).isEqualByComparingTo("120000");
            assertThat(gd.getHinhThuc()).isEqualTo(HinhThucThanhToan.TIEN_MAT);
            assertThat(gd.getHoaDon()).isSameAs(h);
            assertThat(gd.getGhiChu()).isEqualTo("Thu tại quầy Ninh Kiều");
            assertThat(gd.getNgayThanhToan()).isNotNull();

            verify(nhatKyService).ghiNhatKy(anyString(), anyString(), any(), anyString());
        }

        @Test
        @DisplayName("11. Mã giao dịch sinh theo NGÀY của giao dịch, không phải ngày hôm nay")
        void maSinhTheoNgayGiaoDich() {
            HoaDon h = hoaDon("500000", "0", "500000", TrangThaiHoaDon.CHUA_TT);
            sanSang(h);
            ThanhToanForm f = form("1000");
            LocalDate homQua = LocalDate.now().minusDays(3);
            f.setNgayThanhToan(homQua);

            service.ghiNhan(f);

            verify(sinhMaService).sinhMaThanhToan(homQua);
        }
    }
}
