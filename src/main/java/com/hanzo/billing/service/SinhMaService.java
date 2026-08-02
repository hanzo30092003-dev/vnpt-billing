package com.hanzo.billing.service;

/** Sinh mã định danh tự tăng cho các đối tượng nghiệp vụ. */
public interface SinhMaService {

    /**
     * Sinh mã khách hàng kế tiếp, định dạng {@code KH} + 6 chữ số.
     *
     * @return ví dụ {@code KH000051}
     */
    String sinhMaKhachHang();
}
