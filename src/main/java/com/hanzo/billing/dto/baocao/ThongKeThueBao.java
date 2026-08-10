package com.hanzo.billing.dto.baocao;

import com.hanzo.billing.util.DinhDangTien;

import java.math.BigDecimal;
import java.util.List;

/**
 * Số liệu của báo cáo thống kê thuê bao (C.4).
 *
 * @param mocThoiGian trục thời gian <b>gộp</b> của hai chuỗi "mới" và "rời mạng". Hai chuỗi
 *                    được ghép về cùng một trục ở tầng service chứ không để biểu đồ tự ghép:
 *                    tháng có thuê bao mới nhưng không có ai rời mạng phải hiện giá trị 0 chứ
 *                    không được làm lệch trục của chuỗi còn lại
 */
public record ThongKeThueBao(List<MucBieuDo> theoTrangThai, List<MucBieuDo> theoLoai,
                             List<String> mocThoiGian, List<Long> thueBaoMoi,
                             List<Long> thueBaoRoiMang,
                             long tongThueBao, long dangHoatDong, long daThanhLy) {

    /**
     * Tỷ lệ rời mạng = thuê bao đã thanh lý / tổng thuê bao.
     *
     * <p>Đây là tỷ lệ <b>luỹ kế</b> trên toàn bộ lịch sử, không phải tỷ lệ rời mạng theo tháng
     * của ngành viễn thông (vốn tính trên số thuê bao đầu kỳ). Ghi rõ để người đọc báo cáo
     * không so nhầm con số này với chỉ số churn của nhà mạng thật.</p>
     */
    public BigDecimal tyLeRoiMang() {
        return DinhDangTien.tyLePhanTram(BigDecimal.valueOf(daThanhLy),
                BigDecimal.valueOf(tongThueBao));
    }

    public boolean rong() {
        return tongThueBao == 0;
    }
}
