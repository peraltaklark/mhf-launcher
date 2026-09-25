package com.winlator.cmod.core;

import android.content.Context;
import android.content.SharedPreferences;
import kotlinx.coroutines.DebugKt;

/* JADX INFO: loaded from: classes10.dex */
public final class MhfDevicePrefs {
    private static final String KEY_DRIVER = "driver_override";
    private static final String KEY_QUALITY = "quality_override";
    private static final String PREF = "bh7";

    private MhfDevicePrefs() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF, 0);
    }

    public static String getDriverOverride(Context context) {
        return prefs(context).getString(KEY_DRIVER, DebugKt.DEBUG_PROPERTY_VALUE_AUTO);
    }

    public static String getQualityOverride(Context context) {
        return prefs(context).getString(KEY_QUALITY, DebugKt.DEBUG_PROPERTY_VALUE_AUTO);
    }

    public static void setDriverOverride(Context context, String driverId) {
        String v = (driverId == null || driverId.isEmpty()) ? DebugKt.DEBUG_PROPERTY_VALUE_AUTO : driverId;
        prefs(context).edit().putString(KEY_DRIVER, v).apply();
    }

    public static void setQualityOverride(Context context, String qualityId) {
        String v = (qualityId == null || qualityId.isEmpty()) ? DebugKt.DEBUG_PROPERTY_VALUE_AUTO : qualityId;
        prefs(context).edit().putString(KEY_QUALITY, v).apply();
    }
}
