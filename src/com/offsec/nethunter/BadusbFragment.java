package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class BadusbFragment extends Fragment {
    private String sourcePath;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private final ShellExecuter exe = new ShellExecuter();
    private Context context;
    private Activity activity;

    public static BadusbFragment newInstance(int sectionNumber) {
        BadusbFragment fragment = new BadusbFragment();
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
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            sourcePath = NhPaths.APP_SD_FILES_PATH + "/configs/startbadusb-lollipop.sh";
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.badusb, container, false);
        loadOptions(rootView);
        final Button button = rootView.findViewById(R.id.updateOptions);
        button.setOnClickListener(v -> updateOptions());
        setHasOptionsMenu(true);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            loadOptions(getView().getRootView());
        }
    }

    private void loadOptions(View rootView) {
        final EditText ifc = rootView.findViewById(R.id.ifc);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            final String text = exe.ReadFile_SYNC(sourcePath);
            ifc.post(() -> {
                String regExpatInterface = "^INTERFACE=(.*)$";
                Pattern pattern = Pattern.compile(regExpatInterface, Pattern.MULTILINE);
                Matcher matcher = pattern.matcher(text);
                if (matcher.find()) {
                    String ifcValue = matcher.group(1);
                    ifc.setText(ifcValue);
                }
            });
        });
        executor.shutdown();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.badusb, menu);
        // WearOS 优化
        final MenuItem sourceItem = menu.findItem(R.id.source_button);
        boolean iswatch = requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        if (iswatch) {
            sourceItem.setVisible(false);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.start_service:
                start();
                return true;
            case R.id.stop_service:
                stop();
                return true;
            case R.id.source_button:
                Intent i = new Intent(activity, EditSourceActivity.class);
                i.putExtra("path", sourcePath);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateOptions() {
        String sourceFile = exe.ReadFile_SYNC(sourcePath);
        EditText ifc = activity.findViewById(R.id.ifc);
        sourceFile = sourceFile.replaceAll("(?m)^INTERFACE=(.*)$", "INTERFACE=" + ifc.getText().toString());
        boolean r = exe.SaveFileContents(sourceFile, sourcePath);// 第一个参数是内容, 第二个参数是文件路径
        if (r) {
            NhPaths.showMessage(context,"选项已更新！");
        } else {
            NhPaths.showMessage(context,"选项未更新！");
        }
    }

    private void start() {
        String[] command = new String[1];
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            command[0] = NhPaths.APP_SCRIPTS_PATH + "/start-badusb-lollipop &> " + NhPaths.APP_SD_FILES_PATH + "/badusb.log &";
        }
        exe.RunAsRoot(command);
        NhPaths.showMessage(context,"BadUSB 攻击已启动！请查看 /sdcard/nh_files/badusb.log");
    }

    private void stop() {
        String[] command = new String[1];
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
            command[0] = NhPaths.APP_SCRIPTS_PATH + "/stop-badusb-lollipop";
        }
        exe.RunAsRoot(command);
        NhPaths.showMessage(context,"BadUSB 攻击已停止！");
    }
}