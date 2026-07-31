package com.hanzo.billing.repository;

import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KyCuocRepository extends JpaRepository<KyCuoc, Long> {

    Optional<KyCuoc> findByThangAndNam(Integer thang, Integer nam);

    List<KyCuoc> findByTrangThai(TrangThaiKyCuoc trangThai);

    List<KyCuoc> findByNamOrderByThangAsc(Integer nam);
}
