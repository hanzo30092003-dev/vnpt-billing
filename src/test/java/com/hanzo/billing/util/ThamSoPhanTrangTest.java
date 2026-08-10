package com.hanzo.billing.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chuẩn hoá tham số phân trang — hồi quy cho lỗi bắt được ở Phase 7 mục A.
 *
 * <p>Nút <i>"Trước"</i> ở trang đầu sinh {@code ?trang=-1}; lớp {@code disabled} của Bootstrap
 * chỉ làm liên kết <b>trông như</b> bị khoá chứ vẫn bấm được. Chỉ số âm đi thẳng vào
 * {@code PageRequest.of} và làm cả trang trả HTTP 500.</p>
 *
 * <p>Bốn màn hình đã tự chặn bằng {@code Math.max(trang, 0)} rải rác trong controller, nhưng
 * hai màn hình mới nhất của Phase 5 — hóa đơn và thanh toán — <b>không chép lại</b> đoạn chặn
 * đó. Đây đúng là hệ quả của việc để một quy tắc nằm rải thay vì gom lại và đặt tên cho nó.</p>
 */
@DisplayName("Tham số phân trang")
class ThamSoPhanTrangTest {

    @Nested
    @DisplayName("Chỉ số trang")
    class ChiSoTrang {

        @Test
        @DisplayName("1. ⭐ Trang âm được đưa về 0 thay vì ném lỗi")
        void trangAmVeKhong() {
            assertThat(ThamSoPhanTrang.trangHopLe(-1)).isZero();
            assertThat(ThamSoPhanTrang.trangHopLe(-999)).isZero();
            assertThat(ThamSoPhanTrang.trangHopLe(Integer.MIN_VALUE)).isZero();
        }

        @Test
        @DisplayName("2. Trang hợp lệ giữ nguyên")
        void trangHopLeGiuNguyen() {
            assertThat(ThamSoPhanTrang.trangHopLe(0)).isZero();
            assertThat(ThamSoPhanTrang.trangHopLe(7)).isEqualTo(7);
            assertThat(ThamSoPhanTrang.trangHopLe(Integer.MAX_VALUE))
                    .as("Trang quá lớn KHÔNG bị chặn ở đây: Spring Data trả về trang rỗng, "
                            + "và một danh sách rỗng là câu trả lời đúng cho 'trang thứ hai tỷ'")
                    .isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Nested
    @DisplayName("Số dòng mỗi trang")
    class SoDongMoiTrang {

        @Test
        @DisplayName("3. Số dòng nhỏ hơn 1 được đưa về 1")
        void soDongQuaNhoVeMot() {
            assertThat(ThamSoPhanTrang.soDongHopLe(0)).isEqualTo(1);
            assertThat(ThamSoPhanTrang.soDongHopLe(-20)).isEqualTo(1);
        }

        @Test
        @DisplayName("4. Số dòng quá lớn bị kẹp về trần, tránh cạn bộ nhớ")
        void soDongQuaLonBiKep() {
            assertThat(ThamSoPhanTrang.soDongHopLe(1_000_000))
                    .isEqualTo(ThamSoPhanTrang.SO_DONG_TOI_DA);
            assertThat(ThamSoPhanTrang.soDongHopLe(25)).isEqualTo(25);
        }
    }
}
