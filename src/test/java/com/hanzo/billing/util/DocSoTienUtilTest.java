package com.hanzo.billing.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Đọc số tiền thành chữ")
class DocSoTienUtilTest {

    private static String doc(String soTien) {
        return DocSoTienUtil.docSoTien(new BigDecimal(soTien));
    }

    @Nested
    @DisplayName("Bốn trường hợp đặc biệt của tiếng Việt")
    class TruongHopDacBiet {

        @Test
        @DisplayName("1. \"linh\" — hàng chục bằng 0 mà hàng đơn vị khác 0")
        void linh() {
            assertThat(doc("105")).isEqualTo("Một trăm linh năm đồng");
            assertThat(doc("101")).isEqualTo("Một trăm linh một đồng");
            assertThat(doc("209")).isEqualTo("Hai trăm linh chín đồng");
        }

        @Test
        @DisplayName("2. \"mươi\" — hàng chục từ 2 trở lên")
        void muoi() {
            assertThat(doc("25")).isEqualTo("Hai mươi lăm đồng");
            assertThat(doc("30")).isEqualTo("Ba mươi đồng");
            assertThat(doc("99")).isEqualTo("Chín mươi chín đồng");
        }

        @Test
        @DisplayName("3. \"mốt\" — hàng đơn vị bằng 1 khi hàng chục từ 2")
        void mot() {
            assertThat(doc("21")).isEqualTo("Hai mươi mốt đồng");
            assertThat(doc("91")).isEqualTo("Chín mươi mốt đồng");
            // Ranh giới: 11 KHÔNG phải "mười mốt"
            assertThat(doc("11")).isEqualTo("Mười một đồng");
        }

        @Test
        @DisplayName("4. \"lăm\" — hàng đơn vị bằng 5 khi hàng chục từ 1")
        void lam() {
            assertThat(doc("15")).isEqualTo("Mười lăm đồng");
            assertThat(doc("25")).isEqualTo("Hai mươi lăm đồng");
            // Ranh giới: 5 đứng một mình vẫn là "năm"
            assertThat(doc("5")).isEqualTo("Năm đồng");
            assertThat(doc("105")).contains("linh năm");
        }
    }

    @Nested
    @DisplayName("Các mốc và số lớn")
    class MocVaSoLon {

        @Test
        @DisplayName("5. Số 0")
        void khong() {
            assertThat(doc("0")).isEqualTo("Không đồng");
        }

        @Test
        @DisplayName("6. Một tỷ — nhóm rỗng phải bị bỏ hẳn, không đọc \"không triệu không nghìn\"")
        void motTy() {
            assertThat(doc("1000000000")).isEqualTo("Một tỷ đồng");
        }

        @Test
        @DisplayName("7. Số lẻ hàng nghìn")
        void soLeHangNghin() {
            assertThat(doc("1234567"))
                    .isEqualTo("Một triệu hai trăm ba mươi bốn nghìn năm trăm sáu mươi bảy đồng");
        }

        /**
         * Nhóm sau nhóm đầu bắt buộc có "không trăm", nếu không hai nhóm sẽ đọc dính vào nhau
         * và người nghe không tách được hàng.
         */
        @Test
        @DisplayName("8. \"không trăm\" ở nhóm không phải nhóm đầu")
        void khongTramONhomSau() {
            assertThat(doc("1005")).isEqualTo("Một nghìn không trăm linh năm đồng");
            assertThat(doc("1000005")).isEqualTo("Một triệu không trăm linh năm đồng");
        }

        @ParameterizedTest(name = "{0} đ → {1}")
        @DisplayName("9. Các giá trị thường gặp trên hóa đơn thật của hệ thống")
        @CsvSource({
                "50000,       Năm mươi nghìn đồng",
                "150000,      Một trăm năm mươi nghìn đồng",
                "204780,      Hai trăm linh bốn nghìn bảy trăm tám mươi đồng",
                "10446,       Mười nghìn bốn trăm bốn mươi sáu đồng",
                "23940596,    Hai mươi ba triệu chín trăm bốn mươi nghìn năm trăm chín mươi sáu đồng",
                "1991417,     Một triệu chín trăm chín mươi mốt nghìn bốn trăm mười bảy đồng"
        })
        void giaTriThuongGap(String soTien, String kyVong) {
            assertThat(doc(soTien)).isEqualTo(kyVong);
        }
    }

    @Nested
    @DisplayName("Đầu vào bất thường")
    class DauVaoBatThuong {

        @Test
        @DisplayName("10. null coi như 0")
        void nullCoiNhuKhong() {
            assertThat(DocSoTienUtil.docSoTien(null)).isEqualTo("Không đồng");
        }

        /** Cột tiền trong CSDL là {@code DECIMAL(15,2)} nên luôn có phần thập phân {@code .00}. */
        @Test
        @DisplayName("11. Scale 2 của CSDL không làm đổi kết quả")
        void scaleCsdlKhongDoiKetQua() {
            assertThat(doc("204780.00")).isEqualTo("Hai trăm linh bốn nghìn bảy trăm tám mươi đồng");
        }

        @Test
        @DisplayName("12. Số lẻ được làm tròn HALF_UP như engine tính cước")
        void soLeLamTronHalfUp() {
            assertThat(doc("100.4")).isEqualTo("Một trăm đồng");
            assertThat(doc("100.5")).isEqualTo("Một trăm linh một đồng");
        }

        @Test
        @DisplayName("13. Số âm đọc kèm \"Âm\"")
        void soAm() {
            assertThat(doc("-15")).isEqualTo("Âm mười lăm đồng");
        }
    }
}
