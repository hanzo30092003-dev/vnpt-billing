package com.hanzo.billing.enums;

/** Phân loại khách hàng: cá nhân hoặc doanh nghiệp. */
public enum LoaiKhachHang {
    CA_NHAN("Cá nhân"),
    DOANH_NGHIEP("Doanh nghiệp");

    private final String nhan;

    LoaiKhachHang(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
