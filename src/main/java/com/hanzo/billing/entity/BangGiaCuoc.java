package com.hanzo.billing.entity;

import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bảng giá cước theo dịch vụ / hướng / khung giờ.
 *
 * <p>Khi {@code goiCuoc} null nghĩa là đơn giá mặc định áp dụng chung cho mọi gói.
 * Engine tính cước sẽ ưu tiên bản ghi gắn với gói cước cụ thể trước.</p>
 */
@Entity
@Table(name = "bang_gia_cuoc")
@Getter
@Setter
@NoArgsConstructor
public class BangGiaCuoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goi_cuoc_id")
    private GoiCuoc goiCuoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_dich_vu", nullable = false)
    private LoaiDichVu loaiDichVu;

    @Enumerated(EnumType.STRING)
    @Column(name = "huong", nullable = false)
    private HuongCuocGoi huong;

    /** Cột CSDL là TINYINT (0/1), khai báo JdbcTypeCode để khớp đúng kiểu cột. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "gio_cao_diem")
    private Boolean gioCaoDiem;

    /** Đơn vị tính block: giây với thoại, 1 tin với SMS, 1 MB với data. */
    @Column(name = "block_giay", nullable = false)
    private Integer blockGiay;

    @Column(name = "don_gia", nullable = false, precision = 15, scale = 2)
    private BigDecimal donGia;

    @Column(name = "ngay_hieu_luc", nullable = false)
    private LocalDate ngayHieuLuc;

    @Column(name = "ngay_het_hieu_luc")
    private LocalDate ngayHetHieuLuc;
}
