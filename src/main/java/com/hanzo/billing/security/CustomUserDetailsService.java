package com.hanzo.billing.security;

import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.repository.NguoiDungRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nạp thông tin đăng nhập từ bảng {@code nguoi_dung}.
 *
 * <p>Thay cho tài khoản in-memory mà Spring Boot tự sinh ở Phase 0.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final NguoiDungRepository nguoiDungRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String tenDangNhap) throws UsernameNotFoundException {
        NguoiDung nguoiDung = nguoiDungRepository.findByTenDangNhap(tenDangNhap)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Không tìm thấy tài khoản: " + tenDangNhap));

        // Tài khoản bị khoá (trang_thai = 0) được xử lý trong NguoiDungPrincipal.isEnabled(),
        // Spring Security sẽ từ chối đăng nhập thay vì phải kiểm tra thủ công ở đây.
        return new NguoiDungPrincipal(nguoiDung);
    }
}
