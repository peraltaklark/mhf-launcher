package com.winlator.cmod.core;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.util.Log;
import androidx.autofill.HintConstants;
import kotlinx.coroutines.DebugKt;

/* JADX INFO: loaded from: classes10.dex */
public final class SnapdragonProfile {
    public static final String DRIVER_8GEN2 = "turnip-oneui7-8gen2";
    private static final float PHONE_MIN_AR = 1.9f;
    public static final String PHONE_SCREEN = "1600x720";
    public static final int TABLET_HEIGHT = 800;
    private static final String TAG = "MHFSoc";
    public final String autoDriverId;
    public final String box64Preset;
    public final String driverId;
    public final boolean driverOverridden;
    public final String fexPreset;
    public final String formFactor;
    public final int frameRate;
    public final String marketing;
    public final String qualityId;
    public final boolean qualityOverridden;
    public final String screenSizeCap;
    public final int sharpness;
    public final String soc;
    public final Tier tier;
    public static final String DRIVER_8GEN3 = "turnip26.3.0-oneui";
    public static final String DRIVER_A8XX = "turnip-a8xx-v31";
    public static final String DRIVER_A7XX = "turnip26.2.0-oneui-r8";
    public static final String DRIVER_A6XX = "turnip26.2.0";
    public static final String[] DRIVER_CHOICES = {DebugKt.DEBUG_PROPERTY_VALUE_AUTO, "turnip-oneui7-8gen2", DRIVER_8GEN3, DRIVER_A8XX, DRIVER_A7XX, DRIVER_A6XX};
    public static final String[] DRIVER_LABELS = {"Auto (by SoC)", "Turnip OneUI7 8Gen2 (Adreno 740)", "Turnip 26.3 OneUI (8 Gen 3 / A750)", "Turnip A8xx v31 (8 Elite / Elite Gen 5)", "Turnip 26.2 OneUI-R8 (A7xx)", "Turnip 26.2 generic (Adreno 6xx)"};
    public static final String[] QUALITY_CHOICES = {DebugKt.DEBUG_PROPERTY_VALUE_AUTO, "flagship", "mid", "legacy"};
    public static final String[] QUALITY_LABELS = {"Auto quality", "Flagship (sharp 100)", "Mid (sharp 85)", "Legacy (sharp 75)"};

    public enum Tier {
        FLAGSHIP,
        MID,
        LEGACY,
        UNKNOWN
    }

