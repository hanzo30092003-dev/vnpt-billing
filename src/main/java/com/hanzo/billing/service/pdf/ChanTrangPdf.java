package com.hanzo.billing.service.pdf;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Chân trang in trên <b>mọi</b> trang của tài liệu PDF.
 *
 * <p>Dùng sự kiện trang thay vì thêm một đoạn văn ở cuối tài liệu: hóa đơn có nhiều khoản mục
 * sẽ tràn sang trang thứ hai, và dòng cảnh báo "dữ liệu mẫu" phải có mặt ở từng trang chứ
 * không chỉ trang cuối.</p>
 */
public class ChanTrangPdf extends PdfPageEventHelper {

    private final PdfFont font;
    private final String noiDung;

    public ChanTrangPdf(PdfFont font, String noiDung) {
        this.font = font;
        this.noiDung = noiDung;
    }

    @Override
    public void onEndPage(PdfWriter writer, Document taiLieu) {
        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_CENTER,
                new Phrase(noiDung, font.phu(8)),
                (taiLieu.left() + taiLieu.right()) / 2, taiLieu.bottom() - 18, 0);

        ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                new Phrase("Trang " + writer.getPageNumber(), font.phu(8)),
                taiLieu.right(), taiLieu.bottom() - 18, 0);
    }
}
