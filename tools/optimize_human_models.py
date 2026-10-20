#!/usr/bin/env python3
"""Optimize Innerscene CC0 humanoid GLBs for the Android LibGDX build.

Run with Blender 4.x:
  blender -b --python tools/optimize_human_models.py -- input.glb output.glb preview.png

The optimizer removes non-character props, folds finger/helper weights into the
nearest supported bones, prunes the rig to 21 joints, downsizes textures, applies
light mesh decimation before skinning, preserves NLA animation clips, and renders
a transparent portrait preview.
"""
import os
import re
import sys
from collections import defaultdict
from mathutils import Vector
import bpy

CORE_BONES = {
    "Root", "pelvis", "spine_01", "spine_02", "spine_03", "neck_01", "head",
    "clavicle_l", "upperarm_l", "lowerarm_l", "hand_l",
    "clavicle_r", "upperarm_r", "lowerarm_r", "hand_r",
    "thigh_l", "calf_l", "foot_l", "thigh_r", "calf_r", "foot_r",
}


def args_after_separator():
    argv = sys.argv
    return argv[argv.index("--") + 1:] if "--" in argv else []


def map_to_core(name, armature):
    if name in CORE_BONES:
        return name
    match = re.match(r"(?:index|middle|pinky|ring|thumb)_\d+_([lr])$", name)
    if match:
        return "hand_" + match.group(1)
    if name.startswith("lowerarm_prox_"):
        return "lowerarm_" + name[-1]
    if name.startswith("ball_"):
        return "foot_" + name[-1]
    if name == "held_accessory":
        return "hand_r"
    bone = armature.data.bones.get(name)
    while bone is not None:
        if bone.name in CORE_BONES:
            return bone.name
        bone = bone.parent
    return None


def clean_scene(input_glb):
    bpy.ops.object.select_all(action="SELECT")
    bpy.ops.object.delete(use_global=False)
    bpy.ops.import_scene.gltf(filepath=input_glb)
    armature = next((o for o in bpy.context.scene.objects if o.type == "ARMATURE"), None)
    if armature is None:
        raise RuntimeError("Input GLB has no armature")

    for obj in list(bpy.context.scene.objects):
        if obj == armature:
            continue
        lower = obj.name.lower()
        helper = any(token in lower for token in ("briefcase", "phone", "cube", "icosphere", "sphere"))
        non_character = obj.type != "MESH" or (
            obj.find_armature() != armature and obj.parent != armature and obj.parent_type != "BONE"
        )
        if helper or non_character:
            bpy.data.objects.remove(obj, do_unlink=True)

    character_meshes = [o for o in bpy.context.scene.objects if o.type == "MESH"]
    if not character_meshes:
        raise RuntimeError("No character meshes remain after helper removal")

    # Merge fingers and helper-bone weights into the nearest mobile-safe body joint.
    for obj in character_meshes:
        old_groups = {g.index: g.name for g in obj.vertex_groups}
        per_vertex = []
        for vertex in obj.data.vertices:
            merged = defaultdict(float)
            for assignment in vertex.groups:
                old_name = old_groups.get(assignment.group)
                target = map_to_core(old_name, armature) if old_name else None
                if target:
                    merged[target] += assignment.weight
            total = sum(merged.values())
            if total > 0:
                merged = {name: weight / total for name, weight in merged.items()}
            per_vertex.append(merged)
        for group in list(obj.vertex_groups):
            obj.vertex_groups.remove(group)
        new_groups = {name: obj.vertex_groups.new(name=name) for name in sorted(CORE_BONES)}
        for index, weights in enumerate(per_vertex):
            for name, weight in weights.items():
                if weight > 0.0001:
                    new_groups[name].add([index], weight, "REPLACE")

    # Retain only weighted body joints; removed finger channels are not exported.
    bpy.context.view_layer.objects.active = armature
    armature.select_set(True)
    bpy.ops.object.mode_set(mode="EDIT")
    for bone in list(armature.data.edit_bones):
        if bone.name not in CORE_BONES:
            armature.data.edit_bones.remove(bone)
    bpy.ops.object.mode_set(mode="OBJECT")

    for action in bpy.data.actions:
        for curve in list(action.fcurves):
            match = re.search(r'pose\.bones\["([^"]+)"\]', curve.data_path)
            if match and match.group(1) not in CORE_BONES:
                action.fcurves.remove(curve)

    # Downscale large packed textures. Small alpha maps remain untouched.
    for image in list(bpy.data.images):
        if image.source != "FILE" or image.size[0] <= 512 or image.size[1] <= 512:
            continue
        max_dim = 1024 if any(key in image.name.lower() for key in ("body", "diffuse", "hair", "suit")) else 512
        scale = min(1.0, max_dim / max(image.size[0], image.size[1]))
        if scale < 1.0:
            width = max(1, int(round(image.size[0] * scale)))
            height = max(1, int(round(image.size[1] * scale)))
            image.scale(width, height)
            image.pack()

    # Decimate before the armature modifier so the bind pose and skin weights remain valid.
    for obj in character_meshes:
        tri_count = sum(max(0, len(poly.vertices) - 2) for poly in obj.data.polygons)
        if tri_count < 1200:
            continue
        lower = obj.name.lower()
        if "anatomical body" in lower and "low-poly" not in lower:
            ratio = 0.68
        elif any(key in lower for key in ("elegant", "fabric", "suit", "short")):
            ratio = 0.72
        else:
            ratio = 0.82
        modifier = obj.modifiers.new(name="MobileTriangleBudget", type="DECIMATE")
        modifier.ratio = ratio
        modifier.use_collapse_triangulate = True
        armature_modifier = next((m for m in obj.modifiers if m.type == "ARMATURE"), None)
        if armature_modifier is not None:
            while obj.modifiers.find(modifier.name) > obj.modifiers.find(armature_modifier.name):
                bpy.context.view_layer.objects.active = obj
                bpy.ops.object.modifier_move_up(modifier=modifier.name)
        bpy.context.view_layer.objects.active = obj
        bpy.ops.object.modifier_apply(modifier=modifier.name)

    return armature, character_meshes


