package com.example.stepbuystep.ActivityCommon

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.stepbuystep.ActivityCoach.CoachRegisterActivity
import com.example.stepbuystep.R

/**
 * "First screen" — registration role chooser (first_screen.xml).
 *
 * Flow:
 *   btnUserRegister    -> RegisterActivity      (then -> EnterInfoActivity on success)
 *   btnTrainerRegister -> CoachRegisterActivity
 *
 * Login flow is handled from SplashActivity (activity_splash) directly.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.first_screen)

        val btnUserRegister    = findViewById<Button>(R.id.btnUserRegister)
        val btnTrainerRegister = findViewById<Button>(R.id.btnTrainerRegister)

        btnUserRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnTrainerRegister.setOnClickListener {
            startActivity(Intent(this, CoachRegisterActivity::class.java))
        }
    }
}
