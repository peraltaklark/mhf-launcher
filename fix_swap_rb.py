#!/usr/bin/env python3
"""
Replace 'Anime Edge' (effect id 20) with 'Swap R/B'.
No new effect IDs => no C++ pipeline whitelist issue.
Run from repo root.
"""
import os, re, shutil, sys, time

JAVA  = "app/src/main/java/com/winlator/cmod/ui/ReshadeSidebarPanelView.java"
VULKAN= "app/src/main/cpp/winlator/renderer/vulkan"

def ts(): return time.strftime("%Y%m%d-%H%M%S")
def read(p):
    with open(p,"r",encoding="utf-8") as f: return f.read()
def write(p,s):
    with open(p,"w",encoding="utf-8") as f: f.write(s)
def backup(p):
    b=f"{p}.bak-{ts()}"; shutil.copy2(p,b); print(f"  backup -> {b}")

# ---------- Java: rename label, remove id 21, keep id 20 ----------
def patch_java():
    print(f"[java] {JAVA}")
    src = read(JAVA)
    orig = src

    # 1) Swap the label
    src = src.replace('"Anime Edge"', '"Swap R/B"')

    # 2) Drop the trailing ", 21" we added before
    src = re.sub(r'(\bint\[\]\s+EFFECT_MODES\s*=\s*\{[^}]*?)\s*,?\s*21\s*(\})',
                 r'\1\2', src, flags=re.S)

    if src == orig:
        print("  nothing to change (already done?)")
        return
    backup(JAVA); write(JAVA, src)
    print("  relabeled Anime Edge -> Swap R/B, dropped id 21")

# ---------- Shaders ----------
def strip_id21(src):
    # remove the "else if (pc.effectId == 21) ..." line we added before
    src = re.sub(r'\n[ \t]*else\s+if\s*\(\s*pc\.effectId\s*==\s*21\s*\)[^\n]*', '', src)
    # remove any leftover applySwapRB definition we added
    src = re.sub(r'\nvec3\s+applySwapRB\s*\([^)]*\)\s*\{[^}]*\}\n', '\n', src)
    return src

def patch_postfx():
    p = f"{VULKAN}/window_postfx.frag"
    print(f"[shader] {p}")
    src = read(p)
    orig = src

    src = strip_id21(src)

    # Route id 20 through applySwapRB instead of applyAnimeEdge
    src = re.sub(
        r'^([ \t]*)else\s+if\s*\(\s*pc\.effectId\s*==\s*20\s*\)\s*'
        r'rgb\s*=\s*applyAnimeEdge\s*\(\s*uv\s*,\s*pc\.sharpness\s*\)\s*;',
        r'\1else if (pc.effectId == 20) rgb = applySwapRB(rgb);',
        src, flags=re.M)

    # Define helper before void main()
    if 'vec3 applySwapRB(vec3 rgb)' not in src:
        func = ("\nvec3 applySwapRB(vec3 rgb) {\n"
                "    return vec3(rgb.b, rgb.g, rgb.r);\n"
                "}\n")
        m = re.search(r'\nvoid\s+main\s*\(', src)
        if m:
            src = src[:m.start()] + "\n" + func + src[m.start():]

    if src == orig:
        print("  nothing to change")
        return
    backup(p); write(p, src)
    print("  routed id 20 to applySwapRB")

def patch_sgsr_generic(p):
    print(f"[shader] {p}")
    src = read(p)
    orig = src
    src = strip_id21(src)
    if src == orig:
        print("  nothing to change")
        return
    backup(p); write(p, src)
    print("  removed id-21 branch")

def main():
    if not os.path.isfile(JAVA):
        print("!! run from repo root"); sys.exit(1)
    patch_java()
    patch_postfx()
    for f in ("window_sgsr.frag", "window_sgsr_quality.frag"):
        p = f"{VULKAN}/{f}"
        if os.path.isfile(p): patch_sgsr_generic(p)

if __name__ == "__main__":
    main()
