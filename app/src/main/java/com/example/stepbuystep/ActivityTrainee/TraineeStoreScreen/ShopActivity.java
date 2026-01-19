package com.example.stepbuystep.ActivityTrainee.TraineeStoreScreen;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.ActivityTrainee.BaseTraineeActivity;
import com. example.stepbuystep.R;
import com.example.stepbuystep.adapter.ShopAdapter;
import com.example.stepbuystep.ActivityCommon.NotificationManager;
import com.example.stepbuystep.model.ShoeLevel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopActivity extends BaseTraineeActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private NotificationManager notificationManager;

    private TextView tvCoinBalance;
    private TextView tvCurrentShoeName, tvCurrentShoeLevel, tvCurrentMultiplier;
    private RecyclerView rvShoes;

    private long currentCoins = 0;
    private int currentShoeLevel = 1; // Default level 1
    private ShopAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trainee_shop);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        notificationManager = new NotificationManager();

        initViews();
        setupRecyclerView();
        setupNavigationBar(NavItem.SHOE_STORE);
        loadUserData();
    }

    private void initViews() {
        tvCoinBalance = findViewById(R.id.tvCoinBalance);
        tvCurrentShoeName = findViewById(R.id.tvCurrentShoeName);
        tvCurrentShoeLevel = findViewById(R. id.tvCurrentShoeLevel);
        tvCurrentMultiplier = findViewById(R.id.tvCurrentMultiplier);
        rvShoes = findViewById(R.id.rvShoes);
    }

    private void setupRecyclerView() {
        adapter = new ShopAdapter();
        adapter.setListener(this::purchaseShoe);
        rvShoes.setLayoutManager(new LinearLayoutManager(this));
        rvShoes.setAdapter(adapter);
    }

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (e != null || doc == null || !doc.exists()) return;

            Long coins = doc.getLong("coin_balance");
            currentCoins = (coins != null) ? coins : 0;
            tvCoinBalance.setText(String.valueOf(currentCoins));

            // Get current shoe level from inventory
            fetchCurrentShoeLevel(uid);
        });
    }

    private void fetchCurrentShoeLevel(String uid) {
        db.collection("users").document(uid).collection("inventory")
                .orderBy("tier", com.google.firebase.firestore. Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        Long tier = querySnapshot.getDocuments().get(0).getLong("tier");
                        currentShoeLevel = (tier != null) ? tier.intValue() : 1;
                    } else {
                        currentShoeLevel = 1;
                    }

                    updateShoeDisplay();
                })
                .addOnFailureListener(e -> {
                    currentShoeLevel = 1;
                    updateShoeDisplay();
                });
    }

    private void updateShoeDisplay() {
        // Get all shoes with their states
        List<ShoeLevel> allShoes = ShoeProgressionManager.getAllShoesWithStates(currentShoeLevel);
        adapter.setShoes(allShoes, currentCoins);

        // Update current shoe info display
        ShoeLevel currentShoe = allShoes. get(currentShoeLevel - 1); // 0-indexed
        tvCurrentShoeName.setText(currentShoe.getName());
        tvCurrentShoeLevel.setText("Level " + currentShoe.getLevel());
        tvCurrentMultiplier.setText(currentShoe. getMultiplier() + "x");
    }

    private void purchaseShoe(ShoeLevel shoe) {
        if (currentCoins < shoe.getPrice()) {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Can only buy next level
        if (shoe.getLevel() != currentShoeLevel + 1) {
            Toast. makeText(this, "You can only buy the next level shoe!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getUid();
        if (uid == null) return;

        // Transaction to purchase
        db.runTransaction(transaction -> {
            transaction.update(db.collection("users").document(uid),
                    "coin_balance", currentCoins - shoe.getPrice());

            Map<String, Object> invItem = new HashMap<>();
            invItem.put("type", "shoes");
            invItem.put("tier", shoe.getLevel());
            invItem.put("name", shoe.getName());
            invItem. put("multiplier", shoe.getMultiplier());
            invItem.put("purchasedAt", System.currentTimeMillis());

            transaction.set(db.collection("users").document(uid)
                    .collection("inventory")
                    .document("shoe_" + shoe.getLevel()), invItem);

            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Purchased " + shoe.getName() + "!", Toast.LENGTH_SHORT).show();
            currentShoeLevel = shoe.getLevel();

            // Send notification to group
            sendNotificationToGroup(shoe);

            updateShoeDisplay();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void sendNotificationToGroup(ShoeLevel shoe) {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).get()
                .addOnSuccessListener(doc -> {
                    String name = doc.getString("email");
                    if (name != null && name.contains("@")) {
                        name = name.split("@")[0];
                    }

                    final String buyerName = name;
                    notificationManager.notifyGroupOnShoePurchase(uid, buyerName, shoe,
                            (success, message) -> {
                                // Optional: Show toast or log
                                if (success) {
                                    Toast.makeText(this, "Team notified!", Toast.LENGTH_SHORT).show();
                                }
                            });
                });
    }
}