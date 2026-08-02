package com.hanzo.billing.controller;

import com.hanzo.billing.dto.BangGiaForm;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.BangGiaCuocService;
import com.hanzo.billing.service.GoiCuocService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/bang-gia")
@RequiredArgsConstructor
public class BangGiaController {

    /** Giá trị đặc biệt trên ô lọc, tương ứng dòng giá không gắn gói cước nào. */
    private static final String MA_GIA_MAC_DINH = "MAC_DINH";

    private final BangGiaCuocService bangGiaCuocService;
    private final GoiCuocService goiCuocService;

    @GetMapping
    public String danhSach(@RequestParam(required = false) String goiCuoc,
                           @RequestParam(required = false) LoaiDichVu loaiDichVu,
                           @RequestParam(required = false) HuongCuocGoi huong,
                           @RequestParam(defaultValue = "false") boolean chiConHieuLuc,
                           Model model) {

        // Ô lọc gói cước có ba trạng thái nên không dùng được Long đơn thuần:
        //   ""          -> không lọc theo gói
        //   "MAC_DINH"  -> chỉ lấy dòng giá mặc định chung (goi_cuoc_id IS NULL)
        //   "<id>"      -> lọc theo gói cước cụ thể
        boolean locTheoGoi = (goiCuoc != null && !goiCuoc.isBlank());
        Long goiCuocId = null;
        if (locTheoGoi && !MA_GIA_MAC_DINH.equals(goiCuoc)) {
            goiCuocId = Long.valueOf(goiCuoc);
        }

        model.addAttribute("danhSach",
                bangGiaCuocService.timCoLoc(locTheoGoi, goiCuocId, loaiDichVu, huong, chiConHieuLuc));
        napDuLieuChonLua(model);
        model.addAttribute("goiCuoc", goiCuoc);
        model.addAttribute("loaiDichVu", loaiDichVu);
        model.addAttribute("huong", huong);
        model.addAttribute("chiConHieuLuc", chiConHieuLuc);
        model.addAttribute("homNay", LocalDate.now());
        return "bang-gia/danh-sach";
    }

    @GetMapping("/them")
    public String moFormThem(Model model) {
        BangGiaForm form = new BangGiaForm();
        form.setLoaiDichVu(LoaiDichVu.THOAI);
        form.setHuong(HuongCuocGoi.NOI_MANG);
        form.setGioCaoDiem(Boolean.FALSE);
        form.setBlockGiay(6);
        form.setNgayHieuLuc(LocalDate.now());
        model.addAttribute("bangGiaForm", form);
        model.addAttribute("laThemMoi", true);
        napDuLieuChonLua(model);
        return "bang-gia/form";
    }

    @GetMapping("/{id}/sua")
    public String moFormSua(@PathVariable Long id, Model model) {
        model.addAttribute("bangGiaForm", BangGiaForm.tuEntity(bangGiaCuocService.layTheoId(id)));
        model.addAttribute("laThemMoi", false);
        napDuLieuChonLua(model);
        return "bang-gia/form";
    }

    @PostMapping("/luu")
    public String luu(@Valid @ModelAttribute("bangGiaForm") BangGiaForm form,
                      BindingResult ketQua, Model model, RedirectAttributes ra) {
        boolean laThemMoi = (form.getId() == null);
        if (ketQua.hasErrors()) {
            model.addAttribute("laThemMoi", laThemMoi);
            napDuLieuChonLua(model);
            return "bang-gia/form";
        }
        try {
            BangGiaCuoc daLuu = bangGiaCuocService.luu(form);
            ra.addFlashAttribute("thongBaoThanhCong",
                    (laThemMoi ? "Đã thêm dòng bảng giá " : "Đã cập nhật dòng bảng giá ") + daLuu.getId());
            return "redirect:/bang-gia";
        } catch (NghiepVuException ex) {
            ketQua.reject("nghiepVu", ex.getMessage());
            model.addAttribute("laThemMoi", laThemMoi);
            napDuLieuChonLua(model);
            return "bang-gia/form";
        }
    }

    @PostMapping("/{id}/xoa")
    public String xoa(@PathVariable Long id, RedirectAttributes ra) {
        bangGiaCuocService.xoa(id);
        ra.addFlashAttribute("thongBaoThanhCong", "Đã xoá dòng bảng giá");
        return "redirect:/bang-gia";
    }

    /** Màn hình "Xem giá đang áp dụng" tại một ngày bất kỳ. */
    @GetMapping("/tra-cuu")
    public String traCuu(@RequestParam(required = false)
                         @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay,
                         Model model) {
        LocalDate ngayTraCuu = (ngay == null) ? LocalDate.now() : ngay;
        model.addAttribute("ngay", ngayTraCuu);
        model.addAttribute("danhSach", bangGiaCuocService.traCuuTheoNgay(ngayTraCuu));
        return "bang-gia/tra-cuu";
    }

    private void napDuLieuChonLua(Model model) {
        model.addAttribute("danhSachGoiCuoc", goiCuocService.layDanhSach());
        model.addAttribute("danhSachLoaiDichVu", LoaiDichVu.values());
        model.addAttribute("danhSachHuong", HuongCuocGoi.values());
        model.addAttribute("maGiaMacDinh", MA_GIA_MAC_DINH);
    }
}
