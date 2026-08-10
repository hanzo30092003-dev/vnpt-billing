package com.hanzo.billing.repository;

import com.hanzo.billing.entity.ThanhToan;
import com.hanzo.billing.enums.HinhThucThanhToan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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

    /** Lịch sử thanh toán của một hóa đơn, cũ nhất trước, kèm người thu. */
    @Query("""
            SELECT t FROM ThanhToan t
            LEFT JOIN FETCH t.nguoiThu
            WHERE t.hoaDon.id = :hoaDonId
            ORDER BY t.ngayThanhToan ASC, t.id ASC
            """)
    List<ThanhToan> timTheoHoaDonKemNguoiThu(@Param("hoaDonId") Long hoaDonId);

    /** Mã giao dịch lớn nhất trong ngày, để sinh số thứ tự kế tiếp. */
    @Query("SELECT MAX(t.maGiaoDich) FROM ThanhToan t WHERE t.maGiaoDich LIKE CONCAT(:tienTo, '%')")
    String timMaLonNhatTheoTienTo(@Param("tienTo") String tienTo);

    String DIEU_KIEN_LOC = """
             WHERE (:tuLuc     IS NULL OR t.ngayThanhToan >= :tuLuc)
               AND (:denLuc    IS NULL OR t.ngayThanhToan <= :denLuc)
               AND (:hinhThuc  IS NULL OR t.hinhThuc = :hinhThuc)
               AND (:nguoiThuId IS NULL OR t.nguoiThu.id = :nguoiThuId)
            """;

    @Query(value = "SELECT t FROM ThanhToan t JOIN FETCH t.hoaDon h JOIN FETCH h.khachHang "
            + "JOIN FETCH h.thueBao LEFT JOIN FETCH t.nguoiThu " + DIEU_KIEN_LOC
            + " ORDER BY t.ngayThanhToan DESC, t.id DESC",
            countQuery = "SELECT COUNT(t) FROM ThanhToan t " + DIEU_KIEN_LOC)
    Page<ThanhToan> timKiem(@Param("tuLuc") LocalDateTime tuLuc,
                            @Param("denLuc") LocalDateTime denLuc,
                            @Param("hinhThuc") HinhThucThanhToan hinhThuc,
                            @Param("nguoiThuId") Long nguoiThuId,
                            Pageable pageable);

    @Query("SELECT t FROM ThanhToan t JOIN FETCH t.hoaDon h JOIN FETCH h.khachHang "
            + "JOIN FETCH h.thueBao LEFT JOIN FETCH t.nguoiThu " + DIEU_KIEN_LOC
            + " ORDER BY t.ngayThanhToan DESC, t.id DESC")
    List<ThanhToan> timTatCaCoLoc(@Param("tuLuc") LocalDateTime tuLuc,
                                  @Param("denLuc") LocalDateTime denLuc,
                                  @Param("hinhThuc") HinhThucThanhToan hinhThuc,
                                  @Param("nguoiThuId") Long nguoiThuId);

    /**
     * Tổng tiền đã thu của một hóa đơn, tính từ chính bảng thanh toán.
     *
     * <p>⚠️ <b>Chỉ dùng để KIỂM CHỨNG bất biến</b>, tuyệt đối không dùng để hiển thị. Ràng
     * buộc ② của Phase 5: {@code hoa_don.da_thanh_toan} là nguồn duy nhất cho màn hình. Hàm
     * này tồn tại để đối chiếu hai nguồn phải khớp nhau, và nếu lệch thì là có lỗi thật.</p>
     */
    @Query("SELECT COALESCE(SUM(t.soTien), 0) FROM ThanhToan t WHERE t.hoaDon.id = :hoaDonId")
    BigDecimal tinhTongDaThu(@Param("hoaDonId") Long hoaDonId);

    /**
     * Giao dịch gần nhất, nạp sẵn quan hệ — bảng cuối dashboard.
     *
     * <p>Sắp theo {@code (ngayThanhToan, id)} chứ không chỉ theo ngày: dữ liệu mẫu có nhiều
     * giao dịch cùng một mốc 09:00, và sắp theo một khoá không phân biệt được thì thứ tự
     * hiển thị đổi mỗi lần tải trang.</p>
     */
    @Query("""
            SELECT t FROM ThanhToan t
            JOIN FETCH t.hoaDon h
            JOIN FETCH h.khachHang
            JOIN FETCH h.thueBao
            LEFT JOIN FETCH t.nguoiThu
            ORDER BY t.ngayThanhToan DESC, t.id DESC
            """)
    List<ThanhToan> timGiaoDichGanNhat(Pageable pageable);
}
