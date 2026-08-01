#!/usr/bin/env python3
"""
Generates the empty structure templates the propulsion gametests run inside.

A gametest needs a structure NBT to place; ours only needs open air, because the
test builds its aircraft inside a Sable sublevel rather than out of template blocks.

Run:  python3 tools/generate_gametest_structures.py
"""

import gzip
import os
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "create_jet_engines"
DATA_VERSION = 3955  # Minecraft 1.21.1

# 1.21.1 loads structure templates from the resource directory "structure" (singular).
# "structures" is the *generated* directory used inside world saves, not a resource root.
# Verified against StructureTemplateManager.STRUCTURE_RESOURCE_DIRECTORY_NAME.
OUT_DIRS = [
    os.path.join(ROOT, "src/main/resources/data", MODID, "structure"),
]

TAG_END, TAG_INT, TAG_STRING, TAG_LIST, TAG_COMPOUND = 0, 3, 8, 9, 10


def u8(v):
    return struct.pack(">B", v)


def i32(v):
    return struct.pack(">i", v)


def mstr(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def named(tag_type, name, payload):
    return u8(tag_type) + mstr(name) + payload


def int_list(values):
    return u8(TAG_INT) + i32(len(values)) + b"".join(i32(v) for v in values)


def compound_list(items):
    """items: list of already-encoded compound payloads (without the trailing TAG_End)."""
    body = b"".join(item + u8(TAG_END) for item in items)
    return u8(TAG_COMPOUND) + i32(len(items)) + body


def empty_list():
    return u8(TAG_END) + i32(0)


def build(size=(16, 16, 16)):
    # palette: [{Name:"minecraft:air"}]
    air = named(TAG_STRING, "Name", mstr("minecraft:air"))
    palette = compound_list([air])

    # blocks: [{state:0, pos:[0,0,0]}]
    block = named(TAG_INT, "state", i32(0)) + named(TAG_LIST, "pos", int_list([0, 0, 0]))
    blocks = compound_list([block])

    body = b""
    body += named(TAG_INT, "DataVersion", i32(DATA_VERSION))
    body += named(TAG_LIST, "size", int_list(list(size)))
    body += named(TAG_LIST, "palette", palette)
    body += named(TAG_LIST, "blocks", blocks)
    body += named(TAG_LIST, "entities", empty_list())
    body += u8(TAG_END)

    return u8(TAG_COMPOUND) + mstr("") + body


# Vanilla prefixes a @GameTest template with the lowercased holder class name unless the
# template string already carries a namespace, so both spellings are emitted.
NAMES = ["empty_air", "jetpropulsiongametest.empty_air"]


def main():
    data = build()
    for d in OUT_DIRS:
        os.makedirs(d, exist_ok=True)
        for name in NAMES:
            path = os.path.join(d, name + ".nbt")
            with gzip.open(path, "wb") as fh:
                fh.write(data)
            print("wrote", os.path.relpath(path, ROOT), os.path.getsize(path), "bytes")


if __name__ == "__main__":
    main()
