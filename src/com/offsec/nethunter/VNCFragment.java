package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.util.Arrays;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class VNCFragment extends Fragment {
    private static final String TAG = "VNCFragment";
    private String localhostonly = "";
    private Context context;
    private Activity activity;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private String selected_res;
    private String selected_vncres;
    private String selected_vncresCMD = "";
    String selected_disp;
    private String selected_ppi;
    private String selected_user;
    private String selected_display;
    private String vnc_passwd;
    private boolean showingAdvanced;
    private String prevusr = "kali";
    private String delay_cmd = "";
    private Integer posu;
    private Integer posd = 0;
    private static final int MIN_UID = 9000;
    private static final int MAX_UID = 9999;
    final String BUSYBOX_NH= NhPaths.getBusyboxPath();
    private Boolean iswatch;

    public VNCFragment() {
    }

    public static VNCFragment newInstance(int sectionNumber) {
        VNCFragment fragment = new VNCFragment();
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
        final View rootView = inflater.inflate(R.layout.vnc_setup, container, false);
        View AdvancedView = rootView.findViewById(R.id.AdvancedView);
        Button Advanced = rootView.findViewById(R.id.AdvancedButton);
        CheckBox localhostCheckBox = rootView.findViewById(R.id.vnc_checkBox);

        SharedPreferences sharedpreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        CheckBox vncCheckBox = rootView.findViewById(R.id.vnc_checkBox);
        vncCheckBox.setOnClickListener(v -> vncLocalClick());

        boolean confirm_res = sharedpreferences.getBoolean("confirm_res", false);
        if (confirm_res) {
            confirmDialog();
        }
        showingAdvanced = sharedpreferences.getBoolean("advanced_visible", false);

        boolean localhost = sharedpreferences.getBoolean("localhost", true);
        localhostCheckBox.setChecked(localhost);
        AdvancedView.setVisibility(showingAdvanced ? View.VISIBLE : View.INVISIBLE);
        if (showingAdvanced) {
            Advanced.setText(R.string.vnc_hide_advanced_settings);
        }
        else {
            Advanced.setText(R.string.vnc_show_advanced_settings);
        }
        // 检查设备是否为手表
        if (sharedpreferences.getBoolean("running_on_wearos", false)) {
            AdvancedView.setVisibility(View.GONE);
            Advanced.setVisibility(View.GONE);
        }
        // 检查设备是否为手机
        if (sharedpreferences.getBoolean("running_on_phone", false)) {
            AdvancedView.setVisibility(View.VISIBLE);
            Advanced.setVisibility(View.VISIBLE);
        }
        // 检查设备是否为平板
        if (sharedpreferences.getBoolean("running_on_tablet", false)) {
            AdvancedView.setVisibility(View.VISIBLE);
            Advanced.setVisibility(View.VISIBLE);
        }

        // 屏幕尺寸
        DisplayMetrics displaymetrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) activity.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();
        int API_LEVEL =  android.os.Build.VERSION.SDK_INT;
        if (API_LEVEL >= 17)
        {
            disp.getRealMetrics(displaymetrics);
        }
        else
        {
            disp.getMetrics(displaymetrics);
        }
        final int screen_height = displaymetrics.heightPixels;
        final int screen_width = displaymetrics.widthPixels;

        // 因为高度和宽度在屏幕旋转时会变化, 使用较大的作为宽度
        String xwidth;
        String xheight;
        if (screen_height > screen_width) {
            xwidth = Integer.toString(screen_height);
            xheight = Integer.toString(screen_width);
        } else {
            xwidth = Integer.toString(screen_width);
            xheight = Integer.toString(screen_height);
        }

        // 检测手表
        final TextView KexDesc = rootView.findViewById(R.id.kexdesc);
        final TextView KexStatus = rootView.findViewById(R.id.status);
        final TextView KexSessions = rootView.findViewById(R.id.sessions);
        iswatch = sharedpreferences.getBoolean("running_on_wearos", false);
        if (iswatch) {
            KexDesc.setVisibility(View.GONE);
            KexStatus.setText(R.string.vnc_watch_status);
            KexSessions.setText(R.string.vnc_watch_sessions);
        }

        Button StartAudioButton = rootView.findViewById(R.id.vnc_audio);
        Button SetupVNCButton = rootView.findViewById(R.id.set_up_vnc);
        Button StartVNCButton = rootView.findViewById(R.id.start_vnc);
        Button StopVNCButton = rootView.findViewById(R.id.stop_vnc);
        Button OpenVNCButton = rootView.findViewById(R.id.vncClientStart);
        ImageButton RefreshKeX = rootView.findViewById(R.id.refreshKeX);
        Button AddUserButton = rootView.findViewById(R.id.AddUserButton);
        Button DelUserButton = rootView.findViewById(R.id.DelUserButton);
        Button ResetHDMIButton = rootView.findViewById(R.id.reset_hdmi);
        Button AddResolutionButton = rootView.findViewById(R.id.AddResolutionButton);
        Button DelResolutionButton = rootView.findViewById(R.id.DelResolutionButton);
        Button ApplyResolutionButton = rootView.findViewById(R.id.ApplyResolutionButton);
        Button BackupHDMI = rootView.findViewById(R.id.BackupResolutions);
        Button RestoreHDMI = rootView.findViewById(R.id.RestoreResolutions);
        Button AddVNCResolutionButton = rootView.findViewById(R.id.AddVncResolutionButton);
        Button DelVNCResolutionButton = rootView.findViewById(R.id.DelVncResolutionButton);
        Button BackupVNC = rootView.findViewById(R.id.BackupVncResolutions);
        Button RestoreVNC = rootView.findViewById(R.id.RestoreVncResolutions);

        // 将设备分辨率添加到 vnc-resolution（仅首次运行）
        ShellExecuter exe = new ShellExecuter();
        File vncResFile = new File(NhPaths.APP_SD_FILES_PATH + "/configs/vnc-resolutions");
        String device_res = xwidth + "x" + xheight;
        if (vncResFile.length() == 0)
            exe.RunAsRoot(new String[]{"echo \"Auto\"$\"\n\"" + device_res + " > " + vncResFile});

        // HDMI 分辨率
        File hdmiResFile = new File(NhPaths.APP_SD_FILES_PATH + "/configs/hdmi-resolutions");
        String[] commandRES = {"sh", "-c", "cat " + hdmiResFile};
        String outputRES = exe.Executer(Arrays.toString(commandRES));
        final String[] resArray = outputRES.split("\n");

        // VNC 分辨率
        String[] commandVNCRES = {"sh", "-c", "cat " + vncResFile};
        String outputVNCRES = exe.Executer(Arrays.toString(commandVNCRES));
        final String[] vncresArray = outputVNCRES.split("\n");

        // HDMI 分辨率下拉列表
        Spinner resolution = rootView.findViewById(R.id.resolution);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_list_item_1, resArray);
        resolution.setAdapter(adapter);

        // VNC 分辨率下拉列表
        Spinner vncresolution = rootView.findViewById(R.id.vncresolution);
        ArrayAdapter<String> vncadapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_list_item_1, vncresArray);
        vncresolution.setAdapter(vncadapter);

        // 用户
        File passwd = new File(NhPaths.CHROOT_PATH() + "/etc/passwd");
        String commandUSR = ("echo root && " + BUSYBOX_NH + " awk -F':' -v \"min=" + MIN_UID + "\" -v \"max=" + MAX_UID + "\" '{ if ( ( $3 >= min && $3 <= max ) || ( $3 >= 100000 && $3 <= 101000 ) ) print $0}' " + passwd + " | " + BUSYBOX_NH + " cut -d: -f1");
        String outputUSR = exe.RunAsRootOutput(commandUSR);
        final String[] userArray = outputUSR.split("\n");
        Arrays.sort(userArray);

        // 上次选定的用户
        prevusr = sharedpreferences.getString("user", "");

        // 用户下拉列表
        Spinner users = rootView.findViewById(R.id.user);
        ArrayAdapter<String> usersadapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_list_item_1, userArray);
        users.setAdapter(usersadapter);
        Arrays.sort(userArray);
        posu = usersadapter.getPosition(prevusr);
        users.setSelection(posu);

        // 上次选定的显示
        posd = sharedpreferences.getInt("display", 0);

        // 显示下拉列表
        String[] displaylist = new String[]{"1","2","3","4","5","6","7","8","9","10"};
        Spinner displays = rootView.findViewById(R.id.display);
        ArrayAdapter<String> displayadapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_list_item_1, displaylist);
        displays.setAdapter(displayadapter);
        displays.setSelection(posd);

        // 选择用户
        users.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                selected_user = parentView.getItemAtPosition(pos).toString();
                sharedpreferences.edit().putString("user", selected_user).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // 选择显示
        displays.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int posd, long id) {
                selected_display = parentView.getItemAtPosition(posd).toString();
                sharedpreferences.edit().putInt("display", posd).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // 选择 HDMI 分辨率
        resolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView,int pos, long id) {
                selected_res = parentView.getItemAtPosition(pos).toString();
                selected_disp = exe.RunAsRootOutput("echo " + selected_res + " | cut -d : -f 1");
                selected_ppi = exe.RunAsRootOutput("echo " + selected_res + " | cut -d : -f 2 | sed 's/ppi//g'");
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // 上次选定的分辨率
        Integer prevres = sharedpreferences.getInt("last_kex_res", 0);
        String prevres_string = sharedpreferences.getString("last_kex_res_string", "");
        if (exe.RunAsRootOutput("grep "+ prevres_string + " " + NhPaths.APP_SD_FILES_PATH + "/configs/vnc-resolutions").equals(prevres_string)) {
            vncresolution.setSelection(prevres);
        }

        // 选择 VNC 分辨率
        vncresolution.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                selected_vncres = parentView.getItemAtPosition(pos).toString();
                if (selected_vncres.equals("Auto") || selected_vncres.isEmpty()) {
                    selected_vncresCMD = "";

                }
                else selected_vncresCMD = "-geometry " + selected_vncres + " ";
                sharedpreferences.edit().putInt("last_kex_res", pos).apply();
                sharedpreferences.edit().putString("last_kex_res_string", selected_vncres).apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // 沉浸模式开关
        final SwitchCompat immersionSwitch = rootView.findViewById(R.id.immersionSwitch);
        final String immersion = exe.RunAsRootOutput("settings get global policy_control");
        immersionSwitch.setChecked(!immersion.equals("null"));

        immersionSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                exe.RunAsRoot(new String[]{"settings put global policy_control immersive.full=*"});
            } else {
                exe.RunAsRoot(new String[]{"settings put global policy_control null"});
            }
        });

        // 仅限本地主机的复选框
        if (localhostCheckBox.isChecked())
            localhostonly = "-localhost yes ";
        else
            localhostonly = "-localhost no ";
        View.OnClickListener checkBoxListener = v -> {
            if (localhostCheckBox.isChecked()) {
                localhostonly = "-localhost yes ";
                sharedpreferences.edit().putBoolean("localhost", true).apply();

            } else {
                localhostonly = "-localhost no ";
                sharedpreferences.edit().putBoolean("localhost", false).apply();
            }
        };
        localhostCheckBox.setOnClickListener(checkBoxListener);

        // VNC 服务复选框
        File kex_init = new File(NhPaths.APP_PATH + "/etc/init.d/99kex");
        final CheckBox vnc_serviceCheckBox = rootView.findViewById(R.id.vnc_serviceCheckBox);
        final String initfile = exe.RunAsRootOutput("cat " + kex_init);

        vnc_serviceCheckBox.setChecked(initfile.contains("vncserver"));

        vnc_serviceCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                File rootvncpasswd = new File(NhPaths.CHROOT_PATH() + "/root/.vnc/passwd");
                String vnc_passwd = exe.RunAsRootOutput("cat " + rootvncpasswd);
                if (!vnc_passwd.isEmpty()) {
                    String arch_path = exe.RunAsRootOutput("ls " + NhPaths.CHROOT_PATH() + "/usr/lib/ | grep linux-gnu | head -n1");
                    String shebang = "#!/system/bin/sh\n";
                    String kex_prep = "\n# KeX 架构路径: " + arch_path + "\n# 开机时运行的命令:\nHOME=/root\nUSER=root";
                    String kex_cmd = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd LD_PRELOAD=/usr/lib/" + arch_path + "/libgcc_s.so.1 vncserver :1 " + localhostonly + " " + selected_vncresCMD + "'";
                    String fileContents = shebang + "\n" + kex_prep + "\n" + kex_cmd;
                    exe.RunAsRoot(new String[]{
                            "cat > " + kex_init + " <<s0133717hur75\n" + fileContents + "\ns0133717hur75\n",
                            "chmod 700 " + kex_init
                    });
                }
                else {
                    Toast.makeText(requireActivity().getApplicationContext(), "请先设置本地服务器!", Toast.LENGTH_SHORT).show();
                    vnc_serviceCheckBox.setChecked(false);
                }
            } else
                exe.RunAsRoot(new String[]{"rm -rf " + kex_init});
        });

        // 延迟
        final CheckBox delayCheckBox = rootView.findViewById(R.id.delay_checkBox);
        final EditText delayText = rootView.findViewById(R.id.delay_time);
        final Boolean delay = sharedpreferences.getBoolean("delay", false);
        if (delay.equals(true)) {
            delayCheckBox.setChecked(true);
            delayText.setText(String.valueOf(sharedpreferences.getInt("delaysec", 20)));
            delayText.setEnabled(true);
            delayText.setTextColor(Color.parseColor("#FFFFFF"));
        }

        delayCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                sharedpreferences.edit().putBoolean("delay", true).apply();
                delayText.setEnabled(true);
                delayText.setTextColor(Color.parseColor("#FFFFFF"));
            } else {
                sharedpreferences.edit().putBoolean("delay", false).apply();
                delayText.setEnabled(false);
                delayText.setTextColor(Color.parseColor("#40FFFFFF"));
            }
        });

        // 服务器状态
        RefreshKeX.setOnClickListener(v -> refreshVNC(rootView));
        refreshVNC(rootView);

        // KeX 音频
        addClickListener(StartAudioButton, v -> {
            File audio = new File(NhPaths.CHROOT_PATH() + "/usr/bin/audio");
            if (audio.exists()) {
                Log.d("KeXAudio", "音频脚本存在于: " + audio.getAbsolutePath());

                if (StartAudioButton.getText().equals("启用音频")) {
                    // 启动逻辑
                    if (selected_user.equals("root")) {
                        Log.d("KeXAudio", "以 root 身份运行音频启用命令");
                        run_cmd("echo -ne \"\\033]0;启用音频\\007\" && clear && audio start;sleep 2 && exit");
                    } else {
                        Log.d("KeXAudio", "检查非 root 用户的权限: " + selected_user);
                        if (isSuAvailable()) {
                            Log.d("KeXAudio", "使用 su 为非 root 用户启动音频");
                            run_cmd("su -c 'echo -ne \"\\033]0;启用音频\\007\" && clear && sudo -u " + selected_user + " audio start;sleep 2 && exit'");
                        } else {
                            Log.w("KeXAudio", "用户缺乏必要权限或 su 不可用. 权限被拒绝. ");
                            Toast.makeText(requireActivity().getApplicationContext(), "用户缺乏必要权限或 su 不可用. ", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    StartAudioButton.setText(R.string.vnc_disable_audio);
                    refreshVNC(rootView);
                    Log.d("KeXAudio", "为用户启用了音频: " + selected_user);
                    Toast.makeText(requireActivity().getApplicationContext(), "为用户启用了音频:" + selected_user, Toast.LENGTH_SHORT).show();
                } else {
                    // 停止逻辑
                    if (selected_user.equals("root")) {
                        Log.d("KeXAudio", "以 root 身份运行音频禁用命令");
                        run_cmd("echo -ne \"\\033]0;禁用音频\\007\" && clear && audio stop;sleep 2 && exit");
                    } else {
                        Log.d("KeXAudio", "为非 root 用户禁用音频: " + selected_user);
                        if (isSuAvailable()) {
                            Log.d("KeXAudio", "使用 su 为非 root 用户停止音频");
                            run_cmd("su -c 'echo -ne \"\\033]0;禁用音频\\007\" && clear && sudo -u " + selected_user + " audio stop;sleep 2 && exit'");
                        } else {
                            Log.w("KeXAudio", "用户缺乏必要权限或 su 不可用. 权限被拒绝. ");
                            Toast.makeText(requireActivity().getApplicationContext(), "用户缺乏必要权限或 su 不可用. ", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    StartAudioButton.setText(R.string.vnc_enable_audio);
                    refreshVNC(rootView);
                    Log.d("KeXAudio", "为用户禁用了音频: " + selected_user);
                    Toast.makeText(requireActivity().getApplicationContext(), "为用户禁用了音频:" + selected_user, Toast.LENGTH_SHORT).show();
                }
            } else {
                Log.d("KeXAudio", "未找到音频脚本, 尝试安装");
                Toast.makeText(requireActivity().getApplicationContext(), "正在 chroot 中安装缺失的音频脚本..", Toast.LENGTH_SHORT).show();
                run_cmd("echo -ne \"\\033]0;Kali NetHunter 工具\\007\" && clear;apt update && apt install nethunter-utils;sleep 2 && exit");
            }
        });
        addClickListener(SetupVNCButton, v -> {
            String desktop = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd dpkg -l | grep kali-desktop");
            if (desktop.isEmpty()) {
                desktopDialog();
            } else {
                if (iswatch) {
                    Toast.makeText(requireActivity().getApplicationContext(), "在智能手表上为 root 用户使用密码 123456 进行 KeX. ", Toast.LENGTH_LONG).show();
                    run_cmd("echo -ne \"\\033]0;KeX 设置\\007\" && clear;echo '正在设置 root:123456 KeX 凭据..' && sleep 2 && echo 123456\\\\n123456\\\\nn\\\\n | vncpasswd;echo '完成! 正在退出..' && sleep 2 && exit");
                } else run_cmd("echo -ne \"\\033]0;设置服务器\\007\" && clear;chmod +x ~/.vnc/xstartup && clear;echo $'\n'\"请输入您的新 VNC 服务器密码\"$'\n' && " + "if [ \"" + selected_user + "\" == \"root\" ]; then " + "  if [ ! -d /root/.config/tigervnc ]; then mkdir -p -m 0777 /root/.config/tigervnc;fi; " + "  sudo -u root vncpasswd; " + "  if [ ! -f ~/.vnc/passwd ]; then cp -rf ~/.config/tigervnc/passwd ~/.vnc/; fi; " + "else " + " user_uid=$(id -u " + selected_user + "); " + " if [ \"$user_uid\" -eq 100000 ] || [ \"$user_uid\" -eq 9000 ]; then " + " if [ ! -d /home/" + selected_user + "/.config/tigervnc ]; then mkdir -p -m 0777 /home/" + selected_user + "/.config/tigervnc; fi; " + "  sudo -u " + selected_user + " vncpasswd; " + "  if [ ! -f /home/" + selected_user + "/.vnc/passwd ]; then cp -rf /home/" + selected_user + "/.config/tigervnc/passwd /home/" + selected_user + "/.vnc/; fi; " + " fi; " + "fi && sleep 2 && exit"); // 由于是 kali 命令, 我们可以直接发送
            }
        });
        addClickListener(StartVNCButton, v -> {
            if (selected_user.equals("root")) {
                File rootvncpasswd = new File(NhPaths.CHROOT_PATH() + "/root/.vnc/passwd");
                vnc_passwd = exe.RunAsRootOutput("cat " + rootvncpasswd);
            } else {
                File uservncpasswd = new File(NhPaths.CHROOT_PATH() + "/home/" + selected_user + "/.vnc/passwd");
                vnc_passwd = exe.RunAsRootOutput("cat " + uservncpasswd);
            }
            if (delayCheckBox.isChecked()) {
                sharedpreferences.edit().putInt("delaysec", Integer.parseInt(delayText.getText().toString())).apply();
                delay_cmd = "echo \"正在休眠 " + delayText.getText().toString() + " 秒以避免软重启\" && sleep " + delayText.getText().toString() + ";";
            }
            if (vnc_passwd.isEmpty()) {
                Toast.makeText(requireActivity().getApplicationContext(), "请先设置本地服务器!", Toast.LENGTH_SHORT).show();
            } else {
                String arch_path = exe.RunAsRootOutput("ls " + NhPaths.CHROOT_PATH() + "/usr/lib/ | grep linux-gnu | head -n1");
                Toast.makeText(requireActivity().getApplicationContext(), "正在启动服务器.. 请在 NetHunter 应用中刷新状态. ", Toast.LENGTH_LONG).show();
                if(selected_user.equals("root")) {
                    exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus start"});
                    run_cmd("echo -ne \"\\033]0;启动服务器\\007\" && clear;" + delay_cmd + "HOME=/root;USER=root;sudo -u root LD_PRELOAD=/usr/lib/" + arch_path +
                            "/libgcc_s.so.1 nohup vncserver :" + selected_display + " " + localhostonly + "-name \"NetHunter KeX\" " + selected_vncresCMD + " >/dev/null 2>&1 </dev/null; echo \"服务器已启动! 正在关闭终端..\" && sleep 2 && exit");
                } else {
                    exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus start"});
                    run_cmd("echo -ne \"\\033]0;启动服务器\\007\" && clear;" + delay_cmd + "HOME=/home/" + selected_user + ";USER=" + selected_user + ";sudo -u " + selected_user + " LD_PRELOAD=/usr/lib/" + arch_path +
                            "/libgcc_s.so.1 nohup vncserver :" + selected_display + " " + localhostonly + "-name \"NetHunter KeX\" " + selected_vncresCMD + " >/dev/null 2>&1 </dev/null; echo \"服务器已启动! 正在关闭终端..\" && sleep 2 && exit");
                }
                Log.d(TAG, localhostonly);
            }
        });
        final TextView KeXstatus = rootView.findViewById(R.id.KeXstatus);
        addClickListener(StopVNCButton, v -> {
            if (KeXstatus.getText().toString().equals("已停止")) Toast.makeText(requireActivity().getApplicationContext(), "没有活跃的会话!" , Toast.LENGTH_LONG).show();
            else {
                exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd sudo -u " + selected_user+ " vncserver -kill :" + selected_display}); // 由于是 kali 命令, 我们可以直接发送
                dbusDialog();
                refreshVNC(rootView);
                Toast.makeText(requireActivity().getApplicationContext(), "正在停止显示 :" + selected_display + " 对于用户 " + selected_user , Toast.LENGTH_LONG).show();
            }
        });
        addClickListener(OpenVNCButton, v -> {
            intentClickListener_VNC(); // 由于是 kali 命令, 我们可以直接发送
        });
        addClickListener(Advanced, v -> {
            if (!showingAdvanced) {
                AdvancedView.setVisibility(View.VISIBLE);
                Advanced.setText(R.string.vnc_hide_advanced_settings2);
                showingAdvanced = true;
                sharedpreferences.edit().putBoolean("advanced_visible", true).apply();
            } else {
                AdvancedView.setVisibility(View.GONE);
                Advanced.setText(R.string.vnc_show_advanced_settings);
                showingAdvanced = false;
                sharedpreferences.edit().putBoolean("advanced_visible", false).apply();
            }
        });
        addClickListener(AddUserButton, v -> run_cmd("echo -ne \"\\033]0;新用户\\007\" && clear;if [[ $SHELL == *zsh ]];then read \"?请输入您的新用户名\"$'\n' USER;elif [[ $SHELL == *bash ]];then read -p \"请输入您的新用户名\"$'\n' USER;fi && adduser --firstuid " + MIN_UID + " --lastuid " + MAX_UID + " $USER; groupmod -g $(id -u $USER) $USER; usermod -aG sudo $USER; usermod -aG inet $USER; usermod -aG sockets $USER; echo \"请刷新您的 KeX 管理器, 窗口将在 2 秒后关闭\" && sleep 2 && exit"));
        addClickListener(DelUserButton, v -> {
            if (selected_user.contains("root")) {
                Toast.makeText(requireActivity().getApplicationContext(), "无法移除 root!", Toast.LENGTH_SHORT).show();
            } else {
                run_cmd("echo -ne \"\\033]0;移除用户\\007\" && clear;deluser -remove-home " + selected_user + " && sleep 2 && exit");
            }
        });
        addClickListener(ResetHDMIButton, v -> {
            run_cmd_android("wm size reset;wm density reset;am start com.offsec.nethunter/.AppNavHomeActivity -e \":android:show_fragment\" com.offsec.nethunter.VNCFragment;sleep 2 && exit");
            sharedpreferences.edit().putBoolean("confirm_res", false).apply();
        });
        addClickListener(BackupHDMI, v -> {
            exe.RunAsRoot(new String[]{"cp " + hdmiResFile + " " + NhPaths.SD_PATH});
            Toast.makeText(requireActivity().getApplicationContext(), "备份成功!", Toast.LENGTH_SHORT).show();
        });
        addClickListener(RestoreHDMI, v -> {
            String hdmibackup = exe.RunAsRootOutput("cat " + NhPaths.SD_PATH + "/hdmi-resolutions");
            if (hdmibackup.isEmpty()) {
                Toast.makeText(requireActivity().getApplicationContext(), "未找到备份文件!", Toast.LENGTH_SHORT).show();
            } else {
                exe.RunAsRoot(new String[]{"cp " + NhPaths.SD_PATH + "/hdmi-resolutions " + hdmiResFile});
                reload();
                Toast.makeText(requireActivity().getApplicationContext(), "恢复成功!", Toast.LENGTH_SHORT).show();
            }
        });
        addClickListener(AddResolutionButton, v -> openResolutionDialog());
        addClickListener(ApplyResolutionButton, v -> {
            run_cmd_android("wm size " + selected_disp + "; wm density " + selected_ppi + ";am start com.offsec.nethunter/.AppNavHomeActivity -e \":android:show_fragment\" com.offsec.nethunter.VNCFragment;sleep 2 && exit");
            sharedpreferences.edit().putBoolean("confirm_res", true).apply();
        });
        addClickListener(DelResolutionButton, v -> {
            if (!selected_res.equals("1080x1920:300ppi")) {
                exe.RunAsRoot(new String[]{"sed -i '/^" + selected_res + "$/d' " + hdmiResFile});
                reload();
            } else
                Toast.makeText(requireActivity().getApplicationContext(), "无法移除默认分辨率!", Toast.LENGTH_SHORT).show();
        });
        addClickListener(AddVNCResolutionButton, v -> openVNCResolutionDialog());
        addClickListener(DelVNCResolutionButton, v -> {
            if (selected_vncres.equals("Auto")) {
                Toast.makeText(requireActivity().getApplicationContext(), "无法移除默认分辨率!", Toast.LENGTH_SHORT).show();
            } else if (selected_vncres.equals(device_res)) {
                Toast.makeText(requireActivity().getApplicationContext(), "无法移除设备分辨率!", Toast.LENGTH_SHORT).show();
            } else {
                exe.RunAsRoot(new String[]{"sed -i '/^" + selected_vncres + "$/d' " + vncResFile});
                reload();
            }
        });
        addClickListener(BackupVNC, v -> {
            exe.RunAsRoot(new String[]{"cp " + vncResFile + " " + NhPaths.SD_PATH});
            Toast.makeText(requireActivity().getApplicationContext(), "备份成功!", Toast.LENGTH_SHORT).show();
        });
        addClickListener(RestoreVNC, v -> {
            String vncbackup = exe.RunAsRootOutput("cat " + NhPaths.SD_PATH + "/vnc-resolutions");
            if (vncbackup.isEmpty()) {
                Toast.makeText(requireActivity().getApplicationContext(), "未找到备份文件!", Toast.LENGTH_SHORT).show();
            } else {
                exe.RunAsRoot(new String[]{"cp " + NhPaths.SD_PATH + "/vnc-resolutions " + vncResFile});
                reload();
                Toast.makeText(requireActivity().getApplicationContext(), "恢复成功!", Toast.LENGTH_SHORT).show();
            }
        });
        return rootView;
    }

    // 检查 su 是否可用的辅助方法
    private boolean isSuAvailable() {
        try {
            Process process = Runtime.getRuntime().exec("which su");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            if (output != null && output.contains("su")) {
                Log.d("KeXAudio", "su 可用. ");
                return true;
            } else {
                Log.w("KeXAudio", "环境中 su 不可用. ");
                return false;
            }
        } catch (IOException e) {
            Log.e("KeXAudio", "检查 su 可用性时出错", e);
            return false;
        }
    }

    // 检查用户权限的辅助方法
    private boolean checkUserPermissions(String user) {
        if (!isSuAvailable()) return false;  // 如果 sudo 不可用, 提前返回

        try {
            Process process = Runtime.getRuntime().exec("sudo -l -U " + user);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output;
            while ((output = reader.readLine()) != null) {
                if (output.contains("NOPASSWD")) {
                    Log.d("KeXAudio", "用户 " + user + " 具有 NOPASSWD sudo 权限. ");
                    return true;
                }
            }
            Log.d("KeXAudio", "用户 " + user + " 没有 NOPASSWD sudo 权限. ");
            return false;
        } catch (IOException e) {
            Log.e("KeXAudio", "检查用户权限时出错 " + user, e);
            return false;
        }
    }

    private void reload() {
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, VNCFragment.newInstance(0))
                .addToBackStack(null)
                .commit();
    }

    private void refreshVNC(View VNCFragment) {
        final TextView KeXstatus = VNCFragment.findViewById(R.id.KeXstatus);
        final TextView KeXuser = VNCFragment.findViewById(R.id.KeXuser);
        final Button StartAudioButton = VNCFragment.findViewById(R.id.vnc_audio);

        // 服务器状态
        ShellExecuter exe = new ShellExecuter();
        String kex_userCmd;
        String kex_statusCmd = exe.RunAsRootOutput("pidof Xtigervnc");
        if (kex_statusCmd.isEmpty()) {
            KeXstatus.setText(R.string.vnc_stopped);
            KeXuser.setText(R.string.vnc_kexuser_none);
        }
        else {
            KeXstatus.setText(R.string.vnc_running);
            kex_userCmd = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd ps -ef | grep vnc | grep Xauthority | awk '{gsub(/home/,\"\")} {gsub(/\\//,\"\")} {gsub(/.Xauthority/,\"\")} {print $1 $9}'");
            KeXuser.setText(kex_userCmd);
        }

        // 用户
        File passwd = new File(NhPaths.CHROOT_PATH() + "/etc/passwd");
        String commandUSR = ("echo root && " + BUSYBOX_NH + " awk -F':' -v \"min=" + MIN_UID + "\" -v \"max=" + MAX_UID + "\" '{ if ( ( $3 >= min && $3 <= max ) || ( $3 >= 100000 && $3 <= 101000 ) ) print $0}' " + passwd + " | " + BUSYBOX_NH + " cut -d: -f1");
        String outputUSR = exe.RunAsRootOutput(commandUSR);
        final String[] userArray = outputUSR.split("\n");
        Arrays.sort(userArray);
        Spinner users = VNCFragment.findViewById(R.id.user);
        ArrayAdapter<String> usersadapter = new ArrayAdapter<>(requireContext(),android.R.layout.simple_list_item_1, userArray);
        users.setAdapter(usersadapter);
        SharedPreferences sharedpreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        posd = sharedpreferences.getInt("display", 0);
        Spinner displays = VNCFragment.findViewById(R.id.display);
        displays.setSelection(posd);
        prevusr = sharedpreferences.getString("user", "");
        posu = usersadapter.getPosition(prevusr);
        users.setSelection(posu);

        // 音频按钮
        String audio = exe.RunAsRootOutput("pidof pulseaudio");
        if (audio.isEmpty()) StartAudioButton.setText(R.string.vnc_enable_audio2);
        else StartAudioButton.setText(R.string.vnc_disable_audio2);
    }

    private void openResolutionDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.resolutiondialog, null);
        builder.setView(dialogView);
        builder.setTitle("添加新的设备分辨率（竖屏）");
        final EditText width = dialogView.findViewById(R.id.width);
        final EditText height = dialogView.findViewById(R.id.height);
        final EditText density = dialogView.findViewById(R.id.density);
        File hdmiResFile = new File(NhPaths.APP_SD_FILES_PATH + "/configs/hdmi-resolutions");
        ShellExecuter exe = new ShellExecuter();
        builder.setPositiveButton("添加", (dialog, which) -> {
            final String add_width = width.getText().toString();
            final String add_height = height.getText().toString();
            final String add_density = density.getText().toString();
            if (add_width.isEmpty() || add_height.isEmpty() || add_density.isEmpty()) {
                Toast.makeText(requireActivity().getApplicationContext(), "请输入数值!", Toast.LENGTH_SHORT).show();
                openResolutionDialog();
            } else if (Integer.parseInt(width.getText().toString()) > Integer.parseInt(height.getText().toString())){
                MaterialAlertDialogBuilder builder2 = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
                builder2.setTitle("宽度大于高度!");
                builder2.setMessage("较大的宽度通常仅用于平板电脑. 配置错误可能导致设备无响应");
                builder2.setPositiveButton("保持", (dialog2, which1) -> {
                    exe.RunAsRoot(new String[]{"echo " + add_width + "x" + add_height + ":" + add_density + "ppi >> " + hdmiResFile});
                    reload();
                });
                builder2.setNegativeButton("返回", (dialog2, whichButton) -> openResolutionDialog());
                builder2.show();
            } else {
                exe.RunAsRoot(new String[]{"echo " + add_width + "x" + add_height+ ":" + add_density + "ppi >> " + hdmiResFile});
                reload();
            }
        });
        builder.setNegativeButton("取消", (dialog, whichButton) -> {
        });
        builder.show();
    }

    private void openVNCResolutionDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.vncresolutiondialog, null);
        builder.setView(dialogView);
        builder.setTitle("添加新的 VNC 服务器分辨率（横屏）");
        final EditText width = dialogView.findViewById(R.id.width);
        final EditText height = dialogView.findViewById(R.id.height);
        File vncResFile = new File(NhPaths.APP_SD_FILES_PATH + "/configs/vnc-resolutions");
        ShellExecuter exe = new ShellExecuter();
        builder.setPositiveButton("添加", (dialog, which) -> {
            final String add_width = width.getText().toString();
            final String add_height = height.getText().toString();
            if (add_width.isEmpty() || add_height.isEmpty()) {
                Toast.makeText(requireActivity().getApplicationContext(), "请输入数值!", Toast.LENGTH_SHORT).show();
                openResolutionDialog();
            } else {
                exe.RunAsRoot(new String[]{"echo " + add_width + "x" + add_height + " >> " + vncResFile});
                reload();
            }
        });
        builder.setNegativeButton("取消", (dialog, whichButton) -> {
        });
        builder.show();
    }

    private void confirmDialog() {
        SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        final AlertDialog alert = getAlertDialog(sharedpreferences);
        CountDownTimer resetResolution = new CountDownTimer(15000, 1000) {
            @Override
            public void onTick(long l) {
                alert.setMessage("将在 "+ l/1000 + " 秒后重置设备分辨率");
            }
            @Override
            public void onFinish() {
                ShellExecuter exe =new ShellExecuter();
                exe.RunAsRoot(new String[]{"wm size reset; wm density reset"});
                sharedpreferences.edit().putBoolean("confirm_res", false).apply();
            }
        }.start();
        alert.setButton(DialogInterface.BUTTON_POSITIVE,"保持分辨率", (dialog, which) -> {
            sharedpreferences.edit().putBoolean("confirm_res", false).apply();
            alert.cancel();
            resetResolution.cancel();
        });
    }

    @NonNull
    private AlertDialog getAlertDialog(SharedPreferences sharedpreferences) {
        final MaterialAlertDialogBuilder confirmbuilder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        confirmbuilder.setTitle("您想保持此分辨率吗?");
        confirmbuilder.setMessage("加载中..");
        confirmbuilder.setPositiveButton("保持分辨率", (dialogInterface, i) -> {
            sharedpreferences.edit().putBoolean("confirm_res", false).apply();
            dialogInterface.cancel();
        });
        final AlertDialog alert = confirmbuilder.create();
        alert.show();
        return alert;
    }

    private void dbusDialog() {
        final MaterialAlertDialogBuilder dbusbuilder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        ShellExecuter exe = new ShellExecuter();
        dbusbuilder.setMessage("您想停止 dbus 服务吗? 如果您没有其他会话打开, 请按是. ");
        dbusbuilder.setPositiveButton("是", (dialogInterface, i) -> exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus stop"}));
        dbusbuilder.setNegativeButton("否", (dialog, whichButton) -> {
        });
        dbusbuilder.show();
    }

    private void desktopDialog() {
        final MaterialAlertDialogBuilder dbusbuilder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        ShellExecuter exe = new ShellExecuter();
        dbusbuilder.setMessage("未安装桌面环境. 您想安装 kali-desktop-xfce 吗?");
        dbusbuilder.setPositiveButton("是", (dialogInterface, i) -> run_cmd("echo -ne \"\\033]0;安装 XFCE\\007\" && clear;apt update && apt install -y kali-desktop-xfce tigervnc-standalone-server dbus-x11;apt clean; echo '完成! 正在退出..' && sleep 2 && exit"));
        dbusbuilder.setNegativeButton("否", (dialog, whichButton) -> {
        });
        dbusbuilder.show();
    }

    private void addClickListener(Button _button, View.OnClickListener onClickListener) {
        _button.setOnClickListener(onClickListener);
    }

    private void intentClickListener_VNC() {
        try {
            if (getView() == null)
                return;
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.offsec.nethunter.kex");
            startActivity(intent);
        } catch (Exception e) {
            Log.d("errorLaunching", e.toString());
            NhPaths.showMessage(context, "未找到 NetHunter KeX!");
        }
    }

    private void vncLocalClick() {
        // 占位符实现
        Toast.makeText(context, "vncLocalClick 已触发", Toast.LENGTH_SHORT).show();
    }

    ////
    // Bridge 端函数
    ////

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }

    public void run_cmd_android(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/android-su", cmd);
        activity.startActivity(intent);
    }
}