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

        // Nut da mo modal - can nho de TRA TIEU DIEM ve dung cho cu luc dong.
        //
        // Do o viec V6: bam Esc dong modal thi tieu diem nam lai tren nut "Dong y"
        // CUA CHINH MODAL VUA AN DI. Lan Tab ke tiep bat dau lai tu dau trang, nen
        // nguoi dung ban phim mat cho dang dung - giua mot bang 55 dong hoa don thi
        // do la mat that. Bootstrap tu tra tieu diem khi modal mo bang
        // data-bs-toggle, nhung o day modal mo bang ma nen no khong biet nut nao goi.
        var nutDaMo = null;

        document.querySelectorAll('[data-xac-nhan]').forEach(function (btn) {
            btn.addEventListener('click', function (e) {
                e.preventDefault();
                nutDaMo = btn;
                formDangCho = btn.closest('form');
                modalEl.querySelector('.modal-title').textContent =
                    btn.getAttribute('data-xac-nhan-tieu-de') || 'Xác nhận thao tác';
                modalEl.querySelector('.noi-dung-xac-nhan').textContent =
                    btn.getAttribute('data-xac-nhan');
                modal.show();
            });
        });

        modalEl.addEventListener('hidden.bs.modal', function () {
            if (nutDaMo) {
                nutDaMo.focus();
                nutDaMo = null;
            }
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

    // -----------------------------------------------------------------
    // Sidebar thu gon tren man hinh hep (Phase 7 muc B4).
    //
    // Nut chi hien duoi 992px (xem app.css). Lop phu mo phia sau vua de
    // lam ro sidebar dang che noi dung, vua la vung bam de dong - tren
    // dien thoai khong co phim Esc.
    // -----------------------------------------------------------------
    var sidebar = document.querySelector('.sidebar');
    var nutMo = document.getElementById('nutMoSidebar');
    if (sidebar && nutMo) {
        var lopPhu = null;

        function dongSidebar() {
            sidebar.classList.remove('dang-mo');
            if (lopPhu) {
                lopPhu.remove();
                lopPhu = null;
            }
        }

        nutMo.addEventListener('click', function () {
            if (sidebar.classList.contains('dang-mo')) {
                dongSidebar();
                return;
            }
            sidebar.classList.add('dang-mo');
            lopPhu = document.createElement('div');
            lopPhu.className = 'lop-phu-sidebar';
            lopPhu.addEventListener('click', dongSidebar);
            document.body.appendChild(lopPhu);
        });

        // Bam mot muc menu thi dong luon - neu khong, sidebar van che noi
        // dung cua chinh trang vua mo
        sidebar.querySelectorAll('.nav-item').forEach(function (a) {
            a.addEventListener('click', dongSidebar);
        });

        // Keo rong man hinh tro lai thi bo trang thai mo, tranh ket lop phu
        window.addEventListener('resize', function () {
            if (window.innerWidth >= 992) {
                dongSidebar();
            }
        });
    }
});
