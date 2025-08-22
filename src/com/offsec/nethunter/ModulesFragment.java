package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public class ModulesFragment extends Fragment {
    public static final String TAG = "ModulesFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private Activity activity;
    private final ShellExecuter exe = new ShellExecuter();
    // 0: 按字母顺序, 1: 逆序
    private int currentSortOrder = 0;
    public EditText modules_path;

    public static ModulesFragment newInstance(int sectionNumber) {
        ModulesFragment fragment = new ModulesFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    private void showModuleInfo(String moduleName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String modulesPath = modules_path != null ? modules_path.getText().toString() : "";
            String sanitizedModulesPath = modulesPath.replaceAll("[^a-zA-Z0-9/_-]", "");
            String kernelVersion = System.getProperty("os.version");
            String pathWithKernelVersion = sanitizedModulesPath + "/" + kernelVersion;

            // 查找模块完整路径
            String findCommand = "find " + sanitizedModulesPath + " " + pathWithKernelVersion + " -name " + moduleName + ".ko -print -quit";
            String moduleFilePath = exe.RunAsRootOutput(findCommand).trim();

            String info;
            if (moduleFilePath.isEmpty()) {
                info = "未找到模块: " + moduleName;
            } else {
                info = exe.RunAsRootOutput("modinfo " + moduleFilePath);
                if (info == null || info.trim().isEmpty()) {
                    info = "无法获取 " + moduleName + " 的信息";
                }
            }

            Activity currentActivity = getActivity();
            if (currentActivity != null) {
                String finalInfo = info;
                currentActivity.runOnUiThread(() -> new AlertDialog.Builder(currentActivity)
                        .setTitle("模块信息: " + moduleName)
                        .setMessage(finalInfo)
                        .setPositiveButton("确定", null)
                        .show());
            }
        });
    }

    private void showModuleDependencies(String moduleName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String modulesPath = modules_path != null ? modules_path.getText().toString() : "";
            String sanitizedModulesPath = modulesPath.replaceAll("[^a-zA-Z0-9/_-]", "");
            String kernelVersion = System.getProperty("os.version");
            String pathWithKernelVersion = sanitizedModulesPath + "/" + kernelVersion;

            // 查找模块完整路径
            String findCommand = "find " + sanitizedModulesPath + " " + pathWithKernelVersion + " -name " + moduleName + ".ko -print -quit";
            String moduleFilePath = exe.RunAsRootOutput(findCommand).trim();

            String dependencies;
            if (moduleFilePath.isEmpty()) {
                dependencies = "未找到模块: " + moduleName;
            } else {
                dependencies = exe.RunAsRootOutput("modinfo " + moduleFilePath + " | grep depends");
                if (dependencies == null || dependencies.trim().isEmpty()) {
                    dependencies = "未找到 " + moduleName + " 的依赖";
                }
            }

            Activity currentActivity = getActivity();
            if (currentActivity != null) {
                String finalDependencies = dependencies;
                currentActivity.runOnUiThread(() -> new AlertDialog.Builder(currentActivity)
                        .setTitle("模块依赖: " + moduleName)
                        .setMessage(finalDependencies)
                        .setPositiveButton("确定", null)
                        .show());
            }
        });
    }

    private void showLoadedModules(View rootView) {
        final ListView modules = rootView.findViewById(R.id.modulesList);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            String loadedModulesRaw = exe.RunAsRootOutput("lsmod | cut -d' ' -f1");
            String[] loadedModules = loadedModulesRaw.split("\n");
            // 移除标题行（如果存在）
            if (loadedModules.length > 0 && loadedModules[0].trim().equals("Module")) {
                loadedModules = Arrays.copyOfRange(loadedModules, 1, loadedModules.length);
            }
            // 构建 moduleStates: 所有已加载模块标记为 true
            Map<String, Boolean> moduleStates = new HashMap<>();
            for (String module : loadedModules) {
                if (!module.trim().isEmpty()) {
                    moduleStates.put(module, true);
                }
            }
            List<String> moduleList = new ArrayList<>(Arrays.asList(loadedModules));
            // 移除空条目
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                moduleList.removeIf(String::isEmpty);
            }

            Activity currentActivity = getActivity();
            if (currentActivity != null) {
                currentActivity.runOnUiThread(() -> {
                    if (moduleList.isEmpty()) {
                        modules.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, Collections.singletonList("无已加载模块")));
                    } else {
                        modules.setAdapter(new ModuleListAdapter(requireContext(), moduleList, moduleStates));
                    }
                });
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activity = getActivity();
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.modules_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();

        ListView modules = requireView().findViewById(R.id.modulesList);
        assert searchView != null;
        searchView.setQueryHint("搜索模块");
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                if (modules.getAdapter() instanceof ArrayAdapter) {
                    ((ArrayAdapter<?>) modules.getAdapter()).getFilter().filter(newText);
                }
                return true;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.modules, container, false);

        ListView modules = rootView.findViewById(R.id.modulesList);

        // lsmod 按钮
        Button lsmodButton = rootView.findViewById(R.id.lsmod);
        lsmodButton.setOnClickListener(view -> showLoadedModules(rootView));

        // 使用上次路径
        modules_path = rootView.findViewById(R.id.modulesPath);
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        String LastModulesPath = sharedPreferences.getString("last_modulespath", "");
        if (!LastModulesPath.isEmpty()) modules_path.setText(LastModulesPath);

        modules.setOnItemLongClickListener((adapterView, view, position, id) -> {
            String selectedModule = modules.getItemAtPosition(position).toString();
            PopupMenu popup = new PopupMenu(requireContext(), view);
            popup.getMenu().add("显示模块信息");
            popup.getMenu().add("查看依赖");
            popup.setOnMenuItemClickListener(item -> {
                if (Objects.equals(item.getTitle(), "显示模块信息")) {
                    showModuleInfo(selectedModule);
                    return true;
                } else if (Objects.equals(item.getTitle(), "查看依赖")) {
                    showModuleDependencies(selectedModule);
                    return true;
                }
                return false;
            });
            popup.show();
            return true;
        });

        // 刷新模块
        Button refreshButton = rootView.findViewById(R.id.refresh);
        refreshButton.setOnClickListener(view -> refreshModules(rootView));
        refreshModules(rootView);

        // 模块开关
        modules.setOnItemClickListener((adapterView, view, i, l) -> {
            String modulesPath = modules_path != null ? modules_path.getText().toString() : "";
            String sanitizedModulesPath = modulesPath.replaceAll("[^a-zA-Z0-9/_-]", "");
            String kernelVersion = System.getProperty("os.version");
            String pathWithKernelVersion = sanitizedModulesPath + "/" + kernelVersion;

            String selectedModule = modules.getItemAtPosition(i).toString();
            String isModuleLoaded = exe.RunAsRootOutput("lsmod | cut -d' ' -f1 | grep " + selectedModule);
            ImageView statusIcon = view.findViewById(R.id.moduleStatusIcon);

            if (isModuleLoaded != null && isModuleLoaded.trim().equals(selectedModule)) {
                String disableModule = exe.RunAsRootOutput("rmmod " + selectedModule + " && echo Success || echo Failed");
                if (disableModule.contains("Success")) {
                    Log.d(TAG, "模块已禁用: " + selectedModule);
                    Toast.makeText(requireActivity().getApplicationContext(), "模块已禁用: " + selectedModule, Toast.LENGTH_LONG).show();
                    if (statusIcon != null) {
                        statusIcon.setImageResource(R.drawable.ic_module_not_loaded);
                    }
                } else {
                    Toast.makeText(requireActivity().getApplicationContext(), "失败 - rmmod " + selectedModule, Toast.LENGTH_LONG).show();
                }
            } else {
                String findCommand = "find " + sanitizedModulesPath + " " + pathWithKernelVersion + " -name " + selectedModule + ".ko -print -quit";
                String foundModulePath = exe.RunAsRootOutput(findCommand);

                if (foundModulePath == null || foundModulePath.trim().isEmpty()) {
                    Toast.makeText(requireActivity().getApplicationContext(), "在目录结构中未找到模块", Toast.LENGTH_LONG).show();
                    return;
                }
                String modulePath = foundModulePath.trim();

                String toggleModule = exe.RunAsRootOutput("insmod " + modulePath + " && echo Success || echo Failed");
                if (toggleModule.contains("Success")) {
                    Log.d(TAG, "模块已启用: " + selectedModule + " 路径: " + modulePath);
                    Toast.makeText(requireActivity().getApplicationContext(), "模块已启用: " + selectedModule + " 路径: " + modulePath, Toast.LENGTH_LONG).show();
                    if (statusIcon != null) {
                        statusIcon.setImageResource(R.drawable.ic_module_loaded);
                    }
                } else {
                    toggleModule = exe.RunAsRootOutput("modprobe -d " + sanitizedModulesPath + " " + selectedModule + " && echo Success || echo Failed");
                    if (toggleModule.contains("Success")) {
                        Log.d(TAG, "模块已启用: " + selectedModule + " 路径: " + sanitizedModulesPath);
                        Toast.makeText(requireActivity().getApplicationContext(), "模块已启用: " + selectedModule + " 路径: " + sanitizedModulesPath, Toast.LENGTH_LONG).show();
                        if (statusIcon != null) {
                            statusIcon.setImageResource(R.drawable.ic_module_loaded);
                        }
                    } else {
                        Toast.makeText(requireActivity().getApplicationContext(), "失败 - modprobe -d " + sanitizedModulesPath + " " + selectedModule, Toast.LENGTH_LONG).show();
                        if (sharedPreferences.getBoolean("enable_faulty_check", true)) {
                            checkFaultyModule(sanitizedModulesPath, selectedModule);
                        }
                    }
                }
            }
        });

        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void refreshModules(View rootView) {
        SharedPreferences sharedpreferences = null;
        if (activity != null) {
            sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        }
        final ListView modules = rootView.findViewById(R.id.modulesList);

        modules_path = rootView.findViewById(R.id.modulesPath);
        String modulesPath = "";
        if (modules_path != null) {
            modulesPath = modules_path.getText().toString();
        }
        AtomicReference<String> sanitizedModulesPath = new AtomicReference<>(modulesPath.replaceAll("[^a-zA-Z0-9/_-]", ""));
        if (sharedpreferences != null) {
            sharedpreferences.edit().putString("last_modulespath", modulesPath).apply();
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // 检查目录是否存在
            String kernelVersion = System.getProperty("os.version");
            String pathWithKernelVersion = sanitizedModulesPath + "/" + kernelVersion;

            String pathCheck = exe.RunAsRootOutput("test -d " + pathWithKernelVersion + " && echo exists || echo not_exists");
            final String finalSanitizedModulesPath;
            if ("not_exists".equals(pathCheck.trim())) {
                pathCheck = exe.RunAsRootOutput("test -d " + sanitizedModulesPath + " && echo exists || echo not_exists");
                if ("not_exists".equals(pathCheck.trim())) {
                    Activity currentActivity = getActivity();
                    if (currentActivity != null) {
                        finalSanitizedModulesPath = sanitizedModulesPath.get();
                        currentActivity.runOnUiThread(() ->
                                Toast.makeText(currentActivity.getApplicationContext(), finalSanitizedModulesPath + " 不存在", Toast.LENGTH_SHORT).show()
                        );
                    }
                    return;
                }
            } else {
                sanitizedModulesPath.set(pathWithKernelVersion);
            }
            finalSanitizedModulesPath = sanitizedModulesPath.get();

            // 执行一次 `find` 命令
            String modulesRaw = exe.RunAsRootOutput("find " + finalSanitizedModulesPath + " -name *.ko -printf \"%f\\n\" | sed 's/\\.ko$//1'");
            if (modulesRaw.isEmpty()) {
                Activity currentActivity = getActivity();
                if (currentActivity != null) {
                    currentActivity.runOnUiThread(() ->
                            modules.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, Collections.singletonList("未找到模块")))
                    );
                }
                return;
            }
            final String[] modulesArray = modulesRaw.split("\n");

            // 执行一次 `lsmod` 并缓存结果
            String loadedModulesRaw = exe.RunAsRootOutput("lsmod | cut -d' ' -f1");
            List<String> loadedModules = Arrays.asList(loadedModulesRaw.split("\n"));

            // 准备模块状态
            Map<String, Boolean> moduleStates = new HashMap<>();
            for (String module : modulesArray) {
                moduleStates.put(module, loadedModules.contains(module));
            }

            // 根据当前排序方式排序模块列表
            List<String> moduleList = new ArrayList<>(Arrays.asList(modulesArray));
            if (currentSortOrder == 0) {
                Collections.sort(moduleList);
            } else if (currentSortOrder == 1) {
                Collections.sort(moduleList, Collections.reverseOrder());
            }

            Activity currentActivity = getActivity();
            if (currentActivity != null) {
                currentActivity.runOnUiThread(() -> {
                    ModuleListAdapter adapter = new ModuleListAdapter(requireContext(), moduleList, moduleStates);
                    modules.setAdapter(adapter);
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        switch (item.getItemId()) {
            case R.id.action_sort:
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("排序模块")
                        .setItems(new String[]{"按字母顺序", "逆序"}, (dialog, which) -> {
                            currentSortOrder = which;
                            refreshModules(requireView());
                        })
                        .show();
                return true;
            case R.id.action_enable_faulty_check:
                boolean isChecked = !item.isChecked();
                item.setChecked(isChecked);
                sharedPreferences.edit().putBoolean("enable_faulty_check", isChecked).apply();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void checkFaultyModule(String modulePath, String moduleName) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            // 获取内核日志
            String kernelLogs = exe.RunAsRootOutput("dmesg | tail -n 20");
            Activity currentActivity = getActivity();
            if (currentActivity != null) {
                String finalKernelLogs = kernelLogs.trim().isEmpty() ? "无可用内核日志" : kernelLogs;
                currentActivity.runOnUiThread(() -> {
                    // 显示 Toast 提示错误
                    Toast.makeText(currentActivity.getApplicationContext(), "加载模块失败: " + moduleName, Toast.LENGTH_LONG).show();

                    // 显示包含详细日志的 AlertDialog
                    new AlertDialog.Builder(currentActivity)
                            .setTitle("模块加载失败: " + moduleName)
                            .setMessage("内核日志: \n" + finalKernelLogs)
                            .setPositiveButton("确定", null)
                            .show();
                });
            }
        });
    }

    static class ModuleListAdapter extends ArrayAdapter<String> {
        private final List<String> modules;
        private final Context context;
        private final Map<String, Boolean> moduleStates;

        public ModuleListAdapter(Context context, List<String> modules, Map<String, Boolean> moduleStates) {
            super(context, R.layout.module_list_item, modules);
            this.context = context;
            this.modules = modules;
            this.moduleStates = moduleStates;
        }

        private static class ViewHolder {
            TextView textView;
            ImageView statusIcon;
            CheckBox autoLoadCheckBox;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(context);
                convertView = inflater.inflate(R.layout.module_list_item, parent, false);
                holder = new ViewHolder();
                holder.textView = convertView.findViewById(R.id.moduleName);
                holder.statusIcon = convertView.findViewById(R.id.moduleStatusIcon);
                holder.autoLoadCheckBox = convertView.findViewById(R.id.moduleAutoLoad);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            String moduleName = modules.get(position);
            holder.textView.setText(moduleName);

            // 根据模块状态设置图标
            Boolean isLoaded = moduleStates.get(moduleName);
            if (isLoaded != null && isLoaded) {
                holder.statusIcon.setImageResource(R.drawable.ic_module_loaded);
            } else {
                holder.statusIcon.setImageResource(R.drawable.ic_module_not_loaded);
            }

            // 处理自动加载复选框
            SharedPreferences preferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
            holder.autoLoadCheckBox.setChecked(preferences.getBoolean("autoload_" + moduleName, false));
            holder.autoLoadCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                preferences.edit().putBoolean("autoload_" + moduleName, isChecked).apply();
            });

            return convertView;
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences preferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
            String defaultPath = "/system/lib/modules";
            String modulesPath = preferences.getString("last_modulespath", defaultPath);
            Map<String, ?> allEntries = preferences.getAll();
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                if (entry.getKey().startsWith("autoload_") && Boolean.TRUE.equals(entry.getValue())) {
                    String moduleName = entry.getKey().replace("autoload_", "");
                    String modulePath = modulesPath + "/" + moduleName + ".ko";
                    ShellExecuter exe = new ShellExecuter();
                    exe.RunAsRootOutput("insmod " + modulePath);
                }
            }
        }
    }

    /* Bridge 端函数 */

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }

    public static class PreferencesData {
        private static final String PREF_NAME = "com.offsec.nethunter_preferences";

        private static SharedPreferences getSharedPreferences(Context context) {
            return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        }

        public static void saveString(Context context, String key, String value) {
            getSharedPreferences(context).edit().putString(key, value).apply();
        }

        public static String getString(Context context, String key, String defaultValue) {
            return getSharedPreferences(context).getString(key, defaultValue);
        }
    }
}