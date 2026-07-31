package com.hanzo.billing.enums;

/**
 * Trạng thái của kỳ tính cước.
 *
 * <p>MO: đang thu thập CDR. DANG_TINH: engine đang chạy tính cước.
 * DA_CHOT: đã chốt, không nhận thêm CDR và không tính lại.</p>
 */
public enum TrangThaiKyCuoc {
    MO,
    DANG_TINH,
    DA_CHOT
}
