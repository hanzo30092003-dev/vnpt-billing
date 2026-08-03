package com.hanzo.billing.controller;

import com.hanzo.billing.dto.BoLocCdr;
import com.hanzo.billing.dto.KetQuaSinhCdr;
import com.hanzo.billing.dto.SinhCdrForm;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.NguonCdr;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.service.ChiTietSuDungService;
import com.hanzo.billing.service.ThueBaoService;
import com.hanzo.billing.service.rating.CdrGeneratorService;
import com.hanzo.billing.service.rating.CdrImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Controller
@RequestMapping("/cdr")
@RequiredArgsConstructor
public class CdrController {

    private static final int SO_DONG_MOI_TRANG = 25;

    private final CdrGeneratorService cdrGeneratorService;
    private final CdrImportService cdrImportService;
    private final ChiTietSuDungService chiTietSuDungService;
    private final ThueBaoService thueBaoService;

    // =================================================================
    // TRA CỨU CDR
    // =================================================================

    @GetMapping
    public String danhSach(@ModelAttribute("boLoc") BoLocCdr boLoc,
                           @RequestParam(defaultValue = "0") int trang,
                           Model model) {

        Pageable phanTrang = PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG);
        model.addAttribute("ketQua", chiTietSuDungService.timCoLoc(boLoc, phanTrang));
        model.addAttribute("tongHop", chiTietSuDungService.tinhTong(boLoc));
        model.addAttribute("chuoiLoc", boLoc.chuoiQuery());
        napDuLieuChonLua(model);
        return "cdr/danh-sach";
    }

    /** Xuất toàn bộ kết quả lọc hiện tại ra file Excel. */
    @GetMapping("/xuat-excel")
    public ResponseEntity<byte[]> xuatExcel(@ModelAttribute("boLoc") BoLocCdr boLoc) {
        byte[] noiDung = chiTietSuDungService.xuatExcel(boLoc);
        String tenFile = "chi-tiet-su-dung-"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + tenFile + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(noiDung);
    }

    private void napDuLieuChonLua(Model model) {
        model.addAttribute("danhSachLoaiDichVu", LoaiDichVu.values());
        model.addAttribute("danhSachHuong", HuongCuocGoi.values());
        model.addAttribute("danhSachTrangThaiTinhCuoc", TrangThaiTinhCuoc.values());
        model.addAttribute("danhSachNguon", NguonCdr.values());
    }

    // =================================================================
    // SINH DỮ LIỆU GIẢ LẬP
    // =================================================================

    @GetMapping("/sinh-du-lieu")
    public String moFormSinh(Model model) {
        model.addAttribute("sinhCdrForm", new SinhCdrForm());
        napThueBao(model);
        return "cdr/sinh-du-lieu";
    }

    @PostMapping("/sinh-du-lieu")
    public String sinh(@Valid @ModelAttribute("sinhCdrForm") SinhCdrForm form,
                       BindingResult ketQuaRangBuoc, Model model) {
        if (ketQuaRangBuoc.hasErrors()) {
            napThueBao(model);
            return "cdr/sinh-du-lieu";
        }
        try {
            KetQuaSinhCdr ketQua = cdrGeneratorService.sinh(form);
            model.addAttribute("ketQua", ketQua);
        } catch (NghiepVuException ex) {
            ketQuaRangBuoc.reject("nghiepVu", ex.getMessage());
        }
        napThueBao(model);
        return "cdr/sinh-du-lieu";
    }

    // =================================================================
    // NHẬP CDR TỪ FILE CSV
    // =================================================================

    @GetMapping("/import")
    public String moFormImport() {
        return "cdr/import";
    }

    @PostMapping("/import")
    public String nhapFile(@RequestParam("file") MultipartFile file, Model model) {
        try {
            model.addAttribute("ketQua", cdrImportService.nhap(file));
        } catch (NghiepVuException ex) {
            model.addAttribute("thongBaoLoi", ex.getMessage());
        }
        return "cdr/import";
    }

    /**
     * Tải file CSV mẫu.
     *
     * <p>Đọc từ classpath thay vì từ thư mục {@code docs/} để vẫn chạy được khi ứng dụng
     * được đóng gói thành file JAR chạy độc lập.</p>
     */
    @GetMapping("/tai-file-mau")
    public ResponseEntity<Resource> taiFileMau() {
        Resource file = new ClassPathResource("mau-cdr.csv");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-cdr.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(file);
    }

    /** Danh sách thuê bao để chọn phạm vi sinh dữ liệu. */
    private void napThueBao(Model model) {
        model.addAttribute("danhSachThueBao",
                thueBaoService.timKiem(null, TrangThaiThueBao.HOAT_DONG, null, null,
                        PageRequest.of(0, 200)).getContent());
    }
}
