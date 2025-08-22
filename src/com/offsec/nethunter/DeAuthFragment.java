package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.BootKali;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class DeAuthFragment extends Fragment {
    private final ShellExecuter exe = new ShellExecuter();
    private Context context;
    private Activity activity;
    private static final String ARG_SECTION_NUMBER = "section_number";

    public static DeAuthFragment newInstance(int sectionNumber) {
        DeAuthFragment fragment = new DeAuthFragment();
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
        final View rootView = inflater.inflate(R.layout.deauth, container, false);
        SharedPreferences sharedpreferences = context.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        setHasOptionsMenu(true);
        final Button scan = rootView.findViewById(R.id.scan_networks);
        Spinner wlan = (Spinner) rootView.findViewById(R.id.wlan_interface);

        try {
            List<String> wifiAdapters = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isUp() && !networkInterface.isLoopback() && networkInterface.getName().startsWith("wlan")) {
                    wifiAdapters.add(networkInterface.getName());
                }
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, wifiAdapters);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            wlan.setAdapter(adapter);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        final EditText term = rootView.findViewById(R.id.TerminalOutputDeAuth);
        final Button start = rootView.findViewById(R.id.StartDeAuth);
        final CheckBox whitelist = rootView.findViewById(R.id.deauth_whitelist);
        final CheckBox white_me = rootView.findViewById(R.id.deauth_me);
        final EditText channel = rootView.findViewById(R.id.channel);

        channel.setFilters(new InputFilter[]{
                (source, start1, end, dest, dstart, dend) -> {
                    try {
                        int input = Integer.parseInt(dest.toString() + source.toString());
                        if (isInRange(1, 250, input))
                            return null;
                    } catch (NumberFormatException ignored) { }
                    return "";
                }
        });

        whitelist.setChecked(false);
        start.setOnClickListener(v -> {
            String whitelist_command;
            new BootKali("ip link set " + wlan.getSelectedItem() + " up");
            try {
                Thread.sleep(1000);
                new BootKali("airmon-ng start  " + wlan.getSelectedItem()).run_bg();
                Thread.sleep(2000);
                if (whitelist.isChecked()){
                    whitelist_command = "-w /sdcard/nh_files/deauth/whitelist.txt ";
                }
                else{
                    whitelist_command = "";
                }
                run_cmd("echo Press Crtl+C to stop! && mdk3 " + wlan.getSelectedItem() + "mon d " + whitelist_command + "-c " + channel.getText());
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        scan.setOnClickListener(v -> {

            /*TODO: create .sh that executes the commands and puts its output in a file and then read the file in the textview 20/02/17*/
            new BootKali("cp /sdcard/nh_files/deauth/scan.sh /root/scan.sh && chmod +x /root/scan.sh").run_bg();
            String cmd = "./root/scan.sh " + wlan.getSelectedItem().toString() + " | tr -s [:space:] > /sdcard/nh_files/deauth/output.txt";
            try {
                new BootKali("ip link set " + wlan.getSelectedItem().toString() + " up");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            new BootKali(cmd).run_bg();
            try {
                Thread.sleep(5000);
                String output = exe.RunAsRootOutput("cat " + NhPaths.APP_SD_FILES_PATH + "/deauth/output.txt").replace("Channel:", "\n Channel:");
                term.setText(output);
            } catch (Exception e) {
                e.printStackTrace();
                term.setText(e.toString());
            }

        });
        whitelist.setOnClickListener(v -> {
            if (whitelist.isChecked()){
                white_me.setClickable(true);
                String check_me = exe.RunAsRootOutput("grep -q " + getmac(wlan.getSelectedItem().toString()) + " \"" + NhPaths.APP_SD_FILES_PATH + "/deauth/whitelist.txt\" && echo $?");
                white_me.setChecked(check_me.contains("0"));
            }
            else{
                white_me.setChecked(false);
                white_me.setClickable(false);
            }
        });
        white_me.setOnClickListener(v -> {
            if (whitelist.isChecked()) {
                if (white_me.isChecked()) {
                    if (!wlan.getSelectedItem().toString().equals("wlan0")) {
                        exe.RunAsRootOutput("echo '" + getmac("wlan0") + "' >> " + NhPaths.APP_SD_FILES_PATH + "/deauth/whitelist.txt");
                    }
                    exe.RunAsRootOutput("echo '" + getmac(wlan.getSelectedItem().toString()) + "' >> " + NhPaths.APP_SD_FILES_PATH + "/deauth/whitelist.txt");
                } else {
                    if (!wlan.getSelectedItem().toString().equals("wlan0")) {
                        exe.RunAsRootOutput("sed -i '/wlan0/d' /sdcard/nh_files/deauth/whitelist.txt");
                    }
                    exe.RunAsRootOutput("sed -i '/" + getmac(wlan.getSelectedItem().toString()) + "/d' " + NhPaths.APP_SD_FILES_PATH + "/deauth/whitelist.txt");
                }
            }
            else{
                white_me.setChecked(false);
            }
        });
        return rootView;
    }

    private boolean isInRange(int a, int b, int c) {
        return b > a ? c >= a && c <= b : c >= b && c <= a;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.deauth, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.deauth_modify) {
            Intent i = new Intent(activity, DeAuthWhitelistActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public String getmac(final String wlan){
        final String mac;
        mac = exe.RunAsRootOutput("cat /sys/class/net/"+ wlan +  "/address");
        return mac;
    }

    ////
    // Bridge side functions
    ////

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }
}