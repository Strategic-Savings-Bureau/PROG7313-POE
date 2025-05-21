package com.ssba.strategic_savings_budget_app.budget

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ssba.strategic_savings_budget_app.budget.ui.theme.StrategicsavingsbudgetappTheme

class AdvancedBudgetCompose : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StrategicsavingsbudgetappTheme {
                Scaffold( modifier = Modifier.fillMaxSize() ) { innerPadding ->
             AdvancedBudgetScreen(modifier = Modifier.padding(innerPadding),name =  "Advanced Budget")
                }
            }
        }
    }
}

@Composable
fun AdvancedBudgetScreen(name: String, modifier: Modifier = Modifier) {

}


