package com.example.stepbuystep.ActivityTrainee.TraineeHomeScreen;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.example.stepbuystep.R;
import com.example.stepbuystep.service.TrackingService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.Map;

public class TrackingActivity extends ComponentActivity {

    // ----------------------------- Constants ----------------------------

    /** Base reward rates — multiplied by shoe multiplier at session end. */
    private static final int BASE_RATE_WALKING = 100; // coins per km
    private static final int BASE_RATE_RUNNING  = 200; // coins per km

    private static final String KEY_SESSION_ACTIVE = "sessionActive";

    // ----------------------------- State --------------------------------

    private TrackingService trackingService;
    private boolean isBound      = false;
    private boolean sessionActive = false;

    /** "walking" or "running" — set once from the Dashboard intent, never changed here. */
    private String activityType = "walking";

    /** Multiplier of the user's current shoe; fetched from Firestore on launch. */
    private double currentShoeMultiplier = 1.0;

    // ----------------------------- Views --------------------------------

    private TextView tvStats;
    private Button   btnStart, btnStop;

    // ----------------------------- Firebase -----------------------------

    private FirebaseAuth      auth;
    private FirebaseFirestore db;

    // ----------------------------- UI update loop -----------------------

    private final Handler  handler             = new Handler();
    private final Runnable updateStatsRunnable = new Runnable() {
        @Override
        public void run() {
            if (isBound && trackingService != null) {
                double dist  = trackingService.getDistance();
                int    steps = trackingService.getSteps();
                tvStats.setText(String.format("%.2f km\nSteps: %d", dist, steps));
            }
            handler.postDelayed(this, 1000);
        }
    };

    // ----------------------------- Service connection -------------------

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            TrackingService.LocalBinder binder = (TrackingService.LocalBinder) service;
            trackingService = binder.getService();
            isBound = true;

