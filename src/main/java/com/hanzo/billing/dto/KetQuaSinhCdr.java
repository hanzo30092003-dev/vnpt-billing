package com.hanzo.billing.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Thống kê trả về sau khi sinh CDR. */
@Getter
@Setter
public class KetQuaSinhCdr {

    private int tongBanGhi;

    /** Số bản ghi theo từng loại dịch vụ, ví dụ {THOAI=3500, SMS=1000, DATA=500}. */
    private Map<String, Integer> theoLoaiDichVu = new LinkedHashMap<>();

    /** Số bản ghi theo từng hướng. */
    private Map<String, Integer> theoHuong = new LinkedHashMap<>();

    private int soThueBaoCoPhatSinh;

    /** Thời gian thực hiện, tính bằng mili giây. */
    private long thoiGianMs;

    /** Số thuê bao bị bỏ qua vì kích hoạt sau khoảng thời gian yêu cầu. */
    private int soThueBaoBoQua;
}
