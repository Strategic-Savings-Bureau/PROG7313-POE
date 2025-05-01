package com.ssba.strategic_savings_budget_app.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class BudgetSettingsViewModel:ViewModel() {
    val minimumMonthlyIncome = MutableLiveData("")
    val maximumMonthlyExpenses = MutableLiveData("")

    private val _minIncomeError = MutableLiveData<String?>()
    val minIncomeError: LiveData<String?> = _minIncomeError

    private val _maxExpensesError = MutableLiveData<String?>()
    val maxExpensesError: LiveData<String?> = _maxExpensesError

    fun validateAll(): Boolean {
        var valid = true

        // min income > 0
        val min = minimumMonthlyIncome.value?.toDoubleOrNull()
        if (min == null || min <= 0.0) {
            _minIncomeError.value = "Enter an amount greater than 0"
            valid = false
        } else {
            _minIncomeError.value = null
        }

        // max expenses > 0
        val max = maximumMonthlyExpenses.value?.toDoubleOrNull()
        if (max == null || max <= 0.0) {
            _maxExpensesError.value = "Enter an amount greater than 0"
            valid = false
        } else {
            _maxExpensesError.value = null
        }

        return valid
    }
}