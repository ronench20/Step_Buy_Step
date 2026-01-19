package com.example.stepbuystep.adapter;

import android. graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget. LinearLayout;
import android.widget. TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;
import com.example.stepbuystep.model.ShoeLevel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.chip.Chip;
import com. google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ViewHolder> {

    private List<ShoeLevel> shoes = new ArrayList<>();
    private long currentCoins = 0;
    private OnShoeClickListener listener;

    public interface OnShoeClickListener {
        void onPurchaseClick(ShoeLevel shoe);
    }

    public void setListener(OnShoeClickListener listener) {
        this.listener = listener;
    }

    public void setShoes(List<ShoeLevel> shoes, long currentCoins) {
        this.shoes = shoes;
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
        ShoeLevel shoe = shoes.get(position);

        holder.tvShoeName.setText(shoe.getName());
        holder.chipLevel.setText("Level " + shoe.getLevel());
        holder.chipMultiplier.setText(shoe.getMultiplier() + "x");
        holder.tvCost.setText(shoe.getPrice() + " coins");

        // Reset all views
        holder.btnUpgrade.setVisibility(View.GONE);
        holder.layoutNeedMore.setVisibility(View. GONE);
        holder.ivWarning.setVisibility(View. GONE);
        holder.chipUnlocked.setVisibility(View. GONE);
        holder.chipActive.setVisibility(View. GONE);
        holder.cardShoe.setStrokeWidth(0);

        // Apply state-specific styling
        switch (shoe.getState()) {
            case OWNED:
                applyOwnedStyle(holder);
                break;

            case CURRENT:
                applyCurrentStyle(holder);
                break;

            case NEXT:
                applyNextStyle(holder, shoe);
                break;

            case LOCKED:
                applyLockedStyle(holder);
                break;
        }
    }

    private void applyOwnedStyle(ViewHolder holder) {
        // Grey/faded style
        holder.ivIcon.setColorFilter(Color.parseColor("#9CA3AF"));
        holder.ivIcon.setAlpha(0.5f);
        holder.cardShoe.setAlpha(0.6f);
        holder.chipUnlocked.setVisibility(View.VISIBLE);
    }

    private void applyCurrentStyle(ViewHolder holder) {
        // Marked/highlighted style
        holder.ivIcon. setColorFilter(Color.parseColor("#007AFF")); // Blue
        holder.cardShoe.setStrokeColor(Color.parseColor("#007AFF"));
        holder.cardShoe.setStrokeWidth(4);
        holder.chipActive.setVisibility(View. VISIBLE);
    }

    private void applyNextStyle(ViewHolder holder, ShoeLevel shoe) {
        // Normal with progress bar
        boolean canAfford = currentCoins >= shoe. getPrice();

        if (canAfford) {
            holder.ivIcon.setColorFilter(Color. parseColor("#34C759")); // Green
            holder.btnUpgrade.setVisibility(View.VISIBLE);
            holder. btnUpgrade.setOnClickListener(v -> {
                if (listener != null) listener.onPurchaseClick(shoe);
            });
        } else {
            holder.ivIcon. setColorFilter(Color.parseColor("#F97316")); // Orange
            holder.layoutNeedMore.setVisibility(View.VISIBLE);
            holder.ivWarning.setVisibility(View.VISIBLE);

            long diff = shoe.getPrice() - currentCoins;
            holder.tvNeedMore.setText("Need " + diff + " more coins");

            int progress = shoe.getPrice() > 0 ?
                    (int) ((currentCoins * 100) / shoe.getPrice()) : 0;
            holder.progressAfford.setProgress(progress);
        }
    }

    private void applyLockedStyle(ViewHolder holder) {
        // Locked style
        holder.ivIcon. setColorFilter(Color.parseColor("#D1D5DB")); // Light grey
        holder.ivIcon.setAlpha(0.3f);
        holder.cardShoe.setAlpha(0.5f);
        holder.tvShoeName.setAlpha(0.5f);
        holder.chipLevel.setAlpha(0.5f);
        holder.chipMultiplier.setAlpha(0.5f);
        holder.tvCost.setAlpha(0.5f);
    }

    @Override
    public int getItemCount() {
        return shoes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvShoeName, tvCost, tvNeedMore;
        Chip chipLevel, chipMultiplier, chipUnlocked, chipActive;
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
            chipUnlocked = itemView.findViewById(R.id.chipUnlocked);
            chipActive = itemView.findViewById(R.id.chipActive);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            ivWarning = itemView. findViewById(R.id.ivWarning);
            btnUpgrade = itemView.findViewById(R.id.btnUpgrade);
            layoutNeedMore = itemView.findViewById(R.id.layoutNeedMore);
            cardShoe = itemView.findViewById(R.id.cardShoe);
            progressAfford = itemView.findViewById(R.id.progressAfford);
        }
    }
}