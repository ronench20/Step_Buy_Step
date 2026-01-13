package com.example.stepbuystep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;

import java.util.ArrayList;
import java.util.List;

public class UpcomingWorkoutAdapter extends RecyclerView.Adapter<UpcomingWorkoutAdapter.ViewHolder> {

    private List<WorkoutItem> items = new ArrayList<>();

    public static class WorkoutItem {
        public String id;
        public String type;
        public String date;
        public String time;
        public String location;
        public int participants;

        public WorkoutItem(String id, String type, String date, String time, String location, int participants) {
            this.id = id;
            this.type = type;
            this.date = date;
            this.time = time;
            this.location = location;
            this.participants = participants;
        }
    }

    public void setItems(List<WorkoutItem> items) {
        this.items = items;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upcoming_workout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutItem item = items.get(position);

        holder.badgeType.setText(item.type);
        holder.tvDate.setText(item.date);
        holder.tvTime.setText(item.time);
        holder.tvLocation.setText(item.location);
        holder.tvParticipants.setText(item.participants + " participants");

        // Customize badge color based on type if needed
        if (item.type.equalsIgnoreCase("run") || item.type.equalsIgnoreCase("running")) {
             holder.badgeType.setBackgroundResource(R.drawable.bg_badge_blue);
        } else {
             holder.badgeType.setBackgroundResource(R.drawable.bg_badge_green);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView badgeType, tvDate, tvTime, tvLocation, tvParticipants;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            badgeType = itemView.findViewById(R.id.badgeType);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvParticipants = itemView.findViewById(R.id.tvParticipants);
        }
    }
}
