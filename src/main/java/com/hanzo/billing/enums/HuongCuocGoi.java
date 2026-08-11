package com.hanzo.billing.enums;

/**
 * Hướng của cuộc gọi / tin nhắn, dùng để tra đơn giá.
 *
 * <p>Nhãn hiển thị thêm ở đợt làm lại giao diện — xem ghi chú ở {@link LoaiDichVu}.
 */
public enum HuongCuocGoi {
    NOI_MANG("Trong mạng"),
    NGOAI_MANG("Ngoài mạng"),
    QUOC_TE("Quốc tế");

    private final String nhan;

    HuongCuocGoi(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
