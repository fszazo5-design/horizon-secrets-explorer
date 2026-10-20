# Animated human character assets

The three playable, rigged human models in this directory are adapted from the Innerscene 3D Parts Library. The source pages release each model under CC0 and describe the body, rig, and skin as MakeHuman/MPFB CC0 assets. Each source GLB contains seven looping animation clips: idle, relaxed walk, brisk walk, jog, run, carry walk, and sit/stand.

- `harper.glb` — [Locomotion collection - Harper](https://www.innerscene.com/tools/library/3d-parts/locomotion-collection-harper-3d-person-animated-797e1e2a), business clothes; source page lists 18.6 MB.
- `sam.glb` — [Locomotion collection - Sam](https://www.innerscene.com/tools/library/3d-parts/locomotion-collection-sam-3d-person-animated-dc23800f), casual clothes; source page lists 20.9 MB.
- `maya.glb` — [Locomotion collection - Maya](https://www.innerscene.com/tools/library/3d-parts/locomotion-collection-maya-3d-person-animated-1499b2fd), casual clothes; source page lists 20.3 MB.

The app copies were optimized for Android: each rig is reduced from 56 to 21 joints by merging finger/helper weights into the hand, forearm, and foot bones; large textures are downscaled; non-character props are removed; and mesh detail is moderately reduced. The seven animation tracks are retained. `portrait_*.png` are small transparent interface previews rendered from the optimized models.

The reusable processing tools are in the repository's `tools/` directory. Run the Blender optimizer with Blender 4.x, then run the portrait trimming script with Python and Pillow. The license statements are reproduced from the linked source pages; consult those pages for the full terms and current availability.
