"""In-between frames cut from approved art: no new drawing, every pixel is sampled from a finished frame.

Coordinates are canvas pixels of the finished 192x176 frames. They are fixed because the poses are fixed.
"""
import math

import numpy as np
from PIL import Image

UP = 8  # three Scale2x passes, then nearest rotation, then reduce: RotSprite

# LOAD: right arm and flag from load_2 as three linked layers: upper arm from the shoulder, forearm from the elbow,
# hand and flag from the wrist, each lagging the one before. All of it stays below the approved pose, away from
# the torso and head, so nothing behind it has to be invented. Last arm pixel per row, by row range.
LOAD_ARM = [(57, 60, 59), (61, 75, 60), (76, 95, 59), (96, 96, 60), (97, 97, 61), (98, 98, 62), (99, 99, 63), (100, 100, 64),
            (101, 101, 66), (102, 102, 67), (103, 103, 71), (104, 112, 72), (113, 113, 71)]
LOAD_PIVOT = (72.5, 108.5)
LOAD_ELBOW = (63, 103)
LOAD_WRIST = (53, 96)
LOAD_AXIS = (.79, .62)   # along the arm, toward the shoulder
LOAD_OVERLAP = 3         # upper arm runs this far past the elbow, so the outside of the bend stays filled
LOAD_CAP = 5        # sleeve pixels this close to the pivot also stay on the body, so the joint never opens
LOAD_FRAMES = 8
LOAD_UPPER = (-20, 6, 0)     # degrees: centre, swing, lag in loop turns
LOAD_FORE = (-8, 8, .125)
LOAD_FLAG = (-9, 8, .25)
CLOTH = (46, 75)    # flag cloth: x from the pole outward, rows down to this one
CLOTH_WAVE = 1.7    # pixels at the free end

# WELCOME: forearm and hand from welcome_2, swung from the elbow.
WAVE_ARM = [(94, 95, 57), (96, 97, 60), (98, 104, 61), (105, 105, 63), (106, 106, 64), (107, 108, 64)]
WAVE_PIVOT = (63.5, 110.5)
WAVE_CAP = 3
WAVE_RANGE = (6, -18)
WAVE_POSES = [0, 1, 2, 3, 2, 1]
HEAD_ZONE = (62, 104)  # x from, y up to: the hand passes behind the helmet, never over it

# NOD: the head bows, it does not slide. nod_1 is the drawn bow; moved onto nod_2's body it only ever covers the
# neck and collar, so the rest pose stays the highest frame and no longer neck can appear.
NOD_REST_ROW = 101    # last head row of nod_2
NOD_BOW_ROW = 105     # last head row of nod_1 once registered
NOD_BOW_X = -31       # whole-pixel shift that lands nod_1's belt and legs on nod_2's
NOD_HALF = 2          # the half-way frame sits this many pixels above the full bow
NOD_LIFT = [None, NOD_HALF, 0, 0, NOD_HALF, None]  # None is the rest pose

# WIN: whole-pixel path through the three approved poses. Each row: pose, rise, ears, flag, squash.
# Ears and flag trail the jump by pixels; squash sinks everything above WIN_WAIST into the legs.
WIN_PATH = [
    ('win_crouch', 0, 0, 0, 0), ('win_crouch', 0, 0, 0, 2),
    ('win_jump', -18, 2, 1, 0), ('win_jump', -4, 2, 1, 0), ('win_jump', 8, 1, 1, 0),
    ('win_jump', 9, 0, 0, 0), ('win_jump', 0, -2, -1, 0), ('win_jump', -14, -2, -1, 0),
    ('win_land', 0, 0, 0, 3), ('win_land', 0, 0, 0, 0),
]
WIN_SIDE = {'win_crouch': 0, 'win_jump': -14, 'win_land': -1}  # puts the helmet stripe on one column in all poses
WIN_WAIST = {'win_crouch': 133, 'win_land': 136}
WIN_EARS = (68, 58)   # win_jump: x from, last row
WIN_FLAG = (71, 66)   # win_jump: x up to, last row

