package com.offsec.nethunter;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.offsec.nethunter.Executor.CopyBootFilesExecutor;
import com.offsec.nethunter.SQL.CustomCommandsSQL;
import com.offsec.nethunter.SQL.KaliServicesSQL;
import com.offsec.nethunter.SQL.NethunterSQL;
import com.offsec.nethunter.SQL.USBArsenalSQL;
import com.offsec.nethunter.gps.KaliGPSUpdates;
import com.offsec.nethunter.gps.LocationUpdateService;
import com.offsec.nethunter.service.CompatCheckService;
import com.offsec.nethunter.utils.CheckForRoot;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.PermissionCheck;
import com.offsec.nethunter.utils.SharePrefTag;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.Stack;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;


public class AppNavHomeActivity extends AppCompatActivity implements KaliGPSUpdates.Provider {
    public final static String TAG = "AppNavHomeActivity";
    public static final String CHROOT_INSTALLED_TAG = "CHROOT_INSTALLED_TAG";
    public static final String GPS_BACKGROUND_FRAGMENT_TAG = "BG_FRAGMENT_TAG";
    public static final String BOOT_CHANNEL_ID = "BOOT_CHANNEL";
    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private com.google.android.material.navigation.NavigationView navigationView;
    private com.google.android.material.navigation.NavigationView navigationViewWear;
    private CharSequence mTitle = "NetHunter";
    private final Stack<String> titles = new Stack<>();
    private SharedPreferences prefs;
    public static MenuItem lastSelectedMenuItem;
    private boolean locationUpdatesRequested = false;
    private KaliGPSUpdates.Receiver locationUpdateReceiver;
    private NhPaths nhPaths;
    private PermissionCheck permissionCheck;
    private BroadcastReceiver nethunterReceiver;
    public static Boolean isBackPressEnabled = true;
    private int desiredFragment = -1;
    public CopyBootFilesExecutor copyBootFilesExecutor;
    public static MenuItem customCMDitem;
    private final ShellExecuter exe = new ShellExecuter();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // 初始化 NhPaths 单例类, 它将一直存在直到应用退出. 
        // 同时, 通过其注册的 SharedPreference 监听器, 可以在 SharedPreference 更改时立即更新 CHROOT_PATH 变量. 
        nhPaths = NhPaths.getInstance(getApplicationContext());

        // 我们需要在这里运行 root 检查, 以免其他操作被对话框阻塞
        if (!CheckForRoot.isRoot()){
            showWarningDialog("Root 权限", "需要 Root 权限！！", false);
        }

