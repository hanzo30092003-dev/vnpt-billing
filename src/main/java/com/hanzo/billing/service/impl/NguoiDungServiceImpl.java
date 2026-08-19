package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.DoiMatKhauForm;
import com.hanzo.billing.dto.NguoiDungForm;
import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.enums.VaiTro;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.NguoiDungRepository;
import com.hanzo.billing.security.NguoiDungPrincipal;
import com.hanzo.billing.service.NguoiDungService;
import com.hanzo.billing.service.NhatKyService;
import com.hanzo.billing.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NguoiDungServiceImpl implements NguoiDungService {

    /** Ngắn hơn thì mật khẩu đoán được trong vài giây, dài hơn thì người dùng dán giấy lên màn hình. */
    private static final int DO_DAI_MAT_KHAU_TOI_THIEU = 6;

    private final NguoiDungRepository nguoiDungRepository;
    private final PasswordEncoder passwordEncoder;
    private final NhatKyService nhatKyService;
    private final SessionRegistry sessionRegistry;

    @Override
    @Transactional(readOnly = true)
    public List<NguoiDung> danhSach() {
        return nguoiDungRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Override
    @Transactional(readOnly = true)
    public NguoiDung layTheoId(Long id) {
        return nguoiDungRepository.findById(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy tài khoản có mã số " + id));
    }

    @Override
    @Transactional
    public NguoiDung luu(NguoiDungForm form) {
        boolean themMoi = (form.getId() == null);
        NguoiDung nguoiDung = themMoi ? new NguoiDung() : layTheoId(form.getId());

        if (themMoi) {
            String ten = form.getTenDangNhap().trim();
            if (nguoiDungRepository.existsByTenDangNhap(ten)) {
                throw new NghiepVuException("Đã có tài khoản tên đăng nhập " + ten
                        + ". Hãy chọn tên khác, hoặc mở tài khoản đang có trong danh sách "
                        + "để sửa thông tin của nó.");
            }
            nguoiDung.setTenDangNhap(ten);
            nguoiDung.setTrangThai(true);
            nguoiDung.setSoLanSai(0);
            nguoiDung.setNgayTao(LocalDateTime.now());
        } else {
            kiemTraKhongTuDoiQuyenCuaMinh(nguoiDung, form.getVaiTro());
            kiemTraConQuanTriVien(nguoiDung, form.getVaiTro());
        }

        nguoiDung.setHoTen(form.getHoTen().trim());
        nguoiDung.setEmail(rongThanhNull(form.getEmail()));
        nguoiDung.setVaiTro(form.getVaiTro());

        boolean doiMatKhau = datMatKhauNeuCo(nguoiDung, form.getMatKhau(), themMoi);

        NguoiDung daLuu = nguoiDungRepository.save(nguoiDung);

        // Quản trị viên đặt lại mật khẩu cho ai thì phiên đang mở của người đó hết giá trị.
        // Lý do đặt lại mật khẩu thường là "tài khoản có thể đã lộ" — mà để nguyên phiên cũ
        // thì kẻ đang chiếm tài khoản vẫn ngồi trong hệ thống, chỉ là không đăng nhập lại được.
        int soPhien = (doiMatKhau && !themMoi) ? daPhienDangMo(daLuu.getId()) : 0;

        nhatKyService.ghiNhatKy(
                themMoi ? "TAO_NGUOI_DUNG" : "SUA_NGUOI_DUNG",
                "NGUOI_DUNG",
                daLuu.getId(),
                (themMoi ? "Tạo tài khoản " : "Cập nhật tài khoản ")
                        + daLuu.getTenDangNhap() + " - " + daLuu.getHoTen()
                        + ", quyền " + daLuu.getVaiTro().getNhan()
                        + (doiMatKhau && !themMoi ? ", có đặt lại mật khẩu" : "")
                        + (soPhien > 0 ? ", buộc thoát " + soPhien + " phiên đang mở" : ""));

        return daLuu;
    }

    /**
     * {@inheritDoc}
     *
     * <p><b>Thứ tự bốn phép kiểm là cố ý.</b> Mật khẩu hiện tại kiểm <b>trước tiên</b>: chưa
     * chứng minh được là chủ tài khoản thì mọi lời phàn nàn về độ dài hay về hai ô không khớp
     * đều là thông tin cho không. Ba phép kiểm sau chỉ là giúp người dùng khỏi tự khoá mình.</p>
     */
    @Override
    @Transactional
    public void doiMatKhau(DoiMatKhauForm form) {
        NguoiDung nguoiDung = SecurityUtils.layNguoiDungHienTai()
                .map(p -> layTheoId(p.getId()))
                .orElseThrow(() -> new NghiepVuException(
                        "Phiên làm việc đã kết thúc. Hãy đăng nhập lại rồi đổi mật khẩu."));

        if (!passwordEncoder.matches(khongNull(form.getMatKhauCu()), nguoiDung.getMatKhau())) {
            throw new NghiepVuException("Mật khẩu hiện tại không đúng. "
                    + "Hãy gõ lại đúng mật khẩu bạn vừa dùng để đăng nhập.");
        }

        String matKhauMoi = khongNull(form.getMatKhauMoi()).trim();

        if (!matKhauMoi.equals(khongNull(form.getXacNhanMatKhauMoi()).trim())) {
            throw new NghiepVuException("Hai ô mật khẩu mới không giống nhau. "
                    + "Hãy gõ lại cả hai ô cho khớp.");
        }
        if (matKhauMoi.length() < DO_DAI_MAT_KHAU_TOI_THIEU) {
            throw new NghiepVuException("Mật khẩu mới phải dài ít nhất "
                    + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự.");
        }
        if (passwordEncoder.matches(matKhauMoi, nguoiDung.getMatKhau())) {
            throw new NghiepVuException("Mật khẩu mới trùng với mật khẩu đang dùng. "
                    + "Hãy chọn một mật khẩu khác.");
        }

        nguoiDung.setMatKhau(passwordEncoder.encode(matKhauMoi));
        nguoiDungRepository.save(nguoiDung);

        daPhienDangMo(nguoiDung.getId());

        nhatKyService.ghiNhatKy("DOI_MAT_KHAU", "NGUOI_DUNG", nguoiDung.getId(),
                "Tự đổi mật khẩu tài khoản " + nguoiDung.getTenDangNhap()
                        + " - " + nguoiDung.getHoTen());
    }

    private static String khongNull(String chuoi) {
        return chuoi == null ? "" : chuoi;
    }

    @Override
    @Transactional
    public void khoaTaiKhoan(Long id) {
        NguoiDung nguoiDung = layTheoId(id);

        // Tự khoá chính mình là thao tác không có đường lùi: khoá xong là mất luôn màn hình
        // vừa dùng để khoá. Chặn ở đây rẻ hơn nhiều so với đi mở lại bằng câu SQL tay.
        if (laChinhMinh(nguoiDung)) {
            throw new NghiepVuException("Không thể tự khoá tài khoản bạn đang đăng nhập. "
                    + "Nếu muốn khoá tài khoản này, hãy nhờ một quản trị viên khác làm.");
        }

        if (!nguoiDung.dangHoatDong()) {
            throw new NghiepVuException("Tài khoản " + nguoiDung.getTenDangNhap()
                    + " đã bị khoá từ trước, không cần làm lại thao tác này.");
        }

        kiemTraConQuanTriVien(nguoiDung, null);

        nguoiDung.setTrangThai(false);
        nguoiDungRepository.save(nguoiDung);

        int soPhien = daPhienDangMo(nguoiDung.getId());

        nhatKyService.ghiNhatKy("KHOA_NGUOI_DUNG", "NGUOI_DUNG", nguoiDung.getId(),
                "Khoá tài khoản " + nguoiDung.getTenDangNhap() + " - " + nguoiDung.getHoTen()
                        + (soPhien > 0 ? ", buộc thoát " + soPhien + " phiên đang mở" : ""));
    }

    @Override
    @Transactional
    public void moKhoaTaiKhoan(Long id) {
        NguoiDung nguoiDung = layTheoId(id);

        boolean dangKhoaTay = !nguoiDung.dangHoatDong();
        boolean dangKhoaTam = nguoiDung.dangBiKhoa();
        if (!dangKhoaTay && !dangKhoaTam) {
            throw new NghiepVuException("Tài khoản " + nguoiDung.getTenDangNhap()
                    + " đang dùng bình thường, không có khoá nào để mở.");
        }

        nguoiDung.setTrangThai(true);
        nguoiDung.setSoLanSai(0);
        nguoiDung.setKhoaDenLuc(null);
        nguoiDungRepository.save(nguoiDung);

        nhatKyService.ghiNhatKy("MO_KHOA_NGUOI_DUNG", "NGUOI_DUNG", nguoiDung.getId(),
                "Mở khoá tài khoản " + nguoiDung.getTenDangNhap() + " - " + nguoiDung.getHoTen()
                        + (dangKhoaTay ? ", gỡ khoá của quản trị viên" : "")
                        + (dangKhoaTam ? ", gỡ khoá tạm do nhập sai mật khẩu" : ""));
    }

    // =================================================================
    // CÁC PHÉP KIỂM DÙNG CHUNG
    // =================================================================

    /**
     * Hệ thống phải luôn còn <b>ít nhất một quản trị viên đang hoạt động</b>.
     *
     * <p>Đây là bất biến duy nhất của phân hệ này mà vi phạm thì <b>không sửa được bằng chính
     * phần mềm</b>: khoá nốt quản trị viên cuối cùng — hoặc hạ quyền người đó xuống nhân viên
     * — là không còn ai mở được màn hình quản lý người dùng để sửa lại, kể cả người vừa gây
     * ra. Đường ra duy nhất khi đó là mở CSDL lên sửa bằng tay.</p>
     *
     * @param vaiTroMoi vai trò sắp đặt cho tài khoản, hoặc {@code null} nếu đang khoá tài khoản
     */
    private void kiemTraConQuanTriVien(NguoiDung nguoiDung, VaiTro vaiTroMoi) {
        boolean dangLaQuanTri = nguoiDung.getVaiTro() == VaiTro.ADMIN
                && nguoiDung.dangHoatDong();
        boolean thoiLamQuanTri = (vaiTroMoi == null) || (vaiTroMoi != VaiTro.ADMIN);
        if (!dangLaQuanTri || !thoiLamQuanTri) {
            return;
        }

        long conLai = nguoiDungRepository.countByVaiTroAndTrangThai(VaiTro.ADMIN, true);
        if (conLai <= 1) {
            throw new NghiepVuException("Đây là quản trị viên duy nhất còn hoạt động. "
                    + "Khoá hoặc hạ quyền tài khoản này thì sẽ không còn ai vào được màn hình "
                    + "quản lý người dùng nữa. Hãy tạo thêm một quản trị viên trước.");
        }
    }

    /**
     * Không cho tự đổi quyền của chính mình.
     *
     * <p>Hạ quyền chính mình thì ngay yêu cầu kế tiếp đã là 403 — người dùng thấy phần mềm
     * "hỏng" chứ không hiểu là mình vừa tự làm. Nhờ người khác đổi thì việc đó có người thứ
     * hai biết, đúng tinh thần của một thao tác phân quyền.</p>
     */
    private void kiemTraKhongTuDoiQuyenCuaMinh(NguoiDung nguoiDung, VaiTro vaiTroMoi) {
        if (laChinhMinh(nguoiDung) && nguoiDung.getVaiTro() != vaiTroMoi) {
            throw new NghiepVuException("Không thể tự đổi quyền của tài khoản bạn đang đăng "
                    + "nhập. Hãy nhờ một quản trị viên khác đổi giúp.");
        }
    }

    private boolean laChinhMinh(NguoiDung nguoiDung) {
        return SecurityUtils.layNguoiDungHienTai()
                .map(p -> p.getId().equals(nguoiDung.getId()))
                .orElse(false);
    }

    /**
     * Băm và đặt mật khẩu mới nếu người dùng có nhập.
     *
     * @return {@code true} nếu mật khẩu thật sự được đổi
     */
    private boolean datMatKhauNeuCo(NguoiDung nguoiDung, String matKhauTho, boolean themMoi) {
        String matKhau = matKhauTho == null ? "" : matKhauTho.trim();

        if (matKhau.isEmpty()) {
            if (themMoi) {
                throw new NghiepVuException("Vui lòng đặt mật khẩu đăng nhập cho tài khoản mới.");
            }
            return false;   // sửa mà bỏ trống ô mật khẩu = giữ nguyên mật khẩu cũ
        }

        if (matKhau.length() < DO_DAI_MAT_KHAU_TOI_THIEU) {
            throw new NghiepVuException("Mật khẩu phải dài ít nhất "
                    + DO_DAI_MAT_KHAU_TOI_THIEU + " ký tự.");
        }

        nguoiDung.setMatKhau(passwordEncoder.encode(matKhau));
        return true;
    }

    /**
     * Đá mọi phiên đang mở của một tài khoản.
     *
     * <p>Đánh dấu phiên là hết hạn chứ không xoá phiên: yêu cầu kế tiếp của người đó đi qua
     * {@code ConcurrentSessionFilter}, filter thấy dấu hết hạn và đưa họ về trang đăng nhập.
     * Cách này không đụng tới bộ chứa servlet và chạy được cả khi phiên nằm ở máy khác.</p>
     *
     * <p>Sổ phiên đánh chỉ mục theo <i>đối tượng</i> người dùng, mà mỗi lần đăng nhập lại sinh
     * một đối tượng mới — nên phải so theo <b>mã số tài khoản</b>, không so bằng
     * {@code equals}. So bằng {@code equals} thì hàm này im lặng không đá được phiên nào, và
     * nút "Khoá" trông vẫn như đang chạy.</p>
     *
     * @return số phiên đã bị đá
     */
    private int daPhienDangMo(Long nguoiDungId) {
        int soPhien = 0;
        for (Object principal : sessionRegistry.getAllPrincipals()) {
            if (!(principal instanceof NguoiDungPrincipal p) || !nguoiDungId.equals(p.getId())) {
                continue;
            }
            for (SessionInformation phien : sessionRegistry.getAllSessions(principal, false)) {
                phien.expireNow();
                soPhien++;
            }
        }
        return soPhien;
    }

    /** Chuỗi rỗng từ form được quy về null để CSDL không lưu chuỗi trống vô nghĩa. */
    private String rongThanhNull(String giaTri) {
        if (giaTri == null) {
            return null;
        }
        String daCat = giaTri.trim();
        return daCat.isEmpty() ? null : daCat;
    }
}