    private SnapdragonProfile(String soc, String marketing, Tier tier, String autoDriverId, String driverId, String qualityId, int frameRate, int sharpness, String box64Preset, String fexPreset, String screenSizeCap, String formFactor, boolean driverOverridden, boolean qualityOverridden) {
        this.soc = soc;
        this.marketing = marketing;
        this.tier = tier;
        this.autoDriverId = autoDriverId;
        this.driverId = driverId;
        this.qualityId = qualityId;
        this.frameRate = frameRate;
        this.sharpness = sharpness;
        this.box64Preset = box64Preset;
        this.fexPreset = fexPreset;
        this.screenSizeCap = screenSizeCap;
        this.formFactor = formFactor;
        this.driverOverridden = driverOverridden;
        this.qualityOverridden = qualityOverridden;
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Removed duplicated region for block: B:34:0x006e  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x007f  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static SnapdragonProfile resolve(Context context) {
        String soc = socModel();
        Detected detected = detect(soc);
        Tier tier = detected.tier;

        // Default quality tier derived from detected SoC
        String autoQualityId;
        switch (tier) {
            case FLAGSHIP: autoQualityId = "flagship"; break;
            case MID:      autoQualityId = "mid";      break;
            default:       autoQualityId = "legacy";   break;
        }

        // Manual overrides saved from the launcher spinners
        String driverOverride  = MhfDevicePrefs.getDriverOverride(context);
        String qualityOverride = MhfDevicePrefs.getQualityOverride(context);

        boolean driverOverridden  = driverOverride  != null
                && !driverOverride.isEmpty()
                && !"auto".equalsIgnoreCase(driverOverride);
        boolean qualityOverridden = qualityOverride != null
                && !qualityOverride.isEmpty()
                && !"auto".equalsIgnoreCase(qualityOverride);

        String driverId  = driverOverridden  ? driverOverride  : detected.driverId;
        String qualityId = qualityOverridden ? qualityOverride : autoQualityId;

        // Frame rate and sharpness per quality tier
        int frameRate = 35;
        int sharpness;
        switch (qualityId) {
            case "flagship": sharpness = 100; break;
            case "mid":      sharpness = 85;  break;
            default:         sharpness = 75;  break;
        }

        String box64Preset = "PERFORMANCE";
        String fexPreset   = "INTERMEDIATE";

        // Form factor & screen size cap (panelLandscape unknown here; detected later)
        String formFactor    = detectFormFactor(context, null);
        String screenSizeCap = resolveScreenSize(formFactor, null);

        return new SnapdragonProfile(
                soc,
                detected.marketing,
                tier,
                detected.driverId,
                driverId,
                qualityId,
                frameRate,
                sharpness,
                box64Preset,
                fexPreset,
                screenSizeCap,
                formFactor,
                driverOverridden,
                qualityOverridden
        );
    }

    public boolean isTablet() {
        return "tablet".equalsIgnoreCase(this.formFactor);
    }

    public String screenSizeOr(String panelLandscape) {
        return resolveScreenSize(this.formFactor, panelLandscape);
    }

    public String screenSizeOr(Context context, String panelLandscape) {
        String form = detectFormFactor(context, panelLandscape);
        return resolveScreenSize(form, panelLandscape);
    }

    public static String resolveScreenSize(String formFactor, String panelLandscape) {
        if ("tablet".equalsIgnoreCase(formFactor)) {
            return tabletScreenForPanel(panelLandscape);
        }
        return PHONE_SCREEN;
    }

    public static String detectFormFactor(Context context, String panelLandscape) {
        float ar = parseAspect(panelLandscape);
        if (context != null) {
            try {
                Configuration cfg = context.getResources().getConfiguration();
                if (cfg.smallestScreenWidthDp >= 600) {
                    return "tablet";
                }
                if ((cfg.screenLayout & 15) >= 3 && ar > 0.0f && ar < PHONE_MIN_AR) {
                    return "tablet";
                }
                if (ar <= 0.0f) {
                    int w = Math.max(cfg.screenWidthDp, cfg.screenHeightDp);
                    int h = Math.min(cfg.screenWidthDp, cfg.screenHeightDp);
                    if (h > 0) {
                        ar = w / h;
                    }
                }
            } catch (Throwable th) {
            }
        }
        return (ar <= 0.0f || ar >= PHONE_MIN_AR) ? HintConstants.AUTOFILL_HINT_PHONE : "tablet";
    }

    private static float parseAspect(String panelLandscape) {
        if (panelLandscape != null) {
            try {
                if (!panelLandscape.isEmpty()) {
                    String[] p = panelLandscape.toLowerCase().split("x");
                    int w = Integer.parseInt(p[0].trim());
                    int h = Integer.parseInt(p[1].trim());
                    if (w < h) {
                        w = h;
                        h = w;
                    }
                    if (h <= 0) {
                        return 0.0f;
                    }
                    return w / h;
                }
            } catch (Throwable th) {
                return 0.0f;
            }
        }
        return 0.0f;
    }

    public static String tabletScreenForPanel(String panelLandscape) {
        float ar = parseAspect(panelLandscape);
        if (ar <= 0.0f) {
            ar = 1.6f;
        }
        int outW = Math.round(TABLET_HEIGHT * ar) & (-2);
        int outH = 800 & (-2);
        if (outW < 1024) {
            outW = 1024;
        }
        if (outW > 1920) {
            outW = 1920;
        }
        return outW + "x" + outH;
    }

    public String buildEnvVars() {
        String env = "WINE_FAST_YIELD=1 WRAPPER_MAX_IMAGE_COUNT=0 VKD3D_SHADER_MODEL=6_6 MESA_SHADER_CACHE_DISABLE=false MESA_SHADER_CACHE_MAX_SIZE=512MB WINEESYNC=1 WINEDEBUG=-all DXVK_FRAME_RATE=" + this.frameRate + " DXVK_STATE_CACHE=1 DXVK_DISABLE_TIMELINE_SEMAPHORES=1 BOX64_DYNAREC_BIGBLOCK=1 BOX64_DYNAREC_STRONGMEM=0 BOX64_DYNAREC_SAFEFLAGS=2";
        if (DRIVER_A8XX.equals(this.driverId)) {
            return env + " TU_DEBUG=sysmem";
        }
        return env;
    }

    public String dxwrapperConfig(String defaults) {
        String cfg = (defaults == null ? "" : defaults).replace(",framerate=0", ",framerate=" + this.frameRate).replace(",async=0", ",async=1").replace(",asyncCache=0", ",asyncCache=1");
        if (cfg.contains("framerate=")) {
            return cfg.replaceAll("framerate=\\d+", "framerate=" + this.frameRate);
        }
        return cfg;
    }

    public static String socModel() {
        if (Build.VERSION.SDK_INT < 31 || Build.SOC_MODEL == null || Build.SOC_MODEL.isEmpty()) {
            return Build.HARDWARE == null ? "" : Build.HARDWARE;
        }
        return Build.SOC_MODEL;
    }

    private static Detected detect(String soc) {
        if (!match(soc, "SM8550", "SM8550P")) {
            if (!match(soc, "SM8650", "SM8635", "SM7675")) {
                if (!match(soc, "SM8750", "SM8735")) {
                    if (!match(soc, "SM8850", "SM8845")) {
                        if (!match(soc, "SM8450", "SM8475", "SM8350", "SM7450", "SM7475")) {
                            if (match(soc, "SM8250", "SM8150", "SM7325", "SM7350", "SM7225", "SM7250", "SM7150", "SM7125", "SM7315", "SM6115", "SM6125", "SM6150", "SM6350", "SM6375", "SM6450", "SM6650", "SM6800", "SM6850", "SM6950", "SM6225", "SM4375", "QCM6490")) {
                                String name = match(soc, "SM8250") ? "Snapdragon 865 / 860 (Adreno 640)" : "Snapdragon 7-series / Adreno 6xx";
                                return new Detected(name, Tier.LEGACY, DRIVER_A6XX);
                            }
                            String name2 = Build.HARDWARE;
                            String hw = (name2 == null ? "" : Build.HARDWARE).toUpperCase();
                            if (hw.contains("KONA") || hw.contains("SM8250")) {
                                return new Detected("Snapdragon 865/860 (kona)", Tier.LEGACY, DRIVER_A6XX);
                            }
                            if (hw.contains("WAIPIO") || hw.contains("SM8450")) {
                                return new Detected("Snapdragon 8 Gen 1 (waipio)", Tier.MID, DRIVER_A7XX);
                            }
                            if (hw.contains("CALTROP") || hw.contains("SM8550") || hw.contains("TAROKO")) {
                                return new Detected("Snapdragon 8 Gen 2", Tier.FLAGSHIP, "turnip-oneui7-8gen2");
                            }
                            if (hw.contains("LANAI") || hw.contains("PINEAPPLE") || hw.contains("SM8650")) {
                                return new Detected("Snapdragon 8 Gen 3", Tier.FLAGSHIP, DRIVER_8GEN3);
                            }
                            if (hw.contains("SUN") || hw.contains("SM8750")) {
                                return new Detected("Snapdragon 8 Elite (sun)", Tier.FLAGSHIP, DRIVER_A8XX);
                            }
                            if (!hw.contains("CANOE") && !hw.contains("SM8850")) {
                                Log.w(TAG, "unlisted SoC '" + soc + "' hw='" + hw + "' -> generic Turnip + legacy quality");
                                return new Detected("Unknown Snapdragon / other", Tier.UNKNOWN, DRIVER_A6XX);
                            }
                            return new Detected("Snapdragon 8 Elite Gen 5 (canoe)", Tier.FLAGSHIP, DRIVER_A8XX);
                        }
                        return new Detected("Snapdragon 8 Gen 1 / 888 class", Tier.MID, DRIVER_A7XX);
                    }
                    return new Detected("Snapdragon 8 Elite Gen 5 (Adreno 840)", Tier.FLAGSHIP, DRIVER_A8XX);
                }
                return new Detected("Snapdragon 8 Elite (Adreno 830)", Tier.FLAGSHIP, DRIVER_A8XX);
            }
            return new Detected("Snapdragon 8 Gen 3 class", Tier.FLAGSHIP, DRIVER_8GEN3);
        }
        return new Detected("Snapdragon 8 Gen 2", Tier.FLAGSHIP, "turnip-oneui7-8gen2");
    }

    private static boolean match(String soc, String... ids) {
        if (soc == null || soc.isEmpty()) {
            return false;
        }
        for (String id : ids) {
            if (soc.equals(id) || soc.startsWith(id)) {
                return true;
            }
        }
        return false;
    }

    private static Quality qualityFor(Tier tier) {
        switch (tier) {
            case FLAGSHIP:
                return new Quality(35, 100, "PERFORMANCE", "INTERMEDIATE");
            case MID:
                return new Quality(35, 85, "PERFORMANCE", "INTERMEDIATE");
            default:
                return new Quality(35, 75, "PERFORMANCE", "INTERMEDIATE");
        }
    }

    private static final class Detected {
        final String driverId;
        final String marketing;
        final Tier tier;

        Detected(String marketing, Tier tier, String driverId) {
            this.marketing = marketing;
            this.tier = tier;
            this.driverId = driverId;
        }
    }

    private static final class Quality {
        final String box64Preset;
        final String fexPreset;
        final int frameRate;
        final int sharpness;

        Quality(int frameRate, int sharpness, String box64Preset, String fexPreset) {
            this.frameRate = frameRate;
            this.sharpness = sharpness;
            this.box64Preset = box64Preset;
            this.fexPreset = fexPreset;
        }
    }
}
