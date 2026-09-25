#!/usr/bin/env python3
"""
Add 'Swap R/B' ReShade effect (id 21) - v4.
Patches:
  - ReshadeSidebarPanelView.java
  - window_postfx.frag          (anchor: effectId == 20 / applyAnimeEdge)
  - window_sgsr.frag            (anchor: effectId == 4  / applyNatural)
  - window_sgsr_quality.frag    (anchor: effectId == 4  / applyNatural)
Uses ONE signature:  vec3 applySwapRB(vec3 rgb)
Run from repo root:  python3 add_swap_rb.py
Optional:            python3 add_swap_rb.py --build
"""
import os, re, shutil, subprocess, sys, time

JAVA     = "app/src/main/java/com/winlator/cmod/ui/ReshadeSidebarPanelView.java"
VULKAN   = "app/src/main/cpp/winlator/renderer/vulkan"
NEW_LABEL, NEW_ID, FUNC_NAME = "Swap R/B", 21, "applySwapRB"

POSTFX = f"{VULKAN}/window_postfx.frag"
SGSRS  = [f"{VULKAN}/window_sgsr.frag", f"{VULKAN}/window_sgsr_quality.frag"]

def ts(): return time.strftime("%Y%m%d-%H%M%S")
def read(p):
    with open(p,"r",encoding="utf-8") as f: return f.read()
def write(p,s):
    with open(p,"w",encoding="utf-8") as f: f.write(s)
def backup(p):
    b=f"{p}.bak-{ts()}"; shutil.copy2(p,b); print(f"  backup -> {b}")

FUNC_DEF = (
    f"\nvec3 {FUNC_NAME}(vec3 rgb) {{\n"
    f"    return vec3(rgb.b, rgb.g, rgb.r);\n"
    f"}}\n"
)

# ---------------- Java ----------------
def patch_java():
    print(f"[java] {JAVA}")
    if not os.path.isfile(JAVA): print("  !! missing"); return
    src = read(JAVA)
    if f'"{NEW_LABEL}"' in src:
        print("  already patched, skipping"); return

    m = re.search(r'(private\s+static\s+final\s+String\[\]\s+EFFECTS\s*=\s*\{)(.*?)(\};)', src, re.S)
    if not m: print("  !! EFFECTS not found"); return
    body = m.group(2).rstrip()
    if not body.endswith(","): body += ","
    body += f'\n            "{NEW_LABEL}"\n    '
    src = src[:m.start(2)] + body + src[m.end(2):]

    m = re.search(r'(private\s+static\s+final\s+int\[\]\s+EFFECT_MODES\s*=\s*\{)(.*?)(\};)', src, re.S)
    if not m: print("  !! EFFECT_MODES not found"); return
    body = m.group(2).rstrip()
    if not body.endswith(","): body += ","
    body += f" {NEW_ID} "
    src = src[:m.start(2)] + body + src[m.end(2):]

    backup(JAVA); write(JAVA, src)
    print("  patched EFFECTS + EFFECT_MODES")

# ---------------- generic shader helpers ----------------
def already_patched(src):
    return bool(re.search(rf'pc\.effectId\s*==\s*{NEW_ID}\b', src))

def insert_call_after(src, anchor_pattern):
    """Insert 'else if (pc.effectId == 21) rgb = applySwapRB(rgb);'
       after the line matched by anchor_pattern, matching its indent."""
    m = re.search(anchor_pattern, src, re.M)
    if not m: return src, False
    indent = m.group(1)
    new_line = f"\n{indent}else if (pc.effectId == {NEW_ID}) rgb = {FUNC_NAME}(rgb);"
    return src[:m.end()] + new_line + src[m.end():], True

def insert_func_before(src, marker_regex):
    """Insert FUNC_DEF before the first match of marker_regex (e.g. 'void main(')."""
    m = re.search(marker_regex, src)
    if not m: return src, False
    return src[:m.start()] + "\n" + FUNC_DEF + src[m.start():], True

# ---------------- postfx ----------------
def patch_postfx():
    print(f"[shader] {POSTFX}")
    if not os.path.isfile(POSTFX): print("  !! missing"); return
    src = read(POSTFX)
    if already_patched(src):
        print("  already patched, skipping"); return

    anchor = (r'^([ \t]*)else\s+if\s*\(\s*pc\.effectId\s*==\s*20\s*\)\s*'
              r'rgb\s*=\s*applyAnimeEdge[^;]*;')
    src, ok1 = insert_call_after(src, anchor)
    if not ok1: print("  !! anchor (effectId==20) not found"); return

    src, ok2 = insert_func_before(src, r'\nvoid\s+main\s*\(')
    if not ok2: print("  !! void main() not found"); return

    backup(POSTFX); write(POSTFX, src)
    print("  patched")

# ---------------- sgsr / sgsr_quality ----------------
def patch_sgsr(path):
    print(f"[shader] {path}")
    if not os.path.isfile(path): print("  !! missing"); return
    src = read(path)
    if already_patched(src):
        print("  already patched, skipping"); return

    anchor = (r'^([ \t]*)else\s+if\s*\(\s*pc\.effectId\s*==\s*4\s*\)\s*'
              r'rgb\s*=\s*applyNatural\s*\(\s*rgb\s*\)\s*;')
    src, ok1 = insert_call_after(src, anchor)
    if not ok1: print("  !! anchor (effectId==4 applyNatural) not found"); return

    # Define helper before applyPostFX so it's visible inside that function
    src, ok2 = insert_func_before(src, r'\nvoid\s+applyPostFX\s*\(')
    if not ok2:
        # fallback: before main
        src, ok2 = insert_func_before(src, r'\nvoid\s+main\s*\(')
    if not ok2: print("  !! no insertion point found"); return

    backup(path); write(path, src)
    print("  patched")

# ---------------- main ----------------
def main():
    if not os.path.isfile(JAVA):
        print("!! run from repo root (winlator_ludashi_plus)"); sys.exit(1)
    patch_java()
    patch_postfx()
    for p in SGSRS:
        patch_sgsr(p)

    if "--build" in sys.argv:
        print("\n[build] ./gradlew assembleDebug")
        rc = subprocess.call(["./gradlew","assembleDebug"])
        print(f"gradle exit={rc}")
        apk = "app/build/outputs/apk/debug/app-debug.apk"
        if os.path.isfile(apk): print(f"APK ready -> {apk}")

if __name__ == "__main__":
    main()
