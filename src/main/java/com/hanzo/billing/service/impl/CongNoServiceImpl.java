package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.DongCongNo;
import com.hanzo.billing.dto.OTuoiNo;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.NhomTuoiNo;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.service.CongNoService;
import com.hanzo.billing.service.ThueBaoService;
import com.hanzo.billing.service.rating.ThamSoTinhCuoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CongNoServiceImpl implements CongNoService {

    private static final DateTimeFormatter NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[] TIEU_DE_COT = {
            "STT", "Mã hóa đơn", "Kỳ cước", "Khách hàng", "Số thuê bao", "Hạn thanh toán",
            "Số ngày quá hạn", "Nhóm tuổi nợ", "Tổng thanh toán", "Đã thu", "Còn nợ", "Trạng thái"
    };

    private final HoaDonRepository hoaDonRepository;
    private final ThueBaoService thueBaoService;

    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public List<DongCongNo> danhSachConNo(String khachHang, NhomTuoiNo nhomTuoiNo) {
        List<DongCongNo> ketQua = dungDanhSach(khachHang);
        if (nhomTuoiNo != null) {
            ketQua = ketQua.stream().filter(d -> d.nhomTuoiNo() == nhomTuoiNo).toList();
        }
        return ketQua;
    }

    /**
     * Dựng danh sách từ hóa đơn còn nợ, tính số ngày quá hạn <b>một lần</b> ở đây.
     *
     * <p>Sắp theo số ngày quá hạn giảm dần, khoản nợ lâu nhất lên đầu — đó là thứ tự người
     * làm công nợ cần nhìn trước.</p>
     */
    private List<DongCongNo> dungDanhSach(String khachHang) {
        LocalDate homNay = LocalDate.now();
        String loc = (khachHang == null || khachHang.isBlank()) ? null : khachHang.trim();

        List<DongCongNo> ketQua = new ArrayList<>();
        for (HoaDon h : hoaDonRepository.timConNo(loc)) {
            long soNgay = soNgayQuaHan(h, homNay);
            ketQua.add(new DongCongNo(h, soNgay, NhomTuoiNo.cua(soNgay)));
        }
        ketQua.sort(Comparator.comparingLong(DongCongNo::soNgayQuaHan).reversed()
                .thenComparing(d -> d.hoaDon().getMaHoaDon()));
        return ketQua;
    }

    /**
     * Số ngày đã quá hạn. Giá trị ≤ 0 nghĩa là còn trong hạn.
     *
     * <p>Đếm từ <b>ngày sau</b> hạn thanh toán: hóa đơn đến hạn đúng hôm nay thì chưa quá
     * hạn ngày nào, nên phải ra 0 chứ không phải 1.</p>
     */
    static long soNgayQuaHan(HoaDon hoaDon, LocalDate homNay) {
        return java.time.temporal.ChronoUnit.DAYS.between(hoaDon.getHanThanhToan(), homNay);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OTuoiNo> bangTuoiNo(String khachHang) {
        Map<NhomTuoiNo, OTuoiNo> theoNhom = new EnumMap<>(NhomTuoiNo.class);
        for (NhomTuoiNo nhom : NhomTuoiNo.values()) {
            theoNhom.put(nhom, OTuoiNo.rong(nhom));
        }
        for (DongCongNo dong : dungDanhSach(khachHang)) {
            theoNhom.computeIfPresent(dong.nhomTuoiNo(), (k, o) -> o.cong(dong.conNo()));
        }
        // Giữ đúng thứ tự khai báo trong enum để bảng và biểu đồ luôn cùng trục
        return List.copyOf(theoNhom.values());
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal tongConNo(String khachHang) {
        BigDecimal tong = BigDecimal.ZERO;
        for (DongCongNo dong : dungDanhSach(khachHang)) {
            tong = tong.add(dong.conNo());
        }
        return tong;
    }

    // =================================================================
    // ĐỀ XUẤT TẠM NGỪNG
    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public List<DongCongNo> deXuatTamNgung() {
        return dungDanhSach(null).stream()
                .filter(d -> d.soNgayQuaHan() > ThamSoTinhCuoc.SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG)
                // Chỉ đề xuất thuê bao đang hoạt động: thuê bao đã tạm ngừng hoặc thanh lý
                // thì thao tác này vô nghĩa, và ma trận chuyển trạng thái sẽ từ chối.
                .filter(d -> d.hoaDon().getThueBao().getTrangThai() == TrangThaiThueBao.HOAT_DONG)
                .toList();
    }

    @Override
    @Transactional
    public void tamNgungViNoCuoc(Long hoaDonId) {
        HoaDon hoaDon = hoaDonRepository.timKemQuanHe(hoaDonId)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy hóa đơn id " + hoaDonId));

        if (hoaDon.getConNo().signum() <= 0) {
            throw new NghiepVuException("Hóa đơn " + hoaDon.getMaHoaDon()
                    + " đã thanh toán đủ, không có căn cứ tạm ngừng thuê bao.");
        }
        long soNgay = soNgayQuaHan(hoaDon, LocalDate.now());
        if (soNgay <= ThamSoTinhCuoc.SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG) {
            throw new NghiepVuException("Hóa đơn " + hoaDon.getMaHoaDon() + " mới quá hạn "
                    + soNgay + " ngày, chưa vượt ngưỡng "
                    + ThamSoTinhCuoc.SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG + " ngày để tạm ngừng.");
        }

        // Dùng lại nghiệp vụ của Phase 2: ma trận chuyển trạng thái và việc ghi
        // lich_su_thue_bao vẫn đi qua đúng một đường, không viết logic mới ở đây.
        thueBaoService.chuyenTrangThai(hoaDon.getThueBao().getId(), TrangThaiThueBao.TAM_NGUNG_1C,
                "Tạm ngừng do nợ cước quá hạn — hóa đơn " + hoaDon.getMaHoaDon());

        log.info("Đã tạm ngừng thuê bao {} do nợ hóa đơn {} quá hạn {} ngày",
                hoaDon.getThueBao().getSoThueBao(), hoaDon.getMaHoaDon(), soNgay);
    }

    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public byte[] xuatExcel(String khachHang, NhomTuoiNo nhomTuoiNo) {
        List<DongCongNo> danhSach = danhSachConNo(khachHang, nhomTuoiNo);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Công nợ");
            CellStyle kieuDam = taoKieuDam(workbook);

            int soDong = 0;
            Row dongTieuDe = sheet.createRow(soDong++);
            for (int i = 0; i < TIEU_DE_COT.length; i++) {
                Cell o = dongTieuDe.createCell(i);
                o.setCellValue(TIEU_DE_COT[i]);
                o.setCellStyle(kieuDam);
            }

            int stt = 1;
            BigDecimal tong = BigDecimal.ZERO;
            for (DongCongNo d : danhSach) {
                HoaDon h = d.hoaDon();
                Row dong = sheet.createRow(soDong++);
                int cot = 0;
                dong.createCell(cot++).setCellValue(stt++);
                dong.createCell(cot++).setCellValue(h.getMaHoaDon());
                dong.createCell(cot++).setCellValue(
                        h.getKyCuoc().getThang() + "/" + h.getKyCuoc().getNam());
                dong.createCell(cot++).setCellValue(h.getKhachHang().getTenKh());
                dong.createCell(cot++).setCellValue(h.getThueBao().getSoThueBao());
                dong.createCell(cot++).setCellValue(h.getHanThanhToan().format(NGAY));
                dong.createCell(cot++).setCellValue(Math.max(0, d.soNgayQuaHan()));
                dong.createCell(cot++).setCellValue(d.nhomTuoiNo().getNhan());
                dong.createCell(cot++).setCellValue(h.getTongThanhToan().doubleValue());
                dong.createCell(cot++).setCellValue(
                        h.getDaThanhToan() == null ? 0 : h.getDaThanhToan().doubleValue());
                dong.createCell(cot++).setCellValue(h.getConNo().doubleValue());
                dong.createCell(cot).setCellValue(h.getTrangThai().getNhan());
                tong = tong.add(h.getConNo());
            }

            Row dongTong = sheet.createRow(soDong);
            Cell nhan = dongTong.createCell(0);
            nhan.setCellValue("TỔNG CÒN NỢ (" + danhSach.size() + " hóa đơn)");
            nhan.setCellStyle(kieuDam);
            Cell oTong = dongTong.createCell(10);
            oTong.setCellValue(tong.doubleValue());
            oTong.setCellStyle(kieuDam);

            for (int i = 0; i < TIEU_DE_COT.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không xuất được file Excel công nợ", ex);
        }
    }

    private static CellStyle taoKieuDam(Workbook workbook) {
        CellStyle kieu = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        kieu.setFont(font);
        return kieu;
    }
}
