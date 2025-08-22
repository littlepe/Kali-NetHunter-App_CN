package com.offsec.nethunter.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.offsec.nethunter.RecyclerViewData.KaliServicesData;
import com.offsec.nethunter.models.KaliServicesModel;

import java.util.List;


/**
 * Kali 服务视图模型类
 * 用于观察 KaliServicesModel 类的列表
 * 每次创建 KaliServicesFragment 时都应初始化此类
 * 在应用生命周期内, KaliServicesData 单例将始终保持活动状态
 */
public class KaliServicesViewModel extends ViewModel {
    private MutableLiveData<List<KaliServicesModel>> mutableLiveDataKaliServicesModelList;

    /**
     * 初始化方法
     * @param context 应用上下文
     */
    public void init(Context context) {
        if (mutableLiveDataKaliServicesModelList != null) {
            return;
        }
        KaliServicesData kaliServicesData = KaliServicesData.getInstance();
        if (KaliServicesData.isDataInitiated) {
            mutableLiveDataKaliServicesModelList = kaliServicesData.getKaliServicesModels();
        } else {
            mutableLiveDataKaliServicesModelList = kaliServicesData.getKaliServicesModels(context);
        }
    }

    /**
     * 获取 Kali 服务模型列表的 LiveData
     * @return 包含 Kali 服务模型列表的 LiveData
     */
    public LiveData<List<KaliServicesModel>> getLiveDataKaliServicesModelList() {
        return mutableLiveDataKaliServicesModelList;
    }
}
