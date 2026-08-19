package com.hanzo.billing.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Tham số nghiệp vụ đọc từ cấu hình, <b>không</b> chôn cứng trong mã.
 *
 * <h2>Vì sao thuế suất phải ra khỏi mã nguồn</h2>
 *
 * <p>Thuế suất VAT là <b>chính sách nhà nước</b>, không phải quy tắc của phần mềm này. Nó đã
 * đổi trong thực tế: Việt Nam hạ VAT xuống 8% trong các năm 2022–2024 rồi trả về 10%. Chôn nó
 * thành hằng số nghĩa là mỗi lần Quốc hội đổi thuế suất là một lần phải <b>sửa mã, biên dịch
 * lại và triển khai lại</b> — cho một con số mà người vận hành hoàn toàn biết trước.</p>
 *
 * <h2>Ba chỗ hiển thị phải đi theo, không chỉ hai chỗ tính</h2>
 *
 * <p>Đưa con số ra cấu hình mà để hóa đơn vẫn in chữ "Thuế GTGT (10%)" thì <b>tệ hơn là không
 * làm</b>: hệ thống tính 8% rồi in ra tờ hóa đơn ghi 10%, và tờ giấy đó là thứ khách hàng cầm.
 * Vì vậy {@link #nhanThueSuat()} là nguồn duy nhất cho cả ba chỗ hiển thị — màn hình chi tiết
 * hóa đơn, bản PDF, và bảng đối soát.</p>
 *
 * <h2>⚠️ Đổi thuế suất KHÔNG tính lại hóa đơn cũ</h2>
 *
 * <p>Hóa đơn đã lập giữ nguyên số tiền của nó — đó là điều đúng: hóa đơn là chứng từ, không
 * phải một phép tính chạy lại được. Nhưng có một hệ quả cần biết trước:
 * <b>bảng đối soát của hóa đơn cũ sẽ hiện lệch</b> sau khi đổi thuế suất, vì cột "tính lại"
 * của nó dùng thuế suất <i>hiện hành</i> còn cột "đã ghi" giữ thuế suất <i>lúc lập</i>.</p>
 *
 * <p>Muốn hết hẳn thì phải chụp thuế suất lên từng hóa đơn (thêm cột {@code hoa_don.thue_suat}
 * bằng một file di trú mới) — chưa làm, vì nó vượt phạm vi việc V5. Ghi ra đây để người đọc
 * sau không tưởng là đối soát hỏng.</p>
 */
@Component("thamSo")
@ConfigurationProperties(prefix = "billing")
@Validated
@Getter
@Setter
public class ThamSoNghiepVu {

    /**
     * Thuế suất giá trị gia tăng, dạng tỉ lệ ({@code 0.10} = 10%).
     *
     * <p>Ràng buộc kiểm lúc <b>khởi động</b> chứ không lúc lập hóa đơn: một thuế suất sai
     * (âm, hoặc {@code 10} thay vì {@code 0.10}) mà lọt tới lúc lập hóa đơn thì nó đã kịp sinh
     * ra vài trăm tờ hóa đơn sai. Sai cấu hình phải làm ứng dụng <b>không khởi động được</b>,
     * kèm thông báo nói rõ giá trị nào sai.</p>
     */
    @NotNull(message = "Phải khai billing.thue-suat-vat trong cấu hình")
    @DecimalMin(value = "0.0", message = "Thuế suất không thể âm")
    @DecimalMax(value = "1.0", message = "Thuế suất phải ở dạng tỉ lệ — 0.10 nghĩa là 10%, "
            + "không phải 10")
    private BigDecimal thueSuatVat;

    /**
     * Thuế suất viết cho người đọc: {@code 0.10} → {@code "10%"}, {@code 0.085} → {@code "8,5%"}.
     *
     * <p>Dùng dấu phẩy làm dấu thập phân theo quy ước tiếng Việt, giống bean {@code soLieu}.</p>
     */
    public String nhanThueSuat() {
        BigDecimal phanTram = thueSuatVat
                .multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros();
        return phanTram.toPlainString().replace('.', ',') + "%";
    }
}
