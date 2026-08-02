package com.hanzo.billing.service;

import com.hanzo.billing.entity.LichSuThueBao;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.*;
import com.hanzo.billing.service.impl.ThueBaoServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static com.hanzo.billing.enums.TrangThaiThueBao.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Kiểm thử ma trận chuyển trạng thái thuê bao — logic nghiệp vụ quan trọng nhất
 * đã viết ở Phase 2.
 *
 * <p>Chạy bằng Mockito, KHÔNG khởi động Spring context và không cần CSDL.</p>
 *
 * <p><b>Phủ kín toàn bộ ma trận 4×4 = 16 tổ hợp:</b></p>
 * <pre>
 *                  | HOAT_DONG | TAM_NGUNG_1C | TAM_NGUNG_2C | DA_THANH_LY
 *   HOAT_DONG      |     x     |      OK      |      OK      |     OK
 *   TAM_NGUNG_1C   |    OK     |       x      |      OK      |     OK
 *   TAM_NGUNG_2C   |    OK     |       x      |       x      |     OK
 *   DA_THANH_LY    |     x     |       x      |       x      |      x
 * </pre>
 * <p>Tổng cộng <b>8 tổ hợp hợp lệ</b> và <b>8 tổ hợp bị chặn</b>. Bốn ô trên đường
 * chéo là chuyển sang chính trạng thái đang có, cũng bị chặn vì không có ý nghĩa
 * nghiệp vụ. Ô {@code TAM_NGUNG_2C -> TAM_NGUNG_1C} bị chặn theo đúng ma trận đặc tả:
 * đã khoá hai chiều thì phải khôi phục hẳn hoặc thanh lý, không hạ xuống một chiều.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Ma trận chuyển trạng thái thuê bao")
class ThueBaoServiceTest {

    private static final Long ID_THUE_BAO = 1L;
    private static final String SO_THUE_BAO = "0901234501";
    private static final String LY_DO = "Lý do kiểm thử";

    @Mock private ThueBaoRepository thueBaoRepository;
    @Mock private KhachHangRepository khachHangRepository;
    @Mock private GoiCuocRepository goiCuocRepository;
    @Mock private DangKyGoiCuocRepository dangKyGoiCuocRepository;
    @Mock private LichSuThueBaoRepository lichSuThueBaoRepository;
    @Mock private NapTienRepository napTienRepository;
    @Mock private NguoiDungRepository nguoiDungRepository;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks
    private ThueBaoServiceImpl thueBaoService;

    /** Dựng một thuê bao ở trạng thái cho trước và gắn vào repository giả lập. */
    private ThueBao chuanBiThueBao(TrangThaiThueBao trangThaiBanDau) {
        ThueBao thueBao = new ThueBao();
        thueBao.setId(ID_THUE_BAO);
        thueBao.setSoThueBao(SO_THUE_BAO);
        thueBao.setTrangThai(trangThaiBanDau);
        when(thueBaoRepository.timTheoIdKemQuanHe(ID_THUE_BAO)).thenReturn(Optional.of(thueBao));
        return thueBao;
    }

    /** Khẳng định chuyển thành công: trạng thái đổi, có lưu và có ghi lịch sử. */
    private void khangDinhChuyenThanhCong(ThueBao thueBao, TrangThaiThueBao trangThaiMoi) {
        thueBaoService.chuyenTrangThai(ID_THUE_BAO, trangThaiMoi, LY_DO);

        assertThat(thueBao.getTrangThai()).isEqualTo(trangThaiMoi);
        verify(thueBaoRepository).save(thueBao);
        verify(lichSuThueBaoRepository).save(any(LichSuThueBao.class));
        verify(nhatKyService).ghiNhatKy(eq("CHUYEN_TRANG_THAI_THUE_BAO"), eq("THUE_BAO"),
                eq(ID_THUE_BAO), anyString());
    }

