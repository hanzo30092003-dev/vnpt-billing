package com.hanzo.billing.repository;

import com.hanzo.billing.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {

    Optional<ThanhToan> findByMaGiaoDich(String maGiaoDich);

    List<ThanhToan> findByHoaDonId(Long hoaDonId);

    List<ThanhToan> findByNgayThanhToanBetween(LocalDateTime tuLuc, LocalDateTime denLuc);
}
