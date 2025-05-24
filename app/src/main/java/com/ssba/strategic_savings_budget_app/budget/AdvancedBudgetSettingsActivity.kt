package com.ssba.strategic_savings_budget_app.budget

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.adapters.AdvancedExpenseCategoryAdapter
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
    private val db = AppDatabase.getInstance(this)
    val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdvancedBudgetSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (auth.currentUser == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
            Intent(this, LoginActivity::class.java)
            finish()
        }
        val userId = auth.currentUser?.uid
        viewModel.initialDbSet(db)
        viewModel.fetchUserId(userId!!)
        viewModel.initialLoad()
        adapter = AdvancedCategoryAdapter { updatedCategory ->
            lifecycleScope.launch(Dispatchers.IO) {
                db.expenseCategoryDao.upsertExpenseCategory(updatedCategory)
            }
        }
        binding.recyclerExpenseCategories.layoutManager = LinearLayoutManager(this)
        binding.recyclerExpenseCategories.adapter = adapter

        // Observe ViewModel state changes
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.textMaxLimit.text = "Max Limit: R ${state.maximumMonthlyExpenses}"

                // Load and sort categories here
                val categories = withContext(Dispatchers.IO) {

                    db.expenseCategoryDao.getExpenseCategoriesByUserId(userId = userId!!)
                        .sortedByDescending { it.maximumMonthlyTotal }
                }

                val currentTotal = categories.sumOf { it.maximumMonthlyTotal }
                binding.textCurrentTotal.text =
                    "Current Total: R ${String.format("%.2f", currentTotal)}"

                adapter.submitList(categories)
            }
        }
    }

    class AdvancedCategoryAdapter(
        private val onAmountChanged: (ExpenseCategory) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<ExpenseCategory, ExpenseCategoryViewHolder>(
        ExpenseCategoryDiffCallback()
    ) {
        override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ExpenseCategoryViewHolder {
            val inflater = android.view.LayoutInflater.from(parent.context)
            val binding = ItemExpenseCategoryCardBinding.inflate(inflater, parent, false)
            return ExpenseCategoryViewHolder(binding, onAmountChanged)
        }

        override fun onBindViewHolder(holder: ExpenseCategoryViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }

    class ExpenseCategoryViewHolder(
        private val binding: ItemExpenseCategoryCardBinding,
        private val onAmountChanged: (ExpenseCategory) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(category: ExpenseCategory) {
            binding.textCategoryName.text = category.name
            Glide.with(binding.imageIcon.context).load(category.icon).into(binding.imageIcon)
            binding.editAmount.setText(category.maximumMonthlyTotal.toString())

            binding.editAmount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newAmount = s.toString().toDoubleOrNull()
                    if (newAmount != null && newAmount != category.maximumMonthlyTotal) {
                        onAmountChanged(category.copy(maximumMonthlyTotal = newAmount))
                    }
                }
            })
        }
    }

    class ExpenseCategoryDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<ExpenseCategory>() {
        override fun areItemsTheSame(oldItem: ExpenseCategory, newItem: ExpenseCategory): Boolean {
            return oldItem.categoryId == newItem.categoryId
        }

        override fun areContentsTheSame(oldItem: ExpenseCategory, newItem: ExpenseCategory): Boolean {
            return oldItem == newItem
        }
    }
}