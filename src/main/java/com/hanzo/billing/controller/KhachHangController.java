package com.hanzo.billing.controller;

import com.hanzo.billing.dto.KhachHangForm;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.enums.LoaiKhachHang;
import com.hanzo.billing.enums.TrangThaiKhachHang;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.KhachHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private static final int SO_DONG_MOI_TRANG = 15;

    private final KhachHangService khachHangService;

    /** Danh sách có tìm kiếm, lọc và phân trang. */
    @GetMapping
    public String danhSach(@RequestParam(required = false) String tuKhoa,
                           @RequestParam(required = false) LoaiKhachHang loaiKh,
                           @RequestParam(required = false) TrangThaiKhachHang trangThai,
                           @RequestParam(defaultValue = "0") int trang,
                           Model model) {

        Pageable phanTrang = PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG,
                Sort.by(Sort.Direction.DESC, "id"));
        Page<KhachHang> ketQua = khachHangService.timKiem(tuKhoa, loaiKh, trangThai, phanTrang);

        model.addAttribute("ketQua", ketQua);
        model.addAttribute("danhSachLoaiKh", LoaiKhachHang.values());
        model.addAttribute("danhSachTrangThai", TrangThaiKhachHang.values());

        // Giữ lại giá trị đang lọc để form hiển thị đúng và liên kết phân trang
        // mang theo được bộ lọc, không bị mất khi chuyển trang.
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("loaiKh", loaiKh);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("chuoiLoc", chuoiLoc(tuKhoa, loaiKh, trangThai));

        return "khach-hang/danh-sach";
    }

    @GetMapping("/them")
    public String moFormThem(Model model) {
        KhachHangForm form = new KhachHangForm();
        form.setLoaiKh(LoaiKhachHang.CA_NHAN);
        form.setNgayDangKy(LocalDate.now());
        model.addAttribute("khachHangForm", form);
        model.addAttribute("laThemMoi", true);
        return "khach-hang/form";
    }

    @GetMapping("/{id}/sua")
    public String moFormSua(@PathVariable Long id, Model model) {
        model.addAttribute("khachHangForm", KhachHangForm.tuEntity(khachHangService.layTheoId(id)));
        model.addAttribute("laThemMoi", false);
        return "khach-hang/form";
    }

    /**
     * Lưu form thêm/sửa.
     *
     * <p>Bắt {@link NghiepVuException} ngay tại đây thay vì để GlobalExceptionHandler
     * xử lý, vì cần vẽ lại form kèm dữ liệu người dùng vừa nhập. Nếu để chuyển hướng
     * thì toàn bộ nội dung đang gõ dở sẽ mất.</p>
     */
    @PostMapping("/luu")
    public String luu(@Valid @ModelAttribute("khachHangForm") KhachHangForm form,
                      BindingResult ketQuaRangBuoc,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        boolean laThemMoi = (form.getId() == null);

        if (ketQuaRangBuoc.hasErrors()) {
            model.addAttribute("laThemMoi", laThemMoi);
            return "khach-hang/form";
        }

        try {
            KhachHang daLuu = khachHangService.luu(form);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                    (laThemMoi ? "Đã thêm khách hàng " : "Đã cập nhật khách hàng ")
                            + daLuu.getMaKh() + " - " + daLuu.getTenKh());
            return "redirect:/khach-hang/" + daLuu.getId();
        } catch (NghiepVuException ex) {
            ketQuaRangBuoc.reject("nghiepVu", ex.getMessage());
            model.addAttribute("laThemMoi", laThemMoi);
            return "khach-hang/form";
        }
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        model.addAttribute("khachHang", khachHangService.layTheoId(id));
        model.addAttribute("danhSachThueBao", khachHangService.layThueBaoCuaKhachHang(id));
        return "khach-hang/chi-tiet";
    }

    @PostMapping("/{id}/ngung-giao-dich")
    public String ngungGiaoDich(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        khachHangService.ngungGiaoDich(id);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã chuyển khách hàng sang trạng thái ngừng giao dịch");
        return "redirect:/khach-hang/" + id;
    }

    /** Ghép các tham số lọc thành chuỗi query để gắn vào liên kết phân trang. */
    private String chuoiLoc(String tuKhoa, LoaiKhachHang loaiKh, TrangThaiKhachHang trangThai) {
        StringBuilder sb = new StringBuilder();
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sb.append("&tuKhoa=").append(java.net.URLEncoder.encode(tuKhoa,
                    java.nio.charset.StandardCharsets.UTF_8));
        }
        if (loaiKh != null) {
            sb.append("&loaiKh=").append(loaiKh.name());
        }
        if (trangThai != null) {
            sb.append("&trangThai=").append(trangThai.name());
        }
        return sb.toString();
    }
}
