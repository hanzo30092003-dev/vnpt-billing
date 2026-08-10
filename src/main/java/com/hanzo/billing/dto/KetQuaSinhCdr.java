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

    /**
     * Hạt giống <b>thực sự đã dùng</b> cho lần sinh này.
     *
     * <p>Luôn có giá trị, kể cả khi người dùng để trống ô hạt giống — khi đó hệ thống tự bốc
     * một số rồi trả nó về đây. Đó là điểm mấu chốt: một lần sinh "ngẫu nhiên" vẫn phải nói ra
     * được con số để dựng lại chính nó. Không ghi lại thì bộ dữ liệu vừa sinh là thứ không
     * bao giờ tái lập được.</p>
     */
    private long hatGiongDaDung;
}
