package com.hanzo.billing.dto;

import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.validation.CoKhoangHieuLuc;
import com.hanzo.billing.validation.KhoangHieuLucHopLe;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** DTO cho form thêm / sửa một dòng bảng giá cước. */
@Getter
@Setter
@NoArgsConstructor
@KhoangHieuLucHopLe
public class BangGiaForm implements CoKhoangHieuLuc {

    private Long id;

    /** Null nghĩa là "Giá mặc định chung", áp dụng cho mọi gói cước. */
    private Long goiCuocId;

    @NotNull(message = "Vui lòng chọn loại dịch vụ")
    private LoaiDichVu loaiDichVu;

    @NotNull(message = "Vui lòng chọn hướng")
    private HuongCuocGoi huong;

    @NotNull(message = "Vui lòng chọn khung giờ")
    private Boolean gioCaoDiem = Boolean.FALSE;

    @NotNull(message = "Vui lòng nhập đơn vị tính tiền")
    @Positive(message = "Đơn vị tính tiền phải lớn hơn 0")
    private Integer blockGiay;

    @NotNull(message = "Vui lòng nhập đơn giá")
    @PositiveOrZero(message = "Đơn giá không được âm")
    private BigDecimal donGia;

    @NotNull(message = "Vui lòng chọn ngày hiệu lực")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayHieuLuc;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayHetHieuLuc;

    public static BangGiaForm tuEntity(BangGiaCuoc bg) {
        BangGiaForm f = new BangGiaForm();
        f.setId(bg.getId());
        f.setGoiCuocId(bg.getGoiCuoc() == null ? null : bg.getGoiCuoc().getId());
        f.setLoaiDichVu(bg.getLoaiDichVu());
        f.setHuong(bg.getHuong());
        f.setGioCaoDiem(Boolean.TRUE.equals(bg.getGioCaoDiem()));
        f.setBlockGiay(bg.getBlockGiay());
        f.setDonGia(bg.getDonGia());
        f.setNgayHieuLuc(bg.getNgayHieuLuc());
        f.setNgayHetHieuLuc(bg.getNgayHetHieuLuc());
        return f;
    }
}
