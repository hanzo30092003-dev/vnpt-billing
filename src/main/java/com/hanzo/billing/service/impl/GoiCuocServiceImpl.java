package com.hanzo.billing.service.impl;

import com.hanzo.billing.dto.GoiCuocDto;
import com.hanzo.billing.dto.GoiCuocForm;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.BangGiaCuocRepository;
import com.hanzo.billing.repository.GoiCuocRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.GoiCuocService;
import com.hanzo.billing.service.NhatKyService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoiCuocServiceImpl implements GoiCuocService {

    /** Ngưỡng quy đổi MB sang GB khi hiển thị tóm tắt ưu đãi. */
    private static final int MB_MOI_GB = 1024;

    private final GoiCuocRepository goiCuocRepository;
    private final ThueBaoRepository thueBaoRepository;
    private final BangGiaCuocRepository bangGiaCuocRepository;
    private final NhatKyService nhatKyService;

    @Override
    @Transactional(readOnly = true)
    public List<GoiCuocDto> layDanhSach() {
        // Lấy số thuê bao của MỌI gói trong một truy vấn rồi tra theo Map,
        // tránh gọi count riêng cho từng gói (N+1).
        Map<Long, Long> demTheoGoi = new HashMap<>();
        for (Object[] dong : thueBaoRepository.demThueBaoTheoGoiCuoc()) {
            demTheoGoi.put((Long) dong[0], (Long) dong[1]);
        }

        List<GoiCuocDto> ketQua = new ArrayList<>();
        for (GoiCuoc goiCuoc : goiCuocRepository.findAllByOrderByMaGoiAsc()) {
            ketQua.add(new GoiCuocDto(
                    goiCuoc,
                    demTheoGoi.getOrDefault(goiCuoc.getId(), 0L),
                    tomTatUuDai(goiCuoc)));
        }
        return ketQua;
    }

    @Override
    @Transactional(readOnly = true)
    public GoiCuoc layTheoId(Long id) {
        return goiCuocRepository.findById(id)
                .orElseThrow(() -> new NghiepVuException("Không tìm thấy gói cước có mã số " + id));
    }

    @Override
    @Transactional
    public GoiCuoc luu(GoiCuocForm form) {
        boolean themMoi = (form.getId() == null);
        String maGoi = form.getMaGoi().trim().toUpperCase();

        boolean trungMa = themMoi
                ? goiCuocRepository.existsByMaGoi(maGoi)
                : goiCuocRepository.existsByMaGoiAndIdNot(maGoi, form.getId());
        if (trungMa) {
            throw new NghiepVuException("Mã gói " + maGoi + " đã có rồi. Hãy đặt một mã khác, ví dụ thêm số thứ tự ở cuối.");
        }

        GoiCuoc goiCuoc = themMoi ? new GoiCuoc() : layTheoId(form.getId());

        // Đổi loại thuê bao của gói đang có người dùng sẽ làm lệch cách tính cước
        // của toàn bộ thuê bao đó, nên chặn lại.
        if (!themMoi && goiCuoc.getLoaiThueBao() != form.getLoaiThueBao()) {
            long soThueBao = thueBaoRepository.countByGoiCuocId(form.getId());
            if (soThueBao > 0) {
                throw new NghiepVuException("Gói cước đang có " + soThueBao
                        + " thuê bao dùng nên không đổi loại thuê bao của gói được. "
                        + "Hãy tạo một gói mới cho loại thuê bao kia thay vì sửa gói này.");
            }
        }

        goiCuoc.setMaGoi(maGoi);
        goiCuoc.setTenGoi(form.getTenGoi().trim());
        goiCuoc.setLoaiThueBao(form.getLoaiThueBao());
        goiCuoc.setCuocThueBaoThang(form.getCuocThueBaoThang());
        goiCuoc.setPhutNoiMangMienPhi(form.getPhutNoiMangMienPhi());
        goiCuoc.setPhutNgoaiMangMienPhi(form.getPhutNgoaiMangMienPhi());
        goiCuoc.setSmsMienPhi(form.getSmsMienPhi());
        goiCuoc.setDataMienPhiMb(form.getDataMienPhiMb());
        goiCuoc.setMoTa(form.getMoTa() == null || form.getMoTa().isBlank() ? null : form.getMoTa().trim());
        goiCuoc.setNgayHieuLuc(form.getNgayHieuLuc());
        goiCuoc.setNgayHetHieuLuc(form.getNgayHetHieuLuc());
        goiCuoc.setTrangThai(form.getTrangThai() != null && form.getTrangThai());

        GoiCuoc daLuu = goiCuocRepository.save(goiCuoc);

        nhatKyService.ghiNhatKy(themMoi ? "TAO_GOI_CUOC" : "SUA_GOI_CUOC", "GOI_CUOC",
                daLuu.getId(), (themMoi ? "Tạo mới gói cước " : "Cập nhật gói cước ")
                        + daLuu.getMaGoi() + " - " + daLuu.getTenGoi());

        return daLuu;
    }

    @Override
    @Transactional
    public void xoa(Long id) {
        GoiCuoc goiCuoc = layTheoId(id);

        // Xoá gói cước đang có thuê bao dùng sẽ làm mất khoá ngoại và không còn
        // căn cứ tính cước cho các kỳ đã qua. Trường hợp muốn ngừng bán thì đặt
        // ngày hết hiệu lực hoặc tắt trạng thái, không xoá bản ghi.
        long soThueBao = thueBaoRepository.countByGoiCuocId(id);
        if (soThueBao > 0) {
            throw new NghiepVuException("Gói cước đang có " + soThueBao
                    + " thuê bao dùng nên không xoá được — xoá đi thì các thuê bao đó mất căn cứ tính tiền. "
                    + "Muốn ngừng bán gói này thì mở gói ra và đặt Ngày hết hiệu lực.");
        }

        goiCuocRepository.delete(goiCuoc);
        nhatKyService.ghiNhatKy("XOA_GOI_CUOC", "GOI_CUOC", id,
                "Xoá gói cước " + goiCuoc.getMaGoi() + " - " + goiCuoc.getTenGoi());
    }

    @Override
    @Transactional(readOnly = true)
    public long demThueBaoDangDung(Long goiCuocId) {
        return thueBaoRepository.countByGoiCuocId(goiCuocId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BangGiaCuoc> layBangGiaRieng(Long goiCuocId) {
        return bangGiaCuocRepository.findByGoiCuocId(goiCuocId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ThueBao> layThueBaoDangDung(Long goiCuocId, Pageable pageable) {
        return thueBaoRepository.timTheoGoiCuoc(goiCuocId, pageable);
    }

    /**
     * Ghép chuỗi tóm tắt ưu đãi để danh sách nhìn là hiểu ngay gói có gì,
     * thay vì phải mở chi tiết mới thấy 4 con số rời rạc.
     * Ví dụ: "100 phút NM · 30 SMS · 2 GB".
     */
    @Override
    public String tomTatUuDai(GoiCuoc goiCuoc) {
        List<String> phan = new ArrayList<>();

        if (duong(goiCuoc.getPhutNoiMangMienPhi())) {
            phan.add(goiCuoc.getPhutNoiMangMienPhi() + " phút NM");
        }
        if (duong(goiCuoc.getPhutNgoaiMangMienPhi())) {
            phan.add(goiCuoc.getPhutNgoaiMangMienPhi() + " phút NgM");
        }
        if (duong(goiCuoc.getSmsMienPhi())) {
            phan.add(goiCuoc.getSmsMienPhi() + " SMS");
        }
        if (duong(goiCuoc.getDataMienPhiMb())) {
            int mb = goiCuoc.getDataMienPhiMb();
            // Quy đổi sang GB cho gọn khi là bội số tròn của 1024
            phan.add(mb % MB_MOI_GB == 0 ? (mb / MB_MOI_GB) + " GB" : mb + " MB");
        }

        return phan.isEmpty() ? "Không có ưu đãi" : String.join(" · ", phan);
    }

    private boolean duong(Integer giaTri) {
        return giaTri != null && giaTri > 0;
    }
}
