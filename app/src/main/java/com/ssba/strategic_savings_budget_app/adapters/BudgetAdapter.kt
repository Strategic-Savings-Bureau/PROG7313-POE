package com.ssba.strategic_savings_budget_app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.databinding.ActivityItemBudgetCategoryBinding
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory

class BudgetCategoryAdapter(
    private var categories: List<ExpenseCategory>,
    private val onClick: (ExpenseCategory) -> Unit
) : RecyclerView.Adapter<BudgetCategoryAdapter.VH>() {

    inner class VH(val b: ActivityItemBudgetCategoryBinding) :
        RecyclerView.ViewHolder(b.root) {
        fun bind(cat: ExpenseCategory) {
            if (cat.icon.isNotBlank()) {
                Picasso.get()
                    .load(cat.icon)

            } else {
                b.ivCategoryIcon.setImageResource(R.drawable.ic_transaction_expense)
            }
            // name (never overwritten)
            b.tvCategoryName.text = cat.name
            // limit is its own field
            b.etCategoryLimit.setText("R %.2f".format(cat.maximumMonthlyTotal))

            b.root.setOnClickListener {
                onClick(cat)
            }
        }
    }
    fun getEditedCategories(recyclerView: RecyclerView): List<ExpenseCategory> {
        val updatedList = mutableListOf<ExpenseCategory>()
        for (i in categories.indices) {
            val holder = recyclerView.findViewHolderForAdapterPosition(i) as? VH ?: continue
            val text = holder.b.etCategoryLimit.text.toString()
            val newLimit = text.toDoubleOrNull() ?: categories[i].maximumMonthlyTotal
            updatedList.add(categories[i].copy(maximumMonthlyTotal = newLimit))
        }
        return updatedList
    }

    fun setData(newCategories: List<ExpenseCategory>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ActivityItemBudgetCategoryBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount(): Int = categories.size
}
