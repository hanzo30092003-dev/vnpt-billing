package com.hanzo.billing.dto;

import com.hanzo.billing.enums.HinhThucThanhToan;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Form ghi nhận một giao dịch thanh toán.
 *
 * <p>Hai luật còn lại — <i>không vượt số còn nợ</i> và <i>không thanh toán hóa đơn đã trả
 * đủ</i> — <b>không</b> đặt ở đây mà đặt trong service. Chúng phụ thuộc trạng thái hóa đơn
 * tại thời điểm ghi, mà trạng thái đó có thể đã đổi kể từ lúc form được mở; kiểm ở tầng
 * form thì chỉ đúng vào lúc hiển thị.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class ThanhToanForm {

    @NotNull(message = "Chưa biết ghi nhận cho hóa đơn nào. Hãy mở lại hóa đơn rồi bấm Ghi nhận thanh toán.")
    private Long hoaDonId;

    @NotNull(message = "Vui lòng nhập số tiền")
    @DecimalMin(value = "1", message = "Số tiền thanh toán phải lớn hơn 0")
    private BigDecimal soTien;

    @NotNull(message = "Vui lòng chọn hình thức thanh toán")
    private HinhThucThanhToan hinhThuc;

    @NotNull(message = "Vui lòng chọn ngày thanh toán")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayThanhToan;

    @Size(max = 300, message = "Ghi chú tối đa 300 ký tự")
    private String ghiChu;
}
