package com.hanzo.billing.controller;

import com.hanzo.billing.dto.BoLocHoaDon;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.enums.TrangThaiHoaDon;
import com.hanzo.billing.service.HoaDonService;
import com.hanzo.billing.service.KyCuocService;
import com.hanzo.billing.service.pdf.HoaDonPdfService;
import com.hanzo.billing.util.DocSoTienUtil;
import com.hanzo.billing.util.ThamSoPhanTrang;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Màn hình hóa đơn — danh sách, chi tiết, xuất Excel.
 *
 * <p>Phân quyền do {@code SecurityConfig} giữ: {@code /hoa-don/**} chỉ mở cho
 * {@code KE_TOAN} và {@code ADMIN}.</p>
 */
@Controller
@RequestMapping("/hoa-don")
@RequiredArgsConstructor
public class HoaDonController {

    private static final int SO_DONG_MOI_TRANG = 20;

    private final HoaDonService hoaDonService;
    private final KyCuocService kyCuocService;
    private final HoaDonPdfService hoaDonPdfService;

    /** Tên file trong header {@code Content-Disposition} phải mã hoá theo RFC 5987. */
    private static String maHoaUrl(String tenFile) {
        return URLEncoder.encode(tenFile, StandardCharsets.UTF_8).replace("+", "%20");
    }

    @GetMapping
    public String danhSach(@ModelAttribute("boLoc") BoLocHoaDon boLoc,
                           @RequestParam(defaultValue = "0") int trang,
                           Model model) {
        // Cập nhật quá hạn ngay khi mở màn hình: lịch chạy nền lo trường hợp không ai mở
        // suốt ngày, còn lần gọi này lo trường hợp ứng dụng vừa khởi động lại và đã lỡ
        // mất giờ hẹn. Hàm chỉ đổi hóa đơn thực sự quá hạn nên gọi nhiều lần vô hại.
        hoaDonService.capNhatQuaHan();

        Page<HoaDon> ketQua = hoaDonService.timKiem(
                boLoc, PageRequest.of(ThamSoPhanTrang.trangHopLe(trang), SO_DONG_MOI_TRANG));

        model.addAttribute("ketQua", ketQua);
        model.addAttribute("tongHop", hoaDonService.tongHop(boLoc));
        model.addAttribute("danhSachKy", kyCuocService.layTatCa());
        model.addAttribute("danhSachTrangThai", TrangThaiHoaDon.values());
        model.addAttribute("chuoiLoc", boLoc.chuoiQuery());
        return "hoa-don/danh-sach";
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        HoaDon hoaDon = hoaDonService.layTheoId(id);
        model.addAttribute("hoaDon", hoaDon);
        model.addAttribute("khoanMuc", hoaDonService.layKhoanMuc(id));
        model.addAttribute("lichSuThanhToan", hoaDonService.layLichSuThanhToan(id));
        model.addAttribute("tienBangChu", DocSoTienUtil.docSoTien(hoaDon.getTongThanhToan()));
        return "hoa-don/chi-tiet";
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> xuatPdf(@PathVariable Long id) {
        HoaDon hoaDon = hoaDonService.layTheoId(id);
        byte[] noiDung = hoaDonPdfService.xuat(hoaDon, hoaDonService.layKhoanMuc(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename*=UTF-8''" + maHoaUrl(HoaDonPdfService.tenFile(hoaDon)))
                .contentType(MediaType.APPLICATION_PDF)
                .body(noiDung);
    }

    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> xuatExcel(@ModelAttribute("boLoc") BoLocHoaDon boLoc) {
        byte[] noiDung = hoaDonService.xuatExcel(boLoc);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + maHoaUrl("DanhSachHoaDon.xlsx"))
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(noiDung);
    }
}
