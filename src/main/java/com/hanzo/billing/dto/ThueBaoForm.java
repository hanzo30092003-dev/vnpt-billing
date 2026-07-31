package com.hanzo.billing.dto;

import com.hanzo.billing.enums.LoaiThueBao;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO cho form đăng ký thuê bao mới. */
@Getter
@Setter
@NoArgsConstructor
public class ThueBaoForm {

    @NotNull(message = "Vui lòng chọn khách hàng")
    private Long khachHangId;

    /**
     * Đầu số hợp lệ tại Việt Nam sau khi chuyển đổi năm 2018: 03, 05, 07, 08, 09.
     * Tổng cộng đúng 10 chữ số.
     */
    @NotBlank(message = "Vui lòng nhập số thuê bao")
    @Pattern(regexp = "^0[35789]\\d{8}$",
            message = "Số thuê bao phải gồm 10 chữ số và bắt đầu bằng 03, 05, 07, 08 hoặc 09")
    private String soThueBao;

    @NotNull(message = "Vui lòng chọn loại thuê bao")
    private LoaiThueBao loaiThueBao;

    @NotNull(message = "Vui lòng chọn gói cước")
    private Long goiCuocId;

    @NotNull(message = "Vui lòng chọn ngày kích hoạt")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayKichHoat;

    /** Chỉ dùng cho thuê bao trả sau. */
    @PositiveOrZero(message = "Hạn mức tín dụng không được âm")
    private BigDecimal hanMucTinDung;
}
