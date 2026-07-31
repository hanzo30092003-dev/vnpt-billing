package com.hanzo.billing.entity;

import com.hanzo.billing.enums.TrangThaiKyCuoc;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Kỳ tính cước theo tháng. Ràng buộc duy nhất trên cặp (tháng, năm). */
@Entity
@Table(
        name = "ky_cuoc",
        uniqueConstraints = @UniqueConstraint(name = "uq_ky_cuoc_thang_nam", columnNames = {"thang", "nam"})
)
@Getter
@Setter
@NoArgsConstructor
public class KyCuoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "thang", nullable = false)
    private Integer thang;

    @Column(name = "nam", nullable = false)
    private Integer nam;

    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDate ngayBatDau;

    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDate ngayKetThuc;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThaiKyCuoc trangThai;

    @Column(name = "ngay_chot")
    private LocalDateTime ngayChot;

    @Column(name = "so_cdr_xu_ly")
    private Integer soCdrXuLy;

    @Column(name = "so_hoa_don_tao")
    private Integer soHoaDonTao;

    @Column(name = "tong_doanh_thu", precision = 18, scale = 2)
    private BigDecimal tongDoanhThu;
}
