package com.hanzo.billing.controller;

import com.hanzo.billing.dto.NguoiDungForm;
import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.enums.VaiTro;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.NguoiDungService;
import com.hanzo.billing.util.SecurityUtils;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Màn hình quản lý tài khoản đăng nhập.
 *
 * <p>Đặt dưới {@code /quan-tri/**} nên luật chặn "chỉ ADMIN" trong
 * {@code SecurityConfig} đã có sẵn từ Phase 2 — phân hệ này chỉ việc dọn vào đúng chỗ đã
 * dành sẵn cho nó.</p>
 */
@Controller
@RequestMapping("/quan-tri/nguoi-dung")
@RequiredArgsConstructor
public class NguoiDungController {

    private final NguoiDungService nguoiDungService;

    /** Ba vai trò đổ vào ô chọn của form, và cũng để danh sách dịch mã vai trò ra chữ. */
    @ModelAttribute("danhSachVaiTro")
    public VaiTro[] danhSachVaiTro() {
        return VaiTro.values();
    }

    /**
     * Mã số tài khoản đang đăng nhập, để màn hình đánh dấu "chính bạn" và ẩn nút khoá.
     *
     * <p>Ẩn nút chỉ là phép lịch sự với mắt người dùng; chốt chặn thật nằm trong
     * {@code NguoiDungServiceImpl}, nên gửi thẳng request vẫn bị từ chối.</p>
     */
    @ModelAttribute("idDangDangNhap")
    public Long idDangDangNhap() {
        return SecurityUtils.layNguoiDungHienTai().map(p -> p.getId()).orElse(null);
    }

    @GetMapping
    public String danhSach(Model model) {
        model.addAttribute("danhSach", nguoiDungService.danhSach());
        return "quan-tri/nguoi-dung/danh-sach";
    }

    @GetMapping("/them")
    public String moFormThem(Model model) {
        NguoiDungForm form = new NguoiDungForm();
        form.setVaiTro(VaiTro.NHAN_VIEN);
        model.addAttribute("nguoiDungForm", form);
        model.addAttribute("laThemMoi", true);
        return "quan-tri/nguoi-dung/form";
    }

    @GetMapping("/{id}/sua")
    public String moFormSua(@PathVariable Long id, Model model) {
        model.addAttribute("nguoiDungForm", NguoiDungForm.tuEntity(nguoiDungService.layTheoId(id)));
        model.addAttribute("laThemMoi", false);
        return "quan-tri/nguoi-dung/form";
    }

    /**
     * Lưu form thêm/sửa.
     *
     * <p>Bắt {@link NghiepVuException} tại chỗ thay vì để {@code GlobalExceptionHandler}
     * chuyển hướng, để vẽ lại form kèm những gì người dùng vừa gõ — giống màn hình khách hàng.</p>
     */
    @PostMapping("/luu")
    public String luu(@Valid @ModelAttribute("nguoiDungForm") NguoiDungForm form,
                      BindingResult ketQuaRangBuoc,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        boolean laThemMoi = (form.getId() == null);

        if (ketQuaRangBuoc.hasErrors()) {
            model.addAttribute("laThemMoi", laThemMoi);
            return "quan-tri/nguoi-dung/form";
        }

        try {
            NguoiDung daLuu = nguoiDungService.luu(form);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                    (laThemMoi ? "Đã tạo tài khoản " : "Đã cập nhật tài khoản ")
                            + daLuu.getTenDangNhap() + " - " + daLuu.getHoTen());
            return "redirect:/quan-tri/nguoi-dung";
        } catch (NghiepVuException ex) {
            ketQuaRangBuoc.reject("nghiepVu", ex.getMessage());
            model.addAttribute("laThemMoi", laThemMoi);
            return "quan-tri/nguoi-dung/form";
        }
    }

    @PostMapping("/{id}/khoa")
    public String khoa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        nguoiDungService.khoaTaiKhoan(id);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã khoá tài khoản. Người này không đăng nhập được nữa, và phiên đang mở "
                        + "của họ cũng đã bị thoát ra.");
        return "redirect:/quan-tri/nguoi-dung";
    }

    @PostMapping("/{id}/mo-khoa")
    public String moKhoa(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        nguoiDungService.moKhoaTaiKhoan(id);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã mở khoá tài khoản. Người này đăng nhập lại được bằng mật khẩu cũ.");
        return "redirect:/quan-tri/nguoi-dung";
    }
}
