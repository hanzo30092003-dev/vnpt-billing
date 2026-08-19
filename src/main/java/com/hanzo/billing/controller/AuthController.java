package com.hanzo.billing.controller;

import com.hanzo.billing.dto.DoiMatKhauForm;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.NguoiDungService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Các trang liên quan tới đăng nhập, phân quyền và mật khẩu của chính người đang dùng.
 *
 * <p>Việc xử lý POST đăng nhập và đăng xuất do Spring Security đảm nhiệm, controller
 * này chỉ trả về trang hiển thị — trừ màn hình đổi mật khẩu, là nghiệp vụ thật.</p>
 */
@Controller
@RequiredArgsConstructor
public class AuthController {

    private final NguoiDungService nguoiDungService;

    /** Trang đăng nhập. Dùng template riêng, không lồng vào layout có sidebar. */
    @GetMapping("/dang-nhap")
    public String dangNhap() {
        return "dang-nhap";
    }

    /** Trang báo không đủ quyền, được trỏ tới từ {@code accessDeniedPage} trong SecurityConfig. */
    @GetMapping("/403")
    public String khongDuQuyen() {
        return "error/403";
    }

    /**
     * Màn hình tự đổi mật khẩu.
     *
     * <p>Không nằm dưới {@code /quan-tri/**}: đây là việc <b>mọi vai trò</b> đều phải làm được,
     * nên nó rơi vào nhánh {@code anyRequest().authenticated()} của SecurityConfig. Đặt nhầm
     * vào cụm quản trị thì nhân viên quầy và kế toán vĩnh viễn không đổi được mật khẩu của
     * chính họ, và mật khẩu khởi tạo do quản trị viên đặt sẽ dùng mãi mãi.</p>
     */
    @GetMapping("/doi-mat-khau")
    public String moFormDoiMatKhau(Model model) {
        model.addAttribute("doiMatKhauForm", new DoiMatKhauForm());
        return "doi-mat-khau";
    }

    /**
     * Đổi mật khẩu rồi <b>kết thúc phiên</b>, buộc đăng nhập lại bằng mật khẩu mới.
     *
     * <p><b>Vì sao không giữ phiên cho tiện.</b> Đây là cùng một luật với nút Khoá tài khoản
     * dựng ở V3a, chỉ khác đường vào: <i>thông tin xác thực vừa đổi thì phiên dựng trên thông
     * tin cũ không còn giá trị</i>. Giữ nguyên phiên là để lại đúng cái lỗ mà việc đổi mật
     * khẩu sinh ra để bịt — người dùng đổi mật khẩu <b>vì</b> nghi có người biết mật khẩu cũ,
     * mà kẻ đó nếu đang mở sẵn một phiên thì vẫn ngồi nguyên trong hệ thống.</p>
     *
     * <p>Phần thưởng kèm theo: bắt đăng nhập lại ngay là <b>tự chứng minh</b> mật khẩu mới
     * dùng được — người dùng biết chắc mình không vừa tự khoá mình ra ngoài.</p>
     *
     * <p>Không dùng {@code RedirectAttributes} cho lời nhắn thành công: thông báo dạng flash
     * sống trong phiên, mà phiên thì vừa bị huỷ ngay dưới đây. Nhắn qua tham số
     * {@code ?doimatkhau} trên URL, giống cách {@code ?dathoat} và {@code ?hethan} đang làm.</p>
     */
    @PostMapping("/doi-mat-khau")
    public String doiMatKhau(@Valid @ModelAttribute("doiMatKhauForm") DoiMatKhauForm form,
                             BindingResult ketQuaRangBuoc,
                             HttpServletRequest request) {

        if (ketQuaRangBuoc.hasErrors()) {
            return "doi-mat-khau";
        }

        try {
            nguoiDungService.doiMatKhau(form);
        } catch (NghiepVuException ex) {
            // Vẽ lại form kèm lời giải thích, không chuyển hướng: người dùng cần thấy lỗi
            // ngay tại chỗ vừa gõ.
            ketQuaRangBuoc.reject("nghiepVu", ex.getMessage());
            return "doi-mat-khau";
        }

        HttpSession phien = request.getSession(false);
        if (phien != null) {
            phien.invalidate();
        }
        SecurityContextHolder.clearContext();

        return "redirect:/dang-nhap?doimatkhau";
    }
}
