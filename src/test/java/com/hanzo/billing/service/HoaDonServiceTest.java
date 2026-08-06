package com.hanzo.billing.service;

import com.hanzo.billing.dto.BoLocHoaDon;
import com.hanzo.billing.dto.TongHopHoaDon;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.repository.ChiTietHoaDonRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.service.impl.HoaDonServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Nghiệp vụ hóa đơn")
class HoaDonServiceTest {

    @Mock private HoaDonRepository hoaDonRepository;
    @Mock private ChiTietHoaDonRepository chiTietHoaDonRepository;
    @Mock private ThanhToanRepository thanhToanRepository;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks private HoaDonServiceImpl service;

    private static HoaDon hoaDon(String tong, String daThu, String conNo,
                                 TrangThaiHoaDon trangThai) {
        HoaDon h = new HoaDon();
        h.setMaHoaDon("HD202605-000001");
        h.setTongThanhToan(new BigDecimal(tong));
        h.setDaThanhToan(new BigDecimal(daThu));
        h.setConNo(new BigDecimal(conNo));
        h.setTrangThai(trangThai);
        h.setHanThanhToan(LocalDate.of(2026, 6, 15));
        return h;
    }

    @Nested
    @DisplayName("Chuyển trạng thái Quá hạn")
    class ChuyenQuaHan {

        @Test
        @DisplayName("1. Hóa đơn quá hạn còn nợ thì chuyển sang QUA_HAN")
        void quaHanConNo_thiChuyen() {
            HoaDon a = hoaDon("100000", "0", "100000", TrangThaiHoaDon.CHUA_TT);
            HoaDon b = hoaDon("200000", "50000", "150000", TrangThaiHoaDon.TT_MOT_PHAN);
            when(hoaDonRepository.timCanChuyenQuaHan(any())).thenReturn(List.of(a, b));

            int so = service.capNhatQuaHan();

            assertThat(so).isEqualTo(2);
            assertThat(a.getTrangThai()).isEqualTo(TrangThaiHoaDon.QUA_HAN);
            assertThat(b.getTrangThai()).isEqualTo(TrangThaiHoaDon.QUA_HAN);
            verify(hoaDonRepository).saveAll(any());
            verify(nhatKyService).ghiNhatKy(eq("CAP_NHAT_QUA_HAN"), eq("HOA_DON"),
                    isNull(), anyString());
        }

        @Test
        @DisplayName("2. Không có hóa đơn nào cần chuyển thì không ghi gì cả")
        void khongCoGiCanChuyen_thiKhongGhi() {
            when(hoaDonRepository.timCanChuyenQuaHan(any())).thenReturn(List.of());

            assertThat(service.capNhatQuaHan()).isZero();

            verify(hoaDonRepository, never()).saveAll(any());
            verify(nhatKyService, never()).ghiNhatKy(anyString(), anyString(), any(), anyString());
        }

        /**
         * ⭐ Bất biến quan trọng nhất của chức năng này.
         *
         * <p>Truy vấn loại hóa đơn đã thu đủ ở <b>hai</b> lớp — {@code con_no > 0} và
         * {@code trangThai <> DA_TT}. Test này khẳng định hợp đồng đó ở mức truy vấn: service
         * chỉ đổi đúng những gì repository trả về, nên nếu truy vấn lỏng ra thì phải sửa
         * truy vấn chứ không phải thêm lọc trong Java.</p>
         */
        @Test
        @DisplayName("3. ⭐ Truy vấn phải loại DA_TT và hóa đơn hết nợ ngay từ CSDL")
        void truyVanPhaiLoaiDaTt() {
            when(hoaDonRepository.timCanChuyenQuaHan(any())).thenReturn(List.of());

            service.capNhatQuaHan();

            // Mốc so sánh là ngày hôm nay, không phải một hằng số nào khác
            verify(hoaDonRepository).timCanChuyenQuaHan(LocalDate.now());
        }
    }

    @Nested
    @DisplayName("Dòng tổng theo bộ lọc")
    class DongTong {

        @Test
        @DisplayName("4. Cộng dồn trên TOÀN BỘ kết quả lọc, đọc từ cột đã ghi")
        void congDonToanBoKetQua() {
            when(hoaDonRepository.timTatCaCoLoc(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(
                            hoaDon("100000", "100000", "0", TrangThaiHoaDon.DA_TT),
                            hoaDon("200000", "50000", "150000", TrangThaiHoaDon.TT_MOT_PHAN),
                            hoaDon("300000", "0", "300000", TrangThaiHoaDon.QUA_HAN)));

            TongHopHoaDon tong = service.tongHop(new BoLocHoaDon());

            assertThat(tong.soHoaDon()).isEqualTo(3);
            assertThat(tong.tongPhatSinh()).isEqualByComparingTo("600000");
            assertThat(tong.tongDaThu()).isEqualByComparingTo("150000");
            assertThat(tong.tongConNo()).isEqualByComparingTo("450000");
        }

        /**
         * Ràng buộc ② của Phase 5: dòng tổng đọc {@code da_thanh_toan} và {@code con_no} đã
         * ghi trên hóa đơn, <b>không</b> gọi sang bảng thanh toán để cộng lại.
         */
        @Test
        @DisplayName("5. ⭐ Không đụng tới bảng thanh toán — con_no chỉ có một nguồn")
        void khongDungBangThanhToan() {
            when(hoaDonRepository.timTatCaCoLoc(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of(hoaDon("100000", "40000", "60000",
                            TrangThaiHoaDon.TT_MOT_PHAN)));

            service.tongHop(new BoLocHoaDon());

            verify(thanhToanRepository, never()).tinhTongDaThu(anyLong());
        }

        @Test
        @DisplayName("6. Không có hóa đơn nào thì tổng bằng 0, không ném lỗi")
        void khongCoHoaDon_tongBangKhong() {
            when(hoaDonRepository.timTatCaCoLoc(any(), any(), any(), any(), any(), any()))
                    .thenReturn(List.of());

            TongHopHoaDon tong = service.tongHop(new BoLocHoaDon());

            assertThat(tong.soHoaDon()).isZero();
            assertThat(tong.tongPhatSinh()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Bộ lọc")
    class BoLoc {

        @Test
        @DisplayName("7. Chuỗi query giữ nguyên bộ lọc khi chuyển trang")
        void chuoiQueryGiuBoLoc() {
            BoLocHoaDon boLoc = new BoLocHoaDon();
            boLoc.setKyCuocId(2L);
            boLoc.setTrangThai(TrangThaiHoaDon.QUA_HAN);
            boLoc.setSoThueBao("0901234501");
            boLoc.setTuSoTien(new BigDecimal("100000"));

            assertThat(boLoc.chuoiQuery())
                    .contains("&kyCuocId=2")
                    .contains("&trangThai=QUA_HAN")
                    .contains("&soThueBao=0901234501")
                    .contains("&tuSoTien=100000")
                    .doesNotContain("denSoTien");
        }

        @Test
        @DisplayName("8. Ô lọc để trống được chuẩn hoá thành null, không thành chuỗi rỗng")
        void oTrongChuanHoaThanhNull() {
            BoLocHoaDon boLoc = new BoLocHoaDon();
            boLoc.setKhachHang("   ");
            boLoc.setSoThueBao("");

            assertThat(boLoc.khachHangChuan()).isNull();
            assertThat(boLoc.soThueBaoChuan()).isNull();
            assertThat(boLoc.chuoiQuery()).isEmpty();
        }
    }
}
