"""앱 아이콘(2026-11-08). 실행: python design/icon/make_icon.py design/icon
D2 아이콘: 픽셀 방패(테두리 음영·리벳) + 귀가 뚫고 나온 토끼. 216 격자(1칸 = 토끼 스프라이트 1px)에 그린다."""
import sys, os
from PIL import Image, ImageDraw, ImageChops
R = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
OUT = sys.argv[1]; os.makedirs(OUT, exist_ok=True)
G = 216
BG = (13, 15, 19)
OUTLINE = (40, 26, 6); HI = (255, 230, 140); BASE = (255, 197, 46); MID = (232, 160, 30); SH = (176, 108, 18)
INLINE = (34, 22, 6); RIVET = (255, 246, 210)

k = 1.19; ox = 108 - 54 * k; oy = 0  # 방패 중심 x를 격자 중심에
OUTER = [(54, 14), (88, 26), (88, 56), (80, 76), (54, 96), (28, 76), (20, 56), (20, 26)]
INNER = [(54, 22), (80, 31), (80, 55), (73, 71), (54, 86), (35, 71), (28, 55), (28, 31)]

def poly(pts, dy):
    return [(ox + x * k, dy + y * k) for x, y in pts]

def build(dy):
    m_out = Image.new('L', (G, G)); ImageDraw.Draw(m_out).polygon(poly(OUTER, dy), fill=255)
    m_in = Image.new('L', (G, G)); ImageDraw.Draw(m_in).polygon(poly(INNER, dy), fill=255)
    return m_out, m_in

def px(m, x, y):
    return 0 <= x < G and 0 <= y < G and m.getpixel((x, y)) > 127

PATTERN = os.environ.get('PAT', 'stars'); NAME = os.environ.get('NAME', 'icon')
import math
STARS = [(81, 44), (79, 60), (83, 78), (135, 44), (137, 60), (133, 78), (96, 33), (120, 33)]
def fill(x, y, dy, c):
    cx, cy = G / 2, dy + 52 * k
    if PATTERN == 'checker':
        cell = 6
        if ((x // cell) + (y // cell)) % 2 == 0: return tuple(min(255, v + 14) for v in c)
    if PATTERN == 'rays':
        a = math.atan2(y - cy, x - cx)
        if int((a + math.pi) / (2 * math.pi) * 16) % 2 == 0: return (min(255, c[0] + 52), min(255, c[1] + 38), c[2] + 2)
    if PATTERN == 'stars':
        for sx, sy in STARS:
            X, Y = sx, dy + sy
            if (x, y) == (X, Y): return (255, 240, 180)
            if abs(x - X) + abs(y - Y) == 1: return (200, 150, 40)
    return c

def render(dy, rabbit_top, bg=True):
    m_out, m_in = build(dy)
    im = Image.new('RGBA', (G, G), BG + (255,) if bg else (0, 0, 0, 0))
    P = im.load()
    # 방패 안쪽: 위가 살짝 밝은 픽셀 단계 그라데이션
    for y in range(G):
        for x in range(G):
            if px(m_in, x, y):
                band = (y - (dy + 22 * k)) / (64 * k)
                c = (24, 28, 36) if band < 0.33 else (19, 22, 29) if band < 0.66 else (15, 17, 22)
                P[x, y] = fill(x, y, dy, c) + (255,)
    # 테두리: 왼위는 밝게, 오른아래는 어둡게, 안쪽 경계선
    for y in range(G):
        for x in range(G):
            if px(m_out, x, y) and not px(m_in, x, y):
                c = BASE
                if not px(m_out, x - 1, y - 1) or not px(m_out, x - 2, y):
                    c = HI
                elif not px(m_out, x + 1, y + 1) or not px(m_out, x + 2, y):
                    c = SH
                if any(px(m_in, x + a, y + b) for a in (-2, -1, 0, 1, 2) for b in (-2, -1, 0, 1, 2)):
                    c = INLINE if any(px(m_in, x + a, y + b) for a in (-1, 0, 1) for b in (-1, 0, 1)) else (SH if c == BASE or c == SH else c)
                elif x > ox + 54 * k:
                    c = MID
                P[x, y] = c + (255,)
    # 바깥 외곽선
    for y in range(G):
        for x in range(G):
            if not px(m_out, x, y) and any(px(m_out, x + a, y + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                P[x, y] = OUTLINE + (255,)
    # 왼쪽 위 테두리 반짝임(4방향 픽셀 별)
    sx, sy = round(ox + 31 * k), round(dy + 26 * k)
    for a, b, c in [(0, 0, (255, 255, 255)), (1, 0, HI), (-1, 0, HI), (0, 1, HI), (0, -1, HI), (2, 0, BASE), (-2, 0, BASE), (0, 2, BASE), (0, -2, BASE)]:
        P[sx + a, sy + b] = c + (255,)
    # 리벳 4개
    for (vx, vy) in [(24, 29), (84, 29), (24, 53), (84, 53)]:
        cx, cy = round(ox + vx * k), round(dy + vy * k)
        for a, b, c in [(0, 0, RIVET), (1, 0, BASE), (0, 1, BASE), (1, 1, SH)]:
            P[cx + a, cy + b] = c + (255,)
    # 토끼: 방패 안 + 방패 윗선 위쪽(귀)만 보인다
    rabbit = Image.open(os.path.join(R, 'rabbit_idle_00.png')).convert('RGBA').crop((58, 32, 106, 140))
    layer = Image.new('RGBA', (G, G))
    layer.alpha_composite(rabbit, (round(108 - rabbit.width / 2), rabbit_top))
    above = Image.new('L', (G, G)); ImageDraw.Draw(above).rectangle([0, 0, G, dy + 31 * k], fill=255)
    clip = ImageChops.multiply(layer.getchannel('A'), ImageChops.lighter(m_in, above))
    im.paste(layer, (0, 0), clip)
    return im

# 귀 끝~방패 끝을 세로 가운데에 둔다
dy = 40; rabbit_top = dy + 3
img = render(dy, rabbit_top)
bbox = ImageChops.difference(img.convert('RGB'), Image.new('RGB', (G, G), BG)).getbbox()
shift = round(((G - bbox[3]) - bbox[1]) / 2)
dy += shift; rabbit_top += shift
full = render(dy, rabbit_top); fg = render(dy, rabbit_top, bg=False)
bbox = fg.getbbox(); print('bbox', bbox)
# 안전 영역 검사: 66/108 지름 원
import math
r = 66 / 108 * G / 2
bad = [(x, y) for y in range(G) for x in range(G) if fg.getpixel((x, y))[3] > 0 and math.hypot(x + 0.5 - G / 2, y + 0.5 - G / 2) > r]
print('outside safe zone px:', len(bad))
full.resize((G * 2, G * 2), Image.NEAREST).save(f'{OUT}/e-full-432.png')
fg.resize((G * 2, G * 2), Image.NEAREST).save(f'{OUT}/e-fg-432.png')
# 미리보기: 안전 영역만큼 확대한 512 시안
crop = full.crop((round(G / 2 - r), round(G / 2 - r), round(G / 2 + r), round(G / 2 + r)))
crop.resize((512, 512), Image.NEAREST).save(f'{OUT}/{NAME}.png')
crop.resize((48, 48), Image.LANCZOS).save(f'{OUT}/{NAME}-48.png')
full.resize((512, 512), Image.NEAREST).save(f'{OUT}/{NAME}-launcher.png')
print('ok')
