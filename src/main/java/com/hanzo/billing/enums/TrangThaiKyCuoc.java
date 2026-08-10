package com.hanzo.billing.enums;

/**
 * Trạng thái của kỳ tính cước.
 *
 * <p>MO: đang thu thập CDR. DANG_TINH: engine đang chạy tính cước.
 * DA_CHOT: đã chốt, không nhận thêm CDR và không tính lại.</p>
 *
 * <p><b>Nhãn và lớp badge đặt trong enum</b> — thêm ở Phase 6. Trước đó hai thứ này nằm rải
 * trong template dưới dạng ternary lồng nhau
 * ({@code trangThai.name() == 'MO' ? 'Mở' : (... ? 'Đang tính' : 'Đã chốt')}), lặp lại ở mỗi
 * màn hình có hiển thị kỳ cước. Đó đúng là thứ mà {@link TrangThaiHoaDon} và
 * {@link NhomTuoiNo} đã gom vào enum để tránh: nhiều nơi tự chia thì sẽ có lúc nhiều nơi hiện
 * khác nhau cho cùng một trạng thái.</p>
 */
public enum TrangThaiKyCuoc {

    MO("Mở", "text-bg-success"),
    DANG_TINH("Đang tính", "text-bg-warning"),
    DA_CHOT("Đã chốt", "text-bg-secondary");

    private final String nhan;
    private final String lopBadge;

    TrangThaiKyCuoc(String nhan, String lopBadge) {
        this.nhan = nhan;
        this.lopBadge = lopBadge;
    }

    public String getNhan() {
        return nhan;
    }

    public String getLopBadge() {
        return lopBadge;
    }
}
