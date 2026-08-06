package com.hanzo.billing.dto;

import java.math.BigDecimal;

/**
 * Dòng tổng cuối bảng danh sách hóa đơn, tính trên <b>toàn bộ</b> kết quả lọc.
 *
 * <p>Cố ý không tính trên trang hiện tại: người dùng lọc ra một tập rồi muốn biết tổng của
 * <i>tập đó</i>, chứ không phải tổng của 20 dòng đang nhìn thấy.</p>
 *
 * <p>Ba con số đều <b>cộng dồn từ cột đã ghi</b> trên hóa đơn, không tính lại từ bảng thanh
 * toán — ràng buộc ② của Phase 5: {@code con_no} chỉ có một nguồn.</p>
 */
public record TongHopHoaDon(long soHoaDon,
                            BigDecimal tongPhatSinh,
                            BigDecimal tongDaThu,
                            BigDecimal tongConNo) {

    public static TongHopHoaDon rong() {
        return new TongHopHoaDon(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
