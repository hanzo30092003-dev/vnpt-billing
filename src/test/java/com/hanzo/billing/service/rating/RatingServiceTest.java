package com.hanzo.billing.service.rating;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.hanzo.billing.dto.KetQuaRating;
import com.hanzo.billing.entity.BangGiaCuoc;
import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.DangKyGoiCuoc;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import com.hanzo.billing.enums.TrangThaiDangKyGoi;
import com.hanzo.billing.enums.TrangThaiKyCuoc;
import com.hanzo.billing.enums.TrangThaiTinhCuoc;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.enums.LoaiBienDongSoDu;
import com.hanzo.billing.repository.BienDongSoDuRepository;
import com.hanzo.billing.repository.ChiTietSuDungRepository;
import com.hanzo.billing.repository.DangKyGoiCuocRepository;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.KyCuocRepository;
import com.hanzo.billing.service.NhatKyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Kiểm thử engine định giá.
 *
 * <p>Ba nhóm việc được kiểm tách bạch: quy sản lượng về block (tiền phụ thuộc trực tiếp
 * vào đây), chọn đúng gói cước tại thời điểm phát sinh, và vòng đời trạng thái của kỳ.</p>
 *
 * <p>Bảng giá dùng trong test đặt đơn giá tròn và khác nhau giữa các dòng để nhìn con số
 * là biết đã đi vào nhánh nào, không phải suy ngược từ phép nhân.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Engine định giá CDR")
class RatingServiceTest {

    private static final Long GOI_CU = 1L;
    private static final Long GOI_MOI = 2L;
    private static final Long KY_ID = 7L;

    @Mock private JdbcTemplate jdbcTemplate;
    @Mock private ChiTietSuDungRepository chiTietSuDungRepository;
    @Mock private DangKyGoiCuocRepository dangKyGoiCuocRepository;
    @Mock private KyCuocRepository kyCuocRepository;
    @Mock private HoaDonRepository hoaDonRepository;
    @Mock private BienDongSoDuRepository bienDongSoDuRepository;
    @Mock private BangGiaLookup bangGiaLookup;
    @Mock private NhatKyService nhatKyService;

    @InjectMocks
    private RatingService service;

    // =================================================================
    // NHÓM 1 — QUY SẢN LƯỢNG VỀ BLOCK
    // =================================================================

    @Nested
    @DisplayName("Quy sản lượng về block và nhân đơn giá")
    class QuyVeBlock {

