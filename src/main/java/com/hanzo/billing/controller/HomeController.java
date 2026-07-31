package com.hanzo.billing.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller cho trang chủ của hệ thống.
 */
@Controller
public class HomeController {

    /** Hiển thị trang chủ tạm thời (Phase 0 chưa có số liệu nghiệp vụ). */
    @GetMapping("/")
    public String trangChu() {
        return "index";
    }
}
