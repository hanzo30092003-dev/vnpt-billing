package com.hanzo.billing.service.impl;

import com.hanzo.billing.repository.KhachHangRepository;
import com.hanzo.billing.service.SinhMaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SinhMaServiceImpl implements SinhMaService {

    /** Tiền tố mã khách hàng. */
    static final String TIEN_TO_KH = "KH";

    /** Số chữ số của phần số trong mã khách hàng. */
    static final int SO_CHU_SO_KH = 6;

    private final KhachHangRepository khachHangRepository;

    /**
     * Sinh mã khách hàng kế tiếp.
     *
     * <p><b>Cách làm:</b> lấy mã lớn nhất đang có, <b>bỏ tiền tố "KH"</b> rồi
     * {@link Integer#parseInt} phần còn lại để ra số thứ tự, cộng 1 và định dạng lại
     * cho đủ 6 chữ số.</p>
     *
     * <p><b>Vì sao không so sánh chuỗi để tìm mã lớn nhất theo cách thủ công:</b>
     * so sánh chuỗi chỉ cho kết quả đúng khi mọi mã có cùng độ dài phần số. Ở Phase 2
     * dữ liệu mẫu dùng 4 chữ số ({@code KH0050}) còn mã tự sinh dùng 6 chữ số
     * ({@code KH000051}), khi đó {@code "KH0050" > "KH000051"} theo thứ tự chuỗi và
     * hệ thống sẽ sinh ra mã trùng. Phase 3 đã chuẩn hoá toàn bộ về 6 chữ số, nhưng
     * việc ép kiểu số vẫn là cách duy nhất đúng về mặt logic.</p>
     *
     * <p>Dùng số thứ tự lớn nhất thay vì đếm số bản ghi: nếu đếm thì sau khi có khách
     * hàng bị xoá, mã mới sẽ trùng với mã đã từng cấp.</p>
     */
    @Override
    @Transactional(readOnly = true)
    public String sinhMaKhachHang() {
        String maLonNhat = khachHangRepository.timMaKhachHangLonNhat();
        int soTiepTheo = tachSoThuTu(maLonNhat) + 1;
        return TIEN_TO_KH + String.format("%0" + SO_CHU_SO_KH + "d", soTiepTheo);
    }

    /**
     * Tách phần số ra khỏi mã khách hàng.
     *
     * @param ma mã dạng {@code KH000050}; null hoặc rỗng nghĩa là chưa có mã nào
     * @return số thứ tự, hoặc 0 nếu chưa có mã / mã không đúng định dạng
     */
    private int tachSoThuTu(String ma) {
        if (ma == null || ma.isBlank()) {
            return 0;
        }
        String daCat = ma.trim();
        if (!daCat.startsWith(TIEN_TO_KH)) {
            return 0;
        }
        String phanSo = daCat.substring(TIEN_TO_KH.length());
        try {
            // parseInt tự bỏ các số 0 ở đầu: "000007" -> 7
            return Integer.parseInt(phanSo);
        } catch (NumberFormatException e) {
            // Mã cũ không đúng quy ước thì coi như chưa có, để không làm vỡ luồng tạo mới
            return 0;
        }
    }
}
