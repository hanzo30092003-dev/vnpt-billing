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
     * Lấy IP người gọi. Ưu tiên header {@code X-Forwarded-For} để vẫn đúng khi hệ
     * thống chạy sau proxy hoặc bộ cân bằng tải; nếu không có thì lấy IP kết nối trực tiếp.
     */
    private String layDiaChiIp() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) {
            return null;
        }
        HttpServletRequest request = attrs.getRequest();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
