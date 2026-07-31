package com.hanzo.billing.enums;

/** Trạng thái bản ghi đăng ký gói cước của thuê bao. */
public enum TrangThaiDangKyGoi {
    DANG_AP_DUNG("Đang áp dụng", "success"),
    DA_KET_THUC("Đã kết thúc", "secondary");

    private final String nhan;
    private final String mauBadge;

    TrangThaiDangKyGoi(String nhan, String mauBadge) {
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
