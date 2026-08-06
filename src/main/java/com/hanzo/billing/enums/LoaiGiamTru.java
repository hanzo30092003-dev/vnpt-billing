package com.hanzo.billing.enums;

/** Lý do phát sinh khoản giảm trừ trên hóa đơn. */
public enum LoaiGiamTru {

    KHUYEN_MAI("Khuyến mại"),
    SU_CO_DICH_VU("Sự cố dịch vụ"),
    CHIET_KHAU_DN("Chiết khấu doanh nghiệp"),
    KHAC("Khác");

    private final String nhan;

    LoaiGiamTru(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
