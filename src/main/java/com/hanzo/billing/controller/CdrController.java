package com.hanzo.billing.controller;

import com.hanzo.billing.dto.KetQuaSinhCdr;
import com.hanzo.billing.dto.SinhCdrForm;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.ThueBaoService;
import com.hanzo.billing.service.rating.CdrGeneratorService;
import com.hanzo.billing.service.rating.CdrImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequestMapping("/cdr")
@RequiredArgsConstructor
public class CdrController {

    private final CdrGeneratorService cdrGeneratorService;
    private final CdrImportService cdrImportService;
    private final ThueBaoService thueBaoService;

    @GetMapping("/sinh-du-lieu")
    public String moFormSinh(Model model) {
        model.addAttribute("sinhCdrForm", new SinhCdrForm());
        napThueBao(model);
        return "cdr/sinh-du-lieu";
    }

    @PostMapping("/sinh-du-lieu")
    public String sinh(@Valid @ModelAttribute("sinhCdrForm") SinhCdrForm form,
                       BindingResult ketQuaRangBuoc, Model model) {
        if (ketQuaRangBuoc.hasErrors()) {
            napThueBao(model);
            return "cdr/sinh-du-lieu";
        }
        try {
            KetQuaSinhCdr ketQua = cdrGeneratorService.sinh(form);
            model.addAttribute("ketQua", ketQua);
        } catch (NghiepVuException ex) {
            ketQuaRangBuoc.reject("nghiepVu", ex.getMessage());
        }
        napThueBao(model);
        return "cdr/sinh-du-lieu";
    }

    // =================================================================
    // NHẬP CDR TỪ FILE CSV
    // =================================================================

    @GetMapping("/import")
    public String moFormImport() {
        return "cdr/import";
    }

    @PostMapping("/import")
    public String nhapFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            model.addAttribute("ketQua", cdrImportService.nhap(file));
        } catch (NghiepVuException ex) {
            model.addAttribute("thongBaoLoi", ex.getMessage());
        }
        return "cdr/import";
    }

    /**
     * Tải file CSV mẫu.
     *
     * <p>Đọc từ classpath thay vì từ thư mục {@code docs/} để vẫn chạy được khi ứng dụng
     * được đóng gói thành file JAR chạy độc lập.</p>
     */
    @GetMapping("/tai-file-mau")
    public ResponseEntity<Resource> taiFileMau() {
        Resource file = new ClassPathResource("mau-cdr.csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-cdr.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(file);
    }

    /** Danh sách thuê bao để chọn phạm vi sinh dữ liệu. */
    private void napThueBao(Model model) {
        model.addAttribute("danhSachThueBao",
                thueBaoService.timKiem(null, TrangThaiThueBao.HOAT_DONG, null, null,
                        PageRequest.of(0, 200)).getContent());
    }
}
