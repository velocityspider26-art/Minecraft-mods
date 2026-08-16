"""Animation sampling container + forward kinematics + motion metrics."""
from __future__ import annotations

import numpy as np
from pygltflib import GLTF2

from gltf_io import read_accessor, Skeleton, quat_to_mat, mat_to_quat, compose


# ---------------------------------------------------------------- quaternions
def qnorm(q):
    n = np.linalg.norm(q, axis=-1, keepdims=True)
    return q / np.where(n == 0, 1.0, n)


def qmul(a, b):
    ax, ay, az, aw = np.moveaxis(a, -1, 0)
    bx, by, bz, bw = np.moveaxis(b, -1, 0)
    return np.stack([
        aw * bx + ax * bw + ay * bz - az * by,
        aw * by - ax * bz + ay * bw + az * bx,
        aw * bz + ax * by - ay * bx + az * bw,
        aw * bw - ax * bx - ay * by - az * bz,
    ], axis=-1)


def qconj(q):
    return q * np.array([-1.0, -1.0, -1.0, 1.0])


def qangle_between(a, b):
    """Geodesic angle (radians) between two unit quaternion orientations."""
    d = np.abs(np.sum(qnorm(a) * qnorm(b), axis=-1))
    return 2.0 * np.arccos(np.clip(d, -1.0, 1.0))


def make_hemisphere_continuous(q, axis=0):
    """Flip signs along `axis` so consecutive quaternions stay in one hemisphere."""
    q = np.array(q, dtype=np.float64, copy=True)
    q = np.moveaxis(q, axis, 0)
    for i in range(1, q.shape[0]):
        flip = np.sum(q[i] * q[i - 1], axis=-1) < 0
        q[i][flip] *= -1.0
    return np.moveaxis(q, 0, axis)


def slerp(q0, q1, t):
    """Spherical linear interpolation. q0,q1 (...,4); t scalar or (...,)."""
    q0, q1 = qnorm(q0), qnorm(q1)
    d = np.sum(q0 * q1, axis=-1, keepdims=True)
    q1 = np.where(d < 0, -q1, q1)
    d = np.abs(d)
    t = np.asarray(t, dtype=np.float64)
    if t.ndim < q0.ndim:
        t = t.reshape(t.shape + (1,) * (q0.ndim - t.ndim))
    out = np.empty(np.broadcast_shapes(q0.shape, q1.shape, t.shape))
    lin = (d > 0.9995)[..., 0]
    theta = np.arccos(np.clip(d, -1.0, 1.0))
    s = np.sin(theta)
    s = np.where(s == 0, 1.0, s)
    sl = (np.sin((1 - t) * theta) / s) * q0 + (np.sin(t * theta) / s) * q1
    li = (1 - t) * q0 + t * q1
    out[lin] = li[lin]
    out[~lin] = sl[~lin]
    return qnorm(out)


