package com.ssba.strategic_savings_budget_app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ssba.strategic_savings_budget_app.R
import com.ssba.strategic_savings_budget_app.entities.ExpenseCategory

class CategoryAdapter(
    private val items: List<ExpenseCategory>,
    private val onClick: (ExpenseCategory) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        private val ivIcon: ImageView = view.findViewById(R.id.ivIcon)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        fun bind(cat: ExpenseCategory) {
            // load your icon (e.g. from a drawable name or URL)
            val resId = itemView.context.resources.getIdentifier(
                cat.icon, "drawable", itemView.context.packageName
            )
            ivIcon.setImageResource(if (resId != 0) resId else R.drawable.ic_default_category)
            tvName.text = cat.name
            itemView.setOnClickListener { onClick(cat) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) =
        holder.bind(items[position])
}