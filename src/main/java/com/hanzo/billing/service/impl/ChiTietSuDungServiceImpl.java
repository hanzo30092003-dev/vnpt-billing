package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.BoLocCdr;
import com.hanzo.billing.dto.TongHopCdr;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.service.ChiTietSuDungService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChiTietSuDungServiceImpl implements ChiTietSuDungService {

    private static final DateTimeFormatter DINH_DANG_THOI_GIAN =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private static final String[] TIEU_DE_COT = {
            "STT", "Số thuê bao", "Số bị gọi", "Loại dịch vụ", "Hướng",
            "Thời gian bắt đầu", "Thời lượng (giây)", "Số lượng", "Đơn vị",
            "Giờ cao điểm", "Cước phí", "Trạng thái tính cước", "Nguồn"
    };

    private final ChiTietSuDungRepository chiTietSuDungRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ChiTietSuDung> timCoLoc(BoLocCdr boLoc, Pageable pageable) {
        return chiTietSuDungRepository.timCoLoc(
                boLoc.soThueBaoChuan(), boLoc.tuLuc(), boLoc.denLuc(),
                boLoc.getLoaiDichVu(), boLoc.getHuong(),
                boLoc.getTrangThaiTinhCuoc(), boLoc.getNguon(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public TongHopCdr tinhTong(BoLocCdr boLoc) {
        List<Object[]> ketQua = chiTietSuDungRepository.tinhTong(
                boLoc.soThueBaoChuan(), boLoc.tuLuc(), boLoc.denLuc(),
                boLoc.getLoaiDichVu(), boLoc.getHuong(),
                boLoc.getTrangThaiTinhCuoc(), boLoc.getNguon());

        if (ketQua.isEmpty()) {
            return new TongHopCdr(0, 0, 0);
        }
        Object[] dong = ketQua.get(0);
        return new TongHopCdr(
                ((Number) dong[0]).longValue(),
                ((Number) dong[1]).longValue(),
                ((Number) dong[2]).longValue());
    }

    // =================================================================
    // XUẤT EXCEL
    // =================================================================

    /**
     * Xuất toàn bộ kết quả lọc ra file .xlsx.
     *
     * <p>Dùng {@link SXSSFWorkbook} thay vì {@code XSSFWorkbook}: SXSSF chỉ giữ một
     * cửa sổ nhỏ các dòng gần nhất trong bộ nhớ và ghi dần phần còn lại ra đĩa tạm.
     * Với vài nghìn bản ghi CDR, XSSFWorkbook sẽ dựng toàn bộ cây DOM trong RAM.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] xuatExcel(BoLocCdr boLoc) {
        List<ChiTietSuDung> danhSach = chiTietSuDungRepository.timTatCaCoLoc(
                boLoc.soThueBaoChuan(), boLoc.tuLuc(), boLoc.denLuc(),
                boLoc.getLoaiDichVu(), boLoc.getHuong(),
                boLoc.getTrangThaiTinhCuoc(), boLoc.getNguon());

        // Giữ 100 dòng trong RAM, phần còn lại ghi ra file tạm
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Chi tiết sử dụng");

            CellStyle kieuTieuDe = taoKieuTieuDe(workbook);
            CellStyle kieuTong = taoKieuTong(workbook);

            int soDong = 0;

            // Dòng tiêu đề bảng
            Row dongTieuDe = sheet.createRow(soDong++);
            for (int i = 0; i < TIEU_DE_COT.length; i++) {
                Cell o = dongTieuDe.createCell(i);
                o.setCellValue(TIEU_DE_COT[i]);
                o.setCellStyle(kieuTieuDe);
            }

            long tongThoiLuong = 0;
            long tongKb = 0;
            int stt = 1;

            for (ChiTietSuDung c : danhSach) {
                Row dong = sheet.createRow(soDong++);
                int cot = 0;
                dong.createCell(cot++).setCellValue(stt++);
                dong.createCell(cot++).setCellValue(c.getSoThueBao());
                dong.createCell(cot++).setCellValue(c.getSoBiGoi() == null ? "" : c.getSoBiGoi());
                dong.createCell(cot++).setCellValue(c.getLoaiDichVu().name());
                dong.createCell(cot++).setCellValue(c.getHuong().name());
                dong.createCell(cot++).setCellValue(c.getThoiGianBatDau().format(DINH_DANG_THOI_GIAN));

                int thoiLuong = c.getThoiLuongGiay() == null ? 0 : c.getThoiLuongGiay();
                int soLuong = c.getSoLuong() == null ? 0 : c.getSoLuong();
                dong.createCell(cot++).setCellValue(c.getLoaiDichVu() == LoaiDichVu.THOAI ? thoiLuong : 0);

                // Cột số lượng ghi giá trị đã quy đổi cho dễ đọc, cột đơn vị nói rõ là gì
                if (c.getLoaiDichVu() == LoaiDichVu.DATA) {
                    dong.createCell(cot++).setCellValue(
                            BigDecimal.valueOf(soLuong)
                                    .divide(BigDecimal.valueOf(TongHopCdr.KB_MOI_MB), 2, RoundingMode.HALF_UP)
                                    .doubleValue());
                    dong.createCell(cot++).setCellValue("MB");
                } else if (c.getLoaiDichVu() == LoaiDichVu.SMS) {
                    dong.createCell(cot++).setCellValue(soLuong);
                    dong.createCell(cot++).setCellValue("tin");
                } else {
                    dong.createCell(cot++).setCellValue("");
                    dong.createCell(cot++).setCellValue("");
                }

                dong.createCell(cot++).setCellValue(Boolean.TRUE.equals(c.getGioCaoDiem()) ? "Có" : "Không");
                // Phase 3 chưa tính cước nên cột này luôn trống
                dong.createCell(cot++).setCellValue(c.getCuocPhi() == null ? "" : c.getCuocPhi().toPlainString());
                dong.createCell(cot++).setCellValue(c.getTrangThaiTinhCuoc().name());
                dong.createCell(cot).setCellValue(c.getNguon() == null ? "" : c.getNguon().name());

                tongThoiLuong += (c.getLoaiDichVu() == LoaiDichVu.THOAI ? thoiLuong : 0);
                tongKb += (c.getLoaiDichVu() == LoaiDichVu.DATA ? soLuong : 0);
            }

            // Dòng tổng
            soDong++;
            Row dongTong = sheet.createRow(soDong);
            Cell oNhan = dongTong.createCell(0);
            oNhan.setCellValue("TỔNG CỘNG");
            oNhan.setCellStyle(kieuTong);
            sheet.addMergedRegion(new CellRangeAddress(soDong, soDong, 0, 4));

            Cell oSo = dongTong.createCell(5);
            oSo.setCellValue(danhSach.size() + " bản ghi");
            oSo.setCellStyle(kieuTong);

            Cell oThoiLuong = dongTong.createCell(6);
            oThoiLuong.setCellValue(tongThoiLuong);
            oThoiLuong.setCellStyle(kieuTong);

            Cell oDungLuong = dongTong.createCell(7);
            oDungLuong.setCellValue(BigDecimal.valueOf(tongKb)
                    .divide(BigDecimal.valueOf(TongHopCdr.KB_MOI_MB), 2, RoundingMode.HALF_UP).doubleValue());
            oDungLuong.setCellStyle(kieuTong);
            Cell oDonVi = dongTong.createCell(8);
            oDonVi.setCellValue("MB");
            oDonVi.setCellStyle(kieuTong);

            // SXSSF không hỗ trợ autoSizeColumn trên dòng đã ghi ra đĩa, đặt bề rộng cố định
            int[] beRong = {1500, 4000, 4500, 3000, 3500, 5500, 3800, 3000, 2000, 3000, 3000, 5000, 3500};
            for (int i = 0; i < beRong.length; i++) {
                sheet.setColumnWidth(i, beRong[i]);
            }

            workbook.write(out);
            workbook.dispose(); // xoá file tạm SXSSF
            return out.toByteArray();

        } catch (IOException e) {
            throw new NghiepVuException("Không tạo được file Excel. Hãy thử lại; nếu vẫn lỗi thì báo quản trị viên "
                    + "kèm nội dung sau: " + e.getMessage());
        }
    }

    private CellStyle taoKieuTieuDe(Workbook workbook) {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        kieu.setFont(font);
        kieu.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        kieu.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        kieu.setBorderBottom(BorderStyle.THIN);
        return kieu;
    }

    private CellStyle taoKieuTong(Workbook workbook) {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        kieu.setFont(font);
        kieu.setBorderTop(BorderStyle.DOUBLE);
        return kieu;
    }
}
