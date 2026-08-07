package com.hanzo.billing.controller;

import com.hanzo.billing.dto.BoLocThanhToan;
import com.hanzo.billing.dto.ThanhToanForm;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.enums.HinhThucThanhToan;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.HoaDonService;
import com.hanzo.billing.service.ThanhToanService;
import com.hanzo.billing.service.pdf.PhieuThuPdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Controller
@RequestMapping("/thanh-toan")
@RequiredArgsConstructor
public class ThanhToanController {

    private static final int SO_DONG_MOI_TRANG = 20;

    private final ThanhToanService thanhToanService;
    private final HoaDonService hoaDonService;
    private final PhieuThuPdfService phieuThuPdfService;

    // =================================================================
    // GHI NHẬN THANH TOÁN
    // =================================================================

    @GetMapping("/moi/{hoaDonId}")
    public String moForm(@PathVariable Long hoaDonId, Model model) {
        HoaDon hoaDon = hoaDonService.layTheoId(hoaDonId);

        ThanhToanForm form = new ThanhToanForm();
        form.setHoaDonId(hoaDonId);
        // Mặc định thu trọn số còn nợ — thao tác thường gặp nhất, đỡ phải gõ lại
        form.setSoTien(hoaDon.getConNo());
        form.setNgayThanhToan(LocalDate.now());

        napModelForm(model, hoaDon, form);
        return "thanh-toan/form";
    }

    @PostMapping
    public String ghiNhan(@Valid @ModelAttribute("form") ThanhToanForm form,
                          BindingResult loi, Model model, RedirectAttributes ra) {
        HoaDon hoaDon = hoaDonService.layTheoId(form.getHoaDonId());

        if (loi.hasErrors()) {
            napModelForm(model, hoaDon, form);
            return "thanh-toan/form";
        }
        try {
            ThanhToan giaoDich = thanhToanService.ghiNhan(form);
            ra.addFlashAttribute("thongBao", "Đã ghi nhận thanh toán "
                    + giaoDich.getMaGiaoDich() + " — " + giaoDich.getSoTien() + " đ.");
            return "redirect:/hoa-don/" + form.getHoaDonId();
        } catch (NghiepVuException ex) {
            // Ba luật của tầng nghiệp vụ trả về đây để người dùng thấy thông báo tiếng Việt
            // ngay trên form, không phải trang lỗi 500
            model.addAttribute("thongBaoLoi", ex.getMessage());
            napModelForm(model, hoaDon, form);
            return "thanh-toan/form";
        }
    }

    private void napModelForm(Model model, HoaDon hoaDon, ThanhToanForm form) {
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("form", form);
        model.addAttribute("danhSachHinhThuc", HinhThucThanhToan.values());
        model.addAttribute("homNay", LocalDate.now());
    }

    // =================================================================
    // DANH SÁCH GIAO DỊCH
    // =================================================================

    @GetMapping
    public String danhSach(@ModelAttribute("boLoc") BoLocThanhToan boLoc,
                           @RequestParam(defaultValue = "0") int trang,
                           Model model) {
        Page<ThanhToan> ketQua = thanhToanService.timKiem(
                boLoc, PageRequest.of(trang, SO_DONG_MOI_TRANG));

        model.addAttribute("ketQua", ketQua);
        model.addAttribute("tongThu", thanhToanService.tongThu(boLoc));
        model.addAttribute("danhSachHinhThuc", HinhThucThanhToan.values());
        model.addAttribute("chuoiLoc", boLoc.chuoiQuery());
        return "thanh-toan/danh-sach";
    }

    @GetMapping("/{id}/phieu-thu")
    public ResponseEntity<byte[]> phieuThu(@PathVariable Long id) {
        ThanhToan giaoDich = thanhToanService.layTheoId(id);
        byte[] noiDung = phieuThuPdfService.xuat(giaoDich);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''"
                        + maHoaUrl(PhieuThuPdfService.tenFile(giaoDich)))
                .contentType(MediaType.APPLICATION_PDF)
                .body(noiDung);
    }

    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> xuatExcel(@ModelAttribute("boLoc") BoLocThanhToan boLoc) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''"
                        + maHoaUrl("DanhSachThanhToan.xlsx"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(thanhToanService.xuatExcel(boLoc));
    }

    private static String maHoaUrl(String tenFile) {
        return URLEncoder.encode(tenFile, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
