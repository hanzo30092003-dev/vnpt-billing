package com.hanzo.billing.service.rating;

import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Hai quy tắc dùng chung giữa engine lập hóa đơn và bảng đối soát cước.
 *
 * <p>Tách ra vì cả hai nơi đều phải trả lời đúng cùng hai câu hỏi — <i>thuê bao dùng dịch
 * vụ bao nhiêu ngày trong kỳ</i> và <i>gói nào có hiệu lực tại một ngày</i>. Nếu bảng đối
 * soát tự tính lại theo cách riêng thì nó sẽ có lúc mâu thuẫn với chính hóa đơn mà nó đang
 * đối soát — mà đối soát chính là thứ để chứng minh hóa đơn đúng.</p>
 */
public final class QuyTacKyCuoc {

    private QuyTacKyCuoc() {
    }

    /**
     * Khoảng ngày thuê bao thực sự sử dụng dịch vụ trong kỳ.
     *
     * @param soNgaySuDung  số ngày thực dùng, tính <b>cả hai đầu</b>
     * @param soNgayTrongKy tổng số ngày của kỳ
     */
    public record KhoangSuDung(LocalDate tuNgay, LocalDate denNgay,
                               long soNgaySuDung, long soNgayTrongKy) {

        /** Dùng trọn kỳ thì thu đủ cước tháng, không prorate. */
        public boolean troVenTronKy() {
            return soNgaySuDung == soNgayTrongKy;
        }
    }

    /**
     * Tính khoảng sử dụng của một thuê bao trong một kỳ.
     *
     * <p>Một công thức phủ hết mọi trường hợp thay vì phân nhánh theo trạng thái:</p>
     * <ul>
     *   <li>Kích hoạt trước kỳ, chưa hủy → trọn kỳ</li>
     *   <li>Kích hoạt giữa kỳ → từ ngày kích hoạt tới cuối kỳ, tính cả ngày kích hoạt</li>
     *   <li>Hủy giữa kỳ → từ đầu kỳ tới ngày hủy</li>
     *   <li>Hủy trước kỳ, hoặc kích hoạt sau kỳ → khoảng rỗng</li>
     * </ul>
     *
     * <p>Thuê bao đã thanh lý mà <b>không ghi ngày hủy</b> là dữ liệu hỏng. Không đoán mò:
     * trả về rỗng để bên gọi bỏ qua và ghi cảnh báo. Đoán "chưa hủy" sẽ thu đủ cước tháng
     * của một thuê bao đã thanh lý — sai tiền mà không có dấu hiệu gì.</p>
     *
     * @return rỗng nếu thuê bao không dùng ngày nào trong kỳ
     */
    public static Optional<KhoangSuDung> khoangSuDung(ThueBao thueBao, KyCuoc ky) {
        if (thueBao.getTrangThai() == TrangThaiThueBao.DA_THANH_LY
                && thueBao.getNgayHuy() == null) {
            return Optional.empty();
        }

        long soNgayTrongKy =
                ChronoUnit.DAYS.between(ky.getNgayBatDau(), ky.getNgayKetThuc()) + 1;

        LocalDate tuNgay = thueBao.getNgayKichHoat().isAfter(ky.getNgayBatDau())
                ? thueBao.getNgayKichHoat() : ky.getNgayBatDau();
        LocalDate denNgay = (thueBao.getNgayHuy() != null
                && thueBao.getNgayHuy().isBefore(ky.getNgayKetThuc()))
                ? thueBao.getNgayHuy() : ky.getNgayKetThuc();

        if (tuNgay.isAfter(denNgay)) {
            return Optional.empty();
        }
        return Optional.of(new KhoangSuDung(tuNgay, denNgay,
                ChronoUnit.DAYS.between(tuNgay, denNgay) + 1, soNgayTrongKy));
    }

    /**
     * Bản ghi đăng ký gói có hiệu lực tại một ngày.
     *
     * <p>Khoảng hiệu lực đóng hai đầu; {@code ngayKetThuc} null nghĩa là còn áp dụng.
     * Duyệt theo thứ tự danh sách truyền vào, nên nếu danh sách đã sắp xếp ngày bắt đầu
     * giảm dần thì bản đăng ký muộn nhất thắng khi có nhiều bản cùng phủ một ngày.</p>
     */
    public static Optional<GoiCuoc> goiCuocHieuLucTai(List<DangKyGoiCuoc> lichSu, LocalDate ngay) {
        for (DangKyGoiCuoc dangKy : lichSu) {
            boolean daBatDau = !dangKy.getNgayBatDau().isAfter(ngay);
            boolean chuaKetThuc = dangKy.getNgayKetThuc() == null
                    || !dangKy.getNgayKetThuc().isBefore(ngay);
            if (daBatDau && chuaKetThuc) {
                return Optional.of(dangKy.getGoiCuoc());
            }
        }
        return Optional.empty();
    }
}
