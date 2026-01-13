package com.example.stepbuystep.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;
import com.example.stepbuystep.model.Equipment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {

    private List<Equipment> items = new ArrayList<>();
    private long currentCoins = 0;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onPurchaseClick(Equipment item);
    }

    public void setListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Equipment> items, long currentCoins) {
        this.items = items;
        this.currentCoins = currentCoins;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shoe_upgrade, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Equipment item = items.get(position);

        holder.tvShoeName.setText(item.getName());
        holder.chipLevel.setText("Level " + item.getTier());
        holder.chipMultiplier.setText(item.getMultiplier() + "x");
        holder.tvCost.setText(item.getPrice() + " coins"); // Renamed "shoe size" to coins in Java logic

        boolean canAfford = currentCoins >= item.getPrice();

        // Reset visibility
        holder.btnUpgrade.setVisibility(View.GONE);
        holder.layoutNeedMore.setVisibility(View.GONE);
        holder.ivWarning.setVisibility(View.GONE);
        holder.cardShoe.setStrokeWidth(0);

        // Icon tint
        if (canAfford) {
            holder.ivIcon.setColorFilter(Color.parseColor("#34C759")); // Green
            holder.btnUpgrade.setVisibility(View.VISIBLE);
            holder.btnUpgrade.setOnClickListener(v -> {
                if (listener != null) listener.onPurchaseClick(item);
            });
        } else {
            holder.ivIcon.setColorFilter(Color.parseColor("#9CA3AF")); // Gray
            holder.layoutNeedMore.setVisibility(View.VISIBLE);
            holder.ivWarning.setVisibility(View.VISIBLE);

            // Calc difference
            long diff = item.getPrice() - currentCoins;
            holder.tvNeedMore.setText("Need " + diff + " more coins");

            int progress = 0;
            if (item.getPrice() > 0) {
                progress = (int) ((currentCoins * 100) / item.getPrice());
            }
            holder.progressAfford.setProgress(progress);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShoeName, tvCost, tvNeedMore;
        Chip chipLevel, chipMultiplier;
        ImageView ivIcon, ivWarning;
        MaterialButton btnUpgrade;
        LinearLayout layoutNeedMore;
        MaterialCardView cardShoe;
        LinearProgressIndicator progressAfford;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvShoeName = itemView.findViewById(R.id.tvShoeName);
            tvCost = itemView.findViewById(R.id.tvCost);
            tvNeedMore = itemView.findViewById(R.id.tvNeedMore);
            chipLevel = itemView.findViewById(R.id.chipLevel);
            chipMultiplier = itemView.findViewById(R.id.chipMultiplier);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            ivWarning = itemView.findViewById(R.id.ivWarning);
            btnUpgrade = itemView.findViewById(R.id.btnUpgrade);
            layoutNeedMore = itemView.findViewById(R.id.layoutNeedMore);
            cardShoe = itemView.findViewById(R.id.cardShoe);
            progressAfford = itemView.findViewById(R.id.progressAfford);
        }
    }
}