        // 这个函数会在这里延迟, 直到获得 root 权限. 
        // 目的: 在获得 root 访问权限之前, 不要运行任何其他操作 :)
        while (!CheckForRoot.isRoot()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                showWarningDialog("NetHunter 应用无法正常运行", "需要 Root 权限！！", true);
            }
        }

        // 初始化 PermissionCheck 类. 
        permissionCheck = new PermissionCheck(this, getApplicationContext());
        // 注册带有 Intent 操作的 NetHunter 接收器. 
        nethunterReceiver = new NethunterReceiver();
        IntentFilter AppNavHomeIntentFilter = new IntentFilter();
        AppNavHomeIntentFilter.addAction(NethunterReceiver.CHECKCOMPAT);
        AppNavHomeIntentFilter.addAction(NethunterReceiver.BACKPRESSED);
        AppNavHomeIntentFilter.addAction(NethunterReceiver.CHECKCHROOT);
        AppNavHomeIntentFilter.addAction("ChrootManager");
        this.registerReceiver(nethunterReceiver, new IntentFilter(AppNavHomeIntentFilter));
        // 初始化 prefs. 
        prefs = getSharedPreferences(BuildConfig.APPLICATION_ID, Context.MODE_PRIVATE);

        // 开始将应用文件复制到相应路径. 
        ProgressDialog progressDialog = new ProgressDialog(this);
        copyBootFilesExecutor = new CopyBootFilesExecutor(getApplicationContext(), this, progressDialog);
        copyBootFilesExecutor.setListener(new CopyBootFilesExecutor.CopyBootFilesExecutorListener() {
            @Override
            public void onPrepare() {
                progressDialog.setMessage("正在初始化...");
                progressDialog.setCancelable(false);
                progressDialog.setIndeterminate(true);
                progressDialog.show();
            }

            @Override
            public void onFinished(String result) {
                progressDialog.dismiss();
                if (result != null) {
                    switch (result) {
                        case "0":
                        case "1":
                        case "2":
                            showWarningDialog("NetHunter 应用无法正常运行", "复制文件失败, 请检查您的存储权限！！", true);
                            break;
                    }
                }
            }

            public void onExecutorPrepare() {
                Context context = getApplicationContext();
                if (context == null) {
                    Log.e(TAG, "Context 为空. 无法继续. ");
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle("正在初始化...");

                View customView = LayoutInflater.from(context).inflate(R.layout.progress_dialog, null);
                builder.setView(customView);
                builder.setCancelable(false);

                AlertDialog progressDialog = builder.create();
                progressDialog.show();
            }

            public void onExecutorFinished(Object result) {
                Context appContext = getApplicationContext();
                if (appContext == null) {
                    Log.e(TAG, "应用上下文为空. 无法继续. ");
                    return;
                }

                // 在 busybox_nh 复制后再次获取 busybox 路径. 
                NhPaths.BUSYBOX = NhPaths.getBusyboxPath();

                // 初始化所有 SQL 单例以减少切换 Fragment 时的延迟. 
                NethunterSQL.getInstance(appContext);
                KaliServicesSQL.getInstance(appContext);
                CustomCommandsSQL.getInstance(appContext);
                USBArsenalSQL.getInstance(appContext);

                // 设置默认的 SharedPreferences 值. 
                setDefaultSharePreference();

                // 检查是否安装了 busybox. 
                if (!CheckForRoot.isBusyboxInstalled()) {
                    showWarningDialog("NetHunter 应用无法正常运行", "未检测到 busybox, 请确保您已安装 busybox！！", true);
                }

                // 检查是否安装了 NetHunter 终端应用. 
                if (appContext.getPackageManager().getLaunchIntentForPackage("com.offsec.nhterm") == null) {
                    showWarningDialog("NetHunter 应用无法正常运行", "NetHunter 终端尚未安装. ", true);
                }

                // 检查是否已授予所有必需的权限. 
                if (isAllRequiredPermissionsGranted()) {
                    setRootView();
                }
            }
        });

        copyBootFilesExecutor.execute();
        // 检查是否已授予必需的权限. 
        if (isAllRequiredPermissionsGranted()) {
            setRootView();
        } else {
            // 请求必需的权限. 
            permissionCheck.requestPermissions();
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        if (item.getItemId() == android.R.id.home) {
            if (mDrawerLayout.isDrawerOpen(GravityCompat.START)) {
                mDrawerLayout.closeDrawers();
            } else {
                mDrawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionCheck.DEFAULT_PERMISSION_RQCODE || requestCode == PermissionCheck.NH_TERM_PERMISSIONS_RQCODE) {
            for (int grantResult:grantResults) {
                if (grantResult != 0) {
                    if (requestCode == PermissionCheck.NH_TERM_PERMISSIONS_RQCODE) {
                        boolean available = true;
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                getPackageManager().getPackageInfo("com.offsec.nhterm", PackageManager.PackageInfoFlags.of(0));
                            } else {
                                getPackageManager().getPackageInfo("com.offsec.nhterm", 0);
                            }
                            Log.d(TAG, "onRequestPermissionsResult: 找到 com.offsec.nhterm. ");
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(TAG, "onRequestPermissionsResult: 未找到 com.offsec.nhterm. ");
                            available = false;
                        }
                        if (!available) {
                            showWarningDialog("NetHunter 应用无法正常运行", "NetHunter 终端尚未安装, 请从商店安装！", true);
                            return;
                        }
                    }
                    showWarningDialog("NetHunter 应用无法正常运行", "请在应用外部授予所有权限请求, 或重新启动应用以再次授予其余权限. ", true);
                    return;
                }
            }
            if (isAllRequiredPermissionsGranted()) {
                setRootView();
            }
        }
    }

    @Override
    public boolean onReceiverReattach(KaliGPSUpdates.Receiver receiver) {
        Log.d(TAG, "onReceiverReattach");
        if (LocationUpdateService.isInstanceCreated()) {
            // 已经有一个服务在运行, 我们应该重新附加到它
            this.locationUpdateReceiver = receiver;
            Log.d(TAG, "locationService: " + !(locationService == null));
            if (locationService != null) {
                locationService.requestUpdates(locationUpdateReceiver);
            } else { // 应用可能被重新启动. 服务正在运行, 但我们尚未绑定它
                onLocationUpdatesRequested(receiver);
            }
            return true; // 重新附加
        }
        return false; // 没有可重新附加的对象
    }

    @Override
    public void onLocationUpdatesRequested(KaliGPSUpdates.Receiver receiver) {
        locationUpdatesRequested = true;
        this.locationUpdateReceiver = receiver;
        Intent intent = new Intent(getApplicationContext(), LocationUpdateService.class);
        bindService(intent, locationServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private LocationUpdateService locationService;
    private boolean updateServiceBound = false;
    private final ServiceConnection locationServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // 我们已经绑定到更新服务, 转换 IBinder 并获取 LocalService 实例
            LocationUpdateService.ServiceBinder binder = (LocationUpdateService.ServiceBinder) service;
            locationService = binder.getService();
            updateServiceBound = true;
            if (locationUpdatesRequested) {
                locationService.requestUpdates(locationUpdateReceiver);
            }

        }

        public void onServiceDisconnected(ComponentName arg0) {
            updateServiceBound = false;
        }
    };

    @Override
    public void onBackPressed() {
        //如果 isBackPressEnable 为 false, 则不允许用户按返回键. 
        if (isBackPressEnabled) {
            super.onBackPressed();
            if (titles.size() > 1) {
                titles.pop();
                mTitle = titles.peek();
            }
            Menu menuNav = navigationView.getMenu();
            int i = 0;
            int mSize = menuNav.size();
            while (i < mSize) {
                if (menuNav.getItem(i).getTitle() == mTitle) {
                    MenuItem _current = menuNav.getItem(i);
                    if (lastSelectedMenuItem != _current) {
                        //移除上一个
                        lastSelectedMenuItem.setChecked(false);
                        // 更新为下一个
                        lastSelectedMenuItem = _current;
                    }
                    //设置选中
                    _current.setChecked(true);
                    i = mSize;
                }
                i++;
            }
            restoreActionBar();
        }
    }

    @Override
    public void onStopRequested() {
        locationUpdatesRequested = false;
        if (locationService != null) {
            locationService.stopUpdates();
            locationService = null;
        }
        if (updateServiceBound) {
            updateServiceBound = false;
            unbindService(locationServiceConnection);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        // 运行兼容性检查服务
        if (navigationView != null) startService(new Intent(getApplicationContext(), CompatCheckService.class));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lastSelectedMenuItem = null;
        if (nethunterReceiver != null) {
            unregisterReceiver(nethunterReceiver);
        }
        if (nhPaths != null){
            nhPaths.onDestroy();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setRootView(){
        setContentView(R.layout.base_layout);

        //将 boot_kali 壁纸设置为背景
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mDrawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        // WearOS 优化
        Boolean iswatch = getBaseContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        Boolean snowfall;

        // 启用降雪效果 2/2
        prefs.edit().putBoolean("snowfall_enabled", false).apply();

        String model = Build.HARDWARE;
        if(iswatch){
            snowfall = prefs.getBoolean("snowfall_enabled", false);
            navigationView.getMenu().getItem(2).setVisible(false);
            navigationView.getMenu().getItem(3).setVisible(false);
            navigationView.getMenu().getItem(4).setVisible(false);
            if (model.equals("catfish") || model.equals("catshark") || model.equals("catshark-4g")) navigationView.getMenu().getItem(8).setVisible(false);
            navigationView.getMenu().getItem(9).setVisible(false);
            navigationView.getMenu().getItem(14).setVisible(false);
            navigationView.getMenu().getItem(17).setVisible(false);
            navigationView.getMenu().getItem(19).setVisible(false);
            navigationView.getMenu().getItem(20).setVisible(false);
            navigationView.getMenu().getItem(21).setVisible(false);
            navigationView.getMenu().getItem(22).setVisible(false);
            navigationView.getMenu().getItem(23).setVisible(false);
            navigationView.getMenu().getItem(24).setVisible(false);
        } else {
            snowfall = prefs.getBoolean("snowfall_enabled", true);
        }

        // 降雪效果
        View SnowfallView = findViewById(R.id.snowfall);
        if (snowfall) SnowfallView.setVisibility(View.VISIBLE);
        else SnowfallView.setVisibility(View.GONE);

        // 为不支持 ConfigFS 的设备禁用 USB Arsenal
        if (!new File("/config/usb_gadget/g1").exists())
            navigationView.getMenu().getItem(7).setVisible(false);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        @SuppressLint("InflateParams") LinearLayout navigationHeadView = (LinearLayout) inflater.inflate(R.layout.sidenav_header, null);
        navigationView.addHeaderView(navigationHeadView);

        FloatingActionButton readmeButton = navigationHeadView.findViewById(R.id.info_fab);
        readmeButton.setOnClickListener(v -> showLicense());

        /// 将构建信息移动到菜单中
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd KK:mm:ss a zzz",
                Locale.US);

        final String buildTime = sdf.format(BuildConfig.BUILD_TIME);
        TextView buildInfo1 = navigationHeadView.findViewById(R.id.buildinfo1);
        TextView buildInfo2 = navigationHeadView.findViewById(R.id.buildinfo2);
        buildInfo1.setText(String.format("版本: %s (%s)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));
        buildInfo2.setText(String.format("日期: %s", buildTime));

        if (navigationView != null) {
            setupDrawerContent(navigationView);
        }

        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.darkTitle));

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, NetHunterFragment.newInstance(R.id.nethunter_item))
                .commit();
        // 并将标题放入堆栈中, 以便在需要返回时使用
        titles.push(Objects.requireNonNull(navigationView.getMenu().getItem(0).getTitle()).toString());
        // 首先禁用所有 Fragment, 直到通过兼容性检查. 
        navigationView.getMenu().setGroupEnabled(R.id.chrootDependentGroup, false);
        // 如果尚未显示导航栏, 则显示它
        if (!prefs.getBoolean("seenNav", false)) {
            mDrawerLayout.openDrawer(GravityCompat.START);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putBoolean("seenNav", true);
            ed.apply();
        }

        if (lastSelectedMenuItem == null) { // 仅在第一次创建时
            lastSelectedMenuItem = navigationView.getMenu().getItem(0);
            lastSelectedMenuItem.setChecked(true);
        }
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.string.drawer_opened, R.string.drawer_closed);
        mDrawerLayout.addDrawerListener(mDrawerToggle);
        mDrawerToggle.syncState();
        startService(new Intent(getApplicationContext(), CompatCheckService.class));

        if (desiredFragment != -1) {
            changeDrawer(desiredFragment);
            desiredFragment = -1;
        }
    }

    private void showLicense() {
        // @binkybear 这里是更新日志等... \n\n%s
        String readmeData = String.format("%s\n\n%s\n\n%s",
                getResources().getString(R.string.licenseInfo),
                getResources().getString(R.string.nhwarning),
                getResources().getString(R.string.nhteam));
        final SpannableString readmeText = new SpannableString(readmeData);
        Linkify.addLinks(readmeText, Linkify.WEB_URLS);

        MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(this, R.style.DialogStyle);
        adb.setTitle("README 信息")
                .setMessage(readmeText);
        adb.setNegativeButton("取消", (dialog, id) -> dialog.cancel());
        adb.setCancelable(true);

        AlertDialog ad = adb.create();
        ad.setCancelable(false);
        if (ad.getWindow() != null) {
            ad.getWindow().getAttributes().windowAnimations = R.style.DialogStyle;
        }
        ad.show();
        ((TextView) Objects.requireNonNull(ad.findViewById(android.R.id.message))).setMovementMethod(LinkMovementMethod.getInstance());
    }

    @SuppressLint("NonConstantResourceId")
    private void setupDrawerContent(NavigationView navigationView) {
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    // 仅当与上一个不同时才更改
                    if (lastSelectedMenuItem != menuItem) {
                        //移除上一个
                        if(lastSelectedMenuItem != null)
                            lastSelectedMenuItem.setChecked(false);
                        // 更新为下一个
                        lastSelectedMenuItem = menuItem;
                    }
                    //设置选中
                    menuItem.setChecked(true);
                    mDrawerLayout.closeDrawers();
                    mTitle = menuItem.getTitle();
                    assert mTitle != null;
                    titles.push(mTitle.toString());

                    int itemId = menuItem.getItemId();
                    changeDrawer(itemId);
                    restoreActionBar();
                    return true;
                });
    }
    private void setupDrawerContentWear(NavigationView navigationViewWear) {
        navigationViewWear.setNavigationItemSelectedListener(
                menuItem -> {
                    // 仅当与上一个不同时才更改
                    if (lastSelectedMenuItem != menuItem) {
                        //移除上一个
                        if(lastSelectedMenuItem != null)
                            lastSelectedMenuItem.setChecked(false);
                        // 更新为下一个
                        lastSelectedMenuItem = menuItem;
                    }
                    //设置选中
                    menuItem.setChecked(true);
                    mDrawerLayout.closeDrawers();
                    mTitle = menuItem.getTitle();
                    assert mTitle != null;
                    titles.push(mTitle.toString());

                    int itemId = menuItem.getItemId();
                    changeDrawer(itemId);
                    restoreActionBar();
                    return true;
                });
    }

    private void changeDrawer(int itemId) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        switch (itemId) {
            case R.id.nethunter_item:
                changeFragment(fragmentManager, NetHunterFragment.newInstance(itemId));
                break;
                        /*
                        case R.id.kalilauncher_item:
                            fragmentManager
                                    .beginTransaction()
                                    .replace(R.id.container, KaliLauncherFragment.newInstance(itemId))
                                    .addToBackStack(null)
                                    .commit();
                            break;
                        */
            case R.id.can_item:
                changeFragment(fragmentManager, CANFragment.newInstance(itemId));
                break;
            case R.id.deauth_item:
                changeFragment(fragmentManager, DeAuthFragment.newInstance(itemId));
                break;
            case R.id.kaliservices_item:
                changeFragment(fragmentManager, KaliServicesFragment.newInstance(itemId));
                break;
            case R.id.custom_commands_item:
                changeFragment(fragmentManager, CustomCommandsFragment.newInstance(itemId));
                break;
            case R.id.hid_item:
                changeFragment(fragmentManager, HidFragment.newInstance(itemId));
                break;
            case R.id.duckhunter_item:
                changeFragment(fragmentManager, com.offsec.nethunter.DuckHunterFragment.newInstance(itemId));
                break;
            case R.id.usbarsenal_item:
                if (exe.RunAsRootReturnValue("ls /config/usb_gadget/g1") == 0) {
                    changeFragment(fragmentManager, USBArsenalFragment.newInstance(itemId));
                } else {
                    showWarningDialog("", "USB Arsenal (ConfigFS) 仅受 4.x 以上内核支持. 请注意, HID、RNDIS 和大容量存储应在具有 NetHunter 补丁的旧设备上自动启用. ", false);
                }
                break;
            case R.id.badusb_item:
                changeFragment(fragmentManager, BadusbFragment.newInstance(itemId));
                break;
            case R.id.wifipumpkin_item:
                changeFragment(fragmentManager, WifipumpkinFragment.newInstance(itemId));
                break;
            case R.id.wps_item:
                changeFragment(fragmentManager, WPSFragment.newInstance(itemId));
                break;
            case R.id.bt_item:
                changeFragment(fragmentManager, BTFragment.newInstance(itemId));
                break;
            case R.id.audio_item:
                changeFragment(fragmentManager, com.offsec.nethunter.AudioFragment.newInstance(itemId));
                break;
            case R.id.macchanger_item:
                changeFragment(fragmentManager, MacchangerFragment.newInstance(itemId));
                break;
            case R.id.createchroot_item:
                changeFragment(fragmentManager, ChrootManagerFragment.newInstance(itemId));
                break;
            case R.id.mpc_item:
                changeFragment(fragmentManager, MPCFragment.newInstance(itemId));
                break;
            case R.id.vnc_item:
                if (getApplicationContext().getPackageManager().getLaunchIntentForPackage("com.offsec.nethunter.kex") == null) {
                    showWarningDialog("", "NetHunter KeX 尚未安装, 请从商店安装！", false);
                } else {
                    changeFragment(fragmentManager, VNCFragment.newInstance(itemId));
                }
                break;
            case R.id.searchsploit_item:
                changeFragment(fragmentManager, com.offsec.nethunter.SearchSploitFragment.newInstance(itemId));
                break;
            case R.id.nmap_item:
                changeFragment(fragmentManager, NmapFragment.newInstance(itemId));
                break;
            case R.id.pineapple_item:
                changeFragment(fragmentManager, PineappleFragment.newInstance(itemId));
                break;
            case R.id.gps_item:
                changeFragment(fragmentManager, KaliGpsServiceFragment.newInstance(itemId));
                break;
            case R.id.settings_item:
                changeFragment(fragmentManager, SettingsFragment.newInstance(itemId));
                break;
            case R.id.kernel_item:
                changeFragment(fragmentManager, KernelFragment.newInstance(itemId));
                break;
            case R.id.modules_item:
                changeFragment(fragmentManager, ModulesFragment.newInstance(itemId));
                break;
            case R.id.set_item:
                changeFragment(fragmentManager, SETFragment.newInstance(itemId));
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowTitleEnabled(true);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            ab.setTitle(mTitle);
        }
    }

    public void blockActionBar(){
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setHomeButtonEnabled(false);
            ab.setDisplayHomeAsUpEnabled(false);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public void setDefaultSharePreference () {
        if (prefs.getString(SharePrefTag.DUCKHUNTER_LANG_SHAREPREF_TAG, null) == null) {
            prefs.edit().putString(SharePrefTag.DUCKHUNTER_LANG_SHAREPREF_TAG, "us").apply();
        }
        if (prefs.getString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, null) == null) {
            prefs.edit().putString(SharePrefTag.CHROOT_DEFAULT_BACKUP_SHAREPREF_TAG, NhPaths.SD_PATH + "/kalifs-backup.tar.gz").apply();
        }
        if (prefs.getString(SharePrefTag.CHROOT_DEFAULT_STORE_DOWNLOAD_SHAREPREF_TAG, null) == null) {
            prefs.edit().putString(SharePrefTag.CHROOT_DEFAULT_STORE_DOWNLOAD_SHAREPREF_TAG, NhPaths.SD_PATH + "/Download").apply();
        }
    }

    private void changeFragment(FragmentManager fragmentManager, Fragment fragment) {
        fragmentManager
                .beginTransaction()
                .replace(R.id.container, fragment)
                .addToBackStack(null)
                .commit();
    }

    private boolean isAllRequiredPermissionsGranted(){
        if (permissionCheck.isAllPermitted(PermissionCheck.DEFAULT_PERMISSIONS)) {
            permissionCheck.checkPermissions(PermissionCheck.DEFAULT_PERMISSIONS, PermissionCheck.DEFAULT_PERMISSION_RQCODE);
            return false;
        } else if (permissionCheck.isAllPermitted(PermissionCheck.NH_TERM_PERMISSIONS)) {
            permissionCheck.checkPermissions(PermissionCheck.NH_TERM_PERMISSIONS, PermissionCheck.NH_TERM_PERMISSIONS_RQCODE);
            return false;
        }
        return true;
    }

    public void showWarningDialog(String title, String message, boolean NeedToExit) {
        MaterialAlertDialogBuilder warningAD = new MaterialAlertDialogBuilder(this, R.style.DialogStyleCompat);
        warningAD.setCancelable(false);
        warningAD.setTitle(title);
        warningAD.setMessage(message);
        warningAD.setPositiveButton("关闭", (dialog, which) -> {
            dialog.dismiss();
            if (NeedToExit)
                System.exit(1);
        });
        warningAD.create().show();
    }

    // 主应用广播接收器, 用于响应不同的操作. 
    public class NethunterReceiver extends BroadcastReceiver{
        public static final String CHECKCOMPAT = BuildConfig.APPLICATION_ID + ".CHECKCOMPAT";
        public static final String BACKPRESSED = BuildConfig.APPLICATION_ID + ".BACKPRESSED";
        public static final String CHECKCHROOT = BuildConfig.APPLICATION_ID + ".CHECKCHROOT";

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null) {
                switch (intent.getAction()) {
                    case CHECKCOMPAT:
                        showWarningDialog("NetHunter 应用无法正常运行",
                                intent.getStringExtra("message"),
                                true);
                        break;
                    case BACKPRESSED:
                        isBackPressEnabled = (intent.getBooleanExtra("isEnable", true));
                        if (isBackPressEnabled) {
                            restoreActionBar();
                        } else {
                            blockActionBar();
                        }
                        break;
                    case CHECKCHROOT:
                        try{
                            if (intent.getBooleanExtra("ENABLEFRAGMENT", false)){
                                navigationView.getMenu().setGroupEnabled(R.id.chrootDependentGroup, true);
                            } else {
                                navigationView.getMenu().setGroupEnabled(R.id.chrootDependentGroup, false);
                                if (lastSelectedMenuItem.getItemId() != R.id.nethunter_item &&
                                        lastSelectedMenuItem.getItemId() != R.id.createchroot_item) {
                                    FragmentManager fragmentManager = getSupportFragmentManager();
                                    changeFragment(fragmentManager, NetHunterFragment.newInstance(R.id.nethunter_item));
                                }
                            }
                        } catch (Exception e) {
                            if (e.getMessage() != null) {
                                Log.e(AppNavHomeActivity.TAG, e.getMessage());
                            } else {
                                Log.e(AppNavHomeActivity.TAG, "e.getMessage 为空. ");
                            }
                        }
                        break;
                }
            }
        }
    }
}