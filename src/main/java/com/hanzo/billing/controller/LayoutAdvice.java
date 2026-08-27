package com.hanzo.billing.controller;

import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Đưa sẵn vài giá trị dùng chung cho mọi view.
 *
 * <p>Thymeleaf 3.1 đã bỏ biến {@code #request}, nên muốn biết đường dẫn hiện tại để
 * tô sáng mục menu đang mở thì phải truyền qua model như thế này.</p>
 */
@ControllerAdvice
@RequiredArgsConstructor
public class LayoutAdvice {

    private final KyCuocRepository kyCuocRepository;
    private final ChiTietSuDungRepository chiTietSuDungRepository;
    private final HoaDonRepository hoaDonRepository;

    /**
     * Sổ đường dẫn của ứng dụng.
     *
     * <p>Dùng {@link ObjectProvider} chứ không tiêm thẳng: {@code RequestMappingHandlerMapping}
     * được dựng SAU lớp advice này, tiêm thẳng là vòng phụ thuộc lúc khởi động.</p>
     */
    private final ObjectProvider<RequestMappingHandlerMapping> soDuongDan;

    private volatile Set<String> duongDanGet;

    /**
     * Bốn con số trên thanh máy, ứng với bốn câu hỏi người vận hành hỏi mỗi sáng.
     *
     * @param kyDangMo   kỳ cước mới nhất còn mở, hoặc {@code "—"} nếu không còn kỳ nào
     * @param soChuaTinh bản ghi sử dụng chưa tính cước
     * @param soQuaHan   hóa đơn đang ở trạng thái quá hạn
     * @param conNo      tổng số tiền khách còn nợ
     */
    public record TinhTrangVanHanh(String kyDangMo, long soChuaTinh, long soQuaHan,
                                   BigDecimal conNo) {
    }

    /**
     * Số liệu vận hành hiện lên thanh máy ở <b>mọi</b> màn hình.
     *
     * <h2>Vì sao đặt ở đây chứ không ở trang chủ</h2>
     * <p>Trang chủ đã có dashboard, nhưng người dùng chỉ ghé trang chủ lúc mới đăng nhập. Bốn
     * con số này là thứ cần thấy <b>trong lúc đang làm việc khác</b>: đang nhập khách hàng mà
     * thấy 148 hóa đơn quá hạn thì biết chiều nay có việc.</p>
     *
     * <h2>Giá phải trả, nói thẳng</h2>
     * <p>Bốn phép đếm chạy ở <b>mỗi lần tải trang</b>. Cả bốn đều là phép đếm hoặc cộng gộp
     * trong CSDL, có chỉ mục sẵn, và bộ dữ liệu này chỉ vài trăm hóa đơn — nên chi phí không
     * đáng kể ở quy mô đồ án. Với quy mô thật thì đây là chỗ đầu tiên cần một lớp đệm.</p>
     *
     * <p>Trả về {@code null} khi chưa đăng nhập, để trang đăng nhập và trang lỗi không phải
     * chạm vào CSDL. Template khai {@code th:if="${tinhTrang != null}"}.</p>
     */
    @ModelAttribute("tinhTrang")
    public TinhTrangVanHanh tinhTrang() {
        if (SecurityUtils.layNguoiDungHienTai().isEmpty()) {
            return null;
        }
        String kyDangMo = kyCuocRepository.findByTrangThai(TrangThaiKyCuoc.MO).stream()
                .max(Comparator.comparing(KyCuoc::getNam).thenComparing(KyCuoc::getThang))
                .map(k -> k.getThang() + "/" + k.getNam())
                .orElse("—");

        return new TinhTrangVanHanh(
                kyDangMo,
                chiTietSuDungRepository.countByTrangThaiTinhCuoc(TrangThaiTinhCuoc.CHUA_TINH),
                hoaDonRepository.countByTrangThai(TrangThaiHoaDon.QUA_HAN),
                hoaDonRepository.tongConNoToanHeThong());
    }

    /**
     * Một mắt xích của breadcrumb.
     *
     * @param duongDan {@code null} nghĩa là mắt xích này KHÔNG bấm được — template dựng nó
     *                 thành chữ thường thay vì liên kết.
     */
    public record MatXich(String nhan, String duongDan) {
    }

    /**
     * Những đường dẫn GET <b>tĩnh</b> (không chứa biến đường dẫn) mà ứng dụng thật sự phục vụ.
     *
     * <p>Vì sao cần: breadcrumb suy mắt xích từ từng đoạn của đường dẫn, nên nó sinh ra cả
     * những đoạn <b>không có controller nào phục vụ</b>. Ở màn hình ghi nhận thanh toán
     * {@code /thanh-toan/moi/461}, nó sinh mắt xích {@code /thanh-toan/moi} — bấm vào ra
     * <b>404</b>. Tương tự {@code /tinh-cuoc/ky}. Đo được ở đợt rà tính năng: 2 trong 12 mắt
     * xích hỏng, và một trong hai nằm trên màn hình kế toán dùng hằng ngày.</p>
     *
     * <p>Hỏi thẳng Spring thay vì giữ một danh sách khai tay: danh sách khai tay sẽ lệch ngay
     * lần đầu ai đó thêm màn hình mới, và lệch <b>im lặng</b>.</p>
     */
    private Set<String> duongDanGet() {
        Set<String> daCo = duongDanGet;
        if (daCo != null) {
            return daCo;
        }
        RequestMappingHandlerMapping anhXa = soDuongDan.getIfAvailable();
        if (anhXa == null) {
            return Set.of();   // chưa dựng xong: coi như không mắt xích nào bấm được
        }
        Set<String> tap = anhXa.getHandlerMethods().keySet().stream()
                .filter(LayoutAdvice::laGet)
                .flatMap(LayoutAdvice::mauDuongDan)
                .filter(d -> !d.contains("{"))
                .collect(Collectors.toUnmodifiableSet());
        duongDanGet = tap;
        return tap;
    }

    private static boolean laGet(RequestMappingInfo thongTin) {
        var pt = thongTin.getMethodsCondition().getMethods();
        return pt.isEmpty() || pt.stream().anyMatch(m -> "GET".equals(m.name()));
    }

    private static Stream<String> mauDuongDan(RequestMappingInfo thongTin) {
        if (thongTin.getPathPatternsCondition() != null) {
            return thongTin.getPathPatternsCondition().getPatternValues().stream();
        }
        if (thongTin.getPatternsCondition() != null) {
            return thongTin.getPatternsCondition().getPatterns().stream();
        }
        return Stream.empty();
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
            // Chỉ gắn liên kết khi đường dẫn đó THẬT SỰ có controller phục vụ; nếu không thì
            // để null và template dựng thành chữ thường. Xem javadoc của duongDanGet().
            String dich = duongDanGet().contains(daDi.toString()) ? daDi.toString() : null;
            matXich.add(new MatXich(nhan, dich));
            laDoanDau = false;
        }

        // Chi tiết một bản ghi: đường dẫn kết thúc bằng id nên không sinh mắt xích nào cho nó
        if (!matXich.isEmpty() && duongDan.matches(".*/\\d+/?$")) {
            // Mắt xích cuối là chính trang đang mở — không bấm được, để null cho nhất quán.
            matXich.add(new MatXich("Chi tiết", null));
        }
        return matXich;
    }

    private static String viHoaChuDau(String doan) {
        String chu = doan.replace('-', ' ');
        return chu.substring(0, 1).toUpperCase() + chu.substring(1);
    }
}
