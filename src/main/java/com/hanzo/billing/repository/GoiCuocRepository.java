package com.hanzo.billing.repository;

import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.enums.LoaiThueBao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GoiCuocRepository extends JpaRepository<GoiCuoc, Long> {

    Optional<GoiCuoc> findByMaGoi(String maGoi);

    boolean existsByMaGoi(String maGoi);

    List<GoiCuoc> findByLoaiThueBao(LoaiThueBao loaiThueBao);

    /** Các gói đang còn hiệu lực để hiển thị khi đăng ký mới. */
    List<GoiCuoc> findByTrangThaiTrue();
}
