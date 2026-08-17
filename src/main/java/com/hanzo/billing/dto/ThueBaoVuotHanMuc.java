package com.hanzo.billing.dto;

import java.math.BigDecimal;

/**
 * Một thuê bao trả sau đang nợ vượt hạn mức tín dụng của chính nó.
 *
 * <p><b>Vì sao có record này.</b> Cột {@code thue_bao.han_muc_tin_dung} tồn tại từ Phase 1: có
 * trong bảng, có trên form đăng ký, có hiển thị ở màn hình chi tiết thuê bao — nhưng suốt tám
 * phase <b>không một dòng mã nào đọc nó để chặn việc gì</b>. Một cột chết như vậy tệ hơn là
 * không có cột: người dùng nhập số vào đó và tin rằng hệ thống đang canh giúp mình.</p>
 *
 * <p><b>Vì sao đơn vị là THUÊ BAO chứ không phải hóa đơn.</b> {@code deXuatTamNgung()} đang có
 * xét từng <i>hóa đơn</i> quá hạn bao nhiêu ngày. Hạn mức tín dụng thì khác hẳn: nó nói
 * <i>"khách này được nợ tối đa bấy nhiêu"</i>, tức phải cộng <b>toàn bộ</b> hóa đơn chưa trả
 * của thuê bao lại rồi mới so. Một khách có bốn hóa đơn, mỗi cái mới quá hạn vài ngày nhưng
 * cộng lại đã vượt hạn mức, sẽ <b>không</b> lọt vào danh sách theo ngày quá hạn — nhưng đúng
 * là trường hợp đáng chặn nhất.</p>
 *
 * <p>Đây là hai <b>căn cứ độc lập</b> để đề xuất tạm ngừng, không phải hai cách viết của cùng
 * một luật; giữ riêng để mỗi cái nói rõ lý do của nó.</p>
 *
 * <p>Chỉ áp cho thuê bao <b>trả sau</b>: trả trước tiêu tiền đã nạp nên không có khái niệm cho
 * nợ, và dữ liệu thật cũng cho thấy cả 20 thuê bao trả trước đều có hạn mức bằng 0.</p>
 */
public record ThueBaoVuotHanMuc(
        Long thueBaoId,
        String soThueBao,
        Long khachHangId,
        String tenKhachHang,
        BigDecimal hanMuc,
        BigDecimal tongNo) {

    /** Số tiền nợ vượt quá hạn mức. */
    public BigDecimal vuot() {
        return tongNo.subtract(hanMuc);
    }

    /**
     * Tỷ lệ nợ trên hạn mức, tính bằng phần trăm.
     *
     * <p>Hạn mức bằng 0 trả về {@code null} chứ không phải vô cùng: câu truy vấn đã loại thuê
     * bao có hạn mức 0 rồi, nhưng nếu vì lý do nào đó lọt vào thì trả {@code null} để màn hình
     * hiện dấu gạch, chứ không chia cho 0.
     */
    public BigDecimal tyLePhanTram() {
        if (hanMuc == null || hanMuc.signum() == 0) {
            return null;
        }
        return tongNo.multiply(BigDecimal.valueOf(100))
                .divide(hanMuc, 0, java.math.RoundingMode.HALF_UP);
    }
}
