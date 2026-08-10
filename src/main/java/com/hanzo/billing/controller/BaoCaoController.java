package com.hanzo.billing.controller;

import com.hanzo.billing.dto.OTuoiNo;
import com.hanzo.billing.dto.baocao.CoCauCuoc;
import com.hanzo.billing.dto.baocao.DongDoanhThuGoi;
import com.hanzo.billing.dto.baocao.DongDoanhThuKy;
import com.hanzo.billing.dto.baocao.DongKhachHangNo;
import com.hanzo.billing.dto.baocao.DongTopThueBao;
import com.hanzo.billing.dto.baocao.SanLuongDichVu;
import com.hanzo.billing.dto.baocao.ThongKeThueBao;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.service.CongNoService;
import com.hanzo.billing.service.baocao.BaoCaoExcel;
import com.hanzo.billing.service.baocao.BaoCaoService;
import com.hanzo.billing.util.DinhDangTien;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

/**
 * Bảy báo cáo thống kê của Phase 6.
 *
 * <h2>Hai quy ước chung của mọi báo cáo ở đây</h2>
 * <ol>
 *   <li><b>Kỳ mặc định là kỳ mới nhất CÓ hóa đơn.</b> Mở báo cáo mà thấy bảng trống vì kỳ mặc
 *       định là một kỳ vừa tạo chưa chạy gì thì người dùng sẽ tưởng báo cáo hỏng.</li>
 *   <li><b>Không có dữ liệu thì hiện thông báo, không ném lỗi.</b> Tiêu chí nghiệm thu số 7:
 *       mọi báo cáo phải chạy được trên CSDL rỗng. Vì thế mọi tham số kỳ đều chấp nhận
 *       {@code null} và mọi service trả về danh sách rỗng thay vì ném.</li>
 * </ol>
 */
@Controller
@RequestMapping("/bao-cao")
@RequiredArgsConstructor
public class BaoCaoController {

    /** Các lựa chọn cho ô "số lượng" của báo cáo top thuê bao. */
    private static final List<Integer> CAC_MUC_SO_LUONG = List.of(10, 20, 50);

    private static final String LOAI_EXCEL =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private final BaoCaoService baoCaoService;
    private final CongNoService congNoService;

    @GetMapping
    public String menu(Model model) {
        model.addAttribute("doanhThuCacKy", baoCaoService.doanhThuTheoKy());
        return "bao-cao/menu";
    }

    // =================================================================
    // C.1 — DOANH THU THEO KỲ
    // =================================================================

    @GetMapping("/doanh-thu-ky")
    public String doanhThuKy(Model model) {
        List<DongDoanhThuKy> danhSach = baoCaoService.doanhThuTheoKy();
        model.addAttribute("danhSach", danhSach);
        model.addAttribute("tong", congDon(danhSach));
        // Hai mảng cho biểu đồ dựng sẵn ở đây, không để template tự chiếu: bảng và biểu đồ
        // khi đó chắc chắn đọc cùng một nguồn, cùng thứ tự (theo CongNoController)
        model.addAttribute("nhanKy", danhSach.stream().map(DongDoanhThuKy::nhanKy).toList());
        model.addAttribute("soPhatSinh", danhSach.stream().map(DongDoanhThuKy::phatSinh).toList());
        model.addAttribute("soDaThu", danhSach.stream().map(DongDoanhThuKy::daThu).toList());
        model.addAttribute("soTyLeThu", danhSach.stream().map(DongDoanhThuKy::tyLeThu).toList());
        return "bao-cao/doanh-thu-ky";
    }

    @GetMapping("/doanh-thu-ky/xuat-excel")
    public ResponseEntity<byte[]> xuatDoanhThuKy() {
        List<DongDoanhThuKy> danhSach = baoCaoService.doanhThuTheoKy();
        Tong tong = congDon(danhSach);

        BaoCaoExcel excel = new BaoCaoExcel("Doanh thu theo kỳ", "Toàn bộ kỳ cước")
                .tieuDeCot("Kỳ cước", "Trạng thái", "Số hóa đơn", "Phát sinh", "Đã thu",
                        "Còn nợ", "Tỷ lệ thu");
        for (DongDoanhThuKy d : danhSach) {
            excel.dong(r -> r.chu(d.nhanKy()).chu(d.trangThai().getNhan()).so(d.soHoaDon())
                    .tien(d.phatSinh()).tien(d.daThu()).tien(d.conNo()).tyLe(d.tyLeThu()));
        }
        excel.dongTong(r -> r.chu("TỔNG CỘNG").chu("").so(tong.soHoaDon())
                .tien(tong.phatSinh()).tien(tong.daThu()).tien(tong.conNo())
                .tyLe(DinhDangTien.tyLePhanTram(tong.daThu(), tong.phatSinh())));
        return taiVe(excel.xuat(), "BaoCao-DoanhThuTheoKy.xlsx");
    }

