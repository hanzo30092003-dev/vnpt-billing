package com.hanzo.billing.enums;

/**
 * Nguồn gốc của bản ghi chi tiết sử dụng đưa vào hệ thống.
 *
 * <p>Nhãn hiển thị thêm ở đợt làm lại giao diện — xem ghi chú ở {@link LoaiDichVu}.
 * Đây là enum lộ chữ kỹ thuật nặng nhất: màn hình tra cứu in ra {@code GENERATOR}
 * và {@code IMPORT_CSV} cho nhân viên giao dịch đọc.
 */
public enum NguonCdr {
    GENERATOR("Máy tạo thử"),
    IMPORT_CSV("Nhập từ file Excel"),
    NHAP_TAY("Nhập tay");

    private final String nhan;

    NguonCdr(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
