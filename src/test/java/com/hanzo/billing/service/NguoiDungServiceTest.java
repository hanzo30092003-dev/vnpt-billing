package com.hanzo.billing.service;

import com.hanzo.billing.dto.NguoiDungForm;
import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.enums.VaiTro;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.NguoiDungRepository;
import com.hanzo.billing.security.NguoiDungPrincipal;
import com.hanzo.billing.service.impl.NguoiDungServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Quản lý tài khoản người dùng — kiểm các luật <b>không sửa được bằng chính phần mềm</b>
 * nếu để vi phạm.
 *
 * <h2>Vì sao phân hệ này cần test riêng</h2>
 * <p>Ba màn hình nghiệp vụ khác làm sai thì mở lên sửa lại được. Màn hình này thì không:
 * khoá nốt quản trị viên cuối cùng, hoặc tự hạ quyền của chính mình, là <b>mất luôn cái màn
 * hình dùng để sửa</b>. Đường ra duy nhất khi đó là mở CSDL lên gõ SQL tay — tức đúng thứ
 * mà phân hệ này sinh ra để không phải làm nữa.</p>
 *
 * <p>Chạy bằng Mockito, KHÔNG khởi động Spring context và không cần CSDL. Riêng
 * {@link PasswordEncoder} dùng <b>bản thật</b> chứ không giả lập: thứ đáng kiểm ở đây là
 * "mật khẩu có thật sự được băm không", mà một encoder giả lập thì trả về đúng cái mình dạy
 * nó trả về, nên nó chứng minh chính nó.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Quản lý tài khoản người dùng")
class NguoiDungServiceTest {

    private static final Long ID_ADMIN = 1L;
    private static final Long ID_NHAN_VIEN = 2L;
    private static final String MAT_KHAU_CU = "matkhaucu";
    private static final String MAT_KHAU_MOI = "matkhaumoi";

    @Mock private NguoiDungRepository nguoiDungRepository;
    @Mock private NhatKyService nhatKyService;
    @Mock private SessionRegistry sessionRegistry;

    @Spy private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @InjectMocks private NguoiDungServiceImpl nguoiDungService;

    @AfterEach
    void donSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // =================================================================
    // DỰNG DỮ LIỆU
    // =================================================================

    private NguoiDung taiKhoan(Long id, String tenDangNhap, VaiTro vaiTro, boolean dangHoatDong) {
        NguoiDung nd = new NguoiDung();
        nd.setId(id);
        nd.setTenDangNhap(tenDangNhap);
        nd.setHoTen("Người dùng " + tenDangNhap);
        nd.setVaiTro(vaiTro);
        nd.setTrangThai(dangHoatDong);
        nd.setSoLanSai(0);
        nd.setMatKhau(new BCryptPasswordEncoder().encode(MAT_KHAU_CU));
        return nd;
    }

