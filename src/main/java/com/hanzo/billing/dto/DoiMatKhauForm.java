package com.hanzo.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DTO cho form tự đổi mật khẩu của người đang đăng nhập.
 *
 * <p><b>Vì sao phải nhập lại mật khẩu hiện tại</b> — dù người dùng rõ ràng đã đăng nhập rồi:
 * một phiên đang mở <i>không</i> chứng minh người ngồi trước máy là chủ tài khoản. Quầy giao
 * dịch là chỗ máy để không khoá màn hình suốt ngày. Không hỏi mật khẩu cũ thì bất kỳ ai đi
 * ngang một máy bỏ trống cũng đổi được mật khẩu và chiếm hẳn tài khoản — nạn nhân mất quyền
 * vào hệ thống, còn sổ nhật ký từ đó về sau ghi tên họ cho việc người khác làm.</p>
 *
 * <p>Ô xác nhận có mặt vì mật khẩu gõ ra dấu chấm: gõ nhầm một ký tự mà không có ô thứ hai để
 * đối chiếu thì người dùng tự khoá mình ra ngoài, và phải đi tìm quản trị viên.</p>
 */
@Getter
@Setter
@NoArgsConstructor
public class DoiMatKhauForm {

    @NotBlank(message = "Vui lòng nhập mật khẩu hiện tại")
    private String matKhauCu;

    /** Giới hạn 72: BCrypt cắt cụt mọi thứ dài hơn 72 byte mà không báo gì. */
    @NotBlank(message = "Vui lòng nhập mật khẩu mới")
    @Size(max = 72, message = "Mật khẩu tối đa 72 ký tự")
    private String matKhauMoi;

    @NotBlank(message = "Vui lòng nhập lại mật khẩu mới")
    private String xacNhanMatKhauMoi;
}
