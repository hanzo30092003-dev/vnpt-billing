package com.hanzo.billing.util;

import com.hanzo.billing.security.NguoiDungPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/** Tiện ích lấy thông tin người dùng đang đăng nhập từ SecurityContext. */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * Trả về người dùng đang đăng nhập, hoặc {@code Optional.empty()} nếu chưa
     * đăng nhập (ví dụ khi tác vụ chạy nền, không nằm trong một request nào).
     */
    public static Optional<NguoiDungPrincipal> layNguoiDungHienTai() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return Optional.empty();
        }
        if (auth.getPrincipal() instanceof NguoiDungPrincipal principal) {
            return Optional.of(principal);
        }
        return Optional.empty();
    }
}
