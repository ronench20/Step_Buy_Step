package com.example.stepbuystep;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.ComponentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.stepbuystep.adapter.ShopAdapter;
import com.example.stepbuystep.model.Equipment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShopActivity extends BaseTraineeActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextView tvCoinBalance;
    private TextView tvCurrentShoeName, tvCurrentShoeLevel, tvCurrentMultiplier;
    private RecyclerView rvShoes;
    private View btnBack;
    private long currentCoins = 0;
    private Map<String, Integer> currentInventoryTiers = new HashMap<>();
    private List<Equipment> availableUpgrades = new ArrayList<>();
    private ShopAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupListeners();
        setupNavigationBar(NavItem.SHOE_STORE);
        loadUserData();
    }

    private void initViews() {
        tvCoinBalance = findViewById(R.id.tvCoinBalance);
        tvCurrentShoeName = findViewById(R.id.tvCurrentShoeName);
        tvCurrentShoeLevel = findViewById(R.id.tvCurrentShoeLevel);
        tvCurrentMultiplier = findViewById(R.id.tvCurrentMultiplier);
        rvShoes = findViewById(R.id.rvShoes);
        btnBack = findViewById(R.id.btnBack);

    }

    private void setupRecyclerView() {
        adapter = new ShopAdapter();
        adapter.setListener(this::purchaseItem);
        rvShoes.setLayoutManager(new LinearLayoutManager(this));
        rvShoes.setAdapter(adapter);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
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

        // Check availability logic
        // We show next available upgrade for each type
        addNextUpgrade("walking_shoes", "Basic Runner", currentInventoryTiers.get("walking_shoes"));
        addNextUpgrade("running_shoes", "Sport Jogger", currentInventoryTiers.get("running_shoes"));

        adapter.setItems(availableUpgrades, currentCoins);
    }

    private void addNextUpgrade(String type, String baseName, Integer currentTier) {
        if (currentTier == null) currentTier = 0;
        if (currentTier >= 5) return; // Cap at level 5

        int nextTier = currentTier + 1;
        int price = nextTier * 500;
        if (type.equals("walking_shoes") && nextTier == 1) price = 0; // First walking shoe free

        double multiplier = 1.0;
        if (type.equals("walking_shoes")) multiplier = 1.0 + (nextTier * 0.2);
        else if (type.equals("running_shoes")) multiplier = 1.0 + (nextTier * 0.5);

        // Round multiplier
        multiplier = Math.round(multiplier * 10.0) / 10.0;

        Equipment item = new Equipment(type + "_" + nextTier, baseName, type, nextTier, price, multiplier);
        availableUpgrades.add(item);
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
