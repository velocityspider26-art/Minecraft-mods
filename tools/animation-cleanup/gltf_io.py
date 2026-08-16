"""Low-level glTF/GLB accessor reading and skeleton/animation extraction.

Pure numpy + pygltflib. No Blender dependency, so this stage runs anywhere.
"""
from __future__ import annotations

import numpy as np
from pygltflib import GLTF2

COMPONENT_DTYPE = {
    5120: np.int8,
    5121: np.uint8,
    5122: np.int16,
    5123: np.uint16,
    5125: np.uint32,
    5126: np.float32,
}
COMPONENT_SIZE = {k: np.dtype(v).itemsize for k, v in COMPONENT_DTYPE.items()}
TYPE_COUNT = {
    "SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4,
    "MAT2": 4, "MAT3": 9, "MAT4": 16,
}
# Per glTF spec, normalized integer -> float conversion divisors.
NORMALIZE_DIV = {5120: 127.0, 5121: 255.0, 5122: 32767.0, 5123: 65535.0}


def _buffer_bytes(gltf: GLTF2, buffer_index: int) -> bytes:
    """Return raw bytes for a buffer, handling GLB blob and data: URIs."""
    buf = gltf.buffers[buffer_index]
    uri = getattr(buf, "uri", None)
    if uri is None:
        blob = gltf.binary_blob()
        if blob is None:
            raise ValueError("GLB has no binary blob but buffer has no URI")
        return blob
    if uri.startswith("data:"):
        import base64
        return base64.b64decode(uri.split(",", 1)[1])
    raise ValueError(f"External buffer URI not supported here: {uri}")


def read_accessor(gltf: GLTF2, index: int) -> np.ndarray:
    """Decode an accessor into a float64/int array of shape (count, ncomp).

    Handles byteStride (interleaved data), normalized integers, and sparse
    accessors. Returns SCALAR accessors as shape (count,).
    """
    acc = gltf.accessors[index]
    ncomp = TYPE_COUNT[acc.type]
    dtype = COMPONENT_DTYPE[acc.componentType]
    csize = COMPONENT_SIZE[acc.componentType]
    elem_size = ncomp * csize

    if acc.bufferView is None:
        out = np.zeros((acc.count, ncomp), dtype=dtype)
    else:
        bv = gltf.bufferViews[acc.bufferView]
        data = _buffer_bytes(gltf, bv.buffer)
        bv_off = bv.byteOffset or 0
        acc_off = acc.byteOffset or 0
        stride = bv.byteStride or elem_size
        base = bv_off + acc_off
        out = np.empty((acc.count, ncomp), dtype=dtype)
        if stride == elem_size:
            seg = data[base: base + acc.count * elem_size]
            out = np.frombuffer(seg, dtype=dtype, count=acc.count * ncomp).reshape(acc.count, ncomp).copy()
        else:
            for i in range(acc.count):
                s = base + i * stride
                out[i] = np.frombuffer(data[s: s + elem_size], dtype=dtype, count=ncomp)

    # Sparse substitution.
    sparse = getattr(acc, "sparse", None)
    if sparse is not None and getattr(sparse, "count", 0):
        idx_bv = gltf.bufferViews[sparse.indices.bufferView]
        idx_data = _buffer_bytes(gltf, idx_bv.buffer)
        idx_dtype = COMPONENT_DTYPE[sparse.indices.componentType]
        idx_base = (idx_bv.byteOffset or 0) + (sparse.indices.byteOffset or 0)
        indices = np.frombuffer(
            idx_data[idx_base: idx_base + sparse.count * np.dtype(idx_dtype).itemsize],
            dtype=idx_dtype, count=sparse.count,
        )
        val_bv = gltf.bufferViews[sparse.values.bufferView]
        val_data = _buffer_bytes(gltf, val_bv.buffer)
        val_base = (val_bv.byteOffset or 0) + (sparse.values.byteOffset or 0)
        values = np.frombuffer(
            val_data[val_base: val_base + sparse.count * elem_size],
            dtype=dtype, count=sparse.count * ncomp,
        ).reshape(sparse.count, ncomp)
        out = out.copy()
        out[indices.astype(np.int64)] = values

    res = out.astype(np.float64)
    if getattr(acc, "normalized", False) and acc.componentType in NORMALIZE_DIV:
        res = res / NORMALIZE_DIV[acc.componentType]
        if acc.componentType in (5120, 5122):
            res = np.maximum(res, -1.0)
    if ncomp == 1:
        res = res.reshape(-1)
    return res


