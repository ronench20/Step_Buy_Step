package com.example.stepbuystep;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class CoachHomeActivity extends ComponentActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvCoachId;
    private TextView tvActiveAthletesCount;
    private TextView tvPendingRequestsCount;
    private TextView tvUpcomingCount;
    private LinearLayout btnCopyId;
    private LinearLayout btnLogout;
    private Button btnBroadcastMessage;

    // Bottom Nav
    private LinearLayout navMyAthletes;
    private LinearLayout navMyHistory;
    private LinearLayout navCreate;
    private LinearLayout navSettings;

    private long coachIdValue = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.coach_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        fetchCoachData();
    }

    private void initViews() {
        tvCoachId = findViewById(R.id.tvCoachId);
        tvActiveAthletesCount = findViewById(R.id.tvActiveAthletesCount);
        tvPendingRequestsCount = findViewById(R.id.tvPendingRequestsCount);
        tvUpcomingCount = findViewById(R.id.tvUpcomingCount);
        btnCopyId = findViewById(R.id.btnCopyId);
        btnLogout = findViewById(R.id.btnLogout);
        btnBroadcastMessage = findViewById(R.id.btnBroadcastMessage);

        navMyAthletes = findViewById(R.id.navMyAthletes);
        navMyHistory = findViewById(R.id.navMyHistory);
        navCreate = findViewById(R.id.navCreate);
        navSettings = findViewById(R.id.navSettings);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnCopyId.setOnClickListener(v -> {
            if (coachIdValue != 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Coach ID", String.valueOf(coachIdValue));
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, "Copied ID: " + coachIdValue, Toast.LENGTH_SHORT).show();
            }
        });

        btnBroadcastMessage.setOnClickListener(v -> {
            // Future implementation: Dialog to send message
            Toast.makeText(this, "Broadcast message feature coming soon", Toast.LENGTH_SHORT).show();
        });

        navMyAthletes.setOnClickListener(v -> {
             // Already here/Refresh or GroupListActivity
             // For now, redirect to GroupListActivity which seems to be the "My Athletes" page detail
             startActivity(new Intent(this, GroupListActivity.class));
        });

        navCreate.setOnClickListener(v ->
            startActivity(new Intent(this, CreateWorkoutActivity.class))
        );

        navMyHistory.setOnClickListener(v ->
             startActivity(new Intent(this, HistoryCoachActivity.class))
        );

        navSettings.setOnClickListener(v ->
             Toast.makeText(this, "Settings not implemented yet", Toast.LENGTH_SHORT).show()
        );
    }

    private void fetchCoachData() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Long cid = documentSnapshot.getLong("coachID");
                        if (cid != null) {
                            coachIdValue = cid;
                            tvCoachId.setText("ID: " + coachIdValue);
                            fetchAthletesCount(coachIdValue);
                        } else {
                             tvCoachId.setText("ID: N/A");
                        }
                    }
                });
    }

    private void fetchAthletesCount(long coachId) {
        db.collection("users")
                .whereEqualTo("coachID", coachId) // Fixed case to match registration
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size() - 1;
                    tvActiveAthletesCount.setText(String.valueOf(count));
                });

        // Pending requests logic would go here if implemented
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (coachIdValue != 0) {
            fetchAthletesCount(coachIdValue);
        }
    }
}
