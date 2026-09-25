package com.winlator.cmod.core;

import android.content.Context;
import com.winlator.cmod.container.Container;
import java.util.Locale;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes10.dex */
public final class OpenGLDriverDefaults {
    private static final String AUTO_MESA_OVERRIDE = "autoMesaGlVersionOverride";
    private static final String INITIALIZED = "openGlDefaultInitialized";
    private static final String MESA_OVERRIDE = "MESA_GL_VERSION_OVERRIDE";

    private OpenGLDriverDefaults() {
    }

    public static boolean initialize(Context context, JSONObject data) {
        Object obj;
        if (data == null) {
            return false;
        }
        try {
            JSONObject extraData = data.optJSONObject("extraData");
            if (extraData == null) {
                extraData = new JSONObject();
            }
            if ("1".equals(extraData.optString(INITIALIZED, "0"))) {
                return false;
            }
            String config = data.optString("graphicsDriverConfig", Container.DEFAULT_GRAPHICSDRIVERCONFIG);
            try {
                String selectedVersion = resolveDriverVersion(context, configValue(config, "version"));
                boolean freedreno = isTurnipDriver(selectedVersion);
                EnvVars environment = new EnvVars(data.optString("envVars", Container.DEFAULT_ENV_VARS));
                boolean automaticOverride = false;
                if (!freedreno || environment.has(MESA_OVERRIDE)) {
                    obj = "0";
                } else {
                    obj = "0";
                    environment.put(MESA_OVERRIDE, "3.3");
                    automaticOverride = true;
                }
                data.put("graphicsDriver", freedreno ? "freedreno" : Container.DEFAULT_GRAPHICS_DRIVER);
                data.put("graphicsDriverConfig", putConfigValue(config, "version", selectedVersion));
                data.put("envVars", environment.toString());
                extraData.put(INITIALIZED, "1");
                extraData.put(AUTO_MESA_OVERRIDE, automaticOverride ? "1" : obj);
                data.put("extraData", extraData);
                return true;
            } catch (JSONException e) {
                return false;
            }
        } catch (JSONException e2) {
            return false;
        }
    }

    public static boolean initialize(Context context, Container container) {
        if (container == null) {
            return false;
        }
        if ("1".equals(container.getExtra(INITIALIZED, "0"))) {
            return false;
        }
        String config = container.getGraphicsDriverConfig();
        String selectedVersion = resolveDriverVersion(context, configValue(config, "version"));
        boolean freedreno = isTurnipDriver(selectedVersion);
        EnvVars environment = new EnvVars(container.getEnvVars());
        boolean automaticOverride = false;
        if (freedreno && !environment.has(MESA_OVERRIDE)) {
            environment.put(MESA_OVERRIDE, "3.3");
            automaticOverride = true;
        }
        container.setGraphicsDriver(freedreno ? "freedreno" : Container.DEFAULT_GRAPHICS_DRIVER);
        container.setGraphicsDriverConfig(putConfigValue(config, "version", selectedVersion));
        container.setEnvVars(environment.toString());
        container.putExtra(INITIALIZED, "1");
        container.putExtra(AUTO_MESA_OVERRIDE, automaticOverride ? "1" : "0");
        container.saveData();
        return true;
    }

    public static boolean isTurnipDriver(String version) {
        String value = version == null ? "" : version.toLowerCase(Locale.ROOT);
        return value.contains("turnip") || value.startsWith("tu-") || value.startsWith("mesa-turnip");
    }

    private static String resolveDriverVersion(Context context, String configuredVersion) {
        String candidate;
        if (isTurnipDriver(configuredVersion)) {
            candidate = configuredVersion;
        } else {
            candidate = "turnip-oneui7-8gen2";
        }
        try {
            return GPUInformation.isDriverSupported(candidate, context) ? candidate : DefaultVersion.WRAPPER;
        } catch (Throwable th) {
            return DefaultVersion.WRAPPER;
        }
    }

    private static String configValue(String config, String key) {
        if (config == null || config.isEmpty()) {
            return "";
        }
        String prefix = key + "=";
        for (String item : config.split(";", -1)) {
            if (item.startsWith(prefix)) {
                return item.substring(prefix.length());
            }
        }
        return "";
    }

    private static String putConfigValue(String config, String key, String value) {
        String source = config == null ? "" : config;
        String prefix = key + "=";
        String[] items = source.split(";", -1);
        StringBuilder result = new StringBuilder();
        boolean replaced = false;
        for (String item : items) {
            if (result.length() > 0) {
                result.append(';');
            }
            if (item.startsWith(prefix)) {
                result.append(prefix).append(value);
                replaced = true;
            } else {
                result.append(item);
            }
        }
        if (!replaced) {
            if (result.length() > 0 && result.charAt(result.length() - 1) != ';') {
                result.append(';');
            }
            result.append(prefix).append(value);
        }
        return result.toString();
    }
}
