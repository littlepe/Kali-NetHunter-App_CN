package com.offsec.nethunter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Arrays;
import java.util.List;

public class AudioFragment extends Fragment {
    public final static String TAG = "AudioFragment";
    public static final int DEFAULT_INDEX_BUFFER_HEADROOM = 4;
    public static final int DEFAULT_INDEX_TARGET_LATENCY = 6;
    private static final List<Long> VALUES_BUFFER_HEADROOM = Arrays.asList(0L, 15625L, 31250L, 62500L, 125000L, 250000L, 500000L, 1000000L, 2000000L);
    private static final List<Long> VALUES_TARGET_LATENCY = Arrays.asList(0L, 15625L, 31250L, 62500L, 125000L, 250000L, 500000L, 1000000L, 2000000L, 5000000L, 10000000L, -1L);
    private Button playButton;
    private Spinner bufferHeadroomSpinner;
    private Spinner targetLatencySpinner;
    private EditText serverInput;
    private EditText portInput;
    private CheckBox autoStartCheckBox;
    private TextView errorText;
    private ScrollView fullScrollView;
    private Throwable error;
    private boolean isServiceBound = false;
    private AudioPlaybackService boundService;
    private int itemId;

    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            boundService = ((AudioPlaybackService.LocalBinder) service).getService();
            if (boundService != null) {
                // 现在服务已绑定, 更新 UI 并启用播放按钮
                boundService.playState().observe(getViewLifecycleOwner(), playState -> updatePlayState(playState));
                boundService.showNotification();
                updatePrefs(boundService);

                if (boundService.getAutostartPref() && boundService.isStartable()) {
                    play(); // 如果启用了自动启动, 则选择性地开始播放
                }
            }
            isServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName className) {
            boundService = null;
            isServiceBound = false; // 服务断开连接时清除引用
        }
    };

    public Throwable getError() {
        return error;
    }

    // 添加 newInstance 方法
    public static AudioFragment newInstance(int itemId) {
        AudioFragment fragment = new AudioFragment();
        Bundle args = new Bundle();
        args.putInt("ITEM_ID", itemId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateAudioFragment");

        // 检索在 newInstance 中传递的 itemId
        if (getArguments() != null) {
            itemId = getArguments().getInt("ITEM_ID", -1);
        }

        // 根据需要记录或使用 itemId
        Log.d(TAG, "Received itemId: " + itemId);

        // 为此 Fragment 填充布局
        View view = inflater.inflate(R.layout.audio, container, false);

        // 初始化 UI 元素
        fullScrollView = view.findViewById(R.id.fullScrollView);
        serverInput = view.findViewById(R.id.EditTextServer);
        portInput = view.findViewById(R.id.EditTextPort);
        autoStartCheckBox = view.findViewById(R.id.auto_start);
        playButton = view.findViewById(R.id.ButtonPlay);
        errorText = view.findViewById(R.id.errorText);
        bufferHeadroomSpinner = view.findViewById(R.id.bufferHeadroomSpinner);
        targetLatencySpinner = view.findViewById(R.id.targetLatencySpinner);
        TextView moduleInfoLabel = view.findViewById(R.id.moduleInfoLabel);
        TextView builderinfoLabel = view.findViewById(R.id.builderinfoLabel);
        TextView moduleVerLabel = view.findViewById(R.id.buildVersionLabel);

        String builderinfo = getString(R.string.builderinfo);
        builderinfoLabel.setText(MessageFormat.format("维护者: {0}", builderinfo));

        String moduleInfo = getString(R.string.moduleInfo);
        moduleInfoLabel.setText(MessageFormat.format("信息: {0}", moduleInfo));

        String BuildVerInfo = getString(R.string.build_version);
        moduleVerLabel.setText(MessageFormat.format("版本: {0}", BuildVerInfo));

        playButton.setOnClickListener(v -> {
            if (boundService != null) {
                if (boundService.getPlayState().isActive()) {
                    stop();
                } else {
                    play();
                }
            } else {
                // 可选地, 显示消息或处理服务尚未连接的情况
                errorText.setText(R.string.audio_service_not_connected);
            }
        });

        // 使用默认值设置下拉列表
        setupDefaultAudioConfig();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 绑定到 AudioPlaybackService
        Intent intent = new Intent(getActivity(), AudioPlaybackService.class);
        requireActivity().bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        if (isServiceBound) {
            requireActivity().unbindService(mConnection);
            isServiceBound = false;
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 清除视图引用
        autoStartCheckBox = null;
        fullScrollView = null;
        playButton = null;
        portInput = null;
        bufferHeadroomSpinner = null;
        serverInput = null;
        targetLatencySpinner = null;
        errorText = null;

        if (isServiceBound) {
            requireActivity().unbindService(mConnection);
            isServiceBound = false;
        }
        boundService = null;
    }

    private void setupDefaultAudioConfig() {

        serverInput.setText(R.string.audio_serverinput);
        portInput.setText(R.string.audio_portinput);

        // 将缓冲区间隙和目标延迟值格式化为秒
        List<String> formattedBufferHeadroom = formatValuesAsSeconds(VALUES_BUFFER_HEADROOM);
        List<String> formattedTargetLatency = formatValuesAsSeconds(VALUES_TARGET_LATENCY);

        // 使用格式化的字符串值设置适配器
        ArrayAdapter<String> bufferAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, formattedBufferHeadroom);
        bufferHeadroomSpinner.setAdapter(bufferAdapter);

        ArrayAdapter<String> latencyAdapter = new ArrayAdapter<>(requireActivity(), android.R.layout.simple_spinner_dropdown_item, formattedTargetLatency);
        targetLatencySpinner.setAdapter(latencyAdapter);
    }

    // 将值格式化为秒的辅助方法
    private List<String> formatValuesAsSeconds(List<Long> values) {
        List<String> formattedValues = new ArrayList<>();
        for (Long value : values) {
            if (value >= 0) {
                formattedValues.add(String.format(Locale.getDefault(), "%.3fs", value / 1000000.0));
            } else {
                formattedValues.add("默认"); // 或为特殊值（如 -1）使用其他标签
            }
        }
        return formattedValues;
    }

    private void updatePrefs(AudioPlaybackService service) {
        String serverPref = service.getServerPref();
        if (serverPref != null && !serverPref.isEmpty()) {
            serverInput.setText(serverPref);
        }

        int portPref = service.getPortPref();
        if (portPref > 0) {
            portInput.setText(String.valueOf(portPref));
        }
        autoStartCheckBox.setChecked(service.getAutostartPref());

        setUpSpinner(bufferHeadroomSpinner, VALUES_BUFFER_HEADROOM, service.getBufferHeadroom(), DEFAULT_INDEX_BUFFER_HEADROOM);
        setUpSpinner(targetLatencySpinner, VALUES_TARGET_LATENCY, service.getTargetLatency(), DEFAULT_INDEX_TARGET_LATENCY);
    }

    private void setUpSpinner(Spinner spinner, List<Long> values, long value, int defaultIndex) {
        int pos = values.indexOf(value);
        spinner.setSelection(pos >= 0 ? pos : defaultIndex);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (boundService != null) {
                    long headroomUsec = VALUES_BUFFER_HEADROOM.get(bufferHeadroomSpinner.getSelectedItemPosition());
                    long latencyUsec = VALUES_TARGET_LATENCY.get(targetLatencySpinner.getSelectedItemPosition());
                    boundService.setBufferUsec(headroomUsec, latencyUsec);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updatePlayState(@NonNull AudioPlayState playState) {
        playButton.setText(getPlayButtonText(playState));
        playButton.setEnabled(true);

        switch (playState) {
            case STOPPED:
                appendErrorText("已断开连接状态", android.R.color.holo_orange_light);
                appendDashes();
                playButton.setEnabled(true);
                break;
            case STARTING:
                appendErrorText("连接启动中", android.R.color.holo_green_dark);
                playButton.setEnabled(true);
                break;
            case BUFFERING:
                appendErrorText("正在建立连接", android.R.color.holo_orange_light);
                playButton.setEnabled(true);
                break;
            case STARTED:
                appendErrorText("一切正常！请享受！", android.R.color.holo_green_dark);
                appendDashes();
                playButton.setEnabled(true);
                break;
            case STOPPING:
                appendErrorText("连接断开中", android.R.color.holo_red_light);
                playButton.setEnabled(false);
                break;
        }

        if (boundService != null && boundService.getError() != null) {
            appendErrorText("发生错误: " + boundService.getError().getMessage(), android.R.color.holo_red_dark);
            appendDashes();
        }
    }

    private String getPlayButtonText(@NonNull AudioPlayState playState) {
        switch (playState) {
            case STOPPED:
                return getString(R.string.btn_play);
            case STARTING:
                return getString(R.string.btn_starting);
            case BUFFERING:
                return getString(R.string.btn_buffering);
            case STARTED:
                return getString(R.string.btn_stop);
            case STOPPING:
                return getString(R.string.btn_stopping);
            default:
                return getString(R.string.btn_waiting);
        }
    }

    private void appendErrorText(String message, int colorId) {
        SpannableString spannable = new SpannableString(message + "\n");
        spannable.setSpan(new ForegroundColorSpan(getResources().getColor(colorId)), 0, spannable.length(), 0);
        errorText.append(spannable);
    }

    private void appendDashes() {
        SpannableString dashes = new SpannableString("------------------\n");
        dashes.setSpan(new ForegroundColorSpan(getResources().getColor(android.R.color.holo_purple)), 0, dashes.length(), 0);
        errorText.append(dashes);
    }

    public void play() {
        String server = serverInput.getText().toString().trim();
        int port;
        try {
            port = Integer.parseInt(portInput.getText().toString());
        } catch (NumberFormatException e) {
            portInput.setError("端口号无效");
            return;
        }
        // 清除之前的任何错误消息
        portInput.setError(null);

        if (server.isEmpty()) {
            serverInput.setError("服务器不能为空");
            return;
        }

        if (boundService != null) {
            // 记录正在使用的服务器和端口
            Log.d(TAG, "Attempting to play on server: " + server + " port: " + port);

            // 设置首选项并开始播放
            boundService.setPrefs(server, port, autoStartCheckBox.isChecked());
            boundService.play(server, port);
        } else {
            // 处理服务未绑定的情况
            errorText.setText(R.string.audio_service_not_bound);
            Log.e(TAG, "Service not bound when attempting to play.");
        }
    }

    public void stop() {
        if (boundService != null) {
            boundService.stop();
        }
    }

    public ScrollView getFullScrollView() {
        return fullScrollView;
    }

    public void setFullScrollView(ScrollView fullScrollView) {
        this.fullScrollView = fullScrollView;
    }
}