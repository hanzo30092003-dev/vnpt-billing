package com.hanzo.billing.service.rating;

import com.hanzo.billing.entity.ChiTietSuDung;
import com.hanzo.billing.entity.GoiCuoc;
import com.hanzo.billing.enums.HuongCuocGoi;

/**
 * Quỹ ưu đãi của một thuê bao trong một kỳ cước.
 *
 * <p>Lớp giữ trạng thái: khởi tạo từ gói cước, rồi trừ dần theo từng bản ghi CDR được đưa
 * vào theo <b>thứ tự thời gian</b>. Không phụ thuộc CSDL nên kiểm thử được độc lập.</p>
 *
 * <h2>⚠️ So sánh ở đơn vị NHỎ NHẤT, không quy sản lượng lên đơn vị của quỹ</h2>
 *
 * <p>Gói khai ưu đãi bằng <b>phút</b> và <b>MB</b>, còn CDR lưu bằng <b>giây</b> và
 * <b>KB</b>. Có hai cách khớp hai đơn vị đó, và chúng <b>không</b> cho cùng kết quả:</p>
 *
 * <ol>
 *   <li>Quy từng bản ghi lên đơn vị của quỹ, làm tròn lên: {@code Σ ceil(giây/60)}</li>
 *   <li>Quy quỹ xuống đơn vị của bản ghi, phép nhân đúng: so {@code Σ giây} với
 *       {@code phút × 60}</li>
 * </ol>
 *
 * <p>Lớp này dùng <b>cách 2</b>. Cách 1 làm tròn lên ở <i>từng</i> bản ghi rồi cộng dồn,
 * nên <b>ăn mòn quỹ của khách</b>: đo trên dữ liệu mẫu, cách 1 thổi tổng phút thoại lên
 * <b>10,97%</b> so với thực tế, và làm 4 thuê bao bị coi là vượt quỹ trong khi chưa hề
 * vượt. Ví dụ cụ thể: thuê bao 0832345622 gọi 37 cuộc nội mạng tổng 5351 giây — đúng
 * <b>89,2 phút</b>, còn nguyên trong quỹ 100 phút — nhưng cách 1 cộng thành 105 phút và
 * kết luận đã vượt.</p>
 *
 * <p>Đây chính là điều quyết định 5.2 đã chốt ở kế hoạch: <i>"ưu đãi là ưu đãi của khách,
 * không nên bị hao thêm vì quy tắc làm tròn của nhà mạng"</i>. So ở đơn vị nhỏ nhất thì
 * <b>không có phép làm tròn nào</b>, nên không có gì để tranh cãi. Xem
 * {@code docs/PHASE-4-REPORT.md} mục 23.</p>
 *
 * <h2>Quy tắc trừ quỹ</h2>
 *
 * <p>Bản ghi nằm <b>trọn</b> trong quỹ còn lại thì được miễn phí và trừ quỹ. Bản ghi làm
 * vượt quỹ thì bị thu tiền <b>đầy đủ</b> và quỹ <b>không</b> bị trừ — bản ghi không bị cắt
 * đôi (quyết định 5.3), vì mỗi CDR chỉ có một cột {@code cuoc_phi} và một cờ
 * {@code mien_phi}, cắt đôi thì bản ghi tự mâu thuẫn.</p>
 *
 * <p>Sau bản ghi làm vượt, quỹ đó coi như <b>đã đóng</b>: mọi bản ghi sau đều thu tiền, kể
 * cả bản ghi ngắn vốn vẫn còn lọt. Nếu không đóng quỹ thì kết quả phụ thuộc vào việc bản
 * ghi nào tình cờ nhỏ hơn phần dư, rất khó giải thích cho khách hàng.</p>
 *
 * <p>Bốn quỹ <b>độc lập</b> với nhau: hết quỹ data không ảnh hưởng quỹ SMS.</p>
 *
 * <p>Cuộc gọi và tin nhắn <b>quốc tế không có ưu đãi</b> — luôn thu tiền và không đụng tới
 * quỹ nào. Ưu đãi <b>không prorate</b> theo ngày kích hoạt (quyết định 5.6): thuê bao hòa
 * mạng giữa kỳ vẫn được trọn quỹ tháng.</p>
 */
