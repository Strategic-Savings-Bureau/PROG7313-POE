package com.ssba.strategic_savings_budget_app

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.databinding.ActivityMainBinding
import com.ssba.strategic_savings_budget_app.landing.LoginActivity

class MainActivity : AppCompatActivity()
{
    // region Declarations
    // View Binding
    private lateinit var binding: ActivityMainBinding

    // Firebase Authentication
    private lateinit var auth: FirebaseAuth
    // endregion

    override fun onCreate(savedInstanceState: Bundle?)
    {
        // Initialisation
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // btnLogout On Click Listener
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}