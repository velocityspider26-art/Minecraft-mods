"""Grip frames: where a hand meets a weapon, on both sides of the contact.

A hand and a grip are matched by building the same kind of frame on each and
laying one on the other:

    origin   the centre of the contact -- palm centre / grip centre
    U        along the grip's long axis, pointing at its TOP end.  On a pistol
             grip the top is the trigger end, and the hand's matching axis is
             the knuckle line, because index-knuckle-high / pinky-knuckle-low is
             how a hand actually sits on a vertical grip.
    N        out of the palm, and on the weapon side, out of the face the
             fingers wrap onto (the front strap, toward the muzzle).

Given hand frame Hg (in the wrist bone's local space) and weapon frame Wg (in
weapon-local space), the wrist must land at

    wristWorld = weaponWorld * Wg * Hg^-1
"""
import numpy as np


def frame(origin, u, n):
    """Right-handed CFrame-like 4x4 from an origin, a primary axis and a normal."""
    u = np.asarray(u, float)
    u = u / np.linalg.norm(u)
    n = np.asarray(n, float)
    n = n - u * np.dot(n, u)
    ln = np.linalg.norm(n)
    if ln < 1e-8:                       # degenerate hint; pick any perpendicular
        n = np.cross(u, [0.0, 0.0, 1.0])
        if np.linalg.norm(n) < 1e-8:
            n = np.cross(u, [1.0, 0.0, 0.0])
        ln = np.linalg.norm(n)
    n = n / ln
    v = np.cross(n, u)
    M = np.eye(4)
    # columns are the frame's X, Y, Z axes: X = U (grip axis), Y = V, Z = N
    M[:3, 0] = u
    M[:3, 1] = v
    M[:3, 2] = n
    M[:3, 3] = origin
    return M


def hand_frame(rig, side):
    """Palm frame in the wrist bone's local space."""
    W = rig.bind(side + 'Hand')
    inv = np.linalg.inv(W)

    def loc(bone):
        return (inv @ np.append(rig.bind(bone)[:3, 3], 1.0))[:3]

    index = loc(side + 'HandIndex1')
    pinky = loc(side + 'HandPinky1')
    middle = loc(side + 'HandMiddle1')
    thumb = loc(side + 'HandThumb1')

    u = index - pinky                       # knuckle line, pointing index-side
    down = middle                            # wrist -> knuckles
    # The thumb sits on the palm side, so it disambiguates which way the palm
    # faces without hard-coding a per-hand sign.
    n = np.cross(u, down)
    n = n / np.linalg.norm(n)
    if np.dot(n, thumb - down * np.dot(thumb, down) / max(np.dot(down, down), 1e-9)) < 0:
        n = -n
    origin = (middle * 0.52) + n * 0.035     # palm centre, just off the surface
    return frame(origin, u, n)


def weapon_grip_frame(points, forward, up, top_hint=None):
    """Frame for a grip, from the cloud of points that make it up.

    `forward` and `up` are the weapon's own axes. The grip's long axis is its
    dominant principal direction, oriented to point at the end nearest the
    receiver; the fingers wrap onto the face pointing `forward`.
    """
    pts = np.asarray(points, float)
    c = pts.mean(0)
    axis = np.linalg.svd(pts - c, full_matrices=False)[2][0]
    ref = up if top_hint is None else top_hint
    if np.dot(axis, ref) < 0:
        axis = -axis
    return frame(c, axis, forward)
