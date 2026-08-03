package com.hanzo.billing.service;

import com.hanzo.billing.dto.BoLocCdr;
import com.hanzo.billing.dto.TongHopCdr;
import com.hanzo.billing.entity.ChiTietSuDung;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Nghiệp vụ tra cứu bản ghi chi tiết sử dụng (CDR). */
public interface ChiTietSuDungService {

    Page<ChiTietSuDung> timCoLoc(BoLocCdr boLoc, Pageable pageable);

    /** Số liệu tổng tính trên TOÀN BỘ kết quả lọc, không chỉ trang đang xem. */
    TongHopCdr tinhTong(BoLocCdr boLoc);

    /** Xuất toàn bộ kết quả lọc ra file Excel .xlsx. */
    byte[] xuatExcel(BoLocCdr boLoc);
}
