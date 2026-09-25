package com.winlator.cmod.xenvironment;

import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import androidx.appcompat.app.AppCompatActivity;
import com.winlator.cmod.R;
import com.winlator.cmod.MainActivity;
import com.winlator.cmod.SettingsFragment;
import com.winlator.cmod.container.Container;
import com.winlator.cmod.container.ContainerManager;
import com.winlator.cmod.contents.AdrenotoolsManager;
import com.winlator.cmod.core.AppUtils;
import com.winlator.cmod.core.DownloadProgressDialog;
import com.winlator.cmod.core.FileUtils;
import com.winlator.cmod.core.OnExtractFileListener;
import com.winlator.cmod.core.TarCompressorUtils;
import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/* JADX INFO: loaded from: classes12.dex */
public abstract class ImageFsInstaller {
    public static final byte LATEST_VERSION = 22;

    public interface InstallationProgressListener {
        void onFinished(boolean z);

        void onProgress(int i);
    }

    public interface onInstallationFinish {
        void call();
    }

    private static void resetContainerImgVersions(Context context) {
        ContainerManager manager = new ContainerManager(context);
        for (Container container : manager.getContainers()) {
            container.putExtra("imgVersion", null);
            container.saveData();
        }
    }

    public static boolean installWineArchive(Context context, String version, File archiveFile) {
        File rootDir = ImageFs.find(context).getRootDir();
        File outFile = new File(rootDir, "opt/" + version);
        FileUtils.delete(outFile);
        outFile.mkdirs();
        boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, archiveFile, outFile);
        if (!success) {
            success = TarCompressorUtils.extract(TarCompressorUtils.Type.XZ, archiveFile, outFile);
        }
        if (!success) {
            FileUtils.delete(outFile);
        }
        return success;
    }

    public static void installWineFromAssets(final DownloadProgressDialog dialog, final AppCompatActivity activity) {
        String[] versions = activity.getResources().getStringArray(R.array.wine_entries);
        File rootDir = ImageFs.find(activity).getRootDir();
        if (dialog != null) {
            activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    dialog.setMessage(R.string.installing_wine_files);
                }
            });
        }
        int length = versions.length;
        int i = 0;
        while (i < length) {
            String version = versions[i];
            File outFile = new File(rootDir, "opt/" + version);
            outFile.mkdirs();
            final long contentLength = (long) (FileUtils.getSize(activity, version + ".tar.zst") * 4.5454545f);
            final AtomicLong totalSizeRef = new AtomicLong();
            TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, activity, version + ".tar.zst", outFile, new OnExtractFileListener() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda1
                @Override // com.winlator.cmod.core.OnExtractFileListener
                public final File onExtractFile(File file, long j) {
                    return ImageFsInstaller.lambda$installWineFromAssets$2(totalSizeRef, contentLength, dialog, activity, file, j);
                }
            });
            i++;
            versions = versions;
        }
    }

    static /* synthetic */ File lambda$installWineFromAssets$2(AtomicLong totalSizeRef, long contentLength, final DownloadProgressDialog dialog, AppCompatActivity activity, File file, long size) {
        if (size > 0) {
            long totalSize = totalSizeRef.addAndGet(size);
            final int progress = (int) ((totalSize / contentLength) * 100.0f);
            if (dialog != null) {
                activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda14
                    @Override // java.lang.Runnable
                    public final void run() {
                        dialog.setProgress(progress);
                    }
                });
            }
        }
        return file;
    }

    public static void installDriversFromAssets(final DownloadProgressDialog dialog, final AppCompatActivity activity) {
        if (dialog != null) {
            activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    dialog.setMessage(R.string.installing_drivers_files);
                }
            });
        }
        AdrenotoolsManager adrenotoolsManager = new AdrenotoolsManager(activity);
        String[] adrenotoolsAssetDrivers = activity.getResources().getStringArray(R.array.wrapper_graphics_driver_version_entries);
        for (String driver : adrenotoolsAssetDrivers) {
            final long contentLength = (long) (FileUtils.getSize(activity, adrenotoolsManager.getAssetPath(driver)) * 4.5454545f);
            final AtomicLong totalSizeRef = new AtomicLong();
            adrenotoolsManager.extractDriverFromResources(driver, new OnExtractFileListener() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda13
                @Override // com.winlator.cmod.core.OnExtractFileListener
                public final File onExtractFile(File file, long j) {
                    return ImageFsInstaller.lambda$installDriversFromAssets$5(totalSizeRef, contentLength, dialog, activity, file, j);
                }
            });
        }
    }

    static /* synthetic */ File lambda$installDriversFromAssets$5(AtomicLong totalSizeRef, long contentLength, final DownloadProgressDialog dialog, AppCompatActivity activity, File file, long size) {
        if (size > 0) {
            long totalSize = totalSizeRef.addAndGet(size);
            final int progress = (int) ((totalSize / contentLength) * 100.0f);
            if (dialog != null) {
                activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        dialog.setProgress(progress);
                    }
                });
            }
        }
        return file;
    }

    public static void installFromAssets(final MainActivity activity, final onInstallationFinish callback) {
        AppUtils.keepScreenOn(activity);
        final ImageFs imageFs = ImageFs.find(activity);
        final File rootDir = imageFs.getRootDir();
        SettingsFragment.resetEmulatorsVersion(activity);
        final DownloadProgressDialog dialog = new DownloadProgressDialog(activity);
        dialog.show(R.string.installing_system_files);
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ImageFsInstaller.lambda$installFromAssets$9(rootDir, activity, dialog, imageFs, callback);
            }
        });
    }

    static /* synthetic */ void lambda$installFromAssets$9(File rootDir, final MainActivity activity, final DownloadProgressDialog dialog, ImageFs imageFs, final onInstallationFinish callback) {
        clearRootDir(rootDir);
        final long contentLength = (long) (FileUtils.getSize(activity, "imagefs.tar.zst") * 4.5454545f);
        final AtomicLong totalSizeRef = new AtomicLong();
        boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, activity, "imagefs.tar.zst", rootDir, new OnExtractFileListener() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda2
            @Override // com.winlator.cmod.core.OnExtractFileListener
            public final File onExtractFile(File file, long j) {
                return ImageFsInstaller.lambda$installFromAssets$7(totalSizeRef, contentLength, activity, dialog, file, j);
            }
        });
        if (success) {
            installWineFromAssets(dialog, activity);
            installDriversFromAssets(dialog, activity);
            imageFs.createImgVersionFile(22);
            FileUtils.symlink("libSDL2-2.0.so", new File(imageFs.getLibDir(), "libSDL2-2.0.so.0").getAbsolutePath());
            resetContainerImgVersions(activity);
        } else {
            AppUtils.showToast(activity, R.string.unable_to_install_system_files);
        }
        dialog.closeOnUiThread();
        activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                ImageFsInstaller.lambda$installFromAssets$8(callback);
            }
        });
    }

    static /* synthetic */ File lambda$installFromAssets$7(AtomicLong totalSizeRef, long contentLength, MainActivity activity, final DownloadProgressDialog dialog, File file, long size) {
        if (size > 0) {
            long totalSize = totalSizeRef.addAndGet(size);
            final int progress = (int) ((totalSize / contentLength) * 100.0f);
            activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda6
                @Override // java.lang.Runnable
                public final void run() {
                    dialog.setProgress(progress);
                }
            });
        }
        return file;
    }

    static /* synthetic */ void lambda$installFromAssets$8(onInstallationFinish callback) {
        if (callback != null) {
            callback.call();
        }
    }

    public static void installFromAssetsSilently(final AppCompatActivity activity, final InstallationProgressListener listener) {
        AppUtils.keepScreenOn(activity);
        final ImageFs imageFs = ImageFs.find(activity);
        final File rootDir = imageFs.getRootDir();
        SettingsFragment.resetEmulatorsVersion(activity);
        Executors.newSingleThreadExecutor().execute(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                ImageFsInstaller.lambda$installFromAssetsSilently$15(rootDir, activity, listener, imageFs);
            }
        });
    }

    static /* synthetic */ void lambda$installFromAssetsSilently$15(File rootDir, final AppCompatActivity activity, final InstallationProgressListener listener, ImageFs imageFs) {
        Process.setThreadPriority(10);
        clearRootDir(rootDir);
        final long contentLength = (long) (FileUtils.getSize(activity, "imagefs.tar.zst") * 4.5454545f);
        final AtomicLong totalSizeRef = new AtomicLong();
        final AtomicLong lastProgressDispatch = new AtomicLong(0L);
        final AtomicInteger lastPublishedProgress = new AtomicInteger(-1);
        final boolean success = TarCompressorUtils.extract(TarCompressorUtils.Type.ZSTD, activity, "imagefs.tar.zst", rootDir, new OnExtractFileListener() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda8
            @Override // com.winlator.cmod.core.OnExtractFileListener
            public final File onExtractFile(File file, long j) {
                return ImageFsInstaller.lambda$installFromAssetsSilently$11(contentLength, totalSizeRef, lastPublishedProgress, lastProgressDispatch, listener, activity, file, j);
            }
        });
        if (success) {
            installWineFromAssets(null, activity);
            if (listener != null) {
                activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda9
                    @Override // java.lang.Runnable
                    public final void run() {
                        listener.onProgress(88);
                    }
                });
            }
            installDriversFromAssets(null, activity);
            if (listener != null) {
                activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda10
                    @Override // java.lang.Runnable
                    public final void run() {
                        listener.onProgress(96);
                    }
                });
            }
            imageFs.createImgVersionFile(22);
            FileUtils.symlink("libSDL2-2.0.so", new File(imageFs.getLibDir(), "libSDL2-2.0.so.0").getAbsolutePath());
            resetContainerImgVersions(activity);
        } else {
            AppUtils.showToast(activity, R.string.unable_to_install_system_files);
        }
        activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                ImageFsInstaller.lambda$installFromAssetsSilently$14(listener, success);
            }
        });
    }

    static /* synthetic */ File lambda$installFromAssetsSilently$11(long contentLength, AtomicLong totalSizeRef, AtomicInteger lastPublishedProgress, AtomicLong lastProgressDispatch, final InstallationProgressListener listener, AppCompatActivity activity, File file, long size) {
        if (size > 0 && contentLength > 0) {
            long totalSize = totalSizeRef.addAndGet(size);
            final int progress = Math.min(75, (int) ((totalSize / contentLength) * 75.0f));
            long now = SystemClock.uptimeMillis();
            int previous = lastPublishedProgress.get();
            if (progress > previous && ((progress >= 75 || progress - previous >= 2) && now - lastProgressDispatch.get() >= 120)) {
                lastProgressDispatch.set(now);
                lastPublishedProgress.set(progress);
                if (listener != null) {
                    activity.runOnUiThread(new Runnable() { // from class: com.winlator.cmod.xenvironment.ImageFsInstaller$$ExternalSyntheticLambda5
                        @Override // java.lang.Runnable
                        public final void run() {
                            listener.onProgress(progress);
                        }
                    });
                }
            }
        }
        return file;
    }

    static /* synthetic */ void lambda$installFromAssetsSilently$14(InstallationProgressListener listener, boolean completed) {
        if (listener != null && completed) {
            listener.onProgress(100);
        }
        if (listener != null) {
            listener.onFinished(completed);
        }
    }

    public static boolean installIfNeeded(MainActivity activity, onInstallationFinish callback) {
        ImageFs imageFs = ImageFs.find(activity);
        if (!imageFs.isValid() || imageFs.getVersion() < 22) {
            installFromAssets(activity, callback);
            return true;
        }
        return false;
    }

    private static void clearOptDir(File optDir) {
        File[] files = optDir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (!file.getName().equals("installed-wine")) {
                    FileUtils.delete(file);
                }
            }
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:13:0x0025  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    private static void clearRootDir(File r6) {
        if (r6 == null) return;
        if (!r6.exists() || !r6.isDirectory()) {
            r6.mkdirs();
            return;
        }
        File[] children = r6.listFiles();
        if (children == null) return;
        for (File child : children) {
            FileUtils.delete(child);
        }
    }
}
