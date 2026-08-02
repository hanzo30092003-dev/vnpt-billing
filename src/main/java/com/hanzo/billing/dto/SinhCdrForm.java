package com.hanzo.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/** Tham số cho bộ sinh CDR giả lập. */
@Getter
@Setter
@NoArgsConstructor
public class SinhCdrForm {

    @NotNull(message = "Vui lòng chọn ngày bắt đầu")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate tuNgay = LocalDate.of(2026, 6, 1);

    @NotNull(message = "Vui lòng chọn ngày kết thúc")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate denNgay = LocalDate.of(2026, 6, 30);

    @NotNull(message = "Vui lòng nhập số lượng bản ghi")
    @Min(value = 1, message = "Số lượng bản ghi phải lớn hơn 0")
    @Max(value = 200000, message = "Mỗi lần chỉ sinh tối đa 200.000 bản ghi")
    private Integer soLuong = 5000;

    /** TAT_CA = mọi thuê bao đang hoạt động; CHON = chỉ các thuê bao được tick. */
    private String phamVi = "TAT_CA";

    private List<Long> thueBaoIds;
}
