package com.offsec.nethunter.RecyclerViewData;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.offsec.nethunter.Executor.NethunterExecutor;
import com.offsec.nethunter.SQL.NethunterSQL;
import com.offsec.nethunter.models.NethunterModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** NetHunter 数据管理类, 用于处理与 NetHunter 相关的数据操作, 包括获取、刷新、编辑、添加、删除等 */
public class NethunterData {
    private static NethunterData instance;
    public static boolean isDataInitiated = false;
    private final ArrayList<NethunterModel> nethunterModelArrayList = new ArrayList<>();
    private final MutableLiveData<List<NethunterModel>> data = new MutableLiveData<>();
    public List<NethunterModel> nethunterModelListFull;
    private final List<NethunterModel> copyOfNethunterModelListFull = new ArrayList<>();

    /** 获取单例实例 */
    public static synchronized NethunterData getInstance() {
        if (instance == null) {
            instance = new NethunterData();
        }
        return instance;
    }

    /**
     * 获取 NetHunter 模型数据
     * @param context 应用上下文
     * @return 包含 NetHunter 模型列表的 LiveData
     */
    public MutableLiveData<List<NethunterModel>> getNethunterModels(Context context) {
        if (!isDataInitiated) {
            data.setValue(NethunterSQL.getInstance(context).bindData(nethunterModelArrayList));
            nethunterModelListFull = new ArrayList<>(Objects.requireNonNull(data.getValue()));
            isDataInitiated = true;
        }
        return data;
    }

    /** 获取已初始化的 NetHunter 模型数据 LiveData */
    public MutableLiveData<List<NethunterModel>> getNethunterModels() {
        return data;
    }

    /** 刷新 NetHunter 数据 */
    public void refreshData() {
        NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.GETITEMRESULTS);
        nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
            @Override
            public void onPrepare() {
                // 准备时的操作（可选）
            }

