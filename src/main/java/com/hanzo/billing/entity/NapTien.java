package com.hanzo.billing.entity;

import com.hanzo.billing.enums.HinhThucNapTien;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Giao dịch nạp tiền vào tài khoản thuê bao trả trước. */
@Entity
@Table(name = "nap_tien")
@Getter
@Setter
@NoArgsConstructor
public class NapTien {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thue_bao_id", nullable = false)
    private ThueBao thueBao;

    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien;

    /** Số dư trước và sau giao dịch, lưu lại để đối soát về sau. */
    @Column(name = "so_du_truoc", precision = 15, scale = 2)
    private BigDecimal soDuTruoc;

    @Column(name = "so_du_sau", precision = 15, scale = 2)
    private BigDecimal soDuSau;

    @Enumerated(EnumType.STRING)
    @Column(name = "hinh_thuc")
    private HinhThucNapTien hinhThuc;

    @Column(name = "ngay_nap", nullable = false)
    private LocalDateTime ngayNap;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_id")
    private NguoiDung nguoiThucHien;
}
