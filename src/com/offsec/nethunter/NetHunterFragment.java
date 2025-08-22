package com.offsec.nethunter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.offsec.nethunter.RecyclerViewAdapter.NethunterRecyclerViewAdapter;
import com.offsec.nethunter.RecyclerViewAdapter.NethunterRecyclerViewAdapterDeleteItems;
import com.offsec.nethunter.RecyclerViewData.NethunterData;
import com.offsec.nethunter.SQL.NethunterSQL;
import com.offsec.nethunter.models.NethunterModel;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.viewmodels.NethunterViewModel;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static com.offsec.nethunter.R.id.f_nethunter_action_search;
import static com.offsec.nethunter.R.id.f_nethunter_action_snowfall;

public class NetHunterFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private Context context;
    private Activity activity;
    private NethunterRecyclerViewAdapter nethunterRecyclerViewAdapter;
    private Button refreshButton;
    private MenuItem snowfallButton;
    private Button addButton;
    private Button deleteButton;
    private Button moveButton;
    private static int targetPositionId;
    private SharedPreferences sharedpreferences;

    public static NetHunterFragment newInstance(int sectionNumber) {
        NetHunterFragment fragment = new NetHunterFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        this.context = getContext();
        this.activity = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.nethunter, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NethunterViewModel nethunterViewModel = new ViewModelProvider(this).get(NethunterViewModel.class);
        nethunterViewModel.init(context);

        nethunterViewModel.getLiveDataNethunterModelList().observe(getViewLifecycleOwner(), nethunterModelList -> nethunterRecyclerViewAdapter.notifyDataSetChanged());

        nethunterRecyclerViewAdapter = new NethunterRecyclerViewAdapter(context, nethunterViewModel.getLiveDataNethunterModelList().getValue());
        RecyclerView itemRecyclerView = view.findViewById(R.id.f_nethunter_recyclerview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        itemRecyclerView.setLayoutManager(linearLayoutManager);
        itemRecyclerView.setAdapter(nethunterRecyclerViewAdapter);

        refreshButton = view.findViewById(R.id.f_nethunter_refreshButton);
        addButton = view.findViewById(R.id.f_nethunter_addItemButton);
        deleteButton = view.findViewById(R.id.f_nethunter_deleteItemButton);
        moveButton = view.findViewById(R.id.f_nethunter_moveItemButton);

        onRefreshItemSetup();
        onAddItemSetup();
        onDeleteItemSetup();
        onMoveItemSetup();

        /* WearOS 优化 */
        TextView NHDesc = view.findViewById(R.id.f_nethunter_banner2);
        LinearLayout NHButtons = view.findViewById(R.id.f_nethunter_linearlayoutBtn);
        Boolean iswatch = requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        sharedpreferences.edit().putBoolean("running_on_wearos", iswatch).apply();
        if (iswatch) {
            NHDesc.setVisibility(View.GONE);
            NHButtons.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.nethunter, menu);
        final MenuItem searchItem = menu.findItem(f_nethunter_action_search);

        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);

        /* WearOS 优化 */
        Boolean iswatch = requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        Boolean snowfall;
        if (iswatch) {
            snowfall = sharedpreferences.getBoolean("snowfall_enabled", false);
            searchItem.setVisible(false);
        } else {
            snowfall = sharedpreferences.getBoolean("snowfall_enabled", true);
        }
        final SearchView searchView = (SearchView) searchItem.getActionView();
        assert searchView != null;
        searchView.setOnSearchClickListener(v -> menu.setGroupVisible(R.id.f_nethunter_menu_group1, false));
        searchView.setOnCloseListener(() -> {
            menu.setGroupVisible(R.id.f_nethunter_menu_group1, true);
            return false;
        });

        /* 雪花开关 */
        snowfallButton = menu.findItem(f_nethunter_action_snowfall);
        if (snowfall) snowfallButton.setIcon(R.drawable.snowflake_trigger);
        else snowfallButton.setIcon(R.drawable.snowflake_trigger_bw);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                nethunterRecyclerViewAdapter.getFilter().filter(newText);
                return false;
            }
        });
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View promptView = inflater.inflate(R.layout.nethunter_custom_dialog_view, null);
        final TextView titleTextView = promptView.findViewById(R.id.f_nethunter_adb_tv_title1);
        final EditText storedpathEditText = promptView.findViewById(R.id.f_nethunter_adb_et_storedpath);

        switch (item.getItemId()) {
            case R.id.f_nethunter_menu_backupDB:
                titleTextView.setText("请输入要保存数据库的完整路径: ");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentNethunter");
                MaterialAlertDialogBuilder adbBackup = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbBackup.setView(promptView);
                adbBackup.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbBackup.setPositiveButton("确定", (dialog, which) -> { });
                final androidx.appcompat.app.AlertDialog adBackup = adbBackup.create();
                adBackup.setOnShowListener(dialog -> {
                    final Button buttonOK = adBackup.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = NethunterData.getInstance().backupData(NethunterSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null) {
                            NhPaths.showMessage(context, "数据库已成功备份到 " + storedpathEditText.getText().toString());
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("备份失败").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adBackup.show();
                break;
            case R.id.f_nethunter_menu_restoreDB:
                titleTextView.setText("请输入要还原的数据库文件完整路径: ");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentNethunter");
                MaterialAlertDialogBuilder adbRestore = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbRestore.setView(promptView);
                adbRestore.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbRestore.setPositiveButton("确定", (dialog, which) -> { });
                final androidx.appcompat.app.AlertDialog adRestore = adbRestore.create();
                adRestore.setOnShowListener(dialog -> {
                    final Button buttonOK = adRestore.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = NethunterData.getInstance().restoreData(NethunterSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null) {
                            NhPaths.showMessage(context, "数据库已成功还原到 " + storedpathEditText.getText().toString());
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("还原失败").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adRestore.show();
                break;
            case R.id.f_nethunter_menu_ResetToDefault:
                NethunterData.getInstance().resetData(NethunterSQL.getInstance(context));
                break;
            /* 雪花开关 */
            case f_nethunter_action_snowfall:
                trigger_snowfall();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        NethunterData.getInstance().refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshButton = null;
        addButton = null;
        deleteButton = null;
        moveButton = null;
        nethunterRecyclerViewAdapter = null;
    }

    private void onRefreshItemSetup() {
        refreshButton.setOnClickListener(v -> NethunterData.getInstance().refreshData());
    }

    private void trigger_snowfall() {
        sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        Boolean iswatch = requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        Boolean snowfall;
        if (iswatch) {
            snowfall = sharedpreferences.getBoolean("snowfall_enabled", false);
        } else {
            snowfall = sharedpreferences.getBoolean("snowfall_enabled", true);
        }
        if (snowfall) {
            sharedpreferences.edit().putBoolean("snowfall_enabled", false).apply();
            snowfallButton.setIcon(R.drawable.snowflake_trigger_bw);
            Toast.makeText(requireActivity().getApplicationContext(), "雪花已禁用, 重启应用后生效. ", Toast.LENGTH_SHORT).show();
        } else {
            sharedpreferences.edit().putBoolean("snowfall_enabled", true).apply();
            snowfallButton.setIcon(R.drawable.snowflake_trigger);
            Toast.makeText(requireActivity().getApplicationContext(), "雪花已启用, 重启应用后生效. ", Toast.LENGTH_SHORT).show();
        }
    }

    private void onAddItemSetup() {
        addButton.setOnClickListener(v -> {
            List<NethunterModel> nethunterModelList = NethunterData.getInstance().nethunterModelListFull;
            if (nethunterModelList == null) return;
            final LayoutInflater mInflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptView = mInflater.inflate(R.layout.nethunter_add_dialog_view, null);
            final EditText titleEditText = promptView.findViewById(R.id.f_nethunter_add_adb_et_title);
            final EditText cmdEditText = promptView.findViewById(R.id.f_nethunter_add_adb_et_command);
            final EditText delimiterEditText = promptView.findViewById(R.id.f_nethunter_add_adb_et_delimiter);
            final CheckBox runOnCreateCheckbox = promptView.findViewById(R.id.f_nethunters_add_adb_checkbox_runoncreate);
            final Spinner insertPositions = promptView.findViewById(R.id.f_nethunter_add_adb_spr_positions);
            final Spinner insertTitles = promptView.findViewById(R.id.f_nethunter_add_adb_spr_titles);
            ArrayList<String> titleArrayList = new ArrayList<>();
            for (NethunterModel nethunterModel : nethunterModelList) {
                titleArrayList.add(nethunterModel.getTitle());
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, titleArrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            final FloatingActionButton readmeButton1 = promptView.findViewById(R.id.f_nethunter_add_btn_info_fab1);
            final FloatingActionButton readmeButton2 = promptView.findViewById(R.id.f_nethunter_add_btn_info_fab2);
            final FloatingActionButton readmeButton3 = promptView.findViewById(R.id.f_nethunter_add_btn_info_fab3);

            readmeButton1.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adb.setTitle("使用方法: ")
                        .setMessage(activity.getString(R.string.nethunter_howtouse_cmd))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton2.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adb.setTitle("使用方法: ")
                        .setMessage(activity.getString(R.string.nethunter_howtouse_delimiter))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton3.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adb.setTitle("使用方法: ")
                        .setMessage(activity.getString(R.string.nethunter_howtouse_runoncreate))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            delimiterEditText.setText("\\n");
            runOnCreateCheckbox.setChecked(true);

            insertPositions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // 插入到顶部
                    if (position == 0) {
                        insertTitles.setVisibility(View.INVISIBLE);
                        targetPositionId = 1;
                        // 插入到底部
                    } else if (position == 1) {
                        insertTitles.setVisibility(View.INVISIBLE);
                        targetPositionId = nethunterModelList.size() + 1;
                        // 插入到之前
                    } else if (position == 2) {
                        insertTitles.setVisibility(View.VISIBLE);
                        insertTitles.setAdapter(arrayAdapter);
                        insertTitles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                targetPositionId = position + 1;
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                        // 插入到之后
                    } else {
                        insertTitles.setVisibility(View.VISIBLE);
                        insertTitles.setAdapter(arrayAdapter);
                        insertTitles.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                targetPositionId = position + 2;
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });

            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adb.setNegativeButton("取消", (dialog, which) -> dialog.dismiss());
            adb.setPositiveButton("确定", (dialog, which) -> { });
            final androidx.appcompat.app.AlertDialog ad = adb.create();
            ad.setView(promptView);
            ad.setCancelable(true);
            ad.setOnShowListener(dialog -> {
                final Button buttonAdd = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonAdd.setOnClickListener(v1 -> {
                    if (titleEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage(context, "标题不能为空");
                    } else if (cmdEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage(context, "命令不能为空");
                    } else if (delimiterEditText.getText().toString().isEmpty()) {
                        NhPaths.showMessage(context, "分隔符不能为空");
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(titleEditText.getText().toString());
                        dataArrayList.add(cmdEditText.getText().toString());
                        dataArrayList.add(delimiterEditText.getText().toString());
                        dataArrayList.add(runOnCreateCheckbox.isChecked() ? "1" : "0");
                        NethunterData.getInstance().addData(targetPositionId, dataArrayList, NethunterSQL.getInstance(context));
                        ad.dismiss();
                    }
                });
            });
            ad.show();
        });
    }

    private void onDeleteItemSetup() {
        deleteButton.setOnClickListener(v -> {
            List<NethunterModel> nethunterModelList = NethunterData.getInstance().nethunterModelListFull;
            if (nethunterModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewDelete = inflater.inflate(R.layout.nethunter_delete_dialog_view, null, false);
            final RecyclerView recyclerViewDeleteItem = promptViewDelete.findViewById(R.id.f_nethunter_delete_recyclerview);
            NethunterRecyclerViewAdapterDeleteItems nethunterRecyclerViewAdapterDeleteItems = new NethunterRecyclerViewAdapterDeleteItems(context, nethunterModelList);
            LinearLayoutManager linearLayoutManagerDelete = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            recyclerViewDeleteItem.setLayoutManager(linearLayoutManagerDelete);
            recyclerViewDeleteItem.setAdapter(nethunterRecyclerViewAdapterDeleteItems);

            MaterialAlertDialogBuilder adbDelete = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adbDelete.setView(promptViewDelete);
            adbDelete.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            adbDelete.setPositiveButton("删除", (dialog, which) -> { });
            // 如果点击确定后想保持对话框打开, 需要这样处理...
            final androidx.appcompat.app.AlertDialog adDelete = adbDelete.create();
            adDelete.setMessage("请选择要删除的条目: ");
            adDelete.setOnShowListener(dialog -> {
                final Button buttonDelete = adDelete.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonDelete.setOnClickListener(v1 -> {
                    RecyclerView.ViewHolder viewHolder;
                    ArrayList<Integer> selectedPosition = new ArrayList<>();
                    ArrayList<Integer> selectedTargetIds = new ArrayList<>();
                    for (int i = 0; i < recyclerViewDeleteItem.getChildCount(); i++) {
                        viewHolder = recyclerViewDeleteItem.findViewHolderForAdapterPosition(i);
                        if (viewHolder != null) {
                            CheckBox box = viewHolder.itemView.findViewById(R.id.f_nethunter_recyclerview_dialog_chkbox);
                            if (box.isChecked()) {
                                selectedPosition.add(i);
                                selectedTargetIds.add(i + 1);
                            }
                        }
                    }
                    if (!selectedPosition.isEmpty()) {
                        NethunterData.getInstance().deleteData(selectedPosition, selectedTargetIds, NethunterSQL.getInstance(context));
                        NhPaths.showMessage(context, "成功删除 " + selectedPosition.size() + " 条记录. ");
                        adDelete.dismiss();
                    } else NhPaths.showMessage(context, "没有可删除的内容. ");
                });
            });
            adDelete.show();
        });
    }

    private void onMoveItemSetup() {
        moveButton.setOnClickListener(v -> {
            List<NethunterModel> nethunterModelList = NethunterData.getInstance().nethunterModelListFull;
            if (nethunterModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewMove = inflater.inflate(R.layout.nethunter_move_dialog_view, null, false);
            final Spinner titlesBefore = promptViewMove.findViewById(R.id.f_nethunter_move_adb_spr_titlesbefore);
            final Spinner titlesAfter = promptViewMove.findViewById(R.id.f_nethunter_move_adb_spr_titlesafter);
            final Spinner actions = promptViewMove.findViewById(R.id.f_nethunter_move_adb_spr_actions);
            ArrayList<String> titleArrayList = new ArrayList<>();
            for (NethunterModel nethunterModel : nethunterModelList) {
                titleArrayList.add(nethunterModel.getTitle());
            }
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, titleArrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            titlesBefore.setAdapter(arrayAdapter);
            titlesAfter.setAdapter(arrayAdapter);

            MaterialAlertDialogBuilder adbMove = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adbMove.setView(promptViewMove);
            adbMove.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            adbMove.setPositiveButton("移动", (dialog, which) -> { });
            final AlertDialog adMove = adbMove.create();
            adMove.setOnShowListener(dialog -> {
                final Button buttonMove = adMove.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonMove.setOnClickListener(v1 -> {
                    int originalPositionIndex = titlesBefore.getSelectedItemPosition();
                    int targetPositionIndex = titlesAfter.getSelectedItemPosition();
                    if (originalPositionIndex == targetPositionIndex ||
                            (actions.getSelectedItemPosition() == 0 && targetPositionIndex == (originalPositionIndex + 1)) ||
                            (actions.getSelectedItemPosition() == 1 && targetPositionIndex == (originalPositionIndex - 1))) {
                        NhPaths.showMessage(context, "您正在将项目移动到相同位置, 无需移动. ");
                    } else {
                        if (actions.getSelectedItemPosition() == 1) targetPositionIndex += 1;
                        NethunterData.getInstance().moveData(originalPositionIndex, targetPositionIndex, NethunterSQL.getInstance(context));
                        NhPaths.showMessage(context, "成功移动项目. ");
                        adMove.dismiss();
                    }
                });
            });
            adMove.show();
        });
    }
}