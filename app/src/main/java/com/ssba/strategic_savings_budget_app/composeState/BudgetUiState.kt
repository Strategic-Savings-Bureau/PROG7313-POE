package com.ssba.strategic_savings_budget_app.composeState

import co.touchlab.kermit.Message


data class BudgetUiState(
    val currExpenseTotal: String = "",
    val maximumMonthlyExpenses: String = "",
    val minimumMonthlyIncome: String = "",



    val minIncomeError: String = "",
    val maxExpensesError: String = "",
    val currExpenseTotalError: String = "",
    val errorMessage: String = ""
)
