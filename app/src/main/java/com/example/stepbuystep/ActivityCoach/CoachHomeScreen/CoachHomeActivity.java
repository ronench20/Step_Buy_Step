package com.example.stepbuystep.ActivityCoach.CoachHomeScreen;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget. TextView;
import android.widget. Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityCoach.BaseCoachActivity;
import com.example.stepbuystep.ActivityCommon.LeaderBoardActivity;
import com.example.stepbuystep.ActivityCommon.LoginActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.adapter.UpcomingWorkoutAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CoachHomeActivity extends BaseCoachActivity {

    private static final String TAG = "CoachHome";
    /** Max number of upcoming workouts to surface on the dashboard card. */
    private static final int UPCOMING_LIMIT = 3;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvCoachIdShort;
    private TextView tvUserName;
    private TextView tvActiveAthletesCount;
    private TextView tvPendingRequestsCount;
    private LinearLayout cardActiveAthletes;
    private LinearLayout cardPendingRequests;
    private TextView badgeUpcomingCount;
    private LinearLayout btnCopyCoachId;
    private LinearLayout btnBroadcastMessage;
    private LinearLayout btnLogout;
    private RecyclerView rvUpcoming;
    private View cardNoUpcoming;

    private long coachIdValue = 0;
    private UpcomingWorkoutAdapter upcomingAdapter;

    /** Live listener on the coach's workouts. Attached in onStart, detached in onStop. */
    private ListenerRegistration upcomingWorkoutsReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coach_home);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupNavigationBar(BaseCoachActivity.NavItem.DASH_COACH);
        setupRecyclerView();
        setupListeners();
        fetchCoachData();
    }

    private void initViews() {
        tvUserName = findViewById(R.id.tvUserName);
        tvCoachIdShort = findViewById(R.id.tvCoachIdShort);
        tvActiveAthletesCount = findViewById(R.id.tvActiveAthletesCount);
        cardActiveAthletes = findViewById(R.id.cardActiveAthletes);
        tvPendingRequestsCount = findViewById(R.id.tvPendingRequestsCount);
        cardPendingRequests = findViewById(R.id.cardPendingRequests);
        badgeUpcomingCount = findViewById(R.id.badgeUpcomingCount);

        btnCopyCoachId = findViewById(R.id.btnCopyCoachId);
        btnBroadcastMessage = findViewById(R.id.btnBroadcastMessage);
        btnLogout = findViewById(R. id.btnLogout);
        rvUpcoming = findViewById(R.id.rvUpcoming);
        cardNoUpcoming = findViewById(R.id.cardNoUpcoming);
    }

    private void setupRecyclerView() {
        upcomingAdapter = new UpcomingWorkoutAdapter();
        rvUpcoming.setLayoutManager(new LinearLayoutManager(this));
        rvUpcoming.setAdapter(upcomingAdapter);
    }

    private void setupListeners() {
        btnLogout.setOnClickListener(v -> {
            auth. signOut();
            startActivity(new Intent(this, LoginActivity. class));
            finish();
        });

        btnCopyCoachId.setOnClickListener(v -> {
            if (coachIdValue != 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Coach ID", String.valueOf(coachIdValue));
                clipboard. setPrimaryClip(clip);
                Toast.makeText(this, "Copied ID: " + coachIdValue, Toast.LENGTH_SHORT).show();
            }
        });

        btnBroadcastMessage.setOnClickListener(v -> showBroadcastDialog());

        cardActiveAthletes.setOnClickListener(v ->
                startActivity(new Intent(this, LeaderBoardActivity.class))
        );

        cardPendingRequests.setOnClickListener(v ->
                startActivity(new Intent(this, PendingRequestsActivity.class))
        );
    }

    private void showBroadcastDialog() {
        BroadcastMessageDialogActivity dialog = new BroadcastMessageDialogActivity();
        dialog.show(getSupportFragmentManager(),"BroadcastMessageDialog");
    }

    private void fetchCoachData() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Long cid = doc.getLong("coachID");
                        if (cid != null) {
                            coachIdValue = cid;
                            tvCoachIdShort.setText("ID: " + coachIdValue);
                            fetchAthletesCount(coachIdValue);
                            fetchPendingRequestsCount(coachIdValue);
                        } else {
                            tvCoachIdShort.setText("ID: N/A");
                        }
                        String email = doc.getString("email");
                        String name = email != null ? email.split("@")[0] : "Trainee";
                        if (name.length() > 0) {
                            name = name.substring(0, 1).toUpperCase() + name.substring(1);
                        }

                        tvUserName.setText(name);
                    }
                });
    }

    private void fetchPendingRequestsCount(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .whereEqualTo("status", "pending")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;
                    if (querySnapshot != null) {
                        int count = querySnapshot. size();
                        tvPendingRequestsCount.setText(String.valueOf(count));
                    }
                });
    }

    private void fetchAthletesCount(long coachId) {
        db.collection("users")
                .whereEqualTo("role", "trainee")
                .whereEqualTo("coachID", coachId)
                .whereEqualTo("status", "approved")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) return;
                    if (querySnapshot != null) {
                        int count = querySnapshot.size();
                        tvActiveAthletesCount.setText(String.valueOf(count));
                    }
                });
    }



    /**
     * Real-time Upcoming Workouts feed.
     *
     * <p>Previous implementation used a one-shot {@code .get()} plus
     * {@code orderBy("createdAt", DESCENDING).limit(3)} — which has two problems:
     * <ul>
     *   <li>{@code whereEqualTo + orderBy} requires a Firestore composite index;
     *       when the index is missing the query fails silently and the "Upcoming
     *       Workouts" section stays empty even after a successful save.</li>
     *   <li>"Most recently created" isn't the same as "upcoming". A freshly-scheduled
     *       workout for next week and a month-old past workout would both appear.</li>
     * </ul>
     *
     * <p>New implementation subscribes with {@code addSnapshotListener} so newly
     * added workouts push straight onto the dashboard, filters to future date/time
     * client-side (no composite index needed), and sorts by the next occurrence so
     * the soonest workout shows first.
     */
    private void startUpcomingWorkoutsListener() {
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        // Detach any previous registration so we don't double-listen after a config
        // change or navigation back.
        if (upcomingWorkoutsReg != null) {
            upcomingWorkoutsReg.remove();
            upcomingWorkoutsReg = null;
        }

        upcomingWorkoutsReg = db.collection("workouts")
                .whereEqualTo("coachId", uid)
                .addSnapshotListener((qs, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Upcoming workouts listener error", error);
                        Toast.makeText(this,
                                "Couldn't load upcoming workouts: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (qs == null) return;

                    long now = System.currentTimeMillis();
                    List<UpcomingWorkoutAdapter.WorkoutItem> items = new ArrayList<>();
                    // Keep the parsed timestamps alongside items so we can sort without
                    // parsing twice.
                    List<Long> sortKeys = new ArrayList<>();

                    for (QueryDocumentSnapshot doc : qs) {
                        String type = doc.getString("type");
                        String date = doc.getString("date");
                        String time = doc.getString("time");
                        String location = doc.getString("location");

                        if (type == null) type = "Workout";
                        if (date == null) date = "";
                        if (time == null) time = "";
                        if (location == null) location = "";

                        long whenMs = parseWorkoutDateTime(date, time);
                        // Skip past workouts — "Upcoming" literally means in the future.
                        // parseWorkoutDateTime returns Long.MAX_VALUE on parse failure
                        // so malformed dates still surface (safer than hiding them).
                        if (whenMs < now) continue;

                        int participants = 0;
                        Object traineeIds = doc.get("traineeIds");
                        if (traineeIds instanceof List) {
                            participants = ((List<?>) traineeIds).size();
                        }

                        items.add(new UpcomingWorkoutAdapter.WorkoutItem(
                                doc.getId(), type, date, time, location, participants));
                        sortKeys.add(whenMs);
                    }

                    // Sort soonest-first by zipping items with their parsed millis.
                    List<Integer> indices = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) indices.add(i);
                    Collections.sort(indices, new Comparator<Integer>() {
                        @Override public int compare(Integer a, Integer b) {
                            return Long.compare(sortKeys.get(a), sortKeys.get(b));
                        }
                    });

                    List<UpcomingWorkoutAdapter.WorkoutItem> sorted = new ArrayList<>();
                    for (int i = 0; i < Math.min(indices.size(), UPCOMING_LIMIT); i++) {
                        sorted.add(items.get(indices.get(i)));
                    }

                    if (sorted.isEmpty()) {
                        rvUpcoming.setVisibility(View.GONE);
                        cardNoUpcoming.setVisibility(View.VISIBLE);
                        badgeUpcomingCount.setText("0");
                        upcomingAdapter.setItems(new ArrayList<>());
                    } else {
                        rvUpcoming.setVisibility(View.VISIBLE);
                        cardNoUpcoming.setVisibility(View.GONE);
                        // Badge reflects the FULL number of upcoming workouts, not just
                        // the 3 we render in the preview card.
                        badgeUpcomingCount.setText(String.valueOf(items.size()));
                        upcomingAdapter.setItems(sorted);
                    }
                });
    }

    /**
     * Parse the workout date ("dd/MM/yyyy") and time ("HH:mm") into a millisecond
     * timestamp. Returns {@link Long#MAX_VALUE} on any parse failure so malformed
     * records surface at the bottom of the sort and are treated as "future" — safer
     * than silently dropping them.
     */
    private static long parseWorkoutDateTime(String date, String time) {
        if (date == null || date.isEmpty()) return Long.MAX_VALUE;
        String timePart = (time == null || time.isEmpty()) ? "00:00" : time;
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            Date d = fmt.parse(date + " " + timePart);
            return d != null ? d.getTime() : Long.MAX_VALUE;
        } catch (ParseException e) {
            return Long.MAX_VALUE;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Snapshot listener handles live updates; attach here (mirrors TraineeHomeActivity).
        startUpcomingWorkoutsListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (upcomingWorkoutsReg != null) {
            upcomingWorkoutsReg.remove();
            upcomingWorkoutsReg = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (coachIdValue != 0) {
            fetchAthletesCount(coachIdValue);
            fetchPendingRequestsCount(coachIdValue);
        }
        // No manual refresh of upcoming workouts needed — the snapshot listener keeps
        // the list in sync as the coach saves new workouts or they become past-due.
    }
}