"""Bind-pose skeleton, skin weights and body segmentation, in Roblox space.

The FBX is a Mixamo-rigged character in centimetres, Y-up, character front
along +Z.  Roblox is Y-up with the character front along -Z, so the mapping is
a 180 degree turn about Y (mirror X and Z) plus a unit change.  Both spaces are
right-handed, so no winding flip is needed.
"""
import numpy as np

from .scene import Scene

CM_PER_STUD = 35.0                       # this project's scale: 1 stud = 0.35 m
FLIP = np.diag([-1.0, 1.0, -1.0, 1.0])

# Render segments. Each is one rigid piece driven by one bone; the extra bones
# listed in `absorb` have their geometry merged into that piece.
SEGMENTS = [
    ("Pelvis",        "Hips",          []),
    ("Abdomen",       "Spine",         []),
    ("Chest",         "Spine1",        []),
    ("Neck",          "Neck",          []),
    ("Head",          "Head",          ["HeadTop_End"]),
    ("LeftClavicle",  "LeftShoulder",  []),
    ("LeftUpperArm",  "LeftArm",       []),
    ("LeftForeArm",   "LeftForeArm",   []),
    ("LeftHand",      "LeftHand",      ["LeftHandThumb", "LeftHandIndex",
                                        "LeftHandMiddle", "LeftHandRing", "LeftHandPinky"]),
    ("RightClavicle", "RightShoulder", []),
    ("RightUpperArm", "RightArm",      []),
    ("RightForeArm",  "RightForeArm",  []),
    ("RightHand",     "RightHand",     ["RightHandThumb", "RightHandIndex",
                                        "RightHandMiddle", "RightHandRing", "RightHandPinky"]),
    ("LeftThigh",     "LeftUpLeg",     []),
    ("LeftShin",      "LeftLeg",       []),
    ("LeftFoot",      "LeftFoot",      ["LeftToeBase", "LeftToe_End"]),
    ("RightThigh",    "RightUpLeg",    []),
    ("RightShin",     "RightLeg",      []),
    ("RightFoot",     "RightFoot",     ["RightToeBase", "RightToe_End"]),
]

# Bones the runtime actually animates, in parent-before-child order.
DRIVEN = [
    "Hips", "Spine", "Spine1", "Neck", "Head",
    "LeftShoulder", "LeftArm", "LeftForeArm", "LeftHand",
    "RightShoulder", "RightArm", "RightForeArm", "RightHand",
    "LeftUpLeg", "LeftLeg", "LeftFoot", "LeftToeBase",
    "RightUpLeg", "RightLeg", "RightFoot", "RightToeBase",
]


def orthonormal(M):
    """Strip scale/shear from a transform, keeping its rotation and position.

    Mixamo bakes a uniform scale of 100 into every cluster TransformLink (the
    usual cm/m mixup). Left in place it does two damaging things: parent
    relative offsets come out 100x too small, because the inverse parent
    carries 1/100 into the translation, and the 3x3 handed to Roblox is not a
    rotation matrix at all, which CFrame.new does not correct. Polar
    decomposition gives the nearest true rotation and also cleans up the small
    non-uniformities a few of the bones carry.
    """
    U, _, Vt = np.linalg.svd(M[:3, :3])
    Q = U @ Vt
    if np.linalg.det(Q) < 0:            # never turn a rotation into a reflection
        U[:, -1] *= -1
        Q = U @ Vt
    out = np.eye(4)
    out[:3, :3] = Q
    out[:3, 3] = M[:3, 3]
    return out


