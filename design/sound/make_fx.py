"""결과 화면 효과음 4종을 합성한다. 표준 라이브러리만 쓴다.
사용: python design/sound/make_fx.py  (app/src/main/res/raw/fx_*.wav 를 덮어쓴다)
44.1kHz 16bit 모노, 사인과 잡음, 짧은 어택/릴리스로 클릭을 막고 음량은 작게 둔다."""
import math, random, struct, wave
from pathlib import Path

RATE = 44100
OUT = Path(__file__).resolve().parents[2] / "app/src/main/res/raw"


def env(i, n, attack=0.008, release=0.04):
    t = i / RATE
    end = (n - i) / RATE
    return min(1.0, t / attack, end / release)


def tone(freq, ms, level=0.3, sweep_to=None):
    n = RATE * ms // 1000
    phase, out = 0.0, []
    for i in range(n):
        f = freq if sweep_to is None else freq + (sweep_to - freq) * i / n
        phase += 2 * math.pi * f / RATE
        out.append(level * env(i, n) * math.sin(phase))
    return out


def whoosh(ms, level=0.25):
    """잡음을 오르는 필터로 훑는다."""
    rnd, n, low, out = random.Random(7), RATE * ms // 1000, 0.0, []
    for i in range(n):
        a = 0.05 + 0.6 * i / n
        low += a * (rnd.uniform(-1, 1) - low)
        out.append(level * env(i, n, 0.03, 0.08) * low * 2)
    return out


def save(name, samples):
    with wave.open(str(OUT / name), "wb") as w:
        w.setnchannels(1); w.setsampwidth(2); w.setframerate(RATE)
        w.writeframes(b"".join(struct.pack("<h", int(max(-1, min(1, s)) * 32767)) for s in samples))


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    save("fx_light.wav", tone(440, 120, 0.25))
    save("fx_go.wav", tone(880, 280, 0.3))
    save("fx_overtake.wav", [a + b for a, b in zip(whoosh(240, 0.15), tone(300, 240, 0.12, sweep_to=900))])
    save("fx_win.wav", tone(523, 150) + tone(659, 150) + tone(784, 450, 0.3))
