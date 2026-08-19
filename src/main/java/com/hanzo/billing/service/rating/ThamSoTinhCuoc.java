package com.hanzo.billing.service.rating;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tham số nghiệp vụ của engine tính cước — gom về một nơi để đối chiếu với báo cáo.
 *
 * <p>Các giá trị này không nằm trong CSDL vì chúng là hằng số nghiệp vụ, không phải dữ
 * liệu người dùng nhập. Thêm hẳn một bảng cấu hình cho ba hằng số là thừa; nhưng rải
 * chúng rải rác trong code thì không ai đối chiếu được với con số ghi trong báo cáo.</p>
 */
public final class ThamSoTinhCuoc {

    // Thuế suất VAT đã chuyển sang cấu hình (billing.thue-suat-vat), xem
    // com.hanzo.billing.config.ThamSoNghiepVu. Nó là chính sách nhà nước chứ không phải
    // quy tắc của phần mềm này, và nó đã đổi thật (8% các năm 2022-2024) - chôn cứng nghĩa
    // là mỗi lần đổi thuế là một lần sửa mã và triển khai lại.

    /**
     * Ngày trong tháng kế tiếp là hạn thanh toán hóa đơn.
     *
     * <p>Hóa đơn lập vào ngày cuối kỳ, hạn thanh toán là ngày 15 của tháng sau.</p>
     *
     * <p>Cách phát biểu tương đương: <i>ngày lập + 15 ngày</i>. Hai cách cho kết quả
     * <b>giống hệt nhau</b> vì ngày lập luôn là ngày cuối tháng — 30/06 + 15 = 15/07,
     * 31/07 + 15 = 15/08, 28/02 + 15 = 15/03, 29/02 + 15 = 15/03. Chọn cách phát biểu
     * theo ngày cố định vì nó không phụ thuộc vào giả định "ngày lập luôn cuối tháng".</p>
     */
    public static final int NGAY_HAN_THANH_TOAN_THANG_SAU = 15;

    /** Chế độ làm tròn tiền. */
    public static final RoundingMode LAM_TRON = RoundingMode.HALF_UP;

    /**
     * Ngưỡng cảnh báo số dư thấp của thuê bao trả trước, 50.000 đ.
     *
     * <p>Chỉ dùng để tô cảnh báo trên giao diện, <b>không</b> chặn nghiệp vụ nào — thuê bao
     * dưới ngưỡng vẫn dùng dịch vụ và vẫn bị trừ cước bình thường.</p>
     */
    public static final BigDecimal NGUONG_CANH_BAO_SO_DU = new BigDecimal("50000");

    /**
     * Số ngày quá hạn để hệ thống <b>đề xuất</b> tạm ngừng thuê bao, mặc định 15 ngày.
     *
     * <p>Chỉ là <b>đề xuất</b>: hệ thống liệt kê ra và người dùng bấm nút, không tự động
     * chuyển trạng thái. Cắt dịch vụ của khách là quyết định nghiệp vụ có hậu quả thật, và
     * dữ liệu công nợ có thể chưa cập nhật (khách vừa nộp tiền ở quầy khác).</p>
     */
    public static final int SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG = 15;

    /**
     * Số chữ số thập phân khi làm tròn tiền: 0, vì VND không có đơn vị nhỏ hơn đồng.
     *
     * <p>Cột trong CSDL vẫn là {@code DECIMAL(15,2)} nên giá trị lưu xuống có dạng
     * {@code x.00} — làm tròn về đồng rồi mới đưa về scale 2, không phải ngược lại.</p>
     */
    public static final int SO_LE_TIEN = 0;

    /** Scale của các cột tiền trong CSDL. */
    private static final int SCALE_CSDL = 2;

    private ThamSoTinhCuoc() {
    }

    /**
     * Làm tròn một khoản tiền về đồng.
     *
     * <p><b>Bất biến quan trọng:</b> chỉ được làm tròn ở <b>đúng một tầng</b> — tầng từng
     * bản ghi CDR. Các mức trên (cước theo dịch vụ, tổng trước thuế, tổng thanh toán)
     * chỉ cộng dồn, không làm tròn lại. Làm tròn ở nhiều tầng sẽ khiến
     * {@code SUM(cuoc_phi)} không khớp với cột tương ứng trên {@code hoa_don}, và loại
     * lệch một đồng đó gần như không thể truy nguyên.</p>
     */
    public static BigDecimal lamTronTien(BigDecimal soTien) {
        return soTien.setScale(SO_LE_TIEN, LAM_TRON).setScale(SCALE_CSDL);
    }
}
