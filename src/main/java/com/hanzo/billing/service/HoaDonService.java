package com.hanzo.billing.service;

import com.hanzo.billing.dto.BoLocHoaDon;
import com.hanzo.billing.dto.TongHopHoaDon;
import com.hanzo.billing.entity.ChiTietHoaDon;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.ThanhToan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface HoaDonService {

    Page<HoaDon> timKiem(BoLocHoaDon boLoc, Pageable pageable);

    /** Tổng phát sinh / đã thu / còn nợ trên **toàn bộ** kết quả lọc, không chỉ trang hiện tại. */
    TongHopHoaDon tongHop(BoLocHoaDon boLoc);

    HoaDon layTheoId(Long id);

    List<ChiTietHoaDon> layKhoanMuc(Long hoaDonId);

    List<ThanhToan> layLichSuThanhToan(Long hoaDonId);

    byte[] xuatExcel(BoLocHoaDon boLoc);

    /**
     * Chuyển hóa đơn quá hạn thanh toán mà còn nợ sang {@code QUA_HAN}.
     *
     * <p>Nợ tài liệu từ mục 4F. Phase 4 cố ý không cài vì trạng thái hóa đơn phụ thuộc dữ
     * liệu thanh toán mà lúc đó chưa có.</p>
     *
     * @return số hóa đơn đã đổi trạng thái
     */
    int capNhatQuaHan();
}
