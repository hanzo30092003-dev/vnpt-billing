package com.hanzo.billing.repository;

import com.hanzo.billing.entity.NapTien;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NapTienRepository extends JpaRepository<NapTien, Long> {

    List<NapTien> findByThueBaoIdOrderByNgayNapDesc(Long thueBaoId);

    /** Lịch sử nạp tiền, mới nhất trước, kèm người thực hiện (có thể null). */
    @Query("""
            SELECT nt FROM NapTien nt
            LEFT JOIN FETCH nt.nguoiThucHien
            WHERE nt.thueBao.id = :thueBaoId
            ORDER BY nt.ngayNap DESC, nt.id DESC
            """)
    List<NapTien> timLichSuTheoThueBao(@Param("thueBaoId") Long thueBaoId);
}
