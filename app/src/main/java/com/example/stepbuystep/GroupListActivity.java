package com.example.stepbuystep;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class GroupListActivity extends ComponentActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListView lvTrainees;
    private ArrayAdapter<String> adapter;
    private List<String> traineeNames = new ArrayList<>();
    private List<String> traineeIds = new ArrayList<>();
    private String currentUserRole = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_list);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        lvTrainees = findViewById(R.id.lvTrainees);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, traineeNames);
        lvTrainees.setAdapter(adapter);

        lvTrainees.setOnItemClickListener((parent, view, position, id) -> {
            if ("coach".equals(currentUserRole)) {
                String traineeId = traineeIds.get(position);
                showMarkAttendanceDialog(traineeId);
            }
        });

        loadUserData();
    }

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                Long coachId = doc.getLong("coachID");
                currentUserRole = doc.getString("role");

                if (coachId != null) {
                    fetchTrainees(coachId);
                }
            }
        }).addOnFailureListener(e -> Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show());
    }

    private void fetchTrainees(Long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    traineeNames.clear();
                    traineeIds.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String email = doc.getString("email");
                        String name = (email != null) ? email.split("@")[0] : "Trainee";

                        // Optional: Fetch item count/stats for leaderboard
                        // For now just name

                        traineeNames.add(name);
                        traineeIds.add(doc.getId());
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void showMarkAttendanceDialog(String traineeId) {
        new AlertDialog.Builder(this)
                .setTitle("Mark Attendance")
                .setMessage("Award coins for attendance?")
                .setPositiveButton("Award 50 Coins", (dialog, which) -> {
                     awardCoins(traineeId, 50);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void awardCoins(String traineeId, int amount) {
        db.collection("users").document(traineeId)
                .update("coin_balance", com.google.firebase.firestore.FieldValue.increment(amount))
                .addOnSuccessListener(aVoid -> Toast.makeText(this, "Coins awarded!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
