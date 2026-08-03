package com.hanzo.billing.service.rating;

import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.exception.NghiepVuException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Kiểm thử chuỗi dự phòng bốn bước khi tra đơn giá.
 *
 * <p>Dựng {@code Bang} thẳng từ một {@code List} nên không cần Spring context lẫn CSDL —
 * đây chính là lý do lớp ảnh chụp được tách khỏi bean {@link BangGiaLookup}.</p>
 *
 * <p>Quy ước đơn giá trong các kịch bản dưới đây, chọn khác nhau để nhìn con số là biết
 * đã đi vào nhánh nào: <b>11</b> = giá riêng gói + cao điểm, <b>12</b> = giá riêng gói +
 * thường, <b>21</b> = giá chung + cao điểm, <b>22</b> = giá chung + thường.</p>
 */
@DisplayName("Tra đơn giá theo chuỗi dự phòng bốn bước")
class BangGiaLookupTest {

    private static final Long GOI = 3L;
    private static final LocalDate NGAY = LocalDate.of(2026, 6, 15);

    @Test
    @DisplayName("1. Đủ cả bốn dòng thì lấy giá riêng của gói ở đúng khung giờ")
    void duBonDong_thiLayGiaRiengDungKhungGio() {
        BangGiaLookup.Bang bang = bang(
                dong(GOI, true, 11), dong(GOI, false, 12),
                dong(null, true, 21), dong(null, false, 22));

        assertThat(donGia(bang, true)).isEqualTo(11);
    }

    @Test
    @DisplayName("2. Thiếu giá riêng cao điểm thì lùi về giá riêng thường, KHÔNG nhảy sang giá chung")
    void thieuGiaRiengCaoDiem_thiLuiVeGiaRiengThuong() {
        // Đây là bước 2 của chuỗi. Nếu cài sai thành "hết giá riêng thì sang giá chung"
        // thì kết quả sẽ ra 21 — gói cước mất luôn đơn giá riêng đã đăng ký.
        BangGiaLookup.Bang bang = bang(
                dong(GOI, false, 12), dong(null, true, 21), dong(null, false, 22));

        assertThat(donGia(bang, true)).isEqualTo(12);
    }

    @Test
    @DisplayName("3. Gói không có dòng riêng nào thì dùng giá chung ở đúng khung giờ")
    void khongCoGiaRieng_thiDungGiaChungDungKhungGio() {
        BangGiaLookup.Bang bang = bang(dong(null, true, 21), dong(null, false, 22));

        assertThat(donGia(bang, true)).isEqualTo(21);
    }

    @Test
    @DisplayName("4. Không có giá chung cao điểm thì lùi về giá chung thường")
    void khongCoGiaChungCaoDiem_thiLuiVeGiaChungThuong() {
        BangGiaLookup.Bang bang = bang(dong(null, false, 22));

        assertThat(donGia(bang, true)).isEqualTo(22);
    }

    @Test
    @DisplayName("5. Chuỗi dự phòng chỉ đi MỘT CHIỀU: bản ghi giờ thường không được lấy giá cao điểm")
    void gioThuong_khongDuocLayGiaCaoDiem() {
        // Chỉ tồn tại dòng cao điểm. Bản ghi giờ thường phải tra không ra, chứ tuyệt đối
        // không được thu giá cao điểm của khách.
        BangGiaLookup.Bang bang = bang(dong(null, true, 21));

        assertThat(bang.tim(GOI, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, NGAY))
                .isEmpty();
    }

    @Test
    @DisplayName("6. Tra theo NGÀY PHÁT SINH của bản ghi, không phải ngày hiện tại")
    void traTheoNgayPhatSinh() {
        BangGiaCuoc cu = dong(null, false, 15);
        cu.setNgayHieuLuc(LocalDate.of(2025, 1, 1));
        cu.setNgayHetHieuLuc(LocalDate.of(2026, 5, 31));

        BangGiaCuoc moi = dong(null, false, 20);
        moi.setNgayHieuLuc(LocalDate.of(2026, 6, 1));

        BangGiaLookup.Bang bang = bang(cu, moi);

        assertThat(bang.traGia(GOI, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false,
                LocalDate.of(2026, 5, 15)).getDonGia()).isEqualByComparingTo("15");
        assertThat(bang.traGia(GOI, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false,
                LocalDate.of(2026, 6, 15)).getDonGia()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("7. Dòng đã hết hiệu lực trước ngày phát sinh thì không được chọn")
    void dongHetHieuLuc_thiKhongDuocChon() {
        BangGiaCuoc hetHan = dong(null, false, 22);
        hetHan.setNgayHetHieuLuc(LocalDate.of(2026, 5, 31));

        assertThat(bang(hetHan).tim(GOI, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, NGAY))
                .isEmpty();
    }

    @Test
    @DisplayName("8. Cột gio_cao_diem để null trong CSDL được coi như giờ thường")
    void gioCaoDiemNull_coiNhuGioThuong() {
        // Cột cho phép null. Nếu khoá tra cứu phân biệt null với false thì một nửa
        // bảng giá sẽ tra không ra.
        BangGiaCuoc khongKhaiCo = dong(null, null, 22);

        assertThat(donGia(bang(khongKhaiCo), false)).isEqualTo(22);
    }

    @Test
    @DisplayName("9. Tra không ra thì ném lỗi nêu rõ tổ hợp, KHÔNG lùi về một đơn giá bất kỳ")
    void traKhongRa_thiNemLoiNeuRoToHop() {
        // Chỉ có giá THOAI. Bản ghi DATA phải làm engine dừng lại chứ không được
        // lặng lẽ tính theo giá thoại.
        BangGiaLookup.Bang bang = bang(dong(null, false, 22));

        assertThatThrownBy(() -> bang.traGia(GOI, LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG,
                false, NGAY))
                .isInstanceOf(NghiepVuException.class)
                .hasMessageContaining("DATA")
                .hasMessageContaining("NOI_MANG");
    }

    // =================================================================
    // TIỆN ÍCH DỰNG DỮ LIỆU
    // =================================================================

    private static BangGiaLookup.Bang bang(BangGiaCuoc... dong) {
        return new BangGiaLookup.Bang(List.of(dong));
    }

    /** Đơn giá tra được cho THOAI / NOI_MANG của gói {@link #GOI} tại {@link #NGAY}. */
    private static int donGia(BangGiaLookup.Bang bang, boolean gioCaoDiem) {
        return bang.traGia(GOI, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, gioCaoDiem, NGAY)
                .getDonGia().intValueExact();
    }

    /** Một dòng THOAI / NOI_MANG hiệu lực từ 01/01/2025, vô thời hạn. */
    private static BangGiaCuoc dong(Long goiCuocId, Boolean gioCaoDiem, int donGia) {
        BangGiaCuoc bg = new BangGiaCuoc();
        if (goiCuocId != null) {
            GoiCuoc goi = new GoiCuoc();
            goi.setId(goiCuocId);
            bg.setGoiCuoc(goi);
        }
        bg.setLoaiDichVu(LoaiDichVu.THOAI);
        bg.setHuong(HuongCuocGoi.NOI_MANG);
        bg.setGioCaoDiem(gioCaoDiem);
        bg.setBlockGiay(6);
        bg.setDonGia(BigDecimal.valueOf(donGia));
        bg.setNgayHieuLuc(LocalDate.of(2025, 1, 1));
        return bg;
    }
}
