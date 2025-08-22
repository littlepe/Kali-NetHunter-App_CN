package com.offsec.nethunter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.gps.KaliGPSUpdates;
import com.offsec.nethunter.gps.LocationUpdateService;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.File;

public class KaliGpsServiceFragment extends Fragment implements KaliGPSUpdates.Receiver {
    private static final String TAG = "KaliGpsServiceFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private KaliGPSUpdates.Provider gpsProvider = null;
    private TextView gpsTextView;
    private Context context;
    private boolean wantKismet = false;
    private boolean wantHelpView = true;
    private boolean reattachedToRunningService = false;
    private SwitchCompat switch_gps_provider = null;
    private SwitchCompat switch_gpsd = null;
    private String rtlsdr = "";
    private String rtlamr = "";
    private String rtladsb = "";
    private String mousejack = "";

    public KaliGpsServiceFragment() {
    }

    public static KaliGpsServiceFragment newInstance(int sectionNumber) {
        KaliGpsServiceFragment fragment = new KaliGpsServiceFragment();
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
        return inflater.inflate(R.layout.gps, container, false);
    }

    private void setCheckedQuietly(CompoundButton button, boolean state) {
        button.setTag("quiet");
        button.setChecked(state);
        button.setTag(null);
    }

    @SuppressLint("StaticFieldLeak")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        gpsTextView = view.findViewById(R.id.gps_textview);
        TextView gpsHelpView = view.findViewById(R.id.gps_help);
        switch_gps_provider = view.findViewById(R.id.switch_gps_provider);
        switch_gpsd = view.findViewById(R.id.switch_gpsd);
        Button button_launch_app = view.findViewById(R.id.gps_button_launch_app);
        ShellExecuter exe = new ShellExecuter();
        EditText wlan_interface = view.findViewById(R.id.wlan_interface);
        EditText bt_interface = view.findViewById(R.id.bt_interface);
        CheckBox sdrcheckbox = view.findViewById(R.id.rtlsdr);
        CheckBox sdramrcheckbox = view.findViewById(R.id.rtlamr);
        CheckBox sdradsbcheckbox = view.findViewById(R.id.rtladsb);
        CheckBox mousejackcheckbox = view.findViewById(R.id.mousejack);

        // TODO: 使此文本动态化, 以便可以启动其他应用, 而不仅仅是 Kismet
        button_launch_app.setText(R.string.launch_kismet);
        if (!wantHelpView)
            gpsHelpView.setVisibility(View.GONE);
        Log.d(TAG, "reattachedToRunningService: " + reattachedToRunningService);
        if (reattachedToRunningService) {
            // gpsTextView.append("Service already running\n");
            setCheckedQuietly(switch_gps_provider, true);
        }

        // 检查 gpsd 是否正在运行
        check_gpsd();

