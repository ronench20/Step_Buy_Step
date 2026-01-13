package com.example.stepbuystep;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class PendingApprovalActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private LinearLayout btnRefresh;
    private LinearLayout btnLogout;
    private ImageView ivStatusIcon;
    private TextView tvStatusTitle;
    private TextView tvStatusMessage;
    private TextView tvStatusSubMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_approval);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        checkApprovalStatus();
    }

    private void initViews() {
        btnRefresh = findViewById(R.id.btnRefresh);
        btnLogout = findViewById(R.id.btnLogout);
        ivStatusIcon = findViewById(R.id.ivStatusIcon);
        tvStatusTitle = findViewById(R.id.tvStatusTitle);
        tvStatusMessage = findViewById(R.id.tvStatusMessage);
        tvStatusSubMessage = findViewById(R.id.tvStatusSubMessage);
    }

    private void setupListeners() {
        btnRefresh.setOnClickListener(v -> checkApprovalStatus());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void checkApprovalStatus() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String status = doc.getString("status");

                        if ("approved".equals(status)) {
                            // Approved - go to trainee home
                            Toast.makeText(this, "Your account has been approved!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, TraineeHomeActivity.class));
                            finish();
                        } else if ("rejected".equals(status)) {
                            // Rejected - update UI
                            updateUIForRejected();
                        } else {
                            // Still pending - update UI
                            updateUIForPending();
                            Toast.makeText(this, "Still waiting for approval...", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error checking status", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUIForPending() {
        ivStatusIcon.setImageResource(R.drawable.ic_pending);
        ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.brand_blue));

        tvStatusTitle. setText("Pending Approval");
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));

        tvStatusMessage.setText("Your registration has been submitted successfully!");
        tvStatusSubMessage.setText("Please wait for your coach to approve your account.");

        btnRefresh.setVisibility(View.VISIBLE);
    }

    private void updateUIForRejected() {
        ivStatusIcon.setImageResource(R.drawable.ic_close);
        ivStatusIcon.setColorFilter(ContextCompat. getColor(this, R.color.brand_red));

        tvStatusTitle.setText("Request Rejected");
        tvStatusTitle.setTextColor(ContextCompat.getColor(this, R.color.brand_red));

        tvStatusMessage.setText("Your registration request was rejected by the coach.");
        tvStatusSubMessage.setText("Please contact your coach for more information.");

        btnRefresh.setVisibility(View. GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkApprovalStatus();
    }
}