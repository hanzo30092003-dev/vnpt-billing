package com.hanzo.billing.service.rating;

import com.hanzo.billing.dto.KetQuaImportCdr;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.exception.NghiepVuException;
import com.hanzo.billing.repository.ThueBaoRepository;
import com.hanzo.billing.service.NhatKyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phép kiểm cho {@link CdrImportService} — nhập CDR từ file CSV.
 *
 * <h2>Vì sao lớp này tồn tại</h2>
 * <p>Đợt rà tính năng 27/08 tìm ra: nhập CSV là chức năng <b>duy nhất</b> trong phạm vi đã cam
 * kết có đủ mã nguồn, có màn hình, tới được từ menu và trang trả 200 — nhưng <b>không một phép
 * kiểm nào</b> chạm tới. Không test Java, và cả 8 script giao diện đều không nhắc tên nó. Đó là
 * loại lỗ hổng nguy hiểm hơn một chức năng còn thiếu, vì nó tạo ảo giác đã xong.</p>
 *
 * <h2>Vì sao dùng Mockito chứ không chạy trên CSDL thật</h2>
 * <p>Service ghi thẳng vào {@code chi_tiet_su_dung} bằng {@code JdbcTemplate}. Bộ test của dự án
 * chạy trên <b>CSDL thật</b>, mà <b>23.223 CDR</b> là con số tài liệu bàn giao ghi rõ. Một phép
 * kiểm chèn thật vào đó là làm hỏng dữ liệu mẫu ngay lần chạy đầu. Giả lập cả ba phụ thuộc thì
 * không dòng nào được ghi, và vẫn kiểm được trọn bộ luật — vì toàn bộ luật nằm ở tầng phân tích
 * dòng, trước khi chạm CSDL.</p>
 *
 * <p>Phần <b>được</b> kiểm: 2 chốt chặn ở mức file, cách bỏ dòng tiêu đề và dòng trắng, 9 luật
 * kiểm từng dòng, và việc lô hợp lệ có thật sự được đẩy xuống {@code batchUpdate} hay không.
 * Phần <b>không</b> kiểm ở đây: câu SQL chèn có đúng cột không — đó là việc của một phép kiểm
 * chạm CSDL, và nó nằm ngoài phạm vi đợt này.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Nhập CDR từ file CSV")
class CdrImportServiceTest {

    private static final String TIEU_DE =
            "so_thue_bao,so_bi_goi,loai_dich_vu,huong,thoi_gian_bat_dau,thoi_luong_giay,so_luong";

    /** Thuê bao hợp lệ dùng cho phần lớn ca kiểm. */
    private static final String SO_TOT = "0900000001";

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ThueBaoRepository thueBaoRepository;

    @Mock
    private NhatKyService nhatKyService;

    private CdrImportService dichVu;

    @BeforeEach
    void dungBoiCanh() {
        dichVu = new CdrImportService(jdbcTemplate, thueBaoRepository, nhatKyService);
        when(thueBaoRepository.findAll()).thenReturn(List.of(
                thueBao(1L, SO_TOT, TrangThaiThueBao.HOAT_DONG, LocalDate.of(2026, 1, 1)),
                thueBao(2L, "0900000002", TrangThaiThueBao.TAM_NGUNG_2C, LocalDate.of(2026, 1, 1)),
                thueBao(3L, "0900000003", TrangThaiThueBao.DA_THANH_LY, LocalDate.of(2026, 1, 1)),
                thueBao(4L, "0900000004", TrangThaiThueBao.TAM_NGUNG_1C, LocalDate.of(2026, 1, 1))));
    }

    private static ThueBao thueBao(Long id, String so, TrangThaiThueBao tt, LocalDate kichHoat) {
        ThueBao tb = new ThueBao();
        tb.setId(id);
        tb.setSoThueBao(so);
        tb.setTrangThai(tt);
        tb.setNgayKichHoat(kichHoat);
        return tb;
    }

