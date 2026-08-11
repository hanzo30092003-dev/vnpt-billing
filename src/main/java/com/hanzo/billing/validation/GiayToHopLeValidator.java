package com.hanzo.billing.validation;

import com.hanzo.billing.dto.KhachHangForm;
import com.hanzo.billing.enums.LoaiKhachHang;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Quy tắc nghiệp vụ về số giấy tờ:
 * <ul>
 *   <li>Khách CÁ NHÂN khai số CCCD — đúng 12 chữ số</li>
 *   <li>Khách DOANH NGHIỆP khai mã số thuế — 10 chữ số (mã chính) hoặc
 *       13 chữ số (mã đơn vị trực thuộc, dạng 10 số + 3 số hậu tố)</li>
 * </ul>
 */
public class GiayToHopLeValidator implements ConstraintValidator<GiayToHopLe, KhachHangForm> {

    private static final String CCCD = "\\d{12}";
    private static final String MST = "\\d{10}|\\d{13}";

    @Override
    public boolean isValid(KhachHangForm form, ConstraintValidatorContext context) {
        if (form == null || form.getLoaiKh() == null) {
            // Thiếu loại khách hàng đã có @NotNull báo riêng, không báo chồng ở đây
            return true;
        }
        String soGiayTo = form.getSoGiayTo();
        if (soGiayTo == null || soGiayTo.isBlank()) {
            // Bỏ trống đã có @NotBlank báo riêng
            return true;
        }

        boolean hopLe;
        String thongDiep;
        if (form.getLoaiKh() == LoaiKhachHang.CA_NHAN) {
            hopLe = soGiayTo.matches(CCCD);
            thongDiep = "Số CCCD phải gồm đúng 12 chữ số, không có dấu cách — ví dụ 079203001234";
        } else {
            hopLe = soGiayTo.matches(MST);
            thongDiep = "Mã số thuế phải gồm 10 hoặc 13 chữ số — ví dụ 0301234567";
        }

        if (!hopLe) {
            // Gắn lỗi vào đúng trường soGiayTo để giao diện tô đỏ ô nhập tương ứng
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(thongDiep)
                    .addPropertyNode("soGiayTo")
                    .addConstraintViolation();
        }
        return hopLe;
    }
}
