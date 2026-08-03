package com.hanzo.billing.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Kết quả một lần chạy lập hóa đơn cho một kỳ.
 *
 * <p><b>Lưu ý khi đọc {@code tongDoanhThu} ở Phase 4C:</b> hóa đơn chưa trừ ưu đãi của gói
 * cước — mục 4D mới làm việc đó. Con số này là mức <b>trần</b>, sau 4D sẽ giảm.</p>
 */
@Getter
@Setter
public class KetQuaBilling {

    /** Số hóa đơn lập mới trong lần chạy này. */
    private int soHoaDonTao;

    /** Thuê bao bị bỏ qua vì đã có hóa đơn của kỳ. */
    private int soBoQuaDaCo;

    /** Thuê bao bị bỏ qua vì không thuộc diện lập hóa đơn (đã thanh lý trước kỳ...). */
    private int soBoQuaKhongDuDieuKien;

    /** Tổng {@code tong_thanh_toan} của toàn bộ hóa đơn trong kỳ, đếm lại từ CSDL. */
    private BigDecimal tongDoanhThu = BigDecimal.ZERO;

    private long thoiGianMs;

    public int getTongXetDuyet() {
        return soHoaDonTao + soBoQuaDaCo + soBoQuaKhongDuDieuKien;
    }
}