    /** Đặt người đang đăng nhập vào SecurityContext, như khi có một request thật. */
    private void dangNhapBang(NguoiDung nd) {
        NguoiDungPrincipal principal = new NguoiDungPrincipal(nd);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private NguoiDungForm formSua(Long id, VaiTro vaiTro, String matKhau) {
        NguoiDungForm form = new NguoiDungForm();
        form.setId(id);
        form.setTenDangNhap("khongdung");   // sửa thì tên đăng nhập chỉ đọc, service bỏ qua
        form.setHoTen("Tên đã đổi");
        form.setVaiTro(vaiTro);
        form.setMatKhau(matKhau);
        return form;
    }

    private NguoiDungForm formThem(String tenDangNhap, String matKhau) {
        NguoiDungForm form = new NguoiDungForm();
        form.setTenDangNhap(tenDangNhap);
        form.setHoTen("Nhân viên mới");
        form.setVaiTro(VaiTro.NHAN_VIEN);
        form.setMatKhau(matKhau);
        return form;
    }

    private NguoiDung batBanGhiDaLuu() {
        ArgumentCaptor<NguoiDung> bat = ArgumentCaptor.forClass(NguoiDung.class);
        verify(nguoiDungRepository).save(bat.capture());
        return bat.getValue();
    }

    // =================================================================
    @Nested
    @DisplayName("Mật khẩu")
    class MatKhau {

        @Test
        @DisplayName("1. ⭐ Thêm mới thì mật khẩu được băm, CSDL không hề thấy mật khẩu thô")
        void themMoiThiMatKhauDuocBam() {
            when(nguoiDungRepository.existsByTenDangNhap("nhanvien02")).thenReturn(false);
            when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(i -> i.getArgument(0));

            nguoiDungService.luu(formThem("nhanvien02", MAT_KHAU_MOI));

            NguoiDung daLuu = batBanGhiDaLuu();
            assertThat(daLuu.getMatKhau())
                    .as("Mật khẩu thô KHÔNG được có mặt trong bản ghi đem đi lưu")
                    .isNotEqualTo(MAT_KHAU_MOI)
                    .startsWith("$2");
            assertThat(passwordEncoder.matches(MAT_KHAU_MOI, daLuu.getMatKhau()))
                    .as("Chuỗi băm phải khớp lại được với mật khẩu vừa đặt, nếu không thì tài "
                            + "khoản tạo ra đăng nhập không nổi")
                    .isTrue();
            assertThat(daLuu.getTrangThai()).isTrue();
            assertThat(daLuu.getNgayTao()).isNotNull();
        }

        @Test
        @DisplayName("2. Thêm mới mà bỏ trống mật khẩu thì bị chặn")
        void themMoiBoTrongMatKhauThiBiChan() {
            when(nguoiDungRepository.existsByTenDangNhap(anyString())).thenReturn(false);

            assertThatThrownBy(() -> nguoiDungService.luu(formThem("nhanvien02", "  ")))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đặt mật khẩu");

            verify(nguoiDungRepository, never()).save(any());
        }

        @Test
        @DisplayName("3. Mật khẩu ngắn hơn 6 ký tự bị chặn")
        void matKhauQuaNganBiChan() {
            when(nguoiDungRepository.existsByTenDangNhap(anyString())).thenReturn(false);

            assertThatThrownBy(() -> nguoiDungService.luu(formThem("nhanvien02", "12345")))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("6 ký tự");

            verify(nguoiDungRepository, never()).save(any());
        }

        @Test
        @DisplayName("4. ⭐ Sửa mà bỏ trống ô mật khẩu thì giữ nguyên mật khẩu cũ")
        void suaBoTrongOMatKhauThiGiuNguyen() {
            NguoiDung nd = taiKhoan(ID_NHAN_VIEN, "nhanvien01", VaiTro.NHAN_VIEN, true);
            String hashCu = nd.getMatKhau();
            when(nguoiDungRepository.findById(ID_NHAN_VIEN)).thenReturn(Optional.of(nd));
            when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(i -> i.getArgument(0));

            nguoiDungService.luu(formSua(ID_NHAN_VIEN, VaiTro.NHAN_VIEN, ""));

            NguoiDung daLuu = batBanGhiDaLuu();
            assertThat(daLuu.getMatKhau())
                    .as("Sửa họ tên mà mật khẩu bị đổi theo thì người dùng đột nhiên không "
                            + "đăng nhập được, và không ai hiểu vì sao")
                    .isEqualTo(hashCu);
            assertThat(daLuu.getHoTen()).isEqualTo("Tên đã đổi");
        }

        @Test
        @DisplayName("5. Sửa có nhập mật khẩu mới thì mật khẩu cũ hết hiệu lực")
        void suaNhapMatKhauMoiThiMatKhauCuHetHieuLuc() {
            NguoiDung nd = taiKhoan(ID_NHAN_VIEN, "nhanvien01", VaiTro.NHAN_VIEN, true);
            when(nguoiDungRepository.findById(ID_NHAN_VIEN)).thenReturn(Optional.of(nd));
            when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(i -> i.getArgument(0));

            nguoiDungService.luu(formSua(ID_NHAN_VIEN, VaiTro.NHAN_VIEN, MAT_KHAU_MOI));

            NguoiDung daLuu = batBanGhiDaLuu();
            assertThat(passwordEncoder.matches(MAT_KHAU_MOI, daLuu.getMatKhau())).isTrue();
            assertThat(passwordEncoder.matches(MAT_KHAU_CU, daLuu.getMatKhau()))
                    .as("Đặt lại mật khẩu mà mật khẩu cũ vẫn dùng được thì việc đặt lại vô nghĩa")
                    .isFalse();
        }
    }

    // =================================================================
    @Nested
    @DisplayName("Tên đăng nhập")
    class TenDangNhap {

        @Test
        @DisplayName("6. Trùng tên đăng nhập bị chặn ngay ở tầng nghiệp vụ")
        void trungTenDangNhapBiChan() {
            when(nguoiDungRepository.existsByTenDangNhap("admin")).thenReturn(true);

            assertThatThrownBy(() -> nguoiDungService.luu(formThem("admin", MAT_KHAU_MOI)))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("Đã có tài khoản");

            verify(nguoiDungRepository, never()).save(any());
        }

        @Test
        @DisplayName("7. Sửa KHÔNG đổi được tên đăng nhập, dù request có gửi tên khác lên")
        void suaKhongDoiDuocTenDangNhap() {
            NguoiDung nd = taiKhoan(ID_NHAN_VIEN, "nhanvien01", VaiTro.NHAN_VIEN, true);
            when(nguoiDungRepository.findById(ID_NHAN_VIEN)).thenReturn(Optional.of(nd));
            when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(i -> i.getArgument(0));

            // form gửi lên tenDangNhap = "khongdung", tức đúng tình huống người dùng tự chế
            // request để đi vòng qua thuộc tính readonly trên màn hình
            nguoiDungService.luu(formSua(ID_NHAN_VIEN, VaiTro.NHAN_VIEN, ""));

            assertThat(batBanGhiDaLuu().getTenDangNhap()).isEqualTo("nhanvien01");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("⭐ Bất biến: luôn còn ít nhất một quản trị viên đang hoạt động")
    class ConQuanTriVien {

        @Test
        @DisplayName("8. ⭐ Khoá quản trị viên duy nhất còn hoạt động bị chặn")
        void khoaQuanTriVienDuyNhatBiChan() {
            NguoiDung admin = taiKhoan(ID_ADMIN, "admin", VaiTro.ADMIN, true);
            when(nguoiDungRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
            when(nguoiDungRepository.countByVaiTroAndTrangThai(VaiTro.ADMIN, true)).thenReturn(1L);

            assertThatThrownBy(() -> nguoiDungService.khoaTaiKhoan(ID_ADMIN))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("quản trị viên duy nhất");

            verify(nguoiDungRepository, never()).save(any());
        }

        @Test
        @DisplayName("9. ĐỐI CHỨNG — còn hai quản trị viên thì khoá được một người")
        void conHaiQuanTriVienThiKhoaDuoc() {
            NguoiDung admin = taiKhoan(ID_ADMIN, "admin", VaiTro.ADMIN, true);
            when(nguoiDungRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
            when(nguoiDungRepository.countByVaiTroAndTrangThai(VaiTro.ADMIN, true)).thenReturn(2L);

            nguoiDungService.khoaTaiKhoan(ID_ADMIN);

            assertThat(batBanGhiDaLuu().getTrangThai())
                    .as("Không có phép kiểm này thì phép kiểm 8 vẫn xanh ngay cả khi thao tác "
                            + "khoá bị hỏng hoàn toàn và không bao giờ khoá được ai")
                    .isFalse();
        }

        @Test
        @DisplayName("10. ⭐ Hạ quyền quản trị viên duy nhất cũng bị chặn, không chỉ khoá")
        void haQuyenQuanTriVienDuyNhatBiChan() {
            NguoiDung admin = taiKhoan(ID_ADMIN, "admin", VaiTro.ADMIN, true);
            when(nguoiDungRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
            when(nguoiDungRepository.countByVaiTroAndTrangThai(VaiTro.ADMIN, true)).thenReturn(1L);

            // Người đang đăng nhập là một quản trị viên KHÁC, để không vướng luật "tự đổi
            // quyền của chính mình" — thứ đang kiểm ở đây là bất biến còn quản trị viên.
            dangNhapBang(taiKhoan(99L, "adminkhac", VaiTro.ADMIN, true));

            assertThatThrownBy(() ->
                    nguoiDungService.luu(formSua(ID_ADMIN, VaiTro.NHAN_VIEN, "")))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("quản trị viên duy nhất");

            verify(nguoiDungRepository, never()).save(any());
        }
    }

    // =================================================================
    @Nested
    @DisplayName("⭐ Không tự khoá được chính mình")
    class ChinhMinh {

        @Test
        @DisplayName("11. ⭐ Tự khoá tài khoản đang đăng nhập bị chặn")
        void tuKhoaChinhMinhBiChan() {
            NguoiDung admin = taiKhoan(ID_ADMIN, "admin", VaiTro.ADMIN, true);
            when(nguoiDungRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
            dangNhapBang(admin);

            assertThatThrownBy(() -> nguoiDungService.khoaTaiKhoan(ID_ADMIN))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("tự khoá");

            verify(nguoiDungRepository, never()).save(any());
        }

        @Test
        @DisplayName("12. Tự đổi quyền của chính mình bị chặn")
        void tuDoiQuyenChinhMinhBiChan() {
            NguoiDung admin = taiKhoan(ID_ADMIN, "admin", VaiTro.ADMIN, true);
            when(nguoiDungRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
            dangNhapBang(admin);

            assertThatThrownBy(() ->
                    nguoiDungService.luu(formSua(ID_ADMIN, VaiTro.KE_TOAN, "")))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("tự đổi quyền");

            verify(nguoiDungRepository, never()).save(any());
        }

        @Test
        @DisplayName("13. ĐỐI CHỨNG — sửa họ tên của chính mình thì vẫn được")
        void suaHoTenChinhMinhThiDuoc() {
            NguoiDung admin = taiKhoan(ID_ADMIN, "admin", VaiTro.ADMIN, true);
            when(nguoiDungRepository.findById(ID_ADMIN)).thenReturn(Optional.of(admin));
            when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(i -> i.getArgument(0));
            dangNhapBang(admin);

            nguoiDungService.luu(formSua(ID_ADMIN, VaiTro.ADMIN, ""));

            assertThat(batBanGhiDaLuu().getHoTen())
                    .as("Luật chỉ được chặn ĐỔI QUYỀN của chính mình. Chặn luôn cả việc sửa "
                            + "họ tên là chặn quá tay, và không phép kiểm nào ở trên bắt được")
                    .isEqualTo("Tên đã đổi");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("⭐ Khoá tài khoản phải đá được phiên đang mở")
    class DaPhienDangMo {

        @Test
        @DisplayName("14. ⭐ Khoá ai thì phiên của người đó bị đá, phiên người khác không đụng tới")
        void khoaThiDaDungPhienCuaNguoiDo() {
            NguoiDung biKhoa = taiKhoan(ID_NHAN_VIEN, "nhanvien01", VaiTro.NHAN_VIEN, true);
            NguoiDung nguoiKhac = taiKhoan(ID_ADMIN, "admin", VaiTro.ADMIN, true);
            when(nguoiDungRepository.findById(ID_NHAN_VIEN)).thenReturn(Optional.of(biKhoa));

            // Sổ phiên đánh chỉ mục theo ĐỐI TƯỢNG người dùng, và mỗi lần đăng nhập lại sinh
            // một đối tượng mới — nên ở đây cố ý bọc lại thành principal mới, đúng như thật.
            Object principalBiKhoa = new NguoiDungPrincipal(biKhoa);
            Object principalNguoiKhac = new NguoiDungPrincipal(nguoiKhac);
            SessionInformation phienBiKhoa = new SessionInformation(principalBiKhoa, "phien-1",
                    new java.util.Date());
            when(sessionRegistry.getAllPrincipals())
                    .thenReturn(List.of(principalBiKhoa, principalNguoiKhac));
            when(sessionRegistry.getAllSessions(principalBiKhoa, false))
                    .thenReturn(List.of(phienBiKhoa));

            nguoiDungService.khoaTaiKhoan(ID_NHAN_VIEN);

            assertThat(phienBiKhoa.isExpired())
                    .as("Khoá mà không đá phiên đang mở thì người đó vẫn thao tác được tới "
                            + "hết ca — nút Khoá chỉ có tác dụng từ lần đăng nhập sau")
                    .isTrue();
            verify(sessionRegistry, never()).getAllSessions(principalNguoiKhac, false);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("Mở khoá")
    class MoKhoa {

        @Test
        @DisplayName("15. ⭐ Mở khoá gỡ CẢ HAI loại khoá: khoá tay và khoá tạm do nhập sai")
        void moKhoaGoCaHaiLoaiKhoa() {
            NguoiDung nd = taiKhoan(ID_NHAN_VIEN, "nhanvien01", VaiTro.NHAN_VIEN, false);
            nd.setSoLanSai(4);
            nd.setKhoaDenLuc(LocalDateTime.now().plusMinutes(15));
            when(nguoiDungRepository.findById(ID_NHAN_VIEN)).thenReturn(Optional.of(nd));
            when(nguoiDungRepository.save(any(NguoiDung.class))).thenAnswer(i -> i.getArgument(0));

            nguoiDungService.moKhoaTaiKhoan(ID_NHAN_VIEN);

            NguoiDung daLuu = batBanGhiDaLuu();
            assertThat(daLuu.getTrangThai()).isTrue();
            assertThat(daLuu.getKhoaDenLuc())
                    .as("Gỡ khoá tay mà để nguyên khoá tạm thì người dùng vẫn không vào được, "
                            + "và quản trị viên tưởng mình đã mở xong")
                    .isNull();
            assertThat(daLuu.getSoLanSai()).isZero();
        }

        @Test
        @DisplayName("16. Mở khoá một tài khoản đang bình thường thì báo lỗi, không lặng lẽ ghi")
        void moKhoaTaiKhoanBinhThuongThiBaoLoi() {
            NguoiDung nd = taiKhoan(ID_NHAN_VIEN, "nhanvien01", VaiTro.NHAN_VIEN, true);
            when(nguoiDungRepository.findById(ID_NHAN_VIEN)).thenReturn(Optional.of(nd));

            assertThatThrownBy(() -> nguoiDungService.moKhoaTaiKhoan(ID_NHAN_VIEN))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("không có khoá nào để mở");

            verify(nguoiDungRepository, never()).save(any());
        }
    }
}
