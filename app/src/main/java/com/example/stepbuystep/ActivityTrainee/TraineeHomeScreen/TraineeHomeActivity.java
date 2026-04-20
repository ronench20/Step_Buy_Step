package com.example.stepbuystep.ActivityTrainee.TraineeHomeScreen;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.bumptech.glide.Glide;
import com.example.stepbuystep.ActivityCommon.LoginActivity;
import com.example.stepbuystep.ActivityTrainee.BaseTraineeActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeReg.PendingApprovalActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeReg.ProfilePicturePickerBottomSheet;
import com.example.stepbuystep.ActivityTrainee.TraineeReg.ReEnterCoachIdActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.WorkoutAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TraineeHomeActivity extends BaseTraineeActivity
        implements ProfilePicturePickerBottomSheet.Listener {

    private static final String TAG = "TraineeHome";

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private TextView tvUserName, tvShoeCount, tvCurrentShoeName, tvCurrentShoeLevel,
            tvMultiplier, tvCoachScheduledBadge, tvNotificationBadge;
    private CardView btnCategoryRunning, btnCategoryWalking, btnCoachScheduled;
    private LinearLayout btnLogout;
    private FrameLayout btnNotifications;
    private ImageView ivDashProfilePic;

    private final List<WorkoutAdapter.WorkoutItem> workouts = new ArrayList<>();

    /** Live listener for coach-scheduled workouts. Detached in onDestroy. */
    private ListenerRegistration workoutsReg;

    /** Temp file URI the camera writes to. */
    private Uri pendingCameraUri;

    // --------------------- Activity result launchers ----------------------

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    granted -> {
                        if (granted) launchCamera();
                        else Toast.makeText(this, "Camera permission denied",
                                Toast.LENGTH_SHORT).show();
                    });

    private final ActivityResultLauncher<Uri> takePictureLauncher =
            registerForActivityResult(new ActivityResultContracts.TakePicture(),
                    success -> {
                        if (success && pendingCameraUri != null) {
                            handlePickedImage(pendingCameraUri);
                        }
                    });

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(),
                    uri -> { if (uri != null) handlePickedImage(uri); });

    // ------------------------------ Lifecycle -----------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainee_home);

        auth    = FirebaseAuth.getInstance();
        db      = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        initViews();
        setupNavigationBar(NavItem.DASHBOARD);
        setupListeners();
        loadUserData();
        fetchUnreadMessageCount();
        startWorkoutsLiveListener(); // live coach-schedule sync
    }

    private void initViews() {
        tvUserName            = findViewById(R.id.tvUserName);
        tvShoeCount           = findViewById(R.id.tvShoeCount);
        tvCurrentShoeName     = findViewById(R.id.tvCurrentShoeName);
        tvCurrentShoeLevel    = findViewById(R.id.tvCurrentShoeLevel);
        tvMultiplier          = findViewById(R.id.tvMultiplier);

        btnCategoryRunning    = findViewById(R.id.btnCategoryRunning);
        btnCategoryWalking    = findViewById(R.id.btnCategoryWalking);

        btnLogout             = findViewById(R.id.btnLogout);

        btnNotifications      = findViewById(R.id.btnNotifications);
        tvNotificationBadge   = findViewById(R.id.tvNotificationBadge);

        btnCoachScheduled     = findViewById(R.id.btnCoachScheduled);
        tvCoachScheduledBadge = findViewById(R.id.tvCoachScheduledBadge);

        ivDashProfilePic      = findViewById(R.id.ivDashProfilePic);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        btnCategoryRunning.setOnClickListener(v -> startTrackingActivity("run"));
        btnCategoryWalking.setOnClickListener(v -> startTrackingActivity("walk"));
        btnCoachScheduled.setOnClickListener(v -> showUpcomingWorkoutsDialog());
        btnNotifications.setOnClickListener(v -> openMessagesScreen());

        // NEW: tap profile avatar -> picker
        ivDashProfilePic.setOnClickListener(v ->
                new ProfilePicturePickerBottomSheet()
                        .show(getSupportFragmentManager(),
                                ProfilePicturePickerBottomSheet.TAG));
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkApprovalStatus();
        loadUserData();
        // Note: no loadWorkouts() call here anymore — the snapshot listener keeps it fresh.
        fetchUnreadMessageCount();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (workoutsReg != null) {
            workoutsReg.remove();
            workoutsReg = null;
        }
    }

    // ----------------------- Profile picture flow -----------------------

    @Override
    public void onCameraSelected() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchCamera();
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    @Override
    public void onGallerySelected() {
        pickImageLauncher.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void launchCamera() {
        try {
            File dir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
            File photo = File.createTempFile("dash_profile_", ".jpg", dir);
            pendingCameraUri = FileProvider.getUriForFile(this,
                    getPackageName() + ".fileprovider", photo);
            takePictureLauncher.launch(pendingCameraUri);
        } catch (IOException e) {
            Log.e(TAG, "Camera file error", e);
            Toast.makeText(this, "Couldn't start camera: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** Show preview + upload to Storage + persist URL in Firestore. */
    private void handlePickedImage(@NonNull Uri uri) {
        String uid = auth.getUid();
        if (uid == null) return;

        // Immediate preview using Glide (no re-decode cost in memory).
        Glide.with(this).load(uri).circleCrop().into(ivDashProfilePic);

        StorageReference ref = storage.getReference()
                .child("profile_pictures/" + uid + ".jpg");

        ref.putFile(uri)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return ref.getDownloadUrl();
                })
                .addOnSuccessListener(downloadUri -> {
                    String url = downloadUri.toString();
                    db.collection("users").document(uid)
                            .update("profileImageUrl", url)
                            .addOnSuccessListener(u ->
                                    Toast.makeText(this, "Profile picture updated",
                                            Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this,
                                    "Couldn't save URL: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Upload failed: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    // ------------------------------ Loaders -----------------------------

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String email = doc.getString("email");
                    String name  = email != null ? email.split("@")[0] : "Trainee";
                    if (!name.isEmpty()) {
                        name = name.substring(0, 1).toUpperCase() + name.substring(1);
                    }
                    tvUserName.setText(name);

                    Long coins = doc.getLong("coin_balance");
                    tvShoeCount.setText(coins != null ? String.valueOf(coins) : "0");

                    // NEW: profile picture
                    String url = doc.getString("profileImageUrl");
                    if (url != null && !url.isEmpty()) {
                        Glide.with(this).load(url).circleCrop().into(ivDashProfilePic);
                    }

                    fetchBestShoe(uid);
                })
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Failed to load data", Toast.LENGTH_SHORT).show());
        fetchUnreadMessageCount();
    }

    private void fetchBestShoe(String uid) {
        db.collection("users").document(uid).collection("inventory")
                .orderBy("multiplier", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(qs -> {
                    String bestName = "Basic Runner";
                    long   bestTier = 1;
                    double bestMult = 1.0;

                    if (!qs.isEmpty()) {
                        com.google.firebase.firestore.DocumentSnapshot doc = qs.getDocuments().get(0);
                        Double m = doc.getDouble("multiplier");
                        Long   t = doc.getLong("tier");
                        String n = doc.getString("name");
                        if (m != null) bestMult = m;
                        if (t != null) bestTier = t;
                        if (n != null) bestName = n;
                    }

                    tvCurrentShoeName.setText(bestName);
                    tvCurrentShoeLevel.setText("Level " + bestTier);
                    tvMultiplier.setText(bestMult + "x"); // still written (now hidden)

                    updateMultiplierDisplays(bestMult);
                });
    }

    private void updateMultiplierDisplays(double currentMultiplier) {
        TextView tvWalkingMultiplier = findViewById(R.id.tvWalkingMultiplier);
        TextView tvRunningMultiplier = findViewById(R.id.tvRunningMultiplier);

        if (tvWalkingMultiplier != null) {
            tvWalkingMultiplier.setText(String.format("%.0f coins/km", 100 * currentMultiplier));
        }
        if (tvRunningMultiplier != null) {
            tvRunningMultiplier.setText(String.format("%.0f coins/km", 200 * currentMultiplier));
        }
    }

    // ---------------------- REAL-TIME coach schedule ----------------------

    /**
     * Replaces the old one-shot {@code loadWorkouts()} {@code .get()} call.
     *
     * Mirrors how {@link #fetchUnreadMessageCount()} keeps the bell badge fresh —
     * a Firestore {@code addSnapshotListener} fires immediately with the current
     * data and then again on every subsequent change, so the "Coach Scheduled"
     * card updates without needing a screen transition.
     */
    private void startWorkoutsLiveListener() {
        String uid = auth.getUid();
        if (uid == null) return;

        if (workoutsReg != null) workoutsReg.remove();

        workoutsReg = db.collection("workouts")
                .whereArrayContains("traineeIds", uid)
                .addSnapshotListener((qs, error) -> {
                    if (error != null) {
                        Log.e(TAG, "workouts listener error", error);
                        return;
                    }
                    if (qs == null) return;

                    List<WorkoutAdapter.WorkoutItem> all      = new ArrayList<>();
                    List<WorkoutAdapter.WorkoutItem> upcoming = new ArrayList<>();

                    for (QueryDocumentSnapshot d : qs) {
                        WorkoutAdapter.WorkoutItem w = new WorkoutAdapter.WorkoutItem(
                                d.getString("type"),
                                d.getString("date"),
                                d.getString("time"),
                                d.getString("location"));
                        all.add(w);
                        if (isUpcomingWorkout(w.date, w.time)) upcoming.add(w);
                    }

                    workouts.clear();
                    workouts.addAll(all);
                    updateCoachScheduledBadge(upcoming.size());
                });
    }

    private void updateCoachScheduledBadge(int count) {
        if (count > 0) {
            tvCoachScheduledBadge.setText(String.valueOf(count));
            tvCoachScheduledBadge.setVisibility(View.VISIBLE);
        } else {
            tvCoachScheduledBadge.setVisibility(View.GONE);
        }
    }

    // ------------------------- Messages / badges --------------------------

    private void fetchUnreadMessageCount() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("messages")
                .whereEqualTo("traineeId", uid)
                .whereEqualTo("isRead", false)
                .addSnapshotListener((qs, err) -> {
                    if (err != null || qs == null) return;
                    updateNotificationBadge(qs.size());
                });
    }

    private void updateNotificationBadge(int count) {
        if (count > 0) {
            tvNotificationBadge.setText(String.valueOf(count));
            tvNotificationBadge.setVisibility(View.VISIBLE);
        } else {
            tvNotificationBadge.setVisibility(View.GONE);
        }
    }

    // -------------------------- Approval routing --------------------------

    private void checkApprovalStatus() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    String status = doc.getString("status");

                    if ("pending".equals(status) || "rejected".equals(status)) {
                        Toast.makeText(this, "Your account status has changed", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, PendingApprovalActivity.class));
                        finish();
                    } else if ("removed".equals(status)) {
                        Toast.makeText(this, "Your coach has removed you from their team",
                                Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(this, ReEnterCoachIdActivity.class));
                        finish();
                    }
                });
    }

    // -------------------------------- Misc --------------------------------

    private void openMessagesScreen() {
        startActivity(new Intent(this, MessagesActivity.class));
    }

    private void startTrackingActivity(String type) {
        Intent intent = new Intent(this, TrackingActivity.class);
        intent.putExtra("ACTIVITY_TYPE", type);
        startActivity(intent);
    }

    private void showUpcomingWorkoutsDialog() {
        List<WorkoutAdapter.WorkoutItem> upcoming = new ArrayList<>();
        for (WorkoutAdapter.WorkoutItem w : workouts) {
            if (isUpcomingWorkout(w.date, w.time)) upcoming.add(w);
        }
        UpcomingWorkoutsDialog.newInstance(upcoming)
                .show(getSupportFragmentManager(), "UpcomingWorkoutsDialog");
    }

    private boolean isUpcomingWorkout(String dateStr, String timeStr) {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            Date workoutDate = fmt.parse(dateStr);
            if (workoutDate == null) return false;

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            return workoutDate.getTime() >= today.getTimeInMillis();
        } catch (ParseException e) {
            return true;
        }
    }
}
