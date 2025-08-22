package com.offsec.nethunter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.offsec.nethunter.HandlerThread.USBArsenalHandlerThread;
import com.offsec.nethunter.SQL.USBArsenalSQL;
import com.offsec.nethunter.bridge.Bridge;
import com.offsec.nethunter.models.USBArsenalUSBNetworkModel;
import com.offsec.nethunter.models.USBArsenalUSBSwitchModel;
import com.offsec.nethunter.utils.NhPaths;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class USBArsenalFragment extends Fragment {
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "USBArsenalFragment";
    private Context context;
    private Activity activity;
    private final USBArsenalHandlerThread usbArsenalHandlerThread = new USBArsenalHandlerThread();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static boolean is_init_exists = true;
    //private Message msg = new Message();
    /** 当前 USB 状态提示 */
    private TextView usbStatusTextView;
    /** 已挂载镜像提示 */
    private TextView mountedImageTextView;
    /** 镜像挂载提示 */
    private TextView mountedImageHintTextView;
    /** 当前查询字符串提示 */
    private TextView currentInquiryHintTextView;
    /** USB 网络共享提示 */
    private TextView usbNetworkTetheringHintTextView;
    private ImageButton reloadUSBStateImageButton;
    private ImageButton reloadMountStateButton;
    private LinearLayout imageMounterLL;
    private LinearLayout usbNetworkTetheringLL;
    private Button setUSBIfaceButton;
    private Button mountImgButton;
    private Button unmountImgButton;
    private Button setUSBNetworkTetheringButton;
    private Button saveUSBFunctionConfigButton;
    private Button saveUSBNetworkTetheringConfigButton;
    private Spinner targetOSSpinner;
    private Spinner imgFileSpinner;
    private Spinner usbFuncSpinner;
    private Spinner usbNetworkAttackModeSpinner;
    private Spinner adbSpinner;
    private EditText inquiryStringEditText;
    private EditText[] usbSwitchInfoEditTextGroup = new EditText[5];
    private EditText[] usbNetworkInfoEditTextGroup = new EditText[5];
    private String usbStorageFunctionName;

    public static USBArsenalFragment newInstance(int sectionNumber) {
        USBArsenalFragment fragment = new USBArsenalFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context     = getContext();
        activity    = getActivity();
        usbArsenalHandlerThread.start();
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.usbarsenal, container, false);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        targetOSSpinner                     = view.findViewById(R.id.f_usbarsenal_spr_targetplatform);
        usbFuncSpinner                      = view.findViewById(R.id.f_usbarsenal_spr_usbfunctions);
        adbSpinner                          = view.findViewById(R.id.f_usbarsenal_spr_adb);
        imgFileSpinner                      = view.findViewById(R.id.f_usbarsenal_spr_img_files);
        usbNetworkAttackModeSpinner         = view.findViewById(R.id.f_usbarsenal_spr_networkattackmode);
        setUSBIfaceButton                   = view.findViewById(R.id.f_usbarsenal_btn_setusbinterface);
        setUSBNetworkTetheringButton        = view.findViewById(R.id.f_usbarsenal_btn_runusbnetworktethering);
        mountImgButton                      = view.findViewById(R.id.f_usbarsenal_btn_mountImage);
        unmountImgButton                    = view.findViewById(R.id.f_usbarsenal_btn_unmountImage);
        Button      changeInquiryButton     = view.findViewById(R.id.f_usbarsenal_btn_changeInquiryString);
        reloadUSBStateImageButton           = view.findViewById(R.id.f_usbarsenal_imgbtn_reloadUSBStatus);
        reloadMountStateButton              = view.findViewById(R.id.f_usbarsenal_imgbtn_reloadMountStatus);
        saveUSBFunctionConfigButton         = view.findViewById(R.id.f_usbarsenal_btn_saveusbfuncswitch);
        saveUSBNetworkTetheringConfigButton = view.findViewById(R.id.f_usbarsenal_btn_saveusbnetworktethering);
        CheckBox    readOnlyCheckBox        = view.findViewById(R.id.f_usbarsenal_chkbox_ReadOrWrite);
        usbStatusTextView                   = view.findViewById(R.id.f_usbarsenal_tv_current_usb_state);
        mountedImageTextView                = view.findViewById(R.id.f_usbarsenal_tv_mount_state);
        currentInquiryHintTextView          = view.findViewById(R.id.f_usbarsenal_tv_current_inquiry_string);
        mountedImageHintTextView            = view.findViewById(R.id.f_usbarsenal_ll_tv_imagemounter_hint);
        usbNetworkTetheringHintTextView     = view.findViewById(R.id.f_usbarsenal_ll_tv_usbnetworktethering_hint);
        imageMounterLL                      = view.findViewById(R.id.f_usbarsenal_ll_imageMounter_sub2);
        usbNetworkTetheringLL               = view.findViewById(R.id.f_usbarsenal_ll_usbnetworktethering_sub2);
        inquiryStringEditText               = view.findViewById(R.id.f_usbarsenal_et_inquiryString);

        usbSwitchInfoEditTextGroup[0] = view.findViewById(R.id.f_usbarsenal_et_idvendor);
        usbSwitchInfoEditTextGroup[1] = view.findViewById(R.id.f_usbarsenal_et_idproduct);
        usbSwitchInfoEditTextGroup[2] = view.findViewById(R.id.f_usbarsenal_et_manufacturer);
        usbSwitchInfoEditTextGroup[3] = view.findViewById(R.id.f_usbarsenal_et_product);
        usbSwitchInfoEditTextGroup[4] = view.findViewById(R.id.f_usbarsenal_et_serialnumber);
        usbNetworkInfoEditTextGroup[0] = view.findViewById(R.id.f_usbarsenal_et_usbnetwork_upstreamiface);
        usbNetworkInfoEditTextGroup[1] = view.findViewById(R.id.f_usbarsenal_et_usbnetwork_usbfunc);
        usbNetworkInfoEditTextGroup[2] = view.findViewById(R.id.f_usbarsenal_et_usbnetwork_targetip);
        usbNetworkInfoEditTextGroup[3] = view.findViewById(R.id.f_usbarsenal_et_usbnetwork_gatewayip);
        usbNetworkInfoEditTextGroup[4] = view.findViewById(R.id.f_usbarsenal_et_usbnetwork_ipsubnetmask);

        /* 检查 /init.nethunter.rc 是否存在 */
        { Message msg = new Message();
            msg.what = USBArsenalHandlerThread.IS_INIT_EXIST;
            msg.obj = "[ -f /init.nethunter.rc ]";
            usbArsenalHandlerThread.getHandler().sendMessage(msg); }

        /* 获取存储功能目录名 */
        { Message msg = new Message();
            msg.what = USBArsenalHandlerThread.GET_STORAGE_FUNC_FOLDER_NAME;
            msg.obj = "find /config/usb_gadget/g1/functions/ -name \"mass_storage.*\" -maxdepth 1 -type d -exec basename {} \\; | head -n1";
            usbArsenalHandlerThread.getHandler().sendMessage(msg); }

        ArrayAdapter<String> usbFuncWinArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, new ArrayList<>());
        ArrayAdapter<String> usbFuncMACArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, new ArrayList<>());
        /* USB 网络攻击模式下拉框 */
        ArrayAdapter<String> usbNetworkAttackModeArrayAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.usbarsenal_usb_network_attack_mode));
        usbFuncWinArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        usbFuncMACArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        usbNetworkAttackModeArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        targetOSSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 2){
                    usbFuncSpinner.setAdapter(usbFuncMACArrayAdapter);
                } else {
                    usbFuncSpinner.setAdapter(usbFuncWinArrayAdapter);
                }
                refreshUSBSwitchInfos(gettargetOSSpinnerString(), getusbFuncSpinnerString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        usbFuncSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0 || is_init_exists) {
                    adbSpinner.setSelection(1);
                    adbSpinner.setEnabled(false);
                } else {
                    adbSpinner.setSelection(0);
                    adbSpinner.setEnabled(true);
                }
                refreshUSBSwitchInfos(gettargetOSSpinnerString(), getusbFuncSpinnerString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        adbSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshUSBSwitchInfos(gettargetOSSpinnerString(), getusbFuncSpinnerString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        usbNetworkAttackModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshUSBNetworkInfos(getusbNetWorkModeSpinnerPosition());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        /* 设置 USB 接口按钮 */
        setUSBIfaceButton.setOnClickListener(v -> {
            if (isAllUSBInfosValid()) {
                setUSBIfaceButton.setEnabled(false);
                String target = (targetOSSpinner.getSelectedItem() != null && "Windows".equals(targetOSSpinner.getSelectedItem().toString())) ? "win" :
                        (targetOSSpinner.getSelectedItem() != null && "Linux".equals(targetOSSpinner.getSelectedItem().toString())) ? "lnx" :
                                (targetOSSpinner.getSelectedItem() != null && "Mac OS".equals(targetOSSpinner.getSelectedItem().toString())) ? "mac" : "";
                String functions = usbFuncSpinner.getSelectedItem() != null ? usbFuncSpinner.getSelectedItem().toString() : "";
                String adbEnable = (adbSpinner.getSelectedItem() != null && "Enable".equals(adbSpinner.getSelectedItem().toString())) ? ",adb" : "";
                String idVendor = " -v '" + usbSwitchInfoEditTextGroup[0].getText().toString() + "'";
                String idProduct = " -p '" + usbSwitchInfoEditTextGroup[1].getText().toString() + "'";
                String manufacturer = usbSwitchInfoEditTextGroup[2].getText().toString().isEmpty() ? "" : " -m '" + usbSwitchInfoEditTextGroup[2].getText().toString() + "'";
                String product = usbSwitchInfoEditTextGroup[3].getText().toString().isEmpty() ? "" : " -P '" + usbSwitchInfoEditTextGroup[3].getText().toString() + "'";
                String serialnumber = usbSwitchInfoEditTextGroup[4].getText().toString().isEmpty() ? "" : " -s '" + usbSwitchInfoEditTextGroup[4].getText().toString() + "'";

                String commandBuilder = "[ -f /init.nethunter.rc ] && setprop sys.usb.config " +
                        functions +
                        " || " +
                        NhPaths.APP_SCRIPTS_PATH +
                        "/usbarsenal -t '" +
                        target +
                        "' -f '" +
                        functions +
                        "'" +
                        adbEnable +
                        idVendor +
                        idProduct +
                        manufacturer +
                        product +
                        serialnumber;
                Message msg = new Message();
                msg.what = USBArsenalHandlerThread.SETUSBIFACE;
                msg.obj = commandBuilder;
                usbArsenalHandlerThread.getHandler().sendMessage(msg);
            }
        });

        /* 启动 USB 网络共享按钮 */
        setUSBNetworkTetheringButton.setOnClickListener(v -> run_cmd_android("usbtethering -o " + usbNetworkInfoEditTextGroup[0].getText().toString() +
                " -i " + usbNetworkInfoEditTextGroup[1].getText().toString() +
                " -A " + usbNetworkInfoEditTextGroup[2].getText().toString() +
                " -B " + usbNetworkInfoEditTextGroup[2].getText().toString() +
                " -C " + usbNetworkInfoEditTextGroup[3].getText().toString() +
                " -D " + usbNetworkInfoEditTextGroup[4].getText().toString()));

        /* 重新加载 USB 状态按钮 */
        reloadUSBStateImageButton.setOnClickListener(v -> {
            Message msg = new Message();
            msg.what = USBArsenalHandlerThread.RELOAD_USBIFACE;
            msg.obj = "find /config/usb_gadget/g1/configs/b.1 -type l -exec readlink -e {} \\; | xargs echo";
            usbArsenalHandlerThread.getHandler().sendMessage(msg);
        });

        /* 重新加载挂载状态按钮 */
        reloadMountStateButton.setOnClickListener(v -> {
            Message msg = new Message();
            msg.what = USBArsenalHandlerThread.RELOAD_MOUNTSTATUS;
            msg.obj = "cat /config/usb_gadget/g1/functions/" + usbStorageFunctionName + "/lun.0/file";
            usbArsenalHandlerThread.getHandler().sendMessage(msg);
            getImageFiles();
        });

        /* 挂载镜像按钮 */
        mountImgButton.setOnClickListener(v -> {
            if (imgFileSpinner.getSelectedItem() == null) {
                NhPaths.showMessage(context, "未选择镜像文件. ");
            } else {
                mountImgButton.setEnabled(false);
                unmountImgButton.setEnabled(false);
                Message msg = new Message();
                msg.what = USBArsenalHandlerThread.MOUNT_IMAGE;
                if (readOnlyCheckBox.isChecked())
                    msg.obj = String.format("%s%s && echo '%s/%s' > /config/usb_gadget/g1/functions/" + usbStorageFunctionName + "/lun.0/file",
                            "echo '1' > /config/usb_gadget/g1/functions/" + usbStorageFunctionName + "/lun.0/ro",
                            imgFileSpinner.getSelectedItem().toString().contains(".iso") ? " && echo '1' > /config/usb_gadget/g1/functions/" + usbStorageFunctionName + "/lun.0/cdrom" : " && echo '0' > /config/usb_gadget/g1/functions/mass_storage.gs6/lun.0/cdrom",
                            NhPaths.APP_SD_FILES_IMG_PATH,
                            imgFileSpinner.getSelectedItem().toString());
                else
                    msg.obj = String.format("%s%s && echo '%s/%s' > /config/usb_gadget/g1/functions/" + usbStorageFunctionName + "/lun.0/file",
                            "echo '0' > /config/usb_gadget/g1/functions/" + usbStorageFunctionName + "/lun.0/ro",
                            imgFileSpinner.getSelectedItem().toString().contains(".iso") ? " && echo '1' > /config/usb_gadget/g1/functions/" + usbStorageFunctionName + "/lun.0/cdrom" : " && echo '0' > /config/usb_gadget/g1/functions/mass_storage.gs6/lun.0/cdrom",
                            NhPaths.APP_SD_FILES_IMG_PATH,
                            imgFileSpinner.getSelectedItem().toString());
                usbArsenalHandlerThread.getHandler().sendMessage(msg);
            }
        });

        /* 卸载镜像按钮 */
        unmountImgButton.setOnClickListener(v -> {
            mountImgButton.setEnabled(false);
            unmountImgButton.setEnabled(false);
            Message msg = new Message();
            msg.what = USBArsenalHandlerThread.UNMOUNT_IMAGE;
            msg.obj = String.format("echo '' > /config/usb_gadget/g1/functions/%s/lun.0/file" +
                            " && echo '0' > /config/usb_gadget/g1/functions/%s/lun.0/ro" +
                            " && echo '0' > /config/usb_gadget/g1/functions/%s/lun.0/cdrom",
                    usbStorageFunctionName, usbStorageFunctionName, usbStorageFunctionName);
            usbArsenalHandlerThread.getHandler().sendMessage(msg);
        });

        /* 修改查询字符串按钮 */
        changeInquiryButton.setOnClickListener(v -> {
            Message msg = new Message();
            msg.what = USBArsenalHandlerThread.CHANGE_INQUIRY_STRING;
            msg.obj = String.format("echo '%s' > /config/usb_gadget/g1/functions/%s/lun.0/inquiry_string", inquiryStringEditText.getText().toString(), usbStorageFunctionName);
            usbArsenalHandlerThread.getHandler().sendMessage(msg);
        });

        /* 保存 USB 功能配置按钮 */
        saveUSBFunctionConfigButton.setOnClickListener(v -> {
            if (!usbSwitchInfoEditTextGroup[0].getText().toString().matches("0x[0-9a-fA-F]{4}") ||
                    !usbSwitchInfoEditTextGroup[1].getText().toString().matches("0x[0-9a-fA-F]{4}")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 0x[0-9a-fA-F]{4}").create().show();
            } else if (!usbSwitchInfoEditTextGroup[2].getText().toString().matches("\\w+|^$") ||
                    !usbSwitchInfoEditTextGroup[3].getText().toString().matches("\\w+|^$")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 \\w*|^$").create().show();
            } else if (!usbSwitchInfoEditTextGroup[4].getText().toString().matches("[0-9A-Z]{10}|^$")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 [0-9A-Z]{10}|^$").create().show();
            } else {
                for (int i = 0; i < usbSwitchInfoEditTextGroup.length; i++) {
                    if (!USBArsenalSQL.getInstance(context).setUSBSwitchColumnData(
                            getusbFuncSpinnerString(),
                            i + 2,
                            targetOSSpinner.getSelectedItem().toString(),
                            usbSwitchInfoEditTextGroup[i].getText().toString().toLowerCase())){
                        NhPaths.showMessage(context, "处理 " + usbSwitchInfoEditTextGroup[i].getText().toString().toLowerCase() + " 时出错");
                    } else {
                        NhPaths.showMessage(context, "已保存. ");
                    }
                }
            }
        });

        /* 保存 USB 网络配置按钮 */
        saveUSBNetworkTetheringConfigButton.setOnClickListener(v -> {
            USBArsenalUSBNetworkModel usbArsenalUSBNetworkModel = new USBArsenalUSBNetworkModel(
                    usbNetworkInfoEditTextGroup[0].getText().toString(),
                    usbNetworkInfoEditTextGroup[1].getText().toString(),
                    usbNetworkInfoEditTextGroup[2].getText().toString(),
                    usbNetworkInfoEditTextGroup[3].getText().toString(),
                    usbNetworkInfoEditTextGroup[4].getText().toString()
            );
            if(!USBArsenalSQL.getInstance(context).setUSBNetworkColumnData(getusbNetWorkModeSpinnerPosition(), usbArsenalUSBNetworkModel)){
                NhPaths.showMessage(context, "保存配置到数据库失败, 请检查输入. ");
            } else {
                NhPaths.showMessage(context, "已保存. ");
            }
        });

        usbArsenalHandlerThread.setOnShellExecuterFinishedListener((resultObject, actionCode) -> {
            if (getView() != null) {
                switch (actionCode){
                    case USBArsenalHandlerThread.IS_INIT_EXIST:
                        if ((int)resultObject == 0) {
                            is_init_exists = true;
                            /* 读取 USB 功能列表 */
                            { Message msg = new Message();
                                msg.what = USBArsenalHandlerThread.RETRIEVE_USB_FUNCS;
                                msg.obj = "cat /init.nethunter.rc | grep -E -o 'sys.usb.config=([a-zA-Z,_]+)' | sed 's/sys.usb.config=//' | sort | uniq";
                                usbArsenalHandlerThread.getHandler().sendMessage(msg); }
                        } else {
                            is_init_exists = false;
                            uiHandler.post(() -> {
                                usbFuncWinArrayAdapter.clear();
                                usbFuncWinArrayAdapter.addAll(getResources().getStringArray(R.array.usbarsenal_usb_states_win_lin));
                                usbFuncMACArrayAdapter.clear();
                                usbFuncMACArrayAdapter.addAll(getResources().getStringArray(R.array.usbarsenal_usb_states_mac));
                            });
                        }
                        break;
                    case USBArsenalHandlerThread.RETRIEVE_USB_FUNCS:
                        uiHandler.post(() -> {
                            ArrayList<String> usbFuncArray = new ArrayList<>(Arrays.asList(resultObject.toString().split("\\n")));
                            List<String> usbFuncWinArray = Lists.newArrayList(Collections2.filter(usbFuncArray,
                                    Predicates.containsPattern("win")));
                            List<String> usbFuncMacArray = Lists.newArrayList(Collections2.filter(usbFuncArray,
                                    Predicates.containsPattern("mac")));
                            usbFuncWinArrayAdapter.clear();
                            usbFuncWinArrayAdapter.addAll(usbFuncWinArray);
                            usbFuncMACArrayAdapter.clear();
                            usbFuncMACArrayAdapter.addAll(usbFuncMacArray);
                            for (EditText infoEditText: usbSwitchInfoEditTextGroup) {
                                infoEditText.setEnabled(false);
                            }
                            saveUSBFunctionConfigButton.setEnabled(false);
                            adbSpinner.setEnabled(false);
                        });
                        break;
                    case USBArsenalHandlerThread.SETUSBIFACE:
                        uiHandler.post(() -> {
                            if ((int)resultObject != 0){
                                NhPaths.showMessage(context, "设置 USB 功能失败. ");
                            } else {
                                NhPaths.showMessage(context, "USB 功能设置成功. ");
                                reloadUSBStateImageButton.performClick();
                            }
                            setUSBIfaceButton.setEnabled(true);
                        });
                        break;
                    case USBArsenalHandlerThread.RELOAD_USBIFACE:
                        uiHandler.post(() -> {
                            if (resultObject.toString().isEmpty()) {
                                usbStatusTextView.setText("未启用任何 USB 功能");
                                imageMounterLL.setVisibility(View.GONE);
                                mountedImageHintTextView.setVisibility(View.VISIBLE);
                            } else {
                                usbStatusTextView.setText(resultObject.toString()
                                        .replaceAll("/config/usb_gadget/g1/functions/", "")
                                        .replaceAll("/config/usb_gadget/g1/functions","gsi.rndis")
                                        .replaceAll(" ", "\n"));
                                if (usbStatusTextView.getText().toString().contains("mass_storage")){
                                    imageMounterLL.setVisibility(View.VISIBLE);
                                    mountedImageHintTextView.setVisibility(View.GONE);
                                    getImageFiles();
                                } else {
                                    imageMounterLL.setVisibility(View.GONE);
                                    mountedImageHintTextView.setVisibility(View.VISIBLE);
                                }
                                if (usbStatusTextView.getText().toString().contains("rndis") ||
                                        usbStatusTextView.getText().toString().contains("acm")){
                                    usbNetworkTetheringLL.setVisibility(View.VISIBLE);
                                    usbNetworkTetheringHintTextView.setVisibility(View.GONE);
                                    refreshUSBNetworkInfos(getusbNetWorkModeSpinnerPosition());
                                } else {
                                    usbNetworkTetheringLL.setVisibility(View.GONE);
                                    usbNetworkTetheringHintTextView.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                        break;
                    case USBArsenalHandlerThread.GET_STORAGE_FUNC_FOLDER_NAME:
                        String folder_name = resultObject.toString().split("\\n")[0];
                        /* 若未找到则回退到 mass_storage.0 */
                        if (folder_name.isEmpty()) folder_name = "mass_storage.0";
                        usbStorageFunctionName = folder_name;
                        break;
                    case USBArsenalHandlerThread.RELOAD_MOUNTSTATUS:
                        uiHandler.post(() -> {
                            if (resultObject.toString().isEmpty()){ mountedImageTextView.setText("未挂载镜像. "); }
                            else {mountedImageTextView.setText(resultObject.toString());}
                        });
                        break;
                    case USBArsenalHandlerThread.MOUNT_IMAGE:
                        uiHandler.post(() -> {
                            if ((int)resultObject == 0){
                                NhPaths.showMessage(context, imgFileSpinner.getSelectedItem().toString() + " 已挂载. ");
                            } else {
                                NhPaths.showMessage(context, "挂载镜像 " + imgFileSpinner.getSelectedItem().toString() + " 失败. ");
                            }
                            reloadMountStateButton.performClick();
                            mountImgButton.setEnabled(true);
                            unmountImgButton.setEnabled(true);
                        });
                        break;
                    case USBArsenalHandlerThread.UNMOUNT_IMAGE:
                        uiHandler.post(() -> {
                            if ((int)resultObject == 0){
                                NhPaths.showMessage(context, imgFileSpinner.getSelectedItem().toString() + " 已卸载. ");
                                reloadMountStateButton.performClick();
                            } else {
                                NhPaths.showMessage_long(context, "卸载镜像 " + imgFileSpinner.getSelectedItem().toString() +
                                        " 失败, 可能仍在被主机使用, 请先安全弹出, 然后重试. ");
                            }
                            reloadMountStateButton.performClick();
                            mountImgButton.setEnabled(true);
                            unmountImgButton.setEnabled(true);
                        });
                        break;
                    case USBArsenalHandlerThread.CHANGE_INQUIRY_STRING:
                        String newInquiry = inquiryStringEditText.getText().toString();
                        if ((int)resultObject == 0){
                            if (newInquiry.isEmpty()) {
                                NhPaths.showMessage(context, "查询字符串已恢复默认. ");
                                currentInquiryHintTextView.setText("Linux File-CD/Stor gadget（内核默认）");
                            } else {
                                NhPaths.showMessage(context, "查询字符串已更改为: “" + newInquiry + "”");
                                currentInquiryHintTextView.setText(newInquiry);
                            }
                        } else {
                            NhPaths.showMessage_long(context, "更改查询字符串失败. ");
                        }
                        break;
                    case USBArsenalHandlerThread.GET_USBSWITCH_SQL_DATA:
                        uiHandler.post(() -> {
                            usbSwitchInfoEditTextGroup[0].setText(((USBArsenalUSBSwitchModel)resultObject).getidVendor());
                            usbSwitchInfoEditTextGroup[1].setText(((USBArsenalUSBSwitchModel)resultObject).getidProduct());
                            usbSwitchInfoEditTextGroup[2].setText(((USBArsenalUSBSwitchModel)resultObject).getmanufacturer());
                            usbSwitchInfoEditTextGroup[3].setText(((USBArsenalUSBSwitchModel)resultObject).getproduct());
                            usbSwitchInfoEditTextGroup[4].setText(((USBArsenalUSBSwitchModel)resultObject).getserialnumber());
                        });
                        break;
                    case USBArsenalHandlerThread.GET_USBNETWORK_SQL_DATA:
                        uiHandler.post(() -> {
                            usbNetworkInfoEditTextGroup[0].setText(((USBArsenalUSBNetworkModel)resultObject).getupstream_iface());
                            usbNetworkInfoEditTextGroup[1].setText(((USBArsenalUSBNetworkModel)resultObject).getusb_iface());
                            usbNetworkInfoEditTextGroup[2].setText(((USBArsenalUSBNetworkModel)resultObject).getip_address_for_target());
                            usbNetworkInfoEditTextGroup[3].setText(((USBArsenalUSBNetworkModel)resultObject).getip_gateway());
                            usbNetworkInfoEditTextGroup[4].setText(((USBArsenalUSBNetworkModel)resultObject).getip_subnetmask());
                        });
                        break;
                }
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        reloadUSBStateImageButton.performClick();
        if (imageMounterLL.getVisibility() == View.VISIBLE) {
            reloadMountStateButton.performClick();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.usbarsenal, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @SuppressLint({"NonConstantResourceId", "SetTextI18n"})
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        assert inflater != null;
        final View promptView = inflater.inflate(R.layout.kaliservices_custom_dialog_view, null);
        final TextView titleTextView = promptView.findViewById(R.id.f_kaliservices_adb_tv_title1);
        final EditText storedpathEditText = promptView.findViewById(R.id.f_kaliservices_adb_et_storedpath);

        switch (item.getItemId()){
            case R.id.f_usbarsenal_menu_backupDB:
                titleTextView.setText("请输入备份数据库的完整路径: ");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentUSBArsenal");
                MaterialAlertDialogBuilder adbBackup = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbBackup.setView(promptView);
                adbBackup.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbBackup.setPositiveButton("确定", (dialog, which) -> { });
                final AlertDialog adBackup = adbBackup.create();
                adBackup.setOnShowListener(dialog -> {
                    final Button buttonOK = adBackup.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = USBArsenalSQL.getInstance(context).backupData(storedpathEditText.getText().toString());
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
            case R.id.f_usbarsenal_menu_restoreDB:
                titleTextView.setText("请输入要还原的数据库文件完整路径: ");
                storedpathEditText.setText(NhPaths.APP_SD_SQLBACKUP_PATH + "/FragmentUSBArsenal");
                MaterialAlertDialogBuilder adbRestore = new MaterialAlertDialogBuilder(activity, R.style.DialogStyleCompat);
                adbRestore.setView(promptView);
                adbRestore.setNegativeButton("取消", (dialog, which) -> dialog.cancel());
                adbRestore.setPositiveButton("确定", (dialog, which) -> { });
                final AlertDialog adRestore = adbRestore.create();
                adRestore.setOnShowListener(dialog -> {
                    final Button buttonOK = adRestore.getButton(DialogInterface.BUTTON_POSITIVE);
                    buttonOK.setOnClickListener(v -> {
                        String returnedResult = USBArsenalSQL.getInstance(context).restoreData(storedpathEditText.getText().toString());
                        if (returnedResult == null) {
                            NhPaths.showMessage(context, "数据库已成功从 " + storedpathEditText.getText().toString() + " 还原. ");
                            refreshUSBSwitchInfos(gettargetOSSpinnerString(), getusbFuncSpinnerString());
                            refreshUSBNetworkInfos(getusbNetWorkModeSpinnerPosition());
                        } else {
                            dialog.dismiss();
                            new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("还原失败").setMessage(returnedResult).create().show();
                        }
                        dialog.dismiss();
                    });
                });
                adRestore.show();
                break;
            case R.id.f_usbarsenal_menu_ResetToDefault:
                if (USBArsenalSQL.getInstance(context).resetData()) {
                    NhPaths.showMessage(context, "数据库已重置为默认. ");
                    refreshUSBSwitchInfos(gettargetOSSpinnerString(), getusbFuncSpinnerString());
                    refreshUSBNetworkInfos(getusbNetWorkModeSpinnerPosition());
                } else {
                    NhPaths.showMessage_long(context, "重置数据库失败. ");
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        usbStatusTextView = null;
        mountedImageTextView = null;
        mountedImageHintTextView = null;
        usbNetworkTetheringHintTextView = null;
        reloadUSBStateImageButton = null;
        reloadMountStateButton = null;
        imageMounterLL = null;
        usbNetworkTetheringLL = null;
        setUSBIfaceButton = null;
        mountImgButton = null;
        unmountImgButton = null;
        setUSBNetworkTetheringButton = null;
        saveUSBFunctionConfigButton = null;
        saveUSBNetworkTetheringConfigButton = null;
        targetOSSpinner = null;
        imgFileSpinner = null;
        usbFuncSpinner = null;
        usbNetworkAttackModeSpinner = null;
        adbSpinner = null;
        Arrays.fill(usbSwitchInfoEditTextGroup, null);
        usbSwitchInfoEditTextGroup = null;
        Arrays.fill(usbNetworkInfoEditTextGroup, null);
        usbNetworkInfoEditTextGroup = null;
    }

    /* 获取镜像文件列表 */
    private void getImageFiles() {
        mountImgButton.setEnabled(false);
        unmountImgButton.setEnabled(false);
        ArrayList<String> result = new ArrayList<>();
        File image_folder = new File(NhPaths.APP_SD_FILES_IMG_PATH);
        if (!image_folder.exists()) {
            NhPaths.showMessage(context, "正在创建镜像文件存放目录…");
            if (!image_folder.mkdir()) {
                NhPaths.showMessage(context, "创建镜像目录失败: " + NhPaths.APP_SD_FILES_IMG_PATH);
                return;
            }
        }
        try {
            File[] filesInFolder = image_folder.listFiles();
            assert filesInFolder != null;
            for (File file : filesInFolder) {
                if (!file.isDirectory()) {
                    if (file.getName().contains(".img") || file.getName().contains(".iso")) {
                        result.add(file.getName());
                    }
                }
            }
        } catch (NullPointerException e) {
            e.printStackTrace();
            Log.e(TAG, e.toString());
        }
        ArrayAdapter<String> imageAdapter = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, result);
        imageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        imgFileSpinner.setAdapter(imageAdapter);
        mountImgButton.setEnabled(true);
        unmountImgButton.setEnabled(true);
    }

    /* 获取当前 USB 功能字符串 */
    private String getusbFuncSpinnerString() {
        if (usbFuncSpinner.getSelectedItem() != null) {
            return usbFuncSpinner.getSelectedItem().toString() +
                    (adbSpinner.getSelectedItem().equals("Enable")?",adb":"");
        }
        return "reset";
    }

    /* 获取当前目标系统字符串 */
    private String gettargetOSSpinnerString() {
        return targetOSSpinner.getSelectedItem().toString();
    }

    /* 获取 USB 网络模式索引 */
    private int getusbNetWorkModeSpinnerPosition() {
        return usbNetworkAttackModeSpinner.getSelectedItemPosition();
    }

    /* 检查所有 USB 信息是否有效 */
    private boolean isAllUSBInfosValid() {
        if (!is_init_exists){
            if (!usbSwitchInfoEditTextGroup[0].getText().toString().matches("^0x[0-9a-fA-F]{4}$")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 ^0x[0-9a-fA-F]{4}$").create().show();
                return false;
            }
            if (!usbSwitchInfoEditTextGroup[1].getText().toString().matches("^0x[0-9a-fA-F]{4}$")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 ^0x[0-9a-fA-F]{4}$").create().show();
                return false;
            }
            if (!usbSwitchInfoEditTextGroup[2].getText().toString().matches("^\\w+$|^$")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 ^\\w+$|^$").create().show();
                return false;
            }
            if (!usbSwitchInfoEditTextGroup[3].getText().toString().matches("^\\w+$|^$")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 ^\\w+$|^$").create().show();
                return false;
            }
            if (!usbSwitchInfoEditTextGroup[4].getText().toString().matches("^[0-9A-Z]{10}$|^$")) {
                new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat).setTitle("格式错误").setMessage("必须为 ^[0-9A-Z]{10}$|^$").create().show();
                return false;
            }
        }
        return true;
    }

    /* 刷新 USB 配置信息 */
    private void refreshUSBSwitchInfos(String targetOSName, String functionName) {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putString("targetOSName", targetOSName);
        bundle.putString("functionName", functionName);
        msg.what = USBArsenalHandlerThread.GET_USBSWITCH_SQL_DATA;
        msg.obj = context;
        msg.setData(bundle);
        usbArsenalHandlerThread.getHandler().sendMessage(msg);
    }

    /* 刷新 USB 网络配置信息 */
    private void refreshUSBNetworkInfos(int attackModePosition) {
        Message msg = new Message();
        msg.what = USBArsenalHandlerThread.GET_USBNETWORK_SQL_DATA;
        msg.obj = context;
        msg.arg1 = attackModePosition;
        usbArsenalHandlerThread.getHandler().sendMessage(msg);
    }

    /* Bridge 执行命令 */
    public void run_cmd_android(String cmd) {
        Intent intent = Bridge.createExecuteIntent("/data/data/com.offsec.nhterm/files/usr/bin/android-su", cmd);
        context.startActivity(intent);
    }
}