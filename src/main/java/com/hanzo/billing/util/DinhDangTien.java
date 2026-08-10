package com.hanzo.billing.util;

import com.hanzo.billing.service.rating.DonViCuoc;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * Định dạng số cho mọi màn hình báo cáo — <b>một chỗ duy nhất</b>.
 *
 * <h2>Vì sao là một bean chứ không phải hàm tiện ích tĩnh</h2>
 * <p>Thymeleaf gọi được bean bằng cú pháp <code>${@soLieu.tien(x)}</code> nhưng không gọi
 * được phương thức tĩnh nếu không khai báo thêm. Tên bean là {@code soLieu} chứ không phải
 * {@code dinhDang} vì tên đó đã thuộc về {@link DinhDangCdr} từ Phase 3 — hai bean trùng tên
 * làm ứng dụng không khởi động được.</p>
 *
 * <h2>Vì sao gom lại</h2>
 * <p>Trước Phase 6, mỗi template tự viết
 * <code>#numbers.formatDecimal(x, 0, 'POINT', 0, 'COMMA')</code> — chuỗi đó lặp lại hàng chục
 * lần trên mười mấy file. Lặp một quy tắc định dạng ra nhiều chỗ thì sẽ có lúc vài chỗ hiện
 * khác nhau cho cùng một con số, đúng kiểu lỗi mà enum {@code NhomTuoiNo} và
 * {@code TrangThaiHoaDon} đã gom lại để tránh.</p>
 *
 * <p>Quy ước tiếng Việt: <b>dấu chấm</b> phân cách hàng nghìn, <b>dấu phẩy</b> phân cách thập
 * phân — ngược với quy ước Anh–Mỹ.</p>
 */
@Component("soLieu")
public class DinhDangTien {

    private static final DecimalFormatSymbols KY_HIEU_VN = kyHieuVietNam();

    /** Không có định dạng nào là an toàn với null; trả về gạch ngang thay vì ném lỗi giữa trang. */
    private static final String KHONG_CO = "—";

    private static DecimalFormatSymbols kyHieuVietNam() {
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols();
        kyHieu.setGroupingSeparator('.');
        kyHieu.setDecimalSeparator(',');
        return kyHieu;
    }

    /** Số tiền làm tròn về đồng, có phân cách nghìn. Ví dụ {@code 21497051} → {@code 21.497.051}. */
    public String tien(BigDecimal soTien) {
        if (soTien == null) {
            return KHONG_CO;
        }
        return new DecimalFormat("#,##0", KY_HIEU_VN).format(soTien);
    }

    /** Số tiền kèm đơn vị, dùng cho các thẻ số liệu trên dashboard. */
    public String tienDong(BigDecimal soTien) {
        return soTien == null ? KHONG_CO : tien(soTien) + " đ";
    }

    /** Số nguyên có phân cách nghìn: số bản ghi, số hóa đơn, số phút. */
    public String so(Number giaTri) {
        if (giaTri == null) {
            return KHONG_CO;
        }
        return new DecimalFormat("#,##0", KY_HIEU_VN).format(giaTri);
    }

    /** Số thập phân một chữ số, cho sản lượng đã quy đổi (phút, MB). */
    public String soLe(BigDecimal giaTri) {
        if (giaTri == null) {
            return KHONG_CO;
        }
        return new DecimalFormat("#,##0.0", KY_HIEU_VN).format(giaTri);
    }

    /** Tỷ lệ phần trăm một chữ số thập phân, ví dụ {@code 71,0%}. */
    public String phanTram(BigDecimal tyLe) {
        if (tyLe == null) {
            return KHONG_CO;
        }
        return new DecimalFormat("#,##0.0", KY_HIEU_VN).format(tyLe) + "%";
    }

    /** Biến thiên kèm dấu, ví dụ {@code +12,3%} — dấu cộng nói rõ chiều tăng. */
    public String bienThien(BigDecimal tyLe) {
        if (tyLe == null) {
            return KHONG_CO;
        }
        String so = new DecimalFormat("#,##0.0", KY_HIEU_VN).format(tyLe) + "%";
        return tyLe.signum() > 0 ? "+" + so : so;
    }

