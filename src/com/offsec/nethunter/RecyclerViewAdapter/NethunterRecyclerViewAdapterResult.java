package com.offsec.nethunter.RecyclerViewAdapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.offsec.nethunter.R;
import com.offsec.nethunter.utils.NhPaths;

public class NethunterRecyclerViewAdapterResult extends RecyclerView.Adapter<NethunterRecyclerViewAdapterResult.ItemViewHolder>{
    // 日志标签
    public static final String TAG = "NethunterRecyclerView";
    private final String[] resultStrings;
    private final Context context;

    public NethunterRecyclerViewAdapterResult(Context context, String[] resultStrings) {
        this.context = context;
        this.resultStrings = resultStrings;
    }

    @NonNull
    @Override
    public NethunterRecyclerViewAdapterResult.ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 加载结果列表项的布局
        View view = LayoutInflater.from(context).inflate(R.layout.nethunter_recyclerview_result, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NethunterRecyclerViewAdapterResult.ItemViewHolder holder, int position) {
        // 设置结果文本
        holder.resultTextView.setText(resultStrings[position]);
        // 长按复制到剪贴板
        holder.resultTextView.setOnLongClickListener(v -> {
            ClipboardManager cm = (ClipboardManager)context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cData = ClipData.newPlainText("text", holder.resultTextView.getText());
            cm.setPrimaryClip(cData);
            // 提示用户已复制
            NhPaths.showMessage(context, "已复制到剪贴板: " + holder.resultTextView.getText());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        // 返回结果数组长度
        return resultStrings.length;
    }

    @Override
    public long getItemId(int position) {
        return super.getItemId(position);
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder{
        private final TextView resultTextView;
        private ItemViewHolder(View view) {
            super(view);
            // 初始化结果文本视图
            resultTextView = view.findViewById(R.id.f_nethunter_item_result_tv);
        }
    }
}
