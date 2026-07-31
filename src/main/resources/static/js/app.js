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
                formDangCho.submit();
            }
        });
    }
});
