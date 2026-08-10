package com.hanzo.billing.dto.baocao;

import com.hanzo.billing.util.DinhDangTien;

import java.math.BigDecimal;

/** Một dòng của báo cáo doanh thu theo gói cước trong một kỳ. */
public record DongDoanhThuGoi(String maGoi, String tenGoi, long soHoaDon,
                              BigDecimal phatSinh, BigDecimal daThu) {

    /**
     * Tỷ trọng của gói này trên tổng doanh thu kỳ.
     *
     * <p>Truyền tổng vào từ ngoài thay vì để record tự tính: một dòng không biết gì về các
     * dòng còn lại, và nếu cho nó tự truy vấn tổng thì mỗi dòng là một truy vấn.</p>
     */
    public BigDecimal tyTrong(BigDecimal tongPhatSinh) {
        return DinhDangTien.tyLePhanTram(phatSinh, tongPhatSinh);
    }
}
