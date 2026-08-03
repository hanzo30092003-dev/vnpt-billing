package com.hanzo.billing.repository;

import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.NguonCdr;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ChiTietSuDungRepository extends JpaRepository<ChiTietSuDung, Long> {

    List<ChiTietSuDung> findByThueBaoId(Long thueBaoId);

    List<ChiTietSuDung> findByTrangThaiTinhCuoc(TrangThaiTinhCuoc trangThaiTinhCuoc);

    /** CDR của một thuê bao trong khoảng thời gian, dùng khi tính cước theo kỳ. */
    List<ChiTietSuDung> findByThueBaoIdAndThoiGianBatDauBetween(
            Long thueBaoId, LocalDateTime tuLuc, LocalDateTime denLuc);

    long countByKyCuocId(Long kyCuocId);

    long countByTrangThaiTinhCuoc(TrangThaiTinhCuoc trangThaiTinhCuoc);

    /**
     * Điều kiện lọc dùng chung cho ba truy vấn bên dưới (danh sách phân trang,
     * đếm tổng, và lấy toàn bộ để xuất Excel). Mọi tham số null nghĩa là bỏ qua
     * điều kiện tương ứng.
     */
    String DIEU_KIEN_LOC = """
            WHERE (:soThueBao IS NULL OR :soThueBao = '' OR c.soThueBao LIKE CONCAT('%', :soThueBao, '%'))
              AND (:tuLuc IS NULL OR c.thoiGianBatDau >= :tuLuc)
              AND (:denLuc IS NULL OR c.thoiGianBatDau <= :denLuc)
              AND (:loaiDichVu IS NULL OR c.loaiDichVu = :loaiDichVu)
              AND (:huong IS NULL OR c.huong = :huong)
              AND (:trangThaiTinhCuoc IS NULL OR c.trangThaiTinhCuoc = :trangThaiTinhCuoc)
              AND (:nguon IS NULL OR c.nguon = :nguon)
            """;

    @Query(value = "SELECT c FROM ChiTietSuDung c " + DIEU_KIEN_LOC + " ORDER BY c.thoiGianBatDau DESC, c.id DESC",
            countQuery = "SELECT COUNT(c) FROM ChiTietSuDung c " + DIEU_KIEN_LOC)
    Page<ChiTietSuDung> timCoLoc(@Param("soThueBao") String soThueBao,
                                 @Param("tuLuc") LocalDateTime tuLuc,
                                 @Param("denLuc") LocalDateTime denLuc,
                                 @Param("loaiDichVu") LoaiDichVu loaiDichVu,
                                 @Param("huong") HuongCuocGoi huong,
                                 @Param("trangThaiTinhCuoc") TrangThaiTinhCuoc trangThaiTinhCuoc,
                                 @Param("nguon") NguonCdr nguon,
                                 Pageable pageable);

    /**
     * Số liệu tổng theo bộ lọc hiện tại, phục vụ dòng tổng cuối bảng.
     *
     * <p>Tính bằng một câu truy vấn tổng hợp thay vì cộng dồn trên trang đang xem —
     * dòng tổng phải phản ánh TOÀN BỘ kết quả lọc chứ không chỉ 25 dòng đang hiển thị.</p>
     *
     * <p>Kiểu trả về khai là {@code List<Object[]>} chứ không phải {@code Object[]}:
     * với truy vấn tổng hợp một dòng nhiều cột, Spring Data bọc kết quả thành
     * {@code Object[]{ Object[]{...} }} khiến việc ép kiểu trực tiếp sang Number
     * bị ClassCastException. Khai theo danh sách thì cấu trúc luôn rõ ràng.</p>
     *
     * @return danh sách một phần tử, là mảng {@code [soBanGhi, tongThoiLuongGiay, tongDungLuongKb]}
     */
    @Query("""
            SELECT COUNT(c),
                   COALESCE(SUM(c.thoiLuongGiay), 0),
                   COALESCE(SUM(CASE WHEN c.loaiDichVu = com.hanzo.billing.enums.LoaiDichVu.DATA
                                     THEN c.soLuong ELSE 0 END), 0)
            FROM ChiTietSuDung c
            """ + DIEU_KIEN_LOC)
    List<Object[]> tinhTong(@Param("soThueBao") String soThueBao,
                      @Param("tuLuc") LocalDateTime tuLuc,
                      @Param("denLuc") LocalDateTime denLuc,
                      @Param("loaiDichVu") LoaiDichVu loaiDichVu,
                      @Param("huong") HuongCuocGoi huong,
                      @Param("trangThaiTinhCuoc") TrangThaiTinhCuoc trangThaiTinhCuoc,
                      @Param("nguon") NguonCdr nguon);

    /**
     * Các tổ hợp tra giá <b>thực sự</b> có trong dữ liệu CDR.
     *
     * <p>Phục vụ bất biến "mọi tổ hợp có trong CDR đều phải tra được đơn giá" —
     * xem {@code KiemTraDoPhuBangGiaTest}. Gộp cả gói cước của thuê bao vào nhóm vì
     * engine tra giá ưu tiên dòng gắn gói trước dòng mặc định chung, nên phải kiểm
     * đúng cặp (gói, tổ hợp) mà engine sẽ gặp.</p>
     *
     * <p>Trả về ngày sớm nhất và muộn nhất của mỗi nhóm để kiểm được cả hai đầu khoảng
     * hiệu lực: bảng giá có thể còn hiệu lực đầu tháng nhưng đã hết hiệu lực cuối tháng.</p>
     *
     * @return mỗi phần tử là mảng
     *         {@code [goiCuocId, loaiDichVu, huong, gioCaoDiem, soBanGhi, somNhat, muonNhat]}
     */
    @Query("""
            SELECT tb.goiCuoc.id, c.loaiDichVu, c.huong, c.gioCaoDiem,
                   COUNT(c), MIN(c.thoiGianBatDau), MAX(c.thoiGianBatDau)
            FROM ChiTietSuDung c
            JOIN c.thueBao tb
            GROUP BY tb.goiCuoc.id, c.loaiDichVu, c.huong, c.gioCaoDiem
            """)
    List<Object[]> thongKeToHopTraGia();

    /** Toàn bộ bản ghi khớp bộ lọc, dùng khi xuất Excel. */
    @Query("SELECT c FROM ChiTietSuDung c " + DIEU_KIEN_LOC + " ORDER BY c.thoiGianBatDau DESC, c.id DESC")
    List<ChiTietSuDung> timTatCaCoLoc(@Param("soThueBao") String soThueBao,
                                      @Param("tuLuc") LocalDateTime tuLuc,
                                      @Param("denLuc") LocalDateTime denLuc,
                                      @Param("loaiDichVu") LoaiDichVu loaiDichVu,
                                      @Param("huong") HuongCuocGoi huong,
                                      @Param("trangThaiTinhCuoc") TrangThaiTinhCuoc trangThaiTinhCuoc,
                                      @Param("nguon") NguonCdr nguon);
}
