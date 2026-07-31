package com.hanzo.billing.entity;

import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Thuê bao điện thoại thuộc về một khách hàng. */
@Entity
@Table(name = "thue_bao")
@Getter
@Setter
@NoArgsConstructor
public class ThueBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "so_thue_bao", nullable = false, length = 15, unique = true)
    private String soThueBao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goi_cuoc_id", nullable = false)
    private GoiCuoc goiCuoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thue_bao", nullable = false)
    private LoaiThueBao loaiThueBao;

    @Column(name = "ngay_kich_hoat", nullable = false)
    private LocalDate ngayKichHoat;

    @Column(name = "ngay_huy")
    private LocalDate ngayHuy;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThaiThueBao trangThai;

    /** Số dư tài khoản, chỉ có ý nghĩa với thuê bao trả trước. */
    @Column(name = "so_du", precision = 15, scale = 2)
    private BigDecimal soDu;

    /** Hạn mức tín dụng, chỉ có ý nghĩa với thuê bao trả sau. */
    @Column(name = "han_muc_tin_dung", precision = 15, scale = 2)
    private BigDecimal hanMucTinDung;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;
}
