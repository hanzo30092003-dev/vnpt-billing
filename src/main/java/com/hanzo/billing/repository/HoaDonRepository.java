package com.hanzo.billing.repository;

import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {

    Optional<HoaDon> findByMaHoaDon(String maHoaDon);

    /** Kiểm tra trước khi lập hóa đơn để tránh trùng kỳ cho cùng thuê bao. */
    Optional<HoaDon> findByThueBaoIdAndKyCuocId(Long thueBaoId, Long kyCuocId);

    boolean existsByThueBaoIdAndKyCuocId(Long thueBaoId, Long kyCuocId);

    List<HoaDon> findByKhachHangId(Long khachHangId);

    List<HoaDon> findByKyCuocId(Long kyCuocId);

    List<HoaDon> findByTrangThai(TrangThaiHoaDon trangThai);

    long countByKyCuocId(Long kyCuocId);
}
