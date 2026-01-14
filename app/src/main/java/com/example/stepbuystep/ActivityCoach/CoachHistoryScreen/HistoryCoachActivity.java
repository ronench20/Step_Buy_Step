package com.example.stepbuystep.ActivityCoach.CoachHistoryScreen;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityCoach.BaseCoachActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.HistoryAdapter;
import com.example.stepbuystep.model. HistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase. firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryCoachActivity extends BaseCoachActivity {

    private RecyclerView rvWorkouts;
    private View cardEmpty;

    private HistoryAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_coach);

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
        adapter = new HistoryAdapter();
        rvWorkouts.setLayoutManager(new LinearLayoutManager(this));
        rvWorkouts.setAdapter(adapter);
    }

    private void fetchCreatedWorkouts() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("workouts")
                .whereEqualTo("coachId", uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<HistoryItem> items = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc. getString("type");
                        String date = doc.getString("date");
                        String time = doc. getString("time");
                        String location = doc.getString("location");
                        Long createdAt = doc.getLong("createdAt");

                        if (type == null) type = "Workout";
                        if (date == null) date = "";
                        if (time == null) time = "";
                        if (location == null) location = "";
                        if (createdAt == null) createdAt = 0L;

                        String subtitle = String.format("%s at %s", time, location);
                        String iconType = type.toLowerCase().contains("run") ? "run" : "walk";

                        items. add(new HistoryItem(doc.getId(), type, subtitle, date, createdAt, iconType));
                    }

                    updateUI(items);
                });
    }

    private void updateUI(List<HistoryItem> items) {
        if (items.isEmpty()) {
            cardEmpty.setVisibility(View.VISIBLE);
            rvWorkouts.setVisibility(View.GONE);
        } else {
            cardEmpty.setVisibility(View. GONE);
            rvWorkouts.setVisibility(View. VISIBLE);
            adapter.setItems(items);
        }
    }
}