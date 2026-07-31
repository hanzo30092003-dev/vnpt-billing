package com.hanzo.billing.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Nhật ký đổi trạng thái của thuê bao, phục vụ tra cứu và đối soát. */
@Entity
@Table(name = "lich_su_thue_bao")
@Getter
@Setter
@NoArgsConstructor
public class LichSuThueBao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thue_bao_id", nullable = false)
    private ThueBao thueBao;

    @Column(name = "trang_thai_cu", length = 20)
    private String trangThaiCu;

    @Column(name = "trang_thai_moi", nullable = false, length = 20)
    private String trangThaiMoi;

    @Column(name = "ly_do", length = 300)
    private String lyDo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_id")
    private NguoiDung nguoiThucHien;

    @Column(name = "thoi_gian", nullable = false)
    private LocalDateTime thoiGian;
}
