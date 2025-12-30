package com.example.stepbuystep;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.adapter.HistoryAdapter;
import com.example.stepbuystep.model.HistoryItem;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class HistoryCoachActivity extends AppCompatActivity {

    private Button btnBack;
    private TextView tvTotalCreated;
    private RecyclerView rvHistory;
    private View emptyStateCard;

    private HistoryAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_coach);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        fetchCreatedWorkouts();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTotalCreated = findViewById(R.id.tvTotalCreated);
        rvHistory = findViewById(R.id.rvHistory);
        emptyStateCard = findViewById(R.id.emptyStateCard);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
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
                    int count = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String location = doc.getString("location");
                        Long createdAt = doc.getLong("createdAt");

                        if (type == null) type = "Workout";
                        if (date == null) date = "";
                        if (time == null) time = "";
                        if (location == null) location = "";
                        if (createdAt == null) createdAt = 0L;

                        count++;

                        String subtitle = String.format("%s at %s", time, location);
                        String iconType = type.toLowerCase().contains("run") ? "run" : "walk";

                        items.add(new HistoryItem(doc.getId(), type, subtitle, date, createdAt, iconType));
                    }

                    updateUI(items, count);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show();
                    emptyStateCard.setVisibility(View.VISIBLE);
                });
    }

    private void updateUI(List<HistoryItem> items, int count) {
        tvTotalCreated.setText(String.valueOf(count));

        if (items.isEmpty()) {
            emptyStateCard.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            emptyStateCard.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            adapter.setItems(items);
        }
    }
}
