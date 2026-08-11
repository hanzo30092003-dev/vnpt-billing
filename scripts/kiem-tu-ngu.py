# -*- coding: utf-8 -*-
"""Kiem: khong con tu ky thuat trong chu NGUOI DUNG NHIN THAY.

    python scripts/kiem-tu-ngu.py            # quet templates/ cua du an
    python scripts/kiem-tu-ngu.py <thu-muc>  # quet mot thu muc khac

Vi sao khong dung `grep CDR templates/`:

  1. `th:href="@{/cdr}"` la DUONG DAN, doi la gay 404. Dac ta cam doi route.
  2. `${k.soCdrXuLy}` la TEN BIEN. Dac ta yeu cau giu nguyen ten bien.
  3. `class="bang-cdr"` la ten lop CSS.
  4. Chu thich `<!--/* ... */-->` va khoi <script> khong ai doc ngoai lap trinh vien.

Grep tho bao dong o ca bon cho tren. Ban dau cua chinh phep kiem nay bao 16 loi
ma khong loi nao la that - dung loai bao dong lam hong long tin vao moi phep kiem
con lai (bai hoc 43.5). Ban nay bo bon nhom tren truoc khi quet.

DOI CHUNG da chay: tren ban template TRUOC khi sua, phep kiem nay bao dung 16 cho
that; tren ban sau khi sua bao 0. Tuc no co kEU khi co loi.
"""
import io
import glob
import os
import re
import sys

sys.stdout.reconfigure(encoding='utf-8')

MAC_DINH = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        '..', 'src', 'main', 'resources', 'templates')

TU = ['CDR', 'rating', 'billing', 'prorate', 'idempotent', 'snapshot', 'batch']

# Thuoc tinh KHONG chua chu hien thi -> xoa gia tri truoc khi quet.
# th:text, placeholder, title, aria-label CO chua chu hien thi -> giu lai.
KY_THUAT = (r'th:(?:href|action|src|field|object|each|if|unless|with|value|classappend|'
            r'attr|fragment|replace|insert|include|remove|selected|checked|disabled|'
            r'data-[a-zA-Z-]+)|href|src|id|class|name|for|type|colspan|rowspan|'
            r'step|min|max|method|enctype|accept|role|tabindex|aria-controls|'
            r'aria-labelledby|data-bs-[a-zA-Z-]+')


def quet(goc):
    goc = os.path.abspath(goc)
    if not os.path.isdir(goc):
        print('Khong thay thu muc: %s' % goc)
        return 2

    cu = os.getcwd()
    os.chdir(goc)
    try:
        files = sorted(glob.glob('**/*.html', recursive=True))
        hit = []
        for p in files:
            s = io.open(p, encoding='utf-8').read()
            s = re.sub(r'<!--.*?-->', '', s, flags=re.S)
            s = re.sub(r'<script.*?</script>', '', s, flags=re.S | re.I)
            s = re.sub(r'<style.*?</style>', '', s, flags=re.S | re.I)
            s = re.sub(r'\b(?:' + KY_THUAT + r')="[^"]*"', '', s)
            s = re.sub(r'[\$\*@#]\{[^}]*\}', '', s)
            for i, dong in enumerate(s.split('\n'), 1):
                for t in TU:
                    if re.search(t, dong, re.I):
                        hit.append((p, i, t, dong.strip()[:110]))
    finally:
        os.chdir(cu)

    if hit:
        print('  [SAI ] Con %d cho co tu ky thuat trong chu hien thi:' % len(hit))
        for p, i, t, d in hit:
            print('         %s:%d  [%s]  %s' % (p, i, t, d))
        return 1

    print('  [DAT ] Khong con tu ky thuat nao trong chu hien thi (%d file)' % len(files))
    return 0


if __name__ == '__main__':
    sys.exit(quet(sys.argv[1] if len(sys.argv) > 1 else MAC_DINH))
