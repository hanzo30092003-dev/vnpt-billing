package com.hanzo.billing.dto;

import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.NhomTuoiNo;

import java.math.BigDecimal;

/**
 * Một dòng trên màn hình công nợ: hóa đơn còn nợ kèm số ngày quá hạn đã tính sẵn.
 *
 * <p>Số ngày quá hạn tính <b>một lần</b> ở service rồi mang theo, thay vì để template tự tính
 * từ {@code hanThanhToan}. Template tính lấy thì mỗi chỗ hiển thị lại là một cơ hội lệch, và
 * bảng aging sẽ có lúc không khớp với cột "số ngày" ngay cạnh nó.</p>
 */
public record DongCongNo(HoaDon hoaDon, long soNgayQuaHan, NhomTuoiNo nhomTuoiNo) {

    public BigDecimal conNo() {
        return hoaDon.getConNo();
    }
}
