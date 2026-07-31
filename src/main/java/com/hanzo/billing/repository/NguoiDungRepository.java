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
}
