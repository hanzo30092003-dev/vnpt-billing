package com.hanzo.billing.service;

import com.hanzo.billing.dto.DongCongNo;
import com.hanzo.billing.dto.OTuoiNo;
import com.hanzo.billing.enums.NhomTuoiNo;

import java.math.BigDecimal;
import java.util.List;

public interface CongNoService {

    /** Hóa đơn còn nợ, sắp xếp theo số ngày quá hạn <b>giảm dần</b>. */
    List<DongCongNo> danhSachConNo(String khachHang, NhomTuoiNo nhomTuoiNo);

    /** Bảng tuổi nợ trên toàn bộ hóa đơn còn nợ khớp bộ lọc khách hàng. */
    List<OTuoiNo> bangTuoiNo(String khachHang);

    BigDecimal tongConNo(String khachHang);

    /**
     * Thuê bao được <b>đề xuất</b> tạm ngừng: có hóa đơn quá hạn trên ngưỡng và đang hoạt động.
     *
     * <p>Chỉ đề xuất, không tự chuyển trạng thái — xem
     * {@code ThamSoTinhCuoc.SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG}.</p>
     */
    List<DongCongNo> deXuatTamNgung();

    /**
     * Chuyển thuê bao sang tạm ngừng một chiều vì nợ cước.
     *
     * <p>Dùng lại {@code ThueBaoService.chuyenTrangThai} của Phase 2 để ma trận chuyển trạng
     * thái và việc ghi {@code lich_su_thue_bao} vẫn đi qua đúng một đường.</p>
     */
    void tamNgungViNoCuoc(Long hoaDonId);

    byte[] xuatExcel(String khachHang, NhomTuoiNo nhomTuoiNo);
}
