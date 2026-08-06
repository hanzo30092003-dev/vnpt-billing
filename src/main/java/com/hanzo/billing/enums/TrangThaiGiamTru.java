package com.hanzo.billing.enums;

/** Trạng thái áp dụng của khoản giảm trừ. */
public enum TrangThaiGiamTru {

    CHUA_AP_DUNG("Chưa áp dụng", "text-bg-secondary"),
    DA_AP_DUNG("Đã áp dụng", "text-bg-success");

    private final String nhan;
    private final String lopBadge;

    TrangThaiGiamTru(String nhan, String lopBadge) {
        this.nhan = nhan;
        this.lopBadge = lopBadge;
    }

    public String getNhan() {
        return nhan;
    }

    public String getLopBadge() {
        return lopBadge;
    }
}
