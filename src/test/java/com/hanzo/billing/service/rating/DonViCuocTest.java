package com.hanzo.billing.service.rating;

import com.hanzo.billing.exception.NghiepVuException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử ba phép quy đổi đơn vị của engine tính cước.
 *
 * <p>Ba phép này hỏng theo cùng một kiểu: hóa đơn vẫn phát hành bình thường, không lỗi,
 * không cảnh báo, chỉ sai số tiền — sai 1024 lần với data và 60 lần với thoại. Không có
 * cách nào phát hiện ngoài việc kiểm thử trực tiếp từng hàm.</p>
 */
@DisplayName("Quy đổi đơn vị cước")
class DonViCuocTest {

    @Nested
    @DisplayName("Số block, làm tròn lên")
    class SoBlock {

        @Test
        @DisplayName("Chia không hết thì làm tròn LÊN: cuộc 45 giây, block 6 giây → 8 block")
        void chiaKhongHet_thiLamTronLen() {
            // 45 / 6 = 7,5 — tính tiền 8 block chứ không phải 7
            assertThat(DonViCuoc.soBlock(45, 6)).isEqualTo(8);
        }

        @Test
        @DisplayName("Chia hết thì không cộng thêm block thừa: 42 giây, block 6 giây → đúng 7 block")
        void chiaHet_thiKhongCongThemBlock() {
            // Đây là bẫy kinh điển của công thức (a + b - 1) / b viết sai thành (a + b) / b
            assertThat(DonViCuoc.soBlock(42, 6)).isEqualTo(7);
        }

        @Test
        @DisplayName("Dùng dở một block vẫn tính trọn block: 1 giây, block 60 giây → 1 block")
        void dungDoMotBlock_thiTinhTronBlock() {
            assertThat(DonViCuoc.soBlock(1, 60)).isEqualTo(1);
        }

        @Test
        @DisplayName("Sản lượng bằng 0 hoặc âm thì không có block nào")
        void sanLuongKhongDuong_thiKhongCoBlock() {
            // SMS và DATA luôn có thoi_luong_giay = 0, phải trả 0 chứ không phải 1
            assertThat(DonViCuoc.soBlock(0, 6)).isZero();
            assertThat(DonViCuoc.soBlock(-5, 6)).isZero();
        }

        @Test
        @DisplayName("Block nhỏ hơn hoặc bằng 0 thì ném lỗi nghiệp vụ, không chia cho 0")
        void blockKhongDuong_thiNemLoi() {
            assertThatThrownBy(() -> DonViCuoc.soBlock(100, 0))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("block_giay");
            assertThatThrownBy(() -> DonViCuoc.soBlock(100, -1))
                    .isInstanceOf(NghiepVuException.class);
        }
    }

    @Nested
    @DisplayName("KB sang MB — chỗ quy đổi số 1 và số 2")
    class KbSangMb {

        @Test
        @DisplayName("Đúng 1 MB: 1024 KB → 1 MB")
        void dungMotMb() {
            assertThat(DonViCuoc.kbSangMb(1024)).isEqualTo(1);
        }

        @Test
        @DisplayName("Lẻ một chút vẫn tính trọn MB: 1025 KB → 2 MB, 1023 KB → 1 MB")
        void leThiLamTronLen() {
            assertThat(DonViCuoc.kbSangMb(1025)).isEqualTo(2);
            assertThat(DonViCuoc.kbSangMb(1023)).isEqualTo(1);
        }

        @Test
        @DisplayName("Ví dụ trong tài liệu: 1.500.000 KB → 1465 MB, vẫn dưới ưu đãi 2048 MB của MAX70")
        void viDuTrongTaiLieu_vanTrongUuDai() {
            // Đây chính là con số ở docs/mo-ta-csdl.md mục 6.1. Nếu engine so thẳng
            // 1500000 > 2048 thì kết luận vượt ưu đãi và tính cước cho phần "vượt" khổng lồ.
            assertThat(DonViCuoc.kbSangMb(1_500_000)).isEqualTo(1465);
            assertThat(DonViCuoc.kbSangMb(1_500_000)).isLessThan(2048);
        }

        @Test
        @DisplayName("Không dùng data thì 0 MB")
        void khongDungData() {
            assertThat(DonViCuoc.kbSangMb(0)).isZero();
        }
    }

    @Nested
    @DisplayName("Giây sang phút — chỗ quy đổi số 3")
    class GiaySangPhut {

        @Test
        @DisplayName("Đúng 1 phút: 60 giây → 1 phút")
        void dungMotPhut() {
            assertThat(DonViCuoc.giaySangPhut(60)).isEqualTo(1);
        }

        @Test
        @DisplayName("Lẻ một giây vẫn tính trọn phút: 61 giây → 2 phút")
        void leMotGiay_thiLamTronLen() {
            assertThat(DonViCuoc.giaySangPhut(61)).isEqualTo(2);
        }

        @Test
        @DisplayName("Ví dụ trong tài liệu: 5400 giây → 90 phút, vẫn dưới ưu đãi 100 phút của MAX70")
        void viDuTrongTaiLieu_vanTrongUuDai() {
            // So thẳng 5400 > 100 là sai 60 lần, và hóa đơn vẫn phát hành bình thường.
            assertThat(DonViCuoc.giaySangPhut(5400)).isEqualTo(90);
            assertThat(DonViCuoc.giaySangPhut(5400)).isLessThan(100);
        }

        @Test
        @DisplayName("Không gọi thì 0 phút")
        void khongGoi() {
            assertThat(DonViCuoc.giaySangPhut(0)).isZero();
        }
    }
}
