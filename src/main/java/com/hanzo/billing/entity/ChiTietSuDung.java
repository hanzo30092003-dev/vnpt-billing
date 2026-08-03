package com.hanzo.billing.entity;

import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.NguonCdr;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bản ghi chi tiết sử dụng dịch vụ — CDR (Call Detail Record).
 *
 * <p>Đây là dữ liệu đầu vào của engine tính cước ở Phase 4. Trường
 * {@code soThueBao} được lưu lặp lại (bên cạnh khoá ngoại) vì CDR là dữ liệu
 * lịch sử: nếu sau này thuê bao bị thanh lý và số được cấp lại cho khách khác,
 * bản ghi cũ vẫn giữ đúng số tại thời điểm phát sinh.</p>
 */
@Entity
@Table(name = "chi_tiet_su_dung")
@Getter
@Setter
@NoArgsConstructor
public class ChiTietSuDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thue_bao_id", nullable = false)
    private ThueBao thueBao;

    @Column(name = "so_thue_bao", nullable = false, length = 15)
    private String soThueBao;

    @Column(name = "so_bi_goi", length = 20)
    private String soBiGoi;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_dich_vu", nullable = false)
    private LoaiDichVu loaiDichVu;

    @Enumerated(EnumType.STRING)
    @Column(name = "huong", nullable = false)
    private HuongCuocGoi huong;

    @Column(name = "thoi_gian_bat_dau", nullable = false)
    private LocalDateTime thoiGianBatDau;

    /** Thời lượng cuộc gọi tính bằng giây, chỉ dùng cho dịch vụ THOAI. */
    @Column(name = "thoi_luong_giay")
    private Integer thoiLuongGiay;

    /**
     * Số tin nhắn với SMS, số <b>KB</b> với DATA.
     *
     * <p>⚠️ Đơn vị của DATA là KB, <b>không phải MB</b>. Ưu đãi của gói lại khai bằng MB
     * ({@code goi_cuoc.data_mien_phi_mb}), nên mọi phép so sánh và nhân đơn giá đều phải
     * đi qua {@code DonViCuoc.kbSangMb()}. Quên chỗ nào cũng cho ra hóa đơn sai 1024 lần
     * mà không có cảnh báo nào — xem {@code docs/mo-ta-csdl.md} mục 6.</p>
     */
    @Column(name = "so_luong")
    private Integer soLuong;

    /** Cột CSDL là TINYINT (0/1), khai báo JdbcTypeCode để khớp đúng kiểu cột. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "gio_cao_diem")
    private Boolean gioCaoDiem;

    @Column(name = "cuoc_phi", precision = 15, scale = 2)
    private BigDecimal cuocPhi;

    /** Đánh dấu bản ghi được miễn phí do nằm trong ưu đãi của gói cước. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "mien_phi")
    private Boolean mienPhi;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_tinh_cuoc", nullable = false)
    private TrangThaiTinhCuoc trangThaiTinhCuoc;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ky_cuoc_id")
    private KyCuoc kyCuoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "nguon")
    private NguonCdr nguon;
}
