package com.hanzo.billing.dto;

import com.hanzo.billing.entity.GoiCuoc;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Một dòng trên màn hình danh sách gói cước. */
@Getter
@AllArgsConstructor
public class GoiCuocDto {

    private final GoiCuoc goiCuoc;

    /** Số thuê bao đang dùng gói này. */
    private final long soThueBao;

    /** Chuỗi tóm tắt ưu đãi, ví dụ "100 phút NM · 30 SMS · 2 GB". */
    private final String tomTatUuDai;
}
