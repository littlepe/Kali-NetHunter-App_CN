package com.offsec.nethunter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import com.offsec.nethunter.bridge.Bridge;

import java.net.Inet4Address;
import java.net.InetAddress;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MPCFragment extends Fragment {
    private String typeVar;
    private String callbackTypeVar;
    private String payloadVar;
    private String callbackVar;
    private String stagerVar;
    private Context context;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ConnectivityManager connectivityManager;
    private NetworkRequest.Builder builder;

    public MPCFragment() {
    }

    public static MPCFragment newInstance(int sectionNumber) {
        MPCFragment fragment = new MPCFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        assert context != null;
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        builder = new NetworkRequest.Builder();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.payload_maker, container, false);
        SharedPreferences sharedpreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        /* 载荷类型下拉框 */
        Spinner typeSpinner = rootView.findViewById(R.id.mpc_type_spinner);
        ArrayAdapter<CharSequence> typeAdapter = ArrayAdapter.createFromResource(context,
                R.array.mpc_type_array, R.layout.payload_maker_item);
        //typeAdapter.setDropDownViewResource(R.layout.payload_maker_item);
        typeSpinner.setAdapter(typeAdapter);
        // 设置默认值: 在 onItemSelected 触发前使用 spinner 的第一个值
        typeVar = "asp";
        typeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                Log.d("已选择: ", selectedItemText);
                switch (pos) {
                    case 0:
                        typeVar = "asp";
                        break;
                    case 1:
                        typeVar = "aspx";
                        break;
                    case 2:
                        typeVar = "bash";
                        break;
                    case 3:
                        typeVar = "java";
                        break;
                    case 4:
                        typeVar = "linux";
                        break;
                    case 5:
                        typeVar = "osx";
                        break;
                    case 6:
                        typeVar = "perl";
                        break;
                    case 7:
                        typeVar = "php";
                        break;
                    case 8:
                        typeVar = "powershell";
                        break;
                    case 9:
                        typeVar = "python";
                        break;
                    case 10:
                        typeVar = "tomcat";
                        break;
                    case 11:
                        typeVar = "windows";
                        break;
                    case 12:
                        typeVar = "apk";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 另一个接口回调
            }
        });

        /* 载荷下拉框 */
        Spinner payloadSpinner = rootView.findViewById(R.id.mpc_payload_spinner);
        ArrayAdapter<CharSequence> payloadAdapter = ArrayAdapter.createFromResource(context,
                R.array.mpc_payload_array, R.layout.payload_maker_item);
        //typeAdapter.setDropDownViewResource(R.layout.payload_maker_item);
        payloadSpinner.setAdapter(payloadAdapter);
        // 设置默认值: 在 onItemSelected 触发前使用 spinner 的第一个值
        payloadVar = "msf";
        payloadSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                Log.d("已选择: ", selectedItemText);
                if (selectedItemText.equals("MSF")) {
                    payloadVar = "msf";
                } else if (selectedItemText.equals("CMD")) {
                    payloadVar = "cmd";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 另一个接口回调
            }
        });

        /* 回调下拉框 */
        Spinner callbackSpinner = rootView.findViewById(R.id.mpc_callback_spinner);
        ArrayAdapter<CharSequence> callbackAdapter = ArrayAdapter.createFromResource(context,
                R.array.mpc_callback_array, R.layout.payload_maker_item);
        //typeAdapter.setDropDownViewResource(R.layout.payload_maker_item);
        callbackSpinner.setAdapter(callbackAdapter);
        // 设置默认值: 在 onItemSelected 触发前使用 spinner 的第一个值
        callbackVar = "reverse";
        callbackSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                Log.d("已选择: ", selectedItemText);
                if (selectedItemText.equals("Reverse")) {
                    callbackVar = "reverse";
                } else if (selectedItemText.equals("Bind")) {
                    callbackVar = "bind";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 另一个接口回调
            }
        });

        /* 分阶段下拉框 */
        Spinner stageSpinner = rootView.findViewById(R.id.mpc_stage_spinner);
        ArrayAdapter<CharSequence> stagerAdapter = ArrayAdapter.createFromResource(context,
                R.array.mpc_stage_array, R.layout.payload_maker_item);
        //typeAdapter.setDropDownViewResource(R.layout.payload_maker_item);
        stageSpinner.setAdapter(stagerAdapter);
        // 设置默认值: 在 onItemSelected 触发前使用 spinner 的第一个值
        stagerVar = "staged";
        stageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                Log.d("已选择: ", selectedItemText);
                if (selectedItemText.equals("Staged")) {
                    stagerVar = "staged";
                } else if (selectedItemText.equals("Stageless")) {
                    stagerVar = "stageless";
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 另一个接口回调
            }
        });

        /* 回调类型下拉框 */
        Spinner callbackTypeSpinner = rootView.findViewById(R.id.mpc_callbacktype_spinner);
        ArrayAdapter<CharSequence> callbackTypeAdapter = ArrayAdapter.createFromResource(context,
                R.array.mpc_callbacktype_array, R.layout.payload_maker_item);
        //typeAdapter.setDropDownViewResource(R.layout.payload_maker_item);
        callbackTypeSpinner.setAdapter(callbackTypeAdapter);
        // 设置默认值: 在 onItemSelected 触发前使用 spinner 的第一个值
        callbackTypeVar = "tcp";
        callbackTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                String selectedItemText = parent.getItemAtPosition(pos).toString();
                Log.d("已选择: ", selectedItemText);
                // 使用 switch！
                switch (selectedItemText) {
                    case "TCP":
                        callbackTypeVar = "tcp";
                        break;
                    case "HTTP":
                        callbackTypeVar = "http";
                        break;
                    case "HTTPS":
                        callbackTypeVar = "https";
                        break;
                    case "Find Port":
                        callbackTypeVar = "find_port";
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // 另一个接口回调
            }
        });

        /* 端口文本框 */
        EditText port = rootView.findViewById(R.id.mpc_port);
        port.setText(R.string.mpc_port_default);
        // final String PortStr = port.getText().toString();

        /* 获取 IP 地址用于默认 IP 字段 */
        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
                        LinkProperties linkProperties = connectivityManager.getLinkProperties(network);

                        if (networkCapabilities != null && linkProperties != null) {
                            for (LinkAddress linkAddress : linkProperties.getLinkAddresses()) {
                                InetAddress address = linkAddress.getAddress();
                                if (address instanceof Inet4Address) {
                                    String ip = address.getHostAddress();
                                    /* IP 文本框 */
                                    EditText ipaddress = rootView.findViewById(R.id.mpc_ip_address);
                                    ipaddress.setText(ip);
                                }
                            }
                        }
                    }
                }
        );

        Log.d("启动命令值", getCmd(rootView));

        /* 按钮 */
        addClickListener(R.id.mpc_GenerateSDCARD, v -> {
            Log.d("命令", "cd /sdcard/; msfpc " + getCmd(rootView));
            run_cmd("cd /sdcard/; msfpc " + getCmd(rootView)); // 因为是 kali 命令, 可以直接发送
        }, rootView);

        addClickListener(R.id.mpc_GenerateHTTP, v -> {
            Log.d("命令", "cd /var/www/html; msfpc " + getCmd(rootView));
            run_cmd("cd /var/www/html; msfpc " + getCmd(rootView)); // 因为是 kali 命令, 可以直接发送
        }, rootView);

        return rootView;
    }

    private String getCmd(View rootView) {
        EditText ipaddress = rootView.findViewById(R.id.mpc_ip_address);
        EditText port = rootView.findViewById(R.id.mpc_port);
        return typeVar + " " + ipaddress.getText() + " " + port.getText() + " " + payloadVar + " " + callbackVar + " " + " " + stagerVar + " " + callbackTypeVar;
    }

    private void addClickListener(int buttonId, View.OnClickListener onClickListener, View rootView) {
        rootView.findViewById(buttonId).setOnClickListener(onClickListener);
    }

    /* Bridge 端函数 */

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        requireContext().startActivity(intent);
    }
}