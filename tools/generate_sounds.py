#!/usr/bin/env python3
"""
Procedural jet-engine audio for Create: Jet Engines.

Every sample is synthesised from scratch here (turbine harmonics + shaped noise),
so the shipped .ogg files are original work with no licensing encumbrance.

Loop samples are equal-power crossfaded end-to-start so they tile seamlessly with
Minecraft's looping sound instances.

Run:  python3 tools/generate_sounds.py
"""

import json
import os
import subprocess
import wave

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "create_jet_engines"
SND_DIR = os.path.join(ROOT, "src/main/resources/assets", MODID, "sounds")
ASSETS = os.path.join(ROOT, "src/main/resources/assets", MODID)
os.makedirs(SND_DIR, exist_ok=True)

SR = 44100
rng = np.random.default_rng(20260801)

try:
    import imageio_ffmpeg
    FFMPEG = imageio_ffmpeg.get_ffmpeg_exe()
except Exception:
    FFMPEG = "ffmpeg"


# ---------------------------------------------------------------------------
# synthesis primitives
# ---------------------------------------------------------------------------

def t_axis(dur):
    return np.linspace(0, dur, int(SR * dur), endpoint=False)


def one_pole_lp(x, cutoff):
    """Simple one-pole low pass; cutoff in Hz."""
    a = np.exp(-2 * np.pi * cutoff / SR)
    y = np.empty_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def one_pole_hp(x, cutoff):
    return x - one_pole_lp(x, cutoff)


def noise(n):
    return rng.standard_normal(n)


def turbine(t, f0, harmonics=9, detune=0.0015, tilt=1.25):
    """
    Compressor/fan whine: a stack of harmonics with slight inharmonicity, which is
    what gives real turbomachinery its metallic buzz rather than a pure tone.
    """
    out = np.zeros_like(t)
    for k in range(1, harmonics + 1):
        f = f0 * k * (1.0 + detune * k * k)
        amp = 1.0 / (k ** tilt)
        phase = rng.uniform(0, 2 * np.pi)
        out += amp * np.sin(2 * np.pi * f * t + phase)
    return out / np.max(np.abs(out) + 1e-9)


def rumble(n, cutoff=110.0):
    return one_pole_lp(noise(n), cutoff)


def roar(n, low=180.0, high=40.0):
    """Broadband combustion roar: band-limited noise with a low emphasis."""
    x = noise(n)
    return one_pole_hp(one_pole_lp(x, low), high)


def normalize(x, peak=0.86):
    m = np.max(np.abs(x))
    return x if m < 1e-9 else x / m * peak


def fade(x, ms_in=25, ms_out=25):
    n_in = int(SR * ms_in / 1000)
    n_out = int(SR * ms_out / 1000)
    y = x.copy()
    if n_in:
        y[:n_in] *= np.linspace(0, 1, n_in)
    if n_out:
        y[-n_out:] *= np.linspace(1, 0, n_out)
    return y


def loopify(x, xfade_ms=220):
    """
    Equal-power crossfade of the tail back over the head so the sample loops
    without an audible seam.
    """
    n = int(SR * xfade_ms / 1000)
    if n * 2 >= len(x):
        return x
    head, tail = x[:n], x[-n:]
    w = np.linspace(0, 1, n)
    blended = tail * np.cos(w * np.pi / 2) + head * np.sin(w * np.pi / 2)
    return np.concatenate([blended, x[n:-n]])


def write_ogg(name, samples, quality=5):
    samples = np.clip(samples, -1.0, 1.0)
    pcm = (samples * 32767).astype("<i2")
    wav_path = os.path.join(SND_DIR, name + ".wav")
    ogg_path = os.path.join(SND_DIR, name + ".ogg")
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(
        [FFMPEG, "-y", "-loglevel", "error", "-i", wav_path,
         "-c:a", "libvorbis", "-q:a", str(quality), "-ac", "1", ogg_path],
        check=True)
    os.remove(wav_path)
    return os.path.getsize(ogg_path)


# ---------------------------------------------------------------------------
# the six samples
# ---------------------------------------------------------------------------

