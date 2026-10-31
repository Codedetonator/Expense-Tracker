package com.sujal.expensetracker.ui.wallet

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sujal.expensetracker.databinding.ItemDenominationBinding

/**
 * One row per note/coin. Rows keep their own count locally and only touch the row being
 * edited, so typing never rebuilds the list (that was what dropped focus/keyboard before).
 */
class DenominationAdapter(
    private val onCountChanged: (value: Int, count: Int) -> Unit
) : RecyclerView.Adapter<DenominationAdapter.ViewHolder>() {


    private val rows = mutableListOf<Row>()

    init { setHasStableIds(true) }

    override fun getItemId(position: Int): Long = rows[position].value.toLong()

    fun submitRows(newRows: List<Pair<Int, Int>>) {
        rows.clear()
        rows.addAll(newRows.map { Row(it.first, it.second) })
        notifyDataSetChanged()
    }

    /** Inserts a new row (count 0) in descending order. Returns its position, or -1 if it exists. */
    fun addRow(value: Int): Int {
        if (rows.any { it.value == value }) return -1
        val index = rows.indexOfFirst { it.value < value }.let { if (it == -1) rows.size else it }
        rows.add(index, Row(value, 0))
        notifyItemInserted(index)
        return index
    }

    inner class ViewHolder(val binding: ItemDenominationBinding) : RecyclerView.ViewHolder(binding.root) {
        private var binding_ = false

        init {
            binding.editCount.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    if (binding_) return
                    setCount(s?.toString()?.toIntOrNull() ?: 0, rewriteText = false)
                }
            })
            binding.btnPlus.setOnClickListener { setCount(currentCount() + 1, rewriteText = true) }
            binding.btnMinus.setOnClickListener { setCount((currentCount() - 1).coerceAtLeast(0), rewriteText = true) }
        }

        private fun currentRow(): Row? =
            bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }?.let { rows[it] }

        private fun currentCount() = currentRow()?.count ?: 0

        private fun setCount(newCount: Int, rewriteText: Boolean) {
            val row = currentRow() ?: return
            row.count = newCount
            binding.textSubtotal.text = "₹${row.value.toLong() * newCount}"
            if (rewriteText) writeCountText(newCount)
            onCountChanged(row.value, newCount)
        }

        private fun writeCountText(count: Int) {
            binding_ = true
            binding.editCount.setText(if (count == 0) "" else count.toString())
            binding.editCount.setSelection(binding.editCount.text.length)
            binding_ = false
        }

        fun bind(row: Row) {
            binding.textValue.text = "₹${row.value}"
            binding.textSubtotal.text = "₹${row.value.toLong() * row.count}"
            writeCountText(row.count)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemDenominationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(rows[position])

    override fun getItemCount() = rows.size
}
