package com.hanzo.billing.validation;

import java.time.LocalDate;

/**
 * Đánh dấu một DTO có cặp ngày hiệu lực / hết hiệu lực.
 *
 * <p>Nhờ interface này mà {@link KhoangHieuLucHopLe} dùng chung được cho cả form gói
 * cước lẫn form bảng giá, thay vì viết hai validator gần giống nhau.</p>
 */
public interface CoKhoangHieuLuc {

    LocalDate getNgayHieuLuc();

    /** Null nghĩa là còn hiệu lực vô thời hạn. */
    LocalDate getNgayHetHieuLuc();
}
