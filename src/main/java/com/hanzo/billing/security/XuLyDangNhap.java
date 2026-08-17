package com.hanzo.billing.security;

import com.hanzo.billing.entity.NguoiDung;
import com.hanzo.billing.repository.NguoiDungRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Đếm số lần đăng nhập sai và khoá tạm tài khoản khi vượt ngưỡng.
 *
 * <h2>Vì sao cần</h2>
 * <p>Trước đợt này, nhập sai mật khẩu bao nhiêu lần cũng được, với tốc độ tối đa, và hệ thống
 * không chậm lại cũng không để lại dấu vết nào. Ba tài khoản demo lại dùng chung một mật khẩu
 * ngắn — nên đây không phải rủi ro lý thuyết.</p>
 *
 * <h2>Ba quyết định</h2>
 *
 * <p><b>1. Khoá TẠM theo thời gian, không khoá vĩnh viễn.</b> Khoá vĩnh viễn biến một lần gõ
 * nhầm thành việc phải đi tìm quản trị viên. Tệ hơn: nó biến chính cơ chế bảo vệ thành một
 * cách <i>từ chối dịch vụ</i> — kẻ xấu chỉ cần cố tình nhập sai {@value #SO_LAN_TOI_DA} lần
 * vào tài khoản người khác là khoá được họ vô thời hạn.</p>
 *
 * <p><b>2. Thông báo cho người dùng KHÔNG nói tài khoản có tồn tại hay không.</b> Câu "tên đăng
 * nhập hoặc mật khẩu không đúng" giữ nguyên cho mọi trường hợp sai. Nói rõ "tài khoản không
 * tồn tại" là tặng kẻ dò một công cụ liệt kê tài khoản có thật.</p>
 *
 * <p>Riêng trường hợp <b>đang bị khoá</b> thì có nói ra, kèm số phút còn lại — vì người bị khoá
 * gần như luôn là người dùng thật đang bối rối, và giấu đi chỉ khiến họ thử lại liên tục làm
 * thời gian khoá dài thêm.</p>
 *
 * <p><b>3. Đăng nhập đúng thì XOÁ SẠCH bộ đếm.</b> Đếm số lần sai <i>liên tiếp</i>, không phải
 * tổng số lần sai trong đời tài khoản. Không xoá thì một tài khoản dùng lâu năm sẽ tự khoá
 * mình vào một ngày đẹp trời.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class XuLyDangNhap implements AuthenticationFailureHandler, AuthenticationSuccessHandler {

    /** Số lần nhập sai liên tiếp trước khi khoá tạm. */
    public static final int SO_LAN_TOI_DA = 5;

    /** Thời gian khoá tạm, tính bằng phút. */
    public static final int SO_PHUT_KHOA = 15;

    private final NguoiDungRepository nguoiDungRepository;

    // =================================================================
    // ĐĂNG NHẬP SAI
    // =================================================================
    @Override
    @Transactional
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException ex) throws IOException {
        String tenDangNhap = request.getParameter("tenDangNhap");
        String duongDan = "/dang-nhap?loi";

        Optional<NguoiDung> tim = (tenDangNhap == null || tenDangNhap.isBlank())
                ? Optional.empty()
                : nguoiDungRepository.findByTenDangNhap(tenDangNhap);

        if (tim.isPresent()) {
            NguoiDung nd = tim.get();

            if (nd.dangBiKhoa()) {
                // Đang trong thời gian khoá: KHÔNG cộng thêm lần sai nữa. Cộng tiếp thì
                // mỗi lần người dùng sốt ruột thử lại là một lần gia hạn khoá cho chính họ.
                duongDan = "/dang-nhap?khoa=" + soPhutConLai(nd);
            } else {
                int soLanSai = khongNull(nd.getSoLanSai()) + 1;
                nd.setSoLanSai(soLanSai);

                if (soLanSai >= SO_LAN_TOI_DA) {
                    nd.setKhoaDenLuc(LocalDateTime.now().plusMinutes(SO_PHUT_KHOA));
                    nd.setSoLanSai(0);
                    log.warn("Khoá tạm tài khoản '{}' trong {} phút sau {} lần nhập sai liên tiếp",
                            tenDangNhap, SO_PHUT_KHOA, SO_LAN_TOI_DA);
                    duongDan = "/dang-nhap?khoa=" + SO_PHUT_KHOA;
                } else {
                    log.warn("Đăng nhập sai lần {}/{} cho tài khoản '{}'",
                            soLanSai, SO_LAN_TOI_DA, tenDangNhap);
                }
                nguoiDungRepository.save(nd);
            }
        } else {
            // Tên đăng nhập không tồn tại: KHÔNG nói ra điều đó. Ghi log để quản trị viên
            // thấy nếu có ai đang quét, nhưng người dùng chỉ nhận đúng một câu như mọi
            // trường hợp sai khác.
            log.warn("Đăng nhập thất bại cho tên đăng nhập không tồn tại: '{}'", tenDangNhap);
        }

        response.sendRedirect(request.getContextPath() + duongDan);
    }

    // =================================================================
    // ĐĂNG NHẬP ĐÚNG
    // =================================================================
    @Override
    @Transactional
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication auth) throws IOException {
        nguoiDungRepository.findByTenDangNhap(auth.getName()).ifPresent(nd -> {
            // Chỉ ghi khi thật sự có gì để xoá — tránh một câu UPDATE thừa ở mỗi lần đăng nhập.
            if (khongNull(nd.getSoLanSai()) != 0 || nd.getKhoaDenLuc() != null) {
                nd.setSoLanSai(0);
                nd.setKhoaDenLuc(null);
                nguoiDungRepository.save(nd);
            }
        });
        response.sendRedirect(request.getContextPath() + "/");
    }

    private static long soPhutConLai(NguoiDung nd) {
        long phut = java.time.Duration.between(LocalDateTime.now(), nd.getKhoaDenLuc()).toMinutes();
        return Math.max(1, phut);
    }

    private static int khongNull(Integer so) {
        return so == null ? 0 : so;
    }
}
