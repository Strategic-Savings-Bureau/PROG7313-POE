package com.ssba.strategic_savings_budget_app.adapters

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.Income
import com.ssba.strategic_savings_budget_app.entities.Saving
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TransactionHistoryAdapter.TransactionViewHolder, position: Int) {
        val transaction = transactions[position]

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val context = holder.itemView.context

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        when (transaction)
        {
            is Expense -> {

                holder.ivIcon.setImageResource(R.drawable.ic_transaction_expense)
                holder.tvTitle.text = transaction.title
                holder.tvDate.text = dateFormatter.format(transaction.date)
                holder.tvType.text = context.getString(R.string.tv_transaction_type_expense)
                holder.tvAmount.text = currencyFormat.format(transaction.amount)
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

                    holder.itemView.setOnClickListener {

                        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_expense_transaction, null)
                        val dialog = AlertDialog.Builder(context)
                            .setView(dialogView)
                            .create()

                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        dialog.show()

                        // Get view components from dialog
                        val tvTitle = dialogView.findViewById<TextView>(R.id.tvExpenseTitle)
                        val tvDate = dialogView.findViewById<TextView>(R.id.tvExpenseDate)
                        val tvAmount = dialogView.findViewById<TextView>(R.id.tvExpenseAmount)
                        val tvDescription = dialogView.findViewById<TextView>(R.id.tvExpenseDescription)
                        val tvCategory = dialogView.findViewById<TextView>(R.id.tvExpenseCategory)
                        val ivReceipt = dialogView.findViewById<ImageView>(R.id.ivReceipt)
                        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseExpense)

                        tvTitle.text = transaction.title
                        tvDate.text = dateFormatter.format(transaction.date)
                        tvAmount.text = currencyFormat.format(transaction.amount)
                        tvDescription.text = transaction.description

                        if (category != null)
                        {
                            tvCategory.text = category.name
                        }
                        else
                        {
                            tvCategory.text = context.getString(R.string.content_no_category)
                        }

                        if (transaction.receiptPictureUrl.isNotBlank())
                        {
                            Picasso.get()
                                .load(transaction.receiptPictureUrl)
                                .placeholder(R.drawable.ic_default_image)
                                .into(ivReceipt)
                        }
                        else
                        {
                            ivReceipt.visibility = View.GONE
                        }

                        btnClose.setOnClickListener {
                            dialog.dismiss()
                        }
                    }
                }
            }
            is Income -> {

                holder.ivIcon.setImageResource(R.drawable.ic_transaction_income)
                holder.tvTitle.text = transaction.title
                holder.tvDate.text = dateFormatter.format(transaction.date)
                holder.tvType.text = context.getString(R.string.tv_transaction_type_income)
                holder.tvAmount.text = currencyFormat.format(transaction.amount)
                holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.income_green))
                holder.tvTransactionCategory.visibility = View.GONE

                holder.itemView.setOnClickListener {

                    val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_income_transaction, null)
                    val dialog = AlertDialog.Builder(context)
                        .setView(dialogView)
                        .create()

                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    dialog.show()

                    // Get view components from dialog
                    val tvTitle = dialogView.findViewById<TextView>(R.id.tvIncomeTitle)
                    val tvDate = dialogView.findViewById<TextView>(R.id.tvIncomeDate)
                    val tvAmount = dialogView.findViewById<TextView>(R.id.tvIncomeAmount)
                    val tvDescription = dialogView.findViewById<TextView>(R.id.tvIncomeDescription)
                    val btnClose = dialogView.findViewById<Button>(R.id.btnIncomeClose)

                    tvTitle.text = transaction.title
                    tvDate.text = dateFormatter.format(transaction.date)
                    tvAmount.text = currencyFormat.format(transaction.amount)
                    tvDescription.text = transaction.description

                    btnClose.setOnClickListener {
                        dialog.dismiss()
                    }
                }

            }
            is Saving -> {

                holder.ivIcon.setImageResource(R.drawable.ic_transaction_savings)
                holder.tvTitle.text = transaction.title
                holder.tvDate.text = dateFormatter.format(transaction.date)
                holder.tvType.text = context.getString(R.string.tv_transaction_type_savings)
                holder.tvAmount.text = currencyFormat.format(transaction.amount)
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

                    holder.itemView.setOnClickListener {

                        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_savings_transaction, null)
                        val dialog = AlertDialog.Builder(context)
                            .setView(dialogView)
                            .create()

                        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                        dialog.show()

                        // Get view components from dialog
                        val tvTitle = dialogView.findViewById<TextView>(R.id.tvSavingTitle)
                        val tvDate = dialogView.findViewById<TextView>(R.id.tvSavingDate)
                        val tvAmount = dialogView.findViewById<TextView>(R.id.tvSavingAmount)
                        val tvDescription = dialogView.findViewById<TextView>(R.id.tvSavingDescription)
                        val tvSavingGoalName = dialogView.findViewById<TextView>(R.id.tvSavingGoalName)
                        val btnClose = dialogView.findViewById<Button>(R.id.btnCloseSaving)

                        tvTitle.text = transaction.title
                        tvDate.text = dateFormatter.format(transaction.date)
                        tvAmount.text = currencyFormat.format(transaction.amount)
                        tvDescription.text = transaction.description

                        if (savingsGoal != null)
                        {
                            tvSavingGoalName.text = savingsGoal.title
                        }
                        else
                        {
                            tvSavingGoalName.text = context.getString(R.string.content_no_category)
                        }

                        btnClose.setOnClickListener {
                            dialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int = transactions.size

}