"""Mesh decimation + per-bone segmentation for the soldier body."""
import numpy as np


def weld(verts, tris, tol=1e-5):
    """Merge coincident vertices."""
    key = np.round(verts / tol).astype(np.int64)
    _, first, inv = np.unique(key, axis=0, return_index=True, return_inverse=True)
    nv = verts[first]
    nt = inv[tris]
    nt = nt[(nt[:, 0] != nt[:, 1]) & (nt[:, 1] != nt[:, 2]) & (nt[:, 0] != nt[:, 2])]
    return nv, nt


def cluster_decimate(verts, tris, cell):
    """Grid vertex-clustering decimation. Representative = cluster centroid,
    snapped to the cluster member nearest that centroid so silhouettes keep
    their real extremes instead of drifting inward."""
    if len(tris) == 0:
        return verts, tris
    g = np.floor(verts / cell).astype(np.int64)
    _, inv = np.unique(g, axis=0, return_inverse=True)
    n = inv.max() + 1
    sums = np.zeros((n, 3))
    cnt = np.zeros(n)
    np.add.at(sums, inv, verts)
    np.add.at(cnt, inv, 1)
    cent = sums / np.maximum(cnt, 1)[:, None]
    # pick the real vertex closest to each centroid
    d = np.einsum('ij,ij->i', verts - cent[inv], verts - cent[inv])
    best = np.full(n, np.inf)
    pick = np.zeros(n, dtype=np.int64)
    order = np.argsort(-d)          # so smallest distance wins last
    best[inv[order]] = d[order]
    pick[inv[order]] = order
    nv = verts[pick]
    nt = inv[tris]
    nt = nt[(nt[:, 0] != nt[:, 1]) & (nt[:, 1] != nt[:, 2]) & (nt[:, 0] != nt[:, 2])]
    # drop duplicate faces
    s = np.sort(nt, axis=1)
    _, keep = np.unique(s, axis=0, return_index=True)
    nt = nt[np.sort(keep)]
    used = np.unique(nt)
    remap = np.full(len(nv), -1, np.int64)
    remap[used] = np.arange(len(used))
    return nv[used], remap[nt]


def decimate_to(verts, tris, target_tris, lo=0.002, hi=0.5):
    """Binary-search the cluster cell size that hits a triangle budget."""
    if len(tris) <= target_tris:
        return verts, tris
    best = None
    for _ in range(22):
        mid = (lo + hi) * 0.5
        v, t = cluster_decimate(verts, tris, mid)
        if len(t) > target_tris:
            lo = mid
        else:
            best = (v, t)
            hi = mid
        if hi - lo < 1e-4:
            break
    return best if best else cluster_decimate(verts, tris, hi)


def face_normals(verts, tris):
    a, b, c = verts[tris[:, 0]], verts[tris[:, 1]], verts[tris[:, 2]]
    n = np.cross(b - a, c - a)
    ln = np.linalg.norm(n, axis=1, keepdims=True)
    return n / np.maximum(ln, 1e-9)


def vertex_normals(verts, tris):
    fn = face_normals(verts, tris)
    vn = np.zeros_like(verts)
    for k in range(3):
        np.add.at(vn, tris[:, k], fn)
    ln = np.linalg.norm(vn, axis=1, keepdims=True)
    return vn / np.maximum(ln, 1e-9)
