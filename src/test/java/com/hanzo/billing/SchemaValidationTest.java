package com.hanzo.billing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Kiểm tra toàn bộ Entity JPA có khớp với cấu trúc bảng thật trong CSDL hay không.
 *
 * <p><b>Vì sao cần test này:</b> cấu hình chạy thật đặt
 * {@code spring.jpa.hibernate.ddl-auto=none}, nghĩa là Hibernate <i>không</i> đối
 * chiếu Entity với CSDL lúc khởi động. Một cột khai sai tên hoặc sai kiểu sẽ không
 * gây lỗi ngay, mà nằm im cho tới khi có truy vấn chạm vào nó — lúc đó lỗi phát sinh
 * ở tầng nghiệp vụ và rất khó truy nguyên.</p>
 *
 * <p>Test này bật {@code ddl-auto=validate} để Hibernate so từng cột của 15 Entity
 * với schema thật. Nếu lệch, Spring context không khởi động được và test đỏ ngay
 * ở khâu build. Đây chính là cách đã phát hiện lỗi 5 cột {@code TINYINT} bị khai
 * thành {@code Boolean} không kèm {@code @JdbcTypeCode} ở Phase 1.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306} và CSDL
 * {@code vnpt_billing} đã có đủ bảng. Từ việc V4, cấu trúc bảng do Flyway dựng từ
 * {@code db/migration/V*.sql}; test này vì vậy còn kiểm thêm một điều nữa: <b>các file di
 * trú cộng lại có ra đúng cấu trúc mà 15 Entity mong đợi hay không</b>. Tách một cột sang
 * file di trú mới mà quên kiểu dữ liệu là test này đỏ ngay.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class SchemaValidationTest {

    /**
     * Không cần assert gì thêm: chỉ riêng việc Spring context khởi động được với
     * {@code ddl-auto=validate} đã chứng minh cả 15 Entity ánh xạ đúng.
     */
    @Test
    @DisplayName("Toàn bộ Entity ánh xạ khớp với schema CSDL thật")
    void contextLoads() {
    }
}
