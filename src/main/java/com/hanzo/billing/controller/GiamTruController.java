package com.hanzo.billing.controller;

import com.hanzo.billing.dto.GiamTruForm;
import com.hanzo.billing.entity.GiamTru;
import com.hanzo.billing.enums.LoaiGiamTru;
import com.hanzo.billing.enums.TrangThaiGiamTru;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.GiamTruService;
import com.hanzo.billing.service.KyCuocService;
import com.hanzo.billing.service.ThueBaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

@Controller
@RequestMapping("/giam-tru")
@RequiredArgsConstructor
public class GiamTruController {

    private final GiamTruService giamTruService;
    private final KyCuocService kyCuocService;
    private final ThueBaoService thueBaoService;

    @GetMapping
    public String danhSach(@RequestParam(required = false) Long kyCuocId,
                           @RequestParam(required = false) Long thueBaoId,
                           @RequestParam(required = false) TrangThaiGiamTru trangThai,
                           Model model) {
        model.addAttribute("danhSach", giamTruService.timKiem(kyCuocId, thueBaoId, trangThai));
        model.addAttribute("danhSachKy", kyCuocService.layTatCa());
        model.addAttribute("danhSachTrangThai", TrangThaiGiamTru.values());
        model.addAttribute("kyCuocId", kyCuocId);
        model.addAttribute("thueBaoId", thueBaoId);
        model.addAttribute("trangThai", trangThai);
        return "giam-tru/danh-sach";
    }

    @GetMapping("/moi")
    public String moFormThem(Model model) {
        napModel(model, new GiamTruForm());
        return "giam-tru/form";
    }

    @GetMapping("/{id}/sua")
    public String moFormSua(@PathVariable Long id, Model model, RedirectAttributes ra) {
        GiamTru gt = giamTruService.layTheoId(id);
        if (gt.getTrangThai() == TrangThaiGiamTru.DA_AP_DUNG) {
            ra.addFlashAttribute("thongBaoLoi", "Khoản giảm trừ này đã áp vào hóa đơn, "
                    + "phải huỷ hóa đơn của kỳ trước thì mới sửa được.");
            return "redirect:/giam-tru";
        }

        GiamTruForm form = new GiamTruForm();
        form.setId(gt.getId());
        form.setThueBaoId(gt.getThueBao().getId());
        form.setKyCuocId(gt.getKyCuoc() == null ? null : gt.getKyCuoc().getId());
        form.setLoai(gt.getLoai());
        form.setSoTien(gt.getSoTien());
        form.setTyLePhanTram(gt.getTyLePhanTram());
        form.setLyDo(gt.getLyDo());

        napModel(model, form);
        return "giam-tru/form";
    }

    @PostMapping
    public String luu(@Valid @ModelAttribute("form") GiamTruForm form,
                      BindingResult loi, Model model, RedirectAttributes ra) {
        if (loi.hasErrors()) {
            napModel(model, form);
            return "giam-tru/form";
        }
        try {
            giamTruService.luu(form);
            ra.addFlashAttribute("thongBao", "Đã lưu khoản giảm trừ.");
            return "redirect:/giam-tru";
        } catch (NghiepVuException ex) {
            model.addAttribute("thongBaoLoi", ex.getMessage());
            napModel(model, form);
            return "giam-tru/form";
        }
    }

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable Long id, RedirectAttributes ra) {
        try {
            giamTruService.xoa(id);
            ra.addFlashAttribute("thongBao", "Đã xoá khoản giảm trừ.");
        } catch (NghiepVuException ex) {
            ra.addFlashAttribute("thongBaoLoi", ex.getMessage());
        }
        return "redirect:/giam-tru";
    }

    private void napModel(Model model, GiamTruForm form) {
        model.addAttribute("form", form);
        model.addAttribute("danhSachKy", kyCuocService.layTatCa());
        model.addAttribute("danhSachLoai", LoaiGiamTru.values());
        model.addAttribute("danhSachThueBao", thueBaoService.layTraSauDeChon());
    }
}