class SoldierRig:
    def __init__(self, path, body_mesh_name="sol_8_low"):
        self.S = Scene(path)
        S = self.S
        self.body_gid = [g for g in S.geos if S.name[g] == body_mesh_name][0]
        self.body_mid = S.model_of_geo(self.body_gid)
        self._load_skin()
        self._load_bones()

    # ---------------------------------------------------------------- skin
    def _load_skin(self):
        S = self.S
        skin = [d for d in S.deformers.values() if d.props[2] == 'Skin'][0]
        self.clusters = {}
        for d in S.deformers.values():
            if d.props[2] != 'Cluster' or S.parent.get(d.props[0]) != skin.props[0]:
                continue
            bone = None
            for cid in S.children.get(d.props[0], []):
                if cid in S.models:
                    bone = S.name[cid]
            if bone is None:
                continue
            tl, tr = d.find('TransformLink'), d.find('Transform')
            idx, w = d.find('Indexes'), d.find('Weights')
            self.clusters[bone] = {
                'bind': np.array(tl.props[0], float).reshape(4, 4).T if tl else np.eye(4),
                'idx': np.array(idx.props[0], int) if idx else np.zeros(0, int),
                'w': np.array(w.props[0], float) if w else np.zeros(0),
            }

    # ------------------------------------------------------------- bones
    def _load_bones(self):
        S = self.S
        self.bones = {}
        for mid, m in S.models.items():
            nm = S.name[mid]
            if not nm.startswith('mixamorig:'):
                continue
            short = nm.split(':', 1)[1]
            p = S.name.get(S.parent.get(mid), '')
            parent = p.split(':', 1)[1] if p.startswith('mixamorig:') else None
            bind = self.clusters[nm]['bind'] if nm in self.clusters else S.glob(mid)
            # Normalise before anything is composed off it, or the 100x scale
            # multiplies straight into the reconstructed tips below.
            self.bones[short] = {'parent': parent, 'bind': orthonormal(bind)}
        # Unskinned tips keep a posed global; rebuild them off the parent bind.
        for short, b in self.bones.items():
            if ('mixamorig:' + short) in self.clusters:
                continue
            p = b['parent']
            if p and ('mixamorig:' + p) in self.clusters:
                b['bind'] = orthonormal(
                    self.bones[p]['bind'] @ S.local(S.byname['mixamorig:' + short]))

    # -------------------------------------------------------- conversion
    @staticmethod
    def to_roblox(M):
        R = FLIP @ M @ FLIP
        R[:3, 3] /= CM_PER_STUD
        return R

    def bind(self, name):
        return self.to_roblox(self.bones[name]['bind'])

    def pos(self, name):
        return self.bind(name)[:3, 3]

    def body_vertices(self):
        S = self.S
        M = S.glob(self.body_mid) @ S.geom_offset(self.body_mid)
        v = S.vertices(self.body_gid)
        h = np.hstack([v, np.ones((len(v), 1))])
        return ((h @ M.T) @ FLIP.T)[:, :3] / CM_PER_STUD

    def body_triangles(self):
        return np.array(self.S.triangles(self.body_gid), int)

    # ----------------------------------------------------------- weights
    def weight_matrix(self):
        """(n_vertices, n_bones) normalised skin weights + the bone name list."""
        n = len(self.S.vertices(self.body_gid))
        names = sorted(b.split(':', 1)[1] for b in self.clusters)
        col = {b: i for i, b in enumerate(names)}
        W = np.zeros((n, len(names)), np.float32)
        for full, c in self.clusters.items():
            j = col[full.split(':', 1)[1]]
            ok = (c['idx'] >= 0) & (c['idx'] < n)
            np.add.at(W, (c['idx'][ok], j), c['w'][ok])
        tot = W.sum(1, keepdims=True)
        W /= np.maximum(tot, 1e-6)
        return W, names

    def segment_triangles(self, tris, min_keep=0.15, overlap=0.22):
        """Which rigid segment(s) each triangle belongs to.

        Rigid chunks cannot represent a triangle that spans a joint. The worst
        offenders are the armpit/deltoid triangles: mostly chest-weighted, but
        with one vertex out on the upper arm. Assigned to the chest they stay
        in the bind pose while the arm swings away, and read as long spikes
        shooting out of the shoulders.

        So a triangle only joins a segment when EVERY one of its vertices has
        real weight there (min_keep). Triangles that straddle a joint are then
        emitted into both neighbours, so the seam is covered from both sides
        instead of tearing. Anything that belongs to neither is dropped -- the
        surrounding overlap closes the hole.

        Returns (mask, dominant) with mask[tri, seg] boolean.
        """
        segw = self.segment_weights()
        names = [s[0] for s in SEGMENTS]
        W = np.stack([segw[n] for n in names], 1)
        triW = W[tris]                       # (ntri, 3, nseg)
        mean = triW.mean(1)
        low = triW.min(1)
        dominant = mean.argmax(1)
        mask = (low >= min_keep) & ((mean > overlap) |
                                    (np.arange(len(names))[None, :] == dominant[:, None]))
        return mask, dominant, names

    def segment_weights(self):
        """Collapse per-bone weights onto render segments."""
        W, names = self.weight_matrix()
        col = {b: i for i, b in enumerate(names)}
        out = {}
        for seg, bone, absorb in SEGMENTS:
            acc = np.zeros(len(W), np.float32)
            if bone in col:
                acc += W[:, col[bone]]
            for a in absorb:
                for b in names:
                    if b.startswith(a):
                        acc += W[:, col[b]]
            out[seg] = acc
        return out
