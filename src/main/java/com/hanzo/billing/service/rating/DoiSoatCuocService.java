package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.BangDoiSoat;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.DangKyGoiCuocRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Dựng bảng đối soát cước của một thuê bao trong một kỳ.
 *
 * <p>Lớp này <b>chỉ đọc và trình bày</b>, không tính lại gì cả. Mọi con số đều lấy thẳng từ
 * dữ liệu đã ghi: {@code cuoc_phi} và {@code mien_phi} do engine đặt, đơn giá lấy từ dòng
 * bảng giá đã chụp ở {@code bang_gia_cuoc_id}. Nếu bảng đối soát tự tính lại theo cách
 * riêng thì nó không còn chứng minh được gì — nó sẽ chỉ chứng minh chính nó.</p>
 *
 * <p>Riêng khoảng ngày sử dụng và gói cước hiệu lực thì dùng chung
 * {@link QuyTacKyCuoc} với engine lập hóa đơn, đúng vì lý do trên.</p>
 */
@Service
@RequiredArgsConstructor
public class DoiSoatCuocService {

    private static final Set<HuongCuocGoi> HUONG_TRONG_NUOC =
            EnumSet.of(HuongCuocGoi.NOI_MANG, HuongCuocGoi.NGOAI_MANG);

    private final com.hanzo.billing.config.ThamSoNghiepVu thamSo;
    private final ThueBaoRepository thueBaoRepository;
    private final KyCuocRepository kyCuocRepository;
    private final HoaDonRepository hoaDonRepository;
    private final ChiTietSuDungRepository chiTietSuDungRepository;
    private final DangKyGoiCuocRepository dangKyGoiCuocRepository;

    @Transactional(readOnly = true)
    public BangDoiSoat dungBang(Long thueBaoId, Long kyCuocId) {
        ThueBao thueBao = thueBaoRepository.timTheoIdKemQuanHe(thueBaoId)
                .orElseThrow(() -> new NghiepVuException(
                        "Không tìm thấy thuê bao có mã số " + thueBaoId));
        KyCuoc ky = kyCuocRepository.findById(kyCuocId)
                .orElseThrow(() -> new NghiepVuException(
                        "Không tìm thấy kỳ cước có mã số " + kyCuocId));

        GoiCuoc goi = QuyTacKyCuoc.goiCuocHieuLucTai(
                        dangKyGoiCuocRepository.timLichSuTheoThueBao(thueBaoId),
                        ky.getNgayKetThuc())
                .orElseGet(thueBao::getGoiCuoc);

        HoaDon hoaDon = hoaDonRepository
                .findByThueBaoIdAndKyCuocId(thueBaoId, kyCuocId).orElse(null);
        List<ChiTietSuDung> danhSachCdr =
                chiTietSuDungRepository.timTheoThueBaoVaKy(thueBaoId, kyCuocId);

        return new BangDoiSoat(thueBao, ky, goi, hoaDon,
                dungProrate(thueBao, ky, goi, hoaDon),
                dungBangUuDai(danhSachCdr, goi),
                dungChiTietCdr(danhSachCdr),
                dungDoiChieu(danhSachCdr, hoaDon));
    }

    // -----------------------------------------------------------------
    // Khối 1 — prorate cước thuê bao
    // -----------------------------------------------------------------

    private BangDoiSoat.ThongTinProrate dungProrate(ThueBao thueBao, KyCuoc ky,
                                                    GoiCuoc goi, HoaDon hoaDon) {
        Optional<QuyTacKyCuoc.KhoangSuDung> khoang = QuyTacKyCuoc.khoangSuDung(thueBao, ky);
        if (khoang.isEmpty()) {
            return null;
        }
        QuyTacKyCuoc.KhoangSuDung k = khoang.get();
        // Cước thực lấy từ hóa đơn nếu đã có: đó mới là con số đã thu, không phải số tính lại
        BigDecimal cuocThuc = hoaDon != null ? hoaDon.getCuocThueBao() : BigDecimal.ZERO;
        return new BangDoiSoat.ThongTinProrate(k.tuNgay(), k.denNgay(),
                k.soNgaySuDung(), k.soNgayTrongKy(),
                goi.getCuocThueBaoThang(), cuocThuc, !k.troVenTronKy());
    }

