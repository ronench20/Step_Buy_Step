package com.example.stepbuystep;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.adapter.TraineeSelectionAdapter;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BroadcastMessageDialogActivity extends DialogFragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText etMessage;
    private CheckBox cbSendToAll;
    private TextView tvSelectTrainees;
    private RecyclerView rvTraineesList;
    private LinearLayout btnSend;

    private TraineeSelectionAdapter traineeAdapter;
    private Long coachIdValue;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore. getInstance();

        // Inflate the dialog layout
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_broadcast_message, null);

        // Initialize views
        initViews(dialogView);

        // Setup RecyclerView
        setupRecyclerView();

        // Load trainees
        loadTrainees();

        // Setup listeners
        setupListeners();

        // Create and return the dialog
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();

        return dialog;
    }

    private void initViews(View view) {
        etMessage = view.findViewById(R.id.etMessage);
        cbSendToAll = view.findViewById(R. id.cbSendToAll);
        tvSelectTrainees = view.findViewById(R.id.tvSelectTrainees);
        rvTraineesList = view.findViewById(R.id.rvTraineesList);
        btnSend = view.findViewById(R. id.btnSend);
    }

    private void setupRecyclerView() {
        traineeAdapter = new TraineeSelectionAdapter();
        rvTraineesList.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTraineesList. setAdapter(traineeAdapter);
    }

    private void setupListeners() {
        // Handle "Send to All" checkbox
        cbSendToAll.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                rvTraineesList.setVisibility(View. GONE);
                tvSelectTrainees.setVisibility(View. GONE);
                traineeAdapter. deselectAll();
            } else {
                rvTraineesList.setVisibility(View. VISIBLE);
                tvSelectTrainees.setVisibility(View. VISIBLE);
            }
        });

        // Handle Send button
        btnSend.setOnClickListener(v -> sendMessages());
    }

    private void loadTrainees() {
        String uid = auth.getUid();
        if (uid == null) return;

        // Get coach's ID first
        db.collection("users").document(uid).get()
                .addOnSuccessListener(coachDoc -> {
                    if (! coachDoc.exists()) return;

                    coachIdValue = coachDoc.getLong("coachID");
                    if (coachIdValue == null) return;

                    // Fetch trainees under this coach
                    db. collection("users")
                            . whereEqualTo("role", "trainee")
                            .whereEqualTo("coachID", coachIdValue)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                List<TraineeSelectionAdapter.TraineeItem> trainees = new ArrayList<>();

                                for (QueryDocumentSnapshot doc : querySnapshot) {
                                    String traineeId = doc.getId();
                                    String email = doc.getString("email");
                                    String name = (email != null) ? email.split("@")[0] : "Trainee";
                                    if (name.length() > 0) {
                                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                                    }
                                    String city = doc.getString("city");
                                    if (city == null) city = "Unknown";

                                    trainees.add(new TraineeSelectionAdapter.TraineeItem(traineeId, name, city));
                                }

                                traineeAdapter.setTrainees(trainees);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(getContext(), "Error loading trainees", Toast. LENGTH_SHORT).show()
                            );
                });
    }

    private void sendMessages() {
        String messageText = etMessage.getText().toString().trim();

        if (messageText.isEmpty()) {
            Toast.makeText(getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
            return;
        }

        boolean sendToAll = cbSendToAll.isChecked();
        List<String> selectedTraineeIds;

        if (sendToAll) {
            // Get all trainee IDs
            selectedTraineeIds = traineeAdapter.getAllTraineeIds();
        } else {
            // Get only selected trainee IDs
            selectedTraineeIds = traineeAdapter.getSelectedTraineeIds();
            if (selectedTraineeIds.isEmpty()) {
                Toast.makeText(getContext(), "Please select at least one trainee", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (selectedTraineeIds.isEmpty()) {
            Toast.makeText(getContext(), "No trainees found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Send messages
        sendBroadcastMessages(messageText, selectedTraineeIds);
    }

    private void sendBroadcastMessages(String messageText, List<String> traineeIds) {
        String uid = auth. getUid();
        if (uid == null) return;

        // Get coach name
        db. collection("users").document(uid).get()
                .addOnSuccessListener(coachDoc -> {
                    String coachEmail = coachDoc.getString("email");
                    String coachName = (coachEmail != null) ? coachEmail.split("@")[0] : "Coach";
                    if (coachName.length() > 0) {
                        coachName = coachName.substring(0, 1).toUpperCase() + coachName.substring(1);
                    }
                    String coachId = uid;

                    int totalCount = traineeIds.size();
                    int[] sentCount = {0};

                    // Send message to each trainee
                    for (String traineeId : traineeIds) {
                        Map<String, Object> message = new HashMap<>();
                        message.put("coachId", coachId);
                        message.put("coachName", coachName);
                        message.put("traineeId", traineeId);
                        message.put("messageText", messageText);
                        message.put("timestamp", Timestamp.now());
                        message.put("isRead", false);

                        db.collection("messages")
                                .add(message)
                                .addOnSuccessListener(documentReference -> {
                                    sentCount[0]++;
                                    if (sentCount[0] == totalCount) {
                                        Toast.makeText(getContext(),
                                                "Message sent to " + totalCount + " trainee(s)",
                                                Toast.LENGTH_SHORT).show();
                                        dismiss();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(),
                                            "Failed to send to some trainees",
                                            Toast. LENGTH_SHORT).show();
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Error getting coach info", Toast.LENGTH_SHORT).show();
                });
    }
}