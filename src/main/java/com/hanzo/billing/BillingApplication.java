package com.hanzo.billing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lớp khởi động của hệ thống "Quản lý thuê bao và tính cước điện thoại".
 *
 * <p>Đồ án môn Thực tập nghề nghiệp. Toàn bộ dữ liệu trong hệ thống là
 * dữ liệu mẫu tự sinh, phục vụ mục đích học tập.</p>
 */
@SpringBootApplication
public class BillingApplication {

    public static void main(String[] args) {
        SpringApplication.run(BillingApplication.class, args);
    }
}