    // -----------------------------------------------------------------
    // Khối 2 — bảng sản lượng và ưu đãi
    // -----------------------------------------------------------------

    /** Sản lượng cộng dồn của một nhóm ưu đãi. */
    private static final class GomNhom {
        private long tong;
        private long mienPhi;
        private BigDecimal thanhTien = BigDecimal.ZERO;

        void them(long sanLuong, boolean duocMienPhi, BigDecimal cuocPhi) {
            tong += sanLuong;
            if (duocMienPhi) {
                mienPhi += sanLuong;
            }
            if (cuocPhi != null) {
                thanhTien = thanhTien.add(cuocPhi);
            }
        }

        long vuot() {
            return tong - mienPhi;
        }
    }

    private List<BangDoiSoat.DongUuDai> dungBangUuDai(List<ChiTietSuDung> danhSach, GoiCuoc goi) {
        GomNhom thoaiNoiMang = new GomNhom();
        GomNhom thoaiNgoaiMang = new GomNhom();
        GomNhom sms = new GomNhom();
        GomNhom data = new GomNhom();
        GomNhom quocTe = new GomNhom();

        for (ChiTietSuDung cdr : danhSach) {
            boolean mienPhi = Boolean.TRUE.equals(cdr.getMienPhi());
            if (cdr.getHuong() == HuongCuocGoi.QUOC_TE) {
                long sanLuong = cdr.getLoaiDichVu() == LoaiDichVu.THOAI
                        ? soGiay(cdr) : soLuong(cdr);
                quocTe.them(sanLuong, mienPhi, cdr.getCuocPhi());
                continue;
            }
            switch (cdr.getLoaiDichVu()) {
                case THOAI -> (cdr.getHuong() == HuongCuocGoi.NOI_MANG
                        ? thoaiNoiMang : thoaiNgoaiMang)
                        .them(soGiay(cdr), mienPhi, cdr.getCuocPhi());
                case SMS -> sms.them(soLuong(cdr), mienPhi, cdr.getCuocPhi());
                case DATA -> data.them(soLuong(cdr), mienPhi, cdr.getCuocPhi());
            }
        }

        List<BangDoiSoat.DongUuDai> dong = new ArrayList<>();
        dong.add(dongThoai("Thoại nội mạng", thoaiNoiMang, khongNull(goi.getPhutNoiMangMienPhi())));
        dong.add(dongThoai("Thoại ngoại mạng", thoaiNgoaiMang,
                khongNull(goi.getPhutNgoaiMangMienPhi())));
        dong.add(dongSms(sms, khongNull(goi.getSmsMienPhi())));
        dong.add(dongData(data, khongNull(goi.getDataMienPhiMb())));
        dong.add(dongQuocTe(quocTe));
        return dong;
    }

    /**
     * Dòng thoại: hiển thị theo phút nhưng <b>luôn kèm số giây gốc</b>.
     *
     * <p>Chính cặp số này là bằng chứng trực quan cho thấy hệ thống quy đổi đúng. Quota
     * khai bằng phút, sản lượng lưu bằng giây; ai đọc bảng cũng tự nhân chia kiểm được.</p>
     */
    private BangDoiSoat.DongUuDai dongThoai(String nhan, GomNhom nhom, long quotaPhut) {
        return new BangDoiSoat.DongUuDai(nhan,
                phut(nhom.tong), so(nhom.tong) + " giây",
                quotaPhut > 0 ? so(quotaPhut) + " phút" : "Không có ưu đãi",
                phut(nhom.mienPhi), phut(nhom.vuot()),
                nhom.thanhTien, quotaPhut > 0,
                quotaPhut > 0 ? "Quota " + so(quotaPhut * 60) + " giây" : null);
    }

