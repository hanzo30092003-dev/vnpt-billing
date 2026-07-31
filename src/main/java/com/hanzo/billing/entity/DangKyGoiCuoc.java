package com.hanzo.billing.entity;

import com.hanzo.billing.enums.TrangThaiDangKyGoi;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Lịch sử đăng ký gói cước của thuê bao.
 *
 * <p>Một thuê bao có thể đổi gói nhiều lần; mỗi lần là một bản ghi. Bản ghi
 * đang hiệu lực có trạng thái DANG_AP_DUNG và {@code ngayKetThuc} để trống.</p>
 */
@Entity
@Table(name = "dang_ky_goi_cuoc")
@Getter
@Setter
@NoArgsConstructor
public class DangKyGoiCuoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thue_bao_id", nullable = false)
    private ThueBao thueBao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goi_cuoc_id", nullable = false)
    private GoiCuoc goiCuoc;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc")
    private LocalDate ngayKetThuc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThaiDangKyGoi trangThai;
}
