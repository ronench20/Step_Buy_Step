package com.example.stepbuystep.model;

public class SubscriptionTier {
    public static final String TIER_BASIC = "basic";
    public static final String TIER_PRO = "pro";
    public static final String TIER_ELITE = "elite";

    private String name;
    private int maxAthletes;
    private double price;
    private String tier;

    public SubscriptionTier() {}

    public SubscriptionTier(String tier, String name, int maxAthletes, double price) {
        this.tier = tier;
        this.name = name;
        this.maxAthletes = maxAthletes;
        this.price = price;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getMaxAthletes() {
        return maxAthletes;
    }

    public void setMaxAthletes(int maxAthletes) {
        this.maxAthletes = maxAthletes;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public static SubscriptionTier getTierByName(String tierName) {
        switch (tierName.toLowerCase()) {
            case TIER_BASIC:
                return new SubscriptionTier(TIER_BASIC, "Basic", 20, 0.0);
            case TIER_PRO:
                return new SubscriptionTier(TIER_PRO, "Pro", 35, 10.0);
            case TIER_ELITE:
                return new SubscriptionTier(TIER_ELITE, "Elite", 50, 20.0);
            default:
                return new SubscriptionTier(TIER_BASIC, "Basic", 20, 0.0);
        }
    }
}