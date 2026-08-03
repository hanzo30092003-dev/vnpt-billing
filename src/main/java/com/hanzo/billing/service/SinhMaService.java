package com.hanzo.billing.service;

/** Sinh mã định danh tự tăng cho các đối tượng nghiệp vụ. */
public interface SinhMaService {

    /**
     * Sinh mã khách hàng kế tiếp, định dạng {@code KH} + 6 chữ số.
     *
     * @return ví dụ {@code KH000051}
     */
    String sinhMaKhachHang();

    /**
     * Sinh mã hóa đơn kế tiếp của một kỳ, định dạng {@code HD} + {@code yyyyMM} + {@code -}
     * + 6 chữ số.
     *
     * <p>Số thứ tự đếm riêng theo từng kỳ, nên mỗi tháng luôn bắt đầu lại từ
     * {@code 000001}. Nhờ vậy nhìn mã là biết ngay hóa đơn thuộc kỳ nào.</p>
     *
     * @return ví dụ {@code HD202606-000001}
     */
    String sinhMaHoaDon(int thang, int nam);
}
