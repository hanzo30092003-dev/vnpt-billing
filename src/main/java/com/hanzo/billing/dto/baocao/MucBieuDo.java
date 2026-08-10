package com.hanzo.billing.dto.baocao;

/**
 * Một lát của biểu đồ tròn: nhãn và số lượng.
 *
 * <p>Dựng sẵn ở controller thay vì để template tự chiếu từ {@code Object[]} — cùng lý do
 * {@code CongNoController} dựng sẵn hai mảng cho biểu đồ tuổi nợ: bảng và biểu đồ khi đó chắc
 * chắn đọc <b>cùng một nguồn, cùng thứ tự</b>.</p>
 */
public record MucBieuDo(String nhan, long soLuong) {
}
