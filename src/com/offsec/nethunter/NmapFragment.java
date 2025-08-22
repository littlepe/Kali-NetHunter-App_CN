package com.offsec.nethunter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import com.offsec.nethunter.bridge.Bridge;

import java.util.ArrayList;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

public class NmapFragment extends Fragment {
    private static final String TAG = "NMAPFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    // 构建命令行
    private static final ArrayList<String> CommandComposed = new ArrayList<>();
    // Nmap 开关
    private String net_interface;
    private String time_template;
    private String searchall;
    private String OSdetect;
    private String udpscan;
    private String ipv6check;
    private String technique;
    private String sv;
    private String MySearch;
    private String Ports;
    private String fastmode;
    private String topports;
    private EditText searchBar;
    private EditText portsBar;
    private Context context;

    public NmapFragment() {
    }

    public static NmapFragment newInstance(int sectionNumber) {
        NmapFragment fragment = new NmapFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.nmap, container, false);
        // 默认隐藏高级选项
        final LinearLayout AdvLayout = rootView.findViewById(R.id.nmap_adv_layout);
        AdvLayout.setVisibility(View.GONE);
        SharedPreferences sharedpreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        // 开关: 展开/收起高级选项
        SwitchCompat advswitch = rootView.findViewById(R.id.nmap_adv_switch);
        advswitch.setChecked(false);
        advswitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                Log.d(TAG, "高级选项已打开");
                AdvLayout.setVisibility(View.VISIBLE);
            } else {
                Log.d(TAG, "高级选项已关闭");
                AdvLayout.setVisibility(View.GONE);
            }
        });

        final Button searchButton = rootView.findViewById(R.id.nmap_scan_button);
        searchButton.setOnClickListener(
                view -> getCmd());

        /* NMAP 接口下拉框 */
        Spinner typeSpinner = rootView.findViewById(R.id.nmap_int_spinner);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(context,
                R.array.nmap_interface_array, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpinner.setAdapter(typeAdapter);
        net_interface = "wlan0";
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                switch (pos) {
                    case 0:
                        removeFromCmd(net_interface);
                        break;
                    case 1:
                        removeFromCmd(net_interface);
                        net_interface = " -e wlan0";
                        addToCmd(net_interface);
                        break;
                    case 2:
                        removeFromCmd(net_interface);
                        net_interface = " -e wlan1";
                        addToCmd(net_interface);
                        break;
                    case 3:
                        removeFromCmd(net_interface);
                        net_interface = " -e eth0";
                        addToCmd(net_interface);
                        break;
                    case 4:
                        removeFromCmd(net_interface);
                        net_interface = " -e rndis0";
                        addToCmd(net_interface);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + pos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 接口选择的另一个回调
            }
        });

        /* NMAP 扫描技术下拉框 */
        Spinner techSpinner = rootView.findViewById(R.id.nmap_scan_tech_spinner);
        ArrayAdapter<CharSequence> techAdapter = ArrayAdapter.createFromResource(context,
                R.array.nmap_scantechnique_array, android.R.layout.simple_spinner_item);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        techSpinner.setAdapter(techAdapter);
        techSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                switch (pos) {
                    case 0:
                        removeFromCmd(technique);
                        break;
                    case 1:
                        removeFromCmd(technique);
                        technique = " -sS";
                        addToCmd(technique);
                        break;
                    case 2:
                        removeFromCmd(technique);
                        technique = " -sT";
                        addToCmd(technique);
                        break;
                    case 3:
                        removeFromCmd(technique);
                        technique = " -sA";
                        addToCmd(technique);
                        break;
                    case 4:
                        removeFromCmd(technique);
                        technique = " -sW";
                        addToCmd(technique);
                        break;
                    case 5:
                        removeFromCmd(technique);
                        technique = " -sM";
                        addToCmd(technique);
                        break;
                    case 6:
                        removeFromCmd(technique);
                        technique = " -sN";
                        addToCmd(technique);
                        break;
                    case 7:
                        removeFromCmd(technique);
                        technique = " -sF";
                        addToCmd(technique);
                        break;
                    case 8:
                        removeFromCmd(technique);
                        technique = " -sX";
                        addToCmd(technique);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + pos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 接口选择的另一个回调
            }
        });

        /* 搜索按钮 */
        addClickListener(v -> run_cmd("nmap " + getCmd()), rootView);

        /* NMAP 时间模板下拉框 */
        Spinner timeSpinner = rootView.findViewById(R.id.nmap_timing_spinner);
        ArrayAdapter<CharSequence> timeAdapter = ArrayAdapter.createFromResource(context,
                R.array.nmap_timing_array, android.R.layout.simple_spinner_item);
        timeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        timeSpinner.setAdapter(timeAdapter);
        timeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                switch (pos) {
                    case 0:
                        removeFromCmd(time_template);
                        break;
                    case 1:
                        removeFromCmd(time_template);
                        time_template = " -T 0";
                        addToCmd(time_template);
                        break;
                    case 2:
                        removeFromCmd(time_template);
                        time_template = " -T 1";
                        addToCmd(time_template);
                        break;
                    case 3:
                        removeFromCmd(time_template);
                        time_template = " -T 2";
                        addToCmd(time_template);
                        break;
                    case 4:
                        removeFromCmd(time_template);
                        time_template = " -T 3";
                        addToCmd(time_template);
                        break;
                    case 5:
                        removeFromCmd(time_template);
                        time_template = " -T 4";
                        addToCmd(time_template);
                        break;
                    case 6:
                        removeFromCmd(time_template);
                        time_template = " -T 5";
                        addToCmd(time_template);
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + pos);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 接口选择的另一个回调
            }
        });

        /* 全功能扫描复选框 */
        final CheckBox allCheckbox = rootView.findViewById(R.id.nmap_all_check);
        allCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isChecked()) {
                searchall = " -A";
                addToCmd(searchall);
                Log.d(TAG, searchall);
            } else {
                removeFromCmd(searchall);
                Log.d(TAG, searchall);
            }

        });

        /* 快速模式复选框 */
        final CheckBox fastmodeCheckbox = rootView.findViewById(R.id.nmap_fastmode_check);
        View.OnClickListener checkBoxListener = v -> {
            if (fastmodeCheckbox.isChecked()) {
                fastmode = " -F";
                addToCmd(fastmode);
            } else {
                removeFromCmd(fastmode);
            }
        };
        fastmodeCheckbox.setOnClickListener(checkBoxListener);

        /* 仅 Ping 扫描复选框 */
        final CheckBox pingCheckbox = rootView.findViewById(R.id.nmap_ping_check);
        checkBoxListener = v -> {
            if (pingCheckbox.isChecked()) {
                fastmode = " -sn";
                addToCmd(fastmode);
            } else {
                removeFromCmd(fastmode);
            }
        };
        pingCheckbox.setOnClickListener(checkBoxListener);

        /* 常用端口复选框 */
        final CheckBox topportsCheckbox = rootView.findViewById(R.id.nmap_top_ports_check);
        checkBoxListener = v -> {
            if (topportsCheckbox.isChecked()) {
                topports = " --top-ports 20";
                addToCmd(topports);
            } else {
                removeFromCmd(topports);
            }
        };
        topportsCheckbox.setOnClickListener(checkBoxListener);

        /* UDP 扫描复选框 */
        final CheckBox udpCheckbox = rootView.findViewById(R.id.nmap_udp_checkbox);
        checkBoxListener = v -> {
            if (udpCheckbox.isChecked()) {
                udpscan = " -sU";
                addToCmd(udpscan);
            } else {
                removeFromCmd(udpscan);
            }
        };
        allCheckbox.setOnClickListener(checkBoxListener);

        /* IPv6 扫描复选框 */
        final CheckBox ipv6box = rootView.findViewById(R.id.nmap_ipv6_check);
        checkBoxListener = v -> {
            if (ipv6box.isChecked()) {
                ipv6check = " -6";
                addToCmd(ipv6check);
            } else {
                removeFromCmd(ipv6check);
            }
        };
        ipv6box.setOnClickListener(checkBoxListener);

        /* 服务版本检测复选框 */
        final CheckBox svbox = rootView.findViewById(R.id.nmap_SV_checkbox);
        checkBoxListener = v -> {
            if (svbox.isChecked()) {
                sv = " -sV";
                addToCmd(sv);
            } else {
                removeFromCmd(sv);
            }
        };
        svbox.setOnClickListener(checkBoxListener);

        /* 操作系统检测复选框 */
        final CheckBox osdetectbox = rootView.findViewById(R.id.nmap_osonly_check);
        checkBoxListener = v -> {
            if (osdetectbox.isChecked()) {
                OSdetect = " -O";
                addToCmd(OSdetect);
            } else {
                removeFromCmd(OSdetect);
            }
        };
        osdetectbox.setOnClickListener(checkBoxListener);

        /* 搜索字段 */
        searchBar = rootView.findViewById(R.id.nmap_searchbar);
        searchBar.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                removeFromCmd(" " + MySearch);
                MySearch = searchBar.getText().toString();
                addToCmd(" " + MySearch);
            }
        });

        /* 端口文本框 */
        portsBar = rootView.findViewById(R.id.nmap_ports);
        portsBar.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                removeFromCmd(" -p" + Ports);
                Ports = portsBar.getText().toString();
                addToCmd(" -p" + Ports);
            }
        });

        return rootView;
    }

    private String getCmd() {
        StringBuilder genCmd = new StringBuilder();
        for (int j = CommandComposed.size() - 1; j >= 0; j--) {
            genCmd.append(CommandComposed.get(j));
        }
        //Log.d("NMAP SQL:", "nmap --script sqlite-output --script-args=dbname=/tmp/scan.sqlite,dbtable=scandata " + genCmd);
        Log.d("NMAP 命令输出: ", "nmap " + genCmd);

        return genCmd.toString();
    }

    /* 清空命令列表 */
    private static void cleanCmd() {
        if (!CommandComposed.isEmpty()) {
            CommandComposed.subList(0, CommandComposed.size()).clear();
        }
    }

    /* 添加参数到命令列表 */
    private static void addToCmd(String opt) {
        CommandComposed.add(opt);
    }

    /* 从命令列表移除参数 */
    private static void removeFromCmd(String opt) {
        for (int j = CommandComposed.size() - 1; j >= 0; j--) {
            if (CommandComposed.get(j).equals(opt))
                CommandComposed.remove(j);
        }
    }

    /* 为按钮添加点击监听 */
    private void addClickListener(View.OnClickListener onClickListener, View rootView) {
        rootView.findViewById(R.id.nmap_scan_button).setOnClickListener(onClickListener);
    }

    /* Bridge 端函数 */

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        requireContext().startActivity(intent);
    }
}