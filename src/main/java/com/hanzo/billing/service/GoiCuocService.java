package com.hanzo.billing.service;

import com.hanzo.billing.dto.GoiCuocDto;
import com.hanzo.billing.dto.GoiCuocForm;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.ThueBao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Nghiệp vụ quản lý danh mục gói cước. */
public interface GoiCuocService {

    /** Danh sách kèm số thuê bao đang dùng và chuỗi tóm tắt ưu đãi. */
    List<GoiCuocDto> layDanhSach();

    GoiCuoc layTheoId(Long id);

    GoiCuoc luu(GoiCuocForm form);

    /** Xoá gói cước. Bị chặn nếu còn thuê bao đang dùng. */
    void xoa(Long id);

    long demThueBaoDangDung(Long goiCuocId);

    /** Bảng giá riêng gắn với gói cước này (không tính bảng giá mặc định chung). */
    List<BangGiaCuoc> layBangGiaRieng(Long goiCuocId);

    Page<ThueBao> layThueBaoDangDung(Long goiCuocId, Pageable pageable);

    /** Chuỗi tóm tắt ưu đãi của một gói, ví dụ "100 phút NM · 30 SMS · 2 GB". */
    String tomTatUuDai(GoiCuoc goiCuoc);
}
