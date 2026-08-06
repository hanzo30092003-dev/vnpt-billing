package com.hanzo.billing.controller;

import com.hanzo.billing.dto.ThueBaoForm;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.HinhThucNapTien;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.KhachHangService;
import com.hanzo.billing.service.KyCuocService;
import com.hanzo.billing.service.ThueBaoService;
import com.hanzo.billing.service.rating.ThamSoTinhCuoc;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/thue-bao")
@RequiredArgsConstructor
public class ThueBaoController {

    private static final int SO_DONG_MOI_TRANG = 15;

    private final ThueBaoService thueBaoService;
    private final KhachHangService khachHangService;
    private final KyCuocService kyCuocService;

    @GetMapping
    public String danhSach(@RequestParam(required = false) String tuKhoa,
                           @RequestParam(required = false) TrangThaiThueBao trangThai,
                           @RequestParam(required = false) LoaiThueBao loaiThueBao,
                           @RequestParam(required = false) Long goiCuocId,
                           @RequestParam(defaultValue = "0") int trang,
                           Model model) {

        Pageable phanTrang = PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG,
                Sort.by(Sort.Direction.DESC, "id"));
        Page<ThueBao> ketQua = thueBaoService.timKiem(tuKhoa, trangThai, loaiThueBao, goiCuocId, phanTrang);

        model.addAttribute("ketQua", ketQua);
        model.addAttribute("danhSachTrangThai", TrangThaiThueBao.values());
        model.addAttribute("danhSachLoaiThueBao", LoaiThueBao.values());
        model.addAttribute("danhSachGoiCuoc", thueBaoService.layGoiCuocConHieuLuc());

        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("loaiThueBao", loaiThueBao);
        model.addAttribute("goiCuocId", goiCuocId);
        model.addAttribute("chuoiLoc", chuoiLoc(tuKhoa, trangThai, loaiThueBao, goiCuocId));

