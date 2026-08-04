package com.hanzo.billing.service.rating;

import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;
import com.hanzo.billing.enums.LoaiDichVu;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kiểm thử quỹ ưu đãi gói cước.
 *
 * <p>Nhóm đầu là nhóm quan trọng nhất: nó bắt <b>hai cái bẫy quy đổi đơn vị</b>. Cả hai đều
 * hỏng theo cùng kiểu — hóa đơn vẫn phát hành bình thường, không lỗi, không cảnh báo, chỉ
 * sai tiền. Một cái sai 1024 lần, một cái sai 60 lần.</p>
 */
@DisplayName("Quỹ ưu đãi gói cước")
class UuDaiGoiCuocTest {

    // =================================================================
    // NHÓM 1 — BẮT BẪY QUY ĐỔI ĐƠN VỊ
    // =================================================================

    @Nested
    @DisplayName("Bẫy quy đổi đơn vị")
    class BayQuyDoi {

        @Test
        @DisplayName("1. ⚠️ Gói 2048 MB, dùng 1.500.000 KB (≈1465 MB) → VẪN TRONG ưu đãi")
        void bayKbMb_conTrongUuDai() {
            // Đây đúng ví dụ ở docs/mo-ta-csdl.md mục 6.1. Nếu so thẳng 1500000 > 2048 thì
            // kết luận vượt ưu đãi và tính cước cho phần "vượt" khổng lồ — sai 1024 lần.
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(0, 0, 0, 2048));

            assertThat(quy.namTrongUuDai(cdrData(1_500_000))).isTrue();
            assertThat(quy.conLaiKb()).isEqualTo(2048L * 1024 - 1_500_000);
        }

