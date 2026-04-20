package com.example.stepbuystep.ActivityCoach.CoachSettingsScreen;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.stepbuystep.ActivityCoach.BaseCoachActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.model.CoachSubscriptionHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Map;

public class CoachSettingsActivity extends BaseCoachActivity {


    private FirebaseAuth auth;
    private FirebaseFirestore db;


    // Profile card
    private TextView tvName;
    private TextView tvEmail;
    private TextView tvCoachId;

    // Subscription card
    private TextView tvPlanName;
    private TextView badgeStatus;
    private TextView tvAthletesUsage;

    // Rows
    private TextView tvAthletesCount;
    private LinearLayout rowManageTeam, rowEditGroup, rowChangeSubscription;

    private long coachIdValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_settings);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        setupNavigationBar(NavItem.SETTINGS);
        loadCoachData();
    }

    private void initViews() {

        // Profile card (match XML ids)
        tvName = findViewById(R.id.tvFullName);
        tvEmail = findViewById(R.id.tvEmail);
        tvCoachId = findViewById(R.id.badgeCoachId);

        // Subscription card (match XML ids)
        tvPlanName = findViewById(R.id.tvTier);
        badgeStatus = findViewById(R.id.badgeActive);
        tvAthletesUsage = findViewById(R.id.tvAthletesUsage);

        // Rows (match XML ids)
        tvAthletesCount = findViewById(R.id.tvAthletesCount);
        rowManageTeam = findViewById(R.id.rowManageTeam);
        rowEditGroup = findViewById(R.id.rowEditGroup);
        rowChangeSubscription = findViewById(R.id.rowChangeSubscription);
    }

    private void setupListeners() {
        // Click coach id badge to copy
        if (tvCoachId != null) {
            tvCoachId.setOnClickListener(v -> {
                if (coachIdValue == 0) return;

                ClipboardManager clipboard =
                        (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Coach ID", String.valueOf(coachIdValue));
                clipboard.setPrimaryClip(clip);

                Toast.makeText(this, "Copied ID: " + coachIdValue, Toast.LENGTH_SHORT).show();
            });
        }

        if (rowManageTeam != null) {
            rowManageTeam.setOnClickListener(v ->
                    startActivity(new Intent(this, ManageTeamMembersActivity. class)));
        }

        if (rowEditGroup != null) {
            rowEditGroup.setOnClickListener(v ->
                    Toast.makeText(this, "Edit Group: Coming soon", Toast.LENGTH_SHORT).show());
        }

        if (rowChangeSubscription != null) {
            rowChangeSubscription.setOnClickListener(v ->
                    showSubscriptionDialog());
        }
    }

    private void showSubscriptionDialog() {
        SubscriptionSelectionDialog dialog = new SubscriptionSelectionDialog();

        // Get current tier from tvPlanName
        String currentTier = "basic";
        if (tvPlanName != null && tvPlanName.getText() != null) {
            currentTier = tvPlanName.getText().toString().toLowerCase();
        }

        // Get current athlete count
        String athletesText = tvAthletesUsage != null ? tvAthletesUsage.getText().toString() : "0 / 20";
        int currentCount = 0;
        try {
            currentCount = Integer.parseInt(athletesText.split(" / ")[0].trim());
        } catch (Exception e) {
            currentCount = 0;
        }

        dialog.setCurrentTier(currentTier, currentCount);

        dialog.setListener(new SubscriptionSelectionDialog.OnTierSelectedListener() {
            @Override
            public void onTierSelected(String newTier) {
                // Reload data to update UI
                loadCoachData();
            }

            @Override
            public void onError(String error) {
                if (error.startsWith("NEED_REMOVE_ATHLETES")) {
                    // Parse the message
                    String[] parts = error.split("\\|");
                    String targetTier = parts.length > 1 ? parts[1] : "basic";
                    // Store tier to switch to after removal
                    Toast.makeText(CoachSettingsActivity.this,
                            "Go to Manage Team Members to remove athletes", Toast.LENGTH_LONG).show();
                }
            }
        });

        dialog.show(getSupportFragmentManager(), "SubscriptionDialog");
    }

    private void loadCoachData() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String email = doc.getString("email");
                    String name = doc.getString("name");
                    if (name == null && email != null && email.contains("@")) {
                        name = email.substring(0, email.indexOf('@'));
                    }

                    Long cid = doc.getLong("coachID");

                    if (tvName != null) tvName.setText(name != null ? name : "Coach");
                    if (tvEmail != null) tvEmail.setText(email != null ? email : "");

                    if (cid != null) {
                        coachIdValue = cid;
                        if (tvCoachId != null) tvCoachId.setText("Coach ID: " + cid);
                        fetchAthletesCount(cid);

                        Map<String, Object> subscription = (Map<String, Object>) doc.get("subscription");
                        String currentTier = "basic";  // Default

                        if (subscription != null) {
                            Object tierObj = subscription.get("tier");
                            if (tierObj != null) {
                                currentTier = tierObj.toString();
                            }
                        }

                        if (tvPlanName != null) {
                            tvPlanName.setText(currentTier);
                        }

                    } else {
                        coachIdValue = 0;
                        if (tvCoachId != null) tvCoachId.setText("Coach ID: -");
                        if (tvAthletesUsage != null) tvAthletesUsage.setText("0 / 20 athletes");
                        if (tvAthletesCount != null) tvAthletesCount.setText("0 athletes");
                        if (tvPlanName != null) tvPlanName.setText("basic");
                    }

                    // Subscription UI
                    if (badgeStatus != null) badgeStatus.setText("Active");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load coach data", Toast.LENGTH_SHORT).show());
    }

    private void fetchAthletesCount(long coachId) {
        String uid = auth.getCurrentUser().getUid();

        CoachSubscriptionHelper.loadCoachSubscriptionAndCount(
                uid, coachId, db,
                new CoachSubscriptionHelper.OnSubscriptionLoadListener() {
                    @Override
                    public void onSubscriptionLoaded(int maxAthletes, int currentAthletes) {
                        // Update UI with subscription info
                        if (tvAthletesUsage != null) {
                            tvAthletesUsage.setText(currentAthletes + " / " + maxAthletes + " athletes");
                        }
                        if (tvAthletesCount != null) {
                            tvAthletesCount.setText(currentAthletes + " athletes");
                        }
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(CoachSettingsActivity.this,
                                "Failed to load athletes count", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
