"""Manifold-aware filtering, IK and reconstruction primitives."""
from __future__ import annotations

import numpy as np
from anim_data import qmul, qconj, qnorm, slerp


# ------------------------------------------------------------ quaternion log/exp
def qlog(q):
    """Unit quaternion (...,4 xyzw) -> rotation vector (...,3), |r| = angle."""
    q = qnorm(np.asarray(q, dtype=np.float64))
    v, w = q[..., :3], np.clip(q[..., 3], -1.0, 1.0)
    # keep on the +w hemisphere so the log is the shortest arc
    sign = np.where(w < 0, -1.0, 1.0)[..., None]
    v, w = v * sign, np.abs(w)
    n = np.linalg.norm(v, axis=-1, keepdims=True)
    theta = 2.0 * np.arctan2(n[..., 0], w)          # (...,)
    small = n[..., 0] < 1e-12
    axis = np.where(n < 1e-12, 0.0, v / np.where(n < 1e-12, 1.0, n))
    r = axis * theta[..., None]
    r[small] = 0.0
    return r


def qexp(r):
    """Rotation vector (...,3) -> unit quaternion (...,4 xyzw)."""
    r = np.asarray(r, dtype=np.float64)
    theta = np.linalg.norm(r, axis=-1, keepdims=True)
    small = theta[..., 0] < 1e-12
    axis = np.where(theta < 1e-12, 0.0, r / np.where(theta < 1e-12, 1.0, theta))
    s = np.sin(theta / 2.0)
    q = np.concatenate([axis * s, np.cos(theta / 2.0)], axis=-1)
    q[small] = np.array([0.0, 0.0, 0.0, 1.0])
    return qnorm(q)


# --------------------------------------------------- robust local-poly smoothing
def smooth_quat_track(q, half, degree=2, robust=True, weight=None, iters=2):
    """Edge-preserving local polynomial regression on the rotation manifold.

    For every frame the neighbourhood is mapped into the tangent space at that
    frame's own rotation, a low-order polynomial is fitted, and the value at the
    centre is taken. Because the fit is quadratic it preserves velocity and
    curvature (real motion peaks, windlass twists) instead of flattening them
    the way a box/gaussian average does. `robust` adds IRLS reweighting so a
    single corrupted frame cannot drag its neighbours.

    weight: optional (F,) array in [0,1] blending filtered vs original per frame.
    """
    q = qnorm(np.asarray(q, dtype=np.float64))
    F = len(q)
    if half < 1 or F < 3:
        return q.copy()
    out = np.empty_like(q)
    idx = np.arange(-half, half + 1)
    for t in range(F):
        lo, hi = max(0, t - half), min(F, t + half + 1)
        k = idx[(lo - t + half):(hi - t + half)].astype(np.float64)
        nb = q[lo:hi]
        # tangent space at q[t]
        rel = qmul(qconj(np.broadcast_to(q[t], nb.shape)), nb)
        v = qlog(rel)                                    # (n,3)
        # tricube spatial weights
        u = np.abs(k) / (half + 1.0)
        w = (1.0 - u ** 3) ** 3
        deg = min(degree, len(k) - 1)
        V = np.vander(k, deg + 1, increasing=True)       # (n, deg+1)
        coef = None
        for _ in range(iters if robust else 1):
            Wd = w[:, None] * V
            try:
                coef, *_ = np.linalg.lstsq(Wd.T @ V, Wd.T @ v, rcond=None)
            except np.linalg.LinAlgError:
                coef = None
                break
            if not robust:
                break
            resid = np.linalg.norm(v - V @ coef, axis=-1)
            s = np.median(resid) * 1.4826 + 1e-9
            w = w * (1.0 / (1.0 + (resid / (3.0 * s)) ** 2))
        vhat = coef[0] if coef is not None else v[t - lo]
        out[t] = qmul(q[t], qexp(vhat))
    out = qnorm(out)
    if weight is not None:
        wgt = np.clip(np.asarray(weight, dtype=np.float64), 0.0, 1.0)[:, None]
        out = slerp(q, out, wgt[:, 0])
    return out


def smooth_vec_track(x, half, degree=2, weight=None):
    """Same local-polynomial idea for vector tracks (root translation)."""
    x = np.asarray(x, dtype=np.float64)
    F = len(x)
    if half < 1 or F < 3:
        return x.copy()
    out = np.empty_like(x)
    idx = np.arange(-half, half + 1)
    for t in range(F):
        lo, hi = max(0, t - half), min(F, t + half + 1)
        k = idx[(lo - t + half):(hi - t + half)].astype(np.float64)
        u = np.abs(k) / (half + 1.0)
        w = (1.0 - u ** 3) ** 3
        deg = min(degree, len(k) - 1)
        V = np.vander(k, deg + 1, increasing=True)
        Wd = w[:, None] * V
        coef, *_ = np.linalg.lstsq(Wd.T @ V, Wd.T @ x[lo:hi], rcond=None)
        out[t] = coef[0]
    if weight is not None:
        wgt = np.clip(np.asarray(weight, dtype=np.float64), 0.0, 1.0)[:, None]
        out = x * (1 - wgt) + out * wgt
    return out


