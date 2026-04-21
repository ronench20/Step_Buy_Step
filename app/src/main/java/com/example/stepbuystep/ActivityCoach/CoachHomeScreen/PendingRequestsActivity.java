package com.example.stepbuystep.ActivityCoach.CoachHomeScreen;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.PendingRequestsAdapter;
import com.example.stepbuystep.model.CoachSubscriptionHelper;
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
    private int maxAthletes = 20;
    private int currentAthletes = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_requests);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCoachId();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
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
        String uid = auth.getCurrentUser().getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long coachId = doc.getLong("coachID");
                        if (coachId != null) {
                            coachIdValue = coachId;
                            loadPendingRequests(coachId);

                            // ===== USE THE HELPER CLASS =====
                            CoachSubscriptionHelper.loadCoachSubscriptionAndCount(
                                    uid, coachId, db,
                                    new CoachSubscriptionHelper.OnSubscriptionLoadListener() {
                                        @Override
                                        public void onSubscriptionLoaded(int max, int current) {
                                            maxAthletes = max;
                                            currentAthletes = current;
                                        }

                                        @Override
                                        public void onError(String error) {
                                            Toast.makeText(PendingRequestsActivity.this,
                                                    "Error loading subscription", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                            // ===== END HELPER =====
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

                            // Profile image URL written during trainee registration.
                            // Null/empty => the adapter falls back to initials.
                            String profileImageUrl = doc.getString("profileImageUrl");

                            requests.add(new PendingRequestsAdapter.PendingRequest(
                                    userId, name, email, city, age, gender, profileImageUrl
                            ));
                        }

                        adapter.setRequests(requests);

                        if (requests.isEmpty()) {
                            rvPendingRequests.setVisibility(View.GONE);
                            emptyStatePending.setVisibility(View.VISIBLE);
                        } else {
                            rvPendingRequests.setVisibility(View.VISIBLE);
                            emptyStatePending.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void showApproveConfirmation(String userId) {
        if (currentAthletes >= maxAthletes) {
            showTierLimitDialog();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Approve Trainee")
                .setMessage("Are you sure you want to approve this trainee?")
                .setPositiveButton("Approve", (dialog, which) -> approveTrainee(userId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTierLimitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Tier Limit Reached")
                .setMessage("You have reached the maximum number of athletes (" + maxAthletes + ") for your current tier.\n\n" +
                        "To add more athletes, upgrade your subscription in Settings.")
                .setPositiveButton("Got it", null)
                .show();
    }

    private void showRejectConfirmation(String userId) {
        new AlertDialog.Builder(this)
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
                    Toast.makeText(this, "Trainee approved!", Toast.LENGTH_SHORT).show();
                    currentAthletes++;
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to approve trainee", Toast.LENGTH_SHORT).show();
                });
    }

    private void rejectTrainee(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "rejected");
        updates.put("coachID", null);

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Trainee rejected", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to reject trainee", Toast.LENGTH_SHORT).show();
                });
    }
}