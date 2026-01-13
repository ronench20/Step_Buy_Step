package com.example.stepbuystep.ActivityTrainee.TraineeReg;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stepbuystep.ActivityCommon.LoginActivity;
import com.example.stepbuystep.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class ReEnterCoachIdActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etNewCoachId;
    private LinearLayout btnSubmitNewCoachId;
    private LinearLayout btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reenter_coach_id);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore. getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etNewCoachId = findViewById(R.id.etNewCoachId);
        btnSubmitNewCoachId = findViewById(R.id.btnSubmitNewCoachId);
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void setupListeners() {
        btnSubmitNewCoachId.setOnClickListener(v -> submitNewCoachId());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void submitNewCoachId() {
        String coachIdStr = etNewCoachId.getText().toString().trim();

        if (TextUtils.isEmpty(coachIdStr)) {
            Toast. makeText(this, "Please enter a Coach ID", Toast.LENGTH_SHORT).show();
            return;
        }

        long newCoachId;
        try {
            newCoachId = Long.parseLong(coachIdStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid Coach ID format", Toast.LENGTH_SHORT).show();
            return;
        }

        // Verify coach exists
        verifyAndSubmitCoachId(newCoachId);
    }

    private void verifyAndSubmitCoachId(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "coach")
                .whereEqualTo("coachID", coachId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (querySnapshot.isEmpty()) {
                        Toast.makeText(this, "Coach ID not found.  Please check and try again.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    // Coach exists, update trainee's coachID and status
                    updateTraineeCoachId(coachId);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error verifying coach: " + e. getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateTraineeCoachId(long newCoachId) {
        String uid = auth.getUid();
        if (uid == null) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("coachID", newCoachId);
        updates.put("status", "pending");

        db.collection("users")
                .document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Request submitted!  Waiting for new coach approval.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, PendingApprovalActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to submit request: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}