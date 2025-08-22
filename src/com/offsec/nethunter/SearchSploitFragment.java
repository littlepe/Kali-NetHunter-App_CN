package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class SearchSploitFragment extends Fragment {
    public static final String TAG = "SearchSploitFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private Boolean withFilters = true;
    private String sel_type;
    private String sel_platform;
    private String sel_search = "";
    private TextView numex;
    private AlertDialog adi;
    private Boolean isLoaded = false;
    private ListView searchSploitListView;
    private List<SearchSploit> full_exploitList;
    /* 创建并处理数据库 */
    private SearchSploitSQL database;
    private Context context;
    private Activity activity;

    public static SearchSploitFragment newInstance(int sectionNumber) {
        SearchSploitFragment fragment = new SearchSploitFragment();
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.searchsploit, container, false);

        setHasOptionsMenu(true);
        database = new SearchSploitSQL(context);
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
        builder.setTitle("漏洞数据库归档");
        builder.setMessage("加载中…请稍候");

        adi = builder.create();
        adi.setCancelable(false);
        adi.show();
        /* 搜索栏 */
        numex = rootView.findViewById(R.id.numex);
        final SearchView searchStr = rootView.findViewById(R.id.searchSploit_searchbar);
        searchStr.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (query.length() > 1) {
                    sel_search = query;
                } else {
                    sel_search = "";
                }
                loadExploits();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                if (query.isEmpty()) {
                    sel_search = "";
                    loadExploits();
                }

                return false;
            }
        });
        /* 加载/重新加载数据库按钮 */
        final Button searchSearchSploit = rootView.findViewById(R.id.serchsploit_loadDB);
        final ProgressBar progressBar = rootView.findViewById(R.id.progressBar);
        searchSearchSploit.setOnClickListener(v -> {
            progressBar.setVisibility(View.VISIBLE);
            new Thread(() -> {
                final Boolean isFeeded = database.doDbFeed();
                searchSearchSploit.post(() -> {
                    if (isFeeded) {
                        NhPaths.showMessage_long(context, "数据库加载完成");
                        try {
                            /* 复制数据库 */
                            String sd = Environment.getExternalStorageDirectory().getPath();
                            String data = NhPaths.APP_PATH + "/";
                            String DATABASE_NAME = "SearchSploit";
                            String currentDBPath = "databases/" + DATABASE_NAME;
                            String backupDBPath = "/nh_files/" + DATABASE_NAME; // 从 SD 目录

                            File backupDB = new File(data, currentDBPath);
                            File currentDB = new File(sd, backupDBPath);

                            FileChannel src = new FileInputStream(currentDB).getChannel();
                            FileChannel dst = new FileOutputStream(backupDB).getChannel();
                            dst.transferFrom(src, 0, src.size());

                            src.close();
                            dst.close();
                            Log.d("importDB", "成功导入 " + DATABASE_NAME);
                            main(rootView);
                        } catch (Exception e) {
                            Log.d("importDB", e.toString());
                        }
                    } else {
                        NhPaths.showMessage_long(context,
                                "无法找到 Searchsploit 的 files.csv 数据库, 请在 chroot 中安装 exploitdb");
                    }
                    progressBar.setVisibility(View.GONE);
                });
            }).start();
        });
        /* 防止菜单卡住 */
        new android.os.Handler().postDelayed(
                () -> main(rootView), 250);


        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.searchsploit, menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == R.id.rawSearch_ON) {
            if (getView() == null) return true;
            if (!withFilters) {
                assert getView() != null;
                requireView().findViewById(R.id.search_filters).setVisibility(View.VISIBLE);
                withFilters = true;
                item.setTitle("启用原始搜索");
                loadExploits();
                hideSoftKeyboard(getView());
            } else {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                builder.setTitle("原始搜索警告");

                builder.setMessage("漏洞数据库非常大（超过 3 万条）, 启用原始搜索将降低搜索速度. \n当您找不到漏洞时, 可用于全局搜索. ")
                        .setNegativeButton("取消", (dialog, id) -> dialog.dismiss())
                        .setPositiveButton("启用", (dialog, id) -> {
                            getView().findViewById(R.id.search_filters).setVisibility(View.GONE);
                            item.setTitle("禁用原始搜索");
                            withFilters = false;
                            loadExploits();
                            hideSoftKeyboard(getView());
                        });

                AlertDialog ad = builder.create();
                ad.setCancelable(false);
                ad.show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static void hideSoftKeyboard(final View caller) {
        caller.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) caller.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(caller.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        }, 100);
    }

    private void main(final View rootView) {
        searchSploitListView = rootView.findViewById(R.id.searchResultsList);
        Long exploitCount = database.getCount();
        Button searchSearchSploit = rootView.findViewById(R.id.serchsploit_loadDB);
        if (exploitCount == 0) {
            searchSearchSploit.setVisibility(View.VISIBLE);
            rootView.findViewById(R.id.search_filters).setVisibility(View.GONE);
            adi.dismiss();
            hideSoftKeyboard(requireView());
            return;
        } else {
            rootView.findViewById(R.id.search_filters).setVisibility(View.VISIBLE);
            searchSearchSploit.setVisibility(View.GONE);
        }

        final List<String> platformList = database.getPlatforms();
        Spinner platformSpin = rootView.findViewById(R.id.exdb_platform_spinner);
        ArrayAdapter<String> adp12 = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, platformList);
        adp12.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        platformSpin.setAdapter(adp12);
        platformSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                sel_platform = platformList.get(position);
                loadExploits();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });

        final List<String> typeList = database.getTypes();
        Spinner typeSpin = rootView.findViewById(R.id.exdb_type_spinner);
        ArrayAdapter<String> adp13 = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, typeList);
        adp13.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        typeSpin.setAdapter(adp13);
        typeSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                sel_type = typeList.get(position);
                loadExploits();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
            }
        });
        loadExploits();
    }

    private void loadExploits() {
        if ((sel_platform != null) && (sel_type != null)) {
            List<SearchSploit> exploitList;
            if (withFilters) {
                exploitList = database.getAllExploitsFiltered(sel_search, sel_type, sel_platform);
            } else {
                if (sel_search.isEmpty()) {
                    exploitList = full_exploitList;
                } else {
                    exploitList = database.getAllExploitsRaw(sel_search);
                }
            }
            if (exploitList == null) {
                new android.os.Handler().postDelayed(
                        this::loadExploits, 1500);
                return;
            }
            numex.setText(String.format("%d 条结果", exploitList.size()));
            ExploitLoader exploitAdapter = new ExploitLoader(context, exploitList);
            searchSploitListView.setAdapter(exploitAdapter);
            if (!isLoaded) {
                /* 预加载长列表, 看是否更流畅 */
                /* 在后台预加载 */
                new Thread(() -> full_exploitList = database.getAllExploitsRaw("")).start();

                adi.dismiss();
                isLoaded = true;
                hideSoftKeyboard(requireView());
            }
        }
    }
}

