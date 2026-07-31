package com.hanzo.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

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

    /** Thuật toán băm mật khẩu; khớp với hash BCrypt trong bảng {@code nguoi_dung}. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Tài nguyên tĩnh và trang đăng nhập: công khai
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                        .requestMatchers("/dang-nhap").permitAll()

                        // Quầy giao dịch
                        .requestMatchers("/khach-hang/**", "/thue-bao/**")
                        .hasAnyRole("NHAN_VIEN", "ADMIN")

                        // Kế toán
                        .requestMatchers("/hoa-don/**", "/thanh-toan/**", "/cong-no/**")
                        .hasAnyRole("KE_TOAN", "ADMIN")

                        // Danh mục và nghiệp vụ tính cước: chỉ quản trị viên
                        .requestMatchers("/quan-tri/**", "/goi-cuoc/**", "/bang-gia/**",
                                "/tinh-cuoc/**", "/cdr/**")
                        .hasRole("ADMIN")

                        // Báo cáo: mọi vai trò đã đăng nhập đều xem được
                        .requestMatchers("/bao-cao/**").authenticated()

                        // Còn lại (trang chủ, trang 403 ...) chỉ cần đã đăng nhập
                        .anyRequest().authenticated())

                .formLogin(form -> form
                        .loginPage("/dang-nhap")
                        .loginProcessingUrl("/dang-nhap")
                        .usernameParameter("tenDangNhap")
                        .passwordParameter("matKhau")
                        // true = luôn về trang chủ, không nhảy tới URL người dùng gõ dở
                        .defaultSuccessUrl("/", true)
                        .failureUrl("/dang-nhap?loi")
                        .permitAll())

                .logout(logout -> logout
                        .logoutUrl("/dang-xuat")
                        .logoutSuccessUrl("/dang-nhap?dathoat")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll())

                // Đã đăng nhập nhưng không đủ quyền -> trang 403 thân thiện
                .exceptionHandling(ex -> ex.accessDeniedPage("/403"));

        return http.build();
    }
}
