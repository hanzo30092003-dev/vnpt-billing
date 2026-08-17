package com.hanzo.billing.entity;

import com.hanzo.billing.enums.VaiTro;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** Tài khoản đăng nhập hệ thống. */
@Entity
@Table(name = "nguoi_dung")
@Getter
@Setter
@NoArgsConstructor
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_dang_nhap", nullable = false, length = 50, unique = true)
    private String tenDangNhap;

    /** Mật khẩu đã băm bằng BCrypt, không bao giờ lưu dạng thô. */
    @Column(name = "mat_khau", nullable = false, length = 100)
    private String matKhau;

    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    @Column(name = "email", length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "vai_tro", nullable = false)
    private VaiTro vaiTro;

    /** Cột CSDL là TINYINT (0/1), khai báo JdbcTypeCode để khớp đúng kiểu cột. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(name = "trang_thai")
    private Boolean trangThai;

    @Column(name = "ngay_tao")
    private LocalDateTime ngayTao;

    /**
     * Số lần nhập sai mật khẩu <b>liên tiếp</b>. Đăng nhập đúng một lần là về 0.
     *
     * <p>Trước khi có cột này, một kẻ dò mật khẩu có thể thử vô hạn lần với tốc độ tối đa mà
     * hệ thống không hề chậm lại hay ghi vết gì. Ba tài khoản demo lại dùng chung mật khẩu
     * ngắn, nên đó không phải rủi ro lý thuyết.</p>
     */
    @Column(name = "so_lan_sai", nullable = false)
    private Integer soLanSai;

    /**
     * Khoá tạm tới thời điểm này. {@code null} nghĩa là không bị khoá.
     *
     * <p>Chọn khoá <b>tạm theo thời gian</b> chứ không khoá vĩnh viễn: khoá vĩnh viễn biến một
     * lỗi gõ nhầm thành việc phải đi tìm quản trị viên, và biến chính cơ chế bảo vệ thành một
     * cách <i>tấn công từ chối dịch vụ</i> — kẻ xấu chỉ cần nhập sai 5 lần vào tài khoản người
     * khác là khoá được họ vô thời hạn.</p>
     */
    @Column(name = "khoa_den_luc")
    private LocalDateTime khoaDenLuc;

    /** Tài khoản có đang trong thời gian bị khoá tạm không. */
    public boolean dangBiKhoa() {
        return khoaDenLuc != null && khoaDenLuc.isAfter(LocalDateTime.now());
    }
}
