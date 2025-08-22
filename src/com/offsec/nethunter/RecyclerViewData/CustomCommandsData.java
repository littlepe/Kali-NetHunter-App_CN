package com.offsec.nethunter.RecyclerViewData;

import android.content.Context;

import androidx.lifecycle.MutableLiveData;

import com.offsec.nethunter.Executor.CustomCommandsExecutor;
import com.offsec.nethunter.SQL.CustomCommandsSQL;
import com.offsec.nethunter.models.CustomCommandsModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 自定义命令数据仓库
 * 负责管理自定义命令的增删改查、备份恢复及数据同步
 */
public class CustomCommandsData {
	private static CustomCommandsData instance;
	// 标记数据是否已初始化
	public static boolean isDataInitiated = false;
	private final ArrayList<CustomCommandsModel> customCommandsModelArrayList = new ArrayList<>();
	// LiveData 用于 UI 自动刷新
	private final MutableLiveData<List<CustomCommandsModel>> data = new MutableLiveData<>();
	public List<CustomCommandsModel> customCommandsModelListFull;
	private final List<CustomCommandsModel> copyOfCustomCommandsModelListFull = new ArrayList<>();

	/** 获取单例实例 */
	public static synchronized CustomCommandsData getInstance() {
		if (instance == null) {
			instance = new CustomCommandsData();
		}
		return instance;
	}

	/** 首次加载数据库数据并返回 LiveData */
	public MutableLiveData<List<CustomCommandsModel>> getCustomCommandsModels(Context context) {
		if (!isDataInitiated) {
			data.setValue(CustomCommandsSQL.getInstance(context).bindData(customCommandsModelArrayList));
			customCommandsModelListFull = new ArrayList<>(Objects.requireNonNull(data.getValue()));
			isDataInitiated = true;
		}
		return data;
	}

	/** 获取当前 LiveData（不触发加载） */
	public MutableLiveData<List<CustomCommandsModel>> getCustomCommandsModels() {
		return data;
	}

	/** 运行指定位置的命令 */
	public void runCommandforitem(int position, Context context) {
		CustomCommandsExecutor customCommandsExecutor = new CustomCommandsExecutor(CustomCommandsExecutor.RUNCMD, position, context);
		customCommandsExecutor.setListener(new CustomCommandsExecutor.CustomCommandsExecutorListener() {
			@Override
			public void onTaskFinished(List<CustomCommandsModel> customCommandsModelList) {
				updateCustomCommandsModelListFull(customCommandsModelList);
				Objects.requireNonNull(getCustomCommandsModels().getValue()).clear();
				getCustomCommandsModels().getValue().addAll(customCommandsModelList);
				getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}
		});
		customCommandsExecutor.execute(getInitCopyOfCustomCommandsModelListFull());
	}

	/** 编辑指定位置的命令 */
	public void editData(int position, List<String> dataArrayList, CustomCommandsSQL customCommandsSQL) {
		CustomCommandsExecutor customCommandsExecutor = new CustomCommandsExecutor(CustomCommandsExecutor.EDITDATA, position, (ArrayList<String>) dataArrayList, customCommandsSQL);
		customCommandsExecutor.setListener(new CustomCommandsExecutor.CustomCommandsExecutorListener() {
			@Override
			public void onTaskFinished(List<CustomCommandsModel> customCommandsModelList) {
				updateCustomCommandsModelListFull(customCommandsModelList);
				Objects.requireNonNull(getCustomCommandsModels().getValue()).clear();
				getCustomCommandsModels().getValue().addAll(customCommandsModelList);
				getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}
		});
		customCommandsExecutor.execute(getInitCopyOfCustomCommandsModelListFull());
	}

	/** 新增命令 */
	public void addData(int position, List<String> dataArrayList, CustomCommandsSQL customCommandsSQL) {
		CustomCommandsExecutor customCommandsExecutor = new CustomCommandsExecutor(CustomCommandsExecutor.ADDDATA, position, (ArrayList<String>) dataArrayList, customCommandsSQL);
		customCommandsExecutor.setListener(new CustomCommandsExecutor.CustomCommandsExecutorListener() {
			@Override
			public void onTaskFinished(List<CustomCommandsModel> customCommandsModelList) {
				updateCustomCommandsModelListFull(customCommandsModelList);
				Objects.requireNonNull(getCustomCommandsModels().getValue()).clear();
				getCustomCommandsModels().getValue().addAll(customCommandsModelList);
				getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}
		});
		customCommandsExecutor.execute(getInitCopyOfCustomCommandsModelListFull());
	}

