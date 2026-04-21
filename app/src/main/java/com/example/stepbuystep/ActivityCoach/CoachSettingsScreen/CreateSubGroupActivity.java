package com.example.stepbuystep.ActivityCoach.CoachSettingsScreen;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityCommon.NotificationManager;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.SelectTraineesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CreateSubGroupActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // UI Components
    private EditText etSubGroupName;
    private RecyclerView rvSelectTrainees;
    private Button btnCreateSubGroup;
    private LinearLayout btnBack;

    // Adapters and data
    private SelectTraineesAdapter adapter;
    private List<TraineeForSelection> traineesForSelection;
    private long coachIdValue = 0;
    private String coachDocId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_sub_group);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadCoachIdAndTrainees();
    }

    private void initViews() {
        etSubGroupName = findViewById(R.id.etSubGroupName);
        rvSelectTrainees = findViewById(R.id.rvSelectTrainees);
        btnCreateSubGroup = findViewById(R.id.btnCreateSubGroup);
        btnBack = findViewById(R.id.btnBack);
    }

    private void setupRecyclerView() {
        traineesForSelection = new ArrayList<>();
        adapter = new SelectTraineesAdapter(traineesForSelection);
        rvSelectTrainees.setLayoutManager(new LinearLayoutManager(this));
        rvSelectTrainees.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnCreateSubGroup.setOnClickListener(v -> {
            String subGroupName = etSubGroupName.getText().toString().trim();

            // Validation
            if (subGroupName.isEmpty()) {
                Toast.makeText(this, "Please enter a sub-group name", Toast.LENGTH_SHORT).show();
                return;
            }

            List<String> selectedTraineeIds = adapter.getSelectedTraineeIds();
            if (selectedTraineeIds.isEmpty()) {
                Toast.makeText(this, "Please select at least one trainee", Toast.LENGTH_SHORT).show();
                return;
            }

            createSubGroup(subGroupName, selectedTraineeIds);
        });
    }

    private void loadCoachIdAndTrainees() {
        String coachUid = auth.getCurrentUser().getUid();
        if (coachUid == null) return;

        // First, get the coach's coachID
        db.collection("users").document(coachUid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long coachId = doc.getLong("coachID");
                        coachIdValue = (coachId != null) ? coachId : 0;
                        coachDocId = coachUid;

                        // Then, load all trainees under this coach
                        loadTrainees(coachIdValue);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading coach info", Toast.LENGTH_SHORT).show();
                });
    }
    private void loadTrainees(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .get()  // <-- REMOVED the .whereEqualTo("status", "approved") filter
                .addOnSuccessListener(querySnapshot -> {
                    traineesForSelection.clear();

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String traineeId = doc.getId();
                        String email = doc.getString("email");
                        String name = (email != null) ? email.split("@")[0] : "Trainee";
                        if (name.length() > 0) {
                            name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        }
                        String city = doc.getString("city");
                        if (city == null) city = "Unknown";

                        traineesForSelection.add(new TraineeForSelection(traineeId, name, email));
                    }

                    adapter.notifyDataSetChanged();

                    // Show/hide empty state (optional - add this if you want)
                    if (traineesForSelection.isEmpty()) {
                        Toast.makeText(CreateSubGroupActivity.this, "No trainees found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading trainees: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void createSubGroup(String subGroupName, List<String> selectedTraineeIds) {
        // Create a new sub-group document
        Map<String, Object> subGroupData = new HashMap<>();
        subGroupData.put("name", subGroupName);
        subGroupData.put("coachID", coachIdValue);
        subGroupData.put("coachDocId", coachDocId);
        subGroupData.put("trainees", selectedTraineeIds);
        subGroupData.put("createdAt", System.currentTimeMillis());

        // Add to Firestore
        db.collection("subgroups")
                .add(subGroupData)
                .addOnSuccessListener(documentReference -> {
                    String subGroupId = documentReference.getId();
                    addSubGroupToTrainees(selectedTraineeIds, subGroupId, subGroupName);  // Pass subGroupName
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error creating sub-group: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void addSubGroupToTrainees(List<String> traineeIds, String subGroupId, String subGroupName) {
        // For each trainee, add the subGroupId to their subgroups array
        for (String traineeId : traineeIds) {
            db.collection("users").document(traineeId)
                    .update("subgroups", com.google.firebase.firestore.FieldValue.arrayUnion(subGroupId))
                    .addOnSuccessListener(aVoid -> {
                        // Successfully added
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Error updating trainee: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }

        // Send notifications to all selected trainees
        String coachName = auth.getCurrentUser().getEmail();
        if (coachName != null && coachName.contains("@")) {
            coachName = coachName.split("@")[0]; // Get name before @
            if (coachName.length() > 0) {
                coachName = coachName.substring(0, 1).toUpperCase() + coachName.substring(1);
            }
        }

        NotificationManager notificationManager = new NotificationManager();
        notificationManager.notifyTraineesOnSubGroupCreation(subGroupName, traineeIds, coachName,
                (success, message) -> {
                    if (success) {
                        Toast.makeText(CreateSubGroupActivity.this, message, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CreateSubGroupActivity.this, "Error: " + message, Toast.LENGTH_SHORT).show();
                    }
                });

        // Show success and close
        Toast.makeText(this, "Sub-group created successfully!", Toast.LENGTH_SHORT).show();
        finish();
    }
    // Simple data class to hold trainee info for selection
    public static class TraineeForSelection {
        public String id;
        public String name;
        public String email;

        public TraineeForSelection(String id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
        }
    }
}