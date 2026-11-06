"""Second pass: draw frames from their neighbor so only one part changes.

Each entry is (name, reference pose, instruction). Rerun to continue.
"""
import pathlib
import shutil
import subprocess
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROUTE = HERE.parents[2] / '.claude/skills/generate2dmedia/scripts/route_media.py'

BASE = (
    'Edit the attached image only as described. Keep every other pixel the same: identical eyes, eye direction, '
    'face, helmet, ears, body position, legs, scale, pixel style and framing. {change} '
    'Solid flat magenta background #FF00FF, no shadow, no text.'
)
LEGS = (
    'Edit the attached image only as described. Keep the head, helmet, ears, face, torso, both arms, gloves, scale, '
    'pixel style and framing exactly the same. Change only the legs and boots. {change} '
    'Solid flat magenta background #FF00FF, no shadow, no text.'
)
TILT = (
    'The first attached image is the rest pose and the second is further along the same bow, both the same character. Draw the first '
    'image again with only the head and ears tilted forward, {change} Everything from the collar down stays exactly as '
    'in the first image: torso, arms, gloves, legs, boots, position. The head keeps the same size, the same face and '
    'the same helmet; it pivots at the neck, it does not move up and the neck does not get longer. The chin moves '
    'toward the chest and the top of the helmet slowly comes into view. Ears follow a little behind the head. '
    'Same scale, pixel style and framing. Solid flat magenta background #FF00FF, no shadow, no text.'
)
FIXES = [
    ('radio_2b', 'radio_1', 'Change only the mouth: closed small smile. Eyes stay exactly as they are, looking straight ahead.'),
    ('load_0', 'load_2', 'Change only the right arm and the flag: arm tilted a little to the left, flag leaning slightly left.'),
    ('load_3', 'load_2', 'Change only the right arm and the flag: arm tilted a little to the right, flag leaning slightly right.'),
    ('load_4', 'load_3', 'Change only the right arm and the flag: arm tilted further to the right, flag leaning right.'),
    ('load_5', 'load_0', 'Change only the right arm and the flag: arm tilted further to the left, flag leaning left.'),
    ('welcome_a', 'welcome_2', 'Change only the raised right arm: hand a little lower and tilted inward. Keep the same closed happy eyes.'),
    ('welcome_b', 'welcome_2', 'Change only the raised right arm: hand a little higher and tilted outward. Keep the same closed happy eyes.'),
    ('pit_2', 'pit_1', 'Change only the ears: both drooping ears swing slightly outward. Eyes and board unchanged.'),
    ('point_2', 'point_1', 'Change only the pointing arm: raised slightly higher.'),
    ('overtake_a', 'overtake_1', 'Swap which leg is in front: the leg nearest the viewer, with the black side stripe, must now sweep backward to the lower left, drawn on top of the other leg at the hip, its boot behind the body. The far leg, partly hidden behind it, now reaches forward to the lower right. Same stride length.'),
    ('nod_mid', 'nod_2+nod_1', 'about one quarter of the way from the rest pose to the full bow. Eyes still open.'),
    ('nod_q1', 'nod_2+nod_mid', 'exactly halfway between the two images: a slight tilt, ears still tall. Eyes still open.'),
    ('nod_h', 'nod_mid', 'Change only the eyes: both eyes half closed in a gentle friendly smile, soft curved upper lids, no eyelashes, same eye size and spacing. The mouth stays the same.'),
    ('overtake_b', 'overtake_1', 'Passing pose: legs passing under the body, the lead knee raised high, the other leg straight below.'),
]


def src(name):
    return HERE / 'poses' / name / 'generated.png'


def run(name, ref, change):
    out = HERE / 'poses' / name
    if (out / 'generated.png').exists():
        print('skip', name, flush=True)
        return True
    prompt = HERE / 'prompts' / f'{name}.txt'
    base = TILT if name in ('nod_mid', 'nod_q1') else LEGS if name.startswith('overtake') else BASE
    prompt.write_text(base.format(change=change), encoding='utf8')
    refs = [arg for part in ref.split('+') for arg in ('--reference', str(src(part)))]
    cmd = [sys.executable, str(ROUTE), 'image', '--prompt-file', str(prompt), *refs,
           '--size', '1024x1024', '--route', 'codex-cli', '--out-dir', str(out)]
    shutil.rmtree(out, ignore_errors=True)
    try:
        done = subprocess.run(cmd, capture_output=True, text=True, encoding='utf8', errors='replace', timeout=420)
    except subprocess.TimeoutExpired:
        print('timeout', name, flush=True)
        return False
    ok = done.returncode == 0 and (out / 'generated.png').exists()
    print('ok' if ok else 'fail', name, '' if ok else done.stdout[-160:] + done.stderr[-160:], flush=True)
    return ok


want = sys.argv[1:]
for name, ref, change in FIXES:
    if want and name not in want:
        continue
    if not all(src(part).exists() for part in ref.split('+')):
        print('wait', name, 'needs', ref, flush=True)
        continue
    if not run(name, ref, change) :
        break
print('finished', flush=True)
