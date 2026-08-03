package com.hanzo.billing.service;

import com.hanzo.billing.dto.KyCuocForm;
import com.hanzo.billing.entity.KyCuoc;

import java.util.List;

/** Nghiệp vụ quản lý kỳ tính cước. */
public interface KyCuocService {

    /** Danh sách kỳ, mới nhất trước. */
    List<KyCuoc> layTatCa();

    KyCuoc layTheoId(Long id);

    /**
     * Tạo kỳ cước mới. Ngày đầu và ngày cuối tháng do hệ thống tự tính,
     * trạng thái khởi tạo là {@code MO}.
     */
    KyCuoc taoMoi(KyCuocForm form);
}
