package com.hanzo.billing.repository;

import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ChiTietSuDungRepository extends JpaRepository<ChiTietSuDung, Long> {

    List<ChiTietSuDung> findByThueBaoId(Long thueBaoId);

    List<ChiTietSuDung> findByTrangThaiTinhCuoc(TrangThaiTinhCuoc trangThaiTinhCuoc);

    /** CDR của một thuê bao trong khoảng thời gian, dùng khi tính cước theo kỳ. */
    List<ChiTietSuDung> findByThueBaoIdAndThoiGianBatDauBetween(
            Long thueBaoId, LocalDateTime tuLuc, LocalDateTime denLuc);

    long countByKyCuocId(Long kyCuocId);

    long countByTrangThaiTinhCuoc(TrangThaiTinhCuoc trangThaiTinhCuoc);
}
