package com.hanzo.billing.repository;

import com.hanzo.billing.entity.NhatKyHeThong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NhatKyHeThongRepository extends JpaRepository<NhatKyHeThong, Long> {

    List<NhatKyHeThong> findByNguoiDungIdOrderByThoiGianDesc(Long nguoiDungId);

    List<NhatKyHeThong> findByDoiTuongAndDoiTuongId(String doiTuong, Long doiTuongId);
}
