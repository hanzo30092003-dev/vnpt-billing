package com.hanzo.billing.repository;

import com.hanzo.billing.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ThanhToanRepository extends JpaRepository<ThanhToan, Long> {

    Optional<ThanhToan> findByMaGiaoDich(String maGiaoDich);

    List<ThanhToan> findByHoaDonId(Long hoaDonId);

    List<ThanhToan> findByNgayThanhToanBetween(LocalDateTime tuLuc, LocalDateTime denLuc);

    /**
     * Số giao dịch thanh toán đã ghi nhận trên các hóa đơn của một kỳ.
     *
     * <p>Chốt chặn của chức năng hủy lập hóa đơn: đã thu tiền của khách thì không được
     * xóa hóa đơn, vì bản ghi thanh toán sẽ trỏ vào một hóa đơn không còn tồn tại.</p>
     */
    @Query("SELECT COUNT(t) FROM ThanhToan t WHERE t.hoaDon.kyCuoc.id = :kyCuocId")
    long demTheoKyCuoc(@Param("kyCuocId") Long kyCuocId);
}
