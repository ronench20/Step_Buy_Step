package com.example.stepbuystep.ActivityCoach.CoachSettingsScreen;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.stepbuystep.R;
import com.example.stepbuystep.model.SubscriptionTier;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SubscriptionSelectionDialog extends DialogFragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private String currentTier;
    private int currentAthleteCount = 0;
    private OnTierSelectedListener listener;

    public interface OnTierSelectedListener {
        void onTierSelected(String newTier);
        void onError(String error);
    }

    public void setListener(OnTierSelectedListener listener) {
        this.listener = listener;
    }

    public void setCurrentTier(String tier, int athleteCount) {
        this.currentTier = tier;
        this.currentAthleteCount = athleteCount;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.Theme_StepBuyStep);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_subscription_selection, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        LinearLayout tierBasic = view.findViewById(R.id.tierBasic);
        LinearLayout tierPro = view.findViewById(R.id.tierPro);
        LinearLayout tierElite = view.findViewById(R.id.tierElite);
        android.widget.Button btnCancel = view.findViewById(R.id.btnCancel);

        tierBasic.setOnClickListener(v -> handleTierSelection(SubscriptionTier.TIER_BASIC));
        tierPro.setOnClickListener(v -> handleTierSelection(SubscriptionTier.TIER_PRO));
        tierElite.setOnClickListener(v -> handleTierSelection(SubscriptionTier.TIER_ELITE));
        btnCancel.setOnClickListener(v -> dismiss());
    }

    private void handleTierSelection(String selectedTier) {
        // Get tier info
        SubscriptionTier tier = SubscriptionTier.getTierByName(selectedTier);

        if (selectedTier.equals(currentTier)) {
            // Already on this tier
            Toast.makeText(getContext(), "You are already on " + selectedTier + " tier",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // STRICT validation: before allowing any tier change, re-fetch the
        // authoritative Approved-athlete count from Firestore. The value passed
        // in from the settings screen comes from a TextView and may be 0 (still
        // loading) or stale, which previously let a coach downgrade past the cap.
        fetchAuthoritativeApprovedCount(actualCount -> {
            if (actualCount > tier.getMaxAthletes()) {
                int needToRemove = actualCount - tier.getMaxAthletes();
                // Block the downgrade outright and tell the coach exactly how many
                // athletes must be removed before switching to this plan.
                showDowngradeBlocked(selectedTier, tier.getMaxAthletes(), actualCount, needToRemove);
            } else {
                // Capacity is OK — safe to upgrade or downgrade.
                updateTierInFirebase(selectedTier, tier);
            }
        });
    }

    /**
     * Reads Firestore directly to count the coach's currently approved athletes.
     * This is the single source of truth used for downgrade gating.
     */
    private interface CountCallback {
        void onCount(int count);
    }

    private void fetchAuthoritativeApprovedCount(CountCallback cb) {
        if (auth.getCurrentUser() == null) {
            cb.onCount(currentAthleteCount); // fall back to cached value
            return;
        }
        String uid = auth.getCurrentUser().getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(coachDoc -> {
                    Long coachId = coachDoc.getLong("coachID");
                    if (coachId == null) {
                        cb.onCount(currentAthleteCount);
                        return;
                    }

                    db.collection("users")
                            .whereEqualTo("role", "trainee")
                            .whereEqualTo("coachID", coachId)
                            .whereEqualTo("status", "approved")
                            .get()
                            .addOnSuccessListener(qs -> {
                                int count = (qs != null) ? qs.size() : 0;
                                // Keep cached copy in sync so the UI reflects reality.
                                currentAthleteCount = count;
                                cb.onCount(count);
                            })
                            .addOnFailureListener(e -> cb.onCount(currentAthleteCount));
                })
                .addOnFailureListener(e -> cb.onCount(currentAthleteCount));
    }

    /**
     * Hard-blocks a downgrade that would overflow the target plan's athlete cap.
     * The coach is told exactly how many athletes must be removed. We still offer
     * a shortcut to the Manage Team Members screen, but the switch to the new
     * plan will NOT happen automatically — after removing athletes the coach must
     * come back to this dialog and pick the tier again, at which point the
     * authoritative re-fetch in handleTierSelection will let it through.
     */
    private void showDowngradeBlocked(String newTier, int maxAthletes,
                                      int actualCount, int needToRemove) {
        String message = "Downgrade blocked.\n\n"
                + "You currently have " + actualCount + " approved athlete(s).\n"
                + "The \"" + newTier + "\" plan allows a maximum of "
                + maxAthletes + " athlete(s).\n\n"
                + "You must remove " + needToRemove
                + " athlete(s) before switching to this plan.";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Cannot downgrade yet")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("Manage Team Members", (dialog, which) -> {
                    // Navigate to the removal screen — but do NOT pass any
                    // "pendingTierDowngrade" payload. The tier change will only
                    // go through if the coach re-opens this dialog afterwards.
                    Intent intent = new Intent(getContext(), ManageTeamMembersActivity.class);
                    startActivity(intent);
                    dismiss();
                })
                .setNegativeButton("Keep Current Plan", (dialog, which) -> {
                    // Explicitly do nothing: the plan is NOT changed.
                })
                .show();
    }

    private void updateTierInFirebase(String newTier, SubscriptionTier tier) {
        String uid = auth.getCurrentUser().getUid();
        if (uid == null) return;

        Map<String, Object> subscriptionData = new HashMap<>();
        subscriptionData.put("tier", tier.getTier());
        subscriptionData.put("maxAthletes", tier.getMaxAthletes());
        subscriptionData.put("price", tier.getPrice());

        db.collection("users").document(uid)
                .update("subscription", subscriptionData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Tier updated to " + newTier + "!",
                            Toast.LENGTH_SHORT).show();
                    dismiss();
                    if (listener != null) {
                        listener.onTierSelected(newTier);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update tier: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    if (listener != null) {
                        listener.onError(e.getMessage());
                    }
                });
    }
}