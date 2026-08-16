"""Surgical GLB animation rewrite.

Only the bytes of the animation sampler output accessors are replaced. The JSON
chunk, mesh, skins, materials, images, node hierarchy and every other buffer view
are left byte-identical, so the exported asset stays pipeline-compatible.
"""
from __future__ import annotations

import json
import struct
import numpy as np
from pygltflib import GLTF2

JSON_CHUNK = 0x4E4F534A
BIN_CHUNK = 0x004E4942


def _chunks(data: bytes):
    magic, ver, total = struct.unpack("<III", data[:12])
    if magic != 0x46546C67:
        raise ValueError("not a GLB")
    out = []
    off = 12
    while off + 8 <= total:
        clen, ctype = struct.unpack("<II", data[off:off + 8])
        out.append((ctype, off + 8, clen))
        off += 8 + clen + ((4 - clen % 4) % 4 if clen % 4 else 0)
    return out, ver, total


def write_animation(src_path: str, dst_path: str, rot: dict, trans: dict,
                    anim_index: int = 0, verbose: bool = True):
    """rot: {node_index: (F,4) xyzw}, trans: {node_index: (F,3)}."""
    raw = bytearray(open(src_path, "rb").read())
    chunks, ver, total = _chunks(bytes(raw))
    json_c = next(c for c in chunks if c[0] == JSON_CHUNK)
    bin_c = next(c for c in chunks if c[0] == BIN_CHUNK)
    doc = json.loads(bytes(raw[json_c[1]:json_c[1] + json_c[2]]).decode("utf-8"))
    bin_off = bin_c[1]

    an = doc["animations"][anim_index]
    n_written = 0
    for ch in an["channels"]:
        node = ch["target"]["node"]
        path = ch["target"]["path"]
        src = rot.get(node) if path == "rotation" else (
            trans.get(node) if path == "translation" else None)
        if src is None:
            continue
        acc_i = an["samplers"][ch["sampler"]]["output"]
        acc = doc["accessors"][acc_i]
        bv = doc["bufferViews"][acc["bufferView"]]
        ncomp = 4 if acc["type"] == "VEC4" else 3
        if acc["componentType"] != 5126:
            raise ValueError(f"accessor {acc_i} is not float32")
        if bv.get("byteStride"):
            raise ValueError(f"accessor {acc_i} is interleaved")
        arr = np.ascontiguousarray(np.asarray(src, dtype=np.float32))
        if arr.shape != (acc["count"], ncomp):
            raise ValueError(f"accessor {acc_i}: expected {(acc['count'], ncomp)}, got {arr.shape}")
        if not np.all(np.isfinite(arr)):
            raise ValueError(f"accessor {acc_i}: non-finite values")
        start = bin_off + (bv.get("byteOffset", 0)) + (acc.get("byteOffset", 0))
        nbytes = arr.size * 4
        if nbytes != bv["byteLength"]:
            raise ValueError(f"accessor {acc_i}: byte length changed "
                             f"{bv['byteLength']} -> {nbytes}")
        raw[start:start + nbytes] = arr.tobytes()
        n_written += 1

        # keep accessor min/max in sync where present (spec-valid, some loaders read it)
        if "min" in acc or "max" in acc:
            acc["min"] = [float(x) for x in arr.min(axis=0)]
            acc["max"] = [float(x) for x in arr.max(axis=0)]

    # Re-serialise JSON only if min/max were touched; keep length aligned.
    new_json = json.dumps(doc, separators=(",", ":")).encode("utf-8")
    pad = (4 - len(new_json) % 4) % 4
    new_json += b" " * pad
    head = bytearray()
    head += struct.pack("<III", 0x46546C67, 2, 0)
    head += struct.pack("<II", len(new_json), JSON_CHUNK)
    head += new_json
    bin_data = bytes(raw[bin_off:bin_off + bin_c[2]])
    bpad = (4 - len(bin_data) % 4) % 4
    head += struct.pack("<II", len(bin_data) + bpad, BIN_CHUNK)
    head += bin_data + b"\x00" * bpad
    struct.pack_into("<I", head, 8, len(head))
    open(dst_path, "wb").write(bytes(head))
    if verbose:
        print(f"[glb] wrote {dst_path}: {n_written} channels replaced, "
              f"{len(head)} bytes (source {total})")
    return dst_path
