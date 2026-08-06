package com.hanzo.billing.enums;

/** Hình thức khách hàng thanh toán hóa đơn. */
public enum HinhThucThanhToan {

    TIEN_MAT("Tiền mặt"),
    CHUYEN_KHOAN("Chuyển khoản"),
    VI_DIEN_TU("Ví điện tử");

    private final String nhan;

    HinhThucThanhToan(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
