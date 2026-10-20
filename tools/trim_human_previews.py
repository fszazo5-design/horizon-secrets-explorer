#!/usr/bin/env python3
"""Trim transparent Blender previews and create compact Android HUD portraits."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1] / "android-native/app/src/main/assets/characters"
for name in ("harper", "sam", "maya"):
    source = ROOT / f"preview_{name}.png"
    output = ROOT / f"portrait_{name}.png"
    if not source.exists():
        continue
    image = Image.open(source).convert("RGBA")
    bbox = image.getchannel("A").getbbox()
    if not bbox:
        raise RuntimeError(f"No visible portrait pixels in {source}")
    left, top, right, bottom = bbox
    pad = 6
    box = (max(0, left - pad), max(0, top - pad), min(image.width, right + pad), min(image.height, bottom + pad))
    image = image.crop(box)
    image.thumbnail((128, 256), Image.Resampling.LANCZOS)
    image.save(output, format="PNG", optimize=True)
    print(f"{output.name}: {image.width}x{image.height}, {output.stat().st_size} bytes")
