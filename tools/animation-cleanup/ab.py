"""Build a labelled BEFORE/AFTER comparison sheet."""
import sys
import glob
import os
import re
from PIL import Image, ImageDraw

out = sys.argv[1]
title = sys.argv[2]
pairs = []          # (label, dir, view)
for spec in sys.argv[3:]:
    lbl, d, v = spec.split(":")
    pairs.append((lbl, d, v))

scale = 0.60
rows = []
for lbl, d, v in pairs:
    files = sorted(glob.glob(os.path.join(d, f"*_{v}_*.png")),
                   key=lambda p: int(re.search(r"_(\d+)\.png$", p).group(1)))
    ims = []
    for f in files:
        im = Image.open(f).convert("RGB")
        ims.append((int(re.search(r"_(\d+)\.png$", f).group(1)),
                    im.resize((int(im.width * scale), int(im.height * scale)), Image.LANCZOS)))
    rows.append((lbl, ims))

w, h = rows[0][1][0][1].size
cols = max(len(r[1]) for r in rows)
lab, hdr = 19, 34
sheet = Image.new("RGB", (cols * w, hdr + len(rows) * (h + lab)), (14, 15, 18))
d = ImageDraw.Draw(sheet)
d.text((10, 10), title, fill=(250, 250, 255))
for ri, (lbl, ims) in enumerate(rows):
    y0 = hdr + ri * (h + lab)
    col = (255, 150, 140) if "BEFORE" in lbl.upper() else (140, 230, 170)
    for i, (fr, im) in enumerate(ims):
        x = i * w
        sheet.paste(im, (x, y0 + lab))
        d.text((x + 5, y0 + 4), f"f{fr}", fill=(225, 225, 232))
        d.rectangle([x, y0, x + w - 1, y0 + h + lab - 1], outline=(55, 57, 65))
    d.text((6, y0 + lab + 4), lbl, fill=col)
sheet.save(out)
print(f"{out} {sheet.size}")
