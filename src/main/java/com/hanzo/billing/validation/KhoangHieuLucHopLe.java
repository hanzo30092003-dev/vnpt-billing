package com.hanzo.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Ngày hết hiệu lực phải sau ngày hiệu lực.
 *
 * <p>Ràng buộc mức LỚP vì phụ thuộc hai trường cùng lúc.</p>
 */
@Documented
@Constraint(validatedBy = KhoangHieuLucHopLeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface KhoangHieuLucHopLe {

    String message() default "Ngày hết hiệu lực phải sau ngày hiệu lực";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
