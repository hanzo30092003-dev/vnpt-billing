package com.hanzo.billing.controller;

import com.hanzo.billing.dto.KetQuaSinhCdr;
import com.hanzo.billing.dto.SinhCdrForm;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.ThueBaoService;
import com.hanzo.billing.service.rating.CdrGeneratorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/cdr")
@RequiredArgsConstructor
public class CdrController {

    private final CdrGeneratorService cdrGeneratorService;
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

    /** Danh sách thuê bao để chọn phạm vi sinh dữ liệu. */
    private void napThueBao(Model model) {
        model.addAttribute("danhSachThueBao",
                thueBaoService.timKiem(null, TrangThaiThueBao.HOAT_DONG, null, null,
                        PageRequest.of(0, 200)).getContent());
    }
}
