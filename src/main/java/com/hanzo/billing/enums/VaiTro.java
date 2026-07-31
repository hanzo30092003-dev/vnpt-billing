package com.hanzo.billing.enums;

/**
 * Vai trò của tài khoản người dùng trong hệ thống.
 *
 * <p>Tên hằng ({@code name()}) là giá trị lưu trong CSDL và dùng để phân quyền,
 * còn {@code nhan} chỉ để hiển thị lên giao diện. Không đổi tên hằng vì cột
 * {@code nguoi_dung.vai_tro} trong CSDL là ENUM khớp theo tên.</p>
 */
public enum VaiTro {
    ADMIN("Quản trị viên"),
    NHAN_VIEN("Nhân viên giao dịch"),
    KE_TOAN("Kế toán");

    private final String nhan;

    VaiTro(String nhan) {
        this.nhan = nhan;
    }

    public String getNhan() {
        return nhan;
    }
}
