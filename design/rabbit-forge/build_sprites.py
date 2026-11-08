"""Turn generated poses into real pixel sprites: key magenta, downscale to one grid, one palette, one canvas."""
import json
import pathlib

import numpy as np
from PIL import Image
from scipy import ndimage

import inbetween

HERE = pathlib.Path(__file__).resolve().parent
GRID_H = 136          # character height of the master in sprite pixels
CANVAS = (192, 176)   # shared canvas, room for flag, ears and jump
BASE_Y = 168          # feet on this row
COLORS = 48

ACTIONS = {
    'idle': (['master', 'master', 'master', 'idle_blink'], [500, 500, 500, 140], True),
    'win': (['win_crouch', 'win_jump', 'win_jump', 'win_land'], [110, 100, 60, 60, 70, 110, 60, 60, 90, 130], True),
    'overtake': (['overtake_1', 'overtake_2', 'overtake_3', 'overtake_4'], [70] * 8, True),
    'pit': (['pit_1'], [800], True),
    # win, overtake, radio, load, welcome and nod frames come from inbetween.py; these names are the source poses
    # and the palette input, so they stay even where a pose is no longer shown.
    'radio': (['radio_1', 'radio_2'], [220, 220], True),
    'load': (['load_1', 'load_2'], [90] * 8, True),
    'welcome': (['welcome_2', 'master'], [110] * 6, True),
    'nod': (['nod_1', 'nod_2'], [310] + [90] * 7, True),
    'point': (['point_1'], [800], True),
    # 2026-11-08 추가 동작. 기존 프레임이 바뀌지 않게 팔레트 입력에서 뺀다(NEW).
    'cheer': (['cheer_1', 'cheer_1', 'cheer_1', 'cheer_2'], [500, 500, 500, 160], True),
    'sit': (['sit_1', 'sit_2'], [900, 900], True),
    'coupon': (['coupon_1', 'coupon_1', 'coupon_1', 'coupon_2'], [500, 500, 500, 160], True),
    'book': (['book_1'], [800], True),
    'clip': (['clip_1', 'clip_2'], [600, 600], True),
}
NEW = {'cheer_1', 'cheer_2', 'sit_1', 'sit_2', 'coupon_1', 'coupon_2', 'book_1', 'clip_1', 'clip_2'}
HIDDEN = {'pit'}  # 앉은 자세(sit)로 바뀌었다. pit_1은 팔레트 입력으로만 남긴다.
AIRBORNE = {'win_jump': 22}
RUN_BOB = {'overtake_1': 0, 'overtake_2': 0, 'overtake_3': 0, 'overtake_4': 0, 'overtake_a': 0, 'overtake_b': 0}
EXTRA = ['overtake_a', 'overtake_b', 'nod_h']  # kept out of the palette input so the other actions stay byte-identical
HOLD_HEAD = {'radio_1', 'radio_2', 'load_1', 'load_2', 'welcome_2', 'master', 'idle_blink'}


def source(name):
    return HERE / ('master' if name == 'master' else f'poses/{name}') / 'generated.png'


def key(path):
    """Remove the magenta background and the fringe around the character."""
    a = np.asarray(Image.open(path).convert('RGB')).astype(np.int16)
    r, g, b = a[..., 0], a[..., 1], a[..., 2]
    magenta = (r > 150) & (b > 150) & (g < 110) & (np.abs(r - b) < 90)
    rgba = np.dstack([a, np.where(magenta, 0, 255)]).astype(np.uint8)
    return Image.fromarray(rgba, 'RGBA')


def bbox(im):
    return im.getchannel('A').point(lambda v: 255 if v > 128 else 0).getbbox()


def helmet(im):
    """Center and box of the largest red area: the helmet, used to register frames."""
    a = np.asarray(im).astype(np.int16)
    red = (a[..., 0] > 150) & (a[..., 1] < 70) & (a[..., 2] < 70) & (a[..., 3] > 128)
    lab, n = ndimage.label(red)
    if n == 0:
        return im.width / 2, im.height / 3, None
    big = 1 + int(np.argmax(ndimage.sum(red, lab, range(1, n + 1))))
    ys, xs = np.nonzero(lab == big)
    return float(xs.mean()), float(ys.mean()), (xs.min(), ys.min(), xs.max(), ys.max())


