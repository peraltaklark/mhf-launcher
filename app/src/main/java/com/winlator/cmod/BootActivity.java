package com.winlator.cmod;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.winlator.cmod.core.WineThemeManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.autofill.HintConstants;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.contents.ContentProfile;
import com.winlator.cmod.contents.ContentsManager;
import com.winlator.cmod.core.MhfDevicePrefs;
import com.winlator.cmod.core.Callback;
import com.winlator.cmod.core.DefaultVersion;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.GameFolderPrefs;
import com.winlator.cmod.core.OpenGLDriverDefaults;
import com.winlator.cmod.core.SnapdragonProfile;
import com.winlator.cmod.core.WineInfo;
import com.winlator.cmod.core.WineRuntimeGuard;
import com.winlator.cmod.xenvironment.ImageFsInstaller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Iterator;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes8.dex */
public class BootActivity extends AppCompatActivity {
    private static final String CONTAINER_NAME = "MHF";
    private static final int FILTER_MODE_SGSR = 2;
    private static final String GAME_DRIVE = "E:";
    private static final String GAME_EXE = "mhf.exe";
    private static final int REQ_ALL_FILES = 102;
    private static final int REQ_PICK_FOLDER = 103;
    private static final int REQ_STORAGE = 101;
    private static final String TAG = "MHFBoot";
    private Spinner driverSpinner;
    private LinearLayout folderPanel;
    private TextView folderPathView;
    private ProgressBar importProgress;
    private TextView importStatus;
    private SnapdragonProfile profile;
    private Spinner qualitySpinner;
    private File resolvedGameDir;
    private Button selectFolderButton;
    private TextView socInfoView;
    private boolean busy = false;
    private String resolvedDrive = "E:";
    private String extraDrive = "";
    private String resolvedExePath = "E:\\mhf.exe";
    private boolean suppressSpinnerCallbacks = false;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.winlator.cmod.R.layout.activity_boot_splash);
        this.folderPanel = (LinearLayout) findViewById(com.winlator.cmod.R.id.BootFolderPanel);
        this.folderPathView = (TextView) findViewById(com.winlator.cmod.R.id.BootFolderPath);
        this.selectFolderButton = (Button) findViewById(com.winlator.cmod.R.id.BootSelectFolder);
        this.socInfoView = (TextView) findViewById(com.winlator.cmod.R.id.BootSocInfo);
        this.driverSpinner = (Spinner) findViewById(com.winlator.cmod.R.id.BootDriverSpinner);
        this.qualitySpinner = (Spinner) findViewById(com.winlator.cmod.R.id.BootQualitySpinner);
        if (this.selectFolderButton != null) {
            this.selectFolderButton.setOnClickListener(new View.OnClickListener() { // from class: com.winlator.cmod.BootActivity$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    BootActivity.this.lambda$onCreate$0(view);
                }
            });
        }
        setupDeviceSpinners();
        refreshFolderLabel();
        refreshProfileUi();
        Log.i(TAG, "boot start; soc=" + SnapdragonProfile.socModel() + " sdk=" + Build.VERSION.SDK_INT);
        installBootWallpaper();
        startPermissionFlow();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$onCreate$0(View v) {
        openFolderPicker();
    }

    private void setupDeviceSpinners() {
        if (this.driverSpinner != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SnapdragonProfile.DRIVER_LABELS);
            this.driverSpinner.setAdapter((SpinnerAdapter) adapter);
            this.driverSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.cmod.BootActivity.1
                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (BootActivity.this.suppressSpinnerCallbacks || position < 0 || position >= SnapdragonProfile.DRIVER_CHOICES.length) {
                        return;
                    }
                    MhfDevicePrefs.setDriverOverride(BootActivity.this, SnapdragonProfile.DRIVER_CHOICES[position]);
                    BootActivity.this.refreshProfileUi();
                }

                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        if (this.qualitySpinner != null) {
            ArrayAdapter<String> adapter2 = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, SnapdragonProfile.QUALITY_LABELS);
            this.qualitySpinner.setAdapter((SpinnerAdapter) adapter2);
            this.qualitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() { // from class: com.winlator.cmod.BootActivity.2
                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (BootActivity.this.suppressSpinnerCallbacks || position < 0 || position >= SnapdragonProfile.QUALITY_CHOICES.length) {
                        return;
                    }
                    MhfDevicePrefs.setQualityOverride(BootActivity.this, SnapdragonProfile.QUALITY_CHOICES[position]);
                    BootActivity.this.refreshProfileUi();
                }

                @Override // android.widget.AdapterView.OnItemSelectedListener
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
        }
        syncSpinnersFromPrefs();
    }

    private void syncSpinnersFromPrefs() {
        this.suppressSpinnerCallbacks = true;
        try {
            if (this.driverSpinner != null) {
                String cur = MhfDevicePrefs.getDriverOverride(this);
                int idx = indexOf(SnapdragonProfile.DRIVER_CHOICES, cur);
                this.driverSpinner.setSelection(Math.max(0, idx), false);
            }
            if (this.qualitySpinner != null) {
                String cur2 = MhfDevicePrefs.getQualityOverride(this);
                int idx2 = indexOf(SnapdragonProfile.QUALITY_CHOICES, cur2);
                this.qualitySpinner.setSelection(Math.max(0, idx2), false);
            }
        } finally {
            this.suppressSpinnerCallbacks = false;
        }
    }

    private static int indexOf(String[] arr, String value) {
        if (value == null) {
            return 0;
        }
        for (int i = 0; i < arr.length; i++) {
            if (value.equalsIgnoreCase(arr[i])) {
                return i;
            }
        }
        return 0;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void refreshProfileUi() {
        this.profile = SnapdragonProfile.resolve(this);
        if (this.socInfoView == null || this.profile == null) {
            return;
        }
        String screen = this.profile.screenSizeOr(this, displayLandscapeSize());
        String form = SnapdragonProfile.detectFormFactor(this, displayLandscapeSize());
        this.socInfoView.setText("SoC: " + this.profile.soc + " — " + this.profile.marketing + "\nAuto Turnip: " + this.profile.autoDriverId + "\nUsing: " + this.profile.driverId + (this.profile.driverOverridden ? " (manual)" : " (auto)") + " · quality " + this.profile.qualityId + " · " + this.profile.frameRate + " fps · sharp " + this.profile.sharpness + "\nScreen: " + screen + " (" + form + ")");
    }

    private SnapdragonProfile profile() {
        if (this.profile == null) {
            this.profile = SnapdragonProfile.resolve(this);
        }
        return this.profile;
    }

    private void startPermissionFlow() {
        boolean needsLegacy = (ContextCompat.checkSelfPermission(this, "android.permission.READ_EXTERNAL_STORAGE") == 0 && ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") == 0) ? false : true;
        if (needsLegacy) {
            ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.READ_EXTERNAL_STORAGE"}, 101);
        } else {
            continuePermissionFlow();
        }
    }

    private void continuePermissionFlow() {
        if (Build.VERSION.SDK_INT >= 30 && !Environment.isExternalStorageManager()) {
            try {
                Intent intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 102);
                return;
            } catch (Exception e) {
                Log.w(TAG, "all-files settings: " + e);
            }
        }
        proceedToLaunch();
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == 0) {
                continuePermissionFlow();
            } else {
                toast("Storage access is required to open the game folder.");
            }
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 102) {
            continuePermissionFlow();
            return;
        }
        if (requestCode != 103) {
            return;
        }
        if (resultCode != -1 || data == null || data.getData() == null) {
            toast("No folder selected.");
            return;
        }
        Uri uri = data.getData();
        int takeFlags = data.getFlags() & 3;
        if (takeFlags != 0) {
            try {
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
            } catch (SecurityException e) {
                Log.w(TAG, "persistable uri: " + e.getMessage());
            }
        }
        String path = FileUtils.getFilePathFromUri(this, uri);
        if (path == null || path.isEmpty()) {
            toast("That location has no real path. Pick a folder on internal storage.");
            return;
        }
        File picked = new File(path);
        File root = GameFolderPrefs.findGameRoot(picked);
        if (root == null) {
            toast("Need the uncompressed base game folder:\nmhf.exe and game files\n(no DLC required).");
            return;
        }
        GameFolderPrefs.save(this, root, uri);
        Log.i(TAG, "picked in-place folder " + root);
        useInPlace(root);
    }

    private void proceedToLaunch() {
        String saved = GameFolderPrefs.getPath(this);
        File root = GameFolderPrefs.findGameRoot(saved == null ? null : new File(saved));
        if (root != null) {
            Log.i(TAG, "using saved folder in place: " + root);
            useInPlace(root);
        } else {
            refreshFolderLabel();
            showFolderPanel(true);
            this.busy = false;
        }
    }

    private void useInPlace(File dir) {
        if (dir == null) {
            return;
        }
        this.resolvedGameDir = dir;
        this.resolvedDrive = "E:";
        this.extraDrive = "E:" + dir.getAbsolutePath();
        this.resolvedExePath = "E:\\mhf.exe";
        Log.i(TAG, "game root (in place) = " + dir + " drive=" + this.extraDrive);
        runOnUiThread(new Runnable() { // from class: com.winlator.cmod.BootActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                BootActivity.this.lambda$useInPlace$1();
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$useInPlace$1() {
        if (this.busy) {
            return;
        }
        this.busy = true;
        refreshFolderLabel();
        showFolderPanel(true);
        if (this.selectFolderButton != null) {
            this.selectFolderButton.setText(com.winlator.cmod.R.string.change_game_folder);
        }
        continueLaunch();
    }

    private void openFolderPicker() {
        this.busy = false;
        Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
        intent.addFlags(3);
        intent.putExtra("android.provider.extra.SHOW_ADVANCED", true);
        intent.putExtra("android.content.extra.SHOW_ADVANCED", true);
        startActivityForResult(intent, 103);
    }

    private void refreshFolderLabel() {
        if (this.folderPathView == null || this.selectFolderButton == null) {
            return;
        }
        String saved = GameFolderPrefs.getPath(this);
        File root = GameFolderPrefs.findGameRoot(saved == null ? null : new File(saved));
        if (root != null) {
            this.folderPathView.setText("Using in place:\n" + root.getAbsolutePath());
            this.selectFolderButton.setText(com.winlator.cmod.R.string.change_game_folder);
        } else {
            this.folderPathView.setText("Select the uncompressed PC game folder (mhf.exe). Files stay where they are.");
            this.selectFolderButton.setText(com.winlator.cmod.R.string.select_game_folder);
        }
    }

    private void showFolderPanel(boolean visible) {
        if (this.folderPanel == null) {
            return;
        }
        this.folderPanel.setVisibility(0);
        if (this.selectFolderButton != null) {
            this.selectFolderButton.setVisibility(visible ? 0 : 8);
        }
        if (this.folderPathView == null || !visible) {
            return;
        }
        this.folderPathView.setVisibility(0);
    }

    private void continueLaunch() {
        File gameDir = this.resolvedGameDir;
        Log.i(TAG, "game root=" + gameDir + " drive=" + this.resolvedDrive);
        showFolderPanel(false);
        Container container = null;
        showImportUi(false, null);
        SnapdragonProfile p = profile();
        String driverId = p.driverId;
        Log.i(TAG, "driver=" + driverId + " filterMode=2 quality=" + p.qualityId + " fps=" + p.frameRate);
        ContentsManager contentsManager = new ContentsManager(this);
        contentsManager.syncContents();
        try {
            AdrenotoolsManager adrenotools = new AdrenotoolsManager(this);
            File driverDir = new File(getFilesDir(), "contents/adrenotools/" + driverId);
            if (driverDir.isDirectory() && !driverDirLooksValid(driverDir)) {
                deleteRecursive(driverDir);
            }
            if (!adrenotools.extractDriverFromResources(driverId)) {
                Log.w(TAG, "driver not extractable from resources: " + driverId);
            }
        } catch (Throwable t) {
            Log.w(TAG, "driver extract skipped: " + t);
        }
        ContainerManager manager = new ContainerManager(this);
        if (!manager.getContainers().isEmpty()) {
            container = manager.getContainers().get(0);
        }
        Container existing = container;
        if (existing != null) {
            boolean changed = pinGraphicsDriver(existing, driverId);
            if (changed) {
                existing.saveData();
                Log.i(TAG, "pinned graphics driver -> " + driverId);
            }
            Log.i(TAG, "reusing container id=" + existing.id + " driver=" + driverId);
            finishWith(manager, existing);
            return;
        }
        if (!WineRuntimeGuard.isBundledMainInstalled(this)) {
            bootstrapRuntimeThenCreate(manager, contentsManager, driverId);
        } else {
            createContainerAndLaunch(manager, contentsManager, driverId);
        }
    }

    private void bootstrapRuntimeThenCreate(final ContainerManager manager, final ContentsManager contentsManager, final String driverId) {
        Log.i(TAG, "bundled runtime missing -> bootstrapping from APK assets");
        showImportUi(true, "Installing game runtime… (first run only)");
        try {
            ImageFsInstaller.installFromAssetsSilently(this, new ImageFsInstaller.InstallationProgressListener() { // from class: com.winlator.cmod.BootActivity.3
                @Override // com.winlator.cmod.xenvironment.ImageFsInstaller.InstallationProgressListener
                public void onProgress(int progress) {
                    BootActivity.this.publishRuntimeProgress(progress);
                }

                @Override // com.winlator.cmod.xenvironment.ImageFsInstaller.InstallationProgressListener
                public void onFinished(boolean success) {
                    BootActivity.this.showImportUi(false, null);
                    if (!success || !WineRuntimeGuard.isBundledMainInstalled(BootActivity.this)) {
                        Log.e(BootActivity.TAG, "runtime bootstrap failed");
                        BootActivity.this.toast("The game runtime could not be installed from the APK.");
                        BootActivity.this.busy = false;
                    } else {
                        Log.i(BootActivity.TAG, "runtime bootstrap complete");
                        BootActivity.this.createContainerAndLaunch(manager, contentsManager, driverId);
                    }
                }
            });
        } catch (Throwable t) {
            Log.e(TAG, "runtime bootstrap blew up", t);
            showImportUi(false, null);
            toast("The game runtime could not be installed: " + t.getMessage());
            this.busy = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void createContainerAndLaunch(final ContainerManager manager, ContentsManager contentsManager, String driverId) {
        String runtime = resolveRuntime(contentsManager);
        if (runtime == null) {
            toast("Bundled Wine/Proton runtime is missing from the APK.");
            this.busy = false;
            return;
        }
        try {
            JSONObject data = buildContainerData(manager, contentsManager, runtime, driverId);
            manager.createContainerAsync(data, contentsManager, new Callback() { // from class: com.winlator.cmod.BootActivity$$ExternalSyntheticLambda0
                @Override // com.winlator.cmod.core.Callback
                public final void call(Object obj) {
                    BootActivity.this.lambda$createContainerAndLaunch$2(manager, (Container) obj);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "container build failed", e);
            toast("Could not prepare the container: " + e.getMessage());
            this.busy = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$createContainerAndLaunch$2(ContainerManager manager, Container created) {
        if (created == null) {
            toast("Could not create the game container.");
            this.busy = false;
        } else {
            finishWith(manager, created);
        }
    }

    private JSONObject buildContainerData(ContainerManager manager, ContentsManager contentsManager, String runtime, String driverId) throws Exception {
        WineInfo wineInfo = WineInfo.fromIdentifier(this, contentsManager, runtime);
        boolean arm64ec = wineInfo.isArm64EC();
        SnapdragonProfile p = profile();
        JSONObject data = new JSONObject();
        data.put(HintConstants.AUTOFILL_HINT_NAME, CONTAINER_NAME);
        data.put("screenSize", p.screenSizeOr(this, displayLandscapeSize()));
        data.put("graphicsDriver", Container.DEFAULT_GRAPHICS_DRIVER);
        data.put("graphicsDriverConfig", putConfigValue(Container.DEFAULT_GRAPHICSDRIVERCONFIG, "version", driverId));
        data.put("rendererNative", false);
        data.put("rendererPresentMode", "fifo");
        data.put("rendererDriverId", driverId);
        data.put("rendererFilterMode", 2);
        data.put("dxwrapper", Container.DEFAULT_DXWRAPPER);
        data.put("dxwrapperConfig", p.dxwrapperConfig(Container.DEFAULT_DXWRAPPERCONFIG));
        data.put("audioDriver", Container.DEFAULT_AUDIO_DRIVER);
        data.put("emulator", arm64ec ? Container.DEFAULT_EMULATOR : "Box64");
        data.put("wincomponents", Container.DEFAULT_WINCOMPONENTS);
        data.put("drives", drivesString());
        data.put("box64Version", "0.4.2");
        data.put("box64Preset", p.box64Preset != null ? p.box64Preset : "PERFORMANCE");
        data.put("fexcoreVersion", DefaultVersion.FEXCORE);
        data.put("fexcorePreset", p.fexPreset != null ? p.fexPreset : "INTERMEDIATE");
        data.put("wineVersion", runtime);
        data.put("envVars", p.buildEnvVars());
        OpenGLDriverDefaults.initialize(this, data);
        data.put("graphicsDriverConfig", putConfigValue(data.optString("graphicsDriverConfig", Container.DEFAULT_GRAPHICSDRIVERCONFIG), "version", driverId));
        data.put("rendererDriverId", driverId);
        return data;
    }

    private static boolean pinGraphicsDriver(Container container, String driverId) {
        boolean changed = false;
        String prevRenderer = container.getRendererDriverId();
        if (prevRenderer == null || !driverId.equals(prevRenderer)) {
            container.setRendererDriverId(driverId);
            changed = true;
        }
        String config = container.getGraphicsDriverConfig();
        String prevVersion = configValue(config, "version");
        if (!driverId.equals(prevVersion)) {
            config = putConfigValue(config, "version", driverId);
            changed = true;
        }
        if (!"fifo".equals(configValue(config, "presentMode"))) {
            config = putConfigValue(config, "presentMode", "fifo");
            changed = true;
        }
        if (changed) {
            container.setGraphicsDriverConfig(config);
        }
        return changed;
    }

    private static boolean driverDirLooksValid(File driverDir) {
        File meta = new File(driverDir, "meta.json");
        if (!meta.isFile()) {
            return false;
        }
        String libraryName = null;
        try {
            String json = new String(Files.readAllBytes(meta.toPath()), StandardCharsets.UTF_8);
            int key = json.indexOf("\"libraryName\"");
            if (key >= 0) {
                int colon = json.indexOf(58, key);
                int q1 = json.indexOf(34, colon + 1);
                int q2 = json.indexOf(34, q1 + 1);
                if (q1 >= 0 && q2 > q1) {
                    libraryName = json.substring(q1 + 1, q2);
                }
            }
        } catch (Throwable th) {
        }
        if (libraryName == null || libraryName.isEmpty() || !new File(driverDir, libraryName).isFile()) {
            return new File(driverDir, "libvulkan_freedreno.so").isFile() || new File(driverDir, "vulkan.ad07xx.so").isFile() || new File(driverDir, "vulkan.ad07XX.so").isFile();
        }
        return true;
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

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) {
                deleteRecursive(child);
            }
        }
        file.delete();
    }

    private String buildEnvVars() {
        return profile().buildEnvVars();
    }

    private String resolveRuntime(ContentsManager contentsManager) {
        if (WineRuntimeGuard.isBundledMainInstalled(this)) {
            return WineInfo.MAIN_WINE_VERSION.identifier();
        }
        for (ContentProfile.ContentType type : ContentProfile.ContentType.values()) {
            Iterator<ContentProfile> it = contentsManager.getInstalledProfiles(type).iterator();
            if (it.hasNext()) {
                ContentProfile profile = it.next();
                return ContentsManager.getEntryName(profile);
            }
        }
        return null;
    }

    private void finishWith(ContainerManager manager, Container container) {
        try {
            SnapdragonProfile p = profile();
            container.setDrives(drivesString());
            container.setRendererNative(false);
            container.setRendererFilterMode(2);
            container.putExtra("graphicsSharpness", String.valueOf(p.sharpness));
            container.putExtra("graphicsFilterMode", String.valueOf(2));
            container.putExtra("graphicsPostFXMode", "1");
            container.setRendererDriverId(p.driverId);
            pinGraphicsDriver(container, p.driverId);
            container.setFullscreenStretched(true);
            container.setScreenSize(p.screenSizeOr(this, displayLandscapeSize()));
            container.setBox64Preset(p.box64Preset != null ? p.box64Preset : "PERFORMANCE");
            container.setFEXCorePreset(p.fexPreset != null ? p.fexPreset : "INTERMEDIATE");
            container.setDXWrapperConfig(p.dxwrapperConfig(container.getDXWrapperConfig()));
            container.setEnvVars(p.buildEnvVars());
            container.saveData();
        } catch (Throwable t) {
            Log.w(TAG, "container preset re-apply skipped: " + t);
        }
        File desktopFile = writeShortcut(container);
        if (desktopFile == null) {
            toast("Could not create the game shortcut.");
            this.busy = false;
            return;
        }
        Log.i(TAG, "launching container=" + container.id + " shortcut=" + desktopFile + " driver=" + profile().driverId + " quality=" + profile().qualityId + " screen=" + profile().screenSizeOr(this, displayLandscapeSize()) + " form=" + SnapdragonProfile.detectFormFactor(this, displayLandscapeSize()));
        Intent intent = new Intent(this, (Class<?>) XServerDisplayActivity.class);
        intent.putExtra("container_id", container.id);
        intent.putExtra("shortcut_path", desktopFile.getAbsolutePath());
        intent.putExtra("shortcut_name", CONTAINER_NAME);
        intent.putExtra("mhf_boot_cover_ms", 10000L);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    private File writeShortcut(Container container) {
        File desktopDir = container.getDesktopDir();
        if (!desktopDir.exists() && !desktopDir.mkdirs()) {
            return null;
        }
        File desktopFile = new File(desktopDir, "MHF.desktop");
        String body = "[Desktop Entry]\nName=MHF\nExec=" + this.resolvedExePath + "\n\n[Extra Data]\nfullscreenStretched=1\n";
        try {
            OutputStreamWriter w = new OutputStreamWriter(new FileOutputStream(desktopFile), StandardCharsets.UTF_8);
            try {
                w.write(body);
                w.close();
                return desktopFile;
            } finally {
            }
        } catch (Exception e) {
            Log.e(TAG, "shortcut write failed", e);
            return null;
        }
    }

    private String displayLandscapeSize() {
        try {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(dm);
            int longSide = Math.max(dm.widthPixels, dm.heightPixels);
            int shortSide = Math.min(dm.widthPixels, dm.heightPixels);
            return longSide + "x" + shortSide;
        } catch (Throwable t) {
            Log.w(TAG, "display size probe failed, falling back to 1280x720", t);
            return Container.DEFAULT_SCREEN_SIZE;
        }
    }

    private String drivesString() {
        String drives = Container.DEFAULT_DRIVES;
        return (this.extraDrive == null || this.extraDrive.isEmpty() || drives.contains(this.extraDrive)) ? drives : drives + this.extraDrive;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void showImportUi(boolean visible, String message) {
        try {
            if (this.importStatus == null) {
                this.importStatus = (TextView) findViewById(com.winlator.cmod.R.id.BootImportStatus);
            }
            if (this.importProgress == null) {
                this.importProgress = (ProgressBar) findViewById(com.winlator.cmod.R.id.BootImportProgress);
            }
            if (this.importStatus != null) {
                if (message != null) {
                    this.importStatus.setText(message);
                }
                this.importStatus.setVisibility(visible ? 0 : 8);
            }
            if (this.importProgress != null) {
                this.importProgress.setIndeterminate(visible);
                if (visible) {
                    this.importProgress.setProgress(0);
                }
                this.importProgress.setVisibility(visible ? 0 : 8);
            }
            if (visible) {
                getWindow().addFlags(128);
            } else {
                getWindow().clearFlags(128);
            }
        } catch (Throwable t) {
            Log.w(TAG, "import ui: " + t);
        }
    }

    private void publishCopyProgress(int progress) {
        try {
            if (this.importProgress == null) {
                this.importProgress = (ProgressBar) findViewById(com.winlator.cmod.R.id.BootImportProgress);
            }
            if (this.importProgress != null) {
                this.importProgress.setIndeterminate(false);
                this.importProgress.setProgress(progress);
            }
        } catch (Throwable th) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void publishRuntimeProgress(int progress) {
        try {
            if (this.importProgress == null) {
                this.importProgress = (ProgressBar) findViewById(com.winlator.cmod.R.id.BootImportProgress);
            }
            if (this.importStatus == null) {
                this.importStatus = (TextView) findViewById(com.winlator.cmod.R.id.BootImportStatus);
            }
            if (this.importProgress != null) {
                this.importProgress.setIndeterminate(false);
                this.importProgress.setProgress(progress);
            }
            if (this.importStatus != null) {
                this.importStatus.setText("Installing game runtime… " + progress + "%");
            }
        } catch (Throwable th) {
        }
    }

    private void fail(final String message) {
        runOnUiThread(new Runnable() { // from class: com.winlator.cmod.BootActivity$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                BootActivity.this.lambda$fail$3(message);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public /* synthetic */ void lambda$fail$3(String message) {
        toast(message);
        this.busy = false;
        showFolderPanel(true);
        refreshFolderLabel();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public void toast(String message) {
        Log.w(TAG, message);
        Toast.makeText(this, message, 1).show();
    }

    /** Copy the launcher boot cover into Wine's wallpaper slot
     *  so the user sees it on the black Wine desktop while MHF boots. */
    private void installBootWallpaper() {
        try {
            java.io.File wallpaperFile = WineThemeManager.getUserWallpaperFile(this);
            if (wallpaperFile == null) return;
            java.io.File parent = wallpaperFile.getParentFile();
            if (parent != null && !parent.isDirectory()) parent.mkdirs();

            Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.drawable.mhf_boot_cover);
            if (bmp == null) {
                Log.w(TAG, "boot cover decode returned null");
                return;
            }
            try (FileOutputStream out = new FileOutputStream(wallpaperFile)) {
                bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            }
            bmp.recycle();
            Log.i(TAG, "Boot wallpaper written: " + wallpaperFile.getAbsolutePath());
        } catch (Throwable t) {
            Log.w(TAG, "installBootWallpaper failed: " + t);
        }
    }

}
