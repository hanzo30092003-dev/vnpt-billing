# -*- coding: utf-8 -*-
"""Kiem ba tieu chi giao dien doc duoc thang tu template.

    python scripts/kiem-giao-dien.py

  1. Moi trang co MOT dong giai thich ngay duoi tieu de.
  2. Moi trang co TOI DA MOT nut noi bat (btn-primary/btn-success dac).
  3. Moi bang rong co loi giai thich, khong con dong "khong co du lieu" cut lun.

Vi sao doc template chu khong bam thu tung trang: mot so trang chi hien duoc
khi du lieu roi vao dung tinh huong (bang rong, ky da chot...). Doc template
phu duoc het moi nhanh, ke ca nhanh chua bao gio chay trong bo du lieu mau.
Doi lai no khong thay duoc thu do trinh duyet tinh ra - co chu, vung bam -
nen hai thu do do bang trinh duyet that, ghi trong PHASE-8-REPORT.md.

Nut nam TRONG modal khong tinh vao muc 2: do la nut chinh cua chinh modal do.
"""
import io
import glob
import os
import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

GOC = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                   '..', 'src', 'main', 'resources', 'templates')

# Trang khong phai man hinh nghiep vu -> mien tieu chi 1 va 2
MIEN = {'fragments/layout.html', 'fragments/trang.html', 'bao-cao/fragments.html',
        'dang-nhap.html', 'error/400.html', 'error/403.html', 'error/404.html',
        'error/500.html'}


def bo_modal(s):
    """Cat moi khoi <div class="modal ...."> ... </div> ra khoi chuoi."""
    ra = []
    i = 0
    while True:
        m = re.search(r'<div[^>]*class="[^"]*\bmodal\b[^"]*"', s[i:])
        if not m:
            ra.append(s[i:])
            break
        dau = i + m.start()
        ra.append(s[i:dau])
        # dem do sau the div de tim dung the dong
        j = dau
        sau = 0
        while j < len(s):
            k1 = s.find('<div', j + 1)
            k2 = s.find('</div>', j + 1)
            if k2 == -1:
                j = len(s)
                break
            if k1 != -1 and k1 < k2:
                sau += 1
                j = k1
            else:
                if sau == 0:
                    j = k2 + len('</div>')
                    break
                sau -= 1
                j = k2
        i = j
    return ''.join(ra)


def quet(goc=GOC):
    goc = os.path.abspath(goc)
    cu = os.getcwd()
    os.chdir(goc)
    loi = []
    soTrang = 0
    try:
        for p in sorted(glob.glob('**/*.html', recursive=True)):
            khoa = p.replace(os.sep, '/')
            s = io.open(p, encoding='utf-8').read()
            sachChuThich = re.sub(r'<!--.*?-->', '', s, flags=re.S)

            if khoa not in MIEN:
                soTrang += 1

                # 1. dong mo ta: truc tiep, hoac uy quyen qua thanhCongCu cua bao cao
                coMoTa = ('fragments/trang ::' in sachChuThich
                          or 'mo-ta-trang' in sachChuThich
                          or 'thanhCongCu(' in sachChuThich)
                if not coMoTa:
                    loi.append('%s: khong co dong giai thich duoi tieu de' % khoa)

                # 2. so nut noi bat o muc TRANG (bo phan nam trong modal).
                #    Template duoc phep xin mien bang cach ghi mot dong
                #    "NUT-NOI-BAT-CO-Y:" kem ly do trong chu thich - de ngoai le
                #    nam ngay canh cho gay ra no, khong giau trong file kiem thu.
                ngoaiModal = bo_modal(sachChuThich)
                nut = re.findall(r'class="[^"]*\bbtn-(?:primary|success)\b[^"]*"', ngoaiModal)
                nut = [n for n in nut if 'outline' not in n]
                if len(nut) > 1 and 'NUT-NOI-BAT-CO-Y:' not in s:
                    loi.append('%s: %d nut noi bat o muc trang (toi da 1)' % (khoa, len(nut)))

            # 3. bang rong cut lun.
            #    Bo chu MAU nam trong the co th:text - do la chu xem truoc luc mo
            #    file tinh, luc chay bi thay bang gia tri that.
            khongCoMau = re.sub(r'<[^>]*th:text="[^"]*"[^>]*>[^<]*</[a-zA-Z]+>', '', sachChuThich)
            for m in re.finditer(r'(?:Không có dữ liệu|Chưa có dữ liệu)\.?', khongCoMau):
                loi.append('%s: con o rong cut lun %r' % (khoa, m.group(0)))

    finally:
        os.chdir(cu)

    if loi:
        print('  [SAI ] %d van de:' % len(loi))
        for l in loi:
            print('         ' + l)
        return 1
    print('  [DAT ] %d man hinh: deu co dong giai thich, deu <= 1 nut noi bat, '
          'khong con o rong cut lun' % soTrang)
    return 0


if __name__ == '__main__':
    sys.exit(quet(sys.argv[1] if len(sys.argv) > 1 else GOC))
