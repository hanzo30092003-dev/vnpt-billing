package com.hanzo.billing.controller;

import com.hanzo.billing.dto.GoiCuocForm;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.GoiCuocService;
import com.hanzo.billing.util.ThamSoPhanTrang;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/goi-cuoc")
@RequiredArgsConstructor
public class GoiCuocController {

    private static final int SO_DONG_MOI_TRANG = 10;

    private final GoiCuocService goiCuocService;

    @GetMapping
    public String danhSach(Model model) {
        model.addAttribute("danhSach", goiCuocService.layDanhSach());
        return "goi-cuoc/danh-sach";
    }

    @GetMapping("/them")
    public String moFormThem(Model model) {
        GoiCuocForm form = new GoiCuocForm();
        form.setLoaiThueBao(LoaiThueBao.TRA_SAU);
        form.setCuocThueBaoThang(BigDecimal.ZERO);
        form.setPhutNoiMangMienPhi(0);
        form.setPhutNgoaiMangMienPhi(0);
        form.setSmsMienPhi(0);
        form.setDataMienPhiMb(0);
        form.setNgayHieuLuc(LocalDate.now());
        model.addAttribute("goiCuocForm", form);
        model.addAttribute("laThemMoi", true);
        return "goi-cuoc/form";
    }

    @GetMapping("/{id}/sua")
    public String moFormSua(@PathVariable Long id, Model model) {
        model.addAttribute("goiCuocForm", GoiCuocForm.tuEntity(goiCuocService.layTheoId(id)));
        model.addAttribute("laThemMoi", false);
        return "goi-cuoc/form";
    }

    @PostMapping("/luu")
    public String luu(@Valid @ModelAttribute("goiCuocForm") GoiCuocForm form,
                      BindingResult ketQua, Model model, RedirectAttributes ra) {
        boolean laThemMoi = (form.getId() == null);
        if (ketQua.hasErrors()) {
            model.addAttribute("laThemMoi", laThemMoi);
            return "goi-cuoc/form";
        }
        try {
            GoiCuoc daLuu = goiCuocService.luu(form);
            ra.addFlashAttribute("thongBaoThanhCong",
                    (laThemMoi ? "Đã thêm gói cước " : "Đã cập nhật gói cước ") + daLuu.getMaGoi());
            return "redirect:/goi-cuoc/" + daLuu.getId();
        } catch (NghiepVuException ex) {
            // Bắt tại chỗ để vẽ lại form kèm dữ liệu vừa nhập
            ketQua.reject("nghiepVu", ex.getMessage());
            model.addAttribute("laThemMoi", laThemMoi);
            return "goi-cuoc/form";
        }
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id,
                          @RequestParam(defaultValue = "0") int trang,
                          Model model) {
        model.addAttribute("goiCuoc", goiCuocService.layTheoId(id));
        model.addAttribute("tomTatUuDai", goiCuocService.tomTatUuDai(goiCuocService.layTheoId(id)));
        model.addAttribute("bangGiaRieng", goiCuocService.layBangGiaRieng(id));
        model.addAttribute("soThueBao", goiCuocService.demThueBaoDangDung(id));
        model.addAttribute("trangThueBao", goiCuocService.layThueBaoDangDung(id,
                PageRequest.of(ThamSoPhanTrang.trangHopLe(trang), SO_DONG_MOI_TRANG,
                        Sort.by("soThueBao"))));
        return "goi-cuoc/chi-tiet";
    }

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable Long id, RedirectAttributes ra) {
        goiCuocService.xoa(id);
        ra.addFlashAttribute("thongBaoThanhCong", "Đã xoá gói cước");
        return "redirect:/goi-cuoc";
    }
}
