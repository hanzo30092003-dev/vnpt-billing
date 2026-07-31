package com.hanzo.billing.repository;

import com.hanzo.billing.entity.LichSuThueBao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LichSuThueBaoRepository extends JpaRepository<LichSuThueBao, Long> {

    List<LichSuThueBao> findByThueBaoIdOrderByThoiGianDesc(Long thueBaoId);

    /**
     * Lịch sử biến động, mới nhất trước.
     *
     * <p>Dùng {@code LEFT JOIN FETCH} vì {@code nguoiThucHien} cho phép null
     * (thao tác do hệ thống tự sinh); dùng {@code JOIN FETCH} thường sẽ loại mất
     * các dòng đó khỏi kết quả.</p>
     */
    @Query("""
            SELECT ls FROM LichSuThueBao ls
            LEFT JOIN FETCH ls.nguoiThucHien
            WHERE ls.thueBao.id = :thueBaoId
            ORDER BY ls.thoiGian DESC, ls.id DESC
            """)
    List<LichSuThueBao> timLichSuTheoThueBao(@Param("thueBaoId") Long thueBaoId);
}