    /** Khẳng định bị chặn: ném ngoại lệ nghiệp vụ, trạng thái giữ nguyên, không ghi gì. */
    private void khangDinhBiChan(ThueBao thueBao, TrangThaiThueBao trangThaiMoi, String chuaTrongThongDiep) {
        TrangThaiThueBao trangThaiTruocKhiThu = thueBao.getTrangThai();

        assertThatThrownBy(() -> thueBaoService.chuyenTrangThai(ID_THUE_BAO, trangThaiMoi, LY_DO))
                .isInstanceOf(NghiepVuException.class)
                .hasMessageContaining(chuaTrongThongDiep);

        assertThat(thueBao.getTrangThai())
                .as("Trạng thái thuê bao không được thay đổi khi chuyển bị chặn")
                .isEqualTo(trangThaiTruocKhiThu);
        verify(thueBaoRepository, never()).save(any(ThueBao.class));
        verify(lichSuThueBaoRepository, never()).save(any(LichSuThueBao.class));
    }

    // =================================================================
    // NHÓM 1 — Từ HOAT_DONG (3 hợp lệ, 1 bị chặn)
    // =================================================================
    @Nested
    @DisplayName("Từ trạng thái Hoạt động")
    class TuHoatDong {

        @Test
        @DisplayName("Hoạt động → Tạm ngưng 1 chiều: hợp lệ, ghi lịch sử")
        void sangTamNgung1C() {
            khangDinhChuyenThanhCong(chuanBiThueBao(HOAT_DONG), TAM_NGUNG_1C);
        }

        @Test
        @DisplayName("Hoạt động → Tạm ngưng 2 chiều: hợp lệ, ghi lịch sử")
        void sangTamNgung2C() {
            khangDinhChuyenThanhCong(chuanBiThueBao(HOAT_DONG), TAM_NGUNG_2C);
        }

