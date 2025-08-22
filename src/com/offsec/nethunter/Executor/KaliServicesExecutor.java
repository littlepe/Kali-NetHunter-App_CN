package com.offsec.nethunter.Executor;

import android.os.Handler;
import android.os.Looper;

import com.offsec.nethunter.ChrootManagerFragment;
import com.offsec.nethunter.SQL.KaliServicesSQL;
import com.offsec.nethunter.models.KaliServicesModel;
import com.offsec.nethunter.utils.NhPaths;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class KaliServicesExecutor {
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private KaliServicesExecutorListener listener;
	private final int actionCode;
	private int position;
	private int originalPositionIndex;
	private int targetPositionIndex;
	private ArrayList<Integer> selectedPositionsIndex;
	private ArrayList<Integer> selectedTargetIds;
	private ArrayList<String> dataArrayList;
	private KaliServicesSQL kaliServicesSQL;
	public static final int GETITEMSTATUS = 0;
	public static final int START_SERVICE_FOR_ITEM = 1;
	public static final int STOP_SERVICE_FOR_ITEM = 2;
	public static final int EDITDATA = 3;
	public static final int ADDDATA = 4;
	public static final int DELETEDATA = 5;
	public static final int MOVEDATA = 6;
	public static final int BACKUPDATA = 7;
	public static final int RESTOREDATA = 8;
	public static final int RESETDATA = 9;
	public static final int UPDATE_RUNONCHROOTSTART_SCRIPTS = 10;

	public KaliServicesExecutor(int actionCode) {
		this.actionCode = actionCode;
	}

	public KaliServicesExecutor(int actionCode, int position) {
		this.actionCode = actionCode;
		this.position = position;
	}

	public KaliServicesExecutor(int actionCode, int position, ArrayList<String> dataArrayList, KaliServicesSQL kaliServicesSQL) {
		this.actionCode = actionCode;
		this.position = position;
		this.dataArrayList = dataArrayList;
		this.kaliServicesSQL = kaliServicesSQL;
	}

	public KaliServicesExecutor(int actionCode, ArrayList<Integer> selectedPositionsIndex, ArrayList<Integer> selectedTargetIds, KaliServicesSQL kaliServicesSQL) {
		this.actionCode = actionCode;
		this.selectedPositionsIndex = selectedPositionsIndex;
		this.selectedTargetIds = selectedTargetIds;
		this.kaliServicesSQL = kaliServicesSQL;
	}

	public KaliServicesExecutor(int actionCode, int originalPositionIndex, int targetPositionIndex, KaliServicesSQL kaliServicesSQL) {
		this.actionCode = actionCode;
		this.originalPositionIndex = originalPositionIndex;
		this.targetPositionIndex = targetPositionIndex;
		this.kaliServicesSQL = kaliServicesSQL;
	}

	public KaliServicesExecutor(int actionCode, KaliServicesSQL kaliServicesSQL) {
		this.actionCode = actionCode;
		this.kaliServicesSQL = kaliServicesSQL;
	}

	public void execute(List<KaliServicesModel> kaliServicesModelList) {
		executorService.execute(() -> {
			List<KaliServicesModel> result = performTask(kaliServicesModelList);
			mainHandler.post(() -> {
				if (listener != null) {
					listener.onTaskFinished(result);
				}
				ChrootManagerFragment.isExecutorRunning = false;
			});
		});
	}

	private List<KaliServicesModel> performTask(List<KaliServicesModel> kaliServicesModelList) {
		switch (actionCode) {
			case GETITEMSTATUS:
				if (kaliServicesModelList != null) {
					for (KaliServicesModel model : kaliServicesModelList) {
						model.setStatus(new ShellExecuter().RunAsRootReturnValue(NhPaths.BUSYBOX + " ps | grep -v grep | grep -w '" + model.getCommandforCheckServiceStatus() + "'") == 0 ? "[+] 服务正在运行" : "[-] 服务未运行");
					}
				}
				break;
			case START_SERVICE_FOR_ITEM:
				if (kaliServicesModelList != null) {
					kaliServicesModelList.get(position).setStatus(new ShellExecuter().RunAsChrootReturnValue(kaliServicesModelList.get(position).getCommandforStartService()) == 0 ? "[+] 服务正在运行" : "[-] 服务未运行");
				}
				break;
			case STOP_SERVICE_FOR_ITEM:
				if (kaliServicesModelList != null) {
					kaliServicesModelList.get(position).setStatus(new ShellExecuter().RunAsChrootReturnValue(kaliServicesModelList.get(position).getCommandforStopService()) == 0 ? "[-] 服务未运行" : "[+] 服务正在运行");
				}
				break;
			case EDITDATA:
				if (kaliServicesModelList != null) {
					kaliServicesModelList.get(position).setServiceName(dataArrayList.get(0));
					kaliServicesModelList.get(position).setCommandforStartService(dataArrayList.get(1));
					kaliServicesModelList.get(position).setCommandforStopService(dataArrayList.get(2));
					kaliServicesModelList.get(position).setCommandforCheckServiceStatus(dataArrayList.get(3));
					kaliServicesModelList.get(position).setRunOnChrootStart(dataArrayList.get(4));
					updateRunOnChrootStartScripts(kaliServicesModelList);
					kaliServicesSQL.editData(position, dataArrayList);
				}
				break;
			case ADDDATA:
				if (kaliServicesModelList != null) {
					kaliServicesModelList.add(position - 1, new KaliServicesModel(
							dataArrayList.get(0),
							dataArrayList.get(1),
							dataArrayList.get(2),
							dataArrayList.get(3),
							dataArrayList.get(4),
							""));
					if (dataArrayList.get(4).equals("1")) {
						updateRunOnChrootStartScripts(kaliServicesModelList);
					}
					kaliServicesSQL.addData(position, dataArrayList);
				}
				break;
			case DELETEDATA:
				if (kaliServicesModelList != null) {
					Collections.sort(selectedPositionsIndex, Collections.reverseOrder());
					for (Integer selectedPosition : selectedPositionsIndex) {
						kaliServicesModelList.remove((int) selectedPosition);
					}
					kaliServicesSQL.deleteData(selectedTargetIds);
				}
				break;
			case MOVEDATA:
				if (kaliServicesModelList != null) {
					KaliServicesModel tempKaliServicesModel = new KaliServicesModel(
							kaliServicesModelList.get(originalPositionIndex).getServiceName(),
							kaliServicesModelList.get(originalPositionIndex).getCommandforStartService(),
							kaliServicesModelList.get(originalPositionIndex).getCommandforStopService(),
							kaliServicesModelList.get(originalPositionIndex).getCommandforCheckServiceStatus(),
							kaliServicesModelList.get(originalPositionIndex).getRunOnChrootStart(),
							kaliServicesModelList.get(originalPositionIndex).getStatus()
					);
					kaliServicesModelList.remove(originalPositionIndex);
					if (originalPositionIndex < targetPositionIndex) {
						targetPositionIndex = targetPositionIndex - 1;
					}
					kaliServicesModelList.add(targetPositionIndex, tempKaliServicesModel);
					kaliServicesSQL.moveData(originalPositionIndex, targetPositionIndex);
				}
				break;
			case RESTOREDATA:
				if (kaliServicesModelList != null) {
					kaliServicesModelList.clear();
					kaliServicesSQL.bindData(kaliServicesModelList);
				}
				break;
			case UPDATE_RUNONCHROOTSTART_SCRIPTS:
				if (kaliServicesModelList != null) {
					kaliServicesModelList.get(position).setServiceName(dataArrayList.get(0));
					kaliServicesModelList.get(position).setCommandforStartService(dataArrayList.get(1));
					kaliServicesModelList.get(position).setCommandforStopService(dataArrayList.get(2));
					kaliServicesModelList.get(position).setCommandforCheckServiceStatus(dataArrayList.get(3));
					kaliServicesModelList.get(position).setRunOnChrootStart(dataArrayList.get(4));
					kaliServicesSQL.editData(position, dataArrayList);
					updateRunOnChrootStartScripts(kaliServicesModelList);
				}
				break;
		}
		return kaliServicesModelList;
	}

	public void setListener(KaliServicesExecutorListener listener) {
		this.listener = listener;
	}

	public interface KaliServicesExecutorListener {
		void onTaskFinished(List<KaliServicesModel> kaliServicesModelList);
	}

	private void updateRunOnChrootStartScripts(List<KaliServicesModel> kaliServicesModelList) {
		StringBuilder tmpStringBuilder = new StringBuilder();
		for (KaliServicesModel model : kaliServicesModelList) {
			if (model.getRunOnChrootStart().equals("1")) {
				tmpStringBuilder.append(model.getCommandforStartService()).append("\n");
			}
		}
		new ShellExecuter().RunAsRootOutput("cat << 'EOF' > " + NhPaths.APP_SCRIPTS_PATH + "/kaliservices" + "\n" + tmpStringBuilder + "\nEOF");
	}
}