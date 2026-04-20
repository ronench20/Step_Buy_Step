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

        // Check if downgrading
        if (currentAthleteCount > tier.getMaxAthletes()) {
            // Need to remove athletes first
            int needToRemove = currentAthleteCount - tier.getMaxAthletes();
            showDowngradeWarning(selectedTier, tier.getMaxAthletes(), needToRemove);
        } else {
            // Can upgrade/downgrade directly
            updateTierInFirebase(selectedTier, tier);
        }
    }

    private void showDowngradeWarning(String newTier, int maxAthletes, int needToRemove) {
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(requireContext());

        builder.setTitle("Downgrade Warning")
                .setMessage("You have " + currentAthleteCount + " athletes.\n\n" +
                        "The " + newTier + " tier allows only " + maxAthletes + " athletes.\n\n" +
                        "You need to remove at least " + needToRemove + " athlete(s) before downgrading.\n\n" +
                        "Open Manage Team Members to remove athletes.")
                .setPositiveButton("Manage Team Members", (dialog, which) -> {
                    // ===== PASS TIER INFO TO ManageTeamMembersActivity =====
                    Intent intent = new Intent(getContext(), ManageTeamMembersActivity.class);
                    intent.putExtra("pendingTierDowngrade", newTier);
                    intent.putExtra("pendingTierMaxAthletes", maxAthletes);
                    startActivity(intent);
                    dismiss();
                })
                .setNegativeButton("Cancel", null)
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