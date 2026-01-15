package com.example.stepbuystep.ActivityCoach;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.stepbuystep.ActivityCoach.CoachHomeScreen.CoachHomeActivity;
import com.example.stepbuystep.ActivityCoach.CoachSettingsScreen.CoachSettingsActivity;
import com.example.stepbuystep.ActivityCoach.CoachCreateScreen.CreateWorkoutActivity;
import com.example.stepbuystep.ActivityCoach.CoachHistoryScreen.HistoryCoachActivity;
import com.example.stepbuystep.R;

public abstract class BaseCoachActivity extends AppCompatActivity{

    protected LinearLayout navDashboardCoach, navMyHistory, navCreate, navSettings;

    // Define which navigation item should be highlighted
    protected enum NavItem {
        DASH_COACH,
        MY_HISTORY,
        CREATE,
        SETTINGS
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    protected void setupNavigationBar(NavItem currentPage) {
        navDashboardCoach = findViewById(R.id.navDashboardCoach);
        navMyHistory = findViewById(R.id. navMyHistory);
        navCreate = findViewById(R.id.navCreate);
        navSettings = findViewById(R.id.navSettings);

        if (navDashboardCoach != null) {
            navDashboardCoach.setOnClickListener(v -> navigateToPage(NavItem.DASH_COACH));
        }
        if (navMyHistory != null) {
            navMyHistory.setOnClickListener(v -> navigateToPage(NavItem.MY_HISTORY));
        }
        if (navCreate != null) {
            navCreate.setOnClickListener(v -> navigateToPage(NavItem.CREATE));
        }
        if (navSettings != null) {
            navSettings.setOnClickListener(v -> navigateToPage(NavItem. SETTINGS));
        }

        // Highlight the current page
        highlightNavigationItem(currentPage);
    }

    private void navigateToPage(NavItem navItem){
        Intent intent = null;

        switch (navItem) {
            case DASH_COACH:
                intent = new Intent(this, CoachHomeActivity.class);
                break;
            case MY_HISTORY:
                intent = new Intent(this, HistoryCoachActivity.class);
                break;
            case CREATE:
                intent = new Intent(this, CreateWorkoutActivity.class);
                break;
            case SETTINGS:
                intent = new Intent(this, CoachSettingsActivity. class);
                break;
        }

        if (intent != null && !intent.getComponent().getClassName().equals(this.getClass().getName())) {
            startActivity(intent);
            finish(); // Optional: close current activity
        }
    }

    private void highlightNavigationItem(NavItem currentPage) {
        // Reset all nav items to default state
        if (navDashboardCoach != null) resetNavItem(navDashboardCoach);
        if (navMyHistory != null) resetNavItem(navMyHistory);
        if (navCreate != null) resetNavItem(navCreate);
        if (navSettings != null) resetNavItem(navSettings);

        // Highlight the current page
        LinearLayout activeNav = null;
        switch (currentPage) {
            case DASH_COACH:
                activeNav = navDashboardCoach;
                break;
            case MY_HISTORY:
                activeNav = navMyHistory;
                break;
            case CREATE:
                activeNav = navCreate;
                break;
            case SETTINGS:
                activeNav = navSettings;
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
                backgroundContainer.setBackgroundResource(0); // Remove background
                for (int j = 0; j < backgroundContainer.getChildCount(); j++) {
                    if (backgroundContainer.getChildAt(j) instanceof ImageView) {
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
            label. setTypeface(null, android. graphics. Typeface.NORMAL);
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
                backgroundContainer.setBackgroundResource(R.drawable.ic_launcher_background);
                backgroundContainer. setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.blue_50));
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
            label. setTextColor(ContextCompat.getColor(this, R.color.brand_blue));
            label.setTypeface(null, android.graphics.Typeface. BOLD);
        }
    }
}