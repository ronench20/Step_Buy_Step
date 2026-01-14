package com.example.stepbuystep.ActivityTrainee.TraineeHomeScreen;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.MessagesAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class MessagesActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private LinearLayout btnBack;
    private RecyclerView rvMessages;
    private View emptyStateMessages;

    private MessagesAdapter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        loadMessages();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        rvMessages = findViewById(R.id. rvMessages);
        emptyStateMessages = findViewById(R.id.emptyStateMessages);
    }

    private void setupRecyclerView() {
        messagesAdapter = new MessagesAdapter();
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(messagesAdapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        messagesAdapter.setListener(messageId -> showDeleteConfirmation(messageId));
    }

    private void loadMessages() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("messages")
                .whereEqualTo("traineeId", uid)
                .orderBy("timestamp", Query.Direction. DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        //Toast.makeText(this, "Error loading messages", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (querySnapshot != null) {
                        List<MessagesAdapter.MessageItem> messages = new ArrayList<>();

                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            String messageId = doc.getId();
                            String coachName = doc.getString("coachName");
                            String messageText = doc.getString("messageText");
                            com.google.firebase.Timestamp timestamp = doc.getTimestamp("timestamp");
                            Boolean isRead = doc.getBoolean("isRead");

                            if (coachName == null) coachName = "Coach";
                            if (messageText == null) messageText = "";
                            if (isRead == null) isRead = false;

                            messages.add(new MessagesAdapter.MessageItem(
                                    messageId, coachName, messageText, timestamp, isRead
                            ));

                            // Mark message as read if it's not already
                            if (!isRead) {
                                markMessageAsRead(messageId);
                            }
                        }

                        messagesAdapter.setMessages(messages);

                        // Show/hide empty state
                        if (messages.isEmpty()) {
                            rvMessages.setVisibility(View.GONE);
                            emptyStateMessages.setVisibility(View.VISIBLE);
                        } else {
                            rvMessages.setVisibility(View. VISIBLE);
                            emptyStateMessages.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private void markMessageAsRead(String messageId) {
        db.collection("messages")
                .document(messageId)
                .update("isRead", true)
                .addOnFailureListener(e -> {
                    // Silently fail - not critical
                });
    }

    private void showDeleteConfirmation(String messageId) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Message")
                .setMessage("Are you sure you want to delete this message?")
                .setPositiveButton("Delete", (dialog, which) -> deleteMessage(messageId))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteMessage(String messageId) {
        // Immediately remove from UI for instant feedback
        messagesAdapter.removeMessage(messageId);

        // Check if empty after removal
        if (messagesAdapter.getItemCount() == 0) {
            rvMessages.setVisibility(View.GONE);
            emptyStateMessages.setVisibility(View. VISIBLE);
        }

        // Delete from Firestore
        db.collection("messages")
                .document(messageId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Message deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete message", Toast.LENGTH_SHORT).show();
                    // If deletion failed, reload to restore the UI
                    loadMessages();
                });
    }
}