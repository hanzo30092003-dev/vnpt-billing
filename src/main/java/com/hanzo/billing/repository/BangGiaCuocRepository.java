package com.hanzo.billing.repository;

import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface BangGiaCuocRepository extends JpaRepository<BangGiaCuoc, Long> {

    /** Bảng giá mặc định (không gắn với gói cước cụ thể nào). */
    List<BangGiaCuoc> findByGoiCuocIsNull();

    List<BangGiaCuoc> findByGoiCuocId(Long goiCuocId);

    List<BangGiaCuoc> findByLoaiDichVuAndHuong(LoaiDichVu loaiDichVu, HuongCuocGoi huong);

    List<BangGiaCuoc> findByLoaiDichVuAndHuongAndGioCaoDiem(
            LoaiDichVu loaiDichVu, HuongCuocGoi huong, Boolean gioCaoDiem);

    /**
     * Các dòng CÙNG BỘ KHOÁ nghiệp vụ với dòng đang xét, để kiểm tra chồng khoảng
     * hiệu lực. Bộ khoá gồm 4 thành phần: gói cước, loại dịch vụ, hướng, giờ cao điểm.
     *
     * <p>{@code goiCuocId} null nghĩa là dòng giá mặc định chung — phải so bằng
     * {@code bg.goiCuoc IS NULL} chứ không so {@code = null} vì trong SQL mọi phép so
     * sánh với NULL đều cho kết quả UNKNOWN.</p>
     *
     * @param idBoQua id của bản ghi đang sửa, truyền -1 khi thêm mới
     */
    @Query("""
            SELECT bg FROM BangGiaCuoc bg
            WHERE ((:goiCuocId IS NULL AND bg.goiCuoc IS NULL)
                   OR (:goiCuocId IS NOT NULL AND bg.goiCuoc.id = :goiCuocId))
              AND bg.loaiDichVu = :loaiDichVu
              AND bg.huong = :huong
              AND bg.gioCaoDiem = :gioCaoDiem
              AND bg.id <> :idBoQua
            ORDER BY bg.ngayHieuLuc
            """)
    List<BangGiaCuoc> timCungBoKhoa(@Param("goiCuocId") Long goiCuocId,
                                    @Param("loaiDichVu") LoaiDichVu loaiDichVu,
                                    @Param("huong") HuongCuocGoi huong,
                                    @Param("gioCaoDiem") Boolean gioCaoDiem,
                                    @Param("idBoQua") Long idBoQua);

    /**
     * Toàn bộ bảng giá có hiệu lực tại một ngày cho trước.
     *
     * <p>{@code ngay_het_hieu_luc} null nghĩa là vô thời hạn nên vẫn tính là còn hiệu lực.</p>
     */
    @Query("""
            SELECT bg FROM BangGiaCuoc bg
            LEFT JOIN FETCH bg.goiCuoc
            WHERE bg.ngayHieuLuc <= :ngay
              AND (bg.ngayHetHieuLuc IS NULL OR bg.ngayHetHieuLuc >= :ngay)
            ORDER BY bg.goiCuoc.id NULLS FIRST, bg.loaiDichVu, bg.huong, bg.gioCaoDiem
            """)
    List<BangGiaCuoc> timCoHieuLucTaiNgay(@Param("ngay") LocalDate ngay);

    /** Danh sách có lọc; mọi tham số null nghĩa là bỏ qua điều kiện đó. */
    @Query("""
            SELECT bg FROM BangGiaCuoc bg
            LEFT JOIN FETCH bg.goiCuoc
            WHERE (:locTheoGoi = FALSE
                   OR (:goiCuocId IS NULL AND bg.goiCuoc IS NULL)
                   OR (:goiCuocId IS NOT NULL AND bg.goiCuoc.id = :goiCuocId))
              AND (:loaiDichVu IS NULL OR bg.loaiDichVu = :loaiDichVu)
              AND (:huong IS NULL OR bg.huong = :huong)
              AND (:chiConHieuLuc = FALSE
                   OR (bg.ngayHieuLuc <= :homNay
                       AND (bg.ngayHetHieuLuc IS NULL OR bg.ngayHetHieuLuc >= :homNay)))
            ORDER BY bg.goiCuoc.id NULLS FIRST, bg.loaiDichVu, bg.huong, bg.gioCaoDiem, bg.ngayHieuLuc DESC
            """)
    List<BangGiaCuoc> timCoLoc(@Param("locTheoGoi") boolean locTheoGoi,
                               @Param("goiCuocId") Long goiCuocId,
                               @Param("loaiDichVu") LoaiDichVu loaiDichVu,
                               @Param("huong") HuongCuocGoi huong,
                               @Param("chiConHieuLuc") boolean chiConHieuLuc,
                               @Param("homNay") LocalDate homNay);
}
