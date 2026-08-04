package com.hanzo.billing.service.rating;

import com.hanzo.billing.exception.NghiepVuException;

/**
 * ⚠️ QUY ĐỔI ĐƠN VỊ — nơi DUY NHẤT thực hiện ba phép quy đổi của hệ thống.
 *
 * <p>Dữ liệu trong CSDL được lưu ở đơn vị <b>nhỏ</b> (KB, giây), còn ưu đãi của gói cước
 * khai ở đơn vị <b>lớn</b> (MB, phút). Ba chỗ quy đổi đó là:</p>
 *
 * <table border="1">
 *   <caption>Ba chỗ quy đổi bắt buộc</caption>
 *   <tr><th>#</th><th>Cột</th><th>Đơn vị lưu</th><th>Đối chiếu với</th><th>Sai nếu quên</th></tr>
 *   <tr><td>1</td><td>{@code chi_tiet_su_dung.so_luong} (DATA)</td><td>KB</td>
 *       <td>đơn giá tính theo MB</td><td>×1024</td></tr>
 *   <tr><td>2</td><td>{@code goi_cuoc.data_mien_phi_mb}</td><td>MB</td>
 *       <td>sản lượng lưu bằng KB</td><td>×1024</td></tr>
 *   <tr><td>3</td><td>{@code goi_cuoc.phut_*_mien_phi}</td><td>PHÚT</td>
 *       <td>{@code thoi_luong_giay} lưu bằng GIÂY</td><td>×60</td></tr>
 * </table>
 *
 * <p><b>Vì sao phải gom về một lớp:</b> cả ba đều hỏng theo cùng một kiểu — hóa đơn vẫn
 * phát hành bình thường, không lỗi, không cảnh báo, chỉ sai số tiền. Rải phép chia
 * 1024 và 60 ở nhiều chỗ thì chắc chắn có chỗ bị quên. Xem {@code docs/mo-ta-csdl.md}
 * mục 6 và {@code docs/PHASE-4-PLAN.md} mục 4.</p>
 *
 * <p><b>Chế độ làm tròn:</b> cả ba phép đều làm tròn <b>lên</b> (CEILING) vì đây là quy
 * đổi phục vụ tính cước — dùng dở một block thì vẫn tính trọn block, đúng thông lệ ngành.</p>
 */
public final class DonViCuoc {

    /** 1 MB = 1024 KB. */
    public static final int KB_MOI_MB = 1024;

    /** 1 phút = 60 giây. */
    public static final int GIAY_MOI_PHUT = 60;

    private DonViCuoc() {
    }

    /**
     * Số block phải trả tiền, làm tròn <b>lên</b>.
     *
     * <p>Ví dụ: cuộc gọi 45 giây với {@code kichThuocBlock = 6} cho ra 8 block
     * (không phải 7,5). Trong 3537 cuộc thoại của dữ liệu mẫu có 2930 cuộc — tức
     * <b>82,8%</b> — không chia hết cho block, nên quy tắc làm tròn quyết định gần như
     * toàn bộ cước thoại.</p>
     *
     * <p>Viết bằng phép chia nguyên {@code (a + b - 1) / b} thay vì
     * {@code Math.ceil(a / (double) b)}: số nguyên không có sai số dấu phẩy động, nên
     * không bao giờ gặp chuyện 3000/60 ra 49,999... rồi làm tròn thành 50 block.</p>
     *
     * @param soLuong        sản lượng thô (giây, KB, tin…); nhỏ hơn hoặc bằng 0 thì trả 0
     * @param kichThuocBlock kích thước một block, tương ứng cột
     *                       {@code bang_gia_cuoc.block_giay}
     * @throws NghiepVuException nếu {@code kichThuocBlock} nhỏ hơn hoặc bằng 0
     */
    public static long soBlock(long soLuong, int kichThuocBlock) {
        if (kichThuocBlock <= 0) {
            throw new NghiepVuException("Block tính cước phải lớn hơn 0, đang là "
                    + kichThuocBlock + ". Kiểm tra lại cột block_giay của dòng bảng giá.");
        }
        if (soLuong <= 0) {
            return 0;
        }
        return (soLuong + kichThuocBlock - 1) / kichThuocBlock;
    }

    /**
     * KB sang MB, làm tròn lên.
     *
     * <p>Đây là chỗ quy đổi số 1 và số 2 của bảng trên. Dùng cả khi nhân đơn giá lẫn
     * khi so với {@code goi_cuoc.data_mien_phi_mb}.</p>
     */
    public static long kbSangMb(long soKb) {
        return soBlock(soKb, KB_MOI_MB);
    }

    /**
     * Giây sang phút, làm tròn lên.
     *
     * <p>Đây là chỗ quy đổi số 3 — chỗ mà {@code docs/mo-ta-csdl.md} mục 6 chưa nhắc tới.
     * Gói MAX70 cho 100 <b>phút</b> nội mạng; thuê bao gọi 5400 <b>giây</b> vẫn còn trong
     * ưu đãi. So thẳng {@code 5400 > 100} là sai 60 lần mà không có cảnh báo nào.</p>
     */
    public static long giaySangPhut(long soGiay) {
        return soBlock(soGiay, GIAY_MOI_PHUT);
    }

    /**
     * Phút sang giây — phép nhân <b>đúng tuyệt đối</b>, không làm tròn.
     *
     * <p><b>Vì sao cần chiều ngược lại:</b> khi so sản lượng với quỹ ưu đãi, phải so ở
     * đơn vị <b>nhỏ nhất</b> chứ không quy sản lượng lên đơn vị của quỹ. Quy từng bản ghi
     * lên phút rồi cộng sẽ làm tròn lên nhiều lần và <b>ăn mòn quỹ của khách</b>: 37 cuộc
     * gọi tổng 5351 giây (89,2 phút) bị cộng thành 105 phút, vượt quỹ 100 phút mà thực tế
     * chưa hề vượt. Xem {@code docs/PHASE-4-REPORT.md} mục 23.</p>
     */
    public static long phutSangGiay(long soPhut) {
        return soPhut * GIAY_MOI_PHUT;
    }

    /** MB sang KB — phép nhân đúng tuyệt đối, không làm tròn. Cùng lý do như {@link #phutSangGiay}. */
    public static long mbSangKb(long soMb) {
        return soMb * KB_MOI_MB;
    }
}
