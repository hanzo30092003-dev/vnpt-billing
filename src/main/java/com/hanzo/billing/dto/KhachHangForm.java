package com.hanzo.billing.dto;

import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.enums.LoaiKhachHang;
import com.hanzo.billing.enums.TrangThaiKhachHang;
import com.hanzo.billing.validation.GiayToHopLe;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * DTO cho form thêm / sửa khách hàng.
 *
 * <p>Không bind thẳng entity {@link KhachHang} ra view để form không thể vô tình
 * ghi đè những trường không nằm trên màn hình (ví dụ {@code maKh} do hệ thống sinh).</p>
 */
@Getter
@Setter
@NoArgsConstructor
@GiayToHopLe
public class KhachHangForm {

    /** Null khi thêm mới, có giá trị khi sửa. */
    private Long id;

    /** Chỉ để hiển thị, hệ thống tự sinh nên không cho sửa. */
    private String maKh;

    @NotNull(message = "Vui lòng chọn loại khách hàng")
    private LoaiKhachHang loaiKh;

    @NotBlank(message = "Vui lòng nhập tên khách hàng")
    @Size(max = 200, message = "Tên khách hàng tối đa 200 ký tự")
    private String tenKh;

    @NotBlank(message = "Vui lòng nhập số giấy tờ")
    @Size(max = 30, message = "Số giấy tờ tối đa 30 ký tự")
    private String soGiayTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Past(message = "Ngày sinh phải là ngày trong quá khứ")
    private LocalDate ngaySinh;

    @Size(max = 100, message = "Tên người đại diện tối đa 100 ký tự")
    private String nguoiDaiDien;

    @NotBlank(message = "Vui lòng nhập địa chỉ")
    @Size(max = 300, message = "Địa chỉ tối đa 300 ký tự")
    private String diaChi;

    @Pattern(regexp = "^$|^0\\d{9,10}$",
            message = "Số điện thoại phải bắt đầu bằng 0 và gồm 10 hoặc 11 chữ số")
    private String dienThoaiLh;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotNull(message = "Vui lòng chọn ngày đăng ký")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate ngayDangKy;

    private TrangThaiKhachHang trangThai;

    private String ghiChu;

    /** Đổ dữ liệu từ entity sang form khi mở màn hình sửa. */
    public static KhachHangForm tuEntity(KhachHang kh) {
        KhachHangForm form = new KhachHangForm();
        form.setId(kh.getId());
        form.setMaKh(kh.getMaKh());
        form.setLoaiKh(kh.getLoaiKh());
        form.setTenKh(kh.getTenKh());
        form.setSoGiayTo(kh.getSoGiayTo());
        form.setNgaySinh(kh.getNgaySinh());
        form.setNguoiDaiDien(kh.getNguoiDaiDien());
        form.setDiaChi(kh.getDiaChi());
        form.setDienThoaiLh(kh.getDienThoaiLh());
        form.setEmail(kh.getEmail());
        form.setNgayDangKy(kh.getNgayDangKy());
        form.setTrangThai(kh.getTrangThai());
        form.setGhiChu(kh.getGhiChu());
        return form;
    }
}
