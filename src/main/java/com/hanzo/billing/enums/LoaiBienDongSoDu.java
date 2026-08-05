package com.hanzo.billing.enums;

import java.math.BigDecimal;

/**
 * Loại biến động số dư của thuê bao trả trước.
 *
 * <p>Cột {@code so_tien} trong sổ cái luôn lưu giá trị <b>dương</b>; chiều cộng hay trừ
 * do chính loại biến động quyết định. Gom quy tắc dấu vào đây để không nơi nào phải
 * tự đoán — đúng cùng lý do lớp {@code DonViCuoc} gom ba chỗ quy đổi đơn vị.
 */
public enum LoaiBienDongSoDu {

    /** Khách nạp tiền vào tài khoản — cộng số dư. */
    NAP_TIEN("Nạp tiền", true),

    /** Trừ cước sử dụng theo kỳ — trừ số dư. */
    TRU_CUOC("Trừ cước", false),

    /**
     * Bút toán điều chỉnh — cộng số dư. Hiện chỉ dùng cho các dòng <b>mở sổ</b> đưa số dư
     * mẫu từ 0 lên giá trị ban đầu. Nếu về sau cần điều chỉnh <i>giảm</i> thì phải thêm
     * một giá trị riêng chứ không đổi dấu giá trị này — đổi dấu sẽ làm cùng một loại mang
     * hai ý nghĩa và phá quy tắc "dấu suy được từ loại".
     */
    DIEU_CHINH("Điều chỉnh", true);

    private final String nhan;
    private final boolean congVaoSoDu;

    LoaiBienDongSoDu(String nhan, boolean congVaoSoDu) {
        this.nhan = nhan;
        this.congVaoSoDu = congVaoSoDu;
    }

    public String getNhan() {
        return nhan;
    }

    public boolean isCongVaoSoDu() {
        return congVaoSoDu;
    }

    /** Quy số tiền dương của sổ cái về giá trị có dấu để cộng dồn. */
    public BigDecimal apDauCho(BigDecimal soTien) {
        return congVaoSoDu ? soTien : soTien.negate();
    }
}
