package com.hanzo.billing.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Kết quả nhập CDR từ file CSV. */
@Getter
@Setter
public class KetQuaImportCdr {

    /** Tên file người dùng đã tải lên. */
    private String tenFile;

    /** Tổng số dòng dữ liệu đã đọc (không tính dòng tiêu đề). */
    private int tongDong;

    private int soThanhCong;

    private final List<DongLoi> danhSachLoi = new ArrayList<>();

    private long thoiGianMs;

    public int getSoLoi() {
        return danhSachLoi.size();
    }

    /**
     * Một dòng bị loại.
     *
     * @param soDong  số dòng trong file, tính từ 1 và ĐÃ tính cả dòng tiêu đề,
     *                để người dùng mở file ra là thấy đúng dòng cần sửa
     * @param noiDung nội dung gốc của dòng, cắt ngắn nếu quá dài
     * @param lyDo    lý do bị loại, viết bằng tiếng Việt
     */
    public record DongLoi(int soDong, String noiDung, String lyDo) {
    }
}
