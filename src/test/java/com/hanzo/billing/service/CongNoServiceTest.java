package com.hanzo.billing.service;

import com.hanzo.billing.dto.DongCongNo;
import com.hanzo.billing.dto.OTuoiNo;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.NhomTuoiNo;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.service.impl.CongNoServiceImpl;
import com.hanzo.billing.service.rating.ThamSoTinhCuoc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Công nợ và tuổi nợ")
class CongNoServiceTest {

    @Mock private HoaDonRepository hoaDonRepository;
    @Mock private ThueBaoService thueBaoService;

    @InjectMocks private CongNoServiceImpl service;

    private static long idTuTang = 1;

    /** Hóa đơn còn nợ, hạn thanh toán lùi {@code soNgayQuaHan} ngày so với hôm nay. */
    private static HoaDon hoaDon(long soNgayQuaHan, String conNo, TrangThaiThueBao trangThaiTb) {
        KhachHang kh = new KhachHang();
        kh.setMaKh("KH000001");
        kh.setTenKh("Khách thử");

        ThueBao tb = new ThueBao();
        tb.setId(idTuTang);
        tb.setSoThueBao("090000000" + (idTuTang % 10));
        tb.setTrangThai(trangThaiTb);

        KyCuoc ky = new KyCuoc();
        ky.setThang(5);
        ky.setNam(2026);

        HoaDon h = new HoaDon();
        h.setId(idTuTang++);
        h.setMaHoaDon(String.format("HD202605-%06d", h.getId()));
        h.setKhachHang(kh);
        h.setThueBao(tb);
        h.setKyCuoc(ky);
        h.setHanThanhToan(LocalDate.now().minusDays(soNgayQuaHan));
        h.setTongThanhToan(new BigDecimal(conNo));
        h.setDaThanhToan(BigDecimal.ZERO);
        h.setConNo(new BigDecimal(conNo));
        h.setTrangThai(TrangThaiHoaDon.QUA_HAN);
        return h;
    }

    // =================================================================

    @Nested
    @DisplayName("Chia nhóm tuổi nợ")
    class ChiaNhom {

        /**
         * ⭐ Ranh giới là chỗ dễ sai nhất của bảng aging: lệch một ngày ở một mốc là toàn bộ
         * bảng lệch mà tổng vẫn đúng, nên nhìn tổng không phát hiện được.
         */
        @ParameterizedTest(name = "quá hạn {0} ngày → {1}")
        @DisplayName("1. ⭐ Ranh giới từng nhóm, kiểm cả hai đầu mỗi khoảng")
        @CsvSource({
                "-5,  TRONG_HAN",
                "0,   TRONG_HAN",
                "1,   QUA_HAN_1_30",
                "30,  QUA_HAN_1_30",
                "31,  QUA_HAN_31_60",
                "60,  QUA_HAN_31_60",
                "61,  QUA_HAN_61_90",
                "90,  QUA_HAN_61_90",
                "91,  QUA_HAN_TREN_90",
                "365, QUA_HAN_TREN_90"
        })
        void ranhGioiTungNhom(long soNgay, NhomTuoiNo kyVong) {
            assertThat(NhomTuoiNo.cua(soNgay)).isEqualTo(kyVong);
        }

        /**
         * Đếm từ <b>ngày sau</b> hạn thanh toán: hóa đơn đến hạn đúng hôm nay thì chưa quá
         * hạn ngày nào. Lệch một ngày ở đây là mọi hóa đơn trong hạn bị đẩy sang nhóm quá
         * hạn 1–30.
         */
        @Test
        @DisplayName("2. Hóa đơn đến hạn đúng hôm nay chưa quá hạn ngày nào")
        void denHanHomNay_chuaQuaHan() {
            when(hoaDonRepository.timConNo(any())).thenReturn(List.of(
                    hoaDon(0, "100000", TrangThaiThueBao.HOAT_DONG)));

            DongCongNo dong = service.danhSachConNo(null, null).get(0);

            assertThat(dong.soNgayQuaHan()).isZero();
            assertThat(dong.nhomTuoiNo()).isEqualTo(NhomTuoiNo.TRONG_HAN);
            assertThat(dong.nhomTuoiNo().isQuaHan()).isFalse();
        }

        @Test
        @DisplayName("3. Bảng tuổi nợ luôn có đủ 5 nhóm, kể cả nhóm rỗng")
        void bangLuonDuNamNhom() {
            when(hoaDonRepository.timConNo(any())).thenReturn(List.of(
                    hoaDon(10, "100000", TrangThaiThueBao.HOAT_DONG)));

            List<OTuoiNo> bang = service.bangTuoiNo(null);

            assertThat(bang).hasSize(NhomTuoiNo.values().length);
            assertThat(bang).extracting(OTuoiNo::nhom)
                    .containsExactly(NhomTuoiNo.values());
            assertThat(bang.stream().filter(o -> o.soHoaDon() > 0)).hasSize(1);
        }

