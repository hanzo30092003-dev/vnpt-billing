package com.hanzo.billing.repository;

import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BangGiaCuocRepository extends JpaRepository<BangGiaCuoc, Long> {

    /** Bảng giá mặc định (không gắn với gói cước cụ thể nào). */
    List<BangGiaCuoc> findByGoiCuocIsNull();

    List<BangGiaCuoc> findByGoiCuocId(Long goiCuocId);

    List<BangGiaCuoc> findByLoaiDichVuAndHuong(LoaiDichVu loaiDichVu, HuongCuocGoi huong);

    List<BangGiaCuoc> findByLoaiDichVuAndHuongAndGioCaoDiem(
            LoaiDichVu loaiDichVu, HuongCuocGoi huong, Boolean gioCaoDiem);
}
