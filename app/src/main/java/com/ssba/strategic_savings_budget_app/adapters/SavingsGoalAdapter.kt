package com.ssba.strategic_savings_budget_app.adapters

import android.annotation.SuppressLint
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.SavingsGoalActivity
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.SavingGoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 	* Code Attribution
 	* Purpose: Formatting numbers as South African Rand (ZAR) currency using NumberFormat
 	* Author: Android Developers
 	* Date Accessed: 30 April 2025
 	* Source: Developer Guide - Android Developers (Java Platform Standard Edition docs)
 	* URL: https://developer.android.com/reference/java/text/NumberFormat
*/

class SavingsGoalAdapter(private var savingsGoals: List<SavingGoal>) :
    RecyclerView.Adapter<SavingsGoalAdapter.SavingsGoalViewHolder>()
{
    inner class SavingsGoalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val tvGoalTitle: TextView = itemView.findViewById(R.id.tvGoalTitle)
        val tvTargetAmount: TextView = itemView.findViewById(R.id.tvTargetAmount)
        val tvEndDate: TextView = itemView.findViewById(R.id.tvEndDate)
        val pbGoal: ProgressBar = itemView.findViewById(R.id.progressGoal)
        val tvProgressPercentage: TextView = itemView.findViewById(R.id.tvProgressPercentage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavingsGoalViewHolder
    {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_savings_goal, parent, false)
        return SavingsGoalViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: SavingsGoalAdapter.SavingsGoalViewHolder, position: Int)
    {
        val goal = savingsGoals[position]

        val dateFormatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        val context = holder.itemView.context

        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

        holder.tvGoalTitle.text = goal.title
        holder.tvTargetAmount.text = "Target: ${currencyFormat.format(goal.targetAmount)}"
        holder.tvEndDate.text = "End Date: ${dateFormatter.format(goal.endDate)}"

        // if end date is in the past change color to red
        if (goal.endDate.before(Date())) {
            holder.tvEndDate.setTextColor(ContextCompat.getColor(context, R.color.expense_red))
        }

        val db: AppDatabase = AppDatabase.getInstance(context)

        CoroutineScope(Dispatchers.Main).launch {

            // get the total savings for the current goal
            val totalSavings = getTotalSavingsForGoal(goal.title, db)

            val target = goal.targetAmount

            // calculate the progress percentage
            val progressPercentage = (totalSavings / target) * 100

            holder.pbGoal.progress = progressPercentage.toInt()
            holder.tvProgressPercentage.text = "${progressPercentage.toInt()}% saved"

            // if progress percentage is 100% change color to green
            if (progressPercentage >= 100.0) {
                holder.tvProgressPercentage.setTextColor(ContextCompat.getColor(context, R.color.income_green))
            }
        }

        holder.itemView.setOnClickListener {

            // get the savings goal Title
            val goalTitle = goal.title

            // navigate to the savings goal activity and pass the title
            val intent = Intent(context, SavingsGoalActivity::class.java)
            intent.putExtra("SAVINGS_GOAL_TITLE", goalTitle)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = savingsGoals.size

    // region Helper Methods

    // get the total of all savings for the current goal
    private suspend fun getTotalSavingsForGoal(goalTitle: String, db: AppDatabase): Double
    {
        val savingsGoalWithSavings = db.savingsGoalDao().getSavingsBySavingGoalTitle(goalTitle)

        if (savingsGoalWithSavings.isEmpty()) {
            return 0.0
        }

        val savingsList = savingsGoalWithSavings[0].savings

        if (savingsList.isEmpty()) {
            return 0.0
        }

        var savings = 0.0

        for (saving in savingsList) {
            savings += saving.amount
        }

        return savings
    }

    // endregion
}