    private static MultipartFile csv(String... dong) {
        return new MockMultipartFile("file", "cdr.csv", "text/csv",
                String.join("\n", dong).getBytes(StandardCharsets.UTF_8));
    }

    /** Một dòng hợp lệ, để các ca kiểm chỉ đổi đúng phần đang xét. */
    private static String dongTot() {
        return SO_TOT + ",0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,120,1";
    }

    // =================================================================
    @Nested
    @DisplayName("1. Hai chốt chặn ở mức file")
    class ChotChanMucFile {

        @Test
        @DisplayName("Không chọn file thì báo rõ phải chọn file")
        void khongChonFile() {
            assertThatThrownBy(() -> dichVu.nhap(null))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("Vui lòng chọn file CSV");
        }

        @Test
        @DisplayName("File rỗng cũng bị chặn, không phải chạy rồi mới báo 0 dòng")
        void fileRong() {
            MultipartFile rong = new MockMultipartFile("file", "cdr.csv", "text/csv", new byte[0]);
            assertThatThrownBy(() -> dichVu.nhap(rong))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining("Vui lòng chọn file CSV");
        }

        @Test
        @DisplayName("Đuôi file không phải .csv thì bị từ chối")
        void saiDuoiFile() {
            MultipartFile xlsx = new MockMultipartFile("file", "cdr.xlsx", "application/vnd.ms-excel",
                    dongTot().getBytes(StandardCharsets.UTF_8));
            assertThatThrownBy(() -> dichVu.nhap(xlsx))
                    .isInstanceOf(NghiepVuException.class)
                    .hasMessageContaining(".csv");
        }

        @Test
        @DisplayName("Chặn ở mức file thì KHÔNG ghi gì xuống CSDL")
        void chanMucFileThiKhongGhi() {
            assertThatThrownBy(() -> dichVu.nhap(null)).isInstanceOf(NghiepVuException.class);
            verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
        }
    }

    // =================================================================
    @Nested
    @DisplayName("2. Dòng tiêu đề và dòng trắng")
    class TieuDeVaDongTrang {

        @Test
        @DisplayName("Dòng tiêu đề không bị tính là một bản ghi")
        void boQuaTieuDe() {
            KetQuaImportCdr kq = dichVu.nhap(csv(TIEU_DE, dongTot()));
            assertThat(kq.getTongDong()).isEqualTo(1);
            assertThat(kq.getSoThanhCong()).isEqualTo(1);
            assertThat(kq.getSoLoi()).isZero();
        }

        @Test
        @DisplayName("Dòng trắng giữa file bị bỏ qua, không thành dòng lỗi")
        void boQuaDongTrang() {
            KetQuaImportCdr kq = dichVu.nhap(csv(TIEU_DE, dongTot(), "", "   ", dongTot()));
            assertThat(kq.getTongDong()).isEqualTo(2);
            assertThat(kq.getSoThanhCong()).isEqualTo(2);
            assertThat(kq.getSoLoi()).isZero();
        }

