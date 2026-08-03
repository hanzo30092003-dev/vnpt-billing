package com.hanzo.billing.service.rating;

import com.hanzo.billing.enums.LoaiDichVu;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

/**
 * ⚠️ QUY TẮC GIỜ CAO ĐIỂM — nơi DUY NHẤT định nghĩa quy tắc này.
 *
 * <p>Cả bộ sinh CDR giả lập ({@link CdrGeneratorService}) lẫn bộ nhập CDR từ CSV
 * ({@link CdrImportService}) đều gọi vào đây. Viết trùng logic ở hai nơi là con đường
 * chắc chắn dẫn tới việc sửa một chỗ mà quên chỗ kia.</p>
 *
 * <p><b>Nội dung quy tắc:</b> chỉ bản ghi dịch vụ {@code THOAI} mới được mang cờ giờ
 * cao điểm, khi thời điểm phát sinh nằm trong khung 8h–20h các ngày Thứ Hai đến
 * Thứ Sáu. SMS và DATA <b>luôn</b> để 0.</p>
 *
 * <p><b>Vì sao chỉ THOAI:</b> bảng giá hiện chỉ có dòng giờ cao điểm cho THOAI, không
 * có cho SMS và DATA. Nếu gắn cờ cao điểm cho SMS, engine tính cước ở Phase 4 sẽ đi
 * tìm dòng giá {@code SMS + gio_cao_diem = 1}, không tìm thấy, và toàn bộ CDR loại đó
 * rơi vào trạng thái LOI.</p>
 *
 * <p>Nếu sau này bổ sung dòng giá cao điểm cho SMS/DATA thì chỉ cần sửa đúng lớp này.</p>
 */
public final class QuyTacGioCaoDiem {

    /** Giờ bắt đầu khung cao điểm (bao gồm). */
    public static final int GIO_BAT_DAU = 8;

    /** Giờ kết thúc khung cao điểm (không bao gồm). */
    public static final int GIO_KET_THUC = 20;

    private QuyTacGioCaoDiem() {
    }

    /**
     * Bản ghi có được đánh dấu là phát sinh trong giờ cao điểm hay không.
     *
     * @param loaiDichVu loại dịch vụ của bản ghi
     * @param thoiDiem   thời điểm phát sinh
     * @return true chỉ khi là THOAI và rơi vào 8h–20h ngày làm việc
     */
    public static boolean apDung(LoaiDichVu loaiDichVu, LocalDateTime thoiDiem) {
        if (loaiDichVu != LoaiDichVu.THOAI || thoiDiem == null) {
            return false;
        }
        DayOfWeek thu = thoiDiem.getDayOfWeek();
        boolean ngayLamViec = (thu != DayOfWeek.SATURDAY && thu != DayOfWeek.SUNDAY);
        int gio = thoiDiem.getHour();
        return ngayLamViec && gio >= GIO_BAT_DAU && gio < GIO_KET_THUC;
    }
}