            if (sessionActive) {
                // Session was already active (either started before binding completed,
                // or the activity was recreated mid-session — restore the UI and resume).
                btnStart.setVisibility(View.GONE);
                btnStop.setVisibility(View.VISIBLE);
                trackingService.startTracking(); // no-op if already tracking
                handler.post(updateStatsRunnable);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    // ----------------------------- Permission launcher ------------------

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Boolean granted : result.values()) {
                    if (!granted) { allGranted = false; break; }
                }
                if (allGranted) {
                    bindTrackingService();
                } else {
                    Toast.makeText(this, "Permissions required for tracking", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    // ===================================================================
    //  Lifecycle
    // ===================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tracking);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        // --- Resolve activity type from the Dashboard intent ---
        // TraineeHomeActivity passes "run" or "walk" as ACTIVITY_TYPE.
        String intentType = getIntent().getStringExtra("ACTIVITY_TYPE");
        activityType = "run".equals(intentType) ? "running" : "walking";

        // --- Bind views ---
        tvStats   = findViewById(R.id.tvStats);
        btnStart  = findViewById(R.id.btnStartTracking);
        btnStop   = findViewById(R.id.btnStopTracking);

        btnStart.setOnClickListener(v -> startSession());
        btnStop.setOnClickListener(v -> stopSession());

        // --- Restore session state after activity recreation (e.g. screen rotation) ---
        if (savedInstanceState != null && savedInstanceState.getBoolean(KEY_SESSION_ACTIVE, false)) {
            sessionActive = true;
            btnStart.setVisibility(View.GONE);
            btnStop.setVisibility(View.VISIBLE);
        }

        checkPermissions();        // → binds the service
        fetchBestEquipment();      // → loads shoe multiplier silently
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // Persist whether a tracking session is in progress so the UI survives
        // configuration changes (rotation, system-initiated recreation, etc.)
        outState.putBoolean(KEY_SESSION_ACTIVE, sessionActive);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateStatsRunnable);
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    // ===================================================================
    //  Permission & service binding
    // ===================================================================

    private void checkPermissions() {
        java.util.List<String> permList = new java.util.ArrayList<>();
        permList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permList.add(Manifest.permission.ACTIVITY_RECOGNITION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permList.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        String[] permissions = permList.toArray(new String[0]);
        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) bindTrackingService();
        else requestPermissionLauncher.launch(permissions);
    }

    private void bindTrackingService() {
        Intent intent = new Intent(this, TrackingService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    // ===================================================================
    //  Shoe multiplier (loaded silently; used in coin calculation)
    // ===================================================================

    private void fetchBestEquipment() {
        String uid = auth.getUid();
        if (uid == null) return;

        currentShoeMultiplier = 1.0; // default = Basic Runner

        db.collection("users").document(uid).collection("inventory")
                .whereEqualTo("type", "shoes")
                .get()
                .addOnSuccessListener(snapshots -> {
                    double best = 1.0;
                    for (QueryDocumentSnapshot doc : snapshots) {
                        Double m = doc.getDouble("multiplier");
                        if (m != null && m > best) best = m;
                    }
                    currentShoeMultiplier = best;
                });
    }

    // ===================================================================
    //  Session control
    // ===================================================================

    private void startSession() {
        // Lock the UI to "tracking" state immediately — before the async service call —
        // so the button cannot be pressed twice and no race condition can revert the state.
        sessionActive = true;
        btnStart.setVisibility(View.GONE);
        btnStop.setVisibility(View.VISIBLE);

        // Promote the service to foreground so Android won't kill it mid-session.
        Intent intent = new Intent(this, TrackingService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        // If the binding is already established, begin tracking immediately.
        // Otherwise onServiceConnected (above) will do it once the bind completes.
        if (!isBound || trackingService == null) return;
        trackingService.startTracking();
        handler.post(updateStatsRunnable);
    }

    private void stopSession() {
        if (!isBound || trackingService == null) return;

        // Capture distance / steps before the service resets them.
        final double distance = trackingService.getDistance();
        final int    steps    = trackingService.getSteps();

        trackingService.stopTracking();
        handler.removeCallbacks(updateStatsRunnable);
        sessionActive = false;

        // Final Coins = Distance (km) × Base Rate × Shoe Multiplier
        final int  baseRate    = activityType.equals("running") ? BASE_RATE_RUNNING : BASE_RATE_WALKING;
        final long earnedCoins = (long) (distance * baseRate * currentShoeMultiplier);

        final String uid = auth.getUid();
        if (uid == null) {
            finish();
            return;
        }

        // Always save the session first — no minimum distance gate.
        saveSessionToHistory(uid, distance, steps, earnedCoins);

        // Update coin balance if anything was earned (fire-and-forget; session is already saved).
        if (earnedCoins > 0) {
            db.collection("users").document(uid)
                    .update("coin_balance",
                            com.google.firebase.firestore.FieldValue.increment(earnedCoins))
                    .addOnSuccessListener(v ->
                            Toast.makeText(this,
                                    "Earned " + earnedCoins + " coins!", Toast.LENGTH_LONG).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Session saved, but coins could not be updated.",
                                    Toast.LENGTH_SHORT).show());
        } else {
            Toast.makeText(this, "Session saved!", Toast.LENGTH_SHORT).show();
        }
    }

    // ===================================================================
    //  Persistence
    // ===================================================================

    private void saveSessionToHistory(String uid, double distance, int steps, long earnedCoins) {
        String sessionType = activityType.equals("running") ? "Run" : "Walk";

        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
        String date = sdf.format(new java.util.Date());

        Map<String, Object> session = new java.util.HashMap<>();
        session.put("userId",      uid);
        session.put("type",        sessionType);
        session.put("distance",    distance);
        session.put("steps",       steps);
        session.put("earnedCoins", earnedCoins);
        session.put("date",        date);
        session.put("timestamp",   System.currentTimeMillis());

        db.collection("training_history").add(session)
                .addOnCompleteListener(task -> finish());
    }
}
