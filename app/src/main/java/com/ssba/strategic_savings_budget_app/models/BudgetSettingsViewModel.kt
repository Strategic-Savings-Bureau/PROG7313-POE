package com.ssba.strategic_savings_budget_app.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BudgetSettingsViewModel : ViewModel() {

    val minimumMonthlyIncome = MutableLiveData("")
    val maximumMonthlyExpenses = MutableLiveData("")
    val currExpenseTotal = MutableLiveData("")

    private val _minIncomeError = MutableLiveData<String?>()
    val minIncomeError: LiveData<String?> = _minIncomeError

    private val _maxExpensesError = MutableLiveData<String?>()
    val maxExpensesError: LiveData<String?> = _maxExpensesError

    private val _currExpenseTotalError = MutableLiveData<String?>()
    val currExpenseTotalError: LiveData<String?> = _currExpenseTotalError

    fun validateAll(): Boolean {
        var valid = true

        val min = minimumMonthlyIncome.value?.toDoubleOrNull()
        if (min == null || min <= 0.0) {
            _minIncomeError.value = "Enter an amount greater than 0"
            valid = false
        } else {
            _minIncomeError.value = null
        }

        val max = maximumMonthlyExpenses.value?.toDoubleOrNull()
        if (max == null || max <= 0.0) {
            _maxExpensesError.value = "Enter an amount greater than 0"
            valid = false
        } else {
            _maxExpensesError.value = null
        }

        if (!validateMaximumExpense()) valid = false

        return valid
    }

    private fun validateMaximumExpense(): Boolean {
        val curr = currExpenseTotal.value?.toDoubleOrNull()
        val max = maximumMonthlyExpenses.value?.toDoubleOrNull()

        return if (curr != null && max != null && curr > max) {
            _currExpenseTotalError.value = "Expense total ($curr) exceeds maximum allowed ($max)"
            false
        } else {
            _currExpenseTotalError.value = null
            true
        }
    }
}
