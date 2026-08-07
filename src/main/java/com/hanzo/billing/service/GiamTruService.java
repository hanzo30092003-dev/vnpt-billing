package com.hanzo.billing.service;

import com.hanzo.billing.dto.GiamTruForm;
import com.hanzo.billing.entity.GiamTru;
import com.hanzo.billing.enums.TrangThaiGiamTru;

import java.util.List;

public interface GiamTruService {

    List<GiamTru> timKiem(Long kyCuocId, Long thueBaoId, TrangThaiGiamTru trangThai);

    GiamTru layTheoId(Long id);

    GiamTru luu(GiamTruForm form);

    void xoa(Long id);
}
