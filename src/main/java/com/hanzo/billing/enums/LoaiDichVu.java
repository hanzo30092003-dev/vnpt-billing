package com.hanzo.billing.enums;

/**
 * Loại dịch vụ viễn thông phát sinh cước.
 *
 * <p>Nhãn hiển thị thêm ở đợt làm lại giao diện: trước đó bốn màn hình in thẳng tên
 * hằng ra cho người dùng đọc — nhân viên giao dịch thấy chữ {@code THOAI} chứ không
 * thấy chữ "Cuộc gọi".
 */
public enum LoaiDichVu {
    THOAI("Cuộc gọi"),
    SMS("Tin nhắn"),
    DATA("Truy cập mạng");

    private final String nhan;

    LoaiDichVu(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
