#!/usr/bin/env python3
import os

def log(m, ok=True):
    print(("[✓] " if ok else "[!] ") + m)

def patch(path, reps):
    if not os.path.exists(path):
        log(f"Missing: {path}", ok=False)
        return
    with open(path, "r", encoding="utf-8", errors="ignore") as f:
        c = f.read()
    o = c
    for old, new in reps:
        c = c.replace(old, new)
    if c != o:
        with open(path, "w", encoding="utf-8") as f:
            f.write(c)
        log(f"Patched: {path}")
    else:
        log(f"No change: {path}", ok=False)

# 1. GameFolderPrefs.java — swap the 're7' subfolder hint for 'mhf'
patch(
    "app/src/main/java/com/winlator/cmod/core/GameFolderPrefs.java",
    [('new File(picked, "re7")', 'new File(picked, "mhf")')]
)

# 2. Layout — fix the user-facing string
patch(
    "app/src/main/res/layout/activity_boot_splash.xml",
    [("Select the uncompressed PC game folder (re7.exe + .pak). Files stay where they are.",
      "Select the uncompressed PC game folder (mhf.exe). Files stay where they are.")]
)

# 3. Final audit — strict, skip C/C++ false positives
import re
log("\n=== Final audit (Java/XML/Gradle/JSON only) ===")
exts = (".xml", ".java", ".gradle", ".pro", ".json")
hits = 0
for root, _, files in os.walk("app/src"):
    for f in files:
        if not f.endswith(exts):
            continue
        fp = os.path.join(root, f)
        try:
            with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
                for i, line in enumerate(fh, 1):
                    if re.search(r"(?<![A-Za-z0-9_])re7(?![A-Za-z0-9_])",
                                 line, re.IGNORECASE):
                        log(f"{fp}:{i}: {line.strip()[:140]}", ok=False)
                        hits += 1
        except Exception:
            pass
log(f"\nTotal leftover 're7' hits: {hits}")
