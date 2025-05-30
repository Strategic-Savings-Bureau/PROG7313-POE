package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.databinding.ActivityAdvancedBudgetSettingsBinding
import com.ssba.strategic_savings_budget_app.databinding.ItemExpenseCategoryCardBinding
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import com.ssba.strategic_savings_budget_app.models.BudgetViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdvancedBudgetSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdvancedBudgetSettingsBinding
    private val viewModel: BudgetViewModel by viewModels()
    private lateinit var adapter: AdvancedCategoryAdapter
    private val db by lazy { AppDatabase.getInstance(this) }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var currentCategories: List<ExpenseCategory> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvancedBudgetSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val userId = auth.currentUser?.uid ?: return
        viewModel.initialDbSet(db)
        viewModel.fetchUserId(userId)
        viewModel.initialLoad()

        adapter = AdvancedCategoryAdapter(
            getMaxBudget = { viewModel.uiState.value.maximumMonthlyExpenses.toDoubleOrNull() ?: 0.0 },
            getAllCategories = { currentCategories },
            onAmountChanged = { updatedCategory ->
                currentCategories = currentCategories.map {
                    if (it.categoryId == updatedCategory.categoryId) updatedCategory else it
                }
                adapter.submitList(currentCategories)
            }
        )

        binding.recyclerExpenseCategories.layoutManager = LinearLayoutManager(this)
        binding.recyclerExpenseCategories.adapter = adapter

        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.textMaxLimit.text = "Max Limit: R ${state.maximumMonthlyExpenses}"

                val categories = withContext(Dispatchers.IO) {
                    db.expenseCategoryDao.getExpenseCategoriesByUserId(userId).sortedByDescending { it.maximumMonthlyTotal }
                }
                currentCategories = categories
                adapter.submitList(categories)
                updateCurrentTotal()
            }
        }

        binding.cancelBtn.setOnClickListener {
            lifecycleScope.launch {
                val savedCategories = withContext(Dispatchers.IO) {
                    db.expenseCategoryDao.getExpenseCategoriesByUserId(userId)
                }
                currentCategories = savedCategories
                adapter.submitList(savedCategories)
                updateCurrentTotal()
                Toast.makeText(this@AdvancedBudgetSettingsActivity, "Changes discarded", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonSave.setOnClickListener {
            val total = currentCategories.sumOf { it.maximumMonthlyTotal }
            val max = viewModel.uiState.value.maximumMonthlyExpenses.toDoubleOrNull() ?: 0.0

            if (total > max) {
                Toast.makeText(this, "Total exceeds maximum monthly limit.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch(Dispatchers.IO) {
                currentCategories.forEach { db.expenseCategoryDao.upsertExpenseCategory(it) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AdvancedBudgetSettingsActivity, "Changes saved", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun updateCurrentTotal() {
        val total = currentCategories.sumOf { it.maximumMonthlyTotal }
        binding.textCurrentTotal.text = "Current Total: R ${"%.2f".format(total)}"

        val max = viewModel.uiState.value.maximumMonthlyExpenses.toDoubleOrNull() ?: 0.0
        binding.buttonSave.isEnabled = total <= max
    }

    class AdvancedCategoryAdapter(
        private val getMaxBudget: () -> Double,
        private val getAllCategories: () -> List<ExpenseCategory>,
        private val onAmountChanged: (ExpenseCategory) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<ExpenseCategory, AdvancedCategoryAdapter.ExpenseCategoryViewHolder>(
        ExpenseCategoryDiffCallback()
    ) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseCategoryViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemExpenseCategoryCardBinding.inflate(inflater, parent, false)
            return ExpenseCategoryViewHolder(binding)
        }

        override fun onBindViewHolder(holder: ExpenseCategoryViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        inner class ExpenseCategoryViewHolder(
            private val binding: ItemExpenseCategoryCardBinding
        ) : RecyclerView.ViewHolder(binding.root) {

            private var currentWatcher: TextWatcher? = null

            fun bind(category: ExpenseCategory) {
                binding.textCategoryName.text = category.name
                Glide.with(binding.imageIcon.context).load(category.icon).into(binding.imageIcon)

                // Remove old watcher
                currentWatcher?.let { binding.editAmount.removeTextChangedListener(it) }

                // Only update text if it's different from current input (also handle formatting mismatch)
                val displayedText = binding.editAmount.text?.toString() ?: ""
                val expectedText = if (category.maximumMonthlyTotal == 0.0) "" else category.maximumMonthlyTotal.toString()

                val programmaticChange = displayedText.toDoubleOrNull() != category.maximumMonthlyTotal

                if (programmaticChange) {
                    binding.editAmount.setText(expectedText)
                    binding.editAmount.setSelection(expectedText.length)
                }

                // New watcher
                currentWatcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                    override fun afterTextChanged(s: Editable?) {
                        // Skip if we're programmatically setting the same value
                        val userInput = s?.toString()?.toDoubleOrNull() ?: return
                        if (userInput == category.maximumMonthlyTotal) return

                        val otherCategories = getAllCategories().filter { it.categoryId != category.categoryId }
                        val currentTotalWithoutThis = otherCategories.sumOf { it.maximumMonthlyTotal }

                        if (currentTotalWithoutThis + userInput <= getMaxBudget()) {
                            onAmountChanged(category.copy(maximumMonthlyTotal = userInput))
                            binding.editAmount.error = null
                        } else {
                            binding.editAmount.error = "Exceeds total budget"
                        }
                    }
                }

                binding.editAmount.addTextChangedListener(currentWatcher)
            }

        }
    }


    class ExpenseCategoryDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<ExpenseCategory>() {
        override fun areItemsTheSame(oldItem: ExpenseCategory, newItem: ExpenseCategory) =
            oldItem.categoryId == newItem.categoryId

        override fun areContentsTheSame(oldItem: ExpenseCategory, newItem: ExpenseCategory) =
            oldItem == newItem
    }
}
