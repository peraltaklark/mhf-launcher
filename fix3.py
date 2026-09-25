#!/usr/bin/env python3
import os, re

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

# --- Safe cosmetic renames ---
log("=== Cosmetic renames ===")
patch("app/src/main/java/com/winlator/cmod/core/SnapdragonProfile.java", [
    ('"RE7Soc"', '"MHFSoc"'),
])

patch("app/src/main/assets/device-table.json", [
    ("Device auto-detect table for the RE7 port.",
     "Device auto-detect table for the MHF port."),
    ("shared_re7_preset", "shared_mhf_preset"),
])

# --- Strict audit: skip C/C++ and known false positives ---
log("\n=== Strict audit (Java + XML + Gradle only) ===")
FALSE_POSITIVE_FILES = (
    "shim.cpp", "elf.h", "framegen.cpp", "record_impl.inc",
)
extensions = (".xml", ".java", ".gradle", ".pro", ".json")
hits = 0
for root, _, files in os.walk("app/src"):
    for f in files:
        if not f.endswith(extensions):
            continue
        if f in FALSE_POSITIVE_FILES:
            continue
        fp = os.path.join(root, f)
        try:
            with open(fp, "r", encoding="utf-8", errors="ignore") as fh:
                lines = fh.readlines()
        except Exception:
            continue
        for i, line in enumerate(lines, 1):
            # Match 're7' only when preceded by a non-letter (avoid 'hardware7')
            for m in re.finditer(r"(?<![A-Za-z0-9_])re7|re7(?![A-Za-z0-9_])",
                                 line, re.IGNORECASE):
                log(f"{fp}:{i}: {line.strip()[:160]}", ok=False)
                hits += 1
                break

log(f"\nRemaining hits: {hits}")
