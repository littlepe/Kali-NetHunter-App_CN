package com.offsec.nethunter;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
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
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.offsec.nethunter.RecyclerViewAdapter.CustomCommandsRecyclerViewAdapter;
import com.offsec.nethunter.RecyclerViewAdapter.CustomCommandsRecyclerViewAdapterDeleteItems;
import com.offsec.nethunter.RecyclerViewData.CustomCommandsData;
import com.offsec.nethunter.SQL.CustomCommandsSQL;
import com.offsec.nethunter.models.CustomCommandsModel;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.viewmodels.CustomCommandsViewModel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class CustomCommandsFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    public static final String TAG = "CustomCommandsFragment";
    private CustomCommandsRecyclerViewAdapter customCommandsRecyclerViewAdapter;
    private Context context;
    private Activity activity;
    private Button addButton;
    private Button deleteButton;
    private Button moveButton;
    public static int targetPositionId;

    public CustomCommandsFragment() {
        Log.d(TAG, "CustomCommandsFragment: 初始化");
    }

    public static CustomCommandsFragment newInstance(int sectionNumber) {
        CustomCommandsFragment fragment = new CustomCommandsFragment();
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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.customcommands, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TextView customBanner;
        super.onViewCreated(view, savedInstanceState);
        CustomCommandsViewModel customCommandsViewModel = new ViewModelProvider(this).get(CustomCommandsViewModel.class);
        customCommandsViewModel.init(context);
        customCommandsViewModel.getLiveDataCustomCommandsModelList().observe(getViewLifecycleOwner(), customCommandsModelList -> {
            customCommandsRecyclerViewAdapter.notifyDataSetChanged();
        });

        customCommandsRecyclerViewAdapter = new CustomCommandsRecyclerViewAdapter(context, customCommandsViewModel.getLiveDataCustomCommandsModelList().getValue());
        RecyclerView recyclerView = view.findViewById(R.id.f_customcommands_recyclerview);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(customCommandsRecyclerViewAdapter);

        addButton = view.findViewById(R.id.f_customcommands_addItemButton);
        deleteButton = view.findViewById(R.id.f_customcommands_deleteItemButton);
        moveButton = view.findViewById(R.id.f_customcommands_moveItemButton);
        customBanner = view.findViewById(R.id.f_customcommands_banner);

        onAddItemSetup();
        onDeleteItemSetup();
        onMoveItemSetup();

        // WearOS 优化
        SharedPreferences sharedpreferences = activity.getSharedPreferences("com.offsec.nethunter", Context.MODE_PRIVATE);
        boolean iswatch = sharedpreferences.getBoolean("running_on_wearos", false);
        if (iswatch) {
            customBanner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.custom_commands, menu);
        final MenuItem searchItem = menu.findItem(R.id.f_customcommands_action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        // WearOS 优化
        boolean iswatch = requireActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
        if(iswatch) {
            searchItem.setVisible(false);
        }
        assert searchView != null;
        searchView.setOnSearchClickListener(v -> menu.setGroupVisible(R.id.f_customcommands_menu_group1, false));
        searchView.setOnCloseListener(() -> {
            menu.setGroupVisible(R.id.f_customcommands_menu_group1, true);
            return false;
        });
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { // 当按下搜索按钮时
                customCommandsRecyclerViewAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                customCommandsRecyclerViewAdapter.getFilter().filter(newText);
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View promptView = inflater.inflate(R.layout.customcommands_custom_dialog_view, null);
        final TextView titleTextView = promptView.findViewById(R.id.f_customcommands_adb_tv_title1);
        final EditText storedpathEditText = promptView.findViewById(R.id.f_customcommands_adb_et_storedpath);

        switch (item.getItemId()){
            case R.id.f_customcommands_menu_backupDB:
                titleTextView.setText("完整路径, 用于保存数据库: ");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentCustomCommands");
                MaterialAlertDialogBuilder adbBackup = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbBackup.setView(promptView);
                adbBackup.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbBackup.setPositiveButton("确定", (dialog, which) -> { });
                final AlertDialog adBackup = adbBackup.create();
                adBackup.setOnShowListener(dialog -> {
                    final Button buttonOK = adBackup.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = CustomCommandsData.getInstance().backupData(CustomCommandsSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null){
                            NhPaths.showMessage(context, "数据库已成功备份到 " + storedpathEditText.getText().toString());
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("备份数据库失败").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adBackup.show();
                break;
            default:
            case R.id.f_customcommands_menu_restoreDB:
                titleTextView.setText("完整路径, 用于恢复数据库: ");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentCustomCommands");
                MaterialAlertDialogBuilder adbRestore = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbRestore.setView(promptView);
                adbRestore.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbRestore.setPositiveButton("确定", (dialog, which) -> { });
                final AlertDialog adRestore = adbRestore.create();
                adRestore.setOnShowListener(dialog -> {
                    final Button buttonOK = adRestore.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = CustomCommandsData.getInstance().restoreData(CustomCommandsSQL.getInstance(context), storedpathEditText.getText().toString());
                        if (returnedResult == null) {
                            NhPaths.showMessage(context, "数据库已成功恢复到 " + storedpathEditText.getText().toString());
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("恢复数据库失败").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adRestore.show();
                break;
            case R.id.f_customcommands_menu_ResetToDefault:
                CustomCommandsData.getInstance().resetData(CustomCommandsSQL.getInstance(context));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        addButton = null;
        deleteButton = null;
        moveButton = null;
        customCommandsRecyclerViewAdapter = null;
    }

    private void onAddItemSetup() {
        addButton.setOnClickListener(v -> {
            List<CustomCommandsModel> customCommandsModelList = CustomCommandsData.getInstance().customCommandsModelListFull;
            if (customCommandsModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewAdd = inflater.inflate(R.layout.customcommands_add_dialog_view, null);
            final EditText commandLabelEditText = promptViewAdd.findViewById(R.id.f_customcommands_add_adb_et_label);
            final EditText commandEditText = promptViewAdd.findViewById(R.id.f_customcommands_add_adb_et_command);
            final Spinner sendToSpinner = promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_sendto);
            final Spinner execModeSpinner = promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_execmode);
            final CheckBox runOnBootCheckbox = promptViewAdd.findViewById(R.id.f_customcommands_add_adb_checkbox_runonboot);
            final Spinner insertPositions = promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_positions);
            final Spinner insertLabels = promptViewAdd.findViewById(R.id.f_customcommands_add_adb_spr_labels);

            ArrayList<String> commandLabelArrayList = new ArrayList<>();
            for (CustomCommandsModel customCommandsModel: customCommandsModelList){
                commandLabelArrayList.add(customCommandsModel.getCommandLabel());
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, commandLabelArrayList);
            arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

            insertPositions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    // 如果插入到顶部
                    if (position == 0) {
                        insertLabels.setVisibility(View.INVISIBLE);
                        targetPositionId = 1;
                        // 如果插入到底部
                    } else if (position == 1) {
                        insertLabels.setVisibility(View.INVISIBLE);
                        targetPositionId = customCommandsModelList.size() + 1;
                        // 如果插入到某一项之前
                    } else if (position == 2) {
                        insertLabels.setVisibility(View.VISIBLE);
                        insertLabels.setAdapter(arrayAdapter);
                        insertLabels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                targetPositionId = position + 1;
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                                Log.d(TAG, "onNothingSelected: 未选择任何项");
                            }
                        });
                        // 如果插入到某一项之后
                    } else {
                        insertLabels.setVisibility(View.VISIBLE);
                        insertLabels.setAdapter(arrayAdapter);
                        insertLabels.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                targetPositionId = position + 2;
                            }
                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                                Log.d(TAG, "onNothingSelected: 未选择任何项");
                            }
                        });
                    }
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    Log.d(TAG, "onNothingSelected: 未选择任何项");
                }
            });

            MaterialAlertDialogBuilder adbAdd = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adbAdd.setPositiveButton("确定", (dialog, which) -> { });
            final AlertDialog adAdd = adbAdd.create();
            adAdd.setView(promptViewAdd);
            adAdd.setCancelable(true);
            // 如果希望点击确定后对话框保持打开状态, 需要这样处理...
            adAdd.setOnShowListener(dialog -> {
                final Button buttonAdd = adAdd.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonAdd.setOnClickListener(v1 -> {
                    if (commandLabelEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "标签不能为空");
                    } else if (commandEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "命令字符串不能为空");
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(commandLabelEditText.getText().toString());
                        dataArrayList.add(commandEditText.getText().toString());
                        dataArrayList.add(sendToSpinner.getSelectedItem().toString());
                        dataArrayList.add(execModeSpinner.getSelectedItem().toString());
                        dataArrayList.add(runOnBootCheckbox.isChecked()?"1":"0");
                        CustomCommandsData.getInstance().addData(targetPositionId, dataArrayList, CustomCommandsSQL.getInstance(context));
                        adAdd.dismiss();
                    }
                });
            });
            adAdd.show();
        });
    }

    private void onDeleteItemSetup() {
        deleteButton.setOnClickListener(v -> {
            List<CustomCommandsModel> customCommandsModelList = CustomCommandsData.getInstance().customCommandsModelListFull;
            if (customCommandsModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewDelete = inflater.inflate(R.layout.customcommands_delete_dialog_view, null, false);
            final RecyclerView recyclerViewDeleteItem = promptViewDelete.findViewById(R.id.f_customcommands_delete_recyclerview);
            CustomCommandsRecyclerViewAdapterDeleteItems customCommandsRecyclerViewAdapterDeleteItems = new CustomCommandsRecyclerViewAdapterDeleteItems(context, customCommandsModelList);

            LinearLayoutManager linearLayoutManagerDelete = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
            recyclerViewDeleteItem.setLayoutManager(linearLayoutManagerDelete);
            recyclerViewDeleteItem.setAdapter(customCommandsRecyclerViewAdapterDeleteItems);

            MaterialAlertDialogBuilder adbDelete = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
            adbDelete.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
            adbDelete.setPositiveButton("删除", (dialog, which) -> { });
            final AlertDialog adDelete = adbDelete.create();
            adDelete.setMessage("请选择要删除的项: ");
            adDelete.setView(promptViewDelete);
            adDelete.setCancelable(true);
            // 如果希望点击确定后对话框保持打开状态, 需要这样处理...
            adDelete.setOnShowListener(dialog -> {
                final Button buttonDelete = adDelete.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonDelete.setOnClickListener(v1 -> {
                    RecyclerView.ViewHolder viewHolder;
                    ArrayList<Integer> selectedPosition = new ArrayList<>();
                    ArrayList<Integer> selectedTargetIds = new ArrayList<>();
                    for (int i = 0; i < recyclerViewDeleteItem.getChildCount(); i++) {
                        viewHolder = recyclerViewDeleteItem.findViewHolderForAdapterPosition(i);
                        if (viewHolder != null){
                            CheckBox box = viewHolder.itemView.findViewById(R.id.f_customcommands_recyclerview_dialog_chkbox);
                            if (box.isChecked()){
                                selectedPosition.add(i);
                                selectedTargetIds.add(i+1);
                            }
                        }
                    }
                    if (!selectedPosition.isEmpty()) {
                        CustomCommandsData.getInstance().deleteData(selectedPosition, selectedTargetIds, CustomCommandsSQL.getInstance(context));
                        NhPaths.showMessage(context, "成功删除 " + selectedPosition.size() + " 项. ");
                        adDelete.dismiss();
                    } else {
                        NhPaths.showMessage(context, "没有可删除的项. ");
                    }
                });
            });
            adDelete.show();
        });
    }

    private void onMoveItemSetup() {
        moveButton.setOnClickListener(v -> {
            List<CustomCommandsModel> customCommandsModelList = CustomCommandsData.getInstance().customCommandsModelListFull;
            if (customCommandsModelList == null) return;
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewMove = inflater.inflate(R.layout.customcommands_move_dialog_view, null, false);
            final Spinner titlesBefore = promptViewMove.findViewById(R.id.f_customcommands_move_adb_spr_labelsbefore);
            final Spinner titlesAfter = promptViewMove.findViewById(R.id.f_customcommands_move_adb_spr_labelsafter);
            final Spinner actions = promptViewMove.findViewById(R.id.f_customcommands_move_adb_spr_actions);

            ArrayList<String> commandLabelArrayList = new ArrayList<>();
            for (CustomCommandsModel customCommandsModel: customCommandsModelList){
                commandLabelArrayList.add(customCommandsModel.getCommandLabel());
            }

            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, commandLabelArrayList);
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
                        NhPaths.showMessage(context, "您将项目移动到了相同的位置, 无需移动. ");
                    } else {
                        if (actions.getSelectedItemPosition() == 1) targetPositionIndex += 1;
                        CustomCommandsData.getInstance().moveData(originalPositionIndex, targetPositionIndex, CustomCommandsSQL.getInstance(context));
                        NhPaths.showMessage(context, "项目已成功移动. ");
                        adMove.dismiss();
                    }
                });
            });
            adMove.show();
        });
    }
}