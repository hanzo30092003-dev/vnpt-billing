package com.hanzo.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** DTO cho form tạo kỳ cước mới. Chỉ cần tháng và năm, ngày đầu/cuối do hệ thống tự tính. */
@Getter
@Setter
@NoArgsConstructor
public class KyCuocForm {

    @NotNull(message = "Vui lòng chọn tháng")
    @Min(value = 1, message = "Tháng phải từ 1 đến 12")
    @Max(value = 12, message = "Tháng phải từ 1 đến 12")
    private Integer thang;

    @NotNull(message = "Vui lòng nhập năm")
    @Min(value = 2020, message = "Năm phải từ 2020 trở đi")
    @Max(value = 2100, message = "Năm không được quá 2100")
    private Integer nam;
}
