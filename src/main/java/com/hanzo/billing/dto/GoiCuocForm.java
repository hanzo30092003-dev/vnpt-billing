package com.hanzo.billing.dto;

import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.validation.CoKhoangHieuLuc;
import com.hanzo.billing.validation.KhoangHieuLucHopLe;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO cho form thêm / sửa gói cước. */
@Getter
@Setter
@NoArgsConstructor
@KhoangHieuLucHopLe
public class GoiCuocForm implements CoKhoangHieuLuc {

    private Long id;

    @NotBlank(message = "Vui lòng nhập mã gói")
    @Size(max = 20, message = "Mã gói tối đa 20 ký tự")
    @Pattern(regexp = "^[A-Z0-9_]+$", message = "Mã gói chỉ gồm chữ IN HOA, chữ số và dấu gạch dưới")
    private String maGoi;

    @NotBlank(message = "Vui lòng nhập tên gói")
    @Size(max = 100, message = "Tên gói tối đa 100 ký tự")
    private String tenGoi;

    @NotNull(message = "Vui lòng chọn loại thuê bao")
    private LoaiThueBao loaiThueBao;

    @NotNull(message = "Vui lòng nhập cước thuê bao tháng")
    @PositiveOrZero(message = "Cước thuê bao tháng không được âm")
    private BigDecimal cuocThueBaoThang;

    @NotNull(message = "Vui lòng nhập số phút nội mạng miễn phí")
    @PositiveOrZero(message = "Số phút nội mạng miễn phí không được âm")
    private Integer phutNoiMangMienPhi;

    @NotNull(message = "Vui lòng nhập số phút ngoại mạng miễn phí")
    @PositiveOrZero(message = "Số phút ngoại mạng miễn phí không được âm")
    private Integer phutNgoaiMangMienPhi;

    @NotNull(message = "Vui lòng nhập số SMS miễn phí")
    @PositiveOrZero(message = "Số SMS miễn phí không được âm")
    private Integer smsMienPhi;

    @NotNull(message = "Vui lòng nhập dung lượng data miễn phí")
    @PositiveOrZero(message = "Dung lượng data miễn phí không được âm")
    private Integer dataMienPhiMb;

    private String moTa;

    @NotNull(message = "Vui lòng chọn ngày hiệu lực")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayHieuLuc;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayHetHieuLuc;

    private Boolean trangThai = Boolean.TRUE;

    public static GoiCuocForm tuEntity(GoiCuoc gc) {
        GoiCuocForm f = new GoiCuocForm();
        f.setId(gc.getId());
        f.setMaGoi(gc.getMaGoi());
        f.setTenGoi(gc.getTenGoi());
        f.setLoaiThueBao(gc.getLoaiThueBao());
        f.setCuocThueBaoThang(gc.getCuocThueBaoThang());
        f.setPhutNoiMangMienPhi(gc.getPhutNoiMangMienPhi());
        f.setPhutNgoaiMangMienPhi(gc.getPhutNgoaiMangMienPhi());
        f.setSmsMienPhi(gc.getSmsMienPhi());
        f.setDataMienPhiMb(gc.getDataMienPhiMb());
        f.setMoTa(gc.getMoTa());
        f.setNgayHieuLuc(gc.getNgayHieuLuc());
        f.setNgayHetHieuLuc(gc.getNgayHetHieuLuc());
        f.setTrangThai(gc.getTrangThai());
        return f;
    }
}
