package com.hanzo.billing.enums;

/**
 * Nhóm tuổi nợ của một hóa đơn — dùng cho bảng aging.
 *
 * <p>Ranh giới đặt trong chính enum thay vì rải ra service và template: bảng aging, biểu đồ
 * và bộ lọc đều phải chia nhóm <b>giống hệt nhau</b>. Ba nơi tự chia thì sẽ có lúc ba nơi
 * ra ba con số khác nhau cho cùng một tập hóa đơn.</p>
 *
 * <h2>Màu cũng là thông tin, nên cũng chỉ có một nguồn</h2>
 * <p>Trước đây enum giữ tên lớp badge của Bootstrap (chỉ có 3 màu dùng được: xám / vàng / đỏ)
 * còn biểu đồ khai riêng 5 mã màu trong template. Hậu quả: <i>Quá hạn 1–30</i> và
 * <i>31–60</i> cùng vàng ở badge nhưng vàng và cam ở biểu đồ — cùng một nhóm hiện hai màu ở
 * hai khối nằm cạnh nhau. Đó là <b>mâu thuẫn thông tin</b>, không phải chuyện thẩm mỹ: người
 * đọc buộc phải đoán xem cột cam ứng với dòng nào.</p>
 *
 * <p>Nay enum giữ thẳng <b>mã màu</b>. Badge tô bằng {@code style} nội tuyến, biểu đồ đọc
 * cùng danh sách ấy — không còn bảng màu thứ hai để lệch. Cùng cách
 * {@link LoaiBienDongSoDu} giữ quy tắc dấu ở đúng một chỗ.</p>
 */
public enum NhomTuoiNo {

    TRONG_HAN("Trong hạn", "#6c757d", "#ffffff", Integer.MIN_VALUE, 0),
    QUA_HAN_1_30("Quá hạn 1–30 ngày", "#ffc107", "#212529", 1, 30),
    QUA_HAN_31_60("Quá hạn 31–60 ngày", "#fd7e14", "#ffffff", 31, 60),
    QUA_HAN_61_90("Quá hạn 61–90 ngày", "#dc3545", "#ffffff", 61, 90),
    QUA_HAN_TREN_90("Quá hạn trên 90 ngày", "#8b0000", "#ffffff", 91, Integer.MAX_VALUE);

    private final String nhan;
    private final String mauNen;

    /** Màu chữ đi kèm — vàng phải dùng chữ đen mới đủ tương phản để đọc. */
    private final String mauChu;

    private final int tuNgay;
    private final int denNgay;

    NhomTuoiNo(String nhan, String mauNen, String mauChu, int tuNgay, int denNgay) {
        this.nhan = nhan;
        this.mauNen = mauNen;
        this.mauChu = mauChu;
        this.tuNgay = tuNgay;
        this.denNgay = denNgay;
    }

    /**
     * Nhóm tương ứng với số ngày quá hạn.
     *
     * @param soNgayQuaHan số ngày đã quá hạn; <b>âm hoặc 0</b> nghĩa là còn trong hạn
     */
    public static NhomTuoiNo cua(long soNgayQuaHan) {
        for (NhomTuoiNo nhom : values()) {
            if (soNgayQuaHan >= nhom.tuNgay && soNgayQuaHan <= nhom.denNgay) {
                return nhom;
            }
        }
        // Không xảy ra: năm khoảng trên đã phủ kín trục số nguyên. Giữ nhánh này để
        // biên dịch được và để nếu ai đó sửa ranh giới thành hở thì lỗi lộ ra ngay.
        throw new IllegalStateException("Không có nhóm tuổi nợ cho " + soNgayQuaHan + " ngày");
    }

    public String getNhan() {
        return nhan;
    }

    public String getMauNen() {
        return mauNen;
    }

    public String getMauChu() {
        return mauChu;
    }

    /** Chuỗi {@code style} sẵn dùng cho badge, để template không phải tự ghép. */
    public String getStyleBadge() {
        return "background-color:" + mauNen + ";color:" + mauChu + ";";
    }

    public boolean isQuaHan() {
        return this != TRONG_HAN;
    }
}
