package com.hanzo.billing.entity;

import com.hanzo.billing.enums.LoaiThueBao;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Gói cước áp dụng cho thuê bao. */
@Entity
@Table(name = "goi_cuoc")
@Getter
@Setter
@NoArgsConstructor
public class GoiCuoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_goi", nullable = false, length = 20, unique = true)
    private String maGoi;

    @Column(name = "ten_goi", nullable = false, length = 100)
    private String tenGoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_thue_bao", nullable = false)
    private LoaiThueBao loaiThueBao;

    /** Cước thuê bao cố định hàng tháng. */
    @Column(name = "cuoc_thue_bao_thang", nullable = false, precision = 15, scale = 2)
    private BigDecimal cuocThueBaoThang;

    @Column(name = "phut_noi_mang_mien_phi")
    private Integer phutNoiMangMienPhi;

    @Column(name = "phut_ngoai_mang_mien_phi")
    private Integer phutNgoaiMangMienPhi;

    @Column(name = "sms_mien_phi")
    private Integer smsMienPhi;

    @Column(name = "data_mien_phi_mb")
    private Integer dataMienPhiMb;

    @Column(name = "mo_ta", columnDefinition = "TEXT")
    private String moTa;

    @Column(name = "ngay_hieu_luc", nullable = false)
    private LocalDate ngayHieuLuc;

    @Column(name = "ngay_het_hieu_luc")
    private LocalDate ngayHetHieuLuc;

    /** Cột CSDL là TINYINT (0/1), khai báo JdbcTypeCode để khớp đúng kiểu cột. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "trang_thai")
    private Boolean trangThai;
}
