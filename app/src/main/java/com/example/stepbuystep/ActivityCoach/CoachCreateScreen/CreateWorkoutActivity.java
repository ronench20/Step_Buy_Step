package com.example.stepbuystep.ActivityCoach.CoachCreateScreen;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.example.stepbuystep.ActivityCoach.BaseCoachActivity;
import com.example.stepbuystep.ActivityCoach.CoachHistoryScreen.HistoryCoachActivity;
import com.example.stepbuystep.ActivityCoach.CoachHomeScreen.CoachHomeActivity;
import com.example.stepbuystep.ActivityCoach.CoachSettingsScreen.CoachSettingsActivity;
import com.example.stepbuystep.ActivityCommon.LoginActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.TraineeSelectionAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android. app.Activity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class CreateWorkoutActivity extends BaseCoachActivity {

    private EditText etType, etDate, etTime, etLocation;
    private Button btnPublishWorkout;
    private ImageView btnBack;
    private LinearLayout btnLogout;
    private LinearLayout navDashboardCoach, navMyHistory, navCreate, navSettings;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private RecyclerView rvTrainees;
    private TraineeSelectionAdapter traineeAdapter;
    private Button btnDeselectAll;
    private LinearLayout emptyStateTrainees;

    // Store selected location
    private double selectedLatitude = 0.0;
    private double selectedLongitude = 0.0;
    private String selectedAddress = "";

    // Activity Result Launcher for Google Maps
    private final ActivityResultLauncher<Intent> mapLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    selectedLatitude = data.getDoubleExtra("latitude", 0.0);
                    selectedLongitude = data.getDoubleExtra("longitude", 0.0);
                    selectedAddress = data.getStringExtra("address");

                    if (selectedAddress != null && !selectedAddress.isEmpty()) {
                        etLocation.setText(selectedAddress);
                    } else {
                        etLocation. setText(String.format("%.6f, %.6f", selectedLatitude, selectedLongitude));
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R. layout.activity_create_workout);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth. getInstance();

        initViews();
        setupNavigationBar(NavItem.CREATE);
        setupListeners();

        loadTrainees();

    }

    private void initViews() {
        etType = findViewById(R.id.etWorkoutType);
        etDate = findViewById(R.id.etWorkoutDate);
        etTime = findViewById(R.id.etWorkoutTime);
        etLocation = findViewById(R.id.etWorkoutLocation);
        btnPublishWorkout = findViewById(R.id.btnPublishWorkout);
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id. btnLogout);

        navDashboardCoach = findViewById(R.id.navDashboardCoach);
        navMyHistory = findViewById(R.id.navMyHistory);
        navCreate = findViewById(R.id.navCreate);
        navSettings = findViewById(R.id.navSettings);

        // NEW:  Trainee selection views
        rvTrainees = findViewById(R.id.rvTrainees);
        btnDeselectAll = findViewById(R. id.btnDeselectAll);
        emptyStateTrainees = findViewById(R.id.emptyStateTrainees);

        traineeAdapter = new TraineeSelectionAdapter();
        rvTrainees.setLayoutManager(new LinearLayoutManager(this));
        rvTrainees.setAdapter(traineeAdapter);

        etDate.setFocusable(false);
        etDate.setClickable(true);
        etTime.setFocusable(false);
        etTime.setClickable(true);
        etLocation.setFocusable(false);
        etLocation.setClickable(true);
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
        etLocation.setOnClickListener(v -> openMapPicker());

        btnPublishWorkout.setOnClickListener(v -> createWorkout());

        btnDeselectAll.setOnClickListener(v -> traineeAdapter.deselectAll());


        navDashboardCoach.setOnClickListener(v ->
                startActivity(new Intent(this, CoachHomeActivity.class))
        );

        navMyHistory.setOnClickListener(v ->
                startActivity(new Intent(this, HistoryCoachActivity.class))
        );

        navSettings.setOnClickListener(v ->
                startActivity(new Intent(this, CoachSettingsActivity.class))
        );
    }

    private void openMapPicker() {
        // Launch the MapPickerActivity to select location
        Intent intent = new Intent(this, MapPickerActivity.class);
        mapLauncher.launch(intent);
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            etDate.setText(String.format("%02d/%02d/%d", dayOfMonth, month + 1, year));
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            etTime.setText(String.format("%02d:%02d", hourOfDay, minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void createWorkout() {
        String type = etType.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (TextUtils.isEmpty(type)) {
            Toast.makeText(this, "Please enter workout type", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils. isEmpty(date)) {
            Toast.makeText(this, "Please select date", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(time)) {
            Toast.makeText(this, "Please select time", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(location)) {
            Toast.makeText(this, "Please select location", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> selectedTraineeIds = traineeAdapter.getSelectedTraineeIds();
        if (selectedTraineeIds.isEmpty()) {
            Toast.makeText(this, "Please select at least one trainee", Toast.LENGTH_SHORT).show();
            return;
        }

        saveWorkoutToDatabase(type, date, time, location, selectedTraineeIds);
    }

    private void saveWorkoutToDatabase(String type, String date, String time,
                                       String location, List<String> traineeIds) {
        String coachId = auth.getUid();

        Map<String, Object> workout = new HashMap<>();
        workout.put("coachId", coachId);
        workout.put("type", type);
        workout.put("date", date);
        workout.put("time", time);
        workout.put("location", location);
        workout.put("latitude", selectedLatitude);
        workout.put("longitude", selectedLongitude);
        workout.put("traineeIds", traineeIds);
        workout.put("createdAt", System.currentTimeMillis());

        db.collection("workouts")
                .add(workout)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Workout scheduled successfully!", Toast.LENGTH_SHORT).show();

                    // Send automatic notifications to selected trainees
                    sendWorkoutNotifications(type, date, time, location, traineeIds);

                    clearForm();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error:  " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void sendWorkoutNotifications(String type, String date, String time,
                                          String location, List<String> traineeIds) {
        String uid = auth.getUid();
        if (uid == null) return;

        // Get coach name first
        db.collection("users").document(uid).get()
                .addOnSuccessListener(coachDoc -> {
                    String coachEmail = coachDoc.getString("email");
                    String coachName = (coachEmail != null) ? coachEmail.split("@")[0] : "Coach";
                    if (coachName.length() > 0) {
                        coachName = coachName.substring(0, 1).toUpperCase() + coachName.substring(1);
                    }

                    // Create notification message text
                    String messageText = String.format(
                            "New workout scheduled!\n\nType: %s\nDate: %s\nTime:  %s\nLocation: %s",
                            type, date, time, location
                    );

                    String finalCoachName = coachName;
                    // Send message to each selected trainee
                    for (String traineeId : traineeIds) {
                        Map<String, Object> message = new HashMap<>();
                        message.put("coachId", uid);
                        message. put("coachName", finalCoachName);
                        message.put("traineeId", traineeId);
                        message.put("messageText", messageText);
                        message.put("timestamp", com.google.firebase.Timestamp. now());
                        message.put("isRead", false);

                        db.collection("messages")
                                .add(message)
                                .addOnFailureListener(e -> {
                                    // Silently fail - not critical
                                });
                    }
                })
                .addOnFailureListener(e -> {});
    }

    private void clearForm() {
        etType.setText("");
        etDate.setText("");
        etTime.setText("");
        etLocation.setText("");
        selectedLatitude = 0.0;
        selectedLongitude = 0.0;
        selectedAddress = "";

        // Deselect all trainees
        traineeAdapter.deselectAll();

        // Optional: Show a message
        Toast.makeText(this, "Ready to create another workout", Toast.LENGTH_SHORT).show();
    }

    private void loadTrainees() {
        String uid = auth.getUid();
        if (uid == null) return;

        // Get coach's coachID
        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc. exists()) {
                        Long coachId = doc.getLong("coachID");
                        if (coachId != null) {
                            fetchTraineesForSelection(coachId);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
                );
    }

    private void fetchTraineesForSelection(Long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<TraineeSelectionAdapter.TraineeItem> trainees = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String email = doc.getString("email");
                        String name = (email != null) ? email.split("@")[0] : "Trainee";
                        // Capitalize first letter
                        if (name.length() > 0) {
                            name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        }
                        String city = doc.getString("city");
                        if (city == null) city = "Unknown";

                        trainees.add(new TraineeSelectionAdapter. TraineeItem(
                                doc.getId(), name, city
                        ));
                    }

                    traineeAdapter.setTrainees(trainees);

                    // Show/hide empty state
                    if (trainees.isEmpty()) {
                        rvTrainees.setVisibility(View.GONE);
                        emptyStateTrainees.setVisibility(View.VISIBLE);
                    } else {
                        rvTrainees.setVisibility(View. VISIBLE);
                        emptyStateTrainees.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error loading trainees:  " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }
}
