package com.hanzo.billing.entity;

import com.hanzo.billing.enums.LoaiKhachHang;
import com.hanzo.billing.enums.TrangThaiKhachHang;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/** Khách hàng cá nhân hoặc doanh nghiệp. */
@Entity
@Table(name = "khach_hang")
@Getter
@Setter
@NoArgsConstructor
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_kh", nullable = false, length = 20, unique = true)
    private String maKh;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_kh", nullable = false)
    private LoaiKhachHang loaiKh;

    @Column(name = "ten_kh", nullable = false, length = 200)
    private String tenKh;

    /** CCCD với khách cá nhân, mã số thuế với doanh nghiệp. */
    @Column(name = "so_giay_to", nullable = false, length = 30)
    private String soGiayTo;

    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    /** Chỉ dùng cho khách doanh nghiệp. */
    @Column(name = "nguoi_dai_dien", length = 100)
    private String nguoiDaiDien;

    @Column(name = "dia_chi", nullable = false, length = 300)
    private String diaChi;

    @Column(name = "dien_thoai_lh", length = 15)
    private String dienThoaiLh;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "ngay_dang_ky", nullable = false)
    private LocalDate ngayDangKy;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false)
    private TrangThaiKhachHang trangThai;

    @Column(name = "ghi_chu", columnDefinition = "TEXT")
    private String ghiChu;
}
