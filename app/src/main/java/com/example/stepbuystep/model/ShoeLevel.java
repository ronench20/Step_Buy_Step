package com.example.stepbuystep. model;

public class ShoeLevel {
    private int level;
    private String name;
    private int price;
    private double multiplier;
    private ShoeState state;

    public enum ShoeState {
        OWNED,
        CURRENT,
        NEXT,
        LOCKED
    }

    public ShoeLevel(int level, String name, int price, double multiplier) {
        this.level = level;
        this.name = name;
        this.price = price;
        this.multiplier = multiplier;
        this.state = ShoeState.LOCKED;
    }

    // Getters and Setters
    public int getLevel() { return level; }
    public String getName() { return name; }
    public int getPrice() { return price; }
    public double getMultiplier() { return multiplier; }
    public ShoeState getState() { return state; }
    public void setState(ShoeState state) { this.state = state; }

    @Override
    public String toString() {
        return "ShoeLevel{" +
                "level=" + level +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", multiplier=" + multiplier +
                ", state=" + state +
                '}';
    }
}