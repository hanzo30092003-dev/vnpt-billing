package com.hanzo.billing.service.rating;

import com.hanzo.billing.entity.BienDongSoDu;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiBienDongSoDu;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.BienDongSoDuRepository;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.NguoiDungRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.NhatKyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Trừ cước vào số dư thuê bao trả trước")
class TruCuocTraTruocServiceTest {

    @Mock private ChiTietSuDungRepository chiTietSuDungRepository;
    @Mock private BienDongSoDuRepository bienDongSoDuRepository;
    @Mock private ThueBaoRepository thueBaoRepository;
    @Mock private NguoiDungRepository nguoiDungRepository;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks private TruCuocTraTruocService service;

    private static KyCuoc ky(long id, int thang, int nam) {
        KyCuoc k = new KyCuoc();
        k.setId(id);
        k.setThang(thang);
        k.setNam(nam);
        k.setTrangThai(TrangThaiKyCuoc.MO);
        return k;
    }

    private static ThueBao thueBao(long id, String soDu) {
        ThueBao tb = new ThueBao();
        tb.setId(id);
        tb.setSoThueBao("090000000" + id);
        tb.setLoaiThueBao(LoaiThueBao.TRA_TRUOC);
        tb.setSoDu(new BigDecimal(soDu));
        return tb;
    }

    /** CDR với cước cho trước; thời gian tăng dần theo thứ tự gọi để giữ ý nghĩa "theo thời gian". */
    private static ChiTietSuDung cdr(ThueBao tb, long id, String cuocPhi, int phutThu) {
        ChiTietSuDung c = new ChiTietSuDung();
        c.setId(id);
        c.setThueBao(tb);
        c.setCuocPhi(new BigDecimal(cuocPhi));
        c.setThoiGianBatDau(LocalDateTime.of(2026, 6, 1, 0, 0).plusMinutes(phutThu));
        return c;
    }

    private void khongVuongChotChan() {
        when(bienDongSoDuRepository.countByKyCuocIdAndLoaiBienDong(anyLong(), any()))
                .thenReturn(0L);
        when(bienDongSoDuRepository.demTruCuocCuaKyMuonHon(anyInt(), anyInt()))
                .thenReturn(0L);
    }

    private List<BienDongSoDu> chayVaBatSoCai(List<ChiTietSuDung> cdr, KyCuoc k) {
        when(chiTietSuDungRepository.timDaTinhTheoKyChoTraTruoc(k.getId())).thenReturn(cdr);
        service.truCuocKy(k);
        ArgumentCaptor<BienDongSoDu> bat = ArgumentCaptor.forClass(BienDongSoDu.class);
        verify(bienDongSoDuRepository, org.mockito.Mockito.atLeast(0)).save(bat.capture());
        return bat.getAllValues();
    }

    // =================================================================

    @Nested
    @DisplayName("Quy tắc trừ quỹ")
    class QuyTacTruQuy {

        @Test
        @DisplayName("1. Đủ số dư: trừ hết mọi bản ghi, ghi đúng một dòng sổ cái")
        void duSoDu_truHet() {
            KyCuoc k = ky(1, 6, 2026);
            ThueBao tb = thueBao(1, "100000");
            khongVuongChotChan();

            List<BienDongSoDu> soCai = chayVaBatSoCai(List.of(
                    cdr(tb, 10, "1000", 1),
                    cdr(tb, 11, "2000", 2),
                    cdr(tb, 12, "3000", 3)), k);

            assertThat(soCai).hasSize(1);
            BienDongSoDu bd = soCai.get(0);
            assertThat(bd.getLoaiBienDong()).isEqualTo(LoaiBienDongSoDu.TRU_CUOC);
            assertThat(bd.getSoTien()).isEqualByComparingTo("6000");
            assertThat(bd.getSoDuTruoc()).isEqualByComparingTo("100000");
            assertThat(bd.getSoDuSau()).isEqualByComparingTo("94000");
            assertThat(bd.getKyCuoc()).isSameAs(k);
            assertThat(tb.getSoDu()).isEqualByComparingTo("94000");
        }

