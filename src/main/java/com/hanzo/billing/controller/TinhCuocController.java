package com.hanzo.billing.controller;

import com.hanzo.billing.dto.KetQuaBilling;
import com.hanzo.billing.dto.KetQuaRating;
import com.hanzo.billing.dto.TinhTrangKy;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.LoaiBienDongSoDu;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.BienDongSoDuRepository;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.ThanhToanRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.KyCuocService;
import com.hanzo.billing.service.rating.BillingService;
import com.hanzo.billing.service.rating.DoiSoatCuocService;
import com.hanzo.billing.service.rating.RatingService;
import com.hanzo.billing.service.rating.ThamSoTinhCuoc;
import com.hanzo.billing.service.rating.TruCuocTraTruocService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Màn hình điều khiển engine tính cước.
 *
 * <p>Toàn bộ nghiệp vụ nằm ở {@code RatingService} và {@code BillingService}; lớp này chỉ
 * điều phối và chuyển thông báo. Mọi chốt chặn (kỳ đã chốt, kỳ đã có thanh toán, kỳ chưa
 * tính cước xong…) vẫn do tầng nghiệp vụ giữ — giao diện chỉ <b>ẩn bớt nút</b> cho gọn mắt,
 * gõ thẳng URL vẫn bị chặn đúng như cũ.</p>
 */
@Controller
@RequestMapping("/tinh-cuoc")
@RequiredArgsConstructor
public class TinhCuocController {

    private final RatingService ratingService;
    private final BillingService billingService;
    private final DoiSoatCuocService doiSoatCuocService;
    private final KyCuocService kyCuocService;
    private final ChiTietSuDungRepository chiTietSuDungRepository;
    private final HoaDonRepository hoaDonRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final BienDongSoDuRepository bienDongSoDuRepository;
    private final ThueBaoRepository thueBaoRepository;
    private final TruCuocTraTruocService truCuocTraTruocService;

    // =================================================================
    // DANH SÁCH KỲ VÀ CÁC THAO TÁC
    // =================================================================

    @GetMapping
    public String danhSach(Model model) {
        List<TinhTrangKy> danhSach = new ArrayList<>();
        for (KyCuoc ky : kyCuocService.layTatCa()) {
            danhSach.add(tinhTrang(ky));
        }
        model.addAttribute("danhSach", danhSach);
        model.addAttribute("nguongCanhBaoSoDu", ThamSoTinhCuoc.NGUONG_CANH_BAO_SO_DU);
        model.addAttribute("thueBaoSoDuThap", thueBaoRepository.timSoDuDuoiNguong(
                ThamSoTinhCuoc.NGUONG_CANH_BAO_SO_DU));
        return "tinh-cuoc/danh-sach";
    }

    private TinhTrangKy tinhTrang(KyCuoc ky) {
        LocalDateTime tuLuc = ky.getNgayBatDau().atStartOfDay();
        LocalDateTime denLuc = ky.getNgayKetThuc().plusDays(1).atStartOfDay();
        return new TinhTrangKy(ky,
                chiTietSuDungRepository.demTheoKhoangVaTrangThai(
                        tuLuc, denLuc, TrangThaiTinhCuoc.CHUA_TINH),
                chiTietSuDungRepository.demTheoKhoangVaTrangThai(
                        tuLuc, denLuc, TrangThaiTinhCuoc.LOI),
                chiTietSuDungRepository.countByKyCuocIdAndTrangThaiTinhCuoc(
                        ky.getId(), TrangThaiTinhCuoc.DA_TINH),
                hoaDonRepository.countByKyCuocId(ky.getId()),
                thanhToanRepository.demTheoKyCuoc(ky.getId()),
                bienDongSoDuRepository.countByKyCuocIdAndLoaiBienDong(
                        ky.getId(), LoaiBienDongSoDu.TRU_CUOC));
    }

