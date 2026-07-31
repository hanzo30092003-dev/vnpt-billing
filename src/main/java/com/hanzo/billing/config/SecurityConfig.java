package com.hanzo.billing.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Cấu hình bảo mật cho Phase 0.
 *
 * <p><b>Lưu ý:</b> giai đoạn này MỞ TOÀN BỘ (permitAll) để tiện dựng giao diện,
 * đồng thời tắt form đăng nhập mặc định của Spring Security. Phase 2 sẽ thay
 * bằng đăng nhập thật có phân quyền theo vai trò người dùng.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Phase 0: mọi đường dẫn đều truy cập tự do, chưa yêu cầu đăng nhập
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                // Chưa có form nghiệp vụ nên tạm tắt CSRF và các cơ chế đăng nhập
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
