"""Generate rabbit action poses from the approved master through the local Codex route.

Rerun to continue: finished frames are skipped.
"""
import pathlib
import shutil
import subprocess
import sys

HERE = pathlib.Path(__file__).resolve().parent
ROUTE = HERE.parents[2] / '.claude/skills/generate2dmedia/scripts/route_media.py'
MASTER = HERE / 'master/generated.png'

BASE = (
    'The attached image is the exact approved character. Redraw this same rabbit race driver: '
    'identical helmet, ears, face, slim proportions, yellow suit with black side panels, red gloves, '
    'black boots, identical pixel art style, identical outline, and the same character size as the '
    'reference. Only the pose changes. {pose} Solid flat magenta background #FF00FF, no shadow, no ground, '
    'no speed lines, no text, no logos. Whole body and ears inside the frame with generous margin.'
)

FLAG = 'a small checkered racing flag on a thin pole'
BOARD = 'a blank pit board with a yellow frame and an empty dark center, no writing on it'

POSES = {
    'idle_blink': 'Same standing pose as the reference with both eyes closed in a soft blink and a small smile.',
    'win_crouch': f'Happy crouch before a jump, knees bent, holding {FLAG} in the right hand, ears tilted back.',
    'win_jump': f'Mid air at the top of a joyful jump, knees tucked, left fist up, right hand raising {FLAG}, eyes closed happily, ears up.',
    'win_land': f'Landing from a jump with knees slightly bent, big smile, right hand holding {FLAG} out to the side, ears flopping forward.',
    'overtake_1': 'Fast sprint in three quarter view facing right: long stride, left leg forward, right leg back, arms pumping, leaning forward, long ears swept straight back, determined look.',
    'overtake_2': 'Fast sprint in three quarter view facing right: legs passing under the body, one knee high, arms pumping, leaning forward, long ears swept straight back, determined look.',
    'overtake_3': 'Fast sprint in three quarter view facing right: long stride, right leg forward, left leg back, arms pumping, leaning forward, long ears swept straight back, determined look.',
    'overtake_4': 'Fast sprint in three quarter view facing right: both feet off the ground, legs passing, arms pumping, leaning forward, long ears swept straight back, determined look.',
    'pit_1': f'Standing and holding {BOARD} in front of the chest with both hands, both long ears drooping down at the sides, gentle caring smile, eyebrows soft, never angry.',
    'pit_2': f'Standing and holding {BOARD} in front of the chest with both hands, head tilted slightly, both long ears drooping down at the sides, gentle caring smile, eyes closed softly.',
    'radio_1': 'Standing, right hand holding a small black radio handset with a short antenna next to the helmet, mouth open talking, one ear folded over at the middle.',
    'radio_2': 'Standing, right hand holding a small black radio handset with a short antenna next to the helmet, mouth closed in a smile, one ear folded over at the middle.',
    'load_1': f'Standing, right arm raised high to the left waving {FLAG}, flag leaning left, focused smile.',
    'load_2': f'Standing, right arm raised straight up waving {FLAG}, flag upright, focused smile.',
    'load_3': f'Standing, right arm raised high to the right waving {FLAG}, flag leaning right, focused smile.',
    'welcome_1': 'Standing, right hand raised next to the head with an open palm waving hello, big smile.',
    'welcome_2': 'Standing, right hand raised a little higher tilted outward waving hello, big smile, eyes closed happily.',
    'nod_1': 'Standing, head tilted slightly down in a small nod, ears tilted forward, eyes closed happily, smile.',
    'nod_2': 'Standing, head lifted slightly up after a nod, ears up, bright smile.',
    'point_1': 'Standing, right arm stretched straight out to the right side pointing with the index finger, looking toward it, smile.',
    'point_2': 'Standing, right arm stretched out to the right pointing a little higher, leaning slightly toward it, smile, ears tilted toward the same side.',
}


def run(name, pose):
    out = HERE / 'poses' / name
    if (out / 'generated.png').exists():
        print('skip', name, flush=True)
        return True
    (HERE / 'prompts').mkdir(exist_ok=True)
    out.parent.mkdir(exist_ok=True)
    prompt = HERE / 'prompts' / f'{name}.txt'
    prompt.write_text(BASE.format(pose=pose), encoding='utf8')
    cmd = [sys.executable, str(ROUTE), 'image', '--prompt-file', str(prompt), '--reference', str(MASTER),
           '--size', '1024x1024', '--out-dir', str(out)]
    for attempt in (1, 2):
        shutil.rmtree(out, ignore_errors=True)  # route_media needs a new folder
        try:
            done = subprocess.run(cmd, capture_output=True, text=True, encoding='utf8', errors='replace', timeout=420)
        except subprocess.TimeoutExpired:
            print('timeout', name, attempt, flush=True)
            continue
        if done.returncode == 0 and (out / 'generated.png').exists():
            print('ok', name, flush=True)
            return True
        print('fail', name, attempt, done.stdout[-200:], done.stderr[-200:], flush=True)
    return False


names = sys.argv[1:] or list(POSES)
bad = [n for n in names if not run(n, POSES[n])]
print('done, failed:', bad, flush=True)
