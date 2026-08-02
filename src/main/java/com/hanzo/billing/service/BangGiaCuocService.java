package com.hanzo.billing.service;

import com.hanzo.billing.dto.BangGiaForm;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;

import java.time.LocalDate;
import java.util.List;

/** Nghiệp vụ quản lý bảng giá cước. */
public interface BangGiaCuocService {

    List<BangGiaCuoc> timCoLoc(boolean locTheoGoi, Long goiCuocId,
                               LoaiDichVu loaiDichVu, HuongCuocGoi huong, boolean chiConHieuLuc);

    BangGiaCuoc layTheoId(Long id);

    BangGiaCuoc luu(BangGiaForm form);

    void xoa(Long id);

    /** Toàn bộ bảng giá có hiệu lực tại một ngày, phục vụ màn hình tra cứu. */
    List<BangGiaCuoc> traCuuTheoNgay(LocalDate ngay);

    /**
     * Hai khoảng hiệu lực có chồng nhau hay không.
     *
     * <p>Khoảng được hiểu là ĐÓNG hai đầu: ngày hết hiệu lực vẫn còn tính là có hiệu lực.
     * Ngày kết thúc {@code null} nghĩa là vô thời hạn.</p>
     *
     * <p>Tách thành phương thức công khai để viết unit test trực tiếp, vì đây là quy tắc
     * dễ sai và hậu quả nặng: hai dòng giá chồng nhau sẽ khiến engine tính cước ở Phase 4
     * không biết chọn dòng nào.</p>
     */
    boolean chongKhoangHieuLuc(LocalDate batDau1, LocalDate ketThuc1,
                               LocalDate batDau2, LocalDate ketThuc2);
}