# OVERTAKE: one upper body from overtake_1, legs from four poses. overtake_a and overtake_b were drawn from
# overtake_1 for this; overtake_2 gives the other passing pose, so the near leg really alternates.
RUN_WAIST = 101
RUN_HIP = (97.5, 104.5)
RUN_CROTCH = {'overtake_1': ((90, 100), (97, 114)), 'overtake_a': ((96, 100), (102, 112))}
RUN_NARROW = 20      # degrees each leg closes toward the other in a built in-between
RUN_CYCLE = [('overtake_1', 0, 0), ('overtake_1', 1, 1), ('overtake_b', 0, -2), ('overtake_a', 1, -1),
             ('overtake_a', 0, 0), ('overtake_a', 1, 1), ('overtake_2', 0, -2), ('overtake_1', 1, -1)]  # pose, narrow, bob

RUN_BOB = [bob for _, _, bob in RUN_CYCLE]

# RADIO: radio_2 is the same face drawn wider and further right, so match on the nose, not the helmet.
MOUTH = (75, 95, 89, 103)  # x0, y0, x1, y1 in radio_1
MOUTH_DX = 13
MOUTH_ROW0 = 97            # radio_2 has its nose above this row
MOUTH_SKIN = 102           # plain skin row of radio_2, fills the rows above MOUTH_ROW0


def pack(im):
    rgba = np.ascontiguousarray(np.asarray(im.convert('RGBA')))
    a = rgba.view(np.uint32)[..., 0].copy()
    a[rgba[..., 3] == 0] = 0
    return a


def unpack(p):
    return Image.fromarray(np.ascontiguousarray(p).view(np.uint8).reshape(*p.shape, 4), 'RGBA')


def scale2x(a):
    p = np.pad(a, 1, mode='edge')
    b, d, f, h = p[:-2, 1:-1], p[1:-1, :-2], p[1:-1, 2:], p[2:, 1:-1]
    go = (b != h) & (d != f)
    out = np.empty((a.shape[0] * 2, a.shape[1] * 2), a.dtype)
    out[0::2, 0::2] = np.where(go & (d == b), d, a)
    out[0::2, 1::2] = np.where(go & (b == f), f, a)
    out[1::2, 0::2] = np.where(go & (d == h), d, a)
    out[1::2, 1::2] = np.where(go & (h == f), f, a)
    return out


def rotate(layer, chain, warp=None):
    """Apply (pivot, clockwise degrees) rotations in order, in one resample. warp(x) shifts source columns vertically."""
    hi = scale2x(scale2x(scale2x(layer)))
    h, w = layer.shape
    ys, xs = np.mgrid[0:h * UP, 0:w * UP]
    sx, sy = (xs + .5) / UP, (ys + .5) / UP
    for pivot, deg in reversed(chain):
        c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
        x, y = sx - pivot[0], sy - pivot[1]
        sx, sy = c * x + s * y + pivot[0], -s * x + c * y + pivot[1]
    if warp:
        sy = sy - warp(sx)
    ix, iy = np.floor(sx * UP).astype(int), np.floor(sy * UP).astype(int)
    inside = (ix >= 0) & (ix < w * UP) & (iy >= 0) & (iy < h * UP)
    big = np.where(inside, hi[iy.clip(0, h * UP - 1), ix.clip(0, w * UP - 1)], 0)
    return reduce_blocks(big, h, w)


