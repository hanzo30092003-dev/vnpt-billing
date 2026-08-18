package com.hanzo.billing.dto;

import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.enums.VaiTro;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho form thêm / sửa tài khoản người dùng.
 *
 * <p><b>Không bind thẳng entity {@link NguoiDung}.</b> Ở đây lý do mạnh hơn hẳn so với các
 * form khác: entity chứa {@code matKhau} (hash BCrypt), {@code soLanSai} và {@code khoaDenLuc}.
 * Bind thẳng nghĩa là một request tự chế thêm mấy trường ẩn là gỡ được khoá tạm của chính
 * mình, hoặc ghi đè hash mật khẩu bằng chuỗi tuỳ ý — mà không màn hình nào để lộ đường đó.</p>
 *
 * <p><b>Trường không có trên form này, và vì sao:</b></p>
 * <ul>
 *   <li>{@code trangThai} — khoá/mở khoá đi bằng nút riêng có xác nhận, không phải một ô chọn
 *       lẫn giữa mười ô khác. Một đường ghi cho một việc.</li>
 *   <li>{@code soLanSai}, {@code khoaDenLuc} — do cơ chế đăng nhập tự đặt, người dùng không
 *       khai được.</li>
 *   <li>{@code ngayTao} — hệ thống đóng dấu lúc tạo.</li>
 * </ul>
 */
@Getter
@Setter
@NoArgsConstructor
public class NguoiDungForm {

    /** Null khi thêm mới, có giá trị khi sửa. */
    private Long id;

    /**
     * Tên đăng nhập. <b>Chỉ nhập được lúc thêm mới</b>, sửa thì hiển thị dạng chỉ đọc.
     *
     * <p>Cho sửa thì phải trả lời: phiên đang mở của người đó còn hiệu lực không, sổ nhật ký
     * ghi tên cũ có còn đọc được không. Không đổi được thì cả hai câu hỏi biến mất — và đổi
     * tên đăng nhập cũng không phải nhu cầu có thật của một phần mềm quầy giao dịch.</p>
     */
    @NotBlank(message = "Vui lòng nhập tên đăng nhập")
    @Size(max = 50, message = "Tên đăng nhập tối đa 50 ký tự")
    @Pattern(regexp = "^[a-z][a-z0-9._]{2,49}$",
            message = "Tên đăng nhập bắt đầu bằng chữ thường, dài từ 3 ký tự, "
                    + "chỉ gồm chữ thường không dấu, chữ số, dấu chấm hoặc gạch dưới "
                    + "— ví dụ nhanvien02")
    private String tenDangNhap;

    @NotBlank(message = "Vui lòng nhập họ tên")
    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String hoTen;

    @Email(message = "Email không đúng định dạng")
    @Size(max = 100, message = "Email tối đa 100 ký tự")
    private String email;

    @NotNull(message = "Vui lòng chọn quyền sử dụng")
    private VaiTro vaiTro;

    /**
     * Mật khẩu dạng thô, <b>chỉ đi một chiều từ form vào</b> — không bao giờ đổ ngược ra view.
     *
     * <p>Bắt buộc khi thêm mới; để trống khi sửa nghĩa là giữ nguyên mật khẩu cũ. Hai luật đó
     * kiểm trong service chứ không đặt ở đây, vì chúng phụ thuộc việc đang thêm hay đang sửa
     * — một ràng buộc {@code @NotBlank} tĩnh sẽ chặn luôn cả thao tác sửa tên hiển thị.</p>
     *
     * <p>Giới hạn 72: BCrypt <b>cắt cụt</b> mọi thứ dài hơn 72 byte mà không báo gì. Không
     * chặn ở đây thì hai mật khẩu khác nhau từ ký tự thứ 73 trở đi lại đăng nhập được cho
     * nhau, và không ai hiểu vì sao.</p>
     */
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    private String matKhau;

    /** Đổ dữ liệu từ entity sang form khi mở màn hình sửa. Cố ý KHÔNG chép mật khẩu. */
    public static NguoiDungForm tuEntity(NguoiDung nd) {
        NguoiDungForm form = new NguoiDungForm();
        form.setId(nd.getId());
        form.setTenDangNhap(nd.getTenDangNhap());
        form.setHoTen(nd.getHoTen());
        form.setEmail(nd.getEmail());
        form.setVaiTro(nd.getVaiTro());
        return form;
    }
}
