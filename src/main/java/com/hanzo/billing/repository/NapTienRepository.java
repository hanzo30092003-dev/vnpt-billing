package com.hanzo.billing.repository;

import com.hanzo.billing.entity.NapTien;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NapTienRepository extends JpaRepository<NapTien, Long> {

    List<NapTien> findByThueBaoIdOrderByNgayNapDesc(Long thueBaoId);
}
