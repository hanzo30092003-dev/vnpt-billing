package com.hanzo.billing.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class KhoangHieuLucHopLeValidator
        implements ConstraintValidator<KhoangHieuLucHopLe, CoKhoangHieuLuc> {

    @Override
    public boolean isValid(CoKhoangHieuLuc form, ConstraintValidatorContext context) {
        if (form == null || form.getNgayHieuLuc() == null || form.getNgayHetHieuLuc() == null) {
            // Thiếu ngày hiệu lực đã có @NotNull báo riêng; hết hiệu lực null nghĩa là vô hạn
            return true;
        }
        if (form.getNgayHetHieuLuc().isAfter(form.getNgayHieuLuc())) {
            return true;
        }
        // Gắn lỗi vào ô ngày hết hiệu lực để giao diện tô đỏ đúng chỗ
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate("Ngày hết hiệu lực phải sau ngày hiệu lực. Để trống ô này nếu dòng giá áp dụng vô thời hạn.")
                .addPropertyNode("ngayHetHieuLuc")
                .addConstraintViolation();
        return false;
    }
}
