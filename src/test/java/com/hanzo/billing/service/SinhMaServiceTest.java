package com.hanzo.billing.service;

import com.hanzo.billing.repository.KhachHangRepository;
import com.hanzo.billing.service.impl.SinhMaServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử hàm sinh mã khách hàng.
 *
 * <p>Chạy hoàn toàn bằng Mockito, không khởi động Spring context và không cần CSDL,
 * nên nhanh và không phụ thuộc môi trường.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Sinh mã khách hàng")
class SinhMaServiceTest {

    @Mock
    private KhachHangRepository khachHangRepository;

    @InjectMocks
    private SinhMaServiceImpl sinhMaService;

    @Test
    @DisplayName("Chưa có khách hàng nào trong hệ thống thì mã đầu tiên phải là KH000001")
    void chuaCoMaNao_thiSinhMaDauTien() {
        when(khachHangRepository.timMaKhachHangLonNhat()).thenReturn(null);

        assertThat(sinhMaService.sinhMaKhachHang()).isEqualTo("KH000001");
    }

    @Test
    @DisplayName("Mã lớn nhất là KH000050 thì mã kế tiếp phải là KH000051")
    void maLonNhatLaKH000050_thiSinhKH000051() {
        when(khachHangRepository.timMaKhachHangLonNhat()).thenReturn("KH000050");

        assertThat(sinhMaService.sinhMaKhachHang()).isEqualTo("KH000051");
    }

    @Test
    @DisplayName("Mã có nhiều số 0 ở đầu (KH000007) vẫn tách đúng thành số 7 và sinh ra KH000008")
    void maCoSoKhongODau_thiVanTachDungPhanSo() {
        // Đây là trường hợp dễ sai nếu so sánh chuỗi thay vì ép về số:
        // parseInt("000007") phải cho ra 7, không phải lỗi định dạng.
        when(khachHangRepository.timMaKhachHangLonNhat()).thenReturn("KH000007");

        assertThat(sinhMaService.sinhMaKhachHang()).isEqualTo("KH000008");
    }
}
