package com.hanzo.billing.entity;

import com.hanzo.billing.enums.TrangThaiHoaDon;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Hóa đơn cước tháng của một thuê bao trong một kỳ cước.
 *
 * <p>Ràng buộc duy nhất trên cặp (thuê bao, kỳ cước) là chốt chặn nghiệp vụ
 * quan trọng nhất của bảng này: một thuê bao chỉ được lập đúng một hóa đơn cho
 * mỗi kỳ. Nếu engine tính cước chạy lại do lỗi, CSDL sẽ từ chối bản ghi thứ hai
 * thay vì âm thầm tạo hóa đơn trùng.</p>
 */
@Entity
@Table(
        name = "hoa_don",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_hoa_don_thue_bao_ky",
                columnNames = {"thue_bao_id", "ky_cuoc_id"})
)
@Getter
@Setter
@NoArgsConstructor
public class HoaDon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_hoa_don", nullable = false, length = 30, unique = true)
    private String maHoaDon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thue_bao_id", nullable = false)
    private ThueBao thueBao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "khach_hang_id", nullable = false)
    private KhachHang khachHang;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ky_cuoc_id", nullable = false)
    private KyCuoc kyCuoc;

    @Column(name = "ngay_lap", nullable = false)
    private LocalDate ngayLap;

    @Column(name = "han_thanh_toan", nullable = false)
    private LocalDate hanThanhToan;

    @Column(name = "cuoc_thue_bao", precision = 15, scale = 2)
    private BigDecimal cuocThueBao;

    @Column(name = "cuoc_thoai", precision = 15, scale = 2)
    private BigDecimal cuocThoai;

    @Column(name = "cuoc_sms", precision = 15, scale = 2)
    private BigDecimal cuocSms;

    @Column(name = "cuoc_data", precision = 15, scale = 2)
    private BigDecimal cuocData;

    @Column(name = "cuoc_khac", precision = 15, scale = 2)
    private BigDecimal cuocKhac;

    @Column(name = "giam_tru", precision = 15, scale = 2)
    private BigDecimal giamTru;

    @Column(name = "tong_truoc_thue", nullable = false, precision = 15, scale = 2)
    private BigDecimal tongTruocThue;

    @Column(name = "thue_vat", nullable = false, precision = 15, scale = 2)
    private BigDecimal thueVat;

    @Column(name = "tong_thanh_toan", nullable = false, precision = 15, scale = 2)
    private BigDecimal tongThanhToan;

    @Column(name = "da_thanh_toan", precision = 15, scale = 2)
    private BigDecimal daThanhToan;

    @Column(name = "con_no", nullable = false, precision = 15, scale = 2)
    private BigDecimal conNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThaiHoaDon trangThai;

    /**
     * Các dòng khoản mục của hóa đơn. Đây là quan hệ @OneToMany duy nhất trong
     * dự án vì chi tiết hóa đơn không tồn tại độc lập — xoá hóa đơn thì xoá theo,
     * đúng với ràng buộc ON DELETE CASCADE ở CSDL.
     */
    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietHoaDon> chiTiet = new ArrayList<>();
}
