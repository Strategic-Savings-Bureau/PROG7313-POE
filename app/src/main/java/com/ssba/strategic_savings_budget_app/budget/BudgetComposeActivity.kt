package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ModifierLocalBeyondBoundsLayout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.budget.ui.theme.StrategicsavingsbudgetappTheme
import com.ssba.strategic_savings_budget_app.composeState.BudgetUiState
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.models.BudgetSettingsViewModel

class BudgetComposeActivity : ComponentActivity() {
    val viewmodel: BudgetSettingsViewModel by viewModels()
    val db: AppDatabase = AppDatabase.getInstance(this)
    val auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        viewmodel.initialDbSet(db)
        val userId = auth.currentUser!!.uid
        viewmodel.fetchUserId(userId)

        viewmodel.initialLoad()
        viewmodel.accumalateCategoryLimits()
        enableEdgeToEdge()
        setContent {
            StrategicsavingsbudgetappTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BudgetSettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        onAdvancedSettingsClick = {

                                val intent =
                                    Intent(this, AdvancedBudgetCompose::class.java)
                                startActivity(intent)



                        },
                        onCancelClick = { finish() },
                        onSaveClick = {

                                Toast.makeText(
                                    this,
                                    "Successfully saved budget",
                                    Toast.LENGTH_SHORT
                                ).show()
                                val intent = Intent(
                                    this,
                                    MainActivity::class.java
                                )
                                startActivity(intent)
                                finish()

                        },
                        viewModel = viewmodel
                    )
                }
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StrategicsavingsbudgetappTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
Box(Modifier.fillMaxSize(),contentAlignment = Alignment.Center){

}

        }


    }
}

@Composable
fun BudgetSettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: BudgetSettingsViewModel,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onAdvancedSettingsClick: () -> Unit,
) {

    val uiState: BudgetUiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth(),

                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title
                    Text(
                        text = "Budget Settings",
                        fontSize = 24.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Minimum Monthly Income
                    Text(
                        text = "Minimum Monthly Income",
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    OutlinedTextField(
                        value = uiState.minimumMonthlyIncome,     // MAKE SURE TO ADD LOGIC
                        onValueChange = { updatedIncome ->
                            viewModel.updateMinIncome(updatedIncome)
                        },
                        isError = !uiState.minIncomeError.isEmpty(),

                        supportingText = {
                            if (uiState.minIncomeError.isEmpty()) {
                                Text(text = "Minimum Monthly Income")

                            } else {
                                Text(text = uiState.minIncomeError)
                            }
                        },
                        placeholder = { Text("R0.00") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Maximum Monthly Expenses
                    Text(
                        text = "Maximum Monthly Expenses",
                        fontSize = 16.sp,
                        color = Color.Black,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                    OutlinedTextField(
                        value = uiState.maximumMonthlyExpenses,
                        onValueChange = { updatedExpense ->
                            viewModel.updateMaxExpenses(updatedExpense)
                        },
                        isError = !uiState.maxExpensesError.isEmpty(), // if we dont have text, then its good

                        supportingText = {
                            if (uiState.maxExpensesError.isEmpty()) {
                                Text(text = "Maximum Monthly Expense")

                            } else {
                                Text(text = uiState.maxExpensesError)
                            }
                        },
                        placeholder = { Text("R0.00") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )

                    // Advanced Settings Button
                    Button(
                        onClick = { onAdvancedSettingsClick() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp)
                    ) {
                        Text("Advanced Settings", color = Color.Black)
                    }

                    // Action Buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 200.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Button(
                            onClick = {
                                val validate = viewModel.verifyNavigation()
                                if (validate) {
                                    viewModel.onSaveToBudgetDb()
                                    onSaveClick()
                                } else {
                                    Toast.makeText(
                                        context,
                                        uiState.errorMessage,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CD7A9)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.Black)
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Button(
                            onClick = onCancelClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


