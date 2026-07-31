package com.hanzo.billing.security;

import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.enums.VaiTro;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Đối tượng người dùng đã đăng nhập mà Spring Security giữ trong SecurityContext.
 *
 * <p>Bọc quanh entity {@link NguoiDung} thay vì dùng lớp {@code User} mặc định, để
 * giao diện lấy được họ tên và vai trò hiển thị mà không phải truy vấn lại CSDL ở
 * mỗi lần vẽ header.</p>
 */
public class NguoiDungPrincipal implements UserDetails {

    private final NguoiDung nguoiDung;

    public NguoiDungPrincipal(NguoiDung nguoiDung) {
        this.nguoiDung = nguoiDung;
    }

    /**
     * Spring Security quy ước quyền dạng vai trò phải có tiền tố {@code ROLE_}
     * thì biểu thức {@code hasRole("ADMIN")} mới khớp. Vì vậy ghép thêm tiền tố
     * vào tên hằng của enum {@link VaiTro}.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + nguoiDung.getVaiTro().name()));
    }

    @Override
    public String getPassword() {
        return nguoiDung.getMatKhau();
    }

    @Override
    public String getUsername() {
        return nguoiDung.getTenDangNhap();
    }

    /** Tài khoản có {@code trang_thai = 0} bị coi là đã khoá, không đăng nhập được. */
    @Override
    public boolean isEnabled() {
        return Boolean.TRUE.equals(nguoiDung.getTrangThai());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ----- Thông tin bổ sung cho tầng giao diện và ghi nhật ký -----

    public Long getId() {
        return nguoiDung.getId();
    }

    public String getHoTen() {
        return nguoiDung.getHoTen();
    }

    public VaiTro getVaiTro() {
        return nguoiDung.getVaiTro();
    }

    public NguoiDung getNguoiDung() {
        return nguoiDung;
    }
}
