package com.ssba.strategic_savings_budget_app.models

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssba.strategic_savings_budget_app.helpers.RetrofitUtils
import com.ssba.strategic_savings_budget_app.interfaces.CurrencyService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The complete state for the Currency Converter UI.
 * This single object will drive all UI updates.
 */
data class CurrencyConverterUiState(
    val convertedAmount: String = "", // Use String for direct display in EditText
    val isLoading: Boolean = true,   // Start with loading as true for the initial fetch
    val error: String? = null
)

class CurrencyConverterViewModel : ViewModel() {

    // Private state that can be modified within the ViewModel
    private val _uiState = MutableStateFlow(CurrencyConverterUiState())
    // Public, read-only state for the UI to observe
    val uiState: StateFlow<CurrencyConverterUiState> = _uiState.asStateFlow()

    private val currencyService: CurrencyService = RetrofitUtils.retrofit2().create(CurrencyService::class.java)

  // Locally holds the initial exchange rate to avoid overuse of api
    private var allRates: Map<String, Double> = emptyMap()

    /**
     * Fetches all latest currency rates once and stores them.
     * This should be called from the Activity's onCreate.
     *
     * NOTE: This assumes your CurrencyService has a method to get all latest rates.
     * If your API plan requires specifying symbols, you can pass a long comma-separated
     * string of all currencies you support. Getting all rates is preferred.
     *
     * @param apiKey Your API key for the currency service.
     */
    fun loadAllRates(symbols:String,apiKey: String) {
        // Set loading state to true while we fetch
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // We assume the service has a method like `getLatestRates` to fetch all rates.
                val response = currencyService.getDesiredCurrency(apiKey) // Adapt if your endpoint is different

                if (response.isSuccessful && response.body() != null) {
                    // Success: Store the rates and update the state to "not loading".
                    allRates = response.body()!!.rates
                    _uiState.update { it.copy(isLoading = false, error = null) }
                } else {
                    // API error: Update state with the error message.
                    val errorMessage = "API Error: ${response.code()} ${response.message()}"
                    _uiState.update { it.copy(isLoading = false, error = errorMessage) }
                }
            } catch (e: Exception) {
                // Network or other exception: Update state with the error message.
                val errorMessage = "Network Error: ${e.message}"
                _uiState.update { it.copy(isLoading = false, error = errorMessage) }
            }
        }
    }

    /**
     * REFACTORED: Performs the conversion using the pre-fetched rates.
     * This function is now synchronous and extremely fast. It does NOT make a network call.
     *
     * @param amountStr The input amount as a String from the EditText.
     * @param fromCurrency The currency code to convert from (e.g., "ZAR").
     * @param toCurrency The currency code to convert to (e.g., "USD").
     */
    fun calculateConversion(amountStr: String, fromCurrency: String, toCurrency: String) {
        // If rates haven't been loaded yet, do nothing.
        if (allRates.isEmpty()) {
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount == 0.0) {
            _uiState.update { it.copy(convertedAmount = "") }
            return
        }

        if (fromCurrency == toCurrency) {
            _uiState.update { it.copy(convertedAmount = String.format("%.2f", amount)) }
            return
        }

        // Get rates from our stored map
        val fromRate = allRates[fromCurrency]
        val toRate = allRates[toCurrency]

        if (fromRate != null && toRate != null && fromRate != 0.0) {
            // CORE LOGIC: Same math, but using local data.
            // 1. Convert input amount to the base currency (EUR)
            val amountInBase = amount / fromRate
            // 2. Convert base currency amount to the target currency
            val finalResult = amountInBase * toRate

            // Update state with the final result
            _uiState.update { it.copy(convertedAmount = String.format("%.2f", finalResult)) }
        } else {
            // Handle case where a currency wasn't in our fetched rates
            // You might want to show a small error or just clear the field.
            _uiState.update { it.copy(convertedAmount = "", error = "Rates for $fromCurrency or $toCurrency not available.") }
        }
    }
}