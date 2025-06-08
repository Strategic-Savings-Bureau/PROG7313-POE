package com.ssba.strategic_savings_budget_app.interfaces


import com.ssba.strategic_savings_budget_app.entities.CurrencyConverter
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query


interface CurrencyService {
    @GET("/latest?")
    suspend fun getDesiredCurrency(@Query("apikey") apikey:String): Response<CurrencyConverter>
}