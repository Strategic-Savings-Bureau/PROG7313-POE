package com.ssba.strategic_savings_budget_app.adapters

import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory

class AdvancedExpenseCategoryAdapter(
    private val categories: List<ExpenseCategory>,
    private val onAmountChanged: (ExpenseCategory) -> Unit,
) : RecyclerView.Adapter<AdvancedExpenseCategoryAdapter.ViewHolder>() {

    // The ViewHolder now holds a reference to its TextWatcher
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.image_icon)
        val name: TextView = itemView.findViewById(R.id.text_category_name)
        val amount: EditText = itemView.findViewById(R.id.edit_amount)
        var textWatcher: TextWatcher? = null // To hold the listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_expense_category_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]

        // --- Icon Loading Logic (which is already good) ---
        if (category.icon.isNotEmpty()) {
            try {
                val iconUri = category.icon.toUri()
                Glide.with(holder.itemView.context).load(iconUri)
                    .centerInside()
                    .placeholder(R.drawable.ic_default_image)
                    .error(R.drawable.ic_default_expense_category)
                    .into(holder.icon)
            } catch (e: Exception) {
                holder.icon.setImageResource(R.drawable.ic_default_expense_category)
                Log.e("ExpenseCategoryAdapter", "Error loading icon URL: ${category.icon}", e)
            }
        } else {
            holder.icon.setImageResource(R.drawable.ic_default_expense_category)
        }

        holder.name.text = category.name

        // --- CORRECTED TEXTWATCHER LOGIC ---

        // 1. First, remove any existing listener from the recycled view
        if (holder.textWatcher != null) {
            holder.amount.removeTextChangedListener(holder.textWatcher)
        }

        // 2. Set the text value AFTER removing the listener
        holder.amount.setText(category.maximumMonthlyTotal.toString())

        // 3. Create a new listener that closes over the CURRENT category
        holder.textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // Check for programmatic changes to prevent loops
                val currentText = s.toString()
                if (currentText == category.maximumMonthlyTotal.toString()) {
                    return
                }

                val newAmount = currentText.toDoubleOrNull()
                if (newAmount != null) {
                    onAmountChanged(category.copy(maximumMonthlyTotal = newAmount))
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        // 4. Add the new listener
        holder.amount.addTextChangedListener(holder.textWatcher)
    }

    override fun getItemCount(): Int = categories.size
}