package com.hanzo.billing.repository;

import com.hanzo.billing.dto.baocao.CoCauCuoc;
import com.hanzo.billing.dto.baocao.DongDoanhThuGoi;
import com.hanzo.billing.dto.baocao.DongDoanhThuKy;
import com.hanzo.billing.dto.baocao.DongKhachHangNo;
import com.hanzo.billing.dto.baocao.DongTopThueBao;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    // =================================================================
    // PHASE 5 — TRA CỨU, CÔNG NỢ, QUÁ HẠN
    // =================================================================

    String DIEU_KIEN_LOC = """
             WHERE (:kyCuocId  IS NULL OR h.kyCuoc.id = :kyCuocId)
               AND (:trangThai IS NULL OR h.trangThai = :trangThai)
               AND (:khachHang IS NULL OR LOWER(kh.tenKh) LIKE LOWER(CONCAT('%', :khachHang, '%'))
                    OR kh.maKh LIKE CONCAT('%', :khachHang, '%'))
               AND (:soThueBao IS NULL OR tb.soThueBao LIKE CONCAT('%', :soThueBao, '%'))
               AND (:tuSoTien  IS NULL OR h.tongThanhToan >= :tuSoTien)
               AND (:denSoTien IS NULL OR h.tongThanhToan <= :denSoTien)
            """;

    @Query(value = "SELECT h FROM HoaDon h JOIN FETCH h.thueBao tb JOIN FETCH h.khachHang kh "
            + "JOIN FETCH h.kyCuoc " + DIEU_KIEN_LOC + " ORDER BY h.maHoaDon DESC",
            countQuery = "SELECT COUNT(h) FROM HoaDon h JOIN h.thueBao tb JOIN h.khachHang kh "
                    + DIEU_KIEN_LOC)
    Page<HoaDon> timKiem(@Param("kyCuocId") Long kyCuocId,
                         @Param("trangThai") TrangThaiHoaDon trangThai,
                         @Param("khachHang") String khachHang,
                         @Param("soThueBao") String soThueBao,
                         @Param("tuSoTien") BigDecimal tuSoTien,
                         @Param("denSoTien") BigDecimal denSoTien,
                         Pageable pageable);

    /** Toàn bộ kết quả lọc, không phân trang — dùng cho xuất Excel và dòng tổng. */
    @Query("SELECT h FROM HoaDon h JOIN FETCH h.thueBao tb JOIN FETCH h.khachHang kh "
            + "JOIN FETCH h.kyCuoc " + DIEU_KIEN_LOC + " ORDER BY h.maHoaDon DESC")
    List<HoaDon> timTatCaCoLoc(@Param("kyCuocId") Long kyCuocId,
                               @Param("trangThai") TrangThaiHoaDon trangThai,
                               @Param("khachHang") String khachHang,
                               @Param("soThueBao") String soThueBao,
                               @Param("tuSoTien") BigDecimal tuSoTien,
                               @Param("denSoTien") BigDecimal denSoTien);

    /** Một hóa đơn kèm mọi quan hệ cần cho màn hình chi tiết và bản PDF. */
    @Query("""
            SELECT h FROM HoaDon h
            JOIN FETCH h.thueBao tb
            JOIN FETCH tb.goiCuoc
            JOIN FETCH h.khachHang
            JOIN FETCH h.kyCuoc
            WHERE h.id = :id
            """)
    Optional<HoaDon> timKemQuanHe(@Param("id") Long id);

    /**
     * Hóa đơn cần chuyển sang {@code QUA_HAN}: quá hạn thanh toán mà <b>chưa thu được đồng
     * nào</b>.
     *
     * <p>⚠️ Điều kiện {@code trangThai <> DA_TT} là bắt buộc chứ không thừa. Một hóa đơn đã
     * thu đủ thì {@code con_no = 0} nên tự nhiên rơi ra ngoài, nhưng viết rõ ra để ý định
     * "không bao giờ ghi đè DA_TT" nằm ngay trong truy vấn thay vì phụ thuộc vào một suy
     * luận gián tiếp.</p>
     *
     * <p>⭐ <b>Điều kiện {@code da_thanh_toan = 0} thêm ở mục F, và nó sửa một lỗi thật.</b>
     * Bản đầu chỉ loại {@code DA_TT}, nên hóa đơn <b>trả một phần</b> quá hạn cũng bị quét
     * về {@code QUA_HAN}. Hệ quả: {@code TT_MOT_PHAN} là một trạng thái <i>không thể tồn
     * tại</i> khi hạn đã qua — mọi hóa đơn trả dở đều bị xoá dấu vết "đã thu được một phần"
     * ngay lần kế tiếp có ai đó mở màn hình hóa đơn hoặc công nợ.</p>
     *
     * <p>Ranh giới đúng: quét đổi trạng thái của <b>tiền</b> chỉ được phép ghi đè khi chưa
     * có đồng tiền nào đi vào. Thông tin "đã quá hạn" của hóa đơn trả dở <b>không mất</b> —
     * màn hình công nợ suy nó từ {@code han_thanh_toan} qua cột <i>số ngày quá hạn</i> và
     * {@link com.hanzo.billing.enums.NhomTuoiNo}, độc lập hoàn toàn với cột trạng thái.</p>
     */
    @Query("""
            SELECT h FROM HoaDon h
            WHERE h.hanThanhToan < :homNay
              AND h.conNo > 0
              AND (h.daThanhToan IS NULL OR h.daThanhToan = 0)
              AND h.trangThai <> com.hanzo.billing.enums.TrangThaiHoaDon.DA_TT
              AND h.trangThai <> com.hanzo.billing.enums.TrangThaiHoaDon.QUA_HAN
            """)
    List<HoaDon> timCanChuyenQuaHan(@Param("homNay") LocalDate homNay);

    // =================================================================
    // PHASE 6 — TRUY VẤN THỐNG KÊ
    // =================================================================
    // Mọi truy vấn dưới đây gom số NGAY TRONG CSDL bằng SELECT new + GROUP BY,
    // không load entity lên rồi cộng trong Java (yêu cầu D.4). Với 280 hóa đơn
    // thì hai cách đều nhanh; nhưng cách sau tăng tuyến tính theo số hóa đơn và
    // sẽ hỏng lặng lẽ khi dữ liệu lớn lên, đúng lúc không ai còn nhớ vì sao.

    /**
     * Doanh thu từng kỳ — dòng dữ liệu của biểu đồ cột trên dashboard và báo cáo C.1.
     *
     * <p>{@code LEFT JOIN} để kỳ <b>chưa lập hóa đơn nào</b> vẫn hiện ra với số 0 thay vì biến
     * mất khỏi báo cáo. Và vì thế phải đếm {@code COUNT(h.id)} chứ không {@code COUNT(h)}:
     * với kỳ rỗng, {@code LEFT JOIN} trả về một dòng có {@code h} bằng null, mà {@code COUNT}
     * trên cả entity sẽ đếm dòng đó thành 1.</p>
     */
    @Query("""
            SELECT new com.hanzo.billing.dto.baocao.DongDoanhThuKy(
                       k.id, k.thang, k.nam, k.trangThai, COUNT(h.id),
                       COALESCE(SUM(h.tongThanhToan), 0),
                       COALESCE(SUM(h.daThanhToan), 0),
                       COALESCE(SUM(h.conNo), 0))
              FROM KyCuoc k LEFT JOIN HoaDon h ON h.kyCuoc = k
             GROUP BY k.id, k.thang, k.nam, k.trangThai
             ORDER BY k.nam, k.thang
            """)
    List<DongDoanhThuKy> thongKeDoanhThuTheoKy();

    /** Doanh thu theo gói cước trong một kỳ — báo cáo C.2. */
    @Query("""
            SELECT new com.hanzo.billing.dto.baocao.DongDoanhThuGoi(
                       g.maGoi, g.tenGoi, COUNT(h.id),
                       COALESCE(SUM(h.tongThanhToan), 0),
                       COALESCE(SUM(h.daThanhToan), 0))
              FROM HoaDon h JOIN h.thueBao tb JOIN tb.goiCuoc g
             WHERE h.kyCuoc.id = :kyCuocId
             GROUP BY g.id, g.maGoi, g.tenGoi
             ORDER BY SUM(h.tongThanhToan) DESC
            """)
    List<DongDoanhThuGoi> thongKeDoanhThuTheoGoi(@Param("kyCuocId") Long kyCuocId);

    /**
     * Cơ cấu cước một kỳ — báo cáo C.3.
     *
     * <p>Đọc thẳng sáu cột cước của {@code hoa_don}, <b>không</b> cộng lại từ CDR: hóa đơn còn
     * có cước thuê bao tháng không đến từ bản ghi CDR nào, và đã trừ ưu đãi gói cước lẫn giảm
     * trừ. Cộng lại từ CDR sẽ ra một con số gần giống nhưng khác — hai nguồn sự thật.</p>
     */
    @Query("""
            SELECT new com.hanzo.billing.dto.baocao.CoCauCuoc(
                       COALESCE(SUM(h.cuocThueBao), 0), COALESCE(SUM(h.cuocThoai), 0),
                       COALESCE(SUM(h.cuocSms), 0), COALESCE(SUM(h.cuocData), 0),
                       COALESCE(SUM(h.cuocKhac), 0), COALESCE(SUM(h.giamTru), 0),
                       COALESCE(SUM(h.tongTruocThue), 0), COALESCE(SUM(h.thueVat), 0),
                       COALESCE(SUM(h.tongThanhToan), 0))
              FROM HoaDon h
             WHERE h.kyCuoc.id = :kyCuocId
            """)
    CoCauCuoc coCauCuocTheoKy(@Param("kyCuocId") Long kyCuocId);

    /** Thuê bao cước cao nhất trong một kỳ — báo cáo C.5 và bảng trên dashboard. */
    @Query("""
            SELECT new com.hanzo.billing.dto.baocao.DongTopThueBao(
                       h.id, h.maHoaDon, tb.soThueBao, kh.tenKh, g.maGoi,
                       h.tongThanhToan, COALESCE(h.daThanhToan, 0), h.conNo, h.trangThai)
              FROM HoaDon h JOIN h.thueBao tb JOIN tb.goiCuoc g JOIN h.khachHang kh
             WHERE h.kyCuoc.id = :kyCuocId
             ORDER BY h.tongThanhToan DESC, h.maHoaDon
            """)
    List<DongTopThueBao> topThueBaoCuocCao(@Param("kyCuocId") Long kyCuocId, Pageable pageable);

    /** Khách hàng nợ nhiều nhất — báo cáo C.7. */
    @Query("""
            SELECT new com.hanzo.billing.dto.baocao.DongKhachHangNo(
                       kh.id, kh.maKh, kh.tenKh, COUNT(h.id), SUM(h.conNo))
              FROM HoaDon h JOIN h.khachHang kh
             WHERE h.conNo > 0
             GROUP BY kh.id, kh.maKh, kh.tenKh
             ORDER BY SUM(h.conNo) DESC
            """)
    List<DongKhachHangNo> topKhachHangNo(Pageable pageable);

    /** Tổng công nợ toàn hệ thống — thẻ số liệu trên dashboard, đọc cột {@code con_no}. */
    @Query("SELECT COALESCE(SUM(h.conNo), 0) FROM HoaDon h WHERE h.conNo > 0")
    BigDecimal tongConNoToanHeThong();

    /** Hóa đơn còn nợ kèm quan hệ, để dựng màn hình công nợ và bảng tuổi nợ. */
    @Query("""
            SELECT h FROM HoaDon h
            JOIN FETCH h.thueBao tb
            JOIN FETCH h.khachHang kh
            JOIN FETCH h.kyCuoc
            WHERE h.conNo > 0
              AND (:khachHang IS NULL
                   OR LOWER(kh.tenKh) LIKE LOWER(CONCAT('%', :khachHang, '%'))
                   OR kh.maKh LIKE CONCAT('%', :khachHang, '%'))
            ORDER BY h.hanThanhToan ASC, h.maHoaDon ASC
            """)
    List<HoaDon> timConNo(@Param("khachHang") String khachHang);
}
