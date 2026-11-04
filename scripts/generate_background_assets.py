#!/usr/bin/env python3
"""
generate_background_assets.py
Generates illustrated quiet-dark background assets for Perazim Mission Church:
  1. src/main/assets/backgrounds/bg_home_header.png
  2. src/main/assets/backgrounds/bg_splash.png
  3. src/main/assets/backgrounds/bg_prayer_header.png

Constraints:
  - Width: 720px (maxDim 720)
  - Color palette: quiet-dark contemplative (#2A0F33 base)
  - Budget: <= 300 KB each, total <= 1.5 MB
"""

import os
from PIL import Image, ImageEnhance, ImageOps

ASSETS_DIR = "/home/murugu/Android/app/src/main/assets"
BG_DIR = os.path.join(ASSETS_DIR, "backgrounds")
os.makedirs(BG_DIR, exist_ok=True)

def tint_image(img, tint_hex="#2A0F33", blend=0.45):
    """Blends an image with a dark tint color for quiet contemplative mood."""
    tint_rgb = tuple(int(tint_hex.lstrip('#')[i:i+2], 16) for i in (0, 2, 4))
    solid = Image.new("RGB", img.size, tint_rgb)
    return Image.blend(img, solid, blend)

def make_bg(source_path, target_name, width=720, height=400, tint_hex="#2A0F33", blend=0.45, brightness=0.75):
    src = Image.open(source_path).convert("RGB")
    # Resize keeping aspect ratio or center crop
    src = ImageOps.fit(src, (width, height), Image.Resampling.LANCZOS)
    
    # Adjust brightness and contrast
    enhancer = ImageEnhance.Brightness(src)
    src = enhancer.enhance(brightness)
    
    # Tint with quiet-dark palette
    tinted = tint_image(src, tint_hex, blend)
    
    out_path = os.path.join(BG_DIR, target_name)
    tinted.save(out_path, format="PNG", optimize=True)
    
    size_kb = os.path.getsize(out_path) / 1024
    print(f"Generated {target_name}: {tinted.size}, {size_kb:.1f} KB")

# 1. Worship Dusk (16:9 ratio, 720x405, dusk contemplative mood)
worship_src = os.path.join(ASSETS_DIR, "art_worship_classic.jpg")
if not os.path.exists(worship_src):
    worship_src = os.path.join(ASSETS_DIR, "hymnals.jpg")
make_bg(worship_src, "bg_worship_dusk.png", width=720, height=405, tint_hex="#250C2C", blend=0.45, brightness=0.75)

# 2. Prayer Moon (720x360, quiet moonlight prayer altar)
prayer_src = os.path.join(ASSETS_DIR, "candle.jpg")
if not os.path.exists(prayer_src):
    prayer_src = os.path.join(ASSETS_DIR, "fellowship.jpg")
make_bg(prayer_src, "bg_prayer_moon.png", width=720, height=360, tint_hex="#2A0F33", blend=0.45, brightness=0.75)

# 3. Me Path (720x320, member journey & covenant path)
me_src = os.path.join(ASSETS_DIR, "campus.jpg")
if not os.path.exists(me_src):
    me_src = os.path.join(ASSETS_DIR, "bookshelf.jpg")
make_bg(me_src, "bg_me_path.png", width=720, height=320, tint_hex="#2D1036", blend=0.48, brightness=0.75)

# 4. Give Harvest (720x200, gratitude & faithful kingdom giving)
give_src = os.path.join(ASSETS_DIR, "art_christian.jpg")
if not os.path.exists(give_src):
    give_src = os.path.join(ASSETS_DIR, "waters.jpg")
make_bg(give_src, "bg_give_harvest.png", width=720, height=200, tint_hex="#381A22", blend=0.45, brightness=0.78)

# 5. Splash Breakthrough (720x360, supernatural breakthrough sanctuary banner)
breakthrough_src = os.path.join(ASSETS_DIR, "hero_sanctuary.jpg")
if not os.path.exists(breakthrough_src):
    breakthrough_src = os.path.join(ASSETS_DIR, "sanctuary.jpg")
make_bg(breakthrough_src, "bg_splash_breakthrough.png", width=720, height=360, tint_hex="#220A2B", blend=0.42, brightness=0.80)

# Preserve existing bg_splash.png for full-screen entry splash
waters_src = os.path.join(ASSETS_DIR, "waters.jpg")
if os.path.exists(waters_src) and not os.path.exists(os.path.join(BG_DIR, "bg_splash.png")):
    make_bg(waters_src, "bg_splash.png", width=720, height=1280, tint_hex="#1F0826", blend=0.55, brightness=0.65)

print("\nAsset size report:")
total = 0
for f in sorted(os.listdir(BG_DIR)):
    p = os.path.join(BG_DIR, f)
    s = os.path.getsize(p) / 1024
    total += s
    print(f"  {f}: {s:.1f} KB")
print(f"Total: {total:.1f} KB (budget: 1500 KB, each <= 300 KB)")
