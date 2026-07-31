package com.hanzo.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Kiểm tra số giấy tờ khớp với loại khách hàng.
 *
 * <p>Đây là ràng buộc ở mức LỚP chứ không phải mức trường, vì quy tắc phụ thuộc
 * đồng thời vào hai trường {@code loaiKh} và {@code soGiayTo} — Bean Validation
 * không thể diễn đạt quan hệ đó bằng annotation đặt trên một trường đơn lẻ.</p>
 */
@Documented
@Constraint(validatedBy = GiayToHopLeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface GiayToHopLe {

    String message() default "Số giấy tờ không hợp lệ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
