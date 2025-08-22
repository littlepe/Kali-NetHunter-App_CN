package com.offsec.nethunter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.BuildConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class DuckHunterConvertFragment extends Fragment implements View.OnClickListener {
    private static final String TAG = "DuckHunterConvert";
    private final String duckyInputFile;
    private static final String loadFilePath = "/scripts/ducky/";
    private static final int PICKFILE_RESULT_CODE = 1;
    private Context context;
    private Activity activity;
    private boolean isReceiverRegistered;
    private EditText editsource;
    private final ConvertDuckyBroadcastReceiver convertDuckyBroadcastReceiver = new ConvertDuckyBroadcastReceiver();

    public DuckHunterConvertFragment(String inFilePath, String outFilePath){
        this.duckyInputFile = inFilePath;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.duck_hunter_convert, container, false);
        TextView t2 = rootView.findViewById(R.id.reference_text);
        t2.setMovementMethod(LinkMovementMethod.getInstance());

        editsource = rootView.findViewById(R.id.editSource);
        editsource.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                activity.sendBroadcast(new Intent().putExtra("ACTION", "SHOULDCONVERT")
                        .putExtra("SHOULDCONVERT", true)
                        .setAction(BuildConfig.APPLICATION_ID + ".SHOULDCONVERT")
                        .setPackage(activity.getPackageName()));
            }
        });

        Button b = rootView.findViewById(R.id.duckyLoad);
        Button b1 = rootView.findViewById(R.id.duckySave);
        b.setOnClickListener(this);
        b1.setOnClickListener(this);

        // Duckhunter 预设脚本模板
        String[] duckyscript_file = getDuckyScriptFiles();
        Spinner duckyscriptSpinner = rootView.findViewById(R.id.duckhunter_preset_spinner);
        ArrayAdapter<String> duckyscriptAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, duckyscript_file);
        duckyscriptAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        duckyscriptSpinner.setAdapter(duckyscriptAdapter);
        duckyscriptSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                getPreset(duckyscriptSpinner.getSelectedItem().toString());
                write_ducky();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }
        });

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isReceiverRegistered){
            ContextCompat.registerReceiver(activity, convertDuckyBroadcastReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".WRITEDUCKY"), ContextCompat.RECEIVER_NOT_EXPORTED);
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isReceiverRegistered) {
            activity.unregisterReceiver(convertDuckyBroadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        editsource = null;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICKFILE_RESULT_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                if (getView() == null) {
                    return;
                }
                String FilePath = Objects.requireNonNull(data.getData()).getPath();
                EditText editsource = getView().findViewById(R.id.editSource);
                try {
                    StringBuilder text = new StringBuilder();
                    BufferedReader br = new BufferedReader(new FileReader(FilePath));
                    String line;
                    while ((line = br.readLine()) != null) {
                        text.append(line).append('\n');
                    }
                    br.close();
                    editsource.setText(text.toString());
                    NhPaths.showMessage(context, "脚本已加载");
                } catch (Exception e) {
                    NhPaths.showMessage(context, e.getMessage());
                }
            }
        }
    }

    private void getPreset(String filename) {
        if (getView() == null) {
            return;
        }
        String filename_path = "/duckyscripts/";
        filename = filename_path + filename;
        EditText editsource = getView().findViewById(R.id.editSource);
        File file = new File(NhPaths.APP_SD_FILES_PATH, filename);
        StringBuilder text = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
        } catch (IOException e) {
            Log.e(TAG, "exception", e);
        }
        editsource.setText(text);
    }

    private String[] getDuckyScriptFiles() {
        List<String> result = new ArrayList<>();
        File script_folder = new File(NhPaths.APP_SD_FILES_PATH + "/duckyscripts");
        File[] filesInFolder = script_folder.listFiles();
        assert filesInFolder != null;
        for (File file : filesInFolder) {
            if (!file.isDirectory()) {
                result.add(file.getName());
            }
        }
        Collections.sort(result);
        return result.toArray(new String[0]);
    }

    private void write_ducky() {
        try {
            File myFile = new File(duckyInputFile);
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
            myOutWriter.append(editsource.getText().toString());
            myOutWriter.close();
            fOut.close();
        } catch (Exception e) {
            NhPaths.showMessage(context, e.getMessage());
        }
    }
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.duckyLoad:
                try {
                    File scriptsDir = new File(NhPaths.APP_SD_FILES_PATH, loadFilePath);
                    if (!scriptsDir.exists()) scriptsDir.mkdirs();
                } catch (Exception e) {
                    NhPaths.showMessage(context, e.getMessage());
                }
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                Uri selectedUri = Uri.parse(NhPaths.APP_SD_FILES_PATH + loadFilePath);
                intent.setDataAndType(selectedUri, "file/*");
                startActivityForResult(intent, PICKFILE_RESULT_CODE);
                break;
            case R.id.duckySave:
                try {
                    File scriptsDir = new File(NhPaths.APP_SD_FILES_PATH, loadFilePath);
                    if (!scriptsDir.exists()) scriptsDir.mkdirs();
                } catch (Exception e) {
                    NhPaths.showMessage(context, e.getMessage());
                }
                MaterialAlertDialogBuilder alert = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);

                alert.setTitle("名称");
                alert.setMessage("请输入脚本名称. ");

                // 设置一个 EditText 以获取用户输入
                final EditText input = new EditText(activity);
                alert.setView(input);

                alert.setPositiveButton("确定", (dialog, whichButton) -> {
                    String value = input.getText().toString();
                    if (!value.isEmpty()) {
                        // 保存文件（询问名称）
                        File scriptFile = new File(NhPaths.APP_SD_FILES_PATH + loadFilePath + File.separator + value + ".conf");
                        System.out.println(scriptFile.getAbsolutePath());
                        if (!scriptFile.exists()) {
                            try {
                                if (getView() != null) {
                                    EditText source = getView().findViewById(R.id.editSource);
                                    String text = source.getText().toString();
                                    scriptFile.createNewFile();
                                    FileOutputStream fOut = new FileOutputStream(scriptFile);
                                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                                    myOutWriter.append(text);
                                    myOutWriter.close();
                                    fOut.close();
                                    NhPaths.showMessage(context, "脚本已保存");
                                }
                            } catch (Exception e) {
                                NhPaths.showMessage(context, e.getMessage());
                            }
                        } else {
                            NhPaths.showMessage(context, "文件已存在");
                        }
                    } else {
                        NhPaths.showMessage(context, "名称无效");
                    }
                });

                alert.setNegativeButton("取消", (dialog, whichButton) -> {
                    ///Do nothing
                });
                alert.show();
                break;
            default:
                NhPaths.showMessage(context, "未知点击");
                break;
        }
    }

    public class ConvertDuckyBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getStringExtra("ACTION"), "WRITEDUCKY")) {
                write_ducky();
                activity.sendBroadcast(new Intent()
                        .putExtra("ACTION", "PREVIEWDUCKY")
                        .setAction(BuildConfig.APPLICATION_ID + ".PREVIEWDUCKY")
                        .setPackage(context.getPackageName()));
            }
        }
    }
}