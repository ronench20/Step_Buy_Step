package com.example.stepbuystep;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;
import android.widget.Spinner;
import android.widget.ArrayAdapter;


public class EnterInfoActivity extends ComponentActivity {

    private EditText etAge, etGender, etCity, etCoachId;
    private int etShoeCount;
    private Button btnSave;
    private Spinner spGender;



    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.enter_info);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        spGender = findViewById(R.id.spGender);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.gender_options,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spGender.setAdapter(adapter);


        etAge = findViewById(R.id.etAge);
        etCity = findViewById(R.id.etCity);
        etCoachId = findViewById(R.id.etCoachId);
        btnSave = findViewById(R.id.btnSaveProfile);
        //etShoeCount = "0";

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void saveProfile() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        String ageStr = etAge.getText().toString().trim();
        String gender = spGender.getSelectedItem().toString();
        if (gender.equals("Select gender")) {
            Toast.makeText(this, "Please select gender", Toast.LENGTH_SHORT).show();
            return;
        }
        String city = etCity.getText().toString().trim();
        String coachId = etCoachId.getText().toString().trim();

        if (TextUtils.isEmpty(ageStr)) { etAge.setError("Age required"); return; }
        if (TextUtils.isEmpty(gender)) { etGender.setError("Gender required"); return; }
        if (TextUtils.isEmpty(city)) { etCity.setError("City required"); return; }

        int age;
        try {
            age = Integer.parseInt(ageStr);
        } catch (NumberFormatException e) {
            etAge.setError("Invalid age");
            return;
        }

        Map<String, Object> data = new HashMap<>();
        data.put("email", user.getEmail());
        data.put("age", age);
        data.put("gender", gender);
        data.put("city", city);
        String uid = user.getUid();
        if (TextUtils.isEmpty(coachId)) {
            etCoachId.setError("Coach ID required");
            return;
        } else {
            validateCoachAndSave(uid, coachId, data);
        }

    }

    private void validateCoachAndSave(String uid, String coachIdText, Map<String, Object> data) {
        long coachIdNumber;
        try {
            coachIdNumber = Long.parseLong(coachIdText.trim());
        } catch (NumberFormatException e) {
            etCoachId.setError("Coach ID must be a number");
            return;
        }

        db.collection("coaches")
                .whereEqualTo("coachID", coachIdNumber)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String coachDocId = querySnapshot.getDocuments().get(0).getId();

                        data.put("coachID", coachIdNumber);
                        data.put("coachDocId", coachDocId);

                        saveUser(uid, data);
                    } else {
                        etCoachId.setError("Invalid coach ID");
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error checking coach ID: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }


    private void saveUser(String uid, Map<String, Object> data) {
        data.put("role", "trainee");
        db.collection("users")
                .document(uid)
                .set(data)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Profile saved!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, TraineeHomeActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Save failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }

}
