"""Stage 1: structural inspection of the source GLB."""
import sys
import numpy as np
from pygltflib import GLTF2
from gltf_io import read_accessor, Skeleton

path = sys.argv[1]
g = GLTF2().load(path)

print("=" * 78)
print("ASSET / STRUCTURE")
print("=" * 78)
a = g.asset
print(f"generator      : {getattr(a,'generator',None)}")
print(f"version        : {getattr(a,'version',None)}")
print(f"copyright      : {getattr(a,'copyright',None)}")
print(f"extensionsUsed : {g.extensionsUsed}")
print(f"extensionsReq  : {g.extensionsRequired}")
print(f"scenes={len(g.scenes or [])} scene={g.scene} nodes={len(g.nodes or [])} "
      f"meshes={len(g.meshes or [])} skins={len(g.skins or [])} "
      f"materials={len(g.materials or [])} animations={len(g.animations or [])}")
print(f"accessors={len(g.accessors or [])} bufferViews={len(g.bufferViews or [])} "
      f"buffers={len(g.buffers or [])} images={len(g.images or [])}")

skel = Skeleton(g)
print(f"\nroots: {[(i, skel.names[i]) for i in skel.roots]}")

print("\n" + "=" * 78)
print("MESHES / SKINS")
print("=" * 78)
for mi, m in enumerate(g.meshes or []):
    tot = 0
    attrs = set()
    for p in m.primitives:
        at = p.attributes
        for k in ("POSITION", "NORMAL", "TANGENT", "TEXCOORD_0", "JOINTS_0",
                  "WEIGHTS_0", "JOINTS_1", "WEIGHTS_1", "COLOR_0"):
            if getattr(at, k, None) is not None:
                attrs.add(k)
        if getattr(at, "POSITION", None) is not None:
            tot += g.accessors[at.POSITION].count
        tgt = getattr(p, "targets", None)
        if tgt:
            attrs.add(f"MORPH x{len(tgt)}")
    print(f"mesh[{mi}] '{m.name}' prims={len(m.primitives)} verts={tot} attrs={sorted(attrs)}")

for si, sk in enumerate(g.skins or []):
    print(f"skin[{si}] '{sk.name}' joints={len(sk.joints or [])} "
          f"skeleton={sk.skeleton} IBM={sk.inverseBindMatrices}")

# Which nodes carry the mesh
for i, nd in enumerate(g.nodes or []):
    if getattr(nd, "mesh", None) is not None:
        print(f"  node[{i}] '{skel.names[i]}' -> mesh {nd.mesh} skin {getattr(nd,'skin',None)}")

print("\n" + "=" * 78)
print("NODE HIERARCHY")
print("=" * 78)


def dump(i, ind=0, maxd=99):
    nd = g.nodes[i]
    tags = []
    if getattr(nd, "mesh", None) is not None:
        tags.append(f"MESH{nd.mesh}")
    if i in skel.joint_set:
        tags.append("JOINT")
    if skel.has_matrix[i]:
        tags.append("MATRIX")
    t = skel.rest_t[i]
    print(f"{'  '*ind}[{i:3d}] {skel.names[i]:<34s} "
          f"t=({t[0]:+.4f},{t[1]:+.4f},{t[2]:+.4f}) {' '.join(tags)}")
    if ind < maxd:
        for c in skel.children[i]:
            dump(c, ind + 1, maxd)


for r in skel.roots:
    dump(r)

print("\n" + "=" * 78)
print("ANIMATIONS")
print("=" * 78)
for ai, an in enumerate(g.animations or []):
    print(f"\nanimation[{ai}] name={an.name!r} channels={len(an.channels)} samplers={len(an.samplers)}")
    tmin, tmax = None, None
    interps = {}
    paths = {}
    per_node = {}
    sample_counts = []
    for ch in an.channels:
        s = an.samplers[ch.sampler]
        tin = read_accessor(g, s.input)
        tmin = tin[0] if tmin is None else min(tmin, tin[0])
        tmax = tin[-1] if tmax is None else max(tmax, tin[-1])
        interps[s.interpolation] = interps.get(s.interpolation, 0) + 1
        p = ch.target.path
        paths[p] = paths.get(p, 0) + 1
        per_node.setdefault(ch.target.node, set()).add(p)
        sample_counts.append(len(tin))
    print(f"  time range   : {tmin:.6f} .. {tmax:.6f}  (duration {tmax-tmin:.6f}s)")
    print(f"  interpolation: {interps}")
    print(f"  paths        : {paths}")
    print(f"  animated nodes: {len(per_node)}")
    sc = np.array(sample_counts)
    print(f"  samples/chan : min={sc.min()} max={sc.max()} median={int(np.median(sc))}")

    # Sample-rate analysis on the densest channel.
    ch = max(an.channels, key=lambda c: g.accessors[an.samplers[c.sampler].input].count)
    tin = read_accessor(g, an.samplers[ch.sampler].input)
    d = np.diff(tin)
    print(f"  densest chan : node {ch.target.node} '{skel.names[ch.target.node]}' "
          f"path={ch.target.path} n={len(tin)}")
    print(f"  dt           : min={d.min():.6f} max={d.max():.6f} mean={d.mean():.6f} "
          f"std={d.std():.2e}  -> {1.0/d.mean():.3f} fps")
    print(f"  uniform dt   : {bool(np.allclose(d, d[0], atol=1e-6))}")

    # Which paths each animated node has, grouped.
    combos = {}
    for n, ps in per_node.items():
        combos.setdefault(tuple(sorted(ps)), []).append(n)
    print("  channel-set breakdown:")
    for k, v in sorted(combos.items(), key=lambda x: -len(x[1])):
        print(f"    {list(k)} : {len(v)} nodes")
        if len(v) <= 6:
            print(f"        {[skel.names[x] for x in v]}")

    # Nodes that are joints but NOT animated
    anim_nodes = set(per_node)
    missing = [j for j in skel.joint_set if j not in anim_nodes]
    print(f"  joints w/o animation: {len(missing)} {[skel.names[m] for m in missing][:12]}")
    extra = [n for n in anim_nodes if n not in skel.joint_set]
    print(f"  animated non-joints : {len(extra)} {[skel.names[m] for m in extra][:12]}")
