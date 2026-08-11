package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.KyCuocForm;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.service.KyCuocService;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KyCuocServiceImpl implements KyCuocService {

    private final KyCuocRepository kyCuocRepository;
    private final HoaDonRepository hoaDonRepository;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional(readOnly = true)
    public List<KyCuoc> layTatCa() {
        return kyCuocRepository.findAll(
                Sort.by(Sort.Direction.DESC, "nam").and(Sort.by(Sort.Direction.DESC, "thang")));
    }

    @Override
    @Transactional(readOnly = true)
    public KyCuoc layTheoId(Long id) {
        return kyCuocRepository.findById(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy kỳ cước có mã số " + id));
    }

    @Override
    @Transactional
    public KyCuoc taoMoi(KyCuocForm form) {
        // CSDL đã có ràng buộc UNIQUE(thang, nam), nhưng vẫn phải chặn ở đây trước.
        // Nếu để CSDL bắt thì người dùng nhận về lỗi kỹ thuật khó hiểu
        // (DataIntegrityViolationException) thay vì thông báo tiếng Việt rõ nghĩa.
        kyCuocRepository.findByThangAndNam(form.getThang(), form.getNam())
                .ifPresent(daCo -> {
                    throw new NghiepVuException("Tháng tính tiền " + form.getThang() + "/"
                            + form.getNam() + " đã có trong danh sách bên phải, không tạo lại được. "
                            + "Hãy chọn tháng khác, hoặc dùng luôn tháng đã có.");
                });

        // Ngày đầu và ngày cuối tháng tính bằng YearMonth để tự xử lý tháng 28/29/30/31 ngày
        YearMonth thangNam = YearMonth.of(form.getNam(), form.getThang());
        LocalDate ngayBatDau = thangNam.atDay(1);
        LocalDate ngayKetThuc = thangNam.atEndOfMonth();

        KyCuoc kyCuoc = new KyCuoc();
        kyCuoc.setThang(form.getThang());
        kyCuoc.setNam(form.getNam());
        kyCuoc.setNgayBatDau(ngayBatDau);
        kyCuoc.setNgayKetThuc(ngayKetThuc);
        kyCuoc.setTrangThai(TrangThaiKyCuoc.MO);
        kyCuoc.setSoCdrXuLy(0);
        kyCuoc.setSoHoaDonTao(0);
        kyCuoc.setTongDoanhThu(BigDecimal.ZERO);

        KyCuoc daLuu = kyCuocRepository.save(kyCuoc);

        nhatKyService.ghiNhatKy("TAO_KY_CUOC", "KY_CUOC", daLuu.getId(),
                "Tạo kỳ cước tháng " + daLuu.getThang() + "/" + daLuu.getNam()
                        + " (" + ngayBatDau + " đến " + ngayKetThuc + ")");

        return daLuu;
    }

    /**
     * Chốt kỳ cước — thao tác <b>một chiều</b>, không có đường quay lại.
     *
     * <p>Sau khi chốt, mọi thao tác tính cước, lập hóa đơn và hủy đều bị từ chối. Đây là
     * chốt chặn cuối để số liệu của một kỳ đã quyết toán không thể đổi nữa.</p>
     *
     * <p>Bắt buộc kỳ phải <b>đã có hóa đơn</b>: chốt một kỳ chưa lập hóa đơn nào sẽ khóa
     * vĩnh viễn một kỳ trống, và không có cách nào sửa ngoài can thiệp thẳng vào CSDL.</p>
     */
    @Override
    @Transactional
    public KyCuoc chotKy(Long id) {
        KyCuoc ky = layTheoId(id);

        if (ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
            throw new NghiepVuException("Tháng tính tiền " + ky.getThang() + "/" + ky.getNam()
                    + " đã khóa trước đó rồi, không cần khóa lại.");
        }
        if (ky.getTrangThai() == TrangThaiKyCuoc.DANG_TINH) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đang ở trạng thái Đang tính, không thể chốt. Nếu lần chạy trước bị "
                    + "dừng giữa chừng, dùng chức năng \"Gỡ kỳ bị kẹt\" trước.");
        }
        long soHoaDon = hoaDonRepository.countByKyCuocId(id);
        if (soHoaDon == 0) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " chưa có hóa đơn nào. Chốt một kỳ trống sẽ khóa vĩnh viễn kỳ đó — "
                    + "phải chạy tính cước và lập hóa đơn trước khi chốt.");
        }

        ky.setTrangThai(TrangThaiKyCuoc.DA_CHOT);
        ky.setNgayChot(LocalDateTime.now());
        KyCuoc daLuu = kyCuocRepository.save(ky);

        nhatKyService.ghiNhatKy("CHOT_KY_CUOC", "KY_CUOC", id,
                "Chốt kỳ cước tháng " + ky.getThang() + "/" + ky.getNam() + " với "
                        + soHoaDon + " hóa đơn, tổng doanh thu " + ky.getTongDoanhThu()
                        + " đ. Từ nay kỳ này không thể tính lại hay sửa đổi.");
        return daLuu;
    }
}
