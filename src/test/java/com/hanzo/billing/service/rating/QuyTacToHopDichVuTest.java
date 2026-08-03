package com.hanzo.billing.service.rating;

import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử quy tắc tổ hợp (dịch vụ, hướng).
 *
 * <p>Ba tổ hợp bị chặn ở đây chính là ba tổ hợp đã sinh ra 251 bản ghi CDR không tra được
 * đơn giá trước Phase 4A. Xem {@code docs/PHASE-4-REPORT.md} mục 3.</p>
 */
@DisplayName("Quy tắc tổ hợp dịch vụ và hướng")
class QuyTacToHopDichVuTest {

    @Test
    @DisplayName("1. THOAI có đủ cả ba hướng")
    void thoai_coDuBaHuong() {
        assertThat(QuyTacToHopDichVu.huongHopLe(LoaiDichVu.THOAI))
                .containsExactlyInAnyOrder(HuongCuocGoi.values());
    }

    @Test
    @DisplayName("2. SMS có đủ cả ba hướng — tin nhắn quốc tế là dịch vụ có thật, có bảng giá riêng")
    void sms_coDuBaHuong() {
        assertThat(QuyTacToHopDichVu.huongHopLe(LoaiDichVu.SMS))
                .containsExactlyInAnyOrder(HuongCuocGoi.values());
        assertThat(QuyTacToHopDichVu.hopLe(LoaiDichVu.SMS, HuongCuocGoi.QUOC_TE)).isTrue();
    }

    @Test
    @DisplayName("3. DATA chỉ có NOI_MANG")
    void data_chiCoNoiMang() {
        assertThat(QuyTacToHopDichVu.huongHopLe(LoaiDichVu.DATA))
                .containsExactly(HuongCuocGoi.NOI_MANG);
    }

    @Test
    @DisplayName("4. DATA + NGOAI_MANG bị chặn: phiên data đi ra Internet, không có nội/ngoại mạng")
    void dataNgoaiMang_biChan() {
        assertThat(QuyTacToHopDichVu.hopLe(LoaiDichVu.DATA, HuongCuocGoi.NGOAI_MANG)).isFalse();
    }

    @Test
    @DisplayName("5. DATA + QUOC_TE bị chặn: đó là data roaming, ngoài phạm vi đồ án")
    void dataQuocTe_biChan() {
        assertThat(QuyTacToHopDichVu.hopLe(LoaiDichVu.DATA, HuongCuocGoi.QUOC_TE)).isFalse();
    }

    @Test
    @DisplayName("6. Tham số null thì không hợp lệ, không được ném NullPointerException")
    void thamSoNull_thiKhongHopLe() {
        assertThat(QuyTacToHopDichVu.hopLe(null, HuongCuocGoi.NOI_MANG)).isFalse();
        assertThat(QuyTacToHopDichVu.hopLe(LoaiDichVu.DATA, null)).isFalse();
    }

    @Test
    @DisplayName("7. Thông báo từ chối nêu rõ dịch vụ, hướng bị từ chối và các hướng được chấp nhận")
    void thongBaoTuChoi_neuRoLyDo() {
        String thongBao = QuyTacToHopDichVu.thongBaoKhongHopLe(
                LoaiDichVu.DATA, HuongCuocGoi.NGOAI_MANG);
        assertThat(thongBao)
                .contains("DATA")
                .contains("NGOAI_MANG")
                .contains("NOI_MANG");
    }

    @Test
    @DisplayName("8. Tập hướng trả về không sửa được từ bên ngoài")
    void tapHuongTraVe_khongSuaDuoc() {
        // Nếu trả thẳng tập nội bộ thì một lời gọi remove() ở đâu đó sẽ âm thầm
        // đổi luật cho toàn hệ thống.
        assertThat(QuyTacToHopDichVu.huongHopLe(LoaiDichVu.DATA))
                .isUnmodifiable();
    }
}
