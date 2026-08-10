package com.hanzo.billing.dto.baocao;

import com.hanzo.billing.enums.TrangThaiHoaDon;

import java.math.BigDecimal;

/** Một dòng của bảng "Top thuê bao cước cao nhất" trong một kỳ. */
public record DongTopThueBao(Long hoaDonId, String maHoaDon, String soThueBao, String tenKhachHang,
                             String maGoi, BigDecimal tongThanhToan, BigDecimal daThanhToan,
                             BigDecimal conNo, TrangThaiHoaDon trangThai) {
}
