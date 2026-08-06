package com.hanzo.billing.entity;

import com.hanzo.billing.enums.HinhThucNapTien;
import com.hanzo.billing.enums.LoaiBienDongSoDu;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Một dòng sổ cái biến động số dư của thuê bao trả trước.
 *
 * <p>Trước Phase 5 bảng này tên là {@code nap_tien} và chỉ ghi chiều nạp. Trừ cước khi đó
 * không để lại vết nào — trừ xong chỉ còn {@code thue_bao.so_du} đã đổi, không truy nguyên
 * được. Mục G1 tổng quát hoá chính bảng cũ thay vì thêm bảng thứ hai: hai bảng cùng ghi
 * số dư là hai nguồn sự thật.
 *
 * <p>{@link #soTien} luôn dương, chiều do {@link #loaiBienDong} quyết định.
 * {@link #soDuTruoc} / {@link #soDuSau} là thứ làm bảng này thành <b>sổ cái</b> chứ không
 * phải một danh sách giao dịch.
 */
@Entity
@Table(name = "bien_dong_so_du")
@Getter
@Setter
@NoArgsConstructor
public class BienDongSoDu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thue_bao_id", nullable = false)
    private ThueBao thueBao;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai_bien_dong", nullable = false)
    private LoaiBienDongSoDu loaiBienDong;

    /** Luôn dương. Chiều cộng/trừ lấy từ {@link #loaiBienDong}. */
    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien;

    /** Số dư trước và sau giao dịch, lưu lại để đối soát về sau. */
    @Column(name = "so_du_truoc", precision = 15, scale = 2)
    private BigDecimal soDuTruoc;

    @Column(name = "so_du_sau", precision = 15, scale = 2)
    private BigDecimal soDuSau;

    /** Chỉ có nghĩa với dòng {@code NAP_TIEN}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "hinh_thuc")
    private HinhThucNapTien hinhThuc;

    /** Chỉ có giá trị với dòng {@code TRU_CUOC} — trừ cước luôn gắn với một kỳ. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ky_cuoc_id")
    private KyCuoc kyCuoc;

    /**
     * Số bản ghi CDR thực sự được trừ. Chỉ có giá trị với dòng {@code TRU_CUOC}.
     *
     * <p>Ít hơn tổng số bản ghi của thuê bao trong kỳ khi quỹ cạn giữa chừng.</p>
     */
    @Column(name = "so_cdr_da_tru")
    private Integer soCdrDaTru;

    /**
     * Phần cước <b>không thu được</b> vì hết số dư giữa chừng.
     *
     * <p><b>Đây là hiện vật của việc trừ theo lô, không phải một khoản nợ.</b> Thuê bao trả
     * trước không có nợ: với trả trước thời gian thực, cuộc gọi làm cạn số dư sẽ bị chặn ngay
     * nên số tiền này không bao giờ phát sinh. Nó chỉ tồn tại vì hệ thống mô phỏng định giá
     * trước rồi mới trừ sau, theo lô cuối kỳ.</p>
     *
     * <p>Vì vậy cột này ghi lại <i>chênh lệch của mô hình</i> để đối soát được, chứ
     * <b>không</b> phải một khoản phải thu — cố ý không có bảng nợ cho thuê bao trả trước.</p>
     */
    @Column(name = "so_tien_khong_thu_duoc", precision = 15, scale = 2)
    private BigDecimal soTienKhongThuDuoc;

    @Column(name = "ngay_ghi_nhan", nullable = false)
    private LocalDateTime ngayGhiNhan;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_thuc_hien_id")
    private NguoiDung nguoiThucHien;

    /** Số tiền đã áp dấu, dùng khi cộng dồn kiểm bất biến sổ cái. */
    @Transient
    public BigDecimal getSoTienCoDau() {
        return loaiBienDong.apDauCho(soTien);
    }
}
