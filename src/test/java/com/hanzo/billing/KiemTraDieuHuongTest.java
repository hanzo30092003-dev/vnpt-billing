package com.hanzo.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ BẤT BIẾN ĐIỀU HƯỚNG — mọi liên kết trong template phải trỏ tới một endpoint có thật.
 *
 * <h2>Vì sao có test này</h2>
 * <p>Phase 6 phát hiện bốn màn hình của Phase 5 — hóa đơn, thanh toán, công nợ, giảm trừ — có
 * test, có ảnh chụp, có tài liệu, nhưng sidebar vẫn để <code>href="#"</code> suốt cả một phase.
 * Người dùng <b>không có đường nào tới chúng ngoài việc gõ thẳng URL</b>.</p>
 *
 * <p>Lý do không ai phát hiện: mọi phép kiểm đều gọi thẳng <code>/cong-no</code> bằng đường dẫn
 * tuyệt đối — tức <b>đi vòng qua đúng cái thứ bị hỏng</b>. Kiểm một màn hình bằng cách gõ URL
 * của nó chứng minh màn hình chạy được, <b>không</b> chứng minh người dùng tới được nó.</p>
 *
 * <p>Sửa từng chỗ thì lần sau lại quên. Test này là <b>bất biến tổng quát</b>: nó quét mọi
 * template và đối chiếu với bảng định tuyến thật của Spring, nên bắt được cả liên kết hỏng
 * phát sinh về sau — kể cả do gõ nhầm đường dẫn.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy (do dùng {@code @SpringBootTest} để lấy bảng
 * định tuyến thật).</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("Bất biến điều hướng")
class KiemTraDieuHuongTest {

    private static final Path THU_MUC_TEMPLATE = Path.of("src/main/resources/templates");

    /** Bắt {@code href="..."} tĩnh, tức không đi qua Thymeleaf. */
    private static final Pattern HREF_TINH = Pattern.compile("(?<!th:)href\\s*=\\s*\"([^\"]*)\"");

    /**
     * Tiền tố được phép xuất hiện dưới dạng {@code href} tĩnh.
     *
     * <p>Liên kết ra ngoài và neo trong trang không thuộc phạm vi bảng định tuyến của ứng dụng.</p>
     */
    private static final List<String> HREF_TINH_HOP_LE = List.of("http://", "https://", "mailto:",
            "#tab-", "javascript:");

    /**
     * Tài nguyên tĩnh — do {@code ResourceHttpRequestHandler} phục vụ, <b>không</b> có trong
     * bảng {@code RequestMappingHandlerMapping}. Bỏ qua chứ không báo hỏng.
     */
    private static final List<String> TAI_NGUYEN_TINH = List.of("/css/", "/js/", "/images/",
            "/webjars/", "/favicon");

    /**
     * Rút danh sách đường dẫn từ mọi biểu thức {@code th:href="@{...}"} của một template.
     *
     * <p><b>Không dùng một biểu thức chính quy duy nhất</b>, vì đường dẫn có thể chứa chính dấu
     * ngoặc nhọn ({@code /hoa-don/{id}}) — một mẫu regex đơn giản sẽ cắt nhầm ở dấu {@code }}
     * đầu tiên và biến {@code /hoa-don/{id}} thành {@code /hoa-don/{id}, rồi báo hỏng một liên
     * kết hoàn toàn đúng. Đây đúng là loại "phép kiểm sai" mà bài học 43.5 nói tới, và nó đã
     * xảy ra thật ở lần chạy đầu tiên của test này.</p>
     *
     * <p>Cách làm: tìm mốc {@code @{} rồi <b>đếm độ sâu ngoặc</b> để lấy đúng biểu thức, sau đó
     * cắt bỏ phần tham số đường dẫn {@code (id=...)} và phần truy vấn.</p>
     *
     * @return danh sách đường dẫn thô; đường dẫn động (ghép chuỗi, biến) trả về chuỗi rỗng
     */
    private static List<String> rutDuongDan(String noiDung) {
        List<String> ketQua = new ArrayList<>();
        String moc = "th:href";
        int i = noiDung.indexOf(moc);
        while (i >= 0) {
            int mo = noiDung.indexOf("@{", i);
            int hetThe = noiDung.indexOf('>', i);
            if (mo < 0 || (hetThe >= 0 && mo > hetThe)) {
                i = noiDung.indexOf(moc, i + moc.length());
                continue;
            }
            int sau = mo + 2;
            int doSau = 1;
            while (sau < noiDung.length() && doSau > 0) {
                char c = noiDung.charAt(sau);
                if (c == '{') {
                    doSau++;
                } else if (c == '}') {
                    doSau--;
                }
                if (doSau > 0) {
                    sau++;
                }
            }
            ketQua.add(catThamSo(noiDung.substring(mo + 2, Math.min(sau, noiDung.length()))));
            i = noiDung.indexOf(moc, sau);
        }
        return ketQua;
    }