def reduce_blocks(big, h, w):
    """Most common colour per sprite pixel. A pixel is opaque from 3/8 coverage so 1px lines stay unbroken."""
    blocks = big.reshape(h, UP, w, UP).transpose(0, 2, 1, 3).reshape(h, w, UP * UP)
    out = np.zeros((h, w), big.dtype)
    for y, x in zip(*np.nonzero((blocks != 0).sum(2) >= UP * UP * 3 // 8)):
        v, n = np.unique(blocks[y, x][blocks[y, x] != 0], return_counts=True)
        out[y, x] = v[n.argmax()]
    return out


def rows_mask(shape, rows):
    m = np.zeros(shape, bool)
    for y0, y1, last in rows:
        m[y0:y1 + 1, :last + 1] = True
    return m


def near(shape, pivot, r):
    ys, xs = np.mgrid[0:shape[0], 0:shape[1]]
    return (xs + .5 - pivot[0]) ** 2 + (ys + .5 - pivot[1]) ** 2 <= r * r


def split(base, rows, pivot, cap):
    """Cut a limb out of a frame: returns the limb and the body left behind."""
    limb_mask = rows_mask(base.shape, rows) & (base != 0)
    body = np.where(limb_mask & ~near(base.shape, pivot, cap), 0, base)
    return np.where(limb_mask, base, 0), body


def over(under, top):
    return np.where(top != 0, top, under)


def eased(i, n):
    return (1 - math.cos(math.pi * i / (n - 1))) / 2


def load_frames(base):
    arm, body = split(pack(base), LOAD_ARM, LOAD_PIVOT, LOAD_CAP)
    ys, xs = np.mgrid[0:arm.shape[0], 0:arm.shape[1]]
    cloth_mask = (xs >= CLOTH[0]) & (ys <= CLOTH[1]) & (arm != 0)
    length = xs[cloth_mask].max() + 1 - CLOTH[0]

    def along(p):
        return (xs + .5 - p[0]) * LOAD_AXIS[0] + (ys + .5 - p[1]) * LOAD_AXIS[1]

    upper = np.where(along(LOAD_ELBOW) > -LOAD_OVERLAP, arm, 0)
    fore = np.where((along(LOAD_ELBOW) <= 0) & (along(LOAD_WRIST) > -2), arm, 0)
    hand = np.where((along(LOAD_WRIST) <= 0) & ~cloth_mask, arm, 0)
    cloth = np.where(cloth_mask, arm, 0)

    def swing(part, t):
        centre, size, lag = part
        return centre + size * math.cos(2 * math.pi * (t - lag))

    frames = []
    for i in range(LOAD_FRAMES):
        t = i / LOAD_FRAMES
        u, f, g = swing(LOAD_UPPER, t), swing(LOAD_FORE, t), swing(LOAD_FLAG, t)
        to_upper = [(LOAD_PIVOT, u)]
        to_fore = [(LOAD_ELBOW, f - u)] + to_upper
        to_hand = [(LOAD_WRIST, g - f)] + to_fore

        def wave(x):
            v = np.clip((x - CLOTH[0]) / length, 0, 1)
            return CLOTH_WAVE * v * np.sin(2 * math.pi * (v - t))

        limb = rotate(upper, to_upper)
        for part in (rotate(fore, to_fore), rotate(hand, to_hand), rotate(cloth, to_hand, wave)):
            limb = over(limb, part)
        frames.append(unpack(over(body, limb)))
    return frames


def welcome_frames(base):
    arm, body = split(pack(base), WAVE_ARM, WAVE_PIVOT, WAVE_CAP)
    ys, xs = np.mgrid[0:arm.shape[0], 0:arm.shape[1]]
    head = (xs >= HEAD_ZONE[0]) & (ys <= HEAD_ZONE[1])
    a, b = WAVE_RANGE
    frames = []
    for pose in WAVE_POSES:
        limb = rotate(arm, [(WAVE_PIVOT, a + (b - a) * eased(pose, max(WAVE_POSES) + 1))])
        frames.append(unpack(over(body, np.where(head, 0, limb))))
    return frames


def shift(layer, dy):
    return np.roll(layer, dy, axis=0)


def rows(layer, last):
    out = layer.copy()
    out[last + 1:] = 0
    return out


def trail(under, part, follow, lag):
    """Draw a part that lags its body by whole pixels. Above the body, also draw it in place so the join cannot open."""
    if lag < 0:
        under = over(under, shift(part, follow))
    return over(under, shift(part, follow + lag))


def nod_frames(done):
    rest = pack(done['nod_2'])
    body = rest.copy()
    body[:NOD_REST_ROW + 1] = 0
    bow = rows(np.roll(pack(done['nod_1']), NOD_BOW_X, 1), NOD_BOW_ROW)
    return [unpack(rest if lift is None else over(body, shift(bow, -lift))) for lift in NOD_LIFT]


def win_frames(done):
    frames = []
    for pose, rise, ear, flag, squash in WIN_PATH:
        p = pack(done[pose])
        if pose == 'win_jump':
            ys, xs = np.mgrid[0:p.shape[0], 0:p.shape[1]]
            ears_mask = (xs >= WIN_EARS[0]) & (ys <= WIN_EARS[1])
            flag_mask = (xs <= WIN_FLAG[0]) & (ys <= WIN_FLAG[1])
            ears, cloth = np.where(ears_mask, p, 0), np.where(flag_mask, p, 0)
            body = np.where(ears_mask | flag_mask, 0, p)
            p = over(trail(trail(np.zeros_like(p), ears, 0, ear), cloth, 0, flag), body)
            p = shift(p, -rise)
        if squash:
            top = rows(p, WIN_WAIST[pose])
            p = over(np.where(top != 0, 0, p), shift(top, squash))
        frames.append(unpack(np.roll(p, WIN_SIDE[pose], 1)))
    return frames


def align(src, ref, y0, y1):
    """Whole-pixel shift of src that best matches ref between two rows."""
    best, moved = None, src
    for dy in range(-12, 13):
        for dx in range(-12, 13):
            m = np.roll(src, (dy, dx), (0, 1))
            miss = int((m[y0:y1] != ref[y0:y1]).sum())
            if best is None or miss < best:
                best, moved = miss, m
    return moved


def narrowed(legs, crotch):
    """Close a stride: the two legs split at the crotch and each swing toward the other."""
    ys, xs = np.mgrid[0:legs.shape[0], 0:legs.shape[1]]
    (x0, y0), (x1, y1) = crotch
    right = (xs - x0) * (y1 - y0) - (ys - y0) * (x1 - x0) > 0
    front, rear = np.where(right, legs, 0), np.where(right, 0, legs)
    hip = np.where(near(legs.shape, RUN_HIP, 7), legs, 0)
    return over(over(hip, rotate(rear, [(RUN_HIP, -RUN_NARROW)])), rotate(front, [(RUN_HIP, RUN_NARROW)]))


def overtake_frames(done):
    ref = pack(done['overtake_1'])
    top = rows(ref, RUN_WAIST)
    legs = {}
    for n in ('overtake_1', 'overtake_a', 'overtake_b', 'overtake_2'):
        fit = align(pack(done[n]), ref, 85 if n == 'overtake_2' else 40, RUN_WAIST)
        legs[n] = np.where(top != 0, 0, fit) * (np.arange(ref.shape[0])[:, None] > RUN_WAIST)
    frames = []
    for pose, narrow, bob in RUN_CYCLE:
        low = narrowed(legs[pose], RUN_CROTCH[pose]) if narrow else legs[pose]
        low[:RUN_WAIST + 1] = 0
        frames.append(unpack(shift(over(low, top), bob)))
    return frames


def radio_frames(open_mouth, closed_mouth):
    """Frame 2 is frame 1 with only the mouth box replaced, so eyes and cheeks stay the same pixels."""
    out, src = pack(open_mouth), pack(closed_mouth)
    x0, y0, x1, y1 = MOUTH
    for y in range(y0, y1):
        out[y, x0:x1] = src[y if y >= MOUTH_ROW0 else MOUTH_SKIN, x0 + MOUTH_DX:x1 + MOUTH_DX]
    return [open_mouth, unpack(out)]


def build(done):
    return {
        'win': win_frames(done),
        'overtake': overtake_frames(done),
        'nod': nod_frames(done),
        'load': load_frames(done['load_2']),
        'welcome': welcome_frames(done['welcome_2']),
        'radio': radio_frames(done['radio_1'], done['radio_2']),
    }
