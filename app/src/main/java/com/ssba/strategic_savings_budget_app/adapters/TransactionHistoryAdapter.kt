package com.ssba.strategic_savings_budget_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.entities.Saving
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class TransactionHistoryAdapter(private var transactions: List<Any>) :
    RecyclerView.Adapter<TransactionHistoryAdapter.TransactionViewHolder>()
{
    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvType: TextView = itemView.findViewById(R.id.tvTransactionType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvTransactionCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionHistoryAdapter.TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val context = holder.itemView.context

        when (transaction)
        {
            is Expense -> {

                holder.ivIcon.setImageResource(R.drawable.ic_transaction_expense)
                holder.tvTitle.text = transaction.title
                holder.tvDate.text = dateFormatter.format(transaction.date)
                holder.tvType.text = context.getString(R.string.tv_transaction_type_expense)
                holder.tvAmount.text = "R ${transaction.amount}"
                holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.expense_red))

                val db: AppDatabase = AppDatabase.getInstance(context)

                CoroutineScope(Dispatchers.Main).launch {

                    val category = withContext(Dispatchers.IO) {

                        db.expenseCategoryDao.getExpenseCategoryById(transaction.categoryId)
                    }

                    if (category != null)
                    {
                        holder.tvTransactionCategory.text = category.name
                    }
                    else
                    {
                        holder.tvTransactionCategory.text =
                            context.getString(R.string.content_no_category)
                    }
                }
            }
            is Income -> {

                holder.ivIcon.setImageResource(R.drawable.ic_transaction_income)
                holder.tvTitle.text = transaction.title
                holder.tvDate.text = dateFormatter.format(transaction.date)
                holder.tvType.text = context.getString(R.string.tv_transaction_type_income)
                holder.tvAmount.text = "R ${transaction.amount}"
                holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.income_green))
                holder.tvTransactionCategory.visibility = View.GONE
            }
            is Saving -> {

                holder.ivIcon.setImageResource(R.drawable.ic_transaction_savings)
                holder.tvTitle.text = transaction.title
                holder.tvDate.text = dateFormatter.format(transaction.date)
                holder.tvType.text = context.getString(R.string.tv_transaction_type_savings)
                holder.tvAmount.text = "R ${transaction.amount}"
                holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.savings_blue))

                val db: AppDatabase = AppDatabase.getInstance(context)

                CoroutineScope(Dispatchers.Main).launch {

                    val savingsGoal = withContext(Dispatchers.IO) {

                        db.savingsGoalDao.getSavingGoalById(transaction.savingGoalId)
                    }

                    if (savingsGoal != null)
                    {
                        holder.tvTransactionCategory.text = savingsGoal.title
                    }
                    else
                    {
                        holder.tvTransactionCategory.text =
                            context.getString(R.string.content_no_category)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = transactions.size

    fun updateTransactions(newTransactions: List<Any>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}