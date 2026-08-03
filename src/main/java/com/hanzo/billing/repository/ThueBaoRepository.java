package com.hanzo.billing.repository;

import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ThueBaoRepository extends JpaRepository<ThueBao, Long> {

    Optional<ThueBao> findBySoThueBao(String soThueBao);

    boolean existsBySoThueBao(String soThueBao);

    List<ThueBao> findByKhachHangId(Long khachHangId);

    List<ThueBao> findByTrangThai(TrangThaiThueBao trangThai);

    List<ThueBao> findByLoaiThueBao(LoaiThueBao loaiThueBao);

    /**
     * Thuê bao theo loại, nạp sẵn khách hàng và gói cước, sắp xếp theo id.
     *
     * <p>Dùng khi lập hóa đơn cả kỳ. Thứ tự {@code ORDER BY id} là cố định nên hai lần
     * chạy sinh mã hóa đơn theo cùng một trình tự.</p>
     */
    @Query("""
            SELECT tb FROM ThueBao tb
            JOIN FETCH tb.khachHang
            JOIN FETCH tb.goiCuoc
            WHERE tb.loaiThueBao = :loaiThueBao
            ORDER BY tb.id
            """)
    List<ThueBao> timTheoLoaiKemQuanHe(@Param("loaiThueBao") LoaiThueBao loaiThueBao);

    long countByTrangThai(TrangThaiThueBao trangThai);

    long countByLoaiThueBao(LoaiThueBao loaiThueBao);

    /** Đếm thuê bao đang hoạt động của một khách hàng — dùng khi chặn ngừng giao dịch. */
    long countByKhachHangIdAndTrangThai(Long khachHangId, TrangThaiThueBao trangThai);

    /** Đếm thuê bao đang dùng một gói cước — dùng khi chặn xoá gói. */
    long countByGoiCuocId(Long goiCuocId);

    /**
     * Đếm thuê bao theo từng gói cước trong MỘT truy vấn.
     *
     * <p>Dùng cho màn hình danh sách gói cước: nếu gọi {@code countByGoiCuocId} trong
     * vòng lặp thì mỗi gói là một truy vấn (bài toán N+1).</p>
     *
     * @return danh sách cặp {@code [goiCuocId, soLuong]}
     */
    @Query("SELECT tb.goiCuoc.id, COUNT(tb) FROM ThueBao tb GROUP BY tb.goiCuoc.id")
    List<Object[]> demThueBaoTheoGoiCuoc();

    /** Thuê bao của một gói cước, có phân trang, nạp sẵn khách hàng để hiển thị tên. */
    @Query(value = """
            SELECT tb FROM ThueBao tb
            JOIN FETCH tb.khachHang
            WHERE tb.goiCuoc.id = :goiCuocId
            """,
            countQuery = "SELECT COUNT(tb) FROM ThueBao tb WHERE tb.goiCuoc.id = :goiCuocId")
    Page<ThueBao> timTheoGoiCuoc(@Param("goiCuocId") Long goiCuocId, Pageable pageable);

    /**
     * Thuê bao của một khách hàng, nạp sẵn gói cước.
     *
     * <p>Phải dùng {@code JOIN FETCH} vì {@code spring.jpa.open-in-view = false}:
     * phiên Hibernate đã đóng trước khi Thymeleaf vẽ view, nên truy cập
     * {@code thueBao.goiCuoc.tenGoi} ở template sẽ ném LazyInitializationException
     * nếu không nạp trước trong lúc còn giao dịch.</p>
     */
    @Query("""
            SELECT tb FROM ThueBao tb
            JOIN FETCH tb.goiCuoc
            WHERE tb.khachHang.id = :khachHangId
            ORDER BY tb.id
            """)
    List<ThueBao> timTheoKhachHangKemGoiCuoc(@Param("khachHangId") Long khachHangId);

    /** Một thuê bao kèm sẵn khách hàng và gói cước, dùng cho màn hình chi tiết. */
    @Query("""
            SELECT tb FROM ThueBao tb
            JOIN FETCH tb.khachHang
            JOIN FETCH tb.goiCuoc
            WHERE tb.id = :id
            """)
    Optional<ThueBao> timTheoIdKemQuanHe(@Param("id") Long id);

    /**
     * Tìm kiếm kết hợp lọc, có phân trang. Nạp sẵn khách hàng và gói cước để
     * danh sách hiển thị được tên khách và tên gói mà không sinh truy vấn N+1.
     */
    @Query(value = """
            SELECT tb FROM ThueBao tb
            JOIN FETCH tb.khachHang kh
            JOIN FETCH tb.goiCuoc gc
            WHERE (:tuKhoa IS NULL OR :tuKhoa = ''
                   OR tb.soThueBao LIKE CONCAT('%', :tuKhoa, '%')
                   OR LOWER(kh.tenKh) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
              AND (:trangThai IS NULL OR tb.trangThai = :trangThai)
              AND (:loaiThueBao IS NULL OR tb.loaiThueBao = :loaiThueBao)
              AND (:goiCuocId IS NULL OR gc.id = :goiCuocId)
            """,
            countQuery = """
                    SELECT COUNT(tb) FROM ThueBao tb
                    WHERE (:tuKhoa IS NULL OR :tuKhoa = ''
                           OR tb.soThueBao LIKE CONCAT('%', :tuKhoa, '%')
                           OR LOWER(tb.khachHang.tenKh) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
                      AND (:trangThai IS NULL OR tb.trangThai = :trangThai)
                      AND (:loaiThueBao IS NULL OR tb.loaiThueBao = :loaiThueBao)
                      AND (:goiCuocId IS NULL OR tb.goiCuoc.id = :goiCuocId)
                    """)
    Page<ThueBao> timKiem(@Param("tuKhoa") String tuKhoa,
                          @Param("trangThai") TrangThaiThueBao trangThai,
                          @Param("loaiThueBao") LoaiThueBao loaiThueBao,
                          @Param("goiCuocId") Long goiCuocId,
                          Pageable pageable);
}