        @Test
        @DisplayName("Hoạt động → Đã thanh lý: hợp lệ và phải set ngày huỷ")
        void sangDaThanhLy_thiSetNgayHuy() {
            ThueBao thueBao = chuanBiThueBao(HOAT_DONG);

            khangDinhChuyenThanhCong(thueBao, DA_THANH_LY);

            assertThat(thueBao.getNgayHuy())
                    .as("Thanh lý là điểm kết thúc vòng đời nên bắt buộc ghi mốc thời gian")
                    .isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Hoạt động → Hoạt động: bị chặn vì đang ở đúng trạng thái đó rồi")
        void sangChinhNo_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(HOAT_DONG), HOAT_DONG, "không cần chuyển");
        }
    }

    // =================================================================
    // NHÓM 2 — Từ TAM_NGUNG_1C (3 hợp lệ, 1 bị chặn)
    // =================================================================
    @Nested
    @DisplayName("Từ trạng thái Tạm ngưng 1 chiều")
    class TuTamNgung1C {

        @Test
        @DisplayName("Tạm ngưng 1 chiều → Hoạt động: hợp lệ, đây là thao tác khôi phục dịch vụ")
        void sangHoatDong() {
            khangDinhChuyenThanhCong(chuanBiThueBao(TAM_NGUNG_1C), HOAT_DONG);
        }

        @Test
        @DisplayName("Tạm ngưng 1 chiều → Tạm ngưng 2 chiều: hợp lệ, nâng mức khoá do nợ cước kéo dài")
        void sangTamNgung2C() {
            khangDinhChuyenThanhCong(chuanBiThueBao(TAM_NGUNG_1C), TAM_NGUNG_2C);
        }

        @Test
        @DisplayName("Tạm ngưng 1 chiều → Đã thanh lý: hợp lệ và phải set ngày huỷ")
        void sangDaThanhLy_thiSetNgayHuy() {
            ThueBao thueBao = chuanBiThueBao(TAM_NGUNG_1C);

            khangDinhChuyenThanhCong(thueBao, DA_THANH_LY);

            assertThat(thueBao.getNgayHuy()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Tạm ngưng 1 chiều → Tạm ngưng 1 chiều: bị chặn vì đang ở đúng trạng thái đó rồi")
        void sangChinhNo_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(TAM_NGUNG_1C), TAM_NGUNG_1C, "không cần chuyển");
        }
    }

    // =================================================================
    // NHÓM 3 — Từ TAM_NGUNG_2C (2 hợp lệ, 2 bị chặn)
    // =================================================================
    @Nested
    @DisplayName("Từ trạng thái Tạm ngưng 2 chiều")
    class TuTamNgung2C {

        @Test
        @DisplayName("Tạm ngưng 2 chiều → Hoạt động: hợp lệ, khôi phục thẳng về hoạt động")
        void sangHoatDong() {
            khangDinhChuyenThanhCong(chuanBiThueBao(TAM_NGUNG_2C), HOAT_DONG);
        }

        @Test
        @DisplayName("Tạm ngưng 2 chiều → Đã thanh lý: hợp lệ và phải set ngày huỷ")
        void sangDaThanhLy_thiSetNgayHuy() {
            ThueBao thueBao = chuanBiThueBao(TAM_NGUNG_2C);

            khangDinhChuyenThanhCong(thueBao, DA_THANH_LY);

            assertThat(thueBao.getNgayHuy()).isEqualTo(LocalDate.now());
        }

        @Test
        @DisplayName("Tạm ngưng 2 chiều → Tạm ngưng 1 chiều: BỊ CHẶN, đã khoá 2 chiều thì không hạ xuống 1 chiều")
        void sangTamNgung1C_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(TAM_NGUNG_2C), TAM_NGUNG_1C, "Không thể chuyển thuê bao");
        }

        @Test
        @DisplayName("Tạm ngưng 2 chiều → Tạm ngưng 2 chiều: bị chặn vì đang ở đúng trạng thái đó rồi")
        void sangChinhNo_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(TAM_NGUNG_2C), TAM_NGUNG_2C, "không cần chuyển");
        }
    }

    // =================================================================
    // NHÓM 4 — Từ DA_THANH_LY: trạng thái cuối, chặn toàn bộ 4 ô
    // =================================================================
    @Nested
    @DisplayName("Từ trạng thái Đã thanh lý (trạng thái cuối)")
    class TuDaThanhLy {

        @Test
        @DisplayName("Đã thanh lý → Hoạt động: BỊ CHẶN, số thuê bao đã thu hồi và có thể đã cấp lại")
        void sangHoatDong_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(DA_THANH_LY), HOAT_DONG, "đã thanh lý");
        }

        @Test
        @DisplayName("Đã thanh lý → Tạm ngưng 1 chiều: BỊ CHẶN")
        void sangTamNgung1C_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(DA_THANH_LY), TAM_NGUNG_1C, "đã thanh lý");
        }

        @Test
        @DisplayName("Đã thanh lý → Tạm ngưng 2 chiều: BỊ CHẶN")
        void sangTamNgung2C_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(DA_THANH_LY), TAM_NGUNG_2C, "đã thanh lý");
        }

        @Test
        @DisplayName("Đã thanh lý → Đã thanh lý: bị chặn vì đang ở đúng trạng thái đó rồi")
        void sangChinhNo_thiBiChan() {
            khangDinhBiChan(chuanBiThueBao(DA_THANH_LY), DA_THANH_LY, "không cần chuyển");
        }
    }

    // =================================================================
    // Ràng buộc bắt buộc nhập lý do
    // =================================================================
    @Test
    @DisplayName("Không nhập lý do thì bị chặn ngay, không cần biết chuyển sang trạng thái nào")
    void khongNhapLyDo_thiBiChan() {
        assertThatThrownBy(() -> thueBaoService.chuyenTrangThai(ID_THUE_BAO, TAM_NGUNG_1C, "  "))
                .isInstanceOf(NghiepVuException.class)
                .hasMessageContaining("Vui lòng nhập lý do");

        verify(thueBaoRepository, never()).save(any(ThueBao.class));
    }
}
