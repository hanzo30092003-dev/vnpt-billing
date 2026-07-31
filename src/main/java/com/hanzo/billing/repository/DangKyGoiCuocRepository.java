package com.hanzo.billing.repository;

import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.enums.TrangThaiDangKyGoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DangKyGoiCuocRepository extends JpaRepository<DangKyGoiCuoc, Long> {

    List<DangKyGoiCuoc> findByThueBaoId(Long thueBaoId);

    /** Bản ghi gói cước đang có hiệu lực của một thuê bao. */
    Optional<DangKyGoiCuoc> findFirstByThueBaoIdAndTrangThai(Long thueBaoId, TrangThaiDangKyGoi trangThai);

    long countByTrangThai(TrangThaiDangKyGoi trangThai);

    /** Lịch sử gói cước, mới nhất trước, nạp sẵn gói để hiển thị tên gói. */
    @Query("""
            SELECT dk FROM DangKyGoiCuoc dk
            JOIN FETCH dk.goiCuoc
            WHERE dk.thueBao.id = :thueBaoId
            ORDER BY dk.ngayBatDau DESC, dk.id DESC
            """)
    List<DangKyGoiCuoc> timLichSuTheoThueBao(@Param("thueBaoId") Long thueBaoId);
}
