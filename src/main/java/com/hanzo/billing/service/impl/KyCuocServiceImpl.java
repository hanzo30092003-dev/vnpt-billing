package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.KyCuocForm;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.service.KyCuocService;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KyCuocServiceImpl implements KyCuocService {

    private final KyCuocRepository kyCuocRepository;
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
                    throw new NghiepVuException("Kỳ cước tháng " + form.getThang() + "/"
                            + form.getNam() + " đã tồn tại, không thể tạo trùng");
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
}