        /** Giá thoại nội mạng: block 6 giây, 15 đ mỗi block. */
        private void chuanBiGiaThoai() {
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 6, 15)),
                    List.of());
        }

        @Test
        @DisplayName("1. Cuộc gọi 1 giây vẫn tính trọn 1 block: 1 × 15 = 15 đ")
        void motGiay_tinhTronMotBlock() {
            chuanBiGiaThoai();
            assertThat(service.ratingCdr(cuocGoi(1))).isEqualByComparingTo("15");
        }

        @Test
        @DisplayName("2. Cuộc gọi 60 giây chia hết: 10 block × 15 = 150 đ")
        void sauMuoiGiay_muoiBlock() {
            chuanBiGiaThoai();
            assertThat(service.ratingCdr(cuocGoi(60))).isEqualByComparingTo("150");
        }

        @Test
        @DisplayName("3. Cuộc gọi 61 giây lẻ 1 giây vẫn sang block mới: 11 block × 15 = 165 đ")
        void sauMuoiMotGiay_muoiMotBlock() {
            chuanBiGiaThoai();
            assertThat(service.ratingCdr(cuocGoi(61))).isEqualByComparingTo("165");
        }

        @Test
        @DisplayName("4. SMS: số block đúng bằng số tin, 3 tin × 99 = 297 đ")
        void sms_soBlockBangSoTin() {
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.SMS, HuongCuocGoi.NOI_MANG, false, 1, 99)),
                    List.of());
            ChiTietSuDung cdr = cdr(LoaiDichVu.SMS, HuongCuocGoi.NOI_MANG, false, 0, 3);
            assertThat(service.ratingCdr(cdr)).isEqualByComparingTo("297");
        }

        @Test
        @DisplayName("5. DATA 2048 KB = đúng 2 MB: 2 block × 25 = 50 đ")
        void data_haiMbTron() {
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG, false, 1, 25)),
                    List.of());
            ChiTietSuDung cdr = cdr(LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG, false, 0, 2048);
            assertThat(service.ratingCdr(cdr)).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("6. DATA 2049 KB làm tròn LÊN 3 MB, không phải 2: 3 × 25 = 75 đ")
        void data_leMotKb_thiLenBaMb() {
            // Nếu engine chia 1024 rồi cắt phần thập phân thì ra 50 đ - thiếu một block.
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG, false, 1, 25)),
                    List.of());
            ChiTietSuDung cdr = cdr(LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG, false, 0, 2049);
            assertThat(service.ratingCdr(cdr)).isEqualByComparingTo("75");
        }

        @Test
        @DisplayName("7. SMS mang cờ cao điểm vẫn tính được, nhờ chuỗi dự phòng lùi về giá thường")
        void smsCaoDiem_luiVeGiaThuong() {
            // Bảng giá không có dòng SMS cao điểm. Bản ghi như vậy chỉ xuất hiện khi có
            // người chèn thẳng bằng SQL, nhưng engine phải xử lý được chứ không đánh LOI.
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.SMS, HuongCuocGoi.NOI_MANG, false, 1, 99)),
                    List.of());
            ChiTietSuDung cdr = cdr(LoaiDichVu.SMS, HuongCuocGoi.NOI_MANG, true, 0, 1);
            assertThat(service.ratingCdr(cdr)).isEqualByComparingTo("99");
        }
    }

    // =================================================================
    // NHÓM 2 — CHỌN GÓI CƯỚC TẠI THỜI ĐIỂM PHÁT SINH
    // =================================================================

    @Nested
    @DisplayName("Chọn gói cước tại thời điểm phát sinh CDR")
    class ChonGoiCuoc {

        /** Ba dòng giá: gói cũ 10 đ, gói mới 20 đ, giá chung 99 đ — phân biệt được nhánh. */
        private List<BangGiaCuoc> baDongGia() {
            return List.of(
                    giaCuaGoi(GOI_CU, 10),
                    giaCuaGoi(GOI_MOI, 20),
                    giaChung(LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 6, 99));
        }

        @Test
        @DisplayName("8. Đổi gói giữa kỳ: CDR ngày 10/06 dùng gói CŨ, không dùng gói hiện hành")
        void doiGoiGiuaKy_dungGoiTaiThoiDiemCdr() {
            // Thuê bao đang mang gói mới (GOI_MOI) trên cột thue_bao.goi_cuoc_id.
            // CDR phát sinh 10/06, lúc đó còn đang dùng gói cũ.
            ThueBao thueBao = thueBao(GOI_MOI);
            chuanBiTraCuu(baDongGia(), List.of(
                    dangKy(thueBao, GOI_CU, "2026-01-01", "2026-06-14"),
                    dangKy(thueBao, GOI_MOI, "2026-06-15", null)));

            ChiTietSuDung cdr = cdr(thueBao, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG,
                    false, 1, 0, LocalDateTime.of(2026, 6, 10, 9, 0));

            assertThat(service.ratingCdr(cdr)).isEqualByComparingTo("10");
        }

        @Test
        @DisplayName("9. Cùng thuê bao, CDR ngày 20/06 dùng gói MỚI — ranh giới đổi gói đúng ngày")
        void sauNgayDoiGoi_dungGoiMoi() {
            ThueBao thueBao = thueBao(GOI_MOI);
            chuanBiTraCuu(baDongGia(), List.of(
                    dangKy(thueBao, GOI_CU, "2026-01-01", "2026-06-14"),
                    dangKy(thueBao, GOI_MOI, "2026-06-15", null)));

            ChiTietSuDung cdr = cdr(thueBao, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG,
                    false, 1, 0, LocalDateTime.of(2026, 6, 20, 9, 0));

            assertThat(service.ratingCdr(cdr)).isEqualByComparingTo("20");
        }

        @Test
        @DisplayName("10. Thiếu bản ghi đăng ký gói: lùi về gói hiện hành VÀ ghi log WARN")
        void thieuDangKy_luiVeGoiHienHanhVaCanhBao() {
            ThueBao thueBao = thueBao(GOI_MOI);
            chuanBiTraCuu(baDongGia(), List.of());

            ListAppender<ILoggingEvent> batLog = batDauBatLog();
            BigDecimal cuoc;
            try {
                cuoc = service.ratingCdr(cdr(thueBao, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG,
                        false, 1, 0, LocalDateTime.of(2026, 6, 10, 9, 0)));
            } finally {
                ketThucBatLog(batLog);
            }

            assertThat(cuoc).isEqualByComparingTo("20");
            assertThat(batLog.list)
                    .as("Đường lùi phải để lại dấu vết, nếu không thì dữ liệu thiếu bản ghi "
                            + "đăng ký gói sẽ không bao giờ lộ ra")
                    .anyMatch(su -> su.getLevel() == Level.WARN
                            && su.getFormattedMessage().contains("lùi về gói hiện hành"));
        }

        @Test
        @DisplayName("11. Không tra được đơn giá thì ném lỗi nghiệp vụ nêu rõ tổ hợp")
        void khongTraDuocGia_thiNemLoi() {
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 6, 15)),
                    List.of());
            ChiTietSuDung cdr = cdr(LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG, false, 0, 1024);

            assertThatThrownBy(() -> service.ratingCdr(cdr))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("DATA");
        }
    }

    // =================================================================
    // NHÓM 3 — CHẠY CẢ MỘT KỲ
    // =================================================================

    @Nested
    @DisplayName("Chạy tính cước cả kỳ")
    class ChayCaKy {

        @Test
        @DisplayName("12. Kỳ đã chốt thì bị chặn ngay, không đụng tới bản ghi nào")
        void kyDaChot_thiBiChan() {
            KyCuoc ky = ky(TrangThaiKyCuoc.DA_CHOT);

            assertThatThrownBy(() -> service.ratingKy(ky))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã chốt");

            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.DA_CHOT);
            verifyNoInteractions(jdbcTemplate);
            verify(chiTietSuDungRepository, never()).timCanTinhCuoc(any(), any());
        }

        @Test
        @DisplayName("13. Kỳ đang kẹt ở Đang tính thì bị chặn, kèm hướng dẫn dùng chức năng gỡ")
        void kyDangTinh_thiBiChanVaChiDuongGo() {
            KyCuoc ky = ky(TrangThaiKyCuoc.DANG_TINH);

            assertThatThrownBy(() -> service.ratingKy(ky))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("Gỡ kỳ bị kẹt");

            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("14. Chạy lần hai khi mọi bản ghi đã tính: không ghi gì, cước không đổi")
        void chayLanHai_khongGhiGiThem() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 6, 15)),
                    List.of());
            // Truy vấn loại bỏ bản ghi DA_TINH, nên lần chạy thứ hai không còn gì để xử lý
            when(chiTietSuDungRepository.timCanTinhCuoc(any(), any())).thenReturn(List.of());
            when(chiTietSuDungRepository.countByKyCuocIdAndTrangThaiTinhCuoc(
                    KY_ID, TrangThaiTinhCuoc.DA_TINH)).thenReturn(5017L);

            KetQuaRating ketQua = service.ratingKy(ky);

            assertThat(ketQua.getSoThanhCong()).isZero();
            assertThat(ketQua.getSoLoi()).isZero();
            assertThat(ketQua.getTongCuoc()).isEqualByComparingTo("0");
            verify(jdbcTemplate, never()).batchUpdate(anyString(),
                    any(BatchPreparedStatementSetter.class));

            // soCdrXuLy đếm lại từ CSDL, nên không bị tụt về 0 sau lần chạy thứ hai
            assertThat(ky.getSoCdrXuLy()).isEqualTo(5017);
            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.MO);
        }

        @Test
        @DisplayName("15. Chạy xong đưa kỳ về Mở, không tự chốt — chốt kỳ là thao tác riêng")
        void chayXong_kyVeMoChuKhongChot() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            ThueBao thueBao = thueBao(GOI_MOI);
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 6, 15)),
                    List.of(dangKy(thueBao, GOI_MOI, "2026-01-01", null)));
            when(chiTietSuDungRepository.timCanTinhCuoc(any(), any())).thenReturn(List.of(
                    cdr(thueBao, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 60, 0,
                            LocalDateTime.of(2026, 6, 10, 9, 0))));
            when(chiTietSuDungRepository.countByKyCuocIdAndTrangThaiTinhCuoc(
                    KY_ID, TrangThaiTinhCuoc.DA_TINH)).thenReturn(1L);

            KetQuaRating ketQua = service.ratingKy(ky);

            assertThat(ketQua.getSoThanhCong()).isEqualTo(1);
            assertThat(ketQua.getTongCuoc()).isEqualByComparingTo("150");
            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.MO);
            verify(nhatKyService).ghiNhatKy(eq("TINH_CUOC"), eq("KY_CUOC"), eq(KY_ID), anyString());
        }

        @Test
        @DisplayName("16. Một bản ghi hỏng không làm dừng cả kỳ: bản ghi kia vẫn tính xong")
        void motBanGhiHong_khongLamDungCaKy() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            ThueBao thueBao = thueBao(GOI_MOI);
            chuanBiTraCuu(List.of(giaChung(LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 6, 15)),
                    List.of(dangKy(thueBao, GOI_MOI, "2026-01-01", null)));
            when(chiTietSuDungRepository.timCanTinhCuoc(any(), any())).thenReturn(List.of(
                    // Bản ghi tốt
                    cdr(thueBao, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 60, 0,
                            LocalDateTime.of(2026, 6, 10, 9, 0)),
                    // Bản ghi không có dòng bảng giá nào khớp
                    cdr(thueBao, LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG, false, 0, 1024,
                            LocalDateTime.of(2026, 6, 11, 9, 0))));
            when(chiTietSuDungRepository.countByKyCuocIdAndTrangThaiTinhCuoc(
                    KY_ID, TrangThaiTinhCuoc.DA_TINH)).thenReturn(1L);

            KetQuaRating ketQua = service.ratingKy(ky);

            assertThat(ketQua.getSoThanhCong()).isEqualTo(1);
            assertThat(ketQua.getSoLoi()).isEqualTo(1);
            assertThat(ketQua.getTongCuoc()).isEqualByComparingTo("150");
            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.MO);
        }
    }

    // =================================================================
    // NHÓM 4 — HỦY KẾT QUẢ VÀ GỠ KỲ BỊ KẸT
    // =================================================================

    @Nested
    @DisplayName("Hủy kết quả tính cước và gỡ kỳ bị kẹt")
    class HuyVaGo {

        @Test
        @DisplayName("17. Hủy kết quả: CDR về Chưa tính, kỳ về Mở, số bản ghi xử lý reset")
        void huyKetQua_duaCdrVeChuaTinh() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            ky.setSoCdrXuLy(5017);
            when(hoaDonRepository.countByKyCuocId(KY_ID)).thenReturn(0L);
            when(bienDongSoDuRepository.countByKyCuocIdAndLoaiBienDong(
                    KY_ID, LoaiBienDongSoDu.TRU_CUOC)).thenReturn(0L);
            when(jdbcTemplate.update(anyString(), eq(KY_ID))).thenReturn(5017);

            int soBanGhi = service.huyRatingKy(ky);

            assertThat(soBanGhi).isEqualTo(5017);
            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.MO);
            assertThat(ky.getSoCdrXuLy()).isZero();
            verify(nhatKyService).ghiNhatKy(eq("HUY_TINH_CUOC"), eq("KY_CUOC"), eq(KY_ID), anyString());
        }

        @Test
        @DisplayName("18. Kỳ đã có hóa đơn thì không cho hủy — hóa đơn sẽ không còn khớp chi tiết")
        void kyDaCoHoaDon_thiKhongChoHuy() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            when(hoaDonRepository.countByKyCuocId(KY_ID)).thenReturn(58L);

            assertThatThrownBy(() -> service.huyRatingKy(ky))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("58 hóa đơn");

            verifyNoInteractions(jdbcTemplate);
        }

        /**
         * Nhánh trả trước của cùng một lý do với test 18.
         *
         * <p>Dòng {@code TRU_CUOC} lấy số tiền từ {@code cuoc_phi} của chính các bản ghi
         * này. Xóa {@code cuoc_phi} mà giữ dòng trừ thì số dư đã trừ không còn dữ liệu nào
         * giải thích — đúng thứ mục G1 sinh ra để tránh.</p>
         */
        @Test
        @DisplayName("18b. Kỳ đã trừ cước vào số dư thì không cho hủy kết quả tính cước")
        void kyDaTruCuocTraTruoc_thiKhongChoHuy() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            when(hoaDonRepository.countByKyCuocId(KY_ID)).thenReturn(0L);
            when(bienDongSoDuRepository.countByKyCuocIdAndLoaiBienDong(
                    KY_ID, LoaiBienDongSoDu.TRU_CUOC)).thenReturn(16L);

            assertThatThrownBy(() -> service.huyRatingKy(ky))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("16 thuê bao trả trước");

            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("19. Kỳ đã chốt thì không cho hủy kết quả tính cước")
        void kyDaChot_thiKhongChoHuy() {
            KyCuoc ky = ky(TrangThaiKyCuoc.DA_CHOT);

            assertThatThrownBy(() -> service.huyRatingKy(ky))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("đã chốt");

            verifyNoInteractions(jdbcTemplate);
        }

        @Test
        @DisplayName("20. Gỡ kỳ kẹt: kỳ Đang tính được đưa về Mở, có ghi nhật ký")
        void goKyKet_kyDangTinh_thiVeMo() {
            KyCuoc ky = ky(TrangThaiKyCuoc.DANG_TINH);
            when(kyCuocRepository.findById(KY_ID)).thenReturn(Optional.of(ky));

            service.goKyBiKet(KY_ID);

            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.MO);
            verify(nhatKyService).ghiNhatKy(eq("GO_KY_BI_KET"), eq("KY_CUOC"), eq(KY_ID), anyString());
        }

        @Test
        @DisplayName("21. Gỡ kỳ kẹt: kỳ đang Mở thì bị từ chối, không phải kỳ nào cũng gỡ được")
        void goKyKet_kyDangMo_thiTuChoi() {
            KyCuoc ky = ky(TrangThaiKyCuoc.MO);
            when(kyCuocRepository.findById(KY_ID)).thenReturn(Optional.of(ky));

            assertThatThrownBy(() -> service.goKyBiKet(KY_ID))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("không bị kẹt");

            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.MO);
            verify(kyCuocRepository, never()).save(any());
        }

        @Test
        @DisplayName("22. Gỡ kỳ kẹt: kỳ đã chốt cũng bị từ chối")
        void goKyKet_kyDaChot_thiTuChoi() {
            KyCuoc ky = ky(TrangThaiKyCuoc.DA_CHOT);
            when(kyCuocRepository.findById(KY_ID)).thenReturn(Optional.of(ky));

            assertThatThrownBy(() -> service.goKyBiKet(KY_ID))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("Đã chốt");

            assertThat(ky.getTrangThai()).isEqualTo(TrangThaiKyCuoc.DA_CHOT);
        }
    }

    // =================================================================
    // TIỆN ÍCH DỰNG DỮ LIỆU
    // =================================================================

    /** Nạp sẵn bảng giá và lịch sử đăng ký gói cho một lần chạy. */
    private void chuanBiTraCuu(List<BangGiaCuoc> bangGia, List<DangKyGoiCuoc> dangKy) {
        when(bangGiaLookup.nap()).thenReturn(new BangGiaLookup.Bang(bangGia));
        when(dangKyGoiCuocRepository.timTatCaKemGoiVaThueBao()).thenReturn(dangKy);
    }

    private static ListAppender<ILoggingEvent> batDauBatLog() {
        ListAppender<ILoggingEvent> batLog = new ListAppender<>();
        batLog.start();
        ((Logger) LoggerFactory.getLogger(RatingService.class)).addAppender(batLog);
        return batLog;
    }

    private static void ketThucBatLog(ListAppender<ILoggingEvent> batLog) {
        ((Logger) LoggerFactory.getLogger(RatingService.class)).detachAppender(batLog);
        batLog.stop();
    }

    private static GoiCuoc goiCuoc(Long id) {
        GoiCuoc goi = new GoiCuoc();
        goi.setId(id);
        return goi;
    }

    private static ThueBao thueBao(Long goiCuocId) {
        ThueBao tb = new ThueBao();
        tb.setId(100L);
        tb.setSoThueBao("0900000001");
        tb.setGoiCuoc(goiCuoc(goiCuocId));
        return tb;
    }

    private static DangKyGoiCuoc dangKy(ThueBao thueBao, Long goiCuocId, String tu, String den) {
        DangKyGoiCuoc dk = new DangKyGoiCuoc();
        dk.setThueBao(thueBao);
        dk.setGoiCuoc(goiCuoc(goiCuocId));
        dk.setNgayBatDau(LocalDate.parse(tu));
        dk.setNgayKetThuc(den == null ? null : LocalDate.parse(den));
        dk.setTrangThai(den == null ? TrangThaiDangKyGoi.DANG_AP_DUNG
                : TrangThaiDangKyGoi.DA_KET_THUC);
        return dk;
    }

    /** Cuộc gọi nội mạng giờ thường của thuê bao mang gói {@link #GOI_MOI}. */
    private static ChiTietSuDung cuocGoi(int thoiLuongGiay) {
        return cdr(LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, thoiLuongGiay, 0);
    }

    private static ChiTietSuDung cdr(LoaiDichVu dichVu, HuongCuocGoi huong, boolean caoDiem,
                                     int thoiLuongGiay, int soLuong) {
        return cdr(thueBao(GOI_MOI), dichVu, huong, caoDiem, thoiLuongGiay, soLuong,
                LocalDateTime.of(2026, 6, 10, 9, 0));
    }

    private static ChiTietSuDung cdr(ThueBao thueBao, LoaiDichVu dichVu, HuongCuocGoi huong,
                                     boolean caoDiem, int thoiLuongGiay, int soLuong,
                                     LocalDateTime thoiDiem) {
        ChiTietSuDung cdr = new ChiTietSuDung();
        cdr.setId(1L);
        cdr.setThueBao(thueBao);
        cdr.setSoThueBao(thueBao.getSoThueBao());
        cdr.setLoaiDichVu(dichVu);
        cdr.setHuong(huong);
        cdr.setGioCaoDiem(caoDiem);
        cdr.setThoiLuongGiay(thoiLuongGiay);
        cdr.setSoLuong(soLuong);
        cdr.setThoiGianBatDau(thoiDiem);
        cdr.setTrangThaiTinhCuoc(TrangThaiTinhCuoc.CHUA_TINH);
        return cdr;
    }

    private static BangGiaCuoc giaChung(LoaiDichVu dichVu, HuongCuocGoi huong, boolean caoDiem,
                                        int blockGiay, int donGia) {
        return bangGia(null, dichVu, huong, caoDiem, blockGiay, donGia);
    }

    /** Dòng giá riêng của một gói, cho THOAI / NOI_MANG / giờ thường, block 6 giây. */
    private static BangGiaCuoc giaCuaGoi(Long goiCuocId, int donGia) {
        return bangGia(goiCuocId, LoaiDichVu.THOAI, HuongCuocGoi.NOI_MANG, false, 6, donGia);
    }

    private static BangGiaCuoc bangGia(Long goiCuocId, LoaiDichVu dichVu, HuongCuocGoi huong,
                                       boolean caoDiem, int blockGiay, int donGia) {
        BangGiaCuoc bg = new BangGiaCuoc();
        if (goiCuocId != null) {
            bg.setGoiCuoc(goiCuoc(goiCuocId));
        }
        bg.setLoaiDichVu(dichVu);
        bg.setHuong(huong);
        bg.setGioCaoDiem(caoDiem);
        bg.setBlockGiay(blockGiay);
        bg.setDonGia(BigDecimal.valueOf(donGia));
        bg.setNgayHieuLuc(LocalDate.of(2025, 1, 1));
        return bg;
    }

    private static KyCuoc ky(TrangThaiKyCuoc trangThai) {
        KyCuoc ky = new KyCuoc();
        ky.setId(KY_ID);
        ky.setThang(6);
        ky.setNam(2026);
        ky.setNgayBatDau(LocalDate.of(2026, 6, 1));
        ky.setNgayKetThuc(LocalDate.of(2026, 6, 30));
        ky.setTrangThai(trangThai);
        ky.setSoCdrXuLy(0);
        return ky;
    }
}
