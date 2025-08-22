package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Log;

/**
 * WPS 攻击碎片页
 * 用于扫描并执行 WPS-Pixie Dust / Brute Force / PBC 等攻击
 */
public class WPSFragment extends Fragment {
    public static final String TAG = "WPSFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";

    private Spinner ifaceSpinner;
    private String selectedInterface = "wlan0";   // 默认网卡
    private TextView CustomPIN;
    private TextView DelayTime;
    private Spinner WPSList;
    private CheckBox PixieCheckbox;
    private CheckBox PixieForceCheckbox;
    private CheckBox BruteCheckbox;
    private CheckBox CustomPINCheckbox;
    private CheckBox DelayCMD;
    private CheckBox PbcCMD;

    private final ArrayList<String> arrayList = new ArrayList<>();
    private LinearLayout WPSPinLayout;
    private LinearLayout DelayLayout;
    private Activity activity;
    private final ShellExecuter exe = new ShellExecuter();

    private String selected_network;
    private String pixieCMD = "";
    private String pixieforceCMD = "";
    private String bruteCMD = "";
    private String customPINCMD = "";
    private String customPIN = "";
    private String delayCMD = "";
    private String delayTIME = "";
    private String pbcCMD = "";
    private Boolean iswatch;

    public static WPSFragment newInstance(int sectionNumber) {
        WPSFragment fragment = new WPSFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Context context = getContext();
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.wps, container, false);

        // 检测是否为手表
        SharedPreferences sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        iswatch = sharedpreferences.getBoolean("running_on_wearos", false);

        // 确保 Wi-Fi 开启
        if (iswatch) {
            exe.RunAsRoot(new String[]{"settings put system clockwork_wifi_setting on; ifconfig wlan0 up"});
        } else {
            exe.RunAsRoot(new String[]{"svc wifi enable"});
        }

