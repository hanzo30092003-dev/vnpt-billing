package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.BangGiaForm;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.BangGiaCuocRepository;
import com.hanzo.billing.repository.GoiCuocRepository;
import com.hanzo.billing.service.BangGiaCuocService;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BangGiaCuocServiceImpl implements BangGiaCuocService {

    private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Id giả dùng khi thêm mới, để câu truy vấn "bỏ qua chính nó" luôn có tham số. */
    private static final long KHONG_BO_QUA_AI = -1L;

    private final BangGiaCuocRepository bangGiaCuocRepository;
    private final GoiCuocRepository goiCuocRepository;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional(readOnly = true)
    public List<BangGiaCuoc> timCoLoc(boolean locTheoGoi, Long goiCuocId,
                                      LoaiDichVu loaiDichVu, HuongCuocGoi huong,
                                      boolean chiConHieuLuc) {
        return bangGiaCuocRepository.timCoLoc(locTheoGoi, goiCuocId, loaiDichVu, huong,
                chiConHieuLuc, LocalDate.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BangGiaCuoc layTheoId(Long id) {
        return bangGiaCuocRepository.findById(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy dòng bảng giá có mã số " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BangGiaCuoc> traCuuTheoNgay(LocalDate ngay) {
        return bangGiaCuocRepository.timCoHieuLucTaiNgay(ngay);
    }

    // =================================================================
    // KIỂM TRA CHỒNG KHOẢNG HIỆU LỰC
    // =================================================================

    /**
     * Hai khoảng đóng [batDau1, ketThuc1] và [batDau2, ketThuc2] KHÔNG chồng nhau
     * khi và chỉ khi một khoảng kết thúc hẳn trước khi khoảng kia bắt đầu.
     *
     * <p>Ngày kết thúc null nghĩa là vô thời hạn, tức khoảng đó không bao giờ
     * "kết thúc trước" khoảng nào cả.</p>
     *
     * <p>Viết theo hướng phủ định như vậy gọn và ít sai hơn là liệt kê từng kiểu
     * chồng (trùng khít, chồng đầu, chồng cuối, lồng nhau) rồi ghép lại bằng OR.</p>
     */
    @Override
    public boolean chongKhoangHieuLuc(LocalDate batDau1, LocalDate ketThuc1,
                                      LocalDate batDau2, LocalDate ketThuc2) {
        boolean mot_ketThucTruocHai = ketThuc1 != null && ketThuc1.isBefore(batDau2);
        boolean hai_ketThucTruocMot = ketThuc2 != null && ketThuc2.isBefore(batDau1);
        return !(mot_ketThucTruocHai || hai_ketThucTruocMot);
    }

    // =================================================================
    // LƯU
    // =================================================================
    @Override
    @Transactional
    public BangGiaCuoc luu(BangGiaForm form) {
        boolean themMoi = (form.getId() == null);
        Long idBoQua = themMoi ? KHONG_BO_QUA_AI : form.getId();

        // Tìm các dòng CÙNG BỘ KHOÁ (gói, dịch vụ, hướng, giờ cao điểm) rồi soi
        // từng dòng xem khoảng hiệu lực có đè lên nhau không. Nếu để lọt, engine
        // tính cước sẽ gặp hai đơn giá cùng áp dụng cho một thời điểm.
        List<BangGiaCuoc> cungBoKhoa = bangGiaCuocRepository.timCungBoKhoa(
                form.getGoiCuocId(), form.getLoaiDichVu(), form.getHuong(),
                form.getGioCaoDiem(), idBoQua);

        for (BangGiaCuoc daCo : cungBoKhoa) {
            if (chongKhoangHieuLuc(form.getNgayHieuLuc(), form.getNgayHetHieuLuc(),
                    daCo.getNgayHieuLuc(), daCo.getNgayHetHieuLuc())) {
                throw new NghiepVuException(
                        "Khoảng hiệu lực bị chồng với dòng bảng giá đã có (mã số " + daCo.getId()
                                + "): " + moTaKhoang(daCo.getNgayHieuLuc(), daCo.getNgayHetHieuLuc())
                                + ", đơn giá " + daCo.getDonGia()
                                + ". Hai dòng cùng dịch vụ, hướng và khung giờ không được có"
                                + " thời gian hiệu lực đè lên nhau.");
            }
        }

        BangGiaCuoc bangGia = themMoi ? new BangGiaCuoc() : layTheoId(form.getId());

        GoiCuoc goiCuoc = null;
        if (form.getGoiCuocId() != null) {
            goiCuoc = goiCuocRepository.findById(form.getGoiCuocId())
                    .orElseThrow(() -> new NghiepVuException("Không tìm thấy gói cước đã chọn"));
        }
        bangGia.setGoiCuoc(goiCuoc);
        bangGia.setLoaiDichVu(form.getLoaiDichVu());
        bangGia.setHuong(form.getHuong());
        bangGia.setGioCaoDiem(form.getGioCaoDiem());
        bangGia.setBlockGiay(form.getBlockGiay());
        bangGia.setDonGia(form.getDonGia());
        bangGia.setNgayHieuLuc(form.getNgayHieuLuc());
        bangGia.setNgayHetHieuLuc(form.getNgayHetHieuLuc());

        BangGiaCuoc daLuu = bangGiaCuocRepository.save(bangGia);

        nhatKyService.ghiNhatKy(themMoi ? "TAO_BANG_GIA" : "SUA_BANG_GIA", "BANG_GIA_CUOC",
                daLuu.getId(), (themMoi ? "Tạo mới " : "Cập nhật ") + "dòng bảng giá "
                        + form.getLoaiDichVu() + "/" + form.getHuong()
                        + (Boolean.TRUE.equals(form.getGioCaoDiem()) ? " giờ cao điểm" : "")
                        + ", đơn giá " + form.getDonGia());

        return daLuu;
    }

    @Override
    @Transactional
    public void xoa(Long id) {
        BangGiaCuoc bangGia = layTheoId(id);
        bangGiaCuocRepository.delete(bangGia);
        nhatKyService.ghiNhatKy("XOA_BANG_GIA", "BANG_GIA_CUOC", id,
                "Xoá dòng bảng giá " + bangGia.getLoaiDichVu() + "/" + bangGia.getHuong());
    }

    private String moTaKhoang(LocalDate tu, LocalDate den) {
        return "từ " + tu.format(DINH_DANG_NGAY)
                + (den == null ? " (vô thời hạn)" : " đến " + den.format(DINH_DANG_NGAY));
    }
}
