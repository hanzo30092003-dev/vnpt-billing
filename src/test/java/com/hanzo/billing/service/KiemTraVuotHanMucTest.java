package com.hanzo.billing.service;

import com.hanzo.billing.dto.ThueBaoVuotHanMuc;
import com.hanzo.billing.entity.HoaDon;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.enums.LoaiThueBao;
import com.hanzo.billing.enums.TrangThaiThueBao;
import com.hanzo.billing.repository.HoaDonRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ NỢ VƯỢT HẠN MỨC TÍN DỤNG — kiểm trên dữ liệu thật.
 *
 * <p><b>Vì sao có tính năng này.</b> Cột {@code thue_bao.han_muc_tin_dung} có từ Phase 1: có
 * trong bảng, có trên form đăng ký, có hiển thị ở màn hình chi tiết — nhưng suốt tám phase
 * <b>không dòng mã nào đọc nó để chặn việc gì</b>. Người dùng nhập số vào đó và tin rằng hệ
 * thống đang canh giúp mình, trong khi không.</p>
 *
 * <h2>Kiểm CẢ HAI CHIỀU</h2>
 *
 * <p>Một phép kiểm chỉ xét "mọi dòng trả về đều đúng" là phép kiểm <b>một chiều</b>, và nó
 * xanh rực rỡ ngay cả khi câu truy vấn trả về danh sách rỗng. Bài học 43.5 của dự án: cái
 * nguy hiểm không phải dòng sai lọt vào, mà là dòng đúng bị bỏ sót — vì bỏ sót thì không ai
 * nhìn thấy để mà nghi ngờ.</p>
 *
 * <p>Nên ở đây kiểm hai chiều:</p>
 * <ol>
 *   <li><b>Đúng:</b> mọi thuê bao có trong danh sách đều thật sự vượt hạn mức, và đều là
 *       trả sau đang hoạt động</li>
 *   <li><b>Đủ:</b> mọi thuê bao <i>đáng</i> có mặt — tính độc lập bằng Java từ toàn bộ hóa
 *       đơn — đều có mặt, không sót ai</li>
 * </ol>
 *
 * <p>Chiều thứ hai tính lại bằng một đường hoàn toàn khác với câu truy vấn: nạp mọi hóa đơn
 * còn nợ rồi cộng nhóm trong Java. Hai đường độc lập cho cùng một tập hợp thì mới tin được.</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("Nợ vượt hạn mức tín dụng")
class KiemTraVuotHanMucTest {

    @Autowired
    private CongNoService congNoService;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private ThueBaoRepository thueBaoRepository;

    @Test
    @DisplayName("1. ĐÚNG — mọi thuê bao trong danh sách đều thật sự vượt hạn mức")
    void moiDongDeuThatSuVuotHanMuc() {
        List<ThueBaoVuotHanMuc> danhSach = congNoService.thueBaoVuotHanMuc();

        assertThat(danhSach)
                .as("Dữ liệu mẫu phải có ít nhất một thuê bao vượt hạn mức, nếu không phép "
                        + "kiểm này xanh một cách rỗng và không chứng minh được gì")
                .isNotEmpty();

        for (ThueBaoVuotHanMuc v : danhSach) {
            assertThat(v.tongNo())
                    .as("Thuê bao %s phải nợ NHIỀU HƠN hạn mức mới được vào danh sách",
                            v.soThueBao())
                    .isGreaterThan(v.hanMuc());

            assertThat(v.hanMuc().signum())
                    .as("Thuê bao %s có hạn mức 0 — hạn mức 0 nghĩa là CHƯA ĐẶT, không phải "
                            + "không cho nợ đồng nào, nên không được vào danh sách",
                            v.soThueBao())
                    .isPositive();

            assertThat(v.vuot())
                    .as("Số vượt phải bằng đúng tổng nợ trừ hạn mức")
                    .isEqualByComparingTo(v.tongNo().subtract(v.hanMuc()));
        }
    }

