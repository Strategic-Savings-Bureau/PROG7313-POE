package com.ssba.strategic_savings_budget_app.adapters

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.budget.AdvancedBudgetSettingsActivity.AdvancedCategoryAdapter
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory

class AdvancedExpenseCategoryAdapter(
    private val categories: List<ExpenseCategory>,
    private val onAmountChanged: (ExpenseCategory) -> Unit
) : RecyclerView.Adapter<AdvancedExpenseCategoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.image_icon)
        val name: TextView = itemView.findViewById(R.id.text_category_name)
        val amount: EditText = itemView.findViewById(R.id.edit_amount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_category_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        // Load icon using Glide or similar library
        Glide.with(holder.icon.context).load(category.icon).into(holder.icon)
        holder.name.text = category.name
        holder.amount.setText(category.maximumMonthlyTotal.toString())

        holder.amount.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val newAmount = s.toString().toDoubleOrNull()
                if (newAmount != null && newAmount != category.maximumMonthlyTotal) {
                    onAmountChanged(category.copy(maximumMonthlyTotal = newAmount))
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun getItemCount(): Int = categories.size
}
