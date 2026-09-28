package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.io.File;
import java.util.Locale;

/**
 * Game folder discovery + persistence.
 *
 * Handles:
 *  - case-insensitive matching (mhf.exe / MHF.EXE / Mhf.exe)
 *  - nested folder layouts (up to MAX_DEPTH levels)
 *  - parent fallback when user picks too deep
 *  - sentinel-based detection if mhf.exe is missing (pak+dll fingerprint)
 *  - special characters in folder names (no encoding assumptions here —
 *    the path passed in must already be a decoded real filesystem path)
 */
public final class GameFolderPrefs {

    public static final String DRIVE = "E:";
    public static final String GAME_EXE = "mhf.exe";
    private static final String KEY_PATH = "game_folder_path";
    private static final String KEY_URI = "game_folder_uri";
    private static final String PREF = "bh7";

    // Accept any of these as the game executable (case-insensitive)
    private static final String[] EXE_CANDIDATES = {
            "mhf.exe", "mhf2.exe", "mhfz.exe", "mhfrontier.exe"
    };

    // Directory names that usually contain the game root when nested
    private static final String[] HINT_DIRS = {
            "mhf", "mhfz", "monster hunter frontier", "mhf-g", "mhf-z"
    };

    // How deep we search below the picked folder
    private static final int MAX_DEPTH = 4;

    // How many levels of parents to check if user picked too deep
    private static final int MAX_PARENT_WALK = 3;

    private GameFolderPrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF, 0);
    }

    public static String getPath(Context context) {
        return prefs(context).getString(KEY_PATH, null);
    }

    public static String getUri(Context context) {
        return prefs(context).getString(KEY_URI, null);
    }

    public static void save(Context context, File folder, Uri uri) {
        SharedPreferences.Editor ed = prefs(context).edit();
        ed.putString(KEY_PATH, folder != null ? folder.getAbsolutePath() : null);
        ed.putString(KEY_URI, uri != null ? uri.toString() : null);
        ed.apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_PATH).remove(KEY_URI).apply();
    }

    /**
     * Locate the folder that contains the game executable.
     *
     * Tries, in order:
     *  1. The picked folder itself
     *  2. Its immediate parent chain (up to MAX_PARENT_WALK levels) — the user
     *     may have navigated into a subfolder like "bin" or "data"
     *  3. Recursive search of the picked folder to MAX_DEPTH, preferring
     *     directories whose names match HINT_DIRS
     *
     * Returns null if nothing that looks like a game install is found.
     */
    public static File findGameRoot(File picked) {
        if (picked == null) return null;

        // If the user handed us a file (e.g. picked mhf.exe directly), use its dir
        if (picked.isFile()) {
            File parent = picked.getParentFile();
            if (parent != null) picked = parent;
        }
        if (!picked.isDirectory()) return null;

        // 1. The picked folder itself
        if (looksLikeGameDir(picked)) return picked;

        // 2. Parent chain
        File up = picked.getParentFile();
        for (int i = 0; i < MAX_PARENT_WALK && up != null; i++) {
            if (looksLikeGameDir(up)) return up;
            up = up.getParentFile();
        }

        // 3. Recursive descent
        File hit = searchRecursive(picked, 0);
        if (hit != null) return hit;

        // 4. Last resort: hunt HINT_DIRS anywhere below
        return searchByHintName(picked, 0);
    }

    private static File searchRecursive(File dir, int depth) {
        if (dir == null || depth > MAX_DEPTH) return null;
        if (depth > 0 && looksLikeGameDir(dir)) return dir;

        File[] kids = dir.listFiles();
        if (kids == null) return null;

        // Pass 1: prefer hint-named directories
        for (File k : kids) {
            if (!k.isDirectory()) continue;
            if (k.getName().startsWith(".")) continue;
            String lower = k.getName().toLowerCase(Locale.ROOT);
            for (String hint : HINT_DIRS) {
                if (lower.equals(hint) || lower.contains(hint)) {
                    if (looksLikeGameDir(k)) return k;
                    File nested = searchRecursive(k, depth + 1);
                    if (nested != null) return nested;
                }
            }
        }

        // Pass 2: everything else
        for (File k : kids) {
            if (!k.isDirectory()) continue;
            if (k.getName().startsWith(".")) continue;
            File hit = searchRecursive(k, depth + 1);
            if (hit != null) return hit;
        }
        return null;
    }

    private static File searchByHintName(File dir, int depth) {
        if (dir == null || depth > MAX_DEPTH) return null;
        File[] kids = dir.listFiles();
        if (kids == null) return null;
        for (File k : kids) {
            if (!k.isDirectory()) continue;
            String lower = k.getName().toLowerCase(Locale.ROOT);
            for (String hint : HINT_DIRS) {
                if (lower.equals(hint) || lower.contains(hint)) return k;
            }
        }
        for (File k : kids) {
            if (!k.isDirectory()) continue;
            File hit = searchByHintName(k, depth + 1);
            if (hit != null) return hit;
        }
        return null;
    }

    /**
     * True if the folder looks like an MHF install.
     *
     * Primary check: any EXE_CANDIDATES present (case-insensitive).
     * Secondary: at least 3 .pak files plus at least one .dll or .ini
     * (MHF's data layout — useful if the executable was renamed or missing).
     */
    public static boolean looksLikeGameDir(File dir) {
        if (dir == null || !dir.isDirectory()) return false;

        File[] files = dir.listFiles();
        if (files == null) return false;

        boolean hasExe = false;
        int pakCount = 0;
        int dllCount = 0;
        int iniCount = 0;

        for (File f : files) {
            if (!f.isFile()) continue;
            String n = f.getName().toLowerCase(Locale.ROOT);

            for (String exe : EXE_CANDIDATES) {
                if (n.equals(exe)) { hasExe = true; break; }
            }
            if (n.endsWith(".pak")) pakCount++;
            else if (n.endsWith(".dll")) dllCount++;
            else if (n.endsWith(".ini")) iniCount++;
        }

        if (hasExe) return true;
        return pakCount >= 3 && (dllCount >= 1 || iniCount >= 1);
    }

    /** Backwards-compatible alias. */
    public static boolean isGameDir(File dir) {
        return looksLikeGameDir(dir);
    }
}
