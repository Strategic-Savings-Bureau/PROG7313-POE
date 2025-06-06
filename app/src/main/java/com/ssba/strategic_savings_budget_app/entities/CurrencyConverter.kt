package com.ssba.strategic_savings_budget_app.entities

data class CurrencyConverter (
    val date:String,
    val base: String,
    val rates: Map<String, String>
)