# -*- coding: utf-8 -*-
"""Kiem hai dieu kien toi thieu de dung duoc bang BAN PHIM va trinh doc man hinh.

    python scripts/kiem-ban-phim.py

  1. Moi nut / lien ket phai co TEN DOC DUOC.
     Nut chi co bieu tuong (<i class="bi ...">) ma khong co chu, khong co
     aria-label thi trinh doc man hinh doc ra "button" - khong noi duoc no lam gi.
     Chu title mot minh KHONG du: no la goi y chuot, nhieu trinh doc man hinh bo qua,
     va nguoi dung ban phim khong bao gio thay no.

  2. Layout phai co DUONG TAT BO QUA MENU va dich den cua no.
     Do o viec V6: khong co duong tat thi phai bam Tab 20 lan moi toi o nhap dau tien
     cua man hinh them khach hang, va phai tra lai tung ay lan o MOI trang.

VI SAO CAN PHEP KIEM NAY chu khong chi sua mot lan:
20 cho thieu ten duoc sua o viec V6. Man hinh them sau do se lai thieu, vi khong
co gi nhac. Doi chung da chay: tren ban template TRUOC khi sua, phep kiem bao dung
20 cho; sau khi sua bao 0.

GIOI HAN da biet - phep kiem nay KHONG thay duoc:
  - thu tu Tab co khop voi thu tu nhin thay khong
  - hop thoai co nhot tieu diem khong
  - Enter co kich hoat duoc nut khong
Ba thu do phai do bang trinh duyet that; ket qua do ghi trong PHASE-8-REPORT.md.
"""
import io
import glob
import os
import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

MAC_DINH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        '..', 'src', 'main', 'resources', 'templates')


def ten_doc_duoc(thuoc_tinh, ben_trong):
    """Nut co gi de trinh doc man hinh doc ra khong."""
    if 'aria-label' in thuoc_tinh:
        return True
    # Bo the bieu tuong roi xem con chu nao khong
    chu = re.sub(r'<i\b[^>]*>.*?</i>', '', ben_trong, flags=re.S)
    chu = re.sub(r'<[^>]+>', '', chu).strip()
    return bool(chu) or 'th:text' in thuoc_tinh


def quet(goc):
    goc = os.path.abspath(goc)
    if not os.path.isdir(goc):
        print('Khong thay thu muc: %s' % goc)
        return 2

    loi = []
    so_nut = 0
    files = sorted(glob.glob(os.path.join(goc, '**/*.html'), recursive=True))

    for p in files:
        khoa = os.path.relpath(p, goc).replace(os.sep, '/')
        s = re.sub(r'<!--.*?-->', '', io.open(p, encoding='utf-8').read(), flags=re.S)

        for m in re.finditer(r'<(a|button)\b([^>]*)>(.*?)</\1>', s, re.S):
            so_nut += 1
            if ten_doc_duoc(m.group(2), m.group(3)):
                continue
            gon = re.sub(r'\s+', ' ', m.group(2)).strip()[:80]
            loi.append('%s: <%s> chi co bieu tuong, khong co ten doc duoc  [%s]'
                       % (khoa, m.group(1), gon))

    # Duong tat bo qua menu
    layout = os.path.join(goc, 'fragments', 'layout.html')
    if os.path.isfile(layout):
        s = io.open(layout, encoding='utf-8').read()
        if 'class="bo-qua-menu"' not in s:
            loi.append('fragments/layout.html: thieu duong tat bo qua menu')
        if 'id="noi-dung"' not in s:
            loi.append('fragments/layout.html: thieu id="noi-dung" - dich cua duong tat')
        elif 'tabindex="-1"' not in s:
            loi.append('fragments/layout.html: dich duong tat thieu tabindex="-1", '
                       'tieu diem se khong nhay vao ma chi cuon man hinh toi')
    else:
        loi.append('Khong thay fragments/layout.html')

    if loi:
        print('  [SAI ] %d van de:' % len(loi))
        for l in loi:
            print('         ' + l)
        return 1

    print('  [DAT ] %d nut/lien ket deu co ten doc duoc; co duong tat bo qua menu' % so_nut)
    return 0


if __name__ == '__main__':
    sys.exit(quet(sys.argv[1] if len(sys.argv) > 1 else MAC_DINH))
