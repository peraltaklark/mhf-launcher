#!/usr/bin/env python3
import os, re, shutil
from pathlib import Path

# Parent dir where re7_decompiled lives (one level up)
PARENT          = os.path.abspath("..")
RE7_RES         = os.path.join(PARENT, "re7_decompiled/res")
PROJECT_RES     = "app/src/main/res"
JAVA_DIR        = "app/src/main/java/com/winlator/cmod"

def log(m, ok=True):
    print(("[✓] " if ok else "[!] ") + m)

def ensure_parent(path):
    Path(os.path.dirname(path)).mkdir(parents=True, exist_ok=True)

# ---- 1. Copy drawables with MHF names ----
log("=== Copying drawables (with MHF names) ===")
sources = [
    (f"{RE7_RES}/drawable/re7_launcher.png",
     f"{PROJECT_RES}/drawable/mhf_launcher.png"),
    (f"{RE7_RES}/drawable-nodpi/re7_boot_cover.jpg",
     f"{PROJECT_RES}/drawable-nodpi/mhf_boot_cover.jpg"),
]
for src, dst in sources:
    if not os.path.exists(src):
        log(f"Source missing: {src}", ok=False)
        continue
    ensure_parent(dst)
    shutil.copy2(src, dst)
    log(f"Copied: {os.path.basename(src)} -> {dst}")

# ---- 2. Update XML drawable references ----
log("\n=== Patching XML references ===")
def patch_file(path, reps):
    if not os.path.exists(path):
        return False
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        c = f.read()
    o = c
    for old, new in reps:
        c = c.replace(old, new)
    if c != o:
        with open(path, "w", encoding="utf-8") as f:
            f.write(c)
        log(f"Patched: {path}")
        return True
    return False

# Ensure layout points to new MHF drawable
patch_file(
    f"{PROJECT_RES}/layout/activity_boot_splash.xml",
    [("@drawable/re7_boot_cover", "@drawable/mhf_boot_cover"),
     ("@drawable/re7_launcher",   "@drawable/mhf_launcher"),
     ("@id/re7_boot_cover",       "@id/mhf_boot_cover"),
     ("@id/re7_launcher",         "@id/mhf_launcher")]
)

# ---- 3. Report every remaining 're7' (file + line + context) ----
log("\n=== Detailed 're7' audit ===")
extensions = (".xml", ".java", ".kt", ".gradle", ".pro", ".json", ".c", ".cpp", ".h")
total = 0
for root, _, files in os.walk("app/src"):
    for f in files:
        if not f.endswith(extensions):
            continue
        fp = os.path.join(root, f)
        try:
            with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
                lines = fh.readlines()
        except Exception:
            continue
        for i, line in enumerate(lines, 1):
            if re.search(r"re7", line, re.IGNORECASE):
                log(f"{fp}:{i}: {line.strip()[:120]}", ok=False)
                total += 1
log(f"\nTotal hits: {total}")