        switch_gps_provider.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (switch_gps_provider.getTag() != null)
                return;
            Log.d(TAG, "switch_gps_provider clicked: " + isChecked);
            if (isChecked) {
                startGpsProvider();
            } else {
                stopGpsProvider();
            }
        });

        switch_gpsd.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            if (switch_gpsd.getTag() != null)
                return;
            Log.d(TAG, "switch_gpsd clicked: " + isChecked);
            if (isChecked) {
                startChrootGpsd();
            } else {
                stopChrootGpsd();
            }
        });

        button_launch_app.setOnClickListener(view1 -> {
            if (!switch_gps_provider.isChecked()) {
                gpsTextView.append("Android GPS Provider 未运行！\n");
                switch_gps_provider.setChecked(true);
                startGpsProvider();
            }
            if (!switch_gpsd.isChecked()) {
                gpsTextView.append("chroot gpsd 未运行！\n");
                switch_gpsd.setChecked(true);
                startChrootGpsd();
            }
            // WLAN 接口
            String wlaniface = wlan_interface.getText().toString() ;
            if (!wlaniface.isEmpty()) wlaniface = "source=" + wlaniface + "\n";
            else wlaniface = "";

            // BT 接口
            String btiface = bt_interface.getText().toString();
            if (!btiface.isEmpty()) btiface = "source=" + btiface + "\n";
            else btiface = "";

            // SDR 传感器接口
            if (sdrcheckbox.isChecked()) rtlsdr = "source=rtl433-0\n";
            else rtlsdr = "";

            // SDR AMR 接口
            if (sdramrcheckbox.isChecked()) rtlamr = "source=rtlamr-0\n";
            else rtlamr = "";

            // SDR ADSB 接口
            if (sdradsbcheckbox.isChecked()) rtladsb = "source=rtladsb-0\n";
            else rtladsb = "";

            // Mousejack 接口
            if (mousejackcheckbox.isChecked()) mousejack = "source=mousejack:name=nRF,channel_hoprate=100/sec\n";
            else mousejack = "";

            String conf = "log_template=%p/%n\nlog_prefix=/captures/kismet/\ngps=gpsd:host=localhost,port=2947\n" + wlaniface + btiface + rtlsdr + rtlamr + rtladsb + mousejack;

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                exe.RunAsRoot(new String[]{"echo \"" + conf + "\" > " + NhPaths.SD_PATH + "/kismet_site.conf"});
                exe.RunAsRoot(new String[]{"bootkali custom_cmd mv /sdcard/kismet_site.conf /etc/kismet/"});
            });
            executor.shutdown();
            Toast.makeText(requireActivity().getApplicationContext(), "正在启动 Kismet.. Web UI 将在 localhost:2501 上可用", Toast.LENGTH_LONG).show();
            wantKismet = true;
            gpsTextView.append("收到下一个位置后将启动 Kismet. 正在等待...\n");
        });
    }

    private void startGpsProvider() {
        if (gpsProvider != null) {
            gpsTextView.append("正在启动 Android GPS 发布器\n");
            gpsTextView.append("GPS NMEA 消息将发送到 udp://127.0.0.1:" + NhPaths.GPS_PORT + "\n");
            gpsProvider.onLocationUpdatesRequested(KaliGpsServiceFragment.this);
        }
    }

    private void stopGpsProvider() {
        if (gpsProvider != null) {
            gpsTextView.append("正在停止 Android GPS 发布器\n");
            gpsProvider.onStopRequested();
        }
    }

    private void startChrootGpsd() {
        gpsTextView.append("正在 Kali chroot 中启动 gpsd\n");
        // 在线程中执行, 因为它需要一两秒, 会拖慢 UI
        new Thread(() -> {
            ShellExecuter exe = new ShellExecuter();
            String command = "su -c '" + NhPaths.APP_SCRIPTS_PATH + File.separator + "bootkali start_gpsd " + NhPaths.GPS_PORT + "'";
            Log.d(TAG, command);
            String response = exe.RunAsRootOutput(command);
            Log.d(TAG, "Response = " + response);
        }).start();
    }

    private void stopChrootGpsd() {
        gpsTextView.append("正在 Kali chroot 中停止 gpsd\n");
        // 在线程中执行, 因为它需要一两秒, 会拖慢 UI
        new Thread(() -> {
            ShellExecuter exe = new ShellExecuter();
            String command = "su -c '" + NhPaths.APP_SCRIPTS_PATH + File.separator + "stop-gpsd'";
            Log.d(TAG, command);
            exe.RunAsRootOutput(command);
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (LocationUpdateService.isInstanceCreated()) {
            // LocationUpdateService 已经在运行
            setCheckedQuietly(switch_gps_provider, true);
            // 确保它有这个 Fragment 的句柄, 以便可以显示更新
            if (this.gpsProvider != null) {
                reattachedToRunningService = this.gpsProvider.onReceiverReattach(this);
            }
        } else {
            setCheckedQuietly(switch_gps_provider, false);
        }

        // 检查 gpsd 是否正在运行
        check_gpsd();
    }

    private void check_gpsd() {
        ShellExecuter exe = new ShellExecuter();
        String command = "pgrep gpsd";
        Log.d(TAG, "command = " + command);
        String response = exe.RunAsRootOutput(command);
        Log.d(TAG, "response = '" + response + "'");
        setCheckedQuietly(switch_gpsd, !response.isEmpty());
    }

    @Override
    public void onAttach(@NonNull Context context) {
        if (context instanceof KaliGPSUpdates.Provider) {
            this.gpsProvider = (KaliGPSUpdates.Provider) context;
            reattachedToRunningService = this.gpsProvider.onReceiverReattach(this);
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // 我们已经授予了权限, 让提示信息消失
            wantHelpView = false;
        }
        super.onAttach(context);
    }

    @Override
    public void onPositionUpdate(String nmeaSentences) {
        CharSequence charSequence = gpsTextView.getText();
        int lineCnt = 0;
        int i;
        for (i = charSequence.length() - 1; i >= 0; i--) {
            if (charSequence.charAt(i) == '\n')
                lineCnt++;
            if (lineCnt >= 20)
                break;
        }
        // 删除之前的 X 行以上的内容, 以免变得太大
        if (i > 0) {
            gpsTextView.getEditableText().delete(0, i);
        }

        gpsTextView.append(nmeaSentences + "\n");
        if (wantKismet) {
            wantKismet = false;
            gpsTextView.append("在 NetHunter 终端中启动 Kismet\n");
            startKismet();
        }
    }

    @Override
    public void onFirstPositionUpdate() {
    }

    private void startKismet() {
        try {
            run_cmd("/usr/bin/start-kismet");
        } catch (Exception e) {
            NhPaths.showMessage(context, getString(R.string.toast_install_terminal));
        }
    }

    public void run_cmd(String cmd) {
        if (context != null) {
            Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
            context.startActivity(intent);
        }
    }
}