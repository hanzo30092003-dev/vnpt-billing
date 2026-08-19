package com.hanzo.billing.service.pdf;

import com.hanzo.billing.entity.ChiTietHoaDon;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ Test PDF phải <b>ĐỌC LẠI</b> nội dung file, không chỉ kiểm file có tồn tại.
 *
 * <p>Lý do nằm ở chính bài học 43.5 của Phase 4: <i>một phép kiểm sai nguy hiểm ngang thiếu
 * phép kiểm</i>. Nếu quên nhúng font Unicode thì mọi chữ tiếng Việt trong PDF sẽ ra ô vuông
 * hoặc mất dấu — nhưng file vẫn sinh ra bình thường, vẫn mở được, vẫn in được, và
 * <b>không có ngoại lệ nào được ném ra</b>. Một phép kiểm kiểu {@code assertThat(pdf).isNotEmpty()}
 * sẽ xanh trong khi sản phẩm hỏng hoàn toàn.</p>
 *
 * <p>Vì vậy toàn bộ lớp này dựng PDF thật rồi dùng PDFBox trích văn bản ra và so khớp
 * <b>chuỗi có dấu</b>.</p>
 */
@DisplayName("Xuất hóa đơn ra PDF")
class HoaDonPdfServiceTest {

    private static HoaDonPdfService service;
    private static String vanBan;
    private static byte[] pdf;

    @BeforeAll
    static void dungPdfMotLan() throws IOException {
        service = new HoaDonPdfService(thamSoVoiThueSuat("0.10"), new PdfFont());
        pdf = service.xuat(hoaDonMau(), khoanMucMau());
        vanBan = trichVanBan(pdf);
    }

    private static com.hanzo.billing.config.ThamSoNghiepVu thamSoVoiThueSuat(String thueSuat) {
        com.hanzo.billing.config.ThamSoNghiepVu t = new com.hanzo.billing.config.ThamSoNghiepVu();
        t.setThueSuatVat(new java.math.BigDecimal(thueSuat));
        return t;
    }

    /**
     * ⭐ ĐỐI CHỨNG cho việc đưa thuế suất ra cấu hình.
     *
     * <p>Phép kiểm ở trên khẳng định tờ hóa đơn in "Thuế GTGT (10%)". Một mình nó vẫn xanh
     * khi chuỗi đó bị gõ cứng trong mã — tức đúng tình huống tệ nhất: hệ thống tính theo thuế
     * suất mới còn <b>tờ giấy khách hàng cầm</b> vẫn ghi 10%.</p>
     *
     * <p>Phép kiểm này dựng lại bản PDF với thuế suất 8% và đòi tờ hóa đơn nói đúng 8%.</p>
     */
    @Test
    @DisplayName("⭐ Nhãn thuế trên hóa đơn đi theo cấu hình, không phải chuỗi gõ cứng")
    void nhanThueDiTheoCauHinh() throws IOException {
        HoaDonPdfService dichVu8 = new HoaDonPdfService(thamSoVoiThueSuat("0.08"), new PdfFont());
        String vanBan8 = trichVanBan(dichVu8.xuat(hoaDonMau(), khoanMucMau()));

        assertThat(vanBan8).contains("Thuế GTGT (8%)");
        assertThat(vanBan8).doesNotContain("Thuế GTGT (10%)");
    }

    private static String trichVanBan(byte[] noiDung) throws IOException {
        return TrichVanBanPdf.trich(noiDung);
    }

    // =================================================================

