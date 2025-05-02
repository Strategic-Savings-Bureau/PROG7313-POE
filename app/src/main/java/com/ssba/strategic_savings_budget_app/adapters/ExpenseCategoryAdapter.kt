package com.ssba.strategic_savings_budget_app.adapters

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class ExpenseCategoryAdapter(private var expenseCategories: List<ExpenseCategory>) :
    RecyclerView.Adapter<ExpenseCategoryAdapter.CategoryViewHolder>()
{
    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ivCategoryIcon: ImageView = itemView.findViewById(R.id.ivCategoryIcon)
        val tvCategoryTitle: TextView = itemView.findViewById(R.id.tvCategoryTitle)
        val tvMaxMonthlyLimit: TextView = itemView.findViewById(R.id.tvMaxMonthlyLimit)
        val pbLimit: ProgressBar = itemView.findViewById(R.id.progressLimit)
        val tvLimitProgressPercentage: TextView = itemView.findViewById(R.id.tvLimitProgressPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder
    {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expense_category, parent, false)
        return CategoryViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ExpenseCategoryAdapter.CategoryViewHolder, position: Int)
    {
        val category = expenseCategories[position]

        val context = holder.itemView.context

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        holder.tvCategoryTitle.text = category.name
        holder.tvMaxMonthlyLimit.text = "Max Monthly Limit: ${currencyFormat.format(category.maximumMonthlyTotal)}"

        val db: AppDatabase = AppDatabase.getInstance(context)

        CoroutineScope(Dispatchers.Main).launch {

            // get the total monthly expenses for the current category
            val totalMonthlyExpenses = getTotalMonthlyExpensesForCategory(category.name, db)

            val limit = category.maximumMonthlyTotal

            // calculate the progress percentage
            val progressPercentage = (totalMonthlyExpenses / limit) * 100

            holder.pbLimit.progress = progressPercentage.toInt()
            holder.tvLimitProgressPercentage.text = "${progressPercentage.toInt()}% towards limit"

            if (totalMonthlyExpenses > limit)
            {
                holder.tvLimitProgressPercentage.text = "${progressPercentage.toInt()}% past limit"
                holder.tvLimitProgressPercentage.setTextColor(context.getColor(R.color.expense_red))
            }

            // Load category icon
            if (category.icon.isNotEmpty()) {
                try {
                    // Convert the icon string to a URI
                    val iconUri = category.icon.toUri()

                    // Load the icon using Picasso
                    Picasso.get()
                        .load(iconUri)
                        .placeholder(R.drawable.ic_default_image) // shown while loading
                        .error(R.drawable.ic_default_expense_category) // shown if loading fails
                        .into(holder.ivCategoryIcon)

                } catch (e: Exception) {

                    // In case of a malformed URL or any other issue, set fallback icon manually
                    holder.ivCategoryIcon.setImageResource(R.drawable.ic_default_expense_category)

                    // Optionally, log or report the error
                    Log.e("ExpenseCategoryAdapter", "Error loading icon URL: ${category.icon}", e)
                }
            } else {
                // If no icon is set, use a default one
                holder.ivCategoryIcon.setImageResource(R.drawable.ic_default_expense_category)
            }


        }

        holder.itemView.setOnClickListener {
            // get the expense category title
            val categoryTitle = category.name

            // navigate to the expense category activity and pass the title
            //val intent = Intent(context, ExpenseCategoryActivity::class.java)
            //intent.putExtra("EXPENSE_CATEGORY_NAME", categoryTitle)
            //context.startActivity(intent)

            // display a toast for now
            Toast.makeText(context, "Clicked on $categoryTitle", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = expenseCategories.size

    // region Helper Methods

    // get the total of all expenses for the current category in the current month
    private suspend fun getTotalMonthlyExpensesForCategory(categoryTitle: String, db: AppDatabase): Double
    {
        val categoryWithExpenses = db.expenseCategoryDao.getExpensesByCategoryName(categoryTitle)

        if (categoryWithExpenses.isEmpty()) {
            return 0.0
        }

        val expensesList = categoryWithExpenses[0].expenses

        if (expensesList.isEmpty()) {
            return 0.0
        }

        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        var monthlyExpenses = 0.0

        for (expense in expensesList)
        {
            val expenseCalendar = Calendar.getInstance()
            expenseCalendar.time = expense.date

            val expenseMonth = expenseCalendar.get(Calendar.MONTH)
            val expenseYear = expenseCalendar.get(Calendar.YEAR)

            if (expenseMonth == currentMonth && expenseYear == currentYear) {
                monthlyExpenses += expense.amount
            }
        }

        return monthlyExpenses
    }

    // endregion
}