package com.offsec.nethunter.RecyclerViewAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.offsec.nethunter.R;
import com.offsec.nethunter.models.CustomCommandsModel;

import java.util.List;

public class CustomCommandsRecyclerViewAdapterDeleteItems extends RecyclerView.Adapter<CustomCommandsRecyclerViewAdapterDeleteItems.ItemViewHolder>{
	public static final String TAG = "CustomCommandsRVA_Delete";
	private final Context context;
	private final List<CustomCommandsModel> customCommandsModelList;

	public CustomCommandsRecyclerViewAdapterDeleteItems(Context context, List<CustomCommandsModel> customCommandsModelList){
		this.context = context;
		this.customCommandsModelList = customCommandsModelList;
	}

	@NonNull
	@Override
	public CustomCommandsRecyclerViewAdapterDeleteItems.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
		View view = LayoutInflater.from(context).inflate(R.layout.customcommands_recyclerview_dialog_delete, viewGroup, false);
		return new ItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final ItemViewHolder itemViewHolder, int i) {
		// 用命令标签作为复选框文字
		itemViewHolder.runOnChrootStartCheckBox.setText(customCommandsModelList.get(i).getCommandLabel());
	}

	@Override
	public int getItemCount() {
		return customCommandsModelList.size();
	}

	static class ItemViewHolder extends RecyclerView.ViewHolder{
		private final CheckBox runOnChrootStartCheckBox;
		private ItemViewHolder(View view){
			super(view);
			runOnChrootStartCheckBox = view.findViewById(R.id.f_customcommands_recyclerview_dialog_chkbox);
		}
	}
}