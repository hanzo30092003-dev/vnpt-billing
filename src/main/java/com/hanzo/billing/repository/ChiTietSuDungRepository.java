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

    long countByKyCuocIdAndTrangThaiTinhCuoc(Long kyCuocId, TrangThaiTinhCuoc trangThaiTinhCuoc);

    /**
     * Bản ghi đã tính cước nhưng không lưu lại dòng bảng giá đã áp dụng.
     *
     * <p>Phải luôn bằng 0: mọi bản ghi {@code DA_TINH} đều phải truy nguyên được đơn giá.</p>
     */
    long countByTrangThaiTinhCuocAndBangGiaCuocIsNull(TrangThaiTinhCuoc trangThaiTinhCuoc);

    /**
     * CDR cần tính cước trong một kỳ.
     *
     * <p><b>Khoảng thời gian là NỬA MỞ</b> {@code [tuLuc, denLuc)} với
     * {@code denLuc = ngày cuối kỳ + 1 ngày, lúc 00:00}. Cách viết quen thuộc hơn là
     * {@code BETWEEN tuLuc AND ngayKetThuc.atTime(23,59,59)} — nhưng cách đó bỏ sót mọi
     * bản ghi rơi vào khoảng {@code 23:59:59,000001} đến hết ngày. Hiện cột
     * {@code thoi_gian_bat_dau} khai là {@code DATETIME} không có phần lẻ giây nên chưa
     * mất bản ghi nào, song bản ghi muộn nhất đang là {@code 30/06/2026 23:59:03} — chỉ
     * cách ranh giới 56 giây. Đổi cột sang {@code DATETIME(3)} là mất dữ liệu ngay, và
     * mất im lặng.</p>
     *
     * <p>Lấy cả {@code LOI} bên cạnh {@code CHUA_TINH} để chạy lại được các bản ghi hỏng
     * sau khi đã sửa nguyên nhân; bản ghi {@code DA_TINH} bị loại nên chạy lại engine
     * không làm đổi cước đã tính.</p>
     *
     * <p>{@code JOIN FETCH} thuê bao và gói cước để tránh N+1: không có nó, mỗi bản ghi
     * chạm vào gói cước hiện hành sẽ sinh thêm truy vấn.</p>
     *
     * <p>Sắp xếp cố định theo {@code (thoiGianBatDau, id)} để hai lần chạy trên cùng dữ
     * liệu cho ra cùng kết quả — điều kiện bắt buộc khi mục 4D trừ dần quỹ ưu đãi.</p>
     */
    @Query("""
            SELECT c FROM ChiTietSuDung c
            JOIN FETCH c.thueBao tb
            JOIN FETCH tb.goiCuoc
            WHERE c.thoiGianBatDau >= :tuLuc
              AND c.thoiGianBatDau < :denLuc
              AND c.trangThaiTinhCuoc <> com.hanzo.billing.enums.TrangThaiTinhCuoc.DA_TINH
            ORDER BY c.thoiGianBatDau, c.id
            """)
    List<ChiTietSuDung> timCanTinhCuoc(@Param("tuLuc") LocalDateTime tuLuc,
                                       @Param("denLuc") LocalDateTime denLuc);

    /**
     * Toàn bộ CDR của một thuê bao trong một kỳ, kèm dòng bảng giá đã áp dụng.
     *
     * <p>Phục vụ bảng đối soát cước. {@code LEFT JOIN FETCH} chứ không {@code JOIN FETCH}:
     * bản ghi chưa tính cước hoặc lỗi thì {@code bang_gia_cuoc_id} còn trống, dùng
     * {@code JOIN} sẽ loại mất chính những bản ghi cần soi nhất.</p>
     */
    @Query("""
            SELECT c FROM ChiTietSuDung c
            LEFT JOIN FETCH c.bangGiaCuoc
            WHERE c.thueBao.id = :thueBaoId AND c.kyCuoc.id = :kyCuocId
            ORDER BY c.thoiGianBatDau, c.id
            """)
    List<ChiTietSuDung> timTheoThueBaoVaKy(@Param("thueBaoId") Long thueBaoId,
                                           @Param("kyCuocId") Long kyCuocId);

    /**
     * CDR đã định giá của một kỳ, sắp xếp để duyệt quỹ ưu đãi theo từng thuê bao.
     *
     * <p>Thứ tự {@code (thuê bao, thời gian, id)} là <b>bắt buộc</b>: quỹ ưu đãi trừ dần
     * theo thứ tự thời gian, nên thứ tự duyệt quyết định bản ghi nào được miễn phí. Thứ tự
     * cố định là điều kiện để chạy hai lần ra cùng kết quả.</p>
     */
    @Query("""
            SELECT c FROM ChiTietSuDung c
            JOIN FETCH c.thueBao tb
            JOIN FETCH tb.goiCuoc
            WHERE c.kyCuoc.id = :kyCuocId
              AND c.trangThaiTinhCuoc = com.hanzo.billing.enums.TrangThaiTinhCuoc.DA_TINH
            ORDER BY tb.id, c.thoiGianBatDau, c.id
            """)
    List<ChiTietSuDung> timDaTinhTheoKyDeApUuDai(@Param("kyCuocId") Long kyCuocId);

    /**
     * Tổng cước và sản lượng của một kỳ, gom theo (thuê bao, loại dịch vụ).
     *
     * <p>Một truy vấn duy nhất cho cả kỳ thay vì ba truy vấn cho mỗi thuê bao — với 58
     * thuê bao trả sau đó là 174 truy vấn tiết kiệm được.</p>
     *
     * <p>Chỉ lấy bản ghi {@code DA_TINH}: bản ghi {@code LOI} chưa có cước, gộp vào sẽ
     * cho ra hóa đơn thiếu tiền mà không có dấu hiệu gì.</p>
     *
     * @return mỗi phần tử là mảng
     *         {@code [thueBaoId, loaiDichVu, tongCuoc, tongThoiLuongGiay, tongSoLuong, soBanGhi]}
     */
    @Query("""
            SELECT c.thueBao.id, c.loaiDichVu,
                   COALESCE(SUM(c.cuocPhi), 0),
                   COALESCE(SUM(c.thoiLuongGiay), 0),
                   COALESCE(SUM(c.soLuong), 0),
                   COUNT(c)
            FROM ChiTietSuDung c
            WHERE c.kyCuoc.id = :kyCuocId
              AND c.trangThaiTinhCuoc = com.hanzo.billing.enums.TrangThaiTinhCuoc.DA_TINH
            GROUP BY c.thueBao.id, c.loaiDichVu
            """)
    List<Object[]> tongHopCuocTheoThueBao(@Param("kyCuocId") Long kyCuocId);

    /**
     * Số CDR trong khoảng thời gian của kỳ còn ở trạng thái cho trước.
     *
     * <p>Dùng để chặn lập hóa đơn khi kỳ chưa tính cước xong. Lọc theo thời gian chứ không
     * theo {@code ky_cuoc_id}, vì bản ghi {@code CHUA_TINH} chính là bản ghi <b>chưa</b>
     * được gán kỳ.</p>
     */
    @Query("""
            SELECT COUNT(c) FROM ChiTietSuDung c
            WHERE c.thoiGianBatDau >= :tuLuc
              AND c.thoiGianBatDau < :denLuc
              AND c.trangThaiTinhCuoc = :trangThai
            """)
    long demTheoKhoangVaTrangThai(@Param("tuLuc") LocalDateTime tuLuc,
                                  @Param("denLuc") LocalDateTime denLuc,
                                  @Param("trangThai") TrangThaiTinhCuoc trangThai);

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
