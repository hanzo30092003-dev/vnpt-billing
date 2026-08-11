package com.hanzo.billing.enums;

/**
 * Trạng thái xử lý tính tiền của một bản ghi chi tiết sử dụng.
 *
 * <p>Nhãn hiển thị và lớp badge thêm ở đợt làm lại giao diện — xem ghi chú ở
 * {@link LoaiDichVu}. Lớp badge để trong enum theo đúng quy tắc đã dùng cho
 * {@code TrangThaiHoaDon}: màu của một trạng thái khai đúng một chỗ.
 */
public enum TrangThaiTinhCuoc {
    CHUA_TINH("Chưa tính tiền", "text-bg-secondary"),
    DA_TINH("Đã tính tiền", "text-bg-success"),
    LOI("Lỗi", "text-bg-danger");

    private final String nhan;
    private final String lopBadge;

    TrangThaiTinhCuoc(String nhan, String lopBadge) {
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
