package com.hanzo.billing.service;

import com.hanzo.billing.repository.BangGiaCuocRepository;
import com.hanzo.billing.repository.GoiCuocRepository;
import com.hanzo.billing.service.impl.BangGiaCuocServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử hàm phát hiện chồng khoảng hiệu lực của bảng giá cước.
 *
 * <p>Hai dòng bảng giá cùng bộ khoá (gói cước, loại dịch vụ, hướng, giờ cao điểm)
 * mà khoảng hiệu lực đè lên nhau sẽ khiến engine tính cước ở Phase 4 gặp hai đơn giá
 * cùng áp dụng cho một thời điểm và không biết chọn dòng nào. Vì vậy quy tắc này
 * được kiểm thử riêng, đầy đủ các dạng chồng.</p>
 *
 * <p>Khoảng được hiểu là ĐÓNG hai đầu; ngày kết thúc {@code null} nghĩa là vô thời hạn.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Kiểm tra chồng khoảng hiệu lực bảng giá")
class BangGiaCuocServiceTest {

    @Mock private BangGiaCuocRepository bangGiaCuocRepository;
    @Mock private GoiCuocRepository goiCuocRepository;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks
    private BangGiaCuocServiceImpl service;

    private static LocalDate ngay(String iso) {
        return LocalDate.parse(iso);
    }

    @Test
    @DisplayName("1. Trùng khít: hai khoảng giống hệt nhau thì chồng")
    void trungKhit() {
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-01-01"), ngay("2026-01-31"),
                ngay("2026-01-01"), ngay("2026-01-31"))).isTrue();
    }

    @Test
    @DisplayName("2. Chồng một phần đầu: khoảng mới bắt đầu giữa khoảng cũ")
    void chongMotPhanDau() {
        // Cũ:  01/01 ---------- 31/01
        // Mới:        15/01 ---------- 15/02
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-01-15"), ngay("2026-02-15"),
                ngay("2026-01-01"), ngay("2026-01-31"))).isTrue();
    }

    @Test
    @DisplayName("3. Chồng một phần cuối: khoảng mới kết thúc giữa khoảng cũ")
    void chongMotPhanCuoi() {
        // Mới: 01/01 ---------- 20/01
        // Cũ:         15/01 ---------- 15/02
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-01-01"), ngay("2026-01-20"),
                ngay("2026-01-15"), ngay("2026-02-15"))).isTrue();
    }

    @Test
    @DisplayName("4. Lồng nhau: khoảng nhỏ nằm trọn trong khoảng lớn")
    void longNhau() {
        // Lớn: 01/01 -------------------- 31/12
        // Nhỏ:            01/06 -- 30/06
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-06-01"), ngay("2026-06-30"),
                ngay("2026-01-01"), ngay("2026-12-31"))).isTrue();

        // Đảo thứ tự tham số vẫn phải cho cùng kết quả
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-01-01"), ngay("2026-12-31"),
                ngay("2026-06-01"), ngay("2026-06-30"))).isTrue();
    }

    @Test
    @DisplayName("5. Kề nhau nhưng KHÔNG chồng: khoảng sau bắt đầu ngay hôm sau ngày kết thúc của khoảng trước")
    void keNhauKhongChong() {
        // 01/01 -- 31/01  |  01/02 -- 28/02
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-01-01"), ngay("2026-01-31"),
                ngay("2026-02-01"), ngay("2026-02-28"))).isFalse();
    }

    @Test
    @DisplayName("6. Chạm nhau đúng 1 ngày thì VẪN chồng, vì khoảng đóng hai đầu")
    void chamNhauMotNgay_thiVanChong() {
        // 01/01 -- 31/01  và  31/01 -- 28/02  cùng chứa ngày 31/01
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-01-01"), ngay("2026-01-31"),
                ngay("2026-01-31"), ngay("2026-02-28"))).isTrue();
    }

    @Test
    @DisplayName("7. Một bên vô thời hạn và bao trùm khoảng kia thì chồng")
    void motBenVoHan_baoTrum() {
        // Cũ:  01/01 ---> vô thời hạn
        // Mới:        01/06 -- 30/06
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-06-01"), ngay("2026-06-30"),
                ngay("2026-01-01"), null)).isTrue();
    }

    @Test
    @DisplayName("8. Một bên vô thời hạn nhưng bắt đầu SAU khi khoảng kia đã kết thúc thì không chồng")
    void motBenVoHan_batDauSau_thiKhongChong() {
        // Cũ:  01/01 -- 31/01
        // Mới:               01/03 ---> vô thời hạn
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-03-01"), null,
                ngay("2026-01-01"), ngay("2026-01-31"))).isFalse();
    }

    @Test
    @DisplayName("9. Cả hai cùng vô thời hạn thì luôn chồng")
    void caHaiVoHan_thiLuonChong() {
        assertThat(service.chongKhoangHieuLuc(
                ngay("2026-01-01"), null,
                ngay("2030-01-01"), null)).isTrue();
    }
}
