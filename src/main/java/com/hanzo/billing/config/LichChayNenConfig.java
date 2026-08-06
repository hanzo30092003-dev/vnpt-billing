package com.hanzo.billing.config;

import com.hanzo.billing.service.HoaDonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Các việc chạy nền theo lịch.
 *
 * <p>Hiện chỉ có một việc: chuyển hóa đơn quá hạn. Đặt riêng một lớp thay vì gắn
 * {@code @Scheduled} thẳng vào service để tầng nghiệp vụ không phụ thuộc vào việc nó được
 * gọi theo lịch hay do người dùng bấm — {@code capNhatQuaHan()} vẫn gọi tay được và vẫn kiểm
 * thử được mà không cần bật scheduler.</p>
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class LichChayNenConfig {

    private final HoaDonService hoaDonService;

    /**
     * Chạy 00:05 mỗi ngày — ngay sau khi sang ngày mới, vì "quá hạn" đổi theo ngày.
     *
     * <p>Cùng một việc còn được gọi khi vào trang danh sách hóa đơn. Hai đường gọi là cố ý:
     * lịch chạy nền lo trường hợp không ai mở màn hình suốt ngày, còn lần gọi lúc mở màn hình
     * lo trường hợp ứng dụng vừa khởi động lại và đã lỡ mất giờ hẹn. Hàm này chỉ đổi trạng
     * thái của hóa đơn <b>thực sự</b> quá hạn nên chạy nhiều lần không sinh tác dụng phụ.</p>
     */
    @Scheduled(cron = "0 5 0 * * *")
    public void chuyenHoaDonQuaHan() {
        int so = hoaDonService.capNhatQuaHan();
        if (so > 0) {
            log.info("Lịch chạy nền: đã chuyển {} hóa đơn sang trạng thái Quá hạn", so);
        }
    }
}
