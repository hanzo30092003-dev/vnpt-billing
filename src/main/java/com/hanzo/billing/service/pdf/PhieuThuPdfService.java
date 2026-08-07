package com.hanzo.billing.service.pdf;

import com.hanzo.billing.entity.ThanhToan;
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
import java.time.format.DateTimeFormatter;

/**
 * Phiếu thu tiền — bản in kèm theo mỗi giao dịch thanh toán.
 *
 * <p>Khổ A5 nằm ngang thay vì A4: phiếu thu thật in trên giấy nhỏ, và khổ này in được hai
 * phiếu trên một tờ A4 khi cần.</p>
 */
@Service
@RequiredArgsConstructor
public class PhieuThuPdfService {

    private static final DateTimeFormatter NGAY_GIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public static final String CHAN_TRANG = "Phiếu thu mẫu phục vụ mục đích học tập";

    private final PdfFont font;

    public static String tenFile(ThanhToan giaoDich) {
        return "PhieuThu_" + giaoDich.getMaGiaoDich() + ".pdf";
    }

    public byte[] xuat(ThanhToan giaoDich) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document taiLieu = new Document(PageSize.A5.rotate(), 32, 32, 28, 40);
            PdfWriter writer = PdfWriter.getInstance(taiLieu, out);
            writer.setPageEvent(new ChanTrangPdf(font, CHAN_TRANG));
            taiLieu.open();

            taiLieu.add(canhLe("CÔNG TY VIỄN THÔNG VNPT (GIẢ LẬP)", font.dam(10),
                    Element.ALIGN_LEFT, 2f));
            taiLieu.add(canhLe("PHIẾU THU TIỀN", font.dam(15), Element.ALIGN_CENTER, 2f));
            taiLieu.add(canhLe("Số: " + giaoDich.getMaGiaoDich(), font.thuong(10),
                    Element.ALIGN_CENTER, 12f));

            taiLieu.add(bangThongTin(giaoDich));

            Paragraph bangChu = new Paragraph();
            bangChu.add(new Phrase("Số tiền bằng chữ: ", font.phu(9)));
            bangChu.add(new Phrase(DocSoTienUtil.docSoTien(giaoDich.getSoTien()),
                    font.nghieng(10)));
            bangChu.setSpacingBefore(6f);
            bangChu.setSpacingAfter(16f);
            taiLieu.add(bangChu);

            taiLieu.add(khoiKy());

            taiLieu.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Không xuất được phiếu thu " + giaoDich.getMaGiaoDich(), ex);
        }
    }

    private PdfPTable bangThongTin(ThanhToan gd) {
        PdfPTable bang = new PdfPTable(new float[]{32, 68});
        bang.setWidthPercentage(100);

        dong(bang, "Khách hàng", gd.getHoaDon().getKhachHang().getTenKh(), true);
        dong(bang, "Mã khách hàng", gd.getHoaDon().getKhachHang().getMaKh(), false);
        dong(bang, "Số thuê bao", gd.getHoaDon().getThueBao().getSoThueBao(), false);
        dong(bang, "Lý do thu", "Thanh toán cước hóa đơn "
                + gd.getHoaDon().getMaHoaDon() + " (kỳ "
                + gd.getHoaDon().getKyCuoc().getThang() + "/"
                + gd.getHoaDon().getKyCuoc().getNam() + ")", false);
        dong(bang, "Số tiền", HoaDonPdfService.tien(gd.getSoTien()), true);
        dong(bang, "Hình thức", gd.getHinhThuc().getNhan(), false);
        dong(bang, "Ngày thu", gd.getNgayThanhToan().format(NGAY_GIO), false);
        dong(bang, "Người thu", gd.getNguoiThu() == null ? "—" : gd.getNguoiThu().getHoTen(),
                false);
        if (gd.getGhiChu() != null && !gd.getGhiChu().isBlank()) {
            dong(bang, "Ghi chú", gd.getGhiChu(), false);
        }
        return bang;
    }

    private void dong(PdfPTable bang, String nhan, String giaTri, boolean noiBat) {
        PdfPCell oNhan = new PdfPCell(new Phrase(nhan, font.phu(9)));
        oNhan.setBorder(Rectangle.NO_BORDER);
        oNhan.setPaddingBottom(4f);
        bang.addCell(oNhan);

        PdfPCell oGiaTri = new PdfPCell(new Phrase(giaTri,
                noiBat ? font.dam(11) : font.thuong(10)));
        oGiaTri.setBorder(Rectangle.NO_BORDER);
        oGiaTri.setPaddingBottom(4f);
        bang.addCell(oGiaTri);
    }

    private PdfPTable khoiKy() {
        PdfPTable bang = new PdfPTable(new float[]{50, 50});
        bang.setWidthPercentage(100);
        bang.addCell(oKy("NGƯỜI NỘP TIỀN"));
        bang.addCell(oKy("NGƯỜI THU TIỀN"));
        return bang;
    }

    private PdfPCell oKy(String nhan) {
        PdfPCell o = new PdfPCell();
        o.setBorder(Rectangle.NO_BORDER);
        o.addElement(canhLe(nhan, font.dam(9), Element.ALIGN_CENTER, 1f));
        o.addElement(canhLe("(Ký, ghi rõ họ tên)", font.phu(8), Element.ALIGN_CENTER, 1f));
        return o;
    }

    private static Paragraph canhLe(String noiDung, com.lowagie.text.Font kieu,
                                    int canLe, float cachDuoi) {
        Paragraph p = new Paragraph(noiDung, kieu);
        p.setAlignment(canLe);
        p.setSpacingAfter(cachDuoi);
        return p;
    }
}