        @Test
        @DisplayName("File không có dòng tiêu đề vẫn nhập được")
        void khongCoTieuDe() {
            KetQuaImportCdr kq = dichVu.nhap(csv(dongTot()));
            assertThat(kq.getSoThanhCong()).isEqualTo(1);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("3. Chín luật kiểm từng dòng")
    class LuatTungDong {

        /** Nhập một dòng hỏng và trả về lý do mà người dùng sẽ đọc. */
        private String lyDoLoiCua(String dongHong) {
            KetQuaImportCdr kq = dichVu.nhap(csv(TIEU_DE, dongHong));
            assertThat(kq.getSoLoi())
                    .as("dòng này phải bị từ chối: %s", dongHong)
                    .isEqualTo(1);
            assertThat(kq.getSoThanhCong()).isZero();
            return kq.getDanhSachLoi().get(0).lyDo();
        }

        @Test
        @DisplayName("Thiếu hoặc thừa cột — nói rõ đang có mấy cột")
        void saiSoCot() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,THOAI"))
                    .contains("đúng 7 cột").contains("3 cột");
            assertThat(lyDoLoiCua(dongTot() + ",thua"))
                    .contains("đúng 7 cột").contains("8 cột");
        }

        @Test
        @DisplayName("Thuê bao không có trong hệ thống")
        void thueBaoKhongTonTai() {
            assertThat(lyDoLoiCua("0999999999,0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,120,1"))
                    .contains("Không tìm thấy thuê bao 0999999999");
        }

        @Test
        @DisplayName("Thuê bao tạm ngưng hai chiều không nhận bản ghi")
        void thueBaoTamNgungHaiChieu() {
            assertThat(lyDoLoiCua("0900000002,0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,120,1"))
                    .contains("không nhận bản ghi sử dụng");
        }

        @Test
        @DisplayName("Thuê bao đã thanh lý không nhận bản ghi")
        void thueBaoDaThanhLy() {
            assertThat(lyDoLoiCua("0900000003,0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,120,1"))
                    .contains("không nhận bản ghi sử dụng");
        }

        @Test
        @DisplayName("Tạm ngưng MỘT chiều thì VẪN nhận — đối chứng cho hai ca trên")
        void tamNgungMotChieuVanNhan() {
            KetQuaImportCdr kq = dichVu.nhap(csv(TIEU_DE,
                    "0900000004,0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,120,1"));
            assertThat(kq.getSoThanhCong())
                    .as("chặn nhầm TAM_NGUNG_1C là chặn oan thuê bao vẫn gọi ra được")
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("Loại dịch vụ lạ — liệt kê ba giá trị chấp nhận được")
        void loaiDichVuLa() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,VIDEO,NOI_MANG,2026-03-15 08:30:00,120,1"))
                    .contains("THOAI, SMS hoặc DATA");
        }

        @Test
        @DisplayName("Hướng lạ — liệt kê ba giá trị chấp nhận được")
        void huongLa() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,THOAI,LIEN_TINH,2026-03-15 08:30:00,120,1"))
                    .contains("NOI_MANG, NGOAI_MANG hoặc QUOC_TE");
        }

        @Test
        @DisplayName("DATA chỉ có hướng NOI_MANG — hai hướng kia bị từ chối")
        void dataChiCoNoiMang() {
            assertThat(lyDoLoiCua(SO_TOT + ",,DATA,NGOAI_MANG,2026-03-15 08:30:00,0,1024"))
                    .contains("DATA").contains("NGOAI_MANG");
            assertThat(lyDoLoiCua(SO_TOT + ",,DATA,QUOC_TE,2026-03-15 08:30:00,0,1024"))
                    .contains("DATA").contains("QUOC_TE");
        }

        @Test
        @DisplayName("DATA + NOI_MANG thì nhận — đối chứng cho ca trên")
        void dataNoiMangVanNhan() {
            KetQuaImportCdr kq = dichVu.nhap(csv(TIEU_DE,
                    SO_TOT + ",,DATA,NOI_MANG,2026-03-15 08:30:00,0,1024"));
            assertThat(kq.getSoThanhCong()).isEqualTo(1);
        }

        @Test
        @DisplayName("Thời gian sai định dạng — nói rõ định dạng cần dùng")
        void thoiGianSaiDinhDang() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,THOAI,NOI_MANG,15/03/2026 08:30,120,1"))
                    .contains("yyyy-MM-dd HH:mm:ss");
        }

