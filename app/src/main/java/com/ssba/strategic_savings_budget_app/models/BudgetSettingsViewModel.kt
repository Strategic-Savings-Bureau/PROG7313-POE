package com.ssba.strategic_savings_budget_app.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssba.strategic_savings_budget_app.composeState.BudgetUiState
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.Budget
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BudgetSettingsViewModel() : ViewModel() {
    private var db: AppDatabase? = null
    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()
    private var userId: String = ""


    // this will be intially called before the content is called
    fun initialDbSet(db: AppDatabase) {
        this.db = db
    }

    fun initialLoad() {
        viewModelScope.launch {
            if (userId.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Unauthorised user") }         // CHECK USER ID THEN ADD THE ERROR TO UISTATE
            }
            val budget: Budget? = db!!.budgetDao.getBudgetByUserId(userId!!)
            if (budget == null) {
                return@launch   // we just keep default state
            }
            val maxMonthlyExpenses: String = budget.maximumMonthlyExpenses.toString()
            val minMonthlyIncome: String = budget.minimumMonthlyIncome.toString()

            _uiState.update {
                it.copy(
                    maximumMonthlyExpenses = maxMonthlyExpenses,
                    minimumMonthlyIncome = minMonthlyIncome
                )
            }

        }
    }

    fun fetchUserId(userId: String) {
        if (userId.isEmpty()) {
            return
        }
        this.userId = userId
    }

    fun accumalateCategoryLimits() {

        viewModelScope.launch {
            var categories: List<ExpenseCategory> =
                db?.expenseCategoryDao!!.getAllExpenseCategories()
            if (categories.count() == 0) {
                _uiState.update { it.copy(currExpenseTotalError = "There are no categories to view.") }
                return@launch
            }
            val currExpenseAllocation: Double = categories.sumOf { it.maximumMonthlyTotal }
            _uiState.update { it.copy(currExpenseTotal = currExpenseAllocation.toString()) } // make dure to use formateter
        }
    }

    fun updateMinIncome(minMonthlyIncome: String) {

        _uiState.update { it.copy(minimumMonthlyIncome = minMonthlyIncome) }
        if (!validateInput(minMonthlyIncome)) {
            _uiState.update {
                it.copy(

                    minIncomeError = "Enter a valid amount eg: 100.00"
                )
            }
        } else {

            _uiState.update {
                it.copy(

                    minIncomeError = "",
                )
            }
        }

    }

    fun updateMaxExpenses(maxMonthlyExpenses: String) {
        _uiState.update { it.copy(maximumMonthlyExpenses = maxMonthlyExpenses) }
        if (!validateInput(maxMonthlyExpenses)) {
            _uiState.update {
                it.copy(

                    maxExpensesError = "Enter a valid amount eg: 100.00"
                )
            }
        } else {
            _uiState.update {
                it.copy(

                    maxExpensesError = "",
                )
            }
        }

    }

    fun validateInput(input: String): Boolean {
        val value = input.toDoubleOrNull()
        return value != null // will return true if this statement is valid
    }

    fun validateAll(): Boolean {
        resetState()
        var valid = true
        var min = 0.00
        var curr = 0.00
        var max = 0.00
        try {
            min = _uiState.value.minimumMonthlyIncome.toDouble()

        } catch (e: Exception) {
            _uiState.update { it.copy(minIncomeError = "Enter a valid amount eg: 100.00") }
            valid = false
        }
        try {
            curr = _uiState.value.currExpenseTotal.toDouble()
        } catch (e: Exception) {
            _uiState.update { it.copy(currExpenseTotalError = "Enter a valid amount eg: 100.00") }
            // add logging for the exceptions
            valid = false
        }
        try {
            max = _uiState.value.maximumMonthlyExpenses.toDouble()
        } catch (e: Exception) {
            _uiState.update { it.copy(maxExpensesError = "Enter a valid amount eg: 100.00") }
            valid = false
        }

        if (db == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Internal error!"
                )
            }  // this is checking if we fetched the db
            valid = false
        } else if (userId.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Unauthorised user") }
            valid = false
        } else if (curr > max) {
            _uiState.update {
                it.copy(
                    currExpenseTotalError = "Expense total ($curr) exceeds maximum allowed ($max)",

                    )
            }
            valid = false

        } else if (min <= 0.00) {
            _uiState.update {
                it.copy(
                    minimumMonthlyIncome = "",
                    minIncomeError = "Enter an amount greater than 0"
                )

            }
            valid = false
        } else if (max <= 0.00) {
            _uiState.update {
                it.copy(
                    maximumMonthlyExpenses = "",
                    maxExpensesError = "Enter an amount greater than 0"
                )
            }
            valid = false
        }

        return valid
    }

    // Helper method for saving a budget
    suspend fun fetchBudgetId(userId: String): Int? {

        if (userId.isEmpty()) {
            return null
        }
        val budget: Budget? = db!!.budgetDao.getBudgetByUserId(userId)
        if (budget == null) {
            return null
        }
        return budget.budgetId

    }

    fun onSaveToBudgetDb() {
        viewModelScope.launch {
            val budgetId: Int? = fetchBudgetId(userId)
            var minMonthlyIncome: Double = _uiState.value.minimumMonthlyIncome.toDouble()
            var maxMonthlyExpenses: Double = _uiState.value.maximumMonthlyExpenses.toDouble()
            var currBudget: Budget?
            if (budgetId == null) {
                currBudget = Budget(
                    minimumMonthlyIncome = minMonthlyIncome,
                    maximumMonthlyExpenses = maxMonthlyExpenses,
                    userId = userId,
                )
                try {
                    db?.budgetDao?.upsertBudget(currBudget)
                    return@launch
                }catch (e: Exception){
                    _uiState.update {it.copy(errorMessage = "Unable to update database") }
                }
            }
            currBudget = db?.budgetDao!!.getBudgetById(budgetId!!)
            currBudget = currBudget!!.copy(
                minimumMonthlyIncome = minMonthlyIncome,
                maximumMonthlyExpenses = maxMonthlyExpenses,
                userId = userId,
                budgetId = budgetId

            )
            try {
                db?.budgetDao?.upsertBudget(currBudget)
                return@launch
            }catch (e: Exception){
                _uiState.update {it.copy(errorMessage = "Unable to update database") }
            }
            // remember to add a message for success, or a state
        }
    }

    fun resetState() {
        _uiState.update {
            it.copy(
                errorMessage = "",
                maxExpensesError = "",
                minIncomeError = "",
                currExpenseTotalError = ""
            )
        }
    }

    fun verifyNavigation(): Boolean {
        return validateAll()


    }

}

