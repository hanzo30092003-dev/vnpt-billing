package com.hanzo.billing.service;

import com.hanzo.billing.dto.ThueBaoForm;
import com.hanzo.billing.entity.*;
import com.hanzo.billing.enums.HinhThucNapTien;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Nghiệp vụ quản lý thuê bao. */
public interface ThueBaoService {

    Page<ThueBao> timKiem(String tuKhoa, TrangThaiThueBao trangThai,
                          LoaiThueBao loaiThueBao, Long goiCuocId, Pageable pageable);

    ThueBao layTheoId(Long id);

    /** Đăng ký thuê bao mới. Tạo đồng thời đăng ký gói cước và bản ghi lịch sử. */
    ThueBao dangKyMoi(ThueBaoForm form);

    /**
     * Nơi DUY NHẤT được phép đổi trạng thái thuê bao. Kiểm tra ma trận chuyển
     * hợp lệ, ghi lịch sử và nhật ký hệ thống.
     */
    void chuyenTrangThai(Long id, TrangThaiThueBao trangThaiMoi, String lyDo);

    /** Đổi gói cước, hiệu lực từ ngày đầu tháng kế tiếp. */
    void doiGoiCuoc(Long id, Long goiCuocMoiId);

    /** Nạp tiền cho thuê bao trả trước. */
    void napTien(Long id, BigDecimal soTien, HinhThucNapTien hinhThuc);

    List<DangKyGoiCuoc> layLichSuGoiCuoc(Long thueBaoId);

    List<LichSuThueBao> layLichSuTrangThai(Long thueBaoId);

    List<NapTien> layLichSuNapTien(Long thueBaoId);

    /** Ngày gói cước mới bắt đầu có hiệu lực nếu đổi gói ngay bây giờ. */
    LocalDate ngayHieuLucDoiGoi();

    /** Các trạng thái mà thuê bao hiện tại được phép chuyển sang. */
    List<TrangThaiThueBao> cacTrangThaiCoTheChuyen(TrangThaiThueBao trangThaiHienTai);

    /**
     * Danh sách gói cước còn mở bán, dùng đổ vào ô chọn trên form.
     *
     * <p>Đặt tạm ở đây vì màn hình thuê bao cần; quản lý gói cước đầy đủ
     * (thêm/sửa/ngừng bán) thuộc phạm vi Phase 3.</p>
     */
    List<GoiCuoc> layGoiCuocConHieuLuc();
}
