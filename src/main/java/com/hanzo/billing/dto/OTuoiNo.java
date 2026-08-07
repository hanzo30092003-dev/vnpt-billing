package com.hanzo.billing.dto;

import com.hanzo.billing.enums.NhomTuoiNo;

import java.math.BigDecimal;

/** Một ô của bảng tuổi nợ: số hóa đơn và tổng tiền của một nhóm. */
public record OTuoiNo(NhomTuoiNo nhom, long soHoaDon, BigDecimal tongTien) {

    public static OTuoiNo rong(NhomTuoiNo nhom) {
        return new OTuoiNo(nhom, 0, BigDecimal.ZERO);
    }

    public OTuoiNo cong(BigDecimal soTien) {
        return new OTuoiNo(nhom, soHoaDon + 1, tongTien.add(soTien));
    }
}
