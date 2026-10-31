package com.sujal.expensetracker.ui.expenses.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sujal.expensetracker.data.entity.ExpenseEntity
import com.sujal.expensetracker.databinding.ItemExpenseBinding
import com.sujal.expensetracker.databinding.ItemExpenseMonthHeaderBinding
import com.sujal.expensetracker.util.emojiForCategory
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** A row in the expenses list: either a month divider or an actual expense. */
sealed class ExpenseListRow {
    data class MonthHeader(val label: String) : ExpenseListRow()
    data class Item(val expense: ExpenseEntity) : ExpenseListRow()
}

/** Groups [expenses] (already sorted newest-first) into rows with a header inserted
 * wherever the calendar month changes. */
fun buildExpenseListRows(expenses: List<ExpenseEntity>): List<ExpenseListRow> {
    val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    val rows = mutableListOf<ExpenseListRow>()
    var lastMonthLabel: String? = null
    for (expense in expenses) {
        val label = monthFormat.format(Date(expense.dateMillis))
        if (label != lastMonthLabel) {
            rows.add(ExpenseListRow.MonthHeader(label))
            lastMonthLabel = label
        }
        rows.add(ExpenseListRow.Item(expense))
    }
    return rows
}

class ExpenseAdapter(
    private val onDelete: (ExpenseEntity) -> Unit
) : ListAdapter<ExpenseListRow, RecyclerView.ViewHolder>(DIFF) {

    private val moneyFormat = DecimalFormat("₹#,##0.##")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    inner class HeaderViewHolder(val binding: ItemExpenseMonthHeaderBinding) : RecyclerView.ViewHolder(binding.root)
    inner class ItemViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int =
        if (getItem(position) is ExpenseListRow.MonthHeader) TYPE_HEADER else TYPE_ITEM

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(ItemExpenseMonthHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            ItemViewHolder(ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = getItem(position)) {
            is ExpenseListRow.MonthHeader -> {
                (holder as HeaderViewHolder).binding.root.text = row.label
            }
            is ExpenseListRow.Item -> {
                val item = row.expense
                with((holder as ItemViewHolder).binding) {
                    textIcon.text = emojiForCategory(item.category)
                    textDescription.text = item.description
                    textSubtitle.text = "${item.category} · ${dateFormat.format(Date(item.dateMillis))}"
                    textAmount.text = moneyFormat.format(item.amount)
                    btnDelete.setOnClickListener { onDelete(item) }
                }
            }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ITEM = 1

        private val DIFF = object : DiffUtil.ItemCallback<ExpenseListRow>() {
            override fun areItemsTheSame(a: ExpenseListRow, b: ExpenseListRow): Boolean = when {
                a is ExpenseListRow.MonthHeader && b is ExpenseListRow.MonthHeader -> a.label == b.label
                a is ExpenseListRow.Item && b is ExpenseListRow.Item -> a.expense.id == b.expense.id
                else -> false
            }
            override fun areContentsTheSame(a: ExpenseListRow, b: ExpenseListRow): Boolean = a == b
        }
    }
}
