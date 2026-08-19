package com.hanzo.billing.service.pdf;

import com.hanzo.billing.entity.ChiTietHoaDon;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.util.DocSoTienUtil;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Xuất hóa đơn ra file PDF khổ A4.
 *
 * <p>Bản in <b>đọc lại</b> mọi con số từ hóa đơn đã ghi, không tính lại bất cứ giá trị nào —
 * cùng nguyên tắc với bảng đối soát của mục 4E (bài học 43.6). Nếu bản PDF tự tính lại theo
 * cách riêng thì nó không chứng minh được hóa đơn đúng, nó chỉ chứng minh chính nó.</p>
 */
@Service
@RequiredArgsConstructor
public class HoaDonPdfService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final float LE = 36f;

    public static final String CHAN_TRANG = "Hóa đơn mẫu phục vụ mục đích học tập";

    private final com.hanzo.billing.config.ThamSoNghiepVu thamSo;
    private final PdfFont font;

    /** Tên file tải về, theo đúng quy ước {@code HoaDon_{maHoaDon}.pdf}. */
    public static String tenFile(HoaDon hoaDon) {
        return "HoaDon_" + hoaDon.getMaHoaDon() + ".pdf";
    }

    public byte[] xuat(HoaDon hoaDon, List<ChiTietHoaDon> khoanMuc) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document taiLieu = new Document(PageSize.A4, LE, LE, LE, LE + 12);
            PdfWriter writer = PdfWriter.getInstance(taiLieu, out);
            writer.setPageEvent(new ChanTrangPdf(font, CHAN_TRANG));
            taiLieu.open();

            taiLieu.add(tieuDeChinh());
            taiLieu.add(khoiTieuDe(hoaDon));
            taiLieu.add(canhBaoDuLieuMau());
            taiLieu.add(khoiKhachHang(hoaDon));
            taiLieu.add(bangKhoanMuc(khoanMuc));
            taiLieu.add(khoiTong(hoaDon));
            taiLieu.add(dongBangChu(hoaDon));
            taiLieu.add(khoiKy());

            taiLieu.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Không xuất được PDF cho hóa đơn " + hoaDon.getMaHoaDon(), ex);
        }
    }

    // =================================================================

    /**
     * Tiêu đề chạy trọn chiều ngang trang.
     *
     * <p>Cố ý <b>không</b> nhét tiêu đề vào cột phải cùng với số hóa đơn: chuỗi
     * "HÓA ĐƠN CƯỚC DỊCH VỤ VIỄN THÔNG" dài hơn nửa trang khi in đậm, nên nó sẽ tự xuống
     * dòng giữa chừng — bản in vẫn ra nhưng tiêu đề gãy làm đôi. Đặt riêng một dòng thì
     * không phụ thuộc vào việc canh tỷ lệ cột cho vừa khít.</p>
     */
    private Paragraph tieuDeChinh() {
        Paragraph p = doanGiua("HÓA ĐƠN CƯỚC DỊCH VỤ VIỄN THÔNG", font.dam(14));
        p.setSpacingAfter(10f);
        return p;
    }

    private PdfPTable khoiTieuDe(HoaDon hoaDon) {
        PdfPTable bang = bangKhungRong(new float[]{55, 45});

        PdfPCell trai = oTrong();
        trai.addElement(doan("CÔNG TY VIỄN THÔNG VNPT (GIẢ LẬP)", font.dam(11)));
        trai.addElement(doan("01 Đại lộ Hòa Bình, Ninh Kiều, Cần Thơ", font.phu(9)));
        trai.addElement(doan("Mã số thuế: 0100000000 · Điện thoại: 1800 1166", font.phu(9)));
        bang.addCell(trai);

        PdfPCell phai = oTrong();
        phai.setHorizontalAlignment(Element.ALIGN_RIGHT);
        phai.addElement(doanPhai("Số: " + hoaDon.getMaHoaDon(), font.dam(10)));
        phai.addElement(doanPhai("Kỳ cước: " + hoaDon.getKyCuoc().getThang()
                + "/" + hoaDon.getKyCuoc().getNam(), font.thuong(10)));
        phai.addElement(doanPhai("Ngày lập: " + hoaDon.getNgayLap().format(NGAY), font.thuong(10)));
        phai.addElement(doanPhai("Hạn thanh toán: "
                + hoaDon.getHanThanhToan().format(NGAY), font.thuong(10)));
        bang.addCell(phai);

        bang.setSpacingAfter(8f);
        return bang;
    }

    private PdfPTable canhBaoDuLieuMau() {
        PdfPTable bang = new PdfPTable(1);
        bang.setWidthPercentage(100);
        PdfPCell o = new PdfPCell(new Phrase(
                "DỮ LIỆU MẪU TỰ SINH — PHỤC VỤ MỤC ĐÍCH HỌC TẬP, KHÔNG PHẢI HÓA ĐƠN THẬT",
                font.dam(9)));
        o.setHorizontalAlignment(Element.ALIGN_CENTER);
        o.setPadding(5f);
        o.setBackgroundColor(PdfFont.MAU_NEN_TIEU_DE);
        o.setBorderColor(PdfFont.MAU_VIEN);
        bang.addCell(o);
        bang.setSpacingAfter(10f);
        return bang;
    }

    private PdfPTable khoiKhachHang(HoaDon hoaDon) {
        PdfPTable bang = new PdfPTable(new float[]{50, 50});
        bang.setWidthPercentage(100);

        PdfPCell trai = oVien();
        trai.addElement(doan("KHÁCH HÀNG", font.dam(9)));
        trai.addElement(doan("Mã KH: " + hoaDon.getKhachHang().getMaKh(), font.thuong(9)));
        trai.addElement(doan("Tên: " + hoaDon.getKhachHang().getTenKh(), font.thuong(9)));
        trai.addElement(doan("Địa chỉ: " + nvl(hoaDon.getKhachHang().getDiaChi()), font.thuong(9)));
        bang.addCell(trai);

        PdfPCell phai = oVien();
        phai.addElement(doan("THUÊ BAO", font.dam(9)));
        phai.addElement(doan("Số thuê bao: " + hoaDon.getThueBao().getSoThueBao(), font.thuong(9)));
        phai.addElement(doan("Gói cước: " + hoaDon.getThueBao().getGoiCuoc().getMaGoi()
                + " — " + hoaDon.getThueBao().getGoiCuoc().getTenGoi(), font.thuong(9)));
        phai.addElement(doan("Loại: " + hoaDon.getThueBao().getLoaiThueBao().getNhan(),
                font.thuong(9)));
        bang.addCell(phai);

        bang.setSpacingAfter(10f);
        return bang;
    }

    private PdfPTable bangKhoanMuc(List<ChiTietHoaDon> khoanMuc) {
        PdfPTable bang = new PdfPTable(new float[]{7, 43, 13, 10, 13, 14});
        bang.setWidthPercentage(100);
        bang.setHeaderRows(1);

        for (String tieuDe : new String[]{"STT", "Khoản mục", "Số lượng", "Đơn vị",
                "Đơn giá", "Thành tiền"}) {
            PdfPCell o = new PdfPCell(new Phrase(tieuDe, font.dam(9)));
            o.setBackgroundColor(PdfFont.MAU_NEN_TIEU_DE);
            o.setBorderColor(PdfFont.MAU_VIEN);
            o.setPadding(4f);
            o.setHorizontalAlignment(Element.ALIGN_CENTER);
            bang.addCell(o);
        }

        int stt = 1;
        for (ChiTietHoaDon ct : khoanMuc) {
            bang.addCell(oBang(String.valueOf(stt++), Element.ALIGN_CENTER));
            bang.addCell(oBang(ct.getKhoanMuc(), Element.ALIGN_LEFT));
            bang.addCell(oBang(soHoacGach(ct.getSoLuong()), Element.ALIGN_RIGHT));
            bang.addCell(oBang(nvlGach(ct.getDonVi()), Element.ALIGN_CENTER));
            // Đơn giá để trống ở ba dòng cước sử dụng — mỗi dòng là tổng của nhiều bản ghi
            // áp đơn giá khác nhau nên không tồn tại MỘT đơn giá. Xem PHASE-4-REPORT 17.3.
            bang.addCell(oBang(soHoacGach(ct.getDonGia()), Element.ALIGN_RIGHT));
            bang.addCell(oBang(tien(ct.getThanhTien()), Element.ALIGN_RIGHT));
        }

        bang.setSpacingAfter(8f);
        return bang;
    }

    private PdfPTable khoiTong(HoaDon hoaDon) {
        PdfPTable ngoai = bangKhungRong(new float[]{55, 45});
        ngoai.addCell(oTrong());

        PdfPTable trong = new PdfPTable(new float[]{58, 42});
        trong.setWidthPercentage(100);

        if (duong(hoaDon.getGiamTru())) {
            dongTong(trong, "Giảm trừ", "−" + tien(hoaDon.getGiamTru()), false);
        }
        dongTong(trong, "Cộng tiền dịch vụ (trước thuế)", tien(hoaDon.getTongTruocThue()), false);
        dongTong(trong, "Thuế GTGT (" + thamSo.nhanThueSuat() + ")", tien(hoaDon.getThueVat()), false);
        dongTong(trong, "TỔNG THANH TOÁN", tien(hoaDon.getTongThanhToan()), true);
        dongTong(trong, "Đã thanh toán", tien(hoaDon.getDaThanhToan()), false);
        dongTong(trong, "Còn nợ", tien(hoaDon.getConNo()), true);

        PdfPCell o = oTrong();
        o.addElement(trong);
        ngoai.addCell(o);
        ngoai.setSpacingAfter(6f);
        return ngoai;
    }

    private Paragraph dongBangChu(HoaDon hoaDon) {
        Paragraph p = new Paragraph();
        p.add(new Phrase("Số tiền bằng chữ: ", font.phu(9)));
        p.add(new Phrase(DocSoTienUtil.docSoTien(hoaDon.getTongThanhToan()), font.nghieng(10)));
        p.setSpacingAfter(18f);
        return p;
    }

    private PdfPTable khoiKy() {
        PdfPTable bang = bangKhungRong(new float[]{50, 50});
        PdfPCell trai = oTrong();
        trai.setHorizontalAlignment(Element.ALIGN_CENTER);
        trai.addElement(doanGiua("NGƯỜI NỘP TIỀN", font.dam(9)));
        trai.addElement(doanGiua("(Ký, ghi rõ họ tên)", font.phu(8)));
        bang.addCell(trai);

        PdfPCell phai = oTrong();
        phai.addElement(doanGiua("ĐƠN VỊ PHÁT HÀNH", font.dam(9)));
        phai.addElement(doanGiua("(Ký, đóng dấu)", font.phu(8)));
        bang.addCell(phai);
        return bang;
    }

    // =================================================================
    // Tiện ích dựng ô và định dạng
    // =================================================================

    private void dongTong(PdfPTable bang, String nhan, String giaTri, boolean noiBat) {
        com.lowagie.text.Font kieu = noiBat ? font.dam(10) : font.thuong(9);
        PdfPCell oNhan = new PdfPCell(new Phrase(nhan, kieu));
        oNhan.setBorder(Rectangle.NO_BORDER);
        oNhan.setPaddingBottom(3f);
        bang.addCell(oNhan);

        PdfPCell oGiaTri = new PdfPCell(new Phrase(giaTri, kieu));
        oGiaTri.setBorder(Rectangle.NO_BORDER);
        oGiaTri.setHorizontalAlignment(Element.ALIGN_RIGHT);
        oGiaTri.setPaddingBottom(3f);
        bang.addCell(oGiaTri);
    }

    private static PdfPTable bangKhungRong(float[] tyLe) {
        PdfPTable bang = new PdfPTable(tyLe);
        bang.setWidthPercentage(100);
        return bang;
    }

    private static PdfPCell oTrong() {
        PdfPCell o = new PdfPCell();
        o.setBorder(Rectangle.NO_BORDER);
        return o;
    }

    private static PdfPCell oVien() {
        PdfPCell o = new PdfPCell();
        o.setBorderColor(PdfFont.MAU_VIEN);
        o.setPadding(6f);
        return o;
    }

    private PdfPCell oBang(String noiDung, int canLe) {
        PdfPCell o = new PdfPCell(new Phrase(noiDung, font.thuong(9)));
        o.setBorderColor(PdfFont.MAU_VIEN);
        o.setPadding(4f);
        o.setHorizontalAlignment(canLe);
        return o;
    }

    private static Paragraph doan(String noiDung, com.lowagie.text.Font kieu) {
        Paragraph p = new Paragraph(noiDung, kieu);
        p.setSpacingAfter(2f);
        return p;
    }

    private static Paragraph doanPhai(String noiDung, com.lowagie.text.Font kieu) {
        Paragraph p = doan(noiDung, kieu);
        p.setAlignment(Element.ALIGN_RIGHT);
        return p;
    }

    private static Paragraph doanGiua(String noiDung, com.lowagie.text.Font kieu) {
        Paragraph p = doan(noiDung, kieu);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    static String tien(BigDecimal giaTri) {
        BigDecimal v = giaTri == null ? BigDecimal.ZERO : giaTri;
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.ROOT);
        kyHieu.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", kyHieu).format(v) + " đ";
    }

    private static String soHoacGach(BigDecimal giaTri) {
        if (giaTri == null || giaTri.signum() == 0) {
            return "—";
        }
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.ROOT);
        kyHieu.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", kyHieu).format(giaTri);
    }

    private static boolean duong(BigDecimal giaTri) {
        return giaTri != null && giaTri.signum() > 0;
    }

    private static String nvl(String chuoi) {
        return chuoi == null ? "" : chuoi;
    }

    private static String nvlGach(String chuoi) {
        return (chuoi == null || chuoi.isBlank()) ? "—" : chuoi;
    }

    /** Ngày in ra chân trang, tách riêng để test không phụ thuộc đồng hồ. */
    static String ngayInHomNay() {
        return LocalDate.now().format(NGAY);
    }
}