class Skeleton:
    """Node graph + skin info extracted from a glTF document."""

    def __init__(self, gltf: GLTF2):
        self.gltf = gltf
        self.nodes = gltf.nodes or []
        self.n = len(self.nodes)
        self.names = [
            (nd.name if getattr(nd, "name", None) else f"node_{i}")
            for i, nd in enumerate(self.nodes)
        ]
        self.parent = [-1] * self.n
        for i, nd in enumerate(self.nodes):
            for c in (nd.children or []):
                self.parent[c] = i
        self.children = [list(nd.children or []) for nd in self.nodes]
        self.roots = [i for i in range(self.n) if self.parent[i] == -1]

        # Rest-pose TRS per node.
        self.rest_t = np.zeros((self.n, 3))
        self.rest_r = np.zeros((self.n, 4))
        self.rest_r[:, 3] = 1.0  # glTF quaternion order is (x, y, z, w)
        self.rest_s = np.ones((self.n, 3))
        self.has_matrix = [False] * self.n
        for i, nd in enumerate(self.nodes):
            m = getattr(nd, "matrix", None)
            if m:
                self.has_matrix[i] = True
                M = np.array(m, dtype=np.float64).reshape(4, 4).T  # glTF is column-major
                t, r, s = decompose(M)
                self.rest_t[i], self.rest_r[i], self.rest_s[i] = t, r, s
            else:
                if getattr(nd, "translation", None):
                    self.rest_t[i] = nd.translation
                if getattr(nd, "rotation", None):
                    self.rest_r[i] = nd.rotation
                if getattr(nd, "scale", None):
                    self.rest_s[i] = nd.scale

        # Joints referenced by skins.
        self.skin_joints: list[list[int]] = []
        for sk in (gltf.skins or []):
            self.skin_joints.append(list(sk.joints or []))
        self.joint_set = sorted({j for js in self.skin_joints for j in js})

        # Topological order (parents before children).
        self.order = []
        stack = list(self.roots)[::-1]
        while stack:
            i = stack.pop()
            self.order.append(i)
            for c in reversed(self.children[i]):
                stack.append(c)

    def depth(self, i: int) -> int:
        d = 0
        while self.parent[i] != -1:
            i = self.parent[i]
            d += 1
        return d

    def chain_to_root(self, i: int) -> list[int]:
        out = [i]
        while self.parent[out[-1]] != -1:
            out.append(self.parent[out[-1]])
        return out


def quat_to_mat(q: np.ndarray) -> np.ndarray:
    """(..., 4) xyzw quaternions -> (..., 3, 3) rotation matrices."""
    q = np.asarray(q, dtype=np.float64)
    n = np.linalg.norm(q, axis=-1, keepdims=True)
    n = np.where(n == 0, 1.0, n)
    x, y, z, w = np.moveaxis(q / n, -1, 0)
    M = np.empty(q.shape[:-1] + (3, 3))
    M[..., 0, 0] = 1 - 2 * (y * y + z * z)
    M[..., 0, 1] = 2 * (x * y - z * w)
    M[..., 0, 2] = 2 * (x * z + y * w)
    M[..., 1, 0] = 2 * (x * y + z * w)
    M[..., 1, 1] = 1 - 2 * (x * x + z * z)
    M[..., 1, 2] = 2 * (y * z - x * w)
    M[..., 2, 0] = 2 * (x * z - y * w)
    M[..., 2, 1] = 2 * (y * z + x * w)
    M[..., 2, 2] = 1 - 2 * (x * x + y * y)
    return M


