package com.example.stepbuystep.ActivityCoach.CoachSettingsScreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.TeamMembersAdapter;
import com.example.stepbuystep.model.SubscriptionTier;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageTeamMembersActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout btnBack;
    private RecyclerView rvTeamMembers;
    private View emptyStateTeam;
    private TextView tvTeamCount;

    private TeamMembersAdapter adapter;
    private long coachIdValue = 0;

    private String pendingTierDowngrade = null;
    private int pendingTierMaxAthletes = 0;
    private int athletesRemovedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_team_members);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            pendingTierDowngrade = intent.getStringExtra("pendingTierDowngrade");
            pendingTierMaxAthletes = intent.getIntExtra("pendingTierMaxAthletes", 0);
        }

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCoachId();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvTeamMembers = findViewById(R.id.rvTeamMembers);
        emptyStateTeam = findViewById(R.id.emptyStateTeam);
        tvTeamCount = findViewById(R.id.tvTeamCount);
    }

    private void setupRecyclerView() {
        adapter = new TeamMembersAdapter();
        rvTeamMembers.setLayoutManager(new LinearLayoutManager(this));
        rvTeamMembers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> {
            // Back always just closes this screen. The tier downgrade is no longer
            // auto-completed from here — it must be explicitly re-confirmed from the
            // subscription dialog, which re-validates capacity against Firestore.
            finish();
        });

        adapter.setListener((userId, name) -> showRemoveConfirmation(userId, name));
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
                            loadTeamMembers(coachId);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading coach info", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadTeamMembers(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Error loading team members", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        List<TeamMembersAdapter.TeamMember> members = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String userId = doc.getId();
                            String email = doc.getString("email");
                            String name = (email != null) ? email.split("@")[0] : "Trainee";
                            if (name.length() > 0) {
                                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                            }
                            String city = doc.getString("city");
                            if (city == null) city = "Unknown";

                            // Profile image URL written during trainee registration /
                            // dashboard edits. Null/empty => the adapter falls back to initials.
                            String profileImageUrl = doc.getString("profileImageUrl");

                            members.add(new TeamMembersAdapter.TeamMember(
                                    userId, name, email, city, profileImageUrl
                            ));
                        }

                        adapter.setMembers(members);
                        tvTeamCount.setText(String.valueOf(members.size()));

                        // Show/hide empty state
                        if (members.isEmpty()) {
                            rvTeamMembers.setVisibility(View.GONE);
                            emptyStateTeam.setVisibility(View.VISIBLE);
                        } else {
                            rvTeamMembers.setVisibility(View.VISIBLE);
                            emptyStateTeam.setVisibility(View.GONE);
                        }

                        if (pendingTierDowngrade != null) {
                            checkDowngradeComplete(members.size());
                        }
                    }
                });
    }

    private void showRemoveConfirmation(String userId, String name) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Team Member")
                .setMessage("Are you sure you want to remove " + name + " from your team?\n")
                .setPositiveButton("Remove", (dialog, which) -> removeTeamMember(userId, name))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void removeTeamMember(String userId, String name) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", "removed");
        updates.put("coachID", null);

        db.collection("users")
                .document(userId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    athletesRemovedCount++;  // ===== NEW: Track removals =====
                    Toast.makeText(this, name + " has been removed from your team", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove team member", Toast.LENGTH_SHORT).show();
                });
    }

    private void checkDowngradeComplete(int currentMemberCount) {
        if (currentMemberCount <= pendingTierMaxAthletes) {
            // Enough athletes removed!
            Toast.makeText(this, "You can now complete the tier downgrade!", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Writes the new (lower) subscription tier to the coach's user document.
     *
     * Capacity is re-validated here even though the subscription dialog also
     * checks it — this is the final gate before the Firestore write and closes
     * any race window between removing athletes and confirming the downgrade.
     * If the coach still exceeds the target tier's cap, the update is aborted
     * and the user is told exactly how many more athletes must be removed.
     */
    private void completeTierDowngrade() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();
        if (uid == null) return;
        if (pendingTierDowngrade == null) return;

        SubscriptionTier tier = SubscriptionTier.getTierByName(pendingTierDowngrade);

        // Re-fetch the authoritative approved-athlete count before writing.
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachIdValue)
                .whereEqualTo("status", "approved")
                .get()
                .addOnSuccessListener(qs -> {
                    int currentApproved = (qs != null) ? qs.size() : 0;

                    if (currentApproved > tier.getMaxAthletes()) {
                        int needToRemove = currentApproved - tier.getMaxAthletes();
                        Toast.makeText(this,
                                "Downgrade blocked: remove " + needToRemove
                                        + " more athlete(s) to fit the "
                                        + pendingTierDowngrade + " plan ("
                                        + tier.getMaxAthletes() + " max).",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    Map<String, Object> subscriptionData = new HashMap<>();
                    subscriptionData.put("tier", tier.getTier());
                    subscriptionData.put("maxAthletes", tier.getMaxAthletes());
                    subscriptionData.put("price", tier.getPrice());

                    db.collection("users").document(uid)
                            .update("subscription", subscriptionData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this,
                                        "Tier downgraded to " + pendingTierDowngrade + "!",
                                        Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to downgrade tier",
                                            Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to verify team size",
                                Toast.LENGTH_SHORT).show());
    }
}