package com.example.stepbuystep.ActivityCoach.CoachSettingsScreen;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_team_members);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCoachId();
    }

    private void initViews() {
        btnBack = findViewById(R.id. btnBack);
        rvTeamMembers = findViewById(R. id.rvTeamMembers);
        emptyStateTeam = findViewById(R.id.emptyStateTeam);
        tvTeamCount = findViewById(R. id.tvTeamCount);
    }

    private void setupRecyclerView() {
        adapter = new TeamMembersAdapter();
        rvTeamMembers.setLayoutManager(new LinearLayoutManager(this));
        rvTeamMembers.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        adapter.setListener((userId, name) -> showRemoveConfirmation(userId, name));
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

                            members.add(new TeamMembersAdapter.TeamMember(
                                    userId, name, email, city
                            ));
                        }

                        adapter.setMembers(members);
                        tvTeamCount.setText(String. valueOf(members.size()));

                        // Show/hide empty state
                        if (members.isEmpty()) {
                            rvTeamMembers.setVisibility(View.GONE);
                            emptyStateTeam. setVisibility(View.VISIBLE);
                        } else {
                            rvTeamMembers.setVisibility(View. VISIBLE);
                            emptyStateTeam.setVisibility(View.GONE);
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
                    Toast.makeText(this, name + " has been removed from your team", Toast.LENGTH_SHORT).show();
                    // Adapter will auto-update via snapshot listener
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to remove team member", Toast.LENGTH_SHORT).show();
                });
    }
}