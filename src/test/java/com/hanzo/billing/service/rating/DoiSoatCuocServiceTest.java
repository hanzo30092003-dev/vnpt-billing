package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.BangDoiSoat;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiDangKyGoi;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.DangKyGoiCuocRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử bảng đối soát cước.
 *
 * <p>Hai điều quan trọng nhất được kiểm ở đây: bảng <b>chỉ đọc lại</b> chứ không tính lại
 * (nên nó mới chứng minh được hóa đơn đúng), và đơn giá hiển thị lấy từ <b>ảnh chụp</b>
 * trên từng bản ghi chứ không tra lại bảng giá hiện hành.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Bảng đối soát cước")
class DoiSoatCuocServiceTest {

    private static final Long TB_ID = 100L;
    private static final Long KY_ID = 1L;

    @Mock private ThueBaoRepository thueBaoRepository;
    @Mock private KyCuocRepository kyCuocRepository;
    @Mock private HoaDonRepository hoaDonRepository;
    @Mock private ChiTietSuDungRepository chiTietSuDungRepository;
    @Mock private DangKyGoiCuocRepository dangKyGoiCuocRepository;

    @InjectMocks
    private DoiSoatCuocService service;

    @Test
    @DisplayName("1. ⭐ Đơn giá hiển thị lấy từ ẢNH CHỤP của từng bản ghi, không tra lại bảng giá")
    void donGiaLayTuAnhChup() {
        // Hai bản ghi CÙNG dịch vụ, CÙNG hướng, CÙNG khung giờ nhưng mang hai dòng bảng giá
        // khác nhau — đúng tình huống bảng giá đổi giữa kỳ. Nếu bảng đối soát tra lại bảng
        // giá thì cả hai sẽ hiện cùng một đơn giá; đọc từ ảnh chụp thì mỗi bản ghi giữ giá
        // của chính nó.
        BangGiaCuoc giaCu = bangGia(1L, 6, 15);
        BangGiaCuoc giaMoi = bangGia(2L, 6, 99);

        chuanBi(List.of(
                cdrThoai(1L, 60, giaCu, "150", false),
                cdrThoai(2L, 60, giaMoi, "990", false)), null);

        BangDoiSoat bang = service.dungBang(TB_ID, KY_ID);

        assertThat(bang.dongCdr()).extracting(BangDoiSoat.DongCdr::donGia)
                .containsExactly(new BigDecimal("15"), new BigDecimal("99"));
        // Số block vẫn tính đúng theo block của chính dòng bảng giá đó
        assertThat(bang.dongCdr()).extracting(BangDoiSoat.DongCdr::soBlock)
                .containsExactly(10L, 10L);
    }

    @Test
    @DisplayName("2. Bảng sản lượng hiện cả đơn vị đọc được lẫn giá trị gốc trong cơ sở dữ liệu")
    void hienCaHaiDonVi() {
        // 5.351 giây = 89,2 phút — chính cặp số chứng minh hệ thống không nhầm giây với phút
        chuanBi(List.of(cdrThoai(1L, 5351, bangGia(1L, 6, 15), "13380", true)), null);

        BangDoiSoat bang = service.dungBang(TB_ID, KY_ID);
        BangDoiSoat.DongUuDai dongNoiMang = bang.dongUuDai().get(0);

        assertThat(dongNoiMang.loai()).isEqualTo("Thoại nội mạng");
        assertThat(dongNoiMang.daDungHienThi()).isEqualTo("89,2 phút");
        assertThat(dongNoiMang.daDungGoc()).isEqualTo("5.351 giây");
        assertThat(dongNoiMang.quotaHienThi()).isEqualTo("100 phút");
        assertThat(dongNoiMang.mienPhiHienThi()).isEqualTo("89,2 phút");
    }

