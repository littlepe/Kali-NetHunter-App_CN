package com.offsec.nethunter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.BootKali;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Map;
import java.util.HashMap;

public class CANFragment extends Fragment {
    private static final String TAG = "CANFragment";
    private static SharedPreferences sharedpreferences;
    private Context context;
    private Activity activity;
    private static final String ARG_SECTION_NUMBER = "section_number";

    public static CANFragment newInstance(int sectionNumber) {
        CANFragment fragment = new CANFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        Log.d(TAG, "onCreate called");
        sharedpreferences = requireActivity().getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        super.onCreate(savedInstanceState);
        activity = getActivity();
        context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.can, container, false);
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        TabsPagerAdapter tabsPagerAdapter = new TabsPagerAdapter(this);

        ViewPager2 mViewPager = rootView.findViewById(R.id.pagerCAN);
        mViewPager.setAdapter(tabsPagerAdapter);
        mViewPager.setOffscreenPageLimit(6);

        TabLayout tabLayout = rootView.findViewById(R.id.tabLayoutCAN);
        new TabLayoutMediator(tabLayout, mViewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0: tab.setText("主界面"); break;
                        case 1: tab.setText("工具"); break;
                        case 2: tab.setText("CAN-USB"); break;
                        case 3: tab.setText("Caribou"); break;
                        case 4: tab.setText("ICSim"); break;
                        default: tab.setText("标签 " + (position + 1));
                    }
                }
        ).attach();

        mViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                activity.invalidateOptionsMenu();
            }
        });

        // 添加 MenuProvider 以处理菜单
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.can, menu);
            }

            @SuppressLint("NonConstantResourceId")
            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.documentation:
                        sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
                        RunDocumentation();
                        return true;
                    case R.id.setup:
                        sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
                        RunSetup();
                        return true;
                    case R.id.update:
                        sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
                        RunUpdate();
                        return true;
                    case R.id.about:
                        sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
                        RunAbout();
                        return true;
                    default:
                        return false;
                }
            }
        }, getViewLifecycleOwner());

        return rootView;
    }

    // 首次设置
    public void SetupDialog() {
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        builder.setTitle("欢迎使用 CAN Arsenal！");
        builder.setMessage("看起来是首次运行. 是否安装 CAN 工具？");
        builder.setPositiveButton("安装", (dialog, which) -> {
            RunSetup();
            sharedpreferences.edit().putBoolean("setup_done", true).apply();
        });
        builder.setNegativeButton("不再提示", (dialog, which) -> {
            dialog.dismiss();
            sharedpreferences.edit().putBoolean("setup_done", true).apply();
        });
        builder.show();
    }

    // 文档项
    public void RunDocumentation() {
        String url = "https://www.kali.org/docs/nethunter/nethunter-canarsenal/";
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        activity.startActivity(intent);
    }

    // 设置项
    public void RunSetup() {
        Log.d(TAG, "RunSetup called");
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

        Log.i(TAG, "正在执行设置命令");
        String setupCommand = "curl -s https://raw.githubusercontent.com/V0lk3n/NetHunter-CARsenal/refs/heads/main/carsenal_setup.sh | bash -s setup";
        String setupResult = run_cmd(setupCommand);
        Log.d("SetupResult",setupResult);
        sharedpreferences.edit().putBoolean("setup_done", true).apply();
        Log.i(TAG, "设置完成");
    }

    // 更新项
    public void RunUpdate() {
        Log.d(TAG, "RunUpdate called");
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

        Log.i(TAG, "正在执行更新命令");
        String updateCommand = "curl -s https://raw.githubusercontent.com/V0lk3n/NetHunter-CARsenal/refs/heads/main/carsenal_setup.sh | bash -s update";
        String updateResult = run_cmd(updateCommand);
        Log.d("UpdateResult",updateResult);
        sharedpreferences.edit().putBoolean("setup_done", true).apply();
        Log.i(TAG, "更新完成");
    }

    public void RunAbout() {
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        MaterialAlertDialogBuilder aboutDialog = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
        aboutDialog.setTitle("关于 CAN Arsenal");

        TextView message = new TextView(context);
        message.setText(getResources().getText(R.string.about_author));
        message.setMovementMethod(LinkMovementMethod.getInstance());
        message.setPadding(50, 40, 50, 0);
        Linkify.addLinks(message, Linkify.WEB_URLS);

        aboutDialog.setView(message);
        aboutDialog.setNegativeButton("关闭", (dialog, id) -> dialog.cancel());
        aboutDialog.show();
    }

    public static class TabsPagerAdapter extends FragmentStateAdapter {
        public TabsPagerAdapter(@NonNull Fragment fragment) {
            super(fragment);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new MainFragment();
                case 1:
                    return new ToolsFragment();
                case 2:
                    return new CANUSBFragment();
                case 3:
                    return new CANCARIBOUFragment();
                default:
                    return new CANICSIMFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5;
        }
    }

    public static class MainFragment extends CANFragment {
        final ShellExecuter exe = new ShellExecuter();
        private static final long SHORT_DELAY = 1000L;
        private Context context;
        private TextView SelectedIface;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.can_main, container, false);

            // 常用变量
            SelectedIface = rootView.findViewById(R.id.can_iface);

            final EditText bt_target_mac = rootView.findViewById(R.id.bttarget);
            final EditText selected_vin = rootView.findViewById(R.id.vin_number);

            // 首次运行
            boolean setupdone = sharedpreferences.getBoolean("setup_done", false);
            if (!setupdone) {
                SetupDialog();
            }

            // 切换高级选项
            Button btnMtu = rootView.findViewById(R.id.btn_toggle_mtu);
            EditText SelectedMTU = rootView.findViewById(R.id.mtu_value);

            btnMtu.setOnClickListener(v -> {
                boolean isVisible = SelectedMTU.getVisibility() == View.VISIBLE;
                SelectedMTU.setVisibility(isVisible ? View.GONE : View.VISIBLE);

                int color = isVisible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnMtu.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            Button btnTxqueuelen = rootView.findViewById(R.id.btn_toggle_txqueuelen);
            EditText SelectedTxqueuelen = rootView.findViewById(R.id.txqueuelen_value);

            btnTxqueuelen.setOnClickListener(v -> {
                boolean isVisible = SelectedTxqueuelen.getVisibility() == View.VISIBLE;
                SelectedTxqueuelen.setVisibility(isVisible ? View.GONE : View.VISIBLE);

                int color = isVisible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnTxqueuelen.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 连接与守护进程
            // ldattach
            Button LdAttachButton = rootView.findViewById(R.id.start_ldattach);

            // 访问 SharedPreferences
            SharedPreferences ldAttach_prefs = requireActivity().getSharedPreferences("ldAttach_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editorLdAttach = ldAttach_prefs.edit();

            // 加载保存的命令或使用默认
            String savedCmd_ldAttach = ldAttach_prefs.getString("ldAttach_cmd", "ldattach --debug --speed 38400 --eightbits --noparity --onestopbit --iflag -ICRNL,INLCR,-IXOFF 29 /dev/rfcomm0");
            String[] ldAttachCmdHolder = { savedCmd_ldAttach };

            LdAttachButton.setOnClickListener(v -> {
                String ldAttachRun = ldAttachCmdHolder[0];

                if (!ldAttachRun.isEmpty()) {
                    run_cmd(ldAttachRun);
                    showToast("按 Ctrl+C 停止. ");
                } else {
                    showToast("请设置您的 ldattach 命令！");
                }
            });

            // 长按允许用户编辑命令
            LdAttachButton.setOnLongClickListener(v -> {
                AlertDialog.Builder builder_ldAttach = new AlertDialog.Builder(requireContext());
                builder_ldAttach.setTitle("编辑命令");

                final EditText input_ldAttach = new EditText(requireContext());
                input_ldAttach.setText(ldAttachCmdHolder[0]);
                builder_ldAttach.setView(input_ldAttach);

                builder_ldAttach.setPositiveButton("保存", (dialog, which) -> {
                    String newLdAttachCmd = input_ldAttach.getText().toString();
                    ldAttachCmdHolder[0] = newLdAttachCmd;

                    // 保存到 SharedPreferences
                    editorLdAttach.putString("ldAttach_cmd", newLdAttachCmd);
                    editorLdAttach.apply();

                    showToast("命令已更新！");
                });

                builder_ldAttach.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

                AlertDialog dialog = builder_ldAttach.create();
                dialog.setOnShowListener(d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                });
                dialog.show();
                return true; // 长按已处理
            });

            // slcand
            Button SlcandButton = rootView.findViewById(R.id.start_slcand);

            // 访问 SharedPreferences
            SharedPreferences slcand_prefs = requireActivity().getSharedPreferences("slcand_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editorSlcand = slcand_prefs.edit();

            // 加载保存的命令或使用默认
            String savedCmd_slcand = slcand_prefs.getString("slcand_cmd", "slcand -s6 -t sw -S 200000 /dev/ttyUSB0");
            String[] slcandCmdHolder = { savedCmd_slcand };

            SlcandButton.setOnClickListener(v -> {
                String slcandRun = slcandCmdHolder[0];

                if (!slcandRun.isEmpty()) {
                    run_cmd(slcandRun);
                    showToast("按 Ctrl+C 停止. ");
                } else {
                    showToast("请设置您的 slcand 命令！");
                }
            });

            // 长按允许用户编辑命令
            SlcandButton.setOnLongClickListener(v -> {
                AlertDialog.Builder builder_slcand = new AlertDialog.Builder(requireContext());
                builder_slcand.setTitle("编辑命令");

                final EditText input_slcand = new EditText(requireContext());
                input_slcand.setText(slcandCmdHolder[0]);
                builder_slcand.setView(input_slcand);

                builder_slcand.setPositiveButton("保存", (dialog, which) -> {
                    String newSlcandCmd = input_slcand.getText().toString();
                    slcandCmdHolder[0] = newSlcandCmd;

                    // 保存到 SharedPreferences
                    editorSlcand.putString("slcand_cmd", newSlcandCmd);
                    editorSlcand.apply();

                    showToast("命令已更新！");
                });

                builder_slcand.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

                AlertDialog dialog = builder_slcand.create();
                dialog.setOnShowListener(d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                });
                dialog.show();
                return true; // 长按已处理
            });

            // slcan_attach
            Button SlcanAttachButton = rootView.findViewById(R.id.start_slcanattach);

            // 访问 SharedPreferences
            SharedPreferences slcanAttach_prefs = requireActivity().getSharedPreferences("slcanAttach_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editorSlcanAttach = slcanAttach_prefs.edit();

            // 加载保存的命令或使用默认
            String savedCmd_slcanAttach = slcanAttach_prefs.getString("slcanAttach_cmd", "slcan_attach -s6 -o /dev/ttyUSB0");
            String[] slcanAttachCmdHolder = { savedCmd_slcanAttach };

            SlcanAttachButton.setOnClickListener(v -> {
                String slcanAttachRun = slcanAttachCmdHolder[0];

                if (!slcanAttachRun.isEmpty()) {
                    run_cmd(slcanAttachRun);
                    showToast("按 Ctrl+C 停止. ");
                } else {
                    showToast("请设置您的 slcan_attach 命令！");
                }
            });

            // 长按允许用户编辑命令
            SlcanAttachButton.setOnLongClickListener(v -> {
                AlertDialog.Builder builder_slcanAttach = new AlertDialog.Builder(requireContext());
                builder_slcanAttach.setTitle("编辑命令");

                final EditText input_slcanAttach = new EditText(requireContext());
                input_slcanAttach.setText(slcanAttachCmdHolder[0]);
                builder_slcanAttach.setView(input_slcanAttach);

                builder_slcanAttach.setPositiveButton("保存", (dialog, which) -> {
                    String newSlcanAttachCmd = input_slcanAttach.getText().toString();
                    slcanAttachCmdHolder[0] = newSlcanAttachCmd;

                    // 保存到 SharedPreferences
                    editorSlcanAttach.putString("slcanAttach_cmd", newSlcanAttachCmd);
                    editorSlcanAttach.apply();

                    showToast("命令已更新！");
                });

                builder_slcanAttach.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

                AlertDialog dialog = builder_slcanAttach.create();
                dialog.setOnShowListener(d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                });
                dialog.show();
                return true; // 长按已处理
            });

            // hlcan
            Button hlcandButton = rootView.findViewById(R.id.start_hlcand);

            // 访问 SharedPreferences
            SharedPreferences hlcand_prefs = requireActivity().getSharedPreferences("hlcand_prefs", Context.MODE_PRIVATE);
            SharedPreferences.Editor editorHlcand = hlcand_prefs.edit();

            // 加载保存的命令或使用默认
            String savedCmd_hlcand = hlcand_prefs.getString("hlcand_cmd", "hlcand -F -s 500000 /dev/ttyUSB0");
            String[] hlcandCmdHolder = { savedCmd_hlcand };

            hlcandButton.setOnClickListener(v -> {
                String hlcandRun = hlcandCmdHolder[0];

                if (!hlcandRun.isEmpty()) {
                    run_cmd(hlcandRun);
                    showToast("按 Ctrl+C 停止. ");
                } else {
                    showToast("请设置您的 hlcand 命令！");
                }
            });

            // 长按允许用户编辑命令
            hlcandButton.setOnLongClickListener(v -> {
                AlertDialog.Builder builder_hlcand = new AlertDialog.Builder(requireContext());
                builder_hlcand.setTitle("编辑命令");

                final EditText input_hlcand = new EditText(requireContext());
                input_hlcand.setText(hlcandCmdHolder[0]);
                builder_hlcand.setView(input_hlcand);

                builder_hlcand.setPositiveButton("保存", (dialog, which) -> {
                    String newHlcandCmd = input_hlcand.getText().toString();
                    hlcandCmdHolder[0] = newHlcandCmd;

                    // 保存到 SharedPreferences
                    editorHlcand.putString("hlcand_cmd", newHlcandCmd);
                    editorHlcand.apply();

                    showToast("命令已更新！");
                });

                builder_hlcand.setNegativeButton("取消", (dialog, which) -> dialog.cancel());

                AlertDialog dialog = builder_hlcand.create();
                dialog.setOnShowListener(d -> {
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.WHITE);
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.WHITE);
                });
                dialog.show();
                return true; // 长按已处理
            });

            // 启动 rfcomm 绑定
            Button RfcommBinderButton = rootView.findViewById(R.id.start_rfcommbinder);

            RfcommBinderButton.setOnClickListener(v -> {
                String selected_caniface = SelectedIface.getText().toString();
                String bt_target = bt_target_mac.getText().toString();

                if (!selected_caniface.isEmpty() && !bt_target.isEmpty()) {
                    run_cmd("rfcomm bind " + selected_caniface + " " + bt_target);
                } else {
                    showToast("请确保您的 CAN 接口和目标字段已设置！");
                }
            });

            // 启动 Socketcand
            Button SocketCandButton = rootView.findViewById(R.id.start_socketcand);

            SocketCandButton.setOnClickListener(v -> {
                String selected_caniface = SelectedIface.getText().toString();

                if (!selected_caniface.isEmpty()) {
                    run_cmd("socketcand -v -l wlan0 -i " + selected_caniface);
                } else {
                    showToast("请确保您的 CAN 接口字段已设置！");
                }
            });

            // 接口
            // 在类级别声明 SharedPreferences
            SharedPreferences preferences = requireActivity().getSharedPreferences("CANInterfaceState", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();

            // 存储 CAN 接口状态
            Map<String, Boolean> buttonStates = new HashMap<>();

            // 创建活动时从 SharedPreferences 加载保存的按钮状态
            buttonStates.put("start_caniface", preferences.getBoolean("start_caniface", false));

            // CAN 类型下拉框
            // CAN 接口下拉框
            final Spinner canTypeList = rootView.findViewById(R.id.cantype_spinner);
            final String[] interfaceTypeOptions = {"can", "vcan", "slcan"};

            canTypeList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, interfaceTypeOptions));

            canTypeList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    String cantype_selected = parentView.getItemAtPosition(pos).toString();
                    sharedpreferences.edit().putString("cantype_selected", cantype_selected).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 启动 CAN 接口
            Button StartCanButton = rootView.findViewById(R.id.start_caniface);

            // 根据保存的状态设置初始按钮文本
            StartCanButton.setText(Boolean.TRUE.equals(buttonStates.get("start_caniface")) ? "⏹ CAN" : "▶ CAN");

            StartCanButton.setOnClickListener(v -> {
                String selected_caniface = SelectedIface.getText().toString();
                String selected_mtu = SelectedMTU.getText().toString();
                String selected_txqueuelen = SelectedTxqueuelen.getText().toString();
                String interface_type = sharedpreferences.getString("cantype_selected", "");
                boolean isStarted = Boolean.TRUE.equals(buttonStates.get("start_caniface"));

                if (!selected_caniface.isEmpty() && selected_caniface.matches("^(can|vcan|slcan)[0-9]$")) {
                    if (isStarted) {
                        String stopCanIface = exe.RunAsChrootOutput("sudo ip link set " + selected_caniface + " down && echo 成功 || echo 失败");
                        stopCanIface = stopCanIface.trim();
                        if ("vcan".equals(interface_type)) {
                            String delVcanIface = exe.RunAsChrootOutput("sudo ip link delete " + selected_caniface + " && echo 成功 || echo 失败");
                            if (delVcanIface.contains("致命错误: ") || delVcanIface.contains("失败")) {
                                showToast("删除 " + selected_caniface + " 接口失败！");
                            }
                        }
                        if (stopCanIface.contains("致命错误: ") || stopCanIface.contains("失败")) {
                            showToast("停止 " + selected_caniface + " 接口失败！");
                        } else {
                            buttonStates.put("start_caniface", false);
                            StartCanButton.setText("▶ CAN");
                            showToast("接口 " + selected_caniface + " 已停止！");
                        }
                    } else {
                        if ("vcan".equals(interface_type)) {
                            String addVcanIface = exe.RunAsChrootOutput("sudo ip link add dev " + selected_caniface + " type " + interface_type + " && echo 成功 || echo 失败");
                            if (addVcanIface.contains("致命错误: ") || addVcanIface.contains("失败")) {
                                showToast("添加 " + selected_caniface + " 接口失败！接口可能已存在. ");
                            }
                        }
                        if ("can".equals(interface_type) || "slcan".equals(interface_type)) {
                            String usbDevice = exe.RunAsChrootOutput("ls /dev | grep -E '^(ttyUSB|rfcomm|ttyACM)[0-9]+$'");
                            if (usbDevice.isEmpty()) {
                                showToast("未检测到 CAN 硬件, 请连接适配器后重试. ");
                                return;
                            }
                        }

                        if (!selected_mtu.isEmpty()) {
                            exe.RunAsChrootOutput("sudo ip link set " + selected_caniface + " mtu " + selected_mtu + " && echo 成功 || echo 失败");
                        }
                        if (!selected_txqueuelen.isEmpty()) {
                            exe.RunAsChrootOutput("sudo ip link set " + selected_caniface + " txqueuelen " + selected_txqueuelen + " && echo 成功 || echo 失败");
                        }

                        String startCanIface = exe.RunAsChrootOutput("sudo ip link set " + selected_caniface + " up && echo 成功 || echo 失败");
                        if (startCanIface.contains("致命错误: ") || startCanIface.contains("失败")) {
                            showToast("启动 " + selected_caniface + " 接口失败！");
                            return;
                        } else {
                            buttonStates.put("start_caniface", true);
                            StartCanButton.setText("⏹ CAN");
                            showToast("接口 " + selected_caniface + " 已启动！");
                        }
                    }
                } else {
                    if (selected_caniface.isEmpty()) {
                        showToast("请设置一个 CAN 接口！");
                        return;
                    }
                    if (!selected_caniface.matches("^(can|vcan|slcan)[0-9]$")) {
                        showToast("CAN 接口名称应为 \"^(can|vcan|slcan)[0-9]$\"");
                        return;
                    }
                }

                // 将按钮状态保存到 SharedPreferences
                editor.putBoolean("start_caniface", Boolean.TRUE.equals(buttonStates.get("start_caniface")));
                editor.apply();
            });

            // 重置接口按钮
            Button ResetIfaceButton = rootView.findViewById(R.id.reset_iface);

            ResetIfaceButton.setOnClickListener(v -> {
                exe.RunAsChrootOutput("/opt/car_hacking/can_reset.sh");
                buttonStates.put("start_caniface", false);
                StartCanButton.setText("▶ CAN");
                // 将按钮状态保存到 SharedPreferences
                editor.putBoolean("start_caniface", Boolean.TRUE.equals(buttonStates.get("start_caniface")));
                editor.apply();
                showToast("接口已重置！");
            });

            // VIN 信息
            final EditText term = rootView.findViewById(R.id.TerminalOutputVINInfo);
            // 显示
            Button VINShowButton = rootView.findViewById(R.id.vin_show);

            VINShowButton.setOnClickListener(v -> {
                String vinNumber = selected_vin.getText().toString();
                if (vinNumber.length() != 17) {
                    Toast.makeText(context, "VIN 必须恰好为 17 个字符. ", Toast.LENGTH_SHORT).show();
                    return;
                }

                String cmd_show = "/opt/car_hacking/car_venv/bin/vininfo show " + vinNumber + " | tr -s [:space:] > /sdcard/nh_files/can_arsenal/output.txt";
                new BootKali(cmd_show).run_bg();
                try {
                    Thread.sleep(SHORT_DELAY);
                    String output = exe.RunAsRootOutput("cat " + NhPaths.APP_SD_FILES_PATH + "/can_arsenal/output.txt");
                    term.setText(output);
                } catch (Exception e) {
                    Log.e("VINShowError", "读取 VIN 信息时发生异常", e);
                    term.setText("错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            });

            // 检查
            Button VINCheckButton = rootView.findViewById(R.id.vin_check);

            VINCheckButton.setOnClickListener(v -> {
                String vinNumber = selected_vin.getText().toString();
                if (vinNumber.length() != 17) {
                    Toast.makeText(context, "VIN 必须恰好为 17 个字符. ", Toast.LENGTH_SHORT).show();
                    return;
                }

                String cmd_check = "/opt/car_hacking/car_venv/bin/vininfo check " + vinNumber + " | tr -s [:space:] > /sdcard/nh_files/can_arsenal/output.txt";
                new BootKali(cmd_check).run_bg();
                try {
                    Thread.sleep(SHORT_DELAY);
                    String output = exe.RunAsRootOutput("cat " + NhPaths.APP_SD_FILES_PATH + "/can_arsenal/output.txt");
                    term.setText(output);
                } catch (Exception e) {
                    Log.e("VINCheckError", "读取 VIN 信息时发生异常", e);
                    term.setText("错误: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                }
            });

            return rootView;
        }
    }

    public static class ToolsFragment extends CANFragment {
        final ShellExecuter exe = new ShellExecuter();
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private Activity activity;
        private boolean isInteractiveEnabled = false;
        private boolean isVerboseEnabled = false;
        private boolean isDisableLoopbackEnabled = false;
        private Context context;
        private String selected_caniface;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity = getActivity();
            context = getContext();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.can_tools, container, false);

            final EditText cansend_sequence = rootView.findViewById(R.id.cansend_sequence);
            final EditText SelectedRHost = rootView.findViewById(R.id.cannelloni_rhost);
            final EditText SelectedRPort = rootView.findViewById(R.id.cannelloni_rport);
            final EditText SelectedLPort = rootView.findViewById(R.id.cannelloni_lport);
            final EditText inputfilepath = rootView.findViewById(R.id.inputfilepath);
            final Button inputfilebrowse = rootView.findViewById(R.id.inputfilebrowse);
            final EditText outputfilepath = rootView.findViewById(R.id.outputfilepath);
            final Button outputfilebrowse = rootView.findViewById(R.id.outputfilebrowse);
            final EditText CustomCmd = rootView.findViewById(R.id.customcmd);

            // 接口
            final Spinner deviceList = rootView.findViewById(R.id.device_interface);

            executorService.submit(() -> {
                String result = exe.RunAsChrootOutput(
                        "ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$';" +
                                "ls /dev | grep -E '^(ttyUSB|rfcomm|ttyACM)[0-9]+$' | sed 's|^|/dev/|'"
                );

                ArrayList<String> deviceIfaces = new ArrayList<>();

                if (result == null || result.trim().isEmpty()) {
                    deviceIfaces.add("无");
                } else {
                    deviceIfaces.addAll(Arrays.asList(result.split("\n")));
                }

                // 回到主线程更新 UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, deviceIfaces);
                    deviceList.setAdapter(adapter);

                    // 恢复之前的选择（如果已保存）
                    int savedPosition = sharedpreferences.getInt("selected_usb", 0);
                    if (savedPosition < deviceIfaces.size()) {
                        deviceList.setSelection(savedPosition);
                        selected_caniface = deviceIfaces.get(savedPosition);
                    } else {
                        selected_caniface = "无";
                    }

                    deviceList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                            selected_caniface = parentView.getItemAtPosition(pos).toString();
                            sharedpreferences.edit().putInt("selected_usb", pos).apply();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            selected_caniface = "无";
                        }
                    });

                    // 可选: 显示新检测到的设备
                    if (!deviceIfaces.contains("无")) {
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                });
            });

            // 刷新状态
            ImageButton RefreshUSB = rootView.findViewById(R.id.refreshUSB);
            RefreshUSB.setOnClickListener(v -> {
                showToast("正在刷新设备...");
                refresh(rootView);
            });
            executorService.submit(() -> refresh(rootView));

            // 高级选项切换
            Button btnToggle = rootView.findViewById(R.id.btn_toggle_advanced);
            LinearLayout advancedOptionsLayout = rootView.findViewById(R.id.tools_advanced_options);

            btnToggle.setOnClickListener(v -> {
                if (advancedOptionsLayout.getVisibility() == View.GONE) {
                    advancedOptionsLayout.setVisibility(View.VISIBLE);
                    btnToggle.setText("隐藏高级选项");
                } else {
                    advancedOptionsLayout.setVisibility(View.GONE);
                    btnToggle.setText("高级选项");
                }
            });

            // 交互模式
            Button btnInteractive = rootView.findViewById(R.id.btn_toggle_interactive);

            btnInteractive.setOnClickListener(v -> {
                isInteractiveEnabled = !isInteractiveEnabled;

                int color = isInteractiveEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnInteractive.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 详细模式
            Button btnVerbose = rootView.findViewById(R.id.btn_toggle_verbose);

            btnVerbose.setOnClickListener(v -> {
                isVerboseEnabled = !isVerboseEnabled;

                int color = isVerboseEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnVerbose.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 禁用本地回环
            Button btnLoopback = rootView.findViewById(R.id.btn_toggle_loopback);

            btnLoopback.setOnClickListener(v -> {
                isDisableLoopbackEnabled = !isDisableLoopbackEnabled;

                int color = isDisableLoopbackEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnLoopback.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 输入文件
            final ActivityResultLauncher<Intent> inputFileLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            assert uri != null;
                            inputfilepath.setText(uri.getPath());
                        }
                    }
            );

            inputfilebrowse.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("log/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                inputFileLauncher.launch(Intent.createChooser(intent, "选择输入文件"));
            });

            // 输出文件
            final ActivityResultLauncher<Intent> outputFileLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            assert uri != null;
                            outputfilepath.setText(uri.getPath());
                        }
                    }
            );

            outputfilebrowse.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("log/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                outputFileLauncher.launch(Intent.createChooser(intent, "选择输出文件"));
            });

            // 工具
            // 启动 CanGen
            Button CanGenButton = rootView.findViewById(R.id.start_cangen);

            CanGenButton.setOnClickListener(v -> {
                String verboseEnabled = isVerboseEnabled ? " -v" : "";
                String disableLoopbackEnabled = isDisableLoopbackEnabled ? " -x" : "";
                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    run_cmd("cangen " + selected_caniface + verboseEnabled + disableLoopbackEnabled);
                } else {
                    showToast("请确保您的 CAN 接口字段已设置！");
                }
                activity.invalidateOptionsMenu();
            });

            // 启动 CanSniffer
            Button CanSnifferButton = rootView.findViewById(R.id.start_cansniffer);

            CanSnifferButton.setOnClickListener(v -> {
                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    run_cmd("cansniffer " + selected_caniface);
                } else {
                    showToast("请确保您的 CAN 接口字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动 CanDump
            Button CanDumpButton = rootView.findViewById(R.id.start_candump);

            CanDumpButton.setOnClickListener(v -> {
                String outputfile = outputfilepath.getText().toString();

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无") && !outputfile.isEmpty()) {
                    run_cmd("candump " + selected_caniface + " -f " + outputfile);
                } else {
                    showToast("请确保您的 CAN 接口和输出文件字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动 CanSend
            Button CanSendButton = rootView.findViewById(R.id.start_cansend);

            CanSendButton.setOnClickListener(v -> {
                String sequence = cansend_sequence.getText().toString();

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无") && !sequence.isEmpty()) {
                    run_cmd("cansend " + selected_caniface + " " + sequence);
                } else {
                    showToast("请确保您的 CAN 接口和序列字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动 CanPlayer
            Button CanPlayerButton = rootView.findViewById(R.id.start_canplayer);

            CanPlayerButton.setOnClickListener(v -> {
                String interactiveEnabled = isInteractiveEnabled ? " -i" : "";
                String verboseEnabled = isVerboseEnabled ? " -v" : "";
                String disableLoopbackEnabled = isDisableLoopbackEnabled ? " -x" : "";
                String inputfile = inputfilepath.getText().toString();

                if (!inputfile.isEmpty()) {
                    run_cmd("canplayer -I " + inputfile + interactiveEnabled + verboseEnabled + disableLoopbackEnabled);
                } else {
                    showToast("请确保您的输入文件字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动 SequenceFinder
            final Button SequenceFinderButton = rootView.findViewById(R.id.start_sequencefinder);

            SequenceFinderButton.setOnClickListener(v -> {
                String inputfile = inputfilepath.getText().toString();

                if (!inputfile.isEmpty()) {
                    run_cmd("/opt/car_hacking/sequence_finder.sh " + inputfile);
                } else {
                    showToast("请确保您的输入文件字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动 Freediag
            Button FreediagButton = rootView.findViewById(R.id.start_freediag);

            FreediagButton.setOnClickListener(v -> {
                run_cmd("sudo -u kali freediag");

                activity.invalidateOptionsMenu();
            });

            // 启动 diag_test
            Button diagTestButton = rootView.findViewById(R.id.start_diagtest);

            diagTestButton.setOnClickListener(v -> {
                run_cmd("sudo -u kali diag_test");

                activity.invalidateOptionsMenu();
            });

            // Cannelloni
            Button CannelloniButton = rootView.findViewById(R.id.start_cannelloni);

            CannelloniButton.setOnClickListener(v ->  {
                String rhost = SelectedRHost.getText().toString();
                String rport = SelectedRPort.getText().toString();
                String lport = SelectedLPort.getText().toString();

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无") && !rhost.isEmpty() && !rport.isEmpty() && !lport.isEmpty()) {
                    run_cmd("sudo cannelloni -I " + selected_caniface + " -R " + rhost + " -r " + rport + " -l " + lport);
                } else {
                    showToast("请确保您的 CAN 接口、RHOST、RPORT、LPORT 字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动 Asc2Log
            Button Asc2LogButton = rootView.findViewById(R.id.start_asc2log);

            Asc2LogButton.setOnClickListener(v ->  {
                String inputfile = inputfilepath.getText().toString();
                String outputfile = outputfilepath.getText().toString();

                if (!inputfile.isEmpty() && !outputfile.isEmpty()) {
                    run_cmd("asc2log -I " + inputfile + " -O " + outputfile);
                } else {
                    showToast("请确保您的输入和输出文件字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动 Log2asc
            Button Log2AscButton = rootView.findViewById(R.id.start_log2asc);

            Log2AscButton.setOnClickListener(v ->  {
                String inputfile = inputfilepath.getText().toString();
                String outputfile = outputfilepath.getText().toString();

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无") && !inputfile.isEmpty() && !outputfile.isEmpty()) {
                    run_cmd("log2asc -I " + inputfile + " -O " + outputfile + " " + selected_caniface);
                } else {
                    showToast("请确保您的 CAN 接口、输入和输出文件字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动自定义命令
            Button CustomCmdButton = rootView.findViewById(R.id.start_customcmd);

            CustomCmdButton.setOnClickListener(v ->  {
                String command = CustomCmd.getText().toString();

                if (!command.isEmpty()) {
                    run_cmd(command);
                } else {
                    showToast("请确保您的自定义命令字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            return rootView;
        }

        // 刷新接口
        private void refresh(View CANFragment) {
            final Spinner deviceList = CANFragment.findViewById(R.id.device_interface);
            if (context == null) return;

            executorService.submit(() -> {
                String outputDevice = exe.RunAsChrootOutput("ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$'");
                final ArrayList<String> deviceIfaces = new ArrayList<>();
                if (outputDevice != null && !outputDevice.isEmpty()) {
                    final String[] deviceifacesArray = outputDevice.split("\n");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        int lastiface = sharedpreferences.getInt("selected_device", 0);
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceifacesArray));
                            deviceList.setSelection(lastiface);
                        });
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                } else {
                    deviceIfaces.add("无");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceIfaces));
                            sharedpreferences.edit().putInt("selected_device", deviceList.getSelectedItemPosition()).apply();
                        });
                    }
                }
            });

            String message = "设备列表已刷新！";
            showToast(message);
        }
    }

    public static class CANUSBFragment extends CANFragment {
        final ShellExecuter exe = new ShellExecuter();
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private Activity activity;
        private boolean isDebugEnabled = false;
        private Context context;
        private EditText SelectedBaudrateUSB;
        private EditText SelectedCanSpeedUSB;
        private String selected_usb;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity = getActivity();
            context = getContext();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.can_canusb, container, false);

            SelectedBaudrateUSB = rootView.findViewById(R.id.baudrate_usb);
            SelectedCanSpeedUSB = rootView.findViewById(R.id.canspeed_usb);

            // USB 接口
            final Spinner deviceList = rootView.findViewById(R.id.device_interface);

            executorService.submit(() -> {
                String result = exe.RunAsChrootOutput(
                        "ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$';" +
                                "ls /dev | grep -E '^(ttyUSB|rfcomm|ttyACM)[0-9]+$' | sed 's|^|/dev/|'"
                );

                ArrayList<String> deviceIfaces = new ArrayList<>();

                if (result == null || result.trim().isEmpty()) {
                    deviceIfaces.add("无");
                } else {
                    deviceIfaces.addAll(Arrays.asList(result.split("\n")));
                }

                // 回到主线程更新 UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, deviceIfaces);
                    deviceList.setAdapter(adapter);

                    // 恢复之前的选择（如果已保存）
                    int savedPosition = sharedpreferences.getInt("selected_usb", 0);
                    if (savedPosition < deviceIfaces.size()) {
                        deviceList.setSelection(savedPosition);
                        selected_usb = deviceIfaces.get(savedPosition);
                    } else {
                        selected_usb = "无";
                    }

                    deviceList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                            selected_usb = parentView.getItemAtPosition(pos).toString();
                            sharedpreferences.edit().putInt("selected_usb", pos).apply();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            selected_usb = "无";
                        }
                    });

                    if (!deviceIfaces.contains("无")) {
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                });
            });

            // 刷新状态
            ImageButton RefreshUSB = rootView.findViewById(R.id.refreshUSB);
            RefreshUSB.setOnClickListener(v -> {
                showToast("正在刷新设备...");
                refresh(rootView);
            });
            executorService.submit(() -> refresh(rootView));

            // CAN-USB 模式下拉框
            final Spinner canusbModeList = rootView.findViewById(R.id.usb_mode_spinner);
            final String[] modeOptions = {"模式", "0", "1", "2"};

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, modeOptions) {
                @Override
                public boolean isEnabled(int position) {
                    // 禁用“模式”项
                    return position != 0;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView tv = (TextView) view;
                    if (position == 0) {
                        tv.setTextColor(Color.GRAY);  // 提示文本颜色
                    } else {
                        tv.setTextColor(Color.WHITE); // 普通文本
                    }
                    return view;
                }
            };

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            canusbModeList.setAdapter(adapter);
            canusbModeList.setSelection(0);  // 初始选择“模式”

            canusbModeList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    if (pos != 0) { // 忽略“模式”提示
                        String canusbmode_selected = parentView.getItemAtPosition(pos).toString();
                        sharedpreferences.edit().putString("canusbmode_selected", canusbmode_selected).apply();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 切换按钮
            // 计数器
            Button btnCounter = rootView.findViewById(R.id.btn_toggle_usb_counter);
            EditText selectedCount = rootView.findViewById(R.id.usb_counter_value);

            btnCounter.setOnClickListener(v -> {
                boolean visible = selectedCount.getVisibility() == View.VISIBLE;
                selectedCount.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnCounter.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 数据
            Button btnData = rootView.findViewById(R.id.btn_toggle_usb_data);
            EditText selectedData = rootView.findViewById(R.id.usb_data_value);

            btnData.setOnClickListener(v -> {
                boolean visible = selectedData.getVisibility() == View.VISIBLE;
                selectedData.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnData.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // ID
            Button btnID = rootView.findViewById(R.id.btn_toggle_usb_id);
            EditText selectedID = rootView.findViewById(R.id.usb_id_value);

            btnID.setOnClickListener(v -> {
                boolean visible = selectedID.getVisibility() == View.VISIBLE;
                selectedID.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnID.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 模式
            Button btnMode = rootView.findViewById(R.id.btn_toggle_usb_mode);

            btnMode.setOnClickListener(v -> {
                boolean visible = canusbModeList.getVisibility() == View.VISIBLE;
                canusbModeList.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnMode.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 休眠
            Button btnSleep = rootView.findViewById(R.id.btn_toggle_usb_sleep);
            EditText selectedSleep = rootView.findViewById(R.id.usb_sleep_value);

            btnSleep.setOnClickListener(v -> {
                boolean visible = selectedSleep.getVisibility() == View.VISIBLE;
                selectedSleep.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnSleep.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 调试（TTY 输出）
            Button btnDebug = rootView.findViewById(R.id.btn_toggle_usb_ttyOutput);

            btnDebug.setOnClickListener(v -> {
                isDebugEnabled = !isDebugEnabled;

                int color = isDebugEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnDebug.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 启动 USB-CAN
            Button USBCanSendButton = rootView.findViewById(R.id.start_canusb_send);

            USBCanSendButton.setOnClickListener(v -> {
                String USBCANSpeed = SelectedCanSpeedUSB.getText().toString();
                String USBBaudrate = SelectedBaudrateUSB.getText().toString();
                String debugEnabled = isDebugEnabled ? " -t" : "";
                String countValue = getVisibleParam(selectedCount, " -n ");
                String idValue = getVisibleParam(selectedID, " -i ");
                String dataValue = getVisibleParam(selectedData, " -j ");
                String sleepValue = getVisibleParam(selectedSleep, " -g ");
                String modeValue = getVisibleParam(canusbModeList, " -m ");

                if (!selected_usb.isEmpty() && !selected_usb.equals("无") && !USBCANSpeed.isEmpty() && !USBBaudrate.isEmpty()) {
                    run_cmd("canusb -d " + selected_usb + " -s " + USBCANSpeed + " -b " + USBBaudrate + debugEnabled + idValue + dataValue + sleepValue + countValue + modeValue);
                } else {
                    showToast("请确保您的 USB 设备和 USB CAN 速率、波特率、数据字段已设置！");
                }

                activity.invalidateOptionsMenu();
            });

            return rootView;
        }

        private String getVisibleParam(View view, String prefix) {
            if (view.getVisibility() == View.VISIBLE) {
                if (view instanceof EditText) {
                    String input = ((EditText) view).getText().toString().trim();
                    if (!input.isEmpty()) {
                        return prefix + input;
                    }
                } else if (view instanceof Spinner) {
                    String selected = ((Spinner) view).getSelectedItem().toString().trim();
                    if (!selected.isEmpty()) {
                        return prefix + selected;
                    }
                }
            }
            return "";
        }

        // 刷新主界面
        private void refresh(View CANFragment) {
            final Spinner deviceList = CANFragment.findViewById(R.id.device_interface);
            if (context == null) return;

            executorService.submit(() -> {
                String outputDevice = exe.RunAsChrootOutput("ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$';ls /dev | grep -E '^(ttyUSB|rfcomm|ttyACM)[0-9]+$' | sed 's|^|/dev/|'");
                final ArrayList<String> deviceIfaces = new ArrayList<>();
                if (outputDevice != null && !outputDevice.isEmpty()) {
                    final String[] deviceifacesArray = outputDevice.split("\n");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        int lastiface = sharedpreferences.getInt("selected_device", 0);
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceifacesArray));
                            deviceList.setSelection(lastiface);
                        });
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                } else {
                    deviceIfaces.add("无");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceIfaces));
                            sharedpreferences.edit().putInt("selected_device", deviceList.getSelectedItemPosition()).apply();
                        });
                    }
                }
            });

            String message = "设备列表已刷新！";
            showToast(message);
        }
    }

    public static class CANCARIBOUFragment extends CANFragment {
        final ShellExecuter exe = new ShellExecuter();
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private Activity activity;
        private boolean isCandumpEnabled = false;
        private boolean isLoopEnabled = false;
        private boolean isOutputEnabled = false;
        private boolean isPadEnabled = false;
        private boolean isReverseEnabled = false;
        private Context context;
        private EditText SelectedFile;
        private EditText SelectedMessage;
        private String selected_caniface;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            activity = getActivity();
            context = getContext();
        }

        @SuppressLint("SetTextI18n")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.can_caribou, container, false);

            SelectedFile = rootView.findViewById(R.id.caribou_file);
            SelectedMessage = rootView.findViewById(R.id.caribou_message);

            // 接口
            final Spinner deviceList = rootView.findViewById(R.id.device_interface);

            executorService.submit(() -> {
                String result = exe.RunAsChrootOutput(
                        "ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$';" +
                                "ls /dev | grep -E '^(ttyUSB|rfcomm|ttyACM)[0-9]+$' | sed 's|^|/dev/|'"
                );

                ArrayList<String> deviceIfaces = new ArrayList<>();

                if (result == null || result.trim().isEmpty()) {
                    deviceIfaces.add("无");
                } else {
                    deviceIfaces.addAll(Arrays.asList(result.split("\n")));
                }

                // 回到主线程更新 UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, deviceIfaces);
                    deviceList.setAdapter(adapter);

                    // 恢复之前的选择（如果已保存）
                    int savedPosition = sharedpreferences.getInt("selected_usb", 0);
                    if (savedPosition < deviceIfaces.size()) {
                        deviceList.setSelection(savedPosition);
                        selected_caniface = deviceIfaces.get(savedPosition);
                    } else {
                        selected_caniface = "无";
                    }

                    deviceList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                            selected_caniface = parentView.getItemAtPosition(pos).toString();
                            sharedpreferences.edit().putInt("selected_usb", pos).apply();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            selected_caniface = "无";
                        }
                    });

                    // 可选: 显示新检测到的设备
                    if (!deviceIfaces.contains("无")) {
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                });
            });

            // 刷新状态
            ImageButton RefreshUSB = rootView.findViewById(R.id.refreshUSB);
            RefreshUSB.setOnClickListener(v -> {
                showToast("正在刷新设备...");
                refresh(rootView);
            });
            executorService.submit(() -> refresh(rootView));

            // 文件
            final ActivityResultLauncher<Intent> inputFileLauncher = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            assert uri != null;
                            SelectedFile.setText(uri.getPath());
                        }
                    }
            );

            final Button cariboufilebrowse = rootView.findViewById(R.id.cariboufilebrowse);
            cariboufilebrowse.setOnClickListener(v -> {
                Intent intent = new Intent();
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                inputFileLauncher.launch(Intent.createChooser(intent, "选择输入文件"));
            });

            // 高级选项切换
            Button btnToggle = rootView.findViewById(R.id.btn_toggle_advanced);
            LinearLayout advancedOptionsLayout = rootView.findViewById(R.id.caribou_advanced_options);

            btnToggle.setOnClickListener(v -> {
                if (advancedOptionsLayout.getVisibility() == View.GONE) {
                    advancedOptionsLayout.setVisibility(View.VISIBLE);
                    btnToggle.setText("隐藏高级选项");
                } else {
                    advancedOptionsLayout.setVisibility(View.GONE);
                    btnToggle.setText("高级选项");
                }
            });

            // 高级选项 - 选项
            // 起始地址
            Button btnStartAddr = rootView.findViewById(R.id.btn_toggle_start_addr);
            EditText selectedAddr = rootView.findViewById(R.id.start_addr_value);

            btnStartAddr.setOnClickListener(v -> {
                boolean visible = selectedAddr.getVisibility() == View.VISIBLE;
                selectedAddr.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnStartAddr.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 长度
            Button btnLength = rootView.findViewById(R.id.btn_toggle_length);
            EditText selectedLength = rootView.findViewById(R.id.length_value);

            btnLength.setOnClickListener(v -> {
                boolean visible = selectedLength.getVisibility() == View.VISIBLE;
                selectedLength.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnLength.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 分隔行
            Button btnLine = rootView.findViewById(R.id.btn_toggle_separateLine);
            EditText selectedLine = rootView.findViewById(R.id.separate_line_value);

            btnLine.setOnClickListener(v -> {
                boolean visible = selectedLine.getVisibility() == View.VISIBLE;
                selectedLine.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnLine.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 种子
            Button btnSeed = rootView.findViewById(R.id.btn_toggle_seed);
            EditText selectedSeed = rootView.findViewById(R.id.seed_value);

            btnSeed.setOnClickListener(v -> {
                boolean visible = selectedSeed.getVisibility() == View.VISIBLE;
                selectedSeed.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnSeed.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // ID
            Button btnID = rootView.findViewById(R.id.btn_toggle_id);
            EditText selectedID = rootView.findViewById(R.id.id_value);

            btnID.setOnClickListener(v -> {
                boolean visible = selectedID.getVisibility() == View.VISIBLE;
                selectedID.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnID.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 源
            Button btnSrc = rootView.findViewById(R.id.btn_toggle_src);
            EditText selectedSrc = rootView.findViewById(R.id.src_value);

            btnSrc.setOnClickListener(v -> {
                boolean visible = selectedSrc.getVisibility() == View.VISIBLE;
                selectedSrc.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnSrc.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 目标
            Button btnDst = rootView.findViewById(R.id.btn_toggle_dst);
            EditText selectedDst = rootView.findViewById(R.id.dst_value);

            btnDst.setOnClickListener(v -> {
                boolean visible = selectedDst.getVisibility() == View.VISIBLE;
                selectedDst.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnDst.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 最小值
            Button btnMin = rootView.findViewById(R.id.btn_toggle_min);
            EditText selectedMin = rootView.findViewById(R.id.min_value);

            btnMin.setOnClickListener(v -> {
                boolean visible = selectedMin.getVisibility() == View.VISIBLE;
                selectedMin.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnMin.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 最大值
            Button btnMax = rootView.findViewById(R.id.btn_toggle_max);
            EditText selectedMax = rootView.findViewById(R.id.max_value);

            btnMax.setOnClickListener(v -> {
                boolean visible = selectedMax.getVisibility() == View.VISIBLE;
                selectedMax.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnMax.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 延迟
            Button btnDelay = rootView.findViewById(R.id.btn_toggle_delay);
            EditText selectedDelay = rootView.findViewById(R.id.delay_value);

            btnDelay.setOnClickListener(v -> {
                boolean visible = selectedDelay.getVisibility() == View.VISIBLE;
                selectedDelay.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnDelay.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 填充
            Button btnPad = rootView.findViewById(R.id.btn_toggle_pad);

            btnPad.setOnClickListener(v -> {
                isPadEnabled = !isPadEnabled;

                int color = isPadEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnPad.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // Candump 格式
            Button btnCandump = rootView.findViewById(R.id.btn_toggle_candump);

            btnCandump.setOnClickListener(v -> {
                isCandumpEnabled = !isCandumpEnabled;

                int color = isCandumpEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnCandump.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 保存输出
            Button btnOutput = rootView.findViewById(R.id.btn_toggle_output);

            btnOutput.setOnClickListener(v -> {
                isOutputEnabled = !isOutputEnabled;

                int color = isOutputEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnOutput.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 循环
            Button btnLoop = rootView.findViewById(R.id.btn_toggle_loop);

            btnLoop.setOnClickListener(v -> {
                isLoopEnabled = !isLoopEnabled;

                int color = isLoopEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnLoop.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 反向
            Button btnReverse = rootView.findViewById(R.id.btn_toggle_reverse);

            btnReverse.setOnClickListener(v -> {
                isReverseEnabled = !isReverseEnabled;

                int color = isReverseEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
                btnReverse.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // 启动转储
            Button CaribouDumpButton = rootView.findViewById(R.id.start_dump);

            CaribouDumpButton.setOnClickListener(v -> {
                String candumpFormat = isCandumpEnabled ? " -t" : "";
                String outputEnabled = isOutputEnabled ? " -f " + SelectedFile.getText().toString() : "";
                String separateLineValue = getVisibleParam(selectedLine, " -s ");
                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " dump" + separateLineValue + candumpFormat + outputEnabled);
                } else {
                    showToast("请选择一个 CAN 接口！");
                }

                activity.invalidateOptionsMenu();
            });

            // 启动监听器
            Button CaribouListenerButton = rootView.findViewById(R.id.start_listener);

            CaribouListenerButton.setOnClickListener(v -> {
                String reverseEnabled = isReverseEnabled ? " -r" : "";
                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " listener" + reverseEnabled);
                } else {
                    showToast("请选择一个 CAN 接口！");
                }

                activity.invalidateOptionsMenu();
            });

            // FUZZER 下拉框
            final Spinner FUZZERList = rootView.findViewById(R.id.fuzzer_spinner);
            final String[] FUZZEROptions = {"brute","identify","mutate","random","replay"};

            FUZZERList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, FUZZEROptions));

            FUZZERList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    String fuzzer_selected = parentView.getItemAtPosition(pos).toString();
                    sharedpreferences.edit().putString("fuzzer_selected", fuzzer_selected).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 启动 FUZZER
            Button CaribouFUZZERButton = rootView.findViewById(R.id.start_fuzzer);

            CaribouFUZZERButton.setOnClickListener(v -> {
                String fuzzer_module = sharedpreferences.getString("fuzzer_selected", "");
                String idValue           = getVisibleParam(selectedID, " ");
                String minValue          = getVisibleParam(selectedMin, " -min ");
                String outputEnabled = isOutputEnabled ? " -f " + SelectedFile.getText().toString() : "";
                String seedValue         = getVisibleParam(selectedSeed, " --seed ");

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    if ("brute".equals(fuzzer_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " fuzzer brute" + idValue);
                    }
                    if ("identify".equals(fuzzer_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " fuzzer identify" + outputEnabled);
                    }
                    if ("mutate".equals(fuzzer_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " fuzzer mutate" + idValue);
                    }
                    if ("random".equals(fuzzer_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " fuzzer random" + minValue + seedValue + outputEnabled);
                    }
                    if ("replay".equals(fuzzer_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " fuzzer replay" + outputEnabled);
                    }
                } else {
                    showToast("请选择一个 CAN 接口！");
                }

                activity.invalidateOptionsMenu();
            });

            // SEND 下拉框
            final Spinner SENDList = rootView.findViewById(R.id.send_spinner);
            final String[] SENDTypeOptions = {"file","message"};

            SENDList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, SENDTypeOptions));

            SENDList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    String send_selected = parentView.getItemAtPosition(pos).toString();
                    sharedpreferences.edit().putString("send_selected", send_selected).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 启动 SEND
            Button CaribouSENDButton = rootView.findViewById(R.id.start_send);

            CaribouSENDButton.setOnClickListener(v -> {
                String selected_message = SelectedMessage.getText().toString();
                String selected_file    = SelectedFile.getText().toString();
                String delayValue       = getVisibleParam(selectedDelay, " -d ");
                String loopEnabled      = isLoopEnabled ? " -l" : "";
                String padEnabled       = isPadEnabled ? " -p" : "";
                String send_module      = sharedpreferences.getString("send_selected", "");

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    if ("file".equals(send_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " send file" + delayValue + loopEnabled + " " + selected_file);
                    }
                    if ("message".equals(send_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " send message" + padEnabled + delayValue + loopEnabled + " " + selected_message);
                    }
                } else {
                    showToast("请选择一个 CAN 接口！");
                }

                activity.invalidateOptionsMenu();
            });

            // UDS 下拉框
            final Spinner UDSList = rootView.findViewById(R.id.uds_spinner);
            final String[] UDSTypeOptions = {"discovery","services"};

            UDSList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, UDSTypeOptions));

            UDSList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    String uds_selected = parentView.getItemAtPosition(pos).toString();
                    sharedpreferences.edit().putString("uds_selected", uds_selected).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 启动 UDS
            Button CaribouUDSButton = rootView.findViewById(R.id.start_uds);

            CaribouUDSButton.setOnClickListener(v -> {
                String srcValue          = getVisibleParam(selectedSrc, " ");
                String dstValue          = getVisibleParam(selectedDst, " ");
                String minValue          = getVisibleParam(selectedMin, " -min ");
                String maxValue          = getVisibleParam(selectedMax, " -max ");
                String delayValue        = getVisibleParam(selectedDelay, " -d ");
                String uds_module        = sharedpreferences.getString("uds_selected", "");

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    if ("discovery".equals(uds_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " uds discovery" + minValue + maxValue + delayValue);
                    }
                    if ("services".equals(uds_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " uds services" + srcValue + dstValue);
                    }
                } else {
                    showToast("请选择一个 CAN 接口！");
                }

                activity.invalidateOptionsMenu();
            });

            // XCP 下拉框
            final Spinner XCPList = rootView.findViewById(R.id.xcp_spinner);
            final String[] XCPOptions = {"discovery","info","dump"};

            XCPList.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, XCPOptions));

            XCPList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    String xcp_selected = parentView.getItemAtPosition(pos).toString();
                    sharedpreferences.edit().putString("xcp_selected", xcp_selected).apply();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 启动 XCP
            Button CaribouXCPButton = rootView.findViewById(R.id.start_xcp);

            CaribouXCPButton.setOnClickListener(v -> {
                String addrValue         = getVisibleParam(selectedAddr, " ");
                String lengthValue       = getVisibleParam(selectedLength, " ");
                String outputEnabled = isOutputEnabled ? " -f " + SelectedFile.getText().toString() : "";
                String srcValue          = getVisibleParam(selectedSrc, " ");
                String dstValue          = getVisibleParam(selectedDst, " ");
                String minValue          = getVisibleParam(selectedMin, " -min ");
                String maxValue          = getVisibleParam(selectedMax, " -max ");
                String xcp_module = sharedpreferences.getString("xcp_selected", "");

                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    if ("discovery".equals(xcp_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " xcp discovery" + minValue + maxValue);
                    }
                    if ("info".equals(xcp_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " xcp info" + srcValue + dstValue);
                    }
                    if ("dump".equals(xcp_module)) {
                        run_cmd("printf \"[default]\ninterface = socketcan\nchannel = " + selected_caniface + "\" > $HOME/.canrc && caringcaribou -i " + selected_caniface + " xcp dump" + srcValue + dstValue + addrValue + lengthValue + outputEnabled);
                    }
                } else {
                    showToast("请选择一个 CAN 接口！");
                }

                activity.invalidateOptionsMenu();
            });

            return rootView;
        }

        // 刷新接口
        private void refresh(View CANFragment) {
            final Spinner deviceList = CANFragment.findViewById(R.id.device_interface);
            if (context == null) return;

            executorService.submit(() -> {
                String outputDevice = exe.RunAsChrootOutput("ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$'");
                final ArrayList<String> deviceIfaces = new ArrayList<>();
                if (outputDevice != null && !outputDevice.isEmpty()) {
                    final String[] deviceifacesArray = outputDevice.split("\n");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        int lastiface = sharedpreferences.getInt("selected_device", 0);
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceifacesArray));
                            deviceList.setSelection(lastiface);
                        });
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                } else {
                    deviceIfaces.add("无");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceIfaces));
                            sharedpreferences.edit().putInt("selected_device", deviceList.getSelectedItemPosition()).apply();
                        });
                    }
                }
            });

            String message = "设备列表已刷新！";
            showToast(message);
        }

        private String getVisibleParam(EditText field, String prefix) {
            if (field.getVisibility() == View.VISIBLE) {
                String input = field.getText().toString().trim();
                if (!input.isEmpty()) {
                    return prefix + input;
                }
            }
            return "";
        }
    }

    public static class CANICSIMFragment extends CANFragment {
        final ShellExecuter exe = new ShellExecuter();
        private boolean isRandomizeEnabled = false;
        private final ExecutorService executorService = Executors.newCachedThreadPool();
        private static final String ICSIM_SCRIPT_PATH = "/opt/car_hacking/icsim_service.sh";
        private static final long SHORT_DELAY = 1000;
        private static final long LONG_DELAY = 2000;
        private Context context;
        private String selected_caniface;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
        }

        @SuppressLint("SetJavaScriptEnabled")
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.can_icsim, container, false);

            // 接口
            final Spinner deviceList = rootView.findViewById(R.id.device_interface);

            executorService.submit(() -> {
                String result = exe.RunAsChrootOutput(
                        "ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$';" +
                                "ls /dev | grep -E '^(ttyUSB|rfcomm|ttyACM)[0-9]+$' | sed 's|^|/dev/|'"
                );

                ArrayList<String> deviceIfaces = new ArrayList<>();

                if (result == null || result.trim().isEmpty()) {
                    deviceIfaces.add("无");
                } else {
                    deviceIfaces.addAll(Arrays.asList(result.split("\n")));
                }

                // 回到主线程更新 UI
                new Handler(Looper.getMainLooper()).post(() -> {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, deviceIfaces);
                    deviceList.setAdapter(adapter);

                    // 恢复之前的选择（如果已保存）
                    int savedPosition = sharedpreferences.getInt("selected_usb", 0);
                    if (savedPosition < deviceIfaces.size()) {
                        deviceList.setSelection(savedPosition);
                        selected_caniface = deviceIfaces.get(savedPosition);
                    } else {
                        selected_caniface = "无";
                    }

                    deviceList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                            selected_caniface = parentView.getItemAtPosition(pos).toString();
                            sharedpreferences.edit().putInt("selected_usb", pos).apply();
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parentView) {
                            selected_caniface = "无";
                        }
                    });

                    // 可选: 显示新检测到的设备
                    if (!deviceIfaces.contains("无")) {
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                });
            });

            // 级别下拉框
            final Spinner levelList = rootView.findViewById(R.id.level_spinner);
            // 0 = 不对数据包添加随机化, 除了位置和 ID
            // 1 = 添加 NULL 填充
            // 2 = 随机化未使用的字节
            final String[] levelOptions = {"级别", "0", "1", "2"};

            ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, levelOptions) {
                @Override
                public boolean isEnabled(int position) {
                    // 禁用“级别”项
                    return position != 0;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View view = super.getDropDownView(position, convertView, parent);
                    TextView tv = (TextView) view;
                    if (position == 0) {
                        tv.setTextColor(Color.GRAY);  // 提示文本颜色
                    } else {
                        tv.setTextColor(Color.WHITE); // 普通文本
                    }
                    return view;
                }
            };

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            levelList.setAdapter(adapter);
            levelList.setSelection(0);  // 初始选择“级别”

            levelList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                    if (pos != 0) { // 忽略“模式”提示
                        String level_selected = parentView.getItemAtPosition(pos).toString();
                        sharedpreferences.edit().putString("level_selected", level_selected).apply();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parentView) {
                }
            });

            // 刷新状态
            ImageButton RefreshUSB = rootView.findViewById(R.id.refreshUSB);
            RefreshUSB.setOnClickListener(v -> {
                showToast("正在刷新设备...");
                refresh(rootView);
            });
            executorService.submit(() -> refresh(rootView));

            // 随机化
            // Button btnRandomize = rootView.findViewById(R.id.btn_toggle_randomize);

            // btnRandomize.setOnClickListener(v -> {
            //     isRandomizeEnabled = !isRandomizeEnabled;

            //     int color = isRandomizeEnabled ? android.R.color.holo_green_light : android.R.color.holo_red_light;
            //     btnRandomize.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            // });

            // 级别
            Button btnLevel = rootView.findViewById(R.id.btn_toggle_level);

            btnLevel.setOnClickListener(v -> {
                boolean visible = levelList.getVisibility() == View.VISIBLE;
                levelList.setVisibility(visible ? View.GONE : View.VISIBLE);

                int color = visible ? android.R.color.holo_red_light : android.R.color.holo_green_light;
                btnLevel.setTextColor(ContextCompat.getColorStateList(requireContext(), color));
            });

            // ICSIM
            Button runICSIM = rootView.findViewById(R.id.run_icsim);
            runICSIM.setOnClickListener(v -> {
                if (!selected_caniface.isEmpty() && !selected_caniface.equals("无")) {
                    // String randomizeEnabled = isRandomizeEnabled ? " -r" : "";
                    String levelValue = getVisibleParam(levelList, " -l ");
                    run_cmd("su -c 'sh " + ICSIM_SCRIPT_PATH + " " + selected_caniface + levelValue + "'");
                    showToast("正在运行 ICSim...");
                    new Handler().postDelayed(() -> {
                        WebView icsimView = rootView.findViewById(R.id.icsim);
                        WebView controlsView = rootView.findViewById(R.id.controls);

                        for (WebView view : new WebView[]{icsimView, controlsView}) {
                            WebSettings settings = view.getSettings();
                            settings.setJavaScriptEnabled(true);
                            settings.setDomStorageEnabled(true);
                            settings.setLoadWithOverviewMode(true);
                            settings.setUseWideViewPort(true);
                            settings.setBuiltInZoomControls(true);
                            settings.setDisplayZoomControls(false);
                            view.setWebViewClient(new WebViewClient());
                        }

                        icsimView.loadUrl("http://localhost:6080/vnc.html?autoconnect=true&resize=scale");
                        controlsView.loadUrl("http://localhost:6081/vnc.html?autoconnect=true&resize=scale");

                    }, SHORT_DELAY + LONG_DELAY);
                } else {
                    showToast("请设置一个 CAN 接口！");
                }
            });

            Button stopICSIM = rootView.findViewById(R.id.stop_icsim);
            stopICSIM.setOnClickListener(v -> {
                WebView icsimView = rootView.findViewById(R.id.icsim);
                WebView controlsView = rootView.findViewById(R.id.controls);

                run_cmd("su -c 'sh " + ICSIM_SCRIPT_PATH + " stop'");
                showToast("正在停止 ICSim...");
                icsimView.setBackgroundColor(Color.BLACK);
                icsimView.loadUrl("about:blank");
                controlsView.setBackgroundColor(Color.BLACK);
                controlsView.loadUrl("about:blank");
            });

            Button refreshButton = rootView.findViewById(R.id.refresh_icsim);
            refreshButton.setOnClickListener(v -> {
                showToast("正在刷新 ICSim 显示...");
                WebView icsimView = rootView.findViewById(R.id.icsim);
                WebView controlsView = rootView.findViewById(R.id.controls);

                icsimView.reload();
                controlsView.reload();
            });

            return rootView;
        }

        private String getVisibleParam(View view, String prefix) {
            if (view.getVisibility() == View.VISIBLE) {
                if (view instanceof EditText) {
                    String input = ((EditText) view).getText().toString().trim();
                    if (!input.isEmpty()) {
                        return prefix + input;
                    }
                } else if (view instanceof Spinner) {
                    String selected = ((Spinner) view).getSelectedItem().toString().trim();
                    if (!selected.isEmpty()) {
                        return prefix + selected;
                    }
                }
            }
            return "";
        }

        // 刷新接口
        private void refresh(View CANFragment) {
            final Spinner deviceList = CANFragment.findViewById(R.id.device_interface);
            if (context == null) return;

            executorService.submit(() -> {
                String outputDevice = exe.RunAsChrootOutput("ifconfig | awk '/^[a-zA-Z0-9]/ {print $1}' | sed 's/://' | grep -E '^(can|vcan|slcan)[0-9]+$'");
                final ArrayList<String> deviceIfaces = new ArrayList<>();
                if (outputDevice != null && !outputDevice.isEmpty()) {
                    final String[] deviceifacesArray = outputDevice.split("\n");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        int lastiface = sharedpreferences.getInt("selected_device", 0);
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceifacesArray));
                            deviceList.setSelection(lastiface);
                        });
                        String detected_device = exe.RunAsChrootOutput("dmesg | grep \"now attached to\" | tail -1 | awk '{ $1=$2=$3=$4=\"\"; print substr($0, 5) }'");
                        if (detected_device != null && !detected_device.isEmpty() && !detected_device.matches("^(can|vcan|slcan)\\d+$")) {
                            showToast(detected_device);
                        }
                    }
                } else {
                    deviceIfaces.add("无");
                    Activity activity = getActivity();
                    if (sharedpreferences != null && activity != null) {
                        requireActivity().runOnUiThread(() -> {
                            deviceList.setAdapter(new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, deviceIfaces));
                            sharedpreferences.edit().putInt("selected_device", deviceList.getSelectedItemPosition()).apply();
                        });
                    }
                }
            });

            String message = "设备列表已刷新！";
            showToast(message);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // 简化的 Toast 函数
    public void showToast(String message) {
        Toast.makeText(requireActivity().getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    ////
    // Bridge 侧函数
    ////

    public String run_cmd(String cmd) {
        @SuppressLint("SdCardPath") Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
        intent.putExtra("output", cmd);
        return "命令已执行: " + cmd;
    }
}