        @Test
        @DisplayName("Bản ghi phát sinh trước ngày kích hoạt thuê bao")
        void truocNgayKichHoat() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,THOAI,NOI_MANG,2025-12-31 08:30:00,120,1"))
                    .contains("sớm hơn ngày kích hoạt");
        }

        @Test
        @DisplayName("Cột số không phải số nguyên — nói rõ cột nào")
        void cotSoKhongPhaiSo() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,abc,1"))
                    .contains("thoi_luong_giay").contains("số nguyên");
        }

        @Test
        @DisplayName("Số âm bị từ chối")
        void soAm() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,-5,1"))
                    .contains("không được âm");
        }

        @Test
        @DisplayName("Cuộc gọi phải có thời lượng lớn hơn 0")
        void goiKhongCoThoiLuong() {
            assertThat(lyDoLoiCua(SO_TOT + ",0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,0,1"))
                    .contains("THOAI").contains("lớn hơn 0");
        }

        @Test
        @DisplayName("SMS thì thời lượng 0 là bình thường — đối chứng cho ca trên")
        void smsKhongCanThoiLuong() {
            KetQuaImportCdr kq = dichVu.nhap(csv(TIEU_DE,
                    SO_TOT + ",0911111111,SMS,NOI_MANG,2026-03-15 08:30:00,0,1"));
            assertThat(kq.getSoThanhCong()).isEqualTo(1);
        }
    }

    // =================================================================
    @Nested
    @DisplayName("4. File lẫn dòng tốt và dòng hỏng")
    class FileLanLon {

        @Test
        @DisplayName("Dòng hỏng không kéo theo dòng tốt — và số dòng báo lỗi đếm CẢ dòng tiêu đề")
        void dongHongKhongKeoTheoDongTot() {
            KetQuaImportCdr kq = dichVu.nhap(csv(
                    TIEU_DE,                                                     // dòng 1
                    dongTot(),                                                   // dòng 2 — tốt
                    "0999999999,0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,120,1",  // dòng 3 — hỏng
                    dongTot()));                                                 // dòng 4 — tốt

            assertThat(kq.getTongDong()).isEqualTo(3);
            assertThat(kq.getSoThanhCong()).isEqualTo(2);
            assertThat(kq.getSoLoi()).isEqualTo(1);

            KetQuaImportCdr.DongLoi loi = kq.getDanhSachLoi().get(0);
            assertThat(loi.soDong())
                    .as("số dòng phải đếm theo file gốc để người dùng mở CSV ra tìm được")
                    .isEqualTo(3);
            assertThat(loi.lyDo()).contains("Không tìm thấy thuê bao");
        }

        @Test
        @DisplayName("Tên file được giữ lại trong kết quả")
        void giuTenFile() {
            assertThat(dichVu.nhap(csv(TIEU_DE, dongTot())).getTenFile()).isEqualTo("cdr.csv");
        }
    }

    // =================================================================
    @Nested
    @DisplayName("5. Ghi xuống CSDL và ghi nhật ký")
    class GhiXuong {

        @Test
        @DisplayName("Lô dòng hợp lệ được đẩy xuống batchUpdate đúng một lần")
        void loHopLeDuocGhi() {
            dichVu.nhap(csv(TIEU_DE, dongTot(), dongTot()));
            verify(jdbcTemplate).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
        }

        @Test
        @DisplayName("File toàn dòng hỏng thì KHÔNG gọi batchUpdate lần nào")
        void toanDongHongThiKhongGhi() {
            KetQuaImportCdr kq = dichVu.nhap(csv(TIEU_DE,
                    "0999999999,0911111111,THOAI,NOI_MANG,2026-03-15 08:30:00,120,1",
                    SO_TOT + ",0911111111,VIDEO,NOI_MANG,2026-03-15 08:30:00,120,1"));

            assertThat(kq.getSoThanhCong()).isZero();
            assertThat(kq.getSoLoi()).isEqualTo(2);
            verify(jdbcTemplate, never()).batchUpdate(anyString(), any(BatchPreparedStatementSetter.class));
        }

        @Test
        @DisplayName("Mỗi lần nhập đều ghi một dòng nhật ký, kể cả khi có dòng lỗi")
        void ghiNhatKy() {
            dichVu.nhap(csv(TIEU_DE, dongTot()));
            verify(nhatKyService).ghiNhatKy(
                    org.mockito.ArgumentMatchers.eq("IMPORT_CDR"),
                    org.mockito.ArgumentMatchers.eq("CHI_TIET_SU_DUNG"),
                    org.mockito.ArgumentMatchers.isNull(),
                    anyString());
        }
    }
}
