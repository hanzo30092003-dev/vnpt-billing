package com.hanzo.billing.enums;

/** Trạng thái thanh toán của hóa đơn. */
public enum TrangThaiHoaDon {

    CHUA_TT("Chưa thanh toán", "text-bg-secondary"),
    TT_MOT_PHAN("Thanh toán một phần", "text-bg-warning"),
    DA_TT("Đã thanh toán", "text-bg-success"),
    QUA_HAN("Quá hạn", "text-bg-danger");

    private final String nhan;

    /**
     * Lớp CSS của badge Bootstrap.
     *
     * <p>Để ở enum chứ không rải điều kiện trong template: bốn trạng thái này xuất hiện ở
     * danh sách hóa đơn, chi tiết hóa đơn, công nợ và danh sách thanh toán — bốn chỗ, một
     * quy tắc màu. Rải ra thì sẽ có lúc bốn chỗ hiện bốn màu khác nhau cho cùng trạng thái.</p>
     */
    private final String lopBadge;

    TrangThaiHoaDon(String nhan, String lopBadge) {
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
