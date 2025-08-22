package com.offsec.nethunter.service;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.offsec.nethunter.AppNavHomeActivity;
import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.CheckForRoot;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.core.app.NotificationCompat;

import java.util.HashMap;
import java.util.Map;

/**
 * 启动时运行的服务, 用于检查启动时的环境并执行必要的初始化操作
 */
public class RunAtBootService extends JobIntentService {
    private static final String TAG = "Nethunter: Startup";
    static final int SERVICE_JOB_ID = 1;
    private NotificationCompat.Builder n = null;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        NhPaths.getInstance(getApplicationContext());
        // 创建通知渠道
        createNotificationChannel();
        sharedPreferences = getApplicationContext().getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
    }

    private void doNotification(String contents) {
        if (n == null) {
            n = new NotificationCompat.Builder(getApplicationContext(), AppNavHomeActivity.BOOT_CHANNEL_ID);
        }
        n.setStyle(new NotificationCompat.BigTextStyle().bigText(contents))
                .setContentTitle(TAG)
                .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                .setAutoCancel(true);
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(999, n.build());
        }
    }

    public static void enqueueWork(Context context, Intent work) {
        enqueueWork(context, RunAtBootService.class, SERVICE_JOB_ID, work);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        onHandleIntent();
    }

    protected void onHandleIntent() {
        // 1. 检查 Root 权限
        // 2. 检查 Busybox 是否安装
        // 3. 执行 NetHunter 的 init.d 脚本
        // 4. 推送通知

        doNotification("正在执行启动检查...");

        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("ROOT", "未授予 Root 权限");
        hashMap.put("BUSYBOX", "未找到 Busybox");
        hashMap.put("CHROOT", "Chroot 尚未安装");

        if (CheckForRoot.isRoot()) {
            hashMap.put("ROOT", "OK");
        }

        if (CheckForRoot.isBusyboxInstalled()) {
            hashMap.put("BUSYBOX", "OK");
        }

        ShellExecuter exe = new ShellExecuter();

        // 检查 SELinux 是否处于宽容模式, 如果不是, 则将其设置为宽容模式, 除非用户已在设置中手动禁用. 
        if (sharedPreferences.getBoolean("SELinuxOnBoot", true)) {
            exe.RunAsRootOutput("[ ! \"$(getenforce | grep Permissive)\" ] && setenforce 0");
        }

        exe.RunAsRootOutput(NhPaths.BUSYBOX + " run-parts " + NhPaths.APP_INITD_PATH);
        if (exe.RunAsRootReturnValue(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"status\"") == 0) {
            // 移除可能的 VNC 锁（如果手机在运行 VNC 服务器时重启）
            exe.RunAsRootOutput("rm -rf " + NhPaths.CHROOT_PATH() + "/tmp/.X1*");
            hashMap.put("CHROOT", "OK");
        }

        String resultMsg = "启动完成. \n所有检查均通过, Chroot 已启动！";
        for (Map.Entry<String, String> entry : hashMap.entrySet()) {
            if (!entry.getValue().equals("OK")) {
                resultMsg = "请确保满足上述所有要求. ";
                break;
            }
        }

        doNotification(
                "Root: " + hashMap.get("ROOT") + "\n" +
                        "Busybox: " + hashMap.get("BUSYBOX") + "\n" +
                        "Chroot: " + hashMap.get("CHROOT") + "\n" +
                        resultMsg);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    AppNavHomeActivity.BOOT_CHANNEL_ID,
                    "Nethunter 启动检查服务",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
