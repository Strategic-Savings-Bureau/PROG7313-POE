package com.ssba.strategic_savings_budget_app.categories

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.adapters.CategoryAdapter
import com.ssba.strategic_savings_budget_app.data.AppDatabase
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory
import com.ssba.strategic_savings_budget_app.landing.LoginActivity
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var rvCategories: RecyclerView
    private lateinit var db: AppDatabase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        rvCategories = findViewById(R.id.rvCategories)
        rvCategories.layoutManager = GridLayoutManager(this, 3)

        db = AppDatabase.getInstance(this)
        auth = FirebaseAuth.getInstance()

        lifecycleScope.launch {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                // if not logged in, send to your LoginActivity
                startActivity(Intent(this@CategoriesActivity, LoginActivity::class.java))
                finish()
                return@launch
            }

            val categories: List<ExpenseCategory> =
                db.expenseCategoryDao.getCategoriesWithTransactions(uid)

            rvCategories.adapter = CategoryAdapter(categories) { category ->
                // onClick, send user to CategoryDetailActivity
                startActivity(Intent(this@CategoriesActivity, CategoryDetailActivity::class.java)
                    .putExtra("categoryId", category.categoryId))
            }
        }
    }
}