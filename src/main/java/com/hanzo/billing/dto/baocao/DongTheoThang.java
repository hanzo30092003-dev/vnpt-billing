package com.hanzo.billing.dto.baocao;

/** Số lượng theo từng tháng — dùng cho biểu đồ đường thuê bao mới và rời mạng. */
public record DongTheoThang(int nam, int thang, long soLuong) {

    public String nhan() {
        return String.format("%02d/%d", thang, nam);
    }

    /** Khoá sắp xếp và ghép hai chuỗi số liệu về cùng một trục thời gian. */
    public int khoa() {
        return nam * 100 + thang;
    }
}
