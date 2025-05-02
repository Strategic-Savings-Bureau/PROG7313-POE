package com.ssba.strategic_savings_budget_app.adapters

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
 	*   - Loading and displaying images using Picasso library
 	* Author: Android Developers / Square, Inc.
 	* Date Accessed: 30 April 2025
 	* Sources:
 	*   - NumberFormat: https://developer.android.com/reference/java/text/NumberFormat
 	*   - Picasso: https://github.com/square/picasso
*/

class ExpenseCategoryTransactionHistoryAdapter(private var expenses: List<Expense>) :
    RecyclerView.Adapter<ExpenseCategoryTransactionHistoryAdapter.ExpenseCategoryTransactionViewHolder>()
{
    inner class ExpenseCategoryTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvType: TextView = itemView.findViewById(R.id.tvTransactionType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvTransactionCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseCategoryTransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return ExpenseCategoryTransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseCategoryTransactionHistoryAdapter.ExpenseCategoryTransactionViewHolder, position: Int)
    {
        val transaction = expenses[position]

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val context = holder.itemView.context

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

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

    override fun getItemCount(): Int = expenses.size
}