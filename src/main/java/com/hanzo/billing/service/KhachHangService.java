package com.hanzo.billing.service;

import com.hanzo.billing.dto.KhachHangForm;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiKhachHang;
import com.hanzo.billing.enums.TrangThaiKhachHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Nghiệp vụ quản lý khách hàng. */
public interface KhachHangService {

    Page<KhachHang> timKiem(String tuKhoa, LoaiKhachHang loaiKh,
                            TrangThaiKhachHang trangThai, Pageable pageable);

    KhachHang layTheoId(Long id);

    /** Danh sách thuê bao của khách hàng, đã nạp sẵn gói cước để hiển thị. */
    List<ThueBao> layThueBaoCuaKhachHang(Long khachHangId);

    /** Thêm mới hoặc cập nhật. Mã khách hàng được tự sinh khi thêm mới. */
    KhachHang luu(KhachHangForm form);

    /** Xoá mềm: chuyển sang NGUNG_GIAO_DICH. */
    void ngungGiaoDich(Long id);

    /** Khách hàng còn giao dịch, dùng đổ vào ô chọn khi đăng ký thuê bao. */
    List<KhachHang> layKhachHangDangHoatDong();
}
