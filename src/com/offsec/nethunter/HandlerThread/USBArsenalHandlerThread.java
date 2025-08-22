package com.offsec.nethunter.HandlerThread;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import androidx.annotation.NonNull;

import com.offsec.nethunter.SQL.USBArsenalSQL;
import com.offsec.nethunter.utils.ShellExecuter;

public class USBArsenalHandlerThread extends HandlerThread {
    private Handler handler;
    public static final int IS_INIT_EXIST = 1;
    public static final int RETRIEVE_USB_FUNCS = 2;
    public static final int SETUSBIFACE = 3;
    public static final int RELOAD_USBIFACE = 4;
    public static final int GET_STORAGE_FUNC_FOLDER_NAME = 5;
    public static final int RELOAD_MOUNTSTATUS = 6;
    public static final int MOUNT_IMAGE = 7;
    public static final int UNMOUNT_IMAGE = 8;
    public static final int CHANGE_INQUIRY_STRING = 9;
    public static final int GET_USBSWITCH_SQL_DATA = 10;
    public static final int GET_USBNETWORK_SQL_DATA = 11;
    private USBArsenalListener listener;
    private Object resultObject = new Object();
    private final ShellExecuter exe = new ShellExecuter();

    public USBArsenalHandlerThread() {
        super("USBArsenalHandlerThread", Process.THREAD_PRIORITY_DEFAULT);
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case IS_INIT_EXIST: // 检查初始化是否存在
                    case SETUSBIFACE: // 设置USB接口
                    case MOUNT_IMAGE: // 挂载镜像
                    case UNMOUNT_IMAGE: // 卸载镜像
                    case CHANGE_INQUIRY_STRING: // 更改查询字符串
                        resultObject = exe.RunAsRootReturnValue(msg.obj.toString());
                        break;
                    case RETRIEVE_USB_FUNCS: // 检索USB功能
                    case RELOAD_USBIFACE: // 重新加载USB接口
                    case GET_STORAGE_FUNC_FOLDER_NAME: // 获取存储功能文件夹名称
                    case RELOAD_MOUNTSTATUS: // 重新加载挂载状态
                        resultObject = exe.RunAsRootOutput(msg.obj.toString());
                        break;
                    case GET_USBSWITCH_SQL_DATA: // 获取USB切换SQL数据
                        resultObject = USBArsenalSQL.getInstance((Context) msg.obj)
                                .getUSBSwitchColumnData(msg.getData().getString("targetOSName"),
                                        msg.getData().getString("functionName"));
                        break;
                    case GET_USBNETWORK_SQL_DATA: // 获取USB网络SQL数据
                        resultObject = USBArsenalSQL.getInstance((Context) msg.obj).getUSBNetworkColumnData(msg.arg1);
                        break;
                    default:
                        break;
                }
                if (listener != null) {
                    listener.onShellExecuterFinished(resultObject, msg.what);
                }
            }
        };
    }

    public Handler getHandler() {
        return handler;
    }

    public void setOnShellExecuterFinishedListener(USBArsenalListener listener) {
        this.listener = listener;
    }

    public interface USBArsenalListener {
        void onShellExecuterFinished(Object resultObject, int actionCode);
    }
}