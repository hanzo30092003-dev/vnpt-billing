package com.hanzo.billing.repository;

import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.enums.VaiTro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NguoiDungRepository extends JpaRepository<NguoiDung, Long> {

    Optional<NguoiDung> findByTenDangNhap(String tenDangNhap);

    boolean existsByTenDangNhap(String tenDangNhap);

    List<NguoiDung> findByVaiTro(VaiTro vaiTro);

    /**
     * Đếm tài khoản theo vai trò và trạng thái — dùng để canh bất biến "luôn còn ít nhất
     * một quản trị viên đang hoạt động" trước khi khoá hoặc hạ quyền ai đó.
     */
    long countByVaiTroAndTrangThai(VaiTro vaiTro, Boolean trangThai);
}
