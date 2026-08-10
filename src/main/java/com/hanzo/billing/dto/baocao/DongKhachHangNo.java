package com.hanzo.billing.dto.baocao;

import java.math.BigDecimal;

/** Một dòng của bảng "Top khách hàng nợ nhiều nhất". */
public record DongKhachHangNo(Long khachHangId, String maKh, String tenKh,
                              long soHoaDonNo, BigDecimal tongConNo) {
}
