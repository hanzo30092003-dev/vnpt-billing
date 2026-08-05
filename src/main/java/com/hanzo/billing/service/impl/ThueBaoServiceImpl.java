package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.ThueBaoForm;
import com.hanzo.billing.entity.*;
import com.hanzo.billing.enums.*;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.*;
import com.hanzo.billing.service.NhatKyService;
import com.hanzo.billing.service.ThueBaoService;
import com.hanzo.billing.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ThueBaoServiceImpl implements ThueBaoService {

    // =================================================================
    // MA TRẬN CHUYỂN TRẠNG THÁI THUÊ BAO
    // =================================================================
    // Đây là quy tắc nghiệp vụ cốt lõi của vòng đời thuê bao. Đặt thành hằng số
    // ở một chỗ duy nhất thay vì rải if/else trong controller, để:
    //   - Nhìn một chỗ là biết được toàn bộ luật chuyển
    //   - Mọi lối vào (giao diện, sau này là API) đều đi qua cùng một kiểm tra
    //
    // Ý nghĩa nghiệp vụ:
    //   HOAT_DONG    -> có thể tạm ngưng 1 chiều, 2 chiều, hoặc thanh lý
    //   TAM_NGUNG_1C -> nâng lên 2 chiều, khôi phục lại, hoặc thanh lý
    //   TAM_NGUNG_2C -> khôi phục thẳng về hoạt động, hoặc thanh lý
    //   DA_THANH_LY  -> TRẠNG THÁI CUỐI, không quay lại được. Số thuê bao đã
    //                   thu hồi và có thể cấp lại cho khách khác, nên khôi phục
    //                   sẽ làm sai lệch dữ liệu cước lịch sử.
    // =================================================================
    private static final Map<TrangThaiThueBao, Set<TrangThaiThueBao>> CHUYEN_HOP_LE =
            new EnumMap<>(TrangThaiThueBao.class);

    static {
        CHUYEN_HOP_LE.put(TrangThaiThueBao.HOAT_DONG, EnumSet.of(
                TrangThaiThueBao.TAM_NGUNG_1C, TrangThaiThueBao.TAM_NGUNG_2C, TrangThaiThueBao.DA_THANH_LY));
        CHUYEN_HOP_LE.put(TrangThaiThueBao.TAM_NGUNG_1C, EnumSet.of(
                TrangThaiThueBao.TAM_NGUNG_2C, TrangThaiThueBao.HOAT_DONG, TrangThaiThueBao.DA_THANH_LY));
        CHUYEN_HOP_LE.put(TrangThaiThueBao.TAM_NGUNG_2C, EnumSet.of(
                TrangThaiThueBao.HOAT_DONG, TrangThaiThueBao.DA_THANH_LY));
        CHUYEN_HOP_LE.put(TrangThaiThueBao.DA_THANH_LY, EnumSet.noneOf(TrangThaiThueBao.class));
    }

    private final ThueBaoRepository thueBaoRepository;
    private final KhachHangRepository khachHangRepository;
    private final GoiCuocRepository goiCuocRepository;
    private final DangKyGoiCuocRepository dangKyGoiCuocRepository;
    private final LichSuThueBaoRepository lichSuThueBaoRepository;
    private final BienDongSoDuRepository bienDongSoDuRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional(readOnly = true)
    public Page<ThueBao> timKiem(String tuKhoa, TrangThaiThueBao trangThai,
                                 LoaiThueBao loaiThueBao, Long goiCuocId, Pageable pageable) {
        String tuKhoaChuan = (tuKhoa == null || tuKhoa.isBlank()) ? null : tuKhoa.trim();
        return thueBaoRepository.timKiem(tuKhoaChuan, trangThai, loaiThueBao, goiCuocId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public ThueBao layTheoId(Long id) {
        return thueBaoRepository.timTheoIdKemQuanHe(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy thuê bao có mã số " + id));
    }

    // =================================================================
    // ĐĂNG KÝ THUÊ BAO MỚI
    // =================================================================
    @Override
    @Transactional
    public ThueBao dangKyMoi(ThueBaoForm form) {

        if (thueBaoRepository.existsBySoThueBao(form.getSoThueBao())) {
            throw new NghiepVuException("Số thuê bao " + form.getSoThueBao() + " đã tồn tại trong hệ thống");
        }
        if (form.getNgayKichHoat().isAfter(LocalDate.now())) {
            throw new NghiepVuException("Ngày kích hoạt không được ở tương lai");
        }

        KhachHang khachHang = khachHangRepository.findById(form.getKhachHangId())
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy khách hàng đã chọn"));
        if (khachHang.getTrangThai() == TrangThaiKhachHang.NGUNG_GIAO_DICH) {
            throw new NghiepVuException("Khách hàng " + khachHang.getMaKh()
                    + " đã ngừng giao dịch, không thể đăng ký thuê bao mới");
        }

        GoiCuoc goiCuoc = goiCuocRepository.findById(form.getGoiCuocId())
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy gói cước đã chọn"));

        // Gói trả trước và gói trả sau có cách tính cước khác hẳn nhau, không dùng lẫn được
        if (goiCuoc.getLoaiThueBao() != form.getLoaiThueBao()) {
            throw new NghiepVuException("Gói cước " + goiCuoc.getMaGoi() + " chỉ áp dụng cho thuê bao "
                    + goiCuoc.getLoaiThueBao().getNhan().toLowerCase());
        }

        ThueBao thueBao = new ThueBao();
        thueBao.setSoThueBao(form.getSoThueBao());
        thueBao.setKhachHang(khachHang);
        thueBao.setGoiCuoc(goiCuoc);
        thueBao.setLoaiThueBao(form.getLoaiThueBao());
        thueBao.setNgayKichHoat(form.getNgayKichHoat());
        thueBao.setTrangThai(TrangThaiThueBao.HOAT_DONG);
        thueBao.setSoDu(BigDecimal.ZERO);
        thueBao.setHanMucTinDung(form.getLoaiThueBao() == LoaiThueBao.TRA_SAU
                && form.getHanMucTinDung() != null ? form.getHanMucTinDung() : BigDecimal.ZERO);
        thueBao.setNgayTao(LocalDateTime.now());
        ThueBao daLuu = thueBaoRepository.save(thueBao);

        // Bản ghi đăng ký gói cước đang áp dụng
        DangKyGoiCuoc dangKy = new DangKyGoiCuoc();
        dangKy.setThueBao(daLuu);
        dangKy.setGoiCuoc(goiCuoc);
        dangKy.setNgayBatDau(form.getNgayKichHoat());
        dangKy.setTrangThai(TrangThaiDangKyGoi.DANG_AP_DUNG);
        dangKyGoiCuocRepository.save(dangKy);

        // Dòng lịch sử đầu tiên: chưa có trạng thái cũ nên để null
        ghiLichSu(daLuu, null, TrangThaiThueBao.HOAT_DONG, "Đăng ký thuê bao mới");

        nhatKyService.ghiNhatKy("DANG_KY_THUE_BAO", "THUE_BAO", daLuu.getId(),
                "Đăng ký thuê bao " + daLuu.getSoThueBao() + " cho khách hàng "
                        + khachHang.getMaKh() + ", gói " + goiCuoc.getMaGoi());

        return daLuu;
    }

    // =================================================================
    // CHUYỂN TRẠNG THÁI
    // =================================================================
    @Override
    @Transactional
    public void chuyenTrangThai(Long id, TrangThaiThueBao trangThaiMoi, String lyDo) {
        if (lyDo == null || lyDo.isBlank()) {
            throw new NghiepVuException("Vui lòng nhập lý do chuyển trạng thái");
        }

        ThueBao thueBao = layTheoId(id);
        TrangThaiThueBao trangThaiCu = thueBao.getTrangThai();

        if (trangThaiCu == trangThaiMoi) {
            throw new NghiepVuException("Thuê bao đang ở trạng thái "
                    + trangThaiCu.getNhan() + " rồi, không cần chuyển");
        }

        // Tra ma trận: chuyển không nằm trong danh sách hợp lệ thì từ chối
        Set<TrangThaiThueBao> duocPhep = CHUYEN_HOP_LE.getOrDefault(trangThaiCu, Set.of());
        if (!duocPhep.contains(trangThaiMoi)) {
            if (trangThaiCu == TrangThaiThueBao.DA_THANH_LY) {
                throw new NghiepVuException("Thuê bao " + thueBao.getSoThueBao()
                        + " đã thanh lý, không thể chuyển sang trạng thái khác");
            }
            throw new NghiepVuException("Không thể chuyển thuê bao từ trạng thái "
                    + trangThaiCu.getNhan() + " sang " + trangThaiMoi.getNhan());
        }

        thueBao.setTrangThai(trangThaiMoi);
        // Thanh lý là điểm kết thúc vòng đời nên phải ghi lại mốc thời gian
        if (trangThaiMoi == TrangThaiThueBao.DA_THANH_LY) {
            thueBao.setNgayHuy(LocalDate.now());
        }
        thueBaoRepository.save(thueBao);

        ghiLichSu(thueBao, trangThaiCu, trangThaiMoi, lyDo);

        nhatKyService.ghiNhatKy("CHUYEN_TRANG_THAI_THUE_BAO", "THUE_BAO", id,
                "Thuê bao " + thueBao.getSoThueBao() + ": " + trangThaiCu.getNhan()
                        + " -> " + trangThaiMoi.getNhan() + ". Lý do: " + lyDo);
    }

    // =================================================================
    // ĐỔI GÓI CƯỚC
    // =================================================================
    @Override
    @Transactional
    public void doiGoiCuoc(Long id, Long goiCuocMoiId) {
        ThueBao thueBao = layTheoId(id);

        if (thueBao.getTrangThai() == TrangThaiThueBao.DA_THANH_LY) {
            throw new NghiepVuException("Thuê bao đã thanh lý, không thể đổi gói cước");
        }

        GoiCuoc goiMoi = goiCuocRepository.findById(goiCuocMoiId)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy gói cước đã chọn"));

        if (goiMoi.getId().equals(thueBao.getGoiCuoc().getId())) {
            throw new NghiepVuException("Thuê bao đang dùng gói " + goiMoi.getTenGoi() + " rồi");
        }
        if (goiMoi.getLoaiThueBao() != thueBao.getLoaiThueBao()) {
            throw new NghiepVuException("Chỉ được đổi sang gói cùng loại "
                    + thueBao.getLoaiThueBao().getNhan().toLowerCase());
        }

        // Quy tắc viễn thông: gói cước tính theo chu kỳ tháng, đổi giữa chừng sẽ
        // khiến một tháng phải chia cước theo hai bảng giá khác nhau. Vì vậy gói
        // mới chỉ có hiệu lực từ ngày đầu tháng kế tiếp; tháng hiện tại vẫn tính
        // trọn theo gói cũ.
        LocalDate ngayHieuLuc = ngayHieuLucDoiGoi();
        GoiCuoc goiCu = thueBao.getGoiCuoc();

        dangKyGoiCuocRepository.findFirstByThueBaoIdAndTrangThai(id, TrangThaiDangKyGoi.DANG_AP_DUNG)
                .ifPresent(dangKyCu -> {
                    dangKyCu.setNgayKetThuc(ngayHieuLuc.minusDays(1));
                    dangKyCu.setTrangThai(TrangThaiDangKyGoi.DA_KET_THUC);
                    dangKyGoiCuocRepository.save(dangKyCu);
                });

        DangKyGoiCuoc dangKyMoi = new DangKyGoiCuoc();
        dangKyMoi.setThueBao(thueBao);
        dangKyMoi.setGoiCuoc(goiMoi);
        dangKyMoi.setNgayBatDau(ngayHieuLuc);
        dangKyMoi.setTrangThai(TrangThaiDangKyGoi.DANG_AP_DUNG);
        dangKyGoiCuocRepository.save(dangKyMoi);

        thueBao.setGoiCuoc(goiMoi);
        thueBaoRepository.save(thueBao);

        nhatKyService.ghiNhatKy("DOI_GOI_CUOC", "THUE_BAO", id,
                "Thuê bao " + thueBao.getSoThueBao() + " đổi gói " + goiCu.getMaGoi()
                        + " -> " + goiMoi.getMaGoi() + ", hiệu lực từ " + ngayHieuLuc);
    }

    @Override
    public LocalDate ngayHieuLucDoiGoi() {
        return LocalDate.now().plusMonths(1).withDayOfMonth(1);
    }

    // =================================================================
    // NẠP TIỀN
    // =================================================================
    @Override
    @Transactional
    public void napTien(Long id, BigDecimal soTien, HinhThucNapTien hinhThuc) {
        if (soTien == null || soTien.compareTo(BigDecimal.ZERO) <= 0) {
            throw new NghiepVuException("Số tiền nạp phải lớn hơn 0");
        }

        ThueBao thueBao = layTheoId(id);

        // Thuê bao trả sau dùng hạn mức tín dụng và trả cước qua hóa đơn hằng tháng,
        // không có khái niệm số dư tài khoản để nạp vào.
        if (thueBao.getLoaiThueBao() != LoaiThueBao.TRA_TRUOC) {
            throw new NghiepVuException("Chỉ nạp tiền được cho thuê bao trả trước");
        }
        if (thueBao.getTrangThai() == TrangThaiThueBao.DA_THANH_LY) {
            throw new NghiepVuException("Thuê bao đã thanh lý, không thể nạp tiền");
        }

        BigDecimal soDuTruoc = thueBao.getSoDu() == null ? BigDecimal.ZERO : thueBao.getSoDu();
        BigDecimal soDuSau = soDuTruoc.add(soTien);

        BienDongSoDu bienDong = new BienDongSoDu();
        bienDong.setThueBao(thueBao);
        bienDong.setLoaiBienDong(LoaiBienDongSoDu.NAP_TIEN);
        bienDong.setSoTien(soTien);
        // Lưu ảnh chụp số dư hai đầu để về sau đối soát được, không phải suy ngược
        bienDong.setSoDuTruoc(soDuTruoc);
        bienDong.setSoDuSau(soDuSau);
        bienDong.setHinhThuc(hinhThuc);
        bienDong.setNgayGhiNhan(LocalDateTime.now());
        SecurityUtils.layNguoiDungHienTai().ifPresent(p ->
                bienDong.setNguoiThucHien(nguoiDungRepository.getReferenceById(p.getId())));
        bienDongSoDuRepository.save(bienDong);

        thueBao.setSoDu(soDuSau);
        thueBaoRepository.save(thueBao);

        nhatKyService.ghiNhatKy("NAP_TIEN", "THUE_BAO", id,
                "Nạp " + soTien + " đồng cho thuê bao " + thueBao.getSoThueBao()
                        + ", số dư " + soDuTruoc + " -> " + soDuSau);
    }

    // =================================================================
    // TRUY VẤN PHỤC VỤ MÀN HÌNH CHI TIẾT
    // =================================================================
    @Override
    @Transactional(readOnly = true)
    public List<DangKyGoiCuoc> layLichSuGoiCuoc(Long thueBaoId) {
        return dangKyGoiCuocRepository.timLichSuTheoThueBao(thueBaoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LichSuThueBao> layLichSuTrangThai(Long thueBaoId) {
        return lichSuThueBaoRepository.timLichSuTheoThueBao(thueBaoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BienDongSoDu> layLichSuBienDongSoDu(Long thueBaoId) {
        return bienDongSoDuRepository.timLichSuTheoThueBao(thueBaoId);
    }

    @Override
    public List<TrangThaiThueBao> cacTrangThaiCoTheChuyen(TrangThaiThueBao trangThaiHienTai) {
        return CHUYEN_HOP_LE.getOrDefault(trangThaiHienTai, Set.of()).stream().toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoiCuoc> layGoiCuocConHieuLuc() {
        return goiCuocRepository.findByTrangThaiTrue();
    }

    /** Ghi một dòng lịch sử biến động trạng thái, gắn người đang thao tác. */
    private void ghiLichSu(ThueBao thueBao, TrangThaiThueBao cu, TrangThaiThueBao moi, String lyDo) {
        LichSuThueBao lichSu = new LichSuThueBao();
        lichSu.setThueBao(thueBao);
        lichSu.setTrangThaiCu(cu == null ? null : cu.name());
        lichSu.setTrangThaiMoi(moi.name());
        lichSu.setLyDo(lyDo);
        lichSu.setThoiGian(LocalDateTime.now());
        SecurityUtils.layNguoiDungHienTai().ifPresent(p ->
                lichSu.setNguoiThucHien(nguoiDungRepository.getReferenceById(p.getId())));
        lichSuThueBaoRepository.save(lichSu);
    }
}
