package com.example.stepbuystep;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
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

public class HistoryTraineeActivity extends AppCompatActivity {

    private LinearLayout btnBack;
    private TextView tvTotalWorkouts, tvTotalDistance;
    private TextView tvTotalCalories, tvTotalPoints;
    private RecyclerView rvHistory;
    private View cardEmpty;

    private HistoryAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_trainee);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupRecyclerView();
        fetchHistory();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        tvTotalWorkouts = findViewById(R.id.tvTotalWorkouts);
        tvTotalDistance = findViewById(R.id.tvTotalDistance);
        tvTotalCalories = findViewById(R.id.tvTotalCalories);
        tvTotalPoints = findViewById(R.id.tvTotalPoints);
        rvHistory = findViewById(R.id.rvHistory);
        cardEmpty = findViewById(R.id.cardEmpty);

        btnBack.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new HistoryAdapter();
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(adapter);
    }

    private void fetchHistory() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("training_history")
                .whereEqualTo("userId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<HistoryItem> items = new ArrayList<>();
                    double totalDist = 0;
                    int count = 0;
                    // Note: Calories and Points logic not yet in original code, placeholder or calculate
                    int totalCalories = 0;
                    int totalPoints = 0;

                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        Double dist = doc.getDouble("distance");
                        Long steps = doc.getLong("steps");
                        String date = doc.getString("date");
                        Long timestamp = doc.getLong("timestamp");

                        if (type == null) type = "Workout";
                        if (dist == null) dist = 0.0;
                        if (steps == null) steps = 0L;
                        if (timestamp == null) timestamp = 0L;

                        totalDist += dist;
                        count++;

                        // Simple estimation for UI filling
                        totalCalories += (int)(dist * 60);
                        totalPoints += (int)(dist * 100);

                        String subtitle = String.format("%.2f km • %d steps", dist, steps);
                        String iconType = type.toLowerCase().contains("run") ? "run" : "walk";

                        items.add(new HistoryItem(doc.getId(), type, subtitle, date, timestamp, iconType));
                    }

                    updateUI(items, count, totalDist, totalCalories, totalPoints);
                });
    }

    private void updateUI(List<HistoryItem> items, int count, double totalDist, int calories, int points) {
        tvTotalWorkouts.setText(String.valueOf(count));
        tvTotalDistance.setText(String.format("%.1f", totalDist));

        if (tvTotalCalories != null) tvTotalCalories.setText(String.valueOf(calories));
        if (tvTotalPoints != null) tvTotalPoints.setText(String.valueOf(points));

        if (items.isEmpty()) {
            cardEmpty.setVisibility(View.VISIBLE);
            rvHistory.setVisibility(View.GONE);
        } else {
            cardEmpty.setVisibility(View.GONE);
            rvHistory.setVisibility(View.VISIBLE);
            adapter.setItems(items);
        }
    }
}
