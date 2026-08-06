package com.hanzo.billing.dto;

import com.hanzo.billing.enums.TrangThaiHoaDon;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Bộ lọc của màn hình danh sách hóa đơn. */
@Getter
@Setter
@NoArgsConstructor
public class BoLocHoaDon {

    private Long kyCuocId;
    private TrangThaiHoaDon trangThai;
    private String khachHang;
    private String soThueBao;
    private BigDecimal tuSoTien;
    private BigDecimal denSoTien;

    public String khachHangChuan() {
        return (khachHang == null || khachHang.isBlank()) ? null : khachHang.trim();
    }

    public String soThueBaoChuan() {
        return (soThueBao == null || soThueBao.isBlank()) ? null : soThueBao.trim();
    }

    /** Chuỗi query giữ nguyên bộ lọc khi chuyển trang hoặc bấm xuất Excel. */
    public String chuoiQuery() {
        StringBuilder sb = new StringBuilder();
        if (kyCuocId != null) {
            sb.append("&kyCuocId=").append(kyCuocId);
        }
        if (trangThai != null) {
            sb.append("&trangThai=").append(trangThai.name());
        }
        if (khachHangChuan() != null) {
            sb.append("&khachHang=").append(
                    URLEncoder.encode(khachHangChuan(), StandardCharsets.UTF_8));
        }
        if (soThueBaoChuan() != null) {
            sb.append("&soThueBao=").append(
                    URLEncoder.encode(soThueBaoChuan(), StandardCharsets.UTF_8));
        }
        if (tuSoTien != null) {
            sb.append("&tuSoTien=").append(tuSoTien.toPlainString());
        }
        if (denSoTien != null) {
            sb.append("&denSoTien=").append(denSoTien.toPlainString());
        }
        return sb.toString();
    }
}
