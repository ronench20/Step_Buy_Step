package com.example.stepbuystep;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.cardview.widget.CardView;

import com.example.stepbuystep.model.Equipment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopActivity extends ComponentActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvCoinBalance;
    private TextView tvCurrentShoeName, tvCurrentShoeLevel, tvCurrentMultiplier;
    private LinearLayout llShopContainer;
    private ImageView btnBack;

    // Bottom Nav
    private LinearLayout navDashboard, navHistory, navLeaderboard;

    private long currentCoins = 0;
    private Map<String, Integer> currentInventoryTiers = new HashMap<>();
    private List<Equipment> availableUpgrades = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupListeners();
        loadUserData();
    }

    private void initViews() {
        tvCoinBalance = findViewById(R.id.tvCoinBalance);
        tvCurrentShoeName = findViewById(R.id.tvCurrentShoeName);
        tvCurrentShoeLevel = findViewById(R.id.tvCurrentShoeLevel);
        tvCurrentMultiplier = findViewById(R.id.tvCurrentMultiplier);
        llShopContainer = findViewById(R.id.llShopContainer);
        btnBack = findViewById(R.id.btnBack);

        navDashboard = findViewById(R.id.navDashboard);
        navHistory = findViewById(R.id.navHistory);
        navLeaderboard = findViewById(R.id.navLeaderboard);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        navDashboard.setOnClickListener(v -> {
            startActivity(new Intent(this, TraineeHomeActivity.class));
            finish();
        });

        navHistory.setOnClickListener(v ->
             Toast.makeText(this, "History feature coming soon", Toast.LENGTH_SHORT).show()
        );

        navLeaderboard.setOnClickListener(v ->
             startActivity(new Intent(this, GroupListActivity.class))
        );
    }

    private void loadUserData() {
        String uid = auth.getUid();
        if (uid == null) return;

        db.collection("users").document(uid).addSnapshotListener((doc, e) -> {
            if (e != null) return;
            if (doc != null && doc.exists()) {
                Long coins = doc.getLong("coin_balance");
                currentCoins = (coins != null) ? coins : 0;
                tvCoinBalance.setText(String.valueOf(currentCoins));

                fetchInventoryAndLoadShop(uid);
            }
        });
    }

    private void fetchInventoryAndLoadShop(String uid) {
        currentInventoryTiers.clear();
        currentInventoryTiers.put("walking_shoes", 0);
        currentInventoryTiers.put("running_shoes", 0);
        currentInventoryTiers.put("coach_token", 0);

        // Also find best shoe for "Current Shoe" display
        final String[] bestName = {"Basic Runner"};
        final int[] bestTier = {1};
        final double[] bestMult = {1.0};
        final boolean[] foundBest = {false};

        db.collection("users").document(uid).collection("inventory")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        String type = doc.getString("type");
                        Long tier = doc.getLong("tier");
                        Double m = doc.getDouble("multiplier");
                        String n = doc.getString("name");

                        if (type != null && tier != null) {
                            int currentMax = currentInventoryTiers.getOrDefault(type, 0);
                            if (tier > currentMax) {
                                currentInventoryTiers.put(type, tier.intValue());
                            }
                        }

                        if (m != null && m >= bestMult[0]) {
                            bestMult[0] = m;
                            if (n != null) bestName[0] = n;
                            if (tier != null) bestTier[0] = tier.intValue();
                            foundBest[0] = true;
                        }
                    }

                    if (foundBest[0]) {
                        tvCurrentShoeName.setText(bestName[0]);
                        tvCurrentShoeLevel.setText("Level " + bestTier[0]);
                        tvCurrentMultiplier.setText(bestMult[0] + "x");
                    }

                    loadShopItems();
                });
    }

    private void loadShopItems() {
        availableUpgrades.clear();

        addNextUpgrade("walking_shoes", "Basic Runner", currentInventoryTiers.get("walking_shoes"));
        addNextUpgrade("running_shoes", "Sport Jogger", currentInventoryTiers.get("running_shoes"));
        // addNextUpgrade("coach_token", "Coach Token", currentInventoryTiers.get("coach_token")); // Hidden if not in UI mock

        populateShopUI();
    }

    private void addNextUpgrade(String type, String baseName, int currentTier) {
        if (currentTier >= 4) return;

        int nextTier = currentTier + 1;
        int price = nextTier * 500; // Example: 500, 1000, 1500
        if (type.equals("walking_shoes") && nextTier == 1) price = 0; // First one might be free/default

        double multiplier = 1.0;
        if (type.equals("walking_shoes")) multiplier = nextTier * 1.0;
        else if (type.equals("running_shoes")) multiplier = nextTier * 1.5;

        Equipment item = new Equipment(type + "_" + nextTier, baseName, type, nextTier, price, multiplier);
        availableUpgrades.add(item);
    }

    private void populateShopUI() {
        llShopContainer.removeAllViews();

        // Re-implementing manually with code to avoid missing layout crash
        for (Equipment item : availableUpgrades) {
            View card = createShopCard(item);
            llShopContainer.addView(card);
            // Add margin
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
            params.setMargins(0, 0, 0, 32);
            card.setLayoutParams(params);
        }
    }

    private View createShopCard(Equipment item) {
        // Programmatic UI creation to match `trainee store.png` item card
        // CardView -> LinearLayout (Horizontal) -> Icon, Info, Button

        CardView card = new CardView(this);
        card.setCardElevation(4);
        card.setRadius(32); // 16dp roughly
        card.setCardBackgroundColor(Color.WHITE);
        card.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setPadding(48, 48, 48, 48); // 16dp
        root.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Icon Box
        LinearLayout iconBox = new LinearLayout(this);
        iconBox.setLayoutParams(new LinearLayout.LayoutParams(150, 150)); // 50dp
        iconBox.setBackgroundResource(android.R.drawable.dialog_holo_light_frame); // Placeholder
        // Better: use a shape drawable programmatically or a color
        iconBox.setBackgroundColor(0xFFF3F4F6); // Gray 100
        iconBox.setGravity(android.view.Gravity.CENTER);

        ImageView icon = new ImageView(this);
        icon.setImageResource(R.drawable.ic_shoe); // Use lock if locked?
        if (item.getPrice() > currentCoins) {
             icon.setImageResource(R.drawable.ic_lock);
             icon.setColorFilter(0xFF9CA3AF);
        } else {
             icon.setColorFilter(0xFF007AFF);
        }
        iconBox.addView(icon);

        root.addView(iconBox);

        // Text Info
        LinearLayout textInfo = new LinearLayout(this);
        textInfo.setOrientation(LinearLayout.VERTICAL);
        textInfo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.0f));
        textInfo.setPadding(32, 0, 0, 0); // 16dp margin start

        TextView name = new TextView(this);
        name.setText(item.getName());
        name.setTextSize(18);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setTextColor(0xFF030213);
        textInfo.addView(name);

        LinearLayout levelBadge = new LinearLayout(this);
        levelBadge.setOrientation(LinearLayout.HORIZONTAL);
        levelBadge.setPadding(0, 8, 0, 0);

        TextView level = new TextView(this);
        level.setText("Level " + item.getTier());
        level.setBackgroundResource(android.R.drawable.btn_default); // Placeholder
        level.setPadding(16, 4, 16, 4);
        level.setTextSize(12);
        levelBadge.addView(level);

        TextView mult = new TextView(this);
        mult.setText(item.getMultiplier() + "x");
        mult.setBackgroundColor(0xFF34C759); // Green
        mult.setTextColor(Color.WHITE);
        mult.setPadding(16, 4, 16, 4);
        mult.setTextSize(12);
        LinearLayout.LayoutParams multParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        multParams.setMargins(16, 0, 0, 0);
        mult.setLayoutParams(multParams);
        levelBadge.addView(mult);

        textInfo.addView(levelBadge);

        TextView price = new TextView(this);
        price.setText(item.getPrice() + " coins");
        price.setTextColor(0xFF717182);
        price.setTextSize(14);
        textInfo.addView(price);

        root.addView(textInfo);

        // Button
        Button actionBtn = new Button(this);
        if (item.getPrice() <= currentCoins) {
            actionBtn.setText("Upgrade");
            actionBtn.setBackgroundColor(0xFF717182); // Grayish as in screenshot
            actionBtn.setTextColor(Color.WHITE);
            actionBtn.setOnClickListener(v -> purchaseItem(item));
        } else {
            actionBtn.setText("Locked");
            actionBtn.setEnabled(false);
            actionBtn.setBackgroundColor(0xFFE5E7EB);
            actionBtn.setTextColor(0xFF9CA3AF);
        }

        root.addView(actionBtn);

        card.addView(root);
        return card;
    }

    private void purchaseItem(Equipment item) {
        if (currentCoins < item.getPrice()) {
            Toast.makeText(this, "Not enough coins!", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = auth.getUid();
        if (uid == null) return;

        db.runTransaction(transaction -> {
            transaction.update(db.collection("users").document(uid), "coin_balance", currentCoins - item.getPrice());

            Map<String, Object> invItem = new HashMap<>();
            invItem.put("type", item.getType());
            invItem.put("tier", item.getTier());
            invItem.put("name", item.getName());
            invItem.put("multiplier", item.getMultiplier());
            invItem.put("purchasedAt", System.currentTimeMillis());

            transaction.set(db.collection("users").document(uid).collection("inventory").document(item.getId()), invItem);

            return null;
        }).addOnSuccessListener(unused -> {
            Toast.makeText(this, "Purchased " + item.getName(), Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Purchase failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }
}
