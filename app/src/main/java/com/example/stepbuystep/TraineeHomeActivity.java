package com.example.stepbuystep;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.adapter.WorkoutAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text. SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import java.util.ArrayList;
import java.util.List;

public class TraineeHomeActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvUserName, tvShoeCount, tvCurrentShoeName, tvCurrentShoeLevel, tvMultiplier, tvCoachScheduledBadge, tvNotificationBadge;
    private CardView btnCategoryRunning, btnCategoryWalking, btnCoachScheduled;
    private LinearLayout navDashboard, navHistory, navShoeStore, navLeaderboard;
    private LinearLayout btnLogout;
    private RecyclerView rvWorkouts;
    private WorkoutAdapter workoutAdapter;
    private LinearLayout emptyStateWorkouts;
    private FrameLayout btnNotifications;
    private boolean showingWorkouts = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trainee_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        loadUserData();
        fetchUnreadMessageCount();
    }

    private void initViews() {
        tvUserName = findViewById(R.id. tvUserName);
        tvShoeCount = findViewById(R.id.tvShoeCount);
        tvCurrentShoeName = findViewById(R.id.tvCurrentShoeName);
        tvCurrentShoeLevel = findViewById(R.id.tvCurrentShoeLevel);
        tvMultiplier = findViewById(R.id.tvMultiplier);

        btnCategoryRunning = findViewById(R.id. btnCategoryRunning);
        btnCategoryWalking = findViewById(R.id.btnCategoryWalking);

        navDashboard = findViewById(R.id.navDashboard);
        navHistory = findViewById(R.id.navHistory);
        navShoeStore = findViewById(R.id.navShoeStore);
        navLeaderboard = findViewById(R.id. navLeaderboard);

        btnLogout = findViewById(R.id.btnLogout);

        rvWorkouts = findViewById(R.id.rvWorkouts);
        emptyStateWorkouts = findViewById(R.id.emptyStateWorkouts);

        workoutAdapter = new WorkoutAdapter();
        rvWorkouts.setLayoutManager(new LinearLayoutManager(this));
        rvWorkouts.setAdapter(workoutAdapter);
        rvWorkouts.setVisibility(View.GONE);
        emptyStateWorkouts.setVisibility(View. GONE);
        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);

        btnCoachScheduled = findViewById(R. id.btnCoachScheduled);
        tvCoachScheduledBadge = findViewById(R.id. tvCoachScheduledBadge);

        btnNotifications = findViewById(R.id.btnNotifications);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
    }

    private void setupListeners() {
        // Logout
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // Start Activity (Tracking)
        btnCategoryRunning.setOnClickListener(v -> startTrackingActivity("run"));
        btnCategoryWalking.setOnClickListener(v -> startTrackingActivity("walk"));

        btnCoachScheduled.setOnClickListener(v -> showUpcomingWorkoutsDialog());
        // Bottom Navigation
        navShoeStore.setOnClickListener(v -> startActivity(new Intent(this, ShopActivity.class)));

        navLeaderboard.setOnClickListener(v -> {
            // Assuming GroupListActivity serves as the leaderboard/group view for trainees too
            // If not, we might need a separate TraineeLeaderboardActivity
            // For now, let's use GroupListActivity or a Toast if strictly restricted
            // Previous spec said "Trainees can see a list of all members under their coach."
            startActivity(new Intent(this, LeaderBoardActivity.class));
        });

        navHistory.setOnClickListener(v -> startActivity(new Intent(this, HistoryTraineeActivity.class)));

        navDashboard.setOnClickListener(v -> {
            // Already here
        });

        // Notifications
        btnNotifications.setOnClickListener(v -> openMessagesScreen());
    }

    private void showUpcomingWorkoutsDialog() {
        List<WorkoutAdapter.WorkoutItem> upcomingWorkouts = getUpcomingWorkouts();
        UpcomingWorkoutsDialog dialog = UpcomingWorkoutsDialog.newInstance(upcomingWorkouts);
        dialog.show(getSupportFragmentManager(), "UpcomingWorkoutsDialog");
    }

    private List<WorkoutAdapter.WorkoutItem> getUpcomingWorkouts() {
        List<WorkoutAdapter.WorkoutItem> allWorkouts = workoutAdapter.getWorkouts();
        List<WorkoutAdapter.WorkoutItem> upcoming = new ArrayList<>();

        if (allWorkouts != null) {
            for (WorkoutAdapter. WorkoutItem workout : allWorkouts) {
                if (isUpcomingWorkout(workout.date, workout.time)) {
                    upcoming.add(workout);
                }
            }
        }

        return upcoming;
    }

    private void fetchUnreadMessageCount() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("messages")
                .whereEqualTo("traineeId", uid)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;
                    if (querySnapshot != null) {
                        int unreadCount = querySnapshot.size();
                        updateNotificationBadge(unreadCount);
                    }
                });
    }

    private void checkApprovalStatus() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc. exists()) {
                        String status = doc.getString("status");

                        if ("pending".equals(status)) {
                            // Status changed to pending
                            Toast.makeText(this, "Your account status has changed", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, PendingApprovalActivity.class));
                            finish();
                        } else if ("rejected".equals(status)) {
                            // Status changed to rejected
                            Toast.makeText(this, "Your account status has changed", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, PendingApprovalActivity.class));
                            finish();
                        } else if ("removed".equals(status)) {
                            // Coach removed the trainee
                            Toast.makeText(this, "Your coach has removed you from their team", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(this, ReEnterCoachIdActivity.class));
                            finish();
                        }
                    }
                });
    }

    private void updateNotificationBadge(int count) {
        if (count > 0) {
            tvNotificationBadge. setText(String.valueOf(count));
            tvNotificationBadge.setVisibility(View. VISIBLE);
        } else {
            tvNotificationBadge. setVisibility(View.GONE);
        }
    }

    private void openMessagesScreen() {
        Intent intent = new Intent(this, MessagesActivity.class);
        startActivity(intent);
    }

    private void startTrackingActivity(String type) {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra("ACTIVITY_TYPE", type);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkApprovalStatus();
        loadUserData(); // Refresh data when returning from Shop/Tracking
        loadWorkouts();
        fetchUnreadMessageCount();
    }

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String email = doc.getString("email");
                        // Extract name from email for display if name field doesn't exist
                        String name = email != null ? email.split("@")[0] : "Trainee";
                        if (name.length() > 0) {
                            name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        }

                        tvUserName.setText(name);

                        Long coins = doc.getLong("coin_balance");
                        tvShoeCount.setText(coins != null ? String.valueOf(coins) : "0");

                        fetchBestShoe(uid);
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                );
        fetchUnreadMessageCount();
    }

    private void fetchBestShoe(String uid) {
        // Find the best item in inventory to display as "Current Shoe"
        db.collection("users").document(uid).collection("inventory")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    String bestName = "Basic Runner"; // Default
                    long bestTier = 1;
                    double bestMult = 1.0;
                    boolean found = false;

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        Double m = doc.getDouble("multiplier");
                        Long t = doc.getLong("tier");
                        String n = doc.getString("name");

                        if (m != null && m >= bestMult) {
                            bestMult = m;
                            if (n != null) bestName = n;
                            if (t != null) bestTier = t;
                            found = true;
                        }
                    }

                    if (found) {
                        tvCurrentShoeName.setText(bestName);
                        tvCurrentShoeLevel.setText("Level " + bestTier);
                        tvMultiplier.setText(bestMult + "x");
                    }
                });
    }

    private void loadWorkouts() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("workouts")
                .whereArrayContains("traineeIds", uid)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<WorkoutAdapter.WorkoutItem> allWorkouts = new ArrayList<>();
                    List<WorkoutAdapter.WorkoutItem> upcomingWorkouts = new ArrayList<>();

                    for (com.google.firebase.firestore.QueryDocumentSnapshot doc : querySnapshot) {
                        String type = doc.getString("type");
                        String date = doc.getString("date");
                        String time = doc. getString("time");
                        String location = doc.getString("location");

                        WorkoutAdapter.WorkoutItem workout = new WorkoutAdapter.WorkoutItem(type, date, time, location);
                        allWorkouts.add(workout);

                        // Filter upcoming workouts
                        if (isUpcomingWorkout(date, time)) {
                            upcomingWorkouts.add(workout);
                        }
                    }

                    // Set all workouts to adapter (we'll filter when showing dialog)
                    workoutAdapter. setWorkouts(allWorkouts);

                    // Update badge with upcoming count only
                    updateCoachScheduledBadge(upcomingWorkouts.size());

                    // Keep workouts hidden (they're shown in dialog only)
                    rvWorkouts. setVisibility(View.GONE);
                    emptyStateWorkouts.setVisibility(View. GONE);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Error loading workouts:  " + e.getMessage(), Toast.LENGTH_LONG).show();
                    e.printStackTrace();
                });
    }

    private void updateCoachScheduledBadge(int count) {
        if (count > 0) {
            tvCoachScheduledBadge.setText(String.valueOf(count));
            tvCoachScheduledBadge.setVisibility(View. VISIBLE);
        } else {
            tvCoachScheduledBadge.setVisibility(View.GONE);
        }
    }

    private boolean isUpcomingWorkout(String dateStr, String timeStr) {
        try {
            // Assuming date format is "dd/MM/yyyy" or similar - adjust based on your actual format
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale. getDefault());
            Date workoutDate = dateFormat.parse(dateStr);

            if (workoutDate == null) return false;

            // Get current date without time
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return workoutDate.getTime() >= today.getTimeInMillis();
        } catch (ParseException e) {
            e.printStackTrace();
            return true;
        }
    }
}
