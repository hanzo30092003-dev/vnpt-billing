package com.hanzo.billing.enums;

/** Hình thức thanh toán cước của thuê bao. */
public enum LoaiThueBao {
    TRA_TRUOC("Trả trước"),
    TRA_SAU("Trả sau");

    private final String nhan;

    LoaiThueBao(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
