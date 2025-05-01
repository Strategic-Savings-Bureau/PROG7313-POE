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
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.entities.Income
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class IncomeHistoryAdapter(private var incomeTransactions: List<Income>) :
    RecyclerView.Adapter<IncomeHistoryAdapter.IncomeTransactionViewHolder>()
{
    inner class IncomeTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvType: TextView = itemView.findViewById(R.id.tvTransactionType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvTransactionCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IncomeTransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return IncomeTransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: IncomeHistoryAdapter.IncomeTransactionViewHolder, position: Int) {
        val incomeTransaction = incomeTransactions[position]

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val context = holder.itemView.context

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        holder.ivIcon.setImageResource(R.drawable.ic_transaction_income)
        holder.tvTitle.text = incomeTransaction.title
        holder.tvDate.text = dateFormatter.format(incomeTransaction.date)
        holder.tvType.text = context.getString(R.string.tv_transaction_type_income)
        holder.tvAmount.text = currencyFormat.format(incomeTransaction.amount)
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

            tvTitle.text = incomeTransaction.title
            tvDate.text = dateFormatter.format(incomeTransaction.date)
            tvAmount.text = currencyFormat.format(incomeTransaction.amount)
            tvDescription.text = incomeTransaction.description

            btnClose.setOnClickListener {
                dialog.dismiss()
            }
        }


    }

    override fun getItemCount(): Int = incomeTransactions.size
}