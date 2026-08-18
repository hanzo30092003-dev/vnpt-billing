package com.hanzo.billing.controller;

import com.hanzo.billing.dto.baocao.DongDoanhThuKy;
import com.hanzo.billing.dto.baocao.MucBieuDo;
import com.hanzo.billing.dto.baocao.ThongTinDashboard;
import com.hanzo.billing.service.baocao.BaoCaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Trang chủ — dashboard số liệu (Phase 6 mục B).
 *
 * <p>Thay trang giới thiệu tạm của Phase 0. Mọi con số <b>đọc từ cột đã ghi</b>
 * ({@code hoa_don.con_no}, {@code hoa_don.tong_thanh_toan}) chứ không cộng lại từ bảng
 * {@code thanh_toan} — ràng buộc ② của Phase 5.</p>
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final BaoCaoService baoCaoService;

    @GetMapping("/")
    public String trangChu(Model model) {
        ThongTinDashboard dashboard = baoCaoService.dashboard();
        model.addAttribute("dashboard", dashboard);

        // Dựng sẵn các mảng cho biểu đồ ở đây thay vì để template tự chiếu: bảng và biểu đồ
        // khi đó chắc chắn đọc cùng một nguồn, cùng thứ tự.
        model.addAttribute("nhanKy",
                dashboard.doanhThuCacKy().stream().map(DongDoanhThuKy::nhanKy).toList());
        model.addAttribute("soPhatSinh",
                dashboard.doanhThuCacKy().stream().map(DongDoanhThuKy::phatSinh).toList());
        model.addAttribute("soDaThu",
                dashboard.doanhThuCacKy().stream().map(DongDoanhThuKy::daThu).toList());

        model.addAttribute("nhanGoi",
                dashboard.coCauGoiCuoc().stream().map(MucBieuDo::nhan).toList());
        model.addAttribute("soGoi",
                dashboard.coCauGoiCuoc().stream().map(MucBieuDo::soLuong).toList());
        model.addAttribute("nhanTrangThai",
                dashboard.coCauTrangThai().stream().map(MucBieuDo::nhan).toList());
        model.addAttribute("soTrangThai",
                dashboard.coCauTrangThai().stream().map(MucBieuDo::soLuong).toList());
        return "index";
    }

    /**
     * {@code /quan-tri} chưa phải một trang, nhưng phải dẫn đi đâu đó.
     *
     * <p>Vệt bánh mì (breadcrumb) suy tự động từ đường dẫn nên ở màn hình
     * {@code /quan-tri/nguoi-dung} nó sinh ra một mắt xích "Quản trị" trỏ về
     * {@code /quan-tri}. Không có mapping này thì đó là một liên kết dẫn tới 404 — đúng loại
     * lỗi mà bất biến điều hướng của Phase 6 dựng ra để chặn, chỉ khác là lần này nó đến từ
     * một liên kết <b>sinh lúc chạy</b> nên phép kiểm tĩnh không nhìn thấy.</p>
     *
     * <p>Chuyển hướng thay vì dựng một trang mục lục cho đúng một mục con: một trang chỉ có
     * một liên kết là một cú bấm thừa.</p>
     */
    @GetMapping("/quan-tri")
    public String quanTri() {
        return "redirect:/quan-tri/nguoi-dung";
    }
}
