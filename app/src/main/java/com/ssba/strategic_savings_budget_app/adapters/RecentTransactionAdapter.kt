package com.ssba.strategic_savings_budget_app.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.entities.Expense
import com.ssba.strategic_savings_budget_app.entities.Income
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/*
 	* Code Attribution
 	* Purpose: Formatting numbers as South African Rand (ZAR) currency using NumberFormat
 	* Author: Android Developers
 	* Date Accessed: 30 April 2025
 	* Source: Developer Guide - Android Developers (Java Platform Standard Edition docs)
 	* URL: https://developer.android.com/reference/java/text/NumberFormat
*/

class RecentTransactionAdapter(private var transactions: List<Any>) : RecyclerView.Adapter<RecentTransactionAdapter.TransactionViewHolder>()
{
    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvType: TextView = itemView.findViewById(R.id.tvTransactionType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
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
            }
            is Income -> {

                holder.ivIcon.setImageResource(R.drawable.ic_transaction_income)
                holder.tvTitle.text = transaction.title
                holder.tvDate.text = dateFormatter.format(transaction.date)
                holder.tvType.text = context.getString(R.string.tv_transaction_type_income)
                holder.tvAmount.text = currencyFormat.format(transaction.amount)
                holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.income_green))
            }
        }
    }

    override fun getItemCount(): Int = transactions.size

}