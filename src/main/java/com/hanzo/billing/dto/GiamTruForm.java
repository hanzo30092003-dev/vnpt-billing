package com.hanzo.billing.dto;

import com.hanzo.billing.enums.LoaiGiamTru;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Form thêm/sửa một khoản giảm trừ. */
@Getter
@Setter
@NoArgsConstructor
public class GiamTruForm {

    private Long id;

    @NotNull(message = "Vui lòng chọn thuê bao")
    private Long thueBaoId;

    /** Null nghĩa là khoản dùng chung, áp cho kỳ nào lập hóa đơn trước. */
    private Long kyCuocId;

    @NotNull(message = "Vui lòng chọn loại giảm trừ")
    private LoaiGiamTru loai;

    @DecimalMin(value = "1", message = "Số tiền giảm trừ phải lớn hơn 0")
    private BigDecimal soTien;

    @DecimalMin(value = "0.01", message = "Tỷ lệ phải lớn hơn 0")
    @DecimalMax(value = "100.00", message = "Tỷ lệ không vượt quá 100%")
    private BigDecimal tyLePhanTram;

    @Size(max = 300, message = "Lý do tối đa 300 ký tự")
    private String lyDo;

    /**
     * Chỉ được khai <b>một trong hai</b> cách: số tiền tuyệt đối hoặc tỷ lệ phần trăm.
     *
     * <p>Khai cả hai thì không có cách đọc nào là hiển nhiên đúng — engine hiện cho số tiền
     * tuyệt đối thắng, nhưng người nhập rất có thể hiểu là cộng dồn. Chặn ngay ở form thì
     * không ai phải đoán.</p>
     *
     * <p>Không khai gì thì khoản giảm trừ bằng 0, tức một bản ghi không có tác dụng gì
     * ngoài việc làm rối bảng.</p>
     */
    @AssertTrue(message = "Chỉ điền MỘT trong hai ô: số tiền giảm trừ, hoặc tỷ lệ phần trăm. Xoá trống ô còn lại.")
    public boolean isChiMotCachKhai() {
        boolean coSoTien = soTien != null && soTien.signum() > 0;
        boolean coTyLe = tyLePhanTram != null && tyLePhanTram.signum() > 0;
        return coSoTien ^ coTyLe;
    }
}
