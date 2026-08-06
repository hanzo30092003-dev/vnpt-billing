package com.hanzo.billing.service.rating;

import com.hanzo.billing.entity.BienDongSoDu;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiBienDongSoDu;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.BienDongSoDuRepository;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.NguoiDungRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.NhatKyService;
import com.hanzo.billing.util.SecurityUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Trừ cước kỳ vào số dư thuê bao trả trước — mục G2 của Phase 5.
 *
 * <h2>Phạm vi</h2>
 * <p>Trừ <b>theo kỳ</b> (batch), <b>không</b> real-time. Tính cước thời gian thực — trừ ngay
 * lúc phát sinh cuộc gọi và chặn dịch vụ khi hết tiền — vẫn nằm ở phần "Hướng phát triển".</p>
 *
 * <p>Thuê bao trả trước <b>không lập hóa đơn</b> (quyết định 5.4 của Phase 4 giữ nguyên).
 * Engine tính cước đã ghi {@code cuoc_phi} lên từng CDR của họ từ mục 4B; lớp này chỉ làm nốt
 * việc chuyển số tiền đó ra khỏi số dư và <b>ghi vết vào sổ cái</b>.</p>
 *
 * <h2>Quy tắc không cắt đôi bản ghi</h2>
 * <p>Số dư là một quỹ cạn dần, lặp lại <b>nguyên</b> tình huống quyết định 5.3: bản ghi làm
 * cạn quỹ <b>không được cắt đôi</b> thành "phần trả được" và "phần nợ", vì CDR chỉ có một cột
 * {@code cuoc_phi} và không có cột nào lưu phần vượt.</p>
 *
 * <p>Gặp bản ghi không đủ quỹ thì <b>DỪNG HẲN</b> — không phải bỏ qua rồi đi tiếp. Hai cách
 * cho kết quả khác nhau, và khác đáng kể: trên dữ liệu thật, thuê bao {@code 0944567804} dừng
 * ở bản ghi 270 đ trong khi quỹ còn 220 đ, mà hai bản ghi ngay sau đó chỉ 99 đ và 210 đ —
 * "bỏ qua rồi đi tiếp" sẽ trừ thêm cả hai. Chi tiết ở {@code docs/PHASE-5-REPORT.md} mục 10.3.
 *
 * <p>Hệ quả trực tiếp: kết quả <b>phụ thuộc thứ tự</b>, nên truy vấn bắt buộc phải có
 * {@code ORDER BY thoi_gian_bat_dau, id} cố định. Chạy hai lần trên cùng dữ liệu phải ra cùng
 * kết quả.</p>
 *
 * <h2>Trừ cước không giao hoán theo kỳ</h2>
 * <p>Số dư vào mỗi kỳ phụ thuộc kỳ trước đó, nên chạy kỳ 5 <i>sau khi</i> đã chạy kỳ 6 cho ra
 * kết quả khác hẳn chạy đúng thứ tự thời gian. Lớp này chặn hẳn ở {@link #kiemTraTruocKhiChay}
 * thay vì để người dùng tự nhớ.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TruCuocTraTruocService {

    private final ChiTietSuDungRepository chiTietSuDungRepository;
    private final BienDongSoDuRepository bienDongSoDuRepository;
    private final ThueBaoRepository thueBaoRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final NhatKyService nhatKyService;

    /** Kết quả một lần chạy trừ cước cả kỳ. */
    @Getter
    @Setter
    public static class KetQuaTruCuoc {
        private int soThueBaoXuLy;
        private int soThueBaoHetSoDu;
        private int soBanGhiDaTru;
        private BigDecimal tongDaTru = BigDecimal.ZERO;
        private BigDecimal tongKhongThuDuoc = BigDecimal.ZERO;
        private long thoiGianMs;
    }

    // =================================================================
    // A. CHẠY TRỪ CƯỚC CẢ KỲ
    // =================================================================

    @Transactional
    public KetQuaTruCuoc truCuocKy(KyCuoc ky) {
        long batDau = System.currentTimeMillis();
        kiemTraTruocKhiChay(ky);

        List<ChiTietSuDung> tatCa =
                chiTietSuDungRepository.timDaTinhTheoKyChoTraTruoc(ky.getId());

        // Gom theo thuê bao, GIỮ NGUYÊN thứ tự truy vấn đã sắp. LinkedHashMap chứ không
        // HashMap: thứ tự duyệt quyết định bản ghi nào được trừ, nên không được để cấu
        // trúc dữ liệu xáo nó đi.
        Map<Long, List<ChiTietSuDung>> theoThueBao = new LinkedHashMap<>();
        for (ChiTietSuDung cdr : tatCa) {
            theoThueBao.computeIfAbsent(cdr.getThueBao().getId(), k -> new ArrayList<>())
                    .add(cdr);
        }

        KetQuaTruCuoc ketQua = new KetQuaTruCuoc();
        LocalDateTime bayGio = LocalDateTime.now();

        for (List<ChiTietSuDung> cdrCuaThueBao : theoThueBao.values()) {
            truChoMotThueBao(cdrCuaThueBao, ky, bayGio, ketQua);
        }

        ketQua.setThoiGianMs(System.currentTimeMillis() - batDau);
        log.info("Đã trừ cước kỳ {}/{} cho {} thuê bao trả trước: trừ {} đ qua {} bản ghi, "
                        + "{} thuê bao hết số dư giữa chừng, không thu được {} đ ({} ms)",
                ky.getThang(), ky.getNam(), ketQua.getSoThueBaoXuLy(), ketQua.getTongDaTru(),
                ketQua.getSoBanGhiDaTru(), ketQua.getSoThueBaoHetSoDu(),
                ketQua.getTongKhongThuDuoc(), ketQua.getThoiGianMs());
        nhatKyService.ghiNhatKy("TRU_CUOC_TRA_TRUOC", "KY_CUOC", ky.getId(),
                "Trừ cước kỳ " + ky.getThang() + "/" + ky.getNam() + " vào số dư "
                        + ketQua.getSoThueBaoXuLy() + " thuê bao trả trước: trừ "
                        + ketQua.getTongDaTru() + " đ, không thu được "
                        + ketQua.getTongKhongThuDuoc() + " đ");
        return ketQua;
    }

    /**
     * Duyệt CDR của <b>một</b> thuê bao và ghi đúng <b>một</b> dòng sổ cái.
     *
     * <p>Một dòng cho cả kỳ chứ không phải một dòng mỗi CDR: sổ cái ghi lại <i>biến động số
     * dư</i>, và biến động ở đây là một lần trừ cước kỳ. Ghi 76 dòng cho 76 bản ghi sẽ làm
     * sổ cái phình lên mà không nói thêm được gì — đường đi chi tiết tới từng bản ghi đã có
     * sẵn ở bảng đối soát cước của mục 4E.</p>
     */
    private void truChoMotThueBao(List<ChiTietSuDung> cdrCuaThueBao, KyCuoc ky,
                                  LocalDateTime bayGio, KetQuaTruCuoc ketQua) {
        ThueBao thueBao = cdrCuaThueBao.get(0).getThueBao();
        BigDecimal soDuTruoc = thueBao.getSoDu() == null ? BigDecimal.ZERO : thueBao.getSoDu();

        BigDecimal quy = soDuTruoc;
        BigDecimal daTru = BigDecimal.ZERO;
        BigDecimal tongCuocCaKy = BigDecimal.ZERO;
        int soBanGhiDaTru = 0;
        boolean hetSoDu = false;

        for (ChiTietSuDung cdr : cdrCuaThueBao) {
            BigDecimal cuocPhi = cdr.getCuocPhi() == null ? BigDecimal.ZERO : cdr.getCuocPhi();
            tongCuocCaKy = tongCuocCaKy.add(cuocPhi);

            if (hetSoDu) {
                continue;   // chỉ cộng nốt tổng cước để báo cáo phần không thu được
            }
            if (cuocPhi.compareTo(quy) > 0) {
                // DỪNG HẲN, không bỏ qua rồi đi tiếp — xem javadoc lớp
                hetSoDu = true;
                continue;
            }
            quy = quy.subtract(cuocPhi);
            daTru = daTru.add(cuocPhi);
            soBanGhiDaTru++;
        }

        ketQua.setSoThueBaoXuLy(ketQua.getSoThueBaoXuLy() + 1);
        ketQua.setSoBanGhiDaTru(ketQua.getSoBanGhiDaTru() + soBanGhiDaTru);
        ketQua.setTongDaTru(ketQua.getTongDaTru().add(daTru));
        ketQua.setTongKhongThuDuoc(
                ketQua.getTongKhongThuDuoc().add(tongCuocCaKy.subtract(daTru)));
        if (hetSoDu) {
            ketQua.setSoThueBaoHetSoDu(ketQua.getSoThueBaoHetSoDu() + 1);
        }

        // Thuê bao không phát sinh cước nào (mọi bản ghi đều miễn phí) thì không ghi sổ:
        // một dòng 0 đồng không phải là biến động số dư.
        if (daTru.compareTo(BigDecimal.ZERO) == 0) {
            return;
        }

        BigDecimal soDuSau = soDuTruoc.subtract(daTru);

        BienDongSoDu bienDong = new BienDongSoDu();
        bienDong.setThueBao(thueBao);
        bienDong.setLoaiBienDong(LoaiBienDongSoDu.TRU_CUOC);
        bienDong.setSoTien(daTru);              // luôn dương, chiều do loại quyết định
        bienDong.setSoDuTruoc(soDuTruoc);
        bienDong.setSoDuSau(soDuSau);
        bienDong.setKyCuoc(ky);
        bienDong.setNgayGhiNhan(bayGio);
        SecurityUtils.layNguoiDungHienTai().ifPresent(p ->
                bienDong.setNguoiThucHien(nguoiDungRepository.getReferenceById(p.getId())));
        bienDongSoDuRepository.save(bienDong);

        thueBao.setSoDu(soDuSau);
        thueBaoRepository.save(thueBao);
    }

    /**
     * Ba chốt chặn trước khi chạy, đặt ở tầng nghiệp vụ chứ không trông vào giao diện.
     *
     * <p>Theo đúng bài học 4.4 của Phase 3 — chặn ở tầng nghiệp vụ kể cả khi giao diện đã
     * ẩn nút, vì gõ thẳng đường dẫn vẫn tới được.</p>
     */
    private void kiemTraTruocKhiChay(KyCuoc ky) {
        long daTru = bienDongSoDuRepository.countByKyCuocIdAndLoaiBienDong(
                ky.getId(), LoaiBienDongSoDu.TRU_CUOC);
        if (daTru > 0) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã trừ cước vào số dư (" + daTru + " thuê bao). Chạy lại sẽ trừ "
                    + "chồng. Phải hủy kết quả trừ cước trước nếu muốn chạy lại.");
        }

        long kySauHon = bienDongSoDuRepository.demTruCuocCuaKyMuonHon(
                ky.getNam(), ky.getThang());
        if (kySauHon > 0) {
            throw new NghiepVuException("Đã trừ cước cho kỳ muộn hơn kỳ "
                    + ky.getThang() + "/" + ky.getNam() + ". Trừ cước là thao tác có thứ tự "
                    + "theo thời gian: số dư vào mỗi kỳ phụ thuộc kỳ trước đó, nên chạy "
                    + "ngược thứ tự sẽ cho kết quả khác. Phải hủy kết quả trừ cước của các "
                    + "kỳ muộn hơn trước.");
        }
    }

    // =================================================================
    // B. HỦY KẾT QUẢ TRỪ CƯỚC — đối xứng với BillingService.huyBillingKy
    // =================================================================

    /**
     * Trả số dư về trạng thái trước khi trừ cước kỳ này, rồi xóa các dòng sổ cái tương ứng.
     *
     * <p><b>Cộng trả lại số tiền</b> chứ không gán thẳng {@code so_du = so_du_truoc}. Hai cách
     * cho cùng kết quả khi không có biến động nào xen giữa, nhưng nếu khách đã nạp tiền sau
     * lần trừ thì gán thẳng sẽ <b>xóa mất khoản nạp đó</b>. Cộng trả lại luôn giữ đúng bất
     * biến sổ cái {@code so_du = SUM(nạp) − SUM(trừ)} trong mọi trường hợp.</p>
     *
     * @return số thuê bao đã hoàn trả
     */
    @Transactional
    public int huyTruCuocKy(KyCuoc ky) {
        if (ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT) {
            throw new NghiepVuException("Kỳ cước tháng " + ky.getThang() + "/" + ky.getNam()
                    + " đã chốt, không thể hủy kết quả trừ cước");
        }

        long kySauHon = bienDongSoDuRepository.demTruCuocCuaKyMuonHon(
                ky.getNam(), ky.getThang());
        if (kySauHon > 0) {
            throw new NghiepVuException("Đã trừ cước cho kỳ muộn hơn kỳ "
                    + ky.getThang() + "/" + ky.getNam() + ". Hủy kỳ này trước sẽ làm số dư "
                    + "của các kỳ sau không còn giải thích được. Phải hủy từ kỳ muộn nhất "
                    + "trở về.");
        }

        List<BienDongSoDu> danhSach = bienDongSoDuRepository
                .findByKyCuocIdAndLoaiBienDong(ky.getId(), LoaiBienDongSoDu.TRU_CUOC);

        BigDecimal tongHoanTra = BigDecimal.ZERO;
        for (BienDongSoDu bienDong : danhSach) {
            ThueBao thueBao = bienDong.getThueBao();
            BigDecimal soDu = thueBao.getSoDu() == null ? BigDecimal.ZERO : thueBao.getSoDu();
            thueBao.setSoDu(soDu.add(bienDong.getSoTien()));
            thueBaoRepository.save(thueBao);
            tongHoanTra = tongHoanTra.add(bienDong.getSoTien());
        }
        bienDongSoDuRepository.deleteAll(danhSach);

        log.info("Đã hủy trừ cước kỳ {}/{}: hoàn trả {} đ cho {} thuê bao",
                ky.getThang(), ky.getNam(), tongHoanTra, danhSach.size());
        nhatKyService.ghiNhatKy("HUY_TRU_CUOC_TRA_TRUOC", "KY_CUOC", ky.getId(),
                "Hủy trừ cước kỳ " + ky.getThang() + "/" + ky.getNam() + ": hoàn trả "
                        + tongHoanTra + " đ cho " + danhSach.size() + " thuê bao");
        return danhSach.size();
    }
}
