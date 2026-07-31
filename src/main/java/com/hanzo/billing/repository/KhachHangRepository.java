package com.hanzo.billing.repository;

import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.enums.LoaiKhachHang;
import com.hanzo.billing.enums.TrangThaiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    Optional<KhachHang> findByMaKh(String maKh);

    boolean existsByMaKh(String maKh);

    List<KhachHang> findBySoGiayTo(String soGiayTo);

    List<KhachHang> findByTenKhContainingIgnoreCase(String tenKh);

    List<KhachHang> findByLoaiKh(LoaiKhachHang loaiKh);

    long countByLoaiKh(LoaiKhachHang loaiKh);

    long countByTrangThai(TrangThaiKhachHang trangThai);
}
