package com.hanzo.billing.service.baocao;

import com.hanzo.billing.dto.baocao.CoCauCuoc;
import com.hanzo.billing.dto.baocao.DongDoanhThuGoi;
import com.hanzo.billing.dto.baocao.DongDoanhThuKy;
import com.hanzo.billing.dto.baocao.DongKhachHangNo;
import com.hanzo.billing.dto.baocao.DongTopThueBao;
import com.hanzo.billing.dto.baocao.SanLuongDichVu;
import com.hanzo.billing.dto.baocao.ThongKeThueBao;
import com.hanzo.billing.dto.baocao.ThongTinDashboard;
import com.hanzo.billing.entity.KyCuoc;

import java.util.List;
import java.util.Optional;

/**
 * Số liệu cho dashboard và bảy báo cáo thống kê của Phase 6.
 *
 * <p><b>Chỉ ĐỌC.</b> Không phương thức nào ở đây ghi xuống CSDL, và không phương thức nào
 * tính lại một con số đã có cột riêng — đó là ràng buộc ② của Phase 5 áp cho tầng báo cáo.</p>
 */
public interface BaoCaoService {

    /** Toàn bộ số liệu dashboard trong một lần gọi. */
    ThongTinDashboard dashboard();

    /** C.1 — doanh thu từng kỳ, kể cả kỳ chưa lập hóa đơn nào. */
    List<DongDoanhThuKy> doanhThuTheoKy();

    /** C.2 — doanh thu theo gói cước trong một kỳ. */
    List<DongDoanhThuGoi> doanhThuTheoGoi(Long kyCuocId);

    /** C.3 — cơ cấu cước theo loại dịch vụ trong một kỳ. */
    CoCauCuoc coCauCuoc(Long kyCuocId);

    /** C.4 — thống kê thuê bao theo trạng thái, theo loại và biến động theo tháng. */
    ThongKeThueBao thongKeThueBao();

    /** C.5 — thuê bao cước cao nhất của một kỳ. */
    List<DongTopThueBao> topThueBaoCuocCao(Long kyCuocId, int soLuong);

    /** C.6 — sản lượng dịch vụ của một kỳ. */
    SanLuongDichVu sanLuong(Long kyCuocId);

    /**
     * C.6 — sản lượng kỳ <b>liền trước</b> để so sánh; rỗng nếu đây đã là kỳ đầu tiên.
     *
     * <p>"Liền trước" tính theo <b>tháng/năm</b> chứ không theo {@code id}: các kỳ được tạo
     * ra không theo thứ tự thời gian (kỳ 3 và 4 mang id 4 và 5, tạo sau kỳ 5, 6, 7), nên so
     * theo id sẽ ghép nhầm cặp kỳ.
     */
    Optional<KyCuoc> kyLienTruoc(Long kyCuocId);

    /** C.7 — khách hàng nợ nhiều nhất. */
    List<DongKhachHangNo> topKhachHangNo(int soLuong);

    /** Danh sách kỳ cho ô chọn của các báo cáo có tham số kỳ. */
    List<KyCuoc> danhSachKy();

    /** Kỳ mới nhất có hóa đơn — giá trị mặc định của ô chọn kỳ. */
    Optional<KyCuoc> kyMacDinh();
}
