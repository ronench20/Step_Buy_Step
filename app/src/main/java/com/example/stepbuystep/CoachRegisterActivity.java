package com.example.stepbuystep;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CoachRegisterActivity extends ComponentActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etEmail, etPassword;
    private Button btnRegister;

    private final Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.coach_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etEmail = findViewById(R.id.etCoachEmail);
        etPassword = findViewById(R.id.etCoachPassword);
        btnRegister = findViewById(R.id.btnCoachRegister);

        btnRegister.setOnClickListener(v -> registerCoach());
    }

    private void registerCoach() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (TextUtils.isEmpty(email)) { etEmail.setError("Email required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Password required"); return; }
        if (password.length() < 6) { etPassword.setError("Min 6 characters"); return; }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(res -> createCoachProfile(email))
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Toast.makeText(this, "Email already exists. Please login.", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Register failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createCoachProfile(String email) {
        if (auth.getCurrentUser() == null) return;

        String uid = auth.getCurrentUser().getUid();
        int candidate = 1000 + random.nextInt(9000); // 1000-9999
        tryReserveCoachIdAndSave(uid, email, candidate, 0);
    }

    private void tryReserveCoachIdAndSave(String uid, String email, int coachId, int attempt) {
        if (attempt >= 10) {
            Toast.makeText(this, "Could not generate unique coach ID. Try again.", Toast.LENGTH_LONG).show();
            return;
        }

        String coachIdStr = String.valueOf(coachId);
        db.runTransaction((Transaction.Function<Void>) transaction -> {

            var idDocRef = db.collection("coach_ids").document(coachIdStr);
            var idSnap = transaction.get(idDocRef);

            if (idSnap.exists()) {
                throw new IllegalStateException("COACH_ID_TAKEN");
            }

            Map<String, Object> idData = new HashMap<>();
            idData.put("uid", uid);
            idData.put("createdAt", System.currentTimeMillis());
            transaction.set(idDocRef, idData);

            Map<String, Object> coachDoc = new HashMap<>();
            coachDoc.put("uid", uid);
            coachDoc.put("email", email);
            coachDoc.put("coachID", coachId);
            transaction.set(db.collection("coaches").document(uid), coachDoc);

            Map<String, Object> userDoc = new HashMap<>();
            userDoc.put("email", email);
            userDoc.put("role", "coach");
            userDoc.put("coachID", coachId);
            transaction.set(db.collection("users").document(uid), userDoc);

            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Coach created! Coach ID: " + coachIdStr, Toast.LENGTH_LONG).show();

            startActivity(new Intent(this, CoachHomeActivity.class));
            finish();

        }).addOnFailureListener(e -> {
            if (e.getMessage() != null && e.getMessage().contains("COACH_ID_TAKEN")) {
                int newCandidate = 1000 + random.nextInt(9000);
                tryReserveCoachIdAndSave(uid, email, newCandidate, attempt + 1);
            } else {
                Toast.makeText(this, "Failed creating coach profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
