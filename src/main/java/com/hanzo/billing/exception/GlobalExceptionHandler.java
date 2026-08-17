package com.hanzo.billing.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
     * Hai người cùng sửa một hóa đơn — khoá lạc quan từ chối người ghi sau.
     *
     * <p>Xảy ra khi hai thu ngân cùng mở một hóa đơn rồi cùng bấm ghi nhận thanh toán. Người
     * ghi sau mang theo số phiên bản đã cũ, câu UPDATE khớp 0 dòng, Hibernate ném
     * {@code ObjectOptimisticLockingFailureException}. Xem javadoc của
     * {@code HoaDon.phienBan} để biết vì sao thiếu nó thì mất tiền của khách.</p>
     *
     * <p><b>Vì sao xử như vi phạm nghiệp vụ chứ không như lỗi hệ thống.</b> Đây không phải
     * hỏng hóc — đây là hệ thống làm <i>đúng</i> việc của nó: chặn một lần ghi đè. Người dùng
     * chỉ cần mở lại hóa đơn và làm lại, nên đưa họ về đúng màn hình vừa thao tác kèm lời
     * nhắn, giống mọi vi phạm nghiệp vụ khác — thay vì quăng ra trang 500 kèm mã sự cố làm
     * họ tưởng phần mềm hỏng.</p>
     *
     * <p>Ghi log mức {@code warn} chứ không phải {@code error}: nó đáng để lại vết (đụng độ
     * xảy ra nhiều bất thường là dấu hiệu quy trình thu tiền có vấn đề), nhưng không phải
     * chuyện cần ai đó thức dậy giữa đêm.</p>
     */
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public String xuLyDungDoCapNhat(ObjectOptimisticLockingFailureException ex,
                                    HttpServletRequest request,
                                    RedirectAttributes redirectAttributes) {
        log.warn("Đụng độ cập nhật đồng thời tại {} {}: {}",
                request.getMethod(), request.getRequestURI(), ex.getMessage());
        redirectAttributes.addFlashAttribute("thongBaoLoi",
                "Hóa đơn này vừa được người khác cập nhật trong lúc bạn đang thao tác, "
                        + "nên lần ghi vừa rồi chưa được lưu. "
                        + "Hãy mở lại hóa đơn để xem số còn nợ mới nhất rồi ghi nhận lại.");
        return "redirect:" + duongDanQuayLai(request);
    }

    // =================================================================
    // BẮT MỌI NGOẠI LỆ CÒN LẠI — thêm ở Phase 7 mục C1
    // =================================================================

    /**
     * Ngoại lệ <b>không lường trước</b>: hiện trang lỗi tiếng Việt, ghi stacktrace ra log.
     *
     * <p><b>Vì sao cần dù đã có trang 500 mặc định.</b> Không có handler này thì stacktrace
     * chỉ nằm trong log console — mà console đóng lại là mất. Ở đây stacktrace được ghi bằng
     * {@code log.error} nên nó đi vào file log cấu hình trong {@code application.yml}, và người
     * dùng nhận một <b>mã sự cố</b> để đọc cho người hỗ trợ thay vì một trang trắng.</p>
     *
     * <p>⚠️ <b>Không nuốt lỗi trong im lặng.</b> Bắt {@code Exception} rồi chỉ hiện trang đẹp
     * là cách chắc chắn để một lỗi thật biến mất không dấu vết — nguy hiểm hơn nhiều so với
     * việc để nó nổ ra. Vì vậy mọi nhánh ở đây đều {@code log.error} kèm đủ ngữ cảnh.</p>
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String xuLyLoiKhongLuongTruoc(Exception ex, HttpServletRequest request, Model model) {
        String maSuCo = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        log.error("[{}] Lỗi không lường trước tại {} {}", maSuCo, request.getMethod(),
                request.getRequestURI(), ex);
        model.addAttribute("maSuCo", maSuCo);
        model.addAttribute("duongDanLoi", request.getRequestURI());
        return "error/500";
    }

    /**
     * ⭐ Tham số URL sai kiểu — trả <b>400</b> chứ không phải 500.
     *
     * <p>Gõ {@code /hoa-don/abc} hoặc {@code ?trang=abc} làm Spring không đổi được chuỗi sang
     * {@code Long}/{@code int} và ném {@link MethodArgumentTypeMismatchException}. Trước Phase 7
     * ngoại lệ đó rơi vào nhánh "không lường trước" và thành <b>lỗi hệ thống 500</b> — nói sai
     * bản chất: lỗi nằm ở <i>yêu cầu người dùng gửi lên</i>, không phải ở máy chủ.</p>
     *
     * <p>Hệ quả thực tế của việc phân loại sai: mỗi lần ai đó gõ nhầm địa chỉ, log lại có thêm
     * một dòng {@code ERROR} kèm stacktrace và một mã sự cố — đủ để che lấp những lỗi 500 thật.</p>
     *
     * <p>Ghi ở mức {@code warn} chứ không {@code error}, và <b>không</b> kèm stacktrace: đây là
     * chuyện bình thường của một ứng dụng web mở ra Internet.</p>
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String xuLyThamSoSaiKieu(MethodArgumentTypeMismatchException ex,
                                    HttpServletRequest request, Model model) {
        log.warn("Tham số sai kiểu tại {}: '{}' không đổi được sang {}",
                request.getRequestURI(), ex.getValue(),
                ex.getRequiredType() == null ? "?" : ex.getRequiredType().getSimpleName());
        model.addAttribute("thamSoSai", ex.getName());
        model.addAttribute("giaTriSai", String.valueOf(ex.getValue()));
        return "error/400";
    }

    /**
     * Không tìm thấy tài nguyên tĩnh — trả 404 chứ không phải 500.
     *
     * <p>Tách riêng vì nếu để {@link #xuLyLoiKhongLuongTruoc} bắt thì mọi lần trình duyệt hỏi
     * một file không có (favicon, source map) sẽ thành một dòng {@code ERROR} kèm stacktrace
     * đầy trong log — và log đầy tiếng ồn là log không ai đọc nữa.</p>
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String xuLyKhongTimThay(NoResourceFoundException ex, HttpServletRequest request) {
        log.debug("Không tìm thấy tài nguyên {}", request.getRequestURI());
        return "error/404";
    }

    /**
     * ⭐ Ném lại {@link AccessDeniedException} thay vì xử lý.
     *
     * <p>Không có handler này, {@link #xuLyLoiKhongLuongTruoc} sẽ bắt luôn lỗi phân quyền và
     * biến <b>403 thành 500</b>: người dùng thiếu quyền nhận về "lỗi hệ thống" thay vì "không
     * đủ quyền", và cấu hình {@code accessDeniedPage} của Spring Security thành vô hiệu.</p>
     *
     * <p>Ném lại để {@code ExceptionTranslationFilter} — vốn nằm <i>ngoài</i> DispatcherServlet
     * — làm đúng việc của nó. Đây là cái bẫy phổ biến nhất khi thêm một handler bắt
     * {@code Exception}, và nó chỉ lộ ra khi có phép kiểm 403 thật.</p>
     */
    @ExceptionHandler(AccessDeniedException.class)
    public void nemLaiLoiPhanQuyen(AccessDeniedException ex) {
        throw ex;
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
