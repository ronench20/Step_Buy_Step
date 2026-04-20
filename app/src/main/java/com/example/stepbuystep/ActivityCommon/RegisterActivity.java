package com.example.stepbuystep.ActivityCommon;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.annotation.NonNull;

import com.example.stepbuystep.ActivityTrainee.TraineeReg.EnterInfoActivity;
import com.example.stepbuystep.R;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class RegisterActivity extends ComponentActivity {

    private FirebaseAuth auth;

    private EditText etEmail, etPassword;
    private Button btnEmailRegister, btnGoogleRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        auth = FirebaseAuth.getInstance();

        etEmail           = findViewById(R.id.etEmail);
        etPassword        = findViewById(R.id.etPassword);
        btnEmailRegister  = findViewById(R.id.btnEmailRegister);
        btnGoogleRegister = findViewById(R.id.btnGoogleRegister);

        btnEmailRegister.setOnClickListener(v -> registerWithEmail());
        btnGoogleRegister.setOnClickListener(v -> registerWithGoogle());
    }

    // ----------------------- Email/Password registration -----------------------

    private void registerWithEmail() {
        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email))    { etEmail.setError("Email required");    return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }
        if (password.length() < 6)       { etPassword.setError("Min 6 characters"); return; }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> goToEnterInfo())
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "User already exists", Toast.LENGTH_LONG).show();
                    } else if (e instanceof FirebaseNetworkException) {
                        Toast.makeText(this, "Network error. Try again.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Register failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // -------------------------- Google registration ---------------------------

    private void registerWithGoogle() {
        btnGoogleRegister.setEnabled(false);
        GoogleAuthHelper.signIn(this, new GoogleAuthHelper.Callback() {
            @Override public void onSuccess(@NonNull AuthResult result) {
                btnGoogleRegister.setEnabled(true);
                // Either brand-new user or returning Google user; in both cases the
                // trainee onboarding still needs profile info (age/gender/city/coachID).
                goToEnterInfo();
            }
            @Override public void onError(@NonNull Throwable error) {
                btnGoogleRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this,
                        "Google sign-up failed: " + error.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
            @Override public void onCancelled() {
                btnGoogleRegister.setEnabled(true);
            }
        });
    }

    private void goToEnterInfo() {
        startActivity(new Intent(this, EnterInfoActivity.class));
        finish();
    }
}
