package com.hanzo.billing.dto.baocao;

import com.hanzo.billing.util.DinhDangTien;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Cơ cấu doanh thu một kỳ theo loại dịch vụ, đọc thẳng từ sáu cột của {@code hoa_don}.
 *
 * <p><b>Cố ý không tính lại từ CDR.</b> Cộng {@code chi_tiet_su_dung.cuoc_phi} theo loại dịch
 * vụ sẽ ra một con số <i>gần giống</i> nhưng không bằng: hóa đơn còn có cước thuê bao tháng
 * (không đến từ CDR nào), có ưu đãi gói cước đã trừ, và có giảm trừ. Hai cách tính là hai
 * nguồn sự thật — bài học 43.6.</p>
 *
 * @param giamTru luôn là số <b>dương</b> như lưu trong CSDL; nó là khoản <i>trừ đi</i> nên khi
 *                hiển thị và khi cộng tổng phải mang dấu âm
 */
public record CoCauCuoc(BigDecimal cuocThueBao, BigDecimal cuocThoai, BigDecimal cuocSms,
                        BigDecimal cuocData, BigDecimal cuocKhac, BigDecimal giamTru,
                        BigDecimal tongTruocThue, BigDecimal thueVat, BigDecimal tongThanhToan) {

    /** Một khoản mục để vẽ bảng và biểu đồ tròn. */
    public record KhoanMuc(String ten, BigDecimal soTien, BigDecimal tyTrong) {
    }

    /**
     * Năm khoản mục cấu thành, kèm tỷ trọng trên tổng cước <b>trước</b> giảm trừ.
     *
     * <p>Lấy mẫu số là tổng trước giảm trừ chứ không phải {@code tong_truoc_thue}: giảm trừ là
     * một khoản âm, nếu để nó vào mẫu số thì tổng các tỷ trọng vượt quá 100% và biểu đồ tròn
     * không còn đọc được.</p>
     */
    public List<KhoanMuc> khoanMuc() {
        BigDecimal mauSo = tongCuocTruocGiamTru();
        List<KhoanMuc> ketQua = new ArrayList<>();
        them(ketQua, "Cước thuê bao", cuocThueBao, mauSo);
        them(ketQua, "Cước thoại", cuocThoai, mauSo);
        them(ketQua, "Cước SMS", cuocSms, mauSo);
        them(ketQua, "Cước dữ liệu", cuocData, mauSo);
        them(ketQua, "Cước khác", cuocKhac, mauSo);
        return ketQua;
    }

    private static void them(List<KhoanMuc> ds, String ten, BigDecimal soTien, BigDecimal mauSo) {
        BigDecimal giaTri = soTien == null ? BigDecimal.ZERO : soTien;
        ds.add(new KhoanMuc(ten, giaTri, DinhDangTien.tyLePhanTram(giaTri, mauSo)));
    }

    public BigDecimal tongCuocTruocGiamTru() {
        return khongNull(cuocThueBao).add(khongNull(cuocThoai)).add(khongNull(cuocSms))
                .add(khongNull(cuocData)).add(khongNull(cuocKhac));
    }

    /** Kỳ chưa có hóa đơn nào thì mọi cột đều 0 — màn hình hiện "Không có dữ liệu". */
    public boolean rong() {
        return tongCuocTruocGiamTru().signum() == 0;
    }

    private static BigDecimal khongNull(BigDecimal giaTri) {
        return giaTri == null ? BigDecimal.ZERO : giaTri;
    }
}
