package com.hanzo.billing.service;

import com.hanzo.billing.dto.NguoiDungForm;
import com.hanzo.billing.entity.NguoiDung;

import java.util.List;

/**
 * Nghiệp vụ quản lý tài khoản đăng nhập.
 *
 * <p>Trước khi có phân hệ này, thêm một nhân viên mới phải <b>chạy câu SQL bằng tay</b> kèm
 * một chuỗi hash BCrypt sinh sẵn ở đâu đó. Nghĩa là: người vận hành hệ thống phải biết SQL,
 * và mọi lần thêm người đều đi thẳng vào CSDL không qua một lớp kiểm tra nào.</p>
 */
public interface NguoiDungService {

    /** Toàn bộ tài khoản, sắp theo id. Hệ thống chỉ có vài tài khoản nên không cần phân trang. */
    List<NguoiDung> danhSach();

    NguoiDung layTheoId(Long id);

    /**
     * Thêm mới hoặc cập nhật tài khoản.
     *
     * <p>Khi sửa, để trống ô mật khẩu nghĩa là <b>giữ nguyên</b> mật khẩu cũ.</p>
     */
    NguoiDung luu(NguoiDungForm form);

    /**
     * Khoá tài khoản: không đăng nhập được nữa, và <b>phiên đang mở bị đá ra ngay</b>.
     *
     * <p>Không xoá bản ghi. Tài khoản là người đã ký tên trong sổ nhật ký, trong phiếu thu và
     * trong lịch sử thuê bao — xoá đi là xoá luôn khả năng truy trách nhiệm, chưa kể vướng
     * khoá ngoại.</p>
     */
    void khoaTaiKhoan(Long id);

    /**
     * Mở khoá tài khoản — gỡ cả hai loại khoá cùng lúc.
     *
     * <p>Hệ thống có hai cơ chế khoá khác nhau: quản trị viên khoá tay
     * ({@code trang_thai = 0}) và khoá tạm 15 phút do nhập sai mật khẩu 5 lần
     * ({@code khoa_den_luc}). Người dùng gọi điện lên chỉ nói được "tôi không vào được" —
     * họ không phân biệt được hai thứ đó, nên nút mở khoá cũng không bắt họ phân biệt.</p>
     */
    void moKhoaTaiKhoan(Long id);
}
