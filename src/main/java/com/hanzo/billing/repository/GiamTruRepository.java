package com.hanzo.billing.repository;

import com.hanzo.billing.entity.GiamTru;
import com.hanzo.billing.enums.TrangThaiGiamTru;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiamTruRepository extends JpaRepository<GiamTru, Long> {

    List<GiamTru> findByThueBaoId(Long thueBaoId);

    List<GiamTru> findByThueBaoIdAndKyCuocId(Long thueBaoId, Long kyCuocId);

    List<GiamTru> findByTrangThai(TrangThaiGiamTru trangThai);
}
