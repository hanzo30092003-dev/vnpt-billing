package com.hanzo.billing.enums;

/** Trạng thái giao dịch của khách hàng. */
public enum TrangThaiKhachHang {
    HOAT_DONG("Hoạt động", "success"),
    NGUNG_GIAO_DICH("Ngừng giao dịch", "secondary");

    private final String nhan;
    /** Hậu tố lớp CSS của Bootstrap badge, ví dụ "success" -> bg-success. */
    private final String mauBadge;

    TrangThaiKhachHang(String nhan, String mauBadge) {
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
