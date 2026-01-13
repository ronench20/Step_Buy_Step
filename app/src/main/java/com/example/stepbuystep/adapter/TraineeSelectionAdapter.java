package com.example.stepbuystep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;

import java.util.ArrayList;
import java.util.List;

public class TraineeSelectionAdapter extends RecyclerView. Adapter<TraineeSelectionAdapter.ViewHolder> {

    public static class TraineeItem {
        public String id;
        public String name;
        public String city;
        public boolean isSelected;

        public TraineeItem(String id, String name, String city) {
            this.id = id;
            this.name = name;
            this.city = city;
            this.isSelected = false;
        }
    }

    private List<TraineeItem> trainees = new ArrayList<>();

    public void setTrainees(List<TraineeItem> trainees) {
        this.trainees = trainees;
        notifyDataSetChanged();
    }

    public List<String> getSelectedTraineeIds() {
        List<String> selected = new ArrayList<>();
        for (TraineeItem trainee : trainees) {
            if (trainee.isSelected) {
                selected.add(trainee.id);
            }
        }
        return selected;
    }

    public void deselectAll() {
        for (TraineeItem trainee : trainees) {
            trainee. isSelected = false;
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trainee_selection, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TraineeItem trainee = trainees.get(position);
        holder.tvName.setText(trainee.name);
        holder.tvCity. setText(trainee.city);
        holder.checkbox.setChecked(trainee. isSelected);

        holder.itemView.setOnClickListener(v -> {
            trainee.isSelected = !trainee.isSelected;
            holder.checkbox.setChecked(trainee.isSelected);
        });

        holder.checkbox.setOnClickListener(v -> {
            trainee.isSelected = holder.checkbox.isChecked();
        });
    }

    @Override
    public int getItemCount() {
        return trainees.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCity;
        CheckBox checkbox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTraineeName);
            tvCity = itemView.findViewById(R.id. tvTraineeCity);
            checkbox = itemView.findViewById(R.id.checkboxTrainee);
        }
    }
    public List<String> getAllTraineeIds() {
        List<String> allIds = new ArrayList<>();
        for (TraineeItem trainee : trainees) {
            allIds.add(trainee.id);
        }
        return allIds;
    }
}