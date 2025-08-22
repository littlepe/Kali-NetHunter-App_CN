package com.offsec.nethunter;

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
import android.widget.HorizontalScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.offsec.nethunter.RecyclerViewAdapter.KaliServicesRecyclerViewAdapter;
import com.offsec.nethunter.RecyclerViewAdapter.KaliServicesRecyclerViewAdapterDeleteItems;
import com.offsec.nethunter.RecyclerViewData.KaliServicesData;
import com.offsec.nethunter.SQL.KaliServicesSQL;
import com.offsec.nethunter.models.KaliServicesModel;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.viewmodels.KaliServicesViewModel;

import java.text.MessageFormat;
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

public class KaliServicesFragment extends Fragment {
    public static final String TAG = "KaliServicesFragment";
    private static final String ARG_SECTION_NUMBER = "section_number";
    private Activity activity;
    private Context context;
    private Button refreshButton;
    private Button addButton;
    private Button deleteButton;
    private Button moveButton;
    private KaliServicesRecyclerViewAdapter kaliServicesRecyclerViewAdapter;
    private static int targetPositionId;

    public static KaliServicesFragment newInstance(int sectionNumber) {
        KaliServicesFragment fragment = new KaliServicesFragment();
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

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup parent, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.kaliservices, parent, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        KaliServicesViewModel kaliServicesViewModel = new ViewModelProvider(this).get(KaliServicesViewModel.class);
        kaliServicesViewModel.init(context);
        kaliServicesViewModel.getLiveDataKaliServicesModelList().observe(getViewLifecycleOwner(), kaliServicesModelList -> kaliServicesRecyclerViewAdapter.notifyDataSetChanged());

        kaliServicesRecyclerViewAdapter = new KaliServicesRecyclerViewAdapter(context, kaliServicesViewModel.getLiveDataKaliServicesModelList().getValue());
        RecyclerView recyclerViewServiceTitle = view.findViewById(R.id.f_kaliservices_recyclerviewServiceTitle);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerViewServiceTitle.setLayoutManager(linearLayoutManager);
        recyclerViewServiceTitle.setAdapter(kaliServicesRecyclerViewAdapter);

        refreshButton = view.findViewById(R.id.f_kaliservices_refreshButton);
        addButton = view.findViewById(R.id.f_kaliservices_addItemButton);
        deleteButton = view.findViewById(R.id.f_kaliservices_deleteItemButton);
        moveButton = view.findViewById(R.id.f_kaliservices_moveItemButton);
        TextView servicesDesc = view.findViewById(R.id.f_kaliservices_banner);
        HorizontalScrollView servicesButtons = view.findViewById(R.id.f_kaliservices_btn_scrollView);

        onRefreshItemSetup();
        onAddItemSetup();
        onDeleteItemSetup();
        onMoveItemSetup();

        // WearOS 优化: 隐藏说明文字和按钮
        SharedPreferences sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        Boolean iswatch = sharedpreferences.getBoolean("running_on_wearos", false);
        if (iswatch) {
            servicesDesc.setVisibility(View.GONE);
            servicesButtons.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.kaliservices, menu);
        final MenuItem searchItem = menu.findItem(R.id.f_kaliservices_action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        // WearOS 优化: 隐藏搜索
        boolean iswatch = requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        if(iswatch) {
            searchItem.setVisible(false);
        }

        assert searchView != null;
        searchView.setOnSearchClickListener(v -> menu.setGroupVisible(R.id.f_kaliservices_menu_group1, false));
        searchView.setOnCloseListener(() -> {
            menu.setGroupVisible(R.id.f_kaliservices_menu_group1, true);
            return false;
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                kaliServicesRecyclerViewAdapter.getFilter().filter(newText);
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View promptView = inflater.inflate(R.layout.kaliservices_custom_dialog_view, null);
        final TextView titleTextView = promptView.findViewById(R.id.f_kaliservices_adb_tv_title1);
        final EditText storedpathEditText = promptView.findViewById(R.id.f_kaliservices_adb_et_storedpath);

        switch (item.getItemId()){
            case R.id.f_kaliservices_menu_backupDB:
                titleTextView.setText(R.string.kaliservices_full_path_save_db);
                storedpathEditText.setText(MessageFormat.format("{0}/FragmentKaliServices", NhPaths.APP_SD_SQLBACKUP_PATH));
                MaterialAlertDialogBuilder adbBackup = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbBackup.setView(promptView);
                adbBackup.setCancelable(true);
                adbBackup.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbBackup.setPositiveButton("确定", (dialog, which) -> { });
                final AlertDialog adBackup = adbBackup.create();
                adBackup.setOnShowListener(dialog -> {
                    final Button buttonOK = adBackup.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = KaliServicesData.getInstance().backupData(KaliServicesSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null){
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
            case R.id.f_kaliservices_menu_restoreDB:
                titleTextView.setText(R.string.kaliservices_full_path_restore_db);
                storedpathEditText.setText(MessageFormat.format("{0}/FragmentKaliServices", NhPaths.APP_SD_SQLBACKUP_PATH));
                MaterialAlertDialogBuilder adbRestore = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbRestore.setView(promptView);
                adbRestore.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbRestore.setPositiveButton("确定", (dialog, which) -> { });
                final AlertDialog adRestore = adbRestore.create();
                adRestore.setOnShowListener(dialog -> {
                    final Button buttonOK = adRestore.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = KaliServicesData.getInstance().restoreData(KaliServicesSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null) {
                            NhPaths.showMessage(context, "数据库已成功恢复到 " + storedpathEditText.getText().toString());
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("恢复失败").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adRestore.show();
                break;
            case R.id.f_kaliservices_menu_ResetToDefault:
                KaliServicesData.getInstance().resetData(KaliServicesSQL.getInstance(context));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onStart() {
        super.onStart();
        KaliServicesData.getInstance().refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        refreshButton = null;
        addButton = null;
        deleteButton = null;
        moveButton = null;
        kaliServicesRecyclerViewAdapter = null;
    }

    private void onRefreshItemSetup(){
        refreshButton.setOnClickListener(v -> KaliServicesData.getInstance().refreshData());
    }

    private void onAddItemSetup(){
        addButton.setOnClickListener(v -> {
            List<KaliServicesModel> kaliServicesModelList = KaliServicesData.getInstance().kaliServicesModelListFull;
            if (kaliServicesModelList == null) {
                NhPaths.showMessage(context, "服务列表为空, 请先刷新后再试. ");
                return;
            }
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewAdd = inflater.inflate(R.layout.kaliservices_add_dialog_view, null);
            final EditText titleEditText = promptViewAdd.findViewById(R.id.f_kaliservices_add_adb_et_title);
            final EditText startCmdEditText = promptViewAdd.findViewById(R.id.f_kaliservices_add_adb_et_startcommand);
            final EditText stopCmdEditText = promptViewAdd.findViewById(R.id.f_kaliservices_add_adb_et_stopcommand);
            final EditText checkstatusCmdEditText = promptViewAdd.findViewById(R.id.f_kaliservices_add_adb_et_checkstatuscommand);
            final CheckBox runOnChrootStartCheckbox = promptViewAdd.findViewById(R.id.f_kaliservices_add_adb_checkbox_runonboot);
            final FloatingActionButton readmeButton1 = promptViewAdd.findViewById(R.id.f_kaliservices_add_btn_info_fab1);
            final FloatingActionButton readmeButton2 = promptViewAdd.findViewById(R.id.f_kaliservices_add_btn_info_fab2);
            final FloatingActionButton readmeButton3 = promptViewAdd.findViewById(R.id.f_kaliservices_add_btn_info_fab3);
            final FloatingActionButton readmeButton4 = promptViewAdd.findViewById(R.id.f_kaliservices_add_btn_info_fab4);
            final Spinner insertPositions = promptViewAdd.findViewById(R.id.f_kaliservices_add_adb_spr_positions);
            final Spinner insertTitles = promptViewAdd.findViewById(R.id.f_kaliservices_add_adb_spr_titles);

            ArrayList<String> serviceNameArrayList = new ArrayList<>();
            for (KaliServicesModel kaliServicesModel: kaliServicesModelList){
                serviceNameArrayList.add(kaliServicesModel.getServiceName());
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, serviceNameArrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            startCmdEditText.setText(R.string.service_servicename_start);
            stopCmdEditText.setText(R.string.service_servicename_stop);
            checkstatusCmdEditText.setText(R.string.service_servicename);

            readmeButton1.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adb.setTitle("使用说明: ")
                        .setMessage(getString(R.string.kaliservices_howto_startservice))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton2.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adb.setTitle("使用说明: ")
                        .setMessage(getString(R.string.kaliservices_howto_stopservice))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton3.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adb.setTitle("使用说明: ")
                        .setMessage(getString(R.string.kaliservices_howto_checkservice))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton4.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adb.setTitle("使用说明: ")
                        .setMessage(getString(R.string.kaliservices_howto_runServiceOnBoot))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            insertPositions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // 如果选择插入到顶部
                    if (position == 0) {
                        insertTitles.setVisibility(View.INVISIBLE);
                        targetPositionId = 1;
                        // 如果选择插入到底部
                    } else if (position == 1) {
                        insertTitles.setVisibility(View.INVISIBLE);
                        targetPositionId = kaliServicesModelList.size() + 1;
                        // 如果选择插入到某一项之前
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
                        // 如果选择插入到某一项之后
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

            MaterialAlertDialogBuilder adbAdd = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adbAdd.setPositiveButton("确定", (dialog, which) -> { });
            final AlertDialog adAdd = adbAdd.create();
            adAdd.setView(promptViewAdd);
            adAdd.setCancelable(true);
            // 若想点击确定后保持对话框不关闭, 需按如下方式处理
            adAdd.setOnShowListener(dialog -> {
                final Button buttonAdd = adAdd.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonAdd.setOnClickListener(v1 -> {
                    if (titleEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, getString(R.string.error_title_empty));
                    } else if (startCmdEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "启动命令不能为空");
                    } else if (stopCmdEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "停止命令不能为空");
                    } else if (checkstatusCmdEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "状态检查命令不能为空");
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(titleEditText.getText().toString());
                        dataArrayList.add(startCmdEditText.getText().toString());
                        dataArrayList.add(stopCmdEditText.getText().toString());
                        dataArrayList.add(checkstatusCmdEditText.getText().toString());
                        dataArrayList.add(runOnChrootStartCheckbox.isChecked()?"1":"0");
                        KaliServicesData.getInstance().addData(targetPositionId, dataArrayList, KaliServicesSQL.getInstance(context));
                        adAdd.dismiss();
                    }
                });
            });
            adAdd.show();
        });
    }

    private void onDeleteItemSetup(){
        deleteButton.setOnClickListener(v -> {
            List<KaliServicesModel> kaliServicesModelList = KaliServicesData.getInstance().kaliServicesModelListFull;
            if (kaliServicesModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewDelete = inflater.inflate(R.layout.kaliservices_delete_dialog_view, null, false);
            final RecyclerView recyclerViewDeleteItem = promptViewDelete.findViewById(R.id.f_kaliservices_delete_recyclerview);
            KaliServicesRecyclerViewAdapterDeleteItems kaliServicesRecyclerViewAdapterDeleteItems = new KaliServicesRecyclerViewAdapterDeleteItems(context, kaliServicesModelList);

            LinearLayoutManager linearLayoutManagerDelete = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            recyclerViewDeleteItem.setLayoutManager(linearLayoutManagerDelete);
            recyclerViewDeleteItem.setAdapter(kaliServicesRecyclerViewAdapterDeleteItems);

            MaterialAlertDialogBuilder adbDelete = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adbDelete.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            adbDelete.setPositiveButton("删除", (dialog, which) -> { });
            final AlertDialog adDelete = adbDelete.create();
            adDelete.setMessage("请选择要删除的服务: ");
            adDelete.setView(promptViewDelete);
            adDelete.setCancelable(true);
            // 若想点击确定后保持对话框不关闭, 需按如下方式处理
            adDelete.setOnShowListener(dialog -> {
                final Button buttonDelete = adDelete.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonDelete.setOnClickListener(v1 -> {
                    RecyclerView.ViewHolder viewHolder;
                    ArrayList<Integer> selectedPosition = new ArrayList<>();
                    ArrayList<Integer> selectedTargetIds = new ArrayList<>();
                    for (int i = 0; i < recyclerViewDeleteItem.getChildCount(); i++) {
                        viewHolder = recyclerViewDeleteItem.findViewHolderForAdapterPosition(i);
                        if (viewHolder != null){
                            CheckBox box = viewHolder.itemView.findViewById(R.id.f_kaliservices_recyclerview_dialog_chkbox);
                            if (box.isChecked()){
                                selectedPosition.add(i);
                                selectedTargetIds.add(i+1);
                            }
                        }
                    }
                    if (!selectedPosition.isEmpty()) {
                        KaliServicesData.getInstance().deleteData(selectedPosition, selectedTargetIds, KaliServicesSQL.getInstance(context));
                        NhPaths.showMessage(context, "成功删除 " + selectedPosition.size() + " 个项目. ");
                        adDelete.dismiss();
                    } else {
                        NhPaths.showMessage(context, "未选择任何项目. ");
                    }
                });
            });
            adDelete.show();
        });
    }

    private void onMoveItemSetup(){
        moveButton.setOnClickListener(v -> {
            List<KaliServicesModel> kaliServicesModelList = KaliServicesData.getInstance().kaliServicesModelListFull;
            if (kaliServicesModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewMove = inflater.inflate(R.layout.kaliservices_move_dialog_view, null, false);
            final Spinner titlesBefore = promptViewMove.findViewById(R.id.f_kaliservices_move_adb_spr_titlesbefore);
            final Spinner titlesAfter = promptViewMove.findViewById(R.id.f_kaliservices_move_adb_spr_titlesafter);
            final Spinner actions = promptViewMove.findViewById(R.id.f_kaliservices_move_adb_spr_actions);

            ArrayList<String> serviceNameArrayList = new ArrayList<>();
            for (KaliServicesModel kaliServicesModel: kaliServicesModelList){
                serviceNameArrayList.add(kaliServicesModel.getServiceName());
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, serviceNameArrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            titlesBefore.setAdapter(arrayAdapter);
            titlesAfter.setAdapter(arrayAdapter);

            MaterialAlertDialogBuilder adbMove = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adbMove.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            adbMove.setPositiveButton("移动", (dialog, which) -> { });
            final AlertDialog adMove = adbMove.create();
            adMove.setView(promptViewMove);
            adMove.setCancelable(true);
            adMove.setOnShowListener(dialog -> {
                final Button buttonMove = adMove.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonMove.setOnClickListener(v1 -> {
                    int originalPositionIndex = titlesBefore.getSelectedItemPosition();
                    int targetPositionIndex = titlesAfter.getSelectedItemPosition();
                    if (originalPositionIndex == targetPositionIndex ||
                            (actions.getSelectedItemPosition() == 0 && targetPositionIndex == (originalPositionIndex + 1)) ||
                            (actions.getSelectedItemPosition() == 1 && targetPositionIndex == (originalPositionIndex - 1))) {
                        NhPaths.showMessage(context, "项目未移动, 因为目标位置与原始位置相同. ");
                    } else {
                        if (actions.getSelectedItemPosition() == 1) targetPositionIndex += 1;
                        KaliServicesData.getInstance().moveData(originalPositionIndex, targetPositionIndex, KaliServicesSQL.getInstance(context));
                        NhPaths.showMessage(context, "项目移动成功. ");
                        adMove.dismiss();
                    }
                });
            });
            adMove.show();
        });
    }
}