package com.offsec.nethunter.RecyclerViewAdapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.offsec.nethunter.R;
import com.offsec.nethunter.models.NethunterModel;

import java.util.List;


public class NethunterRecyclerViewAdapterDeleteItems extends RecyclerView.Adapter<NethunterRecyclerViewAdapterDeleteItems.ItemViewHolder>{
	// 日志标签
	public static final String TAG = "NethunterRecyclerView";
	private final Context context;
	private final List<NethunterModel> nethunterModelList;

	public NethunterRecyclerViewAdapterDeleteItems(Context context, List<NethunterModel> nethunterModelList){
		this.context = context;
		this.nethunterModelList = nethunterModelList;
	}

	@NonNull
	@Override
	public NethunterRecyclerViewAdapterDeleteItems.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
		// 加载删除确认对话框的布局
		View view = LayoutInflater.from(context).inflate(R.layout.nethunter_recyclerview_dialog_delete, viewGroup, false);
		return new ItemViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final ItemViewHolder itemViewHolder, int i) {
		// 设置复选框显示的文本为模型中的标题
		itemViewHolder.checkBox.setText(nethunterModelList.get(i).getTitle());
	}

	@Override
	public int getItemCount() {
		// 返回列表中项目的总数
		return nethunterModelList.size();
	}

	static class ItemViewHolder extends RecyclerView.ViewHolder{
		private final CheckBox checkBox;
		private ItemViewHolder(View view){
			super(view);
			// 初始化复选框
			checkBox = view.findViewById(R.id.f_nethunter_recyclerview_dialog_chkbox);
		}
	}
}
