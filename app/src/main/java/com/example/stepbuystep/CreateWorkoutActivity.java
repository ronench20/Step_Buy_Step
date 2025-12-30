package com.example.stepbuystep;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.ComponentActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateWorkoutActivity extends ComponentActivity {

    private EditText etType, etDate, etTime, etLocation;
    private ImageView btnBack;
    private LinearLayout btnLogout;

    // Bottom Nav
    private LinearLayout navMyAthletes, navMyHistory, navCreate, navSettings;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_workout);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        etType = findViewById(R.id.etWorkoutType);
        etDate = findViewById(R.id.etWorkoutDate);
        etTime = findViewById(R.id.etWorkoutTime);
        etLocation = findViewById(R.id.etWorkoutLocation);
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);

        navMyAthletes = findViewById(R.id.navMyAthletes);
        navMyHistory = findViewById(R.id.navMyHistory);
        navCreate = findViewById(R.id.navCreate);
        navSettings = findViewById(R.id.navSettings);

        // Make Date/Time EditTexts non-focusable so they trigger pickers
        etDate.setFocusable(false);
        etDate.setClickable(true);
        etTime.setFocusable(false);
        etTime.setClickable(true);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        etDate.setOnClickListener(v -> showDatePicker());
        etTime.setOnClickListener(v -> showTimePicker());

        // Added button in XML
        findViewById(R.id.btnPublishWorkout).setOnClickListener(v -> createWorkout());

        navMyAthletes.setOnClickListener(v ->
             startActivity(new Intent(this, GroupListActivity.class))
        );

        navMyHistory.setOnClickListener(v ->
             Toast.makeText(this, "History feature coming soon", Toast.LENGTH_SHORT).show()
        );

        navSettings.setOnClickListener(v ->
             Toast.makeText(this, "Settings feature coming soon", Toast.LENGTH_SHORT).show()
        );
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            etDate.setText(year + "-" + (month + 1) + "-" + dayOfMonth);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            etTime.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void createWorkout() {
        String type = etType.getText().toString();
        String date = etDate.getText().toString();
        String time = etTime.getText().toString();
        String location = etLocation.getText().toString();

        if (TextUtils.isEmpty(type) || TextUtils.isEmpty(date)) {
            Toast.makeText(this, "Please fill details", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getUid();
        if (uid == null) return;

        Map<String, Object> workout = new HashMap<>();
        workout.put("coachId", uid);
        workout.put("type", type);
        workout.put("date", date);
        workout.put("time", time);
        workout.put("location", location);
        workout.put("createdAt", System.currentTimeMillis());

        db.collection("workouts").add(workout)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Workout Created!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
