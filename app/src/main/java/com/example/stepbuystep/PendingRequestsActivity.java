package com.example.stepbuystep;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.adapter.PendingRequestsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PendingRequestsActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout btnBack;
    private RecyclerView rvPendingRequests;
    private View emptyStatePending;

    private PendingRequestsAdapter adapter;
    private long coachIdValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore. getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCoachId();
    }

    private void initViews() {
        btnBack = findViewById(R.id. btnBack);
        rvPendingRequests = findViewById(R.id.rvPendingRequests);
        emptyStatePending = findViewById(R.id.emptyStatePending);
    }

    private void setupRecyclerView() {
        adapter = new PendingRequestsAdapter();
        rvPendingRequests.setLayoutManager(new LinearLayoutManager(this));
        rvPendingRequests.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        adapter.setListener(new PendingRequestsAdapter.OnRequestActionListener() {
            @Override
            public void onApprove(String userId) {
                showApproveConfirmation(userId);
            }

            @Override
            public void onReject(String userId) {
                showRejectConfirmation(userId);
            }
        });
    }

    private void loadCoachId() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long coachId = doc.getLong("coachID");
                        if (coachId != null) {
                            coachIdValue = coachId;
                            loadPendingRequests(coachId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading coach info", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPendingRequests(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading requests", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        List<PendingRequestsAdapter.PendingRequest> requests = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String userId = doc.getId();
                            String email = doc.getString("email");
                            String name = (email != null) ? email.split("@")[0] : "Trainee";
                            if (name.length() > 0) {
                                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                            }
                            String city = doc.getString("city");
                            if (city == null) city = "Unknown";

                            Long ageL = doc.getLong("age");
                            int age = (ageL != null) ? ageL.intValue() : 0;

                            String gender = doc.getString("gender");
                            if (gender == null) gender = "Unknown";

                            requests.add(new PendingRequestsAdapter.PendingRequest(
                                    userId, name, email, city, age, gender
                            ));
                        }

                        adapter.setRequests(requests);

                        // Show/hide empty state
                        if (requests.isEmpty()) {
                            rvPendingRequests.setVisibility(View.GONE);
                            emptyStatePending.setVisibility(View. VISIBLE);
                        } else {
                            rvPendingRequests.setVisibility(View.VISIBLE);
                            emptyStatePending.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void showApproveConfirmation(String userId) {
        new AlertDialog.Builder(this)
                .setTitle("Approve Trainee")
                .setMessage("Are you sure you want to approve this trainee?")
                .setPositiveButton("Approve", (dialog, which) -> approveTrainee(userId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRejectConfirmation(String userId) {
        new AlertDialog. Builder(this)
                .setTitle("Reject Trainee")
                .setMessage("Are you sure you want to reject this trainee?")
                .setPositiveButton("Reject", (dialog, which) -> rejectTrainee(userId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void approveTrainee(String userId) {
        db.collection("users")
                .document(userId)
                .update("status", "approved")
                .addOnSuccessListener(aVoid -> {
                    Toast. makeText(this, "Trainee approved!", Toast.LENGTH_SHORT).show();
                    // Adapter will auto-update via snapshot listener
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to approve trainee", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectTrainee(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("coachID", null); // Remove coach association

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast. makeText(this, "Trainee rejected", Toast.LENGTH_SHORT).show();
                    // Adapter will auto-update via snapshot listener
                })
                .addOnFailureListener(e -> {
                    Toast. makeText(this, "Failed to reject trainee", Toast.LENGTH_SHORT).show();
                });
    }
}