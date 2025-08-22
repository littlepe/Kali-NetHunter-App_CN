package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

public class WifipumpkinFragment extends Fragment {
    private ViewPager2 mViewPager;
    private SharedPreferences sharedpreferences;
    private Integer selectedScriptIndex = 0;
    private final CharSequence[] scripts = {"mana-nat-full", "mana-nat-simple", "mana-nat-bettercap", "mana-nat-simple-bdf", "hostapd-wpe", "hostapd-wpe-karma"};
    private static final String TAG = "WifipumpkinFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private String selected_template;
    private Context context;
    private Activity activity;
    final ShellExecuter exe = new ShellExecuter();
    private String template_src;

    public static WifipumpkinFragment newInstance(int sectionNumber) {
        WifipumpkinFragment fragment = new WifipumpkinFragment();
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
        String configFilePath = NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh";
    }
    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater menuinflater) {
        menuinflater.inflate(R.menu.bt, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.setup:
            case R.id.update:
                RunSetup();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.wifipumpkin_hostapd, container, false);
        final Button StartButton = rootView.findViewById(R.id.wp3start_button);
        SharedPreferences sharedpreferences = context.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        boolean iswatch = requireContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        CheckBox PreviewCheckbox = rootView.findViewById(R.id.preview_checkbox);

        // 首次运行检查
        Boolean setupwp3done = sharedpreferences.getBoolean("set_setup_done", false);
        String packages = exe.RunAsChrootOutput("if [[ -f /usr/bin/wifipumpkin3 || -f /usr/bin/dnschef ]];then echo Good;else echo Nope;fi");

        // if (!setupwp3done.equals(true))
        if (packages.equals("Nope")) SetupDialog();

        // 手表界面优化
        final TextView Wp3desc = rootView.findViewById(R.id.wp3_desc);
        if (iswatch) {
            Wp3desc.setVisibility(View.GONE);
        }

        // 选定的接口、名称、SSID、BSSID、频道、wlan0to1
        final EditText APinterface = rootView.findViewById(R.id.ap_interface);
        final EditText NETinterface = rootView.findViewById(R.id.net_interface);
        final EditText SSID = rootView.findViewById(R.id.ssid);
        final EditText BSSID = rootView.findViewById(R.id.bssid);
        final EditText Channel = rootView.findViewById(R.id.channel);
        final CheckBox Wlan0to1Checkbox = rootView.findViewById(R.id.wlan0to1_checkbox);

        // 模板下拉列表
        refresh_wp3_templates(rootView);
        Spinner TemplatesSpinner = rootView.findViewById(R.id.templates);

        // 选择模板
        WebView myBrowser = rootView.findViewById(R.id.mybrowser);
        final String[] TemplateString = {""};
        TemplatesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int pos, long id) {
                selected_template = parentView.getItemAtPosition(pos).toString();
                if (selected_template.equals("None")) {
                    PreviewCheckbox.setChecked(false);
                    PreviewCheckbox.setEnabled(false);
                    TemplateString[0] = "";
                } else {
                    PreviewCheckbox.setEnabled(true);
                    if (selected_template.equals("FlaskDemo")) {
                        template_src = NhPaths.CHROOT_PATH() + NhPaths.SD_PATH + "/nh_files/templates/" + selected_template + "/templates/En/templates/login.html";
                    } else {
                        template_src = NhPaths.CHROOT_PATH() + NhPaths.SD_PATH + "/nh_files/templates/" + selected_template + "/templates/login.html";
                    }
                    myBrowser.clearCache(true);
                    myBrowser.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
                    myBrowser.getSettings().setDomStorageEnabled(true);
                    myBrowser.getSettings().setLoadsImagesAutomatically(true);
                    //myBrowser.setInitialScale(200);
                    myBrowser.getSettings().setJavaScriptEnabled(true); // 启用 JavaScript 支持
                    myBrowser.setWebViewClient(new WebViewClient());
                    myBrowser.getSettings().setAllowFileAccess(true);
                    myBrowser.loadDataWithBaseURL("file:///sdcard/nh_files/templates/" + selected_template + "/static", template_src, "text/html", "UTF-8", null);
                    myBrowser.loadUrl(template_src);
                    TemplateString[0] = selected_template;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        // 检查 iptables 版本
        checkiptables();

        // 检查 wlan0 AP 模式支持
        TextView APmode = rootView.findViewById(R.id.wlan0ap);
        String Wlan0AP = exe.RunAsRootOutput("iw list | grep '* AP'");
        if (Wlan0AP.contains("* AP")) APmode.setText("支持");
        else APmode.setText("不支持");

        // 刷新
        refresh_wp3_templates(rootView);
        ImageButton RefreshTemplates = rootView.findViewById(R.id.refreshTemplates);
        RefreshTemplates.setOnClickListener(v -> refresh_wp3_templates(rootView));

        // 加载设置
        String PrevAPiface = exe.RunAsRootOutput("grep ^APIFACE= " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh | awk -F'=' '{print $2}'");
        APinterface.setText(PrevAPiface);
        String PrevNETiface = exe.RunAsRootOutput("grep ^NETIFACE= " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh | awk -F'=' '{print $2}'");
        NETinterface.setText(PrevNETiface);
        String PrevSSID = exe.RunAsRootOutput("grep ^SSID= " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh | awk -F'=' '{print $2}' | tr -d '\"'");
        SSID.setText(PrevSSID);
        String PrevBSSID = exe.RunAsRootOutput("grep ^BSSID= " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh | awk -F'=' '{print $2}'");
        BSSID.setText(PrevBSSID);
        String PrevChannel = exe.RunAsRootOutput("grep ^CHANNEL= " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh | awk -F'=' '{print $2}'");
        Channel.setText(PrevChannel);
        String PrevWlan0to1 = exe.RunAsRootOutput("grep ^WLAN0TO1= " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh | awk -F'=' '{print $2}'");
        Wlan0to1Checkbox.setChecked(PrevWlan0to1.equals("1"));

        // Wlan0to1 复选框
        final String[] Wlan0to1_string = {""};

        // 预览复选框
        View PreView = rootView.findViewById(R.id.pre_view);
        PreviewCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                PreView.setVisibility(View.VISIBLE);
            } else {
                PreView.setVisibility(View.GONE);
            }
        });

        // 启动/停止
        StartButton.setOnClickListener( v -> {
            if (StartButton.getText().equals("启动")) {
                String APiface_string = APinterface.getText().toString();
                String NETiface_string = NETinterface.getText().toString();
                String SSID_string = SSID.getText().toString();
                String BSSID_string = BSSID.getText().toString();
                String Channel_string = Channel.getText().toString();
                if (Wlan0to1Checkbox.isChecked()) {
                    Wlan0to1_string[0] = "1";
                } else {
                    Wlan0to1_string[0] = "0";
                }
                Toast.makeText(requireActivity().getApplicationContext(), "正在启动.. 在终端中输入 'exit' 来停止 Wifipumpkin3", Toast.LENGTH_LONG).show();

                exe.RunAsRoot(new String[]{"sed -i '/^APIFACE=/c\\APIFACE=" + APiface_string + "' " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh"});
                exe.RunAsRoot(new String[]{"sed -i '/^NETIFACE=/c\\NETIFACE=" + NETiface_string + "' " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh"});
                exe.RunAsRoot(new String[]{"sed -i '/^SSID=/c\\SSID=\"" + SSID_string + "\"' " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh"});
                exe.RunAsRoot(new String[]{"sed -i '/^BSSID=/c\\BSSID=" + BSSID_string + "' " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh"});
                exe.RunAsRoot(new String[]{"sed -i '/^CHANNEL=/c\\CHANNEL=" + Channel_string + "' " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh"});
                exe.RunAsRoot(new String[]{"sed -i '/^WLAN0TO1=/c\\WLAN0TO1=" + Wlan0to1_string[0] + "' " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh"});
                exe.RunAsRoot(new String[]{"sed -i '/^TEMPLATE=/c\\TEMPLATE=" + TemplateString[0] + "' " + NhPaths.APP_SD_FILES_PATH + "/modules/start-wp3.sh"});
                run_cmd("echo -ne \"\\033]0;Wifipumpkin3\\007\" && clear;bash /sdcard/nh_files/modules/start-wp3.sh");

            } else if (StartButton.getText().equals("停止")) {
                exe.RunAsRoot(new String[]{"kill `ps -ef | grep '[btk]_server' | awk {'print $2'}`"});
                exe.RunAsRoot(new String[]{"pkill python3"});
                refresh_wp3_templates(rootView);
            }
        });

        // 从文件加载模板
        final Button injectStringButton = rootView.findViewById(R.id.templatebrowse);
        injectStringButton.setOnClickListener( v -> {
            Intent intent = new Intent();
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/zip");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "选择 zip 文件"),1001);
        });
        return rootView;
    }

    public void onActivityResult(int requestCode, int resultCode,
                                 Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && (resultCode == Activity.RESULT_OK)) {
            ShellExecuter exe = new ShellExecuter();
            String FilePath = Objects.requireNonNull(data.getData()).getPath();
            FilePath = exe.RunAsRootOutput("echo " + FilePath + " | sed -e 's/\\/document\\/primary:/\\/sdcard\\//g'");
            String FilePy = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd unzip -Z1 '" + FilePath + "' | grep .py | awk -F'.' '{print $1}'");
            run_cmd("wifipumpkin3 -x \"use misc.custom_captiveflask; install " + FilePy + " \"" +  FilePath + "\"; back; exit\";exit");
        }
    }
    /*@Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater menuinflater) {
        menuinflater.inflate(R.menu.wifipumpkin, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 处理操作栏项目的点击
        switch (item.getItemId()) {
            case R.id.start_service:
                startWP3();
                return true;
            case R.id.stop_service:
                //stopWP3();
                return true;
            case R.id.first_run:
                Firstrun();
                return true;
            case R.id.source_button:
                Intent i = new Intent(activity, EditSourceActivity.class);
                i.putExtra("path", configFilePath);
                startActivity(i);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }*/

    // 首次设置对话框
    public void SetupDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(), R.style.DialogStyleCompat);
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        builder.setTitle("欢迎使用 Wifipumpkin3！");
        builder.setMessage("您缺少必要的软件包. 现在安装吗？");
        builder.setPositiveButton("安装", (dialog, which) -> {
            RunSetup();
            sharedpreferences.edit().putBoolean("wp3_setup_done", true).apply();
        });
        builder.show();
    }

    public void RunSetup() {
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        run_cmd("echo -ne \"\\033]0;Wifipumpkin3 设置\\007\" && clear;apt update && apt install wifipumpkin3 dnschef -y;" +
                "echo '完成！'; echo '窗口将在 3 秒后关闭..'; sleep 3 && exit ");
        sharedpreferences.edit().putBoolean("set_setup_done", true).apply();
    }

    public void LinkTemplates() {
        String cmd =
                "echo -ne \"\\033]0;Wifipumpkin3 模板\\007\" && clear;" +
                        "TEMPL_DIR='/root/.config/wifipumpkin3/config/templates';" +
                        "TARGET='/sdcard/nh_files/templates';" +
                        "if [ -L \"$TEMPL_DIR\" ]; then " +
                        "if [ \"$(readlink \"$TEMPL_DIR\")\" != \"$TARGET\" ]; then " +
                        "rm \"$TEMPL_DIR\"; " +
                        "ln -s \"$TARGET\" \"$TEMPL_DIR\"; " +
                        "fi; " +
                        "elif [ -d \"$TEMPL_DIR\" ]; then " +
                        "mv \"$TEMPL_DIR\" \"${TEMPL_DIR}_orig\"; " +
                        "ln -s \"$TARGET\" \"$TEMPL_DIR\"; " +
                        "elif [ ! -e \"$TEMPL_DIR\" ]; then " +
                        "ln -s \"$TARGET\" \"$TEMPL_DIR\"; " +
                        "fi; " +
                        "echo '完成！'; echo '窗口将在 3 秒后关闭..'; sleep 3 && exit";
        run_cmd(cmd);
        sharedpreferences.edit().putBoolean("set_setup_done", true).apply();
    }

    // 刷新模板列表
    private void refresh_wp3_templates(View WifipumpkinFragment) {
        Spinner TemplatesSpinner = WifipumpkinFragment.findViewById(R.id.templates);
        final String outputTemplates = "None\n" + exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd ls /root/.config/wifipumpkin3/config/templates | tail -n +10");
        final String[] TemplatesArray = outputTemplates.split("\n");
        TemplatesSpinner.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_list_item_1, TemplatesArray));
    }
    private void checkiptables() {
        ShellExecuter exe = new ShellExecuter();
        String iptables_ver = exe.RunAsRootOutput(NhPaths.APP_SCRIPTS_PATH + "/bootkali custom_cmd iptables -V | grep iptables");
        String old_kali = "https://old.kali.org/kali/pool/main/i/iptables/";
        if (iptables_ver.equals("iptables v1.6.2")) {
            MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            builder.setTitle("您需要升级 iptables！");
            builder.setMessage("感谢您耐心使用旧版 iptables 的 Mana. 现在终于可以升级了. ");
            builder.setPositiveButton("升级", (dialog, which) -> run_cmd("echo -ne \"\\033]0;正在升级 iptables\\007\" && clear;" +
                    "apt-mark unhold libip* > /dev/null 2>&1 ; " +
                    "apt-mark unhold libxtables* > /dev/null 2>&1 ; " +
                    "apt-mark unhold iptables* > /dev/null 2>&1 ; " +
                    "apt install iptables -y && sleep 2 && echo '完成！正在关闭窗口..' && exit"));
            builder.setNegativeButton("关闭", (dialog, which) -> {
            });
            builder.show();
        }
    }

    private void startWP3() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
        builder.setTitle("要执行的脚本:");
        builder.setPositiveButton("启动", (dialog, which) -> {
            switch (selectedScriptIndex) {
                // 在终端中启动 mana 以免其突然终止
                case 0:
                    NhPaths.showMessage(context, "正在启动 MANA NAT 完整模式");
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                        run_cmd(NhPaths.makeTermTitle("MANA-FULL") + "/usr/share/mana-toolkit/run-mana/start-nat-full-lollipop.sh");
                    } else {
                        run_cmd(NhPaths.makeTermTitle("MANA-FULL") + "/usr/share/mana-toolkit/run-mana/start-nat-full-kitkat.sh");
                    }
                    break;
                case 1:
                    NhPaths.showMessage(context, "正在启动 MANA NAT 简单模式");
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                        run_cmd(NhPaths.makeTermTitle("MANA-SIMPLE") + "/usr/share/mana-toolkit/run-mana/start-nat-simple-lollipop.sh");
                    } else {
                        run_cmd(NhPaths.makeTermTitle("MANA-SIMPLE") + "/usr/share/mana-toolkit/run-mana/start-nat-simple-kitkat.sh");
                    }
                    break;
                case 2:
                    NhPaths.showMessage(context, "正在启动 MANA Bettercap");
                    run_cmd(NhPaths.makeTermTitle("MANA-BETTERCAP") + "/usr/bin/start-nat-transproxy-lollipop.sh");
                    break;
                case 3:
                    NhPaths.showMessage(context, "正在启动 MANA NAT 简单模式 && BDF");
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                        run_cmd(NhPaths.makeTermTitle("MANA-BDF") + "/usr/share/mana-toolkit/run-mana/start-nat-simple-bdf-lollipop.sh");
                    } else {
                        run_cmd(NhPaths.makeTermTitle("MANA-BDF") + "/usr/share/mana-toolkit/run-mana/start-nat-simple-bdf-kitkat.sh");
                    }
                    // 在启动 msf 前等待约 10 秒
                    new android.os.Handler().postDelayed(
                            () -> {
                                NhPaths.showMessage(context, "正在使用 BDF resource.rc 启动 MSF");
                                run_cmd(NhPaths.makeTermTitle("MSF") + "msfconsole -q -r /usr/share/bdfproxy/bdfproxy_msf_resource.rc");
                            }, 10000);
                    break;
                case 4:
                    NhPaths.showMessage(context, "正在启动 HOSTAPD-WPE");
                    run_cmd(NhPaths.makeTermTitle("HOSTAPD-WPE") + "ip link set wlan1 up && /usr/sbin/hostapd-wpe /sdcard/nh_files/configs/hostapd-wpe.conf");
                    break;
                case 5:
                    NhPaths.showMessage(context, "正在启动带 Karma 的 HOSTAPD-WPE");
                    run_cmd(NhPaths.makeTermTitle("HOSTAPD-WPE-KARMA") + "ip link set wlan1 up && /usr/sbin/hostapd-wpe -k /sdcard/nh_files/configs/hostapd-wpe.conf");
                    break;
                default:
                    NhPaths.showMessage(context, "无效的脚本！");
                    return;
            }
            NhPaths.showMessage(context, getString(R.string.attack_launched));
        });
        builder.setNegativeButton("退出", (dialog, which) -> {
        });
        builder.setSingleChoiceItems(scripts, selectedScriptIndex, (dialog, which) -> selectedScriptIndex = which);
        builder.show();

    }

    public void Firstrun() {
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        run_cmd("echo -ne \"\\033]0;Mana 首次设置\\007\"" +
                "apt update && apt install mana-toolkit hostapd hostapd-wpe");
        sharedpreferences.edit().putBoolean("setup_done", true).apply();
    }

    /* private void stopWP3() {
        ShellExecuter exe = new ShellExecuter();
        String[] command = new String[1];
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            command[0] = NhPaths.APP_SCRIPTS_PATH + "/bootkali mana-lollipop stop'";
        } else {
            command[0] = NhPaths.APP_SCRIPTS_PATH + "/bootkali mana-kitkat stop'";
        }
        exe.RunAsRoot(command);
        NhPaths.showMessage(context, "Mana 已停止");
    } */

    public class HostapdFragmentWPE extends Fragment {
        private Context context;
        private String configFilePath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            configFilePath = NhPaths.APP_SD_FILES_PATH + "/configs/hostapd-wpe.conf";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.mana_hostapd_wpe, container, false);

            Button button = rootView.findViewById(R.id.wpe_updateButton);
            Button gencerts = rootView.findViewById(R.id.wpe_generate_certs);
            loadOptions(rootView);

            // 提取的命令作为常量
            final String GENERATE_CERTS_CMD = "cd /etc/hostapd-wpe/certs && ./bootstrap";

            gencerts.setOnClickListener(v -> run_cmd(GENERATE_CERTS_CMD));

            button.setOnClickListener(v -> {
                ShellExecuter exe = new ShellExecuter();
                File file = new File(configFilePath);
                String source;
                try {
                    source = Files.asCharSource(file, Charsets.UTF_8).read();
                } catch (IOException e) {
                    NhPaths.showMessage(context, "读取配置文件失败. ");
                    e.printStackTrace();
                    return;
                }

                View view = getView();
                if (view == null) {
                    return;
                }

                EditText ifc = view.findViewById(R.id.wpe_ifc);
                EditText bssid = view.findViewById(R.id.wpe_bssid);
                EditText ssid = view.findViewById(R.id.wpe_ssid);
                EditText channel = view.findViewById(R.id.wpe_channel);
                EditText privatekey = view.findViewById(R.id.wpe_private_key);

                if (source != null) {
                    source = updateConfig(source, "interface", ifc.getText().toString());
                    source = updateConfig(source, "bssid", bssid.getText().toString());
                    source = updateConfig(source, "ssid", ssid.getText().toString());
                    source = updateConfig(source, "channel", channel.getText().toString());
                    source = updateConfig(source, "private_key_passwd", privatekey.getText().toString());

                    exe.SaveFileContents(source, configFilePath);
                    NhPaths.showMessage(context, "配置已更新");
                }
            });
            return rootView;
        }

        private String updateConfig(String source, String key, String value) {
            return source.replaceAll("(?m)^" + key + "=(.*)$", key + "=" + value);
        }

        private void loadOptions(View rootView) {
            final EditText ifc = rootView.findViewById(R.id.wpe_ifc);
            final EditText bssid = rootView.findViewById(R.id.wpe_bssid);
            final EditText ssid = rootView.findViewById(R.id.wpe_ssid);
            final EditText channel = rootView.findViewById(R.id.wpe_channel);
            final EditText privatekey = rootView.findViewById(R.id.wpe_private_key);

            new Thread(() -> {
                ShellExecuter exe = new ShellExecuter();
                Log.d("exe: ", configFilePath);
                String text = exe.ReadFile_SYNC(configFilePath);

                String regExpatInterface = "^interface=(.*)$";
                Pattern patternIfc = Pattern.compile(regExpatInterface, Pattern.MULTILINE);
                final Matcher matcherIfc = patternIfc.matcher(text);

                String regExpatbssid = "^bssid=(.*)$";
                Pattern patternBssid = Pattern.compile(regExpatbssid, Pattern.MULTILINE);
                final Matcher matcherBssid = patternBssid.matcher(text);

                String regExpatssid = "^ssid=(.*)$";
                Pattern patternSsid = Pattern.compile(regExpatssid, Pattern.MULTILINE);
                final Matcher matcherSsid = patternSsid.matcher(text);

                String regExpatChannel = "^channel=(.*)$";
                Pattern patternChannel = Pattern.compile(regExpatChannel, Pattern.MULTILINE);
                final Matcher matcherChannel = patternChannel.matcher(text);

                String regExpatEnablePrivateKey = "^private_key_passwd=(.*)$";
                Pattern patternEnablePrivateKey = Pattern.compile(regExpatEnablePrivateKey, Pattern.MULTILINE);
                final Matcher matcherPrivateKey = patternEnablePrivateKey.matcher(text);

                ifc.post(() -> {
                    /*
                     * 接口
                     */
                    if (matcherIfc.find()) {
                        String ifcValue = matcherIfc.group(1);
                        ifc.setText(ifcValue);
                    }
                    /*
                     * bssid
                     */
                    if (matcherBssid.find()) {
                        String bssidVal = matcherBssid.group(1);
                        bssid.setText(bssidVal);
                    }
                    /*
                     * ssid
                     */
                    if (matcherSsid.find()) {
                        String ssidVal = matcherSsid.group(1);
                        ssid.setText(ssidVal);
                    }
                    /*
                     * 频道
                     */
                    if (matcherChannel.find()) {
                        String channelVal = matcherChannel.group(1);
                        channel.setText(channelVal);
                    }
                    /*
                     * 私钥文件
                     */
                    if (matcherPrivateKey.find()) {
                        String PrivateKeyVal = matcherPrivateKey.group(1);
                        privatekey.setText(PrivateKeyVal);
                    }
                });
            }).start();
        }
    }

    public static class DhcpdFragment extends Fragment {
        final ShellExecuter exe = new ShellExecuter();
        private Context context;
        private String configFilePath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            configFilePath = NhPaths.CHROOT_PATH() + "/etc/dhcp/dhcpd.conf";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            final View rootView = inflater.inflate(R.layout.source_short, container, false);

            String description = getResources().getString(R.string.mana_dhcpd);
            TextView desc = rootView.findViewById(R.id.description);
            desc.setText(description);

            EditText source = rootView.findViewById(R.id.source);
            exe.ReadFile_ASYNC(configFilePath, source);
            Button button = rootView.findViewById(R.id.update);
            button.setOnClickListener(v -> {
                Boolean isSaved = exe.SaveFileContents(source.getText().toString(), configFilePath);
                if (isSaved) {
                    NhPaths.showMessage(context, "配置已更新");
                } else {
                    NhPaths.showMessage(context, "配置未更新");
                }
            });
            return rootView;
        }
    }

    public static class DnsspoofFragment extends Fragment {
        private Context context;
        private String configFilePath;
        final ShellExecuter exe = new ShellExecuter();

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            configFilePath = NhPaths.CHROOT_PATH() + "/etc/mana-toolkit/dnsspoof.conf";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.source_short, container, false);
            String description = getResources().getString(R.string.mana_dnsspoof);
            TextView desc = rootView.findViewById(R.id.description);
            desc.setText(description);

            EditText source = rootView.findViewById(R.id.source);
            exe.ReadFile_ASYNC(configFilePath, source);

            Button button = rootView.findViewById(R.id.update);
            button.setOnClickListener(v -> {
                if (getView() == null) {
                    return;
                }
                EditText source1 = getView().findViewById(R.id.source);
                String newSource = source1.getText().toString();
                exe.SaveFileContents(newSource, configFilePath);
                NhPaths.showMessage(context, "配置已更新");
            });
            return rootView;
        }
    }

    public static class ManaNatFullFragment extends Fragment {
        private Context context;
        private String configFilePath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                configFilePath = NhPaths.CHROOT_PATH() + "/usr/share/mana-toolkit/run-mana/start-nat-full-lollipop.sh";
            } else {
                configFilePath = NhPaths.CHROOT_PATH() + "/usr/share/mana-toolkit/run-mana/start-nat-full-kitkat.sh";
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.source_short, container, false);
            TextView desc = rootView.findViewById(R.id.description);

            desc.setText(getResources().getString(R.string.mana_nat_full));

            EditText source = rootView.findViewById(R.id.source);
            ShellExecuter exe = new ShellExecuter();
            exe.ReadFile_ASYNC(configFilePath, source);
            Button button = rootView.findViewById(R.id.update);
            button.setOnClickListener(v -> {
                if (getView() == null) {
                    return;
                }
                EditText source1 = getView().findViewById(R.id.source);
                String newSource = source1.getText().toString();
                ShellExecuter exe1 = new ShellExecuter();
                exe1.SaveFileContents(newSource, configFilePath);
                NhPaths.showMessage(context, "配置已更新");
            });
            return rootView;
        }
    }

    public static class ManaNatSimpleFragment extends Fragment {
        private Context context;
        private String configFilePath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                configFilePath = NhPaths.CHROOT_PATH() + "/usr/share/mana-toolkit/run-mana/start-nat-simple-lollipop.sh";
            } else {
                configFilePath = NhPaths.CHROOT_PATH() + "/usr/share/mana-toolkit/run-mana/start-nat-simple-kitkat.sh";
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.source_short, container, false);

            String description = getResources().getString(R.string.mana_nat_simple);
            TextView desc = rootView.findViewById(R.id.description);
            desc.setText(description);

            EditText source = rootView.findViewById(R.id.source);
            ShellExecuter exe = new ShellExecuter();
            exe.ReadFile_ASYNC(configFilePath, source);

            Button button = rootView.findViewById(R.id.update);
            button.setOnClickListener(v -> {
                if (getView() == null) {
                    return;
                }
                EditText source1 = getView().findViewById(R.id.source);
                String newSource = source1.getText().toString();
                ShellExecuter exe1 = new ShellExecuter();
                exe1.SaveFileContents(newSource, configFilePath);
                NhPaths.showMessage(context, "配置已更新");
            });
            return rootView;
        }
    }

    public static class ManaNatBettercapFragment extends Fragment {
        private Context context;
        private String configFilePath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            configFilePath = NhPaths.CHROOT_PATH() + "/usr/bin/start-nat-transproxy-lollipop.sh";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.source_short, container, false);

            String description = getResources().getString(R.string.mana_bettercap_description);
            TextView desc = rootView.findViewById(R.id.description);
            desc.setText(description);

            EditText source = rootView.findViewById(R.id.source);
            ShellExecuter exe = new ShellExecuter();
            exe.ReadFile_ASYNC(configFilePath, source);

            Button button = rootView.findViewById(R.id.update);
            button.setOnClickListener(v -> {
                if (getView() == null) {
                    return;
                }
                EditText source1 = getView().findViewById(R.id.source);
                String newSource = source1.getText().toString();
                ShellExecuter exe1 = new ShellExecuter();
                exe1.SaveFileContents(newSource, configFilePath);
                NhPaths.showMessage(context, "配置已更新");
            });
            return rootView;
        }
    }

    public static class BdfProxyConfigFragment extends Fragment {
        private Context context;
        private String configFilePath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            configFilePath = NhPaths.APP_SD_FILES_PATH + "/configs/bdfproxy.cfg";
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.source_short, container, false);

            String description = getResources().getString(R.string.bdfproxy_cfg);
            TextView desc = rootView.findViewById(R.id.description);
            desc.setText(description);
            // 使用正确的那个？
            Log.d("BDFPATH", configFilePath);
            EditText source = rootView.findViewById(R.id.source);
            ShellExecuter exe = new ShellExecuter();
            exe.ReadFile_ASYNC(configFilePath, source);

            Button button = rootView.findViewById(R.id.update);
            button.setOnClickListener(v -> {
                if (getView() == null) {
                    return;
                }
                EditText source1 = getView().findViewById(R.id.source);
                String newSource = source1.getText().toString();
                ShellExecuter exe1 = new ShellExecuter();
                exe1.SaveFileContents(newSource, configFilePath);
                NhPaths.showMessage(context, "配置已更新");
            });
            return rootView;
        }
    }

    public static class ManaStartNatSimpleBdfFragment extends Fragment {
        private Context context;
        String configFilePath;

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            context = getContext();
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.LOLLIPOP) {
                configFilePath = NhPaths.CHROOT_PATH() + "/usr/share/mana-toolkit/run-mana/start-nat-simple-bdf-lollipop.sh";
            } else {
                configFilePath = NhPaths.CHROOT_PATH() + "/usr/share/mana-toolkit/run-mana/start-nat-simple-bdf-kitkat.sh";
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.source_short, container, false);

            String description = getResources().getString(R.string.mana_nat_simple_bdf);
            TextView desc = rootView.findViewById(R.id.description);
            desc.setText(description);
            EditText source = rootView.findViewById(R.id.source);
            ShellExecuter exe = new ShellExecuter();
            exe.ReadFile_ASYNC(configFilePath, source);
            Button button = rootView.findViewById(R.id.update);
            button.setOnClickListener(v -> {
                if (getView() == null) {
                    return;
                }
                EditText source1 = getView().findViewById(R.id.source);
                String newSource = source1.getText().toString();
                ShellExecuter exe1 = new ShellExecuter();
                exe1.SaveFileContents(newSource, configFilePath);
                NhPaths.showMessage(context, "配置已更新");
            });
            return rootView;
        }
    }

    ////
    // Bridge 端函数
    ////

    public void run_cmd(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/kali", cmd);
        activity.startActivity(intent);
    }
}