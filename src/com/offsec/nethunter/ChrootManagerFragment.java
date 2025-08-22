package com.offsec.nethunter;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.Executor.ChrootManagerExecutor;
import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.service.CompatCheckService;
import com.offsec.nethunter.service.NotificationChannelService;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.SharePrefTag;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Objects;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

public class ChrootManagerFragment extends Fragment {
    public static final String TAG = "ChrootManager";
    private static final String ARG_SECTION_NUMBER = "section_number";
    public static final String PRIMARY_IMAGE_SERVER = "image-nethunter.kali.org";
    public static final String SECONDARY_IMAGE_SERVER = "kali.download";
    private static final String IMAGE_DIRECTORY = "/nethunter-images/current/rootfs/";
    private static final String INVALID_PATH_REGEX = "^\\.(.*$)|^\\.\\.(.*$)|^/+(.*$)|^.*/+(.*$)|^$";
    private static final String MINORFULL = "";
    private final Intent backPressedintent = new Intent();
    private TextView mountStatsTextView;
    private TextView baseChrootPathTextView;
    private TextView resultViewerLoggerTextView;
    private TextView kaliFolderTextView;
    private Button kaliFolderEditButton;
    private Button mountChrootButton;
    private Button unmountChrootButton;
    private Button installChrootButton;
    private Button addMetaPkgButton;
    private Button removeChrootButton;
    private Button backupChrootButton;
    private LinearLayout ChrootDesc;
    private static SharedPreferences sharedPreferences;
    private ChrootManagerExecutor chrootManagerExecutor;
    private static final int IS_MOUNTED = 0;
    private static final int IS_UNMOUNTED = 1;
    private static final int NEED_TO_INSTALL = 2;
    public static boolean isExecutorRunning = false;
    private Context context;
    private Activity activity;

    public static ChrootManagerFragment newInstance(int sectionNumber) {
        ChrootManagerFragment fragment = new ChrootManagerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.chroot_manager, container, false);

        if (activity != null) {
            sharedPreferences = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        } else {
            throw new IllegalStateException("Activity 为空. 无法初始化 sharedPreferences. ");
        }

        baseChrootPathTextView = rootView.findViewById(R.id.f_chrootmanager_base_path_tv);
        if (baseChrootPathTextView == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_base_path_tv 的视图. ");
        }

        mountStatsTextView = rootView.findViewById(R.id.f_chrootmanager_mountresult_tv);
        if (mountStatsTextView == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_mountresult_tv 的视图. ");
        }

        resultViewerLoggerTextView = rootView.findViewById(R.id.f_chrootmanager_viewlogger);
        if (resultViewerLoggerTextView == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_viewlogger 的视图. ");
        }

        kaliFolderTextView = rootView.findViewById(R.id.f_chrootmanager_kalifolder_tv);
        if (kaliFolderTextView == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_kalifolder_tv 的视图. ");
        }

        kaliFolderEditButton = rootView.findViewById(R.id.f_chrootmanager_edit_btn);
        if (kaliFolderEditButton == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_edit_btn 的视图. ");
        }

        mountChrootButton = rootView.findViewById(R.id.f_chrootmanager_mount_btn);
        if (mountChrootButton == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_mount_btn 的视图. ");
        }

        unmountChrootButton = rootView.findViewById(R.id.f_chrootmanager_unmount_btn);
        if (unmountChrootButton == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_unmount_btn 的视图. ");
        }

        installChrootButton = rootView.findViewById(R.id.f_chrootmanager_install_btn);
        if (installChrootButton == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_install_btn 的视图. ");
        }

        addMetaPkgButton = rootView.findViewById(R.id.f_chrootmanager_addmetapkg_btn);
        if (addMetaPkgButton == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_addmetapkg_btn 的视图. ");
        }

        removeChrootButton = rootView.findViewById(R.id.f_chrootmanager_removechroot_btn);
        if (removeChrootButton == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_removechroot_btn 的视图. ");
        }

