package com.hanzo.billing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Các trang liên quan tới đăng nhập và phân quyền.
 *
 * <p>Việc xử lý POST đăng nhập và đăng xuất do Spring Security đảm nhiệm, controller
 * này chỉ trả về trang hiển thị.</p>
 */
@Controller
public class AuthController {

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
}
