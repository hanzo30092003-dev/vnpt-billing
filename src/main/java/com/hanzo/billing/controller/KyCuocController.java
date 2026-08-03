package com.hanzo.billing.controller;

import com.hanzo.billing.dto.KyCuocForm;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.KyCuocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/ky-cuoc")
@RequiredArgsConstructor
public class KyCuocController {

    private final KyCuocService kyCuocService;

    @GetMapping
    public String danhSach(Model model) {
        napDuLieu(model, null);
        return "ky-cuoc/danh-sach";
    }

    @PostMapping("/tao-moi")
    public String taoMoi(@Valid @ModelAttribute("kyCuocForm") KyCuocForm form,
                         BindingResult ketQua, Model model, RedirectAttributes ra) {
        if (ketQua.hasErrors()) {
            napDuLieu(model, form);
            return "ky-cuoc/danh-sach";
        }
        try {
            KyCuoc daTao = kyCuocService.taoMoi(form);
            ra.addFlashAttribute("thongBaoThanhCong",
                    "Đã tạo kỳ cước tháng " + daTao.getThang() + "/" + daTao.getNam());
            return "redirect:/ky-cuoc";
        } catch (NghiepVuException ex) {
            // Bắt tại chỗ để giữ lại tháng/năm người dùng vừa chọn trên form
            ketQua.reject("nghiepVu", ex.getMessage());
            napDuLieu(model, form);
            return "ky-cuoc/danh-sach";
        }
    }

    private void napDuLieu(Model model, KyCuocForm form) {
        if (form == null) {
            form = new KyCuocForm();
            LocalDate homNay = LocalDate.now();
            form.setThang(homNay.getMonthValue());
            form.setNam(homNay.getYear());
        }
        model.addAttribute("kyCuocForm", form);
        model.addAttribute("danhSach", kyCuocService.layTatCa());
    }
}