def mat_to_quat(M: np.ndarray) -> np.ndarray:
    """(..., 3, 3) rotation matrices -> (..., 4) xyzw quaternions."""
    M = np.asarray(M, dtype=np.float64)
    m00, m01, m02 = M[..., 0, 0], M[..., 0, 1], M[..., 0, 2]
    m10, m11, m12 = M[..., 1, 0], M[..., 1, 1], M[..., 1, 2]
    m20, m21, m22 = M[..., 2, 0], M[..., 2, 1], M[..., 2, 2]
    tr = m00 + m11 + m22
    q = np.zeros(M.shape[:-2] + (4,))

    c0 = tr > 0
    s = np.sqrt(np.maximum(tr + 1.0, 1e-20)) * 2
    q[..., 3] = np.where(c0, 0.25 * s, q[..., 3])
    q[..., 0] = np.where(c0, (m21 - m12) / s, q[..., 0])
    q[..., 1] = np.where(c0, (m02 - m20) / s, q[..., 1])
    q[..., 2] = np.where(c0, (m10 - m01) / s, q[..., 2])

    c1 = (~c0) & (m00 > m11) & (m00 > m22)
    s1 = np.sqrt(np.maximum(1.0 + m00 - m11 - m22, 1e-20)) * 2
    q[..., 3] = np.where(c1, (m21 - m12) / s1, q[..., 3])
    q[..., 0] = np.where(c1, 0.25 * s1, q[..., 0])
    q[..., 1] = np.where(c1, (m01 + m10) / s1, q[..., 1])
    q[..., 2] = np.where(c1, (m02 + m20) / s1, q[..., 2])

    c2 = (~c0) & (~c1) & (m11 > m22)
    s2 = np.sqrt(np.maximum(1.0 + m11 - m00 - m22, 1e-20)) * 2
    q[..., 3] = np.where(c2, (m02 - m20) / s2, q[..., 3])
    q[..., 0] = np.where(c2, (m01 + m10) / s2, q[..., 0])
    q[..., 1] = np.where(c2, 0.25 * s2, q[..., 1])
    q[..., 2] = np.where(c2, (m12 + m21) / s2, q[..., 2])

    c3 = (~c0) & (~c1) & (~c2)
    s3 = np.sqrt(np.maximum(1.0 + m22 - m00 - m11, 1e-20)) * 2
    q[..., 3] = np.where(c3, (m10 - m01) / s3, q[..., 3])
    q[..., 0] = np.where(c3, (m02 + m20) / s3, q[..., 0])
    q[..., 1] = np.where(c3, (m12 + m21) / s3, q[..., 1])
    q[..., 2] = np.where(c3, 0.25 * s3, q[..., 2])

    q /= np.linalg.norm(q, axis=-1, keepdims=True)
    return q


def decompose(M: np.ndarray):
    """4x4 row-major matrix -> (translation, quaternion xyzw, scale)."""
    t = M[:3, 3].copy()
    R = M[:3, :3].copy()
    s = np.linalg.norm(R, axis=0)
    s = np.where(s == 0, 1.0, s)
    if np.linalg.det(R) < 0:
        s[0] = -s[0]
    Rn = R / s
    return t, mat_to_quat(Rn), s


def compose(t: np.ndarray, q: np.ndarray, s: np.ndarray) -> np.ndarray:
    """(...,3),(...,4),(...,3) -> (...,4,4) row-major TRS matrices."""
    R = quat_to_mat(q)
    M = np.zeros(np.broadcast_shapes(t.shape[:-1], q.shape[:-1], s.shape[:-1]) + (4, 4))
    M[..., :3, :3] = R * s[..., None, :]
    M[..., :3, 3] = t
    M[..., 3, 3] = 1.0
    return M
