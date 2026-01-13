package com.example.stepbuystep;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class CoachSettingsActivity extends BaseCoachActivity {

    private Button btnLogout;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Header
    private LinearLayout btnBack;

    // Profile card
    private TextView tvName;      // XML: tvFullName
    private TextView tvEmail;     // XML: tvEmail
    private TextView tvCoachId;   // XML: badgeCoachId (click to copy)

    // Subscription card
    private TextView tvPlanName;      // XML: tvTier
    private TextView badgeStatus;     // XML: badgeActive
    private TextView tvAthletesUsage; // XML: tvAthletesUsage

    // Rows
    private TextView tvAthletesCount; // XML: tvAthletesCount
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
        //btnLogout = findViewById(R.id.btnLogout);

        // Header
        btnBack = findViewById(R.id.btnBack);

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
        if (btnBack != null) btnBack.setOnClickListener(v -> finish());

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
                    Toast.makeText(this, "Change Subscription: Coming soon", Toast.LENGTH_SHORT).show());
        }
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
                    } else {
                        coachIdValue = 0;
                        if (tvCoachId != null) tvCoachId.setText("Coach ID: -");
                        if (tvAthletesUsage != null) tvAthletesUsage.setText("0 / 20 athletes");
                        if (tvAthletesCount != null) tvAthletesCount.setText("0 athletes");
                    }

                    // Subscription UI (placeholder until you connect real plan data)
                    if (tvPlanName != null) tvPlanName.setText("basic");
                    if (badgeStatus != null) badgeStatus.setText("Active");
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load coach data", Toast.LENGTH_SHORT).show());
    }

    private void fetchAthletesCount(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Toast.makeText(this, "Failed to load athletes count", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        int count = querySnapshot.size();

                        if (tvAthletesUsage != null) {
                            tvAthletesUsage. setText(count + " / 20 athletes");
                        }
                        if (tvAthletesCount != null) {
                            tvAthletesCount.setText(count + " athletes");
                        }
                    }
                });
    }
}
