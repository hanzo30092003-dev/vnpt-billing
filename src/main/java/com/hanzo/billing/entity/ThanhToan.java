package com.hanzo.billing.entity;

import com.hanzo.billing.enums.HinhThucThanhToan;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Giao dịch khách hàng thanh toán cho một hóa đơn. */
@Entity
@Table(name = "thanh_toan")
@Getter
@Setter
@NoArgsConstructor
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_giao_dich", nullable = false, length = 30, unique = true)
    private String maGiaoDich;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hoa_don_id", nullable = false)
    private HoaDon hoaDon;

    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien;

    @Enumerated(EnumType.STRING)
    @Column(name = "hinh_thuc", nullable = false)
    private HinhThucThanhToan hinhThuc;

    @Column(name = "ngay_thanh_toan", nullable = false)
    private LocalDateTime ngayThanhToan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thu_id")
    private NguoiDung nguoiThu;

    @Column(name = "ghi_chu", length = 300)
    private String ghiChu;
}
