package com.offsec.nethunter.RecyclerViewAdapter;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.offsec.nethunter.R;
import com.offsec.nethunter.RecyclerViewData.NethunterData;
import com.offsec.nethunter.SQL.NethunterSQL;
import com.offsec.nethunter.models.NethunterModel;
import com.offsec.nethunter.utils.NhPaths;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class NethunterRecyclerViewAdapter extends RecyclerView.Adapter<NethunterRecyclerViewAdapter.ItemViewHolder> implements Filterable {
    public static final String TAG = "NethunterRecyclerView";
    private final Context context;
    private final List<NethunterModel> nethunterModelList;

    public NethunterRecyclerViewAdapter(Context context, List<NethunterModel> nethunterModelList) {
        this.context = context;
        this.nethunterModelList = nethunterModelList;
    }

    @NonNull
    @Override
    public NethunterRecyclerViewAdapter.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.nethunter_recyclerview_main, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NethunterRecyclerViewAdapter.ItemViewHolder holder, int position) {
        holder.titleTextView.setText(nethunterModelList.get(position).getTitle());
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        holder.resultRecyclerView.setLayoutManager(linearLayoutManager);
        holder.resultRecyclerView.setAdapter(new NethunterRecyclerViewAdapterResult(context, nethunterModelList.get(position).getResult()));
        holder.runButton.setOnClickListener(v -> NethunterData.getInstance().runCommandforItem(position));
        holder.titleTextView.setOnLongClickListener(v -> {
            final ViewGroup nullParent = null;
            final LayoutInflater mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final View promptViewEdit = mInflater.inflate(R.layout.nethunter_edit_dialog_view, nullParent);
            final EditText titleEditText = promptViewEdit.findViewById(R.id.f_nethunter_edit_adb_et_title);
            final EditText cmdEditText = promptViewEdit.findViewById(R.id.f_nethunter_edit_adb_et_command);
            final EditText delimiterEditText = promptViewEdit.findViewById(R.id.f_nethunter_edit_adb_et_delimiter);
            final CheckBox runOnCreateCheckbox = promptViewEdit.findViewById(R.id.f_nethunters_edit_adb_checkbox_runoncreate);
            final FloatingActionButton readmeButton1 = promptViewEdit.findViewById(R.id.f_nethunter_edit_btn_info_fab1);
            final FloatingActionButton readmeButton2 = promptViewEdit.findViewById(R.id.f_nethunter_edit_btn_info_fab2);
            final FloatingActionButton readmeButton3 = promptViewEdit.findViewById(R.id.f_nethunter_edit_btn_info_fab3);
            readmeButton1.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat);
                adb.setTitle("使用说明:")
                        .setMessage(context.getString(R.string.nethunter_howtouse_cmd))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton2.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat);
                adb.setTitle("使用说明:")
                        .setMessage(context.getString(R.string.nethunter_howtouse_delimiter))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });

            readmeButton3.setOnClickListener(view -> {
                MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat);
                adb.setTitle("使用说明:")
                        .setMessage(context.getString(R.string.nethunter_howtouse_runoncreate))
                        .setNegativeButton("关闭", (dialogInterface, i) -> dialogInterface.dismiss());
                final AlertDialog ad = adb.create();
                ad.setCancelable(true);
                ad.show();
            });
            titleEditText.setText(NethunterData.getInstance().nethunterModelListFull.get(
                    NethunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getTitle());
            cmdEditText.setText(NethunterData.getInstance().nethunterModelListFull.get(
                    NethunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getCommand());
            delimiterEditText.setText(NethunterData.getInstance().nethunterModelListFull.get(
                    NethunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getDelimiter());
            runOnCreateCheckbox.setChecked(NethunterData.getInstance().nethunterModelListFull.get(
                    NethunterData.getInstance().nethunterModelListFull.indexOf(
                            nethunterModelList.get(position))).getRunOnCreate().equals("1"));

            MaterialAlertDialogBuilder adb = new MaterialAlertDialogBuilder(context, R.style.DialogStyleCompat);
            adb.setPositiveButton("应用", (dialog, which) -> { });
            final AlertDialog ad = adb.create();
            ad.setView(promptViewEdit);
            ad.setCancelable(true);
            ad.setOnShowListener(dialog -> {
                final Button buttonEdit = ad.getButton(DialogInterface.BUTTON_POSITIVE);
                buttonEdit.setOnClickListener(v1 -> {
                    if (titleEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "标题不能为空");
                    } else if (cmdEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "命令不能为空");
                    } else if (delimiterEditText.getText().toString().isEmpty()){
                        NhPaths.showMessage(context, "分隔符不能为空");
                    } else {
                        ArrayList<String> dataArrayList = new ArrayList<>();
                        dataArrayList.add(titleEditText.getText().toString());
                        dataArrayList.add(cmdEditText.getText().toString());
                        dataArrayList.add(delimiterEditText.getText().toString());
                        dataArrayList.add(runOnCreateCheckbox.isChecked()?"1":"0");
                        NethunterData.getInstance().editData(NethunterData.getInstance().nethunterModelListFull.indexOf(
                                nethunterModelList.get(position)), dataArrayList, NethunterSQL.getInstance(context));
                        ad.dismiss();
                    }
                });
            });
            ad.show();
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return nethunterModelList.size();
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    @Override
    public Filter getFilter() {
        return NethunterModelListFilter;
    }

    private final Filter NethunterModelListFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            if (constraint == null || constraint.length() == 0){
                results.values = new ArrayList<>(NethunterData.getInstance().nethunterModelListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                List<NethunterModel> tempNethunterModelList = new ArrayList<>();
                for (NethunterModel nethunterModel: NethunterData.getInstance().nethunterModelListFull){
                    if (nethunterModel.getTitle().toLowerCase().contains(filterPattern)){
                        tempNethunterModelList.add(nethunterModel);
                    }
                }
                results.values = tempNethunterModelList;
            }
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Objects.requireNonNull(NethunterData.getInstance().getNethunterModels().getValue()).clear();
            NethunterData.getInstance().getNethunterModels().getValue().addAll((List<NethunterModel>) results.values);
            NethunterData.getInstance().getNethunterModels().postValue(NethunterData.getInstance().getNethunterModels().getValue());
        }
    };

    static class ItemViewHolder extends RecyclerView.ViewHolder{
        private final TextView titleTextView;
        private final RecyclerView resultRecyclerView;
        private final Button runButton;
        //private Button editButton;
        private ItemViewHolder(View view) {
            super(view);
            titleTextView = view.findViewById(R.id.f_nethunter_item_title_tv);
            resultRecyclerView = view.findViewById(R.id.f_nethunter_item_result_recyclerview);
            runButton = view.findViewById(R.id.f_nethunter_item_run_btn);
            //editButton = view.findViewById(R.id.f_nethunter_item_edit_btn);
        }
    }
}