	/** 批量删除命令 */
	public void deleteData(List<Integer> selectedPositionsIndex, List<Integer> selectedTargetIds, CustomCommandsSQL customCommandsSQL) {
		CustomCommandsExecutor customCommandsExecutor = new CustomCommandsExecutor(CustomCommandsExecutor.DELETEDATA, (ArrayList<Integer>) selectedPositionsIndex, (ArrayList<Integer>) selectedTargetIds, customCommandsSQL);
		customCommandsExecutor.setListener(new CustomCommandsExecutor.CustomCommandsExecutorListener() {
			@Override
			public void onTaskFinished(List<CustomCommandsModel> customCommandsModelList) {
				updateCustomCommandsModelListFull(customCommandsModelList);
				Objects.requireNonNull(getCustomCommandsModels().getValue()).clear();
				getCustomCommandsModels().getValue().addAll(customCommandsModelList);
				getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}
		});
		customCommandsExecutor.execute(getInitCopyOfCustomCommandsModelListFull());
	}

	/** 移动命令排序 */
	public void moveData(int originalPositionIndex, int targetPositionIndex, CustomCommandsSQL customCommandsSQL) {
		CustomCommandsExecutor customCommandsExecutor = new CustomCommandsExecutor(CustomCommandsExecutor.MOVEDATA, originalPositionIndex, targetPositionIndex, customCommandsSQL);
		customCommandsExecutor.setListener(new CustomCommandsExecutor.CustomCommandsExecutorListener() {
			@Override
			public void onTaskFinished(List<CustomCommandsModel> customCommandsModelList) {
				updateCustomCommandsModelListFull(customCommandsModelList);
				Objects.requireNonNull(getCustomCommandsModels().getValue()).clear();
				getCustomCommandsModels().getValue().addAll(customCommandsModelList);
				getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}
		});
		customCommandsExecutor.execute(getInitCopyOfCustomCommandsModelListFull());
	}

	/** 备份数据到指定路径 */
	public String backupData(CustomCommandsSQL customCommandsSQL, String storedDBpath) {
		return customCommandsSQL.backupData(storedDBpath);
	}

	/** 从指定路径恢复数据 */
	public String restoreData(CustomCommandsSQL customCommandsSQL, String storedDBpath) {
		String returnedResult = customCommandsSQL.restoreData(storedDBpath);
		if (returnedResult == null) {
			CustomCommandsExecutor customCommandsExecutor = new CustomCommandsExecutor(CustomCommandsExecutor.RESTOREDATA, customCommandsSQL);
			customCommandsExecutor.setListener(new CustomCommandsExecutor.CustomCommandsExecutorListener() {
				@Override
				public void onTaskFinished(List<CustomCommandsModel> customCommandsModelList) {
					updateCustomCommandsModelListFull(customCommandsModelList);
					Objects.requireNonNull(getCustomCommandsModels().getValue()).clear();
					getCustomCommandsModels().getValue().addAll(customCommandsModelList);
					getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
				}

				public void onExecutorPrepare() {
					// 预执行空实现, 备用
				}
			});
			customCommandsExecutor.execute(getInitCopyOfCustomCommandsModelListFull());
			return null;
		} else {
			return returnedResult;
		}
	}

	/** 重置数据到默认状态 */
	public void resetData(CustomCommandsSQL customCommandsSQL) {
		customCommandsSQL.resetData();
		CustomCommandsExecutor customCommandsExecutor = new CustomCommandsExecutor(CustomCommandsExecutor.RESTOREDATA, customCommandsSQL);
		customCommandsExecutor.setListener(new CustomCommandsExecutor.CustomCommandsExecutorListener() {
			@Override
			public void onTaskFinished(List<CustomCommandsModel> customCommandsModelList) {
				updateCustomCommandsModelListFull(customCommandsModelList);
				Objects.requireNonNull(getCustomCommandsModels().getValue()).clear();
				getCustomCommandsModels().getValue().addAll(customCommandsModelList);
				getCustomCommandsModels().postValue(getCustomCommandsModels().getValue());
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}
		});
		customCommandsExecutor.execute(getInitCopyOfCustomCommandsModelListFull());
	}

	/** 更新全量数据副本 */
	public void updateCustomCommandsModelListFull(List<CustomCommandsModel> copyOfCustomCommandsModelList) {
		customCommandsModelListFull.clear();
		customCommandsModelListFull.addAll(copyOfCustomCommandsModelList);
	}

	/** 获取当前全量数据的深拷贝 */
	private List<CustomCommandsModel> getInitCopyOfCustomCommandsModelListFull() {
		copyOfCustomCommandsModelListFull.clear();
		copyOfCustomCommandsModelListFull.addAll(customCommandsModelListFull);
		return copyOfCustomCommandsModelListFull;
	}
}
