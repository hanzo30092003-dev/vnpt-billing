package com.hanzo.billing.service.rating;

import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.BangGiaCuocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Tra đơn giá cho một bản ghi CDR.
 *
 * <p><b>Vì sao nạp sẵn vào bộ nhớ:</b> một kỳ cước có khoảng 5000 bản ghi CDR. Nếu tra
 * giá bằng một câu SELECT cho mỗi bản ghi thì tính cước một kỳ sinh ra 5000 câu truy vấn.
 * Bảng giá chỉ có chục dòng — nạp một lần vào {@code Map} rồi tra trong bộ nhớ là xong.
 * {@code CdrImportService} đã dùng đúng kỹ thuật này để tra thuê bao.</p>
 *
 * <p><b>Vì sao tách {@link Bang} thành lớp riêng:</b> {@code BangGiaLookup} là bean Spring
 * có phụ thuộc CSDL, còn {@code Bang} là ảnh chụp bất biến, dựng được từ một
 * {@code List} bất kỳ. Nhờ vậy toàn bộ logic tra giá kiểm thử được bằng unit test thuần
 * mà không cần Spring context lẫn CSDL.</p>
 *
 * <p>Ảnh chụp có tuổi thọ đúng bằng một lần chạy tính cước. Không cache lâu hơn: người
 * dùng sửa bảng giá qua giao diện thì lần chạy sau phải thấy giá mới.</p>
 */
@Component
@RequiredArgsConstructor
public class BangGiaLookup {

    private final BangGiaCuocRepository bangGiaCuocRepository;

    /** Chụp toàn bộ bảng giá tại thời điểm gọi. */
    @Transactional(readOnly = true)
    public Bang nap() {
        return new Bang(bangGiaCuocRepository.timTatCaKemGoi());
    }

    /**
     * Khoá tra cứu. {@code goiCuocId} null nghĩa là dòng giá mặc định chung.
     *
     * <p>{@code gioCaoDiem} khai là {@code boolean} nguyên thuỷ chứ không phải
     * {@code Boolean}: cột CSDL cho phép null, mà null và 0 phải được coi là một —
     * nếu để {@code Boolean} thì {@code Khoa(null)} và {@code Khoa(false)} là hai khoá
     * khác nhau và một nửa bảng giá sẽ tra không ra.</p>
     */
    private record Khoa(Long goiCuocId, LoaiDichVu loaiDichVu, HuongCuocGoi huong,
                        boolean gioCaoDiem) {
    }

    /** Ảnh chụp bất biến của bảng giá, đã gom nhóm sẵn theo khoá tra cứu. */
    @Slf4j
    public static final class Bang {

        private final Map<Khoa, List<BangGiaCuoc>> theoKhoa;
        private final int soDong;

        public Bang(List<BangGiaCuoc> danhSach) {
            Map<Khoa, List<BangGiaCuoc>> gom = new HashMap<>();
            for (BangGiaCuoc bg : danhSach) {
                gom.computeIfAbsent(khoaCua(bg), k -> new ArrayList<>()).add(bg);
            }
            this.theoKhoa = gom;
            this.soDong = danhSach.size();
        }

        /** Số dòng bảng giá đang giữ, dùng để ghi log khi bắt đầu chạy tính cước. */
        public int soDong() {
            return soDong;
        }

        /**
         * Tra đơn giá, ném ngoại lệ nếu không có dòng nào áp dụng được.
         *
         * <p>Cố ý <b>không</b> lùi về một đơn giá mặc định nào khác khi tổ hợp
         * {@code (dịch vụ, hướng)} không có trong bảng giá. Lùi như vậy sẽ che mất lỗi
         * dữ liệu: bản ghi sai vẫn ra tiền, và trạng thái {@code LOI} — thứ được thiết kế
         * ra đúng để bắt tình huống này — thành vô dụng.</p>
         */
        public BangGiaCuoc traGia(Long goiCuocId, LoaiDichVu loaiDichVu, HuongCuocGoi huong,
                                  boolean gioCaoDiem, LocalDate ngay) {
            return tim(goiCuocId, loaiDichVu, huong, gioCaoDiem, ngay)
                    .orElseThrow(() -> new NghiepVuException(
                            "Không tìm thấy đơn giá cho tổ hợp " + loaiDichVu + " / " + huong
                                    + (gioCaoDiem ? " / giờ cao điểm" : " / giờ thường")
                                    + " có hiệu lực ngày " + ngay
                                    + ". Bổ sung dòng bảng giá tương ứng rồi tính cước lại."));
        }