    @PostMapping("/{id}/tinh-cuoc")
    public String chayTinhCuoc(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            KetQuaRating kq = ratingService.ratingKy(kyCuocService.layTheoId(id));
            return "Đã tính cước " + kq.getSoThanhCong() + " bản ghi ("
                    + kq.getSoLoi() + " lỗi) · Tổng cước " + tien(kq.getTongCuoc())
                    + " · Thời gian " + kq.getThoiGianMs() + " ms";
        });
    }

    @PostMapping("/{id}/lap-hoa-don")
    public String lapHoaDon(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            KetQuaBilling kq = billingService.billingKy(kyCuocService.layTheoId(id));
            return "Đã tạo " + kq.getSoHoaDonTao() + " hóa đơn (bỏ qua "
                    + kq.getSoBoQuaKhongDuDieuKien() + " thuê bao không đủ điều kiện) · "
                    + "Doanh thu kỳ " + tien(kq.getTongDoanhThu())
                    + " · Thời gian " + kq.getThoiGianMs() + " ms";
        });
    }

    @PostMapping("/{id}/huy-hoa-don")
    public String huyHoaDon(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            int so = billingService.huyBillingKy(kyCuocService.layTheoId(id));
            return "Đã hủy " + so + " hóa đơn của kỳ. Cước phí trên chi tiết sử dụng "
                    + "giữ nguyên — có thể lập lại hóa đơn ngay.";
        });
    }

    @PostMapping("/{id}/tru-cuoc-tra-truoc")
    public String truCuocTraTruoc(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            TruCuocTraTruocService.KetQuaTruCuoc kq =
                    truCuocTraTruocService.truCuocKy(kyCuocService.layTheoId(id));
            return "Đã trừ cước vào số dư " + kq.getSoThueBaoXuLy() + " thuê bao trả trước · "
                    + "Trừ " + tien(kq.getTongDaTru()) + " qua " + kq.getSoBanGhiDaTru()
                    + " bản ghi · " + kq.getSoThueBaoHetSoDu() + " thuê bao hết số dư giữa "
                    + "chừng, không thu được " + tien(kq.getTongKhongThuDuoc())
                    + " · Thời gian " + kq.getThoiGianMs() + " ms";
        });
    }

    @PostMapping("/{id}/huy-tru-cuoc-tra-truoc")
    public String huyTruCuocTraTruoc(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            int so = truCuocTraTruocService.huyTruCuocKy(kyCuocService.layTheoId(id));
            return "Đã hoàn trả số dư cho " + so + " thuê bao trả trước. Cước phí trên chi "
                    + "tiết sử dụng giữ nguyên — có thể trừ lại ngay.";
        });
    }

    @PostMapping("/{id}/huy-tinh-cuoc")
    public String huyTinhCuoc(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            int so = ratingService.huyRatingKy(kyCuocService.layTheoId(id));
            return "Đã đưa " + so + " bản ghi chi tiết sử dụng về trạng thái Chưa tính.";
        });
    }

    @PostMapping("/{id}/go-ky-ket")
    public String goKyKet(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            ratingService.goKyBiKet(id);
            return "Đã gỡ kỳ về trạng thái Mở. Dữ liệu chi tiết sử dụng giữ nguyên — "
                    + "hãy chạy lại tính cước để xử lý nốt phần còn sót.";
        });
    }

    @PostMapping("/{id}/chot-ky")
    public String chotKy(@PathVariable Long id, RedirectAttributes ra) {
        return chay(ra, () -> {
            KyCuoc ky = kyCuocService.chotKy(id);
            return "Đã chốt kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + ". Kỳ này không thể tính lại hay sửa đổi nữa.";
        });
    }

    /**
     * Chạy một thao tác và chuyển kết quả sang trang danh sách.
     *
     * <p>Bắt {@link NghiepVuException} tại đây để người dùng nhận thông báo tiếng Việt của
     * tầng nghiệp vụ thay vì trang lỗi 500.</p>
     */
    private String chay(RedirectAttributes ra, ThaoTac thaoTac) {
        try {
            ra.addFlashAttribute("ketQuaChay", thaoTac.thucHien());
        } catch (NghiepVuException ex) {
            ra.addFlashAttribute("thongBaoLoi", ex.getMessage());
        }
        return "redirect:/tinh-cuoc";
    }

    @FunctionalInterface
    private interface ThaoTac {
        String thucHien();
    }

    // =================================================================
    // DANH SÁCH HÓA ĐƠN CỦA MỘT KỲ
    // =================================================================

    @GetMapping("/ky/{id}")
    public String hoaDonCuaKy(@PathVariable Long id, Model model) {
        KyCuoc ky = kyCuocService.layTheoId(id);
        model.addAttribute("ky", ky);
        model.addAttribute("danhSachHoaDon", hoaDonRepository.timTheoKyKemQuanHe(id));
        return "tinh-cuoc/hoa-don-ky";
    }

    // =================================================================
    // BẢNG ĐỐI SOÁT CƯỚC
    // =================================================================

    @GetMapping("/doi-soat/{thueBaoId}/{kyId}")
    public String doiSoat(@PathVariable Long thueBaoId, @PathVariable Long kyId,
                          Model model) {
        model.addAttribute("bang", doiSoatCuocService.dungBang(thueBaoId, kyId));
        model.addAttribute("danhSachKy", kyCuocService.layTatCa());
        return "tinh-cuoc/doi-soat";
    }

    private static String tien(java.math.BigDecimal soTien) {
        return String.format("%,d đ", soTien.longValue());
    }
}