    // =================================================================
    // C.2 — DOANH THU THEO GÓI CƯỚC
    // =================================================================

    @GetMapping("/doanh-thu-goi-cuoc")
    public String doanhThuGoiCuoc(@RequestParam(required = false) Long kyCuocId, Model model) {
        KyCuoc ky = chonKy(kyCuocId);
        List<DongDoanhThuGoi> danhSach = ky == null ? List.of()
                : baoCaoService.doanhThuTheoGoi(ky.getId());
        BigDecimal tongPhatSinh = danhSach.stream().map(DongDoanhThuGoi::phatSinh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        napChonKy(model, ky);
        model.addAttribute("danhSach", danhSach);
        model.addAttribute("tongPhatSinh", tongPhatSinh);
        model.addAttribute("tongSoHoaDon",
                danhSach.stream().mapToLong(DongDoanhThuGoi::soHoaDon).sum());
        model.addAttribute("tongDaThu", danhSach.stream().map(DongDoanhThuGoi::daThu)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("nhanGoi", danhSach.stream()
                .map(d -> d.maGoi() + " — " + d.tenGoi()).toList());
        model.addAttribute("soTien", danhSach.stream().map(DongDoanhThuGoi::phatSinh).toList());
        return "bao-cao/doanh-thu-goi-cuoc";
    }

    @GetMapping("/doanh-thu-goi-cuoc/xuat-excel")
    public ResponseEntity<byte[]> xuatDoanhThuGoiCuoc(
            @RequestParam(required = false) Long kyCuocId) {
        KyCuoc ky = chonKy(kyCuocId);
        List<DongDoanhThuGoi> danhSach = ky == null ? List.of()
                : baoCaoService.doanhThuTheoGoi(ky.getId());
        BigDecimal tongPhatSinh = danhSach.stream().map(DongDoanhThuGoi::phatSinh)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BaoCaoExcel excel = new BaoCaoExcel("Doanh thu theo gói cước", nhanKy(ky))
                .tieuDeCot("Mã gói", "Tên gói", "Số hóa đơn", "Phát sinh", "Đã thu", "Tỷ trọng");
        if (danhSach.isEmpty()) {
            excel.dongTrong("Không có dữ liệu cho kỳ đã chọn.");
        }
        for (DongDoanhThuGoi d : danhSach) {
            excel.dong(r -> r.chu(d.maGoi()).chu(d.tenGoi()).so(d.soHoaDon())
                    .tien(d.phatSinh()).tien(d.daThu()).tyLe(d.tyTrong(tongPhatSinh)));
        }
        if (!danhSach.isEmpty()) {
            excel.dongTong(r -> r.chu("TỔNG CỘNG").chu("")
                    .so(danhSach.stream().mapToLong(DongDoanhThuGoi::soHoaDon).sum())
                    .tien(tongPhatSinh)
                    .tien(danhSach.stream().map(DongDoanhThuGoi::daThu)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .tyLe(BigDecimal.valueOf(100)));
        }
        return taiVe(excel.xuat(), "BaoCao-DoanhThuTheoGoiCuoc.xlsx");
    }

    // =================================================================
    // C.3 — DOANH THU THEO LOẠI DỊCH VỤ
    // =================================================================

    @GetMapping("/doanh-thu-dich-vu")
    public String doanhThuDichVu(@RequestParam(required = false) Long kyCuocId, Model model) {
        KyCuoc ky = chonKy(kyCuocId);
        CoCauCuoc coCau = baoCaoService.coCauCuoc(ky == null ? null : ky.getId());

        napChonKy(model, ky);
        model.addAttribute("coCau", coCau);
        model.addAttribute("khoanMuc", coCau.khoanMuc());
        model.addAttribute("nhanKhoanMuc", coCau.khoanMuc().stream()
                .map(CoCauCuoc.KhoanMuc::ten).toList());
        model.addAttribute("soTienKhoanMuc", coCau.khoanMuc().stream()
                .map(CoCauCuoc.KhoanMuc::soTien).toList());
        return "bao-cao/doanh-thu-dich-vu";
    }

    @GetMapping("/doanh-thu-dich-vu/xuat-excel")
    public ResponseEntity<byte[]> xuatDoanhThuDichVu(
            @RequestParam(required = false) Long kyCuocId) {
        KyCuoc ky = chonKy(kyCuocId);
        CoCauCuoc coCau = baoCaoService.coCauCuoc(ky == null ? null : ky.getId());

        BaoCaoExcel excel = new BaoCaoExcel("Doanh thu theo loại dịch vụ", nhanKy(ky))
                .tieuDeCot("Khoản mục", "Số tiền", "Tỷ trọng");
        if (coCau.rong()) {
            excel.dongTrong("Không có dữ liệu cho kỳ đã chọn.");
        } else {
            for (CoCauCuoc.KhoanMuc m : coCau.khoanMuc()) {
                excel.dong(r -> r.chu(m.ten()).tien(m.soTien()).tyLe(m.tyTrong()));
            }
            excel.dongTong(r -> r.chu("Tổng cước").tien(coCau.tongCuocTruocGiamTru())
                    .tyLe(BigDecimal.valueOf(100)));
            // Giảm trừ lưu dương nhưng là khoản TRỪ, nên hiện dấu âm cho đúng chiều cộng dồn
            excel.dong(r -> r.chu("Giảm trừ").tien(coCau.giamTru().negate()).chu(""));
            excel.dong(r -> r.chu("Tổng trước thuế").tien(coCau.tongTruocThue()).chu(""));
            excel.dong(r -> r.chu("Thuế VAT").tien(coCau.thueVat()).chu(""));
            excel.dongTong(r -> r.chu("TỔNG THANH TOÁN").tien(coCau.tongThanhToan()).chu(""));
        }
        return taiVe(excel.xuat(), "BaoCao-DoanhThuTheoDichVu.xlsx");
    }

    // =================================================================
    // C.4 — THỐNG KÊ THUÊ BAO
    // =================================================================

    @GetMapping("/thue-bao")
    public String thongKeThueBao(Model model) {
        ThongKeThueBao thongKe = baoCaoService.thongKeThueBao();
        model.addAttribute("thongKe", thongKe);
        return "bao-cao/thue-bao";
    }

    @GetMapping("/thue-bao/xuat-excel")
    public ResponseEntity<byte[]> xuatThongKeThueBao() {
        ThongKeThueBao thongKe = baoCaoService.thongKeThueBao();

        BaoCaoExcel excel = new BaoCaoExcel("Thống kê thuê bao", "Toàn bộ hệ thống")
                .tieuDeCot("Chỉ tiêu", "Nhóm", "Số lượng");
        thongKe.theoTrangThai().forEach(m ->
                excel.dong(r -> r.chu("Theo trạng thái").chu(m.nhan()).so(m.soLuong())));
        thongKe.theoLoai().forEach(m ->
                excel.dong(r -> r.chu("Theo loại").chu(m.nhan()).so(m.soLuong())));
        for (int i = 0; i < thongKe.mocThoiGian().size(); i++) {
            String moc = thongKe.mocThoiGian().get(i);
            long moi = thongKe.thueBaoMoi().get(i);
            long roi = thongKe.thueBaoRoiMang().get(i);
            excel.dong(r -> r.chu("Thuê bao mới").chu(moc).so(moi));
            excel.dong(r -> r.chu("Rời mạng").chu(moc).so(roi));
        }
        excel.dongTong(r -> r.chu("TỔNG THUÊ BAO").chu("").so(thongKe.tongThueBao()));
        return taiVe(excel.xuat(), "BaoCao-ThongKeThueBao.xlsx");
    }

    // =================================================================
    // C.5 — TOP THUÊ BAO CƯỚC CAO
    // =================================================================

    @GetMapping("/top-thue-bao")
    public String topThueBao(@RequestParam(required = false) Long kyCuocId,
                             @RequestParam(defaultValue = "10") int soLuong, Model model) {
        KyCuoc ky = chonKy(kyCuocId);
        napChonKy(model, ky);
        model.addAttribute("soLuong", soLuong);
        model.addAttribute("cacMucSoLuong", CAC_MUC_SO_LUONG);
        model.addAttribute("danhSach", ky == null ? List.of()
                : baoCaoService.topThueBaoCuocCao(ky.getId(), soLuong));
        return "bao-cao/top-thue-bao";
    }

    @GetMapping("/top-thue-bao/xuat-excel")
    public ResponseEntity<byte[]> xuatTopThueBao(@RequestParam(required = false) Long kyCuocId,
                                                 @RequestParam(defaultValue = "10") int soLuong) {
        KyCuoc ky = chonKy(kyCuocId);
        List<DongTopThueBao> danhSach = ky == null ? List.of()
                : baoCaoService.topThueBaoCuocCao(ky.getId(), soLuong);

        BaoCaoExcel excel = new BaoCaoExcel("Top " + soLuong + " thuê bao cước cao", nhanKy(ky))
                .tieuDeCot("STT", "Số thuê bao", "Khách hàng", "Gói cước", "Mã hóa đơn",
                        "Tổng thanh toán", "Đã thu", "Còn nợ", "Trạng thái");
        if (danhSach.isEmpty()) {
            excel.dongTrong("Không có dữ liệu cho kỳ đã chọn.");
        }
        int stt = 1;
        for (DongTopThueBao d : danhSach) {
            int so = stt++;
            excel.dong(r -> r.so(so).chu(d.soThueBao()).chu(d.tenKhachHang()).chu(d.maGoi())
                    .chu(d.maHoaDon()).tien(d.tongThanhToan()).tien(d.daThanhToan())
                    .tien(d.conNo()).chu(d.trangThai().getNhan()));
        }
        return taiVe(excel.xuat(), "BaoCao-TopThueBaoCuocCao.xlsx");
    }

    // =================================================================
    // C.6 — SẢN LƯỢNG DỊCH VỤ
    // =================================================================

    @GetMapping("/san-luong")
    public String sanLuong(@RequestParam(required = false) Long kyCuocId, Model model) {
        KyCuoc ky = chonKy(kyCuocId);
        Optional<KyCuoc> kyTruoc = ky == null ? Optional.empty()
                : baoCaoService.kyLienTruoc(ky.getId());

        napChonKy(model, ky);
        model.addAttribute("sanLuong", baoCaoService.sanLuong(ky == null ? null : ky.getId()));
        model.addAttribute("sanLuongTruoc",
                kyTruoc.map(k -> baoCaoService.sanLuong(k.getId())).orElse(null));
        model.addAttribute("kyTruoc", kyTruoc.orElse(null));
        return "bao-cao/san-luong";
    }

    @GetMapping("/san-luong/xuat-excel")
    public ResponseEntity<byte[]> xuatSanLuong(@RequestParam(required = false) Long kyCuocId) {
        KyCuoc ky = chonKy(kyCuocId);
        SanLuongDichVu nay = baoCaoService.sanLuong(ky == null ? null : ky.getId());
        SanLuongDichVu truoc = ky == null ? null : baoCaoService.kyLienTruoc(ky.getId())
                .map(k -> baoCaoService.sanLuong(k.getId())).orElse(null);

        BaoCaoExcel excel = new BaoCaoExcel("Sản lượng dịch vụ", nhanKy(ky))
                .tieuDeCot("Chỉ tiêu", "Đơn vị", "Kỳ này", "Kỳ trước", "Biến động");
        if (nay.rong()) {
            excel.dongTrong("Không có dữ liệu cho kỳ đã chọn.");
        } else {
            excel.dong(r -> r.chu("Số cuộc gọi").chu("cuộc").so(nay.soCuocGoi())
                    .so(truoc == null ? null : truoc.soCuocGoi())
                    .tyLe(nay.bienThienCuocGoi(truoc)));
            excel.dong(r -> r.chu("Phút gọi nội mạng").chu("phút").so(nay.phutNoiMang())
                    .so(truoc == null ? null : truoc.phutNoiMang()).chu(""));
            excel.dong(r -> r.chu("Phút gọi ngoại mạng").chu("phút").so(nay.phutNgoaiMang())
                    .so(truoc == null ? null : truoc.phutNgoaiMang()).chu(""));
            excel.dong(r -> r.chu("Phút gọi quốc tế").chu("phút").so(nay.phutQuocTe())
                    .so(truoc == null ? null : truoc.phutQuocTe()).chu(""));
            excel.dongTong(r -> r.chu("Tổng phút gọi").chu("phút").so(nay.tongPhut())
                    .so(truoc == null ? null : truoc.tongPhut()).tyLe(nay.bienThienPhut(truoc)));
            excel.dong(r -> r.chu("Số tin SMS").chu("tin").so(nay.soSms())
                    .so(truoc == null ? null : truoc.soSms()).tyLe(nay.bienThienSms(truoc)));
            excel.dong(r -> r.chu("Dung lượng dữ liệu").chu("MB").so(nay.soMb())
                    .so(truoc == null ? null : truoc.soMb()).tyLe(nay.bienThienData(truoc)));
        }
        return taiVe(excel.xuat(), "BaoCao-SanLuongDichVu.xlsx");
    }

    // =================================================================
    // C.7 — CÔNG NỢ TỔNG HỢP
    // =================================================================

    @GetMapping("/cong-no")
    public String congNoTongHop(Model model) {
        List<OTuoiNo> bangTuoiNo = congNoService.bangTuoiNo(null);
        model.addAttribute("bangTuoiNo", bangTuoiNo);
        model.addAttribute("nhanTuoiNo", bangTuoiNo.stream().map(o -> o.nhom().getNhan()).toList());
        model.addAttribute("tienTuoiNo", bangTuoiNo.stream().map(OTuoiNo::tongTien).toList());
        model.addAttribute("tongConNo", congNoService.tongConNo(null));
        model.addAttribute("topKhachNo", baoCaoService.topKhachHangNo(10));
        return "bao-cao/cong-no";
    }

    @GetMapping("/cong-no/xuat-excel")
    public ResponseEntity<byte[]> xuatCongNo() {
        List<OTuoiNo> bangTuoiNo = congNoService.bangTuoiNo(null);
        List<DongKhachHangNo> topKhachNo = baoCaoService.topKhachHangNo(10);

        BaoCaoExcel excel = new BaoCaoExcel("Công nợ tổng hợp", "Toàn bộ hệ thống")
                .tieuDeCot("Nhóm tuổi nợ / Khách hàng", "Số hóa đơn", "Số tiền");
        for (OTuoiNo o : bangTuoiNo) {
            excel.dong(r -> r.chu(o.nhom().getNhan()).so(o.soHoaDon()).tien(o.tongTien()));
        }
        excel.dongTong(r -> r.chu("TỔNG CÔNG NỢ")
                .so(bangTuoiNo.stream().mapToLong(OTuoiNo::soHoaDon).sum())
                .tien(congNoService.tongConNo(null)));
        excel.dongTrong("");
        excel.dongTrong("TOP 10 KHÁCH HÀNG NỢ NHIỀU NHẤT");
        for (DongKhachHangNo d : topKhachNo) {
            excel.dong(r -> r.chu(d.maKh() + " — " + d.tenKh()).so(d.soHoaDonNo())
                    .tien(d.tongConNo()));
        }
        return taiVe(excel.xuat(), "BaoCao-CongNoTongHop.xlsx");
    }

    // =================================================================
    // DÙNG CHUNG
    // =================================================================

    /** Kỳ đang chọn; không truyền thì lấy kỳ mới nhất <b>có hóa đơn</b>. */
    private KyCuoc chonKy(Long kyCuocId) {
        if (kyCuocId != null) {
            return baoCaoService.danhSachKy().stream()
                    .filter(k -> k.getId().equals(kyCuocId))
                    .findFirst()
                    .orElse(baoCaoService.kyMacDinh().orElse(null));
        }
        return baoCaoService.kyMacDinh().orElse(null);
    }

    private void napChonKy(Model model, KyCuoc ky) {
        model.addAttribute("danhSachKy", baoCaoService.danhSachKy());
        model.addAttribute("ky", ky);
        model.addAttribute("kyCuocId", ky == null ? null : ky.getId());
    }

    private static String nhanKy(KyCuoc ky) {
        return ky == null ? "Chưa có kỳ nào" : "Kỳ " + ky.getThang() + "/" + ky.getNam();
    }

    private record Tong(long soHoaDon, BigDecimal phatSinh, BigDecimal daThu, BigDecimal conNo) {
    }

    private static Tong congDon(List<DongDoanhThuKy> danhSach) {
        return new Tong(
                danhSach.stream().mapToLong(DongDoanhThuKy::soHoaDon).sum(),
                danhSach.stream().map(DongDoanhThuKy::phatSinh)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                danhSach.stream().map(DongDoanhThuKy::daThu)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                danhSach.stream().map(DongDoanhThuKy::conNo)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static ResponseEntity<byte[]> taiVe(byte[] noiDung, String tenFile) {
        String ten = URLEncoder.encode(tenFile, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + ten)
                .contentType(MediaType.parseMediaType(LOAI_EXCEL))
                .body(noiDung);
    }
}
