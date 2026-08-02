package com.hanzo.billing.repository;

import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.enums.LoaiKhachHang;
import com.hanzo.billing.enums.TrangThaiKhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    Optional<KhachHang> findByMaKh(String maKh);

    boolean existsByMaKh(String maKh);

    List<KhachHang> findBySoGiayTo(String soGiayTo);

    /** Kiểm tra trùng số giấy tờ, bỏ qua chính bản ghi đang sửa. */
    boolean existsBySoGiayToAndIdNot(String soGiayTo, Long id);

    boolean existsBySoGiayTo(String soGiayTo);

    List<KhachHang> findByTenKhContainingIgnoreCase(String tenKh);

    List<KhachHang> findByLoaiKh(LoaiKhachHang loaiKh);

    long countByLoaiKh(LoaiKhachHang loaiKh);

    long countByTrangThai(TrangThaiKhachHang trangThai);

    /**
     * Tìm kiếm kết hợp lọc, có phân trang.
     *
     * <p>Mỗi điều kiện đều có dạng {@code :thamSo IS NULL OR ...} nên khi người dùng
     * bỏ trống ô lọc thì điều kiện đó tự động bị vô hiệu, tránh phải viết nhiều
     * phương thức truy vấn cho từng tổ hợp bộ lọc.</p>
     */
    @Query("""
            SELECT kh FROM KhachHang kh
            WHERE (:tuKhoa IS NULL OR :tuKhoa = ''
                   OR LOWER(kh.maKh)      LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                   OR LOWER(kh.tenKh)     LIKE LOWER(CONCAT('%', :tuKhoa, '%'))
                   OR kh.soGiayTo         LIKE CONCAT('%', :tuKhoa, '%')
                   OR kh.dienThoaiLh      LIKE CONCAT('%', :tuKhoa, '%'))
              AND (:loaiKh IS NULL OR kh.loaiKh = :loaiKh)
              AND (:trangThai IS NULL OR kh.trangThai = :trangThai)
            """)
    Page<KhachHang> timKiem(@Param("tuKhoa") String tuKhoa,
                            @Param("loaiKh") LoaiKhachHang loaiKh,
                            @Param("trangThai") TrangThaiKhachHang trangThai,
                            Pageable pageable);

    /**
     * Mã khách hàng lớn nhất đang có, hoặc null nếu bảng còn rỗng.
     *
     * <p>Việc tách phần số và cộng 1 được làm ở tầng Java trong
     * {@code SinhMaServiceImpl} để có thể viết unit test, thay vì nhét
     * {@code CAST(SUBSTRING(...))} vào câu SQL như trước.</p>
     */
    @Query("SELECT MAX(kh.maKh) FROM KhachHang kh")
    String timMaKhachHangLonNhat();
}
