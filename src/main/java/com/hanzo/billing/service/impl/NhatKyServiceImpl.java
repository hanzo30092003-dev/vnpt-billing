package com.hanzo.billing.service.impl;

import com.hanzo.billing.entity.NhatKyHeThong;
import com.hanzo.billing.repository.NguoiDungRepository;
import com.hanzo.billing.repository.NhatKyHeThongRepository;
import com.hanzo.billing.service.NhatKyService;
import com.hanzo.billing.util.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NhatKyServiceImpl implements NhatKyService {

    private final NhatKyHeThongRepository nhatKyHeThongRepository;
    private final NguoiDungRepository nguoiDungRepository;

    /**
     * Dùng {@code REQUIRED} để dòng nhật ký nằm chung giao dịch với thao tác nghiệp vụ:
     * nếu nghiệp vụ bị rollback thì nhật ký cũng không được ghi, tránh tình trạng
     * nhật ký báo đã làm nhưng dữ liệu thực tế không đổi.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void ghiNhatKy(String hanhDong, String doiTuong, Long doiTuongId, String noiDung) {
        NhatKyHeThong nhatKy = new NhatKyHeThong();
        nhatKy.setHanhDong(hanhDong);
        nhatKy.setDoiTuong(doiTuong);
        nhatKy.setDoiTuongId(doiTuongId);
        nhatKy.setNoiDung(noiDung);
        nhatKy.setDiaChiIp(layDiaChiIp());
        nhatKy.setThoiGian(LocalDateTime.now());

        SecurityUtils.layNguoiDungHienTai().ifPresent(principal ->
                nhatKy.setNguoiDung(nguoiDungRepository.getReferenceById(principal.getId())));

        nhatKyHeThongRepository.save(nhatKy);
    }

    /**
     * Lấy IP người gọi — <b>chỉ</b> từ kết nối thật, không tin header nào.
     *
     * <h2>Vì sao bỏ {@code X-Forwarded-For}</h2>
     *
     * <p>Bản trước ưu tiên header {@code X-Forwarded-For} "để vẫn đúng khi chạy sau proxy".
     * Nhưng hệ thống này chạy trực tiếp, <b>không có proxy nào ở trước</b> — nên header đó
     * không do hạ tầng đặt, mà do <b>chính người gửi request tự khai</b>. Hai hậu quả, cả hai
     * đều đã dựng lại được:</p>
     *
     * <p><b>1. Phá được mọi đường ghi của hệ thống.</b> Cột {@code dia_chi_ip} chỉ dài 45 ký
     * tự, và {@code ghiNhatKy} chạy <b>chung giao dịch</b> với nghiệp vụ. Gửi một header dài
     * 48 ký tự kèm bất kỳ thao tác nào là MySQL từ chối câu INSERT nhật ký, cả giao dịch bị
     * cuộn lại, và <b>việc nghiệp vụ không được lưu</b>. Đã thử: thêm khách hàng kèm
     * {@code X-Forwarded-For: AAAA…(48)} → HTTP 500, số khách hàng giữ nguyên 50. Lặp lại được
     * vô hạn, trên cả 26 chỗ gọi {@code ghiNhatKy}.</p>
     *
     * <p><b>2. Giả mạo được chính sổ nhật ký.</b> Người thao tác tự chọn được IP mà nhật ký ghi
     * lại cho mình — kể cả đặt thành IP của người khác. Một cột ghi vết mà đối tượng bị ghi vết
     * tự khai thì không dùng để truy trách nhiệm được.</p>
     *
     * <p>Muốn dùng lại header này khi thật sự đặt sau proxy thì phải: chỉ tin khi
     * {@code getRemoteAddr()} nằm trong danh sách proxy tin cậy, kiểm định dạng IP, rồi mới cắt
     * độ dài. Chưa có proxy thì chưa cần, và cái chưa cần mà vẫn để thì chỉ còn là lỗ hổng.</p>
     */
    private String layDiaChiIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        String ip = attrs.getRequest().getRemoteAddr();
        // Cắt phòng xa: IPv6 dạng dài nhất vẫn dưới 45 ký tự, nhưng một cột ghi vết
        // không bao giờ được phép làm hỏng nghiệp vụ mà nó chỉ đi kèm.
        if (ip != null && ip.length() > DO_DAI_IP_TOI_DA) {
            ip = ip.substring(0, DO_DAI_IP_TOI_DA);
        }
        return ip;
    }

    /** Bằng đúng {@code dia_chi_ip VARCHAR(45)} trong {@code schema.sql}. */
    private static final int DO_DAI_IP_TOI_DA = 45;
}
