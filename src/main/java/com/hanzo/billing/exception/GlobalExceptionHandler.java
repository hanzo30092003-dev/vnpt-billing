package com.hanzo.billing.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Bắt các ngoại lệ nghiệp vụ ném ra từ tầng service và biến thành thông báo
 * tiếng Việt hiển thị trên giao diện, thay vì để lộ trang lỗi 500.
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Đưa người dùng trở lại đúng màn hình vừa thao tác, kèm thông báo lỗi.
     * Không dùng trang lỗi riêng vì phần lớn vi phạm nghiệp vụ xảy ra khi bấm nút
     * trên danh sách hoặc trang chi tiết, giữ nguyên ngữ cảnh sẽ dễ hiểu hơn.
     */
    @ExceptionHandler(NghiepVuException.class)
    public String xuLyLoiNghiepVu(NghiepVuException ex,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        log.warn("Vi phạm quy tắc nghiệp vụ: {}", ex.getMessage());
        redirectAttributes.addFlashAttribute("thongBaoLoi", ex.getMessage());
        return "redirect:" + duongDanQuayLai(request);
    }

    /**
     * Lấy đường dẫn quay lại từ header {@code Referer}.
     *
     * <p>Chỉ chấp nhận khi Referer trỏ về chính máy chủ này. Nếu không kiểm tra,
     * kẻ tấn công có thể dụ người dùng bấm vào liên kết khiến ứng dụng chuyển hướng
     * sang trang ngoài (lỗ hổng open redirect).</p>
     */
    private String duongDanQuayLai(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return "/";
        }
        try {
            URI uri = new URI(referer);
            boolean cungMayChu = uri.getHost() == null
                    || uri.getHost().equalsIgnoreCase(request.getServerName());
            if (!cungMayChu) {
                return "/";
            }
            String duongDan = uri.getRawPath();
            if (duongDan == null || duongDan.isBlank()) {
                return "/";
            }
            return uri.getRawQuery() == null ? duongDan : duongDan + "?" + uri.getRawQuery();
        } catch (URISyntaxException e) {
            return "/";
        }
    }
}
