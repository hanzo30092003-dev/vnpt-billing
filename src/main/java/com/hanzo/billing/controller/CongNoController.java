package com.hanzo.billing.controller;

import com.hanzo.billing.dto.OTuoiNo;
import com.hanzo.billing.enums.NhomTuoiNo;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.CongNoService;
import com.hanzo.billing.service.HoaDonService;
import com.hanzo.billing.service.rating.ThamSoTinhCuoc;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/cong-no")
@RequiredArgsConstructor
public class CongNoController {

    private final CongNoService congNoService;
    private final HoaDonService hoaDonService;

    @GetMapping
    public String danhSach(@RequestParam(required = false) String khachHang,
                           @RequestParam(required = false) NhomTuoiNo nhomTuoiNo,
                           Model model) {
        // Cập nhật quá hạn trước khi dựng bảng: nếu không, một hóa đơn vừa quá hạn hôm nay
        // sẽ hiện đúng ở cột "số ngày" nhưng badge trạng thái vẫn còn là CHUA_TT.
        hoaDonService.capNhatQuaHan();

        List<OTuoiNo> bangTuoiNo = congNoService.bangTuoiNo(khachHang);
        model.addAttribute("danhSach", congNoService.danhSachConNo(khachHang, nhomTuoiNo));
        model.addAttribute("bangTuoiNo", bangTuoiNo);
        // Dựng sẵn hai mảng cho biểu đồ ở đây thay vì dùng cú pháp chiếu của Thymeleaf
        // trong template: bảng và biểu đồ khi đó chắc chắn đọc cùng một nguồn, cùng thứ tự.
        model.addAttribute("nhanTuoiNo", bangTuoiNo.stream().map(o -> o.nhom().getNhan()).toList());
        model.addAttribute("tienTuoiNo", bangTuoiNo.stream().map(OTuoiNo::tongTien).toList());
        model.addAttribute("tongConNo", congNoService.tongConNo(khachHang));
        model.addAttribute("deXuatTamNgung", congNoService.deXuatTamNgung());
        model.addAttribute("vuotHanMuc", congNoService.thueBaoVuotHanMuc());
        model.addAttribute("danhSachNhom", NhomTuoiNo.values());
        model.addAttribute("khachHang", khachHang);
        model.addAttribute("nhomTuoiNo", nhomTuoiNo);
        model.addAttribute("nguongTamNgung", ThamSoTinhCuoc.SO_NGAY_QUA_HAN_DE_XUAT_TAM_NGUNG);
        model.addAttribute("chuoiLoc", chuoiLoc(khachHang, nhomTuoiNo));
        return "cong-no/danh-sach";
    }

    @PostMapping("/{hoaDonId}/tam-ngung")
    public String tamNgung(@PathVariable Long hoaDonId, RedirectAttributes ra) {
        try {
            congNoService.tamNgungViNoCuoc(hoaDonId);
            ra.addFlashAttribute("thongBao",
                    "Đã chuyển thuê bao sang trạng thái Tạm ngừng một chiều và ghi lịch sử.");
        } catch (NghiepVuException ex) {
            ra.addFlashAttribute("thongBaoLoi", ex.getMessage());
        }
        return "redirect:/cong-no";
    }

    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> xuatExcel(@RequestParam(required = false) String khachHang,
                                            @RequestParam(required = false) NhomTuoiNo nhomTuoiNo) {
        String tenFile = URLEncoder.encode("CongNo.xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + tenFile)
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(congNoService.xuatExcel(khachHang, nhomTuoiNo));
    }

    private static String chuoiLoc(String khachHang, NhomTuoiNo nhomTuoiNo) {
        StringBuilder sb = new StringBuilder();
        if (khachHang != null && !khachHang.isBlank()) {
            sb.append("&khachHang=").append(
                    URLEncoder.encode(khachHang.trim(), StandardCharsets.UTF_8));
        }
        if (nhomTuoiNo != null) {
            sb.append("&nhomTuoiNo=").append(nhomTuoiNo.name());
        }
        return sb.toString();
    }
}
