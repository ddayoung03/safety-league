"""Turn generated poses into real pixel sprites: key magenta, downscale to one grid, one palette, one canvas."""
import json
import pathlib

import numpy as np
from PIL import Image

HERE = pathlib.Path(__file__).resolve().parent
GRID_H = 136          # character height of the master in sprite pixels
CANVAS = (192, 176)   # shared canvas, room for flag, ears and jump
BASE_Y = 168          # feet on this row
COLORS = 48

ACTIONS = {
    'idle': (['master', 'master', 'master', 'idle_blink'], [500, 500, 500, 140], True),
    'win': (['win_crouch', 'win_jump', 'win_jump', 'win_land'], [140, 220, 220, 180], True),
    'overtake': (['overtake_1', 'overtake_2', 'overtake_3', 'overtake_4'], [90, 90, 90, 90], True),
    'pit': (['pit_1'], [800], True),
    'radio': (['radio_1', 'radio_2'], [220, 220], True),
    'load': (['load_1', 'load_2'], [170, 170], True),
    'welcome': (['welcome_2', 'master'], [420, 220], True),
    'nod': (['nod_1', 'nod_2'], [300, 300], True),
    'point': (['point_1'], [800], True),
}
AIRBORNE = {'win_jump': 22, 'overtake_4': 6}


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


def main():
    master = key(source('master'))
    mb = bbox(master)
    scale = GRID_H / (mb[3] - mb[1])

    names = sorted({n for frames, _, _ in ACTIONS.values() for n in frames})
    small = {}
    for n in names:
        im = key(source(n))
        b = bbox(im)
        crop = im.crop(b)
        w, h = max(1, round(crop.width * scale)), max(1, round(crop.height * scale))
        small[n] = crop.resize((w, h), Image.BOX)

    # One palette for every frame, chosen from all opaque pixels.
    px = np.concatenate([np.asarray(s)[np.asarray(s)[..., 3] > 128][:, :3] for s in small.values()])
    sheet = Image.fromarray(px.reshape(-1, 1, 3), 'RGB')
    pal = sheet.quantize(colors=COLORS, method=Image.Quantize.MEDIANCUT, dither=Image.Dither.NONE)

    out = HERE / 'frames'
    out.mkdir(exist_ok=True)
    for f in out.glob('*.png'):
        f.unlink()

    def finish(n):
        s = small[n]
        rgb = s.convert('RGB').quantize(palette=pal, dither=Image.Dither.NONE).convert('RGB')
        alpha = s.getchannel('A').point(lambda v: 255 if v > 128 else 0)
        rgb.putalpha(alpha)
        c = Image.new('RGBA', CANVAS, (0, 0, 0, 0))
        x = (CANVAS[0] - s.width) // 2
        y = BASE_Y - s.height - AIRBORNE.get(n, 0)
        c.paste(rgb, (x, max(0, y)), rgb)
        return c

    done = {n: finish(n) for n in names}
    meta = {}
    for action, (frames, ms, loop) in ACTIONS.items():
        files = []
        for i, n in enumerate(frames):
            name = f'rabbit_{action}_{i:02d}.png'
            done[n].save(out / name)
            files.append(name)
        meta[action.upper()] = {'frames': files, 'durations_ms': ms, 'loop': loop}
    (HERE / 'frames.json').write_text(json.dumps(meta, indent=1), encoding='utf8')

    row = [done[ACTIONS[a][0][min(1, len(ACTIONS[a][0]) - 1)]] for a in ACTIONS]
    sheet = Image.new('RGBA', (CANVAS[0] * len(row), CANVAS[1]), (13, 15, 19, 255))
    for i, c in enumerate(row):
        sheet.paste(c, (i * CANVAS[0], 0), c)
    sheet.resize((sheet.width * 2, sheet.height * 2), Image.NEAREST).save(HERE / 'consistency.png')

    cards = ''.join(
        f'<figure><canvas data-a="{a.upper()}" width="{CANVAS[0]}" height="{CANVAS[1]}"></canvas><figcaption>{a}</figcaption></figure>'
        for a in ACTIONS)
    html = '''<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>토끼 동작 미리보기</title><style>
body{margin:0;background:#0D0F13;color:#F4F4F1;font-family:sans-serif}h1{font-size:18px;margin:16px 24px}
main{display:flex;flex-wrap:wrap;gap:12px;padding:0 24px 24px}figure{margin:0;text-align:center}
canvas{width:384px;height:352px;image-rendering:pixelated}figcaption{color:#8A909B;font-size:15px}
</style></head><body><h1>토끼 레이서 동작 9종 (2배 확대)</h1><main>''' + cards + '''</main><script>
const M=''' + json.dumps(meta) + ''';
document.querySelectorAll('canvas').forEach(c=>{const m=M[c.dataset.a],g=c.getContext('2d');g.imageSmoothingEnabled=false;
const im=m.frames.map(f=>{const i=new Image();i.src='frames/'+f;return i});let k=0;
const step=()=>{g.clearRect(0,0,c.width,c.height);if(im[k].complete)g.drawImage(im[k],0,0);const d=m.durations_ms[k];k=(k+1)%im.length;setTimeout(step,d)};step()});
</script></body></html>'''
    (HERE / 'preview.html').write_text(html, encoding='utf8')
    total = sum(f.stat().st_size for f in out.glob('*.png'))
    print('frames', len(list(out.glob('*.png'))), 'bytes', total, 'palette', COLORS, 'scale', round(scale, 4))


main()
