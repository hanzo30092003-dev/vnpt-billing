package com.hanzo.billing.service;

/** Ghi vết thao tác của người dùng vào bảng {@code nhat_ky_he_thong}. */
public interface NhatKyService {

    /**
     * Ghi một dòng nhật ký. Người thực hiện và địa chỉ IP được tự lấy từ ngữ cảnh
     * hiện tại nên nơi gọi không cần truyền vào.
     *
     * @param hanhDong  tên hành động, ví dụ {@code "TAO_KHACH_HANG"}
     * @param doiTuong  loại đối tượng bị tác động, ví dụ {@code "KHACH_HANG"}
     * @param doiTuongId khóa chính của đối tượng
     * @param noiDung   diễn giải chi tiết bằng tiếng Việt
     */
    void ghiNhatKy(String hanhDong, String doiTuong, Long doiTuongId, String noiDung);
}
