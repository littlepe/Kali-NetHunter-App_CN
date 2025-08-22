package com.offsec.nethunter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Looper;
import android.os.Parcelable;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.BtDuckyFragment;
import com.offsec.nethunter.BuildConfig;
import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.SharePrefTag;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

public class DuckHunterFragment extends Fragment {
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static SharedPreferences sharedpreferences;
    // 语言变量
    private static final HashMap<String, String> map = new HashMap<>();
    public static String lang = "us"; // 默认语言设置为美国英语
    private static String[] keyboardLayoutString;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "DuckHunterFragment";
    private Context context;
    private Activity activity;
    private Menu menu;
    private ViewPager mViewPager;
    private String duckyInputFile ;
    private String duckyOutputFile;
    private boolean isReceiverRegistered;
    private boolean shouldconvert = true;
    private final DuckHuntBroadcastReceiver duckHuntBroadcastReceiver = new DuckHuntBroadcastReceiver();
    private final ShellExecuter exe = new ShellExecuter();

    public static DuckHunterFragment newInstance(int sectionNumber) {
        DuckHunterFragment fragment = new DuckHunterFragment();
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
        duckyInputFile = NhPaths.APP_SD_FILES_PATH + "/modules/ducky_in.txt";
        duckyOutputFile = NhPaths.APP_SD_FILES_PATH + "/modules/ducky_out.sh";
        map.put("美国英语", "us");
        map.put("土耳其语", "tr");
        map.put("瑞典语", "sv");
        map.put("斯洛文尼亚语", "si");
        map.put("俄语", "ru");
        map.put("葡萄牙语", "pt");
        map.put("挪威语", "no");
        map.put("克罗地亚语", "hr");
        map.put("英国英语", "gb");
        map.put("法语", "fr");
        map.put("芬兰语", "fi");
        map.put("西班牙语", "es");
        map.put("丹麦语", "dk");
        map.put("德语", "de");
        map.put("加拿大英语", "ca");
        map.put("加拿大多元语言标准", "cm");
        map.put("巴西语", "br");
        map.put("比利时语", "be");
        map.put("匈牙利语", "hu");
        keyboardLayoutString = map.keySet().toArray(new String[0]);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.duck_hunter, container, false);
        setHasOptionsMenu(true);
        TabsPagerAdapter tabsPagerAdapter = new TabsPagerAdapter(getChildFragmentManager());
        mViewPager = rootView.findViewById(R.id.pagerDuckHunter);
        mViewPager.setAdapter(tabsPagerAdapter);
        mViewPager.setOffscreenPageLimit(1);
        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position == 1) {
                    menu.findItem(R.id.duckConvertAttack).setVisible(true);
                    setLang();
                    if (shouldconvert)
                        activity.sendBroadcast(new Intent().putExtra("ACTION", "WRITEDUCKY").setAction(BuildConfig.APPLICATION_ID + ".WRITEDUCKY").setPackage(activity.getPackageName()));
                } else
                    menu.findItem(R.id.duckConvertAttack).setVisible(false);
            }
        });

        sharedpreferences = activity.getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        if (!sharedpreferences.contains("DuckHunterLanguageIndex")) {
            for (int i = 0; i < keyboardLayoutString.length; i++) {
                if ("us".equals(map.get(keyboardLayoutString[i]))) {
                    sharedpreferences.edit().putInt("DuckHunterLanguageIndex", i).apply();
                    break;
                }
            }
        }

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        this.menu = menu;
        inflater.inflate(R.menu.duck_hunter, menu);
        menu.findItem(R.id.duckConvertAttack).setVisible(false);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.duckConvertAttack:
                // 执行前步骤
                mainHandler.post(() -> NhPaths.showMessage(context, "正在启动攻击"));

                // 后台任务
                executorService.execute(() -> {
                    boolean result = exe.RunAsRootReturnValue("sh " + duckyOutputFile) == 0;

                    // 执行后步骤
                    mainHandler.post(() -> {
                        if (!result) {
                            if (new File("/config/usb_gadget/g1").exists()) {
                                NhPaths.showMessage_long(context, "HID 接口未启用！请在 USB Arsenal 中启用. ");
                            } else if (new File("/dev/hidg0").exists()) {
                                NhPaths.showMessage_long(context, "正在修复 HID 接口权限...");
                                exe.RunAsRoot(new String[]{"chmod 666 /dev/hidg*"});
                            } else {
                                NhPaths.showMessage_long(context, "HID 接口未启用或未打补丁, 请检查内核配置. ");
                            }
                        }
                    });
                });
                return true;

            case R.id.chooseLanguage:
                openLanguageDialog();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isReceiverRegistered){
            ContextCompat.registerReceiver(activity, duckHuntBroadcastReceiver, new IntentFilter(BuildConfig.APPLICATION_ID + ".SHOULDCONVERT"), ContextCompat.RECEIVER_NOT_EXPORTED);
            isReceiverRegistered = true;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isReceiverRegistered) {
            activity.unregisterReceiver(duckHuntBroadcastReceiver);
            isReceiverRegistered = false;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        menu = null;
        mViewPager = null;
    }

    private static void setLang() {
        int keyboardLayoutIndex = sharedpreferences.getInt("DuckHunterLanguageIndex", 0);
        lang = map.get(keyboardLayoutString[keyboardLayoutIndex]);
    }

    private void openLanguageDialog() {
        int keyboardLayoutIndex = sharedpreferences.getInt("DuckHunterLanguageIndex", 0);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
        builder.setTitle("语言: ");
        builder.setPositiveButton("确定", (dialog, which) -> {
            if (mViewPager.getCurrentItem() == 1) {
                if (getView() == null) {
                    return;
                }
                setLang();
                activity.sendBroadcast(new Intent().putExtra("ACTION", "WRITEDUCKY")
                        .setAction(BuildConfig.APPLICATION_ID + ".WRITEDUCKY")
                        .setPackage(activity.getPackageName()));
            }
        });
        builder.setSingleChoiceItems(keyboardLayoutString, keyboardLayoutIndex, (dialog, which) -> {
            Editor editor = sharedpreferences.edit();
            editor.putInt("DuckHunterLanguageIndex", which);
            editor.putString(SharePrefTag.DUCKHUNTER_LANG_SHAREPREF_TAG, map.get(keyboardLayoutString[which]));
            editor.apply();
        });
        builder.show();
    }

    public class TabsPagerAdapter extends FragmentStatePagerAdapter {
        TabsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int i) {
            switch (i) {
                case 1:
                    return new DuckHunterPreviewFragment(duckyInputFile, duckyOutputFile);
                case 2:
                    return new BtDuckyFragment();
                default:
                    return new DuckHunterConvertFragment(duckyInputFile, duckyOutputFile);
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
                    return "预览";
                case 2:
                    return "BT Ducky";
                default:
                    return "转换";
            }
        }
    }

    public class DuckHuntBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getStringExtra("ACTION"), "SHOULDCONVERT")) {
                shouldconvert = intent.getBooleanExtra("SHOULDCONVERT", true);
            }
        }
    }
}