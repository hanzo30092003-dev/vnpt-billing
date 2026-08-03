package com.hanzo.billing.repository;

import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.enums.TrangThaiDangKyGoi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DangKyGoiCuocRepository extends JpaRepository<DangKyGoiCuoc, Long> {

    List<DangKyGoiCuoc> findByThueBaoId(Long thueBaoId);

    /** Bản ghi gói cước đang có hiệu lực của một thuê bao. */
    Optional<DangKyGoiCuoc> findFirstByThueBaoIdAndTrangThai(Long thueBaoId, TrangThaiDangKyGoi trangThai);

    long countByTrangThai(TrangThaiDangKyGoi trangThai);

    /**
     * Toàn bộ lịch sử đăng ký gói, nạp một lần cho engine tính cước.
     *
     * <p>Engine phải biết thuê bao dùng gói nào <b>tại thời điểm phát sinh CDR</b>, không
     * phải gói hiện hành. Bảng chỉ có chục dòng mỗi thuê bao nên nạp hết rồi gom nhóm
     * trong bộ nhớ, thay vì truy vấn cho từng bản ghi trong số hàng nghìn CDR.</p>
     */
    @Query("SELECT dk FROM DangKyGoiCuoc dk JOIN FETCH dk.goiCuoc JOIN FETCH dk.thueBao")
    List<DangKyGoiCuoc> timTatCaKemGoiVaThueBao();

    /** Lịch sử gói cước, mới nhất trước, nạp sẵn gói để hiển thị tên gói. */
    @Query("""
            SELECT dk FROM DangKyGoiCuoc dk
            JOIN FETCH dk.goiCuoc
            WHERE dk.thueBao.id = :thueBaoId
            ORDER BY dk.ngayBatDau DESC, dk.id DESC
            """)
    List<DangKyGoiCuoc> timLichSuTheoThueBao(@Param("thueBaoId") Long thueBaoId);
}
