package com.hanzo.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ BẤT BIẾN THỨ TỰ THUỘC TÍNH — không thẻ nào mang đồng thời một thuộc tính điều kiện và
 * một thuộc tính chèn mảnh.
 *
 * <h2>Vì sao có test này</h2>
 * <p>Thymeleaf xử lý các thuộc tính trên <b>cùng một thẻ</b> theo thứ tự ưu tiên cố định:
 * {@code th:insert}/{@code th:replace} (100) chạy <b>trước</b> {@code th:each} (200) và
 * {@code th:if}/{@code th:unless}/{@code sec:authorize} (300). Với {@code th:replace}, cả
 * thẻ đã bị thay bằng mảnh ngay ở bước 100, nên {@code th:if} đứng cạnh nó <b>không bao giờ
 * được đánh giá</b> — mảnh luôn hiện, bất kể điều kiện.</p>
 *
 * <p>Sáu màn hình báo cáo (doanh thu theo kỳ, theo gói, theo dịch vụ, sản lượng, thuê bao,
 * top thuê bao) viết đúng kiểu đó cho khối "chưa có dữ liệu": khối này hiện <b>ngay cả khi
 * bảng bên dưới đầy số</b>. Lỗi sống qua ba phase và 55 ảnh chụp, vì nó không ném lỗi, không
 * làm test nào đỏ, và mắt người đọc thấy {@code th:if} thì tin là có điều kiện.</p>
 *
 * <p>Cách viết đúng dùng chung cho cả dự án: đặt điều kiện lên một {@code <th:block>} bọc
 * ngoài, {@code th:replace} nằm ở thẻ con. {@code th:block} không để lại thẻ nào trong HTML.</p>
 *
 * <p>Test này là <b>bất biến tổng quát</b>: quét mọi thẻ của mọi template, không chỉ sáu chỗ
 * đã biết. Không cần Spring hay MySQL.</p>
 */
@DisplayName("Bất biến thứ tự thuộc tính Thymeleaf")
class KiemTraUuTienThuocTinhTest {

    private static final Path THU_MUC_TEMPLATE = Path.of("src/main/resources/templates");

    /**
     * Một thẻ mở, kể cả khi trải nhiều dòng. Giá trị thuộc tính trong ngoặc kép có thể chứa
     * {@code >} (ví dụ {@code th:text="${a > b}"}), nên không dùng {@code <[^>]*>} — mẫu đó
     * cắt thẻ ở giữa và bỏ sót thuộc tính đứng sau.
     */
    private static final Pattern THE_MO = Pattern.compile(
            "<[a-zA-Z][^<>\"']*(?:\"[^\"]*\"[^<>\"']*|'[^']*'[^<>\"']*)*>");

    /** Thuộc tính có ưu tiên 200–300: chạy SAU thuộc tính chèn mảnh, nên bị vô hiệu. */
    private static final Pattern DIEU_KIEN = Pattern.compile(
            "\\s(th:if|th:unless|th:each|sec:authorize)\\s*=");

    /** Thuộc tính chèn mảnh, ưu tiên 100 — chạy trước mọi thứ khác trên cùng thẻ. */
    private static final Pattern CHEN_MANH = Pattern.compile("\\s(th:replace|th:insert)\\s*=");

    private static final Pattern BINH_LUAN = Pattern.compile("<!--.*?-->", Pattern.DOTALL);

    private static List<Path> moiTemplate() {
        try (Stream<Path> duyet = Files.walk(THU_MUC_TEMPLATE)) {
            return duyet.filter(p -> p.toString().endsWith(".html")).sorted().toList();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không đọc được thư mục template", ex);
        }
    }

    /**
     * Đọc template, xoá bình luận nhưng <b>giữ nguyên số dòng</b> để thông báo lỗi chỉ đúng
     * chỗ. Một thẻ nằm trong bình luận không phải là thẻ — đây là chỗ báo động giả kinh điển
     * đã gặp ở {@link KiemTraDieuHuongTest}.
     */
    private static String docFile(Path p) {
        try {
            String noiDung = Files.readString(p, StandardCharsets.UTF_8);
            Matcher m = BINH_LUAN.matcher(noiDung);
            StringBuilder sb = new StringBuilder();
            while (m.find()) {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group().replaceAll("[^\n]", " ")));
            }
            m.appendTail(sb);
            return sb.toString();
        } catch (IOException ex) {
            throw new UncheckedIOException("Không đọc được " + p, ex);
        }
    }

    @Test
    @DisplayName("⭐ Không thẻ nào mang đồng thời th:if/th:unless/th:each/sec:authorize và th:replace/th:insert")
    void khongTheNaoMangDongThoiDieuKienVaChenManh() {
        List<String> viPham = new ArrayList<>();
        int soThe = 0;

        for (Path template : moiTemplate()) {
            String noiDung = docFile(template);
            Matcher the = THE_MO.matcher(noiDung);
            while (the.find()) {
                soThe++;
                Matcher dk = DIEU_KIEN.matcher(the.group());
                Matcher cm = CHEN_MANH.matcher(the.group());
                if (dk.find() && cm.find()) {
                    int dong = 1 + (int) noiDung.chars().limit(the.start()).filter(c -> c == '\n').count();
                    viPham.add(THU_MUC_TEMPLATE.relativize(template) + ":" + dong
                            + "  " + dk.group(1) + " cùng thẻ với " + cm.group(1));
                }
            }
        }

        assertThat(soThe)
                .as("Phải quét được hàng nghìn thẻ; đếm ra %d nghĩa là biểu thức nhận diện thẻ "
                        + "sai và test này xanh mà chẳng kiểm gì (bài học 43.5)", soThe)
                .isGreaterThan(2000);
        assertThat(viPham)
                .as("Đã quét %d thẻ trong %d template. Những thẻ dưới đây đặt điều kiện cùng "
                        + "thẻ với th:replace/th:insert — Thymeleaf chạy phần chèn mảnh TRƯỚC, "
                        + "nên điều kiện bị bỏ qua và mảnh luôn hiện. Bọc điều kiện lên một "
                        + "<th:block> ngoài, để th:replace ở thẻ con:%n  %s",
                        soThe, moiTemplate().size(), String.join("\n  ", viPham))
                .isEmpty();
    }
}
