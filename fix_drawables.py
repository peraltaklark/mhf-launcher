#!/usr/bin/env python3
import os
import re
import shutil
from pathlib import Path

RE7_RES_DIR     = "re7_decompiled/res"
PROJECT_RES_DIR = "app/src/main/res"
JAVA_DIR        = "app/src/main/java/com/winlator/cmod"

# Old name -> New name (all lowercase snake_case)
RENAMES = {
    "re7_boot_cover": "mhf_boot_cover",
    "re7_launcher":   "mhf_launcher",
}

def log(m, ok=True):
    print(("[✓] " if ok else "[!] ") + m)

# ---- 1. Copy & rename drawables ----
log("=== Copying & renaming drawables ===")
# source: (path, new filename)
sources = [
    (f"{RE7_RES_DIR}/drawable/re7_launcher.png",
     f"{PROJECT_RES_DIR}/drawable/mhf_launcher.png"),
    (f"{RE7_RES_DIR}/drawable-nodpi/re7_boot_cover.jpg",
     f"{PROJECT_RES_DIR}/drawable-nodpi/mhf_boot_cover.jpg"),
]

for src, dst in sources:
    if not os.path.exists(src):
        log(f"Source missing: {src}", ok=False)
        continue
    Path(os.path.dirname(dst)).mkdir(parents=True, exist_ok=True)
    shutil.copy2(src, dst)
    log(f"Copied: {src} -> {dst}")

# Remove any old RE7-named drawables from the project (cleanup)
for root, _, files in os.walk(PROJECT_RES_DIR):
    for f in files:
        if f.startswith("re7_"):
            p = os.path.join(root, f)
            os.remove(p)
            log(f"Deleted stale: {p}")

# ---- 2. Update XML references (layout, styles, etc.) ----
log("\n=== Updating XML references ===")
def patch_file(path, replacements):
    if not os.path.exists(path):
        return False
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        c = f.read()
    orig = c
    for old, new in replacements:
        c = c.replace(old, new)
    if c != orig:
        with open(path, "w", encoding="utf-8") as f:
            f.write(c)
        log(f"Patched: {path}")
        return True
    return False

xml_replacements = [
    ("@drawable/re7_boot_cover", "@drawable/mhf_boot_cover"),
    ("@drawable/re7_launcher",   "@drawable/mhf_launcher"),
    ("@id/re7_boot_cover",       "@id/mhf_boot_cover"),
    ("@id/re7_launcher",         "@id/mhf_launcher"),
    # fallback: any remaining re7_ token in XML
    ("re7_boot_cover", "mhf_boot_cover"),
    ("re7_launcher",   "mhf_launcher"),
]

for root, _, files in os.walk(PROJECT_RES_DIR):
    for f in files:
        if f.endswith(".xml"):
            patch_file(os.path.join(root, f), xml_replacements)

# ---- 3. Update Java references ----
log("\n=== Updating Java references ===")
java_replacements = [
    ("re7_boot_cover", "mhf_boot_cover"),
    ("re7_launcher",   "mhf_launcher"),
    ("R.drawable.re7", "R.drawable.mhf"),
    ("R.id.re7",       "R.id.mhf"),
]

for root, _, files in os.walk(JAVA_DIR):
    for f in files:
        if f.endswith(".java"):
            patch_file(os.path.join(root, f), java_replacements)

# ---- 4. Final sweep: nuke any leftover 're7' tokens in project sources ----
log("\n=== Final sweep for leftover 're7' (case-insensitive) ===")
extensions = (".xml", ".java", ".kt", ".gradle", ".pro", ".json")
count = 0
for root, _, files in os.walk("app/src"):
    for f in files:
        if not f.endswith(extensions):
            continue
        fp = os.path.join(root, f)
        try:
            with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
                c = fh.read()
        except Exception:
            continue
        # Report leftovers, don't auto-replace (risky)
        for m in re.finditer(r"re7", c, re.IGNORECASE):
            line_no = c[:m.start()].count("\n") + 1
            log(f"  {fp}:{line_no}  <-- still contains 're7'", ok=False)
            count += 1

log(f"\nTotal leftover 're7' hits: {count}", ok=(count == 0))
log("Done. Now run: git add . && git commit -m 'Rename RE7 -> MHF' && git push")
