package com.hanzo.billing.entity;

import com.hanzo.billing.enums.LoaiGiamTru;
import com.hanzo.billing.enums.TrangThaiGiamTru;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Khoản giảm trừ áp dụng cho thuê bao trong một kỳ cước.
 *
 * <p>Giảm trừ có thể khai theo số tiền tuyệt đối ({@code soTien}) hoặc theo
 * tỷ lệ phần trăm ({@code tyLePhanTram}). Engine tính cước ở Phase 4 sẽ quy
 * định thứ tự ưu tiên khi cả hai cùng có giá trị.</p>
 */
@Entity
@Table(name = "giam_tru")
@Getter
@Setter
@NoArgsConstructor
public class GiamTru {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thue_bao_id", nullable = false)
    private ThueBao thueBao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ky_cuoc_id")
    private KyCuoc kyCuoc;

    @Enumerated(EnumType.STRING)
    @Column(name = "loai", nullable = false)
    private LoaiGiamTru loai;

    @Column(name = "so_tien", precision = 15, scale = 2)
    private BigDecimal soTien;

    @Column(name = "ty_le_phan_tram", precision = 5, scale = 2)
    private BigDecimal tyLePhanTram;

    @Column(name = "ly_do", length = 300)
    private String lyDo;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai")
    private TrangThaiGiamTru trangThai;
}
