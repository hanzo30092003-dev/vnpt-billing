package com.hanzo.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Ghi vết thao tác của người dùng trên hệ thống (audit log). */
@Entity
@Table(name = "nhat_ky_he_thong")
@Getter
@Setter
@NoArgsConstructor
public class NhatKyHeThong {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_dung_id")
    private NguoiDung nguoiDung;

    @Column(name = "hanh_dong", nullable = false, length = 100)
    private String hanhDong;

    /** Tên loại đối tượng bị tác động, ví dụ "THUE_BAO", "HOA_DON". */
    @Column(name = "doi_tuong", length = 50)
    private String doiTuong;

    @Column(name = "doi_tuong_id")
    private Long doiTuongId;

    @Column(name = "noi_dung", columnDefinition = "TEXT")
    private String noiDung;

    @Column(name = "dia_chi_ip", length = 45)
    private String diaChiIp;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;
}