public final class UuDaiGoiCuoc {

    /** Quỹ nội mạng, tính bằng GIÂY. */
    private final Quy giayNoiMang;

    /** Quỹ ngoại mạng, tính bằng GIÂY. */
    private final Quy giayNgoaiMang;

    /** Quỹ tin nhắn, tính bằng TIN. */
    private final Quy tinNhan;

    /** Quỹ dữ liệu, tính bằng KB. */
    private final Quy kbDuLieu;

    public UuDaiGoiCuoc(GoiCuoc goi) {
        this.giayNoiMang = new Quy(DonViCuoc.phutSangGiay(khongNull(goi.getPhutNoiMangMienPhi())));
        this.giayNgoaiMang = new Quy(DonViCuoc.phutSangGiay(khongNull(goi.getPhutNgoaiMangMienPhi())));
        this.tinNhan = new Quy(khongNull(goi.getSmsMienPhi()));
        this.kbDuLieu = new Quy(DonViCuoc.mbSangKb(khongNull(goi.getDataMienPhiMb())));
    }

    /**
     * Đưa một bản ghi vào xét ưu đãi.
     *
     * <p><b>Có tác dụng phụ:</b> nếu bản ghi nằm trong ưu đãi thì quỹ tương ứng bị trừ.
     * Phải gọi đúng một lần cho mỗi bản ghi, theo thứ tự thời gian tăng dần.</p>
     *
     * @return true nếu bản ghi được miễn phí
     */
    public boolean namTrongUuDai(ChiTietSuDung cdr) {
        // Quốc tế không có ưu đãi. Đặt trước mọi thứ khác để không lỡ trừ nhầm quỹ.
        if (cdr.getHuong() == HuongCuocGoi.QUOC_TE) {
            return false;
        }
        return switch (cdr.getLoaiDichVu()) {
            // Sản lượng THÔ: giây với thoại, KB với data, tin với SMS — không quy đổi
            case THOAI -> quyThoai(cdr.getHuong()).thu(khongNull(cdr.getThoiLuongGiay()));
            case SMS -> tinNhan.thu(khongNull(cdr.getSoLuong()));
            case DATA -> kbDuLieu.thu(khongNull(cdr.getSoLuong()));
        };
    }

    private Quy quyThoai(HuongCuocGoi huong) {
        return huong == HuongCuocGoi.NOI_MANG ? giayNoiMang : giayNgoaiMang;
    }

    // ---------- Số dư còn lại, phục vụ hiển thị và kiểm thử ----------

    public long conLaiGiayNoiMang() {
        return giayNoiMang.conLai();
    }

    public long conLaiGiayNgoaiMang() {
        return giayNgoaiMang.conLai();
    }

    public long conLaiTinNhan() {
        return tinNhan.conLai();
    }

    public long conLaiKb() {
        return kbDuLieu.conLai();
    }

    private static long khongNull(Integer giaTri) {
        return giaTri == null ? 0L : giaTri;
    }

    /**
     * Một quỹ ưu đãi: số dư còn lại và cờ đã đóng.
     *
     * <p>Cờ {@code daDong} là thứ phân biệt "hết quỹ" với "còn dư nhưng bản ghi quá lớn".
     * Không có nó thì một bản ghi ngắn phát sinh sau bản ghi làm vượt vẫn lọt vào ưu đãi,
     * và khách hàng không thể hiểu nổi vì sao cuộc gọi dài thì mất tiền còn cuộc ngắn ngay
     * sau đó lại miễn phí.</p>
     */
    private static final class Quy {

        private long conLai;
        private boolean daDong;

        Quy(long banDau) {
            this.conLai = Math.max(0L, banDau);
            // Gói không có ưu đãi loại này thì quỹ đóng ngay từ đầu
            this.daDong = banDau <= 0L;
        }

        boolean thu(long luong) {
            if (daDong || luong > conLai) {
                daDong = true;
                return false;
            }
            conLai -= luong;
            return true;
        }

        long conLai() {
            return conLai;
        }
    }
}
