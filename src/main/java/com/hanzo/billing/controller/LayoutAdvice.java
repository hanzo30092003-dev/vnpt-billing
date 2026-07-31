package com.hanzo.billing.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Đưa sẵn vài giá trị dùng chung cho mọi view.
 *
 * <p>Thymeleaf 3.1 đã bỏ biến {@code #request}, nên muốn biết đường dẫn hiện tại để
 * tô sáng mục menu đang mở thì phải truyền qua model như thế này.</p>
 */
@ControllerAdvice
public class LayoutAdvice {

    @ModelAttribute("duongDanHienTai")
    public String duongDanHienTai(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
