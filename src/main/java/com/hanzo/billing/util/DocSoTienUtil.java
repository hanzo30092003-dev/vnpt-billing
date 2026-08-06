package com.hanzo.billing.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Đọc số tiền thành chữ tiếng Việt — dùng cho hóa đơn và phiếu thu.
 *
 * <p>Hóa đơn thật bắt buộc có dòng "bằng chữ" để chống sửa số. Ở đây nó còn có tác dụng thứ
 * hai: một con số sai kiểu <i>lệch một chữ số</i> rất khó thấy trong bảng, nhưng đọc thành chữ
 * thì lộ ngay.</p>
 *
 * <h2>Bốn trường hợp đặc biệt của tiếng Việt</h2>
 * <ul>
 *   <li><b>linh</b> — hàng chục bằng 0 mà hàng đơn vị khác 0: {@code 105} → "một trăm linh năm"</li>
 *   <li><b>mười / mươi</b> — hàng chục bằng 1 đọc "mười", từ 2 trở lên đọc "X mươi"</li>
 *   <li><b>mốt</b> — hàng đơn vị bằng 1 khi hàng chục từ 2: {@code 21} → "hai mươi mốt"</li>
 *   <li><b>lăm</b> — hàng đơn vị bằng 5 khi hàng chục từ 1: {@code 15} → "mười lăm"</li>
 * </ul>
 *
 * <p><b>Quyết định có chủ đích:</b> hàng đơn vị bằng 4 đọc là <b>"bốn"</b> chứ không phải "tư"
 * ({@code 24} → "hai mươi bốn"). Cả hai cách đều được dùng trong tiếng Việt; chọn "bốn" vì nó
 * chỉ có một dạng duy nhất ở mọi vị trí, nên quy tắc ít nhánh hơn và dễ đối chiếu với báo cáo.
 * Ghi rõ ở đây để về sau không ai tưởng là thiếu sót.</p>
 */
public final class DocSoTienUtil {

    private static final String[] CHU_SO = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };

    /**
     * Tên các nhóm ba chữ số, từ nhỏ tới lớn.
     *
     * <p>Tới "triệu tỷ" là phủ hết {@code DECIMAL(15,2)} của mọi cột tiền trong CSDL.</p>
     */
    private static final String[] DON_VI_NHOM = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ"
    };

    private static final int NHOM = 1000;

    private DocSoTienUtil() {
    }

    /**
     * Đọc một khoản tiền thành chữ, đã viết hoa chữ đầu và kèm đuôi "đồng".
     *
     * <p>Làm tròn về đồng trước khi đọc, cùng chế độ {@code HALF_UP} với engine tính cước —
     * số tiền lưu ở {@code DECIMAL(15,2)} luôn có dạng {@code x.00} nên phép làm tròn này
     * không đổi giá trị nào đang có, nhưng chặn được trường hợp truyền vào số lẻ.</p>
     *
     * @param soTien null được coi như 0
     * @return ví dụ {@code "Một trăm linh năm đồng"}
     */
    public static String docSoTien(BigDecimal soTien) {
        BigDecimal lamTron = (soTien == null ? BigDecimal.ZERO : soTien)
                .setScale(0, RoundingMode.HALF_UP);

        boolean am = lamTron.signum() < 0;
        long giaTri = lamTron.abs().longValueExact();

        String chu = giaTri == 0 ? CHU_SO[0] : docSo(giaTri);
        return vietHoaChuDau((am ? "âm " : "") + chu + " đồng");
    }

    /** Phần đọc số thuần tuý, không viết hoa và không có đuôi "đồng". */
    private static String docSo(long giaTri) {
        // Tách thành các nhóm ba chữ số, nhóm nhỏ nhất nằm ở đầu mảng
        int[] nhom = new int[DON_VI_NHOM.length];
        int soNhom = 0;
        long conLai = giaTri;
        while (conLai > 0) {
            nhom[soNhom++] = (int) (conLai % NHOM);
            conLai /= NHOM;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = soNhom - 1; i >= 0; i--) {
            if (nhom[i] == 0) {
                // Nhóm rỗng thì bỏ hẳn: 1.000.000 đọc là "một triệu", không phải
                // "một triệu không nghìn"
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            // laNhomDau: nhóm đầu tiên được đọc. Chỉ nhóm này mới được phép bỏ "không trăm";
            // các nhóm sau bắt buộc có để không đọc dính vào nhau (1.005 -> "một nghìn
            // không trăm linh năm")
            sb.append(docNhomBaChuSo(nhom[i], sb.isEmpty()));
            if (!DON_VI_NHOM[i].isEmpty()) {
                sb.append(' ').append(DON_VI_NHOM[i]);
            }
        }
        return sb.toString();
    }

    private static String docNhomBaChuSo(int n, boolean laNhomDau) {
        int tram = n / 100;
        int chuc = (n / 10) % 10;
        int donVi = n % 10;

        StringBuilder sb = new StringBuilder();

        if (tram > 0) {
            sb.append(CHU_SO[tram]).append(" trăm");
        } else if (!laNhomDau) {
            sb.append("không trăm");
        }

        if (chuc > 1) {
            them(sb, CHU_SO[chuc] + " mươi");
        } else if (chuc == 1) {
            them(sb, "mười");
        } else if (donVi > 0 && !sb.isEmpty()) {
            // Hàng chục bằng 0 nhưng phía trước đã có chữ -> phải chèn "linh"
            them(sb, "linh");
        }

        if (donVi > 0) {
            if (donVi == 1 && chuc >= 2) {
                them(sb, "mốt");
            } else if (donVi == 5 && chuc >= 1) {
                them(sb, "lăm");
            } else {
                them(sb, CHU_SO[donVi]);
            }
        }

        return sb.toString();
    }

    private static void them(StringBuilder sb, String tu) {
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(tu);
    }

    private static String vietHoaChuDau(String chuoi) {
        return chuoi.substring(0, 1).toUpperCase() + chuoi.substring(1);
    }
}
