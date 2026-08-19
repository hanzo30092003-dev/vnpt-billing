package com.hanzo.billing.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Đưa sẵn vài giá trị dùng chung cho mọi view.
 *
 * <p>Thymeleaf 3.1 đã bỏ biến {@code #request}, nên muốn biết đường dẫn hiện tại để
 * tô sáng mục menu đang mở thì phải truyền qua model như thế này.</p>
 */
@ControllerAdvice
public class LayoutAdvice {

    /** Một mắt xích của breadcrumb. */
    public record MatXich(String nhan, String duongDan) {
    }

    /**
     * Tên hiển thị của từng phân hệ, tra theo đoạn đầu của đường dẫn.
     *
     * <p>Dùng {@code LinkedHashMap} để thứ tự khai báo cũng là thứ tự đọc khi rà soát.</p>
     */
    private static final Map<String, String> TEN_PHAN_HE = new LinkedHashMap<>();

    static {
        TEN_PHAN_HE.put("khach-hang", "Khách hàng");
        TEN_PHAN_HE.put("thue-bao", "Thuê bao");
        TEN_PHAN_HE.put("goi-cuoc", "Gói cước");
        TEN_PHAN_HE.put("bang-gia", "Bảng giá");
        TEN_PHAN_HE.put("cdr", "CDR");
        TEN_PHAN_HE.put("ky-cuoc", "Kỳ cước");
        TEN_PHAN_HE.put("tinh-cuoc", "Tính cước");
        TEN_PHAN_HE.put("hoa-don", "Hóa đơn");
        TEN_PHAN_HE.put("thanh-toan", "Thanh toán");
        TEN_PHAN_HE.put("cong-no", "Công nợ");
        TEN_PHAN_HE.put("giam-tru", "Giảm trừ");
        TEN_PHAN_HE.put("bao-cao", "Báo cáo");
        TEN_PHAN_HE.put("quan-tri", "Quản trị");
        TEN_PHAN_HE.put("doi-mat-khau", "Đổi mật khẩu");
    }

    /** Tên hiển thị của các trang con hay gặp. */
    private static final Map<String, String> TEN_TRANG_CON = new LinkedHashMap<>();

    static {
        TEN_TRANG_CON.put("them", "Thêm mới");
        TEN_TRANG_CON.put("moi", "Thêm mới");
        TEN_TRANG_CON.put("sua", "Chỉnh sửa");
        TEN_TRANG_CON.put("dang-ky", "Đăng ký");
        TEN_TRANG_CON.put("sinh-du-lieu", "Sinh dữ liệu");
        TEN_TRANG_CON.put("import", "Nhập từ CSV");
        TEN_TRANG_CON.put("tra-cuu", "Tra cứu");
        TEN_TRANG_CON.put("doi-soat", "Đối soát cước");
        TEN_TRANG_CON.put("ky", "Hóa đơn của kỳ");
        TEN_TRANG_CON.put("doanh-thu-ky", "Doanh thu theo kỳ");
        TEN_TRANG_CON.put("doanh-thu-goi-cuoc", "Doanh thu theo gói cước");
        TEN_TRANG_CON.put("doanh-thu-dich-vu", "Doanh thu theo loại dịch vụ");
        TEN_TRANG_CON.put("top-thue-bao", "Top thuê bao cước cao");
        TEN_TRANG_CON.put("san-luong", "Sản lượng dịch vụ");
        TEN_TRANG_CON.put("nguoi-dung", "Người dùng");
    }

    @ModelAttribute("duongDanHienTai")
    public String duongDanHienTai(HttpServletRequest request) {
        return request.getRequestURI();
    }

    /**
     * Breadcrumb suy <b>tự động</b> từ đường dẫn.
     *
     * <h2>Vì sao suy tự động thay vì khai ở từng template</h2>
     * <p>Yêu cầu B.1 là "tiêu đề trang + breadcrumb ở <b>mọi</b> màn hình". Khai tay ở 25
     * template nghĩa là 25 chỗ có thể quên, và chắc chắn màn hình thêm sau sẽ thiếu — đúng
     * kiểu lỗi mà mục A vừa phải đi dọn (sidebar {@code href="#"} và luật phân trang chép
     * thiếu ở hai controller).</p>
     *
     * <p>Suy từ đường dẫn thì mọi màn hình <b>đang có và sẽ có</b> đều tự có breadcrumb, và
     * không có đường nào để quên.</p>
     *
     * <p>Đoạn thuần số (id) bị bỏ qua: {@code /hoa-don/307} hiện là <i>Trang chủ › Hóa đơn ›
     * Chi tiết</i> chứ không phải <i>› 307</i> — con số đó không nói gì với người đọc.</p>
     */
    @ModelAttribute("breadcrumb")
    public List<MatXich> breadcrumb(HttpServletRequest request) {
        List<MatXich> matXich = new ArrayList<>();
        String duongDan = request.getRequestURI();
        if (duongDan == null || "/".equals(duongDan)) {
            return matXich;   // trang chủ không cần breadcrumb
        }

        String[] doan = duongDan.split("/");
        StringBuilder daDi = new StringBuilder();
        boolean laDoanDau = true;

        for (String d : doan) {
            if (d.isBlank()) {
                continue;
            }
            if (d.chars().allMatch(Character::isDigit)) {
                // Đoạn là id — không thêm mắt xích, nhưng vẫn phải nối vào đường dẫn
                daDi.append('/').append(d);
                continue;
            }
            daDi.append('/').append(d);
            String nhan = laDoanDau
                    ? TEN_PHAN_HE.getOrDefault(d, viHoaChuDau(d))
                    : TEN_TRANG_CON.getOrDefault(d, viHoaChuDau(d));
            matXich.add(new MatXich(nhan, daDi.toString()));
            laDoanDau = false;
        }

        // Chi tiết một bản ghi: đường dẫn kết thúc bằng id nên không sinh mắt xích nào cho nó
        if (!matXich.isEmpty() && duongDan.matches(".*/\\d+/?$")) {
            matXich.add(new MatXich("Chi tiết", duongDan));
        }
        return matXich;
    }

    private static String viHoaChuDau(String doan) {
        String chu = doan.replace('-', ' ');
        return chu.substring(0, 1).toUpperCase() + chu.substring(1);
    }
}