    /** Cắt bỏ {@code (id=...)}, phần truy vấn và nhận diện biểu thức động. */
    private static String catThamSo(String bieuThuc) {
        String s = bieuThuc.trim();
        // @{${bien}} hoặc @{'/x?' + ${y}} — đường dẫn dựng lúc chạy, không kiểm tĩnh được
        if (s.startsWith("$")) {
            return "";
        }
        if (s.startsWith("'")) {
            int dong = s.indexOf('\'', 1);
            s = dong > 0 ? s.substring(1, dong) : "";
        }
        int ngoac = s.indexOf('(');
        if (ngoac >= 0) {
            s = s.substring(0, ngoac);
        }
        int hoi = s.indexOf('?');
        if (hoi >= 0) {
            s = s.substring(0, hoi);
        }
        return s.trim();
    }

    private static boolean laTaiNguyenTinh(String duongDan) {
        return TAI_NGUYEN_TINH.stream().anyMatch(duongDan::startsWith);
    }

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    /** Mọi mẫu đường dẫn Spring thật sự phục vụ, ví dụ {@code /hoa-don/{id}}. */
    private Set<String> mauDuongDanThat() {
        Set<String> mau = new TreeSet<>();
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e
                : requestMappingHandlerMapping.getHandlerMethods().entrySet()) {
            RequestMappingInfo info = e.getKey();
            if (info.getPathPatternsCondition() != null) {
                info.getPathPatternsCondition().getPatternValues().forEach(mau::add);
            }
        }
        return mau;
    }

    private static List<Path> moiTemplate() {
        try (Stream<Path> duyet = Files.walk(THU_MUC_TEMPLATE)) {
            return duyet.filter(p -> p.toString().endsWith(".html")).sorted().toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không đọc được thư mục template", ex);
        }
    }

    /** Bình luận HTML, gồm cả bình luận Thymeleaf {@code <!--/* ... *\/-->}. */
    private static final Pattern BINH_LUAN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    /**
     * Đọc template và <b>bỏ mọi bình luận</b> trước khi phân tích.
     *
     * <p>Một liên kết nằm trong bình luận không phải là liên kết. Không bỏ bình luận thì test
     * báo hỏng ngay chính đoạn ghi chú giải thích vì sao một {@code href} đã bị xoá — điều đã
     * xảy ra thật khi viết test này, và nó là một báo động giả kinh điển: phép kiểm phân tích
     * <i>văn bản</i> thay vì phân tích <i>mã đánh dấu</i>.</p>
     */
    private static String docFile(Path p) {
        try {
            String noiDung = Files.readString(p, StandardCharsets.UTF_8);
            return BINH_LUAN.matcher(noiDung).replaceAll(" ");
        } catch (IOException ex) {
            throw new UncheckedIOException("Không đọc được " + p, ex);
        }
    }

    /**
     * Chuẩn hoá một đường dẫn lấy từ template về dạng so khớp được với mẫu của Spring.
     *
     * <p>Bỏ tham số truy vấn, bỏ dấu {@code /} thừa ở cuối, và <b>thay mọi biến đường dẫn
     * {@code {id}} bằng một chỗ trống chung</b> — Spring khai {@code /hoa-don/{id}} còn template
     * viết {@code /hoa-don/{id}(id=...)}, hai bên đặt tên biến không nhất thiết giống nhau.</p>
     */
    private static String chuanHoa(String duongDan) {
        String d = duongDan.trim();
        int viTriHoi = d.indexOf('?');
        if (viTriHoi >= 0) {
            d = d.substring(0, viTriHoi);
        }
        d = d.replaceAll("\\{[^}]*\\}", "{}");
        if (d.length() > 1 && d.endsWith("/")) {
            d = d.substring(0, d.length() - 1);
        }
        return d;
    }

    @Test
    @DisplayName("1. ⭐ Mọi th:href=\"@{...}\" đều trỏ tới một endpoint có thật")
    void moiLienKetThymeleafTroToiEndpointCoThat() {
        Set<String> mauThat = new TreeSet<>();
        mauDuongDanThat().forEach(m -> mauThat.add(chuanHoa(m)));

        List<String> hong = new ArrayList<>();
        int soLienKet = 0;

        for (Path template : moiTemplate()) {
            for (String tho : rutDuongDan(docFile(template))) {
                if (tho.isEmpty() || laTaiNguyenTinh(tho)) {
                    continue;
                }
                soLienKet++;
                String duongDan = chuanHoa(tho);
                if (!mauThat.contains(duongDan)) {
                    hong.add(THU_MUC_TEMPLATE.relativize(template) + " → " + tho
                            + "  (chuẩn hoá: " + duongDan + ")");
                }
            }
        }

        assertThat(soLienKet)
                .as("Phải quét được liên kết, nếu không thì biểu thức tìm kiếm sai và test này "
                        + "xanh mà chẳng kiểm gì (bài học 43.5)")
                .isGreaterThan(40);
        assertThat(hong)
                .as("Đã quét %d liên kết trong %d template. Liên kết dưới đây KHÔNG khớp "
                        + "endpoint nào — gõ nhầm đường dẫn, hoặc controller đã đổi mà template "
                        + "chưa đổi theo:%n  %s",
                        soLienKet, moiTemplate().size(), String.join("\n  ", hong))
                .isEmpty();
    }

    /**
     * ⭐ Đây là phép kiểm bắt được đúng lỗi của Phase 5.
     *
     * <p>Một mục menu để <code>href="#"</code> trong khi chức năng đã có controller nghĩa là
     * chức năng đó tồn tại nhưng <b>không ai bấm tới được</b>. Nó không gây lỗi, không làm test
     * nào đỏ, và sống sót qua cả một phase.</p>
     */
    @Test
    @DisplayName("2. ⭐ Không còn href=\"#\" chết trong template")
    void khongConHrefChet() {
        List<String> hrefChet = new ArrayList<>();

        for (Path template : moiTemplate()) {
            Matcher m = HREF_TINH.matcher(docFile(template));
            while (m.find()) {
                String giaTri = m.group(1).trim();
                if (giaTri.isEmpty() || "#".equals(giaTri)) {
                    hrefChet.add(THU_MUC_TEMPLATE.relativize(template) + " → href=\"" + giaTri + "\"");
                }
            }
        }

        assertThat(hrefChet)
                .as("href=\"#\" là một liên kết KHÔNG DẪN ĐI ĐÂU. Nếu chức năng đã có "
                        + "controller thì đây chính là lỗi của Phase 5: màn hình tồn tại nhưng "
                        + "không ai bấm tới được. Nếu chức năng chưa làm thì bỏ hẳn mục menu "
                        + "thay vì để một nút bấm không phản hồi:%n  %s",
                        String.join("\n  ", hrefChet))
                .isEmpty();
    }

    @Test
    @DisplayName("3. href tĩnh chỉ được dùng cho liên kết ngoài hoặc neo trong trang")
    void hrefTinhChiDungChoLienKetNgoai() {
        List<String> sai = new ArrayList<>();

        for (Path template : moiTemplate()) {
            Matcher m = HREF_TINH.matcher(docFile(template));
            while (m.find()) {
                String giaTri = m.group(1).trim();
                if (giaTri.isEmpty() || "#".equals(giaTri)) {
                    continue;   // đã có phép kiểm 2 lo
                }
                boolean hopLe = HREF_TINH_HOP_LE.stream().anyMatch(giaTri::startsWith);
                if (!hopLe) {
                    sai.add(THU_MUC_TEMPLATE.relativize(template) + " → href=\"" + giaTri + "\"");
                }
            }
        }

        assertThat(sai)
                .as("Đường dẫn nội bộ phải viết bằng th:href=\"@{...}\" để Thymeleaf gắn context "
                        + "path. Viết href tĩnh thì liên kết hỏng ngay khi ứng dụng chạy dưới "
                        + "một context path khác '/':%n  %s", String.join("\n  ", sai))
                .isEmpty();
    }

    /**
     * Mọi mục trên thanh điều hướng phải là một endpoint GET có thật.
     *
     * <p>Tách khỏi phép kiểm 1 vì sidebar là <b>con đường duy nhất</b> người dùng có để đi
     * lại giữa các phân hệ — một liên kết hỏng ở đây làm cả một phân hệ biến mất, còn hỏng ở
     * một nút bên trong màn hình thì chỉ hỏng một thao tác.</p>
     */
    @Test
    @DisplayName("4. Mọi mục sidebar đều trỏ tới endpoint GET có thật")
    void moiMucSidebarTroToiEndpointThat() {
        String layout = docFile(THU_MUC_TEMPLATE.resolve("fragments/layout.html"));
        Set<String> mauThat = new LinkedHashSet<>();
        mauDuongDanThat().forEach(m -> mauThat.add(chuanHoa(m)));

        List<String> hong = new ArrayList<>();
        int soMuc = 0;
        for (String tho : rutDuongDan(layout)) {
            if (tho.isEmpty() || laTaiNguyenTinh(tho)) {
                continue;
            }
            soMuc++;
            if (!mauThat.contains(chuanHoa(tho))) {
                hong.add(tho);
            }
        }

        assertThat(soMuc)
                .as("Sidebar phải có ít nhất 10 mục; đếm ra %d nghĩa là biểu thức tìm kiếm sai",
                        soMuc)
                .isGreaterThanOrEqualTo(10);
        assertThat(hong)
                .as("Mục sidebar trỏ vào hư không:%n  %s", String.join("\n  ", hong))
                .isEmpty();
    }

    /**
     * Từ khoá của Thymeleaf, <b>không</b> được dùng làm tên biến lặp trong {@code th:each}.
     *
     * <p>Ngôn ngữ biểu thức của Thymeleaf có các toán tử dạng chữ: {@code gt} (lớn hơn),
     * {@code lt}, {@code ge}, {@code le}, {@code eq}, {@code ne}, {@code and}, {@code or},
     * {@code not}, {@code div}, {@code mod}.</p>
     */
    private static final Set<String> TU_KHOA_THYMELEAF = Set.of(
            "gt", "lt", "ge", "le", "eq", "ne", "and", "or", "not", "div", "mod",
            "true", "false", "null");

    private static final Pattern BIEN_LAP = Pattern.compile(
            "th:each\\s*=\\s*\"\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*[,:]");

    /**
     * ⭐ Phép kiểm này bắt được một lỗi đã sống qua HAI phase mà không ai thấy.
     *
     * <p>Màn hình {@code /giam-tru} dựng ở Phase 5 dùng {@code th:each="gt : ${danhSach}"} —
     * và {@code gt} chính là toán tử <i>lớn hơn</i> của Thymeleaf. Bộ phân tích biểu thức đọc
     * {@code gt} thành toán tử chứ không thành tên biến, trả về {@code null}, rồi ném
     * {@code IllegalArgumentException: Iteration variable cannot be null}. Trang
     * <b>chưa bao giờ hiển thị được</b>: nó trả HTTP 500 ngay từ lần đầu.</p>
     *
     * <p>Vì sao không ai phát hiện: mục E của Phase 5 ghi *"✅ /giam-tru"* vào bảng nghiệm thu
     * mà chưa từng mở trang đó lên, và không phép kiểm tự động nào chạm tới nó. Lỗi chỉ lộ ra ở
     * Phase 7 khi có một script <b>đi theo menu</b> thay vì gõ URL.</p>
     *
     * <p>Đây là loại lỗi mà biên dịch không bắt, kiểu tĩnh không bắt, và mắt người đọc code
     * cũng không bắt — vì {@code gt} trông y hệt một tên viết tắt hợp lý của "giảm trừ".</p>
     */
    @Test
    @DisplayName("5. ⭐ Không biến lặp nào trùng tên toán tử của Thymeleaf")
    void khongBienLapTrungTenToanTu() {
        List<String> trung = new ArrayList<>();
        int soVongLap = 0;

        for (Path template : moiTemplate()) {
            Matcher m = BIEN_LAP.matcher(docFile(template));
            while (m.find()) {
                soVongLap++;
                String bien = m.group(1);
                if (TU_KHOA_THYMELEAF.contains(bien)) {
                    trung.add(THU_MUC_TEMPLATE.relativize(template)
                            + " → th:each=\"" + bien + " : ...\"");
                }
            }
        }

        assertThat(soVongLap)
                .as("Phải quét được vòng lặp; đếm ra %d nghĩa là biểu thức tìm kiếm sai",
                        soVongLap)
                .isGreaterThan(20);
        assertThat(trung)
                .as("Tên biến lặp trùng toán tử của Thymeleaf. Trang sẽ ném "
                        + "\"Iteration variable cannot be null\" và trả HTTP 500 — không phải "
                        + "lúc biên dịch mà đúng lúc người dùng mở trang lên:%n  %s",
                        String.join("\n  ", trung))
                .isEmpty();
    }
}
