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
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.Saving
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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

class SavingGoalTransactionHistoryAdapter(private var savings: List<Saving>) :
    RecyclerView.Adapter<SavingGoalTransactionHistoryAdapter.SavingGoalTransactionViewHolder>()
{
    inner class SavingGoalTransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val ivIcon: ImageView = itemView.findViewById(R.id.ivTransactionType)
        val tvTitle: TextView = itemView.findViewById(R.id.tvTransactionTitle)
        val tvDate: TextView = itemView.findViewById(R.id.tvTransactionDate)
        val tvType: TextView = itemView.findViewById(R.id.tvTransactionType)
        val tvAmount: TextView = itemView.findViewById(R.id.tvTransactionAmount)
        val tvTransactionCategory: TextView = itemView.findViewById(R.id.tvTransactionCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingGoalTransactionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return SavingGoalTransactionViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SavingGoalTransactionHistoryAdapter.SavingGoalTransactionViewHolder, position: Int)
    {
        val savingsTransaction = savings[position]

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val context = holder.itemView.context

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        holder.ivIcon.setImageResource(R.drawable.ic_transaction_savings)
        holder.tvTitle.text = savingsTransaction.title
        holder.tvDate.text = dateFormatter.format(savingsTransaction.date)
        holder.tvType.text = context.getString(R.string.tv_transaction_type_savings)
        holder.tvAmount.text = currencyFormat.format(savingsTransaction.amount)
        holder.tvAmount.setTextColor(holder.itemView.context.getColor(R.color.savings_blue))

        val db: AppDatabase = AppDatabase.getInstance(context)

        CoroutineScope(Dispatchers.Main).launch {

            val savingsGoal = withContext(Dispatchers.IO) {

                db.savingsGoalDao.getSavingGoalById(savingsTransaction.savingGoalId)
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

                /*
 	                        * Code Attribution
 	                        * Purpose: Creating and displaying an AlertDialog in an Android app
 	                        * Author: Android Developers
 	                        * Date Accessed: 29 April 2025
 	                        * Source: Developer Guide - Android Developers
 	                        * URL: https://developer.android.com/guide/topics/ui/dialogs/alert-dialog
                 */

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

                tvTitle.text = savingsTransaction.title
                tvDate.text = dateFormatter.format(savingsTransaction.date)
                tvAmount.text = currencyFormat.format(savingsTransaction.amount)
                tvDescription.text = savingsTransaction.description

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

    override fun getItemCount(): Int = savings.size
}