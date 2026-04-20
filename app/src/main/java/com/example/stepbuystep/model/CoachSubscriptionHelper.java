package com.example.stepbuystep.model;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class CoachSubscriptionHelper {

    public interface OnSubscriptionLoadListener {
        void onSubscriptionLoaded(int maxAthletes, int currentAthletes);
        void onError(String error);
    }

    public static void loadCoachSubscriptionAndCount(String coachUID, long coachID,
                                                     FirebaseFirestore db,
                                                     OnSubscriptionLoadListener listener) {

        // Load subscription tier (max athletes)
        db.collection("users").document(coachUID).get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        listener.onError("Coach document not found");
                        return;
                    }

                    int maxAthletes; // Default basic tier
                    Map<String, Object> subscription = (Map<String, Object>) doc.get("subscription");
                    if (subscription != null) {
                        Object maxObject = subscription.get("maxAthletes");
                        if (maxObject instanceof Number) {
                            maxAthletes = ((Number) maxObject).intValue();
                        } else {
                            maxAthletes = 20;
                        }
                    } else {
                        maxAthletes = 20;
                    }

                    // Count current approved athletes
                    db.collection("users")
                            .whereEqualTo("role", "trainee")
                            .whereEqualTo("coachID", coachID)
                            .whereEqualTo("status", "approved")
                            .addSnapshotListener((querySnapshot, error) -> {
                                if (error != null) {
                                    listener.onError(error.getMessage());
                                    return;
                                }

                                int currentAthletes = (querySnapshot != null) ? querySnapshot.size() : 0;
                                listener.onSubscriptionLoaded(maxAthletes, currentAthletes);
                            });
                })
                .addOnFailureListener(e -> listener.onError(e.getMessage()));
    }

    public static int getMaxAthletesFromTier(String tier) {
        switch (tier.toLowerCase()) {
            case "basic":
                return 20;
            case "pro":
                return 35;
            case "elite":
                return 50;
            default:
                return 20;
        }
    }
}