    @Test
    @DisplayName("3. Dữ liệu hiện MB kèm KB gốc — bằng chứng quy đổi 1024")
    void dataHienCaKbVaMb() {
        chuanBi(List.of(cdrData(1L, 1_500_000, bangGia(1L, 1, 25), "36650", true)), null);

        BangDoiSoat.DongUuDai dongData = service.dungBang(TB_ID, KY_ID).dongUuDai().get(3);

        assertThat(dongData.loai()).isEqualTo("Dữ liệu");
        assertThat(dongData.daDungHienThi()).isEqualTo("1.464,8 MB");
        assertThat(dongData.daDungGoc()).isEqualTo("1.500.000 KB");
        assertThat(dongData.ghiChu()).contains("2.097.152 KB");
    }

    @Test
    @DisplayName("4. Bản ghi ĐẦU TIÊN bị tính tiền của mỗi loại được đánh dấu là dòng làm vượt")
    void danhDauDongLamVuot() {
        chuanBi(List.of(
                cdrThoai(1L, 60, bangGia(1L, 6, 15), "0", true),      // miễn phí
                cdrThoai(2L, 60, bangGia(1L, 6, 15), "150", false),   // ← làm vượt
                cdrThoai(3L, 30, bangGia(1L, 6, 15), "75", false)),   // sau đó, không đánh dấu
                null);

        assertThat(service.dungBang(TB_ID, KY_ID).dongCdr())
                .extracting(BangDoiSoat.DongCdr::laDongLamVuot)
                .containsExactly(false, true, false);
    }

