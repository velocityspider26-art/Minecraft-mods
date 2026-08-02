"""Scene graph over the raw FBX node tree. Column-vector convention, numpy."""
import math
import numpy as np
from . import fbx as fbxmod



def props70(node):
    out = {}
    p = node.find('Properties70')
    if p is None:
        return out
    for c in p.children:
        if c.name == 'P':
            out[c.props[0]] = c.props[4:]
    return out


def T(t):
    m = np.eye(4); m[:3, 3] = t; return m


def S_(s):
    m = np.eye(4); m[0, 0], m[1, 1], m[2, 2] = s; return m


def rot_xyz(r):
    """FBX eOrderXYZ: R = Rx * Ry * Rz applied so that Rz happens first."""
    x, y, z = (math.radians(a) for a in r)
    cx, sx = math.cos(x), math.sin(x)
    cy, sy = math.cos(y), math.sin(y)
    cz, sz = math.cos(z), math.sin(z)
    Rx = np.array([[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1]], float)
    Ry = np.array([[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1]], float)
    Rz = np.array([[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], float)
    return Rx @ Ry @ Rz


class Scene:
    def __init__(self, path):
        self.root, self.ver = fbxmod.parse(path)
        objs = self.root.find('Objects')
        self.objs = objs
        self.models, self.geos, self.deformers = {}, {}, {}
        self.attrs, self.acurves, self.anodes = {}, {}, {}
        self.stacks, self.layers, self.materials = {}, {}, {}
        for m in objs.findall('Model'):
            self.models[m.props[0]] = m
        for g in objs.findall('Geometry'):
            self.geos[g.props[0]] = g
        for d in objs.findall('Deformer'):
            self.deformers[d.props[0]] = d
        for a in objs.findall('NodeAttribute'):
            self.attrs[a.props[0]] = a
        for a in objs.findall('AnimationCurve'):
            self.acurves[a.props[0]] = a
        for a in objs.findall('AnimationCurveNode'):
            self.anodes[a.props[0]] = a
        for a in objs.findall('AnimationStack'):
            self.stacks[a.props[0]] = a
        for a in objs.findall('AnimationLayer'):
            self.layers[a.props[0]] = a
        for a in objs.findall('Material'):
            self.materials[a.props[0]] = a

        # A bone Model has two OO edges: one to its parent Model and one to the
        # skin Cluster that references it. Keep every edge, then resolve the
        # skeletal parent as the one that is itself a Model.
        self.parents, self.children = {}, {}
        self.oprop = []
        for c in self.root.find('Connections').children:
            if c.props[0] == 'OO':
                cid, pid = c.props[1], c.props[2]
                self.parents.setdefault(cid, []).append(pid)
                self.children.setdefault(pid, []).append(cid)
            elif c.props[0] == 'OP':
                self.oprop.append((c.props[1], c.props[2], c.props[3]))
        self.parent = {}
        for cid, pids in self.parents.items():
            pick = pids[0]
            for p in pids:
                if p in self.models:
                    pick = p
                    break
            self.parent[cid] = pick

        self.name = {}
        for d in (self.models, self.geos, self.deformers, self.attrs,
                  self.anodes, self.stacks, self.layers, self.materials):
            for i, n in d.items():
                self.name[i] = n.props[1].split('\x00')[0]
        self.byname = {}
        for i in self.models:
            self.byname[self.name[i]] = i
        self._lcache, self._gcache = {}, {}

    # ---- transforms -------------------------------------------------
    def local(self, mid):
        if mid in self._lcache:
            return self._lcache[mid]
        P = props70(self.models[mid])

        def v(k, d=(0.0, 0.0, 0.0)):
            if k in P and len(P[k]) >= 3:
                return tuple(float(x) for x in P[k][:3])
            return d
        t, r, s = v('Lcl Translation'), v('Lcl Rotation'), v('Lcl Scaling', (1, 1, 1))
        pre, post = v('PreRotation'), v('PostRotation')
        roff, rp = v('RotationOffset'), v('RotationPivot')
        soff, sp = v('ScalingOffset'), v('ScalingPivot')
        M = (T(t) @ T(roff) @ T(rp) @ rot_xyz(pre) @ rot_xyz(r) @
             np.linalg.inv(rot_xyz(post)) @ T(np.negative(rp)) @
             T(soff) @ T(sp) @ S_(s) @ T(np.negative(sp)))
        self._lcache[mid] = M
        return M

    def glob(self, mid):
        if mid in self._gcache:
            return self._gcache[mid]
        p = self.parent.get(mid, 0)
        M = self.local(mid)
        if p in self.models:
            M = self.glob(p) @ M
        self._gcache[mid] = M
        return M

    def geom_offset(self, mid):
        """GeometricTranslation/Rotation/Scaling -- applies to the mesh only."""
        P = props70(self.models[mid])

        def v(k, d=(0.0, 0.0, 0.0)):
            if k in P and len(P[k]) >= 3:
                return tuple(float(x) for x in P[k][:3])
            return d
        return (T(v('GeometricTranslation')) @ rot_xyz(v('GeometricRotation')) @
                S_(v('GeometricScaling', (1, 1, 1))))

    def gname(self, i):
        return self.name.get(i, str(i))

    # ---- geometry ---------------------------------------------------
    def vertices(self, gid):
        g = self.geos[gid]
        v = g.find('Vertices').props[0]
        return np.array(v, float).reshape(-1, 3)

    def polygons(self, gid):
        """Returns list of vertex-index tuples (n-gons kept intact)."""
        pi = self.geos[gid].find('PolygonVertexIndex').props[0]
        polys, cur = [], []
        for i in pi:
            if i < 0:
                cur.append(-i - 1)
                polys.append(tuple(cur))
                cur = []
            else:
                cur.append(i)
        return polys

    def triangles(self, gid):
        tris = []
        for p in self.polygons(gid):
            for k in range(1, len(p) - 1):
                tris.append((p[0], p[k], p[k + 1]))
        return tris

    def model_of_geo(self, gid):
        return self.parent.get(gid)

    def world_verts(self, gid):
        mid = self.model_of_geo(gid)
        M = self.glob(mid) @ self.geom_offset(mid)
        v = self.vertices(gid)
        h = np.hstack([v, np.ones((len(v), 1))])
        return (h @ M.T)[:, :3]
