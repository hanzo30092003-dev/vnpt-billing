package com.hanzo.billing.service;

import com.hanzo.billing.dto.BoLocThanhToan;
import com.hanzo.billing.dto.ThanhToanForm;
import com.hanzo.billing.entity.ThanhToan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ThanhToanService {

    /**
     * Ghi nhận một giao dịch thanh toán và cập nhật lại hóa đơn.
     *
     * <p>Đây là <b>nơi duy nhất</b> ghi {@code hoa_don.da_thanh_toan}, {@code con_no} và
     * {@code trang_thai} — ràng buộc ② của Phase 5.</p>
     */
    ThanhToan ghiNhan(ThanhToanForm form);

    ThanhToan layTheoId(Long id);

    Page<ThanhToan> timKiem(BoLocThanhToan boLoc, Pageable pageable);

    /** Tổng tiền thu được trên toàn bộ kết quả lọc. */
    BigDecimal tongThu(BoLocThanhToan boLoc);

    byte[] xuatExcel(BoLocThanhToan boLoc);
}
