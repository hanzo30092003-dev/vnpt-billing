package com.hanzo.billing.dto.baocao;

import com.hanzo.billing.util.DinhDangTien;

import java.math.BigDecimal;

/**
 * Sản lượng dịch vụ của một kỳ, cộng thẳng từ {@code chi_tiet_su_dung}.
 *
 * <p>⚠️ <b>Cộng ở đơn vị GỐC rồi mới quy đổi MỘT lần ở cuối.</b> Quy từng bản ghi lên phút
 * rồi cộng là đúng cái lỗi đã thổi phồng sản lượng <b>+10,97%</b> ở Phase 4 — mỗi bản ghi bị
 * làm tròn lên một lần, và hàng nghìn lần làm tròn cộng lại thành một con số sai hẳn.</p>
 *
 * <p>Phép quy đổi để <i>hiển thị</i> nằm ở {@link DinhDangTien#giayRaPhut} — làm tròn
 * {@code HALF_UP}, khác với {@code DonViCuoc} vốn làm tròn <b>lên</b> vì đó là luật tính tiền.
 * Hệ số 60 và 1024 vẫn chỉ khai báo một lần trong {@code DonViCuoc}.</p>
 *
 * @param giayNoiMang tổng <b>giây</b> gọi, không phải phút
 */
public record SanLuongDichVu(long soCuocGoi, long giayNoiMang, long giayNgoaiMang,
                             long giayQuocTe, long soSms, long soKb) {

    public static final SanLuongDichVu RONG = new SanLuongDichVu(0, 0, 0, 0, 0, 0);

    public long tongGiay() {
        return giayNoiMang + giayNgoaiMang + giayQuocTe;
    }

    public BigDecimal phutNoiMang() {
        return DinhDangTien.giayRaPhut(giayNoiMang);
    }

    public BigDecimal phutNgoaiMang() {
        return DinhDangTien.giayRaPhut(giayNgoaiMang);
    }

    public BigDecimal phutQuocTe() {
        return DinhDangTien.giayRaPhut(giayQuocTe);
    }

    public BigDecimal tongPhut() {
        return DinhDangTien.giayRaPhut(tongGiay());
    }

    public BigDecimal soMb() {
        return DinhDangTien.kbRaMb(soKb);
    }

    public boolean rong() {
        return soCuocGoi == 0 && soSms == 0 && soKb == 0;
    }

    /** Biến thiên phần trăm so với kỳ liền trước; {@code null} khi không có kỳ trước để so. */
    public BigDecimal bienThienPhut(SanLuongDichVu kyTruoc) {
        return kyTruoc == null ? null
                : DinhDangTien.bienThienPhanTram(tongPhut(), kyTruoc.tongPhut());
    }

    public BigDecimal bienThienSms(SanLuongDichVu kyTruoc) {
        return kyTruoc == null ? null : DinhDangTien.bienThienPhanTram(
                BigDecimal.valueOf(soSms), BigDecimal.valueOf(kyTruoc.soSms()));
    }

    public BigDecimal bienThienData(SanLuongDichVu kyTruoc) {
        return kyTruoc == null ? null : DinhDangTien.bienThienPhanTram(soMb(), kyTruoc.soMb());
    }

    public BigDecimal bienThienCuocGoi(SanLuongDichVu kyTruoc) {
        return kyTruoc == null ? null : DinhDangTien.bienThienPhanTram(
                BigDecimal.valueOf(soCuocGoi), BigDecimal.valueOf(kyTruoc.soCuocGoi()));
    }
}
