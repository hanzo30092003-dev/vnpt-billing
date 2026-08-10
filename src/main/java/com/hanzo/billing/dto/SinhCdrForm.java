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

    /**
     * Hạt giống cho bộ sinh ngẫu nhiên. Để trống thì hệ thống tự bốc.
     *
     * <p><b>Vì sao cần.</b> Tới hết Phase 5 bộ sinh dùng {@code new Random()} không hạt giống,
     * nên bộ CDR sinh ra <b>không tái lập được</b>: mất dữ liệu là mất luôn mọi con số đã viết
     * vào báo cáo. Khiếm khuyết đó không gây lỗi nào cả, nó chỉ lặng lẽ biến {@code reset}
     * thành thao tác không ai dám chạy — xem `PHASE-5-REPORT.md` mục 23.1.</p>
     *
     * <p>Để trống <b>vẫn tái lập được</b>: hệ thống bốc một hạt giống rồi trả về trong
     * {@link KetQuaSinhCdr#getHatGiongDaDung()} và hiện lên màn hình để chép lại.</p>
     */
    private Long hatGiong;
}
