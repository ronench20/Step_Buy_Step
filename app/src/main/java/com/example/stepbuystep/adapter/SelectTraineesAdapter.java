package com.example.stepbuystep.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;
import com.example.stepbuystep.ActivityCoach.CoachSettingsScreen.CreateSubGroupActivity;

import java.util.ArrayList;
import java.util.List;

public class SelectTraineesAdapter extends RecyclerView.Adapter<SelectTraineesAdapter.TraineeViewHolder> {

    private List<CreateSubGroupActivity.TraineeForSelection> trainees;
    private List<String> selectedTraineeIds;

    public SelectTraineesAdapter(List<CreateSubGroupActivity.TraineeForSelection> trainees) {
        this.trainees = trainees;
        this.selectedTraineeIds = new ArrayList<>();
    }

    @NonNull
    @Override
    public TraineeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_select_trainee, parent, false);
        return new TraineeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TraineeViewHolder holder, int position) {
        CreateSubGroupActivity.TraineeForSelection trainee = trainees.get(position);

        holder.tvTraineeName.setText(trainee.name);
        holder.tvTraineeEmail.setText(trainee.email);

        // Set checkbox state
        holder.cbSelectTrainee.setChecked(selectedTraineeIds.contains(trainee.id));

        // Handle checkbox clicks
        holder.cbSelectTrainee.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedTraineeIds.add(trainee.id);
            } else {
                selectedTraineeIds.remove(trainee.id);
            }
        });

        // Allow clicking the whole row to toggle checkbox
        holder.itemView.setOnClickListener(v -> {
            holder.cbSelectTrainee.toggle();
        });
    }

    @Override
    public int getItemCount() {
        return trainees.size();
    }

    public List<String> getSelectedTraineeIds() {
        return selectedTraineeIds;
    }

    public static class TraineeViewHolder extends RecyclerView.ViewHolder {
        TextView tvTraineeName;
        TextView tvTraineeEmail;
        CheckBox cbSelectTrainee;

        public TraineeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTraineeName = itemView.findViewById(R.id.tvTraineeName);
            tvTraineeEmail = itemView.findViewById(R.id.tvTraineeEmail);
            cbSelectTrainee = itemView.findViewById(R.id.cbSelectTrainee);
        }
    }
}