package com.hanzo.billing.repository;

import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.enums.TrangThaiDangKyGoi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DangKyGoiCuocRepository extends JpaRepository<DangKyGoiCuoc, Long> {

    List<DangKyGoiCuoc> findByThueBaoId(Long thueBaoId);

    /** Bản ghi gói cước đang có hiệu lực của một thuê bao. */
    Optional<DangKyGoiCuoc> findFirstByThueBaoIdAndTrangThai(Long thueBaoId, TrangThaiDangKyGoi trangThai);

    long countByTrangThai(TrangThaiDangKyGoi trangThai);
}