    private BangDoiSoat.DongUuDai dongSms(GomNhom nhom, long quotaTin) {
        return new BangDoiSoat.DongUuDai("Tin nhắn trong nước",
                so(nhom.tong) + " tin", so(nhom.tong) + " tin",
                quotaTin > 0 ? so(quotaTin) + " tin" : "Không có ưu đãi",
                so(nhom.mienPhi) + " tin", so(nhom.vuot()) + " tin",
                nhom.thanhTien, quotaTin > 0, "Không phải quy đổi đơn vị");
    }

    /** Dòng data: hiển thị theo MB nhưng luôn kèm số KB gốc — bằng chứng quy đổi 1024. */
    private BangDoiSoat.DongUuDai dongData(GomNhom nhom, long quotaMb) {
        return new BangDoiSoat.DongUuDai("Dữ liệu",
                mb(nhom.tong), so(nhom.tong) + " KB",
                quotaMb > 0 ? so(quotaMb) + " MB" : "Không có ưu đãi",
                mb(nhom.mienPhi), mb(nhom.vuot()),
                nhom.thanhTien, quotaMb > 0,
                quotaMb > 0 ? "Quota " + so(quotaMb * 1024) + " KB" : null);
    }

    private BangDoiSoat.DongUuDai dongQuocTe(GomNhom nhom) {
        return new BangDoiSoat.DongUuDai("Quốc tế (thoại và tin nhắn)",
                so(nhom.tong) + " giây / tin", so(nhom.tong),
                "Không áp dụng ưu đãi", "0", so(nhom.tong),
                nhom.thanhTien, false,
                "Cuộc gọi và tin nhắn quốc tế luôn tính tiền, không trừ vào quỹ nào");
    }

    // -----------------------------------------------------------------
    // Khối 3 — chi tiết từng bản ghi
    // -----------------------------------------------------------------

    private List<BangDoiSoat.DongCdr> dungChiTietCdr(List<ChiTietSuDung> danhSach) {
        // Bản ghi bị tính tiền ĐẦU TIÊN của mỗi nhóm ưu đãi chính là bản ghi làm vượt quota
        Set<String> daGapBanGhiTinhTien = new java.util.HashSet<>();

        List<BangDoiSoat.DongCdr> dong = new ArrayList<>(danhSach.size());
        for (ChiTietSuDung cdr : danhSach) {
            boolean mienPhi = Boolean.TRUE.equals(cdr.getMienPhi());
            boolean coUuDai = HUONG_TRONG_NUOC.contains(cdr.getHuong());
            String nhom = cdr.getLoaiDichVu() + "/" + cdr.getHuong();

            boolean laDongLamVuot = false;
            if (coUuDai && !mienPhi && daGapBanGhiTinhTien.add(nhom)) {
                laDongLamVuot = true;
            }

            dong.add(new BangDoiSoat.DongCdr(
                    cdr.getId(), cdr.getThoiGianBatDau(), cdr.getSoBiGoi(),
                    cdr.getLoaiDichVu().name(), cdr.getHuong().name(),
                    Boolean.TRUE.equals(cdr.getGioCaoDiem()),
                    sanLuongHienThi(cdr), soBlock(cdr),
                    // Đơn giá lấy từ ẢNH CHỤP lúc định giá, không tra lại bảng giá hiện hành
                    cdr.getBangGiaCuoc() == null ? null : cdr.getBangGiaCuoc().getDonGia(),
                    cdr.getCuocPhi(), mienPhi, laDongLamVuot));
        }
        return dong;
    }

    private Long soBlock(ChiTietSuDung cdr) {
        if (cdr.getBangGiaCuoc() == null) {
            return null;
        }
        long sanLuong = switch (cdr.getLoaiDichVu()) {
            case THOAI -> soGiay(cdr);
            case SMS -> soLuong(cdr);
            case DATA -> DonViCuoc.kbSangMb(soLuong(cdr));
        };
        return DonViCuoc.soBlock(sanLuong, cdr.getBangGiaCuoc().getBlockGiay());
    }

