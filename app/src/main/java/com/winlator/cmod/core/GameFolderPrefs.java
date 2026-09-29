package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.io.File;
import java.util.Locale;

/** Game folder discovery + persistence. A folder qualifies only if it contains the game exe. */
public final class GameFolderPrefs {

    public static final String DRIVE = "E:";
    public static final String GAME_EXE = "mhf.exe";
    private static final String KEY_PATH = "game_folder_path";
    private static final String KEY_URI = "game_folder_uri";
    private static final String PREF = "bh7";

    // Some servers rename mhf.exe to HY5.exe; either one is accepted.
    private static final String[] EXE_NAMES = { "mhf.exe", "HY5.exe" };

    private static final String[] HINT_DIRS = {
            "mhf", "mhfz", "monster hunter frontier", "mhf-g", "mhf-z"
    };

    private static final int MAX_DEPTH = 4;
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

    public static boolean save(Context context, File folder, Uri uri) {
        if (!looksLikeGameDir(folder)) return false;
        prefs(context).edit()
                .putString(KEY_PATH, folder.getAbsolutePath())
                .putString(KEY_URI, uri != null ? uri.toString() : null)
                .apply();
        return true;
    }

    public static void clear(Context context) {
        prefs(context).edit().remove(KEY_PATH).remove(KEY_URI).apply();
    }

    public static File findGameRoot(File picked) {
        if (picked == null) return null;

        if (picked.isFile()) {
            File parent = picked.getParentFile();
            if (parent != null) picked = parent;
        }
        if (!picked.isDirectory()) return null;

        if (looksLikeGameDir(picked)) return picked;

        File up = picked.getParentFile();
        for (int i = 0; i < MAX_PARENT_WALK && up != null; i++) {
            if (looksLikeGameDir(up)) return up;
            up = up.getParentFile();
        }

        return searchRecursive(picked, 0);
    }

    private static File searchRecursive(File dir, int depth) {
        if (dir == null || depth > MAX_DEPTH) return null;
        if (depth > 0 && looksLikeGameDir(dir)) return dir;

        File[] kids = dir.listFiles();
        if (kids == null) return null;

        for (File k : kids) {
            if (!k.isDirectory() || k.getName().startsWith(".")) continue;
            String lower = k.getName().toLowerCase(Locale.ROOT);
            for (String hint : HINT_DIRS) {
                if (lower.contains(hint)) {
                    File hit = searchRecursive(k, depth + 1);
                    if (hit != null) return hit;
                    break;
                }
            }
        }

        for (File k : kids) {
            if (!k.isDirectory() || k.getName().startsWith(".")) continue;
            File hit = searchRecursive(k, depth + 1);
            if (hit != null) return hit;
        }
        return null;
    }

    /** Real file name of the game exe in dir (mhf.exe preferred), or null. */
    public static String findExeName(File dir) {
        if (dir == null || !dir.isDirectory()) return null;
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (String want : EXE_NAMES) {
            for (File f : files) {
                if (f.isFile() && f.getName().equalsIgnoreCase(want)) return f.getName();
            }
        }
        return null;
    }

    public static boolean looksLikeGameDir(File dir) {
        return findExeName(dir) != null;
    }

    /** Backwards-compatible alias. */
    public static boolean isGameDir(File dir) {
        return looksLikeGameDir(dir);
    }
}
