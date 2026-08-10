package com.hanzo.billing.service.baocao;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Bộ dựng file Excel dùng chung cho <b>mọi</b> báo cáo của Phase 6.
 *
 * <h2>Vì sao gom lại</h2>
 * <p>Ba lớp của Phase 3–5 ({@code ChiTietSuDungService}, {@code HoaDonServiceImpl},
 * {@code CongNoServiceImpl}, {@code ThanhToanServiceImpl}) mỗi lớp tự viết lại đúng cùng một
 * đoạn: tạo workbook, tạo kiểu chữ đậm, đổ tiêu đề, tự động giãn cột. Bốn bản sao của cùng
 * một việc, và mỗi bản lệch nhau một chút.</p>
 *
 * <p>Bảy báo cáo của Phase 6 sẽ thành bảy bản sao nữa nếu không gom. Lớp này đặt ra khuôn
 * chung, và khuôn đó chính là yêu cầu D.1: tiêu đề, khoảng thời gian, ngày xuất, header in
 * đậm có nền, <b>freeze pane</b>, số tiền có phân cách nghìn, dòng tổng in đậm, và chân trang
 * ghi rõ đây là dữ liệu mẫu.</p>
 *
 * <p>Dùng theo lối gọi dây chuyền:</p>
 * <pre>
 * return new BaoCaoExcel("Doanh thu theo kỳ", "Toàn bộ 5 kỳ")
 *         .tieuDeCot("Kỳ", "Số hóa đơn", "Phát sinh")
 *         .dong(r -> { r.chu("6/2026"); r.so(58); r.tien(...); })
 *         .dongTong(r -> { ... })
 *         .xuat();
 * </pre>
 */
public class BaoCaoExcel {

    private static final DateTimeFormatter NGAY_GIO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Ghi ở chân mọi file xuất ra — bắt buộc theo yêu cầu D.1. */
    private static final String CHAN_TRANG =
            "Dữ liệu mẫu phục vụ mục đích học tập — Đồ án Thực tập nghề nghiệp";

    /** Định dạng tiền của Excel: dấu chấm phân cách nghìn theo quy ước Việt Nam. */
    private static final String DINH_DANG_TIEN = "#,##0";

    private final Workbook workbook = new XSSFWorkbook();
    private final Sheet sheet;
    private final CellStyle kieuTieuDeBang;
    private final CellStyle kieuHeader;
    private final CellStyle kieuTien;
    private final CellStyle kieuTienTong;
    private final CellStyle kieuChuTong;
    private final CellStyle kieuPhu;

    private int soCot;
    private int dongHienTai;
    private int dongHeader = -1;

    public BaoCaoExcel(String tieuDe, String khoangThoiGian) {
        this.sheet = workbook.createSheet(tenSheetHopLe(tieuDe));
        this.kieuTieuDeBang = taoKieuTieuDe();
        this.kieuHeader = taoKieuHeader();
        this.kieuTien = taoKieuTien(false);
        this.kieuTienTong = taoKieuTien(true);
        this.kieuChuTong = taoKieuChuDam();
        this.kieuPhu = taoKieuPhu();

        oChu(dongHienTai++, 0, tieuDe, kieuTieuDeBang);
        oChu(dongHienTai++, 0, "Phạm vi: " + khoangThoiGian, kieuPhu);
        oChu(dongHienTai++, 0, "Ngày xuất: " + LocalDateTime.now().format(NGAY_GIO), kieuPhu);
        dongHienTai++;   // một dòng trống trước bảng
    }

    /** Một dòng đang được ghi. Người gọi ghi lần lượt từ trái sang phải. */
    public final class Dong {
        private final Row row;
        private final boolean dongTong;
        private int cot;

        private Dong(Row row, boolean dongTong) {
            this.row = row;
            this.dongTong = dongTong;
        }

        public Dong chu(String giaTri) {
            Cell o = row.createCell(cot++);
            o.setCellValue(giaTri == null ? "" : giaTri);
            if (dongTong) {
                o.setCellStyle(kieuChuTong);
            }
            return this;
        }

        public Dong so(Number giaTri) {
            Cell o = row.createCell(cot++);
            if (giaTri != null) {
                o.setCellValue(giaTri.doubleValue());
            }
            if (dongTong) {
                o.setCellStyle(kieuChuTong);
            }
            return this;
        }

        /** Ô tiền: luôn mang định dạng phân cách nghìn, kể cả khi giá trị bằng 0. */
        public Dong tien(BigDecimal giaTri) {
            Cell o = row.createCell(cot++);
            o.setCellValue(giaTri == null ? 0 : giaTri.doubleValue());
            o.setCellStyle(dongTong ? kieuTienTong : kieuTien);
            return this;
        }

