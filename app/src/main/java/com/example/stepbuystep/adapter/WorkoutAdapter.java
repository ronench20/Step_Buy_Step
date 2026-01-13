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

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.ViewHolder> {

    public static class WorkoutItem {
        public String type;
        public String date;
        public String time;
        public String location;

        public WorkoutItem(String type, String date, String time, String location) {
            this.type = type;
            this.date = date;
            this.time = time;
            this.location = location;
        }
    }

    private List<WorkoutItem> workouts = new ArrayList<>();

    public void setWorkouts(List<WorkoutItem> workouts) {
        this.workouts = workouts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent. getContext())
                .inflate(R.layout.item_workout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WorkoutItem workout = workouts.get(position);
        holder.tvType.setText(workout.type);
        holder.tvDateTime.setText(workout.date + " at " + workout.time);
        holder.tvLocation.setText(workout.location);
    }

    @Override
    public int getItemCount() {
        return workouts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvType, tvDateTime, tvLocation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvType = itemView.findViewById(R.id.tvWorkoutType);
            tvDateTime = itemView.findViewById(R.id.tvWorkoutDateTime);
            tvLocation = itemView.findViewById(R.id.tvWorkoutLocation);
        }
    }
}