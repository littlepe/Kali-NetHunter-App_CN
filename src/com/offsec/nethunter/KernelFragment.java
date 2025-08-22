package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
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

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KernelFragment extends Fragment {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    public static final String TAG = "KernelFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Activity activity;
    private final ShellExecuter exe = new ShellExecuter();
    // 内核列表地址
    private static final String KERNEL_URL = "https://gitlab.com/yesimxev/kali-nethunter-kernels/-/raw/main/kernels.txt";

    public static KernelFragment newInstance(int sectionNumber) {
        KernelFragment fragment = new KernelFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.kernel, container, false);
        SharedPreferences sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

        // 设备信息
        final TextView Device = rootView.findViewById(R.id.device);
        final TextView Codename = rootView.findViewById(R.id.codename);
        final TextView Android = rootView.findViewById(R.id.android_ver);
        Device.setText(Build.MODEL);
        Codename.setText(Build.DEVICE);
        Android.setText(Build.VERSION.RELEASE);

        // 自定义代号
        final Spinner repoSpinner = rootView.findViewById(R.id.repo_list);
        final Button codenamesearchButton = rootView.findViewById(R.id.custom_search);
        EditText customCodename = rootView.findViewById(R.id.custom_codename);

        final ArrayList<String> repoKernels = new ArrayList<>();
        repoKernels.add("无");
        // 从官网抓取支持的设备列表
        final String[] codenamesList = exe.RunAsRootOutput("echo 无;curl -s https://nethunter.kali.org/kernels.html | sed -n '/<tr class/{n;p;n;p;}' | sed 's/<[^>]*>//g' | sed 'n;/,/!s/^/- /' | paste - - | awk '!x[$0]++' | tail -n +2").split("\n");
        repoSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, codenamesList));

        repoSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                String selected_device = parentView.getItemAtPosition(pos).toString();
                if (!selected_device.equals("无")) {
                    String[] selected_codename = selected_device.split("- ");
                    customCodename.setText(selected_codename[1]);
                } else customCodename.setText("");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // 无需操作
            }
        });

        codenamesearchButton.setOnClickListener(v -> {
            String CustomCodename = customCodename.getText().toString();
            checkKernel(rootView, CustomCodename);
        });

        // 浏览文件
        final Button kernelbrowseButton = rootView.findViewById(R.id.kernelfilebrowse);
        kernelbrowseButton.setOnClickListener(v -> {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            filePickerLauncher.launch(Intent.createChooser(intent, "选择 zip 文件"));
        });

        // 刷入
        Button flashButton = rootView.findViewById(R.id.flash_kernel);
        EditText kernelPath = rootView.findViewById(R.id.kernelpath);
        flashButton.setOnClickListener(v -> {
            String kernelfilepath = kernelPath.getText().toString();
            run_cmd_android(NhPaths.APP_SCRIPTS_PATH + "/bin/magic-flash " + kernelfilepath + " | awk 'gsub(/ui_print /,\" \") && !/^ $/'; echo '正在退出...';exit");
        });

        return rootView;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        EditText kernelPath = requireActivity().findViewById(R.id.kernelpath);
                        String filePath = Objects.requireNonNull(Objects.requireNonNull(result.getData().getData()).getPath()).replace("/document/primary:", "/sdcard/");
                        kernelPath.setText(filePath);
                    } else {
                        Toast.makeText(requireActivity(), "未选择文件", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void checkKernel(View KernelFragment, String custom) {
        executor.execute(() -> {
            String codename = custom.isEmpty() ? Build.DEVICE : custom.replaceAll("[^a-zA-Z0-9_-]", "");
            String version = Build.VERSION.RELEASE;

            String version_text = getString(version);

            String kernel_zip = exe.RunAsRootOutput("curl -s " + KERNEL_URL + " | grep " + codename + " | grep " + version_text + " || echo NA");
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireActivity().getApplicationContext(), "正在为 " + codename + "（Android " + version + "）查找内核...", Toast.LENGTH_SHORT).show()
            );

            if (!kernel_zip.equals("NA")) {
                requireActivity().runOnUiThread(() -> {
                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
                    builder.setTitle("设备受支持！");
                    builder.setMessage("是否检查并刷入可用内核？");
                    builder.setPositiveButton("确定", (dialog, which) -> {
                        if (kernel_zip.contains("\n")) {
                            final String[] kernelsArray = kernel_zip.split("\n");
                            MaterialAlertDialogBuilder builderInner = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
                            builderInner.setTitle("检测到多个内核, 请选择: ");
                            builderInner.setItems(kernelsArray, (dialog2, which2) -> {
                                run_cmd_android("echo -ne \"\\033]0;正在刷入内核\\007\" && clear;cd /sdcard && curl " + KERNEL_URL + "/" + kernelsArray[which2] + " > " + kernelsArray[which2] + "; " +
                                        NhPaths.APP_SCRIPTS_PATH + "/bin/magic-flash " + kernelsArray[which2] + " | awk 'gsub(/ui_print /,\" \") && !/^ $/'; echo '正在退出...';exit");
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(requireActivity().getApplicationContext(), "正在下载到 /sdcard 并刷入...", Toast.LENGTH_SHORT).show()
                                );
                            });
                            builderInner.show();
                        } else {
                            run_cmd_android("echo -ne \"\\033]0;正在刷入内核\\007\" && clear;cd /sdcard && curl " + KERNEL_URL + "/" + kernel_zip + " > " + kernel_zip + "; " +
                                    NhPaths.APP_SCRIPTS_PATH + "/bin/magic-flash " + kernel_zip + " | awk 'gsub(/ui_print /,\" \") && !/^ $/'; echo '正在退出...'; exit");
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireActivity().getApplicationContext(), "正在下载到 /sdcard 并刷入...", Toast.LENGTH_SHORT).show()
                            );
                        }
                    });
                    builder.setNegativeButton("取消", (dialog, which) -> {});
                    builder.show();
                });
            } else {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireActivity().getApplicationContext(), "未找到当前 Android 版本的代号, 请手动下载内核", Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    @Nullable
    private static String getString(String version) {
        Map<String, String> versionMap = new HashMap<>();
        versionMap.put("4", "kitkat");
        versionMap.put("5", "lollipop");
        versionMap.put("6", "marshmallow");
        versionMap.put("7", "nougat");
        versionMap.put("8", "oreo");
        versionMap.put("9", "pie");
        versionMap.put("10", "ten");
        versionMap.put("11", "eleven");
        versionMap.put("12", "twelve");
        versionMap.put("13", "thirteen");
        versionMap.put("14", "fourteen");
        versionMap.put("15", "fifteen");
        versionMap.put("16", "sixteen");
        versionMap.put("17", "seventeen");
        String version_text = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            version_text = versionMap.getOrDefault(version, "unknown");
        }
        return version_text;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    ////
    // Bridge 端函数
    ////

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }
    public void run_cmd_android(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/android-su", cmd);
        activity.startActivity(intent);
    }
    public static class PreferencesData {
        public static void saveString(Context context, String key, String value) {
            SharedPreferences sharedPrefs = context.getSharedPreferences("default_preferences", Context.MODE_PRIVATE);
            sharedPrefs.edit().putString(key, value).apply();
        }
        public static String getString(Context context, String key, String defaultValue) {
            SharedPreferences sharedPrefs = context.getSharedPreferences("default_preferences", Context.MODE_PRIVATE);
            return sharedPrefs.getString(key, defaultValue);
        }
    }
}