        /**
         * ⭐ Bảng aging phải khớp với danh sách: tổng tiền của năm nhóm bằng tổng còn nợ.
         *
         * <p>Kiểm chéo hai đường tính khác nhau trên cùng tập dữ liệu — nếu một bên chia nhóm
         * sai thì hai con số lệch nhau.</p>
         */
        @Test
        @DisplayName("4. ⭐ Tổng năm nhóm của bảng aging bằng tổng còn nợ")
        void tongNamNhomBangTongConNo() {
            when(hoaDonRepository.timConNo(any())).thenReturn(List.of(
                    hoaDon(-3, "10000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(5, "20000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(45, "30000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(75, "40000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(200, "50000", TrangThaiThueBao.HOAT_DONG)));

            BigDecimal tongTuBang = service.bangTuoiNo(null).stream()
                    .map(OTuoiNo::tongTien)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(tongTuBang).isEqualByComparingTo("150000");
            assertThat(tongTuBang).isEqualByComparingTo(service.tongConNo(null));
        }

        @Test
        @DisplayName("5. Danh sách sắp theo số ngày quá hạn giảm dần")
        void sapTheoNgayQuaHanGiamDan() {
            when(hoaDonRepository.timConNo(any())).thenReturn(List.of(
                    hoaDon(5, "10000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(120, "20000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(45, "30000", TrangThaiThueBao.HOAT_DONG)));

            assertThat(service.danhSachConNo(null, null))
                    .extracting(DongCongNo::soNgayQuaHan)
                    .containsExactly(120L, 45L, 5L);
        }

        @Test
        @DisplayName("6. Lọc theo nhóm tuổi nợ chỉ trả về hóa đơn của nhóm đó")
        void locTheoNhom() {
            when(hoaDonRepository.timConNo(any())).thenReturn(List.of(
                    hoaDon(5, "10000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(45, "20000", TrangThaiThueBao.HOAT_DONG),
                    hoaDon(200, "30000", TrangThaiThueBao.HOAT_DONG)));

            assertThat(service.danhSachConNo(null, NhomTuoiNo.QUA_HAN_31_60))
                    .hasSize(1)
                    .allMatch(d -> d.nhomTuoiNo() == NhomTuoiNo.QUA_HAN_31_60);
        }
    }

    @Nested
    @DisplayName("Đề xuất tạm ngừng")
    class DeXuatTamNgung {

        @Test
        @DisplayName("7. Chỉ đề xuất hóa đơn quá hạn TRÊN ngưỡng và thuê bao đang hoạt động")
        void chiDeXuatDungDoiTuong() {
            int nguong = ThamSoTinhCuoc.SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG;
            when(hoaDonRepository.timConNo(any())).thenReturn(List.of(
                    hoaDon(nguong, "10000", TrangThaiThueBao.HOAT_DONG),        // đúng ngưỡng → không
                    hoaDon(nguong + 1, "20000", TrangThaiThueBao.HOAT_DONG),    // vượt → có
                    hoaDon(100, "30000", TrangThaiThueBao.TAM_NGUNG_1C),        // đã tạm ngừng → không
                    hoaDon(100, "40000", TrangThaiThueBao.DA_THANH_LY)));       // đã thanh lý → không

            assertThat(service.deXuatTamNgung())
                    .hasSize(1)
                    .allMatch(d -> d.soNgayQuaHan() == nguong + 1L);
        }

        /**
         * ⭐ Dùng lại {@code ThueBaoService.chuyenTrangThai} của Phase 2 thay vì viết logic
         * mới: ma trận chuyển trạng thái và việc ghi {@code lich_su_thue_bao} phải đi qua
         * đúng một đường.
         */
        @Test
        @DisplayName("8. ⭐ Tạm ngừng gọi lại ThueBaoService.chuyenTrangThai với lý do tự động")
        void dungLaiChuyenTrangThai() {
            HoaDon h = hoaDon(30, "100000", TrangThaiThueBao.HOAT_DONG);
            when(hoaDonRepository.timKemQuanHe(h.getId())).thenReturn(Optional.of(h));

            service.tamNgungViNoCuoc(h.getId());

            verify(thueBaoService).chuyenTrangThai(
                    h.getThueBao().getId(),
                    TrangThaiThueBao.TAM_NGUNG_1C,
                    "Tạm ngừng do nợ cước quá hạn — hóa đơn " + h.getMaHoaDon());
        }

        @Test
        @DisplayName("9. Từ chối tạm ngừng khi hóa đơn đã thanh toán đủ")
        void tuChoiKhiDaTraDu() {
            HoaDon h = hoaDon(30, "100000", TrangThaiThueBao.HOAT_DONG);
            h.setConNo(BigDecimal.ZERO);
            when(hoaDonRepository.timKemQuanHe(h.getId())).thenReturn(Optional.of(h));

            assertThatThrownBy(() -> service.tamNgungViNoCuoc(h.getId()))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã thu đủ");

            verify(thueBaoService, never()).chuyenTrangThai(anyLong(), any(), anyString());
        }

        @Test
        @DisplayName("10. Từ chối tạm ngừng khi chưa vượt ngưỡng ngày")
        void tuChoiKhiChuaVuotNguong() {
            HoaDon h = hoaDon(ThamSoTinhCuoc.SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG,
                    "100000", TrangThaiThueBao.HOAT_DONG);
            when(hoaDonRepository.timKemQuanHe(h.getId())).thenReturn(Optional.of(h));

            assertThatThrownBy(() -> service.tamNgungViNoCuoc(h.getId()))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("chưa vượt ngưỡng");

            verify(thueBaoService, never()).chuyenTrangThai(anyLong(), any(), anyString());
        }
    }
}
