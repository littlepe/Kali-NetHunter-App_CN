package com.offsec.nethunter.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.offsec.nethunter.RecyclerViewData.CustomCommandsData;
import com.offsec.nethunter.models.CustomCommandsModel;

import java.util.List;

/*
    CustomCommands 模型的 ViewModel 类, 用于观察 CustomCommandsModel 类的列表. 
    每次创建 CustomCommandsFragment 时都应初始化此类. 
    CustomCommandsData 单例创建后, 将一直存在直到应用程序终止. 
 */
public class CustomCommandsViewModel extends ViewModel {
    private MutableLiveData<List<CustomCommandsModel>> mutableLiveDataCustomCommandsModelList;

    public void init(Context context){
        if (mutableLiveDataCustomCommandsModelList != null){
            return;
        }
        CustomCommandsData customCommandsData = CustomCommandsData.getInstance();
        if (CustomCommandsData.isDataInitiated) {
            mutableLiveDataCustomCommandsModelList = customCommandsData.getCustomCommandsModels();
        } else {
            mutableLiveDataCustomCommandsModelList = customCommandsData.getCustomCommandsModels(context);
        }
    }

    public LiveData<List<CustomCommandsModel>> getLiveDataCustomCommandsModelList(){
        return mutableLiveDataCustomCommandsModelList;
    }
}
