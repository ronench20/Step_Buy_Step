package com.example.stepbuystep.ActivityCommon;

import com.example.stepbuystep.model.ShoeLevel;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class NotificationManager {

    private final FirebaseFirestore db;

    public NotificationManager() {
        this.db = FirebaseFirestore. getInstance();
    }

    /**
     * Send notification to all group members when a trainee purchases a shoe
     * @param buyerUid The user who made the purchase
     * @param buyerName The name of the buyer
     * @param shoe Level The shoe level purchased
     */
    public void notifyGroupOnShoePurchase(String buyerUid, String buyerName, ShoeLevel shoe, OnCompleteListener listener) {
        // First, get the buyer's coachID to find group members
        db.collection("users").document(buyerUid).get()
                .addOnSuccessListener(buyerDoc -> {
                    if (! buyerDoc.exists()) {
                        listener.onComplete(false, "Buyer not found");
                        return;
                    }

                    Long coachId = buyerDoc. getLong("coachID");
                    if (coachId == null) {
                        listener.onComplete(false, "No coach assigned");
                        return;
                    }

                    // Find all trainees with the same coachID
                    db.collection("users")
                            .whereEqualTo("coachID", coachId)
                            .whereEqualTo("role", "trainee")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                int notificationCount = 0;

                                for (com.google.firebase.firestore. DocumentSnapshot doc : querySnapshot.getDocuments()) {
                                    String traineeId = doc.getId();

                                    // Don't notify the buyer themselves
                                    if (traineeId.equals(buyerUid)) {
                                        continue;
                                    }

                                    // Create notification
                                    Map<String, Object> notification = new HashMap<>();
                                    notification.put("type", "shoe_purchase");
                                    notification.put("buyerName", buyerName);
                                    notification.put("shoeLevel", shoe.getLevel());
                                    notification.put("shoeName", shoe.getName());
                                    notification.put("multiplier", shoe.getMultiplier());
                                    notification.put("message", buyerName + " purchased " + shoe.getName() + " (Level " + shoe.getLevel() + ")!");
                                    notification.put("timestamp", System.currentTimeMillis());
                                    notification.put("read", false);

                                    // Add to trainee's notifications sub-collection
                                    db.collection("users")
                                            .document(traineeId)
                                            .collection("notifications")
                                            .add(notification);

                                    notificationCount++;
                                }

                                listener.onComplete(true, "Notified " + notificationCount + " group members");
                            })
                            .addOnFailureListener(e -> listener.onComplete(false, e.getMessage()));
                })
                .addOnFailureListener(e -> listener.onComplete(false, e.getMessage()));
    }

    public interface OnCompleteListener {
        void onComplete(boolean success, String message);
    }
}