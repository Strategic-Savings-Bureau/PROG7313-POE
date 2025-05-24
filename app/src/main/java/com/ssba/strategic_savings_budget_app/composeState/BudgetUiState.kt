package com.ssba.strategic_savings_budget_app.composeState

import co.touchlab.kermit.Message


data class BudgetUiState(
    val currExpenseTotal: String = "",
    val maximumMonthlyExpenses: String = "",
    val minimumMonthlyIncome: String = "",
    val hasExpensesCategories:Boolean = false,  // checking if the user has expense categories so we can route them to the screen.


    val minIncomeError: String = "",
    val maxExpensesError: String = "",
    val currExpenseTotalError: String = "",
    val errorMessage: String = ""
)