        @Test
        @DisplayName("2. ⚠️ Gói 100 phút nội mạng, gọi 5.400 giây (90 phút) → VẪN TRONG ưu đãi")
        void bayGiayPhut_conTrongUuDai() {
            // So thẳng 5400 > 100 là sai 60 lần.
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(100, 0, 0, 0));

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 5400))).isTrue();
            assertThat(quy.conLaiGiayNoiMang()).isEqualTo(600);
        }

        @Test
        @DisplayName("3. Gói 100 phút, gọi 6.060 giây (101 phút) → VƯỢT, bị tính tiền")
        void vuotQuyPhut_thiTinhTien() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(100, 0, 0, 0));

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 6060))).isFalse();
            // Quỹ KHÔNG bị trừ khi bản ghi không lọt
            assertThat(quy.conLaiGiayNoiMang()).isEqualTo(6000);
        }

        @Test
        @DisplayName("4. Đúng ranh giới: 6.000 giây = tròn 100 phút → vẫn lọt, quỹ về 0")
        void dungRanhGioi_vanLot() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(100, 0, 0, 0));

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 6000))).isTrue();
            assertThat(quy.conLaiGiayNoiMang()).isZero();
        }

        @Test
        @DisplayName("5. Đúng ranh giới data: 2048 MB = 2.097.152 KB → vẫn lọt, quỹ về 0")
        void dungRanhGioiData_vanLot() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(0, 0, 0, 2048));

            assertThat(quy.namTrongUuDai(cdrData(2_097_152))).isTrue();
            assertThat(quy.conLaiKb()).isZero();
        }

        @Test
        @DisplayName("6. SMS không quy đổi: 30 tin với quỹ 30 → miễn phí; tin thứ 31 → tính tiền")
        void sms_khongQuyDoi() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(0, 0, 30, 0));

            assertThat(quy.namTrongUuDai(cdrSms(30))).isTrue();
            assertThat(quy.conLaiTinNhan()).isZero();
            assertThat(quy.namTrongUuDai(cdrSms(1))).isFalse();
        }

        @Test
        @DisplayName("7. Nhiều cuộc gọi ngắn KHÔNG bị làm tròn lên từng cuộc")
        void nhieuCuocNgan_khongLamTronTungCuoc() {
            // 100 cuộc × 30 giây = 3000 giây = 50 phút. Nếu làm tròn LÊN từng cuộc thì
            // thành 100 phút và quỹ 60 phút sẽ bị coi là đã cạn từ cuộc thứ 61.
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(60, 0, 0, 0));
            int soCuocMienPhi = 0;
            for (int i = 0; i < 100; i++) {
                if (quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 30))) {
                    soCuocMienPhi++;
                }
            }
            assertThat(soCuocMienPhi).isEqualTo(100);
            assertThat(quy.conLaiGiayNoiMang()).isEqualTo(600);   // 3600 − 3000
        }
    }

    // =================================================================
    // NHÓM 2 — QUY TẮC TRỪ QUỸ
    // =================================================================

    @Nested
    @DisplayName("Quy tắc trừ quỹ")
    class QuyTacTru {

        @Test
        @DisplayName("8. Sau bản ghi làm vượt, quỹ ĐÓNG: bản ghi ngắn ngay sau đó vẫn tính tiền")
        void sauKhiVuot_quyDong() {
            // Không đóng quỹ thì khách không hiểu nổi vì sao cuộc dài mất tiền còn cuộc
            // ngắn ngay sau đó lại miễn phí.
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(2, 0, 0, 0));   // 120 giây

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 100))).isTrue();
            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 50))).isFalse();
            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 5))).isFalse();
            assertThat(quy.conLaiGiayNoiMang()).isEqualTo(20);
        }

        @Test
        @DisplayName("9. Cuộc gọi quốc tế luôn tính tiền và KHÔNG đụng tới quỹ nào")
        void quocTe_khongCoUuDai() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(100, 100, 30, 2048));

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.QUOC_TE, 300))).isFalse();
            assertThat(quy.conLaiGiayNoiMang()).isEqualTo(6000);
            assertThat(quy.conLaiGiayNgoaiMang()).isEqualTo(6000);
        }

        @Test
        @DisplayName("10. Tin nhắn quốc tế cũng không có ưu đãi, không trừ quỹ SMS")
        void smsQuocTe_khongCoUuDai() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(0, 0, 30, 0));
            ChiTietSuDung cdr = cdrSms(1);
            cdr.setHuong(HuongCuocGoi.QUOC_TE);

            assertThat(quy.namTrongUuDai(cdr)).isFalse();
            assertThat(quy.conLaiTinNhan()).isEqualTo(30);
        }

        @Test
        @DisplayName("11. Gói CB01 không có ưu đãi nào: mọi bản ghi đều tính tiền")
        void goiKhongUuDai_moiBanGhiTinhTien() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(0, 0, 0, 0));

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 1))).isFalse();
            assertThat(quy.namTrongUuDai(cdrSms(1))).isFalse();
            assertThat(quy.namTrongUuDai(cdrData(1))).isFalse();
        }

        @Test
        @DisplayName("12. Gói DN500, thuê bao dùng ít: toàn bộ miễn phí")
        void goiLon_dungIt_toanBoMienPhi() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(2000, 500, 500, 20480));

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 3600))).isTrue();
            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NGOAI_MANG, 1800))).isTrue();
            assertThat(quy.namTrongUuDai(cdrSms(10))).isTrue();
            assertThat(quy.namTrongUuDai(cdrData(500_000))).isTrue();
        }

        @Test
        @DisplayName("13. Bốn quỹ ĐỘC LẬP: cạn quỹ data không ảnh hưởng quỹ SMS hay thoại")
        void bonQuyDocLap() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(100, 100, 30, 1));   // data chỉ 1 MB

            assertThat(quy.namTrongUuDai(cdrData(2_000_000))).isFalse();  // cạn quỹ data
            assertThat(quy.namTrongUuDai(cdrSms(5))).isTrue();
            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 600))).isTrue();
            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NGOAI_MANG, 600))).isTrue();
        }

        @Test
        @DisplayName("14. Quỹ nội mạng và ngoại mạng tách riêng, không dùng lẫn nhau")
        void noiMangVaNgoaiMangTachRieng() {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(1, 10, 0, 0));   // 60 giây / 600 giây

            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NOI_MANG, 100))).isFalse();
            // Quỹ ngoại mạng còn nguyên dù quỹ nội mạng đã đóng
            assertThat(quy.namTrongUuDai(cdrThoai(HuongCuocGoi.NGOAI_MANG, 100))).isTrue();
            assertThat(quy.conLaiGiayNgoaiMang()).isEqualTo(500);
        }
    }

    // =================================================================
    // NHÓM 3 — TÍNH XÁC ĐỊNH VÀ SỰ PHỤ THUỘC THỨ TỰ
    // =================================================================

    @Nested
    @DisplayName("Tính xác định và sự phụ thuộc thứ tự")
    class ThuTuDuyet {

        /** Quỹ 1 phút = 60 giây, ba cuộc 30 / 40 / 20 giây. */
        private final List<ChiTietSuDung> banGhi = List.of(
                cdrThoai(HuongCuocGoi.NOI_MANG, 30),
                cdrThoai(HuongCuocGoi.NOI_MANG, 40),
                cdrThoai(HuongCuocGoi.NOI_MANG, 20));

        private List<Boolean> chay(List<ChiTietSuDung> danhSach) {
            UuDaiGoiCuoc quy = new UuDaiGoiCuoc(goi(1, 0, 0, 0));
            List<Boolean> ketQua = new ArrayList<>();
            for (ChiTietSuDung cdr : danhSach) {
                ketQua.add(quy.namTrongUuDai(cdr));
            }
            return ketQua;
        }

        @Test
        @DisplayName("15. Cùng danh sách, chạy hai lần → kết quả giống hệt (tính xác định)")
        void cungDanhSach_haiLan_giongHet() {
            assertThat(chay(banGhi)).isEqualTo(chay(banGhi));
        }

        @Test
        @DisplayName("16. Thuật toán PHỤ THUỘC thứ tự — chính vì vậy ORDER BY cố định là bắt buộc")
        void thuatToanPhuThuocThuTu() {
            //   Thứ tự gốc  [30, 40, 20]: 30 lọt (còn 30) → 40 vượt, quỹ đóng → 20 tính tiền
            //   Đảo ngược   [20, 40, 30]: 20 lọt (còn 40) → 40 lọt (còn 0) → 30 vượt
            // Hai thứ tự cho hai kết quả KHÁC nhau. Đây không phải khiếm khuyết mà là hệ quả
            // trực tiếp của quy tắc không cắt đôi bản ghi (quyết định 5.3). Vì vậy truy vấn
            // lấy CDR BẮT BUỘC phải có ORDER BY cố định — nếu để CSDL tự chọn thứ tự thì
            // cùng một dữ liệu có thể cho ra hai hóa đơn khác nhau ở hai lần chạy.
            List<ChiTietSuDung> daoNguoc = new ArrayList<>(banGhi);
            Collections.reverse(daoNguoc);

            assertThat(chay(banGhi)).containsExactly(true, false, false);
            assertThat(chay(daoNguoc)).containsExactly(true, true, false);
        }
    }

    // =================================================================
    // TIỆN ÍCH DỰNG DỮ LIỆU
    // =================================================================

    private static GoiCuoc goi(int phutNoiMang, int phutNgoaiMang, int sms, int dataMb) {
        GoiCuoc goi = new GoiCuoc();
        goi.setId(1L);
        goi.setMaGoi("TEST");
        goi.setCuocThueBaoThang(BigDecimal.ZERO);
        goi.setPhutNoiMangMienPhi(phutNoiMang);
        goi.setPhutNgoaiMangMienPhi(phutNgoaiMang);
        goi.setSmsMienPhi(sms);
        goi.setDataMienPhiMb(dataMb);
        return goi;
    }

    private static ChiTietSuDung cdrThoai(HuongCuocGoi huong, int thoiLuongGiay) {
        ChiTietSuDung cdr = cdrGoc(LoaiDichVu.THOAI, huong);
        cdr.setThoiLuongGiay(thoiLuongGiay);
        cdr.setSoLuong(0);
        return cdr;
    }

    private static ChiTietSuDung cdrSms(int soTin) {
        ChiTietSuDung cdr = cdrGoc(LoaiDichVu.SMS, HuongCuocGoi.NOI_MANG);
        cdr.setThoiLuongGiay(0);
        cdr.setSoLuong(soTin);
        return cdr;
    }

    private static ChiTietSuDung cdrData(int soKb) {
        ChiTietSuDung cdr = cdrGoc(LoaiDichVu.DATA, HuongCuocGoi.NOI_MANG);
        cdr.setThoiLuongGiay(0);
        cdr.setSoLuong(soKb);
        return cdr;
    }

    private static ChiTietSuDung cdrGoc(LoaiDichVu dichVu, HuongCuocGoi huong) {
        ChiTietSuDung cdr = new ChiTietSuDung();
        cdr.setId(1L);
        cdr.setLoaiDichVu(dichVu);
        cdr.setHuong(huong);
        cdr.setGioCaoDiem(false);
        cdr.setThoiGianBatDau(LocalDateTime.of(2026, 6, 10, 9, 0));
        return cdr;
    }
}
