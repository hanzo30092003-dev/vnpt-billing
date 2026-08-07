package com.hanzo.billing.service.pdf;

import com.lowagie.text.Font;
import com.lowagie.text.pdf.BaseFont;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;

/**
 * Font Unicode nhúng vào PDF — nạp một lần, dùng lại cho mọi bản in.
 *
 * <h2>Vì sao bắt buộc phải nhúng font</h2>
 * <p>PDF chỉ hiện đúng dấu tiếng Việt khi font được <b>nhúng</b> vào file kèm bảng mã
 * {@link BaseFont#IDENTITY_H}. Dùng font Base14 có sẵn của chuẩn PDF (Helvetica, Times) thì
 * mọi ký tự có dấu sẽ ra ô vuông hoặc mất dấu.</p>
 *
 * <p>Điều nguy hiểm là lỗi đó <b>không làm hỏng file</b>: PDF vẫn mở được bình thường, vẫn in
 * được, chỉ có chữ là sai. Không có ngoại lệ nào ném ra, không có cảnh báo nào. Đây đúng loại
 * lỗi mà bài học 43.5 nói tới — nên test bắt buộc phải <b>đọc lại</b> nội dung PDF và so khớp
 * chuỗi có dấu, chứ kiểm "file có tồn tại" thì không chứng minh được gì.</p>
 *
 * <p>Font dùng là <b>Liberation Sans</b> (SIL OFL 1.1) — xem {@code resources/fonts/NGUON-FONT.txt}
 * về nguồn gốc và lý do không dùng font của Windows.</p>
 */
@Component
@Slf4j
public class PdfFont {

    private static final String DUONG_DAN = "fonts/LiberationSans-Regular.ttf";

    /** Màu chữ phụ (nhãn, ghi chú) — xám đậm để bản in đen trắng vẫn tách được khỏi chữ chính. */
    public static final Color MAU_PHU = new Color(90, 90, 90);
    public static final Color MAU_VIEN = new Color(170, 170, 170);
    public static final Color MAU_NEN_TIEU_DE = new Color(238, 238, 238);

    private final BaseFont baseFont;

    public PdfFont() {
        this.baseFont = nap();
    }

    private static BaseFont nap() {
        try (InputStream in = new ClassPathResource(DUONG_DAN).getInputStream()) {
            byte[] duLieu = in.readAllBytes();
            // Tham số cuối là byte[] của chính file font: buộc OpenPDF nhúng font vào PDF
            // thay vì chỉ tham chiếu tên font và trông chờ máy đọc có sẵn.
            return BaseFont.createFont(DUONG_DAN, BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, duLieu, null);
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Không nạp được font " + DUONG_DAN + " để xuất PDF. Thiếu font này thì "
                            + "mọi chữ có dấu trong PDF sẽ hỏng mà không báo lỗi.", ex);
        }
    }

    public Font thuong(float coChu) {
        return new Font(baseFont, coChu, Font.NORMAL);
    }

    public Font dam(float coChu) {
        return new Font(baseFont, coChu, Font.BOLD);
    }

    public Font nghieng(float coChu) {
        return new Font(baseFont, coChu, Font.ITALIC);
    }

    public Font phu(float coChu) {
        return new Font(baseFont, coChu, Font.NORMAL, MAU_PHU);
    }
}
