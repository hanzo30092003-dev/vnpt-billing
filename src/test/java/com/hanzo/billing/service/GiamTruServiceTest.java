package com.hanzo.billing.service;

import com.hanzo.billing.dto.GiamTruForm;
import com.hanzo.billing.entity.GiamTru;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiGiamTru;
import com.hanzo.billing.enums.TrangThaiGiamTru;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.GiamTruRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.impl.GiamTruServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Khoản giảm trừ")
class GiamTruServiceTest {

    @Mock private GiamTruRepository giamTruRepository;
    @Mock private ThueBaoRepository thueBaoRepository;
    @Mock private KyCuocRepository kyCuocRepository;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks private GiamTruServiceImpl service;

    private static GiamTruForm form(String soTien, String tyLe) {
        GiamTruForm f = new GiamTruForm();
        f.setThueBaoId(41L);
        f.setKyCuocId(1L);
        f.setLoai(LoaiGiamTru.SU_CO_DICH_VU);
        f.setSoTien(soTien == null ? null : new BigDecimal(soTien));
        f.setTyLePhanTram(tyLe == null ? null : new BigDecimal(tyLe));
        return f;
    }

    private static ThueBao thueBao() {
        ThueBao tb = new ThueBao();
        tb.setId(41L);
        tb.setSoThueBao("0941234541");
        return tb;
    }

    private static KyCuoc ky(TrangThaiKyCuoc trangThai) {
        KyCuoc k = new KyCuoc();
        k.setId(1L);
        k.setThang(6);
        k.setNam(2026);
        k.setTrangThai(trangThai);
        return k;
    }

    // =================================================================

    @Nested
    @DisplayName("Chỉ một trong hai cách khai")
    class MotCachKhai {

        @Test
        @DisplayName("1. ⭐ Khai cả hai cách → form không hợp lệ")
        void khaiCaHai_khongHopLe() {
            assertThat(form("50000", "7.5").isChiMotCachKhai()).isFalse();
        }

        @Test
        @DisplayName("2. ⭐ Không khai gì → form không hợp lệ")
        void khongKhaiGi_khongHopLe() {
            assertThat(form(null, null).isChiMotCachKhai()).isFalse();
            assertThat(form("0", "0").isChiMotCachKhai()).isFalse();
        }

        @Test
        @DisplayName("3. Khai đúng một cách → hợp lệ")
        void khaiMotCach_hopLe() {
            assertThat(form("50000", null).isChiMotCachKhai()).isTrue();
            assertThat(form(null, "7.5").isChiMotCachKhai()).isTrue();
        }

        /**
         * Bản ghi lưu xuống chỉ được mang <b>một</b> cách khai. Nếu để cả hai cột cùng có giá
         * trị thì {@code BillingService.tinhGiamTru} phải chọn, và người nhập rất có thể hiểu
         * là cộng dồn.
         */
        @Test
        @DisplayName("4. ⭐ Lưu theo số tiền thì cột tỷ lệ phải để NULL, và ngược lại")
        void luuChiMotCot() {
            when(thueBaoRepository.findById(41L)).thenReturn(Optional.of(thueBao()));
            when(kyCuocRepository.findById(1L)).thenReturn(Optional.of(ky(TrangThaiKyCuoc.MO)));

            service.luu(form("50000", null));

            ArgumentCaptor<GiamTru> bat = ArgumentCaptor.forClass(GiamTru.class);
            verify(giamTruRepository).save(bat.capture());
            assertThat(bat.getValue().getSoTien()).isEqualByComparingTo("50000");
            assertThat(bat.getValue().getTyLePhanTram()).isNull();
            assertThat(bat.getValue().getTrangThai()).isEqualTo(TrangThaiGiamTru.CHUA_AP_DUNG);
        }

        @Test
        @DisplayName("5. Lưu theo tỷ lệ thì cột số tiền để NULL — KHÔNG quy đổi sẵn ở tầng này")
        void luuTheoTyLe_khongQuyDoiSan() {
            when(thueBaoRepository.findById(41L)).thenReturn(Optional.of(thueBao()));
            when(kyCuocRepository.findById(1L)).thenReturn(Optional.of(ky(TrangThaiKyCuoc.MO)));

            service.luu(form(null, "7.5"));

            ArgumentCaptor<GiamTru> bat = ArgumentCaptor.forClass(GiamTru.class);
            verify(giamTruRepository).save(bat.capture());
            assertThat(bat.getValue().getTyLePhanTram()).isEqualByComparingTo("7.5");
            assertThat(bat.getValue().getSoTien())
                    .as("Quy tỷ lệ thành tiền ở tầng này là tạo tầng làm tròn thứ hai — "
                            + "ràng buộc ① của Phase 5 cấm. Việc đó chỉ làm một lần lúc lập hóa đơn")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("Chốt chặn sửa và xoá")
    class ChotChan {

        @Test
        @DisplayName("6. Kỳ đã chốt thì không thêm được giảm trừ")
        void kyDaChot_khongThem() {
            when(kyCuocRepository.findById(1L)).thenReturn(
                    Optional.of(ky(TrangThaiKyCuoc.DA_CHOT)));

            assertThatThrownBy(() -> service.luu(form("50000", null)))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã chốt");

            verify(giamTruRepository, never()).save(any());
        }

        @Test
        @DisplayName("7. ⭐ Khoản DA_AP_DUNG không xoá được — số tiền đã nằm trong hóa đơn")
        void daApDung_khongXoa() {
            GiamTru gt = new GiamTru();
            gt.setId(9L);
            gt.setThueBao(thueBao());
            gt.setSoTien(new BigDecimal("50000"));
            gt.setTrangThai(TrangThaiGiamTru.DA_AP_DUNG);
            when(giamTruRepository.findById(9L)).thenReturn(Optional.of(gt));

            assertThatThrownBy(() -> service.xoa(9L))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã được áp vào hóa đơn");

            verify(giamTruRepository, never()).delete(any());
        }

        @Test
        @DisplayName("8. Khoản CHUA_AP_DUNG xoá được bình thường")
        void chuaApDung_xoaDuoc() {
            GiamTru gt = new GiamTru();
            gt.setId(9L);
            gt.setThueBao(thueBao());
            gt.setSoTien(new BigDecimal("50000"));
            gt.setTrangThai(TrangThaiGiamTru.CHUA_AP_DUNG);
            when(giamTruRepository.findById(9L)).thenReturn(Optional.of(gt));

            service.xoa(9L);

            verify(giamTruRepository).delete(gt);
        }
    }
}
