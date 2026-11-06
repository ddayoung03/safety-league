"""frames.json과 frames/를 앱으로 옮긴다: drawable-nodpi의 rabbit_*.png와 ui/view/RabbitFrames.kt.

실행: python design/rabbit-forge/sync_app.py
"""
import json
import shutil
from pathlib import Path

HERE = Path(__file__).resolve().parent
MAIN = HERE.parents[1] / 'app' / 'src' / 'main'
DRAWABLE = MAIN / 'res' / 'drawable-nodpi'
KOTLIN = MAIN / 'java' / 'io' / 'github' / 'ddayoung03' / 'safetyleague' / 'ui' / 'view' / 'RabbitFrames.kt'


def main():
    meta = json.loads((HERE / 'frames.json').read_text(encoding='utf8'))
    for old in DRAWABLE.glob('rabbit_*.png'):
        old.unlink()
    lines = [
        '// design/rabbit-forge/sync_app.py가 만든 파일이다. 직접 고치지 않는다.',
        'package io.github.ddayoung03.safetyleague.ui.view',
        '',
        'import io.github.ddayoung03.safetyleague.R',
        '',
        'internal class RabbitClip(val frames: List<Int>, val durationsMs: List<Long>, val loop: Boolean)',
        '',
        'internal object RabbitFrames {',
    ]
    for pose, action in meta.items():
        for name in action['frames']:
            shutil.copyfile(HERE / 'frames' / name, DRAWABLE / name)
        ids = ', '.join('R.drawable.' + Path(name).stem for name in action['frames'])
        durations = ', '.join(str(ms) for ms in action['durations_ms'])
        loop = 'true' if action['loop'] else 'false'
        lines.append(f'    val {pose} = RabbitClip(listOf({ids}), listOf({durations}), {loop})')
    lines.append('}')
    KOTLIN.write_text('\n'.join(lines) + '\n', encoding='utf8', newline='\n')
    print(f'{sum(len(a["frames"]) for a in meta.values())} frames, {len(meta)} poses')


if __name__ == '__main__':
    main()
