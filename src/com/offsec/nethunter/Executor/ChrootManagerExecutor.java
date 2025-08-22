package com.offsec.nethunter.Executor;

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import com.offsec.nethunter.ChrootManagerFragment;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChrootManagerExecutor {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private ChrootManagerExecutorListener listener;
    private final ShellExecuter exe = new ShellExecuter();
    private final int ACTIONCODE;
    private int resultCode;
    private final ArrayList<String> resultString = new ArrayList<>();
    public static final int CHECK_CHROOT = 0;
    public static final int MOUNT_CHROOT = 1;
    public static final int UNMOUNT_CHROOT = 2;
    public static final int INSTALL_CHROOT = 3;
    public static final int BACKUP_CHROOT = 4;
    public static final int REMOVE_CHROOT = 5;
    public static final int DOWNLOAD_CHROOT = 6;
    public static final int FIND_CHROOT = 7;
    public static final int ISSUE_BANNER = 8;
    public ChrootManagerExecutor(Integer ACTIONCODE){
        this.ACTIONCODE = ACTIONCODE;
    }
    public void execute(Object... objects) {
        mainHandler.post(this::onPreExecute);
        executorService.submit(() -> {
            doInBackground(objects);
            mainHandler.post(this::onPostExecute);
        });
    }

    private void onPreExecute() {
        ChrootManagerFragment.isExecutorRunning = true;
        if (listener != null) {
            listener.onExecutorPrepare();
        }
    }

    protected void doInBackground(Object... objects) {
        switch (ACTIONCODE) {
            case ISSUE_BANNER:
                exe.RunAsRootOutput("echo \"" + objects[1].toString() + "\"", ((TextView) objects[0]));
                break;
            case CHECK_CHROOT:
                resultCode = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"status\" -p " + objects[1].toString(), ((TextView) objects[0]));
                break;
            case MOUNT_CHROOT:
                resultCode = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali_init", ((TextView) objects[0]));
                exe.RunAsRootOutput("sleep 1 && " + NhPaths.CHROOT_INITD_SCRIPT_PATH, ((TextView) objects[0]));
                break;
            case UNMOUNT_CHROOT:
                resultCode = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/killkali", ((TextView) objects[0]));
                break;
            case INSTALL_CHROOT:
                resultCode = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"restore " + objects[1] + " " + objects[2] + "\"", ((TextView) objects[0]));
                break;
            case REMOVE_CHROOT:
                resultCode = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"remove " + NhPaths.CHROOT_PATH() + "\"", ((TextView) objects[0]));
                break;
            case BACKUP_CHROOT:
                resultCode = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"backup " + objects[1].toString() + " " + objects[2].toString() + "\"", ((TextView) objects[0]));
                break;
            case FIND_CHROOT:
                resultString.addAll(Arrays.asList(new ShellExecuter().RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/chrootmgr -c \"findchroot\"").split("\\n")));
                break;
            case DOWNLOAD_CHROOT:
                try {
                    exe.RunAsRootOutput("echo \"[!] 已开始下载, 请稍候……\"", ((TextView) objects[0]));
                    int count;
                    String[] servers = {ChrootManagerFragment.PRIMARY_IMAGE_SERVER, ChrootManagerFragment.SECONDARY_IMAGE_SERVER};
                    boolean success = false;
                    Exception lastException = null;

                    for (String server : servers) {
                        exe.RunAsRootOutput("echo \"[!] 正在从服务器下载: " + server + "\"", ((TextView) objects[0]));
                        BufferedInputStream reader = null;
                        BufferedOutputStream writer = null;

                        try {
                            String imagePath = objects[2].toString();
                            if (imagePath.contains("minimal") && imagePath.contains("arm64")) {
                                imagePath = "/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz";
                            }
                            URL url = new URL("https://" + server + imagePath);
                            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                            int lengthOfFile = connection.getContentLength();

                            InputStream input = connection.getInputStream();
                            reader = new BufferedInputStream(input);
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                writer = new BufferedOutputStream(Files.newOutputStream(Paths.get(objects[3].toString())));
                            }

                            byte[] data = new byte[1024];
                            long bytes = 0;

                            while ((count = reader.read(data)) != -1) {
                                bytes += count;
                                int progress = (int) ((bytes / (float) lengthOfFile) * 100);
                                publishProgress(progress);
                                if (writer != null) {
                                    writer.write(data, 0, count);
                                }
                            }

                            exe.RunAsRootOutput("echo \"[+] 下载完成, 准备从存储安装. \"", ((TextView) objects[0]));
                            success = true;
                            break;

                        } catch (MalformedURLException e) {
                            exe.RunAsRootOutput("echo \"[-] 无效 URL: " + e.getMessage() + "\"", ((TextView) objects[0]));
                            lastException = e;
                        } catch (IOException e) {
                            exe.RunAsRootOutput("echo \"[-] I/O 错误: " + e.getMessage() + "\"", ((TextView) objects[0]));
                            lastException = e;
                        } catch (SecurityException e) {
                            exe.RunAsRootOutput("echo \"[-] 安全错误: " + e.getMessage() + "\"", ((TextView) objects[0]));
                            lastException = e;
                        } finally {
                            try {
                                if (reader != null) reader.close();
                                if (writer != null) writer.close();
                            } catch (IOException e) {
                                exe.RunAsRootOutput("echo \"[-] 关闭流时出错: " + e.getMessage() + "\"", ((TextView) objects[0]));
                            }
                        }
                    }

                    if (!success) {
                        exe.RunAsRootOutput("echo \"[-] 下载失败: " + lastException.getMessage() + "\"", ((TextView) objects[0]));
                        resultCode = 1;
                    }
                } catch (Exception e) {
                    exe.RunAsRootOutput("echo \"[-] 意外错误: " + e.getMessage() + "\"", ((TextView) objects[0]));
                    resultCode = 1;
                }
                break;
        }
    }

    private void publishProgress(int progress) {
        if (listener != null) {
            listener.onExecutorProgressUpdate(progress);
        }
    }

    private void onPostExecute() {
        if (listener != null) {
            listener.onExecutorFinished(resultCode, resultString);
        }
        ChrootManagerFragment.isExecutorRunning = false;
    }

    public void setListener(ChrootManagerExecutorListener listener) {
        this.listener = listener;
    }

    public interface ChrootManagerExecutorListener {
        void onExecutorPrepare();
        void onExecutorProgressUpdate(int progress);
        void onExecutorFinished(int resultCode, ArrayList<String> resultString);
    }
}
