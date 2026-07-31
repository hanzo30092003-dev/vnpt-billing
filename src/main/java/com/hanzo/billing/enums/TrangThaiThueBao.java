package com.hanzo.billing.enums;

/**
 * Trạng thái vòng đời của thuê bao.
 *
 * <p>TAM_NGUNG_1C: tạm ngưng một chiều — chỉ nhận, không gọi đi.
 * TAM_NGUNG_2C: tạm ngưng hai chiều — khoá cả gọi đi lẫn gọi đến.</p>
 */
public enum TrangThaiThueBao {
    HOAT_DONG,
    TAM_NGUNG_1C,
    TAM_NGUNG_2C,
    DA_THANH_LY
}