    private String sanLuongHienThi(ChiTietSuDung cdr) {
        return switch (cdr.getLoaiDichVu()) {
            case THOAI -> so(soGiay(cdr)) + " giây";
            case SMS -> so(soLuong(cdr)) + " tin";
            case DATA -> so(soLuong(cdr)) + " KB = " + mb(soLuong(cdr));
        };
    }

    // -----------------------------------------------------------------
    // Khối 4 — đối chiếu với hóa đơn
    // -----------------------------------------------------------------

    private List<BangDoiSoat.DongDoiChieu> dungDoiChieu(List<ChiTietSuDung> danhSach,
                                                        HoaDon hoaDon) {
        if (hoaDon == null) {
            return List.of();
        }
        BigDecimal thoai = tongCuoc(danhSach, LoaiDichVu.THOAI);
        BigDecimal sms = tongCuoc(danhSach, LoaiDichVu.SMS);
        BigDecimal data = tongCuoc(danhSach, LoaiDichVu.DATA);
        BigDecimal truocThue = hoaDon.getCuocThueBao().add(thoai).add(sms).add(data)
                .add(hoaDon.getCuocKhac()).subtract(hoaDon.getGiamTru());
        BigDecimal vat = ThamSoTinhCuoc.lamTronTien(
                truocThue.multiply(thamSo.getThueSuatVat()));

        return List.of(
                new BangDoiSoat.DongDoiChieu("Cước thuê bao tháng",
                        hoaDon.getCuocThueBao(), hoaDon.getCuocThueBao()),
                new BangDoiSoat.DongDoiChieu("Cước thoại", thoai, hoaDon.getCuocThoai()),
                new BangDoiSoat.DongDoiChieu("Cước tin nhắn", sms, hoaDon.getCuocSms()),
                new BangDoiSoat.DongDoiChieu("Cước dữ liệu", data, hoaDon.getCuocData()),
                new BangDoiSoat.DongDoiChieu("Tổng trước thuế", truocThue,
                        hoaDon.getTongTruocThue()),
                new BangDoiSoat.DongDoiChieu("Thuế VAT " + thamSo.nhanThueSuat(), vat, hoaDon.getThueVat()),
                new BangDoiSoat.DongDoiChieu("Tổng thanh toán", truocThue.add(vat),
                        hoaDon.getTongThanhToan()));
    }

    private BigDecimal tongCuoc(List<ChiTietSuDung> danhSach, LoaiDichVu dichVu) {
        return danhSach.stream()
                .filter(c -> c.getLoaiDichVu() == dichVu && c.getCuocPhi() != null)
                .map(ChiTietSuDung::getCuocPhi)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // -----------------------------------------------------------------
    // Định dạng
    // -----------------------------------------------------------------

    private static long soGiay(ChiTietSuDung cdr) {
        return cdr.getThoiLuongGiay() == null ? 0L : cdr.getThoiLuongGiay();
    }

    private static long soLuong(ChiTietSuDung cdr) {
        return cdr.getSoLuong() == null ? 0L : cdr.getSoLuong();
    }

    private static long khongNull(Integer giaTri) {
        return giaTri == null ? 0L : giaTri;
    }

    private static DecimalFormat dinhDang(String mau) {
        DecimalFormatSymbols kyHieu = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        kyHieu.setGroupingSeparator('.');
        kyHieu.setDecimalSeparator(',');
        return new DecimalFormat(mau, kyHieu);
    }

    private static String so(long giaTri) {
        return dinhDang("#,##0").format(giaTri);
    }

    /** Giây sang phút, một chữ số thập phân — chỉ để đọc, không dùng để tính. */
    private static String phut(long soGiay) {
        return dinhDang("#,##0.#").format(soGiay / 60.0) + " phút";
    }

    /** KB sang MB, một chữ số thập phân — chỉ để đọc, không dùng để tính. */
    private static String mb(long soKb) {
        return dinhDang("#,##0.#").format(soKb / 1024.0) + " MB";
    }
}
