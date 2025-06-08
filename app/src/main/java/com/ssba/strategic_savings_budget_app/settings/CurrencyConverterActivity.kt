package com.ssba.strategic_savings_budget_app.settings

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.SettingsActivity
import com.ssba.strategic_savings_budget_app.adapters.RecentTransactionAdapter
import com.ssba.strategic_savings_budget_app.databinding.ActivityCurrencyConverterBinding
import com.ssba.strategic_savings_budget_app.models.CurrencyConverterViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch



class CurrencyConverterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCurrencyConverterBinding
    private val viewModel: CurrencyConverterViewModel by viewModels()
    private val dummyCurrencies = listOf("USD", "EUR", "JPY", "GBP", "CAD", "AUD", "ZAR")

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityCurrencyConverterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSpinners()
        setupListeners()
        observeViewModel() // Start observing the ViewModel for state changes
        val symbolFrom = binding.spinnerFrom.selectedItem.toString()
        val symbolTo = binding.spinnerTo.selectedItem.toString()
        val symbols = "$symbolFrom,$symbolTo"
        val key = getString(R.string.currency_freaks)
        viewModel.loadAllRates(apiKey = key, symbols = symbols)
    }



    private fun setupSpinners() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dummyCurrencies)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerFrom.adapter = adapter
        binding.spinnerTo.adapter = adapter

        binding.spinnerFrom.setSelection(adapter.getPosition("ZAR"))
        binding.spinnerTo.setSelection(adapter.getPosition("USD"))
    }

    /**
     * Sets up listeners to report user actions to the ViewModel.
     */
    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }

        binding.swapCurrency.setOnClickListener {
            val posFrom = binding.spinnerFrom.selectedItemPosition
            val posTo = binding.spinnerTo.selectedItemPosition
            binding.spinnerFrom.setSelection(posTo)
            binding.spinnerTo.setSelection(posFrom)
            // The spinner's onItemSelected listener will automatically trigger the conversion.

        }

        binding.etAmountFrom.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // As the user types, trigger the conversion logic.
                triggerConversion()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // When a new currency is selected, trigger the conversion logic.
                val symbolFrom = binding.spinnerFrom.selectedItem.toString()
                val symbolTo = binding.spinnerTo.selectedItem.toString()
                val symbols = "$symbolFrom,$symbolTo"
                val key = getString(R.string.currency_freaks)
                viewModel.loadAllRates(apiKey = key, symbols = symbols)

                triggerConversion()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        binding.spinnerFrom.onItemSelectedListener = spinnerListener
        binding.spinnerTo.onItemSelectedListener = spinnerListener
    }

    /**
     * This function now only gathers inputs and passes them to the ViewModel.
     */
    private fun triggerConversion() {
        val amount = binding.etAmountFrom.text.toString()
        val fromCurrency = binding.spinnerFrom.selectedItem.toString()
        val toCurrency = binding.spinnerTo.selectedItem.toString()

        // Tell the ViewModel that the input has changed.
        viewModel.calculateConversion(amount, fromCurrency, toCurrency)
    }

    /**
     * Observes the ViewModel's state and updates the UI accordingly.
     * This is the core of the reactive pattern.
     */
    private fun observeViewModel() {
        lifecycleScope.launch {
            // collectLatest is efficient here; if a new state arrives before
            // the UI is done updating, it cancels the old one and starts with the new.
            viewModel.uiState.collectLatest { state ->
                // Update the loading indicator's visibility
                binding.progressBar.isVisible = state.isLoading

                // Update the output amount. Check to prevent infinite loops with TextWatcher.
                if (binding.etAmountTo.text.toString() != state.convertedAmount) {
                    binding.etAmountTo.setText(state.convertedAmount)
                }

                // Show an error message if there is one
                state.error?.let { error ->
                    Log.e("CurrencyConverterActivity", "Error: $error")
                    Toast.makeText(this@CurrencyConverterActivity, error, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}