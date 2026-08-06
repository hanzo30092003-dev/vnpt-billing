package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.BoLocHoaDon;
import com.hanzo.billing.dto.TongHopHoaDon;
import com.hanzo.billing.entity.ChiTietHoaDon;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ChiTietHoaDonRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.service.HoaDonService;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class HoaDonServiceImpl implements HoaDonService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] TIEU_DE_COT = {
            "STT", "Mã hóa đơn", "Kỳ cước", "Khách hàng", "Số thuê bao", "Ngày lập",
            "Hạn thanh toán", "Cước thuê bao", "Cước thoại", "Cước SMS", "Cước dữ liệu",
            "Giảm trừ", "Trước thuế", "VAT", "Tổng thanh toán", "Đã thu", "Còn nợ", "Trạng thái"
    };

    private final HoaDonRepository hoaDonRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional(readOnly = true)
    public Page<HoaDon> timKiem(BoLocHoaDon boLoc, Pageable pageable) {
        return hoaDonRepository.timKiem(boLoc.getKyCuocId(), boLoc.getTrangThai(),
                boLoc.khachHangChuan(), boLoc.soThueBaoChuan(),
                boLoc.getTuSoTien(), boLoc.getDenSoTien(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public TongHopHoaDon tongHop(BoLocHoaDon boLoc) {
        List<HoaDon> danhSach = timTatCa(boLoc);
        BigDecimal phatSinh = BigDecimal.ZERO;
        BigDecimal daThu = BigDecimal.ZERO;
        BigDecimal conNo = BigDecimal.ZERO;
        for (HoaDon h : danhSach) {
            phatSinh = phatSinh.add(h.getTongThanhToan());
            daThu = daThu.add(khongNull(h.getDaThanhToan()));
            conNo = conNo.add(h.getConNo());
        }
        return new TongHopHoaDon(danhSach.size(), phatSinh, daThu, conNo);
    }

    @Override
    @Transactional(readOnly = true)
    public HoaDon layTheoId(Long id) {
        return hoaDonRepository.timKemQuanHe(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy hóa đơn id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChiTietHoaDon> layKhoanMuc(Long hoaDonId) {
        return chiTietHoaDonRepository.findByHoaDonId(hoaDonId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThanhToan> layLichSuThanhToan(Long hoaDonId) {
        return thanhToanRepository.timTheoHoaDonKemNguoiThu(hoaDonId);
    }

    // =================================================================
    // CHUYỂN TRẠNG THÁI QUÁ HẠN
    // =================================================================

    /**
     * {@inheritDoc}
     *
     * <p><b>Không bao giờ ghi đè {@code DA_TT}.</b> Truy vấn đã loại hóa đơn đã thu đủ ở hai
     * lớp: {@code con_no > 0} và {@code trangThai <> DA_TT}. Hai lớp là cố ý — nếu về sau có
     * ai đó ghi {@code con_no} sai thì lớp thứ hai vẫn giữ được bất biến quan trọng hơn.</p>
     *
     * <p>Chỉ đọc {@code con_no} chứ không tính lại từ bảng thanh toán, theo ràng buộc ② của
     * Phase 5.</p>
     */
    @Override
    @Transactional
    public int capNhatQuaHan() {
        LocalDate homNay = LocalDate.now();
        List<HoaDon> canChuyen = hoaDonRepository.timCanChuyenQuaHan(homNay);
        if (canChuyen.isEmpty()) {
            return 0;
        }
        for (HoaDon h : canChuyen) {
            h.setTrangThai(TrangThaiHoaDon.QUA_HAN);
        }
        hoaDonRepository.saveAll(canChuyen);

        log.info("Đã chuyển {} hóa đơn sang trạng thái Quá hạn (mốc {})",
                canChuyen.size(), homNay.format(NGAY));
        nhatKyService.ghiNhatKy("CAP_NHAT_QUA_HAN", "HOA_DON", null,
                "Chuyển " + canChuyen.size() + " hóa đơn quá hạn thanh toán sang trạng thái "
                        + "Quá hạn, tính tới ngày " + homNay.format(NGAY));
        return canChuyen.size();
    }

    // =================================================================
    // XUẤT EXCEL
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] xuatExcel(BoLocHoaDon boLoc) {
        List<HoaDon> danhSach = timTatCa(boLoc);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Hóa đơn");
            CellStyle kieuTieuDe = taoKieuDam(workbook);

            int soDong = 0;
            Row dongTieuDe = sheet.createRow(soDong++);
            for (int i = 0; i < TIEU_DE_COT.length; i++) {
                Cell o = dongTieuDe.createCell(i);
                o.setCellValue(TIEU_DE_COT[i]);
                o.setCellStyle(kieuTieuDe);
            }

            int stt = 1;
            BigDecimal tongPhatSinh = BigDecimal.ZERO;
            BigDecimal tongDaThu = BigDecimal.ZERO;
            BigDecimal tongConNo = BigDecimal.ZERO;

            for (HoaDon h : danhSach) {
                Row dong = sheet.createRow(soDong++);
                int cot = 0;
                dong.createCell(cot++).setCellValue(stt++);
                dong.createCell(cot++).setCellValue(h.getMaHoaDon());
                dong.createCell(cot++).setCellValue(
                        h.getKyCuoc().getThang() + "/" + h.getKyCuoc().getNam());
                dong.createCell(cot++).setCellValue(h.getKhachHang().getTenKh());
                dong.createCell(cot++).setCellValue(h.getThueBao().getSoThueBao());
                dong.createCell(cot++).setCellValue(h.getNgayLap().format(NGAY));
                dong.createCell(cot++).setCellValue(h.getHanThanhToan().format(NGAY));
                dong.createCell(cot++).setCellValue(soTien(h.getCuocThueBao()));
                dong.createCell(cot++).setCellValue(soTien(h.getCuocThoai()));
                dong.createCell(cot++).setCellValue(soTien(h.getCuocSms()));
                dong.createCell(cot++).setCellValue(soTien(h.getCuocData()));
                dong.createCell(cot++).setCellValue(soTien(h.getGiamTru()));
                dong.createCell(cot++).setCellValue(soTien(h.getTongTruocThue()));
                dong.createCell(cot++).setCellValue(soTien(h.getThueVat()));
                dong.createCell(cot++).setCellValue(soTien(h.getTongThanhToan()));
                dong.createCell(cot++).setCellValue(soTien(h.getDaThanhToan()));
                dong.createCell(cot++).setCellValue(soTien(h.getConNo()));
                dong.createCell(cot).setCellValue(h.getTrangThai().getNhan());

                tongPhatSinh = tongPhatSinh.add(h.getTongThanhToan());
                tongDaThu = tongDaThu.add(khongNull(h.getDaThanhToan()));
                tongConNo = tongConNo.add(h.getConNo());
            }

            Row dongTong = sheet.createRow(soDong);
            Cell nhan = dongTong.createCell(0);
            nhan.setCellValue("TỔNG CỘNG (" + danhSach.size() + " hóa đơn)");
            nhan.setCellStyle(kieuTieuDe);
            ghiOTong(dongTong, 14, tongPhatSinh, kieuTieuDe);
            ghiOTong(dongTong, 15, tongDaThu, kieuTieuDe);
            ghiOTong(dongTong, 16, tongConNo, kieuTieuDe);

            for (int i = 0; i < TIEU_DE_COT.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không xuất được file Excel danh sách hóa đơn", ex);
        }
    }

    // =================================================================

    private List<HoaDon> timTatCa(BoLocHoaDon boLoc) {
        return hoaDonRepository.timTatCaCoLoc(boLoc.getKyCuocId(), boLoc.getTrangThai(),
                boLoc.khachHangChuan(), boLoc.soThueBaoChuan(),
                boLoc.getTuSoTien(), boLoc.getDenSoTien());
    }

    private static void ghiOTong(Row dong, int cot, BigDecimal giaTri, CellStyle kieu) {
        Cell o = dong.createCell(cot);
        o.setCellValue(soTien(giaTri));
        o.setCellStyle(kieu);
    }

    private static double soTien(BigDecimal giaTri) {
        return khongNull(giaTri).doubleValue();
    }

    private static BigDecimal khongNull(BigDecimal giaTri) {
        return giaTri == null ? BigDecimal.ZERO : giaTri;
    }

    private static CellStyle taoKieuDam(Workbook workbook) {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        kieu.setFont(font);
        return kieu;
    }
}
