package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;

/* JADX INFO: loaded from: classes10.dex */
public final class GameFolderPrefs {
    public static final String DRIVE = "E:";
    public static final String GAME_EXE = "mhf.exe";
    private static final String KEY_PATH = "game_folder_path";
    private static final String KEY_URI = "game_folder_uri";
    private static final String PREF = "bh7";

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

    public static File findGameRoot(File picked) {
        if (isGameDir(picked)) {
            return picked;
        }
        if (picked == null || !picked.isDirectory()) {
            return null;
        }
        File named = new File(picked, "re7");
        if (isGameDir(named)) {
            return named;
        }
        File[] kids = picked.listFiles(new FileFilter() { // from class: com.winlator.cmod.core.GameFolderPrefs$$ExternalSyntheticLambda0
            @Override // java.io.FileFilter
            public final boolean accept(File file) {
                return file.isDirectory();
            }
        });
        if (kids == null) {
            return null;
        }
        for (File kid : kids) {
            if (isGameDir(kid)) {
                return kid;
            }
        }
        return null;
    }

    public static boolean isGameDir(File dir) {
        File[] paks;
        if (dir == null || !dir.isDirectory()) {
            return false;
        }
        File exe = new File(dir, "mhf.exe");
        if (!exe.isFile() || exe.length() < 1048576 || (paks = dir.listFiles(new FilenameFilter() { // from class: com.winlator.cmod.core.GameFolderPrefs$$ExternalSyntheticLambda1
            @Override // java.io.FilenameFilter
            public final boolean accept(File file, String str) {
                return GameFolderPrefs.lambda$isGameDir$0(file, str);
            }
        })) == null) {
            return false;
        }
        long pakBytes = 0;
        for (File p : paks) {
            pakBytes += p.length();
        }
        return pakBytes > 104857600;
    }

    static /* synthetic */ boolean lambda$isGameDir$0(File d, String n) {
        return n != null && n.toLowerCase().endsWith(".pak");
    }
}
