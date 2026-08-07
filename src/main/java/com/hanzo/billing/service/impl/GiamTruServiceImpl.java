package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.GiamTruForm;
import com.hanzo.billing.entity.GiamTru;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiGiamTru;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.GiamTruRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.GiamTruService;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Quản lý khoản giảm trừ.
 *
 * <h2>Ràng buộc ① của Phase 5 nằm ở đâu</h2>
 * <p>Lớp này <b>chỉ lưu ý định</b> giảm trừ — số tiền tuyệt đối hoặc tỷ lệ. Việc quy tỷ lệ
 * thành tiền do {@code BillingService.tinhGiamTru} làm <b>đúng một lần</b> tại thời điểm lập
 * hóa đơn, ghi kết quả vào {@code hoa_don.giam_tru} và snapshot thành một dòng
 * {@code chi_tiet_hoa_don}. Từ đó về sau mọi nơi chỉ cộng trừ trên con số đã chốt, không
 * nhân lại — nên không sinh ra tầng làm tròn thứ hai.</p>
 *
 * <p>Vì vậy ở đây <b>không</b> có phép nhân nào với {@code tyLePhanTram}. Thêm một phép quy
 * đổi ở tầng này chính là tạo ra tầng làm tròn thứ hai mà ràng buộc ① cấm.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GiamTruServiceImpl implements GiamTruService {

    private final GiamTruRepository giamTruRepository;
    private final ThueBaoRepository thueBaoRepository;
    private final KyCuocRepository kyCuocRepository;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional(readOnly = true)
    public List<GiamTru> timKiem(Long kyCuocId, Long thueBaoId, TrangThaiGiamTru trangThai) {
        return giamTruRepository.timKiem(kyCuocId, thueBaoId, trangThai);
    }

    @Override
    @Transactional(readOnly = true)
    public GiamTru layTheoId(Long id) {
        return giamTruRepository.findById(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy khoản giảm trừ id " + id));
    }

    @Override
    @Transactional
    public GiamTru luu(GiamTruForm form) {
        GiamTru gt = form.getId() == null ? new GiamTru() : layTheoId(form.getId());

        if (form.getId() != null) {
            kiemTraConSuaDuoc(gt);
        }

        KyCuoc ky = null;
        if (form.getKyCuocId() != null) {
            ky = kyCuocRepository.findById(form.getKyCuocId())
                    .orElseThrow(() -> new NghiepVuException(
                            "Không tìm thấy kỳ cước id " + form.getKyCuocId()));
            if (ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
                throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                        + " đã chốt, không thêm hay sửa khoản giảm trừ được nữa.");
            }
        }

        gt.setThueBao(thueBaoRepository.findById(form.getThueBaoId())
                .orElseThrow(() -> new NghiepVuException(
                        "Không tìm thấy thuê bao id " + form.getThueBaoId())));
        gt.setKyCuoc(ky);
        gt.setLoai(form.getLoai());
        // Chỉ một trong hai có giá trị — form đã chặn bằng @AssertTrue, ở đây ghi rõ ràng
        // để bản ghi không bao giờ mang cả hai cách khai
        boolean theoSoTien = form.getSoTien() != null && form.getSoTien().signum() > 0;
        gt.setSoTien(theoSoTien ? form.getSoTien() : null);
        gt.setTyLePhanTram(theoSoTien ? null : form.getTyLePhanTram());
        gt.setLyDo(form.getLyDo());
        if (gt.getTrangThai() == null) {
            gt.setTrangThai(TrangThaiGiamTru.CHUA_AP_DUNG);
        }

        giamTruRepository.save(gt);
        log.info("Lưu khoản giảm trừ id={} thuê bao={} {} ", gt.getId(),
                gt.getThueBao().getSoThueBao(), moTaMuc(gt));
        nhatKyService.ghiNhatKy(form.getId() == null ? "THEM_GIAM_TRU" : "SUA_GIAM_TRU",
                "GIAM_TRU", gt.getId(),
                "Giảm trừ cho thuê bao " + gt.getThueBao().getSoThueBao() + ": " + moTaMuc(gt));
        return gt;
    }

    @Override
    @Transactional
    public void xoa(Long id) {
        GiamTru gt = layTheoId(id);
        kiemTraConSuaDuoc(gt);
        giamTruRepository.delete(gt);

        nhatKyService.ghiNhatKy("XOA_GIAM_TRU", "GIAM_TRU", id,
                "Xoá khoản giảm trừ của thuê bao " + gt.getThueBao().getSoThueBao()
                        + ": " + moTaMuc(gt));
    }

    /**
     * Chỉ sửa/xoá được khi khoản còn {@code CHUA_AP_DUNG} và kỳ chưa chốt.
     *
     * <p>Khoản đã áp dụng nghĩa là số tiền của nó đã nằm trong {@code hoa_don.giam_tru} và
     * trong một dòng {@code chi_tiet_hoa_don}. Sửa nó lúc này sẽ làm hóa đơn không còn giải
     * thích được — muốn đổi thì phải huỷ hóa đơn của kỳ trước, khi đó khoản tự quay về
     * {@code CHUA_AP_DUNG}.</p>
     */
    private void kiemTraConSuaDuoc(GiamTru gt) {
        if (gt.getTrangThai() == TrangThaiGiamTru.DA_AP_DUNG) {
            throw new NghiepVuException("Khoản giảm trừ này đã được áp vào hóa đơn. "
                    + "Phải huỷ hóa đơn của kỳ trước thì khoản mới quay về trạng thái "
                    + "Chưa áp dụng và sửa được.");
        }
        if (gt.getKyCuoc() != null && gt.getKyCuoc().getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
            throw new NghiepVuException("Kỳ cước tháng " + gt.getKyCuoc().getThang() + "/"
                    + gt.getKyCuoc().getNam() + " đã chốt, không sửa khoản giảm trừ được nữa.");
        }
    }

    private static String moTaMuc(GiamTru gt) {
        if (gt.getSoTien() != null && gt.getSoTien().compareTo(BigDecimal.ZERO) > 0) {
            return gt.getSoTien() + " đ";
        }
        return gt.getTyLePhanTram() + "%";
    }
}