        backupChrootButton = rootView.findViewById(R.id.f_chrootmanager_backupchroot_btn);
        if (backupChrootButton == null) {
            throw new IllegalStateException("布局中未找到 ID 为 f_chrootmanager_backupchroot_btn 的视图. ");
        }

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        resultViewerLoggerTextView.setMovementMethod(new ScrollingMovementMethod());
        kaliFolderTextView.setClickable(true);
        if (sharedPreferences != null) {
            kaliFolderTextView.setText(sharedPreferences.getString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, NhPaths.ARCH_FOLDER));
        }
        final LinearLayoutCompat kaliViewFolderlinearLayout = view.findViewById(R.id.f_chrootmanager_viewholder);
        kaliViewFolderlinearLayout.setOnClickListener(view1 -> new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat)
                .setMessage(baseChrootPathTextView.getText().toString() +
                        kaliFolderTextView.getText().toString())
                .create().show());
        setEditButton();
        setStopKaliButton();
        setStartKaliButton();
        setInstallChrootButton();
        setRemoveChrootButton();
        setAddMetaPkgButton();
        setBackupChrootButton();

        // WearOS 优化
        if (activity != null) {
            SharedPreferences sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
            Boolean iswatch = sharedpreferences.getBoolean("running_on_wearos", false);
            if (iswatch) {
                kaliViewFolderlinearLayout.setVisibility(View.GONE);
            }
        }

        // 注册文件选择器
        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri fileUri = result.getData().getData();
                        if (context != null && fileUri != null) {
                            File outFile = new File(context.getFilesDir(), "restore.tar.xz");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                try (InputStream in = context.getContentResolver().openInputStream(fileUri);
                                     OutputStream out = Files.newOutputStream(outFile.toPath())) {
                                    byte[] buffer = new byte[4096];
                                    int bytesRead;
                                    long totalBytes = 0;
                                    while (true) {
                                        assert in != null;
                                        if ((bytesRead = in.read(buffer)) == -1) break;
                                        out.write(buffer, 0, bytesRead);
                                        totalBytes += bytesRead;
                                    }
                                    out.flush();
                                    if (outFile.length() == 0 || totalBytes == 0) {
                                        NhPaths.showMessage(context, "复制的文件为空. 请选择有效的备份文件. ");
                                        return;
                                    }
                                    try (InputStream checkIn = new FileInputStream(outFile)) {
                                        byte[] magic = new byte[6];
                                        if (checkIn.read(magic) == 6) {
                                            if (!(magic[0] == (byte) 0xFD && magic[1] == '7' && magic[2] == 'z' && magic[3] == 'X' && magic[4] == 'Z' && magic[5] == 0x00)) {
                                                NhPaths.showMessage(context, "文件似乎不是有效的 .xz 归档文件. ");
                                                return;
                                            }
                                        }
                                    }
                                    if (sharedPreferences != null) {
                                        sharedPreferences.edit().putString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, outFile.getAbsolutePath()).apply();
                                    }
                                    chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.INSTALL_CHROOT);
                                    chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
                                        @Override
                                        public void onExecutorPrepare() {
                                            if (context != null) {
                                                context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.INSTALLING));
                                            }
                                            broadcastBackPressedIntent(false);
                                            setAllButtonEnable(false);
                                        }

                                        @Override
                                        public void onExecutorProgressUpdate(int progress) {
                                        }

                                        @Override
                                        public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                                            broadcastBackPressedIntent(true);
                                            setAllButtonEnable(true);
                                            compatCheck();
                                        }
                                    });
                                    resultViewerLoggerTextView.setText("");
                                    chrootManagerExecutor.execute(resultViewerLoggerTextView, outFile.getAbsolutePath(), NhPaths.CHROOT_PATH());
                                } catch (IOException e) {
                                    NhPaths.showMessage(context, "复制文件失败:  " + e.getMessage());
                                }
                            }
                        } else {
                            NhPaths.showMessage(context, "未选择文件. ");
                        }
                    }
                }
        );
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!isExecutorRunning){
            compatCheck();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mountStatsTextView = null;
        baseChrootPathTextView = null;
        resultViewerLoggerTextView = null;
        kaliFolderTextView = null;
        kaliFolderEditButton = null;
        mountChrootButton = null;
        unmountChrootButton = null;
        installChrootButton = null;
        addMetaPkgButton = null;
        removeChrootButton = null;
        backupChrootButton = null;
        chrootManagerExecutor = null;
    }

    private void setEditButton() {
        if (activity == null || sharedPreferences == null) {
            throw new IllegalStateException("Activity 或 SharedPreferences 为空. 无法继续. ");
        }

        kaliFolderEditButton.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            final AlertDialog ad = adb.create();
            LinearLayout ll = new LinearLayout(activity);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setLayoutParams(layoutParams);

            EditText chrootPathEditText = new EditText(activity);
            TextView availableChrootPathextview = new TextView(activity);
            LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            editTextParams.setMargins(58, 0, 58, 0);

            chrootPathEditText.setText(sharedPreferences.getString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, ""));
            chrootPathEditText.setSingleLine();
            chrootPathEditText.setLayoutParams(editTextParams);

            availableChrootPathextview.setLayoutParams(editTextParams);
            availableChrootPathextview.setTextColor(ContextCompat.getColor(activity, R.color.clearTitle));
            availableChrootPathextview.setText(String.format(getString(R.string.list_of_available_folders), NhPaths.NH_SYSTEM_PATH));

            File chrootDir = new File(NhPaths.NH_SYSTEM_PATH);
            int count = 0;
            File[] files = chrootDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        if (file.getName().equals("kalifs")) continue;
                        count += 1;
                        availableChrootPathextview.append("    " + count + ". " + file.getName() + "\n");
                    }
                }
            } else {
                availableChrootPathextview.append("未找到目录. ");
            }

            ll.addView(chrootPathEditText);
            ll.addView(availableChrootPathextview);

            ad.setCancelable(true);
            ad.setTitle("设置 Chroot 路径");
            ad.setMessage("Chroot 路径会添加到 \n\"/data/local/nhsystem/\"\n\n" +
                    "只需输入 Kali Chroot 文件夹的基本名称: ");
            ad.setView(ll);

            ad.setButton(DialogInterface.BUTTON_POSITIVE, "应用", (dialogInterface, i) -> {
                if (chrootPathEditText.getText().toString().matches(INVALID_PATH_REGEX)) {
                    NhPaths.showMessage(activity, "无效名称, 请重试. ");
                } else {
                    NhPaths.ARCH_FOLDER = chrootPathEditText.getText().toString();
                    kaliFolderTextView.setText(NhPaths.ARCH_FOLDER);
                    sharedPreferences.edit().putString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, NhPaths.ARCH_FOLDER).apply();
                    sharedPreferences.edit().putString(SharePrefTag.CHROOT_PATH_SHAREPREF_TAG, NhPaths.CHROOT_PATH()).apply();
                    new ShellExecuter().RunAsRootOutput("ln -sfn " + NhPaths.CHROOT_PATH() + " " + NhPaths.CHROOT_SYMLINK_PATH);
                    compatCheck();
                }
                dialogInterface.dismiss();
            });

            ad.show();
        });
    }

    private void setStartKaliButton() {
        mountChrootButton.setOnClickListener(view -> {
            chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.MOUNT_CHROOT);
            chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
                @Override
                public void onExecutorPrepare() {
                    setAllButtonEnable(false);
                }

                @Override
                public void onExecutorProgressUpdate(int progress) {}

                @Override
                public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                    if (resultCode == 0){
                        setButtonVisibility(IS_MOUNTED);
                        setMountStatsTextView(IS_MOUNTED);
                        setAllButtonEnable(true);
                        compatCheck();
                        context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.USENETHUNTER));
                    }
                }
            });
            resultViewerLoggerTextView.setText("");
            chrootManagerExecutor.execute(resultViewerLoggerTextView);
        });
    }

    private void setStopKaliButton(){
        unmountChrootButton.setOnClickListener(view -> {
            chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.UNMOUNT_CHROOT);
            chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
                @Override
                public void onExecutorPrepare() {
                    setAllButtonEnable(false);
                }

                @Override
                public void onExecutorProgressUpdate(int progress) {}

                @Override
                public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                    if (resultCode == 0){
                        setMountStatsTextView(IS_UNMOUNTED);
                        setButtonVisibility(IS_UNMOUNTED);
                        setAllButtonEnable(true);
                        compatCheck();
                    }
                }
            });
            resultViewerLoggerTextView.setText("");
            chrootManagerExecutor.execute(resultViewerLoggerTextView);
        });
    }

    private void setInstallChrootButton() {
        installChrootButton.setOnClickListener(view -> {
            String[] options = {"最小化", "完整"};
            new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat)
                    .setTitle("选择 Kali 镜像")
                    .setItems(options, (dialog, which) -> {
                        String arch = getDeviceArch();
                        String type = (which == 0) ? "minimal" : "full";
                        String fileName = "kalifs-" + arch + "-" + type + ".tar.xz";
                        File downloadDir = context.getFilesDir();
                        File targetFile;
                        try {
                            targetFile = new File(downloadDir, fileName);
                        } catch (Exception e) {
                            NhPaths.showMessage(context, "访问文件时出错:  " + e.getMessage());
                            return;
                        }

                        Runnable startProcess = () -> startDownloadAndRestoreChroot(fileName, downloadDir, type, arch);

                        if (targetFile.exists()) {
                            new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat)
                                    .setTitle("覆盖文件？")
                                    .setMessage("镜像文件已存在. 是否要覆盖它？")
                                    .setPositiveButton("覆盖", (d, w) -> startProcess.run())
                                    .setNegativeButton("取消", null)
                                    .show();
                        } else {
                            startProcess.run();
                        }
                    })
                    .setCancelable(true)
                    .show();
        });
    }

    private void startDownloadAndRestoreChroot(String fileName, File downloadDir, String type, String arch) {
        chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.DOWNLOAD_CHROOT);
        chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
            @Override
            public void onExecutorPrepare() {
                setAllButtonEnable(false);
            }

            @Override
            public void onExecutorProgressUpdate(int progress) {}

            @Override
            public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                setAllButtonEnable(true);
                if (resultCode == 0) {
                    restoreChrootImage(new File(downloadDir, fileName).getAbsolutePath());
                } else {
                    NhPaths.showMessage(context, "下载失败. ");
                }
            }
        });

        resultViewerLoggerTextView.setText("");
        String imagePath = "/nethunter-images/current/rootfs/" + fileName;
        try {
            chrootManagerExecutor.execute(
                    resultViewerLoggerTextView,
                    ChrootManagerFragment.PRIMARY_IMAGE_SERVER,
                    imagePath,
                    new File(downloadDir, fileName).getAbsolutePath()
            );
        } catch (Exception e) {
            NhPaths.showMessage(context, "执行期间出错:  " + e.getMessage());
        }
    }

    @NonNull
    public MaterialAlertDialogBuilder getMaterialAlertDialogBuilder(File downloadDir, String targetDownloadFileName) {
        MaterialAlertDialogBuilder adb3 = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
        adb3.setMessage(downloadDir.getAbsoluteFile() + "/" + targetDownloadFileName + " 已存在. 是否要覆盖它？");
        adb3.setPositiveButton("是", (dialogInterface1, i1) -> {
            context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.DOWNLOADING));
            startDownloadChroot(targetDownloadFileName, downloadDir);
        });
        adb3.setNegativeButton("否", (dialogInterface12, i12) -> dialogInterface12.dismiss());
        return adb3;
    }

    private void restoreChrootImage(String imagePath) {
        chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.INSTALL_CHROOT);
        chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
            @Override
            public void onExecutorPrepare() {
                setAllButtonEnable(false);
            }
            @Override
            public void onExecutorProgressUpdate(int progress) {}
            @Override
            public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                setAllButtonEnable(true);
                if (resultCode == 0) {
                    NhPaths.showMessage(context, "Chroot 镜像已成功恢复. ");
                    compatCheck();
                } else {
                    NhPaths.showMessage(context, "恢复 Chroot 镜像失败. ");
                }
            }
        });

        resultViewerLoggerTextView.setText("");
        // 假设 NhPaths.CHROOT_PATH() 返回目标提取目录
        chrootManagerExecutor.execute(
                resultViewerLoggerTextView,
                imagePath,
                NhPaths.CHROOT_PATH()
        );
    }

    private void setRemoveChrootButton(){
        removeChrootButton.setOnClickListener(view -> {
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat)
                    .setTitle("警告！")
                    .setMessage("您确定要删除以下 Kali Chroot 文件夹吗？\n" + NhPaths.CHROOT_PATH())
                    .setPositiveButton("我确定. ", (dialogInterface, i) -> {
                        MaterialAlertDialogBuilder adb1 = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat)
                                .setTitle("警告！")
                                .setMessage("这是您的最后机会！")
                                .setPositiveButton("就这样吧. ", (dialogInterface1, i1) -> {
                                    chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.REMOVE_CHROOT);
                                    chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
                                        @Override
                                        public void onExecutorPrepare() {
                                            broadcastBackPressedIntent(false);
                                            setAllButtonEnable(false);
                                        }

                                        @Override
                                        public void onExecutorProgressUpdate(int progress) {
                                            // 无操作
                                        }

                                        @Override
                                        public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                                            broadcastBackPressedIntent(true);
                                            setAllButtonEnable(true);
                                            compatCheck();
                                        }
                                    });
                                    resultViewerLoggerTextView.setText("");
                                    chrootManagerExecutor.execute(resultViewerLoggerTextView);
                                })
                                .setNegativeButton("好吧, 我错了. ", (dialogInterface12, i12) -> {

                                });
                        adb1.create().show();
                    })
                    .setNegativeButton("算了吧. ", (dialogInterface, i) -> { });
            adb.create().show();
        });
    }

    private void startDownloadChroot(String targetDownloadFileName, File downloadDir) {
        if (activity == null || context == null) return;

        ProgressBar progressBar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat)
                .setTitle("正在下载 " + targetDownloadFileName)
                .setMessage("请不要关闭应用或清除最近应用...")
                .setCancelable(false)
                .setView(progressBar)
                .create();

        chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.DOWNLOAD_CHROOT);
        chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
            @Override
            public void onExecutorPrepare() {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        broadcastBackPressedIntent(false);
                        setAllButtonEnable(false);
                        progressDialog.show();
                    });
                }
            }

            @Override
            public void onExecutorProgressUpdate(int progress) {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        progressBar.setProgress(progress);
                        if (progress == 100) {
                            progressDialog.dismiss();
                            broadcastBackPressedIntent(true);
                            setAllButtonEnable(true);
                        }
                    });
                }
            }

            @Override
            public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                if (activity != null) {
                    activity.runOnUiThread(() -> {
                        if (resultCode == 0) {
                            NhPaths.showMessage(context, "下载完成. ");
                        } else {
                            NhPaths.showMessage(context, "下载失败. 请重试. ");
                        }
                    });
                }
            }
        });

        resultViewerLoggerTextView.setText("");
        String[] servers = {PRIMARY_IMAGE_SERVER, SECONDARY_IMAGE_SERVER};
        for (String server : servers) {
            chrootManagerExecutor.execute(
                    resultViewerLoggerTextView,
                    server,
                    IMAGE_DIRECTORY + targetDownloadFileName,
                    new File(downloadDir, targetDownloadFileName).getAbsolutePath()
            );
        }
    }

    private String getDeviceArch() {
        String abi = Build.SUPPORTED_ABIS != null && Build.SUPPORTED_ABIS.length > 0
                ? Build.SUPPORTED_ABIS[0]
                : Build.CPU_ABI;
        if (abi.contains("arm64")) return "arm64";
        if (abi.contains("armeabi")) return "armhf";
        // 默认回退
        return "arm64";
    }

    private ProgressBar createProgressBar() {
        if (activity == null) {
            throw new IllegalStateException("Activity 为空. 无法创建 ProgressBar. ");
        }
        return new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
    }

    private AlertDialog createProgressDialog(String fileName, ProgressBar progressBar) {
        if (activity == null) {
            throw new IllegalStateException("Activity 为空. 无法创建 ProgressDialog. ");
        }
        return new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat)
                .setTitle("正在下载 " + fileName)
                .setMessage("请不要关闭应用或清除最近应用...")
                .setCancelable(false)
                .setView(progressBar)
                .create();
    }

    private void runOnUiThread(Runnable action) {
        if (activity != null) {
            activity.runOnUiThread(action);
        } else {
            throw new IllegalStateException("Activity 为空. 无法在 UI 线程上运行. ");
        }
    }

    private void setAddMetaPkgButton() {
        addMetaPkgButton.setOnClickListener(view -> {
            // 目前, 我们将在对话框视图中硬编码包. 稍后我们将自动获取它们. 
            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adb.setTitle("元包安装与升级");
            LayoutInflater inflater = activity.getLayoutInflater();
            @SuppressLint("InflateParams") final ScrollView sv = (ScrollView) inflater.inflate(R.layout.metapackagechooser, null);
            adb.setView(sv);
            final Button metapackageButton = sv.findViewById(R.id.metapackagesWeb);
            metapackageButton.setOnClickListener(v -> {
                String metapackagesURL = "https://tools.kali.org/kali-metapackages";
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(metapackagesURL));
                startActivity(browserIntent);
            });
            adb.setPositiveButton(R.string.InstallAndUpdateButtonText, (dialog, which) -> {
                StringBuilder sb = new StringBuilder();
                CheckBox cb;
                // 现在获取对话框中的所有复选框并检查其状态
                // 感谢 "user2" 提供了一个如何获取对话框视图的 2 行示例: https://stackoverflow.com/a/13959585/3035127
                final AlertDialog d = (AlertDialog) dialog;
                final LinearLayout ll = d.findViewById(R.id.metapackageLinearLayout);
                int children = Objects.requireNonNull(ll).getChildCount();
                for (int cnt = 0; cnt < children; cnt++) {
                    if (ll.getChildAt(cnt) instanceof CheckBox) {
                        cb = (CheckBox) ll.getChildAt(cnt);
                        if (cb.isChecked()) {
                            sb.append(cb.getText()).append(" ");
                        }
                    }
                }
                try {
                    run_cmd("apt update && apt install " + sb + " -y && echo \"(现在可以关闭终端了)\n\" ");
                } catch (Exception e) {
                    NhPaths.showMessage(context, getString(R.string.toast_install_terminal));
                }
            });
            AlertDialog ad = adb.create();
            ad.setCancelable(true);
            ad.show();
        });
    }

    private void setBackupChrootButton() {
        backupChrootButton.setOnClickListener(view -> {
            AlertDialog ad = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat).create();
            EditText backupFullPathEditText = new EditText(activity);
            LinearLayout ll = new LinearLayout(activity);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            ll.setOrientation(LinearLayout.VERTICAL);
            ll.setLayoutParams(layoutParams);
            LinearLayout.LayoutParams editTextParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
            editTextParams.setMargins(58,40,58,0);
            backupFullPathEditText.setLayoutParams(editTextParams);
            ll.addView(backupFullPathEditText);
            ad.setView(ll);
            ad.setTitle("备份 Chroot");
            ad.setMessage("* 强烈建议将备份的 Chroot 创建为 tar.gz 格式, 以便更快地处理, 但文件大小会更大. \n\n备份 \"" + NhPaths.CHROOT_PATH() + "\" 到: ");
            backupFullPathEditText.setText(sharedPreferences.getString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, ""));
            ad.setButton(DialogInterface.BUTTON_POSITIVE, "确定", (dialogInterface, i) -> {
                sharedPreferences.edit().putString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, backupFullPathEditText.getText().toString()).apply();
                if (new File(backupFullPathEditText.getText().toString()).exists()){
                    ad.dismiss();
                    AlertDialog ad2 = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat).create();
                    ad2.setMessage("文件已存在, 是否要覆盖它？");
                    ad2.setButton(DialogInterface.BUTTON_POSITIVE, "是", (dialogInterface1, i1) -> {
                        chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.BACKUP_CHROOT);
                        chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
                            @Override
                            public void onExecutorPrepare() {
                                context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.BACKINGUP));
                                broadcastBackPressedIntent(false);
                                setAllButtonEnable(false);
                            }

                            @Override
                            public void onExecutorProgressUpdate(int progress) {
                                // 无操作
                            }

                            @Override
                            public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                                broadcastBackPressedIntent(true);
                                setAllButtonEnable(true);
                            }
                        });
                        resultViewerLoggerTextView.setText("");
                        chrootManagerExecutor.execute(resultViewerLoggerTextView, NhPaths.CHROOT_PATH(), backupFullPathEditText.getText().toString());
                    });
                    ad2.show();
                } else {
                    chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.BACKUP_CHROOT);
                    chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
                        @Override
                        public void onExecutorPrepare() {
                            context.startService(new Intent(context, NotificationChannelService.class).setAction(NotificationChannelService.BACKINGUP));
                            broadcastBackPressedIntent(false);
                            setAllButtonEnable(false);
                        }

                        @Override
                        public void onExecutorProgressUpdate(int progress) {
                            // 无操作
                        }

                        @Override
                        public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                            broadcastBackPressedIntent(true);
                            setAllButtonEnable(true);
                        }
                    });
                    chrootManagerExecutor.execute(resultViewerLoggerTextView, NhPaths.CHROOT_PATH(), backupFullPathEditText.getText().toString());
                }
            });
            ad.show();
        });
    }

    private void showBanner() {
        resultViewerLoggerTextView.setText("");
        chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.ISSUE_BANNER);
        chrootManagerExecutor.execute(resultViewerLoggerTextView, getResources().getString(R.string.aboutchroot));
    }

    private void compatCheck() {
        chrootManagerExecutor = new ChrootManagerExecutor(ChrootManagerExecutor.CHECK_CHROOT);
        chrootManagerExecutor.setListener(new ChrootManagerExecutor.ChrootManagerExecutorListener() {
            @Override
            public void onExecutorPrepare() {
                broadcastBackPressedIntent(false);
            }

            @Override
            public void onExecutorProgressUpdate(int progress) { }

            @Override
            public void onExecutorFinished(int resultCode, ArrayList<String> resultString) {
                broadcastBackPressedIntent(true);
                setButtonVisibility(resultCode);
                setMountStatsTextView(resultCode);
                setAllButtonEnable(true);
                context.startService(new Intent(context, CompatCheckService.class).putExtra("RESULTCODE", resultCode));
            }
        });
        resultViewerLoggerTextView.setText("");
        chrootManagerExecutor.execute(resultViewerLoggerTextView, sharedPreferences.getString(SharePrefTag.CHROOT_PATH_SHAREPREF_TAG, ""));
    }

    private void setMountStatsTextView(int MODE) {
        if (MODE == IS_MOUNTED) {
            mountStatsTextView.setTextColor(Color.GREEN);
            mountStatsTextView.setText(R.string.running);
        } else if  (MODE == IS_UNMOUNTED) {
            mountStatsTextView.setTextColor(Color.RED);
            mountStatsTextView.setText(R.string.stopped);
        } else if  (MODE == NEED_TO_INSTALL) {
            // 如果 Chroot 未安装, 仅显示关于横幅并清除旧日志（以便为新用户显示横幅）
            resultViewerLoggerTextView.setText("");
            showBanner();

            mountStatsTextView.setTextColor(Color.RED);
            mountStatsTextView.setText(R.string.not_yet_installed);
        }
    }

    private void setButtonVisibility(int MODE) {
        SharedPreferences sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        Boolean iswatch = sharedpreferences.getBoolean("running_on_wearos", false);

        switch (MODE) {
            case IS_MOUNTED:
                mountChrootButton.setVisibility(View.GONE);
                unmountChrootButton.setVisibility(View.VISIBLE);
                installChrootButton.setVisibility(View.GONE);
                if (iswatch) {
                    addMetaPkgButton.setVisibility(View.GONE);
                } else {
                    addMetaPkgButton.setVisibility(View.VISIBLE);
                }
                removeChrootButton.setVisibility(View.GONE);
                backupChrootButton.setVisibility(View.GONE);
                break;
            case IS_UNMOUNTED:
                mountChrootButton.setVisibility(View.VISIBLE);
                unmountChrootButton.setVisibility(View.GONE);
                installChrootButton.setVisibility(View.GONE);
                addMetaPkgButton.setVisibility(View.GONE);
                removeChrootButton.setVisibility(View.VISIBLE);
                backupChrootButton.setVisibility(View.VISIBLE);
                break;
            case NEED_TO_INSTALL:
                mountChrootButton.setVisibility(View.GONE);
                unmountChrootButton.setVisibility(View.GONE);
                installChrootButton.setVisibility(View.VISIBLE);
                addMetaPkgButton.setVisibility(View.GONE);
                removeChrootButton.setVisibility(View.GONE);
                backupChrootButton.setVisibility(View.GONE);
                break;
        }
    }

    private void setAllButtonEnable(boolean isEnable) {
        if (mountChrootButton != null) {
            mountChrootButton.setEnabled(isEnable);
        }
        if (unmountChrootButton != null) {
            unmountChrootButton.setEnabled(isEnable);
        }
        if (installChrootButton != null) {
            installChrootButton.setEnabled(isEnable);
        }
        if (addMetaPkgButton != null) {
            addMetaPkgButton.setEnabled(isEnable);
        }
        if (removeChrootButton != null) {
            removeChrootButton.setEnabled(isEnable);
        }
        if (kaliFolderEditButton != null) {
            kaliFolderEditButton.setEnabled(isEnable);
        }
        if (backupChrootButton != null) {
            backupChrootButton.setEnabled(isEnable);
        }
    }

    private void broadcastBackPressedIntent(Boolean isEnabled){
        backPressedintent.setAction(AppNavHomeActivity.NethunterReceiver.BACKPRESSED);
        backPressedintent.putExtra("isEnable", isEnabled);
        context.sendBroadcast(backPressedintent);
        setHasOptionsMenu(isEnabled);
    }

    ////
    // Bridge side functions
    /////

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }
}