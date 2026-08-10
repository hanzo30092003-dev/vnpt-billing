package com.hanzo.billing.service.baocao;

import com.hanzo.billing.dto.baocao.CoCauCuoc;
import com.hanzo.billing.dto.baocao.DongDoanhThuGoi;
import com.hanzo.billing.dto.baocao.DongDoanhThuKy;
import com.hanzo.billing.dto.baocao.DongKhachHangNo;
import com.hanzo.billing.dto.baocao.DongTheoThang;
import com.hanzo.billing.dto.baocao.DongTopThueBao;
import com.hanzo.billing.dto.baocao.MucBieuDo;
import com.hanzo.billing.dto.baocao.SanLuongDichVu;
import com.hanzo.billing.dto.baocao.ThongKeThueBao;
import com.hanzo.billing.dto.baocao.ThongTinDashboard;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaoCaoServiceImpl implements BaoCaoService {

    /** Số dòng của hai bảng phụ trên dashboard. */
    private static final int SO_DONG_DASHBOARD = 5;

    private final HoaDonRepository hoaDonRepository;
    private final ThueBaoRepository thueBaoRepository;
    private final ChiTietSuDungRepository chiTietSuDungRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final KyCuocRepository kyCuocRepository;

    // =================================================================
    // DASHBOARD
    // =================================================================

    @Override
    public ThongTinDashboard dashboard() {
        List<DongDoanhThuKy> doanhThuCacKy = doanhThuTheoKy();

        // Kỳ gần nhất CÓ hóa đơn. Kỳ vừa tạo mà chưa lập hóa đơn thì thẻ "doanh thu kỳ gần
        // nhất" sẽ hiện 0 đ và trông như hệ thống mất số liệu, nên bỏ qua các kỳ rỗng.
        DongDoanhThuKy kyGanNhat = doanhThuCacKy.stream()
                .filter(d -> d.soHoaDon() > 0)
                .reduce((truoc, sau) -> sau)
                .orElse(null);

        YearMonth thangNay = YearMonth.now();
        long thueBaoMoi = thueBaoRepository.countByNgayKichHoatBetween(
                thangNay.atDay(1), thangNay.atEndOfMonth());

        List<DongTopThueBao> top = kyGanNhat == null ? List.of()
                : hoaDonRepository.topThueBaoCuocCao(
                        kyGanNhat.kyId(), PageRequest.of(0, SO_DONG_DASHBOARD));

        return new ThongTinDashboard(
                thueBaoRepository.count(),
                thueBaoRepository.countByTrangThai(TrangThaiThueBao.HOAT_DONG),
                thueBaoMoi,
                kyGanNhat,
                hoaDonRepository.tongConNoToanHeThong(),
                doanhThuCacKy,
                coCauGoiCuoc(),
                coCauTrangThai(),
                top,
                thanhToanRepository.timGiaoDichGanNhat(
                        PageRequest.of(0, SO_DONG_DASHBOARD)));
    }

    private List<MucBieuDo> coCauGoiCuoc() {
        return thueBaoRepository.demTheoGoiCuocKemTen().stream()
                .map(d -> new MucBieuDo(d[0] + " — " + d[1], ((Number) d[2]).longValue()))
                .toList();
    }

    private List<MucBieuDo> coCauTrangThai() {
        return thueBaoRepository.demTheoTrangThai().stream()
                .map(d -> new MucBieuDo(((TrangThaiThueBao) d[0]).getNhan(),
                        ((Number) d[1]).longValue()))
                // Giữ đúng thứ tự khai báo của enum để màu của từng trạng thái không đổi
                // giữa các lần tải trang
                .sorted(Comparator.comparing(m -> thuTuTrangThai(m.nhan())))
                .toList();
    }

    private static int thuTuTrangThai(String nhan) {
        TrangThaiThueBao[] tatCa = TrangThaiThueBao.values();
        for (int i = 0; i < tatCa.length; i++) {
            if (tatCa[i].getNhan().equals(nhan)) {
                return i;
            }
        }
        return tatCa.length;
    }

    // =================================================================
    // BẢY BÁO CÁO
    // =================================================================

    @Override
    public List<DongDoanhThuKy> doanhThuTheoKy() {
        return hoaDonRepository.thongKeDoanhThuTheoKy();
    }

    @Override
    public List<DongDoanhThuGoi> doanhThuTheoGoi(Long kyCuocId) {
        return kyCuocId == null ? List.of() : hoaDonRepository.thongKeDoanhThuTheoGoi(kyCuocId);
    }

    @Override
    public CoCauCuoc coCauCuoc(Long kyCuocId) {
        if (kyCuocId == null) {
            return coCauRong();
        }
        CoCauCuoc coCau = hoaDonRepository.coCauCuocTheoKy(kyCuocId);
        // Truy vấn tổng hợp không có GROUP BY luôn trả về một dòng, nhưng phòng thủ để màn
        // hình không vỡ nếu về sau ai đó thêm điều kiện lọc làm nó trả về rỗng
        return coCau == null ? coCauRong() : coCau;
    }

    private static CoCauCuoc coCauRong() {
        BigDecimal khong = BigDecimal.ZERO;
        return new CoCauCuoc(khong, khong, khong, khong, khong, khong, khong, khong, khong);
    }

    @Override
    public ThongKeThueBao thongKeThueBao() {
        List<MucBieuDo> theoTrangThai = coCauTrangThai();
        List<MucBieuDo> theoLoai = thueBaoRepository.demTheoLoai().stream()
                .map(d -> new MucBieuDo(((LoaiThueBao) d[0]).getNhan(),
                        ((Number) d[1]).longValue()))
                .toList();

        List<DongTheoThang> moi = doiSangDongTheoThang(
                thueBaoRepository.demThueBaoMoiTheoThang());
        List<DongTheoThang> roiMang = doiSangDongTheoThang(
                thueBaoRepository.demThueBaoRoiMangTheoThang());

        // Ghép hai chuỗi về CÙNG một trục thời gian. Không ghép thì tháng chỉ có thuê bao mới
        // và tháng chỉ có thuê bao rời mạng sẽ lệch nhau, và hai đường trên biểu đồ nói về
        // hai trục khác nhau mà nhìn như cùng một trục.
        TreeSet<Integer> khoa = new TreeSet<>();
        moi.forEach(d -> khoa.add(d.khoa()));
        roiMang.forEach(d -> khoa.add(d.khoa()));

        Map<Integer, Long> banDoMoi = moi.stream()
                .collect(Collectors.toMap(DongTheoThang::khoa, DongTheoThang::soLuong));
        Map<Integer, Long> banDoRoi = roiMang.stream()
                .collect(Collectors.toMap(DongTheoThang::khoa, DongTheoThang::soLuong));

        List<String> mocThoiGian = new ArrayList<>();
        List<Long> soMoi = new ArrayList<>();
        List<Long> soRoi = new ArrayList<>();
        for (Integer k : khoa) {
            mocThoiGian.add(String.format("%02d/%d", k % 100, k / 100));
            soMoi.add(banDoMoi.getOrDefault(k, 0L));
            soRoi.add(banDoRoi.getOrDefault(k, 0L));
        }

        return new ThongKeThueBao(theoTrangThai, theoLoai, mocThoiGian, soMoi, soRoi,
                thueBaoRepository.count(),
                thueBaoRepository.countByTrangThai(TrangThaiThueBao.HOAT_DONG),
                thueBaoRepository.countByTrangThai(TrangThaiThueBao.DA_THANH_LY));
    }

    private static List<DongTheoThang> doiSangDongTheoThang(List<Object[]> dong) {
        return dong.stream()
                .map(d -> new DongTheoThang(((Number) d[0]).intValue(),
                        ((Number) d[1]).intValue(), ((Number) d[2]).longValue()))
                .toList();
    }

    @Override
    public List<DongTopThueBao> topThueBaoCuocCao(Long kyCuocId, int soLuong) {
        if (kyCuocId == null) {
            return List.of();
        }
        return hoaDonRepository.topThueBaoCuocCao(kyCuocId, PageRequest.of(0, soLuong));
    }

    @Override
    public SanLuongDichVu sanLuong(Long kyCuocId) {
        if (kyCuocId == null) {
            return SanLuongDichVu.RONG;
        }
        SanLuongDichVu ketQua = chiTietSuDungRepository.sanLuongTheoKy(kyCuocId);
        return ketQua == null ? SanLuongDichVu.RONG : ketQua;
    }

    /**
     * {@inheritDoc}
     *
     * <p>So theo <b>(năm, tháng)</b> chứ không theo {@code id}. Trong dữ liệu mẫu, kỳ 3 và 4
     * mang id 4 và 5 vì chúng được tạo <i>sau</i> kỳ 5, 6, 7 — so theo id sẽ ghép kỳ 3/2026
     * với kỳ 7/2026 làm "kỳ liền trước", và mọi con số so sánh đều vô nghĩa.</p>
     */
    @Override
    public Optional<KyCuoc> kyLienTruoc(Long kyCuocId) {
        if (kyCuocId == null) {
            return Optional.empty();
        }
        Optional<KyCuoc> hienTai = kyCuocRepository.findById(kyCuocId);
        if (hienTai.isEmpty()) {
            return Optional.empty();
        }
        int khoaHienTai = khoaThoiGian(hienTai.get());
        return kyCuocRepository.findAll().stream()
                .filter(k -> khoaThoiGian(k) < khoaHienTai)
                .max(Comparator.comparingInt(BaoCaoServiceImpl::khoaThoiGian));
    }

    private static int khoaThoiGian(KyCuoc ky) {
        return ky.getNam() * 100 + ky.getThang();
    }

    @Override
    public List<DongKhachHangNo> topKhachHangNo(int soLuong) {
        return hoaDonRepository.topKhachHangNo(PageRequest.of(0, soLuong));
    }

    // =================================================================

    @Override
    public List<KyCuoc> danhSachKy() {
        return kyCuocRepository.findAll().stream()
                .sorted(Comparator.comparingInt(BaoCaoServiceImpl::khoaThoiGian).reversed())
                .toList();
    }

    @Override
    public Optional<KyCuoc> kyMacDinh() {
        Map<Long, KyCuoc> theoId = kyCuocRepository.findAll().stream()
                .collect(Collectors.toMap(KyCuoc::getId, Function.identity()));
        return doanhThuTheoKy().stream()
                .filter(d -> d.soHoaDon() > 0)
                .reduce((truoc, sau) -> sau)
                .map(d -> theoId.get(d.kyId()));
    }

    /** Ngày hôm nay — tách ra để test đọc được cùng một mốc với màn hình. */
    public static LocalDate homNay() {
        return LocalDate.now();
    }
}