        /**
         * Chuỗi dự phòng bốn bước, dừng ở bước đầu tiên tìm được:
         *
         * <ol>
         *   <li>giá riêng của gói + đúng khung giờ</li>
         *   <li>giá riêng của gói + khung giờ thường</li>
         *   <li>giá mặc định chung + đúng khung giờ</li>
         *   <li>giá mặc định chung + khung giờ thường</li>
         * </ol>
         *
         * <p>Chỉ lùi trên trục <b>khung giờ</b>, không lùi trên trục
         * {@code (dịch vụ, hướng)}. Lý do: giá cao điểm là một bậc phụ thu tuỳ chọn —
         * thiếu nó thì dùng giá thường là hợp lý. Còn dịch vụ và hướng là danh tính của
         * bản ghi, tra không ra nghĩa là dữ liệu sai chứ không phải thiếu bậc giá.</p>
         */
        public Optional<BangGiaCuoc> tim(Long goiCuocId, LoaiDichVu loaiDichVu,
                                         HuongCuocGoi huong, boolean gioCaoDiem, LocalDate ngay) {
            if (goiCuocId != null) {
                Optional<BangGiaCuoc> giaRieng =
                        timTheoKhungGio(goiCuocId, loaiDichVu, huong, gioCaoDiem, ngay);
                if (giaRieng.isPresent()) {
                    return giaRieng;
                }
            }
            return timTheoKhungGio(null, loaiDichVu, huong, gioCaoDiem, ngay);
        }

        /** Thử đúng khung giờ trước; không có thì lùi về khung giờ thường. */
        private Optional<BangGiaCuoc> timTheoKhungGio(Long goiCuocId, LoaiDichVu loaiDichVu,
                                                      HuongCuocGoi huong, boolean gioCaoDiem,
                                                      LocalDate ngay) {
            Optional<BangGiaCuoc> dungKhungGio =
                    chonTheoNgay(new Khoa(goiCuocId, loaiDichVu, huong, gioCaoDiem), ngay);
            if (dungKhungGio.isPresent() || !gioCaoDiem) {
                return dungKhungGio;
            }
            return chonTheoNgay(new Khoa(goiCuocId, loaiDichVu, huong, false), ngay);
        }

        /**
         * Trong một nhóm cùng khoá, chọn dòng còn hiệu lực tại ngày phát sinh.
         *
         * <p>Lẽ ra mỗi ngày chỉ có đúng một dòng vì Phase 3C đã chặn chồng khoảng hiệu
         * lực khi thêm/sửa. Nhưng dữ liệu có thể được nạp thẳng bằng SQL, nên nếu gặp
         * nhiều hơn một dòng thì ghi cảnh báo rồi lấy dòng có ngày hiệu lực mới nhất —
         * im lặng lấy dòng đầu tiên sẽ khiến kết quả phụ thuộc thứ tự trả về của CSDL.</p>
         */
        private Optional<BangGiaCuoc> chonTheoNgay(Khoa khoa, LocalDate ngay) {
            List<BangGiaCuoc> ungVien = theoKhoa.getOrDefault(khoa, List.of()).stream()
                    .filter(bg -> conHieuLuc(bg, ngay))
                    .toList();
            if (ungVien.size() > 1) {
                log.warn("Có {} dòng bảng giá cùng áp dụng ngày {} cho {} — lấy dòng có "
                        + "ngày hiệu lực mới nhất. Kiểm tra lại chồng khoảng hiệu lực.",
                        ungVien.size(), ngay, khoa);
            }
            return ungVien.stream().max(Comparator.comparing(BangGiaCuoc::getNgayHieuLuc));
        }

        /** Khoảng hiệu lực đóng hai đầu; {@code ngayHetHieuLuc} null là vô thời hạn. */
        private static boolean conHieuLuc(BangGiaCuoc bg, LocalDate ngay) {
            return !bg.getNgayHieuLuc().isAfter(ngay)
                    && (bg.getNgayHetHieuLuc() == null || !bg.getNgayHetHieuLuc().isBefore(ngay));
        }

        private static Khoa khoaCua(BangGiaCuoc bg) {
            return new Khoa(
                    bg.getGoiCuoc() == null ? null : bg.getGoiCuoc().getId(),
                    bg.getLoaiDichVu(),
                    bg.getHuong(),
                    Boolean.TRUE.equals(bg.getGioCaoDiem()));
        }
    }
}
