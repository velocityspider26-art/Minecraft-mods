"""Tile rendered frames into a labelled contact sheet."""
import sys
import glob
import os
import re
from PIL import Image, ImageDraw

indir, view, out = sys.argv[1], sys.argv[2], sys.argv[3]
cols = int(sys.argv[4]) if len(sys.argv) > 4 else 7
scale = float(sys.argv[5]) if len(sys.argv) > 5 else 0.62

files = sorted(glob.glob(os.path.join(indir, f"*_{view}_*.png")),
               key=lambda p: int(re.search(r"_(\d+)\.png$", p).group(1)))
if not files:
    raise SystemExit(f"no files for view {view} in {indir}")

ims = []
for f in files:
    im = Image.open(f).convert("RGB")
    im = im.resize((int(im.width * scale), int(im.height * scale)), Image.LANCZOS)
    ims.append((int(re.search(r"_(\d+)\.png$", f).group(1)), im))

w, h = ims[0][1].size
lab = 20
rows = (len(ims) + cols - 1) // cols
sheet = Image.new("RGB", (cols * w, rows * (h + lab)), (16, 17, 20))
d = ImageDraw.Draw(sheet)
for i, (fr, im) in enumerate(ims):
    r, c = divmod(i, cols)
    x, y = c * w, r * (h + lab)
    sheet.paste(im, (x, y + lab))
    d.text((x + 5, y + 5), f"f{fr}  t={fr/30.0:.2f}s", fill=(235, 235, 240))
    d.rectangle([x, y, x + w - 1, y + h + lab - 1], outline=(60, 62, 70))
sheet.save(out)
print(f"{out}  {sheet.size}  tiles={len(ims)}")