def make_preview(armature, character_meshes, preview_path):
    scene = bpy.context.scene
    scene.render.engine = "BLENDER_EEVEE"
    scene.render.resolution_x = 320
    scene.render.resolution_y = 400
    scene.render.resolution_percentage = 100
    scene.render.film_transparent = True
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.render.image_settings.color_depth = "8"
    scene.render.image_settings.compression = 9
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "Medium High Contrast"

    # NLA tracks are separate game clips; mute all but Idle while rendering the card.
    if armature.animation_data:
        armature.animation_data.action = None
        idle_track = next((t for t in armature.animation_data.nla_tracks if "idle" in t.name.lower()), None)
        for track in armature.animation_data.nla_tracks:
            track.mute = track != idle_track
        if idle_track and idle_track.strips:
            strip = idle_track.strips[0]
            scene.frame_set(int((strip.frame_start + strip.frame_end) / 2))

    depsgraph = bpy.context.evaluated_depsgraph_get()
    points = []
    for obj in character_meshes:
        evaluated = obj.evaluated_get(depsgraph)
        points.extend(evaluated.matrix_world @ Vector(corner) for corner in evaluated.bound_box)
    if not points:
        raise RuntimeError("No visible model bounds for preview")
    minimum = Vector((min(p.x for p in points), min(p.y for p in points), min(p.z for p in points)))
    maximum = Vector((max(p.x for p in points), max(p.y for p in points), max(p.z for p in points)))
    center = (minimum + maximum) * 0.5

    camera_data = bpy.data.cameras.new("CharacterPreviewCamera")
    camera = bpy.data.objects.new("CharacterPreviewCamera", camera_data)
    scene.collection.objects.link(camera)
    camera.location = center + Vector((2.6, -4.4, 0.5))
    camera.rotation_euler = (center - camera.location).to_track_quat("-Z", "Y").to_euler()
    camera.data.type = "ORTHO"
    camera.data.ortho_scale = 2.05
    scene.camera = camera

    def area_light(name, location, energy, size):
        data = bpy.data.lights.new(name, type="AREA")
        data.energy = energy
        data.shape = "DISK"
        data.size = size
        obj = bpy.data.objects.new(name, data)
        scene.collection.objects.link(obj)
        obj.location = location
        obj.rotation_euler = (center - obj.location).to_track_quat("-Z", "Y").to_euler()

    area_light("PreviewKey", center + Vector((3.5, -3.5, 4.0)), 550, 4.0)
    area_light("PreviewFill", center + Vector((-3.0, -2.0, 1.5)), 300, 4.5)
    area_light("PreviewRim", center + Vector((0.5, 3.5, 3.0)), 420, 3.0)
    scene.world.color = (0.18, 0.18, 0.18)
    scene.render.filepath = preview_path
    bpy.ops.render.render(write_still=True)


def main():
    args = args_after_separator()
    if len(args) != 3:
        raise SystemExit("Expected input.glb output.glb preview.png after --")
    source, output, preview = map(os.path.abspath, args)
    os.makedirs(os.path.dirname(output), exist_ok=True)
    os.makedirs(os.path.dirname(preview), exist_ok=True)
    armature, character_meshes = clean_scene(source)

    bpy.ops.object.select_all(action="DESELECT")
    armature.select_set(True)
    for obj in character_meshes:
        obj.select_set(True)
    bpy.context.view_layer.objects.active = armature
    bpy.ops.export_scene.gltf(
        filepath=output,
        export_format="GLB",
        use_selection=True,
        export_animations=True,
        export_animation_mode="NLA_TRACKS",
        export_nla_strips=True,
        export_skins=True,
        export_cameras=False,
        export_lights=False,
        export_apply=True,
        export_image_format="AUTO",
    )
    make_preview(armature, character_meshes, preview)
    print("OPTIMIZED", output, "BONES", len(CORE_BONES), "MESHES", [o.name for o in character_meshes], "TRACKS", [t.name for t in armature.animation_data.nla_tracks])


if __name__ == "__main__":
    main()
