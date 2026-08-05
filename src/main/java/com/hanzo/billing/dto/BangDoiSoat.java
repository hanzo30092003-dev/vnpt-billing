package com.hanzo.billing.dto;

import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bảng đối soát cước của một thuê bao trong một kỳ.
 *
 * <p>Đây là màn hình chứng minh engine tính đúng: nó bày ra <b>toàn bộ đường đi của con
 * số</b>, từ sản lượng thô trên từng bản ghi CDR, qua quy đổi đơn vị và quỹ ưu đãi, tới
 * từng cột trên hóa đơn. Khối cuối cùng đặt hai cột "tính từ CDR" và "trên hóa đơn" cạnh
 * nhau — chênh lệch phải toàn 0.</p>
 */
public record BangDoiSoat(
        ThueBao thueBao,
        KyCuoc kyCuoc,
        GoiCuoc goiCuoc,
        HoaDon hoaDon,
        ThongTinProrate prorate,
        List<DongUuDai> dongUuDai,
        List<DongCdr> dongCdr,
        List<DongDoiChieu> doiChieu) {

    /** Có chênh lệch nào khác 0 giữa số tính từ CDR và số trên hóa đơn không. */
    public boolean coChenhLech() {
        return doiChieu.stream().anyMatch(d -> d.chenhLech().signum() != 0);
    }

    public long soDongMienPhi() {
        return dongCdr.stream().filter(DongCdr::mienPhi).count();
    }

    /**
     * Thông tin prorate cước thuê bao.
     *
     * @param prorate true nếu thuê bao không dùng trọn kỳ nên cước thuê bao bị chia theo ngày
     */
    public record ThongTinProrate(LocalDate tuNgay, LocalDate denNgay,
                                  long soNgaySuDung, long soNgayTrongKy,
                                  BigDecimal cuocThueBaoThang, BigDecimal cuocThueBaoThuc,
                                  boolean prorate) {
    }

    /**
     * Một dòng của bảng sản lượng và ưu đãi.
     *
     * @param daDungHienThi  sản lượng ở đơn vị người đọc hiểu, ví dụ {@code "89,2 phút"}
     * @param daDungGoc      chính sản lượng đó ở đơn vị lưu trong CSDL, ví dụ
     *                       {@code "5.351 giây"} — đây là <b>bằng chứng trực quan</b> cho
     *                       thấy hệ thống quy đổi đúng, không được bỏ khi hiển thị
     * @param quotaHienThi   quota của gói, {@code "—"} nếu loại này không có ưu đãi
     * @param coUuDai        false với hàng quốc tế: không áp dụng ưu đãi
     */
    public record DongUuDai(String loai, String daDungHienThi, String daDungGoc,
                            String quotaHienThi, String mienPhiHienThi, String vuotHienThi,
                            BigDecimal thanhTien, boolean coUuDai, String ghiChu) {
    }

    /**
     * Một dòng chi tiết bản ghi CDR.
     *
     * @param donGia    lấy từ {@code chi_tiet_su_dung.bang_gia_cuoc_id} đã chụp lúc định
     *                  giá, <b>không</b> tra lại bảng giá hiện hành
     * @param laDongLamVuot bản ghi đầu tiên bị tính tiền của loại ưu đãi tương ứng — chính
     *                      là bản ghi làm vượt quota, bị thu tiền toàn bộ theo quy tắc
     *                      không cắt đôi bản ghi
     */
    public record DongCdr(Long id, LocalDateTime thoiGian, String soBiGoi,
                          String loaiDichVu, String huong, boolean gioCaoDiem,
                          String sanLuongHienThi, Long soBlock, BigDecimal donGia,
                          BigDecimal cuocPhi, boolean mienPhi, boolean laDongLamVuot) {
    }

    /** Một dòng của bảng đối chiếu với hóa đơn. */
    public record DongDoiChieu(String khoanMuc, BigDecimal tuCdr, BigDecimal trenHoaDon) {

        public BigDecimal chenhLech() {
            return tuCdr.subtract(trenHoaDon);
        }
    }
}
