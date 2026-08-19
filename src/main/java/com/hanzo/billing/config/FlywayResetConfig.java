package com.hanzo.billing.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Ba bước của profile {@code reset}: xoá sạch → dựng lại cấu trúc → nạp dữ liệu mẫu.
 *
 * <h2>Vì sao dữ liệu mẫu KHÔNG nằm trong thư mục di trú</h2>
 *
 * <p>Đặt {@code data-mau.sql} và {@code data-van-hanh.sql} thành hai file di trú là cách làm
 * ngắn nhất, và nó sai với dự án này. {@code data-van-hanh.sql} là một <b>bản dump được sinh
 * lại</b> mỗi khi trạng thái vận hành đổi (Phase 5 mục F đã dump lại một lần). Flyway lưu
 * checksum của từng file di trú đã chạy — dump lại là đổi checksum, và <b>mọi CSDL đang chạy
 * sẽ từ chối khởi động</b> với "Migration checksum mismatch". Tức là chọn cách ngắn thì đổi
 * lấy việc mất khả năng dump lại — thứ mà quy trình của dự án dựa vào.</p>
 *
 * <p>Nên phân vai rạch ròi: <b>Flyway giữ CẤU TRÚC</b> (thay đổi tích luỹ, không bao giờ sửa
 * lại file cũ), còn <b>dữ liệu mẫu nạp bằng đường riêng</b> ở đây, chỉ trong profile
 * {@code reset}. Chạy thường không đụng tới dữ liệu.</p>
 *
 * <h2>Vì sao nạp bằng mã thay vì {@code spring.sql.init}</h2>
 *
 * <p>Tài liệu Spring Boot khuyến cáo <i>không</i> dùng {@code spring.sql.init} chung với
 * Flyway và nói rõ sẽ bỏ hỗ trợ. Quan trọng hơn: dùng chung thì thứ tự giữa hai cơ chế là
 * thứ mình phải tra tài liệu để đoán, mà thứ tự ở đây <b>quyết định chạy được hay không</b> —
 * nạp dữ liệu trước khi có bảng thì hỏng. Gọi tường minh trong một hàm thì thứ tự đọc thẳng
 * từ mã, không phải đoán.</p>
 *
 * <p>{@link ScriptUtils} chính là thứ {@code spring.sql.init} dùng bên dưới, nên cách tách
 * câu lệnh và cách đọc chú thích không đổi so với trước.</p>
 */
@Configuration
@Profile("reset")
@Slf4j
public class FlywayResetConfig {

    /** Đúng thứ tự này: {@code data-van-hanh.sql} tham chiếu khoá ngoại vào {@code data-mau.sql}. */
    private static final String[] FILE_DU_LIEU = {
            "db/data-mau.sql",
            "db/data-van-hanh.sql"
    };

    @Bean
    public FlywayMigrationStrategy chienLuocReset(DataSource dataSource) {
        return flyway -> {
            log.warn("Profile 'reset': XOÁ SẠCH cơ sở dữ liệu rồi dựng lại từ đầu");
            flyway.clean();
            flyway.migrate();
            napDuLieuMau(dataSource);
        };
    }

    private void napDuLieuMau(DataSource dataSource) {
        try (Connection ketNoi = dataSource.getConnection()) {
            for (String duongDan : FILE_DU_LIEU) {
                ScriptUtils.executeSqlScript(ketNoi,
                        new EncodedResource(new ClassPathResource(duongDan), "UTF-8"));
                log.info("Đã nạp dữ liệu mẫu từ {}", duongDan);
            }
        } catch (SQLException ex) {
            // Ném lại thành lỗi khởi động: một lần reset nạp thiếu dữ liệu mà vẫn để ứng dụng
            // chạy tiếp là tình huống tệ nhất — mọi con số sau đó đều sai mà không ai biết.
            throw new IllegalStateException("Không nạp được dữ liệu mẫu ở profile reset", ex);
        }
    }
}
