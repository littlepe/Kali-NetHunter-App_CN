package com.offsec.nethunter.RecyclerViewData;

import android.content.Context;

import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.MutableLiveData;

import com.offsec.nethunter.Executor.KaliServicesExecutor;
import com.offsec.nethunter.SQL.KaliServicesSQL;
import com.offsec.nethunter.models.KaliServicesModel;
import com.offsec.nethunter.utils.NhPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Kali 服务数据管理类
 * 负责管理 Kali Linux 中服务的启动、停止、状态更新、编辑、添加、删除等操作
 */
public class KaliServicesData {
	private static KaliServicesData instance;
	public static boolean isDataInitiated = false;
	private final ArrayList<KaliServicesModel> kaliServicesModelArrayList = new ArrayList<>();
	private final MutableLiveData<List<KaliServicesModel>> data = new MutableLiveData<>();
	public List<KaliServicesModel> kaliServicesModelListFull;
	private final List<KaliServicesModel> copyOfKaliServicesModelListFull = new ArrayList<>();

	/**
	 * 获取单例实例
	 */
	public static synchronized KaliServicesData getInstance() {
		if (instance == null) {
			instance = new KaliServicesData();
		}
		return instance;
	}

	/**
	 * 首次加载 Kali 服务数据
	 * @param context 应用上下文
	 * @return 包含服务列表的 LiveData
	 */
	public MutableLiveData<List<KaliServicesModel>> getKaliServicesModels(Context context) {
		if (!isDataInitiated) {
			data.setValue(KaliServicesSQL.getInstance(context).bindData(kaliServicesModelArrayList));
			kaliServicesModelListFull = new ArrayList<>(Objects.requireNonNull(data.getValue()));
			isDataInitiated = true;
		}
		return data;
	}

	/**
	 * 获取当前 LiveData 数据（不触发加载）
	 * @return 包含服务列表的 LiveData
	 */
	public MutableLiveData<List<KaliServicesModel>> getKaliServicesModels() {
		return data;
	}

	/**
	 * 刷新服务状态
	 */
	public void refreshData() {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.GETITEMSTATUS);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				if (getKaliServicesModels().getValue() != null) {
					getKaliServicesModels().getValue().clear();
					getKaliServicesModels().getValue().addAll(kaliServicesModelList);
					getKaliServicesModels().postValue(getKaliServicesModels().getValue());
				}
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 启动指定服务
	 * @param position 服务位置索引
	 * @param mSwitch 开关控件
	 * @param context 应用上下文
	 */
	public void startServiceforItem(int position, SwitchCompat mSwitch, Context context) {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.START_SERVICE_FOR_ITEM, position);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				mSwitch.setEnabled(false);
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				mSwitch.setEnabled(true);
				mSwitch.setChecked(kaliServicesModelList.get(position).getStatus().startsWith("[+]"));
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
				if (!mSwitch.isChecked()) {
					NhPaths.showMessage(context, "Failed starting " + getKaliServicesModels().getValue().get(position).getServiceName() + " service");
				}
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 停止指定服务
	 * @param position 服务位置索引
	 * @param mSwitch 开关控件
	 * @param context 应用上下文
	 */
	public void stopServiceforItem(int position, SwitchCompat mSwitch, Context context) {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.STOP_SERVICE_FOR_ITEM, position);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				mSwitch.setEnabled(false);
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				mSwitch.setEnabled(true);
				mSwitch.setChecked(kaliServicesModelList.get(position).getStatus().startsWith("[+]"));
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
				if (mSwitch.isChecked()) {
					NhPaths.showMessage(context, "Failed stopping " + getKaliServicesModels().getValue().get(position).getServiceName() + " service");
				}
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 编辑服务数据
	 * @param position 服务位置索引
	 * @param dataArrayList 编辑数据列表
	 * @param kaliServicesSQL 数据库操作对象
	 */
	public void editData(int position, List<String> dataArrayList, KaliServicesSQL kaliServicesSQL) {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.EDITDATA, position, (ArrayList<String>) dataArrayList, kaliServicesSQL);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				updateKaliServicesModelListFull(kaliServicesModelList);
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 添加服务数据
	 * @param position 插入位置索引
	 * @param dataArrayList 添加数据列表
	 * @param kaliServicesSQL 数据库操作对象
	 */
	public void addData(int position, List<String> dataArrayList, KaliServicesSQL kaliServicesSQL) {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.ADDDATA, position, (ArrayList<String>) dataArrayList, kaliServicesSQL);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				updateKaliServicesModelListFull(kaliServicesModelList);
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 删除服务数据
	 * @param selectedPositionsIndex 选中位置索引列表
	 * @param selectedTargetIds 选中目标 ID 列表
	 * @param kaliServicesSQL 数据库操作对象
	 */
	public void deleteData(List<Integer> selectedPositionsIndex, List<Integer> selectedTargetIds, KaliServicesSQL kaliServicesSQL) {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.DELETEDATA, (ArrayList<Integer>) selectedPositionsIndex, (ArrayList<Integer>) selectedTargetIds, kaliServicesSQL);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				updateKaliServicesModelListFull(kaliServicesModelList);
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 移动服务数据
	 * @param originalPositionIndex 原始位置索引
	 * @param targetPositionIndex 目标位置索引
	 * @param kaliServicesSQL 数据库操作对象
	 */
	public void moveData(int originalPositionIndex, int targetPositionIndex, KaliServicesSQL kaliServicesSQL) {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.MOVEDATA, originalPositionIndex, targetPositionIndex, kaliServicesSQL);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				updateKaliServicesModelListFull(kaliServicesModelList);
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 备份服务数据
	 * @param kaliServicesSQL 数据库操作对象
	 * @param storedDBpath 备份路径
	 * @return 备份结果信息
	 */
	public String backupData(KaliServicesSQL kaliServicesSQL, String storedDBpath) {
		return kaliServicesSQL.backupData(storedDBpath);
	}

