package com.hanzo.billing.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Dòng tổng cuối bảng tra cứu CDR, tính trên TOÀN BỘ kết quả lọc. */
@Getter
@AllArgsConstructor
public class TongHopCdr {

    /** Số kilobyte trong một megabyte — dùng chung cho mọi chỗ quy đổi dung lượng. */
    public static final int KB_MOI_MB = 1024;

    private final long soBanGhi;

    /** Tổng thời lượng thoại, đơn vị giây. */
    private final long tongThoiLuongGiay;

    /** Tổng dung lượng data, đơn vị KB (đúng đơn vị lưu trong CSDL). */
    private final long tongDungLuongKb;

    /** Tổng thời lượng ở dạng {@code HH:mm:ss} cho dễ đọc. */
    public String tongThoiLuongDangChu() {
        long gio = tongThoiLuongGiay / 3600;
        long phut = (tongThoiLuongGiay % 3600) / 60;
        long giay = tongThoiLuongGiay % 60;
        return String.format("%02d:%02d:%02d", gio, phut, giay);
    }

    /** Tổng dung lượng quy đổi sang MB, làm tròn 2 chữ số thập phân. */
    public BigDecimal tongDungLuongMb() {
        return BigDecimal.valueOf(tongDungLuongKb)
                .divide(BigDecimal.valueOf(KB_MOI_MB), 2, RoundingMode.HALF_UP);
    }
}
