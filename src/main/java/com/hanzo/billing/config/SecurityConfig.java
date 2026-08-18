package com.hanzo.billing.config;

import com.hanzo.billing.security.XuLyDangNhap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;

/**
 * Cấu hình bảo mật thật của hệ thống (thay cấu hình tạm {@code permitAll} ở Phase 0).
 *
 * <p><b>Phân quyền theo đường dẫn.</b> Ba vai trò tương ứng ba nhóm nghiệp vụ:</p>
 * <ul>
 *   <li>{@code NHAN_VIEN} — tiếp nhận khách hàng và thuê bao tại quầy giao dịch</li>
 *   <li>{@code KE_TOAN} — hóa đơn, thu tiền, theo dõi công nợ</li>
 *   <li>{@code ADMIN} — toàn quyền, gồm cả danh mục gói cước, bảng giá và tính cước</li>
 * </ul>
 *
 * <p><b>CSRF được bật lại</b> (Phase 0 đã tắt vì chưa có form). Thymeleaf tự chèn
 * token ẩn vào mọi thẻ {@code <form th:action=...>}, nên không phải làm gì thêm ở view.
 * Hệ quả: đăng xuất phải gửi bằng POST chứ không thể dùng liên kết GET.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final XuLyDangNhap xuLyDangNhap;

    public SecurityConfig(XuLyDangNhap xuLyDangNhap) {
        this.xuLyDangNhap = xuLyDangNhap;
    }

    /** Thuật toán băm mật khẩu; khớp với hash BCrypt trong bảng {@code nguoi_dung}. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Sổ ghi các phiên đang mở, tra được theo người dùng.
     *
     * <p><b>Vì sao phải khai tường minh.</b> {@code maximumSessions(1)} vẫn chạy được với một
     * sổ nội bộ mà Spring tự dựng, nhưng sổ đó không ai lấy ra được. Màn hình quản lý người
     * dùng cần lấy ra: khoá một tài khoản mà <b>không đá phiên đang mở của họ</b> thì nút
     * "Khoá" chỉ có tác dụng từ lần đăng nhập sau — người đang mở máy vẫn thao tác bình
     * thường, có thể tới hết ca. Đó là khoá trên giấy.</p>
     */
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    /**
     * Chuyển sự kiện tạo/huỷ phiên của Tomcat sang Spring Security.
     *
     * <p>Thiếu bean này thì {@link SessionRegistry} chỉ có đường ghi vào mà không có đường xoá
     * ra: phiên đã đăng xuất hoặc đã hết hạn vẫn nằm lại trong sổ mãi mãi, vừa phình bộ nhớ
     * vừa làm việc tra "tài khoản này đang mở phiên nào" trả về cả những phiên đã chết.</p>
     */
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SessionRegistry sessionRegistry)
            throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Tài nguyên tĩnh và trang đăng nhập: công khai
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/dang-nhap").permitAll()

                        // Quầy giao dịch
                        .requestMatchers("/khach-hang/**", "/thue-bao/**")
                        .hasAnyRole("NHAN_VIEN", "ADMIN")

                        // Kế toán
                        .requestMatchers("/hoa-don/**", "/thanh-toan/**", "/cong-no/**",
                                "/giam-tru/**")
                        .hasAnyRole("KE_TOAN", "ADMIN")

                        // Danh mục và nghiệp vụ tính cước: chỉ quản trị viên
                        .requestMatchers("/quan-tri/**", "/goi-cuoc/**", "/bang-gia/**",
                                "/tinh-cuoc/**", "/cdr/**", "/ky-cuoc/**")
                        .hasRole("ADMIN")

                        // =========================================================
                        // Báo cáo — chia theo NỘI DUNG, không mở cả cụm
                        // =========================================================
                        // Bản trước để nguyên cụm `/bao-cao/**` ở mức `authenticated()`
                        // với lý do "báo cáo thì ai cũng xem được". Hệ quả không ai
                        // lường: NHAN_VIEN bị 403 ở /cong-no và /hoa-don, nhưng mở
                        // /bao-cao/cong-no thì thấy đủ tên khách hàng và số tiền nợ,
                        // lại còn tải được cả file Excel công nợ. Hệ thống khoá một
                        // cửa và để mở cửa ngay bên cạnh.
                        //
                        // Nay luật đi theo DỮ LIỆU chứ không theo tiền tố đường dẫn:
                        // báo cáo nào có số tiền của khách thì cùng luật với /hoa-don
                        // và /cong-no.
                        .requestMatchers("/bao-cao/cong-no/**", "/bao-cao/top-thue-bao/**",
                                "/bao-cao/doanh-thu-ky/**", "/bao-cao/doanh-thu-goi-cuoc/**",
                                "/bao-cao/doanh-thu-dich-vu/**")
                        .hasAnyRole("KE_TOAN", "ADMIN")

                        // Hai báo cáo còn lại không chứa số tiền của khách hàng nào —
                        // thống kê thuê bao và sản lượng dịch vụ — nên mọi vai trò
                        // đã đăng nhập đều xem được. Đây là thứ nhân viên quầy dùng thật.
                        .requestMatchers("/bao-cao/**").authenticated()

                        // Còn lại (trang chủ, trang 403 ...) chỉ cần đã đăng nhập
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        // Hai handler này thay cho defaultSuccessUrl/failureUrl: chúng
                        // vẫn luôn đưa về trang chủ y hệt, nhưng còn đếm số lần nhập
                        // sai và khoá tạm tài khoản. Xem XuLyDangNhap.
                        .successHandler(xuLyDangNhap)
                        .failureHandler(xuLyDangNhap)
                        .permitAll())

                .logout(logout -> logout
                        .logoutUrl("/dang-xuat")
                        .logoutSuccessUrl("/dang-nhap?dathoat")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())

                // =========================================================
                // Tiêu đề bảo mật HTTP
                // =========================================================
                // Trước đây không khai gì nên chỉ có mặc định tối thiểu của
                // Spring Security. Bốn tiêu đề dưới đây đều rẻ và đều chặn được
                // một lớp tấn công cụ thể.
                .headers(h -> h
                        // Chặn nhúng trang này vào <iframe> của site khác —
                        // chống clickjacking: kẻ tấn công phủ một trang trong
                        // suốt lên nút "Chốt kỳ" rồi dụ người dùng bấm.
                        .frameOptions(f -> f.deny())

                        // Không gửi kèm đường dẫn đầy đủ khi người dùng bấm sang
                        // site khác. Đường dẫn ở đây có id khách hàng, id hóa đơn.
                        .referrerPolicy(r -> r.policy(
                                org.springframework.security.web.header.writers
                                        .ReferrerPolicyHeaderWriter.ReferrerPolicy
                                        .STRICT_ORIGIN_WHEN_CROSS_ORIGIN))

                        // Trình duyệt không được tự đoán kiểu nội dung. Thiếu nó,
                        // một file tải lên có thể bị đoán thành HTML rồi chạy.
                        .contentTypeOptions(c -> {})

                        // ⚠️ CSP có 'unsafe-inline'. Đó là NHƯỢNG BỘ CÓ THẬT, không
                        // phải sơ suất: nhiều template có <script> và style="..."
                        // viết thẳng trong trang, siết hẳn sẽ làm vỡ biểu đồ và
                        // phần đổi form động. Dù vậy vẫn hơn không có CSP — nó
                        // chặn được script nạp từ tên miền lạ, tức chặn đúng
                        // đường mà một lỗ XSS thường dùng để lấy dữ liệu ra ngoài.
                        // Muốn bỏ 'unsafe-inline' thì phải chuyển hết script nội
                        // tuyến ra file riêng hoặc gắn nonce — việc của đợt sau.
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'self'; "
                                        + "script-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; "
                                        + "style-src 'self' https://cdn.jsdelivr.net 'unsafe-inline'; "
                                        + "font-src 'self' https://cdn.jsdelivr.net data:; "
                                        + "img-src 'self' data:; "
                                        + "form-action 'self'; "
                                        + "frame-ancestors 'none'; "
                                        + "base-uri 'self'")))

                // =========================================================
                // Quản lý phiên
                // =========================================================
                .sessionManagement(sm -> sm
                        // Cấp id phiên MỚI sau khi đăng nhập thành công. Không có
                        // dòng này thì id phiên trước lúc đăng nhập vẫn dùng tiếp,
                        // và ai biết được id đó sẽ dùng ké phiên đã đăng nhập
                        // (session fixation).
                        .sessionFixation(sf -> sf.newSession())
                        // Một tài khoản chỉ giữ MỘT phiên. Người đăng nhập sau đẩy
                        // người trước ra. Với phần mềm quầy giao dịch đây là điều
                        // đúng: hai người dùng chung một tài khoản thì không còn
                        // truy được ai đã làm gì trong sổ nhật ký.
                        .maximumSessions(1)
                        // Dùng sổ phiên khai ở trên thay cho sổ nội bộ, để màn hình
                        // quản lý người dùng đá được phiên của tài khoản vừa bị khoá.
                        .sessionRegistry(sessionRegistry)
                        .expiredUrl("/dang-nhap?hethan"))

                // Đã đăng nhập nhưng không đủ quyền -> trang 403 thân thiện
                .exceptionHandling(ex -> ex.accessDeniedPage("/403"));

        return http.build();
    }
}