        /* ===== 1. 网卡选择 ===== */
        ifaceSpinner = rootView.findViewById(R.id.wps_iface_spinner);
        ExecutorService ifaceExecutor = Executors.newSingleThreadExecutor();
        ifaceExecutor.execute(() -> {
            String iwPath;
            String abi = android.os.Build.SUPPORTED_ABIS[0];
            if (abi.contains("arm64")) {
                iwPath = NhPaths.APP_SCRIPTS_BIN_PATH + "/iw";
            } else {
                iwPath = NhPaths.APP_SCRIPTS_BIN_PATH + "/iw-armeabi";
            }
            Log.d(TAG, "使用 iw 二进制文件: " + iwPath);

            String output = exe.RunAsRootOutput(iwPath + " dev | awk '/Interface/ {print $2}' | grep '^wlan'");
            String[] interfaces = output.trim().isEmpty() ? new String[]{"wlan0"} : output.split("\n");
            if (interfaces.length == 0) interfaces = new String[]{"wlan0"};
            if (isAdded()) {
                String[] finalInterfaces = interfaces;
                requireActivity().runOnUiThread(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, finalInterfaces);
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    ifaceSpinner.setAdapter(adapter);
                    selectedInterface = finalInterfaces[0];
                    ifaceSpinner.setSelection(0);
                    ifaceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedInterface = finalInterfaces[position];
                        }
                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {}
                    });
                });
            }
        });

        /* ===== 2. 扫描按钮 ===== */
        Button scanButton = rootView.findViewById(R.id.scanwps);
        scanButton.setOnClickListener(view -> scanWifi());

        WPSList = rootView.findViewById(R.id.wpslist);
        if (getContext() != null) {
            ArrayAdapter<String> WPSadapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, arrayList);
            WPSList.setAdapter(WPSadapter);
        }

        /* ===== 3. 重置接口 ===== */
        Button resetifaceButton = rootView.findViewById(R.id.resetinterface);
        resetifaceButton.setOnClickListener(view -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> requireActivity().runOnUiThread(() -> {
                if (iswatch) {
                    exe.RunAsRoot(new String[]{"ip link set wlan0 down; sleep 1 && ip link set wlan0 up"});
                } else {
                    exe.RunAsRoot(new String[]{"svc wifi disable; sleep 1 && svc wifi enable"});
                }
                Toast.makeText(requireActivity().getApplicationContext(), "已重置接口", Toast.LENGTH_SHORT).show();
            }));
        });

        /* ===== 4. 选择目标网络 ===== */
        WPSList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                String selected_target = WPSList.getItemAtPosition(pos).toString();
                if (selected_target.equals("附近无 WPS 网络") || selected_target.equals("请重置接口！")) {
                    selected_network = "";
                } else {
                    selected_network = exe.RunAsRootOutput("echo \"" + selected_target + "\" | cut -d ' ' -f 1");
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {}
        });

        /* ===== 5. 复选框 ===== */
        PixieCheckbox      = rootView.findViewById(R.id.pixie);
        PixieForceCheckbox = rootView.findViewById(R.id.pixieforce);
        BruteCheckbox      = rootView.findViewById(R.id.brute);
        CustomPINCheckbox  = rootView.findViewById(R.id.custompin);
        CustomPIN          = rootView.findViewById(R.id.wpspin);
        DelayCMD           = rootView.findViewById(R.id.delay);
        PbcCMD             = rootView.findViewById(R.id.pbc);
        WPSPinLayout       = rootView.findViewById(R.id.pinlayout);
        DelayLayout        = rootView.findViewById(R.id.delaylayout);

        PixieCheckbox.setOnClickListener(v -> {
            pixieCMD = PixieCheckbox.isChecked() ? " -K" : "";
        });
        PixieForceCheckbox.setOnClickListener(v -> {
            pixieforceCMD = PixieForceCheckbox.isChecked() ? " -F" : "";
        });
        BruteCheckbox.setOnClickListener(v -> {
            bruteCMD = BruteCheckbox.isChecked() ? " -B" : "";
        });
        CustomPINCheckbox.setOnClickListener(v -> {
            if (CustomPINCheckbox.isChecked()) {
                customPINCMD = " -p ";
                WPSPinLayout.setVisibility(View.VISIBLE);
            } else {
                customPINCMD = "";
                customPIN = "";
                WPSPinLayout.setVisibility(View.GONE);
            }
        });
        DelayCMD.setOnClickListener(v -> {
            if (DelayCMD.isChecked()) {
                delayCMD = " -d ";
                DelayLayout.setVisibility(View.VISIBLE);
            } else {
                delayCMD = "";
                delayTIME = "";
                DelayLayout.setVisibility(View.GONE);
            }
        });
        PbcCMD.setOnClickListener(v -> {
            pbcCMD = PbcCMD.isChecked() ? " --pbc" : "";
        });

        /* ===== 6. 开始攻击按钮 ===== */
        Button startButton = rootView.findViewById(R.id.start_oneshot);
        DelayTime = rootView.findViewById(R.id.delaytime);

        startButton.setOnClickListener(v -> {
            customPIN = CustomPIN.getText().toString().trim();
            delayTIME = DelayTime.getText().toString().trim();
            if (!selected_network.isEmpty()) {
                if (iswatch) {
                    Handler handler = new Handler();
                    handler.postDelayed(() -> exe.RunAsRoot(new String[]{"settings put system clockwork_wifi_setting off"}), 10000);
                    handler.postDelayed(() -> exe.RunAsRoot(new String[]{"ifconfig wlan0 up"}), 11000);
                }
                run_cmd("python3 /sdcard/nh_files/modules/oneshot.py -b " + selected_network +
                        " -i " + selectedInterface + pixieCMD + pixieforceCMD + bruteCMD +
                        customPINCMD + customPIN + delayCMD + delayTIME + pbcCMD);
            } else {
                Toast.makeText(requireActivity().getApplicationContext(), "请先选择目标网络！", Toast.LENGTH_SHORT).show();
            }
        });

        return rootView;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (iswatch) exe.RunAsRoot(new String[]{"settings put system clockwork_wifi_setting on"});
    }

    /* ===== 扫描 Wi-Fi ===== */
    private void scanWifi() {
        arrayList.clear();
        arrayList.add("扫描中…");
        WPSList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, arrayList));
        WPSList.setVisibility(View.VISIBLE);

        Handler handler = new Handler();
        handler.postDelayed(() -> {
            arrayList.clear();
            arrayList.add("扫描中…");
            WPSList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, arrayList));
        }, 1500);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String outputScanLog = exe.RunAsRootOutput(
                    NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd python3 /sdcard/nh_files/modules/oneshot.py -i " +
                            selectedInterface + " -s | grep -E '[0-9])' | tr -s ' ' | cut -d ' ' -f 2-3");
            requireActivity().runOnUiThread(() -> {
                String[] arrayList = outputScanLog.split("\n");
                ArrayAdapter<String> targetsadapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, arrayList);
                if (outputScanLog.isEmpty()) {
                    ArrayList<String> notargets = new ArrayList<>();
                    notargets.add("附近无 WPS 网络");
                    WPSList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, notargets));
                } else if (outputScanLog.equals("Error:;command")) {
                    ArrayList<String> notargets = new ArrayList<>();
                    notargets.add("请重置接口！");
                    WPSList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, notargets));
                } else {
                    WPSList.setAdapter(targetsadapter);
                }
            });
        });
    }

    /* ===== Bridge 执行命令 ===== */
    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }
}
