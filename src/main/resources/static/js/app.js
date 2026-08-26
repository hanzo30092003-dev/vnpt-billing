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
    // Rail dieu huong thu gon tren man hinh hep (Phase 7 muc B4).
    //
    // Nut chi hien duoi 992px (xem app.css). Lop phu mo phia sau vua de
    // lam ro rail dang che noi dung, vua la vung bam de dong - tren
    // dien thoai khong co phim Esc.
    // -----------------------------------------------------------------
    var rail = document.querySelector('.rail');
    var nutMo = document.getElementById('nutMoRail');
    if (rail && nutMo) {
        var lopPhu = null;

        function dongRail() {
            rail.classList.remove('dang-mo');
            if (lopPhu) {
                lopPhu.remove();
                lopPhu = null;
            }
        }

        nutMo.addEventListener('click', function () {
            if (rail.classList.contains('dang-mo')) {
                dongRail();
                return;
            }
            rail.classList.add('dang-mo');
            lopPhu = document.createElement('div');
            lopPhu.className = 'lop-phu-rail';
            lopPhu.addEventListener('click', dongRail);
            document.body.appendChild(lopPhu);
        });

        // Bam mot muc menu thi dong luon - neu khong, rail van che noi
        // dung cua chinh trang vua mo
        rail.querySelectorAll('.nav-item').forEach(function (a) {
            a.addEventListener('click', dongRail);
        });

        // Keo rong man hinh tro lai thi bo trang thai mo, tranh ket lop phu
        window.addEventListener('resize', function () {
            if (window.innerWidth >= 992) {
                dongRail();
            }
        });
    }
});

/* =====================================================================
   BANG MAU BIEU DO - MOT NGUON DUY NHAT
   =====================================================================
   Truoc dot nay, moi man hinh co bieu do tu go bo ma mau cua rieng no:
   trang chu mot bo, bao cao doanh thu ky mot bo, bao cao thue bao mot bo.
   Bay bo mau nam rai o 7 file, nen doi bang mau la phai nho sua du 7 cho -
   va lan nay quen dung 4 cho, phat hien ra khi doi chieu trang chu voi
   phan con lai cua phan mem.

   Doc thang tu bien CSS thi bang mau chi con MOT nguon: :root trong
   app.css. Doi mau o do la ca 7 man hinh doi theo.

   window.MAU la mot object thuong (khong phai ham) vi Chart.js can gia tri
   ngay luc dung; getComputedStyle chay duoc tu luc DOMContentLoaded.
   ===================================================================== */
(function () {
    function doc(ten) {
        return getComputedStyle(document.documentElement).getPropertyValue(ten).trim();
    }

    /*
       CHAY NGAY, KHONG DOI DOMContentLoaded.

       Script noi dung cua tung trang nam TRUOC the <script src=app.js> trong
       body, nen listener DOMContentLoaded cua no dang ky truoc va chay truoc.
       Neu bang mau dat trong listener cua app.js thi luc trang ve bieu do,
       window.MAU van con undefined.

       Chay ngay thi an toan: CSS o <head> da phan tich xong tu lau, con
       Chart.defaults chi can dat xong truoc khi bieu do dau tien duoc tao -
       tuc truoc DOMContentLoaded.
    */
    (function () {
        window.MAU = {
            cham: doc('--nhan'),          // so lieu trung tinh, mau chu dao
            chamNhat: doc('--nhan-nhat'),
            xanh: doc('--tin-on'),        // tien da vao
            vang: doc('--tin-canh'),      // can theo doi
            do: doc('--tin-loi'),         // tien chua vao, qua han
            muc: doc('--muc'),            // mau nen toi
            xam: doc('--chu-mo'),
            vach: doc('--vach')
        };
        // Day du 5 mau phan biet duoc cho bieu do tron nhieu phan
        window.MAU.day = [window.MAU.cham, window.MAU.xanh, window.MAU.vang,
                          window.MAU.do, window.MAU.muc];

        if (window.Chart) {
            Chart.defaults.font.family = doc('--chu-giao-dien') || 'system-ui, sans-serif';
            // D5 — 12px la chu NHO NHAT tren man hinh, va no nam tren dung thu
            // hoi dong nhin lau nhat khi chieu len tuong. Do o 1024x768: bieu do
            // doanh thu 699x233px, hai bieu do tron moi cai 324x280px.
            // Khong len 16: chu giai se an vao vung ve cua bieu do tron 324px.
            Chart.defaults.font.size = 14;
            Chart.defaults.color = window.MAU.xam;
            Chart.defaults.plugins.legend.labels.boxWidth = 12;
            Chart.defaults.plugins.legend.labels.boxHeight = 12;
        }
    }());
}());

/* =====================================================================
   DONG BO CHIEU CAO THANH MAY VAO BIEN CSS
   =====================================================================
   Rail dinh o top = chieu cao that cua thanh may. Neu lech thi hoac ho ra
   mot vach, hoac rail chui xuong duoi thanh may - va do lech chi lo ra khi
   cuon, tuc rat de sot.

   GHI VAO --thanh-may-thuc, KHONG ghi vao --thanh-may-cao. Bien
   --thanh-may-cao la thu .thanh-may doc lam min-height; ghi chieu cao do
   duoc nguoc lai vao no la tao vong phan hoi: thanh cao len mot lan (vi du
   luc cua so hep lam chu xuong dong) thi min-height khoa luon o do va
   khong bao gio co lai. Da dinh dung loi nay mot lan - thanh may bi khoa
   o 375px, chiem gan nua khung.

   CSS khai top: var(--thanh-may-thuc, var(--thanh-may-cao)) nen JavaScript
   hong thi chi lech vai pixel chu khong vo bo cuc.
   ===================================================================== */
(function () {
    var thanhMay = document.querySelector('.thanh-may');
    if (!thanhMay) {
        return;
    }

    function dongBo() {
        document.documentElement.style.setProperty(
            '--thanh-may-thuc', thanhMay.offsetHeight + 'px');
    }

    dongBo();
    window.addEventListener('resize', dongBo);

    /* Nguyen nhan lech LON nhat va de doan nhat la phong chu: luc script nay
       chay, Be Vietnam Pro chua tai xong nen thanh may do duoc 58px; tai xong
       thi no cao 73px. Neo thang vao fonts.ready thay vi trong cho mot co che
       chung chung bat duoc.

       Do duoc: khong co moc nay thi rail dinh o 58px trong khi thanh may cao
       73px - 15px dau cua rail chui xuong duoi thanh, dung muc "Trang chu". */
    if (document.fonts && document.fonts.ready) {
        document.fonts.ready.then(dongBo);
    }
    // Chot chan cuoi: anh, bang, moi thu da xong
    window.addEventListener('load', dongBo);

    /* ResizeObserver la lop bo sung, KHONG duoc la cho dua duy nhat: no chi
       chay khi trinh duyet con chay vong ve. Trong khung xem tich hop cua
       Claude Code no khong chay lan nao - da do. */
    if (window.ResizeObserver) {
        new ResizeObserver(dongBo).observe(thanhMay);
    }
}());
