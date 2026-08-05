package com.hanzo.billing.repository;

import com.hanzo.billing.entity.BienDongSoDu;
import com.hanzo.billing.enums.LoaiBienDongSoDu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BienDongSoDuRepository extends JpaRepository<BienDongSoDu, Long> {

    /** Sổ cái của một thuê bao, mới nhất trước, kèm người thực hiện (có thể null). */
    @Query("""
            SELECT b FROM BienDongSoDu b
            LEFT JOIN FETCH b.nguoiThucHien
            LEFT JOIN FETCH b.kyCuoc
            WHERE b.thueBao.id = :thueBaoId
            ORDER BY b.ngayGhiNhan DESC, b.id DESC
            """)
    List<BienDongSoDu> timLichSuTheoThueBao(@Param("thueBaoId") Long thueBaoId);

    /** Các dòng trừ cước của một kỳ — dùng để chống trừ chồng và để hoàn tác. */
    List<BienDongSoDu> findByKyCuocIdAndLoaiBienDong(Long kyCuocId, LoaiBienDongSoDu loaiBienDong);

    long countByKyCuocIdAndLoaiBienDong(Long kyCuocId, LoaiBienDongSoDu loaiBienDong);
}
