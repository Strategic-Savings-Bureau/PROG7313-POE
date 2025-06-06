package com.ssba.strategic_savings_budget_app.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.Budget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI State holder - No changes here from your last version
data class BudgetUiState(
    val minimumMonthlyIncome: String = "",
    val maximumMonthlyExpenses: String = "",
    val minIncomeError: String = "",
    val maxExpensesError: String = "",
    val errorMessage: String = "",
    val hasExpensesCategories: Boolean = false,
)

class BudgetViewModel : ViewModel() {

    private lateinit var db: AppDatabase
    private var userId: String = ""

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    fun initialDbSet(database: AppDatabase) {
        db = database
    }

    fun fetchUserId(id: String) {
        userId = id
    }

    fun initialLoad() {
        if ( userId.isBlank()) {
            // Handle error or log: DB or UserID not set
            _uiState.update { it.copy(errorMessage = "Initialization error.") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val budget = db.budgetDao().getBudgetByUserId(userId)
            budget?.let {
                _uiState.update { currentState ->
                    currentState.copy(
                        minimumMonthlyIncome = budget.minimumMonthlyIncome.toString(),
                        maximumMonthlyExpenses = budget.maximumMonthlyExpenses.toString(),
                        minIncomeError = "", // Clear errors on load
                        maxExpensesError = "",
                        errorMessage = ""
                    )
                }
            }
        }
    }

    fun acummalateCategoryLimits() {

        viewModelScope.launch(Dispatchers.IO) {
            // Assuming getAllExpenseCategories() should be filtered by userId if applicable
            // For now, using it as is based on your provided code.
            val categoryCount = db.expenseCategoryDao().getExpenseCategoriesByUserId(userId).count()


            _uiState.update { currentState ->
                currentState.copy(
                    hasExpensesCategories = categoryCount > 0,
                    errorMessage = if (categoryCount == 0 && currentState.errorMessage.isBlank()) {
                        "No expense categories found to link for advanced settings."
                    } else {
                        currentState.errorMessage // Preserve other error messages
                    }
                )
            }
        }
    }

    // Called by TextWatchers for live UI updates and error messages
    fun updateMinIncome(value: String) {
        val isNumeric = value.toDoubleOrNull() != null
        val finalError = when {
            value.isBlank() -> "Minimum income cannot be empty"
            !isNumeric -> "Minimum income must be a valid number"
            value.toDouble() <= 0 -> "Minimum income must be greater than 0"
            else -> ""
        }
        _uiState.update { currentState ->
            currentState.copy(
                minimumMonthlyIncome = value,
                minIncomeError = finalError
            )
        }
    }

    // Called by TextWatchers for live UI updates and error messages
    fun updateMaxExpenses(value: String) {
        val isNumeric = value.toDoubleOrNull() != null
        val finalError = when {
            value.isBlank() -> "Maximum expenses cannot be empty"
            !isNumeric -> "Maximum expenses must be a valid number"
            value.toDouble() <= 0 -> "Maximum expenses must be greater than 0"
            else -> ""
        }
        _uiState.update {currentState->
            currentState.copy (
                maximumMonthlyExpenses = value,
                maxExpensesError = finalError
            )
        }
    }

    // Called on Save button click. Validates current inputs and updates UI state with errors.
    fun verifyInputsForSave(minIncomeStr: String, maxExpenseStr: String): Boolean {
        var currentMinIncomeError = ""
        var currentMaxExpenseError = ""
        var currentGeneralError = ""
        var isValid = true

        val minIncome = minIncomeStr.toDoubleOrNull()
        val maxExpense = maxExpenseStr.toDoubleOrNull()

        // Validate Minimum Income
        when {
            minIncomeStr.isBlank() -> {
                currentMinIncomeError = "Minimum income cannot be empty"
                isValid = false
            }
            minIncome == null -> {
                currentMinIncomeError = "Minimum income must be a valid number"
                isValid = false
            }
            minIncome <= 0 -> {
                currentMinIncomeError = "Minimum income must be greater than 0"
                isValid = false
            }
        }

        // Validate Maximum Expenses
        when {
            maxExpenseStr.isBlank() -> {
                currentMaxExpenseError = "Maximum expenses cannot be empty"
                isValid = false
            }
            maxExpense == null -> {
                currentMaxExpenseError = "Maximum expenses must be a valid number"
                isValid = false
            }
            maxExpense <= 0 -> {
                currentMaxExpenseError = "Maximum expenses must be greater than 0"
                isValid = false
            }
        }


        if (userId.isBlank()){
            currentGeneralError = "User information not found. Cannot save."
            isValid = false
        }
        if (!::db.isInitialized){
            currentGeneralError = "Database error. Cannot save."
            isValid = false
        }


        // Update UI state with validation results
        _uiState.update { currentState ->
            currentState.copy(
                minIncomeError = currentMinIncomeError,
                maxExpensesError = currentMaxExpenseError,
                // Only set general error if validation failed and other errors are specific.
                // If specific field errors exist, they are more informative.
                errorMessage = if (!isValid && currentGeneralError.isNotEmpty()) {
                    currentGeneralError
                } else if (!isValid) {
                    "Please fix the errors indicated above."
                }
                else {
                    "" // Clear general error if everything is valid
                }
            )
        }
        return isValid
    }

    // Called after verifyInputsForSave returns true
    fun onSaveToBudgetDb(validMinIncome: Double, validMaxExpense: Double) {
        if (userId.isBlank() || !::db.isInitialized) {
            _uiState.update { it.copy(errorMessage = "Cannot save: User or DB not initialized.") }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            var budgetToSave = db.budgetDao().getBudgetByUserId(userId)

            if (budgetToSave != null) {
                // Update existing budget
                budgetToSave = budgetToSave.copy(
                    minimumMonthlyIncome = validMinIncome,
                    maximumMonthlyExpenses = validMaxExpense
                )
            } else {
                // Create new budget (budgetId will be auto-generated by Room)
                budgetToSave = Budget(
                    userId = userId,
                    minimumMonthlyIncome = validMinIncome,
                    maximumMonthlyExpenses = validMaxExpense
                )
            }
            db.budgetDao().upsertBudget(budgetToSave)

            // Update UI state to reflect the saved values and clear any previous errors
            _uiState.update { currentState ->
                currentState.copy(
                    // Update the string representations in the state
                    minimumMonthlyIncome = validMinIncome.toString(),
                    maximumMonthlyExpenses = validMaxExpense.toString(),
                    minIncomeError = "",
                    maxExpensesError = "",
                    errorMessage = "" // Clear any previous error messages
                )
            }
        }
    }
}