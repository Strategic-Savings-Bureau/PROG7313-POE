package com.ssba.strategic_savings_budget_app.models

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CreateCategoryViewModel: ViewModel() {

    val categoryName = MutableLiveData("")
    val categoryDescription = MutableLiveData("")
    val maximumMonthlyTotal = MutableLiveData("")

    private val _categoryNameError = MutableLiveData<String?>()
    val categoryNameError: LiveData<String?> = _categoryNameError

    private val _categoryDescriptionError = MutableLiveData<String?>()
    val categoryDescriptionError: LiveData<String?> = _categoryDescriptionError

    private val _maximumMonthlyTotalError = MutableLiveData<String?>()
    val maximumMonthlyTotalError: LiveData<String?> = _maximumMonthlyTotalError

    fun validateAll(): Boolean {
        var valid = true

        // Validate category name (not empty, at least 3 characters)
        if (categoryName.value.isNullOrEmpty() || categoryName.value!!.length < 3) {
            _categoryNameError.value = "Name must be at least 3 characters"
            valid = false
        } else {
            _categoryNameError.value = null
        }

        // Validate category description (not empty, at least 3 characters)
        if (categoryDescription.value.isNullOrEmpty() || categoryDescription.value!!.length < 3) {
            _categoryDescriptionError.value = "Description must be at least 3 characters"
            valid = false
        } else {
            _categoryDescriptionError.value = null
        }

        // Validate maximum monthly total (should be a positive number)
        val maxTotal = maximumMonthlyTotal.value?.toDoubleOrNull()
        if (maxTotal == null || maxTotal <= 0.0) {
            _maximumMonthlyTotalError.value = "Enter a valid amount greater than 0"
            valid = false
        } else {
            _maximumMonthlyTotalError.value = null
        }

        return valid
    }

    // Reset all fields
    fun resetFields() {
        categoryName.value = ""
        categoryDescription.value = ""
        maximumMonthlyTotal.value = ""
    }
}
