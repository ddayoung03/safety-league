"""Checks the frames inbetween.py made. Run after build_sprites.py; exits non-zero on a failure."""
import json
import pathlib
import sys

import numpy as np
from PIL import Image
from scipy import ndimage

import inbetween

HERE = pathlib.Path(__file__).resolve().parent
META = json.loads((HERE / 'frames.json').read_text(encoding='utf8'))
HEAD = {'LOAD': (60, 26, 110, 106), 'WELCOME': (62, 26, 110, 105), 'RADIO': (60, 26, 110, 106)}  # x0 y0 x1 y1, y1 exclusive
MOUTH = (75, 95, 89, 103)
BAD = []


def load(name):
    return np.asarray(Image.open(HERE / 'frames' / name).convert('RGBA'))


def check(ok, text):
    print(('ok   ' if ok else 'FAIL ') + text)
    if not ok:
        BAD.append(text)


every = [load(f) for m in META.values() for f in m['frames']]
colors = np.unique(np.concatenate([a[a[..., 3] > 0][:, :3] for a in every]), axis=0)
check(len(colors) <= 48, f'{len(colors)} colours across all frames')
check(all(set(np.unique(a[..., 3])) <= {0, 255} for a in every), 'alpha is binary')
check(all(a.shape[:2] == (176, 192) for a in every), 'all frames 192x176')

for action, (x0, y0, x1, y1) in HEAD.items():
    frames = [load(f) for f in META[action]['frames']]
    check(len(frames) == len(META[action]['durations_ms']), f'{action}: {len(frames)} frames match durations')
    mask = np.ones(frames[0].shape[:2], bool)
    if action == 'LOAD':
        mask &= ~inbetween.rows_mask(mask.shape, inbetween.LOAD_ARM)  # where the arm starts, not where it swings
    if action == 'RADIO':
        mask[MOUTH[1]:MOUTH[3], MOUTH[0]:MOUTH[2]] = False
    head = [a[y0:y1, x0:x1][mask[y0:y1, x0:x1]] for a in frames]
    worst = max(int((h != head[0]).any(1).sum()) for h in head)
    check(worst == 0, f'{action}: head box {x0},{y0}-{x1},{y1} differs by {worst} px at most')
    below = [a[116:] for a in frames]
    check(all(np.array_equal(b, below[0]) for b in below), f'{action}: body below row 116 identical')
    parts = [ndimage.label(a[..., 3] > 0, np.ones((3, 3)))[1] for a in frames]
    check(max(parts) == 1, f'{action}: pieces per frame {parts}')
    holes = [int((ndimage.binary_fill_holes(a[..., 3] > 0) & (a[..., 3] == 0)).sum()) for a in frames]
    print(f'     {action}: enclosed transparent px per frame {holes}')

for action in ('NOD', 'WIN', 'OVERTAKE'):
    frames = [load(f) for f in META[action]['frames']]
    check(len(frames) == len(META[action]['durations_ms']), f'{action}: {len(frames)} frames match durations')
    parts = [ndimage.label(a[..., 3] > 0, np.ones((3, 3)))[1] for a in frames]
    check(max(parts) == 1, f'{action}: pieces per frame {parts}')

nod = [load(f) for f in META['NOD']['frames']]
worst = max(int((a[106:] != nod[0][106:]).any(2).sum()) for a in nod)
check(worst == 0, f'NOD: torso, legs and feet (rows 106 down) differ by {worst} px at most')
heights = [int(np.nonzero(a[..., 3])[0].min()) for a in nod]
check(heights[0] == min(heights), f'NOD: rest is the highest frame, top by frame {heights}')

run = [load(f) for f in META['OVERTAKE']['frames']]
bob = [int(b) for b in inbetween.RUN_BOB]
worst = 0
for a, b in zip(run, bob):
    worst = max(worst, int((np.roll(a, -b, 0)[:inbetween.RUN_WAIST + 1] != run[0][:inbetween.RUN_WAIST + 1]).any(2).sum()))
check(worst == 0, f'OVERTAKE: upper body differs by {worst} px at most after removing the bob {bob}')

sys.exit(1 if BAD else 0)
