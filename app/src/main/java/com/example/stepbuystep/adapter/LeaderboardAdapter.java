package com.example.stepbuystep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private List<LeaderboardItem> items = new ArrayList<>();
    private OnItemClickListener listener;
    private String currentUserId = "";

    public interface OnItemClickListener {
        void onItemClick(LeaderboardItem item);
    }

    public static class LeaderboardItem {
        public String id;
        public String name;
        public int rank;
        public int level; // or shoe level
        public double multiplier;
        public String city;

        public LeaderboardItem(String id, String name, int rank, int level, double multiplier, String city) {
            this.id = id;
            this.name = name;
            this.rank = rank;
            this.level = level;
            this.multiplier = multiplier;
            this.city = city;
        }
    }

    public void setItems(List<LeaderboardItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    public void setCurrentUserId(String uid) {
        this.currentUserId = uid;
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard_athlete, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardItem item = items.get(position);

        holder.tvName.setText(item.name);
        holder.tvCity.setText(item.city);
        holder.tvLevel.setText("Lvl " + item.level);
        holder.tvMultiplier.setText(item.multiplier + "x multiplier");

        // Rank logic
        if (item.rank <= 3) {
            holder.ivRankIcon.setVisibility(View.VISIBLE);
            holder.tvRankNumber.setVisibility(View.GONE);
            // Set color based on rank? For now default trophy tint (gold)
            if (item.rank == 1) holder.ivRankIcon.setColorFilter(0xFFF5B301); // Gold
            else if (item.rank == 2) holder.ivRankIcon.setColorFilter(0xFFC0C0C0); // Silver
            else holder.ivRankIcon.setColorFilter(0xFFCD7F32); // Bronze
        } else {
            holder.ivRankIcon.setVisibility(View.GONE);
            holder.tvRankNumber.setVisibility(View.VISIBLE);
            holder.tvRankNumber.setText(String.valueOf(item.rank));
        }

        // Avatar
        String initial = item.name.isEmpty() ? "?" : item.name.substring(0, 1).toUpperCase();
        holder.tvAvatar.setText(initial);

        // "You" badge
        if (item.id.equals(currentUserId)) {
            holder.badgeYou.setVisibility(View.VISIBLE);
        } else {
            holder.badgeYou.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRankNumber, tvName, tvCity, tvLevel, tvMultiplier, tvAvatar, badgeYou;
        ImageView ivRankIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRankNumber = itemView.findViewById(R.id.tvRankNumber);
            ivRankIcon = itemView.findViewById(R.id.ivRankIcon);
            tvName = itemView.findViewById(R.id.tvName);
            tvCity = itemView.findViewById(R.id.tvCity);
            tvLevel = itemView.findViewById(R.id.tvLevel);
            tvMultiplier = itemView.findViewById(R.id.tvMultiplier);
            tvAvatar = itemView.findViewById(R.id.tvAvatar);
            badgeYou = itemView.findViewById(R.id.badgeYou);
        }
    }
}
