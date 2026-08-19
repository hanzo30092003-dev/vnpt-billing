package com.hanzo.billing.service;

import com.hanzo.billing.entity.BienDongSoDu;
import com.hanzo.billing.entity.ThueBao;
import com.hanzo.billing.repository.BienDongSoDuRepository;
import com.hanzo.billing.repository.ThueBaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ⭐ BẤT BIẾN SỔ CÁI SỐ DƯ — số dư hiện tại phải bằng tổng biến động đã ghi.
 *
 * <p><b>Vì sao có test này.</b> Trước Phase 5, bảng {@code nap_tien} chỉ ghi chiều nạp;
 * trừ cước không để lại vết nào nên {@code thue_bao.so_du} là một con số <i>không giải
 * thích được</i> — nhìn vào không biết nó đến từ đâu. Mục G1 tổng quát hoá bảng đó thành sổ
 * cái {@code bien_dong_so_du}, và bất biến dưới đây chính là thứ biến "sổ cái" từ một cái
 * tên thành một cam kết kiểm được:</p>
 *
 * <pre>so_du = SUM(nạp + điều chỉnh) − SUM(trừ)</pre>
 *
 * <p>Kiểm <b>toàn bộ</b> thuê bao chứ không lấy mẫu (chuẩn làm việc 43.2), và cộng dồn bằng
 * chính {@link com.hanzo.billing.enums.LoaiBienDongSoDu#apDauCho} mà mã nghiệp vụ dùng —
 * không chép lại quy tắc dấu vào test. Chép lại thì khi quy tắc đổi, test vẫn xanh theo quy
 * tắc cũ (đúng kiểu mù mà {@code KiemTraDoPhuBangGiaTest} sinh ra để tránh).</p>
 *
 * <p><b>Điều kiện chạy:</b> cần MySQL đang chạy tại {@code localhost:3306}, giống
 * {@code SchemaValidationTest}.</p>
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=none"
})
@DisplayName("Bất biến sổ cái số dư")
class KiemTraSoCaiSoDuTest {

    @Autowired
    private ThueBaoRepository thueBaoRepository;

    @Autowired
    private BienDongSoDuRepository bienDongSoDuRepository;

    @Test
    @DisplayName("1. Với MỌI thuê bao, so_du bằng tổng biến động trong sổ cái")
    void moiThueBao_soDuKhopSoCai() {
        Map<Long, BigDecimal> tongTheoThueBao = new HashMap<>();
        for (BienDongSoDu bienDong : bienDongSoDuRepository.findAll()) {
            tongTheoThueBao.merge(bienDong.getThueBao().getId(),
                    bienDong.getSoTienCoDau(), BigDecimal::add);
        }

        List<ThueBao> tatCa = thueBaoRepository.findAll();
        List<String> lech = new ArrayList<>();

        for (ThueBao thueBao : tatCa) {
            BigDecimal soDu = thueBao.getSoDu() == null ? BigDecimal.ZERO : thueBao.getSoDu();
            BigDecimal theoSoCai = tongTheoThueBao.getOrDefault(thueBao.getId(), BigDecimal.ZERO);

            // compareTo chứ không equals: 285000 và 285000.00 khác scale nhưng cùng giá trị
            if (soDu.compareTo(theoSoCai) != 0) {
                lech.add(thueBao.getSoThueBao()
                        + " (id " + thueBao.getId() + "): so_du = " + soDu
                        + " nhưng sổ cái cộng ra " + theoSoCai
                        + " — chênh " + soDu.subtract(theoSoCai));
            }
        }

        assertThat(lech)
                .as("Đã kiểm %d thuê bao trên %d dòng sổ cái. Thuê bao dưới đây có số dư "
                        + "không giải thích được bằng các biến động đã ghi — hoặc có chỗ sửa "
                        + "thẳng thue_bao.so_du mà quên ghi sổ, hoặc dòng sổ cái sai dấu.%n  %s",
                        tatCa.size(), bienDongSoDuRepository.count(),
                        String.join("\n  ", lech))
                .isEmpty();
    }

    /**
     * Mỗi dòng sổ cái phải tự nhất quán: {@code so_du_sau − so_du_truoc} đúng bằng số tiền
     * đã áp dấu.
     *
     * <p>Test 1 kiểm ở mức <b>tổng</b> nên hai dòng sai ngược dấu sẽ triệt tiêu nhau và lọt
     * qua (chuẩn làm việc 43.3). Test này kiểm <b>từng dòng</b>, và nó cũng là thứ bảo đảm
     * hai cột ảnh chụp số dư dùng được để đối soát ngược chứ không phải trang trí.</p>
     */
    @Test
    @DisplayName("2. Từng dòng sổ cái: so_du_sau − so_du_truoc khớp số tiền đã áp dấu")
    void tungDong_anhChupSoDuKhopSoTien() {
        List<BienDongSoDu> tatCa = bienDongSoDuRepository.findAll();
        List<String> lech = new ArrayList<>();

        for (BienDongSoDu bienDong : tatCa) {
            if (bienDong.getSoDuTruoc() == null || bienDong.getSoDuSau() == null) {
                lech.add("id " + bienDong.getId() + ": thiếu ảnh chụp số dư");
                continue;
            }
            BigDecimal chenh = bienDong.getSoDuSau().subtract(bienDong.getSoDuTruoc());
            if (chenh.compareTo(bienDong.getSoTienCoDau()) != 0) {
                lech.add("id " + bienDong.getId()
                        + " (" + bienDong.getLoaiBienDong() + "): "
                        + bienDong.getSoDuTruoc() + " -> " + bienDong.getSoDuSau()
                        + " lệch " + chenh + " nhưng so_tien có dấu là "
                        + bienDong.getSoTienCoDau());
            }
        }

        assertThat(lech)
                .as("Đã kiểm %d dòng sổ cái. Dòng dưới đây có ảnh chụp số dư mâu thuẫn với "
                        + "số tiền ghi trên chính nó:%n  %s",
                        tatCa.size(), String.join("\n  ", lech))
                .isEmpty();
    }
}