        /**
         * ⭐ Test quan trọng nhất của lớp này.
         *
         * <p>Quỹ 1.000 đ, ba bản ghi 400 / 900 / 300. Bản ghi 900 làm cạn quỹ (còn 600).
         * Quy tắc đã chốt là <b>DỪNG HẲN</b>, nên bản ghi 300 — vốn <i>vừa</i> quỹ còn lại —
         * cũng KHÔNG được trừ. Nếu ai đó đổi {@code continue} thành "bỏ qua rồi đi tiếp"
         * thì số trừ sẽ là 700 thay vì 400, và test này là thứ duy nhất chặn lại.</p>
         */
        @Test
        @DisplayName("2. ⭐ Hết quỹ giữa chừng: DỪNG HẲN, không bỏ qua rồi trừ tiếp bản ghi rẻ hơn")
        void hetQuy_dungHan_khongBoQuaRoiDiTiep() {
            KyCuoc k = ky(1, 6, 2026);
            ThueBao tb = thueBao(1, "1000");
            khongVuongChotChan();

            List<BienDongSoDu> soCai = chayVaBatSoCai(List.of(
                    cdr(tb, 10, "400", 1),
                    cdr(tb, 11, "900", 2),    // 900 > 600 còn lại -> DỪNG
                    cdr(tb, 12, "300", 3)), k);  // vừa quỹ, nhưng vẫn KHÔNG trừ

            assertThat(soCai).hasSize(1);
            assertThat(soCai.get(0).getSoTien())
                    .as("Chỉ được trừ bản ghi 400 đ. Nếu ra 700 đ nghĩa là thuật toán đã "
                            + "'bỏ qua rồi đi tiếp' thay vì dừng hẳn — sai quyết định 5.3")
                    .isEqualByComparingTo("400");
            assertThat(soCai.get(0).getSoDuSau()).isEqualByComparingTo("600");
            assertThat(tb.getSoDu()).isEqualByComparingTo("600");
        }

