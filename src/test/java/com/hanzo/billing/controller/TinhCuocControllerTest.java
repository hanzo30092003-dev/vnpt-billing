package com.hanzo.billing.controller;

import com.hanzo.billing.dto.BangDoiSoat;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.enums.VaiTro;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.security.NguoiDungPrincipal;
import com.hanzo.billing.service.KyCuocService;
import com.hanzo.billing.service.rating.BillingService;
import com.hanzo.billing.service.rating.DoiSoatCuocService;
import com.hanzo.billing.service.rating.RatingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử màn hình điều khiển tính cước ở tầng HTTP.
 *
 * <p>Tầng nghiệp vụ được thay bằng mock: mục tiêu ở đây là kiểm <b>phân quyền</b> và
 * <b>cách xử lý lỗi nghiệp vụ</b>, không phải kiểm lại engine — engine đã có bộ test riêng
 * và không nên chạy thật trên CSDL trong một test HTTP.</p>
 *
 * <p>Dùng {@link NguoiDungPrincipal} thật thay vì {@code @WithMockUser}: layout đọc
 * {@code #authentication.principal.hoTen} và {@code .vaiTro.nhan}, mà người dùng giả mặc
 * định của Spring Security không có hai thuộc tính đó nên trang không vẽ được. Dùng
 * principal thật cũng đúng với cách hệ thống chạy thật hơn.</p>
 */
@SpringBootTest
@DisplayName("Màn hình điều khiển tính cước")
class TinhCuocControllerTest {

    private static final Long KY_ID = 1L;

    @Autowired private WebApplicationContext webApplicationContext;

    @MockitoBean private RatingService ratingService;
    @MockitoBean private BillingService billingService;
    @MockitoBean private DoiSoatCuocService doiSoatCuocService;
    @MockitoBean private KyCuocService kyCuocService;

    private MockMvc mockMvc;

    @BeforeEach
    void chuanBi() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("1. Vai trò KE_TOAN không vào được màn hình tính cước — trả về 403")
    void vaiTroKeToan_thiBiTuChoi() throws Exception {
        mockMvc.perform(get("/tinh-cuoc").with(user(nguoiDung(VaiTro.KE_TOAN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2. Vai trò NHAN_VIEN cũng không vào được")
    void vaiTroNhanVien_thiBiTuChoi() throws Exception {
        mockMvc.perform(get("/tinh-cuoc").with(user(nguoiDung(VaiTro.NHAN_VIEN))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("3. Vai trò ADMIN vào được và trang vẽ ra bình thường")
    void vaiTroAdmin_thiVaoDuoc() throws Exception {
        when(kyCuocService.layTatCa()).thenReturn(List.of(ky(TrangThaiKyCuoc.MO)));

        mockMvc.perform(get("/tinh-cuoc").with(user(nguoiDung(VaiTro.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("4. Chạy tính cước trên kỳ đã chốt: hiện thông báo lỗi, không chạy thao tác khác")
    void tinhCuocKyDaChot_thiBaoLoi() throws Exception {
        when(kyCuocService.layTheoId(KY_ID)).thenReturn(ky(TrangThaiKyCuoc.DA_CHOT));
        when(ratingService.ratingKy(any(KyCuoc.class))).thenThrow(
                new NghiepVuException("Kỳ cước tháng 6/2026 đã chốt, không thể tính lại"));

        mockMvc.perform(post("/tinh-cuoc/{id}/tinh-cuoc", KY_ID)
                        .with(user(nguoiDung(VaiTro.ADMIN))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/tinh-cuoc"))
                .andExpect(flash().attribute("thongBaoLoi",
                        "Kỳ cước tháng 6/2026 đã chốt, không thể tính lại"));

        verify(billingService, never()).billingKy(any());
    }

    @Test
    @DisplayName("5. Huỷ hóa đơn khi kỳ đã có thanh toán: bị chặn, hiện đúng lý do")
    void huyHoaDonKhiDaCoThanhToan_thiBiChan() throws Exception {
        when(kyCuocService.layTheoId(KY_ID)).thenReturn(ky(TrangThaiKyCuoc.MO));
        when(billingService.huyBillingKy(any(KyCuoc.class))).thenThrow(new NghiepVuException(
                "Kỳ cước tháng 6/2026 đã ghi nhận 12 giao dịch thanh toán. "
                        + "Không thể xóa hóa đơn đã thu tiền"));

        mockMvc.perform(post("/tinh-cuoc/{id}/huy-hoa-don", KY_ID)
                        .with(user(nguoiDung(VaiTro.ADMIN))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("thongBaoLoi"));
    }

    @Test
    @DisplayName("6. Thao tác thành công thì hiện hộp kết quả, không kèm thông báo lỗi")
    void chayThanhCong_thiHienHopKetQua() throws Exception {
        when(kyCuocService.layTheoId(KY_ID)).thenReturn(ky(TrangThaiKyCuoc.MO));
        when(billingService.huyBillingKy(any(KyCuoc.class))).thenReturn(58);

        mockMvc.perform(post("/tinh-cuoc/{id}/huy-hoa-don", KY_ID)
                        .with(user(nguoiDung(VaiTro.ADMIN))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("ketQuaChay"))
                .andExpect(flash().attributeCount(1));
    }

    @Test
    @DisplayName("7. Bảng đối soát của thuê bao chưa có hóa đơn: trang bình thường, không lỗi 500")
    void doiSoatKhongCoHoaDon_thiVanMoDuocTrang() throws Exception {
        // Thuê bao có đủ khách hàng và gói cước như dữ liệu thật, chỉ KHÔNG có hóa đơn —
        // đúng tình huống thuê bao trả trước hoặc kỳ chưa lập hóa đơn
        when(doiSoatCuocService.dungBang(9L, KY_ID)).thenReturn(new BangDoiSoat(
                thueBaoDayDu(), ky(TrangThaiKyCuoc.MO), goi(), null, null,
                List.of(), List.of(), List.of()));
        when(kyCuocService.layTatCa()).thenReturn(List.of(ky(TrangThaiKyCuoc.MO)));

        mockMvc.perform(get("/tinh-cuoc/doi-soat/{tb}/{ky}", 9L, KY_ID)
                        .with(user(nguoiDung(VaiTro.ADMIN))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("8. Chốt kỳ chưa có hóa đơn: bị chặn ở tầng nghiệp vụ")
    void chotKyChuaCoHoaDon_thiBiChan() throws Exception {
        when(kyCuocService.chotKy(anyLong())).thenThrow(new NghiepVuException(
                "Kỳ cước tháng 6/2026 chưa có hóa đơn nào. Chốt một kỳ trống sẽ khóa vĩnh viễn"));

        mockMvc.perform(post("/tinh-cuoc/{id}/chot-ky", KY_ID)
                        .with(user(nguoiDung(VaiTro.ADMIN))).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("thongBaoLoi"));
    }

    @Test
    @DisplayName("9. Chưa đăng nhập thì bị đẩy về trang đăng nhập, không lộ nội dung")
    void chuaDangNhap_thiVeTrangDangNhap() throws Exception {
        mockMvc.perform(get("/tinh-cuoc"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("http://localhost/dang-nhap"));
    }

    private static ThueBao thueBaoDayDu() {
        KhachHang kh = new KhachHang();
        kh.setId(10L);
        kh.setMaKh("KH000010");
        kh.setTenKh("Khách thử");

        ThueBao tb = new ThueBao();
        tb.setId(9L);
        tb.setSoThueBao("0900000009");
        tb.setKhachHang(kh);
        tb.setGoiCuoc(goi());
        tb.setLoaiThueBao(LoaiThueBao.TRA_TRUOC);
        tb.setTrangThai(TrangThaiThueBao.HOAT_DONG);
        tb.setNgayKichHoat(LocalDate.of(2025, 1, 1));
        return tb;
    }

    private static GoiCuoc goi() {
        GoiCuoc goi = new GoiCuoc();
        goi.setId(5L);
        goi.setMaGoi("TT01");
        goi.setTenGoi("Trả trước chuẩn");
        goi.setCuocThueBaoThang(BigDecimal.ZERO);
        goi.setPhutNoiMangMienPhi(0);
        goi.setPhutNgoaiMangMienPhi(0);
        goi.setSmsMienPhi(0);
        goi.setDataMienPhiMb(0);
        return goi;
    }

    private static NguoiDungPrincipal nguoiDung(VaiTro vaiTro) {
        NguoiDung nd = new NguoiDung();
        nd.setId(1L);
        nd.setTenDangNhap("nguoidung_" + vaiTro.name().toLowerCase());
        nd.setMatKhau("{noop}khong-dung-toi");
        nd.setHoTen("Người dùng thử");
        nd.setVaiTro(vaiTro);
        nd.setTrangThai(true);
        return new NguoiDungPrincipal(nd);
    }

    private static KyCuoc ky(TrangThaiKyCuoc trangThai) {
        KyCuoc ky = new KyCuoc();
        ky.setId(KY_ID);
        ky.setThang(6);
        ky.setNam(2026);
        ky.setNgayBatDau(LocalDate.of(2026, 6, 1));
        ky.setNgayKetThuc(LocalDate.of(2026, 6, 30));
        ky.setTrangThai(trangThai);
        ky.setSoCdrXuLy(0);
        ky.setSoHoaDonTao(0);
        ky.setTongDoanhThu(BigDecimal.ZERO);
        return ky;
    }
}
