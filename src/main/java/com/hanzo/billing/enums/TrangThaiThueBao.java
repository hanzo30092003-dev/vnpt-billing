package com.hanzo.billing.enums;

/**
 * Trạng thái vòng đời của thuê bao.
 *
 * <p>TAM_NGUNG_1C: tạm ngưng một chiều — chỉ nhận, không gọi đi.
 * TAM_NGUNG_2C: tạm ngưng hai chiều — khoá cả gọi đi lẫn gọi đến.</p>
 *
 * <p>{@code mauBadge} là hậu tố lớp CSS Bootstrap dùng cho badge trạng thái
 * trên danh sách thuê bao: xanh lá / vàng / cam / xám.</p>
 */
public enum TrangThaiThueBao {
    HOAT_DONG("Hoạt động", "success"),
    TAM_NGUNG_1C("Tạm ngưng 1 chiều", "warning"),
    TAM_NGUNG_2C("Tạm ngưng 2 chiều", "orange"),
    DA_THANH_LY("Đã thanh lý", "secondary");

    private final String nhan;
    private final String mauBadge;

    TrangThaiThueBao(String nhan, String mauBadge) {
        this.nhan = nhan;
        this.mauBadge = mauBadge;
    }

    public String getNhan() {
        return nhan;
    }

    public String getMauBadge() {
        return mauBadge;
    }
}
