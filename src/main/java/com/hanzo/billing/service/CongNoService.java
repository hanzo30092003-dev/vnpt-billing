package com.hanzo.billing.service;

import com.hanzo.billing.dto.DongCongNo;
import com.hanzo.billing.dto.ThueBaoVuotHanMuc;
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

    /**
     * Thuê bao trả sau đang nợ vượt hạn mức tín dụng của chính nó.
     *
     * <p>Căn cứ tạm ngừng thứ hai, <b>độc lập</b> với {@link #deXuatTamNgung()}: cái kia xét
     * từng hóa đơn quá hạn bao nhiêu ngày, cái này cộng <b>toàn bộ</b> nợ của thuê bao rồi so
     * với hạn mức. Một khách có nhiều hóa đơn mới quá hạn vài ngày nhưng cộng lại đã vượt hạn
     * mức sẽ không lọt vào danh sách kia — mà đúng là trường hợp đáng chặn nhất.</p>
     */
    List<ThueBaoVuotHanMuc> thueBaoVuotHanMuc();

    byte[] xuatExcel(String khachHang, NhomTuoiNo nhomTuoiNo);
}