def gauss_smooth(x, sigma):
    """Reflect-padded gaussian smoothing along axis 0."""
    x = np.asarray(x, dtype=np.float64)
    if sigma <= 0:
        return x.copy()
    rad = max(1, int(np.ceil(sigma * 3)))
    k = np.exp(-0.5 * (np.arange(-rad, rad + 1) / sigma) ** 2)
    k /= k.sum()
    pad = [(rad, rad)] + [(0, 0)] * (x.ndim - 1)
    xp = np.pad(x, pad, mode="reflect")
    out = np.zeros_like(x)
    for i, kk in enumerate(k):
        out += kk * xp[i:i + len(x)]
    return out


# ------------------------------------------------------------------ easing utils
def smoothstep(t):
    t = np.clip(t, 0.0, 1.0)
    return t * t * (3.0 - 2.0 * t)


def smootherstep(t):
    t = np.clip(t, 0.0, 1.0)
    return t * t * t * (t * (t * 6.0 - 15.0) + 10.0)


def ramp(F, start, end, rise, fall):
    """Trapezoidal 0->1->0 blend mask over frames [start,end) with eased edges."""
    m = np.zeros(F)
    for f in range(max(0, start), min(F, end)):
        a = smoothstep((f - start + 1) / max(rise, 1e-9)) if rise > 0 else 1.0
        b = smoothstep((end - 1 - f) / max(fall, 1e-9)) if fall > 0 else 1.0
        m[f] = min(a, b)
    return m


# ---------------------------------------------------------------- two-bone IK
def two_bone_ik(root, mid, eff, target, pole, l1, l2, eps=1e-7):
    """Analytic 2-bone IK.

    root/mid/eff/target/pole: (...,3) world positions. Returns the new world
    positions (mid_new, eff_new) with the original bone lengths preserved and
    the bend plane taken from `pole`. Reach is clamped so the limb never snaps
    straight through its target.
    """
    root = np.asarray(root, dtype=np.float64)
    target = np.asarray(target, dtype=np.float64)
    d = target - root
    dist = np.linalg.norm(d, axis=-1, keepdims=True)
    maxr = (l1 + l2) * 0.999
    minr = abs(l1 - l2) * 1.001 + eps
    dist_c = np.clip(dist, minr, maxr)
    dirv = d / np.maximum(dist, eps)

    # angle at the root between the limb axis and the upper bone
    cos_a = np.clip((l1 * l1 + dist_c ** 2 - l2 * l2) / (2 * l1 * dist_c), -1.0, 1.0)
    a = np.arccos(cos_a)

    # bend axis from the pole vector, orthogonalised against the limb axis
    pv = np.asarray(pole, dtype=np.float64) - root
    pv = pv - dirv * np.sum(pv * dirv, axis=-1, keepdims=True)
    n = np.linalg.norm(pv, axis=-1, keepdims=True)
    # degenerate pole -> pick any orthogonal direction
    fallback = np.tile(np.array([0.0, 1.0, 0.0]), pv.shape[:-1] + (1,))
    fb = fallback - dirv * np.sum(fallback * dirv, axis=-1, keepdims=True)
    fbn = np.linalg.norm(fb, axis=-1, keepdims=True)
    fb = np.where(fbn < eps, np.array([1.0, 0.0, 0.0]), fb / np.maximum(fbn, eps))
    pv = np.where(n < eps, fb, pv / np.maximum(n, eps))

    axis = np.cross(dirv, pv)
    an = np.linalg.norm(axis, axis=-1, keepdims=True)
    axis = np.where(an < eps, np.array([0.0, 0.0, 1.0]), axis / np.maximum(an, eps))

    # rotate the limb axis by +a about `axis` to get the upper-bone direction
    def rot(v, k, ang):
        c, s = np.cos(ang), np.sin(ang)
        return v * c + np.cross(k, v) * s + k * np.sum(k * v, axis=-1, keepdims=True) * (1 - c)

    upper = rot(dirv, axis, a)
    mid_new = root + upper * l1
    eff_new = root + dirv * dist_c
    return mid_new, eff_new


def aim_rotation(from_dir, to_dir):
    """Shortest-arc quaternion rotating from_dir onto to_dir. (...,3)->(...,4)"""
    a = from_dir / np.maximum(np.linalg.norm(from_dir, axis=-1, keepdims=True), 1e-12)
    b = to_dir / np.maximum(np.linalg.norm(to_dir, axis=-1, keepdims=True), 1e-12)
    d = np.sum(a * b, axis=-1, keepdims=True)
    axis = np.cross(a, b)
    n = np.linalg.norm(axis, axis=-1, keepdims=True)
    q = np.concatenate([axis, 1.0 + d], axis=-1)
    # 180-degree case: any orthogonal axis
    opp = (d[..., 0] < -1 + 1e-9) & (n[..., 0] < 1e-9)
    if np.any(opp):
        ref = np.tile(np.array([1.0, 0.0, 0.0]), a.shape[:-1] + (1,))
        alt = np.cross(a, ref)
        bad = np.linalg.norm(alt, axis=-1) < 1e-6
        alt[bad] = np.cross(a, np.array([0.0, 1.0, 0.0]))[bad]
        q[opp] = np.concatenate([alt, np.zeros(alt.shape[:-1] + (1,))], axis=-1)[opp]
    return qnorm(q)
