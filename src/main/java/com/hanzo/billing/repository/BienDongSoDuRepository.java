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

    /**
     * Đếm dòng trừ cước của các kỳ <b>muộn hơn</b> kỳ đang xét.
     *
     * <p>Trừ cước <b>không giao hoán</b>: chạy kỳ 5 sau khi đã chạy kỳ 6 cho ra kết quả khác
     * hẳn chạy đúng thứ tự thời gian, vì số dư vào mỗi kỳ phụ thuộc kỳ trước đó. Truy vấn này
     * là căn cứ để chặn ở tầng nghiệp vụ.</p>
     */
    @Query("""
            SELECT COUNT(b) FROM BienDongSoDu b
            WHERE b.loaiBienDong = com.hanzo.billing.enums.LoaiBienDongSoDu.TRU_CUOC
              AND (b.kyCuoc.nam > :nam
                   OR (b.kyCuoc.nam = :nam AND b.kyCuoc.thang > :thang))
            """)
    long demTruCuocCuaKyMuonHon(@Param("nam") int nam, @Param("thang") int thang);
}
