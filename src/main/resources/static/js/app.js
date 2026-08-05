/* Kich ban dung chung cho toan he thong */

document.addEventListener('DOMContentLoaded', function () {

    // -----------------------------------------------------------------
    // Tu an cac thong bao co lop .tu-an sau 5 giay.
    // Dung co che fade cua Bootstrap de co hieu ung mo dan thay vi bien mat dot ngot.
    // -----------------------------------------------------------------
    document.querySelectorAll('.alert.tu-an').forEach(function (alertEl) {
        setTimeout(function () {
            if (window.bootstrap && bootstrap.Alert) {
                bootstrap.Alert.getOrCreateInstance(alertEl).close();
            } else {
                alertEl.remove();
            }
        }, 5000);
    });

    // -----------------------------------------------------------------
    // Modal xac nhan dung chung.
    // Bat ky nut nao co data-xac-nhan se hien modal truoc khi gui form.
    //   data-xac-nhan       : noi dung cau hoi
    //   data-xac-nhan-tieu-de : tieu de modal (khong bat buoc)
    // -----------------------------------------------------------------
    var modalEl = document.getElementById('modalXacNhan');
    if (modalEl) {
        var modal = new bootstrap.Modal(modalEl);
        var formDangCho = null;

        document.querySelectorAll('[data-xac-nhan]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                formDangCho = btn.closest('form');
                modalEl.querySelector('.modal-title').textContent =
                    btn.getAttribute('data-xac-nhan-tieu-de') || 'Xác nhận thao tác';
                modalEl.querySelector('.noi-dung-xac-nhan').textContent =
                    btn.getAttribute('data-xac-nhan');
                modal.show();
            });
        });

        modalEl.querySelector('.nut-dong-y').addEventListener('click', function () {
            if (formDangCho) {
                danhDauDangXuLy(formDangCho);
                formDangCho.submit();
            }
        });
    }

    // -----------------------------------------------------------------
    // Chi bao "dang xu ly" cho cac thao tac cham (tinh cuoc, lap hoa don...).
    // Form khai class="form-thao-tac"; khi gui thi moi nut trong form bi vo
    // hieu hoa va nut vua bam doi thanh spinner.
    //
    // Phai xu ly o CA HAI cho: su kien submit (nut gui thang) va trong nut dong
    // y cua modal - vi form.submit() bang JavaScript KHONG kich hoat su kien
    // submit, neu chi nghe su kien thi cac thao tac co modal se khong co chi bao.
    // -----------------------------------------------------------------
    function danhDauDangXuLy(form) {
        if (form.dataset.dangXuLy === '1') {
            return;
        }
        form.dataset.dangXuLy = '1';

        // Vo hieu hoa nut cua MOI form tren trang, tranh bam hai thao tac cung luc
        document.querySelectorAll('.form-thao-tac button').forEach(function (btn) {
            btn.disabled = true;
        });

        var nutChinh = form.querySelector('button');
        if (nutChinh) {
            nutChinh.innerHTML =
                '<span class="spinner-border spinner-border-sm me-1"></span>Đang xử lý...';
        }
    }

    document.querySelectorAll('form.form-thao-tac').forEach(function (form) {
        form.addEventListener('submit', function () {
            danhDauDangXuLy(form);
        });
    });
});
