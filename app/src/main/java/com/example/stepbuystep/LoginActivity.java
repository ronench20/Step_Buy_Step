package com.example.stepbuystep;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends ComponentActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etEmail, etPassword;
    private Button btnEmailLogin, btnGoogleLogin;

    private GoogleSignInClient googleClient;

    private final ActivityResultLauncher<Intent> googleLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getData() == null) return;

                try {
                    GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(result.getData())
                            .getResult(ApiException.class);

                    if (account == null) {
                        Toast.makeText(this, "Google sign-in cancelled", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String idToken = account.getIdToken();
                    if (idToken == null) {
                        Toast.makeText(this, "Missing ID token. Check Firebase/Google config.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
                    auth.signInWithCredential(credential)
                            .addOnSuccessListener(res -> routeByRole())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Google login failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );

                } catch (ApiException e) {
                    Toast.makeText(this, "Google sign-in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnEmailLogin = findViewById(R.id.btnEmailLogin);
        btnGoogleLogin = findViewById(R.id.btnGoogleLogin);

        // Google Sign-In setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(getString(R.string.default_web_client_id))
                .build();

        googleClient = GoogleSignIn.getClient(this, gso);

        btnEmailLogin.setOnClickListener(v -> loginWithEmail());
        btnGoogleLogin.setOnClickListener(v -> loginWithGoogle());
    }

    private void loginWithEmail() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Email required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }

        auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> routeByRole())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Login failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

    private void loginWithGoogle() {
        googleClient.signOut().addOnCompleteListener(task -> {
            Intent intent = googleClient.getSignInIntent();
            googleLauncher.launch(intent);
        });
    }

    private void routeByRole() {
        if (auth.getCurrentUser() == null) return;

        String uid = auth. getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (! doc.exists()) {
                        startActivity(new Intent(this, RegisterActivity.class));
                        finish();
                        return;
                    }

                    String role = doc. getString("role"); // "coach" / "trainee"

                    if ("coach".equals(role)) {
                        startActivity(new Intent(this, CoachHomeActivity.class));
                        finish();
                    } else if ("trainee".equals(role)) {
                        // Check trainee status
                        String status = doc.getString("status");

                        if ("approved".equals(status)) {
                            // Approved trainee - go to home
                            startActivity(new Intent(this, TraineeHomeActivity.class));
                            finish();
                        } else if ("pending".equals(status)) {
                            // Pending trainee - go to pending screen
                            startActivity(new Intent(this, PendingApprovalActivity.class));
                            finish();
                        } else if ("rejected".equals(status)) {
                            // Rejected trainee - go to pending screen (will show rejected message)
                            startActivity(new Intent(this, PendingApprovalActivity.class));
                            finish();
                        } else if ("removed".equals(status)) {
                            // Removed trainee - go to re-enter coach ID screen
                            startActivity(new Intent(this, ReEnterCoachIdActivity.class));
                            finish();
                        } else {
                            // No status field (old users) - treat as approved
                            startActivity(new Intent(this, TraineeHomeActivity.class));
                            finish();
                        }
                    } else {
                        // Unknown role
                        startActivity(new Intent(this, TraineeHomeActivity.class));
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading profile:  " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}