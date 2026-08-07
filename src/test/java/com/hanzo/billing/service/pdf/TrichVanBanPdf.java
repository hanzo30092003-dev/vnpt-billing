package com.hanzo.billing.service.pdf;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.IOException;

/**
 * Trích văn bản từ PDF để test đọc lại nội dung — dùng chung cho hóa đơn và phiếu thu.
 *
 * <h2>Vì sao KHÔNG bật {@code setSortByPosition}</h2>
 * <p>Bật sắp-theo-vị-trí cho thứ tự đọc đẹp hơn, nhưng nó tính lại khoảng cách từng ký tự để
 * đoán chỗ ngắt từ. Với chữ in đậm cỡ lớn — đúng như tiêu đề "PHIẾU THU TIỀN" 15pt — heuristic
 * đó chèn <b>khoảng trắng giả vào giữa từ</b>: chuỗi trích ra thành {@code "PHIẾU T HU TIỀN"}
 * trong khi bản in hoàn toàn bình thường.</p>
 *
 * <p>Đó là một <b>phép kiểm sai</b> chứ không phải lỗi sản phẩm — nếu tin nó thì sẽ đi sửa
 * layout của một bản in đang đúng (bài học 43.5). Thứ tự đọc không có giá trị gì với các
 * khẳng định {@code contains}, nên bỏ hẳn tuỳ chọn này và giữ hành vi mặc định.</p>
 */
final class TrichVanBanPdf {

    private TrichVanBanPdf() {
    }

    static String trich(byte[] noiDungPdf) throws IOException {
        try (PDDocument taiLieu = Loader.loadPDF(noiDungPdf)) {
            return new PDFTextStripper().getText(taiLieu);
        }
    }
}