    /**
     * Lớp CSS tô màu cho một con số biến thiên.
     *
     * <p>Tính ở đây chứ không viết biểu thức điều kiện trong template: Thymeleaf không cho
     * lồng {@code ${...}} bên trong một biểu thức khác, và cố viết như vậy làm cả trang ném
     * {@code TemplateInputException} — lỗi lộ ra ở <b>thời điểm chạy</b>, không phải lúc biên
     * dịch, nên chỉ phát hiện được khi thật sự mở trang đó lên.</p>
     */
    public String lopBienThien(BigDecimal tyLe) {
        if (tyLe == null || tyLe.signum() == 0) {
            return "text-secondary";
        }
        return tyLe.signum() > 0 ? "text-success" : "text-danger";
    }

    /**
     * Tỷ lệ {@code tuSo / mauSo} tính bằng phần trăm.
     *
     * <p>Mẫu số bằng 0 trả về {@code null} chứ không phải 0: <i>"không có dữ liệu để tính"</i>
     * và <i>"tính ra 0%"</i> là hai điều khác nhau, và hiển thị 0% cho một kỳ chưa lập hóa đơn
     * nào sẽ làm người đọc tưởng kỳ đó thu được 0 đồng trên một khoản phải thu có thật.</p>
     */
    public static BigDecimal tyLePhanTram(BigDecimal tuSo, BigDecimal mauSo) {
        if (tuSo == null || mauSo == null || mauSo.signum() == 0) {
            return null;
        }
        return tuSo.multiply(BigDecimal.valueOf(100))
                .divide(mauSo, 1, RoundingMode.HALF_UP);
    }

    /** Biến thiên phần trăm giữa hai kỳ; {@code null} khi kỳ trước không có số để so. */
    public static BigDecimal bienThienPhanTram(BigDecimal kyNay, BigDecimal kyTruoc) {
        if (kyNay == null || kyTruoc == null || kyTruoc.signum() == 0) {
            return null;
        }
        return kyNay.subtract(kyTruoc).multiply(BigDecimal.valueOf(100))
                .divide(kyTruoc, 1, RoundingMode.HALF_UP);
    }

    // =================================================================
    // QUY ĐỔI ĐƠN VỊ ĐỂ HIỂN THỊ
    // =================================================================

    /**
     * ⚠️ <b>Vì sao không gọi thẳng {@link DonViCuoc} — và vì sao đây KHÔNG phải chỗ quy đổi
     * thứ tư.</b>
     *
     * <p>{@code DonViCuoc.giaySangPhut} làm tròn <b>lên</b>, vì nó là một <i>luật tính tiền</i>:
     * dùng dở một block thì trả trọn block. Với một bảng <i>thống kê sản lượng</i>, làm tròn
     * lên là sai — 5.978 giây phải hiện là <b>99,6 phút</b> chứ không phải 100 phút, đúng như
     * con số mục 35 của Phase 4 đã dùng để minh hoạ ca sát ranh giới quota.</p>
     *
     * <p>Hai phép quy đổi khác nhau về <b>mục đích</b> nên khác nhau về chế độ làm tròn; cái
     * chung giữa chúng là <b>hệ số</b>, và hệ số vẫn chỉ khai báo một lần trong
     * {@link DonViCuoc}. Đó là ranh giới đúng: dùng chung hằng số, không dùng chung luật.</p>
     */
    public static BigDecimal giayRaPhut(long soGiay) {
        return BigDecimal.valueOf(soGiay)
                .divide(BigDecimal.valueOf(DonViCuoc.GIAY_MOI_PHUT), 1, RoundingMode.HALF_UP);
    }

    /** KB sang MB để hiển thị, một chữ số thập phân. Cùng lý do như {@link #giayRaPhut}. */
    public static BigDecimal kbRaMb(long soKb) {
        return BigDecimal.valueOf(soKb)
                .divide(BigDecimal.valueOf(DonViCuoc.KB_MOI_MB), 1, RoundingMode.HALF_UP);
    }
}
