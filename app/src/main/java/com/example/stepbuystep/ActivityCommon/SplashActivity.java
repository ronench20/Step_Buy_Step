package com.example.stepbuystep.ActivityCommon;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.stepbuystep.R;

/**
 * Entry point of the app (activity_splash).
 *
 * Flow:
 *   btnRegister -> MainActivity (first_screen)
 *   btnLogin    -> LoginActivity (login)
 *
 * Note: The previous implementation auto-routed to MainActivity after 2s,
 * which skipped the Register/Login choice built into activity_splash.xml.
 */
public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnLogin    = findViewById(R.id.btnLogin);

        btnRegister.setOnClickListener(v ->
                startActivity(new Intent(SplashActivity.this, MainActivity.class))
        );

        btnLogin.setOnClickListener(v ->
                startActivity(new Intent(SplashActivity.this, LoginActivity.class))
        );
    }
}
