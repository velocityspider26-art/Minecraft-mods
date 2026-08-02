#!/usr/bin/env python3
"""Structural sanity check for Luau sources.

Off-the-shelf Lua parsers reject Luau outright (`+=`, `--!nonstrict`, type
annotations, generalised iteration), so this does the check that actually
catches the mistakes worth catching in hand-written and generated Luau:
unbalanced brackets and unbalanced block keywords.

    python3 tools/check_luau.py src/**/*.lua
"""
import sys
import glob
import os

KEYWORDS_OPEN = ('function', 'if', 'do')
PAIRS = {')': '(', ']': '[', '}': '{'}
OPENERS = set('([{')


def strip(text):
    """Remove comments and string literals, keeping newlines for line numbers."""
    out = []
    i, n = 0, len(text)
    line_starts = [0]
    while i < n:
        c = text[i]
        # long bracket [[ ]] / [=[ ]=]  (as string or, after --, as comment)
        if c == '-' and text.startswith('--', i):
            j = i + 2
            if j < n and text[j] == '[':
                k = j + 1
                eq = 0
                while k < n and text[k] == '=':
                    eq += 1
                    k += 1
                if k < n and text[k] == '[':
                    close = ']' + '=' * eq + ']'
                    end = text.find(close, k)
                    end = n if end < 0 else end + len(close)
                    out.append('\n' * text.count('\n', i, end))
                    i = end
                    continue
            end = text.find('\n', i)
            end = n if end < 0 else end
            i = end
            continue
        if c == '[':
            k = i + 1
            eq = 0
            while k < n and text[k] == '=':
                eq += 1
                k += 1
            if k < n and text[k] == '[':
                close = ']' + '=' * eq + ']'
                end = text.find(close, k)
                end = n if end < 0 else end + len(close)
                out.append('\n' * text.count('\n', i, end))
                i = end
                continue
        if c in '"\'`':
            quote = c
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == quote:
                    j += 1
                    break
                if text[j] == '\n' and quote != '`':
                    break
                j += 1
            out.append('\n' * text.count('\n', i, j))
            i = j
            continue
        out.append(c)
        i += 1
    return ''.join(out)


def words(code):
    tok, cur = [], []
    for ch in code:
        if ch.isalnum() or ch == '_':
            cur.append(ch)
        else:
            if cur:
                tok.append(''.join(cur))
                cur = []
    if cur:
        tok.append(''.join(cur))
    return tok


def check(path):
    raw = open(path, encoding='utf-8').read()
    code = strip(raw)
    problems = []

    stack = []
    line = 1
    for ch in code:
        if ch == '\n':
            line += 1
        elif ch in OPENERS:
            stack.append((ch, line))
        elif ch in PAIRS:
            if not stack:
                problems.append('line %d: stray %r' % (line, ch))
                break
            got, at = stack.pop()
            if got != PAIRS[ch]:
                problems.append('line %d: %r closes %r opened at line %d'
                                % (line, ch, got, at))
                break
    if stack:
        ch, at = stack[-1]
        problems.append('unclosed %r opened at line %d' % (ch, at))

    w = words(code)
    # `for ... do` and `while ... do` both terminate in `do`, so counting
    # function/if/do against end covers every block form except repeat/until.
    opens = sum(w.count(k) for k in KEYWORDS_OPEN)
    ends = w.count('end')
    if opens != ends:
        problems.append('block mismatch: function+if+do = %d, end = %d (%+d)'
                        % (opens, ends, opens - ends))
    if w.count('repeat') != w.count('until'):
        problems.append('repeat/until mismatch: %d vs %d'
                        % (w.count('repeat'), w.count('until')))
    return problems


def main(argv):
    paths = []
    for a in argv:
        paths.extend(sorted(glob.glob(a, recursive=True)) or [a])
    bad = 0
    for p in paths:
        if not os.path.isfile(p):
            continue
        probs = check(p)
        if probs:
            bad += 1
            print('FAIL %s' % p)
            for x in probs:
                print('     %s' % x)
        else:
            print('ok   %s' % p)
    print('\n%d file(s) checked, %d with problems' % (len(paths), bad))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main(sys.argv[1:] or ['src/**/*.lua']))
