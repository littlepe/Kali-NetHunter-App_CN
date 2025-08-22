package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.offsec.nethunter.utils.ShellExecuter;

import java.io.BufferedReader;
import java.net.NetworkInterface;
import java.security.SecureRandom;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MacchangerFragment extends Fragment {
    private static final String TAG = "MacchangerFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static int lastSelectedIfacePosition = 0;
    private Spinner interfaceSpinner;
    private Spinner macModeSpinner;
    private Button changeMacButton;
    private Button setHostNameButton;
    private Button resetMacButton;
    private Button regenerateMacButton;
    private Button clearMacButton;
    private EditText netHostNameEditText;
    private EditText mac1;
    private EditText mac2;
    private EditText mac3;
    private EditText mac4;
    private EditText mac5;
    private EditText mac6;
    private TextView currentMacTextView;
    private ImageButton reloadImageButton;
    private static final HashMap<String, String> iFaceAndMacHashMap = new HashMap<>();
    private Context context;
    private Activity activity;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static MacchangerFragment newInstance(int sectionNumber) {
        MacchangerFragment fragment = new MacchangerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);
        context = getContext();
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.macchanger, container, false);
        interfaceSpinner = rootView.findViewById(R.id.f_macchanger_interface_opts_spr);
        macModeSpinner = rootView.findViewById(R.id.f_macchanger_mode_opts_spr);
        changeMacButton = rootView.findViewById(R.id.f_macchanger_set_mac_btn);
        setHostNameButton = rootView.findViewById(R.id.f_macchanger_setHostname_btn);
        resetMacButton = rootView.findViewById(R.id.f_macchanger_reset_mac_btn);
        netHostNameEditText = rootView.findViewById(R.id.f_macchanger_phone_name_et);
        currentMacTextView = rootView.findViewById(R.id.f_macchanger_currMac_tv);
        reloadImageButton = rootView.findViewById(R.id.f_macchanger_reloadMAC_imgbtn);
        regenerateMacButton = rootView.findViewById(R.id.f_macchanger_regenerate_mac_btn);
        clearMacButton = rootView.findViewById(R.id.f_macchanger_clear_mac_btn);
        mac1 = rootView.findViewById(R.id.f_macchanger_mac1_et);
        mac2 = rootView.findViewById(R.id.f_macchanger_mac2_et);
        mac3 = rootView.findViewById(R.id.f_macchanger_mac3_et);
        mac4 = rootView.findViewById(R.id.f_macchanger_mac4_et);
        mac5 = rootView.findViewById(R.id.f_macchanger_mac5_et);
        mac6 = rootView.findViewById(R.id.f_macchanger_mac6_et);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getIfaceAndMacAddr();
        setupInterfaceSpinner();
        setHostNameEditText();
        setSetHostnameButton();
        setMacModeSpinner();
        setReloadImageButton();
        setChangeMacButton();
        setResetMacButton();
        setRegenerateMacButton();
        setClearMacButton();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.macchanger, menu);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executorService.shutdown();
        interfaceSpinner = null;
        macModeSpinner = null;
        changeMacButton = null;
        setHostNameButton = null;
        resetMacButton = null;
        regenerateMacButton = null;
        clearMacButton = null;
        netHostNameEditText = null;
        mac1 = null;
        mac2 = null;
        mac3 = null;
        mac4 = null;
        mac5 = null;
        mac6 = null;
        currentMacTextView = null;
        reloadImageButton = null;
    }

    private void genRandomMACAddress() {
        SecureRandom random = new SecureRandom();
        byte[] macAddr = new byte[6];
        random.nextBytes(macAddr);
        // 第一个字节的次低有效位和最低有效位必须分别为 1 和 0
        mac1.setText(String.format("%02x", ((macAddr[0] & 0xfc) | 0x2)));
        mac2.setText(String.format("%02x", macAddr[1]));
        mac3.setText(String.format("%02x", macAddr[2]));
        mac4.setText(String.format("%02x", macAddr[3]));
        mac5.setText(String.format("%02x", macAddr[4]));
        mac6.setText(String.format("%02x", macAddr[5]));
    }

    private void setHostNameEditText() {
        executeTask(() -> {
            String result = "unknown";
            try (BufferedReader reader = new BufferedReader(new java.io.FileReader("/proc/sys/kernel/hostname"))) {
                result = reader.readLine();
            } catch (java.io.FileNotFoundException e) {
                Log.e(TAG, "Hostname file not found: " + e.getMessage());
            } catch (java.io.IOException e) {
                Log.e(TAG, "Error reading hostname: " + e.getMessage());
            }
            final String hostname = result;
            mainHandler.post(() -> {
                if (netHostNameEditText != null) {
                    netHostNameEditText.setText(hostname);
                }
            });
        });
    }

    private void setSetHostnameButton() {
        setHostNameButton.setOnClickListener(v -> {
            String newHostName = netHostNameEditText.getText().toString();
            executeTask(() -> {
                new ShellExecuter().RunAsRootOutput("setprop net.hostname " + newHostName);
                mainHandler.post(() -> {
                    showToast("net.hostname 已设置为 " + newHostName);
                    setHostNameEditText();
                });
            });
        });
    }

    private void setupInterfaceSpinner() {
        List<String> keys = new ArrayList<>(iFaceAndMacHashMap.keySet());
        Collections.sort(keys, Collections.reverseOrder());
        String[] iFaceStrings = keys.toArray(new String[0]);
        ArrayAdapter<String> iFaceArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, iFaceStrings);
        iFaceArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        interfaceSpinner.setAdapter(iFaceArrayAdapter);
        interfaceSpinner.setSelection(lastSelectedIfacePosition);
        interfaceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                lastSelectedIfacePosition = position;
                currentMacTextView.setText(iFaceAndMacHashMap.get(interfaceSpinner.getSelectedItem().toString().toLowerCase()));
                changeMacButton.setText(MessageFormat.format("{0} {1}", getString(R.string.changeMAC), interfaceSpinner.getSelectedItem().toString().toUpperCase()));
                resetMacButton.setText(MessageFormat.format("{0} {1}", getString(R.string.resetMAC), interfaceSpinner.getSelectedItem().toString().toUpperCase()));
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setMacModeSpinner() {
        if (macModeSpinner.getSelectedItemPosition() == 0) {
            regenerateMacButton.setVisibility(View.VISIBLE);
            clearMacButton.setVisibility(View.GONE);
        } else {
            regenerateMacButton.setVisibility(View.GONE);
            clearMacButton.setVisibility(View.VISIBLE);
        }
        macModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    regenerateMacButton.setVisibility(View.VISIBLE);
                    clearMacButton.setVisibility(View.GONE);
                    genRandomMACAddress();
                } else if (position == 1) {
                    regenerateMacButton.setVisibility(View.GONE);
                    clearMacButton.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void setRegenerateMacButton() {
        regenerateMacButton.setOnClickListener(v -> genRandomMACAddress());
    }

    private void setClearMacButton() {
        clearMacButton.setOnClickListener(v -> {
            mac1.setText("");
            mac2.setText("");
            mac3.setText("");
            mac4.setText("");
            mac5.setText("");
            mac6.setText("");
        });
    }

    private void setReloadImageButton() {
        reloadImageButton.setOnClickListener(v -> {
            getIfaceAndMacAddr();
            setupInterfaceSpinner();
        });
    }

    private static void getIfaceAndMacAddr() {
        if (!iFaceAndMacHashMap.isEmpty()) iFaceAndMacHashMap.clear();
        try {
            List<NetworkInterface> allIface = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface iface : allIface) {
                byte[] macBytes = iface.getHardwareAddress();
                if (macBytes == null) {
                    continue;
                }
                StringBuilder macaddrStringBuilder = new StringBuilder();
                for (byte b : macBytes) {
                    macaddrStringBuilder.append(String.format("%02X:", b));
                }
                if (macaddrStringBuilder.length() > 0) {
                    macaddrStringBuilder.deleteCharAt(macaddrStringBuilder.length() - 1);
                }
                iFaceAndMacHashMap.put(iface.getName().toLowerCase(), macaddrStringBuilder.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        Log.d("DEBUG", iFaceAndMacHashMap.toString());
    }

    private void setResetMacButton() {
        resetMacButton.setOnClickListener(v -> executeTask(() -> {
            String iface = interfaceSpinner.getSelectedItem().toString().toLowerCase();
            String originalMac = new ShellExecuter().RunAsRootOutput("cat /sys/class/net/" + iface + "/address");
            if (originalMac == null || originalMac.isEmpty()) {
                Log.e(TAG, "获取接口 " + iface + " 的原始 MAC 地址失败");
                mainHandler.post(() -> showToast("获取接口 " + iface + " 的原始 MAC 地址失败"));
                return;
            }
            mainHandler.post(() -> {
                showToast("正在恢复 " + iface + " 的原始 MAC: " + originalMac);
                executeTask(() -> {
                    int result = new ShellExecuter().RunAsRootReturnValue(
                            "ip link set " + iface + " down && " +
                                    "ip link set " + iface + " address " + originalMac + " && " +
                                    "ip link set " + iface + " up"
                    );
                    mainHandler.post(() -> {
                        if (result == 0) {
                            showToast("接口 " + iface + " 的 MAC 已恢复为 " + originalMac);
                        } else {
                            showToast("恢复接口 " + iface + " 的 MAC 失败");
                        }
                        reloadImageButton.performClick();
                    });
                });
            });
        }));
    }

    private void setChangeMacButton() {
        changeMacButton.setOnClickListener(v -> {
            String macAddress = getMacAddress();
            showToast("正在修改 " + interfaceSpinner.getSelectedItem().toString().toLowerCase() + " 的 MAC 地址...");
            executeTask(() -> {
                String iface = interfaceSpinner.getSelectedItem().toString().toLowerCase();
                int result = new ShellExecuter().RunAsRootReturnValue(
                        "ip link set " + iface + " down && " +
                                "ip link set " + iface + " address " + macAddress + " && " +
                                "ip link set " + iface + " up"
                );
                mainHandler.post(() -> {
                    if (result == 0) {
                        showToast("接口 " + iface + " 的 MAC 已更改为 " + macAddress);
                    } else {
                        showToast("更改接口 " + iface + " 的 MAC 失败, 请尝试其他 MAC");
                    }
                    reloadImageButton.performClick();
                });
            });
        });
    }

    // --- 辅助方法 ---
    private String getMacAddress() {
        return mac1.getText().toString().toLowerCase() + ":" +
                mac2.getText().toString().toLowerCase() + ":" +
                mac3.getText().toString().toLowerCase() + ":" +
                mac4.getText().toString().toLowerCase() + ":" +
                mac5.getText().toString().toLowerCase() + ":" +
                mac6.getText().toString().toLowerCase();
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void executeTask(Runnable task) {
        executorService.execute(task);
    }
}