class ExploitLoader extends BaseAdapter {
    private final List<SearchSploit> _exploitList;
    private final Context _mContext;

    ExploitLoader(Context context, List<SearchSploit> exploitList) {
        _mContext = context;
        _exploitList = exploitList;
    }

    static class ViewHolderItem {
        /* 类型 */
        TextView type;
        /* 平台 */
        TextView platform;
        /* 作者 */
        TextView author;
        /* 日期 */
        TextView date;
        /* 描述 */
        TextView description;
        /* 查看源码按钮 */
        Button viewSource;
        /* 打开网页按钮 */
        Button openWeb;
        /* 发送到 HID 按钮 */
        Button sendHid;
    }

    public int getCount() {
        /* 返回服务数量 */
        return _exploitList.size();
    }

    private void start(String file) {
        String[] command = new String[1];
        String nhpath = NhPaths.APP_PATH;
        command[0] = "su -mm -c " + nhpath + "/scripts/bootkali file2hid-file " + file;
        String test = "su -mm -c " + nhpath + "/scripts/bootkali file2hid-file " + file;
        Log.d("执行:", test);
        ShellExecuter exe = new ShellExecuter();
        exe.RunAsRoot(command);
    }

    /* 为 ListView 的每一项生成视图 */
    public View getView(final int position, View convertView, ViewGroup parent) {
        /* 为每一项加载布局（服务） */

        ViewHolderItem vH;

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) _mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.searchsploit_item, parent, false);

            /* 设置 ViewHolder */
            vH = new ViewHolderItem();
            vH.description = convertView.findViewById(R.id.description);
            vH.type = convertView.findViewById(R.id.type);
            vH.platform = convertView.findViewById(R.id.platform);
            vH.author = convertView.findViewById(R.id.author);
            vH.date = convertView.findViewById(R.id.exploit_date);
            vH.viewSource = convertView.findViewById(R.id.viewSource);
            vH.openWeb = convertView.findViewById(R.id.openWeb);
            vH.sendHid = convertView.findViewById(R.id.searchsploit_sendhid_button);
            convertView.setTag(vH);
        } else {
            /* 复用已存在的视图 */
            vH = (ViewHolderItem) convertView.getTag();
        }

        /* 移除监听器 */
        final SearchSploit exploitItem = getItem(position);

        final String _file = exploitItem.getFile();
        final Long _id = exploitItem.getId();
        String _desc = exploitItem.getDescription();
        String _date = exploitItem.getDate();
        String _author = exploitItem.getAuthor();
        String _type = exploitItem.getType();
        String _platform = exploitItem.getPlatform();

        vH.viewSource.setOnClickListener(null);
        vH.openWeb.setOnClickListener(null);
        /* 设置服务名称 */
        vH.description.setText(_desc);
        vH.type.setText(_type);
        vH.platform.setText(_platform);
        vH.author.setText(_author);
        vH.date.setText(_date);
        vH.viewSource.setOnClickListener(v -> {
            Intent i = new Intent(_mContext, EditSourceActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("path", "/data/local/nhsystem/kalifs/usr/share/exploitdb/" + _file);
            _mContext.startActivity(i);

        });
        vH.sendHid.setOnClickListener(v -> {
            start("/usr/share/exploitdb/" + _file);
        });
        vH.openWeb.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String url = "https://www.exploit-db.com/exploits/" + _id + "/";
            i.setData(Uri.parse(url));
            _mContext.startActivity(i);
        });
        return convertView;
    }

    public SearchSploit getItem(int position) {
        return _exploitList.get(position);
    }

    public long getItemId(int position) {
        return position;
    }
}