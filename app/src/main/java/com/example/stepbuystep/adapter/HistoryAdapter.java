package com.example.stepbuystep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.stepbuystep.R;
import com.example.stepbuystep.model.HistoryItem;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> items = new ArrayList<>();

    public void setItems(List<HistoryItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvSubtitle;
        private final TextView tvDate;
        private final ImageView ivIcon;
        private final View iconBackground;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubtitle = itemView.findViewById(R.id.tvSubtitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            iconBackground = itemView.findViewById(R.id.iconBackground);
        }

        public void bind(HistoryItem item) {
            tvTitle.setText(item.getTitle());
            tvSubtitle.setText(item.getSubtitle());
            tvDate.setText(item.getDate());

            // Set Icon based on type
            if ("run".equalsIgnoreCase(item.getIconType())) {
                ivIcon.setImageResource(R.drawable.ic_running);
                iconBackground.setBackgroundResource(R.drawable.circle_blue_light);
            } else if ("walk".equalsIgnoreCase(item.getIconType())) {
                ivIcon.setImageResource(R.drawable.ic_walking);
                iconBackground.setBackgroundResource(R.drawable.circle_green_light);
            } else {
                ivIcon.setImageResource(R.drawable.ic_trending_up); // Default
                iconBackground.setBackgroundResource(R.drawable.circle_blue_light);
            }
        }
    }
}