    @Test
    @DisplayName("1. File sinh ra đúng là một PDF hợp lệ, đọc lại được")
    void laFilePdfHopLe() {
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.ISO_8859_1))
                .as("PDF phải bắt đầu bằng chữ ký %%PDF-")
                .startsWith("%PDF-");
        assertThat(vanBan).isNotBlank();
    }

    /**
     * ⭐ Phép kiểm quan trọng nhất của mục B.
     *
     * <p>Mỗi chuỗi dưới đây chứa ít nhất một ký tự chỉ tồn tại khi font Unicode đã được nhúng
     * đúng. Thiếu font thì chúng biến thành ô vuông hoặc mất dấu, và test đỏ ngay.</p>
     */
    @Test
    @DisplayName("2. ⭐ Trích được chuỗi tiếng Việt CÓ DẤU từ nội dung PDF")
    void trichDuocChuoiCoDau() {
        assertThat(vanBan)
                .as("Thiếu font Unicode nhúng thì mọi chuỗi này sẽ hỏng dấu mà file vẫn hợp lệ")
                .contains("HÓA ĐƠN CƯỚC DỊCH VỤ VIỄN THÔNG")
                .contains("Kỳ cước")
                .contains("Hạn thanh toán")
                .contains("KHÁCH HÀNG")
                .contains("THUÊ BAO")
                .contains("Gói cước")
                .contains("Cộng tiền dịch vụ (trước thuế)")
                .contains("Thuế GTGT (10%)")
                .contains("TỔNG THANH TOÁN")
                .contains("Còn nợ")
                .contains("Số tiền bằng chữ");
    }

    @Test
    @DisplayName("3. ⭐ Dấu tiếng Việt trong DỮ LIỆU (không chỉ trong nhãn cố định)")
    void duLieuCoDauCungDung() {
        assertThat(vanBan)
                .contains("Nguyễn Thị Hoà")
                .contains("Ninh Kiều, Cần Thơ")
                .contains("Cước thoại nội mạng")
                .contains("Trả sau");
    }

    @Test
    @DisplayName("4. Số tiền bằng chữ in ra đúng, kể cả chữ có dấu")
    void soTienBangChu() {
        // 1.234.567 đ — cùng giá trị đã kiểm ở DocSoTienUtilTest
        assertThat(vanBan)
                .contains("Một triệu hai trăm ba mươi bốn nghìn năm trăm sáu mươi bảy đồng");
    }

    @Test
    @DisplayName("5. Chân trang ghi rõ đây là dữ liệu mẫu học tập")
    void chanTrangCanhBao() {
        assertThat(vanBan).contains(HoaDonPdfService.CHAN_TRANG);
        assertThat(vanBan).contains("DỮ LIỆU MẪU TỰ SINH");
    }

    @Test
    @DisplayName("6. Mọi con số tiền của hóa đơn đều xuất hiện, định dạng nghìn bằng dấu chấm")
    void moiConSoTienDeuCo() {
        assertThat(vanBan)
                .contains("1.234.567")   // tổng thanh toán
                .contains("1.122.334")   // trước thuế
                .contains("112.233")     // VAT
                .contains("150.000");    // một khoản mục
    }

    @Test
    @DisplayName("7. Tên file theo đúng quy ước HoaDon_{maHoaDon}.pdf")
    void tenFileDungQuyUoc() {
        assertThat(HoaDonPdfService.tenFile(hoaDonMau()))
                .isEqualTo("HoaDon_HD202606-000042.pdf");
    }

    /**
     * Hóa đơn không phát sinh cước chỉ có một dòng khoản mục — trường hợp đã có thật trong
     * dữ liệu (mục 45.1 của Phase 4). Bản in không được vỡ.
     */
    @Test
    @DisplayName("8. Hóa đơn chỉ có một khoản mục vẫn in được")
    void hoaDonMotKhoanMuc() throws IOException {
        HoaDon h = hoaDonMau();
        byte[] motDong = service.xuat(h, List.of(khoanMuc("Cước thuê bao tháng", null, null,
                new BigDecimal("50000"))));

        assertThat(trichVanBan(motDong))
                .contains("Cước thuê bao tháng")
                .contains("HÓA ĐƠN CƯỚC DỊCH VỤ VIỄN THÔNG");
    }

    // =================================================================
    // Dữ liệu mẫu — cố ý nhiều dấu tiếng Việt để test có cái mà bắt
    // =================================================================

    private static HoaDon hoaDonMau() {
        KhachHang kh = new KhachHang();
        kh.setMaKh("KH000042");
        kh.setTenKh("Nguyễn Thị Hoà");
        kh.setDiaChi("12 Nguyễn Trãi, Ninh Kiều, Cần Thơ");

        GoiCuoc gc = new GoiCuoc();
        gc.setMaGoi("MAX150");
        gc.setTenGoi("Gói cước Max 150");

        ThueBao tb = new ThueBao();
        tb.setId(1L);
        tb.setSoThueBao("0834567834");
        tb.setLoaiThueBao(LoaiThueBao.TRA_SAU);
        tb.setGoiCuoc(gc);

        KyCuoc ky = new KyCuoc();
        ky.setId(1L);
        ky.setThang(6);
        ky.setNam(2026);

        HoaDon h = new HoaDon();
        h.setId(42L);
        h.setMaHoaDon("HD202606-000042");
        h.setKhachHang(kh);
        h.setThueBao(tb);
        h.setKyCuoc(ky);
        h.setNgayLap(LocalDate.of(2026, 6, 30));
        h.setHanThanhToan(LocalDate.of(2026, 7, 15));
        h.setGiamTru(new BigDecimal("10000"));
        h.setTongTruocThue(new BigDecimal("1122334"));
        h.setThueVat(new BigDecimal("112233"));
        h.setTongThanhToan(new BigDecimal("1234567"));
        h.setDaThanhToan(BigDecimal.ZERO);
        h.setConNo(new BigDecimal("1234567"));
        h.setTrangThai(TrangThaiHoaDon.CHUA_TT);
        return h;
    }

    private static List<ChiTietHoaDon> khoanMucMau() {
        return List.of(
                khoanMuc("Cước thuê bao tháng", null, null, new BigDecimal("150000")),
                khoanMuc("Cước thoại nội mạng", new BigDecimal("120"), "phút",
                        new BigDecimal("500000")),
                khoanMuc("Cước tin nhắn ngoại mạng", new BigDecimal("45"), "tin",
                        new BigDecimal("472334")));
    }

    private static ChiTietHoaDon khoanMuc(String ten, BigDecimal soLuong, String donVi,
                                          BigDecimal thanhTien) {
        ChiTietHoaDon ct = new ChiTietHoaDon();
        ct.setKhoanMuc(ten);
        ct.setSoLuong(soLuong);
        ct.setDonVi(donVi);
        ct.setThanhTien(thanhTien);
        return ct;
    }
}
