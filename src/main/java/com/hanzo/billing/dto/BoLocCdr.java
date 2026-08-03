package com.hanzo.billing.dto;

import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.NguonCdr;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Bộ lọc của màn hình tra cứu CDR. */
@Getter
@Setter
@NoArgsConstructor
public class BoLocCdr {

    private String soThueBao;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate tuNgay;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate denNgay;

    private LoaiDichVu loaiDichVu;
    private HuongCuocGoi huong;
    private TrangThaiTinhCuoc trangThaiTinhCuoc;
    private NguonCdr nguon;

    /** Đầu ngày của {@code tuNgay}; null nếu không lọc. */
    public LocalDateTime tuLuc() {
        return tuNgay == null ? null : tuNgay.atStartOfDay();
    }

    /** Cuối ngày của {@code denNgay} — phải lấy đến 23:59:59 chứ không phải 00:00. */
    public LocalDateTime denLuc() {
        return denNgay == null ? null : denNgay.atTime(23, 59, 59);
    }

    public String soThueBaoChuan() {
        return (soThueBao == null || soThueBao.isBlank()) ? null : soThueBao.trim();
    }

    /** Chuỗi query giữ nguyên bộ lọc khi chuyển trang hoặc bấm xuất Excel. */
    public String chuoiQuery() {
        StringBuilder sb = new StringBuilder();
        if (soThueBaoChuan() != null) {
            sb.append("&soThueBao=").append(URLEncoder.encode(soThueBaoChuan(), StandardCharsets.UTF_8));
        }
        if (tuNgay != null) {
            sb.append("&tuNgay=").append(tuNgay);
        }
        if (denNgay != null) {
            sb.append("&denNgay=").append(denNgay);
        }
        if (loaiDichVu != null) {
            sb.append("&loaiDichVu=").append(loaiDichVu.name());
        }
        if (huong != null) {
            sb.append("&huong=").append(huong.name());
        }
        if (trangThaiTinhCuoc != null) {
            sb.append("&trangThaiTinhCuoc=").append(trangThaiTinhCuoc.name());
        }
        if (nguon != null) {
            sb.append("&nguon=").append(nguon.name());
        }
        return sb.toString();
    }
}
