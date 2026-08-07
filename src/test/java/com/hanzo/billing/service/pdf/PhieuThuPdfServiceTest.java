package com.hanzo.billing.service.pdf;

import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.HinhThucThanhToan;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Cùng nguyên tắc với {@link HoaDonPdfServiceTest}: đọc lại nội dung, không chỉ kiểm file. */
@DisplayName("Xuất phiếu thu ra PDF")
class PhieuThuPdfServiceTest {

    private static PhieuThuPdfService service;
    private static String vanBan;

    @BeforeAll
    static void dungPdfMotLan() throws IOException {
        service = new PhieuThuPdfService(new PdfFont());
        vanBan = TrichVanBanPdf.trich(service.xuat(giaoDichMau()));
    }

    @Test
    @DisplayName("1. ⭐ Trích được chuỗi tiếng Việt có dấu từ phiếu thu")
    void trichDuocChuoiCoDau() {
        assertThat(vanBan)
                .contains("PHIẾU THU TIỀN")
                .contains("Khách hàng")
                .contains("Số thuê bao")
                .contains("Lý do thu")
                .contains("Hình thức")
                .contains("Người thu")
                .contains("NGƯỜI NỘP TIỀN")
                .contains("Số tiền bằng chữ");
    }

    @Test
    @DisplayName("2. Đủ thông tin giao dịch: mã, khách hàng, thuê bao, hóa đơn, hình thức")
    void duThongTinGiaoDich() {
        assertThat(vanBan)
                .contains("TT20260615-0001")
                .contains("Nguyễn Văn Bảy")
                .contains("0912345602")
                .contains("HD202605-000007")
                .contains("Chuyển khoản")
                .contains("Trần Thị Kế Toán");
    }

    @Test
    @DisplayName("3. Số tiền in cả bằng số lẫn bằng chữ")
    void soTienBangSoVaBangChu() {
        assertThat(vanBan)
                .contains("204.780 đ")
                .contains("Hai trăm linh bốn nghìn bảy trăm tám mươi đồng");
    }

    @Test
    @DisplayName("4. Chân trang ghi rõ là phiếu thu mẫu học tập")
    void chanTrangCanhBao() {
        assertThat(vanBan).contains(PhieuThuPdfService.CHAN_TRANG);
    }

    @Test
    @DisplayName("5. Tên file theo quy ước PhieuThu_{maGiaoDich}.pdf")
    void tenFileDungQuyUoc() {
        assertThat(PhieuThuPdfService.tenFile(giaoDichMau()))
                .isEqualTo("PhieuThu_TT20260615-0001.pdf");
    }

    @Test
    @DisplayName("6. Giao dịch không có người thu và không ghi chú vẫn in được")
    void thieuNguoiThuVanIn() throws IOException {
        ThanhToan gd = giaoDichMau();
        gd.setNguoiThu(null);
        gd.setGhiChu(null);

        assertThat(TrichVanBanPdf.trich(service.xuat(gd))).contains("PHIẾU THU TIỀN");
    }

    // =================================================================

    private static ThanhToan giaoDichMau() {
        KhachHang kh = new KhachHang();
        kh.setMaKh("KH000002");
        kh.setTenKh("Nguyễn Văn Bảy");

        ThueBao tb = new ThueBao();
        tb.setSoThueBao("0912345602");

        KyCuoc ky = new KyCuoc();
        ky.setThang(5);
        ky.setNam(2026);

        HoaDon hd = new HoaDon();
        hd.setId(7L);
        hd.setMaHoaDon("HD202605-000007");
        hd.setKhachHang(kh);
        hd.setThueBao(tb);
        hd.setKyCuoc(ky);

        NguoiDung nguoiThu = new NguoiDung();
        nguoiThu.setHoTen("Trần Thị Kế Toán");

        ThanhToan gd = new ThanhToan();
        gd.setId(1L);
        gd.setMaGiaoDich("TT20260615-0001");
        gd.setHoaDon(hd);
        gd.setSoTien(new BigDecimal("204780"));
        gd.setHinhThuc(HinhThucThanhToan.CHUYEN_KHOAN);
        gd.setNgayThanhToan(LocalDateTime.of(LocalDate.of(2026, 6, 15), java.time.LocalTime.of(9, 30)));
        gd.setNguoiThu(nguoiThu);
        gd.setGhiChu("Thu tại quầy Ninh Kiều");
        return gd;
    }
}