# ------------------------------------------------------------------ container
class Anim:
    """Dense per-frame TRS for every node, plus FK."""

    def __init__(self, gltf: GLTF2, anim_index: int = 0):
        self.gltf = gltf
        self.skel = Skeleton(gltf)
        self.anim_index = anim_index
        an = gltf.animations[anim_index]
        self.name = an.name
        N = self.skel.n

        # Collect the union timeline (all channels share it in practice).
        times = None
        for ch in an.channels:
            t = read_accessor(gltf, an.samplers[ch.sampler].input)
            times = t if times is None else np.union1d(times, t)
        self.times = np.asarray(times, dtype=np.float64)
        F = len(self.times)
        self.F = F
        self.duration = float(self.times[-1] - self.times[0])
        dt = np.diff(self.times)
        self.dt = float(np.mean(dt))
        self.fps = 1.0 / self.dt

        # Initialise every node to its rest pose, then overwrite animated ones.
        self.trans = np.repeat(self.skel.rest_t[None], F, axis=0)
        self.rot = np.repeat(self.skel.rest_r[None], F, axis=0)
        self.scale = np.repeat(self.skel.rest_s[None], F, axis=0)

        self.animated = {"translation": set(), "rotation": set(), "scale": set()}
        self.raw_samplers = {}

        for ch in an.channels:
            s = an.samplers[ch.sampler]
            node = ch.target.node
            path = ch.target.path
            tin = read_accessor(gltf, s.input)
            out = read_accessor(gltf, s.output)
            interp = s.interpolation or "LINEAR"
            if interp == "CUBICSPLINE":
                out = out.reshape(len(tin), 3, -1)[:, 1, :]  # take value, drop tangents
            self.animated[path].add(node)
            self.raw_samplers[(node, path)] = (tin, out, interp)

            if path == "rotation":
                q = qnorm(out)
                q = make_hemisphere_continuous(q, axis=0)
                self.rot[:, node, :] = self._resample_rot(tin, q, self.times, interp)
            elif path == "translation":
                self.trans[:, node, :] = self._resample_vec(tin, out, self.times, interp)
            elif path == "scale":
                self.scale[:, node, :] = self._resample_vec(tin, out, self.times, interp)

        self._world = None

    @staticmethod
    def _resample_vec(tin, out, times, interp):
        if len(tin) == len(times) and np.allclose(tin, times):
            return out.copy()
        if interp == "STEP":
            idx = np.searchsorted(tin, times, side="right") - 1
            return out[np.clip(idx, 0, len(tin) - 1)]
        return np.stack([np.interp(times, tin, out[:, k]) for k in range(out.shape[1])], axis=-1)

    @staticmethod
    def _resample_rot(tin, q, times, interp):
        if len(tin) == len(times) and np.allclose(tin, times):
            return q.copy()
        idx = np.clip(np.searchsorted(tin, times, side="right") - 1, 0, len(tin) - 2)
        if interp == "STEP":
            return q[np.clip(np.searchsorted(tin, times, side="right") - 1, 0, len(tin) - 1)]
        t0, t1 = tin[idx], tin[idx + 1]
        u = np.where(t1 > t0, (times - t0) / np.where(t1 > t0, t1 - t0, 1.0), 0.0)
        return slerp(q[idx], q[idx + 1], u)

    # ------------------------------------------------------------------- FK
    def fk(self, force=False):
        """Returns (world_matrices (F,N,4,4), world_pos (F,N,3), world_quat (F,N,4))."""
        if self._world is not None and not force:
            return self._world
        F, N = self.F, self.skel.n
        local = compose(self.trans, self.rot, self.scale)  # (F,N,4,4)
        W = np.zeros((F, N, 4, 4))
        for i in self.skel.order:
            p = self.skel.parent[i]
            W[:, i] = local[:, i] if p == -1 else W[:, p] @ local[:, i]
        pos = W[:, :, :3, 3]
        R = W[:, :, :3, :3]
        s = np.linalg.norm(R, axis=-2)
        s = np.where(s == 0, 1.0, s)
        quat = mat_to_quat(R / s[:, :, None, :])
        self._world = (W, pos, quat)
        return self._world

    def world_pos(self):
        return self.fk()[1]

    def world_quat(self):
        return self.fk()[2]

    def copy(self):
        import copy as _c
        new = _c.copy(self)
        new.trans = self.trans.copy()
        new.rot = self.rot.copy()
        new.scale = self.scale.copy()
        new._world = None
        return new


# -------------------------------------------------------------------- metrics
def derivatives(x, dt):
    """Central-difference velocity/accel/jerk for (F,...) signals."""
    v = np.gradient(x, dt, axis=0)
    a = np.gradient(v, dt, axis=0)
    j = np.gradient(a, dt, axis=0)
    return v, a, j


def ang_speed(q, dt):
    """Per-frame geodesic angular speed (rad/s) from a (F,...,4) quat track."""
    d = qangle_between(q[1:], q[:-1]) / dt
    return np.concatenate([d[:1], d], axis=0)


def robust_z(x, win=15):
    """Median-absolute-deviation z-score against a local window (per column)."""
    x = np.asarray(x, dtype=np.float64)
    F = x.shape[0]
    half = win // 2
    med = np.empty_like(x)
    mad = np.empty_like(x)
    for i in range(F):
        lo, hi = max(0, i - half), min(F, i + half + 1)
        seg = x[lo:hi]
        m = np.median(seg, axis=0)
        med[i] = m
        mad[i] = np.median(np.abs(seg - m), axis=0)
    scale = 1.4826 * mad
    scale = np.where(scale < 1e-9, 1e-9, scale)
    return (x - med) / scale
