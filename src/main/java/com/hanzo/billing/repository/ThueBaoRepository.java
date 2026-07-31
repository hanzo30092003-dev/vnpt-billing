package com.hanzo.billing.repository;

import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ThueBaoRepository extends JpaRepository<ThueBao, Long> {

    Optional<ThueBao> findBySoThueBao(String soThueBao);

    boolean existsBySoThueBao(String soThueBao);

    List<ThueBao> findByKhachHangId(Long khachHangId);

    List<ThueBao> findByTrangThai(TrangThaiThueBao trangThai);

    List<ThueBao> findByLoaiThueBao(LoaiThueBao loaiThueBao);

    long countByTrangThai(TrangThaiThueBao trangThai);

    long countByLoaiThueBao(LoaiThueBao loaiThueBao);
}
