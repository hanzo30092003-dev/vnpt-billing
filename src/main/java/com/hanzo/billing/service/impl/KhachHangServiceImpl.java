package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.KhachHangForm;
import com.hanzo.billing.entity.KhachHang;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiKhachHang;
import com.hanzo.billing.enums.TrangThaiKhachHang;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.KhachHangRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.KhachHangService;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class KhachHangServiceImpl implements KhachHangService {

    private static final String TIEN_TO_MA_KH = "KH";

    private final KhachHangRepository khachHangRepository;
    private final ThueBaoRepository thueBaoRepository;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional(readOnly = true)
    public Page<KhachHang> timKiem(String tuKhoa, LoaiKhachHang loaiKh,
                                   TrangThaiKhachHang trangThai, Pageable pageable) {
        String tuKhoaChuan = (tuKhoa == null || tuKhoa.isBlank()) ? null : tuKhoa.trim();
        return khachHangRepository.timKiem(tuKhoaChuan, loaiKh, trangThai, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public KhachHang layTheoId(Long id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy khách hàng có mã số " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ThueBao> layThueBaoCuaKhachHang(Long khachHangId) {
        return thueBaoRepository.timTheoKhachHangKemGoiCuoc(khachHangId);
    }

    @Override
    @Transactional
    public KhachHang luu(KhachHangForm form) {
        boolean themMoi = (form.getId() == null);

        // Số giấy tờ là định danh pháp lý của khách hàng nên không được trùng.
        // Khi sửa thì bỏ qua chính bản ghi đang sửa, nếu không sẽ tự báo trùng với chính mình.
        boolean trungGiayTo = themMoi
                ? khachHangRepository.existsBySoGiayTo(form.getSoGiayTo())
                : khachHangRepository.existsBySoGiayToAndIdNot(form.getSoGiayTo(), form.getId());
        if (trungGiayTo) {
            throw new NghiepVuException(
                    "Số giấy tờ " + form.getSoGiayTo() + " đã được dùng cho một khách hàng khác");
        }

        KhachHang khachHang = themMoi ? new KhachHang() : layTheoId(form.getId());

        if (themMoi) {
            khachHang.setMaKh(sinhMaKhachHang());
            khachHang.setTrangThai(TrangThaiKhachHang.HOAT_DONG);
        } else if (form.getTrangThai() != null) {
            khachHang.setTrangThai(form.getTrangThai());
        }

        khachHang.setLoaiKh(form.getLoaiKh());
        khachHang.setTenKh(form.getTenKh().trim());
        khachHang.setSoGiayTo(form.getSoGiayTo().trim());
        khachHang.setDiaChi(form.getDiaChi().trim());
        khachHang.setDienThoaiLh(rongThanhNull(form.getDienThoaiLh()));
        khachHang.setEmail(rongThanhNull(form.getEmail()));
        khachHang.setNgayDangKy(form.getNgayDangKy());
        khachHang.setGhiChu(rongThanhNull(form.getGhiChu()));

        // Hai trường dưới đây loại trừ nhau theo loại khách hàng. Xoá trắng trường
        // không dùng để dữ liệu không bị "dính" lại khi người dùng đổi loại khách hàng.
        if (form.getLoaiKh() == LoaiKhachHang.CA_NHAN) {
            khachHang.setNgaySinh(form.getNgaySinh());
            khachHang.setNguoiDaiDien(null);
        } else {
            khachHang.setNgaySinh(null);
            khachHang.setNguoiDaiDien(rongThanhNull(form.getNguoiDaiDien()));
        }

        KhachHang daLuu = khachHangRepository.save(khachHang);

        nhatKyService.ghiNhatKy(
                themMoi ? "TAO_KHACH_HANG" : "SUA_KHACH_HANG",
                "KHACH_HANG",
                daLuu.getId(),
                (themMoi ? "Tạo mới khách hàng " : "Cập nhật khách hàng ")
                        + daLuu.getMaKh() + " - " + daLuu.getTenKh());

        return daLuu;
    }

    @Override
    @Transactional
    public void ngungGiaoDich(Long id) {
        KhachHang khachHang = layTheoId(id);

        if (khachHang.getTrangThai() == TrangThaiKhachHang.NGUNG_GIAO_DICH) {
            throw new NghiepVuException("Khách hàng " + khachHang.getMaKh() + " đã ngừng giao dịch từ trước");
        }

        // Không cho ngừng giao dịch khi khách còn thuê bao đang chạy: cước vẫn phát sinh
        // hằng tháng, ngừng giao dịch lúc này sẽ tạo ra công nợ không ai chịu trách nhiệm.
        // Phải thanh lý hoặc tạm ngưng hết thuê bao trước.
        long soThueBaoHoatDong = thueBaoRepository.countByKhachHangIdAndTrangThai(
                id, TrangThaiThueBao.HOAT_DONG);
        if (soThueBaoHoatDong > 0) {
            throw new NghiepVuException("Khách hàng còn " + soThueBaoHoatDong
                    + " thuê bao đang hoạt động, không thể ngừng giao dịch");
        }

        khachHang.setTrangThai(TrangThaiKhachHang.NGUNG_GIAO_DICH);
        khachHangRepository.save(khachHang);

        nhatKyService.ghiNhatKy("NGUNG_GIAO_DICH_KHACH_HANG", "KHACH_HANG", id,
                "Ngừng giao dịch khách hàng " + khachHang.getMaKh() + " - " + khachHang.getTenKh());
    }

    /**
     * Sinh mã khách hàng kế tiếp theo định dạng {@code KH} + 6 chữ số.
     *
     * <p>Lấy số thứ tự lớn nhất đang có rồi cộng 1, thay vì đếm số bản ghi — vì nếu
     * đếm thì sau khi có khách hàng bị xoá, mã mới sẽ trùng với mã đã từng cấp.</p>
     */
    private String sinhMaKhachHang() {
        long soTiepTheo = khachHangRepository.timSoThuTuLonNhat() + 1;
        return TIEN_TO_MA_KH + String.format("%06d", soTiepTheo);
    }

    /** Chuỗi rỗng từ form được quy về null để CSDL không lưu chuỗi trống vô nghĩa. */
    private String rongThanhNull(String giaTri) {
        if (giaTri == null) {
            return null;
        }
        String daCat = giaTri.trim();
        return daCat.isEmpty() ? null : daCat;
    }
}