            @Override
            public void onFinished(List<NethunterModel> nethunterModelList) {
                Objects.requireNonNull(getNethunterModels().getValue()).clear();
                getNethunterModels().getValue().addAll(nethunterModelList);
                getNethunterModels().postValue(getNethunterModels().getValue());
            }
        });
        nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
    }

    /** 运行指定项的命令 */
    public void runCommandforItem(int position) {
        NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.RUNCMDFORITEM, position);
        nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
            @Override
            public void onPrepare() {
                // 准备时的操作（可选）
            }

            @Override
            public void onFinished(List<NethunterModel> nethunterModelList) {
                Objects.requireNonNull(getNethunterModels().getValue()).clear();
                getNethunterModels().getValue().addAll(nethunterModelList);
                getNethunterModels().postValue(getNethunterModels().getValue());
            }
        });
        nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
    }

    /** 编辑数据 */
    public void editData(int position, List<String> dataArrayList, NethunterSQL nethunterSQL) {
        NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.EDITDATA, position, (ArrayList<String>) dataArrayList, nethunterSQL);
        nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
            @Override
            public void onPrepare() {
                // 准备时的操作（可选）
            }

            @Override
            public void onFinished(List<NethunterModel> nethunterModelList) {
                updateNethunterModelListFull(nethunterModelList);
                Objects.requireNonNull(getNethunterModels().getValue()).clear();
                getNethunterModels().getValue().addAll(nethunterModelList);
                getNethunterModels().postValue(getNethunterModels().getValue());
            }
        });
        nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
    }

    /** 添加数据 */
    public void addData(int position, List<String> dataArrayList, NethunterSQL nethunterSQL) {
        NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.ADDDATA, position, (ArrayList<String>) dataArrayList, nethunterSQL);
        nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
            @Override
            public void onPrepare() {
                // 准备时的操作（可选）
            }

            @Override
            public void onFinished(List<NethunterModel> nethunterModelList) {
                updateNethunterModelListFull(nethunterModelList);
                Objects.requireNonNull(getNethunterModels().getValue()).clear();
                getNethunterModels().getValue().addAll(nethunterModelList);
                getNethunterModels().postValue(getNethunterModels().getValue());
            }
        });
        nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
    }

    /** 删除数据 */
    public void deleteData(List<Integer> selectedPositionsIndex, List<Integer> selectedTargetIds, NethunterSQL nethunterSQL) {
        NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.DELETEDATA, (ArrayList<Integer>) selectedPositionsIndex, (ArrayList<Integer>) selectedTargetIds, nethunterSQL);
        nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
            @Override
            public void onPrepare() {
                // 准备时的操作（可选）
            }

            @Override
            public void onFinished(List<NethunterModel> nethunterModelList) {
                updateNethunterModelListFull(nethunterModelList);
                Objects.requireNonNull(getNethunterModels().getValue()).clear();
                getNethunterModels().getValue().addAll(nethunterModelList);
                getNethunterModels().postValue(getNethunterModels().getValue());
            }
        });
        nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
    }

    /** 移动数据 */
    public void moveData(int originalPositionIndex, int targetPositionIndex, NethunterSQL nethunterSQL) {
        NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.MOVEDATA, originalPositionIndex, targetPositionIndex, nethunterSQL);
        nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
            @Override
            public void onPrepare() {
                // 准备时的操作（可选）
            }

            @Override
            public void onFinished(List<NethunterModel> nethunterModelList) {
                updateNethunterModelListFull(nethunterModelList);
                Objects.requireNonNull(getNethunterModels().getValue()).clear();
                getNethunterModels().getValue().addAll(nethunterModelList);
                getNethunterModels().postValue(getNethunterModels().getValue());
            }
        });
        nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
    }

    /** 备份数据 */
    public String backupData(NethunterSQL nethunterSQL, String storedDBpath) {
        return nethunterSQL.backupData(storedDBpath);
    }

    /** 恢复数据 */
    public String restoreData(NethunterSQL nethunterSQL, String storedDBpath) {
        String returnedResult = nethunterSQL.restoreData(storedDBpath);
        if (returnedResult == null) {
            NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.RESTOREDATA, nethunterSQL);
            nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
                @Override
                public void onPrepare() {
                    // 准备时的操作（可选）
                }

                @Override
                public void onFinished(List<NethunterModel> nethunterModelList) {
                    updateNethunterModelListFull(nethunterModelList);
                    Objects.requireNonNull(getNethunterModels().getValue()).clear();
                    getNethunterModels().getValue().addAll(nethunterModelList);
                    getNethunterModels().postValue(getNethunterModels().getValue());
                    refreshData();
                }
            });
            nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
            return null;
        } else {
            return returnedResult;
        }
    }

    /** 重置数据 */
    public void resetData(NethunterSQL nethunterSQL) {
        nethunterSQL.resetData();
        NethunterExecutor nethunterExecutor = new NethunterExecutor(NethunterExecutor.RESTOREDATA, nethunterSQL);
        nethunterExecutor.setListener(new NethunterExecutor.NethunterExecutorListener() {
            @Override
            public             void onPrepare() {
                // 准备时的操作（可选）
            }

            @Override
            public void onFinished(List<NethunterModel> nethunterModelList) {
                updateNethunterModelListFull(nethunterModelList);
                Objects.requireNonNull(getNethunterModels().getValue()).clear();
                getNethunterModels().getValue().addAll(nethunterModelList);
                getNethunterModels().postValue(getNethunterModels().getValue());
                refreshData();
            }
        });
        nethunterExecutor.execute(getInitCopyOfNethunterModelListFull());
    }

    /**
     * 更新全量 NetHunter 模型数据副本
     * @param copyOfNethunterModelList 新数据副本
     */
    public void updateNethunterModelListFull(List<NethunterModel> copyOfNethunterModelList) {
        nethunterModelListFull.clear();
        nethunterModelListFull.addAll(copyOfNethunterModelList);
    }

    /**
     * 获取全量 NetHunter 模型数据的初始副本
     * @return 数据副本
     */
    private List<NethunterModel> getInitCopyOfNethunterModelListFull() {
        copyOfNethunterModelListFull.clear();
        copyOfNethunterModelListFull.addAll(nethunterModelListFull);
        return copyOfNethunterModelListFull;
    }
}
