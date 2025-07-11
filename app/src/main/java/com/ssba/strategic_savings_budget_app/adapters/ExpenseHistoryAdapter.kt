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
import com.bumptech.glide.Glide
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.Expense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/*
 	* Code Attribution
 	* Purpose:
 	*   - Formatting numbers as South African Rand (ZAR) currency using NumberFormat
 	*   - Loading and displaying images using Glide library
 	* Author: Android Developers / BumpTech
 	* Date Accessed: 30 April 2025
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - Glide: https://github.com/bumptech/glide
*/


class ExpenseHistoryAdapter(private var expenseTransactions: List<Expense>) :
    RecyclerView.Adapter<ExpenseHistoryAdapter.ExpenseTransactionViewHolder>()
{
    inner class ExpenseTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvType: TextView = itemView.findViewById(R.id.tvTransactionType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvTransactionCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseTransactionViewHolder
    {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ExpenseTransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ExpenseHistoryAdapter.ExpenseTransactionViewHolder, position: Int)
    {

        val expenseTransaction = expenseTransactions[position]

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val context = holder.itemView.context

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        holder.ivIcon.setImageResource(R.drawable.ic_transaction_expense)
        holder.tvTitle.text = expenseTransaction.title
        holder.tvDate.text = dateFormatter.format(expenseTransaction.date)
        holder.tvType.text = context.getString(R.string.tv_transaction_type_expense)
        holder.tvAmount.text = currencyFormat.format(expenseTransaction.amount)
        holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.expense_red))

        val db: AppDatabase = AppDatabase.getInstance(context)

        CoroutineScope(Dispatchers.Main).launch {

            val category = withContext(Dispatchers.IO) {

                db.expenseCategoryDao().getExpenseCategoryById(expenseTransaction.categoryId)
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

                /*
 	                        * Code Attribution
 	                        * Purpose: Creating and displaying an AlertDialog in an Android app
 	                        * Author: Android Developers
 	                        * Date Accessed: 29 April 2025
 	                        * Source: Developer Guide - Android Developers
 	                        * URL: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
                 */

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

                tvTitle.text = expenseTransaction.title
                tvDate.text = dateFormatter.format(expenseTransaction.date)
                tvAmount.text = currencyFormat.format(expenseTransaction.amount)
                tvDescription.text = expenseTransaction.description

                if (category != null)
                {
                    tvCategory.text = category.name
                }
                else
                {
                    tvCategory.text = context.getString(R.string.content_no_category)
                }

                if (expenseTransaction.receiptPictureUrl.isNotBlank())
                {
                    // Load image with Glide
                    Glide.with(context)
                        .load(expenseTransaction.receiptPictureUrl)
                        .placeholder(R.drawable.ic_default_image)
                        .error(R.drawable.ic_error_image)
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

    override fun getItemCount(): Int = expenseTransactions.size
}