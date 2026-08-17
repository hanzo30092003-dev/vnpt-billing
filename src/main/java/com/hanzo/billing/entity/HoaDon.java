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
     * Số phiên bản dòng, dùng cho khoá lạc quan.
     *
     * <p><b>Vì sao phải có.</b> {@code ThanhToanService.ghiNhan} là một chuỗi
     * <i>đọc → tính → ghi</i>: đọc {@code con_no}, kiểm số tiền không vượt quá nó, rồi ghi
     * {@code da_thanh_toan = da_thanh_toan_cũ + số vừa thu}. Không có gì khoá dòng giữa bước
     * đọc và bước ghi, nên hai thu ngân cùng thu trên một hóa đơn sẽ cùng đọc một giá trị cũ
     * và cùng ghi đè lên nhau:</p>
     *
     * <pre>
     *   Hóa đơn còn nợ 100.000, hai người mỗi người thu 50.000
     *     A đọc con_no = 100.000        B đọc con_no = 100.000
     *     A qua kiểm (50.000 ≤ 100.000) B qua kiểm (50.000 ≤ 100.000)
     *     A ghi da_thanh_toan = 50.000  B ghi da_thanh_toan = 50.000   ← đè lên A
     *   Kết quả: 2 dòng thanh toán tổng 100.000, nhưng da_thanh_toan chỉ 50.000
     * </pre>
     *
     * <p>Nếu xảy ra thì hỏng đúng <b>bất biến trung tâm</b> của cả dự án:
     * {@code da_thanh_toan = SUM(thanh_toan.so_tien)}. Và
     * {@code KiemTraBatBienThanhToanTest} không bao giờ bắt được, vì nó chạy <i>sau</i>, trên
     * dữ liệu đã đứng yên — lúc đó tiền đã mất rồi.</p>
     *
     * <p><b>Đã dựng lại được, không phải suy đoán.</b> Gỡ annotation này ra rồi chạy
     * {@code KiemTraDongThoiThanhToanTest.epDocDocGhiGhi_benGhiSauBiTuChoi}: cả hai bên cùng
     * ghi thành công nhưng số tiền chỉ tăng <b>1.000 thay vì 2.000</b> — mất đúng một lần cộng.
     * Gắn lại: đúng một bên thành công.</p>
     *
     * <p>⚠️ Cách dựng lại <b>đầu tiên</b> — thả 12 luồng cùng gọi {@code ghiNhan} rồi mong
     * chúng giao nhau — <b>không</b> dựng lại được, vì thả cùng lúc không bảo đảm hai bên cùng
     * <i>đọc xong</i> trước khi bên nào <i>kịp ghi</i>. Phải ép thứ tự bằng chốt chặn thì lỗi
     * mới lộ. Một phép kiểm không đỏ chưa chứng minh được là không có lỗi.</p>
     *
     * <p>Với {@code @Version}, Hibernate thêm {@code WHERE phien_ban = ?} vào câu UPDATE; người
     * ghi sau thấy 0 dòng bị ảnh hưởng và nhận {@code ObjectOptimisticLockingFailureException},
     * còn {@code GlobalExceptionHandler} đổi nó thành lời nhắn tiếng Việt bảo mở lại hóa đơn.
     * Thà bắt người dùng làm lại một lần còn hơn im lặng nuốt mất tiền của khách.</p>
     *
     * <p>Chọn khoá <b>lạc quan</b> chứ không phải bi quan vì đụng độ ở đây rất hiếm — hai người
     * thu cùng một hóa đơn trong cùng vài giây — nên trả giá bằng một lần làm lại hiếm hoi rẻ
     * hơn là khoá dòng ở mọi lần thu tiền.</p>
     */
    @Version
    @Column(name = "phien_ban", nullable = false)
    private Long phienBan;

    /**
     * Các dòng khoản mục của hóa đơn. Đây là quan hệ @OneToMany duy nhất trong
     * dự án vì chi tiết hóa đơn không tồn tại độc lập — xoá hóa đơn thì xoá theo,
     * đúng với ràng buộc ON DELETE CASCADE ở CSDL.
     */
    @OneToMany(mappedBy = "hoaDon", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ChiTietHoaDon> chiTiet = new ArrayList<>();
}
