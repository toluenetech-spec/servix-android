#!/usr/bin/env python3
"""Renders the Google Play store artwork from the real brand assets.

Inputs (committed, byte-identical to toluenetech-spec/servix at ref arena/01a0dc4c-servix):

  reference/web/public/brand/servix-logo-dark.svg   the wordmark for dark backgrounds
  app/src/main/res/drawable/ic_launcher_foreground.xml   the launcher monogram vector

Outputs, written into the same directory the Robolectric harness uses so the
existing dimension check and artifact upload pick them up:

  app/build/reports/screenshots/play/icon-512.png
  app/build/reports/screenshots/play/feature-graphic-1024x500.png

Two facts are asserted rather than assumed:

  * the launcher vector carries exactly the monogram path of the brand SVG
    (whitespace-normalised), so the icon is the brand mark and not a lookalike;
  * both PNGs are the exact Play size and fully opaque.

Everything is drawn at 4x and downsampled with Lanczos, which is why the curves
come out smooth without a device renderer. Run: python3 tools/play/render_store_art.py
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from reportlab.graphics import renderPM
from svglib.svglib import svg2rlg

DEEP_FOREST = "#12372A"
WARM_IVORY = "#F7F4EC"
CORAL = "#E56B5D"
TAGLINE = "Hire trusted Nigerian professionals."

ROOT = Path(__file__).resolve().parents[2]
BRAND_SVG = ROOT / "reference/web/public/brand/servix-logo-dark.svg"
LAUNCHER_VECTOR = ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml"
INTER_500 = ROOT / "app/src/main/res/font/inter_500.ttf"
OUT_DIR = ROOT / "app/build/reports/screenshots/play"

SUPERSAMPLE = 4


def die(message: str) -> None:
    print("::error::" + message)
    sys.exit(1)


def normalise_path(data: str) -> str:
    return re.sub(r"\s+", " ", data).strip()


def svg_paths(svg: str) -> list[str]:
    return re.findall(r'<path[^>]*\sd="([^"]+)"', svg)


def png_dims(path: Path) -> tuple[int, int]:
    with Image.open(path) as image:
        return image.size


def render_svg(svg_text: str, target_w: int, target_h: int, background: str) -> Image.Image:
    """Rasterise an SVG string at SUPERSAMPLE x the target size, then downsample."""
    tmp_svg = OUT_DIR / "_tmp.svg"
    tmp_png = OUT_DIR / "_tmp.png"
    tmp_svg.write_text(svg_text)

    drawing = svg2rlg(str(tmp_svg))
    if drawing is None:
        die("svglib could not parse the generated SVG")
    scale = (target_w * SUPERSAMPLE) / float(drawing.width)
    drawing.scale(scale, scale)
    drawing.width *= scale
    drawing.height *= scale
    renderPM.drawToFile(drawing, str(tmp_png), fmt="PNG", bg=int(background.lstrip("#"), 16))

    with Image.open(tmp_png) as rendered:
        image = rendered.convert("RGB")
    image = image.resize((target_w * SUPERSAMPLE, target_h * SUPERSAMPLE), Image.LANCZOS)
    image = image.resize((target_w, target_h), Image.LANCZOS)
    tmp_svg.unlink(missing_ok=True)
    tmp_png.unlink(missing_ok=True)
    return image


def build_icon() -> Image.Image:
    svg = BRAND_SVG.read_text()
    brand_paths = svg_paths(svg)
    if len(brand_paths) != 2:
        die(f"expected 2 paths in {BRAND_SVG.name}, found {len(brand_paths)}")

    vector = LAUNCHER_VECTOR.read_text()
    vector_path = re.search(r'android:pathData="([^"]+)"', vector)
    if vector_path is None:
        die("ic_launcher_foreground.xml has no android:pathData")
    if normalise_path(vector_path.group(1)) != normalise_path(brand_paths[0]):
        die(
            "the launcher icon has drifted from the brand monogram: "
            "ic_launcher_foreground.xml no longer matches the first path of "
            + BRAND_SVG.name
        )

    viewport_w = re.search(r'android:viewportWidth="([\d.]+)"', vector)
    viewport_h = re.search(r'android:viewportHeight="([\d.]+)"', vector)
    if viewport_w is None or viewport_h is None:
        die("ic_launcher_foreground.xml has no viewport")

    # The vector applies its placement with a <group> transform (scale then translate), so the
    # same transform has to be carried into the SVG or the monogram lands off-canvas.
    def group_attr(name: str, default: str) -> str:
        found = re.search(r'android:' + name + r'="(-?[\d.]+)"', vector)
        return found.group(1) if found else default

    scale_x = group_attr("scaleX", "1")
    scale_y = group_attr("scaleY", "1")
    translate_x = group_attr("translateX", "0")
    translate_y = group_attr("translateY", "0")

    icon_svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" '
        f'viewBox="0 0 {viewport_w.group(1)} {viewport_h.group(1)}">'
        f'<rect width="{viewport_w.group(1)}" height="{viewport_h.group(1)}" fill="{DEEP_FOREST}"/>'
        f'<g transform="translate({translate_x},{translate_y}) scale({scale_x},{scale_y})">'
        f'<path d="{vector_path.group(1)}" fill="{WARM_IVORY}"/>'
        "</g></svg>"
    )
    return render_svg(icon_svg, 512, 512, DEEP_FOREST)


def build_feature_graphic() -> Image.Image:
    width, height = 1024, 500
    canvas = Image.new("RGB", (width, height), DEEP_FOREST)

    svg = BRAND_SVG.read_text()
    lockup = render_svg(svg, 620, round(620 * 76.7 / 403.4), DEEP_FOREST)

    tagline_font = ImageFont.truetype(str(INTER_500), 42)
    probe = ImageDraw.Draw(canvas)
    tagline_width = probe.textlength(TAGLINE, font=tagline_font)

    padding = 72
    gap = 52
    rule_width = 88
    rule_height = 6
    rule_gap = 26
    if 620 > width - 2 * padding:
        die("the brand lockup is wider than the safe area")
    if tagline_width > width - 2 * padding:
        die("the tagline is wider than the safe area")

    ascent, descent = tagline_font.getmetrics()
    text_height = ascent + descent
    block_height = lockup.height + gap + text_height + rule_gap + rule_height
    top = (height - block_height) // 2
    mark_left = (width - lockup.width) // 2
    canvas.paste(lockup, (mark_left, top))

    baseline = top + lockup.height + gap + ascent
    probe.text(
        ((width - tagline_width) / 2, baseline),
        TAGLINE,
        font=tagline_font,
        fill="#E6E1D2",
        anchor="ls",
    )

    rule_top = top + lockup.height + gap + text_height + rule_gap
    probe.rectangle(
        [(width - rule_width) // 2, rule_top, (width + rule_width) // 2, rule_top + rule_height],
        fill=CORAL,
    )
    return canvas


def main() -> None:
    for required in (BRAND_SVG, LAUNCHER_VECTOR, INTER_500):
        if not required.is_file():
            die(f"missing input: {required}")

    OUT_DIR.mkdir(parents=True, exist_ok=True)

    icon = build_icon()
    icon_path = OUT_DIR / "icon-512.png"
    icon.save(icon_path, "PNG")
    if png_dims(icon_path) != (512, 512):
        die(f"{icon_path.name} is {png_dims(icon_path)}, expected 512x512")

    graphic = build_feature_graphic()
    graphic_path = OUT_DIR / "feature-graphic-1024x500.png"
    graphic.save(graphic_path, "PNG")
    if png_dims(graphic_path) != (1024, 500):
        die(f"{graphic_path.name} is {png_dims(graphic_path)}, expected 1024x500")

    for path in (icon_path, graphic_path):
        with Image.open(path) as image:
            alpha = image.convert("RGB").getpixel((1, 1))
            if alpha != (0x12, 0x37, 0x2A):
                die(f"{path.name} corner is {alpha}, expected Deep Forest (opaque)")

    print(f"BRAND_ART launcher monogram == brand SVG path 0")
    print(f"BRAND_ART icon-512.png {png_dims(icon_path)} opaque")
    print(f"BRAND_ART feature-graphic-1024x500.png {png_dims(graphic_path)} opaque")


if __name__ == "__main__":
    main()
