package com.example.stepbuystep.model;

import java.util.Objects;

public class Equipment {
    private String id;
    private String name;
    private String type; // "walking_shoes", "running_shoes", "coach_token"
    private int tier; // 1 to 4
    private int price;
    private double multiplier;

    public Equipment() { }

    public Equipment(String id, String name, String type, int tier, int price, double multiplier) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.tier = tier;
        this.price = price;
        this.multiplier = multiplier;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public int getTier() { return tier; }
    public int getPrice() { return price; }
    public double getMultiplier() { return multiplier; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Equipment equipment = (Equipment) o;
        return tier == equipment.tier && Objects.equals(type, equipment.type);
    }
}