def make_start():
    """Igniter click, spooling whine rising in pitch, settling to idle."""
    dur = 4.2
    t = t_axis(dur)
    n = len(t)
    # spool curve 0 -> 1, fast at first then easing
    s = 1 - np.exp(-t * 0.95)
    s /= s[-1]

    f0 = 55 + 165 * s
    phase = 2 * np.pi * np.cumsum(f0) / SR
    whine = np.zeros(n)
    for k in range(1, 10):
        whine += (1.0 / k ** 1.3) * np.sin(phase * k * (1 + 0.0016 * k * k))
    whine = normalize(whine) * (0.20 + 0.55 * s)

    low = rumble(n, 95) * (0.25 + 0.6 * s)
    air = one_pole_hp(one_pole_lp(noise(n), 2400), 500) * 0.13 * s

    # igniter pops in the first half second
    ign = np.zeros(n)
    for pos in (0.05, 0.13, 0.24):
        i = int(pos * SR)
        env = np.exp(-np.arange(n - i) / (SR * 0.02))
        ign[i:] += noise(n - i)[: n - i] * env * 0.55
    return fade(normalize(whine + low + air + ign), 15, 120)


def make_idle():
    """Steady low-power turbine loop."""
    dur = 2.6
    t = t_axis(dur)
    n = len(t)
    whine = turbine(t, 208.0) * 0.34
    # slow amplitude wobble so the loop does not sound sterile
    wob = 1 + 0.05 * np.sin(2 * np.pi * 0.9 * t)
    low = rumble(n, 90) * 0.55
    air = one_pole_hp(one_pole_lp(noise(n), 1800), 400) * 0.07
    return normalize(loopify((whine * wob) + low + air)) * 0.8


def make_power():
    """High dry power: brighter whine, much stronger core roar."""
    dur = 2.6
    t = t_axis(dur)
    n = len(t)
    whine = turbine(t, 335.0, harmonics=11) * 0.38
    core = roar(n, 260, 35) * 0.95
    low = rumble(n, 70) * 0.5
    air = one_pole_hp(one_pole_lp(noise(n), 3600), 900) * 0.16
    return normalize(loopify(whine + core + low + air))


def make_stop():
    """Spool-down: pitch and level decay away."""
    dur = 3.6
    t = t_axis(dur)
    n = len(t)
    s = np.exp(-t * 0.85)
    f0 = 40 + 195 * s
    phase = 2 * np.pi * np.cumsum(f0) / SR
    whine = np.zeros(n)
    for k in range(1, 9):
        whine += (1.0 / k ** 1.3) * np.sin(phase * k * (1 + 0.0016 * k * k))
    whine = normalize(whine) * (0.55 * s)
    low = rumble(n, 85) * 0.5 * s
    return fade(normalize(whine + low), 20, 260)


def make_afterburner_ignite():
    """The reheat 'whoomph': a fast broadband swell with a low thump under it."""
    dur = 1.35
    t = t_axis(dur)
    n = len(t)
    env = np.minimum(1.0, t / 0.06) * np.exp(-t * 2.6)
    body = roar(n, 700, 25) * env * 1.4
    thump = np.sin(2 * np.pi * (46 * np.exp(-t * 2.2)) * t) * np.exp(-t * 4.5) * 0.8
    crack = one_pole_hp(noise(n), 2600) * np.exp(-t * 16) * 0.35
    return fade(normalize(body + thump + crack), 5, 90)


def make_afterburner_loop():
    """Sustained reheat roar: heavy low-frequency energy plus a ragged upper band."""
    dur = 2.6
    t = t_axis(dur)
    n = len(t)
    core = roar(n, 340, 22) * 1.25
    sub = rumble(n, 55) * 0.95
    hiss = one_pole_hp(one_pole_lp(noise(n), 5200), 1400) * 0.22
    whine = turbine(t, 352.0, harmonics=8) * 0.16
    flutter = 1 + 0.09 * np.sin(2 * np.pi * 7.3 * t) + 0.05 * np.sin(2 * np.pi * 3.1 * t)
    return normalize(loopify((core + sub + hiss + whine) * flutter))


SAMPLES = {
    "engine_start": make_start,
    "engine_idle": make_idle,
    "engine_power": make_power,
    "engine_stop": make_stop,
    "afterburner_ignite": make_afterburner_ignite,
    "afterburner_loop": make_afterburner_loop,
}


def main():
    entries = {}
    for name, fn in SAMPLES.items():
        size = write_ogg(name, fn())
        print(f"  {name}.ogg  {size/1024:.1f} KiB")
        entries[name] = {
            "category": "block",
            "subtitle": f"subtitles.{MODID}.{name}",
            "sounds": [{"name": f"{MODID}:{name}", "stream": False}],
        }
    with open(os.path.join(ASSETS, "sounds.json"), "w") as fh:
        json.dump(entries, fh, indent=2)
    print(f"sounds.json written with {len(entries)} entries")


if __name__ == "__main__":
    main()
