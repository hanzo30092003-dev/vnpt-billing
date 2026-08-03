package com.hanzo.billing.util;

import com.hanzo.billing.dto.TongHopCdr;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.enums.LoaiDichVu;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Định dạng các giá trị của bản ghi CDR để hiển thị.
 *
 * <p>Khai báo thành bean tên {@code dinhDang} để template gọi trực tiếp:
 * {@code ${@dinhDang.thoiLuong(c)}} — gọn hơn nhiều so với viết biểu thức
 * tính toán dài trong Thymeleaf.</p>
 */
@Component("dinhDang")
public class DinhDangCdr {

    /**
     * Thời lượng dạng {@code mm:ss}. Chỉ có ý nghĩa với dịch vụ THOẠI;
     * SMS và DATA trả về dấu gạch ngang.
     */
    public String thoiLuong(ChiTietSuDung cdr) {
        if (cdr.getLoaiDichVu() != LoaiDichVu.THOAI || cdr.getThoiLuongGiay() == null) {
            return "—";
        }
        int giay = cdr.getThoiLuongGiay();
        return String.format("%02d:%02d", giay / 60, giay % 60);
    }

    /**
     * Dung lượng quy đổi từ KB sang MB, 2 chữ số thập phân.
     *
     * <p>Cột {@code so_luong} lưu theo KB với dịch vụ DATA — xem cảnh báo ở
     * {@code docs/mo-ta-csdl.md} mục 6.</p>
     */
    public String dungLuong(ChiTietSuDung cdr) {
        if (cdr.getLoaiDichVu() != LoaiDichVu.DATA || cdr.getSoLuong() == null) {
            return "—";
        }
        BigDecimal mb = BigDecimal.valueOf(cdr.getSoLuong())
                .divide(BigDecimal.valueOf(TongHopCdr.KB_MOI_MB), 2, RoundingMode.HALF_UP);
        return mb + " MB";
    }

    /** Số lượng hiển thị: số tin với SMS, dung lượng MB với DATA, gạch ngang với THOẠI. */
    public String soLuong(ChiTietSuDung cdr) {
        if (cdr.getLoaiDichVu() == LoaiDichVu.SMS) {
            return cdr.getSoLuong() + " tin";
        }
        if (cdr.getLoaiDichVu() == LoaiDichVu.DATA) {
            return dungLuong(cdr);
        }
        return "—";
    }
}
