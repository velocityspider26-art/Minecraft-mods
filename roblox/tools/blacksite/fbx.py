"""Minimal binary-FBX reader (version 7000-7500). Pure stdlib."""
import struct, zlib

class Node:
    __slots__ = ("name", "props", "children")
    def __init__(self, name, props, children):
        self.name = name; self.props = props; self.children = children
    def find(self, name):
        for c in self.children:
            if c.name == name: return c
        return None
    def findall(self, name):
        return [c for c in self.children if c.name == name]
    def __repr__(self):
        return "<%s props=%d kids=%d>" % (self.name, len(self.props), len(self.children))


def _read_prop(b, o):
    t = chr(b[o]); o += 1
    if t == 'Y': return struct.unpack_from('<h', b, o)[0], o + 2
    if t == 'C': return bool(b[o]), o + 1
    if t == 'I': return struct.unpack_from('<i', b, o)[0], o + 4
    if t == 'F': return struct.unpack_from('<f', b, o)[0], o + 4
    if t == 'D': return struct.unpack_from('<d', b, o)[0], o + 8
    if t == 'L': return struct.unpack_from('<q', b, o)[0], o + 8
    if t in 'fdlib':
        n, enc, clen = struct.unpack_from('<III', b, o); o += 12
        raw = b[o:o + clen]; o += clen
        if enc == 1: raw = zlib.decompress(raw)
        fmt = {'f': 'f', 'd': 'd', 'l': 'q', 'i': 'i', 'b': 'b'}[t]
        return list(struct.unpack('<%d%s' % (n, fmt), raw)), o
    if t == 'S' or t == 'R':
        n = struct.unpack_from('<I', b, o)[0]; o += 4
        raw = b[o:o + n]; o += n
        if t == 'S':
            return raw.decode('utf-8', 'replace'), o
        return raw, o
    raise ValueError('unknown prop type %r at %d' % (t, o))


def _read_node(b, o, ver):
    if ver >= 7500:
        end, nprops, plen = struct.unpack_from('<QQQ', b, o); o += 24
        nlen = b[o]; o += 1
        sentinel = 25
    else:
        end, nprops, plen = struct.unpack_from('<III', b, o); o += 12
        nlen = b[o]; o += 1
        sentinel = 13
    if end == 0 and nprops == 0 and plen == 0 and nlen == 0:
        return None, o
    name = b[o:o + nlen].decode('utf-8', 'replace'); o += nlen
    props = []
    for _ in range(nprops):
        v, o = _read_prop(b, o)
        props.append(v)
    children = []
    while o < end - sentinel:
        c, o = _read_node(b, o, ver)
        if c is None: break
        children.append(c)
    return Node(name, props, children), end


def parse(path):
    b = open(path, 'rb').read()
    ver = struct.unpack_from('<I', b, 23)[0]
    o = 27
    roots = []
    while o < len(b) - 20:
        n, o = _read_node(b, o, ver)
        if n is None: break
        roots.append(n)
    return Node('ROOT', [], roots), ver
