package com.hanzo.billing.repository;

import com.hanzo.billing.entity.GiamTru;
import com.hanzo.billing.enums.TrangThaiGiamTru;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GiamTruRepository extends JpaRepository<GiamTru, Long> {

    List<GiamTru> findByThueBaoId(Long thueBaoId);

    List<GiamTru> findByThueBaoIdAndKyCuocId(Long thueBaoId, Long kyCuocId);

    List<GiamTru> findByTrangThai(TrangThaiGiamTru trangThai);

    /**
     * Khoản giảm trừ áp dụng cho một kỳ: gồm khoản gắn đúng kỳ và khoản dùng chung
     * ({@code ky_cuoc_id} null), và chỉ lấy khoản <b>chưa</b> áp dụng.
     *
     * <p>Bảng này hiện còn rỗng; đường xử lý viết sẵn để mục 4E chỉ việc nhập liệu.</p>
     */
    @Query("""
            SELECT gt FROM GiamTru gt
            WHERE (gt.kyCuoc.id = :kyCuocId OR gt.kyCuoc IS NULL)
              AND (gt.trangThai IS NULL
                   OR gt.trangThai = com.hanzo.billing.enums.TrangThaiGiamTru.CHUA_AP_DUNG)
            """)
    List<GiamTru> timApDungChoKy(@Param("kyCuocId") Long kyCuocId);

    /** Trả các khoản giảm trừ của một kỳ về chưa áp dụng, khi hủy lập hóa đơn. */
    @Modifying
    @Query("""
            UPDATE GiamTru gt
               SET gt.trangThai = com.hanzo.billing.enums.TrangThaiGiamTru.CHUA_AP_DUNG
             WHERE gt.kyCuoc.id = :kyCuocId
            """)
    int datLaiChuaApDungTheoKy(@Param("kyCuocId") Long kyCuocId);
}
