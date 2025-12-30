package com.example.stepbuystep

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.first_screen)

        val btnUserRegister = findViewById<Button>(R.id.btnUserRegister)
        val btnTrainerRegister = findViewById<Button>(R.id.btnTrainerRegister)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnUserRegister.setOnClickListener {
            //  User Register
            startActivity(Intent(this, RegisterActivity::class.java))
        }
        btnTrainerRegister.setOnClickListener {
            //  Trainer Register
            startActivity(Intent(this, CoachRegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            //  Login
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
