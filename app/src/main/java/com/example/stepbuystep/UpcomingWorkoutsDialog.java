package com.example.stepbuystep;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx. recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.adapter.WorkoutAdapter;
import com.google.android.material.bottomsheet. BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class UpcomingWorkoutsDialog extends BottomSheetDialogFragment {

    private RecyclerView rvDialogWorkouts;
    private LinearLayout emptyStateDialog;
    private ImageView btnCloseDialog;
    private WorkoutAdapter workoutAdapter;
    private List<WorkoutAdapter.WorkoutItem> workouts;

    public static UpcomingWorkoutsDialog newInstance(List<WorkoutAdapter.WorkoutItem> workouts) {
        UpcomingWorkoutsDialog dialog = new UpcomingWorkoutsDialog();
        dialog.workouts = workouts;
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R. layout.dialog_upcoming_workouts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvDialogWorkouts = view. findViewById(R.id.rvDialogWorkouts);
        emptyStateDialog = view.findViewById(R.id.emptyStateDialog);
        btnCloseDialog = view.findViewById(R.id. btnCloseDialog);

        btnCloseDialog.setOnClickListener(v -> dismiss());

        workoutAdapter = new WorkoutAdapter();
        rvDialogWorkouts.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDialogWorkouts.setAdapter(workoutAdapter);

        if (workouts != null && ! workouts.isEmpty()) {
            workoutAdapter.setWorkouts(workouts);
            rvDialogWorkouts.setVisibility(View.VISIBLE);
            emptyStateDialog.setVisibility(View.GONE);
        } else {
            rvDialogWorkouts.setVisibility(View.GONE);
            emptyStateDialog.setVisibility(View.VISIBLE);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new BottomSheetDialog(requireContext(), getTheme());
    }
}