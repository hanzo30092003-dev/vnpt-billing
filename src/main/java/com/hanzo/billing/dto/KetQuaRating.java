package com.hanzo.billing.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Kết quả một lần chạy tính cước cho một kỳ.
 *
 * <p><b>Lưu ý khi đọc {@code tongCuoc} ở Phase 4B:</b> đây là cước <b>gộp</b>, chưa trừ
 * ưu đãi của gói cước. Mục 4D sẽ áp ưu đãi và đánh cờ {@code mien_phi}, khi đó tổng này
 * sẽ giảm xuống. Con số của 4B không phải doanh thu.</p>
 */
@Getter
@Setter
public class KetQuaRating {

    /** Số bản ghi tính được cước và đã chuyển sang {@code DA_TINH}. */
    private int soThanhCong;

    /** Số bản ghi không tính được cước và đã chuyển sang {@code LOI}. */
    private int soLoi;

    /** Tổng cước của các bản ghi tính thành công trong lần chạy này. */
    private BigDecimal tongCuoc = BigDecimal.ZERO;

    private long thoiGianMs;

    public int getTongXuLy() {
        return soThanhCong + soLoi;
    }
}