    @Test
    @DisplayName("2. ĐỦ — không sót thuê bao nào đáng có mặt (tính lại bằng đường khác)")
    void khongSotThueBaoNao() {
        // Đường thứ hai: nạp mọi hóa đơn còn nợ rồi cộng nhóm TRONG JAVA.
        // Cố ý không dùng lại câu truy vấn đang kiểm — tính lại bằng chính nó thì
        // nó chỉ chứng minh chính nó (chuẩn làm việc 43.6).
        Map<Long, BigDecimal> noTheoThueBao = new HashMap<>();
        Map<Long, ThueBao> thueBaoTheoId = new HashMap<>();

        for (HoaDon h : hoaDonRepository.timConNo(null)) {
            if (h.getConNo() == null || h.getConNo().signum() <= 0) {
                continue;
            }
            ThueBao tb = h.getThueBao();
            thueBaoTheoId.put(tb.getId(), tb);
            noTheoThueBao.merge(tb.getId(), h.getConNo(), BigDecimal::add);
        }

        Set<Long> dangLe = noTheoThueBao.entrySet().stream()
                .filter(e -> {
                    ThueBao tb = thueBaoTheoId.get(e.getKey());
                    BigDecimal hanMuc = tb.getHanMucTinDung();
                    return tb.getLoaiThueBao() == LoaiThueBao.TRA_SAU
                            && tb.getTrangThai() == TrangThaiThueBao.HOAT_DONG
                            && hanMuc != null && hanMuc.signum() > 0
                            && e.getValue().compareTo(hanMuc) > 0;
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Set<Long> thucTe = congNoService.thueBaoVuotHanMuc().stream()
                .map(ThueBaoVuotHanMuc::thueBaoId)
                .collect(Collectors.toSet());

        assertThat(thucTe)
                .as("Tập thuê bao vượt hạn mức tính bằng truy vấn phải TRÙNG KHỚP tập tính "
                        + "độc lập trong Java — không thừa ai, không sót ai")
                .isEqualTo(dangLe);
    }

    @Test
    @DisplayName("3. Thuê bao trả trước KHÔNG bao giờ vào danh sách")
    void traTruocKhongVaoDanhSach() {
        // Trả trước tiêu tiền đã nạp nên không có khái niệm cho nợ. Nếu một thuê bao
        // trả trước lọt vào đây thì hoặc câu truy vấn sai, hoặc dữ liệu sai — cả hai
        // đều đáng dừng lại xem.
        //
        // ⚠️ Lấy danh sách trả trước từ BẢNG THUÊ BAO, không phải từ bảng hóa đơn.
        // Bản đầu của phép kiểm này lọc thuê bao trả trước ra từ hoa_don — và tập đó
        // LUÔN RỖNG, vì thuê bao trả trước không hề có hóa đơn tháng (quyết định 5.4:
        // cước của họ trừ thẳng vào số dư). Một phép kiểm "không chứa phần tử nào của
        // tập rỗng" thì không bao giờ phát hiện được gì.
        Set<Long> traTruoc = thueBaoRepository.findAll().stream()
                .filter(tb -> tb.getLoaiThueBao() == LoaiThueBao.TRA_TRUOC)
                .map(ThueBao::getId)
                .collect(Collectors.toSet());

        assertThat(traTruoc)
                .as("Dữ liệu mẫu phải có thuê bao trả trước, nếu không phép kiểm này rỗng")
                .isNotEmpty();

        Set<Long> trongDanhSach = congNoService.thueBaoVuotHanMuc().stream()
                .map(ThueBaoVuotHanMuc::thueBaoId)
                .collect(Collectors.toSet());

        assertThat(trongDanhSach)
                .as("Danh sách vượt hạn mức không được chứa thuê bao trả trước")
                .doesNotContainAnyElementsOf(traTruoc);
    }
}
