package com.offsec.nethunter.service;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.app.JobIntentService;

import com.offsec.nethunter.Executor.CustomCommandsExecutor;
import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.R;

/**
 * 通知渠道服务, 用于在应用后台时向用户推送通知
 * 通知类型包括提醒挂载 Chroot、使用 NetHunter、下载、安装、备份以及自定义命令的执行结果
 */
public class NotificationChannelService extends JobIntentService {
    public static final String CHANNEL_ID = "NethunterNotifyChannel";
    public static final int NOTIFY_ID = 1002;
    public static final int JOB_ID = 1001;
    public Intent resultIntent = null;
    public PendingIntent resultPendingIntent = null;
    public TaskStackBuilder stackBuilder = null;

    // 通知相关动作
    public static final String REMINDMOUNTCHROOT = BuildConfig.APPLICATION_ID + ".REMINDMOUNTCHROOT";
    public static final String USENETHUNTER = BuildConfig.APPLICATION_ID + ".USENETHUNTER";
    public static final String DOWNLOADING = BuildConfig.APPLICATION_ID + ".DOWNLOADING";
    public static final String INSTALLING = BuildConfig.APPLICATION_ID + ".INSTALLING";
    public static final String BACKINGUP = BuildConfig.APPLICATION_ID + ".BACKINGUP";
    public static final String CUSTOMCOMMAND_START = BuildConfig.APPLICATION_ID + ".CUSTOMCOMMAND_START";
    public static final String CUSTOMCOMMAND_FINISH = BuildConfig.APPLICATION_ID + ".CUSTOMCOMMAND_FINISH";

    /**
     * 将工作排队到此服务
     * @param context 应用上下文
     * @param intent 包含工作内容的 Intent
     */
    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, NotificationChannelService.class, JOB_ID, intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // 创建通知渠道（Android O 及以上版本）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "NethunterChannelService",
                    NotificationManager.IMPORTANCE_HIGH
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        // 根据 Intent 的 Action 类型展示不同通知
        if (intent.getAction() != null) {
            NotificationCompat.Builder builder;
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
            notificationManagerCompat.cancelAll(); // 清除旧通知
            resultIntent = new Intent();
            stackBuilder = TaskStackBuilder.create(this);
            stackBuilder.addNextIntentWithParentStack(resultIntent);
            resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            switch (intent.getAction()) {
                case REMINDMOUNTCHROOT:
                    builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText("请打开 NetHunter 应用并导航至 ChrootManager 以设置您的 KaliChroot. "))
                            .setContentTitle("KaliChroot 未启动或未安装")
                            .setContentText("请导航至 ChrootManager 以设置您的 KaliChroot. ")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(resultPendingIntent);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPostNotificationsPermission(this);
                        return;
                    }
                    notificationManagerCompat.notify(NOTIFY_ID, builder.build());
                    break;
                case USENETHUNTER:
                    builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                            .setTimeoutAfter(10000)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText("Happy hunting!"))
                            .setContentTitle("KaliChroot 已启动！")
                            .setContentText("Happy hunting!")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(resultPendingIntent);
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        requestPostNotificationsPermission(this);
                        return;
                    }
                    notificationManagerCompat.notify(NOTIFY_ID, builder.build());
                    break;
                case DOWNLOADING:
                    builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                            .setTimeoutAfter(15000)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText("请勿关闭应用, 否则下载将被取消！"))
                            .setContentTitle("正在下载 Chroot！")
                            .setContentText("请勿关闭应用, 否则下载将被取消！")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(resultPendingIntent);
                    notificationManagerCompat.notify(NOTIFY_ID, builder.build());
                    break;
                case INSTALLING:
                    builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                            .setTimeoutAfter(15000)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText("请勿关闭应用, 因为它仍然会在后台运行！否则您需要手动终止 tar 进程. "))
                            .setContentTitle("正在安装 Chroot")
                            .setContentText("请勿关闭应用, 因为它仍然会在后台运行！否则您需要手动终止 tar 进程. ")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(resultPendingIntent);
                    notificationManagerCompat.notify(NOTIFY_ID, builder.build());
                    break;
                case BACKINGUP:
                    builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setAutoCancel(true)
                            .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                            .setTimeoutAfter(15000)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText("请勿关闭应用, 因为它仍然会在后台运行！否则您需要手动终止 tar 进程. "))
                            .setContentTitle("正在创建 KaliChroot 备份到本地存储. ")
                            .setContentText("请勿关闭应用, 因为它仍然会在后台运行！否则您需要手动终止 tar 进程. ")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(resultPendingIntent);
                    notificationManagerCompat.notify(NOTIFY_ID, builder.build());
                    break;
                case CUSTOMCOMMAND_START:
                    builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setAutoCancel(false)
                            .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(
                                    "命令: \"" + intent.getStringExtra("CMD") +
                                            "\" 正在 " +
                                            intent.getStringExtra("ENV") + " 环境中后台运行. "))
                            .setContentTitle("自定义命令")
                            .setContentText(
                                    "命令: \"" + intent.getStringExtra("CMD") +
                                            "\" 正在 " +
                                            intent.getStringExtra("ENV") + " 环境中后台运行. ")
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(resultPendingIntent);
                    notificationManagerCompat.notify(NOTIFY_ID, builder.build());
                    break;
                case CUSTOMCOMMAND_FINISH:
                    final int returnCode = intent.getIntExtra("RETURNCODE", 0);
                    final String CMD = intent.getStringExtra("CMD");
                    String resultString = getResultString(returnCode, CMD);
                    builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                            .setAutoCancel(false)
                            .setSmallIcon(R.drawable.ic_stat_ic_nh_notification)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(resultString))
                            .setContentTitle("自定义命令")
                            .setContentText(resultString)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(resultPendingIntent);
                    notificationManagerCompat.notify(NOTIFY_ID, builder.build());
                    break;
            }
        }
    }

    /**
     * 请求通知权限（Android 13 及以上版本）
     * @param context 应用上下文
     */
    private void requestPostNotificationsPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && context instanceof Activity) {
            ActivityCompat.requestPermissions(
                    (Activity) context,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    1
            );
        }
    }

    /**
     * 根据返回码和命令生成执行结果字符串
     * @param returnCode 返回码
     * @param CMD 执行的命令
     * @return 执行结果字符串
     */
    @NonNull
    private static String getResultString(int returnCode, String CMD) {
        String resultString = "";
        if (returnCode == CustomCommandsExecutor.ANDROID_CMD_SUCCESS) {
            resultString = "返回成功. \n命令: \"" + CMD + "\" 已在 Android 环境中执行. ";
        } else if (returnCode == CustomCommandsExecutor.ANDROID_CMD_FAIL) {
            resultString = "返回错误. \n命令: \"" + CMD + "\" 已在 Android 环境中执行. ";
        } else if (returnCode == CustomCommandsExecutor.KALI_CMD_SUCCESS) {
            resultString = "返回成功. \n命令: \"" + CMD + "\" 已在 Kali Chroot 环境中执行. ";
        } else if (returnCode == CustomCommandsExecutor.KALI_CMD_FAIL) {
            resultString = "返回错误. \n命令: \"" + CMD + "\" 已在 Kali Chroot 环境中执行. ";
        }
        return resultString;
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return null;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }
}
