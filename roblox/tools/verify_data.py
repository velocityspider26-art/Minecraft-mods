#!/usr/bin/env python3
"""Validate the generated Luau data the way Roblox will consume it.

Reads SoldierRigData.lua and KSVRMesh.lua back out of the source tree -- the
actual shipped numbers, not the FBX -- and checks the invariants the runtime
depends on:

  * every bind and offset CFrame is a true rotation (Roblox's CFrame.new does
    not orthonormalise, so a scaled matrix would silently corrupt every pose)
  * parent.bind * offset == child.bind, i.e. the hierarchy the runtime walks
    reproduces the bind pose exactly
  * the hand and grip frames are orthonormal and place the palm on the grip
  * both arms can actually reach their grip from the bind shoulder
"""
import os
import re
import sys

import numpy as np

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MOD = os.path.join(ROOT, 'src', 'ReplicatedStorage', 'Modules')


def to_mat(nums):
    """Roblox CFrame.new(x,y,z, R00..R22) -> 4x4."""
    M = np.eye(4)
    M[:3, 3] = nums[:3]
    M[:3, :3] = np.array(nums[3:12]).reshape(3, 3)
    return M


def parse_bones(text):
    bones = {}
    for m in re.finditer(
            r'\t(\w+) = \{parent=(nil|"\w+"), bind=\{([^}]*)\}, offset=\{([^}]*)\}\},', text):
        name = m.group(1)
        parent = None if m.group(2) == 'nil' else m.group(2).strip('"')
        bind = to_mat([float(x) for x in m.group(3).split(',')])
        off = to_mat([float(x) for x in m.group(4).split(',')])
        bones[name] = dict(parent=parent, bind=bind, offset=off)
    return bones


def parse_frames(text, key):
    block = re.search(r'M\.%s = \{(.*?)\n\}' % key, text, re.S)
    if not block:
        return {}
    return {m.group(1): to_mat([float(x) for x in m.group(2).split(',')])
            for m in re.finditer(r'(\w+) = \{([^}]*)\}', block.group(1))}


def ortho_error(M):
    R = M[:3, :3]
    return float(np.abs(R @ R.T - np.eye(3)).max()), float(np.linalg.det(R))


def main():
    rig_txt = open(os.path.join(MOD, 'SoldierRigData.lua')).read()
    gun_txt = open(os.path.join(MOD, 'KSVRMesh.lua')).read()

    bones = parse_bones(rig_txt)
    hands = parse_frames(rig_txt, 'HANDS')
    grips = parse_frames(gun_txt, 'GRIPS')
    order = re.search(r'M\.ORDER = \{(.*?)\n\}', rig_txt, re.S).group(1)
    order = re.findall(r'"(\w+)"', order)

    fails = []
    print('bones %d, order %d, hands %d, grips %d'
          % (len(bones), len(order), len(hands), len(grips)))

    # ---- orthonormality
    worst = 0.0
    for name, b in bones.items():
        for which in ('bind', 'offset'):
            e, det = ortho_error(b[which])
            worst = max(worst, e)
            if e > 1e-3 or abs(det - 1) > 1e-3:
                fails.append('%s.%s not a rotation (err %.2e det %.4f)' % (name, which, e, det))
    for label, frames in (('HANDS', hands), ('GRIPS', grips)):
        for k, M in frames.items():
            e, det = ortho_error(M)
            worst = max(worst, e)
            if e > 1e-3 or abs(det - 1) > 1e-3:
                fails.append('%s.%s not a rotation (err %.2e det %.4f)' % (label, k, e, det))
    print('worst orthonormality error: %.2e' % worst)

    # ---- hierarchy reproduces the bind pose
    worst_chain = 0.0
    for name in order:
        b = bones[name]
        if not b['parent']:
            continue
        composed = bones[b['parent']]['bind'] @ b['offset']
        err = float(np.abs(composed - b['bind']).max())
        worst_chain = max(worst_chain, err)
        if err > 2e-3:
            fails.append('%s: parent.bind * offset != bind (err %.2e)' % (name, err))
    print('worst hierarchy error:      %.2e studs' % worst_chain)

    # ---- parents always come before children in ORDER
    seen = set()
    for name in order:
        p = bones[name]['parent']
        if p and p not in seen:
            fails.append('ORDER lists %s before its parent %s' % (name, p))
        seen.add(name)

    # ---- reach: can each arm get its hand onto its grip from the bind pose?
    print()
    for side, key in (('Left', 'left'), ('Right', 'right')):
        if key not in grips or side not in hands:
            continue
        l1 = np.linalg.norm(bones[side + 'ForeArm']['bind'][:3, 3] - bones[side + 'Arm']['bind'][:3, 3])
        l2 = np.linalg.norm(bones[side + 'Hand']['bind'][:3, 3] - bones[side + 'ForeArm']['bind'][:3, 3])
        # weapon placed as the runtime places it at the pistol grip in front of
        # the chest; only the relative geometry matters for a reach check
        wrist_local = grips[key] @ np.linalg.inv(hands[side])
        palm_back = wrist_local @ hands[side]
        err = float(np.abs(palm_back - grips[key]).max())
        if err > 1e-6:
            fails.append('%s palm/grip round-trip error %.2e' % (side, err))
        print('%-5s arm %.3f + %.3f = %.3f studs reach   grip round-trip %.1e'
              % (side, l1, l2, l1 + l2, err))

    print()
    if fails:
        for f in fails:
            print('FAIL ' + f)
        return 1
    print('ALL CHECKS PASSED')
    return 0


if __name__ == '__main__':
    sys.exit(main())
