package com.hanzo.billing.dto;

import com.hanzo.billing.enums.HinhThucThanhToan;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bộ lọc của màn hình danh sách giao dịch thanh toán. */
@Getter
@Setter
@NoArgsConstructor
public class BoLocThanhToan {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate tuNgay;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate denNgay;

    private HinhThucThanhToan hinhThuc;
    private Long nguoiThuId;

    public LocalDateTime tuLuc() {
        return tuNgay == null ? null : tuNgay.atStartOfDay();
    }

    /** Cận trên phải lấy hết ngày, không phải 00:00 — cùng cái bẫy đã gặp ở mục 42.2. */
    public LocalDateTime denLuc() {
        return denNgay == null ? null : denNgay.plusDays(1).atStartOfDay().minusNanos(1);
    }

    public String chuoiQuery() {
        StringBuilder sb = new StringBuilder();
        if (tuNgay != null) {
            sb.append("&tuNgay=").append(tuNgay);
        }
        if (denNgay != null) {
            sb.append("&denNgay=").append(denNgay);
        }
        if (hinhThuc != null) {
            sb.append("&hinhThuc=").append(hinhThuc.name());
        }
        if (nguoiThuId != null) {
            sb.append("&nguoiThuId=").append(nguoiThuId);
        }
        return sb.toString();
    }
}
