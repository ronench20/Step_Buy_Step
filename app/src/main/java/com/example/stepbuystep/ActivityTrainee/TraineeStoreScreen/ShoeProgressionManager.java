package com.example.stepbuystep.ActivityTrainee.TraineeStoreScreen;

import com.example.stepbuystep.model.ShoeLevel;
import java.util.ArrayList;
import java.util.List;

public class ShoeProgressionManager {

    private static final int MAX_LEVEL = 5;

    // Define the 5 shoe levels with good progression
    // Multiplier:  1.0x, 1.5x, 2.5x, 4.0x, 6.0x
    // Price: Free, 1000, 2500, 5000, 10000
    private static final ShoeLevel[] SHOE_DEFINITIONS = {
            new ShoeLevel(1, "Basic Runner", 0, 1.0),
            new ShoeLevel(2, "Speed Jogger", 1000, 1.5),
            new ShoeLevel(3, "Pro Sprinter", 2500, 2.5),
            new ShoeLevel(4, "Elite Racer", 5000, 4.0),
            new ShoeLevel(5, "Champion Dash", 10000, 6.0)
    };

    /**
     * Get all 5 shoe levels with their states calculated based on current level
     * @param currentLevel The user's current shoe level (1-5)
     * @return List of all 5 shoes with states assigned
     */
    public static List<ShoeLevel> getAllShoesWithStates(int currentLevel) {
        List<ShoeLevel> shoes = new ArrayList<>();

        for (int i = 0; i < SHOE_DEFINITIONS.length; i++) {
            ShoeLevel shoe = new ShoeLevel(
                    SHOE_DEFINITIONS[i].getLevel(),
                    SHOE_DEFINITIONS[i].getName(),
                    SHOE_DEFINITIONS[i].getPrice(),
                    SHOE_DEFINITIONS[i].getMultiplier()
            );

            int level = shoe.getLevel();

            if (level < currentLevel) {
                shoe.setState(ShoeLevel.ShoeState.OWNED);
            } else if (level == currentLevel) {
                shoe. setState(ShoeLevel.ShoeState.CURRENT);
            } else if (level == currentLevel + 1) {
                shoe. setState(ShoeLevel.ShoeState.NEXT);
            } else {
                shoe.setState(ShoeLevel.ShoeState.LOCKED);
            }

            shoes.add(shoe);
        }

        return shoes;
    }

    /**
     * Get the next purchasable shoe or null if maxed out
     */
    public static ShoeLevel getNextShoe(int currentLevel) {
        if (currentLevel >= MAX_LEVEL) {
            return null;
        }

        ShoeLevel nextShoe = SHOE_DEFINITIONS[currentLevel]; // Array is 0-indexed
        ShoeLevel shoe = new ShoeLevel(
                nextShoe.getLevel(),
                nextShoe.getName(),
                nextShoe.getPrice(),
                nextShoe.getMultiplier()
        );
        shoe.setState(ShoeLevel.ShoeState.NEXT);
        return shoe;
    }

    /**
     * Check if user can purchase the next level
     */
    public static boolean canPurchaseNextLevel(int currentLevel, long currentCoins) {
        if (currentLevel >= MAX_LEVEL) {
            return false;
        }

        ShoeLevel nextShoe = getNextShoe(currentLevel);
        return nextShoe != null && currentCoins >= nextShoe.getPrice();
    }

    /**
     * Calculate progress percentage toward next purchase
     */
    public static int calculateProgress(int currentLevel, long currentCoins) {
        ShoeLevel nextShoe = getNextShoe(currentLevel);
        if (nextShoe == null || nextShoe.getPrice() == 0) {
            return 100;
        }

        return Math.min(100, (int) ((currentCoins * 100) / nextShoe.getPrice()));
    }
}