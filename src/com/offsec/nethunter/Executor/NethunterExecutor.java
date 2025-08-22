package com.offsec.nethunter.Executor;

import android.os.Handler;
import android.os.Looper;

import com.offsec.nethunter.SQL.NethunterSQL;
import com.offsec.nethunter.models.NethunterModel;
import com.offsec.nethunter.utils.ShellExecuter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NethunterExecutor {
    private NethunterExecutorListener listener;
    private final int actionCode;
    private int position;
    private int originalPositionIndex;
    private int targetPositionIndex;
    private ArrayList<Integer> selectedPositionsIndex;
    private ArrayList<Integer> selectedTargetIds;
    private ArrayList<String> dataArrayList;
    private NethunterSQL nethunterSQL;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public static final int GETITEMRESULTS = 0;
    public static final int RUNCMDFORITEM = 1;
    public static final int EDITDATA = 2;
    public static final int ADDDATA = 3;
    public static final int DELETEDATA = 4;
    public static final int MOVEDATA = 5;
    public static final int BACKUPDATA = 6;
    public static final int RESTOREDATA = 7;
    public static final int RESETDATA = 8;

    public NethunterExecutor(int actionCode) {
        this.actionCode = actionCode;
    }

    public NethunterExecutor(int actionCode, int position) {
        this.actionCode = actionCode;
        this.position = position;
    }

    public NethunterExecutor(int actionCode, int position, ArrayList<String> dataArrayList, NethunterSQL nethunterSQL) {
        this.actionCode = actionCode;
        this.position = position;
        this.dataArrayList = dataArrayList;
        this.nethunterSQL = nethunterSQL;
    }

    public NethunterExecutor(int actionCode, ArrayList<Integer> selectedPositionsIndex, ArrayList<Integer> selectedTargetIds, NethunterSQL nethunterSQL) {
        this.actionCode = actionCode;
        this.selectedPositionsIndex = selectedPositionsIndex;
        this.selectedTargetIds = selectedTargetIds;
        this.nethunterSQL = nethunterSQL;
    }

    public NethunterExecutor(int actionCode, int originalPositionIndex, int targetPositionIndex, NethunterSQL nethunterSQL) {
        this.actionCode = actionCode;
        this.originalPositionIndex = originalPositionIndex;
        this.targetPositionIndex = targetPositionIndex;
        this.nethunterSQL = nethunterSQL;
    }

    public NethunterExecutor(int actionCode, NethunterSQL nethunterSQL) {
        this.actionCode = actionCode;
        this.nethunterSQL = nethunterSQL;
    }

    public void execute(List<NethunterModel> nethunterModelList) {
        if (listener != null) {
            mainHandler.post(listener::onPrepare);
        }

        executorService.execute(() -> {
            List<NethunterModel> result = performTask(nethunterModelList);

            mainHandler.post(() -> {
                if (listener != null) {
                    listener.onFinished(result);
                }
            });
        });
    }

    private List<NethunterModel> performTask(List<NethunterModel> nethunterModelList) {
        switch (actionCode) {
            case GETITEMRESULTS:
                if (nethunterModelList != null) {
                    for (NethunterModel model : nethunterModelList) {
                        model.setResult(model.getRunOnCreate().equals("1")
                                ? new ShellExecuter().RunAsRootOutput(model.getCommand()).split("\\n")
                                : "请手动点击运行按钮. ".split("\\n"));
                    }
                }
                break;
            case RUNCMDFORITEM:
                if (nethunterModelList != null) {
                    nethunterModelList.get(position).setResult(new ShellExecuter().RunAsRootOutput(nethunterModelList.get(position).getCommand()).split("\\n"));
                }
                break;
            case EDITDATA:
                if (nethunterModelList != null) {
                    NethunterModel model = nethunterModelList.get(position);
                    model.setTitle(dataArrayList.get(0));
                    model.setCommand(dataArrayList.get(1));
                    model.setDelimiter(dataArrayList.get(2));
                    model.setRunOnCreate(dataArrayList.get(3));
                    if (dataArrayList.get(3).equals("1")) {
                        model.setResult(new ShellExecuter().RunAsRootOutput(dataArrayList.get(1)).split(dataArrayList.get(2)));
                    }
                    nethunterSQL.editData(position, dataArrayList);
                }
                break;
            case ADDDATA:
                if (nethunterModelList != null) {
                    nethunterModelList.add(position - 1, new NethunterModel(
                            dataArrayList.get(0),
                            dataArrayList.get(1),
                            dataArrayList.get(2),
                            dataArrayList.get(3),
                            "".split(dataArrayList.get(2))));
                    if (dataArrayList.get(3).equals("1")) {
                        nethunterModelList.get(position - 1).setResult(new ShellExecuter().RunAsRootOutput(dataArrayList.get(1)).split(dataArrayList.get(2)));
                    }
                    nethunterSQL.addData(position, dataArrayList);
                }
                break;
            case DELETEDATA:
                if (nethunterModelList != null) {
                    Collections.sort(selectedPositionsIndex, Collections.reverseOrder());
                    for (Integer selectedPosition : selectedPositionsIndex) {
                        nethunterModelList.remove((int) selectedPosition);
                    }
                    nethunterSQL.deleteData(selectedTargetIds);
                }
                break;
            case MOVEDATA:
                if (nethunterModelList != null) {
                    NethunterModel tempNethunterModel = nethunterModelList.get(originalPositionIndex);
                    nethunterModelList.remove(originalPositionIndex);
                    if (originalPositionIndex < targetPositionIndex) {
                        targetPositionIndex--;
                    }
                    nethunterModelList.add(targetPositionIndex, tempNethunterModel);
                    nethunterSQL.moveData(originalPositionIndex, targetPositionIndex);
                }
                break;
            case RESTOREDATA:
                if (nethunterModelList != null) {
                    nethunterModelList.clear();
                    nethunterModelList.addAll(nethunterSQL.bindData(new ArrayList<>()));
                }
                break;
        }
        return nethunterModelList;
    }

    public void setListener(NethunterExecutorListener listener) {
        this.listener = listener;
    }

    public interface NethunterExecutorListener {
        void onPrepare();
        void onFinished(List<NethunterModel> nethunterModelList);
    }
}