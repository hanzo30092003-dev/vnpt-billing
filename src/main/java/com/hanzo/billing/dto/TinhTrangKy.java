package com.hanzo.billing.dto;

import com.hanzo.billing.entity.KyCuoc;
import com.hanzo.billing.enums.TrangThaiKyCuoc;

/**
 * Tình trạng xử lý của một kỳ cước, dùng để quyết định nút nào được bật trên giao diện.
 *
 * <p>Trạng thái lưu trong CSDL chỉ có ba giá trị ({@code MO} / {@code DANG_TINH} /
 * {@code DA_CHOT}), không đủ để biết kỳ đang ở bước nào của quy trình. Phải đếm thêm số bản
 * ghi và số hóa đơn mới suy ra được. Gom vào một chỗ để view không phải tự suy luận.</p>
 *
 * @param soCdrChuaTinh bản ghi trong khoảng ngày của kỳ còn ở {@code CHUA_TINH}
 * @param soCdrDaTinh   bản ghi đã gán vào kỳ và đã tính cước
 */
public record TinhTrangKy(KyCuoc ky, long soCdrChuaTinh, long soCdrLoi, long soCdrDaTinh,
                          long soHoaDon, long soThanhToan, long soTruCuoc) {

    public boolean daChot() {
        return ky.getTrangThai() == TrangThaiKyCuoc.DA_CHOT;
    }

    public boolean biKet() {
        return ky.getTrangThai() == TrangThaiKyCuoc.DANG_TINH;
    }

    private boolean dangMo() {
        return ky.getTrangThai() == TrangThaiKyCuoc.MO;
    }

    /** Còn bản ghi chưa tính hoặc lỗi thì mới có việc để chạy. */
    public boolean choPhepTinhCuoc() {
        return dangMo() && (soCdrChuaTinh > 0 || soCdrLoi > 0);
    }

    /** Phải tính cước xong toàn kỳ mới được lập hóa đơn, nếu không hóa đơn sẽ thiếu tiền. */
    public boolean choPhepLapHoaDon() {
        return dangMo() && soCdrChuaTinh == 0 && soCdrDaTinh > 0 && soHoaDon == 0;
    }

    /** Đã thu tiền của khách thì không xóa hóa đơn được nữa. */
    public boolean choPhepHuyHoaDon() {
        return dangMo() && soHoaDon > 0 && soThanhToan == 0;
    }

    /**
     * Phải hủy hóa đơn <b>và</b> kết quả trừ cước trước mới hủy được kết quả tính cước.
     *
     * <p>Hai nhánh cùng một lý do: hóa đơn và dòng trừ cước đều lấy số tiền từ
     * {@code cuoc_phi} của chính các bản ghi này. Xóa {@code cuoc_phi} mà giữ chúng lại thì
     * con số đã phát hành không còn dữ liệu nào giải thích.</p>
     */
    public boolean choPhepHuyTinhCuoc() {
        return dangMo() && soHoaDon == 0 && soTruCuoc == 0 && soCdrDaTinh > 0;
    }

    /**
     * Trừ cước trả trước độc lập với lập hóa đơn — chỉ cần định giá xong.
     *
     * <p>Thuê bao trả trước không có hóa đơn tháng (quyết định 5.4), nên hai nhánh trả sau
     * và trả trước chạy song song được, không nhánh nào chờ nhánh nào.</p>
     */
    public boolean choPhepTruCuoc() {
        return dangMo() && soCdrChuaTinh == 0 && soCdrDaTinh > 0 && soTruCuoc == 0;
    }

    public boolean choPhepHuyTruCuoc() {
        return dangMo() && soTruCuoc > 0;
    }

    public boolean choPhepChotKy() {
        return dangMo() && soHoaDon > 0;
    }

    /** Tổng số bản ghi liên quan tới kỳ, để hiển thị cho dễ hình dung. */
    public long tongCdr() {
        return soCdrChuaTinh + soCdrLoi + soCdrDaTinh;
    }

    /** Câu mô tả ngắn bước tiếp theo nên làm, hiện ngay trên bảng. */
    public String buocTiepTheo() {
        if (daChot()) {
            return "Kỳ đã chốt, chỉ xem";
        }
        if (biKet()) {
            return "Kỳ đang kẹt ở trạng thái Đang tính — cần gỡ";
        }
        if (choPhepTinhCuoc()) {
            return "Chạy tính cước cho " + (soCdrChuaTinh + soCdrLoi) + " bản ghi";
        }
        if (choPhepLapHoaDon()) {
            return "Đã tính cước xong, chờ lập hóa đơn";
        }
        if (soHoaDon > 0 && choPhepTruCuoc()) {
            return "Đã lập " + soHoaDon + " hóa đơn, còn chờ trừ cước trả trước";
        }
        if (soHoaDon > 0) {
            return "Đã lập " + soHoaDon + " hóa đơn, có thể chốt kỳ";
        }
        if (choPhepTruCuoc()) {
            return "Đã tính cước xong, chờ trừ cước trả trước";
        }
        return "Chưa có bản ghi nào trong kỳ";
    }
}
