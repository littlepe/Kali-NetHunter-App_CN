package com.offsec.nethunter.viewmodels;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.offsec.nethunter.RecyclerViewData.NethunterData;
import com.offsec.nethunter.models.NethunterModel;
import java.util.List;

/**
 * NetHunter 视图模型类
 * 用于观察 NetHunterModel 类的列表
 * 每次创建 NethunterFragment 时都应初始化此类
 * 在应用生命周期内, NethunterData 单例将始终保持活动状态
 */
public class NethunterViewModel extends ViewModel {
    private MutableLiveData<List<NethunterModel>> mutableLiveDataNethunterModelList;

    /**
     * 初始化方法
     * @param context 应用上下文
     */
    public void init(Context context) {
        if (mutableLiveDataNethunterModelList != null) {
            return;
        }
        NethunterData nethunterData = NethunterData.getInstance();
        if (NethunterData.isDataInitiated) {
            mutableLiveDataNethunterModelList = nethunterData.getNethunterModels();
        } else {
            mutableLiveDataNethunterModelList = nethunterData.getNethunterModels(context);
        }
    }

    /**
     * 获取 NetHunter 模型列表的 LiveData
     * @return 包含 NetHunter 模型列表的 LiveData
     */
    public LiveData<List<NethunterModel>> getLiveDataNethunterModelList() {
        return mutableLiveDataNethunterModelList;
    }
}