    @Test
    @DisplayName("5. Cuộc gọi quốc tế xếp riêng và ghi rõ không áp dụng ưu đãi")
    void quocTeXepRieng() {
        ChiTietSuDung quocTe = cdrThoai(1L, 120, bangGia(1L, 60, 3600), "7200", false);
        quocTe.setHuong(HuongCuocGoi.QUOC_TE);
        chuanBi(List.of(quocTe), null);

        BangDoiSoat bang = service.dungBang(TB_ID, KY_ID);
        BangDoiSoat.DongUuDai dongQuocTe = bang.dongUuDai().get(4);

        assertThat(dongQuocTe.coUuDai()).isFalse();
        assertThat(dongQuocTe.quotaHienThi()).isEqualTo("Không áp dụng ưu đãi");
        assertThat(dongQuocTe.thanhTien()).isEqualByComparingTo("7200");
        // Không được cộng nhầm vào quỹ nội mạng
        assertThat(bang.dongUuDai().get(0).thanhTien()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("6. ⭐ Khối đối chiếu: tổng tính từ CDR khớp hóa đơn, chênh lệch toàn 0")
    void doiChieuKhopHoaDon() {
        HoaDon hoaDon = hoaDon("150000", "1000", "300", "2500");
        chuanBi(List.of(
                cdrThoai(1L, 60, bangGia(1L, 6, 15), "1000", false),
                cdrSms(2L, 3, bangGia(4L, 1, 100), "300", false),
                cdrData(3L, 102400, bangGia(6L, 1, 25), "2500", false)), hoaDon);

        BangDoiSoat bang = service.dungBang(TB_ID, KY_ID);

        assertThat(bang.coChenhLech()).isFalse();
        assertThat(bang.doiChieu()).allSatisfy(d ->
                assertThat(d.chenhLech()).isEqualByComparingTo("0"));
        // 150000 + 1000 + 300 + 2500 = 153800; VAT 15380; tổng 169180
        assertThat(bang.doiChieu()).extracting(BangDoiSoat.DongDoiChieu::khoanMuc)
                .contains("Tổng thanh toán");
        assertThat(bang.doiChieu().get(6).tuCdr()).isEqualByComparingTo("169180");
    }

    @Test
    @DisplayName("7. Hóa đơn lệch với chi tiết sử dụng thì bảng đối soát chỉ ra ngay")
    void phatHienLechVoiHoaDon() {
        // Hóa đơn ghi cước thoại 999 trong khi CDR chỉ có 1000 — mô phỏng hóa đơn cũ chưa
        // lập lại sau khi tính cước lại
        HoaDon hoaDon = hoaDon("150000", "999", "0", "0");
        chuanBi(List.of(cdrThoai(1L, 60, bangGia(1L, 6, 15), "1000", false)), hoaDon);

        BangDoiSoat bang = service.dungBang(TB_ID, KY_ID);

        assertThat(bang.coChenhLech()).isTrue();
        assertThat(bang.doiChieu().get(1).chenhLech()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("8. Thuê bao chưa có hóa đơn: bảng vẫn dựng được, khối đối chiếu để rỗng")
    void khongCoHoaDon_thiVanDungDuocBang() {
        chuanBi(List.of(cdrThoai(1L, 60, bangGia(1L, 6, 15), "150", false)), null);

        BangDoiSoat bang = service.dungBang(TB_ID, KY_ID);

        assertThat(bang.hoaDon()).isNull();
        assertThat(bang.doiChieu()).isEmpty();
        assertThat(bang.coChenhLech()).isFalse();
        assertThat(bang.dongCdr()).hasSize(1);
        assertThat(bang.dongUuDai()).hasSize(5);
    }

    @Test
    @DisplayName("9. Thuê bao hòa mạng giữa kỳ: khối 1 nêu rõ số ngày prorate")
    void proratNeuRoSoNgay() {
        ThueBao thueBao = thueBao("2026-06-23");
        chuanBiVoiThueBao(thueBao, List.of(), hoaDon("40000", "0", "0", "0"));

        BangDoiSoat.ThongTinProrate prorate = service.dungBang(TB_ID, KY_ID).prorate();

        assertThat(prorate.prorate()).isTrue();
        assertThat(prorate.soNgaySuDung()).isEqualTo(8);
        assertThat(prorate.soNgayTrongKy()).isEqualTo(30);
        assertThat(prorate.cuocThueBaoThuc()).isEqualByComparingTo("40000");
    }

    // =================================================================
    // TIỆN ÍCH
    // =================================================================

    private void chuanBi(List<ChiTietSuDung> danhSachCdr, HoaDon hoaDon) {
        chuanBiVoiThueBao(thueBao("2025-01-01"), danhSachCdr, hoaDon);
    }

    private void chuanBiVoiThueBao(ThueBao thueBao, List<ChiTietSuDung> danhSachCdr,
                                   HoaDon hoaDon) {
        when(thueBaoRepository.timTheoIdKemQuanHe(TB_ID)).thenReturn(Optional.of(thueBao));
        when(kyCuocRepository.findById(KY_ID)).thenReturn(Optional.of(ky()));
        when(dangKyGoiCuocRepository.timLichSuTheoThueBao(TB_ID))
                .thenReturn(List.of(dangKy(thueBao)));
        when(hoaDonRepository.findByThueBaoIdAndKyCuocId(TB_ID, KY_ID))
                .thenReturn(Optional.ofNullable(hoaDon));
        when(chiTietSuDungRepository.timTheoThueBaoVaKy(TB_ID, KY_ID)).thenReturn(danhSachCdr);
    }

    private static KyCuoc ky() {
        KyCuoc ky = new KyCuoc();
        ky.setId(KY_ID);
        ky.setThang(6);
        ky.setNam(2026);
        ky.setNgayBatDau(LocalDate.of(2026, 6, 1));
        ky.setNgayKetThuc(LocalDate.of(2026, 6, 30));
        ky.setTrangThai(TrangThaiKyCuoc.MO);
        return ky;
    }

    /** Gói MAX70: 100 phút nội mạng, 30 SMS, 2048 MB data. */
    private static GoiCuoc goi() {
        GoiCuoc goi = new GoiCuoc();
        goi.setId(2L);
        goi.setMaGoi("MAX70");
        goi.setTenGoi("MAX70");
        goi.setCuocThueBaoThang(new BigDecimal("150000"));
        goi.setPhutNoiMangMienPhi(100);
        goi.setPhutNgoaiMangMienPhi(0);
        goi.setSmsMienPhi(30);
        goi.setDataMienPhiMb(2048);
        return goi;
    }

    private static ThueBao thueBao(String ngayKichHoat) {
        KhachHang kh = new KhachHang();
        kh.setId(10L);
        kh.setMaKh("KH000010");
        kh.setTenKh("Khách thử");

        ThueBao tb = new ThueBao();
        tb.setId(TB_ID);
        tb.setSoThueBao("0900000100");
        tb.setKhachHang(kh);
        tb.setGoiCuoc(goi());
        tb.setLoaiThueBao(LoaiThueBao.TRA_SAU);
        tb.setTrangThai(TrangThaiThueBao.HOAT_DONG);
        tb.setNgayKichHoat(LocalDate.parse(ngayKichHoat));
        return tb;
    }

    private static DangKyGoiCuoc dangKy(ThueBao thueBao) {
        DangKyGoiCuoc dk = new DangKyGoiCuoc();
        dk.setThueBao(thueBao);
        dk.setGoiCuoc(goi());
        dk.setNgayBatDau(LocalDate.of(2020, 1, 1));
        dk.setTrangThai(TrangThaiDangKyGoi.DANG_AP_DUNG);
        return dk;
    }

    private static HoaDon hoaDon(String thueBaoThang, String thoai, String sms, String data) {
        HoaDon hd = new HoaDon();
        hd.setId(500L);
        hd.setMaHoaDon("HD202606-000001");
        hd.setCuocThueBao(new BigDecimal(thueBaoThang));
        hd.setCuocThoai(new BigDecimal(thoai));
        hd.setCuocSms(new BigDecimal(sms));
        hd.setCuocData(new BigDecimal(data));
        hd.setCuocKhac(BigDecimal.ZERO);
        hd.setGiamTru(BigDecimal.ZERO);

        BigDecimal truocThue = hd.getCuocThueBao().add(hd.getCuocThoai())
                .add(hd.getCuocSms()).add(hd.getCuocData());
        BigDecimal vat = ThamSoTinhCuoc.lamTronTien(
                truocThue.multiply(ThamSoTinhCuoc.THUE_SUAT_VAT));
        hd.setTongTruocThue(truocThue);
        hd.setThueVat(vat);
        hd.setTongThanhToan(truocThue.add(vat));
        return hd;
    }

    private static BangGiaCuoc bangGia(Long id, int blockGiay, int donGia) {
        BangGiaCuoc bg = new BangGiaCuoc();
        bg.setId(id);
        bg.setBlockGiay(blockGiay);
        bg.setDonGia(new BigDecimal(donGia));
        return bg;
    }

    private static ChiTietSuDung cdrThoai(Long id, int giay, BangGiaCuoc gia,
                                          String cuocPhi, boolean mienPhi) {
        ChiTietSuDung cdr = cdrGoc(id, LoaiDichVu.THOAI, gia, cuocPhi, mienPhi);
        cdr.setThoiLuongGiay(giay);
        cdr.setSoLuong(0);
        return cdr;
    }

    private static ChiTietSuDung cdrSms(Long id, int soTin, BangGiaCuoc gia,
                                        String cuocPhi, boolean mienPhi) {
        ChiTietSuDung cdr = cdrGoc(id, LoaiDichVu.SMS, gia, cuocPhi, mienPhi);
        cdr.setThoiLuongGiay(0);
        cdr.setSoLuong(soTin);
        return cdr;
    }

    private static ChiTietSuDung cdrData(Long id, int soKb, BangGiaCuoc gia,
                                         String cuocPhi, boolean mienPhi) {
        ChiTietSuDung cdr = cdrGoc(id, LoaiDichVu.DATA, gia, cuocPhi, mienPhi);
        cdr.setThoiLuongGiay(0);
        cdr.setSoLuong(soKb);
        return cdr;
    }

    private static ChiTietSuDung cdrGoc(Long id, LoaiDichVu dichVu, BangGiaCuoc gia,
                                        String cuocPhi, boolean mienPhi) {
        ChiTietSuDung cdr = new ChiTietSuDung();
        cdr.setId(id);
        cdr.setLoaiDichVu(dichVu);
        cdr.setHuong(HuongCuocGoi.NOI_MANG);
        cdr.setGioCaoDiem(false);
        cdr.setThoiGianBatDau(LocalDateTime.of(2026, 6, 10, 9, 0));
        cdr.setBangGiaCuoc(gia);
        cdr.setCuocPhi(new BigDecimal(cuocPhi));
        cdr.setMienPhi(mienPhi);
        return cdr;
    }
}
