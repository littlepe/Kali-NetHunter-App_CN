package com.offsec.nethunter;

import android.os.Handler;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.ArrayList;
import java.util.Objects;

public class BTFragment extends Fragment {
    private ViewPager mViewPager;
    private SharedPreferences sharedpreferences;
    private Context context;
    private Activity activity;
    private final ShellExecuter exe = new ShellExecuter(); // 修复了未定义 'exe' 的问题
    private static final String ARG_SECTION_NUMBER = "section_number";

    public static BTFragment newInstance(int sectionNumber) {
        BTFragment fragment = new BTFragment();
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
        View rootView = inflater.inflate(R.layout.bt, container, false);
        BTFragment.TabsPagerAdapter tabsPagerAdapter = new TabsPagerAdapter(getChildFragmentManager());

        mViewPager = rootView.findViewById(R.id.pagerBt);
        mViewPager.setAdapter(tabsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                activity.invalidateOptionsMenu();
            }
        });
        if (activity != null) {
            sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        }
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater menuinflater) {
        menuinflater.inflate(R.menu.bt, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        Boolean iswatch = sharedpreferences.getBoolean("running_on_wearos", false);

        switch (item.getItemId()) {
            case R.id.setup:
                if (iswatch) RunSetupWatch();
                else RunSetup();
                return true;
            case R.id.update:
                if (iswatch) {
                    Toast.makeText(requireActivity().getApplicationContext(), "更新必须通过 adb shell 手动完成. 如果首次运行出现任何问题, 请再次运行设置. ", Toast.LENGTH_LONG).show();
                } else {
                    RunUpdate();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void SetupDialog() {
        if (activity != null) {
            sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        }
        Boolean iswatch = sharedpreferences.getBoolean("running_on_wearos", false);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        builder.setTitle("欢迎使用蓝牙武器库！");
        builder.setMessage("这似乎是首次运行. 要安装蓝牙工具吗？");
        builder.setPositiveButton("安装", (dialog, which) -> {
            if (iswatch) RunSetupWatch();
            else RunSetup();
            sharedpreferences.edit().putBoolean("setup_done", true).apply();
        });
        builder.setNegativeButton("禁用消息", (dialog, which) -> {
            dialog.dismiss();
            sharedpreferences.edit().putBoolean("setup_done", true).apply();
        });
        builder.show();
    }

    public void SetupDialogWatch() {
        if (activity != null) {
            sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        builder.setMessage("这似乎是首次运行. 要安装蓝牙工具吗？");
        builder.setPositiveButton("是", (dialog, which) -> {
            RunSetupWatch();
            sharedpreferences.edit().putBoolean("setup_done", true).apply();
        });
        builder.setNegativeButton("否", (dialog, which) -> {
            dialog.dismiss();
            sharedpreferences.edit().putBoolean("setup_done", true).apply();
        });
        builder.show();
    }

    public void RunSetupWatch() {
        if (activity != null) {
            sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        }
        run_cmd("echo -ne \"\\033]0;BT Arsenal Setup\\007\" && clear;" +
                "apt update && apt install screen bluetooth bluez bluez-tools bluez-obexd libbluetooth3 sox spooftooph libglib2.0*-dev " +
                "libsystemd-dev python3-dbus python3-bluez python3-pyudev python3-evdev libbluetooth-dev redfang bluelog blueranger -y;" +
                "if [[ -f /usr/sbin/bluebinder ]]; then echo 'Bluebinder 已安装！'; else wget https://raw.githubusercontent.com/yesimxev/bluebinder/master/prebuilt/armhf/bluebinder -P /usr/sbin/ && chmod +x /usr/sbin/bluebinder;fi;" +
                "if [[ -f /usr/lib/libgbinder.so.1.1.25 ]]; then echo 'libgbinder.so.1.1.25 已安装！'; else wget https://raw.githubusercontent.com/yesimxev/libgbinder/master/prebuilt/armhf/libgbinder.so.1.1.25 -P /usr/lib/ &&" +
                " ln -s libgbinder.so.1.1.25 /usr/lib/libgbinder.so.1.1 && ln -s libgbinder.so.1.1 /usr/lib/libgbinder.so.1 && ln -s libgbinder.so.1 /usr/lib/libgbinder.so;fi;" +
                "if [[ -f /usr/lib/libglibutil.so.1.0.67 ]]; then echo 'libglibutil.so.1.0.67 已安装！'; else wget https://raw.githubusercontent.com/yesimxev/libglibutil/master/prebuilt/armhf/libglibutil.so.1.0.67 -P /usr/lib/ &&" +
                " ln -s libglibutil.so.1.0.67 /usr/lib/libglibutil.so.1.0 && ln -s libglibutil.so.1.0 /usr/lib/libglibutil.so.1 && ln -s libglibutil.so.1 /usr/lib/libglibutil.so;fi;" +
                "if [[ -f /usr/bin/carwhisperer ]]; then echo 'carwhisperer 已安装！'; else wget https://raw.githubusercontent.com/yesimxev/carwhisperer-0.2/master/prebuilt/armhf/carwhisperer -P /usr/bin/ && chmod +x /usr/bin/carwhisperer;fi;" +
                "if [[ -f /usr/bin/rfcomm_scan ]]; then echo 'rfcomm_scan 已安装！'; else wget https://raw.githubusercontent.com/yesimxev/bt_audit/master/prebuilt/armhf/rfcomm_scan -P /usr/bin/ && chmod +x /usr/bin/rfcomm_scan;fi;" +
                "if [[ -d /root/carwhisperer ]]; then echo '/root/carwhisperer 已安装！'; else git clone https://github.com/yesimxev/carwhisperer-0.2 /root/carwhisperer;fi;" +
                "if [[ -f /root/badbt/btk_server.py ]]; then echo 'BadBT 已安装！'; else git clone https://github.com/yesimxev/badbt /root/badbt && cp /root/badbt/org.thanhle.btkbservice.conf /etc/dbus-1/system.d/;fi;" +
                "if [[ ! \"`grep 'noplugin=input' /etc/init.d/bluetooth`\" == \"\" ]]; then echo '蓝牙服务已修补！'; else echo '正在修补蓝牙服务..' && " +
                "sed -i -e 's/# NOPLUGIN_OPTION=.*/NOPLUGIN_OPTION=\"--noplugin=input\"/g' /etc/init.d/bluetooth;fi;" +
                "echo '所有内容已安装！将在 3 秒后关闭..'; sleep 3 && exit ");
        sharedpreferences.edit().putBoolean("setup_done", true).apply();
    }

    public void RunSetup() {
        if (activity != null) {
            sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        }
        run_cmd("echo -ne \"\\033]0;BT Arsenal Setup\\007\" && clear;apt update && apt install screen bluetooth bluez bluez-tools bluez-obexd libbluetooth3 sox spooftooph libglib2.0*-dev " +
                "libsystemd-dev python3-dbus python3-bluez python3-pyudev python3-evdev libbluetooth-dev redfang bluelog blueranger -y;" +
                "if [[ -f /usr/bin/carwhisperer && -f /usr/bin/rfcomm_scan ]];then echo '所有脚本已安装！'; else " +
                "git clone https://github.com/yesimxev/carwhisperer-0.2 /root/carwhisperer;" +
                "cd /root/carwhisperer;make && make install;git clone https://github.com/yesimxev/bt_audit /root/bt_audit;cd /root/bt_audit/src;make;" +
                "cp rfcomm_scan /usr/bin/;fi;" +
                "if [[ -f /usr/lib/libglibutil.so ]]; then echo 'Libglibutil 已安装！'; else git clone https://github.com/yesimxev/libglibutil /root/libglibutil;" +
                "cd /root/libglibutil;make && make install-dev;fi;" +
                "if [[ -f /usr/lib/libgbinder.so ]]; then echo 'Libgbinder 已安装！'; else git clone https://github.com/yesimxev/libgbinder /root/libgbinder;" +
                "cd /root/libgbinder;make && make install-dev;fi;" +
                "if [[ -f /usr/sbin/bluebinder ]]; then echo 'Bluebinder 已安装！'; else git clone https://github.com/yesimxev/bluebinder /root/bluebinder;" +
                "cd /root/bluebinder;make && make install;fi;" +
                "if [[ -f /root/badbt/btk_server.py ]]; then echo 'BadBT 已安装！'; else git clone https://github.com/yesimxev/badbt /root/badbt && cp /root/badbt/org.thanhle.btkbservice.conf /etc/dbus-1/system.d/;fi;" +
                "if [[ ! \"`grep 'noplugin=input' /etc/init.d/bluetooth`\" == \"\" ]]; then echo '蓝牙服务已修补！'; else echo '正在修补蓝牙服务..' && " +
                "sed -i -e 's/.*NOPLUGIN_OPTION=\"\"/NOPLUGIN_OPTION=\"--noplugin=input\"/g' /etc/init.d/bluetooth;fi; echo '所有内容已安装！' && echo '\n按任意键继续...' && read -s -n 1 && exit ");
        sharedpreferences.edit().putBoolean("setup_done", true).apply();
    }

    public void RunUpdate() {
        if (activity != null) {
            sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        }
        run_cmd("echo -ne \"\\033]0;BT Arsenal Update\\007\" && clear;apt update && apt install screen bluetooth bluez bluez-tools bluez-obexd libbluetooth3 sox spooftooph " +
                "libbluetooth-dev redfang bluelog blueranger libglib2.0*-dev libsystemd-dev python3-dbus python3-bluez python3-pyudev python3-evdev  -y;if [[ -f /usr/bin/carwhisperer && -f /usr/bin/rfcomm_scan && -f /root/bluebinder && -f /root/libgbinder && -f /root/libglibutil ]];" +
                "then cd /root/carwhisperer/;git pull && make && make install;cd /root/bluebinder/;git pull && make && make install;cd /root/libgbinder/;git pull && make && " +
                "make install-dev;cd /root/libglibutil/;git pull && make && make install-dev;cd /root/bt_audit; git pull; cd src && make;" +
                "cp rfcomm_scan /usr/bin/;cd /root/badbt/;git pull;fi; echo '完成！将在 3 秒后关闭..'; sleep 3 && exit ");
        sharedpreferences.edit().putBoolean("setup_done", true).apply();
    }

    public static class TabsPagerAdapter extends FragmentPagerAdapter {
        TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new MainFragment();
                case 1:
                    return new ToolsFragment();
                case 2:
                    return new SpoofFragment();
                case 3:
                    return new CWFragment();
                default:
                    return new BadBtFragment();
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public int getCount() {
            return 5;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 4:
                    return "Bad Bluetooth";
                case 3:
                    return "Carwhisperer";
                case 2:
                    return "欺骗";
                case 1:
                    return "工具";
                case 0:
                    return "主页面";
                default:
                    return "";
            }
        }
    }

    public static class MainFragment extends BTFragment {
        private Context context;
        final ShellExecuter exe = new ShellExecuter();
        private String selected_iface;
        private Boolean iswatch;
        String selected_addr;
        String selected_class;
        String selected_name;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
        }
        @Override
        public void onResume(){
            super.onResume();
            Toast.makeText(requireActivity().getApplicationContext(), "状态已更新", Toast.LENGTH_SHORT).show();
            Executors.newSingleThreadExecutor().execute(() -> refresh(requireView().getRootView()));
        }
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.bt_main, container, false);
            SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

            // 检测手表
            final TextView BTMainDesc = rootView.findViewById(R.id.bt_maindesc);
            final TextView BTIface = rootView.findViewById(R.id.bt_if);
            final TextView BTService = rootView.findViewById(R.id.bt_service);

            iswatch = sharedpreferences.getBoolean("running_on_wearos", false);
            if (iswatch) {
                BTMainDesc.setVisibility(View.GONE);
                BTIface.setText(R.string.bt_interface);
                BTService.setText(R.string.bt_service);
            }

            // 首次运行
            Boolean setupdone = sharedpreferences.getBoolean("setup_done", false);
            if (!setupdone.equals(true)) {
                if (iswatch) SetupDialogWatch();
                else SetupDialog();
            }

            final Spinner ifaces = rootView.findViewById(R.id.hci_interface);

            // Bluebinder 或 bt_smd
            final TextView Binder = rootView.findViewById(R.id.bluebinder);
            File bt_smd = new File("/sys/module/hci_smd/parameters/hcismd_set");
            if (bt_smd.exists()) {
                Binder.setText(R.string.bt_smd);
            }

            // 蓝牙接口
            final String[] outputHCI = {""};
            Executors.newSingleThreadExecutor().execute(() -> outputHCI[0] = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig | grep hci | cut -d: -f1"));
            final ArrayList<String> hciIfaces = new ArrayList<>();
            if (outputHCI[0].isEmpty()) {
                hciIfaces.add("无");
                ifaces.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, hciIfaces));
            } else {
                final String[] ifacesArray = outputHCI[0].split("\n");
                ifaces.setAdapter(new ArrayAdapter<>(requireContext(),android.R.layout.simple_list_item_1, ifacesArray));
            }

            ifaces.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    selected_iface = parentView.getItemAtPosition(pos).toString();
                    sharedpreferences.edit().putInt("selected_iface", ifaces.getSelectedItemPosition()).apply();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                    // TODO 说明此方法为空的原因
                }
            });

            // 刷新状态
            ImageButton RefreshStatus = rootView.findViewById(R.id.refreshStatus);
            RefreshStatus.setOnClickListener(v -> refresh(rootView));
            Executors.newSingleThreadExecutor().execute(() -> refresh(rootView));

            // 内部蓝牙支持
            final Button bluebinderButton = rootView.findViewById(R.id.bluebinder_button);
            final Button dbusButton = rootView.findViewById(R.id.dbus_button);
            final Button btButton = rootView.findViewById(R.id.bt_button);
            final Button hciButton = rootView.findViewById(R.id.hci_button);
            File hwbinder = new File("/dev/hwbinder");
            File vhci = new File("/dev/vhci");

            bluebinderButton.setOnClickListener( v -> {
                if (bluebinderButton.getText().equals("启动")) {
                    if (!bt_smd.exists() && !hwbinder.exists() && !vhci.exists()) {
                        final MaterialAlertDialogBuilder confirmbuilder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
                        confirmbuilder.setTitle("内部蓝牙支持已禁用");
                        confirmbuilder.setMessage("您的设备不支持 hwbinder、vhci 或 bt_smd. 请确保您的内核配置已启用推荐的驱动程序, 以便使用内部蓝牙. ");
                        confirmbuilder.setPositiveButton("确定", (dialogInterface, i) -> {
                            bluebinderButton.setEnabled(false);
                            bluebinderButton.setTextColor(Color.parseColor("#40FFFFFF"));
                            dialogInterface.cancel();
                        });
                        confirmbuilder.setNegativeButton("仍然尝试", (dialogInterface, i) -> dialogInterface.cancel());
                        final AlertDialog alert = confirmbuilder.create();
                        alert.show();
                    } else {
                        if (bt_smd.exists()) {
                            exe.RunAsRoot(new String[]{"svc bluetooth disable"});
                            exe.RunAsRoot(new String[]{"echo 0 > " + bt_smd});
                            exe.RunAsRoot(new String[]{"echo 1 > " + bt_smd});
                            exe.RunAsRoot(new String[]{"svc bluetooth enable"});
                        }
                        else {
                            File bluebinder = new File(NhPaths.CHROOT_PATH() + "/usr/sbin/bluebinder");
                            if (bluebinder.exists()) {
                                // TODO - 仅为特定设备启用此功能 1/2
                                //在启用飞行模式以运行 bluebinder 之前, 确保所有服务都已禁用
                                //exe.RunAsRoot(new String[]{
                                //"svc bluetooth disable",
                                //"svc wifi disable",
                                //"settings put global airplane_mode_on 1;am broadcast -a android.intent.action.AIRPLANE_MODE --ez state true",
                                //"pm disable com.android.bluetooth"
                                //});

                                // 运行 Bluebinder 脚本
                                run_cmd("echo -ne \"\\033]0;Bluebinder\\007\" && clear;bluebinder || bluebinder;exit");
                                Toast.makeText(requireActivity().getApplicationContext(), "正在启动 bluebinder...", Toast.LENGTH_SHORT).show();

                                // TODO - 仅为特定设备启用此功能 2/2
                                // 延迟 9 秒后禁用飞行模式并重新启用 Wi-Fi
                                /*new Handler().postDelayed(() -> exe.RunAsRoot(new String[]{
                                    "settings put global airplane_mode_on 0;am broadcast -a android.intent.action.AIRPLANE_MODE --ez state false",
                                    "svc wifi enable"
                                }), 9000); // 9000 毫秒延迟*/
                            } else {
                                Toast.makeText(requireActivity().getApplicationContext(), "Bluebinder 未安装. 正在启动设置..", Toast.LENGTH_SHORT).show();
                                RunSetup();
                            }
                        }
                        refresh(rootView);
                    }
                } else if (bluebinderButton.getText().equals("停止")) {
                    if (bt_smd.exists()) {
                        exe.RunAsRoot(new String[]{"echo 0 > " + bt_smd});
                    }
                    else {
                        exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd pkill bluebinder;exit"});
                        exe.RunAsRoot(new String[]{"pm enable com.android.bluetooth"});
                        exe.RunAsRoot(new String[]{"svc bluetooth enable"});
                    }
                    refresh(rootView);
                }
            });

            // 服务
            dbusButton.setOnClickListener( v -> {
                if (dbusButton.getText().equals("启动")) {
                    exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus start"});
                    refresh(rootView);
                } else if (dbusButton.getText().equals("停止")) {
                    exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus stop"});
                    refresh(rootView);
                }
            });

            btButton.setOnClickListener( v -> {
                String dbus_statusCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus status | grep dbus");
                if (dbus_statusCMD.equals("dbus is running.")) {
                    if (btButton.getText().equals("启动")) {
                        exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service bluetooth start"});
                        refresh(rootView);
                    } else if (btButton.getText().equals("停止")) {
                        exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service bluetooth stop"});
                        refresh(rootView);
                    }
                } else {
                    Toast.makeText(requireActivity().getApplicationContext(), "请先启用 dbus 服务！", Toast.LENGTH_SHORT).show();
                }
            });

            hciButton.setOnClickListener( v -> {
                if (hciButton.getText().equals("启动")) {
                    if (selected_iface.equals("无")) {
                        Toast.makeText(requireActivity().getApplicationContext(), "没有接口, 请刷新或检查连接！", Toast.LENGTH_SHORT).show();
                    } else {
                        exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig " + selected_iface + " up noscan"});
                        refresh(rootView);
                    }
                } else if (hciButton.getText().equals("停止")) {
                    exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig " + selected_iface + " down"});
                    refresh(rootView);
                }
            });

            // 扫描
            Button StartScanButton = rootView.findViewById(R.id.start_scan);
            final TextView BTtime = rootView.findViewById(R.id.bt_time);
            ListView targets = rootView.findViewById(R.id.targets);
            ShellExecuter exe = new ShellExecuter();
            File ScanLog = new File(NhPaths.CHROOT_PATH() + "/root/blue.log");
            StartScanButton.setOnClickListener( v -> {
                if (!selected_iface.equals("无")) {
                    String hci_current = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig "+ selected_iface + " | grep 'UP RUNNING' | cut -f2 -d$'\\t'");
                    if (hci_current.equals("UP RUNNING ")) {
                        final String scantime = BTtime.getText().toString();
                        Executors.newSingleThreadExecutor().execute(() -> {
                            requireActivity().runOnUiThread(() -> {
                                final ArrayList<String> scanning = new ArrayList<>();
                                scanning.add("扫描中..");
                                targets.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, scanning));
                            });
                            exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd rm /root/blue.log"});
                            exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd timeout " + scantime + " bluelog -i " + selected_iface + " -ncqo /root/blue.log;hciconfig " + selected_iface + " noscan"});
                            requireActivity().runOnUiThread(() -> {
                                String outputScanLog = exe.RunAsRootOutput("cat " + ScanLog);
                                final String[] targetsArray = outputScanLog.split("\n");
                                ArrayAdapter<String> targetsadapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, targetsArray);
                                if (!outputScanLog.isEmpty()) {
                                    targets.setAdapter(targetsadapter);
                                } else {
                                    final ArrayList<String> notargets = new ArrayList<>();
                                    notargets.add("未找到设备");
                                    targets.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, notargets));
                                }
                            });
                        });
                    } else
                        Toast.makeText(requireActivity().getApplicationContext(), "接口已关闭！", Toast.LENGTH_SHORT).show();
                }
                else {
                    Toast.makeText(requireActivity().getApplicationContext(), "未选择接口！", Toast.LENGTH_SHORT).show();
                }
            });

            // 目标选择
            targets.setOnItemClickListener((adapterView, view, i, l) -> {
                String selected_target = targets.getItemAtPosition(i).toString();
                if (selected_target.equals("未找到设备"))
                    Toast.makeText(requireActivity().getApplicationContext(), "没有目标！", Toast.LENGTH_SHORT).show();
                else {
                    selected_addr = exe.RunAsRootOutput("echo " + selected_target + " | cut -d , -f 1");
                    selected_class = exe.RunAsRootOutput("echo " + selected_target + " | cut -d , -f 2");
                    selected_name = exe.RunAsRootOutput("echo " + selected_target + " | cut -d , -f 3");
                    PreferencesData.saveString(context, "selected_address", selected_addr);
                    PreferencesData.saveString(context, "selected_class", selected_class);
                    PreferencesData.saveString(context, "selected_name", selected_name);
                    Toast.makeText(requireActivity().getApplicationContext(), "目标已选择！", Toast.LENGTH_SHORT).show();
                }
            });
            return rootView;
        }

        // 刷新主页面
        private void refresh(View BTFragment) {
            final TextView Binderstatus = BTFragment.findViewById(R.id.BinderStatus);
            final TextView DBUSstatus = BTFragment.findViewById(R.id.DBUSstatus);
            final TextView BTstatus = BTFragment.findViewById(R.id.BTstatus);
            final TextView HCIstatus = BTFragment.findViewById(R.id.HCIstatus);
            final Button bluebinderButton = BTFragment.findViewById(R.id.bluebinder_button);
            final Button dbusButton = BTFragment.findViewById(R.id.dbus_button);
            final Button btButton = BTFragment.findViewById(R.id.bt_button);
            final Button hciButton = BTFragment.findViewById(R.id.hci_button);
            final Spinner ifaces = BTFragment.findViewById(R.id.hci_interface);
            SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

            requireActivity().runOnUiThread(() -> {
                String outputHCI = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig | grep hci | cut -d: -f1");
                final ArrayList<String> hciIfaces = new ArrayList<>();
                if (outputHCI.isEmpty()) {
                    hciIfaces.add("无");
                    ifaces.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, hciIfaces));
                } else {
                    final String[] ifacesArray = outputHCI.split("\n");
                    ifaces.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, ifacesArray));
                    int lastiface = sharedpreferences.getInt("selected_iface", 0);
                    ifaces.setSelection(lastiface);
                }
                String binder_statusCMD = exe.RunAsRootOutput("pidof bluebinder");
                File bt_smd = new File("/sys/module/hci_smd/parameters/hcismd_set");
                if (!bt_smd.exists()) {
                    if (binder_statusCMD.isEmpty()) {
                        Binderstatus.setText(R.string.bt_stopped);
                        bluebinderButton.setText(R.string.bt_start);
                    }
                    else {
                        Binderstatus.setText(R.string.bt_running);
                        bluebinderButton.setText(R.string.bt_stop);
                    }
                } else {
                    if (outputHCI.contains("hci0")) {
                        Binderstatus.setText(R.string.bt_enabled);
                        bluebinderButton.setText(R.string.bt_stop);
                    } else {
                        Binderstatus.setText(R.string.bt_disabled);
                        bluebinderButton.setText(R.string.bt_start);
                    }
                }
                String dbus_statusCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus status | grep dbus");
                if (dbus_statusCMD.equals("dbus is running.")) {
                    DBUSstatus.setText(R.string.bt_start);
                    dbusButton.setText(R.string.bt_stop);
                }
                else {
                    DBUSstatus.setText(R.string.bt_stopped);
                    dbusButton.setText(R.string.bt_start);
                }
                String bt_statusCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service bluetooth status | grep bluetooth");
                if (bt_statusCMD.equals("bluetooth is running.")) {
                    BTstatus.setText(R.string.bt_running);
                    btButton.setText(R.string.bt_stop);
                }
                else {
                    BTstatus.setText(R.string.bt_stopped);
                    btButton.setText(R.string.bt_start);
                }
                String hci_statusCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig "+ selected_iface + " | grep 'UP RUNNING' | cut -f2 -d$'\\t'");
                if (hci_statusCMD.equals("UP RUNNING ")) {
                    HCIstatus.setText(R.string.bt_up);
                    hciButton.setText(R.string.bt_stop);
                }
                else {
                    HCIstatus.setText(R.string.bt_down);
                    hciButton.setText(R.string.bt_start);
                }
            });
        }
    }

    public static class ToolsFragment extends BTFragment {
        private Context context;
        final ShellExecuter exe = new ShellExecuter();
        private String reverse = "";
        private String flood = "";

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            Activity activity = getActivity();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.bt_tools, container, false);
            final EditText hci_interface = rootView.findViewById(R.id.hci_interface);
            CheckBox floodCheckBox = rootView.findViewById(R.id.l2ping_flood);
            CheckBox reverseCheckBox = rootView.findViewById(R.id.l2ping_reverse);

            // 目标地址
            final EditText sdp_address = rootView.findViewById(R.id.sdp_address);

            // 设置目标
            Button SetTarget = rootView.findViewById(R.id.set_target);

            SetTarget.setOnClickListener( v -> {
                String selected_addr = PreferencesData.getString(context, "selected_address", "");
                sdp_address.setText(selected_addr);
            });

            // L2ping
            Button StartL2ping = rootView.findViewById(R.id.start_l2ping);
            final EditText l2ping_Size = rootView.findViewById(R.id.l2ping_size);
            final EditText l2ping_Count = rootView.findViewById(R.id.l2ping_count);
            final EditText redfang_Range = rootView.findViewById(R.id.redfang_range);
            final EditText redfang_Log = rootView.findViewById(R.id.redfang_log);

            // 洪水攻击和反向 ping 的复选框
            floodCheckBox.setOnClickListener( v -> {
                if (floodCheckBox.isChecked())
                    flood = " -f ";
                else
                    flood = "";
            });
            reverseCheckBox.setOnClickListener( v -> {
                if (reverseCheckBox.isChecked())
                    reverse = " -r ";
                else
                    reverse = "";
            });

            StartL2ping.setOnClickListener( v -> {
                String l2ping_target = sdp_address.getText().toString();
                if (!l2ping_target.isEmpty()) {
                    String l2ping_size = l2ping_Size.getText().toString();
                    String l2ping_count = l2ping_Count.getText().toString();
                    String l2ping_interface = hci_interface.getText().toString();
                    run_cmd("echo -ne \"\\033]0;正在 Ping BT 设备\\007\" && clear;l2ping -i " + l2ping_interface + " -s " + l2ping_size + " -c " + l2ping_count + flood + reverse + " " + l2ping_target + " && echo \"\nPing 完成, 将在 3 秒后关闭..\";sleep 3 && exit");
                } else {
                    Toast.makeText(requireActivity().getApplicationContext(), "没有目标地址！", Toast.LENGTH_SHORT).show();
                }
            });

            // RFComm_scan
            Button StartRFCommscan = rootView.findViewById(R.id.start_rfcommscan);

            StartRFCommscan.setOnClickListener( v -> {
                String sdp_target = sdp_address.getText().toString();
                if (!sdp_target.isEmpty())
                    run_cmd("echo -ne \"\\033]0;RFComm 扫描\\007\" && clear;rfcomm_scan " + sdp_target);
                else
                    Toast.makeText(requireActivity().getApplicationContext(), "没有目标地址！", Toast.LENGTH_SHORT).show();
            });

            // Redfang
            Button StartRedfang = rootView.findViewById(R.id.start_redfang);

            StartRedfang.setOnClickListener( v -> {
                String redfang_range = redfang_Range.getText().toString();
                String redfang_logfile = redfang_Log.getText().toString();
                if (!redfang_range.isEmpty())
                    run_cmd("echo -ne \"\\033]0;Redfang\\007\" && clear;fang -r " + redfang_range + " -o " + redfang_logfile);
                else
                    Toast.makeText(requireActivity().getApplicationContext(), "没有目标范围！", Toast.LENGTH_SHORT).show();
            });

            // Blueranger
            Button StartBlueranger = rootView.findViewById(R.id.start_blueranger);
            StartBlueranger.setOnClickListener( v -> {
                String blueranger_target = sdp_address.getText().toString();
                String blueranger_interface = hci_interface.getText().toString();
                if (!blueranger_target.isEmpty())
                    run_cmd("echo -ne \"\\033]0;Blueranger\\007\" && clear;blueranger " + blueranger_interface + " " + blueranger_target);
                else
                    Toast.makeText(requireActivity().getApplicationContext(), "没有目标地址！", Toast.LENGTH_SHORT).show();
            });

            // 启动 SDP 工具
            Button StartSDPButton = rootView.findViewById(R.id.start_sdp);
            StartSDPButton.setOnClickListener( v -> {
                Toast.makeText(getContext(), "发现已启动..\n请查看下方输出", Toast.LENGTH_SHORT).show();
                Executors.newSingleThreadExecutor().execute(() -> startSDPtool(rootView));
            });
            return rootView;
        }

        private void startSDPtool(View BTFragment) {
            final EditText sdp_address = BTFragment.findViewById(R.id.sdp_address);
            final EditText hci_interface = BTFragment.findViewById(R.id.hci_interface);
            final TextView output = BTFragment.findViewById(R.id.SDPoutput);
            ShellExecuter exe = new ShellExecuter();
            String sdp_target = sdp_address.getText().toString();
            String sdp_interface = hci_interface.getText().toString();

            requireActivity().runOnUiThread(() -> {
                if (!sdp_target.isEmpty()) {
                    String CMDout = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd sdptool -i " + sdp_interface + " browse " + sdp_target + " | sed '/^\\[/d' | sed '/^Linux/d'");
                    output.setText(CMDout);
                } else
                    Toast.makeText(requireActivity().getApplicationContext(), "没有目标地址！", Toast.LENGTH_SHORT).show();
            });
        }
    }

    public static class SpoofFragment extends BTFragment {
        private Context context;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.bt_spoof, container, false);

            // 选择的接口
            final EditText spoof_interface = rootView.findViewById(R.id.spoof_interface);

            // 目标地址
            final EditText targetAddress = rootView.findViewById(R.id.targetAddress);

            // 目标类别
            final EditText targetClass = rootView.findViewById(R.id.targetClass);

            // 目标名称
            final EditText targetName = rootView.findViewById(R.id.targetName);

            // 设置目标
            Button SetTarget = rootView.findViewById(R.id.set_target);

            SetTarget.setOnClickListener(v -> {
                String selected_address = PreferencesData.getString(context, "selected_address", "");
                String selected_class = PreferencesData.getString(context, "selected_class", "");
                String selected_name = PreferencesData.getString(context, "selected_name", "");
                targetAddress.setText(selected_address);
                targetClass.setText(selected_class);
                targetName.setText(selected_name);
            });

            // 刷新
            Button RefreshStatus = rootView.findViewById(R.id.refreshSpoof);
            RefreshStatus.setOnClickListener(v -> refreshSpoof(rootView));

            // 应用
            Button ApplySpoof = rootView.findViewById(R.id.apply_spoof);

            ApplySpoof.setOnClickListener(v -> {
                String target_interface = spoof_interface.getText().toString();
                String target_address = " -a " + targetAddress.getText().toString();
                String target_class = " -c " + targetClass.getText().toString();
                String target_name = " -n \"" + targetName.getText().toString() + "\"";
                if (target_class.equals(" -c ")) target_class = "";
                if (target_name.equals(" -n \"\"")) target_name = "";
                if (target_address.equals(" -a ") && target_name.isEmpty() && target_class.isEmpty()) {
                    Toast.makeText(requireActivity().getApplicationContext(), "请至少输入一个参数！", Toast.LENGTH_SHORT).show();
                } else {
                    final String target_classname = target_class + target_name;
                    if (!target_address.equals(" -a ")) {
                        run_cmd("echo -ne \"\\033]0;欺骗蓝牙\\007\" && clear;echo 'Spooftooph 已启动..';spooftooph -i " + target_interface + target_address +
                                "; sleep 2 && hciconfig " + target_interface + " up && spooftooph -i " + target_interface + target_classname + " && echo '\n正在使用 hciconfig 启动接口..\n\n类别/名称已更改, 将在 3 秒后关闭..';sleep 3 && exit");
                    } else {
                        run_cmd("echo -ne \"\\033]0;欺骗蓝牙\\007\" && clear;echo 'Spooftooph 已启动..';spooftooph -i " + target_interface + target_classname + " && echo '\n类别/名称已更改, 将在 3 秒后关闭..';sleep 3 && exit");
                    }
                }
            });
            return rootView;
        }

        private void refreshSpoof(View BTFragment) {
            ShellExecuter exe = new ShellExecuter();
            final EditText spoof_interface = BTFragment.findViewById(R.id.spoof_interface);
            final TextView currentAddress = BTFragment.findViewById(R.id.currentAddress);
            final TextView currentClass = BTFragment.findViewById(R.id.currentClass);
            final TextView currentClassType = BTFragment.findViewById(R.id.currentClassType);
            final TextView currentName = BTFragment.findViewById(R.id.currentName);

            requireActivity().runOnUiThread(() -> {
                String selectedIface = spoof_interface.getText().toString();
                String currentAddress_CMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig " + selectedIface + " | awk '/Address/ { print $3 }'");
                if (!currentAddress_CMD.isEmpty()) {
                    currentAddress.setText(currentAddress_CMD);

                    String currentClassCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig " + selectedIface + " -a | awk '/Class:/ { print $2 }' | sed '/^Class:/d'");
                    currentClass.setText(currentClassCMD);

                    String currentClassTypeCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig " + selectedIface + " -a | awk '/Device Class:/ { print $3, $4, $5 }'");
                    currentClassType.setText(currentClassTypeCMD);

                    String currentNameCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig " + selectedIface + " -a | grep Name | cut -d\\' -f2");
                    currentName.setText(currentNameCMD);
                } else
                    Toast.makeText(requireActivity().getApplicationContext(), "接口已关闭！", Toast.LENGTH_SHORT).show();
            });
        }
    }

    public static class CWFragment extends BTFragment {
        private Context context;
        private String selected_mode;
        final ShellExecuter exe = new ShellExecuter();
        private Boolean iswatch;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.bt_carwhisperer, container, false);

            SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

            final TextView CWdesc = rootView.findViewById(R.id.carwhisp_desc);
            iswatch = sharedpreferences.getBoolean("running_on_wearos", false);
            if (iswatch) {
                CWdesc.setVisibility(View.GONE);
            }

            // 选择的接口
            final EditText cw_interface = rootView.findViewById(R.id.hci_interface);

            // 目标地址
            final EditText cw_address = rootView.findViewById(R.id.hci_address);

            // 设置目标
            Button SetTarget = rootView.findViewById(R.id.set_target);

            SetTarget.setOnClickListener( v -> {
                String selected_address = PreferencesData.getString(context, "selected_address", "");
                cw_address.setText(selected_address);
            });

            // 频道
            final EditText hci_channel = rootView.findViewById(R.id.hci_channel);

            // CW 模式
            Spinner cwmode = rootView.findViewById(R.id.cwmode);
            final ArrayList<String> modes = new ArrayList<>();
            modes.add("监听");
            modes.add("注入");
            cwmode.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, modes));
            cwmode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    selected_mode = parentView.getItemAtPosition(pos).toString();
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 监听
            final EditText listenfilename = rootView.findViewById(R.id.listenfilename);

            // 注入
            final EditText injectfilename = rootView.findViewById(R.id.injectfilename);
            final Button injectfilebrowse = rootView.findViewById(R.id.injectfilebrowse);

            injectfilebrowse.setOnClickListener( v -> {
                Intent intent = new Intent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("audio/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "选择音频文件"),1001);
            });

            // 启动
            Button StartCWButton = rootView.findViewById(R.id.start_cw);
            StartCWButton.setOnClickListener( v -> {
                String cw_iface = cw_interface.getText().toString();
                String cw_target = cw_address.getText().toString();
                if (!cw_target.isEmpty()) {
                    String cw_channel = hci_channel.getText().toString();
                    String cw_listenfile = listenfilename.getText().toString();
                    String cw_injectfile = injectfilename.getText().toString();

                    if (selected_mode.equals("监听")) {
                        run_cmd("echo -ne \"\\033]0;监听 BT 音频\\007\" && clear;echo 'Carwhisperer 启动中..\n返回 NetHunter 以停止, 或实时监听！'$'\n';carwhisperer " + cw_iface + " /root/carwhisperer/in.raw /sdcard/rec.raw " + cw_target + " " + cw_channel +
                                " && echo '正在转换为 wav 到目标目录..';sox -t raw -r 8000 -e signed -b 16 /sdcard/rec.raw -r 8000 -b 16 /sdcard/" + cw_listenfile + ";echo 完成! || echo '没有转换文件！';sleep 3 && exit");
                    } else if (selected_mode.equals("注入")) {
                        run_cmd("echo -ne \"\\033]0;注入 BT 音频\\007\" && clear;echo 'Carwhisperer 启动中..';length=$(($(soxi -D '" + cw_injectfile + "' | cut -d. -f1)+8));sox '" + cw_injectfile + "' -r 8000 -b 16 -c 1 tempi.raw && timeout $length " +
                                "carwhisperer " + cw_iface + " tempi.raw tempo.raw " + cw_target + " " + cw_channel + "; rm tempi.raw && rm tempo.raw;echo '\n注入完成, 将在 3 秒后关闭..';sleep 3 && exit");
                    }
                } else
                    Toast.makeText(requireActivity().getApplicationContext(), "没有目标地址！", Toast.LENGTH_SHORT).show();
            });

            // 停止
            Button StopCWButton = rootView.findViewById(R.id.stop_cw);
            StopCWButton.setOnClickListener( v -> {
                exe.RunAsRoot(new String[]{NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd pkill carwhisperer"});
                Toast.makeText(requireActivity().getApplicationContext(), "已停止", Toast.LENGTH_SHORT).show();
            });

            // 流式传输或播放音频
            ImageButton PlayAudioButton = rootView.findViewById(R.id.play_audio);
            ImageButton StopAudioButton = rootView.findViewById(R.id.stop_audio);
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 20000, AudioTrack.MODE_STREAM);
            PlayAudioButton.setOnClickListener( v -> {
                File cw_listenfile = new File(NhPaths.SD_PATH + "/rec.raw");
                if (cw_listenfile.length() == 0) {
                    Toast.makeText(getContext(), "未找到文件！", Toast.LENGTH_SHORT).show();
                } else {
                    Executors.newSingleThreadExecutor().execute(() -> {
                        InputStream s = null;
                        try {
                            s = new FileInputStream(cw_listenfile);
                        } catch (NullPointerException | IOException e) {
                            e.printStackTrace();
                        }
                        audioTrack.play();
                        // 读取数据. 
                        byte[] data = new byte[200];
                        int n;
                        try {
                            while (true) {
                                assert s != null;
                                if ((n = s.read(data)) == -1) break;
                                synchronized (audioTrack) {
                                    audioTrack.write(data, 0, n);
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            });
            StopAudioButton.setOnClickListener(v -> {
                audioTrack.pause();
                audioTrack.flush();
            });
            return rootView;
        }
    }
    public static class BadBtFragment extends BTFragment {
        private Context context;
        private String selected_badbtmode;
        private String selected_preset;
        private String selected_preset_uac;
        private String selected_prefix;
        private String selected_badbt_class;
        String prefixCMD = "";
        String uacCMD = "";
        final ShellExecuter exe = new ShellExecuter();

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
        }

        @Override
        public void onResume(){
            super.onResume();
            Toast.makeText(requireActivity().getApplicationContext(), "状态已更新", Toast.LENGTH_SHORT).show();
            Executors.newSingleThreadExecutor().execute(() -> refresh_badbt(requireView().getRootView()));
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.bt_badbt, container, false);
            final Button badbtServerButton = rootView.findViewById(R.id.badbtserver_button);
            SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
            boolean iswatch = requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);

            // 手表优化
            final TextView BadBTdesc = rootView.findViewById(R.id.badbt_desc);
            if (iswatch) {
                BadBTdesc.setVisibility(View.GONE);
            }

            // 选择的接口、名称、bdaddr、类别
            final EditText badbt_interface = rootView.findViewById(R.id.badbt_interface);
            final EditText badbt_name = rootView.findViewById(R.id.badbt_name);
            final EditText badbt_bdaddr = rootView.findViewById(R.id.badbt_address);
            final EditText badbt_class = rootView.findViewById(R.id.badbt_class);

            // 类别下拉列表
            Spinner badbtclass = rootView.findViewById(R.id.badbt_class_spinner);
            final ArrayList<String> classes = new ArrayList<>();
            classes.add("键盘");
            classes.add("耳机");
            classes.add("扬声器");
            classes.add("鼠标");
            classes.add("打印机");
            classes.add("电脑");
            classes.add("手机");
            classes.add("自定义");
            badbtclass.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, classes));
            badbtclass.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    selected_prefix = parentView.getItemAtPosition(pos).toString();
                    switch (selected_prefix) {
                        case "键盘":
                            badbt_class.setText("0x000540");
                            break;
                        case "耳机":
                            badbt_class.setText("0x000408");
                            break;
                        case "扬声器":
                            badbt_class.setText("0x240414");
                            break;
                        case "鼠标":
                            badbt_class.setText("0x002580");
                            break;
                        case "打印机":
                            badbt_class.setText("0x040680");
                            break;
                        case "电脑":
                            badbt_class.setText("0x02010c");
                            break;
                        case "手机":
                            badbt_class.setText("0x000204");
                            break;
                        case "自定义":
                            badbt_class.setText("");
                            break;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });


            // 刷新
            refresh_badbt(rootView);
            String prevbadbtname = sharedpreferences.getString("badbt-name", "");
            if (!prevbadbtname.isEmpty()) badbt_name.setText(prevbadbtname);
            String prevbadbtiface = sharedpreferences.getString("badbt-iface", "");
            if (!prevbadbtiface.isEmpty()) badbt_interface.setText(prevbadbtiface);
            String prevbadbtaddr = sharedpreferences.getString("badbt-bdaddr", "");
            if (!prevbadbtaddr.isEmpty()) badbt_bdaddr.setText(prevbadbtaddr);
            String prevbadbtclass = sharedpreferences.getString("badbt-class", "");
            if (!prevbadbtclass.isEmpty()) badbt_class.setText(prevbadbtclass);

            // 刷新状态
            ImageButton RefreshBadBTStatus = rootView.findViewById(R.id.refreshBadBTStatus);
            RefreshBadBTStatus.setOnClickListener(v -> refresh_badbt(rootView));

            // 字符串
            final EditText badbt_string = rootView.findViewById(R.id.editBadBT);

            // 服务
            badbtServerButton.setOnClickListener( v -> {
                if (badbtServerButton.getText().equals("启动")) {
                    String BadBT_name = badbt_name.getText().toString();
                    String BadBT_iface = badbt_interface.getText().toString();
                    String BadBT_bdaddr = badbt_bdaddr.getText().toString();
                    String BadBT_class = badbt_class.getText().toString();
                    String dbus_statusCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service dbus status | grep dbus");
                    String bt_statusCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd service bluetooth status | grep bluetooth");
                    String bt_ifaceCMD = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd hciconfig | grep hci");
                    sharedpreferences.edit().putString("badbt-name", BadBT_name).apply();
                    sharedpreferences.edit().putString("badbt-iface", BadBT_iface).apply();
                    sharedpreferences.edit().putString("badbt-bdaddr", BadBT_bdaddr).apply();
                    sharedpreferences.edit().putString("badbt-class", BadBT_class).apply();

                    if (dbus_statusCMD.equals("dbus is running.") && bt_statusCMD.equals("bluetooth is running.") && !bt_ifaceCMD.isEmpty()) {
                        if (!BadBT_name.isEmpty() && !BadBT_iface.isEmpty() && !BadBT_bdaddr.isEmpty()) {
                            Toast.makeText(requireActivity().getApplicationContext(), "正在启动服务器...", Toast.LENGTH_SHORT).show();
                            run_cmd("echo -ne \"\\033]0;BadBT 服务器\\007\" && clear;python3 /root/badbt/btk_server.py -n '"
                                    + BadBT_name + "' -i " + BadBT_iface + " -c " + BadBT_class + " -a " + BadBT_bdaddr + "&;sleep 1 && echo '正在启动代理...' && sleep 1 && bluetoothctl --agent NoInputNoOutput && exit");
                            refresh_badbt(rootView);
                        } else {
                            Toast.makeText(requireActivity().getApplicationContext(), "请输入接口、键盘名称和地址！", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(requireActivity().getApplicationContext(), "蓝牙接口或服务未运行！", Toast.LENGTH_LONG).show();
                    }
                } else if (badbtServerButton.getText().equals("停止")) {
                    exe.RunAsRoot(new String[]{"kill `ps -ef | grep '[btk]_server' | awk {'print $2'}`"});
                    exe.RunAsRoot(new String[]{"pkill bluetoothctl"});
                    refresh_badbt(rootView);
                }
            });

            // 模式
            Spinner badbtmode = rootView.findViewById(R.id.badbtmode);
            View BadBTSettingsView = rootView.findViewById(R.id.badbtsettings_layout);
            final ArrayList<String> modes = new ArrayList<>();
            modes.add("发送字符串");
            modes.add("交互式");
            badbtmode.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, modes));
            badbtmode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    selected_badbtmode = parentView.getItemAtPosition(pos).toString();
                    if (selected_badbtmode.equals("交互式")) {
                        BadBTSettingsView.setVisibility(View.GONE);
                    } else if (selected_badbtmode.equals("发送字符串")){
                        BadBTSettingsView.setVisibility(View.VISIBLE);
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 前缀
            CheckBox uacCheckBox = rootView.findViewById(R.id.uac_bypass);
            View BadBTUACView = rootView.findViewById(R.id.badbtuac_layout);
            Spinner badbtprefix = rootView.findViewById(R.id.badbtprefix);
            Spinner badbtpresets_uac = rootView.findViewById(R.id.badbtpresets_uac);
            final ArrayList<String> presets_uac = new ArrayList<>();
            final ArrayList<String> prefixes = new ArrayList<>();
            prefixes.add("手机主页");
            prefixes.add("手机浏览器");
            prefixes.add("Windows CMD");
            prefixes.add("Mac 终端");
            prefixes.add("Linux 终端");
            prefixes.add("无");
            badbtprefix.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, prefixes));
            badbtprefix.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    selected_prefix = parentView.getItemAtPosition(pos).toString();
                    switch (selected_prefix) {
                        case "手机主页":
                            BadBTUACView.setVisibility(View.GONE);
                            prefixCMD = "mobile";
                            uacCheckBox.setChecked(false);
                            presets_uac.clear();
                            presets_uac.add("无");
                            badbtpresets_uac.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets_uac));
                            uacCMD = "-";
                            break;
                        case "手机浏览器":
                            BadBTUACView.setVisibility(View.GONE);
                            prefixCMD = "mobilewww";
                            uacCheckBox.setChecked(false);
                            presets_uac.clear();
                            presets_uac.add("无");
                            badbtpresets_uac.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets_uac));
                            uacCMD = "-";
                            break;
                        case "Windows CMD":
                            BadBTUACView.setVisibility(View.VISIBLE);
                            prefixCMD = "windows";
                            break;
                        case "Mac 终端":
                            BadBTUACView.setVisibility(View.GONE);
                            prefixCMD = "mac";
                            uacCheckBox.setChecked(false);
                            presets_uac.clear();
                            presets_uac.add("无");
                            badbtpresets_uac.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets_uac));
                            uacCMD = "-";
                            break;
                        case "Linux 终端":
                            BadBTUACView.setVisibility(View.GONE);
                            prefixCMD = "linux";
                            uacCheckBox.setChecked(false);
                            presets_uac.clear();
                            presets_uac.add("无");
                            badbtpresets_uac.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets_uac));
                            uacCMD = "-";
                            break;
                        case "无":
                            BadBTUACView.setVisibility(View.GONE);
                            uacCMD = "-";
                            uacCheckBox.setChecked(false);
                            presets_uac.clear();
                            presets_uac.add("无");
                            badbtpresets_uac.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets_uac));
                            uacCMD = "-";
                            break;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 预设
            Spinner badbtpresets = rootView.findViewById(R.id.badbtpresets);
            EditText badbtstring = rootView.findViewById(R.id.editBadBT);
            final ArrayList<String> presets = new ArrayList<>();
            presets.add("Rickroll");
            presets.add("虚假 Windows 更新");
            presets.add("无");
            badbtpresets.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets));
            badbtpresets.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    selected_preset = parentView.getItemAtPosition(pos).toString();
                    switch (selected_preset) {
                        case "Rickroll":
                            badbtstring.setText(R.string.bt_badbt_string_rickroll);
                            break;
                        case "虚假 Windows 更新":
                            badbtstring.setText(R.string.bt_badbt_string_fakeupdate);
                            break;
                        case "无":
                            badbtstring.setText("");
                            break;
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // UAC
            uacCheckBox.setOnClickListener( v -> {
                if (uacCheckBox.isChecked()) {
                    badbtpresets_uac.setVisibility(View.VISIBLE);
                    presets_uac.clear();
                    presets_uac.add("Windows 7");
                    presets_uac.add("Windows 8");
                    presets_uac.add("Windows 10");
                    presets_uac.add("Windows 11");
                    badbtpresets_uac.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets_uac));
                }
                else {
                    presets_uac.clear();
                    presets_uac.add("无");
                    badbtpresets_uac.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, presets_uac));
                    badbtpresets_uac.setVisibility(View.GONE);
                    uacCMD = "-";
                }
            });
            badbtpresets_uac.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    selected_preset_uac = parentView.getItemAtPosition(pos).toString();
                    if (selected_preset_uac.equals("Windows 7")) {
                        uacCMD = "win7";
                    } else if (selected_preset_uac.equals("Windows 8")) {
                        uacCMD = "win8";
                    } else if (selected_preset_uac.equals("Windows 10")) {
                        uacCMD = "win10";
                    } else if (selected_preset_uac.equals("Windows 11")) {
                        uacCMD = "win11";
                    } else if (selected_preset.equals("无")) {
                        uacCMD = "-";
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 从文件加载
            final Button injectStringButton = rootView.findViewById(R.id.injectstringbrowse);
            injectStringButton.setOnClickListener( v -> {
                Intent intent2 = new Intent();
                intent2.addCategory(Intent.CATEGORY_OPENABLE);
                intent2.setType("text/*");
                intent2.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent2, "选择文本文件"),1002);
            });

            // 启动
            Button StartBadBtButton = rootView.findViewById(R.id.start_badbt);
            StartBadBtButton.setOnClickListener( v -> {
                if (selected_badbtmode.equals("发送字符串")) {
                    String BadBT_string = badbt_string.getText().toString();
                    run_cmd("echo -ne \"\\033]0;BadBT 发送字符串\\007\" && clear;python3 /root/badbt/send_string.py '" + BadBT_string + "' " + prefixCMD + " " + uacCMD + ";sleep 2 && echo '正在退出..' && exit");
                    Toast.makeText(requireActivity().getApplicationContext(), "正在发送字符串..", Toast.LENGTH_SHORT).show();
                } else if (selected_badbtmode.equals("交互式")) {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
                    builder.setTitle("确定吗？");
                    builder.setMessage("交互式模式将在 NetHunter 终端中运行, 但目前需要连接物理键盘. ");
                    builder.setPositiveButton("确定", (dialog, which) -> {
                        run_cmd("echo -ne \"\\033]0;BadBT 客户端\\007\" && clear;python3 /root/badbt/kb_client.py");
                        Toast.makeText(requireActivity().getApplicationContext(), "正在启动键盘客户端..", Toast.LENGTH_SHORT).show();
                    });
                    builder.setNegativeButton("取消", (dialog, which) -> {
                    });
                    builder.show();
                }
            });

            return rootView;
        }

        // 刷新 badbt
        private void refresh_badbt(View BTFragment) {

            final TextView BadBTServerStatus = BTFragment.findViewById(R.id.BadBTServerStatus);
            final Button badbtserverButton = BTFragment.findViewById(R.id.badbtserver_button);
            SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

            requireActivity().runOnUiThread(() -> {
                String badbtserver_statusCMD = exe.RunAsRootOutput("ps -ef | grep btk_server");
                if (!badbtserver_statusCMD.contains("btk_server.py")) {
                    BadBTServerStatus.setText(R.string.bt_stopped);
                    badbtserverButton.setText(R.string.bt_start);
                }
                else {
                    BadBTServerStatus.setText(R.string.bt_running);
                    badbtserverButton.setText(R.string.bt_stop);
                }
            });
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK) {
            EditText injectfilename = requireActivity().findViewById(R.id.injectfilename);
            String FilePath = Objects.requireNonNull(data.getData()).getPath();
            assert FilePath != null;
            FilePath = FilePath.replace("/document/primary:", "/sdcard/");
            injectfilename.setText(FilePath);
        }
        if (requestCode == 1002 && resultCode == Activity.RESULT_OK) {
            EditText badbtstring = requireActivity().findViewById(R.id.editBadBT);
            String FilePath = Objects.requireNonNull(data.getData()).getPath();
            assert FilePath != null;
            FilePath = FilePath.replace("/document/primary:", "/sdcard/");
            final ShellExecuter exe = new ShellExecuter();
            String fileContent = exe.RunAsRootOutput("cat " + FilePath);
            badbtstring.setText(fileContent);
        }
    }

    public static class PreferencesData {
        public static void saveString(Context context, String key, String value) {
            if (context != null) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                sharedPrefs.edit().putString(key, value).apply();
            }
        }

        public static String getString(Context context, String key, String defaultValue) {
            if (context != null) {
                SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
                return sharedPrefs.getString(key, defaultValue);
            }
            return defaultValue;
        }
    }

    ////
    // Bridge 端函数
    ////

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }
}