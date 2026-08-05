package com.hanzo.billing.repository;

import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /**
     * Mã hóa đơn lớn nhất trong một kỳ, tìm theo tiền tố {@code HDyyyyMM-}.
     *
     * @return null nếu kỳ chưa có hóa đơn nào
     */
    @Query("SELECT MAX(h.maHoaDon) FROM HoaDon h WHERE h.maHoaDon LIKE CONCAT(:tienTo, '%')")
    String timMaHoaDonLonNhatTheoTienTo(@Param("tienTo") String tienTo);

    /** Hóa đơn của một kỳ kèm thuê bao và khách hàng, để hiển thị danh sách. */
    @Query("""
            SELECT h FROM HoaDon h
            JOIN FETCH h.thueBao
            JOIN FETCH h.khachHang
            WHERE h.kyCuoc.id = :kyCuocId
            ORDER BY h.maHoaDon
            """)
    List<HoaDon> timTheoKyKemQuanHe(@Param("kyCuocId") Long kyCuocId);

    /** Tổng doanh thu của kỳ, tính lại từ hóa đơn thực tế thay vì cộng dồn khi chạy. */
    @Query("SELECT COALESCE(SUM(h.tongThanhToan), 0) FROM HoaDon h WHERE h.kyCuoc.id = :kyCuocId")
    BigDecimal tinhTongDoanhThuTheoKy(@Param("kyCuocId") Long kyCuocId);

    /**
     * Xóa chi tiết hóa đơn của cả kỳ.
     *
     * <p>Phải gọi <b>trước</b> khi xóa hóa đơn: CSDL có {@code ON DELETE CASCADE} nhưng
     * xóa bằng JPQL không kích hoạt cascade của CSDL lẫn của JPA, nên bỏ bước này sẽ để
     * lại chi tiết mồ côi hoặc vi phạm khóa ngoại.</p>
     */
    @Modifying
    @Query("""
            DELETE FROM ChiTietHoaDon ct
             WHERE ct.hoaDon.id IN (SELECT h.id FROM HoaDon h WHERE h.kyCuoc.id = :kyCuocId)
            """)
    int xoaChiTietTheoKy(@Param("kyCuocId") Long kyCuocId);

    @Modifying
    @Query("DELETE FROM HoaDon h WHERE h.kyCuoc.id = :kyCuocId")
    int xoaTheoKy(@Param("kyCuocId") Long kyCuocId);
}
