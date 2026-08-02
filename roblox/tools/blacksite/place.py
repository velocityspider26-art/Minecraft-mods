"""Unpack / repack the Luau sources of a .rbxlx place file.

Roblox place XML stores every script's text in a <string name="Source"> element.
Round-tripping through ElementTree would reformat the whole document, so this
edits the source strings in place with a targeted parse instead, leaving every
other byte of the file untouched.
"""
import os
import re
import xml.etree.ElementTree as ET

EXT = {'Script': '.server.lua', 'LocalScript': '.client.lua', 'ModuleScript': '.lua'}

_ESC = {'&': '&amp;', '<': '&lt;', '>': '&gt;', '\r': '&#13;'}


def escape(text):
    out = []
    for ch in text:
        out.append(_ESC.get(ch, ch))
    return ''.join(out)


def _props(item):
    p = item.find('Properties')
    if p is None:
        return None, None
    name = src = None
    for s in p.findall('string'):
        if s.get('name') == 'Name':
            name = s.text
        elif s.get('name') == 'Source':
            src = s.text
    return name, src


def scripts(path):
    """[(class, path-in-tree, source)] for every script in the place."""
    tree = ET.parse(path)
    found = []

    def walk(node, prefix):
        for item in node.findall('Item'):
            cls = item.get('class')
            name, src = _props(item)
            here = prefix + [name or cls]
            if src is not None and cls in EXT:
                found.append((cls, '/'.join(here), src))
            walk(item, here)

    walk(tree.getroot(), [])
    return found


def unpack(place, outdir):
    written = []
    for cls, path, src in scripts(place):
        dest = os.path.join(outdir, path + EXT[cls])
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        with open(dest, 'w') as fh:
            fh.write(src)
        written.append(dest)
    return written


# --------------------------------------------------------------------------
# Repacking.  We locate each <Item> that owns a Source property by scanning the
# raw text, then replace only the bytes between <string name="Source"> and its
# closing tag.
_SRC_RE = re.compile(
    r'(<string name="Source">)(.*?)(</string>)', re.S)
_NAME_RE = re.compile(r'<string name="Name">(.*?)</string>', re.S)


def _unescape(s):
    return (s.replace('&#13;', '\r').replace('&lt;', '<')
             .replace('&gt;', '>').replace('&quot;', '"')
             .replace('&apos;', "'").replace('&amp;', '&'))


def repack(place, sources, out=None):
    """`sources` maps script NAME -> new Luau text. Returns the names replaced."""
    raw = open(place, encoding='utf-8').read()
    replaced = []

    def sub(match):
        start = raw.rfind('<Item ', 0, match.start())
        window = raw[start:match.start()]
        nm = _NAME_RE.search(window)
        name = _unescape(nm.group(1)) if nm else None
        if name in sources:
            replaced.append(name)
            return match.group(1) + escape(sources[name]) + match.group(3)
        return match.group(0)

    new = _SRC_RE.sub(sub, raw)
    with open(out or place, 'w', encoding='utf-8') as fh:
        fh.write(new)
    return replaced


def add_modules(place, parent_name, modules, out=None, cls='ModuleScript'):
    """Insert new scripts as children of the instance named `parent_name`.

    `modules` is an ordered list of (name, source). Existing scripts with the
    same name are replaced by `repack` instead of being duplicated. `cls` is the
    Roblox class to create -- ModuleScript for ReplicatedStorage.Modules,
    LocalScript for anything under StarterPlayerScripts.
    """
    raw = open(place, encoding='utf-8').read()
    existing = {name for _, p, _ in scripts(place) for name in [p.rsplit('/', 1)[-1]]}
    fresh = [(n, s) for n, s in modules if n not in existing]
    if not fresh:
        return []

    # Find the folder Item and its closing tag, accounting for nesting.
    key = '<string name="Name">%s</string>' % parent_name
    at = raw.find(key)
    if at < 0:
        raise KeyError('no instance named %r in the place' % parent_name)
    start = raw.rfind('<Item ', 0, at)
    depth, i = 0, start
    while i < len(raw):
        nxt_open = raw.find('<Item ', i)
        nxt_close = raw.find('</Item>', i)
        if nxt_close < 0:
            raise ValueError('unbalanced Item tags')
        if 0 <= nxt_open < nxt_close:
            depth += 1
            i = nxt_open + 6
        else:
            depth -= 1
            i = nxt_close + 7
            if depth == 0:
                break
    insert_at = i - 7

    # Referents must be unique; keep clear of everything already in the file.
    used = {int(m) for m in re.findall(r'referent="(\d+)"', raw)}
    nxt = max(used) + 1 if used else 0

    chunks = []
    for n, (name, src) in enumerate(fresh):
        chunks.append(
            '\t\t\t<Item class="%s" referent="%d">\n'
            '\t\t\t\t<Properties>\n'
            '\t\t\t\t\t<string name="Name">%s</string>\n'
            '\t\t\t\t\t<string name="Source">%s</string>\n'
            '\t\t\t\t</Properties>\n'
            '\t\t\t</Item>\n' % (cls, nxt + n, escape(name), escape(src)))
    new = raw[:insert_at] + ''.join(chunks) + raw[insert_at:]
    with open(out or place, 'w', encoding='utf-8') as fh:
        fh.write(new)
    return [name for name, _ in fresh]
