package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.BoLocThanhToan;
import com.hanzo.billing.dto.ThanhToanForm;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.NguoiDungRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.service.NhatKyService;
import com.hanzo.billing.service.SinhMaService;
import com.hanzo.billing.service.ThanhToanService;
import com.hanzo.billing.util.SecurityUtils;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Ghi nhận thanh toán hóa đơn.
 *
 * <h2>Ràng buộc ② của Phase 5 — một nguồn sự thật cho "còn nợ"</h2>
 * <p>Lớp này là <b>nơi duy nhất</b> ghi {@code hoa_don.da_thanh_toan}, {@code con_no} và
 * {@code trang_thai}. Mọi màn hình chỉ đọc ba cột đó, tuyệt đối không tự cộng lại từ bảng
 * {@code thanh_toan}. Hai cách tính song song là hai nguồn sự thật, và chúng sẽ có lúc mâu
 * thuẫn nhau đúng vào lúc khó truy nhất — khi có thanh toán một phần hoặc giảm trừ.</p>
 *
 * <p>Chuỗi cập nhật luôn đi đủ và đúng thứ tự:</p>
 * <pre>da_thanh_toan += soTien  →  con_no = tong_thanh_toan − da_thanh_toan  →  trang_thai</pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThanhToanServiceImpl implements ThanhToanService {

    private static final DateTimeFormatter NGAY_GIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final String[] TIEU_DE_COT = {
            "STT", "Mã giao dịch", "Thời điểm", "Mã hóa đơn", "Khách hàng", "Số thuê bao",
            "Số tiền", "Hình thức", "Người thu", "Ghi chú"
    };

    private final ThanhToanRepository thanhToanRepository;
    private final HoaDonRepository hoaDonRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final SinhMaService sinhMaService;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional
    public ThanhToan ghiNhan(ThanhToanForm form) {
        HoaDon hoaDon = hoaDonRepository.findById(form.getHoaDonId())
                .orElseThrow(() -> new NghiepVuException(
                        "Không tìm thấy hóa đơn id " + form.getHoaDonId()));

        kiemTra(form, hoaDon);

        ThanhToan giaoDich = new ThanhToan();
        giaoDich.setMaGiaoDich(sinhMaService.sinhMaThanhToan(form.getNgayThanhToan()));
        giaoDich.setHoaDon(hoaDon);
        giaoDich.setSoTien(form.getSoTien());
        giaoDich.setHinhThuc(form.getHinhThuc());
        // Giữ nguyên giờ hiện tại trên ngày người dùng chọn: sổ quỹ ghi theo ngày, nhưng
        // thứ tự trong cùng một ngày vẫn cần phân biệt được.
        giaoDich.setNgayThanhToan(thoiDiemGhiNhan(form.getNgayThanhToan()));
        giaoDich.setGhiChu(form.getGhiChu());
        SecurityUtils.layNguoiDungHienTai().ifPresent(p ->
                giaoDich.setNguoiThu(nguoiDungRepository.getReferenceById(p.getId())));
        thanhToanRepository.save(giaoDich);

        capNhatHoaDon(hoaDon, form.getSoTien());

        log.info("Ghi nhận thanh toán {} cho hóa đơn {}: {} đ, còn nợ {} đ, trạng thái {}",
                giaoDich.getMaGiaoDich(), hoaDon.getMaHoaDon(), form.getSoTien(),
                hoaDon.getConNo(), hoaDon.getTrangThai());
        nhatKyService.ghiNhatKy("GHI_NHAN_THANH_TOAN", "HOA_DON", hoaDon.getId(),
                "Thu " + form.getSoTien() + " đ cho hóa đơn " + hoaDon.getMaHoaDon()
                        + " (" + giaoDich.getMaGiaoDich() + "). Còn nợ " + hoaDon.getConNo()
                        + " đ, trạng thái " + hoaDon.getTrangThai().getNhan());
        return giaoDich;
    }

    /**
     * Ba luật phụ thuộc trạng thái hóa đơn, nên phải kiểm ở đây chứ không ở form.
     *
     * <p>Form chỉ biết dữ liệu người dùng gõ; nó không biết hóa đơn đã được ai đó thu tiền
     * xong trong lúc form đang mở hay chưa.</p>
     */
    private void kiemTra(ThanhToanForm form, HoaDon hoaDon) {
        if (hoaDon.getTrangThai() == TrangThaiHoaDon.DA_TT) {
            throw new NghiepVuException("Hóa đơn " + hoaDon.getMaHoaDon()
                    + " đã thanh toán đủ, không ghi nhận thêm được.");
        }
        if (form.getNgayThanhToan().isAfter(LocalDate.now())) {
            throw new NghiepVuException("Ngày thanh toán không được ở tương lai.");
        }
        if (form.getSoTien().compareTo(hoaDon.getConNo()) > 0) {
            throw new NghiepVuException("Số tiền thanh toán (" + form.getSoTien()
                    + " đ) vượt quá số còn nợ của hóa đơn " + hoaDon.getMaHoaDon()
                    + " (" + hoaDon.getConNo() + " đ).");
        }
    }

    /**
     * Cập nhật <b>trọn chuỗi</b> trên hóa đơn. Cấm sửa lẻ một cột.
     *
     * <p>{@code con_no} được tính lại từ {@code tong_thanh_toan − da_thanh_toan} chứ không
     * trừ dần từ giá trị cũ: trừ dần thì mỗi lần chạy tích thêm một cơ hội lệch, còn tính
     * lại từ hai cột gốc thì kết quả luôn đúng bất kể trước đó có gì sai.</p>
     */
    private void capNhatHoaDon(HoaDon hoaDon, BigDecimal soTien) {
        BigDecimal daThanhToan = khongNull(hoaDon.getDaThanhToan()).add(soTien);
        BigDecimal conNo = hoaDon.getTongThanhToan().subtract(daThanhToan);

        hoaDon.setDaThanhToan(daThanhToan);
        hoaDon.setConNo(conNo);
        hoaDon.setTrangThai(suyRaTrangThai(hoaDon, conNo));
        hoaDonRepository.save(hoaDon);
    }

    /**
     * Trạng thái suy ra từ số còn nợ, không phải từ số vừa thu.
     *
     * <p>Hóa đơn đang {@code QUA_HAN} mà trả một phần thì về {@code TT_MOT_PHAN} — quá hạn
     * là một tình trạng của <i>thời gian</i>, còn ba giá trị kia là tình trạng của
     * <i>tiền</i>. Lịch chạy nền sẽ đưa nó lại về {@code QUA_HAN} nếu vẫn còn nợ sau hạn,
     * nên không mất thông tin.</p>
     */
    private static TrangThaiHoaDon suyRaTrangThai(HoaDon hoaDon, BigDecimal conNo) {
        if (conNo.signum() == 0) {
            return TrangThaiHoaDon.DA_TT;
        }
        if (conNo.compareTo(hoaDon.getTongThanhToan()) < 0) {
            return TrangThaiHoaDon.TT_MOT_PHAN;
        }
        return hoaDon.getTrangThai();
    }

    private static LocalDateTime thoiDiemGhiNhan(LocalDate ngay) {
        LocalDate homNay = LocalDate.now();
        return ngay.isEqual(homNay) ? LocalDateTime.now() : ngay.atTime(9, 0);
    }

    // =================================================================

    @Override
    @Transactional(readOnly = true)
    public ThanhToan layTheoId(Long id) {
        return thanhToanRepository.findById(id)
                .orElseThrow(() -> new NghiepVuException(
                        "Không tìm thấy giao dịch thanh toán id " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ThanhToan> timKiem(BoLocThanhToan boLoc, Pageable pageable) {
        return thanhToanRepository.timKiem(boLoc.tuLuc(), boLoc.denLuc(),
                boLoc.getHinhThuc(), boLoc.getNguoiThuId(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal tongThu(BoLocThanhToan boLoc) {
        BigDecimal tong = BigDecimal.ZERO;
        for (ThanhToan t : timTatCa(boLoc)) {
            tong = tong.add(t.getSoTien());
        }
        return tong;
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] xuatExcel(BoLocThanhToan boLoc) {
        List<ThanhToan> danhSach = timTatCa(boLoc);

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Giao dịch thanh toán");
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
            for (ThanhToan t : danhSach) {
                Row dong = sheet.createRow(soDong++);
                int cot = 0;
                dong.createCell(cot++).setCellValue(stt++);
                dong.createCell(cot++).setCellValue(t.getMaGiaoDich());
                dong.createCell(cot++).setCellValue(t.getNgayThanhToan().format(NGAY_GIO));
                dong.createCell(cot++).setCellValue(t.getHoaDon().getMaHoaDon());
                dong.createCell(cot++).setCellValue(t.getHoaDon().getKhachHang().getTenKh());
                dong.createCell(cot++).setCellValue(t.getHoaDon().getThueBao().getSoThueBao());
                dong.createCell(cot++).setCellValue(t.getSoTien().doubleValue());
                dong.createCell(cot++).setCellValue(t.getHinhThuc().getNhan());
                dong.createCell(cot++).setCellValue(
                        t.getNguoiThu() == null ? "" : t.getNguoiThu().getHoTen());
                dong.createCell(cot).setCellValue(t.getGhiChu() == null ? "" : t.getGhiChu());
                tong = tong.add(t.getSoTien());
            }

            Row dongTong = sheet.createRow(soDong);
            Cell nhan = dongTong.createCell(0);
            nhan.setCellValue("TỔNG THU (" + danhSach.size() + " giao dịch)");
            nhan.setCellStyle(kieuDam);
            Cell oTong = dongTong.createCell(6);
            oTong.setCellValue(tong.doubleValue());
            oTong.setCellStyle(kieuDam);

            for (int i = 0; i < TIEU_DE_COT.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không xuất được file Excel danh sách thanh toán", ex);
        }
    }

    private List<ThanhToan> timTatCa(BoLocThanhToan boLoc) {
        return thanhToanRepository.timTatCaCoLoc(boLoc.tuLuc(), boLoc.denLuc(),
                boLoc.getHinhThuc(), boLoc.getNguoiThuId());
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
