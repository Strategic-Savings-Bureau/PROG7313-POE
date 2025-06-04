package com.ssba.strategic_savings_budget_app.graph_data

import java.util.Date

data class TransactionGraphData(
    val amount: Float,
    val date: Date,
    val type: TransactionType
)