        return "thue-bao/danh-sach";
    }

    /** Form đăng ký. Có thể nhận sẵn khachHangId khi đi từ màn hình chi tiết khách hàng. */
    @GetMapping("/dang-ky")
    public String moFormDangKy(@RequestParam(required = false) Long khachHangId, Model model) {
        ThueBaoForm form = new ThueBaoForm();
        form.setKhachHangId(khachHangId);
        form.setLoaiThueBao(LoaiThueBao.TRA_SAU);
        form.setNgayKichHoat(LocalDate.now());
        form.setHanMucTinDung(BigDecimal.ZERO);
        model.addAttribute("thueBaoForm", form);
        napDuLieuChonLua(model);
        return "thue-bao/dang-ky";
    }

    @PostMapping("/luu")
    public String luu(@Valid @ModelAttribute("thueBaoForm") ThueBaoForm form,
                      BindingResult ketQuaRangBuoc,
                      Model model,
                      RedirectAttributes redirectAttributes) {

        if (ketQuaRangBuoc.hasErrors()) {
            napDuLieuChonLua(model);
            return "thue-bao/dang-ky";
        }
        try {
            ThueBao daLuu = thueBaoService.dangKyMoi(form);
            redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                    "Đã đăng ký thuê bao " + daLuu.getSoThueBao());
            return "redirect:/thue-bao/" + daLuu.getId();
        } catch (NghiepVuException ex) {
            // Bắt tại chỗ để vẽ lại form kèm dữ liệu vừa nhập, thay vì chuyển hướng đi mất
            ketQuaRangBuoc.reject("nghiepVu", ex.getMessage());
            napDuLieuChonLua(model);
            return "thue-bao/dang-ky";
        }
    }

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Long id, Model model) {
        ThueBao thueBao = thueBaoService.layTheoId(id);
        model.addAttribute("thueBao", thueBao);
        model.addAttribute("lichSuGoiCuoc", thueBaoService.layLichSuGoiCuoc(id));
        model.addAttribute("lichSuTrangThai", thueBaoService.layLichSuTrangThai(id));
        model.addAttribute("soCaiSoDu", thueBaoService.layLichSuBienDongSoDu(id));
        model.addAttribute("nguongCanhBaoSoDu", ThamSoTinhCuoc.NGUONG_CANH_BAO_SO_DU);
        model.addAttribute("soDuDuoiNguong",
                thueBao.getLoaiThueBao() == LoaiThueBao.TRA_TRUOC
                        && thueBao.getTrangThai() != TrangThaiThueBao.DA_THANH_LY
                        && thueBao.getSoDu() != null
                        && thueBao.getSoDu().compareTo(
                                ThamSoTinhCuoc.NGUONG_CANH_BAO_SO_DU) < 0);
        model.addAttribute("trangThaiCoTheChuyen",
                thueBaoService.cacTrangThaiCoTheChuyen(thueBao.getTrangThai()));
        model.addAttribute("danhSachGoiCuoc", thueBaoService.layGoiCuocConHieuLuc());
        model.addAttribute("danhSachHinhThucNap", HinhThucNapTien.values());
        model.addAttribute("ngayHieuLucDoiGoi", thueBaoService.ngayHieuLucDoiGoi());
        // Danh sách kỳ để mở bảng đối soát cước; đối soát luôn phải gắn với một kỳ cụ thể
        model.addAttribute("danhSachKy", kyCuocService.layTatCa());
        return "thue-bao/chi-tiet";
    }

    @PostMapping("/{id}/chuyen-trang-thai")
    public String chuyenTrangThai(@PathVariable Long id,
                                  @RequestParam TrangThaiThueBao trangThaiMoi,
                                  @RequestParam String lyDo,
                                  RedirectAttributes redirectAttributes) {
        thueBaoService.chuyenTrangThai(id, trangThaiMoi, lyDo);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã chuyển thuê bao sang trạng thái " + trangThaiMoi.getNhan());
        return "redirect:/thue-bao/" + id;
    }

    @PostMapping("/{id}/doi-goi-cuoc")
    public String doiGoiCuoc(@PathVariable Long id,
                             @RequestParam Long goiCuocMoiId,
                             RedirectAttributes redirectAttributes) {
        thueBaoService.doiGoiCuoc(id, goiCuocMoiId);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã đổi gói cước, hiệu lực từ ngày " + thueBaoService.ngayHieuLucDoiGoi());
        return "redirect:/thue-bao/" + id;
    }

    @PostMapping("/{id}/nap-tien")
    public String napTien(@PathVariable Long id,
                          @RequestParam BigDecimal soTien,
                          @RequestParam HinhThucNapTien hinhThuc,
                          RedirectAttributes redirectAttributes) {
        thueBaoService.napTien(id, soTien, hinhThuc);
        redirectAttributes.addFlashAttribute("thongBaoThanhCong",
                "Đã nạp " + soTien + " đồng vào tài khoản thuê bao");
        return "redirect:/thue-bao/" + id;
    }

    private void napDuLieuChonLua(Model model) {
        model.addAttribute("danhSachKhachHang", khachHangService.layKhachHangDangHoatDong());
        model.addAttribute("danhSachGoiCuoc", thueBaoService.layGoiCuocConHieuLuc());
        model.addAttribute("danhSachLoaiThueBao", LoaiThueBao.values());
    }

    private String chuoiLoc(String tuKhoa, TrangThaiThueBao trangThai,
                            LoaiThueBao loaiThueBao, Long goiCuocId) {
        StringBuilder sb = new StringBuilder();
        if (tuKhoa != null && !tuKhoa.isBlank()) {
            sb.append("&tuKhoa=").append(java.net.URLEncoder.encode(tuKhoa,
                    java.nio.charset.StandardCharsets.UTF_8));
        }
        if (trangThai != null) {
            sb.append("&trangThai=").append(trangThai.name());
        }
        if (loaiThueBao != null) {
            sb.append("&loaiThueBao=").append(loaiThueBao.name());
        }
        if (goiCuocId != null) {
            sb.append("&goiCuocId=").append(goiCuocId);
        }
        return sb.toString();
    }
}
