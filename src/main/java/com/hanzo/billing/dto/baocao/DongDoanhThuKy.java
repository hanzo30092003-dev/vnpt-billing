package com.hanzo.billing.dto.baocao;

import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.util.DinhDangTien;

import java.math.BigDecimal;

/**
 * Một dòng của báo cáo doanh thu theo kỳ.
 *
 * <p>Bốn cột tiền đều <b>đọc thẳng</b> từ {@code hoa_don} chứ không tính lại từ bảng thanh
 * toán — ràng buộc ② của Phase 5. Truy vấn dựng record này bằng {@code SELECT new ...} nên
 * phép cộng chạy trong CSDL, không load entity lên rồi cộng trong Java (yêu cầu D.4).</p>
 *
 * @param soHoaDon đếm bằng {@code COUNT(h.id)} chứ không {@code COUNT(*)}: kỳ chưa có hóa đơn
 *                 nào vẫn phải hiện ra với số 0, mà {@code LEFT JOIN} khi đó trả một dòng
 *                 rỗng và {@code COUNT(*)} sẽ đếm nhầm thành 1
 */
public record DongDoanhThuKy(Long kyId, Integer thang, Integer nam, TrangThaiKyCuoc trangThai,
                             long soHoaDon, BigDecimal phatSinh, BigDecimal daThu,
                             BigDecimal conNo) {

    public String nhanKy() {
        return thang + "/" + nam;
    }

    /** Tỷ lệ thu được; {@code null} khi kỳ chưa phát sinh đồng nào. */
    public BigDecimal tyLeThu() {
        return DinhDangTien.tyLePhanTram(daThu, phatSinh);
    }
}
