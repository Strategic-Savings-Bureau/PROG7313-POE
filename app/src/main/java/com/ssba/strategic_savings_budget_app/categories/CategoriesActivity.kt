package com.ssba.strategic_savings_budget_app.categories

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ssba.strategic_savings_budget_app.databinding.ActivityCategoriesBinding

class CategoriesActivity : AppCompatActivity() {

    // view binding
    private lateinit var binding: ActivityCategoriesBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // initialize view binding
        binding = ActivityCategoriesBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}