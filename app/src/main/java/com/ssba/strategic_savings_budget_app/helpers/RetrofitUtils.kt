package com.ssba.strategic_savings_budget_app.helpers

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitUtils {

    fun retrofit2(): Retrofit {
        return Retrofit.Builder().baseUrl("https://api.currencyfreaks.com/v2.0/rates/")
            .addConverterFactory(GsonConverterFactory.create()).build()
    }


}