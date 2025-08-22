package com.offsec.nethunter.Executor;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import com.offsec.nethunter.AppNavHomeActivity;
import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.CheckForRoot;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.SharePrefTag;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CopyBootFilesExecutor {
    public static final String TAG = "CopyBootFilesExecutor";
    private final File sdCardDir;
    private final File scriptsDir;
    private final File etcDir;
    private final String buildTime;
    private Boolean shouldRun;
    private final WeakReference<ProgressDialog> progressDialogRef;
    private CopyBootFilesExecutorListener listener;
    private static final String result = "";
    private final SharedPreferences prefs;
    private final ShellExecuter exe = new ShellExecuter();
    private final WeakReference<Context> context;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public CopyBootFilesExecutor(Context context, Activity activity, ProgressDialog progressDialog) {
        this.context = new WeakReference<>(context);
        this.progressDialogRef = new WeakReference<>(progressDialog);
        this.sdCardDir = new File(NhPaths.APP_SD_FILES_PATH);
        this.scriptsDir = new File(NhPaths.APP_SCRIPTS_PATH);
        this.etcDir = new File(NhPaths.APP_INITD_PATH);
        this.prefs = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss a zzz", Locale.US);
        this.buildTime = sdf.format(BuildConfig.BUILD_TIME);
        this.shouldRun = true;
    }

    public void execute() {
        mainHandler.post(this::onPreExecute);
        executorService.submit(() -> {
            String result = doInBackground();
            mainHandler.post(() -> onPostExecute(result));
        });
    }

    private void onPreExecute() {
        if (prefs.getInt(SharePrefTag.VERSION_CODE_TAG, 0) != BuildConfig.VERSION_CODE ||
                !prefs.getString(TAG, buildTime).equals(buildTime) ||
                !sdCardDir.isDirectory() ||
                !scriptsDir.isDirectory() ||
                !etcDir.isDirectory()) {
            Log.d(TAG, "正在复制新文件");
            AlertDialog progressDialog = progressDialogRef.get();
            if (progressDialog != null) {
                TextView titleView = progressDialog.findViewById(R.id.progress_title);
                TextView messageView = progressDialog.findViewById(R.id.progress_message);
                if (titleView != null) {
                    titleView.setText("检测到新的应用版本: ");
                }
                if (messageView != null) {
                    messageView.setText("正在复制新文件...");
                }
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        } else {
            Log.d(TAG, "文件未复制");
            shouldRun = false;
        }
        if (listener != null) {
            listener.onPrepare();
        }
    }

    private String doInBackground() {
        if (shouldRun) {
            if (!CheckForRoot.isRoot()) {
                prefs.edit().putBoolean(AppNavHomeActivity.CHROOT_INSTALLED_TAG, false).apply();
                return "需要Root权限！！";
            }
            Log.d(TAG, "正在复制文件....");
            updateProgress("正在更新应用文件. (init.d 和 filesDir). ");
            assetsToFiles(NhPaths.APP_PATH, "", "data");
            updateProgress("正在更新SD卡文件. (nh_files). ");
            assetsToFiles(NhPaths.SD_PATH, "", "sdcard");
            updateProgress("正在为新文件修复权限");
            exe.RunAsRoot(new String[]{"chmod -R 700 " + NhPaths.APP_SCRIPTS_PATH + "/*", "chmod -R 700 " + NhPaths.APP_INITD_PATH + "/*"});
            updateProgress("检查/data是否加密....");
            CheckEncrypted();
            updateProgress("检查bootkali符号链接....");
            Symlink("bootkali");
            Symlink("bootkali_bash");
            Symlink("bootkali_init");
            Symlink("bootkali_login");
            Symlink("killkali");
            disableMagiskNotification();
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(TAG, buildTime);
            ed.putInt(SharePrefTag.VERSION_CODE_TAG, BuildConfig.VERSION_CODE);
            ed.apply();
            updateProgress("检查chroot....");
            String command = "if [ -d " + NhPaths.CHROOT_PATH() + " ];then echo 1; fi";
            final String _res = exe.RunAsRootOutput(command);
            if (_res.equals("1")) {
                ed.putBoolean(AppNavHomeActivity.CHROOT_INSTALLED_TAG, true).apply();
                updateProgress("找到Chroot！");
                updateProgress(exe.RunAsRootOutput(NhPaths.BUSYBOX + " mount -o remount,suid /data && chmod +s " +
                        NhPaths.CHROOT_PATH() + "/usr/bin/sudo" +
                        " && echo \"初始设置完成！\""));
            } else {
                updateProgress("未找到Chroot, 请在Chroot管理器中安装");
            }
            updateProgress("正在安装附加应用....");
            String ApkCachePath = NhPaths.APP_SD_FILES_PATH + "/cache/apk/";
            ArrayList<String> filenames = FetchFiles(ApkCachePath);
            for (String object : filenames) {
                if (object.contains(".apk")) {
                    String apk = ApkCachePath + object;
                    ShellExecuter install = new ShellExecuter();
                    install.RunAsRoot(new String[]{"mv " + apk + " /data/local/tmp/ && pm install /data/local/tmp/" + object + " && rm -f /data/local/tmp/" + object});
                }
            }
        }
        return "";
    }

    private void onPostExecute(String result) {
        ProgressDialog progressDialog = progressDialogRef.get();
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
        if (listener != null) {
            listener.onFinished(result);
        }
    }

    private void updateProgress(String message) {
        mainHandler.post(() -> {
            AlertDialog progressDialog = progressDialogRef.get();
            if (progressDialog != null) {
                TextView progressMessage = progressDialog.findViewById(R.id.progress_message);
                if (progressMessage != null) {
                    progressMessage.setText(message);
                }
            }
        });
    }

    public void setListener(CopyBootFilesExecutorListener listener) {
        this.listener = listener;
    }

    public interface CopyBootFilesExecutorListener {
        void onPrepare();

        void onFinished(String result);
    }

    private Boolean pathIsAllowed(String path, String copyType) {
        // 从不复制 images、sounds 或 webkit
        if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit")) {
            if (copyType.equals("sdcard")) {
                if (path.isEmpty()) {
                    return true;
                } else return path.startsWith(NhPaths.NH_SD_FOLDER_NAME);
            }
            if (copyType.equals("data")) {
                if (path.isEmpty()) {
                    return true;
                } else if (path.startsWith("scripts")) {
                    return true;
                } else if (path.startsWith("wallpapers")) {
                    return true;
                } else return path.startsWith("etc");
            }
            return false;
        }
        return false;
    }

    // 现在这只复制文件夹: scripts、etc、wallpapers 到 /data/data...
    private void assetsToFiles(String TARGET_BASE_PATH, String path, String copyType) {
        AssetManager assetManager = context.get().getAssets();
        String[] assets;
        try {
            // Log.i("tag", "assetsTo" + copyType +"() "+path);
            assets = assetManager.list(path);
            assert assets != null;
            if (assets.length == 0) {
                copyFile(TARGET_BASE_PATH, path);
            } else {
                String fullPath = TARGET_BASE_PATH + "/" + path;
                // Log.i("tag", "path="+fullPath)

                File dir = new File(fullPath);
                if (!dir.exists() && pathIsAllowed(path, copyType) && !dir.mkdirs()) {
                    ShellExecuter create = new ShellExecuter();
                    create.RunAsRoot(new String[]{"mkdir " + fullPath});
                    if (!dir.exists()) {
                        Log.i(TAG, "无法创建目录 " + fullPath);
                    }
                }

                for (String asset : assets) {
                    String p;
                    if (path.isEmpty()) {
                        p = "";
                    } else {
                        p = path + "/";
                    }
                    if (pathIsAllowed(path, copyType)) {
                        assetsToFiles(TARGET_BASE_PATH, p + asset, copyType);
                    }
                }

            }
        } catch (IOException ex) {
            Log.e(TAG, "I/O 异常", ex);
        }
    }

    private void copyFile(String TARGET_BASE_PATH, String filename) {
        if (filename.matches("^.*/kaliservices$|^.*/runonboot_services$")) {
            return;
        }
        AssetManager assetManager = context.get().getAssets();
        InputStream in;
        OutputStream out;
        String newFileName = null;
        try {
            // Log.i("tag", "copyFile() "+filename);
            in = assetManager.open(filename);
            newFileName = TARGET_BASE_PATH + "/" + filename;
            /* 如果文件后缀是 -arm64 或 -armhf, 则在复制文件前重命名文件名. */
            out = new FileOutputStream(renameAssetIfneeded(newFileName));
            byte[] buffer = new byte[8092];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            out.flush();
            out.close();
        } catch (Exception e) {
            Log.e(TAG, "copyFile() 中的异常: " + newFileName);
            Log.e(TAG, "copyFile() 中的异常: " + e);
            Log.e(TAG, "接下来尝试以 root 权限复制");
            // 尝试以 root 权限运行
            ShellExecuter copy = new ShellExecuter();
            copy.RunAsRoot(new String[]{"cp " + filename + " " + TARGET_BASE_PATH});
        }

    }

    // 检查 bootkali 的符号链接
    // https://stackoverflow.com/questions/813710/java-1-6-determine-symbolic-links/813730#813730
    private boolean isSymlink(File file) throws IOException {
        if (file == null)
            throw new NullPointerException("文件不能为空");
        File canon;
        if (file.getParent() == null) {
            canon = file;
        } else {
            File canonDir = Objects.requireNonNull(file.getParentFile()).getCanonicalFile();
            canon = new File(canonDir, file.getName());
        }
        return !canon.getCanonicalFile().equals(canon.getAbsoluteFile());
    }

    private void MakeSYSWriteable() {
        Log.d(TAG, "使 /system 可写以创建符号链接");
        exe.RunAsRoot(new String[]{"if [ \"$(getprop ro.build.system_root_image)\" == \"true\" ]; then export SYSTEM=/; else export SYSTEM=/system;fi;mount -o rw,remount,rw $SYSTEM"});
    }

    private void MakeSYSReadOnly() {
        Log.d(TAG, "使 /system 只读以创建符号链接");
        exe.RunAsRoot(new String[]{"if [ \"$(getprop ro.build.system_root_image)\" == \"true\" ]; then export SYSTEM=/; else export SYSTEM=/system;fi;mount -o ro,remount,ro $SYSTEM"});
    }

    private void CheckEncrypted() {
        Log.d(TAG, "检查 /data 是否加密...");
        String encrypted = exe.RunAsRootOutput("getprop ro.crypto.state");
        Log.d(TAG, "/data 是 " + encrypted);
        if (encrypted.equals("encrypted")) {
            Log.d(TAG, "修复 chroot 中的 pam.d 和 inet");
            exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd sed -i \"s/pam_keyinit.so/pam_keyinit.so #/\" /etc/pam.d/*"});
            exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd echo 'APT::Sandbox::User \"root\";' > /etc/apt/apt.conf.d/01-android-nosandbox"});
            exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd groupadd -g 3003 aid_inet;usermod -G nogroup -g aid_inet _apt"});
        }
    }

    private void Symlink(String filename) {
        File checkfile = new File("/system/bin/" + filename);
        Log.d(TAG, "检查 " + filename + " 符号链接....");
        if (!checkfile.exists()) {
            Log.d(TAG, "正在创建符号链接 " + filename);
            Log.d(TAG, "命令输出: ln -s " + NhPaths.APP_SCRIPTS_PATH + "/" + filename + " /system/bin/" + filename);
            MakeSYSWriteable();
            exe.RunAsRoot(new String[]{"ln -s " + NhPaths.APP_SCRIPTS_PATH + "/" + filename + " /system/bin/" + filename});
            MakeSYSReadOnly();
        }
    }

    // 从目录获取文件列表
    private ArrayList<String> FetchFiles(String folder) {

        ArrayList<String> filenames = new ArrayList<>();
        File directory = new File(folder);

        if (directory.exists()) {
            try {
                File[] files = directory.listFiles();
                assert files != null;
                for (File file : files) {
                    String file_name = file.getName();
                    filenames.add(file_name);
                }
            } catch (NullPointerException e) {
                Log.e(TAG, folder + " 是一个空文件夹, filenames 返回为一个空的字符串 ArrayList. ");
                e.printStackTrace();
            }
        }
        return filenames;
    }

    // 这将根据用户的 CPU ABI 重命名后缀为 [name]-arm64 或 [name]-armhf 的文件名为 [name]. 
    private String renameAssetIfneeded(String asset) {
        String cpuAbi;
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            cpuAbi = Build.SUPPORTED_ABIS[0];
        } else {
            cpuAbi = Build.CPU_ABI;
        }

        if (asset.matches("^.*-arm64$")) {
            if (cpuAbi.equals("arm64-v8a")) {
                return asset.replaceAll("-arm64$", "");
            }
        } else if (asset.matches("^.*-armeabi$") && !cpuAbi.equals("arm64-v8a")) {
            return asset.replaceAll("-armeabi$", "");
        }

        return asset;
    }

    private void disableMagiskNotification() {
        if (exe.RunAsRootReturnValue("[ -f " + NhPaths.MAGISK_DB_PATH + " ]") == 0) {
            Log.d(TAG, "为 nethunter 应用禁用 magisk 通知和日志. ");
            if (exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_BIN_PATH +
                    "/sqlite3 " + NhPaths.MAGISK_DB_PATH + " \"SELECT * from policies\" | grep " +
                    BuildConfig.APPLICATION_ID).startsWith(BuildConfig.APPLICATION_ID)) {
                exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_BIN_PATH +
                        "/sqlite3 " + NhPaths.MAGISK_DB_PATH + " \"UPDATE policies SET logging='0',notification='0' WHERE package_name='" +
                        BuildConfig.APPLICATION_ID + "';\"");
                Log.d(TAG, "成功更新 magisk 数据库. ");
            } else {
                exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_BIN_PATH + "/sqlite3 " +
                        NhPaths.MAGISK_DB_PATH + " \"UPDATE policies SET logging='0',notification='0' WHERE uid='$(stat -c %u /data/data/" +
                        BuildConfig.APPLICATION_ID + ")';\"");
            }
        }
    }
}