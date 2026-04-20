package com.example.stepbuystep.ActivityCommon;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

import com.example.stepbuystep.ActivityCoach.CoachHomeScreen.CoachHomeActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeReg.PendingApprovalActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeHomeScreen.TraineeHomeActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeReg.ReEnterCoachIdActivity;
import com.example.stepbuystep.R;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends ComponentActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etEmail, etPassword;
    private Button btnEmailLogin, btnGoogleLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        etEmail       = findViewById(R.id.etLoginEmail);
        etPassword    = findViewById(R.id.etLoginPassword);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        btnEmailLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> loginWithGoogle());
    }

    // --------------------------- Email/Password ---------------------------

    private void loginWithEmail() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email))    { etEmail.setError("Email required");    return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> routeByRole())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show());
    }

    // ------------------------------- Google -------------------------------

    private void loginWithGoogle() {
        btnGoogleLogin.setEnabled(false);
        GoogleAuthHelper.signIn(this, new GoogleAuthHelper.Callback() {
            @Override public void onSuccess(@NonNull AuthResult result) {
                btnGoogleLogin.setEnabled(true);
                routeByRole();
            }
            @Override public void onError(@NonNull Throwable error) {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "Google sign-in failed: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            @Override public void onCancelled() {
                btnGoogleLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this,
                        "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --------------------------- Role routing -----------------------------

    private void routeByRole() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        // First Google sign-in for this user — send to register
                        startActivity(new Intent(this, RegisterActivity.class));
                        finish();
                        return;
                    }

                    String role = doc.getString("role");
                    if ("coach".equals(role)) {
                        startActivity(new Intent(this, CoachHomeActivity.class));
                        finish();
                    } else if ("trainee".equals(role)) {
                        String status = doc.getString("status");
                        Intent next;
                        if ("approved".equals(status) || status == null) {
                            next = new Intent(this, TraineeHomeActivity.class);
                        } else if ("pending".equals(status) || "rejected".equals(status)) {
                            next = new Intent(this, PendingApprovalActivity.class);
                        } else if ("removed".equals(status)) {
                            next = new Intent(this, ReEnterCoachIdActivity.class);
                        } else {
                            next = new Intent(this, TraineeHomeActivity.class);
                        }
                        startActivity(next);
                        finish();
                    } else {
                        startActivity(new Intent(this, TraineeHomeActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error loading profile: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }
}