        @Test
        @DisplayName("3. Bản ghi miễn phí (0 đ) luôn vừa quỹ, kể cả khi quỹ đã cạn")
        void banGhiMienPhi_luonVuaQuy() {
            KyCuoc k = ky(1, 6, 2026);
            ThueBao tb = thueBao(1, "500");
            khongVuongChotChan();

            List<BienDongSoDu> soCai = chayVaBatSoCai(List.of(
                    cdr(tb, 10, "500", 1),   // quỹ về 0
                    cdr(tb, 11, "0", 2),     // 0 <= 0 -> vẫn vừa
                    cdr(tb, 12, "1", 3)), k);   // 1 > 0 -> dừng

            assertThat(soCai.get(0).getSoTien()).isEqualByComparingTo("500");
            assertThat(tb.getSoDu()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("4. Không phát sinh cước nào thì KHÔNG ghi dòng sổ cái 0 đồng")
        void khongCoCuoc_khongGhiSo() {
            KyCuoc k = ky(1, 6, 2026);
            ThueBao tb = thueBao(1, "100000");
            khongVuongChotChan();
            when(chiTietSuDungRepository.timDaTinhTheoKyChoTraTruoc(1L))
                    .thenReturn(List.of(cdr(tb, 10, "0", 1), cdr(tb, 11, "0", 2)));

            service.truCuocKy(k);

            verify(bienDongSoDuRepository, never()).save(any());
            assertThat(tb.getSoDu()).isEqualByComparingTo("100000");
        }

        @Test
        @DisplayName("5. Nhiều thuê bao: mỗi thuê bao một dòng sổ cái, quỹ độc lập")
        void nhieuThueBao_quyDocLap() {
            KyCuoc k = ky(1, 6, 2026);
            ThueBao a = thueBao(1, "1000");
            ThueBao b = thueBao(2, "100");
            khongVuongChotChan();

            List<BienDongSoDu> soCai = chayVaBatSoCai(List.of(
                    cdr(a, 10, "600", 1),
                    cdr(a, 11, "300", 2),
                    cdr(b, 20, "80", 3),
                    cdr(b, 21, "50", 4)), k);   // 50 > 20 còn lại -> dừng

            assertThat(soCai).hasSize(2);
            assertThat(soCai.get(0).getSoTien()).isEqualByComparingTo("900");
            assertThat(soCai.get(1).getSoTien()).isEqualByComparingTo("80");
            assertThat(a.getSoDu()).isEqualByComparingTo("100");
            assertThat(b.getSoDu()).isEqualByComparingTo("20");
        }
    }

    @Nested
    @DisplayName("Tính xác định và sự phụ thuộc thứ tự")
    class ThuTu {

        @Test
        @DisplayName("6. Cùng danh sách chạy hai lần ra cùng kết quả")
        void tinhXacDinh() {
            List<BigDecimal> ketQua = new ArrayList<>();
            for (int lan = 0; lan < 2; lan++) {
                KyCuoc k = ky(1, 6, 2026);
                ThueBao tb = thueBao(1, "1000");
                khongVuongChotChan();
                ketQua.add(chayVaBatSoCai(List.of(
                        cdr(tb, 10, "400", 1),
                        cdr(tb, 11, "900", 2),
                        cdr(tb, 12, "300", 3)), k).get(0).getSoTien());
                org.mockito.Mockito.reset(bienDongSoDuRepository, chiTietSuDungRepository);
            }
            assertThat(ketQua.get(0)).isEqualByComparingTo(ketQua.get(1));
        }

        /**
         * Bằng chứng vì sao truy vấn <b>bắt buộc</b> phải có {@code ORDER BY} cố định.
         *
         * <p>Không phải test khẳng định "đảo thứ tự thì kết quả không đổi" — điều đó
         * <b>bất khả</b> với quy tắc không cắt đôi, đúng như điểm sai lệch 42.5 của Phase 4
         * đã chỉ ra. Test này khẳng định điều ngược lại và đó mới là sự thật.</p>
         */
        @Test
        @DisplayName("7. ⭐ Đảo thứ tự cho kết quả KHÁC — nên ORDER BY là bắt buộc")
        void phuThuocThuTu() {
            // Quỹ 1000; ba bản ghi 900 / 50 / 60.
            //   Xuôi : 900 -> quỹ 100; 50 -> quỹ 50; 60 > 50 DỪNG   => trừ 950
            //   Ngược:  60 -> quỹ 940; 50 -> quỹ 890; 900 > 890 DỪNG => trừ 110
            KyCuoc k1 = ky(1, 6, 2026);
            ThueBao tb1 = thueBao(1, "1000");
            khongVuongChotChan();
            BigDecimal xuoi = chayVaBatSoCai(List.of(
                    cdr(tb1, 10, "900", 1),
                    cdr(tb1, 11, "50", 2),
                    cdr(tb1, 12, "60", 3)), k1).get(0).getSoTien();

            org.mockito.Mockito.reset(bienDongSoDuRepository, chiTietSuDungRepository);

            KyCuoc k2 = ky(1, 6, 2026);
            ThueBao tb2 = thueBao(1, "1000");
            khongVuongChotChan();
            BigDecimal nguoc = chayVaBatSoCai(List.of(
                    cdr(tb2, 12, "60", 1),
                    cdr(tb2, 11, "50", 2),
                    cdr(tb2, 10, "900", 3)), k2).get(0).getSoTien();

            assertThat(xuoi).isEqualByComparingTo("950");
            assertThat(nguoc).isEqualByComparingTo("110");
            assertThat(xuoi)
                    .as("Hai thứ tự PHẢI cho hai kết quả khác nhau. Nếu bằng nhau thì hoặc "
                            + "thuật toán đã cắt đôi bản ghi, hoặc test dựng sai dữ liệu")
                    .isNotEqualByComparingTo(nguoc);
        }
    }

    @Nested
    @DisplayName("Chốt chặn khi chạy lại và chạy sai thứ tự kỳ")
    class ChotChan {

        @Test
        @DisplayName("8. Kỳ đã trừ cước rồi thì từ chối chạy lại")
        void daTruRoi_tuChoi() {
            KyCuoc k = ky(1, 6, 2026);
            when(bienDongSoDuRepository.countByKyCuocIdAndLoaiBienDong(
                    1L, LoaiBienDongSoDu.TRU_CUOC)).thenReturn(16L);

            assertThatThrownBy(() -> service.truCuocKy(k))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã trừ cước")
                    .hasMessageContaining("trừ chồng");
            verify(bienDongSoDuRepository, never()).save(any());
        }

        @Test
        @DisplayName("9. ⭐ Đã trừ kỳ muộn hơn thì từ chối chạy kỳ cũ — trừ cước không giao hoán")
        void daTruKyMuonHon_tuChoi() {
            KyCuoc k5 = ky(2, 5, 2026);
            when(bienDongSoDuRepository.countByKyCuocIdAndLoaiBienDong(
                    2L, LoaiBienDongSoDu.TRU_CUOC)).thenReturn(0L);
            when(bienDongSoDuRepository.demTruCuocCuaKyMuonHon(2026, 5)).thenReturn(16L);

            assertThatThrownBy(() -> service.truCuocKy(k5))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("kỳ muộn hơn")
                    .hasMessageContaining("thứ tự");
            verify(bienDongSoDuRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Hủy kết quả trừ cước")
    class HuyTruCuoc {

        private BienDongSoDu dongTruCuoc(ThueBao tb, String soTien,
                                         String soDuTruoc, String soDuSau) {
            BienDongSoDu bd = new BienDongSoDu();
            bd.setThueBao(tb);
            bd.setLoaiBienDong(LoaiBienDongSoDu.TRU_CUOC);
            bd.setSoTien(new BigDecimal(soTien));
            bd.setSoDuTruoc(new BigDecimal(soDuTruoc));
            bd.setSoDuSau(new BigDecimal(soDuSau));
            return bd;
        }

        @Test
        @DisplayName("10. Hoàn tác trả số dư về đúng giá trị trước khi trừ")
        void hoanTac_traVeSoDuCu() {
            KyCuoc k = ky(1, 6, 2026);
            ThueBao tb = thueBao(1, "600");
            when(bienDongSoDuRepository.demTruCuocCuaKyMuonHon(2026, 6)).thenReturn(0L);
            when(bienDongSoDuRepository.findByKyCuocIdAndLoaiBienDong(
                    1L, LoaiBienDongSoDu.TRU_CUOC))
                    .thenReturn(List.of(dongTruCuoc(tb, "400", "1000", "600")));

            int so = service.huyTruCuocKy(k);

            assertThat(so).isEqualTo(1);
            assertThat(tb.getSoDu()).isEqualByComparingTo("1000");
            verify(bienDongSoDuRepository).deleteAll(any());
        }

        /**
         * Lý do hoàn tác phải <b>cộng trả lại</b> chứ không gán thẳng {@code so_du_truoc}.
         *
         * <p>Khách trừ cước 400 (1000 → 600) rồi nạp thêm 500 (600 → 1100). Gán thẳng
         * {@code so_du = so_du_truoc = 1000} sẽ <b>xóa mất</b> khoản nạp 500. Cộng trả lại
         * cho 1100 + 400 = 1500, đúng bằng 1000 + 500.</p>
         */
        @Test
        @DisplayName("11. ⭐ Có nạp tiền xen giữa: cộng trả lại, KHÔNG gán thẳng so_du_truoc")
        void coNapTienXenGiua_congTraLai() {
            KyCuoc k = ky(1, 6, 2026);
            ThueBao tb = thueBao(1, "1100");   // đã nạp thêm 500 sau khi bị trừ
            when(bienDongSoDuRepository.demTruCuocCuaKyMuonHon(2026, 6)).thenReturn(0L);
            when(bienDongSoDuRepository.findByKyCuocIdAndLoaiBienDong(
                    1L, LoaiBienDongSoDu.TRU_CUOC))
                    .thenReturn(List.of(dongTruCuoc(tb, "400", "1000", "600")));

            service.huyTruCuocKy(k);

            assertThat(tb.getSoDu())
                    .as("Gán thẳng so_du_truoc sẽ ra 1000 và xóa mất khoản nạp 500 đ")
                    .isEqualByComparingTo("1500");
        }

        @Test
        @DisplayName("12. Kỳ đã chốt thì không hủy được")
        void kyDaChot_khongHuyDuoc() {
            KyCuoc k = ky(1, 6, 2026);
            k.setTrangThai(TrangThaiKyCuoc.DA_CHOT);

            assertThatThrownBy(() -> service.huyTruCuocKy(k))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã chốt");
        }

        @Test
        @DisplayName("13. Còn kỳ muộn hơn chưa hủy thì không hủy kỳ cũ được")
        void conKyMuonHon_khongHuyDuoc() {
            KyCuoc k5 = ky(2, 5, 2026);
            when(bienDongSoDuRepository.demTruCuocCuaKyMuonHon(2026, 5)).thenReturn(16L);

            assertThatThrownBy(() -> service.huyTruCuocKy(k5))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("kỳ muộn nhất");
        }
    }
}
