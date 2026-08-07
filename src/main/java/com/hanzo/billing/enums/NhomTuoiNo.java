package com.hanzo.billing.enums;

/**
 * Nhóm tuổi nợ của một hóa đơn — dùng cho bảng aging.
 *
 * <p>Ranh giới đặt trong chính enum thay vì rải ra service và template: bảng aging, biểu đồ
 * và bộ lọc đều phải chia nhóm <b>giống hệt nhau</b>. Ba nơi tự chia thì sẽ có lúc ba nơi
 * ra ba con số khác nhau cho cùng một tập hóa đơn.</p>
 */
public enum NhomTuoiNo {

    TRONG_HAN("Trong hạn", "text-bg-secondary", Integer.MIN_VALUE, 0),
    QUA_HAN_1_30("Quá hạn 1–30 ngày", "text-bg-warning", 1, 30),
    QUA_HAN_31_60("Quá hạn 31–60 ngày", "text-bg-warning", 31, 60),
    QUA_HAN_61_90("Quá hạn 61–90 ngày", "text-bg-danger", 61, 90),
    QUA_HAN_TREN_90("Quá hạn trên 90 ngày", "text-bg-danger", 91, Integer.MAX_VALUE);

    private final String nhan;
    private final String lopBadge;
    private final int tuNgay;
    private final int denNgay;

    NhomTuoiNo(String nhan, String lopBadge, int tuNgay, int denNgay) {
        this.nhan = nhan;
        this.lopBadge = lopBadge;
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

    public String getLopBadge() {
        return lopBadge;
    }

    public boolean isQuaHan() {
        return this != TRONG_HAN;
    }
}
