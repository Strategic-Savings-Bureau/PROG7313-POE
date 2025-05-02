package com.ssba.strategic_savings_budget_app.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.net.Uri

/**
 * Shared fields and validation for generic entries: Expense, Income, Savings, SavingsGoal
 */
open class BaseEntryViewModel : ViewModel() {
    // Common fields
    val titleOrName = MutableLiveData("")        // "title" or "name"
    val date = MutableLiveData("")               // formatted date string
    val amount = MutableLiveData("")             // amount or goalAmount
    val description = MutableLiveData("")        // optional notes
    val receiptUri = MutableLiveData<Uri?>(null)   // optional attachment

    // Common validation errors
    private val _titleError = MutableLiveData<String?>()
    val titleError: LiveData<String?> = _titleError

    private val _dateError = MutableLiveData<String?>()
    val dateError: LiveData<String?> = _dateError

    private val _amountError = MutableLiveData<String?>()
    val amountError: LiveData<String?> = _amountError

    private val _descriptionError = MutableLiveData<String?>()
    val descriptionError: LiveData<String?> = _descriptionError

    /**
     * Runs validation for shared fields.
     * Override or extend for screen-specific checks.
     */
    open fun validateAll(): Boolean {
        var valid = true
        // title/name
        if (titleOrName.value.isNullOrBlank()) {
            _titleError.value = "Please enter a ${fieldLabelTitleOrName()}"
            valid = false
        } else _titleError.value = null
        // date
        if (date.value.isNullOrBlank()) {
            _dateError.value = "Please pick a date"
            valid = false
        } else _dateError.value = null
        // amount
        val amt = amount.value?.toDoubleOrNull()
        if (amt == null || amt <= 0.0) {
            _amountError.value = "Enter a value greater than 0"
            valid = false
        } else _amountError.value = null
        // description optional by default (override if required)
        return valid
    }

    /**
     * Label for titleOrName field; override to change between "title", "name", etc.
     */
    open fun fieldLabelTitleOrName() = "title"
}

/**
 * Expense-specific ViewModel
 * Adds category selection
 */
class ExpenseEntryViewModel : BaseEntryViewModel() {
    val categoryPosition = MutableLiveData(0)
    private val _categoryError = MutableLiveData<String?>()
    val categoryError: LiveData<String?> = _categoryError

    override fun validateAll(): Boolean {
        var valid = super.validateAll()
        // category
        if ((categoryPosition.value ?: -1) < 0) {
            _categoryError.value = "Please select a category"
            valid = false
        } else _categoryError.value = null
        return valid
    }
}

/**
 * Income-specific ViewModel
 * No category
 */
class IncomeEntryViewModel : BaseEntryViewModel() {
    override fun fieldLabelTitleOrName() = "income title"
}

/**
 * Savings entry ViewModel
 * Preloads a list of saving types (e.g. "Emergency Fund", "Vacation", etc.)
 */
class SavingsEntryViewModel() : BaseEntryViewModel() {
    val typePosition = MutableLiveData(0)

    private val _typeError = MutableLiveData<String?>()
    val typeError: LiveData<String?> = _typeError

    // This will hold the dynamically loaded savings goal titles
    val savingsGoals = MutableLiveData<List<String>>()

    // This will hold the ID of the selected savings goal
    val selectedGoalId = MutableLiveData<Int?>()

    override fun validateAll(): Boolean {
        var valid = super.validateAll()

        val goals = savingsGoals.value ?: emptyList()
        val pos = typePosition.value ?: -1

        if (pos < 0 || pos >= goals.size) {
            _typeError.value = "Please select a saving goal"
            valid = false
        } else {
            _typeError.value = null
        }

        return valid
    }
}

/**
 * Savings Goal ViewModel
 * Renames title -> name, amount -> goal amount, adds deadline
 */
class SavingsGoalViewModel : BaseEntryViewModel() {

}
