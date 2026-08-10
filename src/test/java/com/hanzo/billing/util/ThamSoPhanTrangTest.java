package com.hanzo.billing.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiem chan phan trang.
 *
 * <p>Lop {@link ThamSoPhanTrang} sinh ra o Phase 7 muc A sau khi {@code /hoa-don?trang=-1}
 * tra ve HTTP 500: Spring Data nem {@code IllegalArgumentException} khi so trang am. Bon
 * controller khac da co {@code Math.max(trang, 0)} viet thang trong ham - dung luat nhung
 * moi cho mot ban sao, va hai cho con thieu. Cac test duoi day gan luat vao mot cho.
 */
@DisplayName("Chan tham so phan trang")
class ThamSoPhanTrangTest {

    @ParameterizedTest(name = "trang={0} -> 0")
    @ValueSource(ints = {-1, -7, Integer.MIN_VALUE})
    @DisplayName("So trang am bi keo ve 0 thay vi de Spring Data nem ngoai le")
    void trangAmKeoVeKhong(int trang) {
        assertThat(ThamSoPhanTrang.trangHopLe(trang)).isZero();
    }

    @Test
    @DisplayName("So trang hop le di qua nguyen ven")
    void trangHopLeGiuNguyen() {
        assertThat(ThamSoPhanTrang.trangHopLe(0)).isZero();
        assertThat(ThamSoPhanTrang.trangHopLe(3)).isEqualTo(3);
        assertThat(ThamSoPhanTrang.trangHopLe(Integer.MAX_VALUE)).isEqualTo(Integer.MAX_VALUE);
    }

    @ParameterizedTest(name = "soDong={0} -> 1")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("So dong khong duong bi nang len 1")
    void soDongKhongDuongNangLenMot(int soDong) {
        assertThat(ThamSoPhanTrang.soDongHopLe(soDong)).isEqualTo(1);
    }

    @Test
    @DisplayName("So dong vuot tran bi cat ve 200")
    void soDongVuotTranBiCat() {
        // Khong chan tran thi ?soDong=1000000 keo ca bang ra mot trang: mot cau SELECT
        // khong LIMIT thuc su, cong voi ngan the <tr> Thymeleaf phai dung. Day la duong
        // lam nghen may chu chi bang mot dong dia chi.
        assertThat(ThamSoPhanTrang.soDongHopLe(201)).isEqualTo(200);
        assertThat(ThamSoPhanTrang.soDongHopLe(1_000_000)).isEqualTo(200);
        assertThat(ThamSoPhanTrang.soDongHopLe(Integer.MAX_VALUE)).isEqualTo(200);
    }

    @Test
    @DisplayName("Hai bien cua khoang hop le khong bi dong vao")
    void haiBienKhoangHopLe() {
        assertThat(ThamSoPhanTrang.soDongHopLe(1)).isEqualTo(1);
        assertThat(ThamSoPhanTrang.soDongHopLe(ThamSoPhanTrang.SO_DONG_TOI_DA))
                .isEqualTo(ThamSoPhanTrang.SO_DONG_TOI_DA);
    }

    @Test
    @DisplayName("Ham thuan tuy: goi hai lan tren ket qua lan dau khong doi gi them")
    void hamLuyDang() {
        // Tinh chat luy dang (idempotent) la thu bao dam co the goi ham o nhieu tang ma
        // khong lech: controller goi roi service goi lai van ra cung mot so.
        for (int trang : new int[]{-5, 0, 9}) {
            int lanMot = ThamSoPhanTrang.trangHopLe(trang);
            assertThat(ThamSoPhanTrang.trangHopLe(lanMot)).isEqualTo(lanMot);
        }
        for (int soDong : new int[]{-5, 0, 1, 20, 500}) {
            int lanMot = ThamSoPhanTrang.soDongHopLe(soDong);
            assertThat(ThamSoPhanTrang.soDongHopLe(lanMot)).isEqualTo(lanMot);
        }
    }
}
