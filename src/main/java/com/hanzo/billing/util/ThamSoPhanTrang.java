package com.hanzo.billing.util;

/**
 * Chuẩn hoá tham số phân trang đến từ URL.
 *
 * <h2>Vì sao cần</h2>
 * <p>Sáu màn hình danh sách đều nhận {@code ?trang=} thẳng từ URL rồi đưa vào
 * {@code PageRequest.of(trang, ...)}. Spring Data <b>ném {@code IllegalArgumentException}</b>
 * khi chỉ số trang âm, và ngoại lệ đó thành trang lỗi HTTP 500 ngay trước mặt người dùng.</p>
 *
 * <p><b>Và đó không phải chuyện giả định.</b> Nút <i>"Trước"</i> ở trang đầu sinh ra
 * {@code ?trang=-1}: lớp {@code disabled} của Bootstrap chỉ làm liên kết <i>trông như</i> bị
 * khoá — nó vẫn là một thẻ {@code <a>} có {@code href} và vẫn bấm được bằng bàn phím, bằng
 * chuột giữa, hoặc bằng cách mở trong tab mới. Lỗi này sống qua năm phase vì mọi phép kiểm đều
 * gõ thẳng URL hợp lệ; chỉ tới khi Phase 7 kiểm bằng cách <b>đi theo liên kết thật</b> nó mới
 * lộ ra.</p>
 *
 * <p>Chặn ở tầng máy chủ chứ không sửa mỗi template: người dùng vẫn gõ được
 * {@code ?trang=-5} vào thanh địa chỉ, và sửa giao diện thì không chặn được đường đó. Đây là
 * bài học 4.4 của Phase 3 áp cho phân trang.</p>
 */
public final class ThamSoPhanTrang {

    /** Chặn trên cho số dòng mỗi trang, phòng {@code ?soDong=1000000} làm cạn bộ nhớ. */
    public static final int SO_DONG_TOI_DA = 200;

    private ThamSoPhanTrang() {
    }

    /**
     * Chỉ số trang hợp lệ: âm thì đưa về 0.
     *
     * <p>Cố ý <b>không</b> ném lỗi: người dùng bấm nhầm nút "Trước" ở trang đầu thì thấy lại
     * trang đầu là hành vi đúng và dễ hiểu, còn một trang báo lỗi thì không.</p>
     */
    public static int trangHopLe(int trang) {
        return Math.max(0, trang);
    }

    /** Số dòng mỗi trang hợp lệ: kẹp vào khoảng {@code [1, SO_DONG_TOI_DA]}. */
    public static int soDongHopLe(int soDong) {
        if (soDong < 1) {
            return 1;
        }
        return Math.min(soDong, SO_DONG_TOI_DA);
    }
}