	/**
	 * 恢复服务数据
	 * @param kaliServicesSQL 数据库操作对象
	 * @param storedDBpath 恢复路径
	 * @return 恢复结果信息
	 */
	public String restoreData(KaliServicesSQL kaliServicesSQL, String storedDBpath) {
		String returnedResult = kaliServicesSQL.restoreData(storedDBpath);
		if (returnedResult == null) {
			KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.RESTOREDATA, kaliServicesSQL);
			kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
				@Override
				public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
					// 无需实现
				}

				public void onExecutorPrepare() {
					// 预执行空实现, 备用
				}

				public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
					updateKaliServicesModelListFull(kaliServicesModelList);
					Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
					getKaliServicesModels().getValue().addAll(kaliServicesModelList);
					getKaliServicesModels().postValue(getKaliServicesModels().getValue());
					refreshData();
				}
			});
			kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
			return null;
		} else {
			return returnedResult;
		}
	}

	/**
	 * 重置服务数据
	 * @param kaliServicesSQL 数据库操作对象
	 */
	public void resetData(KaliServicesSQL kaliServicesSQL) {
		kaliServicesSQL.resetData();
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.RESTOREDATA, kaliServicesSQL);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				updateKaliServicesModelListFull(kaliServicesModelList);
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
				refreshData();
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 更新服务在 Chroot 启动时运行的脚本
	 * @param position 服务位置索引
	 * @param dataArrayList 更新数据列表
	 * @param kaliServicesSQL 数据库操作对象
	 */
	public void updateRunOnChrootStartServices(int position, List<String> dataArrayList, KaliServicesSQL kaliServicesSQL) {
		KaliServicesExecutor kaliServicesExecutor = new KaliServicesExecutor(KaliServicesExecutor.UPDATE_RUNONCHROOTSTART_SCRIPTS, position, (ArrayList<String>) dataArrayList, kaliServicesSQL);
		kaliServicesExecutor.setListener(new KaliServicesExecutor.KaliServicesExecutorListener() {
			@Override
			public void onTaskFinished(List<KaliServicesModel> kaliServicesModelList) {
				// 无需实现
			}

			public void onExecutorPrepare() {
				// 预执行空实现, 备用
			}

			public void onExecutorFinished(List<KaliServicesModel> kaliServicesModelList) {
				updateKaliServicesModelListFull(kaliServicesModelList);
				Objects.requireNonNull(getKaliServicesModels().getValue()).clear();
				getKaliServicesModels().getValue().addAll(kaliServicesModelList);
				getKaliServicesModels().postValue(getKaliServicesModels().getValue());
			}
		});
		kaliServicesExecutor.execute(getInitCopyOfKaliServicesModelListFull());
	}

	/**
	 * 更新全量服务数据副本
	 * @param copyOfKaliServicesModelList 新数据副本
	 */
	public void updateKaliServicesModelListFull(List<KaliServicesModel> copyOfKaliServicesModelList) {
		kaliServicesModelListFull.clear();
		kaliServicesModelListFull.addAll(copyOfKaliServicesModelList);
	}

	/**
	 * 获取当前全量服务数据的深拷贝
	 * @return 数据副本
	 */
	private List<KaliServicesModel> getInitCopyOfKaliServicesModelListFull() {
		copyOfKaliServicesModelListFull.clear();
		copyOfKaliServicesModelListFull.addAll(kaliServicesModelListFull);
		return copyOfKaliServicesModelListFull;
	}
}
