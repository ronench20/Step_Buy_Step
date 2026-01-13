package com.example.stepbuystep.ActivityTrainee;

import android.content. Intent;
import android.os. Bundle;
import android.view. View;
import android.widget. ImageView;
import android.widget.LinearLayout;
import android. widget.TextView;

import androidx. appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.stepbuystep.ActivityTrainee.TraineeHistoryScreen.HistoryTraineeActivity;
import com.example.stepbuystep.ActivityCommon.LeaderBoardActivity;
import com.example.stepbuystep.R;
import com.example.stepbuystep.ActivityTrainee.TraineeStoreScreen.ShopActivity;
import com.example.stepbuystep.ActivityTrainee.TraineeHomeScreen.TraineeHomeActivity;

public abstract class BaseTraineeActivity extends AppCompatActivity {

    protected LinearLayout navDashboard, navHistory, navShoeStore, navLeaderboard;

    // Define which navigation item should be highlighted
    protected enum NavItem {
        DASHBOARD,
        HISTORY,
        SHOE_STORE,
        LEADERBOARD
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * Call this method after setContentView() to initialize the navigation bar
     */
    protected void setupNavigationBar(NavItem currentPage) {
        navDashboard = findViewById(R.id.navDashboard);
        navHistory = findViewById(R.id. navHistory);
        navShoeStore = findViewById(R.id. navShoeStore);
        navLeaderboard = findViewById(R. id.navLeaderboard);

        if (navDashboard != null) {
            navDashboard.setOnClickListener(v -> navigateToPage(NavItem.DASHBOARD));
        }
        if (navHistory != null) {
            navHistory.setOnClickListener(v -> navigateToPage(NavItem.HISTORY));
        }
        if (navShoeStore != null) {
            navShoeStore.setOnClickListener(v -> navigateToPage(NavItem.SHOE_STORE));
        }
        if (navLeaderboard != null) {
            navLeaderboard.setOnClickListener(v -> navigateToPage(NavItem. LEADERBOARD));
        }

        // Highlight the current page
        highlightNavigationItem(currentPage);
    }

    private void navigateToPage(NavItem navItem) {
        Intent intent = null;

        switch (navItem) {
            case DASHBOARD:
                intent = new Intent(this, TraineeHomeActivity.class);
                break;
            case HISTORY:
                intent = new Intent(this, HistoryTraineeActivity.class);
                break;
            case SHOE_STORE:
                intent = new Intent(this, ShopActivity.class);
                break;
            case LEADERBOARD:
                intent = new Intent(this, LeaderBoardActivity.class);
                break;
        }

        if (intent != null && !intent.getComponent().getClassName().equals(this.getClass().getName())) {
            startActivity(intent);
            finish(); // Close current activity
        }
    }

    private void highlightNavigationItem(NavItem currentPage) {
        // Reset all nav items to default state
        if (navDashboard != null) resetNavItem(navDashboard);
        if (navHistory != null) resetNavItem(navHistory);
        if (navShoeStore != null) resetNavItem(navShoeStore);
        if (navLeaderboard != null) resetNavItem(navLeaderboard);

        // Highlight the current page
        LinearLayout activeNav = null;
        switch (currentPage) {
            case DASHBOARD:
                activeNav = navDashboard;
                break;
            case HISTORY:
                activeNav = navHistory;
                break;
            case SHOE_STORE:
                activeNav = navShoeStore;
                break;
            case LEADERBOARD:
                activeNav = navLeaderboard;
                break;
        }

        if (activeNav != null) {
            setNavItemActive(activeNav);
        }
    }

    private void resetNavItem(LinearLayout navItem) {
        ImageView icon = null;
        TextView label = null;
        LinearLayout backgroundContainer = null;

        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                icon = (ImageView) child;
            } else if (child instanceof TextView) {
                label = (TextView) child;
            } else if (child instanceof LinearLayout) {
                // For nested LinearLayout (background container)
                backgroundContainer = (LinearLayout) child;
                backgroundContainer. setBackgroundResource(0); // Remove background
                for (int j = 0; j < backgroundContainer.getChildCount(); j++) {
                    if (backgroundContainer. getChildAt(j) instanceof ImageView) {
                        icon = (ImageView) backgroundContainer. getChildAt(j);
                    }
                }
            }
        }

        if (icon != null) {
            icon. setColorFilter(ContextCompat.getColor(this, R. color.text_secondary));
        }
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(this, R.color. text_secondary));
            label. setTypeface(null, android. graphics.Typeface.NORMAL);
        }
    }

    private void setNavItemActive(LinearLayout navItem) {
        ImageView icon = null;
        TextView label = null;
        LinearLayout backgroundContainer = null;

        for (int i = 0; i < navItem.getChildCount(); i++) {
            View child = navItem.getChildAt(i);
            if (child instanceof ImageView) {
                icon = (ImageView) child;
            } else if (child instanceof TextView) {
                label = (TextView) child;
            } else if (child instanceof LinearLayout) {
                // For nested LinearLayout (background container)
                backgroundContainer = (LinearLayout) child;
                // Set the blue background
                backgroundContainer. setBackgroundResource(R.drawable.ic_launcher_background);
                backgroundContainer.setBackgroundTintList(ContextCompat. getColorStateList(this, R.color.blue_50));
                for (int j = 0; j < backgroundContainer.getChildCount(); j++) {
                    if (backgroundContainer.getChildAt(j) instanceof ImageView) {
                        icon = (ImageView) backgroundContainer.getChildAt(j);
                    }
                }
            }
        }

        if (icon != null) {
            icon.setColorFilter(ContextCompat.getColor(this, R.color.brand_blue));
        }
        if (label != null) {
            label.setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
            label.setTypeface(null, android.graphics.Typeface.BOLD);
        }
    }
}