        /**
         * Ô tỷ lệ. {@code null} nghĩa là <b>không tính được</b> (mẫu số bằng 0), và khi đó ghi
         * gạch ngang chứ không ghi 0% — hai điều đó khác nhau.
         */
        public Dong tyLe(BigDecimal giaTri) {
            Cell o = row.createCell(cot++);
            o.setCellValue(giaTri == null ? "—" : giaTri + "%");
            if (dongTong) {
                o.setCellStyle(kieuChuTong);
            }
            return this;
        }
    }

    /** Người dùng của lớp này ghi nội dung một dòng qua giao diện {@link Dong}. */
    @FunctionalInterface
    public interface GhiDong {
        void ghi(Dong dong);
    }

    public BaoCaoExcel tieuDeCot(String... tieuDe) {
        this.soCot = tieuDe.length;
        this.dongHeader = dongHienTai;
        Row row = sheet.createRow(dongHienTai++);
        for (int i = 0; i < tieuDe.length; i++) {
            Cell o = row.createCell(i);
            o.setCellValue(tieuDe[i]);
            o.setCellStyle(kieuHeader);
        }
        return this;
    }

    public BaoCaoExcel dong(GhiDong noiDung) {
        noiDung.ghi(new Dong(sheet.createRow(dongHienTai++), false));
        return this;
    }

    public BaoCaoExcel dongTong(GhiDong noiDung) {
        noiDung.ghi(new Dong(sheet.createRow(dongHienTai++), true));
        return this;
    }

    /** Dòng chữ giữa bảng, dùng khi báo cáo không có dữ liệu. */
    public BaoCaoExcel dongTrong(String thongBao) {
        oChu(dongHienTai++, 0, thongBao, kieuPhu);
        return this;
    }

    /**
     * Đóng file: giãn cột, khoá dòng tiêu đề rồi ghi chân trang.
     *
     * <p><b>Freeze pane đặt ngay dưới dòng header</b> chứ không phải dòng 1: phía trên header
     * còn ba dòng tiêu đề báo cáo, khoá nhầm chỗ thì cuộn xuống sẽ mất tên cột — đúng thứ mà
     * freeze pane sinh ra để giữ.</p>
     */
    public byte[] xuat() {
        dongHienTai++;
        oChu(dongHienTai, 0, CHAN_TRANG, kieuPhu);

        for (int i = 0; i < Math.max(soCot, 1); i++) {
            sheet.autoSizeColumn(i);
            // autoSizeColumn không tính tới ô đã gộp và hay cho cột quá sát chữ
            sheet.setColumnWidth(i, Math.min(sheet.getColumnWidth(i) + 600, 15000));
        }
        if (dongHeader >= 0) {
            sheet.createFreezePane(0, dongHeader + 1);
        }
        if (soCot > 1) {
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, soCot - 1));
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không xuất được file Excel báo cáo", ex);
        } finally {
            dongWorkbook();
        }
    }

    private void dongWorkbook() {
        try {
            workbook.close();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không đóng được workbook", ex);
        }
    }

    // =================================================================

    private void oChu(int dong, int cot, String giaTri, CellStyle kieu) {
        Row row = sheet.getRow(dong) != null ? sheet.getRow(dong) : sheet.createRow(dong);
        Cell o = row.createCell(cot);
        o.setCellValue(giaTri);
        o.setCellStyle(kieu);
    }

    /**
     * Tên sheet hợp lệ với Excel: tối đa 31 ký tự và không chứa {@code : \ / ? * [ ]}.
     *
     * <p>Không cắt gọt thì một tiêu đề dài sẽ làm POI ném lỗi lúc xuất — nghĩa là lỗi rơi vào
     * mặt người dùng ở đúng thao tác cuối cùng.</p>
     */
    private static String tenSheetHopLe(String tieuDe) {
        String sach = tieuDe.replaceAll("[:\\\\/?*\\[\\]]", " ").trim();
        return sach.length() <= 31 ? sach : sach.substring(0, 31);
    }

    private CellStyle taoKieuTieuDe() {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        kieu.setFont(font);
        return kieu;
    }

    private CellStyle taoKieuHeader() {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        kieu.setFont(font);
        kieu.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        kieu.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        kieu.setAlignment(HorizontalAlignment.CENTER);
        kieu.setBorderBottom(BorderStyle.THIN);
        return kieu;
    }

    private CellStyle taoKieuTien(boolean dam) {
        CellStyle kieu = workbook.createCellStyle();
        kieu.setDataFormat(workbook.createDataFormat().getFormat(DINH_DANG_TIEN));
        if (dam) {
            Font font = workbook.createFont();
            font.setBold(true);
            kieu.setFont(font);
            kieu.setBorderTop(BorderStyle.THIN);
        }
        return kieu;
    }

    private CellStyle taoKieuChuDam() {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        kieu.setFont(font);
        kieu.setBorderTop(BorderStyle.THIN);
        return kieu;
    }

    private CellStyle taoKieuPhu() {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        kieu.setFont(font);
        return kieu;
    }
}
