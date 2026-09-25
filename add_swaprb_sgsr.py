#!/usr/bin/env python3
import os, re, shutil, sys, time
VULKAN = "app/src/main/cpp/winlator/renderer/vulkan"
FILES  = ["window_sgsr.frag", "window_sgsr_quality.frag"]

def ts(): return time.strftime("%Y%m%d-%H%M%S")
def read(p):
    with open(p,"r",encoding="utf-8") as f: return f.read()
def write(p,s):
    with open(p,"w",encoding="utf-8") as f: f.write(s)
def backup(p):
    b=f"{p}.bak-{ts()}"; shutil.copy2(p,b); print(f"  backup -> {b}")

for fname in FILES:
    p = f"{VULKAN}/{fname}"
    print(f"[shader] {p}")
    if not os.path.isfile(p): print("  !! missing"); continue
    src = read(p)
    orig = src

    if re.search(r'pc\.effectId\s*==\s*20', src):
        print("  already patched, skipping"); continue

    # 1) helper before applyPostFX
    helper = ("\nvec3 applySwapRB(vec3 c) {\n"
              "    return vec3(c.b, c.g, c.r);\n"
              "}\n")
    m = re.search(r'\nvoid\s+applyPostFX\s*\(', src)
    if not m: print("  !! applyPostFX not found"); continue
    src = src[:m.start()] + "\n" + helper + src[m.start():]

    # 2) branch after the "effectId == 4" line, matching its indent
    m = re.search(
        r'^([ \t]*)else\s+if\s*\(\s*pc\.effectId\s*==\s*4\s*\)\s*'
        r'rgb\s*=\s*applyNatural\s*\(\s*rgb\s*\)\s*;',
        src, re.M)
    if not m: print("  !! anchor (effectId==4) not found"); continue
    indent = m.group(1)
    new_line = f"\n{indent}else if (pc.effectId == 20) rgb = applySwapRB(rgb);"
    src = src[:m.end()] + new_line + src[m.end():]

    if src == orig:
        print("  no change"); continue
    backup(p); write(p, src)
    print("  patched")
