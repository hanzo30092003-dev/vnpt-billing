package com.hanzo.billing.exception;

/**
 * Ngoại lệ cho các vi phạm quy tắc NGHIỆP VỤ (không phải lỗi kỹ thuật).
 *
 * <p>Ví dụ: chuyển trạng thái thuê bao không hợp lệ, ngừng giao dịch khách hàng khi
 * còn thuê bao đang hoạt động, nạp tiền cho thuê bao trả sau.</p>
 *
 * <p>Thông điệp của ngoại lệ này được viết bằng tiếng Việt và hiển thị thẳng cho
 * người dùng, nên phải diễn đạt rõ nguyên nhân chứ không dùng thuật ngữ kỹ thuật.
 * {@link GlobalExceptionHandler} chịu trách nhiệm bắt và đưa ra giao diện.</p>
 */
public class NghiepVuException extends RuntimeException {

    public NghiepVuException(String thongDiep) {
        super(thongDiep);
    }
}
