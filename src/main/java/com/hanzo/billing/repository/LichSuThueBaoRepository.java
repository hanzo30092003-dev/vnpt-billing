package com.hanzo.billing.repository;

import com.hanzo.billing.entity.LichSuThueBao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LichSuThueBaoRepository extends JpaRepository<LichSuThueBao, Long> {

    List<LichSuThueBao> findByThueBaoIdOrderByThoiGianDesc(Long thueBaoId);
}
