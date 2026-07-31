package com.hanzo.billing.enums;

/** Hình thức nạp tiền vào tài khoản thuê bao trả trước. */
public enum HinhThucNapTien {
    THE_CAO("Thẻ cào"),
    CHUYEN_KHOAN("Chuyển khoản"),
    TAI_QUAY("Tại quầy");

    private final String nhan;

    HinhThucNapTien(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