def main():
    master = key(source('master'))
    mb = bbox(master)
    scale = GRID_H / (mb[3] - mb[1])

    names = sorted({n for frames, _, _ in ACTIONS.values() for n in frames} - NEW)
    def shrink(n):
        im = key(source(n))
        crop = im.crop(bbox(im))
        w, h = max(1, round(crop.width * scale)), max(1, round(crop.height * scale))
        return crop.resize((w, h), Image.BOX)

    small = {n: shrink(n) for n in names}

    # One palette for every frame, chosen from all opaque pixels.
    px = np.concatenate([np.asarray(s)[np.asarray(s)[..., 3] > 128][:, :3] for s in small.values()])
    sheet = Image.fromarray(px.reshape(-1, 1, 3), 'RGB')
    pal = sheet.quantize(colors=COLORS, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)

    for n in EXTRA + sorted(NEW):
        small[n] = shrink(n)

    out = HERE / 'frames'
    out.mkdir(exist_ok=True)
    for f in out.glob('*.png'):
        f.unlink()

    anchor = helmet(small['master'])

    def finish(n):
        s = small[n]
        rgb = s.convert('RGB').quantize(palette=pal, dither=Image.Dither.NONE).convert('RGB')
        alpha = s.getchannel('A').point(lambda v: 255 if v > 128 else 0)
        rgb.putalpha(alpha)
        c = Image.new('RGBA', CANVAS, (0, 0, 0, 0))
        hx, hy, _ = helmet(s)
        x = round(CANVAS[0] / 2 - hx)
        y = BASE_Y - s.height - AIRBORNE.get(n, 0)
        if n in RUN_BOB:
            # One fixed head line for the whole run so only the legs and arms move.
            x = round(CANVAS[0] / 2 - hx + 8)
            y = round(62 - hy) + RUN_BOB[n]
        elif n in HOLD_HEAD:
            y = round(BASE_Y - anchor[1] - (small['master'].height - anchor[1]) * 1 + (anchor[1] - hy))
        c.paste(rgb, (x, y), rgb)
        return c

    done = {n: finish(n) for n in names + EXTRA + sorted(NEW)}
    made = inbetween.build(done)
    meta = {}
    shown = []
    for action, (frames, ms, loop) in ACTIONS.items():
        if action in HIDDEN:
            continue
        files = []
        images = made.get(action) or [done[n] for n in frames]
        for i, im in enumerate(images):
            name = f'rabbit_{action}_{i:02d}.png'
            im.save(out / name)
            files.append(name)
        shown.append(images[min(1, len(images) - 1)])
        meta[action.upper()] = {'frames': files, 'durations_ms': ms, 'loop': loop}
    (HERE / 'frames.json').write_text(json.dumps(meta, indent=1), encoding='utf8')

    sheet = Image.new('RGBA', (CANVAS[0] * len(shown), CANVAS[1]), (13, 15, 19, 255))
    for i, c in enumerate(shown):
        sheet.paste(c, (i * CANVAS[0], 0), c)
    sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST).save(HERE / 'consistency.png')

    cards = ''.join(
        f'<figure><canvas data-a="{a.upper()}" width="{CANVAS[0]}" height="{CANVAS[1]}"></canvas><figcaption>{a}</figcaption></figure>'
        for a in ACTIONS if a not in HIDDEN)
    html = '''<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>토끼 동작 미리보기</title><style>
body{margin:0;background:#0D0F13;color:#F4F4F1;font-family:sans-serif}h1{font-size:18px;margin:16px 24px}
main{display:flex;flex-wrap:wrap;gap:12px;padding:0 24px 24px}figure{margin:0;text-align:center}
canvas{width:384px;height:352px;image-rendering:pixelated}figcaption{color:#8A909B;font-size:15px}
</style></head><body><h1>토끼 레이서 동작 13종 (2배 확대)</h1><main>''' + cards + '''</main><script>
const M=''' + json.dumps(meta) + ''';
document.querySelectorAll('canvas').forEach(c=>{const m=M[c.dataset.a],g=c.getContext('2d');g.imageSmoothingEnabled=false;
const im=m.frames.map(f=>{const i=new Image();i.src='frames/'+f;return i});let k=0;
const step=()=>{g.clearRect(0,0,c.width,c.height);if(im[k].complete)g.drawImage(im[k],0,0);const d=m.durations_ms[k];k=(k+1)%im.length;setTimeout(step,d)};step()});
</script></body></html>'''
    (HERE / 'preview.html').write_text(html, encoding='utf8')
    total = sum(f.stat().st_size for f in out.glob('*.png'))
    print('frames', len(list(out.glob('*.png'))), 'bytes', total, 'palette', COLORS, 'scale', round(scale, 4))


main()
