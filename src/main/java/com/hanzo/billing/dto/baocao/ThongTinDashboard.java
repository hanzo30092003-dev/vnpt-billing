package com.hanzo.billing.dto.baocao;

import com.hanzo.billing.entity.ThanhToan;

import java.math.BigDecimal;
import java.util.List;

/**
 * Toàn bộ số liệu của dashboard, dựng trong <b>một</b> lần gọi service.
 *
 * <p>⚠️ Mọi con số ở đây <b>đọc từ cột đã ghi</b> — {@code hoa_don.con_no},
 * {@code hoa_don.tong_thanh_toan} — chứ không cộng lại từ bảng {@code thanh_toan}. Ràng buộc
 * ② của Phase 5: hai cách tính song song là hai nguồn sự thật, và chúng sẽ mâu thuẫn nhau
 * đúng vào lúc khó truy nhất.</p>
 *
 * @param kyGanNhat kỳ mới nhất <b>có hóa đơn</b>; {@code null} khi hệ thống chưa lập hóa đơn
 *                  nào — dashboard khi đó hiện "chưa có dữ liệu" thay vì vỡ
 */
public record ThongTinDashboard(long tongThueBao, long dangHoatDong, long thueBaoMoiTrongThang,
                                DongDoanhThuKy kyGanNhat, BigDecimal tongCongNo,
                                List<DongDoanhThuKy> doanhThuCacKy,
                                List<MucBieuDo> coCauGoiCuoc,
                                List<MucBieuDo> coCauTrangThai,
                                List<DongTopThueBao> topThueBao,
                                List<ThanhToan> giaoDichGanNhat) {

    public boolean chuaCoHoaDon() {
        return kyGanNhat == null;
    }
}
