package com.offsec.nethunter.service;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.offsec.nethunter.AppNavHomeActivity;
import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.CheckForRoot;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.SharePrefTag;
import com.offsec.nethunter.utils.ShellExecuter;

// IntentService 用于每次用户切换回应用时持续检查兼容性
public class CompatCheckService extends IntentService {
    public static final String TAG = "CompatCheckService";
    private int RESULTCODE = -1;
    private SharedPreferences sharedPreferences;
    public CompatCheckService() {
        super("CompatCheckService");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // 如果 ChrootManagerFragment 没有传递 resultCode, 则将 RESULTCODE 设置为 -1
        if (intent != null) {
            RESULTCODE = intent.getIntExtra("RESULTCODE", -1);
        }

        // 运行 checkCompat 函数, 并在用户未通过兼容性检查时向 MainActivity 发送广播
        if (!checkCompat()) {
            String message = "";
            getApplicationContext().sendBroadcast(new Intent()
                    .putExtra("message", message)
                    .setAction(BuildConfig.APPLICATION_ID + ".CHECKCOMPAT"));
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getApplicationContext().getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
    }

    private boolean checkCompat() {
        // 此服务启动时, 以下代码始终会被执行. 
        // 请记住, 除了该函数的最后一行外, 不要在此处返回 true 或 false. 
        /* 其他兼容性检查从这里开始 */

        // 检查 SELinux 是否处于宽容模式, 如果不是, 则将其设置为宽容模式, 除非用户已在设置中手动设置. 
        if (!sharedPreferences.contains("SElinux")) new ShellExecuter().RunAsRootOutput("[ ! \"$(getenforce | grep Permissive)\" ] && setenforce 0");

        // 仅在首次安装时检查, 寻找可能的 chroot 文件夹并将其设置为 chroot 路径. 
        if (sharedPreferences.getString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, null) == null) {
            String[] chrootDirs = new ShellExecuter().RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"findchroot\"").split("\\n");
            // 如果 findchroot 返回空字符串, 则将 chroot 架构默认设置为 kali-arm64, 否则默认为第一个有效的 chroot 架构. 
            if (chrootDirs[0].isEmpty()) {
                sharedPreferences.edit().putString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, "kali-arm64").apply();
                sharedPreferences.edit().putString(SharePrefTag.CHROOT_PATH_SHAREPREF_TAG, NhPaths.NH_SYSTEM_PATH + "/kali-arm64").apply();
                new ShellExecuter().RunAsRootOutput("ln -sfn " + NhPaths.NH_SYSTEM_PATH + "/kali-arm64 " + NhPaths.CHROOT_SYMLINK_PATH);
            } else {
                sharedPreferences.edit().putString(SharePrefTag.CHROOT_ARCH_SHAREPREF_TAG, chrootDirs[0]).apply();
                sharedPreferences.edit().putString(SharePrefTag.CHROOT_PATH_SHAREPREF_TAG, NhPaths.NH_SYSTEM_PATH + "/" + chrootDirs[0]).apply();
                new ShellExecuter().RunAsRootOutput("ln -sfn " + NhPaths.NH_SYSTEM_PATH + "/" + chrootDirs[0] + " " + NhPaths.CHROOT_SYMLINK_PATH);
            }
        }

        // 检查 chroot 状态, 向用户推送通知, 并在 chroot 尚未启动时禁用所有片段. 
        // 如果意图不是由 chrootmanager 发送的, 则再次运行检查执行器. 
        if (RESULTCODE == -1) {
            if ((new ShellExecuter().RunAsRootReturnValue(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"status\" -p " + NhPaths.CHROOT_PATH()) != 0)) {
                if (AppNavHomeActivity.lastSelectedMenuItem.getItemId() != R.id.createchroot_item) {
                    startService(new Intent(getApplicationContext(), NotificationChannelService.class).setAction(NotificationChannelService.REMINDMOUNTCHROOT));
                    getApplicationContext().sendBroadcast(new Intent()
                            .putExtra("ENABLEFRAGMENT", false)
                            .setAction(AppNavHomeActivity.NethunterReceiver.CHECKCHROOT));
                } else {
                    sendBroadcast(new Intent().putExtra("ENABLEFRAGMENT", false).setAction(AppNavHomeActivity.NethunterReceiver.CHECKCHROOT));
                }
            } else {
                getApplicationContext().sendBroadcast(new Intent()
                        .putExtra("ENABLEFRAGMENT", true)
                        .setAction(AppNavHomeActivity.NethunterReceiver.CHECKCHROOT));
            }
        } else {
            // 如果意图是由 chrootmanager 发送的, 则无需再次运行检查执行器. 
            if (RESULTCODE != 0) {
                if (AppNavHomeActivity.lastSelectedMenuItem.getItemId() != R.id.createchroot_item) {
                    startService(new Intent(getApplicationContext(), NotificationChannelService.class).setAction(NotificationChannelService.REMINDMOUNTCHROOT));
                    getApplicationContext().sendBroadcast(new Intent()
                            .putExtra("ENABLEFRAGMENT", false)
                            .setAction(AppNavHomeActivity.NethunterReceiver.CHECKCHROOT));
                } else {
                    sendBroadcast(new Intent().putExtra("ENABLEFRAGMENT", false).setAction(AppNavHomeActivity.NethunterReceiver.CHECKCHROOT));
                }
            } else {
                getApplicationContext().sendBroadcast(new Intent()
                        .putExtra("ENABLEFRAGMENT", true)
                        .setAction(AppNavHomeActivity.NethunterReceiver.CHECKCHROOT));
            }
        }
        /* 其他兼容性检查结束 */
        return true;
    }
}
