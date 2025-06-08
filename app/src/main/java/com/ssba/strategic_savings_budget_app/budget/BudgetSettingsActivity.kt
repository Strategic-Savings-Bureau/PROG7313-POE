package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.MainActivity
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.SettingsActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.models.BudgetViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BudgetSettingsActivity : AppCompatActivity() {

    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var auth: FirebaseAuth
    private lateinit var db: AppDatabase

    private lateinit var minIncomeInput: EditText
    private lateinit var maxExpenseInput: EditText
    private lateinit var advancedButton: Button
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button
    private lateinit var minIncomeErrorText: TextView
    private lateinit var maxExpensesErrorText: TextView
    // Flags to prevent overwriting while user is typing (if EditTexts are directly observed by state)
    // This is important for the observer logic that sets EditText text.
    private var isUserEditingMinIncome = false
    private var isUserEditingMaxExpense = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budget_settings)

        db = AppDatabase.getInstance(this)
        auth = FirebaseAuth.getInstance()

        minIncomeInput = findViewById(R.id.minIncomeInput)
        maxExpenseInput = findViewById(R.id.maxExpensesInput)
        minIncomeErrorText = findViewById(R.id.minIncomeErrorText)
        maxExpensesErrorText = findViewById(R.id.maxExpensesErrorText)
        advancedButton = findViewById(R.id.advancedSettingsBtn)
        saveButton = findViewById(R.id.saveBtn)
        cancelButton = findViewById(R.id.cancelBtn)

        viewModel.initialDbSet(db)
        val currentUser = auth.currentUser
        if (currentUser == null) {
            // Handle user not logged in (e.g., redirect to login)
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_LONG).show()
            finish() // Or navigate to login
            return
        }
        currentUser.uid.let { viewModel.fetchUserId(it) }

        viewModel.initialLoad() // Load existing budget if any
        viewModel.acummalateCategoryLimits() // Check for categories

        setupInputListeners()
        observeUiState()

        advancedButton.setOnClickListener {
            val uiState = viewModel.uiState.value // Get current state
            if (!uiState.hasExpensesCategories) {
                val errorMessage = uiState.errorMessage.ifEmpty { "No expense categories found to setup advanced budget." }
                Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            } else {
                // Assuming AdvancedBudgetCompose is an Activity. If it's a Composable screen, navigation differs.
                startActivity(Intent(this, AdvancedBudgetSettingsActivity::class.java))
            }
        }

        saveButton.setOnClickListener {
            // Reset typing flags so observer can update EditTexts if needed after save
            isUserEditingMinIncome = false
            isUserEditingMaxExpense = false

            val currentMinIncome = minIncomeInput.text.toString()
            val currentMaxExpense = maxExpenseInput.text.toString()

            if (viewModel.verifyInputsForSave(currentMinIncome, currentMaxExpense)) {
                // Inputs are valid, proceed to convert and save
                // toDouble() is safe here because verifyInputsForSave would have returned false if they weren't valid doubles
                val minIncomeDouble = currentMinIncome.toDouble()
                val maxExpenseDouble = currentMaxExpense.toDouble()

                viewModel.onSaveToBudgetDb(minIncomeDouble, maxExpenseDouble)
                Toast.makeText(this, "Budget saved successfully!", Toast.LENGTH_SHORT).show()

                // Navigate back or to the main screen
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                })
                finish()
            } else {
                // viewModel.verifyInputsForSave has updated the uiState with error messages.
                // The observer will update the TextViews.
                // Show a general Toast if there's a general error message from the ViewModel.
                val generalErrorMessage = viewModel.uiState.value.errorMessage
                if (generalErrorMessage.isNotEmpty() && generalErrorMessage != "Please fix the errors indicated above.") {
                    Toast.makeText(this, generalErrorMessage, Toast.LENGTH_LONG).show()
                } else {
                    // Fallback if specific field errors are set but no general message,
                    // or if the general message is the generic "fix errors".
                    Toast.makeText(this, "Please correct the highlighted errors.", Toast.LENGTH_LONG).show()
                }
            }
        }

        cancelButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setupInputListeners() {
        minIncomeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                isUserEditingMinIncome = true
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUserEditingMinIncome) { // Check flag to ensure user is editing
                    viewModel.updateMinIncome(s.toString())
                }
            }
        })

        maxExpenseInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                isUserEditingMaxExpense = true
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUserEditingMaxExpense) { // Check flag
                    viewModel.updateMaxExpenses(s.toString())
                }
            }
        })
    }
    private fun observeUiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    // Update EditText only if the user is not currently editing it AND the text differs
                    if (!isUserEditingMinIncome && minIncomeInput.text.toString() != state.minimumMonthlyIncome) {
                        minIncomeInput.setText(state.minimumMonthlyIncome)
                        // Optionally move cursor to the end: minIncomeInput.setSelection(state.minimumMonthlyIncome.length)
                    }
                    if (!isUserEditingMaxExpense && maxExpenseInput.text.toString() != state.maximumMonthlyExpenses) {
                        maxExpenseInput.setText(state.maximumMonthlyExpenses)
                        // Optionally move cursor to the end: maxExpenseInput.setSelection(state.maximumMonthlyExpenses.length)
                    }

                    minIncomeErrorText.text = state.minIncomeError
                    minIncomeErrorText.visibility = if (state.minIncomeError.isNotEmpty()) View.VISIBLE else View.GONE

                    maxExpensesErrorText.text = state.maxExpensesError
                    maxExpensesErrorText.visibility = if (state.maxExpensesError.isNotEmpty()) View.VISIBLE else View.GONE

                    // Handle general error message display (e.g., if you have a dedicated TextView for it)
                    // Or rely on Toasts as currently implemented in the save button.
                    // If you have a general error TextView:
                    // val generalErrorTextView = findViewById<TextView>(R.id.generalErrorLabel)
                    // generalErrorTextView.text = state.errorMessage
                    // generalErrorTextView.visibility = if (state.errorMessage.isNotEmpty()) View.VISIBLE else View.GONE
                }
            }
        }
    }
}