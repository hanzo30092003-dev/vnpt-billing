package com.hanzo.billing.service.rating;

import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * ⚠️ QUY TẮC TỔ HỢP (DỊCH VỤ, HƯỚNG) — nơi DUY NHẤT định nghĩa quy tắc này.
 *
 * <p>Cả bộ sinh CDR giả lập ({@link CdrGeneratorService}) lẫn bộ nhập CDR từ CSV
 * ({@link CdrImportService}) đều gọi vào đây, đúng theo mô hình đã dùng cho
 * {@link QuyTacGioCaoDiem}.</p>
 *
 * <p><b>Nội dung quy tắc:</b></p>
 * <ul>
 *   <li>{@code THOAI} — cả ba hướng {@code NOI_MANG} / {@code NGOAI_MANG} / {@code QUOC_TE}</li>
 *   <li>{@code SMS} — cả ba hướng; tin nhắn quốc tế là dịch vụ có thật, có dòng bảng giá riêng</li>
 *   <li>{@code DATA} — <b>luôn</b> {@code NOI_MANG}</li>
 * </ul>
 *
 * <p><b>Vì sao DATA chỉ có một hướng:</b> phiên data đi ra Internet, không có khái niệm
 * gọi nội mạng hay ngoại mạng — {@code DATA + NGOAI_MANG} là mô hình hoá sai chứ không
 * phải thiếu bảng giá. Còn {@code DATA + QUOC_TE} là data roaming, đã nằm ngoài phạm vi
 * đồ án ngay từ kế hoạch gốc.</p>
 *
 * <p><b>Vì sao lớp này tồn tại — bài học từ Phase 3:</b> trước khi rà soát Phase 4, bộ sinh
 * chọn hướng độc lập với loại dịch vụ, nên sinh ra 251 bản ghi (5,00%) thuộc ba tổ hợp
 * không có dòng bảng giá nào khớp. Toàn bộ số đó sẽ rơi vào trạng thái {@code LOI} ngay
 * lần chạy engine tính cước đầu tiên. Xem {@code docs/PHASE-4-REPORT.md} mục 3.</p>
 */
public final class QuyTacToHopDichVu {

    private static final Map<LoaiDichVu, Set<HuongCuocGoi>> HUONG_HOP_LE =
            new EnumMap<>(LoaiDichVu.class);

    static {
        HUONG_HOP_LE.put(LoaiDichVu.THOAI, EnumSet.allOf(HuongCuocGoi.class));
        HUONG_HOP_LE.put(LoaiDichVu.SMS, EnumSet.allOf(HuongCuocGoi.class));
        HUONG_HOP_LE.put(LoaiDichVu.DATA, EnumSet.of(HuongCuocGoi.NOI_MANG));
    }

    private QuyTacToHopDichVu() {
    }

    /**
     * Các hướng hợp lệ của một loại dịch vụ.
     *
     * @return tập không rỗng, không sửa được
     */
    public static Set<HuongCuocGoi> huongHopLe(LoaiDichVu loaiDichVu) {
        return Set.copyOf(HUONG_HOP_LE.get(loaiDichVu));
    }

    /** Tổ hợp có được phép tồn tại trong dữ liệu hay không. */
    public static boolean hopLe(LoaiDichVu loaiDichVu, HuongCuocGoi huong) {
        return loaiDichVu != null && huong != null
                && HUONG_HOP_LE.get(loaiDichVu).contains(huong);
    }

    /** Thông báo tiếng Việt để hiển thị cho người dùng khi tổ hợp bị từ chối. */
    public static String thongBaoKhongHopLe(LoaiDichVu loaiDichVu, HuongCuocGoi huong) {
        return "Dịch vụ " + loaiDichVu + " không có hướng " + huong
                + ", chỉ chấp nhận " + huongHopLe(loaiDichVu);
    }
}
