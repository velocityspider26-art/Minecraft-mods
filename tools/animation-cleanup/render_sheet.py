"""Blender headless renderer: contact sheets of an animated GLB from 2 angles.

Usage:
  blender --background --python render_sheet.py -- <glb> <outdir> <tag> <f0,f1,...>
"""
import bpy
import os
import sys
import math
from mathutils import Vector

argv = sys.argv[sys.argv.index("--") + 1:]
glb, outdir, tag, frames_s = argv[0], argv[1], argv[2], argv[3]
# "track" follows the hips (pose detail); "fixed" holds a world-anchored camera
# so global sliding / root drift is directly visible.
mode = argv[4] if len(argv) > 4 else "track"
frames = [int(x) for x in frames_s.split(",")]
os.makedirs(outdir, exist_ok=True)

bpy.ops.wm.read_factory_settings(use_empty=True)
# glTF times are seconds; the importer converts them using the scene fps, so this
# must be 30 BEFORE import for Blender frame numbers to match our sample indices.
bpy.context.scene.render.fps = 30
bpy.context.scene.render.fps_base = 1.0
bpy.ops.import_scene.gltf(filepath=glb)

# The importer materialises a dummy 'Icosphere' for the orphaned second skin in
# this asset. It is not part of the source GLB - drop it so it cannot pollute
# the framing or the render.
for o in [o for o in bpy.data.objects if o.type == 'MESH' and o.name != 'Body']:
    print(f"[info] removing import artifact: {o.name} ({len(o.data.vertices)} verts)")
    bpy.data.objects.remove(o, do_unlink=True)

arm = next((o for o in bpy.data.objects if o.type == 'ARMATURE'), None)
mesh_objs = [o for o in bpy.data.objects if o.type == 'MESH']
print(f"[info] armature={arm.name if arm else None} meshes={[o.name for o in mesh_objs]}")
if arm:
    print(f"[info] bones={len(arm.data.bones)}")
    act = arm.animation_data.action if arm.animation_data else None
    print(f"[info] action={act.name if act else None} "
          f"range={act.frame_range[:] if act else None} fcurves={len(act.fcurves) if act else 0}")

scene = bpy.context.scene
scene.render.engine = 'BLENDER_WORKBENCH'
scene.display.shading.light = 'STUDIO'
scene.display.shading.color_type = 'SINGLE'
scene.display.shading.single_color = (0.62, 0.63, 0.66)
scene.display.shading.show_shadows = True
scene.display.shading.show_cavity = True
scene.display.shading.cavity_type = 'BOTH'
scene.render.resolution_x = 460
scene.render.resolution_y = 460
scene.render.resolution_percentage = 100
scene.render.image_settings.file_format = 'PNG'
scene.world = bpy.data.worlds.new("W")
scene.world.color = (0.09, 0.10, 0.12)

# ---- ground grid plane so foot contact / floating is visible
bpy.ops.mesh.primitive_plane_add(size=8, location=(0, 0, 0))
ground = bpy.context.object
ground.name = "GroundRef"

# ---- world bounds of the character across the sampled frames
lo = Vector((1e9, 1e9, 1e9))
hi = Vector((-1e9, -1e9, -1e9))
dg = bpy.context.evaluated_depsgraph_get()
for f in frames:
    scene.frame_set(f)
    dg.update()
    for o in mesh_objs:
        ev = o.evaluated_get(dg)
        for c in ev.bound_box:
            w = ev.matrix_world @ Vector(c)
            lo = Vector((min(lo[i], w[i]) for i in range(3)))
            hi = Vector((max(hi[i], w[i]) for i in range(3)))
ctr = (lo + hi) / 2
size = max((hi - lo).x, (hi - lo).y, (hi - lo).z)
print(f"[info] bounds lo={lo[:]} hi={hi[:]} center={ctr[:]} size={size:.3f}")

cam_data = bpy.data.cameras.new("Cam")
cam_data.lens = 50
cam = bpy.data.objects.new("Cam", cam_data)
scene.collection.objects.link(cam)
scene.camera = cam

# Frame the character tightly and track it per frame, so pose detail is legible
# instead of being lost in a bounding box sized by the worst frame.
dist = 3.15 if mode == "track" else 4.6
VIEWS = {
    # name: (azimuth deg, elevation deg)
    "front34": (38.0, 12.0),
    "side": (90.0, 6.0),
}

hips_bone = "Hips" if arm and "Hips" in arm.pose.bones else None
FIXED_AIM = Vector((0.0, -0.25, 0.45))


def char_center():
    """Aim point: hips world position lifted toward mid-torso."""
    if mode == "fixed":
        return FIXED_AIM
    if hips_bone:
        p = arm.matrix_world @ arm.pose.bones[hips_bone].head
        return Vector((p.x, p.y, max(p.z, 0.15) + 0.22))
    return ctr


def place(az_deg, el_deg, target):
    az, el = math.radians(az_deg), math.radians(el_deg)
    off = Vector((math.sin(az) * math.cos(el), -math.cos(az) * math.cos(el), math.sin(el))) * dist
    cam.location = target + off
    d = (target - cam.location).normalized()
    cam.rotation_euler = d.to_track_quat('-Z', 'Y').to_euler()


written = []
for vname, (az, el) in VIEWS.items():
    for f in frames:
        scene.frame_set(f)
        bpy.context.view_layer.update()
        place(az, el, char_center())
        p = os.path.join(outdir, f"{tag}_{vname}_{f:04d}.png")
        scene.render.filepath = p
        bpy.ops.render.render(write_still=True)
        written.append(p)
print(f"[info] wrote {len(written)} images")
