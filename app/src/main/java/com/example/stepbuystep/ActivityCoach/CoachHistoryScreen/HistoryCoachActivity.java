package com.example.stepbuystep.ActivityCoach.CoachHistoryScreen;

import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityCoach.BaseCoachActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.UpcomingWorkoutAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase. firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryCoachActivity extends BaseCoachActivity {

    private RecyclerView rvWorkouts;
    private View cardEmpty;

    private UpcomingWorkoutAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_history);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth. getInstance();

        initViews();
        setupNavigationBar(NavItem.MY_HISTORY);  // Highlight "My History"
        setupRecyclerView();
        fetchCreatedWorkouts();
    }

    private void initViews() {
        rvWorkouts = findViewById(R.id.rvWorkouts);
        cardEmpty = findViewById(R.id.cardEmpty);

    }

    private void setupRecyclerView() {
        adapter = new UpcomingWorkoutAdapter();
        rvWorkouts.setLayoutManager(new LinearLayoutManager(this));
        rvWorkouts.setAdapter(adapter);
    }

    private void fetchCreatedWorkouts() {
        String uid = auth.getUid();
        if (uid == null) return;

        // Note: Using a snapshot listener might be better for real-time, 
        // but for history a one-shot get is usually fine.
        db.collection("workouts")
                .whereEqualTo("coachId", uid)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<UpcomingWorkoutAdapter.WorkoutItem> items = new ArrayList<>();
                    long now = System.currentTimeMillis();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String location = doc.getString("location");

                        if (type == null) type = "Workout";
                        if (date == null) date = "";
                        if (time == null) time = "";
                        if (location == null) location = "";

                        // Only include past workouts
                        long workoutTime = parseWorkoutDateTime(date, time);
                        if (workoutTime >= now) continue;

                        int participants = 0;
                        Object traineeIds = doc.get("traineeIds");
                        if (traineeIds instanceof List) {
                            participants = ((List<?>) traineeIds).size();
                        }

                        items.add(new UpcomingWorkoutAdapter.WorkoutItem(
                                doc.getId(), type, date, time, location, participants));
                    }

                    // Sort manually by date descending if we can't use Firestore orderBy yet
                    items.sort((a, b) -> Long.compare(parseWorkoutDateTime(b.date, b.time), parseWorkoutDateTime(a.date, a.time)));

                    updateUI(items);
                })
                .addOnFailureListener(e -> {
                    // Log error for debugging
                    android.util.Log.e("HistoryCoachActivity", "Error fetching workouts", e);
                });
    }

    private static long parseWorkoutDateTime(String date, String time) {
        if (date == null || date.isEmpty()) return Long.MAX_VALUE;
        String timePart = (time == null || time.isEmpty()) ? "00:00" : time;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date d = fmt.parse(date + " " + timePart);
            return d != null ? d.getTime() : Long.MAX_VALUE;
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    private void updateUI(List<UpcomingWorkoutAdapter.WorkoutItem> items) {
        if (items.isEmpty()) {
            cardEmpty.setVisibility(View.VISIBLE);
            rvWorkouts.setVisibility(View.GONE);
        } else {
            cardEmpty.setVisibility(View.GONE);
            rvWorkouts.setVisibility(View.VISIBLE);
            adapter.setItems(items);
        }
    }
}